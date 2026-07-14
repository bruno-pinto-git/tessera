package com.tessera.ticket.event

import com.tessera.ticket.ticket.BoxOfficeAlreadyExistsException
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EventServiceTest {

    private val repo: EventRepository = mock()
    private val service = EventService(repo)

    @Test
    fun `create rejects an invalid status`() {
        assertFailsWith<IllegalArgumentException> {
            service.create(request(status = "BOGUS"))
        }
    }

    @Test
    fun `create defaults to PUBLISHED when no status is given`() {
        val captor = argumentCaptor<Event>()
        doReturn(Event(id = 1L)).whenever(repo).save(captor.capture())

        service.create(request(status = null))

        assertEquals("PUBLISHED", captor.firstValue.status)
    }

    @Test
    fun `create uppercases a valid status`() {
        val captor = argumentCaptor<Event>()
        doReturn(Event(id = 1L)).whenever(repo).save(captor.capture())

        service.create(request(status = "draft"))

        assertEquals("DRAFT", captor.firstValue.status)
    }

    @Test
    fun `create rejects a second box office for the same match`() {
        whenever(repo.existsByMatchIdAndStatusNot(99L, "CANCELLED")).doReturn(true)

        assertFailsWith<BoxOfficeAlreadyExistsException> {
            service.create(request(status = null))
        }
    }

    @Test
    fun `create snapshots the resolved home club on the event`() {
        val captor = argumentCaptor<Event>()
        doReturn(Event(id = 1L)).whenever(repo).save(captor.capture())

        service.create(request(status = null), homeClubId = 7L)

        assertEquals(7L, captor.firstValue.homeClubId)
    }

    private fun request(status: String?) = CreateEventRequest(
        name = "Demo box office",
        matchId = 99L,
        priceNormal = BigDecimal("10.00"),
        priceSupporter = BigDecimal("5.00"),
        status = status,
    )
}
