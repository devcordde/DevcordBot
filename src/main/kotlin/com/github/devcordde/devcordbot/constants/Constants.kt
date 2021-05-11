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

package com.github.devcordde.devcordbot.constants

import okhttp3.HttpUrl
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

/**
 * Miscellaneous constants used in the bot.
 */
object Constants {

    /**
     * The prefix used for commands.
     */
    val prefix: Regex = "^((?i)s(u(do)?)?(?-i)|!)".toRegex()

    /**
     * Prefix used for help messages.
     */
    const val firstPrefix: String = "sudo"

    /**
     * URL that is used for pasting text.
     */
    lateinit var hastebinUrl: HttpUrl

    /**
     * Dateformat used in the bot.
     */
    val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter
        .ofLocalizedDateTime(FormatStyle.SHORT)
        .withLocale(Locale.GERMAN)
        .withZone(ZoneId.of("Europe/Berlin")) // To lazy to set server timezone :P

    /**
     * Regex that matches jdoodle code blocks.
     */
    val JDOODLE_REGEX: Regex = "```([A-z]+)\n([^`]*)```".toRegex(RegexOption.MULTILINE)


    /**
     * Regex that matches discord code blocks.
     * https://regex101.com/r/SBv5RG/3
     */
    val CODE_BLOCK_REGEX: Regex =
        "```(actionscript3|apache|applescript|asp|brainfuck|c|cfm|clojure|cmake|coffee-script|coffeescript|coffee|cpp|cs|csharp|css|csv|bash|diff|elixir|erb|go|haml|http|java|javascript|json|jsx|less|lolcode|make|markdown|matlab|nginx|objectivec|pascal|PHP|Perl|python|profile|rust|salt|saltstate|shell|sh|zsh|bash|sql|scss|sql|svg|swift|rb|jruby|ruby|smalltalk|vim|viml|volt|vhdl|vue|xml|yaml)?([^`]*)```".toRegex(
            RegexOption.MULTILINE
        )

    /**
     * A regex matching a lot of common (non-user) packages.
     *
     * Results in (?:<packages>).*
     */
    val KNOWN_PACKAGES: Regex = listOf(
        "java", "javax", "javafx", // Java
        "com.google", "org.apache", "com.jetbrains", "org.jetbrains", "okhttp3", // publishers of big libs
        "net.minecraft", "org.bukkit", "org.spigotmc", "net.md_5", "co.aikar", "com.destroystokyo", // mc servers
        "net.milkbowl", "me.clip", "org.sk89q", "com.onarandombox" // known libraries
    ).joinToString(prefix = "(?:", separator = "|", postfix = ").*").toRegex()

}
