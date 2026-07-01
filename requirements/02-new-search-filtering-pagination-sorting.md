# Enhancement 02 — Search, Filtering, Pagination & Sorting

## Goal
Allow clients to filter the product list by `category`, `minPrice`, and `maxPrice`
without requiring separate endpoints, prevent unbounded list responses by supporting
page-based retrieval, and provide configurable sort order on the product list endpoint.

---

## Enhancement 01 — Search & Filtering *(Existing Feature)*

### Goal
Allow clients to filter the product list by `category`, `minPrice`, and `maxPrice`
without requiring separate endpoints.

### Requirements
- `GET /api/products?category=Electronics` returns only Electronics products
- `GET /api/products?minPrice=100&maxPrice=500` returns products in that price range
- Parameters are optional; omitting them returns all products
- Filters are combinable: `?category=Electronics&maxPrice=1000`

### API Changes
```
GET /api/products?category={category}&minPrice={min}&maxPrice={max}
```
No new endpoints. Query params added to the existing list endpoint.

### Implementation Notes
- Add `spring-data-jpa` `Specification<Product>` support: `JpaSpecificationExecutor<Product>` on `ProductRepository`
- Create `ProductSpecification` utility class with static methods:
  - `hasCategory(String category)` → `Specification<Product>`
  - `hasPriceGreaterThanOrEqual(BigDecimal min)` → `Specification<Product>`
  - `hasPriceLessThanOrEqual(BigDecimal max)` → `Specification<Product>`
- In `ProductService`, add: `List<ProductResponseDto> searchProducts(String category, BigDecimal minPrice, BigDecimal maxPrice)`
- Combine specs with `Specification.where(...).and(...)`
- Update `ProductController.getAllProducts()` to accept optional `@RequestParam` values and delegate to `searchProducts`

### Acceptance Criteria
- [ ] `GET /api/products?category=Electronics` returns only products with category "Electronics"
- [ ] `GET /api/products?minPrice=500&maxPrice=1500` returns products where 500 ≤ price ≤ 1500
- [ ] `GET /api/products` (no params) still returns all products
- [ ] Unit test: `ProductSpecificationTest` — each spec predicate tested in isolation with an in-memory H2 slice test (`@DataJpaTest`)

---

## Enhancement 02 — Pagination & Sorting *(New Additional Feature)*

### Goal
Prevent unbounded list responses by supporting page-based retrieval with
configurable sort order on the product list endpoint.

### Requirements
- Client can request a specific page: `?page=0&size=10`
- Client can sort by any field: `?sort=price,asc` or `?sort=name,desc`
- Response wraps results in a page object (total elements, total pages, current page, content)
- Default: page 0, size 20, sorted by `id` ascending

### API Changes
```
GET /api/products?page=0&size=10&sort=price,asc
```
Response changes from `List<ProductResponseDto>` to a Spring `Page<ProductResponseDto>`.

### Sample Response Shape
```json
{
  "content": [
    { "id": 1, "name": "Laptop", "category": "Electronics", "price": 899.00 }
  ],
  "totalElements": 47,
  "totalPages": 5,
  "number": 0,
  "size": 10
}
```

### Implementation Notes
- `ProductRepository` already extends `JpaRepository` which includes `PagingAndSortingRepository`
- Add `findAll(Specification<Product> spec, Pageable pageable)` — inherited from `JpaSpecificationExecutor`, no extra code in repository
- Update `ProductService.searchProducts(...)` signature to accept `Pageable` and return `Page<ProductResponseDto>`:
  `Page<ProductResponseDto> searchProducts(String category, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable)`
- Update `ProductController.getAllProducts()` to accept `@PageableDefault(size = 20, sort = "id") Pageable pageable` alongside existing filter `@RequestParam` values
- Spring MVC auto-resolves `Pageable` from query params when `spring-data-web` is on the classpath (it is, via `spring-boot-starter-data-jpa`)
- Repository call becomes: `productRepository.findAll(spec, pageable)` combining filtering specs with pagination in one query

### Acceptance Criteria
- [ ] `GET /api/products?page=0&size=2` returns at most 2 products and includes `totalElements`, `totalPages`
- [ ] `GET /api/products?sort=price,desc` returns products sorted highest price first
- [ ] `GET /api/products` (no params) returns page 0 with 20 items max, sorted by `id` ascending
- [ ] Unit test: mock `productRepository.findAll(any(Specification.class), any(Pageable.class))` and assert the controller passes both `Specification` and `Pageable` through correctly

---

## Combined Usage

All filtering, pagination, and sorting parameters are combinable in a single request:
```
GET /api/products?category=Electronics&maxPrice=1000&page=0&size=10&sort=price,desc
```

### Combined Acceptance Criteria
- [ ] `GET /api/products?category=Electronics&maxPrice=1000&page=0&size=10&sort=price,desc` returns correctly filtered, paginated, and sorted results
- [ ] Integration test: `ProductControllerTest` covering combined filter + pagination + sort scenarios using `@WebMvcTest`
