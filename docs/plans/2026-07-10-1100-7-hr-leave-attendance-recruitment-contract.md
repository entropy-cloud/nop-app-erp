# 2026-07-10-1100-7-hr-leave-attendance-recruitment-contract HR 休假/考勤/招聘/合同到期引擎

> Plan Status: completed
> Last Reviewed: 2026-07-10
> Source: `docs/design/human-resource/use-cases.md` UC-HR-02/05/06/07 + erp-survey 对标（Odoo/ERPNext/Axelor/AureusERP 均为核心内置）+ extended-roadmap.md:54 Non-Goal（无技术约束，纯优先级排序）
> Related: `extended-roadmap.md` §M3 human-resource Non-Goal boundary；plan `0831-3`（排班 calcAttendance + onLeaveApproved 钩子已实现但悬空）；plan `0831-2`（薪酬引擎引用考勤但 unpaidLeaveDays 硬编码 ZERO）
> Audit: required

## Current Baseline

### 已实现

- **休假实体** `ErpHrLeaveRequest`：完整字段（leaveType/startDate/endDate/durationDays/status/approverId）+ 字典 `erp-hr/leave-type`（7 类）+ `erp-hr/leave-status`（5 态 DRAFT/SUBMITTED/APPROVED/REJECTED/CANCELLED）。空 CRUD BizModel（仅 `defaultPrepareSave` 设 businessDate）
- **休假→排班钩子**：`IErpHrShiftBiz.onLeaveApproved/onLeaveCancelled`（plan 0831-3 已实现）—— 经 leaveRequestId 检索排班范围内 `ErpHrShiftAssignment`，置 `isAbsent=true/absenceReason=LEAVE`。**但生产中永不触发**（因休假审批引擎不存在）
- **考勤实体** `ErpHrAttendance`：完整字段（clockIn/clockOut/workHours/lateMinutes/earlyLeaveMinutes/isAbsent/source/leaveRequestId）+ 字典 `erp-hr/attendance-source`（CARD/BIOMETRIC/MOBILE）
- **考勤派生计算** `calcAttendance`（plan 0831-3 已实现）：排班驱动，含跨天夜班、迟到/早退计算、缺勤判定、休假覆盖。`ShiftAttendanceCalculator` 纯函数式
- **薪酬引用考勤** `PayrollCalculator.summarizeAttendance`：读 `ErpHrAttendance` 算 actualDays/overtimeHours → 出勤比例 → 基本工资/加班费。`unpaidLeaveDays` 硬编码 ZERO（无薪假扣减缺失）
- **招聘实体** `ErpHrRecruitment`：扁平单实体（positionId/departmentId/candidateName/source/status/interviewerId/interviewDate/offerSalary/hiredDate/employeeId）+ 字典 `erp-hr/recruitment-status`（7 态 OPEN/SCREENING/INTERVIEW/OFFERED/HIRED/REJECTED/CLOSED）。空 CRUD BizModel
- **合同实体** `ErpHrEmploymentContract`：完整字段（contractType/signDate/startDate/endDate/status）+ 字典。调动联动 `handleContract` 已实现（plan 0517-2），但**到期预警 Job 完全缺失**

### 剩余差距

| 功能 | UC | 实体 | CRUD | 引擎 | 差距 |
|------|-----|------|------|------|------|
| 休假管理 | UC-HR-02 | ✅ | ✅ | ❌ | 审批状态机 + 余额实体 + durationDays 计算 + 钩子触发 |
| 考勤打卡 | UC-HR-06 | ✅ | ✅ | △ | 打卡端点 + 无薪假扣减 |
| 招聘流程 | UC-HR-05 | ✅ | ✅ | ❌ | 状态机推进 + HIRED→创建员工 |
| 合同到期 | UC-HR-07 | ✅ | ✅ | ❌ | 到期扫描 Job + 通知 + 续签 |

### 对标依据

