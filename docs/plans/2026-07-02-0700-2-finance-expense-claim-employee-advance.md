# 2026-07-02-0700-2 finance-expense-claim-employee-advance

> Plan Status: completed
> Last Reviewed: 2026-07-02
> Source: `docs/backlog/core-business-roadmap.md` 工作项 1.8（费用报销/票据/资金模块业务逻辑——本计划覆盖「费用报销 + 员工借款」子面，票据/资金属同工作项的另一结果表面，归后续计划）；补齐 `projects/cost-collection.md` 引用为成本归集来源但无 owner 实体的 P0 缺口
> Related: `2026-07-02-0300-3-ar-ap-settlement-subledger.md`（ErpFinArApItem/ErpFinReconciliation 核销机制复用）、`2026-07-01-2030-1-posting-engine-voucher-facade-processor.md`（过账 Provider 基础设施）、`2026-07-02-0300-2-sales-invoice-receipt-bizmodel.md`（过账派发器范式）
> Mission: erp
> Work Item: 费用报销单 + 员工借款 BizModel + 业财过账 + 借款抵扣核销（1.8 子面）
> Audit: required

## Current Baseline

实时仓库逐行核实的事实：

- **目标实体不存在**：全仓 `rg "ErpFinExpenseClaim|ErpFinEmployeeAdvance"` 在 `*.orm.xml` 零命中。`module-finance/model/app-erp-finance.orm.xml` 现有 27 实体（Voucher/VoucherLine/VoucherTemplate(+Line)/VoucherBillR/AccountingPeriod(+Status)/ArApItem/Reconciliation(+Line)/GlBalance/TrialBalance/FundAccount/BankStatement(+Line)/BankReconciliation(+Line) 等），**无费用报销/员工借款实体**。设计 `expense-claim.md` 的实体为「建议命名，待 ORM 计划落地」。
- **员工/组织主数据**：`ErpMdEmployee` 存在（`module-master-data/model/app-erp-master-data.orm.xml:645`，职员），claimantId/employeeId 可 FK。**`ErpMdDepartment` 不存在**（全仓零命中）；部门维度须复用 `ErpMdOrganization`（组织实体兼作部门，:409 已被多处 to-one 引用）——与 `expense-claim.md` 假设的「部门实体」存在 owner-doc 偏离（见 Task Route Decision）。
- **员工→往来单位解析（关键，不可兼容模式）**：`ErpMdEmployee.partnerId`（`master-data.orm.xml:659`，「对应内部往来单位」）**可空**（无 mandatory），FK → `ErpMdPartner`（:672）；而 `ErpFinArApItem.partnerId`(`finance.orm.xml:379`) 与 `ErpFinReconciliation.partnerId`(:425) 均 **`mandatory=true`** FK → `ErpMdPartner`。故员工辅助账/核销单的 partnerId 必须 = **已解析的 `employee.partnerId`**（即 ErpMdPartner.id），**非 employee.id**（员工与 partner 是不同 id 空间——与 supplierId/customerId 本身即 partnerId 的现有模式不同）；员工无 partner 记录时辅助账生成必失败（mandatory FK 违约），须审核前置校验（见 Phase 2 + Task Route Decision）。
- **无通用 finance 付款实体**：全仓 `rg "ErpFinPayment"` 零命中；付款为域级（purchase 域 `ErpPurPayment`、sales 域 `ErpSalReceipt`，见 0300-1/0300-2）。故 `expense-claim.md`「还款复用 `ErpFinPayment(partyType=EMPLOYEE)`」的前提不成立——现金还款借款无现成通用付款单可复用（见 Non-Goal）。
- **业财业务类型枚举**：`ErpFinBusinessType`（`erp-fin-dao/.../ErpFinBusinessType.java`）现 15 常量，max=`SALES_RETURN(150)`。新增 `EXPENSE_CLAIM(160)`/`EMPLOYEE_ADVANCE(170)`/`EMPLOYEE_ADVANCE_SETTLE(180)`（码段连续无冲突）。
- **过账基础设施**（0811-1/2030-1/0300-2 已落地，逐一核实）：入口 facade `IErpFinVoucherBiz.post`/`reverse`；`IErpFinAcctDocProvider` SPI + `ErpFinAcctDocRegistry`；共享执行器模式（executor 无 `@Transactional`，如 `SalPostingExecutor`）。新 Provider 注册 `app-service.beans.xml`，产 facts 调 `IErpFinVoucherBiz.post`。PostingEvent.billData 为契约载体。
- **辅助账/核销**（0300-3 已落地，机制逐行核实）：`ErpFinArApItem`（应收应付明细账）+ `ErpFinReconciliation`(+Line)（核销单，create/post/reverse + 5 项约束）存在；`ErpFinArApItemGenerator.resolveProfile`（switch on businessType）当前识别 AP_INVOICE/AR_INVOICE/PAYMENT/RECEIPT/PURCHASE_RETURN/SALES_RETURN，**default→null**；方向常量 `DIRECTION_RECEIVABLE=10`/`DIRECTION_PAYABLE=20`；`resolvePartnerId` 读 `SUPPLIER_ID`/`CUSTOMER_ID` billData 键（**不识别 EMPLOYEE_ID**）；`PartnerBalanceUpdater.sumOpen` 按 `direction` 过滤 SUM openAmountFunctional。sourceBillType 常量 `SOURCE_BILL_*` 现含 SAL_RETURN/PUR_RETURN 等。
- **预算控制不存在**：全仓 `rg "IErpFinBudgetControlBiz"` 零命中；`ErpFinBudget*` 实体在 ORM 零命中。`budget.md` 的预算控制为设计未落地。故 `expense-claim.md`「APPROVED 前同步调 `IErpFinBudgetControlBiz.check`」的前提不成立（见 Non-Goal，配置门控预留钩子不实现）。
- **projects 成本归集**：`projects/cost-collection.md` 引用费用报销作为成本来源但无 owner 实体（P0 缺口）；projects CRUD 已完成（Milestone 2），报销行 `projectId` 可 FK 到 `ErpPrjProject`，归集聚合属 projects 域（extended 2.6）。
- **剩余差距**：(1) 无报销单/借款单实体与状态机；(2) 无 EXPENSE_CLAIM/EMPLOYEE_ADVANCE 过账类型与凭证；(3) 辅助账不识别员工方/员工业务类型；(4) 无报销抵扣借款核销；(5) 无 projects 成本归集的 owner 实体（P0）。

