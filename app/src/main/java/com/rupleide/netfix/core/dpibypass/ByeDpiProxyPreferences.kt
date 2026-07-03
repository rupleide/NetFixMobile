package com.rupleide.netfix.core.dpibypass

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

sealed interface ByeDpiProxyPreferences {
    companion object {
        fun fromSharedPreferences(preferences: SharedPreferences, context: Context): ByeDpiProxyPreferences =
            when (preferences.getBoolean("byedpi_enable_cmd_settings", false)) {
                true -> ByeDpiProxyCmdPreferences(preferences, context)
                false -> ByeDpiProxyUIPreferences(preferences)
            }
    }
}

class ByeDpiProxyCmdPreferences(val args: Array<String>) : ByeDpiProxyPreferences {
    constructor(preferences: SharedPreferences, context: Context) : this(
        parseCmdToArguments(preferences, context)
    )

    companion object {
        private fun parseCmdToArguments(preferences: SharedPreferences, context: Context): Array<String> {
            val cmd = preferences.getStringNotNull("byedpi_cmd_args", "-o1 -a1 -r-5+se")
            val preparedCmd = getLists(cmd, context)

            val firstArgIndex = preparedCmd.indexOf("-")
            val args = (if (firstArgIndex > 0) preparedCmd.substring(firstArgIndex) else preparedCmd).trim()

            Log.d("ProxyPref", "CMD: $args")

            val (cmdIp, cmdPort) = preferences.checkIpAndPortInCmd()
            val ip = preferences.getStringNotNull("byedpi_proxy_ip", "127.0.0.1")
            val port = preferences.getStringNotNull("byedpi_proxy_port", "1080")

            val prefix = buildString {
                if (cmdIp == null) append("--ip $ip ")
                if (cmdPort == null) append("--port $port ")
            }

            Log.d("ProxyPref", "Added from settings: $prefix")

            val blacklist = setOf("--help", "--version", "-h", "-v")
            val splitArgs = shellSplit("$prefix$args").filter { it !in blacklist }

            return arrayOf("ciadpi") + splitArgs
        }

        private fun getLists(cmd: String, context: Context): String {
            return Regex("""\{list:([^}]+)\}""").replace(cmd) { matchResult ->
                val listName = matchResult.groupValues[1].trim()
                val lists = DomainListUtils.getLists(context)
                val domainList = lists.find { it.name.equals(listName, ignoreCase = true) }

                if (domainList != null && domainList.domains.isNotEmpty()) {
                    domainList.domains.joinToString(" ")
                } else {
                    Log.w("ProxyPref", "List '$listName' not found or empty")
                    ""
                }
            }
        }
    }
}

class ByeDpiProxyUIPreferences(val settings: UISettings = UISettings()) : ByeDpiProxyPreferences {

    constructor(preferences: SharedPreferences) : this(
        UISettings.fromSharedPreferences(preferences)
    )

    val uiargs: Array<String>
        get() {
            val args = mutableListOf("ciadpi")

            if (settings.ip.isNotEmpty()) args.add("-i${settings.ip}")
            if (settings.port != 0) args.add("-p${settings.port}")
            if (settings.maxConnections != 0) args.add("-c${settings.maxConnections}")
            if (settings.bufferSize != 0) args.add("-b${settings.bufferSize}")

            val protocols = buildList {
                if (settings.desyncHttps) add("t")
                if (settings.desyncHttp) add("h")
            }

            if (!settings.hosts.isNullOrBlank()) {
                val hostStr = ":${settings.hosts.replace("\n", " ")}"
                when (settings.hostsMode) {
                    UISettings.HostsMode.Blacklist -> {
                        args.add("-H$hostStr")
                        args.add("-An")
                        if (protocols.isNotEmpty()) args.add("-K${protocols.joinToString(",")}")
                    }
                    UISettings.HostsMode.Whitelist -> {
                        if (protocols.isNotEmpty()) args.add("-K${protocols.joinToString(",")}")
                        args.add("-H$hostStr")
                    }
                    else -> {}
                }
            } else {
                if (protocols.isNotEmpty()) args.add("-K${protocols.joinToString(",")}")
            }

            if (settings.defaultTtl != 0) args.add("-g${settings.defaultTtl}")
            if (settings.noDomain) args.add("-N")

            if (settings.splitPosition != 0) {
                val posArg = settings.splitPosition.toString() + if (settings.splitAtHost) "+h" else ""
                val option = when (settings.desyncMethod) {
                    UISettings.DesyncMethod.Split -> "-s"
                    UISettings.DesyncMethod.Disorder -> "-d"
                    UISettings.DesyncMethod.OOB -> "-o"
                    UISettings.DesyncMethod.DISOOB -> "-q"
                    UISettings.DesyncMethod.Fake -> "-f"
                    UISettings.DesyncMethod.None -> ""
                }
                if (option.isNotEmpty()) args.add("$option$posArg")
            }

            if (settings.desyncMethod == UISettings.DesyncMethod.Fake) {
                if (settings.fakeTtl != 0) args.add("-t${settings.fakeTtl}")
                if (settings.fakeSni.isNotEmpty()) args.add("-n${settings.fakeSni}")
                if (settings.fakeOffset != 0) args.add("-O${settings.fakeOffset}")
            }

            if (settings.desyncMethod == UISettings.DesyncMethod.OOB || settings.desyncMethod == UISettings.DesyncMethod.DISOOB) {
                args.add("-e${settings.oobChar[0].code.toByte()}")
            }

            val modHttpFlags = buildList {
                if (settings.hostMixedCase) add("h")
                if (settings.domainMixedCase) add("d")
                if (settings.hostRemoveSpaces) add("r")
            }

            if (modHttpFlags.isNotEmpty()) args.add("-M${modHttpFlags.joinToString(",")}")

            if (settings.tlsRecordSplit && settings.tlsRecordSplitPosition != 0) {
                val tlsRecArg = settings.tlsRecordSplitPosition.toString() + if (settings.tlsRecordSplitAtSni) "+s" else ""
                args.add("-r$tlsRecArg")
            }

            if (settings.tcpFastOpen) args.add("-F")
            if (settings.dropSack) args.add("-Y")

            args.add("-An")

            if (settings.desyncUdp) {
                args.add("-Ku")
                if (settings.udpFakeCount != 0) args.add("-a${settings.udpFakeCount}")
                args.add("-An")
            }

            Log.d("ProxyPref", "UI to cmd: ${args.joinToString(" ")}")
            return args.toTypedArray()
        }
}
