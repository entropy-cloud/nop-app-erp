# 人力资源管理域用例

## UC-HR-01 员工入职

| 项目 | 内容 |
|------|------|
| **概述** | HR 录入新员工信息，完成员工主数据创建并按需分配系统账号 |
| **触发条件** | 员工签署劳动合同 / 通过招聘流程 HIRED 后 |
| **前置条件** | ErpHrDepartment、ErpHrPosition 已存在；如有合同需提前配置合同模板 |
| **基本流程** | 1. HR 填写员工信息（姓名/性别/出生日期/证件/联系方式/银行账户等）<br>2. 选择部门（→ErpHrDepartment）和职位（→ErpHrPosition）<br>3. 设置直属上级（→ErpHrEmployee.superiorId）<br>4. 填写入职日期、试用期截止日期<br>5. 设置雇佣状态为 PROBATION（试用期）或 ACTIVE（免试用）<br>6. 填写社保号/个税档案号<br>7. 创建劳动合同（ErpHrEmploymentContract）并关联<br>8. 提交，系统创建 ErpHrEmployee 记录 |
| **后置条件** | 员工主数据可用；可选创建系统账号（UserAccountId） |
| **异常** | 部门/职位不存在时提示先创建；证件号码重复提示 |
| **跨域协作** | 无（纯 HR 域操作） |

## UC-HR-02 休假申请流程

| 项目 | 内容 |
|------|------|
| **概述** | 员工提交休假申请，经审批后生效，扣减假期余额并联动考勤 |
| **触发条件** | 员工需要请假 |
| **前置条件** | ErpHrEmployee 状态为 ACTIVE 或 PROBATION；假期余额充足 |
| **基本流程** | 1. 员工创建 LeaveRequest，选择休假类型（ANNUAL/SICK/etc.）<br>2. 填写起止日期、原因，系统自动计算 durationDays<br>3. 提交 → status = SUBMITTED<br>4. 审批人收到待办，审批通过（→APPROVED）或驳回（→REJECTED）<br>5. 若 APPROVED 扣减假期余额，通知考勤模块在该时段标记为休假<br>6. 若 REJECTED 员工可修改后重新提交 |
| **后置条件** | 假期余额已更新；考勤记录已联动标记 |
| **异常** | 余额不足禁止提交；日期重叠校验；审批人超时自动转派 |
| **跨域协作** | 考勤（ErpHrAttendance.leaveRequestId 关联） |

## UC-HR-03 工时表提交

| 项目 | 内容 |
|------|------|
| **概述** | 员工按周/月填报工时明细，经项目经理审批后归集到项目成本 |
| **触发条件** | 工时周期结束 / 员工定期填报 |
| **前置条件** | ErpHrEmployee 状态为 ACTIVE；项目（projects）和任务（tasks）已存在 |
| **基本流程** | 1. 员工创建 Timesheet，选择周期起止<br>2. 添加 TimesheetLine 每天分项目/任务/活动类型记录小时数<br>3. 提交 → status = SUBMITTED<br>4. 项目经理审批 → APPROVED（工时归集到 projects域 cost-collection）<br>5. 或驳回 → REJECTED，员工修改后重新提交 |
| **后置条件** | 工时数据已归集到项目成本；totalHours 已汇总 |
| **异常** | 同一日工时超过 24h 校验；项目工时段落不可重叠（可选） |
| **跨域协作** | projects/cost-collection 订阅 Timesheet APPROVED 事件 |

## UC-HR-04 薪酬核算

