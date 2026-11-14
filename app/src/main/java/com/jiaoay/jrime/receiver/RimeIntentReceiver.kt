/*
 * SPDX-FileCopyrightText: 2015 - 2025 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jiaoay.jrime.BuildConfig
import com.jiaoay.jrime.daemon.RimeDaemon
import com.jiaoay.jrime.daemon.launchOnReady
import timber.log.Timber

class RimeIntentReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val rime = RimeDaemon.getFirstSessionOrNull() ?: return Timber.w("No active rime session, skipping")
        Timber.i("Received broadcast ${intent.action}")
        when (intent.action) {
            ACTION_DEPLOY -> {
                Timber.i("try to start maintenance ...")
                rime.launchOnReady { it.deploy() }
            }

            else -> {}
        }
    }

    companion object {
        const val ACTION_DEPLOY = "${BuildConfig.APPLICATION_ID}.action.DEPLOY"
    }
}
