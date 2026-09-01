#!/usr/bin/env bash
set -euo pipefail

NAMESPACE="${CLAS_NAMESPACE:-clas}"
APP_SELECTOR='app in (clas-iam,clas-merchant,clas-catalog,clas-order,clas-compat,clas-gateway,frontend)'

if ! kubectl get "namespace/$NAMESPACE" >/dev/null 2>&1; then
  exit 0
fi

kubectl -n "$NAMESPACE" delete hpa clas-catalog --ignore-not-found
deployments=()
while IFS= read -r deployment; do
  [[ -n "$deployment" ]] && deployments+=("$deployment")
done < <(kubectl -n "$NAMESPACE" get deployment -l "$APP_SELECTOR" -o name)
if ((${#deployments[@]})); then
  kubectl -n "$NAMESPACE" scale "${deployments[@]}" --replicas=0
fi

if kubectl -n "$NAMESPACE" get pod -l "$APP_SELECTOR" -o name | grep -q .; then
  kubectl -n "$NAMESPACE" wait --for=delete pod -l "$APP_SELECTOR" --timeout=180s
fi
