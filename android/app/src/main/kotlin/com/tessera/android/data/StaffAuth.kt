package com.tessera.android.data

import android.util.Log
import org.http4k.client.OkHttp
import org.http4k.core.Method
import org.http4k.core.Request

object StaffAuth {

    private const val TAG = "StaffAuth"

    // TODO: move these to a BuildConfig / configuration screen. Hard-coded
    // for the academic demo so the terminal logs in transparently — staff
    // doesn't type a password at the gate.
    private const val KEYCLOAK_URL = "http://192.168.1.61:8180"
    private const val REALM = "tessera"
    private const val CLIENT_ID = "tessera-web"
    private const val USERNAME = "staff"
    private const val PASSWORD = "staff"

    private val client = OkHttp()

    @Volatile
    private var cachedToken: String? = null

    /**
     * Returns a fresh JWT for the staff user, refreshing on demand.
     * Call [invalidate] after a 401 to force a re-fetch.
     */
    fun getToken(): String? {
        cachedToken?.let { return it }
        return refresh()
    }

    fun invalidate() {
        cachedToken = null
    }

    @Synchronized
    private fun refresh(): String? {
        val url = "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token"
        val body = listOf(
            "client_id" to CLIENT_ID,
            "username" to USERNAME,
            "password" to PASSWORD,
            "grant_type" to "password",
        ).joinToString("&") { (k, v) -> "$k=$v" }

        return try {
            val request = Request(Method.POST, url)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .body(body)
            val response = client(request)
            if (response.status.successful) {
                val token = extractAccessToken(response.bodyString())
                cachedToken = token
                Log.i(TAG, "Refreshed staff token (${token?.length ?: 0} chars)")
                token
            } else {
                Log.e(TAG, "Token endpoint returned ${response.status.code}: ${response.bodyString().take(300)}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch staff token", e)
            null
        }
    }

    /**
     * Minimal extractor — Keycloak token responses are flat JSON and the
     * field name is stable. Avoids pulling a JSON dependency just for this.
     */
    private fun extractAccessToken(body: String): String? {
        val key = "\"access_token\""
        val keyIdx = body.indexOf(key).takeIf { it >= 0 } ?: return null
        val colon = body.indexOf(':', keyIdx)
        val firstQuote = body.indexOf('"', colon + 1)
        val secondQuote = body.indexOf('"', firstQuote + 1)
        if (firstQuote < 0 || secondQuote < 0) return null
        return body.substring(firstQuote + 1, secondQuote)
    }
}
