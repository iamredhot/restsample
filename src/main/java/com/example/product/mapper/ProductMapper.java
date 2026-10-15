package com.example.product.mapper;

import com.example.product.dto.PageResponse;
import com.example.product.dto.ProductCreateRequest;
import com.example.product.dto.ProductResponse;
import com.example.product.dto.ProductUpdateRequest;
import com.example.product.entity.Product;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Converts between Product entities and DTOs.
 *
 * WHY A SEPARATE MAPPER?
 * - Single Responsibility: conversion logic lives in one place, not scattered
 *   across controllers and services.
 * - Testability: pure functions, easy to unit test.
 * - Visibility: every field mapping is explicit — no "magic" that hides what's
 *   being copied where.
 *
 * WHY STATIC METHODS (not a Spring @Component)?
 * These are pure functions with no dependencies — they don't need Spring's
 * dependency injection. Static methods are simpler and communicate "I have
 * no state, no side effects, I just transform data."
 *
 * ALTERNATIVE: MapStruct (annotation processor that generates mapping code at
 * compile time). Better for large projects with many entities; overkill here
 * and hides the mappings we want to learn.
 */
public final class ProductMapper {

    // Private constructor prevents instantiation — this is a utility class.
    private ProductMapper() {
    }

    /**
     * Convert a create request DTO to a new Product entity.
     * Note: id, createdAt, updatedAt are NOT set here — they're server-assigned.
     */
    public static Product toEntity(ProductCreateRequest request) {
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setCategory(request.getCategory());
        product.setStockQuantity(
                request.getStockQuantity() != null ? request.getStockQuantity() : 0);
        product.setActive(
                request.getActive() != null ? request.getActive() : true);
        return product;
    }

    /**
     * Apply a full update (PUT) to an existing entity.
     * All editable fields are replaced. Omitted optional fields become null/default.
     */
    public static void updateEntity(Product product, ProductUpdateRequest request) {
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setCategory(request.getCategory());
        product.setStockQuantity(
                request.getStockQuantity() != null ? request.getStockQuantity() : 0);
        product.setActive(
                request.getActive() != null ? request.getActive() : true);
    }

    /**
     * Convert a Product entity to a response DTO.
     * This is the outbound shape — what API consumers see.
     */
    public static ProductResponse toResponse(Product product) {
        ProductResponse response = new ProductResponse();
        response.setId(product.getId());
        response.setName(product.getName());
        response.setDescription(product.getDescription());
        response.setPrice(product.getPrice());
        response.setCategory(product.getCategory());
        response.setStockQuantity(product.getStockQuantity());
        response.setActive(product.getActive());
        response.setCreatedAt(product.getCreatedAt());
        response.setUpdatedAt(product.getUpdatedAt());
        return response;
    }

    /**
     * Convert a Spring Data Page<Product> to our custom PageResponse<ProductResponse>.
     *
     * This is where we decouple from Spring's Page serialization format.
     * The input is Spring's rich Page object; the output is our clean,
     * stable API contract.
     */
    public static PageResponse<ProductResponse> toPageResponse(Page<Product> page) {
        List<ProductResponse> content = page.getContent().stream()
                .map(ProductMapper::toResponse)
                .toList();

        return new PageResponse<>(
                content,
                page.getNumber(),         // current page (0-based)
                page.getSize(),           // requested page size
                page.getTotalElements(),  // total items
                page.getTotalPages(),     // total pages
                page.isFirst(),           // is first page?
                page.isLast()             // is last page?
        );
    }
}
