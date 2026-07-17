# 2026-07-17-1005-2-contract-drp-projects-settlement-orchestration-e2e contract 返利结算 + drp 批量释放 + projects 损益结转编排浏览器层 E2E

> Plan Status: completed
> Last Reviewed: 2026-07-17
> Mission: erp
> Work Item: 各域细化端到端验证（结算/释放编排浏览器层 E2E 深化 + 凭证行数值断言）
> Source: 三项 Successor Required: yes 的 Deferred 承接：
>   - `docs/plans/2026-07-14-0941-1-contract-drp-orchestration-e2e.md` Deferred「返利结算单生成 E2E」（触发条件「返利结算浏览器层 E2E 需求落地时」）
>   - `docs/plans/2026-07-14-0941-1-contract-drp-orchestration-e2e.md` Deferred「drp releaseApproved 批量释放深度编排 E2E」（触发条件「DRP 批量释放→全行 ORDERED→计划 EXECUTED 浏览器层 E2E 需求落地时」）
>   - `docs/plans/2026-07-14-0742-2-projects-posting-lifecycle-voucher-line-e2e.md` Deferred「结算 FINAL/INTERIM 损益结转凭证行断言」（触发条件「项目损益结转浏览器层 E2E 需求落地时」）
>
> 三项触发条件均以「按域推进编排/结算链浏览器层覆盖」为口径，AGENTS.md「当前项目阶段」明示当前重点含「各域细化端到端验证」，与本仓近 10 份编排/凭证行 E2E 计划（0941-1/0742-1/0742-2/0704-1/1218-2 等）一致裁定**触发条件已满足**。
> Related: `2026-07-14-0941-1`（contract 返利计提 + drp 单行释放本体，已 completed；本计划承接其 2 项结算/批量 successor）、`2026-07-14-0742-2`（projects CLOSE 转固结算本体，已 completed；本计划承接其 FINAL/INTERIM 损益结转 successor）、`docs/testing/e2e-runbook.md`、`docs/lessons/05-nop-e2e-failure-log-first-diagnosis.md`
> Audit: required

## Current Baseline

经实时仓库核实（HEAD 2026-07-17）：

### 后端编排动作已落地（DIRECT `@BizMutation`，浏览器层 `/graphql` 可达）

- **contract 返利结算单**：`module-contract/erp-ct-service/.../entity/ErpCtRebateSettlementBizModel.java:64-65 postSettlement(@Name("settlementId"), context)` 为 DIRECT `@BizMutation`。**经主代理复核源码 + 后端测试，行为已确定性核实（非 Phase 1 开放问题）**：`postSettlement` **不过 GL 凭证**，而是跨域创建**负额 credit memo 发票**——PURCHASE agreement→`createNegativeApInvoice`（`ErpPurInvoice`+`ErpPurInvoiceLine`，`:125-153`）/ SALES agreement→`createNegativeArInvoice`（`ErpSalInvoice`+`ErpSalInvoiceLine`，`:158-186`）；`settlement.setCreditMemoBillCode("CT-REBATE-"+settlementId)`（`:99`）回链；标记计提 `ErpCtRebateAccrual.isSettled=true`（`:203` `eq("isSettled", false)` 批量翻转）；结算单 DRAFT→POSTED。由 `TestErpCtContractRebate.java:260-283` 断言 `creditMemo.getTotalAmount().signum() < 0` 且无 GL 凭证产物佐证。0941-1 已交付 `runAccrual` PROGRESSIVE 逐张计提 → `ErpCtRebateAccrual` 行；结算单 `postSettlement` 为 0941-1 显式 Deferred「结算单为独立后端入口，本计划聚焦计提触发面」。
- **drp 批量释放**：`module-drp/erp-drp-service/.../entity/ErpDrpLineBizModel.java:41 releaseApproved(@Name("planId"), context)` 为 DIRECT `@BizMutation`，返回 `ErpDrpPlan`（GraphQL 标量无选择集，0941-1 裁决先例）→ `DrpReleaseService.releaseApproved(planId)`（:99）批量释放 APPROVED 计划全部行 → 下游单据 + 行 ORDERED，`advancePlanToExecutedIfComplete`（:115）在全部行 ORDERED/CANCELLED 时计划 APPROVED→EXECUTED。0941-1 已交付 `releaseLine` 单行释放；批量入口 + 计划 EXECUTED 全量行联动经后端单测 `TestErpDrpScheduleRelease` 覆盖，浏览器层零覆盖。
- **projects FINAL/INTERIM 损益结转**：`module-projects/.../ErpPrjProjectSettlementBizModel.java:49 approve` 为 DIRECT `@BizMutation`，末尾按 `settlementType` 分派。**经主代理复核 `ProjectSettlementAcctDocProvider.java:69-87`，凭证结构已确定性核实**：FINAL/INTERIM 分支 `profitLoss = finalRevenue - finalCost` → `Dr 5101(项目成本)=finalCost` + `Cr 6001(项目收入)=finalRevenue` + **条件性** `4103(本年利润)`：仅当 `profitLoss.signum() != 0`（即 `finalRevenue ≠ finalCost`）才发第四行，方向由符号决定（盈利 Dr 4103=|profitLoss| / 亏损 Cr 4103=|profitLoss|）。CLOSE 走另一分支（Dr 1601/Cr 1603 转固，0742-2 已交付）。

