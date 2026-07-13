# 2026-07-14-0215-3-hr-direct-action-e2e hr 域 DIRECT 业务动作浏览器层 E2E

> Plan Status: completed
> Last Reviewed: 2026-07-14
> Source: `docs/plans/2026-07-09-2004-1-business-action-e2e-maintenance-projects-quality.md` Deferred「全 18 域全业务动作覆盖（DIRECT 域剩余）」（Successor Required: yes，触发条件「当需按域推进全 DIRECT 业务动作浏览器层覆盖时」——**已满足**：当前项目重点为各域细化端到端验证，hr 域为 Tier-1 未覆盖域，@BizMutation 数量最多 45 个；`erp_hr_salary` 带 useWorkflow 但 calculateSalary/runPayroll/voidSalary 及其余 ~13 实体全部 DIRECT 浏览器层可达；markPaid happy-path 因 xwf 审批轴不可达归 negative-path + Deferred）
> Related: `2026-07-09-2004-1`（DIRECT 域扩展范式源）、`2026-07-14-0215-1`（同批 N=1 assets）、`2026-07-14-0215-2`（同批 N=2 contract/drp）、`2026-07-09-2330-1`（useWorkflow 审批轴浏览器层不可行权威裁决）、`docs/testing/e2e-runbook.md`（业务动作套件）
> Audit: required

## Current Baseline

hr 域后端业务逻辑已全部落地（extended-roadmap M3 3.7/3.8/3.9 + core-business S-items 1100-7 + 1100-2 + 0517-2 全 done），共 45 个 @BizMutation：

- **薪酬引擎**（`ErpHrSalaryBizModel`）：`calculateSalary(employeeId,year,month)` / `runPayroll(year,month)` / `markPaid(salaryId)` / `voidSalary(salaryId)` / `generateBankFile(year,month,bankId)` — 计算引擎（出勤比例→基本工资→津贴→加班→绩效→社保→公积金→个税累计预扣→实发）+ SALARY/SALARY_PAYMENT 过账 + 银行代发文件
- **薪酬模拟**（`ErpHrSalarySimulationBizModel`）：`createSimulation` / `adjustItem` / `applyBatchAdjustment` / `submitForReview` / `approve` / `reject` / `convertToFormal` — What-If 引擎 + 状态机
- **招聘**（`ErpHrRecruitmentBizModel`）：`moveToScreening` / `scheduleInterview` / `makeOffer` / `hire` / `reject` / `close` — 6 动作状态机（HIRED→自动创建员工+合同）
- **休假审批**（`ErpHrLeaveRequestBizModel`）：`submit` / `approve` / `reject` / `cancel` — 4 动作状态机 + LeaveBalance 联动
- **排班调换**（`ErpHrShiftSwapRequestBizModel`）：`submit` / `approve` / `reject` / `cancel` — 4 动作状态机
- **考勤**（`ErpHrAttendanceBizModel`）：`clockIn` / `clockOut` — 打卡端点 + (employeeId,date) 唯一约束
- **合同**（`ErpHrEmploymentContractBizModel`）：`renew` / `expireOverdueContracts`

ORM：`erp_hr_salary` = `useWorkflow="true"` + use-approval tagSet。`calculateSalary`/`runPayroll`/`voidSalary` 为 DIRECT @BizMutation 浏览器层可达。但 `markPaid` 守卫硬编码 `approveStatus==APPROVED`（无 config 门控），而 salary 审批轴经 useWorkflow xwf（`salary-approval/v1.xwf`），浏览器层 submit 经 sysUser(0) 阻塞不可达（同 2330-1 裁决）。故 `markPaid` **happy-path（PENDING→PAID）浏览器层不可达**，仅 **negative-path**（UNSUBMITTED salary 调 markPaid 抛 ErrorCode 守卫）浏览器层可测。`generateBankFile(year,month,bankId)` 依赖 APPROVED+PENDING 行，同样仅守卫路径可测。其余 ~13 个 HR 实体无 useWorkflow / 无 useApproval，全部 DIRECT happy-path 可达。

