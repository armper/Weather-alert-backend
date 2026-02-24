# Weather Alert Backend - API Documentation

## Base URL
```
http://localhost:8080
```

## Database Migration Strategy

- Database schema changes are managed with Flyway migrations in `src/main/resources/db/migration`.
- Migrations run automatically at app startup.
- JPA uses schema validation (`ddl-auto: validate`) to catch drift instead of mutating schema.
- Latest criteria schema extension is in `V2__extend_alert_criteria_for_weather_conditions.sql`.

## API Endpoints

### 1. Alert Criteria Management

Create, update, and manage alert criteria for users.

Validation rules for weather-condition fields:
- `temperatureThreshold` and `temperatureDirection` must be provided together.
- `rainThreshold` and `rainThresholdType` must be provided together.
- At least one of `monitorCurrent` or `monitorForecast` must be `true` (or both omitted to use defaults).

Evaluation semantics:
- Criteria are evaluated as: `(all configured filters pass) AND (any configured trigger passes)`.
- Filter rules: `location`, `eventType`, `minSeverity`.
- For condition payloads (`CURRENT_CONDITIONS` / `FORECAST_CONDITIONS`), `eventType` matching also checks headline/description text (for values like `"Rain"`).
- Trigger rules: `temperatureThreshold`, `min/maxTemperature`, `maxWindSpeed`, `maxPrecipitation`, `rainThreshold`.
- `monitorCurrent=true` evaluates NOAA latest observations; `monitorForecast=true` evaluates NOAA hourly forecast within `forecastWindowHours` (default `48`).
- For forecast evaluation, one alert is generated from the first matching forecast period per processing run.
- New criteria are immediately evaluated at creation time; if already true, an alert is generated right away.

#### Create Alert Criteria
```http
POST /api/criteria
Content-Type: application/json

{
  "userId": "dev-admin",
  "location": "Orlando",
  "eventType": "Rain",
  "minSeverity": "MODERATE",
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
```

**Response (200 OK)**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "userId": "dev-admin",
  "location": "Orlando",
  "eventType": "Rain",
  "minSeverity": "MODERATE",
  "temperatureThreshold": 60,
  "temperatureDirection": "BELOW",
  "temperatureUnit": "F",
  "rainThreshold": 40,
  "rainThresholdType": "PROBABILITY",
  "monitorCurrent": true,
  "monitorForecast": true,
  "forecastWindowHours": 48,
  "oncePerEvent": true,
  "rearmWindowMinutes": 120,
  "enabled": true
}
```

#### Update Alert Criteria
```http
PUT /api/criteria/{criteriaId}
Content-Type: application/json

{
  "userId": "dev-admin",
  "location": "Orlando",
  "temperatureThreshold": 57,
  "temperatureDirection": "BELOW",
  "temperatureUnit": "F",
  "rainThreshold": 50,
  "rainThresholdType": "PROBABILITY",
  "monitorCurrent": true,
  "monitorForecast": true,
  "forecastWindowHours": 48,
  "oncePerEvent": true,
  "rearmWindowMinutes": 240
}
```

**Response (200 OK)**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "userId": "dev-admin",
  "location": "Orlando",
  "temperatureThreshold": 57,
  "temperatureDirection": "BELOW",
  "temperatureUnit": "F",
  "rainThreshold": 50,
  "rainThresholdType": "PROBABILITY",
  "monitorCurrent": true,
  "monitorForecast": true,
  "forecastWindowHours": 48,
  "oncePerEvent": true,
  "rearmWindowMinutes": 240,
  "enabled": true
}
```

#### Delete Alert Criteria
```http
DELETE /api/criteria/{criteriaId}
```

**Response (204 No Content)**

#### Get User's Alert Criteria
```http
GET /api/criteria/user/{userId}
```

**Response (200 OK)**
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "userId": "user123",
    "location": "Seattle",
    "eventType": "Tornado",
    "minSeverity": "SEVERE",
    "enabled": true
  }
]
```

#### Get Specific Criteria
```http
GET /api/criteria/{criteriaId}
```

**Response (200 OK)**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "userId": "user123",
  "location": "Seattle",
  "eventType": "Tornado",
  "minSeverity": "SEVERE",
  "enabled": true
}
```

