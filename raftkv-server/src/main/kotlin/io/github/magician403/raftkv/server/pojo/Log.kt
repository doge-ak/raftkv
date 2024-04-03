package io.github.magician403.raftkv.server.pojo

data class Log(
    val command: Command,
    val term: ULong,
    val index: ULong
)