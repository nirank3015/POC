# Product CRUD API

A Spring Boot 3.2 REST API for managing products, built as a POC demonstrating a clean layered architecture with Java 17 and MySQL 8.

---

## Tech Stack

| Technology | Version |
|---|---|
| Java | 17 |
| Spring Boot | 3.2.5 |
| Spring Data JPA | (via Boot parent) |
| MySQL | 8.x |
| Maven | 3.x |
| JUnit 5 + Mockito | (via Boot parent) |

---

## Project Structure

```
product-crud/
├── src/
│   ├── main/
│   │   ├── java/com/tdg/productcrud/
│   │   │   ├── ProductCrudApplication.java     # Entry point
│   │   │   ├── controller/
│   │   │   │   └── ProductController.java      # REST endpoints
│   │   │   ├── service/
│   │   │   │   ├── ProductService.java         # Service interface
│   │   │   │   └── ProductServiceImpl.java     # Business logic
│   │   │   ├── repository/
│   │   │   │   └── ProductRepository.java      # JPA repository
│   │   │   ├── entity/
│   │   │   │   └── Product.java                # JPA entity
│   │   │   ├── dto/
│   │   │   │   ├── ProductRequestDto.java      # Inbound request body
│   │   │   │   └── ProductResponseDto.java     # Outbound response body
│   │   │   └── exception/
│   │   │       ├── ResourceNotFoundException.java
│   │   │       └── GlobalExceptionHandler.java # @RestControllerAdvice
│   │   └── resources/
│   │       └── application.yml
│   └── test/
│       └── java/com/tdg/productcrud/
│           ├── entity/ProductEntityTest.java
│           ├── service/ProductServiceImplTest.java
│           └── controller/ProductControllerTest.java
└── docs/
    └── enhancements/                           # 7 feature spec files
```

---

## Prerequisites

- Java 17+
- Maven 3.6+
- MySQL 8 running locally

---

## Setup

**1. Create the database**

```sql
CREATE DATABASE productdb;
```

**2. Configure credentials**

Open `src/main/resources/application.yml` and update:

```yaml
spring:
  datasource:
    username: your_mysql_username
    password: your_mysql_password
```

**3. Build**

```bash
mvn clean package
```

**4. Run**

```bash
mvn spring-boot:run
```

The application starts on `http://localhost:8080`.

> Hibernate will auto-create the `products` table on first run (`ddl-auto: update`).

---

## API Reference

Base URL: `http://localhost:8080/api/products`

### Create a product
```
POST /api/products
Content-Type: application/json

{
  "name": "Laptop",
  "category": "Electronics",
  "price": 999.99,
  "stockQuantity": 50
}
```
Response: `201 Created`

---

### Get all products
```
GET /api/products
```
Response: `200 OK` — array of products

---

### Get product by ID
```
GET /api/products/{id}
```
Response: `200 OK` or `404 Not Found`

---

### Update a product
```
PUT /api/products/{id}
Content-Type: application/json

{
  "name": "Laptop Pro",
  "category": "Electronics",
  "price": 1299.99,
  "stockQuantity": 30
}
```
Response: `200 OK` or `404 Not Found`

---

### Delete a product
```
DELETE /api/products/{id}
```
Response: `204 No Content` or `404 Not Found`

---

## Error Response Format

All errors return a consistent JSON shape:

```json
{
  "timestamp": "2026-06-29T10:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Product not found with id: 5"
}
```

---

## Running Tests

```bash
mvn test
```

16 tests across 3 test classes:

| Test Class | Count | What it tests |
|---|---|---|
| `ProductEntityTest` | 1 | `@PrePersist` sets `createdAt` |
| `ProductServiceImplTest` | 8 | All service methods + not-found cases |
| `ProductControllerTest` | 7 | HTTP status codes, request/response JSON, 404 handling |

---

## Data Model

**Table:** `products`

| Column | Type | Constraint |
|---|---|---|
| `id` | BIGINT | PK, auto-increment |
| `name` | VARCHAR | NOT NULL |
| `category` | VARCHAR | NOT NULL |
| `price` | DECIMAL | NOT NULL |
| `stock_quantity` | INT | NOT NULL |
| `created_at` | DATETIME | Set on insert, not updated |

---

## Planned Enhancements

Seven feature specs are ready under `docs/enhancements/` — pick one up to extend the app:

| # | File | Feature |
|---|---|---|
| 1 | `01-search-and-filtering.md` | Filter by category and price range (JPA Specifications) |
| 2 | `02-pagination-and-sorting.md` | Page and sort the product list |
| 3 | `03-jwt-security.md` | Protect endpoints with JWT auth |
| 4 | `04-swagger-openapi.md` | Interactive API docs at `/swagger-ui.html` |
| 5 | `05-global-exception-handling.md` | Richer error responses (validation, DB constraint errors) |
| 6 | `06-auditing.md` | Track `createdBy` / `lastModifiedBy` automatically |
| 7 | `07-input-validation.md` | Bean Validation on request DTOs |
