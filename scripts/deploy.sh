#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
cd "$REPO_ROOT"

ENV_FILE="${REPO_ROOT}/docker/.env"
COMPOSE_FILE="${REPO_ROOT}/docker/docker-compose.prod.yml"
if [[ ! -f "$ENV_FILE" ]]; then
  echo "Production environment file is missing: docker/.env (copy docker/.env.prod.example first)." >&2
  exit 1
fi
if grep -Fq 'replace_with_' "$ENV_FILE"; then
  echo "docker/.env still contains replace_with_ placeholders; replace them before deployment." >&2
  exit 1
fi

NO_PULL=false
if [[ "$#" -gt 1 ]]; then
  echo "Usage: bash scripts/deploy.sh [--no-pull]" >&2
  exit 2
fi
if [[ "$#" -eq 1 ]]; then
  if [[ "$1" == "--no-pull" ]]; then
    NO_PULL=true
  else
    echo "Unknown option: $1 (supported option: --no-pull)." >&2
    exit 2
  fi
fi

COMPOSE=(docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE")
API_DOMAIN="$(awk -F= '$1 == "API_DOMAIN" { sub(/\r$/, "", $2); print $2; exit }' "$ENV_FILE")"
if [[ -z "$API_DOMAIN" ]]; then
  echo "API_DOMAIN is missing from docker/.env." >&2
  exit 1
fi

wait_healthy() {
  local service="$1" container_id status attempt
  container_id="$("${COMPOSE[@]}" ps -q "$service")"
  if [[ -z "$container_id" ]]; then
    echo "No container was created for service '$service'." >&2
    "${COMPOSE[@]}" logs --tail 50 "$service" || true
    return 1
  fi

  for attempt in $(seq 1 60); do
    status="$(docker inspect -f '{{.State.Health.Status}}' "$container_id" 2>/dev/null || true)"
    if [[ "$status" == "healthy" ]]; then
      printf '%s is healthy.\n' "$service"
      return 0
    fi
    sleep 5
  done

  printf "Timed out waiting for service '%s' to become healthy.\n" "$service" >&2
  "${COMPOSE[@]}" logs --tail 50 "$service" || true
  return 1
}

if [[ "$NO_PULL" != true ]]; then
  echo 'Pulling production images.'
  "${COMPOSE[@]}" pull
fi

echo 'Starting database and service discovery.'
"${COMPOSE[@]}" up -d postgres discovery-server
wait_healthy postgres
wait_healthy discovery-server

echo 'Starting business services.'
"${COMPOSE[@]}" up -d auth-team-service asset-inventory-service employee-onboarding-service
wait_healthy auth-team-service
wait_healthy asset-inventory-service
wait_healthy employee-onboarding-service

echo 'Starting API gateway.'
"${COMPOSE[@]}" up -d api-gateway
wait_healthy api-gateway

echo 'Starting Caddy.'
"${COMPOSE[@]}" up -d caddy
wait_healthy caddy

"${COMPOSE[@]}" ps
echo 'Checking gateway health inside the Compose network.'
"${COMPOSE[@]}" exec -T api-gateway wget -qO- http://127.0.0.1:8090/actuator/health
printf '\nPublic health URL: https://%s/actuator/health\n' "$API_DOMAIN"
echo 'The gateway may answer 503 for about a minute while service discovery propagates.'
