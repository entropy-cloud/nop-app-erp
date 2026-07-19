# 人力资源管理域 — 视图按钮需求覆盖分析

## 分析范围

本域共 36 个实体，分类如下：

| 分类 | 实体数 | 实体列表 |
|------|--------|----------|
| CRUD | 28 | AssessmentDetail, Competency, CompetencyLevel, Department, DevelopmentPlan, DevelopmentPlanItem, EmployeeAssessment, EmploymentContract, GapAnalysis, LeaveBalance, PayrollBankFile, Position, RoleCompetency, SalaryItem, SalarySimulation, SalarySimulationItemAdjustment, Shift, ShiftAssignment, ShiftRotationPattern, ShiftSwapRequest, SocialInsuranceBase, SocialInsuranceConfig, Survey, SurveyAnswer, SurveyQuestion, SurveyResponse, SurveyResult, TaxConfig, TaxSpecialDeduction, TimesheetLine |
| CRUD+WF | 2 | LeaveRequest, Salary |
| CRUD+Custom | 2 | Employee (row-transfer-button), Recruitment (screening/interview/offer/hire/reject) |
| Custom | 1 | Attendance (clock-in/clock-out toolbar, no add-button) |
| CRUD+WF 预期但实际 CRUD-only | 1 | Timesheet (blocker: submit/approve/reject all missing) |

实体数：36。_tmp/view-buttons/hr.md 中有 2 处分类错误：ErpHrLeaveRequest（标注 CRUD，应为 CRUD+WF）、ErpHrSalary（标注 CRUD，应为 CRUD+WF）。ErpHrTimesheet 标注 CRUD 但按钮数据正确（无 WF 按钮）；需求层面应为 CRUD+WF。

## 期望按钮推导依据

1. **CRUD 基线**（METHODOLOGY §1.1）：所有实体默认期望 toolbar {add-button, batch-delete-button} + row {row-view-button, row-update-button, row-delete-button}。
2. **审批/工作流基线**（METHODOLOGY §1.2）：状态机含 DRAFT→SUBMITTED→APPROVED→REJECTED/CANCELLED 的实体期望 submit/withdraw-approval/approve/reject/reverse-approve/cancel 按钮。对应实体：LeaveRequest（状态 DRAFT/SUBMITTED/APPROVED/REJECTED/CANCELLED）、Timesheet（状态 DRAFT/SUBMITTED/APPROVED/REJECTED）、Salary（approveStatus 含 UNSUBMITTED/SUBMITTED/APPROVED/REJECTED，paymentStatus 含 VOID）。
3. **ui-patterns.md** 专用按钮：
   - §1.1 员工列表页 toolbar：`[批量操作: 导出 / 调动 / 离职]` `[+ 新增员工]`
   - §1.3 操作列：`编辑/更多（dropdown）`
   - §1.4 条件按钮：employmentStatus=ACTIVE 时 [办理离职][部门调动]；PROBATION 时 [转正][办理离职]
   - §5.1 工时表：`[保存草稿] [提交审批]`
   - §6.2 招聘看板卡片操作：`面试安排 / 发 Offer / 拒绝（根据阶段显示不同操作）`
   - §7.1 薪酬审批 toolbar：`[审核] [导出] [查看明细]`
   - §7.2 审批状态条件按钮：审核人[通过][退回]、财务[复核通过][退回]、经理[审批][退回]、经理已审批后[生成银行文件][发放]
4. **STATE MACHINE**（README.md）：LeaveRequest DRAFT/SUBMITTED/APPROVED/REJECTED/CANCELLED；Timesheet DRAFT/SUBMITTED/APPROVED/REJECTED；Recruitment OPEN/SCREENING/INTERVIEW/OFFERED/HIRED/REJECTED/CLOSED（自定义流程，专用按钮）。

## 逐实体分析

### ErpHrAssessmentDetail — CRUD
- **期望按钮**: add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button
- **实际按钮**: 完全一致
- **差距**: 无
- **判定**: clean

### ErpHrAttendance — Custom
- **期望按钮**: CRUD 基线。attendance 是打卡数据实体，按业务语义 toolbar 应含 clock-in/clock-out（非手动新建）。
- **实际按钮**: toolbar: list-clock-in-button, list-clock-out-button；row: row-view-button, row-update-button, row-delete-button。无 add-button, batch-delete-button。
- **差距**:
  - add-button: missing (minor) — 考勤记录通常由打卡/签退生成，手动新增场景少但仍有补录需求。非阻塞。
  - batch-delete-button: missing (minor) — 批量删除考勤记录场景极少，可接受。
