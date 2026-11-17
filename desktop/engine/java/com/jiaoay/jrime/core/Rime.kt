package com.jiaoay.jrime.core

import java.util.ArrayList

/**
 * Minimal desktop-JVM stub matching the signatures the JNI bridge resolves in
 * GlobalRefSingleton (jni-utils.h). Not the real Kotlin class — its only job
 * is to let JNI_OnLoad resolve every FindClass/GetMethodID, let the
 * notification callback round-trip back into Java, and expose the natives the
 * harness drivers call. Fields exist so drivers can read the returned protos.
 */
object Rime {
    @JvmField
    val messages: MutableList<String> = ArrayList()

    @JvmStatic
    fun handleRimeMessage(type: Int, args: Array<Any?>) {
        val value = if (args.isNotEmpty()) java.lang.String.valueOf(args[0]) else null
        messages.add("type=" + type + " value=" + value)
        System.out.println("[C++ -> Java callback] handleRimeMessage type=" + type + " value=" + value)
    }

    @JvmStatic
    external fun startupRime(sharedDir: String, userDir: String, versionName: String)

    @JvmStatic
    external fun startRimeMaintenance(fullCheck: Boolean)

    @JvmStatic
    external fun exitRime()

    @JvmStatic
    external fun getRimeSchemaList(): Array<SchemaItem>

    @JvmStatic
    external fun processRimeKey(keycode: Int, mask: Int): Boolean

    @JvmStatic
    external fun commitRimeComposition(): Boolean

    @JvmStatic
    external fun clearRimeComposition()

    @JvmStatic
    external fun getRimeCommit(): CommitProto?

    @JvmStatic
    external fun getRimeContext(): ContextProto

    @JvmStatic
    external fun getRimeStatus(): StatusProto

    @JvmStatic
    external fun getRimeRawInput(): String

    @JvmStatic
    external fun getRimeCandidates(startIndex: Int, limit: Int): Array<CandidateProto>

    @JvmStatic
    external fun selectRimeCandidate(index: Int, global: Boolean): Boolean

    @JvmStatic
    external fun changeRimeCandidatePage(backward: Boolean): Boolean

    @JvmStatic
    external fun setRimeOption(option: String, value: Boolean)

    @JvmStatic
    external fun getRimeOption(option: String): Boolean
}
