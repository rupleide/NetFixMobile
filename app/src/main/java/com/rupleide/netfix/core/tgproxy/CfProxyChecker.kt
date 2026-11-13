package com.rupleide.netfix.core.tgproxy

import android.content.Context
import com.rupleide.netfix.core.debug.AppDebugManager as Log
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

enum class CfServerState { Works, Busy, Fails }

data class CfServerResult(val domain: String, val state: CfServerState, val latencyMs: Long, val detail: String)

object CfProxyChecker {
    private const val TAG = "CfCheck"
    private const val CACHE_FILE_NAME = "cfproxy-domains-cache.txt"
    private const val PROBE_DC = 2
    private const val CONNECT_TIMEOUT_MS = 5_000
    private const val PARALLEL = 4
    private const val CHECK_DEADLINE_SECONDS = 45L
    private const val CF_SUFFIX = ".co.uk"

    private val encodedDefaults = listOf(
        "virkgj.com", "vmmzovy.com", "mkuosckvso.com", "zaewayzmplad.com", "twdmbzcm.com",
        "awzwsldi.com", "clngqrflngqin.com", "tjacxbqtj.com", "bxaxtxmrw.com", "dmohrsgmohcrwb.com",
        "vwbmtmoi.com", "khgrre.com", "ulihssf.com", "tmhqsdqmfpmk.com", "xwuwoqbm.com"
    )

    fun decodeDomain(encoded: String): String {
        if (!encoded.endsWith(".com")) return encoded
        val body = encoded.dropLast(4)
        val shift = body.count { it in 'a'..'z' || it in 'A'..'Z' } % 26
        val decoded = body.map { c ->
            when (c) {
                in 'a'..'z' -> 'a' + ((c - 'a' - shift + 26) % 26)
                in 'A'..'Z' -> 'A' + ((c - 'A' - shift + 26) % 26)
                else -> c
            }
        }.joinToString("")
        return decoded + CF_SUFFIX
    }

    fun normalizeDomain(raw: String): String? {
        val decoded = decodeDomain(raw.trim()).trim().lowercase().trimEnd('.')
        return decoded.takeIf { it.isNotEmpty() && it.endsWith(CF_SUFFIX) }
    }

    fun defaultDomains(): List<String> = encodedDefaults.mapNotNull { normalizeDomain(it) }

    fun knownDomains(context: Context): List<String> {
        val cached = try {
            File(context.cacheDir, CACHE_FILE_NAME).takeIf { it.isFile }?.readLines().orEmpty()
        } catch (_: Exception) {
            emptyList()
        }
        val pinned = listOfNotNull(TgProxyController.pinnedCfDomain(context))
        return (pinned + cached + defaultDomains()).mapNotNull { normalizeDomain(it) }.distinct()
    }

    fun checkAll(context: Context, onResult: (CfServerResult) -> Unit): List<CfServerResult> {
        val domains = knownDomains(context)
        val pool = Executors.newFixedThreadPool(PARALLEL)
        return try {
            val tasks = domains.map { domain -> Callable { probe(domain).also(onResult) } }
            pool.invokeAll(tasks, CHECK_DEADLINE_SECONDS, TimeUnit.SECONDS).mapNotNull { future ->
                try {
                    if (future.isCancelled) null else future.get()
                } catch (_: Exception) {
                    null
                }
            }.sortedWith(compareBy<CfServerResult> { it.state.ordinal }.thenBy { it.latencyMs })
                .also { results ->
                    Log.i(TAG, "Проверка серверов Cloudflare: работают ${results.count { it.state == CfServerState.Works }} из ${results.size}")
                }
        } finally {
            pool.shutdownNow()
        }
    }

    fun probe(baseDomain: String): CfServerResult {
        val host = "kws$PROBE_DC.$baseDomain"
        val started = System.currentTimeMillis()
        val address = resolve(host) ?: return CfServerResult(baseDomain, CfServerState.Fails, -1, "адрес не найден")
        return try {
            Socket().use { raw ->
                raw.connect(InetSocketAddress(address, 443), CONNECT_TIMEOUT_MS)
                raw.soTimeout = CONNECT_TIMEOUT_MS
                val factory = SSLSocketFactory.getDefault() as SSLSocketFactory
                (factory.createSocket(raw, host, 443, true) as SSLSocket).use { tls ->
                    tls.startHandshake()
                    if (!HttpsURLConnection.getDefaultHostnameVerifier().verify(host, tls.session)) {
                        return CfServerResult(baseDomain, CfServerState.Fails, -1, "чужой сертификат")
                    }
                    val key = ByteArray(16).also { SecureRandom().nextBytes(it) }
                    val request = "GET /apiws HTTP/1.1\r\n" +
                        "Host: $host\r\n" +
                        "Upgrade: websocket\r\n" +
                        "Connection: Upgrade\r\n" +
                        "Sec-WebSocket-Key: ${Base64.getEncoder().encodeToString(key)}\r\n" +
                        "Sec-WebSocket-Version: 13\r\n" +
                        "Sec-WebSocket-Protocol: binary\r\n\r\n"
                    tls.outputStream.write(request.toByteArray(Charsets.US_ASCII))
                    tls.outputStream.flush()
                    val statusLine = BufferedReader(InputStreamReader(tls.inputStream, Charsets.US_ASCII)).readLine().orEmpty()
                    val code = statusLine.split(' ').getOrNull(1)?.toIntOrNull() ?: 0
                    val elapsed = System.currentTimeMillis() - started
                    when (code) {
                        101 -> CfServerResult(baseDomain, CfServerState.Works, elapsed, "работает")
                        429 -> CfServerResult(baseDomain, CfServerState.Busy, elapsed, "перегружен")
                        0 -> CfServerResult(baseDomain, CfServerState.Fails, -1, "не ответил")
                        else -> CfServerResult(baseDomain, CfServerState.Fails, -1, "ответ $code")
                    }
                }
            }
        } catch (_: java.net.SocketTimeoutException) {
            CfServerResult(baseDomain, CfServerState.Fails, -1, "не отвечает")
        } catch (_: Exception) {
            CfServerResult(baseDomain, CfServerState.Fails, -1, "соединение сброшено")
        }
    }

    private fun resolve(host: String): InetAddress? {
        try {
            InetAddress.getAllByName(host).firstOrNull { it is Inet4Address }?.let { return it }
        } catch (_: Exception) {
        }
        return resolveOverHttps(host)
    }

    private fun resolveOverHttps(host: String): InetAddress? {
        var connection: HttpURLConnection? = null
        return try {
            connection = URL("https://1.1.1.1/dns-query?name=$host&type=A").openConnection() as HttpURLConnection
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = CONNECT_TIMEOUT_MS
            connection.setRequestProperty("Accept", "application/dns-json")
            val body = connection.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
            Regex("\"data\":\"(\\d+\\.\\d+\\.\\d+\\.\\d+)\"").find(body)?.groupValues?.get(1)?.let { InetAddress.getByName(it) }
        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }
}
