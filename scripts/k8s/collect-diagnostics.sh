#!/usr/bin/env bash
set -euo pipefail

NAMESPACE="${CLAS_NAMESPACE:-clas}"
OUTPUT_DIR="${1:-artifacts/k8s-diagnostics}"
case "$OUTPUT_DIR" in
  ''|/|.)
    echo "Refusing unsafe diagnostics output directory: $OUTPUT_DIR" >&2
    exit 2
    ;;
esac
mkdir -p "$OUTPUT_DIR"
# 诊断目录会被 CI 回传为本轮唯一证据；先移除上一轮的普通文件，避免旧 Pod、镜像或日志混入。
find "$OUTPUT_DIR" -mindepth 1 -maxdepth 1 -type f -delete

kubectl --request-timeout=15s -n "$NAMESPACE" get all,ingress,pvc,hpa -o wide > "$OUTPUT_DIR/resources.txt" 2>&1 || true
kubectl --request-timeout=15s -n "$NAMESPACE" get events --sort-by=.lastTimestamp > "$OUTPUT_DIR/events.txt" 2>&1 || true
kubectl --request-timeout=15s -n "$NAMESPACE" top nodes > "$OUTPUT_DIR/node-metrics.txt" 2>&1 || true
kubectl --request-timeout=15s -n "$NAMESPACE" top pods > "$OUTPUT_DIR/pod-metrics.txt" 2>&1 || true
kubectl --request-timeout=15s -n "$NAMESPACE" describe hpa clas-catalog > "$OUTPUT_DIR/clas-catalog-hpa.txt" 2>&1 || true
for app in mysql redis clas-iam clas-merchant clas-catalog clas-order clas-compat clas-gateway frontend; do
  kubectl --request-timeout=15s -n "$NAMESPACE" logs -l "app=$app" --all-containers --prefix --tail=200 > "$OUTPUT_DIR/$app.log" 2>&1 || true
done
kubectl --request-timeout=15s -n "$NAMESPACE" logs job/clas-db-migrate --tail=200 > "$OUTPUT_DIR/migrate.log" 2>&1 || true
