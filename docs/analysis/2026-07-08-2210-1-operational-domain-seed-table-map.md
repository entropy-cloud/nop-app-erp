# 运营域（库存/资产/项目）业务交易单据种子 — 表清单、列映射、加载拓扑序与范围裁决

> Owner: `docs/plans/2026-07-08-2210-1-operational-domain-transaction-seeds.md` Phase 1 Exit Criteria
> 权威源: `module-{inventory,assets,projects}/model/app-erp-*.orm.xml`（逐表逐列核实，非采信旧记忆）
> 上游主数据参照: `docs/analysis/2026-07-08-1234-1-seed-data-table-column-map.md`（21 张主数据 CSV 已 seed）
> 前序交易种子范式: `docs/analysis/2026-07-08-1445-1-transaction-seed-table-map.md`（P2P+O2C + 财务产物种子范式）

## 0. 约定（与 1234-1 / 1445-1 一致）

- CSV 列名 = 实体 column `code`（UPPER_SNAKE_CASE 数据库列名）。
- `ID` 列虽 `tagSet="seq-default"`，但跨表 FK 引用需固定 ID，故 CSV 显式提供 `ID`。
- 框架自动填充字段（`CREATED_BY`/`CREATE_TIME`/`UPDATED_BY`/`UPDATE_TIME`/`DEL_VERSION`/`VERSION`）由 ORM 拦截器自动填，CSV 不含。
- 多租户 `TENANT_ID` 由框架兜底（1234-1/1445-1 经验性确认 seed 无须提供）。
- 布尔列值用小写字符串 `true`/`false`（与 1445-1 `POSTED` 列一致）。
- 日期列值 `YYYY-MM-DD`；datetime 列本批不 seed（postedAt/approvedAt/executedAt 等非 mandatory 审计列一律省略）。
- **关键陷阱（库存 UoM 列名）**：库存行表计量单位列 `code="UO_M_ID"`（驼峰 prop `uoMId`，与采购三张行表一致），非 `UOM_ID`。CSV 必须按 `code` 区分，列名错配会在启动期抛 NopException。

## 1. 加载拓扑序（DataInitInitializer 按 ORM `getEntityModelsInTopoOrder()` 自动排序）

跨域依赖序（seed 设计依赖序，确保 FK 上游先于下游）。所有运营域表仅引用 1234-1 已 seed 主数据 + 本批先 seed 的上游域配置/单据，不引用 1445-1 P2P/O2C 单据：

```
[1234-1 主数据(已 seed)] md_organization/md_currency/md_employee/md_partner/
  md_warehouse/md_location/md_material/md_material_sku/md_uom
  → [上游域配置] ast_asset_category / prj_project_type
    → [域头] ast_asset / prj_project
      → [域行/计算产物]
        inv_stock_move → inv_stock_move_line
        inv_stock_balance / inv_cost_layer        （引用 material/warehouse，独立于 move）
        ast_depreciation_schedule                  （引用 asset）
        prj_cost_collection / prj_timesheet /
        prj_budget / prj_project_pnl               （引用 project）
```

> 域配置（asset_category/project_type）必须先于域头（asset/project）：asset.categoryId→asset_category，project.projectTypeId→project_type。
> 域头必须先于域行/计算产物：move→move_line，asset→depreciation_schedule，project→(cost_collection/timesheet/budget/project_pnl)。
> stock_balance / cost_layer 引用 material/warehouse（1234-1 已 seed），与 stock_move 互相独立，加载序无约束。

## 2. seed 表清单 + 列映射（每表：mandatory 业务列 / FK 列 / 框架列省略）

> 标注：**M**=mandatory（CSV 须填）；**FK**=外键引用上游已 seed ID；**opt**=可选（默认值或 null，按需填）。框架审计列（DEL_VERSION/VERSION/CREATED_BY 等）全部省略。

### 2.1 库存域（inventory）

