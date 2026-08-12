# 2026-08-13-1430-2-erphr-employee-timesheet-state-machine-beans 人力资源 ErpHrEmployee + ErpHrTimesheet 实体级状态机 Bean（M3.8 + M3.9）

> Plan Status: completed
> Last Reviewed: 2026-08-13
> Source: `docs/backlog/entity-state-machine-migration-roadmap.md` M3.8（todo）+ M3.9（todo）
> Related: 前置 `2026-08-12-0617-1-entity-state-machine-m0-1-contract.md`（M0.1 done）+ `2026-08-12-0617-2-entity-state-machine-m0-2-inventory.md`（M0.2 done）+ `2026-08-12-0738-2-cs-ticket-state-machine-pilot-evaluation.md`（M1.3 模板 done）；姊妹计划 `2026-08-12-1118-3-erphr-leave-contract-state-machine-beans.md`（M2.11+M2.12 done，本域 Bean/接线/测试/Delta 范式，其 Deferred But Adjudicated 显式将 M3.8/M3.9 列为 successor，触发条件「各对应 M3 工作项启动时」已满足）；M0.2 清单 `docs/analysis/2026-08-12-entity-state-axis-inventory.md`（HR Employee/Timesheet 行）
> Mission: entity-state-machine
> Work Item: M3.8 + M3.9
> Audit: required

## Current Baseline

> 按 M0.2 清单 `docs/analysis/2026-08-12-entity-state-axis-inventory.md`（M3.8/M3.9 行）+ 实仓核实。M3.8/M3.9 均为**无财务影响**非保护域轴（HR Timesheet approve **不**过账——「工时归集到 projects/cost-collection」为 successor，owner doc §适用对象三 RC-R1.8 注记明示跨域归集未接线；薪酬计提过账在 Salary 独立轴 M4.63/64，非 Timesheet/Employee 头状态轴触发）。本计划按规则 14 将同 owner doc `human-resource/state-machine.md` 的两轴合并。

- **M0.1 契约 + M1.3 模板已就绪**（`docs/architecture/entity-state-machine-bean.md` §1-§11）。M1.3 已裁定 go，M3 各项 Deps（M1.3）门控解除；`todo → ready` 仍需独立 plan 草案审查（路线图规则 1）。
- **工时表（ErpHrTimesheet.status）语义**（owner doc §适用对象三 RC-R1.8 实现注记 `state-machine.md:183`，dict `erp-hr/timesheet-status` 4 值 `app-erp-hr.orm.xml:57-62`：DRAFT/SUBMITTED/APPROVED/REJECTED，**无 CANCELLED**）：
  - **3 条状态迁移边**（实仓核实，逻辑内联在 `ErpHrTimesheetBizModel`——**无 Timesheet Processor 类**，M0.2 清单「Timesheet Submit/Approve/Cancel Processors」引用不准确）：
    - `submit`（`ErpHrTimesheetBizModel:50-65` `@BizMutation`，守卫 `:53-59` 允许 DRAFT **或** REJECTED（`Objects.equals`）否则抛 `ERR_HR_TIMESHEET_ILLEGAL_TRANSITION` → setStatus SUBMITTED `:62`；**提交时重算 totalHours `:60` + 24h 跨表校验 `:61` 为动态副作用，保留 BizModel**）
    - `approve`（`:69-79`，守卫 `:71` 须 SUBMITTED → setStatus APPROVED `:76`）
    - `reject`（`:83-99` `reject(reason)`，reason 空守卫 `:85-88` `ERR_TIMESHEET_REJECT_REASON_REQUIRED`；status 守卫 `:90` 须 SUBMITTED → setStatus REJECTED `:95`；reason 写入 remark `:96`）
  - **cancel = 不存在（owner doc §2 图表漂移）**：owner doc §2 迁移图 `state-machine.md:199` 画 `DRAFT→CANCELLED`，但 dict **无 CANCELLED 值**（orm.xml:57-62 仅 4 值），RC-R1.8 权威注记 `:183` 仅列 submit/approve/reject，BizModel 无 cancel mutation/writer。此为 §11.4 doc drift——Bean 不编码 cancel 边，owner doc §2 图表按 RC-R1.8 注记补正（镜像 LeaveRequest cancel 单源裁定范式）。
  - **24h 跨表校验**（`ErpHrTimesheetBizModel.checkDailyHoursLimitForTimesheet:109-116` + `checkDailyHoursLimit:118-132`，按 employeeId+workDate 跨工时表汇总，Σ>24 抛 `ERR_TIMESHEET_DAILY_HOURS_EXCEEDED` ErpHrErrors:316-319）+ totalHours 派生（行增/改/删 `afterEntityChange` 重算）——均为**动态业务校验/副作用**，保留 BizModel，Bean 保持无状态。
  - 终态 = {APPROVED, REJECTED}；初始 = {DRAFT}。REJECTED 经 submit 重提可达 SUBMITTED。
