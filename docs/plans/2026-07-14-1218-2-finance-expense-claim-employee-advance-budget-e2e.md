# 2026-07-14-1218-2-finance-expense-claim-employee-advance-budget-e2e finance 费用报销 + 员工借款 DIRECT 业务动作 + 预算控制 + 预算对比报表数值断言浏览器层 E2E

> Plan Status: completed
> Mission: erp
> Work Item: finance 费用报销/员工借款 DIRECT 业务动作 E2E + 报销预算控制 hook + 预算对比报表数值断言
> Last Reviewed: 2026-07-14
> Source: `docs/plans/2026-07-09-2004-1-business-action-e2e-maintenance-projects-quality.md` Deferred「全 18 域全业务动作覆盖（DIRECT 域剩余）」finance 子集（Successor Required: yes，触发条件「各域细化端到端验证时」——已满足：14 域已覆盖，finance 域 ExpenseClaim/EmployeeAdvance 为 DIRECT useApproval 未覆盖实体）；`docs/plans/2026-07-14-0606-1-finance-budget-lifecycle-control-e2e.md` Deferred「付款/报销预算控制 hook E2E」+「预算对比报表 AMIS 渲染数值断言」（Successor Required: yes，触发条件已满足）
> Related: `2026-07-14-0606-1`（预算方案生命周期 + 预算控制 hook PO 代表验证范式源）、`2026-07-10-0335-1`（useApproval DIRECT 审批轴 E2E 范式源）、`2026-07-14-0215-1`（assets DIRECT E2E 范式源）、`docs/design/finance/expense-claim.md`（费用报销/员工借款 owner doc）、`docs/design/finance/budget.md`（预算管理 owner doc）
> Audit: required

## Current Baseline

finance 域 ExpenseClaim + EmployeeAdvance 后端已全部落地（core-business-roadmap 1.8 done，计划 `2026-07-02-0700-2`）：

- **ErpFinExpenseClaim**（`module-finance/model/app-erp-finance.orm.xml`）：`tagSet="gid,erp.finance,use-approval"`，**无 `useWorkflow`** → DIRECT 审批轴
  - 三轴状态机：docStatus（DRAFT/SUBMITTED/APPROVED/REJECTED/CANCELLED）+ approveStatus（UNSUBMITTED/SUBMITTED/APPROVED/REJECTED）+ posted
  - 审批动作（经 xbiz → `ErpFinExpenseClaimProcessor`）：`submitForApproval` / `approve` / `reject` / `reverseApprove` / `withdrawApproval`
  - 过账：`ExpenseClaimPostingDispatcher` → `ExpenseClaimAcctDocProvider`，businessType=`EXPENSE_CLAIM`，billHeadCode=`claim.code`
  - 凭证行：Dr 6602 管理费用(amountWithoutTax) / Dr 2221 进项税(taxAmount) / Cr 2241 应付-员工(OWN_ACCOUNT) 或 Cr 1002 银行存款(COMPANY_ACCOUNT)
  - 预算控制 hook：`ErpFinExpenseClaimProcessor.approve` → `runBudgetCheckHook`（gated by `erp-fin.expense-budget-check-enabled` 默认 false，subject `erp-fin.budget-expense-subject-code`），sourceBillType=`EXPENSE_CLAIM`
  - 报销抵扣借款：`erp-fin.advance-auto-offset-on-expense`（默认 true），approve 时自动抵扣同员工未还借款

- **ErpFinEmployeeAdvance**：`tagSet="gid,erp.finance,use-approval"`，**无 `useWorkflow`** → DIRECT 审批轴
  - 三轴状态机：同 ExpenseClaim 范式
  - 过账：`EmployeeAdvancePostingDispatcher` → `EmployeeAdvanceAcctDocProvider`，businessType=`EMPLOYEE_ADVANCE`，billHeadCode=`advance.code`
  - 凭证行：Dr 1221 其他应收款-员工预支(amount) / Cr 1002 银行存款(amount)

- **预算对比报表**：`ErpFinReportBizModel.buildBudgetVsActualDataset` → `IErpFinBudgetLineBiz.getBudgetVsActual(acctSchemaId, periodId, subjectId)` `@BizQuery`，返回 `BudgetVsActualRow` 列表（subjectCode/subjectName/budgetAmount/actualAmount/availableAmount）；种子报表模板 `budget-vs-actual.xpt.xml` 已存在

