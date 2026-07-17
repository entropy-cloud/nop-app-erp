# 2026-07-14-0606-2-landed-cost-spare-part-posting-e2e 到岸成本分摊 + 维修备件消耗 GL 过账生命周期浏览器层 E2E

> Plan Status: completed
> Last Reviewed: 2026-07-14
> Source: `docs/plans/2026-07-10-1100-3-landed-cost-allocation.md`（到岸成本引擎 S-3 completed，零生命周期浏览器层 E2E）+ `docs/plans/2026-07-10-1100-6-maintenance-spare-part-posting.md`（备件消耗 GL 过账 S-6 completed，零浏览器层 E2E）；AGENTS.md 当前项目阶段重点「各域细化端到端验证」
> Related: `2026-07-10-1100-3`（到岸成本引擎源）、`2026-07-10-1100-6`（备件消耗过账源）、`2026-07-10-0704-1`（凭证行数值断言范式 findVoucherIdByBillCode/assertVoucherLines）、`2026-07-11-2329-1`（logistics path-2 到岸成本自动编排后端，Non-Goal）、`docs/testing/e2e-runbook.md`（业务动作套件）
> Audit: required

> **R14 bundling justification**：本计划跨 inventory（到岸成本）+ maintenance（备件消耗）两域。两 Phase 共享同一结果表面「recently-landed GL 过账后端的审核/确认生命周期浏览器层 E2E + 凭证行数值断言」，复用同一套 helper 原语（findVoucherIdByBillCode/assertVoucherLines/cleanupVoucherByBillCode），同一验证模式（approve/confirm→过账→凭证行断言→清理）。两 Phase 互相独立无硬依赖，合并可一次性收口「posting-landed-but-E2E-missing」缺口类别，避免两个等价小计划碎片化。仓库先例：0413-2（银行对账+坏账两 finance 子面）、1800-1（库存移动+NCR SCRAP 两域）均为同模式跨域/跨子面合并单计划。

## Current Baseline

两个 GL 过账后端已全部落地，但**零浏览器层 E2E 覆盖其生命周期**：

### 到岸成本分摊（inventory 域，S-3 / plan `2026-07-10-1100-3` completed）

- **BizModel**（`ErpInvLandedCostBizModel`）：`approve(id)` `@BizMutation @SingleSession`（审核编排：分摊→CostAdjust 成本层更新→LANDED_COST(490) 过账）+ `allocate(id)` `@BizQuery`（返回分摊预览 `List<Map>`，不改状态）+ `generateFreightLandedCost(receiveCode, ...)` `@BizMutation`（logistics path-2 自动建单，归 Non-Goal）。**无 useWorkflow / 无 useApproval**，DIRECT 浏览器层可达。
- **Processor**（`ErpInvLandedCostProcessor`）：approve 三步编排——`LandedCostAllocationEngine`（3 种分摊方法 BY_AMOUNT/BY_QUANTITY/BY_WEIGHT）→ `CostAdjustmentService` 更新 `ErpInvCostLayer` 成本层 → `IErpFinPostingExecutor.execute(LANDED_COST)` 过账。
- **AcctDocProvider**（`LandedCostAcctDocProvider`）：LANDED_COST(490) 凭证——借：每入库行→存货科目(1401)，金额=该行分摊金额；贷：每费用要素→应付账款(2202)，金额=费用金额，partnerId=费用行应付对象。billHeadCode = `landedCost.getCode()`（无后缀，`LandedCostPostingDispatcher.java:69`，幂等/红冲键）。
- **finance-voucher-post spec 仅测 post 入口**：`tests/e2e/business-actions/finance-voucher-post.action.spec.ts` 用 LANDED_COST(490) 作最简 PostingEvent 测 `IErpFinVoucherBiz.post` 入口可达性，**不测 ErpInvLandedCost 审核生命周期**（approve→分摊→CostAdjust→过账全链）。
- **后端测试已有**：`TestErpInvLandedCost`（9 tests：3 分摊方法 + CostAdjust + 过账 + 红冲）。

### 维修备件消耗 GL 过账（maintenance 域，S-6 / plan `2026-07-10-1100-6` completed）

