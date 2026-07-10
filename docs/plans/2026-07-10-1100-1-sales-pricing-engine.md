# 2026-07-10-1100-1-sales-pricing-engine 销售定价引擎

> Plan Status: draft
> Last Reviewed: 2026-07-10
> Source: 用户请求 + erp-survey 对标调研（Odoo Pricelist / ERPNext Pricing Rule 均为核心内置功能）
> Related: UC-SAL-11（`docs/design/sales/use-cases.md:231-255`）、core-business-roadmap Non-Goal scope boundary
> Audit: required

## Current Baseline

### 已实现

- **基础取价 `resolvePrice`**（UC-MD-03）：`ErpMdMaterialSkuBizModel.resolvePrice(skuId, partnerId, billType, manualPrice)` 三级优先级：手工价 > 价格表层 SPI > SKU 默认档（purchase/wholesale/retail/sale 四档）。`module-master-data/erp-md-service`
- **最低价校验 `validatePrice`**（UC-MD-04）：`ErpMdMaterialCategory.priceValidationLevel`（OFF/WARN/HARD），minPrice 派生自 SKU 四档价最小正值。
- **采购供应商价格表** `ErpPurSupplierPriceList`：扁平实体（supplierId + materialId + unitPrice + minOrderQuantity + validFrom/validTo + priority + isActive），`module-purchase/model/app-erp-purchase.orm.xml:388-433`。生产环境 `IErpMdSupplierPriceResolver` SPI **无实现**（仅测试桩 `TestStubSupplierPriceResolver`）。
- **CRM 定价规则** `ErpCrmPriceRule` + `PriceRuleEngine`：CPQ 独立链路（ruleType=priority + priceOverride/discountPercent/discountAmount），面向报价单生成，**不接入销售订单行**。`module-crm/model/app-erp-crm.orm.xml:1231`
- **订单头折扣** `ErpSalOrder` 有 `discountRate`/`discountAmount`；**订单行无行级折扣字段**。
- **菜单占位**：`erp-sal.action-auth.xml:84-97` 已注册 `sales-price-list`（销售价格清单）+ `pricing-rule`（促销规则），均标注 `useCases="UC-SAL-11"`。对应页面文件存在但为占位（`alert: "main — 待实现占位页面"`）。
- **对应的 ORM 实体（销售价格清单、销售促销规则）尚未创建**。

### 剩余差距

- 无销售价格清单实体（客户协议价 / 阶梯价 / 客户组价）
- 无销售促销规则引擎（买赠 / 满减 / 折扣）
- 订单行取价不经过价格清单（仅人工录入或前端调 `resolvePrice`）
- 促销规则不接入订单（CRM CPQ 链路独立）
- 订单行无行级折扣字段（折扣仅订单头）

### 对标依据

| 开源 ERP | 定价引擎 | 状态 |
|----------|---------|------|
| **Odoo** | Pricelist（多价格表 + 阶梯价 + 客户组 + 促销 sale_loyalty） | 核心内置 |
| **ERPNext** | Pricing Rule（条件匹配 + 折扣/价格覆盖 + 按客户/物料/数量） | 核心内置 |
| **本项目** | 仅基础取价 + 采购供应商价表（无销售侧） | **gap** |

## Goals

- 实现销售价格清单（头/行），支持客户/客户组维度、物料/SKU 维度、数量阶梯价、生效期间、优先级
- 实现销售促销规则引擎，支持折扣、满减、买赠三类规则，按物料/客户/时段匹配，可配置叠加
- 订单行取价接入价格清单（`resolvePrice` 优先级链新增客户价格清单层）
- 促销规则在订单保存时自动评估和应用
- 前端 CRUD 页面替换占位页面
- 全链路通过 GraphQL Engine 测试覆盖

## Non-Goals

- 多级 `.xwf` 审批工作流（定价审批链）——归 Deferred
- 机器学习定价 / 动态定价算法——超出参考 ERP 范畴
- 跨币种价格清单自动汇率转换——价格清单按币种独立维护
- 合同专用价格条款（contract 域 `InvoicePlan` 已有独立定价）——不在本期接入
- CRM CPQ 定价链路与销售订单定价的合并——两条链路独立维护
- 采购侧价格表 SPI 生产实现——本期仅销售侧

## Task Route

