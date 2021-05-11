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

package com.github.devcordde.devcordbot.commands.general

import com.github.devcordde.devcordbot.command.context.Context
import com.github.devcordde.devcordbot.constants.Embeds
import com.github.johnnyjayjay.javadox.JavadocParser
import com.github.johnnyjayjay.javadox.Javadocs
import org.jsoup.Jsoup

/**
 * Generic javadoc command.
 */
class JavadocCommand : AbstractJavadocCommand() {
    override val displayName: String = "Javadoc"
    override val aliases: List<String> = listOf("javadoc", "jdoc")
    override val usage: String = "[type] [version] <query>"
    override val description: String = "Erlaubt dir andere Versionen von Docs zu benutzen"

    override suspend fun execute(context: Context) {
        val args = context.args
        val (_, version, query) = when (args.size) {
            0 -> return context.sendHelp().queue()
            1 -> RequestContainer(query = args.join(), version = defaultVersionForType("java"))
            2 -> {
                val type = parseType(args.first(), context) ?: return
                RequestContainer(type, defaultVersionForType(type), query = args[1])
            }
            else -> {
                val type = parseType(args.first(), context) ?: return
                val version = DocumentedVersion.values().firstOrNull { it.humanName == args[1] }
                    ?: return context.respond(Embeds.info("Ungültige Version!", "Bitte gib eine gültige Version an"))
                        .queue()
                if (version.docType == type) {
                    return context.respond(
                        Embeds.error(
                            "Inkompatible Version!",
                            "Diese Version ist nicht mit dem angebeenen Typ kompatibel"
                        )
                    ).queue()
                }
                RequestContainer(type, version, args[2])
            }
        }

        val parser = JavadocParser(htmlRenderer::convert)

        val docs = Javadocs(tree = version.url, parser = parser) {
            Jsoup.connect(it).userAgent("Mozilla").get()
        }

        execute(context, version.url, docs, query)
    }

    private fun parseType(input: String, context: Context): String? {
        return if (input.equals("java", ignoreCase = true) or input.equals("spigot", ignoreCase = true)) {
            input
        } else {
            context.respond(
                Embeds.error(
                    "Ungültiger typ",
                    "Du kannst entweder `java` oder `spigot` als Typ angeben"
                )
            ).queue()
            null
        }
    }

    private fun defaultVersionForType(type: String): DocumentedVersion {
        return if (type.equals(
                "java",
                ignoreCase = true
            )
        ) DocumentedVersion.V_10 else DocumentedVersion.V_1_15_2
    }

    private data class RequestContainer(
        val type: String = "java",
        val version: DocumentedVersion,
        val query: String
    )
}
