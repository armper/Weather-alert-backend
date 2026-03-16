import backgroundChanceOfRainImage from '../assets/background-chance-of-rain.png'
import backgroundCloudyImage from '../assets/background-cloudy.png'
import backgroundFogImage from '../assets/background-fog.png'
import backgroundOverviewImage from '../assets/background-overview.png'
import backgroundPartlyCloudyImage from '../assets/background-partly-cloudy.png'
import backgroundRainImage from '../assets/background-rain.png'
import backgroundSnowImage from '../assets/background-snow.png'
import backgroundThunderstormImage from '../assets/background-thunderstorm.png'
import type { WeatherCondition } from '../types'

interface WeatherVisual {
  backgroundImage: string
  icon: string
  label: string
}

function resolveFromHeadline(headline?: string): { icon: string; label: string } | null {
  if (!headline) return null
  const normalized = headline.toLowerCase()

  if (
    normalized.includes('snow') ||
    normalized.includes('blizzard') ||
    normalized.includes('ice') ||
    normalized.includes('sleet') ||
    normalized.includes('freezing')
  ) {
    return { icon: '❄️', label: 'Snow' }
  }
  if (normalized.includes('thunder') || normalized.includes('tstms')) {
    return { icon: '⛈️', label: 'Thunderstorms' }
  }
  if (normalized.includes('rain') || normalized.includes('drizzle') || normalized.includes('shower')) {
    return { icon: '🌧️', label: 'Rainy' }
  }
  if (normalized.includes('fog') || normalized.includes('mist') || normalized.includes('haze')) {
    return { icon: '🌫️', label: 'Foggy' }
  }
  if (normalized.includes('overcast') || normalized.includes('mostly cloudy') || normalized.includes('broken')) {
    return { icon: '☁️', label: 'Cloudy' }
  }
  if (normalized.includes('cloud')) {
    return { icon: '☁️', label: 'Cloudy' }
  }
  if (
    normalized.includes('partly') ||
    normalized.includes('few clouds') ||
    normalized.includes('scattered') ||
    normalized.includes('mostly sunny') ||
    normalized.includes('mostly clear')
  ) {
    return { icon: '⛅', label: 'Partly cloudy' }
  }
  if (normalized.includes('fair') || normalized.includes('sunny') || normalized.includes('clear') || normalized.includes('hot')) {
    return { icon: '☀️', label: 'Sunny' }
  }

  return null
}

function resolveFromNumeric(item: Partial<WeatherCondition>): { icon: string; label: string } {
  if ((item.iceAccumulation ?? 0) > 0 || (item.snowfallAmount ?? 0) > 0) {
    return { icon: '❄️', label: 'Snow' }
  }
  if ((item.probabilityOfThunder ?? 0) > 30) {
    return { icon: '⛈️', label: 'Thunderstorms' }
  }
  if ((item.precipitationAmount ?? 0) > 0 || (item.precipitationProbability ?? 0) > 65) {
    return { icon: '🌧️', label: 'Rainy' }
  }
  if ((item.precipitationProbability ?? 0) > 35) {
    return { icon: '🌦️', label: 'Chance of rain' }
  }
  if ((item.skyCover ?? 0) > 75) {
    return { icon: '☁️', label: 'Cloudy' }
  }
  if ((item.skyCover ?? 0) > 40) {
    return { icon: '⛅', label: 'Partly cloudy' }
  }

  return { icon: '☀️', label: 'Sunny' }
}

function resolveBackgroundImage(label: string): string {
  if (label === 'Thunderstorms') return backgroundThunderstormImage
  if (label === 'Snow') return backgroundSnowImage
  if (label === 'Rainy') return backgroundRainImage
  if (label === 'Chance of rain') return backgroundChanceOfRainImage
  if (label === 'Foggy') return backgroundFogImage
  if (label === 'Cloudy') return backgroundCloudyImage
  if (label === 'Partly cloudy') return backgroundPartlyCloudyImage
  return backgroundOverviewImage
}

export function resolveWeatherVisual(item: Partial<WeatherCondition>): WeatherVisual {
  const condition = resolveFromHeadline(item.headline) ?? resolveFromNumeric(item)
  return {
    ...condition,
    backgroundImage: resolveBackgroundImage(condition.label),
  }
}
