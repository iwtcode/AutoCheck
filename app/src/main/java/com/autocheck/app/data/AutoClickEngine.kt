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
                        state.warning("срок сессии истёк")
                        if (!authorize(credentials, pauseMs)) return
                        continue
                    }

                    is ScheduleResult.Ok -> {
                        if (schedule.lessonIds.isEmpty()) {
                            state.info("нет активных занятий")
                        } else {
                            for (id in schedule.lessonIds) {
                                if (client.startLesson(id, schedule.week)) {
                                    state.success("удалось начать занятие с id: $id")
                                    state.onLessonStarted()
                                } else {
                                    state.info("занятие с id: $id ещё не началось")
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
                state.error("ошибка при подключении | ${describe(e)}")
            }
            state.info("timeout ${credentials.timeoutSec} секунд перед следующим циклом")
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
                        state.success("успешная авторизация")
                        state.setStatus(EngineStatus.RUNNING)
                        return true
                    }

                    LoginResult.BAD_CREDENTIALS -> {
                        state.error("ошибка при авторизации: проверьте логин и пароль")
                        state.setStatus(EngineStatus.ERROR)
                        return false
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                state.error("ошибка при подключении | ${describe(e)}")
                state.info("timeout ${credentials.timeoutSec} перед следующей попыткой авторизации")
                delay(pauseMs)
            }
        }
    }

    private fun describe(e: Exception): String {
        val message = e.message.orEmpty()
        return if (e is SocketException && message.contains("EPERM")) {
            "VPN запрещает обход для этого приложения (EPERM). " +
                "Добавьте AutoCheck в исключения VPN или выберите маршрут «Как в системе»"
        } else {
            "${e.javaClass.simpleName} $message"
        }
    }
}
