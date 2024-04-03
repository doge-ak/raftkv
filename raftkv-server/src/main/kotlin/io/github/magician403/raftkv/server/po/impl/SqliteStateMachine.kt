package io.github.magician403.raftkv.server.po.impl

import io.github.magician403.raftkv.server.po.SqlStateMachine
import io.vertx.core.Vertx
import io.vertx.jdbcclient.JDBCPool
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.kotlin.coroutines.dispatcher
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.templates.SqlTemplate
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext

@Suppress("ConstPropertyName")
class SqliteStateMachine(
    private val vertx: Vertx
) : SqlStateMachine, CoroutineScope {
    override val coroutineContext: CoroutineContext = vertx.dispatcher()
    private val jdbcPool: JDBCPool = vertx.orCreateContext.getLocal("jdbcPool")

    companion object {
        private const val tableName = "state_machine"
        private const val createTableSql =
            "create table if not exists $tableName(key text primary key, value text)"
    }


    override suspend fun init(): SqlStateMachine {
        jdbcPool.connection.coAwait()
            .prepare(createTableSql).coAwait()
        return this
    }

    override suspend fun entries(): Map<String, String> {
        val connection = jdbcPool.connection.coAwait()
        return SqlTemplate.forQuery(connection, "select key, value from $tableName")
            .mapTo(Row::toJson)
            .execute(null)
            .coAwait()
            .associate { Pair(it.getString("key"), it.getString("value")) }
    }

    override suspend fun keys(): Set<String> {
        val connection = jdbcPool.connection.coAwait()
        return SqlTemplate.forQuery(connection, "select key from $tableName")
            .mapTo(Row::toJson)
            .execute(null)
            .coAwait()
            .mapTo(HashSet()) { it.getString("key") }
    }

    override suspend fun values(): Collection<String> {
        val connection = jdbcPool.connection.coAwait()
        return SqlTemplate.forQuery(connection, "select value from $tableName")
            .mapTo(Row::toJson)
            .execute(null)
            .coAwait()
            .mapTo(ArrayList()) { it.getString("value") }
    }

    override suspend fun size(): Int {
        val connection = jdbcPool.connection.coAwait()
        return SqlTemplate.forQuery(connection, "select count(*) from $tableName")
            .execute(null)
            .coAwait()
            .first()
            .getInteger(0)
    }

    override suspend fun clear() {
        val connection = jdbcPool.connection.coAwait()
        @Suppress("SqlWithoutWhere")
        SqlTemplate.forQuery(connection, "delete from $tableName")
            .execute(null)
            .coAwait()
    }

    override suspend fun isEmpty(): Boolean {
        return size() == 0
    }

    override suspend fun remove(key: String) {
        val connection = jdbcPool.connection.coAwait()
        SqlTemplate.forQuery(connection, "delete from $tableName where key = #{key}")
            .execute(mapOf("key" to key))
            .coAwait()
    }

    override suspend fun putAll(map: Map<String, String>) {
        val connection = jdbcPool.connection.coAwait()
        SqlTemplate.forUpdate(connection, "insert into $tableName values(#{key}, #{value})")
            .executeBatch(map.entries.map { mapOf("key" to it.key, "value" to it.value) })
            .coAwait()
    }

    override suspend fun put(key: String, value: String) {
        val connection = jdbcPool.connection.coAwait()
        SqlTemplate.forUpdate(connection, "insert into $tableName values(#{key},#{value})")
            .execute(mapOf("key" to key, "value" to value))
            .coAwait()
    }

    override suspend fun get(key: String): String? {
        val connection = jdbcPool.connection.coAwait()
        return SqlTemplate.forQuery(connection, "select value from $tableName where key = #{key}")
            .execute(mapOf("key" to key))
            .coAwait()
            .firstOrNull()
            ?.getString("value")
    }

    override suspend fun containsKey(key: String): Boolean {
        val connection = jdbcPool.connection.coAwait()
        return SqlTemplate.forQuery(connection, "select count(*) from $tableName where key = #{key}")
            .execute(mapOf("key" to key))
            .coAwait()
            .first()
            .getInteger(0) != 0
    }

    override suspend fun containsValue(value: String): Boolean {
        val connection = jdbcPool.connection.coAwait()
        return SqlTemplate.forQuery(connection, "select count(*) from $tableName where value = #{value}")
            .execute(mapOf("value" to value))
            .coAwait()
            .first()
            .getInteger(0) != 0
    }
}