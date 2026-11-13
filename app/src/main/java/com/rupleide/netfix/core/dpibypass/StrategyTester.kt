package com.rupleide.netfix.core.dpibypass

import android.content.Context
import com.rupleide.netfix.core.compat.CompatibilityManager
import com.rupleide.netfix.core.debug.AppDebugManager as Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URL

class StrategyTester(private val context: Context) {
    companion object {
        private const val SNI_PLACEHOLDER = "{sni}"
        private const val SNI_IDENTITY_KEY = "youtube.com,googlevideo.com,ytimg.com,ggpht.com,google.com"

        private const val FAKE_SNI_DOMAIN = "google.com"

        private val legacyExpandedFakeSniFlags = listOf(
            "youtube.com",
            "music.youtube.com",
            "googlevideo.com",
            "ytimg.com",
            "ggpht.com",
            "google.com",
            "youtubei.googleapis.com",
            "googleapis.com"
        ).joinToString(" ") { "-n $it" }

        private const val PROXY_READY_TIMEOUT_MS = 1_500L
        private const val PROXY_READY_POLL_MS = 50L
        private const val PAUSE_BETWEEN_STRATEGIES_MS = 150L
        private const val OWNER_WAIT_ATTEMPTS = 20
        private const val OWNER_CHECK_MS = 120L
        private const val OWNER_RETRY_MS = 250L
        private const val QUICK_SITE_TIMEOUT_MS = 3_000
        private const val FULL_SITE_TIMEOUT_MS = 5_000
        private const val MAX_PARALLEL_SITE_CHECKS = 30
        private const val FINALIST_COUNT = 5
        private const val MAX_BODY_BYTES_WITHOUT_LENGTH = 1024L * 1024L
        private const val SCAN_PROXY_PORT = 1082
        const val FAILED_STATUS = "тайм-аут"

        private val QUICK_SITES = listOf(
            "youtube.com",
            "i.ytimg.com",
            "yt3.ggpht.com",
            "youtubei.googleapis.com",
            "manifest.googlevideo.com",
            "rr1---sn-4axm-n8vs.googlevideo.com",
            "rr4---sn-q4flrnsl.googlevideo.com",
            "rr2---sn-q4fl6ndl.googlevideo.com",
            "discord.com",
            "gateway.discord.gg",
            "cdn.discordapp.com"
        )

        val TEST_SITES = listOf(
            "youtu.be",
            "youtube.com",
            "i.ytimg.com",
            "i9.ytimg.com",
            "yt3.ggpht.com",
            "yt4.ggpht.com",
            "googleapis.com",
            "jnn-pa.googleapis.com",
            "googleusercontent.com",
            "signaler-pa.youtube.com",
            "youtubei.googleapis.com",
            "manifest.googlevideo.com",
            "yt3.googleusercontent.com",
            "rr1---sn-4axm-n8vs.googlevideo.com",
            "rr1---sn-gvnuxaxjvh-o8ge.googlevideo.com",
            "rr1---sn-ug5onuxaxjvh-p3ul.googlevideo.com",
            "rr1---sn-ug5onuxaxjvh-n8v6.googlevideo.com",
            "rr4---sn-q4flrnsl.googlevideo.com",
            "rr10---sn-gvnuxaxjvh-304z.googlevideo.com",
            "rr14---sn-n8v7kn7r.googlevideo.com",
            "rr16---sn-axq7sn76.googlevideo.com",
            "rr1---sn-8ph2xajvh-5xge.googlevideo.com",
            "rr1---sn-gvnuxaxjvh-5gie.googlevideo.com",
            "rr12---sn-gvnuxaxjvh-bvwz.googlevideo.com",
            "rr5---sn-n8v7knez.googlevideo.com",
            "rr1---sn-u5uuxaxjvhg0-ocje.googlevideo.com",
            "rr2---sn-q4fl6ndl.googlevideo.com",
            "rr5---sn-gvnuxaxjvh-n8vk.googlevideo.com",
            "rr4---sn-jvhnu5g-c35d.googlevideo.com",
            "rr1---sn-q4fl6n6y.googlevideo.com",
            "rr2---sn-hgn7ynek.googlevideo.com",
            "rr1---sn-xguxaxjvh-gufl.googlevideo.com",
            "discord.com",
            "gateway.discord.gg",
            "cdn.discordapp.com",
            "media.discordapp.net",
            "discordapp.com",
            "dl.discordapp.net"
        )

        val YOUTUBE_PROBE_HOSTS = listOf(
            "youtube.com",
            "i.ytimg.com",
            "yt3.ggpht.com",
            "youtubei.googleapis.com",
            "manifest.googlevideo.com",
            "rr1---sn-4axm-n8vs.googlevideo.com"
        )

        val DISCORD_PROBE_HOSTS = listOf(
            "discord.com",
            "gateway.discord.gg",
            "cdn.discordapp.com",
            "media.discordapp.net",
            "discordapp.com",
            "dl.discordapp.net"
        )

        private val passedSitesPattern = Regex("""^(\d+)/(\d+)$""")

        fun successScore(status: String): Double? {
            if (status.contains("мс")) return 1.0
            val match = passedSitesPattern.find(status.trim()) ?: return null
            val passedSites = match.groupValues[1].toIntOrNull() ?: return null
            val totalSites = match.groupValues[2].toIntOrNull() ?: return null
            if (passedSites == 0 || totalSites == 0) return null
            return passedSites.toDouble() / totalSites
        }

        fun isSuccessfulStatus(status: String): Boolean = successScore(status) != null

        fun normalizeStrategy(strategy: String): String =
            strategy.replace(SNI_PLACEHOLDER, SNI_IDENTITY_KEY)

        fun collapseFakeSniDomains(command: String): String =
            command
                .replace(legacyExpandedFakeSniFlags, "-n $SNI_PLACEHOLDER")
                .replace(SNI_IDENTITY_KEY, SNI_PLACEHOLDER)

        fun buildRunnableCommand(strategy: String): String =
            normalizeStrategy(strategy).replace(SNI_IDENTITY_KEY, FAKE_SNI_DOMAIN)

        val defaultStrategies = listOf(
            "-o1 -d1 -a1 -At,r,s -s1 -d1 -s5+s -s10+s -s15+s -s20+s -r1+s -S -a1 -As -s1 -d1 -s5+s -s10+s -s15+s -s20+s -S -a1",
            "-n {sni} -Qr -s1:3+sm -a1 -As -d1:3 -q7 -o2 -a1",
            "-f-200 -Qr -s3:5+sm -a1 -As -d1 -s4+sm -s8+sh -f-300 -d6+sh -a1 -At,r,s -o2 -f-30 -As -r5 -Mh -r6+sh -f-250 -s2:7+s -s3:6+sm -a1 -At,r,s -s3:5+sm -s6+s -s7:9+s -q30+sm -a1",
        "-d1 -d3+s -s6+s -d9+s -s12+s -d15+s -s20+s -d25+s -s30+s -d35+s -r1+s -S -a1 -As -d1 -d3+s -s6+s -d9+s -s12+s -d15+s -s20+s -d25+s -s30+s -d35+s -S -a1",
        "-q2 -s2 -s3+s -r3 -s4 -r4 -s5+s -r5+s -s6 -s7+s -r8 -s9+s -Qr -Mh,d,r -a1 -At,r -s2+s -r2 -d2 -s3 -r3 -r4 -s4 -d5+s -r5 -d6 -s7+s -d7 -a1",
        "-n {sni} -Qr -f-204 -s1:5+sm -a1 -As -d1 -s3+s -s5+s -q7 -a1 -As -o2 -f-43 -a1 -As -r5 -Mh -s1:5+s -s3:7+sm -a1",
        "-n {sni} -Qr -f-205 -a1 -As -s1:3+sm -a1 -As -s5:8+sm -a1 -As -d3 -q7 -o2 -f-43 -f-85 -f-165 -r5 -Mh -a1",
        "-d1+s -s50+s -a1 -As -f20 -r2+s -a1 -At -d2 -s1+s -s5+s -s10+s -s15+s -s25+s -s35+s -s50+s -s60+s -a1",
        "-o1 -a1 -At,r,s -f-1 -a1 -At,r,s -d1:11+sm -S -a1 -At,r,s -n {sni} -Qr -f1 -d1:11+sm -s1:11+sm -S -a1",
        "-d1 -s1 -q1 -a1 -Ar -s5 -o1+s -d3+s -s6+s -d9+s -s12+s -d15+s -s20+s -d25+s -s30+s -d35+s -a1",
        "-f1+nme -t6 -a1 -As -n {sni} -Qr -s1:6+sm -a1 -As -s5:12+sm -a1 -As -d3 -q7 -r6 -Mh -a1",
        "-d1 -s1+s -d3+s -s6+s -d9+s -s12+s -d15+s -s20+s -d25+s -s30+s -d35+s -a1",
        "-d1 -s1+s -d1+s -s3+s -d6+s -s12+s -d14+s -s20+s -d24+s -s30+s -a1",
        "-o1 -a1 -At,r,s -f-1 -a1 -Ar,s -o1 -a1 -At -r1+s -f-1 -t6 -a1",
        "-d1 -s1+s -s3+s -s6+s -s9+s -s12+s -s15+s -s20+s -s30+s -a1",
        "-d1 -d3+s -s6+s -d6+s -s7+s -d8+s -s10+s -a1 -t12 -At,s -r3",
        "-f1 -t5 -n {sni} -q3+h -Qr -f2 -q1 -r1+s -t15 -q1 -o2 -a1",
        "-n {sni} -d2:5:2+h -f-3 -r2+sm -o2 -o50+s -r2+s -f-4 -a1",
        "-f-1 -Qr -s1+sm -d3+s -s5+sm -o2 -a1 -As -r1+s -d8+s -a1",
        "-r-1+s -o20+sm -s3:7+sm -d5:3+sm -f300+s -Qr -f-1 -a1",
        "-o2 -O4 -s1 -q1 -a1 -Ar -s5 -o1+s -f1+s -r20+s -a1",
        "-o1 -r-5+se -a1 -At,r,s -d1 -n {sni} -Qr -f-1 -a1",
        "--fake -1 --ttl 8 --split 1+s --disorder 3+s -a1",
        "-n {sni} -Qr -f6+nr -d2 -d11 -f9+hm -o3 -t7 -a1",
        "-r5+s -s25+s -a1 -At,r,s -s50 -r5+s -s50+s -a1",
        "-d1 -d3+s -s6+s -d9+s -s20+s -d25+s -s30+s -a1",
        "-d9+s -q20+s -s25+s -t5 -a1 -At,r,s -r1+h -a1",
        "-q1+s -s29+s -s30+s -s14+s -o5+s -f-1 -S -a1",
        "-d1 -s1+s -r1+s -e1 -m1 -o1+s -f-1 -t2 -a1",
        "-d1 -o1 -a1 -Ar -o1 -a1 -At -f-1 -r1+s -a1",
        "-d1 -s4 -d8 -s1+s -d5+s -s10+s -d20+s -a1",
        "-f-1 -n {sni} -Qr -s2+s -r3 -o20 -t4 -a1",
        "-n {sni} -Qr -d5+sm -f3+sm -o2 -t4 -a1",
        "-o1 -a1 -Ar -q1 -a1 -At -f-1 -r1+s -a1",
        "-q1 -a1 -Ar -o1 -a1 -At -f-1 -r1+s -a1",
        "-s4+sn -r9+s -Qr -n {sni} -S -a1",
        "-o1 -d1 -r1+s -S -s1+s -d3+s -a1",
        "-q1+s -s29+s -o5+s -f-1 -S -a1",
        "-n {sni} -Qr -m2 -f-1 -d7 -a1",
        "-d1 -s1+s -r1+s -f-1 -t8 -a1",
        "-o1 -a1 -An -f1+nme -t6 -a1",
        "-n {sni} -Qr -f-1 -r1+s -a1",
        "-n {sni} -Qr -d1:3 -f-1 -a1",
        "-s1 -d3+s -a1 -At -r1+s -a1",
        "-f-1 -t8 -n {sni} -s1+s -a1",
        "-n {sni} -Qr -d1 -f-1 -a1",
        "-f64+se -n {sni} -t5 -a1",
        "-o1 -a1 -At,r,s -d1 -a1",
        "-d1+s -o2 -s5 -r5 -a1",
        "-r8 -o2 -s7 -q4+s -a1",
        "-o1 -f-1 -r-5+se -a1",
        "-d6+s -q4+hm -o2 -a1",
        "-s5+s -s35+s -m4 -a1",
        "-f-1+sm -t7 -m2 -a1",
        "-o1 -r-5+se -a1",
        "-o1+s -d3+s -a1",
        "-o1 -s4 -s6 -a1",
        "-q1 -r25+s -a1",
        "-d1 -s3+s -a1",
        "-o3 -d7 -a1",
        "-d7 -s2 -a1",
        "-s1 -d1 -a1 -Ar -d5 -o1+s -a1 -At -f-1 -r1+s -a1 -As -d1 -o1+s -s-1 -a1",
        "-s1 -q1 -a1 -Ar -s5 -o1+s -a1 -At -f-1 -d1+s -a1 -As -s1 -o1+s -s-1 -a1",
        "-s1 -q1 -a1 -Ar -s5 -o2 -a1 -At -f-1 -r1+s -a1 -As -s1 -o1+s -s-1 -a1",
        "-s1 -d1 -r1+s -a1 -Ar -o1 -a1 -At -f-1 -r1+s -a1",
        "-s1 -q1 -r1+s -a1 -Ar -o1 -a1 -At -f-1 -r1+s -a1",
        "-s1 -o1 -r1+s -a1 -Ar -o1 -a1 -At -f-1 -r1+s -a1",
        "-s1 -d1 -o1 -a1 -Ar -o3 -a1 -At -f-1 -r1+s -a1",
        "-s1 -o1 -a1 -Ar -o1 -a1 -At -f-1 -r1+s -a1",
        "-s1 -q1 -a1 -At,r,s -f-1 -r1+s -a1",
        "-s1 -o1 -a1 -At,r,s -f-1 -r1+s -a1",
        "-s1 -d1 -a1 -At,r,s -f-1 -r1+s -a1",
        "-s1+s -d3+s -a1",
        DISCORD_COMBINED_STRATEGY
    )

        const val DISCORD_COMBINED_STRATEGY =
            "-o1 -r-5+se -a1 -At,r,s -q2 -s2 -s3+s -r3 -s4 -r4 -s5+s -r5+s -s6 -s7+s -r8 -s9+s -Qr -Mh,d,r -a1"

        val YOUTUBE_VIDEO_STRATEGIES = listOf(
            "-H :redirector.googlevideo.com -r5+s -s50+s -a1 -An -H :googlevideo.com -q5+s -s25+s -a1 -An -r5+s -s50+s -a1",
            "-H :googlevideo.com -d5+s -d25+s -An -r5+s -s25+s -d1 -a1",
            "-H :googlevideo.com -d5+s -d25+s -An -q5+s -s25+s -a1",
            "-H :googlevideo.com -d5+s -d25+s -An -d1 -d3+s -s6+s -d9+s -s12+s -a1",
            "-q5+s -s25+s -a1",
            "-r5+s -s25+s -d1 -a1",
            "-r5+s -s25+s -o1 -a1",
            "-r5+s -s50+s -a1"
        )

        fun scanStrategies(prefs: android.content.SharedPreferences): List<String> {
            val base = if (CompatibilityManager.discordCombinedStrategyEnabled(prefs)) {
                defaultStrategies
            } else {
                defaultStrategies - DISCORD_COMBINED_STRATEGY
            }
            return if (CompatibilityManager.youtubeVideoStrategiesEnabled(prefs)) YOUTUBE_VIDEO_STRATEGIES + base else base
        }

        const val ECH_PROBE_HOST = "cloudflare-ech.com"

        const val KEY_SCAN_CHECK_DISCORD = "scan_check_discord"

        fun discordCheckEnabled(prefs: android.content.SharedPreferences): Boolean {
            val skipOnTvByDefault = prefs.getBoolean("is_smart_tv", false) && CompatibilityManager.discordOffOnTvEnabled(prefs)
            return prefs.getBoolean(KEY_SCAN_CHECK_DISCORD, !skipOnTvByDefault)
        }

        private fun isDiscordHost(host: String): Boolean = "discord" in host

        fun quickSitesFor(includeEch: Boolean, includeDiscord: Boolean = true): List<String> = when {
            !includeDiscord -> QUICK_SITES.filterNot(::isDiscordHost)
            includeEch -> QUICK_SITES + ECH_PROBE_HOST
            else -> QUICK_SITES
        }

        val SMARTTUBE_HOSTS = listOf(
            "clients1.google.com",
            "oauth2.googleapis.com"
        )

        fun testSitesFor(includeEch: Boolean, includeDiscord: Boolean = true, includeSmartTube: Boolean = false): List<String> {
            val base = when {
                !includeDiscord -> TEST_SITES.filterNot(::isDiscordHost)
                includeEch -> TEST_SITES + ECH_PROBE_HOST
                else -> TEST_SITES
            }
            return if (includeSmartTube) base + SMARTTUBE_HOSTS.filterNot { it in base } else base
        }

        fun discordProbeHostsFor(includeEch: Boolean, includeDiscord: Boolean = true): List<String> = when {
            !includeDiscord -> emptyList()
            includeEch -> DISCORD_PROBE_HOSTS + ECH_PROBE_HOST
            else -> DISCORD_PROBE_HOSTS
        }
    }

