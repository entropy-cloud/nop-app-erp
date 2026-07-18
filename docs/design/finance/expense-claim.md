# 费用报销 / 员工借款 / 备用金（Expense Claim & Employee Advance）

## 目的

设计员工费用报销、员工借款（预支）与备用金的业务语义、状态机、业财过账与跨域协作。补齐 `projects` 文档引用为成本归集来源但无 owner 实体的 P0 缺口（见 `docs/analysis/2026-06-30-0000-advanced-scenario-gap-analysis.md`）。

## 边界

- 本模块负责：员工费用报销（含价税分离、项目/成本中心归集）、员工借款（预支/差旅/备用金）、借款还款与核销。
- 本模块不负责：员工主数据（master-data 域）；银行账户与对账（`bank-reconciliation.md`）；采购费用（purchase 域）。
- 实体为**建议命名，待 ORM 计划落地**（`model/app-erp-finance.orm.xml` 是 ask-first 保护区域，本文件不复述 schema）。

## 实现偏离补注（2026-07-02 计划 `2026-07-02-0700-2` 落地）

落地时相对上文「设计依据」与「跨域协作」的偏离，已在此记录（plan Task Route Decision + Non-Goals）：

1. **部门维度 `departmentId` → `ErpMdOrganization`**：本仓无独立部门实体（全仓无 `ErpMdDepartment`），部门维度复用组织实体 `ErpMdOrganization`（组织实体兼作部门）。残留风险：组织/部门语义混同，列为 Follow-up（需部门级成本中心细分时新增部门实体）。
2. **预算控制钩子预留不实现**：`跨域协作` 表中「APPROVED 前同步调 `IErpFinBudgetControlBiz.check`」的前提（预算模块）未落地（无实体/无 biz）。本计划仅预留配置门控钩子点 `erp-fin.expense-budget-check-enabled`（默认 false，**不实现校验逻辑**），预算模块落地后再接。
3. **现金/银行转账还款为 Non-Goal**：`设计依据 §5`/`跨域协作` 中「还款复用 `ErpFinPayment(partyType=EMPLOYEE)`」的前提不成立——本仓无通用 finance 付款实体（付款为域级 `ErpPurPayment`/`ErpSalReceipt`）。借款清算仅做「报销抵扣」净额核销（复用 `ErpFinReconciliation`）+ `EMPLOYEE_ADVANCE_SETTLE` GL 过账；现金还款/银行转账还款付款指令属资金面 Follow-up。
4. **员工→partnerId 解析**：员工辅助账/核销单的 `partnerId` = **已解析的 `ErpMdEmployee.partnerId`**（即 `ErpMdPartner.id`），**非 `employee.id`**（员工与 partner 是不同 id 空间）。员工无 partner 记录时审核被拒（前置校验强制 `partnerId` 非空）。


## 实现偏离补注（2026-07-04 补充：现金还款 Deferred 解除）

前述补注 #3「现金/银行转账还款为 Non-Goal」的 Deferred 条件**已解除**：

- **解除依据**：`docs/analysis/erp-survey/2026-07-04-0000-frappe-hrms.md` 实测 frappe/hrms 的 Employee Advance 用 Journal Entry（凭证）承载现金还款（`make_return_entry`：借 银行存款 / 贷 advance_account），**不依赖独立 Payment 实体**。
- **本项目落地方案**：现金还款直接生成 `ErpFinVoucher`（`EMPLOYEE_ADVANCE_SETTLE` businessType），经业财过账引擎 + 业财回链承载，**无需新建通用 finance 付款实体**。`EMPLOYEE_ADVANCE_SETTLE` 的借贷方向（§业财过账表）已预留"借：银行存款（现金还款）/ 贷：其他应收款-员工预支"。
- **设计依据 #5 演进**：原"还款复用 `ErpFinPayment`"前提不成立（本仓无该实体），演进出**凭证承载**方案（参 hrms），见下文 §现金还款。
- **未解除项**：薪资扣回作为第三还款路径仍为 Follow-up（依赖 HR `Additional Salary` 机制，触发条件：HR 薪酬扣回项落地时）。

