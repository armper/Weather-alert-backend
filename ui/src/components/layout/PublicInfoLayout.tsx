import type { ReactNode } from 'react'
import { PublicPosterLayout } from './PublicPosterLayout'

interface PublicInfoLayoutProps {
  eyebrow: string
  title: string
  summary: string
  children: ReactNode
}

export function PublicInfoLayout({ eyebrow, title, summary, children }: PublicInfoLayoutProps) {
  return (
    <PublicPosterLayout eyebrow={eyebrow} title={title} summary={summary} width="wide">
      <div className="public-info-content">{children}</div>
    </PublicPosterLayout>
  )
}
