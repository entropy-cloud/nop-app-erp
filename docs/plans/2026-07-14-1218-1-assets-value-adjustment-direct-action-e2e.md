# 2026-07-14-1218-1-assets-value-adjustment-direct-action-e2e assets 资产减值/重估 useApproval DIRECT 业务动作 + 凭证行数值浏览器层 E2E

> Plan Status: completed
> Mission: erp
> Work Item: assets VALUE_ADJUSTMENT DIRECT 审批轴 E2E（0215-1 Deferred successor）
> Last Reviewed: 2026-07-14
> Source: `docs/plans/2026-07-14-0215-1-assets-direct-action-e2e.md` Deferred「资产减值/重估审批轴 E2E（VALUE_ADJUSTMENT）」（Successor Required: yes，触发条件「按域推进 DIRECT useApproval 剩余实体浏览器层覆盖时」——已满足：assets 域 4 条 DIRECT 路径已由 0215-1 + 0742-1 落地，VALUE_ADJUSTMENT 为 assets 域最后一个 useApproval DIRECT 未覆盖实体）
> Related: `2026-07-14-0215-1`（assets DIRECT E2E 范式源）、`2026-07-14-0742-1`（assets 凭证行数值断言范式源）、`2026-07-10-0335-1`（useApproval DIRECT 审批轴 E2E 范式源）
> Audit: required

## Current Baseline

assets 域 VALUE_ADJUSTMENT 后端已全部落地（extended-roadmap 2.14 done，计划 `2026-07-05-0540-3`）：

- **实体**：`ErpAstValueAdjustment`（`module-assets/model/app-erp-assets.orm.xml`），`tagSet="gid,erp.assets,use-approval"`，**无 `useWorkflow`** → DIRECT 审批轴，浏览器层可达
- **三轴状态机**：docStatus（ACTIVE/CANCELLED）+ approveStatus（UNSUBMITTED/SUBMITTED/APPROVED/REJECTED）+ posted
- **审批动作**（经 xbiz → `ErpAstValueAdjustmentProcessor`）：`submitForApproval` / `approve` / `reject` / `reverseApprove` / `withdrawApproval` + `cancel`（Java `@BizMutation`）
- **三种调整类型**（`adjustmentType` 字典 `erp-ast/adjustment-type`）：
  - IMPAIRMENT（减值）：Dr 6702 资产减值损失 / Cr 1604 固定资产减值准备
  - REVALUATION_UP（重估增值）：Dr 1601 固定资产 / Cr 4002 资本公积
  - REVALUATION_DOWN（重估减值）：Dr 6702 资产减值损失 / Cr 1601 固定资产
- **过账**：`ValueAdjustmentPostingDispatcher` → `ValueAdjustmentAcctDocProvider`，businessType=`VALUE_ADJUSTMENT`(390)，billHeadCode=`adjustment.code`
- **净值联动**：approve 时按 adjustmentType 调整资产净值/折旧基数（减值/重估减值减少，重估增值增加，config-gated `erp-ast.revaluation-adjust-depreciation-base`）
- **反向红冲**：`reverseApprove` 红字凭证 + 回退净值/折旧基数

**浏览器层 E2E 缺口**：VALUE_ADJUSTMENT 零 E2E 覆盖（0215-1 Non-Goal → Deferred successor，0742-1 亦未覆盖）。assets 域其余 4 条 DIRECT 路径（折旧/CIP/盘点/维修）已由 0215-1 + 0742-1 落地生命周期 + 凭证行数值断言。

**种子科目缺口**：种子 `erp_md_subject.csv` 当前含 1601/1602/1603/6301/6602（0215-1 补齐）。VALUE_ADJUSTMENT 过账额外需 6702（资产减值损失）/ 1604（固定资产减值准备）/ 4002（资本公积），当前缺失，需补齐。

