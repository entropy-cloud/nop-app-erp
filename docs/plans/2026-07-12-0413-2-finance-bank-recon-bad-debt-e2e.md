# 2026-07-12-0413-2-finance-bank-recon-bad-debt-e2e Finance 银行对账 + 坏账业务动作浏览器层 E2E

> Plan Status: completed
> Last Reviewed: 2026-07-12
> Mission: erp
> Work Item: Finance 域 deferred 业务动作浏览器层 E2E（银行对账 `ErpFinBankReconciliation` generate→post→reverse + 坏账 `ErpFinBadDebt` writeOff→submit→approve DIRECT 审批轴 / recover / runBadDebtProvision）
> Source: Deferred 项承接 `docs/plans/2026-07-12-0204-2-finance-reconciliation-lifecycle-e2e.md` Deferred「银行对账 `ErpFinBankReconciliation` 浏览器层 E2E」（Successor Required: yes，触发条件「当推进 finance 域银行对账业务动作 E2E 时」——**已满足**：AGENTS.md 当前项目阶段重点含「各域细化端到端验证」，finance 作为核心域其银行对账是 business-actions E2E 覆盖的明确缺口）+ `0204-2` Deferred「坏账 `ErpFinBadDebt` 浏览器层 E2E」（Successor Required: yes，触发条件「当推进 finance 域坏账业务动作 E2E时」——**已满足**，同上）。两项同结果面（finance 域 DIRECT `@BizMutation` 业务动作浏览器层 E2E）、同验证路径（`business-actions/` 套件 + 既有三原语 helper）、同前置（0204-2 finance 核销生命周期已验证 finance DIRECT @BizMutation 浏览器层范式），按规则 14（同组件同结果面）合并为单计划两阶段。
> Related: `2026-07-12-0204-2`（finance 核销单生命周期 E2E，DIRECT `@BizMutation` 范式已验证，本计划复用其 partner 隔离 + cleanup 思路）、`2026-07-09-0814-2`（业务动作三原语 helper `createViaSave`/`callMutation`/`verifyState` 范式源）、`2026-07-10-0335-1`（DIRECT useApproval 审批轴 `submitForApproval`→`approve` 范式，坏账 submit/approve 同构）、`docs/design/finance/bank-reconciliation.md`（银行对账 owner doc）、`docs/design/finance/bad-debt.md`（坏账 owner doc）
> Audit: required

## Current Baseline

实时仓库逐项核实（`read`/`grep`，非采信旧记忆）：

