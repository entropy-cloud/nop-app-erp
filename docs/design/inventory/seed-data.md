# inventory 域种子数据

> **资产类型**：部署期种子资产（`app-erp-all/src/main/resources/_vfs/_init-data/`），非测试资产（测试共享夹具见 `app-erp-test-data`，边界裁决见 `docs/architecture/seed-data.md` 头部）。
> **落地批次**：M1.1b（plan `docs/plans/2026-09-01-0838-2-m11b-inventory-seed-expansion.md`，2026-09-01）。此前 5 张 inventory 表 seed（stock_move/stock_move_line/stock_balance/cost_layer/stock_ledger，运营域最小连通集）见 `docs/architecture/seed-data.md` 历史批次段。

## 种子数据范围（M1.1b 批次 16 表）

inventory 域 21 实体中 5 表已由历史批次 seed；本批补齐其余 16 表（**7 组主子表 + 2 独立表**），达成**域内全量 seed 覆盖（21/21）**。

| 实体 | CSV 文件 | 行数 | 用例指示 | 必填 FK 闭环 |
|---|---|---|---|---|
| ErpInvBatch（批次台账，独立表） | `erp_inv_batch.csv` | 3 | P+N-TERM | MATERIAL_ID→ErpMdMaterial〔跨域:md·已seed〕、WAREHOUSE_ID→ErpMdWarehouse〔跨域:md·已seed〕 |
| ErpInvSerialNumber（序列号台账，独立表） | `erp_inv_serial_number.csv` | 3 | P+N-TERM | MATERIAL_ID→ErpMdMaterial〔跨域:md·已seed〕 |
| ErpInvReservation（库存预留单头） | `erp_inv_reservation.csv` | 3 | P+N-TERM | —（无必填 FK；RESERVED_FOR_PARTNER_ID 可选指向 ErpMdPartner〔已seed〕） |
| ErpInvReservationLine（预留单行） | `erp_inv_reservation_line.csv` | 3（1/头） | P | RESERVATION_ID→ErpInvReservation〔本批〕、MATERIAL_ID/WAREHOUSE_ID/UOM_ID→ErpMd*〔跨域:md·已seed〕 |
| ErpInvTransferOrder（调拨单头） | `erp_inv_transfer_order.csv` | 3 | P+N-TERM | FROM_WAREHOUSE_ID/TO_WAREHOUSE_ID→ErpMdWarehouse〔跨域:md·已seed〕 |
| ErpInvTransferOrderLine（调拨单行） | `erp_inv_transfer_order_line.csv` | 3（1/头） | P | TRANSFER_ID→ErpInvTransferOrder〔本批〕、MATERIAL_ID/UO_M_ID→ErpMd*〔跨域:md·已seed〕 |
| ErpInvStockTake（盘点单头） | `erp_inv_stock_take.csv` | 3 | P+N-TERM | WAREHOUSE_ID→ErpMdWarehouse〔跨域:md·已seed〕 |
| ErpInvStockTakeLine（盘点单行） | `erp_inv_stock_take_line.csv` | 3（1/头） | P | TAKE_ID→ErpInvStockTake〔本批〕、MATERIAL_ID/UO_M_ID→ErpMd*〔跨域:md·已seed〕 |
| ErpInvPickingOrder（拣货单头） | `erp_inv_picking_order.csv` | 3 | P+N-TERM | WAREHOUSE_ID→ErpMdWarehouse〔跨域:md·已seed〕 |
| ErpInvPickingOrderLine（拣货单行） | `erp_inv_picking_order_line.csv` | 3（1/头） | P | PICKING_ID→ErpInvPickingOrder〔本批〕、MATERIAL_ID/UO_M_ID→ErpMd*〔跨域:md·已seed〕 |
| ErpInvOwnershipTransfer（所有权转移单头） | `erp_inv_ownership_transfer.csv` | 3 | P+N-TERM | PARTNER_ID→ErpMdPartner〔跨域:md·已seed〕、WAREHOUSE_ID→ErpMdWarehouse〔已seed〕、SOURCE_LOC_ID/DEST_LOC_ID→ErpMdLocation〔已seed〕 |
| ErpInvOwnershipTransferLine（所有权转移单行） | `erp_inv_ownership_transfer_line.csv` | 3（1/头） | P | TRANSFER_ID→ErpInvOwnershipTransfer〔本批〕、MATERIAL_ID→ErpMdMaterial〔已seed〕 |
| ErpInvCostAdjust（成本调整单头） | `erp_inv_cost_adjust.csv` | 3 | P+N-TERM | —（无必填 FK；CURRENCY_ID 可选指向 ErpMdCurrency〔已seed〕） |
| ErpInvCostAdjustLine（成本调整单行） | `erp_inv_cost_adjust_line.csv` | 3（1/头） | P | ADJUST_ID→ErpInvCostAdjust〔本批〕、MATERIAL_ID/WAREHOUSE_ID→ErpMd*〔跨域:md·已seed〕 |
| ErpInvLandedCost（到岸成本单头） | `erp_inv_landed_cost.csv` | 3 | P+N-TERM | RECEIVE_ID→ErpPurReceive 表 id=1（ORM 无 `<to-one>` 关系、BIGINT 必填列，值指向既有 seed `PRCV-2026-001`）；其余 FK 可选指向〔已seed〕 |
| ErpInvLandedCostLine（到岸成本行） | `erp_inv_landed_cost_line.csv` | 3（1/头） | P | LANDED_COST_ID→ErpInvLandedCost〔本批〕；AP_PARTNER_ID 可选指向 ErpMdPartner〔已seed〕 |