## Goals

- ORM 加性新增 `ErpFinExpenseClaim`(头) + `ErpFinExpenseClaimLine`(行) + `ErpFinEmployeeAdvance`(借款) 三实体（finance 源模型，重新 codegen），含多币种四件套 + 价税分离三件套 + 业财三件套(posted) + 三轴状态(docStatus/approveStatus)。
- 报销单 `IErpFinExpenseClaimBiz` 三轴审批状态机（submit/withdrawSubmit/approve/reject/reverseApprove/cancel），对齐 `expense-claim.md §状态机` 与全局三轴范式（复用 1132-2/0300-2 已建立的 finance 域审批形状）。
- 借款单 `IErpFinEmployeeAdvanceBiz` 三轴审批状态机（同形）。
- 报销审核触发 **EXPENSE_CLAIM(160)** 过账（借费用科目(按行)/借进项税/贷应付-员工(own_account) 或 银行存款(company_account)，paymentMode 决定贷方），`posted=true`；红字冲减走 reverse。
- 借款审核触发 **EMPLOYEE_ADVANCE(170)** 过账（借其他应收款-员工预支/贷银行存款），`posted=true`；红字冲减走 reverse。
- 辅助账扩展：EXPENSE_CLAIM → `DIRECTION_PAYABLE`（应付-员工，正 openAmount）+ `SOURCE_BILL_EXPENSE_CLAIM`；EMPLOYEE_ADVANCE → `DIRECTION_RECEIVABLE`（其他应收-员工预支，正 openAmount）+ `SOURCE_BILL_EMPLOYEE_ADVANCE`；`resolvePartnerId` 新增 `EMPLOYEE_ID` 解析。员工余额按 direction 各自分桶。
- **报销抵扣借款核销**：报销审核时若报销人存在未还借款且 `erp-fin.advance-auto-offset-on-expense=true`，复用 `ErpFinReconciliation`（0300-3）核销 EMPLOYEE_ADVANCE 应收 vs EXPENSE_CLAIM 应付（净额抵扣），不足部分留作应付-员工。EMPLOYEE_ADVANCE_SETTLE(180) 过账记净额清算凭证（借应付-员工/贷其他应收-员工预支）。
- 报销行 `projectId`/`costCenterId` 存储为成本归集维度（消除 P0 悬空引用——projects 域后续读取）。
- 行为测试覆盖状态机/价税分离/paymentMode 贷方路由/过账/辅助账双方向/抵扣核销/红字冲减。

## Non-Goals

- **票据/资金（treasury）**：承兑汇票/贴现/授信/现金预测（`treasury.md`）属 1.8 的另一结果表面，归后续独立计划（实体规模 5+/凭证类型 7，证据强度 ⚪ 开源零覆盖，风险更高，单独审计更稳）。
- **预算控制（`IErpFinBudgetControlBiz.check`）**：预算模块未落地（无实体/无 biz）。本计划预留配置门控钩子点（`erp-fin.expense-budget-check-enabled`，默认 false，不实现校验逻辑），预算模块落地后再接（Follow-up）。
- **现金还款借款（`ErpFinPayment(partyType=EMPLOYEE)`）**：无通用 finance 付款实体（付款为域级 ErpPurPayment/ErpSalReceipt）。本计划借款清算仅做「报销抵扣」净额核销 + GL 过账；现金还款/银行转账还款的付款指令属资金面（需新增通用付款机制），列为 Follow-up。
- **projects 成本归集聚合**：本计划只让报销行**存储** projectId/costCenterId（消除悬空引用）；按项目/成本中心归集汇总属 projects 域（extended 2.6）。
- **多级审批人路由（nop-wf）**：审核 = 直接状态迁移 + `@BizMutation`（对齐全仓基线）。
- **备用金定期补足/定额管理自动化**：备用金作为 `advanceType=IMPREST` 特化，定期补足提醒/定额校验属运营自动化（nop-job），本计划只落 IMPREST 类型与基础生命周期。
- **多币种借款汇兑损益**：借款按业务日汇率记账，跨期汇率差异归期末汇兑面（EXCHANGE_GAIN_LOSS，0300-3 Deferred）。
- **红冲后预算回滚联动**：预算未落地，无联动对象。

