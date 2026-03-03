import type { NoticeState } from '../../state/types'

export function NoticeBanner({ notice }: { notice: NoticeState }) {
  return (
    <div className={`notice notice-${notice.kind}`} role="status">
      {notice.text}
    </div>
  )
}
