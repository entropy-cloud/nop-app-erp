# Purchase 视图按钮需求覆盖分析

## 分析范围

| 实体 | 实际分类 | 备注 |
|------|----------|------|
| ErpPurOrder | CRUD+WF | 采购订单头 |
| ErpPurOrderLine | CRUD | 采购订单行，子表实体 |
| ErpPurReceive | CRUD+WF | 采购入库单头 |
| ErpPurReceiveLine | CRUD | 采购入库单行，子表实体 |
| ErpPurInvoice | CRUD+WF | 采购发票头 |
| ErpPurInvoiceLine | CRUD | 采购发票行，子表实体 |
| ErpPurPayment | CRUD+WF | 付款单头 |
| ErpPurPaymentLine | CRUD | 付款单行（核销明细），子表实体 |
| ErpPurReturn | CRUD+WF | 采购退货单头 |
| ErpPurReturnLine | CRUD | 采购退货单行，子表实体 |
| ErpPurRequisition | CRUD+WF | 采购请购单（approveStatus 轴完整） |
| ErpPurRequisitionLine | CRUD | 请购单行，子表实体 |
| ErpPurRfq | CRUD | 询价单（仅 CRUD 基线，缺自定义状态按钮） |
| ErpPurRfqLine | CRUD | 询价单行，子表实体 |
| ErpPurQuotation | CRUD | 供应商报价（仅 CRUD 基线，缺自定义状态按钮） |
| ErpPurQuotationLine | CRUD | 报价单行，子表实体 |
| ErpPurSupplierPriceList | CRUD | 供应商价格清单，配置类实体 |
| ErpPurSupplierScorecard | CRUD | 供应商评分卡，配置类实体 |
| ErpPurSupplierScorecardCriteria | CRUD | 评分卡标准，配置类子实体 |
| ErpPurSupplierScorecardVariable | CRUD | 评分卡变量，配置类子实体 |

## 期望按钮推导依据

1. **CRUD 基线**（所有实体）：`add-button`、`batch-delete-button`（toolbar）；`row-view-button`、`row-update-button`、`row-delete-button`（row）。来源：`METHODOLOGY.md §1.1`。

2. **审批流按钮**（业务单据头，有 approveStatus 轴）：`row-submit-button`、`row-withdraw-approval-button`、`row-approve-button`、`row-reject-button`、`row-reverse-approve-button`、`row-cancel-button`。来源：`state-machine.md §2` 迁移完整性审批轴 + `ui-patterns.md` 编辑页工具栏"反审核[作废]"及列表页操作组描述。

3. **五核心业务单据**（`purchase/README.md §核心业务对象`）：ErpPurOrder、ErpPurReceive、ErpPurInvoice、ErpPurPayment、ErpPurReturn — 全部需审批流按钮 + CRUD。

4. **ErpPurRequisition**（`requisition.md §状态机`）：请购单状态机 DRAFT→SUBMITTED→APPROVED/REJECTED/CANCELLED，需标准审批流按钮 + `row-cancel-button`。

5. **ErpPurRfq**（`requisition.md §状态机`）：DRAFT→SENT→BID_CLOSED→AWARDED/CANCELLED，不适用 approveStatus 轴。期望域专用按钮：`row-publish-button`（发布）、`row-close-bid-button`（收齐报价）、`row-award-button`（比价完成）、`row-cancel-button`（作废/流标）。

6. **ErpPurQuotation**（`requisition.md §状态机`）：DRAFT→SUBMITTED→ACCEPTED/REJECTED，期望域专用按钮：`row-submit-button`、`row-accept-button`、`row-reject-button`。

7. **`[批量审核]`、`[导出]`、`[打印]`**（`ui-patterns.md` 列表页/编辑页工具栏描述）— 无标准 view.xml 按钮 ID，记为 info 级增强点。

## 逐实体分析

### ErpPurOrder — CRUD+WF

- **期望按钮**：add-button, batch-delete-button / row-view-button, row-update-button, row-delete-button, row-submit-button, row-withdraw-approval-button, row-approve-button, row-reject-button, row-reverse-approve-button, row-cancel-button
- **实际按钮**：add-button, batch-delete-button / row-view-button, row-update-button, row-submit-button, row-withdraw-approval-button, row-approve-button, row-reject-button, row-reverse-approve-button, row-cancel-button, row-delete-button
- **差距**：无
- **判定**：clean

### ErpPurOrderLine — CRUD

- **期望按钮**：add-button, batch-delete-button / row-view-button, row-update-button, row-delete-button
- **实际按钮**：add-button, batch-delete-button / row-view-button, row-update-button, row-delete-button
- **差距**：无
- **判定**：clean

### ErpPurReceive — CRUD+WF

