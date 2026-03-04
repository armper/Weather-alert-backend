import { Outlet } from 'react-router-dom'
import { useAppState } from '../../state/useAppState'
import { BackgroundArtwork } from '../common/BackgroundArtwork'
import { NoticeBanner } from '../common/NoticeBanner'
import { AppShell } from './AppShell'

export function AuthenticatedLayout() {
  const { notice } = useAppState()

  return (
    <>
      <BackgroundArtwork />
      <AppShell>
        <Outlet />
      </AppShell>

      {notice ? <NoticeBanner notice={notice} /> : null}
    </>
  )
}
