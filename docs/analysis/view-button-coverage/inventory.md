# 库存域（inventory）视图按钮需求覆盖分析

## 分析范围

21 实体，覆盖完整 18+1 域中的库存域：

| 实体 | 分类 |
|------|------|
| ErpInvStockMove | CRUD+WF 预期 → 实际 CRUD |
| ErpInvStockMoveLine | CRUD |
| ErpInvStockLedger | 只读查询（设计强制）→ 实际 CRUD |
| ErpInvStockBalance | 只读查询 → 实际 CRUD |
| ErpInvStockTake | CRUD+WF+Custom 预期 → 实际 CRUD |
| ErpInvStockTakeLine | CRUD |
| ErpInvTransferOrder | CRUD+WF 预期 → 实际 CRUD |
| ErpInvTransferOrderLine | CRUD |
| ErpInvBatch | 台账查询（只读）→ 实际 CRUD |
| ErpInvSerialNumber | 台账查询（只读）→ 实际 CRUD |
| ErpInvCostAdjust | CRUD+WF（全审批流） |
| ErpInvCostAdjustLine | CRUD |
| ErpInvCostLayer | CRUD（系统管理成本层） |
| ErpInvLandedCost | CRUD+Custom（审批+分摊预览） |
| ErpInvLandedCostLine | CRUD |
| ErpInvOwnershipTransfer | CRUD |
| ErpInvOwnershipTransferLine | CRUD |
| ErpInvPickingOrder | CRUD |
| ErpInvPickingOrderLine | CRUD |
| ErpInvReservation | CRUD |
| ErpInvReservationLine | CRUD |

## 期望按钮推导依据

- **CRUD 基线**（METHODOLOGY §1.1）：全实体默认 toolbar={add, batch-delete} + row={view, update, delete}
- **库存移动单状态机**（`state-machine.md` §2）：DRAFT→CONFIRMED→DONE/CANCELLED，预期 row-submit-button + row-cancel-button
- **盘点单状态机**（`state-machine.md` §10）：DRAFT→COUNTING→DONE/CANCELLED，预期 row-cancel-button + 域专用"开始盘点"/"完成盘点"按钮
- **不可变流水设计**（`ui-patterns.md` §2）："库存流水页面纯只读（不可变），无编辑/删除操作，仅支持查询和导出"
- **库存余额设计**（`ui-patterns.md` §3）："库存余额查询"——只读多维筛选与展示
- **批次/序列号台账设计**（`ui-patterns.md` §7）："批次台账查询"、"序列号台账查询"——只读查询
- **调拨单导航**（`ui-patterns.md` 导航流）："调拨单 → [审核] → 自动生成出入库移动单"——预期审核/确认按钮
- **domain-design-guidelines.md §16.2**：库存域作业单（移动单/盘点单）无审批轴，作业确认即生效——预期 submit/confirm 而非 approve/reject

## 逐实体分析

### ErpInvStockMove — CRUD（预期 CRUD+WF）
- **期望按钮**：CRUD 基线 + row-submit-button（DRAFT→CONFIRMED，`state-machine.md` §2 "提交确认"）+ row-cancel-button（→CANCELLED，`state-machine.md` §2）
- **实际按钮**：CRUD 基线（add, batch-delete, view, update, delete）
- **差距**：
  - row-submit-button: missing (**blocker**) — 库存域核心单据，状态机核心迁移 DRAFT→CONFIRMED 无法通过 UI 触发。`state-machine.md` §2 "提交确认"明确列出此迁移
  - row-cancel-button: missing (**blocker**) — DRAFT 或 CONFIRMED→CANCELLED 终态迁移无对应 UI 按钮。`state-machine.md` §2 草稿/已确认均可达已取消
- **判定**：**blocker** — 移动单是库存域核心操作对象，缺少两个核心工作流按钮意味着完整流程无法通过 UI 完成

### ErpInvStockMoveLine — CRUD
- **期望按钮**：CRUD 基线
- **实际按钮**：CRUD 基线
- **差距**：无
- **判定**：**clean**（子表行数据，在父表单中管理，独立 CRUD 为框架标准）

