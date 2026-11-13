package com.rupleide.netfix

import com.rupleide.netfix.ui.main.ActiveConnection
import com.rupleide.netfix.ui.main.ConnectionTransport
import com.rupleide.netfix.ui.main.connectionLabel
import com.rupleide.netfix.ui.main.isNetworkAssistantManufacturer
import com.rupleide.netfix.ui.main.shouldShowNetworkAssistantHint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ActiveConnectionTest {
    private val wifiWithMobile = ActiveConnection(ConnectionTransport.Wifi, "Wi-Fi", true)

    @Test
    fun labelsMatchTransport() {
        assertEquals("Wi-Fi", connectionLabel(ConnectionTransport.Wifi, null))
        assertEquals("Кабель: Ethernet", connectionLabel(ConnectionTransport.Ethernet, null))
        assertEquals("Мобильная: Билайн", connectionLabel(ConnectionTransport.Mobile, "Билайн"))
        assertEquals("Мобильная сеть", connectionLabel(ConnectionTransport.Mobile, " "))
        assertEquals("Мобильная сеть", connectionLabel(ConnectionTransport.Mobile, null))
        assertNull(connectionLabel(ConnectionTransport.None, "Билайн"))
    }

    @Test
    fun onlyXiaomiFamilyIsRecognised() {
        assertTrue(isNetworkAssistantManufacturer("Xiaomi"))
        assertTrue(isNetworkAssistantManufacturer("realme"))
        assertTrue(isNetworkAssistantManufacturer("POCO"))
        assertTrue(isNetworkAssistantManufacturer("Redmi"))
        assertFalse(isNetworkAssistantManufacturer("samsung"))
        assertFalse(isNetworkAssistantManufacturer("Google"))
    }

    @Test
    fun hintRequiresXiaomiWifiAndMobileAvailable() {
        assertTrue(shouldShowNetworkAssistantHint("Xiaomi", wifiWithMobile, false))
        assertFalse(shouldShowNetworkAssistantHint("samsung", wifiWithMobile, false))
        assertFalse(shouldShowNetworkAssistantHint("Xiaomi", wifiWithMobile, true))
        assertFalse(shouldShowNetworkAssistantHint("Xiaomi", wifiWithMobile.copy(cellularAlsoAvailable = false), false))
        assertFalse(shouldShowNetworkAssistantHint("Xiaomi", wifiWithMobile.copy(transport = ConnectionTransport.Mobile), false))
        assertFalse(shouldShowNetworkAssistantHint("Xiaomi", wifiWithMobile.copy(transport = ConnectionTransport.Ethernet), false))
    }
}
