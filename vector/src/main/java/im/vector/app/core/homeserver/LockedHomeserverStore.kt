/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.homeserver

import android.content.SharedPreferences
import im.vector.app.core.di.DefaultPreferences
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LockedHomeserverStore @Inject constructor(
    @DefaultPreferences private val sharedPreferences: SharedPreferences,
) {
    fun getServerList(): List<ServerConfig> {
        val json = sharedPreferences.getString(KEY_SERVER_LIST, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                ServerConfig(obj.getString("nickname"), obj.getString("url"))
            }
        } catch (e: Exception) { emptyList() }
    }

    fun setServerList(servers: List<ServerConfig>) {
        val arr = JSONArray()
        servers.forEach { s ->
            arr.put(JSONObject().apply {
                put("nickname", s.nickname)
                put("url", s.url)
            })
        }
        sharedPreferences.edit().putString(KEY_SERVER_LIST, arr.toString()).apply()
    }

    fun getSelectedUrl(): String? = sharedPreferences.getString(KEY_SELECTED_URL, null)

    fun setSelectedUrl(url: String) {
        sharedPreferences.edit().putString(KEY_SELECTED_URL, url).apply()
    }

    fun isConfigured(): Boolean = getServerList().isNotEmpty()

    companion object {
        private const val KEY_SERVER_LIST = "server_list_json"
        private const val KEY_SELECTED_URL = "selected_homeserver_url"
    }
}
