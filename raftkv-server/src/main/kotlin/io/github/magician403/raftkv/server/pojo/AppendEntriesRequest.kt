package io.github.magician403.raftkv.server.pojo

data class AppendEntriesRequest(
    /**
     * leader的任期（自己）
     */
    val term: ULong,

    /**
     * leader的id（自己），用于让follower（对方）重定向客户端的调用
     */
    val leaderId: String,

    /**
     * 紧邻新日志条目之前的那个日志条目的索引
     */
    var prevLogIndex: ULong,

    /**
     * 紧邻新日志条目之前的那个日志条目的任期
     */
    val prevLogTerm: ULong,

    /**
     * 要被保存的日志条目（被当做心跳使用时，则日志条目内容为空；为了提高效率可能一次性发送多个）
     */
    val entries: List<Log>,

    /**
     * 领导人的已知已提交的最高的日志条目的索引
     */
    val leaderCommit: ULong,
)