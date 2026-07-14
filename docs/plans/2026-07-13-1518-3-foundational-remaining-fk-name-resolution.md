# 2026-07-13-1518-3-foundational-remaining-fk-name-resolution Master Data + Logistics + Contract + B2B + DRP + APS 域外键名称解析批量推广（列表页 ID→名称）

> Plan Status: completed
> Last Reviewed: 2026-07-13
> Source: `docs/plans/2026-07-11-1643-1-amis-frontend-quality.md` Deferred「全量 1,036 FK 列名称解析」（Successor Required: yes，触发条件已满足）
> Related: `2026-07-11-1643-1-amis-frontend-quality.md`（机制 D 范式源）、`2026-07-13-1518-1-crm-cs-fk-name-resolution.md`（同批 N=1，无依赖）、`2026-07-13-1518-2-hr-fk-name-resolution.md`（同批 N=2，无依赖）
> Audit: required

## Current Baseline

经实时仓库核实（HEAD 范围，独立子代理全量盘点 6 域 ORM + 生成网格 + 生成 xmeta）：

### 机制 D 已验证（全域 12 批次 79+ 实体）

机制 D 三层接线已由 12 前序批次验证。

### 覆盖现状

全部 6 域实体的自定义 view.xml 为空 `<grid id="list"/>`。72 实体清单中 66 需机制 D 变更，4 无 FK 列仅记录，1 有 FK 但无 ext:relation 归 Deferred（ErpMdSettlementMethod），1 无 FK 列仅记录。

> **重要**：列名须严格对齐 ORM 实际 prop 名（如 `uoMId` 非 `uomId`、`defaultTaxRateId` 非 `taxRateId`、`freightCurrencyId` 非 `currencyId`）。`ownerId`/`approvedById` 等 userId VARCHAR 列非数值 FK。
>
> **第二跳（目标显示列存在性）补核（2026-07-14 审计）**：机制 D 的"第二跳"——目标实体是否存在可用显示列——经全量 ORM 核实后裁决如下：(1) **P0**：`configId@ErpB2bMftLog` 目标 `ErpB2bMftConfig` 无 `name`/`code`/任何标识性字符串列（仅 protocol/transportEndpoint/AS2 ID 等技术字段），归 Deferred 保留原始 ID；ErpB2bMftLog 仍属机制 D 实体（orgId 正常解析 →orgName）。(2) **P1**：`contractLineId`/`contractVersionId`/`drpLineId` 目标仅有 Integer 行号/版本号，裁决以 `getLineNo()`/`getVersionNo()`（Integer→String）作 fallback 显示。(3) **P2**：`providerRequestId@ErpCtSignatureRequest` 为 VARCHAR(200) 自引用 to-one，按 VARCHAR 非数值 FK 规则排除。(4) **P3**：Goals 显示 getter 以实现时 ORM 实际列为准（维度型读 `.getName()`、交易型/父单型读 `.getCode()` 或专用名称列、行号型读 `.getLineNo()`/`.getVersionNo()`）。详见 Goals 与各 Phase Decision。

### Master Data 域（24 实体，19 需机制 D）

| # | 实体 | 生成网格 FK 列 |
|---|------|---------------|
| 1 | **ErpMdMaterial** | categoryId, uoMId, defaultWarehouseId, defaultTaxRateId |
| 2 | **ErpMdMaterialCategory** | parentId |
| 3 | **ErpMdMaterialSku** | materialId, uoMId, taxRateId |
| 4 | **ErpMdPartnerAddress** | partnerId |
| 5 | **ErpMdPartnerContact** | partnerId |
| 6 | **ErpMdWarehouse** | orgId, managerId |
| 7 | **ErpMdLocation** | warehouseId, parentId |
| 8 | **ErpMdUoMConversion** | materialId, fromUoMId, toUoMId |
| 9 | **ErpMdExchangeRate** | fromCurrencyId, toCurrencyId |
| 10 | **ErpMdSettlementMethod** | defaultFundAccountId‡ |
| 11 | **ErpMdBankAccount** | partnerId |
| 12 | **ErpMdEmployee** | orgId, partnerId |
| 13 | **ErpMdSubject** | parentId, currencyId |
| 14 | **ErpMdAcctSchema** | orgId, functionalCurrencyId |
| 15 | **ErpMdAcctSchemaCoa** | acctSchemaId |
| 16 | **ErpMdSubjectMapping** | sourceSubjectId, targetAcctSchemaId, targetSubjectId |
| 17 | **ErpMdOrganization** | parentId, functionalCurrencyId |
| 18 | **ErpMdCostCenter** | orgId, managerId, parentId |
| 19 | **ErpMdSupplierApproval** | partnerId, orgId, materialCategoryId |
| 20 | **ErpSysConfig** | orgId |
| 21 | ErpMdPartner | — 无 FK 列 |
| 22 | ErpMdUoM | — 无 FK 列 |
| 23 | ErpMdCurrency | — 无 FK 列 |
| 24 | ErpMdTaxRate | — 无 FK 列 |

