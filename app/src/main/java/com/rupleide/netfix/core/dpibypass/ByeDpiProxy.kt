package com.rupleide.netfix.core.dpibypass

class ByeDpiProxy {
    companion object {
        private val ownershipLock = Any()
        private var owner: ByeDpiProxy? = null
        init {
            System.loadLibrary("byedpi")
        }
    }

    fun startProxy(preferences: ByeDpiProxyPreferences): Int {
        val args = prepareArgs(preferences)
        synchronized(ownershipLock) {
            if (owner != null) return -1
            owner = this
        }
        try {
            return jniStartProxy(args)
        } finally {
            synchronized(ownershipLock) { owner = null }
        }
    }

    fun stopProxy(): Int {
        return synchronized(ownershipLock) {
            if (owner === this) jniStopProxy() else -1
        }
    }

    fun jniForceClose(): Int = synchronized(ownershipLock) {
        if (owner === this) jniCloseProxy() else -1
    }

    private fun prepareArgs(preferences: ByeDpiProxyPreferences): Array<String> =
        when (preferences) {
            is ByeDpiProxyCmdPreferences -> preferences.args
            is ByeDpiProxyUIPreferences -> preferences.uiargs
        }

    private external fun jniStartProxy(args: Array<String>): Int
    private external fun jniStopProxy(): Int
    private external fun jniCloseProxy(): Int
}
