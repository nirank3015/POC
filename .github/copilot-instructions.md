# Project overview

WOAD (Wiley Open Access Dashboard) is an enterprise application that manages funding requests in an academic/research ecosystem. The system integrates with multiple services and applications including:

- Viax
- Product API
- Group Service
- WALS
- Participants Service
- AWS S3
- MySQL Database


The application primarily handles funding request processing workflows, where requests go through various states (Pending, Approval in Process, Approved, Denial in Process, Denied, Cancelled) with specific business rules and integrations controlling these transitions.

## Code review 

When reviewing code changes, ensure:

1. Documentation Alignment: Any changes to business logic, workflows, or integrations must be reflected in the corresponding documentation under the `/docs` folder. Pay special attention to:
   - [Account Configuration](../docs/account_configuration.md)
   - [Eligibility Service](../docs/eligibility_service.md)
   - [Funding Requests](../docs/funding_requests.md)
   - [Security Management](../docs/security_management.md)
   - [Article Data](../docs/updating_article_data.md)

   For enum changes specifically:
   1. Search through ALL documentation files in the /docs folder for references to the changed enum:
      - Check both v2/ and root documentation folders
      - Look for both direct enum names and their business concepts
      - Example: for FundingRequestStatusReasonCode, search for:
        - The exact enum name
        - "status reason"
        - "request status"
        - "reason code"
        - Related business concepts

   2. Determine documentation impact by checking:
      - Is it used in configuration files or spreadsheets?
      - Does it represent a business state or user-visible status?
      - Is it part of external integrations or APIs?
      - Does it affect business flows or decision points?
      - Is it referenced in UI messages or notifications?

   3. Documentation Update Rules:
      - Primary Feature Documentation:
        - Document the enum where it's most relevant to the business flow
        - Example: FundingRequestStatusReasonCode in funding_requests.md where status transitions are explained
        - Include real examples of how different values affect the process

      - Cross-reference in Related Features:
        - Check if the enum affects multiple features
        - Example: A status change might need documentation in:
          - funding_requests.md (main flow)
          - notifying_admins_and_users.md (if it triggers notifications)
          - processing_funding_requests.md (if it affects processing)

      - Configuration Documentation:
        - If used in config files/spreadsheets, document in relevant config guide
        - Include impact of each value on system behavior
        - Add examples of common configurations

      - Integration Documentation:
        - If used in APIs/external systems, document in integration_schema.md
        - Include how different values affect integration behavior

   4. Documentation Content Requirements:
      - Business context of the enum
      - Complete list of possible values
      - Impact of each value on the system
      - Real-world examples of usage
      - Related configurations or states
      - Any dependencies or constraints

## Folder structure

The project follows a modular Maven structure with several key components:

- `WOAD-Admin-app`: Administrative interface and management functionality
- `WOAD-ICL-app`: Main application interface and controllers
- `WOAD-Model`: Core domain models and entities
- `WOAD-Services`: Business logic and service implementations
- `WOAD-Integration`: Integration layer with external services
- `WOAD-JAXB`: XML/JSON binding configurations
- `WOAD-Liquibase`: Database migration and versioning
- `WOAD-WebFlux-Client`: Reactive web client implementations
- `docs/`: Comprehensive documentation covering:
  - Key concepts
  - Data models
  - Integration schemas
  - Processing workflows
  - Security management

## Libraries and Frameworks

The application is built using:

- Spring Framework ecosystem
- Apache Wicket (v9.12.0) for web interface
- MySQL for data persistence
- JaCoCo for code coverage
- MapStruct (v1.6.3) for object mapping
- Apache POI for document processing
- Quartz for job scheduling
- JAXB for XML processing
- Maven for build and dependency management

## Build
Java 17, Maven wrapper (`./mvnw`), 10 modules. Annotation processors: Lombok + MapStruct + Hibernate modelgen. Profiles: `skip-all-checks` (skip tests+checkstyle+spotbugs), `jacoco-gen` (coverage), `mock`, `deploy`, `war`. CI build: `mvn -T 2C -P war install`.

**Before running any `./mvnw` command**, activate the correct JDK (`.sdkmanrc` pins `java=17.0.18-tem`):
```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env
```

Integration with various Wiley services including:
- Product Service
- Auth Service
- Group Service
