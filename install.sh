#!/bin/bash

set -euo pipefail

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

HOST_IP="${HOST_IP:-127.10.1.1}"
HOST_GATEWAY="${HOST_GATEWAY:-host.docker.internal}"
SHARED_NETWORK="${SHARED_NETWORK:-xmrpos-shared}"
HOST_IP_SLUG="${HOST_IP//./-}"
MONEROPAY_COMPOSE_PROJECT="${MONEROPAY_COMPOSE_PROJECT:-moneropay-${HOST_IP_SLUG}}"
XMRPOS_COMPOSE_PROJECT="${XMRPOS_COMPOSE_PROJECT:-xmrpos-${HOST_IP_SLUG}}"
DEFAULT_MONEROPAY_DIR="/root/moneropay-${HOST_IP_SLUG}"
DEFAULT_XMRPOS_DIR="/root/XMRpos-${HOST_IP_SLUG}"
LEGACY_MONEROPAY_DIR="/root/moneropay"
LEGACY_XMRPOS_DIR="/root/XMRpos"
MONEROPAY_SERVICE_HOST="${MONEROPAY_SERVICE_HOST:-${MONEROPAY_COMPOSE_PROJECT}-moneropay-1}"
MONEROPAY_WALLET_RPC_SERVICE_HOST="${MONEROPAY_WALLET_RPC_SERVICE_HOST:-${MONEROPAY_COMPOSE_PROJECT}-monero-wallet-rpc-1}"
XMRPOS_BACKEND_SERVICE_HOST="${XMRPOS_BACKEND_SERVICE_HOST:-${XMRPOS_COMPOSE_PROJECT}-backend-1}"

if [[ -z "${MONEROPAY_DIR:-}" ]]; then
    if [[ "$HOST_IP_SLUG" == "127-10-1-1" && -d "$LEGACY_MONEROPAY_DIR" && ! -d "$DEFAULT_MONEROPAY_DIR" ]]; then
        MONEROPAY_DIR="$LEGACY_MONEROPAY_DIR"
    else
        MONEROPAY_DIR="$DEFAULT_MONEROPAY_DIR"
    fi
fi

if [[ -z "${XMRPOS_DIR:-}" ]]; then
    if [[ "$HOST_IP_SLUG" == "127-10-1-1" && -d "$LEGACY_XMRPOS_DIR" && ! -d "$DEFAULT_XMRPOS_DIR" ]]; then
        XMRPOS_DIR="$LEGACY_XMRPOS_DIR"
    else
        XMRPOS_DIR="$DEFAULT_XMRPOS_DIR"
    fi
fi

XMRPOS_BACKEND_DIR="${XMRPOS_DIR}/XMRpos-backend"

MONERO_DAEMON_HOST="${MONERO_DAEMON_HOST:-node.monerodevs.org}"
MONERO_DAEMON_PORT="${MONERO_DAEMON_PORT:-18089}"
MONEROPAY_DB_USER="${MONEROPAY_DB_USER:-moneropay}"
MONEROPAY_DB_NAME="${MONEROPAY_DB_NAME:-moneropay}"
MONEROPAY_HOST_PORT="${MONEROPAY_HOST_PORT:-5000}"
MONEROPAY_WALLET_RPC_HOST_PORT="${MONEROPAY_WALLET_RPC_HOST_PORT:-18083}"
MONEROPAY_DB_HOST_PORT="${MONEROPAY_DB_HOST_PORT:-15432}"

XMRPOS_DB_USER="${XMRPOS_DB_USER:-xmrpos}"
XMRPOS_DB_NAME="${XMRPOS_DB_NAME:-xmrpos}"
BACKEND_PORT="${BACKEND_PORT:-8080}"
XMRPOS_DB_HOST_PORT="${XMRPOS_DB_HOST_PORT:-55432}"

MONEROPAY_BASE_URL_FOR_XMRPOS="${MONEROPAY_BASE_URL_FOR_XMRPOS:-http://${MONEROPAY_SERVICE_HOST}:5000}"
MONEROPAY_CALLBACK_URL_FOR_XMRPOS="${MONEROPAY_CALLBACK_URL_FOR_XMRPOS:-http://${XMRPOS_BACKEND_SERVICE_HOST}:8080/callback/receive/{jwt}}"
MONERO_WALLET_RPC_ENDPOINT_FOR_XMRPOS="${MONERO_WALLET_RPC_ENDPOINT_FOR_XMRPOS:-http://${MONEROPAY_WALLET_RPC_SERVICE_HOST}:28081/json_rpc}"

