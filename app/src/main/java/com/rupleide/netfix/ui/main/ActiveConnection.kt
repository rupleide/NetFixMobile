package com.rupleide.netfix.ui.main

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.telephony.TelephonyManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

enum class ConnectionTransport { Wifi, Mobile, Ethernet, None }

data class ActiveConnection(
    val transport: ConnectionTransport,
    val label: String?,
    val cellularAlsoAvailable: Boolean
) {
    companion object {
        val Unknown = ActiveConnection(ConnectionTransport.None, null, false)
    }
}

private val networkAssistantManufacturers = listOf("xiaomi", "redmi", "poco", "realme")

fun isNetworkAssistantManufacturer(manufacturer: String): Boolean {
    val normalized = manufacturer.lowercase()
    return networkAssistantManufacturers.any { normalized.contains(it) }
}

fun shouldShowNetworkAssistantHint(manufacturer: String, connection: ActiveConnection, dismissed: Boolean): Boolean =
    !dismissed &&
        isNetworkAssistantManufacturer(manufacturer) &&
        connection.transport == ConnectionTransport.Wifi &&
        connection.cellularAlsoAvailable

fun connectionLabel(transport: ConnectionTransport, operatorName: String?): String? = when (transport) {
    ConnectionTransport.Wifi -> "Wi-Fi"
    ConnectionTransport.Mobile -> operatorName?.takeIf { it.isNotBlank() }?.let { "Мобильная: $it" } ?: "Мобильная сеть"
    ConnectionTransport.Ethernet -> "Кабель: Ethernet"
    ConnectionTransport.None -> null
}

fun Context.activeConnection(): ActiveConnection {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    return try {
        val physical = connectivityManager.allNetworks.mapNotNull { network ->
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return@mapNotNull null
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) return@mapNotNull null
            if (!capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) return@mapNotNull null
            capabilities
        }
        val activeCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            ?.takeUnless { it.hasTransport(NetworkCapabilities.TRANSPORT_VPN) }
        val candidates = listOfNotNull(activeCapabilities) + physical
        val transport = when {
            candidates.any { it.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) } -> ConnectionTransport.Ethernet
            candidates.any { it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } -> ConnectionTransport.Wifi
            candidates.any { it.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) } -> ConnectionTransport.Mobile
            else -> ConnectionTransport.None
        }
        val operatorName = if (transport == ConnectionTransport.Mobile) {
            (getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager)?.networkOperatorName
        } else {
            null
        }
        ActiveConnection(
            transport = transport,
            label = connectionLabel(transport, operatorName),
            cellularAlsoAvailable = physical.any { it.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) }
        )
    } catch (_: SecurityException) {
        ActiveConnection.Unknown
    }
}

@Composable
fun rememberActiveConnection(): ActiveConnection {
    val context = LocalContext.current
    var connection by remember { mutableStateOf(context.activeConnection()) }
    DisposableEffect(context) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                connection = context.activeConnection()
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                connection = context.activeConnection()
            }

            override fun onLost(network: Network) {
                connection = context.activeConnection()
            }
        }
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        try {
            connectivityManager.registerNetworkCallback(request, callback)
        } catch (_: Exception) {
        }
        onDispose {
            try {
                connectivityManager.unregisterNetworkCallback(callback)
            } catch (_: Exception) {
            }
        }
    }
    return connection
}
