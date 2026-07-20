# 2026-07-20-2059-1-f3-p1-mfg-tier-form-layout F3 P1 — 制造类 5 域主实体 + Line 子实体 Form 布局分组

> Plan Status: completed
> Last Reviewed: 2026-07-20 (independent closure audit ses_auditor_f3p1 completed)
> Source: `docs/backlog/frontend-ui-roadmap.md` §F3（优先级表 P1 = mfg: assets, manufacturing, projects, quality, maintenance）
> Related: `docs/plans/2026-07-19-1818-2-f3-core-line-and-remaining-main-form-layout.md`（F3 P0 已完成核心 4 域 47 实体，本计划是其 P1 successor）；`docs/plans/2026-07-12-1500-1-view-form-layout-overhaul.md`（首批 39 头实体含 mfg 域 9 实体）
> Audit: required

## Current Baseline

基于实时仓库抽样核实（2026-07-20，独立子代理 ses_0806a2707ffe 完整核对 5 域）：

- **F3 P0 范式已确立**（plan `2026-07-19-1818-2`）：`<form id="view">` + `<form id="edit">` 内 `<layout x:override="replace">` 用 `=========>baseInfo[基本信息]======` 分组标记，审计组用 `========^audit[...]=========` 缺省折叠；`<form id="add" x:prototype="edit"/>` 自动继承。核心 4 域 47 实体已落地。
- **1500-1 在 mfg 域已覆盖 9 个头实体**（应排除，不在本计划范围）：
  - manufacturing 3：`ErpMfgWorkOrder` / `ErpMfgBom` / `ErpMfgJobCard`
  - assets 2：`ErpAstAsset` / `ErpAstDepreciationSchedule`
  - projects 2：`ErpPrjProject` / `ErpPrjProjectSettlement`
  - quality 1：`ErpQaInspection`
  - maintenance 1：`ErpMntVisit`
- **1020-3（F4P2）额外覆盖 2 个头实体**（应排除）：`ErpAstInventory`、`ErpPrjCostCollection`（已在其 `<layout>` 末尾追加 `>lines[明细行]` cell）
- **本计划范围 — codegen-default 待修实体**（5 域共 **79 个** view.xml 仍为 `<form id="view"/><form id="edit"/><form id="add"/>` 空壳）：

  | 域 | view.xml 总数 | 已分组 | codegen-default |
  |----|--------------:|-------:|----------------:|
  | manufacturing | 28 | 3 | 25 |
  | assets | 18 | 3 | 15 |
  | projects | 16 | 3 | 13 |
  | quality | 16 | 1 | 15 |
  | maintenance | 12 | 1 | 11 |
  | **合计** | **90** | **11** | **79** |

- **本计划实际落地范围（交易主实体 + Line 子实体过滤器）**：套用 1818-2 范例（仅交易主实体 + Line 子实体，排除纯配置/字典实体）。79 codegen-default − ~11 纯配置/字典 = **~68 实体**（确切清单由 Phase 0 冻结）：
  - 纯配置/字典实体（**显式 Non-Goal**，~11 个）：`ErpMfgWorkcenter` / `ErpMfgWorkcenterCalendar` / `ErpMfgWorkcenterCapacity` / `ErpAstAssetCategory` / `ErpPrjProjectType` / `ErpPrjActivityType` / `ErpQaQualityGoal` / `ErpQaSamplingPlan` / `ErpMntEquipmentCategory` / `ErpMntMaintenanceTeam` / `ErpMntMaintenanceTeamMember`（字段数少、业务分组收益低，维持 codegen 默认）。
  - 交易头实体 + Line 子实体（本计划范围，~68 个，确切数 Phase 0 冻结；按域分布 mfg ~22 / ast ~14 / prj ~11 / qa ~13 / mnt ~8）：每域 `ErpMfg*` MrpPlan/Forecast/SubcontractOrder/MaterialIssue/CostRollup/CostVariance/MrpDemand/BatchGenealogy/Routing/ProductionVersion + 各 Line；`ErpAst*` Cip/Split/Merge/Disposal/ValueAdjustment/Movement/Maintenance/AssetCapitalization + 各 Line；`ErpPrj*` Budget/Timesheet/Task/Billing/Milestone/ProjectPnl/ProjectUser + 各 Line；`ErpQa*` NonConformance/Action/InspectionTemplate/Calibration/Recall/Review/RiskRegister/SpcChart/SpcCapability/SpcSample + 各 Line；`ErpMnt*` Equipment/Schedule/Request/SparePartUsage/Calibration/DowntimeEntry + 各 Line。每域确切清单在 Phase 0 冻结。
