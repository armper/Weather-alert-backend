import { Navigate } from 'react-router-dom'
import { AdminUserRow } from '../components/features/dashboard/AdminUserRow'
import { LoadingPlaceholder } from '../components/common/LoadingPlaceholder'
import { formatFriendlyLocation, formatRelativeTime } from '../lib/formatting'
import { useAppState } from '../state/useAppState'

const GCP_PROJECT_ID = 'weather-alerts-panda'
const GCP_REGION = 'us-east1'
const BACKEND_SERVICE = 'weather-alert-backend'
const UI_SERVICE = 'weather-alert-ui'

const ADMIN_JOBS = [
  {
    key: 'weather-processing',
    title: 'Run weather processing',
    description: 'Pull current NOAA data, evaluate rules, and publish any newly matched alert events.',
  },
  {
    key: 'alert-delivery-retries',
    title: 'Run delivery retries',
    description: 'Publish due notification retries for email and SMS delivery failures.',
  },
  {
    key: 'data-retention',
    title: 'Run data retention',
    description: 'Clean old alert events, weather snapshots, and orphaned monitoring state.',
  },
] as const

function cloudRunServiceLink(serviceName: string, tab: 'logs' | 'metrics' = 'logs') {
  return `https://console.cloud.google.com/run/detail/${GCP_REGION}/${serviceName}/${tab}?project=${GCP_PROJECT_ID}`
}

function logsExplorerLink(serviceName: string) {
  const query = `resource.type="cloud_run_revision"\nresource.labels.service_name="${serviceName}"`
  return `https://console.cloud.google.com/logs/query;query=${encodeURIComponent(query)}?project=${GCP_PROJECT_ID}`
}

const CLOUD_CONSOLE_LINKS = [
  {
    title: 'Backend logs',
    description: 'Cloud Run revision logs for alert processing, email, SMS, and auth events.',
    href: logsExplorerLink(BACKEND_SERVICE),
  },
  {
    title: 'UI logs',
    description: 'Cloud Run logs for the public site, proxy traffic, and frontend delivery issues.',
    href: logsExplorerLink(UI_SERVICE),
  },
  {
    title: 'Backend metrics',
    description: 'CPU, memory, request latency, and error-rate metrics for the backend service.',
    href: cloudRunServiceLink(BACKEND_SERVICE, 'metrics'),
  },
  {
    title: 'UI metrics',
    description: 'Traffic, cold starts, and runtime metrics for the frontend service.',
    href: cloudRunServiceLink(UI_SERVICE, 'metrics'),
  },
  {
    title: 'Build history',
    description: 'Cloud Build history for deploys from GitHub and manual release runs.',
    href: `https://console.cloud.google.com/cloud-build/builds?project=${GCP_PROJECT_ID}`,
  },
  {
    title: 'Monitoring overview',
    description: 'Cloud Monitoring dashboards and alerting configuration for the GCP project.',
    href: `https://console.cloud.google.com/monitoring?project=${GCP_PROJECT_ID}`,
  },
  {
    title: 'Error reporting',
    description: 'Application exceptions captured across Cloud Run revisions.',
    href: `https://console.cloud.google.com/errors?project=${GCP_PROJECT_ID}`,
  },
] as const

