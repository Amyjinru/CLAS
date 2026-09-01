#!/usr/bin/env bash
set -euo pipefail

NAMESPACE="${CLAS_NAMESPACE:-clas}"
OUTPUT_DIR="${1:-artifacts/k8s-diagnostics}"
mkdir -p "$OUTPUT_DIR"

kubectl -n "$NAMESPACE" get all,ingress,pvc,hpa -o wide > "$OUTPUT_DIR/resources.txt" 2>&1 || true
kubectl -n "$NAMESPACE" get events --sort-by=.lastTimestamp > "$OUTPUT_DIR/events.txt" 2>&1 || true
kubectl -n "$NAMESPACE" top nodes > "$OUTPUT_DIR/node-metrics.txt" 2>&1 || true
kubectl -n "$NAMESPACE" top pods > "$OUTPUT_DIR/pod-metrics.txt" 2>&1 || true
kubectl -n "$NAMESPACE" describe hpa clas-catalog > "$OUTPUT_DIR/clas-catalog-hpa.txt" 2>&1 || true
for app in mysql redis clas-iam clas-merchant clas-catalog clas-order clas-compat clas-gateway frontend; do
  kubectl -n "$NAMESPACE" logs -l "app=$app" --all-containers --prefix --tail=200 > "$OUTPUT_DIR/$app.log" 2>&1 || true
done
kubectl -n "$NAMESPACE" logs job/clas-db-migrate --tail=200 > "$OUTPUT_DIR/migrate.log" 2>&1 || true
