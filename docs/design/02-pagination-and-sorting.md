# Technical Design: 02-pagination-and-sorting

## 1. Overview
The purpose of this feature is to enhance the `GET /api/products` endpoint by introducing pagination and sorting capabilities. By doing so, the API will prevent unbounded list responses, improving performance and usability for clients. This feature enables clients to request specific pages with configurable sizes and sorting orders, ensuring efficient data retrieval and presentation.

## 2. Scope

### In Scope
- Modifying the `GET /api/products` endpoint to:
  - Accept `page`, `size`, and `sort` query parameters.
  - Return a paginated and sorted response.
- Default behavior:
  - Page size: 20
  - Default sorting: By `id` (ascending)
- Updates to the `ProductService` and `ProductController` to support pagination and sorting.
- Integration tests for the controller and unit tests for the service layer.

### Out of Scope
- Adding new fields for sorting that are not part of the existing `Product` entity.
- Backend caching, rate limiting, or other optimizations outside this requirement.
- Changes to the Angular/React/Frontend or any client integrations.

---

## 3. API Design

### Endpoint and Method
**Endpoint:** `GET /api/products`  
**Method:** `GET`  

### Query Parameters
| Parameter  | Type     | Default | Required | Description                                     |
|------------|----------|---------|----------|------------------------------------------------|
| `page`     | Integer  | `0`     | No       | The page number (zero-based).                  |
| `size`     | Integer  | `20`    | No       | The number of records per page.                |
| `sort`     | String   | `id,asc`| No       | The field and order for sorting (`field,order`).|

### Sample Requests
#### Request 1 (Default behavior)
```http
GET /api/products
```

#### Request 2 (Custom pagination)
```http
GET /api/products?page=0&size=10
```

#### Request 3 (Custom sorting)
```http
GET /api/products?sort=price,desc
```

### Response Format
**Response Body:** Paginated `ProductResponseDto` wrapped in a `Page` object.
```json
{
  "content": [
    {
      "id": 1,
      "name": "Product A",
      "price": 100.00,
      "description": "Description of Product A"
    },
    {
      "id": 2,
      "name": "Product B",
      "price": 200.00,
      "description": "Description of Product B"
    }
  ],
  "pageable": {
    "sort": {
      "sorted": true,
      "unsorted": false,
      "empty": false
    },
    "pageNumber": 0,
    "pageSize": 2,
    "offset": 0,
    "paged": true,
    "unpaged": false
  },
  "totalElements": 50,
  "totalPages": 25,
  "last": false,
  "size": 2,
  "number": 0,
  "sort": {
    "sorted": true,
    "unsorted": false,
    "empty": false
  },
  "numberOfElements": 2,
  "first": true,
  "empty": false
}
```

### HTTP Status Codes
| Status Code | Description              |
|-------------|--------------------------|
| `200 OK`    | Pagination performed successfully. |
| `400 Bad Request` | Invalid query parameter values. |

---

## 4. Data Model Changes
No changes to JPA entities or the database are required as this feature operates on existing data in the `Product` entity.  

---

## 5. Service Layer Design

### `ProductService` Interface
```java
public interface ProductService {
    Page<ProductResponseDto> getAllProducts(Pageable pageable);
}
```

### `ProductServiceImpl`
#### Method Implementation
```java
@Override
@Transactional(readOnly = true)
public Page<ProductResponseDto> getAllProducts(Pageable pageable) {
    return productRepository
        .findAll(pageable)
        .map(product -> mapToProductResponseDto(product));
}
```

### Business Rules
- If `page` or `size` is negative, or `sort` contains invalid fields, a `BadRequestException` will be thrown.
- Ensure a default pageable (page 0, size 20, sorted by `id` in ascending order) is applied.

---

## 6. Repository Layer

### Modifications to `ProductRepository`
No changes are required because `ProductRepository` already extends `JpaRepository`, which provides the `findAll(Pageable pageable)` method out of the box.

---

## 7. Security & Validation

### Input Validation
- Use `@PageableDefault(size = 20, sort = "id")` in the controller to enforce defaults.
- Handle invalid `page`, `size`, or `sort` parameter values with `@RestControllerAdvice` in `GlobalExceptionHandler`.

### Authorization
- Ensure the `GET /api/products` endpoint is accessible by authenticated users (if applicable according to the broader application’s security rules).

---

## 8. Error Handling

### Exceptions
- **`BadRequestException`**: Thrown for invalid query parameters.
  
### Global Exception Mapping
#### `GlobalExceptionHandler` Example
```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadRequest(BadRequestException ex) {
        return ResponseEntity.badRequest().body(new ApiResponse<>(false, ex.getMessage(), null));
    }
}
```

---

## 9. Testing Strategy

### Unit Tests
1. **`ProductService.getAllProducts`**:
   - Verify `productRepository.findAll(any(Pageable.class))` is called with correct `Pageable` for different page/size/sort values.

   ```java
   @Test
   @DisplayName("should return paginated and sorted products")
   void shouldReturnPaginatedProducts() {
       Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "price"));
       when(productRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(product1, product2)));

       Page<ProductResponseDto> response = productService.getAllProducts(pageable);

       assertNotNull(response);
       assertEquals(2, response.getContent().size());
   }
   ```

### Integration Tests
1. **Controller Test (`@WebMvcTest`)**:
   - Test for default pagination and sorting.
   - Test for valid page, size, and sort parameters.
   - Test invalid query parameters (e.g., negative page/size, invalid sort fields).

   ```java
   @Test
   @DisplayName("should return products with default pagination and sorting when no params are provided")
   void shouldReturnProductsWithDefaultPagination() throws Exception {
       mockMvc.perform(get("/api/products"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.content").isArray())
           .andExpect(jsonPath("$.pageable.pageNumber").value(0))
           .andExpect(jsonPath("$.pageable.pageSize").value(20));
   }
   ```

---

## 10. Open Questions
1. Should authenticated users have different default page sizes or limits? E.g., Admins retrieve larger datasets.
2. Are there specific fields that should or shouldn’t be available for sorting in the `sort` query parameter?
3. Are there any additional considerations for performance, such as maximum allowable `size`? 

--- 

This document outlines the completion of the feature to enable efficient, paginated, and sorted access to product data while adhering to the established project conventions and requirements.