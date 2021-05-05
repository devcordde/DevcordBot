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

package com.github.devcordde.devcordbot.menu

import com.github.devcordde.devcordbot.command.context.Context
import com.github.devcordde.devcordbot.constants.Colors
import com.github.devcordde.devcordbot.constants.Embeds
import com.github.devcordde.devcordbot.core.DevCordBot
import com.github.devcordde.devcordbot.dsl.embed
import dev.kord.common.Color
import dev.kord.common.annotation.KordPreview
import dev.kord.core.behavior.edit
import dev.kord.core.entity.Message
import dev.kord.core.entity.ReactionEmoji
import dev.kord.core.entity.User
import dev.kord.core.event.message.ReactionAddEvent
import dev.kord.core.live.LiveMessage
import dev.kord.core.live.live
import dev.kord.core.live.onReactionAdd
import dev.kord.core.live.onShutDown
import dev.kord.x.emoji.DiscordEmoji
import dev.kord.x.emoji.Emojis
import dev.kord.x.emoji.addReaction
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit
import kotlin.math.ceil
import kotlin.math.min

/**
 * Creates a new [Paginator].
 *
 * @param items the list of items to paginate
 * @param user the user that is allowed to paginate
 * @param context the context to send the paginatable list to
 * @param timeoutMillis amount of milliseconds after the paginator should timeout
 * @param firstPage the first page that should be shown
 * @param itemsPerPage the amount of items per page
 * @param loadingTitle the title of the embed displayed while loading
 * @param loadingDescription the description of the embed displayed while loading
 * @param color the color of the embed
 */
@Suppress("FunctionName")
suspend fun Paginator(
    items: List<CharSequence>,
    user: User,
    context: Context,
    title: String,
    timeoutMillis: Long = TimeUnit.SECONDS.toMillis(15),
    firstPage: Int = 1,
    itemsPerPage: Int = 8,
    loadingTitle: String = "Bitte warten!",
    loadingDescription: String = "Bitte warte, während die Liste geladen wird...",
    color: Color = Colors.BLUE
): Paginator {
    require(itemsPerPage > 0) { "Items per page must be > 0" }
    require(items.isNotEmpty()) { "Items must not be empty" }
    val pages = ceil(items.size.toDouble() / itemsPerPage).toInt()
    require(firstPage <= pages) { "First page must exist" }

    val message = context.respond(Embeds.loading(loadingTitle, loadingDescription))
    if (pages > 1) {
        Paginator.addReactions(message)
    }

    return Paginator(
        context.bot,
        items,
        user,
        title,
        timeoutMillis,
        itemsPerPage,
        color,
        firstPage,
        pages,
        message.live(),
    )
}

/**
 * Reaction and embed based paginator for string lists.
 * @see Paginator
 */
@OptIn(KordPreview::class)
class Paginator internal constructor(
    private val bot: DevCordBot,
    private val items: List<CharSequence>,
    private val user: User,
    private val title: String,
    private val timeoutMillis: Long = TimeUnit.SECONDS.toMillis(15),
    private val itemsPerPage: Int = 8,
    private val color: Color = Colors.BLUE,
    private var currentPage: Int,
    private val pages: Int,
    private val message: LiveMessage,
) : CoroutineScope by bot {

    init {
        launch {
            paginate(currentPage)
        }
        if (pages == 1) {
            launch {
                close()
            }
        } else {
            rescheduleTimeout()

            message.onShutDown {
                canceller.cancel()
            }

            message.onReactionAdd {
                onReaction(it)
            }
        }
    }

    private lateinit var canceller: Job

    private suspend fun paginate(destinationPage: Int) {
        currentPage = destinationPage
        val start: Int = (destinationPage - 1) * itemsPerPage
        val end = min(items.size, destinationPage * itemsPerPage)
        val rows = items.subList(start, end)
        message.message.edit { embed = renderEmbed(rows) }
    }

    private fun renderEmbed(rows: List<CharSequence>) = embed {
        color = this@Paginator.color
        title = this@Paginator.title
        val rowBuilder = StringBuilder()
        rows.indices.forEach {
            rowBuilder.append('`').append(it + (itemsPerPage * (currentPage - 1)) + 1).append("`. ")
                .appendLine(rows[it])
        }
        description = rowBuilder.toString()
        footer {
            text = "Seite $currentPage/$pages (${rows.size} Einträge)"
        }
    }

    private suspend fun onReaction(event: ReactionAddEvent) {
        val emoji = event.emoji as? ReactionEmoji.Unicode ?: return
        if (event.user != user) return
        if (event.user.id != event.kord.selfId) {
            event.message.deleteReaction(event.userId, event.emoji)
        } else return // Don't react to the bots reactions

        val nextPage = when (Emojis[emoji.name]) {
            BULK_LEFT -> 1
            LEFT -> currentPage - 1
            RIGHT -> currentPage + 1
            BULK_RIGHT -> pages
            else -> -1
        }

        if (nextPage == -1) {
            close()
            return
        }

        rescheduleTimeout()

        if (nextPage !in 1..pages) return

        paginate(nextPage)
    }

    private fun rescheduleTimeout() {
        if (canceller.isActive) {
            canceller.cancel()
        }
        canceller = GlobalScope.launch {
            delay(timeoutMillis)
            close(false)
        }
    }

    private suspend fun close(cancelJob: Boolean = true) {
        if (cancelJob) canceller.cancel()
        message.shutDown()
        message.message.deleteAllReactions()
    }

    companion object {
        private val BULK_LEFT: DiscordEmoji = Emojis.rewind
        private val LEFT: DiscordEmoji = Emojis.arrowLeft
        private val STOP: DiscordEmoji = Emojis.stopButton
        private val RIGHT: DiscordEmoji = Emojis.arrowRight
        private val BULK_RIGHT: DiscordEmoji = Emojis.fastForward

        internal suspend fun addReactions(message: Message) =
            listOf(BULK_LEFT, LEFT, STOP, RIGHT, BULK_RIGHT).forEach {
                message.addReaction(it)
            }
    }
}
