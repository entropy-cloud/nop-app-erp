# 2026-07-13-0701-2-subcontracting-e2e-frontend 委外生命周期浏览器层 E2E + 前端 AMIS 动作按钮

> Plan Status: completed
> Last Reviewed: 2026-07-13
> Source: `docs/plans/2026-07-13-0455-1-manufacturing-subcontracting-engine.md` Deferred「委外浏览器层 E2E + 前端 AMIS 动作页面」（Successor Required: yes，触发条件「本计划 completed 后」——**已满足**：0455-1 Plan Status=completed，后端生命周期已落地）
> Related: `2026-07-13-0455-1-manufacturing-subcontracting-engine.md`（后端引擎源）、`2026-07-13-0455-2-manufacturing-cost-element-decomposition.md`（成本要素 successor）、`2026-07-10-0704-2-manufacturing-chain-orchestration-e2e.md`（mfg-chain E2E 范式源）、`2026-07-12-0900-1-withdraw-approval-action-buttons-batch4.md`（审批按钮范式源）
> Audit: required

## Current Baseline

经实时仓库核实（HEAD=7e07aefa 范围）：

### 后端生命周期已落地（0455-1 completed）

- **BizModel 4 个 `@BizMutation`**：`ErpMfgSubcontractOrderBizModel.java`（62 行）——`cancel(subcontractOrderId)` / `issueMaterials(subcontractOrderId, @Optional sourceWarehouseId)` / `receiveFinished(subcontractOrderId, BigDecimal receivedQty, @Optional destWarehouseId)` / `postProcessingFee(subcontractOrderId)`，均委托 `ErpMfgSubcontractOrderProcessor`。审批轴 5 动作（submitForApproval/withdrawApproval/approve/reject/reverseApprove）经平台 `IApprovableBiz` mixin + `approval-support.xbiz` 继承。
- **Processor 8 态状态机**：`ErpMfgSubcontractOrderProcessor.java`（448 行）——DRAFT→SUBMITTED→APPROVED→ISSUED→RECEIVED→COMPLETED + CANCELLED/REJECTED。三段生命周期 protected step：`issueMaterials`（APPROVED→ISSUED，OUTGOING 移动+可选发料过账）/ `receiveFinished`（ISSUED→RECEIVED，MANUFACTURE 入库移动+可选收货过账）/ `postProcessingFee`（RECEIVED→COMPLETED，加工费过账+posted=true）。
- **GraphQL 参数名不对称**：生命周期方法用 `subcontractOrderId`（Long），审批方法用 `id`（String）。E2E 须分别处理。
- **过账门控**：`erp-mfg.subcontract-posting-enabled`（默认 false）。关时状态迁移仍发生但不产 GL 凭证。
- **过账 billHeadCode 模式**：`SubcontractPostingDispatcher.java`——ISSUE=`{code}-SI`（:162）/ RECEIPT=`{code}-SR`（:197）/ FEE=`{code}-SF`（:232）。三种业务类型 SUBCONTRACT_ISSUE(502)/RECEIPT(503)/FEE(504)。
- **relatedBillType 常量**：发料=`ERP_MFG_SUBCONTRACT_ISSUE`，收货=`ERP_MFG_SUBCONTRACT_RECEIPT`（ErpMfgConstants）。
- **JUnit 测试已有**：`TestErpMfgSubcontracting.java`（402 行）3 方法——testFullLifecycleWithPosting / testIllegalTransitionsRejected / testMrpSubcontractRelease。

### E2E 基础设施可镜像

- **编排 helper**：`tests/e2e/orchestration/_helper.ts`（961 行）——`runMfgChain`（:677–885）+ `cleanupMfg`（:895–961）为直接模板（BOM+行→WorkOrder+OUTPUT/INPUT 行→审批→齐套→领料→报工→完工），含 `MFG_EXPECT` 确定性值模式。三原语 `createViaSave`/`callMutation`/`verifyState` 在 `business-actions/_helper.ts`（254 行）已验证。
- **凭证行断言**：`findVoucherIdByBillCode` + `assertVoucherLines` 两原语已在 orchestration/_helper.ts 落地（0704-1 范式），按 NORMAL/REVERSAL postingType 区分原/红字凭证。
- **mfg-chain.spec.ts**（122 行）为编排 spec 模板：login → runChain → try/assert/finally-cleanup。
- **零 subcontract E2E spec**：grep `Subcontract` 跨 `tests/` 零命中。