    private fun includeEchHost(): Boolean =
        CompatibilityManager.discordEchCheckEnabled(context.getPreferences())

    private fun includeDiscordHosts(): Boolean =
        discordCheckEnabled(context.getPreferences())

    private fun includeSmartTubeHosts(): Boolean =
        CompatibilityManager.smartTubeHostsEnabled(context.getPreferences()) &&
            com.rupleide.netfix.core.update.UpdateManager.isSmartTubeInstalled(context)

    suspend fun runTests(
        strategies: List<String> = defaultStrategies,
        shouldStopQuickPhase: () -> Boolean = { false },
        onFinalPhase: (finalistCount: Int) -> Unit = {},
        onProgress: (index: Int, strategy: String, status: String, isFinalCheck: Boolean) -> Unit
    ): String? = withContext(Dispatchers.IO) {
        val wasRunning = com.rupleide.netfix.data.appStatus.first == com.rupleide.netfix.data.AppStatus.Running
        if (wasRunning) {
            ServiceManager.stop(context)
            var attempts = 0
            while (com.rupleide.netfix.data.appStatus.first != com.rupleide.netfix.data.AppStatus.Halted && attempts < 30) {
                delay(100)
                attempts++
            }
        }

        val includeEch = includeEchHost()
        val includeDiscord = includeDiscordHosts()
        val quickSites = quickSitesFor(includeEch, includeDiscord)
        val includeSmartTube = includeSmartTubeHosts()
        val testSites = testSitesFor(includeEch, includeDiscord, includeSmartTube)
        Log.i(
            "StrategyTester",
            "Хосты проверки: быстрых ${quickSites.size}, полных ${testSites.size}, SmartTube " +
                (if (includeSmartTube) "проверяется" else "не проверяется") + ", Discord " +
                (if (includeDiscord) "проверяется" else "не проверяется") + ", ECH " +
                if (includeDiscord && includeEch) "включён ($ECH_PROBE_HOST)" else "выключен"
        )
        val quickPassedSites = IntArray(strategies.size)
        for (i in strategies.indices) {
            if (!isActive || shouldStopQuickPhase()) break
            val strategy = strategies[i]
            Log.i("StrategyTester", "Быстрая проверка стратегии №${i + 1}: \"$strategy\"")
            val passedSites = runWithStrategy(strategy, 0) { port ->
                countFullyReachableSites(quickSites, port, QUICK_SITE_TIMEOUT_MS)
            }
            quickPassedSites[i] = passedSites
            val statusText = if (passedSites > 0) "$passedSites/${quickSites.size}" else FAILED_STATUS
            Log.i("StrategyTester", "Быстрый результат стратегии №${i + 1}: $statusText")
            onProgress(i, strategy, statusText, false)
        }

        val stoppedEarly = shouldStopQuickPhase()
        val finalistIndices = if (stoppedEarly) {
            emptyList()
        } else {
            strategies.indices
                .filter { quickPassedSites[it] > 0 }
                .sortedByDescending { quickPassedSites[it] }
                .take(FINALIST_COUNT)
        }

        var bestStrategy: String? = null
        var bestScore = 0.0
        val streamProbeOn = CompatibilityManager.streamProbeEnabled(context.getPreferences())
        if (finalistIndices.isNotEmpty()) onFinalPhase(finalistIndices.size)
        for (index in finalistIndices) {
            if (!isActive || shouldStopQuickPhase()) break
            val strategy = strategies[index]
            Log.i("StrategyTester", "Полная проверка стратегии №${index + 1}")
            val (passedSites, stream) = runWithStrategy(strategy, 0 to null as StreamProbeResult?) { port ->
                val passed = countFullyReachableSites(testSites, port, FULL_SITE_TIMEOUT_MS)
                val streamResult = if (streamProbeOn && passed > 0) probeVideoStream(port) else null
                passed to streamResult
            }
            val statusText = if (passedSites > 0) "$passedSites/${testSites.size}" else FAILED_STATUS
            Log.i("StrategyTester", "Итог стратегии №${index + 1}: $statusText")
            stream?.let { Log.i("StrategyTester", "Видеопоток стратегии №${index + 1}: ${describeStream(it)}") }
            val shownStatus = if (passedSites > 0) statusText else "${quickPassedSites[index]}/${quickSites.size}"
            onProgress(index, strategy, shownStatus, true)
            val score = if (streamProbeOn) StreamChunkProbe.score(passedSites, stream) else passedSites.toDouble()
            if (passedSites > 0 && score > bestScore) {
                bestScore = score
                bestStrategy = strategy
            }
        }

        val allFinalistsFailed = finalistIndices.isNotEmpty() && bestStrategy == null &&
            CompatibilityManager.scanQuickFallbackEnabled(context.getPreferences())
        if (bestStrategy == null && (shouldStopQuickPhase() || allFinalistsFailed)) {
            val bestQuickIndex = strategies.indices
                .filter { quickPassedSites[it] > 0 }
                .maxByOrNull { quickPassedSites[it] }
            bestStrategy = bestQuickIndex?.let { strategies[it] }
            if (allFinalistsFailed) {
                Log.w("StrategyTester", "Полная проверка не прошла ни у одного финалиста, беру лучший быстрый результат: №${bestQuickIndex?.plus(1)}")
            }
        }

        bestStrategy
    }

