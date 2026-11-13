package com.rupleide.netfix

import com.rupleide.netfix.core.whatsnew.WhatsNew
import com.rupleide.netfix.core.whatsnew.WhatsNewIds
import com.rupleide.netfix.core.whatsnew.WhatsNewTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsNewTest {

    @Test
    fun featureIsNewOnlyInItsOwnVersion() {
        assertTrue(WhatsNew.isNew(WhatsNewIds.HELP, "1.0.5"))
        assertFalse(WhatsNew.isNew(WhatsNewIds.HELP, "1.0.6"))
        assertFalse(WhatsNew.isNew(WhatsNewIds.HELP, "1.0.4"))
    }

    @Test
    fun whatsNewRowIsNewWhileCurrentVersionHasNotes() {
        assertTrue(WhatsNew.isNew(WhatsNewIds.WHATS_NEW, "1.0.5"))
        assertFalse(WhatsNew.isNew(WhatsNewIds.WHATS_NEW, "9.9.9"))
    }

    @Test
    fun unknownIdIsNeverNew() {
        assertFalse(WhatsNew.isNew("something_else", "1.0.5"))
    }

    @Test
    fun rootOpensFineTuningRow() {
        assertEquals(WhatsNewIds.TELEGRAM, WhatsNew.settingRowFor(WhatsNewIds.ROOT))
        assertEquals(WhatsNewIds.TUNNEL_DNS, WhatsNew.settingRowFor(WhatsNewIds.TUNNEL_DNS))
    }

    @Test
    fun releasesAreUniqueAndNewestFirst() {
        val versions = WhatsNew.releases.map { it.version }
        assertEquals(versions.distinct(), versions)
        val sorted = versions.sortedWith(compareByDescending<String> { it.split('.').map(String::toInt).let { p -> p[0] * 10000 + p[1] * 100 + p[2] } })
        assertEquals(sorted, versions)
    }

    @Test
    fun itemsHaveUniqueIdsTextsAndOpenLabels() {
        WhatsNew.releases.forEach { release ->
            val ids = release.items.map { it.id }
            assertEquals(release.version, ids.distinct(), ids)
            release.items.forEach { item ->
                assertTrue(item.id, item.title.isNotBlank() && item.text.isNotBlank())
                assertFalse(item.id, item.text.contains('—'))
                if (item.target != WhatsNewTarget.None) assertTrue(item.id, item.openLabel.isNotBlank())
            }
        }
    }
}