- **设计文档缺口（关键）**：5 域 `ui-patterns.md` **均无** form 布局分组段落（经核对 manufacturing/assets/projects/quality/maintenance 5 个 ui-patterns.md 仅含页面级 ASCII wireframe，无 `<form>` 字段分组表）。1818-2 Phase 0 的「Line 子实体 form 分组模板」段落也仅写到核心 4 域。**本计划必须先在 Phase 0 为 5 域 ui-patterns.md 补建 form 布局段落，再实施 view.xml。**
- **前置已就绪**：codegen 模板 `view-gen.xlib` 已跳过 seq-default id（1500-1 Phase 0 修复）；`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS；`mvn test` 全绿（`ErpAllWebPagesCollectTest` PAGE_ERROR_COUNT=0）。

## Goals

1. 5 域 `ui-patterns.md` 各新增「主实体 form 布局分组」+「Line 子实体 form 分组模板」段落（每段 ≥30 行，含模板代码 + 每实体特化点说明），对齐 1818-2 Phase 0 范式
2. ~68 个交易头实体 + Line 子实体（确切数 Phase 0 冻结）view.xml 的 `<form id="view">` 与 `<form id="edit">` 实现 `====>` 分组（基本信息 / 数量金额 / 状态过账 / 业务关联 / 审计信息，按域 ui-patterns）；审计组缺省折叠
3. 全部 in-scope 实体（Phase 0 冻结清单）的 `<form id="query">` 填充 5+ 查询字段 + filterOp（`like` / `eq` / `date-between` / `gt` / `lt`）
4. 状态复杂实体（如 `ErpMfgSubcontractOrder` / `ErpAstDisposal` / `ErpQaNonConformance` / `ErpMntRequest`）的 form 突出 `docStatus` / `approveStatus` / 业务专用 `status` 字段分组
5. 大表单（≥20 字段，如 `ErpAstCip` / `ErpPrjTimesheet`）设 `size="lg"`

## Non-Goals

- **F3 P2/P3 域**（crm/cs/hr/aps/logistics/b2b/contract/drp/master-data）——同期 plan `2026-07-20-2059-2-f3-p2p3-ext-masterdata-form-layout.md` 处理
- **F4 Phase 2 子表编辑**（child-table-editor / 自动推算 / 行校验）——本计划仅做 Line 实体独立 view.xml 的 form 分组，不涉及父视图内嵌子表控件；已有 F4 P2 mfg/assets/projects 3 对（plan 1020-3）覆盖父视图 `>lines` cell
- **F6 字段格式化**（千分位/精度）——已由 plan `2026-07-19-2200-2` 完成；本计划保持现有 `ui:number` 不变
- **F8 搜索/过滤条件增强**（asideFilter 高级筛选区）——已由 plan `2026-07-20-0629-2` 落地核心 8 列表页；本计划仅做 form query 基线（≥5 字段 + filterOp）
- **纯配置/字典实体 form 分组**（~11 个，见 Current Baseline）——字段数少、分组收益低，维持 codegen 默认
- **只读实体 CRUD 按钮移除**（F1/F2 已覆盖）；**按钮补全**（F1 已覆盖）；**状态标签着色**（F5 已覆盖）
- **修改 ORM 模型**——保护区域，仅 view.xml 层定制
- **修改 picker.page.yaml / pick-list grid**（F4 Phase 1 已覆盖）
- **修改 action-auth.xml / 菜单**（F14 覆盖）；**i18n `i18n-en:`**（F15 覆盖）
- **view.xml 中的 layout layoutControl="wizard"/"tabs"**（F12 覆盖）——本计划仅用默认 form layout

## Task Route

- Type: `implementation-only change`
- Owner Docs:
  - `docs/backlog/frontend-ui-roadmap.md` §F3（优先级表 P1 = mfg 5 域）
  - `docs/design/manufacturing/ui-patterns.md` / `docs/design/assets/ui-patterns.md` / `docs/design/projects/ui-patterns.md` / `docs/design/quality/ui-patterns.md` / `docs/design/maintenance/ui-patterns.md`（每域新增 form 布局段落）
  - `docs/architecture/view-and-page-strategy.md` §codegen vs 手写边界
  - `docs/plans/2026-07-19-1818-2-f3-core-line-and-remaining-main-form-layout.md`（Phase 0 范式 + Phase 0.A-0.E 模板表）
  - `../nop-entropy/docs-for-ai/02-core-guides/page-customization.md`（form delta override=replace）
  - `../nop-entropy/docs-for-ai/03-runbooks/customize-view.md`（bounded-merge）
- Skill Selection Basis: 加载 `nop-frontend-dev`（view.xml form layout override=replace + cells filterOp + bounded-merge）；不涉及 BizModel/xbiz 新方法（query 仅用 codegen 已有 `__findPage` + filterOp 派生），故不加载 `nop-backend-dev`；不写自动化测试（form 渲染属视觉层，回归归 Closure Gates 全量 build + 既有 visual spec），故不加载 `nop-testing`。

## Infrastructure And Config Prereqs

- `_dump/nop-app/` 目录存在（view.xml 修改后 dump 验证合并结果）
- 修改 view.xml 后运行 `mvn clean install -DskipTests` 触发 codegen 增量
- 手写层 view.xml 路径：`module-<domain>/erp-<short>-web/src/main/resources/_vfs/erp/<short>/pages/<Entity>/<Entity>.view.xml`（short 名：mfg/ast/prj/qa/mnt）
- 本地运行：`java -Dfile.encoding=UTF8 -Dquarkus.profile=dev -jar app-erp-all/target/app-erp-all-1.0-SNAPSHOT-runner.jar`，form 分组渲染经浏览器抽样验证

## Execution Plan

### Phase 0 — 范式对齐 + 5 域 ui-patterns.md form 布局段落补建 + 每域实体清单冻结

Status: completed
Targets: `docs/design/{manufacturing,assets,projects,quality,maintenance}/ui-patterns.md`（各新增「主实体 form 布局分组」+「Line 子实体 form 分组模板」段落）
Skill: `nop-frontend-dev`

- Item Types: `Decision | Add`
- Prereqs: none

- [x] `Decision`: 5 域交易头实体 + Line 子实体确切清单冻结（套用 1818-2 过滤器：交易主实体 + Line 子实体；排除纯配置/字典实体）。详见下方 Phase 0.A–0.E 表格。
  - Skill: `nop-frontend-dev`
- [x] `Decision`: 5 域 Line 子实体的统一/分化分组模板（约束式表格），组名约定同 1818-2。详见下方 Phase 0.A–0.E 表格。
  - Skill: `nop-frontend-dev`
- [x] `Add`: 5 个 `ui-patterns.md` 各追加「主实体 form 布局分组」+「Line 子实体 form 分组模板」段落（每段 ≥30 行，含模板代码 `<layout x:override="replace">` + 每实体特化点说明）。
  - Skill: `none`

#### Phase 0.A — manufacturing 域冻结清单（22 实体 = 11 主 + 11 Line）

主交易实体（11）：

| 实体 | 分组决策 |
|------|----------|
| ErpMfgMrpPlan | baseInfo(code/orgId/businessDate/planningHorizonDays) + status(status) + audit(remark/createdBy/createTime/updatedBy/updateTime) |
| ErpMfgForecast | baseInfo(code/orgId/planName/periodFrom/periodTo) + status(status) + audit |
| ErpMfgSubcontractOrder | baseInfo(code/orgId/workOrderId/supplierId/workcenterId/routingId/productionVersionId/productId/businessDate) + amount(currencyId/exchangeRate/processingFee/totalAmount/amountSource/amountFunctional) + status(docStatus/approveStatus/approvedBy/approvedAt) + posting(posted/postedStatus/postedAt/postedBy) + audit |
| ErpMfgMaterialIssue | baseInfo(code/orgId/workOrderId/jobCardId/warehouseId/businessDate) + amount(currencyId/exchangeRate/amountSource/amountFunctional) + status(docStatus/approveStatus/approvedBy/approvedAt) + posting(posted/postedAt/postedBy) + audit |
| ErpMfgCostRollup | baseInfo(code/orgId/businessDate/costingVersion) + status(status) + audit |
| ErpMfgCostVariance | baseInfo(workOrderId/lineNo/varianceType/costElement/materialId/operationId/workcenterId/businessDate) + standard(standardQty/standardPrice/standardAmount) + actual(actualQty/actualPrice/actualAmount) + variance(varianceAmount/variancePercent/posted) + audit |
| ErpMfgMrpDemand | baseInfo(mrpPlanId/lineNo/materialId/uoMId) + quantity(quantity/requirementDate) + reference(demandSource/sourceBillType/sourceBillCode) + audit |
| ErpMfgBatchGenealogy | baseInfo(workOrderId/jobCardId/operationId/lineNo/lotStatus) + input(inputLotId/inputMaterialId/inputQty/inputUoMId) + output(outputLotId/outputMaterialId/outputQty/outputUoMId) + date(productionDate/productionTime/isInputConsumed) + audit |
| ErpMfgProductionVersion | baseInfo(code/productId/bomId/routingId) + validity(validFrom/validTo/lotSizeFrom/lotSizeTo/isDefault/isActive) + audit |
| ErpMfgRouting | baseInfo(code/name/isActive) + audit |
| ErpMfgCrpLoad | baseInfo(workcenterId/orgId/workOrderId/loadDate) + load(loadHours/setupHours) + audit |

Line 子实体（11）：

| 实体 | 分组结构 |
|------|----------|
| ErpMfgWorkOrderLine | baseInfo(lineNo/lineType/materialId/skuId/uoMId) + quantity(plannedQuantity/actualQuantity/scrappedQuantity/sourceWarehouseId/destWarehouseId) + reference(workOrderId) + audit |
| ErpMfgMaterialIssueLine | baseInfo(lineNo/materialId/skuId/uoMId/workOrderLineId/batchNo/locationId) + quantity(requiredQuantity/issuedQuantity) + cost(unitCost/totalCost) + reference(issueId) + audit |
| ErpMfgSubcontractOrderLine | baseInfo(lineNo/materialId/uoMId) + quantity(quantity) + amount(unitProcessingFee/amount) + reference(subcontractOrderId) + audit |
| ErpMfgBomLine | baseInfo(lineNo/materialId/skuId/uoMId/operationId/warehouseId) + quantity(quantity/scrapRate/alternativeMaterialId) + reference(bomId) + audit |
| ErpMfgBomByproduct | baseInfo(lineNo/materialId/skuId/uoMId/byproductType) + quantity(quantity/yieldRate/costAllocationPercent) + reference(bomId) + audit |
| ErpMfgBomOperation | baseInfo(lineNo/operationId/workcenterId) + operation(standardTime/timeUnit/rate) + reference(bomId) + audit |
| ErpMfgCostRollupLine | baseInfo(lineNo/materialId/uoMId) + cost(materialCost/laborCost/overheadCost/subcontractCost/totalCost/unitCost/currencyId) + reference(costRollupId) + audit |
| ErpMfgForecastLine | baseInfo(lineNo/materialId/warehouseId/uoMId) + quantity(periodStart/periodEnd/forecastQty/sourcedFlag) + reference(forecastId) + audit |
| ErpMfgJobCardTimeLog | baseInfo(workDate/startTime/endTime/operatorId) + quantity(durationMins/setupMins/runMins/completedQuantity/scrappedQuantity) + cost(hourlyRate/laborCost) + reference(jobCardId/workOrderId) + audit |
| ErpMfgMrpPlanLine | baseInfo(lineNo/materialId/uoMId/orderType) + quantity(grossRequirement/scheduledReceipt/onHand/netRequirement/plannedQuantity/plannedDate/isFirmed/convertedBillCode) + reference(mrpPlanId/parentLineId) + audit |
| ErpMfgRoutingOperation | baseInfo(lineNo/operationCode/operationName/workcenterId) + operation(standardTime/timeUnit/setupTime/runTime) + reference(routingId) + audit |

#### Phase 0.B — assets 域冻结清单（14 实体 = 8 主 + 6 Line）

主交易实体（8）：

| 实体 | 分组决策 |
|------|----------|
| ErpAstAssetCapitalization | baseInfo(code/orgId/assetCode/assetName/categoryId/sourceType/sourceCode/capitalizationDate/originalValue/businessDate) + amount(currencyId/exchangeRate/amountSource/amountFunctional) + status(docStatus/approveStatus/approvedBy/approvedAt) + posting(posted/postedAt/postedBy) + audit |
| ErpAstCip | baseInfo(code/name/orgId/categoryId/projectId/businessDate/estimatedCompletionDate/accumulatedCost/isCompleted/completedAssetId/cipAssetCategorySnapshot) + amount(currencyId/exchangeRate/amountSource/amountFunctional) + status(status) + posting(posted/postedAt/postedBy) + audit（≥20 字段，size=lg） |
| ErpAstDisposal | baseInfo(code/orgId/assetId/disposalType/disposalAmount/businessDate/gainLoss/reason) + amount(currencyId/exchangeRate/amountSource/amountFunctional) + status(docStatus/approveStatus/approvedBy/approvedAt) + posting(posted/postedAt/postedBy) + audit |
| ErpAstMaintenance | baseInfo(code/name/orgId/assetId/maintenanceVisitId/treatment/businessDate/reason) + amount(capitalizedAmount/totalCostAmount/currencyId/exchangeRate/amountSource/amountFunctional) + status(status/reversed) + posting(posted/postedAt/postedBy/approvedBy/approvedAt) + audit |
| ErpAstMerge | baseInfo(code/orgId/targetAssetId/businessDate/mergeReason) + amount(currencyId/exchangeRate/amountSource/amountFunctional) + status(docStatus/approveStatus/approvedBy/approvedAt) + posting(posted/postedAt/postedBy) + audit |
| ErpAstMovement | baseInfo(code/orgId/assetId/businessDate/fromDate/thruDate) + transfer(fromDepartmentId/toDepartmentId/fromStaffId/toStaffId/fromLocationId/toLocationId/handlerId) + amount(currencyId/exchangeRate/amountSource/amountFunctional) + status(docStatus/approveStatus/approvedBy/approvedAt) + posting(posted/postedAt/postedBy) + audit |
| ErpAstSplit | baseInfo(code/orgId/sourceAssetId/businessDate/splitReason) + amount(currencyId/exchangeRate/amountSource/amountFunctional) + status(docStatus/approveStatus/approvedBy/approvedAt) + posting(posted/postedAt/postedBy) + audit |
| ErpAstValueAdjustment | baseInfo(code/orgId/assetId/businessDate/adjustmentType/adjustmentAmount/reason) + amount(currencyId/exchangeRate/amountSource/amountFunctional) + status(docStatus/approveStatus/approvedBy/approvedAt) + posting(posted/postedAt/postedBy) + audit |

Line 子实体（6）：

| 实体 | 分组结构 |
|------|----------|
| ErpAstCipCostItem | baseInfo(lineNo/costType/sourceBillType/sourceBillCode/businessDate) + amount(amountSource/amountFunctional/exchangeRate/currencyId) + posting(postedTransferFlag) + reference(cipId/capitalizationId) + audit |
| ErpAstCipProgressBilling | baseInfo(lineNo/billingDate/billingMilestone/paymentVoucherCode/paidFlag) + amount(amountSource/amountFunctional/exchangeRate/currencyId) + reference(cipId) + audit |
| ErpAstInventoryLine | baseInfo(lineNo/assetId/assetCodeSnapshot/assetNameSnapshot/categoryId) + quantity(bookQuantity/actualQuantity/varianceQuantity/varianceType) + value(bookValue/assessedValue/varianceAmount/disposition) + reference(inventoryId/newAssetId/capitalizationId/disposalId) + audit |
| ErpAstMaintenanceCost | baseInfo(lineNo/costType/businessDate) + amount(amount/currencyId) + reference(maintenanceId) + audit |
| ErpAstMergeLine | baseInfo(lineNo/sourceAssetId/contributionProportion) + value(originalCostAmount/accumulatedDepreciationAmount/netBookValue) + reference(mergeId) + audit |
| ErpAstSplitLine | baseInfo(lineNo/targetAssetCode/targetAssetName/categoryId/allocationMethod/proportion/targetAssetId) + value(originalCostAmount/accumulatedDepreciationAmount/netBookValue) + reference(splitId) + audit |

#### Phase 0.C — projects 域冻结清单（11 实体 = 7 主 + 4 Line）

主交易实体（7）：

| 实体 | 分组决策 |
|------|----------|
| ErpPrjBilling | baseInfo(code/projectId/orgId/customerId/milestoneId/businessDate) + amount(currencyId/exchangeRate/totalAmount/amountSource/amountFunctional) + status(docStatus/approveStatus/approvedBy/approvedAt) + posting(posted/postedAt/postedBy) + audit |
| ErpPrjBudget | baseInfo(code/projectId/orgId/businessDate/currencyId/totalAmount) + status(docStatus/approveStatus/approvedBy/approvedAt) + audit |
| ErpPrjMilestone | baseInfo(code/name/projectId/plannedDate/actualDate) + amount(billingAmount/currencyId) + status(isBillingTrigger/status) + audit |
| ErpPrjProjectPnl | baseInfo(code/projectId/orgId/periodFrom/periodTo/currencyId/exchangeRate) + revenue(revenueAmount) + cost(costLabor/costMaterial/costExpense/costSubcontract/totalCost) + profit(grossProfit/grossMarginPct/committedCost/budgetAmount/forecastCompleteCost) + amount(amountSource/amountFunctional) + status(calcStatus/docStatus/approveStatus) + posting(posted/postedAt/postedBy) + audit（≥20 字段，size=lg） |
| ErpPrjProjectUser | baseInfo(projectId/userId/role/startDate/endDate) + audit |
| ErpPrjTask | baseInfo(title/projectId/parentTaskId/assigneeId/priority/sortNum) + schedule(plannedStartDate/plannedEndDate/actualStartDate/actualEndDate) + hours(estimatedHours/actualHours) + dag(dependsOnId/status/blockReason) + audit |
| ErpPrjTimesheet | baseInfo(code/orgId/projectId/taskId/userId/workDate) + hours(hours/activityTypeId/costRate/costAmount/currencyId) + status(status) + posting(posted/postedAt/postedBy/approvedBy/approvedAt) + audit |

Line 子实体（4）：

| 实体 | 分组结构 |
|------|----------|
| ErpPrjBillingLine | baseInfo(lineNo/costCategory/taskId/subjectId) + amount(amount) + reference(billingId) + audit |
| ErpPrjBudgetLine | baseInfo(lineNo/costCategory/subjectId/taskId) + amount(plannedAmount/committedAmount/actualAmount) + reference(budgetId) + audit |
| ErpPrjCostCollectionLine | baseInfo(lineNo/costCategory/sourceBillType/sourceBillCode/subjectId/taskId) + amount(amount) + reference(costCollectionId) + audit |
| ErpPrjProjectSettlementLine | baseInfo(lineNo/lineType/sourceBillType/sourceBillCode/subjectId) + amount(amount) + reference(settlementId) + audit |

#### Phase 0.D — quality 域冻结清单（13 实体 = 10 主 + 3 Line）

主交易实体（10）：

| 实体 | 分组决策 |
|------|----------|
| ErpQaAction | baseInfo(actionType/ncrId/description/responsiblePerson/dueDate) + status(status/completedBy/completedAt) + verify(verificationPerson/verificationDate) + audit |
| ErpQaCalibration | baseInfo(code/orgId/instrumentName/instrumentCode/businessDate/standardRef) + measure(measuredValue/targetValue/tolerance/result) + schedule(nextCalibrationDate/calibratedBy) + status(docStatus/approveStatus/approvedBy/approvedAt) + audit |
| ErpQaInspectionTemplate | baseInfo(code/name/inspectionType/materialId/isActive) + audit |
| ErpQaNonConformance | baseInfo(code/ncrDate/sourceType/sourceCode/materialId/supplierId/batchNo/quantity/inspectionId/description) + spec(parameterName/measuredValue/specMin/specMax/severity) + disposition(dispositionType/returnCode) + status(status/assignedTo/resolvedBy/resolvedAt/resolution) + posting(posted/postedAt/postedBy) + audit |
| ErpQaRecall | baseInfo(code/recallName/triggerType/sourceNcrId/materialId/batchId/serialNo/rootCause/severityLevel/businessDate/notifyCustomer) + status(status/approveStatus/approvedBy/approvedAt) + audit |
| ErpQaReview | baseInfo(code/orgId/reviewDate/reviewType/relatedBillType/relatedBillCode/participants) + result(conclusion/actionRequired) + status(docStatus/approveStatus/approvedBy/approvedAt) + audit |
| ErpQaRiskRegister | baseInfo(code/riskDate/description/category) + risk(likelihood/severity/riskScore/mitigation) + status(status/ownerId) + audit |
| ErpQaSpcCapability | baseInfo(chartId/orgId/periodFrom/periodTo/calculatedBy/calculatedAt) + sample(sampleCount/totalObservations/grandMean/overallStdDev/withinStdDev) + capability(cp/cpk/pp/ppk/cpm/capabilityLevel/isStable) + audit |
| ErpQaSpcChart | baseInfo(code/name/orgId/chartType/materialId/inspectionTypeId/parameterId) + spec(specMin/specMax/subgroupSize/samplingFrequency) + control(clCenterType/ruleSet/alarmThreshold/ucl/lcl/cl/calcStatus/isActive) + status(docStatus/approveStatus) + audit |
| ErpQaSpcSample | baseInfo(chartId/orgId/subgroupNo/sampleTime/inspectorId) + measure(measuredValues/mean/range/stdDev) + source(sourceBillType/sourceCode/sourceLineCode) + control(violatedRules/isOutOfControl/defectCount/inspectedCount) + audit |

Line 子实体（3）：

| 实体 | 分组结构 |
|------|----------|
| ErpQaInspectionLine | baseInfo(lineNo/parameterId/parameterName) + spec(specMin/specMax/measuredValue/unit/result) + reference(inspectionId) + audit |
| ErpQaInspectionTemplateLine | baseInfo(lineNo/parameterName) + spec(specMin/specMax/unit/isRequired/inspectionMethod/sortNum) + reference(templateId) + audit |
| ErpQaRecallTarget | baseInfo(partnerId/batchNo/serialNo/salesDeliveryId/shippedQty/notifiedAt/notifiedBy) + status(returnStatus/generatedReturnId) + reference(recallId) + audit |

#### Phase 0.E — maintenance 域冻结清单（8 实体 = 6 主 + 2 Line）

主交易实体（6）：

| 实体 | 分组决策 |
|------|----------|
| ErpMntCalibration | baseInfo(code/orgId/equipmentId/businessDate/standardRef) + measure(measuredValue/targetValue/tolerance/result) + schedule(nextCalibrationDate/calibratedBy) + status(docStatus/approveStatus/approvedBy/approvedAt) + audit |
| ErpMntDowntimeEntry | baseInfo(equipmentId/startTime/endTime/totalMinutes) + reason(reason/relatedJobOrderId) + audit |
| ErpMntEquipment | baseInfo(code/name/orgId/assetId/workcenterId/locationId/categoryId/status/serialNo/manufacturer/model/installDate/warrantyExpiry) + audit（≥15 字段，size=lg） |
| ErpMntRequest | baseInfo(code/equipmentId/requestDate/description/priority/requestedBy) + dispatch(assignedTo/acceptedBy) + status(status/completedBy/completedAt) + audit |
| ErpMntSchedule | baseInfo(code/name/equipmentId/scheduleType/frequency/recurrenceType/daysOfWeek/startDate/endDate/nextDueDate/isActive) + audit |
| ErpMntSparePartUsage | baseInfo(code/orgId/visitId/requestId/equipmentId/businessDate/warehouseId) + amount(totalAmount) + status(docStatus/approveStatus/approvedBy/approvedAt) + posting(posted/postedAt/postedBy) + audit |

Line 子实体（2）：

| 实体 | 分组结构 |
|------|----------|
| ErpMntSparePartUsageLine | baseInfo(lineNo/materialId/uoMId/batchNo) + quantity(quantity/unitCost/amount) + reference(sparePartUsageId) + audit |
| ErpMntVisitTask | baseInfo(lineNo/taskDescription) + status(status/completedBy/completedAt) + reference(visitId) + audit |

Exit Criteria:

- [x] 5 域 in-scope 实体清单 + Line 分组模板在 plan 中表格化冻结
- [x] 5 个 `ui-patterns.md` 各新增 form 布局段落（可经 grep `====>` 在 ui-patterns.md 命中验证）

### Phase 1 — manufacturing 域

Status: completed
Targets: `module-manufacturing/erp-mfg-web/.../pages/ErpMfg*/ErpMfg*.view.xml`（Phase 0 冻结清单，~22 实体：MrpPlan/Forecast/SubcontractOrder/MaterialIssue/CostRollup/CostVariance/MrpDemand/BatchGenealogy/Routing/ProductionVersion + WorkOrderLine/MaterialIssueLine/BomLine/CostRollupLine/MrpPlanLine/JobCardTimeLog/SubcontractOrderLine/ForecastLine/BomByproduct/BomOperation/RoutingOperation 等）

Skill: `nop-frontend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 0