## Task Route

- Type: `implementation-only change`（业务逻辑 + ORM 模型加性增量）。**注**：触及 ask-first 保护区域 `module-finance/model/app-erp-finance.orm.xml`（新增 3 实体 + 新字典）与 `erp-fin-dao`/`erp-fin-meta`（枚举/字典扩展，`ErpFinBusinessType` 注释明确为可扩展门面，非保护区域）。此为本业务逻辑阶段首次在 finance 源模型新增实体——按规则 1 从实时基线起，Phase 1 含 codegen 回归。
- Owner Docs: `docs/design/finance/expense-claim.md`（实体/状态机/过账/抵扣）、`docs/design/finance/posting.md`（Provider 三层 + 冲销）、`docs/design/finance/ar-ap-reconciliation.md`（核销机制复用约束）、`docs/design/flow-overview.md §3`（状态映射）、`docs/design/projects/cost-collection.md`（成本归集来源引用）。
- Skill Selection Basis: 全为 ORM 实体增量 + BizModel 审批 + 过账 Provider + 跨实体辅助账/核销 → 加载 `nop-backend-dev`（覆盖 IBiz、跨实体访问、过账 Provider、CodeGen 增量回归自检）。
- **Decision（部门维度）**：**选择** `departmentId` → `ErpMdOrganization`（组织实体兼作部门，无独立部门实体）。**替代**：① 新增 `ErpMdDepartment` master-data 实体（超范围 + master-data 源模型变更）；② 删除部门维度（设计明确需要）。**残留风险**：组织与部门语义混同，列为 Follow-up（触发条件：需部门级成本中心细分时新增部门实体）。owner-doc 偏离在 Phase 1 补注 `expense-claim.md`。
- **Decision（借款清算机制）**：**选择**「报销抵扣净额核销（复用 ErpFinReconciliation）+ EMPLOYEE_ADVANCE_SETTLE GL 过账」，不做现金还款付款单。**替代**：① 新增通用 finance 付款实体支持员工还款（超范围，rejected）；② 纯 GL 过账不做子账核销（员工借款余额不可查，rejected——辅助账可查是 0300-3 已建立的基线）。**残留风险**：现金/银行转账还款场景暂不支持（Non-Goal，资金面 Follow-up）。
- **Decision（员工→partnerId 解析）**：**选择** PostingEvent.billData 的 `EMPLOYEE_ID` 键携带**已解析的 `employee.partnerId`**（派发器从 `ErpMdEmployee` 读 partnerId 填入 billData），`resolvePartnerId` 增 `EMPLOYEE_ID` 分支直接采用（不二次反查）。**替代**：① billData 传 employee.id 由生成器反查 employee.partnerId（finance→master-data 反向依赖，破坏「finance 为 DAG 顶、生成器只读 billData」约束，rejected）；② 扩 ArApItem 加 employeeId 列绕过 partnerId（破坏 0300-3 统一 partner 模型，rejected）。**残留风险**：员工无 partner 记录则辅助账生成失败——由 Phase 2 审核前置校验强制 `claimant/employee.partnerId` 非空兜底（缺失抛 `NopException`）。

## Infrastructure And Config Prereqs

- 配置项（`expense-claim.md §配置项`）：`erp-fin.advance-auto-offset-on-expense`（默认 true，报销时自动抵扣同员工未还借款）、`erp-fin.expense-budget-check-enabled`（默认 false，**钩子预留不实现**——预算模块未落地）、`erp-fin.expense-approval-required`(默认 true)、`erp-fin.expense-reason-required`(默认 true)。经 `AppConfig.var(..., defaultValue)` 读取，缺失走默认，无 .env。
- 模块依赖：`erp-fin-service` 已 compile 依赖 master-data-dao（员工/组织主数据）；projects-dao compile 已存在（projectId FK）。无新增端口/密钥/外部服务/数据迁移（新实体为新建表）。

## Execution Plan

### Phase 1 — ORM 实体增量（报销单/行 + 借款单）+ 字典 + 枚举 + 重新 codegen + 回归

Status: completed
Targets: `module-finance/model/app-erp-finance.orm.xml`（新增 3 实体 + 新字典 `<dict>`）、`erp-fin-dao/.../ErpFinBusinessType.java`（扩 160/170/180）、经 codegen 重新生成 dao entity/meta/`_app.orm.xml`
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: 无。

