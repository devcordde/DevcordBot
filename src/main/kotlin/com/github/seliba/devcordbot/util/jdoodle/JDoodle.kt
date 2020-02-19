/*
 * Copyright 2020 Daniel Scherf & Michael Rittmeister
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

package com.github.seliba.devcordbot.util.jdoodle

import io.github.cdimascio.dotenv.dotenv
import net.dv8tion.jda.api.utils.data.DataObject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody


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
    fun execute(client: OkHttpClient, language: Language, script: String): String? {
        val dataObject = DataObject.empty()
        dataObject.put("clientId", clientId)
        dataObject.put("clientSecret", clientSecret)
        dataObject.put("script", script)
        dataObject.put("language", language.lang)
        dataObject.put("versionIndex", language.code)
        val bodyString = dataObject.toString()

        val request = Request.Builder()
            .url("https://api.jdoodle.com/v1/execute")
            .post(bodyString.toRequestBody("application/json".toMediaTypeOrNull())).build()

        val response = client.newCall(request).execute()

        if (response.code != 200) {
            return null
        }

        return response.body?.string()
    }
}
