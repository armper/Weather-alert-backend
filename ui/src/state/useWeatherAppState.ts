import { useCallback, useEffect, useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import { ApiError, apiRequest, toErrorMessage } from '../api'
import type {
  AlertCriteria,
  AlertEvent,
  BillingCheckoutSession,
  BillingPlan,
  BillingStatus,
  AuthTokenResponse,
  ChannelVerification,
  JobRunResponse,
  MessageResponse,
  NwsProduct,
  PagedResponse,
  RecoveryRequestResponse,
  RegisterUserResponse,
  TravelPlan,
  UserAccount,
  UserNotificationPreference,
  UsernameRecoveryResponse,
  WeatherCondition,
} from '../types'
import { buildCriteriaPayload, RIVER_RULE_TYPES, RIVER_STAGE_RULE_TYPES } from '../lib/criteria'
import {
  DEFAULT_LAT,
  DEFAULT_LON,
  initialCriteriaForm,
  initialForgotPassword,
  initialForgotUsername,
  initialLogin,
  initialMagicLink,
  initialPasswordForm,
  initialRegister,
  initialVerify,
  STORAGE_KEY,
  type CriteriaFormState,
  type ForgotPasswordState,
  type ForgotUsernameState,
  type LoginState,
  type MagicLinkState,
  type NoticeState,
  type PasswordFormState,
  type ProfileFormState,
  type RegisterState,
  type VerifyState,
} from './types'

function unwrapCollection<T>(payload: T[] | PagedResponse<T> | null | undefined): T[] {
  if (Array.isArray(payload)) {
    return payload
  }
  if (payload && Array.isArray(payload.items)) {
    return payload.items
  }
  return []
}

export function useWeatherAppState() {
  type AdminJobKey = 'weather-processing' | 'alert-delivery-retries' | 'data-retention'

  const [token, setToken] = useState<string | null>(() => localStorage.getItem(STORAGE_KEY))
  const [notice, setNotice] = useState<NoticeState | null>(null)

  const [loginState, setLoginState] = useState<LoginState>(initialLogin)
  const [magicLinkState, setMagicLinkState] = useState<MagicLinkState>(initialMagicLink)
  const [registerState, setRegisterState] = useState<RegisterState>(initialRegister)
  const [verifyState, setVerifyState] = useState<VerifyState>(initialVerify)
  const [latestVerification, setLatestVerification] = useState<ChannelVerification | null>(null)
  const [forgotUsernameState, setForgotUsernameState] = useState<ForgotUsernameState>(initialForgotUsername)
  const [forgotPasswordState, setForgotPasswordState] = useState<ForgotPasswordState>(initialForgotPassword)
  const [usernameRecoveryMeta, setUsernameRecoveryMeta] = useState<RecoveryRequestResponse | null>(null)
  const [passwordRecoveryMeta, setPasswordRecoveryMeta] = useState<RecoveryRequestResponse | null>(null)
  const [magicLinkMeta, setMagicLinkMeta] = useState<RecoveryRequestResponse | null>(null)
  const [pendingMagicLinkToken, setPendingMagicLinkToken] = useState<{ recoveryId: string; code: string } | null>(null)

  const [me, setMe] = useState<UserAccount | null>(null)
  const [criteria, setCriteria] = useState<AlertCriteria[]>([])
  const [alerts, setAlerts] = useState<AlertEvent[]>([])
  const [currentWeather, setCurrentWeather] = useState<WeatherCondition | null>(null)
  const [notificationPreference, setNotificationPreference] = useState<UserNotificationPreference | null>(null)
  const [billingStatus, setBillingStatus] = useState<BillingStatus | null>(null)
  const [adminUsers, setAdminUsers] = useState<UserAccount[]>([])
  const [observationHistory, setObservationHistory] = useState<WeatherCondition[]>([])
  const [dailyForecast, setDailyForecast] = useState<WeatherCondition[]>([])
  const [hourlyForecast, setHourlyForecast] = useState<WeatherCondition[]>([])
  const [nwsProducts, setNwsProducts] = useState<NwsProduct[]>([])
  const [travelPlans, setTravelPlans] = useState<TravelPlan[]>([])

  const [profileForm, setProfileForm] = useState<ProfileFormState>({ name: '', phoneNumber: '' })
  const [passwordForm, setPasswordForm] = useState<PasswordFormState>(initialPasswordForm)
  const [criteriaForm, setCriteriaForm] = useState<CriteriaFormState>(initialCriteriaForm)

  const [loadingAuth, setLoadingAuth] = useState(false)
  const [confirmingMagicLink, setConfirmingMagicLink] = useState(false)
  const [loadingData, setLoadingData] = useState(false)
  const [savingCriteria, setSavingCriteria] = useState(false)
  const [savingProfile, setSavingProfile] = useState(false)
  const [savingTravelPlan, setSavingTravelPlan] = useState(false)
  const [loadingBilling, setLoadingBilling] = useState(false)
  const [checkoutPlan, setCheckoutPlan] = useState<BillingPlan | null>(null)
  const [changingPlan, setChangingPlan] = useState<BillingPlan | null>(null)
  const [openingBillingPortal, setOpeningBillingPortal] = useState(false)
  const [deletingAccount, setDeletingAccount] = useState(false)
  const [busyAlertId, setBusyAlertId] = useState<string | null>(null)
  const [busyCriteriaId, setBusyCriteriaId] = useState<string | null>(null)
  const [busyAdminAction, setBusyAdminAction] = useState<string | null>(null)
  const [busyAdminJob, setBusyAdminJob] = useState<AdminJobKey | null>(null)
  const [adminJobResults, setAdminJobResults] = useState<Partial<Record<AdminJobKey, JobRunResponse>>>({})
  const [usernameRetryAfterSeconds, setUsernameRetryAfterSeconds] = useState<number>(0)
  const [passwordRetryAfterSeconds, setPasswordRetryAfterSeconds] = useState<number>(0)

  const isAdmin = useMemo(() => Boolean(me?.role?.includes('ADMIN')), [me?.role])

  const canSubmitCriteria = useMemo(() => {
    if (!criteriaForm.location.trim()) {
      return false
    }
    if (!criteriaForm.latitude.trim() || !criteriaForm.longitude.trim()) {
      return false
    }
    const isRiverRule = RIVER_RULE_TYPES.includes(criteriaForm.ruleType)
    if (isRiverRule && !criteriaForm.riverGaugeId.trim()) {
      return false
    }
    if (criteriaForm.ruleType === 'RIVER_FLOOD_CATEGORY') {
      return true
    }
    if (RIVER_STAGE_RULE_TYPES.includes(criteriaForm.ruleType)) {
      return !Number.isNaN(Number(criteriaForm.threshold))
    }
    return !Number.isNaN(Number(criteriaForm.threshold))
  }, [criteriaForm])

  const refreshData = useCallback(async (activeToken: string, account: UserAccount) => {
    setLoadingData(true)
    setLoadingBilling(true)
    try {
      const freshCriteria = await apiRequest<AlertCriteria[]>(`/api/criteria/user/${account.id}`, {
        token: activeToken,
      })
      setCriteria(freshCriteria)

      const lat = freshCriteria[0]?.latitude?.toString() ?? DEFAULT_LAT
      const lon = freshCriteria[0]?.longitude?.toString() ?? DEFAULT_LON

      const [freshAlerts, preferences, weather, billing, adminAccounts, history, daily, hourly, products, trips] = await Promise.all([
        apiRequest<AlertEvent[] | PagedResponse<AlertEvent>>(`/api/alerts/user/${account.id}`, { token: activeToken }),
        apiRequest<UserNotificationPreference>('/api/users/me/notification-preferences', { token: activeToken }).catch(
          () => null,
        ),
        apiRequest<WeatherCondition>(
          `/api/weather/conditions/current?latitude=${encodeURIComponent(lat)}&longitude=${encodeURIComponent(lon)}`,
          { token: activeToken },
        ).catch(() => null),
        apiRequest<BillingStatus>('/api/billing/me', { token: activeToken }).catch(() => null),
        account.role.includes('ADMIN')
          ? apiRequest<UserAccount[]>('/api/admin/users', { token: activeToken })
          : Promise.resolve([]),
        apiRequest<WeatherCondition[]>(
          `/api/weather/conditions/history?latitude=${encodeURIComponent(lat)}&longitude=${encodeURIComponent(lon)}&hours=6`,
          { token: activeToken },
        ).catch(() => [] as WeatherCondition[]),
        apiRequest<WeatherCondition[]>(
          `/api/weather/conditions/daily?latitude=${encodeURIComponent(lat)}&longitude=${encodeURIComponent(lon)}`,
          { token: activeToken },
        ).catch(() => [] as WeatherCondition[]),
        apiRequest<WeatherCondition[]>(
          `/api/weather/conditions/forecast?latitude=${encodeURIComponent(lat)}&longitude=${encodeURIComponent(lon)}&hours=24`,
          { token: activeToken },
        ).catch(() => [] as WeatherCondition[]),
        apiRequest<NwsProduct[]>('/api/weather/products?type=AFD', { token: activeToken }).catch(() => [] as NwsProduct[]),
        apiRequest<TravelPlan[] | PagedResponse<TravelPlan>>(`/api/travel-plans/user/${account.id}`, {
          token: activeToken,
        }).catch(() => [] as TravelPlan[]),
      ])

      const alertItems = unwrapCollection(freshAlerts)

      setAlerts(
        [...alertItems].sort((a, b) => new Date(b.alertTime ?? '').getTime() - new Date(a.alertTime ?? '').getTime()),
      )
      setNotificationPreference(preferences)
      setCurrentWeather(weather)
      setBillingStatus(billing)
      setAdminUsers(adminAccounts)
      setObservationHistory(history)
      setDailyForecast(daily)
      setHourlyForecast(hourly)
      setNwsProducts(products)
      setTravelPlans(
        unwrapCollection(trips).sort((left, right) => {
          const startCompare = left.startDate.localeCompare(right.startDate)
          return startCompare !== 0 ? startCompare : left.id.localeCompare(right.id)
        }),
      )
    } catch (error) {
      setNotice({ kind: 'error', text: toErrorMessage(error) })
    } finally {
      setLoadingData(false)
      setLoadingBilling(false)
    }
  }, [])

  const bootstrap = useCallback(
    async (activeToken: string) => {
      setLoadingData(true)
      try {
        const account = await apiRequest<UserAccount>('/api/users/me', { token: activeToken })
        setMe(account)
        setProfileForm({
          name: account.name ?? '',
          phoneNumber: account.phoneNumber ?? '',
        })
        await refreshData(activeToken, account)
      } catch (error) {
        localStorage.removeItem(STORAGE_KEY)
        setToken(null)
        setNotice({ kind: 'error', text: `Session expired. ${toErrorMessage(error)}` })
      } finally {
        setLoadingData(false)
      }
    },
    [refreshData],
  )

  useEffect(() => {
    if (!token) {
      setMe(null)
      setCriteria([])
      setAlerts([])
      setCurrentWeather(null)
      setNotificationPreference(null)
      setBillingStatus(null)
      setAdminUsers([])
      setObservationHistory([])
      setDailyForecast([])
      setHourlyForecast([])
      setNwsProducts([])
      setTravelPlans([])
      return
    }
    void bootstrap(token)
  }, [token, bootstrap])

  useEffect(() => {
    if (token) {
      return
    }

    const params = new URLSearchParams(window.location.search)
    const mode = params.get('recoveryMode')
    const authMode = params.get('authMode')
    const recoveryId = params.get('recoveryId') ?? ''
    const recoveryCode = params.get('recoveryCode') ?? ''
    const hasRecoveryQuery = recoveryId !== '' || recoveryCode !== ''

    if (mode === 'username') {
      setForgotUsernameState((state) => ({
        ...state,
        recoveryId: recoveryId || state.recoveryId,
        code: recoveryCode || state.code,
      }))
      if (hasRecoveryQuery) {
        window.history.replaceState(null, '', '/auth/forgot-username')
      }
    } else if (mode === 'password') {
      setForgotPasswordState((state) => ({
        ...state,
        recoveryId: recoveryId || state.recoveryId,
        code: recoveryCode || state.code,
      }))
      if (hasRecoveryQuery) {
        window.history.replaceState(null, '', '/auth/forgot-password')
      }
    } else if (authMode === 'magic-link' && recoveryId && recoveryCode) {
      setPendingMagicLinkToken({ recoveryId, code: recoveryCode })
      window.history.replaceState(null, '', '/auth/login')
    }
  }, [token])

  useEffect(() => {
    if (usernameRetryAfterSeconds <= 0) {
      return
    }
    const id = window.setTimeout(() => setUsernameRetryAfterSeconds((seconds) => Math.max(0, seconds - 1)), 1000)
    return () => window.clearTimeout(id)
  }, [usernameRetryAfterSeconds])

  useEffect(() => {
    if (passwordRetryAfterSeconds <= 0) {
      return
    }
    const id = window.setTimeout(() => setPasswordRetryAfterSeconds((seconds) => Math.max(0, seconds - 1)), 1000)
    return () => window.clearTimeout(id)
  }, [passwordRetryAfterSeconds])

  const persistToken = useCallback((accessToken: string) => {
    localStorage.setItem(STORAGE_KEY, accessToken)
    setToken(accessToken)
  }, [])

  function logout() {
    localStorage.removeItem(STORAGE_KEY)
    setNotice(null)
    setToken(null)
  }

  async function handleLogin(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setLoadingAuth(true)
    setNotice(null)
    try {
      const response = await apiRequest<AuthTokenResponse>('/api/auth/token', {
        method: 'POST',
        body: loginState,
      })
      persistToken(response.accessToken)
      setLoginState(initialLogin)
    } catch (error) {
      setNotice({ kind: 'error', text: toErrorMessage(error) })
    } finally {
      setLoadingAuth(false)
    }
  }

  const confirmMagicLinkToken = useCallback(async (recoveryId: string, code: string, successText: string) => {
    setConfirmingMagicLink(true)
    setNotice(null)

    try {
      const response = await apiRequest<AuthTokenResponse>('/api/auth/magic-link/confirm', {
        method: 'POST',
        body: {
          recoveryId,
          code,
        },
      })
      persistToken(response.accessToken)
      setMagicLinkState(initialMagicLink)
      setMagicLinkMeta(null)
      setNotice({ kind: 'success', text: successText })
      return true
    } catch (error) {
      setNotice({ kind: 'error', text: toErrorMessage(error) })
      return false
    } finally {
      setConfirmingMagicLink(false)
    }
  }, [persistToken])

  useEffect(() => {
    if (token || !pendingMagicLinkToken) {
      return
    }

    void confirmMagicLinkToken(
      pendingMagicLinkToken.recoveryId,
      pendingMagicLinkToken.code,
      'Signed in from your SkyPanda email link.',
    ).finally(() => {
      setPendingMagicLinkToken(null)
    })
  }, [token, pendingMagicLinkToken, confirmMagicLinkToken])

  async function handleMagicLinkRequest(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setLoadingAuth(true)
    setNotice(null)

    try {
      const response = await apiRequest<RecoveryRequestResponse>('/api/auth/magic-link/request', {
        method: 'POST',
        body: {
          usernameOrEmail: magicLinkState.usernameOrEmail,
        },
      })
      setMagicLinkMeta(response)
      setMagicLinkState((state) => ({
        ...state,
        recoveryId: response.recoveryId ?? state.recoveryId,
        code: '',
      }))
      setNotice({
        kind: 'success',
        text: 'Check your email for a secure SkyPanda sign-in link.',
      })
    } catch (error) {
      setNotice({ kind: 'error', text: toErrorMessage(error) })
    } finally {
      setLoadingAuth(false)
    }
  }

  async function handleMagicLinkConfirm(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!magicLinkState.recoveryId.trim()) {
      setNotice({ kind: 'error', text: 'Request a sign-in link first or open the link from your email.' })
      return
    }
    await confirmMagicLinkToken(
      magicLinkState.recoveryId,
      magicLinkState.code,
      'Signed in from your SkyPanda email link.',
    )
  }

  async function handleRegister(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setLoadingAuth(true)
    setNotice(null)

    try {
      const response = await apiRequest<RegisterUserResponse>('/api/auth/register', {
        method: 'POST',
        body: registerState,
      })

      const verification = response.emailVerification ?? null
      setLatestVerification(verification)

      setVerifyState({
        userId: response.account.id,
        verificationId: verification?.id ?? '',
        token: '',
      })

      setNotice({
        kind: 'success',
        text: `Account ${response.account.id} created. Check your email for your verification code.`,
      })
      setRegisterState(initialRegister)
    } catch (error) {
      setNotice({ kind: 'error', text: toErrorMessage(error) })
    } finally {
      setLoadingAuth(false)
    }
  }

  async function handleVerifyEmail(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const verificationId = verifyState.verificationId.trim()
    if (!verificationId) {
      setNotice({
        kind: 'error',
        text: 'Open the verification link from your email or resend the code before confirming.',
      })
      return
    }

    setLoadingAuth(true)
    setNotice(null)

    try {
      await apiRequest<UserAccount>('/api/auth/register/verify-email', {
        method: 'POST',
        body: {
          ...verifyState,
          verificationId,
        },
      })
      setNotice({
        kind: 'success',
        text: 'Email verification successful. You can now log in.',
      })
    } catch (error) {
      setNotice({ kind: 'error', text: toErrorMessage(error) })
    } finally {
      setLoadingAuth(false)
    }
  }

  async function handleResendVerification() {
    const username = verifyState.userId.trim()
    if (!username) {
      setNotice({ kind: 'error', text: 'Enter your username to resend verification.' })
      return
    }

    setLoadingAuth(true)
    setNotice(null)

    try {
      const verification = await apiRequest<ChannelVerification>('/api/auth/register/resend-verification', {
        method: 'POST',
        body: { username },
      })
      setLatestVerification(verification)
      setVerifyState((current) => ({
        ...current,
        userId: username,
        verificationId: verification.id,
        token: '',
      }))
      setNotice({ kind: 'success', text: `A fresh verification code was sent for ${username}.` })
    } catch (error) {
      setNotice({ kind: 'error', text: toErrorMessage(error) })
    } finally {
      setLoadingAuth(false)
    }
  }

  async function handleForgotUsernameRequest(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setLoadingAuth(true)
    setNotice(null)

    try {
      const response = await apiRequest<RecoveryRequestResponse>('/api/auth/recovery/username/request', {
        method: 'POST',
        body: { email: forgotUsernameState.email },
      })
      setUsernameRecoveryMeta(response)
      setUsernameRetryAfterSeconds(response.retryAfterSeconds ?? 0)
      setNotice({ kind: 'success', text: response.message })
    } catch (error) {
      setNotice({ kind: 'error', text: toErrorMessage(error) })
    } finally {
      setLoadingAuth(false)
    }
  }

  async function handleForgotUsernameConfirm(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setLoadingAuth(true)
    setNotice(null)

    try {
      const response = await apiRequest<UsernameRecoveryResponse>('/api/auth/recovery/username/confirm', {
        method: 'POST',
        body: {
          recoveryId: forgotUsernameState.recoveryId,
          code: forgotUsernameState.code,
        },
      })
      setLoginState((state) => ({ ...state, username: response.username }))
      setNotice({ kind: 'success', text: `${response.message} Username: ${response.username}` })
      setForgotUsernameState((state) => ({ ...state, code: '' }))
    } catch (error) {
      setNotice({ kind: 'error', text: toErrorMessage(error) })
    } finally {
      setLoadingAuth(false)
    }
  }

  async function handleForgotPasswordRequest(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setLoadingAuth(true)
    setNotice(null)

    try {
      const response = await apiRequest<RecoveryRequestResponse>('/api/auth/recovery/password/request', {
        method: 'POST',
        body: { usernameOrEmail: forgotPasswordState.usernameOrEmail },
      })
      setPasswordRecoveryMeta(response)
      setForgotPasswordState((state) => ({
        ...state,
        recoveryId: response.recoveryId ?? state.recoveryId,
        code: '',
      }))
      setPasswordRetryAfterSeconds(response.retryAfterSeconds ?? 0)
      setNotice({ kind: 'success', text: 'Check your email for a secure SkyPanda reset link.' })
    } catch (error) {
      setNotice({ kind: 'error', text: toErrorMessage(error) })
    } finally {
      setLoadingAuth(false)
    }
  }

  async function handleForgotPasswordConfirm(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setLoadingAuth(true)
    setNotice(null)

    try {
      const response = await apiRequest<MessageResponse>('/api/auth/recovery/password/confirm', {
        method: 'POST',
        body: {
          recoveryId: forgotPasswordState.recoveryId,
          code: forgotPasswordState.code,
          newPassword: forgotPasswordState.newPassword,
        },
      })
      setNotice({ kind: 'success', text: response.message })
      setLoginState((state) => ({ ...state, password: '' }))
      setForgotPasswordState((state) => ({
        ...state,
        code: '',
        newPassword: '',
      }))
      window.setTimeout(() => {
        window.location.assign('/auth/login')
      }, 400)
    } catch (error) {
      setNotice({ kind: 'error', text: toErrorMessage(error) })
    } finally {
      setLoadingAuth(false)
    }
  }

  async function handleCreateCriteria(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!token || !me) {
      return { ok: false as const, reason: 'unauthorized' as const }
    }

    setSavingCriteria(true)
    setNotice(null)

    try {
      const payload = buildCriteriaPayload(criteriaForm, me.id)
      await apiRequest<AlertCriteria>('/api/criteria', {
        method: 'POST',
        token,
        body: payload,
      })
      setNotice({ kind: 'success', text: `Created alert "${String(payload.name ?? criteriaForm.name ?? 'New alert')}".` })
      await refreshData(token, me)
      return { ok: true as const }
    } catch (error) {
      if (error instanceof ApiError && error.problem?.errorCode === 'BILLING_STATE_ERROR') {
        return {
          ok: false as const,
          reason: 'billing_limit' as const,
          message: toErrorMessage(error),
        }
      }
      setNotice({ kind: 'error', text: toErrorMessage(error) })
      return { ok: false as const, reason: 'error' as const, message: toErrorMessage(error) }
    } finally {
      setSavingCriteria(false)
    }
  }

  async function handleDeleteCriteria(criteriaId: string) {
    if (!token) {
      return
    }

    setBusyCriteriaId(criteriaId)
    setNotice(null)

    try {
      await apiRequest<void>(`/api/criteria/${criteriaId}`, {
        method: 'DELETE',
        token,
      })
      setNotice({ kind: 'success', text: 'Alert deleted.' })
      if (me) {
        await refreshData(token, me)
      }
    } catch (error) {
      setNotice({ kind: 'error', text: toErrorMessage(error) })
    } finally {
      setBusyCriteriaId(null)
    }
  }

  async function handleToggleCriteriaEnabled(criteriaId: string, enabled: boolean) {
    if (!token) {
      return
    }

    const existing = criteria.find((item) => item.id === criteriaId)
    if (!existing) {
      setNotice({ kind: 'error', text: 'Rule not found.' })
      return
    }

    setBusyCriteriaId(criteriaId)
    setNotice(null)

    try {
      await apiRequest<AlertCriteria>(`/api/criteria/${criteriaId}`, {
        method: 'PUT',
        token,
        body: {
          userId: existing.userId,
          name: existing.name,
          location: existing.location,
          latitude: existing.latitude,
          longitude: existing.longitude,
          radiusKm: existing.radiusKm,
          eventType: existing.eventType,
          minSeverity: existing.minSeverity,
          maxTemperature: existing.maxTemperature,
          minTemperature: existing.minTemperature,
          maxWindSpeed: existing.maxWindSpeed,
          maxPrecipitation: existing.maxPrecipitation,
          temperatureThreshold: existing.temperatureThreshold,
          temperatureDirection: existing.temperatureDirection,
          rainThreshold: existing.rainThreshold,
          rainThresholdType: existing.rainThresholdType,
          humidityThreshold: existing.humidityThreshold,
          humidityDirection: existing.humidityDirection,
          dewPointThreshold: existing.dewPointThreshold,
          dewPointDirection: existing.dewPointDirection,
          windGustThreshold: existing.windGustThreshold,
          skyCoverThreshold: existing.skyCoverThreshold,
          skyCoverDirection: existing.skyCoverDirection,
          riverGaugeId: existing.riverGaugeId,
          riverStageThreshold: existing.riverStageThreshold,
          riverStageDirection: existing.riverStageDirection,
          riverFloodCategoryThreshold: existing.riverFloodCategoryThreshold,
          monitorCurrent: existing.monitorCurrent,
          monitorForecast: existing.monitorForecast,
          forecastWindowHours: existing.forecastWindowHours,
          temperatureUnit: existing.temperatureUnit,
          oncePerEvent: existing.oncePerEvent,
          rearmWindowMinutes: existing.rearmWindowMinutes,
          enabled,
        },
      })
      setNotice({ kind: 'success', text: `Rule ${enabled ? 'enabled' : 'paused'}.` })
      if (me) {
        await refreshData(token, me)
      }
    } catch (error) {
      setNotice({ kind: 'error', text: toErrorMessage(error) })
    } finally {
      setBusyCriteriaId(null)
    }
  }

  async function handleCreateTravelPlan(input: {
    name: string
    destination: string
    startDate: string
    endDate: string
    latitude?: number
    longitude?: number
    notes?: string
    alertsEnabled: boolean
  }): Promise<boolean> {
    if (!token || !me) {
      return false
    }

    setSavingTravelPlan(true)
    setNotice(null)

    try {
      const created = await apiRequest<TravelPlan>('/api/travel-plans', {
        method: 'POST',
        token,
        body: {
          ...input,
          userId: me.id,
        },
      })
      setTravelPlans((current) =>
        [...current, created].sort((left, right) => {
          const startCompare = left.startDate.localeCompare(right.startDate)
          return startCompare !== 0 ? startCompare : left.id.localeCompare(right.id)
        }),
      )
      setNotice({ kind: 'success', text: `Trip "${created.name}" added.` })
      return true
    } catch (error) {
      setNotice({ kind: 'error', text: toErrorMessage(error) })
      return false
    } finally {
      setSavingTravelPlan(false)
    }
  }

  async function handleUpdateTravelPlan(
    travelPlanId: string,
    input: {
      name: string
      destination: string
      startDate: string
      endDate: string
      latitude?: number
      longitude?: number
      notes?: string
      alertsEnabled: boolean
    },
  ): Promise<boolean> {
    if (!token || !me) {
      return false
    }

    setSavingTravelPlan(true)
    setNotice(null)

    try {
      const updated = await apiRequest<TravelPlan>(`/api/travel-plans/${travelPlanId}`, {
        method: 'PUT',
        token,
        body: {
          ...input,
          userId: me.id,
        },
      })
      setTravelPlans((current) =>
        current
          .map((item) => (item.id === travelPlanId ? updated : item))
          .sort((left, right) => {
            const startCompare = left.startDate.localeCompare(right.startDate)
            return startCompare !== 0 ? startCompare : left.id.localeCompare(right.id)
          }),
      )
      setNotice({ kind: 'success', text: `Trip "${updated.name}" updated.` })
      return true
    } catch (error) {
      setNotice({ kind: 'error', text: toErrorMessage(error) })
      return false
    } finally {
      setSavingTravelPlan(false)
    }
  }

  async function handleDeleteTravelPlan(travelPlanId: string) {
    if (!token) {
      return
    }

    setNotice(null)
    try {
      await apiRequest<void>(`/api/travel-plans/${travelPlanId}`, {
        method: 'DELETE',
        token,
      })
      setTravelPlans((current) => current.filter((item) => item.id !== travelPlanId))
      setNotice({ kind: 'success', text: 'Trip deleted.' })
    } catch (error) {
      setNotice({ kind: 'error', text: toErrorMessage(error) })
    }
  }

  async function handleAcknowledgeAlert(alertId: string) {
    if (!token) {
      return
    }

    setBusyAlertId(alertId)
    setNotice(null)

    try {
      await apiRequest<AlertEvent>(`/api/alerts/${alertId}/acknowledge`, {
        method: 'POST',
        token,
      })
      setNotice({ kind: 'success', text: 'Alert acknowledged.' })
      if (me) {
        await refreshData(token, me)
      }
    } catch (error) {
      setNotice({ kind: 'error', text: toErrorMessage(error) })
    } finally {
      setBusyAlertId(null)
    }
  }

  async function handleAcknowledgeAllAlerts() {
    if (!token || !me) {
      return
    }

    const sentAlerts = alerts.filter((alert) => alert.status === 'SENT')
    if (sentAlerts.length === 0) {
      return
    }

    setLoadingData(true)
    setNotice(null)
    try {
      await Promise.all(
        sentAlerts.map((alert) =>
          apiRequest<AlertEvent>(`/api/alerts/${alert.id}/acknowledge`, {
            method: 'POST',
            token,
          }),
        ),
      )
      setNotice({ kind: 'success', text: `Acknowledged ${sentAlerts.length} alerts.` })
      await refreshData(token, me)
    } catch (error) {
      setNotice({ kind: 'error', text: toErrorMessage(error) })
    } finally {
      setLoadingData(false)
    }
  }

  async function handleSaveProfile(event: FormEvent<HTMLFormElement>): Promise<boolean> {
    event.preventDefault()
    if (!token) {
      return false
    }

    setSavingProfile(true)
    setNotice(null)

    try {
      const updated = await apiRequest<UserAccount>('/api/users/me', {
        method: 'PUT',
        token,
        body: profileForm,
      })
      setMe(updated)
      setNotice({ kind: 'success', text: 'Profile updated.' })
      return true
    } catch (error) {
      setNotice({ kind: 'error', text: toErrorMessage(error) })
      return false
    } finally {
      setSavingProfile(false)
    }
  }

  async function handleChangePassword(event: FormEvent<HTMLFormElement>): Promise<boolean> {
    event.preventDefault()
    if (!token) {
      return false
    }

    setSavingProfile(true)
    setNotice(null)

    try {
      await apiRequest<UserAccount>('/api/users/me/change-password', {
        method: 'POST',
        token,
        body: passwordForm,
      })
      setPasswordForm(initialPasswordForm)
      setNotice({ kind: 'success', text: 'Password updated successfully.' })
      return true
    } catch (error) {
      setNotice({ kind: 'error', text: toErrorMessage(error) })
      return false
    } finally {
      setSavingProfile(false)
    }
  }

  async function handleSaveNotificationPreference(preferences: {
    enabledChannels: Array<'EMAIL' | 'SMS' | 'PUSH'>
    preferredChannel: 'EMAIL' | 'SMS' | 'PUSH'
    fallbackStrategy: 'FIRST_SUCCESS' | 'FAIL_FAST'
  }): Promise<boolean> {
    if (!token) {
      return false
    }
    setSavingProfile(true)
    setNotice(null)
    try {
      const saved = await apiRequest<UserNotificationPreference>('/api/users/me/notification-preferences', {
        method: 'PUT',
        token,
        body: preferences,
      })
      setNotificationPreference(saved)
      setNotice({ kind: 'success', text: 'Delivery preferences updated.' })
      return true
    } catch (error) {
      setNotice({ kind: 'error', text: toErrorMessage(error) })
      return false
    } finally {
      setSavingProfile(false)
    }
  }

  async function handleStartCheckout(plan: BillingPlan): Promise<boolean> {
    if (!token) {
      return false
    }

    setCheckoutPlan(plan)
    setNotice(null)

    try {
      const session = await apiRequest<BillingCheckoutSession>('/api/billing/checkout-session', {
        method: 'POST',
        token,
        body: { plan },
      })
      window.location.assign(session.url)
      return true
    } catch (error) {
      setNotice({ kind: 'error', text: toErrorMessage(error) })
      return false
    } finally {
      setCheckoutPlan(null)
    }
  }

  async function handleOpenBillingPortal(): Promise<boolean> {
    if (!token) {
      return false
    }

    setOpeningBillingPortal(true)
    setNotice(null)

    try {
      const session = await apiRequest<BillingCheckoutSession>('/api/billing/portal-session', {
        method: 'POST',
        token,
      })
      window.location.assign(session.url)
      return true
    } catch (error) {
      setNotice({ kind: 'error', text: toErrorMessage(error) })
      return false
    } finally {
      setOpeningBillingPortal(false)
    }
  }

  async function handleChangePlan(plan: BillingPlan): Promise<boolean> {
    if (!token) {
      return false
    }

    setChangingPlan(plan)
    setNotice(null)

    try {
      const updatedStatus = await apiRequest<BillingStatus>('/api/billing/change-plan', {
        method: 'POST',
        token,
        body: { plan },
      })
      setBillingStatus(updatedStatus)
      if (me) {
        await refreshData(token, me)
      }
      setNotice({
        kind: 'success',
        text:
          plan === 'FREE'
            ? 'Plan downgraded to The Basics.'
            : `Plan updated to ${plan === 'PRO' ? 'The Globetrotter' : 'The Family Plan'}.`,
      })
      return true
    } catch (error) {
      setNotice({ kind: 'error', text: toErrorMessage(error) })
      return false
    } finally {
      setChangingPlan(null)
    }
  }

  async function handleDeleteAccount(): Promise<boolean> {
    if (!token) {
      return false
    }

    setDeletingAccount(true)
    setNotice(null)

    try {
      await apiRequest<void>('/api/users/me', {
        method: 'DELETE',
        token,
      })
      logout()
      return true
    } catch (error) {
      setNotice({ kind: 'error', text: toErrorMessage(error) })
      return false
    } finally {
      setDeletingAccount(false)
    }
  }

  async function handleAdminAction(userId: string, action: 'suspend' | 'reactivate' | 'force-password-reset') {
    if (!token) {
      return
    }

    const busyKey = `${action}:${userId}`
    setBusyAdminAction(busyKey)
    setNotice(null)

    try {
      await apiRequest<UserAccount>(`/api/admin/users/${userId}/${action}`, {
        method: 'POST',
        token,
      })
      if (me) {
        await refreshData(token, me)
      }
      if (action === 'suspend') {
        setNotice({ kind: 'success', text: `Suspended ${userId}.` })
      } else if (action === 'reactivate') {
        setNotice({ kind: 'success', text: `Reactivated ${userId}.` })
      } else {
        setNotice({ kind: 'success', text: `Forced password reset for ${userId}.` })
      }
    } catch (error) {
      setNotice({ kind: 'error', text: toErrorMessage(error) })
    } finally {
      setBusyAdminAction(null)
    }
  }

  async function handleAdminJobRun(job: AdminJobKey): Promise<boolean> {
    if (!token) {
      return false
    }

    setBusyAdminJob(job)
    setNotice(null)

    try {
      const response = await apiRequest<JobRunResponse>(`/api/admin/jobs/${job}`, {
        method: 'POST',
        token,
      })
      setAdminJobResults((current) => ({ ...current, [job]: response }))
      setNotice({
        kind: 'success',
        text: response.message || `${response.jobName || job} completed successfully.`,
      })
      if (me) {
        await refreshData(token, me)
      }
      return true
    } catch (error) {
      setNotice({ kind: 'error', text: toErrorMessage(error) })
      return false
    } finally {
      setBusyAdminJob(null)
    }
  }

  const refresh = useCallback(async () => {
    if (!token || !me) {
      return
    }
    await refreshData(token, me)
  }, [token, me, refreshData])

  return {
    token,
    me,
    notice,
    setNotice,
    isAdmin,
    canSubmitCriteria,

    loginState,
    setLoginState,
    magicLinkState,
    setMagicLinkState,
    registerState,
    setRegisterState,
    verifyState,
    setVerifyState,
    latestVerification,

    forgotUsernameState,
    setForgotUsernameState,
    forgotPasswordState,
    setForgotPasswordState,
    usernameRecoveryMeta,
    passwordRecoveryMeta,
    magicLinkMeta,
    usernameRetryAfterSeconds,
    passwordRetryAfterSeconds,

    criteria,
    alerts,
    currentWeather,
    notificationPreference,
    billingStatus,
    adminUsers,
    adminJobResults,
    observationHistory,
    dailyForecast,
    hourlyForecast,
    nwsProducts,
    travelPlans,

    profileForm,
    setProfileForm,
    passwordForm,
    setPasswordForm,
    criteriaForm,
    setCriteriaForm,

    loadingAuth,
    confirmingMagicLink,
    loadingData,
    savingCriteria,
    savingProfile,
    savingTravelPlan,
    loadingBilling,
    checkoutPlan,
    changingPlan,
    openingBillingPortal,
    deletingAccount,
    busyAlertId,
    busyCriteriaId,
    busyAdminAction,
    busyAdminJob,

    handleLogin,
    handleMagicLinkRequest,
    handleMagicLinkConfirm,
    handleRegister,
    handleVerifyEmail,
    handleResendVerification,
    handleForgotUsernameRequest,
    handleForgotUsernameConfirm,
    handleForgotPasswordRequest,
    handleForgotPasswordConfirm,
    handleCreateCriteria,
    handleDeleteCriteria,
    handleToggleCriteriaEnabled,
    handleCreateTravelPlan,
    handleUpdateTravelPlan,
    handleDeleteTravelPlan,
    handleAcknowledgeAlert,
    handleAcknowledgeAllAlerts,
    handleSaveProfile,
    handleChangePassword,
    handleSaveNotificationPreference,
    handleStartCheckout,
    handleChangePlan,
    handleOpenBillingPortal,
    handleDeleteAccount,
    handleAdminAction,
    handleAdminJobRun,

    refresh,
    logout,
  }
}

export type WeatherAppState = ReturnType<typeof useWeatherAppState>