    private suspend fun <T> runWithStrategy(strategy: String, fallback: T, check: suspend (Int) -> T): T = coroutineScope {
        val testPort = if (CompatibilityManager.freeProxyPortEnabled(context.getPreferences())) {
            ProxyPortResolver.resolve("127.0.0.1", SCAN_PROXY_PORT)
        } else {
            SCAN_PROXY_PORT
        }
        val cleanCmd = buildRunnableCommand(strategy)
        diagnosticStrategy = cleanCmd
        val prefix = "--ip 127.0.0.1 --port $testPort "
        val splitArgs = shellSplit("$prefix$cleanCmd").filter { it !in setOf("--help", "--version", "-h", "-v") }
        val args = (listOf("ciadpi") + splitArgs + CloudflareUnfreeze.extraArgs(context)).toTypedArray()

        val proxy = ByeDpiProxy()
        var startCode: Int? = null
        var proxyJob = launch(Dispatchers.IO) {
            startCode = proxy.startProxy(ByeDpiProxyCmdPreferences(args))
        }
        var ownerWaits = 0
        while (ownerWaits < OWNER_WAIT_ATTEMPTS) {
            delay(OWNER_CHECK_MS)
            if (proxyJob.isActive || startCode != ByeDpiProxy.BUSY) break
            ownerWaits += 1
            delay(OWNER_RETRY_MS)
            startCode = null
            proxyJob = launch(Dispatchers.IO) {
                startCode = proxy.startProxy(ByeDpiProxyCmdPreferences(args))
            }
        }
        val ownsProxy = proxyJob.isActive

        try {
            if (ownsProxy && waitForProxyPort(testPort)) check(testPort) else fallback
        } finally {
            withContext(NonCancellable) {
                proxy.stopProxy()
                proxyJob.join()
                delay(PAUSE_BETWEEN_STRATEGIES_MS)
            }
        }
    }

