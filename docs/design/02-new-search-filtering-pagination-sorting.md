# Technical Design: 02-new-search-filtering-pagination-sorting

## 1. Overview
This document outlines the design for enhancing the `GET /api/products` endpoint to support pagination and sorting functionality on top of the existing search and filtering capabilities. The goal is to prevent unbounded list responses by introducing page-based retrieval and enable clients to sort results by specific fields in a customizable order, while adhering to the established project conventions.

### Goals:
- Add pagination with configurable page size and number.
- Introduce sorting by specific fields (ascending/descending order).
- Ensure compatibility with existing search and filtering functionalities.
- Maintain adherence to existing project conventions and practices.

---

## 2. Scope

### In Scope:
1. Enhancing the `GET /api/products` endpoint with pagination and sorting.
2. Allowing clients to combine filters, pagination, and sorting in a single request.
3. Modifying service, repository, and controller layers to handle the new pagination and sorting parameters.
4. Unit and integration tests to validate the implementation.

### Out of Scope:
1. Adding additional product filters (e.g., date range, manufacturer).
2. Caching or performance optimization for large datasets (to be handled in a future implementation).
3. User role/permission-specific filtering.

---

## 3. API Design

### Endpoint:
- **Method:** `GET`
- **Path:** `/api/products`
- **Query Parameters:**
  | Parameter     | Type    | Description                                      | Default      |
  |---------------|---------|--------------------------------------------------|--------------|
  | `category`    | String  | Filter products by category                      | N/A          |
  | `minPrice`    | Decimal | Minimum product price (inclusive)                | N/A          |
  | `maxPrice`    | Decimal | Maximum product price (inclusive)                | N/A          |
  | `page`        | Integer | Page number (0-indexed)                          | 0            |
  | `size`        | Integer | Page size                                        | 20           |
  | `sort`        | String  | Sort field and direction, e.g., `price,asc`      | `id,asc`     |

### Response Format:
Responses are wrapped in a `Page` object provided by Spring Data to include metadata about pagination.

#### Example Request:
```http
GET /api/products?category=Electronics&minPrice=100&maxPrice=500&page=1&size=10&sort=price,desc
```

#### Example Response (`Content-Type: application/json`):
```json
{
  "content": [
    {
      "id": 1,
      "name": "Smartphone",
      "category": "Electronics",
      "price": 450.00
    },
    {
      "id": 2,
      "name": "Headphones",
      "category": "Electronics",
      "price": 120.00
    }
  ],
  "totalElements": 47,
  "totalPages": 5,
  "number": 1,
  "size": 10,
  "sort": [
    {
      "property": "price",
      "direction": "DESC"
    }
  ]
}
```

#### Response Metadata:
- `content`: Array of product details matching the query.
- `totalElements`: Total number of matching products.
- `totalPages`: Total pages available based on `size`.
- `number`: Current page number.
- `size`: Number of items per page.
- `sort`: Sorting metadata used in the query.

### Status Codes:
- `200 OK`: Request successful.
- `400 Bad Request`: Invalid query parameters (e.g., negative page/size values, invalid sort fields).
- `500 Internal Server Error`: Unexpected errors during processing.

---

## 4. Data Model Changes
### JPA Entity Changes:
No changes to the `Product` entity are required since no new fields or relations are needed for this enhancement.

### Flyway Migration:
No database schema or index changes are necessary for the implementation of this feature.

---

## 5. Service Layer Design
### Modified Service and Methods:
**`ProductService` (Interface):**
```java
Page<ProductResponseDto> searchProducts(String category, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);
```

**`ProductServiceImpl` (Implementation):**
- **Modified Logic:**
  1. Build `Specification<Product>` for filtering criteria using the existing `ProductSpecification` methods.
  2. Pass combined specifications and `Pageable` to the repository method `findAll(Specification<Product>, Pageable)`.
  3. Map the `Page<Product>` returned from the repository to a `Page<ProductResponseDto>` using a mapper utility.

---

## 6. Repository Layer
### Existing Repository Interface:
`ProductRepository` already extends `JpaRepository` and `JpaSpecificationExecutor`.

### Key Method:
```java
Page<Product> findAll(Specification<Product> spec, Pageable pageable);
```

### Specifications:
The existing `ProductSpecification` utility class will be reused. The individual specifications for `category`, `minPrice`, and `maxPrice` remain unchanged.

---

## 7. Security & Validation
### Input Validation:
- Use `@PageableDefault(page = 0, size = 20, sort = "id") Pageable pageable` in the controller method to enforce default values.
- Validate numeric fields like `minPrice` and `maxPrice` using:
  ```java
  @Min(0)
  private BigDecimal minPrice;
  @Min(0)
  private BigDecimal maxPrice;
  ```

### Authorization:
No new authorization-specific changes are required. Existing authentication via JWT and role-based authorization will remain in place.

---

## 8. Error Handling
### Expected Exceptions:
- **Validation exceptions:** Invalid query parameters (e.g., non-numeric price or negative page/size).
  - Mapped to `400 Bad Request` by `GlobalExceptionHandler`.
- **ResourceNotFoundException:** Thrown if attempting to access a non-existent page.
  - Mapped to `404 Not Found`.

### Error Codes:
For consistency with existing project conventions, no custom error codes are required.

---

## 9. Testing Strategy
### Unit Tests:
1. **Service Layer**
   - Test filtering combinations with mock repository calls.
   - Test pagination correctness (page size, current page).
   - Test sorting functionality based on all supported fields.

2. **Specification Tests**
   - Verify that `ProductSpecification` methods return the correct predicates.
   - Use `@DataJpaTest` with an embedded H2 database for precise filtering tests.

### Integration Tests:
- **Controller Layer**
  - Test `/api/products` endpoint with different combinations of filters, pagination, and sorting.
  - Verify response metadata (e.g., `totalElements`, `totalPages`, `sort`).
  - Use `MockMvc` to simulate real requests and mock service layer dependencies.
- **Edge Cases**
  - Empty results (verify `totalElements = 0` and `content = []`).
  - Invalid parameters: negative `page`/`size`, invalid `sort` fields.

---

## 10. Open Questions
1. Should there be a hard cap on the `size` parameter to prevent exceedingly large requests (e.g., a max size of 100)?
2. Should additional validation mechanisms (e.g., whitelist for allowed sort fields) be implemented?
3. Is there a need to cache frequently accessed paginated queries for high-performance use cases? If yes, what caching mechanism could be used (e.g., Redis)?

--- 

This design aligns with the project conventions and provides a robust foundation for implementing pagination and sorting functionality in a maintainable, extensible way. It ensures consistent behavior with the previously implemented search and filtering logic while addressing performance concerns associated with unbounded result sets.