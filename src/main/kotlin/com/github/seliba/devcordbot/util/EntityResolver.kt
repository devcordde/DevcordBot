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

package com.github.seliba.devcordbot.util

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.*

/**
 * Convenience alias for [Message.MentionType].
 */
typealias MentionType = Message.MentionType

/**
 * Utility for resolving Discord entities by their name, id, or mention.
 */
object EntityResolver {

    /**
     * Resolves a [Role] by its mention, id, or name.
     * @param guild the [Guild] the role is on
     * @param input the [String] to resolve the Role from
     * @param ignoreCase whether to ignore the case of the name or not
     *
     * @return the resolved [Role] or `null` if none was found
     */
    fun resolveRole(guild: Guild, input: String, ignoreCase: Boolean = false): Role? =
        resolveEntity(input, MentionType.ROLE, guild::getRoleById, guild::getRolesByName, ignoreCase)

    /**
     * Resolves a [TextChannel] by its mention, id, or name.
     * @param guild the [Guild] the channel is on
     * @param input the [String] to resolve the Role from
     * @param ignoreCase whether to ignore the case of the name or not
     *
     * @return the resolved [TextChannel] or `null` if none was found
     */
    fun resolveTextChannel(guild: Guild, input: String, ignoreCase: Boolean = false): TextChannel? =
        resolveEntity(input, MentionType.CHANNEL, guild::getTextChannelById, guild::getTextChannelsByName, ignoreCase)

    /**
     * Resolves a [User] by its mention, id, or name.
     * @param jda the [JDA] instance to resolve the User from
     * @param input the [String] to resolve the Role from
     * @param ignoreCase whether to ignore the case of the name or not
     *
     * @return the resolved [User] or `null` if none was found
     */
    fun resolveUser(jda: JDA, input: String, ignoreCase: Boolean = false): User? =
        resolveEntity(input, MentionType.USER, jda::getUserById, jda::getUsersByName, ignoreCase)

    /**
     * Resolves a [Member] by its mention, id, or name.
     * @param guild the [Guild] the member is on
     * @param input the [String] to resolve the Role from
     * @param ignoreCase whether to ignore the case of the name or not
     *
     * @return the resolved [Member] or `null` if none was found
     */
    fun resolveMember(guild: Guild, input: String, ignoreCase: Boolean = false): Member? =
        resolveEntity(input, MentionType.USER, guild::getMemberById, guild::getMembersByName, ignoreCase)

    private fun <T : IMentionable> resolveEntity(
        input: String,
        type: MentionType,
        idResolver: (String) -> T?,
        nameResolver: (String, Boolean) -> Collection<T>,
        ignoreCase: Boolean = false
    ): T? {
        require(input.isNotBlank()) { "Input string cannot be blank" }
        val matcher = type.pattern.matcher(input)
        return if (matcher.matches()) {
            idResolver(matcher.group(1))
        } else {
            if (input.isNumeric()) {
                idResolver(input)
            } else {
                nameResolver(input, ignoreCase).firstOrNull()
            }
        }
    }
}