- [x] `Add`：`ErpFinExpenseClaim`（头）—— code/orgId/claimantId(→ErpMdEmployee)/departmentId(→ErpMdOrganization)/businessDate/paymentMode(dict erp-fin/expense-payment-mode: OWN_ACCOUNT=10/COMPANY_ACCOUNT=20)/多币种四件套(currencyId/exchangeRate/amountSource/amountFunctional)/价税(amountWithoutTax/taxAmount/amountWithTax)/settleAdvanceId(可空)/docStatus(dict erp-fin/expense-claim-status)/approveStatus(共用 erp-fin/approve-status)/posted+postedBy+postedAt + 标准审计。
  - Skill: `nop-backend-dev`
- [x] `Add`：`ErpFinExpenseClaimLine`（行）—— claimId/lineNo/expenseType(dict erp-fin/expense-type)/projectId(→ErpPrjProject,可空)/costCenterId(可空)/subjectId/subjectCode/amountWithoutTax/taxRate/taxAmount/amountWithTax + 标准审计。
  - Skill: `nop-backend-dev`
- [x] `Add`：`ErpFinEmployeeAdvance`（借款）—— code/orgId/employeeId(→ErpMdEmployee)/advanceType(dict erp-fin/advance-type: EXPENSE_ADVANCE=10/IMPREST=20/TRAVEL=30)/businessDate/多币种四件套/settledAmount(派生)/outstandingAmount(派生)/projectId(可空)/docStatus(dict erp-fin/advance-status)/approveStatus/posted+postedBy+postedAt + 标准审计。
  - Skill: `nop-backend-dev`
- [x] `Add`：新字典 `erp-fin/expense-payment-mode`、`erp-fin/expense-type`、`erp-fin/advance-type`、`erp-fin/expense-claim-status`、`erp-fin/advance-status`（ORM `<dict>` 或 meta dict 源，按现仓字典生成范式）。
  - Skill: `nop-backend-dev`
- [x] `Add`：`ErpFinBusinessType` 追加 `EXPENSE_CLAIM(160)`/`EMPLOYEE_ADVANCE(170)`/`EMPLOYEE_ADVANCE_SETTLE(180)`；`business-type.dict.yaml`（由 ORM `<dict>` 生成）同步。
  - Skill: `nop-backend-dev`
- [x] `Decision`：部门维度 `departmentId` → `ErpMdOrganization`（理由见 Task Route Decision）；在 `expense-claim.md` 补注偏离。
  - Skill: none
- [x] `Proof`：重新 codegen 后，finance 既有 CRUD + 0300-3 辅助账/核销套件全绿（加性新实体不破坏既有）；本地化 `mvn test -pl module-finance/erp-fin-service -am`。
  - Skill: `nop-backend-dev`

Exit Criteria:

> Phase 1 交付 3 新实体 + 字典 + 枚举 + codegen 无回归（既有 finance 套件绿）。解除 Phase 2/3 对实体与业务类型的阻塞。

- [x] 三实体 + 字典 + 枚举落库（codegen 产物更新）；既有 finance 测试无回归

### Phase 2 — 报销单/借款单三轴审批状态机 + 报销抵扣借款核销 + 配置

Status: completed
Targets: `module-finance/erp-fin-service/.../entity/ErpFinExpenseClaimBizModel.java`(新)、`.../entity/ErpFinEmployeeAdvanceBizModel.java`(新)、`.../biz/IErpFinExpenseClaimBiz.java`/`IErpFinEmployeeAdvanceBiz.java`(新)、`.../ErpFinErrors.java`(扩)、`.../ErpFinConstants.java`(扩 SOURCE_BILL_* + EMPLOYEE_ID 键)、`app-service.beans.xml`
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: Phase 1（实体/字典/枚举存在）。

- [x] `Add`：`IErpFinExpenseClaimBiz`/`IErpFinEmployeeAdvanceBiz` 声明三轴审批契约 submit/withdrawSubmit/approve/reject/reverseApprove/cancel（`@BizMutation`+`@Name`，对齐 finance 域 IBiz 签名形状）。
  - Skill: `nop-backend-dev`
- [x] `Add`：两 BizModel 实现审批状态机（UNSUBMITTED↔SUBMITTED→APPROVED/REJECTED；reverseApprove APPROVED→REJECTED；cancel 非终态→CANCELLED，APPROVED 须先红冲）。`@BizMutation` 自动包装事务（不叠加 `@Transactional`），每迁移校验前置态，违例抛 `NopException`+`ErpFinErrors` 作用域码。
  - Skill: `nop-backend-dev`
- [x] `Add`：报销审核前置校验——claimant 启用 + **`claimant.partnerId` 非空**（员工须有内部往来单位记录，否则辅助账 mandatory FK 违约——见 Task Route Decision）、行非空、价税合计=Σ行、`reason`/`expenseType` 必填（按配置）、paymentMode 合法；预算钩子点（`erp-fin.expense-budget-check-enabled`，默认 false，**不实现校验**，仅留注入位）。
  - Skill: `nop-backend-dev`