**E2E 基础设施就绪**：`tests/e2e/business-actions/_helper.ts` 三原语 `createViaSave`/`callMutation`/`verifyState` + `tests/e2e/orchestration/_helper.ts` `findVoucherIdByBillCode`/`assertVoucherLines` 凭证行断言范式经 14 域验证可复用。assets 域种子数据已有（asset_category/asset/depreciation_schedule，2210-1）。

## Goals

- assets VALUE_ADJUSTMENT useApproval DIRECT 审批轴经 GraphQL `/graphql` 浏览器层全栈可达性 + 三轴状态机迁移验证
- 覆盖 3 种调整类型（IMPAIRMENT / REVALUATION_UP / REVALUATION_DOWN）的 submit→approve→posted 正向链 + 凭证行精确数值断言（subjectCode + dcDirection + debitAmount/creditAmount）
- 断言 reverseApprove 红冲凭证行同向取负 + 净值联动回退 + cancel 异常路径 + 非法迁移守卫
- 复用既有 useApproval DIRECT 审批轴范式（0335-1）+ 凭证行数值断言范式（0742-1）验证在 assets 多型调整类型下的可复用性

## Non-Goals

- **`ErpAstDisposal` 资产处置 E2E**（`useWorkflow="true"` xwf）——经 2330-1 权威裁决浏览器层不可行，排除
- **资产拆分/合并 E2E**（2.5d）——仅 `cancel` 单动作 + 低价值，0215-1 裁定 `Successor Required: no`
- **折旧基数重算精确数值断言**——config-gated `erp-ast.revaluation-adjust-depreciation-base` 联动折旧重算属折旧引擎深化面，本计划仅断言净值联动方向（增/减），不验证重算后折旧计划条目精确金额
- **多账套 VALUE_ADJUSTMENT 并行传播**——SchemaPropagator 多账套传播为杠杆 B 既有能力，非 VALUE_ADJUSTMENT 特有

## Task Route

- Type: `verification work`（扩展现有 Playwright E2E 套件覆盖至 assets VALUE_ADJUSTMENT DIRECT 审批轴 + 凭证行数值断言）
- Owner Docs: `docs/design/assets/state-machine.md`（资产状态机）、`docs/design/assets/README.md`（资产域概览）、`docs/testing/e2e-runbook.md`（业务动作套件 + 调用范式）
- Skill Selection Basis: 浏览器层 E2E 测试编写（Playwright + GraphQL mutation 驱动 @BizMutation）→ 无匹配技能（`nop-testing` 技能面向 JunitAutoTestCase 后端快照测试，非 Playwright 浏览器层）；沿用 `tests/e2e/business-actions/_helper.ts` 既有范式 → `Skill: none`

## Infrastructure And Config Prereqs

No infra prereqs beyond existing baseline。复用现有 Playwright 配置 + webServer JVM 参数。

> 种子 `erp_md_subject.csv` 需补齐 3 科目行（6702 资产减值损失 / 1604 固定资产减值准备 / 4002 资本公积），按 0215-1/0413-2 范式。VALUE_ADJUSTMENT 过账 Provider 硬编码默认科目码（`ValueAdjustmentAcctDocProvider` 常量 `SUBJECT_IMPAIRMENT_LOSS="6702"` 等），经 `findByCode` 解析，无 webServer JVM arg 追加。

## Execution Plan

### Phase 1 - 三种调整类型生命周期 + 凭证行数值断言

Status: completed
Targets: `tests/e2e/business-actions/ast-value-adjustment.action.spec.ts`（新建）
Skill: none

- Item Types: `Add | Proof | Decision`
- Prereqs: 无（自包含 setup）