- **判定**: minor — toolbar 偏离 CRUD 基线属设计意图（clock-in/out 取代 add），但缺少手动入口可能影响补录场景。

### ErpHrCompetency — CRUD
- **期望按钮**: CRUD 基线
- **实际按钮**: 完全一致
- **差距**: 无
- **判定**: clean

### ErpHrCompetencyLevel — CRUD
- **期望按钮**: CRUD 基线
- **实际按钮**: 完全一致
- **差距**: 无
- **判定**: clean

### ErpHrDepartment — CRUD
- **期望按钮**: CRUD 基线（ui-patterns §3 以树形组织架构为主，但数据维护仍需标准 CRUD）
- **实际按钮**: 完全一致
- **差距**: 无
- **判定**: clean

### ErpHrDevelopmentPlan — CRUD
- **期望按钮**: CRUD 基线
- **实际按钮**: 完全一致
- **差距**: 无
- **判定**: clean

### ErpHrDevelopmentPlanItem — CRUD
- **期望按钮**: CRUD 基线
- **实际按钮**: 完全一致
- **差距**: 无
- **判定**: clean

### ErpHrEmployee — CRUD+Custom
- **期望按钮**:
  - toolbar: add-button (`[新增员工]`), batch-delete-button (CRUD 基线), 批量调动 (`[调动]`, ui-patterns §1.1), 批量离职 (`[离职]`, ui-patterns §1.1), 导出（info 级）
  - row: row-view-button, row-update-button (`[编辑]`), row-more-button (`[更多]`, ui-patterns §1.3), row-transfer-button (`[部门调动]` §1.4), row-resign-button (`[办理离职]` §1.4), row-regularize-button (`[转正]` §1.4 PROBATION 状态条件), row-delete-button
- **实际按钮**: toolbar: add-button, batch-delete-button；row: row-view-button, row-update-button, row-transfer-button, row-delete-button。存在 transfer 自定义页面。
- **差距**:
  - toolbar 批量调动按钮: missing (minor) — ui-patterns §1.1 `[批量操作: 导出 / 调动 / 离职]`。有行级 row-transfer-button，但缺少工具栏批量版本。
  - toolbar 批量离职按钮: missing (minor) — 同上 §1.1，缺少工具栏批量离职入口。
  - row-more-button: missing (info) — ui-patterns §1.3 操作列描述 `编辑/更多（dropdown）`，当前行操作为独立按钮平铺。
  - row-resign-button (`[办理离职]`): missing (minor) — ui-patterns §1.4 条件按钮，employmentStatus=ACTIVE/PROBATION 时显示。
  - row-regularize-button (`[转正]`): missing (minor) — ui-patterns §1.4 条件按钮，employmentStatus=PROBATION 时显示。
  - row-transfer-button: present ✓（对应 `[部门调动]`）
- **判定**: minor — 核心员工管理功能存在（新增/编辑/查看/删除/调动），但缺失条件按钮（离职/转正）和批量操作（批量调动/离职）。

### ErpHrEmployeeAssessment — CRUD
- **期望按钮**: CRUD 基线
- **实际按钮**: 完全一致
- **差距**: 无
- **判定**: clean

### ErpHrEmploymentContract — CRUD
- **期望按钮**: CRUD 基线。状态机 ACTIVE/EXPIRED/TERMINATED/SUSPENDED，无 DRAFT→SUBMITTED→APPROVED 审批流，不期望 WF 按钮。
- **实际按钮**: 完全一致
- **差距**: 无
- **判定**: clean

### ErpHrGapAnalysis — CRUD
- **期望按钮**: CRUD 基线
- **实际按钮**: 完全一致
- **差距**: 无
- **判定**: clean

### ErpHrLeaveBalance — CRUD
- **期望按钮**: CRUD 基线（假期余额计算实体，不需 WF）
- **实际按钮**: 完全一致
- **差距**: 无
- **判定**: clean

### ErpHrLeaveRequest — CRUD+WF
- **期望按钮**:
  - toolbar: add-button, batch-delete-button (CRUD 基线)
  - row: row-view-button, row-update-button, row-submit-button (`[提交]`, 状态 DRAFT→SUBMITTED), row-withdraw-approval-button (`[撤回]`, SUBMITTED→DRAFT, 标准 WF 按钮 METHODOLOGY §1.2), row-approve-button (`[批准]`, SUBMITTED→APPROVED), row-reject-button (`[驳回]`, SUBMITTED→REJECTED), row-cancel-button (`[取消]`, DRAFT/SUBMITTED→CANCELLED), row-delete-button