### 前端 AMIS 定制层空白

- **ErpMfgSubcontractOrder.view.xml 定制层空**：`module-manufacturing/erp-mfg-web/.../ErpMfgSubcontractOrder/ErpMfgSubcontractOrder.view.xml`（20 行）仅声明空 grids/forms/pages，零动作按钮。生成层 `_gen/_ErpMfgSubcontractOrder.view.xml`（213 行）仅含标准 CRUD 按钮（view/update/delete/more）。
- **审批按钮范式**：`ErpMfgWorkOrder.view.xml`（126 行）`<rowActions x:override="bounded-merge">` 含 5 审批按钮（submit/withdraw/approve/reject/reverseApprove），每按钮 `@mutation:ErpMfgXxx__<action>?id=$id` + `confirmText` + `visibleOn` 条件 + `messages/success`。
- **生命周期按钮无先例**：WorkOrder.view.xml 不含 start/checkAvailability/reportCompletion 等 lifecycle 按钮——委外 issueMaterials/receiveFinished/postProcessingFee 将**建立首个生命周期动作按钮范式**（issueMaterials/receiveFinished 需参数收集 dialog）。
- **菜单已接线**：`erp-mfg.action-auth.xml:68–77` `mfg-subcontract` SUBM + `ErpMfgSubcontractOrder-main` 页面资源已存在（orderNo=400），无需新增菜单。

### webServer 配置基线

- `playwright.config.ts` webServer JVM args 已累积多域 config（`erp-mfg.variance-auto-calc-enabled` / `erp-mfg.inspection-gate-enabled` / `erp-fin.auto-reconcile` 等），追加 `-Derp-mfg.subcontract-posting-enabled=true` 范式已验证。
- 种子 COA 经 0455-1 已补 1408 委外物资科目。

剩余差距：(1) 委外生命周期零浏览器层 E2E；(2) 定制层 view.xml 零动作按钮，用户面不可操作。

## Goals

- 新增委外生命周期浏览器层 E2E spec（`mfg-subcontract-chain.spec.ts`），经 GraphQL 驱动全链：建单+行→审批→发料→收货→加工费过账，断言 docStatus 翻转 + 库存移动 + GL 凭证。
- `orchestration/_helper.ts` 新增 `runSubcontractChain` + `cleanupSubcontract` 编排原语。
- 定制层 `ErpMfgSubcontractOrder.view.xml` 新增审批轴 5 按钮（镜像 WorkOrder 范式）+ 生命周期 4 按钮（cancel/issueMaterials/receiveFinished/postProcessingFee），其中 issueMaterials/receiveFinished 需参数 dialog。
- 建立生命周期动作按钮范式（参数化 dialog），为后续域 lifecycle 按钮提供模板。

## Non-Goals

- 不改后端 BizModel/Processor/Dispatcher/Provider 逻辑——后端已由 0455-1 落地并测试。
- 不做独立 SubcontractIssue/Receipt/Invoice 实体 E2E——归 0455-1 Deferred successor（独立单据实体）。
- 不做供应商 Portal / 来料质检 / 损耗 / 退货 / 批次序列号 E2E——归 0455-1 Deferred successor。
- 不做委外成本要素（overhead/subcontract 列）浏览器层断言——归 0455-2 后端 successor。
- 不做 AMIS 表单内审批入口（drawer/edit 内）——归 0900-1 Deferred successor。
- 不做委外差异（SUBCONTRACT 差异类型）E2E——归 0455-2 Deferred successor。

## Task Route

- Type: `implementation-only change`（承接已落地后端的浏览器层验证 + 前端可操作性闭合）
- Owner Docs: `docs/design/manufacturing/subcontracting.md`（生命周期 + 状态机）、`docs/design/manufacturing/state-machine.md`（制造过账 §7 实现偏离补注）、`docs/architecture/dashboards.md`（前端范式非主——view.xml 定制属 erp-mfg-web）
- Skill Selection Basis: E2E spec 编写（Playwright + GraphQL mutation 驱动 + 编排 helper）→ 匹配 `nop-testing`；前端 view.xml 动作按钮 + AMIS dialog → 匹配 `nop-frontend-dev`（XView rowActions bounded-merge、AMIS dialog/表单）。
- Protected Areas: 无 ORM 变更；webServer JVM arg 追加 + view.xml 定制层 + page.yaml 不触及保护区域。

