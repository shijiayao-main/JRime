package com.jiaoay.jrime.core

class Candidates {
    class Paged(
        finished: Boolean,
        lastPageSize: Boolean,
        reverse: Boolean,
        pageSize: Int,
        candidates: Array<CandidateProto>,
    )

    class Bulk(
        selectKeysSize: Int,
        selectedIndex: Int,
        candidates: Array<CandidateProto>,
    )
}