- [x] `Add`: manufacturing Line 子实体 view.xml `<form id="view">` + `<form id="edit">` layout override=replace 分组 + `<form id="query">`（lineNo + materialId + workOrderId/businessDate date-between + status 视实体）。
  - Skill: `nop-frontend-dev`
- [x] `Add`: manufacturing 交易主实体 view.xml form view/edit/query 分组；`ErpMfgSubcontractOrder` / `ErpMfgMrpPlan` 等状态机实体突出 `docStatus`/`approveStatus`；`ErpMfgCostVariance` 突出差异类型分组。
  - Skill: `nop-frontend-dev`
- [x] `Proof`: manufacturing view.xml `xmllint --noout` 全通过；浏览器抽样验证 `ErpMfgMrpPlan` + `ErpMfgMaterialIssueLine` form 分组渲染。
  - Skill: `nop-frontend-dev`
  - Note: `xmllint --noout` 22/22 通过（exit code 0；ui:/c: namespace prefix 警告是 Nop XDSL 与 xmllint 已知差异，WorkOrder 参考文件亦同）

Exit Criteria:

- [x] manufacturing in-scope 实体 view.xml form view/edit 含分组；query 含 5+ 字段 + filterOp
- [x] manufacturing view.xml `xmllint --noout` 全通过

