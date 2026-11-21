package com.jiaoay.jrime.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.jiaoay.jrime.engine.CandidateView
import com.jiaoay.jrime.engine.CompositionView
import com.jiaoay.jrime.engine.ImeCommand
import com.jiaoay.jrime.engine.ImeSessionState
import com.jiaoay.jrime.engine.KeyBinding
import com.jiaoay.jrime.engine.KeyGesture
import com.jiaoay.jrime.engine.KeyboardCatalog
import com.jiaoay.jrime.ui.ime.DesktopCandidateSurface
import com.jiaoay.jrime.ui.ime.ImeSurfaceConfig
import com.jiaoay.jrime.ui.ime.ImeSurfaceHost
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import java.awt.Dimension
import java.awt.GraphicsEnvironment

/** A minimal host: the real engine-protocol host replaces this in the next step. */
private class DemoHost(
    private val state: MutableStateFlow<ImeSessionState>,
) : ImeSurfaceHost {
    override fun dispatch(command: ImeCommand) {
        when (command) {
            is ImeCommand.SelectCandidate -> {
                val candidate = state.value.candidates.firstOrNull { it.index == command.index }
                if (candidate != null) {
                    state.value =
                        state.value.copy(
                            composition = null,
                            composing = false,
                            candidates = emptyList(),
                            candidatePageCount = 1,
                        )
                    println("desktop-ui: commit ${candidate.text}")
                }
            }

            ImeCommand.ClosePanel -> state.value = state.value.copy(activePanel = null)

            else -> println("desktop-ui: command ${command::class.simpleName}")
        }
    }

    override fun reportBandTop(topPx: Int) = Unit

    override fun onKeyFeedback(
        binding: KeyBinding,
        gesture: KeyGesture,
    ) = Unit

    override fun compositionEcho(): String = state.value.composition?.preedit.orEmpty()
}

/**
 * D2 step: the shared `ImeSurface` rendering in a borderless, always-on-top,
 * non-focusable Compose window — the desktop candidate/status window.
 */
fun main() {
    if (GraphicsEnvironment.isHeadless()) {
        println("desktop-ui: SKIP — no display")
        return
    }
    val catalog = KeyboardCatalog.shipped()
    val state =
        MutableStateFlow(
            ImeSessionState(
                composition = CompositionView(preedit = "nihao"),
                candidates =
                listOf(
                    CandidateView(0, "你好", "ni hao"),
                    CandidateView(1, "尼号", "ni hao"),
                    CandidateView(2, "拟好", "ni hao"),
                ),
                highlightedIndex = 0,
                composing = true,
                schemaLabel = "飞鹤快拼",
            ),
        )
    val host = DemoHost(state)
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "JRime candidate",
            undecorated = true,
            alwaysOnTop = true,
            resizable = false,
        ) {
            window.focusableWindowState = false
            window.minimumSize = Dimension(560, 90)
            LaunchedEffect(Unit) {
                println("desktop-ui: shared ImeSurface up (candidates=${state.value.candidates.size})")
                if (System.getenv("JRIME_UI_SMOKE") == "1") {
                    delay(1200)
                    exitApplication()
                }
            }
            Box(modifier = Modifier.size(560.dp, 90.dp).background(Color(0xFF202124))) {
                DesktopCandidateSurface(
                    state = state.value,
                    config = ImeSurfaceConfig(candidateTextSizeSp = 18f),
                    host = host,
                    catalog = catalog,
                )
            }
        }
    }
}
