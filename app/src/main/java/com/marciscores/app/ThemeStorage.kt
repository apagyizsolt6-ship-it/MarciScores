package com.marciscores.app

import android.content.Context
import android.content.SharedPreferences

object ThemeStorage {
    private const val PREFS_NAME = "marciscores_theme"
    private const val KEY_DARK = "is_dark_theme"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isDark(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_DARK, true) // alapból sötét
    }

    fun setDark(context: Context, isDark: Boolean) {
        prefs(context).edit().putBoolean(KEY_DARK, isDark).apply()
    }
}
