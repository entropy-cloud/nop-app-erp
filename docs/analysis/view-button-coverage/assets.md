# 固定资产域视图按钮需求覆盖分析

## 分析范围

| 实体 | 分类 | 说明 |
|------|------|------|
| ErpAstAsset | CRUD（应为 CRUD+Custom） | 资产卡片——核心主实体 |
| ErpAstAssetCapitalization | CRUD+WF | 资产资本化审批单据 |
| ErpAstAssetCategory | CRUD | 资产类别配置 |
| ErpAstCip | CRUD | 在建工程 |
| ErpAstCipCostItem | CRUD | CIP 成本项（子表） |
| ErpAstCipProgressBilling | CRUD | CIP 进度付款 |
| ErpAstDepreciationSchedule | CRUD | 折旧计划条目 |
| ErpAstDisposal | CRUD+WF | 资产处置审批单据 |
| ErpAstInventory | CRUD | 资产盘点库存 |
| ErpAstInventoryLine | CRUD | 盘点行（子表） |
| ErpAstMaintenance | CRUD | 资产维修记录 |
| ErpAstMaintenanceCost | CRUD | 维修成本（子表） |
| ErpAstMerge | CRUD+WF | 资产合并审批单据 |
| ErpAstMergeLine | CRUD | 合并行（子表） |
| ErpAstMovement | CRUD | 资产移动登记 |
| ErpAstSplit | CRUD+WF | 资产拆分审批单据 |
| ErpAstSplitLine | CRUD | 拆分行（子表） |
| ErpAstValueAdjustment | CRUD+WF | 资产价值调整审批单据 |
| asset-repair (page.yaml) | Other | 占位页面，待实现 |
| asset-stocktake (page.yaml) | Other | 占位页面，待实现 |
| dashboard (page.yaml) | Other | KPI 看板，非 CRUD |
| report/* (page.yaml) | Other | 报表页面，非 CRUD |

## 期望按钮推导依据

- **CRUD 基线**（METHODOLOGY §1.1）：所有实体默认 toolbar={add-button, batch-delete-button}, row={row-view-button, row-update-button, row-delete-button}
- **审批/工作流基线**（METHODOLOGY §1.2）：业务单据实体（有 approveStatus）期望 submit/withdraw/approve/reject/reverse-approve
- **ui-patterns.md § 资产卡片详情**：操作按钮组 `[移动]` `[价值调整]` `[处置]` `[查看折旧计划]` `[查看凭证]` —— 对应 ErpAstAsset view 页面
  - `[移动]` → `row-transfer-button`（METHODOLOGY §1.3 域专用按钮表）
  - `[价值调整]` → 新域专用按钮（无标准 ID）
  - `[处置]` → 新域专用按钮（无标准 ID）
  - `[查看折旧计划]` → 导航型链接（METHODOLOGY §2 规则：属导航/信息面板，非 action 按钮，不记为期望）
  - `[查看凭证]` → 导航型链接（同上）
- **ui-patterns.md § 资产处置向导**：处置流程分步引导，从资产卡片详情 `[处置]` 按钮触发
- **ui-patterns.md § 资产移动登记**：简单表单，从资产卡片详情 `[移动]` 按钮触发
- **state-machine.md §2**：资产卡片生命周期 DRAFT→IN_SERVICE→IDLE→SCRAPPED/SOLD，处置按钮仅在 IN_SERVICE/IDLE 可见
- **state-machine.md §6**：报废/出售处置需审批，因此 ErpAstDisposal 需 CRUD+WF 按钮

## 逐实体分析

### ErpAstAsset — CRUD（应为 CRUD+Custom）

- **期望按钮**：toolbar {add-button, batch-delete-button}; row {row-view-button, row-update-button, row-delete-button, row-transfer-button, row-value-adjust-button, row-dispose-button}
- **实际按钮**：toolbar {add-button, batch-delete-button}; row {row-view-button, row-update-button, row-delete-button}
- **差距**：
  - `row-transfer-button`: missing (blocker) — ui-patterns.md 资产卡片详情明确列出 `[移动]` 按钮，原文："操作按钮组: [移动] [价值调整] [处置] [查看折旧计划] [查看凭证]"
  - `row-value-adjust-button`: missing (blocker) — ui-patterns.md 明确列出 `[价值调整]` 按钮
  - `row-dispose-button`: missing (blocker) — ui-patterns.md 明确列出 `[处置]` 按钮，且 state-machine.md 要求该按钮仅在 IN_SERVICE/IDLE 状态可见
- **判定**：blocker — 资产卡片详情页缺少 3 个核心域专用按钮，设计文档明确要求这些按钮直接出现在卡片详情操作区

### ErpAstAssetCapitalization — CRUD+WF

- **期望按钮**：CRUD 基线 + submit/withdraw/approve/reject/reverse-approve
- **实际按钮**：CRUD 基线 + row-submit-button, row-withdraw-approval-button, row-approve-button, row-reject-button, row-reverse-approve-button
- **差距**：无
- **判定**：clean

### ErpAstAssetCategory — CRUD

- **期望按钮**：CRUD 基线
- **实际按钮**：CRUD 基线
- **差距**：无
- **判定**：clean

### ErpAstCip — CRUD

- **期望按钮**：CRUD 基线
- **实际按钮**：CRUD 基线
- **差距**：无
- **判定**：clean（CIP 转固为后台操作，非行级按钮）

### ErpAstCipCostItem — CRUD

- **期望按钮**：CRUD 基线
- **实际按钮**：CRUD 基线
- **差距**：无
- **判定**：clean

### ErpAstCipProgressBilling — CRUD

- **期望按钮**：CRUD 基线
- **实际按钮**：CRUD 基线
- **差距**：无
- **判定**：clean

### ErpAstDepreciationSchedule — CRUD

- **期望按钮**：CRUD 基线（折旧执行为后端批量操作，非行级按钮）
- **实际按钮**：CRUD 基线
- **差距**：无
- **判定**：clean（折旧执行由期末定时任务触发，无需行级执行按钮）

### ErpAstDisposal — CRUD+WF

- **期望按钮**：CRUD 基线 + submit/withdraw/approve/reject/reverse-approve
- **实际按钮**：CRUD 基线 + row-submit-button, row-withdraw-approval-button, row-approve-button, row-reject-button, row-reverse-approve-button
- **差距**：无
- **判定**：clean

### ErpAstInventory — CRUD

- **期望按钮**：CRUD 基线
- **实际按钮**：CRUD 基线
- **差距**：无
- **判定**：clean

### ErpAstInventoryLine — CRUD

- **期望按钮**：CRUD 基线（子表实体）
- **实际按钮**：CRUD 基线
- **差距**：无
- **判定**：clean

### ErpAstMaintenance — CRUD

- **期望按钮**：CRUD 基线
- **实际按钮**：CRUD 基线
- **差距**：无
- **判定**：clean

### ErpAstMaintenanceCost — CRUD

- **期望按钮**：CRUD 基线（子表实体）
- **实际按钮**：CRUD 基线
- **差距**：无
- **判定**：clean

### ErpAstMerge — CRUD+WF

- **期望按钮**：CRUD 基线 + submit/withdraw/approve/reject/reverse-approve
- **实际按钮**：CRUD 基线 + row-submit-button, row-withdraw-approval-button, row-approve-button, row-reject-button, row-reverse-approve-button
- **差距**：无
- **判定**：clean

### ErpAstMergeLine — CRUD

- **期望按钮**：CRUD 基线（子表实体）
- **实际按钮**：CRUD 基线
- **差距**：无
- **判定**：clean

### ErpAstMovement — CRUD

- **期望按钮**：CRUD 基线（按 ui-patterns.md § 资产移动登记，为简单表单，非审批单据）
- **实际按钮**：CRUD 基线
- **差距**：
  - `row-submit-button` / `row-approve-button` / `row-reject-button`: 虽 view.xml 表单含 approveStatus 字段，但 ui-patterns.md 描述为"简单表单"、"不触发任何财务动作"，且 domain-design-guidelines.md §16.2 将作业类单据排除在审批流外。ORM 有 approveStatus 但 UI 无 WF 按钮，属字段级不一致。
  - (minor) 字段 `approveStatus` 在 view form 中展示但无 WF 按钮操作入口
- **判定**：minor — approveStatus 字段与 WF 按钮不一致，但不影响核心业务流程

### ErpAstSplit — CRUD+WF

- **期望按钮**：CRUD 基线 + submit/withdraw/approve/reject/reverse-approve
- **实际按钮**：CRUD 基线 + row-submit-button, row-withdraw-approval-button, row-approve-button, row-reject-button, row-reverse-approve-button
- **差距**：无
- **判定**：clean

### ErpAstSplitLine — CRUD

- **期望按钮**：CRUD 基线（子表实体）
- **实际按钮**：CRUD 基线
- **差距**：无
- **判定**：clean

### ErpAstValueAdjustment — CRUD+WF

- **期望按钮**：CRUD 基线 + submit/withdraw/approve/reject/reverse-approve
- **实际按钮**：CRUD 基线 + row-submit-button, row-withdraw-approval-button, row-approve-button, row-reject-button, row-reverse-approve-button
- **差距**：无
- **判定**：clean

### asset-repair (page.yaml) — Other

- **期望按钮**：N/A（占位页面）
- **实际按钮**：无
- **差距**：无（待实现占位页面，METHODOLOGY §7.3 规则 3：无 CRUD main 页面的实体标记为 Other）
- **判定**：info — 占位页面待实现

### asset-stocktake (page.yaml) — Other

- **期望按钮**：N/A（占位页面）
- **实际按钮**：无
- **差距**：无（待实现占位页面）
- **判定**：info — 占位页面待实现

### dashboard (page.yaml) — Other

- **期望按钮**：N/A（KPI 看板）
- **实际按钮**：刷新按钮
- **差距**：无
- **判定**：clean

### report/* (page.yaml) — Other

- **期望按钮**：N/A（报表页面，action 为渲染/下载，非 CRUD）
- **实际按钮**：渲染报表、下载 XLSX、下载 PDF
- **差距**：无
- **判定**：clean

## 摘要

| 分类 | 实体 | 差距数 | 最高严重级 | 备注 |
|------|------|--------|-----------|------|
| **CRUD** (actual) | ErpAstAsset | 3 | blocker | 缺少 [移动][价值调整][处置] 域专用按钮 |
| CRUD+WF | ErpAstAssetCapitalization | 0 | clean | — |
| CRUD | ErpAstAssetCategory | 0 | clean | — |
| CRUD | ErpAstCip | 0 | clean | — |
| CRUD | ErpAstCipCostItem | 0 | clean | — |
| CRUD | ErpAstCipProgressBilling | 0 | clean | — |
| CRUD | ErpAstDepreciationSchedule | 0 | clean | — |
| CRUD+WF | ErpAstDisposal | 0 | clean | — |
| CRUD | ErpAstInventory | 0 | clean | — |
| CRUD | ErpAstInventoryLine | 0 | clean | — |
| CRUD | ErpAstMaintenance | 0 | clean | — |
| CRUD | ErpAstMaintenanceCost | 0 | clean | — |
| CRUD+WF | ErpAstMerge | 0 | clean | — |
| CRUD | ErpAstMergeLine | 0 | clean | — |
| **CRUD** | ErpAstMovement | 1 | minor | approveStatus 字段存在但无 WF 按钮 |
| CRUD+WF | ErpAstSplit | 0 | clean | — |
| CRUD | ErpAstSplitLine | 0 | clean | — |
| CRUD+WF | ErpAstValueAdjustment | 0 | clean | — |
| Other | asset-repair | 0 | info | 占位页面待实现 |
| Other | asset-stocktake | 0 | info | 占位页面待实现 |
| Other | dashboard | 0 | clean | — |
| Other | report/* (2 pages) | 0 | clean | — |

### 总评

- 总实体数：22（18 标准实体 + 4 非标准页面）
- 无差距实体：19（86.4%）
- Blocker 差距：1（ErpAstAsset）
- Major 差距：0
- Minor 差距：1（ErpAstMovement）
- Info 差距：2（asset-repair、asset-stocktake 占位页面）

**核心发现**: ErpAstAsset view.xml 的资产卡片详情页面缺少 `[移动]`、`[价值调整]`、`[处置]` 三个核心域专用按钮。ui-patterns.md 将这些按钮列为资产卡片详情操作按钮组的核心组成部分，state-machine.md 进一步限定了 `[处置]` 按钮的可见性条件（仅 IN_SERVICE/IDLE 状态可见）。目前资产卡片详情页（`view` page）没有任何 action 按钮，用户需导航到独立实体管理页面才能执行这些操作，与设计文档的"卡片集中展示 + 一键操作"原则不符。

**建议修复方案**: 在 ErpAstAsset.view.xml 的 `main` CRUD rowActions 中添加三个域专用按钮：
1. `row-transfer-button` — 创建 ErpAstMovement 记录（跳转到新增表单或弹窗）
2. `row-value-adjust-button` — 创建 ErpAstValueAdjustment 记录（跳转到新增表单或弹窗）
3. `row-dispose-button` — 创建 ErpAstDisposal 记录（跳转到新增表单或弹窗），可见性条件 `${status == 'IN_SERVICE' || status == 'IDLE'}`
