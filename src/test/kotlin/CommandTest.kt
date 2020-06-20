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

import com.github.devcordde.devcordbot.command.AbstractCommand
import com.github.devcordde.devcordbot.command.AbstractSubCommand
import com.github.devcordde.devcordbot.command.CommandPlace
import com.github.devcordde.devcordbot.command.PermissionHandler
import com.github.devcordde.devcordbot.command.impl.CommandClientImpl
import com.github.devcordde.devcordbot.command.permission.Permission
import com.github.devcordde.devcordbot.command.permission.PermissionState
import com.github.devcordde.devcordbot.constants.Constants
import com.github.devcordde.devcordbot.core.DevCordBot
import com.github.devcordde.devcordbot.database.DatabaseDevCordUser
import com.github.devcordde.devcordbot.database.DevCordUser
import com.github.devcordde.devcordbot.event.DevCordGuildMessageReceivedEvent
import com.nhaarman.mockitokotlin2.KStubbing
import com.nhaarman.mockitokotlin2.argThat
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.requests.RestAction
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import java.util.function.BooleanSupplier
import java.util.function.Consumer

class CommandTest {

    @Test
    fun `check prefixed normal command`() {
        val message = mockMessage {
            on { contentRaw }.thenReturn("!test ${arguments.joinToString(" ")}")
        }

        val command = mockCommand {
            on { aliases }.thenReturn(listOf("test"))
            on { commandPlace }.thenReturn(CommandPlace.ALL)
        }

        ensureCommandCall(message, command, arguments)
    }

    @Test
    fun `check mentioned normal command`() {
        val mention = "<@${selfMember.id}>"
        val message = mockMessage {
            on { contentRaw }.thenReturn("$mention test ${arguments.joinToString(" ")}")
        }

        val command = mockCommand {
            on { aliases }.thenReturn(listOf("test"))
            on { commandPlace }.thenReturn(CommandPlace.ALL)
        }

        ensureCommandCall(message, command, arguments)
    }

    @Test
    fun `check prefixed sub command`() {
        val message = mockMessage {
            on { contentRaw }.thenReturn("!test ${arguments.joinToString(" ")}")
        }

        val subCommand = mock<AbstractSubCommand> {
            on { permission }.thenReturn(Permission.ANY)
            on { commandPlace }.thenReturn(CommandPlace.ALL)
        }

        val command = mockCommand {
            on { aliases }.thenReturn(listOf("test"))
            on { commandAssociations }.thenReturn(mutableMapOf("sub" to subCommand))
            on { commandPlace }.thenReturn(CommandPlace.ALL)
        }

        ensureCommandCall(message, command, arguments.subList(1, arguments.size), subCommand)
    }

    private fun ensureCommandCall(
        message: Message,
        command: AbstractCommand,
        arguments: List<String>,
        subCommand: AbstractSubCommand? = null
    ) {
        val devCordUser = mock<DevCordUser> {
            on { blacklisted }.thenReturn(false)
        }

        val event = DevCordGuildMessageReceivedEvent(jda, 200, message, devCordUser)
        client.registerCommands(command)
        client.onMessage(event)
        val actualCommand = subCommand ?: command

        runBlocking {
            verify(actualCommand).execute(argThat {
                arguments == args.toList() &&
                        client === this.commandClient &&
                        message === this.message &&
                        bot === this.bot &&
                        author === this.author
            })
        }
    }

    private fun mockMessage(
        stubbing: KStubbing<Message>.() -> Unit
    ): Message {
        return mock {
            on { this.author }.thenReturn(author)
            on { this.channel }.thenReturn(channel)
            on { textChannel }.thenReturn(channel)
            on { contentRaw }.thenReturn("!test ${arguments.joinToString(" ")}")
            on { isWebhookMessage }.thenReturn(false)
            on { member }.thenReturn(selfMember)
            on { this.guild }.thenReturn(guild)
            on { channelType }.thenReturn(ChannelType.TEXT)
            stubbing(this)
        }
    }

    private fun mockCommand(
        stubbing: KStubbing<AbstractCommand>.() -> Unit
    ) = mock<AbstractCommand> {
        on { permission }.thenReturn(Permission.ANY)
        on { commandPlace }.thenReturn(CommandPlace.ALL)
        stubbing(this)
    }

    companion object {
        private lateinit var bot: DevCordBot
        private val arguments = listOf("sub", "2", "3")
        private lateinit var jda: JDA
        private lateinit var channel: TextChannel
        private lateinit var selfMember: Member
        private lateinit var guild: Guild
        private lateinit var client: CommandClientImpl
        private lateinit var author: User

        @BeforeAll
        @JvmStatic
        fun `setup mock objects`() {
            val botGuild = mock<Guild> {
                on { id }.thenReturn("")
            }
            bot = mock {
                on { isInitialized }.thenReturn(true)
                on { guild }.thenReturn(
                    botGuild
                )
            }
            client = CommandClientImpl(bot, Constants.prefix, TestPermissionHandler(), Dispatchers.Unconfined)
            jda = mock()
            channel = mock {
                on { sendTyping() }.thenReturn(EmptyRestAction<Void>())
                on { type }.thenReturn(ChannelType.TEXT)
            }
            selfMember = mock {
                on { id }.thenReturn("123456789")
            }
            guild = mock {
                on { this.selfMember }.thenReturn(selfMember)
                on { id }.thenReturn("")
            }
            author = mock {
                on { isBot }.thenReturn(false)
                on { isFake }.thenReturn(false)
            }
        }
    }
}

private class EmptyRestAction<T> : RestAction<T> {
    override fun submit(shouldQueue: Boolean): CompletableFuture<T> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun complete(shouldQueue: Boolean): T {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getJDA(): JDA {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun queue(success: Consumer<in T?>?, failure: Consumer<in Throwable>?) = success?.accept(null) ?: Unit

    override fun setCheck(checks: BooleanSupplier?): RestAction<T> = this

}

private class TestPermissionHandler : PermissionHandler {
    override fun isCovered(
        permission: Permission,
        executor: Member?,
        devCordUser: DevCordUser,
        acknowledgeBlacklist: Boolean
    ): PermissionState {
        return PermissionState.ACCEPTED
    }
}
