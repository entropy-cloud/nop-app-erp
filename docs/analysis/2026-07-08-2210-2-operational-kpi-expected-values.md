# 运营域看板/报表确定性数值期望值表（2210-2 数据驱动 E2E 断言）

> Owner: `docs/plans/2026-07-08-2210-2-operational-domain-value-assertions.md` Phase 1 Exit Criteria
> 数据基线: `app-erp-all/src/main/resources/_vfs/_init-data/erp_{inv,ast,prj}_*.csv`（13 CSV / 18 行，2210-1 固化运营域种子集；叠加 1234-1 主数据 + 1445-1 P2P/O2C 共 57 CSV）
> 聚合口径源: `ErpInvDashboardBizModel` / `ErpAstDashboardBizModel` / `ErpPrjDashboardBizModel` / `Erp{Inv,Ast,Prj}ReportBizModel`
> 范式承接: `docs/analysis/2026-07-08-1445-2-kpi-expected-values.md`（看板 `assertDashboardKpiValues` + 报表 `assertReportRenderedWithValue`）

## 0. 确定性前提

- Playwright webServer 每次启动前 `rm -f db/erp.mv.db db/erp.trace.db`（fresh-DB 重置）+ `-Dnop.orm.init-database-data=true`，57 CSV 按拓扑序确定性插入。
- 运营域种子集固定（2210-1 固化），且三域看板/报表**读域表（stock_balance/asset/depreciation_schedule/project/project_pnl/...）非 GL 凭证**（区别于 finance 看板读 GL）——故运营域 posted 统一 false 不影响聚合结果，后端聚合结果是确定性的，spec 可硬编码期望值。
- **日期漂移防护**：
  - **库存看板**「本期出入库量」依赖业务日期区间（默认 `today.withDayOfMonth(1)`→`today`）。种子移动单 businessDate=2026-07-03。spec **显式传 `startDate=2026-07-01` / `endDate=2026-07-31`** 锁定种子区间。
  - **资产看板**「本期折旧」依赖 `period`（默认服务端 `currentPeriod()`=`YYYY-MM`）。种子折旧计划 period=`2026-07`。spec **显式传 `periodId="2026-07"`** 锁定种子期间，规避服务端日期漂移致 periodDepreciation=0。
  - **项目看板** `getDashboardKpi()`/`getProjectGrossMargin()` 无日期参数，聚合域表全量（OPEN 项目 / 全部 PnL 记录），无日期漂移。
- **seed 漂移同步机制**：若 2210-1 运营域 seed CSV 变更（行增减/金额改动/period 改动），本表期望值须同步更新，对应 `*.value.spec.ts` 的 `expected`/`expectedTokens` 亦须同步（与 1445-2 同机制）。

## 1. 种子数据关键行（期望值派生依据）

### 1.1 库存（`erp_inv_*.csv`）

| 表 | 行 | 关键列 | 值 |
|----|----|--------|-----|
| stock_balance | id=1 | materialId=3 / warehouseId=2 / totalCost | 850.00 |
| stock_balance | id=2 | materialId=1 / warehouseId=1 / totalCost | 9600.00 |
| stock_move | id=1 | code=MV-2026-001 / moveType=INCOMING / businessDate=2026-07-03 / docStatus=DONE | — |
| stock_move_line | id=1 | moveId=1 / materialId=3 / quantity=100 / totalCost=850 | — |
| cost_layer | id=1 | materialId=3 / totalCost=850 | — |
| cost_layer | id=2 | materialId=1 / totalCost=9600 | — |

> 无 OUTGOING 移动单 → outgoingQty/outgoingCost=0。

### 1.2 资产（`erp_ast_*.csv`）

| 表 | 行 | 关键列 | 值 |
|----|----|--------|-----|
| asset_category | id=1/2 | 办公设备 / 机器设备 | — |
| asset | id=1 | AST-2026-001 / categoryId=1 / IN_SERVICE / originalValue=12000 / accumulatedDepreciation=0 / netBookValue=12000 | — |
| asset | id=2 | AST-2026-002 / categoryId=2 / IN_SERVICE / originalValue=120000 / accumulatedDepreciation=6000 / netBookValue=114000 | — |
| asset | id=3 | AST-2026-003 / categoryId=1 / IN_SERVICE / originalValue=3000 / accumulatedDepreciation=0 / netBookValue=3000 | — |
| depreciation_schedule | id=1 | assetId=2 / period=2026-07 / actualAmount=2000 / accumulatedDepreciation=6000 / EXECUTED / businessDate=2026-07-31 | — |

> 3 行 IN_SERVICE；仅 asset 2 有累计折旧 + 本期折旧计划。无 CIP（在建工程）seed → cipBalance=0。

