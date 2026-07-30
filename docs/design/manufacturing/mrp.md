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
ErpMfgPlannedOrder / ErpMfgMrpPlanLine
  │
  ├─ orderType=MANUFACTURING → 一键生成 ErpMfgWorkOrder（工单）
  ├─ orderType=PURCHASE      → 一键生成 ErpPurOrder（采购订单）
  └─ 释放后：行级 isFirmed=true（MrpReleaseService.markFirmed）；全部行 firmed 后计划头 → FIRMED（advancePlanToFirmedIfComplete），不再参与下次 MRP
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
| `erp-mfg.forecast-consume-enabled` | true | 是否消费 APPROVED 预测行作为 FORECAST 需求来源；false 时不消费 |

## 实现约定

本期实现相对上方设计描述的已知偏离（均为 Non-Goal 或必要实现细节）：

- **FORECAST 需求来源**：实体 `ErpMfgForecast`/`ErpMfgForecastLine`（制造域）；`DemandAggregator.collectForecastDemands` 仅消费头 `status=APPROVED` 且区间与计划期相交的预测行，按物料聚合 `forecastQty`，`demandSource=FORECAST`，warehouseId 维度由 MRP 忽略（产品级需求）；config-gated `erp-mfg.forecast-consume-enabled`（默认 true）。状态机 `DRAFT→APPROVED`（approve）/ `→CANCELLED`（cancel）经 `ErpMfgForecastBizModel` 实现；`CONSUMED` 状态值预留但本期不自动迁移（Deferred，触发条件：预测消费后状态回写需求上线）。
- **MRP 计划 CANCELLED 死状态（预留/Deferred）**：`erp-mfg/mrp-status` 字典 5 值含 `CANCELLED`，但 `MrpEngine` 仅 setStatus `RUNNING`/`COMPLETED`、`MrpReleaseService` 仅 setStatus `FIRMED`，`ErpMfgMrpPlanBizModel` 仅 `runMrp`，**无 cancelPlan/cancelMrp mutation，零 CANCELLED writer**（grep `MRP_STATUS_CANCELLED|cancelPlan|cancelMrp` 仅命中常量声明）。主路径 DRAFT→RUNNING→COMPLETED→FIRMED 完整覆盖。处置：采纳 Decision A（保留 dict 值为预留 + owner doc 标注 Deferred，对齐 forecast CONSUMED 先例），不从 ORM 删除。**Successor 触发条件**：MRP 计划取消需求上线时，实现 cancelPlan BizMutation + setStatus writer + 状态迁移守卫（须防 RUNNING 运算中取消的并发与已 FIRMED 释放单的回滚）。
- **预测 CONSUMED 死状态（预留/Deferred）**：`erp-mfg/forecast-status` 字典含 `CONSUMED`，`ErpMfgForecastBizModel` 仅 setStatus `APPROVED`/`CANCELLED`，`CONSUMED` 仅作 cancel 守卫只读引用（拒绝从 CONSUMED 取消）。`CONSUMED` Deferred 标注已完整存在于本文件上方 FORECAST 条目 + `ErpMfgForecastBizModel.java:21` Javadoc。主路径 DRAFT→APPROVED + →CANCELLED 完整覆盖。
- **lot sizing 简化**：上方「固定批量/最小订货量/最大订货量」对应物料级 fixedLotSize/minOrderQty/maxOrderQty 列在 ORM 不存在。本期 lot-for-lot 为主 + 全局配置 `erp-mfg.default-lot-size`（>0 时按倍数取整）。触发条件：物料级批量精细化需求时（须 ask-first 加列）。
- **低层码**：上方「低层编码」经 BomExpander DFS 层级标记实现（同物料取最低层级展开基准），不预计算物化 lowLevelCode 列（ORM 无此列）。
- **可用量来源**：上方「在途采购/在制工单」未实时跨域汇总（purchase/manufacturing 复杂查询）。本期可用量 = `ErpInvStockBalance.availableQuantity`（既有预计算列 = total − reserved − locked；null 时回退计算）；在途/在制以 `ErpMfgMrpPlanLine.scheduledReceipt` 列承载（粗估，计划员录入或后续从在途汇总）。
- **scrapRate**：上方「净需求 × (1 + scrapRate)」本期按标准用量（scrapRate 为 VARCHAR，Non-Goal）。触发条件：损耗精细化核算需求时。
- **委外建议释放**：orderType=SUBCONTRACT_REQUEST 字典存在但委外流程独立面，本期不支持释放。触发条件：委外加工落地时。
- **需求时界 / CRP / AUTO_SCHEDULED**：本期不区分需求时界、不做产能校验（CRP 见 2.8 独立面）、仅 MANUAL 触发。
- **建议单释放耦合度**：上方「一键转为采购订单/生产工单」本期实现为释放直接持久化目标域实体（`ErpPurOrder`/`ErpPurOrderLine`、`ErpMfgWorkOrder`）——IErpPurOrderBiz/IErpMfgWorkOrderBiz 仅订单头级通用 CRUD 无 purpose-built `createFromMrpLine` 方法，故走 service-helper 范式直接落库（仅写 MRP 已知字段：物料/数量/日期/org）。释放分两个 purpose-built 方法（`releasePurchaseRequest` 须 supplierId/currencyId 因 ErpPurOrder ORM 必填；`releaseWorkRequest` 仅需 planLineId）。残留：生成的采购单单价/金额=0、币种由参数提供，须采购员后续补录。
- **释放状态措辞更正：RELEASED → FIRMED/isFirmed**：上方「建议单释放」ASCII 图原文声明「释放后建议单状态标记为 RELEASED」——此为设计与实现的措辞漂移，**`erp-mfg/mrp-status` 字典无 RELEASED 值**，RELEASED 为幻影状态。实际机制：释放后行级 `ErpMfgMrpPlanLine.isFirmed=true`（`MrpReleaseService.markFirmed:129-133` 同步写 `convertedBillCode`），当该计划全部行均 `isFirmed=true` 时，`advancePlanToFirmedIfComplete:218-236` 将计划头 `ErpMfgMrpPlan.status` 置为 `FIRMED`。即「释放 = 行布尔 isFirmed + 计划头状态 FIRMED」两级，非单态 RELEASED。已按此修正上方 ASCII 图措辞。

