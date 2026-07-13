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

全部 6 域实体的自定义 view.xml 为空 `<grid id="list"/>`。72 实体清单中 66 需机制 D 变更，4 无 FK 列仅记录，1 有 FK 但无 ext:relation 归 Deferred，1 无 FK 列仅记录。

> **重要**：列名须严格对齐 ORM 实际 prop 名（如 `uoMId` 非 `uomId`、`defaultTaxRateId` 非 `taxRateId`、`freightCurrencyId` 非 `currencyId`）。`ownerId`/`approvedById` 等 userId VARCHAR 列非数值 FK。

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
| 13 | **ErpB2bMftLog** | orgId, configId |

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
- 高价值 FK 定义：维度型外键（org→orgName 读 `ErpMdOrganization.name`；partner/supplier/preferredSupplier→partnerName 读 `ErpMdPartner.name`；material→materialName 读 `ErpMdMaterial.name`；currency/functionalCurrency/freightCurrency→currencyName 读 `ErpMdCurrency.name`；manager/shipper→employeeName 读 `ErpMdEmployee.name`；warehouse/defaultWarehouse/sourceWarehouse/preferredSourceWarehouse→warehouseName 读 `ErpMdWarehouse.name`；location/stagingLocation→locationName 读 `ErpMdLocation.name`；category/materialCategory→categoryName 读 `ErpMdMaterialCategory.name`；uoM/fromUoM/toUoM→uomName 读 `ErpMdUoM.name`；taxRate/defaultTaxRate→taxRateName 读 `ErpMdTaxRate.name`；parent→parentName 自引用；carrier→carrierName 读 `ErpLogCarrier.name`；carrierConfig→carrierConfigName 读 `ErpLogCarrierConfig.name`；template→templateName 读 `ErpCtTemplate.name`；format→formatName 读 `ErpB2bEdiFormat.name`；contract→contractName 读 `ErpCtContract.name`；contractLine→contractLineName 读 `ErpCtContractLine.name`；contractVersion→contractVersionName 读 `ErpCtContractVersion.name`；approvalMatrix→approvalMatrixName 读 `ErpCtApprovalMatrix.name`；rebateAgreement→rebateAgreementName 读 `ErpCtRebateAgreement.name`；plan→planName 读 `ErpDrpPlan.name`；drpLine→drpLineName 读 `ErpDrpLine.name`；crossDock/dock→crossDockName 读 `ErpInvDrpCrossDock.name`；ediDoc/sourceEdiDoc→ediDocName 读 `ErpB2bEdiDoc.name`；asn→asnName 读 `ErpB2bAsn.name`；partnerProfile→partnerProfileName 读 `ErpB2bPartnerProfile.name`；config→configName 读 `ErpB2bMftConfig.name`；cert→certName 读 `ErpB2bMftCertificate.name`；shipment→shipmentName 读 `ErpLogShipment.name`；subject→subjectName 读 `ErpMdSubject.name`；acctSchema/targetAcctSchema→acctSchemaName 读 `ErpMdAcctSchema.name`；operationOrder→operationOrderName 读 `ErpApsOperationOrder.name`；stockMove/inboundMove/outboundMove→stockMoveName 读 `ErpInvStockMove.name`）。
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

## Closure Gates

> `mvn clean install -DskipTests`（154 模块）+ `mvn test -pl module-master-data/erp-md-service,module-logistics/erp-log-service,module-contract/erp-ct-service,module-b2b/erp-b2b-service,module-drp/erp-drp-service,module-aps/erp-aps-service -am` + 66 view.xml `xmllint --noout`（机制 D 变更实体）。

- [x] 范围内行为完成（66 实体列表页 FK 显示名称；4 无 FK + 1 ext:relation 缺口 + 1 无 FK 仅记录）
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
