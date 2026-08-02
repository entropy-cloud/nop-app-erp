# 2026-08-02-2250-3 rc-ma1-a1-14-hr-f3-payroll-survey hr-F3 薪酬与调研需求符合性审计

> Plan Status: completed
> Last Reviewed: 2026-08-02
> Mission: requirement-compliance
> Work Item: A1.14（MA1 需求追踪矩阵审计 — hr-F3 薪酬与调研：UC-HR-03 工时表提交 + UC-HR-04 薪酬核算 + UC-HR-10 薪酬模拟 + UC-HR-11 员工调研）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A1.14
> Related: `docs/plans/2026-08-02-1458-1-requirement-compliance-methodology.md`（M0.1 done）、`2026-08-02-1530-1-requirement-baseline-extraction.md`（M0.2 done，解除 A1.14 的 0.2 依赖）、`2026-08-02-2250-1-rc-ma1-a1-12-hr-f1-employee-organization.md`（A1.12，同 hr 域同范式，员工主数据/合同为薪酬前置）、`2026-08-02-2250-2-rc-ma1-a1-13-hr-f2-shift-attendance.md`（A1.13，同 hr 域，考勤数据为薪酬输入）、`2026-08-02-2231-2-rc-ma1-a1-11-mfg-f4-variance-batch-kanban.md`（A1.11 done，同范式参考）
> Audit: required

## Current Baseline

> 本计划是**审计工作项**（verification or audit work），结果表面 = 一份审计报告。基线盘点被审功能现状代码/测试/既有证据，**不修改任何代码**。

- **方法论契约 + UC 锚点已就绪**：`docs/audits/requirement-compliance-methodology.md`（§1 五级矩阵 / §2 分级判据 / §3 完整枚举 / §4 Q1 真相源层级 / §5 Q4 修复义务 + 保护区域暂停协议 / §6 报告 9 段骨架 / §7 arm-index 衔接 / §8 过程纪律自检 / §9 真相源冻结 / §10 MR0/MR1 机制 / §去重协议）已落盘；`docs/audits/rc-requirement-baseline-inventory.md` 已为 A1.14 给出 UC 清单 = `UC-HR-03/04/10/11`（4 UC），锚点 `use-cases.md:27 / :39 / :113 / :125`（baseline inventory :70/:71/:77/:78 + 切片索引 :348 确认一致 ✅）。

- **L1 需求契约（权威真相源）**：`docs/design/human-resource/use-cases.md`：
  - UC-HR-03 工时表提交（`:27`）：员工创建 Timesheet 选周期起止 → 添加 TimesheetLine 每天分项目/任务/活动类型记录小时数 → 提交 status=SUBMITTED → 项目经理审批 APPROVED（工时归集到 projects 域 cost-collection）或驳回 REJECTED → totalHours 汇总；异常：同一日工时超过 24h 校验、项目工时段落不可重叠(可选)；跨域：projects/cost-collection 订阅 Timesheet APPROVED 事件。
  - UC-HR-04 薪酬核算（`:39`）：SalaryJob 读取当月所有 ACTIVE 员工的考勤/休假/合同数据 → 计算基本工资(合同月薪+当月出勤比例) + 加班费(考勤 overtimeMinutes×加班费率) + 绩效奖金(绩效模块/手动录入) - 社保 - 公积金 - 个税 = 实发 → 生成 ErpHrSalary(paymentStatus=PENDING) → HR 审核确认发薪；异常：员工缺失合同或薪资配置跳过并告警、社保计算因城市差异需配置；跨域：销售/采购(绩效奖金)、考勤(缺勤/加班)、财务过账(SALARY 凭证)。
  - UC-HR-10 薪酬模拟（`:113`）：HR 选源薪酬期间创建模拟(ErpHrSalarySimulation.status=DRAFT) → 系统复制每位员工薪酬项目行/累计个税/考勤快照 → HR 调整薪酬项目即时应变计算 → 提交审核(IN_REVIEW)审批(APPROVED/REJECTED) → APPROVED 转正式(CONVERTED)创建正式 ErpHrSalary 进入支付流程；异常：目标期间已有 PAID 正式薪酬不允许转正式；跨域：ErpHrSalary(CONVERTED 创建正式)、ErpHrSalaryItem(复用薪酬项目定义)。
  - UC-HR-11 员工调研（`:125`）：HR 创建 ErpHrSurvey(类型 ANNUAL_ENGAGEMENT/PULSE/eNPS/ADHOC) → 添加 ErpHrSurveyQuestion(评分/选择/开放题) → 可选关联驱动因子分类(GROWTH/RECOGNITION/MANAGEMENT/WELLBEING/ALIGNMENT) → 设置匿名模式/目标部门/员工/起止日期 → 发布(OPEN)通知目标员工 → 员工填写提交(ErpHrSurveyResponse+ErpHrSurveyAnswer) → 匿名模式 employeeId 不存储仅存 respondentHash 防重复 → 截止 CLOSED 自动聚合 ErpHrSurveyResult → HR 看结果仪表盘(评分趋势/部门对比/eNPS/驱动因子分析)；异常：同一员工重复提交匿名问卷 respondentHash 拦截、问卷发布后不可再编辑题目(可新建版本)。

