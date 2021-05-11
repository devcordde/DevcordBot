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

package com.github.devcordde.devcordbot.core

import com.github.devcordde.devcordbot.command.permission.Permission
import com.github.devcordde.devcordbot.command.permission.PermissionState
import dev.kord.core.Kord
import dev.kord.core.any
import dev.kord.core.event.message.ReactionAddEvent
import dev.kord.core.on

/**
 * Listener that protects #rat-chat from reactions of non-members.
 *
 * @param bot the bot instance
 */
class RatProtector(private val bot: DevCordBot) {

    /**
     * Adds the listener for the [ReactionAddEvent].
     */
    fun Kord.onReactionAdd() {
        on<ReactionAddEvent> {
            val user = user.asUser()
            val member = getUserAsMember()
            if (user.isBot || channelId != bot.config.devrat.channelId || member?.roles?.any { it.id == bot.config.devrat.roleId } == true || bot.commandClient.permissionHandler.isCovered(
                    Permission.MODERATOR, member, null, false
                ) == PermissionState.ACCEPTED
            ) return@on

            message.deleteReaction(
                userId,
                emoji
            )
        }
    }
}
