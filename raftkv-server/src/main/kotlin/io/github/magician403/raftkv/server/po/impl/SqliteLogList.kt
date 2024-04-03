package io.github.magician403.raftkv.server.po.impl

import io.github.magician403.raftkv.server.po.SqlLogList
import io.github.magician403.raftkv.server.pojo.Log
import io.vertx.core.Vertx
import io.vertx.jdbcclient.JDBCPool
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.sqlclient.templates.SqlTemplate

/**
 * sqlite实现的log列表
 *
 * @property vertx
 */
class SqliteLogList(
    private val vertx: Vertx
) : SqlLogList {
    private val jdbcPool = vertx.orCreateContext.getLocal<JDBCPool>("jdbcPool")

    override suspend fun add(log: Log) {
        val connection = jdbcPool.connection.coAwait()
        SqlTemplate.forUpdate(connection, "insert into $TABLE_NAME values (#{index},#{term},#{command})")
            .mapFrom(Log::class.java)
            .execute(log)
            .coAwait()
    }

    override suspend fun lastOrNull(): Log? {
        val connection = jdbcPool.connection.coAwait()
        return SqlTemplate.forQuery(connection, "select * from $TABLE_NAME order by `index` desc limit 1")
            .mapTo(Log::class.java)
            .execute(null)
            .coAwait()
            .firstOrNull()
    }

    override suspend fun contains(index: ULong, term: ULong): Boolean {
        val connection = jdbcPool.connection.coAwait()
        return SqlTemplate.forQuery(connection, "select count(*) from $TABLE_NAME where `index` = #{index} and  term = #{term}")
            .execute(mapOf("index" to index,"term" to term))
            .coAwait()
            .first()
            .getInteger(0) == 1
    }

    override suspend fun delete(startIndex: ULong): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun getOrNull(index: ULong): Log? {
        TODO("Not yet implemented")
    }

    override suspend fun getSubList(startIndex: ULong): List<Log> {
        TODO("Not yet implemented")
    }

    override suspend fun init(): SqlLogList {
        val connection = jdbcPool.connection.coAwait()
        connection.prepare(CREATE_TABLE_SQL).coAwait()
        return this
    }

    companion object {
        private const val TABLE_NAME = "kv_log"
        private const val CREATE_TABLE_SQL =
            """create table if not exists $TABLE_NAME(`index` integer primary key ,
               term integer not null ,
               command text not null 
                )                
            """
    }
}