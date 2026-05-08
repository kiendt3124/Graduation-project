package com.uet.expensetracker.features.ai.ui.chat

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp

/**
 * Enhanced markdown renderer for bot messages.
 * Supports:
 * - **bold**
 * - `inline code`
 * - Headers (###, ##, #)
 * - Bullet lists (* and -)
 * - Tables (converted to readable format)
 *
 * This is intentionally small and safe for production (no HTML).
 */
@Composable
fun MarkdownText(
    markdown: String,
    maxLines: Int = Int.MAX_VALUE,
) {
    Text(
        text = markdownToAnnotatedString(markdown),
        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
        maxLines = maxLines,
        overflow = TextOverflow.Clip
    )
}

private fun markdownToAnnotatedString(input: String): AnnotatedString {
    // Preprocess: handle tables, headers, and lists
    var processed = input.replace("\r\n", "\n")
    
    // Convert tables to readable format
    processed = convertTablesToText(processed)
    
    // Process headers (remove ### and make bold)
    processed = processHeaders(processed)
    
    // Process lists (normalize bullets)
    processed = normalizeLists(processed)
    
    return buildAnnotatedString {
        var i = 0
        while (i < processed.length) {
            // Inline code: `...`
            if (processed[i] == '`') {
                val end = processed.indexOf('`', startIndex = i + 1)
                if (end != -1) {
                    val code = processed.substring(i + 1, end)
                    pushStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = androidx.compose.ui.graphics.Color(0x1F000000)
                        )
                    )
                    append(code)
                    pop()
                    i = end + 1
                    continue
                }
            }

            // Bold: **...**
            if (i + 1 < processed.length && processed[i] == '*' && processed[i + 1] == '*') {
                val end = processed.indexOf("**", startIndex = i + 2)
                if (end != -1) {
                    val bold = processed.substring(i + 2, end)
                    pushStyle(SpanStyle(fontWeight = FontWeight.SemiBold))
                    append(bold)
                    pop()
                    i = end + 2
                    continue
                }
            }

            append(processed[i])
            i++
        }
    }
}

/**
 * Convert markdown tables to readable text format.
 * Tables are converted to a formatted text representation.
 */
private fun convertTablesToText(input: String): String {
    val lines = input.split("\n")
    val result = mutableListOf<String>()
    var i = 0
    
    while (i < lines.size) {
        val line = lines[i]
        
        // Check if this line looks like a table row (contains |)
        if (line.trim().startsWith("|") && line.trim().endsWith("|")) {
            val tableRows = mutableListOf<String>()
            var j = i
            
            // Collect all consecutive table rows
            while (j < lines.size && lines[j].trim().matches(Regex("^\\|.*\\|$"))) {
                val row = lines[j].trim()
                // Skip separator rows (|---|---| or |:---| etc.)
                // A separator row contains only pipes, spaces, dashes, and colons
                val isSeparator = row.replace("|", "").replace(" ", "").replace("-", "").replace(":", "").isEmpty() && 
                                  row.contains("-")
                if (!isSeparator) {
                    tableRows.add(row)
                }
                j++
            }
            
            if (tableRows.size >= 2) {
                // Parse table
                val parsedTable = parseTable(tableRows)
                result.add(formatTableAsText(parsedTable))
                i = j
                continue
            }
        }
        
        result.add(line)
        i++
    }
    
    return result.joinToString("\n")
}

private fun parseTable(rows: List<String>): List<List<String>> {
    return rows.map { row ->
        row.split("|")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }
}

private fun formatTableAsText(table: List<List<String>>): String {
    if (table.isEmpty()) return ""
    
    val result = StringBuilder()
    
    // If table has headers, format each data row with header labels
    if (table.size > 1) {
        val headers = table[0]
        for (rowIndex in 1 until table.size) {
            val row = table[rowIndex]
            // Build a formatted line for this row
            val rowParts = mutableListOf<String>()
            row.forEachIndexed { colIndex, cell ->
                if (colIndex < headers.size) {
                    val header = headers[colIndex].trim()
                    val value = cell.trim()
                    if (value.isNotEmpty()) {
                        rowParts.add("$header: $value")
                    }
                }
            }
            if (rowParts.isNotEmpty()) {
                if (result.isNotEmpty()) {
                    result.append("\n")
                }
                result.append("• ").append(rowParts.joinToString(" • "))
            }
        }
    } else {
        // Single row (no headers), just format normally
        table[0].forEachIndexed { colIndex, cell ->
            if (colIndex > 0) result.append(" • ")
            result.append(cell.trim())
        }
    }
    
    return result.toString()
}

/**
 * Process markdown headers (###, ##, #) by removing the prefix and making them bold.
 * Headers are converted to bold text with a newline before them.
 */
private fun processHeaders(input: String): String {
    return input.split("\n").joinToString("\n") { line ->
        when {
            line.trim().startsWith("### ") -> {
                val text = line.trim().removePrefix("### ").trim()
                "**$text**"
            }
            line.trim().startsWith("## ") -> {
                val text = line.trim().removePrefix("## ").trim()
                "**$text**"
            }
            line.trim().startsWith("# ") -> {
                val text = line.trim().removePrefix("# ").trim()
                "**$text**"
            }
            else -> line
        }
    }
}

/**
 * Normalize list bullets: convert * to • (bullet point) for consistency.
 * This ensures both * and - lists are displayed consistently.
 */
private fun normalizeLists(input: String): String {
    return input.split("\n").joinToString("\n") { line ->
        val trimmed = line.trimStart()
        when {
            trimmed.startsWith("* ") -> {
                val indent = line.length - trimmed.length
                " ".repeat(indent) + "• " + trimmed.removePrefix("* ")
            }
            trimmed.startsWith("- ") -> {
                val indent = line.length - trimmed.length
                " ".repeat(indent) + "• " + trimmed.removePrefix("- ")
            }
            else -> line
        }
    }
}


