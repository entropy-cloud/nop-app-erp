# 2026-07-10-0704-2 制造工单完整链路编排浏览器层 E2E

> Plan Status: completed
> Last Reviewed: 2026-07-10
> Mission: erp
> Work Item: 各域细化端到端验证（manufacturing WorkOrder 齐套校验→领料出库→报工→完工入库跨多实体编排链浏览器层 E2E）
> Source: `2026-07-10-0335-1` Deferred「WorkOrder 完整制造链编排 E2E」+ `2026-07-09-0814-2` Deferred「全 18 域全业务动作覆盖」manufacturing 子集
> Related: `2026-07-10-0335-1`（WorkOrder 审批轴单步 E2E 源）、`2026-07-09-1249-1`（P2P/O2C 跨域编排范式源）、`docs/plans/2026-07-02-2237-1-manufacturing-workorder-jobcard-state-machine.md`（后端工单/工序卡状态机实现源）
> Audit: required

## Current Baseline

- **WorkOrder 审批轴单步 E2E 已全绿**（`0335-1` `mfg-work-order.action.spec.ts`）：`submitForApproval`→`approve`→approveStatus 翻转 + `checkAvailability`（无子件 BOM 作 enabler → STOCK_RESERVED）→`start`(IN_PROCESS)→`close`(CLOSED)。**仅验证审批轴 + 单步状态迁移，未覆盖领料出库/报工/完工入库的跨多实体编排链**。
- **后端集成测试已全绿**（`2237-1`）：`TestErpMfgWorkOrderStateMachine` 等后端测试经 `IGraphQLEngine.executeRpc` 覆盖工单 10 态状态机 + 齐套校验 + 领料出库 + 报工 + 完工入库 + 成本归集。后端种子经直接 ORM 创建（非 `__save`），用户上下文为 SYS(id=0)。**浏览器层未经 GraphQL 驱动完整链路验证**。
- **制造链路三聚合根经 Java 源核实**（explore 子代理核实）：
  - **WorkOrder**（`ErpMfgWorkOrderBizModel` Facade → `ErpMfgWorkOrderProcessor`）：`checkAvailability`(L43, NOT_STARTED→STOCK_RESERVED/PARTIAL) → `start`(L49, →IN_PROCESS) → `reportCompletion`(L79, →COMPLETED, 生成 MANUFACTURING/40 入库移动 + 成本重算) → `close`(L67, →CLOSED, 纯状态迁移无成本)。审批轴 `submitForApproval`/`approve`/`reject`/`reverseApprove` 经 xbiz 声明。
  - **MaterialIssue**（`ErpMfgMaterialIssueBizModel`）：`confirm`(L80, @BizMutation, DRAFT→CONFIRMED→DONE) → 经 `IErpInvStockMoveBiz.generateMove` 生成 OUTGOING/20 出库移动（`relatedBillType=ERP_MFG_ISSUE`）+ 回写 `WorkOrderLine.actualQuantity` + 聚合 `ledger.totalCost`→`WorkOrder.materialCost`。
  - **JobCard**（`ErpMfgJobCardBizModel` Facade → `ErpMfgJobCardProcessor`）：`startJob`(L33, OPEN→WORK_IN_PROGRESS) → `recordWork`(L39, @BizMutation, `@RequestBean JobCardWorkRecord`) 写 `ErpMfgJobCardTimeLog` + 回写 `WorkOrder.laborCost` → `submitJob`(L45, →SUBMITTED) → `completeJob`(L51, →COMPLETED)。
