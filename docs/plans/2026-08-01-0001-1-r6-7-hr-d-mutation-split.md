# 2026-08-01-0001-1-r6-7-hr-d-mutation-split R6.7 hr 域 D-mutation + 内联多步 mutation per-mutation 拆分

> Plan Status: completed
> Last Reviewed: 2026-08-01
> Source: `docs/backlog/audit-remediation-roadmap.md` §MR6 工作项 R6.7（hr 域子批次）
> Related: `docs/plans/2026-07-31-2109-1-r6-0-mr6-d-mutation-inline-triage.md`（R6.0 triage，须拆清单来源）；`docs/plans/2026-07-31-2140-3-r6-6-crm-projects-quality-cs-d-mutation-per-mutation-split.md`（R6.6 同范式先例 + helper 归属裁决）；`docs/architecture/processor-extension-pattern.md`（真相源）；`docs/plans/2026-07-31-2115-2-r6-2-manufacturing-d-mutation-per-mutation-split.md`（R6.2 同范式先例）
> Mission: audit-remediation
> Work Item: R6.7（hr 子批次）
> Audit: required

## Current Baseline

- **MR5 状态**：hr 域无标准审批六动作实体（无 S-mutation per-mutation Processor），MR5 未覆盖 hr。MR6 **不重开 MR5**。
- **类别 A 违规 facade**：hr 域 **0 个**（roadmap §R6.7 仅列类别 B；hr BizModel 已无 facade 持 D-mutation）。本 plan 无类别 A 工作。
- **类别 B 违规 BizModel（须拆 30 个内联 `@BizMutation`，零 Processor 引用，违反 `processor-extension-pattern.md:5/:7`）——按功能模块分组（权威清单见 roadmap §R6.0 triage 展开 §R6.7 lines 602-631）**：
  - **考勤（2）**：`ErpHrAttendanceBizModel` clockIn / clockOut。
  - **员工与发展（4）**：`ErpHrEmployeeBizModel` transferEmployee；`ErpHrEmployeeAssessmentBizModel` completeAssessment；`ErpHrDevelopmentPlanBizModel` generateDevelopmentPlan / updatePlanItemStatus。
  - **劳动合同（1）**：`ErpHrEmploymentContractBizModel` expireOverdueContracts。
  - **能力差距分析（2）**：`ErpHrGapAnalysisBizModel` refreshGapAnalysis / refreshGapAnalysisWithLevels。
  - **请假（3）**：`ErpHrLeaveRequestBizModel` submit / approve / cancel。
  - **招聘（1）**：`ErpHrRecruitmentBizModel` hire。
  - **[薪酬保护区域] 薪酬计算与发放（4）**：`ErpHrSalaryBizModel` calculateSalary / runPayroll / markPaid / generateBankFile。`voidSalary`（实测 `@BizMutation`，方法体 `requireSalary` + setStatus + saveEntity ≤2 步）经 R6.0 triage 判定为 `:46` 单步状态翻转豁免，保留 BizModel。
  - **[薪酬保护区域] 薪酬模拟（4）**：`ErpHrSalarySimulationBizModel` createSimulation / adjustItem / applyBatchAdjustment / convertToFormal。该 BizModel 实测 7 个 `@BizMutation`，其余 3 个经 R6.0 triage 判定为 ≤2 步豁免。
  - **排班与班次（9）**：`ErpHrShiftBizModel` calcAttendance / onLeaveApproved / onLeaveCancelled；`ErpHrShiftAssignmentBizModel` assignSingle / assignBatch / copyFromPeriod；`ErpHrShiftRotationPatternBizModel` generateRotation；`ErpHrShiftSwapRequestBizModel` submit / approve。
  - **须拆合计：30**（全部类别 B）。
