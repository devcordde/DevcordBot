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

package com.github.seliba.devcordbot.command.context

import com.github.seliba.devcordbot.command.AbstractCommand
import com.github.seliba.devcordbot.command.CommandClient
import com.github.seliba.devcordbot.constants.Embeds
import com.github.seliba.devcordbot.dsl.EmbedConvention
import com.github.seliba.devcordbot.dsl.sendMessage
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.requests.restaction.MessageAction

/**
 * Representation of a context of a command execution.
 * @property command the executed command
 * @property args the [Arguments] of the command
 * @property commandClient the [CommandClient] which executed this command
 * @param event the event that triggered the command
 */
@Suppress("MemberVisibilityCanBePrivate", "unused")
data class Context(
    val command: AbstractCommand,
    val args: List<String>,
    private val event: GuildMessageReceivedEvent,
    val commandClient: CommandClient
) {
    /**
     * The [JDA] instance.
     */
    val jda: JDA
        get() = event.jda

    /**
     * The message that triggered the command.
     */
    val message: Message
        get() = event.message

    /**
     * The id of [event]
     */
    val messageId: Long
        get() = message.idLong

    /**
     * The [TextChannel] of [event]
     */
    val channel: TextChannel
        get() = event.channel

    /**
     * The author of the [event].
     */
    val author: User
        get() = event.author

    /**
     * The member of the [author].
     */
    val member: Member
        get() = event.member!! //CommandClient ignores webhook messages, so this cannot be null

    /**
     * The guild of the [channel].
     */
    val guild: Guild
        get() = event.guild

    /**
     * The [self member][Member] of the bot.
     */
    val me: Member
        get() = guild.selfMember

    /**
     * The [SelfUser] of the bot.
     */
    val selfUser: SelfUser
        get() = jda.selfUser

    /**
     * Sends [content] into [channel].
     * @return a [MessageAction] that sends the message
     */
    fun respond(content: String): MessageAction = channel.sendMessage(content)

    /**
     * Sends [embed] into [channel].
     * @return a [MessageAction] that sends the message
     */
    fun respond(embed: MessageEmbed): MessageAction = channel.sendMessage(embed)

    /**
     * Sends [embedBuilder] into [channel].
     * @return a [MessageAction] that sends the message
     */
    fun respond(embedBuilder: EmbedBuilder): MessageAction = channel.sendMessage(embedBuilder.build())

    /**
     * Sends [embed] into [channel].
     * @return a [MessageAction] that sends the message
     */
    fun respond(embed: EmbedConvention): MessageAction = channel.sendMessage(embed)

}