- **种子数据现状**：制造域种子（`0930-1`）含 work_order(4 行) + cost_variance + forecast + forecast_line + workcenter/calendar/capacity/crp_load。**无 BOM / WorkOrderLine / MaterialIssue / JobCard 种子**——本计划 spec 内联创建全部测试数据。
- **库存前置条件**：领料出库需组件物料有库存余额。seed MAT-001(FINISHED_PRODUCT) 在 WH-RAW 无种子余额（O2C spec 备货后清理）。spec 需内联 `generateMove` INCOMING 为组件物料建库存。
- **成本归集断言目标**（Processor 核实）：`reportCompletion` 后 `WorkOrder.completedQuantity` / `materialCost`(MaterialIssue 回写) / `laborCost`(JobCard recordWork 回写) / `totalCost`(material+labor+overhead+subcontract) / `unitCost`(total/completed) 均可经 `verifyState` `__get` 断言。`posted=false`（MANUFACTURING_RECEIPT GL 过账为 Non-Goal，待 finance 域制造过账 Provider）。
- **剩余差距**：WorkOrder 完整制造链（齐套→领料→报工→完工）的浏览器层 E2E 缺失。`0335-1` 仅验证审批轴 + 单步迁移，三聚合根协作（WorkOrder+MaterialIssue+JobCard）的跨实体编排链未经 GraphQL 驱动验证。

## Goals

- 新增 `tests/e2e/orchestration/mfg-chain.spec.ts`：经 GraphQL 驱动 WorkOrder 完整制造链——BOM/组件库存前置 → 审批 → 齐套校验 → 开工 → 领料出库 → 报工 → 完工入库，每步 `verifyState` 经 `__get` 断言状态/成本字段
- 断言跨聚合根协作产物：(1) MaterialIssue.confirm 触发 OUTGOING 库存移动 + WorkOrder.materialCost 回写；(2) JobCard.recordWork 回写 WorkOrder.laborCost；(3) reportCompletion 触发 MANUFACTURING 入库移动 + WorkOrder.totalCost/unitCost 重算 + status=COMPLETED
- 在 `orchestration/_helper.ts` 新增 `runMfgChain` / `cleanupMfg` 编排原语（镜像 P2P/O2C 范式），为后续制造域 successor 提供可复用基础

## Non-Goals

- 完工入库 GL 过账凭证（MANUFACTURING_RECEIPT voucher）——经核实为 Non-Goal（`2237-1` state-machine doc §实现偏离补注，待 finance 域制造过账 Provider）；本计划断言 `posted=false`（库存移动存在但不过账 GL）
- 生产差异计算（`ProductionVarianceCalculator`）——config-gated `erp-mfg.variance-auto-calc-enabled`（默认关），归独立 successor
- 批次基因追溯（`writeBatchGenealogy`）——config-gated `erp-mfg.genealogy-write-enabled`（默认关），归 `0305-3` successor
- APS 排程→JobCard 自动生成（`generateJobCardsFromSchedule`）——`0427-3` 已覆盖；本计划 JobCard 手动创建
- 质检门控（`inspection-gate-enabled`，config-gated 默认关）——归 quality 域 successor
- 全 10 态状态机分支覆盖（stop/resume/cancel）——本计划覆盖核心正路径 COMPLETED，异常分支归 successor

## Task Route

- Type: `verification work`（浏览器层 E2E 覆盖扩展，纯消费侧测试新增，零生产代码/契约/模型变更）
- Owner Docs: `docs/design/manufacturing/state-machine.md`（工单/工序卡状态机 + 三聚合根协作）、`docs/testing/e2e-runbook.md`（E2E 运行手册跨域编排层段）
- Skill Selection Basis: `nop-testing`（E2E 套件 @BizMutation + GraphQL 驱动范式，`1249-1` 编排链 helper 范式可复用）；无后端代码变更，不加载 `nop-backend-dev`

## Infrastructure And Config Prereqs

No infra prereqs beyond existing baseline. 复用既有 Playwright 基础设施 + orchestration helper 原语 + 种子 COA。制造链 config-gated 特性（variance-auto-calc / genealogy-write / inspection-gate）默认关，不影响正路径。

## Execution Plan

### Phase 1 - WorkOrder 完整制造链编排 E2E

