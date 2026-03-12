# Weather Alert Backend - Architecture Overview

## System Shape

The application uses hexagonal architecture with a Spring Boot runtime and a React dashboard in `ui/`.
Core business logic stays in the domain and application layers, while external concerns are isolated behind adapters.

Current external integrations:

- NOAA Weather API for alerts and forecast/current conditions
- NWPS hydrology data for river gauges
- PostgreSQL for transactional data and the weather read model
- SMTP or AWS SES for email delivery
- Twilio for SMS delivery
- Stripe for subscription billing
- Cloud Scheduler or admins calling `/api/admin/jobs/**` for operational runs

## High-Level View

```text
┌──────────────────────────────────────────────────────────────────────┐
│ External Clients and Services                                       │
├───────────────────────┬────────────────────┬─────────────────────────┤
│ React UI (`ui/`)      │ Cloud Scheduler    │ Swagger / API clients   │
├───────────────────────┼────────────────────┼─────────────────────────┤
│ NOAA / NWPS           │ SMTP / SES /       │ Stripe                  │
│ weather providers     │ Twilio delivery    │ billing                 │
└───────────────┬───────┴──────────────┬─────┴───────────────┬─────────┘
                │                      │                     │
┌───────────────▼──────────────────────▼─────────────────────▼─────────┐
│ Infrastructure Layer                                                 │
│ controllers, config, provider adapters, persistence adapters         │
└───────────────┬──────────────────────┬─────────────────────┬─────────┘
                │                      │                     │
┌───────────────▼──────────────────────▼─────────────────────▼─────────┐
│ Application Layer                                                    │
│ use cases for auth, criteria, alerts, billing, admin jobs, delivery  │
└───────────────┬──────────────────────┬─────────────────────┬─────────┘
                │                      │                     │
┌───────────────▼──────────────────────▼─────────────────────▼─────────┐
│ Domain Layer                                                         │
│ entities, value objects, rule evaluation, ports                      │
└───────────────────────────────────────────────────────────────────────┘
```

## Layer Responsibilities

### Domain

Packages: `src/main/java/com/weather/alert/domain/**`

Responsibilities:

- Weather alert criteria model and rule matching semantics
- Alert lifecycle and dedupe concepts
- User, notification preference, channel verification, and billing domain models
- Port definitions for persistence, weather providers, delivery, and billing
- Rule evaluation and alert orchestration in `AlertProcessingService`

Important ports:

- `AlertRepositoryPort`
- `AlertCriteriaRepositoryPort`
- `AlertCriteriaStateRepositoryPort`
- `WeatherDataPort`
- `WeatherDataSearchPort`
- `AlertDeliveryTaskPublisherPort`
- `EmailSenderPort`
- `SmsSenderPort`

### Application

Packages: `src/main/java/com/weather/alert/application/**`

Responsibilities:

- Use cases coordinating domain logic and adapters
- DTOs used by REST controllers
- Security-sensitive account and recovery flows
- Operational job execution results returned to the UI and scheduler callers

Representative use cases:

- `ManageAlertCriteriaUseCase`
- `QueryAlertsUseCase`
- `ManageUserAccountUseCase`
- `ManageAccountRecoveryUseCase`
- `ManageChannelVerificationUseCase`
- `GetBillingStatusUseCase`
- `RunWeatherAlertProcessingUseCase`
- `PublishDueAlertDeliveryTasksUseCase`
- `RunDataRetentionCleanupUseCase`

### Infrastructure

Packages: `src/main/java/com/weather/alert/infrastructure/**`

Responsibilities:

- REST controllers and OpenAPI annotations
- Spring Security, JWT, rate limiting, scheduling, tracing, and config
- NOAA/NWPS adapters
- JPA persistence adapters
- Delivery adapters for SMTP, SES, and Twilio
- Stripe adapter
- In-process task publishing for alert delivery

Key controller surface:

- `/api/auth/**`
- `/api/users/me`
- `/api/criteria/**`
- `/api/alerts/**`
- `/api/notifications/**`
- `/api/weather/**`
- `/api/admin/users/**`
- `/api/admin/jobs/**`
- `/api/billing/**`
- `/api/stripe/webhook`

## Persistence Model

The app is PostgreSQL-first.

- Transactional entities such as users, criteria, alerts, delivery attempts, verification tokens, and billing state live in PostgreSQL.
- Weather alert search also uses PostgreSQL via `WeatherDataSearchRepositoryAdapter`; this is no longer an Elasticsearch-backed read model.
- Flyway manages schema changes from `src/main/resources/db/migration`.
- Hibernate runs in validation mode (`ddl-auto: validate`) to catch schema drift.

Important persisted concepts:

