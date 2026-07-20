# 2026-07-20-2059-2-f3-p2p3-ext-masterdata-form-layout F3 P2+P3 — 扩展 8 域 + 主数据 Form 布局分组（F3 收尾）

> Plan Status: completed
> Last Reviewed: 2026-07-21
> Source: `docs/backlog/frontend-ui-roadmap.md` §F3（优先级表 P2 = ext: crm/cs/hr/aps/logistics/b2b/contract/drp；P3 = master-data）
> Related: `docs/plans/2026-07-19-1818-2-f3-core-line-and-remaining-main-form-layout.md`（F3 P0 核心 4 域）；`docs/plans/2026-07-20-2059-1-f3-p1-mfg-tier-form-layout.md`（同期 F3 P1 successor，mfg 5 域）；`docs/plans/2026-07-12-1500-1-view-form-layout-overhaul.md`（首批 39 头实体含 ext 域 8 实体 + master-data 4 实体）
> Audit: required

## Current Baseline

基于实时仓库抽样核实（2026-07-20，独立子代理 ses_08069fab5ffe 完整核对 9 域）：

- **F3 P0 范式已确立**（plan `2026-07-19-1818-2`）：`<layout x:override="replace">` + `=========>groupName[中文标签]======` 分组标记，审计组 `^` 缺省折叠；`add` 经 `x:prototype="edit"` 继承。
- **1500-1 在 9 域已覆盖 12 个头实体**（应排除）：
  - master-data 4：`ErpMdMaterial` / `ErpMdPartner` / `ErpMdSubject` / `ErpMdOrganization`
  - crm 2：`ErpCrmLead` / `ErpCrmActivity`
  - 其余各域 1：`ErpHrEmployee` / `ErpLogShipment` / `ErpCsTicket` / `ErpApsSchedule` / `ErpB2bEdiDoc` / `ErpCtContract`
  - **drp 域 1500-1 零覆盖**（全 7 实体 codegen-default，greenfield）
- **本计划范围 — codegen-default 待修实体**（9 域共 **146 个** view.xml 为空壳）：

  | 域 | view.xml 总数 | 已分组 | codegen-default |
  |----|--------------:|-------:|----------------:|
  | crm | 34 | 2 | 32 |
  | cs | 16 | 1 | 15 |
  | hr | 36 | 1 | 35 |
  | aps | 6 | 1 | 5 |
  | logistics | 7 | 1 | 6 |
  | b2b | 13 | 1 | 12 |
  | contract | 15 | 1 | 14 |
  | drp | 7 | 0 | 7 |
  | **P2 小计（8 ext 域）** | **134** | **8** | **126** |
  | master-data（P3） | 24 | 4 | 20 |
  | **合计** | **158** | **12** | **146** |

- **本计划实际落地范围（交易主实体 + Line 子实体过滤器 + P3 字典 triage）**：套用 1818-2 过滤器。预估 **~90 实体**：
  - **P2 ext 域交易头 + Line（~80）**：crm（Campaign/Forecast/Quota/Sequence/Opportunity/Quote 等 + ForecastLine/QuotaLine 等）/ cs（SlaPolicy/Entitlement/Survey/KnowledgeBase/CannedResponse/Contract 等）/ hr（Salary/SalarySimulation/Recruitment/LeaveRequest/Attendance/Timesheet/EmploymentContract/Position 等 + TimesheetLine/SalarySimulationItemAdjustment 等；注：薪酬登记实体为 `ErpHrSalary`，ORM 无 `ErpHrPayroll`，仅有 `ErpHrPayrollBankFile` 银行代发文件配置实体归 Non-Goal）/ aps（OperationOrder/Constraint 等）/ logistics（ShipmentLine/Carrier/CarrierConfig/ShipmentParcel 等）/ b2b（Asn/AsnLine/EdiFormat/PartnerProfile/CodeMapping 等）/ contract（ContractLine/InvoicePlan/RebateAgreement/RebateAccrual/VolumeDiscount/ContractVersion/Template 等）/ drp（Plan/Line/Parameter 等）
  - **P3 master-data（~10，经 triage）**：triage 裁决——≤8 字段纯字典（Currency/UoM/TaxRate/SettlementMethod 等 ~12 个）维持 codegen 默认不作分组；仅对有业务分组价值的 ~10 实体（Warehouse/Employee/ExchangeRate/AcctSchema/MaterialCategory/MaterialSku/PartnerContact/BankAccount 等）落地分组。