Status: completed
Targets: `tests/e2e/orchestration/_helper.ts`（新增 `runMfgChain` / `cleanupMfg`）、`tests/e2e/orchestration/mfg-chain.spec.ts`
Skill: `nop-testing`

- Item Types: `Decision | Add | Proof`
- Prereqs: `0335-1` WorkOrder 审批轴 E2E 范式已验证 + `1249-1` orchestration helper 原语已就绪

- [x] `Decision`：测试数据前置策略裁决——制造链需 BOM + 组件库存 + WorkOrder 行（INPUT/OUTPUT）+ MaterialIssue + JobCard。选项：(a) spec 内联全量创建；() 引用种子数据。选择 (a) 内联创建——种子无 BOM/WorkOrderLine/MaterialIssue/JobCard，且内联创建验证完整链路（含 BOM→齐套校验→领料的依赖链）。
  - 记录替代方案：(b) 更简但种子缺失关键实体，且无法验证 BOM 依赖链。
  - 残留风险：各实体 mandatory 字段名及 WorkOrderLine OUTPUT 行的 `destWarehouseId`（`reportCompletion` 经 `generateCompletionMove` 读此字段生成入库移动，为 null 时静默跳过不抛错——**逻辑必填**非 ORM mandatory）需执行期经 ORM XML 逐项核实填充。BOM/WorkOrderLine 字段名与平台 ORM 约定可能存在差异（如 productId vs materialId、plannedQuantity vs quantity），执行期以 ORM 为权威。
  - Skill: `nop-testing`
  - 执行期裁决补注：组件物料改用**测试专用新建物料**（非种子 MAT-003）——MAT-003 在 WH-RAW 有种子余额（100@8.5，WEIGHTED_AVERAGE），内联备货会与种子混合致 materialCost 非确定性、且清理整行删余额会抹除种子 850 污染 inventory dashboard totalValue（10450→9600 回归）。新建测试物料无种子余额 → materialCost 确定性（1000，无混合）+ 清理整行删余额安全（不污染种子库存基线）。完工入库 posted=false 经核实（MANUFACTURE 移动经 InvPostingDispatcher 优雅降级，MANUFACTURING_RECEIPT GL 过账为 Non-Goal）。`recordWork` 的 `@RequestBean JobCardWorkRecord` 经 GraphQL 展平为独立标量参数（非 record 包装，经后端 `TestErpMfgWorkOrderEndToEnd#recordWorkRequest` 范式核实）。
- [x] `Add`：`orchestration/_helper.ts` 新增 `runMfgChain(page)` 编排函数（链路步骤概要，具体字段经 ORM 核实填充）：
  1. **前置备货**：`generateMove` INCOMING 为组件物料建库存（经 `complete` → DONE），产出确定性 unitCost 供 materialCost 断言
  2. **BOM**：`createViaSave` 建成品 BOM（`ErpMfgBom` 引用成品 MAT-001）+ BOM 行（`ErpMfgBomLine` 引用组件物料 + 用量）
  3. **WorkOrder**：`createViaSave` 建工单头（`ErpMfgWorkOrder` 引用 MAT-001 + bomId + plannedQuantity=10）+ OUTPUT 行（引用成品 + **destWarehouseId=SEED.WH_RAW**，`generateCompletionMove` 必读）+ INPUT 行（引用组件物料 + 需求量）
  4. **审批轴**：`submitForApproval` → `verifyState approveStatus=SUBMITTED` → `approve` → `verifyState approveStatus=APPROVED` + docStatus=NOT_STARTED
  5. **齐套校验**：`checkAvailability` → `verifyState docStatus=STOCK_RESERVED`（KitAvailabilityChecker 读 BOM 组件对照库存余额）
  6. **开工**：`start` → `verifyState docStatus=IN_PROCESS`
  7. **领料出库**：`createViaSave` 建领料单（`ErpMfgMaterialIssue` + 行引用 WorkOrder INPUT 行）→ `confirm` → 查 `ErpInvStockMove`(relatedBillType=ERP_MFG_ISSUE) 存在 + `verifyState WorkOrder.materialCost` > 0
  8. **报工**：`createViaSave` 建工序卡（`ErpMfgJobCard` 引用 WorkOrder）→ `startJob` → `recordWork`（`@RequestBean JobCardWorkRecord`，含 completedQuantity + durationMins + hourlyRate + operatorId）→ `verifyState WorkOrder.laborCost` > 0 → `submitJob` → `completeJob`
  9. **完工入库**：`reportCompletion`(workOrderId, completedQty=10) → `verifyState docStatus=COMPLETED` + completedQuantity=10 + totalCost > 0 + unitCost > 0 → 查 `ErpInvStockMove`(relatedBillType=ERP_MFG_WORK_ORDER) 入库移动存在 + posted=false
  10. 返回 chain result（各实体 id + codes）供 spec 断言/清理
  - Skill: `nop-testing`
