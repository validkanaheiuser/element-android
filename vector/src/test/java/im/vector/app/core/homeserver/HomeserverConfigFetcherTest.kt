/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.homeserver

import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HomeserverConfigFetcherTest {

    private lateinit var server: MockWebServer
    private lateinit var fetcher: HomeserverConfigFetcher

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val client = OkHttpClient()
        fetcher = HomeserverConfigFetcher(client, server.url("/").toString())
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun encryptUrl(plaintext: String): String {
        val key = "12345678901234567890123456789012".toByteArray(Charsets.UTF_8)
        val iv = ByteArray(12) { it.toByte() } // deterministic IV for tests
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        val ct = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val combined = iv + ct
        return Base64.getEncoder().encodeToString(combined).trim()
    }

    @Test
    fun `fetch returns decrypted homeserver URL on success`() = runTest {
        val expected = "https://elmchats.com"
        server.enqueue(MockResponse().setBody(encryptUrl(expected)))

        val result = fetcher.fetch()

        assertTrue(result.isSuccess)
        assertEquals(expected, result.getOrThrow())
    }

    @Test
    fun `fetch returns failure on network error`() = runTest {
        server.shutdown()

        val result = fetcher.fetch()

        assertTrue(result.isFailure)
    }

    @Test
    fun `fetch returns failure on empty body`() = runTest {
        server.enqueue(MockResponse().setBody(""))

        val result = fetcher.fetch()

        assertTrue(result.isFailure)
    }
}
