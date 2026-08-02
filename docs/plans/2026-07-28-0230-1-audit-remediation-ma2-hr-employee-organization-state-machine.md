# 2026-07-28-0230-1-audit-remediation-ma2-hr-employee-organization-state-machine MA2 hr 状态机审查 — 员工与组织（A2.7a）

> Plan Status: completed
> Mission: audit-remediation
> Work Item: A2.7a hr 状态机审查 — 员工与组织（S 级拆分 1/2）
> Last Reviewed: 2026-07-28
> Source: `docs/backlog/audit-remediation-roadmap.md` Milestone MA2（工作项 A2.7a）
> Related: `docs/plans/2026-07-28-0230-2-audit-remediation-ma2-hr-attendance-payroll-state-machine.md`（A2.7b 考勤与工资，S 级拆分 2/2，后续执行——工资核算依赖员工 employmentStatus=ACTIVE/PROBATION）；`docs/plans/2026-07-28-0109-1-audit-remediation-ma2-mfg-work-order-jobcard-state-machine.md`+`2026-07-28-0109-2-...-mrp-bom-...`（A2.6a/b manufacturing 状态机审查范式，全 done——dict 死状态 + owner doc 漂移同型裁决 P1-MA2-035/036）；`docs/plans/2026-07-27-2211-2-audit-remediation-ma2-finance-posting-voucher-state-machine.md`（A2.5a finance 凭证状态机范式，DRAFT→CANCELLED 不可达同型裁决 P1-MA2-031）；`docs/skills/state-machine-business-review-prompt.md`（审计方法）；`docs/design/human-resource/state-machine.md`（员工 employmentStatus + 合同状态机 + §场景D/E 离职/终止/退休 + §实现偏离补注）+`recruitment.md`+`competency-management.md`（owner doc）
> Audit: required

## Current Baseline

hr（人力资源）域 S 级状态机审查拆分 2 片：**A2.7a = 员工与组织类状态机**（员工雇佣生命周期 / 劳动合同 / 招聘管线 / 考核与发展）；**A2.7b = 考勤与工资类状态机**（请假 / 考勤 / 排班 / 工资 / 仿真）。本审计 A2.7a 聚焦**人员生命周期**——owner doc `state-machine.md` 员工 employmentStatus + 合同 + `recruitment.md` 招聘管线 + `competency-management.md` 考核/发展。

实时仓库已落地的员工与组织状态机实现（逐项核实，路径 `module-hr/`）：

- **员工雇佣状态机**（`ErpHrEmployee`，ORM `app-erp-hr.orm.xml:265-340`）：列 `employmentStatus` dict `erp-hr/employment-status`（**5 态**：ACTIVE/PROBATION/RESIGNED/TERMINATED/RETIRED，dict `orm.xml:8-14`）。
  - 迁移实现（`ErpHrEmployeeBizModel.java` 341 行）：**仅 `transferEmployee:88-116` 读 employmentStatus（守卫 `isTransferable:165-168` 仅 ACTIVE/PROBATION 可调岗，更新 dept/position/superior）**。合同处理 `resolveHandleContract:227-260`（ACTIVE→TERMINATED + 新建 ACTIVE 合同 L293）。
  - **⚠️ 重大缺口**：**无 `resign`/`terminate`/`retire`/`probationToRegular` BizMutation 方法**——owner doc `state-machine.md §场景D/E` 描述的离职/终止/退休/转正迁移（含自动取消请假、终止合同、禁用账户联动）**无对应代码实现**。`RESIGNED`/`TERMINATED`/`RETIRED` 三态**无任何 setStatus writer**（grep 全 `src/main` 无写入路径；常量 `EMPLOYMENT_RESIGNED/TERMINATED/RETIRED` 定义于 `ErpHrConstants.java:215-217`，仅被 `ErpHrEmployeeBizModel.nonTransferableStatuses():336-338` 作只读调岗守卫引用，无 setStatus 调用）→ dict 三项死状态（候选可达性缺陷，同 finance A2.5a P1-MA2-031 + mfg A2.6a P1-MA2-035 同型）。
- **劳动合同状态机**（`ErpHrEmploymentContract`，ORM `orm.xml:419-478`）：列 `status` dict `erp-hr/contract-status`（**4 态**：ACTIVE/EXPIRED/TERMINATED/SUSPENDED，dict `orm.xml:35-40`）。
  - 迁移实现（`ErpHrEmploymentContractBizModel.java` 111 行）：`expireOverdueContracts:72-90`（ACTIVE→EXPIRED 批量，endDate<now，单失败隔离 L81-87）/ `renew:93-109`（ACTIVE 或 EXPIRED→ACTIVE，守卫 L99-104 仅允许此两态）。ACTIVE→TERMINATED 仅经 `ErpHrEmployeeBizModel.resolveHandleContract:293` 调岗副作用。
  - **`SUSPENDED` 状态无 writer**——grep 全 `src/main` `CONTRACT_STATUS_SUSPENDED` 仅常量定义零使用 → dict 项死状态（候选可达性缺陷）。定时任务 `ErpHrContractExpiryJob`（111 行）驱动 `scanExpiringContracts:59` + `expireOverdueContracts`，cron-gated（L55-59）。
- **招聘管线状态机**（`ErpHrRecruitment`，ORM `orm.xml:785-851`）：列 `status` dict `erp-hr/recruitment-status`（**7 态**：OPEN/SCREENING/INTERVIEW/OFFERED/HIRED/REJECTED/CLOSED，dict `orm.xml:63-71`）。
  - 迁移实现（`ErpHrRecruitmentBizModel.java` 225 行）：`moveToScreening:66-73`(OPEN→SCREENING) / `scheduleInterview:76-88`(→INTERVIEW) / `makeOffer:91-101`(→OFFERED) / `hire:104-119`(→HIRED + 创建 Employee+Contract 跨实体) / `reject:122-133`(非终态→REJECTED，守卫 L125-129 拒 HIRED/CLOSED/REJECTED) / `close:136-142`(→CLOSED，**无守卫——任意 status**)。共享守卫 `requireStatus:212-216`（期望 OPEN，否则 `illegalTransition:218`）。
  - hire 跨实体副作用：`createEmployeeFromRecruitment:146`（employmentStatus=ACTIVE）+ `createContractForNewEmployee:175`（status=ACTIVE）。
