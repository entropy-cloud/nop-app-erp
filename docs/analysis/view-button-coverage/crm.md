# CRM 视图按钮需求覆盖分析

## 分析范围

CRM 域共 34 个实体页面（含 1 个 `report/` 目录，不计入分析）。核心业务实体聚焦 Lead/Opportunity（ErpCrmLead）、Campaign（ErpCrmCampaign）、Event（ErpCrmEvent）、Activity（ErpCrmActivity）。Customer/Contact 归属 master-data 域（ErpMdPartner），不在 CRM view.xml 覆盖范围内，不计入本报告。

实体分类：
- **CRUD**（30 个）：字典/配置/审计/报表类实体，仅需 CRUD 基线
- **CRUD+Custom**（2 个）：ErpCrmLead（需 pipeline 状态操作按钮）、ErpCrmEvent（需状态变更按钮）
- **CRUD**（2 个）：ErpCrmCampaign、ErpCrmActivity（仅需 CRUD 基线）

## 期望按钮推导依据

1. **CRUD 基线**（METHODOLOGY §1.1）：所有实体默认期望 `add-button`、`batch-delete-button`、`row-view-button`、`row-update-button`、`row-delete-button`。
2. **ui-patterns.md**：
   - 线索详情页工具栏「[编辑] [[跟进]] [[转化▾]] [[标记丢失]] [[取消]] [[创建活动▾]] [[操作历史]]」— ui-patterns.md:65-66
   - 列表页操作按钮「NEW 显示[编辑][验证][取消]，QUALIFIED 显示[跟进][转化][标记丢失]」— ui-patterns.md:56-57
   - 线索状态机 README.md §状态机：`NEW→QUALIFIED`、`QUALIFIED→CONVERTED`、`*→LOST`、`*→CANCELLED`
   - 营销活动详情页含归因报表— ui-patterns.md §营销活动详情
3. **domain-design-guidelines.md** §十六：CRM docStatus 取值 `NEW`/`QUALIFIED`/`CONVERTED`/`LOST`/`CANCELLED`，无审批轴（`approveStatus` 不适用）。
4. **ErpCrmEvent 状态机**（README.md §状态机）：`PLANNED→COMPLETED` 或 `PLANNED→CANCELLED`。
5. Customer/Contact 实体在 CRM 域无独立实体映射（通过 `partnerId→ErpMdPartner`），不产生 CRM view.xml 期望。

## 逐实体分析

### ErpCrmLead（线索/商机）— CRUD+Custom

- **期望按钮**：
  - CRUD 基线：`add-button`, `batch-delete-button`, `row-view-button`, `row-update-button`, `row-delete-button`
  - Pipeline 动作（按 docStatus 动态渲染）：
    - `row-qualify-button`（NEW→QUALIFIED，ui-patterns.md:57「验证」）
    - `row-convert-button`（QUALIFIED→CONVERTED，ui-patterns.md:65「转化▾」）
    - `row-lose-button`（*→LOST，ui-patterns.md:65「标记丢失」）
    - `row-cancel-button`（*→CANCELLED，ui-patterns.md:57「取消」）
  - 联动动作（详情页工具栏）：
    - `row-create-event-button`（创建活动▾，ui-patterns.md:66）
- **实际按钮**：CRUD 基线仅 5 个（add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button）。确认于 `_dump/nop-app/erp/crm/pages/ErpCrmLead/ErpCrmLead.view.xml:343-365`。
- **差距**：
  - `row-qualify-button`: missing (**blocker**) — ui-patterns.md:57 明确要求 NEW 状态显示「验证」按钮；docStatus 状态机要求 NEW→QUALIFIED 入口。
  - `row-convert-button`: missing (**blocker**) — ui-patterns.md:56,65 明确要求 QUALIFIED 显示「转化」按钮及详情页「转化▾」工具栏；Lead→Opportunity→Quotation 转化流是 CRM 核心流程。
  - `row-lose-button`: missing (**blocker**) — ui-patterns.md:56,65 明确要求标记丢失入口；状态机要求 *→LOST。
  - `row-cancel-button`: missing (**major**) — ui-patterns.md:57 要求显示「取消」按钮；状态机要求 *→CANCELLED。
  - `row-create-event-button`: missing (**minor**) — ui-patterns.md:66 详情页工具栏含「创建活动▾」。
- **判定**：blocker

### ErpCrmCampaign（营销活动）— CRUD

- **期望按钮**：CRUD 基线 5 个。ui-patterns.md 提及「[导出]」（列表页，ui-patterns.md:33），属 info 级增强点。
- **实际按钮**：CRUD 基线 5 个。确认于 `_dump/nop-app/erp/crm/pages/ErpCrmCampaign/ErpCrmCampaign.view.xml:215-237`。
- **差距**：
  - `export-button`: missing (**info**) — ui-patterns.md:33 列表页标注「[导出]」，属非关键增强。
