package com.jiaoay.jrime.core;

public class ContextProto {
    public final CompositionProto composition;
    public final String aux;
    public final int selEnd;

    public ContextProto(CompositionProto composition, String aux, int selEnd) {
        this.composition = composition;
        this.aux = aux;
        this.selEnd = selEnd;
    }
}
