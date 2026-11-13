package com.rupleide.netfix.core.dpibypass

class ByeDpiProxy {
    companion object {
        const val BUSY = -2
        private const val TAKEOVER_ATTEMPTS = 20
        private const val TAKEOVER_WAIT_MS = 250L
        private val ownershipLock = Any()
        private var owner: ByeDpiProxy? = null
        init {
            System.loadLibrary("byedpi")
        }

        private fun stopCurrentOwner() {
            synchronized(ownershipLock) {
                if (owner != null) owner?.jniStopProxy()
            }
        }
    }

    fun startProxyTakingOver(
        preferences: ByeDpiProxyPreferences,
        portOverride: Int? = null,
        extraArgs: List<String> = emptyList(),
        leadingArgs: List<String> = emptyList()
    ): Int {
        var code = startProxy(preferences, portOverride, extraArgs, leadingArgs)
        var attempts = 0
        while (code == BUSY && attempts < TAKEOVER_ATTEMPTS && !StrategyTestManager.isTesting && !StrategyTestManager.isVerifying) {
            android.util.Log.w("ByeDpiProxy", "Порт ciadpi занят временной проверкой, останавливаю её")
            stopCurrentOwner()
            Thread.sleep(TAKEOVER_WAIT_MS)
            attempts += 1
            code = startProxy(preferences, portOverride, extraArgs, leadingArgs)
        }
        return code
    }

    fun startProxy(
        preferences: ByeDpiProxyPreferences,
        portOverride: Int? = null,
        extraArgs: List<String> = emptyList(),
        leadingArgs: List<String> = emptyList()
    ): Int {
        val preparedArgs = prepareArgs(preferences)
        val portArgs = if (portOverride != null) ProxyPortResolver.withPort(preparedArgs, portOverride) else preparedArgs
        val withLeading = if (leadingArgs.isEmpty() || portArgs.isEmpty()) portArgs else arrayOf(portArgs[0]) + leadingArgs + portArgs.drop(1)
        val args = withLeading + extraArgs
        synchronized(ownershipLock) {
            if (owner != null) return BUSY
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
