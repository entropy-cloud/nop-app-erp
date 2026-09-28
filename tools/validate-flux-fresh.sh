#!/usr/bin/env bash
# validate-flux-fresh.sh — GATE-03 verification-chain freshness wrapper (lessons/26 stale-jar defense).
#
# Owner doc : docs/architecture/flux-page-export-and-validation.md（validate:flux:fresh 包装节）
# Source    : docs/backlog/perf-ux-debt-consolidation-roadmap.md GATE-03
# Plan      : docs/plans/2026-09-28-1735-1-gate03-validate-flux-fresh.md
#
# Chains「touched-module mvn install → npm run validate:flux」then asserts artifact freshness:
#   A1    REPORT parses, totals.validated > 0; MANIFEST parses, pageCount > 0
#   A2    MANIFEST.failedPages length == 0
#   A3    every tracked+untracked page-family source file is NOT newer than REPORT nor MANIFEST
#         (whole-tree strong form — any source edit after the last full chain run = stale)
#   CHAIN (full-chain mode) REPORT/MANIFEST mtime >= chain start — artifacts were produced by
#         THIS run; an mvn failure exiting rc=1 must not masquerade as the known-325 family
#
# The wrapped `npm run validate:flux` semantics are NOT modified: its exit code is captured
# (0 clean / 1 known adjudicated baseline family per owner doc §7 / >=2 environment error).
#
# Usage:
#   bash tools/validate-flux-fresh.sh [--check-only] [module ...]
#     module        Maven module dir to reinstall (mvn -pl <module> install -DskipTests).
#                   Without args, touched modules derive from git-dirty files (nearest pom.xml
#                   ancestor; root/aggregator packaging=pom hits are skipped with a counted
#                   note; module-<domain>/model/ hits print the ORM protected-area notice).
#   --check-only   skip mvn + validate:flux; assert freshness of existing artifacts only.
#                  Only valid right after a full chain run or a full manual build — never
#                  after a bare validate:flux (lesson 26 case-2 revival path).
#   --self-test    sandbox fault injection in /tmp (never touches the repo tree).
#
# Env overrides (self-test only): VALIDATE_FRESH_ROOT, REPORT_FILE, MANIFEST_FILE
#
# Exit codes: 0 = fresh & chain clean; 1 = policy violation (stale / failedPages>0 /
#             validate:flux rc=1 known baseline); 2 = environment/chain failure.
#
# bash 3.2 compatible (darwin): no declare -A / mapfile / ${var,,}; no empty-array expansion
# under set -u (module lists are newline strings, not arrays).

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
DEFAULT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
REPO_ROOT="${VALIDATE_FRESH_ROOT:-$DEFAULT_ROOT}"
REPORT_FILE="${REPORT_FILE:-$REPO_ROOT/_tmp/flux-page-validation-report.json}"
MANIFEST_FILE="${MANIFEST_FILE:-$REPO_ROOT/app-erp-all/target/flux-pages/manifest.json}"

VRC_WAS_ONE=0

mtime_of() {  # portable mtime (darwin first, GNU fallback)
  stat -f %m "$1" 2>/dev/null || stat -c %Y "$1" 2>/dev/null || echo 0
}

say_pass() { echo "  PASS  $1"; }
say_fail() { echo "  FAIL  $1"; SELFTEST_PASS=0; }

