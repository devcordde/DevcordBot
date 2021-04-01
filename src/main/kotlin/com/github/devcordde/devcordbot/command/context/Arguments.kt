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

package com.github.devcordde.devcordbot.command.context

import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent

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
data class Arguments(
    private val options: Map<String, SlashCommandEvent.OptionData>
) {

    /**
     * Returns the argument with [name].
     */
    fun argument(name: String): SlashCommandEvent.OptionData =
        optionalArgument(name) ?: error("Argument $name not found")

    fun string(name: String): String = argument(name).asString

    fun optionalString(name: String): String? = optionalArgument(name)?.asString

    /**
     * Return the argument at the specified [index] or `null` if there is no argument at that position.
     */
    fun optionalArgument(name: String): SlashCommandEvent.OptionData? = options[name]

    /**
     * Return the argument at the specified [index] as an [Int] or `null` if there is no argument at that position, or it is not an [Int].
     */
    fun optionalInt(name: String): Int? = optionalArgument(name)?.asLong?.toInt()

    /**
     * Return the argument at the specified [index] as a [Long] or `null` if there is no argument at that position, or it is not a [Long].
     */
    fun optionalLong(name: String): Long? = optionalArgument(name)?.asLong

    /**
     * Return the argument at the specified [index] as a [User] or `null` if there is no argument at that position, or it is not a [User].
     * @param ignoreCase whether the case of the name should be ignored or not
     */
    fun optionalUser(name: String): User? =
        optionalArgument(name)?.asUser

    /**
     * Return the argument at the specified [index] as a [Member] or `null` if there is no argument at that position, or it is not a [Member].
     * @param ignoreCase whether the case of the name should be ignored or not
     */
    fun optionalMember(name: String): Member? =
        optionalArgument(name)?.asMember

    /**
     * Return the argument at the specified [index] as a [Role] or `null` if there is no argument at that position, or it is not a [Role].
     * @param ignoreCase whether the case of the name should be ignored or not
     */
    fun optionalRole(name: String): Role? =
        optionalArgument(name)?.asRole

    /**
     * Return the argument at the specified [index] as a [TextChannel] or `null` if there is no argument at that position, or it is not a [TextChannel].
     * @param ignoreCase whether the case of the name should be ignored or not
     */
    fun optionalChannel(
        name: String
    ): MessageChannel? =
        optionalArgument(name)?.asMessageChannel


    /**
     * Return the argument at the specified [index] or `null` if there is no argument at that position.
     * And sends a command help if there is no argument at that position.
     * @param context the context that executed the command
     */
    fun requiredArgument(name: String): SlashCommandEvent.OptionData =
        optionalArgument(name) ?: error("Could not find argument $name")

    /**
     * Return the argument at the specified [index] as an [Int] or `null` if there is no argument at that position.
     * If there is no [Int] at that position it sends a help message.
     * @param context the context that executed the command
     */
    fun int(name: String): Int = requiredArgument(name).asLong.toInt()

    /**
     * Return the argument at the specified [index] as a [Long] or `null` if there is no argument at that position.
     * If there is no [Long] at that position it sends a help message.
     * @param context the context that executed the command
     */
    fun long(name: String): Long = requiredArgument(name).asLong

    /**
     * Return the argument at the specified [index] as a [User] or `null` if there is no argument at that position.
     * If there is no [User] at that position it sends a help message.
     * @param ignoreCase whether the case of the name should be ignored or not
     * @param context the context that executed the command
     */
    fun user(name: String): User = requiredArgument(name).asUser!!

    /**
     * Return the argument at the specified [index] as a [Member] or `null` if there is no argument at that position.
     * If there is no [Member] at that position it sends a help message.
     * @param ignoreCase whether the case of the name should be ignored or not
     * @param context the context that executed the command
     */
    fun member(name: String): Member = requiredArgument(name).asMember!!

    /**
     * Return the argument at the specified [index] as a [Role] or `null` if there is no argument at that position.
     * If there is no [Role] at that position it sends a help message.
     * @param ignoreCase whether the case of the name should be ignored or not
     * @param context the context that executed the command
     */
    fun role(name: String): Role = requiredArgument(name).asRole!!

    /**
     * Return the argument at the specified [index] as a [TextChannel] or `null` if there is no argument at that position.
     * If there is no [TextChannel] at that position it sends a help message.
     * @param ignoreCase whether the case of the name should be ignored or not
     * @param context the context that executed the command
     */
    fun channel(name: String): MessageChannel = requiredArgument(name).asMessageChannel!!

}