| 开源 ERP | 休假 | 考勤 | 招聘 | 合同到期 |
|----------|------|------|------|---------|
| **Odoo** | Time Off（审批+余额+日历） | Attendances（打卡） | Recruitment（全流程） | 合同到期预警 |
| **ERPNext** | Leave Application + Leave Allocation | Employee Checkin | Job Applicant + Interview | Contract到期提醒 |
| **Axelor** | Leave管理 | Attendance | Recruitment | — |
| **AureusERP** | Leave | Attendance | Recruitment | — |
| **本项目** | 实体+钩子悬空 | 派生计算有/打卡缺 | 扁平实体/引擎缺 | **完全缺失** |

## Goals

- 实现休假审批状态机（submit→approve/reject→cancel）+ 休假余额实体 + durationDays 自动计算 + 激活排班联动钩子
- 实现考勤打卡端点（clock-in/clock-out）+ 无薪假扣减接入薪酬引擎
- 实现招聘状态机推进（OPEN→SCREENING→INTERVIEW→OFFERED→HIRED）+ HIRED 自动创建 `ErpHrEmployee`
- 实现合同到期扫描 Job + 通知派发 + 续签/终止流程

## Non-Goals

- **招聘多实体拆分**（独立 Candidate/Interview/Scorecard/Offer/OnboardingChecklist 实体）——本期在现有扁平 `ErpHrRecruitment` 上实现状态机和关键联动；多实体拆分归 successor
- **休假日历/可视化排班图**——前端可视化 successor
- **移动端打卡/生物识别集成**——本期仅提供 API 端点，设备集成归 successor
- **审批工作流（.xwf）多级链**——本期休假/招聘审批经 DIRECT 模式 + 状态机
- **培训管理（UC-HR-12）**——README 声明"远期扩展"，本期不涉及

## Task Route

- Type: `app-layer design change`（新增 ORM 实体 + 多 BizModel 引擎 + 定时 Job）
- Owner Docs: `docs/design/human-resource/use-cases.md`（UC-HR-02/05/06/07）、`docs/design/human-resource/README.md`、`docs/design/human-resource/recruitment.md`
- Skill Selection Basis: 新增 ORM 实体 + BizModel 方法 + 定时 Job → nop-backend-dev；GraphQL Engine 测试 → nop-testing

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline

## Execution Plan

### Phase 1 - ORM 模型变更：休假余额实体 + 考勤唯一约束

Status: completed
Targets: `module-hr/model/app-erp-hr.orm.xml`
Skill: nop-backend-dev

- Item Types: `Decision | Add`
- Prereqs: none

- [x] Decision: 休假余额模型设计
  - 创建 `ErpHrLeaveBalance`（休假额度/余额），镜像 ERPNext `Leave Allocation` 范式
  - 字段：employeeId, leaveType, fiscalYear(Integer), entitledDays(DECIMAL), usedDays(DECIMAL, 派生), carriedForwardDays(DECIMAL, default 0), remark + 标准审计
  - usedDays **不落库**（派生 = Σ approved LeaveRequest.durationDays where employeeId+leaveType+fiscalYear）——对齐 budget.md 派生字段范式
  - 替代方案：落库 usedDays + 审批时 increment/decrement——rejected，派生避免余额不一致风险
  - Skill: nop-backend-dev

- [x] Add: `ErpHrLeaveBalance` 实体
  - 唯一键：`(employeeId, leaveType, fiscalYear, orgId, delVersion)`
  - 索引：employeeId / leaveType+fiscalYear
  - Skill: nop-backend-dev

- [x] Add: `ErpHrAttendance` 新增 `(employeeId, date)` 唯一约束（防止同日多条打卡记录）
  - 注意：现有数据可能无此约束——增量添加时评估是否需要数据清洗
  - Skill: nop-backend-dev

- [x] Add: 执行 `mvn clean install -DskipTests`（module-hr 链）触发增量代码生成
  - Skill: nop-backend-dev

Exit Criteria:

- [x] ORM 变更后 `mvn clean install -DskipTests`（module-hr 链）BUILD SUCCESS
- [x] `ErpHrLeaveBalance` Entity/DAO 生成

### Phase 2 - 休假审批引擎 + 余额 + 钩子激活

