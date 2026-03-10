# Billing Plan Progress

Tracking the rollout of Stripe-backed plan entitlements for Weather Alert.

## Goals

- Support `FREE`, `PLUS`, and `PRO` plans
- Keep free-tier email alerts ad-supported
- Enforce active alert limits in the backend
- Support multiple Stripe price IDs instead of a single subscription price
- Surface resolved plan + entitlements through the billing API
- Add account UI for checkout and billing visibility

## Current rollout

### Phase 1: Backend entitlements

- [x] Create a billing progress tracker
- [x] Add billing plan + entitlement model
- [x] Resolve plan from Stripe subscription status + price ID
- [x] Support multi-plan Stripe Checkout price selection
- [x] Enforce active-alert limits at criteria create/update time
- [x] Add free-tier ad footer to alert emails
- [x] Extend billing status response with resolved plan and entitlements
- [x] Add/update backend tests

### Phase 2: Account UI

- [x] Show current plan in the account page
- [x] Show free-tier limits and upgrade CTA
- [x] Let users choose `PLUS` or `PRO` before checkout
- [x] Add billing success/cancel UX

### Phase 3: Operations

- [x] Update deployment/config docs for multi-price Stripe env vars
- [x] Validate webhook flows with Stripe CLI
- [x] Smoke test checkout, upgrade, cancel, and downgrade flows

## Notes

- The repo already has Stripe foundations:
  - `/api/billing/me`
  - `/api/billing/checkout-session`
  - `/api/stripe/webhook`
- The current implementation is single-price only and stores raw Stripe fields on the user.
- Phase 1 now derives app entitlements from Stripe status + price ID without adding new user-table columns.
- Integration contract tests now clear alert criteria state for shared admin users before each test to keep the free-plan quota deterministic.
- Phase 2 now redirects Stripe success/cancel callbacks back into `/app/account`, so the SPA keeps the billing feedback visible after checkout.
- Phase 3 was validated end to end against the deployed Cloud Run backend on March 10, 2026:
  - free users are limited to 1 active alert
  - checkout sessions resolve the correct `PLUS` and `PRO` Stripe prices
  - Stripe webhook events move users through `FREE -> PLUS -> PRO -> FREE`
  - active paid subscribers cannot create another checkout session
