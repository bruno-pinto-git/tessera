package com.tessera.match.iam

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("tessera.keycloak.admin")
data class KeycloakAdminProperties(
    val baseUrl: String = "http://keycloak:8180",
    val realm: String = "tessera",
    val clientId: String = "tessera-bff",
    val clientSecret: String = "change-me-in-production",
)