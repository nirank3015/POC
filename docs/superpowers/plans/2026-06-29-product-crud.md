# Product CRUD Spring Boot Application — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a fully working Product CRUD REST API using Spring Boot 3.2, Java 17, MySQL 8, and Maven, then write 7 enhancement markdown spec files under `docs/enhancements/`.

**Architecture:** Classic layered pattern — `ProductController` handles HTTP and DTO mapping, `ProductServiceImpl` holds business logic and throws domain exceptions, `ProductRepository` (Spring Data JPA) talks to MySQL. The `Product` entity never crosses the controller boundary; `ProductRequestDto` / `ProductResponseDto` are the API contract.

**Tech Stack:** Java 17, Spring Boot 3.2.x, Spring Data JPA, Hibernate, MySQL 8, Maven, JUnit 5, Mockito, Spring Boot Test.

---

## File Map

| File | Action | Responsibility |
|---|---|---|
| `pom.xml` | Create | Maven dependencies and build config |
| `src/main/resources/application.yml` | Create | DB datasource, JPA config |
| `entity/Product.java` | Create | JPA entity, `@PrePersist` for `createdAt` |
| `dto/ProductRequestDto.java` | Create | Inbound API contract |
| `dto/ProductResponseDto.java` | Create | Outbound API contract |
| `exception/ResourceNotFoundException.java` | Create | Domain exception for missing IDs |
| `exception/GlobalExceptionHandler.java` | Create | `@ControllerAdvice` error handler |
| `repository/ProductRepository.java` | Create | Spring Data JPA interface |
| `service/ProductService.java` | Create | Service interface |
| `service/ProductServiceImpl.java` | Create | Business logic implementation |
| `controller/ProductController.java` | Create | REST endpoints |
| `ProductCrudApplication.java` | Create | Spring Boot entry point |
| `test/.../ProductServiceImplTest.java` | Create | Unit tests for service layer |
| `test/.../ProductControllerTest.java` | Create | `@WebMvcTest` slice tests for controller |
| `docs/enhancements/01-search-and-filtering.md` | Create | Enhancement spec |
| `docs/enhancements/02-pagination-and-sorting.md` | Create | Enhancement spec |
| `docs/enhancements/03-jwt-security.md` | Create | Enhancement spec |
| `docs/enhancements/04-swagger-openapi.md` | Create | Enhancement spec |
| `docs/enhancements/05-global-exception-handling.md` | Create | Enhancement spec |
| `docs/enhancements/06-auditing.md` | Create | Enhancement spec |
| `docs/enhancements/07-input-validation.md` | Create | Enhancement spec |

---

## Task 1: Scaffold Maven Project with pom.xml

**Files:**
- Create: `product-crud/pom.xml`

- [ ] **Step 1: Create project root directory**

```
D:\POC\Automation\product-crud\
```

Run:
```powershell
New-Item -ItemType Directory -Force "D:\POC\Automation\product-crud\src\main\java\com\tdg\productcrud"
New-Item -ItemType Directory -Force "D:\POC\Automation\product-crud\src\main\resources"
New-Item -ItemType Directory -Force "D:\POC\Automation\product-crud\src\test\java\com\tdg\productcrud"
```

- [ ] **Step 2: Create pom.xml**

Create `D:\POC\Automation\product-crud\pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.5</version>
        <relativePath/>
    </parent>

    <groupId>com.tdg</groupId>
    <artifactId>product-crud</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>product-crud</name>
    <description>Product CRUD POC — Spring Boot 3.2 / Java 17 / MySQL</description>

    <properties>
        <java.version>17</java.version>
    </properties>

    <dependencies>
        <!-- Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- JPA -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <!-- MySQL Driver -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 3: Verify pom.xml parses cleanly**

```powershell
cd D:\POC\Automation\product-crud
mvn help:effective-pom -q
```

Expected: No errors. Maven prints the effective POM.

- [ ] **Step 4: Commit**

```powershell
git -C D:\POC\Automation init
git -C D:\POC\Automation add product-crud/pom.xml
git -C D:\POC\Automation commit -m "chore: scaffold maven project"
```

---

## Task 2: Application Entry Point + application.yml

**Files:**
- Create: `product-crud/src/main/java/com/tdg/productcrud/ProductCrudApplication.java`
- Create: `product-crud/src/main/resources/application.yml`

- [ ] **Step 1: Create application entry point**

Create `src/main/java/com/tdg/productcrud/ProductCrudApplication.java`:

```java
package com.tdg.productcrud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ProductCrudApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProductCrudApplication.class, args);
    }
}
```

- [ ] **Step 2: Create application.yml**

Create `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/productdb?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC
    username: root
    password: yourpassword
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
        format_sql: true

