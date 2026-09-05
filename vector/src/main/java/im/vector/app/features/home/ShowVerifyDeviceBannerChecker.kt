/*
 * Copyright 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home

import org.matrix.android.sdk.api.MatrixPatterns.getServerName
import java.security.MessageDigest
import javax.inject.Inject

class ShowVerifyDeviceBannerChecker @Inject constructor() {
    companion object {
        private val EXCLUDED_HASHED_DOMAINS = setOf(
                "9e6d1ca3e739dd3f879b8046af783402a34d247f879dfa1b531edbd56a56c1a6",
        )
    }

    @Suppress("UNUSED_PARAMETER")
    fun canShowVerifyDeviceBanner(userId: String): Boolean {
        // The unverified-device banner is not shown in this build.
        return false
    }
}
