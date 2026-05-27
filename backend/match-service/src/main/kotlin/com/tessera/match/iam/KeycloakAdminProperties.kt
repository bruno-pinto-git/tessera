package com.tessera.match.iam

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Properties for the Keycloak Admin API client. Configured via the
 * `tessera.keycloak.admin.*` keys (see application.yml). Authentication is
 * via the `tessera-bff` confidential client's service account, which has
 * `realm-management` roles granted in the realm export.
 */
@ConfigurationProperties("tessera.keycloak.admin")
data class KeycloakAdminProperties(
    /** Base URL of the Keycloak server, e.g. `http://keycloak:8180`. */
    val baseUrl: String = "http://keycloak:8180",
    /** Realm where Tessera lives. */
    val realm: String = "tessera",
    /** Confidential client used to acquire the admin token (client_credentials). */
    val clientId: String = "tessera-bff",
    /** Client secret. Override via env in production. */
    val clientSecret: String = "change-me-in-production",
)