## 设计依据

> 来源 `docs/analysis/2026-06-30-0001-advanced-scenario-design-comparison.md` §1.1。

### 核心设计点

1. **paymentMode 决定对方科目**：`own_account`（员工垫付）→ 应付-员工；`company_account`（公司直付）→ 银行待结算（🟢 Odoo `hr_expense.py:1780-1805` `_get_expense_account_destination`，`hr_expense.py:244-253`）。
2. **价税分离三件套**：每行 untaxed/tax/total 分列，便于进项税抵扣（🟢 Odoo `hr_expense.py:154-187`）。
3. **项目/成本中心作为报销行原生维度**：直接挂 `projectId`/`costCenterId`（🟢 Odoo `_inherit=['analytic.mixin']`），作为成本归集来源（消除与 `projects/cost-collection.md` 的不一致）。
4. **借款科目方向 = 其他应收款-员工预支**（**非应付**，方向相反）——预付款单独科目，不污染应收应付余额（🟢 ERPNext `payment_entry.py:235-274` `book_advance_payments_in_separate_party_account`）。
5. **还款用凭证承载**（原"复用 `ErpFinPayment`"已演进）：借款还款不建独立还款单，现金还款直接生成 `ErpFinVoucher`（`EMPLOYEE_ADVANCE_SETTLE`）凭证，经业财回链关联源借款单（🟢 frappe/hrms `employee_advance.py:328-394` `make_return_entry` 凭证承载模式）。原 ERPNext `payment_entry.py:548` 复用 Payment Entry 的前提（通用 finance 付款实体）在本仓不成立。

## 实体清单

> 表前缀 `erp_fin_`、类名 `ErpFin*`、字典 `erp-fin/*`。以下为建议命名，待 ORM 计划落地。

### ErpFinExpenseClaim（费用报销单，表 `erp_fin_expense_claim`）

| 字段 | 含义 |
|---|---|
| id/code/orgId | 标准 |
| claimantId | 报销人（→员工/用户主数据） |
| departmentId | 部门 |
| businessDate | 业务日期 |
| paymentMode | dict `erp-fin/expense-payment-mode`：OWN_ACCOUNT=10（员工垫付）/COMPANY_ACCOUNT=20（公司直付） |
| currencyId/exchangeRate/amountSource/amountFunctional | 多币种四件套 |
| amountWithoutTax/taxAmount/amountWithTax | 价税合计（=Σ行） |
| settleAdvanceId | 冲销的借款单（可选，报销时抵扣借款） |
| docStatus | dict `erp-fin/expense-claim-status`：DRAFT/SUBMITTED/APPROVED/REJECTED/CANCELLED |
| approveStatus | dict `erp-fin/approve-status`（共用）：UNSUBMITTED/SUBMITTED/APPROVED/REJECTED |
| posted/postedBy/postedAt | 业财三件套 |
| 标准审计字段 | version/delVersion/createdBy/createTime/updatedBy/updateTime/remark |

**状态机**：`DRAFT → SUBMITTED → APPROVED`（终态，触发过账）；`SUBMITTED → REJECTED → DRAFT`（修改重提）；`APPROVED → CANCELLED`（红冲凭证）。坚持三轴（docStatus + approveStatus + posted），不学 Odoo 单轴串一轴。

### ErpFinExpenseClaimLine（报销明细，表 `erp_fin_expense_claim_line`）

| 字段 | 含义 |
|---|---|
| id/claimId/lineNo/orgId | 标准 |
| expenseType | dict `erp-fin/expense-type`：差旅/招待/办公/通讯/交通/其他 |
| projectId | 项目（成本归集维度，→projects，可空） |
| costCenterId | 成本中心（→`cost-center.md`，可空） |
| subjectId/subjectCode | 费用科目 |
| amountWithoutTax | 不含税金额 |
| taxRate/taxAmount | 税率/税额 |
| amountWithTax | 价税合计 |
| 标准审计字段 | |

