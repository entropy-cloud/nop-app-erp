# 种子数据模块

> **资产类型**：本文描述的是**部署资产**（系统初始化/基础配置数据，随部署一次性导入），**非测试资产**。测试共享夹具见 `app-erp-test-data` 模块与 `testing-strategy.md §四类测试资产边界`。两者不可混淆。
>
> **当前状态**：部署期种子数据**已落地**（2026-07-08，plan `2026-07-08-1234-1`）——经平台 `DataInitInitializer` + `_vfs/_init-data/*.csv`（21 张核心主数据表），config-gated 由 `-Dnop.orm.init-database-data=true` + fresh-DB 重置触发（E2E/演示），生产 `application.yaml` 默认关闭。机制/列映射/门控见 `docs/analysis/2026-07-08-1234-1-seed-data-table-column-map.md`。下方描述的独立 `app-erp-seed` 模块（版本化/增量导入/按租户账套）仍为**独立 follow-up**（未实现），用于更结构化的种子管理场景。
>
> **交易单据种子（P2P+O2C）已落地**（2026-07-08，plan `2026-07-08-1445-1`）——在 21 张主数据 CSV 之上新增 **23 张交易单据 CSV**（共 44 张），覆盖采购到付款（PO→Receive→Invoice→Payment）+ 销售到收款（SO→Delivery→Invoice→Receipt）各 1 条端到端最小连通链，含对应**已过账财务产物**（凭证/凭证行/业财回链/AR-AP 辅助账/GL 余额/会计期间 OPEN）。列映射/拓扑序/范围裁决见 `docs/analysis/2026-07-08-1445-1-transaction-seed-table-map.md`。
>
> **运营域交易单据种子（库存/资产/项目）已落地**（2026-07-08，plan `2026-07-08-2210-1`）——在 44 张 CSV 之上新增 **13 张运营域表 CSV**（共 57 张），覆盖库存（stock_move+line/stock_balance/cost_layer）+ 资产（asset_category/asset/depreciation_schedule）+ 项目（project_type/project/cost_collection/timesheet/budget/project_pnl）三域最小连通集。列映射/拓扑序/范围裁决见 `docs/analysis/2026-07-08-2210-1-operational-domain-seed-table-map.md`。
>
> **制造域交易单据种子已落地**（2026-07-09，plan `2026-07-09-0930-1`）——在 57 张 CSV 之上新增 **4 张制造域表 CSV**（共 61 张）：work_order（4 行覆盖 IN_PROCESS/STOCK_PARTIAL/COMPLETED 三态）+ cost_variance（1 行）+ forecast（1 行 APPROVED）+ forecast_line（1 行）。使制造域看板 4 `@BizQuery`（getDashboardKpi/getWorkOrderStatusDistribution/getDashboardTrend/findDelayedWorkOrderAlert）+ 2 报表（production-variance/forecast-variance）数值转非空可观测。crp_load + crp-load 报表因 mandatory workcenterId FK + workcenter/calendar/capacity 配置链依赖归 Deferred。列映射/拓扑序/范围裁决见 `docs/analysis/2026-07-09-0930-1-manufacturing-seed-table-map.md`。
>
> **维护+质量域交易单据种子已落地**（2026-07-09，plan `2026-07-09-0930-2`）——在 61 张 CSV 之上新增 **11 张维护+质量域表 CSV**（共 72 张）：维护域 8 表（equipment_category/equipment/schedule/request/downtime_entry/visit/visit_task/spare_part_usage）+ 质量域 3 表（inspection/non_conformance/action）。使维护域看板 `getDashboardKpi`（equipmentTotal/runningCount/openRequestCount/periodVisitCount）+ 3 预警（findEquipmentDowntimeAlert/findMaintenanceOverdueAlert + 质量域 findCapaOverdueAlert）+ 2 报表（maintenance-history/downtime-summary）+ 质量域看板 `getDashboardKpi`（inspectionCount/passRate/rejectedCount/openNcrCount）+ 2 报表（inspection-summary/ncr-capa-summary）数值转非空可观测。SPC 三表因 spc_chart.parameterId 配置链依赖归 Deferred。列映射/拓扑序/范围裁决见 `docs/analysis/2026-07-09-0930-2-maintenance-quality-seed-table-map.md`。
>
> **CRM/客服/人力域交易单据种子已落地**（2026-07-09，plan `2026-07-09-1045-1`）——在 72 张 CSV 之上新增 **12 张 CRM/CS/HR 域表 CSV**（共 84 张）+ **2 处既有 CSV 加性追加**（erp_md_partner +1 行 EMPLOYEE 类型 / erp_fin_ar_ap_item +2 行 EMPLOYEE_ADVANCE/EXPENSE_CLAIM·OPEN）：CRM 5 表（stage/lead/forecast_period/forecast/forecast_line）+ CS 3 表（ticket_type/ticket/survey）+ HR 4 表（department/employee/salary_simulation/salary_simulation_item_adj）。使三域 **5 张报表**（CRM lead-conversion-funnel/forecast-accuracy、CS ticket-sla-csat-summary、HR payroll-simulation-comparison/employee-net-balance）数值转非空可观测。HR employee-net-balance 经跨域 finance/master-data 扩展（追加员工型 partner + ar_ap_item OPEN 行）驱动。三域为纯报表域（无看板 BizModel）。列映射/拓扑序/范围裁决见 `docs/analysis/2026-07-09-1045-1-crm-cs-hr-seed-table-map.md`。