- **期望按钮**：add-button, batch-delete-button / row-view-button, row-update-button, row-delete-button + submit, withdraw-approval, approve, reject, reverse-approve, cancel
- **实际按钮**：add-button, batch-delete-button / row-view-button, row-update-button, row-submit-button, row-withdraw-approval-button, row-approve-button, row-reject-button, row-reverse-approve-button, row-delete-button
- **差距**：
  - `row-cancel-button`: missing (minor) — 状态机 §2 定义"任意非终态→作废"适用于所有业务单据，ErpPurOrder 已有此按钮但 ErpPurReceive 缺少。来源：`state-machine.md §2` 迁移表"任意非终态→作废"。
- **判定**：minor

### ErpPurReceiveLine — CRUD

- **期望按钮**：CRUD 基线
- **实际按钮**：CRUD 基线
- **差距**：无
- **判定**：clean

### ErpPurInvoice — CRUD+WF

- **期望按钮**：CRUD + WF + cancel
- **实际按钮**：add-button, batch-delete-button / row-view-button, row-update-button, row-submit-button, row-withdraw-approval-button, row-approve-button, row-reject-button, row-reverse-approve-button, row-delete-button
- **差距**：
  - `row-cancel-button`: missing (minor) — 同 ErpPurReceive 理由。来源：`state-machine.md §2`。
- **判定**：minor

### ErpPurInvoiceLine — CRUD

- **期望按钮**：CRUD 基线
- **实际按钮**：CRUD 基线
- **差距**：无
- **判定**：clean

### ErpPurPayment — CRUD+WF

- **期望按钮**：CRUD + WF + cancel
- **实际按钮**：add-button, batch-delete-button / row-view-button, row-update-button, row-submit-button, row-withdraw-approval-button, row-approve-button, row-reject-button, row-reverse-approve-button, row-delete-button
- **差距**：
  - `row-cancel-button`: missing (minor) — 同理由。来源：`state-machine.md §2`。
- **判定**：minor

### ErpPurPaymentLine — CRUD

- **期望按钮**：CRUD 基线
- **实际按钮**：CRUD 基线
- **差距**：无
- **判定**：clean

### ErpPurReturn — CRUD+WF

- **期望按钮**：CRUD + WF + cancel
- **实际按钮**：add-button, batch-delete-button / row-view-button, row-update-button, row-submit-button, row-withdraw-approval-button, row-approve-button, row-reject-button, row-reverse-approve-button, row-delete-button
- **差距**：
  - `row-cancel-button`: missing (minor) — 同理由。来源：`state-machine.md §2`。
- **判定**：minor

### ErpPurReturnLine — CRUD

- **期望按钮**：CRUD 基线
- **实际按钮**：CRUD 基线
- **差距**：无
- **判定**：clean

### ErpPurRequisition — CRUD+WF

- **期望按钮**：CRUD + WF + cancel（`requisition.md §状态机`：DRAFT→SUBMITTED→APPROVED/REJECTED/CANCELLED）
- **实际按钮**：add-button, batch-delete-button / row-view-button, row-update-button, row-submit-button, row-withdraw-approval-button, row-approve-button, row-reject-button, row-reverse-approve-button, row-delete-button
- **差距**：
  - `row-cancel-button`: missing (minor) — 请购单状态机明确有 CANCELLED 终态，但缺少作废按钮。来源：`requisition.md §状态机`。
- **判定**：minor

### ErpPurRequisitionLine — CRUD

- **期望按钮**：CRUD 基线
- **实际按钮**：CRUD 基线
- **差距**：无
- **判定**：clean

### ErpPurRfq — CRUD

- **期望按钮**：CRUD 基线 + 域专用按钮（`requisition.md §状态机`：DRAFT→SENT→BID_CLOSED→AWARDED/CANCELLED）
- **实际按钮**：add-button, batch-delete-button / row-view-button, row-update-button, row-delete-button
- **差距**：
  - 缺少完整 RFQ 状态机动作按钮：`row-publish-button`（发布→SENT）、`row-close-bid-button`（收齐报价→BID_CLOSED）、`row-award-button`（比价完成→AWARDED）、`row-cancel-button`（作废/流标）均缺失 (major) — `requisition.md` 定义了完整 RFQ 状态机，但 view.xml 仅覆盖 CRUD 基线，无任何状态迁移按钮。来源：`requisition.md §状态机·询价单`。
- **判定**：major

### ErpPurRfqLine — CRUD

- **期望按钮**：CRUD 基线
- **实际按钮**：CRUD 基线
- **差距**：无
- **判定**：clean

### ErpPurQuotation — CRUD

- **期望按钮**：CRUD 基线 + 域专用按钮（`requisition.md §状态机`：DRAFT→SUBMITTED→ACCEPTED/REJECTED）
- **实际按钮**：add-button, batch-delete-button / row-view-button, row-update-button, row-delete-button
- **差距**：
  - 缺少完整报价状态机动作按钮：`row-submit-button`（提交→SUBMITTED）、`row-accept-button`（中标→ACCEPTED）、`row-reject-button`（未中标→REJECTED）均缺失 (major) — `requisition.md` 定义了供应商报价状态机但 view.xml 完全未实现。来源：`requisition.md §状态机·供应商报价`。
- **判定**：major

### ErpPurQuotationLine — CRUD

