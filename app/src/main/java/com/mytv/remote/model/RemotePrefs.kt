package com.mytv.remote.model

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "tv_remote_prefs")

/**
 * Persists (a) which buttons the user wants and in what order, and
 * (b) the last-paired TV's address + client certificate alias, so the
 * app can reconnect without re-pairing every launch.
 */
class RemotePrefs(private val context: Context) {

    private object Keys {
        val LAYOUT = stringPreferencesKey("layout_ordered_csv")
        val TV_HOST = stringPreferencesKey("tv_host")
        val TV_NAME = stringPreferencesKey("tv_name")
        val CERT_ALIAS = stringPreferencesKey("cert_alias")
        val PAIRED_HOSTS = stringSetPreferencesKey("paired_hosts")
    }

    val layout: Flow<List<String>> = context.dataStore.data.map { prefs ->
        prefs[Keys.LAYOUT]?.split(",")?.filter { it.isNotBlank() }
            ?: RemoteButton.DEFAULT_LAYOUT
    }

    suspend fun saveLayout(orderedIds: List<String>) {
        context.dataStore.edit { it[Keys.LAYOUT] = orderedIds.joinToString(",") }
    }

    val lastTv: Flow<Pair<String, String>?> = context.dataStore.data.map { prefs ->
        val host = prefs[Keys.TV_HOST]
        val name = prefs[Keys.TV_NAME]
        if (host != null && name != null) host to name else null
    }

    suspend fun saveLastTv(host: String, name: String, certAlias: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.TV_HOST] = host
            prefs[Keys.TV_NAME] = name
            prefs[Keys.CERT_ALIAS] = certAlias
            val existing = prefs[Keys.PAIRED_HOSTS] ?: emptySet()
            prefs[Keys.PAIRED_HOSTS] = existing + host
        }
    }

    suspend fun isPaired(host: String): Boolean {
        val hosts = context.dataStore.data.map { it[Keys.PAIRED_HOSTS] ?: emptySet() }.first()
        return host in hosts
    }
}
