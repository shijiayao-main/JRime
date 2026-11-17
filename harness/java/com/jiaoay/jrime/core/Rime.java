package com.jiaoay.jrime.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal desktop-JVM stub matching the signatures the JNI bridge resolves in
 * GlobalRefSingleton (jni-utils.h). Not the real Kotlin class — its only job
 * is to let JNI_OnLoad resolve every FindClass/GetMethodID and let the
 * notification callback round-trip back into Java.
 */
public class Rime {
    public static final List<String> messages = new ArrayList<>();

    public static void handleRimeMessage(int type, Object[] args) {
        String value = args.length > 0 ? String.valueOf(args[0]) : null;
        messages.add("type=" + type + " value=" + value);
        System.out.println("[C++ -> Java callback] handleRimeMessage type=" + type + " value=" + value);
    }

    public static native void startupRime(String sharedDir, String userDir, String versionName);

    public static native void startRimeMaintenance(boolean fullCheck);

    public static native void exitRime();

    public static native SchemaItem[] getRimeSchemaList();
}
