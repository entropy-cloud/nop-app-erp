# 2026-07-14-0941-1-contract-drp-orchestration-e2e 合同返利计提 + 发票计划触发 + DRP 行释放跨域编排浏览器层 E2E

> Plan Status: completed
> Mission: erp
> Work Item: 扩展域跨域编排 E2E（contract + drp）
> Last Reviewed: 2026-07-14
> Source: `docs/plans/2026-07-14-0215-2-contract-drp-direct-action-e2e.md` Deferred But Adjudicated 三项
> Related: `docs/plans/2026-07-14-0215-2-contract-drp-direct-action-e2e.md`（前置计划，状态机 E2E 已落地，本计划承接其 Deferred 跨域编排项）
> Audit: required

## Current Baseline

contract + drp 两域 DIRECT 状态机动作浏览器层 E2E 已由 plan 0215-2 全量落地（contract 6 动作状态机 activate/suspend/resume/terminate/expire/amend + 版本 finalize/sign；drp 净需求引擎 runDrp/resetToDraft/approvePlan + 安全库存 calculate/confirmWriteback），合计 9 测试全绿。零生产代码/契约/ORM/种子/config 变更。

0215-2 的三项 Deferred（跨域编排面）仍未覆盖：

1. **contract 返利计提引擎 E2E（`runAccrual`）**：`ErpCtRebateAgreementBizModel.runAccrual(agreementId, asOfDate)` `@BizMutation`（module-contract/erp-ct-service/.../ErpCtRebateAgreementBizModel.java:72）。引擎读 `posted=true` 的 `ErpPurInvoice`（PURCHASE 返利）/`ErpSalInvoice`（SALES 返利）按期间聚合计提（`accrualMethod` PERIOD_END 期末一次性 / PROGRESSIVE 逐张），产 `ErpCtRebateAccrual` 行。自包含测试需 posted 发票前置。

2. **contract 发票计划触发跨域编排（`triggerInvoice` / `triggerDuePlans`）**：`ErpCtInvoicePlanBizModel.triggerInvoice(planId)` `@BizMutation`（:60）——校验合同 ACTIVE → 按 `contractDirection` INBOUND 建 `ErpPurInvoice` 草稿 / OUTBOUND 建 `ErpSalInvoice` 草稿 → 回写 `isInvoiced/invoiceBillCode/invoiceDate`。`triggerDuePlans(contractId, asOfDate)` `@BizMutation`（:99）——批量扫描到期计划（`planDate <= asOfDate` + `isInvoiced=false`），config-gated `erp-ct.invoiceplan-auto-trigger`（默认 true）。

3. **drp 行释放跨域编排（`releaseLine`）**：`ErpDrpLineBizModel.releaseLine(lineId)` `@BizMutation`（module-drp/erp-drp-service/.../ErpDrpLineBizModel.java:38）——委托 `DrpReleaseService.releaseLine`（:61），按 `replenishmentType`：TRANSFER → 建 `ErpInvTransferOrder` + `ErpInvTransferOrderLine`（需 `preferredSourceWarehouseId`）；PURCHASE → 建 `ErpPurOrder`（需 `preferredSupplierId`）。回写 `orderBillType/orderBillCode/status=ORDERED`，全部行 ORDERED 后计划 APPROVED→EXECUTED。`releaseApproved(planId)` `@BizMutation`（:45）批量释放。

三方法均为 DIRECT `@BizMutation`（不经 useWorkflow/useApproval xwf），浏览器层经 GraphQL `/graphql` 可达。后端逻辑已由 JUnit 集成测试覆盖（`TestErpCtContractRebate` / `TestErpCtContractPosting` / `TestErpDrpScheduleRelease`），本计划叠加浏览器层 E2E 面。

## Goals

- contract `runAccrual` 浏览器层 E2E：自包含 posted 发票前置 → `runAccrual` 触发 → `ErpCtRebateAccrual` 行写入断言 + 非法守卫（非 ACTIVE agreement）。
- contract `triggerInvoice` 浏览器层 E2E：自包含合同 + 发票计划 → `triggerInvoice` → AP/AR 发票草稿创建断言 + `isInvoiced` 回写 + 已开票守卫 + 合同非 ACTIVE 守卫。`triggerDuePlans` 批量入口断言。
- drp `releaseLine` 浏览器层 E2E：自包含 DRP 计划 + APPROVED 行 + `ErpDrpParameter` → `releaseLine` → TRANSFER 路径建调拨单 / PURCHASE 路径建采购订单断言 + `orderBillCode` 回写 + status=ORDERED。
- `docs/testing/e2e-runbook.md` 业务动作表新增 3 行 + 套件计数更新。