- **BizModel**（`ErpMntSparePartUsageBizModel`）：`confirm(usageId)` `@BizMutation`——备件消耗确认 → `SparePartIssueService` 经 `IErpInvStockMoveBiz.generateMove`(OUTGOING, relatedBillType=ERP_MNT_SPARE_PART) 扣减库存 → `MaintenanceIssuePostingDispatcher.dispatchIfApplicable`（config-gated `erp-mnt.spare-part-posting-enabled` 默认 false）。**无 useWorkflow / 无 useApproval**，DIRECT 浏览器层可达。
- **AcctDocProvider**（`MaintenanceIssueAcctDocProvider`）：MAINTENANCE_ISSUE(492) 凭证——借：维修费用(6602，config `erp-mnt.expense-subject-code`) / 贷：存货(1403，物料类别存货科目)。billHeadCode=`{usage.code}-MI`，幂等判重。
- **InvPostingDispatcher 跳过**：`ERP_MNT_SPARE_PART` 返回 null（maintenance 域独占过账，防误派 SALES_OUTPUT）。
- **零浏览器层 E2E**：`rg SparePart tests/e2e/business-actions/` 零命中；maintenance 域现有 spec 覆盖 Visit/Request 状态机，**不含备件消耗 confirm + GL 过账**。

**E2E 基础设施就绪**：`tests/e2e/business-actions/_helper.ts` 三原语 + `orchestration/_helper.ts` `findVoucherIdByBillCode` + `assertVoucherLines` 两原语已落地（0704-1 范式），支持 LANDED_COST/MAINTENANCE_ISSUE 凭证行数值断言。maintenance 域种子数据已有（equipment/warehouse/spare_part_usage 表，0930-2）。

## Goals

- 到岸成本审核生命周期经 GraphQL `/graphql` 浏览器层全栈可达性 + LANDED_COST 凭证验证
- 覆盖 ErpInvLandedCost approve→分摊→CostAdjust→LANDED_COST(490) 过账 + 凭证行数值断言（Dr 1401 / Cr 2202）+ allocate 分摊预览查询
- 维修备件消耗 confirm→MAINTENANCE_ISSUE(492) 过账经 GraphQL 浏览器层全栈可达 + 凭证行数值断言（Dr 6602 / Cr 1403）（config-gated 关闭向后兼容对照经后端单测覆盖，见 Deferred）
- 复用既有三原语范式验证在到岸成本分摊型 + 维修备件消耗型 BizModel 下的可复用性

## Non-Goals

- **logistics path-2 自动建单（`generateFreightLandedCost`）**——经 shipment DELIVERED 事件驱动自动创建 DRAFT ErpInvLandedCost（plan 2329-1 后端），非浏览器面手动 approve 入口，归跨域编排 successor
- **到岸成本多段累计管理**——同一入库单多次追加（1100-3 Deferred），归 successor
- **到岸成本 reverse/红冲**——若 Processor 无 reverse 方法（Explore 核实），归后端 successor
- **维修工时费用化过账**（Dr 维修费用 / Cr 应付职工薪酬）——1100-6 Deferred，需工时归集体系，归 successor
- **维修工单成本汇总（备件+工时+外协按设备/部门分摊）**——1018-3 Deferred，归 successor
- **与 assets 域 MAINTENANCE_EXPENSE(470) 并存防双重扣减 E2E**——1100-6 后端已处理（linkedVisit 分支），跨域并存场景复杂度高，归 successor

## Task Route

- Type: `verification work`（扩展现有 Playwright E2E 套件覆盖至到岸成本审核生命周期 + 维修备件消耗 GL 过账）
- Owner Docs: `docs/design/finance/costing-methods.md`（§到岸成本 §到岸成本凭证步骤4）、`docs/design/maintenance/state-machine.md`（§实现偏离补注 备件消耗过账）、`docs/testing/e2e-runbook.md`（业务动作套件 + 调用范式）
- Skill Selection Basis: 浏览器层 E2E 测试编写 → 无匹配技能（Playwright 浏览器层非 `nop-testing` 后端快照范畴）；沿用 `_helper.ts` 既有范式 → `Skill: none`

