import com.github.seliba.devcordbot.command.AbstractCommand
import com.github.seliba.devcordbot.command.impl.CommandClientImpl
import com.github.seliba.devcordbot.command.perrmission.Permissions
import com.github.seliba.devcordbot.core.DevCordBot
import com.github.seliba.devcordbot.util.Constants
import com.nhaarman.mockitokotlin2.argThat
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import kotlinx.coroutines.Dispatchers
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.requests.RestAction
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import java.util.function.BooleanSupplier
import java.util.function.Consumer

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

class CommandTest {

    @Test
    fun test() {
        val arguments = listOf("1", "2", "3")
        val jda = mock<JDA>()
        val channel = mock<TextChannel> {
            on { sendTyping() }.thenReturn(EmptyRestAction<Void>())
        }
        val author = mock<User> {
            on { isBot }.thenReturn(false)
            on { isFake }.thenReturn(false)
        }
        val selfMember = mock<Member> {
            on { asMention }.thenReturn("@mention")
        }
        val guild = mock<Guild> {
            on { this.selfMember }.thenReturn(selfMember)
        }
        val message = mock<Message> {
            on { textChannel }.thenReturn(channel)
            on { contentRaw }.thenReturn("!test ${arguments.joinToString(" ")}")
            on { isWebhookMessage }.thenReturn(false)
            on { member }.thenReturn(selfMember)
            on { this.author }.thenReturn(author)
            on { this.guild }.thenReturn(guild)
        }
        val command = mock<AbstractCommand> {
            on { permissions }.thenReturn(Permissions.ANY)
            on { aliases }.thenReturn(listOf("test"))
        }
        val bot = mock<DevCordBot> {
            on { isInitialized }.thenReturn(true)
        }
        val event = GuildMessageReceivedEvent(jda, 200, message)
        val client = CommandClientImpl(bot, Constants.prefix, Dispatchers.Unconfined)
        client.registerCommands(command)
        client.onMessage(event)
        Thread.sleep(5000L)
        verify(command).execute(argThat {
            arguments == args.toList() &&
                    client === this.commandClient &&
                    message === this.message &&
                    bot === this.bot &&
                    author === this.author
        })
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

    override fun queue(success: Consumer<in T>?, failure: Consumer<in Throwable>?) = Unit

    override fun setCheck(checks: BooleanSupplier?): RestAction<T> = this

}