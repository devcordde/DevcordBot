/*
 * Copyright 2020 Daniel Scherf & Michael Rittmeister
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

package com.github.seliba.devcordbot.command

import com.github.seliba.devcordbot.command.context.Context
import com.github.seliba.devcordbot.command.perrmission.Permissions

/**
 * Skeleton of a command.
 * @property aliases list of strings that invoke the command
 * @property name the name used in usage messages
 * @property displayName name that is used on help messages
 * @property description the description of the command
 * @property usage the full usage of the command
 * @property permissions the command permissions
 * @property commandAssociations all alias-command associations of sub-commands
 * @property category the [CommandCategory] of the command
 */
abstract class AbstractCommand : CommandRegistry {

    override val commandAssociations: MutableMap<String, AbstractCommand> = mutableMapOf()

    abstract val aliases: List<String>
    val name: String
        get() = aliases.first()
    abstract val displayName: String
    abstract val description: String
    abstract val usage: String
    abstract val permissions: Permissions
    abstract val category: CommandCategory

    /**
     * Invokes the command.
     * @param context the [Context] in which the command is invoked
     */
    abstract fun execute(context: Context)

    override fun registerCommands(vararg commands: AbstractCommand) {
        if (commands.any { it !is AbstractSubCommand }) {
            error("SubCommand require extending AbstractSubCommand")
        }
        super.registerCommands(*commands)
    }
}
