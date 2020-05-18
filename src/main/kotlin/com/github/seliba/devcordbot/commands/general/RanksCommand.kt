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
import com.github.seliba.devcordbot.listeners.Level

/**
 * RanksCommand.
 */
class RanksCommand : AbstractCommand() {
    override val aliases: List<String> = listOf("ranks")
    override val displayName: String = "ranks"
    override val description: String = "Zeigt die verfügbaren Ränge an."
    override val usage: String = ""
    override val permission: Permission = Permission.ANY
    override val category: CommandCategory = CommandCategory.GENERAL

    override suspend fun execute(context: Context) {
        context.respond(
            Embeds.info("Rollen") {
                Level.values().forEach {
                    val roleName = context.guild.getRoleById(it.roleId)?.name ?: "Rolle nicht gefunden"
                    addField(
                        "Level ${it.level}",
                        roleName
                    )
                }
            }
        ).queue()
    }

}