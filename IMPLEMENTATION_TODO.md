# Weather Conditions Alerts TODO

## Confirmed Product Requirements
- [x] Location input can be either city or coordinates in future; v1 does not need to enforce one strict mode.
- [x] Forecast horizon default is 48 hours.
- [x] Rain alert thresholds are user-configurable.
- [x] Alerts should repeat for new conditions (not once forever), with anti-spam logic.
- [x] Temperature unit preference must support both Fahrenheit and Celsius.
- [x] If a new criteria is already true when created, notify immediately.
- [x] Users can have multiple criteria.
- [x] Adopt Flyway for schema migrations.

## Chunk 1: Flyway + DB Baseline
- [x] Add Flyway dependency and disable implicit schema drift risk (keep behavior stable during migration rollout).
- [x] Create baseline migration for existing tables.
- [ ] Add migrations for all new tables/columns introduced in later chunks.
- [x] Add local/dev migration notes to README.
- [x] Update API/ARCHITECTURE docs for migration strategy.

## Chunk 2: Domain Model Extensions
- [x] Extend `AlertCriteria` for weather-condition monitoring:
- [x] Add `temperatureThreshold` with direction (`BELOW`/`ABOVE`).
- [x] Add `rainThreshold` and threshold type (e.g., probability, amount).
- [x] Add `monitorCurrent` and `monitorForecast`.
- [x] Add `forecastWindowHours` (default 48).
- [x] Add `temperatureUnit` preference (`F`/`C`).
- [x] Add `oncePerEvent`/rearm policy fields for anti-spam behavior.
- [x] Extend DTOs and validation rules for new criteria fields.
- [x] Update Swagger examples for new criteria schema.
- [x] Update README + API docs with new criteria contract.

## Chunk 3: Weather Data Provider (Current + Forecast)
- [x] Add NOAA integration for current observations by location.
- [x] Add NOAA integration for forecast periods (at least 48h window support).
- [x] Normalize provider payloads into internal weather-condition models.
- [x] Add unit conversion helpers (F/C) and canonical storage strategy.
- [x] Add resilient error handling, timeouts, and fallback behavior.
- [x] Add provider-level tests/mocks.
- [x] Document data source behavior and limits in README/ARCHITECTURE.

## Chunk 4: Criteria Evaluation Engine
- [x] Refactor `AlertCriteria.matches` into explicit rule evaluation components.
- [x] Implement temperature evaluation:
- [x] Evaluate current condition.
- [x] Evaluate forecast condition within 48h window.
- [x] Implement rain evaluation:
- [x] Evaluate current rain condition.
- [x] Evaluate forecasted rain condition against user threshold.
- [x] Ensure combined conditions and multi-criteria are evaluated correctly.
- [x] Ensure criteria true-at-creation can trigger immediate notify.
- [x] Add focused unit tests for all rule combinations.
- [x] Update README/API docs with rule semantics.

## Chunk 5: One-Time-Per-Event Anti-Spam State
- [x] Add `criteria_state` persistence model for edge-triggering.
- [x] Store last condition status (`met`/`not met`) and last notified event signature.
- [x] Implement transition logic (`not met -> met` triggers notify).
- [x] Implement rearm logic for repeat events after condition clears.
- [x] Ensure repeated rain events can notify multiple times per week without spam.
- [x] Add DB indexes for efficient scheduler scans.
- [x] Add integration tests for dedupe/rearm behavior.
- [x] Document anti-spam logic and examples in README.

## Chunk 6: Alert Persistence + Lifecycle
- [ ] Extend alert schema with `eventKey`, `reason`, and weather-condition metadata.
- [ ] Set explicit alert lifecycle transitions (`PENDING -> SENT -> ACKNOWLEDGED/EXPIRED`) where applicable.
- [ ] Prevent duplicate inserts for same criteria/event window.
- [ ] Add repository queries for dedupe checks and alert history.
- [ ] Add tests for lifecycle and duplicate protection.
- [ ] Update API docs for new response fields.

## Chunk 7: API Endpoints + UX Consistency
- [ ] Update create/update criteria endpoints for new fields and validation errors.
- [ ] Add query support for user preference and condition-specific filtering if needed.
- [ ] Keep response payloads concise and predictable.
- [ ] Ensure OpenAPI examples remain realistic and copy/paste usable.
- [ ] Add changelog-style notes in README for new endpoints/fields.

## Chunk 8: Scheduler + Orchestration
- [ ] Update scheduler flow to fetch conditions, evaluate criteria, and publish alerts.
- [ ] Add safe batching/rate limiting for external provider calls.
- [ ] Add observability around rule evaluations and trigger counts.
- [ ] Add guardrails for provider outages (no false positives, recover cleanly).
- [ ] Document operational behavior in README/DEPLOYMENT.

## Chunk 9: Observability + Monitoring
- [ ] Add metrics:
- [ ] Criteria evaluated count.
- [ ] Triggered alert count.
- [ ] Deduped/skipped count.
- [ ] Provider latency and failure rate.
- [ ] Add log fields for criteria ID, event key, and decision outcome.
- [ ] Add Grafana panel/query examples for new metrics/logs.
- [ ] Update observability docs in README.

## Chunk 10: End-to-End Test Matrix
- [ ] Temperature drop below threshold (current) triggers alert once per event.
- [ ] Temperature forecast below threshold triggers alert once per event.
- [ ] Rain current condition triggers alert once per event.
- [ ] Rain forecast condition triggers alert once per event.
- [ ] Condition clears and re-occurs triggers new alert.
- [ ] Multiple criteria per user produce independent alerts.
- [ ] Unit preference (F/C) behaves correctly.
- [ ] Immediate trigger at criteria creation when already true.
- [ ] Add manual test playbook commands to README.

## Documentation Rule (Do This Every Chunk)
- [ ] For each completed chunk, update README before marking chunk done.
- [ ] For each completed chunk, update API.md and ARCHITECTURE.md when contracts/flows change.
- [ ] For each completed chunk, include migration notes if schema changed.
