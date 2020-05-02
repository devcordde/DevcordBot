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

import com.github.seliba.devcordbot.event.AnnotatedEventManager
import com.github.seliba.devcordbot.event.EventSubscriber
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import kotlinx.coroutines.Dispatchers
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.ExceptionEvent
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.hooks.IEventManager
import net.dv8tion.jda.api.hooks.SubscribeEvent
import org.junit.jupiter.api.Test

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

class EventTest {
    @Test
    fun test() {
        val eventManager: IEventManager = AnnotatedEventManager(Dispatchers.Unconfined)
        val validator = mock<Validator>()
        val jda = mock<JDA>()
        val event = ExceptionEvent(jda, RuntimeException("ERROR"), true)
        eventManager.register(Listener(validator))
        eventManager.handle(event)
        verify(validator).onEvent(event)
    }

}

interface Validator {
    @SubscribeEvent
    fun onEvent(event: GenericEvent)
}

class Listener(private val validator: Validator) {
    @EventSubscriber
    fun onEvent(event: ExceptionEvent) {
        validator.onEvent(event)
    }
}
