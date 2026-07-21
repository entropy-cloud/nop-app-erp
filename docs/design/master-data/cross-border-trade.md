# 跨境贸易扩展（Cross-Border Trade Extensions）

> Owner Doc for `deepening-roadmap.md` §C2。
> Plan: `docs/plans/2026-07-21-1206-1-master-data-cross-border-trade-extensions.md`。
> 相关：`README.md`（域概览）、`unified-party-identity.md`（C1 Party 抽象，本计划复用 `IErpPartyBiz` 但不改其契约）、`docs/architecture/tax-framework.md`（税务框架，本计划「物料层跨境税快查」段回链）、`docs/architecture/l10n-strategy.md`（本地化策略，本计划「原产地与 FTA」段回链）。

## 1. 目的与范围

本 owner doc 定义 nop-app-erp 主数据域的**跨境贸易字段扩展**业务语义：在物料层落地跨境报关高频查询字段（税率/HS 编码/原产地等）+ 新建 per-transaction 报关记录实体（`ErpMdMaterialCustoms`）+ 字典扩展（`CUSTOMS_BROKER` Partner 类型、`customs-preference-code` FTA 协定代码）。

### 边界

| 范围 | 归属 |
|------|------|
| 物料层 9 个跨境字段 + `ErpMdMaterialCustoms` 实体 + 字典扩展 | **本计划（C2）** |
| Partner 抽象 / 跨实体检索（C1） | C1 plan `2026-07-21-0827-2` —— 本计划复用 `IErpPartyBiz` 抽象，**不改其契约**；新增 Partner 类型字典值 `CUSTOMS_BROKER` 仅追加选项，不动接口 |
| 海关 EDI 报文 / 单一窗口接入（b2b 域） | b2b successor（触发：业务客户 EDI 报关需求 + b2b 域 owner doc 授权） |
| 关税/增值税/退税过账 Provider（finance 域） | finance successor（触发：业务客户跨境业务量 > 阈值 + 财务 owner doc 显式授权） |
| 报关单状态机 / 审批流 / 跨域编排 | successor plan（触发：业务客户具体业务流程需求 + 跨域编排 owner doc 授权） |
| HS 编码字典全集 / 第三方 HS 查询服务集成 | successor（触发：业务方明确需求 + 第三方服务集成） |
| 报关明细报表 / 退税统计报表 | report successor（字段基础由本计划提供，报表实施归 report 域） |
| 关税计算引擎（含反倾销税/报复性关税） | finance/tax successor |

> 关键约束：`ErpMdMaterial.defaultTaxRateId`（既有 FK→`ErpMdTaxRate`）保留作详细税率配置入口；本计划新增的 `vatRate`/`drawbackRate` 是**冗余快查字段**，二者并行，业务约定何时使用快查 vs 联查（见 §6 / `tax-framework.md` 物料层跨境税快查段）。

## 2. 物料层跨境字段表

下表对照 nop 当前 / Wimoor / OFBiz / ERP5 的物料层跨境字段集，给出本计划采纳决策。

