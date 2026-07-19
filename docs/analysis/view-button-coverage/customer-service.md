# 客服工单域 (customer-service) 视图按钮需求覆盖分析

## 分析范围

CS 域共 16 实体，按实际按钮格局分类：

| 分类 | 实体 | 数量 |
|------|------|------|
| CRUD | ErpCsAgentRate, ErpCsCannedCategory, ErpCsCannedResponse, ErpCsCatalogCategory, ErpCsCatalogFulfillment, ErpCsContract, ErpCsEntitlement, ErpCsServiceCatalogItem, ErpCsTeam, ErpCsTicketAction, ErpCsTicketType, ErpCsTimeEntry | 12 |
| CRUD (info gap) | ErpCsKnowledgeBase, ErpCsSlaPolicy, ErpCsSurvey | 3 |
| CRUD (should be CRUD+WF) | ErpCsTicket | 1 |

## 期望按钮推导依据

- **CRUD 基线**：METHODOLOGY §1.1 — 所有实体默认期望 toolbar {add-button, batch-delete-button} + row {row-view-button, row-update-button, row-delete-button}。
- **cs 工单状态机**：`customer-service/README.md` 定义状态流 NEW→ASSIGNED→IN_PROGRESS→RESOLVED→CLOSED/CANCELLED。
- **工单状态驱动按钮**：`ui-patterns.md §列表页要点`（第 57 行）明确列举状态驱动的行级操作按钮——NEW: [分派][取消], ASSIGNED: [开始处理][重新分派], IN_PROGRESS: [解决][升级]。
- **详情页工具栏**：`ui-patterns.md §详情页通用结构`（第 67 行）列出 [分配] [开始处理] [标记解决] [取消] [打印] [创建知识库文章] [操作历史]。
- **知识库**：`ui-patterns.md §知识库管理`（第 200 行）工具栏含 [新建文章] [管理分类] [导入] [导出]。

## 逐实体分析

### ErpCsAgentRate — CRUD
- **期望按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**：无
- **判定**：clean

### ErpCsCannedCategory — CRUD
- **期望按钮**：CRUD 基线
- **实际按钮**：CRUD 基线
- **差距**：无
- **判定**：clean

### ErpCsCannedResponse — CRUD
- **期望按钮**：CRUD 基线
- **实际按钮**：CRUD 基线
- **差距**：无
- **判定**：clean

### ErpCsCatalogCategory — CRUD
- **期望按钮**：CRUD 基线
- **实际按钮**：CRUD 基线
- **差距**：无
- **判定**：clean

### ErpCsCatalogFulfillment — CRUD
- **期望按钮**：CRUD 基线
- **实际按钮**：CRUD 基线
- **差距**：无
- **判定**：clean

### ErpCsContract — CRUD
- **期望按钮**：CRUD 基线
- **实际按钮**：CRUD 基线
- **差距**：无
- **判定**：clean

### ErpCsEntitlement — CRUD
- **期望按钮**：CRUD 基线
- **实际按钮**：CRUD 基线
- **差距**：无
- **判定**：clean

### ErpCsKnowledgeBase — CRUD (info gap)
- **期望按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**：
  - toolbar missing `[导入]`/`[导出]` (info) — ui-patterns.md §知识库管理（第 200 行）工具栏显式列出 [导入] 和 [导出]，但 view.xml 无对应按钮。按 METHODOLOGY §2 规则，[导入]/[导出] 记为 info 级差距。
- **判定**：info（可增强点，非阻塞）

### ErpCsServiceCatalogItem — CRUD
- **期望按钮**：CRUD 基线
- **实际按钮**：CRUD 基线
- **差距**：无
- **判定**：clean

### ErpCsSlaPolicy — CRUD
- **期望按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**：无
- **判定**：clean

### ErpCsSurvey — CRUD
- **期望按钮**：CRUD 基线（状态由 surveySentAt/respondedAt 派生，无独立工作流按钮）
- **实际按钮**：CRUD 基线
- **差距**：无
- **判定**：clean

### ErpCsTeam — CRUD
- **期望按钮**：CRUD 基线
- **实际按钮**：CRUD 基线
- **差距**：无
- **判定**：clean

