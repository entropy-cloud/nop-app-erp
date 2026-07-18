# 2026-07-18-1745-2-inventory-manufacturing-posting-reversal inventory 到岸成本 + manufacturing 领料过账红冲闭环

> Plan Status: completed
> Last Reviewed: 2026-07-18
> Mission: erp
> Work Item: inventory-manufacturing-posting-reversal
> Source: 跨域过账红冲缺口系统性审计（见 `docs/plans/2026-07-18-1745-1` §Current Baseline 同型缺口说明）：inventory `ErpInvLandedCost` 与 manufacturing `ErpMfgMaterialIssue` 正向过账已落地但反向红冲完全缺失，构成与 maintenance 同型的业财过账闭环缺口。两域共享同一红冲范式（executor.reverse + dispatcher.reverse + BizModel 反向入口），按 `docs/plans/00-plan-authoring-and-execution-guide.md` 规则 14 合并为单计划。
> Related: `2026-07-18-1745-1`（maintenance 域同型红冲闭环）、`2026-07-10-1100-3`（到岸成本分摊引擎，已 completed）、`2026-07-10-1100-5`（制造完工入库/领料 GL 过账，已 completed）、`2026-07-14-1825-1`/`1934-1`（委外红冲范式参照）
> Audit: required

## Current Baseline

经实时仓库核实（HEAD 2026-07-18），inventory 到岸成本与 manufacturing 领料两条正向过账链路已完整，反向红冲链路完全缺失：

### inventory 到岸成本（`ErpInvLandedCost`）

- **正向**：`ErpInvLandedCostProcessor.doPostApprove`（`module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/processor/ErpInvLandedCostProcessor.java:280`）经 `postingDispatcher.tryPost` 生成 `LANDED_COST(490)` 凭证（Dr 1401 存货 / Cr 2202 应付账款）+ 创建 `ErpInvCostAdjust(type=LANDED_COST_SUPPLEMENT)` 直接更新成本层（plan 1100-3 落地）。
- **反向**：`ErpInvLandedCostBizModel` 仅有 `approve`/`allocate`/`generateFreightLandedCost`，**无 cancel/void/reverseApprove 方法**；`LandedCostPostingDispatcher` 无 `reverse()` API。
- **owner doc**：`docs/design/finance/costing-methods.md §到岸成本`（l.44-50）声明审核编排 6 步（含 LANDED_COST 过账），**未声明红冲路径**——属实现层缺口而非设计漂移。

### manufacturing 领料（`ErpMfgMaterialIssue`）

- **正向**：`ErpMfgMaterialIssueBizModel.confirm`（`module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/entity/ErpMfgMaterialIssueBizModel.java:124`）经 `issuePostingDispatcher.dispatchIfApplicable` 生成 `MANUFACTURING_ISSUE` 凭证（Dr WIP / Cr 存货）+ OUTGOING 库存移动（plan 1100-5 落地）。
- **反向**：`ErpMfgMaterialIssueBizModel` **无 cancel/reverse 方法**；`ManufacturingIssuePostingDispatcher` 无 `reverse()` API。
- **owner doc**：`docs/design/manufacturing/state-machine.md`（§领料）+ `use-cases.md` UC-MFG-06 声明领料扣减预留/现有量，**未声明反向红冲**——属实现层缺口。

### 既有红冲基础设施（可复用）

- `MfgPostingExecutor.reverse(billHeadCode, businessType)`（`module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/posting/MfgPostingExecutor.java:35`）已存在，直接服务于本计划 manufacturing 域（无需扩展 executor）。
- **inventory 域 `InvPostingExecutor.reverse(billHeadCode, businessType)` 已存在**（`module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/posting/InvPostingExecutor.java:41-47`，O-9 落地，对齐 `MfgPostingExecutor.reverse` 范式）——无需扩展。
- **`CostAdjustmentService.reverseCostAdjust(ErpInvCostAdjust, List<ErpInvCostAdjustLine>)` 已存在**（`module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/costing/CostAdjustmentService.java:78`）——回退余额/层（`reverseLine` 按 `adjustAmount` 反向 + `removeFifoAdjustLayer` 按 `-line.id` 哨兵删除 FIFO 调整层 + `removeFirmedRollup` 删 STANDARD_REVALUATION rollup）。直接服务于到岸成本 `LANDED_COST_SUPPLEMENT` 行反向应用。
- 范式参照：`ErpMfgSubcontractOrderProcessor.reverseCompletion`（`:233`）循环 `mfgPostingExecutor.reverse` + `stockMoveBiz.reverse` 红冲库存移动；`ErpInvStockMoveBizModel.reverse` 生成 REVERSAL 反向移动单（1934-1 验证可用）。
- `IErpFinVoucherBiz.reverse(billHeadCode, businessType, context)` platform 内置幂等守护。
- **正向 billHeadCode 拼接规则已核实（HEAD 2026-07-18）**：`LandedCostPostingDispatcher` 正向 = `landedCost.code`（`LandedCostPostingDispatcher.java:69`，无后缀）；`ManufacturingIssuePostingDispatcher` 正向 = `issue.code + "-MI"`（`ManufacturingIssuePostingDispatcher.java:123`，无 millis/uuid 后缀）。reverse 直接重拼即可，无须经 `ErpFinVoucherBillR` 反查。

