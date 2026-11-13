package com.rupleide.netfix.core.whatsnew

enum class WhatsNewTarget { None, Help, YoutubeTab, SettingsLaunch, SettingsDns, SettingsCompat }

data class WhatsNewItem(
    val id: String,
    val title: String,
    val text: String,
    val target: WhatsNewTarget = WhatsNewTarget.None,
    val openLabel: String = "Открыть"
)

data class WhatsNewRelease(val version: String, val items: List<WhatsNewItem>)

object WhatsNewIds {
    const val HELP = "help"
    const val AI_APPS = "ai_apps"
    const val SCAN = "scan_video"
    const val TV = "tv"
    const val TUNNEL_DNS = "tunnel_dns"
    const val SECURE_DNS = "secure_dns"
    const val CF_SERVERS = "cf_servers"
    const val SORTING = "sorting"
    const val COMPAT = "compat"
    const val DISCORD = "discord_scan"
    const val TELEGRAM = "telegram_stable"
    const val ROOT = "root_mode"
    const val HOME = "home_status"
    const val CLOUDFLARE = "cf_unfreeze"
    const val YOUTUBE_APP = "youtube_app"
    const val BACKUP = "backup_settings"
    const val POLISH = "polish"
    const val BATTERY = "battery_saver"
    const val VIDEO_START = "video_fast_start"
    const val WHATS_NEW = "whats_new"
}

