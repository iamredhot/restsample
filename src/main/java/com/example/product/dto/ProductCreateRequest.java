package com.example.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * DTO for creating a new product (POST request body).
 *
 * KEY CONCEPTS:
 *
 * This class is the API CONTRACT for product creation. It defines:
 * 1. What fields the client CAN send.
 * 2. What fields are REQUIRED (via validation annotations).
 * 3. What values are VALID (via constraint annotations).
 *
 * Jakarta Bean Validation annotations are checked when the controller
 * parameter is annotated with @Valid. If validation fails, Spring throws
 * MethodArgumentNotValidException BEFORE the controller method executes.
 *
 * ANNOTATION MEANINGS:
 * - @NotBlank: not null AND not empty AND not whitespace-only (strings).
 *   Stricter than @NotNull (which allows "") and @NotEmpty (which allows " ").
 * - @NotNull: the value must be present (but can be zero, empty string, etc.).
 * - @Positive: must be > 0 (not zero). For prices: $0.00 isn't a valid price.
 * - @PositiveOrZero: must be >= 0. For stock: zero stock is valid.
 * - @Size: string length or collection size bounds.
 *
 * NOTICE: no id, createdAt, updatedAt — those are server-assigned.
 * The client should not control them.
 */
public class ProductCreateRequest {

    @NotBlank(message = "Product name is required")
    @Size(max = 255, message = "Product name must not exceed 255 characters")
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be greater than zero")
    private BigDecimal price;

    @Size(max = 100, message = "Category must not exceed 100 characters")
    private String category;

    @PositiveOrZero(message = "Stock quantity must not be negative")
    private Integer stockQuantity;

    private Boolean active;

    // ======================================================================
    // Getters and Setters
    // ======================================================================
    // Jackson uses these for deserialization (JSON → Java object).

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
}