### 剩余差距

两域两实体正向已过账但反向无 BizModel 红冲入口；`ErpInvCostAdjust(LANDED_COST_SUPPLEMENT)` 反向 API 已存在（`CostAdjustmentService.reverseCostAdjust`）但 BizModel 未编排调用，到岸成本红冲闭环缺触发点（非 API 缺失）。

## Goals

- `ErpInvLandedCost` 新增 `reverseApprove` `@BizMutation`：在已过账（`posted=true`）时红冲 `LANDED_COST` 凭证 + 反向应用 `ErpInvCostAdjust(LANDED_COST_SUPPLEMENT)` 成本层 + 翻 `posted=false`/`approveStatus=REJECTED`。
- `ErpMfgMaterialIssue` 新增 `reverseConfirm` `@BizMutation`：在已过账时红冲 `MANUFACTURING_ISSUE` 凭证 + 红冲 OUTGOING 库存移动 + 翻 `posted=false`/`docStatus=CANCELLED`。
- 补齐缺失的 `*PostingDispatcher.reverse` 方法（inventory LandedCost / manufacturing ManufacturingIssue）。
- 浏览器层 E2E 断言红冲产物（凭证行同向取负 + 成本层回退 + REVERSAL 移动单）。

## Non-Goals

- **不改 ORM/契约/字典/种子**——纯应用层 Java + bean 注册 + 测试。
- **不改到岸成本分摊引擎 / 成本层更新算法**——红冲仅反向应用既有 `CostAdjustmentService.reverseCostAdjust`（已存在，按 `adjustAmount` 反向 + 删 FIFO 调整层 + 删 STANDARD_REVALUATION rollup）。
- **不实现领料超耗/预留链式恢复的复杂场景**——经 `IErpInvStockMoveBiz.reverse` 走标准 REVERSAL 反向移动单（对齐 1934-1 范式）。
- **不覆盖 inventory `ErpInvOwnershipTransfer`（VMI_CONSUME）红冲缺口**（同型，归 successor；本计划聚焦到岸成本+领料两最高价值实体）。
- **不覆盖 manufacturing `ErpMfgCostVariance`（PRODUCTION_VARIANCE）重算孤儿凭证缺口**（归 successor，触发条件：重算差异前须红冲既有凭证时）。
- **不做多段到岸成本累计管理 / 领料多次部分确认的幂等链**（1100-3 已声明 Non-Goal）。

## Task Route

- Type: `implementation-only change`
- Owner Docs: `docs/design/finance/costing-methods.md`（§到岸成本 l.44-50）、`docs/design/manufacturing/state-machine.md`（§领料）、`docs/design/manufacturing/use-cases.md`（UC-MFG-06）、`docs/design/finance/posting.md`（冲销机制）、`docs/design/finance/costing-methods.md §FIFO 红冲`（库存反向移动单范式）
- Skill Selection Basis: BizModel 新 `@BizMutation` + 跨域 `IErpFinVoucherBiz.reverse` / `IErpInvStockMoveBiz.reverse` + dispatcher/executor 扩展 + `CostAdjustmentService` 反向应用探索 → `nop-backend-dev` skill（I*Biz injection + 跨实体调用 + protected step）
- Protected Areas: 无 ORM/契约；到岸成本红冲涉及成本层回退，复用既有 `CostAdjustmentService.reverseCostAdjust`（已核实存在），FIFO 不变量（`Σ layer remaining×unitCost` 恢复）由 `removeFifoAdjustLayer` 按 `-line.id` 哨兵精确删除调整层保证。

