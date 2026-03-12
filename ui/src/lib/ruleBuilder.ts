import type { FloodCategory } from '../types'
import type { RuleType } from '../state/types'

export type BuilderMode = 'SIMPLE' | 'ADVANCED'

export type SimpleSituationId =
  | 'HOT_WEATHER'
  | 'COLD_WEATHER'
  | 'RAIN_COMING'
  | 'STRONG_WIND'
  | 'VERY_HUMID'
  | 'MUGGY_NIGHT'
  | 'RIVER_RISING'
  | 'FLOOD_RISK'
  | 'CLEAR_SKIES'
  | 'CUSTOM'

export type RuleBuilderIcon =
  | 'heat'
  | 'jacket'
  | 'rain'
  | 'wind'
  | 'humidity'
  | 'dew'
  | 'river'
  | 'flood'
  | 'sky'
  | 'custom'
  | 'alert'

export interface SimpleSensitivityOption {
  id: string
  label: string
  description: string
  threshold?: string
  floodCategory?: FloodCategory
  custom?: boolean
}

export interface SimpleSituationConfig {
  id: SimpleSituationId
  title: string
  description: string
  helper: string
  icon: RuleBuilderIcon
  ruleType?: RuleType
  defaultThreshold?: string
  defaultTemperatureUnit?: 'F' | 'C'
  defaultFloodCategory?: FloodCategory
  defaultMonitorCurrent?: boolean
  defaultMonitorForecast?: boolean
  defaultForecastWindowHours?: string
  defaultOncePerEvent?: boolean
  sensitivityLabel?: string
  sensitivityOptions: SimpleSensitivityOption[]
}

export interface QuickStartPreset {
  id: string
  title: string
  description: string
  icon: RuleBuilderIcon
  situationId: Exclude<SimpleSituationId, 'CUSTOM'>
  sensitivityId: string
  forecastWindowHours?: string
  rearmWindowMinutes?: string
}

export interface AdvancedRuleOption {
  id: RuleType
  label: string
  description: string
  keywords: string[]
}

export interface AdvancedRuleGroup {
  id: string
  label: string
  options: AdvancedRuleOption[]
}

