import type { UserAccount } from '../../../types'

interface AdminUserRowProps {
  user: UserAccount
  busySuspend: boolean
  busyReactivate: boolean
  busyForceReset: boolean
  onSuspend: (userId: string) => void
  onReactivate: (userId: string) => void
  onForceReset: (userId: string) => void
}

export function AdminUserRow({
  user,
  busySuspend,
  busyReactivate,
  busyForceReset,
  onSuspend,
  onReactivate,
  onForceReset,
}: AdminUserRowProps) {
  return (
    <article className="alert-row">
      <div>
        <p className="alert-row-title">{user.id}</p>
        <p className="muted small">{user.email}</p>
        <p className="muted small">
          {user.approvalStatus}
          {user.passwordResetRequired ? ' | Password reset required' : ''}
        </p>
      </div>
      <div className="alert-row-actions">
        {user.approvalStatus !== 'SUSPENDED' ? (
          <button className="ghost danger" disabled={busySuspend} onClick={() => onSuspend(user.id)}>
            {busySuspend ? 'Working...' : 'Suspend'}
          </button>
        ) : (
          <button className="ghost" disabled={busyReactivate} onClick={() => onReactivate(user.id)}>
            {busyReactivate ? 'Working...' : 'Reactivate'}
          </button>
        )}
        <button className="ghost" disabled={busyForceReset} onClick={() => onForceReset(user.id)}>
          {busyForceReset ? 'Working...' : 'Force reset'}
        </button>
      </div>
    </article>
  )
}
