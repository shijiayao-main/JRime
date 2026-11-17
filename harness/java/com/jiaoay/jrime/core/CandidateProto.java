package com.jiaoay.jrime.core;

public class CandidateProto {
    public final String text;
    public final String comment;
    public final String seqId;

    public CandidateProto(String text, String comment, String seqId) {
        this.text = text;
        this.comment = comment;
        this.seqId = seqId;
    }
}