## 目的

定义 nop-app-erp 的种子数据（基础配置数据）管理机制，使用独立模块 `app-erp-seed` 管理。

## 模块职责

- 管理系统初始化所需的基础数据
- 支持按租户/账套导入种子数据
- 种子数据版本管理

## 种子数据范围

| 数据类型 | 示例 |
|----------|------|
| 字典数据 | 状态枚举、作业类型、审批模式 |
| 主数据模板 | 科目表模板、仓库模板 |
| 系统配置 | 过账模式、审批流配置 |
| 示例数据 | 演示用物料/客户/供应商 |

## 模块结构

```
app-erp-seed/
    └── src/main/resources/
        ├── seed-data/
        │   ├── dict/          # 字典数据
        │   ├── master-data/   # 主数据模板
        │   └── config/        # 系统配置
        └── import.sql         # 初始化导入脚本
```

## 导入策略

- 首次部署时自动导入
- 升级时增量导入（新增数据）
- 不覆盖用户已修改的数据

## 交易单据种子（P2P+O2C，已落地）

### 核心范式：源单据 + 下游财务产物「直 seed」

业财过账（凭证生成）是 **action 驱动**（BizModel 的 `@BizMutation` 动作触发），**原始 CSV 插入源单据不会自动产生下游凭证/辅助账/核销**。因此要 seed 一个**连贯的已过账端到端态**，必须**同时直 seed**：

1. 源单据头/行（PO/Receive/Invoice/Payment、SO/Delivery/Invoice/Receipt 各头+行）
2. 下游财务产物：`erp_fin_voucher` + `erp_fin_voucher_line`（借贷平衡）+ `erp_fin_voucher_bill_r`（凭证-单据反查）+ `erp_fin_ar_ap_item`（AR/AP 辅助账）+ `erp_fin_gl_balance`（期间科目余额）
3. 期间状态：`erp_fin_accounting_period` + `erp_fin_accounting_period_status`（当前期间 OPEN）

全部以一致 FK 串联，并引用 1234-1 已 seed 的主数据固定 ID（org/acctSchema/currency/partner/material/subject/...）。

### 加载拓扑序（跨域）

```
accounting_period → accounting_period_status
  → pur_order → pur_order_line → pur_receive → pur_receive_line
    → pur_invoice → pur_invoice_line → pur_payment → pur_payment_line
  → sal_order → sal_order_line → sal_delivery → sal_delivery_line
    → sal_invoice → sal_invoice_line → sal_receipt → sal_receipt_line
  → fin_voucher → fin_voucher_line → fin_voucher_bill_r
    → fin_ar_ap_item → fin_gl_balance
```

