# Enhancement 05 — Global Exception Handling (Expanded)

## Goal
Expand the existing `GlobalExceptionHandler` to cover validation errors,
database constraint violations, and all other unhandled exceptions with
consistent, structured JSON error responses.

## Requirements
- Validation errors (`@Valid` failures) return `400` with a list of field-level messages
- Database constraint violations (duplicate keys, not-null) return `409 Conflict`
- All unhandled exceptions return `500` (no stack traces leaked to the client)
- All error responses share the same JSON shape

## Error Response Shape
```json
{
  "timestamp": "2026-06-29T10:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "details": ["name: must not be blank", "price: must be greater than 0"]
}
```
The `details` field is omitted for non-validation errors.

## Implementation Notes

### MethodArgumentNotValidException (validation failures)
```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
    List<String> errors = ex.getBindingResult().getFieldErrors().stream()
        .map(e -> e.getField() + ": " + e.getDefaultMessage())
        .collect(Collectors.toList());
    return ResponseEntity.badRequest().body(Map.of(
        "timestamp", LocalDateTime.now().toString(),
        "status", 400,
        "error", "Bad Request",
        "message", "Validation failed",
        "details", errors
    ));
}
```

### DataIntegrityViolationException (DB constraint violations)
```java
@ExceptionHandler(DataIntegrityViolationException.class)
public ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
        "timestamp", LocalDateTime.now().toString(),
        "status", 409,
        "error", "Conflict",
        "message", "Data integrity violation — check for duplicate values"
    ));
}
```

## Acceptance Criteria
- [ ] Sending a product with `name: null` returns `400` with a `details` array naming the field
- [ ] A duplicate-key insert (if unique constraint added) returns `409`
- [ ] Any unhandled runtime exception returns `500` with no stack trace in response body
- [ ] Unit test in `GlobalExceptionHandlerTest` covering all three handler methods using `MockMvc`
