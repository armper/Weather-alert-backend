export type TemperatureDirection = 'ABOVE' | 'BELOW'
export type TemperatureUnit = 'F' | 'C'
export type RainThresholdType = 'PROBABILITY' | 'AMOUNT'
export type AlertStatus = 'PENDING' | 'SENT' | 'ACKNOWLEDGED' | 'EXPIRED'
export type UserApprovalStatus = 'PENDING_APPROVAL' | 'ACTIVE' | 'REJECTED'

export interface ProblemFieldError {
  field: string
  message: string
}

export interface ProblemDetails {
  title?: string
  detail?: string
  status?: number
  instance?: string
  errorCode?: string
  errors?: ProblemFieldError[]
}

export interface AuthTokenResponse {
  accessToken: string
  tokenType: 'Bearer'
  expiresIn: number
}

export interface UserAccount {
  id: string
  email: string
  phoneNumber?: string
  name?: string
  role: string
  approvalStatus: UserApprovalStatus
  emailVerified: boolean
  approvedAt?: string
  createdAt?: string
  updatedAt?: string
}

export interface ChannelVerification {
  id: string
  channel: 'EMAIL' | 'SMS' | 'PUSH'
  destination: string
  status: 'PENDING_VERIFICATION' | 'VERIFIED' | 'EXPIRED'
  tokenExpiresAt?: string
  verifiedAt?: string
  verificationToken?: string
}

export interface RegisterUserResponse {
  account: UserAccount
  emailVerification?: ChannelVerification
}

export interface AlertCriteria {
  id: string
  name?: string
  userId: string
  location?: string
  latitude?: number
  longitude?: number
  radiusKm?: number
  temperatureThreshold?: number
  temperatureDirection?: TemperatureDirection
  temperatureUnit?: TemperatureUnit
  maxWindSpeed?: number
  rainThreshold?: number
  rainThresholdType?: RainThresholdType
  monitorCurrent?: boolean
  monitorForecast?: boolean
  forecastWindowHours?: number
  oncePerEvent?: boolean
  rearmWindowMinutes?: number
  enabled?: boolean
}

export interface AlertEvent {
  id: string
  userId: string
  criteriaId?: string
  eventKey?: string
  reason?: string
  eventType?: string
  severity?: string
  headline?: string
  description?: string
  location?: string
  conditionSource?: string
  conditionOnset?: string
  conditionExpires?: string
  conditionTemperatureC?: number
  conditionPrecipitationProbability?: number
  conditionPrecipitationAmount?: number
  alertTime?: string
  status?: AlertStatus
  sentAt?: string
  acknowledgedAt?: string
  expiredAt?: string
}

export interface WeatherCondition {
  id: string
  location?: string
  eventType?: string
  headline?: string
  description?: string
  temperature?: number
  windSpeed?: number
  precipitationProbability?: number
  precipitationAmount?: number
  humidity?: number
  timestamp?: string
}

export interface UserNotificationPreference {
  userId: string
  enabledChannels: Array<'EMAIL' | 'SMS' | 'PUSH'>
  preferredChannel: 'EMAIL' | 'SMS' | 'PUSH'
  fallbackStrategy: 'FIRST_SUCCESS' | 'FAIL_FAST'
}

export type PendingUser = UserAccount