export const SIMPLE_SITUATIONS: SimpleSituationConfig[] = [
  {
    id: 'HOT_WEATHER',
    title: 'Hot weather',
    description: 'Alert when it gets unusually hot.',
    helper: 'Temperature above',
    icon: 'heat',
    ruleType: 'TEMP_ABOVE',
    defaultThreshold: '90',
    defaultTemperatureUnit: 'F',
    defaultMonitorCurrent: true,
    defaultMonitorForecast: true,
    defaultForecastWindowHours: '24',
    defaultOncePerEvent: true,
    sensitivityLabel: 'Sensitivity',
    sensitivityOptions: [
      { id: 'warm', label: 'Warm', description: 'Above 85°F', threshold: '85' },
      { id: 'hot', label: 'Hot', description: 'Above 90°F', threshold: '90' },
      { id: 'very-hot', label: 'Very hot', description: 'Above 98°F', threshold: '98' },
      { id: 'custom', label: 'Custom', description: 'Choose your own threshold', custom: true },
    ],
  },
  {
    id: 'COLD_WEATHER',
    title: 'Cold weather',
    description: 'Alert when it gets cold enough to matter.',
    helper: 'Temperature below',
    icon: 'jacket',
    ruleType: 'TEMP_BELOW',
    defaultThreshold: '60',
    defaultTemperatureUnit: 'F',
    defaultMonitorCurrent: true,
    defaultMonitorForecast: true,
    defaultForecastWindowHours: '24',
    defaultOncePerEvent: true,
    sensitivityLabel: 'Sensitivity',
    sensitivityOptions: [
      { id: 'little-cold', label: 'A little cold', description: 'Below 65°F', threshold: '65' },
      { id: 'cold', label: 'Cold', description: 'Below 60°F', threshold: '60' },
      { id: 'very-cold', label: 'Very cold', description: 'Below 50°F', threshold: '50' },
      { id: 'custom', label: 'Custom', description: 'Choose your own threshold', custom: true },
    ],
  },
  {
    id: 'RAIN_COMING',
    title: 'Rain coming',
    description: 'Alert when rain is likely soon.',
    helper: 'Rain chance at or above',
    icon: 'rain',
    ruleType: 'RAIN',
    defaultThreshold: '65',
    defaultMonitorCurrent: false,
    defaultMonitorForecast: true,
    defaultForecastWindowHours: '24',
    defaultOncePerEvent: true,
    sensitivityLabel: 'How likely?',
    sensitivityOptions: [
      { id: 'likely', label: 'Likely rain', description: '60% chance', threshold: '60' },
      { id: 'high-chance', label: 'High chance', description: '75% chance', threshold: '75' },
      { id: 'almost-certain', label: 'Almost certain', description: '90% chance', threshold: '90' },
      { id: 'custom', label: 'Custom', description: 'Choose your own threshold', custom: true },
    ],
  },
  {
    id: 'STRONG_WIND',
    title: 'Strong wind',
    description: 'Alert when it gets windy or gusty.',
    helper: 'Wind speed above',
    icon: 'wind',
    ruleType: 'WIND',
    defaultThreshold: '30',
    defaultMonitorCurrent: true,
    defaultMonitorForecast: true,
    defaultForecastWindowHours: '18',
    defaultOncePerEvent: true,
    sensitivityLabel: 'Sensitivity',
    sensitivityOptions: [
      { id: 'breezy', label: 'Breezy', description: 'Above 20 km/h', threshold: '20' },
      { id: 'windy', label: 'Windy', description: 'Above 30 km/h', threshold: '30' },
      { id: 'strong-gusts', label: 'Strong gusts', description: 'Above 40 km/h', threshold: '40' },
      { id: 'custom', label: 'Custom', description: 'Choose your own threshold', custom: true },
    ],
  },
  {
    id: 'VERY_HUMID',
    title: 'Very humid',
    description: 'Alert when the air starts feeling sticky.',
    helper: 'Humidity at or above',
    icon: 'humidity',
    ruleType: 'HUMIDITY_ABOVE',
    defaultThreshold: '80',
    defaultMonitorCurrent: false,
    defaultMonitorForecast: true,
    defaultForecastWindowHours: '18',
    defaultOncePerEvent: true,
    sensitivityLabel: 'Sensitivity',
    sensitivityOptions: [
      { id: 'noticeable', label: 'Noticeable humidity', description: 'Above 70%', threshold: '70' },
      { id: 'humid', label: 'Humid', description: 'Above 80%', threshold: '80' },
      { id: 'very-humid', label: 'Very humid', description: 'Above 90%', threshold: '90' },
      { id: 'custom', label: 'Custom', description: 'Choose your own threshold', custom: true },
    ],
  },
  {
    id: 'MUGGY_NIGHT',
    title: 'Muggy night',
    description: 'Alert when the air stays warm and sticky overnight.',
    helper: 'Dew point at or above',
    icon: 'dew',
    ruleType: 'DEW_POINT_ABOVE',
    defaultThreshold: '68',
    defaultTemperatureUnit: 'F',
    defaultMonitorCurrent: false,
    defaultMonitorForecast: true,
    defaultForecastWindowHours: '18',
    defaultOncePerEvent: true,
    sensitivityLabel: 'Sensitivity',
    sensitivityOptions: [
      { id: 'muggy', label: 'Muggy', description: 'Above 65°F dew point', threshold: '65' },
      { id: 'sticky', label: 'Sticky', description: 'Above 68°F dew point', threshold: '68' },
      { id: 'tropical', label: 'Tropical', description: 'Above 72°F dew point', threshold: '72' },
      { id: 'custom', label: 'Custom', description: 'Choose your own threshold', custom: true },
    ],
  },
  {
    id: 'RIVER_RISING',
    title: 'River rising',
    description: 'Alert when the nearby river starts rising.',
    helper: 'River stage above',
    icon: 'river',
    ruleType: 'RIVER_STAGE_ABOVE',
    defaultThreshold: '8',
    defaultMonitorCurrent: true,
    defaultMonitorForecast: true,
    defaultForecastWindowHours: '24',
    defaultOncePerEvent: true,
    sensitivityLabel: 'Sensitivity',
    sensitivityOptions: [
      { id: 'slight-rise', label: 'Slight rise', description: 'Above 6 ft', threshold: '6' },
      { id: 'rising', label: 'Rising', description: 'Above 8 ft', threshold: '8' },
      { id: 'concerning-rise', label: 'Concerning rise', description: 'Above 10 ft', threshold: '10' },
      { id: 'custom', label: 'Custom', description: 'Choose your own threshold', custom: true },
    ],
  },
  {
    id: 'FLOOD_RISK',
    title: 'Flood risk',
    description: 'Alert when river conditions become concerning.',
    helper: 'River flood category at or above',
    icon: 'flood',
    ruleType: 'RIVER_FLOOD_CATEGORY',
    defaultFloodCategory: 'ACTION',
    defaultMonitorCurrent: true,
    defaultMonitorForecast: true,
    defaultForecastWindowHours: '36',
    defaultOncePerEvent: true,
    sensitivityLabel: 'Sensitivity',
    sensitivityOptions: [
      { id: 'watch', label: 'Watch', description: 'Action stage', floodCategory: 'ACTION' },
      { id: 'minor', label: 'Minor flood', description: 'Minor flood stage', floodCategory: 'MINOR' },
      { id: 'moderate', label: 'Moderate flood', description: 'Moderate flood stage', floodCategory: 'MODERATE' },
      { id: 'custom', label: 'Custom', description: 'Choose your own level', custom: true },
    ],
  },
  {
    id: 'CLEAR_SKIES',
    title: 'Clear skies',
    description: 'Alert when clouds are expected to clear.',
    helper: 'Sky cover below',
    icon: 'sky',
    ruleType: 'SKY_COVER_BELOW',
    defaultThreshold: '20',
    defaultMonitorCurrent: false,
    defaultMonitorForecast: true,
    defaultForecastWindowHours: '12',
    defaultOncePerEvent: true,
    sensitivityLabel: 'Sensitivity',
    sensitivityOptions: [
      { id: 'partly-clearing', label: 'Partly clearing', description: 'Below 40% sky cover', threshold: '40' },
      { id: 'mostly-clear', label: 'Mostly clear', description: 'Below 25% sky cover', threshold: '25' },
      { id: 'clear', label: 'Clear', description: 'Below 10% sky cover', threshold: '10' },
      { id: 'custom', label: 'Custom', description: 'Choose your own threshold', custom: true },
    ],
  },
  {
    id: 'CUSTOM',
    title: 'Custom',
    description: 'Choose a raw weather condition yourself.',
    helper: 'Use the advanced builder',
    icon: 'custom',
    sensitivityOptions: [],
  },
]