> ‡ = 无 ext:relation，归 Deferred。managerId 读 `ErpMdEmployee.name`。

### Logistics 域（7 实体，全部需机制 D）

| # | 实体 | 生成网格 FK 列 |
|---|------|---------------|
| 1 | **ErpLogCarrier** | orgId, partnerId |
| 2 | **ErpLogCarrierConfig** | carrierId, orgId |
| 3 | **ErpLogShipment** | orgId, carrierId, carrierConfigId, freightCurrencyId, shipperId |
| 4 | **ErpLogShipmentLine** | shipmentId, materialId |
| 5 | **ErpLogShipmentParcel** | shipmentId |
| 6 | **ErpLogShipmentLog** | shipmentId, orgId |
| 7 | **ErpLogDeliveryWindow** | partnerId, orgId |

> shipperId 读 `ErpMdEmployee.name`。freightCurrencyId 读 `ErpMdCurrency.name`。sender/receiver 为文本字段非 FK。

### Contract 域（15 实体，14 需机制 D）

| # | 实体 | 生成网格 FK 列 |
|---|------|---------------|
| 1 | **ErpCtContract** | orgId, partnerId, currencyId, templateId, parentContractId |
| 2 | **ErpCtContractLine** | contractId, materialId |
| 3 | **ErpCtContractVersion** | contractId |
| 4 | **ErpCtInvoicePlan** | contractLineId |
| 5 | **ErpCtConsumptionLine** | contractLineId |
| 6 | **ErpCtApprovalMatrix** | orgId |
| 7 | **ErpCtApprovalRecord** | contractId, orgId, approvalMatrixId |
| 8 | **ErpCtVolumeDiscount** | contractLineId, orgId |
| 9 | **ErpCtRebateAgreement** | orgId, contractId, partnerId |
| 10 | **ErpCtRebateTier** | rebateAgreementId |
| 11 | **ErpCtRebateAccrual** | rebateAgreementId, orgId |
| 12 | **ErpCtRebateSettlement** | rebateAgreementId, orgId |
| 13 | **ErpCtSignatureRequest** | orgId, contractVersionId |
| 14 | **ErpCtDocument** | orgId, contractId |
| 15 | ErpCtTemplate | — 无 FK 列 |

### B2B 域（13 实体，全部需机制 D）

| # | 实体 | 生成网格 FK 列 |
|---|------|---------------|
| 1 | **ErpB2bEdiFormat** | orgId |
| 2 | **ErpB2bEdiDoc** | orgId, formatId |
| 3 | **ErpB2bAsn** | orgId, sourceEdiDocId, partnerId |
| 4 | **ErpB2bAsnLine** | asnId, materialId |
| 5 | **ErpB2bCodeMapping** | orgId, partnerId |
| 6 | **ErpB2bEdiLog** | ediDocId, orgId |
| 7 | **ErpB2bPartnerProfile** | orgId, partnerId |
| 8 | **ErpB2bPartnerCredential** | partnerProfileId |
| 9 | **ErpB2bTestExchange** | partnerProfileId |
| 10 | **ErpB2bCertificationChecklist** | partnerProfileId |
| 11 | **ErpB2bMftConfig** | orgId, partnerId, certId |
| 12 | **ErpB2bMftCertificate** | orgId, partnerId |
| 13 | **ErpB2bMftLog** | orgId † |

> † `configId@ErpB2bMftLog` 目标 `ErpB2bMftConfig` 经 ORM 核实**无 `name`/`code`/任何标识性字符串列**（列集合 = {id, orgId, partnerId, protocol, transportEndpoint, localAs2Id, remoteAs2Id, sftpUsername, ...}），机制 D 无法产生有意义显示值，归 Deferred 保留原始 ID（P0 裁决，见 Deferred But Adjudicated）。ErpB2bMftLog 仍属机制 D 实体——orgId 正常解析 →orgName。故 B2B 域仍为 13 个机制 D 实体（ErpB2bMftLog 计入），仅 configId 一列转 Deferred。