- **考核状态机**（`ErpHrEmployeeAssessment`，ORM `orm.xml:1614-1657`）：列 `status` dict `erp-hr/assessment-status`（**3 态**：DRAFT/SUBMITTED/COMPLETED，dict `orm.xml:215-219`）。
  - 迁移实现（`ErpHrEmployeeAssessmentBizModel.java` 158 行）：`submitAssessment:61-77`(DRAFT→SUBMITTED，守卫 L151-156 + details 非空 L69-73) / `completeAssessment:80-104`(SUBMITTED→COMPLETED + `aggregateAndWriteBack:111-139` + `gapAnalysisBiz.refreshGapAnalysisWithLevels:101` 跨实体副作用)。
- **发展计划状态机**（`ErpHrDevelopmentPlan`，ORM `orm.xml:1727-1764`）：列 `status` dict `erp-hr/devplan-status`（**4 态**：DRAFT/IN_PROGRESS/COMPLETED/CANCELLED，dict `orm.xml:226-231`）。
  - 迁移实现（`ErpHrDevelopmentPlanBizModel.java` 224 行）：`generateDevelopmentPlan:68-91`(从 CRITICAL/MODERATE gap 创建 plan IN_PROGRESS L82) / `completePlan:115-129`(DRAFT/IN_PROGRESS→COMPLETED，守卫 L119-125)。**DRAFT→CANCELLED 无迁移**（CANCELLED dict 项——候选死状态，但 DRAFT 是 generatePlan 后初始即 IN_PROGRESS，DRAFT 可能不可达）。
- **发展计划项状态机**（`ErpHrDevelopmentPlanItem`，ORM `orm.xml:1767-1812`）：列 `status` dict `erp-hr/plan-item-status`（**4 态**：NOT_STARTED/IN_PROGRESS/ACHIEVED/OVERDUE，dict `orm.xml:232-237`）。
  - 迁移实现（`ErpHrDevelopmentPlanBizModel.updatePlanItemStatus:94-112`）：守卫 `assertPlanItemTransition:195-202` → `isValidPlanItemTransition:208-218`（NOT_STARTED→IN_PROGRESS；IN_PROGRESS→ACHIEVED/OVERDUE；终态无出边）。**OVERDUE 无主动 writer**（是系统判定逾期标记——候选被动可达性，需确认是否有 job 计算逾期）。
- **员工调查状态机**（`ErpHrSurvey`，ORM `orm.xml:1327-1376`）：列 `status` dict `erp-hr/survey-status`（**4 态**：DRAFT/OPEN/CLOSED/ARCHIVED，dict `orm.xml:183-188`）。
  - 迁移实现（`ErpHrSurveyBizModel.java`）：**⚠️ 18 行 CRUD 桩——无任何状态迁移方法**。OPEN/CLOSED/ARCHIVED 三态**无 writer** → dict 项死状态（候选可达性缺陷，与 mfg A2.6b 预测 CONSUMED 死状态同型）。
- **跨域访问**（员工/组织代码）：员工/组织代码跨域面极小。`ErpHrEmployeeBizModel.countReferences:126-138` 是同域读聚合（F7 引用预览）。`ErpHrReportBizModel.java:268` `daoFor(ErpMdPartner)`（master-data 只读）属 P1-MA1-022 同型（已登记 MR1 dashboard 聚合）。`IErpSysNotificationBiz`（notify 域）注入于 `ErpHrContractExpiryJob:40`（合同到期预警派发）。无 `daoFor(ErpFin*)`/`daoFor(ErpInv*)` 直写。
- **Processor 类**：**module-hr 无任何 `*Processor.java`**（grep 零匹配）——状态迁移逻辑全在 BizModel 内联（与 finance/manufacturing 的 Facade+Processor 两层不同）。`AssessmentAggregator`/`GapAnalysisCalculator`（competency helper）+ `PayrollCalculator`（工资 helper，归 A2.7b）是计算 helper 非状态迁移编排。
- **测试覆盖**：`TestErpHrRecruitmentEngine`（招聘全链 OPEN→HIRED + hire 联动）+`TestErpHrEmployeeTransfer`（调岗 + 合同处理）+`TestErpHrEmployeeReferences`（countReferences）+`TestErpHrCompetencyManagement`（考核+gap+发展计划状态机）+`job/TestErpHrContractExpiry`（合同 expire/renew + job 触发）+`TestErpHrSurveyCrudSmoke`（调查 CRUD 冒烟——**无状态迁移测试，匹配桩 BizModel**）。**无独立员工 employmentStatus 终态迁移测试**（因 resign/terminate/retire 未实现）。

**已登记的直指员工/组织状态机的 finding（本审计须复核其状态机行为）**：

- `P2-MA1-020`（todo MR1，hr）：残留 orphan dict `erp-hr/salary-approval-status`（6 态）在 `orm.xml:77-84` + i18n。**状态机 scope**：orphan dict 是 MA1 平台合规发现，`ErpHrSalary.approveStatus`（ORM:736）实际引用 `wf/approve-status`（4 态）——本审计复核 salary-approval-status 6 态（PENDING/REVIEWED/APPROVED_FINANCE/APPROVED_MANAGER/PAID/VOID）是否在 owner doc `payroll.md §审批状态标准化` 中作为历史状态机被引用（payroll 归 A2.7b，但 orphan dict 在 A2.7a scope 内的 ORM 域，本审计只确认其对员工/组织状态机无影响；salary 侧 approveStatus 归 A2.7b）。

**但从未做过一次覆盖员工与组织状态机（员工雇佣 + 合同 + 招聘 + 考核 + 发展计划 + 调查六组件）、按 `state-machine-business-review-prompt.md` 维度的系统性业务审查**。已知未核验控制点：

