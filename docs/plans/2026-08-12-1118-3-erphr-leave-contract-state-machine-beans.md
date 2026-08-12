# 2026-08-12-1118-3-erphr-leave-contract-state-machine-beans 人力资源 ErpHrLeaveRequest + ErpHrEmploymentContract 实体级状态机 Bean（M2.11 + M2.12）

> Plan Status: active
> Last Reviewed: 2026-08-12
> Source: `docs/backlog/entity-state-machine-migration-roadmap.md` M2.11（todo）+ M2.12（todo）
> Related: 前置 `2026-08-12-0617-1-entity-state-machine-m0-1-contract.md`（M0.1 done）+ `2026-08-12-0617-2-entity-state-machine-m0-2-inventory.md`（M0.2 done）+ `2026-08-12-0738-1-cs-ticket-state-machine-bean-pilot.md`（M1.1 范式）+ `2026-08-12-0738-2-cs-ticket-state-machine-pilot-evaluation.md`（M1.3 模板 done）
> Mission: entity-state-machine
> Work Item: M2.11 + M2.12
> Audit: required

## Current Baseline

- **M0.1 契约 + M1.3 模板已就绪**（`docs/architecture/entity-state-machine-bean.md` §1-§11）。M1.3 已裁定 **go**，M2 各项 Deps（M1.3）门控解除；`todo → ready` 仍需独立 plan 草案审查（路线图规则 1）。
- **本计划按规则 14 将同组件（同 owner doc `human-resource/state-machine.md`、同结果表面「HR 域生命周期 StateMachine Bean」、同验证路径）的两条状态轴合并为一个计划的两个阶段**：LeaveRequest（§适用对象一）+ EmploymentContract（§适用对象五）。二者均 M2 简单生命周期（§11.2），非保护域、无审批子矩阵、无跨域过账副作用（薪酬发放/计提过账是 Salary 独立轴 M4.63/64，非 LeaveRequest/Contract 头状态轴触发）。
- **休假申请（ErpHrLeaveRequest.status）语义**（owner doc §适用对象一）：5 态 DRAFT/SUBMITTED/APPROVED/REJECTED/CANCELLED；终态 = APPROVED/REJECTED/CANCELLED。**cancel 实况（关键漂移发现）**：owner doc §2 迁移图 + §6 权限表声明 cancel 多源（DRAFT/SUBMITTED→CANCELLED，员工本人未审批前自撤），但**生产代码实际为单源 `APPROVED→CANCELLED`**（`ErpHrLeaveRequestCancelProcessor:21` `requireStatus(leave, APPROVED, CANCELLED)` + Javadoc「APPROVED→CANCELLED 取消编排」）——即已批准休假由 HR/员工取消，**无** DRAFT/SUBMITTED→CANCELLED writer。此为 §11.4「owner-doc §迁移表 vs §实现约定 内部漂移」的实例（doc drift），layer-2 须裁定并登记。Bean 矩阵编码**已实现**行为（APPROVED→CANCELLED）；owner doc §2/§6 偏离按 Fix 登记 + successor（见 Phase 3）。假期余额扣减/返还（APPROVED 扣减、CANCELLED 返还）、排班联动解除（`IErpHrShiftBiz.onLeaveCancelled`）是**动态副作用**，保留 Processor。
- **劳动合同（ErpHrEmploymentContract.status）语义**（owner doc §适用对象五）：dict `erp-hr/contract-status` = ACTIVE/EXPIRED/TERMINATED/SUSPENDED 4 值；**SUSPENDED 为预留死状态**（owner doc §适用对象五已记载：零 `setStatus(SUSPENDED)` writer、无 suspendContract mutation）。活态 = ACTIVE/EXPIRED/TERMINATED；终态 = EXPIRED/TERMINATED。合同状态迁移多为**跨聚合副作用**（员工调动/离职 → 合同 TERMINATED；到期扫描 Job → EXPIRED；招聘入职/调动 → 新建合同 ACTIVE），非合同自身专用 mutation。
- **生产 writer 实况（固定迁移判断散布，已核实）**：
  - **LeaveRequest**：`ErpHrLeaveRequestSubmitProcessor:20`（→SUBMITTED）、`ErpHrLeaveRequestApproveProcessor:25`（→APPROVED）、`ErpHrLeaveRequestBizModel`（`entity/`）：reject `:98`（→REJECTED）、`defaultPrepareSave` 创建写初始态 DRAFT `:70`；`ErpHrLeaveRequestCancelProcessor:21-22`（守卫 `requireStatus(APPROVED)` `:21` + setStatus CANCELLED `:22`，**单源 APPROVED→CANCELLED**）；`AbstractErpHrLeaveRequestProcessor:56`（共享守卫 helper + 领域码 `ERR_LEAVE_ILLEGAL_STATUS_TRANSITION` `ErpHrErrors.java:236`）。
  - **EmploymentContract**（**跨聚合 writer 分散，6+ 类**）：
    - **命名动作迁移**（接线注入 Bean）：`ErpHrEmploymentContractBizModel.renew` `:89-104`（`@BizMutation`，守卫 `status∈{ACTIVE,EXPIRED}` `:94-95`→setStatus ACTIVE `:100` + 更新 endDate；2 源 ACTIVE→ACTIVE 自环 + EXPIRED→ACTIVE）、`ErpHrEmploymentContractExpireOverdueContractsProcessor:42`（批量 ACTIVE→EXPIRED）、`ErpHrEmployeeTransferEmployeeProcessor:190`（ACTIVE→TERMINATED 调动联动）、`ErpHrEmployeeBizModel:233`（ACTIVE→TERMINATED 离职联动）；领域码 `ERR_CONTRACT_ILLEGAL_STATUS_TRANSITION` `ErpHrErrors.java:302`（描述含「仅 ACTIVE/EXPIRED 允许续签」）。
    - **初始态 ACTIVE 写入**（非迁移，**不**注入 `assertCan*`，按 §9.2 选项 c 初始态路径）：`ErpHrRecruitmentHireProcessor:94`（入职新建合同）、`ErpHrRecruitmentBizModel:180`（入职新建）、`ErpHrEmployeeTransferEmployeeProcessor:240`（调动后继合同 `newContractFrom`）、`ErpHrEmployeeBizModel:283`（离职再入职后继合同 `newContractFrom`）。
    - 合同迁移多为跨聚合副作用——接线仅在命名动作迁移处注入 Bean 替换固定判断；初始态 ACTIVE 写入不调 `assertCan*`。
