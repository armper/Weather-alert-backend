import {
  Suspense,
  useEffect,
  useMemo,
  useRef,
  useState,
  type CSSProperties,
  type PointerEvent as ReactPointerEvent,
  type ReactNode,
  type TouchEvent as ReactTouchEvent,
} from 'react'
import { NavLink, useLocation, useNavigate } from 'react-router-dom'
import { APP_ADMIN_NAV_ITEM, APP_PRIMARY_NAV_ITEMS, getAppTabPreviewComponent, preloadAppTabPreview } from '../../appTabRoutes'
import { LoadingPlaceholder } from '../common/LoadingPlaceholder'
import { resolveWeatherVisual } from '../../lib/weatherVisuals'
import { useDataState } from '../../state/useAppState'
import { useSessionState } from '../../state/useAppState'

interface AppShellProps {
  children: ReactNode
}

type SwipeDirection = 'forward' | 'backward'

interface SwipeSession {
  active: boolean
  pointerId: number | null
  pointerType: string
  startX: number
  startY: number
}

interface RouteTransitionState {
  direction: SwipeDirection
  phase: 'primed' | 'running'
  previousChildren: ReactNode
  nextChildren: ReactNode
  releaseOffsetPx: number
}

const SWIPE_NAV_ITEMS = APP_PRIMARY_NAV_ITEMS

const SWIPE_TARGET_BLOCKERS = [
  'input',
  'textarea',
  'select',
  '[role="dialog"]',
  '[aria-haspopup="dialog"]',
  '[contenteditable="true"]',
  '.maplibregl-map',
  '.maplibregl-canvas',
  '.static-map-shell',
  '.rules-modal',
  '.overview-location-dialog',
  '.overview-location-dialog-backdrop',
  '.overview-official-alert-dialog',
  '.overview-official-alert-dialog-backdrop',
  '.settings-dialog',
  '.settings-dialog-backdrop',
  '.sub-dialog',
  '.sub-dialog-backdrop',
].join(', ')

const SWIPE_EDGE_GUARD_PX = 24
const SWIPE_DRAG_ACTIVATION_PX = 18
const TOUCH_SWIPE_THRESHOLD_PX = 72
const POINTER_SWIPE_THRESHOLD_PX = 108
const MAX_VERTICAL_DRIFT_PX = 72
const HORIZONTAL_INTENT_RATIO = 1.35
const SWIPE_DRAG_MAX_RATIO = 0.22
const SWIPE_ROUTE_TRANSITION_MS = 280

function matchesSwipeRoute(pathname: string, route: string) {
  return pathname === route || pathname.startsWith(`${route}/`)
}

function getSwipeRouteIndex(pathname: string) {
  return SWIPE_NAV_ITEMS.findIndex((item) => matchesSwipeRoute(pathname, item.to))
}

function getSwipeDirection(deltaX: number): SwipeDirection {
  return deltaX < 0 ? 'forward' : 'backward'
}

function getAdjacentSwipeRouteIndex(routeIndex: number, direction: SwipeDirection) {
  return direction === 'forward' ? routeIndex + 1 : routeIndex - 1
}

function isWithinHorizontalScroller(target: HTMLElement, boundary: HTMLElement) {
  let current: HTMLElement | null = target
  while (current && current !== boundary) {
    const style = window.getComputedStyle(current)
    const canScrollHorizontally =
      current.scrollWidth > current.clientWidth + 8 && /(auto|scroll)/.test(`${style.overflowX} ${style.overflow}`)
    if (canScrollHorizontally) {
      return true
    }
    current = current.parentElement
  }
  return false
}

function shouldBlockSwipeTarget(target: EventTarget | null, boundary: HTMLElement) {
  if (!(target instanceof HTMLElement)) {
    return true
  }

  if (target.closest(SWIPE_TARGET_BLOCKERS)) {
    return true
  }

  return isWithinHorizontalScroller(target, boundary)
}

function clampSwipeOffset(rawOffset: number, width: number, canNavigate: boolean) {
  const dragCeiling = Math.max(84, width * SWIPE_DRAG_MAX_RATIO)
  const resistance = canNavigate ? 0.92 : 0.14
  const dampedOffset = rawOffset * resistance
  return Math.sign(dampedOffset) * Math.min(Math.abs(dampedOffset), dragCeiling)
}

