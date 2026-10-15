package com.example.product.dto;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/**
 * DTO for partial product update (PATCH request body) using JSON Merge Patch
 * semantics (RFC 7396).
 *
 * THE CORE CHALLENGE:
 * Jackson deserializes a missing JSON field and an explicit null the same way:
 * both leave the Java field as null. But for PATCH we need to distinguish:
 *   - Field ABSENT in JSON → don't touch it
 *   - Field present with VALUE → set it
 *   - Field present with NULL → clear it
 *
 * THE SOLUTION: setter-based tracking.
 * Each setter records that its field was explicitly provided in the JSON.
 * When Jackson deserializes {"price": 59.99}, only setPrice() is called,
 * so only "price" appears in fieldsPresent. The service layer checks
 * isFieldPresent("name") → false → leave name unchanged.
 *
 * NO VALIDATION ANNOTATIONS HERE.
 * Unlike Create/Update DTOs, this DTO has no @NotBlank/@Positive because
 * every field is optional (it might not be in the patch). Validation happens
 * in the service layer AFTER merging the patch onto the existing entity:
 * if the merged result has a blank name or negative price, that's rejected.
 */
public class ProductPatchRequest {

    private String name;
    private String description;
    private BigDecimal price;
    private String category;
    private Integer stockQuantity;
    private Boolean active;

    /**
     * Tracks which fields were explicitly present in the JSON body.
     * Jackson calls setters only for fields present in the JSON, so
     * after deserialization this set contains exactly the fields the
     * client intended to change.
     */
    private final Set<String> fieldsPresent = new HashSet<>();

    /**
     * Check if a field was explicitly provided in the JSON payload.
     * Used by the service layer to distinguish "absent" from "set to null."
     */
    public boolean isFieldPresent(String fieldName) {
        return fieldsPresent.contains(fieldName);
    }

    // ======================================================================
    // Getters and Setters — each setter records the field as present
    // ======================================================================

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        fieldsPresent.add("name");
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        fieldsPresent.add("description");
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
        fieldsPresent.add("price");
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
        fieldsPresent.add("category");
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
        fieldsPresent.add("stockQuantity");
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
        fieldsPresent.add("active");
    }
}