| 项目 | 内容 |
|------|------|
| **概述** | 月度薪酬计算 基础工资+津贴+绩效+加班费-社保-公积金-个税=实发 |
| **触发条件** | 每月固定日期 / 手动触发 |
| **前置条件** | 考勤数据已到位；合同中的薪资信息已配置；社保比例已配置 |
| **基本流程** | 1. SalaryJob 读取当月所有 ACTIVE 员工的考勤/休假/合同数据<br>2. 计算基本工资（按合同月薪 + 当月出勤比例）<br>3. 加班费（考勤 overtimeMinutes × 加班费率）<br>4. 绩效奖金（从绩效模块读取或手动录入）<br>5. 计算社保个税平台应扣金额<br>6. 生成 ErpHrSalary 记录（paymentStatus = PENDING）<br>7. HR 审核薪酬表，确认后发薪 |
| **后置条件** | ErpHrSalary 记录生成；paymentStatus 可更新为 PAID |
| **异常** | 员工缺失合同或薪资配置时跳过并告警；社保计算因城市差异需配置 |
| **跨域协作** | 销售/采购（读取绩效奖金）；考勤（读取缺勤/加班数据）；财务过账（SALARY 凭证） |

## UC-HR-05 招聘录用

| 项目 | 内容 |
|------|------|
| **概述** | 从简历筛选到面试、发 Offer、入职的完整招聘流程 |
| **触发条件** | 部门提出招聘需求 |
| **前置条件** | ErpHrPosition、ErpHrDepartment 已存在 |
| **基本流程** | 1. 创建 Recruitment，关联职位和部门<br>2. 填写应聘者信息<br>3. 简历筛选（status = SCREENING）<br>4. 安排面试（status = INTERVIEW）<br>5. 面试通过后发 Offer（status = OFFERED）<br>6. 候选人接受，入职（status = HIRED）→ 创建 ErpHrEmployee<br>7. 或拒绝/拒绝候选人（status = REJECTED）<br>8. 岗位关闭（status = CLOSED） |
| **后置条件** | ErpHrEmployee 已创建（HIRED 时）；该 record 的 employeeId 已关联 |
| **异常** | 候选人接受 Offer 后未到岗需状态回退 |
| **跨域协作** | 无（纯 HR 域） |

## UC-HR-06 考勤跟踪

| 项目 | 内容 |
|------|------|
| **概述** | 每日打卡签到/签退，系统自动计算迟到、早退、旷工 |
| **触发条件** | 员工打卡/签退 |
| **前置条件** | ErpHrEmployee 状态为 ACTIVE 或 PROBATION |
| **基本流程** | 1. 系统从打卡机/移动端获取签到时间<br>2. 创建/更新 ErpHrAttendance 记录 clockIn、clockOut<br>3. 根据排班规则计算 workHours、lateMinutes、earlyLeaveMinutes<br>4. 当天未打卡且无请假记录则 isAbsent = true<br>5. 若当天有已批准的 LeaveRequest，关联 leaveRequestId |
| **后置条件** | 考勤记录写入；迟到/早退/旷工可被薪资计算引用 |
| **异常** | 多次打卡以最后一次为准；跨天打卡处理；设备故障时支持手工补卡 |
| **跨域协作** | 休假（LeaveRequest 联动）；薪酬（考勤数据作为薪资输入） |

## UC-HR-07 合同到期提醒

| 项目 | 内容 |
|------|------|
| **概述** | 劳动合同到期前自动提醒 HR，支持续签或终止 |
| **触发条件** | 定时任务每日扫描即将到期的合同 |
| **前置条件** | ErpHrEmploymentContract.status = ACTIVE，endDate 为未来 30/60/90 天内 |
| **基本流程** | 1. 系统扫描 endDate 在提醒窗口内的 ACTIVE 合同<br>2. 通知 HR 管理员<br>3. HR 操作续签（创建新合同，原合同 endDate 不变但 status→EXPIRED）<br>4. 或到期终止（原合同 status→EXPIRED，员工 employmentStatus 联动） |
| **后置条件** | 新合同已创建或员工状态已变更 |
| **异常** | 连续合同次数到达无固定期限条件时系统提示 |
| **跨域协作** | 员工状态联动（合同到期不续签→RESIGNED） |

## UC-HR-08 部门调动

