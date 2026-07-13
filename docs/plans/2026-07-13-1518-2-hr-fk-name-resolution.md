# 2026-07-13-1518-2-hr-fk-name-resolution HR 域外键名称解析批量推广（列表页 ID→名称）

> Plan Status: completed
> Last Reviewed: 2026-07-13
> Source: `docs/plans/2026-07-11-1643-1-amis-frontend-quality.md` Deferred「全量 1,036 FK 列名称解析」（Successor Required: yes，触发条件已满足）
> Related: `2026-07-11-1643-1-amis-frontend-quality.md`（机制 D 范式源）、`2026-07-13-1518-1-crm-cs-fk-name-resolution.md`（同批 N=1，无依赖）、`2026-07-13-1518-3-foundational-remaining-fk-name-resolution.md`（同批 N=3，无依赖）
> Audit: required

## Current Baseline

经实时仓库核实（HEAD 范围，独立子代理全量盘点 HR ORM + 生成网格 + 生成 xmeta）：

### 机制 D 已验证（全域 12 批次 79+ 实体）

机制 D 三层接线已由 12 前序批次验证。

### HR 域覆盖现状

- **零 FK 名称解析覆盖**：全部 36 实体的自定义 view.xml 为空 `<grid id="list"/>`（继承生成基线列原样）。
- **全部 36 实体均含至少一个数值 FK 列**（至少 orgId）——本计划范围。

> **重要修正**：`ownerId` / `approvedById` / `createdById` 等 `stdDomain="userId"` 列为 VARCHAR 字符串类型，在生成网格中**不**显示为 `ui:number="true"`，不属于机制 D 范畴。`leaveType` 为 String 字典列同理。HR 人员引用（superior/approver/interviewer/assessor/mentor/manager 等）均解析为 `ErpHrEmployee`（域内自引用），**非** `ErpMdEmployee`。`ErpHrEmployee` 无 `name` 列，显示用 `fullName` 或 `code`。

### 未覆盖 36 HR 实体清单（生成网格中显示为 `ui:number="true"` 的 FK 列）

| # | 实体 | 生成网格中原始 `*Id` FK 列 | 用户面价值 |
|---|------|---------------------------|-----------|
| 1 | **ErpHrEmployee** | orgId, departmentId, positionId, superiorId, costCenterId, bankAccountId, userAccountId‡ | 员工（7 FK 列，‡无 ext:relation） |
| 2 | **ErpHrDepartment** | parentId, managerId, costCenterId, orgId | 部门 |
| 3 | **ErpHrPosition** | departmentId, orgId | 岗位 |
| 4 | **ErpHrEmploymentContract** | employeeId, salaryCurrencyId, orgId | 雇佣合同 |
| 5 | **ErpHrLeaveRequest** | employeeId, approverId, orgId | 请假申请 |
| 6 | **ErpHrLeaveBalance** | employeeId, orgId | 假期余额 |
| 7 | **ErpHrTimesheet** | employeeId, orgId | 工时表 |
| 8 | **ErpHrTimesheetLine** | timesheetId†, employeeId, projectId, taskId | 工时行（†目标无 name） |
| 9 | **ErpHrAttendance** | employeeId, leaveRequestId, orgId | 出勤 |
| 10 | **ErpHrSalary** | employeeId, bankFileId†, orgId | 工资（†目标无 name） |
| 11 | **ErpHrRecruitment** | positionId, departmentId, interviewerId, employeeId, orgId | 招聘（5 FK 列） |
| 12 | **ErpHrSalarySimulation** | orgId, sourceSalaryId†, reviewerId, convertedSalaryId† | 薪资模拟（†目标无 name） |
| 13 | **ErpHrSalarySimulationItemAdjustment** | simulationId, employeeId, orgId | 模拟调整 |
| 14 | **ErpHrSalaryItem** | orgId | 薪资项 |
| 15 | **ErpHrSocialInsuranceConfig** | orgId | 社保配置 |
| 16 | **ErpHrSocialInsuranceBase** | employeeId, orgId | 社保基数 |
| 17 | **ErpHrTaxConfig** | orgId | 税务配置 |
| 18 | **ErpHrTaxSpecialDeduction** | employeeId, orgId | 税务专项扣除 |
| 19 | **ErpHrPayrollBankFile** | bankId, orgId | 银行代发 |
| 20 | **ErpHrShift** | orgId | 班次 |
| 21 | **ErpHrShiftAssignment** | orgId, employeeId, shiftId, leaveRequestId, swapRequestId†, replacedByAssignmentId† | 排班分配（†目标无 name） |
| 22 | **ErpHrShiftRotationPattern** | orgId, groupId | 轮班模式（groupId 自引用） |
| 23 | **ErpHrShiftSwapRequest** | orgId, requesterId, targetEmployeeId, sourceAssignmentId†, targetAssignmentId† | 换班请求（†目标无 name） |
| 24 | **ErpHrSurvey** | targetDepartmentId, orgId | 调查 |
| 25 | **ErpHrSurveyQuestion** | surveyId | 调查题 |
| 26 | **ErpHrSurveyResponse** | surveyId, employeeId, orgId | 调查回复 |
| 27 | **ErpHrSurveyAnswer** | responseId†, questionId | 调查答案（†目标无 name） |
| 28 | **ErpHrSurveyResult** | surveyId, departmentId | 调查结果 |
| 29 | **ErpHrCompetency** | parentId, orgId | 能力 |
| 30 | **ErpHrCompetencyLevel** | competencyId | 能力等级 |
| 31 | **ErpHrRoleCompetency** | positionId, competencyId | 岗位能力要求 |
| 32 | **ErpHrEmployeeAssessment** | employeeId, assessorId, orgId | 绩效评估 |
| 33 | **ErpHrAssessmentDetail** | assessmentId†, competencyId | 评估明细（†目标无 name） |
| 34 | **ErpHrGapAnalysis** | employeeId, competencyId | 差距分析 |
| 35 | **ErpHrDevelopmentPlan** | employeeId, orgId | 发展计划 |
| 36 | **ErpHrDevelopmentPlanItem** | planId, competencyId, gapId†, mentorId | 发展计划项（†目标无 name） |