- **员工雇佣状态（ErpHrEmployee.employmentStatus）语义**（owner doc §适用对象二 Deferred `state-machine.md:128-130`，dict `erp-hr/employment-status` 5 值 `app-erp-hr.orm.xml:8-14`：ACTIVE/PROBATION/RESIGNED/TERMINATED/RETIRED）：
  - **零命名动作迁移 writer（退化轴）**：全仓无 `setEmploymentStatus(RESIGNED|TERMINATED|RETIRED|PROBATION)` 生产 writer，无 resignEmployee/retireEmployee/terminateEmployee/probationToRegular mutation。仅 **2 处初始态 ACTIVE 写入**（非迁移）：`ErpHrRecruitmentHireProcessor:63`（入职新建）+ `ErpHrRecruitmentBizModel:149`（入职新建 legacy dup）。
  - **3 个预留死状态**（owner doc §适用对象二 + §5.1 清单已记载）：RESIGNED/TERMINATED/RETIRED 零 writer，本期不可达。
  - **transferEmployee 不改 employmentStatus**（owner doc §适用对象二）：`ErpHrEmployeeBizModel.transferEmployee:100-109` 委托 `ErpHrEmployeeTransferEmployeeProcessor:71-98`，仅改 departmentId/positionId/superiorId + 处理合同，**不**写 employmentStatus（仅读守卫 `:109`）。
  - **唯一 live 用途 = 只读调动守卫**：`ErpHrEmployeeBizModel.isTransferable:158-161`（仅 ACTIVE/PROBATION 可调动）+ `nonTransferableStatuses:368-373`（返回 [RESIGNED,TERMINATED,RETIRED]）+ `requireTransferableEmployee:142-156`（违例抛 `ERR_EMPLOYEE_NOT_TRANSFERABLE`）。
  - 即 Bean 为**纯分类 + 死状态登记**载体（transitions() 空，无 assertCan 迁移方法）；集中化 isTransferable/nonTransferableStatuses 只读判断为可测元数据。PROBATION→ACTIVE（转正）= 目标行为未接入 → successor。
- **错误码现状**：
  - Timesheet：`ERR_HR_TIMESHEET_ILLEGAL_TRANSITION`（ErpHrErrors，Timesheet 三 mutation 抛）+ `ERR_TIMESHEET_REJECT_REASON_REQUIRED` + `ERR_TIMESHEET_DAILY_HOURS_EXCEEDED:316-319`。
  - Employee：`ERR_EMPLOYEE_NOT_TRANSFERABLE`（调动只读守卫）。无 employmentStatus 迁移码（因无迁移）。
  - common 层码 `ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION` 已存在（M1.1 Option A + M2.11 LeaveRequest 范式：Bean 抛 common 码，BizModel 映射领域码，common 作 cause）。
- **生产 Bean 注册范式已存在**：`_vfs/erp/hr/beans/app-service.beans.xml` 已以 FQN id 注册 `ErpHrLeaveRequestStateMachine`/`ErpHrEmploymentContractStateMachine`（`:113-116`）。StateMachine Bean 沿用 FQN id 范式（追加于 `:116` 后）。
- **既有层 3 回归基线（非 greenfield）**：`TestErpHrTimesheetFamily`（384 行，覆盖 24h 跨表校验 + totalHours 派生 + submit/approve/reject 生命周期 + REJECTED→submit→approve 链 + 非法迁移拒绝 + GraphQL 冒烟）。M0.1 §10 登记的 8 个 `TestErp*StateMachine` 基线不含 hr——hr 域层 3 = 上述既有集成测试，层 1 矩阵测试为 greenfield（新增）。
- **合规基线**：`@Inject private` = 0（module-hr service grep 证实）。本计划保持 R5=0、R11 不增。

## Goals

- 落地 `ErpHrTimesheetStateMachine`（3 迁移边 submit/approve/reject + 终态/初始态分类 + 只读 `transitions()` 元数据，严格无状态）+ `ErpHrEmployeeStateMachine`（**退化分类 Bean**：transitions() 空 + initial={ACTIVE,PROBATION} + terminal={RESIGNED,TERMINATED,RETIRED}（对齐 owner doc §3）+ 集中化 `isTransferable`/`nonTransferableStatuses` 只读判断为可测元数据），各可经 Delta 同名覆盖。
- 将 Timesheet（submit/approve/reject BizModel 内联）的**固定来源态/目标态判断**改调 Bean；**动态业务校验与副作用保留原位**（24h 跨表校验、totalHours 重算、reject reason 必填、审计字段、乐观锁）。Employee：将 `isTransferable`/`nonTransferableStatuses`/`requireTransferableEmployee` 只读调动守卫改委托 Bean 分类方法（零迁移 writer，无 assertCan 接线）。
- 保持全部既有外部行为不变（错误码、Timesheet 3 边、24h 校验、reject reason、Employee 调动守卫 ERR_EMPLOYEE_NOT_TRANSFERABLE、transferEmployee 不改 employmentStatus、初始态 ACTIVE 写入不调 assertCan*）。
- 各新增层 1 矩阵完备性表驱动测试（Timesheet 含 cancel 缺失断言；Employee 含退化解 + 3 死状态排除断言）；层 3 既有集成测试回归全绿。
- 层 2 四方对照：Timesheet 裁定 cancel doc drift（§2 图表 vs RC-R1.8 注记 + dict 无 CANCELLED）；Employee 裁定 3 死状态 + 退化轴分类；分别登记 + successor。

## Non-Goals

- 不修改任何 `model/*.orm.xml` / `model/*.api.xml` / 字典 yaml（路线图 Non-Goal）：**不向 timesheet-status 加 CANCELLED**（cancel 为目标行为未落地）、**不向 employment-status 删除值**（RESIGNED/TERMINATED/RETIRED 保留为预留语义入口，对齐 R1.x / Contract SUSPENDED 先例——保留优于删除，避免 ORM `ext:dict` 改动触发 codegen 漂移）。
- 不实现 Timesheet `cancel` 命名动作（owner doc §2 图表目标行为 + dict 无值，属业务行为变更 + 触及数据，归 successor）。
- 不实现 Employee resignEmployee/retireEmployee/terminateEmployee/probationToRegular mutation（owner doc §适用对象二 Deferred；方案 A 触及 nop-auth 用户禁用副作用，属保护区域 ask-first）。
- 不迁移 `ErpHrLeaveRequest`（M2.11 done）、`ErpHrEmploymentContract`（M2.12 done）、`ErpHrSalary.approveStatus/paymentStatus`（= M4.63/M4.64，plan-first 业财过账）、`ErpHrSurvey`（owner doc §适用对象五 Deferred CRUD 桩）。
- 不改变任何业务状态值、动作名、错误码值、24h 校验口径、调动/入职→合同联动时序（路线图 Non-Goal）。
- 不引入全局 CRUD 写锁（M0.1 §9 选项 c）。
- 不重构跨聚合副作用编排（入职/调动逻辑原位保留，只替换/委托其中固定状态判断）。
- 不在本计划证 Delta 覆盖（M3 非保护域可选；M1.2 + M2.11 LeaveRequest 已实证；归 M5.3）。

