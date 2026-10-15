package com.example.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * DTO for full product replacement (PUT request body).
 *
 * PUT semantics: the body is the COMPLETE new representation of all editable
 * fields. Omitted fields are treated as "set to default/null."
 *
 * This has the SAME validation rules as ProductCreateRequest because a PUT
 * is essentially "re-create at this URI." All required fields must be present.
 *
 * WHY A SEPARATE CLASS from ProductCreateRequest?
 * Right now they're identical. But they represent different operations with
 * different future evolution paths:
 * - Create might require fields that update doesn't (e.g., initial category).
 * - Update might allow changing fields that create doesn't (e.g., active status).
 * - Different operations may have different authorization rules.
 * Separate classes give you that freedom without refactoring.
 */
public class ProductUpdateRequest {

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

    // Getters and Setters

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
