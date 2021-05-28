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
import com.github.devcordde.devcordbot.command.ExecutableCommand
import com.github.devcordde.devcordbot.command.context.ResponseStrategy.EphemeralResponseStrategy
import com.github.devcordde.devcordbot.command.context.ResponseStrategy.PublicResponseStrategy
import com.github.devcordde.devcordbot.command.permission.Permission
import com.github.devcordde.devcordbot.command.permission.PermissionState
import com.github.devcordde.devcordbot.constants.Embeds
import com.github.devcordde.devcordbot.core.DevCordBot
import com.github.devcordde.devcordbot.database.DevCordUser
import dev.kord.common.annotation.KordUnsafe
import dev.kord.common.entity.AllowedMentionType
import dev.kord.common.entity.Snowflake
import dev.kord.common.entity.optional.Optional
import dev.kord.common.entity.optional.map
import dev.kord.core.Kord
import dev.kord.core.behavior.*
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.interaction.*
import dev.kord.core.entity.Member
import dev.kord.core.entity.Message
import dev.kord.core.entity.interaction.GuildInteraction
import dev.kord.core.event.interaction.InteractionCreateEvent
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.MessageCreateBuilder
import dev.kord.rest.builder.message.MessageModifyBuilder
import dev.kord.rest.json.request.InteractionResponseModifyRequest
import kotlin.contracts.ExperimentalContracts

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
 * @property responseStrategy the [ResponseStrategy] used for [respond] methods
 * @property member the [Member] which executed the command
 *
 * @param T the type of [InteractionResponseBehavior] which acknowledged this invocation
 */
@Suppress("MemberVisibilityCanBePrivate", "unused")
data class Context<T : InteractionResponseBehavior>(
    val bot: DevCordBot,
    val command: AbstractCommand,
    val args: Arguments,
    val event: InteractionCreateEvent,
    val commandClient: CommandClient,
    val devCordUser: DevCordUser,
    val acknowledgement: T,
    val responseStrategy: ResponseStrategy,
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
    suspend fun respond(content: String): ResponseStrategy.EditableResponse {
        return responseStrategy.respond {
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
    suspend fun respond(embedBuilder: EmbedBuilder): ResponseStrategy.EditableResponse = responseStrategy.respond {
        embed = embedBuilder
    }

    /**
     * Sends a help embed for [command].
     * @see Embeds.command
     */
    suspend fun sendHelp(): ResponseStrategy.EditableResponse = respond(Embeds.command(command))

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

/**
 * Representation of a strateg to respond (publicly or ephemerally).
 *
 * @see PublicResponseStrategy
 * @see EphemeralResponseStrategy
 * @see ExecutableCommand.acknowledge
 */
@OptIn(ExperimentalContracts::class)
sealed interface ResponseStrategy {
    /**
     * Builds a message using [messageBuilder] and sends the message.
     */
    suspend fun respond(messageBuilder: MessageCreateBuilder): EditableResponse

    /**
     * Builds a message using [messageBuilder] and sends the message.
     */
    suspend fun respond(messageBuilder: suspend MessageCreateBuilder.() -> Unit): EditableResponse =
        respond(MessageCreateBuilder().apply { messageBuilder() })

    /**
     * Uses [messageBuilder] to follow up in the command thread.
     */
    suspend fun followUp(messageBuilder: MessageCreateBuilder): EditableResponse

    /**
     * Uses [embedBuilder] to follow up in the command thread.
     */
    suspend fun followUp(embedBuilder: EmbedBuilder): EditableResponse = followUp { embed = embedBuilder }

    /**
     * Uses [messageBuilder] to follow up in the command thread.
     */
    suspend fun followUp(messageBuilder: suspend MessageCreateBuilder.() -> Unit): EditableResponse =
        followUp(MessageCreateBuilder().apply { messageBuilder() })

    /**
     * Abstract sent response which can be editable.
     */
    interface EditableResponse {
        /**
         * Edits the response to match the [messageEditBuilder].
         */
        suspend fun edit(messageEditBuilder: MessageModifyBuilder.() -> Unit)

        /**
         * Edits the response to match the [EmbedBuilder].
         */
        suspend fun edit(embedBuilder: EmbedBuilder): Unit = edit { embed = embedBuilder }

        /**
         * Edits the response to match the [EmbedBuilder].
         */
        suspend fun editEmbed(embedBuilder: EmbedBuilder.() -> Unit): Unit = edit { embed(embedBuilder) }

        /**
         * Implementation of [EditableResponse] which can't be edited. (Ephemerals)
         */
        object NonEditableMessage : EditableResponse {
            override suspend fun edit(messageEditBuilder: MessageModifyBuilder.() -> Unit) {
                throw UnsupportedOperationException("Not supported by this type of response")
            }
        }
    }

    /**
     * Implementation of [ResponseStrategy] using an [PublicInteractionResponseBehavior].
     */
    class PublicResponseStrategy(private val acknowledgement: PublicInteractionResponseBehavior) : ResponseStrategy {
        override suspend fun respond(messageBuilder: MessageCreateBuilder): EditableResponse {
            val response = acknowledgement.edit {
                content = messageBuilder.content
                embeds = messageBuilder.embed.asMutableListOrNull()
                allowedMentions = messageBuilder.allowedMentions
            }

            return EditableMessage(response)
        }

        override suspend fun followUp(messageBuilder: MessageCreateBuilder): EditableResponse {
            val response = acknowledgement.followUp {
                content = messageBuilder.content
                embeds = messageBuilder.embed?.toRequest().asMutableListOrNull()
                allowedMentions = messageBuilder.allowedMentions?.build()
            }

            return EditableFollowup(response)
        }

        private class EditableMessage(private val message: MessageBehavior) : EditableResponse {
            override suspend fun edit(messageEditBuilder: MessageModifyBuilder.() -> Unit) {
                message.edit(messageEditBuilder)
            }
        }

        private class EditableFollowup(private val message: PublicFollowupMessageBehavior) : EditableResponse {
            override suspend fun edit(messageEditBuilder: MessageModifyBuilder.() -> Unit) {
                val builder = MessageModifyBuilder().apply(messageEditBuilder)
                message.edit {
                    allowedMentions = builder.allowedMentions
                    content = builder.content
                    embeds = builder.embed.asMutableListOrNull()
                }
            }
        }
    }

    /**
     * Implementation of [ResponseStrategy] using [EphemeralInteractionResponseBehavior].
     */
    class EphemeralResponseStrategy(private val acknowledgement: EphemeralInteractionResponseBehavior) :
        ResponseStrategy {
        override suspend fun respond(messageBuilder: MessageCreateBuilder): EditableResponse {
            val request = with(messageBuilder) {
                InteractionResponseModifyRequest(
                    (content).nullableOptional(),
                    (embed).nullableOptional().map { listOf(it.toRequest()) },
                    (allowedMentions).nullableOptional().map { it.build() }
                )
            }

            acknowledgement.kord.rest.interaction.modifyInteractionResponse(
                acknowledgement.applicationId,
                acknowledgement.token,
                request
            )

            return EditableResponse.NonEditableMessage
        }

        @OptIn(KordUnsafe::class)
        override suspend fun followUp(messageBuilder: MessageCreateBuilder): EditableResponse {
            throw UnsupportedOperationException("You cannot follow up on ephemerals")
        }
    }
}

private fun <T> T?.asMutableListOrNull() = this?.let { mutableListOf<T>(it) } ?: mutableListOf()

private fun <T> T?.nullableOptional(): Optional<T> = when (this) {
    null -> Optional.Missing()
    else -> Optional.Value(this)
}