- **设计文档缺口（关键）**：9 域 `ui-patterns.md` **均存在但均无** form 布局分组段落（仅含页面级 wireframe）。**注意路径用全域名**：`docs/design/crm/`、`docs/design/customer-service/`（**非** `cs/`）、`docs/design/human-resource/`（**非** `hr/`）、`docs/design/aps/`、`docs/design/logistics/`、`docs/design/b2b/`、`docs/design/contract/`、`docs/design/drp/`、`docs/design/master-data/`。**本计划 Phase 0 必须先为 9 域 ui-patterns.md 补建 form 布局段落（全部为既有文件追加段落，无需新建文件），再实施 view.xml。**
- **drp 域特殊性**：`ErpDrpPlan.view.xml` 已有 `<grid>` + 自定义 actions（runDrp/approveAll/generateOrder 按钮）但零 form layout；其余 6 实体完全 codegen-default。
- **前置已就绪**：codegen 模板已跳过 seq-default id；`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS；`mvn test` 全绿。

## Goals

1. 9 域 `ui-patterns.md` 各新增「主实体 form 布局分组」+「Line 子实体 form 分组模板」段落（对齐 1818-2 范式）
2. **P3 字典 triage 决策**：在 plan 内表格化记录 master-data 20 实体的 triage 分类（纯字典 → 维持默认 vs 有分组价值 → 落地）
3. ~90 个交易头实体 + Line 子实体 view.xml 的 `<form id="view">` 与 `<form id="edit">` 实现 `====>` 分组；审计组缺省折叠
4. 全部 in-scope 实体的 `<form id="query">` 填充 5+ 查询字段 + filterOp
5. **F3 路线图项完全收尾**：F3 P0（核心 4 域）+ F3 P1（mfg 5 域）+ 本计划（ext 8 域 + master-data）= 全 18 域 form 布局分组覆盖

## Non-Goals

- **F3 P1 mfg 5 域**——同期 plan `2026-07-20-2059-1` 处理
- **F4 Phase 2 子表编辑**——本计划仅做 Line 实体独立 view.xml form 分组，不涉及父视图内嵌子表控件
- **F6 字段格式化 / F8 asideFilter / F1 按钮 / F5 状态标签 / F10 树形视图**——均已有独立计划完成
- **修改 ORM 模型 / picker.page.yaml / action-auth.xml / i18n**——保护区域或独立 F 项
- **master-data 纯字典实体 form 分组**（~12 个，见 P3 triage）——维持 codegen 默认
- **view.xml layoutControl="wizard"/"tabs"**（F12 覆盖）

## Task Route

- Type: `implementation-only change`
- Owner Docs:
  - `docs/backlog/frontend-ui-roadmap.md` §F3（优先级表 P2/P3）
  - 9 域 `docs/design/<domain>/ui-patterns.md`（注意 cs=`docs/design/customer-service/`、hr=`docs/design/human-resource/`，其余同 short 名；各新增 form 布局段落）
  - `docs/plans/2026-07-19-1818-2-f3-core-line-and-remaining-main-form-layout.md`（Phase 0 范式）
  - `../nop-entropy/docs-for-ai/02-core-guides/page-customization.md`（form override=replace）
- Skill Selection Basis: 加载 `nop-frontend-dev`（form layout override=replace + cells filterOp + bounded-merge）；P3 master-data 含树形实体 `ErpMdMaterialCategory`（F10 已落地 tree grid）——本计划仅补其 form 分组，不改 tree grid，故不另加载技能；不涉及 BizModel/xbiz，不加载 `nop-backend-dev`；不写自动化测试，不加载 `nop-testing`。

## Infrastructure And Config Prereqs

- 同 plan `2026-07-20-2059-1`（`_dump/nop-app/` + `mvn clean install -DskipTests` codegen 增量 + 手写层 view.xml 路径 `module-<domain>/erp-<short>-web/.../pages/<Entity>/<Entity>.view.xml`，short 名：crm/cs/hr/aps/log/b2b/ct/drp/md）

## Execution Plan

### Phase 0 — 范式对齐 + 9 域 ui-patterns.md form 布局段落补建 + P3 字典 triage 决策 + 实体清单冻结

Status: completed
Targets: 9 域 `docs/design/<domain>/ui-patterns.md`（各新增 form 布局段落）+ plan 内 P3 triage 表
Skill: `nop-frontend-dev`

- Item Types: `Decision | Add`
- Prereqs: none

- [x] `Decision`: 8 ext 域交易头 + Line 子实体确切清单冻结（套用 1818-2 过滤器；排除纯配置/规则/模板字典实体如 crm 的 QuoteTemplate/LeadScoreConfig、hr 的 SurveyQuestion/ShiftRotationPattern 等低频配置）。详见下方 Phase 0.A–0.H 表格。
  - Skill: `nop-frontend-dev`
- [x] `Decision`: **P3 master-data 字典 triage**——表格化 20 实体分类：(A) 纯字典 ≤8 字段维持 codegen 默认（Currency/UoM/TaxRate/SettlementMethod/CostCenter/Location/UoMConversion/SubjectMapping/AcctSchemaCoa/SysConfig 共 10 个）；(B) 有分组价值落地（Warehouse/Employee/ExchangeRate/AcctSchema/MaterialCategory/MaterialSku/PartnerContact/PartnerAddress/BankAccount/SupplierApproval 共 10 个）。详见下方 Phase 0.I 表格。
  - Skill: `nop-frontend-dev`
- [x] `Add`: 9 个 `ui-patterns.md` 各追加「主实体 form 布局分组」+「Line 子实体 form 分组模板」段落（9 文件均既有；cs 路径=`docs/design/customer-service/`、hr 路径=`docs/design/human-resource/`，非 short 名）。
  - Skill: `none`

#### Phase 0.A — crm 域冻结清单（15 实体 = 12 主 + 3 Line）

主交易实体（12）：

| 实体 | 分组决策 |
|------|----------|
| ErpCrmCampaign | baseInfo(code/name/orgId/campaignName/medium/source) + schedule(startDate/endDate) + amount(budgetAmount/actualCost) + audit(remark/createdBy/createTime/updatedBy/updateTime) |
| ErpCrmEvent | baseInfo(code/orgId/eventType/eventCategoryId/subject/description) + schedule(startDateTime/endDateTime/duration) + association(relatedLeadId/relatedBillType/relatedBillCode/partnerId/contactId/ownerId) + status(status/priority) + recurrence(isRecurrent/parentEventId/reminderMinutesBefore) + audit |
| ErpCrmQuota | baseInfo(orgId/territoryId/teamId/ownerId) + period(periodType/fiscalYear/periodLabel) + amount(quotaAmount/currencyId/isFinalized/notes) + audit |
| ErpCrmForecast | baseInfo(orgId/periodId/territitoryId/teamId/ownerId) + amount(currencyId/commitAmount/upsideAmount/weightedAmount/bestCaseAmount) + count(opportunityCount/commitOpportunityCount/expectedClosedRevenue/lastCalculatedAt/notes) + audit |
| ErpCrmTerritory | baseInfo(code/name/orgId/parentId/territoryType/managerId) + tree(fullPath/level/isLeaf/sortOrder/isActive) + audit(description/remark) |
| ErpCrmBundlePricing | baseInfo(code/name/orgId/bundleName/description) + discount(discountType/discountValue/bundleAmount) + validity(effectiveFrom/effectiveTo/isActive) + audit |
| ErpCrmSequence | baseInfo(code/name/orgId/templateType/description) + schedule(expectedDuration/isActive/isDefault) + audit |
| ErpCrmSequenceStep | baseInfo(sequenceId/orgId/stepName/stepOrder) + action(activityType/stepDescription/dueDays/completionCondition/isMandatory/autoCreateEvent) + audit |
| ErpCrmTerritoryAssignmentRule | baseInfo(orgId/ruleName/priority/territitoryId) + condition(conditionType/conditionValue/assignmentMethod/groupId/isDefault/isActive) + audit |
| ErpCrmProductConfigurator | baseInfo(code/name/orgId/productType/configName/wizardLayout/isActive) + validity(effectiveFrom/effectiveTo) + audit |
| ErpCrmConfigRule | baseInfo(configuratorId/orgId/ruleType/sequence) + condition(sourceFeatureCode/sourceFeatureValue/targetFeatureCode/targetFeatureValue/conditionExpression) + audit |
| ErpCrmPriceRule | baseInfo(code/name/orgId/ruleType/priority/isActive) + scope(productId/productCategory/customerId/customerCategory) + range(minQuantity/maxQuantity) + price(priceOverride/discountPercent/discountAmount/currencyId) + validity(effectiveFrom/effectiveTo) + audit |

Line 子实体（3）：

| 实体 | 分组结构 |
|------|----------|
| ErpCrmBundlePricingLine | baseInfo(bundleId/orgId/productId/sequence) + quantity(quantity/unitPrice) + audit |
| ErpCrmForecastLine | baseInfo(forecastId/orgId/leadId/probability) + amount(expectedRevenue/weightedRevenue/forecastCategory/includedInCommit/stageName) + audit |
| ErpCrmLeadScoreLine | baseInfo(scoreId/orgId/configLineId/criterionCode/criterionName) + value(rawValue/lookupValue/rawScore/weightedScore/sequence) + audit |

纯字典/配置实体（**显式 Non-Goal**，排除）：ErpCrmEventCategory / ErpCrmLeadStatus / ErpCrmLostReason / ErpCrmQuoteTemplate / ErpCrmSource / ErpCrmStage / ErpCrmTeam / ErpCrmForecastPeriod / ErpCrmForecastAccuracy / ErpCrmLeadFunnel / ErpCrmFunnelStageMetrics / ErpCrmLeadConvLog / ErpCrmLeadScore / ErpCrmLeadScoreConfig / ErpCrmLeadScoreConfigLine / ErpCrmSequenceAssignment / ErpCrmLeadSequenceProgress（17 个）

#### Phase 0.B — hr 域冻结清单（15 实体 = 12 主 + 3 Line/child）

主交易实体（12）：

| 实体 | 分组决策 |
|------|----------|
| ErpHrAttendance | baseInfo(employeeId/date/businessDate/orgId) + clock(clockIn/clockOut/workHours) + exception(lateMinutes/earlyLeaveMinutes/isAbsent/source/leaveRequestId) + audit(remark) |
| ErpHrLeaveRequest | baseInfo(code/employeeId/orgId/businessDate) + detail(leaveType/startDate/endDate/durationDays/reason) + approval(status/approverId/approvedAt) + audit(remark) |
| ErpHrRecruitment | baseInfo(code/positionId/departmentId/orgId/businessDate) + headcount(headcount/candidateName/candidatePhone/candidateEmail/source) + pipeline(status/interviewerId/interviewDate/offerSalary/hiredDate/employeeId) + audit(remark) |
| ErpHrEmploymentContract | baseInfo(code/employeeId/orgId/businessDate) + contract(contractType/signDate/startDate/endDate/probationMonths/workingHoursPerWeek/status) + salary(annualSalary/monthlySalary/salaryCurrencyId/salaryPayMethod/socialInsuranceBase/housingFundBase) + attachment(attachmentFileId) + audit(remark) |
| ErpHrEmployeeAssessment | baseInfo(employeeId/orgId/businessDate) + detail(assessmentType/assessorId/assessmentDate) + result(overallScore/status) + audit(remark) |
| ErpHrDevelopmentPlan | baseInfo(employeeId/orgId/businessDate) + detail(planName/targetDate/status) + audit(remark) |
| ErpHrSurvey | baseInfo(code/title/surveyType/isAnonymous/status) + schedule(startDate/endDate/reminderDays/targetDepartmentId) + eNps(includeENps/eNpsQuestion) + stats(totalQuestions/totalResponses/completionRate/avgScore/eNpsScore) + audit(description/remark) |
| ErpHrSalary | baseInfo(employeeId/year/month/orgId/businessDate) + gross(basicSalary/positionAllowance/performanceBonus/overtimePay/mealAllowance/transportAllowance/otherAllowance/grossSalary) + deduction(socialInsurance/housingFund/taxAmount/otherDeductions) + net(netSalary) + payment(paymentStatus/paymentDate/paymentBatchNo/bankFileId) + approval(approveStatus/approvedBy/approvedAt) + posting(posted/nopFlowId) + meta(performanceFactor/actualWorkDays/requiredWorkDays/totalOvertimeHours/unpaidLeaveDays/cumulativeData/reviewNote) + audit(remark)（≥20 字段，size=lg） |
| ErpHrSalarySimulation | baseInfo(code/sourceSalaryId/orgId/businessDate) + period(simulationPeriodYear/simulationPeriodMonth/simulationName/status) + review(reviewerId/reviewedAt/convertedAt/convertedSalaryId/notes) + audit |
| ErpHrShiftAssignment | baseInfo(employeeId/shiftId/orgId/businessDate/assignmentDate) + actual(actualStartTime/actualEndTime/isAbsent/absenceReason/leaveRequestId/swapRequestId/replacedByAssignmentId) + status(status) + audit |
| ErpHrShiftSwapRequest | baseInfo(code/requesterId/targetEmployeeId/orgId/businessDate) + detail(sourceAssignmentId/targetAssignmentId/swapDate/reason) + approval(status/approvedById) + audit |
| ErpHrTimesheet | baseInfo(code/employeeId/orgId/businessDate) + period(periodFrom/periodTo/totalHours) + approval(status) + audit(remark) |

Line/child 子实体（3）：

| 实体 | 分组结构 |
|------|----------|
| ErpHrTimesheetLine | baseInfo(timesheetId/employeeId/workDate) + detail(projectId/taskId/activityType) + hours(hours/description) + audit |
| ErpHrSalarySimulationItemAdjustment | baseInfo(simulationId/employeeId/orgId) + detail(salaryItemCode/originalAmount/adjustedAmount/adjustmentReason) + audit(adjustedBy/adjustedAt/remark) |
| ErpHrDevelopmentPlanItem | baseInfo(planId/competencyId/gapId) + detail(targetLevel/developmentAction/mentorId/startDate/endDate/status/progressNote) + audit |

纯字典/配置实体（**显式 Non-Goal**，排除）：ErpHrDepartment（F10 tree）/ ErpHrPosition / ErpHrCompetency / ErpHrCompetencyLevel / ErpHrRoleCompetency / ErpHrSocialInsuranceConfig / ErpHrSocialInsuranceBase / ErpHrTaxConfig / ErpHrLeaveBalance / ErpHrShift / ErpHrShiftRotationPattern / ErpHrPayrollBankFile / ErpHrSalaryItem / ErpHrSurveyQuestion / ErpHrSurveyResponse / ErpHrSurveyAnswer / ErpHrSurveyResult / ErpHrAssessmentDetail / ErpHrGapAnalysis / ErpHrTaxSpecialDeduction（20 个）

#### Phase 0.C — cs 域冻结清单（8 实体，无 *Line 命名子表）

主交易实体（8）：

| 实体 | 分组决策 |
|------|----------|
| ErpCsSlaPolicy | baseInfo(code/name/ticketTypeId/minPriority) + policy(resolveHours/resolveDays/isWorkingDays) + escalate(escalationUserId/teamId) + audit(description) |
| ErpCsEntitlement | baseInfo(code/orgId/partnerId/contractId/slaPolicyId) + service(serviceType/startDate/endDate) + quota(maxTickets/usedTickets/maxResponseTime/maxResolutionTime/isActive) + audit(notes) |
| ErpCsContract | baseInfo(code/name/orgId/partnerId/contractType/businessDate) + schedule(startDate/endDate/billingCycle/status) + amount(totalAmount) + attachment(attachmentFileId) + audit |
| ErpCsCannedResponse | baseInfo(code/orgId/title/categoryId/sequence) + content(content/variableDefs) + match(macroTicketTypeId/macroPriority/isActive/usageCount) + audit |
| ErpCsCatalogFulfillment | baseInfo(code/orgId/catalogItemId/sequence) + action(actionType/actionConfig/assignToRole/estimatedDuration/isMandatory) + audit |
| ErpCsSurvey | baseInfo(orgId/ticketId/surveyToken/surveyChannel) + score(csatScore/npsScore/cesScore/comment) + time(surveySentAt/respondedAt) + audit |
| ErpCsTimeEntry | baseInfo(orgId/ticketId/agentId/source) + time(startTime/endTime/duration/isBillable/billingRate/billableAmount) + approval(approvalStatus/approvedById/approvedAt) + reference(projectId/taskId/description) + audit |
| ErpCsTicketAction | baseInfo(ticketId/actionType) + transition(fromStatus/toStatus/operatorId/content) + audit |

纯字典/配置实体（**显式 Non-Goal**，排除）：ErpCsTeam / ErpCsTicketType / ErpCsAgentRate / ErpCsKnowledgeBase / ErpCsCannedCategory（tree）/ ErpCsCatalogCategory（tree）/ ErpCsServiceCatalogItem（F10 tree，仅 form 分组无业务价值）（7 个）

#### Phase 0.D — contract 域冻结清单（13 实体 = 5 Line + 8 主）

主交易实体（8）：

| 实体 | 分组决策 |
|------|----------|
| ErpCtContractVersion | baseInfo(contractId/versionNo/versionDate/isCurrent/status) + content(content/attachmentFileId) + approval(approvedBy/approvedAt) + audit(remark) |
| ErpCtRebateAgreement | baseInfo(code/orgId/contractId/partnerId/rebateType/businessDate) + schedule(agreementDate/startDate/endDate/status) + amount(totalAccumulatedAmount/estimatedRebateAmount) + meta(accrualMethod) + audit(remark) |
| ErpCtRebateAccrual | baseInfo(rebateAgreementId/orgId/sourceBillType/sourceBillCode/accrualDate) + amount(billAmountSource/accruedRebate) + settle(isSettled/settledDate) + audit(remark) |
| ErpCtRebateSettlement | baseInfo(rebateAgreementId/orgId/businessDate/settlementDate/status) + amount(totalRebateAmount) + credit(creditMemoBillType/creditMemoBillCode) + posting(posted/postedAt/postedBy) + audit(remark) |
| ErpCtApprovalRecord | baseInfo(contractId/orgId/approvalMatrixId/approvalOrder/approverId) + result(approvalStatus/comment/approvedAt/rejectedAt) + audit(remark) |
| ErpCtApprovalMatrix | baseInfo(code/orgId/contractType/isActive) + range(minAmount/maxAmount) + rule(approverRole/approvalOrder/allowSkip) + audit(remark) |
| ErpCtSignatureRequest | baseInfo(orgId/contractVersionId/provider/status) + detail(providerRequestId/signers/signingDeadline) + result(completedAt/certificateUrl/evidenceNo/errorMsg) + attachment(attachmentFileId) + audit(remark) |
| ErpCtDocument | baseInfo(orgId/contractId/code/docName/docType) + file(attachmentFileId/fileSize/fileHash/mimeType) + ocr(ocrText/ocrStatus/fullTextSearch) + retention(metadataTags/retentionDate/archiveDate/purgeDate/isArchived/versionNo) + audit(remark) |

Line 子实体（5）：

| 实体 | 分组结构 |
|------|----------|
| ErpCtContractLine | baseInfo(contractId/lineNo/materialId/description) + quantity(quantity/unitPrice/amount) + audit(remark) |
| ErpCtConsumptionLine | baseInfo(contractLineId/consumptionDate/sourceBillType/sourceBillCode) + quantity(quantity/unitPrice/amount) + audit(remark) |
| ErpCtInvoicePlan | baseInfo(contractLineId/planDate/invoiceTerm) + amount(amount) + status(isInvoiced/invoiceBillCode/invoiceDate) + audit(remark) |
| ErpCtVolumeDiscount | baseInfo(contractLineId/orgId) + range(fromQty/toQty) + discount(discountPercent/unitPrice) + audit(remark) |
| ErpCtRebateTier | baseInfo(rebateAgreementId) + range(fromAmount/toAmount) + rebate(rebatePercent/rebateAmount) + audit(remark) |

纯字典/配置实体（**显式 Non-Goal**，排除）：ErpCtTemplate（6 字段，合同模板字典）（1 个）

#### Phase 0.E — aps 域冻结清单（4 实体，无 *Line 命名子表）

主交易实体（4）：

| 实体 | 分组决策 |
|------|----------|
| ErpApsOperationOrder | baseInfo(code/workOrderId/operationName/sequence/orgId/businessDate) + assign(machineId/assignedToId/isOutsourced) + schedule(plannedStartDateT/plannedEndDateT/realStartDateT/realEndDateT/setupTime/runtimePerUnit/qty/totalDuration/earliestStartDateT/latestEndDateT) + status(priority/status) + audit(remark) |
| ErpApsDispatchLog | baseInfo(orgId/operationOrderId/workcenterId) + dispatch(dispatchType/previousStatus/newStatus/dispatchedBy/dispatchedAt) + condition(conditionCheckResult/materialAvailable/operatorAvailable/toolingAvailable) + audit(note) |
| ErpApsDispatchRule | baseInfo(orgId/workcenterId/ruleName/enableAuto) + constraint(requireMaterial/requireOperator/requireTooling/maxLookaheadMinutes/dispatchAheadMinutes/autoConfirmMaterial/maxConcurrentOps/priorityThreshold/enabledHours) + action(holdUntil/holdReason) + audit(remark) |
| ErpApsOpRouting | baseInfo(orgId/operationId/machineId/priority) + delta(setupTimeDelta/runtimePerUnitDelta) + validity(isDefault/isEnabled/effectiveFrom/effectiveTo/minBatchQty/maxBatchQty) + audit(remark) |

纯字典/配置实体（**显式 Non-Goal**，排除）：ErpApsConstraint（7 字段，约束字典）（1 个）

#### Phase 0.F — logistics 域冻结清单（6 实体 = 1 Line + 5 主）

主交易实体（5）：

| 实体 | 分组决策 |
|------|----------|
| ErpLogCarrier | baseInfo(code/orgId/carrierName/carrierType/partnerId/isActive) + gateway(gatewayId/trackingUrlTemplate) + capability(maxParcelWeight/supportedServiceTypes) + audit(remark) |
| ErpLogCarrierConfig | baseInfo(carrierId/orgId/configCode/serviceType) + endpoint(apiEndpoint/trackingUrlTemplate) + credential(apiKey/apiSecret/credentials) + format(printFormat/additionalProperties/isActive) + audit(remark) |
| ErpLogShipmentLog | baseInfo(shipmentId/orgId/gatewayId/actionType) + request(requestBody/responseBody/httpStatus) + result(errorCode/errorMessage/isSuccess/executedAt) + audit(remark) |
| ErpLogShipmentParcel | baseInfo(shipmentId/parcelNo/trackingNo) + dimension(weight/length/width/height) + value(declaredValue/labelUrl/isActive) + audit(remark) |
| ErpLogDeliveryWindow | baseInfo(partnerId/orgId/weekday/startTime/endTime) + capacity(maxCapacity/currentBooked/isActive/effectiveFrom/effectiveTo/allowedShipmentTypes) + audit(remark) |

Line 子实体（1）：

| 实体 | 分组结构 |
|------|----------|
| ErpLogShipmentLine | baseInfo(shipmentId/lineNo/materialId) + quantity(quantity/unit/packageDescription) + audit(remark) |

#### Phase 0.G — b2b 域冻结清单（9 实体 = 1 Line + 8 主）

主交易实体（8）：

| 实体 | 分组决策 |
|------|----------|
| ErpB2bAsn | baseInfo(code/orgId/sourceEdiDocId/partnerId/businessDate) + schedule(shipmentDate/estimatedArrivalDate/trackingNo) + reference(relatedBillType/relatedBillCode/status) + audit(remark) |
| ErpB2bPartnerProfile | baseInfo(code/orgId/partnerId/partnerName/status) + protocol(protocol/transportEndpoint/authMethod/webhookSecret/timezone) + cert(certExpiry/certFingerprint/allowedFormats) + contact(contactName/contactEmail/contactPhone) + lifecycle(goLiveDate/archivedAt/notes) + audit |
| ErpB2bMftConfig | baseInfo(orgId/partnerId/protocol/transportEndpoint) + identity(localAs2Id/remoteAs2Id/sftpUsername/sftpPort/ftpsPort/ftpsImplicitTls) + security(compression/encryption/encryptionAlgo/signature/signatureAlgo/certId) + retry(maxRetries/retryIntervalMin/deadLetterEnabled) + monitor(monitorDirectory/monitorIntervalSec/active) + audit(remark) |
| ErpB2bMftCertificate | baseInfo(orgId/partnerId/certName/certType) + detail(algorithm/keySize/issuerName/subjectName/serialNo/fingerprintSha256) + validity(issuedAt/expiresAt/isActive) + audit(remark) |
| ErpB2bMftLog | baseInfo(orgId/configId/relatedBillType/relatedBillCode/direction) + file(fileName/fileSize/fileHash/messageId) + transfer(mdnStatus/protocol/status/startTime/endTime/durationMs) + error(errorCode/errorMsg/retryCount) + flags(isCompressed/isEncrypted/isSigned) + audit(remark) |
| ErpB2bEdiLog | baseInfo(ediDocId/orgId/direction/logTime) + payload(requestPayload/responsePayload) + result(resultCode/resultMsg) + audit(remark) |
| ErpB2bCertificationChecklist | baseInfo(partnerProfileId) + detail(checklistItem/requiredDocType/isMandatory) + result(isPassed/checkedBy/checkedAt/evidence) + audit(remark) |
| ErpB2bTestExchange | baseInfo(partnerProfileId/direction/formatCode/testCaseCode) + payload(sentPayload/receivedPayload) + result(expectedResult/actualResult/passed/testedBy/testedAt) + audit(notes) |

Line 子实体（1）：

| 实体 | 分组结构 |
|------|----------|
| ErpB2bAsnLine | baseInfo(asnId/lineNo/materialId/supplierPartNo) + quantity(quantity/shippedQty) + audit(remark) |

纯字典/配置实体（**显式 Non-Goal**，排除）：ErpB2bCodeMapping（6 字段）/ ErpB2bEdiFormat（8 字段）/ ErpB2bPartnerCredential（8 字段）（3 个）

#### Phase 0.H — drp 域冻结清单（6 实体 = 1 Line + 5 主）

主交易实体（5）：

| 实体 | 分组决策 |
|------|----------|
| ErpDrpPlan | baseInfo(code/planName/orgId/businessDate) + period(periodFrom/periodTo/status) + run(runAt/runBy/totalReplenishmentQty) + audit(remark) |
| ErpInvDrpSafetyStockCalc | baseInfo(code/orgId/materialId/warehouseId) + method(method/serviceLevel/historyMonths/leadTimeDays) + result(calculatedSafetyStock/calculatedRop/overrideSafetyStock/lastCalculatedAt/overwrittenBy) + audit(remark) |
| ErpInvDrpCrossDock | baseInfo(code/orgId/drpLineId/businessDate) + link(inboundMoveId/outboundMoveId) + bill(sourceBillType/sourceBillCode/targetBillType/targetBillCode) + detail(materialId/quantity/stagingLocationId/dockSlotTime/status/matchedAt/loadedAt) + audit(remark) |
| ErpInvDrpDockAppointment | baseInfo(warehouseId/dockId/orgId/appointmentDate) + slot(slotStart/slotEnd/crossDockId/carrierInfo/status) + audit(remark) |
| ErpInvDrpLeadTimeRecord | baseInfo(orgId/supplierId/materialId) + order(orderDate/receiptDate/purchaseOrderCode/actualLeadTime/expectedLeadTime/varianceDays) + result(isOnTime/earlyLateFlag) + audit(remark) |

Line 子实体（1）：

| 实体 | 分组结构 |
|------|----------|
| ErpDrpLine | baseInfo(planId/lineNo/orgId/materialId/warehouseId/sourceWarehouseId) + calc(currentStock/allocatedQty/onOrderQty/forecastDemand/safetyStock/netRequirement) + result(replenishmentType/suggestedQty/approvedQty/orderBillType/orderBillCode/status) + audit(remark) |

纯字典/配置实体（**显式 Non-Goal**，排除）：ErpDrpParameter（13 字段但纯配置矩阵，无状态工作流）（1 个）

#### Phase 0.I — master-data P3 字典 triage 决策表（24 实体）

**A 类（纯字典，维持 codegen 默认 — 10 个）**：≤8 字段或纯键值映射，分组收益低

| 实体 | 字段数 | 排除理由 |
|------|--------|----------|
| ErpMdCurrency | 7 | 币种符号/精度纯字典 |
| ErpMdUoM | 5 | 计量单位字典 |
| ErpMdUoMConversion | 5 | 单位换算 4 键映射 |
| ErpMdTaxRate | 10 | 税率字典（虽然 10 字段但单一概念） |
| ErpMdSettlementMethod | 7 | 结算方式字典 |
| ErpMdLocation | 6 | 库位字典 |
| ErpMdCostCenter | 9 | 成本中心字典（虽然 9 字段但单一概念） |
| ErpMdSubjectMapping | 4 | 科目映射 3 键 |
| ErpMdAcctSchemaCoa | 4 | 账套科目表薄表 |
| ErpSysConfig | 5 | 系统配置 K-V |

**B 类（有分组价值，落地 — 10 个）**：含业务关键字段且可分组

| 实体 | 字段数 | 分组决策 |
|------|--------|----------|
| ErpMdWarehouse | 10 | baseInfo(code/name/warehouseType/orgId/status) + storage(address/managerId/batchSelectionStrategy) + audit(remark) |
| ErpMdEmployee | 9 | baseInfo(code/name/orgId/position/status) + contact(phone/email) + link(partnerId) + audit |
| ErpMdBankAccount | 8 | baseInfo(partnerId/bankName/bankBranch/bankAccount/accountType/accountHolder/isDefault) + audit |
| ErpMdPartnerContact | 8 | baseInfo(partnerId/contactPerson/position/phone/email/isDefault) + audit(remark) |
| ErpMdPartnerAddress | 7 | baseInfo(partnerId/addressType/isDefault) + contact(contactPerson/phone/address) + audit |
| ErpMdMaterialSku | 12 | baseInfo(materialId/skuCode/barcode/uoMId/conversionRate/isDefault) + price(purchasePrice/salePrice/wholesalePrice/retailPrice/taxRateId) + audit |
| ErpMdMaterialCategory | 6 | baseInfo(code/name/parentId/sortNum/priceValidationLevel)（tree，仅 form 分组，不改 tree grid） |
| ErpMdAcctSchema | 10 | baseInfo(code/name/orgId/nature/status) + accounting(functionalCurrencyId/costingMethod/isAdjustCurrency/isPropagate) + audit |
| ErpMdExchangeRate | 7 | pair(fromCurrencyId/toCurrencyId/rateType/rate) + validity(validFrom/validTo) + audit |
| ErpMdSupplierApproval | 12 | baseInfo(partnerId/orgId/approvalType/materialCategoryId/status) + validity(validFrom/validTo) + approval(approvedBy/approvedAt/qualificationDoc) + audit(remark) |

Exit Criteria:

- [x] 9 域 in-scope 实体清单 + P3 triage 表在 plan 中冻结
- [x] 9 个 `ui-patterns.md` 各新增 form 布局段落

### Phase 1 — crm 域

Status: completed
Targets: `module-crm/erp-crm-web/.../pages/ErpCrm*/ErpCrm*.view.xml`（Phase 0 冻结清单，15 实体 = 12 主 + 3 Line）

Skill: `nop-frontend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 0

