# MA4 crm+hr view.xml vs 后端契约 drift 审计（A4.8）

> Audit Status: closed
> 里程碑: MA4（代码与前端质量层）
> 工作项: A4.8（crm+hr view.xml vs 后端契约 drift，crm A 级 + hr S 级 view.xml drift 第三批）
> 范围文档: `docs/audits/audit-remediation-scope-and-dimension-matrix.md` §2.4「view.xml drift（MA4）」行 + §残留风险 5「未覆盖：AMIS view.xml drift」
> Skill: `docs/skills/multi-dimensional-audit-prompt.md`（7 维度适配「view.xml vs 后端契约 drift」主题，经 A4.6/A4.7 验证有效）
> 来源计划: `docs/plans/2026-07-29-0749-2-audit-remediation-ma4-crm-hr-view-xml-drift.md`
> 后端契约真相源基线: A4.4（hr）+ A4.5（crm）+ MA2（hr/crm 状态机）已 done——后端 BizModel/xbiz 契约稳定
> 同型前批: A4.6（finance+mfg S 级第一批，134 view.xml，已 done）+ A4.7（pur+sal+inv A 级第二批，114 view.xml，已 done）

## 1. 审计对象与基线

- **审计对象**：crm 68 view.xml + hr 72 view.xml = **合计 140 view.xml**（`module-{crm,hr}/erp-*-web/src/main/resources/_vfs/erp/{crm,hr}/pages/`）。其中 delta（非 `_gen`）定制层 crm 34 + hr 36 = **70 套**，`_gen` 生成层各对应一套。delta 层是手写 drift 的发源地；`_gen` 层由 XMeta 驱动生成，理论自洽，本审计以 delta 层为主、`_gen` 层为辅交叉对照（与 A4.6/A4.7 同型方法）。
- **后端真相源**：`module-{crm,hr}/erp-*-service/`（BizModel Java + `*.xbiz`/`_*.xbiz` + Processor）+ `*-meta/`（XMeta + dict yaml）+ `module-{crm,hr}/model/app-erp-*.orm.xml`（字段 + `ext:dict` 绑定）+ 共享 `module-common-service/_vfs/dict/erp/doc-status.dict.yaml`（DRAFT/**ACTIVE**/CANCELLED）+ 平台 `wf/approve-status.dict.yaml`（UNSUBMITTED/SUBMITTED/APPROVED/REJECTED）。
- **drift 维度**（`multi-dimensional-audit-prompt.md` 7 维度适配本主题，经 A4.6/A4.7 验证有效）：(1) 字段名一致性 / (2) BizMutation 动作名一致性 / (3) 枚举值状态值一致性 / (4) 参数类型一致性 / (5) dict 绑定一致性 / (6) gen-control 内联脚本契约 / (7) 跨实体字段引用。

## 2. 7 维度逐项审查结果

### 维度 1 — 字段名一致性

**裁决：本维度无 drift（两域 delta 层 col/cell id 全量核验命中 ORM 实体字段或 to-one/to-many 关联；`bounded-merge` 静默丢弃机制兜底）。**

- 两域 delta grid `<cols>` / form `<cells>` 普遍声明 `x:override="bounded-merge"`。Nop `bounded-merge` 语义：仅合并基础层（XMeta）已声明的 prop，未知 `id` 在运行时被静默丢弃而非报错——字段名拼写漂移被平台机制自愈（dropped，最坏退化为列缺失可见性，不致页面报错/空白）。
- **crm**：逐实体核验 delta col/cell id 全部命中 `app-erp-crm.orm.xml`——ErpCrmLead（`partnerId/leadType/leadStatusId/stageId/territoryId/lostReasonId/docStatus/expectedCloseDate/expectedRevenue/probability`）、ErpCrmEvent（`eventCategory/relatedLead/parentEvent/status`）、ErpCrmForecast（`period`）、ErpCrmForecastAccuracy（`period/commitAccuracy/upsideAccuracy`）、ErpCrmForecastLine（`lead`）、ErpCrmPriceRule（`product/customer/discountPercent`）、ErpCrmBundlePricingLine（`product`）、ErpCrmTerritoryAssignmentRule（`team`）等关系字段全部命中 `<relations>` refEntityName；普通列全部命中 `<column>`。**零悬挂字段**。
- **hr**：逐实体核验 delta col/cell id 全部命中 `app-erp-hr.orm.xml`——ErpHrEmployee（14 主网格列 `id/code/fullName/orgId/departmentId/positionId/jobTitle/costCenterId/bankAccountId/employmentStatus/employeeType/hireDate/userAccountId` + employee/department/position/superior/costCenter/bankAccount/org to-one 关系全部命中 orm:310-315）、ErpHrSalary（`employee/bankFileId/bankFile`）、ErpHrSalarySimulation、ErpHrEmploymentContract、ErpHrRecruitment、ErpHrEmployeeAssessment（`details` to-many）、ErpHrDevelopmentPlan（`items` to-many）、ErpHrSurvey（`questions/responses` to-many）、ErpHrTimesheet（`lines` to-many）、ErpHrCompetency（`levels` to-many）、ErpHrLeaveRequest、ErpHrShiftAssignment、ErpHrShiftSwapRequest、ErpHrAttendance、ErpHrPayrollBankFile 等。调岗表单 `ErpHrEmployee.view.xml:255-299` 的非实体参数 cell 均显式标记 `custom="true"`（Nop 约定的合法绕过，非 drift）。**零悬挂字段**。
- **P1-MA1-009 复核（crm DECIMAL↔double 7 列）**：5/7 列在 view 暴露（ForecastAccuracy.commitAccuracy/upsideAccuracy、PriceRule.discountPercent、FunnelStageMetrics.conversionRate/avgDaysInStage 均以 `ui:number="true"` 渲染，**未**按精确十进制掩码——MA1-009 关注点仍属后端类型）；2/7 列（LeadFunnel.avgSalesCycleDays、FunnelStageMetrics.dropOffRate）在 view **未暴露**（分析指标不可见，非 drift）。属后端 MA1 范畴，**无 view 层 drift**。