- **L2 owner doc 设计参考**：`docs/design/human-resource/payroll.md`（薪酬核算 + §五/§六/§七 工资审批-支付双轴状态机 + 计提 SALARY(270)+社保公司 SOCIAL_INSURANCE_ER(290)+公积金公司 HOUSING_FUND_ER(300) 过账 + §6/§9.1 approve→APPROVED 联动过账）+ `docs/design/human-resource/payroll-simulation.md`（薪酬模拟 What-If + 仿真 5 态 + convertToFormal）+ `docs/design/human-resource/employee-survey.md`（员工调研 + 匿名模式 + 驱动因子 + 聚合）+ `docs/design/human-resource/state-machine.md`（适用对象三工时表 + 四薪酬审批 + 仿真/银行文件章节[若已补]；P2-MA2-052 watch-only 已登记缺章节）+ `docs/design/human-resource/README.md`。**注意**：L2 为设计参考，与 L1 冲突时按 §4 Q1 以 L1 为准。

- **L3 代码实现现状（执行时实测核验）**：
  - **工时表提交（UC-HR-03）**：`module-hr/erp-hr-service/.../entity/ErpHrTimesheetBizModel.java`（Timesheet 4 态 + submit；执行时核验：仅 submit 无 approve/reject 动作集[P1-MA3-047 审批动作集不对称已登记] + totalHours 汇总 + 24h 校验 + 跨域 cost-collection 归集）+ `ErpHrTimesheetLineBizModel.java`；硬编码 "DRAFT"/"SUBMITTED" 字符串（P1-MA2-044 resolved 状态 HEAD 复核）。跨域：`TimesheetPostingDispatcher.tryPost`（projects 域 P1-MA2-068 同型吞异常悬挂，归 projects 切片，本切片核验 hr 侧 SUBMITTED/APPROVED 触发）。
  - **薪酬核算（UC-HR-04）**：`ErpHrSalaryBizModel.java`（calculateSalary/runPayroll/markPaid）+ `processor/ErpHrSalaryCalculateSalaryProcessor.java` + `processor/ErpHrSalaryRunPayrollProcessor.java`（R6.1 per-mutation 拆分）+ `PayrollCalculator`（基本工资+津贴+绩效+加班费-社保-公积金-个税=实发 + socialInsuranceER/housingFundER 公司承担）+ `IncomeTaxCalculator`（累计预扣法七级累进 + resolveBracket 末档 null NPE **P1-MA4-016 resolved R1.26** + parseCumulativeData 静默吞 **P1-MA4-018 resolved R1.26**）+ `SocialInsuranceCalculator`（基数钳制 min/max + 公积金回退基数）+ `SalaryPostingDispatcher`/`SalaryPostingExecutor`（IErpFinVoucherBiz REQUIRES_NEW Facade）/`SalaryPostingProvider`（SALARY/SALARY_PAYMENT/SOCIAL_INSURANCE_ER/HOUSING_FUND_ER 四类；**计提+公司承担 PostingEvent 链路 P1-MA4-017 resolved R1.26**，HEAD 复核 tryPostAccrual 接线 + ER 金额持久化 + 290/300 event 生成）+ `ErpHrTaxConfigBizModel`/`ErpHrSocialInsuranceConfigBizModel`/`ErpHrSocialInsuranceBaseBizModel`/`ErpHrTaxSpecialDeductionBizModel`（税率/社保配置）。
  - **薪酬模拟（UC-HR-10）**：`ErpHrSalarySimulationBizModel.java`（What-If 克隆源快照不污染 + readSalaryField/applyOverride + 5 态 DRAFT/IN_REVIEW/APPROVED/REJECTED/CONVERTED）+ `processor/ErpHrSalarySimulationConvertToFormalProcessor.java`（per-employee 冲突 skip + all-conflict throw 双层容错 + 目标期间已有 PAID 不允许转正式）+ `ErpHrSalarySimulationItemAdjustmentBizModel.java`；`TestErpHrPayrollSimulation`。
  - **员工调研（UC-HR-11）**：`ErpHrSurveyBizModel.java`（**18 行 CRUD 桩 P1-MA2-041 resolved 状态 HEAD 复核** + 调研类型 + 发布/截止）+ `ErpHrSurveyQuestionBizModel.java`（题库）+ `ErpHrSurveyResponseBizModel.java`/`ErpHrSurveyAnswerBizModel.java`（填写 + 匿名 respondentHash 防重复）+ `ErpHrSurveyResultBizModel.java`（aggregateResult 自动聚合 + eNPS + 驱动因子分析）。