### ErpInvStockLedger — CRUD（设计强制只读）
- **期望按钮**：{row-view-button} 仅查看（`ui-patterns.md` §2 "不可变流水只读：库存流水页面纯只读（不可变），无编辑/删除操作"）
- **实际按钮**：CRUD 基线全量（add, batch-delete, view, update, delete）
- **差距**：
  - add-button: extra (**blocker**) — 设计明确禁止新建流水。流水由移动单 DONE 自动写入，不应有独立 add-button
  - batch-delete-button: extra (**blocker**) — 流水不可删除
  - row-update-button: extra (**blocker**) — 流水不可编辑/修改
  - row-delete-button: extra (**blocker**) — 流水不可删除
  - `ui-patterns.md` §2 原则 4："不可变流水只读"。此违规允许用户通过 UI 直接篡改审计关键数据
- **判定**：**blocker** — 系统设计硬约束被绕过

### ErpInvStockBalance — CRUD（预期只读查询）
- **期望按钮**：{row-view-button} 仅查看（`ui-patterns.md` §3 "库存余额查询"——只读查询页面，仅支持多维筛选与展示）
- **实际按钮**：CRUD 基线全量（add, batch-delete, view, update, delete）
- **差距**：
  - add-button: extra (**major**) — 余额由流水驱动更新，不应手动新建
  - batch-delete-button: extra (**major**) — 余额是实时快照，不可删除
  - row-update-button: extra (**major**) — 余额不可手动编辑
  - row-delete-button: extra (**major**) — 余额不可删除
  - 对比 StockLedger 无显式 "不可编辑" 文字，但类型标注为"只读查询"，不应有 CRUD 动作
- **判定**：**major** — 只读查询页面展示了完整 CRUD，违反设计意图

### ErpInvStockTake — CRUD（预期 CRUD+WF+Custom）
- **期望按钮**：CRUD 基线 + row-cancel-button（DRAFT/COUNTING→CANCELLED，`state-machine.md` §10）+ 域专用按钮（`ui-patterns.md` §6 "盘点单 → [开始] → [录入盘点] → [审核]"）
- **实际按钮**：CRUD 基线（add, batch-delete, view, update, delete）
- **差距**：
  - 域专用"开始盘点"按钮（DRAFT→COUNTING）: missing (**blocker**) — `state-machine.md` §10 和 `ui-patterns.md` §6 明确描述的盘点流程首步
  - 域专用"完成盘点"按钮（COUNTING→DONE）: missing (**blocker**) — 盘点录入完成后的确认动作
  - row-cancel-button: missing (**major**) — `state-machine.md` §10 草稿/盘点中均可取消
  - 盘点单完全缺乏工作流按钮，无法推进 DRAFT→COUNTING→DONE 生命周期，等同于仅能创建草稿
- **判定**：**blocker** — 盘点流程的完整状态机无法通过 UI 执行

### ErpInvStockTakeLine — CRUD
- **期望按钮**：CRUD 基线
- **实际按钮**：CRUD 基线
- **差距**：无
- **判定**：**clean**（子表行）

### ErpInvTransferOrder — CRUD（预期 CRUD+WF）
- **期望按钮**：CRUD 基线 + row-submit-button 或 row-approve-button（`ui-patterns.md` 导航流 "调拨单 → [审核] → 自动生成出入库移动单"）
- **实际按钮**：CRUD 基线（add, batch-delete, view, update, delete）
- **差距**：
  - row-submit-button / row-approve-button: missing (**major**) — `ui-patterns.md` 明确描述调拨单需经审核后自动生成两张出入库移动单，实际只有 CRUD 占位
- **判定**：**major** — 调拨单缺少核心业务触发按钮

### ErpInvTransferOrderLine — CRUD
- **期望按钮**：CRUD 基线
- **实际按钮**：CRUD 基线
- **差距**：无
- **判定**：**clean**（子表行）