**浏览器层 E2E 缺口**：hr 域 0 个 business-action spec。

**E2E 基础设施就绪**：`tests/e2e/business-actions/_helper.ts` 三原语经 10 域验证可复用。hr 域种子数据已有（department/employee/salary_simulation/simulation_item_adj 4 表 1045-1）。

## Goals

- hr 域核心 DIRECT 业务动作经 GraphQL `/graphql` 浏览器层全栈可达性 + 状态机迁移验证
- 覆盖 4 条高价值 DIRECT 路径：薪酬引擎（calculateSalary 计算触发 + markPaid 守卫负路径）、薪酬模拟 What-If、招聘漏斗（含 HIRED→员工联动）、休假审批+考勤打卡
- 复用既有三原语范式验证在 hr 多型状态机（计算引擎 / What-If 模拟 / 招聘漏斗 / 审批状态机 / 打卡端点）下的可复用性

## Non-Goals

- **`erp_hr_salary` 审批轴 xwf 段 + markPaid/generateBankFile happy-path**（APPROVED_MANAGER→PAID 经 useWorkflow 审批）——经 2330-1 权威裁决浏览器层不可行（sysUser(0) 阻塞 submit，salary 无法达 APPROVED）；`markPaid` 硬守卫 `approveStatus==APPROVED` 无 config 旁路。本计划仅覆盖 `markPaid` negative-path（UNSUBMITTED 守卫拒绝）+ calculateSalary/runPayroll/voidSalary DIRECT happy-path。markPaid/generateBankFile happy-path 归 Deferred successor
- **排班生成（generateRotation）/ 批量分配（assignBatch / copyFromPeriod）深度编排**——批量+轮换生成复杂度高，归 successor
- **胜任力评估 + 差距分析 + 发展计划**——1100-2 已落地但属低频人才发展面，归 successor
- **员工调动（transferEmployee）跨域编排**——0517-2 已落地但跨域合同/休假联动复杂，归 successor
- **薪酬过账精确数值断言**——聚焦引擎触发可观测性（salary 金额非空 + 状态翻转），凭证行数值断言归 successor
- **合同到期扫描 Job（expireOverdueContracts）**——nop-job 定时任务非浏览器面动作

## Task Route

- Type: `verification work`（扩展现有 Playwright E2E 套件覆盖至 hr 域 DIRECT 业务动作）
- Owner Docs: `docs/testing/e2e-runbook.md`（业务动作套件 + 调用范式）、`docs/design/human-resource/payroll.md`、`docs/design/human-resource/payroll-simulation.md`、`docs/design/human-resource/shift-scheduling.md`
- Skill Selection Basis: 浏览器层 E2E 测试编写 → 无匹配技能（Playwright 浏览器层非 `nop-testing` 后端快照范畴）；沿用 `_helper.ts` 既有范式 → `Skill: none`

## Infrastructure And Config Prereqs

No infra prereqs beyond existing baseline。复用现有 Playwright 配置 + webServer JVM 参数。

> 注：薪酬引擎依赖 HR 配置实体（`ErpHrSalaryItem` / `SocialInsuranceConfig` / `TaxConfig` 等种子）。`calculateSalary` 还需 active `ErpHrEmploymentContract`（`PayrollCalculator.findActiveContract` 无合同时抛错）。薪酬模拟 `createSimulation` 需源期间已有 salary 行（否则 `ERR_HR_SIMULATION_SOURCE_NOT_FOUND`）。这些预置按自包含 setup 在 spec 内建配置实体 + 合同 + 源 salary（对齐 finance recon 自包含 partner 范式），或种子补齐配置行（Explore 阶段裁定）。

## Execution Plan

### Phase 1 - 薪酬引擎 + 薪酬模拟 E2E

Status: completed
Targets: `tests/e2e/business-actions/hr-payroll.action.spec.ts`（新建）、`tests/e2e/business-actions/hr-salary-simulation.action.spec.ts`（新建）
Skill: none

- Item Types: `Add | Proof`
- Prereqs: 无（自包含 setup）

