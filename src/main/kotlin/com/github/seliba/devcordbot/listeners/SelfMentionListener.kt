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

package com.github.seliba.devcordbot.listeners

import com.github.seliba.devcordbot.constants.Constants
import com.github.seliba.devcordbot.constants.Embeds
import com.github.seliba.devcordbot.dsl.sendMessage
import com.github.seliba.devcordbot.util.asMention
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.hooks.SubscribeEvent

/**
 * Listens for the bot being mentioned.
 */
class SelfMentionListener {

    /**
     * Listens for new Guild messages.
     */
    @SubscribeEvent
    @Suppress("unused")
    fun onMessageReceive(event: GuildMessageReceivedEvent) {
        if (event.message.contentRaw == event.guild.selfMember.asMention()) {
            event.channel.sendMessage(Embeds.info("DevCordBot") {
                addField("Programmiersprache", "[Kotlin](https://kotlinlang.org)", inline = true)
                addField("Entwickler", "das_#9677 & Schlaubi#0001 & kobold#1524", inline = true)
                addField(
                    "Source",
                    "[github.com/Seliba/Devcordbot](https://github.com/Seliba/Devcordbot)",
                    inline = true
                )
                addField("User", event.jda.users.size.toString(), inline = true)
                addField("Prefix", "`${Constants.firstPrefix}`", inline = true)
            }).queue()
        }
    }
}
