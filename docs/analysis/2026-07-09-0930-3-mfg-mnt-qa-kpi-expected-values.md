# 制造/维护/质量域看板/报表确定性数值期望值表（0930-3 数据驱动 E2E 断言）

> Owner: `docs/plans/2026-07-09-0930-3-manufacturing-maintenance-quality-value-assertions.md` Phase 1 Exit Criteria
> 数据基线: `app-erp-all/src/main/resources/_vfs/_init-data/erp_{mfg,mnt,qa}_*.csv`（15 CSV / 25 行，0930-1 制造域 4 表 7 行 + 0930-2 维护+质量域 11 表 18 行；叠加 1234-1 主数据 + 1445-1 P2P/O2C + 2210-1 运营域共 72 CSV）
> 聚合口径源: `ErpMfgDashboardBizModel` / `ErpMntDashboardBizModel` / `ErpQaDashboardBizModel` / `Erp{Mfg,Mnt,Qa}ReportBizModel` + `dashboards.md` §7/§8/§9
> 范式承接: `docs/analysis/2026-07-08-2210-2-operational-kpi-expected-values.md`（看板 `assertDashboardKpiValues` + 报表 `assertReportRenderedWithValue`）

## 0. 确定性前提

- Playwright webServer 每次启动前 `rm -f db/erp.mv.db db/erp.trace.db`（fresh-DB 重置）+ `-Dnop.orm.init-database-data=true`，72 CSV 按拓扑序确定性插入。
- 三域种子集固定（0930-1/0930-2 固化），且三域看板/报表**读域表（work_order/visit/inspection/...）非 GL 凭证**——故 posted 统一 false 不影响聚合结果，后端聚合结果是确定性的，spec 可硬编码期望值。
- **日期漂移防护**（镜像 2210-2 范式）：
  - **制造看板**「本期完工量」依赖 actualEndDate 区间（默认 `today.withDayOfMonth(1)`→`today`）。种子 COMPLETED 工单 actualEndDate 跨 2026-06（WO-1=06-28）/2026-07（WO-2=07-08）。spec **显式传 `startDate=2026-06-01` / `endDate=2026-07-31`** 覆盖种子两月区间，锁定 periodCompletedQty=100+80=180。`onTimeRate`（COMPLETED 全量）/ `inProcessCount`/`stockPartialCount`（status 聚合，不依赖日期）随之确定性。
  - **维护看板**「本期维护访问数」依赖 businessDate 区间。种子 visit 1 businessDate=2026-07-03（本期）/ visit 2=2026-06-20（历史月）。spec **显式传 `startDate=2026-07-01` / `endDate=2026-07-31`** 锁定本期，periodVisitCount=1（仅 visit 1）。`equipmentTotal`/`runningCount`/`openRequestCount` 不依赖日期。
  - **质量看板**「本期质检数」依赖 inspectionDate 区间。种子 3 条检验 inspectionDate 全在 2026-07（07-02/04/06）。spec **显式传 `startDate=2026-07-01` / `endDate=2026-07-31`** 锁定本期，inspectionCount=3。`openNcrCount` 不依赖日期。
- **SPC 预警 `getSpcOutOfControlWarning` 裁决**（Phase 1 Decision）：config-gated 默认开（`erp-dash.qa-spc-include-inadequate`/`erp-dash.qa-spc-include-ncr` 默认 true），但 N=2 未 seed SPC 三表（`spc_chart.parameterId` 配置链依赖，0930-2 Deferred）。故该方法三段计数确定性为 0（`outOfControlChartCount`=0 / `inadequateCapabilityCount`=0 / `openSpcNcrCount`=0），`includeInadequate`/`includeNcr`=true。spec **断言确定性 0（非跳过）**，以覆盖该 `@BizQuery` 的可观测性（镜像 2210-2 范式，约束记录）。
- **seed 漂移同步机制**：若 0930-1/0930-2 三域 seed CSV 变更（行增减/金额改动/业务日期改动），本表期望值须同步更新，对应 `*.value.spec.ts` 的 `expected`/`expectedTokens` 亦须同步（与 1445-2/2210-2 同机制）。

## 1. 种子数据关键行（期望值派生依据）

### 1.1 制造（`erp_mfg_*.csv`，0930-1）