- **判定**：clean（info 级不改变分类）

### ErpCrmEvent（活动/事件）— CRUD+Custom

- **期望按钮**：
  - CRUD 基线 5 个
  - 状态变更：`row-complete-button`（PLANNED→COMPLETED，README.md §状态机）、`row-cancel-button`（PLANNED→CANCELLED）
- **实际按钮**：CRUD 基线 5 个。确认于 `_dump/nop-app/erp/crm/pages/ErpCrmEvent/ErpCrmEvent.view.xml:238-264`。
- **差距**：
  - `row-complete-button`: missing (**major**) — Event 状态机要求 PLANNED→COMPLETED 入口。
  - `row-cancel-button`: missing (**major**) — Event 状态机要求 PLANNED→CANCELLED 入口。
- **判定**：major

### ErpCrmActivity（活动记录）— CRUD

- **期望按钮**：CRUD 基线 5 个。Activity 是轻量操作日志（NOTE/CALL/EMAIL/MEETING），无独立状态机。
- **实际按钮**：CRUD 基线 5 个。确认于 `_dump/nop-app/erp/crm/pages/ErpCrmActivity/ErpCrmActivity.view.xml:155-181`。
- **差距**：无
- **判定**：clean

### 字典/配置/报表实体（29 个，无独立 ui-patterns 需求）

所有字典实体（ErpCrmLeadStatus、ErpCrmStage、ErpCrmSource、ErpCrmLostReason、ErpCrmEventCategory 等）、配置实体（ErpCrmTeam、ErpCrmTerritory、ErpCrmConfigRule 等）、审计实体（ErpCrmLeadConvLog）、评分实体（ErpCrmLeadScore*）、预测实体（ErpCrmForecast*）、定价实体（ErpCrmBundlePricing*）、序列实体（ErpCrmSequence*）等 29 个实体——期望仅 CRUD 基线，实际全部为 CRUD 基线。判定 **clean**。

完整列表：ErpCrmBundlePricing, ErpCrmBundlePricingLine, ErpCrmConfigRule, ErpCrmEventCategory, ErpCrmForecast, ErpCrmForecastAccuracy, ErpCrmForecastLine, ErpCrmForecastPeriod, ErpCrmFunnelStageMetrics, ErpCrmLeadConvLog, ErpCrmLeadFunnel, ErpCrmLeadScore, ErpCrmLeadScoreConfig, ErpCrmLeadScoreConfigLine, ErpCrmLeadScoreLine, ErpCrmLeadSequenceProgress, ErpCrmLeadStatus, ErpCrmLostReason, ErpCrmPriceRule, ErpCrmProductConfigurator, ErpCrmQuota, ErpCrmQuoteTemplate, ErpCrmSequence, ErpCrmSequenceAssignment, ErpCrmSequenceStep, ErpCrmSource, ErpCrmStage, ErpCrmTeam, ErpCrmTerritory, ErpCrmTerritoryAssignmentRule。

## 摘要

| 分类 | 实体 | 差距数 | 最高严重级 | 备注 |
|------|------|--------|-----------|------|
| CRUD+Custom | ErpCrmLead | 5 | blocker | 缺失 qualify/convert/lose/cancel/create-event，Lead 是 CRM 核心 pipeline 实体 |
| CRUD | ErpCrmCampaign | 1 | info | 仅缺少导出按钮（增强项） |
| CRUD+Custom | ErpCrmEvent | 2 | major | 缺失 complete/cancel（Event 有 PLANNED→COMPLETED/CANCELLED 状态机） |
| CRUD | ErpCrmActivity | 0 | clean | — |
| CRUD | 29 个字典/配置/审计/报表实体 | 0 | clean | — |

### 总评
- 总实体数：34
- 无差距实体：30（88.2%）
- Blocker 差距：1（ErpCrmLead）
- Major 差距：1（ErpCrmEvent）
- Minor/Info 差距：1（ErpCrmCampaign 导出按钮）

**核心发现**：CRM 域 88% 实体（字典/配置/审计）均满足 CRUD 基线，无差距。但核心 pipeline 实体 **ErpCrmLead** 缺少全部 4 个状态机动作按钮（qualify/convert/lose/cancel），这是 blocker 级问题——无这些按钮则 Lead→Opportunity→Quotation 转化流无法在 UI 触发，CRM 核心业务流程中断。**ErpCrmEvent** 缺少 complete/cancel 按钮（major 级），事件状态无法从 PLANNED 推进。建议修复 ErpCrmLead.view.xml（添加 row-qualify-button、row-convert-button、row-lose-button、row-cancel-button、row-create-event-button）和 ErpCrmEvent.view.xml（添加 row-complete-button、row-cancel-button）。
