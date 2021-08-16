// Code inspired by: https://github.com/RainbowDashLabs/reputation-bot/blob/development/src/main/java/de/chojo/repbot/util/DiscordWebhookAppender.java
/*
 * Copyright 2021 Daniel Scherf & Michael Rittmeister & Julian König
 *
 *    Licensed under the Apache License, Version 2.0 (the "License")
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

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.LoggingEvent
import ch.qos.logback.classic.spi.ThrowableProxy
import ch.qos.logback.core.AppenderBase
import ch.qos.logback.core.UnsynchronizedAppenderBase
import com.github.devcordde.devcordbot.constants.MAX_CONTENT_LENGTH
import dev.kord.common.Color
import dev.kord.common.entity.Snowflake
import dev.kord.common.kColor
import dev.kord.rest.builder.message.create.WebhookMessageCreateBuilder
import dev.kord.rest.builder.message.create.embed
import dev.kord.rest.request.KtorRequestHandler
import dev.kord.rest.service.RestClient
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import kotlinx.coroutines.*
import kotlinx.datetime.Instant
import org.slf4j.MarkerFactory
import kotlin.coroutines.CoroutineContext
import kotlin.math.abs
import java.awt.Color as JColor

val marker = MarkerFactory.getMarker("LOG_TO_DISCORD")

private data class WebhookInfo(val id: Snowflake, val token: String)

/**
 * Implementation of [DiscordWebhookAppender] sending events to a Discord Webhook.
 */
class DiscordWebhookAppender : UnsynchronizedAppenderBase<LoggingEvent>(), CoroutineScope {

    var webhookUrl: String? = null

    override val coroutineContext: CoroutineContext = Dispatchers.IO + SupervisorJob()
    private val webhookClient = run {
        val requestHandler = KtorRequestHandler(HttpClient(CIO))
        val client = RestClient(requestHandler)

        client.webhook
    }
    private lateinit var webhook: WebhookInfo

    private fun parseUrl(url: String): WebhookInfo {
        val (id, token) = url.substringAfter("/api/webhooks/").split('/')

        return WebhookInfo(Snowflake(id), token)
    }

    /**
     * Initializes the appender.
     */
    override fun start() {
        webhook = parseUrl(webhookUrl!!)
        started = true
    }

    private suspend fun callWebhook(builder: WebhookMessageCreateBuilder.() -> Unit) {
        val (id, token) = webhook

        webhookClient.executeWebhook(id, token, builder = builder)
    }

    private suspend fun appendAsync(eventObject: LoggingEvent) {
        callWebhook {
            embed {
                title = eventObject.loggerName
                timestamp = Instant.fromEpochMilliseconds(eventObject.timeStamp)
                color = resolveColor(eventObject.level)
                if (eventObject.hasCallerData()) {
                    footer {
                        val source = eventObject.callerData.first()
                        text = (
                                source.fileName
                                    ?: "<unknown file>"
                                ) + "#" + source.methodName + ":" + source.lineNumber + "@" + eventObject.threadName
                    }
                }

                val descr = eventObject.formattedMessage.limit(MAX_CONTENT_LENGTH)
                description = descr

                // append throwable if attached
                val throwable = (eventObject.throwableProxy as ThrowableProxy?)?.throwable
                if (throwable != null) {
                    // the linebreaks and code blocks also require some characters (we hardcode this value)
                    val lineChars = "\n\nst``````".length

                    @Suppress("DEPRECATION")
                    val strippedException = throwable.stackTraceToString().strip()
                    val chunks = strippedException.split("Caused by:")
                    val exceptionLength = strippedException.length + chunks.size * lineChars
                    val embedLenght = MAX_CONTENT_LENGTH - descr.length - eventObject.loggerName.length
                    val abbreviate = exceptionLength >= embedLenght
                    val abbreviateChars =
                        abs(MAX_CONTENT_LENGTH - (exceptionLength + embedLenght)) / chunks.size
                    var first = true
                    chunks.forEach { chunk ->
                        val fieldTitle = if (first) throwable::class.simpleName ?: "<unknown class>" else "Caused by"
                        var text = if (abbreviate) chunk.limit(abbreviateChars - 3) else chunk
                        text = "```st%n%s%n```".format(text)

                        field {
                            this.name = fieldTitle
                            value = text
                            inline = false
                        }
                        first = false
                    }
                }
            }
        }
    }

    /**
     * Appends [eventObject].
     */
    override fun append(eventObject: LoggingEvent) {
        if (eventObject.marker == marker)
            launch {
                appendAsync(eventObject)
            }
    }

    /**
     * Closes needed resources.
     */
    override fun stop() {
        coroutineContext.cancel()
        started = false
    }

    private fun resolveColor(level: Level): Color {
        fun produceJavaColor(): JColor {
            if (level === Level.TRACE) {
                return JColor.WHITE
            }
            if (level === Level.DEBUG) {
                return JColor.BLUE
            }
            if (level === Level.INFO) {
                return JColor.GREEN
            }
            if (level === Level.WARN) {
                return JColor.YELLOW
            }
            return if (level === Level.ERROR) {
                JColor.RED
            } else JColor.GRAY
        }

        return produceJavaColor().kColor
    }
}