| 表 | 行 | 关键列 | 值 |
|----|----|--------|-----|
| work_order | id=1 WO-2026-001 | productId=1 / completedQty=100 / docStatus=COMPLETED / plannedEnd=2026-06-30 / actualEnd=2026-06-28（≤planned→准时）/ materialCost=6000.00 | — |
| work_order | id=2 WO-2026-002 | productId=1 / completedQty=80 / docStatus=COMPLETED / plannedEnd=2026-07-05 / actualEnd=2026-07-08（>planned→延期）/ plannedStart=2026-06-20 | — |
| work_order | id=3 WO-2026-003 | productId=1 / docStatus=IN_PROCESS / plannedEnd=2026-06-30（<today）/ actualEnd=null | — |
| work_order | id=4 WO-2026-004 | productId=1 / docStatus=STOCK_PARTIAL / plannedEnd=2026-06-30（<today）/ actualEnd=null | — |
| cost_variance | id=1 | workOrderId=1 / varianceType=MATERIAL_USAGE / standardAmount=6000.00 / actualAmount=6300.00 / varianceAmount=300.00 / businessDate=2026-06-28 | — |
| forecast | id=1 FC-2026-001 | status=APPROVED / periodFrom=2026-06-01 / periodTo=2026-07-31 | — |
| forecast_line | id=1 | forecastId=1 / materialId=1 / UO_M_ID=1 / periodStart=2026-06-01 / periodEnd=2026-06-30 / forecastQty=200 | — |

> COMPLETED×2（WO-1 准时/WO-2 延期）+ IN_PROCESS×1 + STOCK_PARTIAL×1，覆盖看板 4 KPI。forecast-variance 实际量按 productId 聚合、区间重叠用 plannedStartDate/plannedEndDate（非 actualEndDate），WO-1/WO-2 productId=1 与 forecast_line.materialId=1 对齐。

### 1.2 维护（`erp_mnt_*.csv`，0930-2）

| 表 | 行 | 关键列 | 值 |
|----|----|--------|-----|
| equipment | id=1 EQ-2026-001 | status=RUNNING / name=数控机床 CNC-001 | — |
| equipment | id=2 EQ-2026-002 | status=RUNNING / name=注塑机 INJ-001 | — |
| equipment | id=3 EQ-2026-003 | status=DOWN（驱动 findEquipmentDowntimeAlert）/ name=输送带 CONV-001 | — |
| request | id=1 REQ-2026-001 | equipmentId=3 / status=OPEN | — |
| visit | id=1 VIS-2026-001 | equipmentId=1 / status=COMPLETED / visitDate=2026-07-03 / businessDate=2026-07-03 / totalMinutes=120.00 / result=NORMAL | — |
| visit | id=2 VIS-2026-002 | equipmentId=2 / status=COMPLETED / visitDate=2026-06-20 / businessDate=2026-06-20 / totalMinutes=90.00 / result=ABNORMAL | — |
| visit_task | id=1 | visitId=1 / status=COMPLETED（maintenance-history 报表 taskCount=1） | — |
| spare_part_usage | id=1 SPU-2026-001 | visitId=1 / totalAmount=170.00（maintenance-history 报表 sparePartUsageCount=1） | — |
| downtime_entry | id=1 | equipmentId=3 / startTime=2026-07-02 08:00:00 / endTime=null（ongoing）/ totalMinutes=null | — |
| downtime_entry | id=2 | equipmentId=1 / startTime=2026-07-05 10:00:00 / endTime=2026-07-05 14:00:00 / totalMinutes=240.00 | — |
| schedule | id=1 SCH-2026-001 | equipmentId=1 / isActive=1 / nextDueDate=2026-07-01（<today）/ 无 visit 关联 | — |

> equipment 3 行 RUNNING×2 + DOWN×1（无 DECOMMISSIONED）→ equipmentTotal=3/runningCount=2。visit 2 行 COMPLETED，本期（07 月）仅 visit 1。

### 1.3 质量（`erp_qa_*.csv`，0930-2）

| 表 | 行 | 关键列 | 值 |
|----|----|--------|-----|
| inspection | id=1 INS-2026-001 | materialId=3 / inspectionDate=2026-07-02 / result=ACCEPTED | — |
| inspection | id=2 INS-2026-002 | materialId=1 / inspectionDate=2026-07-04 / result=REJECTED | — |
| inspection | id=3 INS-2026-003 | materialId=1 / inspectionDate=2026-07-06 / result=ACCEPTED | — |
| non_conformance | id=1 NCR-2026-001 | materialId=1 / ncrDate=2026-07-04 / severity=HIGH / dispositionType=RETURN / status=OPEN | — |
| non_conformance | id=2 NCR-2026-002 | materialId=1 / ncrDate=2026-07-05 / severity=NORMAL / dispositionType=CONCESSION / status=IN_REVIEW | — |
| action | id=1 | ncrId=1 / actionType=CAPA / dueDate=2026-07-01（<today）/ status=IN_PROGRESS | — |

> 3 条检验 ACCEPTED×2 + REJECTED×1 → passRate=2/3。2 条 NCR OPEN + IN_REVIEW → openNcrCount=2。SPC 三表未 seed → getSpcOutOfControlWarning 全 0。

## 2. 看板 KPI 期望值（GraphQL `__getDashboardKpi`）

