package com.marketplace.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Generic page wrapper used by all list endpoints across services.
 *
 * <p>Provides pagination metadata alongside the page content. Compatible with
 * Spring Data's {@code Page<T>} but kept framework-agnostic so that
 * {@code common} stays a plain java-library.</p>
 *
 * <p><b>IMMUTABLE contract</b> — shape is shared across all REST APIs.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious
) {

    /**
     * Convenience factory that derives pagination flags from totals.
     */
    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        boolean hasNext = page + 1 < totalPages;
        boolean hasPrevious = page > 0;
        return new PageResponse<>(content, page, size, totalElements, totalPages, hasNext, hasPrevious);
    }
}