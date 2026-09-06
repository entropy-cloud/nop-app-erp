---
status: draft
mission: ai-check-r3
work-item: MI.2
group: "2026-09-06-2104"
verify: [test]
---

# 2026-09-06-2104-2 MI.2 LOG 英文化批 1（finance + assets）

## Current Baseline

- 冻结基线（`docs/audits/cjk-baseline.md` §SNAPSHOT，2026-09-06）：CAT-1（LOG 语句含中文）finance = 54 行 / 21 文件，assets = 39 行 / 10 文件；文件级逐文件计数以该 SNAPSHOT `files:` 块为准（finance 21 文件含 `ErpFinAccountingPeriodProcessor` 12 行、`ErpFinPostingProcessor` 7 行、9 个过账 dispatcher 族文件；assets 10 文件含 9 个 `*PostingDispatcher` + 1 个折旧Processor）。
- 修复模式（`docs/architecture/i18n-compliance.md` 修复模式对照表 CAT-1 行）：LOG 消息模板改英文，保留 `{}` 占位参数与参数值不变，不走 i18n 机制。
- 行为不变式（roadmap 横切关注点 8 + `docs/lessons/09-posting-exception-swallow-suspension.md`）：过账 dispatcher 族 warn/error 降级消息语义不变——仅改消息载体字符串，禁止借机改吞异常行为、控制流、日志级别或参数求值顺序。
- 域内 CAT-2/CAT-3 计数（finance CAT2=23 / CAT3=80；assets CAT2=41 / CAT3=58）本批不触碰，归 MI.5a / MI.6（`AssetMergePostingDispatcher`/`AssetSplitPostingDispatcher` 的 CAT3:2 仅清 CAT-1 行）。
- 依赖状态：M0.6 done（2026-09-06 收官）；MI.2 依赖 M0.6，满足。
- 绿色基线：`docs/testing/known-good-baselines.md` 2026-09-06 `ai-check-r3-m0` 行（全 reactor 4006/0/0/1；CJK `--strict` 绿）。
- 剩余差距：两域 93 行 CAT-1 红线；`--strict` 门控下 actual 只降不升，本批目标 = 两域 CAT1 归零。

## Goals

- finance + assets CAT-1 归零（93 行 / 31 文件），LOG 消息模板全部英文化，语义与原中文消息等价。
- 两域模块 `mvn test` 全绿零回归；`node tools/check-hardcoded-cjk.mjs --strict` 保持绿且 finance/assets CAT1=0（单向收紧合法下降）。

## Non-Goals

- 不改业务行为、控制流、异常语义、日志级别（横切关注点 8）。
- 不动 CAT-2/3/4 面（MI.5a/5b/6/7/8 范围）；其余域 LOG 归 MI.3 / MI.4。
- 不动 `_init-data` seed、ORM 模型、页面文件。

## Phase 1 — 红线记录 + finance 扫清（21 文件 54 行）

> 统一类型：Fix-heavy（1 Proof 红线 + 1 Fix + 2 Proof）。
> Skill: none（roadmap MI.2 行指定）
> Targets: SNAPSHOT `files:` 块中 `module-finance/` 带 CAT1 计数的 21 个 Java 文件
> Prereqs: M0.6 done

- [ ] <Proof> 批第一动作：`node tools/check-hardcoded-cjk.mjs` 实跑，记录 finance CAT1=54 / assets CAT1=39 红线数字于本项勾选注记（修复批产物 = 脚本输出数字 + 归零断言，不产 ck-* 报告，roadmap 横切关注点 13）
      - Skill: none
- [ ] <Fix> finance 21 文件 LOG 消息模板逐行英文化：保留 `{}` 占位与参数实参不变；术语与各文件既有英文日志风格一致；`ErpFinAccountingPeriodProcessor`（12 行）与 9 个过账 dispatcher 族文件（`EmployeeAdvancePostingDispatcher`/`ExpenseClaimPostingDispatcher`/`NotesPostingDispatcher`/`ErpFinPostingProcessor` 等）的 warn/error 降级消息语义逐条比对不变
      - Skill: none
- [ ] <Proof> `node tools/check-hardcoded-cjk.mjs` 断言 finance 域 CAT1 = 0
      - Skill: none
- [ ] <Proof> `mvn test -pl module-finance/erp-fin-service`（含依赖模块 `-am`）全绿零回归
      - Skill: none

Exit Criteria:

- [ ] finance 域 CAT1 54 → 0（脚本断言，数字记入勾选注记）
- [ ] erp-fin-service 模块测试全绿，零新增失败

## Phase 2 — assets 扫清（10 文件 39 行）+ 批级归零证明

> 统一类型：Fix-heavy（1 Fix + 2 Proof）。
> Skill: none
> Targets: SNAPSHOT `files:` 块中 `module-assets/` 带 CAT1 计数的 10 个 Java 文件
> Prereqs: Phase 1 完成

- [ ] <Fix> assets 10 文件 LOG 消息模板逐行英文化（同 Phase 1 模式）；9 个 `*PostingDispatcher` 的 warn/error 降级消息语义不变（lesson 09）；`AssetMergePostingDispatcher`/`AssetSplitPostingDispatcher` 仅动 CAT-1 行、CAT-3 行保持原样
      - Skill: none
- [ ] <Proof> `node tools/check-hardcoded-cjk.mjs` 断言 assets 域 CAT1 = 0；`--strict` exit 0（finance/assets CAT1=0 落账，其余域与 CAT2/3/4 计数不高于快照）
      - Skill: none
- [ ] <Proof> `mvn test -pl module-assets/erp-ast-service`（含依赖模块 `-am`）全绿零回归
      - Skill: none

Exit Criteria:

- [ ] assets 域 CAT1 39 → 0；`--strict` 全绿且两域归零落账
- [ ] erp-ast-service 模块测试全绿，零新增失败

## Draft Review Record

## Verification

## Closure
