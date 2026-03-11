import { Navigate } from 'react-router-dom'
import { useAppState } from '../state/useAppState'
import { AdminUserRow } from '../components/features/dashboard/AdminUserRow'

export function AdminPage() {
  const { isAdmin, adminUsers, busyAdminAction, handleAdminAction } = useAppState()

  if (!isAdmin) {
    return <Navigate to="/app/overview" replace />
  }

  return (
    <section className="page-stack">
      <article className="panel">
        <div className="panel-title-row">
          <h2>Account Admin</h2>
          <span className="badge">{adminUsers.length} users</span>
        </div>
        {adminUsers.length === 0 ? (
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
      </article>
    </section>
  )
}
