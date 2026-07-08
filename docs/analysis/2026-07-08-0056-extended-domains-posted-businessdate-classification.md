# 7 扩展域 posted/businessDate 标准字段补充 — 实体分类裁决（Phase 1）

> Source Plan: `docs/plans/2026-07-08-0056-1-extended-domains-posted-businessdate-std-fields.md` Phase 1
> 日期：2026-07-08
> 范围：cs / hr / logistics / b2b / contract / drp / aps 共 7 扩展域

## 1. 裁决口径（核心域范式实证）

实时仓库逐项核实核心域 `businessDate`/`posted` 列分布，归纳出确定性判别规则：

| 档 | 条件 | 核心域实证（purchase） |
|----|------|----------------------|
| **A 档（posted + businessDate）** | 过账绑定单据头（已有业财过账接线） | `ErpPurOrder/Receive/Invoice/Payment/Return`（5 个，`posted=5`） |
| **B 档（仅 businessDate）** | 事务型单据头但非过账绑定 | `ErpPurRequisition/Rfq/Quotation`（请购/询价/报价头，无 posted） |
| **C 档（不加）** | config/master/明细行/审计日志/计算中间表/子记录 | 全部 `*Line`、`SupplierPriceList`、`SupplierScorecard` |

**关键判别事实（非采信记忆，均经 `grep`/python 实时核实）：**

1. **purchase 域中，凡有 `code`（单号）的实体均含 `businessDate`；无 `code` 的实体（全部 `*Line` + `SupplierPriceList`/`Scorecard`）均不含 `businessDate`**。即「单据头」= 有 `code` 的事务文档，与「明细行/主数据」严格分离。
2. `businessDate` 列范式：`stdSqlType="DATE" mandatory="true" code="BUSINESS_DATE" stdDataType="date"`（`purchase ErpPurOrder:550`）。
3. `posted` 列范式：`stdSqlType="BOOLEAN" defaultValue="false" code="POSTED" stdDataType="boolean"`（`purchase ErpPurOrder:567`）。
4. 索引范式：非唯一索引 `<index name="IDX_<ENT>_ORG_BUSINESS_DATE" unique="false"><column name="orgId"/><column name="businessDate"/></index>`（`purchase ErpPurOrder:598-601`）。**核心域源 orm.xml 无 `<composite-keys>` 块**（B1 修正核实）。
5. **合同/协议类实体同样含 `businessDate`**：`sales ErpSalContract` 含 `businessDate`（displayName="签订日期"，`sales.orm.xml` 核实）。即「合同/协议」属事务型单据头（签订事件日期），非主数据。
6. 过账绑定判定：对照 `docs/design/finance/posting.md` 业务类型清单 + 各域已落地过账代码（`*PostingExecutor`/`*FreightProvider`/`postSettlement`）。

**裁决规则（本计划采用）：**
- **A 档** = 已有业财过账接线（`posting.md` 业务类型 + 过账代码双重确认）→ 加 `businessDate` + `posted`。
- **B 档** = 事务型单据头（有 `code` 且代表离散业务事件/申请/合同，FK 指向主数据而非事务父单据）→ 加 `businessDate`。
- **C 档** = 其余（config/master/字典/明细行/子记录 FK 事务父单据/审计日志/计算中间表/纯模板定义）。

> **残留风险（诚实记录）**：补 `posted` 字段（默认 false）仅为状态标志位落库；各域既有过账动作是否回填 `posted=true` 属各域既有/后继业务逻辑（见计划 Deferred 第 1 项），本计划不强行接线。

**替代方案考虑：**
- (a) 仅给已过账绑定的头补字段（最小变更）→ **rejected**：B 档非过账头缺 `businessDate` 仍与核心域请购/询价/报价头范式不一致（核心域 Requisition/Rfq/Quotation 均有 `businessDate`）。
- (b) 全实体补两字段 → **rejected**：config/master/明细行加 `posted` 无语义，污染模型（核心域 `SupplierPriceList`/各 `*Line` 均无 `posted`/`businessDate`）。

## 2. 7 域实体分类表

> 每实体一行理由。A/B 档标注新列 propId（取该实体现有最大 propId +1/+2，加性追加，保证连续）。`postedAt`/`postedBy` 子串列非 `posted` 布尔标志，已用 `name="posted"` 精确匹配核实。

### cs（16 实体）