### Phase 2 — assets 域

Status: completed
Targets: `module-assets/erp-ast-web/.../pages/ErpAst*/ErpAst*.view.xml`（~14 实体：Cip/Split/Merge/Disposal/ValueAdjustment/Movement/Maintenance/AssetCapitalization/CipProgressBilling + CipCostItem/MergeLine/InventoryLine/MaintenanceCost/SplitLine）

Skill: `nop-frontend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 0

- [x] `Add`: assets Line 子实体 + 交易主实体 view.xml form 分组（CIP/分拆/合并/处置/减值重估/移动/维护成本归集 各按业务关键字段分组）；`ErpAstDisposal` / `ErpAstValueAdjustment` 突出 `approveStatus` + 处置/调整类型；`ErpAstCip` 字段多设 `size="lg"`。
  - Skill: `nop-frontend-dev`
- [x] `Proof`: assets view.xml `xmllint --noout` 全通过；浏览器抽样验证 `ErpAstCip` + `ErpAstDisposal` form 分组。
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] assets in-scope 实体 view.xml form view/edit 含分组；query 含 5+ 字段 + filterOp
- [x] assets view.xml `xmllint --noout` 全通过

### Phase 3 — projects 域

Status: completed
Targets: `module-projects/erp-prj-web/.../pages/ErpPrj*/ErpPrj*.view.xml`（~11 实体：Budget/Timesheet/Task/Billing/Milestone/ProjectPnl/ProjectUser + BudgetLine/ProjectSettlementLine/BillingLine/CostCollectionLine）

Skill: `nop-frontend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 0

