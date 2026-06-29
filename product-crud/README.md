# Product CRUD API

A Spring Boot 3.2 REST API for managing products, built as a POC demonstrating a clean layered architecture with Java 17 and MySQL 8.

---

## Tech Stack

| Technology | Version |
|---|---|
| Java | 17 |
| Spring Boot | 3.2.5 |
| Spring Data JPA | (via Boot parent) |
| MySQL | 8.x |
| Maven | 3.x |
| JUnit 5 + Mockito | (via Boot parent) |

---

## Project Structure

```
product-crud/
├── src/
│   ├── main/
│   │   ├── java/com/tdg/productcrud/
│   │   │   ├── ProductCrudApplication.java     # Entry point
│   │   │   ├── controller/
│   │   │   │   └── ProductController.java      # REST endpoints
│   │   │   ├── service/
│   │   │   │   ├── ProductService.java         # Service interface
│   │   │   │   └── ProductServiceImpl.java     # Business logic
│   │   │   ├── repository/
│   │   │   │   └── ProductRepository.java      # JPA repository
│   │   │   ├── entity/
│   │   │   │   └── Product.java                # JPA entity
│   │   │   ├── dto/
│   │   │   │   ├── ProductRequestDto.java      # Inbound request body
│   │   │   │   └── ProductResponseDto.java     # Outbound response body
│   │   │   └── exception/
│   │   │       ├── ResourceNotFoundException.java
│   │   │       └── GlobalExceptionHandler.java # @RestControllerAdvice
│   │   └── resources/
│   │       └── application.yml
│   └── test/
│       └── java/com/tdg/productcrud/
│           ├── entity/ProductEntityTest.java
│           ├── service/ProductServiceImplTest.java
│           └── controller/ProductControllerTest.java
└── docs/
    └── enhancements/                           # 7 feature spec files
```

---

## Prerequisites

- Java 17+
- Maven 3.6+
- MySQL 8 running locally

---

## Setup

**1. Create the database**

```sql
CREATE DATABASE productdb;
```

**2. Configure credentials**

Open `src/main/resources/application.yml` and update:

```yaml
spring:
  datasource:
    username: your_mysql_username
    password: your_mysql_password
```

**3. Build**

```bash
mvn clean package
```

**4. Run**

```bash
mvn spring-boot:run
```

The application starts on `http://localhost:8080`.

> Hibernate will auto-create the `products` table on first run (`ddl-auto: update`).

---

## API Reference

Base URL: `http://localhost:8080/api/products`

### Create a product
```
POST /api/products
Content-Type: application/json

{
  "name": "Laptop",
  "category": "Electronics",
  "price": 999.99,
  "stockQuantity": 50
}
```
Response: `201 Created`

---

### Get all products
```
GET /api/products
```
Response: `200 OK` — array of products

---

### Get product by ID
```
GET /api/products/{id}
```
Response: `200 OK` or `404 Not Found`

---

### Update a product
```
PUT /api/products/{id}
Content-Type: application/json

{
  "name": "Laptop Pro",
  "category": "Electronics",
  "price": 1299.99,
  "stockQuantity": 30
}
```
Response: `200 OK` or `404 Not Found`

---

### Delete a product
```
DELETE /api/products/{id}
```
Response: `204 No Content` or `404 Not Found`

---

## Error Response Format

All errors return a consistent JSON shape:

```json
{
  "timestamp": "2026-06-29T10:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Product not found with id: 5"
}
```

---

## Running Tests

```bash
mvn test
```

16 tests across 3 test classes:

| Test Class | Count | What it tests |
|---|---|---|
| `ProductEntityTest` | 1 | `@PrePersist` sets `createdAt` |
| `ProductServiceImplTest` | 8 | All service methods + not-found cases |
| `ProductControllerTest` | 7 | HTTP status codes, request/response JSON, 404 handling |

---

## Data Model

**Table:** `products`

| Column | Type | Constraint |
|---|---|---|
| `id` | BIGINT | PK, auto-increment |
| `name` | VARCHAR | NOT NULL |
| `category` | VARCHAR | NOT NULL |
| `price` | DECIMAL | NOT NULL |
| `stock_quantity` | INT | NOT NULL |
| `created_at` | DATETIME | Set on insert, not updated |

---

## Planned Enhancements

Seven feature specs are ready under `docs/enhancements/` — pick one up to extend the app:

| # | File | Feature |
|---|---|---|
| 1 | `01-search-and-filtering.md` | Filter by category and price range (JPA Specifications) |
| 2 | `02-pagination-and-sorting.md` | Page and sort the product list |
| 3 | `03-jwt-security.md` | Protect endpoints with JWT auth |
| 4 | `04-swagger-openapi.md` | Interactive API docs at `/swagger-ui.html` |
| 5 | `05-global-exception-handling.md` | Richer error responses (validation, DB constraint errors) |
| 6 | `06-auditing.md` | Track `createdBy` / `lastModifiedBy` automatically |
| 7 | `07-input-validation.md` | Bean Validation on request DTOs |



# Auto Dev Pipeline

An autonomous development pipeline that takes a Markdown requirements file from any local directory, and drives it all the way to a merged PR — with two mandatory human review gates.

