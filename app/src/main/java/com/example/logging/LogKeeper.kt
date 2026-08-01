package com.example.logging

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LogEntry(
    val timestamp: String,
    val tag: String,
    val message: String,
    val stackTrace: String? = null
)

object LogKeeper {
    private const val LOG_FILE_NAME = "log_keeper.txt"
    private const val MAX_LOG_SIZE_BYTES = 500 * 1024 // 500 KB limit for rolling log
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    // Regex patterns for sensitive credential exclusions
    private val SENSITIVE_PATTERNS = listOf(
        Regex("(?i)password[\"']?\\s*[:=]\\s*[\"']?[^\"'\\s]+"),
        Regex("(?i)bearer\\s+[a-zA-Z0-9\\-\\_\\.\\~\\+\\/\\=]+"),
        Regex("AIzaSy[a-zA-Z0-9\\-_]{33}"), // Gemini/Google API key
        Regex("sk-[a-zA-Z0-9]{32,}")        // OpenAI / generic API key
    )

    fun log(context: Context, tag: String, message: String, throwable: Throwable? = null) {
        val sanitizedMessage = sanitize(message)
        val sanitizedTrace = throwable?.let { sanitize(it.stackTraceToString()) }
        val timestamp = dateFormat.format(Date())

        val logLine = StringBuilder()
            .append("[$timestamp] [$tag] $sanitizedMessage")
            .apply {
                if (!sanitizedTrace.isNullOrEmpty()) {
                    append("\nStacktrace: $sanitizedTrace")
                }
            }
            .append("\n---\n")
            .toString()

        try {
            val logFile = File(context.filesDir, LOG_FILE_NAME)
            if (logFile.exists() && logFile.length() > MAX_LOG_SIZE_BYTES) {
                // Trim old log entries if file exceeds limit
                val content = logFile.readText()
                val trimmed = content.takeLast((MAX_LOG_SIZE_BYTES * 0.7).toInt())
                logFile.writeText("...[older logs trimmed]...\n" + trimmed)
            }
            logFile.appendText(logLine)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getLogsAsString(context: Context): String {
        val logFile = File(context.filesDir, LOG_FILE_NAME)
        return if (logFile.exists()) {
            logFile.readText().ifBlank { "No logs recorded yet." }
        } else {
            "No logs recorded yet."
        }
    }

    fun clearLogs(context: Context) {
        try {
            val logFile = File(context.filesDir, LOG_FILE_NAME)
            if (logFile.exists()) {
                logFile.writeText("")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    inline fun catchAndLog(context: Context, tag: String, actionName: String, block: () -> Unit) {
        try {
            block()
        } catch (t: Throwable) {
            log(context, tag, "Exception during $actionName: ${t.message}", t)
        }
    }

    private fun sanitize(input: String): String {
        var result = input
        for (pattern in SENSITIVE_PATTERNS) {
            result = pattern.replace(result, "[REDACTED_CREDENTIAL]")
        }
        return result
    }
}
