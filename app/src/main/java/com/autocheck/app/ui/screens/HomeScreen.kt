package com.autocheck.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.autocheck.app.data.EngineStatus
import com.autocheck.app.data.LogEntry
import com.autocheck.app.data.LogLevel
import com.autocheck.app.data.Stats
import com.autocheck.app.ui.theme.MonoStyle
import com.autocheck.app.ui.theme.color

private data class HeroPalette(val from: Color, val to: Color, val title: String)

private fun EngineStatus.palette() = when (this) {
    EngineStatus.STOPPED -> HeroPalette(Color(0xFF3B4061), Color(0xFF22263D), "Остановлено")
    EngineStatus.CONNECTING -> HeroPalette(Color(0xFFD9930D), Color(0xFFB4530A), "Подключение…")
    EngineStatus.RUNNING -> HeroPalette(Color(0xFF4F5BD5), Color(0xFF0FA894), "Работает")
    EngineStatus.ERROR -> HeroPalette(Color(0xFFD02A45), Color(0xFF7D1630), "Ошибка")
}

@Composable
fun HomeScreen(
    status: EngineStatus,
    stats: Stats,
    recentLogs: List<LogEntry>,
    timeoutSec: Int,
    hasCredentials: Boolean,
    onToggle: () -> Unit,
    onOpenLog: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text("AutoCheck", style = MaterialTheme.typography.headlineMedium)

        Hero(
            status = status,
            subtitle = when (status) {
                EngineStatus.STOPPED -> "Нажмите, чтобы начать отмечаться"
                EngineStatus.CONNECTING -> "Входим в личный кабинет"
                EngineStatus.RUNNING -> "Проверка расписания каждые $timeoutSec с"
                EngineStatus.ERROR -> "Загляните в журнал"
            },
            enabled = hasCredentials || status != EngineStatus.STOPPED,
            onToggle = onToggle,
        )

        if (!hasCredentials) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(onClick = onOpenSettings),
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(Icons.Rounded.Error, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(
                        "Укажите логин и пароль от lk.sut.ru в настройках",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        // Статистика: одна плашка, три колонки — без «россыпи» карточек
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Row(Modifier.padding(vertical = 18.dp)) {
                Stat("Проверок", stats.cycles.toString(), Modifier.weight(1f))
                Box(
                    Modifier
                        .width(1.dp)
                        .height(36.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                        .align(Alignment.CenterVertically)
                )
                Stat("Начато занятий", stats.started.toString(), Modifier.weight(1f))
                Box(
                    Modifier
                        .width(1.dp)
                        .height(36.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                        .align(Alignment.CenterVertically)
                )
                Stat("Последняя", stats.lastCheck ?: "—", Modifier.weight(1f))
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Последние события", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onOpenLog) { Text("Весь журнал") }
            }
            if (recentLogs.isEmpty()) {
                Text(
                    "Здесь появятся события после запуска",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                recentLogs.forEach { LogRow(it) }
            }
        }
    }
}

@Composable
private fun Hero(
    status: EngineStatus,
    subtitle: String,
    enabled: Boolean,
    onToggle: () -> Unit,
) {
    val target = status.palette()
    val from by animateColorAsState(target.from, tween(700), label = "from")
    val to by animateColorAsState(target.to, tween(700), label = "to")
    val active = status == EngineStatus.RUNNING || status == EngineStatus.CONNECTING

    val pulse = rememberInfiniteTransition(label = "pulse")
    val scale by pulse.animateFloat(
        initialValue = 1f, targetValue = 1.75f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearOutSlowInEasing), RepeatMode.Restart),
        label = "scale",
    )
    val ringAlpha by pulse.animateFloat(
        initialValue = 0.4f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Restart),
        label = "alpha",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(36.dp))
            .background(Brush.linearGradient(listOf(from, to)))
            .padding(vertical = 36.dp, horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(200.dp), contentAlignment = Alignment.Center) {
                if (active) {
                    Box(
                        Modifier
                            .size(112.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                alpha = ringAlpha
                            }
                            .background(Color.White, CircleShape)
                    )
                }
                Box(
                    Modifier
                        .size(112.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = if (enabled) 1f else 0.4f))
                        .clickable(enabled = enabled, role = Role.Button, onClick = onToggle),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (active) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                        contentDescription = if (active) "Остановить" else "Запустить",
                        tint = to,
                        modifier = Modifier.size(56.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                target.title,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
            )
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, maxLines = 1)
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun LogRow(entry: LogEntry, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .padding(top = 6.dp)
                .size(8.dp)
                .background(entry.level.color(), CircleShape)
        )
        Column {
            Text(entry.time, style = MonoStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                entry.message,
                style = MaterialTheme.typography.bodyMedium,
                color = if (entry.level == LogLevel.INFO) MaterialTheme.colorScheme.onSurface else entry.level.color(),
                fontWeight = if (entry.level == LogLevel.SUCCESS) FontWeight.Medium else null,
            )
        }
    }
}
