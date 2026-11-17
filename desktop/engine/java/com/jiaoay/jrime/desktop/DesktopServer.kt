@file:JvmName("DesktopServer")

package com.jiaoay.jrime.desktop

import com.jiaoay.jrime.core.Rime
import com.jiaoay.jrime.core.RimeKeyEvent
import java.io.BufferedReader
import java.io.FileDescriptor
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.PrintStream
import java.nio.charset.StandardCharsets

/**
 * Desktop engine server — the shared core behind every platform shell (the
 * macOS IMK client today; a Linux ibus adapter and a Windows TSF server later
 * all speak this same line protocol over their transport).
 *
 * One engine per process. Requests, one per line on stdin:
 *   ready | k <repr> | sel <index> | commit | pg next | pg prev |
 *   opt <name> <bool> | exit
 * Responses: one JSON line per request on stdout, the same shape as the
 * harness trace (doc/harness-design.md §4) plus "handled". librime logs go to
 * stderr; stdout carries protocol lines only.
 */
private const val VERSION = "1.0.0-desktop"

fun main(args: Array<String>) {
    var shared: String? = null
    var user: String? = null
    var schema = "flypy_xhfast"
    var i = 0
    while (i < args.size) {
        when (args[i]) {
            "--shared" -> shared = args[++i]
            "--user" -> user = args[++i]
            "--schema" -> schema = args[++i]
            else -> throw IllegalArgumentException("unknown arg: " + args[i])
        }
        i++
    }
    if (shared == null || user == null) {
        throw IllegalArgumentException("--shared/--user are required")
    }

    // stdout carries the protocol only: every Java-side print (the JNI
    // notification callback logs, anything else) goes to stderr instead.
    // The protocol stream below is bound to FileDescriptor.out directly,
    // so this redirect does not affect responses.
    System.setOut(PrintStream(FileOutputStream(FileDescriptor.err), true, StandardCharsets.UTF_8))

    System.loadLibrary("rime_jni")
    Rime.startupRime(shared, user, VERSION)

    // Ready probe: the session must have mounted the expected schema before
    // the shell sends its first key, or responses would sample a dead engine.
    var ready = false
    var probe = 0
    while (probe < 150 && !ready) {
        val st = Rime.getRimeStatus()
        if (schema == st.schemaId) {
            ready = true
        } else {
            Thread.sleep(200)
        }
        probe++
    }

    val out = PrintStream(FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8)
    val inp = BufferedReader(InputStreamReader(System.`in`, StandardCharsets.UTF_8))

    if (!ready) {
        out.println(
            "{\"error\":\"engine never mounted schema " + json(schema) +
                "\",\"messages\":" + json(java.lang.String.join(", ", Rime.messages)) + "}",
        )
        return
    }

    // Idle watchdog: a shell process that dies without "exit" (crash, force
    // quit) must not leave an orphaned JVM holding the user dir forever.
    val lastActivity = longArrayOf(System.currentTimeMillis())
    val watchdog = Thread {
        while (true) {
            try {
                Thread.sleep(60_000)
            } catch (e: InterruptedException) {
                return@Thread
            }
            if (System.currentTimeMillis() - lastActivity[0] > 600_000) {
                Rime.exitRime()
                System.exit(0)
            }
        }
    }
    watchdog.isDaemon = true
    watchdog.start()

    while (true) {
        val read = inp.readLine() ?: break
        lastActivity[0] = System.currentTimeMillis()
        val line = read.trim()
        if (line.isEmpty()) {
            continue
        }
        try {
            when {
                line == "exit" -> {
                    out.println(sample(line, true))
                    break
                }

                line == "ready" -> out.println(sample(line, true))

                line.startsWith("k ") -> {
                    val ev = RimeKeyEvent.parse(line.substring(2).trim())
                    val handled = Rime.processRimeKey(ev.keycode, ev.modifier)
                    out.println(sample(line, handled))
                }

                line.startsWith("sel ") -> {
                    val handled = Rime.selectRimeCandidate(Integer.parseInt(line.substring(4).trim()), false)
                    out.println(sample(line, handled))
                }

                line == "commit" -> {
                    val handled = Rime.commitRimeComposition()
                    out.println(sample(line, handled))
                }

                line == "pg next" -> out.println(sample(line, Rime.changeRimeCandidatePage(false)))

                line == "pg prev" -> out.println(sample(line, Rime.changeRimeCandidatePage(true)))

                line.startsWith("opt ") -> {
                    val parts = line.split(" +".toRegex()).toTypedArray()
                    Rime.setRimeOption(parts[1], java.lang.Boolean.parseBoolean(parts[2]))
                    out.println(sample(line, true))
                }

                else -> out.println("{\"error\":\"unknown request: " + json(line) + "\"}")
            }
        } catch (e: Exception) {
            out.println("{\"error\":\"" + json(java.lang.String.valueOf(e)) + "\"}")
        }
    }
    Rime.exitRime()
}

/** Samples the engine state; identical shape to the harness trace minus step. */
private fun sample(op: String, handled: Boolean): String {
    val ctx = Rime.getRimeContext()
    val st = Rime.getRimeStatus()
    val commit = Rime.getRimeCommit() // drains the commit buffer
    val comp = ctx.composition
    val preedit = comp?.preedit
    val sb = StringBuilder()
    sb.append("{\"op\":\"").append(json(op)).append('"')
        .append(",\"handled\":").append(handled)
        .append(",\"raw\":\"").append(json(Rime.getRimeRawInput())).append('"')
        .append(",\"preedit\":\"").append(json(preedit)).append('"')
        .append(",\"commit\":")
        .append(
            if (commit == null || commit.text == null) {
                "null"
            } else {
                "\"" + json(commit.text) + "\""
            },
        )
        .append(",\"ascii\":").append(st.isAsciiMode)
        .append(",\"composing\":").append(st.isComposing)
        .append(",\"schema\":\"").append(json(st.schemaId)).append('"')
        .append(",\"cands\":[")
    val cands = Rime.getRimeCandidates(0, 48)
    for (i in cands.indices) {
        if (i > 0) sb.append(',')
        sb.append("[\"").append(json(cands[i].text)).append("\",\"")
            .append(json(cands[i].comment)).append("\"]")
    }
    sb.append("]}")
    return sb.toString()
}

private fun json(s: String?): String {
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
