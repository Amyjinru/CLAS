#!/usr/bin/env bash
set -euo pipefail

NAMESPACE="${CLAS_NAMESPACE:-clas}"
INTERVAL_SECONDS="${CLAS_WATCH_INTERVAL_SECONDS:-10}"
OUTPUT_FILE="${1:-}"

if ! command -v kubectl >/dev/null 2>&1; then
  echo 'kubectl is required.' >&2
  exit 2
fi

if [[ ! "$INTERVAL_SECONDS" =~ ^[1-9][0-9]*$ ]]; then
  echo 'CLAS_WATCH_INTERVAL_SECONDS must be a positive integer.' >&2
  exit 2
fi

if [[ -n "$OUTPUT_FILE" ]]; then
  mkdir -p "$(dirname "$OUTPUT_FILE")"
  exec > >(tee -a "$OUTPUT_FILE") 2>&1
fi

while true; do
  echo "timestamp=$(date -Iseconds)"
  kubectl -n "$NAMESPACE" get hpa clas-catalog -o wide || true
  kubectl -n "$NAMESPACE" get deployment clas-catalog -o wide || true
  kubectl -n "$NAMESPACE" get pods -l app=clas-catalog -o wide || true
  kubectl -n "$NAMESPACE" top pods -l app=clas-catalog || true
  echo
  sleep "$INTERVAL_SECONDS"
done
