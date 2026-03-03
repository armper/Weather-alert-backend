import { Navigate } from 'react-router-dom'
import { useAppState } from '../state/useAppState'
import { AdminUserRow } from '../components/features/dashboard/AdminUserRow'

export function AdminPage() {
  const { isAdmin, pendingUsers, adminUsers, busyApprovalId, busyAdminAction, handleApproveUser, handleAdminAction } =
    useAppState()

  if (!isAdmin) {
    return <Navigate to="/app/overview" replace />
  }

  return (
    <section className="page-stack">
      <article className="panel">
        <div className="panel-title-row">
          <h2>Pending Approvals</h2>
          <span className="badge">{pendingUsers.length}</span>
        </div>
        {pendingUsers.length === 0 ? (
          <p className="muted">No pending users right now.</p>
        ) : (
          <div className="alert-list">
            {pendingUsers.map((user) => (
              <article key={user.id} className="alert-row">
                <div>
                  <p className="alert-row-title">{user.id}</p>
                  <p className="muted small">{user.email}</p>
                </div>
                <button
                  className="primary"
                  disabled={busyApprovalId === user.id}
                  onClick={() => void handleApproveUser(user.id)}
                >
                  {busyApprovalId === user.id ? 'Approving...' : 'Approve'}
                </button>
              </article>
            ))}
          </div>
        )}
      </article>

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
                busyApproval={busyApprovalId === user.id}
                busySuspend={busyAdminAction === `suspend:${user.id}`}
                busyReactivate={busyAdminAction === `reactivate:${user.id}`}
                busyForceReset={busyAdminAction === `force-password-reset:${user.id}`}
                onApprove={(userId) => void handleApproveUser(userId)}
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