> `DataInitInitializer` 按 ORM `getEntityModelsInTopoOrder()` 自动排序，确保 FK 上游先于下游。

### posted 一致性裁决

`posted=true` **当且仅当**该源单据有对应凭证（经 `voucher_bill_r` 串联）：
- PO/SO（订单不直接过账 GL）、采购入库/销售出库（其过账产物是库存移动，属 inventory 域，未 seed 库存表）→ `posted=false`
- 采购发票/付款/销售发票/收款 → `posted=true`

### 已知简化

1234-1 seed 的科目表未含进/销项税科目，故凭证将税额并入相邻科目（AP 发票税额并入存货借方；AR 发票税额并入收入贷方），保证「凭证合计 = 发票价税合计」金额自洽且借贷平衡。精确税金科目分拆是主数据扩展 successor。

### Non-Goals（归后续批次）

- 扩展域交易单据（manufacturing/HR/quality/maintenance/CRM/CS/logistics/b2b/contract/drp/aps）——按域逐批补充（1234-1/1445-1 Deferred 既定策略）。**inventory/assets/projects 已于 2210-1 落地**；**manufacturing 已于 2026-07-09-0930-1 落地**；**maintenance/quality 已于 2026-07-09-0930-2 落地**；**CRM/CS/HR 已于 2026-07-09-1045-1 落地**（见下方「CRM/客服/人力域交易单据种子」段）。
- 运营域 GL 凭证/业财一体 seed（库存估值凭证/资产取得+折旧凭证/项目成本凭证）——三域看板读域表非 GL，且种子科目表无运营域专用科目；触发条件：运营域业财一体端到端数值回归需 GL 串联时。
- 退货链（采购/销售退货 + 红字凭证 + 反向辅助账）
- 核销单文档 `erp_fin_reconciliation`(+line)——本批 ar_ap_item 直表达 SETTLED 态，核销单文档归后续
- 精确 KPI/报表数值断言——归 `2026-07-08-1445-2` 数据驱动 successor（运营域数值断言由 `2026-07-08-2210-2` 承接）

## 运营域交易单据种子（库存/资产/项目，已落地）

### 核心范式：域表「直 seed」（区别于 P2P/O2C「源单据 + 下游财务产物直 seed」）

库存/资产/项目三域看板**读域表而非 GL 凭证**（经 `ErpInvDashboardBizModel`/`ErpAstDashboardBizModel`/`ErpPrjDashboardBizModel.getDashboardKpi` 核实）：

- **库存看板**：库存总值 = Σ `ErpInvStockBalance.totalCost`；本期出入库量 = Σ `ErpInvStockMove`（DONE 期内）关联 `ErpInvStockMoveLine`。
- **资产看板**：资产原值 = Σ `ErpAstAsset.originalValue`（IN_SERVICE）；累计折旧 = Σ `accumulatedDepreciation`；本期折旧 = Σ `ErpAstDepreciationSchedule.actualAmount`（EXECUTED 期内）。
- **项目看板**：在手项目数 = count `ErpPrjProject`（OPEN）；已发生成本 = Σ `ErpPrjCostCollection.totalAmount`；项目毛利率 = `ErpPrjProjectPnl` Σ grossProfit / Σ revenueAmount。

故 seed 域表（stock_balance/asset/depreciation_schedule/project/cost_collection/project_pnl）即令三域看板 KPI **非空**，**无需 seed GL 凭证**。这是运营域 seed 相对 P2P+O2C 的**复杂度减负**。

### 加载拓扑序（跨域）

```
[1234-1 主数据] → [上游域配置] ast_asset_category / prj_project_type
  → [域头] ast_asset / prj_project
    → [域行/计算产物]
      inv_stock_move → inv_stock_move_line
      inv_stock_balance / inv_cost_layer        （引用 material/warehouse，独立于 move）
      ast_depreciation_schedule                  （引用 asset）
      prj_cost_collection / prj_timesheet /
      prj_budget / prj_project_pnl               （引用 project）
```

### posted 一致性裁决（统一 posted=false）

