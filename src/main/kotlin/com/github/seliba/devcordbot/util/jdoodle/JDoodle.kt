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
import io.github.rybalkinsd.kohttp.dsl.httpPost
import io.github.rybalkinsd.kohttp.ext.url
import okhttp3.Response


/**
 * JDoodle Api wrapper
 */
object JDoodle {
    private var clientId = ""
    private var clientSecret = ""

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
    fun execute(language: Language, script: String): Response? {
        return httpPost {
            url("https://api.jdoodle.com/v1/execute")

            body {
                json {
                    "clientId" to clientId
                    "clientSecret" to clientSecret
                    "script" to jsonSafeScript(script)
                    "language" to language.lang
                    "versionIndex" to language.code
                }
            }
        }
    }

    /**
     * Removes newlines and " from a given script.
     *
     * @param script the input script.
     * @return a json safe string
     */
    private fun jsonSafeScript(script: String): String = script.replace("\n", "\\n").replace("\"", "\\\"")
}
