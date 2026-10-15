package com.example.product.dto;

import java.util.List;

/**
 * Generic pagination response DTO.
 *
 * WHY NOT USE Spring's Page<T> DIRECTLY?
 * Spring's Page serializes to JSON with deeply nested, framework-specific
 * fields (pageable.sort.sorted, pageable.offset, numberOfElements, etc.)
 * that:
 *   1. Change between Spring Boot versions (breaking clients)
 *   2. Expose Spring internals as your API contract
 *   3. Are confusing for non-Java consumers
 *
 * This DTO gives us a clean, stable, documented pagination contract that
 * we fully control. The mapper converts Spring's Page to this shape.
 *
 * GENERIC TYPE <T>: the content type. PageResponse<ProductResponse> for
 * products, PageResponse<OrderResponse> for orders, etc. Reusable across
 * all paginated endpoints.
 *
 * @param <T> the type of items in the page
 */
public class PageResponse<T> {

    /** The items on this page. */
    private List<T> content;

    /** Current page number (0-based). */
    private int page;

    /** Requested page size. */
    private int size;

    /** Total number of items across all pages. */
    private long totalElements;

    /** Total number of pages. */
    private int totalPages;

    /** Whether this is the first page. */
    private boolean first;

    /** Whether this is the last page. */
    private boolean last;

    // ======================================================================
    // Constructors
    // ======================================================================

    public PageResponse() {
    }

    public PageResponse(List<T> content, int page, int size,
                        long totalElements, int totalPages,
                        boolean first, boolean last) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.first = first;
        this.last = last;
    }

    // ======================================================================
    // Getters and Setters
    // ======================================================================

    public List<T> getContent() {
        return content;
    }

    public void setContent(List<T> content) {
        this.content = content;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public boolean isFirst() {
        return first;
    }

    public void setFirst(boolean first) {
        this.first = first;
    }

    public boolean isLast() {
        return last;
    }

    public void setLast(boolean last) {
        this.last = last;
    }
}
