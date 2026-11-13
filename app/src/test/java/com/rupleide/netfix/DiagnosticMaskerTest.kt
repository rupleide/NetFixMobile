package com.rupleide.netfix

import com.rupleide.netfix.core.debug.DiagnosticMasker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class DiagnosticMaskerTest {

    @Test
    fun publicIpv4IsMaskedButPrivateAndResolversAreKept() {
        val masked = DiagnosticMasker.mask("ip 93.184.216.34 gw 192.168.1.1 dns 1.1.1.1 lo 127.0.0.1 cg 100.64.0.5 lan 10.0.0.2")
        assertEquals("ip x.x.x.x gw 192.168.1.1 dns 1.1.1.1 lo 127.0.0.1 cg 100.64.0.5 lan 10.0.0.2", masked)
    }

    @Test
    fun versionsAndTimestampsAreNotTouched() {
        assertEquals("[12:34:56.123] NetFix 1.0.4 build 10.0.28000", DiagnosticMasker.mask("[12:34:56.123] NetFix 1.0.4 build 10.0.28000"))
    }

    @Test
    fun globalIpv6IsMaskedAndLinkLocalIsKept() {
        val masked = DiagnosticMasker.mask("a 2a02:6b8::feed:0ff b fe80::1 c ::1")
        assertEquals("a x.x.x.x b fe80::1 c ::1", masked)
    }

    @Test
    fun secretsAndLongHexAreMasked() {
        val secret = "dd" + "ab12cd34".repeat(4)
        val masked = DiagnosticMasker.mask("tg://proxy?server=127.0.0.1&port=1443&secret=$secret and key ${"0f".repeat(20)}")
        assertFalse(masked.contains(secret))
        assertFalse(masked.contains("0f0f0f0f"))
        assertEquals("tg://proxy?server=127.0.0.1&port=1443&secret=*** and key ***", masked)
    }

    @Test
    fun outOfRangeOctetsAreLeftAlone() {
        assertEquals("999.1.1.1", DiagnosticMasker.mask("999.1.1.1"))
    }
}
