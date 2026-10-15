package com.example.product.repository;

import com.example.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

/**
 * Spring Data JPA repository for Product entities.
 *
 * KEY CONCEPTS:
 *
 * JpaRepository<Product, Long> — generic interface that provides CRUD operations,
 * pagination, and sorting out of the box. The type parameters are:
 *   - Product: the entity type
 *   - Long: the primary key type
 *
 * You get these methods for FREE (no implementation needed):
 *   - save(entity), saveAll(entities)
 *   - findById(id), findAll(), findAll(Pageable)
 *   - existsById(id), count()
 *   - deleteById(id), delete(entity), deleteAll()
 *
 * Spring Data generates the implementation class at runtime using a JDK proxy.
 * You never write SQL for basic CRUD.
 *
 * DERIVED QUERY METHODS: Spring Data parses method names and generates JPQL.
 * "findByNameContainingIgnoreCase" becomes:
 *   SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))
 *
 * For complex queries, use @Query with JPQL or native SQL.
 *
 * @Repository is technically optional (JpaRepository is auto-detected), but
 * we include it for clarity and to enable Spring's exception translation
 * (converts low-level JDBC/JPA exceptions to Spring's DataAccessException hierarchy).
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Search products with multiple optional filters.
     *
     * This JPQL query uses conditional logic: each filter is applied only if
     * the parameter is non-null. This is a common pattern for dynamic search
     * queries without using Specification or Criteria API.
     *
     * HOW IT WORKS:
     * - (:name IS NULL OR ...) — if name param is null, the condition is TRUE
     *   (effectively skipped). If non-null, it filters.
     * - LOWER() on both sides makes the search case-insensitive.
     * - % wildcards make it a "contains" search for name.
     *
     * The Pageable parameter adds pagination and sorting automatically.
     *
     * TRADE-OFF: This approach works well for a moderate number of filters.
     * For truly dynamic, complex queries, Spring Data Specifications
     * (JPA Criteria API wrappers) are more maintainable. We use this simpler
     * approach for learning clarity.
     */
    @Query("SELECT p FROM Product p WHERE " +
           "(:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
           "(:category IS NULL OR LOWER(p.category) = LOWER(:category)) AND " +
           "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
           "(:active IS NULL OR p.active = :active)")
    Page<Product> searchProducts(
            @Param("name") String name,
            @Param("category") String category,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("active") Boolean active,
            Pageable pageable);
}
