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

package com.github.devcordde.devcordbot.commands.owners

import com.github.devcordde.devcordbot.command.CommandCategory
import com.github.devcordde.devcordbot.command.CommandPlace
import com.github.devcordde.devcordbot.command.context.Context
import com.github.devcordde.devcordbot.command.permission.Permission
import com.github.devcordde.devcordbot.command.root.AbstractSingleCommand
import com.github.devcordde.devcordbot.constants.Embeds
import com.github.devcordde.devcordbot.constants.Emotes
import com.github.devcordde.devcordbot.constants.TEXT_MAX_LENGTH
import com.github.devcordde.devcordbot.util.HastebinUtil
import com.github.devcordde.devcordbot.util.limit
import dev.kord.core.behavior.interaction.InteractionResponseBehavior
import dev.kord.core.event.interaction.InteractionCreateEvent
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import dev.kord.rest.builder.interaction.string
import org.intellij.lang.annotations.Language
import javax.script.ScriptEngineManager
import javax.script.ScriptException

/**
 * Eval command for bot owners.
 */
class EvalCommand : AbstractSingleCommand<InteractionResponseBehavior>() {
    override val name: String = "ev"
    override val description: String = "Führt Kotlin-Code über den Bot aus."
    override val permission: Permission = Permission.BOT_OWNER
    override val category: CommandCategory = CommandCategory.BOT_OWNER
    override val commandPlace: CommandPlace = CommandPlace.ALL

    override fun ChatInputCreateBuilder.applyOptions() {
        string("code", "Der auszuführende Code") {
            required = true
        }
    }

    override suspend fun InteractionCreateEvent.acknowledge(): InteractionResponseBehavior =
        interaction.acknowledgePublic()

    override suspend fun execute(context: Context<InteractionResponseBehavior>) {
        val message = context.respond(
            Embeds.loading(
                "Code wird kompiliert und ausgeführt",
                "Bitte warte einen Augenblick, während dein Script kompiliert und ausgeführt wird..."
            )
        )

        val scriptEngine = ScriptEngineManager().getEngineByName("kotlin")
        val script = context.args.string("code")
        //language=kotlin
        scriptEngine.eval(
            """
                    import com.github.devcordde.devcordbot.*
                    import com.github.devcordde.devcordbot.database.*
                    import com.github.devcordde.devcordbot.command.*
                    import com.github.devcordde.devcordbot.command.permission.Permission as BotPermission
                    import com.github.devcordde.devcordbot.command.context.*
                    import org.jetbrains.exposed.sql.transactions.*
                    import dev.kord.common.entity.*
                    import kotlinx.coroutines.runBlocking
            """.trimIndent()
        )
        scriptEngine.put("context", context)
        val result = try {
            @Language("kotlin")
            val suspendingScript = """
                runBlocking {
                    $script
                }
            """.trimIndent()
            val evaluation = scriptEngine.eval(suspendingScript)?.toString() ?: "null"
            if (evaluation.length > TEXT_MAX_LENGTH - "Ausgabe: ``````".length) {
                val result = Embeds.info(
                    "Erfolgreich ausgeführt!",
                    "Ausgabe: ${Emotes.LOADING}"
                )
                val hasteUrl = HastebinUtil.postToHastebin(evaluation, context.bot.httpClient)
                message.edit(
                    result.apply {
                        @Suppress("ReplaceNotNullAssertionWithElvisReturn") // Description is set above
                        description = description!!.replace(Emotes.LOADING.toRegex(), hasteUrl)
                    }
                )
                result
            } else {
                Embeds.info("Erfolgreich ausgeführt!", "Ausgabe: ```$evaluation```")
            }
        } catch (e: ScriptException) {
            val result = Embeds.error(
                "Fehler!",
                "Es ist folgender Fehler aufgetreten: ```${e.message?.limit(1024)}``` Detailierter Fehler: ${Emotes.LOADING}"
            )
            val hasteUrl = HastebinUtil.postToHastebin(e.stackTraceToString(), context.bot.httpClient)
            message.edit(
                result.apply {
                    @Suppress("ReplaceNotNullAssertionWithElvisReturn") // Description is set above
                    description = description!!.replace(Emotes.LOADING.toRegex(), hasteUrl)
                }
            )
            result
        }
        context.respond(result)
    }
}