## Non-Goals

- contract 返利结算单生成（`ErpCtRebateSettlement`，runAccrual 仅计提 `ErpCtRebateAccrual` 行，结算归后端单测 successor）。
- contract 发票草稿后续审批/过账（`triggerInvoice` 产 DRAFT 草稿不经审批轴，审批/过账属 P2P/O2C 编排已有 spec 覆盖）。
- drp `releaseApproved` 批量释放深度编排（本计划以 `releaseLine` 单行释放作代表验证释放机制 + 跨域建单；批量入口 successor）。
- P2P/O2C 审批链产 posted 发票的 happy-path（已有 orchestration/p2p-chain + o2c-chain spec 覆盖；本计划自包含建 posted 发票避免审批链耦合）。
- 零生产代码/契约/ORM/种子/config 变更（纯测试 + 文档）。

## Task Route

- Type: `仅实现变更`（浏览器层 E2E spec 新增，无后端/模型/契约变更）
- Owner Docs: `docs/design/contract/README.md`（返利/发票计划）、`docs/design/drp/README.md`（行释放编排）、`docs/testing/e2e-runbook.md`（套件登记）
- Skill Selection Basis: `nop-testing`——Playwright 浏览器层 E2E spec 编写遵循已确立的三原语 helper 范式（`callMutationOk`/`verifyState`/`findItems`）。`nop-frontend-dev` 不适用（纯 GraphQL 层无 AMIS 定制）。
- Protected Areas: 无 ORM/ask-first 变更。webServer JVM arg 若需 config flag 变更须记录理由。

## Infrastructure And Config Prereqs

No infra prereqs beyond existing baseline。webServer `playwright.config.ts` 现有 JVM arg 已覆盖 contract/drp 域测试需求。

`triggerDuePlans` config-gated `erp-ct.invoiceplan-auto-trigger` 默认 true，无需额外 webServer JVM arg。若执行期 Explore 发现需显式启用，增 webServer JVM arg 并记录理由。

## Execution Plan

### Phase 1 - contract 返利计提引擎 E2E（runAccrual）

Status: completed
Targets: `tests/e2e/business-actions/ct-rebate-accrual.action.spec.ts`
Skill: `nop-testing`

- Item Types: `Add | Proof | Explore`
- Prereqs: 无（自包含 setup）

- [x] `Explore`: 核实 posted 发票前置最低成本路径——经 GraphQL `__save` 直置 `ErpPurInvoice`（PURCHASE 返利）或 `ErpSalInvoice`（SALES 返利）`posted=true` + `approveStatus=APPROVED` 是否绕过审批轴直达 posted 态（对齐 inv-landed-cost spec 经 `__save` 直置 APPROVED Receive 范式），或须 runP2pChain/runO2cChain 审批链产 posted 发票。裁决写入本阶段记录。
  - Skill: `nop-testing`
- [x] `Add`: 新建 `ct-rebate-accrual.action.spec.ts`——自包含 setup：建 Partner（E2E-REBATE-PN-）+ Contract（ACTIVE, contractDirection=INBOUND or OUTBOUND）+ ContractLine + RebateAgreement（status=ACTIVE, accrualMethod=PROGRESSIVE, rebateType=PURCHASE or SALES, startDate/endDate 覆盖测试日期）+ posted 发票前置（Explore 裁决路径）→ `ErpCtRebateAgreement__runAccrual(agreementId, asOfDate)` → 断言 `ErpCtRebateAccrual` 行存在（`rebateAgreementId` + `sourceBillCode` 匹配发票 code + `accrualAmount` > 0）。
  - Skill: `nop-testing`
- [x] `Add`: 非法守卫测试——`runAccrual` 对非 ACTIVE agreement（DRAFT / TERMINATED）抛 `ERR_CT_REBATE_AGREEMENT_NOT_ACTIVE`。
  - Skill: `nop-testing`
- [x] `Proof`: `npx playwright test tests/e2e/business-actions/ct-rebate-accrual.action.spec.ts --workers=1` 全绿。
  - Skill: `nop-testing`

