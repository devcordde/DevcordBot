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

package com.github.devcordde.devcordbot.command

import com.github.devcordde.devcordbot.command.context.Context
import com.github.devcordde.devcordbot.command.permission.Permission
import com.github.devcordde.devcordbot.command.slashcommands.permissions.PermissiveSubCommandData
import com.github.devcordde.devcordbot.command.slashcommands.permissions.PermissiveSubCommandGroupData
import net.dv8tion.jda.api.requests.restaction.CommandUpdateAction

/**
 * Skeleton of a sub command.
 * @property parent the parent of the command
 * @property callback an [Exception] that is supposed to highlight class defention line
 * @see AbstractCommand
 */
@Suppress("MemberVisibilityCanBePrivate")
sealed class AbstractSubCommand(val parent: AbstractCommand) : AbstractCommand() {
    override val callback: Exception = Exception()
    override val category: CommandCategory
        get() = parent.category
    override val permission: Permission
        get() = parent.permission
    override val commandPlace: CommandPlace
        get() = parent.commandPlace

    abstract fun CommandUpdateAction.CommandData.addMe()

    abstract class Command(parent: AbstractCommand) : AbstractSubCommand(parent) {

        open val options: List<CommandUpdateAction.OptionData> = emptyList()

        abstract suspend fun execute(context: Context)

        override fun CommandUpdateAction.CommandData.addMe() {
            addSubcommand(toCommandData())
        }

        internal fun toCommandData(): CommandUpdateAction.SubcommandData {
            try {
                val command = PermissiveSubCommandData(
                    name, description
                )
                command.defaultPermission = permission == Permission.ANY
                options.forEach(command::addOption)

                return command
            } catch (e: Exception) {
                throw IllegalStateException(
                    "Could not process command with name $name",
                    e
                )
            }
        }
    }

    abstract class Group(parent: AbstractCommand) : AbstractSubCommand(parent),
        CommandRegistry<Command> {
        override fun CommandUpdateAction.CommandData.addMe() {
            val data = PermissiveSubCommandGroupData(name, description)
            data.defaultPermission = permission == Permission.ANY
            commandAssociations.values
                .map(Command::toCommandData)
                .forEach(data::addSubcommand)

            addSubcommandGroup(data)
        }
    }
}