    data class StrategyProbeResult(
        val youtubeOk: Int,
        val youtubeTotal: Int,
        val discordOk: Int,
        val discordTotal: Int,
        val averageMs: Long
    )

    suspend fun stopRunningVpnForTests(): Boolean = withContext(Dispatchers.IO) {
        val wasRunning = com.rupleide.netfix.data.appStatus.first == com.rupleide.netfix.data.AppStatus.Running
        if (wasRunning) {
            ServiceManager.stop(context)
            var attempts = 0
            while (com.rupleide.netfix.data.appStatus.first != com.rupleide.netfix.data.AppStatus.Halted && attempts < 30) {
                delay(100)
                attempts++
            }
        }
        wasRunning
    }

    suspend fun probeStrategy(strategy: String): StrategyProbeResult = withContext(Dispatchers.IO) {
        val discordHosts = discordProbeHostsFor(includeEchHost(), includeDiscordHosts())
        val empty = StrategyProbeResult(0, YOUTUBE_PROBE_HOSTS.size, 0, discordHosts.size, -1L)
        runWithStrategy(strategy, empty) { port ->
            coroutineScope {
                val hosts = YOUTUBE_PROBE_HOSTS + discordHosts
                val times = hosts.map { host -> async(Dispatchers.IO) { timedReachability(host, port) } }.awaitAll()
                val youtubeTimes = times.take(YOUTUBE_PROBE_HOSTS.size)
                val discordTimes = times.drop(YOUTUBE_PROBE_HOSTS.size)
                val okTimes = (youtubeTimes + discordTimes).filter { it >= 0 }
                StrategyProbeResult(
                    youtubeOk = youtubeTimes.count { it >= 0 },
                    youtubeTotal = youtubeTimes.size,
                    discordOk = discordTimes.count { it >= 0 },
                    discordTotal = discordTimes.size,
                    averageMs = if (okTimes.isEmpty()) -1L else okTimes.average().toLong()
                )
            }
        }
    }