### 浏览器层覆盖缺口（本计划对象）

三项均为「本体计划已交付代表路径，结算/批量/深化 successor 显式 Deferred」：
- contract 返利结算单 `postSettlement`（DRAFT→POSTED + 负额 credit memo 发票跨域创建 + 计提 isSettled 翻转；**无 GL 凭证产物**，故本 spec 不断言凭证行——见 Non-Goals）零浏览器层覆盖。
- drp releaseApproved 批量释放（APPROVED plan → 全行 ORDERED → plan EXECUTED）零浏览器层覆盖。
- projects FINAL/INTERIM 损益结转凭证行精确数值断言（Dr 5101/Cr 6001 + 条件性 4103）零浏览器层覆盖。

### 既有验证范式（本计划复用）

- `tests/e2e/business-actions/_helper.ts`：`createViaSave`/`callMutation`/`verifyState`/`findFirst`（0508-1/0941-1/0742-2 范式）。
- **凭证行数值断言两原语位于姊妹文件 `tests/e2e/orchestration/_helper.ts`**：`findVoucherIdByBillCode(billCode, postingType)` + `assertVoucherLines(voucherId, expectedLines)`；business-actions spec 经 `from '../orchestration/_helper'` 跨目录导入（先例 `projects-settlement-posting.action.spec.ts:13`），非 business-actions 自身。仅 projects spec 需此导入（contract 无 GL 凭证、drp 不过账）。
- 0941-1 contract 跨域范式（复用种子 posted AP 发票作只读前置，__save 直置 posted=true 不可行 use-approval tagSet 强制 false）；0742-2 projects 结算 setup 范式（PnL 快照 refreshPnl 前置 + 自包含 CostCollection/ProjectType defaultSubjectId + orgId 必填驱动 acctSchema）。

### 剩余差距

contract 返利结算 + drp 批量释放 + projects 损益结转为 0941-1/0742-2 三项显式 Deferred，后端齐备但浏览器层零深度覆盖；属当前重点「各域细化端到端验证」的明确 successor 面，且 projects FINAL/INTERIM 可叠加凭证行精确数值断言（项目结算最后一个凭证行缺口）。

## Goals

- 为三项 Deferred 各交付浏览器层 E2E spec，经 GraphQL `/graphql` 驱动 `@BizMutation`，状态/字段翻转经 `verifyState`（`__get`）/`findFirst` 独立断言：
  1. contract 返利结算单 `postSettlement(settlementId)`——DRAFT→POSTED + 跨域负额 credit memo 发票创建（PURCHASE→`ErpPurInvoice` / SALES→`ErpSalInvoice`）+ `settlement.creditMemoBillCode="CT-REBATE-{id}"` + 计提 `ErpCtRebateAccrual.isSettled=true`（**无 GL 凭证，不断言凭证行**——见 Non-Goals）+ 非法态守卫。
  2. drp `releaseApproved(planId)` 批量释放（APPROVED plan → 全行 ORDERED + 计划状态 EXECUTED 翻转 + 下游单据 ErpInvTransferOrder/ErpPurOrder 创建断言 + 非 APPROVED 守卫）。
  3. projects FINAL/INTERIM 损益结转 `approve` → posted + 凭证行精确数值断言：`Dr 5101=finalCost` + `Cr 6001=finalRevenue` + **条件性** `4103`（setup 须工程化 `finalRevenue ≠ finalCost` 使 4103 行确定性出现，方向由符号决定）+ reverse 红冲同向取负 + INTERIM 对照。
- 在 `docs/testing/e2e-runbook.md` 业务动作表 +3 行 + 凭证行断言表 +projects FINAL/INTERIM 行 + 套件计数**对齐实际**（runbook 预存漂移：声称「17 域 55 spec」，实际 58 `.action.spec.ts`；Phase 3 须校正基线而非简单 +3）；`docs/backlog/README.md` +1 done 行。
- 解除 0941-1 两项 + 0742-2 一项 Deferred（各补 `**RELEASED by 2026-07-17-1005-2**` 行）。

## Non-Goals

