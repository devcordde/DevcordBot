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

package com.github.seliba.devcordbot.commands.general

import com.github.seliba.devcordbot.command.AbstractCommand
import com.github.seliba.devcordbot.command.CommandCategory
import com.github.seliba.devcordbot.command.context.Context
import com.github.seliba.devcordbot.command.perrmission.Permission
import com.github.seliba.devcordbot.constants.Embeds
import kotlin.random.Random

/**
 * Mock command.
 */
class MockCommand : AbstractCommand() {
    override val aliases: List<String> = listOf("mock", "m")
    override val displayName: String = "mock"
    override val description: String = "Mockt den eingegebenen Text."
    override val usage: String = "[command] <text>"
    override val permission: Permission = Permission.ANY
    override val category: CommandCategory = CommandCategory.GENERAL

    override fun execute(context: Context) {
        val arguments = context.args

        if (arguments.isEmpty()) {
            return context.respond(Embeds.command(this)).queue()
        }

        context.respond(mock(arguments.join())).queue()
    }

    /**
     * Mocks a text.
     */
    private fun mock(text: String): String = text.asIterable().joinToString("") { tweak(it).toString() }

    /**
     * Tweaks a given char.
     */
    private fun tweak(c: Char): Char = if (Random.nextBoolean()) c.toLowerCase() else c.toUpperCase()

}
