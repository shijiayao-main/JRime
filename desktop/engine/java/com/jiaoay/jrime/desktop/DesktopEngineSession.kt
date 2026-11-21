package com.jiaoay.jrime.desktop

import com.jiaoay.jrime.core.CommitProto
import com.jiaoay.jrime.core.Rime
import com.jiaoay.jrime.core.RimeDataAccess
import com.jiaoay.jrime.core.RimeKeyEvent
import com.jiaoay.jrime.protocol.EngineSnapshotSampler
import com.jiaoay.jrime.protocol.JsonLineCodec
import com.jiaoay.jrime.protocol.SchemaReadyProbe

/** One response line plus the commit text it carried (the shell commits it). */
data class EngineReply(
    val json: String,
    val commitText: String? = null,
)

/**
 * The line-protocol request handling of the desktop engine, separated from the
 * transport so two hosts can share it: the headless [DesktopServer] (harness,
 * tests, future shells) and the desktop app (the same engine in front of the
 * Compose candidate window).
 */
class DesktopEngineSession(
    private val version: String,
    private val protocolVersion: Int,
    private val schema: String,
) {
    private val sampler = EngineSnapshotSampler(RimeDataAccess)
    private val probe = SchemaReadyProbe(RimeDataAccess)

    fun awaitReady(): Boolean = probe.awaitSchemaReady(schema)

    fun lastMessages(): String = java.lang.String.join(", ", Rime.messages)

    /** Handles one request line; null means "exit". */
    fun handle(line: String): EngineReply? {
        when {
            line == "exit" -> return EngineReply(sampler.sampleLine(line, true))

            line == "ready" ->
                return EngineReply(
                    sampler.sampleLine(line, true, proto = protocolVersion, engine = version),
                )

            line.startsWith("k ") -> {
                val ev = RimeKeyEvent.parse(line.substring(2).trim())
                val handled = Rime.processRimeKey(ev.keycode, ev.modifier)
                return sample(line, handled)
            }

            line.startsWith("sel ") -> {
                val handled = Rime.selectRimeCandidate(Integer.parseInt(line.substring(4).trim()), false)
                return sampleCommitAware(line, handled)
            }

            line == "commit" -> {
                val handled = Rime.commitRimeComposition()
                return sampleCommitAware(line, handled)
            }

            line == "pg next" -> return sample(line, Rime.changeRimeCandidatePage(false))

            line == "pg prev" -> return sample(line, Rime.changeRimeCandidatePage(true))

            line.startsWith("opt ") -> {
                val parts = line.split(" +".toRegex()).toTypedArray()
                Rime.setRimeOption(parts[1], java.lang.Boolean.parseBoolean(parts[2]))
                return sample(line, true)
            }

            else ->
                return EngineReply(
                    "{\"error\":\"unknown request: " + JsonLineCodec.encode(line) + "\"}",
                )
        }
    }

    private fun sample(
        op: String,
        handled: Boolean,
    ): EngineReply = EngineReply(sampler.sampleLine(op, handled))

    private fun sampleCommitAware(
        op: String,
        handled: Boolean,
    ): EngineReply {
        // Read the commit first, then let the sampler render it: the app needs
        // the text to forward to the shell (the candidate click happens inside
        // the app process, not as a shell request).
        val commit: CommitProto? = RimeDataAccess.commit()
        return EngineReply(
            sampler.sampleLine(op, handled, commitOverride = commit),
            commit?.text?.takeIf { it.isNotEmpty() },
        )
    }
}
