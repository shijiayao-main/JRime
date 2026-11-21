/*
 * SPDX-FileCopyrightText: 2015 - 2025 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ime.core

import android.os.Build
import android.view.Window
import androidx.core.view.WindowCompat
import com.jiaoay.jrime.ui.design.ImePalette

/**
 * The navigation bar the IME draws behind: edge-to-edge with a transparent bar
 * over the keyboard band, and light/dark system icons matching the palette.
 * The old theme-keyed navbar preference is gone with the theme engine.
 */
class NavigationBarManager {
    private var shouldUpdateForeground = false
    private var shouldUpdateBackground = false

    private fun Window.edgeToEdge(enabled: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            WindowCompat.setDecorFitsSystemWindows(this, enabled)
        }
    }

    private fun Window.enforceNavbarContrast(enforced: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            isNavigationBarContrastEnforced = enforced
        }
    }

    fun evaluate(
        window: Window,
        useVirtualKeyboard: Boolean,
        dark: Boolean,
    ) {
        if (useVirtualKeyboard) {
            // Draw the keyboard behind the navbar: transparent bar, no scrim.
            shouldUpdateForeground = true
            shouldUpdateBackground = false
            window.edgeToEdge(false)
            window.enforceNavbarContrast(false)
        } else {
            shouldUpdateForeground = true
            shouldUpdateBackground = true
            window.edgeToEdge(true)
            window.enforceNavbarContrast(false)
        }
        update(window, dark)
    }

    private fun update(
        window: Window,
        dark: Boolean,
    ) {
        if (shouldUpdateForeground) {
            WindowCompat
                .getInsetsController(window, window.decorView)
                .isAppearanceLightNavigationBars = !dark
        }
        if (shouldUpdateBackground) {
            @Suppress("DEPRECATION")
            window.navigationBarColor = if (dark) ImePalette.Dark.surface.value.toInt() else ImePalette.Light.surface.value.toInt()
        }
    }
}
