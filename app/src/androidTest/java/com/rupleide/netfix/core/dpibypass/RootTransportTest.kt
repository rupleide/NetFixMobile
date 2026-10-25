package com.rupleide.netfix.core.dpibypass

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rupleide.netfix.MainActivity
import com.rupleide.netfix.data.RESUME_ACTION
import com.rupleide.netfix.data.START_ACTION
import com.rupleide.netfix.core.tgproxy.TgProxyController
import org.junit.Before
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.concurrent.thread

@RunWith(AndroidJUnit4::class)
class RootTransportTest {
    private val startupKeys = listOf("byedpi_root_mode", "auto_connect_on_start", "service_enabled")
    private var savedStartup = emptyMap<String, Boolean?>()

    @Before fun stopApplicationServices() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val preferences = context.getPreferences()
        savedStartup = startupKeys.associateWith { if (preferences.contains(it)) preferences.getBoolean(it, false) else null }
        preferences.edit().apply { startupKeys.forEach { putBoolean(it, false) } }.commit()
        context.stopService(Intent(context, RootDpiService::class.java))
        context.stopService(Intent(context, ByeDpiVpnService::class.java))
        TgProxyController.stop()
        instrumentation.waitForIdleSync()
        await("application services stopped", { !ServiceManager.isAlive })
    }

    @After fun restoreStartupPreferences() {
        InstrumentationRegistry.getInstrumentation().targetContext.getPreferences().edit().apply {
            savedStartup.forEach { (key, value) -> if (value == null) remove(key) else putBoolean(key, value) }
        }.commit()
    }

    @Volatile private var lastSocketError = ""
    private val exits = java.util.concurrent.ConcurrentLinkedQueue<String>()
    private fun launchProxy(proxy: ByeDpiProxy, port: Int) = thread(name = "test-byedpi") {
        val result = proxy.startProxy(ByeDpiProxyCmdPreferences(arrayOf("ciadpi", "--ip", "127.0.0.1", "--port", port.toString(), "--debug", "1")))
        exits.add("port $port exited $result")
    }

    private fun socksResponds(port: Int): Boolean = try {
        Socket().use { socket ->
            socket.connect(InetSocketAddress("127.0.0.1", port), 200)
            socket.soTimeout = 200
            socket.getOutputStream().write(byteArrayOf(5, 1, 0))
            socket.getInputStream().read() == 5 && socket.getInputStream().read() == 0
        }
    } catch (e: java.io.IOException) { lastSocketError = e.toString(); false }

    private fun await(message: String, condition: () -> Boolean) {
        val end = android.os.SystemClock.elapsedRealtime() + 5000
        while (!condition() && android.os.SystemClock.elapsedRealtime() < end) Thread.sleep(25)
        assertTrue("$message: $lastSocketError; $exits", condition())
    }

    @Test fun rejectedOwnerCannotStopOrCloseTheActiveEngine() {
        val first = ByeDpiProxy()
        val second = ByeDpiProxy()
        val port = 11181
        val firstThread = launchProxy(first, port)
        var secondThread: Thread? = null
        try {
            await("first engine responds", { socksResponds(port) })
            assertEquals(-1, second.startProxy(ByeDpiProxyCmdPreferences(arrayOf("ciadpi"))))
            assertEquals(-1, second.stopProxy())
            assertEquals(-1, second.jniForceClose())
            assertTrue("foreign teardown leaves first engine alive", socksResponds(port))
            first.stopProxy()
            firstThread.join(2000)
            assertFalse(firstThread.isAlive)
            secondThread = launchProxy(second, port)
            await("lease released after JNI returns", { socksResponds(port) })
        } finally {
            first.stopProxy()
            second.stopProxy()
            firstThread.join(2000)
            secondThread?.join(2000)
        }
    }

    @Suppress("DEPRECATION")
    @Test fun disabledRootRejectsStartAndStaleResumeWithoutStoppingAnotherEngine() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val preferences = context.getPreferences()
        val keys = listOf("byedpi_root_mode", "auto_connect_on_start", "service_enabled")
        val saved = keys.associateWith { if (preferences.contains(it)) preferences.getBoolean(it, false) else null }
        preferences.edit().apply { keys.forEach { putBoolean(it, false) } }.commit()
        val proxy = ByeDpiProxy()
        val port = 11182
        val proxyThread = launchProxy(proxy, port)
        var activity: ActivityScenario<MainActivity>? = null
        try {
            await("independent engine responds", { socksResponds(port) })
            activity = ActivityScenario.launch(MainActivity::class.java)
            for (action in listOf(START_ACTION, RESUME_ACTION)) {
                context.startService(Intent(context, RootDpiService::class.java).setAction(action))
                instrumentation.waitForIdleSync()
                await("disabled root service exits") {
                    context.getSystemService(ActivityManager::class.java).getRunningServices(Int.MAX_VALUE)
                        .none { it.service.className == RootDpiService::class.java.name }
                }
                instrumentation.waitForIdleSync()
                assertFalse(RootDpiService.isAlive)
                assertTrue("root rejection leaves independent engine running", socksResponds(port))
            }
        } finally {
            activity?.close()
            proxy.stopProxy()
            proxyThread.join(2000)
            preferences.edit().apply {
                saved.forEach { (key, value) -> if (value == null) remove(key) else putBoolean(key, value) }
            }.commit()
        }
    }
}
