package com.rupleide.netfix.core.dpibypass

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.rupleide.netfix.core.compat.CompatibilityManager

object VideoFastFail {
    const val WIFI_ACK_TIMEOUT_SECONDS = "1.5"
    const val MOBILE_ACK_TIMEOUT_SECONDS = "2.5"

    fun extraArgs(context: Context, strategyArgs: Array<String>): List<String> {
        if (!CompatibilityManager.videoFastFailEnabled(context.getPreferences())) return emptyList()
        return argsFor(strategyArgs, isMobileNetwork(context))
    }

    const val QUIC_GIVEUP_MS = "400"
    const val VIDEO_HOST_SUFFIX = "googlevideo.com"

    fun argsFor(strategyArgs: Array<String>, mobile: Boolean): List<String> {
        val quicArgs = if (strategyArgs.any { it.startsWith("--quic-giveup") }) emptyList() else listOf("--quic-giveup", QUIC_GIVEUP_MS)
        if (strategyArgs.any { it == "-T" || it.startsWith("--timeout") || (it.startsWith("-T") && it.length > 2) }) return quicArgs
        val seconds = if (mobile) MOBILE_ACK_TIMEOUT_SECONDS else WIFI_ACK_TIMEOUT_SECONDS
        return listOf("-T", "$seconds:0:3:1", "--keep-timeout-suffix", VIDEO_HOST_SUFFIX) + quicArgs
    }

    private fun isMobileNetwork(context: Context): Boolean = runCatching {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        manager.allNetworks
            .mapNotNull { manager.getNetworkCapabilities(it) }
            .filter { !it.hasTransport(NetworkCapabilities.TRANSPORT_VPN) && it.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) }
            .none { it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || it.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) }
    }.getOrDefault(true)
}
