# 人力资源管理域（human-resource）

## 目的

设计人力资源管理模块：员工主数据 → 劳动合同 → 考勤/工时 → 薪酬核算 → 社保/个税 → 休假管理 → 招聘流程，覆盖 ERP"人"维度的全生命周期管理。

## 边界

- 本模块负责：员工主数据、劳动合同、考勤记录、工时表、休假/请假管理、薪酬核算（含社保/个税）、招聘管理、排班与考勤排班、胜任力管理。
- **与 projects 的边界**：工时表（Timesheet）数据作为项目成本归集来源（projects 域 `cost-collection.md` 引用）。HR 域记录"谁花了多少时间"，projects 域归集"这些时间属于哪个项目"。
- **与 finance/expense-claim 的边界**：员工费用报销在 finance/expense-claim.md，HR 域负责费用报销中的员工数据校验。
- 本模块不负责：员工费用报销（finance/expense-claim 域）；培训/绩效/人才发展（远期扩展）；薪酬外部发放（银行接口属集成层）。
- 持久化字段、字典、状态码以 `module-hr/model/app-erp-hr.orm.xml` 为准。
- 跨域协作规则见 `../domain-design-guidelines.md`，全局流程见 `../flow-overview.md`。
- API 命名约定（审批动作集/状态迁移动词/参数命名；hr `markPaid` 唯一性见 §16A.4）见 `../domain-design-guidelines.md §16A`。

## 工程与模型

| 项 | 值 |
|----|----|
| Maven 工程 | `app-erp-hr` |
| appName | `erp-hr`（两级） |
| 权威模型 | `module-hr/model/app-erp-hr.orm.xml` |
| 实体包 | `app.erp.hr.dao.entity` |
| 表前缀 | `erp_hr_` |
| 类名前缀 | `ErpHr*` |
| 字典命名空间 | `erp-hr/*` |

## 核心业务对象

| 对象 | 业务含义 |
|------|----------|
| 员工（Employee） | 员工主记录：工号、姓名、证件与国籍、婚姻状况、联系方式、紧急联系人、部门/职位/直接上级、入职/试用期截止/转正/离职日期与离职原因、用工状态（在职/试用期/离职/解雇/退休）、用工类型、默认成本中心、工资银行账户、社保号与个税档案号、系统用户关联（可空） |
| 部门（Department） | 组织单元：上级部门、部门负责人、部门默认成本中心 |
| 职位（Position） | 岗位编制：所属部门、职级、职位类别 |
| 劳动合同（EmploymentContract） | 合同记录：合同类型（固定期限/无固定期限/项目制）、签订日期与起止日期、试用期月数、每周工时、年薪与月薪及币种、发放方式、社保与公积金基数 |
| 休假申请（LeaveRequest） | 请假单：假别（年假/病假/事假/婚假/产假/丧假/调休）、起止日期与天数（自动计算）、请假原因、审批人与审批时间 |
| 工时表（Timesheet） | 周期工时汇总：员工、周期起止、总工时、状态（草稿/提交/审批/驳回） |
| 工时明细（TimesheetLine） | 工时表逐日明细：工作日期、关联项目与任务、活动类型、小时数、工作内容（项目成本归集来源） |
| 考勤记录（Attendance） | 每日出勤：签到/签退、实际出勤时长（派生）、迟到/早退分钟、是否旷工、数据来源（打卡/指纹/移动）、关联休假 |
| 薪酬记录（Salary） | 月度薪资：基本工资、各项津贴、加班费、应发合计、社保/公积金/个税/其他扣款、实发合计、发放状态与发放日期 |
| 招聘记录（Recruitment） | 招聘流程：职位/部门/招聘人数、应聘者信息与简历、来源、状态（发布/筛选/面试/录用/录用入职/拒绝/关闭）、面试官与面试日期、offer 薪资、入职后关联员工 |

## 状态机

- 员工用工状态：`试用期 ↔ 在职`；`在职/试用期 → 离职/解雇（终态）`；`→ 退休（终态）`。
- 劳动合同：`生效 → 到期/解除/中止`。
- 休假申请：`草稿 → 提交 → 审批通过（终态）`；`提交 → 驳回`；`草稿/提交 → 取消`。
- 招聘：`发布 → 筛选 → 面试 → 录用 → 入职（终态，关联员工）`；`→ 拒绝/关闭（终态）`。

详细规则见 [`state-machine.md`](state-machine.md)。

## 跨域协作