> † = 有 ext:relation 但目标实体（ErpHrTimesheet/Salary/ShiftAssignment/SurveyResponse/EmployeeAssessment/GapAnalysis/SalarySimulation）无 `name`/`code` 显示列，执行时裁决 fallback（getCode() 或 id）或保留原始 ID。‡ = 无 ext:relation。ErpHrEmployee 人员引用显示用 `fullName`（非 `name`）。

### ext:relation 缺口

| # | 实体 | FK prop | 处置裁决 |
|---|------|---------|---------|
| 1 | ErpHrEmployee | userAccountId | 保留原始 ID（外部 auth 用户引用，ORM 无 to-one relation） |

> 实际缺口仅 1 个（userAccountId）。leaveType 为 String 字典非 FK，approvedById/createdById 为 VARCHAR userId 非 FK，均不在数值 FK 范畴。

## Goals

- 36 HR 实体列表页的高价值用户面 FK 列显示名称而非原始 ID（经机制 D）。
- 高价值 FK 定义：维度型外键（org→orgName 读 `ErpMdOrganization.name`；department/targetDepartment→departmentName 读 `ErpHrDepartment.name`；position→positionName 读 `ErpHrPosition.name`；employee/approver/interviewer/assessor/mentor/requester/targetEmployee/manager/superior/reviewer→employeeDisplayName 读 `ErpHrEmployee.fullName`（非 ErpMdEmployee）；costCenter→costCenterName 读 `ErpMdCostCenter.name`；currency/salaryCurrency→currencyName 读 `ErpMdCurrency.name`；shift→shiftName 读 `ErpHrShift.name`；group/parent(self-ref on ShiftRotationPattern)→groupName 读 `ErpHrShiftRotationPattern.name`；competency→competencyName 读 `ErpHrCompetency.name`；survey→surveyTitle 读 `ErpHrSurvey.title`；question→questionText 读 `ErpHrSurveyQuestion.questionText`；plan→planName 读 `ErpHrDevelopmentPlan.planName`；bank→bankDisplayName 读 `ErpMdBankAccount.bankName`；bankFile→bankFileBatchNo 读 `ErpHrPayrollBankFile.batchNo`；leaveRequest→leaveRequestCode 读 `ErpHrLeaveRequest.code`；simulation→simulationCode 读 `ErpHrSalarySimulation.code`；parent→parentName 同实体自引用）+ 高价值跨域父单型（projectId→projectName 读 `ErpPrjProject.name`；taskId→taskName 读 `ErpPrjTask.name`）。
- 1 个 ext:relation 缺口（userAccountId@Employee）保留原始 ID。
- 派生 prop 名遵循 `{relation}Name`/`{relation}Code` 约定。
- 零 ORM/契约变更。

