#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8090}"
ADMIN_EMAIL="${ADMIN_EMAIL:-}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-}"

if [[ -z "$ADMIN_EMAIL" || -z "$ADMIN_PASSWORD" ]]; then
  echo "Smoke test requires ADMIN_EMAIL and ADMIN_PASSWORD in the environment." >&2
  exit 2
fi

BASE_URL="${BASE_URL%/}"
RESPONSE_FILE="$(mktemp)"
trap 'rm -f "$RESPONSE_FILE"' EXIT
SENSITIVE_VALUES=("$ADMIN_EMAIL" "$ADMIN_PASSWORD")

# Keep failure output useful while removing credentials and generated identifiers.
remember_sensitive() {
  SENSITIVE_VALUES+=("$@")
}

fail_call() {
  local step="$1" expected="$2" actual="$3" response
  response="$(head -c 300 "$RESPONSE_FILE" 2>/dev/null || true)"
  local secret
  for secret in "${SENSITIVE_VALUES[@]}"; do
    [[ -z "$secret" ]] || response="${response//"$secret"/[REDACTED]}"
  done
  printf '%s: expected %s but got %s\n' "$step" "$expected" "$actual" >&2
  [[ -z "$response" ]] || printf 'Response: %s\n' "$response" >&2
  exit 1
}

# Make transient gateway and discovery failures retryable without retrying API errors.
call() {
  local step="$1" method="$2" path="$3" token="$4" body="$5" expected="$6"
  local attempt=0 status
  local -a curl_args=(-sS -o "$RESPONSE_FILE" -w '%{http_code}' --max-time 60 -X "$method")

  [[ -z "$token" ]] || curl_args+=(-H "Authorization: Bearer $token")
  [[ -z "$body" ]] || curl_args+=(-H 'Content-Type: application/json' --data "$body")

  while true; do
    attempt=$((attempt + 1))
    status="$(curl "${curl_args[@]}" "${BASE_URL}${path}" 2>/dev/null)" || status=000
    [[ -n "$status" ]] || status=000

    if [[ "$status" == "$expected" ]]; then
      printf 'OK %s\n' "$step"
      return 0
    fi

    if [[ "$status" == 000 || "$status" == 502 || "$status" == 503 || "$status" == 504 ]]; then
      if (( attempt <= 18 )); then
        printf 'retrying %s (status %s)\n' "$step" "$status"
        sleep 10
        continue
      fi
    fi
    fail_call "$step" "$expected" "$status"
  done
}

# Use jq for response checks and extraction so response bodies stay out of the log.
assert_json() {
  local expression="$1" step="$2" message="$3"
  shift 3
  if ! jq -e "$@" "$expression" "$RESPONSE_FILE" >/dev/null; then
    printf '%s assertion failed: %s\n' "$step" "$message" >&2
    exit 1
  fi
}

read_json() {
  local expression="$1" step="$2" value
  value="$(jq -er "$expression" "$RESPONSE_FILE")" || {
    printf '%s assertion failed: expected response field was missing.\n' "$step" >&2
    exit 1
  }
  printf '%s' "$value"
}

# Generate isolated records for every run, including an employee joining next week.
SUFFIX="$(date +%s)$RANDOM"
JOINING_DATE="$(date -u -d '+7 days' +%F)"
PLAIN_EMAIL="plain.${SUFFIX}@example.test"
PLAIN_PASSWORD="PlainSmoke${SUFFIX}"
HR_EMAIL="hr.${SUFFIX}@example.test"
HR_PASSWORD="HrSmoke${SUFFIX}"
IT_EMAIL="it.${SUFFIX}@example.test"
IT_PASSWORD="ItSmoke${SUFFIX}"
ASSET_SERIAL="SMOKE-${SUFFIX}"
EMPLOYEE_EMAIL="smoke.${SUFFIX}@example.test"
remember_sensitive "$PLAIN_EMAIL" "$PLAIN_PASSWORD" "$HR_EMAIL" "$HR_PASSWORD" \
  "$IT_EMAIL" "$IT_PASSWORD" "$ASSET_SERIAL" "$EMPLOYEE_EMAIL"