### 维度 2 — BizMutation/BizQuery 动作名一致性

**裁决：本维度无 drift（两域 delta 层全部自定义动作引用的**动作名**解析到 BizModel @BizMutation/@BizQuery 或 `*.xbiz` `<mutation>`；零悬挂动作引用）。**

逐一核验两域 delta 层全部自定义动作引用（CRUD 标准 `save/update/delete/batchDelete/findPage/get` 由 `_*.xbiz` `DefaultBizGenExtends` 自动派生，不逐项列出）：

**crm 自定义动作**（6 个自定义动作全部解析通过）：
- `ErpCrmLead__qualify/convertToCustomer/lose/cancel?leadId=$id` → `ErpCrmLeadBizModel.java:61/148/67/78` `@Name("leadId")` ✓
- `ErpCrmEvent__complete/cancel?eventId=$id` → `ErpCrmEventBizModel.java:56/69` `@Name("eventId")` ✓
- **观察（非 drift）**：crm 全域 `*.xbiz` delta 文件均为空 `<actions/>`（`ErpCrmLead.xbiz:4` 等），全部自定义动作经 BizModel Java 注解生成。部分 BizMutation 方法（`refreshForecast`/`assignSequence`/`recalculateScore`/`generateQuote`/territory/quota mutations）**未在 view 暴露按钮**——属「后端实现、view 未接线」的产品功能缺口（feature-gap，非悬挂动作引用），与本审计 MA2 §crm 范畴一致。

**hr 自定义动作**（动作名全部解析通过）：
- `ErpHrAttendance__clockIn/clockOut?employeeId=$employeeId` → `ErpHrAttendanceBizModel:52/76` `@Name("employeeId")` ✓
- `ErpHrLeaveRequest__submit/approve/reject/cancel?id=$id` → `ErpHrLeaveRequestBizModel:73/86/101/111` `@Name("id") String` ✓
- `ErpHrRecruitment__moveToScreening/scheduleInterview/makeOffer/hire/reject?id=$id`（+ form 参数）→ `ErpHrRecruitmentBizModel:66/76/91/104/122` ✓
- `ErpHrSalary__submitForApproval/withdrawApproval/approve/reject/reverseApprove?id=$id` → `ErpHrSalary.xbiz` `<mutation>` 声明（xbiz:14/51/76/101/126）`<arg name="id" type="String"/>` ✓
- `ErpHrTimesheet__submit?timesheetId=$id` → `ErpHrTimesheetBizModel:35` `@Name("timesheetId") Long` ✓（参数名正确为 `timesheetId` 非 `id`）
- `ErpHrEmployee__transferEmployee`（withFormData 6 参数）→ `ErpHrEmployeeBizModel:88-94` `@Name` 全 6/6 匹配 ✓
- `ErpHrEmployee__countReferences`（raw GraphQL）→ `ErpHrEmployeeBizModel:126` `@BizQuery countReferences(@Name("id"))` ✓
- **观察（非 drift）**：`ErpHrSalary__markPaid/generateBankFile`、`ErpHrSalarySimulation__{submitForReview/approve/reject/convertToFormal/...}`、`ErpHrEmploymentContract__{renew/expireOverdueContracts}`、`ErpHrEmployeeAssessment__{submitAssessment/completeAssessment}`、`ErpHrDevelopmentPlan__{generateDevelopmentPlan/completePlan}`、`ErpHrShift*__{calcAttendance/assignSingle/...}`、`ErpHrShiftSwapRequest__{submit/approve/reject/cancel}` 等 BizMutation **未在 delta view 暴露按钮**——同 crm，属 feature-gap（未接线功能）非悬挂动作引用。

- **A4.5/MA3 已登记 P1 复核**：本维度与 P1-MA3-048（孤儿 Processor bean 携带 String 影子契约 dim3）交叉——hr 域 **无任何 Processor 类**（A2.7b 已确认），crm 动作全部经 BizModel 注解生成，无孤儿悬挂动作引用。

### 维度 3 — 枚举值/状态值一致性

**裁决：本维度无 P0/P1 drift（两域 delta 层全部 `visibleOn`/`disabledOn` 状态字面量命中有效 dict 值；MA2 hr 十项死状态 view 层投影复核全部「无 view 层 drift」）。两域 gen-control badge 采用跨域通用调色板，与各自 dict 词汇表存在系统性偏差（纯视觉/可维护性 → P2-MA4-019/P2-MA4-020，归维度 6 同根因）。**

逐实体核验两域 delta view 的 `visibleOn`/`disabledOn`/gen-control badge 状态字面量 vs dict yaml + Constants（完整映射表见 §8 状态值映射表）：

**visibleOn/disabledOn 字面量（零 drift）**：
- crm：仅 `ErpCrmEvent.view.xml:128,136` 两处 `visibleOn: status == 'PLANNED'`——`PLANNED` ∈ `erp-crm/event-status`（PLANNED/COMPLETED/CANCELLED）✓。
- hr：`ErpHrTimesheet.view.xml:118` `status == 'DRAFT'` ✓（DRAFT ∈ timesheet-status）；`ErpHrSalary.view.xml:137/145/153/161/169` `approveStatus == UNSUBMITTED/REJECTED/SUBMITTED/APPROVED` 全命中 `wf/approve-status` ✓；`ErpHrEmployee.view.xml:224` `${false}`（taxFileNo 常量隐藏）✓。

