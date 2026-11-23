package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.appVersionDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "app_version_preferences"
)

interface VersionPreferences {
    suspend fun getLastSeenVersion(): String?
    suspend fun setLastSeenVersion(versionName: String)
}

class DataStoreVersionPreferences(
    private val context: Context
) : VersionPreferences {

    private val lastSeenVersionKey = stringPreferencesKey("last_seen_version")

    override suspend fun getLastSeenVersion(): String? {
        return context.appVersionDataStore.data.first()[lastSeenVersionKey]
    }

    override suspend fun setLastSeenVersion(versionName: String) {
        context.appVersionDataStore.edit { prefs ->
            prefs[lastSeenVersionKey] = versionName
        }
    }
}

object VersionComparator {
    fun isVersionNewer(installed: String, stored: String): Boolean {
        val installedParts = installed.trim().split(".").map { it.toIntOrNull() ?: 0 }
        val storedParts = stored.trim().split(".").map { it.toIntOrNull() ?: 0 }
        val maxLen = maxOf(installedParts.size, storedParts.size)
        for (i in 0 until maxLen) {
            val inst = installedParts.getOrElse(i) { 0 }
            val stor = storedParts.getOrElse(i) { 0 }
            if (inst > stor) return true
            if (inst < stor) return false
        }
        return false
    }
}
