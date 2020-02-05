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

package com.github.seliba.devcordbot

import com.github.seliba.devcordbot.core.GameAnimator
import com.github.seliba.devcordbot.event.AnnotatedEventManger
import com.github.seliba.devcordbot.event.EventSubscriber
import kotlinx.coroutines.ObsoleteCoroutinesApi
import mu.KotlinLogging
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.events.ReadyEvent

private val logger = KotlinLogging.logger { }

/**
 * General class to manage the Discord bot.
 */
class DevcordBot(token: String, games: List<GameAnimator.AnimatedGame>) {

    /**
     * JDA instance used to run the Discord bot.
     */
    private val jda: JDA = JDABuilder(token).apply {
        setEventManager(AnnotatedEventManger())
        setActivity(Activity.playing("Starting ..."))
        setStatus(OnlineStatus.DO_NOT_DISTURB)
        addEventListeners(this@DevcordBot)
    }.build()

    private val gameAnimator =
        GameAnimator(jda, games)

    /**
     * Fired when the Discord bot has started successfully.
     */
    @ObsoleteCoroutinesApi
    @Suppress("unused")
    @EventSubscriber
    fun whenReady(event: ReadyEvent) {
        logger.info { "Received Ready event initializing bot internals ..." }
        event.jda.presence.setStatus(OnlineStatus.ONLINE)
        gameAnimator.start()
    }

}
