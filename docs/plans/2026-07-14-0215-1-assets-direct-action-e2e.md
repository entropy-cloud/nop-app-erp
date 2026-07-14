# 2026-07-14-0215-1-assets-direct-action-e2e assets 域 DIRECT 业务动作浏览器层 E2E

> Plan Status: completed
> Last Reviewed: 2026-07-14
> Source: `docs/plans/2026-07-09-2004-1-business-action-e2e-maintenance-projects-quality.md` Deferred「全 18 域全业务动作覆盖（DIRECT 域剩余）」（Successor Required: yes，触发条件「当需按域推进全 DIRECT 业务动作浏览器层覆盖时」——**已满足**：当前项目重点为各域细化端到端验证，inventory/crm/cs/maintenance/projects/quality/manufacturing/purchase/sales/finance 10 域已覆盖，assets 为 Tier-1 未覆盖域）
> Related: `2026-07-09-2004-1`（DIRECT 域扩展范式源）、`2026-07-10-0335-1`（DIRECT useApproval 审批轴范式）、`2026-07-12-0413-2`（DIRECT 状态机+过账断言范式）、`docs/testing/e2e-runbook.md`（业务动作套件）
> Audit: required

## Current Baseline

assets 域后端业务逻辑已全部落地（extended-roadmap M2 2.5/2.5b/2.5c/2.5d/2.14 全 done）：

- **折旧引擎**（`ErpAstDepreciationScheduleBizModel`）：`executeDepreciation(assetId,period)` / `executeBatchDepreciation(period)` / `reverseDepreciation(assetId,period)` / `recalculateForCapitalizationMaintenance(assetId)` — 计算引擎 + DEPRECIATION 业财过账
- **CIP 在建工程转固**（`ErpAstCipBizModel`）：`startConstruction` → `addCostItem` → `addProgressBilling` → `transferToAsset` / `reverseTransfer` — 成本归集 + 转固 + 资本化过账
- **资产盘点**（`ErpAstInventoryBizModel`）：`createInventory` → `submitForCount` → `reconcile` → `processVariance` → `approve` → `post` / `cancel` / `reverse` — 8 动作完整生命周期 + ASSET_INVENTORY_ADJUSTMENT 过账
- **资产维修**（`ErpAstMaintenanceBizModel`）：`createMaintenance` → `submit` → `startWork` → `completeWork` → `decideTreatment` → `approve` → `post` / `cancel` / `reverse` — 费用归集 + CAPITALIZE/EXPENSE 双路径 + MAINTENANCE_EXPENSE/CAPITALIZATION 过账
- **资产减值/重估**（`ErpAstValueAdjustmentBizModel`）：use-approval DIRECT 审批轴 + VALUE_ADJUSTMENT 过账

**浏览器层 E2E 缺口**：assets 域 0 个 business-action spec。`erp_ast_disposal` = `useWorkflow="true"`（xwf，经 2330-1 裁决浏览器层不可行），但折旧/CIP/盘点/维修/减值重估均为 DIRECT（无 useWorkflow），浏览器层可达。

**E2E 基础设施就绪**：`tests/e2e/business-actions/_helper.ts` 三原语 `createViaSave`/`callMutation`/`verifyState` 经 10 域验证可复用；`tests/e2e/orchestration/_helper.ts` `findVoucherIdByBillCode`/`assertVoucherLines` 凭证行断言范式已建立。assets 域种子数据已有（asset_category/asset/depreciation_schedule 3 表）。

## Goals

- assets 域核心 DIRECT 业务动作经 GraphQL `/graphql` 浏览器层全栈可达性 + 状态机迁移验证
- 覆盖 4 条高价值 DIRECT 路径：折旧引擎、CIP 转固链、资产盘点生命周期、资产维修生命周期
- 断言业财过账触发可观测性（posted 标志翻转 / 凭证存在性）在 assets 域过账下的正确性
- 复用既有三原语范式验证在 assets 多型状态机（计算引擎 / 成本归集 / 盘点八态 / 维修费用化+资本化双路径）下的可复用性

## Non-Goals

- **`ErpAstDisposal` 资产处置**（useWorkflow="true" xwf）——经 2330-1 权威裁决浏览器层不可行，排除
- **资产拆分/合并深度数值断言**（2.5d）——`cancel` 仅单动作低价值，归 successor
- **资产减值/重估审批轴 E2E**（2.14 VALUE_ADJUSTMENT）——useApproval DIRECT 审批轴，与 0335-1 Return/Recall 同范式，边际收益递减归 successor
- **资产折旧凭证行精确数值断言的 seed 依赖**——折旧引擎需要科目体系配置链，精确金额断言在凭证行层完成（同 0704-1 范式），但不强求跨期连续折旧数值链

