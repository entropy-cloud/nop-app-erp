#!/bin/bash
# tools/mission-driver.sh - Mission driver launcher
#
# Usage:
#   ./mission-driver.sh <mission>                  Run full mission-driver flow
#   ./mission-driver.sh <mission> --list-steps     List available top-level steps
#   ./mission-driver.sh <mission> --step <STEP>    Run single step only
#   ./mission-driver.sh --list-missions            List available missions
#   ./mission-driver.sh --draft-mission <desc>     Generate a new mission.json
#
# <mission> is the name in missions/<mission>.json (e.g. "mall").
#
# The mission-driver engine lives in the AGE template; this launcher only points
# to it. Override the location with MISSION_DRIVER_HOME if needed.

DIR="$(cd "$(dirname "$0")" && pwd | tr -d '\r')"
MISSION_DRIVER_HOME="${MISSION_DRIVER_HOME:-$DIR/../../attractor-guided-engineering-template/tools/mission-driver}"

exec node "$MISSION_DRIVER_HOME/src/main.js" \
  --dir "$DIR/.." \
  --missions-dir "missions" \
  "$@"
