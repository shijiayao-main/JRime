/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.data.theme.model

sealed interface MaybeStringList {
    fun toList(): List<String>

    data object Empty : MaybeStringList {
        override fun toList(): List<String> = emptyList()
    }

    @JvmInline
    value class Scalar(val value: String) : MaybeStringList {
        override fun toList(): List<String> = if (value.isEmpty()) emptyList() else listOf(value)
    }

    @JvmInline
    value class Sequence(val value: List<String>) : MaybeStringList {
        override fun toList(): List<String> = value
    }
}
