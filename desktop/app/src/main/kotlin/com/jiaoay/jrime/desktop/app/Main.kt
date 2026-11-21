package com.jiaoay.jrime.desktop.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.jiaoay.jrime.core.CompositionProto
import com.jiaoay.jrime.core.Rime
import com.jiaoay.jrime.core.RimeDataAccess
import com.jiaoay.jrime.desktop.DesktopEngineSession
import com.jiaoay.jrime.desktop.settings.DesktopSettings
import com.jiaoay.jrime.engine.AccentPreset
import com.jiaoay.jrime.engine.CandidateView
import com.jiaoay.jrime.engine.CompositionView
import com.jiaoay.jrime.engine.ImeCommand
import com.jiaoay.jrime.engine.ImeSessionState
import com.jiaoay.jrime.engine.KeyBinding
import com.jiaoay.jrime.engine.KeyGesture
import com.jiaoay.jrime.engine.KeyboardCatalog
import com.jiaoay.jrime.engine.PaletteMode
import com.jiaoay.jrime.ui.ime.DesktopCandidateSurface
import com.jiaoay.jrime.ui.ime.ImeSurfaceConfig
import com.jiaoay.jrime.ui.ime.ImeSurfaceHost
import kotlinx.coroutines.flow.MutableStateFlow
import java.awt.GraphicsEnvironment
import java.awt.Rectangle
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.PrintStream
import kotlin.concurrent.thread
import kotlin.system.exitProcess

private const val VERSION = "1.0.0-desktop"
private const val PROTOCOL_VERSION = 1
private const val WINDOW_WIDTH_PX = 560
private const val WINDOW_HEIGHT_PX = 90

/** Everything the protocol thread and the UI share. */
private class Shared(
    val session: DesktopEngineSession,
    val state: MutableStateFlow<ImeSessionState>,
    val placement: MutableStateFlow<Rectangle>,
    val out: PrintStream,
    val paletteMode: PaletteMode,
    val accent: AccentPreset,
    val candidateTextSizeSp: Float,
    val showKeyHints: Boolean,
    val showCandidateComment: Boolean,
)

/**
 * The desktop IME host process: engine + line protocol on stdin/stdout + the
 * Compose candidate window.
 *
 * Shell requests are answered exactly like [com.jiaoay.jrime.desktop.DesktopServer];
 * a `rect x y w h` request (new) places the candidate window near the caret and
 * is acknowledged with `{"ok":true}`. Changes initiated inside the app (candidate
 * clicks) are pushed as an asynchronous `{"event":"update",...}` line so the
 * shell can commit the text / refresh marked text.
 */
fun main(args: Array<String>) {
    var shared: String? = null
    var user: String? = null
    var dylib: String? = null
    var schema = "flypy_xhfast"
    var i = 0
    while (i < args.size) {
        when (args[i]) {
            "--shared" -> shared = args[++i]
            "--user" -> user = args[++i]
            "--dylib" -> dylib = args[++i]
            "--schema" -> schema = args[++i]
            else -> throw IllegalArgumentException("unknown arg: " + args[i])
        }
        i++
    }
    if (shared == null || user == null || dylib == null) {
        throw IllegalArgumentException("--shared/--user/--dylib are required")
    }
    System.setOut(PrintStream(System.err)) // stdout carries protocol lines only
    System.load(File(dylib).absolutePath)
    Rime.startupRime(shared, user, VERSION)

    val session = DesktopEngineSession(VERSION, PROTOCOL_VERSION, schema)
    if (!session.awaitReady()) {
        System.err.println("engine never mounted schema $schema; messages=${session.lastMessages()}")
        exitProcess(2)
    }
    // The desktop settings window writes this file; the candidate window reads
    // it once at start (live reload lands with the settings window integration).
    val appearance = DesktopSettings.read()
    println(
        "desktop-app: settings palette=${appearance[DesktopSettings.PALETTE_MODE]} " +
            "accent=${appearance[DesktopSettings.ACCENT]}",
    )
    val paletteMode = appearance.toPaletteMode()
    val accent = appearance.toAccent()
    val state = MutableStateFlow(readState().copy(paletteMode = paletteMode, accent = accent))
    val placement = MutableStateFlow(Rectangle(0, 0, WINDOW_WIDTH_PX, WINDOW_HEIGHT_PX))
    val out = PrintStream(System.out, true)
    val sharedState =
        Shared(
            session = session,
            state = state,
            placement = placement,
            out = out,
            paletteMode = paletteMode,
            accent = accent,
            candidateTextSizeSp = appearance[DesktopSettings.CANDIDATE_TEXT_SIZE]?.toFloatOrNull() ?: 18f,
            showKeyHints = appearance[DesktopSettings.SHOW_KEY_HINTS]?.toBooleanStrictOrNull() ?: true,
            showCandidateComment = appearance[DesktopSettings.SHOW_CANDIDATE_COMMENT]?.toBooleanStrictOrNull() ?: true,
        )

    thread(isDaemon = true, name = "desktop-protocol") { protocolLoop(sharedState) }

    if (GraphicsEnvironment.isHeadless()) {
        System.err.println("desktop-app: headless; protocol only (send exit to stop)")
        while (true) {
            Thread.sleep(60_000)
        }
    }
    runWindow(sharedState)
}

