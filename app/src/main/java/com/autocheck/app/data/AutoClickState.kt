package com.autocheck.app.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicLong

/** Общее состояние: сервис пишет, UI читает. Аналог класса `Log` из Python-версии. */
object AutoClickState {
    private const val MAX_LOGS = 300
    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss")
    private val counter = AtomicLong()

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val _status = MutableStateFlow(EngineStatus.STOPPED)
    val status: StateFlow<EngineStatus> = _status.asStateFlow()

    private val _stats = MutableStateFlow(Stats())
    val stats: StateFlow<Stats> = _stats.asStateFlow()

    private fun now(): String = LocalTime.now().format(timeFormat)

    fun log(level: LogLevel, message: String) {
        val entry = LogEntry(counter.incrementAndGet(), now(), level, message)
        _logs.update { (it + entry).takeLast(MAX_LOGS) }
    }

    fun info(message: String) = log(LogLevel.INFO, message)
    fun success(message: String) = log(LogLevel.SUCCESS, message)
    fun warning(message: String) = log(LogLevel.WARNING, message)
    fun error(message: String) = log(LogLevel.ERROR, message)

    fun setStatus(status: EngineStatus) {
        _status.value = status
    }

    fun onCycleFinished() {
        _stats.update { it.copy(cycles = it.cycles + 1, lastCheck = now()) }
    }

    fun onLessonStarted() {
        _stats.update { it.copy(started = it.started + 1) }
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }

    fun resetForNewRun() {
        _stats.value = Stats()
        _status.value = EngineStatus.CONNECTING
    }
}
