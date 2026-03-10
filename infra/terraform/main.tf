locals {
  required_services = toset([
    "artifactregistry.googleapis.com",
    "cloudbuild.googleapis.com",
    "cloudscheduler.googleapis.com",
    "run.googleapis.com",
    "secretmanager.googleapis.com",
    "sqladmin.googleapis.com"
  ])

  labels = {
    app        = var.service_name
    managed_by = "terraform"
  }

  image = (
    var.image != ""
    ? var.image
    : "${var.region}-docker.pkg.dev/${var.project_id}/${var.artifact_registry_repository_id}/${var.service_name}:latest"
  )

  stripe_success_url = (
    var.stripe_success_url != ""
    ? var.stripe_success_url
    : "${var.frontend_base_url}/billing/success?session_id={CHECKOUT_SESSION_ID}"
  )

  stripe_cancel_url = (
    var.stripe_cancel_url != ""
    ? var.stripe_cancel_url
    : "${var.frontend_base_url}/billing/cancel"
  )

  scheduler_jobs = {
    weather-processing = {
      schedule = var.weather_processing_schedule
      path     = "/api/admin/jobs/weather-processing"
    }
    alert-delivery-retries = {
      schedule = var.alert_delivery_retries_schedule
      path     = "/api/admin/jobs/alert-delivery-retries"
    }
    data-retention = {
      schedule = var.data_retention_schedule
      path     = "/api/admin/jobs/data-retention"
    }
  }
}

resource "google_project_service" "required" {
  for_each           = local.required_services
  project            = var.project_id
  service            = each.value
  disable_on_destroy = false
}

resource "google_artifact_registry_repository" "backend" {
  location      = var.region
  repository_id = var.artifact_registry_repository_id
  description   = "Docker images for ${var.service_name}"
  format        = "DOCKER"

  depends_on = [google_project_service.required]
}

resource "google_service_account" "runtime" {
  account_id   = replace("${var.service_name}-runtime", "_", "-")
  display_name = "${var.service_name} runtime"
}

resource "google_project_iam_member" "runtime_cloudsql" {
  project = var.project_id
  role    = "roles/cloudsql.client"
  member  = "serviceAccount:${google_service_account.runtime.email}"
}

resource "google_project_iam_member" "runtime_secret_accessor" {
  project = var.project_id
  role    = "roles/secretmanager.secretAccessor"
  member  = "serviceAccount:${google_service_account.runtime.email}"
}

resource "google_secret_manager_secret" "database_password" {
  secret_id = "${var.service_name}-database-password"
  replication {
    auto {}
  }
}

resource "google_secret_manager_secret_version" "database_password" {
  secret      = google_secret_manager_secret.database_password.id
  secret_data = var.database_password
}

resource "google_secret_manager_secret" "user_password" {
  secret_id = "${var.service_name}-user-password"
  replication {
    auto {}
  }
}

resource "google_secret_manager_secret_version" "user_password" {
  secret      = google_secret_manager_secret.user_password.id
  secret_data = var.user_password
}

resource "google_secret_manager_secret" "admin_password" {
  secret_id = "${var.service_name}-admin-password"
  replication {
    auto {}
  }
}

resource "google_secret_manager_secret_version" "admin_password" {
  secret      = google_secret_manager_secret.admin_password.id
  secret_data = var.admin_password
}

resource "google_secret_manager_secret" "jwt_secret" {
  secret_id = "${var.service_name}-jwt-secret"
  replication {
    auto {}
  }
}

resource "google_secret_manager_secret_version" "jwt_secret" {
  secret      = google_secret_manager_secret.jwt_secret.id
  secret_data = var.jwt_secret
}

resource "google_secret_manager_secret" "mail_password" {
  secret_id = "${var.service_name}-mail-password"
  replication {
    auto {}
  }
}

resource "google_secret_manager_secret_version" "mail_password" {
  secret      = google_secret_manager_secret.mail_password.id
  secret_data = var.mail_password
}

resource "google_secret_manager_secret" "admin_jobs_token" {
  secret_id = "${var.service_name}-admin-jobs-token"
  replication {
    auto {}
  }
}

resource "google_secret_manager_secret_version" "admin_jobs_token" {
  secret      = google_secret_manager_secret.admin_jobs_token.id
  secret_data = var.admin_jobs_token
}

resource "google_secret_manager_secret" "stripe_secret_key" {
  secret_id = "${var.service_name}-stripe-secret-key"
  replication {
    auto {}
  }
}

