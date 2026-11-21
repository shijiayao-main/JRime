package com.jiaoay.jrime.protocol

/**
 * Samples one engine snapshot as the canonical protocol JSON line
 * (doc/harness-design.md §4). Single source of truth shared by the engine
 * server (no step field) and the harness replay driver (step-prefixed trace),
 * so the golden trace and live responses keep the same shape minus step.
 *
 * The engine is reached through [EngineDataAccess], never directly: the same
 * sampler serves the librime JNI bridge today and a Kotlin engine later.
 */
class EngineSnapshotSampler(
    private val access: EngineDataAccess,
) {
    fun sampleLine(
        op: String,
        handled: Boolean,
        step: Int? = null,
        proto: Int? = null,
        engine: String? = null,
    ): String {
        val ctx = access.context()
        val st = access.status()
        val commit = access.commit() // drains the commit buffer
        val comp = ctx?.composition
        val preedit = comp?.preedit
        val sb = StringBuilder()
        sb.append("{")
        if (step != null) sb.append("\"step\":").append(step).append(',')
        sb.append("\"op\":\"").append(JsonLineCodec.encode(op)).append('"')
            .append(",\"handled\":").append(handled)
            .append(",\"raw\":\"").append(JsonLineCodec.encode(access.rawInput())).append('"')
            .append(",\"preedit\":\"").append(JsonLineCodec.encode(preedit)).append('"')
            .append(",\"commit\":")
            .append(
                if (commit == null || commit.text == null) {
                    "null"
                } else {
                    "\"" + JsonLineCodec.encode(commit.text) + "\""
                },
            )
            .append(",\"ascii\":").append(st.isAsciiMode)
            .append(",\"composing\":").append(st.isComposing)
            .append(",\"schema\":\"").append(JsonLineCodec.encode(st.schemaId)).append('"')
        if (proto != null) sb.append(",\"proto\":").append(proto)
        if (engine != null) sb.append(",\"engine\":\"").append(JsonLineCodec.encode(engine)).append('"')
        sb.append(",\"cands\":[")
        val cands = access.candidates(0, 48)
        for (i in cands.indices) {
            if (i > 0) sb.append(',')
            sb.append("[\"").append(JsonLineCodec.encode(cands[i].text)).append("\",\"")
                .append(JsonLineCodec.encode(cands[i].comment)).append("\"]")
        }
        sb.append("]}")
        return sb.toString()
    }
}
