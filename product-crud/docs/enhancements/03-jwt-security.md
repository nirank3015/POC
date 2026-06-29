# Enhancement 03 — JWT Authentication

## Goal
Protect all `/api/products` endpoints so only authenticated clients with a valid
JWT token can access them.

## Requirements
- `POST /auth/login` accepts `{ "username": "...", "password": "..." }` and returns a JWT
- All `GET/POST/PUT/DELETE /api/products/**` require `Authorization: Bearer <token>` header
- Invalid or missing token returns `401 Unauthorized`
- Token expiry: 24 hours

## API Changes
New endpoint (no auth required):
```
POST /auth/login
Body: { "username": "admin", "password": "password" }
Response 200: { "token": "eyJhbGci..." }
Response 401: { "message": "Invalid credentials" }
```

## Implementation Notes

### Dependencies to add to pom.xml
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.3</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
</dependency>
```

### New classes
- `security/JwtUtil.java` — `generateToken(username)`, `extractUsername(token)`, `isTokenValid(token)`
- `security/JwtAuthFilter.java` — `OncePerRequestFilter`, reads `Authorization` header, validates token, sets `SecurityContextHolder`
- `security/SecurityConfig.java` — `@Configuration`, disables CSRF, permits `/auth/**`, secures `/api/**`, registers `JwtAuthFilter`
- `controller/AuthController.java` — `POST /auth/login`, hardcoded user for POC (`admin/password`), returns token

### application.yml addition
```yaml
jwt:
  secret: "your-256-bit-secret-key-here-change-in-production"
  expiration-ms: 86400000
```

## Acceptance Criteria
- [ ] `POST /auth/login` with valid credentials returns a JWT string
- [ ] `GET /api/products` without token returns `401`
- [ ] `GET /api/products` with valid `Authorization: Bearer <token>` returns `200`
- [ ] Expired or tampered token returns `401`
- [ ] Unit test: `JwtUtilTest` — token generation, extraction, and expiry validation