server:
  port: 8080
```

> **Note:** Replace `yourpassword` with your actual MySQL root password before running.

- [ ] **Step 3: Commit**

```powershell
git -C D:\POC\Automation add product-crud/src/
git -C D:\POC\Automation commit -m "chore: add application entry point and yml config"
```

---

## Task 3: Product Entity

**Files:**
- Create: `src/main/java/com/tdg/productcrud/entity/Product.java`

- [ ] **Step 1: Write the failing test first**

Create `src/test/java/com/tdg/productcrud/entity/ProductEntityTest.java`:

```java
package com.tdg.productcrud.entity;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;

class ProductEntityTest {

    @Test
    void prePersist_setsCreatedAt() {
        Product product = new Product();
        product.setName("Laptop");
        product.setCategory("Electronics");
        product.setPrice(new BigDecimal("999.99"));
        product.setStockQuantity(10);

        product.onCreate();

        assertThat(product.getCreatedAt()).isNotNull();
    }
}
```

- [ ] **Step 2: Run test — expect FAIL (class not found)**

```powershell
cd D:\POC\Automation\product-crud
mvn test -Dtest=ProductEntityTest -q
```

Expected: Compilation error — `Product` does not exist yet.

- [ ] **Step 3: Create the entity**

Create `src/main/java/com/tdg/productcrud/entity/Product.java`:

```java
package com.tdg.productcrud.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer stockQuantity;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Integer getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
```

- [ ] **Step 4: Run test — expect PASS**

```powershell
mvn test -Dtest=ProductEntityTest -q
```

Expected: `BUILD SUCCESS`

- [ ] **Step 5: Commit**

```powershell
git -C D:\POC\Automation add product-crud/src/
git -C D:\POC\Automation commit -m "feat: add Product JPA entity"
```

---

## Task 4: DTOs

**Files:**
- Create: `src/main/java/com/tdg/productcrud/dto/ProductRequestDto.java`
- Create: `src/main/java/com/tdg/productcrud/dto/ProductResponseDto.java`

- [ ] **Step 1: Create ProductRequestDto**

Create `src/main/java/com/tdg/productcrud/dto/ProductRequestDto.java`:

```java
package com.tdg.productcrud.dto;

import java.math.BigDecimal;

public class ProductRequestDto {

    private String name;
    private String category;
    private BigDecimal price;
    private Integer stockQuantity;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Integer getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }
}
```

- [ ] **Step 2: Create ProductResponseDto**

Create `src/main/java/com/tdg/productcrud/dto/ProductResponseDto.java`:

```java
package com.tdg.productcrud.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ProductResponseDto {

    private Long id;
    private String name;
    private String category;
    private BigDecimal price;
    private Integer stockQuantity;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Integer getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
```

- [ ] **Step 3: Commit**

```powershell
git -C D:\POC\Automation add product-crud/src/
git -C D:\POC\Automation commit -m "feat: add ProductRequestDto and ProductResponseDto"
```

---

## Task 5: Exception Classes

**Files:**
- Create: `src/main/java/com/tdg/productcrud/exception/ResourceNotFoundException.java`
- Create: `src/main/java/com/tdg/productcrud/exception/GlobalExceptionHandler.java`

- [ ] **Step 1: Create ResourceNotFoundException**

Create `src/main/java/com/tdg/productcrud/exception/ResourceNotFoundException.java`:

```java
package com.tdg.productcrud.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
```

- [ ] **Step 2: Create GlobalExceptionHandler**

Create `src/main/java/com/tdg/productcrud/exception/GlobalExceptionHandler.java`:

```java
package com.tdg.productcrud.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
            "timestamp", LocalDateTime.now().toString(),
            "status", 404,
            "error", "Not Found",
            "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
            "timestamp", LocalDateTime.now().toString(),
            "status", 500,
            "error", "Internal Server Error",
            "message", "An unexpected error occurred"
        ));
    }
}
```

- [ ] **Step 3: Commit**

```powershell
git -C D:\POC\Automation add product-crud/src/
git -C D:\POC\Automation commit -m "feat: add ResourceNotFoundException and GlobalExceptionHandler"
```

---

## Task 6: Repository

**Files:**
- Create: `src/main/java/com/tdg/productcrud/repository/ProductRepository.java`

- [ ] **Step 1: Create ProductRepository**

Create `src/main/java/com/tdg/productcrud/repository/ProductRepository.java`:

```java
package com.tdg.productcrud.repository;

