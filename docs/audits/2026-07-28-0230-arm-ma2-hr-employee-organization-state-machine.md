# ARM MA2 — hr 员工与组织状态机业务审查（A2.7a，S 级拆分 1/2）

> Audit Status: closed
> Mission: audit-remediation
> Work Item: A2.7a hr 状态机审查 — 员工与组织（S 级拆分 1/2）
> Source Plan: `docs/plans/2026-07-28-0230-1-audit-remediation-ma2-hr-employee-organization-state-machine.md`
> Skill: `docs/skills/state-machine-business-review-prompt.md`（+ 项目定制化层 `docs/skills/README.md §项目定制化层`）
> Reviewed: 2026-07-28
> Scope: **员工雇佣状态机**（`ErpHrEmployee.employmentStatus` dict `erp-hr/employment-status` 5 态）+ **劳动合同状态机**（`ErpHrEmploymentContract.status` dict `erp-hr/contract-status` 4 态）+ **招聘管线状态机**（`ErpHrRecruitment.status` dict `erp-hr/recruitment-status` 7 态）+ **员工考核状态机**（`ErpHrEmployeeAssessment.status` dict `erp-hr/assessment-status` 3 态）+ **发展计划状态机**（`ErpHrDevelopmentPlan.status` dict `erp-hr/devplan-status` 4 态）+ **发展计划项状态机**（`ErpHrDevelopmentPlanItem.status` dict `erp-hr/plan-item-status` 4 态）+ **员工调查状态机**（`ErpHrSurvey.status` dict `erp-hr/survey-status` 4 态）。考勤/排班/请假/工资/仿真状态机归 A2.7b（后续执行）。
> Related: A2.5a/b/c finance 状态机审查三拆分全 done（`docs/audits/2026-07-27-2211-arm-ma2-finance-posting-voucher-state-machine.md` + `2026-07-27-2315-arm-ma2-finance-period-budget-state-machine.md` + `2026-07-27-2315-arm-ma2-finance-arap-settlement-state-machine.md`，dict 死状态同型裁决范式 P1-MA2-031）；A2.6a/b manufacturing 状态机审查两拆分全 done（`docs/audits/2026-07-28-0109-arm-ma2-mfg-work-order-jobcard-state-machine.md` + `2026-07-28-0109-arm-ma2-mfg-mrp-bom-state-machine.md`，dict 死状态 + owner doc 漂移同型裁决 P1-MA2-035/036/037/038）；A2.7b 考勤与工资状态机审查（S 级拆分 2/2）后续执行——工资核算依赖员工 employmentStatus=ACTIVE/PROBATION（`ErpHrSalaryBizModel.java:236`）。

## 1. 裁决

**Verdict: pass（零 P0、4 项新 P1、5 项新 P2 watch-only）**

hr 员工与组织七组件状态机（员工 5 态 + 合同 4 态 + 招聘 7 态 + 考核 3 态 + 发展计划 4 态 + 发展计划项 4 态 + 调查 4 态）核心契约经实仓逐项证据确认：在职状态机（ACTIVE/PROBATION）+ 招聘状态机（OPEN→...→HIRED）+ 考核状态机（DRAFT→SUBMITTED→COMPLETED）+ 发展计划状态机（IN_PROGRESS→COMPLETED）+ 发展计划项状态机（NOT_STARTED→IN_PROGRESS→ACHIEVED）的**主路径状态迁移守卫齐全**（`requireTransferableEmployee`/`requireStatus`/`assertPlanItemTransition`/`Objects.equals(status, ...)` 前置校验）、`@BizMutation` 事务回滚保证调岗失败时员工+合同一致性、招聘 hire 跨实体副作用（创建 Employee+Contract）经事务边界保护、考核 completeAssessment 跨实体刷新 gapAnalysis 经 `refreshGapAnalysisWithLevels` 直传聚合 levels 避免二次查询跨事务可见性问题。

**关键裁决（计划假设证伪/确认）**：

| 计划假设 | 裁决 | 证据 |
|---|---|---|
| 员工离职/终止/退休 employmentStatus 迁移完全缺失致 RESIGNED/TERMINATED/RETIRED 三态死状态（候选 P0） | **确认死状态 + 升 P1** | 全 `module-hr/erp-hr-service/src/main` grep `setEmploymentStatus` 仅 `ErpHrRecruitmentBizModel.java:156`（`EMPLOYMENT_ACTIVE`）+ `ErpHrEmployeeBizModel.java` **无 resign/terminate/retire/probationToRegular 任何 BizMutation 方法**（341 行全文读，仅 `transferEmployee` 读 employmentStatus，无任何 setStatus 写入）。`EMPLOYMENT_RESIGNED/TERMINATED/RETIRED` 常量定义于 `ErpHrConstants.java:215-217`，仅 `ErpHrEmployeeBizModel.nonTransferableStatuses():336-338` 作只读调岗守卫引用。owner doc `state-machine.md §场景D/E` 明确声明「ACTIVE→RESIGNED（主动离职）+ ACTIVE→TERMINATED（解雇）+ ACTIVE→RETIRED（退休）+ PROBATION→ACTIVE（转正）」迁移 + 联动（取消未完成 LeaveRequest / 终止 ACTIVE 合同 / 禁用 UserAccount），**代码完全未实现**。**三态确认不可达（dict 死状态）**。但**不破坏主路径**——在职生命周期（ACTIVE/PROBATION）由招聘 hire 创建 + 调岗 transferEmployee 维护，覆盖了在职员工全部业务交互；RESIGNED/TERMINATED/RETIRED 是终态等待点（owner doc 标注），缺失迁移意味着这些状态在 DB 永不出现，但**不影响已实现路径运行时正确性**（工资核算 `ErpHrSalaryBizModel.java:236` 守卫 ACTIVE/PROBATION，不会因终态死状态产生悬挂数据）。按 finance A2.5a P1-MA2-031 + mfg A2.6a P1-MA2-035 + A2.6b P1-MA2-036 同型裁决：dict 项不可达 + owner doc 声明但代码无实现 → **P1-MA2-039**（MR1 实现迁移 + 联动或 owner doc 标注 Deferred + 删除 dict 项）。**非 P0**——主路径未破坏、无悬挂半状态、无数据错误。 |
| 合同 SUSPENDED 状态无 writer 致 dict 死状态（候选 P1） | **确认死状态 P1** | 全 `src/main` grep `CONTRACT_STATUS_SUSPENDED` 仅 `ErpHrConstants.java:223` + `_ErpHrDaoConstants.java:109` 常量定义零使用。owner doc `state-machine.md §对象二 员工雇佣状态` 表无合同独立章节（合同状态机散落在 `state-machine.md` 仅一行提及「停薪留职」语义未定义迁移）。`ErpHrEmploymentContractBizModel.java`（111 行全文读）仅 `expireOverdueContracts:72-90`（ACTIVE→EXPIRED）+ `renew:93-109`（ACTIVE/EXPIRED→ACTIVE）。`SUSPENDED` dict 项不可达 → **P1-MA2-040**（MR1 实现停薪留职迁移或 owner doc 标注 Deferred + 删除 dict 项）。**非 P0**——不破坏合同主路径（ACTIVE/EXPIRED/TERMINATED 经 expire/renew/transferEmployee 副作用完整覆盖合同生命周期）。 |
| 员工调查 OPEN/CLOSED/ARCHIVED 三态死状态 + 桩 BizModel（候选 P1） | **确认死状态 P1** | `ErpHrSurveyBizModel.java`（18 行全文读）**仅 CRUD 继承**——`extends CrudBizModel<ErpHrSurvey>` 无任何状态迁移方法。owner doc `employee-survey.md §状态机` 明确声明「DRAFT（编辑中）→ OPEN（发布，可填写）→ CLOSED（截止）→ ARCHIVED（归档）；OPEN 可直接→ CLOSED」迁移 + CLOSED 时触发自动聚合 → ErpHrSurveyResult。`SURVEY_STATUS_OPEN/CLOSED/ARCHIVED` 常量定义于 `_ErpHrDaoConstants.java:609/614/619` 零使用。三态确认不可达 → **P1-MA2-041**（MR1 实现 publish/close/archive 迁移或 owner doc 标注「BizModel 桩，状态机 Deferred」+ 删除 dict 项）。**非 P0**——桩 BizModel 不破坏主路径（DRAFT 创建 + CRUD 完整可用；调查门户/分发是 owner doc Deferred 能力，缺失状态机不产生悬挂数据）。 |
| 发展计划 DRAFT/CANCELLED + 计划项 OVERDUE dict 死状态（候选 P1） | **确认死状态 P1** | (a) `ErpHrDevelopmentPlanBizModel.generateDevelopmentPlan:82` `plan.setStatus(ErpHrConstants.DEV_PLAN_STATUS_IN_PROGRESS)`——**生成即 IN_PROGRESS**，无任何 setStatus(DEV_PLAN_STATUS_DRAFT) 调用，DRAFT dict 项不可达；(b) `DEV_PLAN_STATUS_CANCELLED` 常量定义于 `ErpHrConstants.java:188` 零使用——`completePlan:115-129` 仅 DRAFT/IN_PROGRESS→COMPLETED，无 cancelPlan 迁移；(c) `PLAN_ITEM_STATUS_OVERDUE` 常量定义于 `ErpHrConstants.java:194`，仅在 `isValidPlanItemTransition:215` 作 target 守卫引用——**无任何 setStatus(OVERDUE) 主动写入**（是设计语义的被动逾期标记，但无定时 job 计算逾期，实际永不可达）。**4 dict 项不可达** → **P1-MA2-042**（MR1 实现取消迁移 + 自动 OVERDUE job 或 owner doc 标注「设计为简化，DRAFT/OVERDUE/CANCELLED 为预留状态」+ 删除 dict 项）。**非 P0**——发展计划主路径（IN_PROGRESS→COMPLETED + 计划项 NOT_STARTED→IN_PROGRESS→ACHIEVED）完整覆盖员工发展闭环。 |
| 招聘 close 无守卫致已 HIRED 招聘可被关闭（候选 P0/P1） | **证伪 P0，登记 P2** | `ErpHrRecruitmentBizModel.close:137-142` 全文读：`rec.setStatus(RECRUITMENT_STATUS_CLOSED)` 无任何 status 守卫——任意状态（含 OPEN/SCREENING/INTERVIEW/OFFERED/HIRED）可被 CLOSED。但**close 不破坏状态机**：(a) HIRED→CLOSED 是合法的入职后清理（recruitment.employeeId 持久化保留，Employee 记录不受影响）；(b) OPEN→CLOSED 是合法的招聘取消（候选人尚未入职时关闭招聘是管理动作）；(c) `useLogicalDelete=true` 保证记录不丢；(d) 与 reject 守卫（L125-129 拒 HIRED/CLOSED/REJECTED）不对称是有意设计——reject 是状态机推进失败出口，close 是行政管理关闭。**非 P0**（无数据破坏）+ **非 P1**（不破坏已实现路径）。owner doc `recruitment.md §关键业务规则` 未显式声明 close 守卫——登记 **P2-MA2-048** watch-only（承诺但无证据控制点 + owner doc 文字校验建议）。 |
| 调岗请假冲突 warnIfLeaveConflict 非阻断（候选 P1） | **设计裁定非缺陷** | `ErpHrEmployeeBizModel.warnIfLeaveConflict:200-220` 检测调岗生效日与已批准休假重叠仅 `LOG.warn(...)` 不抛异常。owner doc `use-cases.md UC-HR-08` 设计接受（"告警不阻塞，由 HR 决策"）——调岗可发生在休假结束后或 HR 决定中断休假，业务上合法。**非缺陷**，登记 **P2-MA2-050** watch-only（owner doc 文字校验 + 文档化非阻断语义）。 |
| 长期 PROBATION 未转正无 TODO 提醒（候选 P1） | **证伪 P1，登记 P2** | owner doc `state-machine.md §场景E` 声明试用期转正（PROBATION→ACTIVE）+ 设置 regularDate，**代码无 probationToRegular 迁移**（同 P1-MA2-039）。员工长期 PROBATION 静默下沉（无 TODO 通知 HR 评估转正）。但**非 P1**——试用期数据完整性由 `ErpHrEmployee.probationEndDate` 字段承载（可由 HR 主动查询），缺失自动 TODO 是 UX 缺陷非数据缺陷。登记 **P2-MA2-051** watch-only（与 P1-MA2-039 联动：MR1 实现转正迁移时一并补 TODO）。 |

