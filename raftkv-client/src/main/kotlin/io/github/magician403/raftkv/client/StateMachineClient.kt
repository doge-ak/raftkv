package io.github.magician403.raftkv.client

import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.codec.BodyCodec
import io.vertx.kotlin.core.vertxOptionsOf
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.kotlin.coroutines.dispatcher
import io.vertx.kotlin.coroutines.vertxFuture
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext

@Suppress("UNCHECKED_CAST", "unused")
class StateMachineClient(
    @Suppress("MemberVisibilityCanBePrivate")
    var address: NodeAddress
) : StateMachine, CoroutineScope {
    private val vertx: Vertx = Vertx.vertx(vertxOptionsOf(eventLoopPoolSize = 1, workerPoolSize = 1))
    private val webClient: WebClient = WebClient.create(vertx)
    override val coroutineContext: CoroutineContext = vertx.dispatcher()

    override fun entries(): Future<Map<String, String>> = vertxFuture(vertx, this) {
        val entries = webClient.get(address.port, address.host, "/entries")
            .`as`(BodyCodec.json(Map::class.java))
            .send()
            .coAwait()
            .body()
        return@vertxFuture entries as Map<String, String>
    }

    override fun keys(): Future<Set<String>> = vertxFuture(vertx, this) {
        val keys = webClient.get(address.port, address.host, "/keys")
            .`as`(BodyCodec.json(Set::class.java))
            .send()
            .coAwait()
            .body()
        return@vertxFuture keys as Set<String>
    }

    override fun values(): Future<Collection<String>> = vertxFuture(vertx, this) {
        val values = webClient.get(address.port, address.host, "/values")
            .`as`(BodyCodec.json(Collection::class.java))
            .send()
            .coAwait()
            .body()
        return@vertxFuture values as Collection<String>
    }

    override fun size(): Future<Int> = vertxFuture(vertx, this) {
        val size = webClient.get(address.port, address.host, "/size")
            .`as`(BodyCodec.json(Int::class.java))
            .send()
            .coAwait()
            .body()
        return@vertxFuture size
    }

    override fun clear(): Future<Unit> = vertxFuture(vertx, this) {
        webClient.post(address.port, address.host, "/clear")
            .send()
            .coAwait()
    }

    override fun isEmpty(): Future<Boolean> = vertxFuture(vertx, this) {
        val isEmpty = webClient.get(address.port, address.host, "/isEmpty")
            .`as`(BodyCodec.json(Boolean::class.java))
            .send()
            .coAwait()
            .body()
        return@vertxFuture isEmpty
    }

    override fun remove(key: String): Future<Unit> = vertxFuture(vertx, this) {
        webClient.post(address.port, address.host, "/remove")
            .sendJson(key)
            .coAwait()
    }

    override fun putAll(map: Map<String, String>): Future<Unit> = vertxFuture(vertx, this) {
        webClient.post(address.port, address.host, "/putAll")
            .sendJson(map)
            .coAwait()
    }

    override fun put(key: String, value: String): Future<Unit> = vertxFuture(vertx, this) {
        webClient.post(address.port, address.host, "/put")
            .sendJson(mapOf(key to value))
            .coAwait()
    }

    override fun get(key: String): Future<String?> = vertxFuture(vertx, this) {
        val value = webClient.get(address.port, address.host, "/get")
            .`as`(BodyCodec.json(String::class.java))
            .sendJson(key)
            .coAwait()
            .body()
        return@vertxFuture value
    }

    override fun containsKey(key: String): Future<Boolean> = vertxFuture(vertx, this) {
        val containsKey = webClient.get(address.port, address.host, "/containsKey")
            .`as`(BodyCodec.json(Boolean::class.java))
            .sendJson(key)
            .coAwait()
            .body()
        return@vertxFuture containsKey
    }

    override fun containsValue(value: String): Future<Boolean> = vertxFuture(vertx, this) {
        val containsValue = webClient.get(address.port, address.host, "/containsValue")
            .`as`(BodyCodec.json(Boolean::class.java))
            .sendJson(value)
            .coAwait()
            .body()
        return@vertxFuture containsValue
    }
}