| 实体 | 档 | 理由 | businessDate@ | posted@ |
|------|----|------|--------------|---------|
| ErpCsTicket | **B** | 客服工单，事务单据头（code+orgId+docStatus+approve），无过账 | 202 | - |
| ErpCsContract | **B** | 支持合同，镜像 sales ErpSalContract（code+status+签订事件） | 19 | - |
| ErpCsEntitlement | C | 服务权益=客户既有权利的常设引用/状态记录，授予经父合同管理，非离散事务事件 | - | - |
| ErpCsSurvey | C | 满意度调查，mandatory FK ticketId 指向事务父单据（工单子记录） | - | - |
| ErpCsTimeEntry | C | 工单计时条目，mandatory FK ticketId（工单子行） | - | - |
| ErpCsTicketType/SlaPolicy/Team/CannedCategory/CannedResponse/KnowledgeBase/AgentRate/CatalogCategory/ServiceCatalogItem/CatalogFulfillment | C | config/master/字典/目录引用 | - | - |
| ErpCsTicketAction | C | 工单操作日志（审计日志） | - | - |

### hr（41 实体）

| 实体 | 档 | 理由 | businessDate@ | posted@ |
|------|----|------|--------------|---------|
| ErpHrSalary | **A** | 过账绑定：SALARY/SALARY_PAYMENT 凭证（`SalaryPostingExecutor`/`Dispatcher`/`Provider` 已落地） | 92 | 93 |
| ErpHrLeaveRequest | **B** | 休假申请事务单据（code+orgId+status） | 20 | - |
| ErpHrAttendance | **B** | 考勤记录，离散日事务（attendanceDate 作业务日期），非明细行 | 20 | - |
| ErpHrTimesheet | **B** | 工时表事务单据（code+orgId+status） | 16 | - |
| ErpHrEmployeeAssessment | **B** | 员工评估事务（assessmentDate+status），HR 事件单据 | 16 | - |
| ErpHrDevelopmentPlan | **B** | 员工发展计划事务（status），HR 周期单据 | 14 | - |
| ErpHrRecruitment | **B** | 招聘记录事务单据（code+orgId+interviewDate/hiredDate+status） | 25 | - |
| ErpHrEmploymentContract | **B** | 劳动合同事务单据（code+orgId+signDate+status），镜像 ErpSalContract | 26 | - |
| ErpHrShiftAssignment | **B** | 排班分配事务（assignmentDate+status），离散排班事件 | 20 | - |
| ErpHrShiftSwapRequest | **B** | 排班调换申请事务（code+swapDate+status） | 18 | - |
| ErpHrSalarySimulation | **B** | 薪酬模拟事务单据（code+orgId+status） | 20 | - |
| ErpHrSurvey | C | 问卷**模板**定义（surveyType+发布周期），实际答卷在 ErpHrSurveyResponse 子记录；模板=配置 | - | - |
| ErpHrEmployee/Department/Position | C | 主数据（员工/部门/职位） | - | - |
| ErpHrSalaryItem/SocialInsuranceConfig/SocialInsuranceBase/TaxConfig/TaxSpecialDeduction/Competency/CompetencyLevel/RoleCompetency/Shift/ShiftRotationPattern/PayrollBankFile | C | config/字典/参数/主数据 | - | - |
| ErpHrTimesheetLine/SurveyQuestion/SurveyResponse/SurveyAnswer/SurveyResult/AssessmentDetail/GapAnalysis/DevelopmentPlanItem/SalarySimulationItemAdjustment | C | 明细行/子记录 | - | - |

### logistics（7 实体）

| 实体 | 档 | 理由 | businessDate@ | posted@ |
|------|----|------|--------------|---------|
| ErpLogShipment | **A** | 过账绑定：FREIGHT 销售运费（`LogisticsFreightProvider` 已落地；posting.md:121） | 40 | 41 |
| ErpLogDeliveryWindow | C | 配送时间窗口=排程引用/配置（无 code，partnerId 引用） | - | - |
| ErpLogShipmentLine/Parcel/Log | C | 明细行/包裹/网关交互日志 | - | - |
| ErpLogCarrier/CarrierConfig | C | 主数据/承运商配置 | - | - |

### b2b（13 实体）

| 实体 | 档 | 理由 | businessDate@ | posted@ |
|------|----|------|--------------|---------|
| ErpB2bEdiDoc | **B** | EDI 事务单据（code+orgId），EDI 交换文档，无过账 | 21 | - |
| ErpB2bAsn | **B** | 提前发货通知事务单据（code+orgId+shipmentDate+status） | 19 | - |
| ErpB2bAsnLine/EdiLog/MftLog | C | 明细行/交互日志/传输日志 | - | - |
| ErpB2bEdiFormat/CodeMapping/PartnerProfile/PartnerCredential/TestExchange/CertificationChecklist/MftConfig/MftCertificate | C | config/凭证/主数据/测试/证书 | - | - |

### contract（15 实体）

