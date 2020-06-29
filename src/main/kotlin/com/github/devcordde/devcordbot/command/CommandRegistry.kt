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

package com.github.devcordde.devcordbot.command

/**
 * A registry of [AbstractCommand]s.
 * @property commandAssociations associations between the commands triggers, and the commands
 * @property registeredCommands all registered commands
 * @param T the type of command
 */
interface CommandRegistry<T : AbstractCommand> {
    val commandAssociations: MutableMap<String, T>
    val registeredCommands: List<T>
        get() = commandAssociations.values.distinct()

    private fun registerCommand(command: T) =
        command.aliases.associateWithTo(commandAssociations) { command }

    /**
     * Registers the [commands].
     */
    fun registerCommands(vararg commands: T): Unit = commands.forEach { registerCommand(it) }

    /**
     * Unregisters the [command].
     * @return whether a command got removed or not
     */
    fun unregisterCommand(command: T): Boolean {
        var removed = false
        commandAssociations.forEach { (alias, foundCommand) ->
            run {
                if (foundCommand == command) {
                    commandAssociations.remove(alias)
                    removed = true
                }
            }
        }
        return removed
    }
}
