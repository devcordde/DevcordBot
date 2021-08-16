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

package com.github.devcordde.devcordbot.util

import ru.homyakin.iuliia.Schemas
import ru.homyakin.iuliia.Translator
import net.gcardone.junidecode.Junidecode
import java.text.Normalizer

private val translator = Translator(Schemas.ICAO_DOC_9303)

/**
 * Sanitize names which are unmentionable
 */
fun String.sanitize(): String = latinize().removeAccents().unnorm().unidecode()

private fun String.latinize() = translator.translate(this)

private fun String.removeAccents() = normalize(Normalizer.Form.NFD)
private fun String.unnorm() = normalize(Normalizer.Form.NFKC)
private fun String.unidecode() = Junidecode.unidecode(this)

private fun String.normalize(form: Normalizer.Form): String {
    return Normalizer
        .normalize(this, form)
        .replace("\\p{M}".toRegex(), "")
}

