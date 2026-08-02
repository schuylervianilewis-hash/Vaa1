package com.example.manager

import android.content.Context
import android.webkit.WebStorage
import com.example.logging.LogKeeper

object CacheManager {
    private const val PREF_KEY_CLEAR_CACHE_ON_EXIT = "clear_cache_on_exit"

    fun isClearCacheOnExitEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences("vaa_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean(PREF_KEY_CLEAR_CACHE_ON_EXIT, true)
    }

    fun setClearCacheOnExitEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences("vaa_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean(PREF_KEY_CLEAR_CACHE_ON_EXIT, enabled).apply()
    }

    fun performCacheCleanupIfEnabled(context: Context) {
        if (isClearCacheOnExitEnabled(context)) {
            try {
                WebStorage.getInstance().deleteAllData()
                LogKeeper.log(context, "CACHE_CLEANUP", "WebStorage and page cache cleared on exit (sessions/cookies preserved)")
            } catch (e: Exception) {
                LogKeeper.log(context, "CACHE_CLEANUP_ERROR", "Failed to clear WebStorage: ${e.message}", e)
            }
        }
    }
}
