# 项目管理域（projects）视图按钮需求覆盖分析

## 分析范围

本域共 16 实体，覆盖全部生成的 view.xml（`_dump/nop-app/erp/prj/pages/`）：

| 实体 | 分类 | 依据 |
|------|------|------|
| ErpPrjProject | CRUD+Custom | 主业务实体，state-machine.md 定义 DRAFT/OPEN/ON_HOLD/COMPLETED/CANCELLED 五态生命周期 |
| ErpPrjTask | CRUD+Custom | 主业务实体，state-machine.md 定义 TODO/IN_PROGRESS/DONE/BLOCKED 四态生命周期 |
| ErpPrjTimesheet | CRUD+Custom | 工时记录，ui-patterns.md 规定 [提交] 操作 |
| ErpPrjBudget | CRUD | 项目预算（项目子表），无独立状态机/WF |
| ErpPrjBudgetLine | CRUD | 预算明细行，无独立状态机/WF |
| ErpPrjMilestone | CRUD | 里程碑（项目子表），无独立状态机/WF |
| ErpPrjProjectUser | CRUD | 项目成员关联（资源分配），无独立状态机/WF |
| ErpPrjProjectType | CRUD | 项目分类字典，纯配置 |
| ErpPrjActivityType | CRUD | 活动类型字典，纯配置 |
| ErpPrjBilling | CRUD | 项目开票记录，无独立状态机/WF |
| ErpPrjBillingLine | CRUD | 开票明细行 |
| ErpPrjCostCollection | CRUD | 成本归集记录（工时→成本凭证结果），无独立状态机 |
| ErpPrjCostCollectionLine | CRUD | 成本归集明细 |
| ErpPrjProjectPnl | CRUD | 项目损益（计算聚合表），无独立状态机 |
| ErpPrjProjectSettlement | CRUD | 项目结算记录，无独立状态机 |
| ErpPrjProjectSettlementLine | CRUD | 结算明细 |

## 期望按钮推导依据

1. **CRUD 基线**（METHODOLOGY §1.1）：所有实体期望 toolbar `add-button`/`batch-delete-button` + row `row-view-button`/`row-update-button`/`row-delete-button`。
2. **state-machine.md §适用对象一（项目）**：定义 5 状态 + 5 迁移（DRAFT→OPEN 立项, OPEN→ON_HOLD 暂停, ON_HOLD→OPEN 恢复, OPEN→COMPLETED 完成, →CANCELLED 取消），期望对应生命周期按钮。
3. **state-machine.md §适用对象二（任务）**：定义 4 状态 + 4 迁移（TODO→IN_PROGRESS 开始, IN_PROGRESS→DONE 完成, IN_PROGRESS→BLOCKED 阻塞, BLOCKED→IN_PROGRESS 解除阻塞），期望对应生命周期按钮。
4. **ui-patterns.md §工时录入**：预期录入页底部含 `[提交]` 按钮，映射为 `row-submit-button`（METHODOLOGY §2 翻译规则）。
5. **ui-patterns.md §跨页面导航流**：`[新建项目]` 已由 CRUD `add-button` 覆盖；`[拖拽变更状态]` 和 `[点击卡片编辑]` 属看板视图交互，非 list 页 action 按钮，不记为差距。

## 逐实体分析

### ErpPrjProject — CRUD+Custom

- **期望按钮**：
  - toolbar: `add-button`, `batch-delete-button`
  - row: `row-view-button`, `row-update-button`, `row-delete-button`
  - row: `row-start-button`（DRAFT→OPEN 立项，来源 state-machine.md §2）
  - row: `row-hold-button`（OPEN→ON_HOLD 暂停，来源 state-machine.md §2）
  - row: `row-resume-button`（ON_HOLD→OPEN 恢复，来源 state-machine.md §2）
  - row: `row-complete-button`（OPEN→COMPLETED 完成，来源 state-machine.md §2）
  - row: `row-cancel-button`（→CANCELLED 取消，来源 state-machine.md §2）
