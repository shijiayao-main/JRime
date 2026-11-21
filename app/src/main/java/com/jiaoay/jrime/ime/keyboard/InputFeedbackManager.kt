/*
 * SPDX-FileCopyrightText: 2015 - 2025 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ime.keyboard

import android.content.Context
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.speech.tts.TextToSpeech
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.View
import androidx.annotation.MainThread
import com.jiaoay.jrime.data.prefs.AppPrefs
import splitties.systemservices.audioManager
import splitties.systemservices.vibrator
import timber.log.Timber

/**
 * Manage the key press effects, such as vibration, sound, speaking and so on.
 */
object InputFeedbackManager {
    private val keyboardPrefs = AppPrefs.defaultInstance().keyboard

    private var applicationContext: Context? = null
    private val callbackHandler by lazy { Handler(Looper.getMainLooper()) }
    private val speechRequests = FeedbackRequestDispatcher(
        isOwnerThread = { Looper.myLooper() == Looper.getMainLooper() },
        postToOwner = { request -> callbackHandler.post { request() } },
    )
    private val speech = FeedbackResource<TextToSpeech> { it.shutdown() }
    private data class PendingSpeech(val text: String, val onCommit: Boolean)
    private var pendingSpeech: PendingSpeech? = null

    @MainThread
    fun init(context: Context) {
        resetSpeechRequests()
        applicationContext = context.applicationContext
        speech.init()
    }

    @MainThread
    private fun resetSpeechRequests() {
        speechRequests.invalidate()
        pendingSpeech = null
        speech.ready?.stop()
    }

    @MainThread
    private fun prepareSpeech(onCommit: Boolean) {
        val context = applicationContext ?: return
        try {
            val resource = speech.acquire(
                enabled = if (onCommit) speakOnCommit else speakOnKeyPress,
                create = { complete ->
                    TextToSpeech(context) { status ->
                        callbackHandler.post { complete(status == TextToSpeech.SUCCESS) }
                    }
                },
                onInitialized = { engine, ready ->
                    if (ready) speakPending(engine) else pendingSpeech = null
                },
            )
            if (resource == null) {
                pendingSpeech = null
            } else if (speech.ready === resource) {
                speakPending(resource)
            }
        } catch (e: Exception) {
            pendingSpeech = null
            Timber.e(e, "Error on initializing speech feedback")
        }
    }

    @MainThread
    private fun speakPending(engine: TextToSpeech) {
        val pending = pendingSpeech ?: return
        pendingSpeech = null
        if (if (pending.onCommit) speakOnCommit else speakOnKeyPress) {
            engine.speak(pending.text, TextToSpeech.QUEUE_FLUSH, null, "TrimeTTS")
        }
    }

    private val hasAmplitudeControl = vibrator.hasAmplitudeControl()

    private val vibrateOnKeyPress by keyboardPrefs.vibrateOnKeyPress
    private val vibrationDuration by keyboardPrefs.vibrationDuration
    private val vibrationAmplitude by keyboardPrefs.vibrationAmplitude

    /**
     * Makes a key press vibration if the user has this feature enabled in the preferences.
     */
    fun keyPressVibrate(
        view: View,
        longPress: Boolean = false,
    ) {
        if (!vibrateOnKeyPress) return
        val duration: Long = vibrationDuration.toLong()
        val hfc =
            if (longPress) {
                HapticFeedbackConstants.LONG_PRESS
            } else {
                HapticFeedbackConstants.KEYBOARD_TAP
            }

        if (duration != 0L) { // use vibrator
            if (hasAmplitudeControl && vibrationAmplitude != 0) {
                vibrator.vibrate(VibrationEffect.createOneShot(duration, vibrationAmplitude))
            } else {
                val ve = VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE)
                vibrator.vibrate(ve)
            }
        } else {
            @Suppress("DEPRECATION")
            val flags =
                HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING or HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            view.performHapticFeedback(hfc, flags)
        }
    }

    private val soundOnKeyPress by keyboardPrefs.soundOnKeyPress
    private val soundVolume by keyboardPrefs.soundVolume

    /**
     * Makes a key press sound if the user has this feature enabled in the preferences.
     */
    @MainThread
    fun keyPressSound(keyCode: Int = 0) {
        if (!soundOnKeyPress) return
        val effect =
            when (keyCode) {
                KeyEvent.KEYCODE_SPACE -> AudioManager.FX_KEYPRESS_SPACEBAR
                KeyEvent.KEYCODE_DEL -> AudioManager.FX_KEYPRESS_DELETE
                KeyEvent.KEYCODE_ENTER -> AudioManager.FX_KEYPRESS_RETURN
                else -> AudioManager.FX_KEYPRESS_STANDARD
            }
        val volume =
            if (soundVolume == 0) {
                -1f
            } else {
                soundVolume / 100f
            }
        audioManager.playSoundEffect(effect, volume)
    }

    private val speakOnKeyPress by keyboardPrefs.speakOnKeyPress
    private val speakOnCommit by keyboardPrefs.speakOnCommit

    fun keyPressSpeak(keyCode: Int) {
        speechRequests.dispatch {
            if (speakOnKeyPress) contentSpeakInternal(keyCode, onCommit = false)
        }
    }

    fun textCommitSpeak(text: String) {
        speechRequests.dispatch {
            if (speakOnCommit) contentSpeakInternal(text, onCommit = true)
        }
    }

    @MainThread
    private inline fun <reified T> contentSpeakInternal(content: T, onCommit: Boolean) {
        if (applicationContext == null) return
        val text =
            when {
                0 is T -> {
                    KeyEvent
                        .keyCodeToString(content as Int)
                        .replace("KEYCODE_", "")
                        .replace("_", " ")
                        .lowercase()
                }

                "" is T -> content as String

                else -> return
            }

        pendingSpeech = PendingSpeech(text, onCommit)
        prepareSpeech(onCommit)
    }

    @MainThread
    fun finishInput() {
        resetSpeechRequests()
    }

    @MainThread
    fun destroy() {
        resetSpeechRequests()
        applicationContext = null
        speech.destroy()
    }
}