    suspend fun <T> withStrategyPort(strategy: String, fallback: T, block: suspend (Int) -> T): T = withContext(Dispatchers.IO) {
        runWithStrategy(strategy, fallback, block)
    }

    suspend fun probeHosts(strategy: String, hosts: List<String>): Map<String, Boolean> = withContext(Dispatchers.IO) {
        runWithStrategy(strategy, hosts.associateWith { false }) { port ->
            coroutineScope {
                hosts.map { host -> async(Dispatchers.IO) { host to isSiteFullyReachable(host, port, FULL_SITE_TIMEOUT_MS) } }
                    .awaitAll()
                    .toMap()
            }
        }
    }

    private fun probeVideoStream(port: Int): StreamProbeResult =
        StreamChunkProbe.probe(
            Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", port)),
            FULL_SITE_TIMEOUT_MS,
            register = { activeConnections.add(it) },
            unregister = { activeConnections.remove(it) }
        )

    private fun describeStream(stream: StreamProbeResult): String =
        if (stream.loaded) {
            "${stream.bytes / 1024} КБ, первый байт ${stream.firstByteMs} мс, ${stream.speedKbps} КБ/с"
        } else {
            "не загрузился (${stream.failure})"
        }

    private fun timedReachability(host: String, port: Int, timeoutMs: Int = FULL_SITE_TIMEOUT_MS): Long {
        val started = System.currentTimeMillis()
        return if (isSiteFullyReachable(host, port, timeoutMs)) System.currentTimeMillis() - started else -1L
    }