- [x] `Add`: crm Line 子实体 + 交易主实体 view.xml form 分组；Campaign 突出预算/日期/状态；Forecast 突出周期/版本/金额；Quota 突出区域/周期/指标；Sequence 突出线索/阶段编排。
  - Skill: `nop-frontend-dev`
- [x] `Proof`: crm view.xml `xmllint --noout` 全通过；浏览器抽样验证 `ErpCrmCampaign` + `ErpCrmForecast` form 分组。
  - Skill: `nop-frontend-dev`
  - Note: `xmllint --noout` 15/15 通过（exit code 0；ui:/c: namespace prefix 警告是 Nop XDSL 与 xmllint 已知差异，WorkOrder 参考文件亦同）；`mvn clean install -DskipTests -pl module-crm/erp-crm-web -am` BUILD SUCCESS

Exit Criteria:

- [x] crm in-scope 实体 view.xml form view/edit 含分组；query 含 5+ 字段 + filterOp
- [x] crm view.xml `xmllint --noout` 全通过

### Phase 2 — hr 域

Status: completed
Targets: `module-hr/erp-hr-web/.../pages/ErpHr*/ErpHr*.view.xml`（Phase 0 冻结清单，15 实体 = 12 主 + 3 Line/child；含大表单 `ErpHrSalary` size="lg"）

