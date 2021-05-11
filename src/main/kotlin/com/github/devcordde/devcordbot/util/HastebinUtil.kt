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

package com.github.devcordde.devcordbot.util

import com.github.devcordde.devcordbot.constants.Constants
import io.ktor.client.*
import io.ktor.client.request.*
import kotlinx.serialization.Serializable
import java.util.concurrent.CompletableFuture

/**
 * Util for interacting with Hastebin.
 * @see Constants.hastebinUrl
 */
object HastebinUtil {

    /**
     * Posts the [text] to [Constants.hastebinUrl] using the [client].
     * @return a [CompletableFuture] containing the haste-url
     */
    suspend fun postToHastebin(text: String, client: HttpClient): String {
        return client.post<HastebinResponse>(Constants.hastebinUrl) {
            url {
                path("documents")
            }

            body = text
        }.key.hasteKeyToLink()
    }

    /**
     * Converts the [this@hasteKeyToLink] to a valid hastebin url.
     * @return the url string
     */
    private fun String.hasteKeyToLink(): String {
        var url = Constants.hastebinUrl.toString()
        url += if (url.endsWith("/")) "" else "/"
        return url + this
    }
}

@Serializable
private data class HastebinResponse(
    val key: String
)
