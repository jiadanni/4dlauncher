package net.gsantner.opoc.format.markdown

import org.junit.Assert.assertEquals
import org.junit.Test

class SimpleMarkdownParserTest {

    @Test
    fun testParseSimpleMarkdown() {
        val parser = SimpleMarkdownParser()
        parser.parse("# Heading 1\n\nSome text.", "", SimpleMarkdownParser.FILTER_WEB)

        val html = parser.html
        assertEquals("<h1>Heading 1</h1>\n<br/>\nSome text.", html)
    }
}
