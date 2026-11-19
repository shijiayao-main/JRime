// SPDX-FileCopyrightText: 2015 - 2026 Rime community
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.jiaoay.jrime.daemon

import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.jiaoay.jrime.R
import com.jiaoay.jrime.TrimeApplication
import com.jiaoay.jrime.core.Rime
import com.jiaoay.jrime.core.RimeApi
import com.jiaoay.jrime.core.RimeLifecycle
import com.jiaoay.jrime.core.RimeMessage
import com.jiaoay.jrime.core.RimeOpenCCBridge
import com.jiaoay.jrime.core.data.UserDataManager
import com.jiaoay.jrime.core.lifecycleScope
import com.jiaoay.jrime.core.whenReady
import com.jiaoay.jrime.data.opencc.OpenCCDictManager
import com.jiaoay.jrime.data.prefs.AppPrefs
import com.jiaoay.jrime.ime.core.InlinePreeditFormatter
import com.jiaoay.jrime.ui.main.LogActivity
import com.jiaoay.jrime.util.DeployNotification
import com.jiaoay.jrime.util.appContext
import com.jiaoay.jrime.util.readText
import com.jiaoay.jrime.util.subprocess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import splitties.systemservices.notificationManager
import timber.log.Timber
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Manage the singleton instance of [Rime]
 *
 * To use rime, client should call [createSession] to obtain a [RimeSession],
 * and call [destroySession] on client destroyed. Client should not leak the instance of [RimeApi],
 * and must use [RimeSession] to access rime functionalities.
 *
 * The instance of [Rime] always exists,but whether the dispatcher runs and callback works depend on clients, i.e.
 * if no clients are connected, [Rime.finalize] will be called.
 *
 * Functions are thread-safe in this class.
 *
 * Adapted from [fcitx5-android/FcitxDaemon.kt](https://github.com/fcitx5-android/fcitx5-android/blob/364afb44dcf0d9e3db3d43a21a32601b2190cbdf/app/src/main/java/org/fcitx/fcitx5/android/daemon/FcitxDaemon.kt)
 */
object RimeDaemon {
    private val realRime by lazy {
        Rime(
            inlinePreeditFormatter = InlinePreeditFormatter(),
            showAsciiSwitchTips = { AppPrefs.defaultInstance().general.asciiSwitchTips.getValue() },
            openCCBridge = RimeOpenCCBridge { OpenCCDictManager.buildOpenCCDict() },
        )
    }

    private val rimeImpl by lazy { object : RimeApi by realRime {} }

    private val sessions = mutableMapOf<String, RimeSession>()

    private val lock = ReentrantLock()

    private fun establish(name: String) = object : RimeSession {
        private inline fun <T> ensureEstablished(block: () -> T) = if (name in sessions) {
            block()
        } else {
            throw IllegalStateException("Session $name is not established")
        }

        override fun <T> run(block: suspend RimeApi.() -> T): T = ensureEstablished {
            runBlocking { block(rimeImpl) }
        }

        override suspend fun <T> runOnReady(block: suspend RimeApi.() -> T): T = ensureEstablished {
            realRime.lifecycle.whenReady { block(rimeImpl) }
        }

        override fun runIfReady(block: suspend RimeApi.() -> Unit) {
            ensureEstablished {
                if (realRime.isReady) {
                    realRime.lifecycleScope.launch {
                        block(rimeImpl)
                    }
                }
            }
        }

        override val lifecycleScope: CoroutineScope
            get() = realRime.lifecycle.lifecycleScope
    }

    private fun tryStartRimeLocked(): Boolean {
        if (realRime.lifecycle.currentState != RimeLifecycle.State.STOPPED) {
            return true
        }
        return realRime.startup()
    }

    /**
     * Start Rime if a previous attempt was refused because its preconditions were not
     * met. [Rime.startup] only fails on a precondition (storage unavailable, or not
     * stopped), so callers should invoke this whenever such a precondition may have
     * changed: after the user grants access to a data directory, and when a new input
     * session starts. Startup is refused right after a reboot, when the SAF grant and
     * emulated external storage are not reachable before the first unlock; this covers
     * that case without keeping a retry timer alive.
     *
     * @return true when rime is starting or already usable, false when startup was
     * refused again (then nothing is running and a later call may still succeed).
     */
    fun ensureRimeStarted(): Boolean = lock.withLock {
        if (realRime.lifecycle.currentState != RimeLifecycle.State.STOPPED) {
            return@withLock true
        }
        if (sessions.isEmpty()) {
            return@withLock false
        }
        tryStartRimeLocked().also {
            if (it) Timber.i("Rime started on demand")
        }
    }

    fun createSession(name: String): RimeSession = lock.withLock {
        if (name in sessions) {
            return@withLock sessions.getValue(name)
        }
        if (realRime.lifecycle.currentState == RimeLifecycle.State.STOPPED) {
            Timber.d("RimeDaemon start rime")
            if (!realRime.startup()) {
                Timber.w("Rime startup refused; retrying when an input session starts")
            }
        }
        val session = establish(name)
        sessions[name] = session
        return@withLock session
    }

    fun destroySession(name: String): Unit = lock.withLock {
        if (name !in sessions) {
            return
        }
        sessions -= name
        if (sessions.isEmpty()) {
            Timber.d("RimeDaemon stop rime")
            realRime.finalize()
        }
    }

    /**
     * Reuse a session for remote service
     */
    fun getFirstSessionOrNull() = sessions.firstNotNullOfOrNull { it.value }

    private var restartId = 0

    init {
        DeployNotification.ensureChannel()
        TrimeApplication.getInstance().coroutineScope.launch {
            realRime.messageFlow.collect {
                handleRimeMessage(it)
            }
        }
    }

    private inline fun sendNotification(
        id: Int,
        buildAction: NotificationCompat.Builder.() -> Unit,
    ) {
        val builder =
            NotificationCompat
                .Builder(appContext, DeployNotification.CHANNEL_ID)
                .setContentTitle(appContext.getString(R.string.rime_daemon))
        builder.buildAction()
        builder.build().let { notificationManager.notify(id, it) }
    }

    /**
     * Restart Rime instance to deploy while keep the session
     */
    fun restartRime(fullCheck: Boolean = false) = lock.withLock {
        val id = restartId++
        if (!fullCheck) {
            sendNotification(id) {
                setSmallIcon(R.drawable.ic_baseline_sync_24)
                setContentTitle(appContext.getString(R.string.rime_daemon))
                setContentText(appContext.getString(R.string.restarting_rime))
                setOngoing(true)
                setProgress(100, 0, true)
                setPriority(NotificationCompat.PRIORITY_HIGH)
            }
        }
        realRime.finalize()
        realRime.startup()
        TrimeApplication.getInstance().coroutineScope.launch {
            realRime.lifecycle.whenReady {
                notificationManager.cancel(id)
            }
        }
    }

    fun stopRime() {
        realRime.finalize()
    }

    private suspend fun handleRimeMessage(it: RimeMessage<*>) {
        if (it is RimeMessage.DeployMessage) {
            when (it.data) {
                RimeMessage.DeployMessage.State.Start -> {
                    DeployNotification.showProgress()
                    withContext(Dispatchers.IO) { subprocess("logcat", "--clear") }
                }

                RimeMessage.DeployMessage.State.Success -> {
                    DeployNotification.showSuccess()
                }

                RimeMessage.DeployMessage.State.Failure -> {
                    val intent =
                        Intent(appContext, LogActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            val log =
                                subprocess("logcat", "-v", "brief", "-s", "rime.trime:W", "-d")
                                    .readText()
                            putExtra(LogActivity.FROM_DEPLOY, true)
                            putExtra(LogActivity.DEPLOY_FAILURE_TRACE, log)
                        }
                    val pendingIntent =
                        PendingIntent.getActivity(
                            appContext,
                            0,
                            intent,
                            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE,
                        )
                    DeployNotification.showFailure(pendingIntent)
                }
            }
        }
    }
}
