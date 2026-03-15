import { useMemo, type ReactNode } from 'react'
import {
  ActionStateContext,
  AppStateContextValue,
  AsyncStateContext,
  AuthStateContext,
  DataStateContext,
  FormStateContext,
  NoticeStateContext,
  SessionStateContext,
} from './AppStateContextValue'
import { useWeatherAppState } from './useWeatherAppState'

export function AppStateProvider({ children }: { children: ReactNode }) {
  const state = useWeatherAppState()

  const sessionState = useMemo(
    () => ({
      token: state.token,
      me: state.me,
      isAdmin: state.isAdmin,
      initialDataLoading: state.initialDataLoading,
      refresh: state.refresh,
      logout: state.logout,
    }),
    [state.initialDataLoading, state.isAdmin, state.logout, state.me, state.refresh, state.token],
  )

  const noticeState = useMemo(
    () => ({
      notice: state.notice,
      setNotice: state.setNotice,
    }),
    [state.notice, state.setNotice],
  )

  const authState = useMemo(
    () => ({
      loadingAuth: state.loadingAuth,
      confirmingMagicLink: state.confirmingMagicLink,
      loginState: state.loginState,
      setLoginState: state.setLoginState,
      magicLinkState: state.magicLinkState,
      setMagicLinkState: state.setMagicLinkState,
      registerState: state.registerState,
      setRegisterState: state.setRegisterState,
      verifyState: state.verifyState,
      setVerifyState: state.setVerifyState,
      latestVerification: state.latestVerification,
      forgotUsernameState: state.forgotUsernameState,
      setForgotUsernameState: state.setForgotUsernameState,
      forgotPasswordState: state.forgotPasswordState,
      setForgotPasswordState: state.setForgotPasswordState,
      usernameRecoveryMeta: state.usernameRecoveryMeta,
      passwordRecoveryMeta: state.passwordRecoveryMeta,
      magicLinkMeta: state.magicLinkMeta,
      usernameRetryAfterSeconds: state.usernameRetryAfterSeconds,
      passwordRetryAfterSeconds: state.passwordRetryAfterSeconds,
      handleLogin: state.handleLogin,
      handleMagicLinkRequest: state.handleMagicLinkRequest,
      handleMagicLinkConfirm: state.handleMagicLinkConfirm,
      handleRegister: state.handleRegister,
      handleVerifyEmail: state.handleVerifyEmail,
      handleResendVerification: state.handleResendVerification,
      handleForgotUsernameRequest: state.handleForgotUsernameRequest,
      handleForgotUsernameConfirm: state.handleForgotUsernameConfirm,
      handleForgotPasswordRequest: state.handleForgotPasswordRequest,
      handleForgotPasswordConfirm: state.handleForgotPasswordConfirm,
    }),
    [
      state.confirmingMagicLink,
      state.forgotPasswordState,
      state.forgotUsernameState,
      state.handleForgotPasswordConfirm,
      state.handleForgotPasswordRequest,
      state.handleForgotUsernameConfirm,
      state.handleForgotUsernameRequest,
      state.handleLogin,
      state.handleMagicLinkConfirm,
      state.handleMagicLinkRequest,
      state.handleRegister,
      state.handleResendVerification,
      state.handleVerifyEmail,
      state.latestVerification,
      state.loadingAuth,
      state.loginState,
      state.magicLinkMeta,
      state.magicLinkState,
      state.passwordRecoveryMeta,
      state.passwordRetryAfterSeconds,
      state.registerState,
      state.setForgotPasswordState,
      state.setForgotUsernameState,
      state.setLoginState,
      state.setMagicLinkState,
      state.setRegisterState,
      state.setVerifyState,
      state.usernameRecoveryMeta,
      state.usernameRetryAfterSeconds,
      state.verifyState,
    ],
  )

  const dataState = useMemo(
    () => ({
      criteria: state.criteria,
      alerts: state.alerts,
      currentWeather: state.currentWeather,
      notificationPreference: state.notificationPreference,
      billingStatus: state.billingStatus,
      adminUsers: state.adminUsers,
      adminJobResults: state.adminJobResults,
      observationHistory: state.observationHistory,
      dailyForecast: state.dailyForecast,
      hourlyForecast: state.hourlyForecast,
      nwsProducts: state.nwsProducts,
      travelPlans: state.travelPlans,
    }),
    [
      state.adminJobResults,
      state.adminUsers,
      state.alerts,
      state.billingStatus,
      state.criteria,
      state.currentWeather,
      state.dailyForecast,
      state.hourlyForecast,
      state.notificationPreference,
      state.nwsProducts,
      state.observationHistory,
      state.travelPlans,
    ],
  )

  const formState = useMemo(
    () => ({
      profileForm: state.profileForm,
      setProfileForm: state.setProfileForm,
      passwordForm: state.passwordForm,
      setPasswordForm: state.setPasswordForm,
      criteriaForm: state.criteriaForm,
      setCriteriaForm: state.setCriteriaForm,
    }),
    [
      state.criteriaForm,
      state.passwordForm,
      state.profileForm,
      state.setCriteriaForm,
      state.setPasswordForm,
      state.setProfileForm,
    ],
  )

  const asyncState = useMemo(
    () => ({
      canSubmitCriteria: state.canSubmitCriteria,
      loadingData: state.loadingData,
      loadingBilling: state.loadingBilling,
      savingCriteria: state.savingCriteria,
      savingProfile: state.savingProfile,
      savingTravelPlan: state.savingTravelPlan,
      checkoutPlan: state.checkoutPlan,
      changingPlan: state.changingPlan,
      openingBillingPortal: state.openingBillingPortal,
      deletingAccount: state.deletingAccount,
      busyAlertId: state.busyAlertId,
      busyCriteriaId: state.busyCriteriaId,
      busyAdminAction: state.busyAdminAction,
      busyAdminJob: state.busyAdminJob,
    }),
    [
      state.busyAdminAction,
      state.busyAdminJob,
      state.busyAlertId,
      state.busyCriteriaId,
      state.canSubmitCriteria,
      state.changingPlan,
      state.checkoutPlan,
      state.deletingAccount,
      state.loadingBilling,
      state.loadingData,
      state.openingBillingPortal,
      state.savingCriteria,
      state.savingProfile,
      state.savingTravelPlan,
    ],
  )

  const actionState = useMemo(
    () => ({
      handleCreateCriteria: state.handleCreateCriteria,
      handleDeleteCriteria: state.handleDeleteCriteria,
      handleToggleCriteriaEnabled: state.handleToggleCriteriaEnabled,
      handleCreateTravelPlan: state.handleCreateTravelPlan,
      handleUpdateTravelPlan: state.handleUpdateTravelPlan,
      handleDeleteTravelPlan: state.handleDeleteTravelPlan,
      handleAcknowledgeAlert: state.handleAcknowledgeAlert,
      handleAcknowledgeAllAlerts: state.handleAcknowledgeAllAlerts,
      handleSaveProfile: state.handleSaveProfile,
      handleChangePassword: state.handleChangePassword,
      handleSaveNotificationPreference: state.handleSaveNotificationPreference,
      handleStartCheckout: state.handleStartCheckout,
      handleChangePlan: state.handleChangePlan,
      handleOpenBillingPortal: state.handleOpenBillingPortal,
      handleDeleteAccount: state.handleDeleteAccount,
      handleAdminAction: state.handleAdminAction,
      handleAdminJobRun: state.handleAdminJobRun,
      loadNwsProduct: state.loadNwsProduct,
    }),
    [
      state.handleAcknowledgeAlert,
      state.handleAcknowledgeAllAlerts,
      state.handleAdminAction,
      state.handleAdminJobRun,
      state.handleChangePassword,
      state.handleChangePlan,
      state.handleCreateCriteria,
      state.handleCreateTravelPlan,
      state.handleDeleteAccount,
      state.handleDeleteCriteria,
      state.handleDeleteTravelPlan,
      state.handleOpenBillingPortal,
      state.handleSaveNotificationPreference,
      state.handleSaveProfile,
      state.handleStartCheckout,
      state.handleToggleCriteriaEnabled,
      state.handleUpdateTravelPlan,
      state.loadNwsProduct,
    ],
  )

  return (
    <AppStateContextValue.Provider value={state}>
      <SessionStateContext.Provider value={sessionState}>
        <NoticeStateContext.Provider value={noticeState}>
          <AuthStateContext.Provider value={authState}>
            <DataStateContext.Provider value={dataState}>
              <FormStateContext.Provider value={formState}>
                <AsyncStateContext.Provider value={asyncState}>
                  <ActionStateContext.Provider value={actionState}>{children}</ActionStateContext.Provider>
                </AsyncStateContext.Provider>
              </FormStateContext.Provider>
            </DataStateContext.Provider>
          </AuthStateContext.Provider>
        </NoticeStateContext.Provider>
      </SessionStateContext.Provider>
    </AppStateContextValue.Provider>
  )
}