- [x] `Add`：借款审核前置校验——employee 启用 + **`employee.partnerId` 非空**、金额>0、advanceType 合法；`settledAmount`/`outstandingAmount` 派生（=Σ核销/未核销）。
  - Skill: `nop-backend-dev`
- [x] `Add`：**报销抵扣借款核销**——报销 APPROVED 时若 `erp-fin.advance-auto-offset-on-expense=true` 且报销人存在未还借款（outstanding>0），调 `IErpFinReconciliationBiz` 建 EMPLOYEE_ADVANCE(应收) vs EXPENSE_CLAIM(应付) 核销单（净额=min(借款 outstanding, 报销应付)，0300-3 核销机制），不足部分留作应付-员工。核销发生在过账生成辅助账之后（见 Phase 3）。
  - Skill: `nop-backend-dev`
- [x] `Decision`：抵扣核销的时序——**选择**「过账生成辅助账 → 抵扣核销（同事务）」，保证辅助账 open item 可被核销引用。**替代**：异步核销（最终一致但抵扣不及时，rejected）。**残留风险**：核销与过账同事务增加事务时长（低频，可接受）。
  - Skill: none
- [x] `Proof`：`TestErpFinExpenseClaimApproval`/`TestErpFinEmployeeAdvanceApproval`（三轴迁移正向/反向/非法迁移；前置校验拒绝）。`mvn test -pl module-finance/erp-fin-service -am -Dtest=TestErpFinExpenseClaim*,TestErpFinEmployeeAdvance*`。
  - Skill: `nop-backend-dev`

Exit Criteria:

> Phase 2 交付两单三轴审批状态机 + 抵扣核销编排（不含过账凭证，Phase 3）。解除 Phase 3 对状态机/核销时序的阻塞。

- [x] 两单三轴迁移 + 前置校验单测通过；抵扣核销编排就位（待辅助账）

> 实现补注：抵扣核销经 `AdvanceOffsetOrchestrator` 实现。实测 0300-3 `ErpFinReconciliationBizModel.validateLine` 强制核销双方同 direction，而员工抵扣是 RECEIVABLE(借款) vs PAYABLE(报销) 跨方向净额，无法经 `ErpFinReconciliation` 同方向校验。为保持纯加性（不触动 0300-3 不变量），编排器**复用 open-item 逐笔核销算术（ReconciliationSettler 模式）直接回写双方辅助账**，不经 ErpFinReconciliation 头；EMPLOYEE_ADVANCE_SETTLE 净额清算凭证承载 GL 侧。每张报销单抵扣单笔最旧未还借款（保证抵扣可逆）。

### Phase 3 — 过账（3 业务类型 + Provider + 派发器）+ 辅助账扩展（员工方双方向）+ 端到端

Status: completed
Targets: `module-finance/erp-fin-service/.../posting/ExpenseClaimAcctDocProvider.java`(新)、`.../posting/EmployeeAdvanceAcctDocProvider.java`(新)、`.../posting/ExpenseClaimPostingDispatcher.java`(新)/`EmployeeAdvancePostingDispatcher.java`(新)（复用共享 executor）、`.../ErpFinArApItemGenerator.java`(扩 resolveProfile + resolvePartnerId EMPLOYEE_ID)、`erp-fin-service/.../_vfs/erp/fin/beans/app-service.beans.xml`、`ErpFinConstants.java`(扩 SOURCE_BILL_EXPENSE_CLAIM/SOURCE_BILL_EMPLOYEE_ADVANCE)
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: Phase 2（状态机/抵扣核销编排）；过账基础设施（0811-1/2030-1）+ 辅助账（0300-3）已完成。

- [x] `Add`：`ExpenseClaimAcctDocProvider` 产 EXPENSE_CLAIM facts——按行借费用科目(subjectId)/借进项税(taxAmount)；贷方按 paymentMode：own_account→应付-员工科目，company_account→银行存款科目。金额=amountFunctional（本位币）。注册 beans。
  - Skill: `nop-backend-dev`
- [x] `Add`：`EmployeeAdvanceAcctDocProvider` 产 EMPLOYEE_ADVANCE facts——借其他应收款-员工预支科目/贷银行存款科目，金额=票面。注册 beans。
  - Skill: `nop-backend-dev`
- [x] `Add`：`ExpenseClaimPostingDispatcher`/`EmployeeAdvancePostingDispatcher`（复用共享 executor，无 `@Transactional`；dispatcher 置于 `erp-fin-service`——finance 拥有这些实体，与 Sal/Pur dispatcher 在域审核边界触发不同，本派发器在 finance 自有审核边界触发）——APPROVED 后组装 PostingEvent(businessType, billHeadCode, billData 含 `EMPLOYEE_ID`(**= 已解析的 employee.partnerId**，见 Task Route Decision) + TOTAL_AMOUNT_WITH_TAX/TOTAL + DEPARTMENT_ID + 行级费用科目/项目维度 + paymentMode + orgId + acctSchemaId) 调 `IErpFinVoucherBiz.post`；成功置 posted=true，失败吞异常保持 APPROVED+posted=false（对齐 0300-2 合约）。EMPLOYEE_ADVANCE_SETTLE 由抵扣核销 post 触发净额清算凭证（借应付-员工/贷其他应收-员工预支）。
  - Skill: `nop-backend-dev`
