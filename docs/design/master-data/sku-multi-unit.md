# SKU 多单位多 barcode 设计

## 目的

说明物料与 SKU 的分离设计、多单位换算、多 barcode 管理机制。参考管伊佳的 Material + MaterialExtend 分离设计，为 nop-app-erp 提供灵活的 SKU 管理能力。

本文件是 `jsh-erp.md` 调研结论的落地设计，是 `master-data/README.md` 的详细展开。

## 设计背景

### 调研发现

从管伊佳的调研中发现：

| 发现 | 说明 |
|------|------|
| 物料与 SKU 分离 | Material 承载基础属性，MaterialExtend 承载 SKU 属性 |
| SKU 多单位 | 每个 SKU 对应一个包装单位 |
| SKU 多 barcode | 每个 SKU 可有独立条码 |
| SKU 多档价格 | 采购价/批发价/零售价/最低价 |
| 默认单位标志 | `defaultFlag` 标识默认 SKU |

### 核心价值

- **多单位换算**：同一物料支持多种包装单位
- **多 barcode 管理**：不同包装单位可有不同条码
- **多档价格**：不同销售场景使用不同价格
- **灵活扩展**：SKU 属性独立扩展，不影响物料主数据

## 物料与 SKU 分离

### 物料（Material）

物料承载基础属性：

| 属性 | 说明 |
|------|------|
| materialCode | 物料编码（全局唯一） |
| materialName | 物料名称 |
| materialType | 物料类型（商品/原材料/产成品/服务） |
| category | 物料分类 |
| brand | 品牌 |
| spec | 规格 |
| model | 型号 |
| baseUnitId | 基本单位 |
| shelfLife | 保质期（天） |
| shelfLifeRule | 效期规则（按生产日期/入库日期） |
| batchManaged | 是否批次管理 |
| serialManaged | 是否序列号管理 |
| weight | 重量 |
| volume | 体积 |
| status | 启用/停用 |

### SKU（MaterialSku）

SKU 承载销售/库存属性：

| 属性 | 说明 |
|------|------|
| materialId | 关联物料 |
| skuCode | SKU 编码（物料内唯一） |
| skuName | SKU 名称 |
| unitId | 包装单位 |
| barcode | 条码（SKU 级别） |
| conversionFactor | 换算系数（包装单位→基本单位） |
| purchasePrice | 采购价 |
| wholesalePrice | 批发价 |
| retailPrice | 零售价 |
| minPrice | 最低价 |
| defaultFlag | 是否默认 SKU |
| status | 启用/停用 |

### 物料与 SKU 关系

```
物料与 SKU 关系
        │
        ├─► 物料（Material）
        │      ├─ materialCode: MAT001
        │      ├─ materialName: 可乐
        │      ├─ baseUnitId: 瓶（基本单位）
        │      └─ shelfLife: 365
        │
        ├─► SKU1（MaterialSku）
        │      ├─ materialId: MAT001
        │      ├─ unitId: 瓶
        │      ├─ barcode: 6901234567890
        │      ├─ conversionFactor: 1
        │      ├─ retailPrice: 3.00
        │      └─ defaultFlag: true
        │
        ├─► SKU2（MaterialSku）
        │      ├─ materialId: MAT001
        │      ├─ unitId: 箱
        │      ├─ barcode: 6901234567891
        │      ├─ conversionFactor: 24（1箱=24瓶）
        │      ├─ wholesalePrice: 60.00
        │      └─ defaultFlag: false
        │
        └─► SKU3（MaterialSku）
               ├─ materialId: MAT001
               ├─ unitId: 托盘
               ├─ barcode: 6901234567892
               ├─ conversionFactor: 576（1托盘=24箱=576瓶）
               ├─ purchasePrice: 1200.00
               └─ defaultFlag: false
```

## 多单位换算

### 单位组与换算系数

同一物料的 SKU 必须属于同一单位组：

```
单位组与换算系数
        │
        ├─► 单位组（UoMGroup）
        │      ├─ groupId: UG001
        │      ├─ groupName: 包装单位组
        │      └─ baseUnitId: 瓶
        │
        ├─► 单位换算（UoMConversion）
        │      ├─ 瓶 → 瓶：factor = 1
        │      ├─ 瓶 → 箱：factor = 24
        │      └─ 瓶 → 托盘：factor = 576
        │
        └─► SKU 单位约束
               ├─ 同一物料的 SKU 单位必须属于同一单位组
               └─ 换算系数从单位换算表获取
```

### 数量换算逻辑

