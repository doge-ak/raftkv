package io.github.magician403.raftkv.server.timer

interface Timer {
    fun start()

    fun stop()

    fun reset() {
        stop()
        start()
    }
}