- Type: `app-layer design change` + `implementation-only change`
- Owner Docs: `docs/design/sales/use-cases.md`（UC-SAL-11）、`docs/design/sales/README.md`（§关键业务规则 §7 赠品与折扣）、`docs/design/master-data/use-cases.md`（UC-MD-03/04）
- Skill Selection Basis: 新增 ORM 实体 + BizModel 方法 + 前端页面 → 需 nop-backend-dev（后端方法自检）、nop-frontend-dev（AMIS 页面）；nop-testing（GraphQL Engine 测试）

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline

## Execution Plan

### Phase 1 - ORM 模型变更：新增定价实体 + 订单行折扣字段

Status: planned
Targets: `module-sales/model/app-erp-sales.orm.xml`
Skill: nop-backend-dev（ORM 模型设计参考 `docs-for-ai/02-core-guides/orm-model-design.md`）

- Item Types: `Decision | Add`
- Prereqs: none

- [ ] Decision: 实体结构设计
  - 参考来源：ERPNext Item Price（头/行）+ Pricing Rule（规则引擎）；现有 `ErpPurSupplierPriceList`（扁平实体字段集）；`ErpCrmPriceRule`（ruleType/priority/discount 模式）
  - 替代方案 A：扁平实体（像 `ErpPurSupplierPriceList`）——简单但无法表达"一个价格清单含多个行"
  - 替代方案 B：头/行结构（ERPNext 模式）——支持清单级别管理和批量启用/停用
  - **选择 B**（头/行），理由：菜单占位已命名为"销售价格清单"（暗示头/行）；头/行支持批量管理、生效期间控制、与客户组关联
  - 残留风险：头/行比扁平多一层查询，需确保 resolvePrice 查询效率（SQL-Lib 或缓存）
  - Skill: nop-backend-dev

- [ ] Add: `ErpSalPriceList`（销售价格清单头）
  - 字段：name, code, currencyId(→ErpMdCurrency), customerGroupId(nullable, →ErpMdPartner 或新客户组), partnerId(nullable, 指定客户时优先), validFrom(DATE), validTo(DATE), priority(INT, 小优先), isActive(BOOL), remark
  - 字典：`erp-sal/price-list-status`（ACTIVE/INACTIVE）
  - Skill: nop-backend-dev

- [ ] Add: `ErpSalPriceListLine`（销售价格清单行）
  - 字段：priceListId(→ErpSalPriceList), materialId(→ErpMdMaterial), skuId(→ErpMdMaterialSku), uoMId(→ErpMdUoM), unitPrice(DECIMAL 20,4), minQuantity(DECIMAL 20,4, 阶梯下限, default 0), maxQuantity(DECIMAL 20,4, nullable), validFrom(nullable, 覆盖头), validTo(nullable, 覆盖头)
  - 关系：to-one priceList/material/sku/uoM
  - Skill: nop-backend-dev

- [ ] Add: `ErpSalPricingRule`（销售促销规则）
  - 字段：ruleName, ruleCode, ruleType(字典 `erp-sal/pricing-rule-type`: PERCENT_DISCOUNT/AMOUNT_OFF/GIFT/PRICE_OVERRIDE), targetType(字典 `erp-sal/pricing-target`: LINE/ORDER), materialId(nullable), materialCategoryId(nullable), customerGroupId(nullable), partnerId(nullable), minOrderAmount(nullable, 满减门槛), discountPercent(DECIMAL 10,4, nullable), discountAmount(DECIMAL 20,4, nullable), giftMaterialId(nullable, →ErpMdMaterial), giftSkuId(nullable), giftQuantity(DECIMAL 20,4, nullable), priceOverride(DECIMAL 20,4, nullable), currencyId(nullable), priority(INT, 小优先, default 100), stackable(BOOL, default false), validFrom(TIMESTAMP), validTo(TIMESTAMP), isActive(BOOL), remark
  - Skill: nop-backend-dev

- [ ] Add: `ErpSalOrderLine` 新增字段
  - `discountRate` DECIMAL(10,4) nullable —— 行折扣率(%)
  - `discountAmount` DECIMAL(20,4) nullable —— 行折扣金额
  - `pricingSource` VARCHAR(50) nullable —— 取价来源标记（MANUAL/PRICE_LIST/PROMOTION/SKU_DEFAULT），用于审计追踪
  - 理由：UC-SAL-11 促销"改单价"场景需要保留原始价格来源；行级折扣使促销效果可追溯
  - Skill: nop-backend-dev