- [x] `Decision | Explore`: 裁定薪酬引擎 setup 依赖——`calculateSalary` 需 active `ErpHrEmploymentContract`（monthlySalary）+ `ErpHrSalaryItem` / `SocialInsuranceConfig` / `TaxConfig` 配置链。Explore 实测最小预置集（建 employee + contract + 配置实体）使 calculateSalary 浏览器层可达。薪酬模拟 `createSimulation` 需先 runPayroll 产源期间 salary 行。裁定结果记入执行日志。
  - Skill: none
  - **Explore 裁定（实测）**：calculateSalary 最小预置集 = ErpHrEmployee(ACTIVE) + ErpHrEmploymentContract(ACTIVE, monthlySalary) + ErpHrSocialInsuranceBase(cityCode) + ErpHrSocialInsuranceConfig(HOUSING_FUND, employeeRate/companyRate) + ErpHrTaxConfig(year, taxBrackets JSON 七级累进)。SocialInsuranceCalculator 仅需 ≥1 config（HOUSING_FUND 即可，社保项可 0）+ HousingFund config 必需；IncomeTaxCalculator 需 TaxConfig(year)（无则 ERR_TAX_CONFIG_NOT_FOUND）。**薪酬模拟源 salary**：裁定直接 `__save` 源 ErpHrSalary(2025-6, PENDING) 作模拟源期间——runPayroll 遍历 ALL active 员工（含种子 HR-EMP-001/002 缺合同/社保配置）会抛错，不可行；__save 源 salary 是唯一自包含路径。adjustItem/convertToFormal 经 recalculateWithOverrides → incomeTaxCalculator 需 TaxConfig(目标年 2026)。**已知约束**：adjustmentReason VARCHAR(20) + ext:dict，须用合法 dict 值（SALARY_CHANGE）；applyBatchAdjustment 返回 Map<String,Object> GraphQL 不支持字段选择，须原始 mutation 无选择集。
- [x] `Add`: **薪酬引擎 spec** `hr-payroll.action.spec.ts`
  - `calculateSalary(employeeId, year, month)` 浏览器层可达性：自包含建 `ErpHrEmployee`（ACTIVE）+ `ErpHrEmploymentContract`（ACTIVE + monthlySalary）+ 配置链最小预置 → 调 `calculateSalary` → `verifyState` 断言 `ErpHrSalary` 行创建 + grossSalary/netSalary 非空 + approveStatus=UNSUBMITTED + paymentStatus=PENDING
  - `voidSalary(salaryId)` 作废回退：calculateSalary 后调 voidSalary → 断言状态回退
  - `markPaid(salaryId)` **negative-path 守卫**：calculateSalary 产 UNSUBMITTED salary → 调 markPaid → 断言抛 `ERR_SALARY_ILLEGAL_STATUS_TRANSITION`（approveStatus≠APPROVED 守卫拒绝）+ salary 状态不变
  - `generateBankFile(year, month, bankId)` **negative-path 守卫**：无 APPROVED 行 → 返回空/守卫拒绝
  - Skill: none
- [x] `Add`: **薪酬模拟 What-If spec** `hr-salary-simulation.action.spec.ts`
  - 模拟生命周期正向链：前置先 `runPayroll` 产源期间 salary 行（`createSimulation` 无源 salary 时抛 `ERR_HR_SIMULATION_SOURCE_NOT_FOUND`）→ 自包含建 `ErpHrSalarySimulation`（DRAFT 入口，冻结源快照）→ `adjustItem`（调整薪酬项目金额）→ `submitForReview` → status=IN_REVIEW → `approve` → status=APPROVED → `convertToFormal` → status=CONVERTED + 正式 salary 回链
  - `reject` 路径：IN_REVIEW → reject → REJECTED
  - `applyBatchAdjustment` 批量调整（FIXED/RATIO 范式抽样 1 种）
  - 非法迁移守卫（DRAFT→approve 抛 ERR，因无调整项前置）
  - Skill: none

Exit Criteria:

- [x] 2 spec 文件经 `npx playwright test tests/e2e/business-actions/hr-payroll.action.spec.ts tests/e2e/business-actions/hr-salary-simulation.action.spec.ts --workers=1` 全绿
- [x] 薪酬 calculateSalary 计算触发（金额非空 + UNSUBMITTED/PENDING）+ markPaid negative-path 守卫 + 模拟状态翻转均经 `verifyState` `__get` 独立断言

### Phase 2 - 招聘漏斗 + 休假审批 + 考勤打卡 E2E

Status: completed
Targets: `tests/e2e/business-actions/hr-recruitment.action.spec.ts`（新建）、`tests/e2e/business-actions/hr-leave-attendance.action.spec.ts`（新建）
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 1 范式验证

- [x] `Add`: **招聘漏斗 spec** `hr-recruitment.action.spec.ts`
  - 6 动作状态机正向链：自包含建 `ErpHrRecruitment`（OPEN 入口）→ `moveToScreening` → SCREENING → `scheduleInterview` → INTERVIEW → `makeOffer` → OFFERED → `hire` → HIRED + 断言 `ErpHrEmployee` 自动创建（employeeId 回写 + 合同 ACTIVE）
  - `reject` / `close` 异常路径
  - 非法迁移守卫（HIRED→moveToScreening 抛 ErrorCode message token）
  - Skill: none
- [x] `Add`: **休假审批 + 考勤打卡 spec** `hr-leave-attendance.action.spec.ts`
  - 休假审批状态机：自包含建 `ErpHrLeaveRequest`（DRAFT/PENDING 入口 + 员工 + 日期范围）→ `submit` → `approve` → 断言 status 翻转（`ErpHrLeaveRequestBizModel` 代码容忍 null LeaveBalance——`if (balance == null) return;`，故不建 LeaveBalance 亦可测 approve 状态翻转；LeaveBalance usedDays 联动断言归 successor）
  - `reject` / `cancel` 路径 + 日期重叠守卫
  - 考勤打卡端点：自包含建员工 → `clockIn(employeeId, ...)` → 断言 `ErpHrAttendance` 行创建 + clockInTime 非空 → `clockOut` → clockOutTime 非空 + (employeeId,date) 唯一约束守卫（重复 clockIn 抛 ErrorCode）
  - Skill: none

Exit Criteria:

- [x] 2 spec 文件经 `npx playwright test tests/e2e/business-actions/hr-recruitment.action.spec.ts tests/e2e/business-actions/hr-leave-attendance.action.spec.ts --workers=1` 全绿
- [x] 招聘 HIRED→员工联动 + 休假审批状态翻转 + 考勤打卡均经 `verifyState` 独立断言

## Draft Review Record

- Independent draft review iteration 1: needs revision (ses_0a34c027affe) — 所有 @BizMutation 方法/useWorkflow 标记/recruitment 状态链/hire 自动建员工+合同/helper 原语经实时仓库核实一致。发现 1 项 Blocker B1：`markPaid` 硬守卫 `approveStatus==APPROVED` 无 config 旁路，salary 审批轴经 useWorkflow xwf 浏览器层不可达（2330-1 同根因），markPaid **happy-path 不可测**；计划内部矛盾（Non-Goal 排除 xwf 但 Goal 含 markPaid happy-path）。
- Independent draft review iteration 2: accept (ses_0a34469beffe) — B1 核实已解决（`markPaid` 硬守卫 `approveStatus==APPROVED` 经实时仓库确认无 config 旁路；calculateSalary 确认设 UNSUBMITTED 非 PENDING；negative-path 守卫拒绝浏览器层可行）；EmploymentContract 依赖 + 源 salary 依赖 + generateBankFile 3 参签名均确认；规则合规全 PASS；无新 Blocker。计划可作为执行契约进入实施。

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。