## Non-Goals

- **CRM/CS 域 FK 名称解析**——由 `2026-07-13-1518-1` 承接。
- **Master Data/Logistics/Contract/B2B/DRP/APS 域 FK 名称解析**——由 `2026-07-13-1518-3` 承接。
- **codegen 模板层 FK 名称解析方案**——经 0600-1 裁决否决。
- **drawer 子表/明细行子网格 FK 名称**——本计划仅处理主列表网格 `<grid id="list">`。
- **leaveType 字典列 / approvedById / createdById 等 userId 字符串列**——非数值 FK，不在机制 D 范畴。
- **1 个 ext:relation 缺口 FK（userAccountId@Employee）**——保留原始 ID，归 successor。
- **看板/报表 FK 名称**——已由 1643-1 Phase 4 覆盖。

## Task Route

- Type: `app-layer design change`
- Owner Docs: `docs/architecture/view-and-page-strategy.md`、`../nop-entropy/docs-for-ai/02-core-guides/cross-module-entity-reference.md`、`../nop-entropy/docs-for-ai/03-runbooks/add-bizloader-field.md`
- Skill Selection Basis: xmeta 派生 + view.xml bounded-merge → `nop-frontend-dev`；BizModel `@BizLoader` → `nop-backend-dev`；JUnit 测试 → `nop-testing`。
- Protected Areas: 无 ORM/ask-first 变更。

## Infrastructure And Config Prereqs

No infra prereqs beyond existing baseline。

## Execution Plan

### Phase 1 - 核心人事实体 FK 名称解析（Employee/Department/Position/EmploymentContract/LeaveRequest/LeaveBalance/Timesheet/TimesheetLine/Attendance/Salary/Recruitment）

Status: completed
Targets: 11 实体的 xmeta + BizModel + view.xml
Skill: `nop-frontend-dev`、`nop-backend-dev`

- Item Types: `Decision | Add`
- Prereqs: 无

- [x] `Decision`: 裁决 Phase 1 实体的目标 FK 清单——维度型 FK 全部解析（org/department/position/costCenter/currency/salaryCurrency）。人员引用（superior/approver/interviewer/employee）→employeeDisplayName 读 `ErpHrEmployee.fullName`（非 ErpMdEmployee，非 .name）。跨域（projectId/taskId@TimesheetLine）→projectName 读 ErpPrjProject.name、taskName 读 ErpPrjTask.title（目标实体无 name 列，执行时裁决 getTitle fallback）。bankAccountId@Employee 读 `ErpMdBankAccount.bankName`。bankFileId@Salary 读 `ErpHrPayrollBankFile.batchNo`。timesheetId@TimesheetLine 读 `ErpHrTimesheet.code`。leaveRequestId@Attendance 读 `ErpHrLeaveRequest.code`。1 个缺口（userAccountId@Employee）保留原始 ID。
  - Skill: `nop-backend-dev`
- [x] `Add`: 11 实体 xmeta 增派生 `*Name` prop。
  - Skill: `nop-frontend-dev`
- [x] `Add`: 11 实体 BizModel 增 `@BizLoader(forType = ErpHr*.class)` 方法。
  - Skill: `nop-backend-dev`
- [x] `Add`: 11 实体 view.xml `<grid id="list">` 改 `<cols x:override="bounded-merge">`。
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] 11 核心人事实体列表网格显示 `*Name` 而非原始 `*Id`

### Phase 2 - 薪资/社保/排班/调查实体 FK 名称解析（SalarySimulation/SimulationItemAdjustment/SalaryItem/SocialInsuranceConfig/SocialInsuranceBase/TaxConfig/TaxSpecialDeduction/PayrollBankFile/Shift/ShiftAssignment/ShiftRotationPattern/ShiftSwapRequest/Survey/SurveyQuestion/SurveyResponse/SurveyAnswer/SurveyResult）

