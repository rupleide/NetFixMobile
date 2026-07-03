package com.rupleide.netfix.data

enum class AppStatus {
    Halted,
    Running,
}

enum class Mode {
    Proxy,
    VPN;

    companion object {
        fun fromSender(sender: Sender): Mode = when (sender) {
            Sender.Proxy -> Proxy
            Sender.VPN -> VPN
        }

        fun fromString(name: String): Mode = when (name) {
            "proxy" -> Proxy
            "vpn" -> VPN
            else -> throw IllegalArgumentException("Invalid mode: $name")
        }
    }
}

enum class ServiceStatus {
    Disconnected,
    Connected,
    Failed,
}

enum class Sender(val senderName: String) {
    Proxy("Proxy"),
    VPN("VPN")
}
