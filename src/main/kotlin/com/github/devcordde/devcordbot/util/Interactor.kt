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

package com.github.devcordde.devcordbot.util

import com.github.devcordde.devcordbot.command.context.Context
import com.github.devcordde.devcordbot.constants.Embeds
import dev.kord.core.behavior.MessageBehavior
import dev.kord.core.behavior.edit
import dev.kord.core.entity.Message
import dev.kord.core.event.message.MessageCreateEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

/**
 * Updates a message to a timed out message.
 */
suspend fun MessageBehavior.timeout(): Message = edit {
    Embeds.error("Timed out", "Du hast zu lange gebraucht.")
}

/**
 * Returns the next [Message] by the author in the invocation channel or `null` if [timeout] gets exceeded.
 */
@OptIn(ExperimentalTime::class)
suspend fun Context.readSafe(timeout: Duration = Duration.minutes(1)): Message? {
    return try {
        read(timeout)
    } catch (e: TimeoutCancellationException) {
        null
    }
}

/**
 * Returns the next [Message] by the author in the invocation channel.
 * Throws a [TimeoutCancellationException] after [timeout] in case there was no message
 */
@OptIn(ExperimentalTime::class, ExperimentalCoroutinesApi::class)
suspend fun Context.read(timeout: Duration = Duration.minutes(1)): Message {
    return withTimeout(timeout) {
        bot.kord.events
            .filterIsInstance<MessageCreateEvent>()
            .filter { it.message.author == author }
            .filter { it.message.channel == channel }
            .take(1)
            .single()
            .message
    }
}