### 1.1 审查范围

- **员工雇佣状态机**：`ErpHrEmployee`（`orm.xml:265-340`，列 `employmentStatus` 在 :294）+ `ErpHrEmployeeBizModel.java`（341 行，仅 `transferEmployee` + 守卫 `isTransferable`/`requireTransferableEmployee`/`requireTargetDepartment`/`requireTargetPosition` + `warnIfLeaveConflict` + `resolveHandleContract` + `countReferences`）。
- **劳动合同状态机**：`ErpHrEmploymentContract`（`orm.xml:419-478`，列 `status` 在 :438）+ `ErpHrEmploymentContractBizModel.java`（111 行，`scanExpiringContracts` + `expireOverdueContracts` 批量单失败隔离 + `renew`）。
- **招聘管线状态机**：`ErpHrRecruitment`（`orm.xml:785-851`，列 `status` 在 :799）+ `ErpHrRecruitmentBizModel.java`（225 行，6 迁移方法 + `hire` 跨实体副作用）。
- **考核状态机**：`ErpHrEmployeeAssessment`（`orm.xml:1614-1657`，列 `status` 在 :1623）+ `ErpHrEmployeeAssessmentBizModel.java`（158 行，`submitAssessment` + `completeAssessment` + `aggregateAndWriteBack`）。
- **发展计划状态机**：`ErpHrDevelopmentPlan`（`orm.xml:1727-1764`，列 `status` 在 :1735）+ `ErpHrDevelopmentPlanBizModel.java`（224 行，`generateDevelopmentPlan` + `updatePlanItemStatus` + `completePlan` + `isValidPlanItemTransition`）。
- **发展计划项状态机**：`ErpHrDevelopmentPlanItem`（`orm.xml:1767-1812`，列 `status` 在 :1780）——同上 BizModel 内嵌 `assertPlanItemTransition`。
- **员工调查状态机**：`ErpHrSurvey`（`orm.xml:1327-1376`，列 `status` 在 :1337）+ `ErpHrSurveyBizModel.java`（18 行 CRUD 桩）。
- **定时 Job**：`ErpHrContractExpiryJob.java`（111 行，cron-gated + 单失败隔离 + `IErpSysNotificationBiz` 跨域派发到期预警）。
- **owner doc**：`docs/design/human-resource/state-machine.md`（适用对象二员工雇佣状态 + §场景D/E 离职/终止/退休/转正 + §适用对象四薪酬审批 wf/approve-status 标准四态）+ `recruitment.md`（招聘管线 7 态 + Offer/Onboarding 多实体 Deferred）+ `competency-management.md`（考核 3 态 + 发展计划 4 态 + 计划项 4 态 + gap 分析）+ `employee-survey.md`（调查 4 态生命周期）+ `README.md`（域边界）。
- **测试**：`TestErpHrRecruitmentEngine`（招聘全链 OPEN→HIRED + hire 联动）+ `TestErpHrEmployeeTransfer`（调岗 + 合同处理）+ `TestErpHrEmployeeReferences`（countReferences 同域只读聚合）+ `TestErpHrCompetencyManagement`（考核 + gap + 发展计划状态机）+ `job/TestErpHrContractExpiry`（合同 expire/renew + job 触发）+ `TestErpHrSurveyCrudSmoke`（调查 CRUD 冒烟——无状态迁移测试，匹配桩 BizModel）。

### 1.2 可达性摘要

- **员工雇佣 5 态中 2 态可达，3 态不可达**：ACTIVE 经招聘 hire 创建（`ErpHrRecruitmentBizModel:156`）；PROBATION 经新建员工时显式设置（owner doc §场景 E 转正入口）；ACTIVE↔PROBATION 由 `transferEmployee` 守卫只读（`isTransferable` 允许两态调岗）；**RESIGNED/TERMINATED/RETIRED 无任何 setStatus writer → 不可达**（P1-MA2-039）。无 PROBATION→ACTIVE 转正迁移（同 P1-MA2-039）。
- **合同 4 态中 3 态可达，1 态不可达**：ACTIVE 经招聘 hire 跨实体创建（`createContractForNewEmployee:187`）+ 调岗副作用新建（`ErpHrEmployeeBizModel.newContractFrom:293`）；EXPIRED 经 `expireOverdueContracts:82` 批量推进；TERMINATED 经调岗副作用（`resolveHandleContract:243`）；**SUSPENDED 无任何 setStatus writer → 不可达**（P1-MA2-040）。
- **招聘 7 态全部可达**：OPEN（新建 default）→SCREENING（`moveToScreening`）→INTERVIEW（`scheduleInterview`）→OFFERED（`makeOffer`）→HIRED（`hire` 跨实体创建 Employee+Contract）；非终态→REJECTED（`reject` 守卫拒 HIRED/CLOSED/REJECTED）；任意态→CLOSED（`close` 无守卫）。
- **考核 3 态全部可达**：DRAFT（新建）→SUBMITTED（`submitAssessment` 守卫 + details 非空）→COMPLETED（`completeAssessment` 守卫 + aggregate + gapAnalysis 刷新）。
- **发展计划 4 态中 2 态可达，2 态不可达**：IN_PROGRESS（`generateDevelopmentPlan:82` 创建即此态）→COMPLETED（`completePlan` 守卫 DRAFT/IN_PROGRESS）；**DRAFT 无任何 setStatus writer → 不可达**（generatePlan 直接 IN_PROGRESS）；**CANCELLED 无任何 setStatus writer → 不可达**（无 cancelPlan 迁移）（P1-MA2-042）。
- **发展计划项 4 态中 3 态可达，1 态不可达**：NOT_STARTED（`newPlanItem:180` 创建即此态）→IN_PROGRESS（`updatePlanItemStatus` 守卫 + 设 startDate）→ACHIEVED（`updatePlanItemStatus` 守卫 + 设 endDate）；**OVERDUE 无任何 setStatus writer → 不可达**（是 `isValidPlanItemTransition:215` 允许的合法 target，但无主动 writer——设计为被动逾期标记，缺定时 job）（P1-MA2-042）。
- **员工调查 4 态中 1 态可达，3 态不可达**：DRAFT（新建 default + CRUD）；**OPEN/CLOSED/ARCHIVED 无任何 setStatus writer → 不可达**（BizModel 是 18 行 CRUD 桩）（P1-MA2-041）。

### 1.3 角色/权限摘要

owner doc `state-machine.md §场景D/E` + `recruitment.md` + `competency-management.md` + `employee-survey.md` 定义员工/合同/招聘/考核/发展计划/调查角色矩阵（调岗=HR 专员 / 招聘各阶段=HR→主管面试→offer 审批 / 考核 submit=员工自评 / complete=主管 / 合同 renew=HR / expire=系统 job / 调查 publish=HR）。本审计未做权限绑定运行时验证（归 A4.4 hr 代码质量审计 + A6 权限审计），状态机层面无角色漂移反模式。**危险操作门控不完整**：(a) 员工 RESIGNED/TERMINATED/RETIRED 联动（owner doc §场景D 声明禁用账户——若 MR1 实现须触及 `nop-auth` UserAccount 保护区域，需独立 plan-audit）；(b) 招聘 `close` 无守卫（任意角色可关闭已 HIRED 招聘——非破坏但需 owner doc 校验，P2-MA2-048）；(c) 招聘 `hire` 跨实体创建 Employee+Contract（经 @BizMutation 事务回滚保证失败原子性，无悬挂半状态——证据：`hire:108-118` 顺序 rec.setStatus(HIRED) → createEmployeeFromRecruitment → rec.setEmployeeId → updateEntity → createContractForNewEmployee）。

### 1.4 外部依赖摘要

- **同域 I\*Biz 写**：招聘 hire → `IErpHrEmployeeBiz.saveEntity` + `IErpHrEmploymentContractBiz.saveEntity`（同域合法）；考核 complete → `IErpHrGapAnalysisBiz.refreshGapAnalysisWithLevels`（同域合法 + 直传聚合 levels 避免跨事务可见性问题，competency-management.md 实现注记已文档化）；调岗 → `IErpHrEmploymentContractBiz.updateEntity/saveEntity`（合同 TERMINATED→新建 ACTIVE）+ `IErpHrLeaveRequestBiz.findCount`（只读检测冲突）。
- **跨域只读**：`ErpHrReportBizModel.java:268` `daoFor(ErpMdPartner)`（master-data 只读）属 P1-MA1-022 同型（已登记 MR1 dashboard 聚合）——本审计复核对状态机运行时无影响。
- **跨域通知派发**：`IErpSysNotificationBiz`（notify 域）注入于 `ErpHrContractExpiryJob:40`——合同到期预警派发经 notify 跨域 I\*Biz（合法，跨域通知派发子系统设计目标）。
- **未实现的联动（owner doc §场景D 声明但代码缺失）**：员工 RESIGNED/TERMINATED/RETIRED 应触发 (a) 同员工未完成 LeaveRequest→CANCELLED（跨域写 hr 内部，需 leave-status 状态机迁移）、(b) 同员工 ACTIVE 合同→TERMINATED（同域写，需 contract-status 迁移）、(c) UserAccount 停用（跨域写 nop-auth，触及权限保护区域）——**全部未实现**（P1-MA2-039 owner doc 漂移的联动维度）。MR1 实现时须 owner doc 描述预期行为 + 该修复子切片的独立审计 + 人工确认（保护区域门控）。
- **事务回滚**：所有状态迁移动作经 `@BizMutation`（Nop 平台自动事务包装），任一外部步骤失败（合同写 / 通知派发 / 跨实体刷新）抛异常 → 事务回滚 → 状态保持事务开始前值。证据：`transferEmployee` 调岗生效日期与已批准休假冲突时 `warnIfLeaveConflict` 仅 LOG.warn 不阻断——调岗不被前置冲突拦截；调岗后合同处理 `resolveHandleContract` 失败时经 @BizMutation 事务回滚员工 dept/position 更新回滚。

