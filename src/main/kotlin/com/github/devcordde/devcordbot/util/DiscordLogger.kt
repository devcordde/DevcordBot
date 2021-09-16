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

import com.github.devcordde.devcordbot.config.Config
import dev.kord.core.entity.User
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val loggers: MutableMap<String, Logger> = mutableMapOf()

/**
 * Utility to log discord events.
 */
class DiscordLogger {

    /**
     * Logs an event for [user] into [Config.Discord.logChannel].
     */
    fun logEvent(user: User? = null, title: String, message: () -> String) {
        val prefix = user?.let { user.tag + " (${user.id.asString}) " } ?: ""

        logger(name(message)).info(marker, "$title: $prefix${message()}")
    }

    private fun logger(name: String) =
        loggers.computeIfAbsent(name) { LoggerFactory.getLogger(name) }
}

@Suppress("NOTHING_TO_INLINE")
private inline fun name(noinline func: () -> String): String {
    val name = func.javaClass.name
    val slicedName = when {
        name.contains("Kt$") -> name.substringBefore("Kt$")
        name.contains("$") -> name.substringBefore("$")
        else -> name
    }
    return slicedName
}