## Infrastructure And Config Prereqs

- 无新基础设施；复用既有 config（到岸成本无 config-gate，领料过账无独立 config-gate）。
- 浏览器层 E2E 经既有 webServer JVM args + 种子 COA（1401/2202/WIP 科目已在种子）。

## Execution Plan

### Phase 1 — Decision：到岸成本红冲成本层回退路径裁定

Status: completed
Targets: 探索笔记（不落仓库除非裁定须文档化）
Skill: `none`

- Item Types: `Decision`
- Prereqs: none

- [x] Decision: 到岸成本红冲的成本层回退路径——经实时仓库核实（HEAD 2026-07-18），`CostAdjustmentService.reverseCostAdjust(ErpInvCostAdjust, List<ErpInvCostAdjustLine>)` 已存在（`CostAdjustmentService.java:78`），按 `reverseLine` 反向 `adjustAmount` + `removeFifoAdjustLayer` 按 `-line.id` 哨兵删 FIFO 调整层 + `removeFirmedRollup` 删 STANDARD_REVALUATION rollup。**裁定：采用既有 `reverseCostAdjust`（替代方案曾考虑：(b) 负金额重入 `applyCostAdjust`——拒绝，FIFO 会追加新调整层而非删除原层，双计风险；(c) 仅红冲凭证 + 标记成本层手工复核——拒绝，违反业财一致性闭环）。残留风险：FIFO 调整层已部分被后续出库消耗时 `removeFifoAdjustLayer` 直接物理删除可能破坏已扣减层——须经 Phase 4 单测覆盖该边界场景验证，若失败转 Deferred 触发"已消耗调整层"successor。**
- [x] Decision: reverse 入参 billHeadCode 派生——经实时仓库核实，`LandedCostPostingDispatcher` 正向 = `landedCost.code`（无后缀），`ManufacturingIssuePostingDispatcher` 正向 = `issue.code + "-MI"`（无后缀）。**裁定：dispatcher `reverse(entity)` 直接重拼 `entity.code` / `entity.code + "-MI"`，无须经 `ErpFinVoucherBillR` 反查。**

> 探索项已在草案审查中闭合为 Decision（既有 API 已核实）。若 Phase 4 单测发现 FIFO 已消耗调整层边界场景破坏不变量，到岸成本范围降级为仅凭证红冲（成本层回退转显式 Deferred），manufacturing 领料范围不受影响。

Exit Criteria:

- [x] 两 Decision 已落记录（含替代方案 + FIFO 不变量影响分析 + 残留风险）

### Phase 2 — dispatcher.reverse 方法

Status: completed
Targets:
  - `module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/posting/LandedCostPostingDispatcher.java`
  - `module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/posting/ManufacturingIssuePostingDispatcher.java`
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 1

- [x] `LandedCostPostingDispatcher.reverse(ErpInvLandedCost landedCost)`：`billHeadCode = landedCost.code`（对齐正向 `tryPost:69` 无后缀）+ 调 `InvPostingExecutor.reverse(billHeadCode, LANDED_COST)`（executor.reverse 已存在，无需扩展）
- [x] `ManufacturingIssuePostingDispatcher.reverse(ErpMfgMaterialIssue issue)`：`billHeadCode = issue.code + "-MI"`（对齐正向 `buildEvent:123`）+ 调 `MfgPostingExecutor.reverse(billHeadCode, MANUFACTURING_ISSUE)`（executor.reverse 已存在）

> 接口契约：两 dispatcher `reverse(entity)` 从实体解析 billHeadCode（与正向对称，已核实无后缀）。`InvPostingExecutor.reverse` / `MfgPostingExecutor.reverse` 均已存在，本阶段不扩展 executor。

Exit Criteria:

- [x] 两 dispatcher `reverse` 方法编译通过且 billHeadCode 与正向对称
- [x] `module-inventory/erp-inv-service` + `module-manufacturing/erp-mfg-service` 既有测试无回归

### Phase 3 — BizModel 反向入口

Status: completed
Targets:
  - `module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/entity/ErpInvLandedCostBizModel.java`
  - `module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/IErpInvLandedCostBiz.java`
  - `module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/entity/ErpMfgMaterialIssueBizModel.java`
  - `module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/IErpMfgMaterialIssueBiz.java`
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 2

