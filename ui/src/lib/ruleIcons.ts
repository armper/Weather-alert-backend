import type { ReactNode } from 'react'
import {
  CloudRain,
  CloudSun,
  Droplets,
  type LucideIcon,
  Moon,
  Snowflake,
  Sparkles,
  Sun,
  TriangleAlert,
  TrendingUp,
  Waves,
  Wind,
} from 'lucide-react'
import { renderAppIcon } from './appIcons'
import type { RuleBuilderIcon } from './ruleBuilder'
import type { AlertCriteria } from '../types'

function renderRuleIcon(icon: LucideIcon): ReactNode {
  return renderAppIcon(icon, 'rule-icon-glyph', 2.15)
}

export function resolveRuleEmoji(icon: RuleBuilderIcon): ReactNode {
  switch (icon) {
    case 'heat':
      return renderRuleIcon(Sun)
    case 'jacket':
      return renderRuleIcon(Snowflake)
    case 'rain':
      return renderRuleIcon(CloudRain)
    case 'wind':
      return renderRuleIcon(Wind)
    case 'humidity':
      return renderRuleIcon(Droplets)
    case 'dew':
      return renderRuleIcon(Moon)
    case 'river':
      return renderRuleIcon(TrendingUp)
    case 'flood':
      return renderRuleIcon(Waves)
    case 'alert':
      return renderRuleIcon(TriangleAlert)
    case 'sky':
      return renderRuleIcon(CloudSun)
    default:
      return renderRuleIcon(Sparkles)
  }
}

export function resolveCriteriaTileEmoji(criteria: AlertCriteria): ReactNode {
  if (criteria.temperatureThreshold != null) {
    return criteria.temperatureDirection === 'BELOW' ? renderRuleIcon(Snowflake) : renderRuleIcon(Sun)
  }
  if (criteria.rainThreshold != null) {
    return renderRuleIcon(CloudRain)
  }
  if (criteria.maxWindSpeed != null || criteria.windGustThreshold != null) {
    return renderRuleIcon(Wind)
  }
  if (criteria.humidityThreshold != null) {
    return renderRuleIcon(Droplets)
  }
  if (criteria.dewPointThreshold != null) {
    return renderRuleIcon(Moon)
  }
  if (criteria.skyCoverThreshold != null) {
    return renderRuleIcon(CloudSun)
  }
  if (criteria.riverFloodCategoryThreshold) {
    return renderRuleIcon(Waves)
  }
  if (criteria.riverStageThreshold != null) {
    return renderRuleIcon(TrendingUp)
  }
  return renderRuleIcon(Sparkles)
}
