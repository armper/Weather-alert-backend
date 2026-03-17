import { createContext } from 'react'
import type { WeatherAppState } from './useWeatherAppState'

export const AppStateContextValue = createContext<WeatherAppState | null>(null)

export type SessionState = Pick<
  WeatherAppState,
  'token' | 'me' | 'isAdmin' | 'initialDataLoading' | 'refresh' | 'logout'
>

export type NoticeState = Pick<WeatherAppState, 'notice' | 'setNotice'>

export type AuthState = Pick<
  WeatherAppState,
  | 'loadingAuth'
  | 'confirmingMagicLink'
  | 'loginState'
  | 'setLoginState'
  | 'magicLinkState'
  | 'setMagicLinkState'
  | 'registerState'
  | 'setRegisterState'
  | 'verifyState'
  | 'setVerifyState'
  | 'latestVerification'
  | 'forgotUsernameState'
  | 'setForgotUsernameState'
  | 'forgotPasswordState'
  | 'setForgotPasswordState'
  | 'usernameRecoveryMeta'
  | 'passwordRecoveryMeta'
  | 'magicLinkMeta'
  | 'usernameRetryAfterSeconds'
  | 'passwordRetryAfterSeconds'
  | 'handleLogin'
  | 'handleMagicLinkRequest'
  | 'handleMagicLinkConfirm'
  | 'handleRegister'
  | 'handleVerifyEmail'
  | 'handleResendVerification'
  | 'handleForgotUsernameRequest'
  | 'handleForgotUsernameConfirm'
  | 'handleForgotPasswordRequest'
  | 'handleForgotPasswordConfirm'
>

export type DataState = Pick<
  WeatherAppState,
  | 'criteria'
  | 'alerts'
  | 'currentWeather'
  | 'officialAlerts'
  | 'notificationPreference'
  | 'billingStatus'
  | 'adminUsers'
  | 'adminJobResults'
  | 'observationHistory'
  | 'dailyForecast'
  | 'hourlyForecast'
  | 'nwsProducts'
  | 'travelPlans'
>

export type FormState = Pick<
  WeatherAppState,
  | 'profileForm'
  | 'setProfileForm'
  | 'passwordForm'
  | 'setPasswordForm'
  | 'criteriaForm'
  | 'setCriteriaForm'
>

export type AsyncState = Pick<
  WeatherAppState,
  | 'canSubmitCriteria'
  | 'loadingData'
  | 'loadingBilling'
  | 'savingCriteria'
  | 'savingProfile'
  | 'savingTravelPlan'
  | 'checkoutPlan'
  | 'changingPlan'
  | 'openingBillingPortal'
  | 'deletingAccount'
  | 'busyAlertId'
  | 'busyCriteriaId'
  | 'busyAdminAction'
  | 'busyAdminJob'
>

export type ActionState = Pick<
  WeatherAppState,
  | 'handleCreateCriteria'
  | 'handleDeleteCriteria'
  | 'handleToggleCriteriaEnabled'
  | 'handleCreateTravelPlan'
  | 'handleUpdateTravelPlan'
  | 'handleDeleteTravelPlan'
  | 'handleAcknowledgeAlert'
  | 'handleAcknowledgeAllAlerts'
  | 'handleSaveProfile'
  | 'handleChangePassword'
  | 'handleSaveNotificationPreference'
  | 'handleStartCheckout'
  | 'handleChangePlan'
  | 'handleOpenBillingPortal'
  | 'handleDeleteAccount'
  | 'handleAdminAction'
  | 'handleAdminJobRun'
  | 'loadNwsProduct'
>

export const SessionStateContext = createContext<SessionState | null>(null)
export const NoticeStateContext = createContext<NoticeState | null>(null)
export const AuthStateContext = createContext<AuthState | null>(null)
export const DataStateContext = createContext<DataState | null>(null)
export const FormStateContext = createContext<FormState | null>(null)
export const AsyncStateContext = createContext<AsyncState | null>(null)
export const ActionStateContext = createContext<ActionState | null>(null)
