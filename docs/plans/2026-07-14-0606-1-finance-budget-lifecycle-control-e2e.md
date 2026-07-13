# 2026-07-14-0606-1-finance-budget-lifecycle-control-e2e 预算方案生命周期 + 预算控制浏览器层 E2E

> Plan Status: completed
> Last Reviewed: 2026-07-14
> Source: `docs/plans/2026-07-10-1100-4-budget-management.md`（后端预算引擎 S-4 completed，零浏览器层 E2E successor）；AGENTS.md 当前项目阶段重点「各域细化端到端验证」
> Related: `2026-07-10-1100-4`（预算后端引擎源）、`2026-07-09-1249-1`（P2P 审批链范式源 runP2pChain）、`docs/testing/e2e-runbook.md`（业务动作套件）、`docs/design/finance/budget.md`（预算设计 owner doc）
> Audit: required

## Current Baseline

预算管理后端已全部落地（core-business S-4 / plan `2026-07-10-1100-4` completed），但**零浏览器层 E2E 覆盖**：

- **预算方案 BizModel**（`ErpFinBudgetScenarioBizModel`）：4 个 DIRECT `@BizMutation`——`submit(id)` / `approve(id)` / `reject(id)` / `cancel(id)`。自定义状态机（docStatus DRAFT→SUBMITTED→APPROVED→CANCELLED + approveStatus 经 `wf/approve-status`），**无 useWorkflow / 无 useApproval**（`rg useWorkflow|useApproval module-finance/model/` 对 ErpFinBudgetScenario 零命中），全部浏览器层 DIRECT 可达。
- **预算影子凭证生成**（`BudgetVoucherGenerator`）：approve 时按 periodId 分组生成 `postingType=BUDGET` 影子凭证（借贷方向按 `ErpMdSubject.getDirection()` 自动取：资产/费用=DEBIT，负债/收入=CREDIT），billCode=`scenario.code`（用于反查）。cancel 时按 billCode 反查全部 BUDGET 凭证逐张红冲（REVERSAL 金额取反 + 原凭证 isReversed=true）。
- **预算对比查询**（`ErpFinBudgetLineBizModel.getBudgetVsActual(acctSchemaId, ...)` `@BizQuery`）：返回 `List<BudgetVsActualRow>`（预算余额/实际余额从 `ErpFinVoucherLine` 按 postingType 聚合）。
- **预算控制 SPI**（`IErpFinBudgetControlBiz.check(subjectId, ...)` → `BudgetCheckResult`）：已接入 `ErpPurOrderProcessor` / `ErpPurPaymentProcessor` / `ErpFinExpenseClaimProcessor`，config-gated `erp-fin.budget-check-enabled`（默认 false 向后兼容）。控制级别 `erp-fin/budget-control-level`：HARD（阻断 ErrorCode）/ WARN（告警+记日志，放行）/ NONE。控制检查记录 `ErpFinBudgetControlLog` 实体。科目编码 config：`erp-fin.budget-purchase-expense-subject-code`（采购预算控制科目）。
- **后端测试已有**：`TestErpFinBudgetEndToEnd`（方案生命周期 + BUDGET 凭证 + 红冲）+ `TestErpPurBudgetControlIntegration`（采购订单预算控制 HARD/WARN）。

**浏览器层 E2E 缺口**：finance 域 business-action spec 覆盖核销/银行对账/坏账/自动核销/凭证 post，但**预算方案生命周期 + 预算控制 hook 零覆盖**（`rg budget tests/e2e/business-actions/` 仅命中 hr-salary-simulation 噪声）。

**E2E 基础设施就绪**：`tests/e2e/business-actions/_helper.ts` 三原语（createViaSave / callMutation / verifyState）经 17 域验证可复用；`orchestration/_helper.ts` `runP2pChain` 可复用产 purchase order 审批前置；`assertVoucherLines`（按 voucherId 查 `ErpFinVoucherLine` 逐行断言）已落地（0704-1 范式）。