- [x] `Add`：扩展 `ErpFinArApItemGenerator.resolveProfile`——`case EXPENSE_CLAIM` → `new SourceProfile(DIRECTION_PAYABLE, SOURCE_BILL_EXPENSE_CLAIM)`；`case EMPLOYEE_ADVANCE` → `new SourceProfile(DIRECTION_RECEIVABLE, SOURCE_BILL_EMPLOYEE_ADVANCE)`；`case EMPLOYEE_ADVANCE_SETTLE` → null（清算不生成新辅助账，由核销单驱动 open item 状态）。`resolvePartnerId` 新增 `EMPLOYEE_ID` 键分支**直接采用**（billData 已携带解析后的 employee.partnerId，生成器不反查 master-data）。`ErpFinConstants` 扩两 SOURCE_BILL_* 常量。**约束**：finance 为 DAG 顶，生成器只读 billData；扩展为纯加性（既有 AP/AR/PAYMENT/RECEIPT/PUR_RETURN/SAL_RETURN case 不动），purchase/sales billData 不携带 EMPLOYEE_ID，无回归（与 0300-1/0300-2 兼容）。
- [x] `Decision`：员工辅助账余额语义——**选择** direction 分桶（PAYABLE=应付-员工 / RECEIVABLE=其他应收-员工预支），员工余额 = RECEIVABLE.outstanding − PAYABLE.outstanding（净额）。**替代**：单一员工余额表（破坏 0300-3 direction 模型，rejected）。**残留风险**：员工净余额需两方向相减查询，列为 Follow-up（触发条件：员工借款/报销余额报表需物化净额时）。
  - Skill: none
- [x] `Add`：`reverseApprove`/`cancel` 对已 posted=true 单据——调 `IErpFinVoucherBiz.reverse(code, businessType, ctx)` 红字冲销；`cancelOnReverse` 取消对应辅助账 open item；已抵扣情形回滚核销单（reverse）。
  - Skill: `nop-backend-dev`
- [x] `Proof`：`TestErpFinExpenseClaimPosting`（APPROVED→EXPENSE_CLAIM 凭证 借费用/进项税/贷方按 paymentMode + DIRECTION_PAYABLE 辅助账(partnerId=employee.partnerId) + 员工应付余额；reverseApprove→红冲 + 辅助账 CANCELLED）、`TestErpFinEmployeeAdvancePosting`（APPROVED→EMPLOYEE_ADVANCE 凭证 借其他应收/贷银行 + DIRECTION_RECEIVABLE 辅助账 + 员工预支余额；reverseApprove→红冲）、`TestErpFinExpenseOffsetAdvance`（报销抵扣借款：净额核销 + EMPLOYEE_ADVANCE_SETTLE 清算凭证 + 借款 outstanding 下降 + 应付-员工下降）、`TestErpFinPartnerIdResolution`（claimant/employee.partnerId 为空时审核被拒抛 NopException；billData EMPLOYEE_ID 为 employee.partnerId 非 employee.id）、端到端（借款→部分报销抵扣→剩余应付）。`mvn test -pl module-finance/erp-fin-service -am -Dtest=TestErpFinExpense*,TestErpFinEmployeeAdvance*,TestErpFinPartnerId*`。
  - Skill: `nop-backend-dev`

Exit Criteria:

> Phase 3 交付过账端到端（审核→凭证+辅助账双方向）+ 抵扣核销清算 + 红字冲减。完整仓库验证属 Closure Gates。

- [x] EXPENSE_CLAIM/EMPLOYEE_ADVANCE 凭证落库 + posted=true；辅助账 PAYABLE/RECEIVABLE 双方向 + 员工余额；抵扣核销净额 + EMPLOYEE_ADVANCE_SETTLE 清算凭证；reverseApprove→红冲 + 辅助账 CANCELLED + 核销回滚
- [x] 端到端（借款→报销抵扣→剩余应付）单测全绿

## Draft Review Record

- Independent draft review iteration 1: **needs revision**（`ses_0e019ff71ffei351VHlFEaKax0`，独立 general 子代理，新会话）— 全部 Current Baseline 主张经实时仓库逐行核实属实（无 ErpFinExpenseClaim/ErpFinEmployeeAdvance、ErpMdEmployee 存在/ErpMdDepartment 不存在、无 ErpFinPayment、ErpFinBusinessType max=150、ArApItem/Reconciliation 存在 + resolveProfile/resolvePartnerId/direction 机制、预算模块未落地、过账基础设施齐全）。DAG 安全主张确认：resolvePartnerId 增 EMPLOYEE_ID 为纯加性 fallback，purchase/sales billData 不携带 EMPLOYEE_ID，对 0300-1/0300-2 无回归。1 BLOCKER（B1）：员工→partnerId 解析未定义——`ErpMdEmployee.partnerId` 可空而 `ArApItem/Reconciliation.partnerId` mandatory，billData 的 EMPLOYEE_ID 须 = employee.partnerId（非 employee.id），且员工无 partner 记录时辅助账生成失败。**已修订**：Current Baseline 补员工→partnerId 不可兼容模式事实；Task Route 增「员工→partnerId 解析」Decision（选择派发器填解析后 partnerId + 生成器直接采用，列替代/残留风险）；Phase 2 审核前置校验增 claimant/employee.partnerId 非空；Phase 3 dispatcher/generator/Proof 对齐。非阻塞 S 级 nit（dispatcher 置 finance 自有域、design-doc 漂移补注）已吸收。**共识达成**：Plan Status 升级为 `active`。

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。结束时运行一次完整仓库验证。

