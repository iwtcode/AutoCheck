package com.autocheck.app.data

import java.util.UUID

enum class LogLevel { INFO, SUCCESS, WARNING, ERROR }

/**
 * Тип события. По нему определяется, показывать ли запись в уведомлении
 * (см. [NotificationPrefs]). В журнале записи видны всегда.
 */
enum class LogCategory {
    /** Вход, смена сети, запуск и остановка. */
    SERVICE,

    /** Результат успешной проверки расписания. */
    CHECK,

    /** Занятие начато. */
    LESSON,

    /** Предупреждение, например истёкшая сессия. */
    WARNING,

    /** Ошибка подключения или авторизации. */
    ERROR,

    /** Пауза между проверками. В уведомлении не показывается никогда. */
    PAUSE,
}

data class LogEntry(
    val id: Long,
    val time: String,
    val level: LogLevel,
    val message: String,
    val category: LogCategory = LogCategory.SERVICE,
)

enum class EngineStatus { STOPPED, CONNECTING, RUNNING, ERROR }

/** Как запросы приложения выходят в сеть. Первый вариант используется по умолчанию. */
enum class TrafficRoute(val title: String, val description: String) {
    SYSTEM(
        "Как в системе",
        "Маршрут выбирает Android: если VPN включён, запросы идут через него.",
    ),
    DIRECT(
        "Напрямую, без VPN",
        "Через Wi‑Fi, а если его нет — через мобильную сеть, не используя VPN. " +
            "Если VPN выключен, это обычное соединение.",
    ),
    WIFI("Только Wi‑Fi, без VPN", "Запросы идут только через Wi‑Fi или Ethernet, не используя VPN."),
    CELLULAR(
        "Только мобильная сеть, без VPN",
        "Запросы идут только через мобильный интернет, не используя VPN.",
    ),
}

/** Тема оформления приложения. */
enum class ThemeMode(val title: String, val description: String) {
    SYSTEM("Как в системе", "Тема меняется вместе с настройкой Android."),
    LIGHT("Светлая", "Светлое оформление независимо от настроек Android."),
    DARK("Тёмная", "Тёмное оформление независимо от настроек Android."),
}

/** Как аккаунт входит в личный кабинет. */
enum class AccountType(val title: String) {
    /** Логин и пароль: приложение входит само и при истечении сессии входит заново. */
    PASSWORD("Логин и пароль"),

    /** Готовый токен: значение cookie `miden` из браузера. Повторный вход невозможен. */
    TOKEN("Токен"),
}

/** Один аккаунт личного кабинета. [id] нужен, чтобы отличать аккаунты и хранить сессию каждого отдельно. */
data class Account(
    val id: String = UUID.randomUUID().toString(),
    val type: AccountType = AccountType.PASSWORD,
    val login: String = "",
    val password: String = "",
    val token: String = "",
) {
    val isComplete: Boolean
        get() = when (type) {
            AccountType.PASSWORD -> login.isNotBlank() && password.isNotBlank()
            AccountType.TOKEN -> token.isNotBlank()
        }

    /** Ничего не введено: такой аккаунт при сохранении отбрасывается. */
    val isBlank: Boolean
        get() = login.isBlank() && password.isEmpty() && token.isBlank()

    /** Подпись для журнала и списка: логин либо последние символы токена. */
    val label: String
        get() = when (type) {
            AccountType.PASSWORD -> login.ifBlank { "без логина" }
            AccountType.TOKEN -> "токен ...${token.takeLast(4)}"
        }

    /** Убирает лишние пробелы и поля, не относящиеся к выбранному способу входа. */
    fun normalized(): Account = when (type) {
        AccountType.PASSWORD -> copy(login = login.trim(), token = "")
        AccountType.TOKEN -> copy(login = "", password = "", token = token.trim())
    }
}

data class Credentials(
    val accounts: List<Account> = emptyList(),
    val timeoutSec: Int = 30,
    val route: TrafficRoute = TrafficRoute.SYSTEM,
) {
    /** Есть хотя бы один аккаунт, с которым можно работать. */
    val isComplete get() = accounts.any { it.isComplete }
}

/**
 * Какие события показывать в уведомлении. Само уведомление одно и обновляется на месте:
 * настройки определяют только его текст.
 */
data class NotificationPrefs(
    val lessonStarted: Boolean = true,
    val errors: Boolean = true,
    val warnings: Boolean = false,
    val checks: Boolean = false,
    val service: Boolean = false,
    /** После остановки оставить уведомление с кнопкой «Включить». */
    val controlWhenStopped: Boolean = true,
) {
    fun shows(category: LogCategory): Boolean = when (category) {
        LogCategory.LESSON -> lessonStarted
        LogCategory.ERROR -> errors
        LogCategory.WARNING -> warnings
        LogCategory.CHECK -> checks
        LogCategory.SERVICE -> service
        LogCategory.PAUSE -> false
    }

    /** Сколько типов событий включено (для краткого описания в настройках). */
    val enabledEventTypes: Int
        get() = listOf(lessonStarted, errors, warnings, checks, service).count { it }

    companion object {
        const val EVENT_TYPES = 5
    }
}

data class Stats(
    val cycles: Int = 0,
    val started: Int = 0,
    val lastCheck: String? = null,
)