- **L4 测试证据现状**：`TestErpHrPayrollEngine`（薪酬核算 + 个税累计预扣 + 社保钳制）+ `TestIncomeTaxCalculator`（个税边界）+ `TestErpHrPayrollSimulation`（模拟 What-If + convertToFormal）+ `TestErpHrSalaryWorkflowApproval`（工资审批-支付双轴）+ `TestHrPostingFaultInjection`（过账故障注入）+ `TestErpHrSurveyCrudSmoke`（调研冒烟，**仅冒烟 P1-RC 候选**）。E2E：`tests/e2e/business-actions/hr-payroll.action.spec.ts`（薪酬核算发薪全链）+ `hr-salary-simulation.action.spec.ts`（模拟）。**执行时核验断言强度**（个税高档边界[P1-MA4-019 resolved R2.13 补强] / 过账悬挂 / 累计解析失败 / 公司承担过账 / 计提触发 覆盖情况）。

- **L5 既有证据（MA2/A4.4 复用输入，方法论 §去重协议）**：
  - **`docs/audits/2026-07-28-0230-arm-ma2-hr-attendance-payroll-state-machine.md`（A2.7b）= hr 考勤与工资八组件状态机审查**：Verdict 主路径状态迁移守卫齐全（工资支付轴 PENDING→PAID/VOID 双守卫 / 仿真 5 态全迁移 / convertToFormal per-employee 冲突 skip + all-conflict throw）+ 工资 markPaid 触发跨域过账经 IErpFinVoucherBiz REQUIRES_NEW Facade（hr production 代码零 daoFor(ErpFin*) 跨域写）+ 仿真 convertToFormal 双层容错。**零 P0**；本切片相关 **P1**：P1-MA2-043（工时单 APPROVED/REJECTED dict 死状态 + ErpHrTimesheetBizModel 仅 submit）+ P1-MA2-044（工时单硬编码字符串，resolved 状态 HEAD 复核）+ P1-MA2-045（银行付款文件死状态 + CRUD 桩）+ P1-MA2-047（SalaryPostingDispatcher javadoc drift + posted 死字段）+ P1-MA2-048（工资过账 tryPostPayment/tryPostAccrual 吞异常悬挂）；**P2**：P2-MA2-052（state-machine.md 缺章节）。
  - **`docs/audits/2026-07-29-0430-arm-ma4-hr-code-quality.md`（A4.4）= hr 薪酬/过账/模拟引擎链路代码质量审计**：Verdict FAIL（零 P0）；本切片相关 **4 项 P1**（**P1-MA4-016 个税高档 NPE resolved R1.26** / **P1-MA4-017 计提+公司承担过账链路未接线 resolved R1.26** / **P1-MA4-018 parseCumulativeData 静默吞 resolved R1.26** / **P1-MA4-019 测试有效性 resolved R2.13**）+ **2 项 P2**（P2-MA4-008 可维护性热点 / P2-MA4-009 自动化防护缺口）。**七面扎实**（社保基数钳制 / 累计预扣编排 / BigDecimal 全域货币类型安全 / 模拟 What-If 克隆隔离 / 跨域 Facade 零 daoFor / 异常规范化）。
  - **注意**：A2.7b/A4.4 覆盖**状态机迁移守卫/事务边界/代码质量/个税算术/过账链路**，且 A4.4 的 4 项 P1 已 resolved（R1.26/R2.13）。本切片从**需求契约↔实现符合性**视角补差异（UC-HR-03 工时表 24h 校验+cost-collection 归集 + UC-HR-04 薪酬核算公式完整性[基本+加班+绩效-社保-公积金-个税]+合同/考勤数据读取 + UC-HR-10 模拟 What-If+convertToFormal + UC-HR-11 调研匿名 respondentHash+聚合+驱动因子 + **resolved finding HEAD 复核 R1.26/R2.13 落地确认**）。

- **arm-index 既有 finding 衔接**：薪酬与调研相关——`P1-MA4-016`（个税高档 NPE，**resolved R1.26**，HEAD 复核）/ `P1-MA4-017`（计提+公司承担过账，**resolved R1.26**，HEAD 复核 tryPostAccrual 接线 + ER 持久化 + 290/300 event）/ `P1-MA4-018`（parseCumulativeData 吞，**resolved R1.26**，HEAD 复核）/ `P1-MA4-019`（测试有效性，**resolved R2.13**，HEAD 复核补强测试）/ `P1-MA2-043/044/045/047/048`（工时单/银行文件/工资过账状态机与吞异常）/ `P2-MA4-008/009`（可维护性/防护 watch-only）/ `P2-MA2-052`（state-machine.md 缺章节）/ `P2-MA1-020`（orphan dict salary-approval-status）。UC-HR-03 24h 校验+cost-collection 归集、UC-HR-04 薪酬核算公式完整性、UC-HR-11 调研匿名+聚合+驱动因子为候选新维度（既有审计未从需求契约视角裁决），执行时 grep `arm-index.md` hr 薪酬/工时/模拟/调研同域同控制点后裁决复用 or 新建 `P*-RC-xxx`。

- **保护区域**：本审计为**只读审计**（读代码/测试/报告，不改代码/ORM/api.xml/真相源）。属 roadmap 预授权类目。发现的 P0/P1 finding **不在本计划实施修复**——按方法论 §10，P0 经 MR0 即时通道、P1 经 MR1（R1.0 展开 RC-R1.n）；**触及会计过账逻辑（SalaryPostingDispatcher/PostingProvider/计提+公司承担凭证）的修复行须 ask-first + 独立 plan-audit**（§5 保护区域暂停协议，会计过账核心路径类）。本切片 P1-MA4-016/017/018/019 已 resolved（R1.26/R2.13），HEAD 复核为关键证据。

