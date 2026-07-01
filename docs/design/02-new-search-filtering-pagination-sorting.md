# Technical Design: 02-new-search-filtering-pagination-sorting

---

## 1. Overview

This document describes the technical design approach for implementing the new feature: **Search, Filtering, Pagination, and Sorting** for the product list in the application.

The current system supports filtering products using category, minPrice, and maxPrice parameters by leveraging Spring Data JPA's Specification API. However, the resulting product list is unbounded, which could lead to performance issues in cases of large datasets. To address this, pagination and sorting capabilities will be added to the product listing API, allowing clients to retrieve results in smaller, page-based responses and select sorting preferences. This enhancement will reduce the load and improve API performance while providing more flexibility to the client.

---

## 2. Scope

### In Scope
1. Adding **pagination** and **sorting** to the `/api/products` endpoint.
2. Allowing clients to request specific pages of results with the `page` and `size` query parameters.
3. Enabling sorting by any product field using the `sort` query parameter in the format `{field},{direction}`, where direction is `asc` or `desc`.
4. Modifying the response to include total pages, total elements, and the current page.
5. Updating the service, controller, and repository layers to incorporate pagination and sorting.
6. Writing unit and integration tests to verify functionality.

### Out of Scope
1. Non-product-related filtering/pagination/sorting.
2. New entities, unrelated changes, or breaking existing functionality.
3. Authentication/authorization changes.

---

## 3. API Design

### Endpoint Details
**Method:** `GET`  
**Path:** `/api/products` 

**Query Parameters**  
| Parameter     | Type      | Optional | Description                                                                                  |
|---------------|-----------|----------|----------------------------------------------------------------------------------------------|
| `category`    | `String`  | Yes      | Filter products by the category name.                                                       |
| `minPrice`    | `BigDecimal` | Yes   | Minimum price filter, inclusive.                                                            |
| `maxPrice`    | `BigDecimal` | Yes   | Maximum price filter, inclusive.                                                            |
| `page`        | `Integer` | Yes      | The page number to retrieve (zero-based index). Default: `0`.                               |
| `size`        | `Integer` | Yes      | Number of results per page. Default: `20`.                                                  |
| `sort`        | `String`  | Yes      | Sorting criteria in the format `{field},{direction}` (e.g., `price,asc`). Default: `id,asc`.|