function NavIcon({ itemKey, className = 'shell-mobile-nav-icon' }: Readonly<{ itemKey: string; className?: string }>) {
  switch (itemKey) {
    case 'overview':
      return (
        <svg viewBox="0 0 20 20" className={className} aria-hidden="true" focusable="false">
          <path d="M3.75 10.25 10 4.75l6.25 5.5v5a1 1 0 0 1-1 1h-2.75v-4h-5v4H4.75a1 1 0 0 1-1-1v-5Z" />
        </svg>
      )
    case 'rules':
      return (
        <svg viewBox="0 0 20 20" className={className} aria-hidden="true" focusable="false">
          <path d="M10 4.5v11M4.5 10h11" />
        </svg>
      )
    case 'travel':
      return (
        <svg viewBox="0 0 20 20" className={className} aria-hidden="true" focusable="false">
          <path d="M3.75 9.25h12.5" />
          <path d="M8 9.25 4.75 5.5" />
          <path d="M8 9.25 4.75 13" />
          <path d="M10.75 9.25 15.25 5.75" />
          <path d="M10.75 9.25 15.25 12.75" />
        </svg>
      )
    case 'account':
      return (
        <svg viewBox="0 0 20 20" className={className} aria-hidden="true" focusable="false">
          <circle cx="10" cy="10" r="6.5" fill="none" />
          <path d="M10 6.5a2.25 2.25 0 1 1 0 4.5 2.25 2.25 0 0 1 0-4.5Z" />
          <path d="M12.6 7.4l1.15-1.15M7.4 12.6l-1.15 1.15M13.5 10h1.6M4.9 10H6.5M12.6 12.6l1.15 1.15M7.4 7.4 6.25 6.25" />
        </svg>
      )
    case 'subscription':
      return (
        <svg viewBox="0 0 20 20" className={className} aria-hidden="true" focusable="false">
          <rect x="3.5" y="5" width="13" height="10" rx="1.5" fill="none" />
          <path d="M3.5 8.5h13" />
          <path d="M6.5 12h3" />
        </svg>
      )
    case 'admin':
      return (
        <svg viewBox="0 0 20 20" className={className} aria-hidden="true" focusable="false">
          <path d="M10 2.75 15.75 5v4.25c0 3.7-2.16 6.53-5.75 8-3.59-1.47-5.75-4.3-5.75-8V5L10 2.75Z" />
          <path d="M10 7.25v4.5M7.75 9.5h4.5" />
        </svg>
      )
    default:
      return null
  }
}

