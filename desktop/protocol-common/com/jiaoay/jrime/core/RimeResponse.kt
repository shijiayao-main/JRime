package com.jiaoay.jrime.core

class RimeResponse(
    val commit: CommitProto?,
    val composition: CompositionProto?,
    val candidates: Candidates?,
    val status: StatusProto?,
)