### 2.1 制造看板 `ErpMfgDashboard__getDashboardKpi(startDate=2026-06-01, endDate=2026-07-31)`

口径（`ErpMfgDashboardBizModel.getDashboardKpi`，`dashboards.md §7`）：inProcessCount = count(docStatus IN [IN_PROCESS, STOCK_RESERVED])；periodCompletedQty = Σ completedQuantity(docStatus=COMPLETED AND actualEndDate∈区间)；stockPartialCount = count(docStatus=STOCK_PARTIAL)；onTimeRate = count(COMPLETED 且 actualEndDate≤plannedEndDate) / count(COMPLETED)。

| KPI 字段 | 期望值 | 派生 |
|----------|--------|------|
| `inProcessCount` | **1** | WO-3 IN_PROCESS（无 STOCK_RESERVED） |
| `periodCompletedQty` | **180** | 区间[2026-06-01,2026-07-31]：WO-1(100) + WO-2(80) |
| `stockPartialCount` | **1** | WO-4 STOCK_PARTIAL |
| `onTimeRate` | **0.5** | COMPLETED=2（WO-1 准时 actualEnd 06-28≤plannedEnd 06-30；WO-2 延期 07-08>07-05）→ 1/2 |

### 2.2 维护看板 `ErpMntDashboard__getDashboardKpi(startDate=2026-07-01, endDate=2026-07-31)`

口径（`ErpMntDashboardBizModel.getDashboardKpi`，`dashboards.md §8`）：equipmentTotal = count(status≠DECOMMISSIONED)；runningCount = count(RUNNING)；openRequestCount = count(request.status=OPEN)；periodVisitCount = count(visit.status=COMPLETED AND businessDate∈区间)。

| KPI 字段 | 期望值 | 派生 |
|----------|--------|------|
| `equipmentTotal` | **3** | equipment 1/2/3（无 DECOMMISSIONED） |
| `runningCount` | **2** | equipment 1/2 RUNNING |
| `openRequestCount` | **1** | REQ-2026-001 OPEN |
| `periodVisitCount` | **1** | 区间[2026-07-01,2026-07-31]：仅 visit 1(businessDate 07-03)；visit 2(06-20) 在历史月不计 |

### 2.3 质量看板 `ErpQaDashboard__getDashboardKpi(startDate=2026-07-01, endDate=2026-07-31)`

口径（`ErpQaDashboardBizModel.getDashboardKpi`，`dashboards.md §9`）：inspectionCount = count(inspectionDate∈区间)；passRate = count(result=ACCEPTED)/total；rejectedCount = count(result=REJECTED)；openNcrCount = count(ncr.status IN [OPEN, IN_REVIEW])。

| KPI 字段 | 期望值 | 派生 |
|----------|--------|------|
| `inspectionCount` | **3** | INS-1/2/3 全在 2026-07 |
| `passRate` | **0.6666666666666666** | ACCEPTED=2(INS-1,INS-3) / total=3 → `(double)2/(double)3`（IEEE754 双精度全精度，spec 写 `0.6666666666666666` 与 Java 双精/JS number 位级一致；非 0.6667 截断） |
| `rejectedCount` | **1** | INS-2 REJECTED |
| `openNcrCount` | **2** | NCR-1 OPEN + NCR-2 IN_REVIEW |

### 2.4 质量看板 SPC 预警 `ErpQaDashboard__getSpcOutOfControlWarning`

