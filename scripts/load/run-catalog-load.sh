#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
NAMESPACE="${CLAS_NAMESPACE:-clas}"
BASE_URL="${CLAS_BASE_URL:-http://8.141.112.182}"
MERCHANT_ID="${CLAS_MERCHANT_ID:-}"
RUN_ID="$(date '+%Y%m%d-%H%M%S')"
OUTPUT_DIR="${CLAS_LOAD_OUTPUT_DIR:-$PROJECT_ROOT/artifacts/hpa/catalog-$RUN_ID}"
POST_LOAD_OBSERVE_SECONDS="${CLAS_POST_LOAD_OBSERVE_SECONDS:-240}"
WATCH_PID=''

cleanup() {
  if [[ -n "$WATCH_PID" ]] && kill -0 "$WATCH_PID" >/dev/null 2>&1; then
    kill "$WATCH_PID" >/dev/null 2>&1 || true
    wait "$WATCH_PID" 2>/dev/null || true
  fi
}
trap cleanup EXIT INT TERM

if [[ "${CLAS_CONFIRM_LOAD_TEST:-}" != 'run-clas-catalog-load-test' ]]; then
  echo 'Refusing to generate load without explicit confirmation.' >&2
  echo 'Set CLAS_CONFIRM_LOAD_TEST=run-clas-catalog-load-test.' >&2
  exit 2
fi

for command_name in curl k6 kubectl; do
  if ! command -v "$command_name" >/dev/null 2>&1; then
    echo "$command_name is required." >&2
    exit 2
  fi
done

if [[ ! "$MERCHANT_ID" =~ ^[0-9]+$ ]]; then
  echo 'CLAS_MERCHANT_ID must be an existing numeric merchant id.' >&2
  exit 2
fi

if [[ ! "$POST_LOAD_OBSERVE_SECONDS" =~ ^[0-9]+$ ]]; then
  echo 'CLAS_POST_LOAD_OBSERVE_SECONDS must be a non-negative integer.' >&2
  exit 2
fi

mkdir -p "$OUTPUT_DIR"

echo "Checking catalog baseline at $BASE_URL/api/product/list/$MERCHANT_ID"
curl -fsS --max-time 10 \
  "$BASE_URL/api/product/list/$MERCHANT_ID" \
  -o "$OUTPUT_DIR/baseline-response.json"

if ! kubectl -n "$NAMESPACE" top pods -l app=clas-catalog > "$OUTPUT_DIR/baseline-pod-metrics.txt"; then
  echo 'Pod metrics are unavailable; verify metrics-server before running the HPA experiment.' >&2
  exit 1
fi

kubectl apply -f "$PROJECT_ROOT/k8s/catalog-hpa.yaml"
kubectl -n "$NAMESPACE" get hpa clas-catalog -o yaml > "$OUTPUT_DIR/hpa-before.yaml"
kubectl -n "$NAMESPACE" describe hpa clas-catalog > "$OUTPUT_DIR/hpa-before.txt"

"$PROJECT_ROOT/scripts/k8s/watch-catalog-hpa.sh" \
  "$OUTPUT_DIR/kubectl-hpa-watch.txt" &
WATCH_PID=$!

set +e
k6 run \
  --out "csv=$OUTPUT_DIR/requests.csv" \
  -e "BASE_URL=$BASE_URL" \
  -e "MERCHANT_ID=$MERCHANT_ID" \
  -e "SUMMARY_PATH=$OUTPUT_DIR/k6-summary.json" \
  "$PROJECT_ROOT/scripts/load/load-catalog-list.k6.js" \
  2>&1 | tee "$OUTPUT_DIR/k6-console.txt"
K6_STATUS=${PIPESTATUS[0]}
set -e

if (( POST_LOAD_OBSERVE_SECONDS > 0 )); then
  echo "Load ended; observing scale-down for $POST_LOAD_OBSERVE_SECONDS seconds."
  sleep "$POST_LOAD_OBSERVE_SECONDS"
fi

cleanup
WATCH_PID=''

kubectl -n "$NAMESPACE" get hpa clas-catalog -o yaml > "$OUTPUT_DIR/hpa-after.yaml"
kubectl -n "$NAMESPACE" describe hpa clas-catalog > "$OUTPUT_DIR/hpa-after.txt"
kubectl -n "$NAMESPACE" get events --sort-by=.lastTimestamp > "$OUTPUT_DIR/events.txt"
kubectl -n "$NAMESPACE" get pods -l app=clas-catalog -o wide > "$OUTPUT_DIR/catalog-pods-after.txt"

echo "Load-test evidence saved under: $OUTPUT_DIR"
exit "$K6_STATUS"
