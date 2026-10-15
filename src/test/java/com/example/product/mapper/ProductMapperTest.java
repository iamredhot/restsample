package com.example.product.mapper;

import com.example.product.dto.PageResponse;
import com.example.product.dto.ProductCreateRequest;
import com.example.product.dto.ProductResponse;
import com.example.product.dto.ProductUpdateRequest;
import com.example.product.entity.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ProductMapper.
 *
 * These are pure unit tests — no Spring context, no mocking needed.
 * The mapper is a utility class with static methods and no dependencies.
 * This is the simplest, fastest kind of test.
 *
 * WHAT WE VERIFY:
 * - Every field is mapped correctly (no silent field drops)
 * - Defaults are applied (null stockQuantity → 0, null active → true)
 * - Page metadata is converted accurately
 */
@DisplayName("ProductMapper")
class ProductMapperTest {

    @Test
    @DisplayName("toEntity: should map all fields from CreateRequest")
    void toEntityShouldMapAllFields() {
        ProductCreateRequest request = new ProductCreateRequest();
        request.setName("Keyboard");
        request.setDescription("Bluetooth keyboard");
        request.setPrice(new BigDecimal("49.99"));
        request.setCategory("Electronics");
        request.setStockQuantity(50);
        request.setActive(false);

        Product entity = ProductMapper.toEntity(request);

        assertThat(entity.getId()).isNull(); // not set by mapper
        assertThat(entity.getName()).isEqualTo("Keyboard");
        assertThat(entity.getDescription()).isEqualTo("Bluetooth keyboard");
        assertThat(entity.getPrice()).isEqualByComparingTo("49.99");
        assertThat(entity.getCategory()).isEqualTo("Electronics");
        assertThat(entity.getStockQuantity()).isEqualTo(50);
        assertThat(entity.getActive()).isFalse();
        assertThat(entity.getCreatedAt()).isNull(); // set by service, not mapper
        assertThat(entity.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("toEntity: should default stockQuantity to 0 and active to true")
    void toEntityShouldApplyDefaults() {
        ProductCreateRequest request = new ProductCreateRequest();
        request.setName("Minimal");
        request.setPrice(new BigDecimal("10"));
        // stockQuantity and active not set (null)

        Product entity = ProductMapper.toEntity(request);

        assertThat(entity.getStockQuantity()).isEqualTo(0);
        assertThat(entity.getActive()).isTrue();
    }

    @Test
    @DisplayName("toResponse: should map all fields from entity")
    void toResponseShouldMapAllFields() {
        Product entity = buildSampleProduct();

        ProductResponse response = ProductMapper.toResponse(entity);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Keyboard");
        assertThat(response.getDescription()).isEqualTo("Bluetooth keyboard");
        assertThat(response.getPrice()).isEqualByComparingTo("49.99");
        assertThat(response.getCategory()).isEqualTo("Electronics");
        assertThat(response.getStockQuantity()).isEqualTo(50);
        assertThat(response.getActive()).isTrue();
        assertThat(response.getCreatedAt()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
        assertThat(response.getUpdatedAt()).isEqualTo(Instant.parse("2026-01-01T12:00:00Z"));
    }

    @Test
    @DisplayName("updateEntity: should replace all fields on the entity")
    void updateEntityShouldReplaceAllFields() {
        Product entity = buildSampleProduct();

        ProductUpdateRequest request = new ProductUpdateRequest();
        request.setName("New Name");
        request.setDescription("New Desc");
        request.setPrice(new BigDecimal("99.99"));
        request.setCategory("New Category");
        request.setStockQuantity(200);
        request.setActive(false);

        ProductMapper.updateEntity(entity, request);

        assertThat(entity.getName()).isEqualTo("New Name");
        assertThat(entity.getDescription()).isEqualTo("New Desc");
        assertThat(entity.getPrice()).isEqualByComparingTo("99.99");
        assertThat(entity.getCategory()).isEqualTo("New Category");
        assertThat(entity.getStockQuantity()).isEqualTo(200);
        assertThat(entity.getActive()).isFalse();
        // id and timestamps should NOT be changed by updateEntity
        assertThat(entity.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("updateEntity: should default stockQuantity and active when null")
    void updateEntityShouldApplyDefaults() {
        Product entity = buildSampleProduct();

        ProductUpdateRequest request = new ProductUpdateRequest();
        request.setName("Name");
        request.setPrice(new BigDecimal("10"));
        // stockQuantity and active null → defaults

        ProductMapper.updateEntity(entity, request);

        assertThat(entity.getStockQuantity()).isEqualTo(0);
        assertThat(entity.getActive()).isTrue();
    }

    @Test
    @DisplayName("toPageResponse: should convert Spring Page to our PageResponse")
    void toPageResponseShouldConvertCorrectly() {
        Product product1 = buildSampleProduct();
        Product product2 = buildSampleProduct();
        product2.setId(2L);
        product2.setName("Mouse");

        Page<Product> springPage = new PageImpl<>(
                List.of(product1, product2),
                PageRequest.of(0, 10),
                2);

        PageResponse<ProductResponse> response = ProductMapper.toPageResponse(springPage);

        assertThat(response.getContent()).hasSize(2);
        assertThat(response.getContent().get(0).getName()).isEqualTo("Keyboard");
        assertThat(response.getContent().get(1).getName()).isEqualTo("Mouse");
        assertThat(response.getPage()).isEqualTo(0);
        assertThat(response.getSize()).isEqualTo(10);
        assertThat(response.getTotalElements()).isEqualTo(2);
        assertThat(response.getTotalPages()).isEqualTo(1);
        assertThat(response.isFirst()).isTrue();
        assertThat(response.isLast()).isTrue();
    }

    // Helper
    private Product buildSampleProduct() {
        Product product = new Product();
        product.setId(1L);
        product.setName("Keyboard");
        product.setDescription("Bluetooth keyboard");
        product.setPrice(new BigDecimal("49.99"));
        product.setCategory("Electronics");
        product.setStockQuantity(50);
        product.setActive(true);
        product.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        product.setUpdatedAt(Instant.parse("2026-01-01T12:00:00Z"));
        return product;
    }
}
