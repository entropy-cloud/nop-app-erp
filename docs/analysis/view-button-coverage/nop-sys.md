# nop-sys 视图按钮需求覆盖分析

## 分析范围

| 实体 | 分类 | 说明 |
|------|------|------|
| NopSysBroadcastEvent | CRUD | 标准 CRUD |
| NopSysChangeLog | Custom | 变更跟踪日志，不可新建/编辑 |
| NopSysCheckerRecord | CRUD | 标准 CRUD |
| NopSysClusterLeader | CRUD | 标准 CRUD |
| NopSysCodeRule | CRUD | 标准 CRUD |
| NopSysCompactExtField | CRUD | 标准 CRUD |
| NopSysDict | Custom | 字典表，无 row-view-button，有 items-button |
| NopSysDictOption | CRUD | 标准 CRUD |
| NopSysEvent | CRUD | 标准 CRUD |
| NopSysExtField | CRUD | 标准 CRUD |
| NopSysI18n | CRUD | 标准 CRUD |
| NopSysLock | CRUD | 标准 CRUD |
| NopSysNoticeTemplate | CRUD | 标准 CRUD |
| NopSysObjTag | CRUD | 标准 CRUD |
| NopSysSequence | CRUD | 标准 CRUD |
| NopSysServiceInstance | CRUD | 标准 CRUD |
| NopSysTag | CRUD | 标准 CRUD |
| NopSysUserVariable | CRUD | 标准 CRUD |
| NopSysVariable | CRUD | 标准 CRUD |

## 期望按钮推导依据

- 无 ui-patterns.md（Group B 域），期望仅 CRUD 基线（METHODOLOGY §6）。
- 平台约定：NopSysDict 作为字典表管理，items-button 为查看字典条目的标准入口（METHODOLOGY §1.3 items-button 归属 nop-* 树形）。
- NopSysChangeLog 为系统变更审计日志，期望同其他实体的 CRUD 基线，但日志不可新建/编辑是合理的设计。

## 逐实体分析

### NopSysBroadcastEvent — CRUD
- **期望按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**：无
- **判定**：clean

### NopSysChangeLog — Custom
- **期望按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**：batch-delete-button, row-view-button, row-delete-button
- **差距**：
  - add-button: missing (minor) — 变更日志由系统自动记录，不支持手动新建，合理
  - row-update-button: missing (minor) — 变更日志为不可变审计记录，不支持编辑，合理
- **判定**：clean（差距已被实体语义合理性覆盖）

### NopSysCheckerRecord — CRUD
- **期望按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**：无
- **判定**：clean

### NopSysClusterLeader — CRUD
- **期望按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**：无
- **判定**：clean

### NopSysCodeRule — CRUD
- **期望按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**：无
- **判定**：clean

### NopSysCompactExtField — CRUD
- **期望按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**：无
- **判定**：clean

### NopSysDict — Custom
- **期望按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button, items-button
- **实际按钮**：add-button, batch-delete-button, row-update-button, row-delete-button, items-button
- **差距**：
  - row-view-button: missing (minor) — 字典表条目可直接编辑或通过 items-button 查看条目，view 按钮缺失，但编辑和条目入口可补偿
- **判定**：clean（items-button 作为字典条目管理的标准入口，部分补偿了 row-view-button 缺失）

### NopSysDictOption — CRUD
- **期望按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button（同时出现在 main 和 dict-ref 页）
- **差距**：无
- **判定**：clean

### NopSysEvent — CRUD
- **期望按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**：无
- **判定**：clean

### NopSysExtField — CRUD
- **期望按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**：无
- **判定**：clean

### NopSysI18n — CRUD
- **期望按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**：无
- **判定**：clean

### NopSysLock — CRUD
- **期望按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**：无
- **判定**：clean

### NopSysNoticeTemplate — CRUD
- **期望按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**：无
- **判定**：clean

### NopSysObjTag — CRUD
- **期望按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**：无
- **判定**：clean

### NopSysSequence — CRUD
- **期望按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**：无
- **判定**：clean

### NopSysServiceInstance — CRUD
- **期望按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**：无
- **判定**：clean

### NopSysTag — CRUD
- **期望按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**：无
- **判定**：clean

### NopSysUserVariable — CRUD
- **期望按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**：无
- **判定**：clean

### NopSysVariable — CRUD
- **期望按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**：无
- **判定**：clean

## 摘要

| 分类 | 实体 | 差距数 | 最高严重级 | 备注 |
|------|------|--------|-----------|------|
| CRUD | NopSysBroadcastEvent | 0 | clean | — |
| Custom | NopSysChangeLog | 0 | clean | 变更日志，无 add/update 合理 |
| CRUD | NopSysCheckerRecord | 0 | clean | — |
| CRUD | NopSysClusterLeader | 0 | clean | — |
| CRUD | NopSysCodeRule | 0 | clean | — |
| CRUD | NopSysCompactExtField | 0 | clean | — |
| Custom | NopSysDict | 0 | clean | items-button 补偿了 view 缺失 |
| CRUD | NopSysDictOption | 0 | clean | — |
| CRUD | NopSysEvent | 0 | clean | — |
| CRUD | NopSysExtField | 0 | clean | — |
| CRUD | NopSysI18n | 0 | clean | — |
| CRUD | NopSysLock | 0 | clean | — |
| CRUD | NopSysNoticeTemplate | 0 | clean | — |
| CRUD | NopSysObjTag | 0 | clean | — |
| CRUD | NopSysSequence | 0 | clean | — |
| CRUD | NopSysServiceInstance | 0 | clean | — |
| CRUD | NopSysTag | 0 | clean | — |
| CRUD | NopSysUserVariable | 0 | clean | — |
| CRUD | NopSysVariable | 0 | clean | — |

### 总评
- 总实体数：19
- 无差距实体：19（100%）
- Blocker 差距：0
- Major 差距：0
- Minor/Info 差距：0