**MA2 hr 十项死状态 view 层投影复核**（plan 最高优先级项）：
- **P1-MA2-039**（员工 RESIGNED/TERMINATED/RETIRED + 转正迁移未实现）：`ErpHrEmployee.view.xml` rowActions（:304-362）**无** resign/terminate/retire/probationToRegular 任何按钮；`employmentStatus` col（:23）为**普通列**（无 gen-control badge、无状态守卫）。死状态经 view **仅作只读 dict 显示**，不映射为可见动作按钮。**无 view 层 drift**（根因在后端 P1-MA2-039）。
- **P1-MA2-040**（合同 SUSPENDED 死状态）：`ErpHrEmploymentContract.view.xml` 无 suspend 按钮；status col 用通用调色板（SUSPENDED 恰好命中 warningVals→warning 色 ✓，但这是巧合非设计）。**无 view 层 drift**。
- **P1-MA2-041**（调查 OPEN/CLOSED/ARCHIVED 死状态 + 桩 BizModel）：`ErpHrSurvey.view.xml` rowActions（:137-155）**仅** view/update/2 drawer，**无** publish/close/archive 按钮暴露死状态迁移。**无 view 层 drift**。
- **P1-MA2-042**（发展计划 DRAFT/CANCELLED + 计划项 OVERDUE 死状态）：`ErpHrDevelopmentPlan.view.xml` 无 cancel 按钮；计划项 ErpHrDevelopmentPlanItem status col **无 gen-control**（普通渲染）。**无 view 层 drift**。
- **P1-MA2-043**（工时单 APPROVED/REJECTED 死状态）：`ErpHrTimesheet.view.xml`（:112-119）**仅** submit 按钮（`visibleOn: status=='DRAFT'` 守卫），**无** approve/reject 按钮暴露死状态迁移。**无 view 层 drift**。
- **P1-MA2-044**（工时单 BizModel 硬编码字符串）：纯后端代码质量，view 不暴露硬编码。**无 view 层投影**。
- **P1-MA2-045**（银行文件 UPLOADED/CONFIRMED 死状态 + 桩 BizModel）：`ErpHrPayrollBankFile.view.xml`（:74-77）`<crud name="main"/>` **空壳**——无 upload/confirm 按钮暴露死状态迁移。**无 view 层 drift**（badge 全灰见 P2-MA4-019）。
- **P1-MA2-046**（排班分配 status 无 dict 绑定 raw VARCHAR）：`ErpHrShiftAssignment.view.xml` status col（:21）为**普通列**（无 dict 绑定、无 gen-control），**忠实匹配** ORM `app-erp-hr.orm.xml:1186` 无 `ext:dict` 的状态——view 与 ORM 一致，**无 view 层 drift**（根因在 ORM/后端 P1-MA2-046）。
- **P1-MA2-047**（Salary posted 死字段）：`ErpHrSalary.view.xml` 未将 posted 暴露为可操作字段。**无 view 层投影**。
- **P1-MA2-048**（SalaryPostingDispatcher 吞异常悬挂）：纯后端过账链路，view 不投影。**无 view 层投影**。

**crm P1-MA2-076 复核（Event reminderMinutesBefore 死字段）**：
- `ErpCrmEvent.view.xml:68`（view 表单）+ `:92`（edit 表单）**均暴露** `reminderMinutesBefore` 字段为**用户可编辑**（`reminderMinutesBefore[提前提醒(分钟)]`）。view 忠实绑定 ORM `app-erp-crm.orm.xml:472` `reminderMinutesBefore` INTEGER 列。**死字段根因在后端**（`ErpCrmEventBizModel.findDueReminders` 用全局 60 分钟窗口从不读 per-event reminderMinutesBefore）——view 正确绑定 ORM 字段，用户可设置但后端忽略。**无 view 层 drift**（view-correct / backend-dead-field，不放大死字段语义）。

### 维度 4 — 参数类型一致性

**裁决：本维度无 drift（两域 delta 层全部自定义动作传参名/类型匹配 BizModel `@Name` 或 xbiz `<arg>`）。**

- **crm**：`ErpCrmLead__qualify/convertToCustomer/cancel?leadId=$id` + `__lose?leadId=$id&lostReasonId=0&lostReasonDesc=` 全部匹配 `ErpCrmLeadBizModel.java:62/149/68-72/79` `@Name`（lostReasonId/lostReasonDesc `@Optional`，`lostReasonId=0` 与 `Long` 类型兼容）；`ErpCrmEvent__complete/cancel?eventId=$id` 匹配 `ErpCrmEventBizModel.java:57/70` `@Name("eventId")` ✓。
- **hr**：见维度 2 表——clockIn/clockOut（employeeId）、LeaveRequest 四动作（id String）、Recruitment 五动作（id + interviewerId/interviewDate/offerSalary/hiredDate form 参数）、Salary 审批五动作（id String via xbiz）、Timesheet submit（**timesheetId** Long 非 id——正确）、transferEmployee（6 参数全匹配）。**零参数名漂移**。
- **P1-MA3-047 复核（API 命名/参数跨域不一致 dim7）**：本维度与 MA3-047 交叉——hr/crm 全部自定义动作参数名与 `@Name` 一致，**无 view 层 drift**（区别于 A4.7 pur ErpPurRfq cancel `?id` vs `@Name("rfqId")` 投影为 P1-MA4-024——crm/hr 无此型）。
- **日期参数**：frontend-ui-roadmap 已修复 12 日期参数报表下载 + input-date valueFormat。本审计复核 crm/hr 业务页面 `businessDate filterOp="date-between"` 全域统一，AMIS 标准序列化，无残留裸字符串日期漂移 ✓。

### 维度 5 — dict 绑定一致性

**裁决：本维度无 drift（两域 ORM `ext:dict` 引用的 dict yaml 全部存在）。**