| 字段（ErpMdMaterial） | code | 类型 / 精度 | 字典 | 默认 | 业务语义 | Wimoor | OFBiz | ERP5 | 采纳 |
|----|----|----|----|----|----|----|----|----|----|
| `vatRate`（增值税率） | VAT_RATE | DECIMAL(6,4) | — | null | 报关场景增值税率快查（如 0.13 表示 13%）。冗余于 `defaultTaxRateId→ErpMdTaxRate.rate`，避免报关高频场景联查；本字段优先级 > 联查值 | `vatrate` ✓ | — | — | ✅（Wimoor 对齐） |
| `drawbackRate`（退税率） | DRAWBACK_RATE | DECIMAL(6,4) | — | null | 出口退税率快查（如 0.13 表示 13%）。中国出口退税核心字段 | `drawbackRate` ✓ | — | — | ✅（Wimoor 对齐） |
| `customsHS`（海关 HS 编码） | CUSTOMS_HS | VARCHAR(12) | — | null | Harmonized System Code，国际贸易通用商品分类编码。国际标准 6 位 + 中国延伸 2/4 位（共 8/10 位）。**不做字典约束**（全集上万条，维护成本高），由业务方自行维护或集成第三方 HS 查询服务 | ✓ | ✓ | ✓ | ✅（VARCHAR(12) 不字典化，参考 Wimoor 设计） |
| `countryOfOrigin`（原产地） | COUNTRY_OF_ORIGIN | VARCHAR(2) | — | null | ISO 3166-1 alpha-2 国家代码（如 `CN`/`US`/`VN`）。FTA 优惠协定判定依据。**不做字典约束**（国家全集 200+ 条，由 ISO 标准维护，业务方按需引用） | ✓ | — | ✓ | ✅（VARCHAR(2) ISO 标准） |
| `preferenceCode`（优惠协定代码） | PREFERENCE_CODE | VARCHAR(20) | `erp-md/customs-preference-code` | null | FTA 优惠协定代码（东盟/RCEP/中韩/中澳/...）。**字典化**（FTA 协定数量有限约 15-20 个且变更慢适合字典化） | — | — | — | ✅（字典化，候选 1） |
| `customsNameCn`（报关中文名） | CUSTOMS_NAME_CN | VARCHAR(200) | — | null | 报关单中文商品名称（中国海关申报用） | ✓ | — | — | ✅ |
| `customsNameEn`（报关英文名） | CUSTOMS_NAME_EN | VARCHAR(200) | — | null | 报关单英文商品名称（Invoice/Packing List 用） | ✓ | — | — | ✅ |
| `declarationUnit`（申报计量单位） | DECLARATION_UNIT | VARCHAR(20) | — | null | 报关法定计量单位（海关法定单位有时与内部 `ErpMdUoM` 字典不同——如海关"千克"法定单位 vs 内部库存单位"件"）。**VARCHAR 而非 FK→ErpMdUoM**，因海关法定单位字典与内部单位字典解耦 | ✓ | — | — | ✅（VARCHAR 解耦海关/内部单位字典） |
| `supervisionCondition`（监管条件代码） | SUPERVISION_CONDITION | VARCHAR(10) | — | null | 中国海关监管条件代码（A/B/...标识进/出口监管要求）。**不做字典约束**（海关总署公布约 100+ 代码且频繁更新，自由 VARCHAR 由业务方维护或集成第三方服务） | — | — | — | ✅（VARCHAR 不字典化，候选 1） |

**统一约束**：

- 全部 `mandatory="false"` + 默认 null + 不 mandatory，**向后兼容**既有 INSERT/UPDATE 测试。
- `ui:show` 按字段语义：`vatRate`/`drawbackRate` 在 grid 列表显示（报关场景高频查询）；`customsNameCn`/`customsNameEn` 仅在 form 显示（避免 grid 列过宽）。
- propId 从 26 起顺序分配（既有 ErpMdMaterial max propId=25 → 新字段从 26 起）。

### 字段冗余 vs 联查权衡（vatRate/drawbackRate）

- **风险**：字段冗余在物料主表，可能多场景下与 `ErpMdTaxRate` 不一致（如税率变更后物料层未同步）。
- **缓解**：默认显示 `defaultTaxRate.rate` 联查值；`vatRate`/`drawbackRate` 字段仅在**报关场景显式覆盖**（业务约定 + 文档化于 `tax-framework.md` 物料层跨境税快查段）。

### 多账套/多公司隔离决策

- **候选 A**（采纳）：物料主数据层**不**按 `orgId` 隔离（同物料在多公司用同 `countryOfOrigin`）。
- **候选 B**（拒绝）：按 orgId 隔离（同一物料在出口公司 vs 内销公司可能不同原产地认定）。
- **特殊场景**：候选 B 的需求由 `ErpMdMaterialCustoms` per-transaction 实体覆盖（per-transaction 级别记录原产地）。

## 3. ErpMdMaterialCustoms 实体设计

per-transaction 报关记录实体，每次报关独立记录报关单号、报关行、报关日期、申报数量/金额、关税/增值税金额、退税收据号、业务单据回链。

### 字段表

