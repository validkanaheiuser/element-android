/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.maintenance

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.homeserver.HomeserverConfigFetcher
import im.vector.app.core.homeserver.LockedHomeserverStore
import im.vector.app.features.MainActivity
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Shown when the remote server configuration cannot be fetched and nothing has been cached yet.
 *
 * This screen used to be a dead end: no way to retry, and the back button was swallowed, so a user who hit it
 * had to clear the app data to escape. It now offers a retry and lets the back button work normally.
 */
@AndroidEntryPoint
class MaintenanceActivity : AppCompatActivity() {

    @Inject lateinit var homeserverConfigFetcher: HomeserverConfigFetcher
    @Inject lateinit var lockedHomeserverStore: LockedHomeserverStore

    private lateinit var messageView: TextView
    private lateinit var retryButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.WHITE)
            setPadding(64, 64, 64, 64)
        }

        val title = TextView(this).apply {
            text = "Server Unavailable"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 24)
        }

        messageView = TextView(this).apply {
            text = DEFAULT_MESSAGE
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTextColor(Color.DKGRAY)
            gravity = Gravity.CENTER
        }

        retryButton = Button(this).apply {
            text = "Try again"
            setOnClickListener { retry() }
        }

        layout.addView(title)
        layout.addView(messageView)
        layout.addView(retryButton)
        setContentView(layout)
    }

    private fun retry() {
        retryButton.isEnabled = false
        messageView.text = "Checking..."

        lifecycleScope.launch {
            val servers = homeserverConfigFetcher.fetch().getOrNull().orEmpty()
            if (servers.isEmpty()) {
                messageView.text = DEFAULT_MESSAGE
                retryButton.isEnabled = true
                return@launch
            }

            lockedHomeserverStore.setServerList(servers)
            if (lockedHomeserverStore.getSelectedUrl() == null) {
                lockedHomeserverStore.setSelectedUrl(servers.first().url)
            }

            startActivity(
                    Intent(this@MaintenanceActivity, MainActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
            finish()
        }
    }

    companion object {
        private const val DEFAULT_MESSAGE = "The server is currently undergoing maintenance.\nPlease try again later."
    }
}