### DRP 域（7 实体，全部需机制 D）

| # | 实体 | 生成网格 FK 列 |
|---|------|---------------|
| 1 | **ErpDrpPlan** | orgId |
| 2 | **ErpDrpLine** | planId, materialId, warehouseId, sourceWarehouseId, orgId |
| 3 | **ErpDrpParameter** | warehouseId, materialId, preferredSourceWarehouseId, preferredSupplierId, orgId |
| 4 | **ErpInvDrpSafetyStockCalc** | orgId, materialId, warehouseId |
| 5 | **ErpInvDrpCrossDock** | orgId, drpLineId, inboundMoveId, outboundMoveId, materialId, stagingLocationId |
| 6 | **ErpInvDrpDockAppointment** | warehouseId, dockId, crossDockId, orgId |
| 7 | **ErpInvDrpLeadTimeRecord** | orgId, supplierId, materialId |

> inboundMoveId/outboundMoveId 读 `ErpInvStockMove`（跨域弱引用，有 ext:relation 可加载）。supplierId/preferredSupplierId 读 `ErpMdPartner.name`。

### APS 域（6 实体，全部需机制 D）

| # | 实体 | 生成网格 FK 列 |
|---|------|---------------|
| 1 | **ErpApsOperationOrder** | workOrderId‡, machineId‡, orgId |
| 2 | **ErpApsSchedule** | orgId |
| 3 | **ErpApsConstraint** | machineId‡, orgId |
| 4 | **ErpApsOpRouting** | orgId, operationId‡, machineId‡ |
| 5 | **ErpApsDispatchRule** | orgId, workcenterId‡ |
| 6 | **ErpApsDispatchLog** | orgId, operationOrderId, workcenterId‡ |

> ‡ = 无 ext:relation（APS→manufacturing 跨域弱指针，ORM 未声明 to-one），保留原始 ID。operationOrderId@DispatchLog 有 ext:relation→ErpApsOperationOrder.name。

### ext:relation 缺口（保留原始 ID 的 FK 列）

| # | 实体 | FK prop | 处置 |
|---|------|---------|------|
| 1 | ErpMdSettlementMethod | defaultFundAccountId | 保留原始 ID（无 to-one） |
| 2 | ErpApsOperationOrder | workOrderId | 保留原始 ID（跨域→mfg 弱指针） |
| 3 | ErpApsOperationOrder | machineId | 保留原始 ID（跨域→mfg 弱指针） |
| 4 | ErpApsConstraint | machineId | 保留原始 ID |
| 5 | ErpApsOpRouting | operationId | 保留原始 ID |
| 6 | ErpApsOpRouting | machineId | 保留原始 ID |
| 7 | ErpApsDispatchRule | workcenterId | 保留原始 ID |
| 8 | ErpApsDispatchLog | workcenterId | 保留原始 ID |

剩余差距：66 实体列表页显示原始数字 ID（用户面 P1 缺陷）。4 无 FK + 1 ext:relation 缺口仅记录。

## Goals

