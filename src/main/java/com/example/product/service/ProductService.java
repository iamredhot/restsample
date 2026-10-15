package com.example.product.service;

import com.example.product.dto.PageResponse;
import com.example.product.dto.ProductCreateRequest;
import com.example.product.dto.ProductPatchRequest;
import com.example.product.dto.ProductResponse;
import com.example.product.dto.ProductUpdateRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

/**
 * Service interface for product operations.
 *
 * WHY AN INTERFACE?
 * 1. Testability: in unit tests, you can mock this interface without loading
 *    the full implementation or Spring context.
 * 2. Abstraction: the controller depends on the contract, not the implementation.
 *    You could swap implementations (e.g., caching decorator) without touching
 *    the controller.
 * 3. Documentation: the interface clearly declares the service's capabilities
 *    and method signatures.
 *
 * TRADE-OFF: for a small project with one implementation, an interface adds
 * a layer of indirection. Some teams skip it and test/mock the concrete class
 * directly. Both approaches are valid. We use the interface because:
 * (a) it's the conventional Spring pattern you'll see in most codebases,
 * (b) it makes our mocking examples cleaner in Phase 6.
 */
public interface ProductService {

    ProductResponse createProduct(ProductCreateRequest request);

    ProductResponse getProduct(Long id);

    PageResponse<ProductResponse> listProducts(Pageable pageable);

    ProductResponse updateProduct(Long id, ProductUpdateRequest request);

    ProductResponse patchProduct(Long id, ProductPatchRequest request);

    void deleteProduct(Long id);

    PageResponse<ProductResponse> searchProducts(String name, String category,
                                                  BigDecimal minPrice, BigDecimal maxPrice,
                                                  Boolean active, Pageable pageable);
}
