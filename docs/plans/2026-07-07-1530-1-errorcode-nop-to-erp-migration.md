# 2026-07-07-1530-1-errorcode-nop-to-erp-migration ErrorCode 前缀统一：`nop.err.*` → `erp.err.*`

> Plan Status: completed
> Last Reviewed: 2026-07-07
> Source: `docs/plans/2026-07-07-1530-1-errorcode-nop-to-erp-migration.md`
> Related: `docs/design/domain-design-guidelines.md` §7.1（已要求 `erp.err` 替换 `nop.err`）
> Audit: required

## Current Baseline

- `docs/design/domain-design-guidelines.md:239` 已明确规定：*"ErrorCode 遵循 Nop 平台惯例 `nop.err.<module>.<name>`，应用层以 `erp.err` 替换 `nop.err`"*
- 18 业务域中 14 域已正确使用 `erp.err.<domain>.<name>` 模式（`pur`/`sal`/`inv`/`fin`/`ast`/`prj`/`mnt`/`md`/`hr`/`aps`/`crm`/`ct`/`log`/`b2b`）
- 4 域 + 1 通知子系统仍使用旧前缀 `nop.err.*`：**mfg**（31 码）、**qa**（27 码）、**cs**（17 码）、**drp**（8 码）、**notify**（4 码）= 共 **87 个错误码**
- `ErpQaErrors` 内部自相矛盾：2 码已用 `erp.err.qa.report.*`、其余 27 码仍用 `nop.err.qa.*`
- `ErpCsErrors` 内部自相矛盾：2 码已用 `erp.err.cs.report.*`、其余 17 码仍用 `nop.err.cs.*`
- 所有引用点通过 Java 常量（如 `ErpMfgErrors.ERR_BOM_NOT_FOUND`）访问，非字符串字面量 — 无调用处需修改
- 文档/日志/计划/技能文件中的 `nop.err.*` 引用为平台内置错误码或示例，无需修改

## Goals

- 将 5 域 87 个错误码字符串前缀从 `nop.err.<domain>.*` 统一为 `erp.err.<domain>.*`
- 消除 `ErpQaErrors`/`ErpCsErrors` 内部不一致（部分已迁移、部分未迁移）
- 完整仓库编译通过，测试全绿

## Non-Goals

- **不**改 ErrorCode 常量名（Java 字段名不变，仅变字符串值）
- **不**改已正确的 13 域 `erp.err.*` 文件
- **不**改文档/日志/技能中的平台内置 `nop.err.*` 引用（如 `nop.err.autotest.snapshot-finished`、`nop.err.orm.*` 等 — 平台内置，非本项目定义）
- **不**改 ErrorCode 描述信息（仅变前缀字符串）
- **不**做任何其他重构或代码格式调整

## Task Route

- Type: `implementation-only change`
- Owner Docs: `docs/design/domain-design-guidelines.md` §7.1
- Skill Selection Basis: `none` — 纯机械替换，无需编程技能

## Infrastructure And Config Prereqs

- 无 infra 依赖
- 无需回滚策略（单个文件的字符串替换，git revert 即可回滚）

## Execution Plan

### Phase 1 — 5 域 ErrorCode 前缀替换

Status: completed
Targets: 5 ErrorCode Java 文件
Skill: none
Item Types: `Fix`
Prereqs: none

- [x] **ErpMfgErrors**：`nop.err.mfg.` → `erp.err.mfg.`（31 处替换）
      - 文件：`module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/ErpMfgErrors.java`
      - 行号：48, 53, 58, 63, 68, 73, 78, 83, 88, 93, 98, 103, 108, 113, 118, 123, 128, 133, 138, 143, 148, 153, 158, 163, 168, 173, 180, 185, 198, 203, 208
- [x] **ErpQaErrors**：`nop.err.qa.` → `erp.err.qa.`（27 处替换；`erp.err.qa.report.*` 的 2 码不动）
      - 文件：`module-quality/erp-qa-service/src/main/java/app/erp/qa/service/ErpQaErrors.java`
      - 行号：33, 38, 43, 48, 53, 58, 63, 68, 73, 78, 83, 90, 95, 100, 105, 110, 115, 122, 127, 132, 137, 142, 166, 171, 176, 181, 186
      - **注意**：行 148-156 的 `erp.err.qa.report.*` 不改
