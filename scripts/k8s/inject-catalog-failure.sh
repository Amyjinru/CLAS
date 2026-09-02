#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
NAMESPACE="${CLAS_NAMESPACE:-clas}"
DEPLOYMENT='clas-catalog'
HPA='clas-catalog'
STATE_PREFIX='resilience.clas.example'
RUN_ID="$(date '+%Y%m%d-%H%M%S')"
OUTPUT_DIR="${CLAS_RESILIENCE_OUTPUT_DIR:-$PROJECT_ROOT/artifacts/resilience/catalog-$RUN_ID}"

if [[ "${CLAS_CONFIRM_FAULT_INJECTION:-}" != 'scale-clas-catalog-to-zero' ]]; then
  echo 'Refusing to stop clas-catalog without explicit confirmation.' >&2
  echo 'Set CLAS_CONFIRM_FAULT_INJECTION=scale-clas-catalog-to-zero.' >&2
  exit 2
fi

if ! command -v kubectl >/dev/null 2>&1; then
  echo 'kubectl is required.' >&2
  exit 2
fi

kubectl -n "$NAMESPACE" get deployment "$DEPLOYMENT" >/dev/null
mkdir -p "$OUTPUT_DIR"

replicas="$(kubectl -n "$NAMESPACE" get deployment "$DEPLOYMENT" -o jsonpath='{.spec.replicas}')"
if [[ ! "$replicas" =~ ^[1-9][0-9]*$ ]]; then
  echo "Refusing fault injection because $DEPLOYMENT currently has replicas=$replicas." >&2
  exit 1
fi

hpa_present='false'
if kubectl -n "$NAMESPACE" get hpa "$HPA" >/dev/null 2>&1; then
  hpa_present='true'
  kubectl -n "$NAMESPACE" get hpa "$HPA" -o yaml > "$OUTPUT_DIR/hpa-before.yaml"
fi

kubectl -n "$NAMESPACE" get deployment "$DEPLOYMENT" -o yaml > "$OUTPUT_DIR/deployment-before.yaml"
kubectl -n "$NAMESPACE" get pods -l app=clas-catalog -o wide > "$OUTPUT_DIR/pods-before.txt"
kubectl -n "$NAMESPACE" annotate deployment "$DEPLOYMENT" \
  "$STATE_PREFIX/pre-fault-replicas=$replicas" \
  "$STATE_PREFIX/hpa-was-present=$hpa_present" \
  --overwrite >/dev/null

if [[ "$hpa_present" == 'true' ]]; then
  echo 'Temporarily deleting clas-catalog HPA so it cannot undo scale-to-zero.'
  kubectl -n "$NAMESPACE" delete hpa "$HPA"
fi

echo "Scaling $DEPLOYMENT from $replicas replica(s) to zero in namespace $NAMESPACE."
kubectl -n "$NAMESPACE" scale deployment "$DEPLOYMENT" --replicas=0

deadline=$((SECONDS + 120))
while (( SECONDS < deadline )); do
  pod_count="$(kubectl -n "$NAMESPACE" get pods -l app=clas-catalog --no-headers 2>/dev/null | wc -l | tr -d ' ')"
  if [[ "$pod_count" == '0' ]]; then
    break
  fi
  sleep 2
done

pod_count="$(kubectl -n "$NAMESPACE" get pods -l app=clas-catalog --no-headers 2>/dev/null | wc -l | tr -d ' ')"
if [[ "$pod_count" != '0' ]]; then
  echo 'Catalog Pods did not terminate within 120 seconds. Run the recovery script.' >&2
  exit 1
fi

kubectl -n "$NAMESPACE" get deployment "$DEPLOYMENT" -o wide > "$OUTPUT_DIR/deployment-fault.txt"
kubectl -n "$NAMESPACE" get endpoints clas-catalog -o yaml > "$OUTPUT_DIR/endpoints-fault.yaml"
kubectl -n "$NAMESPACE" get events --sort-by=.lastTimestamp > "$OUTPUT_DIR/events-fault.txt"

echo 'clas-catalog is stopped. Capture order responses and unaffected-service health now.'
echo 'Recover with: CLAS_CONFIRM_RECOVERY=restore-clas-catalog scripts/k8s/recover-catalog.sh'
echo "Fault-injection evidence saved under: $OUTPUT_DIR"
