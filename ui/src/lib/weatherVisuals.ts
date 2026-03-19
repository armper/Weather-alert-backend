import type { ReactNode } from 'react'
import { Cloud, CloudDrizzle, CloudFog, CloudLightning, CloudRain, CloudSnow, CloudSun, Sun } from 'lucide-react'
import { renderAppIcon } from './appIcons'
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
  icon: ReactNode
  label: string
}

function resolveFromText(value?: string): { icon: ReactNode; label: string } | null {
  if (!value) return null
  const normalized = value.toLowerCase()

  if (
    normalized.includes('snow') ||
    normalized.includes('blizzard') ||
    normalized.includes('ice') ||
    normalized.includes('sleet') ||
    normalized.includes('freezing')
  ) {
    return { icon: renderAppIcon(CloudSnow), label: 'Snow' }
  }
  if (normalized.includes('thunder') || normalized.includes('tstms')) {
    return { icon: renderAppIcon(CloudLightning), label: 'Thunderstorms' }
  }
  if (normalized.includes('rain') || normalized.includes('drizzle') || normalized.includes('shower')) {
    return { icon: renderAppIcon(CloudRain), label: 'Rainy' }
  }
  if (normalized.includes('fog') || normalized.includes('mist') || normalized.includes('haze')) {
    return { icon: renderAppIcon(CloudFog), label: 'Foggy' }
  }
  if (normalized.includes('overcast') || normalized.includes('mostly cloudy') || normalized.includes('broken')) {
    return { icon: renderAppIcon(Cloud), label: 'Cloudy' }
  }
  if (normalized.includes('cloud')) {
    return { icon: renderAppIcon(Cloud), label: 'Cloudy' }
  }
  if (
    normalized.includes('partly') ||
    normalized.includes('few clouds') ||
    normalized.includes('scattered') ||
    normalized.includes('mostly sunny') ||
    normalized.includes('mostly clear')
  ) {
    return { icon: renderAppIcon(CloudSun), label: 'Partly cloudy' }
  }
  if (normalized.includes('fair') || normalized.includes('sunny') || normalized.includes('clear') || normalized.includes('hot')) {
    return { icon: renderAppIcon(Sun), label: 'Sunny' }
  }

  return null
}

function resolveFromNumeric(item: Partial<WeatherCondition>): { icon: ReactNode; label: string } {
  if ((item.iceAccumulation ?? 0) > 0 || (item.snowfallAmount ?? 0) > 0) {
    return { icon: renderAppIcon(CloudSnow), label: 'Snow' }
  }
  if ((item.probabilityOfThunder ?? 0) > 30) {
    return { icon: renderAppIcon(CloudLightning), label: 'Thunderstorms' }
  }
  if ((item.precipitationAmount ?? 0) > 0 || (item.precipitationProbability ?? 0) > 65) {
    return { icon: renderAppIcon(CloudRain), label: 'Rainy' }
  }
  if ((item.precipitationProbability ?? 0) > 35) {
    return { icon: renderAppIcon(CloudDrizzle), label: 'Chance of rain' }
  }
  if ((item.skyCover ?? 0) > 75) {
    return { icon: renderAppIcon(Cloud), label: 'Cloudy' }
  }
  if ((item.skyCover ?? 0) > 40) {
    return { icon: renderAppIcon(CloudSun), label: 'Partly cloudy' }
  }

  return { icon: renderAppIcon(Sun), label: 'Sunny' }
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
  const condition = resolveFromText(item.headline) ?? resolveFromText(item.eventType) ?? resolveFromNumeric(item)
  return {
    ...condition,
    backgroundImage: resolveBackgroundImage(condition.label),
  }
}