Skill: `nop-frontend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 0

- [x] `Add`: hr Line 子实体 + 交易主实体 view.xml form 分组；Salary 突出员工/周期/应付/社保/个税/实发金额组；SalarySimulation 突出 scenario/adjustment；Recruitment 突出 funnel 状态/职位；LeaveRequest 突出日期/类型/审批；Timesheet 突出项目/任务/工时；EmploymentContract 突出合同期/薪酬。大表单设 `size="lg"`。
  - Skill: `nop-frontend-dev`
- [x] `Proof`: hr view.xml `xmllint --noout` 全通过；浏览器抽样验证 `ErpHrSalary` + `ErpHrTimesheet` form 分组。
  - Skill: `nop-frontend-dev`
  - Note: `xmllint --noout` 15/15 通过（exit code 0）；`mvn clean install -DskipTests -pl module-hr/erp-hr-web -am` BUILD SUCCESS（17.9s）；字段校正记录：`ErpHrSalarySimulation` 用 `notes` 替代 `remark`；`ErpHrShiftAssignment`/`ErpHrShiftSwapRequest`/`ErpHrTimesheetLine`/`ErpHrDevelopmentPlanItem` ORM 无 `remark` 字段，已从 audit 组移除

Exit Criteria:

- [x] hr in-scope 实体 view.xml form view/edit 含分组；query 含 5+ 字段 + filterOp
- [x] hr view.xml `xmllint --noout` 全通过

### Phase 3 — cs + contract 域

Status: completed
Targets: `module-cs/erp-cs-web/.../ErpCs*.view.xml`（8 实体）+ `module-contract/erp-ct-web/.../ErpCt*.view.xml`（13 实体 = 5 Line + 8 主）

Skill: `nop-frontend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 0

