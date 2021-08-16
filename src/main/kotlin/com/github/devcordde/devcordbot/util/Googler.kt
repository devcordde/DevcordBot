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
import com.google.api.services.customsearch.v1.Customsearch
import com.google.api.services.customsearch.v1.model.Result
import com.google.api.services.customsearch.v1.model.Search
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Utility to google things.
 */
class Googler(private val bot: DevCordBot) {

    private val search =
        Customsearch.Builder(GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory(), null)
            .setApplicationName("DevcordBot")
            .build()

    /**
     * Googles the [query].
     */
    suspend fun google(query: String): List<Result> {
        val api = search.cse().list()
        val request = with(api.apply { q = query }) {
            key = bot.config.cse.key ?: error("Missing CSE key")
            cx = bot.config.cse.id ?: error("Missing CSE id")
            safe = "active"
            buildHttpRequest()
        }

        val response = withContext(Dispatchers.IO) {
            request.execute()
        }

        val list = response.parseAs(Search::class.java)

        return list?.items ?: emptyList()
    }
}
