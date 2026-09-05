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

import org.matrix.android.sdk.api.util.JsonDict
import retrofit2.http.GET

/**
 * Minimal API used only to corroborate a token error before the session is destroyed.
 * See [TokenValidityChecker].
 */
internal interface TokenValidityApi {
    /**
     * Gets information about the owner of a given access token.
     * Answers 200 when the token is still usable, 401 M_UNKNOWN_TOKEN when it is not.
     */
    @GET(NetworkConstants.URI_API_PREFIX_PATH_V3 + "account/whoami")
    suspend fun whoAmI(): JsonDict
}
