# 人力资源管理域（human-resource）

## 目的

设计人力资源管理模块：员工主数据 → 劳动合同 → 考勤/工时 → 薪酬核算 → 社保/个税 → 休假管理 → 招聘流程。完善 ERP 的"人"维度的全生命周期管理。

## 边界

- 本模块负责：员工主数据、劳动合同、考勤记录、工时表、休假/请假管理、薪酬核算（含社保/个税）、招聘管理。
- **与 projects 的边界**：工时表（Timesheet）数据作为项目成本归集来源（projects 域的 cost-collection.md 引用）。HR 域记录"谁花了多少时间"，projects 域归集"这些时间属于哪个项目"。
- **与 finance/expense-claim 的边界**：员工费用报销在 finance/expense-claim.md，HR 域负责费用报销中的员工数据校验。
- 本模块不负责：员工费用报销（finance/expense-claim 域）；培训/绩效/人才发展（远期扩展）；薪酬外部发放（银行接口属集成层）。

## 设计依据

> 参考 **Axelor human-resource**（421 Java 文件）：Employee + EmploymentContract + Payroll + LeaveRequest + Timesheet + Expense 完整 HR 模块。
>
> 参考 **AureusERP**（employees 91 PHP + recruitments 113 PHP + time-off 103 PHP）。
>
> 参考 **Odoo hr** 系列（hr/payroll/attendance/holidays/recruitment）。
>
> 来源 `docs/analysis/erp-survey/2026-06-30-0000-axelor-open-suite.md` §HRMS。

## 实体清单

> 表前缀 `erp_hr_`、类名 `ErpHr*`、字典 `erp-hr/*`。

### ErpHrEmployee（员工主数据）

| 字段 | 含义 | 参考 |
|------|------|------|
| id/code/orgId | 标准（code 为工号） | — |
| firstName/lastName | 姓名（姓/名） | 🟢 Axelor Employee |
| fullName | 全名（派生） | — |
| gender | dict `erp-hr/gender`：MALE/FEMALE | — |
| birthDate/birthPlace | 出生日期/地点 | 🟢 Axelor Employee |
| idCardType/idCardNo | 证件类型/号码 | — |
| nationality | 国籍 | 🟢 Axelor Employee |
| maritalStatus | dict：SINGLE/MARRIED/DIVORCED | — |
| email/mobilePhone | 联系方式 | 🟢 Axelor Employee |
| addressId | 通讯地址 | — |
| emergencyContact/emergencyPhone | 紧急联系人 | — |
| departmentId | 部门（→ErpHrDepartment） | 🟢 Axelor Department |
| positionId | 职位（→ErpHrPosition） | — |
| jobTitle | 岗位名称 | — |
| superiorId | 直接上级（→ErpHrEmployee） | — |
| hireDate | 入职日期 | 🟢 Axelor Employee |
| probationEndDate | 试用期截止 | — |
| regularDate | 转正日期 | — |
| resignationDate | 离职日期 | — |
| resignationReason | 离职原因 | — |
| employmentStatus | dict `erp-hr/employment-status`：ACTIVE/PROBATION/RESIGNED/TERMINATED/RETIRED | 🟢 Axelor Employee.employeeStatus |
| employeeType | dict：FULL_TIME/PART_TIME/CONTRACTOR/INTERN | — |
| costCenterId | 默认成本中心 | — |
| bankAccountId | 工资卡银行账户 | 🟢 Axelor BankCard |
| socialSecurityNo | 社保号 | — |
| taxFileNo | 个税档案号 | — |
| userAccountId | 系统用户关联（→User，可空） | 🟢 Axelor Employee.user |
| 标准审计字段 | | |

**状态机**：`ACTIVE ↔ PROBATION（试用期）`；`ACTIVE/PROBATION → RESIGNED/TERMINATED（离职/解雇，终态）`；`→ RETIRED（退休，终态）`。

### ErpHrDepartment（部门）

