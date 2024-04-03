package io.github.magician403.raftkv.server.config

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.vertx.core.json.jackson.DatabindCodec

class JacksonConfig {
    fun init() {
        val objectMapper = DatabindCodec.mapper()
        val javaTimeModule = JavaTimeModule()
        objectMapper.registerModule(javaTimeModule)
        objectMapper.registerKotlinModule()
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
    }
}