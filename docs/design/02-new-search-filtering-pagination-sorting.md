# Technical Design: 02-new-search-filtering-pagination-sorting

---

## 1. Overview

This design document introduces an enhancement to the existing product listing and filtering functionality in the application. The current API already supports search and filtering by `category`, `minPrice`, and `maxPrice`. This enhancement will introduce pagination to limit the size of the result set, as well as support for configurable sorting by different fields (e.g., price, name, or other product attributes in ascending or descending order). The goal is to optimize API performance and user experience by preventing unbounded responses while enabling flexible data retrieval options.

---

## 2. Scope

### In Scope
- Support for pagination and sorting in the `/api/products` endpoint.
- Integration of pagination and sorting with existing filtering functionality.
- Modifications to the `ProductService` and `ProductController` to support new query parameters `page`, `size`, and `sort`.
- Use of Spring Data JPA `Specification` combined with `Pageable` to create a dynamic and scalable query mechanism.
- Unit and integration testing for the added pagination and sorting functionalities.

### Out of Scope
- Modifications to the existing database schema or new database tables/columns.
- Full-text or fuzzy search (e.g., searching products by a name pattern).
- Enhancements to other parts of the application beyond this specific endpoint.

---

## 3. API Design

### Endpoint

`GET /api/products`

#### Query Parameters
| Parameter    | Type    | Description                                                                                                         | Optional | Default   |
|--------------|---------|---------------------------------------------------------------------------------------------------------------------|----------|-----------|
| `category`   | String  | Filter products by category name.                                                                                   | Yes      | -         |
| `minPrice`   | Decimal | Filter products with prices greater than or equal to this value.                                                    | Yes      | -         |
| `maxPrice`   | Decimal | Filter products with prices less than or equal to this value.                                                       | Yes      | -         |
| `page`       | Integer | The page number to retrieve (0-indexed, i.e., the first page is page 0).                                            | Yes      | 0         |
| `size`       | Integer | The number of products per page.                                                                                    | Yes      | 20        |
| `sort`       | String  | Sorting criteria in the format `field,order` where `order` is either `asc` or `desc` (e.g., `price,desc`).          | Yes      | `id,asc`  |

#### Request Example
```
GET /api/products?category=Electronics&minPrice=100&maxPrice=1000&page=0&size=10&sort=price,asc
```

#### Response Fields
| Field          | Type            | Description                                                 |
|----------------|-----------------|-------------------------------------------------------------|
| `content`      | List<Product>   | The list of products for the requested page.                |
| `totalElements`| Long            | Total number of products matching the filter criteria.      |
| `totalPages`   | Integer         | Total number of pages available.                           |
| `number`       | Integer         | Page number currently being returned. (0-indexed)          |
| `size`         | Integer         | Page size, or the number of products per page.             |

#### Response Example
```json
{
  "content": [
    { "id": 1, "name": "Laptop", "category": "Electronics", "price": 899.00 },
    { "id": 2, "name": "Smartphone", "category": "Electronics", "price": 599.00 }
  ],
  "totalElements": 47,
  "totalPages": 5,
  "number": 0,
  "size": 10
}
```

#### Status Codes
- `200 OK` — Successful response.
- `400 Bad Request` — Invalid query parameters (`page`, `size`, `sort`, etc.).
- `500 Internal Server Error` — Unexpected errors.

---

## 4. Data Model Changes

No changes to the existing database structure or `Product` entity are required. However, new indexes should be considered on commonly queried fields (`category`, `price`) to optimize database queries.

#### Example of database changes:
```sql
CREATE INDEX idx_product_category ON products (category);
CREATE INDEX idx_product_price ON products (price);
```

---

## 5. Service Layer Design

### Changes to `ProductService`
The existing `searchProducts` method in `ProductService` will be updated to support pagination and sorting. The method signature will be updated as follows:

```java
Page<ProductResponse> searchProducts(String category, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);
```

#### Implementation Notes:
- Extend existing logic to accept a `Pageable` object.
- Use the combination of `Specification` and `Pageable` to query the repository.
- Map the `Page<Product>` to `Page<ProductResponse>`.

#### Updated Method Implementation:
```java
@Override
@Transactional(readOnly = true)
public Page<ProductResponse> searchProducts(String category, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
    Specification<Product> spec = Specification.where(null);
    if (category != null) {
        spec = spec.and(ProductSpecification.hasCategory(category));
    }
    if (minPrice != null) {
        spec = spec.and(ProductSpecification.hasPriceGreaterThanOrEqual(minPrice));
    }
    if (maxPrice != null) {
        spec = spec.and(ProductSpecification.hasPriceLessThanOrEqual(maxPrice));
    }

    Page<Product> productsPage = productRepository.findAll(spec, pageable);

    return productsPage.map(product -> productMapper.toProductResponse(product));
}
```

---

## 6. Repository Layer

### No Changes to Repository Interfaces
The existing `ProductRepository` already extends `JpaRepository` and `JpaSpecificationExecutor`, which support dynamic queries and paging out of the box. The following call will retrieve paginated, sorted results:

```java
productRepository.findAll(spec, pageable);
```

#### Updated Query Execution Process:
1. Combine filtering criteria using `Specification`.
2. Pass the combined `Specification` and `Pageable` to the repository.
3. The database will execute a single optimized query for filtering, sorting, and pagination.

---

## 7. Security & Validation

### Input Validation
- Validation will ensure:
  - `page` is non-negative.
  - `size` is within an acceptable range (e.g., 1-100).
  - `sort` is in the correct format (e.g., `field,asc` or `field,desc`).

#### Controller Example
```java
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Validated
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getAllProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @PageableDefault(size = 20, sort = "id") Pageable pageable
    ) {
        Page<ProductResponse> response = productService.searchProducts(category, minPrice, maxPrice, pageable);
        return ResponseEntity.ok(new ApiResponse<>("Products fetched successfully", response));
    }
}
```

### Authorization
No additional role-based access control (RBAC) is required as this is a public API.

---

## 8. Error Handling

### Expected Error Scenarios
1. **Invalid Query Parameters**: 
   - Invalid `page`, `size`, or `sort` format → `400 Bad Request`.

   Example:
   ```json
   {
     "status": "BAD_REQUEST",
     "message": "Invalid sort parameter. Expected format: 'field,asc/desc'.",
     "data": null
   }
   ```

2. **Service or Backend Errors**: 
   - If an uncaught exception occurs, the `GlobalExceptionHandler` will return an HTTP 500 error.

---

## 9. Testing Strategy

### Unit Tests
**Service Layer Tests:**
- Verify `searchProducts` returns correct filtered, paginated, and sorted results.
- Edge cases:
  1. Filters return no matching results.
  2. Pagination inputs are out of range.
  3. Sort input is invalid.

### Integration Tests
**Controller Tests:**
- Validate:
  - Correct transformation of query parameters into `Pageable`.
  - Response structure and pagination metadata.
- Coverage:
  - Filtering combinations: `{category}`, `{minPrice, maxPrice}`.
  - Paging combinations: `{page, size}` within range, out of range.
  - Sorting combinations: `{sort=field,asc/desc}`, invalid sorting.

---

## 10. Open Questions

1. Is there a maximum allowable value for the `size` parameter to prevent excessive responses? (Proposal: Limit `size` to 100.)
2. Which product fields should be allowed for sorting? Should all fields be sortable, or only specific ones (e.g., `id`, `price`, `name`)?
3. Are there any additional constraints on page-based retrieval (e.g., maximum page number)?

---