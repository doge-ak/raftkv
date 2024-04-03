package io.github.magician403.raftkv.server.po

import io.github.magician403.raftkv.server.pojo.Log

/**
 * 基于sql的日志list
 */
interface SqlLogList {
    suspend fun add(log: Log)

    suspend fun lastOrNull(): Log?

    suspend fun contains(index: ULong, term: ULong): Boolean

    suspend fun delete(startIndex: ULong): Boolean

    suspend fun getOrNull(index: ULong): Log?

    suspend fun getSubList(startIndex: ULong): List<Log>

    suspend fun init(): SqlLogList
}