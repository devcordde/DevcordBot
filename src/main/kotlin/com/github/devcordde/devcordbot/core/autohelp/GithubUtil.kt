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

package com.github.devcordde.devcordbot.core.autohelp

import com.github.devcordde.devcordbot.util.executeAsync
import net.dv8tion.jda.api.utils.data.DataObject
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.CompletableFuture

/**
 * Utility to interact with github gist api
 */
class GithubUtil(private val client: OkHttpClient) {

    /**
     * Returns a [CompletableFuture] containing list of url that contain raw content of files by [gistId].
     */
    fun retrieveGistFiles(gistId: String): CompletableFuture<List<String>> {
        val request = Request.Builder()
            .url(GET_GIST_ENDPOINT.newBuilder().addPathSegment(gistId).build())
            .get()
            .build()
        val call = client.newCall(request)
        return call.executeAsync().thenApply {
            val json =
                it.body?.string()?.let { it1 -> DataObject.fromJson(it1) } ?: return@thenApply emptyList<String>()
            val files = json.getObject("files")
            files.keys().map { key ->
                files.getObject(key).getString("raw_url")
            }
        }
    }

    companion object {
        private val API_BASE = "https://api.github.com".toHttpUrl()
        private val GET_GIST_ENDPOINT = API_BASE.newBuilder().addPathSegment("gists").build()
    }
}