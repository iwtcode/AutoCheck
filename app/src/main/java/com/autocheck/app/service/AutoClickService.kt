package com.autocheck.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.autocheck.app.MainActivity
import com.autocheck.app.R
import com.autocheck.app.data.AutoClickEngine
import com.autocheck.app.data.AutoClickState
import com.autocheck.app.data.EngineStatus
import com.autocheck.app.data.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Foreground-сервис: держит цикл проверки живым, пока экран выключен. */
class AutoClickService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var engineJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel(CHANNEL_ID, "Автопосещение", NotificationManager.IMPORTANCE_LOW)
        channel.description = "Статус работы автопосещения"
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            engineJob?.cancel()
            if (engineJob == null) stopSelf()
            return START_NOT_STICKY
        }
        return try {
            startEngine()
            START_STICKY
        } catch (e: Exception) {
            // например, ограничения запуска foreground-сервисов из фона
            AutoClickState.error("не удалось запустить сервис: ${e.message}")
            stopSelf()
            START_NOT_STICKY
        }
    }

    private fun startEngine() {
        if (engineJob?.isActive == true) return

        ServiceCompat.startForeground(
            this, NOTIFICATION_ID, buildNotification("Подключение…"),
            if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0,
        )
        acquireWakeLock()

        val credentials = SettingsStore(this).load()
        AutoClickState.resetForNewRun()
        if (!credentials.isComplete) {
            AutoClickState.error("укажите логин и пароль в настройках")
            AutoClickState.setStatus(EngineStatus.ERROR)
            stopSelf()
            return
        }
        AutoClickState.info("запуск автопосещения")

        // Обновляем уведомление последней строкой журнала
        scope.launch {
            AutoClickState.logs.collect { logs ->
                logs.lastOrNull()?.let {
                    getSystemService(NotificationManager::class.java)
                        .notify(NOTIFICATION_ID, buildNotification(it.message))
                }
            }
        }

        engineJob = scope.launch {
            try {
                AutoClickEngine().run(credentials)
            } finally {
                if (AutoClickState.status.value != EngineStatus.ERROR) {
                    AutoClickState.setStatus(EngineStatus.STOPPED)
                    AutoClickState.info("автопосещение остановлено")
                }
                stopSelf()
            }
        }
    }

    private fun buildNotification(text: String): Notification {
        val openApp = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stop = PendingIntent.getService(
            this, 1,
            Intent(this, AutoClickService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_check)
            .setContentTitle("AutoCheck работает")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openApp)
            .addAction(0, "Остановить", stop)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "autocheck:engine")
            .apply {
                setReferenceCounted(false)
                acquire()
            }
    }

    override fun onDestroy() {
        wakeLock?.takeIf { it.isHeld }?.release()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "autocheck_running"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_START = "com.autocheck.app.START"
        private const val ACTION_STOP = "com.autocheck.app.STOP"

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, AutoClickService::class.java).setAction(ACTION_START),
            )
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, AutoClickService::class.java).setAction(ACTION_STOP)
            )
        }
    }
}