- 66 实体列表页的高价值用户面 FK 列显示名称而非原始 ID（经机制 D）。
- 高价值 FK 定义（**注：以下显示 getter 以实现时 ORM 实际列为准；维度型主数据（Organization/Partner/Material/Currency/Employee/Warehouse/Location/MaterialCategory/UoM/TaxRate/Subject/AcctSchema/Template）读 `.getName()`，交易型/父单型实体读 `.getCode()` 或其专用名称列（如 `getContractName`/`getPlanName`/`getCarrierName`/`getFormatName`/`getPartnerName`/`getCertName`/`getOperationName`），行号型读 `.getLineNo()`/`.getVersionNo()`（Integer→String）。`configId@ErpB2bMftLog` 目标无显示列，保留原始 ID（见 Deferred）**）：维度型外键（org→orgName 读 ErpMdOrganization.getName；partner/supplier/preferredSupplier→partnerName 读 ErpMdPartner.getName；material→materialName 读 ErpMdMaterial.getName；currency/functionalCurrency/freightCurrency→currencyName 读 ErpMdCurrency.getName；manager/shipper→employeeName 读 ErpMdEmployee.getName；warehouse/defaultWarehouse/sourceWarehouse/preferredSourceWarehouse→warehouseName 读 ErpMdWarehouse.getName；location/stagingLocation→locationName 读 ErpMdLocation.getName；category/materialCategory→categoryName 读 ErpMdMaterialCategory.getName；uoM/fromUoM/toUoM→uomName 读 ErpMdUoM.getName；taxRate/defaultTaxRate→taxRateName 读 ErpMdTaxRate.getName；subject→subjectName 读 ErpMdSubject.getName；acctSchema/targetAcctSchema→acctSchemaName 读 ErpMdAcctSchema.getName；parent→parentName 自引用；template→templateName 读 ErpCtTemplate.getName）；交易型/父单型外键（carrier→carrierName 读 ErpLogCarrier.getCarrierName；carrierConfig→carrierConfigName 读 ErpLogCarrierConfig.getConfigCode；format→formatName 读 ErpB2bEdiFormat.getFormatName；contract/parentContract→contractName 读 ErpCtContract.getContractName；contractLine→contractLineName 读 ErpCtContractLine.getLineNo（Integer→String，P1 裁决）；contractVersion→contractVersionName 读 ErpCtContractVersion.getVersionNo（Integer→String，P1 裁决）；approvalMatrix→approvalMatrixName 读 ErpCtApprovalMatrix.getCode；rebateAgreement→rebateAgreementName 读 ErpCtRebateAgreement.getCode；plan→planName 读 ErpDrpPlan.getPlanName；drpLine→drpLineName 读 ErpDrpLine.getLineNo（Integer→String，P1 裁决）；crossDock/dock→crossDockName 读 ErpInvDrpCrossDock.getCode；ediDoc/sourceEdiDoc→ediDocName 读 ErpB2bEdiDoc.getCode；asn→asnName 读 ErpB2bAsn.getCode；partnerProfile→partnerProfileName 读 ErpB2bPartnerProfile.getPartnerName；cert→certName 读 ErpB2bMftCertificate.getCertName；shipment→shipmentName 读 ErpLogShipment.getCode；operationOrder→operationOrderName 读 ErpApsOperationOrder.getOperationName；stockMove/inboundMove/outboundMove→stockMoveName 读 ErpInvStockMove.getCode）。**`config→configName 读 ErpB2bMftConfig.name` 已删除**——目标 ErpB2bMftConfig 无显示列，configId@ErpB2bMftLog 保留原始 ID（P0 Deferred）。
- 8 个 ext:relation 缺口 FK 保留原始 ID，归 successor。
- 派生 prop 名遵循 `{relation}Name`/`{relation}Code` 约定。
- 零 ORM/契约变更。

## Non-Goals

- **CRM/CS/HR 域 FK 名称解析**——由 `2026-07-13-1518-1/2` 承接。
- **codegen 模板层 FK 名称解析方案**——经 0600-1 裁决否决。
- **drawer 子表/明细行子网格 FK 名称**——本计划仅处理主列表网格 `<grid id="list">`。
- **4 个无 FK 列 Master Data 实体**（ErpMdPartner/ErpMdUoM/ErpMdCurrency/ErpMdTaxRate）+ ErpCtTemplate——仅记录，不做机制 D 变更。
- **8 个 ext:relation 缺口 FK**——保留原始 ID，归 successor（触发条件：ext:relation 落地时）。
- **看板/报表 FK 名称**——已由 1643-1 Phase 4 覆盖。

## Task Route

- Type: `app-layer design change`
- Owner Docs: `docs/architecture/view-and-page-strategy.md`、`../nop-entropy/docs-for-ai/02-core-guides/cross-module-entity-reference.md`、`../nop-entropy/docs-for-ai/03-runbooks/add-bizloader-field.md`
- Skill Selection Basis: xmeta 派生 + view.xml bounded-merge → `nop-frontend-dev`；BizModel `@BizLoader` → `nop-backend-dev`；JUnit 测试 → `nop-testing`。
- Protected Areas: 无 ORM/ask-first 变更。

## Infrastructure And Config Prereqs

No infra prereqs beyond existing baseline。

## Execution Plan

### Phase 1 - Master Data 域 FK 名称解析（19 实体含机制 D 变更）