### 1.3 项目（`erp_prj_*.csv`）

| 表 | 行 | 关键列 | 值 |
|----|----|--------|-----|
| project_type | id=1 | PRJ-TYPE-IT / IT 实施项目 | — |
| project | id=1 | PRJ-2026-001 / customerId=1 / budget=50000 / committedCost=30000 / actualCost=30000 / billedAmount=0 / status=OPEN | — |
| cost_collection | id=1 | projectId=1 / totalAmount=30000 / businessDate=2026-07-05 | — |
| budget | id=1 | projectId=1 / totalAmount=50000 | — |
| timesheet | id=1 | projectId=1 / userId=2 / workDate=2026-07-05 / hours=8 / costAmount=800 | — |
| project_pnl | id=1 | projectId=1 / revenueAmount=50000 / totalCost=30000 / grossProfit=20000 / grossMarginPct=40 / CALCULATED | — |

## 2. 看板 KPI 期望值（GraphQL `__getDashboardKpi`）

### 2.1 库存看板 `ErpInvDashboard__getDashboardKpi(startDate=2026-07-01, endDate=2026-07-31)`

口径（`ErpInvDashboardBizModel.getDashboardKpi`）：totalValue = Σ `ErpInvStockBalance.totalCost`；incomingQty/outgoingQty = Σ `ErpInvStockMoveLine.quantity`（DONE 期内，INCOMING 正 / OUTGOING 绝对值）；turnoverRate = outgoingCost / totalValue。

| KPI 字段 | 期望值 | 派生 |
|----------|--------|------|
| `totalValue` | **10450** | 850 + 9600 |
| `incomingQty` | **100** | move 1（INCOMING/DONE/2026-07-03 期内）line qty=100 |
| `outgoingQty` | **0** | 无 OUTGOING 移动单 |
| `turnoverRate` | **0** | outgoingCost(0) / totalValue(10450) |

### 2.2 资产看板 `ErpAstDashboard__getDashboardKpi(periodId="2026-07")`

口径（`ErpAstDashboardBizModel.getDashboardKpi`）：originalValue = Σ `ErpAstAsset.originalValue`（IN_SERVICE）；accumulatedDepreciation = Σ `ErpAstAsset.accumulatedDepreciation`（IN_SERVICE）；netBookValue = originalValue − accumulatedDepreciation；periodDepreciation = Σ `ErpAstDepreciationSchedule.actualAmount`（EXECUTED + period 匹配）。

| KPI 字段 | 期望值 | 派生 |
|----------|--------|------|
| `originalValue` | **135000** | 12000 + 120000 + 3000 |
| `accumulatedDepreciation` | **6000** | 0 + 6000 + 0 |
| `netBookValue` | **129000** | 135000 − 6000 |
| `periodDepreciation` | **2000** | schedule(period=2026-07, EXECUTED) actualAmount=2000 |
| `cipBalance` | 0 | 无 CIP seed |

### 2.3 项目看板 `ErpPrjDashboard__getDashboardKpi()`（无参，聚合全量）

口径（`ErpPrjDashboardBizModel.getDashboardKpi`）：openProjectCount = count `ErpPrjProject`（OPEN）；totalBudget = Σ `ErpPrjBudget.totalAmount`（OPEN 项目）；incurredCost = Σ `ErpPrjCostCollection.totalAmount`（OPEN 项目）；executionRate = incurredCost / totalBudget。

| KPI 字段 | 期望值 | 派生 |
|----------|--------|------|
| `openProjectCount` | **1** | project 1 OPEN |
| `totalBudget` | **50000** | budget(projectId=1).totalAmount |
| `incurredCost` | **30000** | cost_collection(projectId=1).totalAmount |
| `executionRate` | **0.6** | 30000 / 50000 |

### 2.4 项目毛利率 `ErpPrjDashboard__getProjectGrossMargin()`（无 projectId，聚合全部 PnL）

口径（`ErpPrjDashboardBizModel.getProjectGrossMargin`）：聚合 `ErpPrjProjectPnl`，Σ revenue/totalCost/grossProfit；grossMarginPct = Σ grossProfit / Σ revenue。

| KPI 字段 | 期望值 | 派生 |
|----------|--------|------|
| `projectCount` | **1** | PnL projectId=1 |
| `totalRevenue` | **50000** | revenueAmount=50000 |
| `totalCost` | **30000** | totalCost=30000 |
| `totalGrossProfit` | **20000** | grossProfit=20000 |
| `grossMarginPct` | **0.4** | 20000 / 50000 |

