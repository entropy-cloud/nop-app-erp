# 2026-07-11-0730-1-manufacturing-config-gated-e2e 制造域 config-gated 浏览器层 E2E（批次基因追溯 + 质检门控阻断完工）

> Plan Status: completed
> Last Reviewed: 2026-07-11
> Source: deferred items from `2026-07-10-1800-2-manufacturing-exception-variance-e2e.md`（Non-Goals「质检门控阻断完工」+「批次基因追溯浏览器层」）+ `2026-07-10-0704-2-manufacturing-chain-orchestration-e2e.md` Deferred「质检门控 / 批次基因追溯（config-gated 默认关）」
> Related: `2026-07-10-1800-2-manufacturing-exception-variance-e2e.md`（异常分支+生产差异基线），`2026-07-10-0704-2-manufacturing-chain-orchestration-e2e.md`（制造链编排基线），`2026-07-07-0305-3-manufacturing-batch-genealogy-traceability.md`（批次基因追溯后端），`2026-07-02-2237-3-quality-inspection-trigger-ncr-capa.md`（质检触发后端）
> Audit: required

## Current Baseline

- `orchestration/_helper.ts` 已建立 `runMfgChain`/`cleanupMfg` 编排原语（0704-2 落地）：BOM+BOMLine → WorkOrder+行 → 审批轴（submit→approve）→ checkAvailability（STOCK_RESERVED）→ start（IN_PROCESS）→ MaterialIssue.confirm（领料出库）→ JobCard.recordWork（报工）→ reportCompletion（完工入库 COMPLETED）。每步 `verifyState` `__get` 独立断言 docStatus 翻转。1800-2 已扩展异常分支（stop/resume/cancel/close）+ 部分完工 + 生产差异（config `erp-mfg.variance-auto-calc-enabled=true`）。
- webServer JVM args（`playwright.config.ts:18`）当前含 `-Derp-mfg.variance-auto-calc-enabled=true` + `-Derp-qua.ncr-default-acct-schema=1`，**不含** genealogy / inspection-gate config。
- **批次基因追溯后端**（0305-3 completed）：`BatchGenealogyWriter.writeOnCompletion`（`module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/genealogy/BatchGenealogyWriter.java:64`）在完工入库成功后写入 `ErpMfgBatchGenealogy`（inputLot→outputLot 消耗行）。config-gated `erp-mfg.genealogy-write-enabled`（`ErpMfgConstants:202`），**默认 `true`**（`BatchGenealogyWriter.isWriteEnabled:241` `AppConfig.var(CONFIG, "true")`）。best-effort（内部 try/catch，不阻断完工）。基因链写入需前置条件：`findIssueLinesWithBatch(workOrderId)` 筛选 CONFIRMED/DONE MaterialIssue 下 `batchNo` 非空 + `issuedQuantity > 0` 的 MaterialIssueLine（`BatchGenealogyWriter:184-210`）。当前 `runMfgChain` 不设 MaterialIssueLine.batchNo → 基因链为空。产出批次自动创建 `ErpInvBatch`（batchNo=`GENEALOGY_OUTPUT_BATCH_PREFIX + "-" + wo.getCode()`，`ensureOutputLot:149`）。
- **基因追溯查询** `@BizQuery`（`ErpMfgBatchGenealogyBizModel:45-71`）：`forwardTrace(outputLotId)` / `backwardTrace(inputLotId)` / `traceChain(lotId, direction, maxDepth)` / `recallReport(lotId)`，经 GraphQL `ErpMfgBatchGenealogy__forwardTrace` 等浏览器层可达。
- **质检门控后端**（2237-3 completed）：`ErpMfgWorkOrderProcessor.reportCompletion:187-200` 在 `willFinish=true`（满量完工）时有**两条独立门控路径**：
  - **Gate 1**（`isInspectionGated:398-407`）：需 `erp-mfg.inspection-gate-enabled=true`（config，默认 false）**AND** WorkOrder.bomId 非空 **AND** `BOM.inspectionRequired=true`（ORM 字段，默认 false）。命中时直接抛 `ERR_INSPECTION_REQUIRED`。
  - **Gate 2**（`InspectionTrigger.enforceGate`，`module-quality/erp-qa-dao/.../InspectionTrigger.java:39`）：需 `erp-qa.mandatory-inspection-bill-types` config 含 `ERP_MFG_WORK_ORDER`（默认空=CLEARED，不强制）。命中时创建 PENDING 质检单 + 返回 BLOCKED。
  - **裁决**：本计划经 Gate 1 验证（更简洁：config + BOM 字段，不涉 mandatory-bill-types config + 质检单创建编排）。Gate 2 归 Non-Goal。