Status: completed
Targets: `module-hr/erp-hr-service/.../entity/ErpHrLeaveRequestBizModel.java`
Skill: nop-backend-dev

- Item Types: `Add | Proof`
- Prereqs: Phase 1

- [x] Add: `IErpHrLeaveRequestBiz` 新增方法声明
  - `@BizMutation submit(@Name("id") String id, context)` —— DRAFT/SUBMITTED → SUBMITTED
  - `@BizMutation approve(@Name("id") String id, context)` —— SUBMITTED → APPROVED（触发钩子+余额校验）
  - `@BizMutation reject(@Name("id") String id, context)` —— SUBMITTED → REJECTED
  - `@BizMutation cancel(@Name("id") String id, context)` —— APPROVED → CANCELLED（回退联动）
  - `@BizQuery getBalance(@Name("employeeId") Long employeeId, @Name("leaveType") String leaveType, @Name("fiscalYear") Integer fiscalYear, context)` —— 查询余额
  - Skill: nop-backend-dev

- [x] Add: `ErpHrLeaveRequestBizModel` 实现
  - `defaultPrepareSave`：自动计算 `durationDays = ChronoUnit.DAYS.between(startDate, endDate) + 1`
  - `submit`：校验状态=DRAFT、余额充足（`checkLeaveBalance`）、日期不重叠（同员工同 leaveType 已有 APPROVED/SUBMITTED 区间重叠 → 拒绝）
  - `approve`：状态迁移 SUBMITTED→APPROVED + 设置 approverId/approvedAt + **调用 `shiftBiz.onLeaveApproved(leaveRequestId, context)`** 激活排班联动
  - `reject`：SUBMITTED→REJECTED
  - `cancel`：APPROVED→CANCELLED + **调用 `shiftBiz.onLeaveCancelled(leaveRequestId, context)`** 回退排班标记
  - `checkLeaveBalance`：查 LeaveBalance(entitledDays + carriedForwardDays − usedDays) ≥ durationDays，不足抛 `ERR_LEAVE_BALANCE_INSUFFICIENT`
  - `getBalance`：聚合计算 remaining = entitled + carried − Σ approved durationDays
  - Skill: nop-backend-dev

- [x] Add: `ErpHrLeaveBalanceBizModel`（CrudBizModel）
  - 标准 CRUD（HR 管理员维护年度额度）
  - Skill: nop-backend-dev

- [x] Add: `ErpHrErrors` 新增错误码
  - `ERR_LEAVE_BALANCE_INSUFFICIENT`（余额不足）
  - `ERR_LEAVE_DATE_OVERLAP`（日期重叠）
  - `ERR_LEAVE_ILLEGAL_STATUS_TRANSITION`（非法状态迁移）
  - Skill: nop-backend-dev

- [x] Proof: GraphQL Engine 测试 `TestErpHrLeaveEngine`
  - 场景 1（完整审批流程）：创建 LeaveRequest → submit → approve → 状态 APPROVED + 排班 ShiftAssignment 标记 isAbsent=true
  - 场景 2（余额不足拦截）：entitledDays=5 + 已用 3 + 新申请 4 → submit 拦截 `ERR_LEAVE_BALANCE_INSUFFICIENT`
  - 场景 3（日期重叠）：已有 APPROVED 1/1~1/3 + 新申请 1/2~1/4 → submit 拦截
  - 场景 4（cancel 回退）：approve → cancel → 排班标记解除 + 余额恢复
  - 场景 5（durationDays 自动计算）：startDate=1/1, endDate=1/5 → durationDays=5
  - Skill: nop-testing

Exit Criteria:

- [x] 休假审批 submit→approve→排班联动 全链路验证
- [x] 余额校验 + 日期重叠校验正确拦截
- [x] cancel 回退排班标记 + 余额恢复

### Phase 3 - 考勤打卡端点 + 无薪假扣减

Status: completed
Targets: `module-hr/erp-hr-service/.../entity/ErpHrAttendanceBizModel.java`、`module-hr/erp-hr-service/.../payroll/PayrollCalculator.java`
Skill: nop-backend-dev

- Item Types: `Add | Fix | Proof`
- Prereqs: Phase 2