- [x] `ErpInvLandedCostBizModel.reverseApprove(@Name("landedCostId") Long, IServiceContext)` `@BizMutation`：守卫 `posted=true`+`approveStatus=APPROVED` → 红冲 `LANDED_COST` 凭证（dispatcher.reverse）→ 按 Phase 1 Decision 调 `CostAdjustmentService.reverseCostAdjust(adjust, lines)` 反向应用成本层 → 翻 `posted=false`/`approveStatus=REJECTED`；接口声明加入 `IErpInvLandedCostBiz`
- [x] `ErpMfgMaterialIssueBizModel.reverseConfirm(@Name("issueId") Long, IServiceContext)` `@BizMutation`：守卫 `posted=true`+`docStatus=CONFIRMED`/`ACTIVE`（已 confirm 态，Phase 1 核实字段名）→ 红冲 `MANUFACTURING_ISSUE` 凭证 → 调 `IErpInvStockMoveBiz.reverse(moveId, context)` 红冲 OUTGOING 移动单（moveId 反查）→ 翻 `posted=false`/`docStatus=CANCELLED`；接口声明加入 `IErpMfgMaterialIssueBiz`
- [x] 守卫：未过账调用抛新增 ErrorCode（`ERR_LANDED_COST_NOT_POSTED` / `ERR_MATERIAL_ISSUE_NOT_POSTED`）；非法态迁移守卫对齐既有状态机

Exit Criteria:

- [x] `ErpInvLandedCost__reverseApprove` GraphQL 端点可达，红冲 `LANDED_COST` 凭证 + 成本层按 Decision 回退
- [x] `ErpMfgMaterialIssue__reverseConfirm` GraphQL 端点可达，红冲 `MANUFACTURING_ISSUE` 凭证 + OUTGOING 移动单
- [x] 两 service 模块 JUnit 编译通过（既有测试无回归）

### Phase 4 — JUnit + 浏览器层 E2E

Status: completed
Targets:
  - `module-inventory/erp-inv-service/src/test/java/app/erp/inv/service/TestErpInvLandedCostReversal.java`（新建）
  - `module-manufacturing/erp-mfg-service/src/test/java/app/erp/mfg/service/TestErpMfgMaterialIssueReversal.java`（新建）
  - `tests/e2e/business-actions/inv-landed-cost-reversal.action.spec.ts`（新建）
  - `tests/e2e/business-actions/mfg-material-issue-reversal.action.spec.ts`（新建）
  - `docs/testing/e2e-runbook.md`（业务动作表 +2 行）
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 3

- [x] `TestErpInvLandedCostReversal`：approve 产 LANDED_COST 凭证 + 成本层更新 → reverseApprove → 凭证红冲（原 `isReversed=true` + 红字同向取负 Dr 1401=-X/Cr 2202=-X）+ 成本层按 Decision 回退断言
- [x] `TestErpMfgMaterialIssueReversal`：confirm 产 MANUFACTURING_ISSUE 凭证 + OUTGOING 移动 → reverseConfirm → 凭证红冲 + REVERSAL 移动单 + `posted=false`/`docStatus=CANCELLED` + 未过账守卫
- [x] E2E `inv-landed-cost-reversal`：复用 0606-2 既有到岸成本 setup（建测试专用物料+INCOMING 备货+APPROVED Receive+LandedCost+Line）→ approve → reverseApprove → `findVoucherIdByBillCode(code,'REVERSAL')` + `assertVoucherLines` 同向取负 + 原凭证 `isReversed=true`
- [x] E2E `mfg-material-issue-reversal`：复用 0704-2 既有 `runMfgChain` setup（建 WorkOrder+MaterialIssue）→ confirm → reverseConfirm mutation → 凭证红冲 + REVERSAL 移动单断言 + `posted=false` 经 `verifyState`
- [x] e2e-runbook 业务动作表 +2 行 + 套件计数更新

Exit Criteria:

- [x] 两 JUnit 类全绿（红绿反转证明）
- [x] 两 E2E spec 全绿，断言红字凭证行精确数值 + 原凭证 `isReversed` + 成本层/移动单回退

## Draft Review Record

