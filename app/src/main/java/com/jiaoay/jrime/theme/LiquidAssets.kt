/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.theme

import com.jiaoay.jrime.data.theme.model.LiquidKeyboard
import com.jiaoay.jrime.util.appContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Loads the liquid keyboard content (symbol and emoji pages) from the
 * standalone `shared/liquid.json` asset. The content is data, not theme
 * configuration: it outlives the theme YAML engine (task 1.4) as a plain
 * JSON asset consumed directly -- no YAML or theme machinery on the path.
 */
object LiquidAssets {
    private const val ASSET_PATH = "shared/liquid.json"

    /** The app's one liquid-keyboard content, loaded once. */
    val liquidKeyboard: LiquidKeyboard by lazy { load() }

    private val json = Json { ignoreUnknownKeys = true }

    fun load(): LiquidKeyboard = parse(runCatching(::read).getOrElse { LiquidAsset() })

    /** Parses the asset's decoded text; separate so unit tests can feed it directly. */
    fun parse(text: String): LiquidKeyboard = parse(runCatching { json.decodeFromString(LiquidAsset.serializer(), text) }.getOrElse { LiquidAsset() })

    private fun parse(asset: LiquidAsset): LiquidKeyboard = LiquidKeyboard(
        singleWidth = asset.singleWidth,
        keyHeight = asset.keyHeight,
        marginX = asset.marginX,
        fixedKeyBar =
        LiquidKeyboard.KeyBar(
            keys = asset.fixedKeyBar.keys,
            position = keyBarPosition(asset.fixedKeyBar.position),
        ),
        keyboards = asset.keyboards.map(KeyboardAsset::toModel),
    )

    private fun read(): LiquidAsset = appContext.assets.open(ASSET_PATH).bufferedReader().use { reader ->
        json.decodeFromString(LiquidAsset.serializer(), reader.readText())
    }

    private fun keyBarPosition(value: String): LiquidKeyboard.KeyBar.Position = when (value.lowercase()) {
        "top" -> LiquidKeyboard.KeyBar.Position.TOP
        "left" -> LiquidKeyboard.KeyBar.Position.LEFT
        "right" -> LiquidKeyboard.KeyBar.Position.RIGHT
        else -> LiquidKeyboard.KeyBar.Position.BOTTOM
    }
}

@Serializable
private data class LiquidAsset(
    val singleWidth: Int = 0,
    val keyHeight: Int = 0,
    val marginX: Float = 0f,
    val fixedKeyBar: KeyBarAsset = KeyBarAsset(),
    val keyboards: List<KeyboardAsset> = emptyList(),
)

@Serializable
private data class KeyBarAsset(
    val keys: List<String> = emptyList(),
    val position: String = "bottom",
)

@Serializable
private data class KeyboardAsset(
    val id: String,
    val type: String = "SINGLE",
    val name: String? = null,
    val keys: List<String> = emptyList(),
) {
    fun toModel(): LiquidKeyboard.Keyboard = LiquidKeyboard.Keyboard(
        id = id,
        type = when (type.uppercase()) {
            "SYMBOL" -> LiquidKeyboard.DataType.SYMBOL
            "TABS" -> LiquidKeyboard.DataType.TABS
            "HISTORY" -> LiquidKeyboard.DataType.HISTORY
            else -> LiquidKeyboard.DataType.SINGLE
        },
        name = name ?: id,
        keys = keys.map(::keyItem),
    )

    private fun keyItem(text: String): LiquidKeyboard.KeyItem = LiquidKeyboard.KeyItem(text)
}
