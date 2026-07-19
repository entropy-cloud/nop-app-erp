# nop-report 视图按钮需求覆盖分析

## 分析范围

| 实体 | 分类 | 说明 |
|------|------|------|
| NopReportDataset | CRUD | 标准 CRUD |
| NopReportDatasetAuth | CRUD | 标准 CRUD |
| NopReportDatasetRef | CRUD | 标准 CRUD |
| NopReportDatasource | CRUD | 标准 CRUD |
| NopReportDatasourceAuth | CRUD | 标准 CRUD |
| NopReportDefinition | CRUD | 标准 CRUD |
| NopReportDefinitionAuth | CRUD | 标准 CRUD |
| NopReportResultFile | CRUD | 标准 CRUD |
| NopReportSubDataset | CRUD | 标准 CRUD |

## 期望按钮推导依据

- 无 ui-patterns.md（Group B 域），期望仅 CRUD 基线（METHODOLOGY §6）。
- 无平台特定按钮约定需要覆盖。

## 逐实体分析

### NopReportDataset — CRUD
- **期望按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**：无
- **判定**：clean

### NopReportDatasetAuth — CRUD
- **期望按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**：无
- **判定**：clean

### NopReportDatasetRef — CRUD
- **期望按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**：无
- **判定**：clean

### NopReportDatasource — CRUD
- **期望按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**：无
- **判定**：clean

### NopReportDatasourceAuth — CRUD
- **期望按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**：无
- **判定**：clean

### NopReportDefinition — CRUD
- **期望按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**：无
- **判定**：clean

### NopReportDefinitionAuth — CRUD
- **期望按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**：无
- **判定**：clean

### NopReportResultFile — CRUD
- **期望按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**：无
- **判定**：clean

### NopReportSubDataset — CRUD
- **期望按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**：无
- **判定**：clean

## 摘要

| 分类 | 实体 | 差距数 | 最高严重级 | 备注 |
|------|------|--------|-----------|------|
| CRUD | NopReportDataset | 0 | clean | — |
| CRUD | NopReportDatasetAuth | 0 | clean | — |
| CRUD | NopReportDatasetRef | 0 | clean | — |
| CRUD | NopReportDatasource | 0 | clean | — |
| CRUD | NopReportDatasourceAuth | 0 | clean | — |
| CRUD | NopReportDefinition | 0 | clean | — |
| CRUD | NopReportDefinitionAuth | 0 | clean | — |
| CRUD | NopReportResultFile | 0 | clean | — |
| CRUD | NopReportSubDataset | 0 | clean | — |

### 总评
- 总实体数：9
- 无差距实体：9（100%）
- Blocker 差距：0
- Major 差距：0
- Minor/Info 差距：0