## FK 闭环图

```
[跨域:md·已seed] erp_md_material(1..4)   ──MATERIAL_ID──▶ batch / serial_number / 各行表
[跨域:md·已seed] erp_md_warehouse(1..2)  ──WAREHOUSE_ID──▶ batch / serial_number / 各行表 / 调拨 from+to / 盘点 / 拣货 / 所有权转移
[跨域:md·已seed] erp_md_location(1..2)   ──LOCATION_ID/SOURCE_LOC_ID/DEST_LOC_ID──▶ serial_number / 预留行 / 盘点行 / 拣货行（源库位）/ 所有权转移（source=dest）
[跨域:md·已seed] erp_md_uom(1..4)        ──UOM_ID/UO_M_ID──▶ 预留行 / 调拨行 / 盘点行 / 拣货行（注意：预留行列名为 UOM_ID，其余三行表为 UO_M_ID）
[跨域:md·已seed] erp_md_partner(1..5)    ──PARTNER_ID/RESERVED_FOR_PARTNER_ID/AP_PARTNER_ID──▶ 所有权转移 / 预留头 / 到岸成本行
[跨域:md·已seed] erp_md_currency(1)      ──CURRENCY_ID──▶ 成本调整头+行 / 到岸成本头 / 所有权转移头
[已seed] erp_pur_receive(1 PRCV-2026-001) ─RECEIVE_ID（必填 BIGINT 软引用，ORM 无 to-one）─▶ 到岸成本头
[已seed] erp_sal_delivery(1 SDLV-2026-001) / erp_sal_order(1 SO-2026-001) / erp_mfg_work_order(1 WO-2026-001) ─关联单据号（自由文本列）─▶ 拣货 / 预留 / 拣货行
[本批] erp_inv_reservation(1..3)       ─RESERVATION_ID─▶ erp_inv_reservation_line
[本批] erp_inv_transfer_order(1..3)    ─TRANSFER_ID───▶ erp_inv_transfer_order_line
[本批] erp_inv_stock_take(1..3)        ─TAKE_ID───────▶ erp_inv_stock_take_line
[本批] erp_inv_picking_order(1..3)     ─PICKING_ID────▶ erp_inv_picking_order_line
[本批] erp_inv_ownership_transfer(1..3)─TRANSFER_ID───▶ erp_inv_ownership_transfer_line
[本批] erp_inv_cost_adjust(1..3)       ─ADJUST_ID─────▶ erp_inv_cost_adjust_line
[本批] erp_inv_landed_cost(1..3)       ─LANDED_COST_ID─▶ erp_inv_landed_cost_line
```

全部必填 FK 落在〔已seed〕∪〔本批〕集合内，零悬空（`TestErpSeedDataIntegrity` 引用完整性门禁背书；`landed_cost.receiveId` 因 ORM 无 `<to-one>` 关系天然不在门禁扫描面，值仍指向已 seed `erp_pur_receive` 行）。

## 与既有 5 表 seed 的衔接（语义一致性约束）

- **物料/仓库维度值复用既有值域**：物料 id 1..4、仓库 id 1(WH-MAIN)/2(WH-RAW)、库位 id 1(主仓库 A 区)/2(原料仓 B 区)、组织 id 2、币种 id 1、SKU id = 物料 id（既有约定）、UoM id 1(PCS)/2(KG)/3(M)/4(BOX)。
- **账实一致**：批次台账行 1/2 即既有 `erp_inv_stock_balance` 两行（物料3@仓2 在库100 / 物料1@仓1 在库80）的批次化表达（TOTAL=AVAILABLE=既有在库量）；盘点单行 BOOK_QUANTITY 逐仓取既有 balance 值（100/80）；预留/拣货/调拨/所有权转移行数量 ≤ 对应物料+仓库既有在库量——演示数据不自相矛盾（盘盈盘亏不指向不存在的库存维度）。
- **批号对齐**：本批各单据行 BATCH_NO 复用批次台账 `LOT-20260703-001` / `LOT-20260630-002`（与既有 cost_layer 两行的入库日期 2026-07-03 / 2026-06-30 同源）。
- **过账语义**：本批所有携带 `POSTED` 列的单据头统一 `POSTED=false`（对齐运营域「posted=false」裁决，见 `docs/architecture/seed-data.md` 运营域段——运营域过账产物未 seed）。
- **业务日期**：全部落在 2026-07 内（与既有 inventory seed 及看板确定性日期窗一致）。

