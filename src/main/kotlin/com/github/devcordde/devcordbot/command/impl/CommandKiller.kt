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

package com.github.devcordde.devcordbot.command.impl

import dev.kord.core.Kord
import dev.kord.core.event.message.ReactionAddEvent
import dev.kord.core.on
import dev.kord.x.emoji.Emojis

internal fun Kord.registerCommandKiller() = on<ReactionAddEvent> {
    if (emoji.name != Emojis.wastebasket.unicode) return@on
    val message = getMessage()
    val interaction = message.interaction ?: return@on
    if (user.id != interaction.data.user) return@on
    message.delete()
}