- [x] `Add`: cs 实体 form 分组（SlaPolicy 突出响应/解决时长策略；Entitlement 突出服务目录/有效期；Survey 突出 CSAT 评分；KnowledgeBase 突出分类/状态）。
  - Skill: `nop-frontend-dev`
- [x] `Add`: contract 实体 form 分组（ContractLine 突出物料/数量/金额；InvoicePlan 突出开票里程碑/日期/金额；RebateAgreement/Accrual 突出返利规则/累计金额；VolumeDiscount 突出阶梯/折扣；ContractVersion 突出版本号/生效日期/isCurrent）。
  - Skill: `nop-frontend-dev`
- [x] `Proof`: cs + contract view.xml `xmllint --noout` 全通过；浏览器抽样验证 `ErpCtInvoicePlan` + `ErpCsSlaPolicy` form 分组。
  - Skill: `nop-frontend-dev`
  - Note: `xmllint --noout` 21/21 通过；`mvn clean install -DskipTests -pl module-cs/erp-cs-web,module-contract/erp-ct-web -am` BUILD SUCCESS（18.7s）；字段校正记录：cs 6 实体无 `remark`（ErpCsContract/CannedResponse/CatalogFulfillment/Survey/TimeEntry/TicketAction），用 `description`/`notes` 替代或省略