```java
/**
 * SKU 数量换算
 */
public BigDecimal convertQty(Long skuId, BigDecimal qty, Long targetUnitId) {
    MaterialSku sku = skuDao.findById(skuId);
    Long sourceUnitId = sku.getUnitId();
    
    // 获取换算系数
    BigDecimal sourceFactor = getConversionFactor(sku.getMaterialId(), sourceUnitId);
    BigDecimal targetFactor = getConversionFactor(sku.getMaterialId(), targetUnitId);
    
    // 换算公式：目标数量 = 源数量 × 源系数 ÷ 目标系数
    return qty.multiply(sourceFactor).divide(targetFactor, 4, RoundingMode.HALF_UP);
}

/**
 * 获取换算系数（包装单位→基本单位）
 */
public BigDecimal getConversionFactor(Long materialId, Long unitId) {
    Material material = materialDao.findById(materialId);
    if (unitId.equals(material.getBaseUnitId())) {
        return BigDecimal.ONE;
    }
    UoMConversion conversion = conversionDao.findByMaterialAndUnit(materialId, unitId);
    return conversion.getConversionFactor();
}
```

### 业务场景换算

| 场景 | 换算说明 |
|------|----------|
| 采购入库 | 按 SKU 包装单位录入，落账转为基本单位数量 |
| 销售出库 | 按 SKU 包装单位录入，落账转为基本单位数量 |
| 库存余额 | 按基本单位存储，展示时可转换为任意包装单位 |
| 成本核算 | 按基本单位计算成本 |

## 多 barcode 管理

### SKU 条码

每个 SKU 可有独立条码：

| 条码类型 | 说明 |
|----------|------|
| EAN-13 | 国际标准条码（13位） |
| EAN-8 | 简化条码（8位） |
| UPC-A | 美国条码（12位） |
| 内部码 | 企业内部编码 |

### 条码唯一约束

```xml
<entity name="ErpMdMaterialSku">
    <column name="barcode" type="String" length="20"/>
    <index name="idx_barcode" columns="barcode" unique="true" condition="barcode is not null"/>
</entity>
```

### 条码查询

```java
/**
 * 按条码查询 SKU
 */
public MaterialSku findSkuByBarcode(String barcode) {
    return skuDao.findByBarcode(barcode);
}

/**
 * 按条码查询物料
 */
public Material findMaterialByBarcode(String barcode) {
    MaterialSku sku = findSkuByBarcode(barcode);
    if (sku == null) return null;
    return materialDao.findById(sku.getMaterialId());
}
```

### 条码生成规则

```
条码生成规则
        │
        ├─► 规则1：物料编码 + SKU 序号
        │      ├─ MAT001-001 → 6901234567890
        │      └─ MAT001-002 → 6901234567891
        │
        ├─► 规则2：国际条码申请
        │      ├─ 向条码中心申请
        │      └─ 前缀 + 物料编码 + 校验位
        │
        └─► 规则3：内部编码
               ├─ 企业自定义规则
               └─ 不参与国际流通
```

## 多档价格管理

### 价格类型

| 价格类型 | 说明 | 用途 |
|----------|------|------|
| purchasePrice | 采购价 | 采购订单默认价格 |
| wholesalePrice | 批发价 | 批发销售默认价格 |
| retailPrice | 零售价 | 零售销售默认价格 |
| minPrice | 最低价 | 销售价格底线 |

### 价格优先级

```
价格优先级
        │
        ├─► 优先级1：单据行手工输入价格
        │      └─ 最高优先级
        │
        ├─► 优先级2：价格表匹配价格
        │      ├─ 按客户/物料/日期匹配
        │      └─ 价格表优先
        │
        ├─► 优先级3：SKU 默认价格
        │      ├─ 按销售类型选择价格档
        │      └─ 零售→retailPrice，批发→wholesalePrice
        │
        └─► 价格校验
               ├─ 输入价格 ≥ minPrice
               └─ 否则拒绝或警告
```

### 价格表扩展

除 SKU 内建价格外，支持价格表：

```
价格表（PriceList）
        │
        ├─► 价格表头
        │      ├─ priceListId
        │      ├─ priceListName
        │      ├─ priceType（采购/批发/零售）
        │      ├─ partnerId（客户专属价格表）
        │      └─ effectiveDate
        │
        └─► 价格表行
               ├─ materialId
               ├─ skuId
               ├─ price
               └─ minPrice
```

## 默认 SKU

### 默认 SKU 标志

每个物料必须有一个默认 SKU：

