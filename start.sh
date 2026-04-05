#!/bin/bash
set -e

# Create the ~/.clamped directory and generate config.properties from
# environment variables so that any component that reads the file directly
# (e.g. the CLI or DemoApp) also works in the Railway environment.

CONFIG_DIR="$HOME/.clamped"
CONFIG_FILE="$CONFIG_DIR/config.properties"

mkdir -p "$CONFIG_DIR"

# Validate required environment variables and provide clear error messages.
if [ -z "$CLAMPED_JDBC_URL" ]; then
  echo "WARNING: CLAMPED_JDBC_URL is not set. Database connection may fail." >&2
fi

if [ -z "$CLAMPED_USERNAME" ]; then
  echo "WARNING: CLAMPED_USERNAME is not set. Defaulting to 'postgres'." >&2
  CLAMPED_USERNAME="postgres"
fi

if [ -z "$CLAMPED_PASSWORD" ]; then
  echo "WARNING: CLAMPED_PASSWORD is not set. Defaulting to empty string." >&2
  CLAMPED_PASSWORD=""
fi

# Write config.properties from environment variables.
cat > "$CONFIG_FILE" <<EOF
jdbcUrl=${CLAMPED_JDBC_URL}
username=${CLAMPED_USERNAME}
password=${CLAMPED_PASSWORD}
EOF

echo "Config written to $CONFIG_FILE"

# Launch the Spring Boot server.
exec java -Ddemo.mode=true -jar clamped-server/target/clamped-server-1.0.0-SNAPSHOT.jar