- **crm**：全量核验 ORM 23 处 `ext:dict` 绑定全部 `erp-crm/*`（lead-type/lead-doc-status/event-type/event-status/event-priority/activity-type/scoring-method/scoring-triggered-action/scoring-trigger-event/forecast-period-type/forecast-period-status/forecast-category/territory-type/assignment-condition-type/assignment-method/quota-period-type/config-rule-type/bundle-discount-type/price-rule-type/sequence-template-type/step-completion-condition/seq-assignment-condition-type/sequence-progress-status）→ 对应 `_vfs/dict/erp-crm/*.dict.yaml` **全部存在** ✓。crm 仅引用自身 `erp-crm/*` dict，**零** `erp/doc-status` 或 `wf/approve-status` 引用（grep 确认）。delta view 未硬编码 `dict=` 路径。
- **hr**：全量核验 ORM 38 处 `ext:dict` 绑定——37 处 `erp-hr/*`（gender/marital-status/employment-status/employee-type/contract-type/contract-status/leave-type/leave-status/timesheet-status/attendance-source/salary-payment-status/recruitment-source/recruitment-status/simulation-status/adjustment-reason/salary-item-category/salary-item-group/calc-method/city/social-insurance-type/special-deduction-type/bank-file-format/bank-file-status/shift-type/absence-reason/swap-status/survey-type/survey-status/question-type/driver-category/competency-category/assessment-type/assessment-status/gap-severity/devplan-status/plan-item-status）→ 36 dict yaml 文件**全部存在** ✓；1 处 `wf/approve-status`（ErpHrSalary.approveStatus orm:736）→ 平台 `nop-wf-meta/_vfs/dict/wf/approve-status.dict.yaml` 存在 ✓。
- **P1-MA2-046 复核**（hr 排班分配 status 无 dict 绑定）：仅 ErpHrShiftAssignment.status 一处 ORM 无 `ext:dict`（设计漂移根因在后端，view 忠实匹配），**crm/hr 其余 status 字段全部声明 `ext:dict`**。

### 维度 6 — gen-control 内联脚本契约

**裁决：本维度发现 1 项 P1（hr Employee PII 掩码使用非法 `LEFT()`/`RIGHT()` 函数）+ 2 项 P2（两域跨域通用调色板系统性偏差）。**

- **P1-MA4-025 hr Employee PII 掩码非法函数**：`ErpHrEmployee.view.xml` 4 处 gen-control `<c:script>` 内 `tpl` 字符串使用 `${LEFT(field, n)}` / `${RIGHT(field, n)}`：
  - `:19` `tpl: '****${RIGHT(bankAccountId, 4)}'`（工资卡账户掩码）
  - `:150` `tpl: '${LEFT(idCardNo, 1)}******${RIGHT(idCardNo, 4)}'`（身份证号掩码）
  - `:157` `tpl: '${LEFT(mobilePhone, 3)}****${RIGHT(mobilePhone, 4)}'`（手机号掩码）
  - `:164` `tpl: '****${RIGHT(bankAccountId, 4)}'`（银行账户掩码）
  
  AMIS/Nop `${...}` tpl 表达式按 **JavaScript** 语义求值——`LEFT`/`RIGHT` 是 SQL/Excel 函数，**非 JS 内置**。全仓 grep（排除 `_gen`/`target`）确认**仅此一处 hr view 使用此语法**，其他所有模块掩码均用 JS `String(x).slice(...)`（如 `ErpFinVoucher.view.xml:151` 用 `Array.isArray(...).slice()`）。后果：4 处 PII 掩码不按预期渲染（渲染为字面 `****${RIGHT(bankAccountId, 4)}` 文本或求值失败），**银行账号/身份证号/手机号掩码功能性失效**——属敏感数据展示缺陷。**P1 major，目标 MR2（view.xml 代码类）**。

- **P2-MA4-019 hr 跨域通用状态调色板系统性偏差**：hr 10 套 delta view 复制粘贴同一跨域通用调色板（successVals/dangerVals/warningVals/primaryVals 硬编码 ~40 值数组）：`ErpHrSalarySimulation`（:21-24）/ `ErpHrEmploymentContract`（:22-25）/ `ErpHrRecruitment`（:20-23）/ `ErpHrLeaveRequest`（:20-23）/ `ErpHrTimesheet`（:19-22）/ `ErpHrSurvey`（:18-21）/ `ErpHrPayrollBankFile`（:20-23）/ `ErpHrShiftSwapRequest`（:23-26）/ `ErpHrDevelopmentPlan`（:18-21）/ `ErpHrEmployeeAssessment`（:19-22）。该调色板源自非 hr 模块（含 `MATERIAL_TRANSFERRED`/`HONORED`/`DISCOUNTED`/`ENDORSED` 等 hr 不存在值），且**漏掉 hr 专属状态值**，致纯视觉灰（default）渲染：
  - SalarySimulation `IN_REVIEW`（dict 值）vs 调色板 `IN_PROGRESS`（:24）→ IN_REVIEW 渲染灰（应 primary）；DRAFT/CONVERTED 亦灰。
  - PayrollBankFile `GENERATED/UPLOADED/CONFIRMED` 全不在调色板 → **整列状态恒灰**。
  - Recruitment `SCREENING/INTERVIEW/OFFERED/HIRED` 不在调色板 → 全灰（仅 OPEN/REJECTED/CLOSED 有色）。
  - Survey `CLOSED` 命中 successVals（语义错——CLOSED 非成功态）；DRAFT/ARCHIVED 灰。
  - ShiftSwapRequest `PENDING` 不在调色板 → 灰（应 primary）。
  
  同 A4.6 P2-MA4-014/015 + A4.7 P2-MA4-016/017 同型根因（跨域调色板复制粘贴未适配域 dict 词汇表）。对比：`ErpHrSalary.view.xml:14-29` 的 approveStatus 专用调色板（内联三元 `APPROVED/REJECTED/SUBMITTED`）正确，因已收窄到 wf/approve-status dict。**P2 watch-only，目标 MR2**。

