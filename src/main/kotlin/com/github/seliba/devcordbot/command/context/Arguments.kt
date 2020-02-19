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

package com.github.seliba.devcordbot.command.context

import com.github.seliba.devcordbot.constants.Embeds
import com.github.seliba.devcordbot.dsl.EmbedConvention
import com.github.seliba.devcordbot.util.EntityResolver
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.*

/**
 * A converter that converts a command argument.
 * @param T the type of the converted argument
 */
typealias ArgumentConverter<T> = (String) -> T

/**
 * Representation of a commands' arguments.
 * @param list the list of arguments
 * @property raw plain arguments string
 * @see List
 */
@Suppress("MemberVisibilityCanBePrivate", "unused")
data class Arguments(private val list: List<String>, val raw: String) :
    List<String> by list {

    /**
     * Joins the arguments back to their original String.
     */
    fun join(): String = raw

    /**
     * Split's the arguments by the specified [delimiter].
     * @see join
     * @return a new instance of [Arguments] containing the new split arguments
     */
    fun split(delimiter: String): Arguments = Arguments(raw.split(delimiter), raw)

    /**
     * Return the argument at the specified [index] or `null` if there is no argument at that position.
     */
    fun optionalArgument(index: Int): String? = getOrNull(index)

    /**
     * Return the argument at the specified [index] as an [Int] or `null` if there is no argument at that position, or it is not an [Int].
     */
    fun optionalInt(index: Int): Int? = optionalArgument(index, String::toIntOrNull)

    /**
     * Return the argument at the specified [index] as a [Long] or `null` if there is no argument at that position, or it is not a [Long].
     */
    fun optionalLong(index: Int): Long? = optionalArgument(index, String::toLongOrNull)

    /**
     * Return the argument at the specified [index] as a [User] or `null` if there is no argument at that position, or it is not a [User].
     * @param ignoreCase whether the case of the name should be ignored or not
     */
    fun optionalUser(index: Int, ignoreCase: Boolean = false, jda: JDA): User? =
        optionalArgument(index) { EntityResolver.resolveUser(jda, it, ignoreCase) }

    /**
     * Return the argument at the specified [index] as a [Member] or `null` if there is no argument at that position, or it is not a [Member].
     * @param ignoreCase whether the case of the name should be ignored or not
     */
    fun optionalMember(index: Int, ignoreCase: Boolean = false, guild: Guild): Member? =
        optionalArgument(index) { EntityResolver.resolveMember(guild, it, ignoreCase) }

    /**
     * Return the argument at the specified [index] as a [Role] or `null` if there is no argument at that position, or it is not a [Role].
     * @param ignoreCase whether the case of the name should be ignored or not
     */
    fun optionalRole(index: Int, ignoreCase: Boolean = false, guild: Guild): Role? =
        optionalArgument(index) { EntityResolver.resolveRole(guild, it, ignoreCase) }

    /**
     * Return the argument at the specified [index] as a [TextChannel] or `null` if there is no argument at that position, or it is not a [TextChannel].
     * @param ignoreCase whether the case of the name should be ignored or not
     */
    fun optionalChannel(
        index: Int,
        ignoreCase: Boolean = false,
        guild: Guild
    ): TextChannel? =
        optionalArgument(index) { EntityResolver.resolveTextChannel(guild, it, ignoreCase) }

    /**
     * Return the argument at the specified [index] or `null` if there is no argument at that position.
     * And sends a command help if there is no argument at that position.
     * @param context the context that executed the command
     */
    fun requiredArgument(index: Int, context: Context): String? =
        requiredArgument(index, context, this::optionalArgument) {
            Embeds.command(context.command)
        }

    /**
     * Return the argument at the specified [index] as an [Int] or `null` if there is no argument at that position.
     * If there is no [Int] at that position it sends a help message.
     * @param context the context that executed the command
     */
    fun int(index: Int, context: Context): Int? = requiredArgument(index, context, this::optionalInt) {
        Embeds.error(
            "Ungültiger Zahlenwert!",
            "Bitte gebe einen gültigen Zahlenwert an"
        )
    }

    /**
     * Return the argument at the specified [index] as a [Long] or `null` if there is no argument at that position.
     * If there is no [Long] at that position it sends a help message.
     * @param context the context that executed the command
     */
    fun long(index: Int, context: Context): Long? = requiredArgument(index, context, this::optionalLong) {
        Embeds.error(
            "Ungültiger Zahlenwert!",
            "Bitte gebe einen gültigen Zahlenwert an"
        )
    }

    /**
     * Return the argument at the specified [index] as a [User] or `null` if there is no argument at that position.
     * If there is no [User] at that position it sends a help message.
     * @param ignoreCase whether the case of the name should be ignored or not
     * @param context the context that executed the command
     */
    fun user(index: Int, ignoreCase: Boolean = false, context: Context): User? =
        requiredArgument(index, context, { optionalUser(index, ignoreCase, context.jda) }) {
            Embeds.error("Ungültiger Benutzer!", "Der von dir angegebene Benutzer scheint nicht zu existieren.")
        }

    /**
     * Return the argument at the specified [index] as a [Member] or `null` if there is no argument at that position.
     * If there is no [Member] at that position it sends a help message.
     * @param ignoreCase whether the case of the name should be ignored or not
     * @param context the context that executed the command
     */
    fun member(index: Int, ignoreCase: Boolean = false, context: Context): Member? =
        requiredArgument(index, context, { optionalMember(index, ignoreCase, context.guild) }) {
            Embeds.error("Ungültiger Benutzer!", "Der von dir angegebene Benutzer scheint nicht zu existieren.")
        }

    /**
     * Return the argument at the specified [index] as a [Role] or `null` if there is no argument at that position.
     * If there is no [Role] at that position it sends a help message.
     * @param ignoreCase whether the case of the name should be ignored or not
     * @param context the context that executed the command
     */
    fun role(index: Int, ignoreCase: Boolean = false, context: Context): Role? =
        requiredArgument(index, context, { optionalRole(index, ignoreCase, context.guild) }) {
            Embeds.error("Ungültige Rolle!", "Die von dir angegebene Rolle scheint nicht zu existieren.")
        }

    /**
     * Return the argument at the specified [index] as a [TextChannel] or `null` if there is no argument at that position.
     * If there is no [TextChannel] at that position it sends a help message.
     * @param ignoreCase whether the case of the name should be ignored or not
     * @param context the context that executed the command
     */
    fun channel(index: Int, ignoreCase: Boolean = false, context: Context): TextChannel? =
        requiredArgument(index, context, { optionalChannel(index, ignoreCase, context.guild) }) {
            Embeds.error("Ungültige Text-Kanal!", "Der von dir angegebene Text-Kanal scheint nicht zu existieren.")
        }

    private fun <T> optionalArgument(index: Int, transform: ArgumentConverter<T>): T? =
        optionalArgument(index)?.let(transform)

    private fun <T> requiredArgument(
        index: Int,
        context: Context,
        provider: (Int) -> T?,
        lazyError: () -> EmbedConvention
    ): T? {
        val found = provider(index)
        if (found == null) context.respond(lazyError()).queue()
        return found
    }
}