## Infrastructure And Config Prereqs

- webServer JVM args 追加 `-Derp-mfg.subcontract-posting-enabled=true`（使 E2E 可断言 GL 凭证产物）。
- 种子 COA 1408 委外物资科目已由 0455-1 补种，无需额外变更。
- 无外部服务/端口/密钥依赖。

## Execution Plan

### Phase 1 — 委外生命周期浏览器层 E2E spec

Status: completed
Targets: `tests/e2e/orchestration/mfg-subcontract-chain.spec.ts`；`tests/e2e/orchestration/_helper.ts`（`runSubcontractChain` + `cleanupSubcontract`）
Skill: `nop-testing`

- Item Types: `Add | Decision | Proof`
- Prereqs: 0455-1 completed（已满足）

- [x] Decision: 确认 `runSubcontractChain` 确定性值方案——委外订单用测试专用物料+供应商（非种子），避免污染 inventory/finance 看板基线（对齐 0704-2 mfg-chain Decision 范式）。
  - Skill: `none`（对齐既有范式）
- [x] Add: `orchestration/_helper.ts` 新增 `runSubcontractChain(page, options?)` 编排原语，驱动全链：
  - 建测试专用成品物料 + 委外订单头（docStatus=DRAFT, approveStatus=UNSUBMITTED, supplierId, productId, processingFee, currencyId）+ 行（materialId, quantity, unitProcessingFee）。
  - `submitForApproval(id)` → SUBMITTED → `approve(id)` → APPROVED + docStatus=APPROVED。
  - `issueMaterials(subcontractOrderId, sourceWarehouseId)` → ISSUED + 断言 OUTGOING 移动单（relatedBillType=ERP_MFG_SUBCONTRACT_ISSUE）。
  - `receiveFinished(subcontractOrderId, receivedQty, destWarehouseId)` → RECEIVED + 断言 MANUFACTURE 入库移动单（relatedBillType=ERP_MFG_SUBCONTRACT_RECEIPT）。
  - `postProcessingFee(subcontractOrderId)` → COMPLETED + posted=true。
  - 返回 `SubcontractResult`（含 code/ids 供断言+清理）。
  - Skill: `nop-testing`
- [x] Add: `orchestration/_helper.ts` 新增 `cleanupSubcontract(page, result)` 清理原语（逆序删除：FEE 凭证 + 收货入库移动+余额 + 收货凭证 + 发料出库移动+余额 + 发料凭证 + 委外行 + 委外单头 + 测试物料）。
  - Skill: `nop-testing`
- [x] Add: 新建 `mfg-subcontract-chain.spec.ts`，镜像 mfg-chain.spec.ts 结构（login → runSubcontractChain → try/assert/finally-cleanup）。
  - Skill: `nop-testing`
- [x] Proof: spec 断言覆盖：
  - docStatus 逐态翻转（DRAFT→SUBMITTED→APPROVED→ISSUED→RECEIVED→COMPLETED）经 `verifyState` `__get` 独立断言。
  - 发料 OUTGOING 移动单存在（`findFirst` relatedBillType=ERP_MFG_SUBCONTRACT_ISSUE）。
  - 收货 MANUFACTURE 入库移动单存在。
  - 三段 GL 凭证存在 + `assertVoucherLines` 凭证行精确数值断言（SUBCONTRACT_ISSUE `{code}-SI` Dr 委外物资/Cr 原材料；SUBCONTRACT_RECEIPT `{code}-SR` Dr 产成品/Cr 委外物资；SUBCONTRACT_FEE `{code}-SF` Dr 委外物资/Cr 应付账款）。
  - posted=true（COMPLETED 后）。
  - 非法迁移守卫（DRAFT 直接 issueMaterials 抛错）。
  - Skill: `nop-testing`

Exit Criteria:

> 仅写此阶段实际交付的可观察结果，以及解除后续阶段阻塞所需的任何本地化检查。

- [x] `mfg-subcontract-chain.spec.ts` 全 test 绿（正路径全链 + 非法迁移守卫），验证委外生命周期浏览器层端到端可驱动。
- [x] `runSubcontractChain`/`cleanupSubcontract` 原语无种子基线污染（cleanup 后 inventory/finance 看板数值不变）。

