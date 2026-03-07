# Project Gaps TODO

Prioritized from highest value to lowest based on production risk, user-facing breakage, and confidence from the current repository state.

- [ ] P0: Make runtime coordination and abuse controls safe for multi-instance deployments
  Why this matters: the current design will behave differently once the app is scaled horizontally. Duplicate schedulers can re-run polling, retention cleanup, and retry publishing on every replica, while rate limiting and auth lockouts only exist in local memory per node.
  Evidence:
  - `src/main/java/com/weather/alert/infrastructure/config/WeatherAlertScheduler.java`
  - `src/main/java/com/weather/alert/infrastructure/config/DataRetentionScheduler.java`
  - `src/main/java/com/weather/alert/infrastructure/config/AlertDeliveryRetryScheduler.java`
  - `src/main/java/com/weather/alert/infrastructure/config/ApiRateLimitingFilter.java`
  - `src/main/java/com/weather/alert/application/service/AuthSecurityGuardService.java`
  - `DEPLOYMENT.md` suggests scaling replicas to 5.
  TODO:
  - Add distributed scheduler locking or leader election.
  - Move rate-limit and auth-throttle state to shared storage such as Redis or the database.
  - Add `Retry-After` support and instance-safe throttling tests.

- [ ] P0: Resolve the notification-channel mismatch before exposing `SMS` and `PUSH` as supported options
  Why this matters: the API and preference model allow channels that the verification and delivery path do not actually support. That creates broken user flows and false expectations.
  Evidence:
  - `src/main/java/com/weather/alert/infrastructure/web/controller/NotificationPreferenceController.java` exposes `EMAIL`/`SMS` examples.
  - `src/main/java/com/weather/alert/application/usecase/ManageChannelVerificationUseCase.java` only accepts `EMAIL`.
  - `src/main/java/com/weather/alert/application/usecase/ProcessAlertDeliveryTaskUseCase.java` fails any non-`EMAIL` delivery as unsupported.
  - `src/main/java/com/weather/alert/domain/service/notification/NotificationPreferenceResolverService.java` treats `PUSH` as implicitly verified.
  TODO:
  - Either implement SMS/push verification and delivery end to end, or remove/feature-flag those channels from public APIs and UI.
  - Add contract tests for unsupported-channel behavior.

- [ ] P0: Add production-like integration coverage with Flyway and real infrastructure containers
  Why this matters: the current tests pass, but they do so against H2 with `ddl-auto=create-drop` and Flyway disabled. That leaves migration drift, PostgreSQL behavior, Kafka wiring, and Elasticsearch integration under-tested.
  Evidence:
  - `src/test/resources/application.yml` uses H2 and `spring.jpa.hibernate.ddl-auto=create-drop`.
  - `src/test/resources/application.yml` and `src/test/resources/application-test.yml` disable Flyway.
  - `mvn test` currently passes, but it does not validate the real production schema path.
  TODO:
  - Add Testcontainers-based integration tests for PostgreSQL, Kafka, and Elasticsearch.
  - Run Flyway migrations in test startup.
  - Add at least one end-to-end happy path covering registration, approval, criteria creation, alert generation, enqueue, and delivery state changes.

- [ ] P1: Finish observability so the existing metrics and tracing hooks become operationally useful
  Why this matters: the code already emits Micrometer metrics and tracing data, but the repo stops short of a complete observable stack. Grafana provisioning is minimal, and Prometheus scraping is documented but not wired into the build/runtime.
  Evidence:
  - Metrics exist in `src/main/java/com/weather/alert/domain/service/AlertProcessingService.java` and `src/main/java/com/weather/alert/infrastructure/adapter/noaa/NoaaWeatherAdapter.java`.
  - `observability/grafana/provisioning/datasources/loki.yml` is present, but there are no dashboards provisioned.
  - `DEPLOYMENT.md` documents Prometheus setup, but `pom.xml` does not include `micrometer-registry-prometheus`.
  - `README.md` still calls out pending observability work in Chunk 9.
  TODO:
  - Add Prometheus registry dependency and expose `/actuator/prometheus`.
  - Provision Grafana dashboards for scheduler health, NOAA failures, alert throughput, retries, and DLQ volume.
  - Define alert thresholds and an operator runbook.

- [ ] P1: Remove documentation and deployment drift
  Why this matters: the repo contains multiple competing truths about ports, security posture, and runtime expectations. That slows onboarding and increases deployment mistakes.
  Evidence:
  - `README.md` says the UI proxies to `http://localhost:8092` by default.
  - `ui/vite.config.ts` defaults to `http://localhost:8088`.
  - `DEPLOYMENT.md` still includes a security checklist item list for features that are already implemented, such as JWT auth, rate limiting, input validation, and Spring Security.
  - `Dockerfile` builds/runs on Java 21 while project docs are framed around Java 17.
  TODO:
  - Align README, UI docs, compose defaults, and deployment docs on one local/dev topology.
  - Replace outdated “missing feature” checklists with current architecture and operational constraints.
  - Document the supported Java runtime policy explicitly.

- [ ] P1: Add a real production path for frontend-to-backend connectivity
  Why this matters: the current frontend story is strong for local dev because Vite proxies requests, but there is no equivalent backend CORS strategy or documented reverse-proxy setup for separate frontend hosting.
  Evidence:
  - `ui/vite.config.ts` relies on Vite proxy for `/api`, `/actuator`, `/swagger-ui`, and `/v3`.
  - No backend CORS configuration is present in `src/main/java/com/weather/alert/infrastructure/config`.
  - `DEPLOYMENT.md` still lists “Enable CORS properly” as unfinished.
  TODO:
  - Decide on the production model: same-origin reverse proxy, static hosting behind a gateway, or explicit CORS.
  - Implement the corresponding Spring CORS configuration if cross-origin access is required.
  - Add a deployment example for the UI.

- [x] P2: Disable Open Session in View and tighten API behavior around infrastructure concerns
  Why this matters: tests show Spring is still running with `open-in-view` enabled, which can hide lazy-loading and query-boundary problems. The rate-limit filter also returns plain text instead of the structured error format used elsewhere.
  Evidence:
  - `mvn test` logs show `spring.jpa.open-in-view is enabled by default`.
  - `src/main/java/com/weather/alert/infrastructure/config/ApiRateLimitingFilter.java` returns plain text `Rate limit exceeded` instead of problem JSON.
  Status:
  - `spring.jpa.open-in-view=false` is now set in runtime and test configuration.
  - `ApiRateLimitingFilter` now returns RFC7807-style `application/problem+json` responses and emits `Retry-After`.
  - Regression coverage exists in `ApiRateLimitingFilterTest`; integration contract tests pass with the updated JPA/session configuration.
