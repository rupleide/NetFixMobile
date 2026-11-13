package com.rupleide.netfix.core.dpibypass

import java.io.EOFException
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

enum class ScanFailureReason {
    DNS_RESOLUTION_FAILED,
    CONNECTION_TIMEOUT,
    CONNECTION_REFUSED,
    TLS_HANDSHAKE_FAILED,
    CONNECTION_RESET_BY_PEER,
    SOCKS_PROXY_ERROR,
    HTTP_ERROR_STATUS,
    RESPONSE_INCOMPLETE,
    CHECK_ABORTED,
    UNKNOWN;

    companion object {
        private const val MAX_CAUSE_DEPTH = 6

        fun fromThrowable(error: Throwable): ScanFailureReason {
            var current: Throwable? = error
            var depth = 0
            while (current != null && depth < MAX_CAUSE_DEPTH) {
                val reason = classify(current)
                if (reason != UNKNOWN) return reason
                current = current.cause
                depth++
            }
            return UNKNOWN
        }

        fun forIncompleteResponse(httpStatus: Int, bodyError: Throwable?): ScanFailureReason {
            if (bodyError != null) {
                val reason = fromThrowable(bodyError)
                if (reason != UNKNOWN) return reason
            }
            return if (httpStatus >= 400) HTTP_ERROR_STATUS else RESPONSE_INCOMPLETE
        }

        fun formatLogLine(host: String, reason: ScanFailureReason, detail: String, strategy: String): String =
            "[SCANNER] [ERROR] Host: $host | Error: ${reason.name} | Detail: $detail | Strategy: $strategy"

        private fun classify(error: Throwable): ScanFailureReason {
            val message = error.message.orEmpty()
            return when {
                error is UnknownHostException -> DNS_RESOLUTION_FAILED
                error is SocketTimeoutException -> CONNECTION_TIMEOUT
                error is SSLHandshakeException -> TLS_HANDSHAKE_FAILED
                error is ConnectException -> CONNECTION_REFUSED
                error is EOFException -> CONNECTION_RESET_BY_PEER
                error is SocketException && message.contains("SOCKS", ignoreCase = true) -> SOCKS_PROXY_ERROR
                error is SocketException && message.contains("reset", ignoreCase = true) -> CONNECTION_RESET_BY_PEER
                error is SocketException && message.contains("closed", ignoreCase = true) -> CHECK_ABORTED
                error is SocketException && message.contains("abort", ignoreCase = true) -> CONNECTION_RESET_BY_PEER
                else -> UNKNOWN
            }
        }
    }
}
