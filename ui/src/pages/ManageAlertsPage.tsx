import { useAppState } from '../state/useAppState'
import { CriteriaCard } from '../components/features/dashboard/CriteriaCard'

export function ManageAlertsPage() {
  const { criteria, busyCriteriaId, handleDeleteCriteria, handleToggleCriteriaEnabled } = useAppState()
  const activeCount = criteria.filter((item) => item.enabled !== false).length

  return (
    <section className="page-stack">
      <article className="panel">
        <div className="panel-title-row">
          <h2>Manage Alerts</h2>
          <span className="badge">{activeCount} active</span>
        </div>
        {criteria.length === 0 ? (
          <p className="muted">No alerts yet. Go to Create Alert to add your first rule.</p>
        ) : (
          <div className="criteria-grid">
            {criteria.map((item) => (
              <CriteriaCard
                key={item.id}
                criteria={item}
                busy={busyCriteriaId === item.id}
                onDelete={(criteriaId) => void handleDeleteCriteria(criteriaId)}
                onToggleEnabled={(criteriaId, enabled) => void handleToggleCriteriaEnabled(criteriaId, enabled)}
              />
            ))}
          </div>
        )}
      </article>
    </section>
  )
}
