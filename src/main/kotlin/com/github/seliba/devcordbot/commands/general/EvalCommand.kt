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
import com.github.seliba.devcordbot.util.jdoodle.JDoodle

/**
 * Eval command.
 */
class EvalCommand : AbstractCommand() {
    override val aliases: List<String> = listOf("eval", "exec", "execute", "run")
    override val displayName: String = "eval"
    override val description: String = "Führt den angegebenen Code aus."
    override val usage: String = "[language] \n <code>"
    override val permission: Permission = Permission.ANY
    override val category: CommandCategory = CommandCategory.GENERAL

    override fun execute(context: Context) {
        JDoodle.execute(context)
    }

}