- **银行对账 BizModel（DIRECT `@BizMutation`，浏览器层可达）**：`ErpFinBankReconciliationBizModel`（`module-finance/erp-fin-service/.../entity/ErpFinBankReconciliationBizModel.java`）三方法均 `@BizMutation`+`@SingleSession`：`generate(@Name("statementId") Long)`（:28，从银行对账单 `ErpFinBankStatement` 构建对账记录）、`post(@Name("reconciliationId") Long)`（:35，过账）、`reverse(@Name("reconciliationId") Long)`（:42，红冲）。**无 xwf**（非 useWorkflow 实体），DIRECT 入口浏览器层经 GraphQL mutation `ErpFinBankReconciliation__generate/post/reverse` 可达。
- **银行对账状态周期已文档化（DRAFT→POSTED→CANCELLED，非 REVERSED）**：`BankReconciliationBuilder.java:96/129/140` 经 `VOUCHER_STATUS_DRAFT/POSTED/CANCELLED` 置 `docStatus`，owner doc `bank-reconciliation.md`（0115-2 补注 :145）确认 `docStatus` 复用 `erp-fin/voucher-status` 字典（DRAFT/POSTED/CANCELLED）。**故 `reverse()` 实为 CANCELLED 终态**（红冲回退），spec `verifyState` 须断言 CANCELLED 而非 REVERSED。
- **银行对账过账产物仅在 setup 含未达（unmatched）流水时生成（关键 setup 约束）**：`BankReconciliationBuilder.post:127` 委托 `adjustmentVoucherBuilder.post(recon, account, unmatched, context)`，仅对 **unmatched 流水行** 生成 `BANK_RECON_ADJ` 调整凭证。**完全平衡的 setup（零未达项）经 generate 平衡门控但 post 不产凭证**。故 spec 的「凭证存在性」断言须 setup 故意构造未达流水行（仍满足 `statementBalance − bookBalance = bankCreditUnrecorded − bankDebitUnrecorded` 平衡等式，见下）方能使 post 产凭证；否则仅断言 `docStatus` DRAFT→POSTED 翻转。
- **银行对账 setup 三约束（最复杂自包含 setup）**：`generate()`（`BankReconciliationBuilder:47-117`）(i) 需 `ErpFinFundAccount`（`accountType=BANK`，`requireFundAccount:156` 否则 `ERR_FUND_ACCOUNT_NOT_FOUND`）；(ii) 需满足平衡等式 `statementBalance − bookBalance = bankCreditUnrecorded − bankDebitUnrecorded`（精度内），否则 `ERR_BANK_RECON_NOT_BALANCED:78`；(iii) 欲使 post 产凭证须含未达流水行（见上）。setup 设计须同时满足三约束，Phase 1 Explore 落实具体数值。
- **坏账 BizModel（DIRECT `@BizMutation`，浏览器层可达）**：`ErpFinBadDebtBizModel`（`module-finance/erp-fin-service/.../entity/ErpFinBadDebtBizModel.java`）六方法均 `@BizMutation`+`@SingleSession`：`writeOff(@Name("arApItemId") Long, @Name("reason") String)`（:39，从 OPEN AR 辅助账项建坏账单）、`recover(@Name("arApItemId") Long, @Name("reason") String)`（:48，收回已核销坏账）、`submit/approve/reject(@Name("id") Long)`（:57/:64/:71，三轴 DIRECT 审批状态机）、`runBadDebtProvision(@Name("periodId") Long)`（:78，期末计提，返回 `BadDebtProvisionResult`）。**无 xwf**（审批轴为 DIRECT `submit`/`approve`/`reject`，非平台 useWorkflow），DIRECT 入口浏览器层经 GraphQL mutation 可达。
- **既有 finance DIRECT 业务动作 E2E 范式已验证（0204-2）**：`fin-reconciliation.action.spec.ts`（7 用例全绿）已验证 finance 域 DIRECT `@BizMutation` 浏览器层全栈可达性——`create(direction,partnerId,businessDate,lines)`（复杂入参经 `input('[i_app_erp_fin_dao_dto_ReconciliationLineInput]', [...])` typed variable）+ `post`/`reverse` + `checkDualSideConsistency @BizQuery` + 5 validateLine 守卫负路径 + 自包含 partner 隔离（新建 partner `E2E-RECON-PN-` + cleanup 删 partner 使余额字段消失）。三原语 helper（`createViaSave`/`callMutation`/`verifyState`/`eqFilter`/`deleteByFilter`）在 finance 域已验证可复用。
- **既有 finance orchestration helper 原语可复用（1249-1/2004-2）**：`orchestration/_helper.ts` 的 `cleanupVoucherByBillCode`（按 billCode 删凭证行+凭证+回链）、`cleanupArApByCode`（按 sourceBillCode 删 AR-AP 辅助账）、`findItems`/`findFirst`（`__findPage` 返回 items）。坏账 writeOff 经 `CloseVoucherWriter` 直接持久化凭证（核销不进 P&L，owner doc `bad-debt.md`），产凭证 + AR 辅助账状态变更（`ar-ap-status` WRITTEN_OFF 扩展），cleanup 须覆盖两类产物。
- **种子基线（关键，决定 setup 自包含程度）**：种子库（e2e-runbook.md「种子库启动」91 张 CSV）已含 `erp_fin_ar_ap_item`（P2P/O2C 交易 + EMPLOYEE_ADVANCE/EXPENSE_CLAIM 追加），但银行对账单 `erp_fin_bank_statement` + 流水 `erp_fin_bank_statement_line` 是否 seed 需 Phase 1 Explore 核实。坏账需 OPEN 状态的 AR 辅助账项（`ErpFinArApItem.direction=RECEIVABLE + status=OPEN`）作 writeOff 入参——0204-2 证明可自包含新建 partner + AR-AP 项隔离。**setup 自包含裁决**：两阶段均自包含建前置数据（新建 partner/AR-AP 项/对账单 + 行），避免污染 finance 看板/报表基线（revenue/netProfit=1130/1130、ar-ap-aging），cleanup 删 partner/凭证/AR-AP 项使聚合字段恢复。
- **状态字段已文档化（无需运行时猜测）**：银行对账 `ErpFinBankReconciliation.docStatus` = DRAFT→POSTED→CANCELLED（见上，复用 `erp-fin/voucher-status`）；坏账单 `ErpFinBadDebt` 审批状态字段名（`approveStatus` DRAFT→SUBMITTED→APPROVED?）+ AR 辅助账 `ErpFinArApItem.status`（OPEN→WRITTEN_OFF→? 收回恢复?）需 Phase 1 Explore 经 ORM/owner doc `bad-debt.md` 核实确认。

