# Enhancement 01 — Search & Filtering

## Goal
Allow clients to filter the product list by `category`, `minPrice`, and `maxPrice`
without requiring separate endpoints.

## Requirements
- `GET /api/products?category=Electronics` returns only Electronics products
- `GET /api/products?minPrice=100&maxPrice=500` returns products in that price range
- Parameters are optional; omitting them returns all products
- Filters are combinable: `?category=Electronics&maxPrice=1000`

## API Changes
```
GET /api/products?category={category}&minPrice={min}&maxPrice={max}
```
No new endpoints. Query params added to the existing list endpoint.

## Implementation Notes
- Add `spring-data-jpa` `Specification<Product>` support: `JpaSpecificationExecutor<Product>` on `ProductRepository`
- Create `ProductSpecification` utility class with static methods:
  - `hasCategory(String category)` → `Specification<Product>`
  - `hasPriceGreaterThanOrEqual(BigDecimal min)` → `Specification<Product>`
  - `hasPriceLessThanOrEqual(BigDecimal max)` → `Specification<Product>`
- In `ProductService`, add: `List<ProductResponseDto> searchProducts(String category, BigDecimal minPrice, BigDecimal maxPrice)`
- Combine specs with `Specification.where(...).and(...)`
- Update `ProductController.getAllProducts()` to accept optional `@RequestParam` values and delegate to `searchProducts`

## Acceptance Criteria
- [ ] `GET /api/products?category=Electronics` returns only products with category "Electronics"
- [ ] `GET /api/products?minPrice=500&maxPrice=1500` returns products where 500 ≤ price ≤ 1500
- [ ] `GET /api/products` (no params) still returns all products
- [ ] Unit test: `ProductSpecificationTest` — each spec predicate tested in isolation with an in-memory H2 slice test (`@DataJpaTest`)
