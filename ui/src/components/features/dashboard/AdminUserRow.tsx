import type { UserAccount } from '../../../types'

interface AdminUserRowProps {
  user: UserAccount
  busyApproval: boolean
  busySuspend: boolean
  busyReactivate: boolean
  busyForceReset: boolean
  onApprove: (userId: string) => void
  onSuspend: (userId: string) => void
  onReactivate: (userId: string) => void
  onForceReset: (userId: string) => void
}

export function AdminUserRow({
  user,
  busyApproval,
  busySuspend,
  busyReactivate,
  busyForceReset,
  onApprove,
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
        {user.approvalStatus === 'PENDING_APPROVAL' ? (
          <button className="primary" disabled={busyApproval} onClick={() => onApprove(user.id)}>
            {busyApproval ? 'Approving...' : 'Approve'}
          </button>
        ) : null}
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