Status: completed
Targets: 17 实体的 xmeta + BizModel + view.xml
Skill: `nop-frontend-dev`、`nop-backend-dev`

- Item Types: `Decision | Add`
- Prereqs: Phase 1 范式已验证

- [x] `Decision`: 逐项裁决 Phase 2 FK 处置——simulationId→simulationCode 读 `.code`；surveyId→surveyTitle 读 `.title`；shiftId→shiftName；groupId@ShiftRotationPattern→groupName 自引用；bankId@PayrollBankFile→bankDisplayName 读 `ErpMdBankAccount.bankName`；reviewer/requester/targetEmployee→employeeDisplayName 读 `ErpHrEmployee.fullName`；leaveRequestId→leaveRequestCode；swapRequestId@ShiftAssignment→swapRequestCode 读 `ErpHrShiftSwapRequest.code`；sourceSalaryId/convertedSalaryId@SalarySimulation 目标 ErpHrSalary 无 code/name→保留 ID；sourceAssignmentId/targetAssignmentId@ShiftSwapRequest、replacedByAssignmentId@ShiftAssignment 目标 ErpHrShiftAssignment 无 code/name→保留 ID；responseId@SurveyAnswer 目标 ErpHrSurveyResponse 无 code/name→保留 ID；questionId→questionText 读 `.questionText`。
  - Skill: `nop-backend-dev`
- [x] `Add`: 17 实体 xmeta 增派生 `*Name` prop。
  - Skill: `nop-frontend-dev`
- [x] `Add`: 17 实体 BizModel 增 `@BizLoader` 方法。
  - Skill: `nop-backend-dev`
- [x] `Add`: 17 实体 view.xml `<grid id="list">` 改 `<cols x:override="bounded-merge">`。
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] 17 薪资/社保/排班/调查实体列表网格显示 `*Name` 而非原始 `*Id`

### Phase 3 - 能力/评估/发展实体 FK 名称解析（Competency/CompetencyLevel/RoleCompetency/EmployeeAssessment/AssessmentDetail/GapAnalysis/DevelopmentPlan/DevelopmentPlanItem）

Status: completed
Targets: 8 实体的 xmeta + BizModel + view.xml
Skill: `nop-frontend-dev`、`nop-backend-dev`

- Item Types: `Decision | Add`
- Prereqs: Phase 2 范式已验证

- [x] `Decision`: 逐项裁决 Phase 3 FK 处置——competencyId→competencyName 读 `.name`；positionId→positionName 读 `.name`；parentId@Competency→parentName 自引用；employee/assessor/mentor→employeeDisplayName 读 `ErpHrEmployee.fullName`；planId→planName 读 `ErpHrDevelopmentPlan.planName`；assessmentId@AssessmentDetail 目标 ErpHrEmployeeAssessment 无 code/name→保留 ID；gapId@DevelopmentPlanItem 目标 ErpHrGapAnalysis 无 code/name→保留 ID。
  - Skill: `nop-backend-dev`
- [x] `Add`: 8 实体 xmeta 增派生 `*Name` prop。
  - Skill: `nop-frontend-dev`
- [x] `Add`: 8 实体 BizModel 增 `@BizLoader` 方法。
  - Skill: `nop-backend-dev`
- [x] `Add`: 8 实体 view.xml `<grid id="list">` 改 `<cols x:override="bounded-merge">`。
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] 8 能力/评估/发展实体列表网格显示 `*Name` 而非原始 `*Id`

### Phase 4 - BizLoader 测试验证

Status: completed
Targets: `module-hr/erp-hr-service/src/test/java/app/erp/hr/service/TestErpHrFkNameLoader.java`
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1-3 完成

- [x] `Add`: 新建 `TestErpHrFkNameLoader.java`（extends `JunitAutoTestCase`），经 `IGraphQLEngine` findList 请求 `*Name` 字段触发 `@BizLoader`，断言 `ErpHrEmployee`（departmentName/positionName/superiorDisplayName/costCenterName）+ `ErpHrRecruitment`（positionName/departmentName/interviewerDisplayName）+ `ErpHrShiftAssignment`（shiftName/employeeDisplayName）名称对齐。
  - Skill: `nop-testing`

Exit Criteria:

- [x] `TestErpHrFkNameLoader` 全方法绿，验证 `@BizLoader` 批量加载防 N+1 且名称正确