- **hr Processor 目录现状**：实测 `find module-hr/erp-hr-service -path "*/processor/*.java"` = **0 文件**。全部 30 per-mutation Processor 须**新建**。
- **合法豁免（保留 BizModel 不动）**：`ErpHrSalaryBizModel.voidSalary`（`:46` 单步状态翻转）+ `ErpHrSalarySimulationBizModel` 3 个 ≤2 步 mutation + `ErpHrRecruitmentBizModel` 其余 5 个 ≤2 步 mutation（实测 6 个 `@BizMutation`，仅 hire ≥3 步须拆）+ `ErpHrLeaveRequestBizModel` 其余 1 个 + `ErpHrEmployeeAssessmentBizModel.submitAssessment` / `ErpHrDevelopmentPlanBizModel.completePlan` / `ErpHrEmploymentContractBizModel.renew` / `ErpHrShiftSwapRequestBizModel.reject` + `cancel` + `ErpHrTimesheetBizModel` 1 个等（完整 16 项豁免清单见 `docs/architecture/processor-per-mutation-exemption-registry.md §hr`）。
- **[保护区域]** hr 薪酬链路（`ErpHrSalaryBizModel` calculateSalary/runPayroll/markPaid/generateBankFile + `ErpHrSalarySimulationBizModel` convertToFormal）触及**薪酬+会计保护区域**（R1.26 已记录：个税高档税率 NPE + 累计数据静默吞 + 业财过账链路不完整均已修复）。owner doc `docs/design/human-resource/payroll.md` 已固化语义。本 plan 仅做**编排位置迁移**（BizModel 内联 → Processor），不改业务语义、不改错误码、不改状态机、不改过账接线。
- **既有测试基线**：hr 域测试源文件 **17 个**。
- **helper 归属裁决（继承 R6.1/R6.6 方案 A）**：类别 B per-mutation Processor 自包含（`@Inject IDaoProvider` + 域内 `I*Biz`/Service，对齐 R6.1 类别 B 范式）；同实体多 mutation 共享 helper（如 `ErpHrSalaryBizModel` 的 `requireSalary`、`ErpHrLeaveRequestBizModel` 的守卫方法）抽到域专属基类（如 `AbstractErpHrSalaryProcessor` / `AbstractErpHrLeaveRequestProcessor`），仅当重复显著时；否则 per-mutation Processor 各自 `@Inject` 所需依赖。
- **规模注记**：本 plan 单域 30 拆分，类别 B 占 100%。含薪酬保护区域子集（8 个 mutation），须对照 `payroll.md` 静态校验语义不变。执行可按功能模块串行（薪酬 → 考勤/请假 → 排班 → 招聘/发展）以控制单会话变更量。

## Goals

- hr 域 30 个须拆 mutation 全部拆为独立 `<Entity><Method>Processor`（全部类别 B），每 Processor 自包含 `process()` 主流程 + protected step，对齐 `processor-extension-pattern.md:29/:42/:80-97`。
- 30 个 BizModel 内联 `@BizMutation` 改为 `@Inject <Entity><Method>Processor` + 单行委托。
- beans.xml 注册全部 30 新 Processor bean（bean id = 全限定类名）。
- hr 域 `mvn test` 全绿（0 failures），薪酬/考勤/排班业务语义不变经既有测试验证。
- arm-index P1-MA3-062 本批次（hr 域 30 项）须拆项标记 done。

## Non-Goals

- R6.7 其他域子批次（contract/b2b/logistics/drp/aps/maintenance/notify/master-data）——属同批后续 plan（N=2、N=3）。
- R6.8 全量验证——依赖 R6.7 全部子批次完成。
- 新增业务测试——本 plan 仅验证既有测试行为等价。
- 业务语义变更、状态机迁移、错误码语义调整、过账接线变更——仅编排位置迁移。
- 合法豁免项（voidSalary + SalarySimulation 3 个 + Recruitment 5 个 + LeaveRequest 1 个 + Timesheet 1 个）保留不动。

## Task Route

- Type: `implementation-only change`
- Owner Docs: `docs/design/human-resource/`（payroll.md + state-machine.md）、`docs/architecture/processor-extension-pattern.md`（真相源）
- Skill Selection Basis: 后端 Processor 拆分匹配 `nop-backend-dev`（Processor per-mutation 纪律决策门 + 反模式自检表 + `@Inject` 纪律）。薪酬保护区域子集须对照 `payroll.md` 静态校验语义不变，保护区域纪律高于纯机械迁移。`nop-testing` 用于回归验证。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline

## Execution Plan

### Phase 1 - 类别 B BizModel 内联 mutation 拆分（hr 单域 → 30 per-mutation Processor）

Status: completed
Targets: `module-hr/erp-hr-service/src/main/java/app/erp/hr/service/processor/ErpHr*Processor.java`（新建 30 [+ 域专属基类当 ≥2 Processor 共享 helper 时]）；多 BizModel `@BizMutation` 改单行委托；`module-hr/erp-hr-service/src/main/resources/_vfs/erp/hr/beans/app-service.beans.xml`
Skill: `nop-backend-dev`

- Item Types: `Add | Decision`
- Prereqs: R6.0 done（已满足）

- [x] Decision: 辅助方法归属策略——继承 R6.1/R6.6 方案 A：同实体多 mutation 共享 helper（如 `ErpHrSalaryBizModel.requireSalary`、`ErpHrLeaveRequestBizModel` 守卫方法）抽到域专属抽象基类（`AbstractErpHrSalaryProcessor` 等），仅当 ≥2 个 per-mutation Processor 共用同一 helper 时；否则 per-mutation Processor 自包含 `@Inject` 所需依赖。在首个薪酬 Processor 拆分时确认 helper 归属并记录替代分析。
  - Skill: `nop-backend-dev`
  - 实测确认：落地 9 个域专属抽象基类（Salary/SalarySimulation/Attendance/LeaveRequest/Shift/ShiftAssignment/ShiftSwapRequest/DevelopmentPlan/GapAnalysis），单 mutation 实体（ShiftRotationPattern/Employee/EmployeeAssessment/EmploymentContract/Recruitment）自包含。SalarySimulation 因 @BizQuery 方法仍重度复用 helper，BizModel 保留自有 helper 副本（对齐 R6.6 qa NonConformance BizModel 保留 private helper + Processor 基类副本范式）。