**Request/Response Examples**  
**Request (Example #1):**  
```
GET /api/products?category=Electronics&maxPrice=1000&page=1&size=5&sort=price,desc
```

**Response (Example):**
```json
{
  "content": [
    { "id": 4, "name": "Smartphone", "category": "Electronics", "price": 799.00 },
    { "id": 5, "name": "Laptop", "category": "Electronics", "price": 699.00 }
  ],
  "totalElements": 47,
  "totalPages": 10,
  "pageSize": 5,
  "currentPage": 1
}
```

| **HTTP Status Codes** | **Description**                                                                            |
|------------------------|--------------------------------------------------------------------------------------------|
| `200 OK`              | Products list successfully retrieved.                                                      |
| `400 BAD REQUEST`     | Invalid query parameters (e.g., negative page/size, invalid sort format).                   |
| `500 INTERNAL SERVER ERROR` | An unexpected error occurred on the server.                                         |

---

## 4. Data Model Changes

No changes to the database schema or JPA entities are needed for this enhancement. The existing `Product` entity and `products` table will be used as-is.

---

## 5. Service Layer Design

### Changes to `ProductService`
- Update the `searchProducts` method in the `ProductService` interface and its implementation `ProductServiceImpl` to include a `Pageable` parameter and return a `Page<ProductResponseDto>`:
```java
Page<ProductResponseDto> searchProducts(String category, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);
```

### Implementation Steps
1. **Input Validation**: Verify that the `page`, `size`, and `sort` parameters in the `Pageable` object are valid (non-negative, sort format is valid).
2. **Filter Logic**:
   - Use existing `ProductSpecification` with `Specification.where()` to apply filters for category, `minPrice`, and `maxPrice`.
3. **Pagination and Sorting**:
   - Combine the filtering `Specification` with the `Pageable` parameter directly in the repository query:
     ```java
     Page<Product> productPage = productRepository.findAll(spec, pageable);
     ```
4. **Result Mapping**:
   - Map the `Page<Product>` to `Page<ProductResponseDto>` using a utility method.

---

## 6. Repository Layer

### Existing Repository
The `ProductRepository` already extends `JpaRepository` and `JpaSpecificationExecutor`:
```java
@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {}
```

### Required Specifications
Update the `ProductSpecification` utility class to support flexible filters:

```java
public class ProductSpecification {

    public static Specification<Product> hasCategory(String category) {
        return (root, query, cb) -> 
            category == null ? null : cb.equal(root.get("category"), category);
    }

    public static Specification<Product> hasPriceGreaterThanOrEqual(BigDecimal minPrice) {
        return (root, query, cb) -> 
            minPrice == null ? null : cb.greaterThanOrEqualTo(root.get("price"), minPrice);
    }

    public static Specification<Product> hasPriceLessThanOrEqual(BigDecimal maxPrice) {
        return (root, query, cb) -> 
            maxPrice == null ? null : cb.lessThanOrEqualTo(root.get("price"), maxPrice);
    }
}
```

```java
// Example Filter Combination
Specification<Product> spec = Specification
    .where(ProductSpecification.hasCategory(category))
    .and(ProductSpecification.hasPriceGreaterThanOrEqual(minPrice))
    .and(ProductSpecification.hasPriceLessThanOrEqual(maxPrice));
```

---

## 7. Security & Validation

### Input Validation
1. **Pagination and Sorting Parameters:**
   - `page` and `size` must be non-negative.
   - Validate `size` to prevent unreasonably high values (e.g., set an upper limit of 100).
   - Validate `sort` using a custom implementation or helper utilities such as `Sort.by(...)`.

2. **Filter Values:**
   - Ensure valid `category`, `minPrice`, and `maxPrice` formats using `@RequestParam` annotations.
   - Example:
     ```java
     @RequestParam(required = false)
     @Min(value = 0, message = "Price must be greater than or equal to 0")
     BigDecimal minPrice
     ```

### Security
1. No authentication/authorization changes.
2. Ensure user input is sanitized to prevent SQL injection. By using parameterized queries through `JpaSpecificationExecutor`, this is handled.

---

## 8. Error Handling

| **Error**                      | **Exception**                | **HTTP Status** | **Message**                              |
|---------------------------------|------------------------------|-----------------|------------------------------------------|
| Invalid query parameters        | `MethodArgumentNotValidException` | 400            | "Invalid input values."                  |
| Filtered resource not found     | `ResourceNotFoundException`  | 404             | "No products matching the criteria."     |

`GlobalExceptionHandler` will be updated (if necessary) to map new exceptions to appropriate HTTP error responses.

---

## 9. Testing Strategy

### Unit Tests
1. **ProductSpecificationTest**
   - Test individual specifications (e.g., `hasCategory`, `hasPriceGreaterThanOrEqual`) using in-memory H2 and `@DataJpaTest`.
2. **ProductServiceTest**
   - Mock `ProductRepository` and ensure correct `Specification` and `Pageable` parameters are passed.
   - Validate behavior when combining filters, pagination, and sorting logic.

### Integration Tests
1. **ProductControllerTest**
   - Use `@WebMvcTest` and `MockMvc` for testing the `/api/products` endpoint.
   - Test combinations of parameters (e.g., filters + pagination + sorting).
   - Use parameterized tests to cover edge cases.

### Edge Cases
- Combination of all filters with paging and sorting.
- Empty result sets (valid inputs but no matching products).
- Boundary/illegal values for `page`, `size`, `minPrice`, and `maxPrice` parameters.

---

## 10. Open Questions

1. Should we enforce limits on the maximum value of `size` (e.g., 100) to prevent massive data requests?
2. Are there additional product fields that should be supported for sorting?
3. Should the API allow sorting by multiple fields (e.g., `?sort=price,asc,name,desc`)? Would need proper handling. 

--- 

This document outlines a comprehensive design plan for implementing the **Search, Filtering, Pagination, and Sorting** enhancement in a standardized and maintainable manner consistent with the overall project conventions.