- **既有层 3 回归基线（非 greenfield）**：`TestErpHrLeaveEngine`（LeaveRequest 生命周期 + 审批 + 取消 + 余额联动）、`TestErpHrAttendanceMakeUp`、`TestErpHrEmployeeReferences`（含合同/员工引用）、`TestErpHrShiftScheduling`（请假→排班联动）、`TestErpHrPayrollSimulation`。M0.1 §10 登记的 8 个 `TestErp*StateMachine` 基线不含 hr——hr 域层 3 = 上述既有集成测试，**不是**命名矩阵测试（层 1 新增）。
- **领域错误码已存在**：`ErpHrErrors.ERR_LEAVE_ILLEGAL_STATUS_TRANSITION`（`erp.err.hr.leave.illegal-status-transition`，:236）+ `ERR_CONTRACT_ILLEGAL_STATUS_TRANSITION`（:302）。common 层码 `ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION` 已存在（M1.1 Option A 复用 + `action` 补充参数范式）。
- **生产 Bean 注册范式已存在**：`module-hr/erp-hr-service/src/main/resources/_vfs/erp/hr/beans/app-service.beans.xml` 已以 `<bean id="<FQN>" class="<FQN>"/>` 注册 per-mutation Processor（Submit/Approve/Cancel/Transfer/Hire/ExpireOverdue 等）。StateMachine Bean 沿用此范式。
- **合规基线**：R5（`@Inject private`）= 0、R11= 0。本计划新增 2 Bean 注册 + 注入（EmploymentContract 接线涉及 6+ writer 类注入）须保持 R5=0；接线后内联守卫收敛，R11 不增。

## Goals

