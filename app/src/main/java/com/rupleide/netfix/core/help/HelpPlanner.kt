package com.rupleide.netfix.core.help

import com.rupleide.netfix.core.compat.TunnelDnsMode
import com.rupleide.netfix.core.dnscheck.DnsCheckResult
import com.rupleide.netfix.core.dnscheck.TunnelDnsCheck
import com.rupleide.netfix.core.dpibypass.AppListMode
import com.rupleide.netfix.core.dpibypass.NetworkKind

object HelpPlanner {

    fun fixes(symptom: HelpSymptom, facts: HelpFacts): List<HelpFix> {
        val plan = LinkedHashSet<HelpFix>()
        fun add(fix: HelpFix, condition: Boolean = true) {
            if (condition) plan.add(fix)
        }
        val selectedOnly = facts.appListMode == AppListMode.SelectedAppsOnly
        val usesBypass = symptom !in setOf(HelpSymptom.TelegramNoConnect, HelpSymptom.TelegramDrops, HelpSymptom.Other, HelpSymptom.BrokeApp)
        val usesTg = symptom in setOf(HelpSymptom.TelegramNoConnect, HelpSymptom.TelegramDrops)
        val privateDnsBroken = facts.privateDnsActive && !facts.systemDnsWorks

        if (symptom != HelpSymptom.Other && !facts.internetDirect) {
            add(HelpFix.DisablePrivateDns, privateDnsBroken)
            add(HelpFix.AdviceNoInternet, !privateDnsBroken)
            return plan.toList()
        }
        add(HelpFix.DisableOtherVpn, facts.otherVpnActive && symptom != HelpSymptom.Other && symptom != HelpSymptom.BrokeApp)
        add(HelpFix.DisablePrivateDns, privateDnsBroken && symptom != HelpSymptom.Other)
        add(HelpFix.ReconnectNetwork, facts.networkDnsSpoofed && symptom in spoofSensitive)
        add(HelpFix.RequestVpnPermission, facts.vpnPermissionMissing && usesBypass)
        add(HelpFix.StartBypass, usesBypass && !facts.bypassRunning && !facts.vpnPermissionMissing && symptom != HelpSymptom.ScanZero)
        add(HelpFix.StartTgProxy, usesTg && !facts.tgProxyRunning)

        when (symptom) {
            HelpSymptom.YoutubeDown -> {
                add(HelpFix.AddAppToBypass, !facts.youtubeInBypass && selectedOnly)
                add(HelpFix.UseNextStrategy)
                if (!facts.privateDnsActive) dnsChain(facts).forEach { add(it) }
                add(HelpFix.NetworkAssistant, facts.networkAssistantRisk)
                add(HelpFix.RestartYoutube, !facts.isTv)
                add(HelpFix.SmartTubeConscrypt, facts.isTv && facts.smartTubeInstalled)
                add(HelpFix.AdviceVpn)
            }
            HelpSymptom.YoutubeSlow -> {
                add(HelpFix.UseFasterVideoStrategy)
                add(HelpFix.NetworkAssistant, facts.networkAssistantRisk)
                add(HelpFix.AdviceVpn)
            }
            HelpSymptom.YoutubeFromDesktop -> {
                add(HelpFix.AddAppToBypass, !facts.youtubeInBypass && selectedOnly)
                add(HelpFix.RestartYoutube)
                add(HelpFix.NetworkAssistant, facts.networkAssistantRisk)
            }
            HelpSymptom.YoutubeBrowser -> {
                add(HelpFix.AddBrowserToBypass, facts.browserPackage != null && !facts.browserInBypass && selectedOnly)
                add(HelpFix.AllAppsMode, facts.browserPackage == null && selectedOnly)
                add(HelpFix.UseNextStrategy)
                if (!facts.privateDnsActive) dnsChain(facts).forEach { add(it) }
                add(HelpFix.AdviceVpn)
            }
            HelpSymptom.TelegramNoConnect -> {
                add(HelpFix.TgReconnect)
                add(HelpFix.FreePortOn, !facts.freePortOn)
                add(HelpFix.CheckCfServers, facts.tgCfEnabled)
                add(HelpFix.TgCfPriorityOff, facts.tgCfPriority)
                add(HelpFix.TgWatchdogOn, !facts.watchdogOn)
            }
            HelpSymptom.TelegramDrops -> {
                add(HelpFix.CheckCfServers, facts.tgCfEnabled && facts.tgCfPinned != null)
                add(HelpFix.TgWatchdogOn, !facts.watchdogOn)
                add(HelpFix.TgCfPriorityOff, facts.tgCfPriority)
                add(HelpFix.TgStableOn, !facts.tgStable)
                add(HelpFix.BatteryUnrestricted, facts.batteryRestricted)
                add(HelpFix.VendorAutostart)
                add(HelpFix.NetworkAssistant, facts.networkAssistantRisk)
                add(HelpFix.CheckCfServers, facts.tgCfEnabled)
            }
            HelpSymptom.Discord -> {
                add(HelpFix.AddAppToBypass, facts.targetPackage != null && !facts.targetInBypass && selectedOnly)
                add(HelpFix.UseDiscordStrategy)
                add(HelpFix.UseAnotherDiscordStrategy)
                add(HelpFix.CloudflareUnfreeze, !facts.cloudflareFreezeRuledOut)
                dnsChain(facts).forEach { add(it) }
                add(HelpFix.AdviceVpn)
            }
            HelpSymptom.OtherApp -> {
                add(HelpFix.AddAppToBypass, facts.targetPackage != null && !facts.targetInBypass && selectedOnly)
                add(HelpFix.EnableSecureDns, !facts.secureDnsOn && facts.targetPackage in smartDnsApps)
                add(HelpFix.CloudflareUnfreeze, facts.targetPackage in cloudflareApps && !facts.cloudflareFreezeRuledOut)
                dnsChain(facts).forEach { add(it) }
                add(HelpFix.AdviceVpn)
            }
            HelpSymptom.BrokeApp -> if (facts.targetRegionBlocked) {
                add(HelpFix.AddAppToBypass, facts.targetPackage != null && !facts.targetInBypass && selectedOnly)
                dnsChain(facts).forEach { add(it) }
            } else {
                val notAffected = selectedOnly && !facts.targetInBypass
                add(HelpFix.AdviceVpnDetected, notAffected)
                add(HelpFix.RemoveAppFromBypass, facts.targetInBypass && selectedOnly)
                add(HelpFix.SelectedAppsMode, facts.appListMode == AppListMode.AllTraffic)
                if (!notAffected && !facts.privateDnsActive) add(HelpFix.TunnelDnsBest)
            }
            HelpSymptom.WontStart -> {
                add(HelpFix.FreePortOn, !facts.freePortOn || facts.dpiPortBusy)
                add(HelpFix.StartBypass, !facts.bypassRunning && !facts.vpnPermissionMissing)
                add(HelpFix.ResetSettings)
            }
            HelpSymptom.WorseAfterUpdate -> {
                add(HelpFix.NetworkPrevious, facts.previousVersion != null)
            }
            HelpSymptom.Tv -> {
                add(HelpFix.AddAppToBypass, facts.smartTubeInstalled && !facts.smartTubeInBypass && selectedOnly)
                add(HelpFix.DnsSystem, facts.tunnelDns != TunnelDnsMode.System && facts.onEthernet && !facts.privateDnsActive)
                add(HelpFix.UseNextStrategy)
                if (!facts.privateDnsActive) dnsChain(facts).forEach { add(it) }
                add(HelpFix.SmartTubeConscrypt, facts.smartTubeInstalled)
                add(HelpFix.AdviceSmartTube, facts.smartTubeInstalled && facts.oldAndroid)
            }
            HelpSymptom.ScanZero -> {
                add(HelpFix.NetworkLatest, !facts.behaviorLatest)
                add(HelpFix.RescanNetwork, plan.isNotEmpty())
                add(HelpFix.ResetSettings)
                add(HelpFix.AdviceVpn)
            }
            HelpSymptom.StopsInBackground -> {
                add(HelpFix.BatteryUnrestricted, facts.batteryRestricted)
                add(HelpFix.VendorAutostart)
                add(HelpFix.NetworkAssistant, facts.networkAssistantRisk)
                add(HelpFix.TgStableOn, !facts.tgStable && facts.tgWanted)
            }
            HelpSymptom.Other -> Unit
        }
        return plan.toList()
    }

