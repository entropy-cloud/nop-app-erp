# CRM 域 - CPQ 配置-定价-报价（Configure-Price-Quote）

## 目的

设计 CPQ（配置-定价-报价）引擎：支持复杂产品的条件规则配置、引导式销售向导、产品+服务捆绑定价、多维度价格规则（阶梯折扣、促销定价、客户特定定价），以及从配置结果生成报价单。

## 边界

- 本模块负责：产品配置规则引擎、引导式销售向导流程、捆绑定价管理、价格规则引擎、报价生成。
- CPQ 引擎是 CRM `ErpCrmLead` 商机阶段"方案演示/谈判中"的输出工具——配置结果生成报价草稿，通过 `IErpSalQuotationBiz` 创建正式报价单。
- 本模块不负责：产品主数据（master-data 域 `ErpMdProduct`）；报价单审批流（sales 域）；合同签署（sales 域）；价格审批工作流。
- 实体建议命名，ORM 模型见 `module-crm/model/app-erp-crm.orm.xml`。

## 设计依据

> 参考 **Odoo sale_product_configurator**（`sale_product_configurator` 模块）：产品可选特征（product.template.attribute.line）的配置器 UI，配置结果写入销售订单行。
>
> 参考 **ERPNext Product Bundle**（`Product Bundle` 文档类型）：将多个产品打包为一个可售 SKU，支持固定折扣和独立定价。
>
> 参考 **Salesforce CPQ**（`SBQQ__ProductConfig__c` / `SBQQ__Configurator__c`）：条件规则引擎、引导式配置向导、分层定价（volume/segment/customer-specific）、捆绑折扣。

## 实体清单

> 表前缀 `erp_crm_`、类名 `ErpCrm*`、字典 `erp-crm/*`。

### ErpCrmProductConfigurator（产品配置器头）

产品配置模板——定义某个产品品类有哪些可配置特征。

| 字段 | 含义 | 参考 |
|------|------|------|
| id/code/name/orgId | 标准 | — |
| productType | 适用产品品类（如 "SERVER"、"SOFTWARE"、"SERVICE"） | — |
| configName | 配置器名称 | 🟢 Salesforce `SBQQ__ProductConfig__c.Name` |
| isActive | 是否启用 | — |
| effectiveFrom | 生效开始日期 | — |
| effectiveTo | 生效结束日期 | — |
| wizardLayout | 向导步骤布局(JSON)（定义步骤名称和顺序） | 🟢 Salesforce `SBQQ__Configurator__c.Steps` |
| 标准审计字段 | | |

### ErpCrmConfigRule（配置规则行）

条件特征选择规则——一个特征的选择影响其他特征的可用性。

| 字段 | 含义 | 参考 |
|------|------|------|
| id/configuratorId/orgId | 标准 + 所属配置器（→ErpCrmProductConfigurator） | — |
| ruleType | 规则类型：REQUIRED（必选）/ OPTIONAL（可选）/ EXCLUDED（互斥）/ RECOMMENDED（推荐） | 🟢 Odoo `product.attribute` 可选/必选 |
| sourceFeatureCode | 条件特征编码（如 "CPU_TYPE"） | — |
| sourceFeatureValue | 条件特征值（如 "INTEL_XEON"） | — |
| targetFeatureCode | 目标特征编码（如 "HEATSINK"） | — |
| targetFeatureValue | 目标特征值（如 "HEAVY_DUTY"） | — |
| conditionExpression | 复杂条件表达式（备选，优先级高于单行条件） | 🟢 Salesforce CPQ 规则表达式 |
| sequence | 规则执行顺序 | — |
| 标准审计字段 | | |

**规则引擎伪代码**：
```
输入：selectedFeatures = {CPU_TYPE: "INTEL_XEON", MEMORY: "64GB"}
遍历 ErpCrmConfigRule WHERE configuratorId=X ORDER BY sequence:
  for each rule:
    if rule.sourceFeatureCode in selectedFeatures
       AND selectedFeatures[rule.sourceFeatureCode] == rule.sourceFeatureValue:
         if rule.ruleType == REQUIRED → targetFeature 标记为必选
         if rule.ruleType == EXCLUDED → targetFeature 禁用（互斥）
         if rule.ruleType == RECOMMENDED → targetFeature 标记为推荐
```