resource "google_secret_manager_secret_version" "stripe_secret_key" {
  secret      = google_secret_manager_secret.stripe_secret_key.id
  secret_data = var.stripe_secret_key
}

resource "google_secret_manager_secret" "stripe_webhook_secret" {
  secret_id = "${var.service_name}-stripe-webhook-secret"
  replication {
    auto {}
  }
}

resource "google_secret_manager_secret_version" "stripe_webhook_secret" {
  secret      = google_secret_manager_secret.stripe_webhook_secret.id
  secret_data = var.stripe_webhook_secret
}

resource "google_sql_database_instance" "primary" {
  name                = var.cloud_sql_instance_name
  region              = var.region
  database_version    = var.cloud_sql_database_version
  deletion_protection = var.cloud_sql_deletion_protection

  settings {
    tier              = var.cloud_sql_tier
    availability_type = var.cloud_sql_availability_type
    disk_type         = "PD_SSD"
    disk_size         = var.cloud_sql_disk_size_gb
    disk_autoresize   = true

    backup_configuration {
      enabled = true
    }

    ip_configuration {
      ipv4_enabled = true
    }
  }

  depends_on = [google_project_service.required]
}

resource "google_sql_database" "application" {
  name     = var.database_name
  instance = google_sql_database_instance.primary.name
}

resource "google_sql_user" "application" {
  name     = var.database_user
  instance = google_sql_database_instance.primary.name
  password = var.database_password
}

