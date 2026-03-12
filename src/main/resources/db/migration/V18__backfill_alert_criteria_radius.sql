UPDATE alert_criteria
SET radius_km = 80
WHERE radius_km IS NULL
  AND latitude IS NOT NULL
  AND longitude IS NOT NULL;
