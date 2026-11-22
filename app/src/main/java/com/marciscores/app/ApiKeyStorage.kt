
package com.marciscores.app

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object ApiKeyStorage {

    private const val PREFS_NAME = "marciscores_secure_prefs"
    private const val API_KEY_NAME = "statpal_api_key"

    private fun preferences(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun save(context: Context, apiKey: String) {
        preferences(context)
            .edit()
            .putString(API_KEY_NAME, apiKey.trim())
            .apply()
    }

    fun load(context: Context): String {
        return preferences(context)
            .getString(API_KEY_NAME, "")
            .orEmpty()
    }

    fun delete(context: Context) {
        preferences(context)
            .edit()
            .remove(API_KEY_NAME)
            .apply()
    }

    fun hasKey(context: Context): Boolean {
        return load(context).isNotBlank()
    }
}