private fun runWindow(sharedState: Shared) {
    val catalog = KeyboardCatalog.shipped()
    application {
        val position by sharedState.placement.collectAsState()
        val state by sharedState.state.collectAsState()
        val windowState =
            rememberWindowState(
                size = DpSize(560.dp, 90.dp),
                position = WindowPosition(position.x.dp, position.y.dp),
            )
        Window(
            onCloseRequest = {
                Rime.exitRime()
                exitProcess(0)
            },
            state = windowState,
            title = "JRime",
            undecorated = true,
            alwaysOnTop = true,
            resizable = false,
        ) {
            window.focusableWindowState = false
            window.minimumSize = java.awt.Dimension(WINDOW_WIDTH_PX, WINDOW_HEIGHT_PX)
            LaunchedEffect(Unit) { println("desktop-app: candidate window up") }
            LaunchedEffect(position) {
                windowState.position = WindowPosition(position.x.dp, position.y.dp)
            }
            val host = remember { ModelHost(sharedState) }
            Box(modifier = Modifier.size(560.dp, 90.dp).background(Color(0xFF202124))) {
                DesktopCandidateSurface(
                    state = state,
                    config =
                    ImeSurfaceConfig(
                        candidateTextSizeSp = sharedState.candidateTextSizeSp,
                        showKeyHints = sharedState.showKeyHints,
                        showCandidateComment = sharedState.showCandidateComment,
                    ),
                    host = host,
                    catalog = catalog,
                    modifier = Modifier.size(560.dp, 90.dp),
                )
            }
        }
    }
}

private fun protocolLoop(sharedState: Shared) {
    val reader = BufferedReader(InputStreamReader(System.`in`))
    while (true) {
        val line = reader.readLine() ?: break
        val trimmed = line.trim()
        if (trimmed.isEmpty()) continue
        if (trimmed.startsWith("rect ")) {
            val parts = trimmed.substring(5).trim().split(" +".toRegex())
            if (parts.size >= 4) {
                val rect =
                    Rectangle(
                        parts[0].toIntOrNull() ?: 0,
                        parts[1].toIntOrNull() ?: 0,
                        WINDOW_WIDTH_PX,
                        WINDOW_HEIGHT_PX,
                    )
                sharedState.placement.value = rect
            }
            sharedState.out.println("{\"ok\":true}")
            continue
        }
        synchronized(sharedState.session) {
            val reply = sharedState.session.handle(trimmed)
            if (reply == null) {
                Rime.exitRime()
                exitProcess(0)
            }
            sharedState.out.println(reply.json)
            sharedState.state.value =
                readState().copy(
                    paletteMode = sharedState.paletteMode,
                    accent = sharedState.accent,
                )
            if (trimmed == "exit") {
                Rime.exitRime()
                exitProcess(0)
            }
        }
    }
    Rime.exitRime()
    exitProcess(0)
}

/** The Compose-only host: candidate clicks re-enter the engine in this process. */
private class ModelHost(
    private val sharedState: Shared,
) : ImeSurfaceHost {
    override fun dispatch(command: ImeCommand) {
        if (command !is ImeCommand.SelectCandidate) return
        synchronized(sharedState.session) {
            val reply = sharedState.session.handle("sel ${command.index}") ?: return
            val next =
                readState().copy(
                    paletteMode = sharedState.paletteMode,
                    accent = sharedState.accent,
                )
            sharedState.state.value = next
            // The shell did not ask for this selection: push the outcome.
            val event =
                "{\"event\":\"update\",\"preedit\":\"" +
                    com.jiaoay.jrime.protocol.JsonLineCodec.encode(next.composition?.preedit.orEmpty()) +
                    "\",\"composing\":" + next.composing +
                    ",\"commit\":" +
                    (reply.commitText?.let { "\"" + com.jiaoay.jrime.protocol.JsonLineCodec.encode(it) + "\"" } ?: "null") +
                    "}"
            sharedState.out.println(event)
        }
    }

    override fun reportBandTop(topPx: Int) = Unit

    override fun onKeyFeedback(
        binding: KeyBinding,
        gesture: KeyGesture,
    ) = Unit

    override fun compositionEcho(): String = sharedState.state.value.composition?.preedit.orEmpty()
}

/** Snapshots the engine into the shared surface state. */
private fun readState(): ImeSessionState {
    val status = RimeDataAccess.status()
    val ctx = RimeDataAccess.context()
    val composition = ctx?.composition?.let { mapComposition(it) }
    val candidates =
        RimeDataAccess.candidates(0, 48).mapIndexed { index, candidate ->
            CandidateView(
                index = index,
                text = candidate.text.orEmpty(),
                comment = candidate.comment.orEmpty(),
                label = candidate.seqId.orEmpty(),
            )
        }
    return ImeSessionState(
        composition = composition,
        candidates = candidates,
        highlightedIndex = -1,
        composing = status.isComposing,
        asciiMode = status.isAsciiMode,
        asciiPunctuation = status.isAsciiPunch,
        fullShape = status.isFullShape,
        simplified = status.isSimplified,
        schemaLabel = status.schemaName.orEmpty(),
    )
}

private fun mapComposition(proto: CompositionProto): CompositionView = CompositionView(
    preedit = proto.preedit.orEmpty(),
    cursor = proto.preeditCursor,
    selStart = proto.selStart,
    selEnd = proto.selEnd,
    commitPreview = proto.selStartText.orEmpty(),
)

private fun Map<String, String>.toPaletteMode(): PaletteMode = when (this[DesktopSettings.PALETTE_MODE]) {
    "light" -> PaletteMode.ForceLight
    "dark" -> PaletteMode.ForceDark
    else -> PaletteMode.FollowSystem
}

private fun Map<String, String>.toAccent(): AccentPreset = when (this[DesktopSettings.ACCENT]) {
    "green" -> AccentPreset.Green
    "purple" -> AccentPreset.Purple
    else -> AccentPreset.Blue
}