    fun dnsChain(facts: HelpFacts): List<HelpFix> = buildList {
        if (facts.privateDnsActive) add(HelpFix.DisablePrivateDns)
        if (!facts.secureDnsOn) add(HelpFix.EnableSecureDns)
        add(HelpFix.TunnelDnsBest)
        if (!facts.isTv) add(HelpFix.TunnelDnsNext)
    }

    fun changesTunnelDns(fix: HelpFix): Boolean = fix.changesTunnelDns

    fun dnsTargetFor(symptom: HelpSymptom, packageName: String?): String? = when (symptom) {
        HelpSymptom.YoutubeDown, HelpSymptom.YoutubeBrowser, HelpSymptom.Tv -> "YouTube"
        HelpSymptom.Discord -> "Discord"
        else -> when (packageName) {
            "com.instagram.android", "com.instagram.barcelona" -> "Instagram"
            "com.openai.chatgpt" -> "ChatGPT"
            "com.google.android.apps.bard" -> "Gemini"
            "com.spotify.music" -> "Spotify"
            "com.twitter.android" -> "X (Twitter)"
            DISCORD_PACKAGE -> "Discord"
            YOUTUBE_PACKAGE -> "YouTube"
            else -> null
        }
    }

    data class DnsRank(val key: String, val targetOk: Boolean, val okTargets: Int, val answerMs: Long)