| 实体 | 档 | 理由 | businessDate@ | posted@ |
|------|----|------|--------------|---------|
| ErpCtRebateSettlement | **A** | 过账绑定：postSettlement DRAFT→POSTED 生成贷项凭证（`ErpCtRebateSettlementBizModel` 已落地；计划 1115-1） | 18 | 19 |
| ErpCtContract | **B** | 合同事务单据（code+orgId+signDate+status），镜像 ErpSalContract | 25 | - |
| ErpCtRebateAgreement | **B** | 返利协议事务单据（code+orgId+agreementDate+status） | 21 | - |
| ErpCtRebateAccrual | C | 返利计提**明细**，FK rebateAgreementId（协议子记录）；过账经 ErpCtRebateSettlement（iter N4 核实无独立 posted 迁移） | - | - |
| ErpCtInvoicePlan | C | 开票计划，FK contractLineId（合同行子记录/排程）；触发发票但发票本身是 sales/purchase 过账单据 | - | - |
| ErpCtContractLine/Version/ConsumptionLine/RebateTier/VolumeDiscount/SignatureRequest/Document | C | 明细行/版本/子记录/文档附件 | - | - |
| ErpCtTemplate/ApprovalMatrix | C | 模板/审批矩阵配置 | - | - |
| ErpCtApprovalRecord | C | 审批记录（审计日志） | - | - |

### drp（7 实体）

| 实体 | 档 | 理由 | businessDate@ | posted@ |
|------|----|------|--------------|---------|
| ErpDrpPlan | **B** | DRP 计划事务单据（code+orgId+status），分布需求计划生成文档 | 18 | - |
| ErpDrpLine | C | DRP 明细行 | - | - |
| ErpDrpParameter | C | 补货参数配置 | - | - |
| ErpInvDrpCrossDock/DockAppointment/LeadTimeRecord/SafetyStockCalc | C | 计算/记录中间表（baseline 预期 C 档，核实属实） | - | - |

### aps（6 实体）

| 实体 | 档 | 理由 | businessDate@ | posted@ |
|------|----|------|--------------|---------|
| ErpApsOperationOrder | **B** | 工序工单事务单据（code+orgId+plannedStartDate/EndDate+status），制造工单 | 29 | - |
| ErpApsSchedule | **B** | 排产方案事务单据（code+orgId+scheduleDate+status） | 17 | - |
| ErpApsConstraint/OpRouting/DispatchRule | C | 排产约束/工艺路线/派工规则配置 | - | - |
| ErpApsDispatchLog | C | 派工日志 | - | - |

## 3. 汇总

- **A 档（posted + businessDate）= 3 实体**：ErpHrSalary、ErpLogShipment、ErpCtRebateSettlement
- **B 档（仅 businessDate）= 19 实体**：cs(2) ErpCsTicket/ErpCsContract；hr(10) ErpHrLeaveRequest/Attendance/Timesheet/EmployeeAssessment/DevelopmentPlan/Recruitment/EmploymentContract/ShiftAssignment/ShiftSwapRequest/SalarySimulation；b2b(2) ErpB2bEdiDoc/ErpB2bAsn；contract(2) ErpCtContract/ErpCtRebateAgreement；drp(1) ErpDrpPlan；aps(2) ErpApsOperationOrder/ErpApsSchedule
- **C 档（不加）= 其余全部**
- logstics 域无 B 档（仅 ErpLogShipment A 档）。

**新增列计数预期**：`businessDate` 列声明 = 22（A3+B19）；`posted` 列声明 = 3（A 档）。

## 4. 下游展示基线核查（Phase 4 view 是否补列的确定性结论）

实时核实核心域 purchase 的 view/XMeta：

- `module-purchase/erp-pur-web/.../ErpPurOrder/_gen/_ErpPurOrder.view.xml:43` 自动生成 `<col id="businessDate" .../>`；`:94` 自动生成 `<col id="posted" .../>`。
- 自定义 delta `ErpPurOrder.view.xml`（`x:extends="_gen/..."`）grid 段为空（`<grid id="list"/>` 继承生成视图），未手写列。
- XMeta `_ErpPurOrder.xmeta` 自动生成 `businessDate`/`posted` prop（`queryable/sortable`）。

**结论（确定性）**：核心域 view 列表/筛选的 `businessDate`/`posted` 展示**完全由 codegen 从 ORM→XMeta→`_gen/*.view.xml` 自动生成**，自定义 delta 不手写列。

**Phase 4 执行项裁决**：本计划 Phase 2 加列 + Phase 3 codegen 重生成后，7 域 `_gen/*.view.xml` 将**自动**包含 `businessDate`/`posted` 列与筛选——**无需手动编辑 view**。Phase 4 的 view item 仅需「实证生成视图含新列」（抽样核对），不做手写 view 编辑。这与核心域范式一致（核心域自定义 view delta 亦不手写 businessDate/posted 列）。