# ---------------------------------------------------------------------------
# self-test (sandboxed; short-circuits before any mvn/npm invocation)
# ---------------------------------------------------------------------------
run_self_test() {
  SELFTEST_PASS=1
  local SB REPORT MANIFEST SRC rc
  SB="$(mktemp -d "${TMPDIR:-/tmp}/vff-selftest.XXXXXX")"
  git -C "$SB" init -q 2>/dev/null || git -C "$SB" init
  mkdir -p "$SB/module-x/erp-x-web/src/main/resources/_vfs" "$SB/_tmp" "$SB/app-erp-all/target/flux-pages"

  REPORT="$SB/_tmp/flux-page-validation-report.json"
  MANIFEST="$SB/app-erp-all/target/flux-pages/manifest.json"
  SRC="$SB/module-x/erp-x-web/src/main/resources/_vfs/A.view.xml"

  printf '<view/>' > "$SRC"
  printf '{"totals":{"files":1,"validated":1,"errors":325,"warnings":0,"skipped":0,"strippedXui":0}}' > "$REPORT"
  printf '{"pageCount":1,"failedPages":[],"generatedAt":"t"}' > "$MANIFEST"

  # scenario 1: older sources than artifacts -> exit 0
  touch -t 202601010000 "$SRC"
  touch -t 202601020000 "$REPORT" "$MANIFEST"
  if ( cd "$SB" && VALIDATE_FRESH_ROOT="$SB" REPORT_FILE="$REPORT" MANIFEST_FILE="$MANIFEST" \
        bash "$SCRIPT_DIR/validate-flux-fresh.sh" --check-only >/dev/null 2>&1 ); then
    say_pass "scenario1 fresh artifacts pass (exit 0)"
  else
    say_fail "scenario1 fresh artifacts pass (exit 0)"
  fi

  # scenario 2: newer source than artifacts -> exit 1 stale
  touch "$SRC"
  rc=0
  ( cd "$SB" && VALIDATE_FRESH_ROOT="$SB" REPORT_FILE="$REPORT" MANIFEST_FILE="$MANIFEST" \
      bash "$SCRIPT_DIR/validate-flux-fresh.sh" --check-only >/dev/null 2>&1 ) || rc=$?
  if [ "$rc" = "1" ]; then say_pass "scenario2 newer source detected stale (exit 1)"
  else say_fail "scenario2 newer source detected stale (exit 1, got $rc)"; fi

  # scenario 3: failedPages non-empty -> exit 1
  touch -t 202601010000 "$SRC"
  touch -t 202601020000 "$REPORT" "$MANIFEST"
  printf '{"pageCount":3,"failedPages":["a.page.json"],"generatedAt":"t"}' > "$MANIFEST"
  rc=0
  ( cd "$SB" && VALIDATE_FRESH_ROOT="$SB" REPORT_FILE="$REPORT" MANIFEST_FILE="$MANIFEST" \
      bash "$SCRIPT_DIR/validate-flux-fresh.sh" --check-only >/dev/null 2>&1 ) || rc=$?
  if [ "$rc" = "1" ]; then say_pass "scenario3 failedPages>0 (exit 1)"
  else say_fail "scenario3 failedPages>0 (exit 1, got $rc)"; fi

  # scenario 4: totals missing -> exit 2
  printf '{"nope":1}' > "$REPORT"
  printf '{"pageCount":1,"failedPages":[]}' > "$MANIFEST"
  rc=0
  ( cd "$SB" && VALIDATE_FRESH_ROOT="$SB" REPORT_FILE="$REPORT" MANIFEST_FILE="$MANIFEST" \
      bash "$SCRIPT_DIR/validate-flux-fresh.sh" --check-only >/dev/null 2>&1 ) || rc=$?
  if [ "$rc" = "2" ]; then say_pass "scenario4 totals missing (exit 2)"
  else say_fail "scenario4 totals missing (exit 2, got $rc)"; fi

  # scenario 5: artifacts absent -> exit 2
  rm -f "$REPORT" "$MANIFEST"
  rc=0
  ( cd "$SB" && VALIDATE_FRESH_ROOT="$SB" REPORT_FILE="$REPORT" MANIFEST_FILE="$MANIFEST" \
      bash "$SCRIPT_DIR/validate-flux-fresh.sh" --check-only >/dev/null 2>&1 ) || rc=$?
  if [ "$rc" = "2" ]; then say_pass "scenario5 artifacts absent (exit 2)"
  else say_fail "scenario5 artifacts absent (exit 2, got $rc)"; fi

  rm -rf "$SB"
  if [ "$SELFTEST_PASS" = "1" ]; then echo "RESULT: PASS (self-test green)"; return 0; fi
  echo "RESULT: FAIL (self-test detected fake-green risk)"; return 1
}

