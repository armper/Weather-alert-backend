# Weather Alert Backend

A sophisticated weather alert backend system built with Java 17, Spring Boot, and hexagonal clean architecture. This system integrates with NOAA's weather API to fetch real-time weather alerts and sends custom notifications to users based on their personalized criteria and thresholds.

## Architecture

This application follows **Hexagonal (Ports and Adapters) Clean Architecture** principles, ensuring:
- Clear separation of concerns
- Independence from frameworks and external systems
- Testability
- Maintainability

### Layers

1. **Domain Layer** (`domain/`)
   - Core business logic and models
   - Domain services
   - Port interfaces (contracts for external dependencies)

2. **Application Layer** (`application/`)
   - Use cases (business operations)
   - DTOs for API communication
   - CQRS pattern implementation (separate read/write operations)

3. **Infrastructure Layer** (`infrastructure/`)
   - Adapters implementing domain ports
   - NOAA API integration
   - PostgreSQL persistence
   - Kafka messaging
   - Elasticsearch indexing
   - REST API controllers

## Features

- ✅ **Real-time Weather Data**: Integration with NOAA Weather API (https://api.weather.gov/)
- ✅ **Custom Alert Criteria**: Users can define personalized alert conditions
- ✅ **Multi-criteria Matching**: Location, event type, severity, temperature, wind speed, precipitation
- ✅ **Current + Forecast Monitoring Controls**: Criteria can target current conditions, forecast, or both
- ✅ **Temperature Unit Preference**: Thresholds support Fahrenheit and Celsius
- ✅ **NOAA Conditions Pipeline**: Current observations + hourly forecast retrieval by coordinate
- ✅ **One-Time-Per-Event Anti-Spam**: Edge-triggering + rearm window prevents duplicate alert noise
- ✅ **Immediate Evaluation on Criteria Create**: If conditions are already true, alerting can happen right away
- ✅ **Alert Lifecycle + Dedupe**: `PENDING -> SENT -> ACKNOWLEDGED/EXPIRED` with `criteriaId + eventKey` duplicate protection
- ✅ **Geographic Filtering**: Radius-based location matching using Haversine formula
- ✅ **Async Processing**: Kafka-based message queue for scalable alert processing
- ✅ **Search Capabilities**: Elasticsearch integration for fast weather data queries
- ✅ **CQRS Pattern**: Separate command and query operations for optimal performance
- ✅ **Scheduled Updates**: Automatic weather data fetching every 5 minutes
- ✅ **Automatic Data Retention Cleanup**: Scheduled pruning of old alerts, criteria state, indexed weather data, and Kafka topic backlog
- ✅ **RESTful API**: Comprehensive REST endpoints for all operations
- ✅ **JWT Authentication**: Secure API access with bearer tokens and role-based authorization
- ✅ **WebSocket Updates**: STOMP endpoint for real-time weather alert streams
- ✅ **Swagger UI**: Interactive API documentation for exploring and testing endpoints
- ✅ **API Integration + Contract Tests**: RestAssured suite with OpenAPI response/request validation
- ✅ **Email Verification Flow**: Start/confirm verification tokens for channel readiness
- ✅ **Verified Channel Resolution**: Unverified channels are excluded from delivery preference resolution
- ✅ **Email Delivery Adapters**: SMTP (MailHog/local) and AWS SES (production) with retryability classification

## Technology Stack

- **Java 17**: LTS version with modern language features
- **Spring Boot 3.2.2**: Framework for building production-ready applications
- **PostgreSQL**: Relational database for persistent storage
- **Apache Kafka**: Distributed message streaming platform
- **Elasticsearch**: Search and analytics engine
- **Maven**: Dependency management and build tool
- **Lombok**: Reduces boilerplate code
- **WebFlux**: Reactive HTTP client for NOAA API calls

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- PostgreSQL 14+
- Apache Kafka 3.0+
- Elasticsearch 8.0+

## Roadmap

Implementation tracking for weather-condition alerts (temperature/rain current + forecast) lives in `IMPLEMENTATION_TODO.md`.
Notification delivery tracking (email-first with SMS-ready channel preferences) lives in `NOTIFICATION_DELIVERY_TODO.md`.

### Implementation Status (from checked TODO items)

- ✅ Chunk 1: Flyway baseline migration + migration-driven schema workflow
- ✅ Chunk 2: Criteria model extensions (temperature/rain thresholds, monitor current/forecast, unit + cadence controls)
- ✅ Chunk 3: NOAA current observations + hourly forecast integration with timeout/retry/fallback
- ✅ Chunk 4: Rule-evaluation engine refactor for explicit current/forecast condition evaluation
- ✅ Chunk 5: Anti-spam state model (`criteria_state`) with edge-trigger + rearm behavior
- ✅ Chunk 6: Alert persistence lifecycle, event keys, and dedupe queries/indexes
- ✅ Chunk 7: API/validation updates and criteria query filters
- ✅ Chunk 8: Scheduler batching, provider pacing/outage guard, and evaluation metrics/logging
- ⏳ Pending in TODO: Chunk 9 (expanded observability dashboards) and Chunk 10 (end-to-end matrix + manual playbook)

## Changelog

### 2026-02-26 (Data Retention + Cleanup)

- Added scheduled retention cleanup with configurable TTLs:
  - PostgreSQL `alerts` rows older than retention window
  - PostgreSQL `criteria_state` rows older than retention window
  - Orphaned `criteria_state` rows for deleted criteria
  - Elasticsearch `weather-data` documents older than retention window
- Added DB index `idx_alerts_alert_time` to keep alert pruning efficient.
- Added Kafka topic retention policy for `weather-alerts` in `docker-compose.yml` (`retention.ms=86400000`).
- Added retention configuration knobs under `app.retention.*` with `.env` support.

### 2026-02-26 (Notification Delivery Foundations - Chunk 1)

- Added Flyway migration `V6__add_notification_delivery_foundation.sql` with:
  - `user_notification_preferences`
  - `criteria_notification_preferences`
  - `channel_verifications`
  - `alert_delivery`
- Added domain models/enums and repository ports for:
  - channel preferences and fallback strategy
  - channel verification state
  - per-channel alert delivery records
- Added JPA entities/repositories/adapters to persist notification preference, verification, and delivery foundation data.

### 2026-02-26 (Notification Preference Resolution - Chunk 2)

- Added `NotificationPreferenceResolverService` to compute effective delivery channels.
- Resolution precedence:
  - user defaults are baseline
  - criteria overrides are used only when `useUserDefaults=false`
- Default fallback strategy is enforced as `FIRST_SUCCESS`.
- Added validation guardrails for invalid preference configurations:
  - empty enabled-channel overrides
  - preferred channel not present in enabled channels
  - contradictory criteria config (`useUserDefaults=true` with explicit overrides)
- Added unit tests covering core resolution matrix and invalid configurations.

### 2026-02-26 (Notification Verification Flow - Chunk 3)

- Added verification API endpoints:
  - `POST /api/notifications/verifications/start`
  - `POST /api/notifications/verifications/{verificationId}/confirm`
- Added `ManageChannelVerificationUseCase` with:
  - secure token generation
  - SHA-256 token hashing at rest
  - expiry handling (`PENDING_VERIFICATION -> EXPIRED`)
  - confirmation flow (`PENDING_VERIFICATION -> VERIFIED`)
- Added local-dev response support for one-time raw token visibility (`app.notification.verification.expose-raw-token`).
- Added verified-channel enforcement in `NotificationPreferenceResolverService` so unverified channels are removed from effective routing.
- Added unit and integration contract tests for verification and resolver filtering behavior.

### 2026-02-26 (Email Delivery Adapter - Chunk 4)

- Added `EmailSenderPort` with provider adapters:
  - SMTP adapter for local/dev (`app.notification.email.provider=smtp`)
  - AWS SES adapter for production (`app.notification.email.provider=ses`)
- Added provider error mapping to `RETRYABLE` vs `NON_RETRYABLE` via `EmailDeliveryException`.
- Added MailHog service to `docker-compose.yml`:
  - SMTP: `localhost:1025`
  - UI: `http://localhost:8025`
- Updated Kafka alert consumer to send email notifications when a user email is available.
- Added adapter tests for SMTP/SES success + failure classification.

### 2026-02-25 (Automated API Contract Testing)

- Added `ApiIntegrationContractTest` using `@SpringBootTest(webEnvironment = RANDOM_PORT)` + RestAssured.
- Added OpenAPI contract validation filter (`swagger-request-validator-restassured`) on authenticated API happy paths.
- Added test-safe runtime overrides for this suite so it runs without external Kafka/Elasticsearch services.
- Improved OpenAPI metadata:
  - `DELETE /api/criteria/{criteriaId}` now documents `204` and `404` responses.
  - Removed duplicate enum metadata that produced invalid generated request schema for criteria payloads.

### 2026-02-25 (Chunk 7: API Endpoints + UX Consistency)

- Criteria API responses now use a dedicated response model with null fields omitted for cleaner payloads.
- `GET /api/criteria/user/{userId}` now supports optional filters:
  - `temperatureUnit` (`F`/`C`)
  - `monitorCurrent` / `monitorForecast`
  - `enabled`
  - `hasTemperatureRule` / `hasRainRule`
- Create/update criteria validation now returns clearer request-level errors for:
  - threshold pair requirements
  - coordinate requirements for temperature/rain thresholds
  - forecast-window usage when forecast monitoring is disabled
  - probability rain threshold bounds (`<= 100`)
- Swagger/OpenAPI examples were updated to realistic, copy/paste-ready requests with Orlando coordinates.

### 2026-02-25 (Chunk 8: Scheduler + Orchestration)

- Scheduler execution now uses fixed-delay cadence to avoid overlapping runs.
- Criteria processing is now batched and reuses per-run caches for current/forecast calls by coordinate window.
- NOAA client now includes request pacing (`min-request-interval`) and outage guard short-circuiting after repeated failures.
- Criteria evaluation now tracks `MET`, `NOT_MET`, and `UNAVAILABLE` outcomes:
  - `UNAVAILABLE` skips state transitions to avoid clearing/rearming on provider outages.
- Added Micrometer metrics and richer logs around evaluation outcomes, dedupe/suppression, and trigger counts.

## Setup

### 1. Start Local Dependencies with Docker

```bash
# Starts PostgreSQL, Zookeeper, Kafka, Kafka topic bootstrap, and Elasticsearch
docker compose up -d

# Optional: include Kafka UI on http://localhost:8081
docker compose --profile tools up -d

# Optional: include observability stack
# Zipkin:  http://localhost:9411
# Grafana: http://localhost:3000 (admin/admin)
docker compose --profile observability up -d
```

This stack is defined in `docker-compose.yml` and includes:
- PostgreSQL on `localhost:5432` (database: `weather_alerts`)
- Kafka on `localhost:9092` (topic `weather-alerts` created automatically)
- Elasticsearch on `localhost:9200`
- MailHog SMTP on `localhost:1025` with web UI on `http://localhost:8025`
- Optional observability profile:
  - Zipkin (distributed trace UI)
  - Loki + Promtail + Grafana (log aggregation/search)

### 2. Configure Local App Environment

```bash
cp .env.example .env
set -a
source .env
set +a
```

This sets required security credentials and infrastructure endpoints without modifying `src/main/resources/application.yml`.
It also includes `APP_NOAA_MAX_IN_MEMORY_SIZE` to avoid large NOAA payload buffer errors in local dev.
Optional NOAA client tuning values in `.env`:

- `APP_NOAA_REQUEST_TIMEOUT_SECONDS` (default `8`)
- `APP_NOAA_RETRY_MAX_ATTEMPTS` (default `2`)
- `APP_NOAA_RETRY_BACKOFF_MILLIS` (default `250`)
- `APP_NOAA_MIN_REQUEST_INTERVAL_MILLIS` (default `150`)
- `APP_NOAA_OUTAGE_FAILURE_THRESHOLD` (default `4`)
- `APP_NOAA_OUTAGE_OPEN_SECONDS` (default `30`)

Retention tuning values in `.env`:

- `APP_RETENTION_ENABLED` (default `true`)
- `APP_RETENTION_ALERTS_DAYS` (default `2`)
- `APP_RETENTION_WEATHER_DATA_HOURS` (default `72`)
- `APP_RETENTION_CRITERIA_STATE_DAYS` (default `14`)
- `APP_RETENTION_CLEANUP_ORPHAN_CRITERIA_STATE` (default `true`)
- `APP_RETENTION_CLEANUP_FIXED_DELAY_MS` (default `3600000`)
- `APP_RETENTION_CLEANUP_INITIAL_DELAY_MS` (default `120000`)

Notification verification tuning values in `.env`:

- `APP_NOTIFICATION_EMAIL_PROVIDER` (`smtp` for local/MailHog, `ses` for AWS SES)
- `APP_NOTIFICATION_EMAIL_FROM_ADDRESS` (default sender address)
- `APP_NOTIFICATION_EMAIL_SES_REGION` (AWS region for SES, default `us-east-1`)
- `APP_NOTIFICATION_VERIFICATION_TOKEN_TTL_MINUTES` (default `15`)
- `APP_NOTIFICATION_VERIFICATION_EXPOSE_RAW_TOKEN` (default `true` for local/dev)

### 3. Database Migrations (Flyway)

Schema is now migration-driven with Flyway (`src/main/resources/db/migration`).

- On startup, Flyway runs pending migrations before JPA initialization.
- JPA is set to `ddl-auto: validate` (it no longer auto-creates/updates tables).
- Baseline settings are enabled for safer rollout on existing local databases:
  - `baseline-on-migrate: true`
  - `baseline-version: 0`
- Current migrations:
  - `V1__baseline.sql` (existing core tables)
  - `V2__extend_alert_criteria_for_weather_conditions.sql` (new weather-condition criteria fields)
  - `V3__add_criteria_state.sql` (anti-spam criteria edge/notification state)
  - `V4__extend_alerts_for_lifecycle_and_dedupe.sql` (alert event key, lifecycle metadata, dedupe indexes)
  - `V5__add_retention_cleanup_index.sql` (alert retention cleanup index)
  - `V6__add_notification_delivery_foundation.sql` (notification preferences, channel verification, and alert delivery tracking tables)

Common commands:

```bash
# Clean build and run migrations on app startup
mvn clean install
mvn spring-boot:run
```

If you need a clean local database:

```bash
docker compose down -v
docker compose up -d
```

## Running the Application

```bash
# Build the project
mvn clean install

# Run the application against local Docker services
mvn spring-boot:run

# Or run the JAR
java -jar target/weather-alert-backend-0.0.1-SNAPSHOT.jar
```

The application will start on `http://localhost:8080`

### Error Handling + Correlation

The API uses RFC7807 Problem Details for structured errors (e.g., validation and not-found):

```json
{
  "type": "https://weather-alert-backend/errors/criteria_not_found",
  "title": "Not Found",
  "status": 404,
  "detail": "Criteria not found: missing-id",
  "instance": "/api/criteria/missing-id",
  "errorCode": "CRITERIA_NOT_FOUND",
  "timestamp": "2026-02-24T18:30:00Z",
  "path": "/api/criteria/missing-id",
  "traceId": "8f3d8e4f2f6d1a2b",
  "correlationId": "a5a8f85e-9d9b-4308-bfcb-7c6e0f0f4b1d"
}
```

Each response includes `X-Correlation-Id` and logs include `traceId`, `spanId`, and `correlationId` for easier troubleshooting.

### Observability Workflow

1. Start observability services:
```bash
docker compose --profile observability up -d
```
2. Run the app (`mvn spring-boot:run`) with `.env` loaded.
3. Open Zipkin on `http://localhost:9411` to inspect traces.
4. Open Grafana on `http://localhost:3000` (`admin` / `admin`) and query logs from Loki with:
```text
{app="weather-alert-backend"}
```
5. Metrics are available via actuator (for local/manual checks):
```text
GET /actuator/metrics/weather.alert.processing.duration
GET /actuator/metrics/weather.alert.criteria.evaluated
GET /actuator/metrics/weather.alert.criteria.met
GET /actuator/metrics/weather.alert.criteria.not_met
GET /actuator/metrics/weather.alert.criteria.unavailable
GET /actuator/metrics/weather.alert.criteria.suppressed
GET /actuator/metrics/weather.alert.criteria.deduped
GET /actuator/metrics/weather.alert.triggered
GET /actuator/metrics/weather.noaa.requests
GET /actuator/metrics/weather.noaa.request.duration
```

### Scheduler + Orchestration Behavior

- Scheduler runs every 5 minutes with fixed delay and 30s initial delay.
- Each run:
  - fetches active NOAA alerts
  - loads enabled criteria
  - processes criteria in batches of 100
  - reuses per-run caches for current conditions (`lat/lon`) and forecast (`lat/lon/window`)
- Outage guard behavior:
  - repeated NOAA request failures open a short outage window
  - while open, NOAA requests are short-circuited
  - criteria evaluations become `UNAVAILABLE`
  - `UNAVAILABLE` does not mutate `criteria_state`, preventing false transitions/spam on recovery

### Data Retention + Pruning

- A separate scheduled cleanup job runs hourly (after a 2-minute startup delay by default).
- Cleanup scopes:
  - delete old `alerts` rows by `alert_time`
  - delete stale `criteria_state` rows by `updated_at`
  - delete orphaned `criteria_state` rows whose criteria no longer exists
  - delete old Elasticsearch `weather-data` docs by `timestamp`
- Kafka topic `weather-alerts` is configured with 24h retention in docker compose.
- All retention windows are configurable via `APP_RETENTION_*` environment variables.

When you are done:

```bash
docker compose down
# or to also remove data volumes
docker compose down -v
```

## API Endpoints

All `/api/**` endpoints now require JWT Bearer authentication (except token issuance).

- **USER role**: can manage only their own criteria (`POST/PUT/DELETE /api/criteria/**`), read weather/alerts/criteria, and acknowledge alerts
- **ADMIN role**: can manage criteria for any user plus access `/api/alerts/pending` and alert-expire endpoint

Credentials must be configured via environment variables:

- `APP_SECURITY_USER_USERNAME`, `APP_SECURITY_USER_PASSWORD`
- `APP_SECURITY_ADMIN_USERNAME`, `APP_SECURITY_ADMIN_PASSWORD`
- `APP_SECURITY_JWT_SECRET` (minimum 32 characters)

Generate a token:

```bash
POST /api/auth/token
{
  "username": "your-username",
  "password": "your-password"
}
```

WebSocket endpoint for real-time subscriptions:

```text
CONNECT ws://localhost:8080/ws-alerts
SUBSCRIBE /topic/alerts/{userId}
```

Swagger/OpenAPI docs (relative to `http://localhost:8080`):

```bash
GET /swagger-ui/index.html
GET /v3/api-docs
```

### Alert Criteria Management (Commands - CQRS)

```bash
# Create alert criteria
POST /api/criteria
{
  "userId": "dev-admin",
  "location": "Orlando",
  "latitude": 28.5383,
  "longitude": -81.3792,
  "temperatureThreshold": 60,
  "temperatureDirection": "BELOW",
  "temperatureUnit": "F",
  "rainThreshold": 40,
  "rainThresholdType": "PROBABILITY",
  "monitorCurrent": true,
  "monitorForecast": true,
  "forecastWindowHours": 48,
  "oncePerEvent": true,
  "rearmWindowMinutes": 120
}

# Update criteria
PUT /api/criteria/{criteriaId}

# Delete criteria
DELETE /api/criteria/{criteriaId}
```
`userId` is optional for non-admin requests and is inferred from the JWT subject.

### Alert Queries (Queries - CQRS)

```bash
# Get alerts for a user
GET /api/alerts/user/{userId}

# Get specific alert
GET /api/alerts/{alertId}

# Get alert history for a criteria
GET /api/alerts/criteria/{criteriaId}/history

# Get pending alerts
GET /api/alerts/pending

# Acknowledge alert
POST /api/alerts/{alertId}/acknowledge

# Expire alert (admin)
POST /api/alerts/{alertId}/expire

# Get criteria for a user
GET /api/criteria/user/{userId}

# Get criteria for a user with optional filters
GET /api/criteria/user/{userId}?temperatureUnit=F&hasRainRule=true&monitorForecast=true

# Get specific criteria
GET /api/criteria/{criteriaId}
```

### Notification Verification

```bash
# Start email verification (returns verificationToken in local/dev)
POST /api/notifications/verifications/start
{
  "channel": "EMAIL",
  "destination": "dev-admin@example.com"
}

# Confirm token
POST /api/notifications/verifications/{verificationId}/confirm
{
  "token": "paste-token-from-start-response"
}
```

### Weather Data

```bash
# Get active weather alerts (Elasticsearch read model, paginated)
GET /api/weather/active?page=0&size=50

# Get alerts for specific location
GET /api/weather/location?latitude=47.6062&longitude=-122.3321

# Get alerts for state
GET /api/weather/state/WA

# Get current conditions for coordinates (NOAA observations/latest)
GET /api/weather/conditions/current?latitude=28.5383&longitude=-81.3792

# Get hourly forecast conditions for coordinates (default 48h)
GET /api/weather/conditions/forecast?latitude=28.5383&longitude=-81.3792&hours=48

# Search by location (Elasticsearch)
GET /api/weather/search/location/Seattle

# Search by event type
GET /api/weather/search/event/Tornado
```

## Domain Models

### WeatherData
Represents weather information from NOAA including alerts, conditions, and forecasts.

Normalization strategy for NOAA condition fields:
- Temperature is normalized to **Celsius**
- Wind speed is normalized to **km/h**
- Forecast rain probability is stored as `precipitationProbability` (percent)
- Observed rain amount is stored as `precipitationAmount` (mm)

Operational behavior:
- Adapter resolves point metadata (`/points/{lat},{lon}`), then calls:
  - latest observation (`/stations/{stationId}/observations/latest`)
  - hourly forecast (`forecastHourly` URL from point metadata)
- NOAA calls use timeout + retry with safe fallbacks (empty result instead of hard failure)

### AlertCriteria
User-defined criteria for triggering weather alerts. Supports:
- Location-based filtering (city name or coordinates + radius)
- Event type matching (tornado, hurricane, flood, etc.)
- Severity thresholds (minor, moderate, severe, extreme)
- Weather condition thresholds (temperature, wind speed, precipitation)
- Explicit temperature threshold direction (`BELOW`/`ABOVE`) and unit (`F`/`C`)
- Rain threshold modes (`PROBABILITY` or `AMOUNT`)
- Monitoring scope (`monitorCurrent`, `monitorForecast`, `forecastWindowHours`)
- Alert cadence controls (`oncePerEvent`, `rearmWindowMinutes`)

Rule semantics:
- Evaluation is `(all configured filters pass) AND (any configured trigger passes)`.
- Filter rules: location, event type, severity.
- Trigger rules: temperature/rain thresholds and legacy wind/precipitation/temperature ranges.
- For `CURRENT_CONDITIONS` and `FORECAST_CONDITIONS`, `eventType` can match `headline`/`description` text (for user-friendly filters like `"Rain"`).
- Current and forecast evaluations are controlled by `monitorCurrent`, `monitorForecast`, and `forecastWindowHours` (default 48h).
- Forecast processing emits one alert for the first matching forecast period per run.

Anti-spam semantics:
- Criteria state is persisted in `criteria_state` (`lastConditionMet`, `lastEventSignature`, `lastNotifiedAt`).
- Notifications are edge-triggered by default: `not met -> met` emits alert.
- While a condition remains met, duplicate alerts are suppressed.
- When the condition clears and later re-occurs, a new alert is emitted (rearm behavior).
- `rearmWindowMinutes` enforces cooldown between notifications to avoid rapid repeat alerts.
- This enables repeated rain alerts across separate events during the week without scheduler spam.

### Alert
Generated alert matching user criteria with weather data.

Lifecycle + dedupe behavior:
- Alerts are created with `PENDING` status and persisted `eventKey`.
- Kafka consumer marks persisted alerts as `SENT` (`sentAt` set) when processed.
- API supports transitions to `ACKNOWLEDGED` and `EXPIRED`.
- Duplicate inserts for the same `criteriaId + eventKey` are skipped.

### User
User profile with notification preferences.

## CQRS Implementation

The application implements CQRS (Command Query Responsibility Segregation):

- **Commands** (`ManageAlertCriteriaUseCase`): Handle write operations
  - Create/Update/Delete alert criteria
  
- **Queries** (`QueryAlertsUseCase`): Handle read operations
  - Fetch alerts and criteria
  - Optimized for read performance

## Hexagonal Architecture Benefits

1. **Domain Independence**: Core business logic doesn't depend on frameworks
2. **Testability**: Easy to test domain logic in isolation
3. **Flexibility**: Easy to swap implementations (e.g., change database)
4. **Maintainability**: Clear boundaries between layers

## Scheduled Tasks

- Weather data is automatically fetched from NOAA every 5 minutes
- Alerts are processed and matched against user criteria (active alerts + current/forecast conditions for criteria with condition rules)
- Criteria anti-spam state is persisted and evaluated before notifying (`criteria_state`)
- Matched alerts are published to Kafka for async notification processing
- Newly-created criteria are evaluated immediately so already-true conditions can notify right away

## Kafka Topics

- `weather-alerts`: Contains all generated alerts for processing

## Development

### Running Tests

```bash
# Full suite
mvn test

# Contract/integration suite only
mvn -Dtest=ApiIntegrationContractTest test
```

Contract test details:
- Test class: `src/test/java/com/weather/alert/integration/ApiIntegrationContractTest.java`
- Stack: RestAssured + `swagger-request-validator-restassured`
- Validates auth token issuance, criteria CRUD happy path, and weather read endpoints against generated `/v3/api-docs`

## GitHub Pages User Site

A multi-page user-focused documentation site is available in the `docs/` folder:

- `docs/index.html` - product overview and user flow
- `docs/getting-started.html` - first-run setup
- `docs/use-alerts.html` - daily usage patterns
- `docs/api-reference.html` - role-aware endpoint table
- `docs/faq.html` - common troubleshooting

### Publish on GitHub Pages

1. Open your repository on GitHub.
2. Go to **Settings > Pages**.
3. Under **Build and deployment**, choose **Deploy from a branch**.
4. Select your branch (usually `main`) and folder `/docs`.
5. Save and wait for deployment to finish.

### Preview Locally

```bash
cd docs
python3 -m http.server 4000
```

Then open `http://localhost:4000`.

### Code Structure

```
src/main/java/com/weather/alert/
├── domain/
│   ├── model/          # Domain entities
│   ├── port/           # Port interfaces
│   └── service/        # Domain services
├── application/
│   ├── dto/            # Data transfer objects
│   └── usecase/        # Use cases (CQRS)
└── infrastructure/
    ├── adapter/
    │   ├── noaa/       # NOAA API adapter
    │   ├── kafka/      # Kafka adapter
    │   ├── persistence/# Database adapter
    │   └── elasticsearch/ # Search adapter
    ├── config/         # Spring configuration
    └── web/
        └── controller/ # REST controllers
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Future Enhancements

- [x] User authentication and authorization
- [ ] Email/SMS notification integration
- [ ] Mobile push notifications
- [ ] GraphQL API
- [x] WebSocket support for real-time updates
- [ ] Advanced analytics and reporting
- [ ] Machine learning for alert prediction
- [ ] Multi-language support
- [x] Swagger UI/OpenAPI documentation
- [x] Rate limiting and API throttling
- [x] Comprehensive monitoring and observability
