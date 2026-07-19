#!/bin/bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
APP_DIR="$ROOT_DIR/app-erp-all"
DUMP_DIR="$ROOT_DIR/_dump"

echo "=== Removing _dump directory ==="
if [ -d "$DUMP_DIR" ]; then
  rm -rf "$DUMP_DIR"
  echo "Removed: $DUMP_DIR"
else
  echo "Not found (already clean): $DUMP_DIR"
fi

RUNNER="$APP_DIR/target/app-erp-all-1.0-SNAPSHOT-runner.jar"
if [ ! -f "$RUNNER" ]; then
  echo "ERROR: Runner jar not found at $RUNNER"
  echo "Run 'mvn clean install -DskipTests' first."
  exit 1
fi

echo ""
echo "=== Starting app-erp-all ==="
echo "Runner: $RUNNER"
echo "WorkDir: $ROOT_DIR"
echo ""

cd "$ROOT_DIR"
exec java -Dfile.encoding=UTF8 \
  -Dquarkus.profile=dev \
  -jar "$RUNNER"