    private suspend fun waitForProxyPort(port: Int): Boolean {
        val deadline = System.currentTimeMillis() + PROXY_READY_TIMEOUT_MS
        while (System.currentTimeMillis() < deadline) {
            val isListening = try {
                java.net.Socket().use { socket ->
                    socket.connect(InetSocketAddress("127.0.0.1", port), 100)
                    true
                }
            } catch (_: Exception) {
                false
            }
            if (isListening) return true
            delay(PROXY_READY_POLL_MS)
        }
        return false
    }

    private suspend fun countFullyReachableSites(sites: List<String>, proxyPort: Int, timeoutMs: Int): Int = coroutineScope {
        val parallelChecks = Semaphore(MAX_PARALLEL_SITE_CHECKS)
        sites.map { site ->
            async(Dispatchers.IO) {
                parallelChecks.withPermit { isSiteFullyReachable(site, proxyPort, timeoutMs) }
            }
        }.awaitAll().count { it }
    }

    private val activeConnections: MutableSet<HttpURLConnection> =
        java.util.Collections.newSetFromMap(java.util.concurrent.ConcurrentHashMap<HttpURLConnection, Boolean>())

    fun abortActiveChecks() {
        activeConnections.toList().forEach { connection ->
            try {
                connection.disconnect()
            } catch (_: Exception) {
            }
        }
    }