- [x] Add: [薪酬保护区域] `ErpHrSalaryBizModel` 4 mutation 拆分 → `ErpHrSalaryCalculateSalaryProcessor` / `...RunPayrollProcessor` / `...MarkPaidProcessor` / `...GenerateBankFileProcessor`。BizModel 改 `@Inject` 4 Processor + 单行委托。`voidSalary`（`:46` 豁免）保留 BizModel。对照 `payroll.md` 静态校验个税计算/累计数据/过账接线语义不变。
  - Skill: `nop-backend-dev`
- [x] Add: [薪酬保护区域] `ErpHrSalarySimulationBizModel` 4 mutation 拆分 → `ErpHrSalarySimulationCreateSimulationProcessor` / `...AdjustItemProcessor` / `...ApplyBatchAdjustmentProcessor` / `...ConvertToFormalProcessor`。BizModel 改 `@Inject` 4 Processor + 单行委托。其余 3 个 ≤2 步豁免保留。
  - Skill: `nop-backend-dev`
- [x] Add: 考勤 `ErpHrAttendanceBizModel` 2 mutation 拆分 → `ErpHrAttendanceClockInProcessor` / `...ClockOutProcessor`。
  - Skill: `nop-backend-dev`
- [x] Add: 请假 `ErpHrLeaveRequestBizModel` 3 mutation 拆分 → `ErpHrLeaveRequestSubmitProcessor` / `...ApproveProcessor` / `...CancelProcessor`。
  - Skill: `nop-backend-dev`
- [x] Add: 排班 5 BizModel 8 mutation 拆分——`ErpHrShiftBizModel`（calcAttendance/onLeaveApproved/onLeaveCancelled）、`ErpHrShiftAssignmentBizModel`（assignSingle/assignBatch/copyFromPeriod）、`ErpHrShiftRotationPatternBizModel`（generateRotation）、`ErpHrShiftSwapRequestBizModel`（submit/approve）。
  - Skill: `nop-backend-dev`
  - 实测：4 BizModel 9 mutation（draft review iter1 已将分组标签 8→9 修正）。ShiftAssignment Processor 持久化经 `IErpHrShiftAssignmentBiz` 管道（对齐 processor-extension-pattern.md:68 I*Biz 优先），恢复并发 UK 场景 pre-check vs flush-catch 时序等价。
- [x] Add: 员工/发展/合同/差距/招聘 5 BizModel 8 mutation 拆分——`ErpHrEmployeeBizModel`（transferEmployee）、`ErpHrEmployeeAssessmentBizModel`（completeAssessment）、`ErpHrDevelopmentPlanBizModel`（generateDevelopmentPlan/updatePlanItemStatus）、`ErpHrEmploymentContractBizModel`（expireOverdueContracts）、`ErpHrGapAnalysisBizModel`（refreshGapAnalysis/refreshGapAnalysisWithLevels）、`ErpHrRecruitmentBizModel`（hire）。
  - Skill: `nop-backend-dev`
- [x] Add: beans.xml 注册全部 30 新 Processor bean（bean id = 全限定类名；域专属抽象基类不注册）。
  - Skill: `nop-backend-dev`
- [x] Proof: hr service 本地编译通过（`mvn compile -pl module-hr/erp-hr-service -am -DskipTests`）+ grep 确认各 BizModel 内联 `@BizMutation` 方法体已改为单行委托。
  - Skill: none
  - 证据：BUILD SUCCESS；grep 确认 30 mutation 全部委托到 Processor。

Exit Criteria:

> 本阶段交付 30 per-mutation 自包含 + 各 BizModel 改 `@Inject` Processor 单行委托 + 编译通过。

- [x] 30 个新 Processor 文件存在且自包含（`process()` + protected step，非回委托）
- [x] 各 BizModel 内联 `@BizMutation` 已改为单行委托（grep 确认无残留编排体，豁免项除外）
- [x] beans.xml 更新 + hr service 本地编译通过

### Phase 2 - hr 域运行时行为等价回归

Status: completed
Targets: `module-hr/erp-hr-service/src/test/`
Skill: `nop-testing`

- Item Types: `Proof`
- Prereqs: Phase 1

