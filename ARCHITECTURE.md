# Weather Alert Backend - Architecture Overview

## System Architecture

This document provides a detailed overview of the Weather Alert Backend architecture, which implements **Hexagonal Architecture** (Ports and Adapters) with Spring Boot.

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         External Systems                         │
├─────────────┬──────────────┬──────────────┬────────────────────┤
│ NOAA API    │ PostgreSQL   │ Kafka        │ Elasticsearch      │
└──────┬──────┴──────┬───────┴──────┬───────┴──────┬─────────────┘
       │             │              │              │
┌──────▼─────────────▼──────────────▼──────────────▼─────────────┐
│              Infrastructure Layer (Adapters)                     │
│  ┌────────────┐ ┌────────────┐ ┌────────┐ ┌──────────────┐    │
│  │ NOAA       │ │ JPA/       │ │ Kafka  │ │ Elasticsearch│    │
│  │ Adapter    │ │ Repository │ │ Adapter│ │ Adapter      │    │
│  └────────────┘ └────────────┘ └────────┘ └──────────────┘    │
│                                                                  │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │              REST API Controllers                        │  │
│  └─────────────────────────────────────────────────────────┘  │
└──────────────────────────┬───────────────────────────────────┘
                           │
┌──────────────────────────▼────────────────────────────────────┐
│                 Application Layer (Use Cases)                  │
│  ┌─────────────────────┐ ┌─────────────────────────────────┐ │
│  │ Manage Alert        │ │ Query Alerts Use Case           │ │
│  │ Criteria Use Case   │ │ (CQRS Query Side)               │ │
│  │ (CQRS Command Side) │ │                                 │ │
│  └─────────────────────┘ └─────────────────────────────────┘ │
└──────────────────────────┬────────────────────────────────────┘
                           │
┌──────────────────────────▼────────────────────────────────────┐
│                       Domain Layer                              │
│  ┌─────────────────┐ ┌─────────────┐ ┌───────────────────┐   │
│  │ Domain Models   │ │ Domain      │ │ Ports (Interfaces)│   │
│  │ - Alert         │ │ Services    │ │ - Repositories    │   │
│  │ - AlertCriteria │ │             │ │ - External APIs   │   │
│  │ - WeatherData   │ │             │ │ - Messaging       │   │
│  │ - User          │ │             │ │                   │   │
│  └─────────────────┘ └─────────────┘ └───────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

## Layer Details

### 1. Domain Layer (Core Business Logic)

**Purpose**: Contains the core business rules and entities, independent of any framework or technology.

#### Domain Models
- **Alert**: Represents a weather alert sent to a user
- **AlertCriteria**: User-defined criteria for triggering alerts (location/event/severity plus temperature and rain thresholds, unit preference, monitoring scope, and alert cadence policy)
- **WeatherData**: Weather information from NOAA
- **User**: User profile and preferences

#### Domain Services
- **AlertProcessingService**: Orchestrates the alert matching and generation process
- **AlertCriteriaRuleEvaluator**: Explicit filter/trigger rule engine used by `AlertCriteria.matches(...)` and processing flows

#### Ports (Interfaces)
- **AlertRepositoryPort**: Alert persistence operations
- **AlertCriteriaRepositoryPort**: Criteria persistence operations
- **AlertCriteriaStateRepositoryPort**: Criteria anti-spam edge-state persistence
- **UserRepositoryPort**: User persistence operations
- **WeatherDataPort**: Fetch weather data from external API
- **NotificationPort**: Send notifications
- **WeatherDataSearchPort**: Search weather data

### 2. Application Layer (Use Cases)

**Purpose**: Implements application-specific business logic and coordinates domain operations.

#### CQRS Implementation

**Commands (Write Operations)**
- `ManageAlertCriteriaUseCase`
  - Create new alert criteria
  - Update existing criteria
  - Delete criteria

**Queries (Read Operations)**
- `QueryAlertsUseCase`
  - Get alerts by user ID
  - Get alert by ID
  - Get criteria by user ID
  - Get pending alerts

#### DTOs (Data Transfer Objects)
- `CreateAlertCriteriaRequest` (supports temperature/rain threshold pairs, monitoring scope, and cadence controls)
- `AlertResponse`
- `WeatherDataResponse`

### 3. Infrastructure Layer (Technical Details)