**Explore 裁决记录**：经实测核实，`ErpPurInvoice`（use-approval tagSet）经 GraphQL `__save` 时平台强制 `posted=false`（即使 body 显式传 `posted=true`，INSERT 仍写 `false`——`posted` 仅可经完整审批-过账管道置 true，非 `__save` 直达）。故 `__save` 直置 posted 发票路径**不可行**；`runP2pChain` 路径会硬编码 `SEED.MAT_1` 并为 MAT_1 在 WH-MAIN 新增余额行 + 写 GL voucher，污染 inventory dashboard `totalValue` / finance 看板基线（镜像 inv-landed-cost spec 裁决）。**裁定**：复用种子 posted AP 发票 `PINV-2026-001`（orgId=2, supplierId=3=SUP-001, businessDate=2026-07-05, totalAmountWithTax=960.5, posted=true）作只读前置——`runAccrual` 只读发票聚合计提，不修改发票；本 spec 创建独立 agreement（partnerId=3）隔离 accrual 行（每用例自建新 agreement，`loadAccruedBillCodes` 按 agreementId 过滤，无跨用例串扰）。**不删种子发票**（共享只读 fixture，p2p-chain/p2p-reverse 等多 spec 依赖）。

Exit Criteria:

- [x] `runAccrual` 经 GraphQL 浏览器层可达，`ErpCtRebateAccrual` 行写入可观测 + 非法守卫 reject 携带 ErrorCode message token
- [x] posted 发票前置 Explore 裁决已记录（__save 直置 vs 审批链），裁决路径在 spec 中可复现

### Phase 2 - contract 发票计划触发跨域编排 E2E（triggerInvoice / triggerDuePlans）

Status: completed
Targets: `tests/e2e/business-actions/ct-invoice-plan-trigger.action.spec.ts`
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1（合同 setup 范式已验证）

- [x] `Add`: 新建 `ct-invoice-plan-trigger.action.spec.ts`——自包含 setup：建 Partner + Contract（ACTIVE, contractDirection=INBOUND）+ ContractLine + InvoicePlan（planDate <= today, isInvoiced=false, amount > 0）→ `ErpCtInvoicePlan__triggerInvoice(planId)` → 断言 AP 发票草稿创建（`ErpPurInvoice` code=`CT-INV-{planId}` 存在 + supplierId=contract.partnerId + totalAmount=plan.amount + posted=false）+ plan 回写（isInvoiced=true + invoiceBillCode + invoiceDate 非空）。
  - Skill: `nop-testing`
- [x] `Add`: OUTBOUND 方向对照——Contract（contractDirection=OUTBOUND）→ `triggerInvoice` 断言 AR 发票草稿创建（`ErpSalInvoice` code=`CT-INV-{planId}` + customerId=contract.partnerId）。同一 spec 内第二 test 或条件分支。
  - Skill: `nop-testing`
- [x] `Add`: 非法守卫测试——已开票 plan `triggerInvoice` 抛 `ERR_CT_INVOICE_PLAN_ALREADY_INVOICED`；SUSPENDED 合同 plan 抛 `ERR_CT_CONTRACT_SUSPENDED`。
  - Skill: `nop-testing`
- [x] `Add`: `triggerDuePlans` 批量入口——Contract + 2 InvoicePlan（planDate <= today, isInvoiced=false）→ `triggerDuePlans(contractId, asOfDate)` → 返回值 >= 2 + 两 plan isInvoiced=true 断言。
  - Skill: `nop-testing`
- [x] `Proof`: `npx playwright test tests/e2e/business-actions/ct-invoice-plan-trigger.action.spec.ts --workers=1` 全绿。
  - Skill: `nop-testing`

**执行期后端修复（plan Non-Goal 豁免）**：执行 Phase 2 triggerDuePlans 用例发现 `ErpCtInvoicePlanBizModel.triggerDuePlans:99` 使用 `findList(query, null, context)` 内部查询带 `le("planDate", asOfDate)` 算子，但 `ErpCtInvoicePlan` XMeta 的 `planDate` 字段查询算子白名单为 `[eq, in, dateBetween, dateTimeBetween]`（date 类型默认白名单），`findList` 经 meta 安全层校验报「查询字段只允许以下查询运算符...不支持 le」——方法本身**有未发现 bug**（既有 JUnit `TestErpCtContractRebate` 未覆盖 triggerDuePlans，故未被捕获）。**1 行修复**：改用 `daoProvider().daoFor(ErpCtInvoicePlan.class).findAllByQuery(query)` 直查绕过 meta 算子白名单（对齐同模块 `loadAccruedBillCodes` / `findPeriodInvoices` 经 daoProvider 直查的既有范式——内部批量逻辑不经外部 GraphQL 查询算子约束）。修复后 triggerDuePlans 浏览器层可达，4 用例全绿；module-contract/erp-ct-service 38 个 JUnit 全绿（无回归）。此为执行期发现的后端 bug 修复，**超出 plan 起草期「零生产代码」Non-Goal 预期**，但属 plan Goal「triggerDuePlans 批量入口断言」落地的必要前提（方法不可达则断言无意义），符合 AGENTS.md「测试暴露的 bug 应修复」精神。

