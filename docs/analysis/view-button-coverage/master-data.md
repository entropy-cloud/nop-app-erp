# 主数据域（master-data）视图按钮需求覆盖分析

## 分析范围

本域共 24 实体，全为标准 CRUD 页面（无工作流实体）：

| 分类 | 实体数 | 实体列表 |
|------|--------|----------|
| CRUD | 22 | ErpMdAcctSchema, ErpMdAcctSchemaCoa, ErpMdBankAccount, ErpMdCostCenter, ErpMdCurrency, ErpMdEmployee, ErpMdExchangeRate, ErpMdLocation, ErpMdMaterial, ErpMdMaterialSku, ErpMdOrganization, ErpMdPartner, ErpMdPartnerAddress, ErpMdPartnerContact, ErpMdSettlementMethod, ErpMdSubjectMapping, ErpMdSupplierApproval, ErpMdTaxRate, ErpMdUoM, ErpMdUoMConversion, ErpMdWarehouse, ErpSysConfig |
| CRUD+Custom | 2 | ErpMdMaterialCategory（树形实体）, ErpMdSubject（树形实体） |

## 期望按钮推导依据

1. **CRUD 基线**（METHODOLOGY §1.1）：toolbar `{add-button, batch-delete-button}` + row `{row-view-button, row-update-button, row-delete-button}` 适用于所有实体。
2. **无工作流按钮**（`docs/design/master-data/README.md` §启用/停用（非状态机））：主数据实体采用启停二态，不是工作流状态机。不期望 submit/approve/reject/cancel 按钮。
3. **树形实体**（`docs/design/master-data/ui-patterns.md` §物料分类管理 / §科目表管理）：MaterialCategory 期望树节点操作（新增同级、新增子级、重命名、删除、拖拽移动）；Subject 期望树节点操作（新增同级、新增子级、停用）。按 METHODOLOGY §7 规则 5，树实体允许无 CRUD toolbar。
4. **币种与汇率**（ui-patterns.md §币种与汇率）：汇率管理页面提及 `[从外部导入]` 功能。按 METHODOLOGY §2 prose→button-id 字典，此按钮无标准 ID，属于 info 级可增强点。
5. **物料/SKU 编辑**（ui-patterns.md §物料/SKU 编辑）：提及 SKU 子表 `[扫描条码] [批量生成]`，属于子表级操作，非主 CRUD 页 toolbar/row 按钮，不计为期望。

## 逐实体分析

### ErpMdAcctSchema — CRUD
- **期望按钮**: add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**: add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**: 无
- **判定**: clean

### ErpMdAcctSchemaCoa — CRUD
- **期望按钮**: add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**: add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**: 无
- **判定**: clean

### ErpMdBankAccount — CRUD
- **期望按钮**: add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**: add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**: 无
- **判定**: clean

### ErpMdCostCenter — CRUD
- **期望按钮**: add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**: add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**: 无
- **判定**: clean

### ErpMdCurrency — CRUD
- **期望按钮**: add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**: add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**: 无
- **判定**: clean

### ErpMdEmployee — CRUD
- **期望按钮**: add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**: add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**: 无
- **判定**: clean

### ErpMdExchangeRate — CRUD
- **期望按钮**: add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**: add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**:
  - `import-from-external`: missing (info) — ui-patterns.md §币种与汇率 明确描述 `[从外部导入]` 汇率功能（"汇率录入 * 从外部导入"），当前仅标准 CRUD 无导入入口。按 METHODOLOGY §2 此按钮无标准 ID，属于可增强点。
- **判定**: info

### ErpMdLocation — CRUD
- **期望按钮**: add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**: add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**: 无
- **判定**: clean

### ErpMdMaterial — CRUD
- **期望按钮**: add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**: add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**: 无
- **判定**: clean

### ErpMdMaterialCategory — CRUD+Custom
- **期望按钮**: 树节点操作（新增同级、新增子级、重命名、删除、拖拽移动），来源于 ui-patterns.md §物料分类管理 树形结构图下方 `[新增同级] [新增子级] [重命名] [删除] [拖拽移动]`。按 METHODOLOGY §7 规则 5，树实体允许无 CRUD toolbar。
- **实际按钮**: add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button（标准 CRUD 扁平列表）
- **差距**:
  - 设计意图不匹配 (minor): ui-patterns.md 描述树形交互 + 节点级操作（新增同级/新增子级/重命名/拖拽移动），但实际 view.xml（`ErpMdMaterialCategory.view.xml` + `_gen/_ErpMdMaterialCategory.view.xml`）使用标准 codegen CRUD 扁平列表模式。这两者交互范式不同：树操作在节点级上下文（右键/树工具栏），标准 CRUD 在列表 toolbar。METHODOLOGY §2 无标准按钮 ID 映射树操作，故不记为缺失按钮 ID 差距，但设计意图偏离应记录。
  - batch-delete-button (extra/info): 树实体的批量删除语义弱（多为单节点删除或级联删除），该按钮由 codegen 自动生成，功能上不属阻塞。
- **判定**: minor

### ErpMdMaterialSku — CRUD
- **期望按钮**: add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**: add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**: 无
- **判定**: clean

### ErpMdOrganization — CRUD
- **期望按钮**: add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**: add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**: 无
- **判定**: clean

### ErpMdPartner — CRUD
- **期望按钮**: add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**: add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**: 无
- **判定**: clean

