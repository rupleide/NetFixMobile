package com.rupleide.netfix

import com.rupleide.netfix.core.tgproxy.TgDcAddresses
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TgDcAddressesTest {

    private val flowsealSource = """
    ap.add_argument('--dc-ip', metavar='DC:IP', nargs='?', action='append', const=None,
    if args.dc_ip is None:
        args.dc_ip = [
            '2:149.154.167.220',
            '4:149.154.167.220',
        ]
    elif None in args.dc_ip:
        args.dc_ip = []
    """

    @Test
    fun readsDefaultsFromTgWsProxySource() {
        assertEquals("2:149.154.167.220,4:149.154.167.220", TgDcAddresses.parseDefaults(flowsealSource))
    }

    @Test
    fun picksUpChangedAddresses() {
        val changed = flowsealSource.replace("4:149.154.167.220", "4:149.154.167.91")
        assertEquals("2:149.154.167.220,4:149.154.167.91", TgDcAddresses.parseDefaults(changed))
    }

    @Test
    fun ignoresSourceWithoutDefaults() {
        assertNull(TgDcAddresses.parseDefaults("print('hello')"))
        assertNull(TgDcAddresses.parseDefaults("if args.dc_ip is None:\n    args.dc_ip = []\nelif x:"))
    }

    @Test
    fun validatesAddressList() {
        assertTrue(TgDcAddresses.isValid("2:149.154.167.220,4:149.154.167.220"))
        assertFalse(TgDcAddresses.isValid("2:999.1.1.1"))
        assertFalse(TgDcAddresses.isValid("9:1.1.1.1"))
        assertFalse(TgDcAddresses.isValid(""))
    }
}
