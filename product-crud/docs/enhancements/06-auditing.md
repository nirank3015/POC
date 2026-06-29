# Enhancement 06 — Spring Data Auditing

## Goal
Automatically track who created or last modified a product record, and when,
without requiring manual field-setting in service code.

## Requirements
- `createdBy` — username of who created the record; set on insert, never updated
- `lastModifiedBy` — username of who last changed the record; updated on every save
- `lastModifiedDate` — timestamp of last change; updated on every save
- `createdAt` already exists on the entity; keep it as-is

## Data Model Changes

New fields on `Product`:

| Field | Type | Annotation |
|---|---|---|
| `createdBy` | `String` | `@CreatedBy`, `@Column(updatable = false)` |
| `lastModifiedBy` | `String` | `@LastModifiedBy` |
| `lastModifiedDate` | `LocalDateTime` | `@LastModifiedDate` |

Add to `ProductResponseDto`: `createdBy`, `lastModifiedBy`, `lastModifiedDate`

## Implementation Notes

### Enable auditing on the main class
```java
@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class ProductCrudApplication { ... }
```

### AuditorAware bean
```java
@Bean
public AuditorAware<String> auditorProvider() {
    return () -> Optional.of("system");
}
```

### Entity changes
```java
@EntityListeners(AuditingEntityListener.class)
// on the class

@CreatedBy
@Column(updatable = false)
private String createdBy;

@LastModifiedBy
private String lastModifiedBy;

@LastModifiedDate
private LocalDateTime lastModifiedDate;
```

Remove the manual `@PrePersist onCreate()` method — `createdAt` should also be
changed to `@CreatedDate` with `@Column(updatable = false)` for consistency.

## Acceptance Criteria
- [ ] `POST /api/products` — response includes `"createdBy": "system"`
- [ ] `PUT /api/products/{id}` — `lastModifiedBy` and `lastModifiedDate` are updated
- [ ] `createdBy` does not change on a `PUT`
- [ ] After wiring JWT (Enhancement 03), swap `auditorProvider` to return the JWT username and re-test
- [ ] Unit test: verify `@DataJpaTest` slice saves a product and asserts `createdBy` is populated
