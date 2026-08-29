#!/usr/bin/env bash
# One-time setup: derives ~/.clamped/config.properties from .env, so the
# server/CLI/demo jars can be run directly with plain `java -jar ...` -
# no wrapper script, no env vars needed per-session.
#
# Usage: ./scripts/setup-config.sh
set -e

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="$REPO_ROOT/.env"

if [ ! -f "$ENV_FILE" ]; then
    echo "Error: .env not found - copy .env.example to .env and fill in real values first." >&2
    exit 1
fi

set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

CONFIG_DIR="$HOME/.clamped"
CONFIG_FILE="$CONFIG_DIR/config.properties"
mkdir -p "$CONFIG_DIR"

cat > "$CONFIG_FILE" <<EOF
jdbcUrl=jdbc:postgresql://localhost:5432/${POSTGRES_DB:-clamped_db}
username=${POSTGRES_USER:-postgres}
password=${POSTGRES_PASSWORD}
EOF

echo "Wrote $CONFIG_FILE"
echo "You can now run any of the jars directly, e.g.:"
echo "  java -jar clamped-server/target/clamped-server-1.0.0-SNAPSHOT.jar"
