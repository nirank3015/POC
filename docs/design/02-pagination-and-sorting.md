# Technical Design: 02-pagination-and-sorting

## 1. Overview

The purpose of this feature is to enhance the `GET /api/products` endpoint to support pagination and sorting logic. Currently, the API returns an unbounded list of products, which is inefficient and can lead to performance issues for large datasets. This enhancement introduces page-based retrieval with configurable sorting, ensuring API responses are more scalable and flexible for client applications.

---

## 2. Scope

### In Scope
- Pagination support for `GET /api/products` endpoint.
- Sorting support for any product field (e.g., `price`, `name`).
- Default behavior: Return page 0, with a maximum size of 20 items, sorted by `id` in ascending order.
- Updating `ProductController`, `ProductService`, and `ProductRepository` to support pagination and sorting.
- Unit and integration test coverage for the new functionality.

### Out of Scope
- Filtering products based on attributes (e.g., by category or price range).
- UI or frontend changes.
- Caching or any optimizations beyond basic pagination and sorting.

---

## 3. API Design

### Endpoint
#### `GET /api/products`

- **Method**: `GET`
- **Query Parameters**:
  - `page` (optional): The page index (0-based). Default = 0.
  - `size` (optional): The number of items per page. Default = 20.
  - `sort` (optional): The field and direction (e.g., `sort=price,asc` or `sort=name,desc`). Default = `id,asc`.

- **Request Example**:
  - `/api/products?page=1&size=5&sort=price,desc`

- **Response Example**:
  ```json
  {
    "status": "success",
    "message": "Products fetched successfully",
    "data": {
      "content": [
        {
          "id": 1,
          "name": "Product A",
          "price": 10.50
        },
        {
          "id": 2,
          "name": "Product B",
          "price": 15.00
        }
      ],
      "number": 1,
      "size": 5,
      "totalElements": 50,
      "totalPages": 10,
      "sort": [
        {
          "property": "price",
          "direction": "DESC"
        }
      ]
    }
  }
  ```

- **Response Fields**:
  - `content`: List of `ProductResponseDto` objects containing the product data.
  - `number`: Current page index.
  - `size`: Number of items per page.
  - `totalElements`: Total number of items in the database.
  - `totalPages`: Total pages available.
  - `sort`: Sorting metadata (property and direction).

- **Status Codes**:
  - `200 OK`: When the request is successful.
  - `400 Bad Request`: When query parameters are invalid.

---

## 4. Data Model Changes

### JPA Entity
No changes are required for the existing `Product` entity.

---

## 5. Service Layer Design

### Updated Method
#### `ProductService`
```java
public interface ProductService {
    Page<ProductResponseDto> getAllProducts(Pageable pageable);
}
```

#### `ProductServiceImpl`
```java
@Service
@Transactional
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponseDto> getAllProducts(Pageable pageable) {
        Page<Product> productPage = productRepository.findAll(pageable);
        return productPage.map(product -> mapToDto(product));
    }

    private ProductResponseDto mapToDto(Product product) {
        return ProductResponseDto.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .build();
    }
}
```

---

## 6. Repository Layer

### `ProductRepository`
The `ProductRepository` interface already extends `JpaRepository`, which includes pagination and sorting functionality. No additional methods are required:

```java
@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {}
```

---

## 7. Security & Validation

### Input Validation
- Query parameters for `page`, `size`, and `sort` are auto-validated by Spring's `Pageable` resolver.
- No custom validation logic is required.

### Authorization
- If role-based restrictions are required, use `@PreAuthorize` annotations on the controller method.
- Example:
  ```java
  @PreAuthorize("hasRole('USER')")
  ```

---

## 8. Error Handling

### Exceptions
- **Invalid Page Size or Sort Parameter**:
  - Exception: `MethodArgumentNotValidException` (already handled by Spring).
  - Response: `400 Bad Request`.

### Standard Error Response
All errors are wrapped in a standard `ApiResponse` as per the existing convention:
```json
{
  "status": "error",
  "message": "Invalid page index",
  "data": null
}
```

---

## 9. Testing Strategy

### Unit Tests
#### `ProductServiceTest`
1. **Test Case:** `shouldReturnPaginatedProducts_whenPageableIsValid`
   - Mock `productRepository.findAll(Pageable)` to return a sample `Page<Product>` object.
   - Validate that the service correctly maps entities to DTOs.
2. **Test Case:** `shouldReturnEmptyPage_whenNoProductsExist`
   - Mock repository to return an empty page.
   - Assert an empty list in the response.

### Integration Tests
#### `ProductControllerTest`
1. **Test Case:** `shouldReturnProducts_whenValidPageableProvided`
   - Mock `ProductService.getAllProducts(Pageable)` to return a sample `Page<ProductResponseDto>`.
   - Validate that the API response matches the expected structure.
2. **Test Case:** `shouldFallbackToDefaults_whenNoQueryParamsProvided`
   - Ensure `GET /api/products` uses defaults: page 0, size 20, sorted by `id,asc`.
3. **Test Case:** `shouldReturnBadRequest_whenInvalidSortParameter`
   - Simulate passing an invalid `sort` field and ensure the API returns a `400 Bad Request`.

### Edge Cases
1. **Negative Paging Values**:
   - Test with `page=-1` or `size=-5` and ensure proper error handling.
2. **Empty Database**:
   - Verify response when no products exist.
3. **Large Page Request**:
   - Test with excessively large `size` to verify performance and limits.

---

## 10. Open Questions

1. **Maximum Allowed Page Size**:
   - Should the system enforce a maximum page size limit (e.g., 100)?
2. **Default Sort Behavior for Complex Fields**:
   - Should we disallow sorting by fields like `description`, which may not provide meaningful order?

--- 

This document adheres to the project conventions and should serve as a technical blueprint to implement pagination and sorting for the product API.