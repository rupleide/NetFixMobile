package com.rupleide.netfix.core.help

import java.io.ByteArrayOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.Socket
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import kotlin.random.Random

enum class HostVerdict { Ok, Blocked, RegionBlocked }

object HelpHostCheck {
    private const val TIMEOUT_MS = 5_000
    private const val DNS_PORT = 53
    private const val READ_LIMIT = 4096
    private const val MARKER_BYTES = 2048
    private const val MAX_BODY_BYTES = 128L * 1024L
    private val regionBlockMarkers = listOf(
        "unsupported_country",
        "not available in your country",
        "unavailable in your region",
        "isn't currently supported in your country",
        "available-regions"
    )
    private val blockedTraceLocation = Regex("""(?m)^loc=(RU|BY|IR|KP|SY|CU|CN)\s*$""")

    fun encodeQuery(host: String, id: Int): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(id shr 8 and 0xFF)
        out.write(id and 0xFF)
        out.write(0x01)
        out.write(0x00)
        out.write(0x00)
        out.write(0x01)
        repeat(6) { out.write(0x00) }
        host.trimEnd('.').split('.').forEach { label ->
            val bytes = label.toByteArray(Charsets.US_ASCII)
            out.write(bytes.size)
            out.write(bytes)
        }
        out.write(0x00)
        out.write(0x00)
        out.write(0x01)
        out.write(0x00)
        out.write(0x01)
        return out.toByteArray()
    }

    private fun skipName(data: ByteArray, start: Int): Int {
        var position = start
        while (position < data.size) {
            val length = data[position].toInt() and 0xFF
            if (length == 0) return position + 1
            if (length and 0xC0 == 0xC0) return position + 2
            position += length + 1
        }
        return position
    }

    fun parseFirstIpv4(data: ByteArray, length: Int, expectedId: Int): String? {
        if (length < 12) return null
        val id = (data[0].toInt() and 0xFF shl 8) or (data[1].toInt() and 0xFF)
        if (id != expectedId) return null
        val questions = (data[4].toInt() and 0xFF shl 8) or (data[5].toInt() and 0xFF)
        val answers = (data[6].toInt() and 0xFF shl 8) or (data[7].toInt() and 0xFF)
        var position = 12
        repeat(questions) { position = skipName(data, position) + 4 }
        repeat(answers) {
            position = skipName(data, position)
            if (position + 10 > length) return null
            val type = (data[position].toInt() and 0xFF shl 8) or (data[position + 1].toInt() and 0xFF)
            val dataLength = (data[position + 8].toInt() and 0xFF shl 8) or (data[position + 9].toInt() and 0xFF)
            position += 10
            if (type == 1 && dataLength == 4 && position + 4 <= length) {
                return (0 until 4).joinToString(".") { (data[position + it].toInt() and 0xFF).toString() }
            }
            position += dataLength
        }
        return null
    }

    fun resolve(host: String, dnsServer: String?): String? = try {
        if (dnsServer == null) {
            InetAddress.getAllByName(host).firstOrNull { it.address.size == 4 }?.hostAddress
                ?: InetAddress.getByName(host).hostAddress
        } else {
            val id = Random.nextInt(0, 0xFFFF)
            val query = encodeQuery(host, id)
            DatagramSocket().use { socket ->
                socket.soTimeout = TIMEOUT_MS
                socket.send(DatagramPacket(query, query.size, InetAddress.getByName(dnsServer), DNS_PORT))
                val buffer = ByteArray(1500)
                val packet = DatagramPacket(buffer, buffer.size)
                socket.receive(packet)
                parseFirstIpv4(buffer, packet.length, id)
            }
        }
    } catch (_: Exception) {
        null
    }

    fun resolveSecure(host: String, servers: List<String>): String? = try {
        val id = Random.nextInt(0, 0xFFFF)
        val query = encodeQuery(host, id)
        com.rupleide.netfix.core.dpibypass.SecureDnsRelay.resolveVia(servers, query)?.let { answer ->
            val withOurId = answer.copyOf().also {
                it[0] = (id shr 8 and 0xFF).toByte()
                it[1] = (id and 0xFF).toByte()
            }
            parseFirstIpv4(withOurId, withOurId.size, id)
        }
    } catch (_: Exception) {
        null
    }

    fun resolveThroughNetFix(host: String, dnsServer: String?): String? =
        if (com.rupleide.netfix.core.dpibypass.SecureDnsRelay.isRunning) resolveSecure(host, listOfNotNull(dnsServer)) else resolve(host, dnsServer)

    private const val DOT_PORT = 853

    fun resolveViaDot(host: String, dotHost: String): String? {
        val factory = SSLSocketFactory.getDefault() as SSLSocketFactory
        val plain = Socket()
        return try {
            plain.connect(InetSocketAddress(dotHost, DOT_PORT), TIMEOUT_MS)
            plain.soTimeout = TIMEOUT_MS
            (factory.createSocket(plain, dotHost, DOT_PORT, true) as SSLSocket).use { socket ->
                socket.startHandshake()
                if (!HttpsURLConnection.getDefaultHostnameVerifier().verify(dotHost, socket.session)) return null
                val id = Random.nextInt(0, 0xFFFF)
                val query = encodeQuery(host, id)
                val output = socket.getOutputStream()
                output.write(byteArrayOf((query.size shr 8).toByte(), (query.size and 0xFF).toByte()) + query)
                output.flush()
                val input = java.io.DataInputStream(socket.getInputStream())
                val length = input.readUnsignedShort()
                val answer = ByteArray(length)
                input.readFully(answer)
                parseFirstIpv4(answer, length, id)
            }
        } catch (_: Exception) {
            null
        } finally {
            try {
                plain.close()
            } catch (_: Exception) {
            }
        }
    }

    fun answersWithForeignCertificate(host: String, address: String): Boolean? {
        val factory = SSLSocketFactory.getDefault() as SSLSocketFactory
        val plain = Socket()
        return try {
            plain.connect(InetSocketAddress(address, 443), TIMEOUT_MS)
            plain.soTimeout = TIMEOUT_MS
            (factory.createSocket(plain, host, 443, true) as SSLSocket).use { socket ->
                socket.startHandshake()
                !HttpsURLConnection.getDefaultHostnameVerifier().verify(host, socket.session)
            }
        } catch (_: Exception) {
            null
        } finally {
            try {
                plain.close()
            } catch (_: Exception) {
            }
        }
    }

    fun socksUdpPacket(dnsServer: String, payload: ByteArray): ByteArray {
        val ip = InetAddress.getByName(dnsServer).address
        return byteArrayOf(0, 0, 0, 1) + ip + byteArrayOf((DNS_PORT shr 8).toByte(), (DNS_PORT and 0xFF).toByte()) + payload
    }

    fun socksUdpPayloadOffset(data: ByteArray, length: Int): Int? {
        if (length < 4 || data[2].toInt() != 0) return null
        val offset = when (data[3].toInt()) {
            1 -> 10
            4 -> 22
            3 -> 7 + (data[4].toInt() and 0xFF)
            else -> return null
        }
        return offset.takeIf { it <= length }
    }

    fun resolveViaSocks(host: String, dnsServer: String, socksPort: Int): String? {
        val control = Socket()
        return try {
            control.soTimeout = TIMEOUT_MS
            control.connect(InetSocketAddress("127.0.0.1", socksPort), TIMEOUT_MS)
            val output = control.getOutputStream()
            val input = control.getInputStream()
            output.write(byteArrayOf(5, 1, 0))
            output.flush()
            val greeting = ByteArray(2)
            if (input.read(greeting) != 2 || greeting[1].toInt() != 0) return null
            output.write(byteArrayOf(5, 3, 0, 1, 0, 0, 0, 0, 0, 0))
            output.flush()
            val reply = ByteArray(64)
            val replyLength = input.read(reply)
            if (replyLength < 10 || reply[1].toInt() != 0) return null
            val addressLength = when (reply[3].toInt()) {
                1 -> 4
                4 -> 16
                else -> return null
            }
            if (replyLength < 4 + addressLength + 2) return null
            val relayIp = InetAddress.getByAddress(reply.copyOfRange(4, 4 + addressLength)).let {
                if (it.isAnyLocalAddress) InetAddress.getByName("127.0.0.1") else it
            }
            val portAt = 4 + addressLength
            val relayPort = ((reply[portAt].toInt() and 0xFF) shl 8) or (reply[portAt + 1].toInt() and 0xFF)
            val id = Random.nextInt(0, 0xFFFF)
            val packet = socksUdpPacket(dnsServer, encodeQuery(host, id))
            DatagramSocket().use { socket ->
                socket.soTimeout = TIMEOUT_MS
                socket.send(DatagramPacket(packet, packet.size, relayIp, relayPort))
                val buffer = ByteArray(1600)
                val received = DatagramPacket(buffer, buffer.size)
                socket.receive(received)
                val offset = socksUdpPayloadOffset(buffer, received.length) ?: return null
                val answer = buffer.copyOfRange(offset, received.length)
                parseFirstIpv4(answer, answer.size, id)
            }
        } catch (_: Exception) {
            null
        } finally {
            try {
                control.close()
            } catch (_: Exception) {
            }
        }
    }

    private val contentLengthHeader = Regex("""(?im)^content-length:\s*(\d+)\s*$""")

    fun headersEnd(data: ByteArray, length: Int): Int {
        for (i in 0..length - 4) {
            if (data[i] == '\r'.code.toByte() && data[i + 1] == '\n'.code.toByte() && data[i + 2] == '\r'.code.toByte() && data[i + 3] == '\n'.code.toByte()) return i + 4
        }
        return -1
    }

    fun declaredBodyLength(headers: String): Long? = contentLengthHeader.find(headers)?.groupValues?.get(1)?.toLongOrNull()

    private fun bodyArrives(socket: SSLSocket, buffer: ByteArray, received: Int, canReadMore: Boolean): Boolean {
        val end = headersEnd(buffer, received)
        if (end < 0) return canReadMore
        val declared = declaredBodyLength(String(buffer, 0, end, Charsets.ISO_8859_1)) ?: return true
        val needed = minOf(declared, MAX_BODY_BYTES)
        var body = (received - end).toLong()
        if (!canReadMore) return body >= needed
        val chunk = ByteArray(8192)
        while (body < needed) {
            val read = try {
                socket.inputStream.read(chunk, 0, minOf(chunk.size.toLong(), needed - body).toInt())
            } catch (_: java.net.SocketTimeoutException) {
                return false
            }
            if (read <= 0) return false
            body += read
        }
        return true
    }

    fun check(target: String, dnsServer: String?, socksPort: Int?): HostVerdict {
        val host = target.substringBefore('/')
        val address = (if (socksPort != null) resolveThroughNetFix(host, dnsServer) else resolve(host, dnsServer)) ?: return HostVerdict.Blocked
        return checkResolved(target, address, socksPort)
    }

    fun checkResolved(target: String, address: String, socksPort: Int?): HostVerdict {
        val host = target.substringBefore('/')
        val path = "/" + target.substringAfter('/', "")
        val raw = if (socksPort != null) Socket(Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", socksPort))) else Socket()
        return try {
            raw.soTimeout = TIMEOUT_MS
            raw.connect(InetSocketAddress(InetAddress.getByName(address), 443), TIMEOUT_MS)
            val tls = (SSLSocketFactory.getDefault() as SSLSocketFactory).createSocket(raw, host, 443, true) as SSLSocket
            tls.use { socket ->
                socket.soTimeout = TIMEOUT_MS
                socket.startHandshake()
                if (!HttpsURLConnection.getDefaultHostnameVerifier().verify(host, socket.session)) return HostVerdict.Blocked
                socket.outputStream.write("GET $path HTTP/1.1\r\nHost: $host\r\nUser-Agent: Mozilla/5.0\r\nAccept: */*\r\nConnection: close\r\n\r\n".toByteArray())
                socket.outputStream.flush()
                val buffer = ByteArray(READ_LIMIT)
                var total = 0
                var timedOut = false
                while (total < READ_LIMIT) {
                    val read = try {
                        socket.inputStream.read(buffer, total, READ_LIMIT - total)
                    } catch (_: java.net.SocketTimeoutException) {
                        timedOut = true
                        -1
                    }
                    if (read <= 0) break
                    total += read
                    if (headersEnd(buffer, total) >= 0 && total >= MARKER_BYTES) break
                }
                val text = String(buffer, 0, total, Charsets.ISO_8859_1)
                when {
                    !text.startsWith("HTTP/") -> HostVerdict.Blocked
                    regionBlockMarkers.any { text.contains(it, ignoreCase = true) } || blockedTraceLocation.containsMatchIn(text) -> HostVerdict.RegionBlocked
                    !bodyArrives(socket, buffer, total, canReadMore = !timedOut) -> HostVerdict.Blocked
                    else -> HostVerdict.Ok
                }
            }
        } catch (_: Exception) {
            HostVerdict.Blocked
        } finally {
            try {
                raw.close()
            } catch (_: Exception) {
            }
        }
    }
}
