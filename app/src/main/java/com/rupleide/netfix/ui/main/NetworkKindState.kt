package com.rupleide.netfix.ui.main

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.rupleide.netfix.core.dpibypass.NetworkKind
import com.rupleide.netfix.core.dpibypass.currentNetworkKind

@Composable
fun rememberCurrentNetworkKind(): NetworkKind? {
    val context = LocalContext.current
    var kind by remember { mutableStateOf(context.currentNetworkKind()) }
    DisposableEffect(context) {
        val connectivityManager = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                kind = context.currentNetworkKind()
            }

            override fun onLost(network: Network) {
                kind = context.currentNetworkKind()
            }
        }
        try {
            connectivityManager.registerDefaultNetworkCallback(callback)
        } catch (_: Exception) {
        }
        onDispose {
            try {
                connectivityManager.unregisterNetworkCallback(callback)
            } catch (_: Exception) {
            }
        }
    }
    return kind
}
