#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SERVICE="${1:-}"
PORT="${2:-}"
RUNS="${3:-3}"
TIMEOUT_SECONDS="${4:-180}"

if [[ -z "$SERVICE" || -z "$PORT" ]]; then
  echo "Usage: $0 <compose-service> <port> [runs] [timeout-seconds]"
  echo "Example: $0 weather-app 8088 3"
  exit 1
fi

cd "$ROOT_DIR"

echo "Benchmarking service=$SERVICE port=$PORT runs=$RUNS timeout=${TIMEOUT_SECONDS}s"
echo "run,startup_seconds,memory_usage"

for i in $(seq 1 "$RUNS"); do
  docker compose stop "$SERVICE" >/dev/null 2>&1 || true
  docker compose up -d --no-deps "$SERVICE" >/dev/null

  start_ts="$(perl -MTime::HiRes=time -e 'print time')"
  deadline="$(perl -MTime::HiRes=time -e "print time + ${TIMEOUT_SECONDS}")"

  while true; do
    if curl -fsS "http://localhost:${PORT}/actuator/health" 2>/dev/null | grep -q '"status":"UP"'; then
      break
    fi
    now="$(perl -MTime::HiRes=time -e 'print time')"
    if awk "BEGIN {exit !($now > $deadline)}"; then
      echo "$i,TIMEOUT,NA"
      exit 2
    fi
    sleep 1
  done

  end_ts="$(perl -MTime::HiRes=time -e 'print time')"
  elapsed="$(awk "BEGIN {printf \"%.2f\", $end_ts - $start_ts}")"
  container_id="$(docker compose ps -q "$SERVICE")"
  mem_usage="$(docker stats --no-stream "$container_id" --format '{{.MemUsage}}' | awk -F'/' '{gsub(/ /,"",$1); print $1}')"
  echo "$i,$elapsed,$mem_usage"
done