- 落地 `ErpHrLeaveRequestStateMachine`（5 态休假轴）+ `ErpHrEmploymentContractStateMachine`（活态 ACTIVE/EXPIRED/TERMINATED + SUSPENDED 死状态登记）两个独立 Bean，各承载已实现迁移矩阵 + 终态/初始态分类 + 只读 `transitions()` 元数据，严格无状态、可经 Delta 同名覆盖。
- 将 LeaveRequest（Submit/Approve/Reject/Cancel Processor/BizModel）与 EmploymentContract（命名动作迁移：renew/Expire/Transfer/Employee-terminate）的**固定来源态/目标态判断**改调 Bean；初始态 ACTIVE 写入（Recruitment/后继合同 newContractFrom）按 §9.2 选项 c 不调 `assertCan*`；动态副作用（假期余额扣减/返还、考勤/排班联动、调动 NOP 用户禁用边界、乐观锁）保留原位。
- 保持全部既有外部行为不变（错误码、LeaveRequest cancel 单源 APPROVED→CANCELLED、余额联动、合同 renew（ACTIVE/EXPIRED→ACTIVE）、调动/离职→合同 TERMINATED 联动、到期 Job→EXPIRED）。
- 各新增层 1 矩阵完备性表驱动测试；层 3 既有集成测试回归全绿。
- 层 2 四方对照：LeaveRequest 确认无死状态；EmploymentContract 裁定 SUSPENDED 死状态（已登记 owner doc §适用对象五）并指派 successor（不在此重构新增 suspend mutation + dict 写锁）。

## Non-Goals

- 不修改任何 `model/*.orm.xml` / `model/*.api.xml` / 字典 yaml（路线图 Non-Goal）：**不向 contract dict 删除/新增值**（SUSPENDED 死状态保留为预留语义入口，对齐 R1.x 先例——保留优于删除，避免 ORM `ext:dict` 改动触发 codegen 漂移）。
- 不实现 `suspendContract`（ACTIVE→SUSPENDED）命名动作（owner doc §适用对象五 Deferred successor；属业务行为变更 + 可能触及 NOP 用户权限副作用，归 successor）。
- 不改变任何业务状态值、动作名、错误码值、权限、余额联动、调动/离职→合同 TERMINATED 时序（路线图 Non-Goal「不借迁移改变既有行为」）。
- 不迁移 `ErpHrEmployee.employmentStatus`（= M3.8，员工雇佣主生命周期，独立 Bean）、`ErpHrTimesheet.status`（已落地 RC-R1.8，非本计划）、`ErpHrSalary.approveStatus/paymentStatus`（= M4.63/64，plan-first）、`ErpHrSurvey`（owner doc §适用对象五 Deferred CRUD 桩）。
- 不引入全局 CRUD 写锁（M0.1 §9 选项 c）。
- 不重构跨聚合副作用编排（调动/招聘/到期 Job 逻辑原位保留，只替换其中固定状态判断）。

## Task Route

- Type: `implementation-only change`（消费 M0.1 契约 + M1.3 模板 + M0.2 清单，落地两轴 Bean + 接线 + 三层测试 + 四方对照；不改契约/模型/公共 API）
- Owner Docs: `docs/architecture/entity-state-machine-bean.md`（M0.1 契约 + §11 迁移模板）、`docs/design/human-resource/state-machine.md`（§适用对象一 LeaveRequest + §适用对象五 EmploymentContract Deferred）、`docs/design/human-resource/payroll.md`（薪酬边界，确认 LeaveRequest/Contract 不触发薪酬过账）、`docs/analysis/2026-08-12-entity-state-axis-inventory.md`（M0.2 清单 hr 行）、`docs/architecture/processor-extension-pattern.md`
- Skill Selection Basis: 路线图 M2.11/M2.12 指定 `nop-backend-dev` + `nop-testing`。`nop-backend-dev` 匹配「BizModel/Processor 接线、跨聚合 writer 注入边界、动态副作用保留、错误码」；`nop-testing` 匹配「矩阵表驱动测试 + 既有集成测试回归」。层 2 引用 `state-machine-business-review-prompt.md`。必需输入已就绪。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（纯后端 Java + 既有 hr-service 测试容器）。
- 前置依赖：M0.1 + M0.2 + M1.3 done。均已满足。

## Execution Plan

### Phase 1 - ErpHrLeaveRequestStateMachine + ErpHrEmploymentContractStateMachine Bean + 注册 + 层 1 矩阵完备性测试

Status: planned
Targets: `module-hr/erp-hr-service/src/main/java/app/erp/hr/service/statemachine/ErpHrLeaveRequestStateMachine.java`（新）+ `ErpHrEmploymentContractStateMachine.java`（新）；`.../beans/app-service.beans.xml`（追加 2 Bean 注册）；`TestErpHrLeaveRequestStateMachineMatrix.java` + `TestErpHrEmploymentContractStateMachineMatrix.java`（新，层 1）
Skill: `nop-backend-dev`（Bean 形状/注册）+ `nop-testing`（层 1 表驱动测试）

