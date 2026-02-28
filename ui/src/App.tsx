import { useCallback, useEffect, useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import { apiRequest, toErrorMessage } from './api'
import type {
  AlertCriteria,
  AlertEvent,
  AuthTokenResponse,
  ChannelVerification,
  PendingUser,
  RegisterUserResponse,
  UserAccount,
  UserNotificationPreference,
  WeatherCondition,
} from './types'
import './App.css'

const STORAGE_KEY = 'weather-alert-ui.token'
const DEFAULT_LAT = '28.5383'
const DEFAULT_LON = '-81.3792'

type NoticeKind = 'success' | 'error' | 'info'
type RuleType = 'TEMP_BELOW' | 'TEMP_ABOVE' | 'WIND' | 'RAIN'

interface NoticeState {
  kind: NoticeKind
  text: string
}

interface LoginState {
  username: string
  password: string
}

interface RegisterState {
  username: string
  password: string
  email: string
  name: string
  phoneNumber: string
}

interface VerifyState {
  userId: string
  verificationId: string
  token: string
}

interface CriteriaFormState {
  name: string
  location: string
  latitude: string
  longitude: string
  threshold: string
  ruleType: RuleType
  temperatureUnit: 'F' | 'C'
  monitorCurrent: boolean
  monitorForecast: boolean
  forecastWindowHours: string
  oncePerEvent: boolean
  rearmWindowMinutes: string
}

interface ProfileFormState {
  name: string
  phoneNumber: string
}

const initialLogin: LoginState = {
  username: '',
  password: '',
}

const initialRegister: RegisterState = {
  username: '',
  password: '',
  email: '',
  name: '',
  phoneNumber: '',
}

const initialVerify: VerifyState = {
  userId: '',
  verificationId: '',
  token: '',
}

const initialCriteriaForm: CriteriaFormState = {
  name: 'Bring a Jacket',
  location: 'Orlando',
  latitude: DEFAULT_LAT,
  longitude: DEFAULT_LON,
  threshold: '60',
  ruleType: 'TEMP_BELOW',
  temperatureUnit: 'F',
  monitorCurrent: true,
  monitorForecast: true,
  forecastWindowHours: '48',
  oncePerEvent: true,
  rearmWindowMinutes: '240',
}

function App() {
  const [token, setToken] = useState<string | null>(() => localStorage.getItem(STORAGE_KEY))
  const [notice, setNotice] = useState<NoticeState | null>(null)

  const [loginState, setLoginState] = useState<LoginState>(initialLogin)
  const [registerState, setRegisterState] = useState<RegisterState>(initialRegister)
  const [verifyState, setVerifyState] = useState<VerifyState>(initialVerify)
  const [latestVerification, setLatestVerification] = useState<ChannelVerification | null>(null)

  const [me, setMe] = useState<UserAccount | null>(null)
  const [criteria, setCriteria] = useState<AlertCriteria[]>([])
  const [alerts, setAlerts] = useState<AlertEvent[]>([])
  const [currentWeather, setCurrentWeather] = useState<WeatherCondition | null>(null)
  const [notificationPreference, setNotificationPreference] = useState<UserNotificationPreference | null>(null)
  const [pendingUsers, setPendingUsers] = useState<PendingUser[]>([])

  const [profileForm, setProfileForm] = useState<ProfileFormState>({ name: '', phoneNumber: '' })
  const [criteriaForm, setCriteriaForm] = useState<CriteriaFormState>(initialCriteriaForm)

  const [loadingAuth, setLoadingAuth] = useState(false)
  const [loadingData, setLoadingData] = useState(false)
  const [savingCriteria, setSavingCriteria] = useState(false)
  const [savingProfile, setSavingProfile] = useState(false)
  const [busyAlertId, setBusyAlertId] = useState<string | null>(null)
  const [busyCriteriaId, setBusyCriteriaId] = useState<string | null>(null)
  const [busyApprovalId, setBusyApprovalId] = useState<string | null>(null)

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

      const [freshAlerts, preferences, weather, adminPending] = await Promise.all([
        apiRequest<AlertEvent[]>(`/api/alerts/user/${account.id}`, { token: activeToken }),
        apiRequest<UserNotificationPreference>('/api/users/me/notification-preferences', { token: activeToken }),
        apiRequest<WeatherCondition>(
          `/api/weather/conditions/current?latitude=${encodeURIComponent(
            freshCriteria[0]?.latitude?.toString() ?? DEFAULT_LAT,
          )}&longitude=${encodeURIComponent(freshCriteria[0]?.longitude?.toString() ?? DEFAULT_LON)}`,
          { token: activeToken },
        ).catch(() => null),
        account.role.includes('ADMIN')
          ? apiRequest<PendingUser[]>('/api/admin/users/pending', { token: activeToken })
          : Promise.resolve([]),
      ])

      setAlerts(
        [...freshAlerts].sort((a, b) => new Date(b.alertTime ?? '').getTime() - new Date(a.alertTime ?? '').getTime()),
      )
      setNotificationPreference(preferences)
      setCurrentWeather(weather)
      setPendingUsers(adminPending)
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
        logout()
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
      return
    }
    void bootstrap(token)
  }, [token, bootstrap])

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
      setNotice({ kind: 'success', text: `Signed in as ${loginState.username}.` })
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
      if (token && me) {
        await refreshData(token, me)
      }
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
      if (token && me) {
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
      if (token && me) {
        await refreshData(token, me)
      }
    } catch (error) {
      setNotice({ kind: 'error', text: toErrorMessage(error) })
    } finally {
      setBusyAlertId(null)
    }
  }

  async function handleSaveProfile(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!token) {
      return
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
    } catch (error) {
      setNotice({ kind: 'error', text: toErrorMessage(error) })
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
      if (token && me) {
        await refreshData(token, me)
      }
    } catch (error) {
      setNotice({ kind: 'error', text: toErrorMessage(error) })
    } finally {
      setBusyApprovalId(null)
    }
  }

  if (!token) {
    return (
      <div className="app-shell">
        <BackgroundArtwork />
        <main className="auth-layout">
          <section className="panel hero-panel">
            <p className="eyebrow">Weather Alert Console</p>
            <h1>Stay ahead of weather changes before plans break.</h1>
            <p className="muted">
              Create personal threshold alerts, verify channels, and track triggered events from one clean dashboard.
            </p>
            <ul className="feature-list">
              <li>Actionable alert naming and threshold presets</li>
              <li>Email verification and account approval workflow</li>
              <li>Real-time trigger timeline with acknowledgement</li>
            </ul>
          </section>

          <section className="panel stack auth-stack">
            <div className="stack-block">
              <h2>Sign in</h2>
              <form onSubmit={handleLogin} className="grid-form">
                <label>
                  Username
                  <input
                    type="text"
                    required
                    value={loginState.username}
                    onChange={(event) =>
                      setLoginState((state) => ({
                        ...state,
                        username: event.target.value,
                      }))
                    }
                  />
                </label>
                <label>
                  Password
                  <input
                    type="password"
                    required
                    value={loginState.password}
                    onChange={(event) =>
                      setLoginState((state) => ({
                        ...state,
                        password: event.target.value,
                      }))
                    }
                  />
                </label>
                <button type="submit" className="primary" disabled={loadingAuth}>
                  {loadingAuth ? 'Signing in...' : 'Get Token & Enter'}
                </button>
              </form>
            </div>

            <div className="divider" />

            <div className="stack-block">
              <h2>Register</h2>
              <form onSubmit={handleRegister} className="grid-form two-col">
                <label>
                  Username
                  <input
                    type="text"
                    minLength={3}
                    required
                    value={registerState.username}
                    onChange={(event) =>
                      setRegisterState((state) => ({
                        ...state,
                        username: event.target.value,
                      }))
                    }
                  />
                </label>
                <label>
                  Password
                  <input
                    type="password"
                    minLength={8}
                    required
                    value={registerState.password}
                    onChange={(event) =>
                      setRegisterState((state) => ({
                        ...state,
                        password: event.target.value,
                      }))
                    }
                  />
                </label>
                <label>
                  Email
                  <input
                    type="email"
                    required
                    value={registerState.email}
                    onChange={(event) =>
                      setRegisterState((state) => ({
                        ...state,
                        email: event.target.value,
                      }))
                    }
                  />
                </label>
                <label>
                  Phone
                  <input
                    type="text"
                    placeholder="+14075551234"
                    value={registerState.phoneNumber}
                    onChange={(event) =>
                      setRegisterState((state) => ({
                        ...state,
                        phoneNumber: event.target.value,
                      }))
                    }
                  />
                </label>
                <label className="full-row">
                  Name
                  <input
                    type="text"
                    value={registerState.name}
                    onChange={(event) =>
                      setRegisterState((state) => ({
                        ...state,
                        name: event.target.value,
                      }))
                    }
                  />
                </label>
                <button type="submit" className="primary full-row" disabled={loadingAuth}>
                  {loadingAuth ? 'Creating account...' : 'Create account'}
                </button>
              </form>
            </div>

            <div className="divider" />

            <div className="stack-block">
              <h2>Verify email</h2>
              <form onSubmit={handleVerifyEmail} className="grid-form">
                <label>
                  Username
                  <input
                    type="text"
                    required
                    value={verifyState.userId}
                    onChange={(event) =>
                      setVerifyState((state) => ({
                        ...state,
                        userId: event.target.value,
                      }))
                    }
                  />
                </label>
                <label>
                  Verification ID
                  <input
                    type="text"
                    required
                    value={verifyState.verificationId}
                    onChange={(event) =>
                      setVerifyState((state) => ({
                        ...state,
                        verificationId: event.target.value,
                      }))
                    }
                  />
                </label>
                <label>
                  Token
                  <input
                    type="text"
                    required
                    value={verifyState.token}
                    onChange={(event) =>
                      setVerifyState((state) => ({
                        ...state,
                        token: event.target.value,
                      }))
                    }
                  />
                </label>
                <div className="button-row">
                  <button type="submit" className="primary" disabled={loadingAuth}>
                    Confirm email
                  </button>
                  <button type="button" className="ghost" onClick={handleResendVerification} disabled={loadingAuth}>
                    Resend token
                  </button>
                </div>
              </form>
              {latestVerification ? (
                <p className="hint">
                  Latest verification: <strong>{latestVerification.id}</strong>
                  {latestVerification.verificationToken ? (
                    <>
                      {' '}
                      | dev token: <code>{latestVerification.verificationToken}</code>
                    </>
                  ) : null}
                </p>
              ) : null}
            </div>

            {notice ? <NoticeBanner notice={notice} /> : null}
          </section>
        </main>
      </div>
    )
  }

  return (
    <div className="app-shell">
      <BackgroundArtwork />
      <header className="topbar">
        <div>
          <p className="eyebrow">Weather Alert Console</p>
          <h1>Control Center</h1>
        </div>
        <div className="topbar-actions">
          <div className="user-chip">
            <span className="chip-role">{me?.role ?? 'ROLE_USER'}</span>
            <span className="chip-id">{me?.id}</span>
          </div>
          <button
            className="ghost"
            onClick={() => {
              if (token && me) {
                void refreshData(token, me)
              }
            }}
            disabled={loadingData}
          >
            {loadingData ? 'Refreshing...' : 'Refresh'}
          </button>
          <button className="ghost danger" onClick={logout}>
            Sign out
          </button>
        </div>
      </header>

      <main className="dashboard-grid">
        <section className="panel span-2">
          <div className="panel-title-row">
            <h2>Create Alert Rule</h2>
            <span className="muted">Clean first-cut UX for daily use</span>
          </div>
          <form className="grid-form create-grid" onSubmit={handleCreateCriteria}>
            <label>
              Alert name
              <input
                type="text"
                required
                maxLength={120}
                value={criteriaForm.name}
                onChange={(event) => setCriteriaForm((state) => ({ ...state, name: event.target.value }))}
              />
            </label>
            <label>
              Location
              <input
                type="text"
                required
                value={criteriaForm.location}
                onChange={(event) => setCriteriaForm((state) => ({ ...state, location: event.target.value }))}
              />
            </label>

            <label>
              Rule type
              <select
                value={criteriaForm.ruleType}
                onChange={(event) => {
                  const next = event.target.value as RuleType
                  setCriteriaForm((state) => ({
                    ...state,
                    ruleType: next,
                    threshold: defaultThreshold(next),
                  }))
                }}
              >
                <option value="TEMP_BELOW">Temperature below</option>
                <option value="TEMP_ABOVE">Temperature above</option>
                <option value="WIND">Wind speed above</option>
                <option value="RAIN">Rain probability at/above</option>
              </select>
            </label>

            <label>
              Threshold
              <input
                type="number"
                required
                value={criteriaForm.threshold}
                onChange={(event) => setCriteriaForm((state) => ({ ...state, threshold: event.target.value }))}
              />
            </label>

            <label>
              Latitude
              <input
                type="number"
                step="0.0001"
                required
                value={criteriaForm.latitude}
                onChange={(event) => setCriteriaForm((state) => ({ ...state, latitude: event.target.value }))}
              />
            </label>
            <label>
              Longitude
              <input
                type="number"
                step="0.0001"
                required
                value={criteriaForm.longitude}
                onChange={(event) => setCriteriaForm((state) => ({ ...state, longitude: event.target.value }))}
              />
            </label>

            <label>
              Temperature unit
              <select
                value={criteriaForm.temperatureUnit}
                onChange={(event) =>
                  setCriteriaForm((state) => ({
                    ...state,
                    temperatureUnit: event.target.value as 'F' | 'C',
                  }))
                }
              >
                <option value="F">Fahrenheit</option>
                <option value="C">Celsius</option>
              </select>
            </label>
            <label>
              Forecast window (hours)
              <input
                type="number"
                min={1}
                max={168}
                value={criteriaForm.forecastWindowHours}
                onChange={(event) =>
                  setCriteriaForm((state) => ({
                    ...state,
                    forecastWindowHours: event.target.value,
                  }))
                }
              />
            </label>
            <label>
              Rearm window (minutes)
              <input
                type="number"
                min={0}
                value={criteriaForm.rearmWindowMinutes}
                onChange={(event) =>
                  setCriteriaForm((state) => ({
                    ...state,
                    rearmWindowMinutes: event.target.value,
                  }))
                }
              />
            </label>

            <div className="toggle-row full-row">
              <label className="checkbox-pill">
                <input
                  type="checkbox"
                  checked={criteriaForm.monitorCurrent}
                  onChange={(event) =>
                    setCriteriaForm((state) => ({
                      ...state,
                      monitorCurrent: event.target.checked,
                    }))
                  }
                />
                Current conditions
              </label>
              <label className="checkbox-pill">
                <input
                  type="checkbox"
                  checked={criteriaForm.monitorForecast}
                  onChange={(event) =>
                    setCriteriaForm((state) => ({
                      ...state,
                      monitorForecast: event.target.checked,
                    }))
                  }
                />
                Forecast conditions
              </label>
              <label className="checkbox-pill">
                <input
                  type="checkbox"
                  checked={criteriaForm.oncePerEvent}
                  onChange={(event) =>
                    setCriteriaForm((state) => ({
                      ...state,
                      oncePerEvent: event.target.checked,
                    }))
                  }
                />
                Notify once per event
              </label>
            </div>

            <button type="submit" className="primary full-row" disabled={!canSubmitCriteria || savingCriteria}>
              {savingCriteria ? 'Saving rule...' : 'Create alert rule'}
            </button>
          </form>
        </section>

        <section className="panel">
          <h2>Current Conditions</h2>
          {currentWeather ? (
            <div className="weather-stack">
              <p className="weather-location">{currentWeather.location ?? 'Selected area'}</p>
              <p className="weather-temp">{formatTemperature(currentWeather.temperature, 'F')}</p>
              <div className="weather-meta">
                <span>Wind: {formatWind(currentWeather.windSpeed)}</span>
                <span>Rain chance: {formatPercent(currentWeather.precipitationProbability)}</span>
              </div>
              <p className="muted small">{currentWeather.headline ?? 'Current NOAA observation'}</p>
            </div>
          ) : (
            <p className="muted">No current weather snapshot available yet.</p>
          )}
        </section>

        <section className="panel span-2">
          <div className="panel-title-row">
            <h2>Active Alert Rules</h2>
            <span className="badge">{criteria.length} active</span>
          </div>
          {criteria.length === 0 ? (
            <p className="muted">No alert rules yet. Create one above.</p>
          ) : (
            <div className="criteria-grid">
              {criteria.map((item) => (
                <article key={item.id} className="criteria-card">
                  <header>
                    <h3>{item.name ?? 'Custom alert'}</h3>
                    <span className="chip-role">{item.enabled === false ? 'Disabled' : 'Enabled'}</span>
                  </header>
                  <p>{describeCriteria(item)}</p>
                  <footer>
                    <span className="muted small">{item.location ?? 'No location'}</span>
                    <button
                      className="ghost danger"
                      disabled={busyCriteriaId === item.id}
                      onClick={() => handleDeleteCriteria(item.id)}
                    >
                      {busyCriteriaId === item.id ? 'Deleting...' : 'Delete'}
                    </button>
                  </footer>
                </article>
              ))}
            </div>
          )}
        </section>

        <section className="panel span-2">
          <div className="panel-title-row">
            <h2>Triggered Alerts</h2>
            <span className="badge">{alerts.length} events</span>
          </div>

          {alerts.length === 0 ? (
            <p className="muted">No triggered events yet.</p>
          ) : (
            <div className="alert-list">
              {alerts.map((item) => (
                <article key={item.id} className="alert-row">
                  <div>
                    <p className="alert-row-title">{item.headline ?? 'Triggered alert'}</p>
                    <p className="muted small">{item.reason ?? 'Rule matched'}</p>
                    <p className="muted small">{formatDate(item.alertTime)}</p>
                  </div>
                  <div className="alert-row-actions">
                    <span className={`status-chip status-${(item.status ?? 'PENDING').toLowerCase()}`}>
                      {item.status ?? 'PENDING'}
                    </span>
                    {item.status === 'SENT' ? (
                      <button
                        className="ghost"
                        disabled={busyAlertId === item.id}
                        onClick={() => handleAcknowledgeAlert(item.id)}
                      >
                        {busyAlertId === item.id ? 'Saving...' : 'Acknowledge'}
                      </button>
                    ) : null}
                  </div>
                </article>
              ))}
            </div>
          )}
        </section>

        <section className="panel">
          <h2>Account</h2>
          <form onSubmit={handleSaveProfile} className="grid-form">
            <label>
              Name
              <input
                type="text"
                value={profileForm.name}
                onChange={(event) => setProfileForm((state) => ({ ...state, name: event.target.value }))}
              />
            </label>
            <label>
              Phone
              <input
                type="text"
                value={profileForm.phoneNumber}
                onChange={(event) => setProfileForm((state) => ({ ...state, phoneNumber: event.target.value }))}
              />
            </label>
            <button type="submit" className="primary" disabled={savingProfile}>
              {savingProfile ? 'Updating...' : 'Update profile'}
            </button>
          </form>

          {notificationPreference ? (
            <div className="prefs-summary">
              <p className="muted small">Delivery channels</p>
              <p>{notificationPreference.enabledChannels.join(', ')}</p>
              <p className="muted small">Preferred: {notificationPreference.preferredChannel}</p>
            </div>
          ) : null}
        </section>

        {isAdmin ? (
          <section className="panel">
            <div className="panel-title-row">
              <h2>Pending Approvals</h2>
              <span className="badge">{pendingUsers.length}</span>
            </div>
            {pendingUsers.length === 0 ? (
              <p className="muted">No pending users right now.</p>
            ) : (
              <div className="alert-list">
                {pendingUsers.map((user) => (
                  <article key={user.id} className="alert-row">
                    <div>
                      <p className="alert-row-title">{user.id}</p>
                      <p className="muted small">{user.email}</p>
                    </div>
                    <button
                      className="primary"
                      disabled={busyApprovalId === user.id}
                      onClick={() => handleApproveUser(user.id)}
                    >
                      {busyApprovalId === user.id ? 'Approving...' : 'Approve'}
                    </button>
                  </article>
                ))}
              </div>
            )}
          </section>
        ) : null}
      </main>

      {notice ? <NoticeBanner notice={notice} /> : null}
    </div>
  )
}

