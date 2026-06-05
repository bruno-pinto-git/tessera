package com.tessera.android.data

import android.content.Context

class ServerConfigStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun load(): String? = prefs.getString(KEY_HOST, null)

    fun save(host: String) {
        prefs.edit().putString(KEY_HOST, host).apply()
    }

    private companion object {
        const val FILE_NAME = "tessera_server"
        const val KEY_HOST = "host"
    }
}
