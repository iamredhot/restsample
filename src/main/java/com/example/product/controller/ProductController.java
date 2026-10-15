package com.example.product.controller;

import com.example.product.dto.PageResponse;
import com.example.product.dto.ProductCreateRequest;
import com.example.product.dto.ProductPatchRequest;
import com.example.product.dto.ProductResponse;
import com.example.product.dto.ProductUpdateRequest;
import com.example.product.service.ProductService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.net.URI;

/**
 * REST controller for product operations.
 *
 * KEY CONCEPTS:
 *
 * @RestController = @Controller + @ResponseBody.
 * Every method's return value is serialized to JSON (via Jackson) and written
 * to the HTTP response body. No view resolution, no Thymeleaf — pure API.
 *
 * @RequestMapping("/api/v1/products") — base path for all endpoints in this
 * controller. The /api/v1 prefix provides URI-path versioning.
 *
 * ANNOTATION-TO-HTTP MAPPING:
 *   @GetMapping     → GET
 *   @PostMapping    → POST
 *   @PutMapping     → PUT
 *   @PatchMapping   → PATCH
 *   @DeleteMapping  → DELETE
 *
 * ResponseEntity<T>: wraps the response body with explicit control over:
 *   - Status code (201, 204, etc.)
 *   - Headers (Location, etc.)
 *   - Body (the DTO)
 *
 * @Valid: triggers Jakarta Bean Validation on the request body DTO.
 * If validation fails, Spring throws MethodArgumentNotValidException
 * BEFORE this method executes — our GlobalExceptionHandler returns 422.
 *
 * @PathVariable: extracts a value from the URI path ({id} → Long id).
 * @RequestParam: extracts a query parameter (?page=0 → int page).
 * @RequestBody: deserializes the HTTP request body JSON into a Java object.
 *
 * CONTROLLER RESPONSIBILITIES (thin controller pattern):
 * 1. Parse HTTP input (path vars, query params, body)
 * 2. Delegate to the service
 * 3. Build the HTTP response (status code, headers, body)
 *
 * Business logic lives in the SERVICE, not here. The controller is a
 * translation layer between HTTP and Java method calls.
 */
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    private final ProductService productService;

    /**
     * Constructor injection — Spring injects the ProductService implementation.
     */
    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // ======================================================================
    // POST /api/v1/products — Create a product
    // ======================================================================

    /**
     * Create a new product.
     *
     * @param request the product data (validated via @Valid)
     * @return 201 Created with Location header and the created product
     *
     * WHY 201 (not 200): a new resource was created. HTTP semantics.
     * WHY Location header: tells the client the URI of the new resource.
     * WHY return the body: saves the client a follow-up GET to see
     * server-generated fields (id, createdAt, updatedAt).
     */
    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody ProductCreateRequest request) {

        log.debug("POST /api/v1/products - creating product: {}", request.getName());

        ProductResponse created = productService.createProduct(request);

        // Build the Location header URI: /api/v1/products/{id}
        URI location = URI.create("/api/v1/products/" + created.getId());

        return ResponseEntity
                .created(location)     // 201 + Location header
                .body(created);        // response body
    }

    // ======================================================================
    // GET /api/v1/products — List products (paginated)
    // ======================================================================

    /**
     * List all products with pagination and sorting.
     *
     * @param page page number (0-based), default 0
     * @param size items per page, default 20, max 100
     * @param sort sort field and direction, e.g. "name,asc"
     * @return 200 OK with paginated product list
     *
     * NOTE ON PAGEABLE: Spring can auto-resolve Pageable from query params,
     * but we build it manually here for explicit control over defaults
     * and max page size (defensive programming against clients requesting
     * size=999999).
     */
    @GetMapping
    public ResponseEntity<PageResponse<ProductResponse>> listProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,asc") String sort) {

        // Cap page size to prevent abusive requests
        size = Math.min(size, 100);

        Pageable pageable = buildPageable(page, size, sort);
        PageResponse<ProductResponse> response = productService.listProducts(pageable);

        return ResponseEntity.ok(response);
    }

    // ======================================================================
    // GET /api/v1/products/{id} — Get one product
    // ======================================================================

    /**
     * Retrieve a single product by ID.
     *
     * @param id product ID from the URI path
     * @return 200 OK with the product, or 404 if not found
     *
     * @PathVariable extracts {id} from the URI. Spring converts the
     * String "42" to Long 42 automatically. If the path contains a
     * non-numeric value, Spring returns 400 Bad Request.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable Long id) {
        log.debug("GET /api/v1/products/{}", id);

        ProductResponse response = productService.getProduct(id);
        return ResponseEntity.ok(response);
    }

    // ======================================================================
    // PUT /api/v1/products/{id} — Full update
    // ======================================================================

    /**
     * Replace a product's editable fields entirely (PUT semantics).
     *
     * @return 200 OK with the updated product, or 404/422 on error
     *
     * PUT = full replacement. All editable fields must be provided.
     * Omitted optional fields get default values.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductUpdateRequest request) {

        log.debug("PUT /api/v1/products/{}", id);

        ProductResponse updated = productService.updateProduct(id, request);
        return ResponseEntity.ok(updated);
    }

    // ======================================================================
    // PATCH /api/v1/products/{id} — Partial update
    // ======================================================================

    /**
     * Partially update a product (JSON Merge Patch semantics).
     *
     * @return 200 OK with the updated product, or 404/400 on error
     *
     * NOTE: no @Valid here. The PatchRequest DTO has no validation
     * annotations because all fields are optional. Validation happens
     * in the service layer after merging the patch onto the existing entity.
     */
    @PatchMapping("/{id}")
    public ResponseEntity<ProductResponse> patchProduct(
            @PathVariable Long id,
            @RequestBody ProductPatchRequest request) {

        log.debug("PATCH /api/v1/products/{}", id);

        ProductResponse patched = productService.patchProduct(id, request);
        return ResponseEntity.ok(patched);
    }

    // ======================================================================
    // DELETE /api/v1/products/{id} — Delete a product
    // ======================================================================

    /**
     * Delete a product by ID.
     *
     * @return 204 No Content on success, 404 if not found
     *
     * WHY 204 (not 200): success with no body. The resource is gone;
     * there's nothing meaningful to return.
     *
     * ResponseEntity.noContent().build() = status 204, no body.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        log.debug("DELETE /api/v1/products/{}", id);

        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    // ======================================================================
    // GET /api/v1/products/search — Search/filter products
    // ======================================================================

    /**
     * Search products with optional filters.
     *
     * All query parameters are optional. If none are provided, this
     * behaves like the list endpoint (returns all products, paginated).
     *
     * @param name partial name match (case-insensitive contains)
     * @param category exact match (case-insensitive)
     * @param minPrice minimum price (inclusive)
     * @param maxPrice maximum price (inclusive)
     * @param active filter by active status
     * @return 200 OK with paginated search results
     */
    @GetMapping("/search")
    public ResponseEntity<PageResponse<ProductResponse>> searchProducts(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,asc") String sort) {

        size = Math.min(size, 100);
        Pageable pageable = buildPageable(page, size, sort);

        PageResponse<ProductResponse> response = productService.searchProducts(
                name, category, minPrice, maxPrice, active, pageable);

        return ResponseEntity.ok(response);
    }

    // ======================================================================
    // Private helpers
    // ======================================================================

    /**
     * Build a Pageable from query parameters.
     *
     * Parses sort strings like "name,asc" or "price,desc" into Spring's
     * Sort object. Defaults to ascending if direction is missing.
     */
    private Pageable buildPageable(int page, int size, String sort) {
        String[] sortParts = sort.split(",");
        String sortField = sortParts[0].trim();
        Sort.Direction direction = sortParts.length > 1
                ? Sort.Direction.fromOptionalString(sortParts[1].trim())
                        .orElse(Sort.Direction.ASC)
                : Sort.Direction.ASC;

        return PageRequest.of(page, size, Sort.by(direction, sortField));
    }
}
