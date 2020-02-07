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

package com.github.seliba.devcordbot.constants

import com.github.seliba.devcordbot.command.AbstractCommand
import com.github.seliba.devcordbot.command.AbstractSubCommand
import com.github.seliba.devcordbot.dsl.EmbedConvention
import com.github.seliba.devcordbot.dsl.EmbedCreator
import com.github.seliba.devcordbot.util.Constants

/**
 * Some presets for frequently used embeds.
 */
@Suppress("unused")
object Embeds {

    /**
     * Creates a info embed with the given [title] and [description] and applies the [builder] to it.
     * @see EmbedCreator
     * @see EmbedConvention
     */
    fun info(title: String, description: String? = null, builder: EmbedCreator = {}): EmbedConvention =
        EmbedConvention().apply {
            title(Emotes.INFO, title)
            this.description = description
            color = Colors.BLUE
        }.apply(builder)

    /**
     * Creates a success embed with the given [title] and [description] and applies the [builder] to it.
     * @see EmbedCreator
     * @see EmbedConvention
     */
    fun success(title: String, description: String? = null, builder: EmbedCreator = {}): EmbedConvention =
        EmbedConvention().apply {
            title(Emotes.SUCCESS, title)
            this.description = description
            color = Colors.LIGHT_GREEN
        }.apply(builder)

    /**
     * Creates a error embed with the given [title] and [description] and applies the [builder] to it.
     * @see EmbedCreator
     * @see EmbedConvention
     */
    fun error(title: String, description: String?, builder: EmbedCreator = {}): EmbedConvention =
        EmbedConvention().apply {
            title(Emotes.ERROR, title)
            this.description = description
            color = Colors.LIGHT_RED
        }.apply(builder)

    /**
     * Creates a warn embed with the given [title] and [description] and applies the [builder] to it.
     * @see EmbedCreator
     * @see EmbedConvention
     */
    fun warn(title: String, description: String?, builder: EmbedCreator = {}): EmbedConvention =
        EmbedConvention().apply {
            title(Emotes.WARN, title)
            this.description = description
            color = Colors.YELLOW
        }.apply(builder)

    /**
     * Creates a loading embed with the given [title] and [description] and applies the [builder] to it.
     * @see EmbedCreator
     * @see EmbedConvention
     */
    fun loading(title: String, description: String?, builder: EmbedCreator = {}): EmbedConvention =
        EmbedConvention().apply {
            title(Emotes.LOADING, title)
            this.description = description
            color = Colors.DARK_BUT_NOT_BLACK
        }.apply(builder)

    /**
     * Creates a help embed for [command].
     */
    fun command(command: AbstractCommand): EmbedConvention {
        return info("${command.displayName} - Hilfe", command.description) {
            addField("Aliases", command.aliases.joinToString(prefix = "`", separator = "`, `", postfix = "`"))
            addField("Usage", formatUsage(command))
            addField("Permission", command.permissions.name)
        }
    }

    private fun formatUsage(command: AbstractCommand): String {
        val builder = StringBuilder(Constants.prefix)
        if (command is AbstractSubCommand) {
            builder.append(command.parent.name).append(' ')
        }

        builder.append(command.name).append(' ').append(command.usage)
        if (command is AbstractSubCommand) {
            builder.append(" - ").append(command.description)
        }
        return builder.toString()
    }

    private fun EmbedConvention.title(emote: String, title: String) = title("$emote $title")
}
