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

import com.github.devcordde.devcordbot.util.MapJsonObject
import io.github.cdimascio.dotenv.dotenv
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.Duration


/**
 * JDoodle Api wrapper
 */
object JDoodle {
    private val clientId: String
    private val clientSecret: String
    private val httpClient = OkHttpClient.Builder()
        .callTimeout(Duration.ofMinutes(2))
        .connectTimeout(Duration.ofMinutes(2))
        .readTimeout(Duration.ofMinutes(2))
        .writeTimeout(Duration.ofMinutes(2))
        .build()

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
    fun execute(language: Language, script: String): String? {
        val dataObject = MapJsonObject(
            mapOf(
                "clientId" to clientId,
                "clientSecret" to clientSecret,
                "script" to script,
                "language" to language.lang,
                "versionIndex" to language.code
            )
        )

        val bodyString = dataObject.toString()

        val request = Request.Builder()
            .url("https://api.jdoodle.com/v1/execute")
            .post(bodyString.toRequestBody("application/json".toMediaTypeOrNull())).build()

        val response = httpClient.newCall(request).execute()

        if (response.code != 200) {
            return null
        }

        return response.body?.string()
    }
}
