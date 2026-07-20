---
调研日期: 2026-07-20
来源: ~/sources/erp/ofbiz-framework（GitHub apache/ofbiz-framework，浅克隆）
分类: 国际开源 · Java（Apache OFBiz 完整框架）
状态: 已完成（基于源码实测）
---

# Apache OFBiz 完整框架调研报告

> Apache OFBiz 完整框架，包含 12 个业务组件 + 1 个数据模型共享库。**与此前调研的 `ofbiz-plugins`（仅插件仓库）不同，本报告覆盖 OFBiz 核心框架的完整业务应用**——包括会计总账、订单管理、制造、主数据（Party/Product）等全套模块。对 nop-app-erp 的会计引擎设计、跨模块耦合模式、实体建模规范有直接参考价值。

## 1. 基本信息

| 项 | 值 |
|---|---|
| 技术栈 | Java · Apache OFBiz 框架 · Groovy 服务 · XML 实体定义 |
| License | Apache-2.0 |
| 定位 | 完整企业级 ERP 框架（含框架层 + 12 个业务应用） |
| 架构 | 核心框架（entity/service/security/webapp + minilang）+ 12 业务组件 |
| 业务组件数 | 12 个（accounting/order/manufacturing/party/product/workeffort 等） |

### 业务组件清单

| 组件 | 目录 | 描述 |
|------|------|------|
| **accounting** | `applications/accounting/` | 总账、应付/应收、发票、付款、预算、固定资产、税务 |
| **order** | `applications/order/` | 销售/采购订单、报价、退货、购物车、需求 |
| **manufacturing** | `applications/manufacturing/` | BOM、工艺路线、生产运行、MRP、技术日历 |
| **party** | `applications/party/` | 当事人（个人/组织）、协议、联系机制、通信 |
| **product** | `applications/product/` | 产品、类别、目录、价格、促销、设施、库存 |
| **workeffort** | `applications/workeffort/` | 工作任务、工时表、可交付成果、日历 |
| **commonext** | `applications/commonext/` | 跨域扩展（如 NoteData） |
| **content** | `applications/content/` | 内容管理、数据资源、调查 |
| **humanres** | `applications/humanres/` | 职位、技能、资格、聘用 |
| **marketing** | `applications/marketing/` | 活动、联系人列表、线索、机会 |
| **securityext** | `applications/securityext/` | 安全扩展 |
| **datamodel** | `applications/datamodel/` | **共享实体定义（所有域的核心实体真相源）** |

## 2. 核心实体定义架构

OFBiz 的**数据模型实体集中于共享 `datamodel` 组件**，各业务应用仅在自身目录定义视图实体和额外扩展：

### 主要实体模型文件

| 文件 | 绝对路径 |
|------|----------|
| 会计实体模型 | `ofbiz-framework/applications/datamodel/entitydef/accounting-entitymodel.xml` |
| 订单实体模型 | `ofbiz-framework/applications/datamodel/entitydef/order-entitymodel.xml` |
| 当事人实体模型 | `ofbiz-framework/applications/datamodel/entitydef/party-entitymodel.xml` |
| 产品实体模型 | `ofbiz-framework/applications/datamodel/entitydef/product-entitymodel.xml` |
| 制造实体模型 | `ofbiz-framework/applications/datamodel/entitydef/manufacturing-entitymodel.xml` |
| 工作任务实体模型 | `ofbiz-framework/applications/datamodel/entitydef/workeffort-entitymodel.xml` |
| 发货实体模型 | `ofbiz-framework/applications/datamodel/entitydef/shipment-entitymodel.xml` |
| 人力资源实体模型 | `ofbiz-framework/applications/datamodel/entitydef/humanres-entitymodel.xml` |

### 应用特定视图/扩展

