package com.rupleide.netfix.core.dnscheck

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import com.rupleide.netfix.core.compat.CompatibilityManager
import com.rupleide.netfix.core.compat.TunnelDnsMode
import com.rupleide.netfix.core.dpibypass.currentNetworkKind
import com.rupleide.netfix.core.dpibypass.getPreferences
import com.rupleide.netfix.core.help.HelpHostCheck
import com.rupleide.netfix.core.help.HelpProbes
import com.rupleide.netfix.core.help.HostVerdict
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.Inet4Address
import java.net.InetAddress

data class DnsCheckTarget(val label: String, val hosts: List<String>)

data class TunnelDnsPreset(val name: String, val address: String)

object TunnelDnsCatalog {
    const val SYSTEM = "Системный"
    const val CLOUDFLARE = "Cloudflare 1.1.1.1"
    const val CUSTOM = "Свой адрес"

    val presets = listOf(
        TunnelDnsPreset("WLRus", "dns.wlrus.cfd"),
        TunnelDnsPreset("Bezmezhau", "dns.bezmezhau.com"),
        TunnelDnsPreset("Comss", "dns.comss.one"),
        TunnelDnsPreset("GeoHide", "dns.geohide.ru"),
        TunnelDnsPreset("Dagami", "dns.dagami.ru"),
        TunnelDnsPreset("Quad9 9.9.9.9", "9.9.9.9"),
        TunnelDnsPreset("Яндекс 77.88.8.8", "77.88.8.8")
    )

    fun preset(name: String?): TunnelDnsPreset? = presets.firstOrNull { it.name == name }
}

data class DnsCheckResult(
    val server: String?,
    val answerMs: Long,
    val verdicts: Map<String, HostVerdict>,
    val failure: String? = null,
    val checkedAt: Long = 0L,
    val direct: Boolean = false
) {
    val okTargets: Int get() = TunnelDnsCheck.targets.count { verdictFor(it) == HostVerdict.Ok }
    val totalTargets: Int get() = TunnelDnsCheck.targets.size

    fun verdictFor(target: DnsCheckTarget): HostVerdict? {
        val found = target.hosts.mapNotNull { verdicts[it] }
        if (found.isEmpty()) return null
        return when {
            found.all { it == HostVerdict.Ok } -> HostVerdict.Ok
            found.any { it == HostVerdict.RegionBlocked } -> HostVerdict.RegionBlocked
            else -> HostVerdict.Blocked
        }
    }
}

object TunnelDnsCheck {
    private const val TAG = "NetFixDnsCheck"
    val targets = listOf(
        DnsCheckTarget("YouTube", listOf("www.youtube.com", "i.ytimg.com")),
        DnsCheckTarget("Instagram", listOf("www.instagram.com", "i.instagram.com")),
        DnsCheckTarget("ChatGPT", listOf("chatgpt.com/cdn-cgi/trace", "cdn.oaistatic.com")),
        DnsCheckTarget("Gemini", listOf("gemini.google.com")),
        DnsCheckTarget("Spotify", listOf("open.spotify.com", "spclient.wg.spotify.com")),
        DnsCheckTarget("Discord", listOf("discord.com", "cdn.discordapp.com")),
        DnsCheckTarget("X (Twitter)", listOf("x.com", "abs.twimg.com"))
    )

    private val allHosts = targets.flatMap { it.hosts }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val oneAtATime = Mutex()

    private const val PREFS = "tunnel_dns_check"
    private const val RESULTS_KEY = "results"

    private val results = mutableStateMapOf<String, DnsCheckResult>()
    private var loaded = false
    val running = mutableStateListOf<String>()
    val queued = mutableStateListOf<String>()

    fun isBusy(key: String): Boolean = key in running || key in queued

    fun networkKey(context: Context): String = context.currentNetworkKind()?.preferenceSuffix ?: "other"

    fun resultFor(network: String, key: String): DnsCheckResult? = results["$network|$key"]

    fun hasResults(network: String): Boolean = results.keys.any { it.startsWith("$network|") }