- **不重新实现 0941-1/0742-2 的本体范围**——本计划仅消费侧结算/批量/深化 successor + 测试层，零生产代码/契约/ORM/种子变更预期。
- **不新增编排面后端**——三后端均已落地；若执行期发现某 `@BizMutation` 不可达或有 bug（如 0941-1 triggerDuePlans meta 算子白名单类 bug），属执行期豁免须 Phase 内记录 + 模块 JUnit 回归，但不开新 successor 除非确证缺陷。
- **不为 contract 返利结算断言 GL 凭证行**——`postSettlement` 经源码核实**按设计不过 GL 凭证**，改走跨域负额 credit memo 发票（AP/AR 侧），故无凭证行可断言；这不是缺口而是既定行为。contract spec 断言 credit memo 发票 + 计提翻转，不触凭证行。
- **不覆盖 contract PERIOD_END 期末一次性计提路径**——0941-1 裁定 PROGRESSIVE 已代表验证 + 后端单测覆盖 PERIOD_END，Successor Required: no。
- **不覆盖 projects 工时 actualCost 归集精确数值断言**——0742-2 裁定为 optimization candidate（域表断言，非 GL 凭证），Successor Required: no。
- **不触及 xwf 审批轴**——经 2330-1 权威裁决浏览器层不可行，触发条件未满足。

## Task Route

- Type: `verification or audit work`（既有 Playwright E2E 套件的结算/批量/深化 successor；纯消费侧 + 测试维护，零生产契约变更预期）
- Owner Docs: `docs/testing/e2e-runbook.md`、`docs/design/contract/volume-discount.md`（返利结算）、`docs/design/drp/README.md`（DRP 释放）、`docs/design/projects/profitability.md`（项目损益结转）
- Skill Selection Basis: 纯 Playwright 浏览器层测试维护，非 Nop 平台 BizModel/页面开发；`nop-testing` 路由目标 `e2e-testing.md` 不存在故 E2E 覆盖为空（2246-1 裁决先例），依技能实质内容判定 `Skill: none`（nop-testing）。Phase 1 Explore 阶段如发现后端不可达需根因诊断，重新加载 `nop-debugging`。
- Protected Areas: E2E spec 在根 `tests/e2e/` 非 reactor 模块；不改 ORM/契约；任何生产代码修复须 ask-first 并开 successor。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。复用既有 Playwright 基础设施（`playwright.config.ts` webServer fresh-DB + 种子 + auth fixtures）。
- projects FINAL/INTERIM 损益结转科目经种子 `erp_md_subject.csv` 已齐备（经主代理复核：`6001` id=7 既有 + `5101` id=32 / `4103` id=34 由 0742-2 加性补充 + `2211` id=33）；webServer JVM arg `-Derp-prj.default-payroll-subject-id=2211` 已就位（`playwright.config.ts:18`）。无额外科目需求，无种子/config 变更预期。
- contract credit memo 创建经跨域写 `ErpPurInvoice`/`ErpSalInvoice`（purchase/sales 域），自包含 setup 须建匹配 agreement + 计提行（0941-1 范式），无 webServer config 门控。

## Execution Plan

### Phase 1 - Explore：结算入口/过账面 + 后端可达性核实

Status: completed
Targets: `module-contract/erp-ct-service/.../ErpCtRebateSettlementBizModel.java`、`module-drp/erp-drp-service/.../DrpReleaseService.java`、`module-projects/erp-prj-service/.../ProjectSettlementAcctDocProvider.java`
Skill: `nop-debugging`

- Item Types: `Decision | Proof`
- Prereqs: none

> **行为已预决（草案审查迭代 1 收敛）**：contract 过账面 + projects 凭证结构经主代理源码复核 + 后端测试**已确定性核实**（见 Current Baseline），不再是开放问题。本 Phase 仅核实浏览器层调用细节（入参名、返回标量、setup 可达性），不重开「是否过账」。

- [x] `Proof`：冷核实三入口浏览器层调用细节——contract `postSettlement(@Name("settlementId"))` 返回 `ErpCtRebateSettlement`（实体，GraphQL 可选字段集）；drp `releaseApproved(@Name("planId"))` 返回 `ErpDrpPlan`（标量无选择集，0941-1 先例）；projects `approve(@Name("id"))` 返回 `ErpPrjProjectSettlement` + posted 翻转面。核实自包含 setup 可达性（contract 需 agreement+accrual 前置；drp 需 APPROVED plan+多行；projects 需 PnL 快照+CostCollection+`finalRevenue ≠ finalCost` 工程化）。
  - Skill: `nop-debugging`
- [x] `Decision`：界定每 spec 精确断言面（已在 Goals 预定，本 Phase 仅确认 setup 工程化细节）：contract = postSettlement 翻转 + 负额 credit memo + accruals isSettled（**无凭证行**）；drp = 全行 ORDERED + plan EXECUTED + 下游单据；projects = Dr 5101 + Cr 6001 + 条件性 4103（须 `finalRevenue ≠ finalCost`）。
  - Skill: none

**冷核实结论（执行期实时源码复核）**：

