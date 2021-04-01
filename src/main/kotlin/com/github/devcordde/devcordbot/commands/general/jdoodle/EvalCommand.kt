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

package com.github.devcordde.devcordbot.commands.general.jdoodle

import com.github.devcordde.devcordbot.command.AbstractCommand
import com.github.devcordde.devcordbot.command.AbstractSubCommand
import com.github.devcordde.devcordbot.command.CommandCategory
import com.github.devcordde.devcordbot.command.CommandPlace
import com.github.devcordde.devcordbot.command.context.Context
import com.github.devcordde.devcordbot.command.permission.Permission
import com.github.devcordde.devcordbot.constants.Constants
import com.github.devcordde.devcordbot.constants.Embeds
import com.github.devcordde.devcordbot.constants.Emotes
import com.github.devcordde.devcordbot.util.HastebinUtil
import com.github.devcordde.devcordbot.util.await
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.exceptions.ParsingException
import net.dv8tion.jda.api.requests.restaction.CommandUpdateAction
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
    override val commandPlace: CommandPlace = CommandPlace.GUILD_MESSAGE
    override val options: List<CommandUpdateAction.OptionData> = buildOptions {
        string("codeblock", "Der auszuführende Codeblock")
    }

    init {
//        registerCommands(ListCommand())
    }

    private fun example(title: String) = Embeds.error(
        title,
        "`Beispiel`\n${MarkdownSanitizer.escape("```kotlin\nfun main() = print(\"Hello World\")\n```")}"
    ).toEmbedBuilder().build()

    private fun internalError() = Embeds.error(
        "Ein interner Fehler ist aufgetreten",
        "Bei der Kommunikation mit JDoodle ist ein Fehler aufgetreten."
    ).toEmbedBuilder().build()

    override suspend fun execute(context: Context) {
        val message = context.respond(Embeds.loading("Lädt.", "Skript wird ausgeführt.")).await()
        val text = context.args.string("codeblock")

        val blockMatch = Constants.JDOODLE_REGEX.matchEntire(text)

        if (blockMatch == null || blockMatch.groups.size != 3) {
            return context.ack.editOriginal(example("Das Skript muss in einem Multiline-Codeblock liegen")).queue()
        }

        val languageString = blockMatch.groupValues[1]
        val script = blockMatch.groupValues[2].trim()

        if (script.isEmpty()) {
            return context.ack.editOriginal(example("Benutze ein Skript")).queue()
        }

        val language = try {
            Language.valueOf(languageString.toUpperCase())
        } catch (e: IllegalArgumentException) {
            return context.ack.editOriginal(
                Embeds.error(
                    "Sprache `$languageString` nicht gefunden. Verfügbare Sprachen",
                    languageList()
                ).toEmbedBuilder().build()
            ).queue()
        }

        val response = JDoodle.execute(language, script)
            ?: return context.ack.editOriginal(internalError()).queue()

        val output = try {
            DataObject.fromJson(response)["output"].toString()
        } catch (p: ParsingException) {
            return context.ack.editOriginal(internalError()).queue()
        }

        if (output.length > MessageEmbed.TEXT_MAX_LENGTH - "Ergebnis: ``````".length) {
            val result = Embeds.info(
                "Erfolgreich ausgeführt!",
                "Ergebnis: ${Emotes.LOADING}"
            ).toEmbedBuilder()

            context.ack.editOriginal(result.build())

            HastebinUtil.postErrorToHastebin(output, context.bot.httpClient).thenAccept { hasteUrl ->
                context.ack.editOriginal(result.apply {
                    @Suppress("ReplaceNotNullAssertionWithElvisReturn") // Description is set above
                    setDescription(description.replace(Emotes.LOADING.toRegex(), hasteUrl))
                }.build()).queue()
            }
        } else {
            context.ack.editOriginal(Embeds.info("Erfolgreich ausgeführt!", "Ergebnis: ```$output```").toEmbedBuilder().build()).queue()
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