本批所有运营域源单据/计算产物统一 `posted=false`。依据：
1. 三域看板读**域表**非 GL，`posted` 标志不被看板消费；
2. 1234-1 seed 的科目表无库存估值/资产/折旧费用/项目成本专用科目，seed GL 凭证徒增参照复杂度且不解除额外阻塞；
3. 运营域过账 → GL 凭证 seed 归后续（Deferred）。

### 域内金额自洽约束

seed 设计保持三组计算产物金额自洽（启动加载不校验，但 GraphQL 抽样/数值断言可观测）：
- `stock_balance.totalCost` ↔ `cost_layer.totalCost`（同物料+仓库对）
- `asset.accumulatedDepreciation`/`netBookValue` ↔ 最新 `depreciation_schedule` 同名字段
- `project_pnl.totalCost` ↔ Σ `cost_collection.totalAmount`（同项目）

### Non-Goals（归后续批次）

- 运营域 GL 凭证/业财一体 seed（库存估值凭证/资产取得+折旧凭证/项目成本凭证）——三域看板读域表非 GL；触发条件：运营域业财一体端到端数值回归需 GL 串联时。
- 其他扩展域交易种子（manufacturing/quality/maintenance/CRM/CS/HR/logistics/b2b/contract/drp/aps）——按域逐批补充（1445-1 Deferred 既定策略）。**manufacturing 已于 2026-07-09-0930-1 落地（见下方「制造域交易单据种子」段）**；**maintenance/quality 已于 2026-07-09-0930-2 落地（见下方「维护+质量域交易单据种子」段）**。
- 精确运营域 KPI/报表数值断言——本计划解除「运营域交易数据存在」阻塞（数值非零可观测）；精确断言由 `2026-07-08-2210-2` 承接。

## 制造域交易单据种子（已落地）

### 核心范式：域表「直 seed」（镜像运营域范式）

制造域看板/报表**读域表而非 GL 凭证**（经 `ErpMfgDashboardBizModel`/`ErpMfgReportBizModel` 核实）：

- **制造看板**（`ErpMfgDashboardBizModel`，4 `@BizQuery` 均查 `ErpMfgWorkOrder`）：在制工单数 = count(docStatus IN [IN_PROCESS, STOCK_RESERVED])；本期完工量 = Σ completedQuantity（COMPLETED 期内 actualEndDate）；齐套待产 = count(STOCK_PARTIAL)；工单准时率 = count(COMPLETED 且 actualEndDate ≤ plannedEndDate) / count(COMPLETED)。
- **生产差异报表**（`buildProductionVarianceDataset`）：读 `ErpMfgCostVariance`（workOrderId FK→work_order）。
- **预测差异报表**（`buildForecastVarianceDataset`）：读 `ErpMfgForecast`(APPROVED) + `ErpMfgForecastLine` + `ErpMfgWorkOrder`(COMPLETED 实际量，按 productId 聚合、区间重叠用 plannedStartDate/plannedEndDate)。

故 seed 4 表（work_order/cost_variance/forecast/forecast_line）即令看板 + 2 报表 KPI **非空**，**无需 seed GL 凭证**（与运营域范式一致，复杂度减负）。

### 加载拓扑序（跨域）

```
[1234-1 主数据] → [域头] erp_mfg_work_order / erp_mfg_forecast
                    → [域行/计算产物] erp_mfg_cost_variance（workOrderId→work_order）
                                    / erp_mfg_forecast_line（forecastId→forecast）
```

### posted 一致性裁决（统一 posted=false）

本批所有制造域源单据/计算产物统一 `posted=false`。依据（镜像运营域裁决）：
1. 看板/报表读**域表**非 GL，`posted` 标志不被消费；
2. 1234-1 seed 的科目表无制造费用/差异/在产品专用科目，seed GL 凭证徒增参照复杂度；
3. 制造域过账 → GL 凭证 seed 归后续（Deferred）。

### crp_load 移出范围（Deferred）