- **状态定义清晰性**：员工 employmentStatus 5 态——RESIGNED/TERMINATED/RETIRED 是"等待点"还是"动作结果终态"（owner doc §场景D/E 描述为终态但无迁移）；合同 SUSPENDED 语义（停薪留职？——无 writer 无法确认业务等待点）；调查 4 态 OPEN/CLOSED/ARCHIVED 是分发/收集/归档等待点（桩 BizModel 未实现）；招聘 HIRED 是终态（候选人已入职）；发展计划 OVERDUE 是被动逾期标记（是否应有 job 写入）。
- **转换完整性**：员工 employmentStatus **ACTIVE/PROBATION 间无 probationToRegular 迁移**（试用期→正式，owner doc 是否声明）；**ACTIVE→RESIGNED/TERMINATED/RETIRED 无迁移**（owner doc §场景D/E 声明但代码缺失）；合同 **ACTIVE→SUSPENDED 无迁移** + **无 resume from SUSPENDED**；招聘 `close:136-142` **无守卫**（任意 status 可 CLOSE，含已 HIRED——是否应限制）；考核 DRAFT→SUBMITTED→COMPLETED 完整（无 REJECT——考核无驳回？是否设计如此）；发展计划 DRAFT→CANCELLED 无迁移（CANCELLED 死状态？）；发展计划项 IN_PROGRESS→OVERDUE 是否有 job 驱动。
- **终端状态与恢复**：员工 RESIGNED/TERMINATED/RETIRED 终态（不可恢复——重新入职应新建员工记录？owner doc 是否声明）；招聘 HIRED/CLOSED/REJECTED 终态（REJECTED 候选人可重新申请？）；考核 COMPLETED 终态；调查 ARCHIVED 终态；合同 TERMINATED 终态（EXPIRED 可 renew 非真终态）。
- **异常路径**：调岗目标部门/岗位不存在（守卫 `requireTargetDepartment:170-180`/`requireTargetPosition:182-196`）；调岗期间请假冲突（`warnIfLeaveConflict:200-220` **非阻断 warn**——是否应阻断）；招聘 hire 重复入职（同一候选人多次 hire——幂等？）；合同 renew 已 TERMINATED 合同（守卫 L99-104 拒绝）；`expireOverdueContracts` 批量单失败隔离（L81-87——@BizMutation 事务边界是否覆盖批量）；考核 submit 空 details（守卫 L69-73 拒绝）。
- **可达性**：**重点——员工 RESIGNED/TERMINATED/RETIRED 是否可达**（无 writer → dict 项死状态，同 finance P1-MA2-031 + mfg P1-MA2-035/036 同型裁决）；**合同 SUSPENDED 是否可达**（无 writer → 死状态）；**调查 OPEN/CLOSED/ARCHIVED 是否可达**（桩 BizModel 无 writer → 死状态）；**发展计划 CANCELLED 是否可达**（无迁移 → 死状态）；**发展计划 DRAFT 是否可达**（generatePlan 初始即 IN_PROGRESS → DRAFT 不可达？）；招聘从 OPEN 到每个终态可达性。
- **角色与权限**：调岗（HR 专员）/招聘各阶段（HR 专员→主管面试→offer 审批）/考核 submit（员工自评）/complete（主管）/合同 renew（HR）/expire（系统 job）；危险操作（员工 TERMINATED 影响工资/请假/账户——联动未实现是否危险；招聘 hire 创建员工+合同跨实体）；`close` 无守卫危险操作（任意人可关闭已入职招聘？）。
- **外部依赖**：招聘 hire 跨实体创建 Employee+Contract（同域，合法）；考核 complete → gapAnalysis 跨实体刷新（同域）；合同到期 → `IErpSysNotificationBiz` 跨域派发（notify）；员工 TERMINATED 联动（owner doc 声明自动取消请假/禁用账户——**未实现**，无 finance/notify 联动）；外部步骤失败是否阻断状态迁移（@BizMutation 事务回滚）。
- **TODO/任务策略**：合同即将到期（`scanExpiringContracts` 产生预警 TODO——经 notify 派发）；招聘 INTERVIEW/OFFERED 是否产生面试/offer TODO；考核 SUBMITTED 是否产生主管评审 TODO（complete 是主管动作）；员工长期 PROBATION 未转正（无 probationToRegular——无 TODO 提醒转正）；调查 OPEN 是否产生分发收集 TODO（桩未实现）。
- **场景演练**：(a) 招聘快乐路径（OPEN→SCREENING→INTERVIEW→OFFERED→hire→HIRED + 创建员工 ACTIVE + 合同 ACTIVE）；(b) 招聘 reject（SCREENING/INTERVIEW/OFFERED→REJECTED）+ close（任意→CLOSED）；(c) 员工调岗（ACTIVE→transferEmployee→dept/position 更新 + 合同 TERMINATED→新建 ACTIVE）；(d) **员工离职**（owner doc §场景D 声明 ACTIVE→RESIGNED + 联动取消请假/终止合同/禁用账户——**代码未实现，演练确认缺口**）；(e) **员工退休/终止**（§场景E——**代码未实现**）；(f) 合同到期（ACTIVE→expireOverdueContracts→EXPIRED + notify 预警）+ renew（EXPIRED→ACTIVE）；(g) 考核全链（DRAFT→SUBMITTED→COMPLETED + gap 刷新 + 发展计划生成）；(h) 发展计划项迁移（NOT_STARTED→IN_PROGRESS→ACHIEVED）+ OVERDUE（系统判定？）；(i) 调查生命周期（DRAFT→OPEN→CLOSED→ARCHIVED——**桩未实现，演练确认缺口**）；(j) 并发调岗同员工（无 @Version——交接 A2.17）。
- **与设计文档一致性**：`state-machine.md` 员工 employmentStatus + 合同 vs 实现——**重点漂移**：(1) **§场景D/E 离职/终止/退休迁移 + 联动（取消请假/终止合同/禁用账户）owner doc 声明但代码完全未实现**（重大漂移——需裁决是 P1 实现缺口还是 owner doc 标注 Deferred）；(2) 合同 SUSPENDED dict 有但 owner doc/代码无迁移（死状态漂移）；(3) 调查 4 态 owner doc（`employee-survey.md`）声明生命周期但 BizModel 是桩（漂移）；(4) 招聘 `close` 无守卫 owner doc 是否声明（任意 status 可关闭）；(5) 发展计划 DRAFT/CANCELLED 可达性 owner doc 是否注记；(6) `competency-management.md` 考核无 REJECT 是否设计如此。

剩余差距：需要一次系统性状态机业务审查，将上述已落地组件与未核验控制点整合为通过/失败裁决，发现任何遗漏的 P0（**员工离职/终止/退休 employmentStatus 迁移完全缺失致 RESIGNED/TERMINATED/RETIRED 三态死状态 + owner doc 声明联动未实现** [若破坏业务路径——按 finance P1-MA2-031 + mfg P1-MA2-035 同型裁决为 P1 dict 死状态 + 实现缺口而非 P0，因不破坏已实现的主路径] / **招聘 close 无守卫致已 HIRED 招聘可被关闭** [若破坏数据一致性]）走即时通道，P1 登记入 arm-index 待 MR1。

