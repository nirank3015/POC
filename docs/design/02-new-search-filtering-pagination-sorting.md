# Technical Design: 02-new-search-filtering-pagination-sorting

---

## 1. Overview

This document outlines the technical design and implementation plan for the enhancement of the `GET /api/products` endpoint to support search, filtering, sorting, and pagination functionality. The enhancement aims to:

1. Enforce paginated responses to prevent unbounded data retrieval.
2. Allow users to request data in specific page sizes and orders.
3. Support combinable search, filtering, pagination, and sorting in a single API call.

This feature builds upon **Enhancement 01** by continuing to use `JpaSpecificationExecutor` for filtering and extends the `GET /api/products` response format to include pagination and sorting.

---

## 2. Scope

**In Scope**:
- Extend the `GET /api/products` endpoint to support:
  - Pagination using `page` and `size` query parameters.
  - Sorting by any product attribute with `sort` query parameters.
  - Combining pagination, sorting, and existing filtering options (`category`, `minPrice`, `maxPrice`).
- Modify the `ProductService` and `ProductController` to accommodate pagination and sorting.
- Update query logic in the `ProductRepository` to support combined filtering and pagination with sorting.
- Implement unit tests for the service and controller layers.
- Add integration tests to validate the functionality of the endpoint with all combinations of features.
  
**Out of Scope**:
- Introducing additional product filters apart from `category`, `minPrice`, and `maxPrice`.
- Changes to the structure of the database.
- Frontend/UI implementation for pagination and sorting.

---

## 3. API Design

### API Endpoint Definition
**HTTP Method**: `GET`  
**URI**: `/api/v1/products`  

#### Query Parameters
| Name         | Type                | Optional | Description                                           |
|--------------|---------------------|----------|-------------------------------------------------------|
| `category`   | String              | Yes      | Filters products by category name.                   |
| `minPrice`   | BigDecimal          | Yes      | Minimum price of products. (Filter)                  |
| `maxPrice`   | BigDecimal          | Yes      | Maximum price of products. (Filter)                  |
| `page`       | Integer             | Yes      | Page index (0-based). Default is `0`.                |
| `size`       | Integer             | Yes      | Number of records per page. Default is `20`.         |
| `sort`       | String (field,dir)  | Yes      | Field and direction for sorting (e.g., `price,asc`). |

#### Response Format
Response now uses a paginated wrapper object (`Page`) with details about pagination and a list of results.

##### Example Request
```
GET /api/v1/products?category=Electronics&maxPrice=1000&page=0&size=10&sort=price,desc
```

##### Example Response
```json
{
  "content": [
    { "id": 1, "name": "Laptop", "category": "Electronics", "price": 899.00 },
    { "id": 2, "name": "Smartphone", "category": "Electronics", "price": 699.00 }
  ],
  "totalElements": 25,
  "totalPages": 3,
  "number": 0,
  "size": 10
}
```

#### Status Codes
- **200 OK**: Successfully returns paginated and filtered product results.
- **400 BAD REQUEST**: Request contains invalid parameters (e.g., negative page or size values, invalid sort direction).
- **500 INTERNAL SERVER ERROR**: Unexpected server errors.

---

## 4. Data Model Changes

No changes are required to the existing data model or database schema. However, an **index** on the columns frequently used for filtering (`category`, `price`) and sorting can improve performance.

### Database Index Recommendations
Create and maintain indices for the following columns:
- `category` (e.g., `CREATE INDEX idx_category ON products (category);`)
- `price` (e.g., `CREATE INDEX idx_price ON products (price);`)

Maintain existing pagination and sorting capabilities provided by database query optimizations.

---

## 5. Service Layer Design

### `ProductService` Changes
The service layer will handle:
1. Translation of optional filtering and pagination parameters from the controller layer into a Spring Data `Specification` and `Pageable` object.
2. Combining `Specification` objects using `where` and `and` for filtering.
3. Forwarding the combined `Specification` and `Pageable` to `findAll` in the repository layer.

