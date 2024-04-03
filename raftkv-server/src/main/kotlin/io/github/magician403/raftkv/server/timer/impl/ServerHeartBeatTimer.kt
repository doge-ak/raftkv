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
 *
 */
class ServerHeartBeatTimer(
    private val vertx: Vertx,
) : Timer, CoroutineScope {
    private var timerId: Long? = null
    private val service: ClientService = vertx.orCreateContext.getLocal("clientService")
    private val nodeConfig: NodeConfig = vertx.orCreateContext.getLocal("nodeConfig")
    override val coroutineContext: CoroutineContext = vertx.dispatcher()

    override fun start() {
        timerId = vertx.setTimer(nodeConfig.heartBeatTimeMs) {
            launch {
                service.appendEntries(null)
            }
        }
    }

    override fun stop() {
        vertx.cancelTimer(timerId!!)
        timerId = null
    }
}