- Item Types: `Add | Proof`
- Prereqs: M0.1 + M0.2 + M1.3 done

- [ ] `Add`：创建 `ErpHrLeaveRequestStateMachine`（无状态），矩阵（编码**已实现**行为）：`assertCanSubmit(DRAFT)`/`assertCanApprove(SUBMITTED)`/`assertCanReject(SUBMITTED)`/`assertCanCancel(APPROVED)`（**单源**，对齐代码 `CancelProcessor:21`；owner doc DRAFT/SUBMITTED 偏离在 layer-2 登记）+ 目标态方法 + `isTerminal(APPROVED|REJECTED|CANCELLED)` + `transitions()`（submit/approve/reject/cancel 各 1 = 4 边）+ `terminalStatuses()` + `initialStatuses()`(DRAFT)。Skill: `nop-backend-dev`
- [ ] `Add`：创建 `ErpHrEmploymentContractStateMachine`（无状态），矩阵编码**已实现活态**迁移：`assertCanRenew(ACTIVE|EXPIRED)`（2 源：ACTIVE→ACTIVE 自环 + EXPIRED→ACTIVE，对齐 `renew:94-100`）/ `assertCanExpire(ACTIVE)` / `assertCanTerminate(ACTIVE)` + 目标态方法（`renewTargetStatus()`→ACTIVE / `expireTargetStatus()`→EXPIRED / `terminateTargetStatus()`→TERMINATED）+ `isTerminal(EXPIRED|TERMINATED)` + `transitions()`（renew 2 源 + expire 1 + terminate 1 = 4 边）+ `terminalStatuses()`(EXPIRED/TERMINATED) + `initialStatuses()`(ACTIVE)。**SUSPENDED 不在矩阵**（dict 有值但零 writer = 死状态，layer-2 裁定登记）。Skill: `nop-backend-dev`
- [ ] `Add`：在 `app-service.beans.xml` 以 FQN id 注册两个 Bean（沿用既有 Processor 范式，§11.1 步骤 2）。Skill: `nop-backend-dev`
- [ ] `Proof`（层 1 矩阵完备性，新增 greenfield 表驱动测试，§11.1 步骤 4）：`TestErpHrLeaveRequestStateMachineMatrix` 覆盖 submit/approve/reject/cancel 合法/非法来源态 + **cancel 单源 APPROVED**（对 DRAFT/SUBMITTED/REJECTED 等非法）+ 终态无出边 + transitions（4 边）元数据一致；`TestErpHrEmploymentContractStateMachineMatrix` 覆盖 renew（ACTIVE/EXPIRED 合法，TERMINATED 非法）/expire/terminate 合法（仅 ACTIVE）/非法 + ACTIVE→ACTIVE renew 自环 + 终态无出边 + transitions（4 边）一致，并断言 SUSPENDED 不在 transitions（死状态如实反映）。**不经 BizModel 入口**（层 1 只测 Bean）。Skill: `nop-testing`

Exit Criteria:

- [ ] 两 Bean 落地（LeaveRequest 4 动作：submit/approve/reject/cancel + Contract 3 动作：renew/expire/terminate + 目标态 + isTerminal + transitions 元数据），无状态（grep 证实不 import DAO/IBiz/IServiceContext/事务）。
- [ ] 两 Bean 已在 `app-service.beans.xml` 注册（FQN id）；`@Inject` 字段非 private（合规 R5）。
- [ ] 层 1 矩阵测试 `mvn test -pl module-hr/erp-hr-service -Dtest=TestErpHrLeaveRequestStateMachineMatrix,TestErpHrEmploymentContractStateMachineMatrix` 全绿。
- [ ] 本地化编译检查：`mvn compile -pl module-hr/erp-hr-service -am` 通过（解除 Phase 2 接线依赖）。

### Phase 2 - BizModel/Processor 接线（行为保持）+ 层 3 回归

