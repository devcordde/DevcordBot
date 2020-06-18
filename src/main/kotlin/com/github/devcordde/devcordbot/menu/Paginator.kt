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
import com.github.devcordde.devcordbot.dsl.editMessage
import com.github.devcordde.devcordbot.dsl.embed
import com.github.devcordde.devcordbot.event.EventSubscriber
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent
import java.awt.Color
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.math.ceil
import kotlin.math.min

/**
 * Reaction and embed based paginator for string lists.
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
class Paginator(
    private val items: List<CharSequence>,
    private val user: User,
    context: Context,
    private val title: String,
    private val timeoutMillis: Long = TimeUnit.SECONDS.toMillis(15),
    firstPage: Int = 1,
    private val itemsPerPage: Int = 8,
    loadingTitle: String = "Bitte warten!",
    loadingDescription: String = "Bitte warte, während die Liste geladen wird.",
    private val color: Color = Colors.BLUE
) {

    private val pages: Int
    private var currentPage = firstPage
    private val message: Message
    private lateinit var canceller: Job

    init {
        require(itemsPerPage > 0) { "Items per page must be > 0" }
        require(items.isNotEmpty()) { "Items must not be empty" }
        pages = ceil(items.size.toDouble() / itemsPerPage).toInt()
        require(firstPage <= pages) { "First page must exist" }
        if (pages > 1) {
            message = context.respond(Embeds.loading(loadingTitle, loadingDescription)).complete()
            context.jda.addEventListener(this)
            CompletableFuture.allOf(*listOf(BULK_LEFT, LEFT, STOP, RIGHT, BULK_RIGHT).map {
                message.addReaction(it).submit()
            }.toTypedArray())
                .thenAccept {
                    paginate(currentPage)
                }
                .exceptionally {
                    context.commandClient.errorHandler.handleException(it, context, Thread.currentThread())
                    close()
                    null // You cannot return Void
                }
            rescheduleTimeout()
        } else {
            message = context.respond(renderEmbed(items)).complete()
        }
    }

    private fun paginate(destinationPage: Int) {
        currentPage = destinationPage
        val start: Int = (destinationPage - 1) * itemsPerPage
        val end = min(items.size, destinationPage * itemsPerPage)
        val rows = items.subList(start, end)
        return message.editMessage(renderEmbed(rows)).queue()
    }

    private fun renderEmbed(rows: List<CharSequence>) = embed {
        color = this@Paginator.color
        title(title)
        val rowBuilder = StringBuilder()
        rows.indices.forEach {
            rowBuilder.append('`').append(it + (itemsPerPage * (currentPage - 1)) + 1).append("`. ").appendln(rows[it])
        }
        description = rowBuilder
        footer("Seite $currentPage/$pages (${rows.size} Einträge)")
    }

    /**
     * Listens for new reactions.
     */
    @EventSubscriber
    fun onReaction(event: GuildMessageReactionAddEvent) {
        if (event.reactionEmote.isEmote || event.user != user || event.messageIdLong != message.idLong) return
        if (event.user != event.jda.selfUser) {
            event.reaction.removeReaction(event.user).queue()
        } else return // Don't react to the bots reactions

        val nextPage = when (event.reactionEmote.emoji) {
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
        if (::canceller.isInitialized) {
            canceller.cancel()
        }
        canceller = GlobalScope.launch {
            delay(timeoutMillis)
            close(false)
        }
    }

    private fun close(cancelJob: Boolean = true) {
        if (cancelJob) canceller.cancel()
        message.clearReactions().queue()
        message.jda.removeEventListener(this)
    }

    companion object {
        private const val BULK_LEFT: String = """⏪"""
        private const val LEFT: String = """◀"""
        private const val STOP: String = """⏹"""
        private const val RIGHT: String = """▶"""
        private const val BULK_RIGHT: String = """⏩"""
    }
}
