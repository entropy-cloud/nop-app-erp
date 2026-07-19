# Finance 视图按钮需求覆盖分析

## 分析范围

Finance 域共 30 实体，按实际 view.xml 按钮分类：

| 分类 | 实体数 | 实体 |
|------|--------|------|
| CRUD | 27 | AccountingPeriod, AccountingPeriodStatus, ArApItem, BadDebt, BankReconciliation, BankReconciliationLine, BankStatement, BankStatementLine, BudgetControlLog, BudgetLine, CashForecast, CreditFacility, ExpenseClaimLine, FundAccount, GlBalance, NotesDiscount, NotesPayable, NotesReceivable, PostingException, Reconciliation, ReconciliationLine, TrialBalance, Voucher, VoucherBillR, VoucherLine, VoucherTemplate, VoucherTemplateLine |
| CRUD+WF | 2 | EmployeeAdvance, ExpenseClaim |
| CRUD+Custom | 0 | — |
| Custom | 1 | BudgetScenario（缺 row-update-button、row-delete-button；含 WF 按钮） |
| Other | 0 | — |

## 期望按钮推导依据

1. **CRUD 基线**（METHODOLOGY §1.1）：所有实体默认期望 add-button、batch-delete-button（toolbar）+ row-view-button、row-update-button、row-delete-button（row）。
2. **审批/工作流基线**（METHODOLOGY §1.2）：有 approveStatus（UNSUBMITTED→SUBMITTED→APPROVED→REJECTED）的实体期望 submit/withdraw-approval/approve/reject/reverse-approve/cancel。
3. **ui-patterns.md**：
   - `ui-patterns.md:74`：凭证列表"草稿可编辑/过账/作废，已过账可查看/红冲，已作废只查看" → 凭证需过账(row-post)、红冲(row-reverse)、作废(row-cancel) 按钮。
   - `ui-patterns.md:144`："结账按钮点击触发'期末结账向导'" → 会计期间需结账操作按钮。
   - `ui-patterns.md:152`：总账/明细账为只读报表页（筛选条件 + 查询/打印/导出），非 CRUD 实体。
4. **state-machine.md**：
   - Voucher 状态机：DRAFT→POSTED（过账）、POSTED→红冲（生成红字凭证）、DRAFT→CANCELLED（作废）。
   - AccountingPeriod 状态机：OPEN→CLOSING（结账发起）、CLOSED_FINAL→OPEN（反结账）。
5. **roles-and-permissions.md:106**：财务员 FNPT 覆盖 `post, reverse, close, batchDepreciation, reconcile` — 对应凭证过账/红冲、期间结账、核销操作。

## 逐实体分析

### ErpFinVoucher — CRUD（应升为 CRUD+Custom，缺核心业务按钮）

- **期望按钮**：CRUD 基线 + row-post-button（过账）、row-reverse-button（红冲）、row-cancel-button（作废）
- **实际按钮**：CRUD 基线（add, batch-delete, view, update, delete）
- **差距**：
  - `row-post-button`: **MISSING (blocker)** — ui-patterns.md:74 明确"草稿可...过账"；state-machine.md:32 DRAFT→POSTED 是凭证核心迁移；roles-and-permissions.md:106 财务员含 `post` 权限
  - `row-reverse-button`: **MISSING (blocker)** — ui-patterns.md:74 明确"已过账可查看/红冲"；state-machine.md:33 POSTED→红冲是唯一纠错路径
  - `row-cancel-button`: **MISSING (major)** — state-machine.md:35 DRAFT→CANCELLED；ui-patterns.md:74 "草稿可...作废"。row-delete-button 是物理删除，业务上不同（ERP 中草稿可物理删除但非作废）
- **判定**：**blocker** — 凭证是 finance 域核心实体，缺少两个核心业务流程按钮

### ErpFinAccountingPeriod — CRUD（应升为 CRUD+Custom，缺期间操作按钮）

- **期望按钮**：CRUD 基线 + row-close-period-button（结账）、row-reverse-close-button（反结账）
- **实际按钮**：CRUD 基线（add, batch-delete, view, update, delete）
- **差距**：
  - `row-close-period-button`: **MISSING (blocker)** — ui-patterns.md:144 "结账按钮点击触发'期末结账向导'"；state-machine.md:150 OPEN→CLOSING 是期间核心迁移
  - `row-reverse-close-button`: **MISSING (major)** — state-machine.md:178 CLOSED_FINAL→OPEN（反结账），高权限操作需显式按钮
