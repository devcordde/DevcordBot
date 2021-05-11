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

package com.github.devcordde.devcordbot.command.context

import com.github.devcordde.devcordbot.command.AbstractCommand
import com.github.devcordde.devcordbot.command.CommandClient
import com.github.devcordde.devcordbot.command.permission.Permission
import com.github.devcordde.devcordbot.command.permission.PermissionState
import com.github.devcordde.devcordbot.constants.Embeds
import com.github.devcordde.devcordbot.core.DevCordBot
import com.github.devcordde.devcordbot.database.DevCordUser
import dev.kord.common.annotation.KordUnsafe
import dev.kord.common.entity.AllowedMentionType
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.GuildBehavior
import dev.kord.core.behavior.MemberBehavior
import dev.kord.core.behavior.UserBehavior
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.interaction.PublicInteractionResponseBehavior
import dev.kord.core.behavior.interaction.edit
import dev.kord.core.entity.Member
import dev.kord.core.entity.Message
import dev.kord.core.entity.interaction.GuildInteraction
import dev.kord.core.event.interaction.InteractionCreateEvent
import dev.kord.rest.builder.message.EmbedBuilder

/**
 * Representation of a context of a command execution.
 * @property command the executed command
 * @property args the [Arguments] of the command
 * @property commandClient the [CommandClient] which executed this command
 * @property bot instance of the [DevCordBot]
 * @property event the [InteractionCreateEvent] which triggered this invocation
 * @property acknowledgement the [PublicInteractionResponseBehavior] which acknowledged the exeuction of the command
 * it acts like a communication point between the bot and the interaction thread, all message sending should be
 * handles using this [respond] and [sendHelp] methods will also refer to this
 * @property devCordUser User storing database settings. See [DevCordUser]
 * @property member the [Member] which executed the command
 */
@Suppress("MemberVisibilityCanBePrivate", "unused")
data class Context(
    val bot: DevCordBot,
    val command: AbstractCommand,
    val args: Arguments,
    val event: InteractionCreateEvent,
    val commandClient: CommandClient,
    val devCordUser: DevCordUser,
    var acknowledgement: PublicInteractionResponseBehavior,
    val member: Member
) {

    /**
     * The [Kord] instance.
     */
    val kord: Kord
        get() = event.kord

    /**
     * The id of [InteractionCreateEvent.interaction].
     */
    val interactionId: Snowflake
        get() = event.interaction.id

    /**
     * The [MessageChannelBehavior] of [event].
     */
    val channel: MessageChannelBehavior
        get() = event.interaction.channel

    /**
     * The author of the [interactionId].
     */
    val author: UserBehavior
        get() = event.interaction.user

    /**
     * The guild of the [channel].
     */
    val guild: GuildBehavior
        get() = (event.interaction as? GuildInteraction)?.guild ?: bot.guild

    /**
     * The [self member][MemberBehavior] of the bot.
     */
    @OptIn(KordUnsafe::class)
    val me: MemberBehavior
        get() = event.kord.unsafe.member(guild.id, event.kord.selfId)

    /**
     * The [UserBehavior] of the bot user.
     */
    @OptIn(KordUnsafe::class)
    val selfUser: UserBehavior
        get() = event.kord.unsafe.user(event.kord.selfId)

    /**
     * Sends [content] into [channel].
     * @return the [Message] which was sent
     */
    suspend fun respond(content: String): Message {
        return acknowledgement.edit {
            allowedMentions {
                +AllowedMentionType.UserMentions
                +AllowedMentionType.RoleMentions
            }
            this.content = content
        }
    }

    /**
     * Sends [embedBuilder] into [channel].
     * @return the [Message] which was sent
     */
    suspend fun respond(embedBuilder: EmbedBuilder): Message = acknowledgement.edit {
        embeds = mutableListOf(embedBuilder)
    }

    /**
     * Sends a help embed for [command].
     * @see Embeds.command
     */
    suspend fun sendHelp(): Message = respond(Embeds.command(command))

    /**
     * Checks whether the [member] has [Permission.ADMIN] or not.
     */
    suspend fun hasAdmin(): Boolean = hasPermission(Permission.ADMIN)

    /**
     * Checks whether the [member] has [Permission.MODERATOR] or not.
     */
    suspend fun hasModerator(): Boolean = hasPermission(Permission.MODERATOR)

    /**
     * Checks if [devCordUser] has [permission].
     */
    suspend fun hasPermission(permission: Permission): Boolean =
        commandClient.permissionHandler.isCovered(permission, member, devCordUser) == PermissionState.ACCEPTED
}
