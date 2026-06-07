package com.dashboard

import android.content.Context
import androidx.core.content.edit

object PreferenceManager {
    private const val PREF_NAME = "dashboard_prefs"

    fun saveIp(context: Context, gameType: GameType, ip: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit { putString("ip_${gameType.name}", ip) }
    }

    fun getIp(context: Context, gameType: GameType): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString("ip_${gameType.name}", "192.168.1.1") ?: "192.168.1.1"
    }
}