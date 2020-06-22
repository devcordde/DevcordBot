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

import com.codewaves.codehighlight.core.Highlighter
import com.codewaves.codehighlight.core.StyleRenderer
import com.codewaves.codehighlight.core.StyleRendererFactory

/**
 * LanguageGusser
 */
class LanguageGusser(knownLanguages: List<String>) {
    private val languageGuesser = Highlighter(UselessRendererFactoryThing())
    private val knownLanguages = knownLanguages.toTypedArray()

    /**
     * Guesses the programming language of [potentialCode].
     */
    fun guessLanguage(potentialCode: String): Highlighter.HighlightResult? =
        languageGuesser.highlightAuto(potentialCode, knownLanguages)

}

// We don't want to highlight anything
@Suppress("FunctionName") // It should act like a class
private fun UselessRendererFactoryThing() = StyleRendererFactory { NOOPRenderer() }

private class NOOPRenderer : StyleRenderer {
    override fun onFinish() = Unit

    override fun onPushSubLanguage(name: String?, code: CharSequence?) = Unit

    override fun onPopStyle() = Unit

    override fun getResult(): CharSequence = ""

    override fun onPushStyle(style: String?) = Unit

    override fun onPushCodeBlock(codeLexeme: CharSequence?) = Unit

    override fun onAbort(code: CharSequence?) = Unit

    override fun onStart() = Unit
}
