package com.autocheck.app.data

enum class LogLevel { INFO, SUCCESS, WARNING, ERROR }

data class LogEntry(
    val id: Long,
    val time: String,
    val level: LogLevel,
    val message: String,
)

enum class EngineStatus { STOPPED, CONNECTING, RUNNING, ERROR }

/** Как запросы приложения выходят в сеть. */
enum class TrafficRoute(val title: String, val description: String) {
    DIRECT(
        "Напрямую, мимо VPN",
        "Wi‑Fi, а если его нет — мобильная сеть. Если VPN выключен, это обычное соединение.",
    ),
    WIFI("Только Wi‑Fi, мимо VPN", "Запросы идут только через Wi‑Fi или Ethernet."),
    CELLULAR("Только мобильная сеть, мимо VPN", "Запросы идут только через мобильный интернет."),
    SYSTEM("Как в системе", "Android сам выбирает маршрут: через VPN, если он включён."),
}

data class Credentials(
    val login: String = "",
    val password: String = "",
    val timeoutSec: Int = 30,
    val route: TrafficRoute = TrafficRoute.DIRECT,
) {
    val isComplete get() = login.isNotBlank() && password.isNotBlank()
}

data class Stats(
    val cycles: Int = 0,
    val started: Int = 0,
    val lastCheck: String? = null,
)
