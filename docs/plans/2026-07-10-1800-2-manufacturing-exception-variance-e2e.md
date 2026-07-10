# 2026-07-10-1800-2-manufacturing-exception-variance-e2e 制造域扩展 E2E（WorkOrder 异常分支 + 部分完工 + 生产差异浏览器层）

> Plan Status: completed
> Last Reviewed: 2026-07-11
> Source: deferred items from `2026-07-10-0704-2`（WorkOrder 异常分支 stop/resume/cancel + 部分完工 / 生产差异计算 browser-layer E2E）
> Related: `2026-07-10-0704-2-manufacturing-chain-orchestration-e2e.md`（制造链编排基线），`2026-07-10-0335-1-approval-gated-direct-business-action-e2e.md`（WorkOrder DIRECT 审批轴基线），`2026-07-05-1838-2`（生产差异计算后端基线）
> Audit: required

## Current Baseline

- `orchestration/_helper.ts` 已建立 `runMfgChain`/`cleanupMfg` 编排原语（0704-2 落地）：BOM+BOMLine → WorkOrder+行 → 审批轴（submit→approve）→ checkAvailability（STOCK_RESERVED）→ start（IN_PROCESS）→ MaterialIssue.confirm（领料出库）→ JobCard.recordWork（报工）→ reportCompletion（完工入库 COMPLETED）。每步 `verifyState` `__get` 独立断言 docStatus 翻转。
- `business-actions/_helper.ts` 已建立三原语：`createViaSave`/`callMutationOk`/`verifyState`（0814-2 落地）。
- `mfg-work-order.action.spec.ts`（0335-1）已覆盖 WorkOrder DIRECT 审批轴 UNSUBMITTED→SUBMITTED→APPROVED（+ docStatus DRAFT→SUBMITTED→NOT_STARTED）+ reject 守卫 + checkAvailability（STOCK_RESERVED）→ start（IN_PROCESS）→ close（CLOSED）。
- **未覆盖**（Deferred from 0704-2）：
  - **WorkOrder 异常分支**：stop（IN_PROCESS→STOPPED）/ resume（STOPPED→IN_PROCESS）/ cancel（DRAFT/SUBMITTED/NOT_STARTED→CANCELLED）。后端方法已落地（`ErpMfgWorkOrderProcessor:124-166`），但浏览器层 E2E 未覆盖。
  - **部分完工**：`reportCompletion(completedQty < plannedQty)` → 不触发 `willFinish`（newCompleted < planned）→ docStatus 保持 IN_PROCESS → 后续 `close` → CLOSED。0704-2 仅覆盖正路径满量完工 COMPLETED。
  - **生产差异计算浏览器层**：`reportCompletion` 在 `willFinish`（满量完工）+ `erp-mfg.variance-auto-calc-enabled=true` 时触发 `ProductionVarianceCalculator.calculateVariances` + `ProductionVarianceDispatcher.dispatchIfApplicable`（PRODUCTION_VARIANCE 过账）。后端已落地（1838-2），try/catch 失败隔离。0704-2 Deferred「当需验证生产差异浏览器层端到端时」。
- **后端状态机核实**（live repo `ErpMfgWorkOrderProcessor:124-166`）：
  - `stop:124` — requireStatus(IN_PROCESS) → setDocStatus(STOPPED)
  - `resume:132` — requireStatus(STOPPED) → setDocStatus(IN_PROCESS)
  - `close:140` — requireStatus(STOPPED | IN_PROCESS) → setDocStatus(CLOSED) + actualEndDate
  - `cancel:155` — requireStatus(DRAFT | SUBMITTED | NOT_STARTED) → setDocStatus(CANCELLED)
  - `reportCompletion:172` — requireStatus(IN_PROCESS) + 累加 completedQty + `willFinish = newCompleted >= planned`