- `alerts`
- `alert_delivery`
- `alert_criteria`
- `criteria_state`
- `users`
- `channel_verifications`
- `user_notification_preferences`
- `criteria_notification_preferences`
- `account_recovery_tokens`
- `weather_data`

## Core Runtime Flows

### 1. Criteria Creation and Immediate Evaluation

```text
HTTP request
  -> AlertCriteriaController
  -> ManageAlertCriteriaUseCase
  -> AlertCriteriaRepositoryPort.save(...)
  -> AlertProcessingService.processCriteriaImmediately(...)
  -> alert + criteria_state persistence
  -> async delivery task publication
```

Design intent:

- Newly created criteria do not wait for the next scheduler tick.
- If the condition is already true, the user can receive an alert immediately.

### 2. Scheduled or Manual Weather Processing

```text
WeatherAlertScheduler or POST /api/admin/jobs/weather-processing
  -> RunWeatherAlertProcessingUseCase
  -> AlertProcessingService
  -> NOAA/NWPS fetches
  -> weather_data read-model updates
  -> criteria evaluation in batches
  -> deduped alert creation
  -> delivery task publication
```

Important behavior:

- The default cadence is fixed-delay every 5 minutes.
- Criteria are processed in batches.
- Current and forecast lookups are cached per run by coordinate/window.
- Provider outage protection can return `UNAVAILABLE` without mutating anti-spam state.

### 3. Alert Delivery

```text
alert persisted
  -> delivery row persisted
  -> InProcessAlertDeliveryTaskPublisherAdapter
  -> TaskExecutor after transaction commit
  -> ProcessAlertDeliveryTaskUseCase
  -> SMTP / SES / Twilio adapter
  -> SENT or RETRY_SCHEDULED / FAILED
```

This is intentionally in-process today.

- There is no Kafka runtime dependency in the current implementation.
- Retries are persisted and later re-published by `POST /api/admin/jobs/alert-delivery-retries` or the retry poller.
- Permanent failures are logged through the DLQ publisher abstraction.

### 4. Account Security and Recovery

Authentication and recovery are layered:

- Username/password login issues JWTs.
- Magic-link login reuses the recovery-token machinery.
- Forgot-username and forgot-password flows use hashed one-time codes with TTL and cooldowns.
- Email verification is required for normal sign-in.
- Login and recovery flows apply lockouts and throttling.

### 5. Billing

Billing is isolated behind Stripe-focused use cases and adapter code.

- Authenticated users read billing state from `/api/billing/me`.
- Checkout, Customer Portal, and plan changes are explicit POST actions.
- Stripe subscription sync enters through `/api/stripe/webhook`.

## Frontend Relationship

The React app in `ui/` is a first-party client for the backend.

- Vite dev server runs on `http://localhost:5174`.
- Default local proxy target is `http://localhost:8088` for Docker Compose.
- The admin page at `/app/admin` consumes:
  - `/api/admin/users`
  - `/api/admin/jobs/weather-processing`
  - `/api/admin/jobs/alert-delivery-retries`
  - `/api/admin/jobs/data-retention`

In production, the UI Cloud Run service reverse-proxies `/api`, `/actuator`, `/swagger-ui`, and `/v3` to the backend service to keep the browser same-origin.

## Security Model

Current security design:

- JWT bearer auth for most `/api/**` routes
- public bootstrap routes for registration, recovery, magic-link, and Stripe webhook
- role-based authorization using `ROLE_USER` and `ROLE_ADMIN`
- optional machine auth for admin jobs through `X-Admin-Job-Token`
- BCrypt for in-memory configured credentials
- request rate limiting via `ApiRateLimitingFilter`

## Observability and Operations

Built-in operational features:

- Actuator health, info, metrics, and loggers endpoints
- Micrometer tracing with Brave/Zipkin support
- rolling file logs
- admin job endpoints for weather processing, delivery retries, and retention cleanup
- frontend admin console links to Cloud Run logs, metrics, Cloud Build, Monitoring, and Error Reporting

## Testing Strategy

Current test stack:

- JUnit 5 with `spring-boot-starter-test`
- Spring Security test support
- H2 for selected test slices
- MockWebServer for provider integration tests
- RestAssured and OpenAPI contract validation for API coverage

Priority test areas:

- use-case behavior
- controller auth rules
- NOAA/NWPS edge cases
- delivery retry and failure classification
- account recovery and verification flows

## Architecture Constraints

- Keep domain logic out of controllers and adapters.
- Add new integrations behind ports first, then adapters.
- Prefer persistence-backed workflows over instance-local ephemeral state.
- Treat the OpenAPI surface and DTOs as the stable contract for the UI.