Status: completed
Targets: `module-master-data/erp-md-meta/.../ErpMd*/ErpMd*.xmeta`；`module-master-data/erp-md-service/.../entity/ErpMd*BizModel.java`；`module-master-data/erp-md-web/.../ErpMd*/ErpMd*.view.xml`
Skill: `nop-frontend-dev`、`nop-backend-dev`

- Item Types: `Decision | Add`
- Prereqs: 无

- **实施指导（防御模式，2026-07-14 审计 MINOR-2）**：全域 BizLoader 统一加 `row.orm_attached() && row.getRelation() != null ? … : null` 短路守卫（防御返回内存聚合实体的 BizModel 方法，先例 CRM/CS 批次 1518-1）；有复杂 mutation 的实体加 `safeBatchLoad` try-catch 降级（防御会话关闭，先例 manufacturing 批次 1043-2）。行号/版本号 fallback（`getLineNo()`/`getVersionNo()`）同样在此守卫内取值。

- [x] `Decision`: 裁决 master-data 19 实体目标 FK 清单（经 ORM 核实：uoMId 非 uomId、defaultTaxRateId 非 taxRateId、functionalCurrencyId 非 currencyId）。1 个 ext:relation 缺口（defaultFundAccountId@SettlementMethod）保留原始 ID。4 无 FK 实体不做变更。
  - Skill: `nop-backend-dev`
- [x] `Add`: 19 实体 xmeta 增派生 `*Name` prop。
  - Skill: `nop-frontend-dev`
- [x] `Add`: 19 实体 BizModel 增 `@BizLoader(forType = ErpMd*.class)` 方法。
  - Skill: `nop-backend-dev`
- [x] `Add`: 19 实体 view.xml `<grid id="list">` 改 `<cols x:override="bounded-merge">`。
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] 19 master-data 实体列表网格显示 `*Name` 而非原始 `*Id`

### Phase 2 - Logistics 域 FK 名称解析（7 实体）

Status: completed
Targets: 7 实体的 xmeta + BizModel + view.xml
Skill: `nop-frontend-dev`、`nop-backend-dev`

- Item Types: `Decision | Add`
- Prereqs: Phase 1 范式已验证

- [x] `Decision`: 裁决 logistics 7 实体 FK 清单（freightCurrencyId 非 currencyId、shipperId→ErpMdEmployee、carrierConfigId→ErpLogCarrierConfig）。sender/receiver 为文本字段非 FK 不涉及。
  - Skill: `nop-backend-dev`
- [x] `Add`: 7 实体 xmeta + BizModel + view.xml 三层接线。
  - Skill: `nop-frontend-dev`、`nop-backend-dev`

Exit Criteria:

- [x] 7 logistics 实体列表网格显示 `*Name` 而非原始 `*Id`

### Phase 3 - Contract 域 FK 名称解析（14 实体，ErpCtTemplate 无 FK 除外）

Status: completed
Targets: 14 实体的 xmeta + BizModel + view.xml
Skill: `nop-frontend-dev`、`nop-backend-dev`

- Item Types: `Decision | Add`
- Prereqs: Phase 2 范式已验证

- [x] `Decision`: 裁决 contract 14 实体 FK 清单（contractLineId 非 contractId@InvoicePlan/ConsumptionLine/VolumeDiscount；rebateAgreementId 非 agreementId；contractVersionId@SignatureRequest；approvalMatrixId@ApprovalRecord）。
  - **P1 裁决（第二跳，2026-07-14 审计）**：`contractLineId`（InvoicePlan/ConsumptionLine/VolumeDiscount）目标 `ErpCtContractLine` 与 `contractVersionId`（SignatureRequest）目标 `ErpCtContractVersion` 经 ORM 核实**无 `name`/`code`**（前者仅 `lineNo`(Int)/`description`，后者仅 `versionNo`(Int)）。裁决以 `ErpCtContractLine.getLineNo()`（Integer→String）作 `contractLineName`、`ErpCtContractVersion.getVersionNo()`（Integer→String）作 `contractVersionName`。理由：行号/版本号比 raw database ID 对用户更直观（"行 1"优于"42"），且前序 HR 批次 `ErpHrTimesheet.getCode()` 已建立非 `.getName()` getter 先例。两 FK 均有 to-one，`batchLoadProps` 可用。
  - **P2 裁决（隐藏假设）**：`providerRequestId@ErpCtSignatureRequest` 为 VARCHAR(200) 列且有自引用 to-one `providerRequest→ErpCtSignatureRequest`，按"VARCHAR 列非数值 FK"规则排除（与 `ownerId`/`approvedById` userId 列一致），Phase 3 表仅列 `orgId, contractVersionId`，不涉及 providerRequestId 名称解析。
  - Skill: `nop-backend-dev`
