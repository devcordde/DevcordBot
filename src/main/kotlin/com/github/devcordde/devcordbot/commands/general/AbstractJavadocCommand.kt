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

package com.github.devcordde.devcordbot.commands.general

import com.github.johnnyjayjay.javadox.*
import com.github.devcordde.devcordbot.command.AbstractCommand
import com.github.devcordde.devcordbot.command.CommandCategory
import com.github.devcordde.devcordbot.command.CommandPlace
import com.github.devcordde.devcordbot.command.context.Context
import com.github.devcordde.devcordbot.command.permission.Permission
import com.github.devcordde.devcordbot.constants.Embeds
import com.github.devcordde.devcordbot.dsl.EmbedConvention
import com.github.devcordde.devcordbot.dsl.embed
import com.github.devcordde.devcordbot.util.limit
import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter
import net.dv8tion.jda.api.entities.MessageEmbed

/**
 * Abstract implementation for javadoc search command.
 */
abstract class AbstractJavadocCommand() : AbstractCommand() {
    override val usage: String
        get() = "[reference]"
    override val permission: Permission = Permission.ANY
    override val category: CommandCategory = CommandCategory.GENERAL
    override val commandPlace: CommandPlace = CommandPlace.GM

    /**
     * Parses the command in [context].
     * @param url the url of the docs
     * @param docs the [Javadocs]
     */
    protected fun execute(context: Context, url: String, docs: Javadocs, query: String? = null) {
        val queryRaw = query ?: context.args.optionalArgument(0) ?: return context.respond(
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
            val methodDoc = classDoc.methods.find {
                it.name.equals(method, ignoreCase = true)
            } ?: classDoc.methods.find {
                it.name.substring(0, it.name.lastIndexOf('('))
                    .equals(
                        if ('(' in method) method.substring(0, method.lastIndexOf('(')) else method,
                        ignoreCase = true
                    )
            }
            ?: return context.respond(
                Embeds.error(
                    "Nicht gefunden",
                    "Ich konnte die Methode `$method` der klasse `$pakage.$clazz` nicht finden"
                )
            ).queue()
            return renderMethod(context, methodDoc)
        }

        renderClass(context, classDoc)
    }

    private fun renderClass(context: Context, classDoc: DocumentedType) {
        renderDoc(context, classDoc) {
            title {
                url = classDoc.uri
                title = classDoc.displayName
            }
            if (classDoc.enumConstants.isNotEmpty()) {
                addField("values", classDoc.enumConstants.joinToString("\n"))
            }

            renderTags(classDoc.topTags)
        }
    }

    private fun renderMethod(context: Context, methodDoc: DocumentedMember) {
        renderDoc(context, methodDoc) {
            title {
                url = methodDoc.uri
                title = methodDoc.name
            }
        }
    }

    private fun renderDoc(context: Context, doc: Documented, block: EmbedConvention.() -> Unit) {
        context.respond(embed {
            description = doc.description.limit(MessageEmbed.TEXT_MAX_LENGTH)
            if (doc.deprecation != null) {
                addField("Deprecated", doc.deprecation)
            }
            renderTags(doc.tags)
        }.apply(block)).queue()
    }

    private fun limit(string: String) =
        with(string.limit(MessageEmbed.VALUE_MAX_LENGTH, "")) {
            if (',' in this) {
                substring(
                    0,
                    kotlin.math.min(length, lastIndexOf(','))
                )
            } else string
        }

    private fun EmbedConvention.renderTags(tags: List<Pair<String, List<String>>>) =
        tags.forEach { (name, value) ->
            addField(name, limit(value.joinToString("\n")))
        }

    private val DocumentedType.displayName: String
        get() = "$type $name"

    private data class Reference(val pakage: String?, val clazz: String?, val method: String?)

    companion object {
        // https://regex101.com/r/JvZoYD/3
        private val classReferenceRegex = "((?:(?:[a-zA-Z0-9]+)\\.?)+)\\.([a-zA-Z0-9]+)".toRegex()

        // https://regex101.com/r/26jVyw/4
        private val referenceRegex = "((?:[a-zA-Z0-9]+\\.?)+)\\.([a-zA-Z0-9]+)(?:#|\\.)([a-zA-Z0-9(), ]+)".toRegex()

        val htmlRenderer = FlexmarkHtmlConverter.builder().build()
    }

}
