package com.example.logger

import android.util.Log
import com.example.model.LogEntry
import com.example.model.LogLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

object AppLogger {
    private const val MAX_LOGS = 500
    private val logBuffer = mutableListOf<LogEntry>()
    private val _logsFlow = MutableStateFlow<List<LogEntry>>(emptyList())
    val logsFlow: StateFlow<List<LogEntry>> = _logsFlow.asStateFlow()

    private val uuidRegex = Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
    private val vlessUrlRegex = Pattern.compile("vless://[^@]+@")

    fun i(tag: String, message: String) = log(LogLevel.INFO, tag, message)
    fun w(tag: String, message: String) = log(LogLevel.WARNING, tag, message)
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val fullMsg = if (throwable != null) "$message: ${throwable.localizedMessage}" else message
        log(LogLevel.ERROR, tag, fullMsg)
    }
    fun d(tag: String, message: String) = log(LogLevel.DEBUG, tag, message)

    private fun log(level: LogLevel, tag: String, rawMessage: String) {
        val sanitized = sanitize(rawMessage)
        val entry = LogEntry(
            timestamp = System.currentTimeMillis(),
            tag = tag,
            message = sanitized,
            level = level
        )

        when (level) {
            LogLevel.INFO -> Log.i(tag, sanitized)
            LogLevel.WARNING -> Log.w(tag, sanitized)
            LogLevel.ERROR -> Log.e(tag, sanitized)
            LogLevel.DEBUG -> Log.d(tag, sanitized)
        }

        synchronized(logBuffer) {
            logBuffer.add(entry)
            if (logBuffer.size > MAX_LOGS) {
                logBuffer.removeAt(0)
            }
            _logsFlow.value = ArrayList(logBuffer)
        }
    }

    fun clear() {
        synchronized(logBuffer) {
            logBuffer.clear()
            _logsFlow.value = emptyList()
        }
    }

    private fun sanitize(input: String): String {
        var result = input
        result = uuidRegex.matcher(result).replaceAll("********-****-****-****-************")
        result = vlessUrlRegex.matcher(result).replaceAll("vless://********@")
        return result
    }

    fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
        return sdf.format(Date(timestamp))
    }
}