### ErpMdPartnerAddress — CRUD
- **期望按钮**: add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**: add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**: 无
- **判定**: clean

### ErpMdPartnerContact — CRUD
- **期望按钮**: add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**: add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**: 无
- **判定**: clean

### ErpMdSettlementMethod — CRUD
- **期望按钮**: add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**: add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**: 无
- **判定**: clean

### ErpMdSubject — CRUD+Custom
- **期望按钮**: 树节点操作（新增同级、新增子级、保存、停用），来源于 ui-patterns.md §科目表管理 底部 `[保存] [新增同级] [新增子级] [停用]`。按 METHODOLOGY §7 规则 5，树实体允许无 CRUD toolbar。
- **实际按钮**: add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button（标准 CRUD 扁平列表）
- **差距**:
  - 设计意图不匹配 (minor): ui-patterns.md 描述树形交互表单 + 树操作按钮（新增同级/新增子级/停用），实际 view.xml（`ErpMdSubject.view.xml` + `_gen/_ErpMdSubject.view.xml`）使用标准 codegen CRUD 扁平列表模式。交互范式偏离同 ErpMdMaterialCategory。
  - batch-delete-button (extra/info): 树实体的批量删除语义弱，由 codegen 自动生成。
- **判定**: minor

### ErpMdSubjectMapping — CRUD
- **期望按钮**: add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**: add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**: 无
- **判定**: clean

### ErpMdSupplierApproval — CRUD
- **期望按钮**: add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**: add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**: 无
- **判定**: clean

### ErpMdTaxRate — CRUD
- **期望按钮**: add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**: add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**: 无
- **判定**: clean

### ErpMdUoM — CRUD
- **期望按钮**: add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**: add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**: 无
- **判定**: clean

### ErpMdUoMConversion — CRUD
- **期望按钮**: add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**: add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**: 无
- **判定**: clean

### ErpMdWarehouse — CRUD
- **期望按钮**: add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**: add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**: 无
- **判定**: clean

### ErpSysConfig — CRUD
- **期望按钮**: add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**: add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**: 无
- **判定**: clean

## 摘要

| 分类 | 实体 | 差距数 | 最高严重级 | 备注 |
|------|------|--------|-----------|------|
| CRUD | ErpMdAcctSchema | 0 | clean | |
| CRUD | ErpMdAcctSchemaCoa | 0 | clean | |
| CRUD | ErpMdBankAccount | 0 | clean | |
| CRUD | ErpMdCostCenter | 0 | clean | |
| CRUD | ErpMdCurrency | 0 | clean | |
| CRUD | ErpMdEmployee | 0 | clean | |
| CRUD | ErpMdExchangeRate | 1 | info | 缺少汇率导入入口（ui-patterns 提及 `[从外部导入]`） |
| CRUD | ErpMdLocation | 0 | clean | |
| CRUD | ErpMdMaterial | 0 | clean | |
| CRUD+Custom | ErpMdMaterialCategory | 2 | minor | 设计意图偏离：树实体使用扁平 CRUD；batch-delete 语义弱 |
| CRUD | ErpMdMaterialSku | 0 | clean | |
| CRUD | ErpMdOrganization | 0 | clean | |
| CRUD | ErpMdPartner | 0 | clean | |
| CRUD | ErpMdPartnerAddress | 0 | clean | |
| CRUD | ErpMdPartnerContact | 0 | clean | |
| CRUD | ErpMdSettlementMethod | 0 | clean | |
| CRUD+Custom | ErpMdSubject | 2 | minor | 设计意图偏离：树实体使用扁平 CRUD；batch-delete 语义弱 |
| CRUD | ErpMdSubjectMapping | 0 | clean | |
| CRUD | ErpMdSupplierApproval | 0 | clean | |
| CRUD | ErpMdTaxRate | 0 | clean | |
| CRUD | ErpMdUoM | 0 | clean | |
| CRUD | ErpMdUoMConversion | 0 | clean | |
| CRUD | ErpMdWarehouse | 0 | clean | |
| CRUD | ErpSysConfig | 0 | clean | |

### 总评
- 总实体数：24
- 无差距实体：20（83.3%）
- Blocker 差距：0
- Major 差距：0
- Minor 差距：2（设计意图偏离 — 树实体）
- Info 差距：1（汇率导入） + 2（树实体 batch-delete 语义弱）= 3

### 关键发现

1. **CRUD 基线全覆盖**：24 实体均完整包含 CRUD 基线五按钮（add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button），无实体缺失核心 CRUD 能力。

2. **无工作流按钮预期**：主数据域全部实体均为启停二态（非工作流状态机），无实体需要 submit/approve/reject/cancel 按钮。当前无实体误带工作流按钮，符合预期。

3. **树实体设计意图偏离（minor）**：`ErpMdMaterialCategory` 和 `ErpMdSubject` 的 ui-patterns.md 明确描述树形交互模式（节点级新增同级/子级、重命名、拖拽排序），但当前 view.xml 使用标准 codegen CRUD 扁平列表。这是一个交互范式选择问题：flat CRUD 功能上可用（可通过 parentId 字段管理层级），但不符合设计文档描述的树交互体验。修复需要将页面类型从 `crud` 切换为树组件 + 自定义 tree toolbar 按钮。

4. **汇率导入入口缺失（info）**：`ErpMdExchangeRate` 缺少 `[从外部导入]` 按钮，ui-patterns.md 明确描述了此功能。属于产品增强点，非阻塞。
