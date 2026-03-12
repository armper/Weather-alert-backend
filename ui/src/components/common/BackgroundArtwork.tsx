import { useAppState } from '../../state/useAppState'

function resolveAtmosphereTone(headline?: string) {
  const lower = (headline ?? '').toLowerCase()
  if (lower.includes('rain') || lower.includes('storm') || lower.includes('shower')) {
    return 'rainy'
  }
  if (lower.includes('cloud') || lower.includes('overcast') || lower.includes('fog')) {
    return 'cloudy'
  }
  return 'clear'
}

export function BackgroundArtwork() {
  const { currentWeather } = useAppState()
  const tone = resolveAtmosphereTone(currentWeather?.headline)

  return (
    <div aria-hidden="true" className={`bg-art is-${tone}`}>
      <span className="blob blob-a" />
      <span className="blob blob-b" />
      <span className="blob blob-c" />
    </div>
  )
}