---

### 2. Alert Queries

Query and retrieve alerts.

#### Get User's Alerts
```http
GET /api/alerts/user/{userId}
```

**Response (200 OK)**
```json
[
  {
    "id": "alert-001",
    "userId": "user123",
    "eventType": "Tornado",
    "severity": "SEVERE",
    "headline": "Tornado Warning",
    "description": "Tornado warning in your area. Take immediate shelter.",
    "location": "Seattle, WA",
    "alertTime": "2026-02-23T10:30:00Z",
    "status": "PENDING"
  }
]
```

#### Get Specific Alert
```http
GET /api/alerts/{alertId}
```

**Response (200 OK)**
```json
{
  "id": "alert-001",
  "userId": "user123",
  "eventType": "Tornado",
  "severity": "SEVERE",
  "headline": "Tornado Warning",
  "description": "Tornado warning in your area. Take immediate shelter.",
  "location": "Seattle, WA",
  "alertTime": "2026-02-23T10:30:00Z",
  "status": "PENDING"
}
```

#### Get Pending Alerts
```http
GET /api/alerts/pending
```

**Response (200 OK)**
```json
[
  {
    "id": "alert-001",
    "userId": "user123",
    "eventType": "Tornado",
    "severity": "SEVERE",
    "headline": "Tornado Warning",
    "description": "Tornado warning in your area.",
    "location": "Seattle, WA",
    "alertTime": "2026-02-23T10:30:00Z",
    "status": "PENDING"
  }
]
```

---

### 3. Weather Data

Retrieve weather data from NOAA API and search indexed data.

NOAA condition normalization:
- Temperature is normalized to Celsius.
- Wind speed is normalized to km/h.
- Forecast rain probability is returned as `precipitationProbability` (percent).
- Current observed rain amount is returned as `precipitationAmount` (mm).

#### Get Active Weather Alerts
```http
GET /api/weather/active
```

**Response (200 OK)**
```json
[
  {
    "id": "noaa-alert-12345",
    "location": "King County, WA",
    "eventType": "Severe Thunderstorm Warning",
    "severity": "SEVERE",
    "headline": "Severe Thunderstorm Warning issued",
    "description": "Severe thunderstorms with damaging winds expected.",
    "onset": "2026-02-23T10:00:00Z",
    "expires": "2026-02-23T14:00:00Z"
  }
]
```

#### Get Alerts for Location
```http
GET /api/weather/location?latitude=47.6062&longitude=-122.3321
```

**Query Parameters:**
- `latitude` (required): Latitude coordinate
- `longitude` (required): Longitude coordinate

**Response (200 OK)**
```json
[
  {
    "id": "noaa-alert-12345",
    "location": "Seattle, WA",
    "eventType": "Winter Storm Warning",
    "severity": "SEVERE",
    "headline": "Heavy snow expected",
    "description": "6-12 inches of snow expected over the next 24 hours.",
    "onset": "2026-02-23T18:00:00Z",
    "expires": "2026-02-24T18:00:00Z"
  }
]
```

#### Get Alerts for State
```http
GET /api/weather/state/{stateCode}
```

**Path Parameters:**
- `stateCode`: Two-letter state code (e.g., "WA", "CA", "NY")

**Response (200 OK)**
```json
[
  {
    "id": "noaa-alert-67890",
    "location": "Entire State of Washington",
    "eventType": "Winter Weather Advisory",
    "severity": "MODERATE",
    "headline": "Winter weather advisory in effect",
    "description": "Light snow and freezing rain expected.",
    "onset": "2026-02-23T12:00:00Z",
    "expires": "2026-02-24T06:00:00Z"
  }
]
```

#### Get Current Conditions for Coordinates
```http
GET /api/weather/conditions/current?latitude=28.5383&longitude=-81.3792
```

**Response (200 OK)**
```json
{
  "id": "current-KORL-1771960200000",
  "location": "Orlando Executive Airport",
  "eventType": "CURRENT_CONDITIONS",
  "headline": "Clear",
  "description": "Latest NOAA observation from station KORL",
  "temperature": 14.0,
  "windSpeed": 8.0,
  "humidity": 21.0,
  "precipitationAmount": 0.0,
  "timestamp": "2026-02-24T19:10:00Z"
}
```

