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

package com.github.seliba.devcordbot.command.impl

import com.github.seliba.devcordbot.command.ErrorHandler
import com.github.seliba.devcordbot.command.context.Context
import com.github.seliba.devcordbot.constants.Constants
import com.github.seliba.devcordbot.constants.Embeds
import com.github.seliba.devcordbot.constants.Emotes
import com.github.seliba.devcordbot.dsl.editMessage
import mu.KotlinLogging
import net.dv8tion.jda.api.utils.data.DataObject
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.CoroutineContext

/**
 * Implementation of [ErrorHandler] that reports an error log to hastebin.
 */
class HastebinErrorHandler : ErrorHandler {
    private val logger = KotlinLogging.logger { }

    /**
     * @see ErrorHandler.handleException
     */
    override fun handleException(
        exception: Throwable,
        context: Context,
        thread: Thread,
        coroutineContext: CoroutineContext
    ) {
        logger.error(exception) { "An error occurred whilst command execution" }
        context.respond(
            Embeds.error(
                "Ein Fehler ist aufgetreten!",
                "${Emotes.LOADING} Bitte warte einen Augenblick, während ich versuche mehr Informationen über den Fehler herauszufinden."
            )
        ).submit().thenCompose { message ->
            val error = collectErrorInformation(exception, context, thread, coroutineContext)
            postErrorToHastebin(error, context.jda.httpClient).thenApply { it to message }
        }.thenAccept { (url, message) ->
            message.editMessage(
                Embeds.error(
                    "Es ist ein Fehler aufgetreten!",
                    "Bitte zeige einem Entwickler [diesen]($url) Link um Hilfe zu erhalten."
                )
            ).queue()
        }
    }

    private fun postErrorToHastebin(text: String, client: OkHttpClient): CompletableFuture<String> {
        val body = text.toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder()
            .url(Constants.hastebinUrl.newBuilder().addPathSegment("documents").build())
            .post(body)
            .build()
        val future = CompletableFuture<String>()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                future.completeExceptionally(e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    future.complete(
                        Constants.hastebinUrl.newBuilder().addPathSegment(
                            DataObject.fromJson(response.body!!.string()).getString(
                                "key"
                            )
                        ).toString()
                    )
                }
            }
        })
        return future
    }

    private fun collectErrorInformation(
        e: Throwable,
        context: Context,
        thread: Thread,
        coroutineContext: CoroutineContext
    ): String {
        val information = StringBuilder()
        val channel = context.channel
        information.append("TextChannel: ").append("#").append(channel.name)
            .append("(").append(channel.id).appendln(")")
        val guild = context.guild
        information.append("Guild: ").append(guild.name).append("(").append(guild.id)
            .appendln(")")
        val executor = context.author
        information.append("Executor: ").append("@").append(executor.name).append("#")
            .append(executor.discriminator).append("(").append(executor.id).appendln(")")
        val selfMember = guild.selfMember
        information.append("Permissions: ").appendln(selfMember.permissions)
        information.append("Channel permissions: ").appendln(selfMember.getPermissions(channel))
        information.append("Timestamp: ").appendln(LocalDateTime.now())
        information.append("Thread: ").appendln(thread)
        information.append("Coroutine: ").appendln(coroutineContext)
        information.append("Stacktrace: ").appendln().append(e.stringify())
        return information.toString()
    }

    private fun Throwable.stringify(): String {
        val stringWriter = StringWriter()
        val printWriter = PrintWriter(stringWriter)
        return stringWriter.use {
            printWriter.use {
                printStackTrace(printWriter)
                stringWriter.buffer.toString()
            }
        }
    }
}
