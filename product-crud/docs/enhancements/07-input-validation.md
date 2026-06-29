# Enhancement 07 — Input Validation

## Goal
Reject invalid product data at the controller boundary before it reaches the
service or database, returning clear, field-level error messages to the client.

## Requirements
- `name` — required, not blank, max 100 characters
- `category` — required, not blank, max 50 characters
- `price` — required, must be > 0.00
- `stockQuantity` — required, must be >= 0

## Dependency to add to pom.xml
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

## ProductRequestDto Changes
```java
import jakarta.validation.constraints.*;

public class ProductRequestDto {

    @NotBlank(message = "Name must not be blank")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @NotBlank(message = "Category must not be blank")
    @Size(max = 50, message = "Category must not exceed 50 characters")
    private String category;

    @NotNull(message = "Price must not be null")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal price;

    @NotNull(message = "Stock quantity must not be null")
    @Min(value = 0, message = "Stock quantity must be 0 or greater")
    private Integer stockQuantity;

    // existing getters/setters unchanged
}
```

## Controller Changes
Add `@Valid` to the `@RequestBody` parameter on `createProduct` and `updateProduct`:

```java
@PostMapping
public ResponseEntity<ProductResponseDto> createProduct(@Valid @RequestBody ProductRequestDto requestDto) { ... }

@PutMapping("/{id}")
public ResponseEntity<ProductResponseDto> updateProduct(@PathVariable Long id,
                                                        @Valid @RequestBody ProductRequestDto requestDto) { ... }
```

## Acceptance Criteria
- [ ] `POST /api/products` with empty `name` returns `400` with `"name: Name must not be blank"` in `details`
- [ ] `POST /api/products` with `price: -5` returns `400` with `"price: Price must be greater than 0"` in `details`
- [ ] `POST /api/products` with `stockQuantity: -1` returns `400` with a stock quantity error
- [ ] Valid request still returns `201` as before
- [ ] Unit test: `ProductControllerTest` — add test cases sending invalid DTOs and asserting `400` + field error messages