**浏览器层 E2E 缺口**：
- ExpenseClaim + EmployeeAdvance 零 E2E 覆盖（2004-1 Deferred finance 子集）
- ExpenseClaim 预算控制 hook 零 E2E 覆盖（0606-1 Deferred「报销预算控制 hook」——Payment hook 因 `ErpPurPayment` useWorkflow xwf 经 2330-1 裁决浏览器层不可行，仅 ExpenseClaim DIRECT 可达）
- 预算对比报表数值断言零覆盖（0606-1 Deferred「预算对比报表」——0606-1 Phase 2 test (c) 仅断言 getBudgetVsActual 查询结构非空，未断言 actualAmount 增量/availableAmount 余量）

**种子科目缺口**：种子 `erp_md_subject.csv` 当前含 1002/2221/6602（0215-1 补齐 6602）。ExpenseClaim 过账额外需 2241（其他应付款-员工），EmployeeAdvance 过账需 1221（其他应收款-员工预支），当前缺失，需补齐。

**E2E 基础设施就绪**：`tests/e2e/business-actions/_helper.ts` 三原语 + `tests/e2e/orchestration/_helper.ts` `findVoucherIdByBillCode`/`assertVoucherLines` 凭证行断言范式经 14 域验证。预算控制 E2E 范式（HARD 事务回滚无 ControlLog 持久化 + WARN 放行 ControlLog 持久化）经 0606-1 PO 代表验证。webServer JVM arg 已含 `erp-fin.budget-check-enabled=true` + `erp-fin.budget-purchase-expense-subject-code=6601`（0606-1）。

## Goals

- ExpenseClaim + EmployeeAdvance useApproval DIRECT 审批轴经 GraphQL `/graphql` 浏览器层全栈可达性 + 三轴状态机迁移验证
- 覆盖 ExpenseClaim 两种 paymentMode（OWN_ACCOUNT 贷应付-员工 / COMPANY_ACCOUNT 贷银行存款）+ 价税分离三件套凭证行精确数值断言
- 覆盖 EmployeeAdvance 借款审核 + EMPLOYEE_ADVANCE 凭证行精确数值断言 + 报销抵扣借款联动（EMPLOYEE_ADVANCE_SETTLE 凭证）
- 覆盖 ExpenseClaim 预算控制 hook HARD 阻断 + WARN 放行 + config 门控启用向后兼容
- 覆盖预算对比报表 getBudgetVsActual 数值断言：budgetAmount（BUDGET 凭证）+ actualAmount（ExpenseClaim NORMAL 凭证）+ availableAmount 余量

## Non-Goals

- **Payment (AP_PAYMENT) 预算控制 hook E2E**——`ErpPurPayment` `useWorkflow="true"` xwf，经 2330-1 裁决浏览器层不可行，排除
- **ErpSalReceipt 预算控制**——同上 xwf 不可行
- **员工借款现金还款 (EMPLOYEE_ADVANCE_SETTLE 现金还款路径)**——owner doc 设计了现金还款凭证承载路径，但后端 `ErpFinEmployeeAdvanceBizModel` 是否实现 `cashRepay` 入口需 Explore 核实；若未实现归 Deferred
- **薪资扣回还款路径**——依赖 HR `Additional Salary` 机制（expense-claim.md Follow-up）
- **预算方案多期间分组 + parentScenario 层级聚合**——0606-1 Deferred，属预算编制增强面
- **预算对比报表 AMIS 前端渲染层 DOM 断言**——属 `reports/*.visual.spec.ts` 范畴，本计划聚焦 GraphQL `getBudgetVsActual` 数值断言

## Task Route

- Type: `verification work`（扩展现有 Playwright E2E 套件覆盖至 finance ExpenseClaim/EmployeeAdvance DIRECT 业务动作 + 预算控制 hook + 预算对比报表数值断言）
- Owner Docs: `docs/design/finance/expense-claim.md`（费用报销/员工借款设计）、`docs/design/finance/budget.md`（预算管理设计）、`docs/testing/e2e-runbook.md`（业务动作套件 + 调用范式）
- Skill Selection Basis: 浏览器层 E2E 测试编写（Playwright + GraphQL mutation 驱动 @BizMutation）→ 无匹配技能（`nop-testing` 技能面向 JunitAutoTestCase 后端快照测试，非 Playwright 浏览器层）；沿用 `tests/e2e/business-actions/_helper.ts` 既有范式 → `Skill: none`