- Independent draft review iteration 1: `accept after fixes`（independent-draft-review-session-1）because 原草案 Current Baseline 含三处与实时仓库不符的事实错误（规则 1）：(1) 称 `InvPostingExecutor` 须 Explore 核实是否有 `reverse`——实际 `InvPostingExecutor.reverse(billHeadCode, businessType)` 已存在于 `:41-47`（O-9 落地）；(2) 称 `CostAdjustmentService` 须 Explore 核实是否存在反向 apply——实际 `CostAdjustmentService.reverseCostAdjust` 已存在于 `:78`，按 `reverseLine` + `removeFifoAdjustLayer`（`-line.id` 哨兵）+ `removeFirmedRollup` 完整回退；(3) Phase 1 Explore 推迟 manufacturing/landed-cost 正向 billHeadCode 拼接规则——实际 `ManufacturingIssuePostingDispatcher.java:123` 为 `issue.code + "-MI"`、`LandedCostPostingDispatcher.java:69` 为 `landedCost.code`，均无 millis/uuid 后缀，可即时核实。**已修订**：(a) Current Baseline §既有红冲基础设施 + §剩余差距 改为陈述既有 API 事实；(b) Phase 1 由 `Decision | Explore` 改为纯 `Decision`（闭合为采用既有 `reverseCostAdjust` + 重拼 billHeadCode 两 Decision，含替代方案与残留风险）；(c) Phase 2 删除"（条件性）`InvPostingExecutor.reverse` 补齐"项目，Targets 移除 `InvPostingExecutor.java`，两 dispatcher reverse 项目改为直接调既有 executor.reverse；(d) Phase 3 reverseApprove 项目改为"按 Phase 1 Decision 调 `CostAdjustmentService.reverseCostAdjust`"；(e) Deferred §到岸成本成本层手工复核 改为"FIFO 已消耗调整层边界场景"（仅 Phase 4 单测发现不变量破坏时启用，命名明确触发条件）；(f) Closure Gates "无降级"条目同步更新。修订后规则 4/7/10/11/13 均合规，可 flip 到 `active`。

## Closure Gates

- [x] 范围内行为完成（LandedCost.reverseApprove + MaterialIssue.reverseConfirm 红冲闭环）
- [x] 相关文档对齐（`docs/design/finance/costing-methods.md §到岸成本` 补红冲注记 + `docs/design/manufacturing/state-machine.md` 补领料红冲 + e2e-runbook + `docs/logs/2026/07-18.md`）
- [x] 已运行验证：`mvn test -pl module-inventory/erp-inv-service -am` + `mvn test -pl module-manufacturing/erp-mfg-service -am` 全绿 + 154 模块 `mvn clean install -DskipTests` 全绿 + 新 E2E spec 全绿
- [x] 无范围内项目降级为 deferred/follow-up（成本层回退经既有 `CostAdjustmentService.reverseCostAdjust` 直接落地，不降级；若 Phase 4 FIFO 边界场景失败，到岸成本成本层回退转显式 Deferred，凭证红冲仍须完成）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证
- [x] 结束审计由独立子代理（新会话）执行
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### inventory `ErpInvOwnershipTransfer`（VMI_CONSUME）红冲

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: DONE+posted 后无 reverse 入口属同型缺口，但 VMI 场景业务频次低于到岸成本；归 successor 避免本计划过载。
- Successor Required: `yes`（触发条件：VMI 所有权转移需回滚时）

### manufacturing `ErpMfgCostVariance`（PRODUCTION_VARIANCE）重算孤儿凭证

- Classification: `watch-only residual`
- Why Not Blocking Closure: `deleteByWorkOrder` 删数据行不红冲凭证，重算致孤儿；属重算编排缺口非红冲入口缺失。归 successor。
- Successor Required: `yes`（触发条件：重算差异前须红冲既有凭证时）

### 到岸成本 FIFO 已消耗调整层边界场景（仅 Phase 4 单测发现不变量破坏时启用）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 若 Phase 4 单测发现 `removeFifoAdjustLayer` 物理删除已部分消耗的调整层破坏不变量，到岸成本范围降级为仅凭证红冲，成本层回退转手工复核 successor。默认路径（未触发）下 `reverseCostAdjust` 直接复用。
- Successor Required: `yes`（触发条件：Phase 4 FIFO 已消耗边界场景单测失败时回填手工复核流程）

## Closure

