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

package com.github.devcordde.devcordbot.commands.owners

import com.github.devcordde.devcordbot.command.AbstractSingleCommand
import com.github.devcordde.devcordbot.command.CommandCategory
import com.github.devcordde.devcordbot.command.CommandPlace
import com.github.devcordde.devcordbot.command.context.Context
import com.github.devcordde.devcordbot.command.permission.Permission
import com.github.devcordde.devcordbot.constants.Embeds
import io.ktor.client.request.*

/**
 * RedeployCommand.
 */
class RedeployCommand(private val host: String, private val token: String) : AbstractSingleCommand() {
    override val name: String = "redeploy"
    override val description: String = "Updatet den Bot und startet ihn neu."
    override val permission: Permission = Permission.BOT_OWNER
    override val category: CommandCategory = CommandCategory.BOT_OWNER
    override val commandPlace: CommandPlace = CommandPlace.ALL

    override suspend fun execute(context: Context) {
        val response = context.bot.httpClient.get<String>(host) {
            header("Redeploy-Token", token)
        }

        // response.status != HttpStatusCode.OK if status code is not 2xx expectStatus setting will cause it to fail
        if (response == "Hook rules were not satisfied.") {
            context.respond(
                Embeds.error("Fehler", "Der Bot konnte nicht neu gestartet werden.")
            )
            return
        }

        context.respond(Embeds.info("Erfolgreich", "Der Bot startet sich jetzt neu."))
    }
}
