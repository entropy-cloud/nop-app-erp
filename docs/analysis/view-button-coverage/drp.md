# DRP 视图按钮需求覆盖分析

## 分析范围

本域共 7 个实体（`_dump/nop-app/erp/drp/pages/`）：

| 实体 | 设计文档分类 | 实际 view.xml 分类 | 说明 |
|------|-------------|-------------------|------|
| ErpDrpPlan | CRUD+Custom | CRUD | 计划头，需域专用按钮 |
| ErpDrpLine | CRUD+WF | CRUD | 明细行，需审批流按钮 |
| ErpDrpParameter | CRUD | CRUD | 补货参数配置，纯 CRUD |
| ErpInvDrpCrossDock | CRUD | CRUD | 越库记录，辅助实体 |
| ErpInvDrpDockAppointment | CRUD | CRUD | 月台预约，辅助实体 |
| ErpInvDrpLeadTimeRecord | CRUD | CRUD | 提前期记录，辅助实体 |
| ErpInvDrpSafetyStockCalc | CRUD | CRUD | 安全库存计算，辅助实体 |

## 期望按钮推导依据

1. **CRUD 基线**（METHODOLOGY §1.1）：所有实体默认期望 add-button、batch-delete-button、row-view-button、row-update-button、row-delete-button。
2. **`ui-patterns.md` 列表页**：计划列表页 toolbar 有 `[运行DRP计算]`（§"列表页结构"示意图）；明细行子表工具栏有 `[+新行] [×删除] [批量审批] [生成采购/调拨单]`（§"DRP计划详情页结构"）；补货建议审批页面有 `[批量批准] [批量驳回]`。
3. **`ui-patterns.md` 详情页**：详情页 toolbar 有 `[保存草稿] [运行计算] [批准全部] [生成单据]`（§"DRP计划详情页结构"）。
4. **状态机**（`domain-design-guidelines.md §16.2`）：ErpDrpLine 状态 SUGGESTED→APPROVED→ORDERED→CANCELLED，需 approve/cancel 行级按钮。
5. **行级操作**（`ui-patterns.md` 明细行子表）：row 操作列有 `[查看] [批准]`。

### Prose→Button-ID 翻译（DRP 域新增）

| ui-patterns.md 原文 | 对应按钮 ID | 位置 | 备注 |
|---------------------|-------------|------|------|
| `[运行DRP计算]` | `run-drp-button` | toolbar, 列表页 | 核心业务操作 |
| `[批准全部]` | `approve-all-button` | toolbar, 详情页 | 批量批准所有行 |
| `[生成单据]` | `generate-order-button` | toolbar, 详情页/子表 | 生成采购/调拨单 |
| `[批量批准]` | `batch-approve-button` | toolbar, 补货建议审批页 | 勾选行批量批准 |
| `[批量驳回]` | `batch-reject-button` | toolbar, 补货建议审批页 | 勾选行批量驳回 |
| `[批准]` (行级) | `row-approve-button` | row, 子表 | 单行建议→批准 |
| `[驳回]` (行级) | `row-reject-button` | row | 单行驳回 |
| `[导出]` | — | toolbar | info 级差距 |

## 逐实体分析

### ErpDrpPlan — CRUD（当前） / 期望 CRUD+Custom

- **期望按钮**：
  - CRUD 基线: add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
  - 域专用按钮（ui-patterns.md §"列表页结构"+"DRP计划详情页结构"）:
    - `run-drp-button`（[运行DRP计算]，列表页 toolbar）
    - `approve-all-button`（[批准全部]，详情页 toolbar）
    - `generate-order-button`（[生成单据]，详情页 toolbar）
- **实际按钮**：
  - toolbar: add-button, batch-delete-button
  - row: row-view-button, row-update-button, row-delete-button（在 row-more-button 分组内）
- **差距**：
  - `run-drp-button`: **missing** (**blocker**) — ui-patterns.md 列表页 toolbar 明确绘制 `[运行DRP计算]`，是 DRP 核心操作按钮。来源：ui-patterns.md §"列表页结构"。
  - `approve-all-button`: **missing** (**blocker**) — ui-patterns.md 详情页 toolbar 明确绘制 `[批准全部]`，是批量审批核心按钮。来源：ui-patterns.md §"DRP计划详情页结构"。
  - `generate-order-button`: **missing** (**blocker**) — ui-patterns.md 详情页 toolbar 明确绘制 `[生成单据]`，将已批准行生成采购/调拨单。来源：ui-patterns.md §"DRP计划详情页结构"。
  - 无 extra 按钮。
- **判定**：**blocker** — 3 个 blocker 域专用按钮缺失

### ErpDrpLine — CRUD（当前） / 期望 CRUD+WF

