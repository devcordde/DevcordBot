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

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

/**
 * Miscellaneous constants used in the bot.
 */
object Constants {

    /**
     * The prefix used for commands.
     */
    val prefix: Regex = "((?i)sudo|s(?-i)|!)(?s)(.*)".toRegex()

    /**
     * Prefix used for help messages.
     */
    const val firstPrefix: String = "sudo"

    /**
     * URL that is used for pasting text.
     */
    val hastebinUrl: HttpUrl = "https://haste.schlaubi.me".toHttpUrl()

}