- [x] `Add`: 14 实体 xmeta + BizModel + view.xml 三层接线。
  - Skill: `nop-frontend-dev`、`nop-backend-dev`

Exit Criteria:

- [x] 14 contract 实体列表网格显示 `*Name` 而非原始 `*Id`

### Phase 4 - B2B 域 FK 名称解析（13 实体）

Status: completed
Targets: 13 实体的 xmeta + BizModel + view.xml
Skill: `nop-frontend-dev`、`nop-backend-dev`

- Item Types: `Decision | Add`
- Prereqs: Phase 3 范式已验证

- [x] `Decision`: 裁决 b2b 13 实体 FK 清单（sourceEdiDocId 非 materialId@Asn；partnerProfileId 非 partnerId@Credential/TestExchange/CertificationChecklist；ediDocId 非 docId@EdiLog；certId@MftConfig）。
  - **P0 裁决（第二跳，2026-07-14 审计）**：`configId@ErpB2bMftLog` 目标 `ErpB2bMftConfig` 经 ORM 核实无 `name`/`code`/任何标识性字符串列（仅 protocol/transportEndpoint/AS2 ID 等技术字段），`getConfig().getName()` 将编译失败。裁决 configId **保留原始 ID**，归 `Deferred But Adjudicated`（out-of-scope improvement，successor required）。ErpB2bMftLog 仍属机制 D 实体（orgId→orgName 正常解析），故 B2B 域仍为 13 个机制 D 实体。
  - Skill: `nop-backend-dev`
- [x] `Add`: 13 实体 xmeta + BizModel + view.xml 三层接线。
  - Skill: `nop-frontend-dev`、`nop-backend-dev`

Exit Criteria:

- [x] 13 b2b 实体列表网格显示 `*Name` 而非原始 `*Id`

### Phase 5 - DRP + APS 域 FK 名称解析（7 + 6 = 13 实体）

Status: completed
Targets: 13 实体的 xmeta + BizModel + view.xml
Skill: `nop-frontend-dev`、`nop-backend-dev`

- Item Types: `Decision | Add`
- Prereqs: Phase 4 范式已验证

- [x] `Decision`: 裁决 DRP + APS 13 实体 FK 清单。DRP（sourceWarehouseId/preferredSourceWarehouseId/preferredSupplierId/inboundMoveId/outboundMoveId/drpLineId/stagingLocationId 经 ORM 核实列名）。APS 8 个无 ext:relation 的 FK（workOrderId/machineId×3/operationId/workcenterId×2）保留原始 ID，仅 orgId + operationOrderId@DispatchLog 做名称解析。
  - **P1 裁决（第二跳，2026-07-14 审计）**：`drpLineId`（InvDrpCrossDock）目标 `ErpDrpLine` 经 ORM 核实**无 `name`/`code`**（仅 `lineNo`(Int)）。裁决以 `ErpDrpLine.getLineNo()`（Integer→String）作 `drpLineName`（与 contractLineId/contractVersionId 同一裁决逻辑）。FK 有 to-one，`batchLoadProps` 可用。
  - Skill: `nop-backend-dev`
- [x] `Add`: 13 实体 xmeta + BizModel + view.xml 三层接线（APS 仅 orgId/operationOrderId 列做机制 D）。
  - Skill: `nop-frontend-dev`、`nop-backend-dev`

Exit Criteria:

- [x] 13 DRP+APS 实体列表网格显示 `*Name`（APS 仅 orgName/operationOrderName，其余 FK 保留原始 ID）

### Phase 6 - BizLoader 测试验证

Status: completed
Targets: 6 域各 1 个 Test*FkNameLoader.java
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1-5 完成

- [x] `Add`: 新建 6 域 `Test*FkNameLoader.java`（extends `JunitAutoTestCase`，各 2-3 测试）。抽样断言——master-data: ErpMdMaterial（categoryName/uomName/defaultWarehouseName）+ ErpMdEmployee（orgName/partnerName）；logistics: ErpLogShipment（carrierName/carrierConfigName/freightCurrencyName/shipperName）；contract: ErpCtContract（partnerName/currencyName/templateName）；b2b: ErpB2bAsn（sourceEdiDocName/partnerName）；drp: ErpDrpLine（materialName/warehouseName/sourceWarehouseName）；aps: ErpApsOperationOrder（orgName only）。
  - Skill: `nop-testing`