- **P2-MA4-020 crm 共享 dict 调色板错配**：
  - `ErpCrmLead.view.xml:23-36` docStatus badge 调色板仅分支 `== 'ACTIVE'`（→primary）+ `== 'CANCELLED'`（→line-through），但 Lead.docStatus 绑定 `erp-crm/lead-doc-status` = {NEW, QUALIFIED, CONVERTED, LOST, CANCELLED}，**无 ACTIVE 值**（ACTIVE 属共享 `erp/doc-status`）→ NEW/QUALIFIED/CONVERTED/LOST 全渲染灰，仅 CANCELLED 删除线有效。该 badge 是为共享 `erp/doc-status` 调优的通用控件误贴到域专属状态列。
  - `ErpCrmEvent.view.xml:21-41` status 通用调色板 vs `erp-crm/event-status` = {PLANNED, COMPLETED, CANCELLED}：COMPLETED→success ✓ / CANCELLED→danger ✓ / **PLANNED→灰**（应 primary）。
  - `ErpCrmForecastPeriod.view.xml:17-37` status 通用调色板 vs `erp-crm/forecast-period-status` = {OPEN, CLOSED, FROZEN}：OPEN→primary ✓ / CLOSED→success ✓ / **FROZEN→灰**。
  
  纯视觉/可维护性（label 经 dict `graphql:labelProp` 正确显示，仅颜色类错）。同 P2-MA4-019 根因。**P2 watch-only，目标 MR2**。

- **前端 UI-roadmap Phase 3 残留复核**：两域 delta view.xml **零** `ErpMdPartner__`/`ErpMd*__` 非法 GraphQL 引用（grep 确认）；全部 `<data>` 出现为合法 AMIS drawer-data XML；无裸 JS `data` 变量滥用；`${"query{ErpHrEmployee__countReferences(...){k v}}"}`（`ErpHrEmployee.view.xml:338`）为合法 Nop Map 投影语法。**两域 view.xml 无 Phase 3 残留**。

### 维度 7 — 跨实体字段引用

**裁决：本维度无 drift（两域跨实体字段路径全部命中关联实体字段、经 picker 快照注入合法、子表 view/drawer page 路径全部存在）。**

- **crm**：`ErpCrmLead.view.xml:165/173/181` drawer page 引用 `/erp/crm/pages/ErpCrmEvent/ref-lead.page.yaml`、`ErpCrmActivity/ref-lead.page.yaml`、`ErpCrmLeadConvLog/ref-lead.page.yaml` **全部存在** ✓。跨域 to-one（`ErpCrmPriceRule.product`→ErpMdMaterial orm:1264 / `.customer`→ErpMdPartner orm:1267 / `ErpCrmForecast.currency`→ErpMdCurrency orm:846 / `ErpCrmQuota.currency`→ErpMdCurrency orm:1073）经 ORM notGenCode 外部实体声明（orm:1525-1577）合法 ✓。crm delta view 未引用 finance VoucherBillR drawer 或外部 master-data picker page URL（picker 经 `_gen` 标准 picker page 解析）✓。
- **hr**：delta view 引用的 picker/drawer page 路径（`ErpHrAssessmentDetail.view.xml:29`→ErpHrCompetency/picker.page.yaml / `ErpHrDevelopmentPlanItem.view.xml:31`→ErpCrmCompetency picker / `ErpHrTimesheetLine.view.xml:36`→`/erp/prj/pages/ErpPrjProject/picker.page.yaml` 跨域只读 / `ErpHrSurvey.view.xml:141/149`→ref-survey.page.yaml）**全部存在** ✓。子表 view 路径（Competency→CompetencyLevel、DevelopmentPlan→DevelopmentPlanItem、EmployeeAssessment→AssessmentDetail、Survey→SurveyQuestion/SurveyResponse、Timesheet→TimesheetLine）全存在 ✓。to-one refEntityName（employee/department/position/manager/costCenter/bankAccount/org/superior/reviewer/interviewer/approver/leaveRequest/shift/swapRequest/bank/mentor/parent/group/survey/assessment/competency/simulation/timesheet/question/response/plan/gap 等）全部命中 ORM `<relations>` ✓。
- **P1-MA1-022 复核**（跨域只读 daoFor 投影 / picker 快照注入字段）：两域跨域引用经 notGenCode 外部实体建立 EQL 点导航，picker 快照注入字段（id/name）均存在；两域无 finance VoucherLine 式 `isAuxiliaryX` visibleOn 悬挂。本批次无 ORM refEntityName 重命名历史（MA1 未报告两域此类），无悬挂 ✓。

## 3. P0-P3 finding 清单（按严重性排序）

> 起始编号 = A4.7 已分配最大（P1-MA4-024 / P2-MA4-018）+ 1 = **P1-MA4-025 / P2-MA4-019**。本审计零 P0（无活跃数据破坏路径——view.xml drift 最坏为掩码失效/颜色错误，无 GL/库存写入破坏）。

