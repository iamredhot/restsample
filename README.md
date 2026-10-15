# Product Management REST API

REST API for managing products — Spring Boot 3 learning project.

## Tech Stack

- Java 21, Spring Boot 3.5.7, Maven
- Spring Web (MVC), Spring Data JPA, Spring Validation (Jakarta Bean Validation)
- H2 in-memory database
- JUnit 5, Mockito, MockMvc

## Quick Start

```bash
# Build
mvn package -o -DskipTests

# Run
java -jar target/product-api-0.0.1-SNAPSHOT.jar

# Test an endpoint
curl http://localhost:8080/api/v1/products
```

## URLs

| URL | Description |
|---|---|
| `http://localhost:8080/api/v1/products` | Product API |
| `http://localhost:8080/h2-console` | H2 database browser (JDBC URL: `jdbc:h2:mem:productdb`, user: `sa`, no password) |

## Progress Checklist

- [x] Phase 1: REST API fundamentals (conceptual)
- [x] Phase 2: OpenAPI & Swagger (conceptual)
- [x] Phase 3: API design (resource model, endpoints, DTO strategy, diagrams)
- [x] Phase 4: Implementation (entity, repository, DTOs, mapper, service, controller, exception handling)
- [ ] Phase 5: Swagger UI hands-on exercises
- [ ] Phase 6: Testing (unit, controller, integration, contract)
- [ ] Phase 7: Security & production readiness
- [ ] Phase 8: Build & execution instructions (detailed)
- [ ] Phase 9: Architecture review & interview questions
- [ ] Phase 10: Practice assignments

## Endpoints

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/products` | Create product (201 + Location) |
| GET | `/api/v1/products` | List products (paginated) |
| GET | `/api/v1/products/{id}` | Get product |
| PUT | `/api/v1/products/{id}` | Full update |
| PATCH | `/api/v1/products/{id}` | Partial update (JSON Merge Patch) |
| DELETE | `/api/v1/products/{id}` | Delete product (204) |
| GET | `/api/v1/products/search` | Search/filter products |