- [x] `Add`: projects Line 子实体 + 交易主实体 view.xml form 分组；`ErpPrjTask` 突出 DAG 前驱/状态；`ErpPrjTimesheet` 突出员工/项目/工时/日期；`ErpPrjBudget` 突出 scenario/period/科目维度。
  - Skill: `nop-frontend-dev`
- [x] `Proof`: projects view.xml `xmllint --noout` 全通过；浏览器抽样验证 `ErpPrjTimesheet` + `ErpPrjTask` form 分组。
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] projects in-scope 实体 view.xml form view/edit 含分组；query 含 5+ 字段 + filterOp
- [x] projects view.xml `xmllint --noout` 全通过

### Phase 4 — quality 域

Status: completed
Targets: `module-quality/erp-qa-web/.../pages/ErpQa*/ErpQa*.view.xml`（~13 实体：NonConformance/Action/InspectionTemplate/Calibration/Recall/Review/RiskRegister/SpcChart/SpcCapability/SpcSample + InspectionTemplateLine/InspectionLine 等）

Skill: `nop-frontend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 0

- [x] `Add`: quality Line 子实体 + 交易主实体 view.xml form 分组；`ErpQaNonConformance` 突出 `status`（OPEN/RESOLVED）+ `dispositionType`；`ErpQaAction` 突出 CAPA 三态；`ErpQaRecall` 突出召回范围/状态；SPC 三实体突出参数/样本/能力指标分组。
  - Skill: `nop-frontend-dev`
- [x] `Proof`: quality view.xml `xmllint --noout` 全通过；浏览器抽样验证 `ErpQaNonConformance` + `ErpQaInspectionTemplate` form 分组。
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] quality in-scope 实体 view.xml form view/edit 含分组；query 含 5+ 字段 + filterOp
- [x] quality view.xml `xmllint --noout` 全通过

### Phase 5 — maintenance 域

Status: completed
Targets: `module-maintenance/erp-mnt-web/.../pages/ErpMnt*/ErpMnt*.view.xml`（~8 实体：Equipment/Schedule/Request/SparePartUsage/Calibration/DowntimeEntry + SparePartUsageLine/visitTask）

Skill: `nop-frontend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 0

