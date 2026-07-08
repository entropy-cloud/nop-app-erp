# 制造域业务交易单据种子 — 表清单、列映射、加载拓扑序与范围裁决

> Owner: `docs/plans/2026-07-09-0930-1-manufacturing-transaction-seeds.md` Phase 1 Exit Criteria
> 权威源: `module-manufacturing/model/app-erp-manufacturing.orm.xml`（逐表逐列核实，非采信旧记忆）
> 上游主数据参照: `docs/analysis/2026-07-08-1234-1-seed-data-table-column-map.md`（21 张主数据 CSV 已 seed）
> 前序种子范式: `docs/analysis/2026-07-08-2210-1-operational-domain-seed-table-map.md`（运营域域表「直 seed」范式，本计划镜像）

## 0. 约定（与 1234-1 / 1445-1 / 2210-1 一致）

- CSV 列名 = 实体 column `code`（UPPER_SNAKE_CASE 数据库列名）。
- `ID` 列虽 `tagSet="seq-default"`，但跨表 FK 引用需固定 ID，故 CSV 显式提供 `ID`。
- 框架自动填充字段（`CREATED_BY`/`CREATE_TIME`/`UPDATED_BY`/`UPDATE_TIME`/`DEL_VERSION`/`VERSION`）由 ORM 拦截器自动填，CSV 不含。
- 多租户 `TENANT_ID` 由框架兜底（1234-1/1445-1/2210-1 经验性确认 seed 无须提供）。
- 布尔列值用小写字符串 `true`/`false`（与 1445-1/2210-1 `POSTED` 列一致）。
- 日期列值 `YYYY-MM-DD`；datetime 列本批不 seed（postedAt/approvedAt 等非 mandatory 审计列一律省略）。
- **关键陷阱（制造域 UoM 列名）**：forecast_line/work_order_line 计量单位列 `code="UO_M_ID"`（驼峰 prop `uoMId`，与采购三张行表 + 库存行表一致），非 `UOM_ID`。CSV 必须按 `code` 区分，列名错配会在启动期抛 NopException。

## 1. 加载拓扑序（DataInitInitializer 按 ORM `getEntityModelsInTopoOrder()` 自动排序）

本批 4 表仅引用 1234-1 已 seed 主数据 + 本批先 seed 的 work_order，不引用 1445-1 P2P/O2C 单据或 2210-1 运营域表：

```
[1234-1 主数据(已 seed)] md_organization/md_currency/md_material/md_uom
  → [域头] erp_mfg_work_order                       （引用 material/currency，看板 PRIMARY）
  → [域头] erp_mfg_forecast                          （引用 org，独立于 work_order）
    → [域行/计算产物]
      erp_mfg_cost_variance                          （workOrderId mandatory FK→work_order）
      erp_mfg_forecast_line                          （forecastId mandatory FK→forecast）
```

> work_order 必须先于 cost_variance：cost_variance.workOrderId mandatory FK→work_order。
> forecast 必须先于 forecast_line：forecast_line.forecastId mandatory FK→forecast。
> work_order 与 forecast 互相独立（无 FK 关联），加载序无约束。

## 2. seed 表清单 + 列映射（每表：mandatory 业务列 / FK 列 / 框架列省略）

> 标注：**M**=mandatory（CSV 须填）；**FK**=外键引用上游已 seed ID；**opt**=可选（默认值或 null，按需填）。框架审计列（DEL_VERSION/VERSION/CREATED_BY 等）全部省略。

### 2.1 工单头（erp_mfg_work_order）— 看板全部 KPI + forecast-variance 报表实际量 PRIMARY

