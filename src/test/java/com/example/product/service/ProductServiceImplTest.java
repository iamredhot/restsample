package com.example.product.service;

import com.example.product.dto.PageResponse;
import com.example.product.dto.ProductCreateRequest;
import com.example.product.dto.ProductPatchRequest;
import com.example.product.dto.ProductResponse;
import com.example.product.dto.ProductUpdateRequest;
import com.example.product.entity.Product;
import com.example.product.exception.ResourceNotFoundException;
import com.example.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * Unit tests for ProductServiceImpl.
 *
 * KEY TESTING CONCEPTS:
 *
 * @ExtendWith(MockitoExtension.class) — integrates Mockito with JUnit 5.
 * This processes @Mock and @InjectMocks annotations without loading Spring.
 *
 * @Mock — creates a mock (fake) implementation of the interface. Method calls
 * return defaults (null, 0, empty) unless you configure them with given().
 *
 * @InjectMocks — creates the real service and injects the mocks into it
 * via constructor injection. This is our System Under Test (SUT).
 *
 * given(...).willReturn(...) — BDD-style stubbing. "Given the repository
 * returns X when called with Y, then the service should do Z."
 *
 * then(...).should() — BDD-style verification. "Then the repository should
 * have been called with these arguments."
 *
 * assertThat() — AssertJ fluent assertions. More readable and more powerful
 * than JUnit's assertEquals().
 *
 * WHY MOCK THE REPOSITORY?
 * We're testing SERVICE LOGIC, not database queries. Mocking the repository:
 * 1. Makes tests run in milliseconds (no DB startup).
 * 2. Isolates failures — if a test fails, the bug is in the service.
 * 3. Lets us control exactly what the "database" returns (including edge cases
 *    that are hard to set up with real data).
 *
 * @Nested — groups related tests. Improves readability in test reports.
 * @DisplayName — human-readable test name shown in reports.
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    // Reusable test fixtures
    private Product sampleProduct;
    private ProductCreateRequest createRequest;

    @BeforeEach
    void setUp() {
        sampleProduct = new Product();
        sampleProduct.setId(1L);
        sampleProduct.setName("Wireless Keyboard");
        sampleProduct.setDescription("Bluetooth keyboard");
        sampleProduct.setPrice(new BigDecimal("49.99"));
        sampleProduct.setCategory("Electronics");
        sampleProduct.setStockQuantity(150);
        sampleProduct.setActive(true);
        sampleProduct.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        sampleProduct.setUpdatedAt(Instant.parse("2026-01-01T00:00:00Z"));

        createRequest = new ProductCreateRequest();
        createRequest.setName("Wireless Keyboard");
        createRequest.setDescription("Bluetooth keyboard");
        createRequest.setPrice(new BigDecimal("49.99"));
        createRequest.setCategory("Electronics");
        createRequest.setStockQuantity(150);
        createRequest.setActive(true);
    }

    // ==================================================================
    // CREATE
    // ==================================================================

    @Nested
    @DisplayName("createProduct")
    class CreateProduct {

        @Test
        @DisplayName("should create product and return response with generated id")
        void shouldCreateProduct() {
            // Given: repository.save() returns the entity with a generated ID
            given(productRepository.save(any(Product.class))).willReturn(sampleProduct);

            // When: we call the service
            ProductResponse response = productService.createProduct(createRequest);

            // Then: the response has the expected fields
            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getName()).isEqualTo("Wireless Keyboard");
            assertThat(response.getPrice()).isEqualByComparingTo("49.99");
            assertThat(response.getCategory()).isEqualTo("Electronics");
            assertThat(response.getStockQuantity()).isEqualTo(150);
            assertThat(response.getActive()).isTrue();

            // And: the repository was called with an entity that has timestamps set
            ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
            then(productRepository).should().save(captor.capture());
            Product savedEntity = captor.getValue();
            assertThat(savedEntity.getCreatedAt()).isNotNull();
            assertThat(savedEntity.getUpdatedAt()).isNotNull();
            assertThat(savedEntity.getCreatedAt()).isEqualTo(savedEntity.getUpdatedAt());
        }

        @Test
        @DisplayName("should default stockQuantity to 0 when not provided")
        void shouldDefaultStockQuantity() {
            createRequest.setStockQuantity(null);
            given(productRepository.save(any(Product.class))).willReturn(sampleProduct);

            productService.createProduct(createRequest);

            ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
            then(productRepository).should().save(captor.capture());
            assertThat(captor.getValue().getStockQuantity()).isEqualTo(0);
        }

        @Test
        @DisplayName("should default active to true when not provided")
        void shouldDefaultActive() {
            createRequest.setActive(null);
            given(productRepository.save(any(Product.class))).willReturn(sampleProduct);

            productService.createProduct(createRequest);

            ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
            then(productRepository).should().save(captor.capture());
            assertThat(captor.getValue().getActive()).isTrue();
        }
    }

    // ==================================================================
    // GET BY ID
    // ==================================================================

    @Nested
    @DisplayName("getProduct")
    class GetProduct {

        @Test
        @DisplayName("should return product when found")
        void shouldReturnProductWhenFound() {
            given(productRepository.findById(1L)).willReturn(Optional.of(sampleProduct));

            ProductResponse response = productService.getProduct(1L);

            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getName()).isEqualTo("Wireless Keyboard");
            assertThat(response.getPrice()).isEqualByComparingTo("49.99");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when not found")
        void shouldThrowWhenNotFound() {
            given(productRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> productService.getProduct(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("999");
        }
    }

    // ==================================================================
    // LIST
    // ==================================================================

    @Nested
    @DisplayName("listProducts")
    class ListProducts {

        @Test
        @DisplayName("should return paginated products")
        void shouldReturnPaginatedProducts() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Product> page = new PageImpl<>(List.of(sampleProduct), pageable, 1);
            given(productRepository.findAll(pageable)).willReturn(page);

            PageResponse<ProductResponse> response = productService.listProducts(pageable);

            assertThat(response.getContent()).hasSize(1);
            assertThat(response.getPage()).isEqualTo(0);
            assertThat(response.getSize()).isEqualTo(10);
            assertThat(response.getTotalElements()).isEqualTo(1);
            assertThat(response.getTotalPages()).isEqualTo(1);
            assertThat(response.isFirst()).isTrue();
            assertThat(response.isLast()).isTrue();
        }

        @Test
        @DisplayName("should return empty page when no products")
        void shouldReturnEmptyPage() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Product> emptyPage = new PageImpl<>(List.of(), pageable, 0);
            given(productRepository.findAll(pageable)).willReturn(emptyPage);

            PageResponse<ProductResponse> response = productService.listProducts(pageable);

            assertThat(response.getContent()).isEmpty();
            assertThat(response.getTotalElements()).isEqualTo(0);
        }
    }

    // ==================================================================
    // UPDATE (PUT)
    // ==================================================================

    @Nested
    @DisplayName("updateProduct")
    class UpdateProduct {

        @Test
        @DisplayName("should replace all fields and update timestamp")
        void shouldReplaceAllFields() {
            given(productRepository.findById(1L)).willReturn(Optional.of(sampleProduct));
            given(productRepository.save(any(Product.class))).willReturn(sampleProduct);

            ProductUpdateRequest updateRequest = new ProductUpdateRequest();
            updateRequest.setName("Updated Name");
            updateRequest.setDescription("Updated Desc");
            updateRequest.setPrice(new BigDecimal("99.99"));
            updateRequest.setCategory("Updated Category");
            updateRequest.setStockQuantity(200);
            updateRequest.setActive(false);

            productService.updateProduct(1L, updateRequest);

            // Verify the entity was updated with ALL new values
            ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
            then(productRepository).should().save(captor.capture());
            Product saved = captor.getValue();
            assertThat(saved.getName()).isEqualTo("Updated Name");
            assertThat(saved.getDescription()).isEqualTo("Updated Desc");
            assertThat(saved.getPrice()).isEqualByComparingTo("99.99");
            assertThat(saved.getCategory()).isEqualTo("Updated Category");
            assertThat(saved.getStockQuantity()).isEqualTo(200);
            assertThat(saved.getActive()).isFalse();
            // updatedAt should be more recent than the original
            assertThat(saved.getUpdatedAt()).isAfter(
                    Instant.parse("2026-01-01T00:00:00Z"));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when product not found")
        void shouldThrowWhenNotFound() {
            given(productRepository.findById(999L)).willReturn(Optional.empty());

            ProductUpdateRequest request = new ProductUpdateRequest();
            request.setName("X");
            request.setPrice(BigDecimal.TEN);

            assertThatThrownBy(() -> productService.updateProduct(999L, request))
                    .isInstanceOf(ResourceNotFoundException.class);

            then(productRepository).should(never()).save(any());
        }
    }

    // ==================================================================
    // PATCH (partial update)
    // ==================================================================

    @Nested
    @DisplayName("patchProduct")
    class PatchProduct {

        @Test
        @DisplayName("should update only the fields present in the patch")
        void shouldUpdateOnlyPresentFields() {
            given(productRepository.findById(1L)).willReturn(Optional.of(sampleProduct));
            given(productRepository.save(any(Product.class))).willReturn(sampleProduct);

            // Patch only price
            ProductPatchRequest patch = new ProductPatchRequest();
            patch.setPrice(new BigDecimal("59.99"));
            // name, description, category, stockQuantity, active NOT set

            productService.patchProduct(1L, patch);

            ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
            then(productRepository).should().save(captor.capture());
            Product saved = captor.getValue();

            // Price changed
            assertThat(saved.getPrice()).isEqualByComparingTo("59.99");
            // Other fields unchanged (original values preserved)
            assertThat(saved.getName()).isEqualTo("Wireless Keyboard");
            assertThat(saved.getDescription()).isEqualTo("Bluetooth keyboard");
            assertThat(saved.getCategory()).isEqualTo("Electronics");
            assertThat(saved.getStockQuantity()).isEqualTo(150);
            assertThat(saved.getActive()).isTrue();
        }

        @Test
        @DisplayName("should clear field when explicitly set to null")
        void shouldClearFieldWhenNull() {
            given(productRepository.findById(1L)).willReturn(Optional.of(sampleProduct));
            given(productRepository.save(any(Product.class))).willReturn(sampleProduct);

            // Explicitly set description to null (JSON: {"description": null})
            ProductPatchRequest patch = new ProductPatchRequest();
            patch.setDescription(null); // This calls setDescription, adding to fieldsPresent

            productService.patchProduct(1L, patch);

            ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
            then(productRepository).should().save(captor.capture());
            Product saved = captor.getValue();

            // Description was explicitly nulled
            assertThat(saved.getDescription()).isNull();
            // Name untouched
            assertThat(saved.getName()).isEqualTo("Wireless Keyboard");
        }

        @Test
        @DisplayName("should throw when patching results in blank name")
        void shouldRejectBlankName() {
            given(productRepository.findById(1L)).willReturn(Optional.of(sampleProduct));

            ProductPatchRequest patch = new ProductPatchRequest();
            patch.setName("");

            assertThatThrownBy(() -> productService.patchProduct(1L, patch))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("blank");

            then(productRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("should throw when patching results in negative price")
        void shouldRejectNegativePrice() {
            given(productRepository.findById(1L)).willReturn(Optional.of(sampleProduct));

            ProductPatchRequest patch = new ProductPatchRequest();
            patch.setPrice(new BigDecimal("-10"));

            assertThatThrownBy(() -> productService.patchProduct(1L, patch))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Price");

            then(productRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when product not found")
        void shouldThrowWhenNotFound() {
            given(productRepository.findById(999L)).willReturn(Optional.empty());

            ProductPatchRequest patch = new ProductPatchRequest();
            patch.setPrice(new BigDecimal("10"));

            assertThatThrownBy(() -> productService.patchProduct(999L, patch))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ==================================================================
    // DELETE
    // ==================================================================

    @Nested
    @DisplayName("deleteProduct")
    class DeleteProduct {

        @Test
        @DisplayName("should delete when product exists")
        void shouldDeleteWhenExists() {
            given(productRepository.existsById(1L)).willReturn(true);

            productService.deleteProduct(1L);

            then(productRepository).should().deleteById(1L);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when product not found")
        void shouldThrowWhenNotFound() {
            given(productRepository.existsById(999L)).willReturn(false);

            assertThatThrownBy(() -> productService.deleteProduct(999L))
                    .isInstanceOf(ResourceNotFoundException.class);

            then(productRepository).should(never()).deleteById(any());
        }
    }

    // ==================================================================
    // SEARCH
    // ==================================================================

    @Nested
    @DisplayName("searchProducts")
    class SearchProducts {

        @Test
        @DisplayName("should pass all filter parameters to repository")
        void shouldPassFiltersToRepository() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Product> page = new PageImpl<>(List.of(sampleProduct), pageable, 1);
            given(productRepository.searchProducts(
                    "keyboard", "Electronics",
                    new BigDecimal("10"), new BigDecimal("100"),
                    true, pageable)).willReturn(page);

            PageResponse<ProductResponse> response = productService.searchProducts(
                    "keyboard", "Electronics",
                    new BigDecimal("10"), new BigDecimal("100"),
                    true, pageable);

            assertThat(response.getContent()).hasSize(1);
            assertThat(response.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("should pass null filters for unspecified criteria")
        void shouldPassNullsForUnspecifiedCriteria() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Product> page = new PageImpl<>(List.of(), pageable, 0);
            given(productRepository.searchProducts(
                    null, null, null, null, null, pageable)).willReturn(page);

            PageResponse<ProductResponse> response = productService.searchProducts(
                    null, null, null, null, null, pageable);

            assertThat(response.getContent()).isEmpty();
        }
    }
}
