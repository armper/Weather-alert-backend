# Repository Guidelines

## Project Structure & Module Organization
- Core code lives in `src/main/java/com/weather/alert` and follows hexagonal layering:
  - `domain/`: business models, domain services, and port interfaces.
  - `application/`: use cases and DTOs.
  - `infrastructure/`: adapters (NOAA, Kafka, persistence, Elasticsearch), web controllers, and config.
- Runtime configuration is in `src/main/resources/application.yml`.
- Tests mirror production packages under `src/test/java`; test configs are in `src/test/resources`.
- Architecture and API references: `ARCHITECTURE.md`, `API.md`, `DEPLOYMENT.md`.

## Build, Test, and Development Commands
- `mvn clean install`: full build, runs unit/integration tests, produces JAR in `target/`.
- `mvn test`: run test suite only.
- `mvn spring-boot:run`: start backend locally on port `8080`.
- `java -jar target/weather-alert-backend-0.0.1-SNAPSHOT.jar`: run packaged artifact.
- `mvn clean package -DskipTests`: fast packaging when tests are intentionally deferred.

## Coding Style & Naming Conventions
- Java 17 + Spring Boot 3.x; use 4-space indentation and UTF-8 source files.
- Keep package names lowercase (`com.weather.alert...`), classes in PascalCase, methods/fields in camelCase.
- Match existing suffix conventions: `*Controller`, `*UseCase`, `*Adapter`, `*Repository`, `*Entity`, `*Response`, `*Request`.
- Prefer constructor injection and keep domain logic in `domain/`, not in controllers/adapters.

## Testing Guidelines
- Framework stack: JUnit 5 via `spring-boot-starter-test`, plus Spring Security and Kafka test support.
- Name tests `*Test` and place them in mirrored package paths.
- Default command: `mvn test`.
- For feature work, add focused tests for use cases, security behavior, and adapter edge cases (e.g., NOAA/Kafka failures).

## Commit & Pull Request Guidelines
- Follow Conventional Commit style seen in history: `feat:`, `fix:`, `refactor:`, `test:`, `chore:`, `docs:`.
- Keep commits scoped and descriptive (one logical change per commit).
- PRs should include:
  - concise summary and motivation,
  - linked issue/ticket (if applicable),
  - test evidence (`mvn test` output or equivalent),
  - API/config updates reflected in docs (`README.md`, `API.md`) when behavior changes.

## Security & Configuration Tips
- Do not commit credentials. Configure auth via environment variables (for example `APP_SECURITY_JWT_SECRET`, `APP_SECURITY_USER_USERNAME`).
- Use test-specific configs in `src/test/resources/application-test.yml` for local and CI safety.