export const QUICK_START_PRESETS: QuickStartPreset[] = [
  {
    id: 'chilly-weather',
    title: 'Chilly Weather',
    description: 'Get a heads-up when it is cool enough to want a jacket.',
    icon: 'jacket',
    situationId: 'COLD_WEATHER',
    sensitivityId: 'cold',
  },
  {
    id: 'hot-day-ahead',
    title: 'Hot Day Ahead',
    description: 'Get warned before the day turns very hot.',
    icon: 'heat',
    situationId: 'HOT_WEATHER',
    sensitivityId: 'hot',
  },
  {
    id: 'rain-coming',
    title: 'Rain Coming',
    description: 'Get warned when rain chances start looking high.',
    icon: 'rain',
    situationId: 'RAIN_COMING',
    sensitivityId: 'high-chance',
  },
  {
    id: 'windy-outside',
    title: 'Windy Outside',
    description: 'Get a heads-up when winds may get strong.',
    icon: 'wind',
    situationId: 'STRONG_WIND',
    sensitivityId: 'windy',
  },
  {
    id: 'very-humid',
    title: 'Very Humid',
    description: 'Get warned when the air starts feeling sticky and uncomfortable.',
    icon: 'humidity',
    situationId: 'VERY_HUMID',
    sensitivityId: 'humid',
  },
  {
    id: 'warm-muggy-night',
    title: 'Warm, Muggy Night',
    description: 'Get a heads-up when the evening may stay warm and muggy.',
    icon: 'dew',
    situationId: 'MUGGY_NIGHT',
    sensitivityId: 'sticky',
  },
  {
    id: 'river-rising',
    title: 'River Is Rising',
    description: 'Get a heads-up when the nearby river starts rising past your chosen level.',
    icon: 'river',
    situationId: 'RIVER_RISING',
    sensitivityId: 'rising',
  },
  {
    id: 'river-problem',
    title: 'River Could Become a Problem',
    description: 'Get warned when the nearby river may be heading toward trouble.',
    icon: 'alert',
    situationId: 'FLOOD_RISK',
    sensitivityId: 'watch',
  },
  {
    id: 'minor-flooding',
    title: 'Minor Flooding Possible',
    description: 'Get warned when the nearby river may start causing minor flooding.',
    icon: 'flood',
    situationId: 'FLOOD_RISK',
    sensitivityId: 'minor',
  },
]