- **期望按钮**：CRUD 基线
- **实际按钮**：CRUD 基线
- **差距**：无
- **判定**：clean

### ErpPurSupplierPriceList — CRUD

- **期望按钮**：CRUD 基线（配置类实体，无审批流）
- **实际按钮**：CRUD 基线
- **差距**：无
- **判定**：clean

### ErpPurSupplierScorecard — CRUD

- **期望按钮**：CRUD 基线（配置类实体，无审批流）
- **实际按钮**：CRUD 基线
- **差距**：无
- **判定**：clean

### ErpPurSupplierScorecardCriteria — CRUD

- **期望按钮**：CRUD 基线
- **实际按钮**：CRUD 基线
- **差距**：无
- **判定**：clean

### ErpPurSupplierScorecardVariable — CRUD

- **期望按钮**：CRUD 基线
- **实际按钮**：CRUD 基线
- **差距**：无
- **判定**：clean

## 摘要

| 分类 | 实体 | 差距数 | 最高严重级 | 备注 |
|------|------|--------|-----------|------|
| CRUD+WF | ErpPurOrder | 0 | clean | 所有期望按钮齐全 |
| CRUD+WF | ErpPurReceive | 1 | minor | 缺 row-cancel-button |
| CRUD+WF | ErpPurInvoice | 1 | minor | 缺 row-cancel-button |
| CRUD+WF | ErpPurPayment | 1 | minor | 缺 row-cancel-button |
| CRUD+WF | ErpPurReturn | 1 | minor | 缺 row-cancel-button |
| CRUD+WF | ErpPurRequisition | 1 | minor | 缺 row-cancel-button |
| CRUD | ErpPurRfq | 4 | major | 缺整套 RFQ 状态按钮 |
| CRUD | ErpPurQuotation | 3 | major | 缺整套报价状态按钮 |
| CRUD | ErpPurOrderLine | 0 | clean | |
| CRUD | ErpPurReceiveLine | 0 | clean | |
| CRUD | ErpPurInvoiceLine | 0 | clean | |
| CRUD | ErpPurPaymentLine | 0 | clean | |
| CRUD | ErpPurReturnLine | 0 | clean | |
| CRUD | ErpPurRequisitionLine | 0 | clean | |
| CRUD | ErpPurRfqLine | 0 | clean | |
| CRUD | ErpPurQuotationLine | 0 | clean | |
| CRUD | ErpPurSupplierPriceList | 0 | clean | |
| CRUD | ErpPurSupplierScorecard | 0 | clean | |
| CRUD | ErpPurSupplierScorecardCriteria | 0 | clean | |
| CRUD | ErpPurSupplierScorecardVariable | 0 | clean | |

### 跨域 info 级增强点（ui-patterns.md 提及但无标准按钮 ID）

| 增强点 | 原文出处 | 适用实体 |
|--------|----------|----------|
| `[批量审核]` | `ui-patterns.md §列表页结构` 工具栏 | 所有业务单据头实体 |
| `[导出]` | `ui-patterns.md §列表页结构` 工具栏 | 所有业务单据头实体 |
| `[打印]` | `ui-patterns.md §编辑页通用结构` 工具栏 | 所有业务单据头实体 |

这些增强点有设计意图但无标准 view.xml 按钮 ID，属于可选择性实现的能力。不计入按实体的差距计数。

### 总评

- 总实体数：20
- 无差距实体：13（65%）
- Blocker 差距：0
- Major 差距：2（ErpPurRfq、ErpPurQuotation — 寻源链前端实体缺少整套状态按钮）
- Minor 差距：5（五个业务单据头实体缺 `row-cancel-button`）
- Info 增强点：3（跨域，批量审核/导出/打印）

### 关键发现

1. **`row-cancel-button` 缺口（minor x5）**：ErpPurOrder 是唯一有作废按钮的业务单据头。ErpPurReceive、ErpPurInvoice、ErpPurPayment、ErpPurReturn、ErpPurRequisition 均缺少此按钮，尽管 state-machine.md §2 和 requisition.md §状态机都定义 CANCELLED 为合法终态。建议统一补齐，visibleOn 条件参考 ErpPurOrder 的 `${docStatus != 'CANCELLED'}`。

2. **RFQ 与 Quotation 状态动作缺失（major x2）**：ErpPurRfq 和 ErpPurQuotation 的 view.xml 仅覆盖 CRUD 基线（无任何自定义 rowActions 覆盖），但 `requisition.md` 为这两个实体定义了独立的状态机。这是非核心主单据的已知待办，建议在后续 sprint 中补充状态迁移按钮。

3. **CRUD 基线覆盖充分**：全部 20 个实体的 CRUD 基线按钮（add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button）均完整，codegen 生成的基线未出现退化。

4. **核心业务单据审批流按钮齐全**：五个核心业务单据（Order/Receive/Invoice/Payment/Return）和请购单的 `row-submit-button`、`row-withdraw-approval-button`、`row-approve-button`、`row-reject-button`、`row-reverse-approve-button` 全部存在，与 state-machine.md 审批轴迁移完整对齐。
