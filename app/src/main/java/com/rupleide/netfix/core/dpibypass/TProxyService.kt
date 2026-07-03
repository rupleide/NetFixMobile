package com.rupleide.netfix.core.dpibypass

object TProxyService {
    init {
        System.loadLibrary("hev-socks5-tunnel")
    }

    @JvmStatic
    external fun TProxyStartService(configPath: String, fd: Int, isSmartTv: Boolean)

    @JvmStatic
    external fun TProxyStopService()

    @JvmStatic
    @Suppress("unused")
    external fun TProxyGetStats(): LongArray
}