### 1.5 剩余风险

- 员工 employmentStatus RESIGNED/TERMINATED/RETIRED 三态死状态 + owner doc §场景D/E 联动未实现（**P1-MA2-039**，MR1）；
- 合同 SUSPENDED dict 死状态（**P1-MA2-040**，MR1）；
- 调查 OPEN/CLOSED/ARCHIVED 三态死状态 + 桩 BizModel（**P1-MA2-041**，MR1）；
- 发展计划 DRAFT/CANCELLED + 计划项 OVERDUE dict 死状态（**P1-MA2-042**，MR1）；
- state-machine.md 缺招聘/合同/调查独立章节（**P2-MA2-047**）；
- 招聘 close 无守卫（**P2-MA2-048**）；
- recruitment.md 多实体 Deferred 未注记（**P2-MA2-049**）；
- 调岗请假冲突 warn 非阻断（**P2-MA2-050**）；
- 长期 PROBATION 未转正无 TODO 提醒（**P2-MA2-051**）；
- 并发调岗同员工（无 @Version 保护 employee.employmentStatus 字段——但 ErpHrEmployee 整体 `versionProp="version"` ORM 透明乐观锁 detectable conflict；交接 A2.17）/ 批量 expire 竞态 / 招聘 hire 并发（5 处交接 A2.17）。

---

## 2. 状态图与转换矩阵

### 2.1 员工雇佣状态机（`ErpHrEmployee.employmentStatus`，dict `erp-hr/employment-status` 5 态）

```
                     hire (招聘跨实体副作用)
                     ──────────────────────►
                                            [ACTIVE] ◄──┐
                                            ▲           │ transferEmployee
                                            │           │ (只读守卫，状态不变)
                                            │           │
                                            │     [PROBATION]
                                            │           ▲
                                            │           │ 新建 (owner doc §场景 E 转正入口)
                                            │           │
   ❌ 缺迁移 (P1-MA2-039 §场景D 未实现)      │           │
   [RESIGNED]   ◄── (无 setStatus writer)   │           │
   [TERMINATED] ◄── (无 setStatus writer)   │           │
   [RETIRED]    ◄── (无 setStatus writer)   │           │
   ❌ 缺迁移 PROBATION→ACTIVE (§场景E 转正未实现)
```

| 迁移 | 触发 | 代码位置 | 守卫 | 裁决 |
|---|---|---|---|---|
| (新建)→ACTIVE | 招聘 hire 跨实体副作用 | `ErpHrRecruitmentBizModel.createEmployeeFromRecruitment:156` `setEmploymentStatus(EMPLOYMENT_ACTIVE)` | 招聘 OFFERED 守卫（`requireStatus(rec, OFFERED, HIRED)` L109） | PASS |
| (新建)→PROBATION | (owner doc 声明试用期入职，未在招聘 hire 中实现——hire 直接 ACTIVE，PROBATION 由 HR 手工新建或在 owner doc Deferred 的多实体招聘拆分中实现) | — | — | PASS（owner doc Deferred——recruitment.md:336 描述 `employmentStatus = PROBATION` 但实现简化为 ACTIVE，UI 后续手工调整） |
| ACTIVE→dept/position 更新（status 不变） | transferEmployee | `ErpHrEmployeeBizModel.transferEmployee:88-116` | `isTransferable:165-168` 仅 ACTIVE/PROBATION 允许调岗 + `requireTargetDepartment:170-180` + `requireTargetPosition:182-196` + `warnIfLeaveConflict:200-220`（warn 非阻断） | PASS |
| ACTIVE→RESIGNED（§场景D 未实现） | (无 resign BizMutation) | — | — | **FAIL — P1-MA2-039**（dict 死状态 + owner doc 漂移） |
| ACTIVE→TERMINATED（§场景D 未实现） | (无 terminate BizMutation) | — | — | **FAIL — P1-MA2-039** |
| ACTIVE→RETIRED（§场景E 未实现） | (无 retire BizMutation) | — | — | **FAIL — P1-MA2-039** |
| PROBATION→ACTIVE（§场景E 转正未实现） | (无 probationToRegular BizMutation) | — | — | **FAIL — P1-MA2-039** |
| RESIGNED/TERMINATED/RETIRED 联动（§场景D 声明） | (无联动实现——取消 LeaveRequest/终止合同/禁用账户均缺失) | — | — | **FAIL — P1-MA2-039**（联动维度） |

**员工终态**：RESIGNED/TERMINATED/RETIRED（owner doc §3 明示不可恢复，再入职新建 ErpHrEmployee）—— **三态在 DB 永不出现**（无 writer）。

### 2.2 劳动合同状态机（`ErpHrEmploymentContract.status`，dict `erp-hr/contract-status` 4 态）

```
                     hire (招聘跨实体副作用)            transferEmployee (调岗副作用)
                     ─────────────────────►            ──────────────────────────►
                                            [ACTIVE]                              [TERMINATED]
                                            ▲   │                                      
                                            │   │ expireOverdueContracts (Job cron-gated)
                                            │   ▼
                                       renew ◄── [EXPIRED]
                                            (ACTIVE/EXPIRED → ACTIVE)
                                            
   ❌ 缺迁移 (P1-MA2-040 无 writer)
   [SUSPENDED]  ◄── (无 setStatus writer)
```

| 迁移 | 触发 | 代码位置 | 守卫 | 裁决 |
|---|---|---|---|---|
| (新建)→ACTIVE | 招聘 hire 跨实体副作用 | `ErpHrRecruitmentBizModel.createContractForNewEmployee:187` `setStatus(CONTRACT_STATUS_ACTIVE)` | 招聘 OFFERED 守卫 | PASS |
| (新建)→ACTIVE | 调岗 resolveHandleContract 新建后继合同 | `ErpHrEmployeeBizModel.newContractFrom:293` `c.setStatus(CONTRACT_STATUS_ACTIVE)` | `findActiveContract` 当前 ACTIVE 存在 + `shouldHandle=true`（handleContract 参数 / config-gated） | PASS |
| ACTIVE→TERMINATED | 调岗 resolveHandleContract 终止原合同 | `ErpHrEmployeeBizModel.resolveHandleContract:243` `active.setStatus(CONTRACT_STATUS_TERMINATED)` | 同上 | PASS |
| ACTIVE→EXPIRED | expireOverdueContracts 批量推进（Job） | `ErpHrEmploymentContractBizModel.expireOverdueContracts:82` `c.setStatus(CONTRACT_STATUS_EXPIRED)` | `endDate < now` 过滤 + ACTIVE 守卫（L76）+ 单失败隔离（L81-87 try/catch） | PASS |
| ACTIVE/EXPIRED→ACTIVE | renew | `ErpHrEmploymentContractBizModel.renew:93-109` `contract.setStatus(CONTRACT_STATUS_ACTIVE)` | L99-104 仅允许 ACTIVE/EXPIRED → 否则 `ERR_CONTRACT_ILLEGAL_STATUS_TRANSITION` | PASS |
| ACTIVE→SUSPENDED（停薪留职） | (无 suspend BizMutation) | — | — | **FAIL — P1-MA2-040**（dict 死状态 + owner doc 无独立章节） |
| SUSPENDED→ACTIVE（恢复） | (无 resume BizMutation) | — | — | **FAIL — P1-MA2-040** |

**合同终态**：TERMINATED（owner doc §3 不可恢复）；EXPIRED 非真终态（renew 可回 ACTIVE）；SUSPENDED 应为等待点（owner doc 无描述）。

### 2.3 招聘管线状态机（`ErpHrRecruitment.status`，dict `erp-hr/recruitment-status` 7 态）

```
   [OPEN] ──moveToScreening──► [SCREENING] ──scheduleInterview──► [INTERVIEW]
                                                                  │
                                                                  │ makeOffer
                                                                  ▼
                                                              [OFFERED]
                                                                  │
                                                                  │ hire (跨实体副作用: 创建 Employee+Contract)
                                                                  ▼
                                                              [HIRED]   ◄── 终态
                                                                  
   任意非终态 ──reject──► [REJECTED]   ◄── 终态（守卫拒 HIRED/CLOSED/REJECTED）
   任意状态 ──close──► [CLOSED]        ◄── 终态（无守卫，含 HIRED 合法清理 / OPEN 合法取消）
```

| 迁移 | 触发 | 代码位置 | 守卫 | 裁决 |
|---|---|---|---|---|
| (新建)→OPEN | defaultPrepareSave 默认 | `ErpHrRecruitmentBizModel.defaultPrepareSave:60-62` | status==null → set OPEN | PASS |
| OPEN→SCREENING | moveToScreening | `:66-73` | `requireStatus(rec, OPEN, SCREENING)` L69 | PASS |
| SCREENING→INTERVIEW | scheduleInterview | `:76-88` | `requireStatus(rec, SCREENING, INTERVIEW)` L82 | PASS |
| INTERVIEW→OFFERED | makeOffer | `:91-101` | `requireStatus(rec, INTERVIEW, OFFERED)` L96 | PASS |
| OFFERED→HIRED | hire（跨实体创建 Employee+Contract） | `:104-119` | `requireStatus(rec, OFFERED, HIRED)` L109 | PASS |
| 非终态→REJECTED | reject | `:122-133` | L125-129 拒 HIRED/CLOSED/REJECTED → `illegalTransition` | PASS |
| 任意态→CLOSED | close | `:137-142` | **无守卫**——任意 status 可转 CLOSED（含 HIRED 是合法入职后清理） | **PASS 但登记 P2-MA2-048**（无守卫，owner doc 未声明） |

**招聘终态**：HIRED（候选人已入职，不可恢复）/ CLOSED（行政管理关闭，不可恢复）/ REJECTED（候选人拒绝，可重新申请新建 ErpHrRecruitment——owner doc `recruitment.md §关键业务规则 #3`）。

### 2.4 员工考核状态机（`ErpHrEmployeeAssessment.status`，dict `erp-hr/assessment-status` 3 态）