| code 列（角色） | 说明 |
|----|----|
| ID(M) | 显式固定（cost_variance 经 workOrderId 反向引用 + forecast-variance 按 productId 聚合需确定性） |
| CODE(M) | 工单号，唯一键 (code,orgId) |
| ORG_ID(FK org=2) | 业务组织（非 mandatory，但看板/索引按 orgId 聚合，固定填 2=ERP-CO） |
| BOM_ID(opt) | BOM FK；未 seed BOM，留空 |
| ROUTING_ID(opt) | 工艺路线 FK；未 seed，留空 |
| PRODUCTION_VERSION_ID(opt) / SOURCE_MRP_PLAN_ID(opt) / SOURCE_ORDER_TYPE(opt,dict) / SOURCE_ORDER_CODE(opt) | 配置/来源链，全留空 |
| PRODUCT_ID(FK material,M) | 主产出产品（成品），引用 1234-1 material 1-4 |
| PLANNED_QUANTITY(M) | 计划数量 |
| COMPLETED_QUANTITY(opt,default 0) | 完工数量（看板 periodCompletedQty + forecast-variance actualQty 口径） |
| SCRAPPED_QUANTITY(opt,default 0) | 报废数量 |
| BUSINESS_DATE(M) | 工单日期 |
| PLANNED_START_DATE(opt) / PLANNED_END_DATE(opt) | 计划开/完工日期（forecast-variance 区间重叠用 planned 区间 + 延期预警 plannedEndDate<today） |
| ACTUAL_START_DATE(opt) / ACTUAL_END_DATE(opt) | 实际开/完工日期（看板 trend 按 actualEndDate 月份聚合 + periodCompletedQty 按 actualEndDate 区间过滤 + onTimeRate 比较 actual vs planned） |
| CURRENCY_ID(FK currency=1) | 币种（非 mandatory，固定填 1=CNY） |
| MATERIAL_COST(opt,default 0) / LABOR_COST(opt) / OVERHEAD_COST(opt) / SUBCONTRACT_COST(opt) / TOTAL_COST(opt) / UNIT_COST(opt) | 成本四要素汇总（cost_variance.standardAmount 与 material_cost 自洽） |
| DOC_STATUS(M, dict erp-mfg/work-order-status) | 单据状态：DRAFT/SUBMITTED/NOT_STARTED/IN_PROCESS/STOCK_RESERVED/STOCK_PARTIAL/COMPLETED/STOPPED/CLOSED/CANCELLED |
| APPROVE_STATUS(opt, dict wf/approve-status) | 审核状态：UNSUBMITTED/SUBMITTED/APPROVED/REJECTED |
| PRIORITY(opt, dict erp-mfg/priority) | 优先级：LOW/NORMAL/HIGH/URGENT |
| POSTED(opt,default false) | 已过账（统一 false，见 §3） |
| EXCHANGE_RATE(M,default 1) | 汇率（mandatory，固定填 1） |
| AMOUNT_SOURCE(opt,default 0) / AMOUNT_FUNCTIONAL(opt,default 0) | 源/本位币金额 |
| REMARK(opt) | 备注 |

### 2.2 成本差异记录（erp_mfg_cost_variance）— production-variance 报表

| code 列（角色） | 说明 |
|----|----|
| ID(M) | 显式固定 |
| WORK_ORDER_ID(FK work_order,M) | 工单 FK（mandatory，指向本批 work_order） |
| LINE_NO(M) | 行号 |
| VARIANCE_TYPE(M, dict erp-mfg/variance-type) | 差异类型：MATERIAL_USAGE/LABOR_EFFICIENCY/LABOR_RATE/OVERHEAD/VOLUME |
| COST_ELEMENT(M, dict erp-mfg/cost-element) | 成本要素：MATERIAL/LABOR/OVERHEAD/SUBCONTRACT |
| MATERIAL_ID(FK material,opt) | 物料（材料差异填投入原料；其他类型可空） |
| OPERATION_ID(opt) / WORKCENTER_ID(opt) | 工序/工作中心 FK；未 seed BOM-operation/workcenter，全留空 |
| STANDARD_AMOUNT(opt,default 0) / ACTUAL_AMOUNT(opt) / VARIANCE_AMOUNT(opt) | 标准/实际/差异金额（varianceAmount = actualAmount − standardAmount 自洽） |
| VARIANCE_PERCENT(opt,default 0) | 差异百分比（= varianceAmount / standardAmount） |
| STANDARD_QTY(opt) / ACTUAL_QTY(opt) / STANDARD_PRICE(opt) / ACTUAL_PRICE(opt) | 标准/实际量价（MATERIAL_USAGE：价不变量变，标准价=实际价） |
| BUSINESS_DATE(M) | 业务日期 |
| POSTED(opt,default false) | 已过账（统一 false，见 §3） |
| REMARK(opt) | 备注 |

