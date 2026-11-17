package com.jiaoay.jrime.core;

public class RimeResponse {
    public final CommitProto commit;
    public final CompositionProto composition;
    public final Candidates candidates;
    public final StatusProto status;

    public RimeResponse(CommitProto commit, CompositionProto composition,
                        Candidates candidates, StatusProto status) {
        this.commit = commit;
        this.composition = composition;
        this.candidates = candidates;
        this.status = status;
    }
}
