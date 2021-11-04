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

import dev.kord.core.behavior.interaction.InteractionResponseBehavior
import dev.kord.core.cache.data.toData
import dev.kord.core.entity.Message
import dev.kord.rest.builder.message.modify.InteractionResponseModifyBuilder
import dev.kord.rest.request.RestRequestException
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

// This is a workaround because the builtin kord function does not return the message.
/**
 * Requests to edit this interaction response.
 *
 * @return The edited [Message] of the interaction response.
 *
 * @throws [RestRequestException] if something went wrong during the request.
 */
@Suppress("NAME_SHADOWING")
suspend inline fun InteractionResponseBehavior.workaroundEdit(builder: InteractionResponseModifyBuilder.() -> Unit): Message {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    val builder = InteractionResponseModifyBuilder().apply(builder)
    val data = kord.rest.interaction.modifyInteractionResponse(applicationId, token, builder.toRequest()).toData()
    return Message(data, kord)
}
