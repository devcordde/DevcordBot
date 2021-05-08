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

package com.github.devcordde.devcordbot.command.root

import com.github.devcordde.devcordbot.command.AbstractCommand
import com.github.devcordde.devcordbot.command.AbstractSubCommand
import com.github.devcordde.devcordbot.command.CommandRegistry
import com.github.devcordde.devcordbot.command.permission.Permission
import dev.kord.rest.builder.interaction.ApplicationCommandsCreateBuilder

/**
 * Abstract implementation of a slash command with subcommands.
 */
abstract class AbstractRootCommand : AbstractCommand(), CommandRegistry<AbstractSubCommand>, RegisterableCommand {

    override val commandAssociations: MutableMap<String, AbstractSubCommand> = mutableMapOf()

    /**
     * Adds this command to the [ApplicationCommandsCreateBuilder].
     */
    final override fun ApplicationCommandsCreateBuilder.applyCommand() {
        command(name, description) {
            defaultPermission = permission == Permission.ANY

            commandAssociations.values
                .asSequence()
                .distinct()
                .forEach {
                    with(it) {
                        applyCommand() // SubCommand.applyCommand()
                    }
                }
        }
    }
}
