# Top Features By Impact

Ranked by likely product impact for this weather alert backend.

1. **Multi-channel delivery that actually works (`SMS` first)**
   Impact: highest user-facing value.
   Why: the system already models channel preferences, verification, retries, and fallback. Real SMS delivery turns that foundation into a materially better alert product.

2. **User-controlled quiet hours and delivery escalation**
   Impact: very high retention and trust.
   Why: alerts are only useful if they arrive at the right time. Quiet hours plus "escalate to SMS only for severe alerts" makes the product feel production-ready.

3. **Digest mode plus alert grouping**
   Impact: very high for noise reduction.
   Why: current alerts are event-based; adding hourly/daily digests and grouping by region/event would make heavy-alert periods much more usable.

4. **Expanded geospatial targeting**
   Impact: high.
   Why: circles are good, but saved places, polygons, and "home/work/custom zones" would make criteria much more practical.

5. **Severe-weather escalation rules**
   Impact: high.
   Why: let users say "email for moderate, SMS for severe, immediate for tornado/flood warnings." This fits the existing rules engine well.

6. **Historical alert analytics for users**
   Impact: medium-high.
   Why: show how often a rule triggered, false-positive feeling, last matched values, and suggested threshold tuning.

7. **Team / household shared alerts**
   Impact: medium-high.
   Why: one admin could manage alerts for family, crews, schools, or field teams. Strong expansion path beyond single-user accounts.

8. **Webhook / outbound integration support**
   Impact: medium-high.
   Why: opens B2B use cases fast: Slack, Teams, PagerDuty, custom webhooks.

9. **In-app push notifications**
   Impact: medium.
   Why: useful, but lower than SMS because the system already has email and push requires client/device plumbing.

10. **Recommendation engine for rule creation**
    Impact: medium.
    Why: "suggest alerts for my area" or "popular severe-weather presets" would improve activation, but it matters less than better delivery.

## Recommended Next Features

1. **Real SMS delivery**
2. **Quiet hours + severity-based escalation**
3. **Digest/grouped alerts**

These three create the biggest jump in real-world usefulness without changing the core domain model much.