### 2.3 需求预测头（erp_mfg_forecast）— forecast-variance 报表（status=APPROVED 过滤）

| code 列（角色） | 说明 |
|----|----|
| ID(M) | 显式固定（forecast_line.forecastId 反向引用） |
| CODE(M) | 单号，唯一键 (code,orgId) |
| ORG_ID(FK org=2) | 业务组织（非 mandatory，固定填 2） |
| PLAN_NAME(M) | 预测名称 |
| PERIOD_FROM(M) / PERIOD_TO(M) | 预测区间起止 |
| STATUS(M, dict erp-mfg/forecast-status) | 状态：DRAFT/APPROVED/CONSUMED/CANCELLED（报表仅取 APPROVED） |
| REMARK(opt) | 备注 |

### 2.4 需求预测行（erp_mfg_forecast_line）— forecast-variance 报表预测量（按 materialId 聚合）

| code 列（角色） | 说明 |
|----|----|
| ID(M) | 显式固定 |
| FORECAST_ID(FK forecast,M) | 预测头 FK（mandatory） |
| LINE_NO(M) | 行号 |
| MATERIAL_ID(FK material,M) | 物料（**宜与 work_order.productId 对齐**使 forecast-vs-actual 对比有意义；本批 materialId=1=产品甲 对齐 work_order.productId=1） |
| WAREHOUSE_ID(FK warehouse,opt) | 仓库（空=产品级 MRP 消费，本批留空） |
| UO_M_ID(FK uom,M) | 计量单位（**列名陷阱 UO_M_ID**，非 UOM_ID） |
| PERIOD_START(M) / PERIOD_END(M) | 桶起止日期（forecast-variance 区间重叠判断） |
| FORECAST_QTY(M) | 预测数量 |
| SOURCED_FLAG(opt) | 来源标记，留空 |
| REMARK(opt) | 备注 |

**字典码值（已核实 ORM `<dicts>`）**：
- `erp-mfg/work-order-status`：DRAFT/SUBMITTED/NOT_STARTED/IN_PROCESS/STOCK_RESERVED/STOCK_PARTIAL/COMPLETED/STOPPED/CLOSED/CANCELLED → 本批覆盖 IN_PROCESS / STOCK_PARTIAL / COMPLETED 三态（驱动看板 inProcessCount/stockPartialCount/periodCompletedQty/onTimeRate/trend/distribution 六项 KPI）
- `wf/approve-status`：UNSUBMITTED/SUBMITTED/APPROVED/REJECTED → 本批 work_order 用 `APPROVED`
- `erp-mfg/priority`：LOW/NORMAL/HIGH/URGENT → 本批用 `NORMAL`
- `erp-mfg/variance-type`：MATERIAL_USAGE/LABOR_EFFICIENCY/LABOR_RATE/OVERHEAD/VOLUME → 本批用 `MATERIAL_USAGE`（材料用量差异；价格差异由 PPV 在采购入库捕获，避免重复计入，见 ORM 注释）
- `erp-mfg/cost-element`：MATERIAL/LABOR/OVERHEAD/SUBCONTRACT → 本批用 `MATERIAL`
- `erp-mfg/forecast-status`：DRAFT/APPROVED/CONSUMED/CANCELLED → 本批用 `APPROVED`（报表 `loadApprovedForecasts` 仅取 APPROVED）

## 3. 范围 Decision（Phase 1 item 2）

### 3.1 范围 Decision：仅 seed 4 表（crp_load 移出范围至 Deferred）

