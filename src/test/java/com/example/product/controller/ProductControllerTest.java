package com.example.product.controller;

import com.example.product.dto.PageResponse;
import com.example.product.dto.ProductResponse;
import com.example.product.exception.ResourceNotFoundException;
import com.example.product.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller-layer tests using @WebMvcTest + MockMvc.
 *
 * KEY CONCEPTS:
 *
 * @WebMvcTest(ProductController.class) — loads ONLY the Spring MVC layer:
 *   - The specified controller
 *   - Jackson ObjectMapper
 *   - Validation infrastructure
 *   - Exception handlers (@RestControllerAdvice)
 *   - MockMvc auto-configuration
 *   It does NOT load services, repositories, or the database.
 *
 * @MockBean — creates a Mockito mock and registers it in the Spring context
 *   as a replacement for the real bean. The controller gets this mock injected.
 *
 * MockMvc — simulates HTTP requests without starting a real server. It goes
 *   through the DispatcherServlet, so it tests:
 *   - URL routing (@GetMapping, @PostMapping, etc.)
 *   - Request deserialization (JSON → DTO)
 *   - Bean Validation (@Valid)
 *   - Response serialization (DTO → JSON)
 *   - Status codes
 *   - Headers
 *   - Exception handling
 *
 * WHAT WE'RE TESTING HERE: the HTTP contract. Given a specific HTTP request,
 * does the controller return the correct status, headers, and JSON body?
 * Business logic is NOT tested here — the service is mocked.
 *
 * COMPARED TO UNIT TESTS:
 *   Unit tests: "does the service logic work?" (mock the repository)
 *   Controller tests: "does the HTTP layer work?" (mock the service)
 *   Integration tests: "does everything work together?" (mock nothing)
 */