| 表 | code 列（角色） | seed 行 |
|----|----------------|--------|
| erp_inv_stock_move | ID; CODE(M); MOVE_TYPE(M, dict erp-inv/operation-type: INCOMING/OUTGOING/INTERNAL/MANUFACTURE); ORG_ID(FK org=2); BUSINESS_DATE(M); DEST_WAREHOUSE_ID(FK wh); DOC_STATUS(M, dict erp-inv/move-status: DRAFT/CONFIRMED/DONE/CANCELLED); APPROVE_STATUS(M, dict erp-inv/approve-status: UNSUBMITTED/SUBMITTED/APPROVED/REJECTED); POSTED(opt=false); RELATED_BILL_TYPE(opt); RELATED_BILL_CODE(opt) | 1 |
| erp_inv_stock_move_line | ID; MOVE_ID(FK move,M); LINE_NO(M); MATERIAL_ID(FK,M); SKU_ID(FK); UO_M_ID(FK,M); QUANTITY(M); UNIT_COST(opt); TOTAL_COST(opt); CURRENCY_ID(FK) | 1 |
| erp_inv_stock_balance | ID; ORG_ID(FK org=2); MATERIAL_ID(FK,M); SKU_ID(FK); WAREHOUSE_ID(FK,M); TOTAL_QUANTITY(M); AVAILABLE_QUANTITY(M); COST_METHOD(dict erp-md/cost-method); AVG_COST(opt); TOTAL_COST(opt); CURRENCY_ID(FK) | 2 |
| erp_inv_cost_layer | ID; ORG_ID(FK org=2); MATERIAL_ID(FK,M); SKU_ID(FK); WAREHOUSE_ID(FK,M); COST_METHOD(M, dict erp-md/cost-method); INCOMING_QUANTITY(M); REMAINING_QUANTITY(M); UNIT_COST(M); TOTAL_COST(M); CURRENCY_ID(FK); INCOMING_DATE(opt); INCOMING_MOVE_ID(FK opt) | 2 |

### 2.2 资产域（assets）

| 表 | code 列（角色） | seed 行 |
|----|----------------|--------|
| erp_ast_asset_category | ID; CODE(M); NAME(M); DEPRECIATION_METHOD(dict erp-ast/depreciation-method, opt); USEFUL_LIFE_MONTHS(opt); SUBJECT_ID(opt,无 GL 科目留空); DEPRECIATION_SUBJECT_ID(opt); EXPENSE_SUBJECT_ID(opt); REMARK(opt) | 2 |
| erp_ast_asset | ID; CODE(M); NAME(M); ORG_ID(FK org=2); CATEGORY_ID(FK category); ACQUISITION_DATE(M); CURRENCY_ID(FK); ORIGINAL_VALUE(M); CURRENT_VALUE(opt); RESIDUAL_VALUE(opt); DEPRECIATION_METHOD(dict, opt); DEPRECIATION_RATE(opt); USEFUL_LIFE_MONTHS(opt); DEPARTMENT_ID(FK org, opt); LOCATION_ID(FK location, opt); EMPLOYEE_ID(FK emp, opt); STATUS(M, dict erp-ast/asset-status: IN_SERVICE); ACCUMULATED_DEPRECIATION(opt); NET_BOOK_VALUE(opt); REMARK(opt) | 3 |
| erp_ast_depreciation_schedule | ID; ASSET_ID(FK,M); ORG_ID(FK org=2); PERIOD(M,格式 YYYY-MM); PLANNED_AMOUNT(M); ACTUAL_AMOUNT(opt); ACCUMULATED_DEPRECIATION(opt); NET_BOOK_VALUE(opt); STATUS(M, dict erp-ast/depreciation-schedule-status: PENDING/EXECUTED); POSTED(opt=false); BUSINESS_DATE(opt); CURRENCY_ID(FK); EXCHANGE_RATE(M, 默认 1); AMOUNT_SOURCE(opt); AMOUNT_FUNCTIONAL(opt) | 1 |

### 2.3 项目域（projects）

