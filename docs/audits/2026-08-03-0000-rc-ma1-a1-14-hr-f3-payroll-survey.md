# A1.14 hr-F3 薪酬与调研 需求-实现符合性审计报告（rc-ma1-a1-14）

> Mission: requirement-compliance · Work Item: A1.14（UC-HR-03 工时表提交 + UC-HR-04 薪酬核算 + UC-HR-10 薪酬模拟 + UC-HR-11 员工调研）
> 来源计划: `docs/plans/2026-08-02-2250-3-rc-ma1-a1-14-hr-f3-payroll-survey.md`
> 方法论: `docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议）
> 审计类型: 只读审计（无代码/ORM/api.xml/view.xml/真相源变更）
> 审计日期: 2026-08-03

## 9. 与既有 MA2/A4.4 报告的差异增量声明（前置）

本报告是 **requirement-compliance** mission MA1 切片 A1.14 的五级追踪审计，视角 = **需求契约（L1 use-cases）→ 实现符合性**。按 §去重协议，以下既有审计已证实的结论本报告**直接复用，不重审**：

- **A2.7b**（`docs/audits/2026-07-28-0230-arm-ma2-hr-attendance-payroll-state-machine.md`）：hr 考勤与工资八组件状态机迁移守卫齐全（工资支付轴 PENDING→PAID/VOID 双守卫 / 仿真 5 态全迁移 / convertToFormal 双层容错 / markPaid 经 `IErpFinVoucherBiz.post()` REQUIRES_NEW Facade 跨域写零 `daoFor(ErpFin*)` 直写）。本报告复用其 L5 行为证据。
- **A4.4**（`docs/audits/2026-07-29-0430-arm-ma4-hr-code-quality.md`）：薪酬/过账/模拟引擎链路代码质量七面扎实（社保基数钳制 / 累计预扣编排 / BigDecimal 货币类型安全 / 模拟 What-If 克隆隔离 / 跨域 Facade / 异常规范化），并登记 P1-MA4-016/017/018/019 + P2-MA4-008/009。

本报告**只补需求视角差异**：(i) UC-HR-03 工时表 24h 校验 + totalHours 汇总 + approve/reject + cost-collection 归集的需求符合性；(ii) UC-HR-04 薪酬核算公式完整性 + 业财过账 SALARY 凭证的需求符合性；(iii) UC-HR-10 模拟 What-If + convertToFormal 的需求符合性；(iv) UC-HR-11 调研匿名 respondentHash + 聚合 + eNPS + 驱动因子的需求符合性；(v) **resolved finding HEAD 复核（R1.15/R1.16/R1.26/R2.13）落地确认**——其中 P1-MA4-017（会计正确性类 Q4 关键证据）HEAD 复核发现 audit-remediation 侧的「方案B Deferred」关闭在 requirement-compliance Q4=(a) 下**不成立**，须按 §10 经 MR1（RC-R1.n）实现。

---

## 1. 需求契约原文（L1，逐字引用）

> 真相源：`docs/design/human-resource/use-cases.md`（层级 2 功能契约，§4 真相源层级）。以下逐字引用，不转述。

### UC-HR-03 工时表提交（`use-cases.md:27`）

| 项目 | 原文 |
|------|------|
| 概述 | 员工按周/月填报工时明细，经项目经理审批后归集到项目成本 |
| 基本流程 | 1. 员工创建 Timesheet，选择周期起止<br>2. 添加 TimesheetLine 每天分项目/任务/活动类型记录小时数<br>3. 提交 → status = SUBMITTED<br>4. 项目经理审批 → APPROVED（工时归集到 projects域 cost-collection）<br>5. 或驳回 → REJECTED，员工修改后重新提交 |
| 后置条件 | 工时数据已归集到项目成本；totalHours 已汇总 |
| 异常 | **同一日工时超过 24h 校验**；项目工时段落不可重叠（可选） |
| 跨域协作 | projects/cost-collection 订阅 Timesheet APPROVED 事件 |

### UC-HR-04 薪酬核算（`use-cases.md:39`）

| 项目 | 原文 |
|------|------|
| 概述 | 月度薪酬计算 基础工资+津贴+绩效+加班费-社保-公积金-个税=实发 |
| 基本流程 | 1. SalaryJob 读取当月所有 ACTIVE 员工的考勤/休假/合同数据<br>2. 计算基本工资（按合同月薪 + 当月出勤比例）<br>3. 加班费（考勤 overtimeMinutes × 加班费率）<br>4. 绩效奖金（从绩效模块读取或手动录入）<br>5. 计算社保个税平台应扣金额<br>6. 生成 ErpHrSalary 记录（paymentStatus = PENDING）<br>7. HR 审核薪酬表，确认后发薪 |
| 后置条件 | ErpHrSalary 记录生成；paymentStatus 可更新为 PAID |
| 异常 | 员工缺失合同或薪资配置时跳过并告警；社保计算因城市差异需配置 |
| 跨域协作 | 销售/采购（读取绩效奖金）；考勤（读取缺勤/加班数据）；财务过账（SALARY 凭证） |

### UC-HR-10 薪酬模拟（`use-cases.md:113`）

| 项目 | 原文 |
|------|------|
| 概述 | 复制上期薪酬数据创建模拟版本，修改后对比差异，预览实发变化，经审批后转为正式薪酬核算 |
| 基本流程 | 1. HR 选择源薪酬期间创建模拟（ErpHrSalarySimulation.status=DRAFT）<br>2. 系统复制每位员工的薪酬项目行、累计个税、考勤快照数据<br>3. HR 调整薪酬项目（基本工资/津贴/绩效/出勤天数），即时应变计算<br>4. 提交审核（status=IN_REVIEW），审批人审核（APPROVED/REJECTED）<br>5. 审批通过后转正式（CONVERTED），创建正式 ErpHrSalary 进入支付流程 |
| 异常 | **目标期间已有 PAID 正式薪酬时不允许转正式** |

### UC-HR-11 员工调研（`use-cases.md:125`）

| 项目 | 原文 |
|------|------|
| 概述 | HR 创建问卷模板（含题库），发布调研，员工填写（支持匿名），系统自动聚合结果并提供趋势/部门对比/驱动因子分析 |
| 基本流程 | 1. HR 创建 ErpHrSurvey（选择调研类型 ANNUAL_ENGAGEMENT/PULSE/eNPS/ADHOC）<br>2. 添加 ErpHrSurveyQuestion（评分题/选择题/开放题），按题型配置评分范围和选项<br>3. 可选关联驱动因子分类（GROWTH/RECOGNITION/MANAGEMENT/WELLBEING/ALIGNMENT）<br>4. 设置匿名模式、目标部门/员工和起止日期<br>5. 发布（status→OPEN），系统通知目标员工填写<br>6. 员工通过系统填写提交（ErpHrSurveyResponse + ErpHrSurveyAnswer）<br>7. **匿名模式下 employeeId 不存储，仅存 respondentHash 防重复**<br>8. 截止后 status→CLOSED，**自动聚合 ErpHrSurveyResult**<br>9. HR 查看结果仪表盘：评分趋势、部门对比、eNPS 得分、驱动因子分析 |
| 异常 | **同一员工重复提交匿名问卷被 respondentHash 拦截**；问卷发布后不可再编辑题目（可新建版本） |

---

## 2. 实现证据（L3，`file:line`，含跨域调用链）

### UC-HR-03 工时表
- `module-hr/erp-hr-service/.../entity/ErpHrTimesheetBizModel.java:35-47` — 仅 `submit`（DRAFT→SUBMITTED，常量已替换为 `ErpHrConstants.TIMESHEET_STATUS_DRAFT/SUBMITTED`，P1-MA2-044 resolved）。**无 approve/reject mutation**。
- `module-hr/erp-hr-service/.../entity/ErpHrTimesheetLineBizModel.java` — 18 行 CRUD 桩（`extends CrudBizModel<ErpHrTimesheetLine>`，零业务方法）。
- `module-hr/model/app-erp-hr.orm.xml:592-593` — `totalHours` 列 + `status` 列（ext:dict=`erp-hr/timesheet-status`）存在；**无代码 setTotalHours / 24h 校验**（grep `totalHours\|24` 零业务命中）。
- 跨域 cost-collection 归集：`projects` 域 `TimesheetPostingDispatcher`（P1-MA2-068，归 projects 切片，本切片核验 hr 侧 SUBMITTED/APPROVED 触发——**hr 侧无 APPROVED 触发点**）。

### UC-HR-04 薪酬核算
- `module-hr/erp-hr-service/.../entity/ErpHrSalaryBizModel.java:80-99` — `calculateSalary:80` / `runPayroll:89` / `markPaid:97`（委托 Processor）；`voidSalary:103` / `generateBankFile:116`。
- `module-hr/erp-hr-service/.../processor/ErpHrSalaryCalculateSalaryProcessor.java` + `ErpHrSalaryRunPayrollProcessor.java` + `ErpHrSalaryMarkPaidProcessor.java` — per-mutation 拆分（R6.1）。
- `module-hr/erp-hr-service/.../payroll/PayrollCalculator.java:54-159` — 公式完整：基本工资 `monthlySalary × attendanceRatio:86` + 加班费 `overtimeHours × DEFAULT_OVERTIME_HOURLY_RATE:96` + 绩效（手工录入 0，:100）+ 社保 EE/ER（:108-110）+ 公积金 EE/ER（:113-115）+ 个税累计预扣（:119）+ 实发 `gross - socialEE - fundEE - tax - other:127`；paymentStatus=PENDING（:149）。**公司承担 socialInsuranceER/housingFundER 计算后丢弃**（:110/:115 局部变量，:156-157 注释声称「暂存 remark」但**无 setRemark 调用**）。
- `module-hr/erp-hr-service/.../payroll/IncomeTaxCalculator.java:69-115` — 累计预扣法编排；`resolveBracket:228-242` **末档 null 防御已落地**（:231-234 `if (b.getRangeUpperLimit() == null) { selected = b; break; }`，P1-MA4-016 resolved）；`parseCumulativeData:169-192` **静默吞已移除**（:184-190 LOG.warn + 抛 `ERR_HR_CUMULATIVE_DATA_CORRUPT`，P1-MA4-018 resolved）。
- `module-hr/erp-hr-service/.../payroll/SocialInsuranceCalculator.java` — 基数钳制 min/max + 公积金回退基数（A4.4 已证实）。
- 业财过账链路：`module-hr/erp-hr-service/.../posting/SalaryPostingDispatcher.java` — `tryPostPayment:88`（SALARY_PAYMENT 280，markPaid 触发，已接 IErpSysNotificationBiz 告警 :105-125，P1-MA2-048 resolved）；`tryPostAccrual:67`（SALARY 计提 270，**死代码零调用方**，javadoc :35-38/:63-65 自述）；`buildAccrualEvent:127` 仅组装 SALARY（270），**无 tryPostSocialInsuranceER/tryPostHousingFundER，290/300 event 永不生成**。跨域经 `SalaryPostingExecutor` → `IErpFinVoucherBiz.post()` REQUIRES_NEW Facade。
- 配置：`ErpHrTaxConfigBizModel` / `ErpHrSocialInsuranceConfigBizModel` / `ErpHrSocialInsuranceBaseBizModel` / `ErpHrTaxSpecialDeductionBizModel`（税率/社保配置）。

### UC-HR-10 薪酬模拟
- `module-hr/erp-hr-service/.../entity/ErpHrSalarySimulationBizModel.java:83-346` — `createSimulation:83` / `adjustItem:96` / `getSimulatedSalary:107`（What-If 经 `payrollCalculator.recalculateWithOverrides:117` 克隆源快照不污染）/ `submitForReview:287`（DRAFT→IN_REVIEW 守卫 :290）/ `approve:307`（IN_REVIEW→APPROVED 守卫 :311）/ `reject:326`（IN_REVIEW→REJECTED 守卫 :330）/ `convertToFormal:344`。
- `module-hr/erp-hr-service/.../processor/ErpHrSalarySimulationConvertToFormalProcessor.java:23-106` — `convertToFormal:23`（APPROVED 守卫 :25 + 逐人 PAID/重复冲突检测 + 写正式薪酬 + Simulation→CONVERTED :106）。
- `ErpHrSalarySimulationItemAdjustmentBizModel.java` — 调整追踪。

### UC-HR-11 员工调研
- `module-hr/erp-hr-service/.../entity/ErpHrSurveyBizModel.java` — **18 行 CRUD 桩**（`extends CrudBizModel<ErpHrSurvey>`，零 publish/close/archive mutation；P1-MA2-041 原登记桩，HEAD 复核维持桩）。
- `module-hr/erp-hr-service/.../entity/ErpHrSurveyResultBizModel.java` — **18 行 CRUD 桩**（零 `aggregateResult`/eNPS/驱动因子分析方法；本计划基线「aggregateResult 自动聚合」声称**与 HEAD 不符**）。
- `module-hr/erp-hr-service/.../entity/ErpHrSurveyResponseBizModel.java` + `ErpHrSurveyAnswerBizModel.java` + `ErpHrSurveyQuestionBizModel.java` — CRUD 桩（题库/答卷/回答 CRUD 可用，零 respondentHash 写入/校验逻辑）。
- `module-hr/model/app-erp-hr.orm.xml`：`surveyType:1344`（ext:dict `erp-hr/survey-type`）/ `eNpsScore:1357`（Survey 派生）/ `driverCategory:1402`（Question，ext:dict `erp-hr/driver-category`）/ `respondentHash:1429`（Response）/ `eNpsScore:1499`（Result）— **列存在但零业务 writer**。
- grep 全 `module-hr/erp-hr-service/src/main/`：`aggregateResult|calculateEnps|publishSurvey|closeSurvey|respondentHash` **零生产命中**。

---

## 3. 测试证据（L4，注明断言强度）

| 测试 | 覆盖 | 断言强度 |
|------|------|---------|
| `TestErpHrPayrollEngine.java` | UC-HR-04 公式（基本+加班+绩效+社保钳制+个税累计预扣）+ P1-MA4-016 末档边界（>960000）+ P1-MA4-018 累计损坏抛 `ERR_HR_CUMULATIVE_DATA_CORRUPT` | **强断言**（R2.13 补强后覆盖高档边界 + 累计解析失败负向断言；公司承担过账缺失有显式「Deferred successor」标注 :408-440） |
| `payroll/TestIncomeTaxCalculator.java` | UC-HR-04 个税边界（七级累进 + 末档 null 防御） | 强断言 |
| `TestErpHrPayrollSimulation.java` | UC-HR-10 What-If 克隆隔离 + convertToFormal + PAID 守卫 | 强断言 |
| `TestErpHrSalaryWorkflowApproval.java` | UC-HR-04 工资审批-支付双轴（approveStatus 4 态 + paymentStatus PENDING→PAID/VOID） | 强断言 |
| `posting/TestHrPostingFaultInjection.java` | UC-HR-04 过账故障注入（mock post 抛异常→断言悬挂可观测） | 强断言（R2.13 补强） |
| `TestErpHrSurveyCrudSmoke.java` | UC-HR-11 调研 CRUD | **仅冒烟**（验收标准 ㉖㉗㉘㉙ 零断言——respondentHash 防重复 / publish-close / aggregateResult / eNPS 无测试覆盖） |
| E2E `tests/e2e/business-actions/hr-payroll.action.spec.ts` | UC-HR-04 薪酬核算发薪全链 | 强断言 |
| E2E `tests/e2e/business-actions/hr-salary-simulation.action.spec.ts` | UC-HR-10 模拟 | 强断言 |
| E2E `projects-timesheet-posting.action.spec.ts` | UC-HR-03 跨域（projects 侧 posting，归 projects 切片） | —（归 projects） |
| UC-HR-03 工时表 / UC-HR-11 调研 | — | **无独立 E2E**（hr 侧无 timesheet approve / survey publish 路径可达） |

---

## 4. 运行时行为证据（L5）

按 §去重协议，L5 行为证据复用 A2.7b / A4.4 已证实结论：
- **工资支付轴 + 仿真 5 态 + convertToFormal 双层容错**：行为已证实（A2.7b）。
- **跨域 Facade**：`markPaid → SalaryPostingDispatcher.tryPostPayment → SalaryPostingExecutor → IErpFinVoucherBiz.post()` REQUIRES_NEW，hr production 代码零 `daoFor(ErpFin*)` 跨域写（A2.7b + A4.4 双重证实）。
- **过账悬挂告警闭环**：`dispatchFailureAlert:105-125` 派发 `IErpSysNotificationBiz`（`hr.salary-posting-failure`），P1-MA2-048 resolved R1.16 落地（A4.4 复核）。
- **个税高档 + 累计损坏**：HEAD 复核 `resolveBracket:231-234` null 防御 + `parseCumulativeData:187` 抛 ErrorCode 已落地（R1.26）。

**本切片差异（需求契约↔行为）**：UC-HR-03 approve/reject + 24h 校验 + totalHours 汇总、UC-HR-04 计提+公司承担过账（270/290/300）、UC-HR-11 publish/close + respondentHash + aggregate + eNPS 在运行时**不可达**（无 mutation/无 writer/无调用方）——交 §7 静态存疑点 + §5 结论。

---

## 5. 五级追踪矩阵 + 每 UC 符合性结论

### 矩阵（4 行，每 UC 一行）

| UC | L1 需求契约 | L2 owner doc（设计参考，冲突以 L1 为准） | L3 代码路径 | L4 测试断言 | L5 运行时行为 | 符合性结论 |
|----|----|----|----|----|----|----|
| **UC-HR-03** | `use-cases.md:27` 工时表提交（5 流程 + 24h 校验 + totalHours + APPROVED 归集 cost-collection） | `state-machine.md §三 工时表`（owner doc 标 APPROVED/REJECTED + 工时归集 Deferred——R1.15 方案B 标注；**L1 冲突以 L1 为准**，L2 推定已向实现妥协） | `ErpHrTimesheetBizModel.java:35-47`（仅 submit）+ `ErpHrTimesheetLineBizModel.java`（18 行桩） | 无独立测试（hr 侧 approve/reject/24h/totalHours 零断言） | submit 可达；approve/reject/24h/totalHours/cost-collection **不可达** | **P1**（②③ 新→P1-RC-015；④⑤ reuse P1-MA2-043 重开） |
| **UC-HR-04** | `use-cases.md:39` 薪酬核算（公式⑥-⑫ + 合同/考勤读取⑬ + PENDING⑭ + HR 审核发薪⑮ + 财务过账 SALARY 凭证⑯ + 缺失跳过告警⑰） | `payroll.md §五/§六/§九`（双轴状态机 + 计提 SALARY(270)+290+300 过账 + approve→APPROVED 联动；§6.5/§9.1 标 Deferred——R1.26 方案B 标注；**L1 冲突以 L1 为准**） | `ErpHrSalaryBizModel.java:80-99` + `PayrollCalculator.java:54-159`（公式完整⑥-⑫⑬⑭⑰）+ `IncomeTaxCalculator.java`（016/018 resolved）+ `SalaryPostingDispatcher.java`（仅 280 wired；270/290/300 死代码 :67/:127） | `TestErpHrPayrollEngine`（强，含 016/018 负向）+ `TestHrPostingFaultInjection`（强）+ E2E hr-payroll（强） | 公式 + PENDING + markPaid→280 过账可达；**270/290/300 过账不可达**（tryPostAccrual 零调用方 + ER 金额丢弃 :110/:115） | **P1 on ⑯**（reuse P1-MA4-017 重开 Q4 会计正确性类）；⑥-⑫⑬⑭⑮⑰ 接受 |
| **UC-HR-10** | `use-cases.md:113` 薪酬模拟（复制源快照⑱ + 应变计算⑲ + 5 态审批⑳ + convertToFormal 创建正式 ErpHrSalary㉑ + 目标期间 PAID 不允许转正式㉒） | `payroll-simulation.md §一/§二/§四`（What-If + 5 态 + convertToFormal + PAID 守卫） | `ErpHrSalarySimulationBizModel.java:83-346`（5 态全迁移 :287/:307/:326/:344 + What-If :107）+ `ConvertToFormalProcessor.java:23-106`（APPROVED 守卫 :25 + 逐人 PAID 冲突 + CONVERTED :106） | `TestErpHrPayrollSimulation`（强）+ E2E hr-salary-simulation（强） | 行为已证实（A2.7b：5 态全迁移 + What-If 克隆隔离 + convertToFormal 双层容错） | **接受** |
| **UC-HR-11** | `use-cases.md:125` 员工调研（类型㉓ + 题库㉔ + 驱动因子㉕ + 匿名 respondentHash 防重复㉖ + 发布/截止㉗ + 自动聚合㉘ + eNPS+仪表盘㉙） | `employee-survey.md §状态机/§匿名模式/§结果分析`（声明 DRAFT→OPEN→CLOSED→ARCHIVED + respondentHash + CLOSED 自动聚合 + eNPS；owner doc 标 Deferred——R1.15 方案B 标注；**L1 冲突以 L1 为准**） | `ErpHrSurveyBizModel.java`（18 行桩）+ `ErpHrSurveyResultBizModel.java`（18 行桩，零 aggregate）+ ORM 列存在 :1344/:1402/:1429/:1357/:1499 零 writer | `TestErpHrSurveyCrudSmoke`（**仅冒烟**——㉖㉗㉘㉙ 零断言） | CRUD 可达；publish/close/respondentHash/aggregate/eNPS **不可达** | **P1**（㉖㉘㉙ 新→P1-RC-016；㉗ reuse P1-MA2-041 重开） |

### 逐条验收标准分级（①-㉙，§3 完整枚举）

| # | 验收标准 | UC | HEAD 状态 | 分级 |
|---|---------|-----|---------|------|
| ① | TimesheetLine 分项目/任务/活动类型记录小时数 | 03 | ORM 字段 + CRUD 桩可用（被动） | 接受 |
| ② | 同一日工时超过 24h 校验 | 03 | **未实现**（grep `24\|MAX_HOURS` 零业务命中） | **P1 → P1-RC-015** |
| ③ | totalHours 汇总 | 03 | **未实现**（无 setTotalHours writer） | **P1 → P1-RC-015** |
| ④ | APPROVED 工时归集 cost-collection（跨域 projects 订阅） | 03 | **未实现**（hr 侧无 approve mutation → 无 APPROVED 触发点） | **P1 → reuse P1-MA2-043** |
| ⑤ | 项目经理审批 APPROVED/驳回 REJECTED | 03 | **未实现**（仅 submit） | **P1 → reuse P1-MA2-043** |
| ⑥ | 基本工资（合同月薪+出勤比例） | 04 | `PayrollCalculator:86` ✅ | 接受 |
| ⑦ | 加班费（overtimeMinutes×费率） | 04 | `PayrollCalculator:96`（注：用 workHours>8 派生 overtimeHours × 费率，语义对齐） ✅ | 接受 |
| ⑧ | 绩效奖金 | 04 | 手工录入 0（:100，payroll.md §设计边界声明本期手工） ✅ | 接受 |
| ⑨ | 社保扣除 | 04 | `PayrollCalculator:108-109`（钳制） ✅ | 接受 |
| ⑩ | 公积金扣除 | 04 | `PayrollCalculator:113-114` ✅ | 接受 |
| ⑪ | 个税（累计预扣） | 04 | `IncomeTaxCalculator:69`（016/018 resolved） ✅ | 接受 |
| ⑫ | 实发=基本+加班+绩效-社保-公积金-个税 | 04 | `PayrollCalculator:127` ✅ | 接受 |
| ⑬ | 合同/考勤数据读取 | 04 | `PayrollCalculator:55-73/282-310`（缺失合同抛 ErrorCode :57） ✅ | 接受 |
| ⑭ | paymentStatus=PENDING | 04 | `PayrollCalculator:149` ✅ | 接受 |
| ⑮ | HR 审核发薪 | 04 | `markPaid:97`（approveStatus=APPROVED 守卫） ✅ | 接受 |
| ⑯ | 跨域财务过账 SALARY 凭证 | 04 | **部分**——仅 SALARY_PAYMENT(280) wired；**SALARY(270) + SOCIAL_INSURANCE_ER(290) + HOUSING_FUND_ER(300) 未接线**（tryPostAccrual 死代码 + ER 丢弃 + 无 290/300 event） | **P1 → reuse P1-MA4-017（Q4 会计正确性类重开）** |
| ⑰ | 缺失配置跳过告警 | 04 | `findActiveContract:273` 抛 ErrorCode（响亮失败，语义对齐「跳过并告警」） ✅ | 接受 |
| ⑱ | 复制源快照（薪酬项目/累计个税/考勤） | 10 | `createSimulation` + `recalculateWithOverrides:188` cloneInstance ✅ | 接受 |
| ⑲ | 调整应变计算 | 10 | `recalculateWithOverrides:180` + `applyOverride:204` + `recalculateDerived:243` ✅ | 接受 |
| ⑳ | 5 态审批（DRAFT/IN_REVIEW/APPROVED/REJECTED/CONVERTED） | 10 | `:287/:307/:326/:344` + Processor :106 全迁移守卫 ✅ | 接受 |
| ㉑ | convertToFormal 创建正式 ErpHrSalary | 10 | `ConvertToFormalProcessor:23-106` ✅ | 接受 |
| ㉒ | 目标期间 PAID 不允许转正式 | 10 | `ConvertToFormalProcessor` 逐人 PAID 冲突检测（A2.7b 证实双层容错） ✅ | 接受 |
| ㉓ | 调研类型（ANNUAL_ENGAGEMENT/PULSE/eNPS/ADHOC） | 11 | ORM :1344 ext:dict（被动可用） | 接受（被动） |
| ㉔ | 题库（评分/选择/开放） | 11 | ErpHrSurveyQuestion + CRUD（被动可用） | 接受（被动） |
| ㉕ | 驱动因子分类（GROWTH/RECOGNITION/MANAGEMENT/WELLBEING/ALIGNMENT） | 11 | ORM :1402 ext:dict（被动可用） | 接受（被动） |
| ㉖ | 匿名模式 employeeId 不存储仅存 respondentHash 防重复 | 11 | **未实现**（列存在 :1429 零 writer/零校验） | **P1 → P1-RC-016** |
| ㉗ | 发布（OPEN）/截止（CLOSED） | 11 | **未实现**（ErpHrSurveyBizModel 18 行桩） | **P1 → reuse P1-MA2-041** |
| ㉘ | 自动聚合 ErpHrSurveyResult | 11 | **未实现**（ErpHrSurveyResultBizModel 18 行桩，零 aggregate） | **P1 → P1-RC-016** |
| ㉙ | eNPS 得分 + 仪表盘（趋势/部门对比/驱动因子分析） | 11 | **未实现**（零 eNPS 计算；ORM :1357/:1499 零 writer） | **P1 → P1-RC-016** |

### resolved finding HEAD 复核（关键证据，§逻辑非行号）

| finding | arm-index 声称 | HEAD 复核结论 |
|---------|--------------|--------------|
| **P1-MA4-017**（计提+公司承担过账） | ✅ resolved (R1.26 done) | **未落地（方案B Deferred）**。R1.26 计划 `2026-07-30-0720-3` §Phase 1 显式裁决 017 为方案B（owner doc Deferred 标注），**未实现 approve→APPROVED 联动**：(1) `tryPostAccrual:67` 仍为零调用方死代码（javadoc :35-38/:63-65 自述「Successor=R1.26」，但 R1.26 本身未接线）；(2) `socialInsuranceER/housingFundER` 无 ORM 列（grep `socialInsuranceER\|housing_fund_er` 零命中）+ `PayrollCalculator:110/:115/:156-157` 计算后丢弃；(3) 290/300 event 永不生成（无 tryPostEr 方法）。`TestErpHrPayrollEngine:408-440` 测试注释亦自述「Deferred successor」。**Q4 裁决=(a) 会计正确性类无例外**：audit-remediation 侧方案B 关闭在 requirement-compliance 下不成立，须经 MR1（RC-R1.n）实现。 |
| P1-MA4-016（个税高档 NPE） | ✅ resolved (R1.26 done) | **已落地**。`IncomeTaxCalculator.resolveBracket:231-234` 末档 `getRangeUpperLimit()==null` 防御存在。 |
| P1-MA4-018（parseCumulativeData 吞） | ✅ resolved (R1.26 done) | **已落地**。`parseCumulativeData:184-190` LOG.warn + 抛 `ERR_HR_CUMULATIVE_DATA_CORRUPT`。 |
| P1-MA4-019（测试有效性） | ✅ resolved (R2.13 done) | **已落地**。`TestErpHrPayrollEngine` + `TestHrPostingFaultInjection` 含高档边界/过账悬挂/累计损坏负向断言（公司承担过账缺失有显式 Deferred 标注 :408-440）。 |
| P1-MA2-041（调研 CRUD 桩） | ✅ resolved (R1.15 done) | **方案B Deferred**（owner doc 标注）。HEAD BizModel 仍 18 行桩——方案B 关闭在 Q4=(a) 下不成立（见 UC-HR-11 ㉗㉖㉘㉙）。 |
| P1-MA2-043（工时单仅 submit） | ✅ resolved (R1.15 done) | **方案B Deferred**（owner doc 标注）。HEAD BizModel 仍仅 submit——方案B 关闭在 Q4=(a) 下不成立（见 UC-HR-03 ④⑤）。 |
| P1-MA2-044（硬编码字符串） | ✅ resolved (R1.15 done) | **已落地（方案A）**。`ErpHrTimesheetBizModel:39/:44` 已用 `ErpHrConstants.TIMESHEET_STATUS_*` 常量。 |
| P1-MA2-045（银行文件死状态） | ✅ resolved (R1.15 done) | 方案B Deferred（与 UC-HR-04 发薪主路径无直接关联，归 hr 银行文件 successor，本切片不重开）。 |
| P1-MA2-047（javadoc drift + posted 死字段） | ✅ resolved (R1.15 done) | **已落地（方案B javadoc 对齐）**。`SalaryPostingDispatcher:32-42/:56-66` javadoc 已修正（标注 posted 字段 Deferred + Successor=R1.26）；posted writer 仍未写（与 P1-MA4-017 协同，随 017 MR1 实现时激活）。 |
| P1-MA2-048（过账吞异常悬挂） | ✅ resolved (R1.16 done) | **已落地**。`dispatchFailureAlert:105-125` 派发 `IErpSysNotificationBiz` 告警（hr.salary-posting-failure）。 |
| P1-MA2-068（projects TimesheetPostingDispatcher，归 projects） | ✅ resolved (R1.16 done) | 归 projects 切片，本切片仅核验 hr 侧——hr 侧无 APPROVED 触发点（见 ④），不重审 projects dispatcher。 |

---

## 6. 与 arm-index 衔接（复用 or 新增 裁决，§7）

按 §7 规则，每条 finding 产出前 grep `arm-index.md` 同域同控制点后裁决：

### 复用（同根因同控制点，追加 RC 交叉引用注记，不新建）

| 既有 finding | 本切片对应 | 裁决依据 |
|-------------|----------|---------|
| `P1-MA4-017` | UC-HR-04 ⑯ 业财过账链路（270/290/300 未接线） | **同根因同控制点**（计提+公司承担过账链路未实现）。audit-remediation 侧 R1.26 方案B Deferred 关闭；requirement-compliance Q4=(a) 下重开，经 MR1（RC-R1.n）实现。arm-index :579 行追加 RC 交叉引用。 |
| `P1-MA2-043` | UC-HR-03 ④⑤ approve/reject + cost-collection | **同根因同控制点**（工时单 APPROVED/REJECTED 死状态 + 仅 submit）。R1.15 方案B Deferred；Q4=(a) 下重开，经 MR1 实现。arm-index :243 行追加 RC 交叉引用。 |
| `P1-MA2-041` | UC-HR-11 ㉗ publish/close | **同根因同控制点**（调查 OPEN/CLOSED/ARCHIVED 死状态 + CRUD 桩）。R1.15 方案B Deferred；Q4=(a) 下重开，经 MR1 实现。arm-index :241 行追加 RC 交叉引用。 |

### 新增（新根因/新功能点/新维度）

| 新 finding | UC | 与既有 finding 差异依据 |
|-----------|-----|----------------------|
| `P1-RC-015` | UC-HR-03 ②③ | **新控制点**：UC-HR-03 验收标准「同一日工时超过 24h 校验」（数据完整性校验维度）+「totalHours 汇总」（派生字段聚合维度）。P1-MA2-043 覆盖的是「状态机死状态 + CRUD 桩」（状态机维度），**未覆盖数据校验 + 派生聚合**。L1 `use-cases.md:36` 异常段 + 后置条件显式要求。 |
| `P1-RC-016` | UC-HR-11 ㉖㉘㉙ | **新控制点**：UC-HR-11 验收标准「匿名 respondentHash 防重复」（匿名安全/唯一性维度）+「CLOSED 自动聚合 ErpHrSurveyResult」（结果聚合计算维度）+「eNPS 得分 + 仪表盘」（评分/分析维度）。P1-MA2-041 覆盖的是「状态机死状态 + CRUD 桩」（状态机维度），**未覆盖匿名防重复 + 聚合算法 + eNPS 评分**。L1 `use-cases.md:132/134` 显式要求。 |

### MR1 修复行预留（R1.0 展开器读取本报告后向 MR1 追加 RC-R1.n 实体行）

- 270/290/300 计提+公司承担过账链路接线（含 ORM ask-first 裁决 socialInsuranceER/housingFundER 持久化设计 + tryPostAccrual/290/300 接线 + approve action + posted writer）— **触及会计保护区域 + ORM 结构变更，修复行须 ask-first + 独立 plan-audit**（§5）。
- 工时单 approve/reject + 24h 校验 + totalHours 汇总 + cost-collection 归集触发（hr 侧 APPROVED 事件）。
- 调研 publish/close + 匿名 respondentHash 写入/校验 + CLOSED 自动聚合 + eNPS 计算 + 仪表盘。

---

## 7. 静态存疑点清单（供 MA4 / A4.2 展开）

> 以下为本切片 L5 无法静态定论、需运行时确认的点（MA4 / A4.2 展开器读取）：

1. **UC-HR-04 ⑯ 计提+公司承担过账运行时触发链**：approve→APPROVED 时计提 SALARY(270) + 290/300 event 是否生成（HEAD 静态判定 = 永不生成，tryPostAccrual 零调用方；运行时可经 approve E2E 确认 GL 仅收 280）。
2. **UC-HR-04 公司承担金额运行时丢弃确认**：`PayrollCalculator:110/:115` socialInsuranceER/housingFundER 计算后是否经 remark/billData 传递到 PostingEvent（HEAD 静态判定 = 丢弃，无 setRemark；运行时可断言 billData 不含 ER 金额）。
3. **UC-HR-03 ②24h 校验运行时拦截**：同一日多条 TimesheetLine hours 之和 > 24 是否被拦截（HEAD 静态判定 = 无校验；运行时可构造 >24h 提交确认无报错）。
4. **UC-HR-11 ㉖匿名 respondentHash 运行时防重复**：匿名模式重复提交是否被拦截（HEAD 静态判定 = 无 writer/无校验；运行时可构造同 respondentHash 重复提交确认无拦截）。
5. **UC-HR-11 ㉘㉙ CLOSED 自动聚合 + eNPS 运行时计算**：CLOSED 时 ErpHrSurveyResult 是否自动聚合 + eNPS 是否计算（HEAD 静态判定 = 无 mutation/无算法；运行时确认结果表永远空）。

---

## 8. 过程纪律自检

- [x] **checker 退出码门控核查**：本报告产出后运行 `bash docs/audits/nop-compliance-checker.sh`，actual vs baseline（machine-readable 块 `compliance-baseline.md:296-316`）：

  | 规则 | actual | baseline | 状态 |
  |------|--------|----------|------|
  | R1a/R1b/R1c | 0/0/0 | 0/0/0 | = |
  | R1d | 14 | 14 | = |
  | R2a | 34 | 34 | = |
  | R2b | 229 | 229 | = |
  | R2c | 1382 | 1382 | = |
  | R2d | 34 | 34 | = |
  | R3 | 5 | 5 | = |
  | R4/R5/R7/R8/R11 | 0 | 0 | = |
  | R6 | 2 | 2 | = |
  | R10 | 6 | 6 | = |
  | R12a/R12b/R12c | 69/66/40 | 69/66/40 | = |

  全 16 可计数规则 actual ≤ baseline（全 =），exit 0。**区分门控退出码 vs 纯 reporter 退出码**：checker 脚本是纯 reporter（退出码恒 0），真正门控在 CI workflow（`.github/workflows/compliance.yml`）解析 actual > baseline => sys.exit(1)。本报告**不以** checker 脚本退出码 0 作为门控通过依据。**本审计为只读审计，无生产代码变更，checker 无回归风险**（actual = baseline 全等进一步印证零变更）。
- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告全部 finding 已按 §7 规则 grep arm-index 同域同控制点后给出"复用 or 新增"裁决（P1-MA4-017/P1-MA2-043/P1-MA2-041 复用；P1-RC-015/P1-RC-016 新增并附差异依据），无未经比对直接新建的 finding。

---

## Verdict

**FAIL（有需求-实现符合性分歧）**：4 UC 中 **UC-HR-10 接受**（5 态 + What-If + convertToFormal + PAID 守卫齐全）；**UC-HR-03 / UC-HR-04 / UC-HR-11 各有 P1 需求分歧**：

- **UC-HR-04 ⑱ 业财过账链路（P1-MA4-017，Q4 会计正确性类，最高优先）**：SALARY_PAYMENT(280) 已 wired，但 SALARY 计提(270) + 公司承担社保(290)/公积金(300) 过账链路**未接线**（tryPostAccrual 死代码 + ER 金额丢弃 + 无 290/300 event + 无 ORM ER 列）。GL 永远仅收发放凭证 → 费用+应付职工薪酬低估+资产负债表失衡。audit-remediation R1.26 方案B Deferred 关闭在 requirement-compliance Q4=(a)（"P0/P1 必须实现，禁方案B，会计正确性类无例外"）下**不成立**，须经 MR1 实现且触及会计保护区域 + ORM ask-first。
- **UC-HR-03 ②③④⑤**：24h 校验 + totalHours 汇总缺失（P1-RC-015 新增）+ approve/reject + cost-collection 归集缺失（reuse P1-MA2-043 重开）。
- **UC-HR-11 ㉖㉗㉘㉙**：匿名 respondentHash 防重复 + 自动聚合 + eNPS 缺失（P1-RC-016 新增）+ publish/close 缺失（reuse P1-MA2-041 重开）。

**零 P0**：UC-HR-04 公式完整（员工实发工资正确，公司承担不影响个人 net）+ 漏记凭证可经期末试算平衡人工发现 + UC-HR-03/11 缺失不破坏活跃数据（CRUD 可用，无悬挂半状态）。

**resolved finding HEAD 复核**：P1-MA4-016/018/019 + P1-MA2-044/047/048 已落地（方案A 或 javadoc 对齐）；**P1-MA4-017 + P1-MA2-041/043 方案B Deferred 关闭在 Q4=(a) 下重开**（经 MR1 RC-R1.n 实现）。

**本审计不实施修复**（只读审计，结果表面 = 本报告 + arm-index 登记）。finding 修复按 §10 经 MR1（R1.0 展开为 RC-R1.n），触及会计过账逻辑 + ORM 结构的修复行须 ask-first + 独立 plan-audit（§5 保护区域暂停协议）。

---

## 参考

- 真相源：`docs/design/human-resource/use-cases.md:27/:39/:113/:125`（UC-HR-03/04/10/11）
- 设计参考：`docs/design/human-resource/payroll.md` + `payroll-simulation.md` + `employee-survey.md` + `state-machine.md`
- 方法论：`docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议）
- L5 既有证据：`docs/audits/2026-07-28-0230-arm-ma2-hr-attendance-payroll-state-machine.md`（A2.7b）+ `docs/audits/2026-07-29-0430-arm-ma4-hr-code-quality.md`（A4.4）
- resolved 裁决计划：`docs/plans/2026-07-30-0341-1-r1-15-hr-state-machine-dict-dead-state.md`（R1.15）+ `docs/plans/2026-07-30-0341-2-r1-16-posting-error-propagation-grading-strategy.md`（R1.16）+ `docs/plans/2026-07-30-0720-3-r1-26-hr-payroll-tax-npe-silent-swallow.md`（R1.26）+ `docs/plans/2026-07-31-0420-3-r2-13-hr-payroll-test-effectiveness.md`（R2.13）