## Infrastructure And Config Prereqs

- 种子 `erp_md_subject.csv` 需补齐 2 科目行：2241（其他应付款-员工，LIABILITY/CREDIT）+ 1221（其他应收款-员工预支，ASSET/DEBIT），按 0215-1/0413-2 范式
- webServer JVM arg 追加：`-Derp-fin.expense-budget-check-enabled=true` + `-Derp-fin.budget-expense-subject-code=6602`（ExpenseClaim 预算控制 hook 独立开关 + 科目编码，与 PO 的 `budget-check-enabled`/`budget-purchase-expense-subject-code` 分离）
- 向后兼容验证：启用 `expense-budget-check-enabled=true` 后，既有 ExpenseClaim 相关 spec（本计划新建）在无匹配 APPROVED 预算方案时须 PASS-on-no-match（同 0606-1 PO 向后兼容范式）

## Execution Plan

### Phase 1 - ExpenseClaim 生命周期 + 凭证行数值断言

Status: completed
Targets: `tests/e2e/business-actions/fin-expense-claim.action.spec.ts`（新建）
Skill: none

- Item Types: `Add | Proof | Decision`
- Prereqs: 无（自包含 setup）

- [x] `Decision | Explore`: 裁定 ExpenseClaim setup 最小预置集——claimantId（员工 + partnerId 解析，expense-claim.md 补注 #4：员工无 partner 记录时审核被拒）、paymentMode、行级 expenseType/subjectId/amountWithoutTax/taxAmount/amountWithTax。裁定报销抵扣借款联动（`advance-auto-offset-on-expense` 默认 true）是否在无借款时跳过（无 settleAdvanceId → 无抵扣）。裁定 `erp-fin.expense-budget-check-enabled=true` 启用后无匹配预算时 PASS-on-no-match 向后兼容。裁定结果记入执行日志。
  - Skill: none
- [x] `Add`: **ExpenseClaim 生命周期 spec** `fin-expense-claim.action.spec.ts`
  - **OWN_ACCOUNT 路径**：自包含建员工（含 partnerId）+ ExpenseClaim（DRAFT/UNSUBMITTED + 1 行 expenseType/subjectId/amountWithoutTax=100/taxAmount=13/amountWithTax=113）→ `submitForApproval`(UNSUBMITTED→SUBMITTED) → `approve`(SUBMITTED→APPROVED + posted=true) → `verifyState` 经 `__get` 核实实际 docStatus（不预设值）→ `assertVoucherLines` 断言 EXPENSE_CLAIM 凭证行 Dr 6602=100 / Dr 2221=13 / Cr 2241=113（billHeadCode=claim.code）
  - **COMPANY_ACCOUNT 路径**：同上但 paymentMode=COMPANY_ACCOUNT → `assertVoucherLines` 断言 Cr 1002=113（非 2241）
  - `reverseApprove` 红冲：APPROVED → reverseApprove → `findVoucherIdByBillCode(code, 'REVERSAL')` 断言红字凭证行同向取负 + 原凭证 isReversed=true + posted=false
  - 非法迁移守卫（UNSUBMITTED→approve / APPROVED→submitForApproval 拒绝，ErrorCode message token）
  - Skill: none

Exit Criteria:

- [x] 1 spec 文件经 `npx playwright test tests/e2e/business-actions/fin-expense-claim.action.spec.ts --workers=1` 全绿
- [x] OWN_ACCOUNT + COMPANY_ACCOUNT 两路径凭证行精确数值断言经 `assertVoucherLines` 验证
- [x] approve→posted 状态翻转经 `verifyState` `__get` 独立断言

### Phase 2 - ExpenseClaim 预算控制 hook + 预算对比报表数值断言

Status: completed
Targets: `tests/e2e/business-actions/fin-expense-claim-budget.action.spec.ts`（新建）、`tests/e2e/business-actions/fin-budget-vs-actual.value.spec.ts`（新建）
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 1 ExpenseClaim setup 范式验证