Exit Criteria:

- [x] 全 6 域 `Test*FkNameLoader` 全方法绿

## Draft Review Record

- Independent draft review iteration 1: needs revision (`ses_0a4a3d5c9ffefxBm3Ex0uLX2A7`，general agent) — MAJOR：实体计数算术错误（72≠67，正确为 66 机制 D 变更）；ErpMdSettlementMethod 有 defaultFundAccountId FK 被误标"无 FK"。
- Independent draft review iteration 2: needs revision (`ses_0a4923690ffe10aPiSK207tKvj`，general agent) — BLOCKER：ErpMdMaterial FK 列错（uomId→uoMId、taxRateId→defaultTaxRateId、defaultSupplierId→defaultWarehouseId）、ErpLogShipment FK 列错（senderId/receiverId 不存在→carrierConfigId/shipperId/freightCurrencyId）、ErpMdSettlementMethod 表条目未修。经独立子代理全量 6 域 ORM+生成网格重新盘点后，全部实体表已按真实列名重建（发现全域系统性列名错误跨 6 域 72 实体，含 contract/b2b/drp/aps 域大量虚构列名）。
- Independent draft review iteration 3: accept (`ses_0a4864477ffePNItqPekIEULgs`，general agent 新会话) — 全部 iteration 2 BLOCKER 已修正：ErpMdMaterial/ErpLogShipment/ErpMdEmployee/ErpCtInvoicePlan/ErpB2bPartnerCredential/ErpDrpLine 7 实体 FK 列经 ORM 逐一核实正确。66 实体计数算术确认（66 机制 D + 4 无 FK + 1 ext:relation 缺口 + 1 无 FK = 72）。8 个 ext:relation 缺口（1 MD + 7 APS）全部确认。0 Blocker / 0 Major / 1 Minor（uomName vs uoMName 显示标签叙事不一致，实现时自然遵循 ORM 关系名，不阻塞）。
  - 补注（2026-07-14 多维+开放式审计）：iteration 1-3 FK **列名**（leftProp / 第一跳）已逐一核实正确；**目标实体显示列存在性**（第二跳）本次审计补核，发现 1 P0（configId@ErpB2bMftLog 目标无显示列）+ 3 P1（contractLineId/contractVersionId/drpLineId 仅行号/版本号）+ 1 P2（providerRequestId VARCHAR 自引用）+ P3 叙事（Goals ~15 处 `.name` 实为 `.getCode()`/专用名称列），已由修订计划 `2026-07-14-0930-1-1518-3-audit-revisions` 逐条裁决修正（P0 归 Deferred / P1 lineNo·versionNo fallback / P2 排除说明 / P3 Goals 顶部加注+逐条订正）。

## Closure Gates

> `mvn clean install -DskipTests`（154 模块）+ `mvn test -pl module-master-data/erp-md-service,module-logistics/erp-log-service,module-contract/erp-ct-service,module-b2b/erp-b2b-service,module-drp/erp-drp-service,module-aps/erp-aps-service -am` + 66 view.xml `xmllint --noout`（机制 D 变更实体）。

- [x] 范围内行为完成（66 实体列表页 FK 显示名称；4 无 FK + 1 ext:relation 缺口 + 1 无 FK 仅记录；configId@ErpB2bMftLog 目标无显示列归 Deferred 保留原始 ID——见 Deferred But Adjudicated）
- [x] 相关文档对齐
- [x] 已运行验证（全绿）
- [x] 无范围内项目降级
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证
- [x] 结束审计由独立子代理执行
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 8 个 ext:relation 缺口 FK

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: defaultFundAccountId@ErpMdSettlementMethod（概念上指向银行账户但 ORM 无 to-one）；APS 域 7 个 FK（workOrderId/machineId×3/operationId/workcenterId×2）为跨域→manufacturing 弱指针，ORM 无 to-one。batchLoadProps 不可用。
- Successor Required: `yes`（触发条件：对应 ext:relation 落地或业务需求要求解析时）

