package com.rupleide.netfix.core.help

import com.rupleide.netfix.core.compat.TunnelDnsMode
import com.rupleide.netfix.core.dpibypass.AppListMode
import com.rupleide.netfix.core.dpibypass.NetworkKind

enum class HelpSymptom(val title: String, val subtitle: String, val needsAppPick: Boolean = false) {
    YoutubeDown("YouTube не открывается", "Не грузится лента или не идёт видео"),
    YoutubeSlow("YouTube тормозит", "Видео зависает, долго грузится"),
    YoutubeFromDesktop("YouTube медленный с рабочего стола", "Из NetFix быстрый, с иконки тормозит"),
    YoutubeBrowser("YouTube в браузере", "Сайт youtube.com не открывается"),
    TelegramNoConnect("Telegram не подключается", "Вечное «Соединение…» или «Подключение к прокси»"),
    TelegramDrops("Telegram отваливается", "Работает, а потом пропадает, чаще на мобильной сети"),
    Discord("Discord не работает", "Не грузится, нет звонков или вечное подключение"),
    OtherApp("Другое приложение не работает", "Instagram, WhatsApp, X, ChatGPT…", needsAppPick = true),
    BrokeApp("С NetFix сломалось приложение", "Без NetFix работает, с ним нет", needsAppPick = true),
    WontStart("NetFix не включается", "Или включается только Telegram"),
    WorseAfterUpdate("Стало хуже после обновления", "На прошлой версии всё работало"),
    Tv("YouTube на телевизоре не работает", "SmartTube или YouTube не грузится на приставке"),
    ScanZero("Подбор не нашёл способов", "0 из N на всех способах"),
    StopsInBackground("NetFix сам выключается", "Через какое-то время в фоне всё перестаёт работать"),
    Other("Другое или не знаю", "Отправлю отчёт в поддержку")
}

enum class HelpFixKind { Auto, Manual, Advice }

