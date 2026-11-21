/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ui.main.settings

import com.jiaoay.jrime.daemon.RimeDaemon
import com.jiaoay.jrime.ui.settings.RuntimeOptionSource

/**
 * Reads/sets the engine session's runtime options (the schema's `switches`).
 * When no session exists (IME not started) the hub renders the rule rows as
 * unavailable instead of failing.
 */
class RimeRuntimeOptionSource : RuntimeOptionSource {
    override suspend fun get(option: String): Boolean? = RimeDaemon.getFirstSessionOrNull()?.runOnReady { getRuntimeOption(option) }

    override suspend fun set(
        option: String,
        value: Boolean,
    ) {
        RimeDaemon.getFirstSessionOrNull()?.runOnReady { setRuntimeOption(option, value) }
    }
}
