/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.maintenance

import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MaintenanceActivity : AppCompatActivity() {

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

        val message = TextView(this).apply {
            text = "The server is currently undergoing maintenance.\nPlease try again later."
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTextColor(Color.DKGRAY)
            gravity = Gravity.CENTER
        }

        layout.addView(title)
        layout.addView(message)
        setContentView(layout)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Block back navigation — user must wait for server to come back
    }
}