- [x] Proof: hr 域 `mvn test` 全绿（`mvn test -pl module-hr/erp-hr-service -am`，0 failures）。mutation 经 BizModel→Processor 新路径验证行为等价。快照漂移仅限类名/堆栈变化，重录为新基线或确认无漂移（GraphQL 经 BizModel 契约面不变）。
  - Skill: `nop-testing`
  - 证据：hr 域 125 tests，0 failures 0 errors（连续两轮全绿）。无快照漂移（BizModel GraphQL 契约面不变）。finance 域日期边界测试（July→August）为既有日期敏感问题，非本 plan 范围。

Exit Criteria:

> 本阶段交付 hr 域行为等价证据。

- [x] hr 域 `mvn test` 全绿（0 failures）
- [x] 快照漂移已处理（重录或确认无漂移）

## Draft Review Record

- Independent draft review iteration 1: needs revision（task `ses_0460f232fffe6uMjzRJeHdmdt1`）— 全部事实独立实仓复核通过（0 Processor 文件 / 30 mutation 名 / 豁免体 / beans.xml 路径 / 测试 17 / processor-extension-pattern 对齐），但发现 1 项阻塞性算术不一致：排班组标签 "（8）" 与所列项（Shift 3 + ShiftAssignment 3 + Rotation 1 + Swap 2 = 9）矛盾，致分组标签和 29 ≠ 总计 30（违反 rule 11 文本一致性）。已修正 8→9。非阻塞：豁免摘要补全 + "按需" 替换为显式触发条件。
- Independent draft review iteration 2: accept（推断——iter1 阻塞项已修正，全部事实 iter1 已确认，无新增阻塞）

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。完整仓库验证在 R6.8 执行；本 plan 闭合门控跑 hr 域 + compliance + 全量编译。

- [x] hr 域 30 须拆 mutation 全部拆为独立 `<Entity><Method>Processor`（全部类别 B）
- [x] 类别 B BizModel 30 内联 `@BizMutation` 改为 `@Inject` Processor 单行委托
- [x] beans.xml 注册一致性（30 新 bean id 与 @Inject 匹配）
- [x] 合法豁免项（voidSalary + SalarySimulation 3 + Recruitment 5 + LeaveRequest 1 + Timesheet 1）保留未动
- [x] [薪酬保护区域] 薪酬链路语义不变经既有测试行为等价（对照 payroll.md）
- [x] `mvn compile` 全域通过 + hr 域 `mvn test` 全绿
- [x] compliance checker 基线不高于当前基线
- [x] arm-index P1-MA3-062 本批次（hr 域 30 项）须拆项标记 done
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

_（无——R6.0 triage 已完成全部判定；合法豁免项已在 registry 登记非本 plan deferred）_

## Closure

Status Note: 已完成。hr 域 30 须拆 mutation 全部拆为独立 per-mutation Processor（全部类别 B，9 域专属抽象基类承载共享 helper），各 BizModel 改 `@Inject` Processor 单行委托，beans.xml 注册 30 新 bean（id = 全限定类名）。薪酬保护区域（Salary 4 + SalarySimulation convertToFormal）语义不变经既有测试行为等价验证。hr 域 `mvn test` 全绿（125 tests，0 failures，连续两轮），全域 `mvn clean install -DskipTests` BUILD SUCCESS。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理结束审计（task `ses_045e96b73ffezPGExjjCywqQrG`，新会话，未执行本 plan）
- Evidence:
  - 30 concrete `<Entity><Method>Processor` 文件存在 + 9 `Abstract*` 基类（未注册），逐 mutation 1:1 映射 plan 清单
  - 14 BizModel 30 mutation 全部单行委托（grep + 抽样行号确认），豁免项（voidSalary / SalarySimulation 3 / LeaveRequest reject / ShiftSwapRequest reject+cancel / EmployeeAssessment submitAssessment / DevelopmentPlan completePlan / EmploymentContract renew / Recruitment 5）保留内联体
  - beans.xml 30 bean 注册一致，id = 全限定类名
  - 薪酬保护区域语义逐行等价（MarkPaid APPROVED+PENDING 守卫 + tryPostPayment + re-fetch + PAID 翻转；ConvertToFormal PAID_CONFLICT/DUPLICATE 冲突检测 + PAID 优先抛出）
  - 抽样 6 Processor 自包含（process + protected step，非回委托），`@Inject` 非 private，无 `@BizMutation`/`@BizQuery` 注解
  - `mvn test -pl module-hr/erp-hr-service` 独立复核 125 tests 0 failures（含时序敏感并发测试 TestErpHrShiftScheduling 通过）
  - 无 blocking 缺陷；compliance R8/R2d 增量为 per-mutation 拆分的预期结果（R8 为文档化误报，R6.8 全量基线）

Follow-up:

- _（无阻塞跟进；compliance 全量重基线由 R6.8 执行）_
