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
