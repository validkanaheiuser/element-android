/*
 * Copyright 2021 The Matrix.org Foundation C.I.C.
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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.failure.GlobalError
import org.matrix.android.sdk.internal.auth.SessionParamsStore
import org.matrix.android.sdk.internal.di.SessionId
import org.matrix.android.sdk.internal.session.SessionScope
import org.matrix.android.sdk.internal.task.TaskExecutor
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@SessionScope
internal class GlobalErrorHandler @Inject constructor(
        private val taskExecutor: TaskExecutor,
        private val sessionParamsStore: SessionParamsStore,
        private val tokenValidityChecker: TokenValidityChecker,
        @SessionId private val sessionId: String
) : GlobalErrorReceiver {

    var listener: Listener? = null

    /** A confirmed token error ends the session once; further ones are noise. */
    private val invalidTokenReported = AtomicBoolean(false)

    override fun handleGlobalError(globalError: GlobalError) {
        Timber.e("Global error received: $globalError")

        if (globalError is GlobalError.InvalidToken) {
            // Do NOT act on a single, unverified 401. Ask the homeserver first - see handleInvalidToken().
            taskExecutor.executorScope.launch(Dispatchers.IO) {
                handleInvalidToken(globalError)
            }
            return
        }

        listener?.onGlobalError(globalError)
    }

    /**
     * Corroborates a token error with the homeserver before the session is destroyed.
     *
     * Destroying a session is irreversible: it deletes the credentials, the Realm encryption keys and the
     * session files. It must therefore only happen when the homeserver actually says the token is gone -
     * a deleted device, a changed password, or a deactivated / locked / suspended account. Those all still
     * sign the user out, exactly as before. What no longer signs them out is a single unconfirmed 401,
     * which may come from a transient failure or from a background call that has no business ending a session.
     */
    private suspend fun handleInvalidToken(globalError: GlobalError.InvalidToken) {
        if (invalidTokenReported.get()) {
            Timber.w("Invalid token already confirmed and reported, ignoring")
            return
        }

        when (val verdict = tokenValidityChecker.check()) {
            is TokenVerdict.Valid -> {
                Timber.w("Ignoring an invalid token error: the homeserver still accepts our token")
                listener?.onInvalidTokenDismissed()
            }
            is TokenVerdict.Inconclusive -> {
                // Keeping the session is the safe choice: a token that really is dead will fail again.
                Timber.w("Ignoring an invalid token error, could not corroborate it (${verdict.reason})")
                listener?.onInvalidTokenDismissed()
            }
            is TokenVerdict.Invalid -> {
                // The homeserver is authoritative about soft logout, prefer its answer.
                signOut(GlobalError.InvalidToken(softLogout = verdict.softLogout || globalError.softLogout), verdict.reason)
            }
            is TokenVerdict.Unverifiable -> {
                // Nothing to corroborate against, so behave as the app always did and trust the error.
                signOut(globalError, "unverifiable: ${verdict.reason}")
            }
        }
    }

    private fun signOut(confirmed: GlobalError.InvalidToken, reason: String) {
        if (!invalidTokenReported.compareAndSet(false, true)) return

        Timber.e("Confirmed invalid token ($reason), signing out. softLogout=${confirmed.softLogout}")
        if (confirmed.softLogout) {
            // Mark the token as invalid
            taskExecutor.executorScope.launch(Dispatchers.IO) {
                sessionParamsStore.setTokenInvalid(sessionId)
            }
        }
        listener?.onGlobalError(confirmed)
    }

    internal interface Listener {
        fun onGlobalError(globalError: GlobalError)

        /**
         * A token error was received but the homeserver did not confirm it, so the session is kept.
         * The sync thread stops itself on any token error and only resumes when asked, so it has to be
         * woken back up here - otherwise the app would stay logged in but silently stop syncing.
         */
        fun onInvalidTokenDismissed()
    }
}