# ---------------------------------------------------------------------------
# touched-module derivation (git-dirty files -> nearest pom.xml ancestor)
# emits lines: MODULE <path> | SKIP <reason> | ORM-NOTE <hint>
# ---------------------------------------------------------------------------
derive_modules() {
  local line f base d cand
  git -C "$REPO_ROOT" status --porcelain 2>/dev/null | while IFS= read -r line; do
    base="$(printf '%s' "${line:3}" | sed -e "s/^'//" -e "s/'\$//")"
    case "$line" in
      *" -> "*) base="${base##* -> }" ;;
    esac
    [ -z "$base" ] && continue
    f="$REPO_ROOT/$base"
    [ -e "$f" ] || f="$REPO_ROOT/$(echo "$base" | cut -d/ -f1)"  # deleted file: fall back to top dir
    d="$(dirname "$f")"
    cand=""
    while [ "$d" != "/" ]; do
      if [ -f "$d/pom.xml" ]; then
        if [ "$d" = "$REPO_ROOT" ]; then cand="ROOT"; else cand="$d"; fi
        break
      fi
      d="$(dirname "$d")"
    done
    if [ -z "$cand" ] || [ "$cand" = "ROOT" ]; then
      echo "SKIP $base (no module pom ancestor outside root pom)"
      continue
    fi
    case "$base" in
      module-*/model/*)
        echo "ORM-NOTE $base (ORM source model change -> protected-area full 'mvn clean install -DskipTests' obligation, not automated here)"
        ;;
    esac
    if grep -q '<packaging>pom</packaging>' "$cand/pom.xml"; then
      echo "SKIP $base (aggregator/parent pom ancestor installs no resources: ${cand#"$REPO_ROOT"/})"
      continue
    fi
    echo "MODULE ${cand#"$REPO_ROOT"/}"
  done
}

# ---------------------------------------------------------------------------
# freshness assertions A1/A2/A3 (shared by both modes)
# return 0 pass / 1 policy violation / 2 environment failure
# ---------------------------------------------------------------------------
assert_freshness() {
  # A1: artifacts present & parseable & populated
  if [ ! -f "$REPORT_FILE" ]; then echo "ERROR: report missing: $REPORT_FILE" >&2; return 2; fi
  if [ ! -f "$MANIFEST_FILE" ]; then echo "ERROR: manifest missing: $MANIFEST_FILE" >&2; return 2; fi

  local probe fp live_errors
  probe="$(node -e '
    const fs = require("fs");
    try {
      const r = JSON.parse(fs.readFileSync(process.argv[1], "utf8"));
      const m = JSON.parse(fs.readFileSync(process.argv[2], "utf8"));
      if (!r.totals || typeof r.totals.validated !== "number") { console.log("ERR report totals.validated missing"); process.exit(0); }
      if (!(r.totals.validated > 0)) { console.log("ERR totals.validated not > 0"); process.exit(0); }
      if (typeof m.pageCount !== "number" || !(m.pageCount > 0)) { console.log("ERR manifest pageCount not > 0"); process.exit(0); }
      if (!Array.isArray(m.failedPages)) { console.log("ERR manifest failedPages not array"); process.exit(0); }
      console.log("OK " + m.failedPages.length + " " + r.totals.errors);
    } catch (e) { console.log("ERR " + e.message); }
  ' "$REPORT_FILE" "$MANIFEST_FILE")"
  case "$probe" in
    OK\ *) : ;;
    ERR*) echo "ERROR: artifact assertion failed: $probe" >&2; return 2 ;;
    *) echo "ERROR: artifact assertion crashed" >&2; return 2 ;;
  esac
  fp="${probe#OK }"
  live_errors="${fp#* }"
  fp="${fp%% *}"

  # A2: failedPages == 0
  if [ "$fp" != "0" ]; then
    echo "manifest failedPages             : $fp"
    echo "RESULT: FAIL (A2: manifest failedPages=$fp — export layer unhealthy, lessons/26 telltale)"
    return 1
  fi

  # A3: whole-tree page-family freshness
  local src stale_count stale_samples f mtime_src
  src="$(git -C "$REPO_ROOT" ls-files --cached --others --exclude-standard 2>/dev/null \
    | grep -E '\.(view\.xml|page\.yaml|flux\.yaml)$' | grep '/src/main/resources/' | grep -v '/target/' || true)"
  local total=0
  if [ -n "$src" ]; then total="$(printf '%s\n' "$src" | grep -c .)"; fi
  stale_count=0
  stale_samples=""
  if [ -n "$src" ]; then
    while IFS= read -r f; do
      [ -z "$f" ] && continue
      if [ -f "$REPO_ROOT/$f" ]; then
        mtime_src="$(mtime_of "$REPO_ROOT/$f")"
        if [ "$mtime_src" -gt 0 ] && { [ "$mtime_src" -gt "$(mtime_of "$REPORT_FILE")" ] || [ "$mtime_src" -gt "$(mtime_of "$MANIFEST_FILE")" ]; }; then
          stale_count=$((stale_count + 1))
          if [ "$stale_count" -le 10 ]; then
            stale_samples="${stale_samples}  $f (mtime $mtime_src)
"
          fi
        fi
      fi
    done <<< "$src"
  fi

  echo "scanned page-family source files : $total"
  echo "manifest failedPages             : $fp"
  echo "report totals.errors (live)      : $live_errors"
  if [ "$stale_count" -gt 0 ]; then
    echo "RESULT: FAIL (A3: $stale_count page-family source file(s) newer than artifacts — stale verification chain, lessons/26)"
    printf '%s' "$stale_samples"
    return 1
  fi
  return 0
}

# ---------------------------------------------------------------------------
# main
# ---------------------------------------------------------------------------
main() {
  echo "========================================================================"
  echo "validate-flux-fresh (GATE-03 — lessons/26 stale-jar defense; wraps npm run validate:flux, semantics untouched)"

  local mode_check=0
  local args_mode="" first=1
  local a modules="" line mod
  for a in "$@"; do
    case "$a" in
      --check-only) mode_check=1 ;;
      --self-test) run_self_test; return $? ;;
      *)
        if [ "$first" = "1" ]; then modules="$a"; first=0; else modules="$modules
$a"; fi
        ;;
    esac
  done

  if [ "$mode_check" = "1" ]; then
    echo "WARNING: --check-only is only valid right after a full chain run or a full manual build."
    echo "         Never treat it as a completion gate after a bare validate:flux (lesson 26 case-2)."
    echo "(check-only: skipping mvn install and validate:flux)"
  else
    local chain_start
    chain_start="$(date +%s)"

    if [ -z "$modules" ]; then
      # derive touched modules from git-dirty files
      while IFS= read -r line; do
        case "$line" in
          MODULE*) mod="${line#MODULE }"; if [ -z "$modules" ]; then modules="$mod"; else modules="$modules
$mod"; fi ;;
          SKIP*) echo "derive-skip: $line" ;;
          ORM-NOTE*) echo "derive-note: $line" ;;
        esac
      done < <(derive_modules)
      echo "derived touched modules from git-dirty files"
    fi

    if [ -n "$modules" ]; then
      while IFS= read -r mod; do
        [ -z "$mod" ] && continue
        echo "[install] mvn -pl $mod install -DskipTests ..."
        local mrc=0
        (cd "$REPO_ROOT" && mvn -pl "$mod" install -DskipTests -q) || mrc=$?
        if [ "$mrc" != "0" ]; then
          echo "ERROR: module install failed (rc=$mrc): $mod — chain did not complete" >&2
          return 2
        fi
      done <<< "$modules"
    else
      echo "no touched modules to reinstall (dirty files are all non-module paths)"
    fi

    echo "[validate] npm run validate:flux (exit code captured, semantics untouched) ..."
    local vrc=0
    (cd "$REPO_ROOT" && npm run validate:flux) || vrc=$?
    if [ "$vrc" -ge 2 ]; then
      echo "ERROR: validate:flux environment failure (rc=$vrc)" >&2
      return 2
    fi
    if [ "$vrc" = "1" ]; then VRC_WAS_ONE=1; fi

    # CHAIN completion assertion: artifacts must be products of THIS run
    local f mt
    for f in "$REPORT_FILE" "$MANIFEST_FILE"; do
      if [ ! -f "$f" ]; then
        echo "ERROR: chain did not complete — artifact missing: $f" >&2
        return 2
      fi
      mt="$(mtime_of "$f")"
      if [ "$mt" -lt "$chain_start" ]; then
        echo "ERROR: chain did not complete — artifact mtime ($mt) older than chain start ($chain_start); the chain failed before regenerating it: $f" >&2
        return 2
      fi
    done
  fi

  local frc=0 rc_final=0
  assert_freshness || frc=$?
  if [ "$frc" = "2" ]; then return 2; fi
  if [ "$frc" != "0" ]; then rc_final=1; fi
  if [ "$VRC_WAS_ONE" = "1" ]; then rc_final=1; fi

  if [ "$rc_final" = "0" ]; then
    echo "RESULT: PASS (freshness green; validate:flux rc=0)"
  elif [ "$VRC_WAS_ONE" = "1" ]; then
    echo "RESULT: exit 1 (validate:flux rc=1 — known 325 baseline family, own gate semantics; freshness assertions above)"
  else
    echo "RESULT: exit 1 (freshness violations above)"
  fi
  return "$rc_final"
}

main "$@"
exit $?
