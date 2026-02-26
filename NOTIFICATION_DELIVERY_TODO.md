# Notification Delivery TODO

## Goals
- Add email delivery first, with clean support for SMS/push later.
- Support user-level notification preferences and per-criteria overrides.
- Support channel verification before delivery.
- Support channel fallback (`FIRST_SUCCESS`) and delivery observability.

## Chunk 1: Schema + Domain Foundations
- [x] Add Flyway migration for:
- [x] `user_notification_preferences`
- [x] `criteria_notification_preferences`
- [x] `channel_verifications`
- [x] `alert_delivery`
- [x] Add domain models/enums for channels, fallback strategy, verification, and delivery records.
- [x] Add repository ports/adapters/entities for new tables.
- [x] Update README/API docs with migration notes and roadmap pointer.

## Chunk 2: Preference Resolution Service
- [x] Implement preference resolution service.
- [x] Resolve effective channels from user defaults + criteria overrides.
- [x] Enforce default `FIRST_SUCCESS` strategy.
- [x] Add validation for impossible/empty channel configurations.
- [x] Add unit tests for resolution matrix.

## Chunk 3: Verification Flow
- [x] Add email verification flow:
- [x] Start verification (token issue + persistence).
- [x] Confirm verification (token validation + status update).
- [x] Block unverified channels from delivery selection.
- [x] Add API endpoints + DTOs + tests.

## Chunk 4: Email Delivery Adapter
- [ ] Add `EmailSenderPort` and concrete adapter.
- [ ] Local dev sender via MailHog SMTP.
- [ ] Production sender via AWS SES.
- [ ] Add provider error mapping for retry vs non-retryable failures.
- [ ] Add integration tests for sender behavior.

## Chunk 5: Delivery Worker + Retry
- [ ] Add async delivery worker consuming new delivery tasks.
- [ ] Persist delivery attempts in `alert_delivery`.
- [ ] Add retry with backoff and max-attempt policy.
- [ ] Add idempotency guard (`alert_id + channel`).
- [ ] Add DLQ path for permanent failures.

## Chunk 6: API for Preferences
- [ ] Add endpoints:
- [ ] `GET/PUT /api/users/me/notification-preferences`
- [ ] Criteria-level overrides (inherit or explicit channels).
- [ ] Add request/response examples in OpenAPI.
- [ ] Add auth rules and tests.

## Chunk 7: Observability + Ops
- [ ] Add metrics: attempts/success/failure/retry by channel/provider.
- [ ] Add structured logs with `alertId`, `channel`, `deliveryId`, `providerMessageId`.
- [ ] Add dashboard/query examples.
- [ ] Add retention cleanup support for `alert_delivery`.

## Chunk 8: SMS Extension (Later)
- [ ] Add `SmsSenderPort` and provider adapter.
- [ ] Add SMS verification flow.
- [ ] Reuse same fallback and delivery tracking pipeline.
- [ ] Add docs/tests for mixed channel behavior.

## Documentation Rule
- [ ] Update README for each completed chunk before marking it done.
- [ ] Update API.md whenever contracts change.
- [ ] Add migration notes when schema changes.