- **剩余差距**：A1.14 切片的五级追踪审计报告缺失 = MA4（A4.2 扩展域展开器，Deps=MA1 done）及 MR1（R1.0，Deps=MA1-MA4 done）的该切片证据缺口来源。本计划产出 A1.14 报告并登记 finding，解除其在 MA4/MR1 链路的该切片证据缺口。

## Goals

- 产出 A1.14 切片审计报告 `docs/audits/<执行时间戳>-rc-ma1-a1-14-hr-f3-payroll-survey.md`，含方法论 §6 **9 段全部内容**：①UC-HR-03/04/10/11 需求契约原文（逐字引用，不转述）②实现证据（`file:line`，含工时表 + 薪酬核算引擎 + 个税/社保计算 + 过账 Dispatcher/Executor/Provider + 模拟 What-If + 调研匿名/聚合）③测试证据（注明断言强度）④运行时行为证据（复用 A2.7b/A4.4，补差异）⑤五级追踪矩阵 + 每 UC 符合性结论（P0/P1/P2/接受）⑥与 arm-index 衔接（复用 or 新增 裁决）⑦静态存疑点清单（供 MA4 展开）⑧过程纪律自检段 ⑨与 MA2/MA4 报告差异增量声明。
- 对 4 UC 逐条核验**每条验收标准**（完整枚举，§3）：UC-HR-03（TimesheetLine 分项目/任务/活动 + 24h 校验 + totalHours 汇总 + APPROVED 归集 cost-collection + 跨域 projects 订阅）+ UC-HR-04（基本工资+加班费+绩效-社保-公积金-个税=实发公式完整性 + 合同/考勤数据读取 + paymentStatus=PENDING + HR 审核发薪 + 跨域财务过账 SALARY 凭证 + 缺失配置跳过告警）+ UC-HR-10（复制源快照 + 调整应变计算 + 5 态审批 + convertToFormal 创建正式 ErpHrSalary + 目标期间 PAID 不允许转正式）+ UC-HR-11（调研类型 + 题库 + 驱动因子分类 + 匿名 respondentHash 防重复 + 发布/截止 + 自动聚合 ErpHrSurveyResult + eNPS + 仪表盘），各一矩阵行。
- 对候选缺口/偏离给出分级结论：**UC-HR-04 薪酬核算业财过账链 P1-MA4-017（计提+公司承担 PostingEvent，resolved R1.26，HEAD 复核确认 tryPostAccrual 接线 + ER 持久化 + 290/300 event 生成则 dedup 闭环，仍 open 则 Q4 会计正确性类维持 P1 触发 MR1）** + 个税高档 NPE P1-MA4-016（resolved R1.26 HEAD 复核）+ parseCumulativeData P1-MA4-018（resolved R1.26 HEAD 复核）+ 测试有效性 P1-MA4-019（resolved R2.13 HEAD 复核）；**UC-HR-03 工时表 P1-MA2-043 APPROVED/REJECTED 死状态 + 仅 submit 无 approve/reject**（HEAD 复核 resolved 状态）；**UC-HR-11 调研 ErpHrSurveyBizModel 18 行 CRUD 桩 P1-MA2-041**（HEAD 复核）；UC-HR-03 24h 校验 / UC-HR-04 公式完整性 / UC-HR-11 聚合+驱动因子（执行时 HEAD 核验）——按 §2 判据定级，若为 P0/P1 则新建 `P0-RC-xxx`/`P1-RC-xxx` 并按 §10 触发 MR0/MR1（本计划仅登记，不实施修复）。
- 报告产出即更新 `docs/audits/arm-index.md`（新 RC finding 入对应分区）。

## Non-Goals

- **不修复 finding**（修复属 MR0 即时通道 / MR1 R1.0 展开的 RC-R1.n；本计划是审计，结果表面 = 一份报告 + arm-index 登记）。
- **不修改真相源**（product-scope / hr use-cases / payroll.md / payroll-simulation.md / employee-survey.md / state-machine.md 需求契约段落；§9 冻结条款——分歧记入报告，不直改真相源）。
- **不修改代码/ORM/api.xml/BizModel/Processor/view.xml**（只读审计）。
- **不审计其他 MA1 切片**（A1.14 只覆盖 UC-HR-03/04/10/11；UC-HR-01/05/07/08/12 归 A1.12，UC-HR-02/06/09 归 A1.13）。**UC-HR-03 跨域 cost-collection 归集与 projects 域 P1-MA2-068 同型吞异常归 projects 切片**，本切片核验 hr 侧 Timesheet APPROVED 触发，不重复核验 projects dispatcher 本身。
- **不重跑既有状态机/代码质量行为审计**（§去重协议：A2.7b/A4.4 已证实状态机迁移守卫/事务边界/代码质量/个税算术/过账链路，且 A4.4 4 项 P1 已 resolved，只补需求视角差异；不重审架构/代码质量维度）。

