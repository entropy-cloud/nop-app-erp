# 种子数据模块

> **资产类型**：本文描述的是**部署资产**（系统初始化/基础配置数据，随部署一次性导入），**非测试资产**。测试共享夹具见 `app-erp-test-data` 模块与 `testing-strategy.md §四类测试资产边界`。两者不可混淆。
>
> **当前状态**：部署期种子数据**已落地**（2026-07-08，plan `2026-07-08-1234-1`）——经平台 `DataInitInitializer` + `_vfs/_init-data/*.csv`（21 张核心主数据表），config-gated 由 `-Dnop.orm.init-database-data=true` + fresh-DB 重置触发（E2E/演示），生产 `application.yaml` 默认关闭。机制/列映射/门控见 `docs/analysis/2026-07-08-1234-1-seed-data-table-column-map.md`。下方描述的独立 `app-erp-seed` 模块（版本化/增量导入/按租户账套）仍为**独立 follow-up**（未实现），用于更结构化的种子管理场景。
>
> **交易单据种子（P2P+O2C）已落地**（2026-07-08，plan `2026-07-08-1445-1`）——在 21 张主数据 CSV 之上新增 **23 张交易单据 CSV**（共 44 张），覆盖采购到付款（PO→Receive→Invoice→Payment）+ 销售到收款（SO→Delivery→Invoice→Receipt）各 1 条端到端最小连通链，含对应**已过账财务产物**（凭证/凭证行/业财回链/AR-AP 辅助账/GL 余额/会计期间 OPEN）。列映射/拓扑序/范围裁决见 `docs/analysis/2026-07-08-1445-1-transaction-seed-table-map.md`。
>
> **运营域交易单据种子（库存/资产/项目）已落地**（2026-07-08，plan `2026-07-08-2210-1`）——在 44 张 CSV 之上新增 **13 张运营域表 CSV**（共 57 张），覆盖库存（stock_move+line/stock_balance/cost_layer）+ 资产（asset_category/asset/depreciation_schedule）+ 项目（project_type/project/cost_collection/timesheet/budget/project_pnl）三域最小连通集。列映射/拓扑序/范围裁决见 `docs/analysis/2026-07-08-2210-1-operational-domain-seed-table-map.md`。

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

- 扩展域交易单据（manufacturing/HR/quality/maintenance/CRM/CS/logistics/b2b/contract/drp/aps）——按域逐批补充（1234-1/1445-1 Deferred 既定策略）。**inventory/assets/projects 已于 2210-1 落地**（见上方「运营域交易单据种子」段）。
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
- 其他扩展域交易种子（manufacturing/quality/maintenance/CRM/CS/HR/logistics/b2b/contract/drp/aps）——按域逐批补充（1445-1 Deferred 既定策略）。
- 精确运营域 KPI/报表数值断言——本计划解除「运营域交易数据存在」阻塞（数值非零可观测）；精确断言由 `2026-07-08-2210-2` 承接。
