package com.jiaoay.jrime.desktop.uispike

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.delay
import java.awt.Component
import java.awt.Frame
import java.awt.GraphicsEnvironment
import java.awt.KeyboardFocusManager
import java.awt.Window as AwtWindow

/** The focus owner's window title (or class name) for the focus assertions. */
private fun describeFocus(owner: Component): String {
    var current: Component? = owner
    while (true) {
        val node = current ?: break
        if (node is AwtWindow) return (node as? Frame)?.title?.ifEmpty { node.javaClass.simpleName } ?: node.javaClass.simpleName
        current = node.parent
    }
    return owner.javaClass.simpleName
}

/**
 * D0 spike: a borderless, always-on-top Compose window that must not take
 * focus (the candidate window contract). Prints the AWT facts the decision
 * depends on, then exits; headless environments are reported instead of
 * crashing.
 */
fun main() {
    println("spike: headless=${GraphicsEnvironment.isHeadless()}")
    if (GraphicsEnvironment.isHeadless()) {
        println("spike: SKIP — no display available")
        return
    }
    // A focusable app-like window to test the "candidate window must not steal
    // focus" contract against.
    val appFrame =
        Frame("spike-app").apply {
            setSize(240, 120)
            isVisible = true
        }
    appFrame.requestFocus()
    val candidates = listOf("你好", "尼号", "拟好", "你", "妳", "泥")
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "JRime candidate spike",
            undecorated = true,
            alwaysOnTop = true,
        ) {
            // The non-activating contract: the window must never take key
            // focus. ComposeWindow is AWT-backed, so the AWT flag is the hook.
            window.focusableWindowState = false // AWT: non-activating candidate window
            LaunchedEffect(Unit) {
                println("spike: undecorated=${window.isUndecorated}")
                println("spike: alwaysOnTop=${window.isAlwaysOnTop}")
                println("spike: focusableWindowState=${window.focusableWindowState}")
                println("spike: type=${window.type}")
                val fkm = KeyboardFocusManager.getCurrentKeyboardFocusManager()
                appFrame.requestFocus()
                delay(300)
                println("spike: focusOwnerBefore=${fkm.focusOwner?.let { describeFocus(it) }}")
                window.requestFocus()
                delay(500)
                println("spike: focusOwnerAfter=${fkm.focusOwner?.let { describeFocus(it) }}")
                delay(1000)
                println("spike: alive=${window.isDisplayable}")
                exitApplication()
            }
            Column(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF1F3F4))
                    .padding(8.dp),
            ) {
                LazyColumn {
                    items(candidates) { candidate ->
                        Text(text = candidate, color = Color(0xFF202124), modifier = Modifier.padding(4.dp))
                    }
                }
            }
        }
    }
    appFrame.dispose()
    println("spike: DONE")
}