1. **contract** `ErpCtRebateSettlementBizModel.postSettlement(@Name("settlementId"), context):65` 返回实体 `ErpCtRebateSettlement`（GraphQL 选择集可达）。守卫 `status==DRAFT`（:67，否则 ERR_CT_SETTLEMENT_ILLEGAL_TRANSITION）。行为：聚合 `findUnsettledAccruals(agreementId)`（:74，`eq(isSettled,false)`）→ `creditAmount=total.negate()`（:91）→ PURCHASE 建 `ErpPurInvoice`(code=`CT-REBATE-{id}`, supplierId=partnerId, posted=false) + Line（:123-154）；SALES 建 `ErpSalInvoice`（:156-187）→ `setCreditMemoBillCode("CT-REBATE-"+id)`（:99）+ accruals `isSettled=true`（:104-108）→ status=POSTED（:112）。**经 `TestErpCtContractRebate.testRebateProgressiveAccrualAndSettlement:260-283` + `testSettlementIllegalTransition:287-300` 佐证**。
   - setup 可达性：须建 `ErpCtContract`(currencyId) + `ErpCtContractLine`(materialId) + `ErpCtRebateAgreement`(ACTIVE, contractId 非空驱动 resolveCurrencyId/MaterialId，否则发票 NOT NULL 违约) + Tier + `runAccrual` 产 accrual（isSettled=false）+ `ErpCtRebateSettlement`(DRAFT, rebateAgreementId)。复用种子 posted AP 发票 PINV-2026-001（supplierId=3）作 runAccrual 只读前置（0941-1 范式，__save 直置 posted=true 不可行）。

2. **drp** `ErpDrpLineBizModel.releaseApproved(@Name("planId"), context):41` **返回 null**（:43 `return null`）→ GraphQL `data` 为 null 但 `errors` 亦 null（成功）。委派 `DrpReleaseService.releaseApproved(planId):99` 按 `eq(status,APPROVED)` 过滤行（:101-103）逐行 `releaseLine`（:106）建下游单据，全部行 ORDERED/CANCELLED 后 `advancePlanToExecutedIfComplete`（:110,115-134）置 plan=EXECUTED。**无 plan 级状态守卫**（非 APPROVED 行被过滤跳过，released=0 时空返回，不抛错）。每行 releaseLine 含 per-line 守卫（ORDERED→ALREADY_ORDERED / 非 APPROVED→NOT_SUGGESTED，:144-149）。下游 code 前缀 `RELEASE_TO_CODE_PREFIX="DRP-"`（ErpDrpConstants）→ `DRP-TO-{lineId}` / `DRP-PO-{lineId}`（DrpReleaseService:173,197）。
   - setup 可达性：`__save` 直置 APPROVED plan + 多 APPROVED 行（TRANSFER/PURCHASE 混合，每行独立 materialId + Parameter 三元组隔离，0941-1 releaseLine 范式）。断言须经 `verifyState(plan)` + 逐行 `verifyState(line)`（mutation 返回 null 不可断言）。

3. **projects** `ErpPrjProjectSettlementBizModel.approve(@Name("id"), context):49` 返回实体 `ErpPrjProjectSettlement`（委派 Processor.approve:103）。FINAL/INTERIM `transferToAsset=false`（Processor.createSettlement:85，仅 CLOSE=true）故 approve 不建资产卡片。`finalRevenue=snapshot.revenueAmount`（Processor:82，来自 `ErpPrjBilling.amountFunctional` 聚合，ProjectPnlCalculator.sumRevenue:144-161 过滤 docStatus≠CANCELLED + businessDate 区间）；`finalCost=snapshot.totalCost`（Processor:83，来自 `ErpPrjCostCollectionLine.amount` 按 costCategory 聚合，sumCostByCategory:163-194 经父头 businessDate 区间）。凭证经 `ProjectSettlementAcctDocProvider:67-88` FINAL/INTERIM 分支：`profitLoss=finalRevenue-finalCost`（:70）→ Dr 5101(项目成本)=finalCost（:71）+ **条件性** `profitLoss.signum()!=0` 才发 4103（:75，方向由符号 signum>0→DEBIT / <0→CREDIT，金额=abs）+ Cr 6001(项目收入)=finalRevenue（:85）。billHeadCode=settlement.code（ProjectSettlementPostingDispatcher，同 CLOSE 0742-2）。
   - setup 工程化（`finalRevenue ≠ finalCost` 使 4103 确定性出现）：建 `ErpPrjBilling`(amountFunctional=10000) + `ErpPrjCostCollection`(APPROVED)+Line(MATERIAL,amount=6000) → refreshPnl → revenueAmount=10000/totalCost=6000 → profitLoss=4000>0 → **Dr 4103=4000（DEBIT）确定性出现**。`ErpPrjBilling/CostCollection/Settlement` ORM 无 use-approval tagSet（核实 app-erp-projects.orm.xml），__save 可直置 docStatus/approveStatus（同 CLOSE 0742-2 范式）。科目 6001 id=7 + 5101 id=32 + 4103 id=34 种子齐备（0742-2）。

Exit Criteria:

- [x] 三入口浏览器层调用细节（入参名/返回类型/setup 可达性）冷核实结论记录入计划
- [x] 每 spec setup 工程化细节界定（尤其 projects `finalRevenue ≠ finalCost` 使 4103 确定性出现）

---

### Phase 2 - spec 落地 + 全套件回归

