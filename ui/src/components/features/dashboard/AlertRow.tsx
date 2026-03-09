import type { AlertEvent } from '../../../types'
import {
  formatDate,
  formatMetricLabel,
  formatNumber,
  formatMillimeters,
  formatPercent,
  formatRelativeTime,
  formatStatusLabel,
  formatTemperature,
  formatWind,
} from '../../../lib/formatting'
import { AriaButton } from '../../ui/AriaButton'

interface AlertRowProps {
  alert: AlertEvent
  busy: boolean
  duplicateCount?: number
  onAcknowledge: (alertId: string) => void
}

const STATUS_ICON: Record<string, string> = {
  pending: '…',
  sent: '✉',
  acknowledged: '✓',
  expired: '⌛',
}

export function AlertRow({ alert, busy, duplicateCount = 1, onAcknowledge }: AlertRowProps) {
  const status = (alert.status ?? 'PENDING').toLowerCase()
  const statusLabel = formatStatusLabel(alert.status ?? 'PENDING')
  const icon = STATUS_ICON[status] ?? '•'
  const metrics = [
    alert.conditionTemperatureC != null
      ? formatMetricLabel('Temp', formatTemperature(alert.conditionTemperatureC, 'F'))
      : null,
    alert.conditionHumidity != null ? formatMetricLabel('Humidity', formatPercent(alert.conditionHumidity)) : null,
    alert.conditionDewPointC != null
      ? formatMetricLabel('Dew point', formatTemperature(alert.conditionDewPointC, 'F'))
      : null,
    alert.conditionWindGust != null ? formatMetricLabel('Gust', formatWind(alert.conditionWindGust)) : null,
    alert.conditionSkyCover != null ? formatMetricLabel('Sky', formatPercent(alert.conditionSkyCover)) : null,
    alert.conditionPrecipitationProbability != null
      ? formatMetricLabel('Rain', formatPercent(alert.conditionPrecipitationProbability))
      : null,
    alert.conditionPrecipitationAmount != null
      ? formatMetricLabel('Accum.', formatMillimeters(alert.conditionPrecipitationAmount))
      : null,
    alert.conditionRiverGaugeId ? formatMetricLabel('Gauge', alert.conditionRiverGaugeId) : null,
    alert.conditionRiverObservedStage != null
      ? formatMetricLabel('Observed', formatRiverStage(alert.conditionRiverObservedStage, alert.conditionRiverStageUnit))
      : null,
    alert.conditionRiverForecastStage != null
      ? formatMetricLabel('Forecast', formatRiverStage(alert.conditionRiverForecastStage, alert.conditionRiverStageUnit))
      : null,
    alert.conditionRiverFloodStage != null
      ? formatMetricLabel('Flood stage', formatRiverStage(alert.conditionRiverFloodStage, alert.conditionRiverStageUnit))
      : null,
    alert.conditionRiverObservedCategory
      ? formatMetricLabel('Current flood', formatRiverCategory(alert.conditionRiverObservedCategory))
      : null,
    alert.conditionRiverForecastCategory
      ? formatMetricLabel('Forecast flood', formatRiverCategory(alert.conditionRiverForecastCategory))
      : null,
  ].filter((value): value is string => Boolean(value))

  return (
    <article className="alert-row">
      <div>
        <p className="alert-row-title">
          {alert.headline ?? 'Triggered alert'}
          {duplicateCount > 1 ? <span className="badge duplicate-badge">x{duplicateCount}</span> : null}
        </p>
        <p className="muted small">{alert.reason ?? 'Rule matched'}</p>
        <p className="muted small" title={formatDate(alert.alertTime)}>
          {formatRelativeTime(alert.alertTime)}
        </p>
        {metrics.length > 0 ? (
          <div className="alert-metrics">
            {metrics.map((metric) => (
              <span key={metric} className="metric-pill">
                {metric}
              </span>
            ))}
          </div>
        ) : null}
      </div>
      <div className="alert-row-actions">
        <span className={`delivery-indicator status-${status}`} title={statusLabel}>
          {icon}
        </span>
        {alert.status === 'SENT' ? (
          <AriaButton className="primary" isDisabled={busy} onPress={() => onAcknowledge(alert.id)}>
            {busy ? 'Saving...' : 'Acknowledge'}
          </AriaButton>
        ) : null}
      </div>
    </article>
  )
}

function formatRiverStage(stage?: number | null, unit?: string | null): string {
  if (stage === undefined || stage === null || Number.isNaN(stage)) {
    return '--'
  }

  return `${formatNumber(stage)} ${unit ?? 'ft'}`
}

function formatRiverCategory(category?: string | null): string {
  if (!category) {
    return 'Unknown'
  }

  const normalized = category.replace(/_/g, ' ').trim()
  if (normalized.length === 0) {
    return 'Unknown'
  }

  return normalized.charAt(0).toUpperCase() + normalized.slice(1)
}
