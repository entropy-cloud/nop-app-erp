# 单据模型设计

## 目的

说明 ERP 系统中业务单据的统一模型设计，包括双维度类型设计（docType + bizType）、进销存三单链（order→trade→refund）、单据头行结构、单据编号规则等。

本文件是 `jsh-erp.md` 和 `ruoyi-erp.md` 调研结论的落地设计。

## 设计背景

### 调研发现

从管伊佳和若依 ERP 的调研中发现：

| 发现 | 说明 | 参考项目 |
|------|------|----------|
| 双维度类型设计简洁 | type + subType 单表设计，采购和销售靠同一张表区分 | 管伊佳 |
| 进销存三单链清晰 | order→trade→refund 闭环 | 若依 |
| 单据四级拆分 | 头 + Detail + DetailBundle + DetailLot | 星云 |
| 状态双维度灵活 | checked_status + stock_status 分离 | 若依 |
| 单据唯一编号约束 | UNIQUE INDEX idx_bill_no(doc_no) | 若依 |

### 核心原则

1. **双维度类型**：docType（单据大类）+ bizType（业务细分）
2. **三单链闭环**：订单 → 成交/出入库 → 退货
3. **头行分离**：单据头承载单据级信息，单据行承载明细级信息
4. **编号唯一**：单据编号全局唯一，按单据类型分类编号

## 双维度类型设计

### 类型定义

| 维度 | 说明 | 示例值 |
|------|------|--------|
| docType | 单据大类（入库/出库/其它） | INPUT（入库）、OUTPUT（出库）、OTHER（其它） |
| bizType | 业务细分（采购/销售/调拨/盘点等） | PURCHASE（采购入库）、SALES（销售出库）、TRANSFER（调拨）、TAKE_STOCK（盘点） |

### 类型组合

| docType | bizType | 单据类型说明 |
|---------|---------|-------------|
| INPUT | PURCHASE | 采购入库 |
| INPUT | PURCHASE_RETURN | 销售退货入库 |
| INPUT | TRANSFER_IN | 调拨入库 |
| INPUT | TAKE_STOCK_GAIN | 盘点盘盈 |
| INPUT | MANUFACTURING | 生产完工入库 |
| OUTPUT | SALES | 销售出库 |
| OUTPUT | PURCHASE_RETURN | 采购退货出库 |
| OUTPUT | TRANSFER_OUT | 调拨出库 |
| OUTPUT | TAKE_STOCK_LOSS | 盘点盘亏 |
| OUTPUT | MANUFACTURING_PICK | 生产领料出库 |
| OTHER | TRANSFER | 调拨单（双向） |
| OTHER | TAKE_STOCK | 盘点单 |
| OTHER | ADJUST | 库存调整 |

### 类型字典定义

```xml
<!-- docType 字典 -->
<dict name="erp/doc-type">
    <option value="INPUT" label="入库"/>
    <option value="OUTPUT" label="出库"/>
    <option value="OTHER" label="其它"/>
</dict>

<!-- bizType 字典 -->
<dict name="erp/biz-type">
    <option value="PURCHASE" label="采购入库"/>
    <option value="SALES" label="销售出库"/>
    <option value="PURCHASE_RETURN" label="采购退货"/>
    <option value="SALES_RETURN" label="销售退货"/>
    <option value="TRANSFER" label="调拨"/>
    <option value="TRANSFER_IN" label="调拨入库"/>
    <option value="TRANSFER_OUT" label="调拨出库"/>
    <option value="TAKE_STOCK" label="盘点"/>
    <option value="TAKE_STOCK_GAIN" label="盘点盘盈"/>
    <option value="TAKE_STOCK_LOSS" label="盘点盘亏"/>
    <option value="MANUFACTURING" label="生产入库"/>
    <option value="MANUFACTURING_PICK" label="生产领料"/>
    <option value="ADJUST" label="库存调整"/>
</dict>
```

### 类型驱动逻辑

单据类型驱动以下逻辑：