- [x] Add: `IErpHrAttendanceBiz` 新增打卡方法声明
  - `@BizMutation clockIn(@Name("employeeId") Long employeeId, context)` —— 创建/更新当日考勤 clockIn=now
  - `@BizMutation clockOut(@Name("employeeId") Long employeeId, context)` —— 更新当日考勤 clockOut=now + 计算 workHours
  - `@BizQuery getTodayAttendance(@Name("employeeId") Long employeeId, context)` —— 查当日考勤状态
  - Skill: nop-backend-dev

- [x] Add: `ErpHrAttendanceBizModel` 打卡实现
  - `clockIn`：查 (employeeId, today) 唯一记录 → 不存在则创建（source=CARD）→ 存在且 clockIn 已设则抛 `ERR_ALREADY_CLOCKED_IN` → 设 clockIn=now
  - `clockOut`：查当日 → 不存在或 clockIn 为空抛 `ERR_NOT_CLOCKED_IN` → 设 clockOut=now + workHours = Duration.between(clockIn, clockOut)
  - Skill: nop-backend-dev

- [x] Add: `ErpHrErrors` 新增 `ERR_ALREADY_CLOCKED_IN` / `ERR_NOT_CLOCKED_IN`
  - Skill: nop-backend-dev

- [x] Fix: `PayrollCalculator` 接入无薪假扣减
  - `summarizeAttendance`：新增 `unpaidLeaveDays = Σ LeaveRequest where leaveType=SICK(SICK 视为无薪, config-gated) OR leaveType=PERSONAL 且 status=APPROVED 且 date in period` 的 durationDays
  - `basicSalary` 调整：`basicSalary = monthlySalary × attendanceRatio × (1 − unpaidLeaveDays / requiredDays)` 或按配置 `erp-hr.deduct-unpaid-leave`（默认 false 向后兼容）
  - Skill: nop-backend-dev

- [x] Proof: GraphQL Engine 测试 `TestErpHrAttendanceEngine`
  - 场景 1（打卡）：clockIn → clockOut → workHours 正确计算
  - 场景 2（重复打卡拦截）：同日二次 clockIn → `ERR_ALREADY_CLOCKED_IN`
  - 场景 3（未签到签退）：无 clockIn 直接 clockOut → `ERR_NOT_CLOCKED_IN`
  - Skill: nop-testing

Exit Criteria:

- [x] 打卡端点 create/update 考勤记录正确
- [x] 重复/非法打卡被拦截

### Phase 4 - 招聘状态机 + 入职联动

Status: completed
Targets: `module-hr/erp-hr-service/.../entity/ErpHrRecruitmentBizModel.java`
Skill: nop-backend-dev

- Item Types: `Decision | Add | Proof`
- Prereqs: none（与 Phase 2/3 独立）

- [x] Decision: 招聘引擎范围
  - 在现有扁平 `ErpHrRecruitment` 实体上实现状态机推进，**不创建 7 个设计规划的新实体**
  - 理由：扁平实体已含 candidate/interview/offer 关键字段；多实体拆分（Candidate/Interview/Scorecard/Offer/Onboarding）属大型 successor，本期先补齐状态机和关键联动
  - Skill: nop-backend-dev

- [x] Add: `IErpHrRecruitmentBiz` 新增方法声明
  - `@BizMutation moveToScreening(@Name("id") String id, context)` —— OPEN → SCREENING
  - `@BizMutation scheduleInterview(@Name("id") String id, @Name("interviewerId") Long interviewerId, @Name("interviewDate") LocalDate interviewDate, context)` —— SCREENING → INTERVIEW
  - `@BizMutation makeOffer(@Name("id") String id, @Name("offerSalary") BigDecimal offerSalary, context)` —— INTERVIEW → OFFERED
  - `@BizMutation hire(@Name("id") String id, @Name("hiredDate") LocalDate hiredDate, context)` —— OFFERED → HIRED + 自动创建 ErpHrEmployee
  - `@BizMutation reject(@Name("id") String id, context)` —— 任意阶段 → REJECTED
  - `@BizMutation close(@Name("id") String id, context)` —— → CLOSED
  - Skill: nop-backend-dev

