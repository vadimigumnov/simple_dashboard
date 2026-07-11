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

    fun savePort(context: Context, gameType: GameType, port: String) {
        val prefs = context.getSharedPreferences("DashboardPrefs", Context.MODE_PRIVATE)
        prefs.edit { putString("port_${gameType.name}", port) }
    }

    fun getPort(context: Context, gameType: GameType): String {
        val prefs = context.getSharedPreferences("DashboardPrefs", Context.MODE_PRIVATE)

        val defaultPort = when (gameType) {
            GameType.SIMHUB -> "2080"
            GameType.RBR -> "6776"
            GameType.GT7 -> "33740"
            GameType.AC -> "9996"
        }

        return prefs.getString("port_${gameType.name}", defaultPort) ?: defaultPort
    }
}