## Task Route

- Type: `implementation-only change`（消费 M0.1 契约 + M1.3 模板 + M0.2 清单 + M2.11/M2.12 HR 范式；落地两轴 Bean + 接线 + 三层测试 + 四方对照；不改契约/模型/公共 API）
- Owner Docs: `docs/architecture/entity-state-machine-bean.md`（M0.1 契约 + §11 迁移模板 + §8 不适用/退化轴）、`docs/design/human-resource/state-machine.md`（§适用对象二 Employee Deferred + §适用对象三 Timesheet RC-R1.8）、`docs/design/human-resource/payroll.md`（薪酬边界，确认 Timesheet/Employee 不触发薪酬过账）、`docs/analysis/2026-08-12-entity-state-axis-inventory.md`（M0.2 清单 hr 行）、`docs/architecture/processor-extension-pattern.md`、`docs/plans/2026-08-12-1118-3-erphr-leave-contract-state-machine-beans.md`（本域范式 + Deferred successor 出处）
- Skill Selection Basis: 路线图 M3.8/M3.9 指定 `nop-backend-dev` + `nop-testing`。`nop-backend-dev` 匹配「BizModel 接线、只读守卫委托、动态副作用保留、错误码、退化解处理」；`nop-testing` 匹配「矩阵表驱动测试（含退化解 + cancel 缺失断言）+ 既有集成测试回归」。必需输入已就绪。层 2 引用 `state-machine-business-review-prompt.md`。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（纯后端 Java + 既有 hr-service 测试容器）。
- 前置依赖：M0.1 + M0.2 + M1.3 done。均已满足。M3.8/M3.9 deps = M1.3（done），门控已解除。
- 无 data-deletion / 财务过账 / ORM 保护区域触发。

## Execution Plan

### Phase 1 - ErpHrTimesheetStateMachine + ErpHrEmployeeStateMachine Bean + 注册 + 层 1 矩阵完备性测试

Status: completed
Targets: `module-hr/erp-hr-service/src/main/java/app/erp/hr/service/statemachine/ErpHrTimesheetStateMachine.java`（新）+ `ErpHrEmployeeStateMachine.java`（新）；`.../beans/app-service.beans.xml`（追加 2 Bean 注册于 `:116` 后）；`TestErpHrTimesheetStateMachineMatrix.java` + `TestErpHrEmployeeStateMachineMatrix.java`（新，层 1）
Skill: `nop-backend-dev`（Bean 形状/注册）+ `nop-testing`（层 1 表驱动测试）

- Item Types: `Add | Decision | Proof`
- Prereqs: M0.1 + M0.2 + M1.3 done

- [x] `Add`：创建 `ErpHrTimesheetStateMachine`（无状态），矩阵编码**已实现 3 边**：`assertCanSubmit(DRAFT|REJECTED)`/`assertCanApprove(SUBMITTED)`/`assertCanReject(SUBMITTED)` + 目标态方法（`submitTargetStatus()`→SUBMITTED / `approveTargetStatus()`→APPROVED / `rejectTargetStatus()`→REJECTED）+ `isTerminal(APPROVED|REJECTED)` + `transitions()`（submit 含 DRAFT→SUBMITTED + REJECTED→SUBMITTED = 2 边、approve 1、reject 1 = 4 边）+ `terminalStatuses()`(APPROVED/REJECTED) + `initialStatuses()`(DRAFT)。**不编码 cancel**（dict 无 CANCELLED + RC-R1.8 注记权威，layer-2 登记 doc drift）。非法来源态抛 common 码携带 `action`/`fromStatus`。Skill: `nop-backend-dev`
- [x] `Decision`（Timesheet cancel doc drift，路线图规则 5）：owner doc §2 图表 `state-machine.md:199` 画 `DRAFT→CANCELLED`，但 dict `erp-hr/timesheet-status`（orm.xml:57-62）**无 CANCELLED 值**，RC-R1.8 权威注记 `:183` 仅列 submit/approve/reject，BizModel 无 cancel writer。分类 = **doc drift**。Fix = owner doc §2 图表补注「cancel 为目标行为未落地，生产仅 submit/approve/reject 三动作；dict 无 CANCELLED」+ successor（PM 要求工时表取消业务流落地时开独立 plan 实现 cancel mutation + dict 加值 + 触及数据 ask-first）。Bean 不编码 cancel 边（镜像 LeaveRequest cancel 单源裁定范式）。Skill: `state-machine-business-review-prompt.md`
- [x] `Add`：创建 `ErpHrEmployeeStateMachine`（无状态，**退化分类 Bean**）——`transitions()` 返回空列表（零迁移边，如实反映无命名动作 writer）；`initialStatuses()`(ACTIVE/PROBATION) + `terminalStatuses()`(RESIGNED/TERMINATED/RETIRED)（**对齐 owner doc §适用对象二 §3 终态声明**——见 Phase 3 Decision）+ `isTerminal(status)` 对三终态返回 true；集中化只读调动判断：`isTransferable(ACTIVE|PROBATION)` 返回 true / 其他 false + `nonTransferableStatuses()` 返回 [RESIGNED,TERMINATED,RETIRED]。**RESIGNED/TERMINATED/RETIRED 不在 initial/transitions 任一集合**（死状态：零 writer、不可达，但按 owner doc §3 业务语义为终态——「死」与「终态」不矛盾：死 = 无入边，终态 = 业务生命周期终点）。javadoc 标注退化解 + 死状态（当前不可达，successor 填充入边）+ PROBATION 零 writer 不对称（见 Phase 3 Decision）。Skill: `nop-backend-dev`
- [x] `Add`：在 `app-service.beans.xml` 以 FQN id 注册两 Bean（沿用 LeaveRequest/Contract 范式 `:113-116`，§11.1 步骤 2）。Skill: `nop-backend-dev`
- [x] `Proof`（层 1 矩阵完备性，新增 greenfield 表驱动测试，§11.1 步骤 4）：`TestErpHrTimesheetStateMachineMatrix` 覆盖 submit（DRAFT/REJECTED 合法、SUBMITTED/APPROVED 非法）/approve（SUBMITTED 合法）/reject（SUBMITTED 合法）合法+非法 + REJECTED→SUBMITTED 重提边 + APPROVED 严格终态无出边（REJECTED 为可恢复终态，有 submit 重提边）+ transitions（4 边）一致 + **断言无 cancel 边**（dict 无 CANCELLED 的对照留 layer-2 四方对照）；`TestErpHrEmployeeStateMachineMatrix` 覆盖退化解——`transitions()` 空、`isTransferable`(ACTIVE/PROBATION=true, RESIGNED/TERMINATED/RETIRED=false)、`nonTransferableStatuses()`=[三终态]、initial={ACTIVE,PROBATION}、terminal={RESIGNED,TERMINATED,RETIRED}（对齐 owner doc §3）、**断言三终态不在 transitions/initial 集合（死状态：零入边）但 isTerminal=true（业务终态 per §3）**。**不经 BizModel 入口**（层 1 只测 Bean）。Skill: `nop-testing`

