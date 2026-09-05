/*
 * Copyright 2024 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.internal.network

import dagger.Lazy
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.failure.MatrixError
import org.matrix.android.sdk.internal.session.SessionScope
import retrofit2.Retrofit
import timber.log.Timber
import java.net.HttpURLConnection
import javax.inject.Inject

/**
 * Outcome of a token corroboration.
 */
internal sealed interface TokenVerdict {
    /**
     * The homeserver confirmed the token is gone: the device was deleted, the password was changed,
     * or the account was deactivated / locked / suspended. The session must be destroyed.
     */
    data class Invalid(val softLogout: Boolean, val reason: String) : TokenVerdict

    /**
     * The homeserver still accepts the token. The 401 we reacted to was spurious and must be ignored.
     */
    object Valid : TokenVerdict

    /**
     * We could not find out (network error, 5xx, edge error page...). We must NOT destroy the session:
     * if the token really is dead the next request will tell us again.
     */
    data class Inconclusive(val reason: String) : TokenVerdict

    /**
     * The homeserver answered, but it does not implement the endpoint we corroborate with. There is nothing
     * to check against, so the original error has to be trusted - otherwise a genuinely deleted or banned
     * account would never be signed out on such a deployment.
     */
    data class Unverifiable(val reason: String) : TokenVerdict
}

/**
 * Asks the homeserver whether an access token really is invalid, before the app destroys the local session.
 *
 * A single unverified 401 used to be enough to wipe the credentials, the Realm encryption keys and the
 * session files, with no retry and no undo. Any 401 that did not come from a genuine token revocation -
 * a spurious edge response, a background pusher call, a half read body - cost the user their account.
 *
 * This deliberately keeps every real reason for a logout working: a deleted device, a deactivated,
 * locked or suspended account all answer with an authenticated error here and are reported as [TokenVerdict.Invalid].
 */
@SessionScope
internal class TokenValidityChecker @Inject constructor(
        private val retrofit: Lazy<Retrofit>,
) {
    private val mutex = Mutex()
    private var lastVerdict: TokenVerdict? = null
    private var lastVerdictAt = 0L

    /**
     * Corroborates a token error against the homeserver. Concurrent callers share a single request, and a
     * verdict is reused for a few seconds so a burst of parallel 401s does not become a burst of whoami calls.
     */
    suspend fun check(): TokenVerdict = mutex.withLock {
        val cached = lastVerdict
        if (cached != null && System.currentTimeMillis() - lastVerdictAt < VERDICT_TTL_MILLIS) {
            Timber.w("TokenValidityChecker: reusing recent verdict $cached")
            return@withLock cached
        }
        val verdict = query()
        lastVerdict = verdict
        lastVerdictAt = System.currentTimeMillis()
        Timber.w("TokenValidityChecker: verdict is $verdict")
        verdict
    }

    private suspend fun query(): TokenVerdict {
        return try {
            // globalErrorReceiver is null on purpose: this call must never be able to trigger a logout by itself.
            executeRequest(null) {
                retrofit.get().create(TokenValidityApi::class.java).whoAmI()
            }
            TokenVerdict.Valid
        } catch (failure: Throwable) {
            failure.toVerdict()
        }
    }

    private fun Throwable.toVerdict(): TokenVerdict {
        if (this !is Failure.ServerError) {
            // IOException, unrecognised body, 5xx wrapped as OtherServerError... we simply do not know.
            return TokenVerdict.Inconclusive(this.javaClass.simpleName)
        }
        return when {
            httpCode == HttpURLConnection.HTTP_UNAUTHORIZED && error.code == MatrixError.M_UNKNOWN_TOKEN -> {
                TokenVerdict.Invalid(softLogout = error.isSoftLogout.orFalse(), reason = error.code)
            }
            error.code in ACCOUNT_GONE_ERROR_CODES -> {
                // The account itself is deactivated, locked or suspended. Signing the user out is correct.
                TokenVerdict.Invalid(softLogout = error.isSoftLogout.orFalse(), reason = error.code)
            }
            httpCode == HttpURLConnection.HTTP_NOT_FOUND && error.code == MatrixError.M_UNRECOGNIZED -> {
                // The homeserver does not expose /account/whoami, so it cannot corroborate anything.
                TokenVerdict.Unverifiable("${error.code} ($httpCode)")
            }
            else -> TokenVerdict.Inconclusive("${error.code} ($httpCode)")
        }
    }

    companion object {
        private const val VERDICT_TTL_MILLIS = 10_000L

        private val ACCOUNT_GONE_ERROR_CODES = setOf(
                MatrixError.M_USER_DEACTIVATED,
                MatrixError.M_USER_LOCKED,
                MatrixError.M_USER_SUSPENDED,
        )
    }
}