### ErpFinEmployeeAdvance（员工借款/预支，表 `erp_fin_employee_advance`）

| 字段 | 含义 |
|---|---|
| id/code/orgId | 标准 |
| employeeId | 借款人 |
| advanceType | dict `erp-fin/advance-type`：EXPENSE_ADVANCE=10（费用预支）/IMPREST=20（备用金）/TRAVEL=30（差旅借款） |
| businessDate | 借款日期 |
| currencyId/exchangeRate/amountSource/amountFunctional | 多币种四件套 |
| settledAmount | 已核销金额（派生，=Σ还款核销） |
| outstandingAmount | 未还金额（派生，=amount−settledAmount） |
| projectId | 关联项目（项目借款） |
| docStatus | dict `erp-fin/advance-status`：DRAFT/SUBMITTED/APPROVED/REJECTED/CANCELLED |
| approveStatus | dict `erp-fin/approve-status`：UNSUBMITTED/SUBMITTED/APPROVED/REJECTED |
| posted/postedBy/postedAt | 业财三件套 |
| 标准审计字段 | |

**裁决 D4（备用金不建独立实体）**：备用金作为 `advanceType=IMPREST` 特化，不建独立备用金实体。⚪ 开源零覆盖（4 个开源 ERP 均无独立备用金实体），属中国会计实践。备用金特点：长期周转、定额、定期补足，通过 IMPREST 类型 + 定期报销冲抵 + 补足新借款实现闭环。

**借款科目方向 = 其他应收款-员工预支**（资产/借方），与报销（应付-员工，负债/贷方）方向相反。

## 业财过账（businessType）

复用 `IErpFinAcctDocProvider` + `ErpFinAcctDocRegistry`（见 `posting.md`），新增三类 businessType：

| businessType | 触发单据 | 借贷方向（典型） |
|---|---|---|
| EXPENSE_CLAIM | 报销单 APPROVED | 借：费用科目（按行）/ 借：进项税 / 贷：应付-员工（own_account）或 银行存款（company_account） |
| EMPLOYEE_ADVANCE | 借款单 APPROVED | 借：其他应收款-员工预支 / 贷：银行存款 |
| EMPLOYEE_ADVANCE_SETTLE | 还款核销 | 借：应付-员工（报销抵扣）或 银行存款（现金还款）/ 贷：其他应收款-员工预支 |

> paymentMode 决定 EXPENSE_CLAIM 的贷方科目（员工垫付挂应付-员工，公司直付直接减银行），由凭证模板按 paymentMode 选模板行实现（posting.md 的模板配置化机制）。

## 现金还款（Cash Repayment）

> 来源：`docs/analysis/erp-survey/2026-07-04-0000-frappe-hrms.md`（frappe/hrms `make_return_entry` 实测）。员工借款未消费部分以现金/银行转账退回，不建独立还款单，**用 `ErpFinVoucher` 凭证承载**。

### 还款分录

现金还款生成 `EMPLOYEE_ADVANCE_SETTLE` 凭证：

```
借：银行存款（或库存现金）          returnAmount
    贷：其他应收款-员工预支               returnAmount
```

凭证经业财过账引擎（`IErpFinAcctDocProvider`）生成，业财回链（`ErpFinVoucherBillR`）关联源借款单。**无需新建通用 finance 付款实体**——这是解除原 Deferred 的关键（参 hrms `make_return_entry` 模式：用 JE 承载，不依赖 Payment 实体）。

### 实现注记（plan 2026-07-18-0718-2 落地）