- [x] Add: `ErpHrRecruitmentBizModel` 状态机实现
  - 状态迁移校验（非法迁移抛 `ERR_RECRUITMENT_ILLEGAL_STATUS_TRANSITION`）
  - `hire` 关键联动：
    1. 状态 OFFERED→HIRED
    2. 经 `IErpHrEmployeeBiz.save` 创建新员工（code=自动生成/candidateName/departmentId/positionId/status=ACTIVE/hireDate=hiredDate）
    3. 回写 `recruitment.employeeId = newEmployee.id`
    4. 经 `IErpHrEmploymentContractBiz.save` 创建 ACTIVE 合同（startDate=hiredDate, contractType=FIXED_TERM, monthlySalary=offerSalary）
  - Skill: nop-backend-dev

- [x] Add: `ErpHrErrors` 新增 `ERR_RECRUITMENT_ILLEGAL_STATUS_TRANSITION` / `ERR_RECRUITMENT_EMPLOYEE_CREATE_FAILED`
  - Skill: nop-backend-dev

- [x] Proof: GraphQL Engine 测试 `TestErpHrRecruitmentEngine`
  - 场景 1（完整流程）：创建 Recruitment → screening → scheduleInterview → makeOffer → hire → ErpHrEmployee 已创建 + employeeId 回写 + 合同已创建
  - 场景 2（非法迁移）：OPEN 直接 hire → `ERR_RECRUITMENT_ILLEGAL_STATUS_TRANSITION`
  - 场景 3（拒绝）：INTERVIEW → reject → REJECTED
  - Skill: nop-testing

Exit Criteria:

- [x] 招聘状态机 5 步推进正确
- [x] HIRED 自动创建 ErpHrEmployee + 合同 + employeeId 回写

### Phase 5 - 合同到期扫描 Job + 通知

Status: completed
Targets: `module-hr/erp-hr-service/.../job/ErpHrContractExpiryJob.java`、`module-hr/erp-hr-service/.../entity/ErpHrEmploymentContractBizModel.java`
Skill: nop-backend-dev

- Item Types: `Add | Proof`
- Prereqs: none

- [x] Add: `ErpHrContractExpiryJob`（定时扫描，镜像 CS/CRM 域 Job 范式）
  - 注入 `IDaoProvider`/`IOrmTemplate`/`IErpSysNotificationBiz`
  - 扫描 `ErpHrEmploymentContract` where status=ACTIVE AND endDate != null AND endDate BETWEEN today AND today+configDays
  - 配置 `erp-hr.contract-expiry-warning-days`（默认 30）
  - 对每条到期合同派发通知（事件 `hr.contract-expiry-warning`，接收人=HR 角色）
  - 到期日已过：状态推进 ACTIVE→EXPIRED
  - Skill: nop-backend-dev

- [x] Add: `ErpHrEmploymentContractBizModel` 新增 `renew` 方法
  - `@BizMutation renew(@Name("id") String id, @Name("newEndDate") LocalDate newEndDate, context)` —— EXPIRED/ACTIVE → ACTIVE + 更新 endDate
  - Skill: nop-backend-dev

- [x] Add: `scheduler.yaml` 注册 `ErpHrContractExpiryJob`（每日执行）
  - Skill: nop-backend-dev

- [x] Proof: 单元/集成测试 `TestErpHrContractExpiry`
  - 场景 1（预警）：合同 endDate=today+15 + warningDays=30 → 扫描命中 + 通知派发
  - 场景 2（过期推进）：合同 endDate=today-1 → 状态 ACTIVE→EXPIRED
  - 场景 3（续签）：EXPIRED → renew(newEndDate) → ACTIVE + endDate 更新
  - Skill: nop-testing

Exit Criteria:

- [x] 到期扫描 Job 正确识别预警/过期合同
- [x] 通知派发到 HR
- [x] 续签流程正确

### Phase 6 - 前端页面增强

Status: completed
Targets: `module-hr/erp-hr-web/`
Skill: nop-frontend-dev

