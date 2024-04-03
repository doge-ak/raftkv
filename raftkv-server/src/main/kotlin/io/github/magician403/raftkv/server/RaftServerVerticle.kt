package io.github.magician403.raftkv.server

import io.github.magician403.raftkv.server.config.JacksonConfig
import io.github.magician403.raftkv.server.controller.Controller
import io.github.magician403.raftkv.server.po.impl.SqliteStateMachine
import io.github.magician403.raftkv.server.pojo.NodeConfig
import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.client.WebClientOptions
import io.vertx.jdbcclient.JDBCPool
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.kotlin.jdbcclient.jdbcConnectOptionsOf
import io.vertx.kotlin.sqlclient.poolOptionsOf

class RaftServerVerticle : CoroutineVerticle() {
    override suspend fun start() {
        // jackson配置
        JacksonConfig().init()
        // config.yaml
        val configStoreOptions = ConfigStoreOptions()
            .setType("file")
            .setFormat("yaml")
            .setConfig(JsonObject().put("path", "config.yaml"))
        val retriever = ConfigRetriever.create(vertx, ConfigRetrieverOptions().addStore(configStoreOptions))
        val nodeConfig = retriever.config
            .coAwait()
            .mapTo(NodeConfig::class.java)
        vertx.orCreateContext.putLocal("nodeConfig", nodeConfig)
        // router
        val router = Router.router(vertx)
        vertx.orCreateContext.putLocal("router", router)
        // controller
        val controller = Controller(vertx)
        // webClient
        val webClient = WebClient.create(vertx, WebClientOptions().setHttp2ClearTextUpgrade(true))
        vertx.orCreateContext.putLocal("webClient", webClient)
        // StateMachine
        val sqliteStateMachine = SqliteStateMachine(vertx)
        vertx.orCreateContext.putLocal("sqliteStateMachine", sqliteStateMachine)
        // jdbcPool
        val jdbcPool = JDBCPool.pool(vertx, jdbcConnectOptionsOf(jdbcUrl = "jdbc:sqlite:kv.db"), poolOptionsOf())
        vertx.orCreateContext.putLocal("jdbcPool", jdbcPool)
        // httpServer
        vertx.createHttpServer(HttpServerOptions().setLogActivity(true))
            .requestHandler(router)
            .listen(nodeConfig.port)
            .coAwait()
    }

    override suspend fun stop() {

    }
}