package com.rupleide.netfix

import com.rupleide.netfix.core.dpibypass.ScanFailureReason
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.EOFException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

class ScanFailureReasonTest {

    @Test
    fun exceptionsAreMappedToSpecificReasons() {
        assertEquals(ScanFailureReason.DNS_RESOLUTION_FAILED, ScanFailureReason.fromThrowable(UnknownHostException("a.b")))
        assertEquals(ScanFailureReason.CONNECTION_TIMEOUT, ScanFailureReason.fromThrowable(SocketTimeoutException("connect timed out")))
        assertEquals(ScanFailureReason.CONNECTION_REFUSED, ScanFailureReason.fromThrowable(ConnectException("Connection refused")))
        assertEquals(ScanFailureReason.TLS_HANDSHAKE_FAILED, ScanFailureReason.fromThrowable(SSLHandshakeException("handshake")))
        assertEquals(ScanFailureReason.CONNECTION_RESET_BY_PEER, ScanFailureReason.fromThrowable(EOFException()))
        assertEquals(ScanFailureReason.CONNECTION_RESET_BY_PEER, ScanFailureReason.fromThrowable(SocketException("Connection reset")))
        assertEquals(ScanFailureReason.SOCKS_PROXY_ERROR, ScanFailureReason.fromThrowable(SocketException("SOCKS server general failure")))
        assertEquals(ScanFailureReason.CHECK_ABORTED, ScanFailureReason.fromThrowable(SocketException("Socket closed")))
    }

    @Test
    fun unknownErrorStaysUnknown() {
        assertEquals(ScanFailureReason.UNKNOWN, ScanFailureReason.fromThrowable(IOException("strange")))
    }

    @Test
    fun wrappedCauseIsClassified() {
        assertEquals(
            ScanFailureReason.CONNECTION_TIMEOUT,
            ScanFailureReason.fromThrowable(IOException("wrapper", SocketTimeoutException("read timed out")))
        )
    }

    @Test
    fun incompleteResponseUsesBodyErrorFirst() {
        assertEquals(
            ScanFailureReason.CONNECTION_RESET_BY_PEER,
            ScanFailureReason.forIncompleteResponse(200, SocketException("Connection reset"))
        )
    }

    @Test
    fun incompleteResponseWithErrorStatusIsHttpError() {
        assertEquals(ScanFailureReason.HTTP_ERROR_STATUS, ScanFailureReason.forIncompleteResponse(503, null))
        assertEquals(ScanFailureReason.RESPONSE_INCOMPLETE, ScanFailureReason.forIncompleteResponse(200, null))
    }

    @Test
    fun logLineContainsHostReasonDetailAndStrategy() {
        val line = ScanFailureReason.formatLogLine(
            "manifest.googlevideo.com",
            ScanFailureReason.CONNECTION_TIMEOUT,
            "SocketTimeoutException: failed to connect",
            "-o1 -r-5+se -a1"
        )
        assertEquals(
            "[SCANNER] [ERROR] Host: manifest.googlevideo.com | Error: CONNECTION_TIMEOUT | Detail: SocketTimeoutException: failed to connect | Strategy: -o1 -r-5+se -a1",
            line
        )
    }
}
