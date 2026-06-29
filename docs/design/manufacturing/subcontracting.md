# 外协加工（Subcontracting）

## 目的

外协加工是将部分工序或整个工单委托外部供应商完成的业务模式。核心难点在于**提供物料（Provided Material）**的库存转移与管理，以及外协费用的财务结算。

## 边界

- 本模块负责：外协工序定义、提供物料出库、外协入库、外协费用归集。
- 本模块不负责：供应商主数据（master-data 域）；采购合同/订单的商务条款（purchase 域）。
- ORM 实体见 `model/app-erp-manufacturing.orm.xml`（ErpMfgSubcontractingOrder、ErpMfgSubcontractingProvidedMaterial 等）。

## 流程

```
工单外协（subcontracted operation in routing）
       │
       ▼
创建外协采购订单（ErpPurPurchaseOrder，orderType=SUBCONTRACT）
  └─ 关联工单（sourceBillType=MANUFACTURING + sourceBillCode=workOrderCode）
       │
       ▼
提供物料出库
  ├─ 按 BOM 子件清单 × 工单数量计算需提供量
  ├─ 生成外协出库移动单（operationType=SUBCONTRACT_OUT，库存转至外协虚拟仓）
  └─ 记录至 ErpMfgSubcontractingProvidedMaterial
       │
       ▼
供应商加工
  ├─ 接收成品时：生成外协入库移动单（operationType=SUBCONTRACT_IN）
  ├─ 同时核销提供物料消耗（实际耗用量 = BOM 标准量 ± 差异）
  └─ 剩余物料退回（SUBCONTRACT_RETURN）或留供应商处（下次使用）
       │
       ▼
费用结算
  ├─ 外协发票（purchase 域）→ 过账为制造费用
  └─ 费用归集到工单成本（ErpMfgWorkOrder.costSubcontract）
```

## 关键业务规则

1. **提供物料超耗处理**：供应商实际消耗超出 BOM 标准量时，超出部分在结算时扣减供应商款项或由我方承担（按协议）。
2. **外协虚拟仓**：提供物料出库至"外协在途"虚拟仓库（warehouseType=SUBCONTRACT），不计入可用库存但属于公司资产。
3. **剩余物料**：可选择退回实物入库（SUBCONTRACT_RETURN）或留在供应商处（下次外协时优先消耗）。
4. **外协费用入工单成本**：外协工序的加工费归入对应工单的制造费用科目。
5. **检验关联**：外协入库可触发来料检验（quality 域），与外协采购入库联动。

## 跨域协作

| 协作场景 | 对端域 | 协作方式 |
|----------|--------|----------|
| 提供物料出库 | inventory | SUBCONTRACT_OUT 移动单出库至外协虚拟仓 |
| 成品入库 | inventory | SUBCONTRACT_IN 移动单入库 |
| 外协采购单 | purchase | orderType=SUBCONTRACT 的采购订单 |
| 费用凭证 | finance | 外协费用过账至制造费用科目 |

## 配置选项

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| 提供物料方式 | BOM_QUANTITY | BOM_QUANTITY（按 BOM 标准量）/ WORK_ORDER_QUANTITY（按工单需求量） |
| 剩余物料处理 | RETURN | RETURN（退回）/ KEEP（留供应商处，下次优先消耗） |
| 外协虚拟仓 | 系统预置 | 每个组织默认一个外协虚拟仓 |
