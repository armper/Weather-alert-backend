import type { AlertCriteria } from '../types'
import type { CriteriaFormState, RuleType } from '../state/types'

export function defaultThreshold(ruleType: RuleType): string {
  switch (ruleType) {
    case 'TEMP_BELOW':
      return '60'
    case 'TEMP_ABOVE':
      return '90'
    case 'WIND':
      return '25'
    case 'RAIN':
      return '50'
    default:
      return '60'
  }
}

export function buildCriteriaPayload(criteriaForm: CriteriaFormState, userId: string) {
  const payload: Record<string, unknown> = {
    userId,
    name: criteriaForm.name.trim(),
    location: criteriaForm.location.trim(),
    latitude: Number(criteriaForm.latitude),
    longitude: Number(criteriaForm.longitude),
    monitorCurrent: criteriaForm.monitorCurrent,
    monitorForecast: criteriaForm.monitorForecast,
    forecastWindowHours: Number(criteriaForm.forecastWindowHours),
    temperatureUnit: criteriaForm.temperatureUnit,
    oncePerEvent: criteriaForm.oncePerEvent,
    rearmWindowMinutes: Number(criteriaForm.rearmWindowMinutes),
  }

  const threshold = Number(criteriaForm.threshold)

  if (criteriaForm.ruleType === 'TEMP_BELOW') {
    payload.temperatureThreshold = threshold
    payload.temperatureDirection = 'BELOW'
  }
  if (criteriaForm.ruleType === 'TEMP_ABOVE') {
    payload.temperatureThreshold = threshold
    payload.temperatureDirection = 'ABOVE'
  }
  if (criteriaForm.ruleType === 'WIND') {
    payload.maxWindSpeed = threshold
  }
  if (criteriaForm.ruleType === 'RAIN') {
    payload.rainThreshold = threshold
    payload.rainThresholdType = 'PROBABILITY'
  }

  return payload
}

export function describeCriteria(criteria: AlertCriteria): string {
  if (criteria.temperatureThreshold !== undefined && criteria.temperatureThreshold !== null && criteria.temperatureDirection) {
    return `Temperature ${criteria.temperatureDirection.toLowerCase()} ${formatNumber(criteria.temperatureThreshold)}°${
      criteria.temperatureUnit ?? 'F'
    }`
  }
  if (criteria.maxWindSpeed !== undefined && criteria.maxWindSpeed !== null) {
    return `Wind speed above ${formatNumber(criteria.maxWindSpeed)} km/h`
  }
  if (criteria.rainThreshold !== undefined && criteria.rainThreshold !== null && criteria.rainThresholdType) {
    if (criteria.rainThresholdType === 'PROBABILITY') {
      return `Rain chance at or above ${formatNumber(criteria.rainThreshold)}%`
    }
    return `Rain amount at or above ${formatNumber(criteria.rainThreshold)} mm`
  }
  return 'Custom weather condition'
}

function formatNumber(value?: number | null): string {
  if (value === undefined || value === null || Number.isNaN(value)) {
    return '-'
  }
  return Number(value).toFixed(1).replace(/\.0$/, '')
}
