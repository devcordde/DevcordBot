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

package com.github.devcordde.devcordbot.commands.general.jdoodle

import com.github.devcordde.devcordbot.core.DevCordBot
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime

/**
 * JDoodle Api wrapper
 */
object JDoodle {

    /**
     * Executes the given script in the given language
     *
     * @param language the script's language
     * @param script the script
     */
    @OptIn(ExperimentalTime::class)
    suspend fun execute(bot: DevCordBot, language: Language, script: String): JDoodleResponse {
        val config = bot.config.jdoodle
        val httpClient = bot.httpClient
        return httpClient.post("https://api.jdoodle.com/v1/execute") {
            contentType(ContentType.Application.Json)

            timeout {
                requestTimeoutMillis = 2.minutes.inWholeMilliseconds
                socketTimeoutMillis = 2.minutes.inWholeMilliseconds
            }

            body = JDoodleRequest(
                config.clientId,
                config.clientSecret,
                script,
                language.lang,
                language.code
            )
        }
    }
}

@Serializable
private data class JDoodleRequest(
    val clientId: String,
    val clientSecret: String,
    val script: String,
    val language: String,
    val versionIndex: Int
)

/**
 * Response from a JDoodle execute request.
 *
 * @property output the output of the code executed
 */
@Serializable
data class JDoodleResponse(val output: String)
