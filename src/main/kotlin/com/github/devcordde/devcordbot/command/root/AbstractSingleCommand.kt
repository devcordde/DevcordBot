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
import com.github.devcordde.devcordbot.command.ExecutableCommand
import com.github.devcordde.devcordbot.command.context.Context
import dev.kord.core.behavior.interaction.InteractionResponseBehavior
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import dev.kord.rest.builder.interaction.MultiApplicationCommandBuilder

/**
 * Abstract single command (without sub commands).
 */
abstract class AbstractSingleCommand<T : InteractionResponseBehavior> :
    AbstractCommand(),
    RegisterableCommand,
    ExecutableCommand<T> {

    /**
     * Invokes the command.
     * @param context the [Context] in which the command is invoked
     */
    abstract override suspend fun execute(context: Context<T>)

    /**
     * Function called in [applyCommand] to add options.
     */
    open fun ChatInputCreateBuilder.applyOptions(): Unit = Unit

    final override fun MultiApplicationCommandBuilder.applyCommand() {
        input(name, description) {
            applyOptions()
        }
    }
}
