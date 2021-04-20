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

import com.github.devcordde.devcordbot.command.AbstractRootCommand
import com.github.devcordde.devcordbot.command.AbstractSubCommand
import com.github.devcordde.devcordbot.command.CommandCategory
import com.github.devcordde.devcordbot.command.CommandPlace
import com.github.devcordde.devcordbot.command.context.Context
import com.github.devcordde.devcordbot.command.permission.Permission
import com.github.devcordde.devcordbot.constants.Embeds
import com.github.devcordde.devcordbot.constants.Emotes
import com.github.devcordde.devcordbot.dsl.editMessage
import com.github.devcordde.devcordbot.dsl.sendMessage
import com.github.devcordde.devcordbot.util.HastebinUtil
import com.github.devcordde.devcordbot.util.await
import com.github.devcordde.devcordbot.util.readSafe
import com.github.devcordde.devcordbot.util.timeout
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.exceptions.ParsingException
import net.dv8tion.jda.api.requests.restaction.CommandUpdateAction
import net.dv8tion.jda.api.utils.MarkdownSanitizer
import net.dv8tion.jda.api.utils.data.DataObject

/**
 * Eval command.
 */
class EvalCommand : AbstractRootCommand() {
    override val name: String = "eval"
    override val description: String = "Führt den angegebenen Code aus."
    override val permission: Permission = Permission.ANY
    override val category: CommandCategory = CommandCategory.GENERAL
    override val commandPlace: CommandPlace = CommandPlace.GUILD_MESSAGE

    init {
        registerCommands(ExecuteCommand())
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

    private inner class ExecuteCommand : AbstractSubCommand.Command(this) {
        override val name: String = "execute"
        override val description: String = "Führt den angegebenen Code aus."
        override val options: List<CommandUpdateAction.OptionData> = buildOptions {
            int("language", "Die Sprache in der das Codesnippet ist") {
                isRequired = true
                Language.values().forEach {
                    addChoice(it.humanReadable, it.ordinal)
                }
            }
        }

        override suspend fun execute(context: Context) {
            val origin = context.respond(
                Embeds.info(
                    "Bitte gebe Code an",
                    "Bitte sende den Code der ausgeführt werden soll in einer neuen Nachricht"
                )
            ).await()

            val language = Language.values()[context.args.int("language")]
            val script = context.readSafe()?.contentRaw ?: return origin.timeout().queue()

            origin.editMessage(
                Embeds.info(
                    "Code erhalten!",
                    "Dein Code wird nun ausgeführt."
                )
            ).queue()

            val loading = context.ack.sendMessage(
                Embeds.loading(
                    "Code wird ausgeführt.",
                    "Bitte warten"
                )
            ).await()

            val response = JDoodle.execute(language, script)
                ?: return loading.editMessage(internalError()).queue()

            val output = try {
                DataObject.fromJson(response)["output"].toString()
            } catch (p: ParsingException) {
                return loading.editMessage(internalError()).queue()
            }

            if (output.length > MessageEmbed.TEXT_MAX_LENGTH - "Ergebnis: ``````".length) {
                val result = Embeds.info(
                    "Erfolgreich ausgeführt!",
                    "Ergebnis: ${Emotes.LOADING}"
                ).toEmbedBuilder()

                loading.editMessage(result.build())

                val hasteUrl = HastebinUtil.postErrorToHastebin(output, context.bot.httpClient)
                description.replace(Emotes.LOADING.toRegex(), hasteUrl)
            } else {
                loading.editMessage(
                    Embeds.info("Erfolgreich ausgeführt!", "Ergebnis: ```$output```")
                ).queue()
            }
        }
    }

    private fun languageList() = Language.values().joinToString(
        prefix = "`",
        separator = "`, `",
        postfix = "`"
    ) { it.name.toLowerCase() }

    private inner class ListCommand : AbstractSubCommand.Command(this) {
        override val name: String = "list"
        override val description: String = "Listet die verfügbaren Sprachen auf."

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
