package io.github.magician403.raftkv.server

import io.vertx.core.Vertx

fun main() {
    val vertx = Vertx.vertx()
    vertx.deployVerticle(RaftServerVerticle())
}