### ErpCrmBundlePricing（捆绑定价）

产品+服务捆绑包定价。

| 字段 | 含义 | 参考 |
|------|------|------|
| id/code/name/orgId | 标准 | — |
| bundleName | 捆绑包名称 | 🟢 ERPNext `Product Bundle.title` |
| description | 捆绑包描述 | — |
| discountType | 折扣类型：PERCENTAGE（百分比折扣）/ FIXED（固定金额折扣） | 🟢 ERPNext `Product Bundle.discount_type` |
| discountValue | 折扣值（百分比值如 15，或金额如 5000） | — |
| bundleAmount | 捆绑包总价（覆盖折扣计算后的手工定价） | — |
| effectiveFrom | 生效开始日期 | — |
| effectiveTo | 生效结束日期 | — |
| isActive | 是否启用 | — |
| 标准审计字段 | | |

**捆绑包行**：捆绑包所含单品通过子表 `ErpCrmBundlePricingLine` 定义（独立实体或 JSON，选独立实体更方便查询）。

### ErpCrmBundlePricingLine（捆绑包明细行）

| 字段 | 含义 |
|------|------|
| id/bundleId/orgId | 标准 + 所属捆绑包（→ErpCrmBundlePricing） |
| productId | 产品（→ErpMdProduct） |
| quantity | 数量 |
| unitPrice | 单品价格（覆盖产品主数据价格） |
| sequence | 排序 |
| 标准审计字段 | |

### 价格规则引擎支撑实体

价格规则在 CPQ 流程中的定价阶段生效。建议用单表存储多类型价格规则：

### ErpCrmPriceRule（价格规则）

| 字段 | 含义 | 参考 |
|------|------|------|
| id/code/name/orgId | 标准 | — |
| ruleType | 规则类型：VOLUME（数量折扣）/ PROMOTIONAL（促销定价）/ CUSTOMER_SPECIFIC（客户特定价格） | 🟢 Salesforce CPQ `PriceRule` |
| priority | 应用优先级（数值越小越优先） | — |
| productId | 适用产品（→ErpMdProduct，可空，空=全局规则） | — |
| productCategory | 适用产品品类（可空） | — |
| customerId | 适用客户（→ErpMdPartner，可空） | — |
| customerCategory | 适用客户类别（可空） | — |
| minQuantity | 最小数量（阶梯折扣的下限） | — |
| maxQuantity | 最大数量（阶梯折扣的上限，空=无上限） | — |
| priceOverride | 覆盖单价（固定价格） | 🟢 Odoo `product.pricelist.item.fixed_price` |
| discountPercent | 折扣百分比 | — |
| discountAmount | 折扣固定金额 | — |
| currencyId | 币种 | — |
| effectiveFrom | 生效开始日期 | — |
| effectiveTo | 生效结束日期 | — |
| isActive | 是否启用 | — |
| 标准审计字段 | | |

> **为什么不命名 `ErpCrmPricingRule` 而是 `ErpCrmPriceRule`？** 与标准名对齐，避免与 "Pricing" 概念混淆。

## 业务规则

### 1. 配置规则引擎

```
用户选择特征 A（如 CPU_TYPE=INTEL_XEON） →
  触发条件规则评估：
    if REQUIRED → 目标特征自动选中或标记必选
    if EXCLUDED → 目标特征禁用或隐藏
    if RECOMMENDED → 目标特征高亮推荐
  UI 即时更新可选列表
  配置完成后 → 生成配置快照(JSON) → 传递给定价引擎
```

### 2. 引导式销售向导

```
wizardLayout = [
  {"step": 1, "name": "选择CPU", "features": ["CPU_TYPE", "CORE_COUNT"]},
  {"step": 2, "name": "选择内存", "features": ["MEMORY", "ECC_SUPPORT"]},
  {"step": 3, "name": "选择存储", "features": ["STORAGE_TYPE", "STORAGE_SIZE"]},
  {"step": 4, "name": "选择服务", "features": ["WARRANTY", "INSTALL_SERVICE"]}
]

用户按顺序完成每个 step →
  每 step 完成时触发条件规则重新评估（影响后续 step 可选范围）
  全部 step 完成后 → 进入预览和定价
```

