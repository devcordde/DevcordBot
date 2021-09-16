/*
 * Copyright 2021 Daniel Scherf & Michael Rittmeister & Julian König
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.github.devcordde.devcordbot.config

import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.devcordde.devcordbot.config.codecs.AnimatedGameCodec
import com.github.devcordde.devcordbot.config.codecs.SnowflakeCodec
import com.github.devcordde.devcordbot.config.codecs.UrlCodec
import com.uchuhimo.konf.BaseConfig
import com.uchuhimo.konf.Feature
import com.uchuhimo.konf.source.Source
import com.uchuhimo.konf.source.base.FlatSource
import io.github.cdimascio.dotenv.dotenv

/**
 * Loads a new Config from the .env file.
 */
fun Config(): Config {
    val objectMapper = jacksonObjectMapper().apply {
        val module = SimpleModule().apply {
            addDeserializer(SnowflakeCodec)
            addDeserializer(UrlCodec)
            addDeserializer(AnimatedGameCodec)
        }

        registerModule(module)
    }

    val dotenv = dotenv()
    val envMap = dotenv
        .entries()
        .associateBy { it.key }
        .mapValues { (_, it) -> it.value }
        .toMap()
    val konfig = BaseConfig(mapper = objectMapper).apply {
        addSpec(ConfigSpec)
    }.withSource(envMap(envMap))
    return Config(konfig)
}

private val validEnv = Regex("(\\w+)(.\\w+)*")

private fun envMap(map: Map<String, String>, nested: Boolean = true): Source {
    return FlatSource(
        map.mapKeys { (key, _) ->
            if (nested) key.replaceFirst('_', '.') else key
        }.filter { (key, _) ->
            key.matches(validEnv)
        }.toSortedMap(),
        type = "system-environment",
        allowConflict = true
    ).enabled(
        Feature.LOAD_KEYS_CASE_INSENSITIVELY
    ).disabled(
        Feature.SUBSTITUTE_SOURCE_BEFORE_LOADED
    )
}

private inline fun <reified T> SimpleModule.addDeserializer(deserializer: JsonDeserializer<T>) =
    addDeserializer(T::class.java, deserializer)