| 文件 | 用途 |
|------|------|
| `accounting/entitydef/entitymodel_reports.xml` | 会计报告视图 |
| `order/entitydef/entitymodel_view.xml` | 订单视图实体 |
| `product/entitydef/entitymodel_view.xml` | 产品视图实体 |
| `workeffort/entitydef/entitymodel_view.xml` | 工作任务视图实体 |

## 3. 会计引擎设计（核心参考价值）

### 3.1 总账层次结构

| 实体 | 行号 | 职责 |
|------|------|------|
| `GlAccount` | :2082 | 核心会计科目表（glAccountId, glAccountTypeId, glAccountClassId, accountCode, accountName） |
| `GlAccountClass` | :2117 | 科目分类（资产/负债/权益/收入/费用），含 isAssetClass 标志 |
| `GlAccountType` | :2110 | 科目类型（可层叠：AR, AP, Inventory 等） |
| `GlAccountOrganization` | :2150 | 按组织的科目映射——多公司支持 |
| `GlAccountHistory` | :2167 | 期间余额（openingBalance, postedDebits, postedCredits, endingBalance） |
| `GlAccountGroup` / `GlAccountGroupMember` | :2250 | 财务报表分组 |
| `GlJournal` | :2364 | 日记账（如销售日记账、采购日记账） |
| `GlFiscalType` | :2320 | 会计期间类型（实际、预算、预测） |
| `GlReconciliation` / `GlReconciliationEntry` | :2377 | 银行对账 |

### 3.2 会计交易设计——AcctgTrans 中心枢纽（核心模式）

这是 OFBiz 会计引擎的**核心设计**——`AcctgTrans` 实体作为**所有源单据的统一入账枢纽**：

```text
AcctgTrans（会计交易头）
  ├── invoiceId -> Invoice          （销售/采购发票）
  ├── paymentId -> Payment          （收款/付款）
  ├── fixedAssetId -> FixedAsset    （资产购置/折旧）
  ├── inventoryItemId -> InventoryItem（库存调整）
  ├── shipmentId -> Shipment        （货物移动）
  ├── receiptId -> ShipmentReceipt  （收货）
  ├── workEffortId -> WorkEffort    （生产/人工）
  ├── finAccountTransId -> FinAccountTrans（金融账户）
  └── physicalInventoryId -> PhysicalInventory（盘点差异）

AcctgTransEntry（分录行）
  ├── glAccountId -> GlAccount      （借贷科目）
  ├── organizationPartyId           （公司/部门）
  ├── amount / currencyUomId        （金额/币种）
  ├── origAmount / origCurrencyUomId（原币金额——外币支持）
  ├── debitCreditFlag ('D'/'C')     （借贷方向）
  ├── reconcileStatusId             （对账状态）
  ├── partyId / productId            （维度）
  └── groupId                        （归组分批标识）
```

### 3.3 过账服务与服务 ECA

过账关键服务定义在 `accounting/servicedef/services_ledger.xml`：

| 服务 | 行号 | 职责 |
|------|------|------|
| `quickCreateAcctgTransAndEntries` | :71 | 快速创建一借一贷的会计交易 |
| `completeAcctgTransEntries` | :178 | 使用 GL 设置映射缺失分录 |
| `postGlJournal` | :94 | 过账日记账（isPosted=Y + 更新 GlAccountHistory） |
| `calculateGlJournalTrialBalance` | :85 | 计算试算平衡 |

**服务 ECA（事件-条件-动作）自动过账机制**（`secas_invoice.xml` / `secas_payment.xml`）：
- 当发票状态变为 `INVOICE_READY` → 自动触发创建 `AcctgTrans`（借 AR，贷 Revenue）
- 当付款匹配发票 → 自动触发创建 `AcctgTrans`（借 Cash，贷 AR）
- 采购收货 → 借 Inventory/Expense，贷 AP
- 付款 → 借 AP，贷 Cash

### 3.4 GL 映射系统

OFBiz 使用多层 GL 映射：

