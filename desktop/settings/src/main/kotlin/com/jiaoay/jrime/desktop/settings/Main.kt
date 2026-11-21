package com.jiaoay.jrime.desktop.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.jiaoay.jrime.ui.settings.SettingsScreen
import com.jiaoay.jrime.ui.settings.SettingsStrings
import kotlinx.coroutines.delay
import java.awt.GraphicsEnvironment

/** The desktop settings window: shared [SettingsScreen] + properties store. */
fun main() {
    if (GraphicsEnvironment.isHeadless()) {
        println("desktop-settings: SKIP — no display")
        return
    }
    val store = DesktopSettingsStore()
    val groups = DesktopSettingSpecs.build()
    val strings =
        SettingsStrings(
            title = "Settings",
            searchPlaceholder = "Search settings",
            empty = "No matching settings",
            cancel = "Cancel",
            confirm = "OK",
        )
    application {
        Window(
            onCloseRequest = ::exitApplication,
            state = rememberWindowState(size = DpSize(520.dp, 640.dp)),
            title = "JRime Settings",
        ) {
            LaunchedEffect(Unit) {
                println("desktop-settings: window up (rows=${groups.sumOf { it.specs.size }})")
                println("desktop-settings: ${DesktopSettings.read()}")
                if (System.getenv("JRIME_UI_SMOKE") == "1") {
                    delay(1200)
                    exitApplication()
                }
            }
            MaterialTheme {
                Surface {
                    SettingsScreen(
                        groups = groups,
                        store = store,
                        strings = strings,
                        onAction = {},
                    )
                }
            }
        }
    }
}
