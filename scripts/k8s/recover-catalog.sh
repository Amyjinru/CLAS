#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
NAMESPACE="${CLAS_NAMESPACE:-clas}"
DEPLOYMENT='clas-catalog'
HPA='clas-catalog'
STATE_PREFIX='resilience.clas.example'
RUN_ID="$(date '+%Y%m%d-%H%M%S')"
OUTPUT_DIR="${CLAS_RESILIENCE_OUTPUT_DIR:-$PROJECT_ROOT/artifacts/resilience/catalog-recovery-$RUN_ID}"

if [[ "${CLAS_CONFIRM_RECOVERY:-}" != 'restore-clas-catalog' ]]; then
  echo 'Set CLAS_CONFIRM_RECOVERY=restore-clas-catalog to restore clas-catalog.' >&2
  exit 2
fi

if ! command -v kubectl >/dev/null 2>&1; then
  echo 'kubectl is required.' >&2
  exit 2
fi

kubectl -n "$NAMESPACE" get deployment "$DEPLOYMENT" >/dev/null
mkdir -p "$OUTPUT_DIR"

replicas="$(kubectl -n "$NAMESPACE" get deployment "$DEPLOYMENT" \
  -o jsonpath="{.metadata.annotations['$STATE_PREFIX/pre-fault-replicas']}" 2>/dev/null || true)"
hpa_was_present="$(kubectl -n "$NAMESPACE" get deployment "$DEPLOYMENT" \
  -o jsonpath="{.metadata.annotations['$STATE_PREFIX/hpa-was-present']}" 2>/dev/null || true)"

if [[ ! "$replicas" =~ ^[1-9][0-9]*$ ]]; then
  replicas=1
fi

echo "Restoring $DEPLOYMENT to $replicas replica(s)."
kubectl -n "$NAMESPACE" scale deployment "$DEPLOYMENT" --replicas="$replicas"
kubectl -n "$NAMESPACE" rollout status deployment/"$DEPLOYMENT" --timeout=300s

if [[ "$hpa_was_present" == 'true' ]]; then
  kubectl apply -f "$PROJECT_ROOT/k8s/catalog-hpa.yaml"
fi

kubectl -n "$NAMESPACE" annotate deployment "$DEPLOYMENT" \
  "$STATE_PREFIX/pre-fault-replicas-" \
  "$STATE_PREFIX/hpa-was-present-" >/dev/null || true

kubectl -n "$NAMESPACE" get deployment "$DEPLOYMENT" -o yaml > "$OUTPUT_DIR/deployment-recovered.yaml"
kubectl -n "$NAMESPACE" get pods -l app=clas-catalog -o wide > "$OUTPUT_DIR/pods-recovered.txt"
kubectl -n "$NAMESPACE" get endpoints clas-catalog -o yaml > "$OUTPUT_DIR/endpoints-recovered.yaml"
kubectl -n "$NAMESPACE" get hpa "$HPA" -o wide > "$OUTPUT_DIR/hpa-recovered.txt" 2>&1 || true

if kubectl -n "$NAMESPACE" exec deployment/clas-gateway -- \
  wget -qO- -T 10 http://clas-catalog:8082/api/health \
  > "$OUTPUT_DIR/catalog-health.json"; then
  echo 'clas-catalog health check passed through the cluster network.'
else
  echo 'Catalog Pods are ready, but the gateway-side health request failed.' >&2
  exit 1
fi

echo "Recovery evidence saved under: $OUTPUT_DIR"
