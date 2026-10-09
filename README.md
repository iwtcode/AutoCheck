# AutoCheck (Android, Kotlin + Jetpack Compose)

Порт `auto_click.py` на Android: автоматически нажимает «Начать занятие» в личном кабинете lk.sut.ru.

## Сборка
1. Откройте папку проекта в Android Studio (Ladybug 2024.2+ / JDK 17).
2. Дождитесь Gradle Sync (Studio сама скачает Gradle 8.9).
3. Run ▶ на устройстве/эмуляторе (Android 8.0+), либо `Build > Build APK(s)`.

## Соответствие Python → Kotlin
| Python                              | Kotlin                                   |
|-------------------------------------|------------------------------------------|
| `Log`                               | `data/AutoClickState.kt` + экран «Журнал» |
| `options.txt` / CLI-аргументы       | `data/SettingsStore.kt` + экран «Настройки» |
| `AutoClickAPI.login()`              | `SutClient.login()`                      |
| парсинг расписания (BeautifulSoup)  | `SutClient.fetchSchedule()` (Jsoup)      |
| POST `?open=1&rasp=…&week=…`        | `SutClient.startLesson()`                |
| `while True` в `auto_click()`       | `data/AutoClickEngine.kt`                |
| запуск в консоли                    | `service/AutoClickService.kt` (foreground-сервис) |

## Уведомления
У приложения одно уведомление, оно обновляется на месте и не создаётся заново.
- Пока проверка работает, в уведомлении есть кнопка «Остановить»; после остановки — «Включить».
- Какие события попадают в текст уведомления, выбирается в разделе «Настройки → Уведомления»
  (по умолчанию показываются только начатые занятия и ошибки).
- Логика текста уведомления: `service/NotificationPresenter.kt`; сборка и обновление: `service/AutoClickService.kt`.

## Настройки
Раздел «Настройки» состоит из страниц: «Аккаунт», «Подключение» (маршрут трафика и интервал),
«Уведомления», «Работа в фоне». Маршрут трафика по умолчанию — «Как в системе».
