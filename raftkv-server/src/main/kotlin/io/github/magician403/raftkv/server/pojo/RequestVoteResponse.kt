package io.github.magician403.raftkv.server.pojo

class RequestVoteResponse(
    /**
     * 投票方(对方)的当前任期号
     */
    var term: ULong,

    /**
     * 是否candidate(己方)收到投票
     */
    var voteGranted: Boolean
)