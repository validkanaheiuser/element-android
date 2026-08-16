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
import org.json.JSONArray
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject

class HomeserverConfigFetcher @Inject constructor() {

    private val okHttpClient = OkHttpClient()
    private val workerUrl = "https://fancy-union-e62f.ofgswfjnva.workers.dev/"
    private val AES_KEY = "12345678901234567890123456789012".toByteArray(Charsets.UTF_8)

    suspend fun fetch(): Result<List<ServerConfig>> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url(workerUrl).build()
            val body = okHttpClient.newCall(request).execute().use { response ->
                response.body?.string() ?: throw Exception("Empty response")
            }.trim()
            val raw = Base64.decode(body, Base64.DEFAULT)
            val iv = raw.copyOfRange(0, 12)
            val ciphertext = raw.copyOfRange(12, raw.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(AES_KEY, "AES"), GCMParameterSpec(128, iv))
            val json = String(cipher.doFinal(ciphertext), Charsets.UTF_8).trim()
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                ServerConfig(
                    nickname = obj.getString("nickname"),
                    url = obj.getString("url")
                )
            }
        }
    }
}
