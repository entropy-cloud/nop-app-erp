# CRM 域 - 线索漏斗分析（Lead Waterfall / Funnel Analytics）

## 目的

设计线索漏斗分析引擎：追踪每条线索在各阶段的停留时间、阶段间转化率、整体漏斗各层容量，支持丢失原因按阶段分析，提供可视化漏斗报表数据源。

## 边界

- 本模块负责：阶段转化时序追踪、停留时间分析、转化率计算、漏斗容量视图、丢失原因阶段分析。
- 漏斗分析引擎读取 `ErpCrmLeadConvLog`（阶段流转日志）和 `ErpCrmLead`（当前状态）作为原始数据源，预聚合到 `ErpCrmLeadFunnel` 和 `ErpCrmFunnelStageMetrics` 供报表查询。
- 本模块不负责：阶段定义（`ErpCrmStage`）；线索创建与推进（`ErpCrmLead`）；流转日志记录（`ErpCrmLeadConvLog`）；预测计算（`sales-forecast.md`）。
- 实体建议命名，ORM 模型见 `module-crm/model/app-erp-crm.orm.xml`。

## 设计依据

> 参考 **Marketo Lead Waterfall**（`Lead Waterfall` 报告）：按阶段展示线索数量、进入时间、停留天数、转化率。支持按来源/区域/产品线维度筛选。
>
> 参考 **Salesforce Funnel Analytics**（`Funnel` / `StageHistory` 报表）：基于 `OpportunityStageHistory` 的阶段停留时间 + 阶段间转化率 + 丢失原因阶段归因。
>
> 参考 **Tableau CRM Funnel Dashboard**：可视化漏斗各层容量，展示通过率，支持向下钻取单阶段详情。

## 实体清单

> 表前缀 `erp_crm_`、类名 `ErpCrm*`、字典 `erp-crm/*`。
>
> `ErpCrmLeadFunnel` 为物化聚合表（定时 Job 刷新），`ErpCrmFunnelStageMetrics` 为阶段明细度量。

### ErpCrmLeadFunnel（线索漏斗物化聚合）

按期间 + 组织 + 区域维度聚合的漏斗各层容量。每次聚合计算覆盖一个时间段的数据。

| 字段 | 含义 | 参考 |
|------|------|------|
| id/orgId | 标准 | — |
| funnelName | 漏斗名称/标签（如 "2026-Q3 商机漏斗"） | — |
| periodStart | 分析期间开始 | 🟢 Salesforce 漏斗报表日期范围 |
| periodEnd | 分析期间结束 | — |
| territoryId | 区域维度（→ErpCrmTerritory，可空） | — |
| teamId | 团队维度（→ErpCrmTeam，可空） | — |
| sourceId | 来源维度（→ErpCrmSource，可空） | 🟢 Marketo 来源筛选 |
| totalLeadsAtTop | 漏斗顶部线索总量 | — |
| totalOpportunities | 商机总量（leadType=OPPORTUNITY） | — |
| totalWon | 赢单总量 | — |
| totalLost | 丢单总量 | — |
| totalRevenue | 赢单总收入 | — |
| lostRevenue | 丢失金额合计 | — |
| weightedRevenue | 加权收入合计 | — |
| avgDealSize | 平均赢单金额 | — |
| avgSalesCycleDays | 平均销售周期（天，从 QUALIFIED 到 CONVERTED） | — |
| calculatedAt | 聚合计算时间 | — |
| calculatedBy | 聚合计算人 | — |
| 标准审计字段 | | |

### ErpCrmFunnelStageMetrics（阶段度量明细）

每个阶段在分析期间内的度量明细。

| 字段 | 含义 | 参考 |
|------|------|------|
| id/funnelId/orgId | 标准 + 所属漏斗（→ErpCrmLeadFunnel） | — |
| stageId | 阶段（→ErpCrmStage） | 🟢 Salesforce `StageHistory` |
| stageOrder | 阶段排序（冗余，方便报表排序） | — |
| stageName | 阶段名称（快照） | — |
| leadCountIn | 进入本阶段的线索数（期间内首次进入） | 🟢 Marketo 阶段入口计数 |
| leadCountOut | 流出本阶段的线索数（进入下一阶段或流失） | — |
| leadCountRemaining | 期末仍在本阶段的线索数 | — |
| conversionRate | 本阶段→下一阶段转化率（leadCountOutForward / leadCountIn） | 🟢 Salesforce 阶段转化率 |
| dropOffRate | 本阶段流失率（leadCountLost / leadCountIn） | — |
| avgDaysInStage | 平均在本阶段停留天数 | 🟢 Salesforce `StageHistory.DaysInStage` |
| lostCount | 在本阶段丢失的线索数 | — |
| lostAmount | 在本阶段丢失的金额合计 | — |
| lostReasonTop | 本阶段 TOP 丢失原因（JSON，如 `{"价格": 5, "竞品": 3}`） | — |
| 标准审计字段 | | |