## Infrastructure And Config Prereqs

- `playwright.config.ts` webServer JVM args 追加 `-Derp-mnt.spare-part-posting-enabled=true`（Phase 2 备件消耗 GL 过账启用；config 默认 false 向后兼容，E2E 按需开启对齐 subcontract-posting-enabled/inspection-gate-enabled 既有范式）
- 到岸成本（Phase 1）approve→过账**非 config-gated**（1100-3 无 config 门控），无需额外 webServer arg
- Phase 2 备件消耗需 equipment + warehouse + 备件物料 + 库存余量前置（自包含 setup 或种子复用）

## Execution Plan

### Phase 1 - 到岸成本审核生命周期 + LANDED_COST 过账 E2E

Status: completed
Targets: `tests/e2e/business-actions/inv-landed-cost.action.spec.ts`（新建）
Skill: none

- Item Types: `Add | Proof | Decision`
- Prereqs: 无（自包含 setup）

- [x] `Decision | Explore`: **到岸成本 approve setup 可达性核实**
  - Explore `ErpInvLandedCostProcessor.approve` 的前置依赖：`ErpInvLandedCost`（DRAFT）+ `ErpInvLandedCostLine`（费用要素行：costElement + amount + supplierId/partnerId）+ 关联 `ErpPurReceive`（receiveId FK mandatory）+ Receive 行已有库存余量（CostAdjust 更新成本层需源余额）。核实自包含建 LandedCost + Line + Receive + 库存余量是否足够，还是需 posted Receive 前置。
  - Decision：裁定**自包含 __save setup（非 runP2pChain）**。runP2pChain 硬编码 `SEED.MAT_1`（`orchestration/_helper.ts:288`），物料不可控——复用会为 MAT_1 在 WH-RAW 新增余额行且 CostAdjust 修改 avgCost，污染 inventory dashboard totalValue 基线。裁定：建测试专用物料（RAW_MATERIAL, MOVING_AVERAGE, 无种子余额 → CostAdjust 确定性 + 清理安全），`__save` 预置 APPROVED Receive（docStatus=ACTIVE erp-pur/doc-status, approveStatus=APPROVED, 无业务副作用——__save 不触发审批工作流，对齐后端 TestErpInvLandedCostEndToEnd#seedReceive 直接置 APPROVED 范式），generateMove INCOMING 建库存余量。billHeadCode 模式裁定：`landedCost.getCode()` 无后缀（LandedCostPostingDispatcher.java:69 经源码核实）。
  - Skill: none
- [x] `Add`: **到岸成本审核生命周期 spec** `inv-landed-cost.action.spec.ts`
  - `allocate(id)` 分摊预览：自包含建 DRAFT `ErpInvLandedCost` + Line → 调 `allocate` `@BizQuery` → 断言返回分摊结果 `List<Map>` 非空（含分摊金额字段，状态不改）
  - `approve(id)` 审核过账：DRAFT → `approve` → `verifyState` 断言 approveStatus=APPROVED + docStatus=DONE + posted=true（`ErpInvLandedCostProcessor.java:284,287,289`）
  - **LANDED_COST 凭证行数值断言**：经 `findVoucherIdByBillCode(landedCost.code, 'NORMAL')` 反查 LANDED_COST 凭证（billHeadCode=`landedCost.getCode()` 无后缀，`LandedCostPostingDispatcher.java:69`）+ `assertVoucherLines` 断言 Dr 1401（存货，分摊金额）/ Cr 2202（应付账款，费用金额）
  - **CostAdjust 成本层更新断言**：approve 后 ErpInvStockBalance.avgCost 更新（MOVING_AVERAGE path 经 applyAverageLike 更新 StockBalance，非 CostLayer；若可达查询——经 materialId 查询可达，avgCost=15=10+50/10）
  - 非法迁移守卫（APPROVED→approve 抛 ErrorCode message token）
  - **清理**：spec finally 经 `cleanupVoucherByBillCode(landedCost.code)` + CostAdjust(行)按 materialId + StockLedger 按 materialId + 逆序删 LandedCost/Receive/库存余额/测试物料（复用 `orchestration/_helper.ts:171` 范式）
  - Skill: none