| 字段 | code | 类型 / 精度 | mandatory | 默认 | 业务语义 |
|----|----|----|----|----|----|
| `id` | ID | BIGINT | ✓ | seq-default | 主键 |
| `code` | CODE | VARCHAR(50) | ✓ | — | 报关记录编码（业务编码，便于跨系统引用） |
| `materialId` | MATERIAL_ID | BIGINT | ✓ | — | FK→`ErpMdMaterial.id` |
| `declarationNo` | DECLARATION_NO | VARCHAR(50) | ✓ | — | 报关单号（海关分配，UK 强制全局唯一） |
| `partnerId` | PARTNER_ID | BIGINT | — | null | FK→`ErpMdPartner.id`（报关行；Partner 类型必须为 `CUSTOMS_BROKER`，BizModel 校验） |
| `declarationDate` | DECLARATION_DATE | DATE | ✓ | — | 报关日期 |
| `qtyDeclared` | QTY_DECLARED | DECIMAL(20,4) | ✓ | — | 申报数量 |
| `uomDeclared` | UOM_DECLARED | VARCHAR(20) | ✓ | — | 申报计量单位（海关法定单位，**VARCHAR 而非 FK→ErpMdUoM**，因海关法定单位字典与内部单位字典解耦——如"千克"法定单位 vs "件"内部单位） |
| `amountDeclared` | AMOUNT_DECLARED | DECIMAL(18,2) | ✓ | — | 申报金额（原币） |
| `currencyId` | CURRENCY_ID | BIGINT | — | null | FK→`ErpMdCurrency.id`（申报币种） |
| `exchangeRate` | EXCHANGE_RATE | DECIMAL(20,8) | — | null | 报关日汇率（申报币种→本位币） |
| `amountFunctional` | AMOUNT_FUNCTIONAL | DECIMAL(18,2) | — | null | 本位币金额 = `amountDeclared × exchangeRate` |
| `dutyAmount` | DUTY_AMOUNT | DECIMAL(18,2) | — | null | 关税金额（由 finance successor 关税计算引擎填充；本计划仅落地字段） |
| `vatAmount` | VAT_AMOUNT | DECIMAL(18,2) | — | null | 增值税金额（同上） |
| `drawbackReceiptNo` | DRAWBACK_RECEIPT_NO | VARCHAR(50) | — | null | 退税收据号（税务部门分配） |
| `sourceBillType` | SOURCE_BILL_TYPE | VARCHAR(50) | — | null | 业务单据类型（如 `PURCHASE_RECEIVE`/`SALES_SHIP`），与 `sourceBillCode` 二者至少一个非空 |
| `sourceBillCode` | SOURCE_BILL_CODE | VARCHAR(50) | — | null | 业务单据编码（业务回链） |
| `delFlag`/`version`/`createdBy`/`createTime`/`updatedBy`/`updateTime`/`remark` | (标准审计字段) | | | | tagSet `audit,audit-save` |

### Relations

- `material`（to-one, FK `materialId`）→ `ErpMdMaterial`
- `partner`（to-one, FK `partnerId`）→ `ErpMdPartner`（报关行）
- `currency`（to-one, FK `currencyId`）→ `ErpMdCurrency`

### UK + Index

- **UK**：`UK_MD_MATERIAL_CUSTOMS_DECL_NO`（`declarationNo`）—— 海关分配的报关单号全局唯一，DB 层强制
- **Indexes**：
  - `IDX_MD_MATERIAL_CUSTOMS_MATERIAL_ID`（`materialId`）—— 按物料查报关记录
  - `IDX_MD_MATERIAL_CUSTOMS_PARTNER_ID`（`partnerId`）—— 按报关行查
  - `IDX_MD_MATERIAL_CUSTOMS_DECL_DATE`（`declarationDate`）—— 按日期范围查
  - `IDX_MD_MATERIAL_CUSTOMS_SOURCE_BILL`（`sourceBillType`, `sourceBillCode`）—— 业务单据回链查询

### estRows

`ext:estRows="100"` —— 按业务客户报关频次估算（日均 < 100 单/跨境客户），与既有 estRows 范式一致（如 `ErpMdMaterial.skus estRows="10"`）。

### UK + 前置友好校验协同

