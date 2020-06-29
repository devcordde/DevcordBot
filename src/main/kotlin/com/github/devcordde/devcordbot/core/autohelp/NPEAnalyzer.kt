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

import com.github.devcordde.devcordbot.constants.Embeds
import com.github.devcordde.devcordbot.dsl.EmbedConvention
import com.github.devcordde.devcordbot.dsl.sendMessage
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent

/**
 * Utility to analyze Java14+ Helpful NPEs.
 */
object NPEAnalyzer {

    /**
     * Handles an NPE.
     */
    fun handleHelpfulNpe(helpfulNpe: MatchResult, event: GuildMessageReceivedEvent) {
        val embed = Embeds.info("NullPointerExceptions verstehen | Was ist eine NPE?") {
            footer("AutoHelp V1 BETA - Bitte Bugs auf GitHub.com/devcordde/DevcordBot melden.")
        }
        val values = helpfulNpe.groupValues
        val operator = values[1]
        val type = values[2]
        val accessedTokenName = values[3]
        val nullName = values[4]

        val error = when (operator) {
            "assign" -> "Das Field `$accessedTokenName` kann nicht zugewiesen werden"
            "read" -> if (type == "the array length") "Die Länge des Arrays kann nicht gelesen werden" else "Das Field `$accessedTokenName` kann nicht gelesen werden"
            "load from" -> formatArrayMessage("Es kann kein Element des %sArrays gelesen werden", type)
            "store to" -> formatArrayMessage("Es kann kein Element zum %sArray hinzugefügt werden.", type)
            "throw" -> "Die Exception kann nicht geworfen werden"
            "invoke" -> "Die Methode `$accessedTokenName` kann nicht aufgerufen werden"
            "enter" -> "Der $type kann nicht betreten werden"
            "exit" -> "Der $type kann nicht verlassen werden"
            else -> "Unbekannt, ein freundliches Communitymitglied wird dir gleich helfen"
        }

        embed.addField("Symptom", error)

        analyzeCause(nullName, embed)

        embed.addField(
            "Weitere Informationen",
            "Weitere Informationen zum Theme NullPointerExceptions findest du mit dem Befehlt `st npe`"
        )

        event.channel.sendMessage(embed).queue()
    }

    private fun analyzeCause(nullName: String, embed: EmbedConvention) {
        val analyzedName = JAVA_TOKEN_CHAIN_PATTERN.findAll(nullName).lastOrNull() ?: return
        val name = analyzedName.value
        if (name.endsWith("()")) {
            embed.addField("Ursache", "Die Methode `$name` hat `null` zurückgegeben")
            embed.addField(
                "Lösung",
                "Meistens bedeuted dies, dass diese Methode manchmal `null` returned und manchmal nicht. \nVersuche über die Dokumentation herauszufinden wieso sie das tut und füge einen Check hinzu um den Fall, dass sie `null` zurück gibt zu beachten"
            )
        } else {
            embed.addField("Ursache", "Das Field `$name` ist null")
            embed.addField(
                "Lösung",
                "Dies bedeuted, dass dem Field `$name` nie ein Wert zugewiesen wurde. \nUm den Fehler zu beheben weise ihm bitte einen Wert zu."
            )
        }

    }

    private fun formatArrayMessage(input: String, type: String) =
        input.format(if (type == "array") "" else type.substring(0, type.indexOf("array") - 1))

    // https://regex101.com/r/NlsHY1/1
    private val JAVA_TOKEN_CHAIN_PATTERN = "[a-zA-Z_\$][a-zA-Z_\$0-9]*(?:\\(\\))?".toRegex()
}