- [ ] Add: `ErpSalQuotationLine` 同步新增 `discountRate`/`discountAmount`/`pricingSource`（与订单行对称）
  - Skill: nop-backend-dev

- [ ] Add: 执行 `mvn clean install -DskipTests`（module-sales 链）触发增量代码生成，验证生成的 DAO/Entity/Meta/XBiz 无错误
  - Skill: nop-backend-dev

Exit Criteria:

- [ ] ORM 模型变更后 `mvn clean install -DskipTests`（module-sales + module-master-data 链）BUILD SUCCESS
- [ ] 生成的 Entity/DAO 类包含新增实体和字段（抽查 `ErpSalPriceList.java`、`ErpSalPriceListLine.java`、`ErpSalPricingRule.java` 存在；`ErpSalOrderLine` 含 `discountRate`/`discountAmount`/`pricingSource` getter）

### Phase 2 - 定价解析引擎 + 促销规则引擎

Status: planned
Targets: `module-master-data/erp-md-dao/src/main/java/.../spi/`（SPI 声明）、`module-sales/erp-sal-service/src/main/java/.../`（引擎实现 + BizModel）
Skill: nop-backend-dev

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1

- [ ] Decision: SPI 设计
  - 创建 `IErpMdCustomerPriceResolver`（parallel to `IErpMdSupplierPriceResolver`），位于 `erp-md-dao`（基础域声明端口，销售域实现）
  - 方法签名：`ResolvedPrice resolveCustomerPrice(ErpMdMaterialSku sku, Long partnerId, String billType, BigDecimal quantity, Long currencyId, IServiceContext context)`
  - `ResolvedPrice` 数据 bean：`BigDecimal unitPrice; String source; Long priceListId; String priceListName;`
  - 替代方案：扩展 `IErpMdSupplierPriceResolver` 新增 `resolveCustomerPrice` 方法——rejected，因现有方法名和语义面向供应商，混入客户方法违反接口单一职责
  - Skill: nop-backend-dev

- [ ] Add: `ErpMdMaterialSkuBizModel.resolvePrice` 增加客户价格清单层
  - 修改优先级链为：手工价 > 客户价格清单（`IErpMdCustomerPriceResolver`）> 供应商价格表（`IErpMdSupplierPriceResolver`）> SKU 默认档
  - `@Inject @Nullable IErpMdCustomerPriceResolver customerPriceResolver` —— 无实现时空转
  - Skill: nop-backend-dev

- [ ] Add: `ErpSalPriceListBizModel`（CrudBizModel）
  - 标准 CRUD + `defaultPrepareQuery`（按 isActive/validFrom/validTo 过滤）
  - `@BizQuery resolveCustomerPrice` 委托给 `ErpSalCustomerPriceResolver`（内部实现 `IErpMdCustomerPriceResolver`）
  - Skill: nop-backend-dev

- [ ] Add: `ErpSalCustomerPriceResolver`（implements `IErpMdCustomerPriceResolver`）
  - 查询逻辑：按 partnerId/customerGroupId + skuId/materialId + currencyId + 期间匹配 + minQuantity/maxQuantity 阶梯 → 按 priority 选最优
  - 使用 `@SqlLibMapper` 或 QueryBean 构造查询（按性能选择，需注释理由）
  - 返回 `ResolvedPrice(unitPrice, source="PRICE_LIST", priceListId, priceListName)`
  - Skill: nop-backend-dev

- [ ] Add: `ErpSalPricingRuleBizModel`（CrudBizModel）
  - 标准 CRUD + `defaultPrepareQuery`（按 isActive/validFrom/validTo 过滤）
  - Skill: nop-backend-dev

- [ ] Add: `ErpSalPricingRuleEngine`（纯函数式引擎，`erp-sal-service/.../support/`）
  - 输入：订单头 + 订单行列表 + 客户上下文（partnerId, customerGroupId）
  - 评估步骤：
    1. 加载所有 active 且期间有效的规则（`findActiveRules(customerId, materialIds, now)`）
    2. 按 targetType 分类：LINE 规则逐行匹配，ORDER 规则匹配头
    3. PERCENT_DISCOUNT → 计算行 discountAmount = unitPrice × qty × discountPercent/100
    4. AMOUNT_OFF → 头级 discountAmount（满足 minOrderAmount 时）
    5. GIFT → 在订单行末尾追加赠品行（unitPrice=0, quantity=giftQuantity, pricingSource="PROMOTION"）
    6. PRICE_OVERRIDE → 覆盖 unitPrice（记录 pricingSource="PROMOTION"）
    7. stackable=false 的规则命中后跳过同类型后续规则；stackable=true 可叠加
  - 参考：CRM `PriceRuleEngine`（`module-crm/erp-crm-service/.../support/PriceRuleEngine.java`）的优先级+期间+数量区间评估模式
  - 输出：应用的规则列表 + 修改后的订单行折扣/赠品行快照
  - Skill: nop-backend-dev