## 数据来源与聚合逻辑

### 聚合数据源

```
ErpCrmLeadConvLog（阶段流转日志）：fromStageId, toStageId, changedAt, leadId
ErpCrmLead（线索主表）: docStatus, stageId, leadType, lostReasonId, expectedRevenue
ErpCrmStage（阶段定义）: sequence, stageName
ErpCrmLostReason（丢单原因）: name
```

### 聚合计算流程

```
定时 Job（每日/每周）→
  1. 确定分析期间（periodStart ~ periodEnd）
  
  2. 计算 ErpCrmLeadFunnel 头：
     totalLeadsAtTop = 期间内进入第一阶段的新线索数
     totalOpportunities = 期间内首次变为 OPPORTUNITY 的线索数
     totalWon = 期间内 docStatus=CONVERTED 且 isWonStage=true 的线索数
     totalLost = 期间内 docStatus=LOST 的线索数
     totalRevenue = SUM(CONVERTED 线索的 expectedRevenue)
     avgSalesCycleDays = AVG(CONVERTED 线索的 cycleDays)
  
  3. 计算 ErpCrmFunnelStageMetrics 明细（每个阶段一条）：
     leadCountIn = 期间内第一条 ConvLog 进入本阶段的线索数
     leadCountOutForward = ConvLog 从本阶段进入下一阶段的线索数
     leadCountOutLost = ConvLog 从本阶段进入 LOST 的线索数
     conversionRate = leadCountOutForward / leadCountIn
     avgDaysInStage = AVG(exitTime - entryTime) 在期间内的记录
     lostReasonTop = GROUP BY lostReasonId 取前 3 名（JSON 聚合）
  
  4. 更新或插入聚合表（upsert by funnelId + stageId）
```

### 时间线分析（单线索视角）

每条线索的阶段历史可以从 `ErpCrmLeadConvLog` 重建时间线：

```
Lead A 时间线：
  2026-07-01 → 进入 Stage 1（新线索）
  2026-07-05 → 进入 Stage 2（已联系）→ 停留 4 天
  2026-07-12 → 进入 Stage 3（需求分析）→ 停留 7 天
  2026-07-15 → 标记 LOST → 停留在 Stage 3，原因："价格太高"
```

从时间线可以计算：
- 各阶段停留天数
- 从创建到丢单的总天数
- 丢单时所在的阶段（归因到具体阶段）

## 业务规则

### 1. 阶段停留时间计算

```
每条 ConvLog 记录 (leadId, fromStageId, toStageId, changedAt) →
  某线索在 Stage N 的停留时间 =
    MIN(进入 Stage N+1 的 changedAt, 丢失/转化的 changedAt, 分析期末)
    - 进入 Stage N 的 changedAt
```

### 2. 转化率计算

```
Stage N → Stage N+1 转化率 =
  (期间内从 Stage N 前进到 Stage N+1 的线索数)
  / (期间内进入 Stage N 的线索总数)

Stage N 流失率 =
  (期间内从 Stage N 丢失的线索数)
  / (期间内进入 Stage N 的线索总数)
```

### 3. 丢失原因阶段归因

```
丢失原因按丢单时的 stageId 归因：
  每一笔丢单记录 (leadId, lostReasonId, stageId) →
    stageId 是丢单时线索所在的阶段
  聚合：每个阶段的前 N 个丢失原因和数量
```

### 4. 漏斗可视化数据结构

前端漏斗图展示需要的数据结构：

```json
{
  "funnelName": "2026-Q3 商机漏斗",
  "stages": [
    {"name": "新线索",       "count": 1000, "avgDays": 2.3},
    {"name": "已联系",       "count": 800,  "avgDays": 4.1, "conversionRate": 0.80},
    {"name": "需求分析",     "count": 500,  "avgDays": 7.5, "conversionRate": 0.63},
    {"name": "方案演示",     "count": 300,  "avgDays": 5.2, "conversionRate": 0.60},
    {"name": "谈判中",       "count": 150,  "avgDays": 10.1, "conversionRate": 0.50},
    {"name": "赢单",         "count": 80,   "avgDays": null},
    {"name": "丢失",         "count": 200,  "lostReasonTop": {"价格": 80, "竞品": 60, "决策延迟": 40}}
  ]
}
```

