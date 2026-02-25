# Weather Alert Backend

A sophisticated weather alert backend system built with Java 21, Spring Boot, and hexagonal clean architecture. This system integrates with NOAA's weather API to fetch real-time weather alerts and sends custom notifications to users based on their personalized criteria and thresholds.

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
- ✅ **Geographic Filtering**: Radius-based location matching using Haversine formula
- ✅ **Async Processing**: Kafka-based message queue for scalable alert processing
- ✅ **Search Capabilities**: Elasticsearch integration for fast weather data queries
- ✅ **CQRS Pattern**: Separate command and query operations for optimal performance
- ✅ **Scheduled Updates**: Automatic weather data fetching every 5 minutes
- ✅ **RESTful API**: Comprehensive REST endpoints for all operations
- ✅ **JWT Authentication**: Secure API access with bearer tokens and role-based authorization
- ✅ **WebSocket Updates**: STOMP endpoint for real-time weather alert streams
- ✅ **Swagger UI**: Interactive API documentation for exploring and testing endpoints

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

## Changelog

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

When you are done:

```bash
docker compose down
# or to also remove data volumes
docker compose down -v
```

## API Endpoints

All `/api/**` endpoints now require JWT Bearer authentication (except token issuance).

- **USER role**: read-only API access
- **ADMIN role**: includes write access for criteria management, pending alerts endpoint, and alert-expire endpoint

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
mvn test
```

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