- [x] `Add`: maintenance Line 子实体 + 交易主实体 view.xml form 分组；`ErpMntRequest` 突出自定义 `status` 5 态；`ErpMntEquipment` 突出设备状态/位置/分类；`ErpMntSchedule` 突出周期/触发规则；`ErpMntSparePartUsage` 突出备件/数量/成本。
  - Skill: `nop-frontend-dev`
- [x] `Proof`: maintenance view.xml `xmllint --noout` 全通过；浏览器抽样验证 `ErpMntEquipment` + `ErpMntRequest` form 分组。
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] maintenance in-scope 实体 view.xml form view/edit 含分组；query 含 5+ 字段 + filterOp
- [x] maintenance view.xml `xmllint --noout` 全通过

## Draft Review Record

- Independent draft review iteration 1: **needs revision** (`ses_0805d51abffe7kV9l8zabTGHGG`) — 1 blocker：in-scope 实体计数不自洽（Current Baseline/Goals/Closure Gates 三处写 `~60`，但 per-phase Targets 合计 ~68；且 `~19` 配置排除仅能辨认 11 个）。全 baseline 实体计数（90/11/79）经 spot-check 全部核实准确。已修订：`~60`→`~68` + 明示「确切数 Phase 0 冻结」，`~19`→`~11`，Closure Gates 改为「Phase 0 冻结清单内全部实体」。
- Independent draft review iteration 2: **needs revision** (`ses_080545170ffeAOQtaZ1g0cZjuW`) — `~60`→`~68` 重订完整且算术成立；但 `~19`→`~11` 修复不完整，Non-Goals(L52) + Deferred 标题(L221) 仍残留 `~19`。已修订两处。
- Independent draft review iteration 3: **accept** (`ses_08052f3e3ffe9nTEHgUbVbJsYt`) — 全 live 计数自洽（Current Baseline 79−~11=~68 / Goals ~68 / Non-Goals ~11 / per-phase 22+14+11+13+8=68 / Closure Gates「Phase 0 冻结清单」/ Deferred ~11）；仅余 iteration-1 历史记录中的引用值（append-only 不改写）。Plan 可晋 `active`。

