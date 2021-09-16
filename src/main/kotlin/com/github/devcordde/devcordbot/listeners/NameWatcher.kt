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

package com.github.devcordde.devcordbot.listeners

import com.github.devcordde.devcordbot.core.DevCordBot
import com.github.devcordde.devcordbot.util.effictiveName
import com.github.devcordde.devcordbot.util.sanitize
import dev.kord.core.Kord
import dev.kord.core.behavior.edit
import dev.kord.core.entity.Member
import dev.kord.core.event.guild.MemberJoinEvent
import dev.kord.core.event.guild.MemberUpdateEvent
import dev.kord.core.on

fun Kord.addNameWatcher(bot: DevCordBot) {
    on<MemberJoinEvent> {
        member.sanitizeNameIfNeeded(bot)
    }

    on<MemberUpdateEvent> {
        member.sanitizeNameIfNeeded(bot)
    }
}

private suspend fun Member.sanitizeNameIfNeeded(bot: DevCordBot) {
    val sanitizedName = effictiveName.sanitize()
    if (sanitizedName != effictiveName) {
        bot.discordLogger.logEvent(asUser(), "SANITIZE_NAME") { "$effictiveName -> $sanitizedName" }

        edit {
            nickname = sanitizedName
            reason = "Update unmentionable name"
        }
    }
}
