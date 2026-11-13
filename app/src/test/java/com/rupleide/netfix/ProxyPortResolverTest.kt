package com.rupleide.netfix

import com.rupleide.netfix.core.dpibypass.ProxyPortResolver
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetAddress
import java.net.ServerSocket

class ProxyPortResolverTest {
    private val loopback = "127.0.0.1"

    @Test
    fun freePortIsKept() {
        val free = ProxyPortResolver.findFreePort(loopback)
        assertEquals(free, ProxyPortResolver.resolve(loopback, free))
    }

    @Test
    fun busyPortIsReplacedWithFreeOne() {
        ServerSocket(0, 0, InetAddress.getByName(loopback)).use { occupied ->
            val chosen = ProxyPortResolver.resolve(loopback, occupied.localPort)
            assertNotEquals(occupied.localPort, chosen)
            assertTrue(ProxyPortResolver.isPortFree(loopback, chosen))
        }
    }

    @Test
    fun busyPortIsDetected() {
        ServerSocket(0, 0, InetAddress.getByName(loopback)).use { occupied ->
            assertFalse(ProxyPortResolver.isPortFree(loopback, occupied.localPort))
        }
    }

    @Test
    fun invalidPreferredPortFallsBackToFreeOne() {
        val chosen = ProxyPortResolver.resolve(loopback, 0)
        assertTrue(chosen in 1..65535)
    }

    @Test
    fun portIsReplacedInSeparateOption() {
        val args = arrayOf("ciadpi", "--ip", "127.0.0.1", "--port", "1080", "-o1")
        assertArrayEquals(
            arrayOf("ciadpi", "--ip", "127.0.0.1", "--port", "2000", "-o1"),
            ProxyPortResolver.withPort(args, 2000)
        )
    }

    @Test
    fun portIsReplacedInShortOptions() {
        assertArrayEquals(
            arrayOf("ciadpi", "-i127.0.0.1", "-p2000", "-c512"),
            ProxyPortResolver.withPort(arrayOf("ciadpi", "-i127.0.0.1", "-p1080", "-c512"), 2000)
        )
        assertArrayEquals(
            arrayOf("ciadpi", "-p", "2000"),
            ProxyPortResolver.withPort(arrayOf("ciadpi", "-p", "1080"), 2000)
        )
    }

    @Test
    fun portIsReplacedInEqualsForm() {
        assertArrayEquals(
            arrayOf("ciadpi", "--port=2000"),
            ProxyPortResolver.withPort(arrayOf("ciadpi", "--port=1080"), 2000)
        )
    }

    @Test
    fun portIsAppendedWhenMissing() {
        assertArrayEquals(
            arrayOf("ciadpi", "-o1", "--port", "2000"),
            ProxyPortResolver.withPort(arrayOf("ciadpi", "-o1"), 2000)
        )
    }

    @Test
    fun otherOptionsStartingWithPAreNotTouched() {
        assertArrayEquals(
            arrayOf("ciadpi", "-pxyz", "-p", "2000"),
            ProxyPortResolver.withPort(arrayOf("ciadpi", "-pxyz", "-p", "1080"), 2000)
        )
    }
}
