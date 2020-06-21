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
 * UnmuteCommand.
 */
class UnmuteCommand(private val muteRoleId: String) : AbstractCommand() {
    override val aliases: List<String> = listOf("unmute")
    override val displayName: String = "unmute"
    override val description: String = "Unmuted einen Member."
    override val usage: String = "<user>"
    override val permission: Permission = Permission.MODERATOR
    override val category: CommandCategory = CommandCategory.MODERATION
    override val commandPlace: CommandPlace = CommandPlace.GM

    override suspend fun execute(context: Context) {
        val member = context.args.member(0, context = context) ?: return

        val muteRole = context.jda.getRoleById(muteRoleId) ?: return

        if (!member.roles.contains(muteRole)) return context.respond(
            Embeds.info("Schon gemuted.", "`${member.effectiveName}` ist nicht gemuted.")
        ).queue()

        context.guild.removeRoleFromMember(member, muteRole).queue()

        return context.respond(
            Embeds.info("Unmuted.", "`${member.effectiveName}` wurde entmuted.")
        ).queue()
    }
}