@WebMvcTest(ProductController.class)
@DisplayName("ProductController")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    private static final String BASE_URL = "/api/v1/products";

    // ==================================================================
    // POST /api/v1/products — Create
    // ==================================================================

    @Nested
    @DisplayName("POST /api/v1/products")
    class CreateProduct {

        @Test
        @DisplayName("should return 201 with Location header and body")
        void shouldReturn201WithLocationAndBody() throws Exception {
            ProductResponse response = buildResponse(1L, "Keyboard", "49.99");
            given(productService.createProduct(any())).willReturn(response);

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "name": "Keyboard",
                                      "price": 49.99,
                                      "category": "Electronics"
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "/api/v1/products/1"))
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("Keyboard"))
                    .andExpect(jsonPath("$.price").value(49.99));
        }

        @Test
        @DisplayName("should return 422 when name is blank")
        void shouldReturn422WhenNameBlank() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": "", "price": 10}
                                    """))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.title").value("Validation Failed"))
                    .andExpect(jsonPath("$.fieldErrors").isArray())
                    .andExpect(jsonPath("$.fieldErrors[?(@.field=='name')]").exists());
        }

        @Test
        @DisplayName("should return 422 when price is negative")
        void shouldReturn422WhenPriceNegative() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": "Valid", "price": -5}
                                    """))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.fieldErrors[?(@.field=='price')]").exists());
        }

        @Test
        @DisplayName("should return 422 when price is zero")
        void shouldReturn422WhenPriceZero() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": "Valid", "price": 0}
                                    """))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.fieldErrors[?(@.field=='price')]").exists());
        }

        @Test
        @DisplayName("should return 422 when required fields missing")
        void shouldReturn422WhenFieldsMissing() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.fieldErrors", hasSize(greaterThanOrEqualTo(2))));
        }

        @Test
        @DisplayName("should return 422 when stockQuantity is negative")
        void shouldReturn422WhenStockNegative() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": "Valid", "price": 10, "stockQuantity": -1}
                                    """))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.fieldErrors[?(@.field=='stockQuantity')]").exists());
        }
    }

    // ==================================================================
    // GET /api/v1/products/{id}
    // ==================================================================

    @Nested
    @DisplayName("GET /api/v1/products/{id}")
    class GetProduct {

        @Test
        @DisplayName("should return 200 with product when found")
        void shouldReturn200WhenFound() throws Exception {
            ProductResponse response = buildResponse(1L, "Keyboard", "49.99");
            given(productService.getProduct(1L)).willReturn(response);

            mockMvc.perform(get(BASE_URL + "/1"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("Keyboard"))
                    .andExpect(jsonPath("$.price").value(49.99));
        }

        @Test
        @DisplayName("should return 404 when not found")
        void shouldReturn404WhenNotFound() throws Exception {
            given(productService.getProduct(999L))
                    .willThrow(new ResourceNotFoundException("Product", 999L));

            mockMvc.perform(get(BASE_URL + "/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.detail").value(containsString("999")));
        }
    }

    // ==================================================================
    // GET /api/v1/products — List
    // ==================================================================

    @Nested
    @DisplayName("GET /api/v1/products")
    class ListProducts {

        @Test
        @DisplayName("should return 200 with paginated products")
        void shouldReturn200WithPaginatedProducts() throws Exception {
            ProductResponse product = buildResponse(1L, "Keyboard", "49.99");
            PageResponse<ProductResponse> pageResponse = new PageResponse<>(
                    List.of(product), 0, 20, 1, 1, true, true);
            given(productService.listProducts(any())).willReturn(pageResponse);

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].name").value("Keyboard"))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(20))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.first").value(true))
                    .andExpect(jsonPath("$.last").value(true));
        }

        @Test
        @DisplayName("should accept pagination parameters")
        void shouldAcceptPaginationParams() throws Exception {
            PageResponse<ProductResponse> pageResponse = new PageResponse<>(
                    List.of(), 2, 5, 0, 0, false, true);
            given(productService.listProducts(any())).willReturn(pageResponse);

            mockMvc.perform(get(BASE_URL)
                            .param("page", "2")
                            .param("size", "5")
                            .param("sort", "name,desc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.page").value(2))
                    .andExpect(jsonPath("$.size").value(5));
        }
    }

    // ==================================================================
    // PUT /api/v1/products/{id}
    // ==================================================================

    @Nested
    @DisplayName("PUT /api/v1/products/{id}")
    class UpdateProduct {

        @Test
        @DisplayName("should return 200 with updated product")
        void shouldReturn200WithUpdatedProduct() throws Exception {
            ProductResponse response = buildResponse(1L, "Updated", "99.99");
            given(productService.updateProduct(eq(1L), any())).willReturn(response);

            mockMvc.perform(put(BASE_URL + "/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": "Updated", "price": 99.99}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Updated"))
                    .andExpect(jsonPath("$.price").value(99.99));
        }

        @Test
        @DisplayName("should return 404 when product not found")
        void shouldReturn404WhenNotFound() throws Exception {
            given(productService.updateProduct(eq(999L), any()))
                    .willThrow(new ResourceNotFoundException("Product", 999L));

            mockMvc.perform(put(BASE_URL + "/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": "X", "price": 10}
                                    """))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 422 when validation fails")
        void shouldReturn422WhenValidationFails() throws Exception {
            mockMvc.perform(put(BASE_URL + "/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": "", "price": -5}
                                    """))
                    .andExpect(status().isUnprocessableEntity());
        }
    }

    // ==================================================================
    // PATCH /api/v1/products/{id}
    // ==================================================================

    @Nested
    @DisplayName("PATCH /api/v1/products/{id}")
    class PatchProduct {

        @Test
        @DisplayName("should return 200 with patched product")
        void shouldReturn200WithPatchedProduct() throws Exception {
            ProductResponse response = buildResponse(1L, "Keyboard", "59.99");
            given(productService.patchProduct(eq(1L), any())).willReturn(response);

            mockMvc.perform(patch(BASE_URL + "/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"price": 59.99}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.price").value(59.99));
        }

        @Test
        @DisplayName("should return 404 when product not found")
        void shouldReturn404WhenNotFound() throws Exception {
            given(productService.patchProduct(eq(999L), any()))
                    .willThrow(new ResourceNotFoundException("Product", 999L));

            mockMvc.perform(patch(BASE_URL + "/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"price": 10}
                                    """))
                    .andExpect(status().isNotFound());
        }
    }

    // ==================================================================
    // DELETE /api/v1/products/{id}
    // ==================================================================

    @Nested
    @DisplayName("DELETE /api/v1/products/{id}")
    class DeleteProduct {

        @Test
        @DisplayName("should return 204 when product deleted")
        void shouldReturn204WhenDeleted() throws Exception {
            willDoNothing().given(productService).deleteProduct(1L);

            mockMvc.perform(delete(BASE_URL + "/1"))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));
        }

        @Test
        @DisplayName("should return 404 when product not found")
        void shouldReturn404WhenNotFound() throws Exception {
            willThrow(new ResourceNotFoundException("Product", 999L))
                    .given(productService).deleteProduct(999L);

            mockMvc.perform(delete(BASE_URL + "/999"))
                    .andExpect(status().isNotFound());
        }
    }

    // ==================================================================
    // GET /api/v1/products/search
    // ==================================================================

    @Nested
    @DisplayName("GET /api/v1/products/search")
    class SearchProducts {

        @Test
        @DisplayName("should return 200 with filtered results")
        void shouldReturn200WithFilteredResults() throws Exception {
            ProductResponse product = buildResponse(1L, "Keyboard", "49.99");
            PageResponse<ProductResponse> pageResponse = new PageResponse<>(
                    List.of(product), 0, 20, 1, 1, true, true);
            given(productService.searchProducts(any(), any(), any(), any(), any(), any()))
                    .willReturn(pageResponse);

            mockMvc.perform(get(BASE_URL + "/search")
                            .param("name", "keyboard")
                            .param("category", "Electronics")
                            .param("minPrice", "10")
                            .param("maxPrice", "100"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].name").value("Keyboard"));
        }

        @Test
        @DisplayName("should return 200 with empty results when no match")
        void shouldReturn200EmptyWhenNoMatch() throws Exception {
            PageResponse<ProductResponse> emptyResponse = new PageResponse<>(
                    List.of(), 0, 20, 0, 0, true, true);
            given(productService.searchProducts(any(), any(), any(), any(), any(), any()))
                    .willReturn(emptyResponse);

            mockMvc.perform(get(BASE_URL + "/search")
                            .param("name", "nonexistent"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)))
                    .andExpect(jsonPath("$.totalElements").value(0));
        }
    }

    // ==================================================================
    // Helper methods
    // ==================================================================

    private ProductResponse buildResponse(Long id, String name, String price) {
        ProductResponse response = new ProductResponse();
        response.setId(id);
        response.setName(name);
        response.setPrice(new BigDecimal(price));
        response.setCategory("Electronics");
        response.setStockQuantity(100);
        response.setActive(true);
        response.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        response.setUpdatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        return response;
    }
}
