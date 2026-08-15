package com.example.model

enum class LogLevel { INFO, WARNING, ERROR, DEBUG }

data class LogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val tag: String,
    val message: String,
    val level: LogLevel = LogLevel.INFO
)