- [x] `Add`: **ExpenseClaim 预算控制 hook spec** `fin-expense-claim-budget.action.spec.ts`
  - 自包含建预算方案（HARD controlLevel + budget line subjectId=6602（种子 id=31，科目编码 6602）+ periodId=1 + budgetAmount=50 + costCenterId=null + projectId=null 以匹配 NORMAL 凭证行聚合键）+ `approve` 预算方案 → BUDGET 凭证
  - **HARD 阻断负路径**：建 ExpenseClaim amountWithoutTax=100 > 预算 50 → `approve` 抛 ERR_BUDGET_EXCEEDED「预算超支」+ approveStatus 保持 SUBMITTED + 无 ControlLog 持久化（事务回滚，同 0606-1 PO HARD 范式）
  - **WARN 放行正路径**：建预算方案 controlLevel=WARN + ExpenseClaim amountWithoutTax=100 > 预算 50 → `approve` APPROVED + ControlLog WARNED 持久化（findFirst actionResult=WARNED）
  - Skill: none
- [x] `Add`: **预算对比报表数值断言 spec** `fin-budget-vs-actual.value.spec.ts`
  - 自包含建预算方案（NONE controlLevel + budget line subjectId=6602（种子 id=31）+ periodId=1 + budgetAmount=1000 + costCenterId=null + projectId=null）+ `approve` → BUDGET 凭证 Dr 6602=1000
  - **初始断言**：`getBudgetVsActual(acctSchemaId, periodId, subjectId)` → BudgetVsActualRow budgetAmount=1000 / actualAmount=0 / availableAmount=1000（subjectName 按种子实际名称"折旧费用"断言，非 Provider 注释"管理费用"）
  - **actual 增量断言**：建 ExpenseClaim amountWithoutTax=200 + `approve` → NORMAL 凭证 Dr 6602=200 → 重新 `getBudgetVsActual` → actualAmount=200 / availableAmount=800
  - **红冲后回退断言**：ExpenseClaim `reverseApprove` → 红字凭证 Dr 6602=−200 → 重新 `getBudgetVsActual` → actualAmount=0 / availableAmount=1000
  - Skill: none

Exit Criteria:

- [x] 2 spec 文件经 `npx playwright test tests/e2e/business-actions/fin-expense-claim-budget.action.spec.ts tests/e2e/business-actions/fin-budget-vs-actual.value.spec.ts --workers=1` 全绿
- [x] HARD 阻断事务回滚无 ControlLog + WARN 放行 ControlLog 持久化断言验证
- [x] budgetAmount/actualAmount/availableAmount 三值增量断言验证（初始 → actual 增量 → 红冲回退）

### Phase 3 - EmployeeAdvance 生命周期 + 凭证行数值断言 + 报销抵扣联动

Status: completed
Targets: `tests/e2e/business-actions/fin-employee-advance.action.spec.ts`（新建）
Skill: none

- Item Types: `Add | Proof | Decision`
- Prereqs: Phase 1 ExpenseClaim setup 范式验证（报销抵扣联动需 ExpenseClaim + EmployeeAdvance 双实体 setup）

- [x] `Decision | Explore`: 裁定 EmployeeAdvance setup 最小预置集——employeeId（同 ExpenseClaim 员工）、advanceType（EXPENSE_ADVANCE）、amount。裁定报销抵扣借款联动（`advance-auto-offset-on-expense`）：先 approve EmployeeAdvance（建借款 EMPLOYEE_ADVANCE 凭证）→ 再 approve ExpenseClaim（建报销 EXPENSE_CLAIM 凭证 + 自动抵扣 EMPLOYEE_ADVANCE_SETTLE 凭证）→ 断言三张凭证。若 `AdvanceOffsetOrchestrator` 在浏览器层不可达或 offset 金额为零（借款金额 > 报销金额时 offset=报销金额），裁定降级路径。裁定结果记入执行日志。
  - Skill: none
