package com.tessera.statistics.common

import org.springframework.data.domain.Page

data class PageEnvelope<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
) {
    companion object {
        fun <S, T> of(page: Page<S>, transform: (S) -> T): PageEnvelope<T> =
            PageEnvelope(
                content = page.content.map(transform),
                page = page.number,
                size = page.size,
                totalElements = page.totalElements,
                totalPages = page.totalPages,
            )
    }
}
