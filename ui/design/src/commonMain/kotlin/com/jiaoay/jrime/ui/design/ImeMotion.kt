/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ui.design

/**
 * The single source of IME motion durations (in milliseconds) and edge easing.
 * Components must not hardcode animation lengths.
 */
object ImeMotion {
    /** Key press visual feedback. */
    const val PRESS: Int = 80

    /** Candidate highlight / page flip. */
    const val CANDIDATE: Int = 120

    /** Panel enter. */
    const val PANEL_ENTER: Int = 200

    /** Panel exit; slightly faster than enter feels responsive. */
    const val PANEL_EXIT: Int = 160

    /** Long-press entering the cursor-slide mode. */
    const val LONG_PRESS_ENTER: Int = 350

    /** Default long-press timeout. */
    const val LONG_PRESS_TIMEOUT: Int = 500

    /** Default key-repeat interval. */
    const val REPEAT_INTERVAL: Int = 80

    /** Double-space window for the full-stop gesture. */
    const val DOUBLE_SPACE_WINDOW_MIN: Int = 120
    const val DOUBLE_SPACE_WINDOW_MAX: Int = 350
}
