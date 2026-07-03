# CRM 域 - 销售预测（Sales Forecast）

## 目的

设计销售预测系统：基于商机管道的阶段概率，计算加权管道收入；按销售员/团队/区域分层汇总预测金额；区分承诺金额（commit）与乐观金额（upside）；追踪预测准确率，驱动管理层决策。

## 边界

- 本模块负责：预测期间管理、加权管道计算、commit/upside 分离、预测层级汇总（个人→团队→区域→公司）、历史准确率追踪。
- 预测引擎是 CRM `ErpCrmLead` 的聚合层——读取 `lead.expectedRevenue` × `lead.probability` 按阶段聚合。
- 本模块不负责：线索/商机主数据（`ErpCrmLead`）；漏斗阶段配置（`ErpCrmStage`）；实际收入回写（sales 域 `ErpSalQuotation` 核销后在 CRM 外部）。

## 设计依据

> 参考 **ERPNext opportunity-to-forecast pipeline**（`Opportunity` → `expected_revenue` × `probability` 加权管道 + `opportunity_forecast` 报表）：按销售员/月份汇总加权金额，commit/upside 分离基于概率阈值。
>
> 参考 **Salesforce forecasting**：`forecastingitem`（预测条目）/ `forecastingperiod`（预测期间）/ `forecastingtype`（commit/upside/bestcase 分类）三层模型，预测层级 = 用户层级（individual → team → region → company）。

## 实体清单

> 表前缀 `erp_crm_`、类名 `ErpCrm*`、字典 `erp-crm/*`。

### ErpCrmForecastPeriod（预测期间）

定义预测的时间窗口。期间可手动创建或自动按月/季/年模板生成。

| 字段 | 含义 | 参考 |
|------|------|------|
| id/code/orgId | 标准 | — |
| periodType | dict `erp-crm/forecast-period-type`：MONTHLY（月度）/ QUARTERLY（季度）/ ANNUAL（年度） | 🟢 Salesforce `forecastingperiod.periodType` |
| periodStart/periodEnd | 期间起止日期 | — |
| label | 期间标签（如"2026-Q3"） | — |
| status | dict `erp-crm/forecast-period-status`：OPEN（进行中）/ CLOSED（已结束）/ FROZEN（已冻结，不再重新计算） | — |
| isCurrent | 是否当前活跃期间 | — |
| 标准审计字段 | | |

### ErpCrmForecast（预测数据行）

预测的核心聚合行。每个 `periodId × territoryId × teamId × ownerId` 唯一。

| 字段 | 含义 | 参考 |
|------|------|------|
| id/orgId | 标准 | — |
| periodId | 预测期间（→ErpCrmForecastPeriod） | 🟢 Salesforce `forecastingitem` |
| territoryId | 销售区域（→ErpCrmTerritory，可选，层级汇总用） | 🟢 Salesforce 预测层级 |
| teamId | 销售团队（→ErpCrmTeam，可选） | 🟢 Odoo sales teams |
| ownerId | 销售员（→User，可选。为空表示团队/区域汇总行） | 🟢 ERPNext 按销售员汇总 |
| currencyId | 币种 | — |
| commitAmount | 承诺金额：Σ(expectedRevenue) WHERE probability >= commitThreshold（默认 >= 80%） | 🟢 Salesforce `forecastingitem.forecast` = commit |
| upsideAmount | 乐观金额：Σ(expectedRevenue) WHERE probability >= upsideThreshold AND probability < commitThreshold（默认 >= 30% 且 < 80%） | 🟢 Salesforce `forecastingitem.forecast` = upside |
| weightedAmount | 加权金额：Σ(expectedRevenue × probability / 100) | 🟢 ERPNext opportunity forecast 报表 |
| bestCaseAmount | 最佳金额：Σ(expectedRevenue)（全部商机，忽略概率） | 🟢 Salesforce `forecastingitem.bestcase` |
| opportunityCount | 参与计算的商机总数 | — |
| commitOpportunityCount | 归属 commit 的商机数 | — |
| expectedClosedRevenue | 实际已关闭收入（期间内 CONVERTED 的商机 `expectedRevenue` 汇总） | — |
| lastCalculatedAt | 最近一次重新计算时间 | — |
| notes | 预测备注（销售员手写注释） | 🟢 Salesforce 笔记 |
| 标准审计字段 | | |

### ErpCrmForecastLine（预测明细溯源行）

记录每个商机对预测行的贡献，支持钻取查询。

| 字段 | 含义 |
|------|------|
| id/forecastId/orgId | 标准 |
| leadId | 商机（→ErpCrmLead） |
| probability | 商机当前概率（快照，预测计算时的值） |
| expectedRevenue | 商机预期收入（快照） |
| weightedRevenue | 加权收入（= expectedRevenue × probability / 100） |
| forecastCategory | dict `erp-crm/forecast-category`：COMMIT（承诺）/ UPSIDE（乐观）/ BEST_CASE（最佳） |
| includedInCommit | 是否计入 commitAmount（probability >= commitThreshold） |
| stageName | 阶段名（快照） |
| 标准审计字段 | |

