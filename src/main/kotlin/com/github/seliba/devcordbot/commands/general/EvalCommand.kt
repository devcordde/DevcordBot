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

import com.github.seliba.devcordbot.command.AbstractCommand
import com.github.seliba.devcordbot.command.AbstractSubCommand
import com.github.seliba.devcordbot.command.CommandCategory
import com.github.seliba.devcordbot.command.context.Context
import com.github.seliba.devcordbot.command.perrmission.Permission
import com.github.seliba.devcordbot.constants.Embeds
import com.github.seliba.devcordbot.dsl.editMessage
import com.github.seliba.devcordbot.util.jdoodle.JDoodle
import com.github.seliba.devcordbot.util.jdoodle.Language
import io.github.rybalkinsd.kohttp.ext.asString
import net.dv8tion.jda.api.utils.data.DataObject

/**
 * Eval command.
 */
class EvalCommand : AbstractCommand() {
    override val aliases: List<String> = listOf("eval", "exec", "execute", "run")
    override val displayName: String = "eval"
    override val description: String = "Führt den angegebenen Code aus."
    override val usage: String = "[language] \n <code>"
    override val permission: Permission = Permission.ANY
    override val category: CommandCategory = CommandCategory.GENERAL

    init {
        registerCommands(ListCommand())
    }

    override fun execute(context: Context) {
        context.respond(Embeds.loading("Läd.", "Skript wird ausgeführt.")).flatMap {
            val text = context.args.raw

            if (!text.startsWith("```") && !text.endsWith("```")) {
                return@flatMap it.editMessage(
                    Embeds.error(
                        "Konnte nicht evaluiert werden.",
                        "Die Nachricht muss in einem Multiline-Codeblock liegen"
                    )
                )
            }

            val split = text.substring(3, text.length - 3).split("\n")

            if (split.size < 2) {
                return@flatMap it.editMessage(
                    Embeds.error(
                        "Kein Skript angegeben.",
                        "Benutze ein Skript"
                    )
                )
            }

            val languageString = split.first()

            val language = try {
                Language.valueOf(languageString.toUpperCase())
            } catch (e: IllegalArgumentException) {
                return@flatMap it.editMessage(
                    Embeds.error(
                        "Sprache `$languageString` nicht gefunden. Verfügbare Sprachen",
                        Language.values().joinToString(", ") { name.toLowerCase() }
                    )
                )
            }

            val script = split.subList(1, split.size).joinToString("\n")

            val response = JDoodle.execute(language, script)?.asString()
                ?: return@flatMap it.editMessage(
                    Embeds.error(
                        "Ein interner Fehler ist aufgetreten",
                        "Bei der Kommunikation mit JDoodle ist ein Fehler aufgetreten."
                    )
                )

            val output = DataObject.fromJson(response)["output"].toString()

            it.editMessage(
                Embeds.success(
                    "Skript ausgeführt",
                    "Sprache: ${language.humanReadable}\nSkript:```$script```Output:\n```$output```"
                )
            )
        }.queue()
    }

    private inner class ListCommand : AbstractSubCommand(this) {
        override val aliases: List<String> = listOf("list", "ls")
        override val displayName: String = "list"
        override val description: String = "Listet die verfügbaren Sprachen auf."
        override val usage: String = ""

        override fun execute(context: Context) {
            return context.respond(Embeds.info(
                "Verfügbare Sprachen",
                Language.values().joinToString(", ") { it.name.toLowerCase() }
            )).queue()
        }

    }
}