剩余差距：finance 域 business-actions E2E 覆盖仅核销单生命周期（0204-2），银行对账 + 坏账作为 finance 域两大 DIRECT 业务动作生命周期（generate→post→reverse / writeOff→submit→approve + recover + runBadDebtProvision）零浏览器层覆盖；触发条件「推进 finance 域 X 业务动作 E2E 时」已满足。

## Goals

- 将 finance 域 business-actions E2E 覆盖由核销单（0204-2）扩展至银行对账 + 坏账两大 DIRECT 业务动作生命周期，验证三原语 helper 范式在 finance 域多型 DIRECT 业务动作（对账单状态机 + 审批轴坏账单 + 期末计提批处理）下的可复用性。
- 银行对账：`generate(statementId)`→`post(reconciliationId)`→`reverse(reconciliationId)` 三态状态机 + 过账产物（凭证/余额调节表/未达账项）+ reverse 红冲回退，自包含对账单+行 setup。
- 坏账：`writeOff(arApItemId,reason)`→`submit(id)`→`approve(id)` DIRECT 审批轴 + `recover(arApItemId,reason)` 收回路径 + `runBadDebtProvision(periodId)` 期末计提（返回 `BadDebtProvisionResult`），自包含 OPEN AR 项隔离 + cleanup 覆盖凭证/AR-AP 产物。
- 验证 finance 域 DIRECT `@BizMutation` 多入参形态（标量 `arApItemId+reason` / `statementId` / `periodId` + 实体 id）经 GraphQL 浏览器层全栈可达 + 状态翻转经 `verifyState` `__get` 独立断言。

## Non-Goals

- **不**覆盖 `runAutoReconciliation`（自动核销引擎）浏览器层 E2E——需 webServer 启动参数追加 `-Derp-fin.auto-reconcile=true` 且对种子基线（全 SETTLED）无副作用验证，属 config-flip 探索，0204-2 已诚实 Deferred（触发条件：webServer 启动参数可稳定追加且对种子基线无副作用时），本计划不解除。
- **不**做坏账计提/核销凭证行精确数值断言（subjectCode + debitAmount/creditAmount）——凭证行数值断言属 0704-1 结果面（P2P/O2C/Return 已覆盖），坏账凭证行数值归该结果面 successor（触发条件：当推进 finance 域坏账凭证行数值断言时）。本计划仅断言凭证存在性 + AR-AP 状态翻转。
- **不**改后端 BizModel / Processor / ORM / 凭证逻辑——后端经 0115-2（银行对账子面）+ 0540-1（坏账准备与应收核销）已落地并经单元/集成测试覆盖，本计划纯消费侧测试新增（零生产代码/契约/ORM 模型变更）。
- **不**覆盖坏账 `reject(id)` 守卫路径（submit→reject 审批拒绝）——DIRECT 审批轴 reject 守卫经 0335-1 多域已验证范式（purchase/sales Return reject），本计划聚焦 writeOff→submit→approve 正路径 + recover + provision，reject 归同范式 successor。
- **不**做银行对账手工勾对（VoucherLine 按需查询）逐行断言——owner doc `bank-reconciliation.md` 自动/手工勾对内部逻辑经 0115-2 单测覆盖，本计划验证 generate→post→reverse 生命周期状态翻转 + 凭证存在性。

