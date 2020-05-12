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

package com.github.seliba.devcordbot.commands.general

import com.github.seliba.devcordbot.command.AbstractCommand
import com.github.seliba.devcordbot.command.CommandCategory
import com.github.seliba.devcordbot.command.context.Context
import com.github.seliba.devcordbot.command.permission.Permission
import com.github.seliba.devcordbot.constants.Embeds
import com.github.seliba.devcordbot.menu.Paginator
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.customsearch.Customsearch

class GoogleCommand(private val apiKey: String, private val engineId: String) : AbstractCommand() {

    private val search = Customsearch(GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory(), null)

    override val aliases: List<String> = listOf("google", "search", "g")
    override val displayName: String = "google"
    override val description: String = "Sucht nach der angegebenen Query bei Google und zeigt das erste Ergebnis."
    override val usage: String = "<query>"
    override val permission: Permission = Permission.ANY
    override val category: CommandCategory = CommandCategory.GENERAL

    override suspend fun execute(context: Context) {
        val query = context.args.join()
        val results = with(search.cse().list(query)) {
            key = apiKey
            cx = engineId
            execute()
        }.items

        if (results.isEmpty()) {
            context.respond(
                Embeds.error(
                    title = "Keine Suchergebnisse gefunden",
                    description = "Für die Anfrage `$query` konnten keine Ergebnisse gefunden werden."
                )
            )
        } else {
            val displayResults = results.map {
                "**${it.title}**\n\"${it.snippet}\"\n[${it.displayLink}](${it.link})"
            }
            Paginator(
                items = displayResults, itemsPerPage = 1, title = "Suchergebnisse",
                context = context, user = context.author, timeoutMillis = 25 * 1000
            )
        }
    }
}
