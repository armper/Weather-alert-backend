# NOAA Feature Opportunities

Feature ideas grounded in NOAA's published API capabilities and adjacent NOAA data services.

1. **Richer threshold alerts from `forecastGridData`**
   Use NOAA's raw grid forecast, not just hourly summary, to add rules like humidity, dew point, wind gust, sky cover, heat-risk proxies, snowfall/ice proxies, and better rain accumulation logic.
   Source:
   - https://www.weather.gov/documentation/services-web-api
   - https://api.weather.gov/openapi.json

2. **Point-aware alert coverage and smarter geofencing**
   NOAA's `/points/{lat},{lon}` returns the forecast office, observation stations, and zones for a location. That lets you build "home/work/route" area models and improve alert matching by point, county, and zone.
   Source:
   - https://www.weather.gov/documentation/services-web-api
   - https://www.weather.gov/media/documentation/docs/NWS_Geolocation.pdf

3. **7-day alert history and post-event timelines**
   NOAA distinguishes `/alerts` from `/alerts/active`; `/alerts` includes alerts issued over the last 7 days. That is enough for user history, recent-event summaries, "what happened while I was away," and tuning recommendations.
   Source:
   - https://www.weather.gov/documentation/services-web-api

4. **Better alert intelligence from CAP fields**
   NOAA added more CAP fields to individual alerts in May 2025, including `eventCode`, `scope`, `language`, and `web`. That supports better routing, severity presets, linked official detail pages, and richer audit/history views.
   Source:
   - https://www.weather.gov/media/notification/pdf_2025/scn25-44_API_latest_changesmay22_2025.pdf

5. **Near-real-time alert sync using ATOM/CAP**
   Instead of naive polling, use NOAA's alert indexes as change feeds, then fetch full products only when needed. This is a strong fit for the existing Kafka pipeline.
   Source:
   - https://www.weather.gov/documentation/services-web-alerts

6. **Aviation mode**
   NOAA notes TAFs are available again from `/stations/{stationId}/tafs`. That enables airport-area alerts for low ceilings, gusts, visibility, and flight planning users.
   Source:
   - https://www.weather.gov/documentation/services-web-api

7. **Marine/boater mode**
   NOAA says coastal marine grid forecasts are only available via `forecastGridData`. You could add coastal wind and wave-oriented alert products for marinas, fishing, and boating users.
   Source:
   - https://www.weather.gov/documentation/services-web-api

8. **Flood and river-stage alerts via NOAA NWPS**
   Separate from `api.weather.gov`, NOAA's National Water Prediction Service API exposes waterway observations, forecasts, and HEFS ensemble data. This is probably the best adjacent product expansion.
   Source:
   - https://water.noaa.gov/about/api
   - https://www.weather.gov/index.php/dmx/nwps_info

9. **Radar and precipitation overlays**
   NOAA explicitly says `api.weather.gov` does not provide radar display data, but points to RIDGE2, OGC radar services, and MRMS. This would make the UI much stronger visually.
   Source:
   - https://www.weather.gov/documentation/services-web-api

10. **Observational confidence/status UX**
    NOAA documents that station observations can be delayed by up to about 20 minutes due to MADIS quality control. Exposing "last observation age" would improve trust in current-condition alerts.
    Source:
    - https://www.weather.gov/documentation/services-web-api

## Best Next 3 From NOAA

1. **`forecastGridData`-based advanced rules**
2. **7-day alert history plus CAP-enriched alert timelines**
3. **NWPS flood and river integration**

## Notes

- NOAA recommends not requesting alerts more often than every 30 seconds.
  Source:
  - https://www.weather.gov/documentation/services-web-alerts

- These are product ideas inferred from NOAA's published endpoints and documentation, not explicit NOAA feature recommendations.