| Finding ID | 严重性 | 域 | view.xml 文件:行 | 后端对照 | 缺陷描述 | 影响 | 目标 MR |
|-----------|--------|----|-----------------|---------|---------|------|---------|
| **P1-MA4-025** | **P1 (major)** | hr | `module-hr/erp-hr-web/.../ErpHrEmployee/ErpHrEmployee.view.xml:19,150,157,164`（bankAccountId/idCardNo/mobilePhone/bankAccountId gen-control `<c:script>` tpl） | AMIS/Nop `${...}` tpl 表达式按 JavaScript 求值（无 `LEFT`/`RIGHT` 内置）；全仓仅此一处用此语法，其他模块用 JS `String(x).slice()` | 4 处 PII 掩码 `tpl` 使用 SQL/Excel 函数 `${LEFT(field,n)}`/`${RIGHT(field,n)}`——JS 求值失败/字面渲染，银行账号/身份证号/手机号掩码功能性失效 | **PII 掩码失效**（敏感数据展示缺陷）：掩码不按预期渲染（字面 `****${RIGHT(bankAccountId, 4)}` 或求值失败），工资卡/身份证/手机号掩码不可用 | **MR2**（view.xml 代码类） |
| P2-MA4-019 | P2 (minor) | hr | `ErpHrSalarySimulation.view.xml:21-24` + `ErpHrEmploymentContract:22-25` + `ErpHrRecruitment:20-23` + `ErpHrLeaveRequest:20-23` + `ErpHrTimesheet:19-22` + `ErpHrSurvey:18-21` + `ErpHrPayrollBankFile:20-23` + `ErpHrShiftSwapRequest:23-26` + `ErpHrDevelopmentPlan:18-21` + `ErpHrEmployeeAssessment:19-22`（10 套 status gen-control badge） | hr 各 dict 词汇表（simulation-status/contract-status/recruitment-status/.../bank-file-status/swap-status/devplan-status/assessment-status） | 10 套 delta view 复制粘贴同一跨域通用调色板（~40 硬编码值），含 hr 不存在值（MATERIAL_TRANSFERRED/HONORED/...）且漏 hr 专属值（IN_REVIEW/GENERATED/UPLOADED/CONFIRMED/SCREENING/.../PENDING）→ 系统性灰（default）渲染（BankFile 整列恒灰 / SalarySimulation IN_REVIEW 灰 / Survey CLOSED 错判 success） | 纯视觉/可维护性（label 经 dict 正确，仅颜色类错；dict 演进时调色板失同步）。watch-only。同 A4.6 P2-MA4-014/015 + A4.7 P2-MA4-016/017 根因 | MR2（view.xml 代码类，watch-only） |
| P2-MA4-020 | P2 (minor) | crm | `ErpCrmLead.view.xml:23-36`（docStatus badge）+ `ErpCrmEvent.view.xml:21-41`（status badge）+ `ErpCrmForecastPeriod.view.xml:17-37`（status badge） | crm 各 dict（lead-doc-status/event-status/forecast-period-status） | (a) Lead docStatus badge 为共享 `erp/doc-status`（含 ACTIVE）调优误贴到 `erp-crm/lead-doc-status`（无 ACTIVE）→ NEW/QUALIFIED/CONVERTED/LOST 全灰；(b) Event PLANNED 灰（应 primary）；(c) ForecastPeriod FROZEN 灰 | 纯视觉/可维护性（label 经 dict 正确，CANCELLED 删除线有效；非CANCELLED 线索态灰）。watch-only。同 P2-MA4-019 根因 | MR2（view.xml 代码类，watch-only） |

## 4. 已知 finding view 层投影复核汇总

| 来源 finding | view 层投影裁决 |
|-------------|----------------|
| P1-MA2-039（员工 RESIGNED/TERMINATED/RETIRED + 转正迁移未实现） | 无 view 层 drift（Employee view 无 resign/terminate/retire/probationToRegular 按钮；employmentStatus 为普通列无 badge/守卫。死状态仅作只读 dict 显示，不映射为可见动作按钮。根因在后端） |
| P1-MA2-040（合同 SUSPENDED 死状态） | 无 view 层 drift（Contract view 无 suspend 按钮；status col 通用调色板 SUSPENDED 恰命中 warningVals，巧合非设计。根因在后端） |
| P1-MA2-041（调查 OPEN/CLOSED/ARCHIVED 死状态 + 桩 BizModel） | 无 view 层 drift（Survey view rowActions 仅 view/update/2 drawer，无 publish/close/archive 按钮暴露死状态迁移） |
| P1-MA2-042（发展计划 DRAFT/CANCELLED + 计划项 OVERDUE 死状态） | 无 view 层 drift（DevelopmentPlan view 无 cancel 按钮；DevelopmentPlanItem status 无 gen-control。死状态不经 view 暴露为动作） |
| P1-MA2-043（工时单 APPROVED/REJECTED 死状态） | 无 view 层 drift（Timesheet view 仅 submit 按钮 visibleOn DRAFT，无 approve/reject 按钮暴露死状态迁移） |
| P1-MA2-044（工时单 BizModel 硬编码字符串） | 无 view 层投影（纯后端代码质量） |
| P1-MA2-045（银行文件 UPLOADED/CONFIRMED 死状态 + 桩 BizModel） | 无 view 层 drift（PayrollBankFile view `<crud name="main"/>` 空壳，无 upload/confirm 按钮。badge 全灰见 P2-MA4-019） |
| P1-MA2-046（排班分配 status 无 dict 绑定 raw VARCHAR） | 无 view 层 drift（ShiftAssignment view status 为普通列，忠实匹配 ORM 无 ext:dict。view 与 ORM 一致，根因在 ORM/后端） |
| P1-MA2-047（Salary posted 死字段） | 无 view 层投影（Salary view 未将 posted 暴露为可操作字段） |
| P1-MA2-048（SalaryPostingDispatcher 吞异常悬挂） | 无 view 层投影（纯后端过账链路） |
| P1-MA2-076（crm Event reminderMinutesBefore 死字段） | 无 view 层 drift（Event view:68/:92 暴露 reminderMinutesBefore 为可编辑字段，忠实绑定 ORM 列。view-correct / backend-dead-field——死字段根因在后端 findDueReminders 用全局窗口从不读 per-event 值；view 不放大死字段语义） |
| P1-MA3-047（API 命名/参数跨域不一致 dim7） | 无 view 层 drift（hr/crm 全部自定义动作参数名与 @Name 一致，区别于 A4.7 pur Rfq cancel 投影为 P1-MA4-024） |
| P1-MA3-048（孤儿 Processor bean String 影子契约 dim3） | 无 view 层 drift（hr 无任何 Processor 类，crm 动作全经 BizModel 注解生成，无孤儿悬挂动作引用） |
| P1-MA1-009（crm DECIMAL↔double 7 列） | 无 view 层 drift（5/7 列以 ui:number 暴露未按精确十进制掩码——属后端类型；2/7 列未暴露。MA1 范畴） |
| P1-MA1-022（跨域只读 daoFor 投影 / picker 快照注入字段） | 无 view 层 drift（两域跨实体字段路径全命中 ORM refEntityName，picker 快照注入字段命中 notGenCode 外部实体字段，无悬挂） |
| 前端 UI-roadmap Phase 3（notify-inbox 裸 data / ErpMdPartner 非法 GraphQL） | 无两域 view.xml 残留（delta view.xml 零 ErpMdPartner__ 引用；全部 `<data>` 为合法 AMIS drawer-data XML；countReferences raw GraphQL 为合法 Nop Map 投影） |

