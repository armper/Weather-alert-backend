#!/usr/bin/env bash

set -euo pipefail

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  cat <<'EOF'
Rotate the Cloud Run admin password secret and roll the service.

Defaults:
  PROJECT_ID   from `gcloud config get-value project`
  REGION       us-east1
  SERVICE_NAME weather-alert-backend
  SECRET_NAME  <service>-admin-password

Optional overrides:
  PROJECT_ID=weather-alerts-panda \
  REGION=us-east1 \
  SERVICE_NAME=weather-alert-backend \
  SECRET_NAME=weather-alert-backend-admin-password \
  ./scripts/rotate-cloud-run-admin-password.sh
EOF
  exit 0
fi

if [[ -n "${GCLOUD_BIN:-}" ]]; then
  GCLOUD="$GCLOUD_BIN"
elif command -v gcloud >/dev/null 2>&1; then
  GCLOUD="$(command -v gcloud)"
elif [[ -x /usr/local/bin/gcloud ]]; then
  GCLOUD="/usr/local/bin/gcloud"
else
  echo "gcloud is not installed. Install Google Cloud CLI first."
  exit 1
fi

PROJECT_ID="${PROJECT_ID:-$("$GCLOUD" config get-value project 2>/dev/null || true)}"
if [[ "$PROJECT_ID" == "(unset)" ]]; then
  PROJECT_ID=""
fi

REGION="${REGION:-us-east1}"
SERVICE_NAME="${SERVICE_NAME:-weather-alert-backend}"
SECRET_NAME="${SECRET_NAME:-${SERVICE_NAME}-admin-password}"
SECRET_ENV_VAR="${SECRET_ENV_VAR:-APP_SECURITY_ADMIN_PASSWORD}"

if [[ -z "$PROJECT_ID" ]]; then
  echo "PROJECT_ID is not set and gcloud has no active project."
  echo "Set PROJECT_ID or run: $GCLOUD config set project YOUR_PROJECT_ID"
  exit 1
fi

if ! "$GCLOUD" auth list --filter=status:ACTIVE --format="value(account)" | grep -q .; then
  echo "No active gcloud account found."
  echo "Run: $GCLOUD auth login"
  exit 1
fi

echo "Project: $PROJECT_ID"
echo "Region: $REGION"
echo "Service: $SERVICE_NAME"
echo "Secret: $SECRET_NAME"
echo

if ! "$GCLOUD" secrets describe "$SECRET_NAME" --project "$PROJECT_ID" >/dev/null 2>&1; then
  echo "Secret not found: $SECRET_NAME"
  exit 1
fi

if ! "$GCLOUD" run services describe "$SERVICE_NAME" --project "$PROJECT_ID" --region "$REGION" >/dev/null 2>&1; then
  echo "Cloud Run service not found: $SERVICE_NAME"
  exit 1
fi

read -r -s -p "Enter new admin password: " PASSWORD_1
echo
read -r -s -p "Confirm new admin password: " PASSWORD_2
echo

if [[ -z "$PASSWORD_1" ]]; then
  echo "Password cannot be empty."
  exit 1
fi

if [[ "$PASSWORD_1" != "$PASSWORD_2" ]]; then
  echo "Passwords do not match."
  exit 1
fi

printf '%s' "$PASSWORD_1" | "$GCLOUD" secrets versions add "$SECRET_NAME" \
  --project "$PROJECT_ID" \
  --data-file=-

"$GCLOUD" run services update "$SERVICE_NAME" \
  --project "$PROJECT_ID" \
  --region "$REGION" \
  --update-secrets "${SECRET_ENV_VAR}=${SECRET_NAME}:latest"

echo
echo "Admin password rotated."
echo "Service updated: $SERVICE_NAME"
echo "Secret updated: $SECRET_NAME"