| 字段 | 含义 |
|------|------|
| id/code/name/orgId | 标准 |
| parentId | 上级部门 |
| managerId | 部门负责人（→ErpHrEmployee） |
| costCenterId | 部门默认成本中心 |

### ErpHrPosition（职位）

| 字段 | 含义 |
|------|------|
| id/code/name/orgId | 标准 |
| departmentId | 所属部门 |
| jobGrade | 职级 |
| jobCategory | 职位类别 |

### ErpHrEmploymentContract（劳动合同）

| 字段 | 含义 | 参考 |
|------|------|------|
| id/code/employeeId/orgId | 标准 | 🟢 Axelor EmploymentContract |
| contractType | dict `erp-hr/contract-type`：FIXED_TERM（固定期限）/ OPEN_ENDED（无固定期限）/ PROJECT（项目制） | 🟢 Axelor EmploymentContract.contractType |
| signDate | 签订日期 | — |
| startDate/endDate | 合同起止日期 | 🟢 Axelor EmploymentContract |
| probationMonths | 试用期月数 | — |
| workingHoursPerWeek | 每周工时 | — |
| annualSalary | 年薪（税前） | 🟢 Axelor EmploymentContract.annualGrossSalary |
| monthlySalary | 月薪（税前） | — |
| salaryCurrencyId | 薪资币种 | — |
| salaryPayMethod | dict：BANK_TRANSFER/CASH | — |
| socialInsuranceBase | 社保基数 | — |
| housingFundBase | 公积金基数 | — |
| status | dict `erp-hr/contract-status`：ACTIVE/EXPIRED/TERMINATED/SUSPENDED | — |
| attachmentId | 合同文件 | — |

**状态机**：`ACTIVE → EXPIRED（到期）`；`ACTIVE → TERMINATED（解除）`；`ACTIVE → SUSPENDED（中止）`。

### ErpHrLeaveRequest（休假申请）

| 字段 | 含义 | 参考 |
|------|------|------|
| id/code/employeeId/orgId | 标准 | 🟢 Axelor LeaveRequest |
| leaveType | dict `erp-hr/leave-type`：ANNUAL（年假）/ SICK（病假）/ PERSONAL（事假）/ MARRIAGE（婚假）/ MATERNITY（产假）/ FUNERAL（丧假）/ COMPENSATORY（调休） | 🟢 Axelor LeaveRequest |
| startDate/endDate | 起止日期 | 🟢 Axelor LeaveRequest |
| durationDays | 天数（由起止日期自动计算） | 🟢 Axelor LeaveRequest |
| reason | 请假原因 | 🟢 Axelor LeaveRequest |
| status | dict `erp-hr/leave-status`：DRAFT/SUBMITTED/APPROVED/REJECTED/CANCELLED | — |
| approverId | 审批人（→ErpHrEmployee） | — |
| approvedAt | 审批时间 | — |

**状态机**：`DRAFT → SUBMITTED → APPROVED`（终态）；`SUBMITTED → REJECTED`（驳回）；`DRAFT/SUBMITTED → CANCELLED`。

### ErpHrTimesheet（工时表）

| 字段 | 含义 | 参考 |
|------|------|------|
| id/code/employeeId/orgId | 标准 | 🟢 Axelor Timesheet |
| periodFrom/periodTo | 周期（周/月） | 🟢 Axelor Timesheet |
| totalHours | 总工时 | 🟢 Axelor Timesheet |
| status | dict `erp-hr/timesheet-status`：DRAFT/SUBMITTED/APPROVED/REJECTED | 🟢 Axelor Timesheet |
| 标准审计字段 | | |

#### ErpHrTimesheetLine（工时明细）

| 字段 | 含义 |
|------|------|
| id/timesheetId/employeeId | 标准 |
| workDate | 工作日期 |
| projectId | 项目（→projects） |
| taskId | 任务（→projects，可空） |
| activityType | 活动类型（开发/测试/实施/管理） |
| hours | 小时数 |
| description | 工作内容 |