## 3. 报表渲染数值期望 token（GraphQL `Erp{Inv,Ast,Prj}Report__renderHtml`）

> 断言范式：POST `renderHtml` GraphQL query 取 HTML 字符串，断言含期望数值 token（剥离千分位逗号后匹配，规避 AMIS 渲染层 DOM 抖动）。数值经模板 `numberFormat=#,##0.00` / `0.00%` 格式化。

### 3.1 资产折旧明细表 `asset-depreciation-detail`（无 data → 全部 3 资产）

数据集（`buildAssetDepreciationDetailDataset`，每资产行 originalValue/accumulatedDepreciation/netBookValue/periodDepreciation）：

| assetId | originalValue | accumulatedDepreciation | netBookValue | periodDepreciation |
|---------|---------------|------------------------|--------------|--------------------|
| 1 | 12000.00 | 0 | 12000.00 | 0 |
| 2 | 120000.00 | 6000.00 | 114000.00 | 2000.00 |
| 3 | 3000.00 | 0 | 3000.00 | 0 |

**期望 token**（HTML 含）：标题 `资产折旧明细表`；asset 2 原值 `120000.00`；asset 2 累计折旧 `6000.00`；asset 2 本期折旧 `2000.00`；asset 2 净值 `114000.00`。

### 3.2 库存追溯链可视化报表 `inventory-trace-report`（data:{moveId:1}）

数据集（`buildInventoryTraceDataset`）：moveId=1 经 `TraceChainQuery.forwardTrace` 返回根节点（move 1，无下游 originMoveId 链）→ 1 行 {moveCode=MV-2026-001, moveType=INCOMING, businessDate=2026-07-03, destWarehouseId=2, docStatus=DONE, quantity=100, ...}。

> **variables 选择依据**：`buildInventoryTraceDataset` 在 batchNo/materialId/warehouseId/moveId 全空时返回空集（`findCandidateMoves(null,null)` 短路）。故 spec 必须传至少一个定位键；选 `moveId=1`（唯一移动单，确定性正向链根节点）。

**期望 token**（HTML 含）：标题 `库存追溯链可视化报表`；移动单号 `MV-2026-001`；数量 `100.00`（num 格式 #,##0.00）。

### 3.3 项目成本汇总表 `project-cost-summary`（无 data → 全部项目）

数据集（`buildProjectCostSummaryDataset`，每项目行 budget/committedCost/actualCost/billedAmount/executionRate）：

| projectId | budget | committedCost | actualCost | billedAmount | executionRate |
|-----------|--------|---------------|------------|--------------|---------------|
| 1 | 50000.00 | 30000.00 | 30000.00 | 0.00 | 0.6000 → 60.00% |

**期望 token**（HTML 含）：标题 `项目成本汇总表`；预算 `50000.00`；实际/承诺成本 `30000.00`；预算执行率 `60.00%`（alert 样式 numberFormat=0.00%）。

## 4. 断言范式 Decision（Phase 1 item 2）

**选择**：复用 1445-2 既有 helper（`assertDashboardKpiValues` / `assertReportRenderedWithValue`），**直接 GraphQL query 取值断言**。

- **看板**：`page.request.post('/graphql', { query, variables })` 取 `getDashboardKpi` / `getProjectGrossMargin` 原始 Map，逐字段 `expect(Number(field) === expected)`。项目域断言两个方法（getDashboardKpi + getProjectGrossMargin，1100-3 已就绪）。
- **报表**：`page.request.post` 取 `renderHtml` HTML 字符串，断言含期望数值 token（剥离千分位逗号后匹配）。

**variables 选择依据（每 spec）**：

| spec | variables | 依据 |
|------|-----------|------|
| inventory 看板 | `startDate=2026-07-01, endDate=2026-07-31` | 锁定种子移动单 businessDate 区间（镜像 sales/purchase 范式） |
| assets 看板 | `periodId="2026-07"` | 锁定种子折旧 period（镜像 finance periodId 范式），规避 currentPeriod() 漂移 |
| projects 看板 | `{}`（无参） | getDashboardKpi/getProjectGrossMargin 无日期参数，聚合域表全量 |
| asset-depreciation 报表 | `{}`（无 data） | 全资产明细 |
| inventory-trace 报表 | `data:{moveId:1}` | forwardTrace 需根节点定位键（全空返回空集） |
| project-cost-summary 报表 | `{}`（无 data） | 全项目汇总 |

**残留风险与防护**：2210-1 seed 行变更致期望值漂移 → 本表 §1 标注 seed 依赖；seed 变更须同步本表 + spec。日期漂移（库存/资产看板默认区间/期间依赖运行时日期）→ spec 显式传 startDate/endDate/periodId 锁定。