- **实际按钮**：CRUD 基线 5 按钮（add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button）
- **差距**：
  - `row-start-button`: missing (blocker) — state-machine.md 定义"立项"迁移，无按钮无法从 DRAFT 进入 OPEN
  - `row-hold-button`: missing (blocker) — state-machine.md 定义"暂停"迁移，无按钮无法暂停执行中项目
  - `row-resume-button`: missing (blocker) — state-machine.md 定义"恢复"迁移，无按钮无法从 ON_HOLD 恢复
  - `row-complete-button`: missing (blocker) — state-machine.md 定义"完成"迁移，无按钮无法将项目标记完成
  - `row-cancel-button`: missing (blocker) — state-machine.md 定义 CANCELLED 终态，无按钮无法取消项目
- **判定**：blocker（5 个核心生命周期按钮全部缺失，项目状态机不可操作）

### ErpPrjTask — CRUD+Custom

- **期望按钮**：
  - toolbar: `add-button`, `batch-delete-button`
  - row: `row-view-button`, `row-update-button`, `row-delete-button`
  - row: `row-start-button`（TODO→IN_PROGRESS 开始，来源 state-machine.md §适用对象二）
  - row: `row-complete-button`（IN_PROGRESS→DONE 完成，来源 state-machine.md §适用对象二）
  - row: `row-block-button`（IN_PROGRESS→BLOCKED 阻塞，来源 state-machine.md §适用对象二）
  - row: `row-unblock-button`（BLOCKED→IN_PROGRESS 解除阻塞，来源 state-machine.md §适用对象二）
- **实际按钮**：CRUD 基线 5 按钮
- **差距**：
  - `row-start-button`: missing (blocker) — state-machine.md 定义"开始"迁移，无按钮无法启动任务
  - `row-complete-button`: missing (blocker) — state-machine.md 定义"完成"迁移，无按钮无法完成任务
  - `row-block-button`: missing (blocker) — state-machine.md 定义"阻塞"迁移，无按钮无法标记阻塞
  - `row-unblock-button`: missing (blocker) — state-machine.md 定义"解除阻塞"迁移，无按钮无法解除阻塞
- **判定**：blocker（4 个核心生命周期按钮全部缺失，任务状态机不可操作）

### ErpPrjTimesheet — CRUD+Custom

- **期望按钮**：
  - toolbar: `add-button`, `batch-delete-button`
  - row: `row-view-button`, `row-update-button`, `row-delete-button`
  - row: `row-submit-button`（提交，来源 ui-patterns.md §工时录入："[提交] [保存草稿]"）
- **实际按钮**：CRUD 基线 5 按钮
- **差距**：
  - `row-submit-button`: missing (blocker) — ui-patterns.md 工时录入页面明确展示 [提交] 按钮；提交后触发成本归集（README §工时计入成本）
- **判定**：blocker（ui-patterns.md 明确列举的提交按钮缺失，工时提交流程无法触发）

### ErpPrjBudget — CRUD

- **期望按钮**：CRUD 基线（add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button）
- **实际按钮**：CRUD 基线（全部存在）
- **差距**：无
- **判定**：clean

### ErpPrjBudgetLine — CRUD

- **期望按钮**：CRUD 基线
- **实际按钮**：CRUD 基线
- **差距**：无
- **判定**：clean

### ErpPrjMilestone — CRUD

- **期望按钮**：CRUD 基线
- **实际按钮**：CRUD 基线
- **差距**：无
- **判定**：clean

### ErpPrjProjectUser — CRUD

- **期望按钮**：CRUD 基线
- **实际按钮**：CRUD 基线
- **差距**：无
- **判定**：clean

### ErpPrjProjectType — CRUD

- **期望按钮**：CRUD 基线
- **实际按钮**：CRUD 基线
- **差距**：无
- **判定**：clean

### ErpPrjActivityType — CRUD

