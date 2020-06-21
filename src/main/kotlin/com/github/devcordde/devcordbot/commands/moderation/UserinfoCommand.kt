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
import net.dv8tion.jda.api.EmbedBuilder
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

/**
 * UserinfoCommand.
 */
class UserinfoCommand : AbstractCommand() {
    override val aliases: List<String> = listOf("userinfo")
    override val displayName: String = "userinfo"
    override val description: String = "Shows information about the User."
    override val usage: String = "<user>"
    override val permission: Permission = Permission.MODERATOR
    override val category: CommandCategory = CommandCategory.MODERATION
    override val commandPlace: CommandPlace = CommandPlace.GM

    private val formatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
        .withLocale(Locale.GERMANY)
        .withZone(ZoneId.systemDefault())

    override suspend fun execute(context: Context) {
        if (context.args.size < 1) return context.sendHelp().queue()

        val member = context.args.member(0, context = context) ?: return
        return context.respond(
            EmbedBuilder().addField("Joined", member.timeJoined.format(formatter), true)
                .build()
        ).queue()
    }
}