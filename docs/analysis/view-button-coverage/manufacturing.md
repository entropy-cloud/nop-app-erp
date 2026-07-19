# Manufacturing 视图按钮需求覆盖分析

## 分析范围

| 实体 | 分类 | 说明 |
|------|------|------|
| ErpMfgWorkOrder | CRUD+Custom | 工单（核心业务单据，带审批流+生产执行流） |
| ErpMfgSubcontractOrder | CRUD+Custom | 委外加工单（带审批流+发料/收货/加工费过账） |
| ErpMfgJobCard | CRUD | 工序作业卡（有独立 8 态状态机，但仅 CRUD 按钮） |
| ErpMfgBom | CRUD | BOM 定义（标准 CRUD） |
| ErpMfgBomLine | CRUD | BOM 子件子表（标准 CRUD） |
| ErpMfgBomByproduct | CRUD | BOM 联副产品（标准 CRUD） |
| ErpMfgBomOperation | CRUD | BOM 工艺（标准 CRUD） |
| ErpMfgRouting | CRUD | 工艺路线（标准 CRUD） |
| ErpMfgRoutingOperation | CRUD | 工艺路线工序（标准 CRUD） |
| ErpMfgWorkOrderLine | CRUD | 工单行（标准 CRUD） |
| ErpMfgMaterialIssue | CRUD | 领料单（标准 CRUD） |
| ErpMfgMaterialIssueLine | CRUD | 领料单行（标准 CRUD） |
| ErpMfgJobCardTimeLog | CRUD | 作业卡工时记录（标准 CRUD） |
| ErpMfgCostRollup | CRUD | 标准成本滚算（标准 CRUD） |
| ErpMfgCostRollupLine | CRUD | 成本滚算行（标准 CRUD） |
| ErpMfgCostVariance | CRUD | 成本差异（标准 CRUD） |
| ErpMfgMrpPlan | CRUD | MRP 计划（标准 CRUD） |
| ErpMfgMrpPlanLine | CRUD | MRP 计划行（标准 CRUD） |
| ErpMfgMrpDemand | CRUD | MRP 需求（标准 CRUD） |
| ErpMfgProductionVersion | CRUD | 生产版本（标准 CRUD） |
| ErpMfgForecast | CRUD | 预测（标准 CRUD） |
| ErpMfgForecastLine | CRUD | 预测行（标准 CRUD） |
| ErpMfgBatchGenealogy | CRUD | 批次追溯（标准 CRUD） |
| ErpMfgCrpLoad | CRUD | 产能负荷（标准 CRUD） |
| ErpMfgWorkcenter | CRUD | 工作中心（标准 CRUD） |
| ErpMfgWorkcenterCalendar | CRUD | 工作中心日历（标准 CRUD） |
| ErpMfgWorkcenterCapacity | CRUD | 工作中心产能（标准 CRUD） |
| ErpMfgSubcontractOrderLine | CRUD | 委外加工单行（标准 CRUD） |

## 期望按钮推导依据

1. **CRUD 基线**（METHODOLOGY §1.1）：所有实体默认期望 add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button。
2. **审批/工作流按钮**（METHODOLOGY §1.2 + `domain-design-guidelines.md` §1.2）：业务单据实体期望 submit, withdraw-approval, approve, reject, reverse-approve, cancel 按钮。
3. **域专用按钮**（METHODOLOGY §1.3 + `ui-patterns.md`）：manufacturing 域注册 row-issue-materials-button, row-receive-finished-button, row-post-processing-fee-button。`ui-patterns.md` 工单详情页导航流 `[审核] → [领料] → [报工] → [完工入库]` 及工单列表的操作区域需对应这三个按钮。
4. **作业卡状态机**（`state-machine.md` §适用对象二）：JobCard 有独立 8 态状态机（OPEN → WORK_IN_PROGRESS → SUBMITTED → COMPLETED/CANCELLED 等），其独立 view.xml 应提供状态迁移按钮。

## 逐实体分析

### ErpMfgWorkOrder — CRUD+Custom
- **期望按钮**：CRUD 基线 + submit/withdraw-approval/approve/reject/reverse-approve/cancel + issue-materials/receive-finished/post-processing-fee
- **实际按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-submit-button, row-withdraw-approval-button, row-approve-button, row-reject-button, row-reverse-approve-button, row-delete-button
- **差距**：
  - `row-cancel-button`: missing (**major**) — 工单状态机支持 SUBMITTED/NOT_STARTED → CANCELLED 迁移，任何带 lifeycle 的业务单据均应提供作废按钮（METHODOLOGY §1.2, `domain-design-guidelines.md` §1.2）
  - `row-issue-materials-button`: missing (**blocker**) — `ui-patterns.md` 导航流明确描述 `[领料]` 作为工单操作，SubcontractOrder 同域已实现（METHODOLOGY §1.3）
  - `row-receive-finished-button`: missing (**blocker**) — `ui-patterns.md` 导航流明确描述 `[完工入库]` 作为工单操作（METHODOLOGY §1.3）
  - `row-post-processing-fee-button`: missing (**blocker**) — `ui-patterns.md` 导航流明确描述 `[报工]` 及工单详情页"报工操作"弹出报工表单（METHODOLOGY §1.3）
