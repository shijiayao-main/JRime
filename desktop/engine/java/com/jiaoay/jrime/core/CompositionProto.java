package com.jiaoay.jrime.core;

public class CompositionProto {
    public final int preeditStart;
    public final int preeditCursor;
    public final int selStart;
    public final int selEnd;
    public final String preedit;
    public final String selStartText;

    public CompositionProto(int preeditStart, int preeditCursor, int selStart, int selEnd,
                            String preedit, String selStartText) {
        this.preeditStart = preeditStart;
        this.preeditCursor = preeditCursor;
        this.selStart = selStart;
        this.selEnd = selEnd;
        this.preedit = preedit;
        this.selStartText = selStartText;
    }
}
