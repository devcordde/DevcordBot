/*
 * Copyright 2020 Daniel Scherf & Michael Rittmeister & Julian König
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

import io.github.cdimascio.dotenv.dotenv
import io.ktor.client.*
import io.ktor.client.request.*
import kotlinx.serialization.Serializable


/**
 * JDoodle Api wrapper
 */
object JDoodle {
    private val clientId: String
    private val clientSecret: String

    /**
     * Init the values for execution.
     */
    init {
        val env = dotenv()
        clientId = env["JDOODLE_CLIENTID"].orEmpty()
        clientSecret = env["JDOODLE_CLIENTSECRET"].orEmpty()
    }

    /**
     * Executes the given script in the given language
     *
     * @param language the script's language
     * @param script the script
     */
    suspend fun execute(httpClient: HttpClient, language: Language, script: String): JDoodleResponse {
        return httpClient.post("https://api.jdoodle.com/v1/execute") {
            body = JDoodleRequest(
                clientId,
                clientSecret,
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

@Serializable
data class JDoodleResponse(val output: String)
