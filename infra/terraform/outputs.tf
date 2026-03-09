output "artifact_registry_repository" {
  value       = google_artifact_registry_repository.backend.id
  description = "Artifact Registry repository resource ID."
}

output "cloud_run_service_url" {
  value       = google_cloud_run_v2_service.backend.uri
  description = "Public Cloud Run service URL."
}

output "cloud_sql_instance_connection_name" {
  value       = google_sql_database_instance.primary.connection_name
  description = "Cloud SQL instance connection name."
}

output "cloud_sql_instance_name" {
  value       = google_sql_database_instance.primary.name
  description = "Cloud SQL instance name."
}

output "runtime_service_account_email" {
  value       = google_service_account.runtime.email
  description = "Runtime service account email."
}

output "admin_job_endpoints" {
  value = {
    weather_processing     = "${google_cloud_run_v2_service.backend.uri}/api/admin/jobs/weather-processing"
    alert_delivery_retries = "${google_cloud_run_v2_service.backend.uri}/api/admin/jobs/alert-delivery-retries"
    data_retention_cleanup = "${google_cloud_run_v2_service.backend.uri}/api/admin/jobs/data-retention"
  }
  description = "Admin job endpoints for external scheduling."
}