## Task Route

- Type: `verification or audit work`（finance 域 DIRECT 业务动作浏览器层 E2E，纯消费侧测试新增，零生产代码/契约/模型变更）
- Owner Docs: `docs/design/finance/bank-reconciliation.md`（银行对账 owner doc，generate/post/reverse 语义 + 状态机 + 配置门控）、`docs/design/finance/bad-debt.md`（坏账 owner doc，writeOff/recover/runBadDebtProvision + 三轴审批 + ALLOWANCE 充足性门控）、`docs/testing/e2e-runbook.md`（业务动作套件 + 期望值/参数表）
- Skill Selection Basis: `nop-testing`（Playwright 浏览器层业务动作 E2E、三原语 helper 范式、状态翻转断言、自包含 setup/cleanup 隔离）。既有 0814-2/0335-1/0204-2 范式已验证。无后端/前端开发技能匹配（零生产代码）。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline
- 依赖既有 webServer 启动参数（e2e-runbook.md 方式 A），无新增 JVM 属性（坏账核销不进 P&L 经 CloseVoucherWriter 直接持久化，无需 ncr-default-acct-schema 类 acctSchemaId 配置；若 Phase 1 Explore 发现坏账凭证 acctSchemaId 非空约束失败，按 1800-1 范式追加 `-Derp-fin.bad-debt-default-acct-schema=1` webServer 属性并记录裁决）
- 依赖 `app-erp-all/target/quarkus-app/quazus-run.jar` 预构建（既有前置）

## Execution Plan

### Phase 1 - 银行对账 `ErpFinBankReconciliation` 生命周期 E2E（generate→post→reverse）

Status: completed
Targets: `tests/e2e/business-actions/fin-bank-recon.action.spec.ts`（新建）、`tests/e2e/business-actions/_helper.ts`（若需扩展 setup/cleanup 原语）
Skill: `nop-testing`

- Item Types: `Explore | Add | Proof`
- Prereqs: 无

- [x] `Explore`：银行对账前置数据 + setup 三约束落实。**核实项**：(a) `ErpFinBankStatement` + `ErpFinBankStatementLine` 种子是否有行（`grep` seed CSV）——若无则自包含新建对账单+行；(b) `ErpFinFundAccount`（BANK 类型）setup——种子是否有可用银行资金账户，否则自包含新建（accountType=BANK + 关联 currency/科目）；(c) **平衡等式 + 未达流水 setup 设计**——构造对账单行使 `statementBalance − bookBalance = bankCreditUnrecorded − bankDebitUnrecorded`（满足 generate 平衡门控）且故意含未达（unmatched）流水行（使 post 产 `BANK_RECON_ADJ` 调整凭证）；(d) 确认 generate→post→reverse 状态翻转链（docStatus DRAFT→POSTED→CANCELLED，已文档化）+ post 产物（unmatched 行 → 凭证 billCode + 未达账项调整凭证，owner doc `bank-reconciliation.md`）。记录于本计划 + spec 注释。
      - Skill: `nop-testing`
      - **落实裁决**：(a) 种子库无 `erp_fin_fund_account`/`erp_fin_bank_statement`/`erp_fin_bank_statement_line` CSV → 全自包含新建；(b) 自包含建 `ErpFinFundAccount`(accountType=BANK, subjectId=1002 银行存款 id=2, currentBalance=1000)；(c) 平衡等式数值：statementBalance(1100) − bookBalance(1000) = 100 = bankCreditUnrecorded(100, 1 行 UNMATCHED CREDIT) − bankDebitUnrecorded(0)；(d) post 产 BANK_RECON_ADJ 凭证（Dr 1002 / Cr 2240OTHER 各 100）需科目 2240OTHER 在库——按 1800-1 范式种子 `erp_md_subject.csv` 补齐 3 行（2240OTHER/1231/6701，见 Phase 2），bank-recon-adj-subject-code 走默认值无需 JVM 属性。