| 驱动项 | 说明 |
|--------|------|
| 默认库位 | 按 docType 确定默认来源/目的库位 |
| 科目映射 | 按 bizType 确定凭证科目 |
| 单据编号前缀 | 按 bizType 确定编号前缀 |
| 审批流程 | 按 bizType 确定审批流程 |
| 关联单据类型 | 按 bizType 确定可关联的上游单据类型 |

## 进销存三单链

### 三单链结构

```
进销存三单链
        │
        ├─► 订单层（Order）
        │      ├─ 采购订单（PurOrder）
        │      ├─ 销售订单（SalOrder）
        │      └─ 订单行（OrderLine）
        │
        ├─► 成交/出入库层（Trade/StockMove）
        │      ├─ 采购入库单（PurReceive）
        │      ├─ 销售出库单（SalDelivery）
        │      ├─ 成交行（TradeLine）
        │      └─ 关联订单行
        │
        └─► 退货层（Refund/Return）
               ├─ 采购退货单（PurReturn）
               ├─ 销售退货单（SalReturn）
               ├─ 退货行（RefundLine）
               └─ 关联成交行
```

### 三单链关系

```
采购三单链：
  PurOrder → PurReceive → PurReturn
     │           │            │
     │           │            └─ 关联 PurReceiveLine
     │           └─ 关联 PurOrderLine
     └─ 订单行

销售三单链：
  SalOrder → SalDelivery → SalReturn
     │           │            │
     │           │            └─ 关联 SalDeliveryLine
     │           └─ 关联 SalOrderLine
     └─ 订单行
```

### 单据关联字段

所有单据行必须包含关联追溯字段：

| 字段 | 说明 |
|------|------|
| sourceBillType | 来源单据类型 |
| sourceBillId | 来源单据头ID |
| sourceBillLineId | 来源单据行ID |

### 三单链闭环示例

```
采购业务闭环示例
        │
        ├─► 采购订单（PO001）
        │      ├─ 订单行1：物料A，数量100
        │      └─ 订单行2：物料B，数量50
        │
        ├─► 采购入库单（RC001）
        │      ├─ 入库行1：物料A，数量80（关联订单行1）
        │      ├─ 入库行2：物料B，数量50（关联订单行2）
        │      └─ 订单未交货量更新：物料A=20，物料B=0
        │
        ├─► 采购发票（PI001）
        │      ├─ 发票行1：物料A，金额800（关联入库行1）
        │      └─ 发票行2：物料B，金额500（关联入库行2）
        │
        ├─► 付款单（PAY001）
        │      ├─ 付款核销发票PI001
        │      └─ 发票状态更新：已付款
        │
        └─► 采购退货单（RT001）
               ├─ 退货行1：物料A，数量10（关联入库行1）
               ├─ 生成反向出库移动单
               ├─ 生成红字发票
               └─ 入库单未退货量更新：物料A=70
```

## 单据头行结构

### 单据头字段

所有单据头必须包含的基础字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| docType | String | 单据大类 |
| bizType | String | 业务细分 |
| docNo | String | 单据编号（唯一） |
| docStatus | String | 业务状态 |
| approveStatus | String | 审批状态 |
| partnerId | Long | 往来单位ID |
| warehouseId | Long | 仓库ID |
| bizDate | Date | 业务日期 |
| totalAmount | Decimal | 总金额 |
| totalQty | Decimal | 总数量 |
| memo | String | 备注 |
| sourceBillType | String | 来源单据类型 |
| sourceBillId | Long | 来源单据ID |
| posted | Boolean | 是否已过账 |
| postedTime | DateTime | 过账时间 |

### 单据行字段

所有单据行必须包含的基础字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| headId | Long | 单据头ID |
| lineNo | Integer | 行号 |
| materialId | Long | 物料ID |
| skuId | Long | SKU ID |
| qty | Decimal | 数量 |
| unitId | Long | 单位ID |
| price | Decimal | 单价（不含税） |
| taxRate | Decimal | 税率 |
| taxAmount | Decimal | 税额 |
| amount | Decimal | 金额（不含税） |
| amountWithTax | Decimal | 金额（含税） |
| warehouseId | Long | 仓库ID |
| locationId | Long | 库位ID |
| batchId | Long | 批次ID |
| sourceBillType | String | 来源单据类型 |
| sourceBillId | Long | 来源单据头ID |
| sourceBillLineId | Long | 来源单据行ID |
| memo | String | 备注 |

