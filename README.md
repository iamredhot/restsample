# Product Management REST API

A complete REST API for managing products, built as a hands-on learning project for REST API development, testing, and design principles.

## Tech Stack

| Component | Version | Purpose |
|---|---|---|
| Java | 21 | Language runtime |
| Spring Boot | 3.5.7 | Application framework |
| Spring Web (MVC) | 6.2.12 | REST controllers, DispatcherServlet |
| Spring Data JPA | 3.5.7 | Repository abstraction over Hibernate |
| Hibernate ORM | 6.6.33 | JPA implementation |
| Jakarta Bean Validation | 3.0 | Request validation (@NotBlank, @Positive, etc.) |
| H2 Database | 1.4.199 | In-memory database (dev/learning) |
| Jackson | 2.19.x | JSON serialization/deserialization |
| JUnit 5 | 5.12.2 | Test framework |
| Mockito | 5.17.0 | Mocking framework |
| AssertJ | 3.27.6 | Fluent test assertions |
| Maven | 3.9.9 | Build tool |

## Prerequisites

```bash
# Verify Java 21+
java -version

# Verify Maven 3.8+
mvn -version
```

## Quick Start

```bash
# Clone / navigate to the project
cd /path/to/testproj

# Build (compile + test + package)
mvn clean package -o

# Run the application
java -jar target/product-api-0.0.1-SNAPSHOT.jar

# Application starts on http://localhost:8080
# 5 sample products are loaded automatically
```

### Windows PowerShell

```powershell
# Build
mvn clean package -o

# Run
java -jar target\product-api-0.0.1-SNAPSHOT.jar

# Test an endpoint
Invoke-RestMethod -Uri http://localhost:8080/api/v1/products
```

### Linux / macOS

```bash
# Build
mvn clean package -o

# Run (background)
java -jar target/product-api-0.0.1-SNAPSHOT.jar &

# Test an endpoint
curl http://localhost:8080/api/v1/products | python3 -m json.tool

# Stop
kill %1
```

## URLs

| URL | Description |
|---|---|
| `http://localhost:8080/api/v1/products` | Product API base |
| `http://localhost:8080/api/v1/products/{id}` | Single product |
| `http://localhost:8080/api/v1/products/search` | Search/filter |