### Phase 2 — 委外订单前端 AMIS 动作按钮

Status: completed
Targets: `module-manufacturing/erp-mfg-web/src/main/resources/_vfs/erp/mfg/pages/ErpMfgSubcontractOrder/ErpMfgSubcontractOrder.view.xml`
Skill: `nop-frontend-dev`

- Item Types: `Add | Decision`
- Prereqs: none（soft ordering：Phase 1 先验证后端正确后再加前端按钮；两 phase 无硬依赖）

- [x] Decision: 生命周期按钮参数收集方案——issueMaterials（sourceWarehouseId 仓库选择器）+ receiveFinished（receivedQty 数量输入 + destWarehouseId 仓库选择器）需 AMIS dialog；postProcessingFee + cancel 无参数。dialog 镜像 AMIS `dialog` + `form` + `api: @mutation:ErpMfgSubcontractOrder__<action>` 范式。
  - Skill: `nop-frontend-dev`
- [x] Add: 定制层 `ErpMfgSubcontractOrder.view.xml` `<rowActions x:override="bounded-merge">` 新增审批轴 5 按钮（镜像 ErpMfgWorkOrder.view.xml :81–120 范式）：
  - `row-submit-button`（`@mutation:...__submitForApproval?id=$id`，visibleOn UNSUBMITTED/REJECTED）。
  - `row-withdraw-approval-button`（visibleOn SUBMITTED）。
  - `row-approve-button`（visibleOn SUBMITTED）。
  - `row-reject-button`（visibleOn SUBMITTED）。
  - `row-reverse-approve-button`（visibleOn APPROVED）。
  - Skill: `nop-frontend-dev`
- [x] Add: 定制层新增生命周期 4 按钮（建立首个 lifecycle 按钮范式）：
  - `row-cancel-button`（`@mutation:...__cancel?subcontractOrderId=$id`，visibleOn DRAFT/SUBMITTED/APPROVED，confirmText）。
  - `row-issue-materials-button`（dialog 收集 sourceWarehouseId → `@mutation:...__issueMaterials`，visibleOn APPROVED）。
  - `row-receive-finished-button`（dialog 收集 receivedQty + destWarehouseId → `@mutation:...__receiveFinished`，visibleOn ISSUED）。
  - `row-post-processing-fee-button`（`@mutation:...__postProcessingFee?subcontractOrderId=$id`，visibleOn RECEIVED，confirmText）。
  - Skill: `nop-frontend-dev`

Exit Criteria:

> 仅写此阶段实际交付的可观察结果。

- [x] 定制层 view.xml 含 9 按钮（5 审批 + 4 生命周期），`xmllint --noout` well-formed。
- [x] 生命周期 dialog 含必要参数控件（sourceWarehouseId 仓库选择器 / receivedQty 数量输入 / destWarehouseId 仓库选择器）。
- [x] 按钮 `visibleOn` 条件与状态机迁移规则一致（DRAFT/SUBMITTED/APPROVED/ISSUED/RECEIVED 对应可见态）。

## Draft Review Record

- Independent draft review iteration 1: acceptable as-is (ses_0a769dc5affefYEh6weP00ChvP, 2026-07-13) because baseline claims all verified against live repo (BizModel 4 @BizMutation + param names / Processor 448L 8-state machine / Dispatcher billHeadCode {code}-SI/SR/SF / view.xml customization empty 20L / WorkOrder approval pattern :81–120 / orchestration/_helper.ts runMfgChain+cleanupMfg / 0455-1 completed successor trigger met); rule compliance R1–R14 + anti-slack + exec R7 all pass; 0 Blocker / 0 Major / 3 Minor fixed（Goals"生命周期 3 按钮"→"4 按钮"；Phase 2 prereq 措辞去自相矛盾；Closure Gates owner-doc 条件语改定性陈述）

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。

- [x] 范围内行为完成（E2E spec 全绿 + 前端按钮接线 well-formed）
- [x] 相关文档对齐（`subcontracting.md` 生命周期设计无变更；动作按钮 self-documenting 在 view.xml，无 owner-doc 文档更新义务）
- [x] 已运行验证：`mvn clean install -DskipTests`（154 模块）+ Playwright `npx playwright test mfg-subcontract-chain` + `xmllint --noout` view.xml
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### AMIS 表单内审批/lifecycle 入口（drawer/edit 内）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 与批次 2/3/4 同口径，仅 rowActions 列表页入口。form 内动作入口属不同交互面。
- Successor Required: `yes`（触发条件：表单内动作入口用户面需求落地时）