## Task Route

- Type: `verification work`（扩展现有 Playwright E2E 套件覆盖至 assets 域 DIRECT 业务动作）
- Owner Docs: `docs/testing/e2e-runbook.md`（业务动作套件 + 调用范式）、`docs/design/assets/depreciation-and-posting.md`、`docs/design/assets/inventory.md`、`docs/design/assets/maintenance.md`、`docs/design/assets/cip.md`
- Skill Selection Basis: 浏览器层 E2E 测试编写（Playwright + GraphQL mutation 驱动 @BizMutation）→ 无匹配技能（`nop-testing` 技能面向 JunitAutoTestCase 后端快照测试，非 Playwright 浏览器层）；沿用 `tests/e2e/business-actions/_helper.ts` 既有范式 → `Skill: none`

## Infrastructure And Config Prereqs

No infra prereqs beyond existing baseline。复用现有 Playwright 配置 + webServer JVM 参数。assets 域种子数据已有（asset_category/asset/depreciation_schedule 3 表 2210-1）。

> 注：assets 过账依赖科目体系（如折旧科目 1601/6602、WIP 1411、维修费用 6602 等）。种子 `erp_md_subject.csv` 已含基础科目。若特定路径需补科目行，按 1800-1/0413-2 范式种子补齐 + webServer JVM arg 追加（在 Explore 阶段裁定）。

## Execution Plan

### Phase 1 - 资产折旧引擎 + CIP 转固链 E2E

Status: completed
Targets: `tests/e2e/business-actions/ast-depreciation.action.spec.ts`（新建）、`tests/e2e/business-actions/ast-cip-capitalization.action.spec.ts`（新建）
Skill: none

- Item Types: `Add | Proof | Decision`
- Prereqs: 无（自包含 setup）

- [x] `Decision | Explore`: 裁定 assets 过账科目依赖——折旧（1601/6602 等）/ 盘点（1601/6301/6711）/ 维修（6602/1601）过账 Provider 读 `findByCode` 解析科目码。Explore 实测种子 `erp_md_subject.csv` 是否含所需科目行；若缺失按 1800-1/0413-2 范式种子补齐 + webServer JVM arg 追加对应 config（如 `erp-ast.depreciation-expense-subject-code`）。裁定结果记入执行日志。
  - Skill: none
- [x] `Add`: **折旧引擎 spec** `ast-depreciation.action.spec.ts`
  - `executeDepreciation(assetId, period)` 浏览器层可达性：自包含建 `ErpAstAsset`（ACTIVE 状态 + 原值 + 残值 + 折旧方法 + 科目体系）→ 调 `executeDepreciation` → `verifyState` 断言折旧单 status 翻转 + `ErpAstDepreciationSchedule` 累计折旧字段更新
  - `reverseDepreciation(assetId, period)` 红冲回退：折旧后调 reverse → 断言状态回退 / 红字凭证标记
  - 非法迁移守卫（如已折旧期间重复折旧 / 不存在资产 抛 ErrorCode message token）
  - Skill: none
- [x] `Add`: **CIP 转固链 spec** `ast-cip-capitalization.action.spec.ts`
  - CIP 全链编排：自包含建 `ErpAstCip`（DRAFT 入口）→ `startConstruction` → `addCostItem`（归集成本）→ `addProgressBilling`（进度付款）→ `transferToAsset`（转固：生成 `ErpAstAsset` + CIP 状态翻转 TRANSFERRED）→ 断言资产原值 = 成本归集合计 + `completedAssetId` 回链
  - `reverseTransfer` 回退：转固后 reverse → 断言 CIP 恢复 + 资产删除/回退（按实现裁定）
  - Skill: none

Exit Criteria:

- [x] 2 spec 文件经 `npx playwright test tests/e2e/business-actions/ast-*.action.spec.ts --workers=1` 全绿
- [x] 折旧 + CIP 转固状态翻转均经 `verifyState` `__get` 独立断言（独立于 mutation 返回值）

### Phase 2 - 资产盘点 + 资产维修生命周期 E2E

Status: completed
Targets: `tests/e2e/business-actions/ast-inventory-count.action.spec.ts`（新建）、`tests/e2e/business-actions/ast-maintenance.action.spec.ts`（新建）
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 1 范式验证

- [x] `Add`: **资产盘点生命周期 spec** `ast-inventory-count.action.spec.ts`
  - 8 动作状态机正向链：自包含建 `ErpAstInventory`（含盘点范围 asset）→ `createInventory` → `submitForCount` → `reconcile`（差异计算）→ `processVariance`（盘盈建卡 / 盘亏 SCRAPPED）→ `approve` → `post`（ASSET_INVENTORY_ADJUSTMENT 过账）→ 断言 status 翻转 DRAFT→COUNTING→RECONCILING→POSTED
  - `reverse` 红冲回退：post 后 reverse → 断言 status 回退 + 红字凭证
  - `cancel` 异常路径 + 非法迁移守卫（POSTED→submitForCount 等）
  - Skill: none