import com.tdg.productcrud.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
}
```

- [ ] **Step 2: Commit**

```powershell
git -C D:\POC\Automation add product-crud/src/
git -C D:\POC\Automation commit -m "feat: add ProductRepository"
```

---

## Task 7: Service Layer

**Files:**
- Create: `src/main/java/com/tdg/productcrud/service/ProductService.java`
- Create: `src/main/java/com/tdg/productcrud/service/ProductServiceImpl.java`
- Create: `src/test/java/com/tdg/productcrud/service/ProductServiceImplTest.java`

- [ ] **Step 1: Write failing unit tests**

Create `src/test/java/com/tdg/productcrud/service/ProductServiceImplTest.java`:

```java
package com.tdg.productcrud.service;

import com.tdg.productcrud.dto.ProductRequestDto;
import com.tdg.productcrud.dto.ProductResponseDto;
import com.tdg.productcrud.entity.Product;
import com.tdg.productcrud.exception.ResourceNotFoundException;
import com.tdg.productcrud.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product product;
    private ProductRequestDto requestDto;

    @BeforeEach
    void setUp() {
        product = new Product();
        product.setId(1L);
        product.setName("Laptop");
        product.setCategory("Electronics");
        product.setPrice(new BigDecimal("999.99"));
        product.setStockQuantity(50);
        product.setCreatedAt(LocalDateTime.now());

        requestDto = new ProductRequestDto();
        requestDto.setName("Laptop");
        requestDto.setCategory("Electronics");
        requestDto.setPrice(new BigDecimal("999.99"));
        requestDto.setStockQuantity(50);
    }

    @Test
    void createProduct_returnsResponseDto() {
        when(productRepository.save(any(Product.class))).thenReturn(product);

        ProductResponseDto result = productService.createProduct(requestDto);

        assertThat(result.getName()).isEqualTo("Laptop");
        assertThat(result.getCategory()).isEqualTo("Electronics");
        assertThat(result.getPrice()).isEqualByComparingTo("999.99");
    }

    @Test
    void getAllProducts_returnsList() {
        when(productRepository.findAll()).thenReturn(List.of(product));

        List<ProductResponseDto> result = productService.getAllProducts();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Laptop");
    }

    @Test
    void getProductById_found_returnsDto() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductResponseDto result = productService.getProductById(1L);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void getProductById_notFound_throwsException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(99L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("99");
    }

    @Test
    void updateProduct_found_updatesAndReturns() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        ProductResponseDto result = productService.updateProduct(1L, requestDto);

        assertThat(result.getName()).isEqualTo("Laptop");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void updateProduct_notFound_throwsException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateProduct(99L, requestDto))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteProduct_found_deletesSuccessfully() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        productService.deleteProduct(1L);

        verify(productRepository).delete(product);
    }

    @Test
    void deleteProduct_notFound_throwsException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.deleteProduct(99L))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
```

- [ ] **Step 2: Run tests — expect FAIL (service classes missing)**

```powershell
mvn test -Dtest=ProductServiceImplTest -q
```

Expected: Compilation error.

- [ ] **Step 3: Create ProductService interface**

Create `src/main/java/com/tdg/productcrud/service/ProductService.java`:

```java
package com.tdg.productcrud.service;

import com.tdg.productcrud.dto.ProductRequestDto;
import com.tdg.productcrud.dto.ProductResponseDto;

import java.util.List;

public interface ProductService {
    ProductResponseDto createProduct(ProductRequestDto requestDto);
    List<ProductResponseDto> getAllProducts();
    ProductResponseDto getProductById(Long id);
    ProductResponseDto updateProduct(Long id, ProductRequestDto requestDto);
    void deleteProduct(Long id);
}
```

- [ ] **Step 4: Create ProductServiceImpl**

Create `src/main/java/com/tdg/productcrud/service/ProductServiceImpl.java`:

```java
package com.tdg.productcrud.service;

