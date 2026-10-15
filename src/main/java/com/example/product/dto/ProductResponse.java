package com.example.product.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO for product API responses.
 *
 * This is the shape every API consumer sees. It includes:
 * - All public product fields (id, name, description, price, etc.)
 * - Server-generated fields (id, createdAt, updatedAt)
 *
 * It does NOT include:
 * - Internal/sensitive fields (if we had any)
 * - JPA-specific concerns (version, proxy types, lazy refs)
 *
 * Jackson serializes this to JSON via getters. The @JsonInclude(NON_NULL)
 * setting in application.yml means null fields are omitted from the JSON,
 * keeping responses clean.
 */
public class ProductResponse {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private String category;
    private Integer stockQuantity;
    private Boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    // ======================================================================
    // Constructors
    // ======================================================================

    public ProductResponse() {
    }

    // ======================================================================
    // Getters and Setters
    // ======================================================================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
