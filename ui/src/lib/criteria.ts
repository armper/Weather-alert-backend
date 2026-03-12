import type { AlertCriteria, ComparisonDirection } from '../types'
import type { CriteriaFormState, RuleType } from '../state/types'

export const RIVER_STAGE_RULE_TYPES: RuleType[] = ['RIVER_STAGE_ABOVE', 'RIVER_STAGE_BELOW']
export const RIVER_RULE_TYPES: RuleType[] = [...RIVER_STAGE_RULE_TYPES, 'RIVER_FLOOD_CATEGORY']

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
    case 'HUMIDITY_ABOVE':
      return '85'
    case 'HUMIDITY_BELOW':
      return '30'
    case 'DEW_POINT_ABOVE':
      return '70'
    case 'DEW_POINT_BELOW':
      return '35'
    case 'WIND_GUST':
      return '40'
    case 'SKY_COVER_ABOVE':
      return '80'
    case 'SKY_COVER_BELOW':
      return '20'
    case 'RIVER_STAGE_ABOVE':
      return '10'
    case 'RIVER_STAGE_BELOW':
      return '5'
    case 'RIVER_FLOOD_CATEGORY':
      return ''
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
    radiusKm: Number(criteriaForm.gaugeSearchRadiusKm),
    riverGaugeId: criteriaForm.riverGaugeId.trim() || undefined,
    monitorCurrent: criteriaForm.monitorCurrent,
    monitorForecast: criteriaForm.monitorForecast,
    forecastWindowHours: Number(criteriaForm.forecastWindowHours),
    temperatureUnit: criteriaForm.temperatureUnit,
    oncePerEvent: criteriaForm.oncePerEvent,
    rearmWindowMinutes: Number(criteriaForm.rearmWindowMinutes),
  }

  const threshold = Number(criteriaForm.threshold)

  switch (criteriaForm.ruleType) {
    case 'TEMP_BELOW':
      payload.temperatureThreshold = threshold
      payload.temperatureDirection = 'BELOW'
      break
    case 'TEMP_ABOVE':
      payload.temperatureThreshold = threshold
      payload.temperatureDirection = 'ABOVE'
      break
    case 'WIND':
      payload.maxWindSpeed = threshold
      break
    case 'RAIN':
      payload.rainThreshold = threshold
      payload.rainThresholdType = 'PROBABILITY'
      break
    case 'HUMIDITY_ABOVE':
      payload.humidityThreshold = threshold
      payload.humidityDirection = 'ABOVE'
      break
    case 'HUMIDITY_BELOW':
      payload.humidityThreshold = threshold
      payload.humidityDirection = 'BELOW'
      break
    case 'DEW_POINT_ABOVE':
      payload.dewPointThreshold = threshold
      payload.dewPointDirection = 'ABOVE'
      break
    case 'DEW_POINT_BELOW':
      payload.dewPointThreshold = threshold
      payload.dewPointDirection = 'BELOW'
      break
    case 'WIND_GUST':
      payload.windGustThreshold = threshold
      break
    case 'SKY_COVER_ABOVE':
      payload.skyCoverThreshold = threshold
      payload.skyCoverDirection = 'ABOVE'
      break
    case 'SKY_COVER_BELOW':
      payload.skyCoverThreshold = threshold
      payload.skyCoverDirection = 'BELOW'
      break
    case 'RIVER_STAGE_ABOVE':
      payload.riverStageThreshold = threshold
      payload.riverStageDirection = 'ABOVE'
      break
    case 'RIVER_STAGE_BELOW':
      payload.riverStageThreshold = threshold
      payload.riverStageDirection = 'BELOW'
      break
    case 'RIVER_FLOOD_CATEGORY':
      payload.riverFloodCategoryThreshold = criteriaForm.riverFloodCategoryThreshold
      break
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
  if (criteria.humidityThreshold !== undefined && criteria.humidityThreshold !== null && criteria.humidityDirection) {
    return `${formatDirectionLabel(criteria.humidityDirection)} humidity ${formatNumber(criteria.humidityThreshold)}%`
  }
  if (criteria.dewPointThreshold !== undefined && criteria.dewPointThreshold !== null && criteria.dewPointDirection) {
    return `${formatDirectionLabel(criteria.dewPointDirection)} dew point ${formatNumber(criteria.dewPointThreshold)}°${
      criteria.temperatureUnit ?? 'F'
    }`
  }
  if (criteria.windGustThreshold !== undefined && criteria.windGustThreshold !== null) {
    return `Wind gust above ${formatNumber(criteria.windGustThreshold)} km/h`
  }
  if (criteria.skyCoverThreshold !== undefined && criteria.skyCoverThreshold !== null && criteria.skyCoverDirection) {
    return `${formatDirectionLabel(criteria.skyCoverDirection)} sky cover ${formatNumber(criteria.skyCoverThreshold)}%`
  }
  if (criteria.riverStageThreshold !== undefined && criteria.riverStageThreshold !== null && criteria.riverStageDirection) {
    const suffix = criteria.riverGaugeId ? ` at ${criteria.riverGaugeId}` : ''
    return `River stage ${criteria.riverStageDirection.toLowerCase()} ${formatNumber(criteria.riverStageThreshold)} ft${suffix}`
  }
  if (criteria.riverFloodCategoryThreshold) {
    const suffix = criteria.riverGaugeId ? ` at ${criteria.riverGaugeId}` : ''
    return `River flood category at or above ${criteria.riverFloodCategoryThreshold.toLowerCase()}${suffix}`
  }
  return 'Custom weather condition'
}

function formatDirectionLabel(direction: ComparisonDirection): string {
  return direction === 'ABOVE' ? 'At or above' : 'Below'
}

function formatNumber(value?: number | null): string {
  if (value === undefined || value === null || Number.isNaN(value)) {
    return '-'
  }
  return Number(value).toFixed(1).replace(/\.0$/, '')
}
