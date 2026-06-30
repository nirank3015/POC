# Enhancement 02 — Search, Filtering, Pagination & Sorting

## Goal
Allow clients to filter the product list by `category`, `minPrice`, and `maxPrice`,
paginate results, and sort by a chosen field — all without requiring separate endpoints.

## Requirements

### Filtering
- `GET /api/products?category=Electronics` returns only Electronics products
- `GET /api/products?minPrice=100&maxPrice=500` returns products in that price range
- Parameters are optional; omitting them returns all products
- Filters are combinable: `?category=Electronics&maxPrice=1000`

### Pagination
- `GET /api/products?page=0&size=20` returns the first 20 products
- `page` is zero-indexed; defaults to `0` if not provided
- `size` defaults to `20` if not provided, with a maximum allowed value of `100`
- Response includes pagination metadata: `totalElements`, `totalPages`, `currentPage`, `pageSize`

### Sorting
- `GET /api/products?sortBy=price&sortDirection=asc` sorts results by price ascending
- `sortBy` accepts: `name`, `price`, `category`, `createdAt`
- `sortDirection` accepts: `asc`, `desc` (defaults to `asc` if not provided)
- Invalid `sortBy` values return `400 Bad Request` with a descriptive error message

### Combined Usage
All filtering, pagination, and sorting parameters are combinable in a single request:
```
GET /api/products?category=Electronics&maxPrice=1000&page=0&size=10&sortBy=price&sortDirection=desc
```

## API Changes
```
GET /api/products?category={category}&minPrice={min}&maxPrice={max}&page={page}&size={size}&sortBy={field}&sortDirection={asc|desc}
```
No new endpoints. Query params added to the existing list endpoint.

### Sample Response Shape
```json
{
  "content": [
    { "id": 1, "name": "Laptop", "category": "Electronics", "price": 899.00 }
  ],
  "totalElements": 47,
  "totalPages": 5,
  "currentPage": 0,
  "pageSize": 10
}
```

## Implementation Notes

### Filtering
- Add `spring-data-jpa` `Specification<Product>` support: `JpaSpecificationExecutor<Product>` on `ProductRepository`
- Create `ProductSpecification` utility class with static methods:
  - `hasCategory(String category)` → `Specification<Product>`
  - `hasPriceGreaterThanOrEqual(BigDecimal min)` → `Specification<Product>`
  - `hasPriceLessThanOrEqual(BigDecimal max)` → `Specification<Product>`
- Combine specs with `Specification.where(...).and(...)`

### Pagination & Sorting
- Update `ProductService.searchProducts(...)` signature to:
  `Page<ProductResponseDto> searchProducts(String category, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable)`
- Use Spring Data's `Pageable` and `PageRequest.of(page, size, sort)` to build the request
- Build `Sort` from `sortBy` + `sortDirection`, validating `sortBy` against an allow-list of fields before constructing it
- Repository call becomes: `productRepository.findAll(spec, pageable)` (returns `Page<Product>`)
- Map `Page<Product>` to a `PagedResponseDto<ProductResponseDto>` wrapper containing `content`, `totalElements`, `totalPages`, `currentPage`, `pageSize`

### Controller
- Update `ProductController.getAllProducts()` to accept optional `@RequestParam` values:
  - `category`, `minPrice`, `maxPrice` (existing)
  - `page` (default `0`), `size` (default `20`, capped at `100`)
  - `sortBy` (default `"id"`), `sortDirection` (default `"asc"`)
- Validate `size` does not exceed `100`; clamp or reject with `400` per project convention
- Delegate to `searchProducts(...)` and return the paged response wrapper

## Acceptance Criteria

### Filtering
- [ ] `GET /api/products?category=Electronics` returns only products with category "Electronics"
- [ ] `GET /api/products?minPrice=500&maxPrice=1500` returns products where 500 ≤ price ≤ 1500
- [ ] `GET /api/products` (no params) still returns all products
- [ ] Unit test: `ProductSpecificationTest` — each spec predicate tested in isolation with an in-memory H2 slice test (`@DataJpaTest`)

### Pagination
- [ ] `GET /api/products?page=0&size=10` returns exactly 10 (or fewer, on the last page) products
- [ ] Response includes correct `totalElements`, `totalPages`, `currentPage`, `pageSize`
- [ ] `size` greater than `100` is rejected or clamped per project convention (document which in the design doc)
- [ ] Omitting `page`/`size` defaults to page `0`, size `20`

### Sorting
- [ ] `GET /api/products?sortBy=price&sortDirection=asc` returns products in ascending price order
- [ ] `GET /api/products?sortBy=price&sortDirection=desc` returns products in descending price order
- [ ] An invalid `sortBy` value (e.g. `sortBy=foo`) returns `400 Bad Request` with a descriptive message
- [ ] Omitting `sortBy`/`sortDirection` defaults to sorting by `id` ascending

### Combined
- [ ] `GET /api/products?category=Electronics&maxPrice=1000&page=0&size=10&sortBy=price&sortDirection=desc` returns correctly filtered, sorted, and paginated results
- [ ] Integration test: `ProductControllerTest` covering combined filter + pagination + sort scenarios using `@WebMvcTest` or `@SpringBootTest`
