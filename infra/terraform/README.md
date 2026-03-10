# Terraform Deployment

This folder provisions the baseline Google Cloud resources for the backend:

- required Google APIs
- Artifact Registry
- Cloud SQL for PostgreSQL (instance, database, and application user)
- runtime service account
- Secret Manager secrets and latest versions
- Cloud Run service
- optional Cloud Scheduler jobs targeting `/api/admin/jobs/**`
- optional Stripe runtime wiring for checkout and webhook secrets

## Prerequisites

- Terraform 1.6+
- `gcloud auth application-default login`
- a Google Cloud project with billing enabled

## Usage

For a brand-new project, publish the first image before the first full apply:

```bash
gcloud builds submit --config cloudbuild.yaml \
  --substitutions=_REGION=us-east1,_SERVICE_NAME=weather-alert-backend,_AR_REPOSITORY=weather-alert-backend,_IMAGE_NAME=weather-alert-backend,_DEPLOY=false
```

Then provision the infrastructure:

```bash
cd infra/terraform
cp terraform.tfvars.example terraform.tfvars
terraform init
terraform plan
terraform apply
```

## Notes

- `google_cloud_run_v2_service.backend` ignores image changes so `cloudbuild.yaml` can deploy new revisions without Terraform fighting those updates.
- `terraform.tfvars` now controls Cloud SQL sizing and protection flags. The Cloud Run service reads the generated Cloud SQL connection name directly from Terraform-managed resources.
- Stripe runtime values can be managed with `stripe_enabled`, `stripe_plus_price_id`, `stripe_pro_price_id`, `stripe_secret_key`, and `stripe_webhook_secret`.
- `stripe_price_id` remains available as a legacy fallback and should generally match `stripe_plus_price_id` if you still use it.
- If `create_scheduler_jobs=true`, Terraform stores `admin_jobs_token` in state because Cloud Scheduler needs it in an HTTP header. Use a secured remote backend if you keep that enabled.
- The Cloud Run service is configured for external traffic and authenticated admin job triggers via `X-Admin-Job-Token`.
