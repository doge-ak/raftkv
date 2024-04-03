package io.github.magician403.raftkv.server.po

/**
 * 基于sql的MutableMap
 */
interface SqlMutableMap<K, V> {
    suspend fun entries(): Map<K, V>

    suspend fun keys(): Set<K>

    suspend fun values(): Collection<V>

    suspend fun size(): Int

    suspend fun clear()

    suspend fun isEmpty(): Boolean

    suspend fun remove(key: K)

    suspend fun putAll(map: Map<K, V>)

    suspend fun put(key: K, value: V)

    suspend fun get(key: K): V?

    suspend fun containsKey(key: K): Boolean

    suspend fun containsValue(value: V): Boolean

    suspend fun init(): SqlMutableMap<K, V>
}