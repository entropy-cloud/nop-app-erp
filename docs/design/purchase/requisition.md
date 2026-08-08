# 采购寻源链：请购→询价→报价

## 目的

设计采购寻源前置流程：请购申请 → 询价 RFQ → 供应商报价 → 比价 → 转采购订单。衔接 `purchase/state-machine.md`（订单→入库→发票→付款）的前端环节。

## 边界

- 本模块负责：采购请购单的创建/审批/合并、RFQ 询价单的生成与发送、供应商报价登记与比价、中标后转采购订单。
- **与 purchase 核心状态机的衔接**：请购审批通过 → 可选直接转订单或先询价；RFQ 报价中标 → 转采购订单。两者都走到 `purchase/state-machine.md` 的订单阶段。
- 本模块不负责：供应商价格清单维护（`supplier-evaluation.md`）；采购订单的后续执行（订单/入库/发票/付款属 `state-machine.md`）。

## 设计依据

> 实体已存在：`ErpPurRequisition` / `ErpPurRfq` / `ErpPurQuotation` / `ErpPurSupplierPriceList`（`module-purchase/model/app-erp-purchase.orm.xml`）。
>
> 参考 Odoo `purchase_requisition` + ERPNext `Material Request` + 管伊佳询价比价模式。

## 实体清单

| 实体 | 表名 | 业务角色 |
|------|------|---------|
| ErpPurRequisition | erp_pur_requisition | 请购单头（申请部门/日期/紧急程度/状态） |
| ErpPurRequisitionLine | erp_pur_requisition_line | 请购明细（物料/数量/需求日期/建议供应商） |
| ErpPurRfq | erp_pur_rfq | 询价单头（关联请购单/报价截止日/状态） |
| ErpPurQuotation | erp_pur_quotation | 供应商报价头（供应商/金额/中标标志） |
| ErpPurQuotationLine | erp_pur_quotation_line | 报价明细（物料/单价/数量/交期） |

## 状态机

### 请购单（ErpPurRequisition）

```
DRAFT（草稿）
  ├─ 提交 → SUBMITTED（已提交审批）
  │          ├─ 审批通过 → APPROVED（已批准）
  │          │               ├─ 转询价 → 生成 ErpPurRfq（走询价比价）
  │          │               └─ 直转订单 → 生成 ErpPurOrder（比价非必需）
  │          └─ 驳回 → REJECTED（可改后重提）
  └─ → CANCELLED（作废）
```

**跨域触发**：APPROVED 后根据采购策略（直接采购/需询价）决定创建 ErpPurRfq 或 ErpPurOrder。

### 询价单（ErpPurRfq）

```
DRAFT（创建询价单，选择供应商列表）
  └─ 发布 → SENT（已发送给供应商）
            ├─ 收齐报价 → BID_CLOSED（报价截止，进入比价）
            │               ├─ 比价完成 → AWARDED（确定中标供应商）
            │               │              └─ 转订单 → 选中的 ErpPurQuotation 生成 ErpPurOrder
            │               └─ → CANCELLED（流标）
            └─ → CANCELLED（取消询价）
```

### 供应商报价（ErpPurQuotation）

```
DRAFT（供应商报价草稿，可录入外部报价）
  └─ SUBMITTED（已提交报价）
       ├─ ACCEPTED（中标——比价选中）
       │    └─ 转订单 → 生成 ErpPurOrder
       └─ REJECTED（未中标）
```

## 业务规则

1. **请购审批后处理**：APPROVED 后根据物料采购策略决定寻源路径——直接采购（已有固定供应商）→ 转订单；需询价 → 创建 RFQ。
2. **RFQ 供应商列表**：从 `ErpMdSupplierApproval`（AVL 准入合格的供应商）+ `ErpPurSupplierPriceList`（有价格记录的供应商）推荐候选。
3. **比价算法**：按 `ErpPurQuotationLine.unitPrice` 总计比较，结合交期/评分 `standing` 综合排序。最低价不一定中标（考虑质量评分 standing）。
4. **中标转订单**：`ErpPurQuotation.isAccepted=true` 后调用 `IErpPurOrderBiz` 创建采购订单，报价行转为订单行。
5. **请购合并**：同一物料多个部门的请购单可在 APPROVED 后合并到一张 RFQ 或订单。

## 跨域协作

| 对端 | 协作内容 |
|------|---------|
| master-data（ErpMdSupplierApproval） | RFQ 供应商候选从 AVL 准入列表获取 |
| purchase/supplier-evaluation | 比价时参考供应商评分 standing |
| purchase/state-machine | 中标转订单 → ErpPurOrder 走订单状态机 |

## 配置点

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `erp-pur.requisition-auto-create-order` | false | 请购审批通过后是否直转订单（跳过询价） |
| `erp-pur.rfq-bid-days-default` | 7 | RFQ 报价截止默认天数 |

## 反模式警示

- ⛔ **请购直通订单跳过审批**——请购与订单是不同实体，各有独立审批流。请购 APPROVED 后才创建订单。
- ⛔ **比价只看价格不看评分**——价格最低的供应商可能质量/交期不达标，必须结合 `supplier-evaluation.md` 的评分 standing。

## 实现注记：请购→转订单多供应商拆分（RC-R1.10）

> 本段为实现注记，不修改需求契约段（L1 `use-cases.md` UC-PUR-08 ⑫ 为真相源）。

- **拆单语义**：`ErpPurRequisitionProcessor.convertToOrder` 按行 `suggestedSupplierId` 分组（`groupLinesBySupplier`，LinkedHashMap 保留行首次出现顺序），每组生成一个 `ErpPurOrder`；单供应商时生成 1 个订单（现状不变）。任一缺失 supplier 的行整次转化拒绝（`ERR_REQ_MIXED_OR_MISSING_SUPPLIER`，不做静默裁剪）。幂等保持：一次调用全量转化，已转化再转 → `ERR_REQ_ALREADY_CONVERTED`（全部订单作废后可再转）。
- **per-supplier 头字段**：`ConvertToOrderRequest.supplierOptions`（`Map<String, SupplierConversionOption>`，key = `String.valueOf(supplierId)`——执行期决策：BeanCopier 对 `Map<Long,...>` 键类型转换不完整，String 键在 Java/JSON 两路径一致）为各供应商订单头覆盖 `warehouseId`/`currencyId`/`deliveryDate`；未提供映射的供应商回退全局 `warehouseId`/`currencyId`（`deliveryDate` 置空，单供应商现状不变）。
- **返回契约**：`ErpPurRequisition__convertToOrder`（`IErpPurRequisitionBiz.convertToOrder`）返回 `List<ErpPurOrder>`；`IErpPurOrderBiz.createFromRequisition` 保持单组实现由 Processor 循环调用（执行期决策，保留单组派生覆盖点，见 `IErpPurOrderBiz` javadoc）。
- **行归属**：订单行 `lineNo` 按组内从 1 重排；单价/税率仍按请购行原始 `lineNo` 经 `lineUnitPrices`/`lineTaxRates` 解析；金额族（amount/taxAmount/amountWithTax）per-order 汇总。
- **successor 注记**：行级到货期拆分（同供应商组内行级 `deliveryDate` 不同）无 ORM 载体，归 successor（当前按组头 `ErpPurOrder.deliveryDate` 承载）；前端 AMIS 拆单展示交互属页面层增强，当前零消费。

## 参考

- `purchase/state-machine.md`（订单→入库→发票→付款）
- `purchase/supplier-evaluation.md`（供应商评分/AVL 准入）
- `purchase/three-way-match.md`（三单匹配）