Exit Criteria:

- [x] 到岸成本 spec 经 `npx playwright test tests/e2e/business-actions/inv-landed-cost.action.spec.ts --workers=1` 全绿（allocate 预览 + approve 过账 + LANDED_COST 凭证行数值断言经 verifyState/findVoucherIdByBillCode 独立断言）
- [x] Explore Decision 已落地（approve setup 可达性 + billHeadCode 模式裁定有记录）

### Phase 2 - 维修备件消耗 confirm + MAINTENANCE_ISSUE 过账 E2E

Status: completed
Targets: `tests/e2e/business-actions/mnt-spare-part-posting.action.spec.ts`（新建）
Skill: none

- Item Types: `Add | Proof | Decision`
- Prereqs: Phase 1 范式验证 + webServer config 启用

- [x] `Decision | Explore`: **备件消耗 confirm setup 可达性核实**
  - Explore `ErpMntSparePartUsageBizModel.confirm` 的前置依赖：`ErpMntSparePartUsage`（DRAFT）+ `ErpMntSparePartUsageLine`（materialId + quantity + warehouseId）+ equipmentId + 备件物料库存余量（generateMove OUTGOING 扣减需源余额）。核实自包含建 SparePartUsage + Line + equipment + 备件物料 + 库存余量是否足够。
  - Decision：裁定最小自包含 setup。备件物料用**测试专用新建物料**（非种子，对齐 0704-2 mfg-chain 测试专用物料隔离范式）避免清理抹除种子余额污染 inventory dashboard totalValue 基线。设备用种子 id=1（EQ-2026-001 RUNNING）——confirm 不改变设备状态（设备状态联动仅 Visit start/complete），无污染风险。config 门控经 webServer JVM arg `-Derp-mnt.spare-part-posting-enabled=true` 启用。billHeadCode 模式裁定：`usage.code + "-MI"`（MaintenanceIssuePostingDispatcher.java:93,129 经源码核实）。
  - Skill: none
- [x] `Add`: **备件消耗 confirm + GL 过账 spec** `mnt-spare-part-posting.action.spec.ts`
  - `confirm(usageId)` 正路径：自包含建 `ErpMntSparePartUsage`（DRAFT + Line + equipment + 备件物料 + 库存余量）→ `confirm` → `verifyState` 断言 posted=true（库存已出库 + GL 已过账）+ docStatus=ACTIVE + OUTGOING 移动单存在（relatedBillType=ERP_MNT_SPARE_PART, docStatus=DONE）
  - **MAINTENANCE_ISSUE 凭证行数值断言**：经 `findVoucherIdByBillCode(usage.code + "-MI", 'NORMAL')` 反查 MAINTENANCE_ISSUE 凭证（billHeadCode=`{usage.code}-MI`，`MaintenanceIssuePostingDispatcher.java:93`）+ `assertVoucherLines` 断言 Dr 6602（维修费用，config subject-code 默认 6602）/ Cr 1403（存货，config subject-code 默认 1403，行级成本）
  - **清理**：spec finally 经 `cleanupVoucherByBillCode(usage.code + "-MI")` + OUTGOING 移动清理（StockLedger/StockMoveLine/StockMove 按 moveId）+ StockLedger 按 materialId + StockBalance 按 materialId+warehouseId + 逆序删 SparePartUsage/备件物料/余额（复用 `orchestration/_helper.ts:171,192` 范式）
  - Skill: none

Exit Criteria:

- [x] 备件消耗 spec 经 `npx playwright test tests/e2e/business-actions/mnt-spare-part-posting.action.spec.ts --workers=1` 全绿（confirm 过账 + MAINTENANCE_ISSUE 凭证行数值断言经 verifyState/findVoucherIdByBillCode 独立断言）
- [x] Explore Decision 已落地（confirm setup 可达性 + 测试专用物料隔离裁定有记录）

## Draft Review Record

