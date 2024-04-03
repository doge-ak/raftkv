package io.github.magician403.raftkv.server.service

import io.github.magician403.raftkv.server.pojo.*

interface RpcClient {
    suspend fun requestVote(address: NodeAddress, param: RequestVoteRequest): RequestVoteResponse

    suspend fun appendEntries(address: NodeAddress, param: AppendEntriesRequest): AppendEntriesResponse
}