call 'gateway health' GET /actuator/health '' '' 200
assert_json '.status == "UP"' 'gateway health' 'health status is not UP'

call 'employee list requires authentication' GET /api/employees '' '' 401

body="$(jq -n --arg email "$ADMIN_EMAIL" --arg password "$ADMIN_PASSWORD" '{email:$email,password:$password}')"
call 'bootstrap admin login' POST /api/auth/login '' "$body" 200
ADMIN_TOKEN="$(read_json '.token' 'bootstrap admin login')"
remember_sensitive "$ADMIN_TOKEN"

body="$(jq -n --arg fullName 'Smoke Employee' --arg email "$PLAIN_EMAIL" --arg password "$PLAIN_PASSWORD" '{fullName:$fullName,email:$email,password:$password}')"
call 'public employee registration' POST /api/auth/register '' "$body" 201
body="$(jq -n --arg email "$PLAIN_EMAIL" --arg password "$PLAIN_PASSWORD" '{email:$email,password:$password}')"
call 'plain employee login' POST /api/auth/login '' "$body" 200
EMPLOYEE_TOKEN="$(read_json '.token' 'plain employee login')"
remember_sensitive "$EMPLOYEE_TOKEN"

body="$(jq -n --arg fullName 'Forbidden HR Registration' --arg email "forbidden.${SUFFIX}@example.test" --arg password "$PLAIN_PASSWORD" '{fullName:$fullName,email:$email,password:$password,role:"HR"}')"
remember_sensitive "forbidden.${SUFFIX}@example.test"
call 'public HR registration is forbidden' POST /api/auth/register '' "$body" 403

body="$(jq -n --arg fullName 'Smoke HR' --arg email "$HR_EMAIL" --arg password "$HR_PASSWORD" '{fullName:$fullName,email:$email,password:$password,role:"HR"}')"
call 'admin creates HR user' POST /api/users "$ADMIN_TOKEN" "$body" 201
body="$(jq -n --arg fullName 'Smoke IT' --arg email "$IT_EMAIL" --arg password "$IT_PASSWORD" '{fullName:$fullName,email:$email,password:$password,role:"IT"}')"
call 'admin creates IT user' POST /api/users "$ADMIN_TOKEN" "$body" 201

body="$(jq -n --arg email "$HR_EMAIL" --arg password "$HR_PASSWORD" '{email:$email,password:$password}')"
call 'HR login' POST /api/auth/login '' "$body" 200
HR_TOKEN="$(read_json '.token' 'HR login')"
remember_sensitive "$HR_TOKEN"
body="$(jq -n --arg email "$IT_EMAIL" --arg password "$IT_PASSWORD" '{email:$email,password:$password}')"
call 'IT login' POST /api/auth/login '' "$body" 200
IT_TOKEN="$(read_json '.token' 'IT login')"
remember_sensitive "$IT_TOKEN"
call 'user list forbidden for HR' GET /api/users "$HR_TOKEN" '' 403
call 'admin lists users' GET /api/users "$ADMIN_TOKEN" '' 200

asset_body="$(jq -n --arg serialNumber "$ASSET_SERIAL" '{type:"LAPTOP",make:"Dell",model:"Latitude 5440",serialNumber:$serialNumber}')"
call 'IT creates laptop asset' POST /api/assets "$IT_TOKEN" "$asset_body" 201
assert_json '.assetTag | test("^LAP-")' 'IT creates laptop asset' 'asset tag does not start with LAP-'
assert_json '.status == "AVAILABLE"' 'IT creates laptop asset' 'new asset is not AVAILABLE'
ASSET_ID="$(read_json '.id' 'IT creates laptop asset')"
ASSET_TAG="$(read_json '.assetTag' 'IT creates laptop asset')"
call 'HR cannot create asset' POST /api/assets "$HR_TOKEN" "$asset_body" 403
call 'duplicate asset serial conflicts' POST /api/assets "$IT_TOKEN" "$asset_body" 409

