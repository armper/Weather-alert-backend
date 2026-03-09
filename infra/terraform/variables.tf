variable "project_id" {
  type        = string
  description = "Google Cloud project ID."
}

variable "region" {
  type        = string
  description = "Primary Google Cloud region."
  default     = "us-east1"
}

variable "service_name" {
  type        = string
  description = "Cloud Run service name."
  default     = "weather-alert-backend"
}

variable "artifact_registry_repository_id" {
  type        = string
  description = "Artifact Registry Docker repository ID."
  default     = "weather-alert-backend"
}

variable "image" {
  type        = string
  description = "Container image to run. Leave empty to use the repository latest tag."
  default     = ""
}

variable "database_name" {
  type        = string
  description = "PostgreSQL database name."
  default     = "weather_alerts"
}

variable "database_user" {
  type        = string
  description = "PostgreSQL username."
  default     = "weather_app"
}

variable "cloud_sql_instance_name" {
  type        = string
  description = "Cloud SQL instance name."
  default     = "weather-alert-db"
}

variable "cloud_sql_database_version" {
  type        = string
  description = "Cloud SQL PostgreSQL major version."
  default     = "POSTGRES_15"
}

variable "cloud_sql_tier" {
  type        = string
  description = "Cloud SQL machine tier."
  default     = "db-custom-1-3840"
}

variable "cloud_sql_availability_type" {
  type        = string
  description = "Cloud SQL availability type."
  default     = "ZONAL"
}

variable "cloud_sql_disk_size_gb" {
  type        = number
  description = "Cloud SQL initial disk size in GB."
  default     = 20
}

variable "cloud_sql_deletion_protection" {
  type        = bool
  description = "Whether to enable deletion protection on the Cloud SQL instance."
  default     = true
}

variable "frontend_base_url" {
  type        = string
  description = "Frontend base URL used for recovery links."
}

variable "notification_email_provider" {
  type        = string
  description = "Notification email provider."
  default     = "smtp"
}

variable "stripe_enabled" {
  type        = bool
  description = "Whether Stripe billing endpoints should be enabled."
  default     = false
}

variable "stripe_price_id" {
  type        = string
  description = "Stripe recurring price ID used for checkout."
  default     = ""
}

variable "stripe_success_url" {
  type        = string
  description = "Optional Stripe checkout success URL override."
  default     = ""
}

variable "stripe_cancel_url" {
  type        = string
  description = "Optional Stripe checkout cancel URL override."
  default     = ""
}

variable "mail_host" {
  type        = string
  description = "SMTP host."
}

variable "mail_port" {
  type        = string
  description = "SMTP port."
  default     = "587"
}

variable "mail_username" {
  type        = string
  description = "SMTP username."
}

variable "mail_from_address" {
  type        = string
  description = "From address for notification emails."
}

variable "user_username" {
  type        = string
  description = "Built-in USER account username."
  default     = "weather-user"
}

variable "admin_username" {
  type        = string
  description = "Built-in ADMIN account username."
  default     = "weather-admin"
}

variable "allow_unauthenticated" {
  type        = bool
  description = "Whether Cloud Run should allow unauthenticated invocations."
  default     = true
}

variable "min_instance_count" {
  type        = number
  description = "Minimum Cloud Run instances."
  default     = 0
}

variable "max_instance_count" {
  type        = number
  description = "Maximum Cloud Run instances."
  default     = 5
}

variable "concurrency" {
  type        = number
  description = "Cloud Run max concurrent requests per instance."
  default     = 40
}

variable "request_timeout_seconds" {
  type        = number
  description = "Cloud Run request timeout in seconds."
  default     = 300
}

variable "cpu" {
  type        = string
  description = "Cloud Run CPU limit."
  default     = "1"
}

variable "memory" {
  type        = string
  description = "Cloud Run memory limit."
  default     = "1Gi"
}

variable "create_scheduler_jobs" {
  type        = bool
  description = "Whether to create Cloud Scheduler jobs that call the admin job endpoints."
  default     = false
}

variable "scheduler_time_zone" {
  type        = string
  description = "Time zone for Cloud Scheduler jobs."
  default     = "America/New_York"
}

variable "weather_processing_schedule" {
  type        = string
  description = "Cron schedule for weather processing."
  default     = "*/5 * * * *"
}

variable "alert_delivery_retries_schedule" {
  type        = string
  description = "Cron schedule for alert delivery retries."
  default     = "* * * * *"
}

variable "data_retention_schedule" {
  type        = string
  description = "Cron schedule for retention cleanup."
  default     = "0 * * * *"
}

variable "database_password" {
  type        = string
  description = "Secret value for the database password."
  sensitive   = true
}

variable "user_password" {
  type        = string
  description = "Secret value for the built-in USER account password."
  sensitive   = true
}

variable "admin_password" {
  type        = string
  description = "Secret value for the built-in ADMIN account password."
  sensitive   = true
}

variable "jwt_secret" {
  type        = string
  description = "Secret value for JWT signing."
  sensitive   = true
}

variable "mail_password" {
  type        = string
  description = "Secret value for the SMTP password."
  sensitive   = true
}

variable "admin_jobs_token" {
  type        = string
  description = "Secret value for the shared admin job token used by Cloud Scheduler."
  sensitive   = true
}

variable "stripe_secret_key" {
  type        = string
  description = "Secret value for the Stripe secret key."
  sensitive   = true
  default     = ""
}

variable "stripe_webhook_secret" {
  type        = string
  description = "Secret value for the Stripe webhook signing secret."
  sensitive   = true
  default     = ""
}
