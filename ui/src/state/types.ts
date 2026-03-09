export const STORAGE_KEY = 'weather-alert-ui.token'
export const DEFAULT_LAT = '28.5383'
export const DEFAULT_LON = '-81.3792'

export type NoticeKind = 'success' | 'error' | 'info'
export type RuleType =
  | 'TEMP_BELOW'
  | 'TEMP_ABOVE'
  | 'WIND'
  | 'RAIN'
  | 'HUMIDITY_ABOVE'
  | 'HUMIDITY_BELOW'
  | 'DEW_POINT_ABOVE'
  | 'DEW_POINT_BELOW'
  | 'WIND_GUST'
  | 'SKY_COVER_ABOVE'
  | 'SKY_COVER_BELOW'
  | 'RIVER_STAGE_ABOVE'
  | 'RIVER_STAGE_BELOW'
  | 'RIVER_FLOOD_CATEGORY'

export interface NoticeState {
  kind: NoticeKind
  text: string
}

export interface LoginState {
  username: string
  password: string
}

export interface RegisterState {
  username: string
  password: string
  email: string
  name: string
  phoneNumber: string
}

export interface VerifyState {
  userId: string
  verificationId: string
  token: string
}

export interface ForgotUsernameState {
  email: string
  recoveryId: string
  code: string
}

export interface ForgotPasswordState {
  usernameOrEmail: string
  recoveryId: string
  code: string
  newPassword: string
}

export interface CriteriaFormState {
  name: string
  location: string
  latitude: string
  longitude: string
  threshold: string
  ruleType: RuleType
  temperatureUnit: 'F' | 'C'
  riverGaugeId: string
  riverFloodCategoryThreshold: 'ACTION' | 'MINOR' | 'MODERATE' | 'MAJOR'
  gaugeSearchRadiusKm: string
  monitorCurrent: boolean
  monitorForecast: boolean
  forecastWindowHours: string
  oncePerEvent: boolean
  rearmWindowMinutes: string
}

export interface ProfileFormState {
  name: string
  phoneNumber: string
}

export interface PasswordFormState {
  currentPassword: string
  newPassword: string
  confirmNewPassword: string
}

export const initialLogin: LoginState = {
  username: '',
  password: '',
}

export const initialRegister: RegisterState = {
  username: '',
  password: '',
  email: '',
  name: '',
  phoneNumber: '',
}

export const initialVerify: VerifyState = {
  userId: '',
  verificationId: '',
  token: '',
}

export const initialForgotUsername: ForgotUsernameState = {
  email: '',
  recoveryId: '',
  code: '',
}

export const initialForgotPassword: ForgotPasswordState = {
  usernameOrEmail: '',
  recoveryId: '',
  code: '',
  newPassword: '',
}

export const initialCriteriaForm: CriteriaFormState = {
  name: 'Bring a Jacket',
  location: 'Orlando',
  latitude: DEFAULT_LAT,
  longitude: DEFAULT_LON,
  threshold: '60',
  ruleType: 'TEMP_BELOW',
  temperatureUnit: 'F',
  riverGaugeId: '',
  riverFloodCategoryThreshold: 'ACTION',
  gaugeSearchRadiusKm: '80',
  monitorCurrent: true,
  monitorForecast: true,
  forecastWindowHours: '48',
  oncePerEvent: true,
  rearmWindowMinutes: '240',
}

export const initialPasswordForm: PasswordFormState = {
  currentPassword: '',
  newPassword: '',
  confirmNewPassword: '',
}
