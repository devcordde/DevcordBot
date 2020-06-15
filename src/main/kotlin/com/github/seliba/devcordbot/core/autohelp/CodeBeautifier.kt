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

package com.github.seliba.devcordbot.core.autohelp

import com.github.seliba.devcordbot.util.executeAsync
import net.dv8tion.jda.api.utils.data.DataObject
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.CompletableFuture

/**
 * Helper class to format java code.
 */
class CodeBeautifier(private val httpClient: OkHttpClient) {

    private val apiUrl =
        "https://tools.tutorialspoint.com/format_javascript.php" // IDK why it's called JS it's literally from https://www.tutorialspoint.com/online_java_formatter.htm

    /**
     * Formats the [input] as programming code.
     */
    fun formatCode(input: String): CompletableFuture<String> {
        val body = FormBody.Builder()
            .add("code", input)
            .build()
        val request = Request.Builder()
            .url(apiUrl)
            .post(body)
            .build()
        return httpClient.newCall(request).executeAsync()
            .thenApply { DataObject.fromJson(it.body!!.string()).getString("code") }
    }
}
