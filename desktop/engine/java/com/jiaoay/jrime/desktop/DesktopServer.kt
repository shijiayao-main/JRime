@file:JvmName("DesktopServer")

package com.jiaoay.jrime.desktop

import com.jiaoay.jrime.core.Rime
import com.jiaoay.jrime.protocol.JsonLineCodec
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
 * harness trace minus the step field, plus "handled". The "ready" response
 * additionally carries the handshake fields "proto" (protocol version, only
 * ever incremented) and "engine" (server version). librime logs go to
 * stderr; stdout carries protocol lines only.
 */
private const val VERSION = "1.0.0-desktop"
private const val PROTOCOL_VERSION = 1

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
    val session = DesktopEngineSession(VERSION, PROTOCOL_VERSION, schema)
    val ready = session.awaitReady()

    val out = PrintStream(FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8)
    val inp = BufferedReader(InputStreamReader(System.`in`, StandardCharsets.UTF_8))

    if (!ready) {
        out.println(
            "{\"error\":\"engine never mounted schema " + JsonLineCodec.encode(schema) +
                "\",\"messages\":" + JsonLineCodec.encode(session.lastMessages()) + "}",
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
            val reply = session.handle(line)
            if (reply == null) {
                break
            }
            out.println(reply.json)
            if (line == "exit") {
                break
            }
        } catch (e: Exception) {
            out.println("{\"error\":\"" + JsonLineCodec.encode(java.lang.String.valueOf(e)) + "\"}")
        }
    }
    Rime.exitRime()
}