- **UK=declarationNo** 由 DB 层强制；
- **BizModel.defaultPrepareSave 钩子**在持久化前查询 declarationNo 重复时抛 `ERP_MD_CUSTOMS_DECLARATION_NO_DUPLICATE`（更友好的错误消息 + 避免 DB UK violation stack trace 暴露）；
- **sourceBillType/sourceBillCode 校验**：BizModel.defaultPrepareSave 校验二者之一非空（业务回链必填）；
- **partnerId 报关行校验**：BizModel.defaultPrepareSave 校验 partnerId 引用的 Partner 类型必须为 `CUSTOMS_BROKER`（非此类型抛 `ERP_MD_PARTNER_NOT_CUSTOMS_BROKER`）。

## 4. 报关场景工作流

### 业务流程（语义描述，非状态机实施）

1. **业务单据触发**：业务方从采购入库 / 销售出库等单据发起报关需求，回链记录在 `sourceBillType`/`sourceBillCode`。
2. **报关行选择**：选择已登记为 `CUSTOMS_BROKER` 的 Partner 作为报关行（partnerId）。
3. **申报信息录入**：申报数量（`qtyDeclared`）、申报计量单位（`uomDeclared`）、申报金额（`amountDeclared`）、申报币种（`currencyId`）、报关日汇率（`exchangeRate`）、本位币金额（`amountFunctional`）。
4. **报关单号回填**：海关分配的报关单号（`declarationNo`）回填，UK 强制全局唯一。
5. **关税/增值税金额记录**：`dutyAmount`/`vatAmount`（由 finance successor 关税计算引擎填充；本计划仅落地字段）。
6. **退税收据号回填**：税务部门分配的退税收据号（`drawbackReceiptNo`）。

### 状态字段说明（Non-Goal：状态机实施）

本计划**不**实施报关单状态机（如 DRAFT→DECLARED→CLEARED→DREW_BACK）。`ErpMdMaterialCustoms` 仅记录已完成的报关数据快照。状态机/审批流/与采购入库销售出库的业务联动属跨域编排，归 successor plan（触发：业务客户具体业务流程需求 + 跨域编排 owner doc 授权）。

## 5. FTA 判定流程

### 原产地（countryOfOrigin）+ 优惠协定代码（preferenceCode）配合

FTA（Free Trade Agreement）优惠协定判定的字段基础：

1. **原产地录入**：物料主表 `countryOfOrigin`（ISO 3166-1 alpha-2）记录原产国。
2. **优惠协定选择**：报关时根据原产国 + 进口国选择适用的 FTA 协定代码（`preferenceCode` 字典值：`ASEAN`/`CKFTA`/`CHAFTA`/...）。
3. **税率优惠应用**：FTA 协定生效时，适用协定优惠税率（如东盟协定下中国→东盟成员国零关税）。

### 判定流程概要

```
物料 countryOfOrigin + 报关单 进口国/出口国
  ↓
FTA 协定适用性判定（人工或第三方服务）
  ↓
选择 preferenceCode（如 ASEAN）
  ↓
适用协定优惠税率
  ↓
记录在 ErpMdMaterialCustoms（per-transaction，可能因报关场景不同而override物料层preferenceCode）
```

> **详细 FTA 判定算法**（含原产地认定规则、增值比例计算、直接运输规则等）属关税计算引擎，归 finance/tax successor。本计划仅提供字段基础。

## 6. 与既有 owner doc 关系

### `docs/architecture/tax-framework.md` —— 物料层跨境税快查段（Phase 3 增量）

- `vatRate`/`drawbackRate` 字段语义；
- 与 `defaultTaxRateId` 联查路径的双轨设计；
- 报关场景何时使用快查 vs 联查；
- 与 `ErpMdMaterialCustoms.dutyAmount`/`vatAmount` 字段的关系。

### `docs/architecture/l10n-strategy.md` —— 原产地与 FTA 段（Phase 3 增量）

- `countryOfOrigin` 字段（ISO 3166-1 alpha-2）；
- `preferenceCode` 字典（FTA 协定代码）；
- FTA 判定流程概要（详细见本 doc §5）。

### `docs/design/master-data/README.md` —— 跨境贸易扩展段（Phase 3 增量）

