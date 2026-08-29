#!/usr/bin/env bash
# Installs a `clamped` command on PATH that works from any shell
# (bash, zsh, fish) - not a shell-specific profile function.
#
# Usage: ./scripts/install-cli.sh
set -e

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
JAR_PATH="$REPO_ROOT/clamped-cli/target/clamped-cli-1.0.0-SNAPSHOT.jar"

if [ ! -f "$JAR_PATH" ]; then
    echo "Error: $JAR_PATH not found - build first: ./mvnw -pl clamped-core,clamped-server,clamped-cli,clamped-demo -am package -DskipTests" >&2
    exit 1
fi

BIN_DIR="$HOME/.local/bin"
mkdir -p "$BIN_DIR"

SHIM_PATH="$BIN_DIR/clamped"
cat > "$SHIM_PATH" <<EOF
#!/usr/bin/env bash
exec java -jar "$JAR_PATH" "\$@"
EOF
chmod +x "$SHIM_PATH"

echo "Installed shim: $SHIM_PATH"

case ":$PATH:" in
    *":$BIN_DIR:"*)
        echo "$BIN_DIR is already on PATH."
        ;;
    *)
        echo ""
        echo "$BIN_DIR is not on your PATH yet. Add this line to your shell config"
        echo "(~/.zshrc, ~/.bashrc, or ~/.bash_profile) and open a new terminal:"
        echo ""
        echo "  export PATH=\"\$HOME/.local/bin:\$PATH\""
        echo ""
        ;;
esac

echo "Once on PATH, 'clamped list' works from any shell."