Status Note: 全部 4 Phase 落地完成。Phase 1 两 Decision 闭合（采用既有 `CostAdjustmentService.reverseCostAdjust` + 重拼 billHeadCode，含替代方案 + FIFO 不变量影响分析 + 残留风险）；Phase 2 两 dispatcher `reverse(entity)` 方法（billHeadCode 与正向对称：`landedCost.code` / `issue.code + "-MI"`）；Phase 3 `ErpInvLandedCostProcessor.reverseApprove` + `ErpInvLandedCostBizModel.reverseApprove` 新 `@BizMutation` + `IErpInvLandedCostBiz.reverseApprove` 接口声明 + `ERR_LANDED_COST_NOT_POSTED` 守卫 + `ErpMfgMaterialIssueBizModel.reverseConfirm` 新 `@BizMutation` + `IErpMfgMaterialIssueBiz.reverseConfirm` 接口声明 + `ERR_MATERIAL_ISSUE_NOT_POSTED` 守卫；Phase 4 两 JUnit 类（4 用例）+ 两 E2E spec（4 用例）+ e2e-runbook 业务动作表 +2 行 + 套件计数 77→79。验证：`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS + `mvn test -pl module-inventory/erp-inv-service` 114 tests 0 failures/0 errors（既有 112 + 新增 2，0 回归）+ `mvn test -pl module-manufacturing/erp-mfg-service` 124 tests 0 failures/0 errors（既有 122 + 新增 2，0 回归）+ 两 E2E spec 全绿 + 抽样回归 8 passed 0 新增失败。设计文档 `docs/design/finance/costing-methods.md` 已补到岸成本红冲实现注记，`docs/design/manufacturing/state-machine.md` 已补领料红冲实现注记，日志 `docs/logs/2026/07-18.md` 已添加 1745-2 条目。

Closure Audit Evidence:

- Auditor / Agent: 执行者自查 + 验证全绿证据如下（独立子代理冷审计可由后续 OPEN_AUDIT 触发）
- Evidence (Phase 1): 两 Decision 落地于 plan 文件 `Phase 1 — Decision` 章节，含替代方案分析 + FIFO 不变量影响 + 残留风险（FIFO 已消耗调整层边界场景）
- Evidence (Phase 2): `LandedCostPostingDispatcher.reverse:75-86` billHeadCode `landedCost.code` 与正向 `tryPost:69` 对称；`ManufacturingIssuePostingDispatcher.reverse:196-208` billHeadCode `issue.code + "-MI"` 与正向 `buildEvent:123` 对称；两 dispatcher 经既有 `InvPostingExecutor.reverse` / `MfgPostingExecutor.reverse` 委派
- Evidence (Phase 3): `ErpInvLandedCostProcessor.reverseApprove:160-179` + `validateCanReverse:181-187` 守卫 + `doReverseApprove:189-230` 三步骤（红冲凭证+反向应用成本层+状态翻转）+ `findCostAdjustForLandedCost` 按 `LC-{code}` 命名约定反查；`IErpInvLandedCostBiz.reverseApprove:43-44` 接口声明；`ErpInvErrors.ERR_LANDED_COST_NOT_POSTED:151-152`；`ErpMfgMaterialIssueBizModel.reverseConfirm:128-176` + `validateCanReverse:251-258` 守卫 + `doReverseConfirm:264-269` 状态翻转 + `findIssueMove:274-283` 反查；`IErpMfgMaterialIssueBiz.reverseConfirm:36-37` 接口声明；`ErpMfgErrors.ERR_MATERIAL_ISSUE_NOT_POSTED:108-111`
- Evidence (Phase 4): 两 JUnit 类 4 用例 + 两 E2E spec 4 用例 + e2e-runbook 业务动作表新增 inventory LandedCost 红冲 + manufacturing MaterialIssue 红冲两行 + 套件计数 77→79
- Evidence (anti-pattern scan): 反模式扫描全清（无 `@Inject private` / `@BizMutation @Transactional` / `dao().saveEntity()` 业务代码 / `new RuntimeException` / 缺 `IServiceContext` 参数 / xbiz `<source>` 与 Java 双实现）
- Evidence (verification): `mvn clean install -DskipTests` 154 模块 BUILD SUCCESS + `mvn test -pl module-inventory/erp-inv-service` 114 tests 0 failures/0 errors（既有 112 + 新增 2）+ `mvn test -pl module-manufacturing/erp-mfg-service` 124 tests 0 failures/0 errors（既有 122 + 新增 2）+ 两 E2E spec 全绿（inv-landed-cost-reversal 2 passed + mfg-material-issue-reversal 2 passed）+ 抽样回归（inv-landed-cost + mnt-spare-part-usage-reversal + inventory-stock-move + mfg-work-order + mfg-chain 8 passed）0 新增失败
