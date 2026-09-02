#!/usr/bin/env bash
set -euo pipefail

NAMESPACE="${CLAS_NAMESPACE:-clas}"
APP_SELECTOR='app in (clas-iam,clas-merchant,clas-catalog,clas-order,clas-compat,clas-gateway,frontend)'

if ! kubectl get "namespace/$NAMESPACE" >/dev/null 2>&1; then
  exit 0
fi

kubectl -n "$NAMESPACE" delete hpa clas-catalog --ignore-not-found
deployments=()
deployment_names="$(
  kubectl -n "$NAMESPACE" get deployment \
    -o jsonpath='{range .items[*]}{.metadata.name}{"\n"}{end}'
)"
while IFS= read -r deployment_name; do
  case "$deployment_name" in
    clas-iam|clas-merchant|clas-catalog|clas-order|clas-compat|clas-gateway|frontend)
      deployments+=("deployment/$deployment_name")
      ;;
  esac
done <<< "$deployment_names"
if ((${#deployments[@]})); then
  kubectl -n "$NAMESPACE" scale "${deployments[@]}" --replicas=0
fi

if kubectl -n "$NAMESPACE" get pod -l "$APP_SELECTOR" -o name | grep -q .; then
  if ! kubectl -n "$NAMESPACE" wait --for=delete pod -l "$APP_SELECTOR" --timeout=180s; then
    # A terminating Java Pod can remain stuck while a resource-starved single
    # node is recovering. It no longer serves traffic after the Deployment was
    # scaled to zero, so force removal is safe and prevents the next rollout
    # from being blocked indefinitely.
    echo 'Timed out waiting for previous application Pods; force deleting remaining Pods.' >&2
    kubectl -n "$NAMESPACE" delete pod -l "$APP_SELECTOR" --grace-period=0 --force --wait=true
    kubectl -n "$NAMESPACE" wait --for=delete pod -l "$APP_SELECTOR" --timeout=60s
  fi
fi
