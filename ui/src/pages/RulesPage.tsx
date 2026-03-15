import { useState } from 'react'
import backgroundOverviewImage from '../assets/background-overview.png'
import { QUICK_START_PRESETS, type RuleBuilderIcon } from '../lib/ruleBuilder'

function resolveRuleEmoji(icon: RuleBuilderIcon): string {
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

const TILE_SUBTITLES: Record<string, string> = {
  'chilly-weather': 'Temperature below 60°F',
  'hot-day-ahead': 'Temperature above 90°F',
  'rain-coming': 'Rain chance above 75%',
  'windy-outside': 'Wind speed above 30 km/h',
  'very-humid': 'Humidity above 80%',
  'warm-muggy-night': 'Dew point above 68°F',
  'river-rising': 'River stage above 8 ft',
  'river-problem': 'Flood category: Action',
  'minor-flooding': 'Flood category: Minor',
}

export function RulesPage() {
  const [enabled, setEnabled] = useState<Set<string>>(() => new Set())

  function toggle(id: string) {
    setEnabled((prev) => {
      const next = new Set(prev)
      if (next.has(id)) {
        next.delete(id)
      } else {
        next.add(id)
      }
      return next
    })
  }

  return (
    <section className="page-stack rules-page-fresh">
      <div className="overview-page-background" aria-hidden="true">
        <img className="overview-page-background-image" src={backgroundOverviewImage} alt="" />
      </div>

      <div className="rules-page-content">
        <p className="rules-page-subtitle">Tap a tile to enable an alert</p>

        <div className="rules-tile-grid">
          {QUICK_START_PRESETS.map((preset) => (
            <button
              key={preset.id}
              className={`rules-tile${enabled.has(preset.id) ? ' is-enabled' : ''}`}
              type="button"
              aria-pressed={enabled.has(preset.id)}
              onClick={() => toggle(preset.id)}
            >
              <span className="rules-tile-icon">{resolveRuleEmoji(preset.icon)}</span>
              <span className="rules-tile-name">{preset.title}</span>
              <span className="rules-tile-desc">
                {TILE_SUBTITLES[preset.id] ?? preset.description}
              </span>
            </button>
          ))}
        </div>
      </div>
    </section>
  )
}