- [x] 范围内行为完成：报销单/借款单三轴状态机 + EXPENSE_CLAIM/EMPLOYEE_ADVANCE/EMPLOYEE_ADVANCE_SETTLE 过账 + 辅助账双方向 + 抵扣核销 + 红字冲减，行为测试通过
- [x] 相关文档对齐：`core-business-roadmap.md` 1.8 标注进展（费用/借款子面）；当日日志已记；`expense-claim.md` 偏离（departmentId→ErpMdOrganization、预算钩子预留、现金还款 Non-Goal）补注
- [x] 已运行验证：`mvn test -pl module-finance/erp-fin-service -am` 全绿（55 测试）；根 `mvn clean install -DskipTests` + `mvn test -fae` 无回归
- [x] 无范围内项目降级为 deferred/follow-up（treasury/预算控制/现金还款/成本归集聚合/nop-wf/备用金自动化/汇兑损益均为计划内 Non-Goal）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：Plan Status、各 Phase Status、Exit Criteria、Closure Gates、日志一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于 `Closure` 节

## Deferred But Adjudicated

### 部门实体（ErpMdDepartment）缺失，departmentId 复用 ErpMdOrganization

- Classification: `watch-only residual`
- Why Not Blocking Closure: 组织实体兼作部门，存储与 FK 正常；组织/部门语义混同为设计简化。
- Successor Required: yes（触发条件：需部门级成本中心细分时新增部门实体）

### 现金/银行转账还款借款（通用 finance 付款实体）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 无通用 finance 付款实体（付款为域级）；本计划借款清算仅做报销抵扣净额核销 + GL 过账。
- Successor Required: yes（触发条件：实施通用资金付款机制/treasury 资金面时）

### 预算控制（IErpFinBudgetControlBiz.check）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 预算模块未落地（无实体/无 biz）；本计划预留配置门控钩子点不实现校验。
- Successor Required: yes（触发条件：预算模块落地后接线校验）

### projects 成本归集聚合

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划只让报销行存储 projectId/costCenterId（消除 P0 悬空引用）；归集汇总属 projects 域（extended 2.6）。
- Successor Required: yes（触发条件：实施 2.6 projects 成本归集时）

### 员工净余额物化报表

- Classification: `optimization candidate`
- Why Not Blocking Closure: 员工净余额 = RECEIVABLE.outstanding − PAYABLE.outstanding 实时两方向相减查询可得。
- Successor Required: yes（触发条件：员工借款/报销余额报表需高频查询/物化净额时）

## Closure

