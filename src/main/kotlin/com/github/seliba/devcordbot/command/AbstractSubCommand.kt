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

package com.github.seliba.devcordbot.command

import com.github.seliba.devcordbot.command.perrmission.Permission

/**
 * Skeleton of a sub command.
 * @property parent the parent of the command
 * @see AbstractCommand
 */
@Suppress("MemberVisibilityCanBePrivate")
abstract class AbstractSubCommand(val parent: AbstractCommand) : AbstractCommand() {
    override val category: CommandCategory
        get() = parent.category
    override val permission: Permission
        get() = parent.permission
}