| 协作场景 | 对端域 | 协作方式 |
|----------|--------|----------|
| 工时成本归集 | projects | Timesheet 工时作为项目成本归集来源 |
| 薪酬凭证过账 | finance/posting | 薪酬凭证经财务域标准过账（本域注册薪资相关 businessType） |
| 费用报销校验 | finance/expense-claim | 费用报销中的员工数据校验 |
| 部门/成本中心主数据 | master-data | 组织主数据引用 |

跨域调用走 `I*Biz` 接口，不做 ORM 层跨工程 `refEntityName`。

## 关键业务规则

1. **用工生命周期**：试用期→转正；在职→离职/解雇/退休（终态）。离职/退休员工的历史数据保留。
2. **薪酬核算为聚合逻辑**：薪酬（Salary）计算依赖考勤 + 休假 + 合同，但三者是独立实体，薪酬计算是独立的核算作业（不与考勤直接耦合）。
3. **中国本地化配置化**：社保比例/公积金比例/个税公式因城市而异，必须通过 `erp-hr` 配置表管理，不硬编码。
4. **工时驱动项目成本**：工时明细关联项目与任务，作为 projects 域成本归集来源。
5. **员工与系统用户分离**：员工（ErpHrEmployee）和系统用户（nop-auth User）是不同概念，员工可通过 userAccountId 关联系统账户，但 HR 数据维护独立于系统认证。
6. **个税起征点与加班上限**：个税起征点、月加班工时上限、每日标准工时均为可配置项。

## 业财过账

| businessType | 触发 | 借贷方向（典型） |
|-------------|------|-----------------|
| SALARY | 薪酬核算确认 | 借：管理费用-工资 / 贷：应付职工薪酬 |
| SALARY_PAYMENT | 薪酬发放 | 借：应付职工薪酬 / 贷：银行存款 |
| SOCIAL_INSURANCE | 社保缴纳 | 借：管理费用-社保 / 贷：银行存款（个人部分挂其他应收款） |

## 配置点

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `erp-hr.auto-generate-salary` | false | 是否每月自动生成薪酬（运行薪酬作业） |
| `erp-hr.social-insurance-rate` | — | 社保公司/个人比例（按城市配置） |
| `erp-hr.housing-fund-rate` | — | 公积金公司/个人比例 |
| `erp-hr.tax-threshold` | 5000 | 个税起征点 |
| `erp-hr.max-overtime-hours-per-month` | 36 | 月加班工时上限 |
| `erp-hr.default-work-hours-per-day` | 8 | 每日标准工时 |

## 菜单归属

hr 域 TOPM「人力资源管理」，分组：组织管理（部门、职位）、员工管理（员工主数据、劳动合同、部门调动）、考勤工时（考勤记录、工时表）、薪酬管理（薪酬核算、薪酬记录）、休假管理（休假申请、假期余额）、招聘管理（招聘计划、应聘者）。

## 反模式警示

- ⛔ **HR 与系统用户混为一谈**——员工和系统用户是不同概念，员工可有系统账户但 HR 数据维护独立于系统认证。
- ⛔ **中国本地化硬编码**——社保比例/个税公式因城市而异，必须通过配置表管理。
- ⛔ **薪酬与考勤耦合过紧**——考勤是原始数据，薪酬计算是聚合逻辑，两者独立实体。

## 日期范围命名变体（C3）

`ErpHrSocialInsuranceConfig` / `ErpHrSocialInsuranceBase` 使用 `effectiveFrom` / `effectiveTo` 表达记录有效期（薪酬档/社保配置的有效区间），属历史命名变体（非规范 `validFrom/validTo`），按 `docs/design/date-ranged-validity-pattern.md §Decision B` 不重命名。区间互斥校验的接入为后续 follow-up（薪酬档调整流程细化后授权）。

## 本域文档

| 文档 | 职责 |
|------|------|
| `README.md`（本文件） | 域概览、员工与薪酬模型、跨域协作 |
| `state-machine.md` | 员工/合同/休假/招聘状态机 |
| `payroll.md` | 薪酬核算规则 |
| `payroll-simulation.md` | 薪酬模拟 |
| `recruitment.md` | 招聘流程 |
| `shift-scheduling.md` | 排班管理 |
| `competency-management.md` | 胜任力管理 |
| `employee-survey.md` | 员工调研 |
| `ui-patterns.md` | 前端模式 |
| `use-cases.md` | 用例 |

## 参考

- `docs/analysis/erp-survey/2026-06-30-0000-axelor-open-suite.md` §HRMS（源码分析见 erp-survey）
- `docs/analysis/erp-survey/2026-06-30-0000-aureuserp.md` §HR
- `docs/design/projects/cost-collection.md`（工时成本归集）
- `docs/design/finance/posting.md`（薪资凭证过账）
- `docs/design/finance/expense-claim.md`（员工费用报销）
