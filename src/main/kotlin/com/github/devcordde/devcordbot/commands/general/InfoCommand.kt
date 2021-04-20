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

import com.github.devcordde.devcordbot.command.AbstractSingleCommand
import com.github.devcordde.devcordbot.command.CommandCategory
import com.github.devcordde.devcordbot.command.CommandPlace
import com.github.devcordde.devcordbot.command.context.Context
import com.github.devcordde.devcordbot.command.permission.Permission
import com.github.devcordde.devcordbot.dsl.editOriginal
import com.github.devcordde.devcordbot.listeners.SelfMentionListener

/**
 * InfoCommand.
 */
class InfoCommand : AbstractSingleCommand() {
    override val name: String = "info"
    override val description: String = "Zeigt Bot-Informationen an."
    override val permission: Permission = Permission.ANY
    override val category: CommandCategory = CommandCategory.GENERAL
    override val commandPlace: CommandPlace = CommandPlace.ALL

    override suspend fun execute(context: Context) {
        val devCordBot = context.bot
        val contributors = SelfMentionListener.fetchContributors(devCordBot)

        context.respond(SelfMentionListener.makeEmbed(devCordBot)).flatMap {
            context.ack.editOriginal(SelfMentionListener.makeEmbed(devCordBot, contributors))
        }.queue()
    }
}
