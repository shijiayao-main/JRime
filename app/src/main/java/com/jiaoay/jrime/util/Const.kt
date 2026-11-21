/*
 * SPDX-FileCopyrightText: 2015 - 2025 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.util

import com.jiaoay.jrime.BuildConfig

object Const {
    const val VERSION_NAME = "${BuildConfig.BUILD_VERSION_NAME}-${BuildConfig.BUILD_TYPE}"
    const val LICENSE_SPDX_ID = "GPL-3.0-or-later"
    const val LICENSE_URL = "https://www.gnu.org/licenses/gpl-3.0.html"
    const val PRIVACY_POLICY_URL = "https://github.com/shijiayao-main/JRime/blob/main/PRIVACY.md"
    const val LIBRIME_URL = "https://github.com/rime/librime"
    const val OPENCC_URL = "https://github.com/BYVoid/OpenCC"

    /** Upstream engine projects JRime is built on (credits / attribution). */
    const val RIME_URL = "https://rime.im/"
    const val TRIME_UPSTREAM_URL = "https://github.com/osfans/trime"
}