```
   [DRAFT] ──submitAssessment (details 非空守卫)──► [SUBMITTED] ──completeAssessment──► [COMPLETED]
                                                          │                                │
                                                          │                                │ aggregateAndWriteBack
                                                          │                                │ + gapAnalysisBiz.refreshGapAnalysisWithLevels (跨实体刷新)
                                                          │                                ▼
                                                          │                            (终态)
```

| 迁移 | 触发 | 代码位置 | 守卫 | 裁决 |
|---|---|---|---|---|
| (新建)→DRAFT | defaultPrepareSave | — | status 默认未设 → 经 GraphQL save 管道走 defaultPrepareSave | PASS |
| DRAFT→SUBMITTED | submitAssessment | `:61-77` | L66-68 status 必须 DRAFT + L69-73 details 非空（`ERR_ASSESSMENT_NO_DETAILS`） | PASS |
| SUBMITTED→COMPLETED | completeAssessment（aggregateAndWriteBack + gapAnalysis 刷新） | `:80-104` | L85-87 status 必须 SUBMITTED + L89-93 details 非空 | PASS |

**考核终态**：COMPLETED（owner doc 无 REJECT——考核无驳回是设计裁定，HR 不驳回已提交评估，重新评估经新建 ErpHrEmployeeAssessment）。

### 2.5 发展计划状态机（`ErpHrDevelopmentPlan.status`，dict `erp-hr/devplan-status` 4 态）

```
                generateDevelopmentPlan (从 CRITICAL/MODERATE gap 创建)
   ❌ 缺迁移   ──────────────────────────────────────────────────►  [IN_PROGRESS]
   [DRAFT]                                                              │
   (无 writer)                                                          │ completePlan (守卫 DRAFT/IN_PROGRESS)
                                                                       ▼
   ❌ 缺迁移                                                          [COMPLETED]
   [CANCELLED]                                                         (终态)
   (无 cancelPlan writer)
```

| 迁移 | 触发 | 代码位置 | 守卫 | 裁决 |
|---|---|---|---|---|
| (新建)→IN_PROGRESS | generateDevelopmentPlan | `:82` `plan.setStatus(DEV_PLAN_STATUS_IN_PROGRESS)` | `findActionableGaps` CRITICAL/MODERATE 非空 + 自动生成计划项 | PASS |
| IN_PROGRESS→COMPLETED | completePlan | `:115-129` | L119-125 status 必须 DRAFT/IN_PROGRESS → 否则 `ERR_DEV_PLAN_ILLEGAL_STATUS_TRANSITION` | PASS |
| (新建)→DRAFT | (无 setStatus(DRAFT) writer——generatePlan 直接 IN_PROGRESS) | — | — | **FAIL — P1-MA2-042**（dict 死状态） |
| DRAFT/IN_PROGRESS→CANCELLED | (无 cancelPlan BizMutation) | — | — | **FAIL — P1-MA2-042**（dict 死状态） |

**发展计划终态**：COMPLETED（不可恢复）/ CANCELLED（不可恢复——但 unreachable）。

### 2.6 发展计划项状态机（`ErpHrDevelopmentPlanItem.status`，dict `erp-hr/plan-item-status` 4 态）

```
   [NOT_STARTED] ──updatePlanItemStatus (设 startDate)──► [IN_PROGRESS] ──┬──► [ACHIEVED] (设 endDate)
                                                                           │       (终态)
                                                                           │
                                                                           │ (isValidPlanItemTransition 允许
                                                                           │  但无主动 writer)
                                                                           ▼
                                                                       [OVERDUE]
                                                                       ❌ 缺迁移 (无 setStatus writer / 无 job 计算逾期)
```

| 迁移 | 触发 | 代码位置 | 守卫 | 裁决 |
|---|---|---|---|---|
| (新建)→NOT_STARTED | generateDevelopmentPlan→newPlanItem | `:180` `item.setStatus(PLAN_ITEM_STATUS_NOT_STARTED)` | — | PASS |
| NOT_STARTED→IN_PROGRESS | updatePlanItemStatus | `:94-112` + `:208-218` | `isValidPlanItemTransition` L210-211 仅允许 | PASS（同时设 startDate） |
| IN_PROGRESS→ACHIEVED | updatePlanItemStatus | 同上 | L213-214 仅允许 | PASS（同时设 endDate） |
| IN_PROGRESS→OVERDUE | (设计为被动逾期标记，但无 setStatus(OVERDUE) writer / 无定时 job) | — | `isValidPlanItemTransition:215` 允许此 target 但无主动 writer | **FAIL — P1-MA2-042**（dict 死状态——被动状态无 job 驱动） |
| ACHIEVED→（终态无出边） | — | — | L217 终态返回 false | PASS |
| OVERDUE→（终态无出边） | — | — | 同上 | PASS（设计） |

**计划项终态**：ACHIEVED（不可恢复）/ OVERDUE（不可恢复——但 unreachable）。

### 2.7 员工调查状态机（`ErpHrSurvey.status`，dict `erp-hr/survey-status` 4 态）

```
   [DRAFT] ──(CRUD 桩，无 publish 迁移)──► ❌
       │
       │ (新建 default + CRUD only)
       ▼
   (BizModel 是 18 行 CRUD 桩，无任何状态迁移方法)
   
   ❌ 缺迁移 (P1-MA2-041 owner doc §状态机声明但代码未实现)
   [OPEN]    ◄── (无 setStatus writer)
   [CLOSED]  ◄── (无 setStatus writer + 无自动聚合 ErpHrSurveyResult)
   [ARCHIVED] ◄── (无 setStatus writer)
```

| 迁移 | 触发 | 代码位置 | 守卫 | 裁决 |
|---|---|---|---|---|
| (新建)→DRAFT | defaultPrepareSave（继承 CrudBizModel） | — | status 默认未设 → CRUD 管道走 default | PASS |
| DRAFT→OPEN（owner doc §状态机声明 publish） | (无 publish BizMutation——BizModel 18 行 CRUD 桩) | — | — | **FAIL — P1-MA2-041**（dict 死状态 + 桩 BizModel） |
| OPEN→CLOSED（owner doc §状态机声明 close + 触发自动聚合） | (无 close BizMutation + 无聚合触发) | — | — | **FAIL — P1-MA2-041** |
| CLOSED→ARCHIVED（owner doc §状态机声明 archive） | (无 archive BizMutation) | — | — | **FAIL — P1-MA2-041** |

**调查终态**：ARCHIVED（owner doc §状态机声明——但 unreachable）。

---

## 3. 10 维度审查裁决

### 3.1 维度「状态定义」

**裁决：FAIL（4 项 dict 死状态，登记 4 项 P1）**

| 状态对象 | 状态 | 业务含义（owner doc 声明） | 死状态？ | 裁决 |
|---|---|---|---|---|
| 员工 employmentStatus | ACTIVE | 正常在职 | 否（招聘 hire + 调岗守卫） | PASS |
| 员工 employmentStatus | PROBATION | 试用期 | 否（招聘 hire owner doc §场景 E 入口 + HR 手工新建） | PASS |
| 员工 employmentStatus | RESIGNED | 已离职（终态，§场景D） | **是**（无 writer） | **FAIL — P1-MA2-039** |
| 员工 employmentStatus | TERMINATED | 已解雇（终态，§场景D） | **是** | **FAIL — P1-MA2-039** |
| 员工 employmentStatus | RETIRED | 已退休（终态，§场景E） | **是** | **FAIL — P1-MA2-039** |
| 合同 status | ACTIVE | 生效中 | 否 | PASS |
| 合同 status | EXPIRED | 已到期 | 否（expire 批量推进） | PASS |
| 合同 status | TERMINATED | 已解除 | 否（调岗副作用） | PASS |
| 合同 status | SUSPENDED | 已中止（停薪留职） | **是**（无 writer + owner doc 无独立合同状态机章节描述） | **FAIL — P1-MA2-040** |
| 招聘 status | OPEN/SCREENING/INTERVIEW/OFFERED/HIRED/REJECTED/CLOSED | 全部 7 态 | 否（各迁移可达） | PASS |
| 考核 status | DRAFT/SUBMITTED/COMPLETED | 全部 3 态 | 否 | PASS |
| 发展计划 status | DRAFT | 草稿 | **是**（generateDevelopmentPlan 直接 IN_PROGRESS） | **FAIL — P1-MA2-042** |
| 发展计划 status | IN_PROGRESS | 进行中 | 否（generateDevelopmentPlan） | PASS |
| 发展计划 status | COMPLETED | 已完成 | 否（completePlan） | PASS |
| 发展计划 status | CANCELLED | 已取消 | **是**（无 cancelPlan writer） | **FAIL — P1-MA2-042** |
| 发展计划项 status | NOT_STARTED/IN_PROGRESS/ACHIEVED | 全部 3 态 | 否 | PASS |
| 发展计划项 status | OVERDUE | 已逾期（被动标记） | **是**（无 setStatus writer + 无 job 计算逾期） | **FAIL — P1-MA2-042** |
| 调查 status | DRAFT | 草稿 | 否（CRUD 桩 + 新建 default） | PASS |
| 调查 status | OPEN/CLOSED/ARCHIVED | 全部 3 态 | **是**（BizModel 是 18 行 CRUD 桩） | **FAIL — P1-MA2-041** |

**核心反模式**：「动作作为状态」反模式未发现——所有状态都是合法业务等待点。「死状态」反模式确认 4 项（员工三态 + 合同 SUSPENDED + 调查三态 + 发展计划 DRAFT/CANCELLED + 计划项 OVERDUE = 共 4 项 P1 finding）。

### 3.2 维度「转换完整性」

**裁决：FAIL（员工/合同/调查/发展计划多组件转换缺失，已在 §2 状态图列出）**

逐组件转换矩阵分析（§2.1-2.7）：

- **员工**：5 态间应有 8 个迁移（hire→ACTIVE, hire→PROBATION, transfer 守卫, §场景D/E 4 个终态迁移 + PROBATION→ACTIVE 转正），**实现 2 个**（hire→ACTIVE + transfer 守卫只读），缺 6 个（P1-MA2-039）。无非法跳转（实现部分守卫完整）。
- **合同**：4 态间应有 6 个迁移（hire→ACTIVE, transfer→ACTIVE+TERMINATED, expire, renew, suspend+resume），**实现 5 个**，缺 suspend/resume（P1-MA2-040）。
- **招聘**：7 态间应有 9 个迁移（6 个状态机推进 + reject + close + hire 跨实体副作用），**全部实现**。close 无守卫是设计选择（非缺陷）。
- **考核**：3 态间应有 2 个迁移（submit + complete），**全部实现 + 守卫完整**。
- **发展计划**：4 态间应有 4 个迁移（generate→IN_PROGRESS + complete + cancel），**实现 2 个**，缺 DRAFT 入口 + cancelPlan（P1-MA2-042）。
- **发展计划项**：4 态间应有 4 个迁移（newItem→NOT_STARTED + start + achieve + 自动 OVERDUE），**实现 3 个**，缺自动 OVERDUE（P1-MA2-042）。
- **调查**：4 态间应有 3 个迁移（publish + close + archive + 自动聚合），**实现 0 个**（BizModel 桩）（P1-MA2-041）。

