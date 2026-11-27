#!/usr/bin/env bash
set -euo pipefail

if [[ ! -r /etc/os-release ]]; then
  echo "Cannot identify this operating system: /etc/os-release is unavailable." >&2
  exit 1
fi

# shellcheck disable=SC1091
source /etc/os-release
if [[ "${ID:-}" != "ubuntu" ]]; then
  echo "Unsupported operating system '${ID:-unknown}'; this bootstrap supports Ubuntu only." >&2
  exit 1
fi

case "${VERSION_ID:-}" in
  24.04|26.04) ;;
  *)
    if [[ "${ALLOW_UNTESTED_OS:-0}" != "1" ]]; then
      echo "Untested Ubuntu version '${VERSION_ID:-unknown}'. Set ALLOW_UNTESTED_OS=1 to continue anyway." >&2
      exit 1
    fi
    echo "Warning: proceeding with untested Ubuntu ${VERSION_ID:-unknown} because ALLOW_UNTESTED_OS=1."
    ;;
esac

if [[ "${EUID}" -eq 0 ]]; then
  SUDO=()
  TARGET_USER="${SUDO_USER:-${USER:-root}}"
else
  if ! command -v sudo >/dev/null 2>&1 || ! sudo -v; then
    echo "Run this script as a sudo-capable user on a fresh Ubuntu server." >&2
    exit 1
  fi
  SUDO=(sudo)
  TARGET_USER="${USER}"
fi

"${SUDO[@]}" apt-get update
"${SUDO[@]}" apt-get install -y docker.io git curl jq

if apt-cache show docker-compose-v2 >/dev/null 2>&1; then
  COMPOSE_PACKAGE=docker-compose-v2
elif apt-cache show docker-compose-plugin >/dev/null 2>&1; then
  COMPOSE_PACKAGE=docker-compose-plugin
else
  echo "Neither docker-compose-v2 nor docker-compose-plugin is available from the configured apt repositories." >&2
  exit 1
fi
"${SUDO[@]}" apt-get install -y "$COMPOSE_PACKAGE"

"${SUDO[@]}" systemctl enable --now docker
"${SUDO[@]}" usermod -aG docker "$TARGET_USER"

if [[ -z "$(swapon --noheadings --show=NAME)" ]]; then
  if [[ ! -f /swapfile ]]; then
    "${SUDO[@]}" fallocate -l 2G /swapfile
  elif [[ "$(stat -c '%s' /swapfile)" -ne 2147483648 ]]; then
    "${SUDO[@]}" fallocate -l 2G /swapfile
  fi
  "${SUDO[@]}" chmod 600 /swapfile
  "${SUDO[@]}" mkswap -f /swapfile
  "${SUDO[@]}" swapon /swapfile
  if ! grep -qE '^/swapfile[[:space:]]' /etc/fstab; then
    echo '/swapfile none swap sw 0 0' | "${SUDO[@]}" tee -a /etc/fstab >/dev/null
  fi
fi

echo 'vm.swappiness=10' | "${SUDO[@]}" tee /etc/sysctl.d/99-onboardhub.conf >/dev/null
"${SUDO[@]}" sysctl --system >/dev/null

echo 'Bootstrap complete.'
docker --version
docker compose version
printf 'Next steps:\n'
printf '  1. Log out and back in so user %s receives docker group membership.\n' "$TARGET_USER"
printf '  2. Clone the OnboardHub repository and configure docker/.env.\n'
printf '  3. Run bash scripts/deploy.sh.\n'