## Task Route

- Type: `verification or audit work`（需求→实现符合性五级追踪审计；非实现变更、非需求澄清）
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（审计契约 §1-§10 + §去重协议）+ `docs/backlog/requirement-compliance-roadmap.md`（A1.14 工作项 + Work Item Details MA1）+ `docs/audits/rc-requirement-baseline-inventory.md`（A1.14 UC 锚点）+ `docs/design/human-resource/use-cases.md`（L1 真相源）+ `docs/design/human-resource/payroll.md` + `payroll-simulation.md` + `employee-survey.md` + `state-machine.md` + `README.md`（L2 设计参考，非真相源）+ `docs/audits/arm-index.md`（finding 衔接）+ A2.7b/A4.4 报告（L5 既有证据）
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA1 全部 A1.x 指定）。该技能定义多维审计 prompt 范式，本切片需求↔实现符合性审计复用其维度框架；其必需输入（owner doc + use-cases + 代码路径 + 测试）均已就绪。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 审计以读代码/测试/报告为主（纯分析）。**L5 行为证据**默认复用 A2.7b/A4.4 审计（方法论 §去重协议），无需起服务；若需对存疑点做即时行为确认，可跑既有 JUnit（`mvn test -pl module-hr/erp-hr-service -Dtest=TestErpHrPayrollEngine,TestIncomeTaxCalculator,TestErpHrPayrollSimulation,TestErpHrSalaryWorkflowApproval,TestHrPostingFaultInjection,TestErpHrSurveyCrudSmoke`），不引入新依赖。§8 过程纪律自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter，退出码恒 0；本审计无生产代码变更故无回归风险，仅记录 actual vs baseline）。

## Execution Plan

### Phase 1 - 五级追踪矩阵填充与逐 UC 符合性结论 + resolved finding HEAD 复核

Status: completed
Targets: `docs/audits/<执行时间戳>-rc-ma1-a1-14-hr-f3-payroll-survey.md`（落盘 §1-§5；命名遵循方法论 §归档规范）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: M0.1 + M0.2 done（方法论契约 + UC 锚点就绪）

