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
            .getOrDefault(TrafficRoute.DIRECT),
    )

    fun save(credentials: Credentials) = prefs.edit {
        putString(KEY_LOGIN, credentials.login)
        putString(KEY_PASSWORD, credentials.password)
        putInt(KEY_TIMEOUT, credentials.timeoutSec)
        putString(KEY_ROUTE, credentials.route.name)
    }

    private companion object {
        const val FILE = "autocheck_secure_prefs"
        const val KEY_LOGIN = "login"
        const val KEY_PASSWORD = "password"
        const val KEY_TIMEOUT = "timeout"
        const val KEY_ROUTE = "route"
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
                // Keystore мог сброситься (восстановление устройства и т.п.) — начинаем с чистого листа
                context.deleteSharedPreferences(FILE)
                create()
            }
        }
    }
}