export const ADVANCED_RULE_GROUPS: AdvancedRuleGroup[] = [
  {
    id: 'temperature',
    label: 'Temperature',
    options: [
      { id: 'TEMP_ABOVE', label: 'Temperature above', description: 'Alert when it gets hotter than your threshold.', keywords: ['heat', 'hot', 'temperature', 'above', 'warm'] },
      { id: 'TEMP_BELOW', label: 'Temperature below', description: 'Alert when it gets colder than your threshold.', keywords: ['cold', 'freeze', 'temperature', 'below', 'jacket'] },
    ],
  },
  {
    id: 'rain-storms',
    label: 'Rain and storms',
    options: [
      { id: 'RAIN', label: 'Rain probability at/above', description: 'Use forecast rain probability as the trigger.', keywords: ['rain', 'storm', 'precipitation', 'chance', 'wet'] },
    ],
  },
  {
    id: 'wind',
    label: 'Wind',
    options: [
      { id: 'WIND', label: 'Wind speed above', description: 'Trigger from sustained wind speed.', keywords: ['wind', 'breezy', 'gust', 'sustained'] },
      { id: 'WIND_GUST', label: 'Wind gust above', description: 'Trigger from stronger gust spikes.', keywords: ['wind', 'gust', 'strong', 'blowing'] },
    ],
  },
  {
    id: 'humidity-comfort',
    label: 'Humidity and comfort',
    options: [
      { id: 'HUMIDITY_ABOVE', label: 'Humidity at/above', description: 'Alert when the air feels more humid.', keywords: ['humidity', 'humid', 'sticky', 'above'] },
      { id: 'HUMIDITY_BELOW', label: 'Humidity below', description: 'Alert when the air gets drier.', keywords: ['humidity', 'dry', 'below'] },
      { id: 'DEW_POINT_ABOVE', label: 'Dew point at/above', description: 'Good for muggy-night comfort watches.', keywords: ['dew', 'muggy', 'sticky', 'comfort', 'above'] },
      { id: 'DEW_POINT_BELOW', label: 'Dew point below', description: 'Alert when dew point drops off.', keywords: ['dew', 'dry', 'below'] },
    ],
  },
  {
    id: 'sky',
    label: 'Sky',
    options: [
      { id: 'SKY_COVER_ABOVE', label: 'Sky cover at/above', description: 'Watch for increasing cloud cover.', keywords: ['sky', 'cloud', 'overcast', 'cover', 'above'] },
      { id: 'SKY_COVER_BELOW', label: 'Sky cover below', description: 'Watch for clearing skies.', keywords: ['sky', 'clear', 'cloud', 'cover', 'below'] },
    ],
  },
  {
    id: 'river',
    label: 'River and flooding',
    options: [
      { id: 'RIVER_STAGE_ABOVE', label: 'River stage above', description: 'Alert when the river rises above a stage.', keywords: ['river', 'rising', 'stage', 'flood', 'above'] },
      { id: 'RIVER_STAGE_BELOW', label: 'River stage below', description: 'Alert when the river drops below a stage.', keywords: ['river', 'stage', 'below', 'drop'] },
      { id: 'RIVER_FLOOD_CATEGORY', label: 'River flood category at/above', description: 'Alert by flood-risk stage instead of raw feet.', keywords: ['river', 'flood', 'category', 'risk', 'stage'] },
    ],
  },
]

export function getSituationConfig(id: SimpleSituationId) {
  return SIMPLE_SITUATIONS.find((item) => item.id === id) ?? null
}

export function getSituationForRuleType(ruleType: RuleType): SimpleSituationId {
  const match = SIMPLE_SITUATIONS.find((item) => item.ruleType === ruleType)
  return match?.id ?? 'CUSTOM'
}

export function getSensitivityForSituation(situationId: SimpleSituationId, sensitivityId: string) {
  return getSituationConfig(situationId)?.sensitivityOptions.find((item) => item.id === sensitivityId) ?? null
}

export function resolveMatchingSensitivityId(
  situationId: SimpleSituationId,
  threshold: string,
  floodCategory: FloodCategory,
): string {
  const situation = getSituationConfig(situationId)
  if (!situation || situation.id === 'CUSTOM') {
    return 'custom'
  }

  const match = situation.sensitivityOptions.find((item) => {
    if (item.custom) {
      return false
    }
    if (item.floodCategory) {
      return item.floodCategory === floodCategory
    }
    return item.threshold === threshold
  })

  return match?.id ?? 'custom'
}

export function filterAdvancedRuleGroups(query: string): AdvancedRuleGroup[] {
  const trimmedQuery = query.trim().toLowerCase()
  if (!trimmedQuery) {
    return ADVANCED_RULE_GROUPS
  }

  return ADVANCED_RULE_GROUPS.map((group) => ({
    ...group,
    options: group.options.filter((option) => {
      const haystack = [option.label, option.description, ...option.keywords].join(' ').toLowerCase()
      return haystack.includes(trimmedQuery)
    }),
  })).filter((group) => group.options.length > 0)
}

export function getAdvancedRuleLabel(ruleType: RuleType) {
  for (const group of ADVANCED_RULE_GROUPS) {
    const match = group.options.find((option) => option.id === ruleType)
    if (match) {
      return match.label
    }
  }
  return 'Custom weather condition'
}