export function AdminPage() {
  const {
    isAdmin,
    me,
    criteria,
    alerts,
    currentWeather,
    adminUsers,
    initialDataLoading,
    busyAdminAction,
    busyAdminJob,
    adminJobResults,
    handleAdminAction,
    handleAdminJobRun,
  } = useAppState()

  if (!isAdmin) {
    return <Navigate to="/app/overview" replace />
  }

  const suspendedUsers = adminUsers.filter((user) => user.approvalStatus === 'SUSPENDED').length
  const forcedResetUsers = adminUsers.filter((user) => user.passwordResetRequired).length
  const activeRules = criteria.filter((rule) => rule.enabled !== false).length
  const activeAlerts = alerts.filter((alert) => alert.status === 'SENT').length
  const watchLocation = formatFriendlyLocation(criteria[0]?.location?.trim())
  const freshnessLabel = currentWeather?.timestamp
    ? `Forecast updated ${formatRelativeTime(currentWeather.timestamp)}`
    : 'Forecast freshness unavailable'

  return (
    <section className="page-stack">
      <article className="panel admin-control-panel">
        <div className="panel-title-row">
          <div>
            <p className="eyebrow">Admin</p>
            <h2>SkyPanda control panel</h2>
          </div>
          <span className="badge">Admin only</span>
        </div>
        <p className="muted admin-panel-intro">
          This page is available only to administrator accounts and is reachable on the deployed site at
          {' '}
          <strong>/app/admin</strong>.
        </p>

        <div className="admin-summary-grid">
          <article className="admin-summary-card">
            <span className="admin-summary-label">Accounts</span>
            <strong>{initialDataLoading ? '...' : adminUsers.length}</strong>
            <span className="muted">Total users in the system</span>
          </article>
          <article className="admin-summary-card">
            <span className="admin-summary-label">Suspended</span>
            <strong>{initialDataLoading ? '...' : suspendedUsers}</strong>
            <span className="muted">Accounts currently blocked from access</span>
          </article>
          <article className="admin-summary-card">
            <span className="admin-summary-label">Password resets</span>
            <strong>{initialDataLoading ? '...' : forcedResetUsers}</strong>
            <span className="muted">Users forced to set a new password next sign-in</span>
          </article>
          <article className="admin-summary-card">
            <span className="admin-summary-label">Live monitoring</span>
            <strong>{initialDataLoading ? 'Loading monitoring…' : activeAlerts > 0 ? `${activeAlerts} active alerts` : `${activeRules} active rules`}</strong>
            <span className="muted">
              {watchLocation === 'your watch area' ? freshnessLabel : `Watching ${watchLocation}. ${freshnessLabel}`}
            </span>
          </article>
        </div>
      </article>

      <article className="panel">
        <div className="panel-title-row">
          <div>
            <p className="eyebrow">Operations</p>
            <h2>Operational actions</h2>
          </div>
          <span className="badge">Run with care</span>
        </div>
        <div className="admin-job-grid">
          {ADMIN_JOBS.map((job) => {
            const result = adminJobResults[job.key]
            const isBusy = busyAdminJob === job.key
            return (
              <article key={job.key} className="admin-job-card">
                <div className="stack-sm">
                  <strong>{job.title}</strong>
                  <p className="muted">{job.description}</p>
                </div>
                <button className="ghost" disabled={isBusy} onClick={() => void handleAdminJobRun(job.key)}>
                  {isBusy ? 'Running…' : 'Run now'}
                </button>
                {result ? (
                  <div className="admin-job-result">
                    <span className="badge">{result.status || 'completed'}</span>
                    <p className="muted small">{result.message || `${result.jobName || job.title} completed.`}</p>
                    {result.finishedAt ? (
                      <p className="muted small">Finished {formatRelativeTime(result.finishedAt)}</p>
                    ) : null}
                    {result.durationMillis ? (
                      <p className="muted small">Duration {Math.max(1, Math.round(result.durationMillis / 1000))}s</p>
                    ) : null}
                    {result.metrics && Object.keys(result.metrics).length > 0 ? (
                      <div className="admin-job-metrics">
                        {Object.entries(result.metrics).map(([metric, value]) => (
                          <span key={metric} className="badge is-muted">
                            {metric}: {value}
                          </span>
                        ))}
                      </div>
                    ) : null}
                  </div>
                ) : null}
              </article>
            )
          })}
        </div>
      </article>

      <article className="panel">
        <div className="panel-title-row">
          <div>
            <p className="eyebrow">Cloud</p>
            <h2>Logs and analytics</h2>
          </div>
          <span className="badge">weather-alerts-panda</span>
        </div>
        <div className="admin-console-link-grid">
          {CLOUD_CONSOLE_LINKS.map((item) => (
            <a
              key={item.title}
              className="admin-console-link"
              href={item.href}
              target="_blank"
              rel="noreferrer"
            >
              <strong>{item.title}</strong>
              <span>{item.description}</span>
            </a>
          ))}
        </div>
      </article>

      <article className="panel">
        <div className="panel-title-row">
          <div>
            <p className="eyebrow">Accounts</p>
            <h2>Account admin</h2>
          </div>
          <span className="badge">{initialDataLoading ? 'Loading…' : `${adminUsers.length} users`}</span>
        </div>
        <p className="muted admin-panel-intro">
          Use these controls to suspend abusive accounts, reactivate access, or force a password reset without leaving
          the deployed admin panel.
        </p>
        {initialDataLoading ? (
          <LoadingPlaceholder
            title="Loading account directory"
            copy="Fetching the admin user list and current account states."
            lineCount={5}
          />
        ) : adminUsers.length === 0 ? (
          <p className="muted">No users found.</p>
        ) : (
          <div className="alert-list">
            {adminUsers.map((user) => (
              <AdminUserRow
                key={user.id}
                user={user}
                busySuspend={busyAdminAction === `suspend:${user.id}`}
                busyReactivate={busyAdminAction === `reactivate:${user.id}`}
                busyForceReset={busyAdminAction === `force-password-reset:${user.id}`}
                onSuspend={(userId) => void handleAdminAction(userId, 'suspend')}
                onReactivate={(userId) => void handleAdminAction(userId, 'reactivate')}
                onForceReset={(userId) => void handleAdminAction(userId, 'force-password-reset')}
              />
            ))}
          </div>
        )}
        <p className="muted small">Signed in as {me?.id ?? 'admin'}.</p>
      </article>
    </section>
  )
}
