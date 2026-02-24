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

## Setup

### 1. Start Local Dependencies with Docker

```bash
# Starts PostgreSQL, Zookeeper, Kafka, Kafka topic bootstrap, and Elasticsearch
docker compose up -d

# Optional: include Kafka UI on http://localhost:8081
docker compose --profile tools up -d
```

This stack is defined in `docker-compose.yml` and includes:
- PostgreSQL on `localhost:5432` (database: `weather_alerts`)
- Kafka on `localhost:9092` (topic `weather-alerts` created automatically)
- Elasticsearch on `localhost:9200`

### 2. Configure Local App Environment

```bash
cp .env.example .env
set -a
source .env
set +a
```

This sets required security credentials and infrastructure endpoints without modifying `src/main/resources/application.yml`.

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

When you are done:

```bash
docker compose down
# or to also remove data volumes
docker compose down -v
```

## API Endpoints

All `/api/**` endpoints now require JWT Bearer authentication (except token issuance).

- **USER role**: read-only API access
- **ADMIN role**: includes write access for criteria management and pending alerts endpoint

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
  "userId": "user123",
  "location": "Seattle",
  "latitude": 47.6062,
  "longitude": -122.3321,
  "radiusKm": 50,
  "eventType": "Tornado",
  "minSeverity": "SEVERE",
  "maxTemperature": 35,
  "minTemperature": -10,
  "maxWindSpeed": 100
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

# Get pending alerts
GET /api/alerts/pending

# Get criteria for a user
GET /api/criteria/user/{userId}

# Get specific criteria
GET /api/criteria/{criteriaId}
```

### Weather Data

```bash
# Get active weather alerts from NOAA
GET /api/weather/active

# Get alerts for specific location
GET /api/weather/location?latitude=47.6062&longitude=-122.3321

# Get alerts for state
GET /api/weather/state/WA

# Search by location (Elasticsearch)
GET /api/weather/search/location/Seattle

# Search by event type
GET /api/weather/search/event/Tornado
```

## Domain Models

### WeatherData
Represents weather information from NOAA including alerts, conditions, and forecasts.

### AlertCriteria
User-defined criteria for triggering weather alerts. Supports:
- Location-based filtering (city name or coordinates + radius)
- Event type matching (tornado, hurricane, flood, etc.)
- Severity thresholds (minor, moderate, severe, extreme)
- Weather condition thresholds (temperature, wind speed, precipitation)

### Alert
Generated alert matching user criteria with weather data.

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
- Alerts are processed and matched against user criteria
- Matched alerts are published to Kafka for async notification processing

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
