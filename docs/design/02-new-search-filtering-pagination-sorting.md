# Technical Design: 02-new-search-filtering-pagination-sorting

## 1. Overview
This enhancement introduces functionality to allow clients to retrieve product lists with search, filtering, pagination, and sorting capabilities in the existing `/api/products` REST endpoint. Users can filter products by `category`, `minPrice`, and `maxPrice` parameters, as well as specify pagination (`page`, `size`) and sorting (`sort`) configurations. The response will be paginated and include metadata such as total elements and total pages to prevent large payloads and improve performance for broader result sets.

The primary goals are:
1. Enhance the usability and performance of the product list API.
2. Provide clean and efficient filtering, pagination, and sorting using Spring Data JPA features.
3. Maintain backward compatibility with existing functionality.

---

## 2. Scope

### In Scope
- Enhance the `/api/products` endpoint with pagination and sorting capabilities.
- Modify the `ProductService` and `ProductRepository` to implement combined filtering, pagination, and sorting based on client-specified parameters.
- Modify the API response to include paginated metadata.
- Create proper integration and unit tests for the new functionality.

### Out of Scope
- Modifications to other endpoints.
- Comprehensive frontend/UI changes.

---

## 3. API Design

### Endpoint
```
GET /api/products
```

### Query Parameters
| Parameter     | Type       | Required | Description                                                                                      |
|---------------|------------|----------|--------------------------------------------------------------------------------------------------|
| category      | String     | No       | Filter products by category (case-sensitive exact matches).                                     |
| minPrice      | BigDecimal | No       | Minimum price filter (inclusive).                                                              |
| maxPrice      | BigDecimal | No       | Maximum price filter (inclusive).                                                              |
| page          | Integer    | No       | The zero-based page index to retrieve. Defaults to 0 if not provided.                          |
| size          | Integer    | No       | The number of items per page. Defaults to 20 if not provided.                                   |
| sort          | String     | No       | Sorting criteria in the format `property,[asc|desc]` (e.g., `price,asc` or `name,desc`).        |

### Example Requests

#### 1. Retrieve paginated results with default settings
```
GET /api/products
```
**Response:**
```json
{
  "content": [
    { "id": 1, "name": "Laptop", "category": "Electronics", "price": 899.00 },
    { "id": 2, "name": "Headphones", "category": "Electronics", "price": 150.00 }
  ],
  "totalElements": 47,
  "totalPages": 3,
  "number": 0,
  "size": 20
}
```

#### 2. Filter by category and paginate
```
GET /api/products?category=Electronics&page=0&size=5
```

#### 3. Sort by price in descending order and paginate
```
GET /api/products?page=0&size=10&sort=price,desc
```

### Status Codes
- **200 OK**: Successful response.
- **400 Bad Request**: Validation error on query parameters.
- **500 Internal Server Error**: Unhandled server-side error.

---

## 4. Data Model Changes
### JPA Entity Modifications
No changes are introduced to the `Product` JPA entity.

### Database Migration
No schema changes are required for this enhancement, so no Flyway migrations are necessary.

---

## 5. Service Layer Design
### Modified Methods in `ProductService` Interface
```java
public interface ProductService {
    Page<ProductResponse> searchProducts(String category, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);
}
```

### `ProductServiceImpl` Implementation
- The `ProductServiceImpl` method will:
  1. Create a `Specification<Product>` by combining filtering conditions (`ProductSpecification` utility methods).
  2. Use the `ProductRepository.findAll(Specification, Pageable)` method to fetch the data while considering both filtering conditions and pagination.
  3. Convert the `Page<Product>` returned by the repository into a `Page<ProductResponse>` DTO object for clean API response.

### Example Code
```java
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ModelMapper modelMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> searchProducts(String category, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        Specification<Product> spec = Specification.where(null);

        if (category != null && !category.isBlank()) {
            spec = spec.and(ProductSpecification.hasCategory(category));
        }
        if (minPrice != null) {
            spec = spec.and(ProductSpecification.hasPriceGreaterThanOrEqual(minPrice));
        }
        if (maxPrice != null) {
            spec = spec.and(ProductSpecification.hasPriceLessThanOrEqual(maxPrice));
        }

        return productRepository.findAll(spec, pageable)
                .map(product -> modelMapper.map(product, ProductResponse.class));
    }
}
```

---

## 6. Repository Layer
- **Modified `ProductRepository`:**
```java
@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
}
```

- **ProductSpecification Utilities:**
```java
public class ProductSpecification {

    public static Specification<Product> hasCategory(String category) {
        return (root, query, criteriaBuilder) -> 
            criteriaBuilder.equal(root.get("category"), category);
    }

    public static Specification<Product> hasPriceGreaterThanOrEqual(BigDecimal minPrice) {
        return (root, query, criteriaBuilder) -> 
            criteriaBuilder.greaterThanOrEqualTo(root.get("price"), minPrice);
    }

    public static Specification<Product> hasPriceLessThanOrEqual(BigDecimal maxPrice) {
        return (root, query, criteriaBuilder) -> 
            criteriaBuilder.lessThanOrEqualTo(root.get("price"), maxPrice);
    }
}
```

---

## 7. Security & Validation
- **Input Validation:**
  - `@PositiveOrZero` for `page` and `size` query parameters.
  - `@Positive` for `minPrice` and `maxPrice` query parameters, as prices cannot be negative.
  - Custom error message via `@RequestParam` annotation for invalid input.

- **Authorization:**
  - Endpoint access may require user roles or tokens, verified by existing Spring Security and JWT configurations.

---

## 8. Error Handling
- **Invalid Parameter Values:**
  ```json
  {
    "status": 400,
    "message": "Invalid value for parameter 'size'. Must be a positive number.",
    "data": null
  }
  ```
- **Resource Not Found:**
  Return `404` with a structured `ApiResponse` object.
- **Other Unhandled Exceptions:**
  Fallback to HTTP 500 in `GlobalExceptionHandler`.

---

## 9. Testing Strategy

### Unit Tests
- **Service Tests:**
  - Verify `searchProducts` correctly combines filtering parameters into a `Specification`.
  - Mock repository to verify correct invocation of `findAll(Specification, Pageable)`.
  - Test boundary cases like null or empty parameters.

- **Specification Tests:**
  - Unit test each static method in `ProductSpecification` using an in-memory H2 database and `@DataJpaTest`.

### Integration Tests
- **Controller Layer Tests:**
  - Use `@WebMvcTest` with a mocked service layer (`ProductService`).
  - Validate the expected output structure for basic, filtered, paginated, and sorted queries.
  - Test invalid parameter handling (e.g., `page=-1`, `size=-10`, `sort=unsupportedField,asc`).

---

## 10. Open Questions
1. Should `category` filtering be case-insensitive (e.g., `?category=electronics` matches `Electronics`)?
2. Should the backend enforce upper bounds for `size` (e.g., max 50 items per page)?
3. Should we provide default sorting support for multiple fields (e.g., `sort=price,asc&name,desc`)?