- [ ] Add: `ErpSalOrderBizModel` 新增 `applyPricingRules` 方法
  - `@BizMutation @Name("applyPricingRules") void applyPricingRules(@Name("orderId") String orderId, IServiceContext context)`
  - 调用 `ErpSalPricingRuleEngine.evaluate(order)`，将结果写回订单行（discountRate/discountAmount/pricingSource + 赠品行）
  - 重新计算订单头合计（amount/taxAmount/totalAmountWithTax/discountAmount）
  - Skill: nop-backend-dev

- [ ] Add: `ErpSalErrors` 新增错误码
  - `ERR_PRICING_RULE_CONFLICT`（多条同优先级规则冲突）
  - `ERR_PRICE_LIST_EXPIRED`（引用的价格清单已过期）
  - Skill: nop-backend-dev

- [ ] Proof: 单元测试 `TestErpSalPricingRuleEngine`
  - 纯函数引擎测试（无 IoC）：规则匹配 + 优先级 + 叠加 + 期间过滤 + 数量阶梯
  - 覆盖场景：PERCENT_DISCOUNT 行级、AMOUNT_OFF 头级满减、GIFT 买赠、PRICE_OVERRIDE 覆盖、stackable 叠加、非叠加排他、期间外不匹配、数量区间外不匹配
  - Skill: nop-testing

Exit Criteria:

- [ ] `ErpSalCustomerPriceResolver` 实现 `IErpMdCustomerPriceResolver`，被 `ErpMdMaterialSkuBizModel.resolvePrice` 正确调用
- [ ] `ErpSalPricingRuleEngine` 纯单元测试全绿（覆盖全部 4 种 ruleType + stackable 场景）
- [ ] 所有新增 BizModel 方法通过 nop-backend-dev 19 项自检

### Phase 3 - 订单定价集成 + GraphQL API 测试

Status: planned
Targets: `module-sales/erp-sal-service/`（Processor / BizModel 集成）
Skill: nop-backend-dev, nop-testing

- Item Types: `Add | Proof`
- Prereqs: Phase 2

- [ ] Add: `ErpSalOrderProcessor.approve` 增强——审核时不重取价（保持现状），但新增 pricingSource 审计断言
  - 审核日志记录每行 pricingSource 分布
  - Skill: nop-backend-dev

- [ ] Add: 订单行保存时自动填充 pricingSource
  - `ErpSalOrderBizModel.defaultPrepareSave` / `defaultPrepareUpdate`：若 unitPrice 来自前端 resolvePrice 调用，标记 pricingSource
  - 若手动填入 unitPrice → pricingSource = "MANUAL"
  - Skill: nop-backend-dev

- [ ] Add: 配置项
  - `erp-sal.auto-pricing-on-save`（默认 true）——订单保存时自动应用促销规则
  - `erp-sal.pricing-rule-stack-default`（默认 false）——促销规则默认是否可叠加
  - Skill: nop-backend-dev

- [ ] Proof: GraphQL Engine 集成测试 `TestErpSalPricingEndToEnd`
  - 场景 1：价格清单取价——创建价格清单（客户组+物料+阶梯），创建订单行 → 断言 unitPrice 来自价格清单
  - 场景 2：促销折扣——创建 PERCENT_DISCOUNT 规则，创建订单，调用 `applyPricingRules` → 断言行 discountAmount 正确
  - 场景 3：满减——创建 AMOUNT_OFF 规则（minOrderAmount），创建超门槛订单 → 断言头 discountAmount 正确
  - 场景 4：买赠——创建 GIFT 规则，创建订单 → 断言赠品行追加正确
  - 场景 5：叠加——两条 stackable 规则 → 断言叠加后折扣正确
  - 场景 6：优先级——两条非叠加同物料规则 → 断言高优先级（priority 小）规则生效
  - 场景 7：最低价校验——促销后价低于 SKU minPrice + level=HARD → 断言拒绝
  - Skill: nop-testing

