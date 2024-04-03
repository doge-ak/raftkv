package io.github.magician403.raftkv.server.util

import io.github.magician403.raftkv.server.po.SqlStateMachine
import io.github.magician403.raftkv.server.pojo.Command
import io.github.magician403.raftkv.server.pojo.Operation.*
import io.vertx.core.impl.logging.Logger

suspend fun Command.addToStateMachine(stateMachine: SqlStateMachine) {
    when (operation) {
        CLEAR -> stateMachine.clear()
        REMOVE -> stateMachine.remove(args[0])
        PUT -> stateMachine.put(args[0], args[1])
        PUT_ALL -> {
            val map = HashMap<String, String>()
            for (i in args.indices step 2) {
                map[args[i]] = args[i + 1]
            }
            stateMachine.putAll(map)
        }
    }
}

fun Logger.warn4kt(message: () -> String) {
    if (isWarnEnabled) {
        warn(message)
    }
}
