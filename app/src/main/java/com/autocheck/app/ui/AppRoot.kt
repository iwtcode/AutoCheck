package com.autocheck.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.autocheck.app.data.AutoClickState
import com.autocheck.app.data.EngineStatus
import com.autocheck.app.ui.screens.HomeScreen
import com.autocheck.app.ui.screens.LogScreen
import com.autocheck.app.ui.screens.SettingsScreen

private enum class Tab(val label: String, val icon: ImageVector) {
    Home("Главная", Icons.Rounded.Home),
    Log("Журнал", Icons.Rounded.History),
    Settings("Настройки", Icons.Rounded.Settings),
}

@Composable
fun AppRoot(vm: MainViewModel) {
    var tab by rememberSaveable { mutableStateOf(Tab.Home) }
    val context = LocalContext.current

    val status by AutoClickState.status.collectAsStateWithLifecycle()
    val stats by AutoClickState.stats.collectAsStateWithLifecycle()
    val logs by AutoClickState.logs.collectAsStateWithLifecycle()

    // Разрешение на уведомления нужно на Android 13+; сервис стартует в любом случае
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { vm.start() }

    val onToggle: () -> Unit = {
        if (status == EngineStatus.RUNNING || status == EngineStatus.CONNECTING) {
            vm.stop()
        } else if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            vm.start()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                Tab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(item.label) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        Crossfade(targetState = tab, label = "tabs", modifier = Modifier.padding(padding)) { current ->
            when (current) {
                Tab.Home -> HomeScreen(
                    status = status,
                    stats = stats,
                    recentLogs = logs.takeLast(4).reversed(),
                    timeoutSec = vm.saved.timeoutSec,
                    hasCredentials = vm.canStart,
                    onToggle = onToggle,
                    onOpenLog = { tab = Tab.Log },
                    onOpenSettings = { tab = Tab.Settings },
                )

                Tab.Log -> LogScreen(logs = logs.asReversed(), onClear = vm::clearLogs)
                Tab.Settings -> SettingsScreen(
                    vm = vm,
                    running = status == EngineStatus.RUNNING || status == EngineStatus.CONNECTING,
                )
            }
        }
    }
}
