# Technical Design: 02-pagination-and-sorting

## 1. Overview

The goal of this enhancement is to introduce pagination and sorting capabilities to the `GET /api/products` endpoint. This prevents unbounded list responses from overwhelming the client or server and provides clients with flexibility to retrieve specific pages of data and sort them by supported fields. By implementing this feature, responses will conform to a pageable format that includes metadata such as total elements, total pages, and the current page index.

Default behavior for this endpoint will be:
- Page size = 20
- Page index = 0 (first page)
- Sort field = `id` in ascending order

Clients can customize these parameters via query string.

---

## 2. Scope

### In Scope
- Adding pagination (`page` and `size`) and sorting (`sort`) parameters to the `GET /api/products` endpoint.
- Modifying service and controller logic to support pagination and sorting using Spring Data's `Pageable` abstraction.
- Returning paginated responses wrapped in a `Page<ProductResponseDto>` object.
- Unit and integration tests for controller and service logic.

### Out of Scope
- Changes to the underlying `Product` entity or database schema.
- Modifying unrelated endpoints or service methods.
- Implementing authorization (assumed to use existing Spring Security configuration).
- Advanced search or filtering capabilities (e.g., name contains "X").

---

## 3. API Design

### Endpoint

**HTTP Method:** `GET`  
**Path:** `/api/products`

#### Query Parameters
| Parameter | Type     | Optional? | Description                                                    | Default   |
|-----------|----------|-----------|----------------------------------------------------------------|-----------|
| `page`    | `int`    | Yes       | Zero-based page index                                          | `0`       |
| `size`    | `int`    | Yes       | Number of elements per page                                    | `20`      |
| `sort`    | `String` | Yes       | Sorting criteria in the format `{field},{direction}` (asc/desc)| `id,asc`  |

### Request Example
```http
GET /api/products?page=0&size=2&sort=price,desc
```

### Response Example
#### Success (200 OK)
```json
{
  "status": "success",
  "message": "Products retrieved successfully",
  "data": {
    "content": [
      {
        "id": 1,
        "name": "Product A",
        "price": 100.0,
        "description": "Description of Product A",
        "createdAt": "2023-01-01T12:00:00Z",
        "updatedAt": "2023-01-02T12:00:00Z"
      },
      {
        "id": 2,
        "name": "Product B",
        "price": 90.0,
        "description": "Description of Product B",
        "createdAt": "2023-01-01T12:00:00Z",
        "updatedAt": "2023-01-02T12:30:00Z"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 2,
      "offset": 0,
      "paged": true
    },
    "totalPages": 5,
    "totalElements": 10,
    "last": false,
    "first": true,
    "sort": {
      "empty": false,
      "sorted": true,
      "unsorted": false
    }
  }
}
```

#### Error (400 Bad Request)
```json
{
  "status": "error",
  "message": "Invalid sort parameter: 'unsupportedField'",
  "data": null
}
```

---

## 4. Data Model Changes

No changes are required to the existing `Product` entity or database schema.

---

## 5. Service Layer Design

### Modified Service
**Service Interface:** `ProductService`  
**Method Signature:**
```java
Page<ProductResponseDto> getAllProducts(Pageable pageable);
```

**Service Implementation:** `ProductServiceImpl`  
**Modified Logic:**
- Accept the `Pageable` object as a parameter.
- Use the repository's built-in `findAll(Pageable pageable)` method to fetch paginated results.
- Map `Page<Product>` to `Page<ProductResponseDto>` using a DTO mapper.

Example code:
```java
@Override
@Transactional(readOnly = true)
public Page<ProductResponseDto> getAllProducts(Pageable pageable) {
    Page<Product> products = productRepository.findAll(pageable);
    return products.map(productMapper::toDto);
}
```

---

## 6. Repository Layer

### Modified Repository
**Repository Interface:** `ProductRepository`  
No new methods or queries required. The inherited `findAll(Pageable pageable)` method from `JpaRepository` will handle both pagination and sorting.

---

## 7. Security & Validation

- **Validation:** Ensure the query parameters (`page`, `size`, `sort`) are valid.
  - Use the `@PageableDefault` annotation in the controller to apply defaults and enforce constraints.
  - Leverage `spring-boot-starter-data-jpa` to validate the `sort` parameter against available entity fields.
- **Authorization:** Reuse the existing Spring Security configuration to ensure only authorized users can access the endpoint.

---

## 8. Error Handling

- **Invalid Page or Size:**
  - Handled automatically by Spring, which throws a `MethodArgumentNotValidException`.
- **Invalid Sort Parameters:**
  - Spring's `Sort` class will throw an `IllegalArgumentException` for unsupported fields or invalid syntax.
  - `GlobalExceptionHandler` will map these to a `400 Bad Request` response.
- **Unavailable Page:** 
  - If the requested `page` exceeds the total number of pages, the repository will return an empty `Page`.

---

## 9. Testing Strategy

### Unit Testing
1. **Service Layer**
   - Verify `productRepository.findAll(Pageable)` is called with the correct `Pageable` object.
   - Validate that the `Page<Product>` is correctly mapped to `Page<ProductResponseDto>`.

2. **Controller Layer**
   - Mock `ProductService` and use `MockMvc` to simulate API calls.
   - Test various combinations of query parameters (`page`, `size`, `sort`) for successful responses.
   - Handle error scenarios for invalid parameters.

#### Example Test Case Names
- `shouldReturnDefaultPage_whenNoQueryParamsProvided()`
- `shouldReturnSortedPage_whenSortParamProvided()`
- `shouldThrowException_whenInvalidSortFieldProvided()`
- `shouldRespectPageAndSizeParams_whenValidParamsProvided()`

### Integration Testing
- Use in-memory H2 database with test data.
- Test end-to-end flow: API request → Service → Repository → Database → Response.
- Verify pagination metadata (e.g., total pages, total elements).

---

## 10. Open Questions

1. Should we allow sorting by non-indexed fields in the database, knowing it may impact performance at scale?
2. Are there any restrictions on `size` (e.g., maximum size of a single page)?