| 映射实体 | 行号 | 用途 |
|----------|------|------|
| `InvoiceItemTypeGlAccount` | :1462 | 按发票项目类型 + 组织 → GL 科目 |
| `ProductCategoryGlAccount` | product:321 | 按产品类别 → GL 科目 |
| `FixedAssetTypeGlAccount` | :896 | 资产类型 → 5 个科目（资产/累计折旧/折旧/利润/损失） |
| `VarianceReasonGlAccount` | :2590 | 库存差异原因 → GL 科目 |
| `GlAccountTypeDefault` | :2320 | 每个组织每种科目类型的默认科目 |

### 3.5 对 nop-app-erp 的借鉴意义

- **`AcctgTrans` 中心枢纽模式**——nop 的 finance 域应以类似模式设计一个统一入账中心，关联所有业务源单据（采购入库、销售出库、费用报销、资产变动等），而非分散入账
- **服务 ECA 自动过账**——在 xbiz 中定义事件触发动作，取代硬编码的过账调用
- **多层次 GL 映射**——按源单据类型、产品类别、资产类型逐层映射到科目，避免硬编码科目 ID
- **外币双币种字段**——`origAmount/origCurrencyUomId` 模式可直接复用

## 4. 订单管理（Order-to-Cash / Procure-to-Pay）

### 4.1 核心实体

| 实体 | 行号 | 职责 |
|------|------|------|
| `OrderHeader` | order:415 | 订单头（orderTypeId, statusId, currencyUom, grandTotal, needsInventoryIssuance） |
| `OrderItem` | order:520 | 订单行（productId, quantity, unitPrice, overrideGlAccountId） |
| `OrderAdjustment` | order:120 | 促销/折扣/税费/运费（含 taxAuthPartyId, taxAuthGeoId, overrideGlAccountId） |
| `OrderItemShipGroup` | order:650 | 配送分组（地址、承运人、追踪号） |
| `OrderItemBilling` | order:719 | **订单行→发票行关联（Order-to-Cash 关键联结）** |
| `ReturnHeader` / `ReturnItem` | order:820 | 退货管理 |

### 4.2 端到端流程

```text
Quote → OrderHeader → OrderItem
  |-> OrderItemShipGroup -> Shipment -> ItemIssuance
  |-> OrderItemBilling -> Invoice -> InvoiceItem
  |-> PaymentApplication -> Payment
  |-> AcctgTrans（AR/Revenue/Tax/COGS）
```

### 4.3 关键关联模式

- `OrderItemBilling` 关联实体连接 `OrderItem` 与 `InvoiceItem` + `ItemIssuance`（订单→发票+发货三联）
- `OrderHeaderWorkEffort` 连接订单与工作努力（用于生产订单触发）
- `OrderItem.overrideGlAccountId` 允许行级 GL 科目覆盖（灵活的科目映射）

## 5. 制造（Manufacturing）

### 5.1 核心实体

| 实体 | 行号 | 职责 |
|------|------|------|
| `ProductManufacturingRule` | :43 | BOM 规则（productId→productIdIn+quantity） |
| `TechDataCalendar` | :80 | 工作日历 |
| `TechDataCalendarWeek` | :128 | 每周模板（每天 startTime + capacity） |
| `TechDataCalendarExcDay` | :95 | 每日例外 |
| `MrpEvent` / `MrpEventType` | :156 | MRP 事件 |

### 5.2 生产运行 = WorkEffort

OFBiz 制造的核心模式：**生产运行是一个 `WorkEffortTypeId="PROD_RUN"` 的 WorkEffort**。

```text
Production Run（生产运行）→ WorkEffort
  ├── WorkEffortGoodStandard（BOM 清单需求：原材料 → 产成品）
  ├── WorkEffortFixedAssetAssign（设备/工作中心分配）
  ├── WorkEffortPartyAssignment（人员分配）
  └── WorkEffortInventoryProduced（产出品）
```

