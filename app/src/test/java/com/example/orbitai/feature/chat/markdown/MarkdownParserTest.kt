package com.example.orbitai.feature.chat.markdown

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownParserTest {

    private fun blocks(s: String) = MarkdownParser.parseBlocks(s)

    @Test fun headingsParagraphsAndRules() {
        assertEquals(
            listOf(MdBlock.Heading(2, "Title"), MdBlock.Paragraph("Line one\nline two"), MdBlock.Rule, MdBlock.Paragraph("After")),
            blocks("## Title\nLine one\nline two\n\n---\nAfter"),
        )
    }

    @Test fun orderedListsKeepTheirNumbers() {
        assertEquals(
            listOf(MdBlock.ListItem(0, 1, null, "first"), MdBlock.ListItem(0, 2, null, "second"), MdBlock.ListItem(0, 10, null, "tenth")),
            blocks("1. first\n2. second\n10. tenth"),
        )
    }

    @Test fun nestedAndTaskLists() {
        assertEquals(
            listOf(
                MdBlock.ListItem(0, null, null, "top"),
                MdBlock.ListItem(1, null, null, "child"),
                MdBlock.ListItem(0, null, true, "done"),
                MdBlock.ListItem(0, null, false, "todo"),
            ),
            blocks("- top\n  - child\n- [x] done\n- [ ] todo"),
        )
    }

    @Test fun blockQuotesGroupLines() {
        assertEquals(listOf(MdBlock.Quote("one\ntwo")), blocks("> one\n> two"))
    }

    @Test fun tablesWithAlignmentAndRaggedRows() {
        val table = blocks("| Name | Qty | Price |\n|:---|:---:|---:|\n| Tea | 2 | 10 |\n| Coffee | 1 |") .single() as MdBlock.Table
        assertEquals(listOf("Name", "Qty", "Price"), table.header)
        assertEquals(listOf(Align.START, Align.CENTER, Align.END), table.alignments)
        assertEquals(listOf(listOf("Tea", "2", "10"), listOf("Coffee", "1", "")), table.rows)
    }

    @Test fun tableHeaderWithoutSeparatorStaysParagraphWhileStreaming() {
        assertEquals(listOf(MdBlock.Paragraph("| Name | Qty |")), blocks("| Name | Qty |"))
        assertTrue(blocks("| Name | Qty |\n|---|---|").single() is MdBlock.Table)
    }

    @Test fun unclosedFenceIsOpenCodeBlock() {
        val code = blocks("Run this:\n```kotlin\nval x = 1\n") .last() as MdBlock.Code
        assertEquals("kotlin", code.language)
        assertFalse(code.closed)
        assertTrue((blocks("```\nx\n```").single() as MdBlock.Code).closed)
    }

    @Test fun markdownInsideCodeIsNotParsed() {
        val code = blocks("```\n# not heading\n- not list\n| a | b |\n|---|---|\n```").single() as MdBlock.Code
        assertEquals("# not heading\n- not list\n| a | b |\n|---|---|", code.code)
    }

    @Test fun inlineStyles() {
        assertEquals(
            listOf(
                MdSpan("Use "), MdSpan("bold", bold = true), MdSpan(", "), MdSpan("italic", italic = true),
                MdSpan(", "), MdSpan("old", strike = true), MdSpan(" and "), MdSpan("code()", code = true),
            ),
            MarkdownParser.parseInline("Use **bold**, *italic*, ~~old~~ and `code()`"),
        )
    }

    @Test fun boldCanContainItalic() {
        assertEquals(
            listOf(MdSpan("see ", bold = true), MdSpan("this", bold = true, italic = true)),
            MarkdownParser.parseInline("**see *this***"),
        )
    }

    @Test fun linksAndBareUrls() {
        assertEquals(
            listOf(MdSpan("Docs", url = "https://example.org/a"), MdSpan(" or "), MdSpan("https://example.org/b", url = "https://example.org/b"), MdSpan(".")),
            MarkdownParser.parseInline("[Docs](https://example.org/a) or https://example.org/b."),
        )
    }

    @Test fun arithmeticAndSnakeCaseAreNotEmphasis() {
        assertEquals(listOf(MdSpan("2 * 3 * 4 and my_var_name")), MarkdownParser.parseInline("2 * 3 * 4 and my_var_name"))
    }

    @Test fun htmlForBubbleEscapesAndFormats() {
        val html = MarkdownHtml.toHtml("**Hi** <b>x</b>\n1. one")
        assertTrue(html.contains("<b>Hi</b>"))
        assertTrue(html.contains("&lt;b&gt;x&lt;/b&gt;"))
        assertTrue(html.contains("1. one"))
    }

    @Test fun highlighterFindsKeywordsStringsCommentsNumbers() {
        val code = "val x = \"hi\" // note\nreturn 42"
        val kinds = CodeHighlighter.tokens(code, "kotlin").map { code.substring(it.start, it.end) to it.kind }
        assertEquals(
            listOf("val" to TokenKind.KEYWORD, "\"hi\"" to TokenKind.STRING, "// note" to TokenKind.COMMENT,
                "return" to TokenKind.KEYWORD, "42" to TokenKind.NUMBER),
            kinds,
        )
    }
}