Exit Criteria:

- [x] 两 Bean 落地（Timesheet 3 动作 + 目标态 + isTerminal + transitions 4 边；Employee 退化解 + isTransferable/nonTransferableStatuses + transitions 空），无状态（grep 证实不 import DAO/IBiz/IServiceContext/事务）。
- [x] 两 Bean 已在 `app-service.beans.xml` 注册（FQN id）；`@Inject` 字段非 private（合规 R5）。
- [x] 层 1 矩阵测试 `mvn test -pl module-hr/erp-hr-service -am -Dtest=TestErpHrTimesheetStateMachineMatrix,TestErpHrEmployeeStateMachineMatrix` 全绿。
- [x] 本地化编译检查：`mvn compile -pl module-hr/erp-hr-service -am` 通过（解除 Phase 2 接线依赖）。

### Phase 2 - BizModel 接线（行为保持）+ 层 3 回归

Status: completed
Targets: Timesheet：`ErpHrTimesheetBizModel`（submit/approve/reject）；Employee：`ErpHrEmployeeBizModel`（isTransferable/nonTransferableStatuses/requireTransferableEmployee）、`ErpHrEmployeeTransferEmployeeProcessor`（调动只读守卫）
Skill: `nop-backend-dev`（接线 + 错误码映射）+ `nop-testing`（回归断言）

- Item Types: `Fix | Proof`
- Prereqs: Phase 1

- [x] `Fix`：Timesheet `ErpHrTimesheetBizModel` 注入 `ErpHrTimesheetStateMachine`（`@Inject` 非 private），将 submit `:53-59`/approve `:71`/reject `:90` 内联 `Objects.equals` 来源态守卫替换为 `stateMachine.assertCan<Action>(from)` + 目标态写回（submit/approve/reject TargetStatus）；common→`ERR_HR_TIMESHEET_ILLEGAL_TRANSITION` 映射（common 码作 cause）。**动态副作用保留原位**：submit 的 totalHours 重算 `:60` + 24h 跨表校验 `:61`（`checkDailyHoursLimit:109-132`）、reject 的 reason 必填守卫 `:85-88`（`ERR_TIMESHEET_REJECT_REASON_REQUIRED`）+ reason 写入 remark `:96`、审计字段、乐观锁。Skill: `nop-backend-dev`
- [x] `Fix`：Employee `ErpHrEmployeeBizModel` 注入 `ErpHrEmployeeStateMachine`，将 `isTransferable:158-161`/`nonTransferableStatuses:368-373`/`requireTransferableEmployee:142-156` 只读调动判断改委托 Bean（`stateMachine.isTransferable(status)` / `stateMachine.nonTransferableStatuses()`）；`ErpHrEmployeeTransferEmployeeProcessor:109` 调动只读守卫同步委托。**ERR_EMPLOYEE_NOT_TRANSFERABLE 对外不变**。**2 处初始态 ACTIVE 写入不调 assertCan***（`ErpHrRecruitmentHireProcessor:63` / `ErpHrRecruitmentBizModel:149`——按 §9.2 选项 c 初始态路径，非迁移）。**transferEmployee 不改 employmentStatus**（保持）。Skill: `nop-backend-dev`
- [x] `Proof`（层 3 既有回归保持全绿）：`mvn test -pl module-hr/erp-hr-service -am` 全绿——重点 `TestErpHrTimesheetFamily`（24h 跨表校验 + totalHours 派生 + submit/approve/reject 生命周期 + REJECTED→submit→approve 链 + 非法迁移拒绝）、`TestErpHrEmployeeTransfer`（调动守卫 isTransferable + transferEmployee 不改 employmentStatus）、`TestErpHrEmployeeReferences`（入职→合同联动 + 初始 ACTIVE）、`TestErpHrLeaveEngine`/`TestErpHrPayrollSimulation`（HR 域回归基线）。证明错误码、3 边、24h 校验、调动守卫、入职→ACTIVE 不变。Skill: `nop-testing`