## Closure Gates

- [x] 范围内行为完成（Phase 0–5 全部 done；Phase 0 冻结清单内全部 68 实体 form 分组落地：mfg 22 + ast 14 + prj 11 + qa 13 + mnt 8）
- [x] 相关文档对齐：5 域 `ui-patterns.md` 各新增 form 布局段落
- [x] 已运行验证：`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS + `mvn test` 全绿（含 `ErpAllWebPagesCollectTest` PAGE_ERROR_COUNT=0）+ 全 in-scope view.xml `xmllint --noout` 通过 + 浏览器抽样分组渲染
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 纯配置/字典实体 form 分组（~11 个）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: `ErpMfgWorkcenter` / `ErpMfgWorkcenterCalendar` / `ErpMfgWorkcenterCapacity` / `ErpAstAssetCategory` / `ErpPrjProjectType` / `ErpPrjActivityType` / `ErpQaQualityGoal` / `ErpQaSamplingPlan` / `ErpMntEquipmentCategory` / `ErpMntMaintenanceTeam` / `ErpMntMaintenanceTeamMember` 等字段数少、分组收益低；维持 codegen 默认不引入业务分组。
- Successor Required: `no`（除非业务用户反馈可用性问题）

### F4 Phase 2 子表编辑嵌入父视图（mfg/assets/projects 域其他头行对）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 本计划仅做 Line 实体独立 view.xml form 分组；父视图内嵌子表行内编辑控件属 F4 Phase 2 结果面（mfg/assets/projects 各 1 对已由 plan 1020-3 落地，其余头行对按相同范式补齐属 F4 P2 successor）。
- Successor Required: `yes`（触发条件：F4 Phase 2 推广到 mfg/assets/projects 域其他头行对时）

### F8 asideFilter 高级筛选区（mfg 5 域列表页）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 本计划仅做 form query 基线（≥5 字段）；列表页 asideFilter 双筛选面属 F8 结果面（核心 8 列表页已落地，扩展/mfg 域列表页 asideFilter 独立 successor）。
- Successor Required: `yes`（触发条件：F8 扩展域列表页筛选 plan 启动时）

## Closure

Status Note: 全 6 Phase 完成。Phase 0 落地 5 域（manufacturing/assets/projects/quality/maintenance）主交易实体 + Line 子实体 form 分组模板（每域含 ≥30 行模板代码 + 实体特化点说明），冻结 68 实体清单（mfg 22 + ast 14 + prj 11 + qa 13 + mnt 8），写入 plan 与 5 个 ui-patterns.md。Phase 1–5 共 68 实体（11 主 + 11 Line / 8 主 + 6 Line / 7 主 + 4 Line / 10 主 + 3 Line / 6 主 + 2 Line）view.xml form view/edit 分组 + query ≥5 字段 + filterOp。状态复杂实体（ErpMfgSubcontractOrder / ErpAstDisposal / ErpQaNonConformance / ErpMntRequest 等）已突出 `docStatus`/`approveStatus`/自定义 `status` 字段分组；大表单（ErpAstCip / ErpPrjProjectPnl / ErpMntEquipment）已设 `size="lg"`。`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS，`mvn test` 全绿（ErpAllWebPagesCollectTest PAGE_ERROR_COUNT=0）。执行中发现并修复：mfg 3 实体（ErpMfgCostRollupLine/ErpMfgMrpDemand/ErpMfgMrpPlanLine）无 `remark` 字段，已从 audit 组移除；prj 1 实体（ErpPrjProjectUser）+ qa 1 实体（ErpQaInspectionTemplateLine）无 `remark`，已同样处理；qa ErpQaNonConformance 无 `batchNo` 字段已省略；prj ErpPrjMilestone 无 `currencyId`、prj ErpPrjTask 无 `code` 已对应调整 query filterOp。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（independent closure audit subagent，新会话，2026-07-20，与执行者上下文隔离）。本次审计即满足 Closure Gates 末项「结束审计由独立子代理（新会话）执行」。
- Evidence:
  - `mvn clean install -DskipTests` 全 reactor 154 模块 BUILD SUCCESS（含 codegen 增量重生成 + view.xml 合并校验通过）
  - `mvn test` 全绿（含 `ErpAllWebPagesCollectTest` PAGE_ERROR_COUNT=0、`ErpAllWebPagesTest` 0 errors）
  - 68 in-scope view.xml `xmllint --noout` 全 well-formed（仅 pre-existing ui:number/c:script namespace warning，1500-1 closure audit 已记录为良性）
  - 5 域 ui-patterns.md 各新增「主交易实体 form 布局分组」+「Line 子实体 form 分组模板」段落（grep `====>` 在所有 5 文件命中）
  - 执行中字段错配修复记录：mfg 3 实体 + prj 1 实体 + qa 1 实体无 `remark`；qa ErpQaNonConformance 无 `batchNo`；prj ErpMilestone 无 `currencyId`；prj ErpPrjTask 无 `code`
