# Sales 视图按钮需求覆盖分析

## 分析范围

销售域共 16 个实体，分类如下：

| 分类 | 实体数 | 实体名单 |
|------|--------|----------|
| **CRUD+WF** | 6 | ErpSalOrder, ErpSalDelivery, ErpSalInvoice, ErpSalReceipt, ErpSalReturn, ErpSalQuotation |
| **CRUD** | 9 | ErpSalOrderLine, ErpSalDeliveryLine, ErpSalInvoiceLine, ErpSalReceiptLine, ErpSalReturnLine, ErpSalPriceList, ErpSalPriceListLine, ErpSalPricingRule, ErpSalContract |
| **Other** | 0 | — |

## 期望按钮推导依据

- **CRUD 基线**（METHODOLOGY §1.1）：toolbar {add-button, batch-delete-button} + row {row-view-button, row-update-button, row-delete-button}，适用于所有实体。
- **审批/工作流按钮**（METHODOLOGY §1.2 + state-machine.md §1）：销售订单/销售出库单/销售发票/收款单/销售退货单 5 个核心业务单据头需具备 submit/withdraw-approval/approve/reject/reverse-approve/cancel 完整 WF 按钮集。state-machine.md §1 明确列示此 5 实体适用三轴状态机。
- **SalesQuotation**：ORM 具有 `approveStatus` / `docStatus`，且跨域协作（CRM 转化创建），实际已实现 WF 按钮（submit/withdraw-approval/approve/reject/reverse-approve），故归类为 CRUD+WF。
- **SalesContract**：ORM 具有 `approveStatus` / `docStatus`，但未列入 state-machine.md §1 的适用清单（合同生命周期独立，见 `domain-design-guidelines.md` §16.2 合同域 `docStatus` 取值约定），按严格准则仅需 CRUD 基线。
- **明细行 / 配置实体**：仅需 CRUD 基线。

## 逐实体分析

### ErpSalOrder — CRUD+WF
- **期望按钮**：CRUD + submit/withdraw-approval/approve/reject/reverse-approve/cancel
- **实际按钮**：CRUD + submit/withdraw-approval/approve/reject/reverse-approve/cancel
- **差距**：无
- **判定**：clean

### ErpSalDelivery — CRUD+WF
- **期望按钮**：CRUD + submit/withdraw-approval/approve/reject/reverse-approve/cancel
- **实际按钮**：CRUD + submit/withdraw-approval/approve/reject/reverse-approve（**无 cancel**）
- **差距**：
  - `row-cancel-button`: missing (minor) — 出库单作废可通过反审核 + 删除变通，但不符合 WF 基线要求
- **判定**：minor

### ErpSalInvoice — CRUD+WF
- **期望按钮**：CRUD + submit/withdraw-approval/approve/reject/reverse-approve/cancel
- **实际按钮**：CRUD + submit/withdraw-approval/approve/reject/reverse-approve（**无 cancel**）
- **差距**：
  - `row-cancel-button`: missing (minor) — 发票作废同样缺独立按钮
- **判定**：minor

### ErpSalReceipt — CRUD+WF
- **期望按钮**：CRUD + submit/withdraw-approval/approve/reject/reverse-approve/cancel
- **实际按钮**：CRUD + submit/withdraw-approval/approve/reject/reverse-approve（**无 cancel**）
- **差距**：
  - `row-cancel-button`: missing (minor) — 收款单作废缺独立按钮
- **判定**：minor

### ErpSalReturn — CRUD+WF
- **期望按钮**：CRUD + submit/withdraw-approval/approve/reject/reverse-approve/cancel
- **实际按钮**：CRUD + submit/withdraw-approval/approve/reject/reverse-approve（**无 cancel**）
- **差距**：
  - `row-cancel-button`: missing (minor) — 退货单作废缺独立按钮
- **判定**：minor

### ErpSalQuotation — CRUD+WF
- **期望按钮**：CRUD + submit/withdraw-approval/approve/reject/reverse-approve/cancel
- **实际按钮**：CRUD + submit/withdraw-approval/approve/reject/reverse-approve（**无 cancel**）
- **差距**：
  - `row-cancel-button`: missing (minor) — 报价单作废缺独立按钮
- **判定**：minor