Status: planned
Targets: LeaveRequest：`ErpHrLeaveRequestSubmitProcessor`/`ApproveProcessor`/`CancelProcessor`、`ErpHrLeaveRequestBizModel`（reject/defaultPrepareSave）、`AbstractErpHrLeaveRequestProcessor`；EmploymentContract（跨聚合）：`ErpHrEmploymentContractExpireOverdueContractsProcessor`、`ErpHrEmployeeTransferEmployeeProcessor`、`ErpHrEmployeeBizModel`、`ErpHrRecruitmentHireProcessor`、`ErpHrRecruitmentBizModel`、`ErpHrEmploymentContractBizModel`
Skill: `nop-backend-dev`（接线 + 错误码映射）+ `nop-testing`（回归断言）

- Item Types: `Fix | Proof`
- Prereqs: Phase 1

- [ ] `Fix`：LeaveRequest Submit/Approve/Cancel Processor + reject BizModel 注入 `ErpHrLeaveRequestStateMachine`，将固定守卫替换为 `stateMachine.assertCan<Action>(from)` + 目标态写回；`AbstractErpHrLeaveRequestProcessor` 守卫改调 Bean，common→领域 `ERR_LEAVE_ILLEGAL_STATUS_TRANSITION` 映射（common 码作 cause）。**cancel 保持单源 APPROVED→CANCELLED**（`assertCanCancel(APPROVED)`，不放开至 DRAFT/SUBMITTED——owner doc 偏离在 layer-2 登记，非本重构改业务行为）。**动态副作用保留原位**：APPROVED 扣减/ CANCELLED 返还假期余额、排班联动解除（`IErpHrShiftBiz.onLeaveCancelled`）。Skill: `nop-backend-dev`
- [ ] `Fix`：EmploymentContract 命名动作迁移注入 `ErpHrEmploymentContractStateMachine`：`ErpHrEmploymentContractBizModel.renew`（`assertCanRenew(from)` before setStatus ACTIVE）、ExpireOverdueContracts（`assertCanExpire` before 批量 setStatus EXPIRED）、TransferEmployee/EmployeeBizModel（`assertCanTerminate` before 联动 setStatus TERMINATED）；common→领域 `ERR_CONTRACT_ILLEGAL_STATUS_TRANSITION` 映射。**初始态 ACTIVE 写入不调 `assertCan*`**（4 处：RecruitmentHire:94 / RecruitmentBizModel:180 / TransferEmployee:240 newContractFrom / EmployeeBizModel:283 newContractFrom —— 按 §9.2 选项 c 初始态路径，非迁移）。**动态副作用保留原位**：调动 NOP 用户禁用边界、到期 Job 批量编排、renew 的 endDate 更新、乐观锁。Skill: `nop-backend-dev`
- [ ] `Proof`（层 3 既有回归保持全绿）：`mvn test -pl module-hr/erp-hr-service` 全绿——重点 `TestErpHrLeaveEngine`（生命周期 + 审批 + cancel APPROVED→CANCELLED + 余额联动）、`TestErpHrEmployeeReferences`（合同/员工引用 + renew + 调动/离职联动）、`TestErpHrShiftScheduling`（请假→排班）、`TestErpHrAttendanceMakeUp`。证明错误码、cancel 单源、余额扣减/返还、合同 renew（ACTIVE/EXPIRED→ACTIVE）、调动/离职→合同 TERMINATED、到期 Job→EXPIRED 均不变。Skill: `nop-testing`

Exit Criteria:

- [ ] LeaveRequest 4 处（Submit/Approve/Cancel/reject）+ EmploymentContract 命名动作迁移处（renew/Expire/Transfer-terminate/Employee-terminate）固定判断均改调 Bean，grep 证实相关方法体内不再有内联 `Objects.equals(from, *_STATUS_*)` / `requireStatus(...)` 矩阵判断（动态副作用如余额联动/排班/NOP 用户边界除外；**4 处初始态 ACTIVE 写入不调 `assertCan*`**——按 §9.2 初始态路径）。
- [ ] 领域错误码 + 参数对外不变（层 3 断言证实）；LeaveRequest cancel 保持单源 APPROVED→CANCELLED；合同 renew（ACTIVE/EXPIRED→ACTIVE）、调动/离职→合同 TERMINATED、到期 Job→EXPIRED 联动行为不变。
- [ ] 层 3 `mvn test -pl module-hr/erp-hr-service` 全绿。

### Phase 3 - 层 2 四方对照（LeaveRequest + EmploymentContract 双轴）+ Delta 适用性

