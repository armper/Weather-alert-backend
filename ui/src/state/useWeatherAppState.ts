import { useCallback, useEffect, useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import { apiRequest, toErrorMessage } from '../api'
import type {
  AlertCriteria,
  AlertEvent,
  AuthTokenResponse,
  ChannelVerification,
  MessageResponse,
  PendingUser,
  RecoveryRequestResponse,
  RegisterUserResponse,
  UserAccount,
  UserNotificationPreference,
  UsernameRecoveryResponse,
  WeatherCondition,
} from '../types'
import { buildCriteriaPayload } from '../lib/criteria'
import {
  DEFAULT_LAT,
  DEFAULT_LON,
  initialCriteriaForm,
  initialForgotPassword,
  initialForgotUsername,
  initialLogin,
  initialPasswordForm,
  initialRegister,
  initialVerify,
  STORAGE_KEY,
  type CriteriaFormState,
  type ForgotPasswordState,
  type ForgotUsernameState,
  type LoginState,
  type NoticeState,
  type PasswordFormState,
  type ProfileFormState,
  type RegisterState,
  type VerifyState,
} from './types'

export function useWeatherAppState() {
  const [token, setToken] = useState<string | null>(() => localStorage.getItem(STORAGE_KEY))
  const [notice, setNotice] = useState<NoticeState | null>(null)

  const [loginState, setLoginState] = useState<LoginState>(initialLogin)
  const [registerState, setRegisterState] = useState<RegisterState>(initialRegister)
  const [verifyState, setVerifyState] = useState<VerifyState>(initialVerify)
  const [latestVerification, setLatestVerification] = useState<ChannelVerification | null>(null)
  const [forgotUsernameState, setForgotUsernameState] = useState<ForgotUsernameState>(initialForgotUsername)
  const [forgotPasswordState, setForgotPasswordState] = useState<ForgotPasswordState>(initialForgotPassword)
  const [usernameRecoveryMeta, setUsernameRecoveryMeta] = useState<RecoveryRequestResponse | null>(null)
  const [passwordRecoveryMeta, setPasswordRecoveryMeta] = useState<RecoveryRequestResponse | null>(null)

  const [me, setMe] = useState<UserAccount | null>(null)
  const [criteria, setCriteria] = useState<AlertCriteria[]>([])
  const [alerts, setAlerts] = useState<AlertEvent[]>([])
  const [currentWeather, setCurrentWeather] = useState<WeatherCondition | null>(null)
  const [notificationPreference, setNotificationPreference] = useState<UserNotificationPreference | null>(null)
  const [pendingUsers, setPendingUsers] = useState<PendingUser[]>([])
  const [adminUsers, setAdminUsers] = useState<UserAccount[]>([])

  const [profileForm, setProfileForm] = useState<ProfileFormState>({ name: '', phoneNumber: '' })
  const [passwordForm, setPasswordForm] = useState<PasswordFormState>(initialPasswordForm)
  const [criteriaForm, setCriteriaForm] = useState<CriteriaFormState>(initialCriteriaForm)

  const [loadingAuth, setLoadingAuth] = useState(false)
  const [loadingData, setLoadingData] = useState(false)
  const [savingCriteria, setSavingCriteria] = useState(false)
  const [savingProfile, setSavingProfile] = useState(false)
  const [busyAlertId, setBusyAlertId] = useState<string | null>(null)
  const [busyCriteriaId, setBusyCriteriaId] = useState<string | null>(null)
  const [busyApprovalId, setBusyApprovalId] = useState<string | null>(null)
  const [busyAdminAction, setBusyAdminAction] = useState<string | null>(null)
  const [usernameRetryAfterSeconds, setUsernameRetryAfterSeconds] = useState<number>(0)
  const [passwordRetryAfterSeconds, setPasswordRetryAfterSeconds] = useState<number>(0)

  const isAdmin = useMemo(() => Boolean(me?.role?.includes('ADMIN')), [me?.role])

  const canSubmitCriteria = useMemo(() => {
    if (!criteriaForm.name.trim() || !criteriaForm.location.trim()) {
      return false
    }
    if (!criteriaForm.latitude.trim() || !criteriaForm.longitude.trim()) {
      return false
    }
    return !Number.isNaN(Number(criteriaForm.threshold))
  }, [criteriaForm])

  const refreshData = useCallback(async (activeToken: string, account: UserAccount) => {
    setLoadingData(true)
    try {
      const freshCriteria = await apiRequest<AlertCriteria[]>(`/api/criteria/user/${account.id}`, {
        token: activeToken,
      })
      setCriteria(freshCriteria)

      const [freshAlerts, preferences, weather, adminAccounts] = await Promise.all([
        apiRequest<AlertEvent[]>(`/api/alerts/user/${account.id}`, { token: activeToken }),
        apiRequest<UserNotificationPreference>('/api/users/me/notification-preferences', { token: activeToken }),
        apiRequest<WeatherCondition>(
          `/api/weather/conditions/current?latitude=${encodeURIComponent(
            freshCriteria[0]?.latitude?.toString() ?? DEFAULT_LAT,
          )}&longitude=${encodeURIComponent(freshCriteria[0]?.longitude?.toString() ?? DEFAULT_LON)}`,
          { token: activeToken },
        ).catch(() => null),
        account.role.includes('ADMIN')
          ? apiRequest<UserAccount[]>('/api/admin/users', { token: activeToken })
          : Promise.resolve([]),
      ])

      setAlerts(
        [...freshAlerts].sort((a, b) => new Date(b.alertTime ?? '').getTime() - new Date(a.alertTime ?? '').getTime()),
      )
      setNotificationPreference(preferences)
      setCurrentWeather(weather)
      setAdminUsers(adminAccounts)
      setPendingUsers(adminAccounts.filter((user) => user.approvalStatus === 'PENDING_APPROVAL'))
    } catch (error) {
      setNotice({ kind: 'error', text: toErrorMessage(error) })
    } finally {
      setLoadingData(false)
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
      setPendingUsers([])
      setAdminUsers([])
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

  function persistToken(accessToken: string) {
    localStorage.setItem(STORAGE_KEY, accessToken)
    setToken(accessToken)
  }

  function logout() {
    localStorage.removeItem(STORAGE_KEY)
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
        token: verification?.verificationToken ?? '',
      })

      setNotice({
        kind: 'success',
        text: `Account ${response.account.id} created. Verify email first, then ask admin approval.`,
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
    setLoadingAuth(true)
    setNotice(null)

    try {
      await apiRequest<UserAccount>('/api/auth/register/verify-email', {
        method: 'POST',
        body: verifyState,
      })
      setNotice({
        kind: 'success',
        text: 'Email verification successful. You can now log in after admin approval.',
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
        verificationId: verification.id,
        token: verification.verificationToken ?? current.token,
      }))
      setNotice({ kind: 'success', text: `Verification resent for ${username}.` })
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
      setPasswordRetryAfterSeconds(response.retryAfterSeconds ?? 0)
      setNotice({ kind: 'success', text: response.message })
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
      return
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
      setNotice({ kind: 'success', text: `Created alert "${criteriaForm.name}".` })
      await refreshData(token, me)
    } catch (error) {
      setNotice({ kind: 'error', text: toErrorMessage(error) })
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

  async function handleApproveUser(userId: string) {
    if (!token) {
      return
    }

    setBusyApprovalId(userId)
    setNotice(null)

    try {
      await apiRequest<UserAccount>(`/api/admin/users/${userId}/approve`, {
        method: 'POST',
        token,
      })
      setNotice({ kind: 'success', text: `Approved ${userId}.` })
      if (me) {
        await refreshData(token, me)
      }
    } catch (error) {
      setNotice({ kind: 'error', text: toErrorMessage(error) })
    } finally {
      setBusyApprovalId(null)
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
    usernameRetryAfterSeconds,
    passwordRetryAfterSeconds,

    criteria,
    alerts,
    currentWeather,
    notificationPreference,
    pendingUsers,
    adminUsers,

    profileForm,
    setProfileForm,
    passwordForm,
    setPasswordForm,
    criteriaForm,
    setCriteriaForm,

    loadingAuth,
    loadingData,
    savingCriteria,
    savingProfile,
    busyAlertId,
    busyCriteriaId,
    busyApprovalId,
    busyAdminAction,

    handleLogin,
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
    handleAcknowledgeAlert,
    handleAcknowledgeAllAlerts,
    handleSaveProfile,
    handleChangePassword,
    handleSaveNotificationPreference,
    handleApproveUser,
    handleAdminAction,

    refresh,
    logout,
  }
}

export type WeatherAppState = ReturnType<typeof useWeatherAppState>
