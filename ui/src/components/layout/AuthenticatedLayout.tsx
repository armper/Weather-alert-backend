import { Outlet } from 'react-router-dom'
import { BackgroundArtwork } from '../common/BackgroundArtwork'
import { AppShell } from './AppShell'

export function AuthenticatedLayout() {
  return (
    <>
      <BackgroundArtwork />
      <AppShell>
        <Outlet />
      </AppShell>
    </>
  )
}