Status: planned
Targets: 四方对照审计记录（写入本计划 Closure 段）；HR 域单轴 Delta 证据
Skill: `state-machine-business-review-prompt.md`（四方对照 + 10 维度）+ `nop-testing`（Delta 双加载）

- Item Types: `Proof | Decision | Fix | Add`
- Prereqs: Phase 2

- [ ] `Proof`（四方对照，§11.1 步骤 5）：以 `state-machine-business-review-prompt.md` 10 维度审查双轴——LeaveRequest（dict leave-status 5 值 ↔ owner-doc §适用对象一 ↔ Bean ↔ writer，**重点裁定 cancel 漂移**：owner doc §2 迁移图 + §6 权限表声明 DRAFT/SUBMITTED→CANCELLED，但代码实际 APPROVED→CANCELLED 单源）；EmploymentContract（dict contract-status 4 值 ↔ owner-doc §适用对象五 ↔ Bean ↔ 全部 writer 含 CRUD/初始态路径，**重点裁定 SUSPENDED 死状态**：dict 有值 + 零 writer + 无 suspend mutation；并复核 renew ACTIVE→ACTIVE 自环与 owner-doc 一致性）。writer 盘点含跨聚合副作用 writer（Expire/Transfer/Recruitment/Employee）+ 4 处初始态 ACTIVE 写入 + 框架入口。Skill: `state-machine-business-review-prompt.md`
- [ ] `Decision`（漂移裁定，路线图规则 5）：
  - **LeaveRequest cancel 漂移**：owner doc §2/§6（DRAFT/SUBMITTED→CANCELLED）vs 代码（APPROVED→CANCELLED 单源）= **doc drift**。Fix = 就地补正 `human-resource/state-machine.md §2` 迁移图 + §6 权限表为「cancel 单源 APPROVED→CANCELLED（已批准休假取消）」+ successor（若 PM 要求 DRAFT/SUBMITTED 自撤为产品需求，开独立 plan 实现 cancel-from-draft/submitted mutation + 触及业务行为 ask-first；本重构不改 cancel 行为）。
  - **SUSPENDED 死状态**：分类 = `intentional reserved`（owner doc §适用对象五已记载 Deferred）——登记为 Decision（保留 dict 值为预留语义入口，不删除，对齐 R1.x 先例）+ successor（suspendContract 业务流落地时）。
  - 任何 owner-doc §迁移表 vs §实现约定 内部漂移按 §11.4 补正；其他不一致按 Fix/Decision 登记。
  Skill: `state-machine-business-review-prompt.md`
- [ ] `Add | Proof`（Delta 适用性，§11.1 步骤 7；M2 非保护域）：在 LeaveRequest 轴证 Delta（派生类覆盖一个动作，如放开 cancel 至 APPROVED 由 HR 操作，或收紧 submit），VFS Delta 层同名 bean id 覆盖，基线/Delta 双加载可区分（复用 M1.2 范式）。Contract 轴继承既有证明，不重复证。Skill: `nop-testing`

Exit Criteria:

- [ ] 双轴四方对照审计记录存在且非空，每维有可追溯结论（引用 Bean 元数据 / owner doc 章节 / dict 位置 / writer 类:行）。
- [ ] LeaveRequest cancel doc drift（§2/§6 补正为 APPROVED→CANCELLED 单源）+ SUSPENDED 死状态 Decision 均已登记 + successor，无静默排除；其他不一致项（若有）已 Fix 登记。
- [ ] LeaveRequest 轴 Delta 双加载运行时证据存在（非静态检查），基线/Delta 可区分。

## Draft Review Record

