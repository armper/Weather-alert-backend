interface TrendSparklineProps {
  label: string
  data: (number | undefined | null)[]
  unit: string
}

export function TrendSparkline({ label, data, unit }: TrendSparklineProps) {
  const values = data.filter((value): value is number => value != null)

  if (values.length < 2) {
    return null
  }

  const width = 80
  const height = 24
  const min = Math.min(...values)
  const max = Math.max(...values)
  const range = max - min || 1

  const points = values
    .map((value, index) => {
      const x = (index / (values.length - 1)) * width
      const y = height - ((value - min) / range) * height
      return `${x.toFixed(1)},${y.toFixed(1)}`
    })
    .join(' ')

  const delta = values[values.length - 1] - values[0]
  const deltaLabel = `${delta >= 0 ? '▲' : '▼'} ${Math.abs(delta).toFixed(1)}${unit}`
  const deltaClass = delta > 0 ? 'trend-sparkline-delta-up' : delta < 0 ? 'trend-sparkline-delta-down' : ''

  return (
    <div className="trend-sparkline">
      <span className="trend-sparkline-label">{label}</span>
      <svg
        width={width}
        height={height}
        viewBox={`0 0 ${width} ${height}`}
        aria-hidden
        className="trend-sparkline-svg"
      >
        <polyline
          points={points}
          fill="none"
          stroke="currentColor"
          strokeWidth="1.5"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
      </svg>
      <span className={`trend-sparkline-delta ${deltaClass}`}>{deltaLabel}</span>
    </div>
  )
}
