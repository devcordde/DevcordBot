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

import com.github.devcordde.devcordbot.constants.Constants
import net.dv8tion.jda.api.utils.data.DataObject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
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
    fun postErrorToHastebin(text: String, client: OkHttpClient): CompletableFuture<String> {
        val body = text.toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder()
            .url(Constants.hastebinUrl.newBuilder().addPathSegment("documents").build())
            .post(body)
            .build()
        return client.newCall(request).executeAsync().thenApply {
            it.use { response ->
                response.body!!.use { body ->
                    Constants.hastebinUrl.newBuilder().addPathSegment(
                        DataObject.fromJson(body.string()).getString(
                            "key"
                        )
                    ).toString()
                }
            }
        }
    }
}
