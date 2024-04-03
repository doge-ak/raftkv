package io.github.magician403.raftkv.server.timer.impl

import io.github.magician403.raftkv.server.pojo.NodeConfig
import io.github.magician403.raftkv.server.service.ClientService
import io.github.magician403.raftkv.server.timer.Timer
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * 选举超时计时器，计时器的任务开始执行时，切换到candidate类型
 *
 * @property vertx
 */
class ElectionTimeoutTimer(
    private val vertx: Vertx
) : Timer, CoroutineScope {
    override val coroutineContext: CoroutineContext = vertx.dispatcher()
    private var timerId: Long? = null
    private val service: ClientService = vertx.orCreateContext.getLocal("clientService")
    private val nodeConfig: NodeConfig = vertx.orCreateContext.getLocal("nodeConfig")

    override fun start() {
        timerId = vertx.setTimer(nodeConfig.electionTimeoutMs) {
            launch(coroutineContext) {
                service.electLeader()
            }
        }
    }

    override fun stop() {
        vertx.cancelTimer(timerId!!)
        timerId = null
    }
}