Status: completed
Targets: `tests/e2e/business-actions/ct-rebate-settlement.action.spec.ts`、`tests/e2e/business-actions/drp-release-approved.action.spec.ts`、`tests/e2e/business-actions/projects-pnl-settlement.action.spec.ts`
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 1

> **执行期 dict 修正（Phase 2 捕获）**：projects spec 首跑失败——`ErpPrjBilling/CostCollection.docStatus` 绑定字典 `erp-prj/project-status`（DRAFT/OPEN/ON_HOLD/COMPLETED/CANCELLED，无 APPROVED），`__save` 传 `docStatus:'APPROVED'` 抛「非法的字典项:APPROVED」。同 0742-2 范式改为 `docStatus:'OPEN' + approveStatus:'APPROVED'`（sumRevenue/sumCostByCategory 仅过滤 `docStatus≠CANCELLED`，OPEN 命中聚合）。草案 Phase 1「`ErpPrjBilling/CostCollection/Settlement` ORM 无 use-approval tagSet」核实有误——三者均带 `use-approval` tagSet，但 docStatus 字典独立裁决。修正后 6/6 全绿。

- [x] `Add`：contract 返利结算 spec——复用 0941-1 范式建 agreement（PURCHASE 或 SALES）+ 种子 posted 发票作计提前置 → `runAccrual` 产 `ErpCtRebateAccrual`（isSettled=false）→ 建 `ErpCtRebateSettlement`(DRAFT) 关联 → `ErpCtRebateSettlement__postSettlement(settlementId)` → `verifyState` POSTED + `settlement.creditMemoBillCode="CT-REBATE-{id}"` + `findFirst` 跨域反查负额 credit memo 发票（PURCHASE→`ErpPurInvoice` totalAmount<0 / SALES→`ErpSalInvoice` totalAmount<0，posted=false）+ accruals `isSettled=true` 翻转 + 非法态守卫（已 POSTED 再 postSettlement）。
  - Skill: none
- [x] `Add`：drp releaseApproved 批量释放 spec——自包含建 APPROVED plan（`__save` 直置 APPROVED 绕过引擎精确控制 replenishmentType，0941-1 范式）+ 多行（TRANSFER/PURCHASE 混合）→ `ErpDrpLine__releaseApproved(planId)` → 全行 status=ORDERED + 计划 EXECUTED 翻转（`verifyState`/`findFirst`）+ 下游单据（ErpInvTransferOrder/ErpPurOrder）创建断言 + 非 APPROVED 守卫。
  - Skill: none
- [x] `Add`：projects FINAL/INTERIM 损益结转 spec——自包含 ProjectType(defaultSubjectId)+Project(OPEN)+PnL 快照（refreshPnl 产 CALCULATED）+ CostCollection/Line(MATERIAL) + Settlement(settlementType=FINAL)，**关键：setup 须工程化 `finalRevenue ≠ finalCost` 使 4103 行确定性出现**（CLOSE 先例 `projects-settlement-posting:56` 仅 finalCost 无 revenue，不可照搬）→ submit→approve → posted + 凭证行断言 `Dr 5101=finalCost` + `Cr 6001=finalRevenue` + 条件性 4103（方向由 `finalRevenue-finalCost` 符号决定）+ reverse 红冲同向取负 + INTERIM 对照（凭证结构同 FINAL）。
  - Skill: none
- [x] `Proof`：新增 spec `--workers=1` 全绿 + business-actions 全套件回归 0 新增失败。
  - 验证命令：`BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1 npx playwright test tests/e2e/business-actions/ct-rebate-settlement.action.spec.ts tests/e2e/business-actions/drp-release-approved.action.spec.ts tests/e2e/business-actions/projects-pnl-settlement.action.spec.ts --workers=1` + 全套件抽样回归
  - Skill: none
  - **实测结果**：3 spec 6/6 全绿（46.4s）；business-actions 全套件 153 passed（18.8m，0 新增失败）。

Exit Criteria:

- [x] 3 spec 全绿，状态/字段翻转 + 凭证行数值均经 `verifyState`/`findFirst`/`assertVoucherLines`（projects spec 经 `from '../orchestration/_helper'` 导入）独立断言
- [x] business-actions 全套件回归 0 新增失败

---

### Phase 3 - 文档对齐 + Deferred RELEASED 登记

Status: completed
Targets: `docs/testing/e2e-runbook.md`、`docs/backlog/README.md`、`docs/plans/2026-07-14-0941-1-contract-drp-orchestration-e2e.md`、`docs/plans/2026-07-14-0742-2-projects-posting-lifecycle-voucher-line-e2e.md`、`docs/logs/2026/07-17.md`
Skill: none

- Item Types: `Add`
- Prereqs: Phase 2