- [x] `Proof` 对 UC-HR-03/04/10/11 **逐 UC 一矩阵行**填 L1-L5（§1 格式）：L1 逐字引用 `use-cases.md:27/:39/:113/:125` 验收标准原文（禁止转述）；L2 引用 `payroll.md`（薪酬核算 + §五/§六/§七 双轴状态机 + 计提 SALARY(270)+290+300 过账 + §6/§9.1 approve→APPROVED 联动）+ `payroll-simulation.md`（What-If + 5 态 + convertToFormal）+ `employee-survey.md`（匿名 + 驱动因子 + 聚合）+ `state-machine.md`（工时表 + 薪酬审批 + 仿真/银行文件，标注"设计参考，冲突以 L1 为准"）；L3 引用 `ErpHrTimesheetBizModel.java:line`（4 态 + submit + totalHours）+ `ErpHrTimesheetLineBizModel:line` + `ErpHrSalaryBizModel.java:line`（calculateSalary/runPayroll/markPaid）+ `ErpHrSalaryCalculateSalaryProcessor`/`RunPayrollProcessor:line` + `PayrollCalculator:line`（公式 + socialInsuranceER/housingFundER）+ `IncomeTaxCalculator.java:line`（累计预扣 + resolveBracket 末档 + parseCumulativeData）+ `SocialInsuranceCalculator:line`（基数钳制）+ `SalaryPostingDispatcher`/`SalaryPostingExecutor`/`SalaryPostingProvider:line`（四类 PostingEvent + REQUIRES_NEW Facade）+ `ErpHrSalarySimulationBizModel.java:line`（What-If 克隆 + readSalaryField/applyOverride + 5 态）+ `ErpHrSalarySimulationConvertToFormalProcessor:line`（双层容错 + PAID 守卫）+ `ErpHrSurveyBizModel.java:line`（CRUD + 类型 + 发布/截止）+ `ErpHrSurveyQuestionBizModel`/`ErpHrSurveyResponseBizModel`/`ErpHrSurveyAnswerBizModel`/`ErpHrSurveyResultBizModel:line`（匿名 respondentHash + aggregateResult + eNPS）；L4 引用 `Test*.java#method`（注明断言强度）；L5 复用 A2.7b/A4.4 + 本切片差异。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` 重点核验**候选缺口/偏离**（逐条验收标准对照）：UC-HR-03——①TimesheetLine 分项目/任务/活动类型记录小时数；②同一日工时超过 24h 校验；③totalHours 汇总；④APPROVED 工时归集 cost-collection（跨域 projects 订阅）；⑤项目经理审批 APPROVED/驳回 REJECTED。UC-HR-04——⑥基本工资(合同月薪+出勤比例)；⑦加班费(overtimeMinutes×费率)；⑧绩效奖金；⑨社保扣除；⑩公积金扣除；⑪个税(累计预扣)；⑫实发=基本+加班+绩效-社保-公积金-个税；⑬合同/考勤数据读取；⑭paymentStatus=PENDING；⑮HR 审核发薪；⑯跨域财务过账 SALARY 凭证；⑰缺失配置跳过告警。UC-HR-10——⑱复制源快照(薪酬项目/累计个税/考勤)；⑲调整应变计算；⑳5 态审批(DRAFT/IN_REVIEW/APPROVED/REJECTED/CONVERTED)；㉑convertToFormal 创建正式 ErpHrSalary；㉒目标期间 PAID 不允许转正式。UC-HR-11——㉓调研类型(ANNUAL_ENGAGEMENT/PULSE/eNPS/ADHOC)；㉔题库(评分/选择/开放)；㉕驱动因子分类(GROWTH/RECOGNITION/MANAGEMENT/WELLBEING/ALIGNMENT)；㉖匿名模式 employeeId 不存储仅存 respondentHash 防重复；㉗发布(OPEN)/截止(CLOSED)；㉘自动聚合 ErpHrSurveyResult；㉙eNPS 得分 + 仪表盘(趋势/部门对比/驱动因子分析)。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` **resolved finding HEAD 复核**（会计正确性类关键证据）：对薪酬与调研相关 finding 在当前 HEAD 实际落地（按逻辑非行号核验）。**P1-MA4-017 计提+公司承担过账链路（resolved R1.26）**——HEAD 复核 tryPostAccrual 是否已接线（xbiz source append 或 BizModel 覆盖 approve）+ socialInsuranceER/housingFundER 是否持久化 + 290/300 event 是否生成（会计正确性类 Q4 无例外，闭环关键证据）；**P1-MA4-016 个税高档 NPE（resolved R1.26）**——resolveBracket 末档 null 防御；**P1-MA4-018 parseCumulativeData 吞（resolved R1.26）**——静默吞移除；**P1-MA4-019 测试有效性（resolved R2.13）**——高档边界/过账悬挂/累计解析/公司承担负向断言补强；P1-MA2-043/044/045/047/048 状态机与吞异常 + P1-MA2-041 调研桩（resolved 状态经 arm-index grep 确认，未确认者按"未定"处理）。逐条记录复核结论（已落地/回退/部分落地/仍 open successor）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Decision` 按 §2 判据对每 UC 给出符合性结论（P0/P1/P2/接受）：UC-HR-04 业财过账（P1-MA4-017 HEAD 复核：resolved R1.26 则接受 on ⑯，仍 open 则 Q4 会计正确性类维持 P1 触发 MR1；P1-MA4-016/018 HEAD 复核 resolved 则接受，open 则 dedup；P1-MA4-019 测试有效性 HEAD 复核）。UC-HR-03 工时表（P1-MA2-043/044 HEAD 复核 + 24h 校验/cost-collection 归集执行时核验）。UC-HR-10 模拟（执行时 HEAD 核验 What-If/convertToFormal）。UC-HR-11 调研（P1-MA2-041 桩 HEAD 复核 + 匿名/聚合/驱动因子执行时核验：缺失倾向 P1①功能缺失）。每结论须列明命中判据编号 + 三源对照。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

- [x] 报告 §1-§5 已落盘：UC-HR-03/04/10/11 各一矩阵行（验收标准全覆盖），L1 逐字引用、L3 含行号、L4 注明断言强度、L5 标注复用 A2.7b/A4.4 来源
- [x] 每 UC 有符合性结论（P0/P1/P2/接受）+ §2 判据编号；候选缺口 ①-㉙ 有明确分级（非悬空"待查"）；**P1-MA4-017 计提+公司承担过账 HEAD 复核结论已记录（会计正确性类 Q4 关键证据）**；P1-MA4-016/018/019 + P1-MA2-043/044/041 HEAD 复核结论已记录

### Phase 2 - finding 登记 / arm-index 衔接 / 静态存疑点 / 过程纪律自检 / 报告完整性

Status: completed
Targets: `docs/audits/<执行时间戳>-rc-ma1-a1-14-hr-f3-payroll-survey.md`（落盘 §6-§9，报告定稿）；`docs/audits/arm-index.md`（新 RC finding 入分区）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1 完成（矩阵 + 结论已出）

- [x] `Decision` **复用 or 新增 裁决**（§7）：产出 finding 前 grep `arm-index.md` hr 薪酬/工时/模拟/调研同域同控制点（如 P1-MA4-016/017/018/019、P1-MA2-043/044/045/047/048、P1-MA2-041）后裁决——同根因同控制点 → 复用既有 ID（追加 RC 交叉引用注记，不新建）；新根因/新功能点（如 UC-HR-11 调研匿名 respondentHash/聚合/eNPS = 需求契约视角新维度 / UC-HR-04 公式完整性若发现缺失）→ 新建 `P0-RC-xxx`/`P1-RC-xxx` 并列明与既有 finding 的差异依据。**特别注意**：UC-HR-04 业财过账与 P1-MA4-017 同根因则交叉引用而非重复新建；UC-HR-03 24h 校验/cost-collection 归集与 projects P1-MA2-068 不同控制点[hr 侧触发 vs projects dispatcher 吞异常]。禁止未经比对直接新建。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Add` 报告 §6 与 arm-index 衔接段：列明每条 finding 的复用/新增裁决 + 双向可追溯（finding ID ↔ 修复行预留 MR0/MR1）。
      - Skill: none
