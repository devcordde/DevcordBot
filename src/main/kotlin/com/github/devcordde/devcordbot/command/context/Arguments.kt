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
 * Representation of a commands' arguments.
 * @property options a map storing a [OptionValue]s with their keys
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

    /**
     * Retrieves the string option by [name].
     */
    fun string(name: String): String = typedArgument<String>(name).value

    /**
     * Retrieves the optional string option by [name].
     */
    fun optionalString(name: String): String? = optionalTypedArgument<String>(name)?.value

    /**
     * Retrieves the an optional option by [name].
     */
    fun optionalArgument(name: String): OptionValue<*>? = options[name]

    @Suppress("unchecked_cast")
    private fun <T> optionalTypedArgument(name: String): OptionValue<T>? = optionalArgument(name) as OptionValue<T>?

    /**
     * Retrieves the an optional [Int] option by [name].
     */
    fun optionalInt(name: String): Int? = optionalTypedArgument<Int>(name)?.value

    /**
     * Retrieves the an optional [Long] option by [name].
     */
    fun optionalLong(name: String): Long? = optionalTypedArgument<Long>(name)?.value

    /**
     * Retrieves the an optional [User] option by [name].
     */
    fun optionalUser(name: String): User? =
        optionalTypedArgument<User>(name)?.value

    /**
     * Retrieves the an optional [Member] option by [name].
     */
    fun optionalMember(name: String): Member? =
        optionalTypedArgument<Member>(name)?.value

    /**
     * Retrieves the an optional [Role] option by [name].
     */
    fun optionalRole(name: String): Role? =
        optionalTypedArgument<Role>(name)?.value

    /**
     * Retrieves the an optional [MessageChannel] option by [name].
     */
    fun optionalChannel(
        name: String
    ): MessageChannel? =
        optionalTypedArgument<MessageChannel>(name)?.value

    /**
     * Retrieves the an required option by [name].
     */
    fun requiredArgument(name: String): OptionValue<*> =
        optionalArgument(name) ?: error("Could not find argument $name")

    /**
     * Retrieves the an required [Int] option by [name].
     */
    fun int(name: String): Int = long(name).toInt()

    /**
     * Retrieves the an required [Long] option by [name].
     */
    fun long(name: String): Long = typedArgument<Long>(name).value

    /**
     * Retrieves the an required [User] option by [name].
     */
    fun user(name: String): User = typedArgument<User>(name).value

    /**
     * Retrieves the an required [Member] option by [name].
     */
    fun member(name: String): Member = typedArgument<Member>(name).value

    /**
     * Retrieves the an required [Role] option by [name].
     */
    fun role(name: String): Role = typedArgument<Role>(name).value

    /**
     * Retrieves the an required [MessageChannel] option by [name].
     */
    fun channel(name: String): MessageChannel = typedArgument<MessageChannel>(name).value
}
