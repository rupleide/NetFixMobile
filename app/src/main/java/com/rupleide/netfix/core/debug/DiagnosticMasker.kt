package com.rupleide.netfix.core.debug

object DiagnosticMasker {
    private const val MASKED_ADDRESS = "x.x.x.x"
    private const val MASKED_SECRET = "***"

    private val ipv4 = Regex("(?<![\\d.])(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})(?![\\d.]*\\d)")
    private val ipv6 = Regex("(?<![\\w:])[0-9A-Fa-f]{0,4}(?::[0-9A-Fa-f]{0,4}){2,7}(?![\\w:])")
    private val secretValue = Regex("(secret[_a-zA-Z]*\\s*[=:]\\s*\"?)[^\\s\"&,]+", RegexOption.IGNORE_CASE)
    private val longHex = Regex("(?<![0-9A-Fa-f])[0-9A-Fa-f]{32,}(?![0-9A-Fa-f])")
    private val keepResolvers = setOf("1.1.1.1", "1.0.0.1", "8.8.8.8", "8.8.4.4", "9.9.9.9", "77.88.8.8")

    fun mask(text: String): String {
        var result = secretValue.replace(text) { it.groupValues[1] + MASKED_SECRET }
        result = longHex.replace(result, MASKED_SECRET)
        result = ipv4.replace(result) { match ->
            val octets = match.groupValues.drop(1).map { it.toInt() }
            when {
                octets.any { it > 255 } -> match.value
                match.value in keepResolvers -> match.value
                isNonPublic(octets) -> match.value
                else -> MASKED_ADDRESS
            }
        }
        result = ipv6.replace(result) { match -> if (shouldMaskIpv6(match.value)) MASKED_ADDRESS else match.value }
        return result
    }

    private fun isNonPublic(octets: List<Int>): Boolean {
        val (a, b) = octets[0] to octets[1]
        return a == 0 || a == 10 || a == 127 ||
            (a == 169 && b == 254) ||
            (a == 172 && b in 16..31) ||
            (a == 192 && b == 168) ||
            (a == 100 && b in 64..127)
    }

    private fun shouldMaskIpv6(value: String): Boolean {
        val lower = value.lowercase()
        val looksLikeAddress = lower.contains("::") || lower.count { it == ':' } >= 5
        if (!looksLikeAddress) return false
        if (lower == "::1" || lower == "::") return false
        if (lower.startsWith("fe80") || lower.startsWith("fc") || lower.startsWith("fd")) return false
        return true
    }
}