    @Volatile
    private var diagnosticStrategy: String = ""

    private fun reportCheckFailure(site: String, reason: ScanFailureReason, detail: String) {
        if (reason == ScanFailureReason.CHECK_ABORTED) return
        Log.d("StrategyTester", ScanFailureReason.formatLogLine(site, reason, detail, diagnosticStrategy))
    }

    private fun isSiteFullyReachable(site: String, proxyPort: Int, timeoutMs: Int): Boolean {
        val socksProxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", proxyPort))
        var connection: HttpURLConnection? = null
        var responseCode = -1
        return try {
            connection = URL("https://$site").openConnection(socksProxy) as HttpURLConnection
            activeConnections.add(connection)
            connection.connectTimeout = timeoutMs
            connection.readTimeout = timeoutMs
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("Connection", "close")

            responseCode = connection.responseCode
            val declaredLength = connection.contentLengthLong
            val readLimit = if (declaredLength > 0) declaredLength else MAX_BODY_BYTES_WITHOUT_LENGTH
            var receivedLength = 0L
            var bodyError: java.io.IOException? = null
            try {
                val body = if (responseCode in 200..299) connection.inputStream else connection.errorStream
                if (body != null) {
                    val buffer = ByteArray(8192)
                    while (receivedLength < readLimit) {
                        val bytesToRead = minOf(buffer.size.toLong(), readLimit - receivedLength).toInt()
                        val bytesRead = body.read(buffer, 0, bytesToRead)
                        if (bytesRead == -1) break
                        receivedLength += bytesRead
                    }
                }
            } catch (e: java.io.IOException) {
                bodyError = e
            }
            val complete = declaredLength <= 0L || receivedLength >= declaredLength
            if (!complete) {
                reportCheckFailure(
                    site,
                    ScanFailureReason.forIncompleteResponse(responseCode, bodyError),
                    "HTTP $responseCode, получено $receivedLength из $declaredLength байт" + (bodyError?.let { ", ${it}" } ?: "")
                )
            }
            complete
        } catch (e: Exception) {
            reportCheckFailure(site, ScanFailureReason.fromThrowable(e), e.toString())
            false
        } finally {
            connection?.let { activeConnections.remove(it) }
            connection?.disconnect()
        }
    }
}
