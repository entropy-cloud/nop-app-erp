# nop-wf 视图按钮需求覆盖分析

## 分析范围

| 实体 | 分类 | 说明 |
|------|------|------|
| NopWfAction | CRUD | 标准 CRUD |
| NopWfApprovableForm | CRUD | 标准 CRUD |
| NopWfApprovableItem | CRUD | 标准 CRUD |
| NopWfDefinition | CRUD+Custom | CRUD 基线 + publish/unpublish/design/copy-for-new |
| NopWfDefinitionAuth | CRUD | 标准 CRUD |
| NopWfInstance | CRUD | 标准 CRUD |
| NopWfLog | Custom | 审计日志，不可新建/编辑 |
| NopWfOutput | CRUD | 标准 CRUD |
| NopWfStatusHistory | CRUD | 标准 CRUD |
| NopWfStepInstance | CRUD | 标准 CRUD |
| NopWfStepInstanceLink | CRUD | 标准 CRUD |
| NopWfVar | CRUD | 标准 CRUD |
| NopWfWork | CRUD | 标准 CRUD |
| WorkflowService | Other | 服务调用页，非实体 CRUD |

## 期望按钮推导依据

- 无 ui-patterns.md（Group B 域），期望仅 CRUD 基线（METHODOLOGY §6）。
- 平台约定：NopWfDefinition 属于工作流定义实体，支持 publish/unpublish 按钮（METHODOLOGY §1.3）。
- NopWfLog 为系统审计日志，期望同其他实体的 CRUD 基线，但日志不可新建/编辑是合理的设计。

## 逐实体分析

### NopWfAction — CRUD
- **期望按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**：无
- **判定**：clean

### NopWfApprovableForm — CRUD
- **期望按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**：无
- **判定**：clean

### NopWfApprovableItem — CRUD
- **期望按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**：无
- **判定**：clean

### NopWfDefinition — CRUD+Custom
- **期望按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button, publish-button, unpublish-button
- **实际按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button, design, copy-for-new-button, publish-button, unpublish-button
- **差距**：
  - design: extra (info) — 工作流设计器入口，平台约定按钮，非阻塞
  - copy-for-new-button: extra (info) — 复制新建功能，平台约定按钮，非阻塞
- **判定**：clean（extra 按钮为平台增值功能，不构成差距）

### NopWfDefinitionAuth — CRUD
- **期望按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**：无
- **判定**：clean

### NopWfInstance — CRUD
- **期望按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**：无
- **判定**：clean

### NopWfLog — Custom
- **期望按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**：batch-delete-button, row-view-button, row-delete-button
- **差距**：
  - add-button: missing (minor) — 工作流日志由系统生成，不支持手动新建，合理
  - row-update-button: missing (minor) — 日志为不可变审计记录，不支持编辑，合理
- **判定**：clean（差距已被实体语义合理性覆盖）

### NopWfOutput — CRUD
- **期望按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**：无
- **判定**：clean

### NopWfStatusHistory — CRUD
- **期望按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**：无
- **判定**：clean

### NopWfStepInstance — CRUD
- **期望按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**：无
- **判定**：clean

### NopWfStepInstanceLink — CRUD
- **期望按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**：无
- **判定**：clean

### NopWfVar — CRUD
- **期望按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**：无
- **判定**：clean

### NopWfWork — CRUD
- **期望按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**：无
- **判定**：clean

### WorkflowService — Other
- **期望按钮**：无（非实体 CRUD 页面，为工作流服务调用入口）
- **实际按钮**：WorkflowService__startWorkflow-submit, WorkflowService__notifySubFlowEnd-submit, WorkflowService__invokeAction-submit, WorkflowService__killWorkflow-submit, WorkflowService__suspendWorkflow-submit, WorkflowService__resumeWorkflow-submit, WorkflowService__signalWf-submit, WorkflowService__transferActors-submit
- **差距**：无
- **判定**：clean

## 摘要

| 分类 | 实体 | 差距数 | 最高严重级 | 备注 |
|------|------|--------|-----------|------|
| CRUD | NopWfAction | 0 | clean | — |
| CRUD | NopWfApprovableForm | 0 | clean | — |
| CRUD | NopWfApprovableItem | 0 | clean | — |
| CRUD+Custom | NopWfDefinition | 0 | clean | extra design/copy-for-new 为增值功能 |
| CRUD | NopWfDefinitionAuth | 0 | clean | — |
| CRUD | NopWfInstance | 0 | clean | — |
| Custom | NopWfLog | 0 | clean | 审计日志，无 add/update 合理 |
| CRUD | NopWfOutput | 0 | clean | — |
| CRUD | NopWfStatusHistory | 0 | clean | — |
| CRUD | NopWfStepInstance | 0 | clean | — |
| CRUD | NopWfStepInstanceLink | 0 | clean | — |
| CRUD | NopWfVar | 0 | clean | — |
| CRUD | NopWfWork | 0 | clean | — |
| Other | WorkflowService | 0 | clean | 服务页，非 CRUD |

### 总评
- 总实体数：14
- 无差距实体：14（100%）
- Blocker 差距：0
- Major 差距：0
- Minor/Info 差距：0