    fun rankDns(results: Map<String, DnsCheckResult>, target: String?): List<DnsRank> {
        val checkTarget = TunnelDnsCheck.targets.firstOrNull { it.label == target }
        return results
            .filterValues { it.verdicts.isNotEmpty() && it.okTargets > 0 }
            .map { (key, result) ->
                DnsRank(
                    key = key,
                    targetOk = checkTarget == null || result.verdictFor(checkTarget) == HostVerdict.Ok,
                    okTargets = result.okTargets,
                    answerMs = result.answerMs.takeIf { it >= 0 } ?: Long.MAX_VALUE
                )
            }
            .sortedWith(compareByDescending<DnsRank> { it.targetOk }.thenByDescending { it.okTargets }.thenBy { it.answerMs })
    }

    fun pickDns(ranking: List<DnsRank>, currentKey: String?, tried: Set<String>, atLeastAsGoodAsCurrent: Boolean): DnsRank? {
        val current = ranking.firstOrNull { it.key == currentKey }
        val candidate = ranking.firstOrNull { it.key != currentKey && it.key !in tried && it.targetOk } ?: return null
        if (!atLeastAsGoodAsCurrent || current == null || !current.targetOk) return candidate
        return candidate.takeIf { it.okTargets >= current.okTargets }
    }

    val spoofSensitive: Set<HelpSymptom> = setOf(
        HelpSymptom.ScanZero,
        HelpSymptom.YoutubeDown,
        HelpSymptom.Discord,
        HelpSymptom.OtherApp,
        HelpSymptom.Tv
    )

    val doneWithoutQuestion: Set<HelpSymptom> = setOf(HelpSymptom.WontStart, HelpSymptom.ScanZero)

    fun detailFor(fix: HelpFix, facts: HelpFacts?): String? = when {
        fix == HelpFix.AdviceVpn && facts?.targetPackage == DISCORD_PACKAGE && facts.networkKind == NetworkKind.Mobile ->
            "Оператор обрывает загрузку с серверов Discord, которые работают через Cloudflare. Способы и DNS этого не лечат. На Wi-Fi Discord может работать. Напишите в поддержку, разберёмся вместе."
        fix == HelpFix.UseDiscordStrategy && facts?.discordBestLabel != null ->
            "Лучше всего Discord работает на ${facts.discordBestLabel}${facts.discordCurrentLabel?.let { ", у текущего $it" } ?: ""}. Включу его."
        else -> null
    }