Exit Criteria:

- [x] `triggerInvoice` INBOUND/OUTBOUND 双方向经 GraphQL 浏览器层可达，AP/AR 发票草稿创建 + plan 回写可观测
- [x] `triggerDuePlans` 批量入口返回受影响行数 + 逐 plan 回写断言

### Phase 3 - drp 行释放跨域编排 E2E（releaseLine）

Status: completed
Targets: `tests/e2e/business-actions/drp-release-line.action.spec.ts`
Skill: `nop-testing`

- Item Types: `Add | Proof | Explore`
- Prereqs: 无（自包含 setup）

- [x] `Explore`: 核实 `ErpDrpParameter` 必填字段最小集（`preferredSourceWarehouseId` for TRANSFER / `preferredSupplierId` for PURCHASE）+ `ErpDrpLine` APPROVED 态前置（runDrp 产出 COMPUTED → approvePlan → APPROVED 线，或 `__save` 直置 APPROVED 绕过引擎）。裁决写入本阶段记录。
  - Skill: `nop-testing`
- [x] `Add`: 新建 `drp-release-line.action.spec.ts`——TRANSFER 路径：自包含 setup（Material + 2 Warehouse + DrpPlan APPROVED + DrpLine APPROVED replenishmentType=TRANSFER + DrpParameter preferredSourceWarehouseId）→ `ErpDrpLine__releaseLine(lineId)` → 断言 `ErpInvTransferOrder` 创建（code 匹配 + sourceWarehouseId/destWarehouseId + status=DRAFT）+ line 回写（orderBillType=TRANSFER_ORDER + orderBillCode 非空 + status=ORDERED）。
  - Skill: `nop-testing`
- [x] `Add`: PURCHASE 路径——DrpLine replenishmentType=PURCHASE + DrpParameter preferredSupplierId → `releaseLine` → 断言 `ErpPurOrder` 创建（code 匹配 + supplierId=preferredSupplierId + docStatus=DRAFT）+ line 回写（orderBillType=PURCHASE_ORDER + status=ORDERED）。
  - Skill: `nop-testing`
- [x] `Add`: 非法守卫——非 APPROVED 行 `releaseLine` 拒绝（按后端 `requireReleasable` ErrorCode 守卫）；TRANSFER 缺 sourceWarehouseId 抛 `ERR_DRP_NO_SOURCE_WAREHOUSE`；PURCHASE 缺 preferredSupplierId 抛 `ERR_DRP_NO_PREFERRED_SUPPLIER`。
  - Skill: `nop-testing`
- [x] `Proof`: `npx playwright test tests/e2e/business-actions/drp-release-line.action.spec.ts --workers=1` 全绿。
  - Skill: `nop-testing`

**Explore 裁决记录**：DrpLine APPROVED 态前置 = `__save` 直置 APPROVED（非 runDrp→approvePlan 引擎链）。裁决依据：(1) runDrp 产出的 SUGGESTED 行 `replenishmentType` 由 DrpEngine 内部逻辑决定，本 spec 须精确控制 `replenishmentType=TRANSFER/PURCHASE` 分别验证两路径；(2) `__save` 直置 APPROVED + replenishmentType 绕过引擎，确定性可控（对齐 drp-plan-engine / drp-safety-stock __save 直置 DRAFT 范式）。**隔离策略**：建测试专用物料（避免与 drp-plan-engine (mat4,wh1,org2) / drp-safety-stock (mat4,wh2,org2) 参数三元组冲突——`requireParameter` 用 `setLimit(1)` 返回首条匹配，若复用 seed mat4 + 已有参数行会命中他用例参数致非确定性）。

Exit Criteria:

- [x] `releaseLine` TRANSFER/PURCHASE 双路径经 GraphQL 浏览器层可达，下游单据创建 + line 回写可观测
- [x] 非法守卫 reject 携带 ErrorCode message token

## Draft Review Record

- Independent draft review iteration 1: acceptable as-is (ses_0a1b2db03ffeuVew1w1kVhhvS7) — 全部 10 审查维度 PASS。所有后端方法签名/行号/注解/跨域依赖经实时仓库逐一核实正确。3 非阻塞观察（Phase 1 setup 遗漏 RebateTier 实体 → accrualAmount>0 需 tier；字段名 accruedRebate 非 accrualAmount；Explore 类型不在 Rule 7 规范列表但 Rule 9 授权）均为实现细节，不阻塞执行契约。

## Closure Gates