#### Get Forecast Conditions for Coordinates
```http
GET /api/weather/conditions/forecast?latitude=28.5383&longitude=-81.3792&hours=48
```

**Query Parameters:**
- `latitude` (required): Latitude coordinate
- `longitude` (required): Longitude coordinate
- `hours` (optional): Forecast window in hours (1..168, default 48)

**Response (200 OK)**
```json
[
  {
    "id": "forecast-28.5383--81.3792-1771960800000",
    "location": "lat=28.5383,lon=-81.3792",
    "eventType": "FORECAST_CONDITIONS",
    "headline": "Sunny",
    "description": "Sunny",
    "onset": "2026-02-24T20:00:00Z",
    "expires": "2026-02-24T21:00:00Z",
    "temperature": 14.4,
    "windSpeed": 8.0,
    "humidity": 24.0,
    "precipitationProbability": 0.0,
    "timestamp": "2026-02-24T19:30:00Z"
  }
]
```

#### Search Weather Data by Location
```http
GET /api/weather/search/location/{location}
```

**Path Parameters:**
- `location`: Location name (e.g., "Seattle", "San Francisco")

**Response (200 OK)**
```json
[
  {
    "id": "weather-001",
    "location": "Seattle, WA",
    "eventType": "Flood Watch",
    "severity": "MODERATE",
    "headline": "Flood watch in effect",
    "description": "Heavy rainfall may cause flooding.",
    "onset": "2026-02-23T08:00:00Z",
    "expires": "2026-02-24T20:00:00Z"
  }
]
```

#### Search Weather Data by Event Type
```http
GET /api/weather/search/event/{eventType}
```

**Path Parameters:**
- `eventType`: Type of weather event (e.g., "Tornado", "Hurricane", "Flood")

**Response (200 OK)**
```json
[
  {
    "id": "weather-002",
    "location": "Oklahoma City, OK",
    "eventType": "Tornado",
    "severity": "EXTREME",
    "headline": "Tornado emergency",
    "description": "Large and extremely dangerous tornado on the ground.",
    "onset": "2026-02-23T15:30:00Z",
    "expires": "2026-02-23T17:00:00Z"
  }
]
```

---

## Data Models

### AlertCriteria

```json
{
  "id": "string (UUID)",
  "userId": "string",
  "location": "string (optional)",
  "latitude": "number (optional)",
  "longitude": "number (optional)",
  "radiusKm": "number (optional)",
  "eventType": "string (optional)",
  "minSeverity": "string (optional) - MINOR|MODERATE|SEVERE|EXTREME",
  "maxTemperature": "number (optional) - Celsius",
  "minTemperature": "number (optional) - Celsius",
  "maxWindSpeed": "number (optional) - km/h",
  "maxPrecipitation": "number (optional) - mm",
  "temperatureThreshold": "number (optional)",
  "temperatureDirection": "string (optional) - BELOW|ABOVE",
  "temperatureUnit": "string (optional, default F) - F|C",
  "rainThreshold": "number (optional)",
  "rainThresholdType": "string (optional) - PROBABILITY|AMOUNT",
  "monitorCurrent": "boolean (optional, default true)",
  "monitorForecast": "boolean (optional, default true)",
  "forecastWindowHours": "integer (optional, default 48, range 1..168)",
  "oncePerEvent": "boolean (optional, default true)",
  "rearmWindowMinutes": "integer (optional, default 0, range 0..10080)",
  "enabled": "boolean"
}
```

### Alert

```json
{
  "id": "string (UUID)",
  "userId": "string",
  "eventType": "string",
  "severity": "string",
  "headline": "string",
  "description": "string",
  "location": "string",
  "alertTime": "string (ISO 8601)",
  "status": "string - PENDING|SENT|ACKNOWLEDGED|EXPIRED"
}
```

### WeatherData

