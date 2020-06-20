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

package com.github.devcordde.devcordbot.event

import com.github.devcordde.devcordbot.database.DevCordUser
import com.github.devcordde.devcordbot.database.Users
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.ChannelType
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.Event
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageUpdateEvent
import org.jetbrains.exposed.dao.entityCache
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Extension of [MessageReceivedEvent] containing [devCordUser].
 * @property devCordUser [DevCordUser] corresponding to [Message.getAuthor]
 */
@EventDescriber(callParents = false)
class DevCordMessageReceivedEvent(
    api: JDA, responseNumber: Long, message: Message,
    val devCordUser: DevCordUser
) : MessageReceivedEvent(api, responseNumber, message)

/**
 * Extension of [GuildMessageReceivedEvent] containing [devCordUser].
 * @property devCordUser [DevCordUser] corresponding to [Message.getAuthor]
 */
@EventDescriber(callParents = false)
class DevCordGuildMessageReceivedEvent(api: JDA, responseNumber: Long, message: Message, val devCordUser: DevCordUser) :
    GuildMessageReceivedEvent(api, responseNumber, message)

/**
 * Extension of [GuildMessageReceivedEvent] containing [devCordUser].
 * @property devCordUser [DevCordUser] corresponding to [Message.getAuthor]
 */
@EventDescriber(callParents = false)
class DevCordGuildMessageEditEvent(api: JDA, responseNumber: Long, message: Message, val devCordUser: DevCordUser) :
    GuildMessageUpdateEvent(api, responseNumber, message)

/**
 * Shorthand for user of events above
 */
val Event.devCordUser: DevCordUser
    get() =
        when (this) {
            is DevCordMessageReceivedEvent -> devCordUser
            is DevCordGuildMessageReceivedEvent -> devCordUser
            is DevCordGuildMessageEditEvent -> devCordUser
            else -> throw UnsupportedOperationException("Unsupported event")
        }

internal class MessageListener {

    @EventSubscriber
    fun onMessageEdit(event: GuildMessageUpdateEvent) {
        if (event.author.isBot) return
        val devCordUser = transaction {
            val userId = event.author.idLong
            entityCache.data[Users]?.get(userId) as? DevCordUser ?: DevCordUser.findById(userId)!!
        }
        event.jda.eventManager.handle(
            DevCordGuildMessageEditEvent(
                event.jda,
                event.responseNumber,
                event.message,
                devCordUser
            )
        )
    }

    @EventSubscriber
    fun onMessage(event: MessageReceivedEvent) {
        if (event.author.isBot) return

        val devCordUser = transaction { DevCordUser.findOrCreateById(event.author.idLong) }
        if (event.isFromType(ChannelType.PRIVATE)) {
            event.jda.eventManager.handle(
                DevCordMessageReceivedEvent(
                    event.jda,
                    event.responseNumber,
                    event.message,
                    devCordUser
                )
            )
        } else {
            event.jda.eventManager.handle(
                DevCordGuildMessageReceivedEvent(
                    event.jda,
                    event.responseNumber,
                    event.message,
                    devCordUser
                )
            )
        }
    }
}
