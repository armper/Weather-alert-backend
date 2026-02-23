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

### 1. Database Setup

```bash
# Create PostgreSQL database
createdb weather_alerts

# Or using psql
psql -U postgres
CREATE DATABASE weather_alerts;
```

### 2. Kafka Setup

```bash
# Start Kafka (with Zookeeper)
bin/zookeeper-server-start.sh config/zookeeper.properties
bin/kafka-server-start.sh config/server.properties

# Create topic
bin/kafka-topics.sh --create --topic weather-alerts --bootstrap-server localhost:9092
```

### 3. Elasticsearch Setup

```bash
# Start Elasticsearch
bin/elasticsearch
```

### 4. Application Configuration

Update `src/main/resources/application.yml` with your configuration:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/weather_alerts
    username: your_username
    password: your_password
```

## Running the Application

```bash
# Build the project
mvn clean install

# Run the application
mvn spring-boot:run

# Or run the JAR
java -jar target/weather-alert-backend-0.0.1-SNAPSHOT.jar
```

The application will start on `http://localhost:8080`

## API Endpoints

Interactive API documentation is available at:
- `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

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

- [ ] User authentication and authorization
- [ ] Email/SMS notification integration
- [ ] Mobile push notifications
- [ ] GraphQL API
- [ ] WebSocket support for real-time updates
- [ ] Advanced analytics and reporting
- [ ] Machine learning for alert prediction
- [ ] Multi-language support
- [x] Rate limiting and API throttling
- [x] Comprehensive monitoring and observability