**选择**：仅 seed 看板/报表直接读且 FK 可解析的 4 表（work_order/cost_variance/forecast/forecast_line），覆盖制造域看板 4 `@BizQuery`（getDashboardKpi/getWorkOrderStatusDistribution/getDashboardTrend/findDelayedWorkOrderAlert，均查 work_order）+ 2 报表（production-variance 读 cost_variance / forecast-variance 读 forecast+forecast_line+work_order）。

**crp_load + crp-load 报表移出范围（Deferred）依据**：
- `erp_mfg_crp_load.workcenterId` 是 **mandatory FK→ErpMfgWorkcenter**（orm L530），且 crp-load 报表经 `ErpMfgReportBizModel.buildCrpLoadDataset`(:237) 委托 `IErpMfgCrpLoadBiz.getLoadReport` → `CrpLoadCalculator` 内部 resolveReportWorkcenters 取 workcenterCode + 从 WorkcenterCalendar/WorkcenterCapacity 算 capacityHours/loadRate/overloaded——**依赖整条 workcenter 配置链（workcenter/calendar/capacity 均未 seed）**。
- 替代方案分析：
  - (A) seed workcenter 配置链 + crp_load：**rejected**——引入 workcenter/workcenter_calendar/workcenter_capacity 配置链膨胀，超出「域表直 seed」范式（2210-1 既定），且与制造域看板（仅查 work_order）非空无关。
  - (B) crp_load + crp-load 报表移出范围至 Deferred：**selected**——范围内 production-variance/forecast-variance 两报表 + 看板 4 KPI 已达成非空核心 Goal；crp-load 报表仍空但属计划内 Deferred（触发条件：workcenter 配置链 seed 落地后）。
- 残留风险：crp-load 报表仍空——本计划 Deferred 段显式登记，触发条件明确。

### 3.2 posted Decision：统一 `posted=false`

本批所有制造域源单据/计算产物统一 `posted=false`。依据（镜像 2210-1 裁决）：
1. 制造域看板/报表读**域表**非 GL（看板读 work_order、production-variance 读 cost_variance、forecast-variance 读 forecast+work_order），`posted` 标志不被看板/报表消费；
2. 1234-1 seed 的 `erp_md_subject` 仅 8 个 GL 科目（库存现金/银行存款/应收/库存商品/应付/主营收入/主营成本/销售费用），**无制造费用/差异/在产品专用科目**，seed GL 凭证徒增参照复杂度且不解除额外看板/报表阻塞；
3. 制造域过账 → GL 凭证 seed 归后续（Deferred：制造域业财一体端到端数值回归需 GL 串联时）。

### 3.3 日期窗口 Decision

work_order 日期 + forecast 期间置于 **2026-06（历史月）与 2026-07（当前月）**，使看板本期 KPI（periodCompletedQty 默认按 actualEndDate 当月区间）+ trend（按 actualEndDate 月份聚合近 12 月）非空。具体设计（4 工单覆盖 IN_PROCESS/STOCK_PARTIAL/COMPLETED 三态 + onTimeRate 准时/延期两类）：

- **WO-1（COMPLETED 准时，6 月）**：product 1、plannedQty 100、completedQty 100、plannedEndDate 2026-06-30、actualEndDate 2026-06-28（≤ planned → 准时分子）。→ periodCompletedQty（6 月区间）/ trend（2026-06）/ onTimeRate（准时分子）/ distribution（COMPLETED）。
- **WO-2（COMPLETED 延期，7 月）**：product 1、plannedQty 100、completedQty 80、plannedEndDate 2026-07-05、actualEndDate 2026-07-08（> planned → 延期分母）。→ periodCompletedQty（7 月区间）/ trend（2026-07）/ onTimeRate（延期分母，准时率=1/2=0.5）/ distribution（COMPLETED）。
- **WO-3（IN_PROCESS 延期预警）**：plannedEndDate 2026-06-30 < today、actualEndDate 空。→ inProcessCount（IN_PROCESS 计数）/ findDelayedWorkOrderAlert（plannedEndDate<today 且非 COMPLETED）/ distribution（IN_PROCESS）。
- **WO-4（STOCK_PARTIAL 延期预警）**：plannedEndDate 2026-06-30 < today。→ stockPartialCount（STOCK_PARTIAL 计数）/ findDelayedWorkOrderAlert / distribution（STOCK_PARTIAL）。