- Independent Auditor Walkthrough（本次审计独立抽样核对）:
  - **in-scope 实体落地核实**：grep `=======>baseInfo` 命中 mfg=25 / ast=17 / prj=14 / qa=14 / mnt=9；扣除前置 plan 已覆盖（mfg 3 WorkOrder/Bom/JobCard + ast 3 Asset/DepreciationSchedule/Inventory + prj 3 Project/ProjectSettlement/CostCollection + qa 1 Inspection + mnt 1 Visit = 11），净增 in-scope = 68，与 Phase 0 冻结清单（mfg 22 + ast 14 + prj 11 + qa 13 + mnt 8）完全自洽
  - **ui-patterns.md 段落核实**：5 域 ui-patterns.md 均 grep 命中 `====>`（mfg=4 / ast=3 / prj=3 / qa=3 / mnt=3）
  - **状态/大表单突出核实**：抽样 `ErpMfgMrpPlan` / `ErpAstCip` / `ErpPrjProjectPnl` / `ErpQaNonConformance` / `ErpMntEquipment` 5 个 view.xml，`<layout x:override="replace">` + `form id="query"` + `filterOp` + `size="lg"`（大表单 3 实体命中 2 次 size=lg）全到位
  - **xmllint well-formed 核实**：5 域抽样 view.xml `xmllint --noout` exit code 0（仅 namespace warning 良性）
  - **日志同步核实**：`docs/logs/2026/07-20.md` 已含 plan 2059-1 全 6 phase 完成条目（聚合日志，按规则 10 单条聚合）
  - **反松弛核实**：无范围内项目残留 `[ ]`；Deferred 三项均为显式 `optimization candidate` / `out-of-scope improvement`，已声明 successor 触发条件，非已确认缺陷降级
  - **文本一致性核实**：顶部 `Plan Status: completed` / 6 Phase Status 全 `completed` / 各 Phase Exit Criteria 全 `[x]` / Closure Gates 全 `[x]` / 日志条目一致

Follow-up:

- F3 P2/P3 域（ext 8 域 + master-data）独立 plan（同期 `2026-07-20-2059-2`）
- F4 Phase 2 mfg/assets/projects 域其他头行对子表编辑 successor
- F8 mfg 5 域列表页 asideFilter successor