    fun findings(symptom: HelpSymptom, facts: HelpFacts): List<HelpFinding> {
        val list = mutableListOf<HelpFinding>()
        fun ok(text: String) = list.add(HelpFinding(HelpLevel.Ok, text))
        fun warn(text: String) = list.add(HelpFinding(HelpLevel.Warn, text))
        fun bad(text: String) = list.add(HelpFinding(HelpLevel.Bad, text))

        if (facts.betaExpired) bad("Срок тестовой версии закончился")
        if (!facts.internetDirect) bad("Интернета нет даже без NetFix, проверьте Wi-Fi или мобильную сеть") else ok("Интернет есть")
        val network = when {
            facts.onEthernet -> "кабель"
            facts.networkKind == NetworkKind.Mobile -> "мобильная сеть"
            facts.networkKind == NetworkKind.Wifi -> "Wi-Fi"
            else -> "не определена"
        }
        ok("Сеть: $network")
        if (facts.otherVpnActive) bad("Включён другой VPN, с ним NetFix не работает")
        if (facts.privateDnsActive) {
            val host = facts.privateDnsHost?.let { " $it" } ?: ""
            val tunnelDnsMatters = symptom == HelpSymptom.OtherApp || symptom == HelpSymptom.Discord
            if (!facts.systemDnsWorks) bad("Частный DNS$host не отвечает, из-за него не открываются сайты")
            else if (tunnelDnsMatters) warn("Включён частный DNS$host, DNS в NetFix пока не работает")
            else ok("Частный DNS$host работает")
        }
        if (facts.networkDnsSpoofed) bad("Сеть отдаёт неправильные адреса сайтов, способы так не проверить")
        if (facts.networkAssistantRisk) warn("Включены Wi-Fi и мобильная сеть, «Сетевой ассистент» может уводить трафик мимо NetFix")
        if (facts.batteryRestricted && symptom in setOf(HelpSymptom.TelegramDrops, HelpSymptom.StopsInBackground)) warn("Телефон может усыплять NetFix в фоне")

        val tgSymptom = symptom == HelpSymptom.TelegramNoConnect || symptom == HelpSymptom.TelegramDrops
        if (tgSymptom) {
            if (!facts.tgProxyRunning) bad("Прокси Telegram выключен")
            else if (!facts.tgPortOpen) bad("Прокси Telegram запущен, но порт не отвечает")
            else ok("Прокси Telegram работает")
            facts.tgPortMovedFrom?.let { warn("Порт прокси сменился с $it, в Telegram мог остаться старый") }
            when (facts.tgTrafficFlowing) {
                false -> warn("Через прокси сейчас не идёт трафик")
                true -> ok("Через прокси идёт трафик")
                null -> Unit
            }
            ok("Приоритет Cloudflare: ${if (facts.tgCfPriority) "включён" else "выключен"}")
            ok("Сервер Cloudflare: ${facts.tgCfPinned?.let { "закреплён $it" } ?: "выбирается автоматически"}")
            ok("«Чинить зависший Telegram»: ${if (facts.watchdogOn) "включено" else "выключено"}")
        } else if (symptom != HelpSymptom.Other) {
            if (facts.vpnPermissionMissing) bad("У NetFix нет разрешения на VPN")
            if (!facts.bypassRunning) bad("NetFix выключен") else ok("NetFix включён")
            if (facts.dpiPortBusy) warn("Порт NetFix занят другим приложением")
            ok("Через NetFix идут: ${if (facts.appListMode == AppListMode.AllTraffic) "все приложения" else "только выбранные приложения"}")
            ok("DNS: ${dnsLabel(facts.tunnelDns)}")
            if (facts.secureDnsOn) ok("Защищённый DNS включён") else warn("Защищённый DNS выключен, оператор может подменить адреса сайтов")
            ok("Работа сети: как в ${facts.behaviorVersion}${if (facts.behaviorLatest) " (новая)" else ""}")
            if (!facts.hasScanForNetwork) warn("Для этой сети ещё не подбирали способ")
            facts.currentProbe?.let { probe ->
                when {
                    probe.youtubeOk == 0 && probe.discordOk == 0 -> bad("Текущий способ обхода не работает: ${probe.describe}")
                    facts.currentProbeYoutubeWeak || facts.currentProbeDiscordWeak -> warn("Текущий способ обхода работает частично: ${probe.describe}")
                    else -> ok("Текущий способ обхода работает: ${probe.describe}")
                }
            }
        }
        if (symptom in setOf(HelpSymptom.YoutubeDown, HelpSymptom.YoutubeFromDesktop) && !facts.youtubeInBypass && facts.appListMode == AppListMode.SelectedAppsOnly) {
            bad("YouTube не идёт через NetFix")
        }
        val target = facts.targetLabel
        if (target != null) {
            if (symptom != HelpSymptom.BrokeApp && facts.appListMode == AppListMode.SelectedAppsOnly) {
                if (facts.targetInBypass) ok("$target идёт через NetFix") else bad("$target не идёт через NetFix")
            }
            if (symptom == HelpSymptom.BrokeApp && facts.targetInBypass) warn("$target идёт через NetFix")
            if (facts.targetHostsDirect.isNotEmpty()) {
                if (facts.targetWorksDirect) ok("Сервера $target открываются и без NetFix")
                else warn("Без NetFix не открываются: ${facts.targetHostsDirect.filterValues { !it }.keys.joinToString { it.substringBefore('/') }}")
            }
            if (facts.targetHostsViaBypass.isNotEmpty()) {
                val failed = facts.targetHostsViaBypass.filterValues { !it }.keys
                if (failed.isEmpty()) ok("Через NetFix сервера $target открываются") else warn("Через NetFix не открываются: ${failed.joinToString { it.substringBefore('/') }}")
            }
        }
        if (target != null && facts.targetRegionBlocked) bad("$target сам не пускает пользователей из России, попробуем другой DNS")
        if (symptom == HelpSymptom.YoutubeBrowser && facts.browserPackage != null && facts.appListMode == AppListMode.SelectedAppsOnly) {
            if (facts.browserInBypass) ok("Браузер идёт через NetFix") else bad("Браузер не идёт через NetFix")
        }
        if (symptom == HelpSymptom.Tv) {
            if (facts.smartTubeInstalled) {
                if (facts.smartTubeInBypass || facts.appListMode == AppListMode.AllTraffic) ok("SmartTube идёт через NetFix") else bad("SmartTube не идёт через NetFix")
            }
        }
        return list
    }