### ErpHrAttendance（考勤记录）

| 字段 | 含义 |
|------|------|
| id/employeeId/orgId | 标准 |
| date | 考勤日期 |
| clockIn/clockOut | 签到/签退时间 |
| workHours | 实际出勤时长（派生） |
| lateMinutes | 迟到分钟数 |
| earlyLeaveMinutes | 早退分钟数 |
| isAbsent | 是否旷工 |
| source | dict：CARD（打卡）/ BIOMETRIC（指纹）/ MOBILE（移动） |
| leaveRequestId | 关联休假（若当天有请假） |

### ErpHrSalary（薪酬记录）

| 字段 | 含义 |
|------|------|
| id/employeeId/orgId | 标准 |
| year/month | 年份/月份 |
| basicSalary | 基本工资 |
| positionAllowance | 岗位津贴 |
| performanceBonus | 绩效奖金 |
| overtimePay | 加班费 |
| mealAllowance | 餐补 |
| transportAllowance | 交通补贴 |
| otherAllowance | 其他补贴 |
| grossSalary | 应发合计（=Σ 各项） |
| socialInsurance | 社保个人部分 |
| housingFund | 公积金个人部分 |
| taxAmount | 个税 |
| otherDeductions | 其他扣款 |
| netSalary | 实发合计（=gross - 各项扣款） |
| paymentStatus | dict：PENDING/PAID/VOID |
| paymentDate | 实发日期 |
| remark | 备注 |

### ErpHrRecruitment（招聘记录）

| 字段 | 含义 | 参考 |
|------|------|------|
| id/code/orgId | 标准 | 🟢 AureusERP recruitments |
| positionId | 招聘职位（→ErpHrPosition） | — |
| departmentId | 招聘部门 | — |
| headcount | 招聘人数 | — |
| candidateName | 应聘者姓名 | 🟢 AureusERP recruitments |
| candidatePhone/candidateEmail | 联系方式 | — |
| source | dict：RECRUITMENT_WEBSITE/AGENCY/REFERRAL/CAMPUS | — |
| resumeAttachmentId | 简历附件 | — |
| status | dict `erp-hr/recruitment-status`：OPEN/SCREENING/INTERVIEW/OFFERED/HIRED/REJECTED/CLOSED | 🟢 AureusERP recruitments |
| interviewerId | 面试官 | — |
| interviewDate | 面试日期 | — |
| offerSalary | 面试薪资 | — |
| hiredDate | 入职日期（HIRED 时） | — |
| employeeId | 关联员工（HIRED 后关联 ErpHrEmployee） | — |

**状态机**：`OPEN → SCREENING → INTERVIEW → OFFERED → HIRED`（终态，关联员工）；`→ REJECTED`（终态）；`→ CLOSED`（终态）。

## 业财过账

| businessType | 触发 | 借贷方向（典型） |
|-------------|------|-----------------|
| SALARY | 薪酬核算确认 | 借：管理费用-工资 / 贷：应付职工薪酬 |
| SALARY_PAYMENT | 薪酬发放 | 借：应付职工薪酬 / 贷：银行存款 |
| SOCIAL_INSURANCE | 社保缴纳 | 借：管理费用-社保 / 贷：银行存款（+ 个人部分挂其他应收款） |

HR 域的薪资凭证通过 `IErpFinAcctDocProvider` 注册 `SALARY/SALARY_PAYMENT/SOCIAL_INSURANCE` 三类 businessType 走标准过账流程。

## 跨域协作

| 对端 | 协作方式 |
|------|---------|
| projects（cost-collection） | Timesheet 工时作为项目成本归集来源 |
| finance/posting | 薪酬凭证走 IErpFinAcctDocProvider SPI |
| finance/expense-claim | 费用报销中员工验证 |
| master-data（organization） | 部门/成本中心主数据 |

