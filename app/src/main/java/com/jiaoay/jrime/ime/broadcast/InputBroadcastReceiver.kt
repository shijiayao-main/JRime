/*
 * SPDX-FileCopyrightText: 2015 - 2025 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ime.broadcast

import android.view.inputmethod.EditorInfo
import com.jiaoay.jrime.core.Candidates
import com.jiaoay.jrime.core.CompositionProto
import com.jiaoay.jrime.core.RimeMessage
import com.jiaoay.jrime.core.SchemaItem
import com.jiaoay.jrime.core.StatusProto

interface InputBroadcastReceiver {
    fun onStartInput(info: EditorInfo) {}

    fun onSelectionUpdate(start: Int, end: Int) {}

    fun onRimeSchemaUpdated(schema: SchemaItem) {}

    fun onRimeOptionUpdated(value: RimeMessage.OptionMessage.Data) {}

    fun onCandidateListUpdate(data: Candidates.Bulk) {}

    fun onCompositionUpdate(data: CompositionProto) {}

    fun onKeyAppearanceUpdate(composing: Boolean, menu: Boolean, paging: Boolean) {}

    fun onInputStatusUpdate(value: StatusProto) {}

    fun onEnterKeyLabelUpdate(label: String) {}
}