enum class HelpFix(
    val kind: HelpFixKind,
    val title: String,
    val whatChanges: String,
    val howToUndo: String = "",
    val changesStrategy: Boolean = false,
    val changesTunnelDns: Boolean = false
) {
    EnableSecureDns(HelpFixKind.Auto, "Включить защищённый DNS", "Оператор не сможет подменить адреса сайтов, а ChatGPT и Gemini откроются с любым DNS. NetFix перезапустится.", changesTunnelDns = true),
    CheckCfServers(HelpFixKind.Auto, "Проверить серверы Cloudflare", "Проверю серверы Cloudflare, через которые работает Telegram. Если закреплённый сервер не отвечает, верну автоматический выбор."),
    TunnelDnsBest(HelpFixKind.Auto, "Подобрать DNS в NetFix", "Проверю все DNS по очереди, как в «DNS внутри туннеля», и включу лучший для вашей проблемы. Это около минуты.", changesTunnelDns = true),
    CloudflareUnfreeze(HelpFixKind.Auto, "Обойти заморозку Cloudflare", "Некоторые операторы обрывают загрузку с серверов Cloudflare, на которых работают Discord, ChatGPT и другие. NetFix ищет обход сам, но мог найти его давно, а сеть с тех пор поменялась. Проверю заново прямо сейчас. Это около минуты."),
    TunnelDnsNext(HelpFixKind.Auto, "Попробовать другой DNS", "Включу следующий DNS по результатам проверки.", changesTunnelDns = true),
    StartBypass(HelpFixKind.Auto, "Включить NetFix", "NetFix выключен, поэтому блокировки работают."),
    StartTgProxy(HelpFixKind.Auto, "Включить прокси Telegram", "Прокси Telegram выключен."),
    AddAppToBypass(HelpFixKind.Auto, "Пустить {app} через NetFix", "Сейчас {app} идёт мимо NetFix."),
    AddBrowserToBypass(HelpFixKind.Auto, "Пустить браузер через NetFix", "Браузер идёт мимо NetFix, поэтому YouTube в нём не открывается."),
    RemoveAppFromBypass(HelpFixKind.Auto, "Убрать {app} из NetFix", "Некоторым приложениям NetFix мешает. {app} будет работать напрямую."),
    UseNextStrategy(HelpFixKind.Auto, "Включить лучший способ для YouTube", "Выберу по проверке на этой сети способ, где YouTube работает лучше всего. Если проверки ещё не было, сначала проверю способы, это пара минут.", changesStrategy = true),
    UseFasterVideoStrategy(HelpFixKind.Auto, "Включить способ, где видео быстрее", "Замерю скорость видео на лучших способах этой сети и включу самый быстрый. Это минута-две.", changesStrategy = true),
    RescanNetwork(HelpFixKind.Auto, "Подобрать способ заново", "Займёт 2–5 минут, на это время NetFix выключится.", changesStrategy = true),
    UseDiscordStrategy(HelpFixKind.Auto, "Включить лучший способ для Discord", "Выберу по проверке на этой сети способ, где Discord работает лучше всего. Если проверки ещё не было, сначала проверю способы, это пара минут.", changesStrategy = true),
    UseAnotherDiscordStrategy(HelpFixKind.Auto, "Попробовать ещё один способ для Discord", "Включу следующий по качеству способ для Discord.", changesStrategy = true),
    ResetSettings(HelpFixKind.Auto, "Сбросить настройки NetFix", "Если после обновления ничего не работает, обычно помогает сброс. Способы, результаты проверок и список приложений сохранятся, NetFix перезапустится и заново подберёт способ."),
    AdviceVpnDetected(HelpFixKind.Advice, "{app} не работает с VPN", "{app} не идёт через NetFix, но отказывается работать, когда на телефоне включён любой VPN. Так делают многие банки и Госуслуги. Выключайте NetFix, пока пользуетесь {app}."),
    AdviceNoInternet(HelpFixKind.Advice, "Нет интернета", "Интернета нет даже без NetFix. Проверьте Wi-Fi или мобильную сеть и запустите помощник снова."),
    EnableDiscordCheck(HelpFixKind.Auto, "Учитывать Discord при подборе", "Сейчас способ подбирается только под YouTube."),
    EnableStreamProbe(HelpFixKind.Auto, "Проверять скорость видео", "При подборе NetFix будет смотреть, как быстро грузится видео."),
    EnableSmartTubeHosts(HelpFixKind.Auto, "Проверять SmartTube", "При подборе NetFix будет проверять и SmartTube."),
    DnsSystem(HelpFixKind.Auto, "DNS в NetFix: провайдерский", "Иногда сайты открываются только через DNS провайдера.", changesTunnelDns = true),
    AllAppsMode(HelpFixKind.Auto, "Пустить все приложения", "Сейчас через NetFix идут только выбранные приложения."),
    SelectedAppsMode(HelpFixKind.Auto, "Только выбранные приложения", "Сейчас через NetFix идут все приложения, а некоторым это мешает."),
    NetworkLatest(HelpFixKind.Auto, "Включить новое из этой версии", "Сейчас NetFix работает с сетью как старая версия."),
    NetworkPrevious(HelpFixKind.Auto, "Вернуть сеть как до обновления", "Выключу новое из этой версии."),
    FreePortOn(HelpFixKind.Auto, "Обходить занятый порт", "Порт NetFix может быть занят другим приложением."),
    TgWatchdogOn(HelpFixKind.Auto, "Чинить зависший Telegram", "NetFix сам перезапустит прокси, если Telegram зависнет."),
    TgCfPriorityOff(HelpFixKind.Auto, "Выключить «Приоритет Cloudflare»", "На мобильной сети это часто помогает."),
    TgStableOn(HelpFixKind.Auto, "Не усыплять прокси", "Телефон не будет усыплять прокси в фоне. Батарея будет садиться чуть быстрее."),
    TgReconnect(HelpFixKind.Auto, "Заново подключить Telegram", "Откроется Telegram, там нажмите «Подключить»."),
    RequestVpnPermission(HelpFixKind.Auto, "Дать разрешение NetFix", "Без разрешения NetFix не работает. Нажмите «ОК» в окне телефона."),
    DisableOtherVpn(HelpFixKind.Manual, "Выключить другой VPN", "Работать может только один VPN. Выключите другой в открывшихся настройках."),
    ReconnectNetwork(HelpFixKind.Manual, "Переподключить сеть", "Сеть отдаёт неправильные адреса сайтов, поэтому ни один способ не работает. Выключите и включите Wi-Fi или перейдите на мобильную сеть. Если не поможет, включите частный DNS в Настройки, DNS, «Маршрутизация DNS»."),
    DisablePrivateDns(HelpFixKind.Manual, "Выключить «Частный DNS»", "Пока он включён, DNS в NetFix не работает. В настройках выберите «Отключено» или «Автоматически»."),
    BatteryUnrestricted(HelpFixKind.Manual, "Не усыплять NetFix", "Телефон выключает NetFix в фоне. Нажмите «Разрешить»."),
    VendorAutostart(HelpFixKind.Manual, "Разрешить работу в фоне", "Включите для NetFix автозапуск и работу в фоне."),
    NetworkAssistant(HelpFixKind.Manual, "Выключить «Сетевой ассистент»", "Он уводит трафик мимо NetFix. Выключите его в настройках Wi-Fi."),
    RestartYoutube(HelpFixKind.Manual, "Перезапустить YouTube", "Нажмите «Остановить», потом откройте YouTube заново."),
    SmartTubeConscrypt(HelpFixKind.Manual, "Включить Conscrypt в SmartTube", "Откроется SmartTube. Зайдите в Настройки, Общие, «Настройки сети» и включите «Включить Conscrypt». Потом закройте SmartTube, откройте снова и проверьте видео. Если станет хуже, выключите этот пункт обратно."),
    AdviceVpn(HelpFixKind.Advice, "Помощник не справился с этой проблемой", "Провайдер блокирует сильнее, чем NetFix сейчас умеет обходить. Напишите в поддержку, разберёмся вместе."),
    AdviceSmartTube(HelpFixKind.Advice, "Обновите SmartTube", "На старых Android он может не показывать видео. Обновите его на вкладке YouTube.")
}