### ErpCsTicket — CRUD (should be CRUD+WF)
- **期望按钮**：
  - CRUD 基线：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
  - 状态驱动行级按钮（`ui-patterns.md §列表页要点`第 57 行）：
    - NEW 状态：`row-assign-button`（分派）, `row-cancel-button`（取消）
    - ASSIGNED 状态：`row-start-button`（开始处理）, `row-reassign-button`（重新分派）
    - IN_PROGRESS 状态：`row-resolve-button`（解决）, `row-escalate-button`（升级）
- **实际按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button（仅 CRUD 基线）
- **差距**：
  - `row-assign-button`: missing (blocker) — ui-patterns.md 明确说明 NEW 显示 [分派]，customer-service/README.md 状态机需此按钮驱动 NEW→ASSIGNED
  - `row-cancel-button`: missing (blocker) — ui-patterns.md 明确说明 NEW 显示 [取消]，状态机允许 NEW→CANCELLED
  - `row-start-button`: missing (blocker) — ui-patterns.md 明确说明 ASSIGNED 显示 [开始处理]，驱动 ASSIGNED→IN_PROGRESS
  - `row-reassign-button`: missing (blocker) — ui-patterns.md 明确说明 ASSIGNED 显示 [重新分派]
  - `row-resolve-button`: missing (blocker) — ui-patterns.md 明确说明 IN_PROGRESS 显示 [解决]，驱动 IN_PROGRESS→RESOLVED
  - `row-escalate-button`: missing (blocker) — ui-patterns.md 明确说明 IN_PROGRESS 显示 [升级]
- **判定**：blocker（6 个核心业务按钮全部缺失，工单状态流转无法在列表页操作）

### ErpCsTicketAction — CRUD
- **期望按钮**：CRUD 基线（操作日志实体，无特殊业务按钮预期）
- **实际按钮**：CRUD 基线
- **差距**：无
- **判定**：clean

### ErpCsTicketType — CRUD
- **期望按钮**：CRUD 基线
- **实际按钮**：CRUD 基线
- **差距**：无
- **判定**：clean

### ErpCsTimeEntry — CRUD
- **期望按钮**：CRUD 基线
- **实际按钮**：CRUD 基线
- **差距**：无
- **判定**：clean

## 摘要

| 分类 | 实体 | 差距数 | 最高严重级 | 备注 |
|------|------|--------|-----------|------|
| CRUD+WF | ErpCsTicket | 6 | blocker | 6 个状态驱动按钮全部缺失 |
| CRUD | ErpCsKnowledgeBase | 2 | info | 缺少 [导入][导出] |
| CRUD | ErpCsAgentRate | 0 | clean | |
| CRUD | ErpCsCannedCategory | 0 | clean | |
| CRUD | ErpCsCannedResponse | 0 | clean | |
| CRUD | ErpCsCatalogCategory | 0 | clean | |
| CRUD | ErpCsCatalogFulfillment | 0 | clean | |
| CRUD | ErpCsContract | 0 | clean | |
| CRUD | ErpCsEntitlement | 0 | clean | |
| CRUD | ErpCsServiceCatalogItem | 0 | clean | |
| CRUD | ErpCsSlaPolicy | 0 | clean | |
| CRUD | ErpCsSurvey | 0 | clean | |
| CRUD | ErpCsTeam | 0 | clean | |
| CRUD | ErpCsTicketAction | 0 | clean | |
| CRUD | ErpCsTicketType | 0 | clean | |
| CRUD | ErpCsTimeEntry | 0 | clean | |

### 总评
- 总实体数：16
- 无差距实体：13（81.25%）
- Blocker 差距：1 实体（ErpCsTicket × 6 个缺失按钮）
- Major 差距：0
- Minor 差距：0
- Info 差距：1 实体（ErpCsKnowledgeBase × 2）

**核心问题**：ErpCsTicket 作为客服工单域的核心业务流程实体，缺少所有状态驱动操作按钮。ui-patterns.md 明确要求的 6 个按钮（分派/取消/开始处理/重新分派/解决/升级）在 view.xml 中完全不存在。工单列表页仅支持 CRUD 操作，无法进行状态流转。建议优先在 ErpCsTicket 的 main crud 页 rowActions 中添加条件渲染的状态按钮，后端对应 `@BizMutation` 方法已在实现计划中（`docs/plans/2026-07-04-0700-2-cs-ticket-sla-csat.md`）。