- [x] `Add`：`orchestration/_helper.ts` 新增 `cleanupMfg(page, r)` 清理原语——逐域逻辑删除（入库移动凭证+流水+余额 → MaterialIssue(行) → JobCard+TimeLog → 出库移动凭证+流水+余额 → WorkOrderLine → WorkOrder → BOM(行) → 组件物料余额+物料），保护共享 DB 数值断言基线
  - Skill: `nop-testing`
- [x] `Add`：`tests/e2e/orchestration/mfg-chain.spec.ts`——制造链 E2E spec：(1) 调 `runMfgChain` 驱动全链；(2) 断言领料出库触发 OUTGOING 库存移动 + WorkOrder.materialCost > 0；(3) 断言完工入库触发 MANUFACTURING 入库移动 + WorkOrder.status=COMPLETED + completedQuantity=10 + totalCost > 0 + unitCost > 0 + posted=false；(4) `finally` 调 `cleanupMfg` 清理
  - Skill: `nop-testing`
- [x] `Proof`：运行 `npx playwright test tests/e2e/orchestration/mfg-chain.spec.ts --workers=1`，制造链全绿；全套件 0 回归。
  - 验证命令：`npx playwright test tests/e2e/orchestration/mfg-chain.spec.ts --workers=1` + 全套件 `npx playwright test --workers=1`
  - Skill: `nop-testing`
  - 证据：制造链 spec 全绿（7.5s）；全套件 166 passed / 0 failed（21.7m）；`mvn install -DskipTests` 154 模块 BUILD SUCCESS。

Exit Criteria:

- [x] WorkOrder 完整链路（审批→齐套→开工→领料→报工→完工）经 GraphQL 驱动全绿，每步状态翻转经 `verifyState` `__get` 独立断言
- [x] MaterialIssue.confirm 触发 OUTGOING 移动 + WorkOrder.materialCost 回写 + reportCompletion 触发 MANUFACTURE 入库移动 + WorkOrder.totalCost/unitCost 重算 均经验立断言

### Phase 2 - 文档对齐

Status: completed
Targets: `docs/testing/e2e-runbook.md`
Skill: none

- Item Types: `Add`
- Prereqs: Phase 1 全绿

- [x] `Add`：`docs/testing/e2e-runbook.md` 跨域编排层段增「制造链编排」子段——`runMfgChain` 三聚合根协作范式 + 状态/成本断言 + `posted=false` Non-Goal 说明 + 套件计数更新（167→168）
  - Skill: none
  - 执行期裁决补注：实测全套件基线为 165（非 runbook 声称的 167，runbook 计数预先偏低 2），加 1 制造链 spec 后实测 166 passed / 0 failed。按「真相源=实时仓库实测」原则，套件计数校准为 166（overview 段「共 166 测试」+ 全套件运行时间段「166 测试」+ 编排套件行描述增制造链 + 跨域编排层表格增制造链行 + 新增「制造链编排层」子段含 runMfgChain 范式 / 跨聚合根协作产物断言 / 确定性成本裁决（测试专用物料）/ @RequestBean 入参序列化裁决 / posted=false Non-Goal / cleanupMfg 清理原语）。