- [x] `Add`: **资产维修生命周期 spec** `ast-maintenance.action.spec.ts`
  - 维修费用化路径：自包含建 `ErpAstMaintenance`（含维修成本行）→ `createMaintenance` → `submit` → `startWork` → `completeWork` → `decideTreatment(treatment=EXPENSE)` → `approve` → `post`（MAINTENANCE_EXPENSE 过账）→ 断言 status 翻转 + 费用化凭证
  - 维修资本化路径：同上但 `decideTreatment(treatment=CAPITALIZE)` → `post`（MAINTENANCE_CAPITALIZATION 过账 + 资产原值增量 + 折旧重算联动）
  - `reverse` 红冲 + `cancel` 异常路径
  - Skill: none

Exit Criteria:

- [x] 2 spec 文件经 `npx playwright test tests/e2e/business-actions/ast-inventory-count.action.spec.ts tests/e2e/business-actions/ast-maintenance.action.spec.ts --workers=1` 全绿
- [x] 盘点 8 动作 + 维修费用化/资本化双路径状态翻转均经 `verifyState` 独立断言

## Draft Review Record

- Independent draft review iteration 1: accept (ses_0a34c439bffe) — 所有 @BizMutation 方法/useWorkflow 标记/helper 原语/种子数据经实时仓库核实一致，规则合规全 PASS，无 Blocker。已采纳 4 项非阻塞建议：(1) Goal 凭证行措辞收紧为「过账触发可观测性」对齐 Deferred；(2) CIP 终态 TRANSFERRED 修正（非 COMPLETED）；(3) 新增 Phase 1 `Decision|Explore` 项覆盖科目依赖裁定；(4) Source 文件名 slug 修正。计划可作为执行契约进入实施。

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。完整仓库验证在此处运行一次。

- [x] 范围内行为完成：4 spec 覆盖 assets 4 条 DIRECT 路径（折旧 / CIP 转固 / 盘点 / 维修）
- [x] 相关文档对齐：`docs/testing/e2e-runbook.md` 业务动作表 +assets 行 + 套件计数更新
- [x] 已运行验证：`npx playwright test tests/e2e/business-actions/ast-*.action.spec.ts --workers=1` 全绿 + 全套件回归无新增失败
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### assets 域凭证行精确数值断言

- Classification: `optimization candidate`
- Why Not Blocking Closure: 本计划聚焦状态机迁移 + 过账触发可观测性（posted 标志 / 凭证存在性）。凭证行科目码/金额精确数值断言（如折旧 Dr 6602 / Cr 1602 精确金额）属数值断言层增量，依赖 assets 科目配置链完整性。
- Successor Required: `yes`（触发条件：assets 域凭证行数值断言需求落地时）

### 资产减值/重估审批轴 E2E（VALUE_ADJUSTMENT）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: useApproval DIRECT 审批轴 + VALUE_ADJUSTMENT 过账，与 0335-1 Return/Recall 同范式。边际收益递减。
- Successor Required: `yes`（触发条件：按域推进 DIRECT useApproval 剩余实体浏览器层覆盖时）

### 资产拆分/合并 E2E

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 仅 `cancel` 单动作 + 按比例原值分配，状态机浅，低价值。
- Successor Required: `no`

## Closure

Status Note: 全 2 Phase 完成。4 spec（ast-depreciation / ast-cip-capitalization / ast-inventory-count / ast-maintenance）8 测试全绿。种子 erp_md_subject.csv 补齐 5 assets 过账科目（1601/1602/1603/6301/6602）解除过账优雅降级。

Closure Audit Evidence:

- Auditor / Agent: pending independent closure audit（执行者自查：全 Phase [x] + Status completed + 8 测试全绿 + 0 回归；独立子代理结束审计待执行）


- **Independent Closure Audit (2026-07-14-1449-1 batch)** — Auditor: independent closure audit subagent (fresh session, cold-replay, 2026-07-14). Verdict: **PASS**. All 4 specs / 8 tests verified with independent verifyState assertions; seed COA补齐 (5 rows); runbook updates; deferred items (voucher numeric / VALUE_ADJUSTMENT / split-merge) all have successors confirmed landed. (Audit dispatch ref: docs/plans/2026-07-14-1449-1-closure-audit-consistency-remediation-batch.md Phase 2; this evidence block appended by Phase 3 backfill.)
Follow-up:

- assets 凭证行精确数值断言 successor（触发条件见 Deferred）
