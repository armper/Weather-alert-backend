#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
IMAGE_NAME="${1:-weather-alert-backend-native:local}"

cd "$ROOT_DIR"

echo "Building native image: $IMAGE_NAME"
echo "This uses Spring Boot buildpacks + GraalVM Native Image (can take several minutes)."

BP_NATIVE_IMAGE_BUILD_ARGUMENTS="${BP_NATIVE_IMAGE_BUILD_ARGUMENTS:---initialize-at-run-time=ch.qos.logback.core.CoreConstants,ch.qos.logback.classic.Logger,ch.qos.logback.classic.Level,ch.qos.logback.core.util.StatusPrinter,ch.qos.logback.core.util.Loader,ch.qos.logback.core.status.StatusBase,ch.qos.logback.core.status.InfoStatus,org.slf4j.LoggerFactory}"

mvn -Pnative -DskipTests \
  -Dspring-boot.build-image.imageName="$IMAGE_NAME" \
  -Dspring-boot.build-image.builder=paketobuildpacks/builder-jammy-tiny \
  -Dspring-boot.build-image.environment.BP_NATIVE_IMAGE=true \
  -Dspring-boot.build-image.environment.BP_JVM_VERSION=21 \
  -Dspring-boot.build-image.environment.BP_NATIVE_IMAGE_BUILD_ARGUMENTS="$BP_NATIVE_IMAGE_BUILD_ARGUMENTS" \
  spring-boot:build-image

echo "Native image build complete: $IMAGE_NAME"
