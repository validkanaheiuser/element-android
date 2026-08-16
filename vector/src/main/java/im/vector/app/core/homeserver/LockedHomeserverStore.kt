/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.homeserver

import android.content.SharedPreferences
import im.vector.app.core.di.DefaultPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LockedHomeserverStore @Inject constructor(
    @DefaultPreferences private val sharedPreferences: SharedPreferences,
) {
    companion object {
        private const val KEY_LOCKED_URL = "locked_homeserver_url"
    }

    fun getLockedUrl(): String? = sharedPreferences.getString(KEY_LOCKED_URL, null)

    fun setLockedUrl(url: String) {
        sharedPreferences.edit().putString(KEY_LOCKED_URL, url).apply()
    }

    fun isLocked(): Boolean = getLockedUrl() != null
}
