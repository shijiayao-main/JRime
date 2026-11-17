@file:JvmName("Replay")

import com.jiaoay.jrime.core.Rime
import com.jiaoay.jrime.core.RimeKeyEvent
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.nio.charset.StandardCharsets

/**
 * Harness H1 engine driver (doc/harness-design.md §3/§4): replays one corpus
 * case against librime through the desktop JNI bridge, sampling a JSONL trace
 * after every step. One engine per process — cases are isolated by fresh user
 * dirs; the shared staging dir is prepared by run_cases.py.
 *
 * Step tokens: k:<repr> (librime key repr via RimeKeyEvent.parse), sel:<idx>,
 * commit, pg:next, pg:prev, opt:<name>=<bool>.
 */
fun main(args: Array<String>) {
    var shared: String? = null
    var user: String? = null
    var out: String? = null
    var schema = "flypy_xhfast"
    var steps: List<String> = listOf()
    var i = 0
    while (i < args.size) {
        when (args[i]) {
            "--shared" -> shared = args[++i]

            "--user" -> user = args[++i]

            "--out" -> out = args[++i]

            "--schema" -> schema = args[++i]

            "--steps" -> {
                val raw = args[++i]
                steps = if (raw.isBlank()) listOf() else raw.split(",")
            }

            else -> throw IllegalArgumentException("unknown arg: " + args[i])
        }
        i++
    }
    if (shared == null || user == null || out == null) {
        throw IllegalArgumentException("--shared/--user/--out are required")
    }

    System.loadLibrary("rime_jni")
    Rime.startupRime(shared, user, "1.0.0-harness")

    // Ready probe: the session must have mounted the expected schema before
    // the first key goes in, or the trace would sample a dead engine.
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
    if (!ready) {
        System.err.println(
            "engine never mounted schema " + schema +
                "; messages=" + Rime.messages,
        )
        System.exit(2)
    }

    PrintWriter(OutputStreamWriter(FileOutputStream(out), StandardCharsets.UTF_8)).use { w ->
        sample(w, 0, "start", true)
        var n = 1
        for (op in steps) {
            val handled = apply(op.trim())
            sample(w, n, op.trim(), handled)
            n++
        }
        sample(w, n, "end", true)
    }
    Rime.exitRime()
    System.out.println("REPLAY OK")
}

/** Applies one op; returns whether the engine consumed it (false = the app layer would act). */
private fun apply(op: String): Boolean {
    if (op.startsWith("k:")) {
        val ev = RimeKeyEvent.parse(op.substring(2))
        return Rime.processRimeKey(ev.keycode, ev.modifier)
    }
    if (op.startsWith("sel:")) {
        return Rime.selectRimeCandidate(Integer.parseInt(op.substring(4)), false)
    }
    if (op == "commit") {
        return Rime.commitRimeComposition()
    }
    if (op == "pg:next") {
        return Rime.changeRimeCandidatePage(false)
    }
    if (op == "pg:prev") {
        return Rime.changeRimeCandidatePage(true)
    }
    if (op.startsWith("opt:")) {
        val eq = op.indexOf('=')
        if (eq < 0) throw IllegalArgumentException("opt needs name=value: " + op)
        Rime.setRimeOption(op.substring(4, eq), java.lang.Boolean.parseBoolean(op.substring(eq + 1)))
        return true
    }
    throw IllegalArgumentException("unknown op: " + op)
}

private fun sample(w: PrintWriter, step: Int, op: String, handled: Boolean) {
    val ctx = Rime.getRimeContext()
    val st = Rime.getRimeStatus()
    val commit = Rime.getRimeCommit() // drains the commit buffer
    val comp = ctx.composition
    val preedit = comp?.preedit
    val sb = StringBuilder()
    sb.append("{\"step\":").append(step)
        .append(",\"op\":\"").append(json(op)).append('"')
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
    w.println(sb.toString())
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
