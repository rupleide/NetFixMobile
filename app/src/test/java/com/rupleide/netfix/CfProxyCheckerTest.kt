package com.rupleide.netfix

import com.rupleide.netfix.core.tgproxy.CfProxyChecker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CfProxyCheckerTest {

    @Test
    fun builtInDomainsDecodeLikeTheCore() {
        assertEquals("pyatdesyatdva.co.uk", CfProxyChecker.decodeDomain("clngqrflngqin.com"))
        assertEquals("sorokdva.co.uk", CfProxyChecker.decodeDomain("awzwsldi.com"))
        assertTrue(CfProxyChecker.defaultDomains().all { it.endsWith(".co.uk") })
        assertEquals(15, CfProxyChecker.defaultDomains().size)
    }

    @Test
    fun cachedLinesAreNormalized() {
        assertEquals("cakeisalie.co.uk", CfProxyChecker.normalizeDomain(" CakeIsALie.co.uk. "))
        assertNull(CfProxyChecker.normalizeDomain("example.org"))
        assertNull(CfProxyChecker.normalizeDomain(""))
    }
}