- **期望按钮**：
  - CRUD 基线: add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
  - 审批流按钮（ui-patterns.md §"补货建议审批" + §"DRP计划详情页结构" 子表 + 状态机 SUGGESTED→APPROVED→CANCELLED）:
    - `row-approve-button`（行级[批准]，SUGGESTED→APPROVED）
    - `row-reject-button`（行级[驳回]，SUGGESTED→REJECTED）
    - `row-cancel-button`（[作废]，CANCELLED 终态支持）
- **实际按钮**：
  - toolbar: add-button, batch-delete-button
  - row: row-view-button, row-update-button, row-delete-button
- **差距**：
  - `row-approve-button`: **missing** (**blocker**) — ui-patterns.md 子表行操作列有 `[批准]`，且状态机 SUGGESTED→APPROVED 必须。来源：ui-patterns.md §"DRP计划详情页结构" 明细行子表操作列 + `domain-design-guidelines.md §16.2` drp 行级状态。
  - `row-reject-button`: **missing** (**major**) — ui-patterns.md 补货建议审批页面提及 `[批量驳回]`，行级驳回是配套能力。来源：ui-patterns.md §"补货建议审批"。
  - `row-cancel-button`: **missing** (**major**) — status 字典含 CANCELLED（`domain-design-guidelines.md §16.2`），无行级取消按钮。来源：`domain-design-guidelines.md §16.2` drp 明细行状态。
  - 补货建议审批页面本身的 toolbar 按钮（`batch-approve-button`、`batch-reject-button`、`generate-order-button`）属于专门化视图，不计入 ErpDrpLine 标准 CRUD 页面差距。
- **判定**：**blocker** — 1 blocker + 2 major 审批流按钮缺失

### ErpDrpParameter — CRUD

- **期望按钮**：CRUD 基线（ui-patterns.md §"仓库补货参数"：标准列表页+编辑弹窗）
- **实际按钮**：同 CRUD 基线
- **差距**：无
- **判定**：**clean**

### ErpInvDrpCrossDock — CRUD

- **期望按钮**：CRUD 基线（辅助实体，设计文档未提及专用按钮）
- **实际按钮**：同 CRUD 基线
- **差距**：无
- **判定**：**clean**

### ErpInvDrpDockAppointment — CRUD

- **期望按钮**：CRUD 基线（辅助实体，设计文档未提及专用按钮）
- **实际按钮**：同 CRUD 基线
- **差距**：无
- **判定**：**clean**

### ErpInvDrpLeadTimeRecord — CRUD

- **期望按钮**：CRUD 基线（辅助实体，设计文档未提及专用按钮）
- **实际按钮**：同 CRUD 基线
- **差距**：无
- **判定**：**clean**

### ErpInvDrpSafetyStockCalc — CRUD

- **期望按钮**：CRUD 基线（辅助实体，设计文档未提及专用按钮）
- **实际按钮**：同 CRUD 基线
- **差距**：无
- **判定**：**clean**

## 摘要

| 分类 | 实体 | 差距数 | 最高严重级 | 备注 |
|------|------|--------|-----------|------|
| CRUD (期望 CRUD+Custom) | ErpDrpPlan | 3 | blocker | run-drp-button / approve-all-button / generate-order-button 缺失 |
| CRUD (期望 CRUD+WF) | ErpDrpLine | 3 | blocker | row-approve-button 缺失（blocker）；row-reject-button / row-cancel-button 缺失（major） |
| CRUD | ErpDrpParameter | 0 | clean | — |
| CRUD | ErpInvDrpCrossDock | 0 | clean | — |
| CRUD | ErpInvDrpDockAppointment | 0 | clean | — |
| CRUD | ErpInvDrpLeadTimeRecord | 0 | clean | — |
| CRUD | ErpInvDrpSafetyStockCalc | 0 | clean | — |

### 总评

- 总实体数：7
- 无差距实体：5（71.4%）
- Blocker 差距：2 实体（ErpDrpPlan、ErpDrpLine），合计 4 个 blocker 差距
- Major 差距：1 实体（ErpDrpLine），合计 2 个 major 差距
- Minor/Info 差距：0

**核心结论**：DRP 域的两个核心业务实体（ErpDrpPlan、ErpDrpLine）的 view.xml 页面仍处于"纯 codegen CRUD"阶段，尚未接入任何域专用业务按钮。ErpDrpPlan 缺少运行 DRP 计算、批量批准、生成单据三个核心操作按钮；ErpDrpLine 缺少行级批准/驳回/取消三个审批流按钮。五个辅助/配置实体（包括 ErpDrpParameter 和四个 ErpInvDrp* 实体）的 CRUD 基线完整，无差距。