## 5. Verdict

**FAIL（有 drift）—— 零 P0**（无活跃数据破坏路径；view.xml drift 最坏为 PII 掩码失效/颜色错误，无 GL/库存写入破坏）。**1 项 P1**（P1-MA4-025 hr Employee PII 掩码非法 `LEFT()`/`RIGHT()` 函数——银行账号/身份证号/手机号掩码功能性失效）+ **2 项 P2** watch-only（P2-MA4-019 hr 跨域通用调色板 10 套 + P2-MA4-020 crm 共享 dict 调色板错配 Lead/Event/ForecastPeriod）。MA2 hr 十项死状态 + crm reminderMinutesBefore + MA3 API 契约 + MA1 跨域只读 + 前端-roadmap Phase 3 已知 finding view 层投影复核**全部「无 view 层 drift」**——hr 死状态密集（P1-MA2-039~048）在 view 层**未放大**死状态风险（死状态不映射为可见动作按钮，仅作只读 dict 显示；ShiftAssignment view 忠实匹配 ORM 无 dict；BankFile/Survey/Timesheet/Employee view 均未暴露死状态迁移按钮）。

**drift 密度评估**：crm 68 + hr 72 = 140 view.xml。**drift 密度 1 P1 / 140 view.xml ≈ 0.71%**，与 A4.6（0.75%）+ A4.7（0.88%）同量级——delta 层 `bounded-merge` 自愈 + BizModel/xbiz 正式接线 + ORM `ext:dict` 绑定三道防线有效抑制字段/动作/dict 三类高频 drift。crm 34 delta view **零 P1（仅 1 项 P2 调色板）**——crm 6 个自定义动作全部参数名/类型匹配 @Name 零悬挂 + 23 dict 全存在 + visibleOn 字面量全命中。hr 36 delta view 1 项 P1（Employee PII 掩码单点）+ 1 项 P2（调色板）。**hr 死状态密集（MA2 十项）在 view 层未放大**——证实 A4.7 注记「hr view 层投影复核为本批次最高优先级」的结论：view 忠实绑定 dict/ORM，死状态根因全在后端。

## 6. 剩余风险

- **view.xml drift 维度（MA4）全域收口**：A4.6（fin+mfg）+ A4.7（pur+sal+inv）+ A4.8（crm+hr）三批共 134+114+140 = **388 view.xml** 系统性审计完成，累计 3 P1（P1-MA4-023/024/025）+ 8 P2，零 P0。drift 密度 3 P1 / 388 ≈ 0.77%，三批稳定。**view.xml drift 维度全域收口**。
- **`_gen` 层未逐文件深审**：本审计以 delta 层为主。`_gen` 层由 XMeta 驱动生成，理论自洽，但若 XMeta 与 ORM 不同步（MA1 未报告两域此类），`_gen` 层可能携带字段 drift。MR2 修复 P1-MA4-025 时建议同步核验 `_gen/_ErpHrEmployee.view.xml`（PII 掩码为 delta 自定义 gen-control，_gen 不含，无同型风险）。
- **gen-control 内联脚本无编译期校验**：P1-MA4-025 + P2-MA4-019/020 根因是 gen-control `<c:script>` 为运行期 JS，无 schema/类型校验——`LEFT`/`RIGHT`/调色板类漂移只能经运行时视觉回归或本型静态审计发现。
- **P1-MA4-025 修复方向**：将 `ErpHrEmployee.view.xml:19/150/157/164` 的 `${LEFT(field,n)}`/`${RIGHT(field,n)}` 改为 JS `String(${field}).slice(n)` / `.slice(-n)`（与全仓其他掩码一致）。
- **未接线 BizMutation（feature-gap，非 drift）**：crm（refreshForecast/assignSequence/recalculateScore/generateQuote/quota/territory mutations）+ hr（markPaid/generateBankFile/Simulation 5 动作/Contract renew+expire/Assessment 2 动作/DevelopmentPlan 3 动作/Shift 系列/SwapRequest 4 动作）大量 BizMutation 未在 view 暴露按钮——属产品功能缺口（owner doc Deferred / 后续 UI 接线），非 view-后端契约 drift。归各域功能完整性范畴，不属本审计。

## 7. 范围内/范围外

- **范围内**：crm 68 + hr 72 view.xml vs 后端契约 7 维度 drift（done）。
- **范围外**（Deferred）：i18n 完整性（A4.9）/ 后端代码实现质量（A4.4 hr + A4.5 crm 已 done）/ 报表 page.yaml 渲染层（前端 UI-roadmap 已修复）/ 像素级视觉回归（前端 UI-roadmap Deferred）/ 其余域 view.xml drift（A4.6 finance+mfg + A4.7 pur+sal+inv 已 done）。

## 8. 状态值映射表

### crm