| 项目 | 内容 |
|------|------|
| **概述** | 员工跨部门调转，更新部门/职位/上级信息 |
| **触发条件** | 部门主管/HR 发起调动申请 |
| **前置条件** | 目标部门和职位已存在 |
| **基本流程** | 1. HR 选择员工，填写目标部门（—ErpHrDepartment）<br>2. 可选调整职位与直属上级<br>3. 设置调动生效日期<br>4. 提交，系统更新 ErpHrEmployee.departmentId/positionId/superiorId<br>5. 如有劳动合同，标记原合同→TERMINATED，创建新合同 |
| **后置条件** | 员工新部门/职位已生效 |
| **异常** | 调动日期与已有休假冲突时告警 |
| **跨域协作** | 成本中心变更可能影响项目工时归集 |

> **实现状态**：已落地 `ErpHrEmployeeBizModel.transferEmployee` `@BizMutation`（单步直接更新，无审批；经 `IErpHrDepartmentBiz`/`IErpHrPositionBiz`/`IErpHrEmploymentContractBiz`/`IErpHrLeaveRequestBiz` 跨实体校验/联动；合同处理三态 `handleContract` AUTO/YES/NO + config-gated；休假冲突告警不阻塞；AMIS 员工页「调动」drawer 入口）。调动单实体 + 审批工作流归 Deferred（触发条件=调动需人工审批留痕或批量调动报表时，经独立 ORM ask-first 计划承接）。详见 `docs/plans/2026-07-08-0517-2-hr-employee-transfer-uc-hr-08.md`。

## UC-HR-09 排班管理

| 项目 | 内容 |
|------|------|
| **概述** | 定义班次模板，为员工分配排班，支持轮换排班、排班调换申请与审批，休假自动联动标记缺席 |
| **触发条件** | HR/排班管理员需要配置班次或为员工排班 |
| **前置条件** | ErpHrEmployee 主数据已存在 |
| **基本流程** | 1. HR 创建班次模板（ErpHrShift），配置起止时间、宽容期、是否需打卡等<br>2. 为员工分配排班（ErpHrShiftAssignment），支持单个/批量/按轮换模板分配<br>3. 员工可发起排班调换申请（ErpHrShiftSwapRequest），经审批后交换班次<br>4. 休假审批通过后自动关联并标记排班为缺席（isAbsent=true）<br>5. 排班数据作为考勤迟到/早退/缺勤计算的标准输入 |
| **后置条件** | 排班已分配；考勤模块可读取排班数据计算迟到/早退 |
| **异常** | 同一员工同一天重复排班时拦截；调换申请目标员工已有冲突排班时拒绝 |
| **跨域协作** | ErpHrAttendance（考勤迟到/早退基于排班计算）；ErpHrLeaveRequest（休假联动标记缺席） |

## UC-HR-10 薪酬模拟

| 项目 | 内容 |
|------|------|
| **概述** | 复制上期薪酬数据创建模拟版本，修改后对比差异，预览实发变化，经审批后转为正式薪酬核算 |
| **触发条件** | HR 薪酬专员在调薪或调整津贴前需模拟影响 |
| **前置条件** | 上期/当前期薪酬数据已存在（ErpHrSalary） |
| **基本流程** | 1. HR 选择源薪酬期间创建模拟（ErpHrSalarySimulation.status=DRAFT）<br>2. 系统复制每位员工的薪酬项目行、累计个税、考勤快照数据<br>3. HR 调整薪酬项目（基本工资/津贴/绩效/出勤天数），即时应变计算<br>4. 提交审核（status=IN_REVIEW），审批人审核（APPROVED/REJECTED）<br>5. 审批通过后转正式（CONVERTED），创建正式 ErpHrSalary 进入支付流程 |
| **后置条件** | 模拟版本已审批通过并转化为正式薪酬记录（paymentStatus=PENDING） |
| **异常** | 目标期间已有 PAID 正式薪酬时不允许转正式 |
| **跨域协作** | ErpHrSalary（CONVERTED 时创建正式薪酬）；ErpHrSalaryItem（复用薪酬项目定义） |

