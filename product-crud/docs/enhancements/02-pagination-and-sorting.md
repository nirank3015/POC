# Enhancement 02 — Pagination & Sorting

## Goal
Prevent unbounded list responses by supporting page-based retrieval with
configurable sort order on the product list endpoint.

## Requirements
- Client can request a specific page: `?page=0&size=10`
- Client can sort by any field: `?sort=price,asc` or `?sort=name,desc`
- Response wraps results in a page object (total elements, total pages, current page, content)
- Default: page 0, size 20, sorted by `id` ascending

## API Changes
```
GET /api/products?page=0&size=10&sort=price,asc
```
Response changes from `List<ProductResponseDto>` to a Spring `Page<ProductResponseDto>`.

## Implementation Notes
- `ProductRepository` already extends `JpaRepository` which includes `PagingAndSortingRepository`
- Add `findAll(Pageable pageable)` — already inherited, no extra code in repository
- Update `ProductService.getAllProducts()` signature to accept `Pageable` and return `Page<ProductResponseDto>`
- Update `ProductController.getAllProducts()` to accept `@PageableDefault(size = 20, sort = "id") Pageable pageable`
- Spring MVC auto-resolves `Pageable` from query params when `spring-data-web` is on the classpath (it is, via `spring-boot-starter-data-jpa`)

## Acceptance Criteria
- [ ] `GET /api/products?page=0&size=2` returns at most 2 products and includes `totalElements`, `totalPages`
- [ ] `GET /api/products?sort=price,desc` returns products sorted highest price first
- [ ] `GET /api/products` (no params) returns page 0 with 20 items max
- [ ] Unit test: mock `productRepository.findAll(any(Pageable.class))` and assert the controller passes `Pageable` through correctly
