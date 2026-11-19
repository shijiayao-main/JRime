/*
 * SPDX-FileCopyrightText: 2015 - 2025 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.core

/**
 * Bridge the engine calls when a deploy starts and the OpenCC conversion
 * dictionaries need rebuilding. Implemented on the ime/daemon side, so the
 * engine core carries no data-layer knowledge.
 */
fun interface RimeOpenCCBridge {
    fun buildOpenCCDict()
}