## Goals

- 按 `state-machine-business-review-prompt.md` 10 维度对 **员工雇佣状态机**（5 态）+ **劳动合同状态机**（4 态）+ **招聘管线状态机**（7 态）+ **考核状态机**（3 态）+ **发展计划状态机**（4 态）+ **发展计划项状态机**（4 态）+ **员工调查状态机**（4 态）做系统性业务审查，产出审计报告。**严格限定 A2.7a scope = 员工与组织类状态机**；考勤/排班/请假/工资/仿真归 A2.7b。
- 重点核验已识别控制点：(1) 状态定义清晰性（RESIGNED/TERMINATED/RETIRED 终态语义 / 合同 SUSPENDED 语义 / 调查 OPEN/CLOSED/ARCHIVED / 发展计划 OVERDUE 被动标记）；(2) 转换完整性（**员工 ACTIVE→RESIGNED/TERMINATED/RETIRED 缺失 + probationToRegular 缺失** / 合同 SUSPENDED 无迁移 / 招聘 close 无守卫 / 考核无 REJECT / 发展计划 DRAFT→CANCELLED 缺失）；(3) 终端与恢复（员工三终态不可恢复 / 招聘 REJECTED 重申 / 合同 EXPIRED renew）；(4) 异常路径（调岗目标不存在 / 请假冲突 warn 非阻断 / hire 重复 / renew TERMINATED 拒绝 / 批量 expire 单失败隔离 / submit 空 details）；(5) 可达性（**员工 RESIGNED/TERMINATED/RETIRED / 合同 SUSPENDED / 调查 OPEN/CLOSED/ARCHIVED / 发展计划 CANCELLED·DRAFT 是否死状态**）；(6) 角色权限（TERMINATED 联动未实现危险 / 招聘 close 无守卫 / hire 跨实体）；(7) 外部依赖（hire 创建 Employee+Contract / complete 刷新 gap / 合同到期 notify / **TERMINATED 联动未实现**）；(8) TODO 任务策略（合同到期预警 / 长期 PROBATION 未转正 / 调查分发 TODO 未实现）；(9) 场景演练（10 个代表性场景）。
- 复核已登记 finding 在员工/组织状态机运行时的行为影响：P2-MA1-020（orphan dict salary-approval-status——确认对员工/组织状态机无影响；salary 侧归 A2.7b），标注终态（仅治理缺陷 / 产生运行时缺陷升级）。
- scope matrix §状态机正确性 hr 列 `❓S拆` → `⚠️(P1)(A2.7a✅;A2.7b❓)` 终态标记。
- 发现的 P0 走即时通道；P1 汇总登记至 `arm-index.md` §P1 发现汇总（目标 MR1）。roadmap A2.7a 推进至 `done`（经独立 closure audit）。

## Non-Goals

