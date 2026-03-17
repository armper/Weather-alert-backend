import type { WeatherCondition } from '../types'

export type OfficialAlertTone = 'extreme' | 'severe' | 'warning' | 'advisory' | 'notice'

export interface OfficialAlertVisual {
  icon: string
  label: string
  tone: OfficialAlertTone
}

function normalize(value?: string): string {
  return (value ?? '').trim().toLowerCase()
}

function inferTone(item: Pick<WeatherCondition, 'severity' | 'eventType' | 'headline'>): OfficialAlertTone {
  const severity = normalize(item.severity)
  const eventText = `${normalize(item.eventType)} ${normalize(item.headline)}`

  if (severity === 'extreme') {
    return 'extreme'
  }
  if (severity === 'severe') {
    return 'severe'
  }
  if (eventText.includes('emergency') || eventText.includes('evacuation')) {
    return 'extreme'
  }
  if (eventText.includes('warning')) {
    return 'severe'
  }
  if (eventText.includes('watch')) {
    return 'warning'
  }
  if (eventText.includes('advisory')) {
    return 'advisory'
  }
  return 'notice'
}

export function resolveOfficialAlertVisual(item: Pick<WeatherCondition, 'severity' | 'eventType' | 'headline'>): OfficialAlertVisual {
  const text = `${normalize(item.eventType)} ${normalize(item.headline)}`
  const tone = inferTone(item)

  if (text.includes('tornado')) {
    return { icon: '🌪️', label: 'Tornado', tone }
  }
  if (text.includes('hurricane') || text.includes('tropical storm')) {
    return { icon: '🌀', label: 'Tropical', tone }
  }
  if (text.includes('flood') || text.includes('flash flood')) {
    return { icon: '🌊', label: 'Flood', tone }
  }
  if (text.includes('thunderstorm') || text.includes('lightning')) {
    return { icon: '⛈️', label: 'Storm', tone }
  }
  if (text.includes('wind')) {
    return { icon: '💨', label: 'Wind', tone }
  }
  if (text.includes('heat')) {
    return { icon: '🔥', label: 'Heat', tone }
  }
  if (text.includes('fire') || text.includes('smoke')) {
    return { icon: '🔥', label: 'Fire weather', tone }
  }
  if (text.includes('snow') || text.includes('blizzard') || text.includes('ice') || text.includes('freez')) {
    return { icon: '❄️', label: 'Winter', tone }
  }
  if (text.includes('rain')) {
    return { icon: '🌧️', label: 'Rain', tone }
  }
  if (text.includes('air quality')) {
    return { icon: '🌫️', label: 'Air quality', tone }
  }
  if (text.includes('marine') || text.includes('surf') || text.includes('coastal')) {
    return { icon: '🌊', label: 'Marine', tone }
  }

  return { icon: '⚠️', label: 'Official alert', tone }
}