- [x] `Add`：`e2e-runbook.md` 业务动作表 +3 行（contract 返利结算 postSettlement、drp releaseApproved、projects FINAL/INTERIM 损益结转）+ 凭证行断言表 +projects FINAL/INTERIM 行 + **套件计数对齐实际**（预存漂移校正：runbook 声称「17 域 55 spec」，实际 58 `.action.spec.ts`；本计划后为 61，须先校正基线 55→58 再 +3，非简单 +3）；`backlog/README.md` +1 done 行（2026-07-17-1005-2）。
  - Skill: none
  - **实测落地**：业务动作表头/目录树注释 55→61（漂移校正合并），分层运行表 55→61，overview + full-suite 描述 58→61；业务动作表 +3 行（contract/drp/projects），凭证行断言表 +PROJECT_SETTLEMENT 行 + 套件计数注 +1005-2 说明；backlog README +1 ✅ done 行。
- [x] `Add`：0941-1 Deferred 两段 + 0742-2 Deferred 一段各补 `**RELEASED by 2026-07-17-1005-2**` 行（触发条件已满足 + 本计划交付证据）；`docs/logs/2026/07-17.md` 增聚合条目（spec 数/验证状态/范围纪律 + 1005-1 取消记录）。
  - Skill: none
  - **实测落地**：0941-1「返利结算单生成 E2E」+「drp releaseApproved 批量释放深度编排 E2E」+ 0742-2「结算 FINAL/INTERIM 损益结转凭证行断言」三段各补 RELEASED；日志顶部新条目（背景/Phase 1-3/验证状态/范围纪律/1005-1 取消记录）。

Exit Criteria:

- [x] e2e-runbook（含漂移校正）+ backlog README + 0941-1/0742-2 RELEASED 登记 + 日志五点落地一致

## Draft Review Record

- Independent draft review iteration 1: **needs revision**（独立 general 子代理 `ses_0922a2796ffegO3XBKepca5969`，新会话冷重播无执行者/起草者上下文，2026-07-17）。**VERDICT: needs revision（light — 1 MAJOR，0 BLOCKER）**。全部 load-bearing 主张经实时仓库核实零伪：三项 Deferred 真实（0941-1:157-167 / 0742-2:140-144）+ 三后端 DIRECT `@BizMutation` 浏览器层可达 + 种子 COA（6001 id=7 / 5101 id=32 / 4103 id=34 / 2211 id=33）+ webServer JVM arg 就位 + scope-manufacturing 裁决 legitimately warranted（非 padding）。规则 R1-R14/anti-slack/template/naming 全 PASS（R4/R14 bundling 合理）。
  - **MAJOR 1（已修订）**：Phase 1 Explore 过度 hedge——contract 过账面与 projects 凭证结构在源码 + 后端测试中**已有确定性答案**，不应留为 Explore 开放问题。主代理源码复核确认：① contract `postSettlement:64-65` **不过 GL 凭证**，跨域建负额 credit memo 发票（`:125-186`）+ `TestErpCtContractRebate:260-283` 佐证；② projects `ProjectSettlementAcctDocProvider:69-87` FINAL/INTERIM 凭证 = Dr 5101 + 条件性 4103（仅 `profitLoss≠0`）+ Cr 6001。**已修订**：Current Baseline 预决两行为 + Goals/Phase 2 contract spec 收紧为 `postSettlement` + 负额 credit memo（非「若过账」）+ projects spec 标注 4103 条件性 + setup 须工程化 `finalRevenue≠finalCost` + Phase 1 收窄为调用细节核实。
  - **MINOR 1（已修订）**：凭证行两原语位于 `tests/e2e/orchestration/_helper.ts`（非 business-actions/_helper.ts），先例 `projects-settlement-posting.action.spec.ts:13` 跨目录导入。**已修订** Current Baseline + Phase 2 Exit Criteria 标注导入路径。
  - **MINOR 2（已修订）**：projects 4103 行条件性（`profitLoss.signum()!=0` 才发，方向由符号）—— spec setup 须工程化 `finalRevenue≠finalCost`。**已修订** Goals/Phase 2 显式标注。
  - **MINOR 3（已修订）**：e2e-runbook 预存漂移（声称 17 域 55 spec，实际 58）。**已修订** Phase 3 须校正基线再 +3。
- **结论**：4 项（1 MAJOR + 3 MINOR）全部修订落地；阻塞项 0。待独立 re-review 收敛后转 active。
- Independent draft review iteration 2: **accept**（独立 general 子代理 `ses_0921cbadaffeW1xqFgVsqlN25D`，新会话冷重播无执行者/起草者上下文，2026-07-17）。**VERDICT: accept**。iteration-1 全部 4 项 finding 经独立源码复核确认 **addressed-verified**：MAJOR 1（contract `postSettlement:64-65` 无 GL 凭证 + 负额 credit memo `:125-186` + accruals isSettled + TestErpCtContractRebate:260-283 佐证；projects `ProjectSettlementAcctDocProvider:69-87` Dr 5101 + 条件性 4103 signum check + Cr 6001；hedge 全消解）/ MINOR 1（凭证原语 orchestration/_helper.ts:92/151 + 先例 :13 导入）/ MINOR 2（4103 条件性 + finalRevenue≠finalCost setup）/ MINOR 3（runbook 55→58 漂移校正再 +3）。0 new BLOCKER / 0 new MAJOR / 1 self-correcting MINOR（projects `approve(@Name("id"))` 非 `@Name("settlementId")`——Phase 1 冷核实会捕获，已就地修订 Phase 1:87）。规则 R1-R14/anti-slack/template/naming 全 PASS；internal consistency 全 agree；scope-manufacturing legitimately warranted（3 spec 均真实净增缺口，0941-1 仅交付 runAccrual+单行 releaseLine / 0742-2 仅交付 CLOSE 转固）。
- **共识达成 → `Plan Status: active`**。