- **实际按钮**: toolbar: add-button, batch-delete-button；row: row-view-button, row-update-button, row-submit-button, row-approve-button, row-reject-button, row-cancel-button, row-delete-button。
- **差距**:
  - row-withdraw-approval-button: missing (minor) — 状态机未显式定义撤回（SUBMITTED→DRAFT），但 METHODOLOGY §1.2 列为标准 WF 按钮。缺失影响用户在提交后可撤回修改的能力。
- **判定**: minor — 撤回缺失是标准 WF 功能缺口，但核心审批流（提交→批准/驳回→取消）完整。

### ErpHrPayrollBankFile — CRUD
- **期望按钮**: CRUD 基线（银行代发文件管理实体）
- **实际按钮**: 完全一致
- **差距**: 无
- **判定**: clean

### ErpHrPosition — CRUD
- **期望按钮**: CRUD 基线
- **实际按钮**: 完全一致
- **差距**: 无
- **判定**: clean

### ErpHrRecruitment — CRUD+Custom
- **期望按钮**:
  - CRUD 基线 + 域专用按钮（ui-patterns §6, README 状态机）
  - `[筛选]` → row-screening-button (OPEN→SCREENING)
  - `[面试安排]` → row-interview-button (SCREENING→INTERVIEW)
  - `[发 Offer]` → row-offer-button (INTERVIEW→OFFERED)
  - `[入职]` → row-hire-button (OFFERED→HIRED)
  - `[拒绝]` → row-reject-button (任意阶段→REJECTED)
- **实际按钮**: toolbar: add-button, batch-delete-button；row: row-view-button, row-update-button, row-screening-button, row-interview-button, row-offer-button, row-hire-button, row-reject-button, row-delete-button。存在 scheduleInterview/makeOffer/hire 三个自定义表单页面。
- **差距**: 无
- **判定**: clean — 全部域专用按钮已实现，覆盖招聘管道全流程。

### ErpHrRoleCompetency — CRUD
- **期望按钮**: CRUD 基线
- **实际按钮**: 完全一致
- **差距**: 无
- **判定**: clean

### ErpHrSalary — CRUD+WF
- **期望按钮**:
  - CRUD 基线
  - WF 行按钮（approveStatus 状态机 UNSUBMITTED→SUBMITTED→APPROVED→REJECTED）：row-submit-button, row-withdraw-approval-button, row-approve-button, row-reject-button, row-reverse-approve-button
  - payment 按钮（ui-patterns §7.2）：`[生成银行文件]`（经理审批后）, `[发放]`（经理审批后）
  - row-cancel-button（paymentStatus 含 VOID）
- **实际按钮**: toolbar: add-button, batch-delete-button；row: row-view-button, row-update-button, row-submit-button, row-withdraw-approval-button, row-approve-button, row-reject-button, row-reverse-approve-button, row-delete-button。全部 WF 按钮带 visibleOn 条件控制。
- **差距**:
  - `[生成银行文件]` 按钮: missing (minor) — ui-patterns §7.2 经理审批通过后应有生成银行代发文件操作。
  - `[发放]` 按钮: missing (minor) — ui-patterns §7.2 银行文件生成后可触发发放操作。
  - row-cancel-button: missing (info) — paymentStatus 有 VOID 态但无显式取消/作废按钮。可能通过状态机自动控制。
- **判定**: minor — 核心审批按钮完备（包括反审核），但薪酬发放流程（银行文件/发放）尚未接入 UI。

### ErpHrSalaryItem — CRUD
- **期望按钮**: CRUD 基线（薪酬明细子实体）
- **实际按钮**: 完全一致
- **差距**: 无
- **判定**: clean

### ErpHrSalarySimulation — CRUD
- **期望按钮**: CRUD 基线（薪酬模拟实体）
- **实际按钮**: 完全一致
- **差距**: 无
- **判定**: clean

### ErpHrSalarySimulationItemAdjustment — CRUD
- **期望按钮**: CRUD 基线（模拟调整子实体）
- **实际按钮**: 完全一致
- **差距**: 无
- **判定**: clean

### ErpHrShift — CRUD
- **期望按钮**: CRUD 基线（班次定义实体）
- **实际按钮**: 完全一致
- **差距**: 无
- **判定**: clean

### ErpHrShiftAssignment — CRUD
- **期望按钮**: CRUD 基线（排班分配实体）
- **实际按钮**: 完全一致
- **差距**: 无
- **判定**: clean

