export type TemperatureDirection = 'ABOVE' | 'BELOW'
export type ComparisonDirection = 'ABOVE' | 'BELOW'
export type TemperatureUnit = 'F' | 'C'
export type RainThresholdType = 'PROBABILITY' | 'AMOUNT'
export type FloodCategory = 'ACTION' | 'MINOR' | 'MODERATE' | 'MAJOR'
export type AlertStatus = 'PENDING' | 'SENT' | 'ACKNOWLEDGED' | 'EXPIRED'
export type UserApprovalStatus = 'PENDING_APPROVAL' | 'ACTIVE' | 'SUSPENDED' | 'REJECTED'
export type BillingPlan = 'FREE' | 'PLUS' | 'PRO'

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
  passwordResetRequired?: boolean
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

export interface RecoveryRequestResponse {
  message: string
  recoveryId?: string
  codeExpiresAt?: string
  recoveryCode?: string
  retryAfterSeconds?: number
}

export interface UsernameRecoveryResponse {
  message: string
  username: string
}

export interface MessageResponse {
  message: string
}

export interface AlertCriteria {
  id: string
  name?: string
  userId: string
  location?: string
  latitude?: number
  longitude?: number
  radiusKm?: number
  eventType?: string
  minSeverity?: string
  maxTemperature?: number
  minTemperature?: number
  temperatureThreshold?: number
  temperatureDirection?: TemperatureDirection
  temperatureUnit?: TemperatureUnit
  maxWindSpeed?: number
  maxPrecipitation?: number
  rainThreshold?: number
  rainThresholdType?: RainThresholdType
  humidityThreshold?: number
  humidityDirection?: ComparisonDirection
  dewPointThreshold?: number
  dewPointDirection?: ComparisonDirection
  windGustThreshold?: number
  skyCoverThreshold?: number
  skyCoverDirection?: ComparisonDirection
  riverGaugeId?: string
  riverStageThreshold?: number
  riverStageDirection?: ComparisonDirection
  riverFloodCategoryThreshold?: FloodCategory
  monitorCurrent?: boolean
  monitorForecast?: boolean
  forecastWindowHours?: number
  oncePerEvent?: boolean
  rearmWindowMinutes?: number
  enabled?: boolean
  createdAt?: string
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
  conditionHumidity?: number
  conditionDewPointC?: number
  conditionWindGust?: number
  conditionSkyCover?: number
  conditionRiverGaugeId?: string
  conditionRiverObservedStage?: number
  conditionRiverForecastStage?: number
  conditionRiverFloodStage?: number
  conditionRiverActionStage?: number
  conditionRiverObservedCategory?: string
  conditionRiverForecastCategory?: string
  conditionRiverStageUnit?: string
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
  dewPoint?: number
  windGust?: number
  skyCover?: number
  riverGaugeId?: string
  riverObservedStage?: number
  riverForecastStage?: number
  riverFloodStage?: number
  riverActionStage?: number
  riverObservedCategory?: string
  riverForecastCategory?: string
  riverStageUnit?: string
  riverDistanceKm?: number
  timestamp?: string
}

export interface UserNotificationPreference {
  userId: string
  enabledChannels: Array<'EMAIL' | 'SMS' | 'PUSH'>
  preferredChannel: 'EMAIL' | 'SMS' | 'PUSH'
  fallbackStrategy: 'FIRST_SUCCESS' | 'FAIL_FAST'
}

export interface BillingStatus {
  userId: string
  plan: BillingPlan
  paidPlan: boolean
  maxActiveAlerts: number
  adSponsoredEmails: boolean
  stripeCustomerId?: string
  stripeSubscriptionId?: string
  stripePriceId?: string
  stripeSubscriptionStatus?: string
  stripeCurrentPeriodEnd?: string
  activeSubscription: boolean
}

export interface BillingCheckoutSession {
  sessionId: string
  url: string
}
