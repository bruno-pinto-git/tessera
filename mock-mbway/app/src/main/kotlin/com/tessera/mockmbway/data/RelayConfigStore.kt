package com.tessera.mockmbway.data

import android.content.Context

class RelayConfigStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun loadHost(): String? = prefs.getString(KEY_HOST, null)

    fun loadSecret(): String? = prefs.getString(KEY_SECRET, null)

    fun save(host: String, secret: String) {
        prefs.edit()
            .putString(KEY_HOST, host)
            .putString(KEY_SECRET, secret)
            .apply()
    }

    private companion object {
        const val FILE_NAME = "tessera_mockmbway_relay"
        const val KEY_HOST = "host"
        const val KEY_SECRET = "secret"
    }
}
