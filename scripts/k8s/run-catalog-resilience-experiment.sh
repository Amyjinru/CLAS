#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
NAMESPACE="${CLAS_NAMESPACE:-clas}"
BASE_URL="${CLAS_BASE_URL:-http://8.141.112.182}"
MERCHANT_ID="${CLAS_MERCHANT_ID:-}"
AUTH_TOKEN="${CLAS_AUTH_TOKEN:-}"
RUN_ID="$(date '+%Y%m%d-%H%M%S')"
OUTPUT_DIR="${CLAS_RESILIENCE_OUTPUT_DIR:-$PROJECT_ROOT/artifacts/resilience/catalog-$RUN_ID}"
RECOVERY_REQUIRED='false'

recover_on_exit() {
  if [[ "$RECOVERY_REQUIRED" == 'true' ]]; then
    RECOVERY_REQUIRED='false'
    echo 'Experiment exited early; attempting automatic catalog recovery.' >&2
    CLAS_CONFIRM_RECOVERY=restore-clas-catalog \
      CLAS_RESILIENCE_OUTPUT_DIR="$OUTPUT_DIR" \
      "$PROJECT_ROOT/scripts/k8s/recover-catalog.sh" || true
  fi
}
trap recover_on_exit EXIT INT TERM

if [[ "${CLAS_CONFIRM_RESILIENCE_EXPERIMENT:-}" != 'run-order-catalog-failure-test' ]]; then
  echo 'Refusing to run the resilience experiment without explicit confirmation.' >&2
  echo 'Set CLAS_CONFIRM_RESILIENCE_EXPERIMENT=run-order-catalog-failure-test.' >&2
  exit 2
fi

for command_name in curl kubectl; do
  if ! command -v "$command_name" >/dev/null 2>&1; then
    echo "$command_name is required." >&2
    exit 2
  fi
done

if [[ ! "$MERCHANT_ID" =~ ^[0-9]+$ ]]; then
  echo 'CLAS_MERCHANT_ID must be an existing numeric merchant id.' >&2
  exit 2
fi

if [[ -z "$AUTH_TOKEN" ]]; then
  echo 'CLAS_AUTH_TOKEN is required and will not be written to the evidence directory.' >&2
  exit 2
fi

mkdir -p "$OUTPUT_DIR"
PREVIEW_URL="$BASE_URL/api/order/preview?merchantId=$MERCHANT_ID"

capture_preview() {
  local phase="$1"
  local expected_status="$2"
  local attempts="${3:-1}"
  local attempt
  local status=''

  for ((attempt = 1; attempt <= attempts; attempt++)); do
    status="$(curl -sS --max-time 10 \
      -H "Authorization: Bearer $AUTH_TOKEN" \
      -H "X-Request-Id: resilience-$RUN_ID-$phase-$attempt" \
      -o "$OUTPUT_DIR/$phase-response.json" \
      -w '%{http_code}' \
      "$PREVIEW_URL")"
    if [[ "$status" == "$expected_status" ]]; then
      break
    fi
    if (( attempt < attempts )); then
      sleep 5
    fi
  done
  printf '%s\n' "$status" > "$OUTPUT_DIR/$phase-http-status.txt"
  if [[ "$status" != "$expected_status" ]]; then
    echo "$phase request returned HTTP $status; expected $expected_status." >&2
    return 1
  fi
}

capture_service_health() {
  local phase="$1"
  shift
  local service_spec
  local service_name
  local service_port

  for service_spec in "$@"; do
    service_name="${service_spec%%:*}"
    service_port="${service_spec##*:}"
    kubectl -n "$NAMESPACE" exec deployment/clas-gateway -- \
      wget -qO- -T 10 "http://$service_name:$service_port/api/health" \
      > "$OUTPUT_DIR/$phase-$service_name-health.json"
  done
}

printf 'run_id=%s\nnamespace=%s\nbase_url=%s\nmerchant_id=%s\n' \
  "$RUN_ID" "$NAMESPACE" "$BASE_URL" "$MERCHANT_ID" \
  > "$OUTPUT_DIR/experiment-metadata.txt"

echo 'Capturing healthy baseline.'
capture_preview normal 200
capture_service_health normal \
  clas-iam:8081 clas-merchant:8085 clas-catalog:8082 clas-order:8083 clas-compat:8084

RECOVERY_REQUIRED='true'
CLAS_CONFIRM_FAULT_INJECTION=scale-clas-catalog-to-zero \
  CLAS_RESILIENCE_OUTPUT_DIR="$OUTPUT_DIR" \
  "$PROJECT_ROOT/scripts/k8s/inject-catalog-failure.sh"

echo 'Capturing degraded order response and unaffected-service health.'
capture_preview fault 503
capture_service_health fault clas-iam:8081 clas-merchant:8085 clas-order:8083 clas-compat:8084
kubectl -n "$NAMESPACE" logs deployment/clas-order \
  --all-containers --since=10m > "$OUTPUT_DIR/order-fault.log" 2>&1 || true
kubectl -n "$NAMESPACE" get events --sort-by=.lastTimestamp > "$OUTPUT_DIR/events-fault.txt"

CLAS_CONFIRM_RECOVERY=restore-clas-catalog \
  CLAS_RESILIENCE_OUTPUT_DIR="$OUTPUT_DIR" \
  "$PROJECT_ROOT/scripts/k8s/recover-catalog.sh"
RECOVERY_REQUIRED='false'

echo 'Capturing recovered response and service health.'
capture_preview recovery 200 12
capture_service_health recovery \
  clas-iam:8081 clas-merchant:8085 clas-catalog:8082 clas-order:8083 clas-compat:8084

echo "Resilience evidence saved under: $OUTPUT_DIR"