---

## Pipeline Overview

```
Local MD File
     │
     ▼
[kickoff.sh] ──── Creates branch ──── Uploads MD ──── Triggers GH Action
                                                              │
                                                              ▼
                                                   ┌─────────────────────┐
                                                   │  Workflow 1         │
                                                   │  Generate Design    │
                                                   │  Doc (Claude API)   │
                                                   └────────┬────────────┘
                                                            │ Opens Design PR
                                                            ▼
                                              ┌─────────────────────────────┐
                                              │  🧑 HUMAN GATE 1            │
                                              │  Review design doc PR       │
                                              │  /approve-design            │
                                              │  /revise-design: <feedback> │
                                              └────────────┬────────────────┘
                                    ┌───────── revise ─────┘    │ approve
                                    │  (Workflow 2 iterates)    ▼
                                    │                  ┌─────────────────────┐
                                    │                  │  Workflow 3         │
                                    │                  │  Generate Code      │
                                    │                  │  (Claude API)       │
                                    │                  └────────┬────────────┘
                                    │                           │ Opens Code PR
                                    │                           ▼
                                    │               ┌────────────────────────────┐
                                    │               │  🧑 HUMAN GATE 2           │
                                    │               │  Standard GitHub PR review │
                                    │               │  /revise-code: <feedback>  │
                                    └──── revise ───┤  (Workflow 4 iterates)     │
                                                    │  Approve & Merge ✅        │
                                                    └────────────────────────────┘
```

---

## Setup

### 1. GitHub Secrets

Add these to your repository (Settings → Secrets → Actions):

| Secret | Value |
|--------|-------|
| `GH_PAT` | GitHub Personal Access Token (org-scoped, with `repo`, `workflow` permissions) |
| `ANTHROPIC_API_KEY` | Your Anthropic API key |

### 2. Copy workflows to your repo

```bash
cp .github/workflows/*.yml  /path/to/your-spring-boot-repo/.github/workflows/
cp .github/copilot-instructions.md  /path/to/your-spring-boot-repo/.github/
```

> **Important:** Update `.github/copilot-instructions.md` to reflect your actual project's package names, conventions, and patterns. This is what keeps AI-generated code consistent with your codebase.

### 3. Install prerequisites (local)

```bash
# GitHub CLI
brew install gh          # macOS
# or: https://cli.github.com/

# Authenticate gh CLI
gh auth login

# Make kickoff script executable
chmod +x scripts/kickoff.sh
```

### 4. Set environment variables (local)

Add to your `~/.bashrc` or `~/.zshrc`:

```bash
export GITHUB_TOKEN="ghp_your_pat_here"
export GITHUB_REPO="your-org/your-spring-boot-repo"
```

---

## Usage

### Start the pipeline

```bash
# Feature branch
./scripts/kickoff.sh --md /any/local/path/user-profile-api.md --type feature

# Bug fix branch
./scripts/kickoff.sh --md /any/local/path/fix-login-timeout.md --type bugfix

# Custom base branch
./scripts/kickoff.sh --md /path/to/req.md --type feature --base develop
```

The branch name is derived from the MD filename:
- `user-profile-api.md` → `feature/user-profile-api`
- `fix-login-timeout.md` → `bugfix/fix-login-timeout`

---

### Design Review Gate

After the pipeline runs, a PR titled `🎨 [Design Review] {ticket-id}` will appear.

Review the generated `docs/design/{ticket-id}.md` and comment:

```
/approve-design
```
or
```
/revise-design: Add pagination support to the list endpoint, and include caching strategy
```

You can iterate with `/revise-design` as many times as needed. Each comment triggers a new AI revision pushed to the same PR.

---

### Code Review Gate

After design approval, a PR titled `🚀 [Code Review] {ticket-id}` will appear.

This is a **standard GitHub PR** — review the diff normally. For AI-assisted iteration:

```
/revise-code: Extract the mapping logic into a separate mapper class and add null checks
```

Once satisfied, approve and merge as usual.

---

## File Structure (what gets created in your repo)

```
your-repo/
├── requirements/
│   └── {ticket-id}.md          ← your original MD file, committed here
├── docs/
│   └── design/
│       └── {ticket-id}.md      ← AI-generated design doc
└── src/
    └── main/java/...           ← AI-generated Spring Boot code
    └── test/java/...           ← AI-generated tests
```

---

## Workflow Reference

| Workflow File | Trigger | What it does |
|---|---|---|
| `1-generate-design.yml` | `repository_dispatch: generate-design` | Generates design doc PR |
| `2-design-review-handler.yml` | PR comment `/approve-design` or `/revise-design:` | Approves or iterates design |
| `3-generate-code.yml` | `repository_dispatch: generate-code` | Generates Spring Boot code PR |
| `4-code-review-handler.yml` | PR comment `/revise-code:` | Iterates code on same PR |

---

## Extending to Jira

When you're ready to add Jira ticket ID support, the `kickoff.sh` script accepts `--jira PROJ-123` (add this flag). The Atlassian MCP server (already connected to Claude.ai) can fetch ticket content. The pipeline architecture stays identical — only the input source changes.