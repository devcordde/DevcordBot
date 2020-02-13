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
import io.github.rybalkinsd.kohttp.ext.asString
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
        var text = context.args.raw

        if (!text.startsWith("```") && !text.endsWith("```")) {
            context.respond(
                Embeds.error("Konnte nicht evaluiert werden.", "Die Nachricht muss in einem Codeblock liegen")
            )
            return
        }

        text = text.subSequence(3, text.length - 3).toString()

        val split = text.split("\n")

        if (split.size < 2) {
            context.respond(
                Embeds.error(
                    "Kein Skript angegeben.",
                    "Benutze ein Skript"
                )
            ).queue()

            return
        }

        val language: Language

        try {
            language = Language.valueOf(split[0].toUpperCase())
        } catch (e: IllegalArgumentException) {
            context.respond(
                Embeds.error(
                    "Sprache nicht gefunden.",
                    "Verfügbare Sprachen: ${Language.values().joinToString(", ") { it.name.toLowerCase() }}"
                )
            ).queue()

            return
        }

        val response =
            execute(language, split.subList(1, split.size).joinToString("\n"))?.asString() ?: return internalError(
                context
            )

        context.respond(Embeds.success("Skript ausgeführt", response)).queue()
    }

    /**
     * Outputs an internal error
     */
    private fun internalError(context: Context) {
        context.respond(
            Embeds.error(
                "Ein interner Fehler ist aufgetreten",
                "Bei der Kommunikation mit JDoodle ist ein Fehler aufgetreten."
            )
        ).queue()
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
