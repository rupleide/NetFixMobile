package com.rupleide.netfix

import com.rupleide.netfix.core.compat.TunnelDnsMode
import com.rupleide.netfix.core.dnscheck.DnsCheckResult
import com.rupleide.netfix.core.dnscheck.TunnelDnsCheck
import com.rupleide.netfix.core.help.HostVerdict
import com.rupleide.netfix.core.dpibypass.AppListMode
import com.rupleide.netfix.core.dpibypass.NetworkKind
import com.rupleide.netfix.core.help.HelpFacts
import com.rupleide.netfix.core.help.HelpFix
import com.rupleide.netfix.core.help.HelpFixKind
import com.rupleide.netfix.core.help.HelpLevel
import com.rupleide.netfix.core.help.HelpPlanner
import com.rupleide.netfix.core.help.HelpSymptom
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HelpPlannerTest {

    @Test
    fun otherVpnComesFirstForAnyRealProblem() {
        val facts = HelpFacts(otherVpnActive = true)
        HelpSymptom.entries.filter { it != HelpSymptom.Other && it != HelpSymptom.BrokeApp }.forEach {
            assertEquals(it.name, HelpFix.DisableOtherVpn, HelpPlanner.fixes(it, facts).first())
        }
    }

    @Test
    fun tvChainIsShortWithoutSecondDnsAndYoutubeRestart() {
        val tvPlan = HelpPlanner.fixes(HelpSymptom.YoutubeDown, HelpFacts(isTv = true))
        assertFalse(HelpFix.TunnelDnsNext in tvPlan)
        assertFalse(HelpFix.RestartYoutube in tvPlan)
        assertTrue(HelpFix.TunnelDnsBest in tvPlan)
        val phonePlan = HelpPlanner.fixes(HelpSymptom.YoutubeDown, HelpFacts(isTv = false))
        assertTrue(HelpFix.TunnelDnsNext in phonePlan)
        assertTrue(HelpFix.RestartYoutube in phonePlan)
    }

    @Test
    fun secureDnsComesBeforeDnsPickingWhenOff() {
        val off = HelpPlanner.fixes(HelpSymptom.YoutubeDown, HelpFacts(secureDnsOn = false))
        assertTrue(off.indexOf(HelpFix.EnableSecureDns) in 0 until off.indexOf(HelpFix.TunnelDnsBest))
        assertFalse(HelpFix.EnableSecureDns in HelpPlanner.fixes(HelpSymptom.YoutubeDown, HelpFacts(secureDnsOn = true)))
        val chatGpt = HelpPlanner.fixes(HelpSymptom.OtherApp, HelpFacts(secureDnsOn = false, targetPackage = "com.openai.chatgpt", targetInBypass = true))
        assertEquals(HelpFix.EnableSecureDns, chatGpt.first())
    }

    @Test
    fun telegramChecksCloudflareServers() {
        val noConnect = HelpPlanner.fixes(HelpSymptom.TelegramNoConnect, HelpFacts(tgCfEnabled = true))
        assertTrue(HelpFix.CheckCfServers in noConnect)
        assertFalse(HelpFix.CheckCfServers in HelpPlanner.fixes(HelpSymptom.TelegramNoConnect, HelpFacts(tgCfEnabled = false)))
        val pinned = HelpPlanner.fixes(HelpSymptom.TelegramDrops, HelpFacts(tgCfEnabled = true, tgCfPinned = "sorokodin.co.uk", watchdogOn = false))
        assertEquals(HelpFix.CheckCfServers, pinned.first())
    }

    @Test
    fun smartTubeConscryptOnlyOnTvWithSmartTubeAfterDns() {
        val tvPlan = HelpPlanner.fixes(HelpSymptom.Tv, HelpFacts(isTv = true, smartTubeInstalled = true))
        assertTrue(HelpFix.SmartTubeConscrypt in tvPlan)
        assertTrue(tvPlan.indexOf(HelpFix.SmartTubeConscrypt) > tvPlan.indexOf(HelpFix.TunnelDnsBest))
        val tvYoutube = HelpPlanner.fixes(HelpSymptom.YoutubeDown, HelpFacts(isTv = true, smartTubeInstalled = true))
        assertTrue(tvYoutube.indexOf(HelpFix.SmartTubeConscrypt) in (tvYoutube.indexOf(HelpFix.TunnelDnsBest) + 1) until tvYoutube.indexOf(HelpFix.AdviceVpn))
        assertFalse(HelpFix.SmartTubeConscrypt in HelpPlanner.fixes(HelpSymptom.Tv, HelpFacts(isTv = true, smartTubeInstalled = false)))
        assertFalse(HelpFix.SmartTubeConscrypt in HelpPlanner.fixes(HelpSymptom.YoutubeDown, HelpFacts(isTv = false, smartTubeInstalled = true)))
    }

    @Test
    fun spoofedNetworkDnsAsksToReconnectBeforeRescan() {
        val facts = HelpFacts(networkDnsSpoofed = true)
        val plan = HelpPlanner.fixes(HelpSymptom.ScanZero, facts)
        assertEquals(HelpFix.ReconnectNetwork, plan.first())
        assertTrue(plan.indexOf(HelpFix.ReconnectNetwork) < plan.indexOf(HelpFix.RescanNetwork))
        assertFalse(HelpFix.ReconnectNetwork in HelpPlanner.fixes(HelpSymptom.TelegramDrops, facts))
        assertFalse(HelpFix.ReconnectNetwork in HelpPlanner.fixes(HelpSymptom.ScanZero, HelpFacts()))
        assertTrue(HelpPlanner.findings(HelpSymptom.ScanZero, facts).any { it.level == HelpLevel.Bad && "неправильные адреса" in it.text })
    }

    @Test
    fun youtubeMissingFromBypassListIsFixedFirst() {
        val facts = HelpFacts(youtubeInBypass = false, targetPackage = HelpPlanner.YOUTUBE_PACKAGE)
        assertEquals(HelpFix.AddAppToBypass, HelpPlanner.fixes(HelpSymptom.YoutubeDown, facts).first())
    }

    @Test
    fun youtubeInAllAppsModeDoesNotOfferAddingToList() {
        val facts = HelpFacts(youtubeInBypass = false, appListMode = AppListMode.AllTraffic)
        assertFalse(HelpFix.AddAppToBypass in HelpPlanner.fixes(HelpSymptom.YoutubeDown, facts))
    }

    @Test
    fun stoppedBypassIsStartedBeforeAnythingElse() {
        val facts = HelpFacts(bypassRunning = false)
        assertEquals(HelpFix.StartBypass, HelpPlanner.fixes(HelpSymptom.YoutubeDown, facts).first())
    }

    @Test
    fun missingVpnPermissionAsksForItInsteadOfStarting() {
        val plan = HelpPlanner.fixes(HelpSymptom.YoutubeDown, HelpFacts(bypassRunning = false, vpnPermissionMissing = true))
        assertEquals(HelpFix.RequestVpnPermission, plan.first())
        assertFalse(HelpFix.StartBypass in plan)
    }

    @Test
    fun telegramDropsOnMobileFollowsTheOrderThatHelpedInChats() {
        val facts = HelpFacts(networkKind = NetworkKind.Mobile, watchdogOn = false, tgCfPriority = true, batteryRestricted = true)
        val plan = HelpPlanner.fixes(HelpSymptom.TelegramDrops, facts)
        assertEquals(listOf(HelpFix.TgWatchdogOn, HelpFix.TgCfPriorityOff, HelpFix.TgStableOn, HelpFix.BatteryUnrestricted), plan.take(4))
    }

    @Test
    fun telegramWithCfPriorityAlreadyOffDoesNotOfferIt() {
        val plan = HelpPlanner.fixes(HelpSymptom.TelegramDrops, HelpFacts(tgCfPriority = false))
        assertFalse(HelpFix.TgCfPriorityOff in plan)
    }

    @Test
    fun appProblemChecksAllDnsAndThenTriesTheNextOne() {
        val facts = HelpFacts(targetPackage = "com.openai.chatgpt", targetInBypass = true, tunnelDns = TunnelDnsMode.System)
        val plan = HelpPlanner.fixes(HelpSymptom.OtherApp, facts)
        assertEquals(listOf(HelpFix.CloudflareUnfreeze, HelpFix.TunnelDnsBest, HelpFix.TunnelDnsNext, HelpFix.AdviceVpn), plan)
    }

    @Test
    fun privateDnsIsTurnedOffBeforeTunnelDns() {
        val facts = HelpFacts(targetPackage = "com.openai.chatgpt", targetInBypass = true, privateDnsActive = true, privateDnsHost = "dns.dagami.ru")
        val plan = HelpPlanner.fixes(HelpSymptom.OtherApp, facts)
        assertEquals(listOf(HelpFix.CloudflareUnfreeze, HelpFix.DisablePrivateDns, HelpFix.TunnelDnsBest, HelpFix.TunnelDnsNext), plan.take(4))
    }

    @Test
    fun youtubeAndTvNoLongerJumpStraightToCloudflare() {
        listOf(HelpSymptom.YoutubeDown, HelpSymptom.YoutubeBrowser, HelpSymptom.Tv).forEach { symptom ->
            val plan = HelpPlanner.fixes(symptom, HelpFacts(tunnelDns = TunnelDnsMode.System))
            assertTrue(symptom.name, HelpFix.TunnelDnsBest in plan)
        }
    }

    private fun dnsResult(ok: Set<String>, ms: Long): DnsCheckResult =
        DnsCheckResult("1.2.3.4", ms, TunnelDnsCheck.targets.flatMap { target ->
            target.hosts.map { it to if (target.label in ok) HostVerdict.Ok else HostVerdict.Blocked }
        }.toMap())

    @Test
    fun dnsRankingPutsTheProblemServiceFirstThenCoverageThenSpeed() {
        val ranking = HelpPlanner.rankDns(
            mapOf(
                "Cloudflare 1.1.1.1" to dnsResult(setOf("Instagram", "Spotify", "X (Twitter)"), 30),
                "MagicDNS" to dnsResult(setOf("YouTube", "ChatGPT"), 90),
                "Comss" to dnsResult(setOf("YouTube", "ChatGPT", "Spotify"), 120),
                "Dagami" to DnsCheckResult(null, -1, emptyMap(), "DNS не отвечает")
            ),
            target = "ChatGPT"
        )
        assertEquals(listOf("Comss", "MagicDNS", "Cloudflare 1.1.1.1"), ranking.map { it.key })
    }

    @Test
    fun dnsPickSkipsCurrentAndTriedAndNeverPicksOneWithoutTheService() {
        val ranking = HelpPlanner.rankDns(
            mapOf(
                "Системный" to dnsResult(setOf("Instagram"), 20),
                "MagicDNS" to dnsResult(setOf("ChatGPT", "YouTube"), 90),
                "Comss" to dnsResult(setOf("ChatGPT"), 60)
            ),
            target = "ChatGPT"
        )
        assertEquals("MagicDNS", HelpPlanner.pickDns(ranking, "Системный", setOf("Системный"), atLeastAsGoodAsCurrent = true)?.key)
        assertEquals("Comss", HelpPlanner.pickDns(ranking, "Системный", setOf("Системный", "MagicDNS"), atLeastAsGoodAsCurrent = false)?.key)
        assertNull(HelpPlanner.pickDns(ranking, "Системный", setOf("Системный", "MagicDNS", "Comss"), atLeastAsGoodAsCurrent = false))
    }

    @Test
    fun dnsPickKeepsTheCurrentWhenItIsAlreadyTheBest() {
        val ranking = HelpPlanner.rankDns(
            mapOf(
                "MagicDNS" to dnsResult(setOf("ChatGPT", "YouTube", "Spotify"), 90),
                "Comss" to dnsResult(setOf("ChatGPT"), 60)
            ),
            target = "ChatGPT"
        )
        assertNull(HelpPlanner.pickDns(ranking, "MagicDNS", setOf("MagicDNS"), atLeastAsGoodAsCurrent = true))
    }

    @Test
    fun discordCardNamesTheBestStoredConfig() {
        val facts = HelpFacts(discordBestStrategy = "-o1 -a1", discordBestLabel = "«Способ 12»: Discord 6/6", discordCurrentLabel = "2/6")
        val detail = HelpPlanner.detailFor(HelpFix.UseDiscordStrategy, facts)
        assertTrue(detail!!.contains("Способ 12"))
        assertFalse(detail.contains("74"))
    }

    @Test
    fun discordOnMobileExplainsTheOperatorInsteadOfGenericAdvice() {
        val mobile = HelpFacts(targetPackage = HelpPlanner.DISCORD_PACKAGE, networkKind = NetworkKind.Mobile)
        assertTrue(HelpPlanner.detailFor(HelpFix.AdviceVpn, mobile)!!.contains("Оператор"))
        assertNull(HelpPlanner.detailFor(HelpFix.AdviceVpn, mobile.copy(networkKind = NetworkKind.Wifi)))
    }

    @Test
    fun regionBlockedAppIsNotToldToTurnOffVpn() {
        val facts = HelpFacts(targetPackage = "com.openai.chatgpt", targetInBypass = false, targetRegionBlocked = true)
        val plan = HelpPlanner.fixes(HelpSymptom.BrokeApp, facts)
        assertFalse(HelpFix.AdviceVpnDetected in plan)
        assertEquals(listOf(HelpFix.AddAppToBypass, HelpFix.TunnelDnsBest, HelpFix.TunnelDnsNext), plan)
    }

    @Test
    fun bankAppOutsideNetFixGetsVpnDetectionAdvice() {
        val plan = HelpPlanner.fixes(HelpSymptom.BrokeApp, HelpFacts(targetPackage = "ru.sberbankmobile", targetInBypass = false))
        assertEquals(listOf(HelpFix.AdviceVpnDetected), plan)
    }

    @Test
    fun scanZeroWithoutAnyCauseDoesNotRepeatTheSameScan() {
        val plan = HelpPlanner.fixes(HelpSymptom.ScanZero, HelpFacts(bypassRunning = false))
        assertFalse(HelpFix.RescanNetwork in plan)
        assertFalse(HelpFix.TunnelDnsBest in plan)
        assertFalse(HelpFix.DnsSystem in plan)
    }

    @Test
    fun scanZeroRescansOnlyAfterFixingACause() {
        val plan = HelpPlanner.fixes(HelpSymptom.ScanZero, HelpFacts(privateDnsActive = true, systemDnsWorks = false, privateDnsHost = "dot.xbox-dns.ru"))
        assertEquals(listOf(HelpFix.DisablePrivateDns, HelpFix.RescanNetwork), plan.take(2))
    }

    @Test
    fun noInternetStopsEverythingElse() {
        assertEquals(listOf(HelpFix.AdviceNoInternet), HelpPlanner.fixes(HelpSymptom.YoutubeDown, HelpFacts(internetDirect = false)))
    }

    @Test
    fun discordTriesTwoConfigsThenDnsThenVpn() {
        val plan = HelpPlanner.fixes(HelpSymptom.Discord, HelpFacts(tunnelDns = TunnelDnsMode.System))
        assertEquals(
            listOf(HelpFix.UseDiscordStrategy, HelpFix.UseAnotherDiscordStrategy, HelpFix.CloudflareUnfreeze, HelpFix.TunnelDnsBest, HelpFix.TunnelDnsNext),
            plan.take(5)
        )
        assertEquals(HelpFix.AdviceVpn, plan.last())
    }

    @Test
    fun brokenSpotifyIsTakenOutOfBypass() {
        val facts = HelpFacts(targetPackage = "com.spotify.music", targetInBypass = true)
        assertEquals(HelpFix.RemoveAppFromBypass, HelpPlanner.fixes(HelpSymptom.BrokeApp, facts).first())
    }

    @Test
    fun worseAfterUpdateOffersPreviousNetworkFirst() {
        val plan = HelpPlanner.fixes(HelpSymptom.WorseAfterUpdate, HelpFacts(previousVersion = "1.0.4"))
        assertEquals(HelpFix.NetworkPrevious, plan.first())
    }

    @Test
    fun cloudflareStepIsSkippedWhenThisNetworkWasJustCheckedAndDoesNotFreeze() {
        val facts = HelpFacts(tunnelDns = TunnelDnsMode.System, cloudflareFreezeRuledOut = true)
        assertTrue(HelpFix.CloudflareUnfreeze !in HelpPlanner.fixes(HelpSymptom.Discord, facts))
        assertTrue(HelpFix.CloudflareUnfreeze !in HelpPlanner.fixes(HelpSymptom.OtherApp, facts.copy(targetPackage = "com.openai.chatgpt", targetInBypass = true)))
    }

    @Test
    fun discordEndsWithHonestVpnAdvice() {
        val plan = HelpPlanner.fixes(HelpSymptom.Discord, HelpFacts())
        assertTrue(HelpFix.UseDiscordStrategy in plan)
        assertEquals(HelpFix.AdviceVpn, plan.last())
    }

    @Test
    fun tvOnCableWithCloudflareDnsSwitchesToSystemDnsLikeMaxOne() {
        val facts = HelpFacts(isTv = true, onEthernet = true, tunnelDns = TunnelDnsMode.Cloudflare)
        assertEquals(HelpFix.DnsSystem, HelpPlanner.fixes(HelpSymptom.Tv, facts).first())
    }

    @Test
    fun otherSymptomHasNoFixesAndGoesToReport() {
        assertTrue(HelpPlanner.fixes(HelpSymptom.Other, HelpFacts(otherVpnActive = true)).isEmpty())
    }

    @Test
    fun everySymptomPlanHasNoDuplicatesAndAdviceOnlyAtTheEnd() {
        val busyFacts = HelpFacts(
            bypassRunning = false,
            otherVpnActive = true,
            privateDnsActive = true,
            networkAssistantRisk = true,
            batteryRestricted = true,
            behaviorLatest = false,
            streamProbeOn = false,
            watchdogOn = false,
            freePortOn = false,
            nextWorkingStrategy = "-o1 -a1",
            fasterStrategy = "-d1 -a1",
            smartTubeInstalled = true,
            oldAndroid = true
        )
        HelpSymptom.entries.forEach { symptom ->
            val plan = HelpPlanner.fixes(symptom, busyFacts)
            assertEquals(symptom.name, plan.distinct(), plan)
            val firstAdvice = plan.indexOfFirst { it.kind == HelpFixKind.Advice }
            if (firstAdvice >= 0) assertTrue(symptom.name, plan.drop(firstAdvice).all { it.kind == HelpFixKind.Advice })
        }
    }

    @Test
    fun findingsReportOtherVpnAsProblem() {
        val findings = HelpPlanner.findings(HelpSymptom.YoutubeDown, HelpFacts(otherVpnActive = true))
        assertTrue(findings.any { it.level == HelpLevel.Bad && "VPN" in it.text })
    }
}