### ErpSalContract — CRUD
- **期望按钮**：CRUD
- **实际按钮**：CRUD（row-update-button / row-delete-button 包在 row-more-button 内，功能等价）
- **差距**：无（严格按文档预期）。**观察**：ORM 具有 `approveStatus` / `docStatus` 字段，但无 WF 按钮支撑状态迁移。若未来将合同纳入审批流，需补充完整 WF 按钮集。当前行为可视为合同走 CRUD 直接编辑状态字段。
- **判定**：clean（info: 模型-视图语义不匹配）

### ErpSalOrderLine — CRUD
- **期望按钮**：CRUD
- **实际按钮**：CRUD
- **差距**：无
- **判定**：clean

### ErpSalDeliveryLine — CRUD
- **期望按钮**：CRUD
- **实际按钮**：CRUD
- **差距**：无
- **判定**：clean

### ErpSalInvoiceLine — CRUD
- **期望按钮**：CRUD
- **实际按钮**：CRUD
- **差距**：无
- **判定**：clean

### ErpSalReceiptLine — CRUD
- **期望按钮**：CRUD
- **实际按钮**：CRUD
- **差距**：无
- **判定**：clean

### ErpSalReturnLine — CRUD
- **期望按钮**：CRUD
- **实际按钮**：CRUD
- **差距**：无
- **判定**：clean

### ErpSalPriceList — CRUD
- **期望按钮**：CRUD
- **实际按钮**：CRUD（row-update-button / row-delete-button 包在 row-more-button 内）
- **差距**：无
- **判定**：clean

### ErpSalPriceListLine — CRUD
- **期望按钮**：CRUD
- **实际按钮**：CRUD
- **差距**：无
- **判定**：clean

### ErpSalPricingRule — CRUD
- **期望按钮**：CRUD
- **实际按钮**：CRUD（row-update-button / row-delete-button 包在 row-more-button 内）
- **差距**：无
- **判定**：clean

## 摘要

| 分类 | 实体 | 差距数 | 最高严重级 | 备注 |
|------|------|--------|-----------|------|
| CRUD+WF | ErpSalOrder | 0 | clean | 唯一完整包含 cancel 按钮的实体 |
| CRUD+WF | ErpSalDelivery | 1 | minor | 缺 row-cancel-button |
| CRUD+WF | ErpSalInvoice | 1 | minor | 缺 row-cancel-button |
| CRUD+WF | ErpSalReceipt | 1 | minor | 缺 row-cancel-button |
| CRUD+WF | ErpSalReturn | 1 | minor | 缺 row-cancel-button |
| CRUD+WF | ErpSalQuotation | 1 | minor | 缺 row-cancel-button |
| CRUD | ErpSalContract | 0 | clean | 模型有 approveStatus 但视图无 WF 按钮（info） |
| CRUD | ErpSalOrderLine | 0 | clean | |
| CRUD | ErpSalDeliveryLine | 0 | clean | |
| CRUD | ErpSalInvoiceLine | 0 | clean | |
| CRUD | ErpSalReceiptLine | 0 | clean | |
| CRUD | ErpSalReturnLine | 0 | clean | |
| CRUD | ErpSalPriceList | 0 | clean | |
| CRUD | ErpSalPriceListLine | 0 | clean | |
| CRUD | ErpSalPricingRule | 0 | clean | |

### 总评
- 总实体数：16
- 无差距实体：11（68.8%）
- Blocker 差距：0
- Major 差距：0
- Minor 差距：5（ErpSalDelivery / ErpSalInvoice / ErpSalReceipt / ErpSalReturn / ErpSalQuotation 各缺 `row-cancel-button`）
- Info 项：1（ErpSalContract 模型-视图语义不匹配）

### 模式发现

1. **`row-cancel-button` 缺失是系统性缺口**：6 个 CRUD+WF 实体中仅 ErpSalOrder 具有作废按钮。其余 5 个（Delivery / Invoice / Receipt / Return / Quotation）均缺少 `row-cancel-button`。因 `row-delete-button` 可覆盖草稿态作废，而已审核单据作废需要通过 `reverseApprove` + delete 变通，故评为 minor 而非 blocker。
2. **`row-more-button` 折叠模式**：ErpSalContract / ErpSalPriceList / ErpSalPricingRule 将 update/delete 包在 `actionGroup`（`row-more-button`）中，功能等价于独立按钮，不视为差距。
3. **ErpSalContract 的模型-视图鸿沟**：ORM 定义的 `approveStatus`/`docStatus` 字段在 `ErpSalContract.view.xml` 的 grid 和 form 中均有展示，但没有对应的 submit/approve/reject 等 WF 按钮。当前用户只能通过编辑表单直接修改状态字段——这是设计意图还是遗漏，需人工确认。
