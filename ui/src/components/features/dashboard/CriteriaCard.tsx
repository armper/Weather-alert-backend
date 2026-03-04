import type { AlertCriteria } from '../../../types'
import { describeCriteria } from '../../../lib/criteria'

interface CriteriaCardProps {
  criteria: AlertCriteria
  busy: boolean
  onDelete: (criteriaId: string) => void
  onToggleEnabled: (criteriaId: string, enabled: boolean) => void
}

export function CriteriaCard({ criteria, busy, onDelete, onToggleEnabled }: CriteriaCardProps) {
  const enabled = criteria.enabled !== false
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
          <label className="switch-field compact">
            <span>{enabled ? 'On' : 'Off'}</span>
            <span className="switch">
              <input
                type="checkbox"
                checked={enabled}
                disabled={busy}
                onChange={(event) => onToggleEnabled(criteria.id, event.target.checked)}
              />
              <span className="switch-slider" />
            </span>
          </label>
          <button
            className="ghost icon-button"
            title="Delete rule"
            aria-label="Delete rule"
            disabled={busy}
            onClick={() => onDelete(criteria.id)}
          >
            {busy ? '…' : '🗑'}
          </button>
        </div>
      </footer>
    </article>
  )
}
