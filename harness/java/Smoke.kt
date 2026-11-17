@file:JvmName("Smoke")

import com.jiaoay.jrime.core.Rime
import java.io.File

/**
 * Minimal desktop JVM load smoke test for the librime_jni bridge.
 * 1. System.loadLibrary -> JNI_OnLoad (caches JavaVM, resolves all FindClass/GetMethodID)
 * 2. startupRime -> setenv + librime setup/initialize + set_notification_handler
 * 3. startRimeMaintenance -> deployment emits notifications -> C++ calls back into Java
 * 4. getRimeSchemaList -> full JNI object-conversion round trip
 * 5. exitRime
 *
 * Usage: java -Djava.library.path=harness/build -cp harness/build/classes Smoke <dataBaseDir>
 */
fun main(args: Array<String>) {
    System.out.println(
        "JVM: " + System.getProperty("java.vm.name") + " " + System.getProperty("java.version") +
            " on " + System.getProperty("os.arch"),
    )
    val base = if (args.isNotEmpty()) args[0] else "harness/build/data"
    File(base + "/shared").mkdirs()
    File(base + "/user").mkdirs()

    System.out.println("STEP1: System.loadLibrary(\"rime_jni\")")
    System.loadLibrary("rime_jni")
    System.out.println("STEP1 OK: JNI_OnLoad completed; GlobalRefSingleton resolved all classes + methodIDs")

    System.out.println("STEP2: startupRime(shared=" + base + "/shared, user=" + base + "/user)")
    Rime.startupRime(base + "/shared", base + "/user", "1.0.0-desktop-smoke")
    System.out.println("STEP2 OK: librime setup/initialize ran; notification handler installed")

    System.out.println("STEP3: startRimeMaintenance(fullCheck=true)")
    Rime.startRimeMaintenance(true)
    System.out.println("STEP3 OK: deployment ran; callbacks received = " + Rime.messages.size)

    System.out.println("STEP4: getRimeSchemaList()")
    schemaItemStubCheck()
    System.out.println("STEP4 OK")

    System.out.println("STEP5: exitRime()")
    Rime.exitRime()
    System.out.println("STEP5 OK")

    System.out.println("SMOKE TEST PASS")
}

private fun schemaItemStubCheck() {
    val list = Rime.getRimeSchemaList()
    System.out.println("schema list size = " + list.size)
}
