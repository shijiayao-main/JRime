package com.jiaoay.jrime.core

class RimeKeyEvent(
    val keycode: Int,
    val modifier: Int,
    val repr: String?,
) {
    companion object {
        /** Implemented by key.cc (Java_com_jiaoay_jrime_core_RimeKeyEvent_parse). */
        @JvmStatic
        external fun parse(repr: String): RimeKeyEvent
    }
}
