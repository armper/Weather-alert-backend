import { Suspense, lazy, useMemo, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { CriteriaCard } from '../components/features/dashboard/CriteriaCard'
import { RuleHeartbeatStrip } from '../components/features/dashboard/RuleHeartbeatStrip'
import { LoadingPlaceholder } from '../components/common/LoadingPlaceholder'
import { AriaSelect } from '../components/ui/AriaSelect'
import {
  buildRuleDashboardSummary,
  compareRuleViewModel,
  type RuleSortMode,
  type RuleViewModel,
} from '../lib/ruleDashboard'
import { useActionState, useAsyncState, useDataState, useSessionState } from '../state/useAppState'

const MonitoringRulesMap = lazy(() =>
  import('../components/maps/MonitoringRulesMap').then((module) => ({ default: module.MonitoringRulesMap })),
)

const SORT_OPTIONS = [
  { id: 'attention', label: 'Attention first' },
  { id: 'recentlyTriggered', label: 'Recently triggered' },
  { id: 'alphabetical', label: 'Alphabetical' },
  { id: 'location', label: 'Location' },
] as const

export function ManageAlertsPage() {
  const { initialDataLoading } = useSessionState()
  const { busyCriteriaId } = useAsyncState()
  const { handleDeleteCriteria, handleToggleCriteriaEnabled } = useActionState()
  const { alerts, criteria, currentWeather } = useDataState()
  const [sortMode, setSortMode] = useState<RuleSortMode>('attention')
  const [pinnedGroupId, setPinnedGroupId] = useState<string | null>(null)
  const [hoveredGroupId, setHoveredGroupId] = useState<string | null>(null)
  const cardRefs = useRef(new Map<string, HTMLElement>())

  const summary = useMemo(
    () => buildRuleDashboardSummary(criteria, alerts, currentWeather),
    [criteria, alerts, currentWeather],
  )

  const activeFocusedGroupId =
    hoveredGroupId ??
    pinnedGroupId ??
    summary.locationGroups.find((item) => item.statusTone === 'critical')?.id ??
    summary.locationGroups[0]?.id ??
    null

  const activeFocusedRuleIds = useMemo(
    () => new Set(summary.locationGroups.find((item) => item.id === activeFocusedGroupId)?.rules.map((item) => item.criteriaId) ?? []),
    [activeFocusedGroupId, summary.locationGroups],
  )

  const sortedActiveRules = useMemo(
    () => [...summary.activeRules].sort((left, right) => compareRuleViewModel(left, right, sortMode)),
    [summary.activeRules, sortMode],
  )

  const sortedPausedRules = useMemo(
    () => [...summary.pausedRules].sort((left, right) => compareRuleViewModel(left, right, sortMode)),
    [summary.pausedRules, sortMode],
  )

  const focusedGroup = useMemo(
    () => summary.locationGroups.find((item) => item.id === activeFocusedGroupId) ?? null,
    [activeFocusedGroupId, summary.locationGroups],
  )

  function registerCardRef(criteriaId: string, element: HTMLElement | null) {
    if (element) {
      cardRefs.current.set(criteriaId, element)
      return
    }
    cardRefs.current.delete(criteriaId)
  }

  function focusGroup(groupId: string | null, shouldScroll = false) {
    if (!groupId) {
      setPinnedGroupId(null)
      return
    }
    setPinnedGroupId(groupId)
    if (!shouldScroll) {
      return
    }
    const firstCriteriaId = summary.locationGroups.find((item) => item.id === groupId)?.rules[0]?.criteriaId
    const target = firstCriteriaId ? cardRefs.current.get(firstCriteriaId) : undefined
    target?.scrollIntoView({ behavior: 'smooth', block: 'center' })
  }

  function renderCriteriaCard(item: RuleViewModel) {
    return (
      <CriteriaCard
        key={item.criteria.id}
        criteria={item.criteria}
        busy={busyCriteriaId === item.criteria.id}
        triggerCondition={item.triggerCondition}
        locationLabel={item.locationLabel}
        monitoringState={item.statusLabel}
        monitoringTone={item.statusTone}
        monitoringDetail={item.statusDetail}
        historyLabel={item.lastAlertLabel}
        emphasis={item.emphasis}
        selected={item.locationGroupId != null && activeFocusedRuleIds.has(item.criteria.id)}
        onDelete={(criteriaId) => void handleDeleteCriteria(criteriaId)}
        onToggleEnabled={(criteriaId, enabled) => void handleToggleCriteriaEnabled(criteriaId, enabled)}
        onPointerEnter={() => setHoveredGroupId(item.locationGroupId ?? null)}
        onPointerLeave={() => setHoveredGroupId(null)}
        onSelect={() => focusGroup(item.locationGroupId ?? null)}
        cardRef={(element) => registerCardRef(item.criteria.id, element)}
      />
    )
  }

  return (
    <section className="page-stack">
      <article className="panel">
        <div className="panel-title-row">
          <h2>Monitoring Rules</h2>
          <span className="badge">{initialDataLoading ? 'Loading…' : `${criteria.length} total`}</span>
        </div>

        {initialDataLoading ? (
          <LoadingPlaceholder
            title="Loading monitoring rules"
            copy="Fetching your saved watches, map groups, and latest rule heartbeat."
            lineCount={4}
          />
        ) : (
          <RuleHeartbeatStrip summary={summary.heartbeat} />
        )}

        {!initialDataLoading && summary.locationGroups.length > 0 ? (
          <section className="rule-monitoring-map-panel">
            <div className="panel-title-row rule-monitoring-map-header">
              <div>
                <p className="eyebrow">Monitoring Map</p>
                <h3>Watched places</h3>
              </div>
              <span className={`badge ${summary.heartbeat.allClear ? '' : 'is-live'}`}>
                {summary.locationGroups.length} location{summary.locationGroups.length === 1 ? '' : 's'}
              </span>
            </div>
            <p className="muted small rule-monitoring-map-copy">
              Click a rule or marker to jump between the list and the places SkyPanda is actively watching.
            </p>
            <div className="rule-monitoring-layout">
              <Suspense
                fallback={
                  <div className="rule-monitoring-map static-map-shell">
                    <LoadingPlaceholder title="Loading map" copy="Preparing watched places." compact />
                  </div>
                }
              >
                <MonitoringRulesMap
                  groups={summary.locationGroups}
                  selectedGroupId={activeFocusedGroupId}
                  onMarkerSelect={(groupId) => focusGroup(groupId, true)}
                  onMarkerHover={setHoveredGroupId}
                  className="rule-monitoring-map"
                />
              </Suspense>
              <div className="rule-monitoring-context">
                <div>
                  <p className="eyebrow">Selected Watch Area</p>
                  <h3>{focusedGroup?.locationLabel ?? 'Monitoring overview'}</h3>
                  <p className="muted small">
                    {focusedGroup
                      ? `${focusedGroup.statusLabel}. ${focusedGroup.ruleCount} rule${focusedGroup.ruleCount === 1 ? '' : 's'} linked here.`
                      : 'Select a marker to inspect the rules protecting that location.'}
                  </p>
                </div>
                {focusedGroup ? (
                  <div className="monitoring-watch-chips rule-monitoring-context-chips">
                    {focusedGroup.rules.map((rule) => (
                      <button
                        key={rule.criteriaId}
                        type="button"
                        className={`metric-pill rule-monitoring-context-chip is-${rule.monitoringTone}`}
                        onClick={() => {
                          setPinnedGroupId(focusedGroup.id)
                          cardRefs.current.get(rule.criteriaId)?.scrollIntoView({ behavior: 'smooth', block: 'center' })
                        }}
                      >
                        <span aria-hidden>{rule.icon}</span>
                        <span>{rule.ruleName}</span>
                      </button>
                    ))}
                  </div>
                ) : null}
              </div>
            </div>
          </section>
        ) : null}

        <div className="manage-alerts-toolbar">
          <div className="manage-alerts-badges">
            <span className="badge">{initialDataLoading ? 'Loading…' : `${summary.heartbeat.activeRules} monitoring`}</span>
            <span className="badge">{initialDataLoading ? 'Loading…' : `${summary.heartbeat.pausedRules} paused`}</span>
            <span className="badge">{initialDataLoading ? 'Loading…' : `${summary.heartbeat.recentRules} recently alerted`}</span>
          </div>
          {!initialDataLoading && criteria.length > 1 ? (
            <AriaSelect
              label="Sort rules"
              buttonClassName="aria-select-trigger"
              popoverClassName="aria-select-popover"
              listBoxClassName="aria-select-listbox"
              selectedKey={sortMode}
              options={SORT_OPTIONS.map((option) => ({ ...option }))}
              onSelectionChange={(value) => setSortMode(value as RuleSortMode)}
            />
          ) : null}
        </div>

        {initialDataLoading ? (
          <LoadingPlaceholder
            title="Loading rule cards"
            copy="SkyPanda is matching each saved rule with the latest live weather state."
            lineCount={5}
          />
        ) : criteria.length === 0 ? (
          <div className="empty-state-panel">
            <h3>No monitoring rules yet</h3>
            <p className="muted">
              Create your first rule and it will show up here with live monitoring status, location context, and quick controls.
            </p>
            <div className="button-row empty-state-actions">
              <Link to="/app/rules#create-custom-alert" className="primary overview-action-link">
                New alert
              </Link>
            </div>
          </div>
        ) : (
          <div className="stack">
            <section className="criteria-section">
              <div className="criteria-section-header">
                <div>
                  <h3>Monitoring now</h3>
                  <p className="muted small">These rules are actively watching your saved locations.</p>
                </div>
                <span className={`badge ${summary.heartbeat.allClear ? '' : 'is-live'}`}>
                  {summary.heartbeat.allClear ? 'All clear' : `${summary.heartbeat.triggeredRules} triggered`}
                </span>
              </div>
              <div className="criteria-grid">
                {sortedActiveRules.map((item) => renderCriteriaCard(item))}
              </div>
            </section>

            {sortedPausedRules.length > 0 ? (
              <section className="criteria-section">
                <div className="criteria-section-header">
                  <div>
                    <h3>Paused rules</h3>
                    <p className="muted small">Saved rules that are not currently monitoring.</p>
                  </div>
                  <span className="badge is-muted">{sortedPausedRules.length}</span>
                </div>
                <div className="criteria-grid">
                  {sortedPausedRules.map((item) => renderCriteriaCard(item))}
                </div>
              </section>
            ) : null}
          </div>
        )}
      </article>
    </section>
  )
}