- **不**审计 A2.7b 考勤与工资类状态机（请假/考勤/排班/工资/仿真） — 那是 S 级拆分 2/2；本审计只确认员工 employmentStatus=ACTIVE/PROBATION 是工资 runPayroll 的前置（A2.7b 复核工资侧）。
- **不**审计 A2.1/A2.2 P2P/O2C 端到端编排正确性 — done；本审计不涉及采购/销售链路。
- **不**审计 A2.5 finance 状态机 — done；本审计只确认 hr 过账（工资）经 finance I*Biz（归 A2.7b）。
- **不**审计 A4.4 hr 代码质量 — BizModel/Calculator 代码质量（异常处理/N+1/索引/辅助方法）系统性审查归 A4.4；本审计只做状态机业务正确性审查。
- **不**审计 A2.17 并发与乐观锁 — 并发调岗同员工（无 @Version）归 A2.17；本审计只标注观察到的并发敏感点。
- **不**审计 owner doc 已裁定的 Deferred 偏离是否应实现（调查门户分发 / 考核 360 深化 / 发展计划自动跟踪） — 这些是 owner doc Deferred/Non-Goal，本审计只确认其在状态机上不引入悬挂（死状态归状态定义清晰性维度裁决为 P1/P2 dict 死状态清理或 owner doc 标注 Deferred 而非实现）。
- **不**在本计划内批量修复 P1 — P1 经 R1.0 展开机制进入 MR1。仅 P0 走即时通道。
- **不**手改生成物。任何代码/ORM 变更（P0 即时修复）须改源文件 + `mvn clean install -DskipTests` + 该修复子切片独立审计。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/design/human-resource/state-machine.md`（员工 employmentStatus + 合同 + §场景D/E 离职/终止/退休 + §实现偏离补注 — **需复核 §场景D/E 联动未实现是否漂移 / 合同 SUSPENDED 死状态 / 调查状态机**）；`docs/design/human-resource/recruitment.md`（招聘管线 7 态 + hire 联动 + close 守卫）；`docs/design/human-resource/competency-management.md`（考核 3 态 + 发展计划 + gap 分析）；`docs/design/human-resource/employee-survey.md`（调查 4 态生命周期——**BizModel 是桩，需复核 owner doc vs 实现漂移**）；`docs/design/human-resource/README.md`（域边界 vs projects/finance）
- Skill Selection Basis: `state-machine-business-review-prompt.md`（roadmap A2.7a 指定此 skill，状态机业务审查专用方法，项目定制化层见 `docs/skills/README.md`）
- Verification: 审计不改代码，故无单测回归；报告产出即更新 `arm-index.md`。若 P0 即时修复触及代码/ORM，则该修复需 `mvn clean install -DskipTests` + 相关测试。

## Infrastructure And Config Prereqs

- 无超出现有基线的 infra 依赖。构建走 Maven Reactor，`nop-entropy` 父 POM 已在本地 Maven 仓库。
- **保护区域门控**：员工/组织状态机本身非 ask-first 最高级保护区域（hr 非会计/财务/数据删除保护区域）。但员工 TERMINATED 联动（owner doc 声明禁用账户——若 P0 即时修复触及账户/权限）触及权限保护区域，须 owner doc + 人工确认。P0 即时修复若触及 `ErpHrEmployeeBizModel`/`ErpHrRecruitmentBizModel`/`ErpHrEmploymentContractBizModel`/`ErpHrSurveyBizModel`，须有 owner doc 描述预期行为 + 该修复子切片的独立审计 + 人工确认。ORM 字典变更（employment-status/contract-status/recruitment-status/survey-status/devplan-status/plan-item-status）属 ask-first。
- **审计 plan 的 BUILD_VERIFY**：审计不改代码，按 roadmap §其他纪律声明 BUILD_VERIFY 的 `mvn test` 仅作回归基线确认（~20min）。P0 即时修复的 build 在其各自 fix plan 内验证。

## Execution Plan

### Phase 1 - 员工与组织状态机系统性业务审查

Status: completed
Targets: `module-hr/erp-hr-service/.../service/entity/ErpHrEmployeeBizModel.java`（transferEmployee:88-116/守卫 isTransferable:165-168/requireTargetDepartment:170-180/requireTargetPosition:182-196/warnIfLeaveConflict:200-220/resolveHandleContract:227-260/createEmployeeFromRecruitment 转 recruitment/countReferences:126-138）；`.../service/entity/ErpHrRecruitmentBizModel.java`（moveToScreening:66-73/scheduleInterview:76-88/makeOffer:91-101/hire:104-119 + createEmployeeFromRecruitment:146/createContractForNewEmployee:175/reject:122-133/close:136-142 无守卫/守卫 requireStatus:212-216/illegalTransition:218）；`.../service/entity/ErpHrEmploymentContractBizModel.java`（scanExpiringContracts:59-69/expireOverdueContracts:72-90 单失败隔离:81-87/renew:93-109 守卫:99-104）；`.../service/entity/ErpHrEmployeeAssessmentBizModel.java`（submitAssessment:61-77/completeAssessment:80-104 + aggregateAndWriteBack:111-139 + 守卫 illegalTransition:151-156）；`.../service/entity/ErpHrDevelopmentPlanBizModel.java`（generateDevelopmentPlan:68-91 IN_PROGRESS:82/updatePlanItemStatus:94-112/completePlan:115-129 + assertPlanItemTransition:195-202/isValidPlanItemTransition:208-218）；`.../service/entity/ErpHrSurveyBizModel.java`（18 行 CRUD 桩，无状态迁移）；`.../service/job/ErpHrContractExpiryJob.java`（scan+expire cron-gated:55-59 + IErpSysNotificationBiz:40）；`module-hr/model/app-erp-hr.orm.xml`（employment-status:8-14/contract-status:35-40/recruitment-status:63-71/survey-status:183-188/assessment-status:215-219/devplan-status:226-231/plan-item-status:232-237 + ErpHrEmployee:265-340 employmentStatus:294/ErpHrEmploymentContract:419-478 status:438/ErpHrRecruitment:785-851 status:799/ErpHrEmployeeAssessment:1614-1657 status:1623/ErpHrDevelopmentPlan:1727-1764 status:1735/ErpHrDevelopmentPlanItem:1767-1812 status:1780/ErpHrSurvey:1327-1376 status:1337 + salary-approval-status orphan:77-84）；`docs/design/human-resource/state-machine.md`+`recruitment.md`+`competency-management.md`+`employee-survey.md`；服务层 `TestErpHrRecruitmentEngine`+`TestErpHrEmployeeTransfer`+`TestErpHrEmployeeReferences`+`TestErpHrCompetencyManagement`+`job/TestErpHrContractExpiry`+`TestErpHrSurveyCrudSmoke`
Skill: `state-machine-business-review-prompt.md`

- Item Types: `Proof`
- Prereqs: M0.3 done（绿色基线）；MA1 done（P2-MA1-020 orphan dict 已登记待 MR1，本审计复核状态机角度无升级）；A2.5a done（finance 凭证状态机 dict 死状态同型裁决范式 P1-MA2-031）；A2.6a/b done（mfg 状态机 dict 死状态同型裁决范式 P1-MA2-035/036）

- [x] 维度「状态定义」：审查员工 employmentStatus 5 态语义清晰性——RESIGNED/TERMINATED/RETIRED 是终态等待点还是动作（owner doc §场景D/E 声明为终态）；合同 SUSPENDED 语义（停薪留职等待点——无 writer）；调查 OPEN/CLOSED/ARCHIVED 是分发/收集/归档等待点（桩未实现）；招聘 HIRED 终态；发展计划 OVERDUE 被动逾期标记（是否应有 job 写入）。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「转换完整性」：列出员工每个状态传入/传出——**ACTIVE/PROBATION 间无 probationToRegular** + **ACTIVE→RESIGNED/TERMINATED/RETIRED 无迁移**（owner doc §场景D/E 声明但代码缺失——重点）；合同 ACTIVE→EXPIRED（expire）/ EXPIRED→ACTIVE（renew）/ ACTIVE→TERMINATED（调岗副作用）/ **SUSPENDED 无进出迁移**；招聘 7 态迁移矩阵 + **close 无守卫**（任意 status 可 CLOSE 含 HIRED——重点）；考核 DRAFT→SUBMITTED→COMPLETED（无 REJECT——设计如此？）；发展计划 DRAFT→IN_PROGRESS（generate）/ IN_PROGRESS→COMPLETED（complete）/ **DRAFT→CANCELLED 无迁移**；发展计划项 NOT_STARTED→IN_PROGRESS→ACHIEVED/OVERDUE。是否有非法跳转或缺失条件分支。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「终端状态和恢复」：员工 RESIGNED/TERMINATED/RETIRED 终态（不可恢复——重新入职新建？owner doc 是否声明）；合同 TERMINATED 终态（EXPIRED 可 renew 非真终态）；招聘 HIRED/CLOSED/REJECTED 终态（REJECTED 候选人可重新申请？）；考核 COMPLETED 终态；调查 ARCHIVED 终态；归档与活动记录是否可区分（employmentStatus/status）。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「异常路径」：核验全覆盖——调岗目标部门/岗位不存在（守卫拒绝）；调岗请假冲突（warnIfLeaveConflict **非阻断 warn**——是否应阻断，交接 A2.7b 请假联动）；招聘 hire 重复入职（幂等？）；合同 renew 已 TERMINATED（守卫 L99-104 拒绝）；expireOverdueContracts 批量单失败隔离（L81-87——@BizMutation 事务边界覆盖范围）；考核 submit 空 details（守卫拒绝）；招聘 close 已 HIRED（无守卫——异常路径？）。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「可达性」：**重点——员工 RESIGNED/TERMINATED/RETIRED 是否可达**（grep 无 writer → dict 项死状态，同 finance P1-MA2-031 + mfg P1-MA2-035/036 同型裁决）；**合同 SUSPENDED 是否可达**（无 writer → 死状态）；**调查 OPEN/CLOSED/ARCHIVED 是否可达**（桩 BizModel 无 writer → 死状态）；**发展计划 CANCELLED 是否可达**（无迁移 → 死状态）；**发展计划 DRAFT 是否可达**（generatePlan 初始 IN_PROGRESS → DRAFT 不可达？）；招聘从 OPEN 到每个终态可达性；是否有死循环或不可达终态。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「角色和权限」：每个转换绑定执行角色——调岗（HR 专员）/招聘各阶段（HR→主管面试→offer 审批）/考核 submit（员工自评）/complete（主管）/合同 renew（HR）/expire（系统 job）；危险操作（员工 TERMINATED 联动未实现——工资/请假/账户悬挂风险 / 招聘 close 无守卫 / hire 创建员工+合同跨实体）；多角色冲突。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「外部依赖」：招聘 hire 跨实体创建 Employee+Contract（同域合法）；考核 complete → gapAnalysis 刷新（同域）；合同到期 → IErpSysNotificationBiz 跨域派发（notify，预fire）；**员工 TERMINATED 联动**（owner doc §场景D 声明自动取消请假/终止合同/禁用账户——**未实现**，无 finance/notify 联动，重点核验缺口影响）；外部步骤失败是否阻断状态迁移（@BizMutation 事务回滚）。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「TODO/任务策略」：每个非终端状态是否产生正确类型待办——合同即将到期（scanExpiringContracts 经 notify 预警 TODO）；招聘 INTERVIEW/OFFERED 面试/offer TODO；考核 SUBMITTED 主管评审 TODO；**长期 PROBATION 未转正无 TODO**（probationToRegular 未实现——静默下沉？）；调查 OPEN 分发收集 TODO（桩未实现——静默）；是否存在期望有人行动但不产生待办的状态。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「场景演练（最重要）」：端到端演练代表性场景——(a) 招聘快乐路径（OPEN→SCREENING→INTERVIEW→OFFERED→hire→HIRED + 创建员工+合同）；(b) 招聘 reject + close（任意→CLOSED 含已 HIRED？）；(c) 员工调岗（ACTIVE→transferEmployee + 合同 TERMINATED→新建 ACTIVE）；(d) **员工离职**（§场景D ACTIVE→RESIGNED + 联动——**代码未实现，演练确认缺口**）；(e) **员工退休/终止**（§场景E——**未实现**）；(f) 合同到期（expire + notify）+ renew；(g) 考核全链（DRAFT→SUBMITTED→COMPLETED + gap 刷新 + 发展计划生成）；(h) 发展计划项迁移 + OVERDUE（系统判定？）；(i) **调查生命周期**（DRAFT→OPEN→CLOSED→ARCHIVED——**桩未实现，演练确认缺口**）；(j) 并发调岗同员工（无 @Version，交接 A2.17）。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「与设计文档一致性」：每个状态/转换在 `state-machine.md`/`recruitment.md`/`competency-management.md`/`employee-survey.md` 是否有匹配——**重点漂移**：(1) **§场景D/E 离职/终止/退休迁移 + 联动 owner doc 声明但代码完全未实现**（重大漂移——裁决 P1 实现缺口 / Deferred 标注 / owner doc 删除声明）；(2) 合同 SUSPENDED dict 有但无迁移（死状态漂移）；(3) 调查 4 态 owner doc 声明生命周期但 BizModel 桩（漂移）；(4) 招聘 close 无守卫 owner doc 是否声明；(5) 发展计划 DRAFT/CANCELLED 可达性 owner doc 是否注记；(6) 考核无 REJECT 是否设计如此；(7) P2-MA1-020 orphan dict salary-approval-status 是否在 owner doc 中残留引用（payroll 侧归 A2.7b，本审计只确认员工/组织侧无残留）。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 复核已登记 finding 员工/组织状态机角度：P2-MA1-020（orphan dict salary-approval-status——确认对员工/组织状态机无影响，salary 侧归 A2.7b）。标注终态（仅治理缺陷 / 产生运行时缺陷升级）。
      - Skill: none
- [x] 产出审计报告 `docs/audits/2026-07-28-0230-arm-ma2-hr-employee-organization-state-machine.md`（含：员工/合同/招聘/考核/发展计划/调查状态图、各维度通过/失败裁决、控制点 PASS/FAIL、MA1 finding 运行时影响复核表、并发敏感点交接 A2.17、残留风险）。
      - Skill: none

Exit Criteria:

> 审计报告是唯一可观察产物。完整仓库 `mvn test` 属 Closure Gates（见执行时规则 7）。仅本阶段交付的本地化检查列在此。

- [x] 员工雇佣（5 态）+ 合同（4 态）+ 招聘（7 态）+ 考核（3 态）+ 发展计划（4 态）+ 发展计划项（4 态）+ 调查（4 态）的状态图与转换矩阵产出，每个状态/转换有通过/失败裁决与证据
- [x] 已识别控制点（状态定义 / 转换完整性[含员工终态迁移缺失 + 合同 SUSPENDED + 招聘 close 无守卫] / 终端与恢复 / 异常路径 / 可达性[含 RESIGNED/TERMINATED/RETIRED/SUSPENDED/调查三态/CANCELLED 死状态] / 角色权限 / 外部依赖[含 TERMINATED 联动未实现] / TODO 任务策略 / 场景演练）均有通过/失败裁决与证据
- [x] state-machine-business-review 10 维度至少一句裁决（含「本维度无发现」）

### Phase 2 - P0 即时通道处理 + P1 汇总交接 MR1 + 索引/矩阵更新

Status: completed
Targets: 员工/组织状态机审计发现的 P0/P1 finding；`docs/audits/arm-index.md`；`docs/audits/audit-remediation-scope-and-dimension-matrix.md` §状态机正确性 hr 列
Skill: none

- Item Types: `Fix | Add | Follow-up`
- Prereqs: Phase 1 完成（finding 全部识别）

- [x] P0 finding 即时处理：每个 P0（**员工离职/终止/退休 employmentStatus 迁移完全缺失 + owner doc §场景D/E 联动未实现** [若破坏已实现业务路径——按 finance P1-MA2-031 + mfg P1-MA2-035/036 同型裁决，不破坏主路径（ACTIVE/PROBATION 调岗 + 招聘 hire 完整覆盖在职生命周期），归 P1 dict 死状态 + 实现缺口而非 P0] / **招聘 close 无守卫致已 HIRED 招聘可被关闭** [若破坏数据一致性——若 close 已 HIRED 不产生悬挂则 P1]）当即就地修复（改源文件 + `mvn clean install -DskipTests` + 该修复独立审计 + 人工确认）或异步注入 fix plan（`docs/plans/YYYY-MM-DD-HHmm-arm-fix-*.md`）。P0 永不进入 MR 批量修复。每个 P0 在报告中标注修复路径与状态。
      - Skill: none
      - **裁决：本审计零 P0**——候选 P0 经证据证伪或降级：(a) 员工离职/终止/退休迁移完全缺失——按 finance P1-MA2-031 + mfg P1-MA2-035/036 同型裁决为 **P1-MA2-039**（dict 死状态 + 实现缺口，不破坏主路径——在职生命周期 ACTIVE/PROBATION 完整覆盖）；(b) 招聘 close 无守卫致 HIRED→CLOSED——**证伪 P0**，HIRED→CLOSED 是合法入职后清理（employeeId 持久化保留），无数据破坏，登记 **P2-MA2-048** watch-only。无即时通道 fix plan 产出。
- [x] P1 finding 汇总：全部 P1 登记至 `arm-index.md` §P1 发现汇总（Finding ID `P1-MA2-NNN`、报告、描述、目标 MR1、修复状态 todo）。注意：本审计对已登记 finding（P2-MA1-020）只复核状态机运行时影响不重复登记根因；若发现新 P1（如员工 RESIGNED/TERMINATED/RETIRED dict 死状态 + owner doc §场景D/E 联动未实现 [同 finance P1-MA2-031 + mfg P1-MA2-035/036 同型] / 合同 SUSPENDED 死状态 / 调查 OPEN/CLOSED/ARCHIVED 死状态 + 桩 BizModel / 招聘 close 无守卫 / 发展计划 CANCELLED 死状态）按新 finding ID 登记。
      - Skill: none
      - **已登记 4 项新 P1**（全部按 finance P1-MA2-031 + mfg P1-MA2-035/036 同型裁决）：**P1-MA2-039** 员工 employmentStatus RESIGNED/TERMINATED/RETIRED 三态死状态 + owner doc §场景D/E 离职/退休/转正迁移 + 联动完全未实现 / **P1-MA2-040** 合同 SUSPENDED dict 死状态 + owner doc 无合同独立章节 / **P1-MA2-041** 调查 OPEN/CLOSED/ARCHIVED 三态死状态 + ErpHrSurveyBizModel 18 行 CRUD 桩 + owner doc §状态机声明漂移 / **P1-MA2-042** 发展计划 DRAFT/CANCELLED + 计划项 OVERDUE dict 死状态 + 无 cancelPlan + 无 OVERDUE 自动 job。5 项新 P2 watch-only（P2-MA2-047/048/049/050/051）同步登记。已登记 finding（P2-MA1-020 + P1-MA1-022）运行时复核无升级（不重复登记根因）。
- [x] 更新 arm-index 报告清单（新增本报告行）+ scope matrix §状态机正确性 hr 列终态标记（`❓S拆` → `⚠️(P1)(A2.7a✅;A2.7b❓)`）。
      - Skill: none
      - **已更新**：(a) `arm-index.md` 报告清单新增 `2026-07-28-0230-arm-ma2-hr-employee-organization-state-machine.md` 行（done）；(b) `arm-index.md` §A2.7a 新增项总结段落（位置紧随 A2.6b 之后）；(c) `arm-index.md` §P1 详细清单新增 P1-MA2-039/040/041/042 行；(d) `arm-index.md` §P2 汇总新增 P2-MA2-047/048/049/050/051 行；(e) `audit-remediation-scope-and-dimension-matrix.md` §状态机正确性 hr 列由 `❓S拆` 推进至 `⚠️P1(A2.7a✅;A2.7b❓)`；(f) §2.2 介绍文本新增 A2.7a 完成段落。

Exit Criteria:

- [x] 所有 P0 已即时处理（修复或注入 fix plan）并标注状态
- [x] 所有 P1 已登记 arm-index §P1 汇总，待 R1.0 展开
- [x] arm-index 报告清单 + scope matrix 已反映审计结论

## Draft Review Record

- Independent draft review iteration 1: **accept**（`ses_05b35da33ffeZmN4evUAwIFy4O`，独立 general 子代理，fresh-context，对照实时仓库逐行复核）。VERDICT = accept，**无 BLOCKER**。核实要点：7 个 dict（employment-status:8-14/contract-status:35-40/recruitment-status:63-71/survey-status:183-188/assessment-status:215-219/devplan-status:226-231/plan-item-status:232-237）行号精确 ✓；7 实体 status 列行号精确（Employee:294/Contract:438/Recruitment:799/Survey:1337/Assessment:1623/DevPlan:1735/DevPlanItem:1780）✓；BizModel 方法行号精确（Recruitment 6 迁移 + close 无守卫 / Contract expire+renew / Assessment submit+complete / DevPlan generate+updateItem+complete）✓；**ErpHrSurveyBizModel = 18 行 CRUD 桩无迁移方法** ✓；**无 resign/terminate/retire/probationToRegular BizMutation + 无 *Processor.java** ✓；6 测试文件存在 ✓；P2-MA1-020 + 4 owner doc 存在 ✓。检查清单全 PASS（基线准确性/单一结果表面员工与组织/Item 类型 Proof+Fix/Add/Follow-up/技能匹配/反松弛无禁用词/不可降级项 RESIGNED·TERMINATED·RETIRED·SUSPENDED·调查死 dict 路由 P0 即时通道或 P1 MR1/结束门控含独立审计+全量验证在 Closure Gates/退出标准可观察）。**采纳的非阻塞精化**：(1) 常量引用更正——`EMPLOYMENT_RESIGNED/TERMINATED/RETIRED`（无 _STATUS 后缀）位于 `ErpHrConstants.java:215-217`，仅 `nonTransferableStatuses():336-338` 只读守卫引用无 setStatus writer（已应用至 Current Baseline）；(2) orphan dict 行范围 `orm.xml:77-83` → `77-84`（已应用）。Plan Status 转 active。

## Closure Gates

> 本计划主体是审计（不改代码）。完整仓库验证在此处运行一次（确认审计期间任何 P0 即时修复未引入回归）。若无 P0 即时修复（仅 P1 登记），则 build/test 门控为回归基线确认。

- [x] 范围内行为完成（A2.7a 员工与组织状态机系统性审查报告产出 + arm-index 更新 + scope matrix 标记完成）
- [x] 相关文档对齐（审计报告、arm-index、scope matrix、state-machine/recruitment/competency-management/employee-survey owner doc 结论已反映）
- [x] 已运行验证：零 P0 即时修复 → 全量 `mvn clean install -DskipTests`（BUILD SUCCESS）+ `mvn test -pl module-hr/erp-hr-service -am`（Tests run: 113, Failures: 0, Errors: 0, Skipped: 0）作回归基线确认；若有 P0 即时修复，该修复模块测试全绿
- [x] 无范围内项目降级为 deferred/follow-up（P1 不属降级——按设计进入 MR1；P0 注入即时通道 fix plan，不降级为 MR；本审计零 P0 即时修复）
- [x] 独立草案审查已完成并记录（见 Draft Review Record iteration 1 accept）
- [x] 文本一致性已验证（状态、阶段、门控、日志都一致——Plan Status: completed / Phase 1: completed / Phase 2: completed / 报告产出 / arm-index 同步 / scope matrix hr 列终态标记）
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中（见下方 `## Closure`）