- **期望按钮**：CRUD 基线
- **实际按钮**：CRUD 基线
- **差距**：无
- **判定**：clean

### ErpPrjBilling — CRUD

- **期望按钮**：CRUD 基线
- **实际按钮**：CRUD 基线
- **差距**：无
- **判定**：clean

### ErpPrjBillingLine — CRUD

- **期望按钮**：CRUD 基线
- **实际按钮**：CRUD 基线
- **差距**：无
- **判定**：clean

### ErpPrjCostCollection — CRUD

- **期望按钮**：CRUD 基线
- **实际按钮**：CRUD 基线
- **差距**：无
- **判定**：clean

### ErpPrjCostCollectionLine — CRUD

- **期望按钮**：CRUD 基线
- **实际按钮**：CRUD 基线
- **差距**：无
- **判定**：clean

### ErpPrjProjectPnl — CRUD

- **期望按钮**：CRUD 基线
- **实际按钮**：CRUD 基线
- **差距**：无
- **判定**：clean

### ErpPrjProjectSettlement — CRUD

- **期望按钮**：CRUD 基线
- **实际按钮**：CRUD 基线
- **差距**：无
- **判定**：clean

### ErpPrjProjectSettlementLine — CRUD

- **期望按钮**：CRUD 基线
- **实际按钮**：CRUD 基线
- **差距**：无
- **判定**：clean

## 摘要

| 分类 | 实体 | 差距数 | 最高严重级 | 备注 |
|------|------|--------|-----------|------|
| CRUD+Custom | ErpPrjProject | 5 | blocker | 缺少全部生命周期按钮（立项/暂停/恢复/完成/取消）；state-machine.md §2 |
| CRUD+Custom | ErpPrjTask | 4 | blocker | 缺少全部生命周期按钮（开始/完成/阻塞/解除阻塞）；state-machine.md §适用对象二 |
| CRUD+Custom | ErpPrjTimesheet | 1 | blocker | 缺少 [提交] 按钮；ui-patterns.md §工时录入 |
| CRUD | ErpPrjBudget | 0 | clean | — |
| CRUD | ErpPrjBudgetLine | 0 | clean | — |
| CRUD | ErpPrjMilestone | 0 | clean | — |
| CRUD | ErpPrjProjectUser | 0 | clean | — |
| CRUD | ErpPrjProjectType | 0 | clean | — |
| CRUD | ErpPrjActivityType | 0 | clean | — |
| CRUD | ErpPrjBilling | 0 | clean | — |
| CRUD | ErpPrjBillingLine | 0 | clean | — |
| CRUD | ErpPrjCostCollection | 0 | clean | — |
| CRUD | ErpPrjCostCollectionLine | 0 | clean | — |
| CRUD | ErpPrjProjectPnl | 0 | clean | — |
| CRUD | ErpPrjProjectSettlement | 0 | clean | — |
| CRUD | ErpPrjProjectSettlementLine | 0 | clean | — |

### 总评

- 总实体数：16
- 无差距实体：13（81.25%）
- Blocker 差距：3 实体（Project, Task, Timesheet），共 10 个缺失按钮
- Major 差距：0
- Minor/Info 差距：0

**核心发现**：所有 16 实体 view.xml 均为纯代码生成 CRUD 页面，仅含 CRUD 基线 5 按钮。**3 个主业务实体**（Project/Task/Timesheet）缺少来自 `state-machine.md` 和 `ui-patterns.md` 定义的核心操作按钮。13 个辅助/配置实体无差距。

建议按优先级补充：
1. ErpPrjProject — 实现 5 个生命周期 BizModel `@BizMutation`（startProject/hold/resume/complete/cancel），对应 view.xml row 按钮
2. ErpPrjTask — 实现 4 个生命周期 BizModel `@BizMutation`（startTask/completeTask/blockTask/unblockTask），对应 view.xml row 按钮
3. ErpPrjTimesheet — 实现 `row-submit-button`，提交后触发成本归集流程