- [x] `Add` 报告 §7 静态存疑点清单（供 MA4 展开）：登记本切片 L5 无法静态定论、需运行时确认的点（如计提+公司承担过账运行时 approve→APPROVED 触发链、个税高档边界运行时触发、模拟 What-If 克隆隔离运行时不污染、调研匿名 respondentHash 运行时防重复、聚合 eNPS 运行时计算；每存疑点一行；无则注明"无"）。**P0 即时通道**：若 Phase 1 定级出 P0，按 §10 在报告登记并在本计划记录"已触发 MR0 追加 R0.n 实体行"（本计划不实施修复）。
      - Skill: none
- [x] `Proof` 报告 §8 过程纪律自检段（§8 模板）：实际运行 `bash docs/audits/nop-compliance-checker.sh` 并附 actual vs baseline 汇总表（本审计无生产代码变更，注明"无回归风险"）；closure-audit 独立性声明；与 arm-index 交叉去重声明。**不以 checker 脚本退出码 0 作为门控通过依据**（区分 reporter vs CI 门控）。
      - Skill: none
- [x] `Add` 报告 §9 与 MA2/MA4 报告差异增量声明：声明复用 A2.7b（考勤与工资八组件状态机 + 工资支付轴双守卫 + 仿真 5 态 + convertToFormal 双层容错 + markPaid REQUIRES_NEW Facade + P1-MA2-043/044/045/047/048 + P2-MA2-052 finding）+ A4.4（薪酬/过账/模拟引擎链路代码质量 + 七面扎实 + **P1-MA4-016/017/018/019 resolved R1.26/R2.13** + P2-MA4-008/009 finding）已证实结论，列明本切片只补的需求视角差异（UC-HR-03 工时表 24h 校验+cost-collection 归集 + UC-HR-04 薪酬核算公式完整性+合同/考勤读取 + UC-HR-10 模拟 What-If+convertToFormal + UC-HR-11 调研匿名+聚合+驱动因子 + **resolved finding HEAD 复核 R1.26/R2.13 落地确认**）。
      - Skill: none
- [x] `Add` 报告产出即更新 `docs/audits/arm-index.md`：新 `P*-RC-xxx` 入对应分区（MA1 finding 区），既有行追加 RC 交叉引用注记。
      - Skill: none
- [x] `Proof` 报告 9 段完整性自检（§6 段落完整性自检）：落盘前自查 §1-§9 全部存在；缺任一段即回到 Phase 补齐。
      - Skill: none

Exit Criteria:

- [x] 报告 §6-§9 已落盘，9 段齐全；finding 复用/新增裁决均有 arm-index grep 依据（无未经比对新建）
- [x] 新 RC finding 已写入 `arm-index.md` 对应分区；静态存疑点清单已登记（供 A4.2 展开）
- [x] §8 自检段含 checker actual vs baseline 实测表 + 独立性 + 交叉去重声明

## Draft Review Record

- Independent draft review iteration 1: `acceptable as-is`（独立子代理 `ses_03cef2da7ffeY94nNLtsQC82lj`，fresh session，未起草本计划）。逐项实测核验（最高风险会计/过账切片）：roadmap 对齐（A1.14 / UC-HR-03/04/10/11 / Deps=M0.1+M0.2 done / Skill）、4 UC 锚点 :27/:39/:113/:125 全匹配（完整枚举无跳无合并）、L3 全部代码路径存在（ErpHrTimesheetBizModel + TimesheetLine + ErpHrSalaryBizModel calculateSalary:80/runPayroll:89/markPaid:97 + Salary CalculateSalary/RunPayrollProcessor + PayrollCalculator + IncomeTaxCalculator resolveBracket:228/parseCumulativeData:169 + SocialInsuranceCalculator + SalaryPostingDispatcher/Executor/Provider[四类 SALARY/SALARY_PAYMENT/SOCIAL_INSURANCE_ER/HOUSING_FUND_ER :55-58 全在] + ErpHrSalarySimulationBizModel convertToFormal:344/readSalaryField:515 + ConvertToFormalProcessor + SimulationItemAdjustmentBizModel + ErpHrSurvey* 5 BizModel 全在）、L4 6 测试 + 2 E2E 全存在。**L5 dedup 输入 + resolved 状态声称（最高风险，全验证）**：A2.7b/A4.4 报告存在；**P1-MA4-016 arm-index:567 "✅ resolved (R1.26 done)" 一致** / **P1-MA4-017 arm-index:568 "✅ resolved (R1.26 done)" 一致** / **P1-MA4-018 arm-index:569 "✅ resolved (R1.26 done)" 一致** / **P1-MA4-019 arm-index:570 "✅ resolved (R2.13 done)" 一致**（正确区分 R1.26 vs R2.13）；P1-MA2-041/043/044/045/047/048 全在 arm-index:230-237 均 resolved R1.15/R1.16，本计划正确框架为"resolved 状态经 arm-index grep 确认，未确认者按未定处理"未误称 open；P2-MA4-008/009、P2-MA2-052、P2-MA1-020 全在。跨切片边界正确（**P1-MA2-068 TimesheetPostingDispatcher 归 projects arm-index:253，本计划正确收窄到 hr 侧 Timesheet APPROVED 触发 :56 不重审 projects dispatcher**；UC-HR-11 调研正确归 A1.14 非 A1.12）。只读审计删门控有据（:130-139）、会计保护区域（SalaryPostingDispatcher/PostingProvider/VoucherFact/PostingProcessor）ask-first 延后 MR0/MR1 合规（:40/:146）。**会计 Q4 强调正确**（P1-MA4-017 计提+公司承担过账 framed 为 Q4 关键证据 :48 "resolved R1.26→dedup 闭环；仍 open→Q4 会计正确性类维持 P1 触发 MR1" 非可降级，:92 HEAD 复核为门控证据）。**无阻塞 issue**，共识达成，转 active。

