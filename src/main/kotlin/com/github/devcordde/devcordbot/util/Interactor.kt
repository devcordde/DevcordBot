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
import com.github.devcordde.devcordbot.dsl.editMessage
import com.github.devcordde.devcordbot.event.EventSubscriber
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.requests.restaction.MessageAction
import kotlin.coroutines.resume
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.minutes

/**
 * Updates a message to a timed out message.
 */
fun Message.timeout(): MessageAction = editMessage(
    Embeds.error("Timed out", "Du hast zu lange gebraucht.")
)

/**
 * Returns the next [Message] by the author in the invocation channel or `null` if [timeout] gets exceeded.
 */
@OptIn(ExperimentalTime::class)
suspend fun Context.readSafe(timeout: Duration = 1.minutes): Message? {
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
suspend fun Context.read(timeout: Duration = 1.minutes): Message {
    return withTimeout(timeout) {
        suspendCancellableCoroutine { cont ->
            jda.addEventListener(object {
                @EventSubscriber
                fun onMessage(event: MessageReceivedEvent) {
                    if (
                        event.author == author &&
                        event.channel == channel
                    )
                        cont.resume(event.message)
                }
            })
        }
    }
}