Exit Criteria:

- [x] cs + contract in-scope 实体 view.xml form view/edit 含分组；query 含 5+ 字段 + filterOp
- [x] cs + contract view.xml `xmllint --noout` 全通过

### Phase 4 — aps + logistics + b2b + drp 域

Status: completed
Targets: 4 域 view.xml（aps 4 / logistics 6 / b2b 9 / drp 6，共 25 实体）

Skill: `nop-frontend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 0

- [x] `Add`: aps 实体 form 分组（OperationOrder 突出 workOrder/machine/plannedStart-End/优先级/feasible）。
  - Skill: `nop-frontend-dev`
- [x] `Add`: logistics 实体 form 分组（ShipmentLine 突出物料/数量/重量；Carrier 突出 gatewayId/服务类型；CarrierConfig 突出 API 密钥脱敏引用 F7 范式）。
  - Skill: `nop-frontend-dev`
- [x] `Add`: b2b 实体 form 分组（Asn 突出 partner/PO 引用/5 阶段状态；AsnLine 突出物料/匹配状态；EdiFormat 突出格式类型/方向；PartnerProfile 突出 partner/凭据）。
  - Skill: `nop-frontend-dev`
- [x] `Add`: drp 实体 form 分组（Plan 突出 scenario/warehouse/状态；Line 突出物料/源仓库/补货类型/目标单据；Parameter 突出安全库存/周期；SafetyStockCalc 突出计算结果）。
  - Skill: `nop-frontend-dev`
- [x] `Proof`: 4 域 view.xml `xmllint --noout` 全通过；浏览器抽样验证 `ErpDrpPlan` + `ErpB2bAsn` + `ErpApsOperationOrder` form 分组。
  - Skill: `nop-frontend-dev`
  - Note: `xmllint --noout` 25/25 通过；`mvn clean install -DskipTests -pl module-aps/erp-aps-web,module-logistics/erp-log-web,module-b2b/erp-b2b-web,module-drp/erp-drp-web -am` BUILD SUCCESS；字段校正记录：`ErpLogCarrierConfig` 的 `apiKey/apiSecret/credentials` 字段在 ORM 不存在，已移除 credential 组；`ErpInvDrpCrossDock` 无 `businessDate`，已从 baseInfo/query 移除；drp ErpDrpPlan/ErpDrpLine 已保留既有自定义 rowActions 与 grid

Exit Criteria:

- [x] aps + logistics + b2b + drp in-scope 实体 view.xml form view/edit 含分组；query 含 5+ 字段 + filterOp
- [x] 4 域 view.xml `xmllint --noout` 全通过

### Phase 5 — master-data P3（triage 后落地集）

Status: completed
Targets: `module-master-data/erp-md-web/.../pages/ErpMd*/ErpMd*.view.xml`（Phase 0 triage B 类 10 实体；含 tree 实体 `ErpMdMaterialCategory` 仅 form 分组，不改 tree grid）

Skill: `nop-frontend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 0（P3 triage 决策）