### 价税分离字段

参考管伊佳/若依的价税分离设计：

```
价税分离字段关系：
  amount（不含税金额） = qty × price
  taxAmount（税额） = amount × taxRate
  amountWithTax（含税金额） = amount + taxAmount
  
示例：
  qty = 100
  price = 10.00（不含税单价）
  taxRate = 0.13（13%）
  amount = 100 × 10 = 1000.00
  taxAmount = 1000 × 0.13 = 130.00
  amountWithTax = 1000 + 130 = 1130.00
```

## 单据编号规则

### 编号结构

单据编号采用"前缀 + 日期 + 流水号"结构：

```
编号结构：
  前缀（2-4字符） + 日期（YYYYMMDD） + 流水号（4-6位）
  
示例：
  PO202606220001（采购订单）
  RC202606220001（采购入库）
  SI202606220001（销售发票）
```

### 编号前缀配置

| bizType | 编号前缀 | 说明 |
|---------|----------|------|
| PURCHASE_ORDER | PO | 采购订单 |
| PURCHASE_RECEIVE | RC | 采购入库 |
| PURCHASE_INVOICE | PI | 采购发票 |
| PURCHASE_RETURN | PR | 采购退货 |
| SALES_ORDER | SO | 销售订单 |
| SALES_DELIVERY | SD | 销售出库 |
| SALES_INVOICE | SI | 销售发票 |
| SALES_RETURN | SR | 销售退货 |
| TRANSFER | TF | 调拨 |
| TAKE_STOCK | TS | 盘点 |

### 编号生成器

```java
@Component
public class DocNoGenerator {
    @Inject
    private IDocNoSequenceService sequenceService;
    
    /**
     * 生成单据编号
     */
    public String generateDocNo(String bizType, Date bizDate) {
        String prefix = getPrefix(bizType);
        String dateStr = formatDate(bizDate, "yyyyMMdd");
        Long sequence = sequenceService.getNextSequence(bizType, dateStr);
        return prefix + dateStr + String.format("%04d", sequence);
    }
    
    private String getPrefix(String bizType) {
        // 从配置或字典获取前缀
        return DictHelper.getLabel("erp/doc-no-prefix", bizType);
    }
}
```

### 编号唯一约束

```xml
<entity name="ErpPurOrder">
    <column name="docNo" type="String" length="20" mandatory="true"/>
    <index name="idx_doc_no" columns="docNo" unique="true"/>
</entity>
```

## 单据四级拆分

参考星云 ERP 的四级拆分设计：

| 层级 | 说明 | 适用场景 |
|------|------|----------|
| Head | 单据头 | 所有单据 |
| Detail | 单据明细行 | 所有单据 |
| DetailBundle | 组合商品明细 | 组合商品销售 |
| DetailLot | 批次明细 | 批次管理物料 |

### 四级拆分示例

```
销售出库单四级拆分示例
        │
        ├─► SalDeliveryHead
        │      ├─ docNo: SD202606220001
        │      └─ totalAmount: 5000
        │
        ├─► SalDeliveryDetail
        │      ├─ 行1：物料A，数量10
        │      └─ 行2：组合商品B，数量5
        │
        ├─► SalDeliveryDetailBundle
        │      └─ 组合商品B展开：
        │           ├─ 子件1：物料C，数量10
        │           └─ 子件2：物料D，数量15
        │
        └─► SalDeliveryDetailLot
               ├─ 物料A批次：
               │    ├─ 批次1：LOT001，数量6
               │    └─ 批次2：LOT002，数量4
               └─ 物料C批次：
                    └─ 批次1：LOT003，数量10
```

## 单据状态双维度

参考若依 ERP 的状态双维度设计：

| 状态维度 | 说明 | 适用场景 |
|----------|------|----------|
| docStatus | 业务生命周期状态 | 所有单据 |
| approveStatus | 审批状态 | 需审批单据 |
| stockStatus | 库存状态 | 入库/出库单据 |
| paidStatus | 收付款状态 | 发票/收付款单据 |
| postedStatus | 过账状态 | 财务相关单据 |

