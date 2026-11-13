package com.rupleide.netfix.core.dpibypass

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.Proxy
import java.net.URL

data class StreamProbeResult(
    val loaded: Boolean,
    val firstByteMs: Long,
    val speedKbps: Long,
    val bytes: Int,
    val failure: String?,
    val viaFallback: Boolean = false
) {
    companion object {
        fun failed(reason: String) = StreamProbeResult(false, -1L, 0L, 0, reason)
    }
}

object StreamChunkProbe {
    const val PROBE_VIDEO_ID = "jNQXAC9IVRw"
    const val CHUNK_BYTES = 131_072
    private const val PLAYER_URL = "https://www.youtube.com/youtubei/v1/player?prettyPrint=false"
    private const val CLIENT_NAME = "IOS"
    private const val CLIENT_ID = "5"
    private const val CLIENT_VERSION = "20.10.4"
    private const val USER_AGENT = "com.google.ios.youtube/20.10.4 (iPhone16,2; U; CPU iOS 18_3_2 like Mac OS X;)"
    private const val MIN_PING_MS = 20L

    fun playerRequestBody(videoId: String = PROBE_VIDEO_ID): String {
        val client = JSONObject()
            .put("clientName", CLIENT_NAME)
            .put("clientVersion", CLIENT_VERSION)
            .put("deviceMake", "Apple")
            .put("deviceModel", "iPhone16,2")
            .put("osName", "iPhone")
            .put("osVersion", "18.3.2.22D82")
            .put("hl", "en")
            .put("gl", "US")
        return JSONObject()
            .put("videoId", videoId)
            .put("context", JSONObject().put("client", client))
            .toString()
    }

    fun pickChunkUrl(playerResponse: String): String? {
        val root = JSONObject(playerResponse)
        val status = root.optJSONObject("playabilityStatus")?.optString("status")
        if (status != "OK") return null
        val formats = root.optJSONObject("streamingData")?.optJSONArray("adaptiveFormats") ?: return null
        var bestUrl: String? = null
        var bestBitrate = Long.MAX_VALUE
        for (i in 0 until formats.length()) {
            val format = formats.optJSONObject(i) ?: continue
            val url = format.optString("url")
            val length = format.optString("contentLength").toLongOrNull() ?: 0L
            val bitrate = format.optLong("bitrate", Long.MAX_VALUE)
            val isVideo = format.optString("mimeType").startsWith("video/")
            if (isVideo && url.isNotEmpty() && length >= CHUNK_BYTES && bitrate < bestBitrate) {
                bestUrl = url
                bestBitrate = bitrate
            }
        }
        return bestUrl
    }

    fun score(reachableHosts: Int, stream: StreamProbeResult?): Double {
        val hostsPart = reachableHosts * 100.0
        if (stream == null || !stream.loaded) return hostsPart
        if (stream.viaFallback) {
            return hostsPart + 1000.0 / (maxOf(stream.firstByteMs, MIN_PING_MS) + APP_FALLBACK_WAIT_MS) + stream.speedKbps * 0.5 * FALLBACK_SPEED_WEIGHT
        }
        return hostsPart + 1000.0 / maxOf(stream.firstByteMs, MIN_PING_MS) + stream.speedKbps * 0.5
    }

    private const val APP_FALLBACK_WAIT_MS = 4_000L
    private const val FALLBACK_SPEED_WEIGHT = 0.2

    fun probe(proxy: Proxy, timeoutMs: Int, register: (HttpURLConnection) -> Unit, unregister: (HttpURLConnection) -> Unit): StreamProbeResult {
        val chunkUrl = try {
            requestChunkUrl(proxy, timeoutMs, register, unregister)
        } catch (e: Exception) {
            return StreamProbeResult.failed("player: $e")
        } ?: return StreamProbeResult.failed("player: нет ссылки на поток")
        val primary = downloadChunk(chunkUrl, proxy, timeoutMs, register, unregister)
        if (primary.loaded) return primary
        val fallback = fallbackNodeUrl(chunkUrl) ?: return primary
        val retried = downloadChunk(fallback, proxy, timeoutMs, register, unregister)
        return if (retried.loaded) retried.copy(viaFallback = true) else primary
    }

    fun fallbackNodeUrl(chunkUrl: String): String? {
        val query = chunkUrl.substringAfter('?', "")
        val params = query.split('&').mapNotNull { pair ->
            val key = pair.substringBefore('=', "")
            if (key.isEmpty()) null else key to java.net.URLDecoder.decode(pair.substringAfter('=', ""), "UTF-8")
        }.toMap()
        val nodes = params["mn"]?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() } ?: return null
        val backupNode = nodes.getOrNull(1) ?: return null
        val serverIndex = params["fvip"]?.toIntOrNull() ?: return null
        val path = chunkUrl.substringAfter("://").substringAfter('/', "")
        return "https://rr$serverIndex---$backupNode.googlevideo.com/$path"
    }

    private fun requestChunkUrl(proxy: Proxy, timeoutMs: Int, register: (HttpURLConnection) -> Unit, unregister: (HttpURLConnection) -> Unit): String? {
        val connection = URL(PLAYER_URL).openConnection(proxy) as HttpURLConnection
        register(connection)
        try {
            connection.connectTimeout = timeoutMs
            connection.readTimeout = timeoutMs
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.setRequestProperty("X-YouTube-Client-Name", CLIENT_ID)
            connection.setRequestProperty("X-YouTube-Client-Version", CLIENT_VERSION)
            connection.setRequestProperty("Connection", "close")
            connection.outputStream.use { it.write(playerRequestBody().toByteArray()) }
            if (connection.responseCode !in 200..299) return null
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            return pickChunkUrl(response)
        } finally {
            unregister(connection)
            connection.disconnect()
        }
    }

    private fun downloadChunk(chunkUrl: String, proxy: Proxy, timeoutMs: Int, register: (HttpURLConnection) -> Unit, unregister: (HttpURLConnection) -> Unit): StreamProbeResult {
        val connection = URL(chunkUrl).openConnection(proxy) as HttpURLConnection
        register(connection)
        var received = 0
        try {
            connection.connectTimeout = timeoutMs
            connection.readTimeout = timeoutMs
            connection.setRequestProperty("Range", "bytes=0-${CHUNK_BYTES - 1}")
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.setRequestProperty("Connection", "close")
            val started = System.nanoTime()
            val code = connection.responseCode
            if (code !in 200..299) return StreamProbeResult.failed("HTTP $code")
            val body = connection.inputStream
            val buffer = ByteArray(16_384)
            var firstByteMs = -1L
            while (received < CHUNK_BYTES) {
                val read = body.read(buffer, 0, minOf(buffer.size, CHUNK_BYTES - received))
                if (read == -1) break
                if (firstByteMs < 0) firstByteMs = (System.nanoTime() - started) / 1_000_000
                received += read
            }
            val totalMs = maxOf((System.nanoTime() - started) / 1_000_000, 1L)
            if (received < CHUNK_BYTES) {
                return StreamProbeResult(false, firstByteMs, 0L, received, "оборвалось на $received из $CHUNK_BYTES байт")
            }
            val speedKbps = received.toLong() * 1000 / 1024 / totalMs
            return StreamProbeResult(true, firstByteMs, speedKbps, received, null)
        } catch (e: Exception) {
            return StreamProbeResult(false, -1L, 0L, received, "поток: $e" + if (received > 0) ", получено $received байт" else "")
        } finally {
            unregister(connection)
            connection.disconnect()
        }
    }
}
