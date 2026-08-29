#!/usr/bin/env bash
set -e

MAVEN_VERSION=3.9.6
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MAVEN_HOME="$DIR/.mvn/apache-maven-$MAVEN_VERSION"
MAVEN_BIN="$MAVEN_HOME/bin/mvn"

if [ ! -x "$MAVEN_BIN" ]; then
    echo "Downloading Maven $MAVEN_VERSION..."
    mkdir -p "$DIR/.mvn"
    ZIP="$DIR/.mvn/maven.zip"
    curl -fsSL -o "$ZIP" "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$MAVEN_VERSION/apache-maven-$MAVEN_VERSION-bin.zip"
    unzip -q -o "$ZIP" -d "$DIR/.mvn"
    rm -f "$ZIP"
    chmod +x "$MAVEN_BIN"

    if [ ! -x "$MAVEN_BIN" ]; then
        echo "Failed to set up Maven. Check your internet connection."
        exit 1
    fi
    echo "Maven ready."
fi

exec "$MAVEN_BIN" "$@"