Exit Criteria:

- [x] e2e-runbook 含制造链编排子段 + 套件计数一致

## Draft Review Record

- Independent draft review iteration 1: needs revision (`ses_0b6dae5f0ffe`，独立 general 子代理，新会话冷重播无执行者上下文) — 2 MAJOR + 2 MINOR：
  - M1（MAJOR）：执行步骤使用错误字段名（materialId→productId, quantity→plannedQuantity, uomId→uoMId, status→docStatus 等），与实际 ORM 不符，且违反 guide rule 6「无代码设计转库」。**已修复**：移除字段级映射细节，改为链路步骤概要 + 残留风险注（字段名以 ORM 为权威，执行期核实）。
  - M2（MAJOR）：OUTPUT WorkOrderLine 缺 `destWarehouseId`——`generateCompletionMove` 读此字段，为 null 时静默跳过（不抛错），致 exit criterion「入库移动存在」必然失败。**已修复**：Decision 残留风险显式标注 `destWarehouseId` 为逻辑必填，步骤 3 OUTPUT 行标注 `destWarehouseId=SEED.WH_RAW`。
  - m1（MINOR）：`recordWork` 的 `JobCardWorkRecord` bean 需 `operatorId`（TimeLog mandatory）。步骤 8 已补含 operatorId 入参。
  - m2（MINOR）：步骤含过多低级字段映射（guide rule 6）。已随 M1 修复抽象化。
  - BizModel/Processor 行为声明（方法名/行号/状态迁移/三聚合根协作）均经 ORM + Java 源核实准确——无行为级偏差。
- Independent draft review iteration 2: accept (`ses_0b6d591ebffe`，独立 general 子代理，新会话冷重播) — M1/M2/m1/m2 全部修复确认：字段名已抽象化（guide rule 6 合规）；destWarehouseId 逻辑必填已标注；operatorId 已补入；Decision 残留风险正确声明字段名不确定性；执行步骤保持足够具体性。无新问题。

## Closure Gates

- [x] 范围内行为完成（WorkOrder 完整制造链 E2E spec 全绿：审批→齐套→开工→领料→报工→完工 + 跨聚合根协作产物断言）
- [x] 相关文档对齐（e2e-runbook 制造链编排子段 + 套件计数）
- [x] 已运行验证：`npx playwright test tests/e2e/orchestration/ --workers=1`（制造链 + 既有 P2P/O2C/反向全绿 0 回归）+ `mvn install -DskipTests`（154 模块 BUILD SUCCESS）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 完工入库 GL 过账凭证（MANUFACTURING_RECEIPT voucher）

- Classification: `out-of-scope improvement`（经核实为 Non-Goal）→ **已由 successor plan `2026-07-10-1100-5` 落地，Deferred 解除**
- Why Not Blocking Closure: 完工入库移动（MANUFACTURING/40）经 `reportCompletion` 生成但 `posted=false`——MANUFACTURING_RECEIPT GL 过账凭证依赖 finance 域制造过账 Provider（尚未构建，见 `2237-1` state-machine doc §实现偏离补注）。成本归集（materialCost/laborCost/totalCost/unitCost 在 WorkOrder 上）已完整，仅 GL 过账缺失。
- Successor Required: `yes` → **已落地（plan 1100-5 completed）**
- Trigger Condition: 当 finance 域制造过账 Provider 落地后。→ **已满足**