- Independent draft review iteration 1: needs revision (ses_0a2788e74ffeNaH0yZhSrQObm6) — 1 BLOCKER + 3 Major + 3 Minor。B1：LANDED_COST billHeadCode 误写 `{code}-LC`（实际 `landedCost.getCode()` 无后缀，`LandedCostPostingDispatcher.java:69`）+ "或 Processor billHeadCode 模式"反松弛不确定 hedge。M1：跨域 bundling 缺 R14 显式 justification。M2：Phase 1 approve 断言 "或 posted=true" 反松弛 hedge（实际 approveStatus=APPROVED + docStatus=DONE + posted=true 三者确定，`ErpInvLandedCostProcessor.java:284,287,289`）。M3：config-gated 关闭对照项内嵌降级路径违反反松弛。N1：缺清理原语引用。N2：runP2pChain 复用机会未提。N3：maintenance 种子声明未核实。
- Independent draft review iteration 2: accept (ses_0a2788e74ffe<待新会话>) after B1/M1/M2/M3/N1/N2 修复——billHeadCode 改 `landedCost.code` 无后缀 + 删 hedge；增 R14 bundling justification 段；approve 断言改三者确定值；config-gated 关闭项移入 Deferred But Adjudicated（watch-only residual，后端单测覆盖）；两 Phase 增清理原语引用；Phase 1 Explore 增 runP2pChain 复用裁定。规则合规 R2-R14 + 反松弛全 PASS。计划可作为执行契约进入实施。

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。完整仓库验证在此处运行一次。

- [x] 范围内行为完成：到岸成本审核生命周期 + LANDED_COST 凭证 + 维修备件消耗 confirm + MAINTENANCE_ISSUE 凭证经 GraphQL 浏览器层全栈可达
- [x] 相关文档对齐：`docs/testing/e2e-runbook.md` 业务动作表 +inventory landed-cost/maintenance spare-part 行 + 套件计数更新
- [x] 已运行验证：`npx playwright test tests/e2e/business-actions/inv-landed-cost.action.spec.ts tests/e2e/business-actions/mnt-spare-part-posting.action.spec.ts --workers=1` 全绿（2 passed, 15.2s）+ 全套件回归无新增失败
- [x] 无范围内项目降级为 deferred/follow-up（config-gated 关闭对照降级须记录理由并归类，非范围内项目静默移除）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### config-gated 关闭对照路径（spare-part-posting-enabled=false）

- Classification: `watch-only residual`
- Why Not Blocking Closure: Playwright `webServer` 为 JVM 全局（`playwright.config.ts:17-23`），单运行内不可按 test 切换 config。主 webServer 已 enabled `erp-mnt.spare-part-posting-enabled=true`（Phase 2 正路径所需），关闭路径经后端单测 `TestErpMntSparePartPosting` 场景 3（config 关闭仅出库不产凭证向后兼容）覆盖。
- Successor Required: `no`

### logistics path-2 自动建单 E2E（generateFreightLandedCost）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 经 shipment DELIVERED 事件驱动自动创建 DRAFT ErpInvLandedCost（plan 2329-1 后端），非浏览器面手动 approve 入口。本计划聚焦手动建单→approve 生命周期。
- Successor Required: `yes`（触发条件：logistics path-2 自动建单浏览器层 E2E 需求落地时）

### 到岸成本 reverse/红冲 E2E

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 若 `ErpInvLandedCostProcessor` 无 reverse 方法（Explore 核实），E2E 无可验证对象。红冲须等后端 successor。
- Successor Required: `yes`（触发条件：到岸成本红冲后端 successor 落地时）

### 到岸成本多段累计管理

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 同一入库单多次追加到岸成本（1100-3 Deferred），属增强面。本计划单次建单审核。
- Successor Required: `yes`（触发条件：多段到岸成本累计浏览器层 E2E 需求落地时）

### 维修工时费用化过账 + 工单成本汇总 E2E

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 维修工时计提（Dr 维修费用 / Cr 应付职工薪酬）需工时归集体系（1100-6 Deferred）；工单级成本多维分摊（1018-3 Deferred）。本计划聚焦备件实物消耗 GL 过账。
- Successor Required: `yes`（触发条件：维修工时成本核算浏览器层 E2E 需求落地时）

