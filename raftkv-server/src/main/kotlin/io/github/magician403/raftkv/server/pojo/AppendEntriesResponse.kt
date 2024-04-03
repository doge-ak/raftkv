package io.github.magician403.raftkv.server.pojo

data class AppendEntriesResponse(
    /**
     * follower的任期
     */
    val term: ULong,     // 当前任期，对于领导人而言 它会更新自己的任期

    /**
     * 添加日志是否成功
     */
    val success: Boolean // 如果跟随者所含有的条目和 prevLogIndex 以及 prevLogTerm 匹配上了，则为 true
)