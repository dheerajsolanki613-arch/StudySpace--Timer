package com.studyspace.timer.data.transfer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class CsvCodecTest {

    // ---------- csvEscape ----------

    @Test
    fun `plain values are left alone`() {
        assertEquals("Mathematics", csvEscape("Mathematics"))
        assertEquals("", csvEscape(""))
    }

    @Test
    fun `values with commas, quotes or line breaks are quoted and quotes doubled`() {
        assertEquals("\"a,b\"", csvEscape("a,b"))
        assertEquals("\"say \"\"hi\"\"\"", csvEscape("say \"hi\""))
        assertEquals("\"line1\nline2\"", csvEscape("line1\nline2"))
        assertEquals("\"a\rb\"", csvEscape("a\rb"))
    }

    // ---------- formula-injection guard ----------

    @Test
    fun `text starting with a formula trigger gets a guarding apostrophe`() {
        assertEquals("'=SUM(A1)", csvSafeText("=SUM(A1)"))
        assertEquals("'+1", csvSafeText("+1"))
        assertEquals("'-1", csvSafeText("-1"))
        assertEquals("'@cmd", csvSafeText("@cmd"))
        assertEquals("'\tx", csvSafeText("\tx"))
    }

    @Test
    fun `ordinary text and empty text are not touched by the guard`() {
        assertEquals("Physics", csvSafeText("Physics"))
        assertEquals("", csvSafeText(""))
        assertEquals("a=b", csvSafeText("a=b")) // only a *leading* trigger matters
    }

    @Test
    fun `unsafe text removes only the guarding apostrophe`() {
        assertEquals("=SUM(A1)", csvUnsafeText("'=SUM(A1)"))
        assertEquals("'hello", csvUnsafeText("'hello")) // an apostrophe that isn't guarding anything stays
        assertEquals("'", csvUnsafeText("'"))
        assertEquals("Physics", csvUnsafeText("Physics"))
    }

    @Test
    fun `guard and un-guard round-trip`() {
        listOf("=1+1", "+x", "-x", "@y", "plain", "", "it's").forEach {
            assertEquals(it, csvUnsafeText(csvSafeText(it)))
        }
    }

    // ---------- parseCsv ----------

    @Test
    fun `parses simple rows`() {
        assertEquals(listOf(listOf("a", "b", "c"), listOf("1", "2", "3")), parseCsv("a,b,c\n1,2,3"))
    }

    @Test
    fun `parses quoted commas, doubled quotes and embedded newlines`() {
        val rows = parseCsv("\"a,b\",\"say \"\"hi\"\"\",\"x\ny\"\n")
        assertEquals(listOf(listOf("a,b", "say \"hi\"", "x\ny")), rows)
    }

    @Test
    fun `handles CRLF, lone CR and LF row endings`() {
        val expected = listOf(listOf("a", "b"), listOf("c", "d"))
        assertEquals(expected, parseCsv("a,b\r\nc,d"))
        assertEquals(expected, parseCsv("a,b\rc,d"))
        assertEquals(expected, parseCsv("a,b\nc,d"))
    }

    @Test
    fun `a trailing newline does not add an empty row`() {
        assertEquals(listOf(listOf("a", "b")), parseCsv("a,b\r\n"))
    }

    @Test
    fun `empty text has no rows and a blank line is a single empty cell`() {
        assertTrue(parseCsv("").isEmpty())
        assertEquals(listOf(listOf("a"), listOf(""), listOf("b")), parseCsv("a\n\nb"))
    }

    @Test
    fun `empty quoted and empty unquoted fields are both empty strings`() {
        assertEquals(listOf(listOf("a", "", "b")), parseCsv("a,\"\",b"))
        assertEquals(listOf(listOf("a", "", "b")), parseCsv("a,,b"))
    }

    @Test
    fun `an unterminated quote is an error, not a giant cell`() {
        try {
            parseCsv("a,\"never closed\nb,c")
            fail("expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertFalse(e.message.isNullOrBlank())
        }
    }

    @Test
    fun `escape then parse round-trips awkward values`() {
        val values = listOf("plain", "a,b", "say \"hi\"", "two\nlines", "", " padded ", "ünïcode ✓")
        val line = values.joinToString(",") { csvEscape(it) }
        assertEquals(listOf(values), parseCsv(line))
    }
}
