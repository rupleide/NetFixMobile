package com.rupleide.netfix.core.dpibypass

import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files
import java.util.concurrent.TimeUnit

class RootDpiRulesTest {
    private val rules = RootDpiRules(10400, setOf(10253, 10347), setOf("wlan0", "rmnet_data0"), 11080)

    @Test fun onlySelectedAppsOnPhysicalNetworksAreIntercepted() {
        val script = rules.guardian("/test/root-dpi.lock")
        val routing = script.lines().filter { "--uid-owner" in it }
        assertTrue(routing.isNotEmpty())
        assertTrue(routing.all { "--uid-owner 10253" in it || "--uid-owner 10347" in it })
        assertTrue(routing.all { "-o 'wlan0'" in it || "-o 'rmnet_data0'" in it })
        assertFalse(script.contains("tun0"))
        assertFalse(script.contains("--uid-owner 10400"))
        assertTrue(script.contains("--dports 80,443 -j REDIRECT --to-ports 11080"))
        assertTrue(script.contains("--dport 443 -j REJECT"))
        assertTrue(script.contains("-d 127.0.0.0/8 -j RETURN"))
    }

    @Test fun cleanupNeverFlushesOtherFirewallRules() {
        val lines = rules.cleanup().lines().filter { "iptables" in it || "ip6tables" in it }
        assertTrue(lines.all { "NFX_ROOT_10400" in it })
        assertFalse(lines.any { "-F OUTPUT" in it || "-X OUTPUT" in it || "-P " in it })
        assertTrue(rules.guardian("/test/root-dpi.lock").contains("cleanup || status=1"))
        assertTrue(rules.guardian("/test/root-dpi.lock").endsWith("while IFS= read -r command; do :; done\n"))
    }

    @Test fun rejectsShellInjectionAndProxyLoops() {
        for (iface in listOf("wlan0;id", "$(id)", "wlan0\nreboot", "a".repeat(16))) {
            assertThrows(IllegalArgumentException::class.java) { rules.copy(interfaces = setOf(iface)) }
        }
        assertThrows(IllegalArgumentException::class.java) { rules.copy(appUids = setOf(10400)) }
        assertThrows(IllegalArgumentException::class.java) { rules.copy(appUids = emptySet()) }
        assertThrows(IllegalArgumentException::class.java) { rules.copy(interfaces = emptySet()) }
        assertThrows(IllegalArgumentException::class.java) { rules.copy(port = 80) }
    }

    @Test fun guardianRemovesItsRulesOnPipeClosureAndPartialInstallFailure() {
        for (fail in listOf(false, true)) {
            val directory = Files.createTempDirectory("root-dpi-test").toFile()
            try {
                val tool = directory.resolve("iptables")
                tool.writeText("""
                    #!/bin/sh
                    set -eu
                    echo "${'$'}*" >> "${'$'}STATE/commands"
                    shift 2
                    table=${'$'}2
                    action=${'$'}3
                    chain=${'$'}4
                    marker="${'$'}STATE/${'$'}table-${'$'}chain"
                    case "${'$'}action" in
                      -S) test -e "${'$'}marker" ;;
                      -N) touch "${'$'}marker" ;;
                      -C) test -e "${'$'}STATE/${'$'}table-jump" ;;
                      -D) rm -f "${'$'}STATE/${'$'}table-jump" ;;
                      -I) if [ "${'$'}FAIL" = 1 ] && [ "${'$'}table" = nat ]; then exit 42; fi
                          touch "${'$'}STATE/${'$'}table-jump" ;;
                      -F) test -e "${'$'}marker" ;;
                      -X) rm -f "${'$'}marker" ;;
                    esac
                """.trimIndent())
                tool.setExecutable(true)
                val ipv6 = directory.resolve("ip6tables")
                ipv6.writeText(tool.readText())
                ipv6.setExecutable(true)
                directory.resolve("flock").apply { writeText("#!/bin/sh\ntest -f /dev/fd/0\n"); setExecutable(true) }
                val script = directory.resolve("guardian.sh").apply { writeText(rules.guardian(directory.resolve("root-dpi.lock").absolutePath)) }
                val builder = ProcessBuilder("sh", script.absolutePath).redirectErrorStream(true)
                builder.environment()["PATH"] = directory.absolutePath + ":" + System.getenv("PATH")
                builder.environment()["STATE"] = directory.absolutePath
                builder.environment()["FAIL"] = if (fail) "1" else "0"
                val process = builder.start()
                try {
                    if (!fail) assertEquals("READY", process.inputStream.bufferedReader().readLine())
                    process.outputStream.close()
                    assertTrue("guardian exits on EOF", process.waitFor(5, TimeUnit.SECONDS))
                    assertEquals(if (fail) 42 else 0, process.exitValue())
                    assertFalse(directory.listFiles()!!.any { it.name.endsWith("-jump") || "NFX_ROOT" in it.name })
                    val commands = directory.resolve("commands").readLines()
                    assertTrue(commands.filter { "--uid-owner" in it }.all {
                        ("--uid-owner 10253" in it || "--uid-owner 10347" in it) &&
                            ("-o wlan0" in it || "-o rmnet_data0" in it)
                    })
                } finally { process.destroyForcibly() }
            } finally { directory.deleteRecursively() }
        }
    }
}
