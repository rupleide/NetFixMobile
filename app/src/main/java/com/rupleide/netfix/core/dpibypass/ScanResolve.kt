package com.rupleide.netfix.core.dpibypass

import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.Proxy
import java.net.Socket
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSocketFactory

object ScanResolve {
    private const val TIMEOUT_MS = 4_000
    private const val CACHE_MS = 5 * 60 * 1000L
    private val dohEndpoints = listOf("https://1.1.1.1/dns-query", "https://1.0.0.1/dns-query", "https://8.8.8.8/resolve")
    private val ipv4Answer = Regex("\"data\":\"(\\d+\\.\\d+\\.\\d+\\.\\d+)\"")
    private val cache = ConcurrentHashMap<String, Pair<String, Long>>()

    fun isHostUnreachable(error: Throwable?): Boolean =
        generateSequence(error) { it.cause }.any { it.message?.contains("Host unreachable", ignoreCase = true) == true }

    fun isHostUnreachable(failure: String?): Boolean = failure?.contains("Host unreachable", ignoreCase = true) == true

    fun resolve(host: String): String? {
        cache[host]?.let { (ip, at) -> if (System.currentTimeMillis() - at < CACHE_MS) return ip }
        val ip = dohEndpoints.firstNotNullOfOrNull { endpoint -> dohLookup(endpoint, host) } ?: return null
        cache[host] = ip to System.currentTimeMillis()
        return ip
    }

    private fun dohLookup(endpoint: String, host: String): String? {
        var connection: HttpURLConnection? = null
        return try {
            connection = URL("$endpoint?name=$host&type=A").openConnection(Proxy.NO_PROXY) as HttpURLConnection
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.setRequestProperty("Accept", "application/dns-json")
            if (connection.responseCode !in 200..299) return null
            val body = connection.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
            ipv4Answer.findAll(body).map { it.groupValues[1] }.firstOrNull { it != "0.0.0.0" && !it.startsWith("127.") }
        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    fun open(url: String, proxy: Proxy, resolved: Boolean): HttpURLConnection {
        val original = URL(url)
        if (!resolved || original.protocol != "https") return original.openConnection(proxy) as HttpURLConnection
        val host = original.host
        val ip = resolve(host) ?: throw java.net.UnknownHostException("$host: нет адреса и через DoH")
        val byAddress = URL(original.protocol, ip, original.port, original.file)
        val connection = byAddress.openConnection(proxy) as HttpsURLConnection
        connection.sslSocketFactory = SniSocketFactory(host)
        connection.hostnameVerifier = HostnameVerifier { _, session -> HttpsURLConnection.getDefaultHostnameVerifier().verify(host, session) }
        connection.setRequestProperty("Host", host)
        connection.instanceFollowRedirects = false
        return connection
    }

    private class SniSocketFactory(private val host: String) : SSLSocketFactory() {
        private val delegate = SSLSocketFactory.getDefault() as SSLSocketFactory
        override fun getDefaultCipherSuites(): Array<String> = delegate.defaultCipherSuites
        override fun getSupportedCipherSuites(): Array<String> = delegate.supportedCipherSuites
        override fun createSocket(s: Socket?, ignoredHost: String?, port: Int, autoClose: Boolean): Socket = withSni(delegate.createSocket(s, host, port, autoClose))

        private fun withSni(socket: Socket): Socket {
            if (socket is javax.net.ssl.SSLSocket) {
                val params = socket.sslParameters
                params.serverNames = listOf(javax.net.ssl.SNIHostName(host))
                socket.sslParameters = params
            }
            return socket
        }
        override fun createSocket(ignoredHost: String?, port: Int): Socket = delegate.createSocket(host, port)
        override fun createSocket(ignoredHost: String?, port: Int, localHost: InetAddress?, localPort: Int): Socket = delegate.createSocket(host, port, localHost, localPort)
        override fun createSocket(address: InetAddress?, port: Int): Socket = delegate.createSocket(address, port)
        override fun createSocket(address: InetAddress?, port: Int, localAddress: InetAddress?, localPort: Int): Socket = delegate.createSocket(address, port, localAddress, localPort)
    }
}