| 表 | code 列（角色） | seed 行 |
|----|----------------|--------|
| erp_prj_project_type | ID; CODE(M); NAME(M); DEFAULT_SUBJECT_ID(opt,无 GL 科目留空); REMARK(opt) | 1 |
| erp_prj_project | ID; CODE(M); NAME(M); ORG_ID(FK org=2); PROJECT_TYPE_ID(FK project_type); CUSTOMER_ID(FK partner); CURRENCY_ID(FK); START_DATE(opt); END_DATE(opt); BUDGET(opt); COMMITTED_COST(opt); ACTUAL_COST(opt); BILLED_AMOUNT(opt); STATUS(M, dict erp-prj/project-status: DRAFT/OPEN/ON_HOLD/COMPLETED/CANCELLED); MANAGER_ID(FK emp); REMARK(opt) | 1 |
| erp_prj_cost_collection | ID; CODE(M); PROJECT_ID(FK,M); ORG_ID(FK org=2); BUSINESS_DATE(M); CURRENCY_ID(FK); TOTAL_AMOUNT(opt); DOC_STATUS(M, dict erp-prj/project-status); APPROVE_STATUS(M, dict wf/approve-status: UNSUBMITTED/SUBMITTED/APPROVED/REJECTED); POSTED(opt=false); EXCHANGE_RATE(M, 默认 1); AMOUNT_SOURCE(opt); AMOUNT_FUNCTIONAL(opt); REMARK(opt) | 1 |
| erp_prj_timesheet | ID; CODE(M); ORG_ID(FK org=2); PROJECT_ID(FK,M); USER_ID(FK emp,M); WORK_DATE(M); HOURS(M); CURRENCY_ID(FK); COST_RATE(opt); COST_AMOUNT(opt); STATUS(M, dict wf/approve-status); POSTED(opt=false) | 1 |
| erp_prj_budget | ID; CODE(M); PROJECT_ID(FK,M); ORG_ID(FK org=2); BUSINESS_DATE(M); CURRENCY_ID(FK); TOTAL_AMOUNT(opt); DOC_STATUS(M, dict erp-prj/project-status); APPROVE_STATUS(M, dict wf/approve-status); REMARK(opt) | 1 |
| erp_prj_project_pnl | ID; CODE(M); PROJECT_ID(FK,M); ORG_ID(FK org=2); PERIOD_FROM(M); PERIOD_TO(M); CURRENCY_ID(FK); EXCHANGE_RATE(M, 默认 1); REVENUE_AMOUNT(opt); COST_LABOR(opt); COST_MATERIAL(opt); COST_EXPENSE(opt); COST_SUBCONTRACT(opt); TOTAL_COST(opt); GROSS_PROFIT(opt); GROSS_MARGIN_PCT(opt); CALC_STATUS(M, dict erp-prj/pnl-calc-status: PENDING/CALCULATED); DOC_STATUS(M, dict erp-prj/project-status); APPROVE_STATUS(M, dict wf/approve-status); POSTED(opt=false) | 1 |

**字典码值（已核实 dict.yaml）**：
- `erp-inv/operation-type`：INCOMING/OUTGOING/INTERNAL/MANUFACTURE → 本批 INCOMING 移动用 `INCOMING`
- `erp-inv/move-status`：DRAFT/CONFIRMED/DONE/CANCELLED → 本批 move 用 `DONE`（看板 `loadDoneMovesInRange` 仅统计 DOC_STATUS=DONE）
- `erp-inv/approve-status`：UNSUBMITTED/SUBMITTED/APPROVED/REJECTED → 本批 move 用 `APPROVED`
- `erp-md/cost-method`：MOVING_AVERAGE/WEIGHTED_AVERAGE/FIFO/... → 与 md_material.COST_METHOD 一致：物料 1 用 `MOVING_AVERAGE`、物料 3 用 `WEIGHTED_AVERAGE`
- `erp-ast/asset-status`：DRAFT/IN_SERVICE/IDLE/SCRAPPED/SOLD/DISPOSED → 本批资产全用 `IN_SERVICE`（看板 `loadInServiceAssets` 仅统计 IN_SERVICE）
- `erp-ast/depreciation-method`：STRAIGHT_LINE/DECLINING/UNITS → 本批用 `STRAIGHT_LINE`
- `erp-ast/depreciation-schedule-status`：PENDING/EXECUTED/REVERSED/CANCELLED → 已折旧行用 `EXECUTED`（看板 `sumPeriodDepreciation` 仅统计 EXECUTED）
- `erp-prj/project-status`：DRAFT/OPEN/ON_HOLD/COMPLETED/CANCELLED → project 用 `OPEN`（看板 `loadOpenProjects` 仅统计 OPEN）；cost_collection/budget/project_pnl 的 DOC_STATUS 用 `OPEN`（活跃态）/`COMPLETED`（预算已定稿）
- `erp-prj/pnl-calc-status`：PENDING/CALCULATED → project_pnl 用 `CALCULATED`
- `wf/approve-status`：UNSUBMITTED/SUBMITTED/APPROVED/REJECTED → cost_collection/timesheet/project_pnl 的 APPROVE_STATUS/STATUS 用 `APPROVED`

## 3. 范围 Decision（Phase 1 item 2）

**选择**：运营域三域各 1-3 条最小连通记录（镜像 1445-1「每域 1-2 条最小连通」范式），资产 2-3 台含 1 台已折旧、库存 2 物料余额 + 1 条 DONE 移动、项目 1 OPEN 项目含成本 + PnL。

**记录设计**（引用 1234-1 已 seed 主数据固定 ID；通用 orgId=2(ERP-CO)、currencyId=1(CNY)、acctSchemaId=1）：

### 3.1 库存链（镜像 1445-1 P2P：物料 3 原料 X 钢材 / WH-RAW 原料仓）