### ErpInvBatch — CRUD（预期台账只读查询）
- **期望按钮**：{row-view-button} 仅查看（`ui-patterns.md` §7 "批次台账查询"——只读台账查询）
- **实际按钮**：CRUD 基线全量（add, batch-delete, view, update, delete）
- **差距**：
  - add-button: extra (**major**) — 批次由移动单流转自动创建，不应手动新建
  - batch-delete-button: extra (**major**) — 台账不可删除
  - row-update-button: extra (**major**) — 台账不可编辑
  - row-delete-button: extra (**major**) — 台账不可删除
- **判定**：**major** — 台账查询页呈现完整 CRUD，违反只读设计

### ErpInvSerialNumber — CRUD（预期台账只读查询）
- **期望按钮**：{row-view-button} 仅查看（`ui-patterns.md` §7 "序列号台账查询"——只读台账查询）
- **实际按钮**：CRUD 基线全量（add, batch-delete, view, update, delete）
- **差距**：
  - add-button: extra (**major**) — 序列号由移动单流转自动创建
  - batch-delete-button: extra (**major**) — 台账不可删除
  - row-update-button: extra (**major**) — 台账不可编辑
  - row-delete-button: extra (**major**) — 台账不可删除
- **判定**：**major** — 台账查询页呈现完整 CRUD，违反只读设计

### ErpInvCostAdjust — CRUD+WF
- **期望按钮**：CRUD 基线 + 审批流全套（submit, withdraw-approval, approve, reject, reverse-approve）——实体具有 approveStatus 和 posted 字段，隐含审批流程
- **实际按钮**：CRUD 基线 + row-submit-button, row-withdraw-approval-button, row-approve-button, row-reject-button, row-reverse-approve-button, row-delete-button
- **差距**：无（全审批流按钮覆盖完备，含 visibility 条件判断，`visibleOn` 表达式正确绑定 approveStatus 状态）
- **判定**：**clean**。注意：`state-machine.md` 未覆盖 CostAdjust 状态机，建议在后续补充设计文档。

### ErpInvCostAdjustLine — CRUD
- **期望按钮**：CRUD 基线
- **实际按钮**：CRUD 基线
- **差距**：无
- **判定**：**clean**（子表行）

### ErpInvCostLayer — CRUD
- **期望按钮**：CRUD 基线
- **实际按钮**：CRUD 基线
- **差距**：无（成本层为系统内部管理的 FIFO 分层数据结构，CRUD 由系统接口而非 UI 操作。现有 CRUD 基线属于标准 codegen 产物，不阻塞业务）
- **判定**：**clean**（系统内部实体，CRUD 不影响业务操作）

### ErpInvLandedCost — CRUD+Custom
- **期望按钮**：CRUD 基线 + row-approve-button（到岸成本审核触发分摊）+ row-allocate-preview-button（分摊预览）
- **实际按钮**：CRUD 基线 + row-approve-button, row-allocate-preview-button, row-delete-button
- **差距**：无（审批 + 分摊预览按钮完备）
- **判定**：**clean**。注意：`state-machine.md` 未覆盖 LandedCost 状态机，无显式设计文档描述其审批流程与状态迁移。现有按钮实现合理，建议补充设计覆盖。

### ErpInvLandedCostLine — CRUD
- **期望按钮**：CRUD 基线
- **实际按钮**：CRUD 基线
- **差距**：无
- **判定**：**clean**（子表行）

### ErpInvOwnershipTransfer — CRUD
- **期望按钮**：CRUD 基线（无明确状态机设计；`consignment.md` 描述业务语义但未定义 UI 按钮级别操作）
- **实际按钮**：CRUD 基线
- **差距**：无
- **判定**：**clean**（基础 CRUD，无设计文档要求更多按钮）

### ErpInvOwnershipTransferLine — CRUD
- **期望按钮**：CRUD 基线
- **实际按钮**：CRUD 基线
- **差距**：无
- **判定**：**clean**（子表行）

