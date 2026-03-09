#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEFAULT_ENV_FILE="$ROOT_DIR/deploy/cloudrun/env.yaml"

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
ENV_FILE="${ENV_FILE:-$DEFAULT_ENV_FILE}"
MIN_INSTANCES="${MIN_INSTANCES:-0}"
MAX_INSTANCES="${MAX_INSTANCES:-5}"
CONCURRENCY="${CONCURRENCY:-40}"

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

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Environment file not found: $ENV_FILE"
  echo "Start from: $ROOT_DIR/deploy/cloudrun/env.example.yaml"
  exit 1
fi

echo "Using gcloud: $GCLOUD"
echo "Project: $PROJECT_ID"
echo "Region: $REGION"
echo "Service: $SERVICE_NAME"
echo "Env file: $ENV_FILE"
echo "Min instances: $MIN_INSTANCES"
echo "Max instances: $MAX_INSTANCES"
echo "Concurrency: $CONCURRENCY"

"$GCLOUD" services enable \
  run.googleapis.com \
  cloudbuild.googleapis.com \
  artifactregistry.googleapis.com \
  sqladmin.googleapis.com \
  secretmanager.googleapis.com \
  --project "$PROJECT_ID"

"$GCLOUD" run deploy "$SERVICE_NAME" \
  --project "$PROJECT_ID" \
  --region "$REGION" \
  --source "$ROOT_DIR" \
  --port 8080 \
  --allow-unauthenticated \
  --cpu 1 \
  --memory 1Gi \
  --concurrency "$CONCURRENCY" \
  --timeout 300 \
  --min-instances "$MIN_INSTANCES" \
  --max-instances "$MAX_INSTANCES" \
  --env-vars-file "$ENV_FILE"
