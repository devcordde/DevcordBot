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

package com.github.seliba.devcordbot.commands.general

import com.github.johnnyjayjay.javadox.DocumentedMember
import com.github.johnnyjayjay.javadox.DocumentedType
import com.github.johnnyjayjay.javadox.JavadocParser
import com.github.johnnyjayjay.javadox.Javadocs
import com.github.seliba.devcordbot.command.AbstractCommand
import com.github.seliba.devcordbot.command.CommandCategory
import com.github.seliba.devcordbot.command.context.Context
import com.github.seliba.devcordbot.command.permission.Permission
import com.github.seliba.devcordbot.constants.Embeds
import com.github.seliba.devcordbot.dsl.EmbedConvention
import com.github.seliba.devcordbot.dsl.embed
import com.github.seliba.devcordbot.util.limit
import net.dv8tion.jda.api.entities.MessageEmbed
import org.jsoup.Jsoup

/**
 * Abstract implementation for javadoc search command.
 * @property url the url of the javadoc
 */
abstract class JavadocCommand(private val url: String) : AbstractCommand() {
    override val usage: String
        get() = "[reference]"
    override val permission: Permission = Permission.ANY
    override val category: CommandCategory = CommandCategory.GENERAL

    private val parser: JavadocParser = JavadocParser()
    private val docs: Javadocs = Javadocs(allClasses = url, parser = parser) {
        Jsoup.connect(it).userAgent("Mozilla").get()
    }

    override fun execute(context: Context) {
        val queryRaw = context.args.optionalArgument(0) ?: return context.respond(
            Embeds.info(
                "Javadocs",
                "Die javadocs findest du [hier]($url)"
            )
        ).queue()
        val classReference = classReferenceRegex.matchEntire(queryRaw)
        // I srsly could not think of a better name than pakage
        val (pakage, clazz, method) = if (classReference != null) {
            Reference(classReference.groupValues[1], classReference.groupValues[2], null)
        } else {
            val genericReference = referenceRegex.matchEntire(queryRaw)
            if (genericReference != null) {
                Reference(
                    genericReference.groupValues[1],
                    genericReference.groupValues[2],
                    genericReference.groupValues[3]
                )

            } else {
                Reference(null, null, null)
            }
        }

        if (pakage == null || clazz == null) {
            return context.respond(
                Embeds.error(
                    "Ungültige Referenz",
                    "Bitte gebe eine gültige Referenz an: `java.util.List#add()`"
                )
            ).queue()
        }

        // ClassDoc:TM:
        val classDoc = docs.find(pakage, clazz).firstOrNull() ?: return context.respond(
            Embeds.error("Nicht gefunden", "Es konnte kein javadoc für `$queryRaw` gefunden werden")
        ).queue()

        if (method != null) {
            val methodDoc = classDoc.methods.find { it.name.equals(method, ignoreCase = true) }
                ?: return context.respond(
                    Embeds.error(
                        "Nicht gefunden",
                        "Ich konnte die Methode `$method` der klasse `$pakage$clazz` nicht finden"
                    )
                ).queue()
            return renderMethod(context, methodDoc)
        }

        renderClass(context, classDoc)
    }

    private fun renderClass(context: Context, classDoc: DocumentedType) {
        renderDoc(context, embed {
            title {
                url = classDoc.uri
                title = classDoc.declaration
            }
            description = classDoc.description.limit(MessageEmbed.TEXT_MAX_LENGTH)
            if (classDoc.enumConstants.isNotEmpty()) {
                addField("values", classDoc.enumConstants.joinToString("\n"))
            }
            if(classDoc.deprecation != null) {
                addField("Deprecated", classDoc.deprecation)
            }
        })
    }

    private fun renderMethod(context: Context, methodDoc: DocumentedMember) {
        renderDoc(context, embed {
            title {
                url = methodDoc.uri
                title = methodDoc.declaration
            }
            description = methodDoc.description.limit(MessageEmbed.TEXT_MAX_LENGTH)
        })
    }

    private fun renderDoc(context: Context, embed: EmbedConvention) = context.respond(embed).queue()

    companion object {
        // https://regex101.com/r/JvZoYD/3
        private val classReferenceRegex = "((?:(?:[a-zA-Z0-9]+)\\.?)+)\\.([a-zA-Z0-9]+)".toRegex()

        // https://regex101.com/r/26jVyw/2
        private val referenceRegex = "((?:[a-zA-Z0-9]+\\.?)+)\\.([a-zA-Z0-9]+)(?:#|\\.)([a-zA-Z0-9]+)".toRegex()
    }

    private data class Reference(val pakage: String?, val clazz: String?, val method: String?)
}