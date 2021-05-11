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

import com.github.devcordde.devcordbot.core.DevCordBot
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.customsearch.v1.CustomSearchAPI
import com.google.api.services.customsearch.v1.model.Result

/**
 * Utility to google things.
 */
class Googler(private val bot: DevCordBot) {

    private val search =
        CustomSearchAPI.Builder(GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory(), null)
            .setApplicationName("DevcordBot")
            .build()

    /**
     * Googles the [query].
     */
    fun google(query: String): List<Result> {
        return with(search.cse().list().apply { q = query }) {
            key = bot.config.cse.key ?: error("Missing CSE key")
            cx = bot.config.cse.id ?: error("Missing CSE id")
            safe = "active"
            execute()
        }.items ?: emptyList()
    }
}
