package com.autocheck.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/** Замена `options.txt`: логин, пароль и интервал хранятся в зашифрованных SharedPreferences. */
class SettingsStore(context: Context) {
    private val prefs: SharedPreferences = createPrefs(context.applicationContext)

    fun load() = Credentials(
        login = prefs.getString(KEY_LOGIN, "").orEmpty(),
        password = prefs.getString(KEY_PASSWORD, "").orEmpty(),
        timeoutSec = prefs.getInt(KEY_TIMEOUT, DEFAULT_TIMEOUT),
        route = runCatching { TrafficRoute.valueOf(prefs.getString(KEY_ROUTE, null).orEmpty()) }
            .getOrDefault(TrafficRoute.SYSTEM),
    )

    fun save(credentials: Credentials) = prefs.edit {
        putString(KEY_LOGIN, credentials.login)
        putString(KEY_PASSWORD, credentials.password)
        putInt(KEY_TIMEOUT, credentials.timeoutSec)
        putString(KEY_ROUTE, credentials.route.name)
    }

    fun loadNotifications(): NotificationPrefs {
        val d = NotificationPrefs()
        return NotificationPrefs(
            lessonStarted = prefs.getBoolean(KEY_N_LESSON, d.lessonStarted),
            errors = prefs.getBoolean(KEY_N_ERRORS, d.errors),
            warnings = prefs.getBoolean(KEY_N_WARNINGS, d.warnings),
            checks = prefs.getBoolean(KEY_N_CHECKS, d.checks),
            service = prefs.getBoolean(KEY_N_SERVICE, d.service),
            controlWhenStopped = prefs.getBoolean(KEY_N_CONTROL, d.controlWhenStopped),
        )
    }

    fun saveNotifications(value: NotificationPrefs) = prefs.edit {
        putBoolean(KEY_N_LESSON, value.lessonStarted)
        putBoolean(KEY_N_ERRORS, value.errors)
        putBoolean(KEY_N_WARNINGS, value.warnings)
        putBoolean(KEY_N_CHECKS, value.checks)
        putBoolean(KEY_N_SERVICE, value.service)
        putBoolean(KEY_N_CONTROL, value.controlWhenStopped)
    }

    fun loadThemeMode(): ThemeMode =
        runCatching { ThemeMode.valueOf(prefs.getString(KEY_THEME, null).orEmpty()) }
            .getOrDefault(ThemeMode.SYSTEM)

    fun saveThemeMode(mode: ThemeMode) = prefs.edit { putString(KEY_THEME, mode.name) }

    private companion object {
        const val FILE = "autocheck_secure_prefs"
        const val KEY_LOGIN = "login"
        const val KEY_PASSWORD = "password"
        const val KEY_TIMEOUT = "timeout"
        const val KEY_ROUTE = "route"
        const val KEY_THEME = "theme_mode"
        const val KEY_N_LESSON = "notify_lesson"
        const val KEY_N_ERRORS = "notify_errors"
        const val KEY_N_WARNINGS = "notify_warnings"
        const val KEY_N_CHECKS = "notify_checks"
        const val KEY_N_SERVICE = "notify_service"
        const val KEY_N_CONTROL = "notify_control"
        const val DEFAULT_TIMEOUT = 30

        fun createPrefs(context: Context): SharedPreferences {
            fun create(): SharedPreferences {
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                return EncryptedSharedPreferences.create(
                    context,
                    FILE,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
                )
            }
            return try {
                create()
            } catch (e: Exception) {
                // Keystore мог сброситься (например, после восстановления устройства) — создаём хранилище заново
                context.deleteSharedPreferences(FILE)
                create()
            }
        }
    }
}