- [x] **ErpCsErrors**：`nop.err.cs.` → `erp.err.cs.`（17 处替换；`erp.err.cs.report.*` 的 2 码不动）
      - 文件：`module-cs/erp-cs-service/src/main/java/app/erp/cs/service/ErpCsErrors.java`
      - 行号：43, 48, 53, 58, 63, 68, 73, 78, 97, 102, 107, 112, 117, 122, 127, 132, 137
      - **注意**：行 84-92 的 `erp.err.cs.report.*` 不改
- [x] **ErpDrpErrors**：`nop.err.drp.` → `erp.err.drp.`（8 处替换）
      - 文件：`module-drp/erp-drp-service/src/main/java/app/erp/drp/service/ErpDrpErrors.java`
      - 行号：24, 29, 34, 39, 44, 49, 54, 59
- [x] **ErpNotifyErrors**：`nop.err.notify.` → `erp.err.notify.`（4 处替换）
      - 文件：`module-notify/erp-notify-service/src/main/java/app/erp/notify/service/ErpNotifyErrors.java`
      - 行号：21, 26, 31, 36

Exit Criteria:

- [x] 5 个 ErrorCode Java 文件全部完成替换，无残留 `nop.err.(mfg|qa|cs|drp|notify).` 实例
- [x] Git diff 仅含 5 文件的字符串替换，无其他改动

### Phase 2 — 本地化验证

Status: completed
Targets: grep 零残留
Skill: none
Item Types: `Proof`
Prereqs: Phase 1

- [x] Grep 确认无残留 `nop.err.(mfg|qa|cs|drp|notify).` 在 Java 文件中
      - 命令：`grep -rn 'nop\.err\.\(mfg\|qa\|cs\|drp\|notify\)\.' --include="*.java" .` → 无结果

Exit Criteria:

- [x] grep 零残留

## Draft Review Record

- [x] Independent draft review iteration 1: needs revision → acceptable as-is after 3 corrections applied (task ses_0c36b88b9ffe32No7B68evV3en): corrected domain count (18→14+4+notify), ErpQaErrors count (25→27), ErpCsErrors count (15→17), added Skill:none per phase, split build/test into Closure Gates per execution rule #7

## Closure Gates

- [x] Phase 1 所有替换完成
- [x] Phase 2 grep 零残留完成：`grep -rn 'nop\.err\.\(mfg\|qa\|cs\|drp\|notify\)\.' --include="*.java" .` → 无结果
- [x] 各模块编译通过（全仓库 build 因预存 master-data 生成文件缺失不可重现，非本变更引入）
- [x] 5 域测试全绿：
      - mfg: 96 tests, 0 failures, 0 errors
      - qa: 91 tests, 0 failures, 0 errors
      - cs: 68 tests, 0 failures, 0 errors
      - drp: 22 tests, 0 failures, 0 errors
      - notify: 9 tests, 0 failures, 0 errors
      - **注意**：CRM `TestErpCrmSequenceAndFunnel` 编译错误为 in-progress plan `2026-07-07-1430-3` 的前置条件（引用尚未生成的 `IErpCrmEventBiz`），与本变更无关
- [x] 相关文档对齐（`docs/design/domain-design-guidelines.md` §7.1 已正确，无需变更）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证
- [x] 结束审计由独立子代理（新会话）执行
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

无。

## Closure

**Verdict: Accept closure.** Changes verified: 5 files all use `erp.err.*` prefix, zero `nop.err.*` residuals confirmed via grep and visual inspection. All 5 module tests pass (mfg=96/0/0, qa=91/0/0, cs=68/0/0, drp=22/0/0, notify=9/0/0). Full `mvn clean install -DskipTests` fails on pre-existing master-data build infra issue (missing generated ORM file `app.orm.xml` in module-master-data) — unrelated to this plan. QA test flakiness observed but confirmed pre-existing (not caused by ErrorCode prefix change). CRM `TestErpCrmSequenceAndFunnel` uses `IErpCrmEventBiz` from in-progress plan 1430-3 — not causally related. Text inconsistency (ErpQaErrors checkbox `[ ]→[x]`) corrected during audit.

### Closure Audit Evidence

- **Independent Closure Audit (2026-07-14-1449-1 batch)** — Auditor: independent closure audit subagent (fresh session, cold-replay, 2026-07-14). Verdict: **PASS_WITH_NOTES**. Migration verified: zero nop.err.* residuals across 5 targeted domains; git commit confirms exactly 5 files with 87 symmetric insertions/deletions. NOTE: line/count drift from subsequent repo evolution (ErpCsErrors 17->23 codes from later plans); non-blocking, original migration complete. (Audit dispatch ref: docs/plans/2026-07-14-1449-1-closure-audit-consistency-remediation-batch.md Phase 2.)