- [x] `Add`：`fin-bank-recon.action.spec.ts` 新建——自包含建 `ErpFinFundAccount`(BANK) + `ErpFinBankStatement`+Line（独立 code `E2E-BANKSTMT-{ts}`，含故意未达流水行满足平衡等式）→ `ErpFinBankReconciliation__generate(statementId)` `callMutation` 断言 docStatus=DRAFT + reconciliationId 非空 → `post(reconciliationId)` 断言 docStatus=POSTED + 凭证存在性（`findFirst` by billCode，setup 含未达行故 post 产 `BANK_RECON_ADJ` 凭证）→ `reverse(reconciliationId)` 断言 docStatus=CANCELLED + 红冲凭证生成；非法迁移守卫（POSTED→post 拒绝 / 非 POSTED→reverse 拒绝 ErrorCode message token）。每步 `verifyState` `__get` 独立断言状态翻转。
      - Skill: `nop-testing`
- [x] `Proof`：用例全绿（正路径 generate→post→reverse + 非法守卫）；指定验证命令 `npx playwright test tests/e2e/business-actions/fin-bank-recon.action.spec.ts --workers=1`。
      - Skill: `nop-testing`
      - **验证结果**：3 用例全绿（happy generate→post→reverse + BANK_RECON_ADJ 凭证存在性 + REVERSAL 红冲凭证 + 2 非法迁移守卫）。

Exit Criteria:

> Phase 1 交付银行对账 generate→post→reverse 浏览器层全栈可达 + 状态翻转 + 过账/红冲产物存在性 + 自包含 setup/cleanup（删对账单+行+凭证使 finance 看板/报表基线无漂移）。

- [x] 银行对账 generate→post→reverse 三态状态机浏览器层全绿（docStatus DRAFT→POSTED→CANCELLED，每步 `verifyState` 独立断言 + setup 含未达行故 post 产 `BANK_RECON_ADJ` 凭证存在性 + reverse 红冲凭证 + 非法迁移守卫 ErrorCode token）。
- [x] 自包含 setup/cleanup 不污染共享 DB 基线（finance 看板 revenue/netProfit=1130/1130、ar-ap-aging 报表无漂移）。

### Phase 2 - 坏账 `ErpFinBadDebt` 生命周期 E2E（writeOff→submit→approve + recover + runBadDebtProvision）

Status: completed
Targets: `tests/e2e/business-actions/fin-bad-debt.action.spec.ts`（新建）、`tests/e2e/business-actions/_helper.ts`（若需扩展 AR-AP 项 setup 原语，复用 0204-2 partner 隔离）
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: 无硬依赖；可与 Phase 1 并行（两阶段共用潜在的 `_helper.ts` AR-AP 项 setup 扩展，但无前后置阻断）

