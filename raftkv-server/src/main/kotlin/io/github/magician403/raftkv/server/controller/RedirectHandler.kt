package io.github.magician403.raftkv.server.controller

import io.github.magician403.raftkv.server.pojo.NodeRole
import io.github.magician403.raftkv.server.pojo.NodeState
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.ext.web.RoutingContext

/**
 * 重定向处理器
 */
class RedirectHandler private constructor(
    private val vertx: Vertx
) : Handler<RoutingContext> {
    private val nodeState = vertx.orCreateContext.getLocal<NodeState>("nodeState")
    override fun handle(event: RoutingContext) {
        if (nodeState.role == NodeRole.LEADER) {
            event.next()
        } else if (nodeState.role == NodeRole.FOLLOWER) {
            event.redirect(
                "${if (event.request().isSSL) "https://" else "http://"}${nodeState.leaderHost}:${nodeState.leaderPort}${
                    event.request().path()
                }"
            )
        } else {
            event.fail(503)
        }
    }

    companion object {
        fun create(vertx: Vertx): RedirectHandler = RedirectHandler(vertx)
    }
}