- [ ] Proof: 价格清单/促销规则 CRUD GraphQL 测试 `TestErpSalPriceListCrud` + `TestErpSalPricingRuleCrud`
  - 标准 CRUD 冒烟（create/findPage/update/delete + 期间/启用过滤）
  - Skill: nop-testing

Exit Criteria:

- [ ] GraphQL Engine 集成测试全绿（定价取价 + 促销应用 + CRUD 冒烟，≥7 场景）
- [ ] 配置项可通过 SysConfig 控制（不硬编码）

### Phase 4 - 前端页面

Status: planned
Targets: `module-sales/erp-sal-web/src/main/resources/_vfs/erp/sal/pages/`
Skill: nop-frontend-dev

- Item Types: `Add`
- Prereqs: Phase 3

- [ ] Add: `sales-price-list/main.page.yaml` — 价格清单列表页（替换占位）
  - 标准 grid：name/code/currency/customerGroup/validFrom/validTo/priority/isActive
  - 行操作：查看明细（展开行显示 ErpSalPriceListLine grid）
  - Skill: nop-frontend-dev

- [ ] Add: `sales-price-list/edit.page.yaml` — 价格清单编辑页（头 + 行内嵌 grid）
  - 头字段 + 行 grid（增删改行：material/sku/uoM/unitPrice/minQuantity/maxQuantity）
  - Skill: nop-frontend-dev

- [ ] Add: `pricing-rule/main.page.yaml` — 促销规则列表页（替换占位）
  - 标准 grid：ruleName/ruleType/targetType/material/customerGroup/discountPercent/discountAmount/validFrom/validTo/isActive
  - Skill: nop-frontend-dev

- [ ] Add: `pricing-rule/edit.page.yaml` — 促销规则编辑表单
  - 按 ruleType 条件渲染不同字段组（PERCENT_DISCOUNT → discountPercent；AMOUNT_OFF → discountAmount+minOrderAmount；GIFT → giftMaterial+giftQuantity；PRICE_OVERRIDE → priceOverride）
  - Skill: nop-frontend-dev

- [ ] Add: `ErpSalOrder` 编辑页增强——订单行 grid 新增 discountRate/discountAmount/pricingSource 列（只读展示）
  - 价格列右侧新增"取价"按钮，点击调 GraphQL `ErpMdMaterialSku__resolvePrice` 自动填充
  - Skill: nop-frontend-dev

Exit Criteria:

- [ ] 4 个页面 YAML 文件替换占位，通过 AMIS 页面加载无报错
- [ ] 订单行编辑页展示 pricingSource 和行级折扣

## Draft Review Record

- Independent draft review iteration 1: pending

## Closure Gates

- [ ] 范围内行为完成
- [ ] 相关文档对齐（`docs/design/sales/use-cases.md` UC-SAL-11 标注已实现；`docs/design/sales/README.md` §关键业务规则 更新定价引擎描述；`core-business-roadmap.md:27` Non-Goal 标注修正）
- [ ] 已运行验证：`mvn clean install -DskipTests`（全 reactor）+ `mvn test -pl module-sales/erp-sal-service` + Playwright 定价页面冒烟
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### 多级 xwf 信用审批工作流链（定价审批）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 定价规则审批不在本期范围；定价规则 CRUD 直接生效
- Successor Required: yes（触发条件：定价规则需多级审批时，承接 `docs/plans/2026-07-04-2050-1-use-approval-migration.md` Deferred 范式）

### 采购侧价格表 SPI 生产实现

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本期仅实现销售侧客户价格清单；采购供应商价格表 SPI 生产实现归独立工作项
- Successor Required: no（已有测试桩，需求驱动时实现）

### 跨币种价格清单自动汇率转换

- Classification: `optimization candidate`
- Why Not Blocking Closure: 价格清单按币种独立维护，不做自动转换
- Successor Required: no

## Closure

Status Note: pending

Closure Audit Evidence:

- Auditor / Agent: pending
- Evidence: pending

Follow-up:

- core-business-roadmap.md:27 UC 编号标签漂移修正（UC-SAL-06/08/10 与 use-cases.md 真实定义冲突）
