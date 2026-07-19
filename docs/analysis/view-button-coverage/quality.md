# Quality 视图按钮需求覆盖分析

## 分析范围

| 实体 | ORM 映射 | 分类 |
|------|----------|------|
| Inspection | ErpQaInspection | CRUD+Custom |
| InspectionPlan | ErpQaSamplingPlan | CRUD |
| Sample | ErpQaSpcSample | CRUD |
| NCR | ErpQaNonConformance | CRUD+Custom |
| CAPA | ErpQaAction | CRUD |
| Calibration | ErpQaCalibration | CRUD |
| SPC Chart | ErpQaSpcChart | CRUD |
| SPC Capability | ErpQaSpcCapability | CRUD |
| SPC Sample | ErpQaSpcSample | CRUD |

## 期望按钮推导依据

- **CRUD 基线**（METHODOLOGY §1.1）：所有实体默认期望 add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button。
- **Inspection 域专用按钮**：ui-patterns.md:54 明确描述 `[提交合格]`、`[触发 NCR]`、`[让步接收]` 三个操作按钮。state-machine.md §适用对象一 定义状态机 PENDING→ACCEPTED/CONDITIONAL/REJECTED，对应三个结果迁移动作。
- **NCR 域专用按钮**：ui-patterns.md:118 描述 `[创建 CAPA]` 按钮。state-machine.md §适用对象二 定义 5 态 NCR 状态机（OPEN→IN_REVIEW→RESOLVED/ESCALATED_TO_RECALL/CANCELLED），每个迁移需对应行按钮。
- **Calibration 偏离说明**：state-machine.md §实现偏离补注 明确"校准管理...实体存在但 BizModel 深化不落地（仅标准 CRUD 空壳）"，故仅期望 CRUD 基线。
- **SPC/SamplingPlan 无状态机**：分析/配置类实体，仅期望 CRUD 基线。
- **CAPA**：NCR 的子项实体，无独立状态机深度落地，仅期望 CRUD 基线。

## 逐实体分析

### ErpQaInspection (Inspection) — CRUD+Custom
- **期望按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button, row-submit-result, row-trigger-ncr, row-concession-accept
- **实际按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**：
  - row-submit-result: missing (blocker) — ui-patterns.md:54 `[提交合格]`，对应 PENDING→ACCEPTED
  - row-trigger-ncr: missing (blocker) — ui-patterns.md:54 `[触发 NCR]`，不合格时引导创建 NCR
  - row-concession-accept: missing (blocker) — ui-patterns.md:54 `[让步接收]`，对应 PENDING→CONDITIONAL
- **判定**：blocker

### ErpQaSamplingPlan (InspectionPlan) — CRUD
- **期望按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**：无
- **判定**：clean

### ErpQaSpcSample (Sample) — CRUD
- **期望按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**：无
- **判定**：clean

### ErpQaNonConformance (NCR) — CRUD+Custom
- **期望按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button, row-submit-review, row-resolve, row-escalate-to-recall, row-cancel, row-create-capa
- **实际按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**：
  - row-submit-review: missing (blocker) — state-machine.md §NCR 定义 OPEN→IN_REVIEW 迁移
  - row-resolve: missing (blocker) — state-machine.md §NCR 定义 IN_REVIEW→RESOLVED 迁移
  - row-escalate-to-recall: missing (blocker) — state-machine.md §NCR 定义 IN_REVIEW→ESCALATED_TO_RECALL 迁移
  - row-cancel: missing (blocker) — state-machine.md §NCR 定义 OPEN/IN_REVIEW→CANCELLED 迁移
  - row-create-capa: missing (blocker) — ui-patterns.md:118 `[创建 CAPA]`
- **判定**：blocker

### ErpQaAction (CAPA) — CRUD
- **期望按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**：无
- **判定**：clean

### ErpQaCalibration — CRUD
- **期望按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**：无（注：state-machine.md §实现偏离补记 标注"仅标准 CRUD 空壳"，非落地目标）
- **判定**：clean

### ErpQaSpcChart (SPC Chart) — CRUD
- **期望按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**：无
- **判定**：clean

### ErpQaSpcCapability (SPC Capability) — CRUD
- **期望按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**：无
- **判定**：clean

## 摘要

| 分类 | 实体 | 差距数 | 最高严重级 | 备注 |
|------|------|--------|-----------|------|
| CRUD+Custom | ErpQaInspection (Inspection) | 3 | blocker | 缺提交合格/触发NCR/让步接收 |
| CRUD | ErpQaSamplingPlan (InspectionPlan) | 0 | clean | — |
| CRUD | ErpQaSpcSample (Sample) | 0 | clean | — |
| CRUD+Custom | ErpQaNonConformance (NCR) | 5 | blocker | 缺评审/解决/升级召回/取消/创建CAPA |
| CRUD | ErpQaAction (CAPA) | 0 | clean | — |
| CRUD | ErpQaCalibration | 0 | clean | 偏离注：标准CRUD空壳，未列为落地目标 |
| CRUD | ErpQaSpcChart (SPC) | 0 | clean | — |
| CRUD | ErpQaSpcCapability (SPC) | 0 | clean | — |

### 总评
- 总实体数：8
- 无差距实体：6（75.0%）
- Blocker 差距：2（Inspection, NCR）
- Major 差距：0
- Minor/Info 差距：0

### 发现说明

两个 blocker 实体（Inspection 和 NCR）均缺乏其核心业务状态机所需的域专用按钮。Inspection 缺少全部三个 result 迁移按钮（提交合格、触发 NCR、让步接收），NCR 缺少全部五个状态迁移按钮（评审、解决、升级召回、取消、创建 CAPA）。这与两个实体均已具备 approveStatus 轴但 view.xml 停留在 CRUD 基线快照的现状一致。需在 BizModel 实现对应 `@BizMutation` 方法后，在 view.xml rowActions 中添加相应按钮定义。
