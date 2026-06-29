# Enhancement 04 — Swagger / OpenAPI Documentation

## Goal
Auto-generate interactive API documentation so developers can explore and test
all endpoints from a browser without writing any client code.

## Requirements
- Swagger UI available at `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON spec available at `http://localhost:8080/v3/api-docs`
- All 5 CRUD endpoints documented with request/response schema
- API has a title, description, and version shown in the UI

## API Changes
No functional changes. Two new read-only documentation URLs:
- `GET /swagger-ui.html` — interactive HTML UI
- `GET /v3/api-docs` — raw OpenAPI 3 JSON

## Implementation Notes

### Dependency to add to pom.xml
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.3.0</version>
</dependency>
```

### application.yml addition
```yaml
springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
```

### Controller annotations to add
On `ProductController` class:
```java
@Tag(name = "Products", description = "CRUD operations for products")
```

On each method:
```java
@Operation(summary = "Create a new product")
@ApiResponse(responseCode = "201", description = "Product created successfully")

@Operation(summary = "Get all products")

@Operation(summary = "Get product by ID")
@ApiResponse(responseCode = "404", description = "Product not found")
```

### OpenAPI bean (optional, for title/version)
```java
@Bean
public OpenAPI productCrudOpenAPI() {
    return new OpenAPI()
        .info(new Info()
            .title("Product CRUD API")
            .description("POC Spring Boot CRUD application")
            .version("1.0.0"));
}
```

## Acceptance Criteria
- [ ] `http://localhost:8080/swagger-ui.html` loads the Swagger UI with all 5 endpoints listed
- [ ] `http://localhost:8080/v3/api-docs` returns valid OpenAPI 3 JSON
- [ ] Each endpoint shows its request body schema and response codes
- [ ] "Try it out" button on `GET /api/products` returns a real response