**外部触发器**：合同到期扫描由 `ErpHrContractExpiryJob` cron-gated 驱动（设计默认每日 01:00）；owner doc §场景D/E 员工离职联动触发器未实现（缺 resign BizMutation 入口）。

### 3.3 维度「终端状态和恢复」

**裁决：PASS（设计合理；不可达终态已并 P1-MA2-039/040/041/042）**

| 状态对象 | 终态 | 是否真终态 | 恢复路径 |
|---|---|---|---|
| 员工 | RESIGNED/TERMINATED/RETIRED | 是（owner doc §3 明示） | 再入职新建 ErpHrEmployee（设计裁定） |
| 合同 | TERMINATED | 是 | 新建合同（如招聘 hire 或调岗 resolveHandleContract 后继合同） |
| 合同 | EXPIRED | 否（renew 可回 ACTIVE） | renew 迁移已实现 |
| 招聘 | HIRED/CLOSED/REJECTED | 是 | HIRED 不可恢复（员工已入职）；CLOSED 行政管理不可恢复；REJECTED 候选人可重新申请新建 ErpHrRecruitment（owner doc §关键业务规则 #3） |
| 考核 | COMPLETED | 是 | 重新评估新建 ErpHrEmployeeAssessment（设计裁定——考核无 REJECT，HR 不驳回已提交评估） |
| 发展计划 | COMPLETED/CANCELLED | 是（CANCELLED 不可达） | — |
| 计划项 | ACHIEVED/OVERDUE | 是（OVERDUE 不可达） | — |
| 调查 | ARCHIVED | 是（不可达） | — |

**归档与活动记录可区分**：所有状态字段都是独立 dict 列（`employmentStatus`/`status`），归档（终态）vs 活动（非终态）可经 dict 过滤区分；`useLogicalDelete=true` 保证逻辑删除与状态终态分离（不混用）。

### 3.4 维度「异常路径」

**裁决：PASS（实现路径异常处理完整；未实现路径归 P1）**

| 异常路径 | 实现 | 裁决 |
|---|---|---|
| 调岗目标部门不存在 | `requireTargetDepartment:170-180` 抛 `ERR_TRANSFER_TARGET_DEPT_NOT_FOUND` | PASS |
| 调岗目标岗位不存在 / 不属于目标部门 | `requireTargetPosition:182-196` 抛 `ERR_TRANSFER_TARGET_POSITION_NOT_FOUND` | PASS |
| 调岗生效日期与已批准休假冲突 | `warnIfLeaveConflict:200-220` LOG.warn 不阻断（设计裁定，P2-MA2-050） | PASS（owner doc UC-HR-08 接受 warn 语义） |
| 招聘 hire 重复入职（同一候选人多次 hire） | `hire:108-118` 守卫 requireStatus OFFERED——已 HIRED 招聘不能再 hire；同一候选人在不同 ErpHrRecruitment 记录中分别 hire 会创建多个 ErpHrEmployee（owner doc 未声明幂等，但实际不同招聘记录代表不同职位申请，是合法场景） | PASS（设计可接受） |
| 合同 renew 已 TERMINATED | `renew:99-104` 守卫仅允许 ACTIVE/EXPIRED → 抛 `ERR_CONTRACT_ILLEGAL_STATUS_TRANSITION` | PASS |
| expireOverdueContracts 批量单失败 | `:81-87` try/catch 单失败隔离继续；@BizMutation 事务边界在**单次 updateEntity 调用**（非整个 for 循环）——单失败不阻断其他合同推进，已推进的不回滚 | PASS（设计裁定——批量 Job 容错） |
| 考核 submit 空 details | `submitAssessment:69-73` 抛 `ERR_ASSESSMENT_NO_DETAILS` | PASS |
| 考核 complete 空 details | `completeAssessment:89-93` 抛 `ERR_ASSESSMENT_NO_DETAILS` | PASS |
| 招聘 close 已 HIRED | `close:137-142` 无守卫——HIRED→CLOSED 合法入职后清理 | PASS（P2-MA2-048 owner doc 校验建议） |
| 招聘 reject 已终态 | `reject:125-129` 守卫拒 HIRED/CLOSED/REJECTED → `illegalTransition` | PASS |
| 计划项非法跳转（如 NOT_STARTED→ACHIEVED） | `assertPlanItemTransition:195-202` + `isValidPlanItemTransition:208-218` 抛 `ERR_DEV_PLAN_ILLEGAL_STATUS_TRANSITION` | PASS |
| 员工离职联动失败（§场景D 未实现） | (无 resign BizMutation，无异常路径) | **FAIL — P1-MA2-039** |

### 3.5 维度「可达性」

**裁决：FAIL（11 个 dict 项不可达，分 4 项 P1）**

| 死状态 | 数量 | 归属 finding |
|---|---|---|
| 员工 RESIGNED/TERMINATED/RETIRED | 3 | P1-MA2-039 |
| 合同 SUSPENDED | 1 | P1-MA2-040 |
| 调查 OPEN/CLOSED/ARCHIVED | 3 | P1-MA2-041 |
| 发展计划 DRAFT/CANCELLED + 计划项 OVERDUE | 4 | P1-MA2-042 |
| **合计** | **11** | **4 P1** |

无死循环（所有迁移守卫严格单向 + 终态无出边）。从开始状态（招聘 OPEN / 员工新建 ACTIVE/PROBATION / 合同新建 ACTIVE / 考核 DRAFT / 发展计划 IN_PROGRESS / 计划项 NOT_STARTED / 调查 DRAFT）可达实现路径的每个状态。

### 3.6 维度「角色和权限」

**裁决：PASS（状态机层面无角色漂移反模式；权限注解审计归 A6）**

| 迁移 | owner doc 角色 | 实现权限注解 | 裁决 |
|---|---|---|---|
| 招聘 hire | HR 专员 + 主管面试 + offer 审批 | @BizMutation（无显式 @Auth，归 A6.1 grep） | PASS（状态机层无漂移） |
| 调岗 transferEmployee | HR 专员 | @BizMutation（同上） | PASS |
| 考核 submit | 员工自评 | @BizMutation | PASS |
| 考核 complete | 主管 | @BizMutation | PASS |
| 合同 renew | HR | @BizMutation | PASS |
| 合同 expire | 系统 Job（cron-gated） | Job Bean | PASS |
| 调查（未实现） | HR（owner doc） | — | FAIL（P1-MA2-041——桩 BizModel） |

**危险操作**：(a) 员工 RESIGNED/TERMINATED/RETIRED 联动（owner doc §场景D 声明禁用账户——若 MR1 实现须触及 nop-auth 保护区域 + 独立 plan-audit + 人工确认）；(b) 招聘 close 无守卫（P2-MA2-048）；(c) 招聘 hire 跨实体创建 Employee+Contract（经 @BizMutation 事务回滚保证失败原子性）。**无角色漂移反模式**（每个迁移有明确 owner doc 角色）。

### 3.7 维度「外部依赖」

**裁决：PASS（已实现外部依赖经 I\*Biz Facade 跨域，未实现联动归 P1-MA2-039）**

| 外部依赖 | 入站/出站 | 实现 | 裁决 |
|---|---|---|---|
| 招聘 hire → Employee+Contract（同域写） | 出站 | `IErpHrEmployeeBiz.saveEntity` + `IErpHrEmploymentContractBiz.saveEntity`（同域合法） | PASS |
| 考核 complete → GapAnalysis 刷新（同域写） | 出站 | `IErpHrGapAnalysisBiz.refreshGapAnalysisWithLevels`（同域 + 直传 levels 避免跨事务可见性） | PASS |
| 调岗 → LeaveRequest 冲突检测（同域读） | 出站（只读） | `IErpHrLeaveRequestBiz.findCount`（同域只读） | PASS |
| 调岗 → Contract 处理（同域写） | 出站 | `IErpHrEmploymentContractBiz.updateEntity/saveEntity`（同域合法） | PASS |
| 合同到期 → SysNotification 派发（跨域 notify） | 出站 | `IErpSysNotificationBiz.notify`（跨域合法——notify 是跨域通知派发子系统） | PASS |
| 员工 RESIGNED/TERMINATED/RETIRED → LeaveRequest CANCELLED（§场景D 声明） | 出站 | **未实现** | **FAIL — P1-MA2-039**（联动维度） |
| 员工 RESIGNED/TERMINATED/RETIRED → Contract TERMINATED（§场景D 声明） | 出站 | **未实现** | **FAIL — P1-MA2-039** |
| 员工 RESIGNED/TERMINATED/RETIRED → UserAccount 禁用（§场景D 声明） | 出站 | **未实现**（触及 nop-auth 保护区域） | **FAIL — P1-MA2-039** |
| 调查 CLOSED → 自动聚合 ErpHrSurveyResult（owner doc §结果聚合声明） | 出站 | **未实现** | **FAIL — P1-MA2-041** |

外部步骤失败是否阻断状态迁移：所有迁移经 @BizMutation 事务回滚——失败抛异常 → 状态保持事务开始前值（强一致）。证据：`transferEmployee` 顺序 require → update → resolveHandleContract，任一步失败回滚员工 dept/position 更新。

### 3.8 维度「TODO/任务策略」

**裁决：FAIL（owner doc 声明 TODO 未实现，归 P1/P2）**

| 状态 | 是否产生 TODO | 实现 | 裁决 |
|---|---|---|---|
| 招聘 INTERVIEW（等待面试） | 是（owner doc：面试官 assigned） | 实现仅 `setInterviewerId/setInterviewDate`，无显式 TODO 派发（owner doc `recruitment.md §面试管理` Deferred——多实体 Interview 拆分） | PASS（Deferred） |
| 招聘 OFFERED（等待 offer 接受） | 是（owner doc：HR 跟进 offer） | 实现仅 setStatus，无 TODO 派发 | PASS（Deferred） |
| 考核 SUBMITTED（等待主管评审） | 是（owner doc：主管 assigned） | 实现仅 setStatus，无 TODO 派发（owner doc `competency-management.md §评估流程` Deferred） | PASS（Deferred） |
| 合同即将到期 | 是（owner doc：HR 续签提醒） | 实现 `ErpHrContractExpiryJob.scanExpiringContracts` + `IErpSysNotificationBiz.notify` 派发提醒 | PASS |
| **长期 PROBATION 未转正** | 是（owner doc §场景E：转正 TODO） | **未实现**（无 probationToRegular + 无 TODO 提醒） | **FAIL — P2-MA2-051**（静默下沉——员工长期 PROBATION 状态不被检测提醒） |
| **调查 OPEN（分发收集）** | 是（owner doc：员工催填 TODO） | **未实现**（BizModel 桩 + reminderDays 字段存在但无 writer） | **FAIL — P1-MA2-041**（reminderDays=3 字段已有，但无 job/迁移消费） |
| 员工 RESIGNED/TERMINATED/RETIRED 后续交接 TODO | 是（owner doc §场景D：HR 交接清单） | **未实现** | **FAIL — P1-MA2-039** |