## 配置点

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `erp-hr.auto-generate-salary` | false | 是否每月自动生成薪酬（运行 SalaryJob） |
| `erp-hr.social-insurance-rate` | — | 社保公司/个人比例（按城市配置） |
| `erp-hr.housing-fund-rate` | — | 公积金公司/个人比例 |
| `erp-hr.tax-threshold` | 5000 | 个税起征点 |
| `erp-hr.max-overtime-hours-per-month` | 36 | 月加班工时上限 |
| `erp-hr.default-work-hours-per-day` | 8 | 每日标准工时 |

## 菜单归属

新增 hr 域 TOPM「人力资源管理」，分组：
- 组织管理：部门、职位
- 员工管理：员工主数据、劳动合同、部门调动（`ErpHrEmployeeBizModel.transferEmployee` 已落地，UC-HR-08）
- 考勤工时：考勤记录、工时表
- 薪酬管理：薪酬核算、薪酬记录
- 休假管理：休假申请、假期余额
- 招聘管理：招聘计划、应聘者

## 反模式警示

- ⛔ **HR 与系统用户混为一谈**——员工（ErpHrEmployee）和系统用户（nop-auth User）是不同概念，员工可以有系统账户（通过 userAccountId 关联），但 HR 数据的维护独立于系统认证。
- ⛔ **中国本地化硬编码**——社保比例/个税公式因城市而异，必须通过 erp-hr 配置表管理，不硬编码 Java。
- ⛔ **薪酬与考勤耦合过紧**——考勤（ErpHrAttendance）是原始数据，薪酬（ErpHrSalary）计算依赖于考勤+休假+合同，但两者是独立实体，薪酬计算是 Job 聚合逻辑。

### 日期范围有效性（C3 交叉引用）

`ErpHrSocialInsuranceConfig` / `ErpHrSocialInsuranceBase` 使用 `effectiveFrom` / `effectiveTo` 字段表达记录有效期（薪酬档/社保配置的有效区间）。该命名属于历史变体（非规范 `validFrom/validTo`），按 `docs/design/date-ranged-validity-pattern.md §Decision B` **不重命名**：

- 接入区间互斥校验时，调用方（hr-service BizModel）在 `defaultPrepareSave/Update` 中构造匿名 `IDateRange` 适配器包装 `effectiveFrom/effectiveTo`，再调 `ErpDateRangeOverlapValidator.enforceMutex`
- helper（`ErpDateRanges` / `ErpDateRangeOverlapValidator`）位于 `erp-md-service/daterange/`，hr-service 经 `app-erp-master-data-service` 依赖可达
- **接入触发条件**：薪酬档调整流程细化 + hr-service owner doc 授权（本计划未试点，归 follow-up）

## 证据强度标注

| 证据 | 强度 | 说明 |
|------|------|------|
| 员工主数据 + 合同 + 薪酬 | 🟢 | Axelor `Employee.xml` + `EmploymentContract.xml` + `Payroll.xml` 421 Java 文件 |
| 休假申请（LeaveRequest） | 🟢 | Axelor `LeaveRequest.xml`（含审批流/假期类型/时长计算） |
| 工时表（Timesheet） | 🟢 | Axelor `Timesheet.xml` + React 前端 |
| 招聘管理 | 🟢 | AureusERP recruitments（113 PHP 文件完整招聘流） |
| 考勤管理（Attendance） | 🟢 | Odoo `hr_attendance` 源码 |
| 社保/个税/公积金（中国本地化） | ⚪ | 中国劳动法领域常识（Axelor 为法国社保体系，需独立设计） |

## 参考

- `docs/analysis/erp-survey/2026-06-30-0000-axelor-open-suite.md` §HRMS
- `docs/analysis/erp-survey/2026-06-30-0000-aureuserp.md` §HR
- `docs/design/projects/cost-collection.md`（工时成本归集）
- `docs/design/finance/posting.md`（薪资凭证 IErpFinAcctDocProvider）
- `docs/design/finance/expense-claim.md`（员工费用报销）
