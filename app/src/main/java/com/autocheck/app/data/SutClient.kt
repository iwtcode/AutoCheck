package com.autocheck.app.data

import android.net.Network
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.Dns
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.jsoup.Jsoup
import java.io.IOException
import java.net.InetAddress
import java.net.Socket
import java.util.concurrent.TimeUnit
import javax.net.SocketFactory
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

enum class LoginResult { SUCCESS, BAD_CREDENTIALS }

sealed interface ScheduleResult {
    /** Сессия истекла — нужна повторная авторизация. */
    data object SessionExpired : ScheduleResult

    /** [lessonIds] — занятия с доступной кнопкой «Начать занятие». */
    data class Ok(val week: String, val lessonIds: List<String>) : ScheduleResult
}

/** HTTP-клиент личного кабинета lk.sut.ru. Порт методов `login()` и тела цикла `auto_click()`. */
class SutClient(
    private val route: TrafficRoute = TrafficRoute.SYSTEM,
    private val router: NetworkRouter? = null,
) {
    private val cookieJar = MemoryCookieJar()
    private var lastNetwork: Network? = null

    private val http = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .callTimeout(10, TimeUnit.SECONDS)
        .apply {
            if (route != TrafficRoute.SYSTEM && router != null) {
                // Сокеты и DNS-запросы привязываются к выбранной физической сети (в обход VPN)
                socketFactory(RoutedSocketFactory(::resolveNetwork))
                dns(object : Dns {
                    override fun lookup(hostname: String): List<InetAddress> =
                        resolveNetwork().getAllByName(hostname).toList()
                })
            }
        }
        .build()

    /** Выбранная сеть. Запасного маршрута нет: иначе трафик мог бы незаметно уйти в VPN. */
    private fun resolveNetwork(): Network {
        val r = router ?: throw IOException("маршрутизатор сети не инициализирован")
        val network = r.resolve(route)
            ?: throw IOException("выбранная сеть недоступна (${route.title})")
        if (network != lastNetwork) {
            lastNetwork = network
            AutoClickState.info("маршрут: ${r.label(network)}")
        }
        return network
    }

    /** @throws IOException при сетевых ошибках. */
    suspend fun login(login: String, password: String): LoginResult {
        cookieJar.clear() // как и в Python — каждая авторизация начинается с новой сессии

        http.newCall(Request.Builder().url("$CABINET?login=no").get().build()).text()

        val authUrl = "${CABINET}lib/autentificationok.php".toHttpUrl().newBuilder()
            .addQueryParameter("users", login)
            .addQueryParameter("parole", password)
            .build()
        val answer = http.newCall(
            Request.Builder().url(authUrl).post(EMPTY_BODY.toRequestBody()).build()
        ).text()

        if (answer.trim() != "1") return LoginResult.BAD_CREDENTIALS

        http.newCall(Request.Builder().url("$CABINET?login=yes").get().build()).text()
        return LoginResult.SUCCESS
    }

    /** @throws IOException, IllegalStateException */
    suspend fun fetchSchedule(): ScheduleResult {
        val text = http.newCall(Request.Builder().url(SCHEDULE_URL).get().build()).text()
        if (text.contains(ERR_MSG)) return ScheduleResult.SessionExpired

        val doc = Jsoup.parse(text)
        val week = doc.selectFirst("h3")?.text()
            ?.substringAfter("№", missingDelimiterValue = "")
            ?.trim()
            ?.split(Regex("\\s+"))
            ?.firstOrNull()
            ?.takeIf { it.isNotEmpty() }
            ?: error("не удалось определить номер недели")

        val ids = doc.select("span[id^=knop]")
            .filter { it.text() == START_LABEL }
            .map { it.id().removePrefix("knop") }

        return ScheduleResult.Ok(week, ids)
    }

    /** @return true, если сервер ответил непустым телом (занятие начато). */
    suspend fun startLesson(lessonId: String, week: String): Boolean {
        val url: HttpUrl = SCHEDULE_URL.toHttpUrl().newBuilder()
            .addQueryParameter("open", "1")
            .addQueryParameter("rasp", lessonId)
            .addQueryParameter("week", week)
            .build()
        val body = http.newCall(
            Request.Builder().url(url).post(EMPTY_BODY.toRequestBody()).build()
        ).text()
        return body.isNotEmpty()
    }

    private suspend fun Call.text(): String = withContext(Dispatchers.IO) {
        await().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            response.body?.string().orEmpty()
        }
    }

    private suspend fun Call.await(): Response = suspendCancellableCoroutine { cont ->
        cont.invokeOnCancellation { cancel() }
        enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (cont.isActive) cont.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                cont.resume(response)
            }
        })
    }

    /** Каждый новый сокет создаётся фабрикой текущей выбранной сети. */
    private class RoutedSocketFactory(private val network: () -> Network) : SocketFactory() {
        private fun factory(): SocketFactory = network().socketFactory

        override fun createSocket(): Socket = factory().createSocket()
        override fun createSocket(host: String, port: Int): Socket = factory().createSocket(host, port)
        override fun createSocket(host: String, port: Int, localHost: InetAddress, localPort: Int): Socket =
            factory().createSocket(host, port, localHost, localPort)

        override fun createSocket(host: InetAddress, port: Int): Socket = factory().createSocket(host, port)
        override fun createSocket(address: InetAddress, port: Int, localAddress: InetAddress, localPort: Int): Socket =
            factory().createSocket(address, port, localAddress, localPort)
    }

    private class MemoryCookieJar : CookieJar {
        private val store = mutableListOf<Cookie>()

        @Synchronized
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            cookies.forEach { new ->
                store.removeAll { it.name == new.name && it.domain == new.domain && it.path == new.path }
                store.add(new)
            }
        }

        @Synchronized
        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            val now = System.currentTimeMillis()
            store.removeAll { it.expiresAt < now }
            return store.filter { it.matches(url) }
        }

        @Synchronized
        fun clear() = store.clear()
    }

    private companion object {
        const val CABINET = "https://lk.sut.ru/cabinet/"
        const val SCHEDULE_URL = "https://lk.sut.ru/cabinet/project/cabinet/forms/raspisanie.php"
        const val ERR_MSG = "У Вас нет прав доступа. Или необходимо перезагрузить приложение.."
        const val START_LABEL = "Начать занятие"
        val EMPTY_BODY = ByteArray(0)
    }
}