### ErpCrmForecastAccuracy（预测准确率追踪）

期间关闭后，对比预测金额与实际收入，计算准确率。

| 字段 | 含义 | 参考 |
|------|------|------|
| id/forecastId/orgId | 标准 | — |
| periodId | 预测期间（→ErpCrmForecastPeriod） | — |
| ownerId/teamId/territoryId | 汇总维度（与预测行对齐） | — |
| commitAmount | 期间末的最终 commitAmount | — |
| upsideAmount | 期间末的最终 upsideAmount | — |
| actualClosedRevenue | 实际关闭收入（期间内 `ErpCrmLead` CONVERTED 的 expectedRevenue 汇总） | — |
| commitAccuracy | 准确率：commit 口径 = 1 - \|commitAmount - actualClosedRevenue\| / MAX(commitAmount, actualClosedRevenue) | 🟢 Salesforce `forecastingaccuracy` |
| upsideAccuracy | 准确率：upside 口径（含部分未关闭） = 1 - \|upsideAmount - actualClosedRevenue\| / MAX(upsideAmount, actualClosedRevenue) | — |
| deviationAmount | 偏差绝对值 | — |
| calculatedBy | 计算人（Job 自动或手动触发） | — |
| calculatedAt | 计算时间 | — |
| 标准审计字段 | | |

## 业务规则

1. **加权管道计算**：`weightedAmount = Σ(lead.expectedRevenue × lead.probability / 100)`，仅计算 `leadType=OPPORTUNITY` 且 `docStatus=QUALIFIED` 且 `expectedCloseDate` 在当前期间内的商机。
2. **Commit / Upside / Best Case 分类**：

   | 分类 | 条件 | 行为 |
   |------|------|------|
   | COMMIT | probability >= commitThreshold（默认 80%） | 计入 `commitAmount`，管理层按此规划资源 |
   | UPSIDE | upsideThreshold <= probability < commitThreshold（默认 30%-80%） | 计入 `upsideAmount`，乐观预期 |
   | BEST_CASE | 全部商机（含 probability < 30%） | 计入 `bestCaseAmount`，上限参考 |

3. **预测层级聚合**：
   ```
   个人预测（ownerId 非空, teamId/territoryId 为空）
         │
         ▼
   团队预测（teamId 非空, ownerId 为空）
   字段 = SUM(团队成员的个人预测对应字段)
         │
         ▼
   区域预测（territoryId 非空, teamId 为空）
   字段 = SUM(区域内团队的团队预测对应字段)
         │
         ▼
   公司预测（territoryId/teamId/ownerId 均为空）
   字段 = SUM(所有区域预测对应字段)
   ```

4. **预测重新计算触发**：
   - 商机 `probability` 或 `expectedRevenue` 或 `expectedCloseDate` 变更 → 异步重新计算受影响期间的预测行
   - `stageId` 变更（特别是达到 `isWonStage`）→ 重新计算
   - 商机 `docStatus → CONVERTED` → 重新计算 + 累计 `expectedClosedRevenue`
   - 管理员手动"刷新预测"
   - 定时 Job（每日凌晨重算所有 OPEN 期间）

5. **期间变更不可改写历史**：预测期间 CLOSED/FROZEN 后冻结预测数据（`lastCalculatedAt` 固定，不再重新计算）。
6. **准确率计算时机**：预测期间 CLOSED 后，自动计算 `ErpCrmForecastAccuracy`。期间内可临时查看实时准确率（供参考，不持久）。
7. **预测只读展示层**：`commitAmount` / `upsideAmount` 等是派生字段，由系统计算。销售员可写 `notes` 调整说明。

### 预测重新计算流程

```
触发（商机变更 / 手动 / 定时 Job）
    │
    ├─ 确定受影响的 ErpCrmForecastPeriod（OPEN 状态）
    │
    ├─ 查询该期间内所有符合条件的商机：
    │     leadType=OPPORTUNITY, docStatus=QUALIFIED,
    │     expectedCloseDate BETWEEN periodStart AND periodEnd
    │
    ├─ 按 ownerId 分组聚合：
    │     commitAmount = Σ(expectedRevenue) WHERE probability >= commitThreshold
    │     upsideAmount = Σ(expectedRevenue) WHERE probability BETWEEN upsetThreshold AND commitThreshold
    │     weightedAmount = Σ(expectedRevenue × probability / 100)
    │     bestCaseAmount = Σ(expectedRevenue)
    │
    ├─ 写入 / 更新 ErpCrmForecast（upsertBy ownerId + periodId + teamId + territoryId）
    │
    ├─ 创建 ErpCrmForecastLine（明细，每个商机一条）
    │
    └─ 触发上级层级聚合（团队 → 区域 → 公司）
```