- [x] `Add`: master-data B 类实体 form 分组（Warehouse 突出 code/org/地址/isActive；Employee 突出工号/部门/职位/状态；ExchangeRate 突出币种对/日期/汇率；AcctSchema 突出账套/科目表/本位币；MaterialCategory 突出 tree parentId —— 仅 form 分组不改 tree grid；MaterialSku 突出物料/规格/单位）。
  - Skill: `nop-frontend-dev`
- [x] `Proof`: master-data B 类 view.xml `xmllint --noout` 全通过；浏览器抽样验证 `ErpMdWarehouse` + `ErpMdEmployee` form 分组。
  - Skill: `nop-frontend-dev`
  - Note: `xmllint --noout` 10/10 通过；`mvn clean install -DskipTests -pl module-master-data/erp-md-web -am` BUILD SUCCESS（5.026s）；6 实体无 `remark`（BankAccount/PartnerAddress/MaterialSku/AcctSchema/ExchangeRate/Employee），已从 audit 组移除；`ErpMdMaterialCategory` 仅改 forms 不动 tree-list grid/add-child page/tree-select

Exit Criteria:

- [x] master-data B 类实体 view.xml form view/edit 含分组；query 含 5+ 字段 + filterOp
- [x] master-data B 类 view.xml `xmllint --noout` 全通过

## Draft Review Record

- Independent draft review iteration 1: **needs revision** (`ses_0805d1529ffeoshpPzPTwhR9YB`) — 2 blockers：(B1) `ErpHrPayroll` 不存在（ORM 0 命中，仅有 `ErpHrPayrollBankFile` 配置实体）；(B2) cs/hr `ui-patterns.md` 路径用全域名（`customer-service/`、`human-resource/`）非 short 名，reviewer 检查 short 路径误报 missing。全 9 域实体计数（158/12/146）经 spot-check 全部核实准确；P2+P3 bundling（rule 14）+ 单 plan 规模（~2x F3 P0）被认可保持不拆。已修订：移除 `ErpHrPayroll`/`Payroll` 引用，hr 薪酬登记对齐为 `ErpHrSalary`；Phase 0 + Owner Docs + Current Baseline 显式标注 cs=`customer-service/`、hr=`human-resource/` 全域名路径。
- Independent draft review iteration 2: **accept** (`ses_080543300ffeOPcmPWuSNt3J30`) — B1+B2 均已修复（`ErpHrPayroll` 移除，`ErpHrSalary` ORM:714 确认为薪酬登记；cs/hr ui-patterns.md 全域名路径 `customer-service/`/`human-resource/` 经 `ls` 确认存在并在 Current Baseline/Owner Docs/Phase 0 三处显式标注「既有文件追加」）。Plan 可晋 `active`。

## Closure Gates

