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

import dev.kord.core.entity.channel.DmChannel
import dev.kord.core.entity.channel.GuildChannel
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.core.event.interaction.InteractionCreateEvent

/**
 * CommandPlace defines the places where the command may be executed.
 */
enum class CommandPlace {
    /**
     * Private Messages
     */
    PRIVATE_MESSAGE,

    /**
     * Guild Messages
     */
    GUILD_MESSAGE,

    /**
     * Guild and Private Messages
     */
    ALL;

    /**
     * Check if the message matches the CommandPlace.
     */
    fun matches(event: InteractionCreateEvent): Boolean {
        return when (this) {
            ALL -> true
            PRIVATE_MESSAGE -> (event.interaction as? ChatInputCommandInteraction)?.channel is DmChannel
            GUILD_MESSAGE -> (event.interaction as? ChatInputCommandInteraction)?.channel is GuildChannel
        }
    }
}
