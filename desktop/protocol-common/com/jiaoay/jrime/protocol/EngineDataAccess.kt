package com.jiaoay.jrime.protocol

import com.jiaoay.jrime.core.CandidateProto
import com.jiaoay.jrime.core.CommitProto
import com.jiaoay.jrime.core.ContextProto
import com.jiaoay.jrime.core.StatusProto

/**
 * The data surface the protocol sampler reads from. The protocol module must
 * not know whether it is talking to the librime JNI bridge, the harness stub or
 * a future Kotlin engine, so every engine implementation supplies this seam
 * (dependency inversion; doc/multiplatform-design.md §1.3 Step 2).
 */
interface EngineDataAccess {
    fun rawInput(): String

    fun context(): ContextProto?

    fun status(): StatusProto

    /** Drains the pending commit buffer (null when there is nothing). */
    fun commit(): CommitProto?

    fun candidates(
        start: Int,
        limit: Int,
    ): Array<CandidateProto>
}
