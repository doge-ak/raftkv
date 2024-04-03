package io.github.magician403.raftkv.server.service

import io.github.magician403.raftkv.server.pojo.*
import io.vertx.core.Vertx
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.codec.BodyCodec
import io.vertx.kotlin.coroutines.coAwait

class Http2RpcClient(
    vertx: Vertx
) : RpcClient {
    private val webClient: WebClient = vertx.orCreateContext.getLocal("webClient")
    private val nodeConfig: NodeConfig = vertx.orCreateContext.getLocal("nodeConfig")

    override suspend fun requestVote(address: NodeAddress, param: RequestVoteRequest): RequestVoteResponse {
        val response = webClient.post(address.port, address.host, "/requestVote")
            .`as`(BodyCodec.json(RequestVoteResponse::class.java))
            .timeout(nodeConfig.requestVoteTimeoutMs)
            .sendJson(param)
            .coAwait()
            .body()
        return response
    }

    override suspend fun appendEntries(address: NodeAddress, param: AppendEntriesRequest): AppendEntriesResponse {
        val response = webClient.post(address.port, address.host, "/appendEntries")
            .`as`(BodyCodec.json(AppendEntriesResponse::class.java))
            .timeout(nodeConfig.requestVoteTimeoutMs)
            .sendJson(param)
            .coAwait()
            .body()
        return response
    }
}