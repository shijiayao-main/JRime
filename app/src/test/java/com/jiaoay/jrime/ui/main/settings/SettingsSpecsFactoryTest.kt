/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ui.main.settings

import com.jiaoay.jrime.data.prefs.AppPrefs
import com.jiaoay.jrime.data.prefs.PreferenceDelegateOwner
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

/**
 * The settings hub's single-source contract: the hub is generated from the
 * preference declarations, so every declared setting must produce a row and
 * every row must point at a real preference (the no-orphan rule, both ways).
 */
class SettingsSpecsFactoryTest :
    StringSpec({
        val fake = FakeSharedPreferences()
        val providers: List<PreferenceDelegateOwner> =
            listOf(
                AppPrefs.General(fake),
                AppPrefs.Keyboard(fake),
                AppPrefs.Appearance(fake),
                AppPrefs.Clipboard(fake),
                AppPrefs.Advanced(fake),
            )
        val factory =
            SettingsSpecsFactory(
                labeler = { "label-$it" },
                dynamicChoices = { emptyList() },
                providers = providers,
            )
        val groups = factory.build()
        val specs = groups.flatMap { it.specs }
        val specKeys = specs.map { it.key }.toSet() - SettingsSpecsFactory.ACTION_IMPORT_FONT
        val declaredUiKeys = providers.flatMap { provider -> provider.preferenceDelegatesUi.map { it.key } }.toSet()
        val declaredKeys = providers.flatMap { provider -> provider.preferenceDelegates.keys }.toSet()

        "every declared setting becomes a hub row" {
            (declaredUiKeys - specKeys).shouldBeEmpty()
        }

        "every hub row points at a declared preference" {
            (specKeys - declaredKeys).shouldBeEmpty()
        }

        "row keys are unique" {
            specs.map { it.key }.size shouldBe specs.map { it.key }.toSet().size
        }

        "the hub uses the six product groups" {
            groups.map { it.id }.shouldContainAll(
                "input",
                "keyboard",
                "candidates",
                "appearance",
                "clipboard",
                "system",
            )
        }

        "the appearance rows land in the appearance group" {
            val appearanceKeys = specs.filter { it.groupId == "appearance" }.map { it.key }.toSet()
            appearanceKeys.shouldContainAll(
                AppPrefs.Appearance.PALETTE_MODE,
                AppPrefs.Appearance.ACCENT,
                AppPrefs.Appearance.SHOW_KEY_HINTS,
            )
        }

        "the import-font action lives in the system group" {
            specs.first { it.key == SettingsSpecsFactory.ACTION_IMPORT_FONT }.groupId shouldBe "system"
        }
    })

/** The store seam writes and reports revisions (the hub re-reads on bumps). */
class AppSettingsStoreTest :
    StringSpec({
        "values round-trip and the revision bumps per change" {
            val fake = FakeSharedPreferences()
            val store = AppSettingsStore(fake)
            val before = store.revision.value
            store.setBool("flag", true)
            store.setInt("count", 7)
            store.setString("name", "x")
            store.bool("flag", false) shouldBe true
            store.int("count", 0) shouldBe 7
            store.string("name", "") shouldBe "x"
            store.revision.value shouldBeGreaterThan before
            store.dispose()
        }
    })
