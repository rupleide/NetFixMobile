package com.rupleide.netfix

import com.rupleide.netfix.core.compat.CompatFeature
import com.rupleide.netfix.core.compat.CompatibilityManager
import com.rupleide.netfix.core.compat.TunnelDnsMode
import com.rupleide.netfix.core.dpibypass.AppListMode
import com.rupleide.netfix.core.dpibypass.appListMode
import com.rupleide.netfix.core.dpibypass.setAppListMode
import com.rupleide.netfix.core.help.HelpSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HelpSnapshotTest {

    private fun userOnV104() = InMemoryPreferences().apply {
        edit()
            .putBoolean("onboarding_completed", true)
            .putString("byedpi_cmd_args", "-s1+s -d3+s -a1")
            .putBoolean("byedpi_enable_cmd_settings", true)
            .putStringSet("selected_apps", setOf("com.google.android.youtube"))
            .putInt("tgproxy_port", 1443)
            .putString("unrelated_ui_flag", "keep")
            .apply()
        CompatibilityManager.markPromptShown(this, 8)
    }

    @Test
    fun rollbackReturnsEveryNetworkKeyExactly() {
        val prefs = userOnV104()
        val before = HashMap(prefs.values)
        val snapshot = HelpSnapshot.capture(prefs)

        CompatibilityManager.setTunnelDnsMode(prefs, TunnelDnsMode.System)
        CompatibilityManager.setFeature(prefs, CompatFeature.TgProxyWatchdog, true)
        prefs.setAppListMode(AppListMode.AllTraffic)
        prefs.edit()
            .putStringSet("selected_apps", setOf("com.google.android.youtube", "com.instagram.android"))
            .putBoolean("tgproxy_cf_priority", false)
            .putString("byedpi_cmd_args", "-o1 -a1")
            .apply()

        HelpSnapshot.restore(prefs, snapshot)

        assertEquals(before, prefs.values)
    }

    @Test
    fun keysThatDidNotExistAreRemovedNotDefaulted() {
        val prefs = userOnV104()
        val snapshot = HelpSnapshot.capture(prefs)
        assertFalse(prefs.contains(CompatibilityManager.KEY_TUNNEL_DNS_MODE))

        CompatibilityManager.setTunnelDnsMode(prefs, TunnelDnsMode.System)
        HelpSnapshot.restore(prefs, snapshot)

        assertFalse(prefs.contains(CompatibilityManager.KEY_TUNNEL_DNS_MODE))
        assertEquals(TunnelDnsMode.Cloudflare, CompatibilityManager.tunnelDnsMode(prefs))
        CompatFeature.entries.forEach { assertFalse(it.name, CompatibilityManager.featureEnabled(prefs, it)) }
    }

    @Test
    fun rollbackAfterVersionSwitchKeepsOldBehavior() {
        val prefs = userOnV104()
        val snapshot = HelpSnapshot.capture(prefs)

        CompatibilityManager.enableEverythingNew(prefs)
        assertTrue(CompatibilityManager.isLatestBehavior(prefs))

        HelpSnapshot.restore(prefs, snapshot)
        assertEquals("1.0.4", CompatibilityManager.behaviorVersion(prefs))
        assertFalse(CompatibilityManager.isCustomized(prefs))
    }

    @Test
    fun unrelatedKeysAreNotTouched() {
        val prefs = userOnV104()
        val snapshot = HelpSnapshot.capture(prefs)
        prefs.edit().putString("unrelated_ui_flag", "changed by user later").apply()

        HelpSnapshot.restore(prefs, snapshot)

        assertEquals("changed by user later", prefs.getString("unrelated_ui_flag", null))
    }

    @Test
    fun changedKeysListsOnlyWhatMoved() {
        val prefs = userOnV104()
        val snapshot = HelpSnapshot.capture(prefs)
        prefs.edit().putBoolean("tgproxy_cf_priority", false).apply()
        prefs.setAppListMode(AppListMode.AllTraffic)

        assertEquals(setOf("tgproxy_cf_priority", "applist_type"), HelpSnapshot.changedKeys(prefs, snapshot))
    }

    @Test
    fun savedSnapshotSurvivesStoreRoundTripIncludingAbsentKeys() {
        val prefs = userOnV104()
        val store = InMemoryPreferences()
        val snapshot = HelpSnapshot.capture(prefs)

        HelpSnapshot.save(store, "before", snapshot)
        val loaded = HelpSnapshot.load(store, "before")

        assertEquals(snapshot, loaded)
        assertNull(loaded!![CompatibilityManager.KEY_TUNNEL_DNS_MODE])
        assertEquals(setOf("com.google.android.youtube"), loaded["selected_apps"])
    }

    @Test
    fun resavingSlotDropsValuesOfKeysThatBecameAbsent() {
        val prefs = userOnV104()
        val store = InMemoryPreferences()
        prefs.edit().putBoolean("tgproxy_cf_priority", false).apply()
        HelpSnapshot.save(store, "before", HelpSnapshot.capture(prefs))
        prefs.edit().remove("tgproxy_cf_priority").apply()
        HelpSnapshot.save(store, "before", HelpSnapshot.capture(prefs))

        assertNull(HelpSnapshot.load(store, "before")!!["tgproxy_cf_priority"])
    }

    @Test
    fun clearRemovesSlot() {
        val store = InMemoryPreferences()
        HelpSnapshot.save(store, "before", HelpSnapshot.capture(userOnV104()))
        HelpSnapshot.clear(store, "before")
        assertNull(HelpSnapshot.load(store, "before"))
        assertTrue(store.values.isEmpty())
    }

    @Test
    fun appListModeRoundTrips() {
        val prefs = userOnV104()
        val snapshot = HelpSnapshot.capture(prefs)
        prefs.setAppListMode(AppListMode.AllTraffic)
        HelpSnapshot.restore(prefs, snapshot)
        assertEquals(AppListMode.SelectedAppsOnly, prefs.appListMode())
    }
}