**Purpose**: Implements the domain ports using specific technologies and frameworks.

#### Adapters

**NOAA API Adapter**
```
NoaaWeatherAdapter
├── Fetches active alerts from https://api.weather.gov/alerts/active
├── Fetches current conditions via points -> stations -> observations/latest
├── Fetches hourly forecast via points -> forecastHourly (windowed, e.g., 48h)
├── Normalizes units (temperature C, wind km/h, precipitation probability/amount)
└── Applies timeout + retry + empty-result fallback for provider resilience
```

**Persistence Adapters**
```
JPA/PostgreSQL
├── AlertEntity → AlertRepositoryAdapter
├── AlertCriteriaEntity → AlertCriteriaRepositoryAdapter
├── AlertCriteriaStateEntity → AlertCriteriaStateRepositoryAdapter
├── UserEntity → UserRepositoryAdapter
└── Flyway-managed schema migrations + Hibernate validation (including criteria extension in V2 and anti-spam state in V3)
```

**Kafka Adapter**
```
Kafka Messaging
├── KafkaNotificationAdapter (Producer)
│   └── Publishes alerts to "weather-alerts" topic
└── AlertKafkaConsumer (Consumer)
    └── Consumes and processes alerts
```

**Elasticsearch Adapter**
```
Elasticsearch Search
├── WeatherDataDocument (Index mapping)
├── ElasticsearchWeatherRepository
└── ElasticsearchWeatherAdapter
    ├── Index weather data for fast search
    └── Search by location, event type, severity
```

#### REST API Controllers

**Alert Query Controller** (`/api/alerts`)
- GET `/user/{userId}` - Get user's alerts
- GET `/{alertId}` - Get specific alert
- GET `/pending` - Get pending alerts

**Alert Criteria Controller** (`/api/criteria`)
- POST `/` - Create criteria
- PUT `/{criteriaId}` - Update criteria
- DELETE `/{criteriaId}` - Delete criteria
- GET `/user/{userId}` - Get user's criteria
- GET `/{criteriaId}` - Get specific criteria

**Weather Data Controller** (`/api/weather`)
- GET `/active` - Get active NOAA alerts
- GET `/location?lat={lat}&lon={lon}` - Get alerts for location
- GET `/state/{stateCode}` - Get alerts for state
- GET `/conditions/current?latitude={lat}&longitude={lon}` - Get latest current conditions
- GET `/conditions/forecast?latitude={lat}&longitude={lon}&hours={h}` - Get hourly forecast conditions
- GET `/search/location/{location}` - Search by location
- GET `/search/event/{eventType}` - Search by event type

## Data Flow

### 1. Alert Processing Flow (Scheduled)

```
┌──────────────┐
│  Scheduler   │ (Every 5 minutes)
└──────┬───────┘
       │
       ▼
┌──────────────────────┐
│ AlertProcessing      │
│ Service              │
└──────┬───────────────┘
       │
       ├─► Fetch from NOAA API
       │   └─► WeatherDataPort → NoaaWeatherAdapter
       │
       ├─► Index in Elasticsearch
       │   └─► WeatherDataSearchPort → ElasticsearchAdapter
       │
       ├─► Load enabled criteria
       │   └─► AlertCriteriaRepositoryPort
       │
       ├─► Evaluate criteria using explicit rules
       │   ├─► Filter rules: location, event type, severity
       │   └─► Trigger rules: temperature, rain, wind, precipitation
       │
       ├─► For criteria with weather-condition rules + coordinates
       │   ├─► Fetch current conditions (if monitorCurrent=true)
       │   └─► Fetch forecast conditions in forecastWindowHours (if monitorForecast=true)
       │
       ├─► Load + evaluate criteria_state (anti-spam edge state)
       │   ├─► If transition is not met -> met, generate alert
       │   ├─► If still met, suppress duplicates
       │   └─► If cleared then re-occurs, allow rearm notification
       │
       ├─► Persist alert + updated criteria_state
       │   └─► AlertRepositoryPort.save() + AlertCriteriaStateRepositoryPort.save()
       │
       └─► Publish to Kafka
           └─► NotificationPort.publishAlert()
```

### 2. User Creates Alert Criteria Flow