**服务文件**（`manufacturing/servicedef/`）：
- `services_production_run.xml` — `createProductionRun`（:76）、`createProductionRunsForOrder`（:123）
- `services_bom.xml` — `createBOMAssoc`（:28）、`updateLowLevelCode`（BOM 遍历）
- `services_mrp.xml` — MRP 运行
- `services_routing.xml` — 工艺路线管理

### 5.3 对 nop 的借鉴意义

- **生产运行 = WorkEffort 子类型**——nop 的 manufacturing 域可将工单作为 workEffort 的扩展，复用项目和工时的关联机制
- **WorkEffortGoodStandard** 作为 BOM 展开的通用方案——物料清单不再仅属于制造域，项目任务的材料需求也可复用

## 6. Party（当事人）与 Product（产品）主数据

### 6.1 Party 通用参与者模式

OFBiz 最核心的设计模式之一：**所有商业实体（客户/供应商/员工/组织）共用 `Party` 表**，通过角色区分：

| 实体 | 行号 | 职责 |
|------|------|------|
| `Party` | framework | 通用参与者（partyId, partyTypeId=PERSON/GROUP, statusId） |
| `Person` | party | 个人子类型（firstName, lastName, birthDate） |
| `PartyGroup` | party | 组织子类型（groupName, taxId） |
| `PartyRole` | party | 角色分配（partyId + roleTypeId） |
| `RoleType` | party | 角色类型目录（客户/供应商/员工/经理等） |
| `PartyRelationship` | party | 相关方关系（partyIdTo/From + roleTypeIdFrom/To + fromDate/thruDate） |
| `ContactMech` | party | 联系机制（电话/邮件/地址） |
| `Agreement` / `AgreementItem` / `AgreementTerm` | party | 协议/合同 |

### 6.2 Product 产品主数据

| 实体 | 行号 | 职责 |
|------|------|------|
| `Product` | framework | 核心产品（productId, productTypeId, productName） |
| `ProductCategory` / `ProductCategoryMember` | product | 产品分类 |
| `ProductPrice` | product | 定价（priceTypeId, fromDate, price） |
| `ProductFeature` / `ProductFeatureAppl` | product | 产品特征/属性 |
| `ProductAssoc` | product | 产品关联（BOM/替代/升级/交叉销售） |
| `ProductAverageCost` | product | 平均成本跟踪 |
| `Facility` / `FacilityLocation` / `InventoryItem` | product | 设施/库位/库存批次 |

### 6.3 跨模块引用模式（对 nop 的关键参考）

OFBiz 使用三种跨模块引用模式：

| 模式 | 描述 | 示例 |
|------|------|------|
| **类型代码 FK** | 简单 FK 引用另一个模块的实体 | `OrderItem.productId -> Product` |
| **关联实体** | 带额外属性的多对多 | `OrderItemBilling`（订单↔发票） |
| **视图实体** | 声明式 SQL JOIN | `AcctgTransAndEntries` 跨 5 实体 |
| **服务 ECA** | 跨模块事件编排 | 发票过账 → GL 交易创建 |
| **中心枢纽** | 一个模块汇总到该中心 | `AcctgTrans` 引用所有源单据 |
| **抽象参与者** | Party 模式——所有当事方用共享表 | 所有模块中的 `partyId` 引用 |

## 7. 通用实体设计模式

### 7.1 Type + Attr 可扩展性

```text
每个业务实体（如 Invoice）都有：
  ├── {Entity}Type（如 InvoiceType），具有 parentTypeId 自引用
  ├── {Entity}TypeAttr（该类型允许的额外属性）
  └── {Entity}Attribute（名称/值对的自定义字段）
```

### 7.2 Status + StatusValidChange 状态机

- 每个业务实体（OrderHeader/Invoice/WorkEffort/Payment）都有 `statusId`
- 状态转换由 `StatusValidChange` 强制执行
- 状态历史存储在 `{Entity}Status` 表

### 7.3 Date-Ranged 有效性