### ErpInvPickingOrder — CRUD
- **期望按钮**：CRUD 基线（无状态机/UI 设计覆盖）
- **实际按钮**：CRUD 基线
- **差距**：无
- **判定**：**clean**（基础 CRUD）

### ErpInvPickingOrderLine — CRUD
- **期望按钮**：CRUD 基线
- **实际按钮**：CRUD 基线
- **差距**：无
- **判定**：**clean**（子表行）

### ErpInvReservation — CRUD
- **期望按钮**：CRUD 基线（预留量由系统管理，但 UI 人工预留/释放有业务意义）
- **实际按钮**：CRUD 基线
- **差距**：无
- **判定**：**clean**（基础 CRUD）

### ErpInvReservationLine — CRUD
- **期望按钮**：CRUD 基线
- **实际按钮**：CRUD 基线
- **差距**：无
- **判定**：**clean**（子表行）

## 摘要

| 分类 | 实体 | 差距数 | 最高严重级 | 备注 |
|------|------|--------|-----------|------|
| CRUD（预期 CRUD+WF） | ErpInvStockMove | 2 | blocker | 缺少 submit + cancel 按钮，核心状态机不可达 |
| CRUD（预期只读查询） | ErpInvStockLedger | 4 | blocker | 设计强制不可变流水，实际有完整 CRUD |
| CRUD（预期 CRUD+WF+Custom） | ErpInvStockTake | 3 | blocker | 缺少开始盘点/完成盘点/取消，完整盘点流程不可达 |
| CRUD（预期只读查询） | ErpInvStockBalance | 4 | major | 只读余额查询页有完整 CRUD |
| CRUD（预期 CRUD+WF） | ErpInvTransferOrder | 1 | major | 缺少审核按钮 |
| CRUD（预期台账只读） | ErpInvBatch | 4 | major | 批次台账查询有完整 CRUD |
| CRUD（预期台账只读） | ErpInvSerialNumber | 4 | major | 序列号台账查询有完整 CRUD |
| CRUD+WF | ErpInvCostAdjust | 0 | clean | 全审批流完备 |
| CRUD+Custom | ErpInvLandedCost | 0 | clean | 审批+分摊预览完备 |
| CRUD | ErpInvCostAdjustLine | 0 | clean | 子表行 |
| CRUD | ErpInvCostLayer | 0 | clean | 系统内部实体 |
| CRUD | ErpInvLandedCostLine | 0 | clean | 子表行 |
| CRUD | ErpInvOwnershipTransfer | 0 | clean | 基础 CRUD |
| CRUD | ErpInvOwnershipTransferLine | 0 | clean | 子表行 |
| CRUD | ErpInvPickingOrder | 0 | clean | 基础 CRUD |
| CRUD | ErpInvPickingOrderLine | 0 | clean | 子表行 |
| CRUD | ErpInvReservation | 0 | clean | 基础 CRUD |
| CRUD | ErpInvReservationLine | 0 | clean | 子表行 |
| CRUD | ErpInvStockMoveLine | 0 | clean | 子表行 |
| CRUD | ErpInvStockTakeLine | 0 | clean | 子表行 |
| CRUD | ErpInvTransferOrderLine | 0 | clean | 子表行 |

### 总评
- 总实体数：21
- 无差距实体：13（61.9%）
- Blocker 差距：3 实体（ErpInvStockMove, ErpInvStockLedger, ErpInvStockTake）
- Major 差距：4 实体（ErpInvStockBalance, ErpInvTransferOrder, ErpInvBatch, ErpInvSerialNumber）
- Minor/Info 差距：0

**库存域按钮覆盖存在 3 个 blocker 和 4 个 major 问题**。核心业务实体（StockMove, StockTake）完全缺乏状态迁移按钮，用户只能创建草稿但无法推进流程。三个设计为只读的查询页面（StockLedger、Batch、SerialNumber）和 StockBalance 展示了完整 CRUD，其中 StockLedger 的 CRUD 按钮违反了"不可变流水"的设计硬约束。调拨单缺少审核触发按钮，导致无法自动生成出入库移动单。
