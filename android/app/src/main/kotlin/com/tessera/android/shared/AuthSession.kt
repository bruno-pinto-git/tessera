package com.tessera.android.shared

import android.util.Base64
import androidx.compose.runtime.mutableStateOf

data class TokenSet(
    val accessToken: String,
    val refreshToken: String?,
    val expiresAtMs: Long,
    val refreshExpiresAtMs: Long?,
)

object AuthSession {

    val tokens = mutableStateOf<TokenSet?>(null)

    val isAuthenticated: Boolean
        get() = tokens.value != null

    val accessToken: String?
        get() = tokens.value?.accessToken

    val roles: List<String>
        get() = parseRealmRoles(accessToken)

    val username: String?
        get() = parseClaim(accessToken, "preferred_username")

    fun update(set: TokenSet) {
        tokens.value = set
    }

    fun clear() {
        tokens.value = null
    }

    private fun parseClaim(jwt: String?, name: String): String? {
        val payload = decodePayload(jwt) ?: return null
        val needle = "\"$name\""
        val keyIdx = payload.indexOf(needle).takeIf { it >= 0 } ?: return null
        val colon = payload.indexOf(':', keyIdx)
        val firstQuote = payload.indexOf('"', colon + 1)
        val secondQuote = payload.indexOf('"', firstQuote + 1)
        if (firstQuote < 0 || secondQuote < 0) return null
        return payload.substring(firstQuote + 1, secondQuote)
    }

    private fun parseRealmRoles(jwt: String?): List<String> {
        val payload = decodePayload(jwt) ?: return emptyList()
        val realmAccessIdx = payload.indexOf("\"realm_access\"").takeIf { it >= 0 }
            ?: return emptyList()
        val rolesIdx = payload.indexOf("\"roles\"", realmAccessIdx).takeIf { it >= 0 }
            ?: return emptyList()
        val openBracket = payload.indexOf('[', rolesIdx).takeIf { it >= 0 } ?: return emptyList()
        val closeBracket = payload.indexOf(']', openBracket).takeIf { it >= 0 } ?: return emptyList()
        val inner = payload.substring(openBracket + 1, closeBracket)
        return Regex("\"([^\"]+)\"").findAll(inner).map { it.groupValues[1] }.toList()
    }

    private fun decodePayload(jwt: String?): String? {
        if (jwt.isNullOrBlank()) return null
        val parts = jwt.split('.')
        if (parts.size < 2) return null
        return try {
            val bytes = Base64.decode(
                parts[1],
                Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
            )
            String(bytes, Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }
}