- **入口**：`ErpFinEmployeeAdvanceBizModel.cashRepay(advanceId, amount, context)` `@BizMutation`（Java 自动暴露 GraphQL `ErpFinEmployeeAdvance__cashRepay` 端点，无需 xbiz action 注册）。
- **守卫**：advance 须 `posted=true && approveStatus=APPROVED`（`ERR_EMPLOYEE_ADVANCE_NOT_REPAYABLE`）；`amount > 0`（`ERR_EMPLOYEE_ADVANCE_CASH_REPAY_AMOUNT_INVALID`）；`amount <= outstandingAmount`（`ERR_EMPLOYEE_ADVANCE_CASH_REPAY_EXCEEDS_OUTSTANDING`）。
- **字段更新**：`settledAmount += amount`；`outstandingAmount -= amount`；字段经 `updateEntity` 先于凭证持久化（对齐 `postSettle` 失败不阻断范式——残留风险：字段已更新但凭证缺失，归异常工作台补录）。
- **docStatus 保持不变**：字典 `erp-fin/advance-status` 无 SETTLED 值；outstandingAmount=0 由查询/UI 派生投影表达「已结清」，非字典推进（对齐 §还款状态派生）。
- **SETTLE_TYPE 分派机制**：`EmployeeAdvanceAcctDocProvider.createFacts` SETTLE 分支按 `billData.SETTLE_TYPE` 分派——`CASH` → Dr 1002 / Cr 1221（现金还款）；`OFFSET` 或缺省 → Dr 2241 / Cr 1221（既有报销抵扣路径，零回归）。
- **billHeadCode 格式**：`EA-CASH-REPAY-<advanceCode>-<millis>`（含时间戳避免同 advance 多次还款碰撞）。
- **三金额闭环仍 Deferred**：`settledAmount` 混合累计报销抵扣 + 现金还款；`claimedAmount`/`returnedAmount` 拆分属 ORM 保护区域（见 §借款金额维度建议）。

### 借款清算三路径

员工借款 `outstandingAmount` 归零有三条路径，可组合使用：

| 路径 | 触发动作 | 凭证/核销 | businessType |
|---|---|---|---|
| **报销抵扣** | 报销单 APPROVED，自动抵扣同员工未还借款 | `ErpFinReconciliation` 净额核销 + 凭证 | EXPENSE_CLAIM（报销）/ EMPLOYEE_ADVANCE_SETTLE（抵扣结算） |
| **现金还款** | 员工主动退回未消费现金 | `ErpFinVoucher` 凭证承载 | EMPLOYEE_ADVANCE_SETTLE |
| **薪资扣回** | 从下月薪资抵扣 | 依赖 HR 薪酬扣回项 | Follow-up（HR `Additional Salary` 落地后） |

### 借款金额维度建议

> 对标 frappe/hrms 三金额闭环（paid/claimed/return）。属 ORM 字段建议（保护区域），落地须计划批准。

`ErpFinEmployeeAdvance` 当前 `settledAmount`（已核销金额）混合了"报销抵扣"与"现金还款"。建议细化为二维以支持 hrms 式三金额闭环：

| 维度 | 含义 | 对应 hrms |
|---|---|---|
| `claimedAmount` | 报销抵扣金额 | claimed |
| `returnedAmount` | 现金还款金额 | return |
| `settledAmount` | = claimed + returned（派生，向后兼容） | — |

闭环等式：`paidAmount（已付借款）= claimedAmount + returnedAmount + outstandingAmount（未还）`。

### 还款状态派生

借款状态由金额比例派生（参 hrms `set_status` 派生投影，本项目 `ErpHrSalary.paymentStatus` 已有派生投影先例）：

| 派生状态 | 条件 |
|---|---|
| 未还 | settledAmount = 0 |
| 部分还款 | 0 < settledAmount < amount |
| 已结清 | settledAmount = amount |

## 跨域协作

