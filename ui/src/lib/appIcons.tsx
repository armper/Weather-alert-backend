import { createElement, type ReactNode } from 'react'
import type { LucideIcon } from 'lucide-react'

export function renderAppIcon(icon: LucideIcon, className = 'app-icon-glyph', strokeWidth = 2.1): ReactNode {
  return createElement(icon, {
    className,
    'aria-hidden': true,
    size: '1em',
    strokeWidth,
  })
}
