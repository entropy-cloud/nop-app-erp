# Tools Workspace

`tools/` is an independent pnpm subproject for repository-local engineering utilities.

The template root is intentionally not a Node.js project. This keeps the copied template usable for non-Node repositories while still allowing optional Node-based tooling.

The scripts in this directory inspect the parent repository.

## Install

Run from `tools/`:

```bash
pnpm install
```

## Tool Selection Rule

Files kept in this directory should satisfy at least one of these conditions:

- generic enough to be useful across many copied projects
- representative enough to serve as a reusable example pattern

Do not keep one-off migration scripts, repo-specific cleanup scripts, or tools that mainly encode a single team's naming policy.

## Core Tools

- `check-active-doc-code-anchors.mjs`: validate repo paths referenced in active docs
- `check-oversized-code-files.mjs`: flag tracked code files that exceed line thresholds
- `check-docs-garbled.mjs`: scan docs for suspicious Unicode and mojibake
- `parse-nop-errors.mjs`: parse a Nop server log, dedupe & summarize structured errors (`errorCode` + `@_loc` file:line). See `docs/lessons/05-nop-e2e-failure-log-first-diagnosis.md`. Run: `pnpm parse:nop-errors -- <logfile> [--recent] [--grep PAT]`

These are lightweight, generic, and reasonable to keep enabled by default.

## Example Tools

- `check-duplicates.mjs`: wrap `jscpd` for copy-paste detection
- `code-stats.mjs`: print code and docs statistics
- `audit/`: example rule-based audit scanner plus starter rules

These are kept as representative examples of reusable tooling patterns, not as mandatory policy for every copied project.

## Common Commands

Run from `tools/`:

```bash
pnpm check
pnpm stats
pnpm check:duplicates
pnpm audit:suspects
```

## Mission Driver

This project drives development via the AGE template's **mission-driver** — a mission-driven loop engine (health-check → execute plans → draft plans → review plans → deep audit). The engine source lives in the template (`attractor-guided-engineering-template/tools/mission-driver/`); this repo holds only a thin launcher plus per-project mission configs, and does not maintain a local copy.

- `tools/mission-driver.sh` — launcher; resolves `MISSION_DRIVER_HOME` to the template and forwards args. Override the location with the `MISSION_DRIVER_HOME` env var.
- `missions/<name>.json` — per-project mission config (paths + commands). See the template's `mission.json.example` and `design/mission-design.md` for the full schema.

Commands (k8s-style subcommands):

```bash
./tools/mission-driver.sh run <mission>              # run the full flow
./tools/mission-driver.sh run <mission> --step <S>   # run a single step
./tools/mission-driver.sh draft "<description>"      # AI-generate a mission.json
./tools/mission-driver.sh list [missions|steps]      # list (default: missions)
```

Run `./tools/mission-driver.sh --help` for all options.

## Configuration

- `check-active-doc-code-anchors.mjs`
  Uses `AGE_REPO_ROOT`, `AGE_ACTIVE_DOC_ROOTS`, and `AGE_ACTIVE_DOC_FILES`.
- `check-oversized-code-files.mjs`
  Uses `AGE_OVERSIZED_WARN_LINES`, `AGE_OVERSIZED_ERROR_LINES`, and `AGE_CODE_ROOT_PREFIXES`.
- `check-duplicates.mjs`
  Uses `AGE_DUPLICATE_SCAN_ROOTS`.
- `audit/`
  Uses `AGE_AUDIT_ROOTS`.
