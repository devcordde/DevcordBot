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

package com.github.devcordde.devcordbot.util

import dev.kord.core.behavior.MemberBehavior
import dev.kord.core.behavior.MessageBehavior
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.interaction.PublicInteractionResponseBehavior
import dev.kord.core.behavior.interaction.edit
import dev.kord.core.behavior.interaction.followUp
import dev.kord.core.entity.Member
import dev.kord.core.entity.Message
import dev.kord.core.entity.User
import dev.kord.core.entity.interaction.PublicFollowupMessage
import dev.kord.rest.Image
import dev.kord.rest.builder.message.EmbedBuilder

/**
 * Checks whether a string is numeric or not.
 */
fun String.isNumeric(): Boolean = all(Char::isDigit)

/**
 * Checks whether a string is not numeric or not
 * @see isNumeric
 */
@Suppress("unused")
fun String.isNotNumeric(): Boolean = !isNumeric()

/**
 * Modification of [MemberBehavior.mention] which can validate any format.
 */
fun MemberBehavior.asMention(): Regex = "<@!?$id>\\s?".toRegex()

/**
 * Limits the length of a string by [amount] and adds [contraction] at the end.
 */
fun String.limit(amount: Int, contraction: String = "..."): String =
    if (length < amount) this else "${substring(0, amount - contraction.length)}$contraction"

/**
 * Creates a new message in this channel containing [embedBuilder].
 */
suspend fun MessageChannelBehavior.createMessage(embedBuilder: EmbedBuilder): Message =
    createMessage { embed = embedBuilder }

/**
 * Edits this [public slash command acknowledgement][PublicInteractionResponseBehavior] to contain [embedBuilder].
 */
suspend fun PublicInteractionResponseBehavior.edit(embedBuilder: EmbedBuilder): Message =
    edit { embeds = mutableListOf(embedBuilder) }

/**
 * Follows up in the interaction thread with [embedBuilder].
 */
suspend fun PublicInteractionResponseBehavior.followUp(embedBuilder: EmbedBuilder): PublicFollowupMessage =
    followUp { embeds = mutableListOf(embedBuilder.toRequest()) }

/**
 * Edits this message to contain [embedBuilder].
 */
suspend fun MessageBehavior.edit(embedBuilder: EmbedBuilder): Message = edit { embed = embedBuilder }

/**
 * This uses [User.Avatar.defaultUrl] if [User.Avatar.isCustom] is `false` otherwhise it uses [User.Avatar.getUrl]
 */
val User.effectiveAvatarUrl: String
    get() = with(avatar) { if (isCustom) getUrl(Image.Size.Size64) else defaultUrl }

/**
 * The users nick name if specified, otherwise the username, effectivly the name that is rendered in the Discord UI.
 */
val Member.effictiveName: String
    get() = nickname ?: username