require_root() {
    if [[ "$(id -u)" -ne 0 ]]; then
        echo -e "${RED}Please run this script with sudo or as root.${NC}"
        exit 1
    fi
}

ensure_prereqs() {
    echo -e "${GREEN}Installing prerequisites...${NC}"
    apt-get update -qq
    DEBIAN_FRONTEND=noninteractive apt-get install -y -qq git curl ca-certificates jq openssl iproute2 >/dev/null
}

random_hex() {
    openssl rand -hex "${1:-32}"
}

update_env_var() {
    local file="$1" key="$2" value="$3"
    python3 - <<PY
from pathlib import Path
path = Path("$file")
lines = path.read_text().splitlines() if path.exists() else []
for idx,line in enumerate(lines):
    if line.startswith("$key="):
        lines[idx] = "$key=$value"
        break
else:
    lines.append("$key=$value")
path.write_text("\n".join(lines) + "\n")
PY
}

read_env_var() {
    local file="$1" key="$2"
    python3 - "$file" "$key" <<'PY'
import sys
from pathlib import Path

path = Path(sys.argv[1])
key = sys.argv[2]
if not path.exists():
    raise SystemExit(0)
prefix = f"{key}="
for line in path.read_text().splitlines():
    if line.startswith(prefix):
        print(line[len(prefix):])
        break
PY
}

update_compose_port() {
    local file="$1" service="$2" mapping="$3"
    python3 - "$file" "$service" "$mapping" <<'PY'
import sys
from pathlib import Path

file, service, mapping = sys.argv[1:]
path = Path(file)
lines = path.read_text().splitlines()
service_header = f"  {service}:"
inside = False
ports_idx = None
for idx, line in enumerate(lines):
    if line.startswith(service_header):
        inside = True
        continue
    if inside:
        if line.startswith("  ") and not line.startswith("    ") and line.strip():
            break
        if line.strip() == "ports:":
            ports_idx = idx
            break
if ports_idx is None:
    raise SystemExit(f"Ports block not found for service '{service}' in {file}")
line = f'      - "{mapping}"'
target_idx = ports_idx + 1
if target_idx >= len(lines):
    lines.append(line)
else:
    lines[target_idx] = line
path.write_text("\n".join(lines) + "\n")
PY
}

ensure_host_ip_on_loopback() {
    if ip -o addr show dev lo | awk '{print $4}' | grep -qx "${HOST_IP}/32"; then
        echo -e "${YELLOW}${HOST_IP}/32 already present on loopback interface.${NC}"
    else
        echo -e "${GREEN}Adding ${HOST_IP}/32 to loopback interface...${NC}"
        ip addr add "${HOST_IP}/32" dev lo
    fi
}

ensure_shared_network() {
    if docker network inspect "${SHARED_NETWORK}" >/dev/null 2>&1; then
        echo -e "${YELLOW}Docker network ${SHARED_NETWORK} already exists.${NC}"
    else
        echo -e "${GREEN}Creating shared Docker network ${SHARED_NETWORK}...${NC}"
        docker network create "${SHARED_NETWORK}" >/dev/null
    fi
}

