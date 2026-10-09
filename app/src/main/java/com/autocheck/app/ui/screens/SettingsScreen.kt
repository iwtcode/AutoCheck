package com.autocheck.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.autocheck.app.data.NotificationPrefs
import com.autocheck.app.data.ThemeMode
import com.autocheck.app.data.TrafficRoute
import com.autocheck.app.ui.MainViewModel
import kotlin.math.roundToInt

/** Разделы настроек. Главная страница настроек — список этих разделов. */
private enum class SettingsPage(val title: String, val icon: ImageVector) {
    Account("Аккаунт", Icons.Rounded.Person),
    Connection("Подключение", Icons.Rounded.Wifi),
    Notifications("Уведомления", Icons.Rounded.Notifications),
    Appearance("Оформление", Icons.Rounded.Palette),
    Background("Работа в фоне", Icons.Rounded.BatteryChargingFull),
}

@Composable
fun SettingsScreen(vm: MainViewModel, running: Boolean) {
    val context = LocalContext.current
    var page by rememberSaveable { mutableStateOf<SettingsPage?>(null) }
    var ignoringBattery by remember { mutableStateOf(isIgnoringBatteryOptimizations(context)) }
    var notificationsAllowed by remember { mutableStateOf(areNotificationsAllowed(context)) }

    // Обновляем состояние после возврата из системных настроек
    LifecycleResumeEffect(Unit) {
        ignoringBattery = isIgnoringBatteryOptimizations(context)
        notificationsAllowed = areNotificationsAllowed(context)
        onPauseOrDispose { }
    }

    // Системная кнопка «Назад» возвращает к списку разделов
    BackHandler(enabled = page != null) { page = null }

    val showSaveBar = vm.isDirty &&
        (page == null || page == SettingsPage.Account || page == SettingsPage.Connection)

    Column(Modifier.fillMaxSize()) {
        Crossfade(
            targetState = page,
            label = "settings-page",
            modifier = Modifier.weight(1f),
        ) { current ->
            when (current) {
                null -> SettingsHub(
                    vm = vm,
                    ignoringBattery = ignoringBattery,
                    onOpen = { page = it },
                )

                SettingsPage.Account -> AccountPage(vm, onBack = { page = null })
                SettingsPage.Connection -> ConnectionPage(vm, onBack = { page = null })
                SettingsPage.Notifications -> NotificationsPage(
                    vm = vm,
                    notificationsAllowed = notificationsAllowed,
                    onBack = { page = null },
                )

                SettingsPage.Appearance -> AppearancePage(vm, onBack = { page = null })

                SettingsPage.Background -> BackgroundPage(
                    ignoringBattery = ignoringBattery,
                    onBack = { page = null },
                )
            }
        }

        if (showSaveBar) {
            SaveBar(onSave = vm::save, running = running)
        }
    }
}

// ───────────────────────── Главная страница настроек ─────────────────────────

@Composable
private fun SettingsHub(vm: MainViewModel, ignoringBattery: Boolean, onOpen: (SettingsPage) -> Unit) {
    PageScaffold {
        Text(
            "Настройки",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(start = 4.dp),
        )

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Column {
                SettingsPage.entries.forEachIndexed { index, item ->
                    if (index > 0) GroupDivider()
                    NavRow(
                        icon = item.icon,
                        title = item.title,
                        summary = summaryOf(item, vm, ignoringBattery),
                        onClick = { onOpen(item) },
                    )
                }
            }
        }
    }
}

private fun summaryOf(page: SettingsPage, vm: MainViewModel, ignoringBattery: Boolean): String =
    when (page) {
        SettingsPage.Account -> vm.login.trim().ifEmpty { "Логин и пароль не указаны" }
        SettingsPage.Connection -> "${vm.route.title} · проверка каждые ${vm.timeout} с"
        SettingsPage.Notifications ->
            "Типов событий в уведомлении: ${vm.notifications.enabledEventTypes} из ${NotificationPrefs.EVENT_TYPES}"

        SettingsPage.Appearance -> vm.themeMode.title

        SettingsPage.Background ->
            if (ignoringBattery) "Оптимизация батареи отключена" else "Рекомендуется отключить оптимизацию батареи"
    }