- **判定**：**blocker**（3 个 blocker 域专用按钮缺失 + 1 个 major WF 按钮缺失）

### ErpMfgSubcontractOrder — CRUD+Custom
- **期望按钮**：CRUD 基线 + submit/withdraw-approval/approve/reject/reverse-approve/cancel + issue-materials/receive-finished/post-processing-fee
- **实际按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-submit-button, row-withdraw-approval-button, row-approve-button, row-reject-button, row-reverse-approve-button, row-cancel-button, row-delete-button, row-issue-materials-button, row-receive-finished-button, row-post-processing-fee-button
- **差距**：无
- **判定**：**clean**（参考实现，作为同域实现标杆）

### ErpMfgJobCard — CRUD
- **期望按钮**：CRUD 基线 + 状态迁移按钮（start/pause/resume/submit/complete/cancel，对应于 8 态状态机 `state-machine.md` §适用对象二）
- **实际按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**：
  - 无任何状态迁移按钮（如 `row-start-button`, `row-complete-button`, `row-submit-button`, `row-pause-button` 等）: missing (**minor**) — JobCard 有独立 8 态状态机但 view.xml 仅提供标准 CRUD；这些操作目前可能嵌入在工单详情页的子表行操作中（`[完成]`, `[报工]` per `ui-patterns.md` 工单详情页），不作为主列表必选按钮，记录为 minor 差距
- **判定**：**minor**

### ErpMfgBom — CRUD
- **期望按钮**：CRUD 基线
- **实际按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**：无
- **判定**：**clean**

### ErpMfgRouting — CRUD
- **期望按钮**：CRUD 基线
- **实际按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**：无
- **判定**：**clean**

### ErpMfgCostRollup — CRUD
- **期望按钮**：CRUD 基线
- **实际按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**：无
- **判定**：**clean**

### ErpMfgMrpPlan — CRUD
- **期望按钮**：CRUD 基线
- **实际按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**：无
- **判定**：**clean**

### 剩余 21 个实体（CRUD 子表/辅助实体）
- **期望按钮**：CRUD 基线
- **实际按钮**：均为标准 CRUD（add, batch-delete, view, update, delete）
- **差距**：无
- **判定**：**clean**

## 摘要

| 分类 | 实体 | 差距数 | 最高严重级 | 备注 |
|------|------|--------|-----------|------|
| CRUD+Custom | ErpMfgWorkOrder | 4 | blocker | 缺 row-cancel-button（major）+ 3 个域专用按钮（blocker） |
| CRUD+Custom | ErpMfgSubcontractOrder | 0 | clean | 完整实现，同域参考标杆 |
| CRUD | ErpMfgJobCard | 1 | minor | 缺状态迁移按钮（8 态状态机无对应行按钮） |
| CRUD | ErpMfgBom | 0 | clean | — |
| CRUD | ErpMfgRouting | 0 | clean | — |
| CRUD | ErpMfgCostRollup | 0 | clean | — |
| CRUD | ErpMfgMrpPlan | 0 | clean | — |
| CRUD | 其他 21 个实体 | 0 | clean | 全部标准 CRUD |

### 总评
- 总实体数：28
- 无差距实体：26（92.9%）
- Blocker 差距：1（ErpMfgWorkOrder 缺 3 个域专用按钮）
- Major 差距：1（ErpMfgWorkOrder 缺 row-cancel-button）
- Minor/Info 差距：1（ErpMfgJobCard 缺状态迁移按钮）

**核心发现**：ErpMfgWorkOrder 作为制造域核心业务实体，其 view.xml 缺少 `row-cancel-button` 及三个注册的域专用按钮（`row-issue-materials-button`, `row-receive-finished-button`, `row-post-processing-fee-button`）。同域的 ErpMfgSubcontractOrder 已完整实现相同按钮集（包括带状态守卫的 `visibleOn` 条件），建议按相同范式补充 WorkOrder 的按钮定义。ErpMfgJobCard 的独立状态机按钮可考虑后续增强。