**forecast-variance 口径注意**（区别于看板 trend）：实际量按 **productId 聚合**、区间重叠用 **plannedStartDate/plannedEndDate**（非 actualEndDate，见 `aggregateActualQty` :392-407）。本批 WO-1/WO-2 productId=1，forecast_line.materialId=1 对齐，故 forecast-variance material 1 行 forecastQty=200 / actualQty=180（100+80）/ variance=-20 / varianceRatio=-0.1000 可观测非空。

### 3.4 域内金额自洽约束

seed 设计保持两组金额自洽（启动加载不校验，但 GraphQL 抽样/数值断言可观测）：
- `cost_variance.standardAmount` ↔ `work_order.materialCost`（同工单材料成本标准，cost_variance.standardAmount = work_order.materialCost）
- `cost_variance` 三金额链：`varianceAmount = actualAmount − standardAmount`、`variancePercent = varianceAmount / standardAmount`（MATERIAL_USAGE：standardPrice = actualPrice，差异纯由用量差驱动，符合 ORM 注释「材料段仅算用量差异」）

### 3.5 替代方案分析（汇总）

- (a) seed GL 凭证/业财一体：**rejected**——看板/报表读域表非 GL，种子科目表无制造域专用科目，徒增参照复杂度（镜像 2210-1 裁决）。
- (b) seed crp_load + workcenter 配置链：**rejected**——配置链膨胀超出域表直 seed 范式（见 §3.1）。
- (c) seed 全部制造域配置/执行链（BOM/Routing/Workcenter/MRP/JobCard/...）：**rejected**——这些表不被范围内看板/报表 `QueryBean` 直接读，work_order.bomId/routingId 非强制可留 null（见 Deferred 段）。
- (d) 仅 seed work_order 不 seed cost_variance/forecast：**rejected**——production-variance/forecast-variance 两报表仍空，未达成核心 Goal「2 报表非空」。

### 3.6 残留风险与防护

- 参照完整性遗漏（FK 列引用未 seed 的上游 ID）→ 启动期 DataInitInitializer 抛 NopException（不静默跳过），Phase 3 fresh-DB 启动验证兜底暴露。
- 列名错配（尤其 `UO_M_ID`）→ 同上启动期暴露。
- 非幂等（1234-1/1445-1/2210-1 已确认）→ fresh-DB 重置（删 `db/erp.mv.db`）是必需前置，playwright webServer 已内置。
- 字典码值错配（如 docStatus/varianceType/status 非法值）→ 同上启动期暴露（dict 校验在 entity 校验阶段）。

## 4. 条件性 SQL 裁决

Phase 2 条件性 SQL Add 项：**移出范围**。所有制造域种子经 CSV INSERT 表达，无序列重置 / 批量 UPDATE 需求。故不补 `NN-init-manufacturing-*.sql`。

## 5. seed 行数汇总

| 表 | 行数 |
|----|------|
| erp_mfg_work_order（IN_PROCESS/STOCK_PARTIAL/COMPLETED×2） | 4 |
| erp_mfg_cost_variance（MATERIAL_USAGE，workOrderId→WO-1） | 1 |
| erp_mfg_forecast（APPROVED） | 1 |
| erp_mfg_forecast_line（materialId=1 对齐 work_order.productId=1） | 1 |
| **合计** | **4 张制造域表 CSV，7 行** |

> 4 张新制造域表 CSV 加入 `_vfs/_init-data/`，与 1234-1 的 21 张主数据 + 1445-1 的 23 张 P2P/O2C 交易 + 2210-1 的 13 张运营域 CSV 共存（总计 57 + 4 = 61 张 CSV）。
