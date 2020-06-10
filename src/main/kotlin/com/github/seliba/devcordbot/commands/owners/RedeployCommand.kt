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

package com.github.seliba.devcordbot.commands.owners

import com.github.seliba.devcordbot.command.AbstractCommand
import com.github.seliba.devcordbot.command.CommandCategory
import com.github.seliba.devcordbot.command.CommandPlace
import com.github.seliba.devcordbot.command.context.Context
import com.github.seliba.devcordbot.command.permission.Permission
import com.github.seliba.devcordbot.constants.Embeds
import okhttp3.Request

/**
 * RedeployCommand.
 */
class RedeployCommand(private val host: String, private val token: String) : AbstractCommand() {
    override val aliases: List<String> = listOf("redeploy")
    override val displayName: String = "redeploy"
    override val description: String = "Erlaubt dem Bot sich zu updaten und neu zu starten."
    override val usage: String = ""
    override val permission: Permission = Permission.BOT_OWNER
    override val category: CommandCategory = CommandCategory.BOT_OWNER
    override val commandPlace: CommandPlace = CommandPlace.ALL

    override suspend fun execute(context: Context) {
        val request = Request.Builder().url(host).addHeader("Redeploy-Token", token).build()
        val response = context.bot.httpClient.newCall(request).execute()

        if (response.code != 200 || response.body?.string().equals("Hook rules were not satisfied.")) {
            return context.respond(
                Embeds.error("Fehler", "Der Bot konnte nicht neu gestartet werden.")
            ).queue()
        }

        return context.respond(Embeds.info("Erfolgreich", "Der Bot startet sich jetzt neu.")).queue()
    }
}