- **未覆盖**（Deferred from 1800-2 Non-Goals）：
  - **批次基因追溯浏览器层**：genealogy-write-enabled 默认 true（已激活），但 `runMfgChain` 未设 MaterialIssueLine.batchNo → 基因链为空 → 追溯查询返回空。浏览器层 E2E 未覆盖。触发条件「当需验证批次基因追溯浏览器层端到端时」已满足（后端 + 查询 API 齐备）。
  - **质检门控阻断完工**（Gate 1 路径）：config-gated `erp-mfg.inspection-gate-enabled`（默认 false），2237-3 后端已落地但浏览器层 E2E 未覆盖。触发条件「当需验证质检门控阻断完工路径时」已满足。
- E2E 套件当前 174 测试（1800-2 基线）。
- 种子数据：`app-erp-all/src/main/resources/_vfs/_init-data/`（92 CSV 文件）。**无 `erp_inv_batch.csv` 种子**——输入批次须运行时创建。制造域种子含 work_order/cost_variance/forecast 等。

## Goals

- 扩展制造域 config-gated 功能浏览器层 E2E 覆盖：批次基因追溯链写入 + 四类追溯查询；质检门控阻断完工正/负路径。
- 解除 1800-2 两项 Non-Goals + 0704-2 两项 Deferred。

## Non-Goals

