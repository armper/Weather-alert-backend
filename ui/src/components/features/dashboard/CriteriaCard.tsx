import type { AlertCriteria } from '../../../types'
import { describeCriteria } from '../../../lib/criteria'
import { AriaButton } from '../../ui/AriaButton'
import { AriaSwitch } from '../../ui/AriaSwitch'

interface CriteriaCardProps {
  criteria: AlertCriteria
  busy: boolean
  lastTriggeredLabel?: string
  onDelete: (criteriaId: string) => void
  onToggleEnabled: (criteriaId: string, enabled: boolean) => void
  severityLabel?: string
  severityTone?: 'calm' | 'critical' | 'muted' | 'warning'
}

export function CriteriaCard({
  criteria,
  busy,
  lastTriggeredLabel,
  onDelete,
  onToggleEnabled,
  severityLabel,
  severityTone = 'muted',
}: CriteriaCardProps) {
  const enabled = criteria.enabled !== false
  const ruleName = criteria.name ?? 'custom alert'
  return (
    <article className="criteria-card">
      <header>
        <div>
          <h3 className="criteria-name">{criteria.name ?? 'Custom alert'}</h3>
          <p className="criteria-rule-line">{describeCriteria(criteria)}</p>
        </div>
        <div className="criteria-card-statuses">
          <span className={`badge${enabled ? '' : ' is-paused'}`}>{enabled ? 'Active' : 'Paused'}</span>
          {severityLabel ? (
            <span className={`criteria-signal-chip is-${severityTone}`}>{severityLabel}</span>
          ) : null}
        </div>
      </header>
      <div className="criteria-card-meta">
        <span className="muted small">{criteria.location ?? 'No location'}</span>
        <span className="criteria-meta-note">
          {lastTriggeredLabel ? `Last triggered ${lastTriggeredLabel}` : 'No triggered events yet'}
        </span>
      </div>
      <footer>
        <div className="criteria-card-actions">
          <AriaSwitch
            compact
            label={enabled ? 'On' : 'Off'}
            isSelected={enabled}
            isDisabled={busy}
            onChange={(value) => onToggleEnabled(criteria.id, value)}
          />
          <AriaButton
            className="ghost icon-button"
            aria-label={`Delete ${ruleName}`}
            isDisabled={busy}
            onPress={() => onDelete(criteria.id)}
          >
            {busy ? '…' : '🗑'}
          </AriaButton>
        </div>
      </footer>
    </article>
  )
}