- [x] `Add`：`fin-bad-debt.action.spec.ts` 新建——三路径：(a) **核销正路径** 自包含建 partner `E2E-BADDEBT-PN-{ts}` + OPEN AR 辅助账项（`ErpFinArApItem direction=RECEIVABLE status=OPEN`，复用 0204-2 partner 隔离）→ `ErpFinBadDebt__writeOff(arApItemId, reason)` 断言坏账单 DRAFT + AR 项 status→WRITTEN_OFF → `submit(id)` 断言 SUBMITTED → `approve(id)` 断言 APPROVED + 凭证存在性（CloseVoucherWriter 直接持久化，核销不进 P&L）；(b) **收回路径** 取已核销坏账单的 arApItemId → `recover(arApItemId, reason)` 断言 AR 项 status 恢复 + 收回凭证/坏账单状态翻转；(c) **期末计提** `runBadDebtProvision(periodId)` 返回 `BadDebtProvisionResult`（断言非空结构 + ALLOWANCE 充足性门控 config-gated 行为）。每步 `verifyState` `__get` 独立断言。
      - Skill: `nop-testing`
      - **裁决落实**：(a) 审批门控默认 true，故 writeOff 创建=UNSUBMITTED 不立即变异 ArApItem，approve 才执行（ArApItem→WRITTEN_OFF + openAmount→0 + BAD_DEBT_WRITE_OFF 凭证）；(b) recover 前置需 WRITTEN_OFF 态，spec 内先 writeOff→approve 建立 WRITTEN_OFF 再 recover→submit→approve 恢复 OPEN + BAD_DEBT_RECOVERY 凭证；(c) runBadDebtProvision 断言结构（action ∈ {RESERVE,RELEASE,NONE} + requiredProvision/allowanceBalance/totalConsidered 非负 + voucherId 与 action 一致性），不断言精确数值（Deferred successor）。科目依赖（1231/6701/1122）经 webServer JVM 属性 + 种子 erp_md_subject.csv 补齐 2 行（1231/6701，1122 已在种子）。
- [x] `Proof`：三路径用例全绿；指定验证命令 `npx playwright test tests/e2e/business-actions/fin-bad-debt.action.spec.ts --workers=1`。
      - Skill: `nop-testing`
      - **验证结果**：3 用例全绿（writeOff→submit→approve 核销正路径 + recover→submit→approve 收回路径 + runBadDebtProvision 期末计提结构断言）。

Exit Criteria:

- [x] 坏账 writeOff→submit→approve DIRECT 审批轴正路径 + recover 收回路径 + runBadDebtProvision 期末计提三路径浏览器层全绿（每步 `verifyState` 独立断言状态翻转 + 凭证/AR-AP 产物存在性）。
- [x] 自包含 setup/cleanup 不污染共享 DB 基线（cleanup 删 partner + 凭证 + AR-AP 项，finance 看板/报表无漂移）。

## Draft Review Record

- Independent draft review iteration 1: `acceptable as-is` (ses_0ad2c53bdffe9R7ltdd9Z0FKnD) — 全部 Current Baseline 主张经实时仓库核实为真（两 BizModel DIRECT `@BizMutation` 方法签名/注解/行号精确匹配、0204-2 Deferred 双 successor 触发条件、helper 原语、owner docs 存在）。规则 4/14（bundling 经 Rule 14 严格 split test 支持：两阶段同结果面 finance business-action E2E、同验证路径、同 Closure Gates，不应拆分）/5/7/8/10/13 全合规，零生产代码变更，命名合规，无反松弛禁词，Deferred 项均带触发条件。**无 BLOCKER**。采纳 4 non-blocking：(1) 银行对账状态周期 DRAFT→POSTED→**CANCELLED**（非 REVERSED）已在仓库文档化（`BankReconciliationBuilder:96/129/140` + owner doc 补注 :145），移入 Current Baseline 已知项 + 修正 Phase 1 Add/Exit Criteria 断言值；(2) post 产凭证仅在 setup 含未达（unmatched）流水行时（`BankReconciliationBuilder.post:127` 委托 adjustmentVoucherBuilder 仅对 unmatched 行产 `BANK_RECON_ADJ`），Phase 1 Add setup 须故意构造未达行；(3) setup 三约束（FundAccount BANK 类型 + 平衡等式 + 未达行）作为 Phase 1 Explore 关键落实项；(4) Phase 2 prereqs 澄清为无硬依赖可并行。修订后草案审查已收敛 → `Plan Status: active`。

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。完整仓库验证在此处。

