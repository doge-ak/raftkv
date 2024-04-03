package io.github.magician403.raftkv.client

import io.vertx.core.Future

interface StateMachine {
    fun entries(): Future<Map<String, String>>
    fun keys(): Future<Set<String>>
    fun values(): Future<Collection<String>>
    fun size(): Future<Int>
    fun clear(): Future<Unit>
    fun isEmpty(): Future<Boolean>
    fun remove(key: String): Future<Unit>
    fun putAll(map: Map<String, String>): Future<Unit>
    fun put(key: String, value: String): Future<Unit>
    fun get(key: String): Future<String?>
    fun containsKey(key: String): Future<Boolean>
    fun containsValue(value: String): Future<Boolean>
}