**Note:** Swagger UI (`/swagger-ui/index.html`) and Actuator (`/actuator/health`) require additional dependencies not currently in the local Maven cache. See [Adding Optional Dependencies](#adding-optional-dependencies) below.

## API Endpoints

| Method | Endpoint | Description | Success | Error |
|---|---|---|---|---|
| `POST` | `/api/v1/products` | Create product | 201 + Location | 400, 422 |
| `GET` | `/api/v1/products` | List products (paginated) | 200 | — |
| `GET` | `/api/v1/products/{id}` | Get product by ID | 200 | 404 |
| `PUT` | `/api/v1/products/{id}` | Full update (replace) | 200 | 404, 422 |
| `PATCH` | `/api/v1/products/{id}` | Partial update (merge patch) | 200 | 400, 404 |
| `DELETE` | `/api/v1/products/{id}` | Delete product | 204 | 404 |
| `GET` | `/api/v1/products/search` | Search with filters | 200 | — |

### Query Parameters

**List / Search pagination:**
- `page` — page number, 0-based (default: `0`)
- `size` — items per page (default: `20`, max: `100`)
- `sort` — field and direction, e.g. `name,asc` or `price,desc`

**Search filters (all optional):**
- `name` — partial match, case-insensitive
- `category` — exact match, case-insensitive
- `minPrice` / `maxPrice` — inclusive price range
- `active` — boolean filter

### Example Requests

**Create a product:**
```bash
curl -X POST http://localhost:8080/api/v1/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Wireless Keyboard",
    "description": "Bluetooth mechanical keyboard",
    "price": 49.99,
    "category": "Electronics",
    "stockQuantity": 20,
    "active": true
  }'
```

**Search by category and price range:**
```bash
curl "http://localhost:8080/api/v1/products/search?category=Electronics&minPrice=20&maxPrice=100"
```

**Partial update (change price only):**
```bash
curl -X PATCH http://localhost:8080/api/v1/products/1 \
  -H "Content-Type: application/json" \
  -d '{"price": 59.99}'
```

### Error Response Format

All errors use RFC 9457 ProblemDetail:

```json
{
  "type": "about:blank",
  "title": "Validation Failed",
  "status": 422,
  "detail": "Request validation failed",
  "instance": "/api/v1/products",
  "fieldErrors": [
    {
      "field": "name",
      "message": "Product name is required",
      "rejectedValue": "null"
    }
  ]
}
```

## Testing

```bash
# Run all 54 tests
mvn test -o

# Run only unit tests (fast, ~1s)
mvn test -o -Dtest="*ServiceImplTest,*MapperTest"

# Run only controller tests (~2s)
mvn test -o -Dtest="*ControllerTest"

# Run only integration tests (~8s)
mvn test -o -Dtest="*IntegrationTest"
```

### Test Summary

| Test Class | Layer | Tests | What It Verifies |
|---|---|---|---|
| `ProductServiceImplTest` | Unit (Mockito) | 16 | Service logic, defaults, exceptions, patch merge |
| `ProductMapperTest` | Unit (pure) | 6 | Entity↔DTO mapping, defaults, page conversion |
| `ProductControllerTest` | Controller (@WebMvcTest) | 19 | HTTP routing, validation, status codes, JSON shapes |
| `ProductApiIntegrationTest` | Integration (@SpringBootTest) | 11 | Full flow: HTTP → DB → response, CRUD lifecycle |
| **Total** | | **54** | |

## Project Structure

```
src/main/java/com/example/product/
├── ProductApiApplication.java          # Entry point (@SpringBootApplication)
├── controller/
│   └── ProductController.java          # REST endpoints (@RestController)
├── service/
│   ├── ProductService.java             # Service interface
│   └── ProductServiceImpl.java         # Business logic (@Service, @Transactional)
├── repository/
│   └── ProductRepository.java          # Data access (JpaRepository + @Query search)
├── entity/
│   └── Product.java                    # JPA entity (@Entity)
├── dto/
│   ├── ProductCreateRequest.java       # POST body (with validation)
│   ├── ProductUpdateRequest.java       # PUT body (with validation)
│   ├── ProductPatchRequest.java        # PATCH body (setter tracking for merge patch)
│   ├── ProductResponse.java            # Response shape
│   └── PageResponse.java              # Custom pagination DTO (replaces Spring's Page)
├── mapper/
│   └── ProductMapper.java              # Entity ↔ DTO conversion (static methods)
└── exception/
    ├── GlobalExceptionHandler.java     # @RestControllerAdvice → ProblemDetail
    └── ResourceNotFoundException.java  # Custom 404 exception

src/main/resources/
├── application.yml                     # All configuration
└── data.sql                            # 5 sample products loaded on startup

src/test/java/com/example/product/
├── service/ProductServiceImplTest.java # Unit tests (Mockito)
├── mapper/ProductMapperTest.java       # Unit tests (pure)
├── controller/ProductControllerTest.java # @WebMvcTest + MockMvc
└── ProductApiIntegrationTest.java      # @SpringBootTest + real H2
```

## Design Decisions

| Decision | Choice | Rationale |
|---|---|---|
| Entity vs DTO | Separate DTOs per operation | Contract stability, validation specificity, mass-assignment prevention |
| Timestamps | `Instant` (UTC) | Unambiguous, timezone-safe |
| Money type | `BigDecimal` | Exact precision (never float/double for money) |
| Pagination | Custom `PageResponse<T>` | Decouples API contract from Spring's internal `Page` format |
| PATCH format | JSON Merge Patch (RFC 7396) | Simple; setter tracking distinguishes absent vs explicit null |
| PUT semantics | Full replacement | Omitted optional fields reset to defaults |
| Error format | RFC 9457 ProblemDetail | Standard, machine-readable, consistent |
| Versioning | URI path (`/api/v1`) | Simple routing/caching/documentation |
| Validation errors | 422 (not 400) | Distinguishes "semantically invalid" from "malformed request" |
| Mapper | Hand-written static methods | Transparent field mapping; swap to MapStruct for large projects |

## Adding Optional Dependencies

When Maven network access is restored, add these to `pom.xml`:

**Swagger UI + OpenAPI documentation:**
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.6.0</version>
</dependency>
```
Access: `http://localhost:8080/swagger-ui/index.html`

**Spring Boot Actuator (health checks, metrics):**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```
Access: `http://localhost:8080/actuator/health`

**REST Assured (fluent API testing):**
```xml
<dependency>
    <groupId>io.rest-assured</groupId>
    <artifactId>rest-assured</artifactId>
    <scope>test</scope>
</dependency>
```

**Flyway (database migrations):**
```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
```

## Troubleshooting

| Problem | Cause | Fix |
|---|---|---|
| `Port 8080 already in use` | Another process on 8080 | `lsof -ti:8080 \| xargs kill -9` (Linux/Mac) or `netstat -ano \| findstr :8080` then `taskkill /PID <pid> /F` (Windows) |
| `Connection refused` on curl | App not running | Check `java -jar` output for errors |
| `415 Unsupported Media Type` | Missing Content-Type header | Add `-H "Content-Type: application/json"` to curl |
| `400 Failed to read request` | Malformed JSON | Check for missing quotes, trailing commas |
| `422 Validation Failed` | Input violates constraints | Check `fieldErrors` array for specific fields |
| `500 Internal Server Error` | Unhandled exception | Check application console for stack trace |
| `mvn` can't download deps | Corporate proxy TLS issue | Use `-o` (offline) flag; all core deps are cached |
| Tests fail with `ClassNotFoundException` | Stale compiled classes | Run `mvn clean test -o` |
| H2 data gone after restart | In-memory DB (by design) | `data.sql` re-seeds 5 products on each start |
| `@MockBean` deprecation warning | Spring Boot 3.4+ migration | Safe to ignore; replace with `@MockitoBean` when ready |

## Environment Notes

This project was built in an environment with a corporate Maven proxy (CITnet) that has TLS certificate issues. All dependencies use versions available in the local Maven cache:

- Spring Boot 3.5.7 parent + starters: cached
- H2: pinned to 1.4.199 (cached; Hibernate warns about minimum 2.1.214 but works)
- maven-jar-plugin: pinned to 3.3.0 (cached; parent wants 3.4.2)
- Build uses `-o` (offline) flag

When network access is restored, remove version pins and the `-o` flag to use current dependency versions.

## Progress Checklist

- [x] Phase 1: REST API fundamentals (conceptual)
- [x] Phase 2: OpenAPI & Swagger (conceptual)
- [x] Phase 3: API design (resource model, endpoints, DTO strategy, diagrams)
- [x] Phase 4: Implementation (14 source files, layered architecture)
- [x] Phase 5: API hands-on exercises (35 curl-based scenarios, all verified)
- [x] Phase 6: Testing — 54 tests (unit + controller + integration), all passing
- [x] Phase 7: Security & production readiness (conceptual extension)
- [x] Phase 8: Build & execution instructions, README
- [ ] Phase 9: Architecture review & interview questions
- [ ] Phase 10: Practice assignments