    fun load(context: Context) {
        if (loaded) return
        loaded = true
        val raw = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(RESULTS_KEY, null) ?: return
        try {
            val root = JSONObject(raw)
            root.keys().forEach { key ->
                val item = root.getJSONObject(key)
                val verdictsJson = item.optJSONObject("verdicts") ?: JSONObject()
                val verdicts = verdictsJson.keys().asSequence().mapNotNull { host ->
                    runCatching { host to HostVerdict.valueOf(verdictsJson.getString(host)) }.getOrNull()
                }.toMap()
                results[key] = DnsCheckResult(
                    server = item.optString("server").ifBlank { null },
                    answerMs = item.optLong("ms", -1),
                    verdicts = verdicts,
                    failure = item.optString("failure").ifBlank { null },
                    checkedAt = item.optLong("at", 0L),
                    direct = item.optBoolean("direct", false)
                )
            }
        } catch (_: Exception) {
        }
    }

    private fun save(context: Context) {
        val root = JSONObject()
        results.forEach { (key, result) ->
            root.put(key, JSONObject().apply {
                put("server", result.server.orEmpty())
                put("ms", result.answerMs)
                put("failure", result.failure.orEmpty())
                put("at", result.checkedAt)
                put("direct", result.direct)
                put("verdicts", JSONObject().apply { result.verdicts.forEach { (host, verdict) -> put(host, verdict.name) } })
            })
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(RESULTS_KEY, root.toString()).apply()
    }

    fun start(
        context: Context,
        key: String,
        server: suspend () -> List<String>,
        systemDns: Boolean,
        encrypted: Boolean = false,
        allowDirect: Boolean = false
    ) {
        if (isBusy(key)) return
        val app = context.applicationContext
        load(app)
        val network = networkKey(app)
        queued.add(key)
        scope.launch { checkQueued(app, network, key, server, systemDns, encrypted, allowDirect) }
    }

    suspend fun checkNow(context: Context, key: String, server: suspend () -> List<String>, systemDns: Boolean): DnsCheckResult {
        val app = context.applicationContext
        withContext(Dispatchers.Main) {
            load(app)
            queued.add(key)
        }
        return checkQueued(app, networkKey(app), key, server, systemDns)
    }

    private suspend fun checkQueued(
        app: Context,
        network: String,
        key: String,
        server: suspend () -> List<String>,
        systemDns: Boolean,
        encrypted: Boolean = false,
        allowDirect: Boolean = false
    ): DnsCheckResult =
        oneAtATime.withLock {
            withContext(Dispatchers.Main) {
                queued.remove(key)
                running.add(key)
            }
            val startedOnLivePath = withContext(Dispatchers.IO) { HelpProbes.liveProxyPort(app) != null }
            var checked = runCheck(app, server, systemDns, encrypted, allowDirect)
            val interruptedByStart = !startedOnLivePath && checked.verdicts.values.none { it == HostVerdict.Ok } &&
                withContext(Dispatchers.IO) { HelpProbes.liveProxyPort(app) != null }
            if (interruptedByStart) checked = runCheck(app, server, systemDns, encrypted, allowDirect)
            val result = checked.copy(checkedAt = System.currentTimeMillis())
            withContext(Dispatchers.Main) {
                results["$network|$key"] = result
                running.remove(key)
                save(app)
            }
            refreshChosenPresetAddresses(app, key, result)
            result
        }

    private suspend fun refreshChosenPresetAddresses(app: Context, key: String, result: DnsCheckResult) {
        val preset = TunnelDnsCatalog.preset(key) ?: return
        if (CompatibilityManager.isIpLiteral(preset.address)) return
        val prefs = app.getPreferences()
        if (CompatibilityManager.tunnelDnsMode(prefs) != TunnelDnsMode.Custom) return
        if (CompatibilityManager.customTunnelDnsName(prefs) != preset.name) return
        if (result.okTargets == 0) return
        val freshAddresses = resolveServers(preset.address).joinToString(", ")
        if (CompatibilityManager.parseDnsServers(freshAddresses).isEmpty()) return
        val storedAddresses = CompatibilityManager.parseDnsServers(CompatibilityManager.customTunnelDns(prefs)).toSet()
        if (CompatibilityManager.parseDnsServers(freshAddresses).toSet() == storedAddresses) return
        prefs.edit().putString(CompatibilityManager.KEY_TUNNEL_DNS_CUSTOM, freshAddresses).apply()
        Log.i(TAG, "Адреса DNS ${preset.name} обновлены: $freshAddresses")
        withContext(Dispatchers.IO) {
            if (com.rupleide.netfix.core.dpibypass.SecureDnsRelay.isRunning) com.rupleide.netfix.core.dpibypass.SecureDnsRelay.start(app)
        }
    }

    suspend fun resolveServers(address: String): List<String> = withContext(Dispatchers.IO) {
        try {
            InetAddress.getAllByName(address.trim()).filterIsInstance<Inet4Address>().mapNotNull { it.hostAddress }.distinct().take(2)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun resolveLikeTunnel(host: String, servers: List<String>, socksPort: Int?, encrypted: Boolean): Pair<String, String?>? {
        val name = host.substringBefore('/')
        if (socksPort != null && !encrypted && com.rupleide.netfix.core.dpibypass.SecureDnsRelay.isRunning) {
            if (servers.isEmpty()) return HelpHostCheck.resolveSecure(name, emptyList())?.let { it to null }
            for (server in servers) {
                HelpHostCheck.resolveSecure(name, listOf(server))?.let { return it to server }
            }
            return null
        }
        if (servers.isEmpty()) return HelpHostCheck.resolve(name, null)?.let { it to null }
        for (server in servers) {
            val resolved = when {
                encrypted -> HelpHostCheck.resolveViaDot(name, server)
                socksPort == null -> HelpHostCheck.resolve(name, server)
                else -> HelpHostCheck.resolveViaSocks(name, server, socksPort)
            }
            resolved?.let { return it to server }
        }
        return null
    }

    private suspend fun runCheck(
        context: Context,
        servers: suspend () -> List<String>,
        systemDns: Boolean,
        encrypted: Boolean,
        allowDirect: Boolean
    ): DnsCheckResult {
        val addresses = if (systemDns) emptyList() else servers()
        if (!systemDns && addresses.isEmpty()) {
            return DnsCheckResult(null, -1, emptyMap(), "Не получилось узнать адрес этого DNS")
        }
        val shownAddress = addresses.joinToString(", ").ifBlank { null }
        val scanRunning = com.rupleide.netfix.core.dpibypass.StrategyTestManager.isTesting ||
            com.rupleide.netfix.core.dpibypass.StrategyTestManager.isVerifying
        val noPathReason = if (scanRunning) "Сейчас идёт подбор способа, проверьте позже" else "Сначала подберите способ на главном экране, потом вернитесь сюда"
        val noPath = DnsCheckResult(shownAddress, -1, emptyMap(), noPathReason)
        val viaNetFix = HelpProbes.onCurrentPath(context, null as DnsCheckResult?) { port ->
            checkThrough(port, addresses, shownAddress, encrypted)
        }
        return viaNetFix ?: if (allowDirect && !scanRunning) checkThrough(null, addresses, shownAddress, encrypted) else noPath
    }

    private suspend fun checkThrough(port: Int?, addresses: List<String>, shownAddress: String?, encrypted: Boolean): DnsCheckResult {
        val started = System.nanoTime()
        val first = withContext(Dispatchers.IO) { resolveLikeTunnel(allHosts.first(), addresses, port, encrypted) }
        val answerMs = if (first != null) (System.nanoTime() - started) / 1_000_000 else -1
        if (first == null) {
            Log.i(TAG, "DNS $shownAddress не ответил ${if (port == null) "напрямую" else "через NetFix"}")
            return DnsCheckResult(shownAddress, -1, allHosts.associateWith { HostVerdict.Blocked }, "DNS не отвечает", direct = port == null)
        }
        val answering = first.second
        val ordered = if (answering == null) addresses else listOf(answering) + (addresses - answering)
        val verdicts = coroutineScope {
            allHosts.map { host ->
                async(Dispatchers.IO) {
                    val resolved = if (host == allHosts.first()) first.first else resolveLikeTunnel(host, ordered, port, encrypted)?.first
                    host to (resolved?.let { HelpHostCheck.checkResolved(host, it, port) } ?: HostVerdict.Blocked)
                }
            }.awaitAll().toMap()
        }
        Log.i(TAG, "Проверка DNS $shownAddress (ответил ${answering ?: "DNS сети"}) ${if (port == null) "напрямую" else "через порт $port"}: $verdicts")
        return DnsCheckResult(answering ?: shownAddress, answerMs, verdicts, direct = port == null)
    }
}
