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

package com.github.seliba.devcordbot.commands.`fun`

import com.github.seliba.devcordbot.command.AbstractCommand
import com.github.seliba.devcordbot.command.CommandCategory
import com.github.seliba.devcordbot.command.context.Context
import com.github.seliba.devcordbot.command.permission.Permission
import com.github.seliba.devcordbot.core.autohelp.ImageRecognizer
import kotlinx.coroutines.future.await

/**
 * Command that reads text on images, or at least tries to do it.
 */
class ImageCommand : AbstractCommand() {
    override val aliases: List<String> = listOf("image", "foodporn", "carporn")
    override val displayName: String = "image"
    override val description: String = "reads text on images, or at least tries to do it"
    override val usage: String = "<image attachment, to message obviusly>"
    override val permission: Permission = Permission.SCHLAUBI
    override val category: CommandCategory = CommandCategory.FUN

    override suspend fun execute(context: Context) {
        val attachment =
            context.message.attachments.firstOrNull() ?: return context.respond("Bitte Bild anhängen").queue()
        if (!attachment.isImage) {
            return context.respond("Das ist kein Bild!").queue()
        }
        val image = attachment.retrieveInputStream().await()
        val text = ImageRecognizer.readImageText(image)
        context.respond(text).queue()
    }
}