口径（`ErpQaDashboardBizModel.getSpcOutOfControlWarning`，`dashboards.md §9`）：outOfControlChartCount = distinct spc_sample.chartId where isOutOfControl=true；inadequateCapabilityCount = distinct spc_capability.chartId where capabilityLevel=INADEQUATE（config-gated）；openSpcNcrCount = count(ncr sourceType=SPC 且 status IN [OPEN,IN_REVIEW]）（config-gated）。

| 字段 | 期望值 | 派生 |
|------|--------|------|
| `outOfControlChartCount` | **0** | SPC 三表未 seed（0930-2 Deferred） |
| `inadequateCapabilityCount` | **0** | 同上（config-gated 开但无数据） |
| `openSpcNcrCount` | **0** | 2 条 NCR sourceType=INSPECTION 非 SPC |

> 确定性 0 断言（非跳过），覆盖该 `@BizQuery` 可观测性。`includeInadequate`/`includeNcr` 为布尔 true（非数值，spec 不纳入断言）。

## 3. 报表渲染数值 token 期望值（GraphQL `__renderHtml`）

> 报表单元格经 `#,##0.00`（金额/数量）/ `#,##0`（整数）/ `0.00%`（比率）格式化。helper 断言时剥离千分位逗号后匹配 token，规避 AMIS DOM 抖动。

### 3.1 制造生产差异报表 `ErpMfgReport__renderHtml(reportName="production-variance-report")`

数据集 `buildProductionVarianceDataset(null,null,null)` → cost_variance id=1（workOrderId=1）。num 格式 `#,##0.00`。

| token（剥离逗号后） | 来源 |
|--------------------|------|
| `生产差异分析表` | 标题 |
| `WO-2026-001` | workOrderCode（经 cost_variance.workOrderId→work_order 关系解析） |
| `6000.00` | standardAmount=6000.00 → "6,000.00" |
| `6300.00` | actualAmount=6300.00 → "6,300.00" |
| `300.00` | varianceAmount=300.00 → "300.00" |

### 3.2 制造预测差异报表 `ErpMfgReport__renderHtml(reportName="forecast-variance-report")`

数据集 `buildForecastVarianceDataset(null,null,null)` → forecast(APPROVED)×forecast_line×COMPLETED work_order 按 productId 聚合：materialId=1 forecastQty=200 / actualQty=180(100+80) / variance=-20 / varianceRatio=-0.1000。num 格式 `#,##0.00`。

| token | 来源 |
|-------|------|
| `需求预测差异分析表` | 标题 |
| `200.00` | forecastQty=200 → "200.00" |
| `180.00` | actualQty=180 → "180.00" |
| `-20.00` | variance=-20 → "-20.00" |

> varianceRatio=-0.1000 渲染 "-0.1000"（spec 可选附加断言；本表主断言取前三项确定性强 token）。

### 3.3 维护历史报表 `ErpMntReport__renderHtml(reportName="maintenance-history")`

数据集 `buildMaintenanceHistoryDataset(null,null,null)` → visit 1/2 全量。num 格式 `#,##0.00`（visitId/equipmentId/totalMinutes/taskCount/sparePartUsageCount）。

| token | 来源 |
|-------|------|
| `维护历史表` | 标题 |
| `VIS-2026-001` | visit 1 visitCode |
| `120.00` | visit 1 totalMinutes=120.00 |
| `90.00` | visit 2 totalMinutes=90.00 |

> visit 1 taskCount=1/sparePartUsageCount=1（visit_task 1 + spare_part_usage 1 均 visitId=1）渲染 "1.00"；visit 2 均为 0 渲染 "0.00"。

### 3.4 质量检验合格率报表 `ErpQaReport__renderHtml(reportName="inspection-summary")`

数据集 `buildInspectionSummaryDataset(null,null,null)` → 按 materialId 聚合：material 3（INS-1 ACCEPTED，total=1/accepted=1）+ material 1（INS-2 REJECTED+INS-3 ACCEPTED，total=2/accepted=1/rejected=1）。整数列 `#,##0`，passRate 列 `0.00%`（乘 100 加 %）。

| token | 来源 |
|-------|------|
| `质检合格率统计表` | 标题 |
| `产品甲` | material 1 materialName（"ERP 标准型产品甲"含子串） |
| `100.00%` | material 3 passRate=1.0 → "100.00%" |
| `50.00%` | material 1 passRate=0.5 → "50.00%" |

> material 1 totalInspections=2/acceptedCount=1/rejectedCount=1 渲染整数 "2"/"1"/"1"；material 3 totalInspections=1/acceptedCount=1。

## 4. variables 选择依据汇总

| spec | variables | 依据 |
|------|-----------|------|
| manufacturing.value（KPI） | `{startDate:'2026-06-01', endDate:'2026-07-31'}` | 覆盖 COMPLETED 工单 actualEndDate 两月区间，锁定 periodCompletedQty=180 |
| maintenance.value（KPI） | `{startDate:'2026-07-01', endDate:'2026-07-31'}` | 锁定本期 visit，periodVisitCount=1（visit 2 历史月不计） |
| quality.value（KPI） | `{startDate:'2026-07-01', endDate:'2026-07-31'}` | 锁定本期检验，inspectionCount=3 |
| quality.value（SPC） | `{}` | getSpcOutOfControlWarning 无参数，确定性返回 0 |
| mfg-production-variance / mfg-forecast-variance / mnt-maintenance-history / qa-inspection-summary（报表） | `{reportName:'<name>'}` | 报表仅 reportName，数据集聚合全量（无区间过滤），确定性 |

## 5. 非范围（Deferred，期望值不派生）

- mfg crp-load 报表 → 0930-1 Deferred（workcenter 配置链未 seed）。
- mnt downtime-summary / qa ncr-capa-summary → 0930-3 Deferred（同域增强，范式相同，本计划至少覆盖 4 报表）。
- 三域 GL 凭证串联数值 → 三域 posted=false，0930-1/0930-2 Deferred。
- 其他扩展域（CRM/CS/HR/logistics/b2b/contract/drp/aps/master-data）→ 对应域交易种子未 seed，0930-3 Deferred。
