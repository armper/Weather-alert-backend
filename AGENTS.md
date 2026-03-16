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
- `cd ui && npm ci && npm run lint && npm run build`: frontend CI-equivalent check; `npm run build` includes the TypeScript compile gate.

## Coding Style & Naming Conventions
- Java 17 + Spring Boot 3.x; use 4-space indentation and UTF-8 source files.
- Keep package names lowercase (`com.weather.alert...`), classes in PascalCase, methods/fields in camelCase.
- Match existing suffix conventions: `*Controller`, `*UseCase`, `*Adapter`, `*Repository`, `*Entity`, `*Response`, `*Request`.
- Prefer constructor injection and keep domain logic in `domain/`, not in controllers/adapters.

## Frontend UX Direction
- Design consumer-facing pages for clarity first. Reduce cognitive load, remove nonessential copy, and make the primary action obvious.
- Prefer strong visual hierarchy: one dominant action, a small number of supporting actions, and generous spacing between groups.
- Keep public-facing screens simple enough for non-technical users. Avoid feature walls, dense explanatory text, and layouts that require interpretation.
- When creating or refreshing pages, favor cohesive visual systems over ad hoc components. Inputs, buttons, surfaces, spacing, and helper text should feel like part of one family.
- Use decorative/brand visuals to support the experience, but keep interaction elements easy to recognize and separate from background art.
- Prefer lightweight, contextual guidance. If a field or action needs explanation, show a small inline hint near that field only when needed instead of large warning blocks.
- Avoid silent disabled states. When an action is unavailable, provide a nearby reason in plain language.
- For dashboards and utility screens, compress secondary controls and explanations. Prefer modals, popovers, and compact summaries over large persistent instruction sections.
- When updating older pages, simplify before adding. Remove low-value elements, shorten copy, and tighten action hierarchy before introducing new UI.

## Testing Guidelines
- Framework stack: JUnit 5 via `spring-boot-starter-test`, plus Spring Security and Kafka test support.
- Name tests `*Test` and place them in mirrored package paths.
- Default command: `mvn test`.
- For feature work, add focused tests for use cases, security behavior, and adapter edge cases (e.g., NOAA/Kafka failures).
- Before merging to `main`, the minimum required local validation is:
  - backend: `mvn test`
  - frontend: `cd ui && npm ci && npm run lint && npm run build`
- Treat TypeScript compile errors, ESLint errors, and failing backend tests as merge blockers. Do not merge with red checks.

## Commit & Pull Request Guidelines
- Follow Conventional Commit style seen in history: `feat:`, `fix:`, `refactor:`, `test:`, `chore:`, `docs:`.
- Keep commits scoped and descriptive (one logical change per commit).
- Do not push directly to `main` for feature or fix work. Open a pull request and wait for CI to pass before merging.
- PRs should include:
  - concise summary and motivation,
  - linked issue/ticket (if applicable),
  - test evidence (`mvn test` output or equivalent),
  - API/config updates reflected in docs (`README.md`, `API.md`) when behavior changes.

## Security & Configuration Tips
- Do not commit credentials. Configure auth via environment variables (for example `APP_SECURITY_JWT_SECRET`, `APP_SECURITY_USER_USERNAME`).
- Use test-specific configs in `src/test/resources/application-test.yml` for local and CI safety.

## Google Cloud Deployment
- GCP project ID: `weather-alerts-panda`.
- Cloud Build triggers live in **us-east1** region. Two triggers fire on push to `main`:
  - `backend-main` → uses `cloudbuild.yaml` (builds & deploys the Spring Boot backend).
  - `ui-main` → uses `ui/cloudbuild.yaml` (builds & deploys the frontend).
- GitHub Actions should be the pre-merge gate. Cloud Build is the post-merge deployment path and should not be relied on to catch preventable TypeScript, lint, or test failures after code is already on `main`.
- Useful commands:
  - List recent builds: `gcloud builds list --project=weather-alerts-panda --region=us-east1 --limit=5 --format='table(id,status,startTime,duration,substitutions.TRIGGER_NAME)'`
  - List triggers: `gcloud builds triggers list --project=weather-alerts-panda --region=us-east1`
  - Stream build logs: `gcloud builds log --project=weather-alerts-panda --region=us-east1 <BUILD_ID> --stream`
