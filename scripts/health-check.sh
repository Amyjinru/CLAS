#!/usr/bin/env bash
set -Eeuo pipefail
url="${HEALTH_URL:-http://localhost/api/health}"
curl --fail --silent --show-error --retry 30 --retry-delay 2 "$url" >/dev/null
printf 'health check passed: %s\n' "$url"
