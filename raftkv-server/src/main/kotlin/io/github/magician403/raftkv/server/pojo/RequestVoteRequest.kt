package io.github.magician403.raftkv.server.pojo

class RequestVoteRequest(
    /**
     * 当前节点(candidate)的任期
     */
    val term: ULong,

    /**
     * 当前节点(candidate)的id
     */
    val candidateId: String,

    /**
     * 当前节点(candidate)最后的日志下标
     */
    val lastLogIndex: ULong,

    /**
     * 当前节点(candidate)最后的日志任期
     */
    val lastLogTerm: ULong
)