- [x] `Add`: **EmployeeAdvance 生命周期 spec** `fin-employee-advance.action.spec.ts`
  - **借款审核路径**：自包含建员工 + EmployeeAdvance（DRAFT/UNSUBMITTED + amount=500）→ `submitForApproval` → `approve`(APPROVED + posted=true) → `assertVoucherLines` 断言 EMPLOYEE_ADVANCE 凭证行 Dr 1221=500 / Cr 1002=500（billHeadCode=advance.code）
  - `reverseApprove` 红冲：APPROVED → reverseApprove → 红字凭证行同向取负 Dr 1221=−500 / Cr 1002=−500 + 原凭证 isReversed=true
  - **报销抵扣联动**（if Explore 裁定可达）：先 approve EmployeeAdvance(amount=500) → 再 approve ExpenseClaim(amountWithoutTax=200, taxAmount=26, amountWithTax=226, OWN_ACCOUNT) → 断言三张凭证：EMPLOYEE_ADVANCE(Dr 1221=500/Cr 1002=500) + EXPENSE_CLAIM(Dr 6602=200/Dr 2221=26/Cr 2241=226) + EMPLOYEE_ADVANCE_SETTLE(Dr 2241=226/Cr 1221=226, offset=min(payableOpen=226, receivableOpen=500)=226) → 断定 advance.settledAmount 增量=226
  - 非法迁移守卫（UNSUBMITTED→approve / APPROVED→submitForApproval 拒绝）
  - Skill: none

Exit Criteria:

- [x] 1 spec 文件经 `npx playwright test tests/e2e/business-actions/fin-employee-advance.action.spec.ts --workers=1` 全绿
- [x] EMPLOYEE_ADVANCE 凭证行 Dr 1221/Cr 1002 精确数值断言经 `assertVoucherLines` 验证
- [x] 报销抵扣联动 EMPLOYEE_ADVANCE_SETTLE 凭证断言（if Explore 裁定可达）或降级为 Deferred（if 不可达）

## Draft Review Record

- Independent draft review iteration 1: `needs revision` (ses_0a11f6bf7ffeTl4v0Xk0iayier) — M1: Phase 3 报销抵扣 SETTLE 凭证金额自相矛盾（offset=min(payableOpen=226, receivableOpen=500)=226 非 200，SETTLE 凭证 Dr 2241=226/Cr 1221=226 非 200）；S1: 种子科目 6602 名称为"折旧费用"非"管理费用"（getBudgetVsActual 返回 subjectName 须按种子名称断言）；S2: 预算对比报表聚合键要求 budget line costCenterId/projectId=null 以匹配 NORMAL 凭证行；S3: "seed id" 措辞模糊；S4: ExpenseClaim approve 后 docStatus 未经 processor 设置，须 verifyState 核实不预设。M1+S2+S3+S4 已修订：Phase 3 SETTLE 凭证金额修正为 226 + offset 公式修正；Phase 2 budget line 补 costCenterId/projectId=null 约束 + subjectId 澄清种子 id=31；Phase 1 docStatus 改为 verifyState 核实不预设。无 BLOCKER。草案审查已收敛 → `Plan Status: active`。

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。完整仓库验证在此处运行一次。

- [x] 范围内行为完成：3-4 spec 覆盖 ExpenseClaim 生命周期 + 预算控制 hook + 预算对比报表数值断言 + EmployeeAdvance 生命周期 + 报销抵扣联动
- [x] 相关文档对齐：`docs/testing/e2e-runbook.md` 业务动作表 +finance 3 行 + 套件计数更新 + webServer JVM arg 段 +expense-budget-check/budget-expense-subject-code
- [x] 已运行验证：`npx playwright test tests/e2e/business-actions/fin-expense-claim*.action.spec.ts tests/e2e/business-actions/fin-employee-advance.action.spec.ts tests/e2e/business-actions/fin-budget-vs-actual.value.spec.ts --workers=1` 全绿 + finance 域既有 spec 回归无新增失败 + `mvn clean install -DskipTests` 154 模块 BUILD SUCCESS
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### Payment (AP_PAYMENT) 预算控制 hook E2E

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: `ErpPurPayment` `useWorkflow="true"` xwf，经 2330-1 权威裁决浏览器层 submit→approve 不可达（sysUser(0) 阻塞）。Payment 预算控制 hook 实现于 `ErpPurPaymentProcessor.validateBusinessRulesForApprove` 但无法经浏览器层触发。后端单测 `TestErpPurBudgetControlIntegration` 覆盖 PO 段（Payment 段同范式）。
- Successor Required: `yes`（触发条件：useWorkflow 浏览器层身份映射落地时，或平台支持 Payment 审批轴 DIRECT 化时——同 2330-1 重评触发条件）