- [x] 范围内行为完成：4 spec 覆盖 hr 4 条 DIRECT 路径（薪酬引擎 / 薪酬模拟 / 招聘漏斗 / 休假+考勤）
- [x] 相关文档对齐：`docs/testing/e2e-runbook.md` 业务动作表 +hr 行 + 套件计数更新
- [x] 已运行验证：`npx playwright test tests/e2e/business-actions/hr-*.action.spec.ts --workers=1` 全绿 + 全套件回归无新增失败
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### hr markPaid / generateBankFile happy-path（salary xwf 审批轴依赖）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: `markPaid` 硬守卫 `approveStatus==APPROVED`，salary 审批轴经 useWorkflow xwf（`salary-approval/v1.xwf`），浏览器层 submit 经 sysUser(0) 阻塞不可达（同 2330-1 裁决）。本计划仅覆盖 markPaid negative-path（UNSUBMITTED 守卫拒绝）+ calculateSalary/runPayroll/voidSalary happy-path。
- Successor Required: `yes`（触发条件：useWorkflow 浏览器层身份映射落地时，或平台支持 salary 审批轴 DIRECT 化时——同 2330-1 重评触发条件）

### hr 薪酬过账精确数值断言（SALARY/SALARY_PAYMENT 凭证行）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 本计划聚焦引擎触发可观测性（salary 金额非空 + 状态翻转）。凭证行科目码/金额精确数值断言（如 SALARY Dr 6601 / Cr 2211 等）属数值断言层增量，依赖薪酬科目配置链完整性。
- Successor Required: `yes`（触发条件：hr 薪酬过账凭证行数值断言需求落地时）

### 排班批量生成 + 调换审批深度编排 E2E

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: `generateRotation`（轮换模式 JSON 序列 + 错峰）+ `assignBatch`（员工组×日期范围）+ 排班调换互换双方班次复杂度高。本计划仅覆盖休假审批+考勤打卡基础状态机。
- Successor Required: `yes`（触发条件：排班深度编排浏览器层 E2E 需求落地时）

### 胜任力评估 / 差距分析 / 发展计划 / 员工调动 E2E

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 1100-2 胜任力段 + 0517-2 调动段已落地但属低频人才发展面 + 跨域编排，边际收益递减。
- Successor Required: `yes`（触发条件：人才发展域浏览器层 E2E 需求落地时）

### hr 合同到期扫描 Job + renew 续签

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: `expireOverdueContracts` 为 nop-job 定时任务非浏览器面动作；`renew` 续签为单动作低价值。
- Successor Required: `no`

## Closure

Status Note: 已完成。hr 域 4 spec（15 测试）全绿覆盖 4 条 DIRECT 路径，business-actions 全套件 89 测试回归无新增失败。

Closure Audit Evidence:

- Auditor / Agent: independent closure audit subagent (ses_0215-3-closure) — 裁决 PASS：
  - 范围核实：4 spec 文件存在且覆盖薪酬引擎（calculateSalary/voidSalary DIRECT happy-path + markPaid/generateBankFile negative-path 守卫）/ 薪酬模拟 What-If（createSimulation→adjustItem→submitForReview→approve→convertToFormal 正向链 + reject + applyBatchAdjustment FIXED + 非法迁移守卫）/ 招聘漏斗（OPEN→...→HIRED + hire 员工/合同联动 + reject/close + 非法守卫）/ 休假审批+考勤打卡（DRAFT→SUBMITTED→APPROVED→CANCELLED + reject + 日期重叠守卫 + clockIn/clockOut + 重复 clockIn 守卫）
  - 验证核实：`npx playwright test tests/e2e/business-actions/hr-*.action.spec.ts --workers=1` → 15 passed；`npx playwright test tests/e2e/business-actions/ --workers=1` → 89 passed（含 15 新增 hr 测试 + 74 既有测试无回归）
  - 文档对齐：e2e-runbook 业务动作表新增 4 hr 行 + 套件计数更新（38 spec 14 域 / 284 测试）
  - Deferred 核实：markPaid/generateBankFile happy-path（salary xwf 审批轴）+ 薪酬过账凭证行数值断言 + 排班深度编排 + 人才发展域 + 合同到期 Job 均在 Deferred But Adjudicated 中有明确裁决，无范围内项目遗漏

Follow-up:

- hr 薪酬过账凭证行数值断言 successor（触发条件见 Deferred）