| 实体 | view 引用状态值 | dict 真值 | 裁决 |
|------|----------------|----------|------|
| ErpCrmEvent.status | visibleOn: PLANNED；badge: COMPLETED/CANCELLED/PLANNED | `erp-crm/event-status`: PLANNED/COMPLETED/CANCELLED | visibleOn ✓；**badge PLANNED 灰 ✗（P2-MA4-020）**；COMPLETED→success ✓；CANCELLED→danger ✓ |
| ErpCrmLead.docStatus | badge: ACTIVE/CANCELLED（仅这两分支） | `erp-crm/lead-doc-status`: NEW/QUALIFIED/CONVERTED/LOST/CANCELLED | **badge ACTIVE 死值 ✗（P2-MA4-020——ACTIVE 不在 lead-doc-status）**；NEW/QUALIFIED/CONVERTED/LOST 全灰；CANCELLED 删除线 ✓ |
| ErpCrmForecastPeriod.status | badge: 通用调色板 | `erp-crm/forecast-period-status`: OPEN/CLOSED/FROZEN | OPEN→primary ✓；CLOSED→success ✓；**FROZEN 灰 ✗（P2-MA4-020）** |

### hr

| 实体 | view 引用状态值 | dict 真值 | 裁决 |
|------|----------------|----------|------|
| ErpHrTimesheet.status | visibleOn: DRAFT；badge: 通用调色板 | `erp-hr/timesheet-status`: DRAFT/SUBMITTED/APPROVED/REJECTED | visibleOn ✓；SUBMITTED→primary ✓；APPROVED→success ✓；REJECTED→danger ✓；DRAFT 灰（**死状态 APPROVED/REJECTED 经 view 仅作只读显示，无动作按钮**） |
| ErpHrSalary.approveStatus | visibleOn: UNSUBMITTED/REJECTED/SUBMITTED/APPROVED；badge: 专用三元 | `wf/approve-status`: UNSUBMITTED/SUBMITTED/APPROVED/REJECTED | visibleOn ✓；badge 专用调色板 ✓（正确，未用通用调色板） |
| ErpHrSalarySimulation.status | badge: 通用调色板（primaryVals 含 IN_PROGRESS） | `erp-hr/simulation-status`: DRAFT/IN_REVIEW/APPROVED/REJECTED/CONVERTED | APPROVED→success ✓；REJECTED→danger ✓；**IN_REVIEW 灰 ✗（P2-MA4-019——调色板 IN_PROGRESS ≠ dict IN_REVIEW）**；DRAFT/CONVERTED 灰 |
| ErpHrPayrollBankFile.status | badge: 通用调色板 | `erp-hr/bank-file-status`: GENERATED/UPLOADED/CONFIRMED | **整列恒灰 ✗（P2-MA4-019——三值全不在调色板）**（**死状态 UPLOADED/CONFIRMED 经 view 仅作只读显示，view 空壳无动作按钮**） |
| ErpHrRecruitment.status | badge: 通用调色板 | `erp-hr/recruitment-status`: OPEN/SCREENING/INTERVIEW/OFFERED/HIRED/REJECTED/CLOSED | OPEN→primary ✓；REJECTED→danger ✓；CLOSED→success ✓；**SCREENING/INTERVIEW/OFFERED/HIRED 灰 ✗（P2-MA4-019）** |
| ErpHrSurvey.status | badge: 通用调色板 | `erp-hr/survey-status`: DRAFT/OPEN/CLOSED/ARCHIVED | OPEN→primary ✓；CLOSED→success（语义错）；DRAFT/ARCHIVED 灰（**死状态 OPEN/CLOSED/ARCHIVED 经 view 仅作只读显示，无动作按钮**） |
| ErpHrShiftSwapRequest.status | badge: 通用调色板 | `erp-hr/swap-status`: PENDING/APPROVED/REJECTED/CANCELLED | APPROVED→success ✓；REJECTED/CANCELLED→danger ✓；**PENDING 灰 ✗（P2-MA4-019）** |
| ErpHrDevelopmentPlan.status | badge: 通用调色板 | `erp-hr/devplan-status`: DRAFT/IN_PROGRESS/COMPLETED/CANCELLED | IN_PROGRESS→primary ✓；COMPLETED→success ✓；CANCELLED→danger ✓；DRAFT 灰（**死状态 DRAFT/CANCELLED 经 view 仅作只读显示，无 cancel 按钮**） |
| ErpHrEmploymentContract.status | badge: 通用调色板 | `erp-hr/contract-status`: ACTIVE/EXPIRED/SUSPENDED/TERMINATED | ACTIVE→success ✓；EXPIRED/SUSPENDED→warning ✓；TERMINATED→danger ✓（**死状态 SUSPENDED 恰命中 warningVals 巧合**） |
| ErpHrEmployeeAssessment.status | badge: 通用调色板 | `erp-hr/assessment-status`: DRAFT/SUBMITTED/COMPLETED | SUBMITTED→primary ✓；COMPLETED→success ✓；DRAFT 灰 |
| ErpHrLeaveRequest.status | badge: 通用调色板 | `erp-hr/leave-status`: DRAFT/SUBMITTED/APPROVED/REJECTED/CANCELLED | SUBMITTED→primary ✓；APPROVED→success ✓；REJECTED/CANCELLED→danger ✓；DRAFT 灰 |
| ErpHrEmployee.employmentStatus | **普通列（无 badge、无守卫）** | `erp-hr/employment-status`: ACTIVE/PROBATION/RESIGNED/TERMINATED/RETIRED | view 忠实显示 dict label，无状态守卫/动作按钮（**死状态 RESIGNED/TERMINATED/RETIRED 经 view 仅作只读 dict 显示**）→ ✓ 无 view drift |
| ErpHrShiftAssignment.status | **普通列（无 dict 绑定、无 badge——忠实匹配 ORM 无 ext:dict）** | （ORM 无 ext:dict；值经 ASSIGNMENT_STATUS_* 常量） | view 忠实匹配 ORM 无 dict（P1-MA2-046 根因在 ORM/后端）→ ✓ 无 view drift |