### 员工借款现金还款 (EMPLOYEE_ADVANCE_SETTLE 现金还款路径)

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: owner doc 设计了现金还款凭证承载路径（`ErpFinVoucher` EMPLOYEE_ADVANCE_SETTLE），但后端是否有 `cashRepay` 入口需 Explore 核实。报销抵扣路径（本计划 Phase 3）已代表验证 EMPLOYEE_ADVANCE_SETTLE 凭证。
- Successor Required: `yes`（触发条件：现金还款浏览器层入口落地时）

### 预算对比报表 AMIS 前端渲染层 DOM 断言

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划聚焦 GraphQL `getBudgetVsActual` 数值断言（数据层）。AMIS 前端渲染层 DOM 断言属 `reports/*.visual.spec.ts` 范畴（渲染层），独立 successor。
- Successor Required: `yes`（触发条件：预算对比报表 AMIS 渲染 DOM 断言需求落地时）

## Closure

Status Note: completed — all 3 Phases executed, 4 spec files (11 tests) all green, finance domain existing specs regression-free, mvn clean install -DskipTests BUILD SUCCESS. Independent closure audit passed (independent subagent, fresh session, no executor context reused).

Closure Audit Evidence:

- Auditor / Agent: independent closure auditor subagent (fresh session, did not reuse executor context)
- Audit Scope: structural checklist completeness + live-repo semantic verification (exit criteria vs live code, anti-hollow, five-point consistency, deferred honesty, docs sync)
- Live-Repo Verification: (1) 4 spec files exist under `tests/e2e/business-actions/` — `fin-expense-claim.action.spec.ts`, `fin-expense-claim-budget.action.spec.ts`, `fin-budget-vs-actual.value.spec.ts`, `fin-employee-advance.action.spec.ts` (glob confirmed); (2) seed subjects 2241(id=38 其他应付款-员工 LIABILITY/CREDIT) + 1221(id=39 其他应收款-员工预支 ASSET/DEBIT) present in `app-erp-all/src/main/resources/_vfs/_init-data/erp_md_subject.csv`; (3) webServer JVM args `-Derp-fin.expense-budget-check-enabled=true -Derp-fin.budget-expense-subject-code=6602` present in `playwright.config.ts` (correct keys, independent from PO budget-check config); (4) `docs/testing/e2e-runbook.md` updated with 3 finance business-action rows + 1 value-spec row + 4 voucher-line assertion rows + webServer JVM arg + seed COA notes; (5) `docs/logs/2026/07-14.md` detailed log entry present; (6) `docs/backlog/README.md` 1218-2 ✅ done row present.
- Anti-Hollow Check: spot-read `fin-expense-claim-budget.action.spec.ts` + `fin-budget-vs-actual.value.spec.ts` — real assertions (ERR_BUDGET_EXCEEDED expect, ControlLog WARNED persistence check, budgetAmount/actualAmount/availableAmount triple increment assertions), no empty bodies / no return-null placeholders / no swallowed exceptions.
- Five-Point Consistency: Plan Status=completed / all 3 Phase Status=completed / all Exit Criteria [x] / all Closure Gates [x] / log entry present — all agree.
- Deferred Honesty: 3 Deferred items (Payment xwf E2E / cashRepay path / AMIS DOM assertion) all classified `out-of-scope improvement` with explicit successor triggers — no live defect or contract drift hidden.
- Risk Profile: pure test + seed + config plan, zero production-code / contract / ORM-model change (per backlog README) — low-risk closure surface.
- Execution Evidence: 4 new spec files created (fin-expense-claim.action.spec.ts 4 tests / fin-expense-claim-budget.action.spec.ts 2 tests / fin-budget-vs-actual.value.spec.ts 1 test / fin-employee-advance.action.spec.ts 4 tests = 11 tests all green); seed subjects 2241+1221 added to erp_md_subject.csv; webServer JVM args +expense-budget-check-enabled=true +budget-expense-subject-code=6602 added to playwright.config.ts; existing fin-budget-control + fin-budget-scenario specs regression-free (8 tests green).

Follow-up:

- Payment 预算控制 hook E2E successor（触发条件见 Deferred，同 2330-1 xwf 阻塞）
- 现金还款路径 successor（触发条件见 Deferred）
- 预算对比报表 AMIS 渲染 DOM 断言 successor（触发条件见 Deferred）