- **stock_move 1**（DONE 入库）：`MV-2026-001`，moveType=INCOMING，businessDate=2026-07-03，destWarehouseId=2(WH-RAW)，docStatus=DONE，approveStatus=APPROVED，posted=false（无 GL 凭证，Non-Goal）。
- **stock_move_line 1**：materialId=3(原料 X 钢材)、skuId=3、uoMId=2(KG)、quantity=100、unitCost=8.50、totalCost=850.00、currencyId=1（与 1445-1 采购入库 100KG×8.50 同口径，金额自洽）。
- **stock_balance**（2 行，金额自洽 cost_layer）：
  - 1：materialId=3、warehouseId=2(WH-RAW)、totalQuantity=100、availableQuantity=100、costMethod=WEIGHTED_AVERAGE、avgCost=8.50、totalCost=850.00、currencyId=1。
  - 2：materialId=1(产品甲)、warehouseId=1(WH-MAIN)、totalQuantity=80、availableQuantity=80、costMethod=MOVING_AVERAGE、avgCost=120.00、totalCost=9600.00、currencyId=1。
- **cost_layer**（2 行，与 stock_balance 同物料/仓库 totalCost 一致）：
  - 1：materialId=3、warehouseId=2、costMethod=WEIGHTED_AVERAGE、incomingQuantity=100、remainingQuantity=100、unitCost=8.50、totalCost=850.00、incomingDate=2026-07-03、incomingMoveId=1、currencyId=1。
  - 2：materialId=1、warehouseId=1、costMethod=MOVING_AVERAGE、incomingQuantity=80、remainingQuantity=80、unitCost=120.00、totalCost=9600.00、incomingDate=2026-07-02、currencyId=1（无入库 move 引用，独立初始余额）。

**金额自洽**：stock_balance(m3).totalCost(850) = cost_layer(m3).totalCost(850)；stock_balance(m1).totalCost(9600) = cost_layer(m1).totalCost(9600)。

### 3.2 资产链（3 台 IN_SERVICE，含 1 台已折旧；2 类别无 GL 科目）

- **asset_category**（2 行，无 GL 科目，posted=false 裁决依据）：
  - 1：`AST-CAT-IT`，办公设备，depreciationMethod=STRAIGHT_LINE，usefulLifeMonths=36。
  - 2：`AST-CAT-MACH`，机器设备，depreciationMethod=STRAIGHT_LINE，usefulLifeMonths=60。
- **asset**（3 行，全 IN_SERVICE）：
  - 1：`AST-2026-001` 笔记本电脑，categoryId=1(IT)，acquisitionDate=2026-06-15，originalValue=12000.00，accumulatedDepreciation=0、netBookValue=12000.00，status=IN_SERVICE，departmentId=2，employeeId=2。
  - 2：`AST-2026-002` 数控机床，categoryId=2(MACH)，acquisitionDate=2026-01-05，originalValue=120000.00，usefulLifeMonths=60，depreciationMethod=STRAIGHT_LINE，accumulatedDepreciation=6000.00（3 个月 × 2000）、netBookValue=114000.00，status=IN_SERVICE，departmentId=2。
  - 3：`AST-2026-003` 激光打印机，categoryId=1(IT)，acquisitionDate=2026-06-20，originalValue=3000.00，accumulatedDepreciation=0、netBookValue=3000.00，status=IN_SERVICE。
- **depreciation_schedule**（1 行，EXECUTED，与 asset 2 同名字段一致）：
  - 1：assetId=2、period=`2026-07`、plannedAmount=2000.00、actualAmount=2000.00、accumulatedDepreciation=6000.00、netBookValue=114000.00、status=EXECUTED、posted=false、businessDate=2026-07-31、currencyId=1、exchangeRate=1。

**金额自洽**：asset 2.accumulatedDepreciation(6000) = depreciation_schedule(asset2).accumulatedDepreciation(6000)；asset 2.netBookValue(114000) = depreciation_schedule(asset2).netBookValue(114000)。

### 3.3 项目链（1 OPEN 项目含成本 + PnL）

