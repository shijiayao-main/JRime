package com.jiaoay.jrime.protocol

/**
 * Canonical JSON string encoding for the desktop protocol lines
 * (doc/harness-design.md §4). Single source of truth shared by the engine
 * server and the harness replay driver, so live responses and golden traces
 * stay byte-identical by construction instead of by two copies.
 */
object JsonLineCodec {
    fun encode(s: String?): String {
        if (s == null) return ""
        val b = StringBuilder()
        for (c in s.toCharArray()) {
            when (c) {
                '"' -> b.append("\\\"")

                '\\' -> b.append("\\\\")

                '\n' -> b.append("\\n")

                '\r' -> b.append("\\r")

                '\t' -> b.append("\\t")

                else -> {
                    if (c.code < 0x20) b.append(String.format("\\u%04x", c.code)) else b.append(c)
                }
            }
        }
        return b.toString()
    }
}
