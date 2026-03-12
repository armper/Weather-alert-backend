import type { AlertCriteria } from '../../../types'
import { AriaButton } from '../../ui/AriaButton'
import { AriaSwitch } from '../../ui/AriaSwitch'
import type { RuleEmphasis, RuleMonitoringState, RuleTone } from '../../../lib/ruleDashboard'

interface CriteriaCardProps {
  criteria: AlertCriteria
  busy: boolean
  onDelete: (criteriaId: string) => void
  onToggleEnabled: (criteriaId: string, enabled: boolean) => void
  triggerCondition: string
  locationLabel: string
  monitoringState: RuleMonitoringState
  monitoringTone: RuleTone
  monitoringDetail: string
  historyLabel: string
  emphasis: RuleEmphasis
  selected?: boolean
  onPointerEnter?: () => void
  onPointerLeave?: () => void
  onSelect?: () => void
  cardRef?: (element: HTMLElement | null) => void
}

export function CriteriaCard({
  criteria,
  busy,
  onDelete,
  onToggleEnabled,
  triggerCondition,
  locationLabel,
  monitoringState,
  monitoringTone,
  monitoringDetail,
  historyLabel,
  emphasis,
  selected = false,
  onPointerEnter,
  onPointerLeave,
  onSelect,
  cardRef,
}: CriteriaCardProps) {
  const enabled = criteria.enabled !== false
  const ruleName = criteria.name ?? 'custom alert'
  return (
    <article
      ref={cardRef}
      className={`criteria-card is-${emphasis}${selected ? ' is-selected' : ''}`}
      onMouseEnter={onPointerEnter}
      onMouseLeave={onPointerLeave}
      onFocus={onPointerEnter}
      onBlur={onPointerLeave}
      onClick={onSelect}
    >
      <header>
        <div>
          <h3 className="criteria-name">{criteria.name ?? 'Custom alert'}</h3>
          <p className="criteria-rule-line">{triggerCondition}</p>
        </div>
        <div className="criteria-card-statuses">
          <span className={`badge${enabled ? '' : ' is-paused'}`}>{enabled ? 'Active' : 'Paused'}</span>
          <span className={`criteria-signal-chip is-${monitoringTone}`}>{monitoringState}</span>
        </div>
      </header>
      <div className="criteria-card-meta">
        <span className="criteria-location-line">{locationLabel}</span>
        <span className="criteria-meta-note">{monitoringDetail}</span>
        <span className="criteria-meta-note">{historyLabel}</span>
      </div>
      <footer onClick={(event) => event.stopPropagation()}>
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