- §核心业务对象段增 `ErpMdMaterialCustoms` 行（报关记录）；
- 物料层跨境字段概述；
- 回链本 doc。

### C1 关系（`unified-party-identity.md`）

- 本计划复用 `IErpPartyBiz` 抽象（Partner 抽象基础），但**不改 C1 接口契约**；
- 新增 Partner 类型字典值 `CUSTOMS_BROKER`（仅追加选项到 `erp-md/partner-type` 字典，不动 `IErpPartyBiz` 接口签名 / `PartyRef` DTO / SPI 端口）。

## 7. 反模式自检表

| 反模式 | 正确做法 |
|--------|---------|
| 直接修改 C1 `IErpPartyBiz` 接口签名或 `PartyRef` DTO | 仅在 `erp-md/partner-type` 字典追加 `CUSTOMS_BROKER` 选项 |
| 在 `ErpMdMaterialCustoms.BizModel` 中跨域调用 finance 关税计算 | 本计划 Non-Goal：finance Provider 接入归 successor；BizModel 仅校验 sourceBill/partnerType + CRUD |
| 在 `ErpMdMaterialCustoms` 实施 status 状态机字段 | 本计划 Non-Goal：状态机/审批流归 successor plan |
| `uomDeclared` 用 FK→`ErpMdUoM` | 海关法定单位与内部单位字典解耦，用 VARCHAR(20) |
| `customsHS`/`supervisionCondition` 字典化 | 全集过大（HS 上万条/监管条件 100+ 频繁更新），用自由 VARCHAR，业务方自行维护 |
| `vatRate`/`drawbackRate` 默认显示覆盖 `defaultTaxRate.rate` 联查值 | 默认显示联查值，本字段仅在报关场景显式覆盖 |
| `countryOfOrigin` 按 orgId 隔离 | 候选 A：物料主数据层不隔离；per-transaction 差异由 `ErpMdMaterialCustoms` 覆盖 |
| 在 `ErpMdMaterial` view.xml grid 列显示 `customsNameCn`/`customsNameEn` | grid 列过宽；仅 form 分组显示 |
| `ErpMdMaterialCustoms.BizModel.defaultPrepareSave` 不校验 partnerId Partner 类型 | 必须校验 `partnerType=CUSTOMS_BROKER`，抛 `ERP_MD_PARTNER_NOT_CUSTOMS_BROKER` |
| 不前置校验 declarationNo 重复（依赖 DB UK violation） | BizModel 钩子前置校验抛友好错误 `ERP_MD_CUSTOMS_DECLARATION_NO_DUPLICATE` |
| 在本计划接入 finance 关税过账 Provider | 本计划 Non-Goal；successor 触发条件：业务客户跨境业务量 > 100 单/月 或 财务 owner doc 显式授权 |

## 8. 落地证据

（本计划完成后填，见 `docs/plans/2026-07-21-1206-1-master-data-cross-border-trade-extensions.md` Phase 3 §deepening-roadmap.md §8.3 落地证据段落）

- Plan: `docs/plans/2026-07-21-1206-1-master-data-cross-border-trade-extensions.md`（4 Phase 全 done）
- Owner Doc：本文件（8 节完整）
- ORM 变更：`ErpMdMaterial` 增 9 字段（propId 26-34）+ 新建 `ErpMdMaterialCustoms` 实体 + `erp-md/customs-preference-code` 字典 + `erp-md/partner-type` 增 `CUSTOMS_BROKER`
- Codegen 产物：ErpMdMaterialCustoms.java + Entity + DAO + BizModel + IBiz + xmeta + view.xml + page.yaml 骨架
- 测试基线：`TestErpMdMaterialCustoms`（NEW，至少 3 场景：CRUD 生命周期 + partnerId 报关行类型校验 + sourceBillType/Code 业务回链校验）+ master-data service 既有测试无回归
- Deferred successor：finance 关税/退税 Provider 接入 / b2b 海关 EDI 报文 / HS 编码字典全集 / ErpMdMaterialSku 跨境字段 / 海关申报完整业务流程编排 / 跨境报表实施 / 关税计算引擎