## Closure Gates

> 本计划为**只读审计**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控——审计报告产出不触发编译或测试。验证 = 报告 9 段完整性 + 五级矩阵逐 UC 覆盖 + resolved finding HEAD 复核（含会计正确性类 P1-MA4-017）+ finding arm-index 衔接 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [x] 范围内行为完成：A1.14 报告 9 段齐全 + UC-HR-03/04/10/11 逐矩阵行 + resolved finding HEAD 复核（含 P1-MA4-017 会计正确性类）+ finding 登记入 arm-index
- [x] 相关文档对齐：报告与方法论 §1-§10 + §去重协议一致；与 rc-requirement-baseline-inventory A1.14 锚点一致
- [x] 已运行验证：报告 9 段完整性自检 + §8 checker actual vs baseline 实测记录 + finding 复用/新增裁决可追溯（本计划无代码变更故不跑 build/test）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、退出标准、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此项留空作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### finding 的修复实施

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划是审计，结果表面 = 报告 + arm-index 登记。finding 的修复按方法论 §10 经 MR0（P0 即时通道）/ MR1（R1.0 展开 RC-R1.n，P1 批量）实施；**触及会计过账逻辑（SalaryPostingDispatcher/PostingProvider/计提+公司承担凭证 VoucherFact/PostingProcessor 核心路径）的修复行须 ask-first + 独立 plan-audit**（§5 保护区域暂停协议，会计过账核心路径类）。本审计闭环不阻塞于修复落地。
- Successor Required: yes（MR0/MR1 按本报告 finding 交叉引用展开修复行）

## Closure

Status Note: 已完成（独立结束审计 pass，2026-08-03）。报告 `docs/audits/2026-08-03-0000-rc-ma1-a1-14-hr-f3-payroll-survey.md` 9 段齐全；UC-HR-03/04/10/11 逐矩阵行 + 验收标准 ①-㉙ 全枚举分级；resolved finding HEAD 复核（P1-MA4-017 方案B Deferred 在 Q4=(a) 下重开经 MR1 + P1-MA4-016/018/019 + P1-MA2-044/047/048 已落地）；2 项新 P1（P1-RC-015 UC-HR-03 24h/totalHours、P1-RC-016 UC-HR-11 匿名/聚合/eNPS）+ 3 项复用（P1-MA4-017/P1-MA2-043/P1-MA2-041）入 arm-index；§8 checker actual=baseline 全等。roadmap A1.14 推进至 done。finding 修复属 MR1 successor（R1.0 展开为 RC-R1.n），非阻塞本审计闭环。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理 `ses_03cc58c4cffefgbXgjCiCgQzhW`（新会话，未执行本计划）
- Evidence: 独立核验 7 项全 pass——(1) 计划 Phase 1/2 全 [x] + Status completed；(2) 报告 §1-§9 齐全 + 4 UC 独立矩阵行 + L1 逐字 + L3 file:line + L4 断言强度；(3) **P1-MA4-017 HEAD 复核结论准确**（grep 确认 tryPostAccrual 零生产调用方 + 无 xbiz + 无 ORM ER 列 + R1.26 Phase 1 显式裁决方案B Deferred）；(4) P1-RC-015/016 入 arm-index + 3 复用 finding 交叉引用注记；(5) §8 checker actual=baseline 全等（独立复跑）；(6) L3 引用 spot-check（IncomeTaxCalculator:231-234 null 防御 / ErpHrSurveyBizModel 18 行桩 / Simulation 5 态迁移行号准确）；(7) git status 确认仅 docs/ 变更，零生产代码/ORM/api.xml/view.xml 修改。Verdict `passes closure audit`。P1-MA4-017 Q4 会计正确性类重开经方法论确认 sound（audit-remediation 方案B 不约束 requirement-compliance Q4=(a)）。

Follow-up:

- finding 修复属 MR0（P0 即时通道）/ MR1（R1.0 展开 RC-R1.n）successor，非阻塞本审计闭环（§Deferred But Adjudicated 已 adjudicated）
