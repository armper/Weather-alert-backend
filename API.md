# Weather Alert Backend - API Documentation

## Base URL
```
http://localhost:8080
```

## Database Migration Strategy

- Database schema changes are managed with Flyway migrations in `src/main/resources/db/migration`.
- Migrations run automatically at app startup.
- JPA uses schema validation (`ddl-auto: validate`) to catch drift instead of mutating schema.
- Latest notification foundation migration is in `V6__add_notification_delivery_foundation.sql`.

## API Endpoints

### 1. Alert Criteria Management

Create, update, and manage alert criteria for users.

Validation rules for weather-condition fields:
- `temperatureThreshold` and `temperatureDirection` must be provided together.
- `rainThreshold` and `rainThresholdType` must be provided together.
- `temperatureThreshold`/`temperatureDirection` cannot be combined with legacy `minTemperature`/`maxTemperature`.
- `rainThreshold`/`rainThresholdType` cannot be combined with legacy `maxPrecipitation`.
- At least one of `monitorCurrent` or `monitorForecast` must be `true` (or both omitted to use defaults).
- `latitude` and `longitude` must be provided together.
- `radiusKm` requires both `latitude` and `longitude`.
- `temperatureThreshold` or `rainThreshold` requires `latitude` and `longitude`.
- `forecastWindowHours` can only be set when `monitorForecast` is enabled.
- `rainThreshold` must be `<= 100` when `rainThresholdType=PROBABILITY`.

Response payload behavior:
- Criteria responses omit null fields for concise, predictable JSON.

Evaluation semantics:
- Criteria are evaluated as: `(all configured filters pass) AND (any configured trigger passes)`.
- Filter rules: `location`, `eventType`, `minSeverity`.
- For condition payloads (`CURRENT_CONDITIONS` / `FORECAST_CONDITIONS`), `eventType` matching also checks headline/description text (for values like `"Rain"`).
- Trigger rules: `temperatureThreshold`, `min/maxTemperature`, `maxWindSpeed`, `maxPrecipitation`, `rainThreshold`.
- `monitorCurrent=true` evaluates NOAA latest observations; `monitorForecast=true` evaluates NOAA hourly forecast within `forecastWindowHours` (default `48`).
- For forecast evaluation, one alert is generated from the first matching forecast period per processing run.
- New criteria are immediately evaluated at creation time; if already true, an alert is generated right away.
- Anti-spam state is persisted per criteria (`criteria_state`): notifications fire on `not met -> met` transition, are deduped while still met, and can re-fire after condition clears.
- `rearmWindowMinutes` applies cooldown to prevent rapid re-notify loops.
- Alerts are persisted with an `eventKey`; duplicate inserts for the same `criteriaId + eventKey` are skipped.
- Lifecycle transitions:
  - Alert entity: `PENDING -> SENT` (after successful channel delivery), then `SENT -> ACKNOWLEDGED` or `SENT/PENDING -> EXPIRED`.
  - Delivery entity (`alert_delivery`): `PENDING -> IN_PROGRESS -> SENT` or `RETRY_SCHEDULED -> FAILED`.
- Scheduler orchestration uses batched criteria evaluation and per-run coordinate caches for current/forecast NOAA lookups.
- During NOAA outages, criteria can evaluate as `UNAVAILABLE`; in that case no anti-spam state transition is persisted.
- Retention cleanup runs on a separate schedule and prunes old `alerts`, old/orphaned `criteria_state`, and old indexed weather read-model documents.
- Notification delivery foundations now persist user/criteria channel preferences, verification state, and per-channel delivery attempts for future email/SMS flows.
- Notification routing resolution now follows precedence: user defaults, then criteria override only when `useUserDefaults=false`; invalid channel configs are rejected by validation logic.
- Effective routing now excludes unverified channels (EMAIL/SMS require a `VERIFIED` channel verification record for the current user destination).
- Email sender providers:
  - `smtp` for local/dev (MailHog)
  - `ses` for production (AWS SES)
- Delivery provider failures are mapped to `RETRYABLE` or `NON_RETRYABLE` classification for later retry workflow.
- Async delivery workflow:
  - Alert events enqueue delivery tasks on Kafka topic `weather-alert-delivery-tasks`.
  - Retries are scheduled via `nextAttemptAt` with exponential backoff and max-attempt cutoff.
  - Permanent failures are published to `weather-alert-delivery-dlq`.

#### Create Alert Criteria
```http
POST /api/criteria
Content-Type: application/json

{
  "userId": "dev-admin",
  "location": "Orlando",
  "latitude": 28.5383,
  "longitude": -81.3792,
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
`userId` ownership rules:
- `ROLE_USER`: `userId` is optional and always resolved to the authenticated JWT subject.
- `ROLE_ADMIN`: can create for another user by providing `userId`; if omitted, uses JWT subject.

**Response (200 OK)**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "userId": "dev-admin",
  "location": "Orlando",
  "latitude": 28.5383,
  "longitude": -81.3792,
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
  "latitude": 28.5383,
  "longitude": -81.3792,
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
  "latitude": 28.5383,
  "longitude": -81.3792,
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

Optional query parameters:
- `temperatureUnit` (`F` or `C`)
- `monitorCurrent` (`true` or `false`)
- `monitorForecast` (`true` or `false`)
- `enabled` (`true` or `false`)
- `hasTemperatureRule` (`true` or `false`)
- `hasRainRule` (`true` or `false`)

Example with filters:
```http
GET /api/criteria/user/dev-admin?temperatureUnit=F&hasRainRule=true&monitorForecast=true
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

