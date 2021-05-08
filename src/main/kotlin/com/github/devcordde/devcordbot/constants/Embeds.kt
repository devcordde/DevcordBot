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

package com.github.devcordde.devcordbot.constants

import com.github.devcordde.devcordbot.command.AbstractCommand
import com.github.devcordde.devcordbot.command.root.AbstractRootCommand
import com.github.devcordde.devcordbot.dsl.EmbedCreator
import dev.kord.rest.builder.message.EmbedBuilder

/**
 * Some presets for frequently used embeds.
 */
@Suppress("unused")
object Embeds {

    /**
     * Creates a info embed with the given [title] and [description] and applies the [builder] to it.
     * @see EmbedCreator
     */
    inline fun info(title: String, description: String? = null, builder: EmbedCreator = {}): EmbedBuilder =
        EmbedBuilder().apply {
            title(Emotes.INFO, title)
            this.description = description
            color = Colors.BLUE
        }.apply(builder)

    /**
     * Creates a success embed with the given [title] and [description] and applies the [builder] to it.
     * @see EmbedCreator
     * @see EmbedBuilder
     */
    inline fun success(title: String, description: String? = null, builder: EmbedCreator = {}): EmbedBuilder =
        EmbedBuilder().apply {
            title(Emotes.SUCCESS, title)
            this.description = description
            color = Colors.LIGHT_GREEN
        }.apply(builder)

    /**
     * Creates a error embed with the given [title] and [description] and applies the [builder] to it.
     * @see EmbedCreator
     * @see EmbedBuilder
     */
    inline fun error(title: String, description: String?, builder: EmbedCreator = {}): EmbedBuilder =
        EmbedBuilder().apply {
            title(Emotes.ERROR, title)
            this.description = description
            color = Colors.LIGHT_RED
        }.apply(builder)

    /**
     * Creates a warning embed with the given [title] and [description] and applies the [builder] to it.
     * @see EmbedCreator
     * @see EmbedBuilder
     */
    inline fun warn(title: String, description: String?, builder: EmbedCreator = {}): EmbedBuilder =
        EmbedBuilder().apply {
            title(Emotes.WARN, title)
            this.description = description
            color = Colors.YELLOW
        }.apply(builder)

    /**
     * Creates a loading embed with the given [title] and [description] and applies the [builder] to it.
     * @see EmbedCreator
     * @see EmbedBuilder
     */
    inline fun loading(title: String, description: String?, builder: EmbedCreator = {}): EmbedBuilder =
        EmbedBuilder().apply {
            title(Emotes.LOADING, title)
            this.description = description
            color = Colors.DARK_BUT_NOT_BLACK
        }.apply(builder)

    /**
     * Creates a help embed for [command].
     */
    fun command(command: AbstractCommand): EmbedBuilder {
        return info("${command.name} - Hilfe", command.description) {
            field {
                name = "Name"
                value = command.name
            }
            field {
                name = "Berechtigung"
                value = command.permission.name
            }
            val subCommands =
                (command as? AbstractRootCommand)?.registeredCommands?.map(AbstractCommand::name)
            if (!subCommands.isNullOrEmpty()) {
                field {
                    name = "Unterbefehle"
                    value = subCommands.joinToString("\n")
                }
            }
        }
    }

    @PublishedApi
    internal fun EmbedBuilder.title(emote: String, title: String) {
        this.title = "$emote $title"
    }
}
