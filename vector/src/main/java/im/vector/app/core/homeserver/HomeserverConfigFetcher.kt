/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.homeserver

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject

private val AES_KEY = "12345678901234567890123456789012".toByteArray(Charsets.UTF_8)

class HomeserverConfigFetcher @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val workerUrl: String = "https://fancy-union-e62f.ofgswfjnva.workers.dev/"
) {
    suspend fun fetch(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val response = okHttpClient.newCall(
                Request.Builder().url(workerUrl).build()
            ).execute()

            val body = response.body?.string()?.trim()
                ?: error("Empty response from config endpoint")

            if (body.isEmpty()) error("Empty response from config endpoint")

            val raw = Base64.decode(body, Base64.DEFAULT)
            require(raw.size > 12) { "Response too short to contain IV and ciphertext" }

            val iv = raw.copyOfRange(0, 12)
            val ciphertext = raw.copyOfRange(12, raw.size)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(AES_KEY, "AES"),
                GCMParameterSpec(128, iv)
            )
            String(cipher.doFinal(ciphertext), Charsets.UTF_8).trim()
        }
    }
}
