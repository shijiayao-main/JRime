package com.jiaoay.jrime.core

import com.jiaoay.jrime.protocol.EngineDataAccess

/**
 * The librime bridge implementation of the protocol's engine seam. Kept next
 * to the JNI facade so the protocol module stays engine-agnostic.
 */
object RimeDataAccess : EngineDataAccess {
    override fun rawInput(): String = Rime.getRimeRawInput()

    override fun context(): ContextProto? = Rime.getRimeContext()

    override fun status(): StatusProto = Rime.getRimeStatus()

    override fun commit(): CommitProto? = Rime.getRimeCommit()

    override fun candidates(
        start: Int,
        limit: Int,
    ): Array<CandidateProto> = Rime.getRimeCandidates(start, limit)
}
