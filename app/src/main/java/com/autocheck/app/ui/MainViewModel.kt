package com.autocheck.app.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.autocheck.app.data.AutoClickState
import com.autocheck.app.data.Credentials
import com.autocheck.app.data.NotificationPrefs
import com.autocheck.app.data.SettingsStore
import com.autocheck.app.data.TrafficRoute
import com.autocheck.app.service.AutoClickService

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val store = SettingsStore(app)
    private val initial = store.load()

    var saved by mutableStateOf(initial)
        private set

    var login by mutableStateOf(initial.login)
    var password by mutableStateOf(initial.password)
    var timeout by mutableIntStateOf(initial.timeoutSec)
    var route by mutableStateOf(initial.route)

    /** Настройки уведомлений применяются сразу и не требуют кнопки «Сохранить». */
    var notifications by mutableStateOf(store.loadNotifications())
        private set

    init {
        AutoClickState.setNotificationPrefs(notifications)
    }

    val isDirty: Boolean
        get() = login.trim() != saved.login || password != saved.password || timeout != saved.timeoutSec || route != saved.route

    val canStart: Boolean
        get() = login.isNotBlank() && password.isNotBlank()

    fun save() {
        saved = Credentials(login.trim(), password, timeout, route)
        store.save(saved)
    }

    fun updateNotifications(transform: (NotificationPrefs) -> NotificationPrefs) {
        notifications = transform(notifications)
        store.saveNotifications(notifications)
        AutoClickState.setNotificationPrefs(notifications)
    }

    fun start() {
        save()
        AutoClickService.start(getApplication<Application>())
    }

    fun stop() = AutoClickService.stop(getApplication<Application>())

    fun clearLogs() = AutoClickState.clearLogs()
}
