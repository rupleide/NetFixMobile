package com.rupleide.netfix

import com.rupleide.netfix.core.dpibypass.defaultYoutubeApps
import com.rupleide.netfix.core.dpibypass.extraBypassApps
import com.rupleide.netfix.core.dpibypass.rememberAiAppChoice
import com.rupleide.netfix.core.dpibypass.selectedAppsWithDefaults
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultBypassAppsTest {
    private val excludedByDefault = setOf("com.spotify.music", "com.whatsapp", "com.whatsapp.w4b")

    @Test
    fun excludedAppsAreNotInDefaultSets() {
        assertTrue((defaultYoutubeApps + extraBypassApps).intersect(excludedByDefault).isEmpty())
    }

    @Test
    fun cleanInstallDoesNotSelectExcludedApps() {
        val selected = InMemoryPreferences().selectedAppsWithDefaults()
        assertTrue(selected.intersect(excludedByDefault).isEmpty())
        assertTrue(selected.contains("com.discord"))
        assertTrue(selected.contains("com.google.android.youtube"))
    }

    @Test
    fun migrationDoesNotAddExcludedAppsAndKeepsSavedOnes() {
        val prefs = InMemoryPreferences()
        prefs.edit()
            .putStringSet("selected_apps", mutableSetOf("com.google.android.youtube", "com.spotify.music"))
            .putInt("apps_defaults_version", 2)
            .apply()
        val selected = prefs.selectedAppsWithDefaults()
        assertTrue(selected.contains("com.spotify.music"))
        assertFalse(selected.contains("com.whatsapp"))
        assertTrue(selected.contains("com.discord"))
    }

    @Test
    fun aiAppsComeBackUnlessUserRemovedThem() {
        val prefs = InMemoryPreferences()
        com.rupleide.netfix.core.compat.CompatibilityManager.adoptLatestForFreshInstall(prefs)
        prefs.edit().putStringSet("selected_apps", mutableSetOf("com.google.android.youtube")).putInt("apps_defaults_version", 3).apply()
        assertTrue(prefs.selectedAppsWithDefaults().containsAll(setOf("com.openai.chatgpt", "com.google.android.apps.bard")))
        prefs.edit().putStringSet("selected_apps", mutableSetOf("com.google.android.youtube", "com.google.android.apps.bard")).apply()
        prefs.rememberAiAppChoice("com.openai.chatgpt", false)
        val afterRemoval = prefs.selectedAppsWithDefaults()
        assertFalse(afterRemoval.contains("com.openai.chatgpt"))
        assertTrue(afterRemoval.contains("com.google.android.apps.bard"))
    }

    @Test
    fun aiAppsAreNotAddedWhenKeepingV104() {
        val prefs = InMemoryPreferences()
        prefs.edit().putStringSet("selected_apps", mutableSetOf("com.google.android.youtube")).putInt("apps_defaults_version", 3).apply()
        assertFalse(prefs.selectedAppsWithDefaults().contains("com.openai.chatgpt"))
    }

    @Test
    fun currentVersionListIsReturnedUntouched() {
        val prefs = InMemoryPreferences()
        val saved = mutableSetOf("com.google.android.youtube", "com.whatsapp")
        prefs.edit().putStringSet("selected_apps", saved).putInt("apps_defaults_version", 3).apply()
        assertTrue(prefs.selectedAppsWithDefaults() == saved)
    }
}
