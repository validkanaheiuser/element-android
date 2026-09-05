/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.log

import android.util.Log
import timber.log.Timber

/**
 * Forwards a short, explicitly allow-listed set of tags to logcat, in release builds too.
 *
 * Release builds plant no DebugTree, so until now the only record of a push was VectorFileLogger's file
 * under filesDir - which cannot be read on a non-rooted device. Everything that happens when a push wakes
 * a cold process lived in that blind spot, including the single line the push handler logs when it gives
 * up. These tags stay visible so the next failure can be read directly:
 *
 *     adb logcat -s SYNC/Push:V
 *
 * Only the push path is allow-listed; message contents are never logged at these tags.
 */
class DiagnosticLogTree : Timber.Tree() {

    override fun isLoggable(tag: String?, priority: Int): Boolean {
        return priority >= Log.DEBUG && tag != null && ALLOWED_TAG_PREFIXES.any { tag.startsWith(it) }
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val body = if (t == null) message else message + "\n" + Log.getStackTraceString(t)
        Log.println(priority, tag ?: "Aero", body)
    }

    companion object {
        private val ALLOWED_TAG_PREFIXES = listOf("SYNC/Push")
    }
}