employee_body="$(jq -n --arg email "$EMPLOYEE_EMAIL" --arg joiningDate "$JOINING_DATE" '{fullName:"Smoke Test",email:$email,department:"ENGINEERING",jobTitle:"Backend Engineer",joiningDate:$joiningDate,requiredAssetTypes:["LAPTOP"]}')"
call 'HR creates employee checklist' POST /api/employees "$HR_TOKEN" "$employee_body" 201
assert_json '.employeeCode | test("^EMP-")' 'HR creates employee checklist' 'employee code does not start with EMP-'
assert_json '(.tasks | length) == 6' 'HR creates employee checklist' 'checklist does not contain six tasks'
assert_json '[.tasks[] | select(.category == "EQUIPMENT")] | length == 1' 'HR creates employee checklist' 'expected exactly one equipment task'
EMPLOYEE_ID="$(read_json '.id' 'HR creates employee checklist')"
EQUIPMENT_TASK_ID="$(read_json '[.tasks[] | select(.category == "EQUIPMENT")][0].id' 'HR creates employee checklist')"
call 'duplicate employee email conflicts' POST /api/employees "$HR_TOKEN" "$employee_body" 409
call 'IT cannot create employee' POST /api/employees "$IT_TOKEN" "$employee_body" 403

assignment_body="$(jq -n --argjson assetId "$ASSET_ID" '{assetId:$assetId}')"
assignment_path="/api/onboarding/tasks/${EQUIPMENT_TASK_ID}/assign-asset"
call 'HR cannot assign asset' POST "$assignment_path" "$HR_TOKEN" "$assignment_body" 403
call 'IT assigns asset to equipment task' POST "$assignment_path" "$IT_TOKEN" "$assignment_body" 200
assert_json '.status == $status' 'IT assigns asset to equipment task' 'task is not DONE' --arg status 'DONE'
assert_json '.assetTag == $assetTag' 'IT assigns asset to equipment task' 'task asset tag does not match' --arg assetTag "$ASSET_TAG"
call 'completed task cannot be assigned again' POST "$assignment_path" "$IT_TOKEN" "$assignment_body" 409

call 'IT reads assigned asset' GET "/api/assets/${ASSET_ID}" "$IT_TOKEN" '' 200
assert_json '.status == "ASSIGNED"' 'IT reads assigned asset' 'asset is not ASSIGNED'
call 'IT reads assignment history' GET "/api/assets/${ASSET_ID}/history" "$IT_TOKEN" '' 200
assert_json 'length == 1' 'IT reads assignment history' 'expected one history entry'

call 'HR reads employee status' GET "/api/employees/${EMPLOYEE_ID}" "$HR_TOKEN" '' 200
assert_json '.status == "ONBOARDING"' 'HR reads employee status' 'employee status is not ONBOARDING'
call 'employee cannot read another employee record' GET "/api/employees/${EMPLOYEE_ID}" "$EMPLOYEE_TOKEN" '' 404

call 'HR reads onboarding dashboard' GET /api/onboarding/dashboard "$HR_TOKEN" '' 200
call 'IT reads asset summary' GET /api/assets/summary "$IT_TOKEN" '' 200
assert_json '.assigned >= 1 and (.byType | length) == 6' 'IT reads asset summary' 'expected an assigned asset and six asset types'

call 'IT returns asset' POST "/api/assets/${ASSET_ID}/return" "$IT_TOKEN" '' 200
assert_json '.status == "AVAILABLE"' 'IT returns asset' 'asset did not return to AVAILABLE'
call 'available asset cannot be returned again' POST "/api/assets/${ASSET_ID}/return" "$IT_TOKEN" '' 409

printf 'SMOKE TEST PASSED\n'