function NoticeBanner({ notice }: { notice: NoticeState }) {
  return (
    <div className={`notice notice-${notice.kind}`} role="status">
      {notice.text}
    </div>
  )
}

function BackgroundArtwork() {
  return (
    <div aria-hidden="true" className="bg-art">
      <span className="blob blob-a" />
      <span className="blob blob-b" />
      <span className="blob blob-c" />
    </div>
  )
}

function defaultThreshold(ruleType: RuleType): string {
  switch (ruleType) {
    case 'TEMP_BELOW':
      return '60'
    case 'TEMP_ABOVE':
      return '90'
    case 'WIND':
      return '25'
    case 'RAIN':
      return '50'
    default:
      return '60'
  }
}

function buildCriteriaPayload(criteriaForm: CriteriaFormState, userId: string) {
  const payload: Record<string, unknown> = {
    userId,
    name: criteriaForm.name.trim(),
    location: criteriaForm.location.trim(),
    latitude: Number(criteriaForm.latitude),
    longitude: Number(criteriaForm.longitude),
    monitorCurrent: criteriaForm.monitorCurrent,
    monitorForecast: criteriaForm.monitorForecast,
    forecastWindowHours: Number(criteriaForm.forecastWindowHours),
    temperatureUnit: criteriaForm.temperatureUnit,
    oncePerEvent: criteriaForm.oncePerEvent,
    rearmWindowMinutes: Number(criteriaForm.rearmWindowMinutes),
  }

  const threshold = Number(criteriaForm.threshold)

  if (criteriaForm.ruleType === 'TEMP_BELOW') {
    payload.temperatureThreshold = threshold
    payload.temperatureDirection = 'BELOW'
  }
  if (criteriaForm.ruleType === 'TEMP_ABOVE') {
    payload.temperatureThreshold = threshold
    payload.temperatureDirection = 'ABOVE'
  }
  if (criteriaForm.ruleType === 'WIND') {
    payload.maxWindSpeed = threshold
  }
  if (criteriaForm.ruleType === 'RAIN') {
    payload.rainThreshold = threshold
    payload.rainThresholdType = 'PROBABILITY'
  }

  return payload
}