import com.tdg.productcrud.dto.ProductRequestDto;
import com.tdg.productcrud.dto.ProductResponseDto;
import com.tdg.productcrud.entity.Product;
import com.tdg.productcrud.exception.ResourceNotFoundException;
import com.tdg.productcrud.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    public ProductServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public ProductResponseDto createProduct(ProductRequestDto requestDto) {
        Product product = toEntity(requestDto);
        return toDto(productRepository.save(product));
    }

    @Override
    public List<ProductResponseDto> getAllProducts() {
        return productRepository.findAll().stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    @Override
    public ProductResponseDto getProductById(Long id) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return toDto(product);
    }

    @Override
    public ProductResponseDto updateProduct(Long id, ProductRequestDto requestDto) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        product.setName(requestDto.getName());
        product.setCategory(requestDto.getCategory());
        product.setPrice(requestDto.getPrice());
        product.setStockQuantity(requestDto.getStockQuantity());
        return toDto(productRepository.save(product));
    }

    @Override
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        productRepository.delete(product);
    }

    private Product toEntity(ProductRequestDto dto) {
        Product p = new Product();
        p.setName(dto.getName());
        p.setCategory(dto.getCategory());
        p.setPrice(dto.getPrice());
        p.setStockQuantity(dto.getStockQuantity());
        return p;
    }

    private ProductResponseDto toDto(Product p) {
        ProductResponseDto dto = new ProductResponseDto();
        dto.setId(p.getId());
        dto.setName(p.getName());
        dto.setCategory(p.getCategory());
        dto.setPrice(p.getPrice());
        dto.setStockQuantity(p.getStockQuantity());
        dto.setCreatedAt(p.getCreatedAt());
        return dto;
    }
}
```

- [ ] **Step 5: Run tests — expect PASS**

```powershell
mvn test -Dtest=ProductServiceImplTest -q
```

Expected: `BUILD SUCCESS`, 8 tests passed.

- [ ] **Step 6: Commit**

```powershell
git -C D:\POC\Automation add product-crud/src/
git -C D:\POC\Automation commit -m "feat: add ProductService interface and ProductServiceImpl"
```

---

## Task 8: REST Controller

**Files:**
- Create: `src/main/java/com/tdg/productcrud/controller/ProductController.java`
- Create: `src/test/java/com/tdg/productcrud/controller/ProductControllerTest.java`

- [ ] **Step 1: Write failing controller tests**

Create `src/test/java/com/tdg/productcrud/controller/ProductControllerTest.java`:

```java
package com.tdg.productcrud.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tdg.productcrud.dto.ProductRequestDto;
import com.tdg.productcrud.dto.ProductResponseDto;
import com.tdg.productcrud.exception.GlobalExceptionHandler;
import com.tdg.productcrud.exception.ResourceNotFoundException;
import com.tdg.productcrud.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {ProductController.class, GlobalExceptionHandler.class})
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @Autowired
    private ObjectMapper objectMapper;

    private ProductResponseDto responseDto;
    private ProductRequestDto requestDto;

    @BeforeEach
    void setUp() {
        responseDto = new ProductResponseDto();
        responseDto.setId(1L);
        responseDto.setName("Laptop");
        responseDto.setCategory("Electronics");
        responseDto.setPrice(new BigDecimal("999.99"));
        responseDto.setStockQuantity(50);
        responseDto.setCreatedAt(LocalDateTime.now());

        requestDto = new ProductRequestDto();
        requestDto.setName("Laptop");
        requestDto.setCategory("Electronics");
        requestDto.setPrice(new BigDecimal("999.99"));
        requestDto.setStockQuantity(50);
    }

    @Test
    void createProduct_returns201() throws Exception {
        when(productService.createProduct(any())).thenReturn(responseDto);

        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Laptop"))
            .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getAllProducts_returns200WithList() throws Exception {
        when(productService.getAllProducts()).thenReturn(List.of(responseDto));

        mockMvc.perform(get("/api/products"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("Laptop"));
    }

    @Test
    void getProductById_found_returns200() throws Exception {
        when(productService.getProductById(1L)).thenReturn(responseDto);

        mockMvc.perform(get("/api/products/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getProductById_notFound_returns404() throws Exception {
        when(productService.getProductById(99L))
            .thenThrow(new ResourceNotFoundException("Product not found with id: 99"));

        mockMvc.perform(get("/api/products/99"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Product not found with id: 99"));
    }

    @Test
    void updateProduct_returns200() throws Exception {
        when(productService.updateProduct(eq(1L), any())).thenReturn(responseDto);

        mockMvc.perform(put("/api/products/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Laptop"));
    }

    @Test
    void deleteProduct_returns204() throws Exception {
        mockMvc.perform(delete("/api/products/1"))
            .andExpect(status().isNoContent());
    }
}
```

- [ ] **Step 2: Run tests — expect FAIL (controller missing)**

```powershell
mvn test -Dtest=ProductControllerTest -q
```

Expected: Compilation error.

- [ ] **Step 3: Create ProductController**

Create `src/main/java/com/tdg/productcrud/controller/ProductController.java`:

```java
package com.tdg.productcrud.controller;

import com.tdg.productcrud.dto.ProductRequestDto;
import com.tdg.productcrud.dto.ProductResponseDto;
import com.tdg.productcrud.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<ProductResponseDto> createProduct(@RequestBody ProductRequestDto requestDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(requestDto));
    }

    @GetMapping
    public ResponseEntity<List<ProductResponseDto>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDto> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponseDto> updateProduct(
            @PathVariable Long id,
            @RequestBody ProductRequestDto requestDto) {
        return ResponseEntity.ok(productService.updateProduct(id, requestDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 4: Run all tests — expect PASS**

```powershell
mvn test -q
```

Expected: `BUILD SUCCESS`, all tests pass.

- [ ] **Step 5: Verify project compiles and packages**

```powershell
mvn clean package -DskipTests -q
```

Expected: `BUILD SUCCESS`, jar created in `target/`.

- [ ] **Step 6: Commit**

```powershell
git -C D:\POC\Automation add product-crud/src/
git -C D:\POC\Automation commit -m "feat: add ProductController with full CRUD endpoints"
```

---

## Task 9: Enhancement Spec Files

**Files:**
- Create: `docs/enhancements/01-search-and-filtering.md`
- Create: `docs/enhancements/02-pagination-and-sorting.md`
- Create: `docs/enhancements/03-jwt-security.md`
- Create: `docs/enhancements/04-swagger-openapi.md`
- Create: `docs/enhancements/05-global-exception-handling.md`
- Create: `docs/enhancements/06-auditing.md`
- Create: `docs/enhancements/07-input-validation.md`

- [ ] **Step 1: Create docs/enhancements/ directory**

```powershell
New-Item -ItemType Directory -Force "D:\POC\Automation\product-crud\docs\enhancements"
```

- [ ] **Step 2: Create 01-search-and-filtering.md**

Create `docs/enhancements/01-search-and-filtering.md`:

```markdown
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
```

- [ ] **Step 3: Create 02-pagination-and-sorting.md**

Create `docs/enhancements/02-pagination-and-sorting.md`:

```markdown
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
```

- [ ] **Step 4: Create 03-jwt-security.md**

Create `docs/enhancements/03-jwt-security.md`:

```markdown
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
```

- [ ] **Step 5: Create 04-swagger-openapi.md**

Create `docs/enhancements/04-swagger-openapi.md`:

```markdown
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

On each method — examples:
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
```

- [ ] **Step 6: Create 05-global-exception-handling.md**

Create `docs/enhancements/05-global-exception-handling.md`:

```markdown
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

## Implementation Notes — handlers to add in `GlobalExceptionHandler`

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
```

- [ ] **Step 7: Create 06-auditing.md**

Create `docs/enhancements/06-auditing.md`:

```markdown
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
For the POC, return a hardcoded username. After JWT (Enhancement 03) is added,
swap this to read from `SecurityContextHolder`.

```java
@Bean
public AuditorAware<String> auditorProvider() {
    return () -> Optional.of("system");
}
```

### Entity changes
Add to `Product`:
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
```

- [ ] **Step 8: Create 07-input-validation.md**

Create `docs/enhancements/07-input-validation.md`:

```markdown
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

    // ... existing getters/setters unchanged
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

`@Valid` triggers Bean Validation. If any constraint fails, Spring throws
`MethodArgumentNotValidException` which `GlobalExceptionHandler` (Enhancement 05)
already handles and returns a `400` with `details`.

## Acceptance Criteria
- [ ] `POST /api/products` with empty `name` returns `400` with `"name: Name must not be blank"` in `details`
- [ ] `POST /api/products` with `price: -5` returns `400` with `"price: Price must be greater than 0"` in `details`
- [ ] `POST /api/products` with `stockQuantity: -1` returns `400` with a stock quantity error
- [ ] Valid request still returns `201` as before
- [ ] Unit test: `ProductControllerTest` — add test cases sending invalid DTOs and asserting `400` + field error messages
```

- [ ] **Step 9: Run full test suite one final time**

```powershell
cd D:\POC\Automation\product-crud
mvn clean test -q
```

Expected: `BUILD SUCCESS`, all tests pass.

- [ ] **Step 10: Commit enhancement specs**

```powershell
git -C D:\POC\Automation add product-crud/docs/
git -C D:\POC\Automation commit -m "docs: add 7 enhancement spec files"
```

---

## Final Verification

- [ ] `mvn clean package -q` — jar builds successfully
- [ ] All unit tests pass (`ProductEntityTest`, `ProductServiceImplTest`, `ProductControllerTest`)
- [ ] `GET /api/products/99` returns `404` with structured JSON error
- [ ] `docs/enhancements/` contains all 7 markdown files
- [ ] Each spec file contains: Goal, Requirements, API Changes, Implementation Notes, Acceptance Criteria