- [x] `Decision | Explore`: 裁定 VALUE_ADJUSTMENT 过账科目依赖——`ValueAdjustmentAcctDocProvider` 读 3 组硬编码默认科目码（6702/1604/1601/4002）。Explore 实测种子 `erp_md_subject.csv` 缺 6702/1604/4002，按 0215-1 范式补齐 3 行（id=35/36/37）。**关键裁定：`ValueAdjustmentPostingDispatcher` 从 `ErpAstAssetCategory` 字段解析科目码（`expenseSubjectId`→IMPAIRMENT_LOSS、`depreciationSubjectId`→IMPAIRMENT_PROVISION），fallback 到硬编码默认值。测试复用种子 category id=1（AST-CAT-IT）三 subject 字段均 null（CSV 未设）→ fallback 默认值生效，凭证行科目码 = 6702/1604/1601/4002 默认值。** 净值联动 config-gate `erp-ast.revaluation-adjust-depreciation-base` 默认 true（重算折旧基数），本计划仅断言 `netBookValue` 方向（增/减），不验证重算后折旧计划条目精确金额。裁定结果记入执行日志。
  - Skill: none
- [x] `Add`: **VALUE_ADJUSTMENT 3 种调整类型 spec** `ast-value-adjustment.action.spec.ts`
  - 自包含建 `ErpAstAsset`（IN_SERVICE + 原值 12000 + 累计折旧 0 + 净值 12000 + categoryId=1 种子 category 三 subject 字段 null 使过账 Provider fallback 到硬编码默认科目码 6702/1604/1601/4002）+ `ErpAstValueAdjustment`（DRAFT/UNSUBMITTED 入口）
  - **IMPAIRMENT 减值路径**：`submitForApproval`(UNSUBMITTED→SUBMITTED) → `approve`(SUBMITTED→APPROVED + posted=true + docStatus=ACTIVE) → `assertVoucherLines` 断言 VALUE_ADJUSTMENT 凭证行 Dr 6702 / Cr 1604 精确金额（=adjustmentAmount=3000）→ 断言资产 `netBookValue` 减少（12000−3000=9000）
  - **REVALUATION_UP 重估增值路径**：同上 approve 链 → `assertVoucherLines` 断言 Dr 1601 / Cr 4002 精确金额（=5000）→ 断言资产 `netBookValue` 增加（12000+5000=17000）
  - **REVALUATION_DOWN 重估减值路径**：同上 approve 链 → `assertVoucherLines` 断言 Dr 6702 / Cr 1601 精确金额（=2000）→ 断言资产 `netBookValue` 减少（12000−2000=10000）
  - 非法迁移守卫（UNSUBMITTED→approve 拒绝 / APPROVED→submitForApproval 拒绝，ErrorCode message token 断言）
  - Skill: none

Exit Criteria:

- [x] 1 spec 文件经 `npx playwright test tests/e2e/business-actions/ast-value-adjustment.action.spec.ts --workers=1` 全绿（6 passed, 51.9s）
- [x] 3 种调整类型 approve→posted 状态翻转均经 `verifyState` `__get` 独立断言（独立于 mutation 返回值）
- [x] 3 组凭证行（6702/1604、1601/4002、6702/1601）精确数值断言经 `assertVoucherLines` 验证

### Phase 2 - 反向红冲 + cancel + 净值回退

Status: completed
Targets: `tests/e2e/business-actions/ast-value-adjustment.action.spec.ts`（同文件追加用例）
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 1 范式验证

- [x] `Add`: **reverseApprove 红冲 + cancel + 净值回退用例**
  - `reverseApprove`(APPROVED→approveStatus=REJECTED + posted=false + docStatus 保持 ACTIVE)：IMPAIRMENT 路径 approve 后 reverseApprove → `findVoucherIdByBillCode(code, 'REVERSAL')` 断言红字凭证行同向取负（Dr 6702=−3000 / Cr 1604=−3000）+ 原正向凭证 `isReversed=true` → 断言资产 `netBookValue` 回退至调整前值（9000+3000=12000）
  - `cancel` 路径：DRAFT/UNSUBMITTED 态 `cancel` → docStatus=CANCELLED + approveStatus 不变（UNSUBMITTED）
  - `withdrawApproval` 路径：SUBMITTED→UNSUBMITTED 撤回
  - 非法守卫（CANCELLED→approve 拒绝）
  - Skill: none

Exit Criteria:

- [x] reverseApprove 红冲凭证行同向取负断言经 `assertVoucherLines` 验证
- [x] 资产净值回退方向断言（reverseApprove 后 netBookValue 恢复调整前值 12000）
- [x] cancel/withdrawApproval 状态翻转经 `verifyState` 独立断言

## Draft Review Record

- Independent draft review iteration 1: `needs revision` (ses_0a11faa1effeAv1NzuAJKOA2i7) — B1: `reverseApprove` docStatus=CANCELLED 事实错误（实际保持 ACTIVE，仅 approveStatus→REJECTED + posted=false）；M1: category 科目字段→凭证科目码映射非显式（`expenseSubjectId`/`depreciationSubjectId` 须置 null 使 fallback 默认值 6702/1604 生效）；S1: config `erp-ast.revaluation-adjust-depreciation-base` 默认 true 非 false。B1+M1+S1 已修订：Phase 2 reverseApprove 描述修正为 docStatus 保持 ACTIVE；Phase 1 Decision|Explore 增 category 科目字段置 null 裁定 + config 默认 true 注记。无新增 BLOCKER。

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。完整仓库验证在此处运行一次。

- [x] 范围内行为完成：1 spec 覆盖 VALUE_ADJUSTMENT 3 种调整类型生命周期 + 凭证行数值断言 + 红冲 + cancel
- [x] 相关文档对齐：`docs/testing/e2e-runbook.md` 业务动作表 +assets VALUE_ADJUSTMENT 行 + 套件计数更新
- [x] 已运行验证：`npx playwright test tests/e2e/business-actions/ast-value-adjustment.action.spec.ts --workers=1` 全绿（6 passed, 51.9s）+ assets 域既有 spec 回归无新增失败（8 passed, 1.1m）+ `mvn clean install -DskipTests` 154 模块 BUILD SUCCESS
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 折旧基数重算精确数值断言

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: config-gated `erp-ast.revaluation-adjust-depreciation-base` 联动折旧重算属折旧引擎深化面。本计划仅断言净值方向（增/减），不验证重算后折旧计划条目精确金额。
- Successor Required: `yes`（触发条件：折旧基数重算浏览器层精确数值断言需求落地时）

## Closure

Status Note: completed — Phase 1 + Phase 2 全部交付。1 spec（`ast-value-adjustment.action.spec.ts`）6 测试全绿（51.9s），覆盖 VALUE_ADJUSTMENT 3 种调整类型（IMPAIRMENT/REVALUATION_UP/REVALUATION_DOWN）useApproval DIRECT 审批轴生命周期（submit→approve→posted=true + Dr/Cr 凭证行精确数值断言 + 净值联动方向）+ reverseApprove 红冲（同向取负 + 原凭证 isReversed + 净值回退）+ cancel + withdrawApproval + 非法迁移守卫。种子 `erp_md_subject.csv` 补齐 3 科目行（6702 资产减值损失/1604 固定资产减值准备/4002 资本公积），解除 VALUE_ADJUSTMENT 过账优雅降级。assets 域既有 4 spec 回归 0 失败（8 passed, 1.1m）。`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS。

Closure Audit Evidence:

- Auditor / Agent: pending independent closure audit（执行者自查：全 Phase [x] + Status completed + 6 测试全绿 + 0 回归 + 154 模块 BUILD SUCCESS；独立子代理结束审计待执行）


- **Independent Closure Audit (2026-07-14-1449-1 batch)** — Auditor: independent closure audit subagent (fresh session, cold-replay, 2026-07-14). Verdict: **PASS**. All 6 exit criteria verified: 6 tests with voucher-line assertions (Dr 6702/Cr 1604 etc.), REVERSAL negative-amount assertion, netBookValue rollback, cancel/withdraw state assertions. Backend infrastructure + seed subjects + ORM tagSet + log entry all consistent. (Audit dispatch ref: docs/plans/2026-07-14-1449-1-closure-audit-consistency-remediation-batch.md Phase 2; this evidence block appended by Phase 3 backfill.)
Follow-up:

- 折旧基数重算精确数值断言 successor（触发条件见 Deferred）
