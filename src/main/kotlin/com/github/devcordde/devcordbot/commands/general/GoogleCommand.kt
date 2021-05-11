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

package com.github.devcordde.devcordbot.commands.general

import com.github.devcordde.devcordbot.command.CommandCategory
import com.github.devcordde.devcordbot.command.CommandPlace
import com.github.devcordde.devcordbot.command.context.Context
import com.github.devcordde.devcordbot.command.permission.Permission
import com.github.devcordde.devcordbot.command.root.AbstractSingleCommand
import com.github.devcordde.devcordbot.constants.Embeds
import com.github.devcordde.devcordbot.menu.Paginator
import dev.kord.core.behavior.interaction.PublicInteractionResponseBehavior
import dev.kord.core.event.interaction.InteractionCreateEvent
import dev.kord.rest.builder.interaction.ApplicationCommandCreateBuilder

/**
 * Google command.
 */
class GoogleCommand : AbstractSingleCommand<PublicInteractionResponseBehavior>() {

    override val name: String = "google"
    override val description: String =
        "Sucht nach der angegebenen Query bei Google und zeigt die ersten 10 Ergebnisse an."
    override val permission: Permission = Permission.ANY
    override val category: CommandCategory = CommandCategory.GENERAL
    override val commandPlace: CommandPlace = CommandPlace.GUILD_MESSAGE

    override fun ApplicationCommandCreateBuilder.applyOptions() {
        string("query", "Die Query, nach der gesucht werden soll") {
            required = true
        }
    }

    override suspend fun InteractionCreateEvent.acknowledge(): PublicInteractionResponseBehavior =
        interaction.ackowledgePublic()

    override suspend fun execute(context: Context<PublicInteractionResponseBehavior>) {
        val query = context.args.string("query")

        if (query.isBlank()) return run { context.sendHelp() }

        val results = context.bot.googler.google(query)

        if (results.isEmpty()) {
            context.respond(
                Embeds.error(
                    title = "Keine Suchergebnisse gefunden",
                    description = "Für die Anfrage `$query` konnten keine Ergebnisse gefunden werden."
                )
            )
            return
        }

        val displayResults = results.map {
            "**${it.title}**\n\"${it.snippet?.replace("\n", "") ?: "..."}\"\n[${it.displayLink}](${it.link})"
        }
        Paginator(
            items = displayResults, itemsPerPage = 1, title = "Suchergebnisse",
            context = context, user = context.author.asUser(), timeoutMillis = 25 * 1000
        )
    }
}