enum class HelpFixRole { Precondition, Change, Manual, Advice }

val HelpFix.role: HelpFixRole
    get() = when {
        kind == HelpFixKind.Advice -> HelpFixRole.Advice
        kind == HelpFixKind.Manual -> HelpFixRole.Manual
        this in setOf(
            HelpFix.StartBypass,
            HelpFix.StartTgProxy,
            HelpFix.RequestVpnPermission,
            HelpFix.AddAppToBypass,
            HelpFix.AddBrowserToBypass,
            HelpFix.TgReconnect
        ) -> HelpFixRole.Precondition
        else -> HelpFixRole.Change
    }

fun HelpFix.titleFor(appLabel: String?): String = title.replace("{app}", appLabel ?: "приложение")

fun HelpFix.textFor(appLabel: String?): String =
    whatChanges.replace("{app}", appLabel ?: "приложение")

enum class HelpAnswer(val label: String) {
    Worked("Заработало"),
    Same("Ничего не изменилось"),
    Applied("Сделано"),
    Worse("Стало хуже"),
    Skipped("Пропущено"),
    Manual("Сделано вручную")
}

data class HelpProbe(val youtubeOk: Int, val youtubeTotal: Int, val discordOk: Int, val discordTotal: Int, val averageMs: Long) {
    val describe: String
        get() = buildString {
            append("YouTube $youtubeOk/$youtubeTotal")
            if (discordTotal > 0) append(", Discord $discordOk/$discordTotal")
            if (averageMs >= 0) append(", $averageMs мс")
        }
}

