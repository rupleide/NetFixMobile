package com.rupleide.netfix.core.tgproxy

import android.content.Context
import android.util.Log
import java.net.InetSocketAddress
import java.net.Socket
import java.security.SecureRandom

object TgProxyController {
    const val DEFAULT_PORT = 1443
    const val DEFAULT_BIND_IP = "127.0.0.1"

    private fun generateSecret(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun getSharedPrefs(context: Context) =
        context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)

    fun getOrGenerateSecret(context: Context): String {
        val prefs = getSharedPrefs(context)
        var secret = prefs.getString("secret_key", "") ?: ""
        if (secret.isEmpty()) {
            secret = generateSecret()
            prefs.edit().putString("secret_key", secret).apply()
        }
        return secret
    }

    fun regenerateSecret(context: Context): String {
        val secret = generateSecret()
        getSharedPrefs(context).edit().putString("secret_key", secret).apply()
        return secret
    }

    fun isAutostartEnabled(context: Context): Boolean {
        return getSharedPrefs(context).getBoolean("autostart", false)
    }

    fun setAutostartEnabled(context: Context, enabled: Boolean) {
        getSharedPrefs(context).edit().putBoolean("autostart", enabled).apply()
    }

    fun getDcIps(context: Context): String {
        return getSharedPrefs(context).getString("tgproxy_dc_ips", "") ?: ""
    }

    fun setDcIps(context: Context, value: String) {
        getSharedPrefs(context).edit().putString("tgproxy_dc_ips", value).apply()
    }

    fun getPort(context: Context): Int {
        return getSharedPrefs(context).getInt("tgproxy_port", DEFAULT_PORT)
    }

    fun setPort(context: Context, port: Int) {
        getSharedPrefs(context).edit().putInt("tgproxy_port", port).apply()
    }

    fun getPoolSize(context: Context): Int {
        return getSharedPrefs(context).getInt("tgproxy_pool_size", 4)
    }

    fun setPoolSize(context: Context, size: Int) {
        getSharedPrefs(context).edit().putInt("tgproxy_pool_size", size).apply()
    }

    fun isCfEnabled(context: Context): Boolean {
        return getSharedPrefs(context).getBoolean("tgproxy_cf_enabled", true)
    }

    fun setCfEnabled(context: Context, enabled: Boolean) {
        getSharedPrefs(context).edit().putBoolean("tgproxy_cf_enabled", enabled).apply()
    }

    fun isCfPriority(context: Context): Boolean {
        return getSharedPrefs(context).getBoolean("tgproxy_cf_priority", true)
    }

    fun setCfPriority(context: Context, priority: Boolean) {
        getSharedPrefs(context).edit().putBoolean("tgproxy_cf_priority", priority).apply()
    }

    fun getTgProxyUrl(bindIp: String, port: Int, secret: String): String {
        return "tg://proxy?server=$bindIp&port=$port&secret=dd$secret"
    }

    fun isPortOpen(host: String, port: Int, timeoutMs: Int): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), timeoutMs)
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    fun measureLatency(host: String, port: Int): Int {
        val startTime = System.currentTimeMillis()
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), 500)
                (System.currentTimeMillis() - startTime).toInt()
            }
        } catch (e: Exception) {
            -1
        }
    }

    fun startAsync(
        context: Context,
        onSuccess: () -> Unit,
        onError: () -> Unit
    ) {
        val secretKey = getOrGenerateSecret(context)
        val cacheDir = context.cacheDir.absolutePath
        val dcIps = getDcIps(context)
        val port = getPort(context)
        val poolSize = getPoolSize(context)
        val cfEnabled = isCfEnabled(context)
        val cfPriority = isCfPriority(context)

        Thread {
            try {
                NativeProxy.setPoolSize(poolSize)
                NativeProxy.setCfProxyCacheDir(cacheDir)
                NativeProxy.setCfProxyConfig(cfEnabled, cfPriority, "")
                NativeProxy.startProxy(DEFAULT_BIND_IP, port, dcIps, secretKey, 1)
            } catch (e: Throwable) {
                Log.e("TgProxyController", "Error in startProxy thread", e)
            }
        }.start()

        Thread {
            var success = false
            for (i in 1..6) {
                try {
                    Thread.sleep(500)
                } catch (e: InterruptedException) {
                    break
                }
                if (isPortOpen(DEFAULT_BIND_IP, port, 1000)) {
                    success = true
                    break
                }
            }
            if (success) {
                onSuccess()
            } else {
                onError()
            }
        }.start()
    }

    fun stop() {
        Thread {
            try {
                NativeProxy.stopProxy()
            } catch (e: Exception) {
                Log.w("TgProxyController", "StopProxy failed", e)
            }
        }.start()
    }
}
