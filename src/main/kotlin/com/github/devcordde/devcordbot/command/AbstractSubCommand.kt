/*
 * Copyright 2021 Daniel Scherf & Michael Rittmeister & Julian König
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
import dev.kord.rest.builder.interaction.ApplicationCommandCreateBuilder
import dev.kord.rest.builder.interaction.ApplicationCommandsCreateBuilder
import dev.kord.rest.builder.interaction.SubCommandBuilder

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

    /**
     * Adds this command to the [ApplicationCommandsCreateBuilder].
     */
    abstract fun ApplicationCommandCreateBuilder.applyCommand()

    /**
     * Abstract implementation of a slash sub-command.
     */
    abstract class Command(parent: AbstractCommand) : AbstractSubCommand(parent) {

        /**
         * Function that is called when building command to add options.
         * @see SubCommandBuilder
         */
        open fun SubCommandBuilder.applyOptions(): Unit = Unit

        /**
         * Invokes the command.
         * @param context the [Context] in which the command is invoked
         */
        abstract suspend fun execute(context: Context)

        final override fun ApplicationCommandCreateBuilder.applyCommand() {
            subCommand(this@Command.name, this@Command.description) {
                applyOptions()
            }
        }
    }

    /**
     * Representation of a [sub commands group](https://discord.com/developers/docs/interactions/slash-commands#subcommands-and-subcommand-groups).
     */
    abstract class Group(parent: AbstractCommand) :
        AbstractSubCommand(parent),
        CommandRegistry<Command> {
        override fun ApplicationCommandCreateBuilder.applyCommand() {
            group(name, description) {
                commandAssociations.values
                    .forEach {
                        with(it as AbstractSubCommand) {
                            this@applyCommand.applyCommand()
                        }
                    }
            }
        }
    }
}
