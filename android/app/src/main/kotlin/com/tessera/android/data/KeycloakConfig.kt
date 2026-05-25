package com.tessera.android.data

object KeycloakConfig {
    const val ISSUER = "http://192.168.1.61:8180/realms/tessera"
    const val CLIENT_ID = "tessera-android"
    val SCOPES = listOf("openid", "profile", "email", "roles")
}