- Independent draft review iteration 1: `needs revision` (`ses_00bb30faeffe7K4qIoki0Tz5cZ`) — 1 BLOCKER + 2 MAJOR。**B1**：LeaveRequest cancel 基线误称多源 DRAFT/SUBMITTED→CANCELLED，实仓代码（`ErpHrLeaveRequestCancelProcessor:21`）为单源 APPROVED→CANCELLED，Bean 矩阵（5 边）建于错误前提。**M1**：`ErpHrEmploymentContractBizModel.renew`（:89-104，@BizMutation 守卫 ACTIVE/EXPIRED→ACTIVE）遗漏出 Contract 矩阵（仅 expire+terminate 2 边）。**M2**：4 处初始态 ACTIVE 写入（Recruitment:94/180 + TransferEmployee:240/Employee:283 newContractFrom）未枚举为非迁移路径。CRITICAL 歧义消解（M2.12=ErpHrEmploymentContract 非 M3.8 Employee）CONFIRMED 正确。修订：cancel 改单源 APPROVED + doc drift 登记；Contract 矩阵补 renew（4 边）；4 处初始态 ACTIVE 显式枚举为 §9.2 选项 c 非迁移。
- Independent draft review iteration 2: `acceptable as-is` (`ses_00ba986deffekcqpqhttsWJtbv`) — B1/M1/M2 全部 RESOLVED（实仓复核证）。无新 BLOCKER/MAJOR。内部一致性健全（边数 4/4、Goals/Exit Criteria/Phase/Deferred 对齐单源 cancel + renew + 4 初始写入）。1 MINOR（Phase 1 Exit Criteria「Contract 2 动作」→「3 动作」残留）已就地修正。反松弛扫描 clean；保护区/§11.1 phasing/rule 14 bundling/build in Closure Gates 均通过。草案审查收敛，Plan Status → active。

## Closure Gates

> 本计划含生产代码变更（2 Bean + 接线含跨聚合 writer + 测试），Closure Gates 运行完整仓库验证。

- [ ] 范围内行为完成（LeaveRequest + EmploymentContract 双轴 Bean + 接线 + 层 1 矩阵 + 层 3 回归 + 层 2 四方对照 + Delta 证据）
- [ ] 相关文档对齐（`human-resource/state-machine.md §2/§6` cancel 单源补正 + SUSPENDED 死状态 Decision 补注；路线图 M2.11 + M2.12 done）
- [ ] 已运行验证：`mvn clean install -DskipTests`（全仓库 BUILD SUCCESS）+ `mvn test -pl module-hr/erp-hr-service`（全绿）+ `bash docs/audits/nop-compliance-checker.sh`（exit 0，R5=0/R11=0 无漂移）
- [ ] 无范围内项目降级为 deferred/follow-up（SUSPENDED 死状态 Decision 必须落地登记）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：Plan Status、各 Phase Status、Exit Criteria、Closure Gates、日志一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### LeaveRequest DRAFT/SUBMITTED→CANCELLED 自撤命名动作

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: owner doc §2/§6 声明 cancel 多源（DRAFT/SUBMITTED），但代码实际仅 APPROVED→CANCELLED。Bean 如实编码已实现行为（单源 APPROVED）；owner doc 已就地补正为单源。实现 DRAFT/SUBMITTED 自撤属业务行为变更（员工未审批前自撤），须 ask-first，非状态机集中重构范围。
- Successor Required: yes（触发条件 = PM 要求员工休假自撤业务流落地时，开独立 plan 实现 cancel-from-draft/submitted mutation）

### EmploymentContract suspendContract（ACTIVE→SUSPENDED）命名动作

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: owner doc §适用对象五已记载 SUSPENDED 为预留死状态（零 writer + 无 mutation）。实现 suspendContract 属业务行为变更，可能触及 NOP 用户权限/调动副作用（保护区域 ask-first），非状态机集中重构范围。dict SUSPENDED 值保留为预留语义入口（不删除，对齐 R1.x 先例）。
- Successor Required: yes（触发条件 = PM 要求合同中止/恢复业务流落地时，开独立 plan 实现 suspend/resume mutation + 状态迁移守卫）

### hr 域其余状态轴（Employee employmentStatus / Salary / Timesheet / Survey）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: `ErpHrEmployee.employmentStatus` = M3.8（员工雇佣主生命周期，含 RESIGNED/TERMINATED/RETIRED 预留死状态，独立 Bean）；`ErpHrSalary.approveStatus/paymentStatus` = M4.63/64（plan-first 业财过账）；`ErpHrTimesheet.status` 已落地 RC-R1.8（非本计划重做）；`ErpHrSurvey` = §适用对象五 Deferred CRUD 桩。
- Successor Required: yes（触发条件 = 各对应 M3/M4 工作项启动时）

## Closure

Status Note: <待执行后填写>

Closure Audit Evidence:

- Auditor / Agent: <待独立结束审计子代理填写>
- Evidence: <task id / log link / walkthrough record>

Follow-up:

- <仅非阻塞跟进项目；已确认缺陷不得出现在此处>
