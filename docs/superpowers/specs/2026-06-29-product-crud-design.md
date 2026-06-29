# Product CRUD Spring Boot Application — Design Spec

**Date:** 2026-06-29  
**Author:** Nirank Jawale  
**Status:** Approved

---

## 1. Overview

A small Spring Boot CRUD application managing **Products**. It serves as a POC baseline in the `Automation` folder, with 7 enhancement specs planned for iterative feature development.

**Stack:**
- Java 17
- Spring Boot 3.2.x
- Spring Data JPA + Hibernate
- MySQL 8
- Maven

---

## 2. Project Structure

```
product-crud/
├── src/main/java/com/tdg/productcrud/
│   ├── controller/
│   │   └── ProductController.java
│   ├── service/
│   │   ├── ProductService.java          (interface)
│   │   └── ProductServiceImpl.java
│   ├── repository/
│   │   └── ProductRepository.java
│   ├── entity/
│   │   └── Product.java
│   ├── dto/
│   │   ├── ProductRequestDto.java
│   │   └── ProductResponseDto.java
│   └── exception/
│       ├── ResourceNotFoundException.java
│       └── GlobalExceptionHandler.java
├── src/main/resources/
│   └── application.yml
└── pom.xml
```

---

## 3. Data Model

**Entity:** `Product`

| Field | Type | Constraint | Notes |
|---|---|---|---|
| `id` | `Long` | PK, auto-generated | |
| `name` | `String` | NOT NULL | Product name |
| `category` | `String` | NOT NULL | e.g. Electronics, Clothing |
| `price` | `BigDecimal` | NOT NULL | Must be > 0 |
| `stockQuantity` | `Integer` | NOT NULL | Must be >= 0 |
| `createdAt` | `LocalDateTime` | Auto-set on insert | Set via `@PrePersist` |

---

## 4. API Endpoints

Base path: `/api/products`

| Method | Path | Description | Request Body | Response |
|---|---|---|---|---|
| `POST` | `/api/products` | Create product | `ProductRequestDto` | `201 ProductResponseDto` |
| `GET` | `/api/products` | Get all products | — | `200 List<ProductResponseDto>` |
| `GET` | `/api/products/{id}` | Get by ID | — | `200 ProductResponseDto` |
| `PUT` | `/api/products/{id}` | Full update | `ProductRequestDto` | `200 ProductResponseDto` |
| `DELETE` | `/api/products/{id}` | Delete by ID | — | `204 No Content` |

### DTOs

**ProductRequestDto** (inbound):
```json
{
  "name": "Laptop",
  "category": "Electronics",
  "price": 999.99,
  "stockQuantity": 50
}
```

**ProductResponseDto** (outbound):
```json
{
  "id": 1,
  "name": "Laptop",
  "category": "Electronics",
  "price": 999.99,
  "stockQuantity": 50,
  "createdAt": "2026-06-29T10:00:00"
}
```

---

## 5. Error Handling

A `ResourceNotFoundException` is thrown from the service when a product ID is not found. A `GlobalExceptionHandler` (`@ControllerAdvice`) catches it and returns:

```json
{
  "timestamp": "2026-06-29T10:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Product not found with id: 5"
}
```

---

## 6. Architecture — Layered Pattern

```
HTTP Request
    ↓
ProductController       (maps HTTP ↔ DTOs, delegates to service)
    ↓
ProductService          (business logic, throws domain exceptions)
    ↓
ProductRepository       (Spring Data JPA, talks to MySQL)
    ↓
MySQL 8 Database
```

Each layer communicates only with its immediate neighbour. The entity never crosses the controller boundary — DTOs are the API contract.

---

## 7. Enhancement Specs (Planned)

Seven feature specs live under `docs/enhancements/`. Each follows:
**Goal → Requirements → API Changes → Implementation Notes → Acceptance Criteria**

| # | File | Feature |
|---|---|---|
| 1 | `01-search-and-filtering.md` | Filter by category, price range via JPA Specifications |
| 2 | `02-pagination-and-sorting.md` | `Pageable` support on list endpoint |
| 3 | `03-jwt-security.md` | Spring Security + JWT token auth |
| 4 | `04-swagger-openapi.md` | springdoc-openapi, `/swagger-ui.html` |
| 5 | `05-global-exception-handling.md` | Expand handler: validation errors, DB constraint violations, 500 fallback |
| 6 | `06-auditing.md` | `@CreatedBy`, `@LastModifiedBy`, `@LastModifiedDate` via Spring Data Auditing |
| 7 | `07-input-validation.md` | Bean Validation on DTOs: `@NotNull`, `@Min`, `@Size`, `@DecimalMin` |

---

## 8. Database Configuration (application.yml sketch)

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/productdb
    username: root
    password: yourpassword
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
```

---

## 9. Acceptance Criteria (Baseline)

- [ ] All 5 CRUD endpoints return correct HTTP status codes
- [ ] Missing product ID returns `404` with structured error JSON
- [ ] `createdAt` is auto-populated on insert
- [ ] DTOs are used at the controller boundary — entity is never serialised directly
- [ ] MySQL connection configured via `application.yml`
- [ ] Project builds cleanly with `mvn clean package`