### 状态双维度示例

```
采购入库单状态双维度：
  docStatus: DRAFT → PREPARED → COMPLETED → CLOSED
  approveStatus: UNSUBMITTED → SUBMITTED → APPROVED
  stockStatus: UNSTOCKED → STOCKED（库存已写入）
  postedStatus: UNPOSTED → POSTED（凭证已生成）

状态约束：
  - stockStatus = STOCKED 要求 docStatus = COMPLETED
  - postedStatus = POSTED 要求 stockStatus = STOCKED
```

## 单据模板配置

### 单据类型模板

每种单据类型可配置模板，预设默认值：

```yaml
# 单据类型模板配置
bizType: PURCHASE_RECEIVE
defaults:
  warehouseId: 1  # 默认仓库
  locationId: 1   # 默认库位
  docNoPrefix: RC
  approvalRequired: true
  approvalFlowCode: pur_receive_approval
  voucherTemplateCode: PUR_RECEIVE
```

### 单据类型服务

```java
@Component
public class DocTypeService {
    @Inject
    private Map<String, IDocTypeConfig> configs; // key = bizType
    
    /**
     * 获取单据类型配置
     */
    public IDocTypeConfig getConfig(String bizType) {
        return configs.get(bizType);
    }
    
    /**
     * 获取默认仓库
     */
    public Long getDefaultWarehouse(String bizType) {
        IDocTypeConfig config = configs.get(bizType);
        return config != null ? config.getDefaultWarehouseId() : null;
    }
}
```

## 单据关联追溯

### 关联追溯字段

所有单据必须包含关联追溯字段：

| 字段 | 说明 | 用途 |
|------|------|------|
| sourceBillType | 来源单据类型 | 追溯上游单据 |
| sourceBillId | 来源单据头ID | 追溯上游单据头 |
| sourceBillLineId | 来源单据行ID | 追溯上游单据行 |

### 关联追溯查询

```java
/**
 * 查询单据的下游单据
 */
public List<Object> findDownstreamBills(String sourceBillType, Long sourceBillId) {
    // 查询所有 sourceBillType = 当前单据类型 且 sourceBillId = 当前单据ID 的单据
    return dao.findAllDownstream(sourceBillType, sourceBillId);
}

/**
 * 查询单据的上游单据
 */
public Object findUpstreamBill(String billType, Long billId) {
    // 查询当前单据的 sourceBillType 和 sourceBillId
    Object bill = dao.findById(billType, billId);
    if (bill.getSourceBillType() == null) return null;
    return dao.findById(bill.getSourceBillType(), bill.getSourceBillId());
}
```

### 三单匹配追溯

采购三单匹配（订单-入库-发票）：

```
三单匹配追溯：
  采购订单 → 采购入库 → 采购发票
     │           │           │
     │           │           └─ sourceBillId = 入库单ID
     │           └─ sourceBillId = 订单ID
     └─ 订单行
  
匹配校验：
  1. 入库数量 ≤ 订单数量
  2. 发票数量 ≤ 入库数量
  3. 发票金额 ≤ 入库金额
```

## 配置项

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `erp.doc.auto-generate-no` | true | 是否自动生成单据编号 |
| `erp.doc.no-sequence-length` | 4 | 编号流水号位数 |
| `erp.doc.approval-required-default` | true | 默认是否需要审批 |
| `erp.doc.source-bill-required` | true | 是否必须关联来源单据 |

## 开源参考

| 项目 | 参考维度 | 具体借鉴 |
|------|----------|----------|
| 管伊佳 | 双维度类型设计 | DepotHead.type + subType 单表设计 |
| 管伊佳 | SKU 多单位多 barcode | Material + MaterialExtend 分离 |
| 若依 | 进销存三单链 | order → trade → refund 闭环 |
| 若依 | 状态双维度 | checked_status + stock_status 分离 |
| 星云 | 单据四级拆分 | Head + Detail + DetailBundle + DetailLot |
| 赤龙 | 发票关联追溯 | invoiceSourceType + invoiceSourceHeadCode |