### configId@ErpB2bMftLog（目标无显示列，2026-07-14 审计 P0 裁决）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: `configId@ErpB2bMftLog` 有 to-one `config→ErpB2bMftConfig`（ext:relation 存在，与上一类不同），但目标实体 `ErpB2bMftConfig` 经 ORM 全量核实**无 `name`/`code`/任何标识性字符串列**（列集合 = {id, orgId, partnerId, protocol, transportEndpoint, localAs2Id, remoteAs2Id, sftpUsername, sftpPort, ftpsPort, ...}，仅 protocol/transportEndpoint/AS2 ID 等技术字段），`getConfig().getName()` 编译失败，机制 D 无法产生有意义显示值。前序 HR 批次（sourceSalaryId→ErpHrSalary 无 code/name）已建立"保留原始 ID"先例。
- 处置：configId 保留原始 ID。ErpB2bMftLog 仍属机制 D 实体（orgId→orgName 正常解析），故机制 D 实体数维持 66（B2B 域 13 不变），configId 为 66 机制 D 实体内的延后 FK 列。
- Successor Required: `yes`（触发条件：ErpB2bMftConfig 增设标识性显示列，如 `name`/`code`/`configName`）

### 5 个无 FK 列实体

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: ErpMdPartner/ErpMdUoM/ErpMdCurrency/ErpMdTaxRate/ErpCtTemplate 均无 FK 列。
- Successor Required: `no`

## Closure

Status Note: closed

Closure Audit Evidence:

- Auditor / Agent: 独立子代理（新会话 `ses_0a3e1d845ffeaepQKgLMwO7P9c`，未参与实现），2026-07-13 针对实时仓库验证（不依赖计划勾选）。
- Verdict: **PASS**。8 项审计任务全部通过：
  1. 实体计数与覆盖（66 实体）— 6 域每域三层（xmeta 派生 prop + @BizLoader 方法 + bounded-merge grid）均 19/7/14/13/7/6 = 66，零遗漏。
  2. ORM 不变性 — 6 域 `model/*.orm.xml` 修改数=0，零 ORM/契约变更目标达成。
  3. 生成物不变性 — `_gen/`/`_templates/` 下文件为 `mvn install` 自动重生成（bounded-merge 要求 `*Name` 列存在于 _gen 原型以通过有界交集），非手改；保留层 xmeta/view.xml 为正确的定制面。
  4. @BizLoader 守卫抽检（8 实体）— `orm_attached()` 守卫 + `batchLoadProps` 批量加载全域一致，关系名正确（`"uoM"`/`"defaultWarehouse"`/`"organization"` 等）。
  5. FK ID 移除抽检（5 实体）— ErpMdMaterial/ErpLogShipment/ErpCtContract/ErpB2bAsn/ErpDrpLine 数值 FK 均替换为 `*Name`。
  6. ext:relation 缺口保留 — APS 8 个无 to-one 的弱指针 FK（workOrderId/machineId×3/operationId/workcenterId×2）正确保留为 `*Id`。
  7. 测试文件 — 6 域 `Test*FkNameLoader` 均 `extends JunitAutoTestCase`，经 `IGraphQLEngine` findList + `FieldSelectionBean` 请求 `*Name` 并 `assertEquals` 具体值。
  8. 构建/测试 — `mvn clean install -DskipTests`（154 模块）BUILD SUCCESS；6 域 `mvn test` 7 方法全绿（0 failures/0 errors）；66 view.xml `xmllint --noout` well-formed。
- Minor（非阻塞）：表单 layout 仍保留 `*Id`（机制 D 仅作用于 `<grid id="list">`，符合范围）；日志待补（已补，见 docs/logs/2026/2026-07-13.md）。

Follow-up:

- 全部 18+1 域的 FK 名称解析覆盖完成（本计划为最后一组——全域收官）
- 2026-07-14 多维+开放式审计补核"第二跳"（目标显示列存在性）：本计划 closure 时实现层 ErpB2bMftLog.configId 已实际保留为原始 ID（目标 ErpB2bMftConfig 无显示列），审计将其正式裁决归 Deferred（P0）；contractLineId/contractVersionId/drpLineId 裁决以 `getLineNo()`/`getVersionNo()` fallback（P1）；providerRequestId VARCHAR 自引用按规则排除（P2）；Goals 显示 getter 按实际 ORM 列订正（P3）。机制 D 实体数维持 66（ErpB2bMftLog 因 orgId 仍计入），全域算术不变。详见修订计划 `2026-07-14-0930-1-1518-3-audit-revisions`。
