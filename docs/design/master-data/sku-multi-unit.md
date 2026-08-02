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

物料承载基础属性：编码（全局唯一）、名称、类型（商品/原材料/产成品/服务）、分类、品牌、规格、型号、基本单位、保质期与效期规则、批次/序列号管理标志、重量、体积、启停状态。字段定义以 `module-master-data/model/app-erp-master-data.orm.xml`（`ErpMdMaterial` 实体）为权威源。

### SKU（MaterialSku）

SKU 承载销售/库存属性：物料回链（materialId）、SKU 编码（物料内唯一）、名称、包装单位、条码、换算系数（包装单位→基本单位）、四档价格（采购/批发/零售/最低）、默认 SKU 标志、启停状态。字段定义以 orm.xml（`ErpMdMaterialSku` 实体）为权威源。

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

换算公式：**目标数量 = 源数量 × 源系数 ÷ 目标系数**（系数 = 包装单位→基本单位的换算系数；基本单位自身系数 = 1，精度 4 位四舍五入）。实现见 erp-md-service 模块。

### 业务场景换算

| 场景 | 换算说明 |
|------|----------|
| 采购入库 | 按 SKU 包装单位录入，落账转为基本单位数量 |
| 销售出库 | 按客户要求的单位出库，校验可用量用基本单位 |
| 库存盘点 | 按基本单位盘点，差异按基本单位计算 |
| 成本核算 | 基本单位成本 × 基本单位数量 |

### 近似换算支持

部分物料的单位换算不是精确整数倍（如 1 箱 ≈ 12.5 瓶），系统支持单据行级别的自定义换算系数：

- **严格模式**（默认）：只允许使用预定义的单位换算系数，不允许行级覆盖
- **宽松模式**：允许在单据行上手动输入自定义换算系数（`customConversionFactor`），覆盖标准换算
- **配置项**：`erp-md.uom-conversion-strict`（默认 true），按物料可覆盖

> 宽松模式下，自定义换算系数仅影响当前单据行的数量换算，不修改主数据中的标准换算系数。
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

`barcode` 全局唯一（允许 null，仅非 null 时强制唯一）。约束定义以 orm.xml（`ErpMdMaterialSku` 实体）为权威源。

### 条码查询

支持按条码查询 SKU，以及按条码查询所属物料（经 SKU 回链 materialId）。查询接口见 erp-md-service 模块。

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

SKU 支持四档可配置价格：

| 价格类型 | 说明 | 用途 | 可配置 |
|----------|------|------|--------|
| purchasePrice | 采购价 | 采购订单默认价格 | 是（可启用/禁用） |
| wholesalePrice | 批发价 | 批发销售默认价格 | 是（可启用/禁用） |
| retailPrice | 零售价 | 零售销售默认价格 | 是（可启用/禁用） |
| minPrice | 最低价 | 销售价格底线 | 始终启用 |

> 价格档位可按物料类别配置启用/禁用（如服务类物料不需要批发价）。配置项：`erp-md.price-tiers`（按物料类别配置可用档位列表）。

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
               ├─ 输入价格 ≥ minPrice（默认最低价）
               └─ 否则拒绝或警告
```

### 折扣叠加规则

- 折扣在源币种金额上扣减后再按汇率转换本位币
- 折扣类型：固定金额折扣、百分比折扣
- 折扣叠加：单头折扣 + 行级折扣叠加（行级折扣先算，单头折扣再算）
- 折扣后价格不低于 minPrice（最低价保护）
- 折扣不改变 SKU 的标准价格档位值

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

`resolveSku(materialId, unitId)`：指定单位时按 物料+单位 匹配 SKU；未指定单位时回退到物料的默认 SKU（defaultFlag=true）。实现见 erp-md-service 模块。

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

停用 SKU 前校验两条业务规则：(1) 若为默认 SKU，必须存在其他可用 SKU 可接替默认标志，否则拒绝；(2) 若被未完成业务单据引用，则拒绝停用。校验逻辑见 erp-md-service 模块。

## 业务单据 SKU 引用

### 单据行 SKU 引用

业务单据行同时引用物料（materialId）+ SKU（skuId）+ 单位（unitId），并记录包装单位数量（qty）与基本单位数量（baseQty，落账用）+ 价格/金额。各单据行的字段定义以所属域 orm.xml 为权威源（如采购订单行见 `module-purchase/model/`）。

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
| `MaterialCategory.priceValidationLevel` | WARN(20) | 价格校验级别(OFF/WARN/HARD),按物料类别配置,见 ORM 字段 |
| `erp-md.sku-auto-create-default` | true | 创建物料时是否自动创建默认 SKU |

## 开源参考

| 项目 | 参考维度 | 具体借鉴 |
|------|----------|----------|
| 管伊佳 | 物料 SKU 分离 | Material + MaterialExtend 分离设计 |
| 管伊佳 | SKU 多档价格 | purchaseDecimal/commodityDecimal/wholesaleDecimal/lowDecimal |
| 管伊佳 | 默认 SKU 标志 | defaultFlag 标识默认 SKU |
| Dolibarr | 含税/不含税双价 | price/price_ttc（含税）/price_base_type |
| ERPNext | SKU 变体属性 | Item Variant 属性组合 |