object WhatsNew {
    val releases: List<WhatsNewRelease> = listOf(
        WhatsNewRelease(
            version = "1.0.5",
            items = listOf(
                WhatsNewItem(
                    id = WhatsNewIds.AI_APPS,
                    title = "ChatGPT и Gemini без VPN",
                    text = "Больше никаких «недоступно в вашей стране». ChatGPT и Gemini уже в списке NetFix, нужный DNS он подставляет сам. Просто включите NetFix.",
                    target = WhatsNewTarget.SettingsLaunch,
                    openLabel = "Открыть настройку"
                ),
                WhatsNewItem(
                    id = WhatsNewIds.VIDEO_START,
                    title = "Видео YouTube стартует быстрее",
                    text = "Раньше YouTube мог секунды ждать заблокированный сервер видео. Теперь NetFix сразу говорит ему взять другой, а зависшее видео сам перезапускает."
                ),
                WhatsNewItem(
                    id = WhatsNewIds.HELP,
                    title = "Помощник починит сам",
                    text = "Что-то не работает? Нажмите «?» и выберите, что именно. NetFix проверит сеть и предложит решение по шагам. Не помогло, вернёт как было. Не справился, соберёт отчёт для поддержки.",
                    target = WhatsNewTarget.Help,
                    openLabel = "Открыть помощник"
                ),
                WhatsNewItem(
                    id = WhatsNewIds.CLOUDFLARE,
                    title = "Discord больше не замирает",
                    text = "Оператор обрывал Discord, ChatGPT и другие сайты на Cloudflare через пару секунд. Теперь NetFix сам находит адрес, который не тормозят, и запоминает его для каждой сети."
                ),
                WhatsNewItem(
                    id = WhatsNewIds.SECURE_DNS,
                    title = "YouTube открывается с любым DNS",
                    text = "Оператор подменяет ответы DNS 1.1.1.1 и 8.8.8.8, и YouTube из-за этого не открывался. NetFix получает настоящие адреса сам, ничего включать не нужно.",
                    target = WhatsNewTarget.SettingsDns,
                    openLabel = "Открыть DNS"
                ),
                WhatsNewItem(
                    id = WhatsNewIds.SCAN,
                    title = "Способ выбирается по видео",
                    text = "Подбор проверяет, как грузится настоящее видео YouTube, а не только сайт. Появились новые способы для серверов видео. С SmartTube проверяются и его серверы.",
                    target = WhatsNewTarget.YoutubeTab,
                    openLabel = "Открыть способы"
                ),
                WhatsNewItem(
                    id = WhatsNewIds.YOUTUBE_APP,
                    title = "Любое приложение YouTube",
                    text = "SmartTube, ReVanced, NewPipe или другое: кнопка YouTube на главной откроет ваше приложение, а NetFix сам добавит его в список. Там же ссылки, где их скачать.",
                    target = WhatsNewTarget.SettingsLaunch,
                    openLabel = "Открыть настройку"
                ),
                WhatsNewItem(
                    id = WhatsNewIds.TELEGRAM,
                    title = "Telegram стабильнее",
                    text = "Если прокси завис, NetFix перезапустит его сам. Если порт занят другим приложением, возьмёт свободный.",
                    target = WhatsNewTarget.SettingsCompat,
                    openLabel = "Открыть настройки"
                ),
                WhatsNewItem(
                    id = WhatsNewIds.BATTERY,
                    title = "Меньше расход батареи",
                    text = "Когда экран выключен, прокси Telegram не держит лишние соединения, а анимации не работают в фоне. Сообщения приходят так же."
                ),
                WhatsNewItem(
                    id = WhatsNewIds.DISCORD,
                    title = "Discord с голосом",
                    text = "Включите «Проверять Discord», и NetFix подберёт способ, при котором работают и чат, и голосовые.",
                    target = WhatsNewTarget.SettingsLaunch,
                    openLabel = "Открыть настройку"
                ),
                WhatsNewItem(
                    id = WhatsNewIds.TUNNEL_DNS,
                    title = "Проверка DNS",
                    text = "Нажмите на значок рядом с DNS и увидите, какие сервисы с ним открываются и как быстро он отвечает. Добавлены WLRus и Bezmezhau, нерабочие DNS убраны.",
                    target = WhatsNewTarget.SettingsDns,
                    openLabel = "Открыть DNS"
                ),
                WhatsNewItem(
                    id = WhatsNewIds.CF_SERVERS,
                    title = "Серверы для Telegram",
                    text = "Во вкладке «Прокси» видно, какие серверы Cloudflare для Telegram работают и как быстро. Лучший можно закрепить.",
                    target = WhatsNewTarget.None
                ),
                WhatsNewItem(
                    id = WhatsNewIds.TV,
                    title = "Удобнее на ТВ-приставке",
                    text = "Помощник, выбор плеера и проверка DNS управляются пультом. Отчёт в поддержку отправляется с телефона по QR-коду."
                ),
                WhatsNewItem(
                    id = WhatsNewIds.COMPAT,
                    title = "Можно вернуть как было",
                    text = "Если после обновления что-то сломалось, выберите прошлую версию в «Работе сети», и всё заработает как раньше.",
                    target = WhatsNewTarget.SettingsCompat,
                    openLabel = "Открыть «Работа сети»"
                ),
                WhatsNewItem(
                    id = WhatsNewIds.ROOT,
                    title = "Без VPN на телефонах с root",
                    text = "С root NetFix может работать без значка VPN. Включается в «Тонкой настройке».",
                    target = WhatsNewTarget.SettingsCompat,
                    openLabel = "Открыть настройки"
                ),
                WhatsNewItem(
                    id = WhatsNewIds.SORTING,
                    title = "Сортировка способов",
                    text = "Способы можно сортировать как удобно: по работе, YouTube, Discord или задержке.",
                    target = WhatsNewTarget.YoutubeTab,
                    openLabel = "Открыть способы"
                ),
                WhatsNewItem(
                    id = WhatsNewIds.BACKUP,
                    title = "Перенос настроек",
                    text = "«Экспорт всех данных» сохраняет не только способы, но и список приложений, DNS и выбор YouTube. Удобно при переустановке."
                ),
                WhatsNewItem(
                    id = WhatsNewIds.HOME,
                    title = "Главный экран",
                    text = "Видно, через какую сеть работает NetFix. На Xiaomi и Realme есть подсказка про «Сетевой ассистент»."
                ),
                WhatsNewItem(
                    id = WhatsNewIds.POLISH,
                    title = "Мелкие улучшения",
                    text = "Плавнее полосы проверки, спокойнее навигация в настройках, понятнее тексты. Если после обновления что-то не так, «Начать с чистого листа» сбросит настройки, а способы останутся."
                )
            )
        )
    )

    fun settingRowFor(id: String): String = if (id == WhatsNewIds.ROOT) WhatsNewIds.TELEGRAM else id

    fun releaseFor(versionName: String): WhatsNewRelease? = releases.firstOrNull { it.version == versionName }

    fun isNew(id: String, versionName: String): Boolean =
        releaseFor(versionName)?.items?.any { it.id == id } == true || (id == WhatsNewIds.WHATS_NEW && releaseFor(versionName) != null)
}