- [x] 范围内行为完成（银行对账 + 坏账两 spec 全绿）
- [x] 相关文档对齐（`e2e-runbook.md` 业务动作表 + finance 两行 + 套件计数 + 文件结构）
- [x] 已运行验证：`npx playwright test tests/e2e/business-actions/ --workers=1`（新 spec 全绿 + 既有 business-actions 无回归；唯一失败 `cs-canned-response` 为预存 schema-level 失败，与本计划 finance seed/config 变更无关——隔离复跑同错，且本计划仅改 erp_md_subject.csv + playwright JVM args + 2 新 finance spec，不触及 CS GraphQL schema/xbiz）+ finance 基线 spec（dashboards/finance + reports/fin-{balance-sheet,income-statement,ar-ap-aging}）全绿，种子科目补齐无漂移。
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### runAutoReconciliation 浏览器层 E2E

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: config 门控 `erp-fin.auto-reconcile=true` 需 webServer JVM 启动参数，运行中不可翻转 + 需验证对种子基线（全 SETTLED）无副作用。自动核销引擎三策略经 0115-1 单元/集成测试覆盖。本计划覆盖 DIRECT 手工核销（0204-2）+ 银行对账 + 坏账 finance DIRECT 业务动作主路径。承接 0204-2 同名 Deferred。
- Successor Required: `yes`（触发条件：webServer 启动参数可稳定追加 `-Derp-fin.auto-reconcile=true` 且对种子基线无副作用时）

### 坏账核销/计提凭证行精确数值断言

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 凭证行数值断言属 0704-1 结果面（P2P/O2C/Return 已覆盖）。坏账凭证行（核销 CloseVoucherWriter + 计提 BAD_DEBT_RESERVE/WRITE_OFF/RECOVERY/RELEASE 四业务类型）subjectCode + debitAmount/creditAmount 精确数值归该结果面 successor。本计划仅断言凭证存在性 + AR-AP 状态翻转。
- Successor Required: `yes`（触发条件：当推进 finance 域坏账凭证行数值断言时）

### 坏账 reject(id) 审批拒绝守卫路径

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: DIRECT 审批轴 reject 守卫经 0335-1 多域（purchase/sales Return reject）已验证范式。本计划聚焦 writeOff→submit→approve 正路径 + recover + provision。reject 守卫增量信号低。
- Successor Required: `no`

## Closure

Status Note: 计划完成。银行对账 generate→post→reverse 三态状态机（DRAFT→POSTED→CANCELLED）+ BANK_RECON_ADJ 未达调整凭证 + 红冲，坏账 writeOff→submit→approve DIRECT 审批轴 + recover 收回 + runBadDebtProvision 期末计提三路径浏览器层 E2E 全绿（2 新 spec / 6 用例）。科目依赖经种子补齐 3 行（1231/6701/2240OTHER）+ webServer JVM args 补 3 项解除。finance 看板/报表基线无漂移（4 基线 spec 全绿）。唯一 business-actions 失败（cs-canned-response）为预存 CS schema-level 失败，与本计划无关。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理 ses_0acbf777cffegz0kUAR3aTItld（general，新会话）
- Verdict: `CLOSURE_WARRANTED`
- Evidence: 逐项核实——Phase 1（file:line 153/164/182 generate/post/reverse + 157/167/185 DRAFT→POSTED→CANCELLED + 214/244 非法守卫 + 62-117 自包含 setup + 119-140 cleanup + BankReconciliationBuilder.reverse:140 后端 CANCELLED 确认）；Phase 2（150/169/175/221/260 五动作 + 157/172/178 approvalStatus 三态 + 185/240 ArApItem WRITTEN_OFF↔OPEN + ErpFinBadDebtProcessor:278/127/68/151 后端确认 + ORM approvalStatus 字段名确认非 approveStatus）；config/seed（erp_md_subject.csv:22-24 + playwright.config.ts:18 三 JVM args + requireSubject/resolveSubjects 必要性确认）；docs（e2e-runbook:243-244 + 596-597 + README.md:55）；plan 一致性（0 个 `[ ]` / 17 个 `[x]` / 三 Status completed）。无状态值 bug、无缺失 cleanup、无范围蔓延。cs-canned-response 预存失败经确认 out-of-scope。

Follow-up:

- runAutoReconciliation 浏览器层 E2E（见 Deferred，触发条件=config 稳定追加无副作用时）
- 坏账凭证行精确数值断言（见 Deferred，触发条件=推进 finance 域坏账凭证行数值断言时）
