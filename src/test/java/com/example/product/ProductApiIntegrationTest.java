package com.example.product;

import com.example.product.dto.PageResponse;
import com.example.product.dto.ProductResponse;
import com.example.product.entity.Product;
import com.example.product.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests — full application context with real database.
 *
 * KEY CONCEPTS:
 *
 * @SpringBootTest — loads the COMPLETE Spring ApplicationContext:
 *   - All beans (controllers, services, repositories, config)
 *   - Real DataSource (H2 in-memory)
 *   - Real Hibernate and JPA
 *   - Real validation
 *   - Real exception handling
 *
 * @AutoConfigureMockMvc — provides MockMvc wired to the full context.
 *   Unlike @WebMvcTest, this MockMvc goes through the entire real stack,
 *   including service → repository → database.
 *
 * @BeforeEach + repository.deleteAll() — ensures each test starts with
 *   a clean database. This is critical for test isolation:
 *   tests must not depend on each other's data.
 *
 * WHAT WE'RE TESTING: the complete flow, end-to-end.
 *   HTTP request → controller → validation → service → repository → H2 → response.
 *   Nothing is mocked. If any layer has a bug, these tests catch it.
 *
 * TRADE-OFF: these tests are slower (full context startup, real DB) but
 *   provide the highest confidence. We write fewer of these and focus
 *   on critical paths and scenarios that can't be caught by unit/controller tests.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Product API Integration Tests")
class ProductApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    private static final String BASE_URL = "/api/v1/products";

    @BeforeEach
    void setUp() {
        // Clean DB before each test for isolation.
        // data.sql seeds also run, but deleteAll ensures a clean slate.
        productRepository.deleteAll();
    }

    // ==================================================================
    // CREATE → READ flow
    // ==================================================================

    @Nested
    @DisplayName("Create and Read")
    class CreateAndRead {

        @Test
        @DisplayName("should create product and retrieve it by ID")
        void shouldCreateAndRetrieve() throws Exception {
            // Create
            MvcResult createResult = mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "name": "Integration Test Product",
                                      "description": "Created in integration test",
                                      "price": 25.50,
                                      "category": "Testing",
                                      "stockQuantity": 10,
                                      "active": true
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"))
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.name").value("Integration Test Product"))
                    .andExpect(jsonPath("$.price").value(25.50))
                    .andExpect(jsonPath("$.createdAt").exists())
                    .andReturn();

            // Extract ID from response
            String responseJson = createResult.getResponse().getContentAsString();
            ProductResponse created = objectMapper.readValue(responseJson, ProductResponse.class);
            Long id = created.getId();

            // Read back
            mockMvc.perform(get(BASE_URL + "/" + id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id))
                    .andExpect(jsonPath("$.name").value("Integration Test Product"))
                    .andExpect(jsonPath("$.price").value(25.50))
                    .andExpect(jsonPath("$.category").value("Testing"));

            // Verify in database directly
            Product fromDb = productRepository.findById(id).orElseThrow();
            assertThat(fromDb.getName()).isEqualTo("Integration Test Product");
            assertThat(fromDb.getPrice()).isEqualByComparingTo("25.50");
        }

        @Test
        @DisplayName("should persist to database and retrieve in list")
        void shouldPersistAndList() throws Exception {
            // Create two products
            createProduct("Product A", "10.00");
            createProduct("Product B", "20.00");

            // List
            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(2)))
                    .andExpect(jsonPath("$.totalElements").value(2));
        }
    }

    // ==================================================================
    // UPDATE flow
    // ==================================================================

    @Nested
    @DisplayName("Update flows")
    class UpdateFlows {

        @Test
        @DisplayName("PUT should replace all fields and persist")
        void putShouldReplaceAndPersist() throws Exception {
            Long id = createProductAndGetId("Original", "10.00");

            mockMvc.perform(put(BASE_URL + "/" + id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "name": "Replaced",
                                      "price": 99.99,
                                      "category": "New Category",
                                      "stockQuantity": 500
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Replaced"))
                    .andExpect(jsonPath("$.price").value(99.99))
                    .andExpect(jsonPath("$.category").value("New Category"));

            // Verify in DB: description should be null (PUT clears omitted fields)
            Product fromDb = productRepository.findById(id).orElseThrow();
            assertThat(fromDb.getName()).isEqualTo("Replaced");
            assertThat(fromDb.getDescription()).isNull();
        }

        @Test
        @DisplayName("PATCH should update only specified fields")
        void patchShouldUpdateOnlySpecifiedFields() throws Exception {
            Long id = createProductAndGetId("Original", "10.00");

            // PATCH only the price
            mockMvc.perform(patch(BASE_URL + "/" + id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"price": 55.55}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.price").value(55.55))
                    .andExpect(jsonPath("$.name").value("Original")); // unchanged

            // Verify in DB
            Product fromDb = productRepository.findById(id).orElseThrow();
            assertThat(fromDb.getPrice()).isEqualByComparingTo("55.55");
            assertThat(fromDb.getName()).isEqualTo("Original");
        }
    }

    // ==================================================================
    // DELETE flow
    // ==================================================================

    @Nested
    @DisplayName("Delete flow")
    class DeleteFlow {

        @Test
        @DisplayName("should delete and confirm removal")
        void shouldDeleteAndConfirmRemoval() throws Exception {
            Long id = createProductAndGetId("ToDelete", "10.00");

            // Delete
            mockMvc.perform(delete(BASE_URL + "/" + id))
                    .andExpect(status().isNoContent());

            // Confirm gone
            mockMvc.perform(get(BASE_URL + "/" + id))
                    .andExpect(status().isNotFound());

            // Confirm gone in DB
            assertThat(productRepository.findById(id)).isEmpty();
        }

        @Test
        @DisplayName("should return 404 when deleting non-existent product")
        void shouldReturn404ForNonExistent() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/99999"))
                    .andExpect(status().isNotFound());
        }
    }

    // ==================================================================
    // Validation (end-to-end through all layers)
    // ==================================================================

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("should reject creation with blank name")
        void shouldRejectBlankName() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": "", "price": 10}
                                    """))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.fieldErrors[?(@.field=='name')]").exists());

            // Verify nothing was persisted
            assertThat(productRepository.count()).isEqualTo(0);
        }

        @Test
        @DisplayName("should reject PATCH that would result in invalid state")
        void shouldRejectInvalidPatch() throws Exception {
            Long id = createProductAndGetId("Valid Product", "10.00");

            mockMvc.perform(patch(BASE_URL + "/" + id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"price": -5}
                                    """))
                    .andExpect(status().isBadRequest());

            // Verify original product unchanged
            Product fromDb = productRepository.findById(id).orElseThrow();
            assertThat(fromDb.getPrice()).isEqualByComparingTo("10.00");
        }
    }

    // ==================================================================
    // Search
    // ==================================================================

    @Nested
    @DisplayName("Search")
    class Search {

        @Test
        @DisplayName("should find products by category")
        void shouldFindByCategory() throws Exception {
            createProduct("Keyboard", "49.99", "Electronics");
            createProduct("Mouse", "29.99", "Electronics");
            createProduct("Book", "19.99", "Books");

            mockMvc.perform(get(BASE_URL + "/search")
                            .param("category", "Electronics"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(2))
                    .andExpect(jsonPath("$.content[*].category",
                            everyItem(equalToIgnoringCase("Electronics"))));
        }

        @Test
        @DisplayName("should find products by price range")
        void shouldFindByPriceRange() throws Exception {
            createProduct("Cheap", "5.00", "A");
            createProduct("Mid", "50.00", "A");
            createProduct("Expensive", "500.00", "A");

            mockMvc.perform(get(BASE_URL + "/search")
                            .param("minPrice", "10")
                            .param("maxPrice", "100"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].name").value("Mid"));
        }
    }

    // ==================================================================
    // Pagination
    // ==================================================================

    @Nested
    @DisplayName("Pagination")
    class PaginationTests {

        @Test
        @DisplayName("should paginate results correctly")
        void shouldPaginateCorrectly() throws Exception {
            // Create 5 products
            for (int i = 1; i <= 5; i++) {
                createProduct("Product " + i, String.valueOf(i * 10.0), "Cat");
            }

            // Page 0, size 2
            mockMvc.perform(get(BASE_URL)
                            .param("page", "0")
                            .param("size", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(2)))
                    .andExpect(jsonPath("$.totalElements").value(5))
                    .andExpect(jsonPath("$.totalPages").value(3))
                    .andExpect(jsonPath("$.first").value(true))
                    .andExpect(jsonPath("$.last").value(false));

            // Last page
            mockMvc.perform(get(BASE_URL)
                            .param("page", "2")
                            .param("size", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))  // 5th item
                    .andExpect(jsonPath("$.first").value(false))
                    .andExpect(jsonPath("$.last").value(true));
        }
    }

    // ==================================================================
    // Helpers
    // ==================================================================

    private void createProduct(String name, String price) throws Exception {
        createProduct(name, price, "Default");
    }

    private void createProduct(String name, String price, String category) throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {"name": "%s", "price": %s, "category": "%s"}
                                """, name, price, category)))
                .andExpect(status().isCreated());
    }

    private Long createProductAndGetId(String name, String price) throws Exception {
        MvcResult result = mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {"name": "%s", "description": "Test desc", "price": %s, "category": "Test"}
                                """, name, price)))
                .andExpect(status().isCreated())
                .andReturn();

        ProductResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), ProductResponse.class);
        return response.getId();
    }
}
