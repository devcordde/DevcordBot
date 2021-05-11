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

package com.github.devcordde.devcordbot.command

/**
 * Category of an [AbstractCommand].
 * @property displayName the name that is displayed in help messages
 */
enum class CommandCategory(val displayName: String) {
    /**
     * General commands.
     */
    GENERAL("Allgemein"),

    /**
     * Bot owner exclusive commands.
     */
    BOT_OWNER("Bot-Entwickler"),

    /**
     * Moderation Commands
     */
    MODERATION("Moderation"),

    /**
     * It's fun.
     */
    FUN("Spaß")
}