### ErpHrShiftRotationPattern — CRUD
- **期望按钮**: CRUD 基线（轮班模式实体）
- **实际按钮**: 完全一致
- **差距**: 无
- **判定**: clean

### ErpHrShiftSwapRequest — CRUD
- **期望按钮**: CRUD 基线（换班申请实体）。当前无审批流状态机定义，CRUD 基线合理。
- **实际按钮**: 完全一致
- **差距**: 无
- **判定**: clean

### ErpHrSocialInsuranceBase — CRUD
- **期望按钮**: CRUD 基线
- **实际按钮**: 完全一致
- **差距**: 无
- **判定**: clean

### ErpHrSocialInsuranceConfig — CRUD
- **期望按钮**: CRUD 基线（配置类实体）
- **实际按钮**: 完全一致
- **差距**: 无
- **判定**: clean

### ErpHrSurvey — CRUD
- **期望按钮**: CRUD 基线
- **实际按钮**: 完全一致
- **差距**: 无
- **判定**: clean

### ErpHrSurveyAnswer — CRUD
- **期望按钮**: CRUD 基线
- **实际按钮**: 完全一致
- **差距**: 无
- **判定**: clean

### ErpHrSurveyQuestion — CRUD
- **期望按钮**: CRUD 基线
- **实际按钮**: 完全一致
- **差距**: 无
- **判定**: clean

### ErpHrSurveyResponse — CRUD
- **期望按钮**: CRUD 基线
- **实际按钮**: 完全一致
- **差距**: 无
- **判定**: clean

### ErpHrSurveyResult — CRUD
- **期望按钮**: CRUD 基线
- **实际按钮**: 完全一致
- **差距**: 无
- **判定**: clean

### ErpHrTaxConfig — CRUD
- **期望按钮**: CRUD 基线（配置类实体）
- **实际按钮**: 完全一致
- **差距**: 无
- **判定**: clean

### ErpHrTaxSpecialDeduction — CRUD
- **期望按钮**: CRUD 基线
- **实际按钮**: 完全一致
- **差距**: 无
- **判定**: clean

### ErpHrTimesheet — CRUD+WF（实际只有 CRUD）
- **期望按钮**:
  - toolbar: add-button, batch-delete-button (CRUD 基线)
  - row: row-view-button, row-update-button, row-submit-button (`[提交审批]`, ui-patterns §5.1, 状态 DRAFT→SUBMITTED), row-withdraw-approval-button (SUBMITTED→DRAFT, 标准 WF), row-approve-button (SUBMITTED→APPROVED), row-reject-button (SUBMITTED→REJECTED), row-cancel-button (DRAFT/SUBMITTED→CANCELLED, domain-design-guidelines §16.2), row-delete-button
- **实际按钮**: toolbar: add-button, batch-delete-button；rowActions 含 row-view-button 和 row-more-button（内嵌 row-update-button, row-delete-button）。**无任何 WF 按钮。**
- **差距**:
  - row-submit-button: missing (blocker) — ui-patterns §5.1 显式标注 `[提交审批]`，工时表状态机有 DRAFT→SUBMITTED。完全缺失。
  - row-approve-button: missing (blocker) — 工时表状态机有 SUBMITTED→APPROVED。完全缺失。
  - row-reject-button: missing (blocker) — 工时表状态机有 SUBMITTED→REJECTED。完全缺失。
  - row-withdraw-approval-button: missing (minor) — 标准 WF 撤回按钮。
  - row-cancel-button: missing (minor) — domain-design-guidelines §16.2 明确 CANCELLED 态。
- **判定**: **blocker** — 工时表为 HR 域核心业务实体，有明确审批状态机（DRAFT/SUBMITTED/APPROVED/REJECTED）和 ui-patterns 按钮要求（`[提交审批]`），但 view.xml 仅生成 CRUD 按钮，无任何 WF 操作入口。

### ErpHrTimesheetLine — CRUD
- **期望按钮**: CRUD 基线（工时表行子实体，不独立走审批）
- **实际按钮**: 完全一致
- **差距**: 无
- **判定**: clean

## 摘要

