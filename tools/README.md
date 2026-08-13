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
- `check-bigint-id-types.mjs`: BIGINT 主键/外键 `stdDataType` 检查与批量修改工具。规则：BIGINT 主键/外键必须显式 `stdDataType="string"`（JS long 精度问题，见 nop-entropy `docs-for-ai/02-core-guides/orm-model-design.md` 主键设计节）。
  - `node tools/check-bigint-id-types.mjs scan [root]` — 盘点所有 module-* / model 下 orm.xml 的 PK/FK 列（含 to-one join 交叉校验，0 告警即查找完整）
  - `node tools/check-bigint-id-types.mjs dry-run [root]` — 生成修改副本到 `_tmp/bigint-id-string-fix/`，xmllint 校验 XML、重扫确认零残留与幂等，不修改源文件
  - `node tools/check-bigint-id-types.mjs apply --yes` — 审核副本后回写源文件（ORM 增量重生成 + 全量验证后方可提交）

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
