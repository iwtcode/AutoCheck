package com.autocheck.app.data

enum class LogLevel { INFO, SUCCESS, WARNING, ERROR }

data class LogEntry(
    val id: Long,
    val time: String,
    val level: LogLevel,
    val message: String,
)

enum class EngineStatus { STOPPED, CONNECTING, RUNNING, ERROR }

data class Credentials(
    val login: String = "",
    val password: String = "",
    val timeoutSec: Int = 30,
) {
    val isComplete get() = login.isNotBlank() && password.isNotBlank()
}

data class Stats(
    val cycles: Int = 0,
    val started: Int = 0,
    val lastCheck: String? = null,
)