- 直接人工/制造费用 GL 过账（1100-5 Deferred「人工/制费计提」，需独立成本会计面，触发条件未满足）。
- 副产品/联产品成本分配（1100-5 Deferred，高级制造会计）。
- Gate 2 质检门控（`InspectionTrigger.enforceGate` + `erp-qa.mandatory-inspection-bill-types`，需质检单创建编排，独立 successor）。
- SPC 失控→CAPA 级联浏览器层（config-gated `erp-qa.spc-*`，quality 域独立 successor）。
- 基因追溯链可视化前端（echarts 树/图，前端能力面，1606-2 Deferred）。
- 召回报告降级标记消除（依赖 inventory 域按批次库存位置/已售去向查询，0305-3 Deferred）。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/testing/e2e-runbook.md`（§制造链编排层段扩展），`docs/design/manufacturing/batch-genealogy.md`（§数据模型 §追溯查询），`docs/design/manufacturing/state-machine.md`（§质检门控），`docs/design/quality/inspection-trigger.md`（§触发机制）
- Skill Selection Basis: 本计划为 Playwright E2E spec（非平台 `JunitAutoTestCase`），`nop-testing` 技能覆盖平台测试非 Playwright → `Skill: none`；测试编写遵循 0704-2/1800-2 已验证 `runMfgChain` 范式 + `callMutationOk`/`verifyState` 三原语。

## Infrastructure And Config Prereqs

- Phase 1：genealogy-write-enabled 默认 true（**无需加 config**）。需运行时创建输入 `ErpInvBatch`（经 GraphQL `ErpInvBatch__save`，batchNo + materialId + warehouseId）+ MaterialIssueLine.batchNo 引用该批次。产出批次由 `ensureOutputLot` 自动创建。无种子文件变更（无 `erp_inv_batch.csv` 种子）。
- Phase 2：需 config `-Derp-mfg.inspection-gate-enabled=true` 加入 webServer JVM args（`playwright.config.ts:18`）。Gate 1 路径还需 `runMfgChain` 创建的 BOM 设 `inspectionRequired=true`（ORM 字段，经 GraphQL `ErpMfgBom__save` 时设）。无需质检模板种子（Gate 1 不涉模板匹配）。

## Execution Plan

### Phase 1 — 批次基因追溯浏览器层 E2E

Status: completed
Targets: `tests/e2e/orchestration/mfg-genealogy.spec.ts`（新建），`tests/e2e/orchestration/_helper.ts`（`runMfgChain` 扩展支持 MaterialIssueLine.batchNo）
Skill: none

- Item Types: `Add | Proof`
- Prereqs: 0704-2 完成（`runMfgChain` 范式已落地）+ 0305-3 完成（基因追溯后端已落地）

- [x] `Add`：扩展 `runMfgChain`——在 MaterialIssue/Line 创建步骤中，为 MaterialIssueLine 设 `batchNo`（测试专用唯一值如 `GEN-IN-{ts}`）+ 预先经 GraphQL `ErpInvBatch__save` 创建输入 `ErpInvBatch`（同 materialId + warehouseId + batchNo）。`runMfgChain` 扩展须经可选参数 `withBatchTracking` 控制（默认 false，不影响既有 mfg-chain/mfg-variance/exception spec）
  - Skill: none
- [x] `Add`：新建 `mfg-genealogy.spec.ts`——经 `runMfgChain({ withBatchTracking: true })` 驱动 WorkOrder 全链至 reportCompletion（满量完工 COMPLETED）
  - Skill: none
- [x] `Proof`：断言基因链写入——`verifyState` WorkOrder docStatus=COMPLETED 后，经 GraphQL `ErpMfgBatchGenealogy__findPage(filter{workOrderId})` 获取基因链行，断言非空 + inputLotId/outputLotId 非空 + inputMaterialId/outputMaterialId 正确
  - 成功模式：基因链行 inputLotId=输入批次 id，outputLotId=产出批次 id（batchNo=`GENEALOGY_OUTPUT_BATCH_PREFIX + "-" + woCode`）
  - Skill: none
- [x] `Proof`：断言追溯查询可达——经 GraphQL `ErpMfgBatchGenealogy__forwardTrace(outputLotId)` 返回非空列表（含 inputLot→outputLot 消耗行）；`ErpMfgBatchGenealogy__backwardTrace(inputLotId)` 返回含产出批次
  - Skill: none

Exit Criteria:

- [x] `mfg-genealogy.spec.ts` 全绿：`findPage` 非空基因链行 + forwardTrace/backwardTrace 返回非空消耗关系
- [x] 不引入生产代码/契约/ORM 模型变更（纯测试 + helper 扩展）

### Phase 2 — 质检门控阻断完工浏览器层 E2E（Gate 1 路径）

Status: completed
Targets: `tests/e2e/orchestration/mfg-inspection-gate.spec.ts`（新建），`playwright.config.ts`（webServer JVM args 追加），`tests/e2e/orchestration/_helper.ts`（`runMfgChain` 扩展支持 BOM inspectionRequired）
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 1 完成 + 2237-3 完成（质检触发后端已落地）

- [x] `Add`：`playwright.config.ts:18` webServer command 追加 `-Derp-mfg.inspection-gate-enabled=true`
  - Skill: none
- [x] `Add`：扩展 `runMfgChain`——可选参数 `inspectionRequired`（默认 false）。当 true 时，BOM 经 GraphQL `ErpMfgBom__save` 创建时设 `inspectionRequired=true`，使 Gate 1 `isInspectionGated` 命中（config + bomId + inspectionRequired 三条件满足）
  - Skill: none
- [x] `Proof`：负路径——经 `runMfgChain({ inspectionRequired: true })` 驱动至 `reportCompletion`（满量完工 willFinish=true），断言返回 `ERR_INSPECTION_REQUIRED` 错误（GraphQL errors[0].message 含错误码/描述），WorkOrder docStatus 保持 IN_PROCESS（未完工）
  - 成功模式（负路径）：reportCompletion 被 Gate 1 拦截，docStatus 不变
  - Skill: none
- [x] `Proof`：对照路径——经 `runMfgChain({ inspectionRequired: false })`（默认）驱动至 `reportCompletion` 成功 docStatus=COMPLETED（Gate 1 未命中——inspectionRequired=false），验证门控精确性
  - Skill: none

Exit Criteria:

- [x] `mfg-inspection-gate.spec.ts` 全绿：inspection-gate-enabled=true + BOM.inspectionRequired=true 时 reportCompletion 抛 `ERR_INSPECTION_REQUIRED` + docStatus 保持 IN_PROCESS；inspectionRequired=false 时 reportCompletion 成功 COMPLETED
- [x] 不引入生产代码/契约/ORM 模型变更（纯测试 + config + helper 扩展）

## Draft Review Record

- Independent draft review iteration 1: needs revision (`ses_0b19ee1e3ffedEhCGOGNUI1Z2d`, general agent 新会话) — 3 BLOCKING: ① genealogy-write-enabled 默认 true 非 false（`BatchGenealogyWriter.isWriteEnabled:241`），无需加 config，既有 mfg-chain spec 已在基因链为空状态运行；② 质检门控有两条独立路径（Gate 1 config+BOM.inspectionRequired / Gate 2 mandatory-inspection-bill-types），原计划 setup 两路径均不命中；③ 种子目录路径错误（`db/_init-data/`→`app-erp-all/src/main/resources/_vfs/_init-data/`）+ 无 `erp_inv_batch.csv` 种子。全部已修订：baseline 纠正默认 true + 两门控路径裁决 Gate 1 + Phase 1 移除 config-add + 输入批次运行时创建 + Phase 2 改为 Gate 1（config + BOM.inspectionRequired）+ 种子路径纠正。
- Independent draft review iteration 2: accept (`ses_0b19569a8ffeWdBfMVFEiChlW7`, general agent 新会话) — 3 BLOCKING 全部确认修复：① genealogy 默认 true baseline 已纠正 + Phase 1 移除 config-add；② Gate 1/Gate 2 裁决记录 + Phase 2 经 Gate 1（config + BOM.inspectionRequired）；③ 种子路径纠正 `app-erp-all/src/main/resources/_vfs/_init-data/` + 无 erp_inv_batch 种子声明。规则 1/4/7/9/10/14/anti-slack 全通过。计划为可接受执行契约。

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。在结束时运行 `npx playwright test`（或项目等效命令）一次。

- [x] 范围内行为完成（基因追溯 forwardTrace/backwardTrace 非空 + 质检门控阻断负路径）
- [x] 相关文档对齐（`e2e-runbook.md` §制造链编排层段扩展 — 基因追溯/质检门控子段；1800-2/0704-2 Deferred 标记承接 done）
- [x] 已运行验证（`npx playwright test tests/e2e/orchestration/mfg-genealogy.spec.ts tests/e2e/orchestration/mfg-inspection-gate.spec.ts` 全绿 + 全 workspace `npx playwright test` 无回归）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 基因追溯链可视化前端（echarts 树/图）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 前端可视化（echarts 树/图渲染追溯链路）属前端能力面，1606-2 Deferred。本计划验证后端查询 API 经 GraphQL 浏览器层可达 + 数据正确性。
- Successor Required: yes（触发条件：追溯链交互式可视化需求落地时）

### 召回报告降级标记消除

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: `recallReport` 当前降级（`degraded=true`），仅返回受影响成品批次集合，不含库存位置/已售去向。依赖 inventory 域按批次查询能力，0305-3 Deferred。
- Successor Required: yes（触发条件：inventory 域按批次库存位置/已售去向查询 API 落地时）

## Closure

Status Note: 执行完成（2026-07-11，主代理执行 Phase 1+2 全绿）。制造域 config-gated 两项功能浏览器层 E2E 落地：批次基因追溯（`mfg-genealogy.spec.ts`，`runMfgChain({ withBatchTracking: true })` 创建输入 ErpInvBatch + 备货/领料移动携 batchNo 使余额按批次维度落入 + MaterialIssueLine.batchNo → 满量完工触发 BatchGenealogyWriter 写入 inputLot→outputLot 基因链 + forwardTrace/backwardTrace 返回非空消耗关系）+ 质检门控 Gate 1（`mfg-inspection-gate.spec.ts`，config `erp-mfg.inspection-gate-enabled=true` + BOM.inspectionRequired=true → reportCompletion 被 `isInspectionGated` 拦截抛 ERR_INSPECTION_REQUIRED + docStatus 保持 IN_PROCESS 负路径 + inspectionRequired=false 对照路径 COMPLETED）。`runMfgChain` 扩展 `withBatchTracking`/`inspectionRequired` 两可选参数 + `callMutation` 导出 + `cleanupMfg` 扩展基因链产物清理。**批次余额维度裁决**（StockMoveBookkeeper.upsertBalance 以 batchNo 为余额键维度之一，故备货/领料须共用同一 batchNo 使余额按批次维度落入，否则领料出库时可用量不足）。验证全绿：`npx playwright test --workers=1`（PLAYWRIGHT_PORT=8011）177 passed（174 基线 + 3 新测试：1 genealogy + 2 inspection-gate），0 回归，23.1m。纯测试+config+helper 扩展，零生产代码/契约/ORM 模型变更。结束审计由独立子代理执行（closure audit gate `[ ]` 留待独立审计）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，closure auditor task，不复用执行者上下文）— approved
- Audit Scope: 语义验证六项（phase/items 一致性、exit criteria vs live repo、anti-hollow、five-point consistency、deferred honesty、docs sync）
- Live-Repo Verification:
  - `tests/e2e/orchestration/mfg-genealogy.spec.ts` 存在且非空壳——真实断言 findPage 非空 + inputMaterialId/outputMaterialId 正确 + forwardTrace/backwardTrace 返回非空消耗关系（非 `return null`/空体/吞异常）
  - `tests/e2e/orchestration/mfg-inspection-gate.spec.ts` 存在且非空壳——负路径断言 reportCompletion GraphQL errors 含「质检」token + docStatus 保持 IN_PROCESS；对照路径断言 COMPLETED
  - `playwright.config.ts:18` 确含 `-Derp-mfg.inspection-gate-enabled=true`（与 baseline 一致）
  - `_helper.ts` `runMfgChain` 扩展 `withBatchTracking`（创建输入 ErpInvBatch + 备货/领料移动携 batchNo + MaterialIssueLine.batchNo）+ `inspectionRequired`（BOM __save 设字段）+ `callMutation` 导出 + `cleanupMfg` 基因链产物清理——均运行时可达，非注册未引用
- Five-Point Consistency: Plan Status=completed / Phase 1+2 Status=completed / 两阶段 Exit Criteria 全 [x] / Closure Gates 全 [x] / Closure 证据非 placeholder — 一致
- Deferred Honesty: Gate 2（mandatory-inspection-bill-types）显式 Non-Goal；可视化前端 + 召回报告降级均 out-of-scope improvement 且命名后继触发条件——无范围内缺陷隐藏
- Docs Sync: `docs/logs/2026/07-11.md` +0730-1 条目；`docs/testing/e2e-runbook.md` 增 config-gated 制造域子段 + 套件计数 174→177；`docs/backlog/README.md` +0730-1 ✅；`docs/testing/known-good-baselines.md` +2026-07-11 full-green 基线行——纯测试+config+helper，无 owner-doc/architecture 基线变更
- Execution Evidence（执行者记录，审计复核确认）: `npx playwright test --workers=1`（PLAYWRIGHT_PORT=8011）= 177 passed（23.1m），0 回归。Phase 1 `mfg-genealogy.spec.ts` 1 passed（7.4s）+ Phase 2 `mfg-inspection-gate.spec.ts` 2 passed（28.9s）+ orchestration 局部 7 passed（1.1m，P2P/O2C/制造链/差异 0 回归）。

Follow-up:

- 基因追溯链可视化前端（见上方 Deferred）
- 召回报告降级标记消除（见上方 Deferred）