### 生产差异计算（ProductionVarianceCalculator）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: config-gated `erp-mfg.variance-auto-calc-enabled`（默认关），完工时不触发差异计算。差异计算 + 过账经 `1838-2` 后端已落地。
- Successor Required: `yes`
- Trigger Condition: 当需验证生产差异浏览器层端到端时。

### WorkOrder 异常分支（stop/resume/cancel）+ 部分完工

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划覆盖核心正路径 COMPLETED。异常分支（stop/resume/cancel）+ 部分完工（reportCompletion qty < planned）+ 质检门控阻断 归独立 successor。
- Successor Required: `yes`
- Trigger Condition: 当需验证 WorkOrder 异常分支浏览器层覆盖时。

## Closure

Status Note: completed

Execution Evidence (executor, 2026-07-10):

- 新增 `tests/e2e/orchestration/mfg-chain.spec.ts`（1 spec / 1 test）+ `tests/e2e/orchestration/_helper.ts` 增 `runMfgChain`/`MFG_EXPECT`/`cleanupMfg` 编排原语（+ `SEED.UOM_KG`）。
- `docs/testing/e2e-runbook.md` 增「制造链编排层」子段 + 跨域编排层表格增制造链行 + 套件计数校准 167→166（实测基线 165 + 1 新 = 166 passed / 0 failed；runbook 预存 167 偏差 2 经实测校准）。
- `docs/backlog/README.md` 增 0704-2 done 行。
- 验证：`npx playwright test tests/e2e/orchestration/ --workers=1` → 5 passed（制造链 + P2P/O2C/反向 0 回归）；`npx playwright test --workers=1` → 166 passed / 0 failed（21.7m）；`mvn install -DskipTests` → BUILD SUCCESS（154 模块）。
- 执行期裁决（偏离草案之处，已记入 Phase Decision 补注）：(1) 组件物料改用测试专用新建物料（非种子 MAT-003）——MAT-003 在 WH-RAW 有种子余额（100@8.5），内联备货混合致 materialCost 非确定性 + 清理抹除种子 850 污染 inventory dashboard totalValue（10450→9600 回归，全套件首轮实测捕获）；新建物料无种子余额 → materialCost 确定性 1000 + 清理安全。(2) `recordWork` 的 `@RequestBean JobCardWorkRecord` 经 GraphQL 展平为独立标量参数（非 record 包装），经后端 `TestErpMfgWorkOrderEndToEnd#recordWorkRequest` 范式核实。(3) posted=false 经实测确认（completionMove.posted=false + WorkOrder.posted=false，MANUFACTURE 移动经 InvPostingDispatcher 优雅降级）。

Closure Audit Evidence:

- Auditor / Agent: 独立 general 子代理 `ses_0b6773866ffe`（新会话冷重播，无执行者上下文）。
- Verdict: **PASS**（0 BLOCKER）。6 项检查全 PASS：计划内一致性（Plan Status completed + 两 Phase 全 `[x]` + Status completed）；mfg-chain.spec.ts 存在且断言完备（ERP_MFG_ISSUE/ERP_MFG_WORK_ORDER 移动 + materialCost/laborCost/totalCost/unitCost + COMPLETED + posted=false + finally cleanupMfg）；runMfgChain/cleanupMfg 协作连贯（测试专用物料 + recordWork 标量展平 + 依赖反向清理）；**零生产代码/模型变更**（仅 _helper.ts/M + mfg-chain.spec.ts/new + 文档，无 module-* 改动）；e2e-runbook 制造链子段 + 166 计数一致（无残留 167 测试）；Deferred 三项保持 Non-Goal。
- Non-blocking note：e2e-runbook:295（0704-1 凭证行段）残留 "167（167→167）" 反映 runbook 预存校准偏差（真实基线 165），属 0704-2 范围外，归后续全文档校准 follow-up。

Follow-up:

- 完工入库 GL 过账凭证 / 生产差异计算 / WorkOrder 异常分支 —— 见「Deferred But Adjudicated」各自 successor 触发条件。