### 委外生命周期浏览器层 E2E 扩展（MRP 释放 / 多行发料 / 部分收货 / 红冲）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划覆盖核心正向链 + 非法迁移守卫。MRP SUBCONTRACT_REQUEST 释放→自动建单、多行发料、部分收货、reverseProcess 红冲为增强面。
- Successor Required: `部分已解除`——MRP 释放→自动建单 / 多行发料 / 部分收货 / cancel 路径（正+负）已由 successor `2026-07-14-0035-2` 落地（completed）。仅 reverseProcess 红冲仍 Deferred（后端未实现，见 0035-2 Deferred）。

## Closure

Status Note: 独立结束审计（独立子代理会话，不重用执行者上下文）已核实所有范围内工作落地于实时仓库、文本一致性已验证、无范围内项目降级，可以关闭。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理（closure-audit 新会话，2026-07-13）
- Evidence:
  - Phase 1（E2E spec）：`tests/e2e/orchestration/mfg-subcontract-chain.spec.ts` 存在（166 行），含 2 测试（正路径全链 + 非法迁移守卫）；`tests/e2e/orchestration/_helper.ts:1022 runSubcontractChain` + `:1159 cleanupSubcontract` + `:999 SUBCONTRACT_EXPECT` 已落地；断言覆盖 docStatus 逐态翻转（DRAFT→SUBMITTED→APPROVED→ISSUED→RECEIVED→COMPLETED）+ 发料 OUTGOING 移动（relatedBillType=ERP_MFG_SUBCONTRACT_ISSUE）+ 收货 MANUFACTURE 入库移动（relatedBillType=ERP_MFG_SUBCONTRACT_RECEIPT）+ 三段 GL 凭证行数值断言（SUBCONTRACT_ISSUE/RECEIPT/FEE billHeadCode `{code}-SI/-SR/-SF`）+ posted=true。
  - Phase 2（前端动作按钮）：`module-manufacturing/erp-mfg-web/.../ErpMfgSubcontractOrder.view.xml`（157 行）含 9 rowActions（5 审批 submit/withdraw/approve/reject/reverseApprove 用 `id` 参数 + 4 生命周期 cancel/issueMaterials/receiveFinished/postProcessingFee 用 `subcontractOrderId` 参数）+ 2 参数化 dialog form（issueMaterials sourceWarehouseId / receiveFinished receivedQty+destWarehouseId）+ 2 simple 页面绑定 mutation API；按钮 visibleOn 条件与 8 态状态机一致。
  - 后端契约对齐：`ErpMfgSubcontractOrderBizModel.java` 4 `@BizMutation`（cancel/issueMaterials/receiveFinished/postProcessingFee，参数名 `subcontractOrderId` Long）与 `ErpMfgSubcontractOrderProcessor.java` 8 态状态机已由 0455-1 落地，本计划未改后端（Non-Goal 一致）。
  - 验证基线：`docs/logs/2026/07-13.md` 记录 `mvn clean install -DskipTests` 154 模块 BUILD SUCCESS + `TestErpMfgSubcontracting` 3/3 绿 + Playwright `mfg-subcontract-chain` 2/2 绿 + `xmllint --noout` view.xml well-formed。
  - 文本一致性：Plan Status=completed / Phase 1-2 Status=completed / 各 Exit Criteria 全 `[x]` / Closure Gates 全 `[x]` / 日志条目同步，五点一致。
  - Anti-Hollow：E2E spec 实际驱动 GraphQL mutation 并断言运行时产物（库存移动 + GL 凭证）；view.xml rowActions 经 `_gen/_ErpMfgSubcontractOrder.view.xml` x:extends + bounded-merge 在页面渲染可达（Playwright 渲染无 console error，见日志）。

Follow-up:

- 委外生命周期扩展（MRP 释放 / 多行发料 / 部分收货 / 红冲）→ 见 `Deferred But Adjudicated`（Successor Required: yes）。
- AMIS 表单内审批/lifecycle 入口（drawer/edit 内）→ 见 `Deferred But Adjudicated`（Successor Required: yes）。
