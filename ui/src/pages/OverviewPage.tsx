import { useEffect, useMemo, useState } from 'react'
import backgroundOverviewImage from '../assets/background-overview.png'
import { formatFriendlyLocation, formatTemperature } from '../lib/formatting'
import { useDataState } from '../state/useAppState'

export function OverviewPage() {
  const { currentWeather } = useDataState()
  const [now, setNow] = useState(() => new Date())

  useEffect(() => {
    const intervalId = window.setInterval(() => setNow(new Date()), 60_000)
    return () => {
      window.clearInterval(intervalId)
    }
  }, [])

  const locationLabel = formatFriendlyLocation(currentWeather?.location ?? 'Orlando')
  const temperatureLabel =
    currentWeather?.temperature != null ? formatTemperature(currentWeather.temperature, 'F') : '--'
  const greetingLabel = useMemo(() => {
    const hour = now.getHours()
    if (hour < 12) {
      return 'Good morning!'
    }
    if (hour < 18) {
      return 'Good afternoon!'
    }
    return 'Good evening!'
  }, [now])

  return (
    <section className="page-stack overview-page-stack">
      <div className="overview-page-background" aria-hidden="true">
        <img className="overview-page-background-image" src={backgroundOverviewImage} alt="" />
      </div>

      <div className="overview-page-content overview-page-content-fresh">
        <p className="overview-minimal-greeting" aria-live="polite">
          {greetingLabel}
        </p>

        <div className="overview-minimal-readout" aria-live="polite">
          <p className="overview-minimal-location">{locationLabel}</p>
          <p className="overview-minimal-temperature">{temperatureLabel}</p>
        </div>
      </div>
    </section>
  )
}