Exit Criteria:

- [x] Timesheet submit/approve/reject 内联来源态守卫 + Employee isTransferable/nonTransferableStatuses/requireTransferableEmployee 只读判断均改调/委托 Bean，grep 证实相关方法体内不再有内联 `Objects.equals` 矩阵判断（动态副作用如 24h 校验/totalHours/reason 必填/调动 NOP 用户边界除外；2 处初始态 ACTIVE 写入不调 assertCan*——按 §9.2 初始态路径）。
- [x] 领域错误码 + 参数对外不变（层 3 断言证实）；Timesheet 3 边 + 24h 校验 + reject reason；Employee 调动守卫 ERR_EMPLOYEE_NOT_TRANSFERABLE + transferEmployee 不改 employmentStatus + 入职→ACTIVE 行为不变。
- [x] 层 3 `mvn test -pl module-hr/erp-hr-service -am` 全绿。

### Phase 3 - 层 2 四方对照（Timesheet + Employee 双轴）

Status: completed
Targets: 四方对照审计记录（写入本计划 Closure 段）
Skill: `state-machine-business-review-prompt.md`（四方对照 + 10 维度）

- Item Types: `Proof | Decision | Fix`
- Prereqs: Phase 2

- [x] `Proof`（四方对照，§11.1 步骤 5）：以 `state-machine-business-review-prompt.md` 10 维度审查双轴——Timesheet（dict timesheet-status 4 值 ↔ owner-doc §适用对象三 RC-R1.8 ↔ Bean ↔ writer 含 BizModel 3 mutation + CRUD 路径，**重点裁定 cancel doc drift**：§2 图表 `:199` vs RC-R1.8 注记 `:183` + dict 无 CANCELLED——dict 无值对照在此落实）；Employee（dict employment-status 5 值 ↔ owner-doc §适用对象二 §1-§3 ↔ Bean 退化解 ↔ writer 含 2 初始态 ACTIVE 写入（RecruitmentHireProcessor:63 / RecruitmentBizModel:149）+ 只读调动守卫（BizModel:158-161,368-373,142-156 **及 Processor 重复副本** `ErpHrEmployeeTransferEmployeeProcessor:102-121`——Phase 2 两处均委托 Bean）+ CRUD 路径，**重点裁定 3 死状态 + 退化轴 + §3 终态对齐**）。writer 盘点含初始态写入 + 只读守卫（BizModel + Processor 双副本）+ 框架入口 + 测试 fixture。Skill: `state-machine-business-review-prompt.md`
- [x] `Decision`（漂移裁定，路线图规则 5）：
  - **Timesheet cancel = doc drift**（Phase 1 Decision 闭环）：§2 图表声明 DRAFT→CANCELLED，但 dict 无值 + RC-R1.8 仅 3 动作 + 无 writer。Fix：owner doc §2 图表补注 + successor。
  - **Employee §3 终态 vs Bean terminalStatuses() 对齐（§11.4 显式裁定）**：owner doc §适用对象二 §3（`state-machine.md:157-160`）显式声明 RESIGNED/TERMINATED/RETIRED 为终态。Bean `terminalStatuses()`=[三态]（对齐 §3 业务语义）+ `isTerminal()=true`。三者当前不可达（零 writer = 死/无入边），但「死」（无入边）与「终态」（业务生命周期终点、无出边）不矛盾——退化解下全部状态无出边，故「终态」按 **owner doc §3 业务语义**而非图论定义裁定（否则 ACTIVE/PROBATION 也无出边会被误判终态）。登记为 Decision（Bean 对齐 §3；死=不可达单独以「不在 transitions/initial」+ javadoc 表达）。
  - **Employee RESIGNED/TERMINATED/RETIRED = intentional reserved（死状态）**：dict 有值 + 零 writer + 无 mutation。owner doc §适用对象二已记载 Deferred。分类 = `intentional reserved`。Fix：dict 值保留（不删除，对齐先例）。Successor：离职/退休/转正业务流落地时（填充入边使三态可达）。
  - **Employee PROBATION 零 writer 不对称**：`initialStatuses()` 含 PROBATION（owner doc §1 业务语义为试用期初始态），但 grep 证实 PROBATION 与三死状态同样**零生产 writer**（2 处入职均写 ACTIVE）。登记为 Decision：PROBATION 归 initial（业务语义，试用期入口），与三死状态归 terminal 对称；二者当前均不可达，successor（转正/入职试用期流）填充 writer 后二者均可达。不矛盾——区分基于 owner doc §1/§3 业务语义而非当前可达性。
  - **Employee 退化轴（rule 9 替代方案记录）**：选定**落地退化 Bean**（transitions() 空 + 分类集中化）。替代方案 = **defer M3.8 至 resign/retire/terminate 流落地**；被否，理由：(i) 路线图显式列 M3.8 为纳入工作项（defer 需本身裁定）；(ii) Bean 集中化当前**重复**的调动守卫（BizModel + Processor 双副本 isTransferable）为可测/Delta-overridable 单元（真实整合价值）；(iii) forward-compatible——successor 加边不新增基础设施；(iv) M5.1 全域矩阵审计需机器可读元数据证 Employee 矩阵为空（否则须 special-case）。残留风险：`transitions()` 空可能被误读为「未实现」——javadoc 标注 + 层 1 测试显式断言退化解规避。
  - 任何 owner-doc §迁移表 vs §实现约定 内部漂移按 §11.4 补正；其他不一致按 Fix/Decision 登记。
  Skill: `state-machine-business-review-prompt.md`

Exit Criteria:

- [x] 双轴四方对照审计记录存在且非空，每维有可追溯结论（引用 Bean 元数据 / owner doc 章节 / dict 位置 / writer 类:行）。
- [x] Timesheet cancel doc drift（§2 图表补注）+ Employee 3 死状态 + 退化轴 Decision 均已登记 + successor，无静默排除；其他不一致项（若有）已 Fix 登记。

