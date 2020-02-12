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

import com.github.seliba.devcordbot.command.context.Context
import com.github.seliba.devcordbot.constants.Embeds
import io.github.cdimascio.dotenv.Dotenv
import io.github.rybalkinsd.kohttp.dsl.httpPost
import io.github.rybalkinsd.kohttp.ext.url
import okhttp3.Response


/**
 * JDoodle Api wrapper
 */
object JDoodle {
    private var clientId = ""
    private var clientSecret = ""
    private const val error = "Ein Fehler ist Aufgetreten"

    /**
     * Init the values for execution.
     */
    fun init(env: Dotenv) {
        clientId = env["JDOODLE_CLIENTID"].orEmpty()
        clientSecret = env["JDOODLE_CLIENTSECRET"].orEmpty()
    }

    /**
     * Execute a script from command context.
     *
     * @param context a command context.
     */
    fun execute(context: Context) {
        context.respond("Command not yet implemented.").queue()

        val lang = "goasd"

        val language: Language

        try {
            language = Language.valueOf(lang.toUpperCase())
        } catch (e: IllegalArgumentException) {
            context.respond(
                Embeds.error(
                    "Sprache nicht gefunden.",
                    "Verfügbare Sprachen: ${Language.values().joinToString(", ") { it.name.toLowerCase() }}"
                )
            ).queue()

            return
        }

        println(language.toString())
    }

    /**
     * Executes the given script in the given language
     *
     * @param language the script's language
     * @param script the script
     */
    fun execute(language: Language, script: String): Response? {
        println(script)
        return httpPost {
            url("https://api.jdoodle.com/v1/execute")

            body {
                json {
                    "clientId" to clientId
                    "clientSecret" to clientSecret
                    "script" to jsonSafeScript(script)
                    "language" to language.langString
                    "versionIndex" to language.codeInt
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