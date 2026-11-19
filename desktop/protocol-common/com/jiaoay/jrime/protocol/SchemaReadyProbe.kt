package com.jiaoay.jrime.protocol

import com.jiaoay.jrime.core.Rime

/**
 * Ready probe shared by the engine server and the harness replay driver: the
 * session must have mounted the expected schema before the first request, or
 * responses would sample a dead engine. Polls the schema up to [probe] times
 * at [interval] ms and returns whether it came up.
 */
object SchemaReadyProbe {
    fun awaitSchemaReady(schema: String, probe: Int = 150, interval: Long = 200): Boolean {
        var ready = false
        var attempts = 0
        while (attempts < probe && !ready) {
            val st = Rime.getRimeStatus()
            if (schema == st.schemaId) {
                ready = true
            } else {
                Thread.sleep(interval)
            }
            attempts++
        }
        return ready
    }
}