## Deferred But Adjudicated

### A2.7b 考勤与工资类状态机

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 那是 S 级拆分 2/2，后续执行。本审计只确认员工 employmentStatus=ACTIVE/PROBATION 是工资 runPayroll 前置（A2.7b 复核工资侧）。
- Successor Required: `yes`——A2.7b 执行时复核。

### A4.4 hr 代码质量审计

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本审计做员工/合同/招聘/考核/发展计划状态机**业务正确性**审查；BizModel/Calculator 代码质量（异常处理/N+1/索引）系统性审查归 A4.4。
- Successor Required: `yes`——A4.4 执行时复核。

### A2.17 并发与乐观锁（并发调岗同员工）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: roadmap 显式分配给 A2.17。本审计标注观察到的并发敏感点（调岗无 @Version / 批量 expire 竞态 / 招聘 hire 并发），不做系统性并发正确性裁决。
- Successor Required: `yes`——A2.17 执行时复核。

### owner doc 已裁定 Deferred 偏离本身（调查门户分发 / 考核 360 深化 / 发展计划自动跟踪 / 员工离职联动自动化）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: owner doc 已裁定为 Deferred/Non-Goal。本审计只确认其在状态机上不引入悬挂（死状态归状态定义清晰性维度裁决为 P1/P2 dict 死状态清理或 owner doc 标注 Deferred 而非实现）。
- Successor Required: `yes`——各 successor 触发条件满足时（如调查门户上线 / 考核 360 深化 / 离职联动自动化需求落地）。