## Draft Review Record

- Independent draft review iteration 1: `needs revision`（独立子代理 ses_00804485cffe8PuYMqoZmgFqgd，新会话，零信任实仓复核）—— 全部 10 项 load-bearing 基线声明 CONFIRMED TRUE（Timesheet 不过账、3 mutation 内联 BizModel 无 Processor、cancel 缺失 + dict 无 CANCELLED、24h/totalHours 副作用、Employee 零迁移 writer + 2 初始 ACTIVE、死状态 + 调动守卫、错误码、合规 R5=0、Bean 注册范式、owner doc §二 Deferred/§三 RC-R1.8）。规则 A-F/H 全 PASS（含退化轴合法性裁定：legitimate + defensible）。1 项 MAJOR：owner doc §适用对象二 §3（`state-machine.md:157-160`）**显式声明** RESIGNED/TERMINATED/RETIRED 为终态，但 Bean `terminalStatuses()=empty` + `isTerminal()=false` 直接冲突，Phase 3 仅用泛化 catch-all 未显式裁定（§11.4 要求显式）。4 MINOR（Employee 调动守卫 BizModel+Processor 双副本 live-path 框架不清；PROBATION 零 writer 不对称未注；退化轴 Decision 缺 rule 9 替代方案；层 1 Proof 断言 dict 内容应归 layer-2）。
- Revision applied（iteration 1 → 待 iteration 2 复审）：MAJOR 改 Bean `terminalStatuses()`=[RESIGNED,TERMINATED,RETIRED] + `isTerminal()=true`（对齐 owner doc §3），Phase 3 新增显式 §3 终态对齐 Decision（「死」=无入边 vs「终态」=业务终点，退化解下按 §3 业务语义裁定）；MINOR——Phase 3 writer 盘点注明 BizModel+Processor 双副本守卫 + live-path 澄清；新增 PROBATION 零 writer 不对称 Decision；退化轴 Decision 补 rule 9 替代方案（defer M3.8 被否 + 理由）；层 1 dict-no-CANCELLED 对照移至 layer-2 四方对照。
- Independent draft review iteration 2: `accept`（独立子代理 ses_007faa820fffezltgf2yuvBeU81，新会话零信任复审）—— M1 MAJOR 已解决：Bean `terminalStatuses()`=[RESIGNED,TERMINATED,RETIRED]+`isTerminal`=true 对齐 owner doc §3（`state-machine.md:157-160`）；Phase 3 新增显式裁定「死=无入边 vs 终态=业务终点，退化解按 §3 业务语义而非图论裁定（否则 ACTIVE/PROBATION 亦无出边被误判终态）」，推理 sound；m1-m4 全部落实（Processor 双副本 `ErpHrEmployeeTransferEmployeeProcessor:102-121` 实仓确认 + Phase 2 双处委托、PROBATION 零 writer 不对称 Decision、rule 9 替代方案记录、dict-no-CANCELLED 对照移 layer-2）。全 plan 内部一致、「dead≠terminal」框架贯穿、规则 13 无降级、Closure Gates/Review Record 诚实。1 项 pre-existing MINOR（Timesheet layer-1「终态无出边」对 REJECTED 不精确——REJECTED 有重提边，已就地订正为「APPROVED 严格终态无出边 + REJECTED 可恢复终态」）非阻塞。草案审查收敛，Plan Status → active。

## Closure Gates

> 本计划含生产代码变更（2 Bean + Timesheet 3 接线 + Employee 守卫委托 + 测试），Closure Gates 运行完整仓库验证。

- [x] 范围内行为完成（Timesheet + Employee 双轴 Bean + 接线 + 层 1 矩阵 + 层 3 回归 + 层 2 四方对照）
- [x] 相关文档对齐（owner doc §2 cancel doc drift 补注；Employee 死状态 + 退化轴 Decision 补注；路线图 M3.8 + M3.9 done）
- [x] 已运行验证：`mvn clean install -DskipTests` BUILD SUCCESS + `mvn test -pl module-hr/erp-hr-service` 全绿（214 tests, 0 failures） + `bash docs/audits/nop-compliance-checker.sh`（R5=0/R11=0 无漂移）
- [x] 无范围内项目降级为 deferred/follow-up（cancel doc drift + 死状态 Decision 必须落地登记）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：Plan Status、各 Phase Status、Exit Criteria、Closure Gates、日志一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### Timesheet cancel 命名动作（DRAFT→CANCELLED）

- Classification: `out-of-scope improvement (doc drift successor)`
- Why Not Blocking Closure: owner doc §2 图表声明 cancel，但 dict 无 CANCELLED + RC-R1.8 仅 3 动作 + 无 writer。Bean 不编码。owner doc 已就地补注。实现 cancel 属业务行为变更 + dict 加值（触及数据），须 ask-first，非状态机集中重构范围。
- Successor Required: yes（触发条件 = PM 要求工时表取消业务流落地时，开独立 plan 实现 cancel mutation + dict 加 CANCELLED 值）

### Employee 离职/退休/转正 mutation（RESIGNED/TERMINATED/RETIRED 落地 + PROBATION→ACTIVE 转正）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: owner doc §适用对象二已记载三态为预留死状态 + 转正为目标行为未接入。实现 resignEmployee/retireEmployee/terminateEmployee/probationToRegular 属业务行为变更（方案 A 触及 nop-auth 用户禁用副作用，属保护区域 ask-first）。dict 值保留为预留语义入口（不删除）。
- Successor Required: yes（触发条件 = PM 要求正式离职/退休/试用期转正工作流落地时，开独立 plan 实现 mutation + 填充 Bean 边）