`erp_mfg_crp_load.workcenterId` 是 mandatory FK→ErpMfgWorkcenter，且 crp-load 报表经 `CrpLoadCalculator` 依赖 workcenter/workcenter_calendar/workcenter_capacity 配置链（均未 seed）算 capacityHours/loadRate。seed crp_load 需先 seed 整条配置链，超出「域表直 seed」范式。触发条件：workcenter 配置链 seed 落地后，由独立 successor 承接 crp_load + crp-load 报表。

### 域内金额自洽约束

- `cost_variance.standardAmount` ↔ `work_order.materialCost`（同工单材料成本标准）
- `cost_variance`：`varianceAmount = actualAmount − standardAmount`、`variancePercent = varianceAmount / standardAmount`（MATERIAL_USAGE：standardPrice = actualPrice，差异纯由用量差驱动）
- `forecast_line.materialId` 对齐 `work_order.productId`（forecast-vs-actual 对比有意义）

### Non-Goals（归后续批次）

- 制造域 GL 凭证/业财一体 seed（制造费用/差异过账凭证）——看板/报表读域表非 GL，种子科目表无制造域专用科目；触发条件：制造域业财一体端到端数值回归需 GL 串联时。
- crp_load + crp-load 报表 seed——mandatory workcenterId FK + workcenter/calendar/capacity 配置链依赖；触发条件：workcenter 配置链 seed 落地后。
- 制造域配置/执行链 seed（BOM/Routing/Workcenter/MRP/JobCard/MaterialIssue/Subcontract/CostRollup/BatchGenealogy/work_order_line）——这些表不被看板/报表 `QueryBean` 直接读，work_order.bomId/routingId 非强制可留 null；触发条件：制造域配置/执行链端到端回归需这些数据时。
- 精确制造域 KPI/报表数值断言——本计划解除「制造域交易数据存在」阻塞（数值非零可观测）；精确断言由 `2026-07-09-0930-3` 承接。
- 其他扩展域交易种子（maintenance/quality 同批 N=2；CRM/CS/HR/logistics/b2b/contract/drp/aps 后续批次）——1445-1 Deferred 既定策略。**maintenance/quality 已于 2026-07-09-0930-2 落地（见下方「维护+质量域交易单据种子」段）**；**CRM/CS/HR 已于 2026-07-09-1045-1 落地（见下方「CRM/客服/人力域交易单据种子」段）**。

## 维护+质量域交易单据种子（已落地）

### 核心范式：域表「直 seed」（镜像运营域/制造域范式）

维护/质量域看板/报表**读域表而非 GL 凭证**（经 `ErpMntDashboardBizModel`/`ErpQaDashboardBizModel`/`ErpMntReportBizModel`/`ErpQaReportBizModel` 核实，零 GL/Voucher 引用）：

- **维护看板**（`ErpMntDashboardBizModel`）：设备总数 = count `ErpMntEquipment`（status≠DECOMMISSIONED）；运行中 = count(RUNNING)；待处理请求 = count `ErpMntRequest`(OPEN)；本期维护访问 = count `ErpMntVisit`(COMPLETED + businessDate 区间)。
- **维护预警**：`findEquipmentDowntimeAlert`（equipment DOWN + `ErpMntDowntimeEntry` endTime=null）；`findMaintenanceOverdueAlert`（`ErpMntSchedule` isActive=1 + nextDueDate<today + 无 visit 关联）。
- **维护报表**：maintenance-history 读 `ErpMntVisit`（visitDate 区间 + taskCount via visit_task + sparePartUsageCount via spare_part_usage）；downtime-summary 读 `ErpMntDowntimeEntry`（startTime 区间，按设备/原因聚合 totalMinutes）。
- **质量看板**（`ErpQaDashboardBizModel`）：本期质检数 = count `ErpQaInspection`（inspectionDate 区间）；合格率 = ACCEPTED/total；不合格数 = count(REJECTED)；开放 NCR = count `ErpQaNonConformance`(status IN [OPEN,IN_REVIEW])。
- **质量预警/报表**：`findCapaOverdueAlert`（`ErpQaAction` status≠COMPLETED + dueDate<today）；inspection-summary 读 inspection（按 materialId 聚合）；ncr-capa-summary 读 non_conformance（ncrDate 区间 + action 计数 by ncrId）。

