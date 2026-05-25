package com.tessera.android.data

import android.content.Context
import android.util.Log
import com.tessera.android.shared.AuthSession
import com.tessera.android.shared.TokenSet
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.http4k.client.OkHttp
import org.http4k.core.Method
import org.http4k.core.Request
import org.json.JSONObject

class KeycloakClient(context: Context) {

    private val tag = "KeycloakClient"
    private val client = OkHttp()
    private val refreshMutex = Mutex()
    private val tokenStore = TokenStore(context)

    suspend fun login(username: String, password: String): Result<TokenSet> =
        withContext(Dispatchers.IO) {
            runCatching {
                exchange(
                    form(
                        "grant_type" to "password",
                        "client_id" to KeycloakConfig.CLIENT_ID,
                        "username" to username,
                        "password" to password,
                        "scope" to KeycloakConfig.SCOPES.joinToString(" "),
                    ),
                )
            }
        }

    suspend fun freshAccessToken(): String? {
        val current = AuthSession.tokens.value ?: return null
        if (System.currentTimeMillis() < current.expiresAtMs - SKEW_MS) return current.accessToken
        return refreshMutex.withLock {
            val latest = AuthSession.tokens.value ?: return@withLock null
            if (System.currentTimeMillis() < latest.expiresAtMs - SKEW_MS) return@withLock latest.accessToken
            val refresh = latest.refreshToken ?: return@withLock null
            runCatching {
                withContext(Dispatchers.IO) {
                    exchange(
                        form(
                            "grant_type" to "refresh_token",
                            "client_id" to KeycloakConfig.CLIENT_ID,
                            "refresh_token" to refresh,
                        ),
                    )
                }
            }.onFailure {
                Log.w(tag, "Refresh failed", it)
                AuthSession.clear()
                tokenStore.clear()
            }.getOrNull()?.accessToken
        }
    }

    suspend fun bootstrap() {
        val saved = runCatching { tokenStore.load() }.getOrNull() ?: return
        AuthSession.update(saved)
        val refreshExpired = saved.refreshExpiresAtMs?.let {
            System.currentTimeMillis() >= it - SKEW_MS
        } ?: false
        if (refreshExpired) {
            Log.i(tag, "Saved refresh token expired — clearing session")
            AuthSession.clear()
            tokenStore.clear()
            return
        }
        if (System.currentTimeMillis() >= saved.expiresAtMs - SKEW_MS) {
            Log.i(tag, "Saved access token expired — attempting refresh")
            freshAccessToken()
        }
    }

    fun logout() {
        AuthSession.clear()
        tokenStore.clear()
    }

    fun dispose() = Unit

    private fun exchange(formBody: String): TokenSet {
        val req = Request(Method.POST, "${KeycloakConfig.ISSUER}/protocol/openid-connect/token")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Accept", "application/json")
            .body(formBody)
        val resp = client(req)
        if (!resp.status.successful) {
            val msg = "Token endpoint returned ${resp.status.code}"
            Log.w(tag, "$msg — ${resp.bodyString().take(200)}")
            throw IllegalStateException(msg)
        }
        val json = JSONObject(resp.bodyString())
        val now = System.currentTimeMillis()
        val accessToken = json.getString("access_token")
        val refreshToken = if (json.has("refresh_token")) json.getString("refresh_token") else null
        val expiresIn = json.optInt("expires_in", 300)
        val refreshExpiresIn =
            if (json.has("refresh_expires_in")) json.optInt("refresh_expires_in") else null
        val set = TokenSet(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAtMs = now + expiresIn * 1000L,
            refreshExpiresAtMs = refreshExpiresIn?.let { now + it * 1000L },
        )
        AuthSession.update(set)
        tokenStore.save(set)
        Log.i(tag, "Token exchange OK — user=${AuthSession.username}, roles=${AuthSession.roles}")
        return set
    }

    private fun form(vararg pairs: Pair<String, String>): String =
        pairs.joinToString("&") { (k, v) ->
            "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
        }

    private companion object {
        const val SKEW_MS = 5_000L
    }
}
