// Credit: https://raw.githubusercontent.com/bankobotv14/BankoBot/5c5ab29a58d78b7276b7a5e94d6bfabb96355048/src/main/kotlin/de/nycode/bankobot/docdex/CodeBlockFormatter.kt
/*
 * MIT License
 *
 * Copyright (c) 2021 BankoBot Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package de.nycode.bankobot.docdex

import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter
import com.vladsch.flexmark.html2md.converter.HtmlNodeRendererHandler
import com.vladsch.flexmark.html2md.converter.PhasedHtmlNodeRenderer
import com.vladsch.flexmark.html2md.converter.internal.HtmlConverterCoreNodeRenderer
import org.jsoup.nodes.Element

/**
 * [FlexmarkHtmlConverter] used to render HTML as markdown.
 */
val htmlRenderer: FlexmarkHtmlConverter = buildRenderer()

private fun buildRenderer(): FlexmarkHtmlConverter =
    FlexmarkHtmlConverter.Builder().apply {
        htmlNodeRendererFactory { options ->
            DelegatedHtmlRenderer(HtmlConverterCoreNodeRenderer(options))
        }
    }.build()

private class DelegatedHtmlRenderer(private val renderer: PhasedHtmlNodeRenderer) :
    PhasedHtmlNodeRenderer by renderer {
    private val overwrittenNodes =
        listOf(FlexmarkHtmlConverter.CODE_NODE, FlexmarkHtmlConverter.BLOCKQUOTE_NODE)

    override fun getHtmlNodeRendererHandlers(): MutableSet<HtmlNodeRendererHandler<*>> {
        val superList = renderer.htmlNodeRendererHandlers
        superList.removeIf {
            it.tagName in overwrittenNodes
        }

        return (
            superList + HtmlNodeRendererHandler(
                FlexmarkHtmlConverter.PRE_NODE,
                Element::class.java,
                FlexmarkUtils::processPre
            )
            ).toMutableSet()
    }
}