    fun dnsLabel(mode: TunnelDnsMode): String = when (mode) {
        TunnelDnsMode.System -> "системный"
        TunnelDnsMode.Cloudflare -> "1.1.1.1"
        TunnelDnsMode.Custom -> "свой"
    }

    val probesStrategy: Set<HelpSymptom> = setOf(
        HelpSymptom.YoutubeDown,
        HelpSymptom.YoutubeSlow,
        HelpSymptom.YoutubeBrowser,
        HelpSymptom.Discord,
        HelpSymptom.Tv,
        HelpSymptom.ScanZero,
        HelpSymptom.WorseAfterUpdate
    )

    fun appForSymptom(symptom: HelpSymptom): String? = when (symptom) {
        HelpSymptom.YoutubeDown, HelpSymptom.YoutubeSlow, HelpSymptom.YoutubeFromDesktop -> YOUTUBE_PACKAGE
        HelpSymptom.Discord -> DISCORD_PACKAGE
        else -> null
    }

    val cloudflareApps = setOf("com.openai.chatgpt", "com.discord")

    val smartDnsApps = setOf("com.openai.chatgpt", "com.google.android.apps.bard")

    const val YOUTUBE_PACKAGE = "com.google.android.youtube"
    const val DISCORD_PACKAGE = "com.discord"

    val knownAppHosts: Map<String, List<String>> = mapOf(
        "com.instagram.android" to listOf("www.instagram.com", "i.instagram.com", "static.cdninstagram.com"),
        "com.instagram.barcelona" to listOf("www.threads.net", "i.instagram.com"),
        "com.whatsapp" to listOf("www.whatsapp.com", "web.whatsapp.com"),
        "com.twitter.android" to listOf("x.com", "api.x.com", "abs.twimg.com"),
        "com.openai.chatgpt" to listOf("chatgpt.com/cdn-cgi/trace", "cdn.oaistatic.com"),
        "com.google.android.apps.bard" to listOf("gemini.google.com"),
        "com.spotify.music" to listOf("open.spotify.com", "spclient.wg.spotify.com"),
        "com.facebook.katana" to listOf("www.facebook.com", "graph.facebook.com"),
        "com.facebook.orca" to listOf("www.messenger.com", "graph.facebook.com"),
        "com.discord" to listOf("discord.com", "gateway.discord.gg", "cdn.discordapp.com"),
        "tv.twitch.android.app" to listOf("www.twitch.tv", "gql.twitch.tv"),
        "com.reddit.frontpage" to listOf("www.reddit.com", "gql.reddit.com"),
        "org.thoughtcrime.securesms" to listOf("chat.signal.org"),
        "com.viber.voip" to listOf("www.viber.com"),
        "com.linkedin.android" to listOf("www.linkedin.com"),
        "com.pinterest" to listOf("www.pinterest.com")
    )

    val popularApps: List<String> = listOf(
        "com.instagram.android",
        "com.whatsapp",
        "com.twitter.android",
        "com.openai.chatgpt",
        "com.google.android.apps.bard",
        "com.spotify.music",
        "com.facebook.katana",
        "com.instagram.barcelona",
        "tv.twitch.android.app",
        "com.reddit.frontpage",
        "org.thoughtcrime.securesms",
        "com.viber.voip"
    )
}