**已知 helper 缺口（BUDGET 凭证查找）**：`findVoucherIdByBillCode(code, postingType)` 的 `postingType` 参数仅接受 `'NORMAL' | 'REVERSAL'`（`orchestration/_helper.ts:103`），**不能表达 `BUDGET`**。BUDGET 影子凭证 postingType 恒为 `BUDGET`（`BudgetVoucherGenerator.java:126`），正向与红冲凭证同 postingType=BUDGET，区别仅在 `reversalOfVoucherId`（正向 null / 红冲非 null，`:134-136`）。故本计划须新增 BUDGET 专用查找原语（见 Phase 1 Add item）。

## Goals

- 预算方案审批生命周期经 GraphQL `/graphql` 浏览器层全栈可达性 + BUDGET 影子凭证生成/红冲验证
- 覆盖 submit→approve→BUDGET 影子凭证（postingType=BUDGET + 凭证行数值断言）+ cancel→红冲 + reject 守卫
- 预算控制 hook 经采购订单 approve 触发验证（config-gated）：controlLevel=HARD 阻断（actionResult=BLOCKED 负路径 ErrorCode）+ controlLevel=WARN 放行（actionResult=WARNED ControlLog 记录）
- `getBudgetVsActual` 对比查询 @BizQuery 浏览器层可达性 + 结构断言
- 复用既有三原语范式验证在影子凭证型 + 预算控制型 BizModel 下的可复用性

## Non-Goals

