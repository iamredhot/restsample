package com.example.product.service;

import com.example.product.dto.PageResponse;
import com.example.product.dto.ProductCreateRequest;
import com.example.product.dto.ProductPatchRequest;
import com.example.product.dto.ProductResponse;
import com.example.product.dto.ProductUpdateRequest;
import com.example.product.entity.Product;
import com.example.product.exception.ResourceNotFoundException;
import com.example.product.mapper.ProductMapper;
import com.example.product.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Implementation of ProductService.
 *
 * KEY CONCEPTS:
 *
 * @Service — a specialization of @Component. Marks this class for Spring's
 * component scan and communicates its role as a service-layer bean.
 *
 * @Transactional — at the class level, wraps every public method in a
 * database transaction. The transaction:
 *   - Starts when the method is called (via Spring's proxy)
 *   - Commits when the method returns normally
 *   - Rolls back when an unchecked exception propagates out
 *
 * readOnly=true on the class level is an optimization hint for read methods.
 * Write methods override it with @Transactional (without readOnly).
 *
 * WHY @Transactional HERE (service layer) and NOT on the controller or repository?
 * - Controller: too early — the transaction would span HTTP serialization,
 *   keeping the DB connection open longer than needed.
 * - Repository: too granular — a service method that calls multiple repository
 *   methods needs them in ONE transaction (atomicity).
 * - Service: the right boundary — it represents a business operation (unit of work).
 *
 * DEPENDENCY INJECTION: Spring injects ProductRepository via the constructor.
 * Constructor injection is preferred over field injection (@Autowired on fields)
 * because:
 *   1. Dependencies are explicit and final (immutable after construction).
 *   2. The class can be instantiated in tests without Spring.
 *   3. Circular dependencies fail fast at startup, not at runtime.
 */
@Service
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductServiceImpl.class);

    private final ProductRepository productRepository;

    /**
     * Constructor injection. Spring auto-detects this as the injection point
     * when there's exactly one constructor (no @Autowired needed since Spring 4.3).
     */
    public ProductServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * Create a new product.
     *
     * @Transactional overrides the class-level readOnly=true because this
     * method writes to the database.
     *
     * Flow: DTO → entity (via mapper) → set timestamps → save → entity → DTO.
     */
    @Override
    @Transactional
    public ProductResponse createProduct(ProductCreateRequest request) {
        log.debug("Creating product: {}", request.getName());

        Product product = ProductMapper.toEntity(request);
        Instant now = Instant.now();
        product.setCreatedAt(now);
        product.setUpdatedAt(now);

        Product saved = productRepository.save(product);
        log.info("Created product with id: {}", saved.getId());

        return ProductMapper.toResponse(saved);
    }

    /**
     * Get a single product by ID.
     *
     * findById returns Optional<Product>. We use orElseThrow to convert
     * an empty Optional into our ResourceNotFoundException, which the
     * GlobalExceptionHandler maps to 404.
     */
    @Override
    public ProductResponse getProduct(Long id) {
        log.debug("Fetching product with id: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));

        return ProductMapper.toResponse(product);
    }

    /**
     * List all products with pagination.
     *
     * Spring Data's findAll(Pageable) returns a Page<Product> with the
     * requested slice plus metadata (total count, total pages, etc.).
     * We convert it to our stable PageResponse via the mapper.
     */
    @Override
    public PageResponse<ProductResponse> listProducts(Pageable pageable) {
        log.debug("Listing products - page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());

        Page<Product> page = productRepository.findAll(pageable);
        return ProductMapper.toPageResponse(page);
    }

    /**
     * Full update (PUT semantics): replace all editable fields.
     *
     * 1. Find existing entity (or 404).
     * 2. Replace all fields from the request.
     * 3. Update the timestamp.
     * 4. Save (Hibernate detects changes via dirty checking).
     */
    @Override
    @Transactional
    public ProductResponse updateProduct(Long id, ProductUpdateRequest request) {
        log.debug("Updating product with id: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));

        ProductMapper.updateEntity(product, request);
        product.setUpdatedAt(Instant.now());

        // save() is technically optional here because the entity is managed
        // (attached to the persistence context), so Hibernate's dirty checking
        // will flush changes automatically at transaction commit. We call save()
        // explicitly for clarity — it makes the write intent obvious in the code.
        Product saved = productRepository.save(product);
        return ProductMapper.toResponse(saved);
    }

    /**
     * Partial update (PATCH with JSON Merge Patch semantics).
     *
     * This is the most nuanced operation:
     * 1. Find existing entity (or 404).
     * 2. For each field the client sent (fieldsPresent), apply it to the entity.
     * 3. Validate the MERGED result (not just the patch, the final state).
     * 4. Save.
     *
     * The isFieldPresent() check is what makes this a proper merge patch:
     * - Field absent → entity field untouched
     * - Field present with value → entity field updated
     * - Field present with null → entity field cleared
     */
    @Override
    @Transactional
    public ProductResponse patchProduct(Long id, ProductPatchRequest request) {
        log.debug("Patching product with id: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));

        // Apply only the fields that were present in the JSON
        if (request.isFieldPresent("name")) {
            product.setName(request.getName());
        }
        if (request.isFieldPresent("description")) {
            product.setDescription(request.getDescription());
        }
        if (request.isFieldPresent("price")) {
            product.setPrice(request.getPrice());
        }
        if (request.isFieldPresent("category")) {
            product.setCategory(request.getCategory());
        }
        if (request.isFieldPresent("stockQuantity")) {
            product.setStockQuantity(request.getStockQuantity());
        }
        if (request.isFieldPresent("active")) {
            product.setActive(request.getActive());
        }

        // Validate the merged result
        validateProduct(product);

        product.setUpdatedAt(Instant.now());
        Product saved = productRepository.save(product);
        return ProductMapper.toResponse(saved);
    }

    /**
     * Delete a product by ID.
     *
     * Returns void → controller returns 204 No Content.
     * Throws ResourceNotFoundException if not found → 404.
     *
     * We check existence first so we can return 404 for non-existent products
     * (instead of silently succeeding, which some APIs prefer for idempotency).
     */
    @Override
    @Transactional
    public void deleteProduct(Long id) {
        log.debug("Deleting product with id: {}", id);

        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product", id);
        }

        productRepository.deleteById(id);
        log.info("Deleted product with id: {}", id);
    }

    /**
     * Search/filter products with optional criteria.
     *
     * All parameters are optional (null = don't filter on that field).
     * The repository's @Query handles the null-check logic.
     */
    @Override
    public PageResponse<ProductResponse> searchProducts(
            String name, String category,
            BigDecimal minPrice, BigDecimal maxPrice,
            Boolean active, Pageable pageable) {

        log.debug("Searching products - name: {}, category: {}, price: {}-{}, active: {}",
                name, category, minPrice, maxPrice, active);

        Page<Product> page = productRepository.searchProducts(
                name, category, minPrice, maxPrice, active, pageable);
        return ProductMapper.toPageResponse(page);
    }

    // ======================================================================
    // Private helpers
    // ======================================================================

    /**
     * Validate a product entity after a PATCH merge.
     *
     * We can't use @Valid on the PatchRequest DTO because all fields are
     * optional there. Instead, we validate the MERGED entity state.
     *
     * This is manual validation — an alternative would be using the
     * Validator interface programmatically. For a small number of rules,
     * explicit checks are clearer.
     */
    private void validateProduct(Product product) {
        if (product.getName() == null || product.getName().isBlank()) {
            throw new IllegalArgumentException("Product name must not be blank");
        }
        if (product.getName().length() > 255) {
            throw new IllegalArgumentException("Product name must not exceed 255 characters");
        }
        if (product.getPrice() == null || product.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price must be greater than zero");
        }
        if (product.getStockQuantity() != null && product.getStockQuantity() < 0) {
            throw new IllegalArgumentException("Stock quantity must not be negative");
        }
    }
}