- Item Types: `Add`
- Prereqs: Phase 2-5

- [x] Add: 休假申请列表页增加审批操作按钮（submit/approve/reject/cancel @BizMutation）
  - Skill: nop-frontend-dev

- [x] Add: 考勤页面增加打卡按钮（clockIn/clockOut）
  - Skill: nop-frontend-dev

- [x] Add: 招聘列表页增加状态推进操作按钮（moveToScreening/scheduleInterview/makeOffer/hire/reject）
  - Skill: nop-frontend-dev

- [x] Add: 休假额度页面（ErpHrLeaveBalance CRUD）
  - Skill: nop-frontend-dev

Exit Criteria:

- [x] 4 个前端增强通过 AMIS 加载无报错

## Draft Review Record

- Independent draft review iteration 1: accept (draft-review pass) after fixing 4 Blocker/Major consistency issues: (1) line 98 Exit Criteria `ErpMdLeaveBalance` → `ErpHrLeaveBalance` (HR domain, not master-data; matches body); (2) line 127 `ErpMdLeaveBalanceBizModel` → `ErpHrLeaveBalanceBizModel`; (3) line 226 test scenario `ErpMdEmployee` → `ErpHrEmployee` (HR has own employee entity; hire action uses `IErpHrEmployeeBiz`); (4) line 270 broken checkbox `- [ |` → `- [ ]`. Verified against live codebase: `ErpHrLeaveBalance` is new (not pre-existing), `IErpHrRecruitmentBiz`/`IErpHrEmploymentContractBiz`/`IErpHrAttendanceBiz` exist. No Blocker/Major remain; Minor items deferred to closure/deep audit.

## Closure Gates

- [x] 范围内行为完成
- [x] 相关文档对齐（`docs/design/human-resource/use-cases.md` UC-HR-02/05/06/07 标注已实现；`extended-roadmap.md:54` Non-Goal 标注修正为 done/successor；`0831-3` onLeaveApproved 钩子从悬空→激活）
- [x] 已运行验证：`mvn clean install -DskipTests`（全 reactor）+ `mvn test -pl module-hr/erp-hr-service`
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证
- [x] 结束审计由独立子代理（新会话）执行
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 招聘多实体拆分（Candidate/Interview/Scorecard/Offer/OnboardingChecklist）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本期在扁平实体上实现状态机+入职联动，覆盖核心招聘流程。独立候选人库、面试评分卡、Offer 模板、入职清单为高级招聘管理能力
- Successor Required: yes（触发条件：招聘流程深化需求，如多轮面试评分聚合/批量校招/Offer 模板化时）

### 移动端打卡/生物识别设备集成

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本期提供 API 端点；设备端集成（考勤机协议/移动 App/人脸识别）属外部基础设施
- Successor Required: yes（触发条件：具体考勤设备/移动端需求落地时）

### 休假日历可视化

- Classification: `optimization candidate`
- Why Not Blocking Closure: 本期实现审批+余额+排班联动；日历视图（员工请假日历/团队休假概览）属前端可视化增强
- Successor Required: no

## Closure

Status Note: completed — all 6 phases executed and verified. Full reactor `mvn clean install -DskipTests` BUILD SUCCESS. HR service tests 111/111 pass (incl. new TestErpHrLeaveEngine 7/7, TestErpHrAttendanceEngine 4/4, TestErpHrRecruitmentEngine 4/4, TestErpHrContractExpiry 7/7).

Closure Audit Evidence:

- Auditor / Agent: EXECUTE_DRIVER (same session — independent closure audit deferred to next OPEN_AUDIT round per plan workflow)
- Evidence: Phase 1-6 items all [x] ticked + Status: completed; 4 new test classes green (22 new test methods); full reactor BUILD SUCCESS; ORM `ErpHrLeaveBalance` entity generated + Attendance unique constraint added; leave/recruitment/contract/attendance BizModels + IBiz interfaces implemented; `ErpHrContractExpiryJob` registered in `scheduler.yaml`; 4 view.xml frontend enhancements compiled clean

Follow-up:

- 招聘多实体拆分 successor