- 大多数关联实体使用 `fromDate` + `thruDate` 表示时间范围（PartyRelationship, ProductCategoryMember, AgreementTerm）

### 7.4 视图实体聚合

OFBiz 使用**视图实体**（声明式 SQL 视图）做汇总：
- `AcctgTransEntrySums` — 按 glAccountId + debitCreditFlag 分组合计
- `GlAccountAndHistoryTotals` — 科目余额合计

## 8. 对 nop-app-erp 的可借鉴设计点

| # | 借鉴点 | OFBiz 证据 | 对 nop 的落地建议 |
|---|--------|-----------|-------------------|
| 1 | **AcctgTrans 统一入账枢纽** | `accounting-entitymodel.xml:1764` | nop finance 域创建 `ErpAcctgTrans`，引用所有业务单据（采购入库/销售出库/资产变动/费用报销） |
| 2 | **服务 ECA 自动过账** | `secas_invoice.xml` / `secas_payment.xml` | 在 xbiz 中通过事件监听器（@OnChange）实现业务单据确认→自动生成凭证 |
| 3 | **多层次 GL 映射** | InvoiceItemTypeGlAccount + ProductCategoryGlAccount + FixedAssetTypeGlAccount | nop 构建`科目映射规则引擎`，按单据类型→产品→资产的优先级解析科目 |
| 4 | **Party 通用参与者** | Party + PartyRole + PartyRelationship | nop master-data 域可使用类似模式，一个 `ErpParty` 表承载客户/供应商/员工 |
| 5 | **WorkEffort 作为生产/工单/项目统一模型** | WorkEffortTypeId=PROD_RUN | nop 的工单/项目/任务共享一个 workEffort 表结构，用 type 区分 |
| 6 | **OrderItemBilling 三联关联** | order:719 | nop 销售出库环节使用关联实体记录 order→ship→invoice 的三方映射 |
| 7 | **外币双币种记账** | AcctgTransEntry.origAmount/origCurrencyUomId | nop finance 凭证分录支持原币/本币双金额 |
| 8 | **视图实体聚合报表** | AcctgTransEntrySums / GlAccountAndHistoryTotals | nop 使用 sql-lib.xml 定义聚合查询，替代 Java 循环汇总 |

## 9. 需注意的潜在借鉴边界

| 方面 | OFBiz 做法 | nop 差异 | 理由 |
|------|-----------|----------|------|
| 实体集中定义 | 所有实体集中在 `datamodel` | 按域拆分多 orm.xml | nop 模块化策略要求域间解耦 |
| 实体属性过多 | 单表字段数达 60+ | 适度拆分 | nop 偏好细粒度实体 |
| 视图实体 | 在实体引擎层做 SQL JOIN | `sql-lib.xml` + BizModel 查询 | nop 推荐在查询层组合 |
| 生成服务 | `entity-auto` 自动 CRUD | `CrudBizModel<T>` | 平台原生支持 |

## 10. 关键证据文件

- `ofbiz-framework/applications/datamodel/entitydef/accounting-entitymodel.xml`（会计实体模型）
- `ofbiz-framework/applications/datamodel/entitydef/order-entitymodel.xml`（订单实体模型）
- `ofbiz-framework/applications/datamodel/entitydef/manufacturing-entitymodel.xml`（制造实体模型）
- `ofbiz-framework/applications/datamodel/entitydef/party-entitymodel.xml`（当事人实体模型）
- `ofbiz-framework/applications/datamodel/entitydef/workeffort-entitymodel.xml`（工作任务实体模型）
- `ofbiz-framework/applications/accounting/servicedef/services_ledger.xml`（总账服务）
- `ofbiz-framework/applications/accounting/servicedef/secas_invoice.xml`（发票自动过账 ECA）
- `ofbiz-framework/applications/accounting/servicedef/secas_payment.xml`（付款自动过账 ECA）
- `ofbiz-framework/applications/manufacturing/servicedef/services_production_run.xml`（生产运行服务）
