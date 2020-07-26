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

import com.github.devcordde.devcordbot.util.GithubUtil
import com.github.devcordde.devcordbot.util.executeAsync
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future
import net.dv8tion.jda.api.entities.Message
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.CompletableFuture

/**
 * ContentFetcher
 */
class ContentFetcher(
    private val httpClient: OkHttpClient,
    private val github: GithubUtil,
    private val executor: CoroutineDispatcher
) {

    /**
     * Trigger AutoHelp on Message.
     */
    suspend fun fetchMessageContents(message: Message): List<CompletableFuture<out List<String?>>> {
        val input = message.contentRaw

        // Asynchronously fetch potential content
        val attachments = fetchAttachments(message)
        val messageContents: MutableList<CompletableFuture<List<String?>>> = if (input.isNotBlank()) {
            val hastebinMatches = findInput(HASTEBIN_PATTERN, input, ::fetchHastebin)
            val pastebinMatches = findInput(PASTEBIN_PATTERN, input, ::fetchPastebin)
            val ghostbinMatches = findInput(GHOSTBIN_PATTERN, input, ::fetchGhostbin)
            val githubMatches = findInputs(GITHUB_GIST_PATTERN, input, ::fetchGithub)

            mutableListOf(hastebinMatches, pastebinMatches, ghostbinMatches, githubMatches)
        } else mutableListOf()
        return messageContents.plus(attachments).toList()
    }

    private fun findInput(
        pattern: Regex,
        input: String,
        fetcher: (MatchResult) -> CompletableFuture<String?>
    ): CompletableFuture<List<String?>> {
        return GlobalScope.future(executor) {
            pattern.findAll(input).toList().map {
                fetcher(it).await()
            }
        }
    }

    private fun findInputs(
        pattern: Regex,
        input: String,
        fetcher: (MatchResult, String) -> CompletableFuture<List<String?>>
    ): CompletableFuture<List<String?>> {
        return GlobalScope.future(executor) {
            pattern.findAll(input).toList().map {
                fetcher(it, input).await()
            }.flatten()
        }
    }

    private suspend fun fetchAttachments(message: Message): CompletableFuture<List<String?>> {
        return GlobalScope.future(executor) {
            message.attachments.filter { !it.isVideo && !it.isImage }.map {
                fetchAttachment(it)
            }
        }
    }

    private suspend fun fetchAttachment(attachment: Message.Attachment): String {
        val stream = attachment.retrieveInputStream().await()
        return BufferedReader(InputStreamReader(stream)).use { reader ->
            reader.readText()
        }
    }

    private fun fetchHastebin(match: MatchResult): CompletableFuture<String?> {
        val domain = match.groupValues[1]
        val pasteId = match.groupValues[2]
        val rawPaste = "https://$domain/raw/$pasteId"
        return fetchWebContent(rawPaste)
    }

    private fun fetchPastebin(match: MatchResult): CompletableFuture<String?> {
        val pasteId = match.groupValues[1]
        val rawPaste = "https://pastebin.com/raw/$pasteId"
        return fetchWebContent(rawPaste)
    }

    private fun fetchGhostbin(match: MatchResult): CompletableFuture<String?> {
        val pasteId = match.groupValues[1]
        val rawPaste = "https://ghostbin.co/paste/$pasteId/raw"
        return fetchWebContent(rawPaste)
    }

    private fun fetchGithub(match: MatchResult, url: String): CompletableFuture<List<String?>> {
        val host = match.groupValues[1]
        val gistId = match.groupValues[3]
        if (host == "gist.githubusercontent.com") {
            return fetchWebContent(url).thenApply { listOf(it) }
        }
        return github.retrieveGistFiles(gistId).thenApplyAsync { fileUrls ->
            fileUrls.map { fetchWebContent(it).join() }
        }
    }

    /**
     * Fetch content from given Url.
     */
    private fun fetchWebContent(url: String): CompletableFuture<String?> {
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        return httpClient.newCall(request).executeAsync().thenApply { response ->
            response.body.use {
                it?.string()
            }
        }
    }

    companion object {

        // https://regex101.com/r/u0QAR6/6
        private val HASTEBIN_PATTERN =
            "(?:https?:\\/\\/)?(?:(?:www\\.)?)?(hastebin\\.com|hasteb\\.in|paste\\.helpch\\.at)\\/(?:raw\\/)?(.+?(?=\\.|\$|\\/|#))".toRegex()

        // https://regex101.com/r/N8NBDz/2
        private val PASTEBIN_PATTERN =
            "(?:https?:\\/\\/(?:www\\.)?)?pastebin\\.com\\/(?:raw\\/)?(.+?(?=\\.|\$|\\/|#))".toRegex()

        // https://regex101.com/r/CyjiKt/2
        private val GHOSTBIN_PATTERN =
            "(?:https?:\\/\\/(?:www\\.)?)?(?:ghostbin\\.co)\\/(?:paste\\/)?(.+?(?=\\.|\$|\\/))(?:\\/raw)?".toRegex()

        // https://regex101.com/r/AlVYjn/2
        private val GITHUB_GIST_PATTERN =
            "(?:https?:\\/\\/)?(gist\\.github\\.com|gist.githubusercontent.com)\\/(.+?(?=\\.|\$|\\/))\\/(.+?(?=\\.|\$|\\/|#))".toRegex()

    }
}