故 seed 域表（equipment/visit/inspection/non_conformance 等）即令两域看板 KPI + 4 报表 + 3 预警**非空**，**无需 seed GL 凭证**（与运营域/制造域范式一致，复杂度减负）。

### 加载拓扑序（跨域）

```
[1234-1/2210-1 主数据] md_organization/md_material/md_uom/md_warehouse/md_employee/md_partner
  /ast_asset(AST-2026-002，mnt equipment.assetId 跨域可选复用)
  → [维护域配置] mnt_equipment_category
    → [维护域头] mnt_equipment
      → [维护域单据/记录]
        mnt_schedule / mnt_request / mnt_downtime_entry / mnt_visit
          → mnt_visit_task / mnt_spare_part_usage
  → [质量域单据] qa_inspection → qa_non_conformance → qa_action
```

### posted 一致性裁决（统一 posted=false）

本批所有维护/质量域源单据统一 `posted=false`。依据（镜像运营域/制造域裁决）：
1. 两域看板/报表读**域表**非 GL，`posted` 标志不被消费；
2. 1234-1 seed 的科目表无维护费用/备件消耗/质量损失/报废处置专用科目，seed GL 凭证徒增参照复杂度；
3. 两域过账 → GL 凭证 seed 归后续（Deferred）。

### SPC 三表移出范围（Deferred）

`getSpcOutOfControlWarning` 读 `ErpQaSpcSample`（isOutOfControl）+ `ErpQaSpcCapability`（capabilityLevel=INADEQUATE），二者 chartId mandatory FK→`ErpQaSpcChart`，而 spc_chart.parameterId 是 mandatory FK 但 quality ORM 无独立 ErpQaParameter 实体（检验参数仅以 inspection_template_line.parameterName 自由文本存在），seed spc_chart 缺 FK 上游——与制造域 crp_load 因 workcenter 配置链依赖归 Deferred 同构。不 seed 则该预警返回 outOfControlChartCount=0 属预期非缺陷（核心 KPI inspection/passRate/openNcrCount 仍非空）。触发条件：SPC 配置链（检验参数实体/template_line 物化为 parameterId）seed 落地后承接。

### 域内金额/计数自洽约束

- maintenance-history 报表：visit 的 taskCount（visit_task.visitId 计数）+ sparePartUsageCount（spare_part_usage.visitId 计数）自洽。
- downtime-summary 报表：downtime_entry 已恢复行（endTime≠null）的 totalMinutes 聚合自洽（ongoing 行 endTime=null + totalMinutes=null 不计入聚合）。
- ncr-capa 报表：ncr 的 capaActionCount（action.ncrId 计数）+ completedActionCount（action.status=COMPLETED 计数）自洽。
- inspection-summary 报表：inspection 按 materialId 聚合 totalInspections/acceptedCount(ACCEPTED+CONDITIONAL)/rejectedCount(REJECTED) 自洽。

### Non-Goals（归后续批次）

- 维护/质量域 GL 凭证/业财一体 seed（维护费用/备件消耗/质量损失/报废处置过账凭证）——看板/报表读域表非 GL，种子科目表无两域专用科目；触发条件：维护/质量域业财一体端到端数值回归需 GL 串联时。
- 维护域 calibration / 质量域 risk_register/quality_goal/review/calibration/recall(+target)/sampling_plan/inspection_template(+line) seed——这些表不被看板/报表 `QueryBean` 直接读，inspection.templateId 非强制可留 null；触发条件：对应域配置/执行链端到端回归需这些数据时。
- 质量域 SPC 三表 seed（spc_chart/spc_sample/spc_capability）——spc_chart.parameterId 配置链依赖；触发条件：SPC 配置链 seed 落地后（见上方「SPC 三表移出范围」）。
- 备件消耗行 `erp_mnt_spare_part_usage_line` seed——看板/报表仅按 spare_part_usage 头计数，不读行；触发条件：备件消耗明细端到端回归需行数据时（注意 UoM 列名 `UO_M_ID` 陷阱）。
- 精确维护/质量域 KPI/报表数值断言——本计划解除「维护/质量域交易数据存在」阻塞（数值非零可观测）；精确断言由 `2026-07-09-0930-3` 承接。
- 其他扩展域交易种子（CRM/CS/HR 已于 2026-07-09-1045-1 落地，见下方「CRM/客服/人力域交易单据种子」段；logistics/b2b/contract/drp/aps 后续批次）——1445-1 Deferred 既定策略。