### 3. 捆绑定价

```
捆绑包 A（服务器 + 安装服务 + 3年维保）：
  单品零售价合计 = 100,000 + 5,000 + 15,000 = 120,000
  捆绑折扣 = 15%（百分比）
  捆绑特价 = 102,000
  OR 手工定价 bundleAmount = 100,000（覆盖折扣计算）
```

### 4. 价格规则优先级

```
价格规则应用顺序：
  1. 客户特定定价（customerId 匹配）→ 最高优先级
  2. 促销定价（promotional，有生效日期范围）
  3. 数量阶梯折扣（volume，按 min/maxQuantity）
  4. 标准定价（产品主数据价格）→ 默认

同一条规则匹配多条 → priority 数值小的优先
```

### 5. 报价生成流程

```
CPQ 配置完成 + 定价计算完成 →
  生成配置快照(JSON)存入 lead 或临时表
  调用 IErpSalQuotationBiz.createFromConfig(
    leadId, configSnapshot, bundlePricingId?, priceRuleIds?
  ) → 创建 ErpSalQuotation 报价单
  回写 lead.relatedBillType/Code
```

## 配置点

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `erp-crm.cpq.max-rules-per-configurator` | 100 | 单个配置器的最大规则数 |
| `erp-crm.cpq.enable-wizard` | true | 是否启用引导式向导（false=单页配置） |
| `erp-crm.cpq.default-currency` | CNY | 定价默认币种 |

## 状态机关联

CPQ 配置本身无独立状态机。配置结果通过弱指针（`lead.relatedBillType/Code`）关联到 sales `ErpSalQuotation`，报价单的生命周期在 sales 域管理。

## 反模式警示

- ⛔ **将配置规则硬编码在 Java if-else**——规则应通过 `ErpCrmConfigRule` 表配置，新增规则零代码。
- ⛔ **捆绑折扣与单品折扣混用同一条规则**——捆绑折扣是包级整体折扣，单品折扣是行级规则，独立计算后取最优（或按优先级）。
- ⛔ **CPQ 与产品主数据耦合过紧**——CPQ 引用产品 ID 但不修改产品主数据；价格规则覆盖产品价格但不回写。
- ⛔ **引导步骤硬编码在前端**——步骤顺序和包含特征应在 `wizardLayout` 可配置。

## 跨域协作

| 对端 | 协作方式 |
|------|---------|
| master-data（ErpMdProduct） | 配置规则源（特征/属性映射） |
| sales（ErpSalQuotation） | 配置结果通过 IErpSalQuotationBiz 创建报价单 |
| CRM（ErpCrmLead） | 配置结果关联 lead，报价单 by lead.relatedBillType/Code |

## 证据强度标注

| 证据 | 强度 | 说明 |
|------|------|------|
| 条件特征选择规则（conditional feature selection） | 🟢 | Odoo `sale_product_configurator` 模块；Salesforce CPQ rules |
| 捆绑定价（product bundle） | 🟢 | ERPNext `Product Bundle` doctype |
| 引导式销售向导（guided selling wizard） | 🟢 | Salesforce CPQ `Configurator` 引导步骤 |
| 数量阶梯折扣（volume discount） | 🟢 | Odoo `product.pricelist` 多层级价格 |
| 客户特定定价（customer-specific pricing） | 🟢 | Odoo `product.pricelist` partner 维度 |
| 促销定价（promotional pricing） | 🟢 | Salesforce CPQ `PriceRule` with date range |
| 配置→报价生成 | 🟢 | Salesforce CPQ `SBQQ__Quote__c` 从配置生成 |

## 参考

- `README.md` §跨域协作 §衔接契约（报价单生成契约）
- `use-cases.md` §UC-CRM-13（CPQ 用例）
- `../sales/README.md`（报价单域边界）
- `../../analysis/erp-survey/` — Odoo/ERPNext CPQ 机制分析
