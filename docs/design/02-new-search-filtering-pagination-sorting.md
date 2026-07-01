# Technical Design: 02-new-search-filtering-pagination-sorting

---

## 1. Overview

The purpose of this technical design document is to detail the implementation of a new functionality that enhances the product search endpoint's capabilities by adding pagination and sorting support. This enhancement will improve the scalability and flexibility of the product listing service, allowing clients to retrieve paginated results and sort them by any field in ascending or descending order.

### Goals
1. Prevent unbounded list responses by introducing pagination for the product list endpoint.
2. Allow clients to specify sorting behavior via query parameters (e.g., sorting by price, name, etc.).
3. Ensure that all query parameters for filtering, pagination, and sorting are combinable into a single, manageable request.

---

## 2. Scope

### In Scope
- Modifications to the existing `/api/products` endpoint to:
  - Support for **pagination** using `page` and `size` query parameters.
  - Support for **sorting** using `sort` query parameter, enabling sorting by any field present in the `Product` entity.
  - Combine pagination and sorting with filtering options (`category`, `minPrice`, `maxPrice`) from **Enhancement 01**.
- Updating the service and repository layers to support the new functionality.
- Unit tests for service implementation.
- Integration tests for the controller using `@WebMvcTest`.

### Out of Scope
- New product-related endpoints: the existing `GET /api/products` will be enhanced.
- UI-related changes/implementation.
- Advanced search features such as full-text search.

---

## 3. API Design

### Endpoint
#### GET /api/products
This endpoint supports filtering, pagination, and sorting, offering significant flexibility for clients.

#### Query Parameters
| Parameter        | Required | Type    | Description                                                                                      | Example                          |
|------------------|----------|---------|--------------------------------------------------------------------------------------------------|----------------------------------|
| `category`       | No       | String  | (Optional) Filter products by a specific category.                                               | `category=Electronics`           |
| `minPrice`       | No       | Decimal | (Optional) Filter products with a price greater than or equal to the specified value.            | `minPrice=100`                   |
| `maxPrice`       | No       | Decimal | (Optional) Filter products with a price less than or equal to the specified value.               | `maxPrice=500`                   |
| `page`           | No       | Integer | (Optional) Index of the result page (zero-based). Defaults to `0` (first page).                 | `page=1`                         |
| `size`           | No       | Integer | (Optional) Number of results per page. Defaults to `20`.                                         | `size=10`                        |
| `sort`           | No       | String  | (Optional) Specifies sorting criteria in the format `{property},{direction}` (`asc` or `desc`).  | `sort=price,asc` or `sort=id,des`|

#### Default Behavior
- Pagination defaults to `page=0` and `size=20`.
- Sorting defaults to `sort=id,asc`.

### Example Request/Response

#### Example 1: Request Without Parameters
Request:
```
GET /api/products
```
Response:
```json
{
  "content": [
    {
      "id": 1,
      "name": "Product A",
      "category": "Electronics",
      "price": 100.0
    },
    ...
  ],
  "totalElements": 100,
  "totalPages": 5,
  "number": 0,
  "size": 20
}
```

#### Example 2: Request With Filters, Pagination, and Sort
Request:
```
GET /api/products?category=Electronics&minPrice=500&page=1&size=5&sort=price,desc
```
Response:
```json
{
  "content": [
    {
      "id": 25,
      "name": "Smartphone B",
      "category": "Electronics",
      "price": 900.0
    },
    ...
  ],
  "totalElements": 25,
  "totalPages": 5,
  "number": 1,
  "size": 5
}
```

### Status Codes
- **200 OK**: Success.
- **400 Bad Request**: Invalid query parameters.
- **500 Internal Server Error**: Unexpected internal server error.

---

## 4. Data Model Changes

No changes to the `Product` entity or database schema are required for this enhancement, as pagination and sorting are handled dynamically in the query level, leveraging existing fields.

---

## 5. Service Layer Design

### Changes to `ProductService`
- Modify `searchProducts` in `ProductService` to accept an additional `Pageable` argument and change the return signature to `Page<ProductResponseDto>`:
```java
Page<ProductResponseDto> searchProducts(String category, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);
```

- Implementation of the method in `ProductServiceImpl`:

```java
@Service
@RequiredArgsConstructor
@Transactional
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponseDto> searchProducts(String category, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        Specification<Product> spec = Specification.where(ProductSpecification.hasCategory(category))
            .and(ProductSpecification.hasPriceGreaterThanOrEqual(minPrice))
            .and(ProductSpecification.hasPriceLessThanOrEqual(maxPrice));

        return productRepository.findAll(spec, pageable)
                .map(product -> toDto(product));  // Mapping Product entity to ProductResponseDto
    }
    
    private ProductResponseDto toDto(Product product) {
        return ProductResponseDto.builder()
                .id(product.getId())
                .name(product.getName())
                .category(product.getCategory())
                .price(product.getPrice())
                .build();
    }
}
```

---

## 6. Repository Layer

No new repository methods are required since `findAll(Specification<T> spec, Pageable pageable)` is already provided by the `JpaSpecificationExecutor` interface.

However, ensure the `ProductRepository` extends `JpaSpecificationExecutor<Product>`:
```java
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
}
```

---

## 7. Security & Validation

### Input Validation
- Each query parameter will be validated for type and range:
  - `page` and `size` should be integers ≥ 0.
  - `minPrice` and `maxPrice` should be positive decimals.
- Use `@Validated` at the controller level and validate individual query parameters as needed.

### Authorization
No additional authorization or role-specific validation is required for this enhancement. Any authenticated user may access the product list endpoint.

---

## 8. Error Handling

- Invalid query parameter values will result in `400 Bad Request`. Use reasonable custom validation exceptions and integration with the existing `GlobalExceptionHandler`:
```java
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidPaginationParameterException extends RuntimeException {
    public InvalidPaginationParameterException(String message) {
        super(message);
    }
}
```
- Map this exception in `GlobalExceptionHandler` to return a standardized error response.

---

## 9. Testing Strategy

### Unit Tests
1. **ProductServiceTest**:
   - Test cases for `searchProducts` to validate different combinations of filters, pagination, and sorting.
   - Mock the `findAll(Specification, Pageable)` method in `ProductRepository`.

2. **ProductSpecificationTest**:
   - Verify that each individual Specification (`hasCategory`, `hasPriceGreaterThanOrEqual`, `hasPriceLessThanOrEqual`) generates the correct SQL or HQL fragment using `JpaSpecificationExecutor` tests.

### Integration Tests
1. **ProductControllerTest**:
   - Write tests ensuring that responses match the API contract for all possible combinations of query parameters.
   - Mock `ProductService` and verify the request parameter-to-service call mapping.

2. **End-to-End Test** (Optional):
   - If the infrastructure is available, test the full integration of the controller, service, repository, and data layers using an embedded H2 database.

### Edge Cases to Cover
- Missing parameters (default pagination and sorting behavior).
- Invalid values (e.g., negative size, unknown category).
- Combinations of filters, pagination, and sorting.

---

## 10. Open Questions

1. Which fields are explicitly allowed to be sorted? Should unverified properties result in an error, or should the request use the default sort?
   - **Suggestion:** Allow sorting only by existing `Product` entity fields and return a `400 Bad Request` for any unspecified sort fields.

2. What should happen if `minPrice` > `maxPrice`? Should it return a `400 Bad Request`, or simply be ignored?
   - **Suggestion:** Return `400 Bad Request` with an appropriate error message.