// ───────────────────────── Разделы ─────────────────────────

@Composable
private fun AccountPage(vm: MainViewModel, onBack: () -> Unit) {
    var showPassword by remember { mutableStateOf(false) }

    PageScaffold {
        PageHeader("Аккаунт", onBack)

        Section(
            title = "Вход в lk.sut.ru",
            hint = "Данные хранятся на устройстве в зашифрованном виде.",
        ) {
            OutlinedTextField(
                value = vm.login,
                onValueChange = { vm.login = it },
                label = { Text("Логин") },
                leadingIcon = { Icon(Icons.Rounded.Person, null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = vm.password,
                onValueChange = { vm.password = it },
                label = { Text("Пароль") },
                leadingIcon = { Icon(Icons.Rounded.Lock, null) },
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            contentDescription = if (showPassword) "Скрыть пароль" else "Показать пароль",
                        )
                    }
                },
                singleLine = true,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ConnectionPage(vm: MainViewModel, onBack: () -> Unit) {
    PageScaffold {
        PageHeader("Подключение", onBack)

        Section(
            title = "Маршрут трафика",
            hint = "Режимы «без VPN» не сработают, если в настройках VPN включена блокировка соединений без VPN. " +
                "Если выбранной сети нет, запрос не отправляется и будет повторён при следующей проверке.",
        ) {
            Column {
                TrafficRoute.entries.forEach { option ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .selectable(
                                selected = vm.route == option,
                                onClick = { vm.route = option },
                                role = Role.RadioButton,
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        RadioButton(selected = vm.route == option, onClick = null)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(option.title, style = MaterialTheme.typography.titleSmall)
                            Text(
                                option.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        Section(
            title = "Интервал проверки",
            hint = "Как часто приложение проверяет расписание. Чем меньше значение, тем быстрее срабатывает " +
                "автопосещение и тем выше расход батареи.",
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Каждые", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${vm.timeout} сек", style = MaterialTheme.typography.titleMedium)
            }
            Slider(
                value = vm.timeout.toFloat(),
                onValueChange = { vm.timeout = (it / 5).roundToInt() * 5 },
                valueRange = 5f..300f,
                steps = 58,
            )
        }
    }
}

@Composable
private fun NotificationsPage(vm: MainViewModel, notificationsAllowed: Boolean, onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = vm.notifications

    PageScaffold {
        PageHeader("Уведомления", onBack)

        if (!notificationsAllowed) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Row(
                    Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "Уведомления отключены в настройках Android",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { openNotificationSettings(context) }) { Text("Открыть") }
                }
            }
        }

        Text(
            "Приложение показывает одно уведомление. Оно обновляется на месте, новые не создаются. " +
                "Ниже выберите, какие события отображаются в его тексте.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )

        GroupLabel("События в уведомлении")
        SettingsGroup {
            SwitchRow(
                title = "Занятие начато",
                description = "Когда приложение успешно начало занятие.",
                checked = prefs.lessonStarted,
                onCheckedChange = { on -> vm.updateNotifications { it.copy(lessonStarted = on) } },
            )
            GroupDivider()
            SwitchRow(
                title = "Ошибки",
                description = "Сбои подключения, неверный логин или пароль.",
                checked = prefs.errors,
                onCheckedChange = { on -> vm.updateNotifications { it.copy(errors = on) } },
            )
            GroupDivider()
            SwitchRow(
                title = "Предупреждения",
                description = "Например, истёкшая сессия, из-за которой нужен повторный вход.",
                checked = prefs.warnings,
                onCheckedChange = { on -> vm.updateNotifications { it.copy(warnings = on) } },
            )
            GroupDivider()
            SwitchRow(
                title = "Результат каждой проверки",
                description = "Например, «Нет активных занятий». Уведомление будет обновляться при каждой проверке.",
                checked = prefs.checks,
                onCheckedChange = { on -> vm.updateNotifications { it.copy(checks = on) } },
            )
            GroupDivider()
            SwitchRow(
                title = "Служебные сообщения",
                description = "Вход в кабинет, смена сети, запуск и остановка.",
                checked = prefs.service,
                onCheckedChange = { on -> vm.updateNotifications { it.copy(service = on) } },
            )
        }

        GroupLabel("Управление")
        SettingsGroup {
            SwitchRow(
                title = "Кнопка «Включить» после остановки",
                description = "После остановки уведомление остаётся в шторке, и проверку можно снова " +
                    "запустить прямо из него. Уведомление можно смахнуть.",
                checked = prefs.controlWhenStopped,
                onCheckedChange = { on -> vm.updateNotifications { it.copy(controlWhenStopped = on) } },
            )
        }

        Text(
            "Изменения применяются сразу. Пока приложение работает, в уведомлении есть кнопка «Остановить».",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}

@Composable
private fun AppearancePage(vm: MainViewModel, onBack: () -> Unit) {
    PageScaffold {
        PageHeader("Оформление", onBack)

        Section(
            title = "Тема",
            hint = "Изменение применяется сразу.",
        ) {
            Column {
                ThemeMode.entries.forEach { option ->
                    OptionRow(
                        selected = vm.themeMode == option,
                        title = option.title,
                        description = option.description,
                        onClick = { vm.updateThemeMode(option) },
                    )
                }
            }
        }
    }
}

@Composable
private fun BackgroundPage(ignoringBattery: Boolean, onBack: () -> Unit) {
    val context = LocalContext.current

    PageScaffold {
        PageHeader("Работа в фоне", onBack)

        Section(
            title = "Оптимизация батареи",
            hint = "Android может ограничивать работу приложений при выключенном экране. " +
                "Исключите AutoCheck из оптимизации батареи, чтобы проверки не прерывались.",
        ) {
            if (ignoringBattery) {
                Text("Оптимизация батареи отключена ✓", color = MaterialTheme.colorScheme.secondary)
            } else {
                OutlinedButton(
                    onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                                .setData(Uri.parse("package:${context.packageName}"))
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Отключить оптимизацию батареи") }
            }
        }
    }
}

// ───────────────────────── Общие элементы ─────────────────────────

/** Прокручиваемая страница с едиными отступами. */
@Composable
private fun PageScaffold(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        content()
    }
}

@Composable
private fun PageHeader(title: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад")
        }
        Text(title, style = MaterialTheme.typography.headlineSmall)
    }
}

/** Панель «Сохранить» закреплена внизу, пока есть несохранённые изменения. */
@Composable
private fun SaveBar(onSave: () -> Unit, running: Boolean) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        Row(
            Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Есть несохранённые изменения",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                if (running) {
                    Text(
                        "Применятся при следующем запуске",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Button(onClick = onSave, shape = RoundedCornerShape(14.dp)) { Text("Сохранить") }
        }
    }
}

/** Строка с радиокнопкой: название и пояснение. */
@Composable
private fun OptionRow(selected: Boolean, title: String, description: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NavRow(icon: ImageVector, title: String, summary: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = checked, onValueChange = onCheckedChange, role = Role.Switch)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun GroupLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp),
    )
}

@Composable
private fun SettingsGroup(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column { content() }
    }
}

@Composable
private fun GroupDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 20.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
    )
}

@Composable
private fun Section(
    title: String,
    hint: String,
    content: @Composable () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
            Text(
                hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun isIgnoringBatteryOptimizations(context: Context): Boolean =
    context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)

private fun areNotificationsAllowed(context: Context): Boolean =
    NotificationManagerCompat.from(context).areNotificationsEnabled()

private fun openNotificationSettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    )
}
