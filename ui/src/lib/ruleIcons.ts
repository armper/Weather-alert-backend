import type { RuleBuilderIcon } from './ruleBuilder'
import type { AlertCriteria } from '../types'

export function resolveRuleEmoji(icon: RuleBuilderIcon): string {
  switch (icon) {
    case 'heat':
      return '🔥'
    case 'jacket':
      return '🧥'
    case 'rain':
      return '🌧️'
    case 'wind':
      return '💨'
    case 'humidity':
      return '💧'
    case 'dew':
      return '🌙'
    case 'river':
      return '🏞️'
    case 'flood':
      return '🌊'
    case 'alert':
      return '🚨'
    case 'sky':
      return '☀️'
    default:
      return '✨'
  }
}

export function resolveCriteriaTileEmoji(criteria: AlertCriteria): string {
  if (criteria.temperatureThreshold != null) {
    return criteria.temperatureDirection === 'BELOW' ? '🧥' : '🔥'
  }
  if (criteria.rainThreshold != null) {
    return '🌧️'
  }
  if (criteria.maxWindSpeed != null || criteria.windGustThreshold != null) {
    return '💨'
  }
  if (criteria.humidityThreshold != null) {
    return '💧'
  }
  if (criteria.dewPointThreshold != null) {
    return '🌙'
  }
  if (criteria.skyCoverThreshold != null) {
    return '☀️'
  }
  if (criteria.riverFloodCategoryThreshold) {
    return '🌊'
  }
  if (criteria.riverStageThreshold != null) {
    return '🏞️'
  }
  return '✨'
}