## Closure Gates

> 本计划为前端/浏览器 E2E（行为驱动结果面），纯消费侧结算/批量/深化 successor + 测试层（预期零生产契约变更）。结束前运行新增 spec + business-actions 回归 + 后端构建（确认 spec 变更未污染后端）。

- [x] 范围内行为完成（0941-1 两项 + 0742-2 一项 Deferred 各交付浏览器层 E2E + 状态/字段/凭证行翻转独立断言）
- [x] 相关文档对齐（e2e-runbook 业务动作表 +3 行 + 凭证行断言表、backlog README done 行、0941-1/0742-2 RELEASED 登记、日志）
- [x] 已运行验证：新增 spec `--workers=1` 全绿 + business-actions 回归 0 新增失败 + `mvn clean install -DskipTests` 154 模块 BUILD SUCCESS（确认零后端污染）
- [x] 无范围内项目降级为 deferred/follow-up（疑似生产缺陷须开显式 successor，不得模糊化）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

> 行为预决（草案审查迭代 1 收敛）：contract 过账面 + projects 凭证结构已确定性核实，原「待 Explore」条件项已消解为既定事实（见 Current Baseline / Non-Goals）。

### contract 返利结算 GL 凭证化（如未来要求）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: `postSettlement` 经源码核实**按设计不过 GL 凭证**，改走跨域负额 credit memo 发票（AP/AR 侧，由发票自身审批/过账链处理）。这不是缺口而是既定行为；contract spec 断言 credit memo + 计提翻转，不触凭证行。
- Successor Required: `yes`（触发条件：产品要求返利结算独立生成 GL 凭证时——须后端先增 `IErpFinVoucherBiz.post` 接线）

### drp releaseApproved 全行下游单据凭证行断言

- Classification: `optimization candidate`
- Why Not Blocking Closure: 下游 ErpInvTransferOrder/ErpPurOrder 创建即各自过账链（inventory/purchase 域），非 drp 释放面产物。本计划断言下游单据创建存在性，不深入其凭证行（归各自域 successor）。
- Successor Required: `no`

## Closure

Status Note: 全部 3 Phase 完成。Phase 1（Explore）经草案审查迭代已预决并 [x]；Phase 2 落地 3 新 spec（6 用例）全绿——`ct-rebate-settlement`（postSettlement DRAFT→POSTED + 跨域负额 credit memo AP 发票 totalAmount=-96.05 + creditMemoBillCode=CT-REBATE-{id} + accruals isSettled 翻转 + 非法态守卫；**按设计不过 GL 凭证**改断言 credit memo 发票）、`drp-release-approved`（releaseApproved APPROVED plan 全行 ORDERED + 计划 EXECUTED + 下游 TransferOrder/PurOrder 创建 + SUGGESTED-only no-op 守卫；mutation 返回 null 经 verifyState/findFirst 断言）、`projects-pnl-settlement`（FINAL/INTERIM approve→posted + 凭证行 Dr 5101=6000/条件性 Dr 4103=4000(profitLoss>0)/Cr 6001=10000 + reverseSettlement 红冲同向取负 + INTERIM 对照；setup 工程化 finalRevenue≠finalCost 使 4103 确定性出现）。**执行期 dict 修正**：projects spec 首跑失败因 `ErpPrjBilling/CostCollection.docStatus` 绑字典 `erp-prj/project-status`（无 APPROVED），改 `docStatus=OPEN + approveStatus=APPROVED`（同 0742-2 范式）后 6/6 全绿。Phase 3 文档对齐：e2e-runbook 业务动作表 +3 行 + 凭证行断言表 +PROJECT_SETTLEMENT 行 + 套件计数漂移校正（55→61 spec，overview/full-suite 58→61）+ 套件计数注；backlog README +1 ✅ done 行；0941-1 两段 + 0742-2 一段各补 RELEASED；日志顶部新条目（含 1005-1 取消记录）。验证基线：3 spec 6 passed（46.4s）+ business-actions 全套件 153 passed（18.8m，0 新增失败）+ `mvn clean install -DskipTests` 154 模块 BUILD SUCCESS（closure gate，零后端污染）。范围纪律：纯测试 + 文档，零生产代码/契约/ORM/种子/config 变更。

Closure Audit Evidence:

