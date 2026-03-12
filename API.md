# Weather Alert Backend - API Documentation

## Base URLs

- Local Spring Boot: `http://localhost:8080`
- Local Docker Compose: `http://localhost:8088`
- Swagger UI: `/swagger-ui/index.html`
- OpenAPI JSON: `/v3/api-docs`

## Authentication

All `/api/**` routes require JWT bearer authentication except these public endpoints:

- `POST /api/auth/token`
- `POST /api/auth/magic-link/request`
- `POST /api/auth/magic-link/confirm`
- `POST /api/auth/register`
- `POST /api/auth/register/verify-email`
- `POST /api/auth/register/resend-verification`
- `POST /api/auth/recovery/username/request`
- `POST /api/auth/recovery/username/confirm`
- `POST /api/auth/recovery/password/request`
- `POST /api/auth/recovery/password/confirm`
- `POST /api/stripe/webhook`

Role summary:

- `ROLE_USER`: own account/profile, own criteria, own notification preferences, weather reads, alert reads, billing endpoints, alert acknowledgements
- `ROLE_ADMIN`: everything above plus `/api/admin/users/**`, `/api/admin/jobs/**`, `GET /api/alerts/pending`, and `POST /api/alerts/{alertId}/expire`

Machine-triggered admin jobs can also authenticate with `X-Admin-Job-Token` when `APP_ADMIN_JOBS_TOKEN` is configured.

Example token response:

```json
{
  "accessToken": "<jwt>",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

## Endpoint Groups

### Auth and Recovery

| Method | Path | Notes |
| --- | --- | --- |
| `POST` | `/api/auth/token` | Username/password login |
| `POST` | `/api/auth/magic-link/request` | Request one-time sign-in link or code by `usernameOrEmail` |
| `POST` | `/api/auth/magic-link/confirm` | Exchange `recoveryId` + `code` for JWT |
| `POST` | `/api/auth/register` | Register account and start email verification |
| `POST` | `/api/auth/register/verify-email` | Confirm registration email token |
| `POST` | `/api/auth/register/resend-verification` | Resend registration verification |
| `POST` | `/api/auth/recovery/username/request` | Request username reminder |
| `POST` | `/api/auth/recovery/username/confirm` | Reveal username with `recoveryId` + `code` |
| `POST` | `/api/auth/recovery/password/request` | Request password reset code |
| `POST` | `/api/auth/recovery/password/confirm` | Reset password with `recoveryId` + `code` + `newPassword` |

Representative request payloads:

```http
POST /api/auth/token
Content-Type: application/json

{
  "username": "alice",
  "password": "StrongPass123!"
}
```

```http
POST /api/auth/magic-link/request
Content-Type: application/json

{
  "usernameOrEmail": "alice@example.com"
}
```

```http
POST /api/auth/recovery/password/confirm
Content-Type: application/json

