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

package com.github.devcordde.devcordbot.command.slashcommands.permissions

import com.fasterxml.jackson.annotation.JsonProperty
import net.dv8tion.jda.api.requests.restaction.CommandUpdateAction

/**
 * Extension of [CommandUpdateAction.CommandData] adding permission support.
 *
 * @property defaultPermission whether users can use this command by default or not.
 */
class PermissiveCommandData(name: String, description: String) : CommandUpdateAction.CommandData(name, description) {
    @get:JsonProperty("default_permission")
    var defaultPermission: Boolean = true
}

/**
 * Extension of [CommandUpdateAction.SubcommandData] adding permission support.
 *
 * @property defaultPermission whether users can use this command by default or not.
 */
class PermissiveSubCommandData(name: String, description: String) :
    CommandUpdateAction.SubcommandData(name, description) {
    @get:JsonProperty("default_permission")
    var defaultPermission: Boolean = true
}

/**
 * Extension of [CommandUpdateAction.SubcommandGroupData] adding permission support.
 *
 * @property defaultPermission whether users can use this command by default or not.
 */
class PermissiveSubCommandGroupData(name: String, description: String) :
    CommandUpdateAction.SubcommandGroupData(name, description) {
    @get:JsonProperty("default_permission")
    var defaultPermission: Boolean = true
}