### 3.9 维度「场景演练（最重要）」

**裁决：FAIL（场景 d/e/i 演练确认代码未实现，归 P1）**

| 场景 | 端到端演练结果 | 裁决 |
|---|---|---|
| (a) 招聘快乐路径 | OPEN→SCREENING（moveToScreening）→INTERVIEW（scheduleInterview）→OFFERED（makeOffer）→hire→HIRED + 创建 ErpHrEmployee（ACTIVE）+ ErpMployrmentContract（ACTIVE）+ employeeId 回写 | **PASS**（主路径完整 + 跨实体副作用经 @BizMutation 事务） |
| (b) 招聘 reject + close | INTERVIEW→REJECTED（reject 守卫允许非终态）+ 任意→CLOSED（close 无守卫）——HIRED→CLOSED 合法入职后清理 | **PASS**（无悬挂 + P2-MA2-048 owner doc 校验建议） |
| (c) 员工调岗 | ACTIVE→transferEmployee→dept/position/superior 更新 + 原 ACTIVE 合同→TERMINATED + 新建 ACTIVE 合同（如 shouldHandle=true） | **PASS**（@BizMutation 事务回滚覆盖） |
| (d) **员工离职** | owner doc §场景D：ACTIVE→RESIGNED + 联动（未完成 LeaveRequest→CANCELLED + ACTIVE 合同→TERMINATED + UserAccount 禁用）——**代码完全未实现**（无 resign BizMutation） | **FAIL — P1-MA2-039**（演练确认 owner doc 漂移 + 联动缺口） |
| (e) **员工退休/终止** | owner doc §场景E：同 (d) 但状态为 RETIRED/TERMINATED——**代码完全未实现** | **FAIL — P1-MA2-039** |
| (f) 合同到期 + renew | ACTIVE→expireOverdueContracts→EXPIRED（Job cron-gated 批量 + 单失败隔离）+ IErpSysNotificationBiz 派发预警 + EXPIRED→renew→ACTIVE | **PASS**（Job + 主流程完整 + 跨域通知派发） |
| (g) 考核全链 | DRAFT→submitAssessment（details 非空守卫）→SUBMITTED→completeAssessment（aggregateAndWriteBack + gapAnalysis 刷新）→COMPLETED + 自动发展计划生成 | **PASS**（跨实体刷新经 refreshGapAnalysisWithLevels 直传 levels） |
| (h) 发展计划项迁移 + OVERDUE | NOT_STARTED→updatePlanItemStatus→IN_PROGRESS（设 startDate）→ACHIEVED（设 endDate）；OVERDUE 应有 job 计算逾期——**无 job，OVERDUE 永不可达** | **FAIL — P1-MA2-042**（部分 PASS） |
| (i) **调查生命周期** | owner doc §状态机：DRAFT→publish→OPEN（分发收集 + 催填 TODO）→close→CLOSED（自动聚合 ErpHrSurveyResult）→archive→ARCHIVED——**BizModel 是 18 行 CRUD 桩，全部未实现** | **FAIL — P1-MA2-041**（演练确认桩 BizModel 缺口） |
| (j) 并发调岗同员工 | 无 employee.employmentStatus 字段级 @Version 保护——但 `ErpHrEmployee.versionProp="version"` ORM 整体透明乐观锁（detectable conflict，silent lost-update 风险已降级） | **PASS（降级）**——交接 A2.17 |

### 3.10 维度「与设计文档一致性」

**裁决：FAIL（多处 owner doc 漂移，登记 P1 + P2）**

| 漂移项 | owner doc | 实现 | 裁决 |
|---|---|---|---|
| 员工 §场景D/E 离职/终止/退休/转正迁移 + 联动 | `state-machine.md §场景D/E` 明确声明 | **完全未实现**（无 resign/terminate/retire/probationToRegular BizMutation + 无联动） | **FAIL — P1-MA2-039**（重大漂移——裁决方案 A 实现 / 方案 B owner doc 标注 Deferred + 删除 dict 项） |
| 合同 SUSPENDED 状态 | `state-machine.md` 无合同独立章节（无 SUSPENDED 描述） | dict 有 SUSPENDED + 无 writer | **FAIL — P1-MA2-040**（owner doc 缺失 + dict 死状态） |
| 调查 4 态生命周期 | `employee-survey.md §状态机` 明确声明 DRAFT→OPEN→CLOSED→ARCHIVED + 自动聚合 | BizModel 18 行 CRUD 桩 + 无迁移 | **FAIL — P1-MA2-041**（owner doc 声明但代码完全未实现） |
| 发展计划 DRAFT 入口 | `competency-management.md` 表声明 status=DRAFT/IN_PROGRESS/COMPLETED/CANCELLED | generateDevelopmentPlan 直接 IN_PROGRESS（DRAFT 不可达） | **FAIL — P1-MA2-042**（owner doc 表声明漂移） |
| 招聘 close 守卫 | `recruitment.md §关键业务规则` 未声明 close 守卫 | close 无守卫 | **FAIL — P2-MA2-048**（owner doc 文字校验建议） |
| 招聘多实体拆分 | `recruitment.md` 描述 Candidate/Interview/Offer/OnboardingChecklist 多实体 | 实现为扁平 ErpHrRecruitment | **FAIL — P2-MA2-049**（owner doc Deferred 标注缺失） |
| state-machine.md 章节 | 仅有 休假/员工雇佣状态/工时表/薪酬审批 章节 | 缺 招聘/合同/考核/发展计划/调查独立章节 | **FAIL — P2-MA2-047**（与 P2-MA2-037/043/045 同型） |
| 考核无 REJECT | `competency-management.md` 表无 REJECT | 实现无 REJECT（设计可接受） | PASS（设计裁定——重新评估新建记录） |
| P2-MA1-020 orphan dict salary-approval-status | `payroll.md §审批状态标准化` 明示已废弃（拆为 wf/approve-status） | column 已正确引用 wf/approve-status；orphan dict 仅在 i18n + javadoc 残留 | PASS（已登记 P2-MA1-020，本审计复核对员工/组织状态机无影响——salary 侧归 A2.7b） |

---

## 4. MA1 finding 运行时影响复核表

| Finding | 原登记 | 本审计状态机角度复核 | 升级？ |
|---|---|---|---|
| `P2-MA1-020` orphan dict salary-approval-status | watch-only MR1（hr 平台合规） | orphan dict 是 MA1 平台合规发现，`ErpHrSalary.approveStatus`（ORM:736）实际引用 `wf/approve-status`（4 态）——本审计复核 salary-approval-status 6 态对员工/组织状态机**无影响**（员工/合同/招聘/考核/发展计划/调查均不引用 salary-approval-status）；salary 侧归 A2.7b 复核 | **不升级**（维持 P2 watch-only MR1） |
| `P1-MA1-022` 跨域只读 IDaoProvider | todo MR1（9 域合并） | hr 域 `ErpHrReportBizModel.java:268` `daoFor(ErpMdPartner)` 是 dashboard 聚合（master-data 只读）——本审计复核对员工/组织状态机**无影响**（状态迁移路径不调用 report 聚合） | **不升级**（维持 P1 todo MR1） |

---

## 5. P0/P1/P2 finding 详细

### P0

无。

### P1-MA2-039 — 员工 employmentStatus RESIGNED/TERMINATED/RETIRED 三态死状态 + owner doc §场景D/E 离职/终止/退休/转正迁移 + 联动完全未实现

- **严重性**：P1（major）
- **位置**：`module-hr/model/app-erp-hr.orm.xml:11-13`（dict `erp-hr/employment-status` 含 RESIGNED/TERMINATED/RETIRED）+ `ErpHrConstants.java:215-217`（常量定义零 setStatus 使用）+ `ErpHrEmployeeBizModel.java`（341 行无 resign/terminate/retire/probationToRegular BizMutation）+ `docs/design/human-resource/state-machine.md §场景D/E`
- **问题**：owner doc `state-machine.md §场景D/E` 明确声明「ACTIVE→RESIGNED（主动离职）+ ACTIVE→TERMINATED（解雇）+ ACTIVE→RETIRED（退休）+ PROBATION→ACTIVE（转正）」迁移 + 联动（未完成 LeaveRequest→CANCELLED + ACTIVE 合同→TERMINATED + UserAccount 禁用），**代码完全未实现**。`setEmploymentStatus` 在生产代码中仅 `ErpHrRecruitmentBizModel.java:156`（招聘 hire 创建 ACTIVE），无任何 setStatus(RESIGNED/TERMINATED/RETIRED) 调用。三态 dict 项不可达（dict 死状态）。按 finance A2.5a P1-MA2-031 + mfg A2.6a P1-MA2-035 + A2.6b P1-MA2-036 同型裁决。
- **重要性原因**：dict 项不可达致查询筛选语义混乱（UI 按 dict 渲染状态选项包含永不到达的状态）+ owner doc 声明的离职/退休/转正迁移 + 联动完全缺失（员工离职需 HR 手工 DB update 改 employmentStatus + 手工取消 LeaveRequest + 手工终止合同 + 手工禁用账户——违反「单一状态迁移触发多联动」契约）。**不破坏主路径**——在职生命周期（ACTIVE/PROBATION）由招聘 hire + 调岗 transferEmployee 完整覆盖；工资核算 `ErpHrSalaryBizModel.java:236` 守卫 ACTIVE/PROBATION，不会因终态死状态产生悬挂数据。
- **处置**：MR1 裁决——方案 A（推荐）实现 resign/terminate/retire/probationToRegular BizMutation + 联动（取消 LeaveRequest/终止合同/禁用账户）+ owner doc 同步实际实现；方案 B owner doc `state-machine.md §场景D/E` 标注「Deferred——离职经手工 DB update + 跨域联动经 HR 流程处理」+ 删除 dict 三项 + 删除常量定义。方案 A 触及 `nop-auth` UserAccount 保护区域（禁用账户），须有 owner doc 描述预期行为 + 该修复子切片的独立审计 + 人工确认。

### P1-MA2-040 — 合同 status SUSPENDED dict 死状态 + owner doc 无合同独立章节