```
┌──────────────┐
│  REST API    │ POST /api/criteria
└──────┬───────┘
       │
       ▼
┌──────────────────────┐
│ AlertCriteria        │
│ Controller           │
└──────┬───────────────┘
       │
       ▼
┌──────────────────────┐
│ ManageAlertCriteria  │
│ UseCase              │
└──────┬───────────────┘
       │
       ├─► Persist criteria
       │
       ├─► Immediate criteria evaluation
       │   └─► AlertProcessingService.processCriteriaImmediately(...)
       │
       ▼
┌──────────────────────┐
│ AlertCriteria        │
│ RepositoryPort       │
└──────┬───────────────┘
       │
       ▼
┌──────────────────────┐
│ AlertCriteria        │
│ RepositoryAdapter    │
└──────┬───────────────┘
       │
       ▼
┌──────────────────────┐
│ JPA Repository       │
│ → PostgreSQL         │
└──────────────────────┘
```

After persistence, `ManageAlertCriteriaUseCase` invokes `AlertProcessingService.processCriteriaImmediately(...)`
so criteria that are already true can produce an immediate alert.

## Key Design Patterns

### 1. Hexagonal Architecture (Ports & Adapters)
- **Domain** is at the center, independent of infrastructure
- **Ports** define contracts (interfaces)
- **Adapters** implement ports for specific technologies
- Easy to swap implementations (e.g., change database)

### 2. CQRS (Command Query Responsibility Segregation)
- **Commands**: `ManageAlertCriteriaUseCase` handles writes
- **Queries**: `QueryAlertsUseCase` handles reads
- Separate models for reading and writing
- Can be optimized independently

### 3. Repository Pattern
- Abstracts data persistence
- Domain works with ports, not concrete implementations
- Easy to mock for testing

### 4. Adapter Pattern
- Each external system has its own adapter
- Adapters translate between domain and external formats
- Isolates external dependencies

### 5. Dependency Inversion Principle
- High-level modules (domain) don't depend on low-level modules (infrastructure)
- Both depend on abstractions (ports)
- Domain defines interfaces that infrastructure implements

## Configuration

### Application Configuration
- **application.yml**: Main configuration
- **application-test.yml**: Test-specific configuration

### Key Configurations
- PostgreSQL database connection
- Kafka broker settings
- Elasticsearch connection
- Scheduled task intervals
- Logging levels

## Testing Strategy

### Unit Tests
- **Domain Tests**: Test business logic in isolation
  - `AlertCriteriaTest`: Tests matching logic
- **Use Case Tests**: Test application logic with mocks
  - `ManageAlertCriteriaUseCaseTest`: Tests CQRS commands

### Test Coverage
- Domain models: 100% coverage
- Use cases: 100% coverage
- Adapters: Can be tested with integration tests

## Benefits of This Architecture

1. **Testability**: Business logic can be tested without infrastructure
2. **Maintainability**: Clear separation of concerns
3. **Flexibility**: Easy to change implementations
4. **Independence**: Domain doesn't depend on frameworks
5. **Scalability**: Each layer can be scaled independently
6. **Evolution**: Easy to add new features without breaking existing code

## Technology Stack Summary

| Layer | Technologies |
|-------|-------------|
| Domain | Pure Java POJOs |
| Application | Java, Lombok |
| Infrastructure | Spring Boot, Spring Data JPA, Spring Kafka, Spring Data Elasticsearch |
| Database | PostgreSQL |
| Messaging | Apache Kafka |
| Search | Elasticsearch |
| External API | NOAA Weather API (WebClient) |
| Build | Maven |
| Testing | JUnit 5, Mockito, H2 (in-memory) |

## Security Considerations

1. **API Authentication**: Add Spring Security for API endpoints
2. **Database Security**: Use encrypted connections
3. **Kafka Security**: Configure SASL/SSL
4. **Input Validation**: Use `@Valid` annotations
5. **Rate Limiting**: Prevent API abuse
6. **HTTPS**: Enforce secure connections

## Future Enhancements

1. **Event Sourcing**: Store all domain events
2. **Distributed Tracing**: Add Sleuth/Zipkin
3. **Circuit Breaker**: Add Resilience4j for fault tolerance
4. **Caching**: Add Redis for frequently accessed data
5. **Multi-tenancy**: Support multiple organizations
6. **GraphQL**: Add GraphQL API alongside REST
7. **Real-time Updates**: Add WebSocket support
8. **Machine Learning**: Predict severe weather patterns
