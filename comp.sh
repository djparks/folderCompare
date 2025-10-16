#!/usr/bin/env bash
set -euo pipefail

# Determine script directory (project root)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

JAR="target/folderCompare-1.0-SNAPSHOT-shaded.jar"

if [[ ! -f "$JAR" ]]; then
  echo "Shaded JAR not found, building..."
  ./mvnw -q -DskipTests package
fi

echo "Running folderCompare..."
exec java -jar "$JAR" "$@"
