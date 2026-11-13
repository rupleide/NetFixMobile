package com.rupleide.netfix

import com.rupleide.netfix.core.dpibypass.StreamChunkProbe
import com.rupleide.netfix.core.dpibypass.StreamProbeResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamChunkProbeTest {
    private fun format(mime: String, bitrate: Long, length: Long, url: String) =
        """{"mimeType":"$mime","bitrate":$bitrate,"contentLength":"$length","url":"$url"}"""

    private fun player(status: String, vararg formats: String) =
        """{"playabilityStatus":{"status":"$status"},"streamingData":{"adaptiveFormats":[${formats.joinToString(",")}]}}"""

    @Test
    fun picksLowestBitrateVideoLongEnoughForChunk() {
        val response = player(
            "OK",
            format("audio/mp4", 10, 500_000, "https://a/audio"),
            format("video/mp4", 900, 500_000, "https://a/high"),
            format("video/mp4", 100, 50_000, "https://a/short"),
            format("video/webm", 300, 300_000, "https://a/low")
        )
        assertEquals("https://a/low", StreamChunkProbe.pickChunkUrl(response))
    }

    @Test
    fun noUrlWhenVideoIsNotPlayable() {
        assertNull(StreamChunkProbe.pickChunkUrl(player("LOGIN_REQUIRED", format("video/mp4", 100, 500_000, "https://a/v"))))
    }

    @Test
    fun noUrlWhenFormatsAreCiphered() {
        val ciphered = """{"mimeType":"video/mp4","bitrate":100,"contentLength":"500000","signatureCipher":"s=x"}"""
        assertNull(StreamChunkProbe.pickChunkUrl(player("OK", ciphered)))
    }

    @Test
    fun scoreFollowsTaskFormula() {
        val stream = StreamProbeResult(true, 50, 400, StreamChunkProbe.CHUNK_BYTES, null)
        assertEquals(30 * 100.0 + 1000.0 / 50 + 400 * 0.5, StreamChunkProbe.score(30, stream), 0.0001)
        val fast = StreamProbeResult(true, 5, 0, StreamChunkProbe.CHUNK_BYTES, null)
        assertEquals(1000.0 / 20, StreamChunkProbe.score(0, fast), 0.0001)
    }

    @Test
    fun videoFromOwnServerBeatsVideoOnlyFromBackupServer() {
        val own = StreamProbeResult(true, 300, 1500, StreamChunkProbe.CHUNK_BYTES, null)
        val backup = own.copy(firstByteMs = 100, speedKbps = 3000, viaFallback = true)
        assertTrue(StreamChunkProbe.score(30, own) > StreamChunkProbe.score(30, backup))
        assertTrue(StreamChunkProbe.score(30, backup) > StreamChunkProbe.score(30, StreamProbeResult.failed("нет")))
    }

    @Test
    fun brokenStreamLosesToWorkingOneWithSameHosts() {
        val broken = StreamProbeResult.failed("оборвалось")
        val working = StreamProbeResult(true, 900, 30, StreamChunkProbe.CHUNK_BYTES, null)
        assertEquals(3000.0, StreamChunkProbe.score(30, broken), 0.0001)
        assertTrue(StreamChunkProbe.score(30, working) > StreamChunkProbe.score(30, broken))
        assertTrue(StreamChunkProbe.score(31, broken) > StreamChunkProbe.score(30, working))
    }

    @Test
    fun requestBodyUsesStableProbeVideo() {
        assertTrue(StreamChunkProbe.playerRequestBody().contains(StreamChunkProbe.PROBE_VIDEO_ID))
    }

    @Test
    fun fallbackGoesToSecondNodeWithFvipServer() {
        val url = "https://rr6---sn-jvhnu5g-n8me.googlevideo.com/videoplayback?expire=1&mn=sn-jvhnu5g-n8me%2Csn-4g5lznlz&fvip=5&mime=video%2Fmp4"
        assertEquals(
            "https://rr5---sn-4g5lznlz.googlevideo.com/videoplayback?expire=1&mn=sn-jvhnu5g-n8me%2Csn-4g5lznlz&fvip=5&mime=video%2Fmp4",
            StreamChunkProbe.fallbackNodeUrl(url)
        )
    }

    @Test
    fun noFallbackWithoutSecondNodeOrServerIndex() {
        assertNull(StreamChunkProbe.fallbackNodeUrl("https://rr1---sn-a.googlevideo.com/videoplayback?mn=sn-a&fvip=1"))
        assertNull(StreamChunkProbe.fallbackNodeUrl("https://rr1---sn-a.googlevideo.com/videoplayback?mn=sn-a%2Csn-b"))
    }
}
