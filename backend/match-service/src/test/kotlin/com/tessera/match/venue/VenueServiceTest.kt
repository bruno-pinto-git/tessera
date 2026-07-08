package com.tessera.match.venue

import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import java.time.OffsetDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class VenueServiceTest {

    private val repo: VenueRepository = mock()
    private val service = VenueService(repo)

    @Test
    fun `create rejects a duplicate name`() {
        whenever(repo.existsActiveByNameIgnoreCase("Alvalade")).thenReturn(true)
        assertFailsWith<VenueNameConflictException> {
            service.create(VenueCreateRequest(name = "Alvalade", capacity = 50000))
        }
        verify(repo, never()).save(any())
    }

    @Test
    fun `create persists a venue with a free name`() {
        whenever(repo.existsActiveByNameIgnoreCase("Alvalade")).thenReturn(false)
        doReturn(venue(1L, "Alvalade")).whenever(repo).save(any())

        val created = service.create(VenueCreateRequest(name = "  Alvalade  ", capacity = 50000))

        assertEquals("Alvalade", created.name)
    }

    @Test
    fun `update fails for an unknown venue`() {
        whenever(repo.findActiveById(404L)).thenReturn(null)
        assertFailsWith<VenueNotFoundException> { service.update(404L, VenueUpdateRequest(name = "X")) }
    }

    @Test
    fun `update rejects renaming to an existing venue name`() {
        whenever(repo.findActiveById(1L)).thenReturn(venue(1L, "Alvalade"))
        whenever(repo.existsActiveByNameIgnoreCaseExcluding("Luz", 1L)).thenReturn(true)
        assertFailsWith<VenueNameConflictException> {
            service.update(1L, VenueUpdateRequest(name = "Luz"))
        }
    }

    @Test
    fun `update keeping the same name (case-insensitive) skips the conflict check`() {
        whenever(repo.findActiveById(1L)).thenReturn(venue(1L, "Alvalade"))

        val updated = service.update(1L, VenueUpdateRequest(name = "alvalade", capacity = 52000))

        assertEquals("alvalade", updated.name)
        assertEquals(52000, updated.capacity)
        verify(repo, never()).existsActiveByNameIgnoreCaseExcluding(any(), any())
    }

    @Test
    fun `delete soft-deletes the venue`() {
        val venue = venue(1L, "Alvalade")
        whenever(repo.findActiveById(1L)).thenReturn(venue)
        service.delete(1L)
        assert(venue.deletedAt != null)
    }

    @Test
    fun `get fails for an unknown venue`() {
        whenever(repo.findActiveById(404L)).thenReturn(null)
        assertFailsWith<VenueNotFoundException> { service.get(404L) }
    }

    @Test
    fun `list without a name filter returns all active venues`() {
        val page: Page<Venue> = PageImpl(listOf(venue(1L, "Alvalade")))
        whenever(repo.findAllActive(Pageable.unpaged())).thenReturn(page)
        assertEquals(1, service.list(null, Pageable.unpaged()).totalElements.toInt())
    }

    @Test
    fun `list with a name filter searches by name`() {
        val page: Page<Venue> = PageImpl(listOf(venue(1L, "Alvalade")))
        whenever(repo.findActiveByNameLike("Alva", Pageable.unpaged())).thenReturn(page)
        assertEquals(1, service.list("Alva", Pageable.unpaged()).totalElements.toInt())
    }

    private fun venue(id: Long, name: String) =
        Venue(id = id, name = name, capacity = 50000, createdAt = OffsetDateTime.now())
}
