import type { AlertCriteria } from '../../../types'
import { describeCriteria } from '../../../lib/criteria'

interface CriteriaCardProps {
  criteria: AlertCriteria
  busy: boolean
  onDelete: (criteriaId: string) => void
}

export function CriteriaCard({ criteria, busy, onDelete }: CriteriaCardProps) {
  return (
    <article className="criteria-card">
      <header>
        <h3>{criteria.name ?? 'Custom alert'}</h3>
        <span className="chip-role">{criteria.enabled === false ? 'Disabled' : 'Enabled'}</span>
      </header>
      <p>{describeCriteria(criteria)}</p>
      <footer>
        <span className="muted small">{criteria.location ?? 'No location'}</span>
        <button className="ghost danger" disabled={busy} onClick={() => onDelete(criteria.id)}>
          {busy ? 'Deleting...' : 'Delete'}
        </button>
      </footer>
    </article>
  )
}
