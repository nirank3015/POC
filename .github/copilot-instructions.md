# GitHub Copilot Instructions — Spring Boot Project Conventions

> This file is used by the auto-dev pipeline to give the AI model context
> about your project's coding standards, architecture, and conventions.
> Update this file to match your actual project structure.

## Project Overview
- **Framework:** Java 17 + Spring Boot 3.x
- **Build Tool:** Maven
- **Database:** PostgreSQL with Flyway migrations
- **ORM:** Spring Data JPA + Hibernate
- **Security:** Spring Security with JWT
- **Testing:** JUnit 5 + Mockito + @WebMvcTest / @DataJpaTest

## Package Structure
```
com.example.yourapp
├── config/          # Spring configuration classes
├── controller/      # REST controllers (@RestController)
├── dto/             # Request/Response DTOs (record or class)
├── entity/          # JPA entities
├── exception/       # Custom exceptions + GlobalExceptionHandler
├── repository/      # Spring Data JPA repositories
├── service/         # Service interfaces
│   └── impl/        # Service implementations
├── specification/   # JPA Specifications for dynamic queries
└── util/            # Utility classes
```

## Naming Conventions
- Controllers: `{Feature}Controller` → `UserController`
- Services: interface `{Feature}Service`, impl `{Feature}ServiceImpl`
- Repositories: `{Entity}Repository`
- DTOs: `{Feature}Request`, `{Feature}Response`
- Entities: singular noun → `User`, `Account`, `Transaction`
- DB tables: snake_case plural → `users`, `user_accounts`
- Flyway migrations: `V{timestamp}__{description}.sql`

## Entity Conventions
```java
@Entity
@Table(name = "table_name")
@Getter @Setter           // Use Lombok
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MyEntity extends BaseEntity {  // BaseEntity has id, createdAt, updatedAt
    // fields here
}
```

## Service Layer Conventions
- All service methods wrapped in `@Transactional`
- Read-only methods use `@Transactional(readOnly = true)`
- Throw custom exceptions: `ResourceNotFoundException`, `BusinessException`
- Never expose entities directly — always map to DTOs

## Controller Conventions
```java
@RestController
@RequestMapping("/api/v1/{resource}")
@RequiredArgsConstructor
@Validated
public class MyController {
    // Use @Valid on request body
    // Return ResponseEntity<ApiResponse<T>>
}
```

## Repository Conventions
- Use `JpaSpecificationExecutor<T>` for dynamic queries
- Use Specification pattern for complex filters
- Prefer Spring Data method names for simple queries
- Use `@Query` JPQL for multi-join queries

## Error Handling
- `GlobalExceptionHandler` with `@RestControllerAdvice` handles all exceptions
- Standard response: `ApiResponse<T>` wrapper with `status`, `message`, `data`
- HTTP 404 for not found, 400 for validation, 409 for conflicts, 500 for unexpected

## Testing Conventions
- Service tests: `@ExtendWith(MockitoExtension.class)`, mock repositories
- Controller tests: `@WebMvcTest`, use `MockMvc`, mock service layer
- Use `@DisplayName` on all test methods
- Test naming: `should{ExpectedBehavior}_when{Condition}`