- **判定**：**blocker** — 期间结账是 finance 域核心功能入口

### ErpFinBudgetScenario — Custom（缺 CRUD 基线按钮，含 WF 按钮）

- **期望按钮**：CRUD 基线 + row-submit-button, row-approve-button, row-reject-button, row-cancel-button
- **实际按钮**：toolbar (add, batch-delete) + row (view, submit, approve, reject, cancel)
- **差距**：
  - `row-update-button`: **MISSING (minor)** — CRUD 基线缺失；BudgetScenario 为配置类实体，非核心业务单据，降为 minor
  - `row-delete-button`: **MISSING (minor)** — 同上
- **判定**：**minor** — 配置类实体 CRUD 基线不全

### ErpFinGlBalance — CRUD（只读报表误生 CRUD 按钮）

- **期望按钮**：仅 row-view-button（只读查询页，ui-patterns.md:152 描述为"总账/明细账查询"）
- **实际按钮**：完整 CRUD 基线（add, batch-delete, view, update, delete）
- **差距**：
  - `add-button`: **EXTRA (info)** — 总账是派生视图，不应手动新增
  - `batch-delete-button`: **EXTRA (info)** — 同上
  - `row-update-button`: **EXTRA (info)** — 总账余额由过账/结账自动生成，不应直接编辑
  - `row-delete-button`: **EXTRA (info)** — 同上
- **判定**：**info** — 功能正确但只读报表不应有写按钮

### ErpFinTrialBalance — CRUD（只读报表误生 CRUD 按钮）

- **期望按钮**：仅 row-view-button（试算平衡表为只读报表）
- **实际按钮**：完整 CRUD 基线
- **差距**：
  - `add-button`, `batch-delete-button`, `row-update-button`, `row-delete-button`: **EXTRA (info)** — 试算平衡表是派生视图，不应有写入操作
- **判定**：**info**

### ErpFinReconciliation — CRUD（缺核销专用操作按钮）

- **期望按钮**：CRUD 基线 + row-reconcile-button（核销执行）
- **实际按钮**：CRUD 基线
- **差距**：
  - `row-reconcile-button`: **MISSING (info)** — ui-patterns.md 提及应收应付核销页但未具体描述按钮布局；roles-and-permissions.md:106 财务员含 `reconcile` 权限。有 BizModel 级操作但 UI 层未暴露按钮
- **判定**：**info**

### ErpFinBadDebt — CRUD（缺坏账专用操作按钮）

- **期望按钮**：CRUD 基线 + 坏账计提/核销/收回等专用按钮（bad-debt.md 描述的工作流）
- **实际按钮**：CRUD 基线
- **差距**：
  - 坏账动作按钮（计提/核销/收回/释放）: **MISSING (info)** — bad-debt.md 定义了复杂生命周期但 ui-patterns.md 未描述具体 UI 按钮布局；坏账操作可能以向导/弹窗形式实现，不能仅凭 ORM 实体推断期望按钮
- **判定**：**info**

### ErpFinBankReconciliation — CRUD（缺银行对账专用按钮）

- **期望按钮**：CRUD 基线 + 银行对账执行/导入按钮
- **实际按钮**：CRUD 基线
- **差距**：
  - 银行对账按钮: **MISSING (info)** — 银行对账是专用流程，ui-patterns.md 未描述详细 UI
- **判定**：**info**

### ErpFinEmployeeAdvance — CRUD+WF（完整，无差距）

- **期望按钮**：CRUD 基线 + submit, withdraw-approval, approve, reject, reverse-approve
- **实际按钮**：CRUD 基线 + submit, withdraw-approval, approve, reject, reverse-approve（已验证 view.xml:223-263）
- **差距**：无
- **判定**：**clean**

### ErpFinExpenseClaim — CRUD+WF（完整，无差距）

- **期望按钮**：CRUD 基线 + submit, withdraw-approval, approve, reject, reverse-approve
- **实际按钮**：CRUD 基线 + submit, withdraw-approval, approve, reject, reverse-approve（已验证 view.xml:266-305）
- **差距**：无
- **判定**：**clean**

### 剩余 19 实体 — CRUD（完整，无差距）

以下实体均为标准 CRUD，view.xml 含完整 CRUD 基线，设计文档未描述额外业务按钮：