| 对端 | 协作内容 |
|---|---|
| projects | 报销行 `projectId` 归集项目成本（消除 `projects/cost-collection.md` 的悬空引用） |
| finance/budget | APPROVED 前同步调 `IErpFinBudgetControlBiz.check(subjectId, costCenterId, periodId, amount, sourceBill)`（见 `budget.md:74`），BLOCKED 则阻断审核 |
| finance/reconciliation | 借款还款通过 `ErpFinReconciliation`（`sourceBillType=EMPLOYEE_ADVANCE`）核销，复用 AR/AP 核销机制 |
| master-data | 员工/部门主数据 |

## 配置点

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `erp-fin.expense-budget-check-enabled` | true | 报销 APPROVED 前是否强制预算校验 |
| `erp-fin.advance-auto-offset-on-expense` | true | 报销时是否自动抵扣同员工未还借款 |
| `erp-fin.imprest-topup-threshold` | — | 备用金补足阈值（低于则触发补足提醒） |

## 关键业务规则

1. **报销抵扣借款**：报销人存在未还借款时，APPROVED 时自动尝试抵扣（`advance-auto-offset-on-expense`），抵扣不完的部分走 paymentMode 正常支付。
2. **预算控制强一致**：预算校验在审核事务内同步调用，不走事件（见 `budget.md:74` 钩子位置）。
3. **借款核销多对多**：一笔还款可核销多笔借款，一笔借款可分多次还款，复用 `ErpFinReconciliation` 的 open-item 机制。
4. **价税分离**：行级 untaxed/tax/total 保证进项税可抵扣，与采购发票一致。
5. **红冲联动**：报销单/借款单 CANCELLED 时按业财回链红冲已过账凭证（posting.md 冲销机制）。

## 反模式警示

- ⛔ **学 Odoo 单轴 state**（draft→submitted→approved→posted→in_payment→paid 七态串一轴，🟢 `hr_expense.py:122-142`）——本项目坚持三轴分离（docStatus + approveStatus + posted + paidStatus），业务态与过账态解耦。
- ⛔ **把员工借款塞进应付发票表**——借款方向=其他应收（资产），与应付（负债）方向相反，混表会导致余额语义错乱。
- ⛔ **学管伊佳逗号字符串多账户**（business-design-takeaways.md 主题 8.2）——多费用类别用子表（ErpFinExpenseClaimLine），不用单字段逗号串。
- ⛔ **建独立还款单 / 依赖通用 Payment 实体**——现金还款用 `ErpFinVoucher` 凭证承载（`EMPLOYEE_ADVANCE_SETTLE` businessType + 业财回链），不建独立还款单，也不依赖本仓不存在的 `ErpFinPayment`（参 frappe/hrms `make_return_entry` 凭证承载模式，🟢 `employee_advance.py:328-394`）。

## 菜单归属

finance 域「费用管理」分组：费用报销单、员工借款。备用金通过 `advanceType=IMPREST` 筛选，不单独建菜单。

## 证据强度标注

| 证据 | 强度 | 说明 |
|---|---|---|
| paymentMode 对方科目决策 | 🟢 | Odoo `hr_expense.py:1780-1805` 源码实测 |
| 价税分离、项目归集维度 | 🟢 | Odoo `hr_expense.py:154-187` 源码实测 |
| 借款单独科目（其他应收） | 🟢 | ERPNext `payment_entry.py:235-274` 源码实测 |
| 还款复用收付款单 | 🟢 | ERPNext `payment_entry.py:548` 源码实测 |
| Odoo 单轴 state（反模式） | 🟢 | Odoo `hr_expense.py:122-142` 源码实测 |
| 备用金（advanceType=IMPREST） | ⚪ | 开源零覆盖，中国会计实践领域常识 |

## 参考

- `docs/analysis/2026-06-30-0001-advanced-scenario-design-comparison.md` §1.1（设计依据）
- `docs/design/finance/posting.md`（IErpFinAcctDocProvider 过账机制）
- `docs/design/finance/budget.md:74`（预算控制钩子）
- `docs/design/finance/ar-ap-reconciliation.md`（核销机制复用）
- `docs/design/projects/cost-collection.md`（成本归集来源引用）
