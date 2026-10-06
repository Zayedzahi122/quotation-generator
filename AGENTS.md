# AGENTS.md

High-signal instructions for working in this repository.

## Tech Stack & Configuration
- **Java**: JDK 25, **Spring Boot 4.1.1**, **Maven** (do not downgrade; confirmed by `pom.xml`)
- **Base package**: `com.riyalo.quotation`
- **Config**: `src/main/resources/application.yaml` (YAML). Do not create `application.properties` unless explicitly requested.
- **Database**: H2 (local/dev)

## Architecture & Conventions
- **Layers**: controller → service → repository. No business logic in controllers.
- **Package layout**: under `com.riyalo.quotation` (controllers, services, repositories, entities/models, dtos, mappers, config, enums, exception, util as needed). Keep consistent with existing style.
- **Money**: Always use `BigDecimal` with **3 decimals** (OMR). Never use `double` or `float`. Use `RoundingMode.HALF_UP`.
- **VAT**: Config-driven in `application.yaml` with default 5%. VAT must remain optional and may only be added if/when required. Do not hardcode VAT. Only apply VAT when explicitly configured/enabled; support per-quotation override if VAT is present.
- **Template layouts**: Support selectable quotation layout designs (e.g. CLASSIC, MODERN, COMPACT). Store the selected layout on the entity; render via Thymeleaf template paths based on the chosen layout (share common fragments). Add only if needed; otherwise keep minimal.

## Build & Test Commands
- **Run all tests**: `mvn test` (run after every change and fix failures)
- **Compile only**: `mvn compile`
- **Clean build**: `mvn clean test` (if tests misbehave)

## Testing Requirements
- **Unit tests required**: Write a unit test for every service method. Place tests under `src/test/java` mirroring the service package. Use JUnit 5; prefer mocking for services.
- **Verification**: Run `mvn test` after every code change and fix failures before proceeding.

## Development Notes
- Preserve existing code style, naming, and structure. Make minimal, consistent changes.
- Trust executable sources (pom.xml, mvnw, configs) over docs if conflicting.
- If unsure about a path or file, ask - do not guess.
- Make all code edits inside the working directory `C:\Users\Service-Riyalo\downloads\quotation-generator\Quotation-Generator`. Do not modify outside this folder.
- Follow Spring Boot + Maven conventions. Avoid speculative code.