## CRM/客服/人力域交易单据种子（已落地）

### 核心范式：域表「直 seed」+ 跨域加性追加（镜像运营/制造/维护+质量域范式）

CRM/CS/HR 三域为**纯报表域（无看板 BizModel）**，各 1 个 `ErpXxxReportBizModel` 共 5 张报表，**读域表而非 GL 凭证**（逐方法 `findAll`/`findAllByQuery` 核实）：

- **CRM 线索转化漏斗**（`buildLeadConversionFunnelDataset`）：读 `ErpCrmLead`（按 stageId 非 null 聚合 leadCount/expectedRevenue）+ `ErpCrmStage`（解析 stageName）。
- **CRM 销售预测准确率**（`buildForecastAccuracyDataset`）：读 `ErpCrmForecast`（periodId mandatory FK→forecast_period）+ `ErpCrmForecastLine`（forecastId+leadId mandatory FK，按 forecastId 聚合 lineCount/lineWeightedRevenue）。
- **CS 工单 SLA/CSAT**（`buildTicketSlaCsatSummaryDataset`）：读 `ErpCsTicket`（ticketTypeId mandatory FK、isSlaCompleted 布尔列内存派生）+ `ErpCsSurvey`（ticketId mandatory FK，csatScore/npsScore 经 `orm_propValueByName` 读取）+ `ErpCsTicketType`（解析 ticketTypeName）。
- **HR 薪酬模拟对比**（`buildPayrollSimulationComparisonDataset`）：simulationId 为强制入参；读 `ErpHrSalarySimulationItemAdjustment`（simulationId+employeeId mandatory FK，employee.departmentId 驱动 DEPT_SUBTOTAL 小计行）+ `ErpHrEmployee`。
- **HR 员工净余额**（`buildEmployeeNetBalanceDataset`）：**唯一跨域读取**——经注入 biz `IErpFinArApItemBiz.findOpenItems(direction)`（过滤 direction + status IN [OPEN,PARTIAL]）读 finance `erp_fin_ar_ap_item`，再内存按 sourceBillType 二次过滤（预支余额=RECEIVABLE+EMPLOYEE_ADVANCE、报销余额=PAYABLE+EXPENSE_CLAIM），按 partnerId 汇总 openAmountFunctional；再 `findAllByQuery` 读 `erp_md_partner` 解析姓名。

故 seed 域表 + HR 跨域 finance/master-data 加性追加即令 5 报表 KPI **非空**，**无需 seed GL 凭证**。

### 加载拓扑序（跨域）

```
[1234-1 主数据(已 seed)] md_organization(2) / md_currency(1) / md_partner(1-4)
  → [本批跨域追加] md_partner +1 行(id=5 EMPLOYEE 类型)              ← 早于 fin_ar_ap_item 追加行
[CRM 域] stage → lead(stageId) ; forecast_period → forecast(periodId) → forecast_line(forecastId+leadId)
[CS 域]  ticket_type → ticket(ticketTypeId+customerId) → survey(ticketId)
[HR 域]  department → employee(departmentId) ; salary_simulation → simulation_item_adj(simulationId+employeeId)
[HR 跨域 finance 扩展] erp_fin_ar_ap_item +2 行(partnerId=5；引用 1234-1 已 seed org=2/acctSchema=1/currency=1/period=1)
```

> `md_partner` 追加行（id=5）属 1234-1 主数据批，先于 finance `fin_ar_ap_item`（1445-1 批）加载，FK 天然满足。

### posted 一致性裁决（统一无 posted 列）