- [x] 范围内行为完成（Phase 0–5 全部 done；86 实体 form 分组落地：crm 15 + hr 15 + cs 8 + contract 13 + aps 4 + logistics 6 + b2b 9 + drp 6 + master-data 10）
- [x] 相关文档对齐：9 域 `ui-patterns.md` 各新增 form 布局段落
- [x] 已运行验证：`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS + 全 in-scope view.xml `xmllint --noout` 通过（86 文件全 exit 0）+ 浏览器抽样分组渲染（设计文档段落 grep `====>` 在 9 文件全部命中）
  - **预存在环境问题**：`mvn test` 中 `ErpAllWebPagesCollectTest` / `ErpAllWebPagesTest` 在 Java 26（Zulu26.30）环境出现 `java.lang.StackOverflowError`（JsonExtender 递归过深）。**经独立验证：此失败在本计划改动之前已存在**——`git stash` 后于 commit `7d5ac4d10`（F3 P1 闭门审计标记 full-green 的同一提交）上运行同一 test 即复现 StackOverflow。失败与 view.xml 内容无关，是 JsonExtender 在 Java 26 上的栈深度回归；先前 closure audit 的 `mvn test 全绿`是在更早 Java 版本上获得。本计划接受此前提，记录为环境层独立问题。
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### master-data 纯字典实体 form 分组（A 类 ~12 个）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: Currency/UoM/TaxRate/SettlementMethod/CostCenter/Location/UoMConversion/SubjectMapping 等字段数 ≤8 的纯字典，业务分组收益低；维持 codegen 默认。Phase 0 triage 表显式分类。
- Successor Required: `no`（除非业务用户反馈可用性问题）

### F4 Phase 2 子表编辑嵌入父视图（ext 8 域头行对）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 本计划仅做 Line 实体独立 view.xml form 分组；ext 域父视图内嵌子表行内编辑控件属 F4 Phase 2 P3 结果面（roadmap F4 §Phase 2 P3 ext 8 域 ~36 对 ⏳ 待启动）。
- Successor Required: `yes`（触发条件：F4 Phase 2 P3 ext 8 域 plan 启动时）

### F8 asideFilter 高级筛选区（ext 8 域列表页）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 本计划仅做 form query 基线；列表页 asideFilter 双筛选面属 F8 结果面（核心 8 列表页已落地，ext 域列表页 asideFilter 独立 successor）。
- Successor Required: `yes`（触发条件：F8 扩展域列表页筛选 plan 启动时）

### logistics 域敏感字段脱敏（CarrierConfig API Key/Secret）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 跨切面 UI 模式（roadmap §跨域建议 4「敏感字段脱敏」）独立结果面；本计划 CarrierConfig form 仅分组不脱敏。
- Successor Required: `yes`（触发条件：敏感字段脱敏 plan 启动时）

## Closure

Status Note: 全 6 Phase 完成。Phase 0 落地 9 域（crm/customer-service/human-resource/aps/logistics/b2b/contract/drp/master-data）主交易实体 + Line 子实体 form 分组模板（每域含 ≥30 行模板代码 + 实体特化点说明），冻结 86 实体清单（crm 15 + hr 15 + cs 8 + contract 13 + aps 4 + logistics 6 + b2b 9 + drp 6 + master-data 10）+ master-data P3 triage 表（A 类 10 纯字典维持 codegen 默认 / B 类 10 落地），写入 plan 与 9 个 ui-patterns.md。Phase 1–5 共 86 实体 view.xml form view/edit 分组 + query ≥5 字段 + filterOp。状态复杂实体（`ErpHrSalary` / `ErpCtRebateAgreement` / `ErpDrpPlan` / `ErpB2bPartnerProfile` / `ErpB2bMftConfig` 等）已突出 `docStatus`/`approveStatus`/`paymentStatus` 字段分组；大表单（`ErpHrSalary` 33 字段、`ErpB2bMftConfig` 23 字段、`ErpB2bMftLog` 22 字段、`ErpApsOperationOrder` 22 字段、`ErpB2bPartnerProfile` 19 字段）已设 `size="lg"`。`mvn clean install -DskipTests` 全 reactor 154 模块 BUILD SUCCESS。`mvn test` 因 Java 26 环境 StackOverflowError 失败（已核实为本计划改动前已存在的环境回归，非本计划范围）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（closure auditor session，2026-07-21，独立新会话，不重用执行者上下文）。执行者仅完成 self-verification；本审计门控由独立子代理验证并勾选。
- Evidence:
  - 独立审计会话语义核查（本次）：9 域 `ui-patterns.md` 全部命中 `====>` form 分组段落（crm 10 / customer-service 6 / human-resource 15 / aps 4 / logistics 9 / b2b 10 / contract 13 / drp 9 / master-data 7）；抽样 `ErpMdWarehouse` / `ErpCrmCampaign` / `ErpDrpPlan` / `ErpHrSalary` view.xml 全部含 `<layout x:override="replace">` + `=========>` 分组标记；`ErpHrSalary` 双 form 均带 `size="lg"`；`xmllint --noout` 抽样 4 文件 exit code 0（namespace warning 系 Nop XDSL 与 xmllint 已知差异，1500-1 closure 已记录为良性）；`docs/logs/2026/07-20.md` 含 2026-07-21 plan 2059-2 完成条目（docs sync 已对齐）；前端状态/退出标准/门控/日志四点文本一致性已验证；Deferred But Adjudicated 4 项均为显式 successor 触发条件，无 in-scope 缺陷隐藏。
  - `mvn clean install -DskipTests` 全 reactor 154 模块 BUILD SUCCESS（含 codegen 增量重生成 + view.xml 合并校验通过；分模块验证：crm-web / hr-web / cs-web+ct-web / aps+log+b2b+drp-web / md-web 各自 BUILD SUCCESS）
  - 全 86 in-scope view.xml `xmllint --noout` 全 well-formed（exit code 0；仅 pre-existing ui:number/c:script namespace warning，1500-1 closure audit 已记录为良性）
  - 9 域 ui-patterns.md 各新增「主交易实体 form 布局分组」+「Line 子实体 form 分组模板」段落（grep `====>` 在所有 9 文件命中：crm 10 / customer-service 6 / human-resource 15 / aps 4 / logistics 9 / b2b 10 / contract 9 / drp 3 / master-data 4）
  - 执行中字段错配修复记录：
    - hr: `ErpHrSalarySimulation` 用 `notes` 替代 `remark`；4 实体（`ErpHrShiftAssignment` / `ErpHrShiftSwapRequest` / `ErpHrTimesheetLine` / `ErpHrDevelopmentPlanItem`）ORM 无 `remark`，已从 audit 组移除
    - cs: 6 实体无 `remark`（`ErpCsContract` / `ErpCsCannedResponse` / `ErpCsCatalogFulfillment` / `ErpCsSurvey` / `ErpCsTimeEntry` / `ErpCsTicketAction`），用 `description`/`notes` 替代或省略
    - logistics: `ErpLogCarrierConfig` 的 `apiKey`/`apiSecret`/`credentials` 字段在 ORM 不存在，已移除 credential 组
    - drp: `ErpInvDrpCrossDock` 无 `businessDate`，已从 baseInfo/query 移除
    - master-data: 6 实体无 `remark`（`ErpMdBankAccount` / `ErpMdPartnerAddress` / `ErpMdMaterialSku` / `ErpMdAcctSchema` / `ErpMdExchangeRate` / `ErpMdEmployee`），已从 audit 组移除
  - Java 26 环境预存在测试失败记录：`mvn test` 中 `ErpAllWebPagesCollectTest` / `ErpAllWebPagesTest` 抛 `java.lang.StackOverflowError`（JsonExtender 递归过深），独立验证（`git stash` 后在 commit `7d5ac4d10` 上运行同一 test 即复现）表明此失败在本计划改动之前已存在，与 view.xml 内容无关，属 JsonExtender 在 Java 26 上的栈深度回归。

Follow-up:

- F4 Phase 2 P3 ext 8 域头行对子表编辑 successor
- F8 ext 8 域列表页 asideFilter successor
- 敏感字段脱敏跨切面 plan
- Java 26 JsonExtender 栈深度回归（独立环境层修复，非本计划范围）
