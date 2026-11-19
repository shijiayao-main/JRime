@file:JvmName("DesktopServer")

package com.jiaoay.jrime.desktop

import com.jiaoay.jrime.core.Rime
import com.jiaoay.jrime.core.RimeKeyEvent
import com.jiaoay.jrime.protocol.EngineSnapshotSampler
import com.jiaoay.jrime.protocol.JsonLineCodec
import com.jiaoay.jrime.protocol.SchemaReadyProbe
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
    val ready = SchemaReadyProbe.awaitSchemaReady(schema)

    val out = PrintStream(FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8)
    val inp = BufferedReader(InputStreamReader(System.`in`, StandardCharsets.UTF_8))

    if (!ready) {
        out.println(
            "{\"error\":\"engine never mounted schema " + JsonLineCodec.encode(schema) +
                "\",\"messages\":" + JsonLineCodec.encode(java.lang.String.join(", ", Rime.messages)) + "}",
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
                    out.println(EngineSnapshotSampler.sampleLine(line, true))
                    break
                }

                line == "ready" -> out.println(EngineSnapshotSampler.sampleLine(line, true))

                line.startsWith("k ") -> {
                    val ev = RimeKeyEvent.parse(line.substring(2).trim())
                    val handled = Rime.processRimeKey(ev.keycode, ev.modifier)
                    out.println(EngineSnapshotSampler.sampleLine(line, handled))
                }

                line.startsWith("sel ") -> {
                    val handled = Rime.selectRimeCandidate(Integer.parseInt(line.substring(4).trim()), false)
                    out.println(EngineSnapshotSampler.sampleLine(line, handled))
                }

                line == "commit" -> {
                    val handled = Rime.commitRimeComposition()
                    out.println(EngineSnapshotSampler.sampleLine(line, handled))
                }

                line == "pg next" -> out.println(EngineSnapshotSampler.sampleLine(line, Rime.changeRimeCandidatePage(false)))

                line == "pg prev" -> out.println(EngineSnapshotSampler.sampleLine(line, Rime.changeRimeCandidatePage(true)))

                line.startsWith("opt ") -> {
                    val parts = line.split(" +".toRegex()).toTypedArray()
                    Rime.setRimeOption(parts[1], java.lang.Boolean.parseBoolean(parts[2]))
                    out.println(EngineSnapshotSampler.sampleLine(line, true))
                }

                else -> out.println("{\"error\":\"unknown request: " + JsonLineCodec.encode(line) + "\"}")
            }
        } catch (e: Exception) {
            out.println("{\"error\":\"" + JsonLineCodec.encode(java.lang.String.valueOf(e)) + "\"}")
        }
    }
    Rime.exitRime()
}
