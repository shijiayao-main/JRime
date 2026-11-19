@file:JvmName("Replay")

import com.jiaoay.jrime.core.Rime
import com.jiaoay.jrime.core.RimeKeyEvent
import com.jiaoay.jrime.protocol.EngineSnapshotSampler
import com.jiaoay.jrime.protocol.SchemaReadyProbe
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
    val ready = SchemaReadyProbe.awaitSchemaReady(schema)
    if (!ready) {
        System.err.println(
            "engine never mounted schema " + schema +
                "; messages=" + Rime.messages,
        )
        System.exit(2)
    }

    PrintWriter(OutputStreamWriter(FileOutputStream(out), StandardCharsets.UTF_8)).use { w ->
        w.println(EngineSnapshotSampler.sampleLine("start", true, 0))
        var n = 1
        for (op in steps) {
            val handled = apply(op.trim())
            w.println(EngineSnapshotSampler.sampleLine(op.trim(), handled, n))
            n++
        }
        w.println(EngineSnapshotSampler.sampleLine("end", true, n))
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
