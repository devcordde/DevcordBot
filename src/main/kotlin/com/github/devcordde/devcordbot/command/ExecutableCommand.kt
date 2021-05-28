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
import dev.kord.core.behavior.interaction.InteractionResponseBehavior
import dev.kord.core.entity.interaction.Interaction
import dev.kord.core.event.interaction.InteractionCreateEvent

/**
 * Command that can be executed (non groups, sub commands, single commands)
 *
 * @param T the [InteractionResponseBehavior] produced by this commands acknowledgement (See [acknowledge])
 */
interface ExecutableCommand<T : InteractionResponseBehavior> {
    /**
     * Function acknowledging the [InteractionCreateEvent].
     *
     * @see Interaction.ackowledgePublic
     * @see Interaction.acknowledgeEphemeral
     */
    suspend fun InteractionCreateEvent.acknowledge(): T

    /**
     * Executes the command logic.
     *
     * @see Context
     */
    suspend fun execute(context: Context<T>)
}
