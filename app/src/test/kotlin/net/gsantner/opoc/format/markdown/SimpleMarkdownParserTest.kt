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

    @Test
    fun testRemoveMultiNewlines() {
        val parser = SimpleMarkdownParser()

        // Test newline removal
        parser.setHtml("Line 1\nLine 2")
        parser.removeMultiNewlines()
        assertEquals("Line 1Line 2", parser.html)

        // Test <br/> collapsing (3 or more)
        parser.setHtml("Line 1<br/><br/><br/>Line 2")
        parser.removeMultiNewlines()
        assertEquals("Line 1<br/><br/>Line 2", parser.html)

        parser.setHtml("Line 1<br/><br/><br/><br/>Line 2")
        parser.removeMultiNewlines()
        assertEquals("Line 1<br/><br/>Line 2", parser.html)

        // Test <br/> not collapsed if less than 3
        parser.setHtml("Line 1<br/><br/>Line 2")
        parser.removeMultiNewlines()
        assertEquals("Line 1<br/><br/>Line 2", parser.html)
    }

    @Test
    fun testReplaceBulletCharacter() {
        val parser = SimpleMarkdownParser()
        parser.setHtml("&#8226; Item 1\n&#8226; Item 2")
        parser.replaceBulletCharacter("*")
        assertEquals("* Item 1\n* Item 2", parser.html)
    }

}
