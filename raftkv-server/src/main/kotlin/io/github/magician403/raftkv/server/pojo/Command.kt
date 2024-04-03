package io.github.magician403.raftkv.server.pojo

data class Command(
    val operation: Operation,
    val args: List<String>
)