resource "google_cloud_run_v2_service" "backend" {
  name     = var.service_name
  location = var.region
  ingress  = "INGRESS_TRAFFIC_ALL"

  labels = local.labels

  template {
    service_account                  = google_service_account.runtime.email
    timeout                          = "${var.request_timeout_seconds}s"
    max_instance_request_concurrency = var.concurrency

    scaling {
      min_instance_count = var.min_instance_count
      max_instance_count = var.max_instance_count
    }

    containers {
      image = local.image

      ports {
        container_port = 8080
      }

      resources {
        limits = {
          cpu    = var.cpu
          memory = var.memory
        }
      }

      env {
        name  = "SPRING_DATASOURCE_URL"
        value = "jdbc:postgresql://google/${var.database_name}?socketFactory=com.google.cloud.sql.postgres.SocketFactory&cloudSqlInstance=${google_sql_database_instance.primary.connection_name}&ipTypes=PUBLIC,PRIVATE&cloudSqlRefreshStrategy=lazy"
      }

      env {
        name  = "SPRING_DATASOURCE_USERNAME"
        value = var.database_user
      }

      env {
        name  = "APP_SECURITY_USER_USERNAME"
        value = var.user_username
      }

      env {
        name  = "APP_SECURITY_ADMIN_USERNAME"
        value = var.admin_username
      }

      env {
        name  = "APP_NOTIFICATION_EMAIL_PROVIDER"
        value = var.notification_email_provider
      }

      env {
        name  = "SPRING_MAIL_HOST"
        value = var.mail_host
      }

      env {
        name  = "SPRING_MAIL_PORT"
        value = var.mail_port
      }

      env {
        name  = "SPRING_MAIL_USERNAME"
        value = var.mail_username
      }

      env {
        name  = "SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH"
        value = "true"
      }

      env {
        name  = "SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE"
        value = "true"
      }

      env {
        name  = "APP_NOTIFICATION_EMAIL_FROM_ADDRESS"
        value = var.mail_from_address
      }

      env {
        name  = "APP_AUTH_RECOVERY_FRONTEND_BASE_URL"
        value = var.frontend_base_url
      }

      env {
        name  = "APP_BILLING_STRIPE_ENABLED"
        value = tostring(var.stripe_enabled)
      }

      env {
        name  = "APP_BILLING_STRIPE_PRICE_ID"
        value = var.stripe_price_id
      }

      env {
        name  = "APP_BILLING_STRIPE_PLUS_PRICE_ID"
        value = var.stripe_plus_price_id
      }

      env {
        name  = "APP_BILLING_STRIPE_PRO_PRICE_ID"
        value = var.stripe_pro_price_id
      }

      env {
        name  = "APP_BILLING_STRIPE_SUCCESS_URL"
        value = local.stripe_success_url
      }

      env {
        name  = "APP_BILLING_STRIPE_CANCEL_URL"
        value = local.stripe_cancel_url
      }

      env {
        name  = "APP_NOTIFICATION_VERIFICATION_SEND_EMAIL"
        value = "true"
      }

      env {
        name  = "APP_AUTH_RECOVERY_SEND_EMAIL"
        value = "true"
      }

      env {
        name  = "APP_NOTIFICATION_CRITERIA_CREATED_SEND_EMAIL"
        value = "true"
      }

      env {
        name  = "APP_NOTIFICATION_CRITERIA_DELETED_SEND_EMAIL"
        value = "true"
      }

      env {
        name  = "APP_NOTIFICATION_DELIVERY_WORKER_ENABLED"
        value = "true"
      }

      env {
        name  = "APP_WEATHER_PROCESSING_SCHEDULE_ENABLED"
        value = "false"
      }

      env {
        name  = "APP_NOTIFICATION_DELIVERY_RETRY_POLLER_ENABLED"
        value = "false"
      }

      env {
        name  = "APP_RETENTION_SCHEDULE_ENABLED"
        value = "false"
      }

      env {
        name  = "LOGGING_FILE_NAME"
        value = "/tmp/weather-alert-backend.log"
      }

      env {
        name  = "MANAGEMENT_TRACING_SAMPLING_PROBABILITY"
        value = "0.1"
      }

      env {
        name = "SPRING_DATASOURCE_PASSWORD"
        value_source {
          secret_key_ref {
            secret  = google_secret_manager_secret.database_password.secret_id
            version = "latest"
          }
        }
      }

      env {
        name = "APP_SECURITY_USER_PASSWORD"
        value_source {
          secret_key_ref {
            secret  = google_secret_manager_secret.user_password.secret_id
            version = "latest"
          }
        }
      }

      env {
        name = "APP_SECURITY_ADMIN_PASSWORD"
        value_source {
          secret_key_ref {
            secret  = google_secret_manager_secret.admin_password.secret_id
            version = "latest"
          }
        }
      }

      env {
        name = "APP_SECURITY_JWT_SECRET"
        value_source {
          secret_key_ref {
            secret  = google_secret_manager_secret.jwt_secret.secret_id
            version = "latest"
          }
        }
      }

      env {
        name = "SPRING_MAIL_PASSWORD"
        value_source {
          secret_key_ref {
            secret  = google_secret_manager_secret.mail_password.secret_id
            version = "latest"
          }
        }
      }

      env {
        name = "APP_ADMIN_JOBS_TOKEN"
        value_source {
          secret_key_ref {
            secret  = google_secret_manager_secret.admin_jobs_token.secret_id
            version = "latest"
          }
        }
      }

      env {
        name = "APP_BILLING_STRIPE_SECRET_KEY"
        value_source {
          secret_key_ref {
            secret  = google_secret_manager_secret.stripe_secret_key.secret_id
            version = "latest"
          }
        }
      }

      env {
        name = "APP_BILLING_STRIPE_WEBHOOK_SECRET"
        value_source {
          secret_key_ref {
            secret  = google_secret_manager_secret.stripe_webhook_secret.secret_id
            version = "latest"
          }
        }
      }
    }
  }

  lifecycle {
    ignore_changes = [
      template[0].containers[0].image
    ]
  }

  depends_on = [
    google_project_service.required,
    google_project_iam_member.runtime_cloudsql,
    google_project_iam_member.runtime_secret_accessor,
    google_sql_database.application,
    google_sql_user.application,
    google_secret_manager_secret_version.database_password,
    google_secret_manager_secret_version.user_password,
    google_secret_manager_secret_version.admin_password,
    google_secret_manager_secret_version.jwt_secret,
    google_secret_manager_secret_version.mail_password,
    google_secret_manager_secret_version.admin_jobs_token,
    google_secret_manager_secret_version.stripe_secret_key,
    google_secret_manager_secret_version.stripe_webhook_secret
  ]
}

resource "google_cloud_run_v2_service_iam_member" "public_invoker" {
  count    = var.allow_unauthenticated ? 1 : 0
  name     = google_cloud_run_v2_service.backend.name
  location = google_cloud_run_v2_service.backend.location
  role     = "roles/run.invoker"
  member   = "allUsers"
}

resource "google_cloud_scheduler_job" "admin_jobs" {
  for_each = var.create_scheduler_jobs ? local.scheduler_jobs : {}

  name             = "${var.service_name}-${each.key}"
  description      = "Trigger ${each.key} for ${var.service_name}"
  region           = var.region
  schedule         = each.value.schedule
  time_zone        = var.scheduler_time_zone
  attempt_deadline = "320s"

  http_target {
    http_method = "POST"
    uri         = "${google_cloud_run_v2_service.backend.uri}${each.value.path}"
    headers = {
      "X-Admin-Job-Token" = var.admin_jobs_token
    }
  }

  depends_on = [google_cloud_run_v2_service.backend]
}