```
默认 SKU 规则
        │
        ├─► 规则1：每个物料必须有一个 defaultFlag = true 的 SKU
        │
        ├─► 规则2：默认 SKU 通常是基本单位 SKU
        │      └─ conversionFactor = 1
        │
        ├─► 规则3：创建新物料时自动创建默认 SKU
        │      └─ unitId = baseUnitId
        │
        └─► 规则4：业务单据未指定 SKU 时使用默认 SKU
               └─ 查询 materialId + defaultFlag = true
```

### 默认 SKU 查询

```java
/**
 * 查询物料的默认 SKU
 */
public MaterialSku findDefaultSku(Long materialId) {
    return skuDao.findDefaultSku(materialId);
}

/**
 * 业务单据未指定 SKU 时使用默认 SKU
 */
public MaterialSku resolveSku(Long materialId, Long unitId) {
    if (unitId != null) {
        return skuDao.findByMaterialAndUnit(materialId, unitId);
    }
    return findDefaultSku(materialId);
}
```

## SKU 状态管理

### SKU 启停

SKU 状态与物料状态联动：

```
SKU 启停规则
        │
        ├─► 物料停用 → 所有 SKU 一并不可被新单据引用
        │
        ├─► SKU 独立停用 → 该 SKU 不可被新单据引用
        │      └─ 其他 SKU 仍可用
        │
        ├─► 默认 SKU 停用 → 必须先设置其他 SKU 为默认
        │      └─ 不能停用唯一的默认 SKU
        │
        └─► SKU 删除 → 必须校验是否被业务单据引用
               └─ 被引用则拒绝删除
```

### SKU 状态校验

```java
/**
 * 停用 SKU 前校验
 */
public void validateSkuDeactivation(Long skuId) {
    MaterialSku sku = skuDao.findById(skuId);
    
    // 校验是否为默认 SKU
    if (sku.getDefaultFlag()) {
        // 查询是否有其他可用 SKU
        MaterialSku otherSku = skuDao.findOtherActiveSku(sku.getMaterialId(), skuId);
        if (otherSku == null) {
            throw new NopException("不能停用唯一的默认 SKU，请先设置其他 SKU 为默认");
        }
    }
    
    // 校验是否被未完成单据引用
    if (hasUnfinishedReference(skuId)) {
        throw new NopException("SKU 被未完成单据引用，不能停用");
    }
}
```

## 业务单据 SKU 引用

### 单据行 SKU 引用

业务单据行引用 SKU：

```xml
<entity name="ErpPurOrderLine">
    <column name="materialId" type="Long" mandatory="true"/>
    <column name="skuId" type="Long" mandatory="true"/>
    <column name="unitId" type="Long" mandatory="true"/>
    <column name="qty" type="Decimal" mandatory="true"/>
    <column name="baseQty" type="Decimal" mandatory="true"/> <!-- 基本单位数量 -->
    <column name="price" type="Decimal"/>
    <column name="amount" type="Decimal"/>
</entity>
```

### SKU 选择逻辑

```
业务单据 SKU 选择
        │
        ├─► 场景1：选择物料后自动选择默认 SKU
        │      ├─ materialId → 查询 defaultFlag = true 的 SKU
        │      └─ 自动填充 skuId、unitId
        │
        ├─► 场景2：选择物料 + 单位后匹配 SKU
        │      ├─ materialId + unitId → 查询匹配 SKU
        │      └─ 如果无匹配 SKU，提示创建
        │
        ├─► 场景3：扫描条码直接选择 SKU
        │      ├─ barcode → 查询 SKU
        │      └─ 自动填充 materialId、skuId、unitId
        │
        └─► 数量换算
               ├─ qty（包装单位数量）× conversionFactor = baseQty（基本单位数量）
               └─ 落账时使用 baseQty
```

## 配置项

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `erp-md.sku-default-required` | true | 是否必须有默认 SKU |
| `erp-md.sku-barcode-unique` | true | 条码是否全局唯一 |
| `erp-md.sku-price-validation` | true | 是否校验最低价 |
| `erp-md.sku-auto-create-default` | true | 创建物料时是否自动创建默认 SKU |

## 开源参考

| 项目 | 参考维度 | 具体借鉴 |
|------|----------|----------|
| 管伊佳 | 物料 SKU 分离 | Material + MaterialExtend 分离设计 |
| 管伊佳 | SKU 多档价格 | purchaseDecimal/commodityDecimal/wholesaleDecimal/lowDecimal |
| 管伊佳 | 默认 SKU 标志 | defaultFlag 标识默认 SKU |
| Dolibarr | 含税/不含税双价 | price/price_ttc（含税）/price_base_type |
| ERPNext | SKU 变体属性 | Item Variant 属性组合 |