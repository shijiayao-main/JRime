package com.jiaoay.jrime.core;

public class Candidates {
    public static class Paged {
        public Paged(boolean finished, boolean lastPageSize, boolean reverse,
                     int pageSize, CandidateProto[] candidates) {}
    }

    public static class Bulk {
        public Bulk(int selectKeysSize, int selectedIndex, CandidateProto[] candidates) {}
    }
}