{
  "recoveryId": "4f5f913d-baa8-4d20-8f72-e894712b8b23",
  "code": "A2B3C4D5",
  "newPassword": "StrongPass123!"
}
```

Notes:

- Registration returns `account` plus `emailVerification`.
- Recovery and magic-link request endpoints can return `recoveryId`, `codeExpiresAt`, optional dev-only `recoveryCode`, and `retryAfterSeconds`.
- Registered users must verify email before login succeeds.

### My Account

| Method | Path | Notes |
| --- | --- | --- |
| `GET` | `/api/users/me` | Current user profile |
| `PUT` | `/api/users/me` | Update `name` and `phoneNumber` |
| `POST` | `/api/users/me/change-password` | Change password using current credentials |
| `DELETE` | `/api/users/me` | Permanently delete current account |

Representative response:

```json
{
  "id": "alice",
  "email": "alice@example.com",
  "phoneNumber": "+14075551234",
  "name": "Alice",
  "role": "ROLE_USER",
  "approvalStatus": "ACTIVE",
  "emailVerified": true,
  "passwordResetRequired": false,
  "approvedAt": "2026-02-26T18:45:00Z",
  "createdAt": "2026-02-26T18:40:00Z",
  "updatedAt": "2026-02-26T18:41:30Z"
}
```

### Alert Criteria

| Method | Path | Notes |
| --- | --- | --- |
| `POST` | `/api/criteria` | Create criteria |
| `PUT` | `/api/criteria/{criteriaId}` | Update criteria |
| `DELETE` | `/api/criteria/{criteriaId}` | Delete criteria |
| `GET` | `/api/criteria/user/{userId}` | List criteria for a user |
| `GET` | `/api/criteria/{criteriaId}` | Get one criteria |

Important validation rules:

- `temperatureThreshold` and `temperatureDirection` must be provided together.
- `rainThreshold` and `rainThresholdType` must be provided together.
- `temperatureThreshold`/`temperatureDirection` cannot be combined with legacy `minTemperature`/`maxTemperature`.
- `rainThreshold`/`rainThresholdType` cannot be combined with legacy `maxPrecipitation`.
- At least one of `monitorCurrent` or `monitorForecast` must be `true`.
- `latitude` and `longitude` must be provided together.
- `radiusKm` requires coordinates.
- `temperatureThreshold` or `rainThreshold` requires coordinates.
- `forecastWindowHours` requires `monitorForecast=true`.
- `rainThresholdType=PROBABILITY` requires `rainThreshold <= 100`.

Ownership rules:

- `ROLE_USER`: `userId` is optional and resolved from the JWT subject.
- `ROLE_ADMIN`: may create or update criteria for another user by supplying `userId`.

Supported list filters on `GET /api/criteria/user/{userId}`:

- `temperatureUnit`
- `monitorCurrent`
- `monitorForecast`
- `enabled`
- `hasTemperatureRule`
- `hasRainRule`

Representative criteria payload:

```json
{
  "name": "Annoying Winds",
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

Evaluation semantics:

- Criteria are evaluated as `(all configured filters pass) AND (any configured trigger passes)`.
- New criteria are evaluated immediately after creation.
- Alert dedupe uses `criteriaId + eventKey`.
- Anti-spam state persists `MET`, `NOT_MET`, and `UNAVAILABLE` style outcomes so NOAA outages do not trigger false clears or re-arms.

### Alerts

| Method | Path | Notes |
| --- | --- | --- |
| `GET` | `/api/alerts/user/{userId}` | List alerts for a user |
| `GET` | `/api/alerts/{alertId}` | Get one alert |
| `GET` | `/api/alerts/criteria/{criteriaId}/history` | Alert history for a criteria |
| `GET` | `/api/alerts/pending` | Admin-only pending alerts |
| `POST` | `/api/alerts/{alertId}/acknowledge` | Mark alert acknowledged |
| `POST` | `/api/alerts/{alertId}/expire` | Admin-only manual expire |

Alert lifecycle:

- `PENDING -> SENT`
- `SENT -> ACKNOWLEDGED`
- `PENDING` or `SENT -> EXPIRED`

Representative alert payload:

```json
{
  "id": "alert-001",
  "userId": "alice",
  "criteriaId": "criteria-123",
  "eventKey": "forecast|criteria-123|2026-02-23T10:00:00Z",
  "reason": "Matched FORECAST: Rain likely",
  "eventType": "FORECAST_CONDITIONS",
  "severity": "MODERATE",
  "headline": "Chance Showers",
  "description": "Rain likely during the next forecast period.",
  "location": "Orlando, FL",
  "conditionSource": "FORECAST",
  "conditionOnset": "2026-02-23T10:00:00Z",
  "conditionExpires": "2026-02-23T11:00:00Z",
  "conditionTemperatureC": 12.8,
  "conditionPrecipitationProbability": 70.0,
  "alertTime": "2026-02-23T10:30:00Z",
  "status": "SENT",
  "sentAt": "2026-02-23T10:30:02Z"
}
```

### Notification Verification and Preferences

| Method | Path | Notes |
| --- | --- | --- |
| `POST` | `/api/notifications/verifications/start` | Start EMAIL verification |
| `POST` | `/api/notifications/verifications/{verificationId}/confirm` | Confirm verification token |
| `GET` | `/api/users/me/notification-preferences` | User-level routing preferences |
| `PUT` | `/api/users/me/notification-preferences` | Update user-level routing |
| `GET` | `/api/criteria/{criteriaId}/notification-preferences` | Criteria override |
| `PUT` | `/api/criteria/{criteriaId}/notification-preferences` | Update criteria override |

Notes:

- Unverified EMAIL/SMS channels are excluded from effective routing.
- User-level preferences require `enabledChannels`, `preferredChannel`, and optional `fallbackStrategy`.
- Criteria-level overrides can either set `useUserDefaults=true` or provide explicit override fields.

Representative verification response:

```json
{
  "id": "2b4f4f31-5a4c-45d8-b274-301f8c6fb5f4",
  "channel": "EMAIL",
  "destination": "alice@example.com",
  "status": "PENDING_VERIFICATION",
  "tokenExpiresAt": "2026-02-26T18:42:12Z",
  "verificationToken": "2aQWQCi4k9c43-SprCuhbkJYE1S8rFf5"
}
```

### Weather and Hydrology

| Method | Path | Notes |
| --- | --- | --- |
| `GET` | `/api/weather/active?page=0&size=50` | Paginated indexed weather read model |
| `GET` | `/api/weather/location?latitude={lat}&longitude={lon}` | NOAA alerts for a coordinate |
| `GET` | `/api/weather/state/{stateCode}` | NOAA alerts by state |
| `GET` | `/api/weather/conditions/current?latitude={lat}&longitude={lon}` | Latest current conditions |
| `GET` | `/api/weather/conditions/forecast?latitude={lat}&longitude={lon}&hours=48` | Hourly forecast conditions |
| `GET` | `/api/weather/hydrology/current?...` | NWPS observed river conditions |
| `GET` | `/api/weather/hydrology/forecast?...` | NWPS forecast river conditions |
| `GET` | `/api/weather/search/location/{location}` | Search indexed weather by location text |
| `GET` | `/api/weather/search/event/{eventType}` | Search indexed weather by event text |

Hydrology query options:

- `gaugeId=ABNG1`
- or `latitude`, `longitude`, and optional `radiusKm`

`GET /api/weather/active` returns a paged wrapper:

```json
{
  "items": [
    {
      "id": "NWS-IDP-PROD-1234567",
      "location": "Seattle, WA",
      "eventType": "Flood Warning",
      "severity": "MODERATE",
      "headline": "Flood Warning issued",
      "timestamp": "2026-02-24T19:10:00Z"
    }
  ],
  "page": 0,
  "size": 50,
  "totalElements": 1,
  "totalPages": 1,
  "hasNext": false,
  "hasPrevious": false
}
```

Weather normalization notes:

- Temperature is normalized to Celsius.
- Wind speed is normalized to km/h.
- Forecast rain uses `precipitationProbability`.
- Current observation rain uses `precipitationAmount`.
- Hydrology responses may include `riverGaugeId`, observed/forecast stages, flood/action stages, categories, unit, and distance.

### Admin Users

| Method | Path | Notes |
| --- | --- | --- |
| `GET` | `/api/admin/users` | List all accounts |
| `POST` | `/api/admin/users/{userId}/suspend` | Suspend account |
| `POST` | `/api/admin/users/{userId}/reactivate` | Reactivate account |
| `POST` | `/api/admin/users/{userId}/force-password-reset` | Require password reset next sign-in |

### Admin Jobs

| Method | Path | Notes |
| --- | --- | --- |
| `POST` | `/api/admin/jobs/weather-processing` | Pull NOAA data, evaluate criteria, create alerts |
| `POST` | `/api/admin/jobs/alert-delivery-retries` | Publish due delivery retries |
| `POST` | `/api/admin/jobs/data-retention` | Prune old alerts, weather rows, and orphaned criteria state |

Representative response:

```json
{
  "jobName": "weather-processing",
  "status": "COMPLETED",
  "startedAt": "2026-03-12T22:05:10Z",
  "finishedAt": "2026-03-12T22:05:12Z",
  "durationMillis": 1873,
  "message": "Weather processing completed successfully.",
  "metrics": {
    "criteriaEvaluated": 42,
    "alertsTriggered": 3
  }
}
```

These endpoints are what the React admin control panel at `/app/admin` invokes.

### Billing and Stripe

| Method | Path | Notes |
| --- | --- | --- |
| `GET` | `/api/billing/me` | Billing/subscription status |
| `POST` | `/api/billing/checkout-session` | Create Stripe Checkout session |
| `POST` | `/api/billing/portal-session` | Create Stripe Customer Portal session |
| `POST` | `/api/billing/change-plan` | Change plan for authenticated user |
| `POST` | `/api/stripe/webhook` | Public Stripe webhook |

Representative billing status:

```json
{
  "userId": "alice",
  "plan": "PLUS",
  "paidPlan": true,
  "maxActiveAlerts": 25,
  "adSponsoredEmails": false,
  "stripeCustomerId": "cus_123",
  "stripeSubscriptionId": "sub_123",
  "stripePriceId": "price_123",
  "stripeSubscriptionStatus": "active",
  "stripeCurrentPeriodEnd": "2026-04-12T00:00:00Z",
  "activeSubscription": true
}
```

Plan-changing requests use:

```json
{
  "plan": "PRO"
}
```

## Runtime Notes

- Weather search is backed by the PostgreSQL read model, not Elasticsearch.
- Alert delivery is handled by persisted delivery records plus in-process async workers.
- Admin jobs can be triggered either by an authenticated admin user or by `X-Admin-Job-Token`.
- Swagger/OpenAPI is the authoritative contract for field-level detail when examples here are abbreviated.

## Error Shape

Validation, auth, and application errors are returned as `application/problem+json`.

Representative validation error:

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
  "correlationId": "0fce4f73-cb93-4a4f-a5f6-0346765ccaf0",
  "errors": [
    {
      "field": "request",
      "message": "rainThreshold and rainThresholdType must be provided together"
    }
  ]
}
```