- **严重性**：P1（major）
- **位置**：`module-hr/model/app-erp-hr.orm.xml:39`（dict `erp-hr/contract-status` 含 SUSPENDED）+ `ErpHrConstants.java:223` + `_ErpHrDaoConstants.java:109`（常量定义零使用）+ `ErpHrEmploymentContractBizModel.java`（111 行无 suspend/resume BizMutation）+ `docs/design/human-resource/state-machine.md`（无合同独立章节，仅一行提及「停薪留职」语义）
- **问题**：dict `erp-hr/contract-status` 含 SUSPENDED 但**无任何 setStatus(SUSPENDED) 调用**——全 `src/main` grep `CONTRACT_STATUS_SUSPENDED` 仅常量定义零使用。owner doc `state-machine.md` 无合同独立章节描述 SUSPENDED 迁移（仅一行提及「停薪留职」语义未定义进出迁移）。按 finance A2.5a P1-MA2-031 + mfg A2.6a P1-MA2-035 同型裁决。
- **重要性原因**：SUSPENDED dict 项不可达致查询筛选语义混乱 + owner doc 无独立章节描述合同状态机（散落在员工雇佣状态章节一行提及）。**不破坏合同主路径**（ACTIVE/EXPIRED/TERMINATED 经 expire/renew/transferEmployee 副作用完整覆盖合同生命周期）。
- **处置**：MR1 裁决——方案 A 实现 suspend/resume BizMutation（停薪留职业务场景 + owner doc `state-machine.md` 新增合同独立章节）+ 同步实现员工 employmentStatus 联动（员工 SUSPENDED 联动？须 owner doc 描述）；方案 B owner doc 标注「SUSPENDED 为预留状态，本期不实现停薪留职」+ 删除 dict 项 + 删除常量。

### P1-MA2-041 — 员工调查 OPEN/CLOSED/ARCHIVED 三态死状态 + ErpHrSurveyBizModel 桩 + owner doc §状态机声明漂移

- **严重性**：P1（major）
- **位置**：`module-hr/model/app-erp-hr.orm.xml:185-187`（dict `erp-hr/survey-status` 含 OPEN/CLOSED/ARCHIVED）+ `_ErpHrDaoConstants.java:609/614/619`（常量定义零使用）+ `ErpHrSurveyBizModel.java`（**18 行 CRUD 桩，无任何状态迁移方法**）+ `docs/design/human-resource/employee-survey.md §状态机`
- **问题**：owner doc `employee-survey.md §状态机` 明确声明「DRAFT（编辑中）→ OPEN（发布，可填写）→ CLOSED（截止）→ ARCHIVED（归档）；OPEN 可直接→ CLOSED」迁移 + CLOSED 时触发自动聚合 → ErpHrSurveyResult。`ErpHrSurveyBizModel` 是 18 行 CRUD 桩（`extends CrudBizModel<ErpHrSurvey>` 无任何方法），**全部状态迁移缺失**。`SURVEY_STATUS_OPEN/CLOSED/ARCHIVED` 常量定义于 `_ErpHrDaoConstants.java:609/614/619` 零使用。三态 dict 项不可达（dict 死状态）。与 mfg A2.6b 预测 CONSUMED 死状态（P1-MA2-036）+ finance A2.5a DRAFT→CANCELLED（P1-MA2-031）同型。
- **重要性原因**：调查门户/分发是 owner doc 设计目标能力，但本期未实现状态机 + 自动聚合。`reminderDays=3` 字段已存在但无 job 消费。**不破坏主路径**——CRUD 完整可用（DRAFT 创建/查询/更新/逻辑删除），调查门户/分发是 owner doc Deferred 能力（employee-survey.md §边界 明示「不负责：通用调研平台」），缺失状态机不产生悬挂数据（调查停留在 DRAFT 是合法初始态）。
- **处置**：MR1 裁决——方案 A 实现 publish/close/archive BizMutation + 自动聚合 ErpHrSurveyResult + 催填 TODO（消费 reminderDays）；方案 B owner doc `employee-survey.md §状态机` 标注「本期 BizModel 为 CRUD 桩，状态机 Deferred——调查门户上线时实现」+ 删除 dict 三项 + 删除常量。

### P1-MA2-042 — 发展计划 DRAFT/CANCELLED + 计划项 OVERDUE dict 死状态 + 无 cancelPlan + 无 OVERDUE 自动 job

- **严重性**：P1（major）
- **位置**：`module-hr/model/app-erp-hr.orm.xml:227/230`（dict `erp-hr/devplan-status` 含 DRAFT/CANCELLED）+ `orm.xml:236`（dict `erp-hr/plan-item-status` 含 OVERDUE）+ `ErpHrConstants.java:185/188/194`（常量定义零 setStatus 使用）+ `ErpHrDevelopmentPlanBizModel.java`（224 行：generateDevelopmentPlan:82 直接 IN_PROGRESS + 无 cancelPlan + isValidPlanItemTransition:215 允许 OVERDUE 但无 writer）+ `docs/design/human-resource/competency-management.md`（表声明 4 态但无 DRAFT/CANCELLED/OVERDUE 可达性注记）
- **问题**：(a) `generateDevelopmentPlan:82` `plan.setStatus(DEV_PLAN_STATUS_IN_PROGRESS)`——**生成即 IN_PROGRESS**，DRAFT dict 项不可达；(b) `DEV_PLAN_STATUS_CANCELLED` 常量定义零使用，`completePlan:115-129` 仅 DRAFT/IN_PROGRESS→COMPLETED，无 cancelPlan 迁移——CANCELLED dict 项不可达；(c) `PLAN_ITEM_STATUS_OVERDUE` 常量仅在 `isValidPlanItemTransition:215` 作 target 守卫引用，**无任何 setStatus(OVERDUE) 主动写入**（是设计语义的被动逾期标记，但无定时 job 计算逾期，实际永不可达）——OVERDUE dict 项不可达。共 4 dict 项死状态。
- **重要性原因**：dict 项不可达致查询筛选语义混乱 + OVERDUE 被动逾期标记缺失自动 job 意味着逾期计划项永远不会被标记（HR 无法识别需介入的员工发展）。**不破坏发展计划主路径**（IN_PROGRESS→COMPLETED + 计划项 NOT_STARTED→IN_PROGRESS→ACHIEVED 完整覆盖员工发展闭环）。
- **处置**：MR1 裁决——方案 A 实现 cancelPlan BizMutation（IN_PROGRESS→CANCELLED 守卫）+ 定时 job 计算逾期计划项（endDate<now 且未 ACHIEVED → setStatus(OVERDUE)）+ owner doc 注记 DRAFT 是手工创建入口（增加 createPlan 显式 DRAFT 状态）；方案 B owner doc `competency-management.md` 标注「DRAFT 是预留状态、CANCELLED 走 useLogicalDelete、OVERDUE 是设计等待点（HR 手工标记或归 successor）」+ 删除 DRAFT/CANCELLED dict 项 + OVERDUE 保留为合法 target 但 owner doc 注记「需 HR 手工触发」。

### P2-MA2-047 — state-machine.md 缺招聘/合同/考核/发展计划/调查独立章节

- **严重性**：P2（watch-only，文档组织问题）
- **位置**：`docs/design/human-resource/state-machine.md`（适用对象一休假 + 二员工雇佣状态 + 三工时表 + 四薪酬审批，无招聘/合同/考核/发展计划/调查独立章节）
- **问题**：5 个状态机组件（招聘 7 态 / 合同 4 态 / 考核 3 态 / 发展计划 4 态 / 发展计划项 4 态 / 调查 4 态）owner doc 散落在 `recruitment.md §状态机` + `competency-management.md §实体设计 status 列` + `employee-survey.md §状态机`，无 `state-machine.md` 独立章节。与 P2-MA2-037（finance state-machine.md 缺 AR/AP+坏账独立章节）+ P2-MA2-043（mfg 领料 owner doc 无独立章节）+ P2-MA2-045（mfg state-machine.md 无 MRP/预测独立章节）同型。
- **处置**：watch-only，MR1 顺手——方案 A（推荐）`state-machine.md` 新增「适用对象五：招聘管线状态机」+「适用对象六：劳动合同状态机」+「适用对象七：员工考核状态机」+「适用对象八：发展计划与计划项状态机」+「适用对象九：员工调查状态机」章节（本审计 §2 状态图可直接采用）；方案 B 交叉链接到各 owner doc。

### P2-MA2-048 — 招聘 close 无守卫（任意 status 可 CLOSE 含已 HIRED）

- **严重性**：P2（watch-only，「承诺但无证据」控制点）
- **位置**：`ErpHrRecruitmentBizModel.close:137-142`
- **问题**：`close` 无任何 status 守卫——任意状态（含 OPEN/SCREENING/INTERVIEW/OFFERED/HIRED）可被 CLOSED。与 `reject:125-129` 守卫（拒 HIRED/CLOSED/REJECTED）不对称。**不破坏状态机**：HIRED→CLOSED 是合法入职后清理（employeeId 持久化保留），OPEN→CLOSED 是合法招聘取消。但 owner doc `recruitment.md §关键业务规则` 未显式声明 close 守卫，审查者无法判断是否应限制。
- **处置**：watch-only，MR1 裁决——方案 A 补 close 守卫（仅允许非 HIRED 状态关闭，HIRED 招聘记录不可关闭以保留入职历史可读性）；方案 B owner doc `recruitment.md §关键业务规则` 补注「close 无守卫——任意状态可行政关闭，HIRED→CLOSED 是合法入职后清理」。

### P2-MA2-049 — recruitment.md 多实体 Deferred 未注记（Candidate/Interview/Offer/OnboardingChecklist）

- **严重性**：P2（watch-only，文档完整性问题）
- **位置**：`docs/design/human-resource/recruitment.md`（§三/§四/§五/§六 描述 ErpHrCandidate/ErpHrInterview/ErpHrInterviewScorecard/ErpHrOffer/ErpHrOfferTemplate/ErpHrOnboardingChecklist 多实体设计）+ `ErpHrRecruitment` 实体（扁平单实体，无 Candidate/Interview/Offer 拆分）
- **问题**：owner doc `recruitment.md` 详细描述招聘需求/计划/职位发布/候选人/面试/Offer/入职清单多实体设计（参考 AureusERP `recruitments/` + Odoo `hr_recruitment`），但实现是扁平 `ErpHrRecruitment` 单实体（候选人姓名/面试官/面试日期/Offer 薪资/入职日期 等字段全部塞入一张表）。owner doc 未显式标注 Deferred——审查者期望多实体设计但实际是简化扁平。
- **处置**：watch-only，MR1 顺手——owner doc `recruitment.md §参考` 或新增「§实现注记」标注「本期实现为扁平 ErpHrRecruitment 单实体，Candidate/Interview/Offer/OnboardingChecklist 多实体拆分归 successor」。

### P2-MA2-050 — 调岗请假冲突 warnIfLeaveConflict 非阻断（owner doc §异常路径校验）