Status Note: 已执行全部 3 阶段（Phase 1 ORM 实体增量 + 字典 + 枚举 + codegen；Phase 2 三轴审批状态机 + 前置校验 + 抵扣核销编排；Phase 3 过账 Provider/派发器 + 辅助账双方向扩展 + 报销抵扣借款净额核销 + 红字冲减）。验证全绿（finance-service 55 测试 + 全仓库 `mvn clean install -DskipTests` + `mvn test -fae` 无回归）。独立结束审计已通过。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，非执行者上下文），担任独立 closure auditor。
- Verdict: passes closure audit（无 BLOCKER/MAJOR）。
- Evidence:
  - 结构与文本一致性：`Plan Status: completed`、三 Phase `Status: completed`、各 Phase Exit Criteria 全 `[x]`、Closure Gates 全 `[x]`、`docs/logs/2026/07-02.md` 0700-2 条目一致。
  - 实时仓库逐项核实（Exit Criteria vs live repo）：① 3 实体落地——`module-finance/model/app-erp-finance.orm.xml:776/822/855` + 生成产物 `erp-fin-dao/_app.orm.xml:1498/1617/1697` + `ErpFinExpenseClaim`/`ErpFinExpenseClaimLine`/`ErpFinEmployeeAdvance` entity/`_gen` 存在；② `ErpFinBusinessType` 160/170/180（`erp-fin-dao/ErpFinBusinessType.java:28-30`）；③ 5 字典 + business-type 扩；④ BizModel/IBiz 落地（`ErpFinExpenseClaimBizModel`/`IErpFinExpenseClaimBiz`、`ErpFinEmployeeAdvanceBizModel`/`IErpFinEmployeeAdvanceBiz`）；⑤ 过账组件 `ExpenseClaimAcctDocProvider`/`EmployeeAdvanceAcctDocProvider`/`FinPostingExecutor`/`ExpenseClaimPostingDispatcher`/`EmployeeAdvancePostingDispatcher`/`AdvanceOffsetOrchestrator` 在 `app-service.beans.xml:43-62` 全注册。
  - Anti-Hollow（接线核实，非仅注入）：`ErpFinExpenseClaimBizModel`/`ErpFinEmployeeAdvanceBizModel` 实调 `postingDispatcher.tryPost`/`reverse`、`offsetOrchestrator.offset`/`reverseOffset`；`ErpFinArApItemGenerator` 实现 EXPENSE_CLAIM→PAYABLE / EMPLOYEE_ADVANCE→RECEIVABLE / EMPLOYEE_ADVANCE_SETTLE→null 三 case + `resolvePartnerId` 的 EMPLOYEE_ID 分支（`BILL_DATA_EMPLOYEE_ID` 直接采用，不反查 master-data，finance DAG 顶约束保持）。
  - Deferred honesty：treasury/预算控制/现金还款/成本归集聚合/净余额物化报表均为计划内 Non-Goal 或 watch-only residual，无 in-scope 缺陷隐藏其中，后继触发条件明确。
  - 实现偏离（抵扣核销不经 ErpFinReconciliation 头而经 AdvanceOffsetOrchestrator 直写辅助账、公司直付跳过员工应付辅助账）已在 plan Phase 2/3 + `docs/logs/2026/07-02.md` 记录并含替代分析，符合规则 10「检查清单完整性」与规则 11「文本一致性」。
  - 既有证据:
  - 新增实体：`ErpFinExpenseClaim`/`ErpFinExpenseClaimLine`/`ErpFinEmployeeAdvance`（finance ORM + codegen 产物）
  - 新增字典：`erp-fin/approve-status`、`expense-payment-mode`、`expense-type`、`advance-type`、`expense-claim-status`、`advance-status`；`business-type` 扩 160/170/180
  - 新增 BizModel/IBiz：`ErpFinExpenseClaimBizModel`/`IErpFinExpenseClaimBiz`、`ErpFinEmployeeAdvanceBizModel`/`IErpFinEmployeeAdvanceBiz`（三轴审批状态机 + 前置校验）
  - 新增过账组件：`ExpenseClaimAcctDocProvider`、`EmployeeAdvanceAcctDocProvider`、`FinPostingExecutor`、`ExpenseClaimPostingDispatcher`、`EmployeeAdvancePostingDispatcher`、`AdvanceOffsetOrchestrator`
  - 扩展：`ErpFinArApItemGenerator`（resolveProfile + EMPLOYEE_ID 解析 + 公司直付跳过辅助账）、`ErpFinConstants`/`ErpFinErrors`（作用域码）、`app-service.beans.xml`（6 bean 注册）
  - 测试：`TestErpFinExpenseClaimApproval`(8)、`TestErpFinEmployeeAdvanceApproval`(7)、`TestErpFinExpenseClaimPosting`(3)、`TestErpFinEmployeeAdvancePosting`(2)、`TestErpFinExpenseOffsetAdvance`(2)、`TestErpFinPartnerIdResolution`(3) = 25 新测试，finance-service 合计 55 测试全绿
  - 验证命令：`mvn test -pl module-finance/erp-fin-service -am`（55 绿）、`mvn clean install -DskipTests`（146 模块 + app-erp-all 绿）、`mvn test -fae`（全仓库无回归）

实现偏离补注（已在代码 javadoc + Phase 2 Exit 记录）：

1. **抵扣核销机制**：plan 原文「复用 ErpFinReconciliation」。实测 0300-3 `ErpFinReconciliationBizModel.validateLine` 强制核销双方同 direction，而员工抵扣是 RECEIVABLE(借款) vs PAYABLE(报销) 跨方向净额，无法经 ErpFinReconciliation 同方向校验。为保持纯加性（不触动 0300-3 不变量），`AdvanceOffsetOrchestrator` 复用 open-item 逐笔核销算术（ReconciliationSettler 模式）直接回写双方辅助账，不经 ErpFinReconciliation 头；EMPLOYEE_ADVANCE_SETTLE 净额清算凭证承载 GL 侧。每张报销单抵扣单笔最旧未还借款（保证抵扣可逆）。
2. **公司直付辅助账**：`paymentMode=COMPANY_ACCOUNT` 的报销贷银行存款，`ErpFinArApItemGenerator` 跳过员工应付辅助账生成（不挂虚增应付-员工）。

Follow-up:

- 部门实体（见上方 Deferred）
- 现金还款借款（见上方 Deferred，通用付款机制/treasury 时）
- 预算控制接线（见上方 Deferred，预算模块落地时）
- projects 成本归集聚合（见上方 Deferred，2.6 时）
- 员工净余额物化报表（见上方 Deferred）
- 备用金定期补足自动化（触发条件：nop-job 接线时）
