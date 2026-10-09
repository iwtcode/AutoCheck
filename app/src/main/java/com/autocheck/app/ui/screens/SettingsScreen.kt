package com.autocheck.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.RadioButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import com.autocheck.app.data.TrafficRoute
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.autocheck.app.ui.MainViewModel
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(vm: MainViewModel, running: Boolean) {
    val context = LocalContext.current
    var showPassword by remember { mutableStateOf(false) }
    var ignoringBattery by remember { mutableStateOf(isIgnoringBatteryOptimizations(context)) }

    // Обновляем статус после возврата из системного диалога
    LifecycleResumeEffect(Unit) {
        ignoringBattery = isIgnoringBatteryOptimizations(context)
        onPauseOrDispose { }
    }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text("Настройки", style = MaterialTheme.typography.headlineMedium)

        Section(
            title = "Аккаунт lk.sut.ru",
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

        Section(
            title = "Маршрут трафика",
            hint = "Режимы «мимо VPN» не сработают, если в настройках VPN включена блокировка соединений без VPN. " +
                "Если выбранной сети нет, запрос не уходит в обход и повторяется на следующем цикле.",
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
            hint = "Как часто заглядывать в расписание. Чем меньше значение, тем быстрее отметка и выше расход батареи.",
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

        Section(
            title = "Работа в фоне",
            hint = "Android может усыплять приложения с выключенным экраном. Исключите AutoCheck из оптимизации батареи, чтобы проверки не прерывались.",
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

        Column {
            Button(
                onClick = vm::save,
                enabled = vm.isDirty,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) { Text(if (vm.isDirty) "Сохранить" else "Сохранено") }

            if (running && vm.isDirty) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Изменения применятся при следующем запуске.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
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