```json
{
  "id": "string",
  "location": "string",
  "eventType": "string",
  "severity": "string",
  "headline": "string",
  "description": "string",
  "onset": "string (ISO 8601)",
  "expires": "string (ISO 8601)",
  "temperature": "number (Celsius)",
  "windSpeed": "number (km/h)",
  "precipitationProbability": "number (percent, forecast)",
  "precipitationAmount": "number (mm, current observation)",
  "humidity": "number (percent)",
  "timestamp": "string (ISO 8601)"
}
```

---

## Error Responses

### 400 Bad Request
```json
{
  "timestamp": "2026-02-23T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid request parameters",
  "path": "/api/criteria"
}
```

### 404 Not Found
```json
{
  "timestamp": "2026-02-23T10:30:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Criteria not found",
  "path": "/api/criteria/550e8400-e29b-41d4-a716-446655440000"
}
```

### 500 Internal Server Error
```json
{
  "timestamp": "2026-02-23T10:30:00Z",
  "status": 500,
  "error": "Internal Server Error",
  "message": "An unexpected error occurred",
  "path": "/api/weather/active"
}
```

---

## NOAA API Integration

The backend integrates with the NOAA Weather API:
- **Base URL**: https://api.weather.gov
- **Endpoints Used**:
  - `/alerts/active` - Get all active alerts
  - `/alerts/active?point={lat},{lon}` - Get alerts for coordinates
  - `/alerts/active?area={state}` - Get alerts for state

### NOAA API Requirements
- User-Agent header required: "Weather-Alert-Backend/1.0"
- Rate limiting: Respect NOAA's rate limits
- Data format: JSON with GeoJSON features

---

## Kafka Topics

### weather-alerts
Messages published when new alerts are generated.

**Message Format**:
```json
{
  "id": "alert-001",
  "userId": "user123",
  "eventType": "Tornado",
  "severity": "SEVERE",
  "headline": "Tornado Warning",
  "description": "Tornado warning in your area.",
  "location": "Seattle, WA",
  "alertTime": "2026-02-23T10:30:00Z",
  "status": "PENDING"
}
```

---

## WebSocket Real-Time Updates

The backend provides a STOMP WebSocket endpoint for real-time alert updates.

- **Handshake endpoint**: `/ws-alerts`
- **Subscription topic**: `/topic/alerts/{userId}`
- **Payload**: `Alert` JSON message
- **Allowed origins**: configured via `app.websocket.allowed-origins` (default: `http://localhost:3000`)

When a new alert is consumed from Kafka (`weather-alerts` topic), it is broadcast to the user-specific destination `/topic/alerts/{userId}`.

---

## Usage Examples

### Example 1: Create Tornado Alert for Seattle
```bash
curl -X POST http://localhost:8080/api/criteria \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "location": "Seattle",
    "latitude": 47.6062,
    "longitude": -122.3321,
    "radiusKm": 50,
    "eventType": "Tornado",
    "minSeverity": "SEVERE"
  }'
```

### Example 2: Get All Alerts for a User
```bash
curl http://localhost:8080/api/alerts/user/user123
```

### Example 3: Get Active Weather Alerts from NOAA
```bash
curl http://localhost:8080/api/weather/active
```

### Example 4: Search for Hurricane Alerts
```bash
curl http://localhost:8080/api/weather/search/event/Hurricane
```

---

## Best Practices

1. **Location Filtering**: Use either location name OR coordinates+radius, not both
2. **Severity Levels**: Use standard values: MINOR, MODERATE, SEVERE, EXTREME
3. **Event Types**: Match NOAA event types for best results
4. **Pagination**: For large result sets, consider adding pagination parameters
5. **Caching**: Cache NOAA API responses to reduce external API calls
6. **Error Handling**: Always check response status codes
7. **Rate Limiting**: Implement rate limiting on your client to avoid overwhelming the server

---

## Authentication & Authorization

> **Note**: Current version does not implement authentication.
> Future versions will include:
> - JWT token-based authentication
> - Role-based access control (RBAC)
> - API key management
> - OAuth2 integration

---

## Monitoring & Metrics

Consider implementing:
- Request/response times
- Error rates
- NOAA API call frequency
- Alert generation rates
- User activity metrics

---

## Support

For issues or questions, please contact:
- GitHub: https://github.com/armper/Weather-alert-backend
- Email: support@weatheralert.example.com
