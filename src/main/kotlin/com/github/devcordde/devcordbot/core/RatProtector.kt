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

package com.github.devcordde.devcordbot.core

import com.github.devcordde.devcordbot.command.permission.Permission
import com.github.devcordde.devcordbot.command.permission.PermissionState
import com.github.devcordde.devcordbot.event.EventSubscriber
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent

/**
 * Listener that protects #rat-chat from reactions of non-members.
 *
 * @param channelId the id of the rat channel
 * @param roleId the id of the rat role with bypass permissions
 */
class RatProtector(private val channelId: Long, private val roleId: Long, private val bot: DevCordBot) {

    /**
     * Removes "bad" reactions.
     */
    @EventSubscriber
    fun onReactionAdd(event: MessageReactionAddEvent) {
        if (event.user?.isBot == true || event.channel.idLong != channelId || event.member?.roles?.any { it.idLong == roleId } == true || bot.commandClient.permissionHandler.isCovered(
                Permission.MODERATOR, event.member, null, false
            ) == PermissionState.ACCEPTED) return

        @Suppress("ReplaceNotNullAssertionWithElvisReturn") // We have caching enabled so it cannot be null
        event.reaction.removeReaction(event.user!!).queue()
    }
}
