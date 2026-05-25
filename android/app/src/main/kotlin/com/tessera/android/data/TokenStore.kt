package com.tessera.android.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.tessera.android.shared.TokenSet

class TokenStore(context: Context) {

    private val prefs = EncryptedSharedPreferences.create(
        context.applicationContext,
        FILE_NAME,
        MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun save(set: TokenSet) {
        val editor = prefs.edit()
            .putString(KEY_ACCESS, set.accessToken)
            .putLong(KEY_EXPIRES_AT, set.expiresAtMs)
        if (set.refreshToken != null) {
            editor.putString(KEY_REFRESH, set.refreshToken)
        } else {
            editor.remove(KEY_REFRESH)
        }
        if (set.refreshExpiresAtMs != null) {
            editor.putLong(KEY_REFRESH_EXPIRES_AT, set.refreshExpiresAtMs)
        } else {
            editor.remove(KEY_REFRESH_EXPIRES_AT)
        }
        editor.apply()
    }

    fun load(): TokenSet? {
        val access = prefs.getString(KEY_ACCESS, null) ?: return null
        return TokenSet(
            accessToken = access,
            refreshToken = prefs.getString(KEY_REFRESH, null),
            expiresAtMs = prefs.getLong(KEY_EXPIRES_AT, 0L),
            refreshExpiresAtMs = prefs.getLong(KEY_REFRESH_EXPIRES_AT, 0L).takeIf { it > 0L },
        )
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val FILE_NAME = "tessera_auth"
        const val KEY_ACCESS = "access_token"
        const val KEY_REFRESH = "refresh_token"
        const val KEY_EXPIRES_AT = "expires_at"
        const val KEY_REFRESH_EXPIRES_AT = "refresh_expires_at"
    }
}