本批 CRM/CS/HR 新增域表 + 跨域追加表（fin_ar_ap_item / md_partner）实体**本身均无 `posted` 列**（逐表 ORM 核实），CSV 不含 posted。依据（镜像前序批次裁决）：
1. 三域 5 报表读域表/状态列非 posted（lead.stageId / forecast·forecast_line / ticket.isSlaCompleted / ar_ap_item.status·sourceBillType·direction / simulation_item_adj），`posted` 非任何报表过滤列；
2. 1234-1 seed 的科目表无 CRM/CS/HR 域专用科目，seed GL 凭证徒增参照复杂度；
3. 三域过账 → GL 凭证 seed 归后续（Deferred）。

### HR 跨域 finance/master-data 扩展裁决（方案 A）

`buildEmployeeNetBalanceDataset` 需 `erp_fin_ar_ap_item` 含 EMPLOYEE_ADVANCE(RECEIVABLE)/EXPENSE_CLAIM(PAYABLE)+status=OPEN 行。选择**加性追加**（方案 A）：`erp_md_partner` 追加 1 行 PARTNER_TYPE=EMPLOYEE（对齐 `docs/design/finance/expense-claim.md` 员工-as-partner 设计）+ `erp_fin_ar_ap_item` 追加 2 行 OPEN（partnerId 指向员工型 partner）。

**对既有 finance 报表/看板无回归核证**：(1) finance 看板 `getDashboardKpi` 的 revenue/netProfit/expense 读 GL，arBalance/apBalance 虽读 ar_ap_item open 但非 `finance.value.spec.ts` 断言字段；(2) `fin-ar-ap-aging.value.spec.ts` 仅断言报表标题 + 合计行标签 token 存在，不断言具体数值；(3) `findOpenItems` 仅取 status IN [OPEN,PARTIAL]，既有 4 行全 SETTLED 不被取。E2E 全套 74 spec 0 回归实证。

### 域内金额/计数自洽约束

- CRM funnel：lead 按 stageId 聚合 leadCount + ΣexpectedRevenue（每 stage ≥1 行）。
- CRM forecast-accuracy：forecast_line.weightedRevenue = expectedRevenue × probability/100（行自洽）；forecast.commitAmount/lineCount/lineWeightedRevenue 跨表可观测。
- CS ticket-sla-csat：ticket 按 ticketTypeId 聚合 totalTickets/slaCompleted(isSlaCompleted=true)/slaBreached(false)；survey 按 ticketId 摊回 ticketType 桶驱动 avgCsat/avgNps。
- HR payroll-sim：difference = adjustedAmount − originalAmount；DEPT_SUBTOTAL = Σ difference by departmentId。
- HR employee-net-balance：OPEN 态 openAmountFunctional = amount（全额未核销）；netBalance = advanceBalance(ΣRECEIVABLE·EMPLOYEE_ADVANCE) − expenseBalance(ΣPAYABLE·EXPENSE_CLAIM)。

### Non-Goals（归后续批次）

- CRM/CS/HR 域 GL 凭证/业财一体 seed（凭证↔源单据↔辅助账串联）——三域报表读域表/ar_ap_item 状态列非 GL；HR ar_ap_item 追加行作为可观测独立行（无凭证回链）；触发条件：三域业财一体端到端数值回归需 GL 串联时。
- CRM/CS/HR 域配置/执行链 seed（CRM product_config_rule/price_rule/bundle_pricing/territory/team/campaign；CS knowledge_base/sla_policy/entitlement/catalog；HR salary/salary_item/leave/attendance/shift/competency/social_insurance）——这些表不被范围内 5 报表 `QueryBean` 直接读（lead/ticket/simulation 的配置 FK 非强制可留 null）；触发条件：对应域配置/执行链端到端回归需这些数据时。
- 精确 CRM/CS/HR 域报表数值断言——本计划解除「数据存在」阻塞（报表非空可观测）；精确断言由 `2026-07-09-1045-2-crm-cs-hr-report-value-assertions.md` 承接。
- 其他扩展域交易种子（logistics/b2b/contract/drp/aps 后续批次）——无看板无报表（seed 不解除额外阻塞）；触发条件：对应域端到端数值回归需交易数据时。
