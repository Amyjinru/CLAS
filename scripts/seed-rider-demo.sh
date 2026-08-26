#!/usr/bin/env bash
# Seeds only the dedicated 139000090xx rider-demo records on a CLAS server.
# Run on the server after database/seed-rider-demo.sql has been imported.
set -euo pipefail

APP_DIR="${APP_DIR:-/opt/clas}"
ENV_FILE="${CLAS_ENV_FILE:-/etc/clas/clas.env}"
API_BASE_URL="${API_BASE_URL:-http://127.0.0.1:8080/api}"

if [[ ! -r "$ENV_FILE" ]]; then
  echo "Missing server environment file: $ENV_FILE" >&2
  exit 1
fi

set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

MYSQL_ARGS=(--host="$MYSQL_HOST" --port="${MYSQL_PORT:-3306}" --user="$MYSQL_USER" "$MYSQL_DATABASE")
MYSQL_PWD="$MYSQL_PASSWORD"
export MYSQL_PWD

mysql "${MYSQL_ARGS[@]}" < "$APP_DIR/database/seed-rider-demo.sql"

json_field() {
  local field="$1"
  python3 -c "import json, sys; print(json.load(sys.stdin)['data']${field})"
}

login_as_user() {
  local phone="$1"
  local login_response token switch_response
  login_response="$(curl --fail --silent --show-error -X POST "$API_BASE_URL/user/login" -H 'Content-Type: application/json' --data "{\"phone\":\"$phone\",\"password\":\"Abc123!\"}")"
  token="$(printf '%s' "$login_response" | json_field "['token']")"
  switch_response="$(curl --fail --silent --show-error -X POST "$API_BASE_URL/user/switch-role" -H 'Content-Type: application/json' -H "Authorization: Bearer $token" --data '{"role":"USER"}')"
  printf '%s' "$switch_response" | json_field "['token']"
}

login_as_admin() {
  curl --fail --silent --show-error -X POST "$API_BASE_URL/user/login" -H 'Content-Type: application/json' --data '{"phone":"13900009003","password":"Abc123!"}' | json_field "['token']"
}

ensure_rider() {
  local phone="$1" name="$2" id_card="$3" existing rider_token application_response application_id admin_token
  existing="$(mysql --batch --skip-column-names "${MYSQL_ARGS[@]}" -e "SELECT COUNT(*) FROM rider_profile WHERE user_id = '$phone'")"
  if [[ "$existing" == "1" ]]; then
    echo "Rider profile already exists: $phone"
    return
  fi

  rider_token="$(login_as_user "$phone")"
  application_response="$(curl --fail --silent --show-error -X POST "$API_BASE_URL/rider/applications" -H 'Content-Type: application/json' -H "Authorization: Bearer $rider_token" --data "{\"realName\":\"$name\",\"idCardNo\":\"$id_card\",\"vehicleType\":\"电动车\",\"serviceArea\":\"北京市东城区\",\"emergencyContactName\":\"演示紧急联系人\",\"emergencyContactPhone\":\"13900009001\",\"credentialUrls\":\"https://example.invalid/rider-demo\"}")"
  application_id="$(printf '%s' "$application_response" | json_field "['id']")"
  admin_token="$(login_as_admin)"
  curl --fail --silent --show-error -X PATCH "$API_BASE_URL/rider/admin/applications/$application_id" -H 'Content-Type: application/json' -H "Authorization: Bearer $admin_token" --data '{"decision":"APPROVE","maxActiveOrders":3}' >/dev/null
  echo "Approved rider application: $phone"
}

ensure_rider '13900009004' '配送测试骑手一' '11010519491231002X'
ensure_rider '13900009005' '配送测试骑手二' '11010519491231003X'

# Keep both riders offline initially, but seed a nearby coordinate so browser location denial does not hide the demo task pool.
mysql "${MYSQL_ARGS[@]}" -e "
  UPDATE rider_profile
  SET online_status = 0, accepting_orders = 0,
      current_longitude = 116.397428, current_latitude = 39.909230,
      location_updated_at = NOW(), updated_at = NOW()
  WHERE user_id IN ('13900009004', '13900009005');
  SELECT 'approved_demo_riders' AS metric, COUNT(*) AS value FROM rider_profile WHERE user_id IN ('13900009004', '13900009005') AND status = 'APPROVED';
  SELECT 'demo_orders' AS metric, COUNT(*) AS value FROM orders WHERE remark LIKE 'RIDER_DEMO_%';
"
