package com.tessera.match

import com.tessera.match.iam.KeycloakAdminProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(KeycloakAdminProperties::class)
class MatchServiceApplication

fun main(args: Array<String>) {
    runApplication<MatchServiceApplication>(*args)
}
