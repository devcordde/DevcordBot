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

import io.ktor.http.*
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
    lateinit var hastebinUrl: Url

    /**
     * Dateformat used in the bot.
     */
    val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter
        .ofLocalizedDateTime(FormatStyle.SHORT)
        .withLocale(Locale.GERMAN)
        .withZone(ZoneId.of("Europe/Berlin")) // To lazy to set server timezone :P

    /**
     *  The max length of the tag content in the database.
     */
    const val DATABASE_TAG_CONTENT_LENGTH: Int = 3000
}
