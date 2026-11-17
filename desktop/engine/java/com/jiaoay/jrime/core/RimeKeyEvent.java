package com.jiaoay.jrime.core;

public class RimeKeyEvent {
    public final int keycode;
    public final int modifier;
    public final String repr;

    public RimeKeyEvent(int keycode, int modifier, String repr) {
        this.keycode = keycode;
        this.modifier = modifier;
        this.repr = repr;
    }

    /** Implemented by key.cc (Java_com_jiaoay_jrime_core_RimeKeyEvent_parse). */
    public static native RimeKeyEvent parse(String repr);
}