### CRM 销售预测 vs 运营需求预测的关系

CRM 域 `ErpCrmForecast`/`ErpCrmForecastLine`（金额/分类/owner 维度，COMMIT/UPSIDE/BEST_CASE 分类）与本域 `ErpMfgForecast`（产品×数量×时间桶运营预测）**语义不同**，本期两者独立维护：

- CRM 预测服务于销售目标管理/线索评分，单位为金额；
- 运营预测（ErpMfgForecast）服务于 MRP/DRP 单位级需求计算，单位为数量。

**CRM 金额预测 → 运营数量预测的自动 disaggregation 本期不实现**（Decision：金额→数量分解依赖售价策略 + 多币种 + 折扣，误差大；归后继）。触发条件：CRM 金额预测驱动运营数量需求的产品决策落地时（successor）。

### 仿真引擎关系

本节单次 MRP 引擎（`MrpEngine` / `DemandAggregator`）不变；MRP/DRP 多场景仿真包装经独立 owner doc [`simulation-engine.md`](simulation-engine.md) 承载：

- **场景-版本模型**：`ErpMfgMrpScenario`（场景）1:N `ErpMfgMrpScenarioVersion`（版本，引用 COMPUTED `ErpMfgMrpPlan` 快照）+ `ErpMfgMrpScenarioParam`（参数变体覆盖）
- **覆盖机制（Decision E2 fork）**：fork `SimulationMrpEngine` 复用 MrpEngine 算法但替换全局/主数据读取为场景覆盖值，**单次 MRP 路径零触及**（既有 200+ manufacturing 测试零回归）
- **3 参数覆盖**：`LEAD_TIME`（提前期偏移）/ `LOT_SIZE`（批量）/ `SAFETY_STOCK`（安全库存），未覆盖时回退全局配置/主数据（Decision B 回退顺序）
- **结果对比**：`compareVersions(versionIdA, versionIdB)` `@BizQuery` 返回 4 维 diff DTO（净需求差/建议量差/缺料物料集差/总采购额差），不持久化（Decision C）
- **转正式计划**：`promoteToFormalPlan(versionId)` 生成新 DRAFT `ErpMfgMrpPlan`，原版本 ARCHIVED，转正后走既有单次释放路径（Decision D）
- **config-gated**：`erp-mfg.simulation-enabled` 默认 false，门控仿真入口（不保护单次 MRP 路径回归——E2 零触及已保证）

DRP 同构对应物（`ErpDrpScenario`/`ErpDrpScenarioVersion`/`ErpDrpScenarioParam` + `SimulationDrpEngine` + 2 维 diff 补货量差/安全库存差）见 `simulation-engine.md §DRP 对应物`。
