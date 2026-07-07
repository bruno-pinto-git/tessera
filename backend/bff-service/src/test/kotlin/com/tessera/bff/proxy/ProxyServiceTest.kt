package com.tessera.bff.proxy

import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestTemplate
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProxyServiceTest {

    private val rest = RestTemplate()
    private val server = MockRestServiceServer.bindTo(rest).build()
    private val proxy = ProxyService(rest)

    @Test
    fun `forwards an already-encoded query without double-encoding it`() {
        val query = "from=2026-06-11T00%3A00%3A00.000Z&to=2026-06-12T00%3A00%3A00.000Z"
        val expected = "http://stats:8083/api/v1/stats/sales/range?$query"

        server.expect { req ->
            val actual = req.uri.toString()
            assertEquals(expected, actual, "downstream URI must not be re-encoded")
            assertTrue(!actual.contains("%253A"), "colons must not be double-encoded")
        }.andRespond(withSuccess("""{"sold":1}""", MediaType.APPLICATION_JSON))

        val req = MockHttpServletRequest("GET", "/api/v1/stats/sales/range").apply {
            queryString = query
        }

        val resp = proxy.forward(req, "http://stats:8083", null)

        server.verify()
        assertEquals(HttpStatus.OK, resp.statusCode)
        assertEquals("""{"sold":1}""", resp.body)
    }

    @Test
    fun `forwards path and query for a plain request`() {
        val expected = "http://stats:8083/api/v1/stats/sales/by-club/7"

        server.expect { req ->
            assertEquals(expected, req.uri.toString())
        }.andRespond(withSuccess("""{"clubId":7}""", MediaType.APPLICATION_JSON))

        val req = MockHttpServletRequest("GET", "/api/v1/stats/sales/by-club/7")

        val resp = proxy.forward(req, "http://stats:8083", null)

        server.verify()
        assertEquals(HttpStatus.OK, resp.statusCode)
    }
}