### 与 assets 域 MAINTENANCE_EXPENSE(470) 并存防双重扣减 E2E

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 1100-6 后端已处理 linkedVisit 分支（assets 维修工单关联维护工单时贷中转清算防双重）。跨域并存场景复杂度高（maintenance 备件消耗 + assets 维修工单双域 setup）。
- Successor Required: `yes`（触发条件：双域并存防双重扣减浏览器层 E2E 需求落地时）
- **RELEASED by 2026-07-17-2256-2**：触发条件经实时仓库核实已满足（AGENTS.md / project-context.md:34「各域细化端到端验证」重点 + 防双重扣减后端分支已由 1100-6 落地）；交付证据 = `tests/e2e/business-actions/mnt-ast-linked-visit-anti-double-deduct.action.spec.ts`（2 测试：linkedVisit=true 正路径 Cr 2502 中转清算 + linkedVisit=false 对照 Cr 1002 银行存款）+ 种子 `erp_md_subject.csv` 补 2502 维修中转清算行。

## Closure

Status Note: 完成。两 Phase 均全绿。Phase 1 到岸成本审核生命周期 spec（allocate 预览 + approve 三步编排 + LANDED_COST 凭证行 Dr 1401/Cr 2202 精确断言）；Phase 2 备件消耗 confirm + MAINTENANCE_ISSUE 凭证行 Dr 6602/Cr 1403 精确断言。两 spec 互相独立无硬依赖，合并运行 2 passed 15.2s。playwright.config.ts webServer JVM arg 追加 `-Derp-mnt.spare-part-posting-enabled=true`（Phase 2 config 门控启用）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，非执行者）
  - 结构校验：front matter `Plan Status: completed` + `Last Reviewed: 2026-07-14` 齐全；两 Phase 均有 `Status: completed` + `Exit Criteria` 全 `[x]`；Closure Gates 全 `[x]`；Closure 段含真实执行证据（非占位符）。
  - 活仓库核对（grep/glob/read）：`tests/e2e/business-actions/inv-landed-cost.action.spec.ts`（235 行，12.3KB）+ `mnt-spare-part-posting.action.spec.ts`（9.7KB）真实存在且含 Dr/Cr 凭证行数值断言逻辑；`playwright.config.ts:18` 已追加 `-Derp-mnt.spare-part-posting-enabled=true`；`docs/testing/e2e-runbook.md:266-267` 两业务动作行齐全；`docs/logs/2026/07-14.md` 含 full-green 验证条目（2 passed 15.2s）；`docs/backlog/README.md` 标 ✅ done。
  - 反空壳核对：两 spec 含真实 `callQuery`/`callMutation`/`verifyState`/`findVoucherIdByBillCode`/`assertVoucherLines` 调用与 cleanup finally，非空函数体/return null 占位。
  - 五点一致性：Plan Status / 两 Phase Status / Exit Criteria / Closure Gates / Closure evidence 全部 agree（completed）。
  - Deferred honesty：config-gated 关闭对照路径归类 `watch-only residual`（后端单测覆盖）+ 5 项 out-of-scope improvement 均命名 successor 触发条件，无范围内缺陷静默降级。
  - Docs sync：`docs/logs/2026/07-14.md` + `docs/testing/e2e-runbook.md` + `docs/backlog/README.md` 已按 AGENTS.md 更新。
- 执行证据：
  - Phase 1 spec：`tests/e2e/business-actions/inv-landed-cost.action.spec.ts`（1 test, 7.3s green）
  - Phase 2 spec：`tests/e2e/business-actions/mnt-spare-part-posting.action.spec.ts`（1 test, 7.2s green）
  - 合并运行：`npx playwright test inv-landed-cost mnt-spare-part-posting --workers=1` → 2 passed 15.2s
  - Phase 1 Decision 裁定：自包含 __save setup（非 runP2pChain），测试专用物料隔离，billHeadCode=landedCost.code 无后缀
  - Phase 2 Decision 裁定：测试专用备件物料隔离，种子设备 id=1 无污染，billHeadCode=usage.code+"-MI"

Follow-up:

- <仅非阻塞跟进项目；已确认的缺陷不得出现在此处>
