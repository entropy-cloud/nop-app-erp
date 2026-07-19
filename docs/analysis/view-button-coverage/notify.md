# notify 视图按钮需求覆盖分析

## 分析范围

| 实体 | 分类 | 说明 |
|------|------|------|
| ErpSysNotification | CRUD | 通知记录，标准 CRUD |
| ErpSysNotificationRead | CRUD | 通知已读记录，标准 CRUD |
| ErpSysNotificationTemplate | CRUD | 通知模板，标准 CRUD |

## 期望按钮推导依据

- 无 ui-patterns.md（Group B 域），期望仅 CRUD 基线（METHODOLOGY §6）。
- METHODOLOGY §6 提示 notify 域可能含通知预览/发送等专用按钮，需人工判断。
- 经核查 view.xml 实际按钮，ErpSysNotification 和 ErpSysNotificationTemplate 均保持标准 CRUD 基线，无额外按钮。当前 notify 域暂未实现 row-preview-button/row-send-button 等通知专用操作，作为 info 级记录。

## 逐实体分析

### ErpSysNotification — CRUD
- **期望按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**：
  - row-send-button / row-preview-button: missing (info) — 通知实体将来可增加发送/预览按钮增强功能，当前 CRUD 基线已满足基本管理需求
- **判定**：clean

### ErpSysNotificationRead — CRUD
- **期望按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**：无
- **判定**：clean

### ErpSysNotificationTemplate — CRUD
- **期望按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**：add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **差距**：无
- **判定**：clean

## 摘要

| 分类 | 实体 | 差距数 | 最高严重级 | 备注 |
|------|------|--------|-----------|------|
| CRUD | ErpSysNotification | 0 | clean | 可将来增加 row-send-button 等增强 |
| CRUD | ErpSysNotificationRead | 0 | clean | — |
| CRUD | ErpSysNotificationTemplate | 0 | clean | — |

### 总评
- 总实体数：3
- 无差距实体：3（100%）
- Blocker 差距：0
- Major 差距：0
- Minor/Info 差距：0