- **严重性**：P2（watch-only，设计裁定校验）
- **位置**：`ErpHrEmployeeBizModel.warnIfLeaveConflict:200-220`
- **问题**：`warnIfLeaveConflict` 检测调岗生效日与已批准休假重叠仅 `LOG.warn(...)` 不抛异常。owner doc `use-cases.md UC-HR-08` 设计接受「告警不阻塞，由 HR 决策」——调岗可发生在休假结束后或 HR 决定中断休假。但 `state-machine.md §4 异常路径` 「已提交后员工离职 → 自动取消未完成的 LeaveRequest」联动语义未完整文档化（与 P1-MA2-039 联动维度相关）。
- **处置**：watch-only，MR1 顺手——owner doc `state-machine.md §4 异常路径` 补注「调岗生效日期与已批准休假冲突 → warn 不阻断（UC-HR-08 设计），由 HR 决策是否调整生效日期或中断休假」。

### P2-MA2-051 — 长期 PROBATION 未转正无 TODO 提醒（静默下沉）

- **严重性**：P2（watch-only，UX 缺陷非数据缺陷）
- **位置**：`ErpHrEmployee.probationEndDate`（字段已存在）+ 无 probationToRegular BizMigration + 无定时 Job 检测长期 PROBATION
- **问题**：员工长期停留在 PROBATION 状态（probationEndDate 已过）无 TODO 提醒 HR 评估转正——静默下沉。owner doc `state-machine.md §场景E` 声明试用期转正（PROBATION→ACTIVE + 设置 regularDate），代码完全未实现（同 P1-MA2-039）。但**非 P1**——试用期数据完整性由 `probationEndDate` 字段承载（可由 HR 主动查询），缺失自动 TODO 是 UX 缺陷非数据缺陷。
- **处置**：watch-only，与 P1-MA2-039 联动——MR1 实现 probationToRegular BizMutation 时一并补定时 Job 检测 probationEndDate 已过的 PROBATION 员工并派发转正评估 TODO（经 `IErpSysNotificationBiz`）。

---

## 6. 并发敏感点（交接 A2.17）

| 敏感点 | 位置 | 现状 | 交接 |
|---|---|---|---|
| 并发调岗同员工 | `ErpHrEmployeeBizModel.transferEmployee` | `ErpHrEmployee.versionProp="version"` ORM 透明乐观锁 detectable conflict（silent lost-update 风险已降级） | A2.17 |
| 批量 expire 竞态 | `ErpHrEmploymentContractBizModel.expireOverdueContracts:80-89` | 单失败隔离 try/catch；@BizMutation 事务边界在单次 updateEntity（非整个 for 循环）——并发 Job 触发可能重复推进同合同（但 setStatus(EXPIRED) 幂等，无数据错误） | A2.17 |
| 招聘 hire 并发（同候选人多次 hire） | `ErpHrRecruitmentBizModel.hire:104-119` | 守卫 requireStatus OFFERED——并发 hire 第二次会因 status 已变 HIRED 抛 illegalTransition（detectable） | A2.17 |
| 考核 submit/complete 并发（同 assessment 双调用） | `ErpHrEmployeeAssessmentBizModel.submitAssessment/completeAssessment` | `ErpHrEmployeeAssessment.versionProp="version"` ORM 透明乐观锁；状态守卫（DRAFT/SUBMITTED）保证幂等拒绝 | A2.17 |
| 发展计划项 updatePlanItemStatus 并发（同计划项双更新） | `ErpHrDevelopmentPlanBizModel.updatePlanItemStatus` | `ErpHrDevelopmentPlanItem.versionProp="version"` ORM 透明乐观锁；状态守卫保证非法跳转拒绝 | A2.17 |

> **重要降级**：本审计发现 `ErpHrEmployee`/`ErpHrEmploymentContract`/`ErpHrRecruitment`/`ErpHrEmployeeAssessment`/`ErpHrDevelopmentPlan`/`ErpHrDevelopmentPlanItem`/`ErpHrSurvey` 全部声明 `versionProp="version"`，Nop ORM 透明乐观锁将「silent lost-update 风险」降级为「detectable optimistic conflict」。A2.17 系统性并发审计时应纳入此事实。

---

## 7. 残留风险

1. **员工 employmentStatus RESIGNED/TERMINATED/RETIRED 三态死状态 + §场景D/E 联动未实现**（**P1-MA2-039**，MR1）——离职/退休/转正经手工 DB update 是已知简化，MR1 实现或 owner doc 标注 Deferred；
2. **合同 SUSPENDED dict 死状态**（**P1-MA2-040**，MR1）——停薪留职业务场景未实现；
3. **调查 OPEN/CLOSED/ARCHIVED 三态死状态 + 桩 BizModel**（**P1-MA2-041**，MR1）——调查门户/分发是 owner doc Deferred；
4. **发展计划 DRAFT/CANCELLED + 计划项 OVERDUE dict 死状态**（**P1-MA2-042**，MR1）——无 cancelPlan + 无 OVERDUE 自动 job；
5. **state-machine.md 缺 5 组件独立章节**（**P2-MA2-047**）；
6. **招聘 close 无守卫**（**P2-MA2-048**）；
7. **recruitment.md 多实体 Deferred 未注记**（**P2-MA2-049**）；
8. **调岗请假冲突 warn 非阻断**（**P2-MA2-050**）；
9. **长期 PROBATION 未转正无 TODO 提醒**（**P2-MA2-051**，与 P1-MA2-039 联动）；
10. **并发敏感点 5 处交接 A2.17**（含 @Version 透明乐观锁降级重要事实——7 个 hr 状态机实体全部声明 versionProp）；
11. **A2.7b hr 考勤与工资状态机审查未执行**（S 级拆分 2/2 后续执行——工资核算依赖员工 employmentStatus=ACTIVE/PROBATION，本审计已确认此前置）。

---

## 8. 审计范围声明

本审计严格限定 A2.7a scope = hr 员工与组织类状态机（员工雇佣 + 合同 + 招聘 + 考核 + 发展计划 + 发展计划项 + 调查七组件）。以下明确排除（Non-Goal）：

- **A2.7b hr 考勤与工资类状态机**（S 级拆分 2/2，后续执行）——本审计只确认员工 employmentStatus=ACTIVE/PROBATION 是工资 runPayroll 的前置（`ErpHrSalaryBizModel.java:236` 守卫）；
- **A2.1/A2.2 P2P/O2C 端到端编排正确性**（done）——本审计不涉及采购/销售链路；
- **A2.5 finance 状态机**（done）——本审计只确认 hr 过账（工资）经 finance I*Biz（归 A2.7b）；
- **A4.4 hr 代码质量审计**——BizModel/Calculator 代码质量（异常处理/N+1/索引/辅助方法）系统性审查归 A4.4；本审计只做状态机业务正确性审查；
- **A2.17 并发与乐观锁**——并发调岗同员工 / 批量 expire 竞态 / 招聘 hire 并发归 A2.17；本审计只标注观察到的并发敏感点；
- **owner doc 已裁定的 Deferred 偏离本身是否应实现**（调查门户分发 / 考核 360 深化 / 发展计划自动跟踪 / 招聘多实体拆分 / 员工离职联动自动化 / 合同停薪留职）——这些是 owner doc Deferred/Non-Goal，本审计只确认其在状态机上不引入悬挂（死状态归状态定义清晰性维度裁决为 P1 dict 死状态清理或 owner doc 标注 Deferred 而非实现）；
- **A6 权限注解审计**——@BizMutation/@BizQuery 显式 @Auth 注解完整性归 A6；本审计只做状态机角色绑定语义层面裁决（无角色漂移反模式）；
- **P2-MA1-020 orphan dict salary-approval-status 是否应清理**——MA1 平台合规已登记，本审计只复核对员工/组织状态机无影响（salary 侧归 A2.7b）。

---

## 9. 结论

hr 员工与组织七组件状态机（员工 5 态 + 合同 4 态 + 招聘 7 态 + 考核 3 态 + 发展计划 4 态 + 发展计划项 4 态 + 调查 4 态）核心契约经实仓逐项证据确认：在职状态机 + 招聘状态机 + 考核状态机 + 发展计划状态机 + 发展计划项状态机的**主路径状态迁移守卫齐全**（requireTransferableEmployee/requireStatus/assertPlanItemTransition/Objects.equals 前置校验）、事务边界清晰（@BizMutation 自动事务）、招聘 hire 跨实体副作用经事务回滚保证失败原子性、考核 completeAssessment 跨实体刷新 gapAnalysis 经直传 levels 避免跨事务可见性、合同到期经 cron-gated Job + 单失败隔离 + 跨域通知派发。**零 P0**（六个候选 P0 经证据证伪或降级：(a) 员工离职/终止/退休 employmentStatus 迁移完全缺失——按 finance P1-MA2-031 + mfg P1-MA2-035/036 同型裁决为 P1 dict 死状态 + 实现缺口而非 P0，因不破坏已实现主路径 / (b) 合同 SUSPENDED 死状态——P1，不破坏合同主路径 / (c) 调查三态死状态——P1，CRUD 桩不破坏 DRAFT 创建 / (d) 发展计划 DRAFT/CANCELLED + 计划项 OVERDUE 死状态——P1，不破坏发展计划主路径 / (e) 招聘 close 无守卫——证伪 P0，HIRED→CLOSED 是合法入职后清理无数据破坏，登记 P2 watch-only / (f) 调岗请假冲突 warn 非阻断——设计裁定非缺陷，登记 P2 watch-only）；**4 项新 P1**（P1-MA2-039 员工 employmentStatus RESIGNED/TERMINATED/RETIRED 三态死状态 + owner doc §场景D/E 联动完全未实现 / P1-MA2-040 合同 SUSPENDED dict 死状态 + owner doc 无合同独立章节 / P1-MA2-041 调查 OPEN/CLOSED/ARCHIVED 三态死状态 + 桩 BizModel + owner doc §状态机声明漂移 / P1-MA2-042 发展计划 DRAFT/CANCELLED + 计划项 OVERDUE dict 死状态 + 无 cancelPlan + 无 OVERDUE 自动 job——全部按 finance P1-MA2-031 + mfg P1-MA2-035/036 同型裁决）；**5 项新 P2** watch-only（P2-MA2-047 state-machine.md 缺 5 组件独立章节 / P2-MA2-048 招聘 close 无守卫 / P2-MA2-049 recruitment.md 多实体 Deferred 未注记 / P2-MA2-050 调岗请假冲突 warn 非阻断 / P2-MA2-051 长期 PROBATION 未转正无 TODO 提醒）；MA1 finding（P2-MA1-020 + P1-MA1-022）运行时复核**无升级**（仅治理缺陷 / 不影响员工组织状态机运行时）；并发敏感点 5 处交接 A2.17（含 @Version 透明乐观锁降级重要事实——7 个 hr 状态机实体全部声明 versionProp）。

**Verdict: pass**。A2.7a 完成，hr 员工与组织状态机系统性审查 done。A2.7b hr 考勤与工资状态机（S 级拆分 2/2）后续执行。