### 1.5 Notification Channel Verification

Start and confirm verification for delivery channels (email in current version).

#### Start Verification
```http
POST /api/notifications/verifications/start
Content-Type: application/json

{
  "channel": "EMAIL",
  "destination": "dev-admin@example.com"
}
```

**Response (200 OK)**
```json
{
  "id": "2b4f4f31-5a4c-45d8-b274-301f8c6fb5f4",
  "channel": "EMAIL",
  "destination": "dev-admin@example.com",
  "status": "PENDING_VERIFICATION",
  "tokenExpiresAt": "2026-02-26T18:42:12Z",
  "verifiedAt": null,
  "verificationToken": "2aQWQCi4k9c43-SprCuhbkJYE1S8rFf5"
}
```

Notes:
- Raw `verificationToken` is intended for local/dev workflow and is controlled by `APP_NOTIFICATION_VERIFICATION_EXPOSE_RAW_TOKEN`.
- Persisted tokens are hashed; raw token is never stored.

#### Confirm Verification
```http
POST /api/notifications/verifications/{verificationId}/confirm
Content-Type: application/json

{
  "token": "2aQWQCi4k9c43-SprCuhbkJYE1S8rFf5"
}
```

**Response (200 OK)**
```json
{
  "id": "2b4f4f31-5a4c-45d8-b274-301f8c6fb5f4",
  "channel": "EMAIL",
  "destination": "dev-admin@example.com",
  "status": "VERIFIED",
  "tokenExpiresAt": null,
  "verifiedAt": "2026-02-26T18:40:00Z",
  "verificationToken": null
}
```

---

### 1.6 Notification Preferences

Manage default user routing and optional criteria-level overrides.

#### Get My Notification Preferences
```http
GET /api/users/me/notification-preferences
Authorization: Bearer <token>
```

**Response (200 OK)**
```json
{
  "userId": "dev-admin",
  "enabledChannels": ["EMAIL", "SMS"],
  "preferredChannel": "EMAIL",
  "fallbackStrategy": "FIRST_SUCCESS",
  "createdAt": "2026-02-26T18:40:00Z",
  "updatedAt": "2026-02-26T18:41:30Z"
}
```

#### Update My Notification Preferences
```http
PUT /api/users/me/notification-preferences
Authorization: Bearer <token>
Content-Type: application/json

{
  "enabledChannels": ["EMAIL", "SMS"],
  "preferredChannel": "EMAIL",
  "fallbackStrategy": "FIRST_SUCCESS"
}
```

Notes:
- If no persisted profile exists for the authenticated user, update returns `400`.
- In local/dev flow, start verification first (`POST /api/notifications/verifications/start`) to create the user profile.

#### Get Criteria Notification Preference Override
```http
GET /api/criteria/{criteriaId}/notification-preferences
Authorization: Bearer <token>
```

**Response (200 OK)**
```json
{
  "criteriaId": "ac8d5d8f-ea03-4df6-bf0a-3f56a41795e6",
  "useUserDefaults": false,
  "enabledChannels": ["EMAIL"],
  "preferredChannel": "EMAIL",
  "fallbackStrategy": "FIRST_SUCCESS",
  "createdAt": "2026-02-26T18:42:00Z",
  "updatedAt": "2026-02-26T18:42:00Z"
}
```

#### Update Criteria Notification Preference Override
```http
PUT /api/criteria/{criteriaId}/notification-preferences
Authorization: Bearer <token>
Content-Type: application/json

{
  "useUserDefaults": false,
  "enabledChannels": ["EMAIL"],
  "preferredChannel": "EMAIL",
  "fallbackStrategy": "FIRST_SUCCESS"
}
```

To revert a criteria to inherited behavior:
```http
PUT /api/criteria/{criteriaId}/notification-preferences
Authorization: Bearer <token>
Content-Type: application/json

{
  "useUserDefaults": true
}
```

Auth rules:
- non-admin users can only access overrides for criteria they own
- admins can access any criteria override

---

### 1.7 Email Delivery Provider Configuration

Environment-driven provider selection:

```bash
APP_NOTIFICATION_EMAIL_PROVIDER=smtp
APP_NOTIFICATION_EMAIL_FROM_ADDRESS=no-reply@weather-alert.local
APP_NOTIFICATION_EMAIL_SES_REGION=us-east-1
SPRING_MAIL_HOST=localhost
SPRING_MAIL_PORT=1025
```