| 分类 | 实体 | 差距数 | 最高严重级 | 备注 |
|------|------|--------|-----------|------|
| CRUD | ErpHrAssessmentDetail | 0 | clean | |
| Custom | ErpHrAttendance | 2 | minor | toolbar 替换为 clock-in/out，缺 add/batch-delete |
| CRUD | ErpHrCompetency | 0 | clean | |
| CRUD | ErpHrCompetencyLevel | 0 | clean | |
| CRUD | ErpHrDepartment | 0 | clean | |
| CRUD | ErpHrDevelopmentPlan | 0 | clean | |
| CRUD | ErpHrDevelopmentPlanItem | 0 | clean | |
| CRUD+Custom | ErpHrEmployee | 5 | minor | 缺批量调动/离职 toolbar、条件按钮离职/转正 |
| CRUD | ErpHrEmployeeAssessment | 0 | clean | |
| CRUD | ErpHrEmploymentContract | 0 | clean | |
| CRUD | ErpHrGapAnalysis | 0 | clean | |
| CRUD | ErpHrLeaveBalance | 0 | clean | |
| CRUD+WF | ErpHrLeaveRequest | 1 | minor | 缺撤回按钮 |
| CRUD | ErpHrPayrollBankFile | 0 | clean | |
| CRUD | ErpHrPosition | 0 | clean | |
| CRUD+Custom | ErpHrRecruitment | 0 | clean | 招聘管道全流程按钮完备 |
| CRUD | ErpHrRoleCompetency | 0 | clean | |
| CRUD+WF | ErpHrSalary | 3 | minor | 缺生成银行文件/发放按钮、取消按钮 |
| CRUD | ErpHrSalaryItem | 0 | clean | |
| CRUD | ErpHrSalarySimulation | 0 | clean | |
| CRUD | ErpHrSalarySimulationItemAdjustment | 0 | clean | |
| CRUD | ErpHrShift | 0 | clean | |
| CRUD | ErpHrShiftAssignment | 0 | clean | |
| CRUD | ErpHrShiftRotationPattern | 0 | clean | |
| CRUD | ErpHrShiftSwapRequest | 0 | clean | |
| CRUD | ErpHrSocialInsuranceBase | 0 | clean | |
| CRUD | ErpHrSocialInsuranceConfig | 0 | clean | |
| CRUD | ErpHrSurvey | 0 | clean | |
| CRUD | ErpHrSurveyAnswer | 0 | clean | |
| CRUD | ErpHrSurveyQuestion | 0 | clean | |
| CRUD | ErpHrSurveyResponse | 0 | clean | |
| CRUD | ErpHrSurveyResult | 0 | clean | |
| CRUD | ErpHrTaxConfig | 0 | clean | |
| CRUD | ErpHrTaxSpecialDeduction | 0 | clean | |
| CRUD+WF (但实际 CRUD) | **ErpHrTimesheet** | **5** | **blocker** | **核心工时表无任何提交/审批按钮** |
| CRUD | ErpHrTimesheetLine | 0 | clean | |

### 总评
- 总实体数：36
- 无差距实体：28（77.8%）
- Blocker 差距：**1**（ErpHrTimesheet — 工时表完全缺失审批按钮）
- Major 差距：0
- Minor 差距：5（Attendance、Employee、LeaveRequest、Salary、Timesheet 含 minor 级子差距）
- Info 差距：0（Employee 的导出/更多按钮未算入正式差距，Methodology §2 规定 [导出] 无标准 ID 且不视为正式期望）

### 关键发现

1. **ErpHrTimesheet blocker**：工时表有明确状态机（DRAFT/SUBMITTED/APPROVED/REJECTED）和 ui-patterns 按钮要求（§5.1 `[提交审批]`），但 view.xml 仅含 CRUD 按钮，**完全缺失 submit/approve/reject 三个核心 WF 按钮**。这是 HR 域最严重的按钮缺口。

2. **ErpHrEmployee 条件按钮缺口**：ui-patterns §1.4 明确定义了按 employmentStatus 条件显示的 [办理离职][转正][部门调动] 按钮，当前实现了 [部门调动]（row-transfer-button），但 [办理离职] 和 [转正] 条件按钮未实现。

3. **ErpHrSalary 发放流程按钮缺口**：审批流按钮完备（提交→撤回→批准→驳回→反审批），但与支付相关的 [生成银行文件][发放] 按钮（ui-patterns §7.2）未实现。

4. **template 生成遗憾**：28 个 CRUD 实体全部由 codegen 生成标准 view.xml，按钮一致性好。4 个非 CRUD 实体中 ErpHrRecruitment 实现最佳（全流程自定义按钮完备），ErpHrTimesheet 缺口最大。

5. **后端可能已有 BizModel 方法**：Timesheet 的 submitForApproval/approve/reject/reject 等方法可能在 BizModel 中存在但 view.xml 未接线。建议确认 `ErpHrTimesheetBizModel` 实现状态后再决定是新增 view.xml 按钮还是在 BizModel 层补齐。
