# Technical Design: 02-new-search-filtering-pagination-sorting

## 1. Overview
Enhancement 02 introduces pagination and sorting capabilities to the `GET /api/products` endpoint, supplementing the existing search and filtering functionality (category, minPrice, maxPrice). This ensures manageable response payloads, minimizes server load for large datasets, and provides clients with flexibility in data consumption.

The endpoint will support the following query parameters:
- Pagination parameters: `page` (default `0`) and `size` (default `20`)
- Sorting: `sort` (default `id,asc`; custom fields e.g., `price,desc`).

This update will adhere to the existing Spring Boot conventions established in the project, such as using `JpaSpecificationExecutor` for filtering and `Pageable` for pagination/sorting.

## 2. Scope

**In-Scope**
- Introduce pagination and sorting for the `GET /api/products` endpoint.
- Update the response of this endpoint to a paginated format.
- Modify the service and repository layers to support combining specifications with pagination and sort criteria.
- Unit test and integration test the new implementation.
- Ensure strict alignment with project standards and existing functionality.

**Out of Scope**
- Modifications to existing filtering functionalities, except as necessary to ensure proper integration.
- Backend performance optimizations (e.g., query execution time).
- Feature extensions like full-text search or advanced search operators.

---

## 3. API Design

### Endpoint: Product Listing with Filter, Pagination, and Sorting
- **HTTP Method:** `GET`
- **Path:** `/api/products`
- **Query Parameters:**
  - **`category`** *(optional)*: Filter for products by category.
  - **`minPrice`** *(optional)*: Filter for products with prices above the specified value.
  - **`maxPrice`** *(optional)*: Filter for products with prices below the specified value.
  - **`page`** *(optional)*: The zero-based page number to retrieve (default: `0`).
  - **`size`** *(optional)*: The number of items per page (default: `20`).
  - **`sort`** *(optional)*: Sort field and direction (e.g., `price,asc`). Defaults to `id,asc`.

#### Request Examples
```http
GET /api/products?page=0&size=10&sort=price,desc&category=Electronics&maxPrice=1000
```

#### Response Body Examples
**For a successful request (`200 OK`):**
```json
{
  "content": [
    { "id": 1, "name": "Laptop", "category": "Electronics", "price": 899.00 },
    { "id": 2, "name": "Headphones", "category": "Electronics", "price": 199.99 }
  ],
  "totalElements": 47,
  "totalPages": 5,
  "number": 0,
  "size": 10
}
```

**For an invalid request (`400 Bad Request`):**
```json
{
  "status": "error",
  "message": "Invalid page parameter. Page number cannot be negative."
}
```

---

## 4. Data Model Changes

No changes to the existing `Product` entity are needed, nor are database schema changes required, as pagination/sorting utilize the current data model.

---

## 5. Service Layer Design

### `ProductService` Interface
```java
public interface ProductService {
    Page<ProductResponse> searchProducts(String category, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);
}
```

### `ProductServiceImpl` Implementation
Updates:
- Modify the `searchProducts` method to accept `Pageable` and return `Page<ProductResponse>`.
- Use `JpaSpecificationExecutor` to combine `Specification<Product>` with pagination and sorting criteria.

**Updated `searchProducts` Code (Partial)**
```java
@Override
@Transactional(readOnly = true)
public Page<ProductResponse> searchProducts(String category, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
    Specification<Product> spec = Specification.where(ProductSpecification.hasCategory(category))
                                                .and(ProductSpecification.hasPriceGreaterThanOrEqual(minPrice))
                                                .and(ProductSpecification.hasPriceLessThanOrEqual(maxPrice));
    Page<Product> products = productRepository.findAll(spec, pageable);
    return products.map(this::convertToResponse);
}
```

---

## 6. Repository Layer

### `ProductRepository`
No changes are required to the existing `ProductRepository` as it already:
1. Extends `JpaRepository`, which provides paging and sorting support.
2. Implements `JpaSpecificationExecutor<Product>`, enabling dynamic filtering with `Specification`.

### `ProductSpecification`
The existing `ProductSpecification` class already provides filtering logic. This will remain unchanged:
- `hasCategory(String category)`
- `hasPriceGreaterThanOrEqual(BigDecimal min)`
- `hasPriceLessThanOrEqual(BigDecimal max)`

---

## 7. Security & Validation

### Input Validation
- `category`, `minPrice`, and `maxPrice` are optional but validated for type correctness:
  - Example: negative values for `minPrice` or `maxPrice` will be invalid.
- Query parameters (e.g., `page`, `size`, `sort`) are validated implicitly via Spring's `@PageableDefault`.

### Authorization
- No additional role/authorization changes are required for this enhancement.

---

## 8. Error Handling

### Expected Exceptions
- **`ConstraintViolationException`**: Thrown for invalid query parameters (e.g., negative `size`).
- **`IllegalArgumentException`**: Thrown for invalid `sort` field values.

### Global Exception Handling
- Handled by the `GlobalExceptionHandler`:
  - `400` for validation errors.
  - `500` for unexpected exceptions.

Example Error Response:
```json
{
  "status": "error",
  "message": "Invalid sort parameter. Expected format: {field,asc|desc}."
}
```

---

## 9. Testing Strategy

### Unit Tests
- **`ProductServiceImplTest`**
  - `shouldReturnPagedAndSortedProducts_whenValidInputProvided()`
  - `shouldDefaultToFirstPageAndSortedById_whenNoPaginationOrSortingProvided()`
  - Mock `productRepository.findAll(any(Specification.class), any(Pageable.class))`.

- **`ProductSpecificationTest`**
  - Validate individual specifications, e.g., `hasCategory()`, `hasPriceGreaterThanOrEqual()`.

### Integration Tests
- **`ProductControllerTest`**
  - Validate combined filters, pagination, and sorting:
    - `shouldReturnCorrectResults_whenValidFiltersWithPagingAndSortingProvided()`
    - Mock responses from `ProductService`.

---

## 10. Open Questions
1. Should we allow sorting on multiple fields (e.g., `sort=price,asc&name,desc`)? If yes, how do we handle validation for invalid fields?
2. Is there a specific maximum for the `size` parameter to prevent clients from requesting excessively large pages?