data class HelpFacts(
    val bypassWanted: Boolean = true,
    val bypassRunning: Boolean = true,
    val tgWanted: Boolean = true,
    val tgProxyRunning: Boolean = true,
    val tgPortOpen: Boolean = true,
    val tgTrafficFlowing: Boolean? = null,
    val tgPortMovedFrom: Int? = null,
    val tgCfPriority: Boolean = true,
    val tgStable: Boolean = false,
    val vpnPermissionMissing: Boolean = false,
    val otherVpnActive: Boolean = false,
    val privateDnsActive: Boolean = false,
    val privateDnsHost: String? = null,
    val networkDnsSpoofed: Boolean = false,
    val internetDirect: Boolean = true,
    val networkKind: NetworkKind? = NetworkKind.Wifi,
    val onEthernet: Boolean = false,
    val isTv: Boolean = false,
    val hasScanForNetwork: Boolean = true,
    val nextWorkingStrategy: String? = null,
    val fasterStrategy: String? = null,
    val currentProbe: HelpProbe? = null,
    val appListMode: AppListMode = AppListMode.SelectedAppsOnly,
    val youtubeInBypass: Boolean = true,
    val targetPackage: String? = null,
    val targetLabel: String? = null,
    val targetInBypass: Boolean = false,
    val targetHostsViaBypass: Map<String, Boolean> = emptyMap(),
    val targetHostsDirect: Map<String, Boolean> = emptyMap(),
    val browserPackage: String? = null,
    val browserInBypass: Boolean = false,
    val smartTubeInstalled: Boolean = false,
    val smartTubeInBypass: Boolean = false,
    val oldAndroid: Boolean = false,
    val tunnelDns: TunnelDnsMode = TunnelDnsMode.System,
    val tunnelDnsName: String? = null,
    val behaviorVersion: String = "1.0.5",
    val behaviorLatest: Boolean = true,
    val previousVersion: String? = "1.0.4",
    val freePortOn: Boolean = true,
    val watchdogOn: Boolean = true,
    val streamProbeOn: Boolean = true,
    val smartTubeHostsOn: Boolean = true,
    val discordCheckOn: Boolean = true,
    val batteryRestricted: Boolean = false,
    val networkAssistantRisk: Boolean = false,
    val dpiPortBusy: Boolean = false,
    val betaExpired: Boolean = false,
    val discordBestStrategy: String? = null,
    val discordBestLabel: String? = null,
    val discordCurrentLabel: String? = null,
    val targetRegionBlocked: Boolean = false,
    val systemDnsWorks: Boolean = true,
    val secureDnsOn: Boolean = true,
    val tgCfEnabled: Boolean = true,
    val tgCfPinned: String? = null,
    val cloudflareFreezeRuledOut: Boolean = false
) {
    val currentProbeAllFailed: Boolean get() = currentProbe != null && currentProbe.youtubeOk == 0 && currentProbe.discordOk == 0
    val currentProbeYoutubeWeak: Boolean get() = currentProbe != null && currentProbe.youtubeOk * 4 < currentProbe.youtubeTotal * 3
    val currentProbeDiscordWeak: Boolean get() = currentProbe != null && currentProbe.discordTotal > 0 && currentProbe.discordOk * 4 < currentProbe.discordTotal * 3
    val targetBlockedDirect: Boolean get() = targetHostsDirect.isNotEmpty() && targetHostsDirect.values.any { !it }
    val targetWorksDirect: Boolean get() = targetHostsDirect.isNotEmpty() && targetHostsDirect.values.all { it }
    val targetBrokenViaBypass: Boolean get() = targetHostsViaBypass.isNotEmpty() && targetHostsViaBypass.values.any { !it }
}

enum class HelpLevel { Ok, Warn, Bad }

data class HelpFinding(val level: HelpLevel, val text: String)
