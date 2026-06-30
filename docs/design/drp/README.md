# DRP（分销需求计划）

## 目的

设计分销需求计划（Distribution Requirements Planning）模块：多级分销网络中的库存补货计划。与 MRP（制造端物料需求）形成互补——MRP 计算"生产什么"，DRP 计算"从哪里调拨/采购什么到哪个仓库"。

## 边界

- 本模块负责：多仓库库存网络建模、分销需求计算（基于安全库存+预测+已分配量→净需求）、补货建议（仓间调拨/向供应商采购）、补货单自动生成。
- 本模块不负责：制造端 MRP 物料需求（`mrp.md`）；实际库存移动（inventory 域）。
- 前置条件：启用了多仓库管理 + 批次追溯（可选），各仓库维护了安全库存/补货策略。

## 设计依据

> 参考 **Axelor supplychain**（386 Java 文件，本仓最大模块）：集成了采购/销售/库存/生产的跨域供应链计划。SuppyChain 需求计划覆盖了多仓库补货逻辑。
>
> 参考 **ERPNext** `Material Request` 跨仓补货概念。
>
> 来源 `docs/analysis/erp-survey/2026-06-30-0000-axelor-open-suite.md` §供应链计划。

## 实体清单

> 表前缀 `erp_inv_drp_`、类名 `ErpInvDrp*`、字典 `erp-inv/drp-*`。

### ErpInvDrpPlan（DRP 计划头）

| 字段 | 含义 |
|------|------|
| id/code/orgId | 标准 |
| planName | 计划名称（如"2026-07 月度 DRP"） |
| periodFrom/periodTo | 计划覆盖区间 |
| status | dict `erp-inv/drp-plan-status`：DRAFT / COMPUTED / APPROVED / EXECUTED |
| totalReplenishmentQty | 总补货数量（派生） |
| runAt | 运行时间 |
| runBy | 运行人 |
| 标准审计字段 | |

### ErpInvDrpLine（DRP 明细行）

| 字段 | 含义 |
|------|------|
| id/planId/lineNo/orgId | 标准 |
| materialId | 物料 |
| warehouseId | 目标仓库（需要补货的仓库） |
| sourceWarehouseId | 来源仓库（调出仓库，仓间调拨时） |
| replenishmentType | dict `erp-inv/drp-replenishment-type`：TRANSFER（仓间调拨）/ PURCHASE（向供应商采购） |
| currentStock | 当前库存量 |
| allocatedQty | 已分配量 |
| onOrderQty | 在途/在单量 |
| forecastDemand | 预测需求量（期间内） |
| safetyStock | 安全库存 |
| netRequirement | 净需求（= safetyStock + forecastDemand - currentStock + allocatedQty - onOrderQty，<0 时不补货） |
| suggestedQty | 建议补货量（净需求向上取整到包装倍数） |
| approvedQty | 批准补货量（人工调整后） |
| orderBillType/orderBillCode | 生成的补货单（TransferOrder 或 PurchaseOrder，已执行后回写） |
| status | dict `erp-inv/drp-line-status`：SUGGESTED / APPROVED / ORDERED / CANCELLED |
| 标准审计字段 | |

### ErpInvDrpParameter（仓库补货参数）

| 字段 | 含义 |
|------|------|
| id/warehouseId/materialId/orgId | 标准 |
| safetyStock | 安全库存 |
| replenishmentLeadTime | 补货提前期（天） |
| orderMultiple | 包装/订货倍数 |
| preferredSourceWarehouseId | 首选调出仓库 |
| preferredSupplierId | 首选供应商（采购类型时） |
| replenishmentMethod | dict：MIN_MAX（最小-最大）/ PERIODIC（定期）/ LOT_FOR_LOT（按需） |
| minStockLevel | 最低库存（MIN_MAX 时） |
| maxStockLevel | 最高库存（MIN_MAX 时） |
| reviewPeriodDays | 审视周期天数（PERIODIC 时） |

## 业务规则

1. **净需求计算**：`netRequirement = max(0, safetyStock + forecastDemand - currentStock + allocatedQty - onOrderQty)`。结果为 0 或负时表示库存充足，不产生补货建议。
2. **补货类型决策**：若 `warehouse.distributionCenterId` 存在（有上级分销中心），优先走仓间调拨（sourceWarehouseId = distributionCenterId）；否则走采购（supplierId = preferredSupplierId）。
3. **补货量调整**：netRequirement 向上取整到 `orderMultiple` 倍数，生成 `suggestedQty`。
4. **与 MRP 的关系**：DRP 运行在 MRP 之前（DRP 补货采购单的到货时间作为 MRP 的可供量输入之一）；或并行运行（DRP 管分销网络，MRP 管制造端）。
5. **与库存移动的关系**：DRP 不直接写库存。APPROVED 的补货行生成 TransferOrder 或 PurchaseOrder，走 inventory/purchase 域的标准流程。

## 业财过账

DRP 本身不产生会计凭证。DRP 触发的调拨单走跨法人调拨过账（内部交易），采购单走采购过账。

## 跨域协作

| 对端 | 协作方式 |
|------|---------|
| inventory/transfer | DRP 补货行（TRANSFER 类型）生成调拨单 |
| purchase | DRP 补货行（PURCHASE 类型）生成采购订单 |
| manufacturing/MRP | DRP 的采购到货时间作为 MRP 输入 |
| master-data（Warehouse） | 仓库网络（分销中心/区域仓/前置仓层级） |

## 配置点

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `erp-inv.drp-run-schedule` | — | DRP 定时运行 cron |
| `erp-inv.drp-default-forecast-horizon-days` | 90 | 默认预测展望期 |
| `erp-inv.drp-auto-generate-order` | false | DRP 批准后是否自动生成补货单 |

## 菜单归属

manufacturing 域「供应链计划」分组：DRP 计划、DRP 明细、仓库补货参数。

## 反模式警示

- ⛔ **DRP 与 MRP 混为一谈**——DRP 是分销网络补货（仓库 → 仓库，或 供应商 → 仓库），MRP 是制造物料需求（BOM 展开）。运算逻辑不同，实体分开。
- ⛔ **DRP 直接写库存或采购单**——DRP 输出是"建议"，经人工审批后才转成 TransferOrder/PurchaseOrder。
- ⛔ **净需求计算忽略在途库存**——`onOrderQty`（已下单未到货）必须参与计算，否则会重复补货。

## 证据强度标注

| 证据 | 强度 | 说明 |
|------|------|------|
| 跨域供应链计划（采购+库存+销售集成） | 🟢 | Axelor supplychain（386 Java 文件，本仓最大模块） |
| 多仓库补货逻辑 | 🟢 | ERPNext Material Request | 
| 净需求计算公式 | ⚪ | 标准 DRP 算法（领域常识） |

## 参考

- `docs/analysis/erp-survey/2026-06-30-0000-axelor-open-suite.md` §供应链计划
- `docs/design/manufacturing/mrp.md`（MRP 边界）
- `docs/design/inventory/README.md`（库存移动/调拨）
- `docs/design/purchase/README.md`（采购订单）