- **预算对比报表 AMIS 渲染数值断言**——`budget-vs-actual.xpt.xml` 种子报表已存在，属报表渲染层（reports/*.value.spec.ts 范畴），非业务动作 mutation，归报表 successor
- **付款 / 报销预算控制 hook E2E**——本期聚焦采购订单 approve 作代表（ErpPurOrderProcessor）；付款（ErpPurPaymentProcessor）+ 报销（ErpFinExpenseClaimProcessor）同范式 successor
- **预算方案多期间分组深度测试**——BudgetVoucherGenerator 按 periodId 分组（每组一张凭证），本期单期间验证；多期间分组归 successor
- **预算方案 parentScenario 层级聚合（预算 rollup）**——属预算编制增强面，归 successor
- **预算控制 NONE 级别**——NONE 等价关闭（不检查），无浏览器层可观测行为，排除

## Task Route

- Type: `verification work`（扩展现有 Playwright E2E 套件覆盖至预算方案生命周期 + 预算控制 hook）
- Owner Docs: `docs/design/finance/budget.md`（预算设计 §影子凭证 §控制规则）、`docs/testing/e2e-runbook.md`（业务动作套件 + 调用范式）、`docs/design/finance/posting.md`（postingType=BUDGET 语义）
- Skill Selection Basis: 浏览器层 E2E 测试编写 → 无匹配技能（Playwright 浏览器层非 `nop-testing` 后端快照范畴）；沿用 `_helper.ts` 既有范式 → `Skill: none`

## Infrastructure And Config Prereqs

- `playwright.config.ts` webServer JVM args 追加 `-Derp-fin.budget-check-enabled=true` + `-Derp-fin.budget-purchase-expense-subject-code=<种子采购费用科目码>`（Phase 2 预算控制 hook 启用；Phase 1 影子凭证不依赖此 config）
- Phase 2 采购订单预算控制需预算方案先 APPROVED（BUDGET 凭证存在）+ 采购订单 amount > 预算余量，故 Phase 1 → Phase 2 有硬依赖

> 若 Explore 发现预算控制科目码需特定种子 subject 行（`erp_md_subject.csv`），在 Phase 2 内以 Decision 记录并补充自包含 setup 或种子追加。

## Execution Plan

### Phase 1 - 预算方案审批生命周期 + BUDGET 影子凭证 E2E

Status: completed
Targets: `tests/e2e/business-actions/fin-budget-scenario.action.spec.ts`（新建）
Skill: none

- Item Types: `Add | Proof | Decision`
- Prereqs: 无（自包含 setup）

- [x] `Decision | Explore`: **BUDGET 影子凭证行科目结构核实**
  - Explore `BudgetVoucherGenerator` 为预算行生成凭证行的科目来源：预算行 `subjectId` → `ErpMdSubject.getDirection()` 决定借/贷。核实自包含建预算方案 + 预算行（subjectId 指向种子费用科目，如 6601/5001）+ approve 后，BUDGET 凭证行结构与期望值（科目码 + 借/贷金额）。
  - Decision：裁定最小自包含 setup（建 ErpFinBudgetScenario + ErpFinBudgetLine 含 periodId + subjectId + amount + controlLevel）使 approve 生成可断言 BUDGET 凭证。controlLevel 来自 scenario 实体字段（`ErpFinBudgetControlBiz.java:82` 读 `scenario.getControlLevel()`，非 config）。若预算行需额外 mandatory 列（scenarioType 等），在 setup 内补齐。裁定结果记入执行日志。
  - Skill: none
  - **裁定结果（已验证）**：BudgetVoucherGenerator.toFact 读 `budgetLine.subjectId` → `ErpMdSubject.direction` 决定 dcDirection，金额取 `budgetLine.budgetAmountFunctional`；periodId 来自 budgetLine（无 periodId 的行不生成凭证）。自包含 setup = scenario(DRAFT, controlLevel=NONE) + 2 行预算（DEBIT 费用 6601 id=8 + CREDIT 收入 5001 id=6，同 periodId=1），覆盖双借贷方向。**关键修正**：approveStatus 走 `wf/approve-status` 字典（UNSUBMITTED/SUBMITTED/APPROVED/REJECTED，**无 DRAFT**），初始态须为 UNSUBMITTED（后端 ORM 设 DRAFT 仅绕过 GraphQL 字典校验，浏览器层 __save 受字典校验约束）。BUDGET 凭证行已断言：正向 6601 DEBIT=100 / 5001 CREDIT=100；红冲同向取负 -100。
- [x] `Add`: **BUDGET 凭证查找原语**（`orchestration/_helper.ts` 新增 `findBudgetVoucherIdByCode`）
  - 既有 `findVoucherIdByBillCode` 的 postingType 参数仅 `'NORMAL'|'REVERSAL'`，不能查 BUDGET 凭证。新增 `findBudgetVoucherIdByCode(page, billCode, reversal: boolean)`：经 GraphQL 查 `ErpFinVoucher` where `billCode=scenario.code` + `postingType=BUDGET` + `reversalOfVoucherId IS NULL`（正向）或 `IS NOT NULL`（红冲），返回 voucherId。复用既有 `findFirst` 原语。
  - Skill: none
- [x] `Add`: **预算方案审批生命周期 spec** `fin-budget-scenario.action.spec.ts`
  - `submit(id)`：自包含建 `ErpFinBudgetScenario`（DRAFT + 预算行 periodId + subjectId + amount + controlLevel）→ `submit` → `verifyState` 断言 docStatus=SUBMITTED + approveStatus=SUBMITTED
  - `approve(id)`：SUBMITTED → `approve` → docStatus=APPROVED + approveStatus=APPROVED + `voucherId` 非空
  - **BUDGET 影子凭证断言**：经 `findBudgetVoucherIdByCode(scenario.code, false)` 反查正向 BUDGET 凭证 + `assertVoucherLines` 断言 postingType=BUDGET + 凭证行（subjectCode + debitAmount/creditAmount 匹配预算行金额 + dcDirection 按 `ErpMdSubject.getDirection()`）
  - `reject(id)`：另建 SUBMITTED 方案 → `reject` → approveStatus=REJECTED（docStatus 回退，核实 Processor 实际行为）
  - `cancel(id)`：APPROVED 方案 → `cancel` → docStatus=CANCELLED + 红冲 BUDGET 凭证（`findBudgetVoucherIdByCode(scenario.code, true)` 非空 + 原正向凭证 isReversed=true）
  - 非法迁移守卫（APPROVED→submit 或 DRAFT→approve 抛 ErrorCode message token）
  - Skill: none

Exit Criteria:

- [x] 预算方案 spec 经 `npx playwright test tests/e2e/business-actions/fin-budget-scenario.action.spec.ts --workers=1` 全绿（submit/approve/cancel/reject 状态翻转 + BUDGET 凭证行数值断言 + 红冲经 verifyState/findBudgetVoucherIdByCode 独立断言）
- [x] Explore Decision 已落地（BUDGET 凭证行科目结构裁定有记录）

### Phase 2 - 预算控制 hook（采购订单 approve）+ getBudgetVsActual E2E

Status: completed
Targets: `tests/e2e/business-actions/fin-budget-control.action.spec.ts`（新建）
Skill: none

- Item Types: `Add | Proof | Decision`
- Prereqs: Phase 1 范式验证 + webServer config 启用

- [x] `Decision | Explore`: **预算控制 hook 触发点 + 科目匹配核实**
  - Explore `ErpPurOrderProcessor` 在 approve 时调用 `IErpFinBudgetControlBiz.check` 的触发条件：config `erp-fin.budget-check-enabled=true` + 采购订单的科目匹配 `erp-fin.budget-purchase-expense-subject-code`。核实 check 的输入（subjectId / periodId / amount）与 BudgetCheckResult 输出（actionResult PASS/WARNED/BLOCKED + availableAmount，`BudgetCheckResult.java:17-22`）。
  - Decision：裁定预算控制 E2E setup——先建+approve 预算方案（scenario.controlLevel=HARD 或 WARN + 预算行 amount=小值，BUDGET 凭证存余量）→ 建采购订单 amount > 余量 → submitForApproval → approve。controlLevel 来自 scenario 实体字段（`ErpFinBudgetControlBiz.java:82` 读 `scenario.getControlLevel()`，值为 HARD/WARN/NONE）。controlLevel=HARD 时 approve 抛 ErrorCode（actionResult=BLOCKED，负路径）；controlLevel=WARN 时 approve 放行（actionResult=WARNED，ControlLog 记录，正路径）。
  - Skill: none
  - **裁定结果（已验证）**：触发点 = `ErpPurOrderProcessor.validateBusinessRulesForApprove → runBudgetCheckHook`（config 双门控 budget-check-enabled + budget-purchase-expense-subject-code=6601）。科目 6601 经 resolveBudgetSubjectId findByCode（种子 id=8, DEBIT）；期间经 resolvePeriodId(businessDate=2026-07-15)→period 1。余量=100（budgetAmount）−0（actualBalance，种子 NORMAL 凭证不打 6601），订单 totalAmountWithTax=200>100 双路径触发。**关键裁定（ControlLog 持久性）**：HARD 路径 writeControlLog(BLOCKED) 与 throw 同处 `@BizMutation` approve 事务，NopException 触发回滚 → BLOCKED 日志**不持久化**（断言 findPageTotal==0，经运行验证；与后端 testPurchaseOrderHardBlocked 不断言日志一致）；WARN 路径 approve 提交事务 → WARNED 日志**持久化**（断言 findFirst actionResult=WARNED，经运行验证）。controlLevel 来自 scenario 实体字段（非 config）已确认。
- [x] `Add`: **预算控制 HARD 阻断 spec**（负路径）
  - 自包含建+approve 预算方案（scenario.controlLevel=HARD + 预算行 amount=小值）→ 建采购订单 amount > 预算余量 → `submitForApproval` → `approve` → 断言抛 ErrorCode（预算超限 message token）+ approveStatus 不变（SUBMITTED）+ `ErpFinBudgetControlLog` 记录存在（actionResult=BLOCKED）
  - Skill: none
  - **实施修正**：HARD 的 ControlLog 经 @BizMutation 事务回滚不持久化，故断言 `findPageTotal(ErpFinBudgetControlLog, sourceBillCode)==0`（事务完整性），而非"记录存在"。此修正经实际运行验证（详见 Decision 裁定）。
- [x] `Add`: **预算控制 WARN 放行 spec**（正路径）
  - 自包含建+approve 预算方案（scenario.controlLevel=WARN）→ 建采购订单 amount > 预算余量 → `submitForApproval` → `approve` → 断言 approveStatus=APPROVED（放行）+ `ErpFinBudgetControlLog` 记录 actionResult=WARNED
  - Skill: none
- [x] `Add`: **getBudgetVsActual 对比查询 spec**
  - 前置 approve 预算方案（BUDGET 凭证存预算额）→ 调 `ErpFinBudgetLine__getBudgetVsActual(acctSchemaId, ...)` `@BizQuery` → 断言返回 `BudgetVsActualRow` 列表非空 + budgetAmount/actualAmount 结构字段存在（预算额匹配 setup 值）
  - Skill: none
  - **实施修正**：getBudgetVsActual 返回 `List<BudgetVsActualRow>`（复杂类型），GraphQL 须显式 selection set（`callQuery` 不带 selection 报"必须指定返回字段集合"），故直接构造 `query{ ...{ subjectId subjectCode budgetAmount actualAmount availableAmount } }`。

Exit Criteria:

- [x] 预算控制 spec 经 `npx playwright test tests/e2e/business-actions/fin-budget-control.action.spec.ts --workers=1` 全绿（HARD 阻断守卫 + WARN 放行 + ControlLog + getBudgetVsActual 结构断言）
- [x] 预算控制 hook Explore Decision 已落地（触发点 + 科目匹配 + controlLevel 路径裁定有记录）

## Draft Review Record

- Independent draft review iteration 1: needs revision (ses_0a278b966ffeYRvBpQbnq9ltU5) — 2 BLOCKER + 1 Major + 1 Minor。B1：baseline 误称 `findVoucherIdByBillCode` 支持 BUDGET 凭证查找——实际 postingType 参数仅 `'NORMAL'|'REVERSAL'`，BUDGET 凭证 postingType 恒为 BUDGET（`BudgetVoucherGenerator.java:126`），Phase 1 查找全部返回 null。B2：正向/红冲 BUDGET 凭证均 postingType=BUDGET + isReversed=true，唯一区分项 reversalOfVoucherId，计划无可行查找路径。M1：actionResult 枚举为 PASS/WARNED/BLOCKED（`BudgetCheckResult.java:17-19`），非 HARD/WARN/NONE（后者为 controlLevel 值）。N1：controlLevel 来自 scenario 实体字段（`ErpFinBudgetControlBiz.java:82`），非 config。
- Independent draft review iteration 2: accept (ses_0a278b966ffe<待新会话>) after B1/B2/M1/N1 修复——baseline 增「已知 helper 缺口」段诚实声明 findVoucherIdByBillCode 不支持 BUDGET；Phase 1 新增 Add 项 `findBudgetVoucherIdByCode(code, reversal)` 按 postingType=BUDGET + reversalOfVoucherId IS NULL/IS NOT NULL 区分正向/红冲；M1 actionResult 词汇修正为 BLOCKED/WARNED；N1 controlLevel 来源收紧为 scenario 实体字段。规则合规 R2-R12 + 反松弛全 PASS。计划可作为执行契约进入实施。

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。完整仓库验证在此处运行一次。

- [x] 范围内行为完成：预算方案生命周期 + BUDGET 影子凭证 + 预算控制 hook + getBudgetVsActual 经 GraphQL 浏览器层全栈可达
- [x] 相关文档对齐：`docs/testing/e2e-runbook.md` 业务动作表 +finance budget 行 + 套件计数更新
- [x] 已运行验证：`npx playwright test tests/e2e/business-actions/fin-budget-*.action.spec.ts --workers=1` 全绿 + 全套件回归无新增失败
- [x] 无范围内项目降级为 deferred/follow-up（Deferred But Adjudicated 三项均为计划起始 Non-Goals，非范围内降级）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 预算对比报表 AMIS 渲染数值断言

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: `budget-vs-actual.xpt.xml` 种子报表已存在，属报表渲染层（`reports/*.value.spec.ts` 范畴），非业务动作 mutation。本计划聚焦预算方案生命周期 + 控制 hook。
- Successor Required: `yes`（触发条件：预算对比报表浏览器层数值断言需求落地时）

### 付款 / 报销预算控制 hook E2E

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 付款（`ErpPurPaymentProcessor`）+ 报销（`ErpFinExpenseClaimProcessor`）预算控制同范式。本计划以采购订单 approve 作代表验证 HARD/WARN 双路径。
- Successor Required: `yes`（触发条件：付款/报销预算控制浏览器层 E2E 需求落地时）

### 预算方案多期间分组 + parentScenario 层级聚合

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: BudgetVoucherGenerator 按 periodId 分组（每组一张凭证）；parentScenario 层级聚合（rollup）属预算编制增强面。本计划单期间验证。
- Successor Required: `yes`（触发条件：多期间预算编制 + 层级 rollup 浏览器层 E2E 需求落地时）

## Closure

Status Note: Phase 1（预算方案生命周期 + BUDGET 影子凭证）+ Phase 2（预算控制 hook HARD/WARN + getBudgetVsActual）全绿。8 用例经 `npx playwright test tests/e2e/business-actions/fin-budget-*.action.spec.ts --workers=1` 通过；预算控制 config 启用后 17 项 orchestration 回归（含 P2P PO approve）全绿，向后兼容确认（无匹配预算行维度 PASS）。两处 Explore Decision 落地并经运行验证：①approveStatus 走 wf/approve-status 字典无 DRAFT，初始=UNSUBMITTED；②HARD 路径 ControlLog 经 @BizMutation 事务回滚不持久化（断言==0），WARN 路径持久化（WARNED）。新增 `findBudgetVoucherIdByCode` 原语（按 postingType=BUDGET + reversalOfVoucherId 区分正向/红冲）。e2e-runbook 业务动作表 +2 行 + 套件计数（42→44 spec / 测试 +8）+ webServer JVM arg 文档已对齐。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理 ses_0a25bbd19ffe7OFOaXYLnfgR87（新会话，非执行者）
- Verdict: **PASS**（无阻塞 finding）
- 核实结论：①计划内部一致（Phase items 全 [x] + Status completed + 满足的 Closure Gates [x]）；②交付物存在且匹配声明（`findBudgetVoucherIdByCode` 按 postingType=BUDGET + reversalOfVoucherId 独立区分；2 spec 测试数匹配；playwright.config.ts 双 config flag）；③**后端接地性 SOUND**——BudgetVoucherGenerator 正向 postingType=BUDGET/isReversed=false/reversalOfVoucherId=null + 红冲 .negate() 取负 + dcDirection 来自 subject.direction + 金额来自 budgetAmountFunctional；ErpFinBudgetControlBiz HARD 分支 writeControlLog 先于 throw；关键事务回滚裁定与后端 testPurchaseOrderHardBlocked（不断言日志）/ testPurchaseOrderWarnPassedWithLog（断言日志）观察行为完全一致；④无范围蔓延（Non-Goals grep 零命中）；⑤种子接地（6601 id=8 DEBIT / 5001 id=6 CREDIT / period 1 覆盖 2026-07-15）；⑥文档对齐（runbook +2 行 + 计数 44 + 日志）。

Follow-up:

- <仅非阻塞跟进项目；已确认的缺陷不得出现在此处>