## 用例指示编码与 negative 行语义

编码定义见 `docs/architecture/seed-data.md`「270 个缺 seed 实体最小可用数据集规格表」通用约定 5：

- **P（最小正例行）**：全部 16 表均有（每表前 2 行；行表 1 行/头随头）。
- **N-TERM（终态行）**：每表第 3 行——单据头 `DOC_STATUS=CANCELLED`（move-status / ownership-transfer-status / picking-status 字典终态）或台账 `STATUS` 终态（批次 `EXPIRED` 可用为 0 / 序列号 `OUT` 已出库 / 预留 `CANCELLED`），供单据状态非法迁移与库存动作守卫负路径消费（如 `ErpInvTransferOrderStateMachine` / `ErpInvStockTakeStateMachine` / `ErpInvCostAdjustStateMachine` / `ErpInvLandedCostStateMachine` / `ErpInvOwnershipTransferStateMachine` 的非法迁移守卫族）。
- **N-DIS（禁用行）**：本批 16 表无 enabled/isActive 列，未设。

## 状态口径

- 正例单据头默认 `DOC_STATUS=CONFIRMED + APPROVE_STATUS=APPROVED + POSTED=false`（确认待过账——运营域过账产物未 seed，DONE 态会隐含已执行移动而与既有 stock_move 只有 2 行采购入库/期初相矛盾；`erp-inv/move-status` 字典 = DRAFT/CONFIRMED/DONE/CANCELLED）；调拨单行 2 与到岸成本单 P 行为 `DRAFT + UNSUBMITTED`（草稿态，见下方中立性裁决）。
- 预留单 `STATUS=OPEN` / `PARTIALLY_CONSUMED`（预留数量 ≤ 既有在库，已消耗 ≤ 预留）。
- 拣货单 `DOC_STATUS=PICKING`/`PICKED`（已拣 ≤ 应拣）；所有权转移 SOURCE_LOC_ID = DEST_LOC_ID（法权变更物理位置不变，见 `docs/design/inventory/consignment.md`）。

### 与既有测试流的中立性裁决（2026-09-01 执行期）

新 seed 行不得改变既有集成用例锚定的业务行为，两处状态设计据此裁决：

1. **调拨单方向中立（C20a DRP 净需求）**：`DrpDemandAggregator.inboundTransferQty` 聚合「toWarehouseId=目标仓 且 docStatus≠CANCELLED」的调拨行进 `onOrderQty` 减项。本批 2 张 P 调拨单均取**物料 3（原料 X，既有在库 100@仓2）仓 2→仓 1** 方向（合计 80 ≤ 在库），不产出任何（物料 1, 仓 2）/（物料 2, 仓 1）进仓对——`TestErpC20aDrpNetRequirementRelease` 的 `onOrderQty=0`/净需求 90/50 锚点不被改写（执行期实证：物料 1 仓 1→2 CONFIRMED 行曾使 totalReplenishmentQty 140→120）。
2. **到岸成本草稿态中立（C13 期末预检）**：`ErpFinAccountingPeriodProcessor.findUnresolvedLandedCosts` 将「posted=false + approveStatus=APPROVED + businessDate 在结账期间」的到岸成本单列为结账阻断项。本批 P 行取 `DRAFT + UNSUBMITTED`（未进审核即不构成「已审核悬挂」），不进入 `TestErpC13FinPeriodCloseReverse` 的 pre-check 阻断清单；CANCELLED 终态行天然不可见。

## 约定对齐

- 列头 = DB 列名大写下划线；省略审计列（delVersion/version/createdBy/createTime/updatedBy/updateTime）；ISO 日期（TIMESTAMP 列 = `yyyy-MM-dd HH:mm:ss`）；小写布尔；ID < 100000（`zz-sequence-advance.sql` 序列推进值域约束）。
- 新增/修改本域 seed 时同步履行 `docs/architecture/seed-data.md`「快照重录义务」与 `TestErpSeedDataIntegrity` 门禁（基线常量随批更新协议）。
