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

package com.github.devcordde.devcordbot.commands.owners

import com.github.devcordde.devcordbot.command.AbstractSingleCommand
import com.github.devcordde.devcordbot.command.CommandCategory
import com.github.devcordde.devcordbot.command.CommandPlace
import com.github.devcordde.devcordbot.command.context.Context
import com.github.devcordde.devcordbot.command.permission.Permission
import com.github.devcordde.devcordbot.constants.Embeds
import com.github.devcordde.devcordbot.constants.Emotes
import com.github.devcordde.devcordbot.dsl.editMessage
import com.github.devcordde.devcordbot.dsl.editOriginal
import com.github.devcordde.devcordbot.util.HastebinUtil
import com.github.devcordde.devcordbot.util.await
import com.github.devcordde.devcordbot.util.limit
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.requests.restaction.CommandUpdateAction
import javax.script.ScriptEngineManager
import javax.script.ScriptException

/**
 * Eval command for bot owners.
 */
class EvalCommand : AbstractSingleCommand() {
    override val name: String = "ev"
    override val description: String = "Führt Kotlin Code über den Bot aus"
    override val permission: Permission = Permission.BOT_OWNER
    override val category: CommandCategory = CommandCategory.BOT_OWNER
    override val commandPlace: CommandPlace = CommandPlace.ALL
    override val options: List<CommandUpdateAction.OptionData> = buildOptions {
        string("code", "Der auszuführende Code")
    }

    override suspend fun execute(context: Context) {
        val message = context.respond(
            Embeds.loading(
                "Code wird kompiliert und ausgeführt",
                "Bitte warte einen Augenblick während dein Script kompiliert und ausgeführt wird"
            )
        ).await()

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
                import okhttp3.*
                import net.dv8tion.jda.api.*
                import net.dv8tion.jda.api.entities.*
            """.trimIndent()
        )
        scriptEngine.put("context", context)
        val result = try {
            val evaluation = scriptEngine.eval(script)?.toString() ?: "null"
            if (evaluation.length > MessageEmbed.TEXT_MAX_LENGTH - "Ergebnis: ``````".length) {
                val result = Embeds.info(
                    "Erfolgreich ausgeführt!",
                    "Ergebnis: ${Emotes.LOADING}"
                )
                val hasteUrl = HastebinUtil.postErrorToHastebin(evaluation, context.bot.httpClient)
                message.editMessage(result.apply {
                    @Suppress("ReplaceNotNullAssertionWithElvisReturn") // Description is set above
                    description = description!!.replace(Emotes.LOADING.toRegex(), hasteUrl)
                }).queue()
                result
            } else {
                Embeds.info("Erfolgreich ausgeführt!", "Ergebnis: ```$evaluation```")
            }
        } catch (e: ScriptException) {
            val result = Embeds.error(
                "Fehler!",
                "Es ist folgender Fehler aufgetreten: ```${e.message?.limit(1024)}``` Detailierter Fehler: ${Emotes.LOADING}"
            )
            val hasteUrl = HastebinUtil.postErrorToHastebin(e.stackTraceToString(), context.bot.httpClient)
            message.editMessage(result.apply {
                @Suppress("ReplaceNotNullAssertionWithElvisReturn") // Description is set above
                description = description!!.replace(Emotes.LOADING.toRegex(), hasteUrl)
            }).queue()
            result
        }
        context.ack.editOriginal(result)
    }
}
