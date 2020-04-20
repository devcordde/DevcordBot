/*
 * Copyright 2020 Daniel Scherf & Michael Rittmeister & Julian König
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
import com.github.seliba.devcordbot.constants.Constants
import com.github.seliba.devcordbot.constants.Embeds
import com.github.seliba.devcordbot.constants.Emotes
import com.github.seliba.devcordbot.dsl.editMessage
import com.github.seliba.devcordbot.util.HastebinUtil
import com.github.seliba.devcordbot.util.await
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.exceptions.ParsingException
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

    override suspend fun execute(context: Context) {
        val message = context.respond(Embeds.loading("Lädt.", "Skript wird ausgeführt.")).await()
        val text = context.args.join()

        val blockMatch = Constants.CODE_BLOCK_REGEX.matchEntire(text)

        if (blockMatch == null || blockMatch.groups.size < 2) {
            return message.editMessage(example("Das Skript muss in einem Multiline-Codeblock liegen")).queue()
        }

        val languageString = blockMatch.groupValues[1]
        val script = blockMatch.groupValues[2].trim()

        if (script.isEmpty()) {
            return message.editMessage(example("Benutze ein Skript")).queue()
        }

        val language = try {
            Language.valueOf(languageString.toUpperCase())
        } catch (e: IllegalArgumentException) {
            return message.editMessage(
                Embeds.error(
                    "Sprache `$languageString` nicht gefunden. Verfügbare Sprachen",
                    languageList()
                )
            ).queue()
        }

        val response = JDoodle.execute(language, script)
            ?: return message.editMessage(internalError()).queue()

        val output = try {
            DataObject.fromJson(response)["output"].toString()
        } catch (p: ParsingException) {
            return message.editMessage(internalError()).queue()
        }

        if (output.length > MessageEmbed.TEXT_MAX_LENGTH - "Ergebnis: ``````".length) {
            val result = Embeds.info(
                "Zu langes Ergebnis!",
                "Ergebnis: ${Emotes.LOADING}"
            )

            message.editMessage(result)

            HastebinUtil.postErrorToHastebin(output, context.bot.httpClient).thenAccept { hasteUrl ->
                message.editMessage(result.apply {
                    @Suppress("ReplaceNotNullAssertionWithElvisReturn") // Description is set above
                    description = description!!.replace(Emotes.LOADING.toRegex(), hasteUrl)
                }).queue()
            }
        } else {
            message.editMessage(Embeds.info("Erfolgreich ausgeführt!", "Ergebnis: ```$output```")).queue()
        }
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

        override suspend fun execute(context: Context) {
            return context.respond(
                Embeds.info(
                    "Verfügbare Sprachen",
                    languageList()
                )
            ).queue()
        }
    }
}
