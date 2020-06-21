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

package com.github.devcordde.devcordbot.commands.moderation

import com.github.devcordde.devcordbot.command.AbstractCommand
import com.github.devcordde.devcordbot.command.CommandCategory
import com.github.devcordde.devcordbot.command.CommandPlace
import com.github.devcordde.devcordbot.command.context.Context
import com.github.devcordde.devcordbot.command.permission.Permission
import com.github.devcordde.devcordbot.constants.Embeds

/**
 * KickCommand.
 */
class KickCommand : AbstractCommand() {
    override val aliases: List<String> = listOf("kick")
    override val displayName: String = "kick"
    override val description: String = "Kickt einen User."
    override val usage: String = "<user> <reason>"
    override val permission: Permission = Permission.MODERATOR
    override val category: CommandCategory = CommandCategory.MODERATION
    override val commandPlace: CommandPlace = CommandPlace.GM

    override suspend fun execute(context: Context) {
        val member = context.args.member(0, context = context) ?: return
        val reason = context.args.subList(1, context.args.size).joinToString(" ")

        if (reason.isBlank()) return context.sendHelp().queue()

        member.kick(reason).queue()

        return context.respond(
            Embeds.info("Kick erfolgreich.", "`${member.effectiveName}` wurde gekickt.")
        ).queue()
    }
}