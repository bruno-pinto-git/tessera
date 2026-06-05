package com.tessera.android.data

object KeycloakConfig {
    const val CLIENT_ID = "tessera-android"
    const val REDIRECT_URI = "tessera://callback"
    val SCOPES = listOf("openid", "profile", "email", "roles")
}
