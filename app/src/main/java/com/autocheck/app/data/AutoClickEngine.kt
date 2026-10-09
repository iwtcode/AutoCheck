package com.autocheck.app.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import java.net.SocketException

/**
 * Главный цикл — порт `AutoClickAPI.auto_click()`.
 * Возвращается только при фатальной ошибке (неверный пароль); остановка — отменой корутины.
 */
class AutoClickEngine(private val client: SutClient = SutClient()) {
    private val state = AutoClickState

    suspend fun run(credentials: Credentials) {
        val pauseMs = credentials.timeoutSec * 1000L

        if (!authorize(credentials, pauseMs)) return

        while (true) {
            try {
                when (val schedule = client.fetchSchedule()) {
                    ScheduleResult.SessionExpired -> {
                        state.warning("Срок сессии истёк, выполняется повторный вход")
                        if (!authorize(credentials, pauseMs)) return
                        continue
                    }

                    is ScheduleResult.Ok -> {
                        if (schedule.lessonIds.isEmpty()) {
                            state.info("Нет активных занятий", LogCategory.CHECK)
                        } else {
                            for (id in schedule.lessonIds) {
                                if (client.startLesson(id, schedule.week)) {
                                    state.success("Занятие начато (id: $id)", LogCategory.LESSON)
                                    state.onLessonStarted()
                                } else {
                                    state.info("Занятие (id: $id) ещё не началось", LogCategory.CHECK)
                                }
                            }
                        }
                        state.onCycleFinished()
                    }
                }
                state.setStatus(EngineStatus.RUNNING)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                state.error("Ошибка подключения: ${describe(e)}")
            }
            state.info("Следующая проверка через ${credentials.timeoutSec} с", LogCategory.PAUSE)
            delay(pauseMs)
        }
    }

    /** @return true — вошли; false — неверные данные, дальнейшая работа бессмысленна. */
    private suspend fun authorize(credentials: Credentials, pauseMs: Long): Boolean {
        while (true) {
            state.setStatus(EngineStatus.CONNECTING)
            try {
                when (client.login(credentials.login, credentials.password)) {
                    LoginResult.SUCCESS -> {
                        state.success("Вход выполнен")
                        state.setStatus(EngineStatus.RUNNING)
                        return true
                    }

                    LoginResult.BAD_CREDENTIALS -> {
                        state.error("Ошибка входа: проверьте логин и пароль")
                        state.setStatus(EngineStatus.ERROR)
                        return false
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                state.error("Ошибка подключения: ${describe(e)}")
                state.info("Повторная попытка входа через ${credentials.timeoutSec} с", LogCategory.PAUSE)
                delay(pauseMs)
            }
        }
    }

    private fun describe(e: Exception): String {
        val message = e.message.orEmpty()
        return if (e is SocketException && message.contains("EPERM")) {
            "VPN не разрешает приложению использовать выбранную сеть (EPERM). " +
                "Добавьте AutoCheck в исключения VPN или выберите маршрут «Как в системе»"
        } else {
            val type = e.javaClass.simpleName
            if (message.isBlank()) type else "$type: $message"
        }
    }
}