- 执行者自证（待独立结束审计复核）：实时仓库核实——3 spec 文件存在（`tests/e2e/business-actions/{ct-rebate-settlement,drp-release-approved,projects-pnl-settlement}.action.spec.ts`）且测试结构与 Phase 2 item 对齐（2+2+2=6 测试）；e2e-runbook 业务动作表 +3 行 + 凭证行断言表 +1 行 + 套件计数 4 处对齐（55→61 / 58→61）+ 套件计数注 +1005-2 说明；backlog/README +1 ✅ done 行；0941-1/0742-2 三段 Deferred 各补 `**RELEASED by 2026-07-17-1005-2**`；日志顶部新条目；plan 全 Phase item [x] 无 [ ] 残留 + 三 Phase Status completed + Plan Status completed；Deferred But Adjudicated 仅 2 显式项（contract GL 凭证化 yes-successor / drp 下游凭证行 no-successor）无静默降级；执行期 dict 修正已记录在 Phase 2 段 + 日志 + backlog README 行。独立测试运行实测：3 spec 6 passed（46.4s）/ business-actions 全套件 153 passed（18.8m，0 新增失败）/ `mvn clean install -DskipTests` 154 模块 BUILD SUCCESS（01:45 min）。
- Auditor / Agent: 独立结束审计子代理（新会话冷重播，无执行者/起草者上下文，2026-07-17）。**Verdict: PASS** —— 逐项核对全部确认：(1) 计划内部一致性 PASS——`Plan Status: completed`(plan:3)、三 Phase `Status: completed`(:78/:112/:141)、全部 Phase item [x] 无 [ ] 残留、Closure Gates 除 gate7（本次勾选）外均 [x]、Status Note(:201) + Closure Audit Evidence(:205) 均非占位符已填实。(2) Spec 工件 PASS——3 spec 文件均存在 `tests/e2e/business-actions/{ct-rebate-settlement,drp-release-approved,projects-pnl-settlement}.action.spec.ts`；test() 计数 2+2+2=6 与 Phase 2 item 对齐；ct spec `ct-rebate-settlement:54/167` 2 测试（happy postSettlement DRAFT→POSTED + 跨域负额 credit memo ErpPurInvoice totalAmount=-96.05 `:140` + accruals isSettled=true `:150` + 非法态守卫，用 verifyState/findFirst 无凭证行断言符合 Non-Goal）；drp spec `drp-release-approved:138/194` 2 测试（happy 全行 ORDERED + plan EXECUTED + 下游 TransferOrder/PurOrder + SUGGESTED-only no-op 守卫，mutation 返回 null 经 verifyState/findFirst 断言 `:151-180`）；projects spec `projects-pnl-settlement:142/219` 2 测试（FINAL 凭证行 Dr 5101=6000/Dr 4103=4000/Cr 6001=10000 `:126-132` + reverse 同向取负 `:133-139` + INTERIM 对照），经 `from '../orchestration/_helper'` 导入 findVoucherIdByBillCode/assertVoucherLines（`:13`）✓，docStatus='OPEN' 非 'APPROVED'（执行期修正，`:88/:96`）✓。(3) 文档对齐 PASS——e2e-runbook 业务动作表 +3 行（contract/drp/projects `runbook` diff）+ 凭证行断言表 +PROJECT_SETTLEMENT 行；套件计数 4 处对齐「61 spec/61 业务动作 spec」（`:5/:115/:119/:220/:643`），磁盘实测 61 个 `.action.spec.ts` 文件吻合；唯一「55 spec」为漂移校正说明注（`:384`，非活跃计数）；backlog/README +1 `2026-07-17-1005-2 ✅ done` 行；0941-1 两段 + 0742-2 一段各补 `**RELEASED by 2026-07-17-1005-2**`；logs/2026/07-17.md 顶部新条目（倒序）含 1005-1 取消记录。(4) Deferred But Adjudicated 完整性 PASS——仅 2 显式项（contract GL 凭证化 yes-successor :187-191 / drp 下游凭证行 no-successor :193-197）无静默降级；`git diff --stat HEAD` 仅 docs + 3 新 .ts（零 Java/ORM/seed/config），实测 tracked 变更 name-only 全部 docs/。(5) 验证证据 PASS——`_tmp/mvn-build.log:8515` BUILD SUCCESS，Reactor Summary 8358-8515 范围 154 模块全 SUCCESS 0 FAILURE/SKIPPED，01:45 min，与 Status Note 声称一致无矛盾。**观察项（非阻塞）**：git status 另含 `2026-07-14-0508-1` plan 修改（补 0941-2 overdue RELEASED 注解 b2b/aps/logistics）+ 未跟踪 `1005-1` 计划文件（已取消姊妹计划）——均为 doc-only housekeeping 且内容正确，不违反「纯测试+文档」范围纪律，不影响关闭正确性。

Follow-up:

- <仅非阻塞跟进项目；已确认的缺陷须以显式 successor 承接，不得出现在此处>
