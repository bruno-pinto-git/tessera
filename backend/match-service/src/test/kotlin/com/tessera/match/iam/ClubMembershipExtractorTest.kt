package com.tessera.match.iam

import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.Jwt
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Pure unit test for the parser that turns a Keycloak `groups` claim into
 * `ClubMembership` records. No Spring context, no mocks — just JWT
 * fixtures and the extractor under test.
 */
class ClubMembershipExtractorTest {

    private val extractor = ClubMembershipExtractor()

    @Test
    fun `no groups claim yields an empty set`() {
        val jwt = jwtWith(null)
        assertTrue(extractor.extract(jwt).isEmpty())
    }

    @Test
    fun `empty groups claim yields an empty set`() {
        val jwt = jwtWith(emptyList())
        assertTrue(extractor.extract(jwt).isEmpty())
    }

    @Test
    fun `single manager path is parsed`() {
        val jwt = jwtWith(listOf("/clubs/1/managers"))
        assertEquals(setOf(ClubMembership(1, ClubRole.MANAGER)), extractor.extract(jwt))
    }

    @Test
    fun `staff path is parsed`() {
        val jwt = jwtWith(listOf("/clubs/42/staff"))
        assertEquals(setOf(ClubMembership(42, ClubRole.STAFF)), extractor.extract(jwt))
    }

    @Test
    fun `mixed paths give multiple memberships`() {
        val jwt = jwtWith(
            listOf(
                "/clubs/1/managers",
                "/clubs/2/staff",
                "/clubs/3/managers",
            )
        )
        assertEquals(
            setOf(
                ClubMembership(1, ClubRole.MANAGER),
                ClubMembership(2, ClubRole.STAFF),
                ClubMembership(3, ClubRole.MANAGER),
            ),
            extractor.extract(jwt),
        )
    }

    @Test
    fun `unrelated group paths are silently ignored`() {
        val jwt = jwtWith(
            listOf(
                "/platform-admins",
                "/clubs",                // missing id + role
                "/clubs/abc/managers",   // non-numeric id
                "/clubs/1",              // missing role
                "/clubs/1/owners",       // unknown subgroup
                "/other/1/managers",     // wrong root
                "/clubs/5/managers",     // valid — keep this one
            )
        )
        assertEquals(setOf(ClubMembership(5, ClubRole.MANAGER)), extractor.extract(jwt))
    }

    @Test
    fun `duplicate paths are deduplicated`() {
        val jwt = jwtWith(listOf("/clubs/7/managers", "/clubs/7/managers"))
        assertEquals(setOf(ClubMembership(7, ClubRole.MANAGER)), extractor.extract(jwt))
    }

    @Test
    fun `subgroup matching is case-insensitive on the role name`() {
        val jwt = jwtWith(listOf("/clubs/9/MANAGERS", "/clubs/10/Staff"))
        assertEquals(
            setOf(
                ClubMembership(9, ClubRole.MANAGER),
                ClubMembership(10, ClubRole.STAFF),
            ),
            extractor.extract(jwt),
        )
    }

    // -------------------------------------------------------------------------

    private fun jwtWith(groups: List<String>?): Jwt {
        val builder = Jwt.withTokenValue("test-token")
            .header("alg", "none")
            .issuer("test")
            .subject("test-user")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(60))
        if (groups != null) builder.claim("groups", groups)
        return builder.build()
    }
}