## Closure

Status Note: A2.7a hr 员工与组织状态机审查已落地——审计报告产出（10 维度裁决 + 7 组件状态图 + 零 P0 + 4 项 P1 + 5 项 P2），arm-index + scope matrix + roadmap 同步，docs/logs/2026/07-28.md 日志已记录。计划类型是 audit-only（零生产代码变更），故无单测回归；全量 build/test 作回归基线确认。Plan Status 转 completed 经独立结束审计通过。

Closure Audit Evidence:

- Auditor / Agent: 独立 closure 审计子代理（fresh-context，不重用执行者上下文，对照实时仓库逐项复核）
- Audit Method: `node tools/mission-driver/src/plan-check.mjs ... --strict`（修正未勾选结束门控 + 补 Closure section 后重跑 PASS）+ 实仓语义复核（grep + read 验证产物存在与内容一致）
- Verification Walkthrough:
  - **产物存在性 ✓**：`docs/audits/2026-07-28-0230-arm-ma2-hr-employee-organization-state-machine.md` 存在（571+ 行，9 大节：裁决 / 状态图 / 10 维度 / MA1 复核 / P0-P1-P2 / 并发 / 残留风险 / 范围声明 / 结论）
  - **arm-index 同步 ✓**：报告清单 line 32 新增 done 行；§A2.7a 完成段 line 128-130；§P1 详细清单 line 97-100（P1-MA2-039/040/041/042 全部 MR1 todo）；§P2 汇总 line 180-184（P2-MA2-047~051 watch-only）
  - **scope matrix 同步 ✓**：`audit-remediation-scope-and-dimension-matrix.md` §状态机正确性 hr 列由 `❓S拆` 推进至 `⚠️P1(A2.7a✅;A2.7b❓)`（line 103）
  - **docs/logs/2026/07-28.md ✓**：日志条目完整（任务 / scope / 维度 / 4 P1 / 5 P2 / MA1 复核 / 并发敏感点 / 同步产物 / Skill）
  - **五点一致性 ✓**：Plan Status: completed / Phase 1: completed / Phase 2: completed / 全部 Exit Criteria `[x]` / 全部 Closure Gates `[x]` / 日志条目与计划一致
  - **Anti-Hollow ✓**：audit-only 计划无生产代码变更；报告非空壳——Verdict + 6 候选 P0 逐项裁决 + 4 P1 详细描述含行号证据 + 5 P2 含 owner doc 引用 + 7 组件状态图 + 5 并发敏感点交接 A2.17（含 versionProp 降级事实）
  - **Deferred honesty ✓**：A2.7b/A4.4/A2.17 在 Non-Goals + Deferred But Adjudicated 显式 out-of-scope（S 级拆分 / 工作项分配）；零 P0/P1 finding 隐藏在 Deferred；4 项 P1 已登记 MR1 非降级
  - **MA1 finding 运行时复核 ✓**：P2-MA1-020（orphan dict salary-approval-status）+ P1-MA1-022（跨域只读 daoFor）经状态机角度复核**无升级**，仅治理缺陷
- Closure Verdict: approved（无 BLOCKER，计划可标记 completed）

Follow-up:

- A2.7b hr 考勤与工资状态机审查（S 级拆分 2/2）—— 后续执行
- P1-MA2-039/040/041/042 —— MR1 展开机制进入修复
- P2-MA2-047~051 —— MR1 watch-only（含 owner doc 文字校验建议）
- 并发敏感点 5 处 —— 交接 A2.17（含 7 实体 versionProp 降级事实已记录）
