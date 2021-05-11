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

import com.github.devcordde.devcordbot.command.AbstractSubCommand
import com.github.devcordde.devcordbot.command.CommandCategory
import com.github.devcordde.devcordbot.command.CommandPlace
import com.github.devcordde.devcordbot.command.context.Context
import com.github.devcordde.devcordbot.command.permission.Permission
import com.github.devcordde.devcordbot.command.root.AbstractRootCommand
import com.github.devcordde.devcordbot.constants.Embeds
import com.github.devcordde.devcordbot.constants.Emotes
import com.github.devcordde.devcordbot.constants.TEXT_MAX_LENGTH
import com.github.devcordde.devcordbot.util.HastebinUtil
import com.github.devcordde.devcordbot.util.readSafe
import com.github.devcordde.devcordbot.util.timeout
import dev.kord.core.behavior.interaction.InteractionResponseBehavior
import dev.kord.core.event.interaction.InteractionCreateEvent
import dev.kord.rest.builder.interaction.SubCommandBuilder
import java.util.*

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

    private inner class ExecuteCommand : AbstractSubCommand.Command<InteractionResponseBehavior>(this) {
        override val name: String = "execute"
        override val description: String = "Führt den angegebenen Code aus."

        override suspend fun InteractionCreateEvent.acknowledge(): InteractionResponseBehavior =
            interaction.ackowledgePublic()

        override fun SubCommandBuilder.applyOptions() {
            int("language", "Die Sprache, in der das Codesnippet ist") {
                required = true
                Language.values().forEach {
                    choice(it.humanReadable, it.ordinal)
                }
            }
        }

        override suspend fun execute(context: Context<InteractionResponseBehavior>) {
            val origin = context.respond(
                Embeds.info(
                    "Bitte gebe Code an",
                    "Bitte sende den Code, der ausgeführt werden soll, in einer neuen Nachricht"
                )
            )

            val language = Language.values()[context.args.int("language")]
            val script = context.readSafe()?.content ?: return run { origin.timeout() }

            origin.edit(
                Embeds.info(
                    "Code erhalten!",
                    "Dein Code wird nun ausgeführt."
                )
            )

            origin.edit(
                Embeds.loading(
                    "Code wird ausgeführt.",
                    "Bitte warten..."
                )
            )

            val (output) = JDoodle.execute(context.bot, language, script)

            if (output.length > TEXT_MAX_LENGTH - "Ausgabe: ``````".length) {
                val result = Embeds.info(
                    "Erfolgreich ausgeführt!",
                    "Ausgabe: ${Emotes.LOADING}"
                )

                origin.edit(result)

                val hasteUrl = HastebinUtil.postToHastebin(output, context.bot.httpClient)
                description.replace(Emotes.LOADING.toRegex(), hasteUrl)
            } else {
                origin.edit(
                    Embeds.info("Erfolgreich ausgeführt!", "Ausgabe: ```$output```")
                )
            }
        }
    }

    private fun languageList() = Language.values().joinToString(
        prefix = "`",
        separator = "`, `",
        postfix = "`"
    ) { it.name.lowercase(Locale.getDefault()) }

    private inner class ListCommand : AbstractSubCommand.Command<InteractionResponseBehavior>(this) {
        override val name: String = "list"
        override val description: String = "Listet die verfügbaren Programmiersprachen auf."

        override suspend fun InteractionCreateEvent.acknowledge(): InteractionResponseBehavior =
            interaction.ackowledgePublic()

        override suspend fun execute(context: Context<InteractionResponseBehavior>) {
            context.respond(
                Embeds.info(
                    "Verfügbare Sprachen",
                    languageList()
                )
            )
        }
    }
}
