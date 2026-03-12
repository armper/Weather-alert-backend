import type { RuleHeartbeatSummary } from '../../../lib/ruleDashboard'

interface RuleHeartbeatStripProps {
  summary: RuleHeartbeatSummary
}

export function RuleHeartbeatStrip({ summary }: RuleHeartbeatStripProps) {
  return (
    <section className={`rule-heartbeat-strip${summary.allClear ? ' is-clear' : ' is-alerting'}`}>
      <div className="rule-heartbeat-primary">
        <strong>Monitoring {summary.activeRules} rules</strong>
        <span>{summary.monitoredLocationsLabel}</span>
      </div>
      <div className="rule-heartbeat-secondary">
        <span>{summary.freshnessLabel}</span>
        <span className={`badge ${summary.allClear ? '' : 'is-live'}`}>{summary.systemStateLabel}</span>
      </div>
    </section>
  )
}
