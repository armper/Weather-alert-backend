import type { AlertCriteria } from '../../../types'
import { describeCriteria } from '../../../lib/criteria'
import { AriaButton } from '../../ui/AriaButton'
import { AriaSwitch } from '../../ui/AriaSwitch'

interface CriteriaCardProps {
  criteria: AlertCriteria
  busy: boolean
  onDelete: (criteriaId: string) => void
  onToggleEnabled: (criteriaId: string, enabled: boolean) => void
}

export function CriteriaCard({ criteria, busy, onDelete, onToggleEnabled }: CriteriaCardProps) {
  const enabled = criteria.enabled !== false
  const ruleName = criteria.name ?? 'custom alert'
  return (
    <article className="criteria-card">
      <header>
        <h3 className="criteria-name">{criteria.name ?? 'Custom alert'}</h3>
        <span className="badge">{enabled ? 'Active' : 'Paused'}</span>
      </header>
      <p className="criteria-rule-line">{describeCriteria(criteria)}</p>
      <footer>
        <span className="muted small">{criteria.location ?? 'No location'}</span>
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