- ErpFinAccountingPeriodStatus（字典表）
- ErpFinArApItem（系统管理辅助账项）
- ErpFinBankReconciliationLine（核销行子表）
- ErpFinBankStatement（银行对账单）
- ErpFinBankStatementLine（银行对账单行）
- ErpFinBudgetControlLog（预算控制日志）
- ErpFinBudgetLine（预算行明细）
- ErpFinCashForecast（资金预测）
- ErpFinCreditFacility（授信额度）
- ErpFinExpenseClaimLine（报销单行）
- ErpFinFundAccount（资金账户）
- ErpFinNotesDiscount（票据贴现）
- ErpFinNotesPayable（应付票据）
- ErpFinNotesReceivable（应收票据）
- ErpFinPostingException（过账异常日志）
- ErpFinReconciliationLine（核销行子表）
- ErpFinVoucherBillR（业财回链，系统维护表）
- ErpFinVoucherLine（凭证分录行，子表）
- ErpFinVoucherTemplate（凭证模板，配置表）
- ErpFinVoucherTemplateLine（模板行）

所有 20 实体实际按钮 = CRUD 基线，无差距。

- **判定**：**clean**（20 实体）

## 摘要

| 分类 | 实体 | 差距数 | 最高严重级 | 备注 |
|------|------|--------|-----------|------|
| CRUD | ErpFinVoucher | 3 | blocker | 缺过账/红冲/作废按钮 |
| CRUD | ErpFinAccountingPeriod | 2 | blocker | 缺结账/反结账按钮 |
| Custom | ErpFinBudgetScenario | 2 | minor | 缺 update/delete 按钮 |
| CRUD | ErpFinGlBalance | 4 | info | 只读报表误生 CRUD 按钮 |
| CRUD | ErpFinTrialBalance | 4 | info | 只读报表误生 CRUD 按钮 |
| CRUD | ErpFinReconciliation | 1 | info | 缺核销专用按钮 |
| CRUD | ErpFinBadDebt | 1 | info | 缺坏账专用按钮 |
| CRUD | ErpFinBankReconciliation | 1 | info | 缺对账专用按钮 |
| CRUD+WF | ErpFinEmployeeAdvance | 0 | clean | 完整 |
| CRUD+WF | ErpFinExpenseClaim | 0 | clean | 完整 |
| CRUD | 其他 20 实体 | 0 | clean | 标准 CRUD 无差距 |

### 总评
- 总实体数：30
- 无差距实体：22（73.3%）
- Blocker 差距：2 实体（ErpFinVoucher, ErpFinAccountingPeriod）
- Major 差距：0（独立；ErpFinVoucher.row-cancel-button 合并计入 blocker 实体内）
- Minor 差距：1 实体（ErpFinBudgetScenario）
- Info 差距：5 实体（GlBalance, TrialBalance, Reconciliation, BadDebt, BankReconciliation）

### 缺省按钮汇总（全部域）

| 缺失按钮 | 涉及实体 | 严重级 | 来源 |
|----------|----------|--------|------|
| `row-post-button` | ErpFinVoucher | blocker | ui-patterns.md:74，state-machine.md:32 |
| `row-reverse-button` | ErpFinVoucher | blocker | ui-patterns.md:74，state-machine.md:33 |
| `row-cancel-button` | ErpFinVoucher | major | state-machine.md:35，row-delete 不等同作废 |
| `row-close-period-button` | ErpFinAccountingPeriod | blocker | ui-patterns.md:144，state-machine.md:150 |
| `row-reverse-close-button` | ErpFinAccountingPeriod | major | state-machine.md:178 |
| `row-update-button` | ErpFinBudgetScenario | minor | CRUD 基线 |
| `row-delete-button` | ErpFinBudgetScenario | minor | CRUD 基线 |

### 处理建议

1. **ErpFinVoucher**（最高优先级）：在 rowActions 中添加：
   - `row-post-button`（`ErpFinVoucher__post`），visibleOn DRAFT 状态
   - `row-reverse-button`（`ErpFinVoucher__reverse`），visibleOn POSTED 状态
   - `row-cancel-button`（`ErpFinVoucher__cancel`），visibleOn DRAFT 状态
2. **ErpFinAccountingPeriod**：在 rowActions 中添加：
   - `row-close-period-button` 触发结账流程（`ErpFinAccountingPeriod__closePeriod`）
   - `row-reverse-close-button` 触发反结账（`ErpFinAccountingPeriod__reverseClose`）
3. **ErpFinBudgetScenario**：补充 `row-update-button` 和 `row-delete-button`
4. **ErpFinGlBalance / ErpFinTrialBalance**：后续可考虑移除写操作按钮，转为只读报表页
