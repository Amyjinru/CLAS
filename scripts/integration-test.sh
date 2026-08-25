#!/usr/bin/env bash
set -Eeuo pipefail

base_url="${BASE_URL:-http://localhost}"
backend_url="${BACKEND_URL:-http://localhost:8080}"

curl --fail --silent --show-error --retry 30 --retry-delay 2 "$backend_url/api/health" >/dev/null
curl --fail --silent --show-error --retry 10 --retry-delay 2 "$base_url/" >/dev/null
printf 'integration checks passed\n'
