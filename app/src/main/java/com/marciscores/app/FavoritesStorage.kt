package com.marciscores.app

import android.content.Context
import android.content.SharedPreferences

object FavoritesStorage {
    private const val PREFS_NAME = "marciscores_favorites"
    private const val KEY_TEAMS = "favorite_teams"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getFavorites(context: Context): Set<String> {
        return prefs(context).getStringSet(KEY_TEAMS, emptySet()) ?: emptySet()
    }

    fun isFavorite(context: Context, teamName: String): Boolean {
        return getFavorites(context).contains(teamName.trim())
    }

    fun toggle(context: Context, teamName: String) {
        val name = teamName.trim()
        if (name.isBlank()) return
        val current = getFavorites(context).toMutableSet()
        if (current.contains(name)) {
            current.remove(name)
        } else {
            current.add(name)
        }
        prefs(context).edit().putStringSet(KEY_TEAMS, current).apply()
    }

    fun add(context: Context, teamName: String) {
        val name = teamName.trim()
        if (name.isBlank()) return
        val current = getFavorites(context).toMutableSet()
        current.add(name)
        prefs(context).edit().putStringSet(KEY_TEAMS, current).apply()
    }
}