- [x] 范围内行为完成：3 spec（ct-rebate-accrual / ct-invoice-plan-trigger / drp-release-line）经 GraphQL 浏览器层全栈可达 + 跨域建单断言 + 非法守卫
- [x] 相关文档对齐：`docs/testing/e2e-runbook.md` 业务动作表 +3 行 + 套件计数更新（48→51 spec / 308→317 测试）
- [x] 已运行验证：`npx playwright test tests/e2e/business-actions/ct-rebate-accrual.action.spec.ts tests/e2e/business-actions/ct-invoice-plan-trigger.action.spec.ts tests/e2e/business-actions/drp-release-line.action.spec.ts --workers=1` 全绿（9 passed）+ 全套件回归无新增失败（business-actions 全套件 124 passed，0 回归）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### PERIOD_END 期末一次性计提路径

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: PROGRESSIVE 逐张计提路径已作代表验证 `runAccrual` 引擎触发 + `ErpCtRebateAccrual` 行写入。PERIOD_END 聚合路径经后端单测覆盖（`TestErpCtContractRebate`）。
- Successor Required: `no`

### 返利结算单生成 E2E

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: `runAccrual` 仅计提 `ErpCtRebateAccrual` 行，结算单（`ErpCtRebateSettlement`）为独立后端入口。本计划聚焦计提触发面。
- Successor Required: `yes`（触发条件：返利结算浏览器层 E2E 需求落地时）

### drp releaseApproved 批量释放深度编排 E2E

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: `releaseLine` 单行释放已验证释放机制 + 跨域建单。批量入口 + 计划 APPROVED→EXECUTED 全量行联动经后端单测覆盖（`TestErpDrpScheduleRelease`）。
- Successor Required: `yes`（触发条件：DRP 批量释放→全行 ORDERED→计划 EXECUTED 浏览器层 E2E 需求落地时）

## Closure

Status Note: 全部 3 Phase 完成。3 新 spec（9 用例）全绿——contract 返利计提引擎 runAccrual（Phase 1，复用种子 posted AP 发票作只读前置经 Explore 裁决__save 直置 posted=true 不可行）+ contract 发票计划触发跨域编排 triggerInvoice/triggerDuePlans（Phase 2，INBOUND→AP/OUTBOUND→AR 双方向 + 批量入口 + 已开票/SUSPENDED 守卫；执行期发现并修复 triggerDuePlans 后端 bug：findList 经 meta 算子白名单拒 le(planDate) 改用 daoProvider 直查，对齐同模块范式，module-contract/erp-ct-service 38 JUnit 全绿无回归）+ drp 行释放跨域编排 releaseLine（Phase 3，TRANSFER→ErpInvTransferOrder/PURCHASE→ErpPurOrder 双路径 + 3 守卫）。验证基线：3 spec 9 passed + business-actions 全套件 124 passed（115 既有 + 9 new）0 回归 + mvn clean install -DskipTests 全绿 + module-contract/erp-ct-service mvn test 38 全绿。e2e-runbook.md 业务动作表 +3 行 + 套件计数 48→51 spec / 308→317 测试更新。docs/backlog/README.md +1 行（0941-1 ✅ done）。范围外 successor（PERIOD_END 计提/返利结算单 E2E/drp releaseApproved 批量深度）已在 Deferred But Adjudicated 段记录。执行期 1 项后端 bug 修复（triggerDuePlans）超出 plan 起草期「零生产代码」Non-Goal 预期，但属 plan Goal 落地的必要前提，已记录在 Phase 2 段 + backlog README 行 + 拟记录于 docs/logs/2026/07-14.md。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理 `ses_0a16dfacfffeCHzX0Pw5G35DHq`（新会话，非执行者）。**Verdict: PASS**——10 checkpoint 全确认：(1) Plan Status/Phase status/checkbox 一致；(2) Closure Gates 全 [x]；(3) 3 spec 文件存在且测试结构与 Phase item 对齐（2+4+3=9 测试）；(4) 后端 1 行修复（triggerDuePlans findList→daoProvider.findAllByQuery）已落地 + 经文档记录；(5) e2e-runbook +3 行 + 套件计数 48→51 spec / 308→317 测试；(6) backlog/README +1 行 ✅ done；(7) daily log 顶部新条目；(8) 3 Deferred 项仍 deferred（无静默实现）；(9) Status Note 已填写；(10) 范围纪律清洁（仅 4 文件变更：3 docs + 1 Java 后端 bug 修复，无 ORM/契约/种子/config 变更）。

Follow-up:

- <仅非阻塞跟进项目；已确认的缺陷不得出现在此处>