remove_compose_project() {
    local project="$1"
    local label="$2"
    echo -e "${YELLOW}Removing Docker resources for ${label} project ${project}...${NC}"

    local containers=()
    mapfile -t containers < <(docker ps -aq --filter "label=com.docker.compose.project=${project}")
    if (( ${#containers[@]} )); then
        docker rm -f "${containers[@]}" >/dev/null 2>&1 || true
    fi

    local networks=()
    mapfile -t networks < <(docker network ls -q --filter "label=com.docker.compose.project=${project}")
    if (( ${#networks[@]} )); then
        docker network rm "${networks[@]}" >/dev/null 2>&1 || true
    fi

    local volumes=()
    mapfile -t volumes < <(docker volume ls -q --filter "label=com.docker.compose.project=${project}")
    if (( ${#volumes[@]} )); then
        docker volume rm "${volumes[@]}" >/dev/null 2>&1 || true
    fi
}

install_moneropay() {
    if [[ ! -d "$MONEROPAY_DIR" ]]; then
        echo -e "${GREEN}Cloning MoneroPay...${NC}"
        git clone https://gitlab.com/moneropay/moneropay.git "$MONEROPAY_DIR"
    fi

    cd "$MONEROPAY_DIR"

    [[ -f .env ]] || cp .env.example .env
    [[ -f docker-compose.override.yaml ]] || cp docker-compose.override.yaml.example docker-compose.override.yaml

    local postgres_password="${MONEROPAY_DB_PASSWORD:-}"
    if [[ -z "$postgres_password" ]]; then
        local existing_postgres_password
        existing_postgres_password="$(read_env_var ".env" "POSTGRES_PASSWORD" 2>/dev/null || true)"
        if [[ -n "$existing_postgres_password" ]]; then
            postgres_password="$existing_postgres_password"
        else
            postgres_password="$(random_hex 12)"
        fi
    fi

    ensure_shared_network

    update_env_var ".env" "MONERO_DAEMON_RPC_HOSTNAME" "$MONERO_DAEMON_HOST"
    update_env_var ".env" "MONERO_DAEMON_RPC_PORT" "$MONERO_DAEMON_PORT"
    update_env_var ".env" "MONERO_DAEMON_RPC_USERNAME" ""
    update_env_var ".env" "MONERO_DAEMON_RPC_PASSWORD" ""
    update_env_var ".env" "POSTGRES_USERNAME" "$MONEROPAY_DB_USER"
    update_env_var ".env" "POSTGRES_PASSWORD" "$postgres_password"
    update_env_var ".env" "POSTGRES_DATABASE" "$MONEROPAY_DB_NAME"
    update_env_var ".env" "ZERO_CONF" "true"
    update_env_var ".env" "LOG_FORMAT" "pretty"
    update_env_var ".env" "POLL_FREQUENCY" "1"

    cat > docker-compose.override.yaml <<EOF
services:
  moneropay:
    ports:
      - ${HOST_IP}:${MONEROPAY_HOST_PORT}:5000
    extra_hosts:
      - "host.docker.internal:host-gateway"
    networks:
      - default
      - ${SHARED_NETWORK}
  monero-wallet-rpc:
    ports:
      - ${HOST_IP}:${MONEROPAY_WALLET_RPC_HOST_PORT}:28081
    networks:
      - default
      - ${SHARED_NETWORK}
  postgresql:
    ports:
      - ${HOST_IP}:${MONEROPAY_DB_HOST_PORT}:5432
    networks:
      - default
      - ${SHARED_NETWORK}
networks:
  ${SHARED_NETWORK}:
    external: true
EOF

    echo -e "${GREEN}Starting MoneroPay stack...${NC}"
    COMPOSE_PROJECT_NAME="${MONEROPAY_COMPOSE_PROJECT}" docker compose up -d

    echo -e "${YELLOW}MoneroPay containers:${NC}"
    COMPOSE_PROJECT_NAME="${MONEROPAY_COMPOSE_PROJECT}" docker compose ps

    echo -e "${YELLOW}Waiting for MoneroPay health endpoint (takes about 30s)...${NC}"
    local health_url="http://${HOST_IP}:${MONEROPAY_HOST_PORT}/health"
    local attempts=10
    while (( attempts-- > 0 )); do
        if curl -fsS "${health_url}" >/tmp/moneropay-health 2>/dev/null; then
            cat /tmp/moneropay-health | jq .
            if jq -e '.status == 200 and .services.walletrpc == true' /tmp/moneropay-health >/dev/null 2>&1; then
                echo -e "${GREEN}Enabling wallet auto-refresh (2s) via RPC...${NC}"
                curl -s -H 'Content-Type: application/json' \
                    -d '{"jsonrpc":"2.0","id":"auto_refresh","method":"auto_refresh","params":{"enable":true,"period":2}}' \
                    "http://${HOST_IP}:${MONEROPAY_WALLET_RPC_HOST_PORT}/json_rpc" >/dev/null || true
            fi
            rm -f /tmp/moneropay-health
            return
        fi
        sleep 3
    done
    rm -f /tmp/moneropay-health 2>/dev/null || true
    echo -e "${RED}MoneroPay health endpoint not reachable yet. Check logs with 'cd ${MONEROPAY_DIR} && docker compose logs -f'.${NC}"
}

install_xmrpos() {
    if [[ ! -d "$XMRPOS_DIR" ]]; then
        echo -e "${GREEN}Cloning XMRpos...${NC}"
        git clone https://github.com/MoneroKon/XMRpos "$XMRPOS_DIR"
    fi

    cd "$XMRPOS_BACKEND_DIR"

    local env_path=".env"
    local existing_admin_name existing_admin_password existing_db_user existing_db_password existing_db_name
    local existing_jwt_secret existing_jwt_refresh_secret existing_jwt_moneropay_secret
    local existing_wallet_name existing_wallet_password existing_rpc_username existing_rpc_password existing_port

    existing_admin_name="$(read_env_var "$env_path" "ADMIN_NAME" 2>/dev/null || true)"
    existing_admin_password="$(read_env_var "$env_path" "ADMIN_PASSWORD" 2>/dev/null || true)"
    existing_db_user="$(read_env_var "$env_path" "DB_USER" 2>/dev/null || true)"
    existing_db_password="$(read_env_var "$env_path" "DB_PASSWORD" 2>/dev/null || true)"
    existing_db_name="$(read_env_var "$env_path" "DB_NAME" 2>/dev/null || true)"
    existing_jwt_secret="$(read_env_var "$env_path" "JWT_SECRET" 2>/dev/null || true)"
    existing_jwt_refresh_secret="$(read_env_var "$env_path" "JWT_REFRESH_SECRET" 2>/dev/null || true)"
    existing_jwt_moneropay_secret="$(read_env_var "$env_path" "JWT_MONEROPAY_SECRET" 2>/dev/null || true)"
    existing_wallet_name="$(read_env_var "$env_path" "WALLET_NAME" 2>/dev/null || true)"
    existing_wallet_password="$(read_env_var "$env_path" "WALLET_PASSWORD" 2>/dev/null || true)"
    existing_rpc_username="$(read_env_var "$env_path" "MONERO_WALLET_RPC_USERNAME" 2>/dev/null || true)"
    existing_rpc_password="$(read_env_var "$env_path" "MONERO_WALLET_RPC_PASSWORD" 2>/dev/null || true)"
    existing_port="$(read_env_var "$env_path" "PORT" 2>/dev/null || true)"

    local admin_name
    if [[ -n "${ADMIN_NAME:-}" ]]; then
        admin_name="$ADMIN_NAME"
    elif [[ -n "$existing_admin_name" ]]; then
        admin_name="$existing_admin_name"
    else
        admin_name="admin"
    fi

    local admin_password
    if [[ -n "${ADMIN_PASSWORD:-}" ]]; then
        admin_password="$ADMIN_PASSWORD"
    elif [[ -n "$existing_admin_password" ]]; then
        admin_password="$existing_admin_password"
    else
        admin_password="$(random_hex 8)"
    fi

    local db_user
    if [[ -n "${XMRPOS_DB_USER:-}" ]]; then
        db_user="$XMRPOS_DB_USER"
    elif [[ -n "$existing_db_user" ]]; then
        db_user="$existing_db_user"
    else
        db_user="xmrpos"
    fi

    local db_password
    if [[ -n "${XMRPOS_DB_PASSWORD:-}" ]]; then
        db_password="$XMRPOS_DB_PASSWORD"
    elif [[ -n "$existing_db_password" ]]; then
        db_password="$existing_db_password"
    else
        db_password="$(random_hex 16)"
    fi

    local db_name
    if [[ -n "${XMRPOS_DB_NAME:-}" ]]; then
        db_name="$XMRPOS_DB_NAME"
    elif [[ -n "$existing_db_name" ]]; then
        db_name="$existing_db_name"
    else
        db_name="xmrpos"
    fi

    local port_value="$BACKEND_PORT"
    if [[ -z "$port_value" && -n "$existing_port" ]]; then
        port_value="$existing_port"
    fi
    if [[ -z "$port_value" ]]; then
        port_value="8080"
    fi

    local jwt_secret
    if [[ -n "${JWT_SECRET:-}" ]]; then
        jwt_secret="$JWT_SECRET"
    elif [[ -n "$existing_jwt_secret" ]]; then
        jwt_secret="$existing_jwt_secret"
    else
        jwt_secret="$(random_hex 32)"
    fi

    local jwt_refresh_secret
    if [[ -n "${JWT_REFRESH_SECRET:-}" ]]; then
        jwt_refresh_secret="$JWT_REFRESH_SECRET"
    elif [[ -n "$existing_jwt_refresh_secret" ]]; then
        jwt_refresh_secret="$existing_jwt_refresh_secret"
    else
        jwt_refresh_secret="$(random_hex 32)"
    fi

    local jwt_moneropay_secret
    if [[ -n "${JWT_MONEROPAY_SECRET:-}" ]]; then
        jwt_moneropay_secret="$JWT_MONEROPAY_SECRET"
    elif [[ -n "$existing_jwt_moneropay_secret" ]]; then
        jwt_moneropay_secret="$existing_jwt_moneropay_secret"
    else
        jwt_moneropay_secret="$(random_hex 32)"
    fi

    local wallet_name
    if [[ -n "${WALLET_NAME:-}" ]]; then
        wallet_name="$WALLET_NAME"
    elif [[ -n "$existing_wallet_name" ]]; then
        wallet_name="$existing_wallet_name"
    else
        wallet_name="wallet"
    fi

    local wallet_password
    if [[ -n "${WALLET_PASSWORD:-}" ]]; then
        wallet_password="$WALLET_PASSWORD"
    else
        wallet_password="$existing_wallet_password"
    fi

    local rpc_username="$existing_rpc_username"
    if [[ -n "${MONERO_WALLET_RPC_USERNAME:-}" ]]; then
        rpc_username="$MONERO_WALLET_RPC_USERNAME"
    fi

    local rpc_password="$existing_rpc_password"
    if [[ -n "${MONERO_WALLET_RPC_PASSWORD:-}" ]]; then
        rpc_password="$MONERO_WALLET_RPC_PASSWORD"
    fi

    cat > "$env_path" <<EOF
ADMIN_NAME=${admin_name}
ADMIN_PASSWORD=${admin_password}

PORT=${port_value}

DB_HOST=db
DB_USER=${db_user}
DB_PASSWORD=${db_password}
DB_NAME=${db_name}
DB_PORT=5432

JWT_SECRET=${jwt_secret}
JWT_REFRESH_SECRET=${jwt_refresh_secret}
JWT_MONEROPAY_SECRET=${jwt_moneropay_secret}

MONEROPAY_BASE_URL=${MONEROPAY_BASE_URL_FOR_XMRPOS}
MONEROPAY_CALLBACK_URL=${MONEROPAY_CALLBACK_URL_FOR_XMRPOS}

MONERO_WALLET_RPC_ENDPOINT=${MONERO_WALLET_RPC_ENDPOINT_FOR_XMRPOS}
MONERO_WALLET_RPC_USERNAME=${rpc_username}
MONERO_WALLET_RPC_PASSWORD=${rpc_password}

WALLET_NAME=${wallet_name}
WALLET_PASSWORD=${wallet_password}
EOF

    update_compose_port "docker-compose.yaml" "backend" "${HOST_IP}:${BACKEND_PORT}:8080"
    update_compose_port "docker-compose.yaml" "db" "${HOST_IP}:${XMRPOS_DB_HOST_PORT}:5432"

    cat > docker-compose.override.yaml <<EOF
services:
  backend:
    networks:
      - xmrnet
      - ${SHARED_NETWORK}
networks:
  ${SHARED_NETWORK}:
    external: true
EOF

    echo -e "${GREEN}Building XMRpos backend image...${NC}"
    COMPOSE_PROJECT_NAME="${XMRPOS_COMPOSE_PROJECT}" docker compose build --no-cache

    echo -e "${GREEN}Starting XMRpos stack...${NC}"
    COMPOSE_PROJECT_NAME="${XMRPOS_COMPOSE_PROJECT}" docker compose up -d

    echo -e "${YELLOW}XMRpos containers:${NC}"
    COMPOSE_PROJECT_NAME="${XMRPOS_COMPOSE_PROJECT}" docker compose ps

    echo -e "${YELLOW}Waiting for XMRpos health endpoint...${NC}"
    local health_url="http://${HOST_IP}:${BACKEND_PORT}/misc/health"
    sleep 5
    local attempts=10
    while (( attempts-- > 0 )); do
        if curl -fsS "${health_url}" >/tmp/xmrpos-health 2>/dev/null; then
            cat /tmp/xmrpos-health | jq .
            rm -f /tmp/xmrpos-health
            return
        fi
        sleep 3
    done
    rm -f /tmp/xmrpos-health 2>/dev/null || true
    echo -e "${RED}XMRpos health endpoint not reachable yet. Check logs with 'cd ${XMRPOS_BACKEND_DIR} && docker compose logs -f'.${NC}"
}

install_all() {
    require_root
    ensure_prereqs
    install_moneropay
    install_xmrpos
    ensure_host_ip_on_loopback
    echo -e "${GREEN}Combined MoneroPay + XMRpos installation complete.${NC}"
}

clean_all() {
    require_root
    echo -e "${RED}WARNING: This will remove MoneroPay and XMRpos, including wallets under ${MONEROPAY_DIR}/data/wallet and ~/wallets. Make sure you have backups before continuing.${NC}"
    read -r -p "Type 'delete' to confirm cleanup: " response
    if [[ "$response" != "delete" ]]; then
        echo -e "${YELLOW}Cleanup cancelled.${NC}"
        exit 0
    fi

    remove_compose_project "${XMRPOS_COMPOSE_PROJECT}" "XMRpos"
    remove_compose_project "${MONEROPAY_COMPOSE_PROJECT}" "MoneroPay"

    declare -A seen_dirs=()
    for dir in "$XMRPOS_DIR" "$DEFAULT_XMRPOS_DIR"; do
        if [[ -n "$dir" && -d "$dir" && -z "${seen_dirs[$dir]:-}" ]]; then
            echo -e "${YELLOW}Removing XMRpos directory ${dir}...${NC}"
            rm -rf "$dir"
            seen_dirs["$dir"]=1
        fi
    done
    if [[ "$HOST_IP_SLUG" == "127-10-1-1" && -d "$LEGACY_XMRPOS_DIR" && -z "${seen_dirs[$LEGACY_XMRPOS_DIR]:-}" ]]; then
        echo -e "${YELLOW}Removing legacy XMRpos directory ${LEGACY_XMRPOS_DIR}...${NC}"
        rm -rf "$LEGACY_XMRPOS_DIR"
        seen_dirs["$LEGACY_XMRPOS_DIR"]=1
    fi

    declare -A seen_moneropay_dirs=()
    for dir in "$MONEROPAY_DIR" "$DEFAULT_MONEROPAY_DIR"; do
        if [[ -n "$dir" && -d "$dir" && -z "${seen_moneropay_dirs[$dir]:-}" ]]; then
            echo -e "${YELLOW}Removing MoneroPay directory ${dir}...${NC}"
            rm -rf "$dir"
            seen_moneropay_dirs["$dir"]=1
        fi
    done
    if [[ "$HOST_IP_SLUG" == "127-10-1-1" && -d "$LEGACY_MONEROPAY_DIR" && -z "${seen_moneropay_dirs[$LEGACY_MONEROPAY_DIR]:-}" ]]; then
        echo -e "${YELLOW}Removing legacy MoneroPay directory ${LEGACY_MONEROPAY_DIR}...${NC}"
        rm -rf "$LEGACY_MONEROPAY_DIR"
        seen_moneropay_dirs["$LEGACY_MONEROPAY_DIR"]=1
    fi

    if [[ -d "/root/wallets" ]]; then
        echo -e "${YELLOW}Removing wallet directory /root/wallets...${NC}"
        rm -rf /root/wallets
    fi

    echo -e "${GREEN}Cleanup complete. System is ready for a fresh installation.${NC}"
}

case "${1:-install}" in
    install)
        install_all
        ;;
    clean)
        clean_all
        ;;
    *)
        echo -e "${RED}Usage: $0 [install|clean]${NC}"
        exit 1
        ;;
esac
