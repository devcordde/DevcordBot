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

import dev.kord.core.entity.Member
import dev.kord.core.entity.Role
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.MessageChannel
import dev.kord.core.entity.interaction.OptionValue


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
    private val options: Map<String, OptionValue<*>>
) {

    /**
     * Returns the argument with [name].
     */
    fun argument(name: String): OptionValue<*> =
        optionalArgument(name) ?: error("Argument $name not found")

    @Suppress("unchecked_cast")
    private fun <T> typedArgument(name: String): OptionValue<T> = requiredArgument(name) as OptionValue<T>

    fun string(name: String): String = typedArgument<String>(name).value

    fun optionalString(name: String): String? = optionalTypedArgument<String>(name)?.value

    /**
     * Return the argument at the specified [index] or `null` if there is no argument at that position.
     */
    fun optionalArgument(name: String): OptionValue<*>? = options[name]


    @Suppress("unchecked_cast")
    private fun <T> optionalTypedArgument(name: String): OptionValue<T>? = optionalArgument(name) as OptionValue<T>?

    /**
     * Return the argument at the specified [index] as an [Int] or `null` if there is no argument at that position, or it is not an [Int].
     */
    fun optionalInt(name: String): Int? = optionalLong(name)?.toInt()

    /**
     * Return the argument at the specified [index] as a [Long] or `null` if there is no argument at that position, or it is not a [Long].
     */
    fun optionalLong(name: String): Long? = optionalTypedArgument<Long>(name)?.value

    /**
     * Return the argument at the specified [index] as a [User] or `null` if there is no argument at that position, or it is not a [User].
     * @param ignoreCase whether the case of the name should be ignored or not
     */
    fun optionalUser(name: String): User? =
        optionalTypedArgument<User>(name)?.value

    /**
     * Return the argument at the specified [index] as a [Member] or `null` if there is no argument at that position, or it is not a [Member].
     * @param ignoreCase whether the case of the name should be ignored or not
     */
    fun optionalMember(name: String): Member? =
        optionalTypedArgument<Member>(name)?.value

    /**
     * Return the argument at the specified [index] as a [Role] or `null` if there is no argument at that position, or it is not a [Role].
     * @param ignoreCase whether the case of the name should be ignored or not
     */
    fun optionalRole(name: String): Role? =
        optionalTypedArgument<Role>(name)?.value

    /**
     * Return the argument at the specified [index] as a [TextChannel] or `null` if there is no argument at that position, or it is not a [TextChannel].
     * @param ignoreCase whether the case of the name should be ignored or not
     */
    fun optionalChannel(
        name: String
    ): MessageChannel? =
        optionalTypedArgument<MessageChannel>(name)?.value


    /**
     * Return the argument at the specified [index] or `null` if there is no argument at that position.
     * And sends a command help if there is no argument at that position.
     * @param context the context that executed the command
     */
    fun requiredArgument(name: String): OptionValue<*> =
        optionalArgument(name) ?: error("Could not find argument $name")

    /**
     * Return the argument at the specified [index] as an [Int] or `null` if there is no argument at that position.
     * If there is no [Int] at that position it sends a help message.
     * @param context the context that executed the command
     */
    fun int(name: String): Int = long(name).toInt()

    /**
     * Return the argument at the specified [index] as a [Long] or `null` if there is no argument at that position.
     * If there is no [Long] at that position it sends a help message.
     * @param context the context that executed the command
     */
    fun long(name: String): Long = typedArgument<Long>(name).value

    /**
     * Return the argument at the specified [index] as a [User] or `null` if there is no argument at that position.
     * If there is no [User] at that position it sends a help message.
     * @param ignoreCase whether the case of the name should be ignored or not
     * @param context the context that executed the command
     */
    fun user(name: String): User = typedArgument<User>(name).value

    /**
     * Return the argument at the specified [index] as a [Member] or `null` if there is no argument at that position.
     * If there is no [Member] at that position it sends a help message.
     * @param ignoreCase whether the case of the name should be ignored or not
     * @param context the context that executed the command
     */
    fun member(name: String): Member = typedArgument<Member>(name).value

    /**
     * Return the argument at the specified [index] as a [Role] or `null` if there is no argument at that position.
     * If there is no [Role] at that position it sends a help message.
     * @param ignoreCase whether the case of the name should be ignored or not
     * @param context the context that executed the command
     */
    fun role(name: String): Role = typedArgument<Role>(name).value

    /**
     * Return the argument at the specified [index] as a [TextChannel] or `null` if there is no argument at that position.
     * If there is no [TextChannel] at that position it sends a help message.
     * @param ignoreCase whether the case of the name should be ignored or not
     * @param context the context that executed the command
     */
    fun channel(name: String): MessageChannel = typedArgument<MessageChannel>(name).value

}
