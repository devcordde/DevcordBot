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

package com.github.seliba.devcordbot.commands.general.jdoodle

import com.github.seliba.devcordbot.command.AbstractCommand
import com.github.seliba.devcordbot.command.AbstractSubCommand
import com.github.seliba.devcordbot.command.CommandCategory
import com.github.seliba.devcordbot.command.context.Context
import com.github.seliba.devcordbot.command.permission.Permission
import com.github.seliba.devcordbot.constants.Embeds
import com.github.seliba.devcordbot.dsl.editMessage
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.exceptions.ParsingException
import net.dv8tion.jda.api.requests.restaction.MessageAction
import net.dv8tion.jda.api.utils.MarkdownSanitizer
import net.dv8tion.jda.api.utils.data.DataObject

/**
 * Eval command.
 */
class EvalCommand : AbstractCommand() {
    override val aliases: List<String> = listOf("eval", "exec", "execute", "run")
    override val displayName: String = "eval"
    override val description: String = "Führt den angegebenen Code aus."
    override val usage: String = MarkdownSanitizer.escape("\n```<language>\n<code>\n```")
    override val permission: Permission = Permission.ANY
    override val category: CommandCategory = CommandCategory.GENERAL

    init {
        registerCommands(ListCommand())
    }

    private fun example(title: String) = Embeds.error(
        title,
        "`Beispiel`\n${MarkdownSanitizer.escape("```kotlin\nfun main() = print(\"Hello World\")\n```")}"
    )

    private fun internalError() = Embeds.error(
        "Ein interner Fehler ist aufgetreten",
        "Bei der Kommunikation mit JDoodle ist ein Fehler aufgetreten."
    )

    override fun execute(context: Context) {
        context.respond(Embeds.loading("Läd.", "Skript wird ausgeführt.")).flatMap(fun(it: Message): MessageAction {
            val text = context.args.join()

            if (!text.startsWith("```") && !text.endsWith("```")) {
                return it.editMessage(example("Das Skript muss in einem Multiline-Codeblock liegen"))
            }

            val split = text.substring(3, text.length - 3).split("\n")
            val scriptEmpty = split.subList(1, split.size).joinToString("").trim().isEmpty()

            if (split.size < 2 || scriptEmpty) {
                return it.editMessage(example("Benutze ein Skript"))
            }

            val languageString = split.first()

            val language = try {
                Language.valueOf(languageString.toUpperCase())
            } catch (e: IllegalArgumentException) {
                return it.editMessage(
                    Embeds.error(
                        "Sprache `$languageString` nicht gefunden. Verfügbare Sprachen",
                        languageList()
                    )
                )
            }

            val script = split.subList(1, split.size).joinToString("\n")

            val response = JDoodle.execute(context.jda.httpClient, language, script)
                ?: return it.editMessage(internalError())

            val output = try {
                DataObject.fromJson(response)["output"].toString()
            } catch (p: ParsingException) {
                return it.editMessage(internalError())
            }

            return it.editMessage(
                Embeds.success(
                    "Skript ausgeführt",
                    "Sprache: `${language.humanReadable}`\nSkript:${text}Output:\n```$output```"
                )
            )
        }).queue()
    }

    private fun languageList() = Language.values().joinToString(
        prefix = "`",
        separator = "`, `",
        postfix = "`"
    ) { it.name.toLowerCase() }

    private inner class ListCommand : AbstractSubCommand(this) {
        override val aliases: List<String> = listOf("list", "ls")
        override val displayName: String = "list"
        override val description: String = "Listet die verfügbaren Sprachen auf."
        override val usage: String = ""

        override fun execute(context: Context) {
            return context.respond(
                Embeds.info(
                    "Verfügbare Sprachen",
                    languageList()
                )
            ).queue()
        }
    }
}