## Draft Review Record

- Independent draft review iteration 1: needs revision (`ses_0a4a40a51ffeXq70rK9LKO2z0T`，general agent 新会话) — BLOCKER：基线 FK 列清单系统性伪造（~13 个虚构 FK 列跨 6/7 抽查实体；supervisorId→实际 superiorId、employmentContractId 不存在、ErpHrShiftAssignment 6 FK 全部错误、recruiterId/hiringManagerId 不存在）。人员引用应解析为 ErpHrEmployee（域内自引用）非 ErpMdEmployee。经独立子代理全量 ORM+生成网格核实后，基线表已按真实列名重建。
- Independent draft review iteration 2: accept (`ses_0a49253b8ffeuVxE1C2s1KgXnV`，general agent 新会话) — 全部 iteration 1 BLOCKER 已修正：ErpHrEmployee 7 FK 列经 ORM 逐一核实（superiorId 非 supervisorId、无 employmentContractId、userAccountId 确认无 ext:relation），ErpHrShiftAssignment 6 FK 列核实（无 departmentId/patternId/supervisorId），ErpHrRecruitment 5 FK 列核实（无 hiringManagerId/recruiterId）。人员引用解析 ErpHrEmployee.fullName 确认。格式合规、反松弛通过、0 Blocker / 0 Major / 0 Minor。

## Closure Gates

> `mvn clean install -DskipTests`（154 模块）+ `mvn test -pl module-hr/erp-hr-service -am`（含 `TestErpHrFkNameLoader`）+ 36 view.xml `xmllint --noout`。

- [x] 范围内行为完成（36 HR 实体列表页 FK 显示名称）
- [x] 相关文档对齐（机制 D 范式无需更新；本计划为既有范式批量推广；父计划 `1643-1` Deferred Successor Progress 已追加 HR 批次完成记录）
- [x] 已运行验证（`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS + hr-service 114 `mvn test` 0 failures/0 errors 含新增 `TestErpHrFkNameLoader` 3 方法 + 36 view.xml `xmllint --noout` well-formed）
- [x] 无范围内项目降级（1 个 ext:relation 缺口 FK userAccountId@Employee 为 Non-Goal，已归 successor）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理执行（执行者未自我审计）
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### userAccountId@ErpHrEmployee 无 ext:relation

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 外部 auth 用户引用，ORM 无 to-one relation 声明，batchLoadProps 不可用。
- Successor Required: `yes`（触发条件：ext:relation 落地或业务需求要求解析时）

## Closure

Status Note: closed

Closure Audit Evidence:

- Auditor / Agent: 独立子代理（新会话 `ses_0a40f8d91ffe1yVQoyVpz8OwHb`，未参与实现），2026-07-13 针对实时仓库验证（不依赖计划勾选）。
- Verdict: **PASS**。6 项审计任务全部通过：
  1. 机制 D 计数检查（36 实体）— 36/36 实体均具备 (a) ≥1 `queryable="false"` 派生 prop、(b) ≥1 `@BizLoader(forType=…)` 方法含 `orm().batchLoadProps` + `orm_attached()` 守卫、(c) `<cols x:override="bounded-merge">`。零遗漏。
  2. FK ID 移除抽检（5 实体）— userAccountId@Employee、sourceSalaryId/convertedSalaryId@SalarySimulation、replacedByAssignmentId@ShiftAssignment、gapId@DevelopmentPlanItem、responseId@SurveyAnswer 均正确保留为 `*Id`。
  3. 特殊 getter 验证 — task→getTitle()（ErpPrjTask title 非 name）、superior→getFullName()（ErpHrEmployee fullName 非 name）、bankFile→getBatchNo()、survey→getTitle()、plan→getPlanName() 全部正确。
  4. 测试文件 — `TestErpHrFkNameLoader` extends `JunitAutoTestCase`，3 @Test 方法，经 `IGraphQLEngine` findList + `FieldSelectionBean` 请求 `*Name` 字段并 `assertEquals` 具体名称值。
  5. ORM 保持 — `git diff module-hr/model/` 修改数=0，零 ORM/契约变更目标达成。
  6. 构建 — `mvn clean install -DskipTests` HR 模块 BUILD SUCCESS。

Follow-up:

- 其余域 FK 名称解析 successor（见 `1518-3`）