function describeCriteria(criteria: AlertCriteria): string {
  if (criteria.temperatureThreshold && criteria.temperatureDirection) {
    return `Temperature ${criteria.temperatureDirection.toLowerCase()} ${formatNumber(
      criteria.temperatureThreshold,
    )} ${criteria.temperatureUnit ?? 'F'}`
  }
  if (criteria.maxWindSpeed) {
    return `Wind speed above ${formatNumber(criteria.maxWindSpeed)} km/h`
  }
  if (criteria.rainThreshold && criteria.rainThresholdType) {
    if (criteria.rainThresholdType === 'PROBABILITY') {
      return `Rain probability at or above ${formatNumber(criteria.rainThreshold)}%`
    }
    return `Rain amount at or above ${formatNumber(criteria.rainThreshold)} mm`
  }
  return 'Custom weather condition'
}

function formatNumber(value?: number | null): string {
  if (value === undefined || value === null || Number.isNaN(value)) {
    return '-'
  }
  return Number(value).toFixed(1).replace(/\.0$/, '')
}

function formatDate(value?: string): string {
  if (!value) {
    return 'Unknown time'
  }

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }

  return date.toLocaleString()
}

function formatTemperature(celsius?: number, target: 'F' | 'C' = 'F'): string {
  if (celsius === undefined || celsius === null || Number.isNaN(celsius)) {
    return '--'
  }

  if (target === 'C') {
    return `${formatNumber(celsius)} C`
  }

  return `${formatNumber((celsius * 9) / 5 + 32)} F`
}

function formatWind(kmh?: number): string {
  if (kmh === undefined || kmh === null || Number.isNaN(kmh)) {
    return '--'
  }
  return `${formatNumber(kmh)} km/h`
}

function formatPercent(value?: number): string {
  if (value === undefined || value === null || Number.isNaN(value)) {
    return '--'
  }
  return `${formatNumber(value)}%`
}

export default App
