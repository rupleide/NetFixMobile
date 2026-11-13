package com.rupleide.netfix.core.help

import android.content.SharedPreferences
import com.rupleide.netfix.core.compat.CompatFeature
import com.rupleide.netfix.core.compat.CompatibilityManager

object HelpSnapshot {
    private const val LEGACY_COMPAT_PROFILE_KEY = "compat_profile"
    private const val SLOT_KEYS_SUFFIX = "|keys"
    private const val SLOT_VALUE_INFIX = "|v|"

    val NETWORK_KEYS: List<String> = listOf(
        "byedpi_cmd_args",
        "byedpi_cmd_args_wifi",
        "byedpi_cmd_args_mobile",
        "byedpi_enable_cmd_settings",
        "universal_strategy",
        "selected_apps",
        "apps_defaults_version",
        "applist_type",
        "scan_check_discord",
        "tgproxy_cf_enabled",
        "tgproxy_cf_priority",
        "tgproxy_cf_pinned_domain",
        "tgproxy_pool_size",
        "tgproxy_port",
        "tgproxy_port_moved_from",
        "tg_proxy_wakelock_enabled",
        CompatibilityManager.KEY_BEHAVIOR_VERSION,
        CompatibilityManager.KEY_TUNNEL_DNS_MODE,
        CompatibilityManager.KEY_TUNNEL_DNS_CUSTOM,
        CompatibilityManager.KEY_TUNNEL_DNS_NAME,
        LEGACY_COMPAT_PROFILE_KEY
    ) + CompatFeature.entries.map { it.storageKey } + com.rupleide.netfix.core.dpibypass.CloudflareUnfreeze.KEY_RECORDS

    val TG_PROXY_KEYS: Set<String> = setOf(
        "tgproxy_cf_enabled",
        "tgproxy_cf_priority",
        "tgproxy_cf_pinned_domain",
        "tgproxy_pool_size",
        "tgproxy_port",
        "tg_proxy_wakelock_enabled",
        CompatFeature.FreeProxyPort.storageKey
    )

    fun capture(source: SharedPreferences, keys: Collection<String> = NETWORK_KEYS): Map<String, Any?> {
        val all = source.all
        return keys.associateWith { key -> if (all.containsKey(key)) copyValue(all[key]) else null }
    }

    fun restore(target: SharedPreferences, captured: Map<String, Any?>) {
        val editor = target.edit()
        captured.forEach { (key, value) -> putValue(editor, key, value) }
        editor.apply()
    }

    fun changedKeys(source: SharedPreferences, captured: Map<String, Any?>): Set<String> {
        val current = capture(source, captured.keys)
        return captured.keys.filter { current[it] != captured[it] }.toSet()
    }

    fun save(store: SharedPreferences, slot: String, captured: Map<String, Any?>) {
        val editor = store.edit()
        store.all.keys.filter { it.startsWith(slot + SLOT_VALUE_INFIX) }.forEach { editor.remove(it) }
        editor.putStringSet(slot + SLOT_KEYS_SUFFIX, captured.keys.toSet())
        captured.forEach { (key, value) -> if (value != null) putValue(editor, slot + SLOT_VALUE_INFIX + key, value) }
        editor.apply()
    }

    fun load(store: SharedPreferences, slot: String): Map<String, Any?>? {
        val keys = store.getStringSet(slot + SLOT_KEYS_SUFFIX, null) ?: return null
        val all = store.all
        return keys.associateWith { copyValue(all[slot + SLOT_VALUE_INFIX + it]) }
    }

    fun clear(store: SharedPreferences, slot: String) {
        val editor = store.edit()
        store.all.keys
            .filter { it == slot + SLOT_KEYS_SUFFIX || it.startsWith(slot + SLOT_VALUE_INFIX) }
            .forEach { editor.remove(it) }
        editor.apply()
    }

    private fun copyValue(value: Any?): Any? =
        if (value is Set<*>) value.filterIsInstance<String>().toSet() else value

    private fun putValue(editor: SharedPreferences.Editor, key: String, value: Any?) {
        when (value) {
            null -> editor.remove(key)
            is String -> editor.putString(key, value)
            is Boolean -> editor.putBoolean(key, value)
            is Int -> editor.putInt(key, value)
            is Long -> editor.putLong(key, value)
            is Float -> editor.putFloat(key, value)
            is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toSet())
            else -> editor.remove(key)
        }
    }
}
