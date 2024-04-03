package io.github.magician403.raftkv.server.controller

import io.github.magician403.raftkv.server.po.SqlLogList
import io.github.magician403.raftkv.server.po.SqlStateMachine
import io.github.magician403.raftkv.server.pojo.*
import io.github.magician403.raftkv.server.timer.impl.ElectionTimeoutTimer
import io.vertx.core.Vertx
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.kotlin.coroutines.CoroutineRouterSupport
import io.vertx.kotlin.coroutines.dispatcher
import kotlin.coroutines.CoroutineContext
import kotlin.math.min

/**
 * 本机节点为follower类型的时候，请求接收控制器
 * leader类型的时候转发kv请求
 * @property vertx
 */
class Controller(
    private val vertx: Vertx
) : CoroutineRouterSupport {
    private val router: Router = vertx.orCreateContext.getLocal("router")
    override val coroutineContext: CoroutineContext = vertx.dispatcher()
    private val bodyHandler: BodyHandler = BodyHandler.create()
    private val electionTimeoutTimer: ElectionTimeoutTimer = vertx.orCreateContext.getLocal("electionTimeoutTimer")
    private val stateMachine: SqlStateMachine = vertx.orCreateContext.getLocal("sqlStateMachine")
    private val nodeState: NodeState = vertx.orCreateContext.getLocal("serverState")
    private val redirectHandler: RedirectHandler = vertx.orCreateContext.getLocal("redirectHandler")
    private lateinit var logs: SqlLogList

    init {
        requestVote()
        appendEntries()
        entries()
        keys()
        values()
        size()
        clear()
        isEmpty()
        remove()
        put()
        putAll()
        get()
        containsKey()
        containsValue()
    }

    private fun requestVote() {
        router.post("/requestVote")
            .handler(bodyHandler)
            .handler { rc ->
                val request = rc.body()
                    .asJsonObject()
                    .mapTo(RequestVoteRequest::class.java)

                val currentTerm = nodeState.currentTerm!!
                val voteFor = nodeState.voteFor
                val candidateId = request.candidateId
                // 任期号小于本节点任期号，不给投票
                if (request.term < currentTerm) {
                    val response = RequestVoteResponse(currentTerm, false)
                    response.voteGranted = false
                    rc.json(response)
                    return@handler
                }
                // 下面的都是拉票方任期号大于等于本节点任期号
                // 如果 votedFor 为空或者为 candidateId，并且候选人的日志至少和自己一样新，那么就投票给他
                if (voteFor == null || voteFor == candidateId) {
                    // 转变角色为follower
                    nodeState.role = NodeRole.FOLLOWER
                    if (request.term > currentTerm) {
                        nodeState.currentTerm = request.term
                    }
                    val response = RequestVoteResponse(currentTerm, true)
                    rc.json(response)
                    return@handler
                }
            }
    }

    private fun appendEntries() {
        router.post("/appendEntries")
            .handler(bodyHandler)
            .coHandler { rc ->
                val request = rc.body()
                    .asJsonObject()
                    .mapTo(AppendEntriesRequest::class.java)
                nodeState.leaderId = request.leaderId
                nodeState.leaderHost = rc.request().remoteAddress().hostAddress()
                nodeState.leaderPort = nodeState.remoteNodes[0].port
                val currentTerm = nodeState.currentTerm!!
                // 重置选举超时计时器
                electionTimeoutTimer.reset()
                // 发送者的term小于接收者的term，leader过期
                if (request.term < currentTerm) {
                    val response = AppendEntriesResponse(currentTerm, false)
                    rc.json(response)
                    return@coHandler
                }

                // 返回假 如果接收者日志中没有包含这样一个条目 即该条目的任期在 prevLogIndex 上能和 prevLogTerm 匹配上
                // （译者注：在接收者日志中 如果能找到一个和 prevLogIndex 以及 prevLogTerm 一样的索引和任期的日志条目 则继续执行下面的步骤
                // 否则返回假
                // 情况：日志缺失，无法添加日志
                if (logs.contains(request.prevLogIndex, request.prevLogTerm)
                    && request.prevLogTerm != 0uL && request.prevLogTerm != 0uL
                ) {
                    val response = AppendEntriesResponse(currentTerm, false)
                    rc.json(response)
                    return@coHandler
                }
                // 空日志
                if (request.entries.isEmpty()) {
                    // 角色切换（如果有必要的话）
                    if (nodeState.role != NodeRole.FOLLOWER) {
                        nodeState.role = NodeRole.FOLLOWER
                    }
                    val response = AppendEntriesResponse(currentTerm, true)
                    rc.json(response)
                    return@coHandler
                }
                // 如果一个已经存在的条目和新条目（译者注：即刚刚接收到的日志条目）发生了冲突（因为索引相同，任期不同），
                // 那么就删除这个已经存在的条目以及它之后的所有条目
                // 非空日志
                // 日志同步的开始索引
                var startIndex = request.entries[0].index
                // 查找不一致的 开始索引，并删除开始不一致的日志
                for (entry in request.entries) {
                    if (logs.contains(entry.index, entry.term)) {
                        startIndex = entry.index
                        logs.delete(entry.index)
                        break
                    }
                }
                // 追加日志中尚未存在的任何新条目
                for (entry in request.entries) {
                    if (entry.index < startIndex) {
                        continue
                    }
                    logs.add(entry)
                }
                // 如果领导人的已知已提交的最高日志条目的索引大于接收者的已知已提交最高日志条目的索引（leaderCommit > commitIndex），
                // 则把接收者的已知已经提交的最高的日志条目的索引commitIndex 重置为 领导人的已知已经提交的最高的日志条目的索引 leaderCommit
                // 或者是 上一个新条目的索引 取两者的最小值
                if (request.leaderCommit > nodeState.commitIndex!!) {
                    nodeState.commitIndex = min(request.leaderCommit, request.entries.last().index)
                }
                val response = AppendEntriesResponse(currentTerm, true)
                rc.json(response)
                return@coHandler
            }
    }

    private fun entries() {
        router.get("/entries")
            .handler(redirectHandler)
            .handler(bodyHandler)
            .coHandler {
                val entries = stateMachine.entries()
                it.json(entries)
            }
    }

    private fun keys() {
        router.get("/keys")
            .handler(redirectHandler)
            .handler(bodyHandler)
            .coHandler {
                val keys = stateMachine.keys()
                it.json(keys)
            }
    }

    private fun values() {
        router.get("/values")
            .handler(redirectHandler)
            .handler(bodyHandler)
            .coHandler {
                val values = stateMachine.values()
                it.json(values)
            }
    }

    private fun size() {
        router.get("/size")
            .handler(redirectHandler)
            .handler(bodyHandler)
            .coHandler {
                val size = stateMachine.size()
                it.json(size)
            }
    }

    private fun clear() {
        router.post("/clear")
            .handler(redirectHandler)
            .handler(bodyHandler)
            .coHandler {
                stateMachine.clear()
                it.end()
            }
    }

    private fun isEmpty() {
        router.get("/empty")
            .handler(redirectHandler)
            .handler(bodyHandler)
            .coHandler {
                val isEmpty = stateMachine.isEmpty()
                it.json(isEmpty)
            }
    }

    private fun remove() {
        router.post("/remove")
            .handler(redirectHandler)
            .handler(bodyHandler)
            .coHandler {
                val key = it.body().asString()
                stateMachine.remove(key)
                it.end()
            }
    }

    private fun putAll() {
        router.post("/putAll")
            .handler(redirectHandler)
            .handler(bodyHandler)
            .coHandler {
                @Suppress("UNCHECKED_CAST")
                val map = it.body().asJsonObject().map as Map<String, String>
                stateMachine.putAll(map)
                it.end()
            }
    }

    private fun get() {
        router.get("/get")
            .handler(redirectHandler)
            .handler(bodyHandler)
            .coHandler {
                val key = it.body().asString()
                val value = stateMachine.get(key)
                it.json(value)
            }
    }

    private fun containsKey() {
        router.get("/containsKey")
            .handler(redirectHandler)
            .handler(bodyHandler)
            .coHandler {
                val key = it.body().asString()
                val containsKey = stateMachine.containsKey(key)
                it.json(containsKey)
            }
    }

    private fun containsValue() {
        router.get("/containsValue")
            .handler(redirectHandler)
            .handler(bodyHandler)
            .coHandler {
                val value = it.body().asString()
                val containsValue = stateMachine.containsValue(value)
                it.json(containsValue)
            }
    }

    private fun put() {
        router.post("/put")
            .handler(redirectHandler)
            .handler(bodyHandler)
            .coHandler {
                val map = it.body().asJsonObject().map
                val entry = map.entries.first()
                stateMachine.put(entry.key, entry.value as String)
                it.end()
            }
    }
}