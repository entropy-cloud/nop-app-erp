# MRP（物料需求计划）

## 目的

MRP（Material Requirements Planning）根据独立需求（销售订单、预测、安全库存）计算物料需求，通过 BOM 展开生成采购/生产建议。MRP 解决"需要什么、需要多少、什么时候需要"三个问题。

## 边界

- 本模块负责：需求来源整合、BOM 多级展开、净需求计算、建议采购单/生产单生成。
- 本模块不负责：产能计划（CRP，后续版本）；实际采购/生产执行（由 purchase/manufacturing 域执行建议单）。
- ORM 实体见 `model/app-erp-manufacturing.orm.xml`（ErpMfgDemand、ErpMfgMrpResult、ErpMfgPlannedOrder 等）。

## MRP 流程

```
需求来源
  ├─ 销售订单（ErpSalSalesOrder）
  ├─ 销售预测（ErpMfgForecast）
  ├─ 安全库存补货（ErpMdProduct.safetyStock）
  └─ 独立需求单（ErpMfgDemand，手动创建）
       │
       ▼ (MRP 运行)
  需求整合 → 合并同物料同期的毛需求
       │
       ▼
  库存可用量计算
    ┌─ 现有库存（onHandQty）
    ├─ 已预留量（reservedQty）
    ├─ 在途采购（openPurchaseQty）
    ├─ 在制工单（openWorkOrderQty）
    └─ 已分配量（allocatedQty）
    = 可用量 = onHandQty − reservedQty + openPurchaseQty + openWorkOrderQty
       │
       ▼
  BOM 多级展开（仅制造件，采购件不展开）
    ├─ 上层毛需求 × BOM 数量 = 本层毛需求
    ├─ 净需求 = 毛需求 − 可用量（负值归零）
    └─ 考虑损耗率（scrapRate）: 净需求 × (1 + scrapRate)
       │
       ▼
  按期分单（lot sizing）
    ├─ 固定批量（fixedLotSize）— 按倍数取整
    ├─ 按需批量（lot-for-lot）— 净需求即建议量
    └─ 最小订货量（minOrderQty）/ 最大订货量（maxOrderQty）约束
       │
       ▼
  生成建议单
    ├─ 制造件 → ErpMfgPlannedOrder（orderType=MANUFACTURING）
    ├─ 采购件 → ErpMfgPlannedOrder（orderType=PURCHASE）
    │            └─ 可一键转为采购订单 / 生产工单
    └─ 记录需求来源追溯（pegging: 建议单 → 需求来源行）
```

## 关键业务规则

1. **需求时界**：MRP 计算时区分"已锁定需求"（近期销售订单）和"可调整需求"（远期预测），锁定需求优先满足。
2. **提前期偏移**：采购件按 supplier lead time、制造件按 BOM routing 累计提前期，计算需求下达日期。
3. **低层编码**：同一物料若出现在多个 BOM 层级，取其最低层级编码作为 MRP 展开基准（避免重复计算）。
4. **Pegging 追溯**：每条建议单记录其需求来源行（sourceBillType/sourceBillCode/sourceLineNo），支持多级追溯。
5. **MRP 范围**：按公司（orgId）独立运行，不跨公司合并需求。

## 建议单释放

```
ErpMfgPlannedOrder
  │
  ├─ orderType=MANUFACTURING → 一键生成 ErpMfgWorkOrder（工单）
  ├─ orderType=PURCHASE      → 一键生成 ErpPurPurchaseOrder（采购订单）
  └─ 释放后建议单状态标记为 RELEASED，不再参与下次 MRP
```

## 配置选项

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| MRP 运行模式 | MANUAL（手动触发） | MANUAL/AUTO_SCHEDULED |
| 需求时界天数 | 30 | 锁定需求的时间窗口 |
| 安全库存包含在净需求中 | true | false 时仅告警不参与计算 |
| 损耗率来源 | BOM 子件行 | 物料主数据/类别默认值 |
| `erp-mfg.default-lot-size` | 0（lot-for-lot） | >0 时按倍数向上取整（本期全局配置；物料级 fixedLotSize/minOrderQty/maxOrderQty 列不存在，Non-Goal） |
| `erp-mfg.mfg-leadtime-days-per-routing-hour` | 0.125（8h/天） | 制造件 BOM 工序累计工时换算提前期天数 |

## 实现偏离补注（2026-07-03，plan 2026-07-02-2237-2）

本期实现相对上方设计描述的已知偏离（均为计划内 Non-Goal 或必要实现细节，已记录于计划 Task Route Decision / Deferred）：

- **FORECAST 需求来源**：上方「销售预测（ErpMfgForecast）」ORM 无此实体，本期不支持。需求整合仅销售订单 + 安全库存 + 手工需求。`demandSource` 字典含 FORECAST=20 码值但无数据入口。触发条件：预测实体建模落地时（successor）。
- **lot sizing 简化**：上方「固定批量/最小订货量/最大订货量」对应物料级 fixedLotSize/minOrderQty/maxOrderQty 列在 ORM 不存在。本期 lot-for-lot 为主 + 全局配置 `erp-mfg.default-lot-size`（>0 时按倍数取整）。触发条件：物料级批量精细化需求时（须 ask-first 加列）。
- **低层码**：上方「低层编码」经 BomExpander DFS 层级标记实现（同物料取最低层级展开基准），不预计算物化 lowLevelCode 列（ORM 无此列）。
- **可用量来源**：上方「在途采购/在制工单」未实时跨域汇总（purchase/manufacturing 复杂查询）。本期可用量 = `ErpInvStockBalance.availableQuantity`（既有预计算列 = total − reserved − locked；null 时回退计算）；在途/在制以 `ErpMfgMrpPlanLine.scheduledReceipt` 列承载（粗估，计划员录入或后续从在途汇总）。
- **scrapRate**：上方「净需求 × (1 + scrapRate)」本期按标准用量（scrapRate 为 VARCHAR，1538-2 Non-Goal）。触发条件：损耗精细化核算需求时。
- **委外建议释放**：orderType=SUBCONTRACT_REQUEST 字典存在但委外流程独立面，本期不支持释放。触发条件：委外加工落地时。
- **需求时界 / CRP / AUTO_SCHEDULED**：本期不区分需求时界、不做产能校验（CRP 见 2.8 独立面）、仅 MANUAL 触发。
- **建议单释放耦合度**：上方「一键转为采购订单/生产工单」本期实现为释放直接持久化目标域实体（`ErpPurOrder`/`ErpPurOrderLine`、`ErpMfgWorkOrder`）——IErpPurOrderBiz/IErpMfgWorkOrderBiz 仅订单头级通用 CRUD 无 purpose-built `createFromMrpLine` 方法，故走 service-helper 范式直接落库（仅写 MRP 已知字段：物料/数量/日期/org）。释放分两个 purpose-built 方法（`releasePurchaseRequest` 须 supplierId/currencyId 因 ErpPurOrder ORM 必填；`releaseWorkRequest` 仅需 planLineId）。残留：生成的采购单单价/金额=0、币种由参数提供，须采购员后续补录。
