package com.rupleide.netfix.core.dpibypass

/** Only HTTP(S) on physical networks is intercepted; another VPN keeps its routes. */
internal data class RootDpiRules(
    val ownerUid: Int,
    val appUids: Set<Int>,
    val interfaces: Set<String>,
    val port: Int,
) {
    init {
        require(ownerUid > 0 && appUids.isNotEmpty() && appUids.all { it > 0 && it != ownerUid })
        require(interfaces.isNotEmpty() && interfaces.all { it.matches(Regex("[a-zA-Z0-9_.:-]{1,15}")) })
        require(port in 1024..65535)
    }

    private val chain = "NFX_ROOT_$ownerUid"

    fun cleanup(): String = buildString {
        appendLine("cleanup_result=0")
        for ((tool, table) in listOf("iptables" to "nat", "iptables" to "filter", "ip6tables" to "filter")) {
            appendLine("if $tool -w 5 -t $table -S $chain >/dev/null 2>&1; then")
            appendLine("while $tool -w 5 -t $table -C OUTPUT -j $chain 2>/dev/null; do")
            appendLine("$tool -w 5 -t $table -D OUTPUT -j $chain || { cleanup_result=1; break; }")
            appendLine("done")
            appendLine("$tool -w 5 -t $table -F $chain || cleanup_result=1")
            appendLine("$tool -w 5 -t $table -X $chain || cleanup_result=1")
            appendLine("fi")
        }
        appendLine("return \"\$cleanup_result\"")
    }

    fun guardian(lockFile: String): String = buildString {
        require(lockFile.startsWith("/") && '\n' !in lockFile && '\u0000' !in lockFile)
        appendLine("#!/system/bin/sh")
        appendLine("set -eu")
        appendLine("exec 9>>'${lockFile.replace("'", "'\\''")}'")
        // Android mksh marks persistent descriptors >2 close-on-exec; pass the same file as fd 0.
        appendLine("flock -n 0 <&9 || { echo 'Cannot acquire root session lock'; exit 75; }")
        appendLine("cleanup() {")
        append(cleanup())
        appendLine("}")
        appendLine("trap 'status=\$?; trap - EXIT; cleanup || status=1; exit \"\$status\"' EXIT")
        appendLine("trap 'exit 0' HUP INT TERM")
        appendLine("cleanup")
        appendLine("iptables -w 5 -t nat -N $chain")
        appendLine("iptables -w 5 -t filter -N $chain")
        appendLine("ipv6=0")
        appendLine("if [ -e /proc/net/if_inet6 ]; then ip6tables -w 5 -t filter -N $chain; ipv6=1; fi")
        appendLine("iptables -w 5 -t nat -A $chain -d 127.0.0.0/8 -j RETURN")
        for (uid in appUids.sorted()) for (iface in interfaces.sorted()) {
            val match = "-m owner --uid-owner $uid -o '$iface'"
            appendLine("iptables -w 5 -t nat -A $chain $match -p tcp -m multiport --dports 80,443 -j REDIRECT --to-ports $port")
            appendLine("iptables -w 5 -t filter -A $chain $match -p udp --dport 443 -j REJECT --reject-with icmp-port-unreachable")
            // Some stock kernels have no IPv6 NAT. Let clients fall back to IPv4/TCP.
            appendLine("if [ \"\$ipv6\" = 1 ]; then")
            appendLine("ip6tables -w 5 -t filter -A $chain $match -p tcp -m multiport --dports 80,443 -j REJECT --reject-with tcp-reset")
            appendLine("ip6tables -w 5 -t filter -A $chain $match -p udp --dport 443 -j REJECT --reject-with icmp6-port-unreachable")
            appendLine("fi")
        }
        // Publish only complete chains; a partial install is rolled back by the EXIT trap.
        appendLine("iptables -w 5 -t filter -I OUTPUT 1 -j $chain")
        appendLine("if [ \"\$ipv6\" = 1 ]; then ip6tables -w 5 -t filter -I OUTPUT 1 -j $chain; fi")
        appendLine("iptables -w 5 -t nat -I OUTPUT 1 -j $chain")
        appendLine("echo READY")
        // The app owns the pipe. EOF on process death also removes the rules.
        appendLine("while IFS= read -r command; do :; done")
    }
}
