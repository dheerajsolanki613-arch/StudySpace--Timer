package com.studyspace.timer.data.transfer

/**
 * Phase 13 — a small, dependency-free RFC 4180 CSV reader/writer helpers.
 * Pure Kotlin so it's covered by fast local unit tests (`CsvCodecTest`).
 */

/** Quotes a field when it contains a comma, quote, or line break; doubles embedded quotes. */
fun csvEscape(value: String): String {
    val needsQuotes = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
    return if (needsQuotes) "\"" + value.replace("\"", "\"\"") + "\"" else value
}

private const val FORMULA_TRIGGERS = "=+-@\t\r"

/**
 * Guards against spreadsheet formula injection. A cell that *starts* with
 * `=`, `+`, `-`, `@`, tab or carriage return is treated as a formula by
 * Excel/Sheets/LibreOffice, so a subject or label the user typed as
 * `=HYPERLINK(...)` would run when the exported file is opened. The standard
 * mitigation is a leading apostrophe, which spreadsheets render as plain
 * text. [csvUnsafeText] removes exactly that guard on import so a
 * round-trip returns the original text.
 */
fun csvSafeText(value: String): String =
    if (value.isNotEmpty() && value[0] in FORMULA_TRIGGERS) "'$value" else value

/** Inverse of [csvSafeText]: strips a leading apostrophe only when it is guarding a formula trigger. */
fun csvUnsafeText(value: String): String =
    if (value.length >= 2 && value[0] == '\'' && value[1] in FORMULA_TRIGGERS) value.substring(1) else value

/**
 * Parses CSV text into rows of cells. Handles quoted fields, doubled quotes,
 * commas and line breaks inside quotes, and `\n`, `\r\n` or lone `\r` row
 * endings. A blank line comes back as a row with a single empty cell (callers
 * skip those); a trailing newline does not produce an extra empty row.
 *
 * @throws IllegalArgumentException if a quoted field is never closed — a
 *   truncated or corrupted file — rather than silently swallowing the rest of
 *   the file into one giant cell.
 */
fun parseCsv(text: String): List<List<String>> {
    val rows = mutableListOf<List<String>>()
    var row = mutableListOf<String>()
    val field = StringBuilder()
    var inQuotes = false

    fun endRow() {
        row.add(field.toString())
        field.setLength(0)
        rows.add(row)
        row = mutableListOf()
    }

    var i = 0
    while (i < text.length) {
        val c = text[i]
        if (inQuotes) {
            if (c == '"') {
                if (i + 1 < text.length && text[i + 1] == '"') {
                    field.append('"')
                    i++
                } else {
                    inQuotes = false
                }
            } else {
                field.append(c)
            }
        } else {
            when (c) {
                '"' -> inQuotes = true
                ',' -> {
                    row.add(field.toString())
                    field.setLength(0)
                }
                '\r' -> {
                    if (i + 1 < text.length && text[i + 1] == '\n') i++
                    endRow()
                }
                '\n' -> endRow()
                else -> field.append(c)
            }
        }
        i++
    }
    require(!inQuotes) { "A quoted value in the CSV is never closed." }
    if (field.isNotEmpty() || row.isNotEmpty()) endRow()
    return rows
}
