package com.rupleide.netfix.core.dpibypass

import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket

object ProxyPortResolver {
    const val DEFAULT_PROXY_PORT = 1080
    private const val MIN_PORT = 1
    private const val MAX_PORT = 65535

    fun resolve(ip: String, preferredPort: Int): Int {
        if (preferredPort in MIN_PORT..MAX_PORT && isPortFree(ip, preferredPort)) return preferredPort
        return try {
            findFreePort(ip)
        } catch (_: Exception) {
            preferredPort
        }
    }

    fun isPortFree(ip: String, port: Int): Boolean =
        try {
            ServerSocket().use { socket ->
                socket.reuseAddress = true
                socket.bind(InetSocketAddress(InetAddress.getByName(ip), port))
                true
            }
        } catch (_: Exception) {
            false
        }

    fun findFreePort(ip: String): Int =
        ServerSocket().use { socket ->
            socket.reuseAddress = true
            socket.bind(InetSocketAddress(InetAddress.getByName(ip), 0))
            socket.localPort
        }

    fun withPort(args: Array<String>, port: Int): Array<String> {
        val result = args.toMutableList()
        var index = 0
        while (index < result.size) {
            val token = result[index]
            when {
                (token == "-p" || token == "--port") && index + 1 < result.size -> {
                    result[index + 1] = port.toString()
                    return result.toTypedArray()
                }
                token.startsWith("--port=") -> {
                    result[index] = "--port=$port"
                    return result.toTypedArray()
                }
                token.length > 2 && token.startsWith("-p") && token.substring(2).all { it.isDigit() } -> {
                    result[index] = "-p$port"
                    return result.toTypedArray()
                }
            }
            index++
        }
        result.add("--port")
        result.add(port.toString())
        return result.toTypedArray()
    }
}
