package com.example.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * JPA entity representing a product in the database.
 *
 * KEY CONCEPTS:
 *
 * @Entity — marks this class as a JPA entity. Hibernate maps it to a database
 * table. The class must have a no-arg constructor (Hibernate creates instances
 * via reflection), and fields are mapped to columns by default.
 *
 * @Table — optional; specifies the table name. Without it, Hibernate uses the
 * class name. We include it for explicitness.
 *
 * @Id + @GeneratedValue — the primary key. IDENTITY strategy means the database
 * generates the ID on INSERT (AUTO_INCREMENT in H2/MySQL, SERIAL in PostgreSQL).
 * The entity has no ID until after persist/flush.
 *
 * @Column — optional for simple mappings (Hibernate maps fields by name using
 * SpringPhysicalNamingStrategy: camelCase → snake_case). We use it where we
 * need to specify constraints (nullable, length, precision) that affect DDL
 * generation and serve as documentation.
 *
 * WHY BigDecimal for price: floating-point types (double, float) lose precision
 * in arithmetic (0.1 + 0.2 ≠ 0.3). BigDecimal is exact, making it mandatory
 * for monetary values.
 *
 * WHY Instant for timestamps: Instant represents an absolute point in time (UTC).
 * LocalDateTime has no timezone and is ambiguous. APIs should exchange UTC
 * timestamps; clients convert to local time for display.
 *
 * THIS CLASS IS NOT EXPOSED TO API CONSUMERS. DTOs handle that.
 */
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Column(length = 100)
    private String category;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity = 0;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // ======================================================================
    // Constructors
    // ======================================================================

    /**
     * No-arg constructor required by JPA. Hibernate uses reflection to
     * instantiate entities, so this must exist. We make it public because
     * the mapper (in a different package) also needs to create instances.
     * JPA requires at least protected; public is fine and avoids access issues.
     */
    public Product() {
    }

    // ======================================================================
    // Getters and Setters
    // ======================================================================
    // JPA entities need getters/setters for field access by Hibernate and
    // by our mapper layer. We follow JavaBean conventions.

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