- **project_type**（1 行，无 GL 科目）：1：`PRJ-TYPE-IT`，IT 实施项目。
- **project**（1 行，OPEN）：1：`PRJ-2026-001` 华东科技 ERP 实施项目，projectTypeId=1，customerId=1(华东科技)，currencyId=1，startDate=2026-06-01，endDate=2026-12-31，budget=50000.00、actualCost=30000.00，status=OPEN，managerId=2(李四)。
- **cost_collection**（1 行，totalAmount=30000 与 project_pnl.totalCost 一致）：1：`PRJ-CC-2026-001`，projectId=1，businessDate=2026-07-05，currencyId=1，totalAmount=30000.00、amountSource=30000.00、amountFunctional=30000.00，docStatus=OPEN、approveStatus=APPROVED，posted=false。
- **timesheet**（1 行，工时成本，体现 costLabor）：1：`PRJ-TS-2026-001`，projectId=1，userId=2(李四)，workDate=2026-07-05，hours=8.00，costRate=100.00、costAmount=800.00、currencyId=1，status=APPROVED，posted=false。
- **budget**（1 行，totalAmount=50000 与 project.budget 一致）：1：`PRJ-BD-2026-001`，projectId=1，businessDate=2026-06-01，currencyId=1，totalAmount=50000.00，docStatus=COMPLETED、approveStatus=APPROVED。
- **project_pnl**（1 行，CALCULATED，totalCost 与 cost_collection Σ 一致）：1：`PRJ-PNL-2026-001`，projectId=1，periodFrom=2026-06-01、periodTo=2026-07-31，currencyId=1，exchangeRate=1，revenueAmount=50000.00、costLabor=800.00（=timesheet.costAmount）、costMaterial=29200.00、totalCost=30000.00（=cost_collection.totalAmount Σ）、grossProfit=20000.00、grossMarginPct=40.0000、calcStatus=CALCULATED、docStatus=OPEN、approveStatus=APPROVED，posted=false。

**金额自洽**：project_pnl.totalCost(30000) = Σ cost_collection.totalAmount(30000)；costLabor(800) = timesheet.costAmount(800)。

### posted 一致性裁决

本批所有运营域源单据/计算产物统一 `posted=false`。依据（镜像 1445-1「PO/SO posted=false（无对应 GL 凭证）」裁决）：
1. 三域看板读**域表**非 GL（库存读 stock_balance/stock_move_line、资产读 asset/depreciation_schedule、项目读 project/cost_collection/project_pnl），`posted` 标志不被看板消费；
2. 1234-1 seed 的 `erp_md_subject` 仅 8 个 GL 科目（库存现金/银行存款/应收/库存商品/应付/主营收入/主营成本/销售费用），**无库存估值/资产/折旧费用/项目成本专用科目**，seed GL 凭证徒增参照复杂度且不解除额外看板阻塞；
3. 运营域过账 → GL 凭证 seed 归后续（Deferred：运营域业财一体端到端数值回归需 GL 串联时）。

### 替代方案分析

- (a) 同时 seed 运营域 GL 凭证：**rejected**——三域看板读域表非 GL，且种子科目表无运营域专用科目，seed GL 徒增参照复杂度且不解除额外阻塞。
- (b) seed 全部 13 扩展域：**rejected**——复杂度爆炸，按域逐批是 1445-1 Deferred 既定策略。
- (c) 仅 seed 库存不 seed 资产/项目：**rejected**——三域看板并列缺数据，且资产/项目 seed 同样 tractable，合并一批降低 N=2 断言计划跨批碎片。

### 残留风险与防护

- 参照完整性遗漏（FK 列引用未 seed 的上游 ID）→ 启动期 DataInitInitializer 抛 NopException（不静默跳过），Phase 3 fresh-DB 启动验证兜底暴露。
- 列名错配（尤其库存 `UO_M_ID`）→ 同上启动期暴露。
- 非幂等（1234-1/1445-1 已确认）→ fresh-DB 重置（删 `db/erp.mv.db`）是必需前置，playwright webServer 已内置。
- 字典码值错配（如 moveType/period/status 非法值）→ 同上启动期暴露（dict 校验在 entity 校验阶段）。

## 4. 条件性 SQL 裁决

Phase 2 条件性 SQL Add 项：**移出范围**。所有运营域种子经 CSV INSERT 表达，无序列重置 / 批量 UPDATE 需求。故不补 `NN-init-operational-*.sql`。

## 5. seed 行数汇总

| 域 | 表数 | 行数 |
|----|------|------|
| inventory（移动+余额+成本层） | 4（stock_move, stock_move_line, stock_balance, cost_layer） | 1+1+2+2 = 6 |
| assets（类别+资产+折旧） | 3（asset_category, asset, depreciation_schedule） | 2+3+1 = 6 |
| projects（类型+项目+成本+工时+预算+损益） | 6（project_type, project, cost_collection, timesheet, budget, project_pnl） | 1+1+1+1+1+1 = 6 |
| **合计** | **13 张运营域表 CSV** | **18 行** |

> 13 张新运营域表 CSV 加入 `_vfs/_init-data/`，与 1234-1 的 21 张主数据 + 1445-1 的 23 张 P2P/O2C 交易 CSV 共存（总计 44 + 13 = 57 张 CSV）。