## 配置点

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `erp-crm.forecast.commit-threshold` | 80 | commit 概率阈值（%） |
| `erp-crm.forecast.upside-threshold` | 30 | upside 概率阈值（%） |
| `erp-crm.forecast.auto-create-period` | true | 是否自动创建下期预测期间（月度/季度） |
| `erp-crm.forecast.recalc-cron` | 0 3 * * * | 定时重算 cron（每日凌晨 3 点） |
| `erp-crm.forecast.accuracy-auto-compute` | true | 期间关闭后是否自动计算准确率 |

## 状态机关联

预测期间状态机独立于线索状态机：

```
OPEN（当前可刷新预测）
  ├─ 手动冻结 → FROZEN（锁定预测，不再更新，终态）
  ├─ 自动结束 → CLOSED（期间结束，触发准确率计算，终态）
  └─ → CLOSED（直接关闭）
```

预测不修改 `ErpCrmLead.docStatus`，是纯聚合查询层。

## 反模式警示

- ⛔ **预测行直接存快照 JSON 替代明细行**——`ErpCrmForecastLine` 保留商机级明细支持钻取，避免"黑箱预测"。
- ⛔ **预测结果覆盖实际收入字段**——`expectedClosedRevenue` 与 `commitAmount` 独立维护，`actualClosedRevenue` 仅在实际关闭后更新。
- ⛔ **实时聚合写入预测表**——预测重算异步执行（商机变更→发事件→消费端批量聚合），不阻塞商机保存流程。
- ⛔ **预测与目标/配额混合**——预测是"预计收入"，配额（quota）是"目标收入"，两套独立数据。预测不修改配额。

## 跨域协作

| 对端 | 协作方式 |
|------|---------|
| CRM（ErpCrmLead） | 读取商机概率 + 预期收入 + 预期关闭日期 |
| CRM（ErpCrmStage） | isWonStage 触发 CONVERTED，参与 actualClosedRevenue 计算 |
| CRM（ErpCrmTeam） | 团队层级聚合 |
| CRM（ErpCrmTerritory） | 区域层级聚合（territory 域） |
| sales（ErpSalQuotation） | 不直接调用——预测仅基于 CRM 商机数据，实际收入在 sales 域闭合后通过回写标记 |

## 证据强度标注

| 证据 | 强度 | 说明 |
|------|------|------|
| 加权管道（expectedRevenue × probability） | 🟢 | ERPNext `Opportunity.expected_revenue` + `.probability` → opportunity_forecast 报表 |
| commit/upside 分离（概率阈值） | 🟢 | Salesforce forecasting `forecastcategory` commit/upside/bestcase |
| 预测层级（individual→team→region→company） | 🟢 | Salesforce 预测层级模型（`forecastingitem.user/forecastinggroup`） |
| 预测期间管理 | 🟢 | Salesforce `forecastingperiod`（periodType/startDate/endDate） |
| 预测准确率 | 🟡 | Salesforce `forecastingaccuracy`（commit 准确率） |
| 预测明细溯源（ErpCrmForecastLine） | 🟡 | Salesforce `forecastingitem` 行级，ERPNext 无独立明细 |
| 最佳金额（best case） | 🟢 | Salesforce `forecastingitem.bestcase` |
| 销售员预测备注 | 🟢 | Salesforce 预测 grid 中的 notes 列 |

## 参考

- `use-cases.md` §UC-CRM-03（商机转报价单，触发 actual 收入更新）
- `state-machine.md` §Lead（docStatus CONVERTED → 触发预测准确率更新）
- `README.md` §ErpCrmLead §ErpCrmStage（预测数据源）
- `territory.md`（区域层级，预测聚合维度）
- `docs/analysis/erp-survey/` — Salesforce/ERPNext/Odoo 预测模型分析

## 实现偏离补注

> **实现偏离补注**（2026-07-04，plan 2026-07-04-0700-1 §3.4）：预测聚合引擎实现于 `ErpCrmForecastBizModel.refreshForecast`（GraphQL 动作 `ErpCrmForecast__refreshForecast`）+ 期间状态机于 `ErpCrmForecastPeriodBizModel.freeze/closePeriod`（GraphQL 动作 `ErpCrmForecastPeriod__freeze/closePeriod`），核心逻辑在 support 类 `ForecastAggregator`。层级 rollup 首版实现个人（ownerId 非空）→ 团队（teamId 非空、ownerId 空）→ 公司（均为空）三级；区域（territory）层级因 Lead ORM 无 territoryId 直接关联暂未实现（记 Follow-up，触发条件：Lead→Territory 映射就绪时）。`refreshForecast` 采用"清旧重建"策略（先删除期间内所有 Forecast + ForecastLine 再重建）而非逐行 upsert，保证一致性。多币种首版不做汇率换算（`currencyId` 取商机币种，跨币种聚合按 Lead 主币种记，符合 Non-Goal）。准确率公式：`accuracy = 1 - |预测 - 实际| / MAX(预测, 实际)`，预测与实际均为 0 时返回 1.0。`commitAccuracy`/`upsideAccuracy` 为 Double 类型（ORM 列 COMMIT_ACCURACY/UPSIDE_ACCURACY 映射）。FROZEN/CLOSED 拒绝重算抛 `ERR_FORECAST_PERIOD_NOT_OPEN`。