Local developer workflow:
- Use MailHog SMTP (`localhost:1025`) and inspect sent messages in `http://localhost:8025`.

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
    "criteriaId": "criteria-123",
    "eventKey": "forecast|criteria-123|2026-02-23T10:00:00Z",
    "reason": "Matched FORECAST: Rain likely",
    "eventType": "Tornado",
    "severity": "SEVERE",
    "headline": "Tornado Warning",
    "description": "Tornado warning in your area. Take immediate shelter.",
    "location": "Seattle, WA",
    "conditionSource": "FORECAST",
    "conditionOnset": "2026-02-23T10:00:00Z",
    "conditionExpires": "2026-02-23T11:00:00Z",
    "conditionTemperatureC": 12.8,
    "conditionPrecipitationProbability": 70.0,
    "conditionPrecipitationAmount": null,
    "alertTime": "2026-02-23T10:30:00Z",
    "status": "SENT",
    "sentAt": "2026-02-23T10:30:02Z",
    "acknowledgedAt": null,
    "expiredAt": null
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
  "criteriaId": "criteria-123",
  "eventKey": "forecast|criteria-123|2026-02-23T10:00:00Z",
  "reason": "Matched FORECAST: Rain likely",
  "eventType": "Tornado",
  "severity": "SEVERE",
  "headline": "Tornado Warning",
  "description": "Tornado warning in your area. Take immediate shelter.",
  "location": "Seattle, WA",
  "conditionSource": "FORECAST",
  "conditionOnset": "2026-02-23T10:00:00Z",
  "conditionExpires": "2026-02-23T11:00:00Z",
  "conditionTemperatureC": 12.8,
  "conditionPrecipitationProbability": 70.0,
  "conditionPrecipitationAmount": null,
  "alertTime": "2026-02-23T10:30:00Z",
  "status": "SENT",
  "sentAt": "2026-02-23T10:30:02Z",
  "acknowledgedAt": null,
  "expiredAt": null
}
```

#### Get Alert History by Criteria
```http
GET /api/alerts/criteria/{criteriaId}/history
```

#### Acknowledge Alert
```http
POST /api/alerts/{alertId}/acknowledge
```

**Response (200 OK)** status changes to `ACKNOWLEDGED` and `acknowledgedAt` is set.

#### Expire Alert (admin)
```http
POST /api/alerts/{alertId}/expire
```

**Response (200 OK)** status changes to `EXPIRED` and `expiredAt` is set.

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
  "criteriaId": "string",
  "eventKey": "string (dedupe key per criteria/event window)",
  "reason": "string (human readable trigger reason)",
  "eventType": "string",
  "severity": "string",
  "headline": "string",
  "description": "string",
  "location": "string",
  "conditionSource": "string (CURRENT|FORECAST|ALERT)",
  "conditionOnset": "string (ISO 8601)",
  "conditionExpires": "string (ISO 8601)",
  "conditionTemperatureC": "number (optional)",
  "conditionPrecipitationProbability": "number (optional)",
  "conditionPrecipitationAmount": "number (optional)",
  "alertTime": "string (ISO 8601)",
  "status": "string - PENDING|SENT|ACKNOWLEDGED|EXPIRED",
  "sentAt": "string (ISO 8601, optional)",
  "acknowledgedAt": "string (ISO 8601, optional)",
  "expiredAt": "string (ISO 8601, optional)"
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
  "type": "https://weather-alert-backend/errors/validation_error",
  "title": "Bad Request",
  "status": 400,
  "detail": "Request validation failed",
  "instance": "/api/criteria",
  "timestamp": "2026-02-23T10:30:00Z",
  "errorCode": "VALIDATION_ERROR",
  "path": "/api/criteria",
  "traceId": "n/a",
  "correlationId": "0fce4f73-cb93-4a4f-a5f6-0346765ccaf0",
  "errors": [
    {
      "field": "request",
      "message": "rainThreshold and rainThresholdType must be provided together"
    }
  ]
}
```

### 404 Not Found
```json
{
  "type": "https://weather-alert-backend/errors/criteria_not_found",
  "title": "Not Found",
  "status": 404,
  "detail": "Criteria not found: 550e8400-e29b-41d4-a716-446655440000",
  "instance": "/api/criteria/550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2026-02-23T10:30:00Z",
  "errorCode": "CRITERIA_NOT_FOUND",
  "path": "/api/criteria/550e8400-e29b-41d4-a716-446655440000",
  "traceId": "n/a",
  "correlationId": "9d53c62a-89c8-4b31-87b5-cf8db29f2e38"
}
```

### 500 Internal Server Error
```json
{
  "type": "https://weather-alert-backend/errors/internal_server_error",
  "title": "Internal Server Error",
  "status": 500,
  "detail": "An unexpected error occurred",
  "instance": "/api/weather/active",
  "timestamp": "2026-02-23T10:30:00Z",
  "errorCode": "INTERNAL_SERVER_ERROR",
  "path": "/api/weather/active",
  "traceId": "n/a",
  "correlationId": "7639d4f8-c7c4-49e5-a7f3-2bf7fd36a47f"
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

Current version uses JWT bearer authentication for all `/api/**` endpoints except token issuance.

- `POST /api/auth/token` issues JWTs.
- `ROLE_USER` can create/update/delete only their own criteria and access read endpoints.
- `ROLE_ADMIN` can manage criteria for any user and access alert-expire operations.

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