- **生产差异 config gate**（live repo 核实）：`ErpMfgConstants.CONFIG_VARIANCE_AUTO_CALC_ENABLED = "erp-mfg.variance-auto-calc-enabled"`（默认 false）。webServer JVM args（`playwright.config.ts:18`）当前不含此 config。`ProductionVarianceCalculator` 需 FIRMED `ErpMfgCostRollupLine`（标准成本来源），无 FIRMED 行时 `calculateVariances` 抛异常被 try/catch 吞掉仅记 ERROR 日志，不阻断完工。
- E2E 套件当前 166 测试（0704-2 校准基线）。

## Goals

- 扩展制造域 WorkOrder 业务动作浏览器层 E2E 覆盖至异常分支（stop/resume/cancel/close）+ 部分完工路径 + 生产差异浏览器层验证。
- 解除 0704-2 三项 Deferred。

## Non-Goals

- 完工入库 GL 过账凭证断言（MANUFACTURING_RECEIPT voucher）——**已落地**：mfg-chain.spec.ts 已断言 `completionMove.posted=true` + MANUFACTURING_RECEIPT 凭证行（Dr 1401 / Cr 1411，1100-5 落地后基线校准已完成）。
- 质检门控阻断完工（config-gated `erp-mfg.inspection-on-completion-enabled`，0335-1 Deferred「config-gated 质检门控」归 successor）。
- 批次基因追溯浏览器层（config-gated `erp-mfg.genealogy-write-enabled`，0305-3 后端已落地但浏览器层归 successor）。
- 直接人工/制造费用 GL 过账（1100-5 Deferred「人工/制费计提」，需独立成本会计面）。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/testing/e2e-runbook.md`（§制造链编排层段扩展），`docs/design/manufacturing/state-machine.md`（§工单状态机 §异常分支），`docs/design/manufacturing/variance-analysis.md`（§核心计算逻辑）
- Skill Selection Basis: `nop-testing`（测试编写技能，但本计划为 Playwright E2E spec 非平台 JunitAutoTestCase，技能中无 Playwright 专项路由 → `Skill: none`；测试编写遵循 0704-2 已验证 `runMfgChain` 范式）

## Infrastructure And Config Prereqs

- Phase 1+2：No infra prereqs beyond existing baseline（DIRECT @BizMutation 状态迁移，无 config gate）。
- Phase 3：需 config `erp-mfg.variance-auto-calc-enabled=true`（当前 webServer JVM args 不含），+ 种子 `ErpMfgCostRollup`(FIRMED) + `ErpMfgCostRollupLine` 使标准成本可达。详见 Phase 3 Decision。

## Execution Plan

### Phase 1 — WorkOrder 异常分支 stop/resume/close + cancel 浏览器层 E2E

Status: completed
Targets: `tests/e2e/business-actions/mfg-work-order-exception.action.spec.ts`（新建）
Skill: none

- Item Types: `Add | Proof`
- Prereqs: 0704-2 完成（runMfgChain 范式已落地）+ 0335-1 完成（WorkOrder DIRECT 审批轴已验证）

- [x] Add: 新建 `mfg-work-order-exception.action.spec.ts`——WorkOrder 异常分支完整路径 E2E。复用 0335-1 范式建 WorkOrder + 审批 + checkAvailability + start 到 IN_PROCESS，然后：
  - 正路径 A：stop（IN_PROCESS→STOPPED）→ resume（STOPPED→IN_PROCESS）→ close（IN_PROCESS→CLOSED，actualEndDate 回填）。每步 `verifyState` `__get` 独立断言 docStatus。
  - 正路径 B：stop（IN_PROCESS→STOPPED）→ close（STOPPED→CLOSED）。验证 close 从 STOPPED 也可达。
  - 正路径 C：cancel 从 NOT_STARTED（approve 后未 start）→ CANCELLED。验证 cancel 从 NOT_STARTED 可达。
  - 非法迁移守卫：IN_PROCESS→cancel 抛 ErrorCode（cancel 仅允许 DRAFT/SUBMITTED/NOT_STARTED）；CLOSED→stop/resume 抛 ErrorCode。
  - Skill: none
- [x] Proof: 清理——WorkOrder 本身逻辑删除。WorkOrder 无审批产物（DIRECT 模式无 xwf 步骤）、无过账产物（异常分支不触发 reportCompletion 无凭证）、无库存移动（未领料）。清理范围极小。
  - Skill: none

Exit Criteria:

> WorkOrder 异常分支 stop/resume/close/cancel + 非法迁移守卫经浏览器层 GraphQL `@BizMutation` 可观测。

- [x] `mfg-work-order-exception.action.spec.ts` 新 spec 全绿（3 正路径 + 2+ 非法迁移守卫）
- [x] `npx playwright test tests/e2e/business-actions/mfg-work-order-exception.action.spec.ts --workers=1` 通过

### Phase 2 — 部分完工路径浏览器层 E2E

Status: completed
Targets: `tests/e2e/business-actions/mfg-work-order-exception.action.spec.ts`（同 Phase 1 spec，新增 test case）
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 1

- [x] Add: 同 Phase 1 spec 新增 test case——部分完工路径：(1) runMfgChain 范式建 WorkOrder（plannedQty=20）+ 审批 + checkAvailability + start 到 IN_PROCESS + MaterialIssue.confirm（领料出库）；(2) reportCompletion(completedQty=10 < plannedQty=20) → `willFinish=false` → docStatus 保持 IN_PROCESS + completedQuantity=10；(3) close（IN_PROCESS→CLOSED）；(4) `verifyState` 断言 completedQuantity=10 + docStatus=CLOSED（非 COMPLETED）。
  - Skill: none
- [x] Proof: 清理——复用 0704-2 `cleanupMfg`（部分完工已生成 MANUFACTURE 入库移动 10 件 + 成品余额，清理覆盖）。
  - Skill: none

Exit Criteria:

> 部分完工路径（reportCompletion qty < planned → IN_PROCESS 保留 → close → CLOSED）经浏览器层可观测，验证 willFinish 门控正确性。

- [x] 部分完工 test case 全绿（completedQuantity 累加 + docStatus IN_PROCESS 保留 + close → CLOSED）
- [x] `npx playwright test tests/e2e/business-actions/mfg-work-order-exception.action.spec.ts --workers=1` 通过（Phase 1+2 合并验证）

### Phase 3 — 生产差异计算浏览器层 E2E

Status: completed
Targets: `tests/e2e/orchestration/mfg-variance.spec.ts`（新建），`playwright.config.ts`（webServer JVM args 扩展），`tests/e2e/orchestration/_helper.ts`（cleanupMfg 扩展）
Skill: none

- Item Types: `Add | Decision | Proof`
- Prereqs: Phase 1+2 完成（异常分支 + 部分完工基线稳定）

- [x] Decision: config 启用 + 回归隔离方式裁决——在 webServer JVM args 追加 `-Derp-mfg.variance-auto-calc-enabled=true`（全局生效）。**回归隔离**：现有 `mfg-chain.spec.ts` 使用 MAT-001（`SEED.MAT_1`）为 WorkOrder 产品（`_helper.ts:701` `productId: SEED.MAT_1`），MAT-001 无 FIRMED `ErpMfgCostRollupLine` → `ProductionVarianceCalculator.calculateVariances` 抛 `ERR_VARIANCE_NO_STANDARD_COST` → 被 `Processor:226-235` try/catch 吞掉仅记 ERROR 日志，不阻断完工、不产生差异记录、不产生凭证。本 Phase 差异 spec 使用**测试专用成品物料**（运行时 `createViaSave`，非 MAT-001）+ 运行时创建 FIRMED `ErpMfgCostRollup`/`Line`，仅对该物料产生差异——与 MAT-001 链路完全隔离。残留风险：现有 mfg-chain.spec.ts 完工时 try/catch 记一条 ERROR 日志——acceptable（E2E 套件不检查 ERROR 日志）。替代方案（否决）：NopSysVariable 种子行（同全局效果但 seed 维护更重）/ 独立 Playwright project 独立 webServer（过度工程）。
  - Skill: none
- [x] Add: webServer JVM args 追加 `-Derp-mfg.variance-auto-calc-enabled=true`（`playwright.config.ts:18` command 行扩展）。
  - Skill: none
- [x] Add: 新建 `mfg-variance.spec.ts`——生产差异完整路径 E2E（自包含，不依赖 MAT-001）：(1) 运行时创建测试专用成品物料（FINISHED_PRODUCT, STANDARD costing）+ FIRMED `ErpMfgCostRollup` + `ErpMfgCostRollupLine`（materialCost/laborCost/overheadCost 分解行，定义与实际成本有偏差的标准成本）；(2) 运行时创建 BOM（productId=测试专用成品）+ 组件物料 + 备货；(3) runMfgChain 变体（或内联编排）建 WorkOrder + 审批 + 齐套 + start + 领料 + 报工 + reportCompletion（满量完工 → willFinish=true → `calculateVariances` + `dispatchIfApplicable`）；(4) `verifyState` 断言 docStatus=COMPLETED；(5) `findItems` 查 `ErpMfgCostVariance`（按 workOrderId）非空 + varianceType 存在；(6) `findVoucherIdByBillCode(page, woCode + '-PV')` 反查 PRODUCTION_VARIANCE 凭证 + `assertVoucherLines` 断言行（科目码 + 金额由差异值派生——期望值在实现期从标准 vs 实际成本差额派生）。
  - Skill: none
- [x] Add: `cleanupMfg` 扩展——增 `ErpMfgCostVariance` 清理（`deleteByFilter` 按 workOrderId）+ PRODUCTION_VARIANCE 凭证清理（`cleanupVoucherByBillCode(page, woCode + '-PV')`，注意 `ProductionVarianceDispatcher` billHeadCode 后缀 `-PV`）。虽然 config 全局生效，但因差异仅对测试专用物料（有 FIRMED rollup）产生，MAT-001 链路无差异记录，cleanupMfg 扩展对 MAT-001 链路为空操作安全。
  - Skill: none
- [x] Proof: 清理——差异 spec 自清理全部产物（`ErpMfgCostVariance` + PRODUCTION_VARIANCE 凭证 + FIRMED rollup + 测试专用物料 + 链路库存移动/余额 + WorkOrder/BOM/JobCard 逐域逻辑删除）。保护共享 DB 数值断言基线。
  - Skill: none

Exit Criteria:

> 生产差异计算 + 过账经浏览器层可观测（ErpMfgCostVariance 记录非空 + PRODUCTION_VARIANCE 凭证行可断言）。config 全局启用经裁决无回归（MAT-001 链路 try/catch 隔离）。

- [x] `mfg-variance.spec.ts` 新 spec 全绿（ErpMfgCostVariance 非空 + PRODUCTION_VARIANCE 凭证行断言）
- [x] config 启用后既有 mfg-chain.spec.ts + mfg-work-order spec 0 回归（`npx playwright test tests/e2e/orchestration/ tests/e2e/business-actions/mfg- --workers=1` 全绿）

## Draft Review Record

- Independent draft review iteration 1: needs revision (ses_0b41af493ffeKmIcRGVXTSn0lV) because two issues: (Blocker 1) Phase 3 Decision regression analysis factually incorrect — after adding FIRMED rollup seed for MAT-001, existing mfg-chain.spec.ts (which uses MAT-001 as WorkOrder product at `_helper.ts:701`) would trigger variance calc producing uncleansed ErpMfgCostVariance + PRODUCTION_VARIANCE voucher; (Suggestion 1) Deferred/Non-Goal stale references to mfg-chain.spec.ts `posted=false` — live file already asserts `posted=true` + MANUFACTURING_RECEIPT voucher lines (1100-5 updated it).
- Independent draft review iteration 2: accept (ses_0b40f59caffeRhGA1M2BiDVw42) — Blocker 1 fixed: Phase 3 restructured to use test-specific product + runtime FIRMED rollup isolating from MAT-001 chain; cleanupMfg extended with variance cleanup (`-PV` suffix); stale posted=false references corrected to live state. Decision rationale complete. Plan is an acceptable execution contract.

## Closure Gates

> 完整仓库验证在此处运行。本计划为纯测试 + 种子数据 + webServer config 变更，无生产代码/契约/ORM 模型变更。验证：`mvn clean install -DskipTests`（154 模块 BUILD SUCCESS）+ `npx playwright test --workers=1`（全套件 0 回归）。

- [x] 范围内行为完成（Phase 1+2 异常分支/部分完工 + Phase 3 生产差异全绿）
- [x] 相关文档对齐（`docs/testing/e2e-runbook.md` §制造链编排层段扩展 + §业务动作表扩展）
- [x] 已运行验证（`mvn clean install -DskipTests` + `npx playwright test --workers=1`）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 完工入库 GL 过账凭证行断言（MANUFACTURING_RECEIPT voucher）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: **已落地**——1100-5 后端落地后，mfg-chain.spec.ts 已更新为断言 `completionMove.posted=true` + MANUFACTURING_RECEIPT 凭证行（Dr 1401 / Cr 1411，`MFG_EXPECT.totalCost` 金额）。此 Deferred 条件已满足，无需 successor。
- Successor Required: `no`

### 质检门控阻断完工

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: config-gated `erp-mfg.inspection-on-completion-enabled`（默认关），完工时如产品有 FINAL 质检模板则强制质检阻塞。属不同 config gate + 不同业务路径。
- Successor Required: `yes`（触发条件：当需验证质检门控阻断完工路径时）

### 批次基因追溯浏览器层

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: config-gated `erp-mfg.genealogy-write-enabled`（默认关），完工时写入批次基因链。0305-3 后端已落地。属不同 config gate。
- Successor Required: `yes`（触发条件：当需验证批次基因追溯浏览器层端到端时）

## Closure

Status Note: completed

Closure Audit Evidence:

- Auditor / Agent: independent closure audit subagent（新会话，不重用执行者上下文；本审计即 `2026-07-11` 独立结束审计 pass）
- Evidence: 独立子代理逐项核实——(1) Exit Criteria 对比 live repo：`mfg-work-order-exception.action.spec.ts`（5 tests：3 正路径 stop/resume/close/cancel + 2 非法迁移守卫 + 1 部分完工 willFinish 门控，断言 `expect.toBe` 非空壳）+ `mfg-variance.spec.ts`（ErpMfgCostVariance 5 行 + posted=true + PRODUCTION_VARIANCE 凭证 6 行精确数值断言 Dr 1410/Cr 1411/Dr 1412/Cr 1413/Dr 1415/Cr 1414）存在且非 hollow；(2) `playwright.config.ts:18` 已含 `-Derp-mfg.variance-auto-calc-enabled=true`；(3) `_helper.ts:856-863` cleanupMfg 扩展（deleteByFilter ErpMfgCostVariance + cleanupVoucherByBillCode `-PV`）落地；(4) Anti-Hollow：所有 spec 含真实 GraphQL 编排 + `verifyState`/`assertVoucherLines` 断言 + try/finally 清理，无 `return null`/空体/吞异常占位；(5) 五点一致性：Plan Status completed ↔ Phase 1/2/3 completed ↔ Exit Criteria 全 `[x]` ↔ Closure Gates 全 `[x]` ↔ `docs/logs/2026/07-11.md` full-green 记录一致；(6) Deferred honesty：三项 Deferred（MANUFACTURING_RECEIPT 已落地/质检门控/批次基因）均为真实 out-of-scope config-gated successor，无隐藏 live defect；(7) Docs sync：`docs/testing/e2e-runbook.md`（lines 5/232/252/297 增异常分支+部分完工+生产差异行 + PRODUCTION_VARIANCE 凭证期望值表）+ `docs/logs/2026/07-11.md`（full-green：`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS + `npx playwright test --workers=1` 174 passed，0 回归）已更新。审计结论：approved，可关闭。

Follow-up:

- 质检门控阻断完工 + 批次基因追溯浏览器层（各自 config gate successor）
