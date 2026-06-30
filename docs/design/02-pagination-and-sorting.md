# Technical Design: 02-pagination-and-sorting

---

## 1. Overview
The purpose of this enhancement is to implement pagination and sorting for the product retrieval endpoint (`GET /api/products`). This ensures predictable response sizes, prevents unbounded list responses, and improves scalability. Clients will be able to retrieve paginated products with configurable sort criteria, adhering to the default page size of 20 and sorting by `id` in ascending order if no parameters are specified.

---

## 2. Scope

### In Scope
- Adding pagination and sorting capabilities to the `GET /api/products` endpoint.
- Supporting `page`, `size`, and `sort` query parameters.
- Default values: Page = 0, Size = 20, Sort = `id,asc`.
- Updating `ProductService`, `ProductController`, and repository integration as necessary.
- Wrapping the response in a page object with total elements, total pages, current page, and content.
- Unit and integration testing for the functionality.

### Out of Scope
- Any changes to the `Product` entity.
- Complex filtering logic unrelated to pagination and sorting.
- Changes to authentication/authorization and security configurations.

---

## 3. API Design

### Endpoint Details
```
GET /api/products
```
- HTTP Method: `GET`
- Query Parameters:
  - `page` (optional, default: 0): Page number (0-based index).
  - `size` (optional, default: 20): Number of items per page.
  - `sort` (optional, default: `id,asc`): Sorting criteria in the format `field,direction` where:
    - Valid `field` options: Fields of the `Product` entity or its DTO (e.g., `id`, `name`, `price`).
    - `direction`: `"asc"` or `"desc"`.

### Response Body
The response will wrap the list of products in a Spring `Page` object. The structure of the response is as follows:

```json
{
  "status": "success",
  "message": "Products retrieved successfully",
  "data": {
    "content": [
      {
        "id": 1,
        "name": "Product A",
        "price": 19.99,
        "description": "Description of Product A"
      },
      {
        "id": 2,
        "name": "Product B",
        "price": 24.99,
        "description": "Description of Product B"
      }
    ],
    "totalElements": 50,
    "totalPages": 25,
    "size": 2,
    "number": 0,
    "sort": [
      {
        "property": "price",
        "direction": "ASC",
        "ignoreCase": false,
        "nullHandling": "NATIVE",
        "ascending": true,
        "descending": false
      }
    ],
    "first": true,
    "last": false,
    "numberOfElements": 2,
    "empty": false
  }
}
```

### Response Codes
- **200 OK**: On successful retrieval of the products.
- **400 Bad Request**: When invalid query parameters are supplied (e.g., incorrect sorting field).
- **500 Internal Server Error**: For unexpected errors.

---

## 4. Data Model Changes

### JPA Entity Changes
No changes required to the `Product` entity for this enhancement.

### Database Migration
No new columns, tables, indexes, or constraints are required.

---

## 5. Service Layer Design

### Updates to `ProductService`
#### Interface:
```java
Page<ProductResponseDto> getAllProducts(Pageable pageable);
```

#### Implementation (in `ProductServiceImpl`):
- Update the `getAllProducts()` method:
  - Accept `Pageable` as a parameter.
  - Use the `productRepository.findAll(Pageable)` method to perform pagination and sorting.
  - Convert entities (`Product`) to DTOs (`ProductResponseDto`) using a Mapper (`ProductMapper`).
  - Return a `Page<ProductResponseDto>`.

#### Example Code:
```java
@Override
@Transactional(readOnly = true)
public Page<ProductResponseDto> getAllProducts(Pageable pageable) {
    Page<Product> productPage = productRepository.findAll(pageable);
    return productPage.map(productMapper::toDto);
}
```

---

## 6. Repository Layer

### Necessary Updates
- The `ProductRepository` interface already extends `JpaRepository`, which inherits the `findAll(Pageable pageable)` method from `PagingAndSortingRepository`.
- No additional changes are required to the repository.

---

## 7. Security & Validation

- The `@PageableDefault` annotation will enforce defaults for pagination and sorting if query parameters are not provided, ensuring robust validation at the controller level.
- Input validation will ensure query parameters like `size` and `sort` are valid. For invalid parameters (e.g., unsupported sorting fields), a `ConstraintViolationException` will be thrown and handled via the `GlobalExceptionHandler`.
- Ensure the `GET /api/v1/products` endpoint is secured using Spring Security (e.g., only authenticated users can access the endpoint unless otherwise specified).

---

## 8. Error Handling

### Global Exception Handling
Update the `GlobalExceptionHandler` if necessary, to handle:
- `MethodArgumentNotValidException`: For invalid query parameter values (e.g., `@Min`, `@Max` errors).
- `IllegalArgumentException`: For unsupported sorting fields or directions.
- Return a standard error response using the `ApiResponse<T>` wrapper.

#### Example Error Response
```json
{
  "status": "error",
  "message": "Invalid page or sort parameters",
  "data": null
}
```

---

## 9. Testing Strategy

### Unit Tests
#### `ProductServiceTest`
- `shouldReturnPageOfProducts_whenValidPageable()`:
  - Mock `productRepository.findAll(Pageable)` to return a `Page<Product>`.
  - Test `ProductService.getAllProducts(pageable)` returns expected `Page<ProductResponseDto>`.

### Controller Tests
#### `ProductControllerTest`
- `shouldReturnPaginatedProducts_whenValidQueryParamsPassed()`:
  - Use `@WebMvcTest` and `MockMvc`.
  - Mock `ProductService.getAllProducts(pageable)` to return a `Page<ProductResponseDto>`.
  - Assert HTTP status `200` and verify `Page<ProductResponseDto>` is returned in response JSON.

- `shouldUseDefaultPagination_whenNoQueryParamsProvided()`:
  - Test pagination with no `page`, `size`, or `sort` query parameters.
  - Assert defaults: `page=0`, `size=20`, and `sort=id,asc`.

- Edge Cases:
  - Invalid `page`, `size`, or `sort` value (e.g., `page=-1`).
  - Unsupported sort fields.
  - Empty results (no products).

---

## 10. Open Questions
1. Do we need to validate or restrict which fields are allowed for sorting (e.g., exclude `description`)?
2. Should there be a maximum allowable `size` parameter to prevent overly large pages? If so, what value (e.g., 100)?
3. Should we include links for navigation (`first`, `last`, `next`, `prev`) in the paginated response?

--- 

**End of Document**