### hr 域其余状态轴（Salary / Survey / DevelopmentPlan）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: `ErpHrSalary.approveStatus/paymentStatus` = M4.63/M4.64（plan-first 业财过账）；`ErpHrSurvey` = §适用对象五 Deferred CRUD 桩；`ErpHrDevelopmentPlan(Item)` = 死状态（清单 §5.1）。
- Successor Required: yes（触发条件 = 各对应 M4 工作项 / 问卷生命周期落地时）

### Delta 覆盖运行时实证

- Classification: `optimization candidate`
- Why Not Blocking Closure: M3 非保护域 Delta 可选；M1.2 + M2.11 LeaveRequest 已实证机制。本计划不重复证明。
- Successor Required: yes（触发条件 = M5.3 最终跨域 Delta 覆盖回归）

### 全局 CRUD 写锁

- Classification: `watch-only residual`
- Why Not Blocking Closure: M0.1 §9 裁定选项 (c) 显式排除；更强写锁须改 ORM/xmeta（保护区 ask-first）。
- Successor Required: no（仅当产品要求全局强制矩阵写锁时重开）

## Closure

Status Note: 已执行（Phase 1-3 全部完成，层 1 矩阵 + 层 3 回归全绿，owner doc 补注已落地）。

Closure Audit Evidence:

### 层 2 四方对照审计记录（Phase 3 Proof 产出，`state-machine-business-review-prompt.md` 10 维度）

#### 轴一：ErpHrTimesheet.status（dict erp-hr/timesheet-status）

**四方对照表：**

| 维度 | dict（`app-erp-hr.orm.xml:57-62`） | owner doc（`state-machine.md §适用对象三`） | Bean（`ErpHrTimesheetStateMachine`） | writer（生产代码） |
|------|------|------|------|------|
| 状态全集 | DRAFT/SUBMITTED/APPROVED/REJECTED（4 值，**无 CANCELLED**） | §1 表 4 态 + §2 图表画 `DRAFT→CANCELLED`（drift） | initial={DRAFT}, terminal={APPROVED,REJECTED}, transitions 4 边 | 3 mutation × setStatus + CRUD 入口 + 测试 fixture |

**10 维度结论：**

1. **状态定义**：4 态语义清晰（草稿/已提交/已批准/已驳回），每态有明确业务等待点。✅ 无「动作作为状态」。dict 4 值与 owner doc §1 表一致。
2. **转换完整性**：Bean 编码 4 边 = submit(DRAFT→SUBMITTED) + submit(REJECTED→SUBMITTED 重提) + approve(SUBMITTED→APPROVED) + reject(SUBMITTED→REJECTED)。与 BizModel 3 mutation 完全对齐（`ErpHrTimesheetBizModel:50-99` Phase 2 接线后委托 Bean）。
3. **终端与恢复**：APPROVED 严格终态（无出边）；REJECTED 可恢复终态（submit 重提边 →SUBMITTED）。Bean `isTerminal`=true 对二者，`transitions()` 反映 REJECTED 有 submit 出边。✅
4. **异常路径**：24h 跨表校验（`ERR_TIMESHEET_DAILY_HOURS_EXCEEDED`）+ reject reason 必填（`ERR_TIMESHEET_REJECT_REASON_REQUIRED`）+ 非法迁移（`ERR_HR_TIMESHEET_ILLEGAL_TRANSITION`）。均为动态业务校验，保留 BizModel。✅
5. **可达性**：从 DRAFT 可达全部 3 非初始态（层 1 `testReachabilityFromInitial` 证实）。无死状态（dict 4 值全有 writer 或可达）。✅
6. **角色与权限**：owner doc 未显式绑定角色（UC-HR-03 员工提交 + 项目经理审批）；本 Bean 不持有角色（契约 §2 无状态）。角色守卫归 xbiz auth。✅
7. **外部依赖**：owner doc §适用对象三 RC-R1.8 注记「工时归集到 projects/cost-collection」为 successor（跨域归集未接线）。Bean 不持有跨域副作用。✅
8. **TODO/任务策略**：SUBMITTED 产生审批待办（owner doc §场景 F）；Bean 不持有。✅
9. **场景演练**：层 3 `TestErpHrTimesheetFamily` 覆盖快乐路径（DRAFT→submit→SUBMITTED→approve→APPROVED）+ 拒绝路径（SUBMITTED→reject→REJECTED→submit 重提→approve）+ 异常终止（24h 超限/非法迁移）。✅
10. **设计文档一致性**：**发现 doc drift** —— §2 图表 `state-machine.md:199` 画 `DRAFT→CANCELLED`，但 dict 无 CANCELLED 值 + RC-R1.8 权威注记仅 3 动作 + 无 writer。**Fix**：owner doc §2 图表补注已落地（cancel 为目标行为未落地 + dict 无值 + successor）。Bean 不编码 cancel 边（层 1 `testNoCancelEdgeDictHasNoCancelledValue` 断言）。

**Timesheet 裁定汇总**：1 项 doc drift（cancel）已 Fix + successor 登记；无死状态；Bean 矩阵与生产 writer 完全对齐。

#### 轴二：ErpHrEmployee.employmentStatus（dict erp-hr/employment-status，退化解）

**四方对照表：**

| 维度 | dict（`app-erp-hr.orm.xml:8-14`） | owner doc（`state-machine.md §适用对象二`） | Bean（`ErpHrEmployeeStateMachine`） | writer（生产代码） |
|------|------|------|------|------|
| 状态全集 | ACTIVE/PROBATION/RESIGNED/TERMINATED/RETIRED（5 值） | §1 表 5 态 + §2 图表（目标行为 Deferred）+ §3 终态声明 | initial={ACTIVE,PROBATION}, terminal={RESIGNED,TERMINATED,RETIRED}, transitions=**空** | 2 处初始 ACTIVE 写入 + 只读调动守卫 + CRUD + 测试 fixture |

