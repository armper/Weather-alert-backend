#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
IMAGE_NAME="${1:-}"
DOCKERFILE="${2:-Dockerfile.lite}"
MODE="${3:-local}"
PLATFORMS="${4:-linux/amd64,linux/arm64}"

if [[ -z "$IMAGE_NAME" ]]; then
  echo "Usage: $0 <image-name> [dockerfile] [local|push] [platforms]"
  echo "Example (local current arch): $0 weather-alert-backend-weather-app-lite:local Dockerfile.lite local"
  echo "Example (multi-arch push):    $0 ghcr.io/acme/weather-alert-backend-lite:0.1.0 Dockerfile.lite push"
  exit 1
fi

cd "$ROOT_DIR"

if [[ "$MODE" == "push" ]]; then
  echo "Building and pushing multi-arch image: $IMAGE_NAME"
  docker buildx build \
    --platform "$PLATFORMS" \
    -f "$DOCKERFILE" \
    -t "$IMAGE_NAME" \
    --push \
    .
  exit 0
fi

echo "Building local image for current platform only: $IMAGE_NAME"
docker buildx build \
  -f "$DOCKERFILE" \
  -t "$IMAGE_NAME" \
  --load \
  .