export function AppShell({ children }: AppShellProps) {
  const { isAdmin } = useSessionState()
  const { currentWeather } = useDataState()
  const location = useLocation()
  const navigate = useNavigate()
  const contentRef = useRef<HTMLElement | null>(null)
  const swipeSessionRef = useRef<SwipeSession>({
    active: false,
    pointerId: null,
    pointerType: '',
    startX: 0,
    startY: 0,
  })
  const committedChildrenRef = useRef<ReactNode>(children)
  const committedPathRef = useRef(location.pathname)
  const pendingNavigationRef = useRef<{ direction: SwipeDirection; releaseOffsetPx: number } | null>(null)
  const transitionFrameRef = useRef<number | null>(null)
  const transitionTimerRef = useRef<number | null>(null)
  const [dragOffsetPx, setDragOffsetPx] = useState(0)
  const [dragDirection, setDragDirection] = useState<SwipeDirection | null>(null)
  const [previewRoutePath, setPreviewRoutePath] = useState<string | null>(null)
  const [routeTransition, setRouteTransition] = useState<RouteTransitionState | null>(null)
  const immersiveRoutePrefixes = [
    '/app/overview',
    '/app/rules',
  ]
  const bottomNavRoutePrefixes = [
    ...immersiveRoutePrefixes,
    '/app/travel',
    '/app/account',
    '/app/subscription',
  ]
  const hasBottomNav = bottomNavRoutePrefixes.some((prefix) => location.pathname.startsWith(prefix))
  const isImmersiveRoute = immersiveRoutePrefixes.some((prefix) => location.pathname.startsWith(prefix))
  const swipeRouteIndex = getSwipeRouteIndex(location.pathname)
  const appTabBackgroundImage = resolveWeatherVisual(currentWeather ?? {}).backgroundImage
  const desktopNavItems = isAdmin
    ? [...APP_PRIMARY_NAV_ITEMS, APP_ADMIN_NAV_ITEM]
    : APP_PRIMARY_NAV_ITEMS
  const prefersReducedMotion =
    typeof window !== 'undefined' && window.matchMedia('(prefers-reduced-motion: reduce)').matches
  const routeTransitionDurationMs = prefersReducedMotion ? 1 : SWIPE_ROUTE_TRANSITION_MS
  const isSwipeDragging = Math.abs(dragOffsetPx) > 0
  const activeSwipeDirection = routeTransition?.direction ?? dragDirection
  const previewComponent = useMemo(
    () => (previewRoutePath ? getAppTabPreviewComponent(previewRoutePath) : null),
    [previewRoutePath],
  )
  const dragProgress = Math.min(
    Math.abs(dragOffsetPx) / Math.max((contentRef.current?.clientWidth ?? window.innerWidth) * 0.24, 1),
    1,
  )
  const PreviewComponent = previewComponent
  const shellContentStyle = {
    '--shell-swipe-drag-offset': `${dragOffsetPx}px`,
    '--shell-swipe-progress': `${dragProgress}`,
    '--shell-swipe-release-offset': `${routeTransition?.releaseOffsetPx ?? 0}px`,
    '--shell-route-transition-duration': `${routeTransitionDurationMs}ms`,
  } as CSSProperties

  function clearTransitionTimers() {
    if (transitionFrameRef.current !== null) {
      cancelAnimationFrame(transitionFrameRef.current)
      transitionFrameRef.current = null
    }
    if (transitionTimerRef.current !== null) {
      window.clearTimeout(transitionTimerRef.current)
      transitionTimerRef.current = null
    }
  }

  function resetSwipeSession() {
    swipeSessionRef.current = {
      active: false,
      pointerId: null,
      pointerType: '',
      startX: 0,
      startY: 0,
    }
  }

  function resetDragState() {
    setDragOffsetPx(0)
    setDragDirection(null)
    setPreviewRoutePath(null)
  }

  function beginSwipe(
    clientX: number,
    clientY: number,
    pointerType: string,
    target: EventTarget | null,
    boundary: HTMLElement,
  ) {
    if (swipeRouteIndex < 0 || routeTransition) {
      return false
    }

    const nearViewportEdge =
      clientX <= SWIPE_EDGE_GUARD_PX || clientX >= window.innerWidth - SWIPE_EDGE_GUARD_PX
    const blocked = nearViewportEdge || shouldBlockSwipeTarget(target, boundary)
    if (blocked) {
      resetSwipeSession()
      resetDragState()
      return false
    }

    swipeSessionRef.current = {
      active: true,
      pointerId: pointerType === 'touch' ? null : 0,
      pointerType,
      startX: clientX,
      startY: clientY,
    }

    return true
  }

  function updateSwipeDrag(clientX: number, clientY: number) {
    const session = swipeSessionRef.current
    if (!session.active || routeTransition) {
      return false
    }

    const deltaX = clientX - session.startX
    const deltaY = clientY - session.startY
    const horizontalDistance = Math.abs(deltaX)
    const verticalDistance = Math.abs(deltaY)

    if (verticalDistance > MAX_VERTICAL_DRIFT_PX && horizontalDistance < TOUCH_SWIPE_THRESHOLD_PX) {
      resetDragState()
      return false
    }

    if (horizontalDistance < SWIPE_DRAG_ACTIVATION_PX || horizontalDistance <= verticalDistance) {
      resetDragState()
      return false
    }

    const direction = getSwipeDirection(deltaX)
    const targetIndex = getAdjacentSwipeRouteIndex(swipeRouteIndex, direction)
    const previewRoute = SWIPE_NAV_ITEMS[targetIndex]
    const width = Math.max(contentRef.current?.clientWidth ?? window.innerWidth, 1)
    const clampedOffset = clampSwipeOffset(deltaX, width, Boolean(previewRoute))

    setDragDirection(direction)
    setDragOffsetPx(clampedOffset)
    setPreviewRoutePath(previewRoute?.to ?? null)
    if (previewRoute) {
      void preloadAppTabPreview(previewRoute.to)
    }

    return true
  }

  function finalizeSwipe(clientX: number, clientY: number) {
    const session = swipeSessionRef.current
    if (!session.active || swipeRouteIndex < 0 || routeTransition) {
      resetSwipeSession()
      resetDragState()
      return
    }

    const deltaX = clientX - session.startX
    const deltaY = clientY - session.startY
    const threshold = session.pointerType === 'mouse' ? POINTER_SWIPE_THRESHOLD_PX : TOUCH_SWIPE_THRESHOLD_PX
    const horizontalDistance = Math.abs(deltaX)
    const verticalDistance = Math.abs(deltaY)
    const direction = getSwipeDirection(deltaX)
    const nextIndex = getAdjacentSwipeRouteIndex(swipeRouteIndex, direction)
    const nextRoute = SWIPE_NAV_ITEMS[nextIndex]
    const releaseOffsetPx = dragOffsetPx || deltaX

    const hasHorizontalIntent =
      horizontalDistance >= threshold &&
      verticalDistance <= MAX_VERTICAL_DRIFT_PX &&
      horizontalDistance > verticalDistance * HORIZONTAL_INTENT_RATIO &&
      Boolean(nextRoute)

    resetSwipeSession()

    if (!hasHorizontalIntent || !nextRoute) {
      resetDragState()
      return
    }

    pendingNavigationRef.current = {
      direction,
      releaseOffsetPx,
    }
    resetDragState()
    navigate(nextRoute.to)
  }

  function handleSwipeStart(event: ReactPointerEvent<HTMLElement>) {
    if (event.pointerType === 'touch') {
      return
    }

    if (!event.isPrimary) {
      return
    }

    if (event.pointerType === 'mouse' && event.button !== 0) {
      return
    }

    const didStart = beginSwipe(event.clientX, event.clientY, event.pointerType, event.target, event.currentTarget)
    if (didStart) {
      swipeSessionRef.current.pointerId = event.pointerId
      event.currentTarget.setPointerCapture(event.pointerId)
    }
  }

  function handleSwipeMove(event: ReactPointerEvent<HTMLElement>) {
    if (event.pointerType === 'touch') {
      return
    }

    const session = swipeSessionRef.current
    if (!session.active || session.pointerId !== event.pointerId) {
      return
    }

    updateSwipeDrag(event.clientX, event.clientY)
  }

  function handleSwipeEnd(event: ReactPointerEvent<HTMLElement>) {
    if (event.pointerType === 'touch') {
      return
    }

    const session = swipeSessionRef.current
    if (session.pointerId !== event.pointerId) {
      return
    }

    if (event.currentTarget.hasPointerCapture(event.pointerId)) {
      event.currentTarget.releasePointerCapture(event.pointerId)
    }

    finalizeSwipe(event.clientX, event.clientY)
  }

  function handleSwipeCancel(event: ReactPointerEvent<HTMLElement>) {
    if (event.pointerType === 'touch') {
      return
    }

    const session = swipeSessionRef.current
    if (session.pointerId === event.pointerId && event.currentTarget.hasPointerCapture(event.pointerId)) {
      event.currentTarget.releasePointerCapture(event.pointerId)
    }
    resetSwipeSession()
    resetDragState()
  }

  function handleTouchStart(event: ReactTouchEvent<HTMLElement>) {
    const touch = event.touches[0]
    if (!touch) {
      return
    }

    beginSwipe(touch.clientX, touch.clientY, 'touch', event.target, event.currentTarget)
  }

  function handleTouchMove(event: ReactTouchEvent<HTMLElement>) {
    const touch = event.touches[0]
    if (!touch) {
      return
    }

    updateSwipeDrag(touch.clientX, touch.clientY)
  }

  function handleTouchEnd(event: ReactTouchEvent<HTMLElement>) {
    const touch = event.changedTouches[0]
    if (!touch) {
      resetSwipeSession()
      resetDragState()
      return
    }

    finalizeSwipe(touch.clientX, touch.clientY)
  }

  function handleTouchCancel() {
    resetSwipeSession()
    resetDragState()
  }

  useEffect(() => {
    if (routeTransition) {
      return
    }

    if (location.pathname === committedPathRef.current) {
      committedChildrenRef.current = children
      return
    }

    const previousPath = committedPathRef.current
    const previousChildren = committedChildrenRef.current
    const previousIndex = getSwipeRouteIndex(previousPath)
    const nextIndex = getSwipeRouteIndex(location.pathname)
    const pendingNavigation = pendingNavigationRef.current

    if (previousIndex >= 0 && nextIndex >= 0 && previousIndex !== nextIndex) {
      const direction = pendingNavigation?.direction ?? (nextIndex > previousIndex ? 'forward' : 'backward')
      const releaseOffsetPx = pendingNavigation?.direction === direction ? pendingNavigation.releaseOffsetPx : 0
      pendingNavigationRef.current = null
      setRouteTransition({
        direction,
        phase: 'primed',
        previousChildren,
        nextChildren: children,
        releaseOffsetPx,
      })
      return
    }

    pendingNavigationRef.current = null
    committedPathRef.current = location.pathname
    committedChildrenRef.current = children
  }, [children, location.pathname, routeTransition])

  useEffect(() => {
    if (!routeTransition || routeTransition.phase !== 'primed') {
      return
    }

    clearTransitionTimers()
    transitionFrameRef.current = requestAnimationFrame(() => {
      setRouteTransition((current) => (current ? { ...current, phase: 'running' } : null))
    })

    return () => {
      if (transitionFrameRef.current !== null) {
        cancelAnimationFrame(transitionFrameRef.current)
        transitionFrameRef.current = null
      }
    }
  }, [routeTransition])

  useEffect(() => {
    if (!routeTransition || routeTransition.phase !== 'running') {
      return
    }

    transitionTimerRef.current = window.setTimeout(() => {
      committedPathRef.current = location.pathname
      committedChildrenRef.current = children
      setRouteTransition(null)
      transitionTimerRef.current = null
    }, routeTransitionDurationMs)

    return () => {
      if (transitionTimerRef.current !== null) {
        window.clearTimeout(transitionTimerRef.current)
        transitionTimerRef.current = null
      }
    }
  }, [children, location.pathname, routeTransition, routeTransitionDurationMs])

  useEffect(() => clearTransitionTimers, [])

  return (
    <div className={`app-shell${hasBottomNav ? ' has-bottom-nav' : ''}${isImmersiveRoute ? ' is-immersive-route' : ''}`}>
      {swipeRouteIndex >= 0 ? (
        <div className="app-shell-route-background" aria-hidden="true">
          <img className="app-shell-route-background-image" src={appTabBackgroundImage} alt="" />
        </div>
      ) : null}
      <div className="shell-body">
        <nav className="shell-sidebar panel" aria-label="Primary navigation">
          {desktopNavItems.map((item) => (
            <NavLink key={item.key} to={item.to} className={({ isActive }) => `shell-nav-link${isActive ? ' active' : ''}`}>
              <NavIcon itemKey={item.key} className="shell-nav-icon" />
              {item.label}
            </NavLink>
          ))}
        </nav>

        <main
          ref={contentRef}
          className={`shell-content${isSwipeDragging ? ' is-swipe-dragging' : ''}${routeTransition ? ' is-route-transitioning' : ''}${routeTransition?.phase === 'running' ? ' is-route-transition-running' : ''}`}
          data-swipe-direction={activeSwipeDirection ?? undefined}
          style={shellContentStyle}
          onPointerDown={handleSwipeStart}
          onPointerMove={handleSwipeMove}
          onPointerUp={handleSwipeEnd}
          onPointerCancel={handleSwipeCancel}
          onTouchStart={handleTouchStart}
          onTouchMove={handleTouchMove}
          onTouchEnd={handleTouchEnd}
          onTouchCancel={handleTouchCancel}
        >
          <div className="shell-route-stage">
            {routeTransition ? (
              <>
                <div className="shell-route-layer shell-route-layer--previous">{routeTransition.previousChildren}</div>
                <div className="shell-route-layer shell-route-layer--current">{routeTransition.nextChildren}</div>
              </>
            ) : (
              <>
                <div className="shell-route-layer shell-route-layer--current">{children}</div>
                {previewComponent ? (
                  <div className="shell-route-layer shell-route-layer--preview" aria-hidden="true">
                    <Suspense
                      fallback={(
                        <section className="page-stack">
                          <article className="panel">
                            <LoadingPlaceholder
                              title="Loading next page"
                              copy="Preparing the preview."
                              lineCount={3}
                            />
                          </article>
                        </section>
                      )}
                    >
                      {PreviewComponent ? <PreviewComponent /> : null}
                    </Suspense>
                  </div>
                ) : null}
              </>
            )}
          </div>
        </main>
      </div>

      <nav className="shell-mobile-nav" aria-label="Mobile primary navigation">
        {APP_PRIMARY_NAV_ITEMS.map((item) => (
          <NavLink
            key={item.key}
            to={item.to}
            className={({ isActive }) => `shell-mobile-nav-link${isActive ? ' active' : ''}`}
            aria-label={item.label}
            aria-current={location.pathname === item.to ? 'page' : undefined}
          >
            <span className="shell-mobile-nav-icon-frame">
              <NavIcon itemKey={item.key} />
            </span>
          </NavLink>
        ))}
      </nav>
    </div>
  )
}