### 5. 聚合刷新策略

| 触发方式 | 说明 |
|---------|------|
| 定时 Job（每日凌晨） | 常规刷新，覆盖所有分析期间 |
| 手动刷新（管理员触发） | 重新计算指定期间 |
| 增量更新（实时，可选） | ConvLog 插入时更新度量子（高阶方案） |

## 配置点

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `erp-crm.funnel.aggregation-cron` | 0 0 3 * * ? | 漏斗聚合定时 Job（每日凌晨 3 点） |
| `erp-crm.funnel.retention-period-months` | 24 | 漏斗历史数据保留月数 |
| `erp-crm.funnel.top-lost-reasons` | 5 | 每个阶段展示的 TOP 丢失原因数 |

## 状态机关联

漏斗分析不改变任何状态。纯读取 `ErpCrmLeadConvLog` + `ErpCrmLead` 的聚合引擎，无状态依赖。

## 反模式警示

- ⛔ **漏斗数据实时查询原始表**——`ErpCrmLeadConvLog` 可能海量（单线索多次往返阶段），预聚合到 `ErpCrmLeadFunnel` / `ErpCrmFunnelStageMetrics` 避免查询超时。
- ⛔ **阶段定义变化后漏斗历史错误**——阶段定义（stageName/sequence）变更时，`ErpCrmFunnelStageMetrics` 保存 stageName 快照，历史阶段名称不变。
- ⛔ **丢失原因只聚合最顶层**——丢失原因按阶段归因才能发现"哪一阶段最容易因价格输单"。
- ⛔ **漏斗仅展示数量不展示金额**——`leadCountIn` 和 `lostAmount` 应同时展示，避免"大量小单过漏斗"误导。

## 跨域协作

| 对端 | 协作方式 |
|------|---------|
| CRM（ErpCrmLeadConvLog） | 阶段流转日志作为聚合原始数据源 |
| CRM（ErpCrmLead） | 当前状态、金额、丢失原因作为聚合数据源 |
| CRM（ErpCrmStage） | 阶段定义和排序 |
| CRM（ErpCrmLostReason） | 丢失原因名称查询 |

## 证据强度标注

| 证据 | 强度 | 说明 |
|------|------|------|
| 阶段转化追踪（stage conversion tracking） | 🟢 | Salesforce `OpportunityStageHistory`；Marketo `Lead Waterfall` |
| 停留时间分析（time-in-stage analytics） | 🟢 | Salesforce `StageHistory.DaysInStage` |
| 转换率按阶段 | 🟢 | Salesforce 漏斗报表 |
| 水分漏斗视图（waterfall visualization） | 🟢 | Marketo/Tableau CRM 漏斗图 |
| 丢失原因阶段归因 | 🟢 | Salesforce 丢单分析报表 |
| 聚合刷新策略 | 🟡 | Tableau CRM 数据流定时刷新 |
| 增量实时更新 | ⚪ | 高阶方案，初期用定时 Job 即可 |

## 参考

- `state-machine.md` §Lead §ConvLog（阶段流转审计数据来源）
- `README.md` §ErpCrmLeadConvLog §ErpCrmStage（核心依赖实体）
- `use-cases.md` §UC-CRM-15（漏斗分析用例）
- `../../analysis/erp-survey/` — Salesforce/Marketo 漏斗分析机制

## 实现注记（plan 2026-07-07-1430-3）

- **聚合刷新策略（清旧重建）**：`refreshFunnel` 采用清旧重建范式（对齐 0700-1 forecast），按 periodStart/periodEnd + 维度（territoryId/teamId/sourceId）精确匹配既有 LeadFunnel + FunnelStageMetrics 删除后重建。
- **增量实时更新归 successor**：本期用定时 Job 全量刷新 + 手动 refresh，不实时更新度量子（design 标注为高阶方案 ⚪）。触发条件：实时漏斗监控业务需求上线时。
- **stageName 快照**：FunnelStageMetrics.stageName 在聚合时刻取 ErpCrmStage.stageName 快照，防阶段定义后续变更致历史错误。
- **丢失原因 TOP N**：每阶段丢失原因按 lostReasonId 聚合计数降序取 TOP N（配置键 `erp-crm.funnel.top-lost-reasons`，默认 5），以 JSON 字符串存入 FunnelStageMetrics.lostReasonTop。
- **漏斗 AMIS 可视化前端归 successor**：本期后端聚合 + getFunnelView 查询就绪；前端漏斗图归 successor（触发条件：CRM 前端可视化套件建立时）。
