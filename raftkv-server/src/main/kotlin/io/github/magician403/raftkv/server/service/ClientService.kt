package io.github.magician403.raftkv.server.service

import io.github.magician403.raftkv.server.po.SqlLogList
import io.github.magician403.raftkv.server.po.SqlStateMachine
import io.github.magician403.raftkv.server.pojo.*
import io.github.magician403.raftkv.server.timer.impl.ElectionTimeoutTimer
import io.github.magician403.raftkv.server.util.addToStateMachine
import io.github.magician403.raftkv.server.util.warn4kt
import io.vertx.core.Vertx
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.*
import java.util.concurrent.TimeoutException
import kotlin.coroutines.CoroutineContext
import kotlin.random.Random
import kotlin.random.nextLong

/**
 * 当前角色为candidate，leader
 *
 * @property vertx
 */
class ClientService(
    private val vertx: Vertx
) : CoroutineScope {
    override val coroutineContext: CoroutineContext = vertx.dispatcher()
    private val rpcClient: RpcClient = vertx.orCreateContext.getLocal("http2RpcClient")
    private val nodeConfig: NodeConfig = vertx.orCreateContext.getLocal("nodeConfig")
    private val electionTimeoutTimer: ElectionTimeoutTimer = vertx.orCreateContext.getLocal("electionTimeoutTimer")
    private lateinit var nodeState: NodeState
    private lateinit var stateMachine: SqlStateMachine
    private lateinit var logs: SqlLogList

    /**
     * 对于每一台服务器，发送到该服务器的下一个日志条目的索引（初始值为领导人最后的日志条目的索引+1）(选举后已经重新初始化)
     */
    private val nextIndex = HashMap<String, ULong>()

    /**
     * 对于每一台服务器，已知的已经复制到该服务器的最高日志条目的索引（初始值为0，单调递增）(选举后已经重新初始化)
     */
    private val matchIndex = HashMap<String, ULong>()

    /**
     * leader选举
     */
    suspend fun electLeader() = coroutineScope {
        // 自增当前的任期号
        nodeState.currentTerm = nodeState.currentTerm!! + 1UL
        // 给自己投票
        nodeState.voteFor = nodeState.id
        // 将自己的角色变为候选人
        nodeState.role = NodeRole.CANDIDATE
        // 重置选举超时计时器
        electionTimeoutTimer.stop()
        // 发送投票请求
        voteLoop@ while (true) {
            val log = logs.lastOrNull()
            val currentTerm = nodeState.currentTerm!!

            val responses = nodeState.remoteNodes.map { node ->
                async {
                    try {
                        val param =
                            RequestVoteRequest(
                                currentTerm,
                                nodeState.id,
                                log?.index ?: 0UL,
                                log?.term ?: 0UL
                            )
                        rpcClient.requestVote(NodeAddress(node.host!!, node.port!!), param)
                    } catch (e: TimeoutException) {
                        logger.warn4kt { "投票超时,节点为$node" }
                        null
                    }
                }
            }.awaitAll()
            // 选票包括自己投给自己的
            val votePercentage: Double =
                (responses.count { it?.voteGranted == true }.toDouble() + 1) / nodeConfig.nodes.size.toDouble()
            // 如果接收到大多数服务器的选票，那么就变成领导人
            if (votePercentage >= 0.5) {
                nodeState.role = NodeRole.LEADER
                // 建立心跳 todo
                break@voteLoop
            }
            // 当投票请求响应的term大于当前候选节点的term时，认为其它节点赢得了选举，则切换回follower
            if (responses.any { it != null && it.term > currentTerm }) {
                nodeState.role = NodeRole.FOLLOWER
                initLeaderInfo()
                break@voteLoop
            }
            // 如果选举过程超时，则再次发起一轮选举
            if (votePercentage < 0.5) {
                delay(Random.nextLong(nodeConfig.voteTimeoutMinMs..nodeConfig.voteTimeoutMaxMs))
            }
        }
    }


    /**
     * 日志复制
     * @param command 命令
     */
    suspend fun appendEntries(command: Command?): Boolean = coroutineScope {
        val currentTerm = nodeState.currentTerm!!
        val commitIndex = nodeState.commitIndex!!
        val id = nodeState.id
        if (command != null) {
            logs.add(Log(command, currentTerm, commitIndex))
        }
        val lastLog = logs.lastOrNull()
        val lastLogIndex = lastLog?.index ?: 0UL
        val lastLogTerm = lastLog?.term ?: 0UL
        while (true) {
            val deferredResponses = nodeState.remoteNodes.filter { matchIndex[it.id]!! < lastLogIndex }
                .associateWith { node ->
                    async {
                        val entries = logs.getSubList(nextIndex[node.id]!!)
                        val request =
                            AppendEntriesRequest(
                                currentTerm,
                                id,
                                lastLogIndex,
                                lastLogTerm,
                                entries,
                                matchIndex[node.id]!!
                            )
                        try {
                            rpcClient.appendEntries(NodeAddress(node.host!!, node.port!!), request)
                        } catch (e: TimeoutException) {
                            logger.warn4kt { "发送给${node}的日志复制请求超时" }
                            null
                        }
                    }
                }
            deferredResponses.values.awaitAll()
            val responses = deferredResponses.mapValues { it.value.getCompleted() }
            // 多数节点响应成功
            if (responses.values.count { it != null && it.success }
                    .toDouble() / nodeConfig.nodes.size.toDouble() >= 0.5) {
                command?.addToStateMachine(stateMachine)
                return@coroutineScope true
            } else {
                // 修改matchIndex和nextIndex
                responses.filter { it.value?.success ?: false }
                    .forEach { (node, response) ->
                        matchIndex.putIfAbsent(node.id!!, matchIndex[node.id]!! - 1UL)
                        nextIndex.putIfAbsent(node.id!!, nextIndex[node.id]!! - 1UL)
                    }
            }
        }
        return@coroutineScope false
    }

    private suspend fun initLeaderInfo() {
        for (node in nodeState.remoteNodes) {
            matchIndex[node.id!!] = 0UL
        }
        val lastLog = logs.lastOrNull()
        for (node in nodeState.remoteNodes) {
            nextIndex[node.id!!] = lastLog?.index ?: 1UL
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(ClientService::class.java)!!
    }
}