```java
public interface ProductService {
    Page<ProductResponseDto> searchProducts(
        String category,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Pageable pageable
    );
}
```

### `ProductServiceImpl` Implementation
Key logic:
- Build a `Specification` using `ProductSpecification` static methods.
- Combine filtering Specifications with `.and(...)`.
- Invoke `productRepository.findAll(combinedSpec, pageable)` and map the result to a `Page<ProductResponseDto>`.

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Override
    public Page<ProductResponseDto> searchProducts(String category, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        Specification<Product> spec = Specification.where(ProductSpecifications.hasCategory(category))
                                                    .and(ProductSpecifications.hasPriceGreaterThanOrEqual(minPrice))
                                                    .and(ProductSpecifications.hasPriceLessThanOrEqual(maxPrice));

        return productRepository.findAll(spec, pageable)
                                .map(product -> ProductMapper.toDto(product));
    }
}
```

---

## 6. Repository Layer Design

### `ProductRepository`
No new repository method is required as `JpaSpecificationExecutor` provides `findAll(Specification<T> spec, Pageable pageable)` by default.

### `ProductSpecification`
Update `ProductSpecification` to include additional static helpers for the Specification pattern:

```java
public class ProductSpecifications {
    public static Specification<Product> hasCategory(String category) {
        return (root, query, criteriaBuilder) -> {
            if (category == null) return null;
            return criteriaBuilder.equal(root.get("category"), category);
        };
    }

    public static Specification<Product> hasPriceGreaterThanOrEqual(BigDecimal minPrice) {
        return (root, query, criteriaBuilder) -> {
            if (minPrice == null) return null;
            return criteriaBuilder.greaterThanOrEqualTo(root.get("price"), minPrice);
        };
    }

    public static Specification<Product> hasPriceLessThanOrEqual(BigDecimal maxPrice) {
        return (root, query, criteriaBuilder) -> {
            if (maxPrice == null) return null;
            return criteriaBuilder.lessThanOrEqualTo(root.get("price"), maxPrice);
        };
    }
}
```

---

## 7. Security & Validation

1. **Input Validation**:
   - `@Validated` at the `ProductController` class level.
   - Valid values for `page` and `size` (non-negative numbers).
   - Validate proper sorting syntax in the `sort` parameter (e.g., `field,direction`).

2. **Authorization**: No specific restrictions required unless additional security rules are requested.

---

## 8. Error Handling

- **400 BAD REQUEST**:
  - Invalid query parameters (e.g., empty `category`, negative `page`/`size`, malformed `sort`).
  - Constraint violation exceptions for input values.

- **404 NOT FOUND**:
  - If no products match the provided filters.

- All exceptions will be handled by the `GlobalExceptionHandler` defined in the `@RestControllerAdvice`.

---

## 9. Testing Strategy

1. **Unit Tests**:
   - `ProductServiceTest`:
     - Test filtering by each parameter.
     - Test combination of filters.
     - Mock `productRepository.findAll` to validate filtering and pagination logic integration.
   - `ProductSpecificationTest`:
     - Unit test Specification building blocks.

2. **Integration Tests** (`@WebMvcTest`):
   - `ProductControllerTest`:
     - Validate that query parameters are correctly translated into service layer calls.
     - Ensure correct pagination/sorting behavior.
     - End-to-end cases for representative API calls.

3. **Edge Cases**:
   - Sort by non-existent fields.
   - Page numbers out of bounds.
   - Combination of query params — no results.
   - Default pagination behavior when `page`/`size` are omitted.
 
---

## 10. Open Questions

- Should the pagination fields (`page` and `size`) have a hard upper limit? For example, should `size` be capped at 100 results per page?
- Should rate-limiting mechanisms be applied to prevent abuse of the paginated endpoint?

This concludes the technical design document for the implementation of Enhancement 02 — Search, Filtering, Pagination & Sorting.