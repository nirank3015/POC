# Technical Design: 04-swagger-openapi

---

## **1. Overview**
The purpose of this enhancement is to integrate Swagger/OpenAPI support into the Spring Boot project. This will enable developers and stakeholders to view, explore, and interact with the API endpoints through an auto-generated, user-friendly API documentation interface. Two primary endpoints will be exposed, one for serving the OpenAPI JSON specification and the other for rendering a graphical Swagger UI. These tools will streamline the understanding, testing, and maintenance processes of the application, ensuring adherence to industry standards and enhancing the developer experience.

---

## **2. Scope**

### **In Scope**
1. Integration of **SpringDoc OpenAPI** into the Spring Boot application by adding the necessary dependency.
2. Auto-generation of the OpenAPI specification for all API endpoints using annotations on controllers and methods.
3. Exposing the Swagger UI at `/swagger-ui.html` for a user-friendly interface to test API endpoints interactively.
4. Exposing the OpenAPI specification as a JSON object at `/v3/api-docs`.
5. Annotating controllers and methods with metadata via SpringDoc annotations to enhance the documentation.

### **Out of Scope**
1. The implementation of new features or changes to API functionality.
2. Custom styling or branding of the Swagger UI, beyond setting the title, description, and version.
3. Adding authorization or authentication to access the documentation endpoints (can be added as a future enhancement).
4. Extensive diagnostics or custom query functionalities beyond the default offerings of Swagger/OpenAPI.

---

## **3. API Design**

### **New Endpoints**
1. GET `/swagger-ui.html`
   - Description: Provides access to the Swagger UI.
   - Request Body: None
   - Response: HTML/CSS/JavaScript required to render the Swagger UI.
   - HTTP Response Code: `200`

   **Example**
   ```http
   GET /swagger-ui.html
   ```
   **Response**
   ```
   [HTML page with Swagger UI]
   ```

2. GET `/v3/api-docs`
   - Description: Serves the OpenAPI specification for the API in JSON format.
   - Request Body: None
   - Response: OpenAPI Specification v3 as JSON.
   - HTTP Response Code: `200`

   **Example**
   ```http
   GET /v3/api-docs
   ```
   **Response**
   ```json
   {
     "openapi": "3.0.1",
     "info": {
       "title": "Product CRUD API",
       "description": "POC Spring Boot CRUD application",
       "version": "1.0.0"
     },
     "paths": {
       ...
     }
   }
   ```

### **Existing Endpoints**
The five existing product-related CRUD endpoints will have their annotations updated with metadata for enhanced API documentation. Example:

#### GET `/api/v1/products`
- Summary: Get all products
- Produces: `application/json`
- Responses:
  - `200 OK`: List of products.
  - `500 Internal Server Error`: Unexpected issue.
  
**Example Response**
```json
[
  {
    "id": 123,
    "name": "Product A",
    "description": "An amazing product.",
    "price": 100.0
  },
  {
    "id": 124,
    "name": "Product B",
    "description": "Another great product.",
    "price": 200.0
  }
]
```

#### POST `/api/v1/products`
- Summary: Create a new product
- Request Body: 
   ```json
   {
     "name": "Product A",
     "description": "An amazing product.",
     "price": 100.0
   }
   ```
- Produces: `application/json`
- Responses:
  - `201 Created`: Product successfully created.
  - `400 Bad Request`: Validation error.
  - `500 Internal Server Error`: Unexpected issue.
  
**Example Response**
```json
{
  "id": 125,
  "name": "Product A",
  "description": "An amazing product.",
  "price": 100.0
}
```

Add similar annotations for the remaining endpoints (`GET /api/v1/products/{id}`, `PUT /api/v1/products/{id}`, `DELETE /api/v1/products/{id}`).

---

## **4. Data Model Changes**

### **No New JPA Entity Modifications Required**
The enhancement is limited to documentation and does not require any changes to the database schema, JPA entities, or Flyway migrations.

---

## **5. Service Layer Design**

### **No New Service Classes or Modifications Needed**
The functionality of services will remain unchanged. No new service methods or modifications are required as this enhancement focuses only on documentation.

---

## **6. Repository Layer**

### **No New Repository Methods Required**
Existing `ProductRepository` and other related repositories are sufficient to support this enhancement. No additional query methods or specifications need to be added.

---

## **7. Security & Validation**

### **Security Requirements**
1. The `/swagger-ui.html` and `/v3/api-docs` endpoints are public and should not require authentication. This ensures ease of access for developers and stakeholders during the development process. If authentication is required, future enhancements can include configuration changes to enforce role-based access.

2. APIs will maintain current validation rules:
   - Use of `@Valid` for request bodies in controller methods.
   - Appropriate validation annotations already applied on DTO fields (e.g., `@NotNull`, `@Size`, etc.).

---

## **8. Error Handling**

### **Expected Errors**
1. **Bad Request (400):** For endpoints expecting input, the system ensures the request body is validated and returns descriptive error messages for invalid input.
2. **Not Found (404):** For endpoints like `GET /api/v1/products/{id}`, a `ResourceNotFoundException` is thrown if no matching product exists.
3. **Internal Server Errors (500):** Captures unexpected errors and provides a friendly error message.

### **Custom Error Responses**
No alterations are needed to the existing `GlobalExceptionHandler` to support this enhancement.

---

## **9. Testing Strategy**

### **Unit Tests**
- Tests are not required for `/swagger-ui.html` and `/v3/api-docs` endpoints as they are automatically generated by SpringDoc OpenAPI and have no business logic.

### **Integration Tests**
1. Verify the existence and accessibility of `/swagger-ui.html` and `/v3/api-docs`.
2. Verify that the OpenAPI JSON contains the title, description, version, and all defined endpoints.
3. Test "Try it out" functionality by invoking a real endpoint through the Swagger UI or OpenAPI tooling.

### **Validation Tests**
- Confirm that request schema and response schema match the definitions in OpenAPI documentation.
- Use tools like `swagger-parser` for automated JSON schema validation.

### **Naming Convention**
Use `should{Behavior}_when{Condition}` format. Example:
```java
@DisplayName("shouldReturnSwaggerUI_whenRequestingSwaggerUIEndpoint")
```

---

## **10. Open Questions**

1. Do we need to restrict access to documentation endpoints (`/swagger-ui.html`, `/v3/api-docs`) in production environments? If so, should this be implemented now or during a future enhancement?
2. Should any additional metadata (e.g., custom attributes) be provided in the OpenAPI spec to document specific use cases?

---

This technical design aligns with standard Spring Boot practices and ensures minimal disruption to the existing codebase while enhancing its usability for developers and stakeholders. The integration of Swagger/OpenAPI will improve the overall developer experience, enabling faster and more reliable interaction with the API.