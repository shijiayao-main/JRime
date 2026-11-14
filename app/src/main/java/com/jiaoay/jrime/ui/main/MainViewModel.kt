// SPDX-FileCopyrightText: 2015 - 2024 Rime community
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.jiaoay.jrime.ui.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jiaoay.jrime.daemon.RimeDaemon
import com.jiaoay.jrime.daemon.RimeSession

class MainViewModel : ViewModel() {
    val toolbarTitle = MutableLiveData<String>()

    val topOptionsMenu = MutableLiveData<Boolean>()

    val rime = RimeDaemon.createSession(javaClass.name)

    val toolbarEditButtonVisible = MutableLiveData(false)

    val toolbarEditButtonOnClickListener = MutableLiveData<(() -> Unit)?>()

    val toolbarDeleteButtonOnClickListener = MutableLiveData<(() -> Unit)?>()

    fun setToolbarTitle(title: String) {
        toolbarTitle.value = title
    }

    fun enableTopOptionsMenu() {
        topOptionsMenu.value = true
    }

    fun disableTopOptionsMenu() {
        topOptionsMenu.value = false
    }

    fun enableToolbarEditButton(
        visible: Boolean = true,
        onClick: () -> Unit,
    ) {
        toolbarEditButtonOnClickListener.value = onClick
        toolbarEditButtonVisible.value = visible
    }

    fun disableToolbarEditButton() {
        toolbarEditButtonOnClickListener.value = null
        hideToolbarEditButton()
    }

    fun hideToolbarEditButton() {
        toolbarEditButtonVisible.value = false
    }

    fun showToolbarEditButton() {
        toolbarEditButtonVisible.value = true
    }

    fun enableToolbarDeleteButton(onClick: () -> Unit) {
        toolbarDeleteButtonOnClickListener.value = onClick
    }

    fun disableToolbarDeleteButton() {
        toolbarDeleteButtonOnClickListener.value = null
    }

    override fun onCleared() {
        RimeDaemon.destroySession(javaClass.name)
    }
}