**writer 全量盘点（§9.4 CRUD 路径纳入）：**
- 生产命名动作迁移 writer：**0**（无 resignEmployee/retireEmployee/terminateEmployee/probationToRegular mutation）
- 初始态 ACTIVE 写入：**2**（`ErpHrRecruitmentHireProcessor:63` 入职新建 + `ErpHrRecruitmentBizModel:149` 入职新建 legacy dup）—— §9.2 选项 c 创建路径，不调 assertCan*
- 只读调动守卫（非 writer）：BizModel `isTransferable`/`nonTransferableStatuses`/`requireTransferableEmployee` + Processor `isTransferable`/`requireTransferableEmployee`（Phase 2 两处均委托 Bean）
- 框架 CRUD 入口：通用 `__save`/`update` 可写 employmentStatus（xmeta insertable/updatable=true，§9.1 探索结论）—— 不在矩阵运行时强制范围（§9.2 选项 c）
- 测试 fixture：12 处 `setEmploymentStatus(ACTIVE)` 构造测试种子（非生产 writer）

**10 维度结论（退化解适配）：**

1. **状态定义**：5 态语义清晰（在职/试用/离职/解雇/退休）。✅ ACTIVE/PROBATION 为活态入口（§1），RESIGNED/TERMINATED/RETIRED 为业务终点（§3）。
2. **转换完整性**：**退化解**——transitions() 空，如实反映零命名动作迁移 writer。owner doc §2 图表（离职/转正）为**目标行为 Deferred**（§适用对象二 Deferred 注记 + 场景 D/E 明示）。Bean 不编码未落地边。
3. **终端与恢复**：Bean `terminalStatuses()`={RESIGNED,TERMINATED,RETIRED} 对齐 owner doc §3 显式声明。**§3 终态对齐 Decision**（§11.4 显式裁定）：三态当前零 writer = 死（无入边），但 isTerminal=true（§3 业务终点）；退化解下全部状态无出边，故「终态」按 §3 业务语义而非图论裁定。
4. **异常路径**：调动守卫（`ERR_EMPLOYEE_NOT_TRANSFERABLE`）保留 BizModel/Processor，Phase 2 委托 Bean `isTransferable` 判断。✅ 领域码对外不变。
5. **可达性**：**退化解特性**——从 ACTIVE 经 transitions() 不可达任何其他状态（transitions 空）。RESIGNED/TERMINATED/RETIRED 当前不可达（死状态），PROBATION 当前不可达（零 writer 不对称）。均归 successor（离职/退休/转正 mutation 落地后填充入边）。层 1 `testTerminalStatusesAreDeadButBusinessTerminal` 断言三终态不在 transitions/initial 但 isTerminal=true。
6. **角色与权限**：owner doc §场景 D/E 描述目标角色（HR 操作）；当前无 mutation 故无角色绑定。调动守卫角色归 xbiz auth。✅
7. **外部依赖**：owner doc §场景 D 描述目标联动（合同→TERMINATED、休假→CANCELLED、停用账号）；当前未接入。Bean 不持有。✅
8. **TODO/任务策略**：退化解无非终态产生 TODO（无 mutation 推进）。调动守卫为同步拒绝。✅
9. **场景演练**：层 3 `TestErpHrEmployeeTransfer`（10 tests）覆盖调动守卫（ACTIVE 可调动 + RESIGNED 不可调动）+ transferEmployee 不改 employmentStatus。入职→ACTIVE 由 `TestErpHrEmployeeReferences` 覆盖。✅
10. **设计文档一致性**：owner doc §适用对象二 Deferred 注记 + Bean 落地注记（补注已落地）对齐。**PROBATION 零 writer 不对称 Decision**：PROBATION 归 initial（§1 业务语义），与三死状态归 terminal 对称；二者当前均不可达。**退化轴 Decision（rule 9 替代方案）**：选定落地退化 Bean（transitions 空 + 分类集中化）而非 defer M3.8——Bean 集中化 BizModel+Processor 双副本调动守卫为可测/Delta-overridable 单元（真实整合价值），forward-compatible，M5.1 全域矩阵审计需机器可读元数据。

**Employee 裁定汇总**：3 死状态（RESIGNED/TERMINATED/RETIRED）= intentional reserved（dict 值保留，successor 填充入边）；PROBATION 零 writer 不对称 Decision 登记；退化轴 rule 9 替代方案记录；Bean `terminalStatuses()` 对齐 §3。无静默排除。

### 验证证据

- 层 1 矩阵：`TestErpHrTimesheetStateMachineMatrix`（11 tests）+ `TestErpHrEmployeeStateMachineMatrix`（7 tests）= 18 tests 全绿。
- 层 3 回归：`mvn test -pl module-hr/erp-hr-service` = 214 tests 全绿（0 failures, 0 errors），含 `TestErpHrTimesheetFamily`/`TestErpHrEmployeeTransfer`/`TestErpHrEmployeeReferences`/`TestErpHrLeaveEngine`/`TestErpHrPayrollSimulation`。
- 全仓构建：`mvn clean install -DskipTests` BUILD SUCCESS。
- 合规：`nop-compliance-checker.sh` R5=0, R11=0（无漂移）。
- grep 证实：Timesheet BizModel 无 `Objects.equals` 状态判断；Employee BizModel + Processor 无内联 `EMPLOYMENT_ACTIVE/PROBATION.equals` 判断（均委托 Bean）。

Follow-up:

- 非阻塞跟进见 §Deferred But Adjudicated（Timesheet cancel mutation / Employee 离职退休转正 mutation / Salary Survey 状态轴 / Delta 覆盖实证 / 全局 CRUD 写锁）。