## UC-HR-11 员工调研

| 项目 | 内容 |
|------|------|
| **概述** | HR 创建问卷模板（含题库），发布调研，员工填写（支持匿名），系统自动聚合结果并提供趋势/部门对比/驱动因子分析 |
| **触发条件** | HR 需要开展敬业度调研、脉搏调研或 eNPS 调查 |
| **前置条件** | ErpHrEmployee 主数据已存在；可选配置驱动因子分类 |
| **基本流程** | 1. HR 创建 ErpHrSurvey（选择调研类型 ANNUAL_ENGAGEMENT/PULSE/eNPS/ADHOC）<br>2. 添加 ErpHrSurveyQuestion（评分题/选择题/开放题），按题型配置评分范围和选项<br>3. 可选关联驱动因子分类（GROWTH/RECOGNITION/MANAGEMENT/WELLBEING/ALIGNMENT）<br>4. 设置匿名模式、目标部门/员工和起止日期<br>5. 发布（status→OPEN），系统通知目标员工填写<br>6. 员工通过系统填写提交（ErpHrSurveyResponse + ErpHrSurveyAnswer）<br>7. 匿名模式下 employeeId 不存储，仅存 respondentHash 防重复<br>8. 截止后 status→CLOSED，自动聚合 ErpHrSurveyResult<br>9. HR 查看结果仪表盘：评分趋势、部门对比、eNPS 得分、驱动因子分析 |
| **后置条件** | ErpHrSurveyResult 已聚合可供分析 |
| **异常** | 同一员工重复提交匿名问卷被 respondentHash 拦截；问卷发布后不可再编辑题目（可新建版本） |
| **跨域协作** | 无（纯 HR 域操作） |

## UC-HR-12 胜任力管理与评估

| 项目 | 内容 |
|------|------|
| **概述** | 维护胜任力字典（技能/行为/知识）及等级锚定，定义岗位-胜任力矩阵，进行员工多视角评估，产出差距分析并生成发展计划 |
| **触发条件** | HR 需要建立胜任力体系或对员工进行能力评估 |
| **前置条件** | ErpHrPosition、ErpHrEmployee 主数据已存在 |
| **基本流程** | 1. HR 创建 ErpHrCompetency（分类 SKILL/BEHAVIOR/KNOWLEDGE），配置能力组和层级结构<br>2. 为每个胜任力配置 ErpHrCompetencyLevel（1-5 级，含行为锚定描述）<br>3. 配置 ErpHrRoleCompetency（每岗位所需胜任力及要求等级、权重、是否关键）<br>4. HR 发起评估周期，创建 ErpHrEmployeeAssessment（SELF/MANAGER/PEER/SUBORDINATE/360）<br>5. 各评估人独立填写 ErpHrAssessmentDetail（对每个胜任力打分 + 评语）<br>6. 全部提交后系统按权重聚合（默认 SELF 15%/MANAGER 50%/PEER 25%/SUBORDINATE 10%）<br>7. 自动对比 ErpHrRoleCompetency 计算 ErpHrGapAnalysis（gapValue = requiredLevel - actualLevel）<br>8. 标记 gapSeverity（NONE/MINOR/MODERATE/CRITICAL）<br>9. 针对 CRITICAL/MODERATE 差距生成 ErpHrDevelopmentPlan 建议<br>10. HR 审核并调整发展计划项，指定目标等级、发展行动和导师 |
| **后置条件** | 差距分析已生成；发展计划已创建并可跟踪执行 |
| **异常** | 同一员工同类型评估重复提交时校验；缺少岗位胜任力配置时跳过差距分析 |
| **跨域协作** | 发展计划中的 trainingCourseId 远期关联培训模块 |
