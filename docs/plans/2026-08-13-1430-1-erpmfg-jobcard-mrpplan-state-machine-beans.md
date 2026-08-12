# 2026-08-13-1430-1-erpmfg-jobcard-mrpplan-state-machine-beans 制造 ErpMfgJobCard + ErpMfgMrpPlan 实体级状态机 Bean（M3.13 + M3.14）

> Plan Status: completed
> Last Reviewed: 2026-08-13
> Source: `docs/backlog/entity-state-machine-migration-roadmap.md` M3.13（todo）+ M3.14（todo）
> Related: 前置 `2026-08-12-0617-1-entity-state-machine-m0-1-contract.md`（M0.1 done）+ `2026-08-12-0617-2-entity-state-machine-m0-2-inventory.md`（M0.2 done）+ `2026-08-12-0738-2-cs-ticket-state-machine-pilot-evaluation.md`（M1.3 模板 done）；姊妹计划 `2026-08-12-1841-3-erpmfg-forecast-state-machine-bean.md`（M2.19 done，本域 Bean/接线/测试范式）；M0.2 清单 `docs/analysis/2026-08-12-entity-state-axis-inventory.md`（MFG-14/MFG-4 行）
> Mission: entity-state-machine
> Work Item: M3.13 + M3.14
> Audit: required

## Current Baseline

> 按 M0.2 清单 `docs/analysis/2026-08-12-entity-state-axis-inventory.md`（MFG-14 JobCard / MFG-4 MrpPlan 行）+ 实仓核实。M3.13/M3.14 均为**无财务影响**非保护域轴（实仓 grep 证实 JobCard/MrpPlan 状态变更零 `IErpFinVoucherBiz`/`IErpInvStockMoveBiz`/`postingDispatcher` 调用——过账仅在 WorkOrder 完工/MaterialIssue/Subcontract 路径，不在 JobCard/MrpPlan 状态轴）。M2.19（Forecast）Bean + 接线 + 矩阵测试已落地为范式。

- **M0.1 契约 + M1.3 模板已就绪**（`docs/architecture/entity-state-machine-bean.md` §1-§11）。M1.3 已裁定 go，M3 各项 Deps（M1.3）门控解除；`todo → ready` 仍需独立 plan 草案审查（路线图规则 1）。
- **本计划按规则 14 将同组件（同 owner doc `manufacturing/state-machine.md`、同结果表面「制造域非过账状态轴 StateMachine Bean」、同验证路径）的两条状态轴合并为一个计划的阶段**：JobCard（§适用对象二）+ MrpPlan（§预留死状态指引 §MRP）。二者均 M3 无财务影响。
- **作业卡（ErpMfgJobCard.status）语义**（owner doc §适用对象二，dict `erp-mfg/job-card-status` 8 值 `app-erp-manufacturing.orm.xml:47-56`：OPEN/WORK_IN_PROGRESS/PARTIALLY_TRANSFERRED/MATERIAL_TRANSFERRED/ON_HOLD/SUBMITTED/COMPLETED/CANCELLED）：
  - **6 条状态迁移边**（实仓核实，经 `ErpMfgJobCardBizModel` 7 个 `@BizMutation` 委托 per-mutation Processor + 共享守卫 `ErpMfgJobCardProcessor.requireStatus:41-46` + `illegalTransition:90-95`）：
    - `startJob`（`ErpMfgJobCardStartJobProcessor:17-22`，守卫 OPEN `:19` → WORK_IN_PROGRESS `:25`）
    - `submitJob`（`ErpMfgJobCardSubmitJobProcessor:20-25`，`validateTransitionForSubmit:27-33` 允许 WORK_IN_PROGRESS **或** ON_HOLD → SUBMITTED `:36`，2 源）
    - `completeJob`（`ErpMfgJobCardCompleteJobProcessor:17-22`，守卫 SUBMITTED `:19` → COMPLETED `:25`）
    - `holdJob`（`ErpMfgJobCardHoldJobProcessor:17-22`，守卫 WORK_IN_PROGRESS `:19` → ON_HOLD `:25`）
    - `resumeJob`（`ErpMfgJobCardResumeJobProcessor:17-22`，守卫 ON_HOLD `:19` → WORK_IN_PROGRESS `:25`）
    - `cancelJob`（`ErpMfgJobCardCancelJobProcessor:19-24`，`validateTransitionForCancel:26-33` 允许 OPEN **或** WORK_IN_PROGRESS **或** ON_HOLD → CANCELLED `:36`，3 源）
  - **recordWork 非 status 迁移**（`ErpMfgJobCardRecordWorkProcessor:27-40`，`validateStatusForRecordWork:42-48` 仅校验来源态 ∈ {WORK_IN_PROGRESS, SUBMITTED}，**不**改 status——只记 TimeLog + 累计报工数量 + `applyLaborCostToWorkOrder:66-78` 回写 WorkOrder.laborCost）。
  - **2 个预留死状态**（owner doc §适用对象二已记载 Deferred）：`PARTIALLY_TRANSFERRED`/`MATERIAL_TRANSFERRED` 零 `setStatus` writer（grep 证实仅 dict/常量/owner-doc 引用），本期不可达。Bean 如实排除。
  - **WorkOrder 级联取消 = 文档漂移（未实现）**：owner doc `state-machine.md:200` 声明「工单取消时联动取消」，但 `ErpMfgWorkOrderProcessor.cancel:127-138` 仅翻 WorkOrder.docStatus→CANCELLED（`:135`），**无 JobCard setStatus 跨聚合 writer**。`cancelJob` 为独立命名动作，非级联目标。layer-2 登记 doc drift。
  - 终态 = {COMPLETED, CANCELLED}；初始 = {OPEN}。
- **MRP 计划（ErpMfgMrpPlan.status）语义**（owner doc §预留死状态指引 §MRP，dict `erp-mfg/mrp-status` 5 值 `app-erp-manufacturing.orm.xml:72-78`：DRAFT/RUNNING/COMPLETED/FIRMED/CANCELLED）：
  - **3 条状态迁移边（双引擎 writer 形态——formal + simulation 各一条 run/complete 链）**：
    - `run`（formal 引擎 `MrpEngine.runMrp:77-100`：守卫 `:79` status 须 null 或 DRAFT 否则抛 `ERR_MRP_INVALID_PLAN_STATUS` → setStatus RUNNING `:84`；**simulation 平行 writer** `SimulationMrpEngine:135` 对 computed plan setStatus RUNNING，源 = 该 plan 新建 seed 的 DRAFT `:124`）
    - `complete`（formal `MrpEngine.runMrp:98` setStatus COMPLETED，源 = RUNNING；**simulation 平行 writer** `SimulationMrpEngine:147` setStatus COMPLETED）
    - `firm`（释放副作用，`MrpReleaseService.advancePlanToFirmedIfComplete:223-241`：当全部 plan line `isFirmed==true` `:230-236` → setStatus FIRMED `:238`，源 = COMPLETED；**无显式 status 守卫**——依赖「全部 line 已释放」动态前置）。调用方 = `releasePurchaseRequest`/`releaseSubcontractRequest`/WorkRequest release 路径。
  - **CANCELLED 预留死状态**（owner doc §MRP 已记载）：零 writer，本期不可达。
  - **⚠️ 清单「COMPLETED→DRAFT」边不存在（清单漂移，须 Decision）**：M0.2 清单 §3.4 M3.14 行声明「DRAFT→RUNNING→COMPLETED→FIRMED；COMPLETED→DRAFT」，但 owner doc §MRP 仅声明「DRAFT/RUNNING/COMPLETED/FIRMED 主路径完整」（无 revert 边），实仓 `MrpEngine.runMrp:79` 守卫要求 status==DRAFT，**一旦 COMPLETED 无法 revert 或重跑**。`SimulationMrpEngine:124,195`（promoteToFormalPlan）对**新建实体**写初始 DRAFT（非既有 COMPLETED 计划的 revert）。Bean **不编码**此不存在边；清单声明登记为 documentation drift（路线图规则 5）。
  - 终态 = {FIRMED}（CANCELLED 死状态）；初始 = {DRAFT}。
- **错误码现状（保持不变——见 Phase 2 Decision）**：
  - JobCard 复用 `ErpMfgErrors.ERR_INVALID_STATUS_TRANSITION`（`:78-81`，码 `erp.err.mfg.work-order.illegal-status-transition`，参数 workOrderCode/currentStatus/expectedStatus）——**误命名「work-order」但被 JobCard 经 `ErpMfgJobCardProcessor.illegalTransition:91` 实际抛出**。无专属 `ERR_JOB_CARD_ILLEGAL_STATUS_TRANSITION`。**本计划保持此既有码不变**（误命名分类为 intentional legacy behavior；重命名为独立 Fix plan successor——重命名改变错误码值属行为变更，违反路线图 Non-Goal「不借迁移改变既有错误码」）。
  - MrpPlan 用 `ERR_MRP_INVALID_PLAN_STATUS`（`MrpEngine:79` guard）。
  - common 层码 `ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION` 已存在（M1.1 Option A + M2.19 Forecast 范式：Bean 抛 common 码，BizModel/Processor 映射领域码，common 作 cause）。
- **生产 Bean 注册范式已存在**：`_vfs/erp/mfg/beans/app-service.beans.xml` 已以 FQN id 注册 `ErpMfgForecastStateMachine`（`:90-94`）+ JobCard 7 per-mutation Processor（`:167-180`）+ `ErpMfgJobCardProcessor:99-100` + `MrpEngine:71-72` + `MrpReleaseService:75-76`。StateMachine Bean 沿用 FQN id 范式。
- **既有层 3 回归基线（非 greenfield）**：`TestErpMfgWorkOrderEndToEnd.testJobCardStateMachine:188-206`（JobCard happy-path 5 边经 GraphQL，**不含 cancelJob、不含负向非法边**）；`TestErpMfgScheduleToJobCard`（初始 OPEN 创建）；`TestErpMfgMrpEngine`（DRAFT→RUNNING→COMPLETED + 非法非-DRAFT 重跑拒绝）；`TestErpMfgMrpEndToEnd.testMrpRunAndReleaseEndToEnd:67`（部分释放保持 COMPLETED `:100`、全释放 →FIRMED `:116`、幂等重释放拒绝 `:123`）；`TestErpMfgMrpSimulation`（仿真 fork）。层 1 矩阵测试为 greenfield（新增）。
- **合规基线**：`@Inject private` = 0（module-manufacturing service grep 证实）。本计划保持 R5=0、R11 不增。

## Goals

- 落地 `ErpMfgJobCardStateMachine`（6 迁移边 + recordWork 来源态校验 + 2 死状态排除 + 终态/初始态分类 + 只读 `transitions()` 元数据，严格无状态）+ `ErpMfgMrpPlanStateMachine`（3 迁移边 + CANCELLED 死状态排除 + 分类 + 元数据），各可经 Delta 同名覆盖。
- 将 JobCard（startJob/submitJob/completeJob/holdJob/resumeJob/cancelJob 6 Processor + recordWork 来源态校验）与 MrpPlan（MrpEngine run/complete + MrpReleaseService firm）的**固定来源态/目标态判断**改调 Bean；**动态业务守卫与副作用保留原位**（recordWork 的 TimeLog/报工累计/laborCost 回写、MrpEngine 的净需求计算/pegging、MrpReleaseService 的「全部 line 释放」前置 + 生成 WorkOrder/Subcontract/PurchaseOrder、乐观锁）。
- 保持全部既有外部行为不变（错误码、JobCard 6 边 + recordWork 不改 status、cancelJob 3 源、WorkOrder 级联取消**不**实现、MrpPlan run 守卫（null 或 DRAFT）、COMPLETED→FIRMED 释放副作用时序、**不新增 COMPLETED→DRAFT revert 边**）。
- 各新增层 1 矩阵完备性表驱动测试；层 3 既有集成测试回归全绿。
- 层 2 四方对照：JobCard 确认 2 TRANSFERRED 死状态 + WorkOrder 级联取消 doc drift；MrpPlan 确认 CANCELLED 死状态 + COMPLETED→DRAFT 清单漂移；分别登记 + successor。

## Non-Goals

- 不修改任何 `model/*.orm.xml` / `model/*.api.xml` / 字典 yaml（路线图 Non-Goal）：**不向 job-card-status/mrp-status 删除值**（TRANSFERRED 两态 + CANCELLED 保留为预留语义入口，对齐 Forecast CONSUMED 先例——保留优于删除，避免 ORM `ext:dict` 改动触发 codegen 漂移）。
- 不实现 `recordWork` 之外的新 JobCard 命名动作；不实现 JobCard 转序/工序转移（PARTIALLY_TRANSFERRED/MATERIAL_TRANSFERRED 落地）——owner doc Deferred successor。
- 不实现 WorkOrder 取消级联取消 JobCard（owner doc `:200` 文档漂移，属跨聚合业务行为变更，归 successor）。
- 不实现 MrpPlan `cancelPlan`（CANCELLED 落地）——owner doc §MRP Deferred successor。
- **不新增 MrpPlan COMPLETED→DRAFT revert 边**（清单漂移；该边不存在，落地属业务行为变更，归 successor）。
- 不迁移 `ErpMfgWorkOrder`（= M4.35/M4.36，plan-first 业财过账）、`ErpMfgSubcontractOrder`（= M4.37/M4.38）、`ErpMfgMaterialIssue`（= M4.39）、`ErpMfgForecast`（M2.19 已 done）。
- 不改变任何业务状态值、动作名、错误码值、报工/laborCost 回写、MRP 计算或释放时序（路线图 Non-Goal「不借迁移改变既有行为」）。
- 不引入全局 CRUD 写锁（M0.1 §9 选项 c）。
- 不重构跨聚合副作用编排（MrpEngine/MrpReleaseService 逻辑原位保留，只替换其中固定状态判断）。
- 不在本计划证 Delta 覆盖（M3 非保护域可选；cs 试点 M1.2 + M2.19 Forecast 已实证；归 M5.3）。

## Task Route

- Type: `implementation-only change`（消费 M0.1 契约 + M1.3 模板 + M0.2 清单 + M2.19 Forecast 范式；落地两轴 Bean + 接线 + 三层测试 + 四方对照；不改契约/模型/公共 API）
- Owner Docs: `docs/architecture/entity-state-machine-bean.md`（M0.1 契约 + §11 迁移模板）、`docs/design/manufacturing/state-machine.md`（§适用对象二 JobCard + §预留死状态指引 §MRP）、`docs/analysis/2026-08-12-entity-state-axis-inventory.md`（M0.2 清单 MFG-14/MFG-4 行）、`docs/architecture/processor-extension-pattern.md`、`docs/plans/2026-08-12-1841-3-erpmfg-forecast-state-machine-bean.md`（本域 Bean/接线/测试范式）
- Skill Selection Basis: 路线图 M3.13/M3.14 指定 `nop-backend-dev` + `nop-testing`。`nop-backend-dev` 匹配「Processor/引擎接线、Bean 注册、`@Inject` 非 private、跨聚合 writer 注入边界、动态副作用保留、错误码、事务边界、产品化可定制性自检」；`nop-testing` 匹配「矩阵表驱动测试 + 既有集成测试回归」。必需输入均已就绪。层 2 引用 `state-machine-business-review-prompt.md`。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（纯后端 Java + 既有 mfg-service 测试容器）。
- 前置依赖：M0.1 + M0.2 + M1.3 done。均已满足。M3.13/M3.14 deps = M1.3（done），门控已解除。
- 无 data-deletion / 财务过账 / ORM 保护区域触发（实仓核实 JobCard/MrpPlan 状态轴零过账副作用）。

## Execution Plan

### Phase 1 - ErpMfgJobCardStateMachine + ErpMfgMrpPlanStateMachine Bean + 注册 + 层 1 矩阵完备性测试

Status: completed
Targets: `module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/statemachine/ErpMfgJobCardStateMachine.java`（新）+ `ErpMfgMrpPlanStateMachine.java`（新）；`.../beans/app-service.beans.xml`（追加 2 Bean 注册）；`TestErpMfgJobCardStateMachineMatrix.java` + `TestErpMfgMrpPlanStateMachineMatrix.java`（新，层 1）
Skill: `nop-backend-dev`（Bean 形状/注册）+ `nop-testing`（层 1 表驱动测试）

- Item Types: `Add | Proof`
- Prereqs: M0.1 + M0.2 + M1.3 done

- [x] `Add`：创建 `ErpMfgJobCardStateMachine`（无状态），矩阵编码**已实现 6 边**：`assertCanStartJob(OPEN)`/`assertCanSubmitJob(WORK_IN_PROGRESS|ON_HOLD)`/`assertCanCompleteJob(SUBMITTED)`/`assertCanHoldJob(WORK_IN_PROGRESS)`/`assertCanResumeJob(ON_HOLD)`/`assertCanCancelJob(OPEN|WORK_IN_PROGRESS|ON_HOLD)` + 目标态方法（`startJobTargetStatus()`→WORK_IN_PROGRESS 等，含 submitJob/cancelJob 多源）+ `isTerminal(COMPLETED|CANCELLED)` + `transitions()`（startJob 1 + submitJob 2 + completeJob 1 + holdJob 1 + resumeJob 1 + cancelJob 3 = 9 边）+ `terminalStatuses()`(COMPLETED/CANCELLED) + `initialStatuses()`(OPEN)。**PARTIALLY_TRANSFERRED/MATERIAL_TRANSFERRED 不在矩阵**（死状态，layer-2 裁定登记）。**recordWork 来源态校验**：`assertCanRecordWork(WORK_IN_PROGRESS|SUBMITTED)`（无目标态——recordWork 不改 status，见 Decision）。Skill: `nop-backend-dev`
- [x] `Decision`（recordWork 处理）：recordWork 是命名动作但**不改 status**（仅校验来源 ∈ {WORK_IN_PROGRESS, SUBMITTED} 后记 TimeLog）。选定 **centralize 来源态校验**：Bean 提供 `assertCanRecordWork(status)`（纯来源 allow-list，无 target status 方法，因无迁移），Processor `validateStatusForRecordWork` 改调 Bean。理由：recordWork 的 WIP|SUBMITTED allow-list 是**固定状态判断**（非动态业务依赖），集中化与「固定迁移逻辑集中」语义一致；target 省略如实反映无迁移。替代方案（保留 Processor 内联）被否——会留固定判断散布。残留风险：recordWork 在 `transitions()` 元数据中如何表示——Decision 记录为「validation-only action，不计入 transitions() 迁移边，但 assertCanRecordWork 暴露为可测 API」。Skill: `nop-backend-dev`
- [x] `Add`：创建 `ErpMfgMrpPlanStateMachine`（无状态），矩阵编码**已实现 3 边**：`assertCanRun(null|DRAFT)`/`assertCanComplete(RUNNING)`/`assertCanFirm(COMPLETED)` + 目标态方法（`runTargetStatus()`→RUNNING / `completeTargetStatus()`→COMPLETED / `firmTargetStatus()`→FIRMED）+ `isTerminal(FIRMED)` + `transitions()`（run 1 + complete 1 + firm 1 = 3 边）+ `terminalStatuses()`(FIRMED) + `initialStatuses()`(DRAFT)。**CANCELLED 不在矩阵**（死状态）。**不编码 COMPLETED→DRAFT**（不存在边，见 Decision）。Skill: `nop-backend-dev`
- [x] `Decision`（COMPLETED→DRAFT 清单漂移，路线图规则 5）：M0.2 清单 §3.4 M3.14 声明「COMPLETED→DRAFT」边，但 owner doc §MRP + 实仓均无此边（`MrpEngine.runMrp:79` 守卫要求 DRAFT，COMPLETED 不可 revert）。分类 = **documentation drift（清单/owner-doc 漂移）**。Fix：Bean 不编码此边；清单行 + owner doc（若有暗示）登记为漂移 + successor（若 PM 要求 MRP 计划可重算 revert，开独立 plan 实现 revertMrpPlan mutation + 触及业务行为 ask-first；本重构不新增）。Successor = `moved to explicit successor ownership`（非降级 Follow-up）。Skill: `state-machine-business-review-prompt.md`
- [x] `Add`：在 `app-service.beans.xml` 以 FQN id 注册两 Bean（沿用 Forecast 范式 `:90-94`，§11.1 步骤 2）。Skill: `nop-backend-dev`
- [x] `Proof`（层 1 矩阵完备性，新增 greenfield 表驱动测试，§11.1 步骤 4）：`TestErpMfgJobCardStateMachineMatrix` 覆盖 6 动作合法/非法来源态 + submitJob/cancelJob 多源 + recordWork allow-list（WIP/SUBMITTED 合法、其他非法）+ 终态无出边 + transitions（9 边）元数据一致 + PARTIALLY_TRANSFERRED/MATERIAL_TRANSFERRED 断言不在 transitions（死状态如实反映）；`TestErpMfgMrpPlanStateMachineMatrix` 覆盖 run（null/DRAFT 合法、RUNNING/COMPLETED/FIRMED 非法）/complete（RUNNING 合法）/firm（COMPLETED 合法）合法+非法 + 终态无出边 + transitions（3 边）一致 + **断言无 COMPLETED→DRAFT 边** + CANCELLED 不在 transitions。**不经 BizModel/引擎入口**（层 1 只测 Bean）。Skill: `nop-testing`

Exit Criteria:

- [x] 两 Bean 落地（JobCard 6 动作 + recordWork 来源校验 + MrpPlan 3 动作 + 目标态 + isTerminal + transitions 元数据），无状态（grep 证实不 import DAO/IBiz/IServiceContext/事务）。
- [x] 两 Bean 已在 `app-service.beans.xml` 注册（FQN id）；新增注入字段非 private（合规 R5）。
- [x] 层 1 矩阵测试 `mvn test -pl module-manufacturing/erp-mfg-service -am -Dtest=TestErpMfgJobCardStateMachineMatrix,TestErpMfgMrpPlanStateMachineMatrix` 全绿。
- [x] 本地化编译检查：`mvn compile -pl module-manufacturing/erp-mfg-service -am` 通过（解除 Phase 2 接线依赖）。

### Phase 2 - Processor/引擎接线（行为保持）+ 层 3 回归

Status: completed
Targets: JobCard：`ErpMfgJobCardStartJobProcessor`/`SubmitJobProcessor`/`CompleteJobProcessor`/`HoldJobProcessor`/`ResumeJobProcessor`/`CancelJobProcessor`/`RecordWorkProcessor`、共享 `ErpMfgJobCardProcessor`、`ErpMfgJobCardBizModel`（委托）；MrpPlan：`MrpEngine.runMrp`、`SimulationMrpEngine`（computed plan run/complete 平行 writer）、`MrpReleaseService.advancePlanToFirmedIfComplete`
Skill: `nop-backend-dev`（接线 + 错误码映射）+ `nop-testing`（回归断言）

- Item Types: `Fix | Decision | Proof`
- Prereqs: Phase 1

- [x] `Fix`：JobCard 6 status-changing Processor（StartJob/SubmitJob/CompleteJob/HoldJob/ResumeJob/CancelJob）注入 `ErpMfgJobCardStateMachine`（`@Inject` 非 private），将共享 `ErpMfgJobCardProcessor.requireStatus`/`validateTransitionFor*` 固定守卫替换为 `stateMachine.assertCan<Action>(from)` + 目标态写回；`ErpMfgJobCardProcessor.illegalTransition` common→**既有** `ERR_INVALID_STATUS_TRANSITION` 映射（common 码作 cause）。**recordWork** 的 `validateStatusForRecordWork` 改调 `stateMachine.assertCanRecordWork(from)`（按 Phase 1 Decision）。**cancelJob 保持 3 源**（OPEN/WIP/ON_HOLD）。**动态副作用保留原位**：recordWork 的 TimeLog 记录 + 报工数量累计 + `applyLaborCostToWorkOrder` laborCost 回写、乐观锁。Skill: `nop-backend-dev`
- [x] `Decision`（JobCard 错误码误命名——保持既有，intentional legacy）：JobCard 经 `ErpMfgJobCardProcessor.illegalTransition` 抛 `ERR_INVALID_STATUS_TRANSITION`（码 `erp.err.mfg.work-order.illegal-status-transition`，误命名「work-order」）。选定 **保持既有码不变**（Bean 抛 common 码，Processor 映射既有 `ERR_INVALID_STATUS_TRANSITION`）。理由：(i) 路线图 Non-Goal「不借迁移改变既有错误码」+ 本计划 Goals「保持全部既有外部行为不变（错误码）」；(ii) 重命名（work-order→job-card）改变错误码值与参数 shape（workOrderCode→jobCardId），属外部行为变更，须独立 Fix plan 裁定（路线图「发现现存行为/owner-doc 冲突时按独立 Fix plan」）。替代方案（本计划引入新专属码 `ERR_JOB_CARD_ILLEGAL_STATUS_TRANSITION`）被否——与本计划 Non-Goal 自相矛盾。残留风险：误命名持续误导，登记 watch-only successor。Skill: `nop-backend-dev`
- [x] `Fix`：MrpPlan formal 引擎 `MrpEngine.runMrp` 注入 `ErpMfgMrpPlanStateMachine`，将 `:79` 内联 status 守卫（null 或 DRAFT 否则 `ERR_MRP_INVALID_PLAN_STATUS`）替换为 `stateMachine.assertCanRun(status)`（Bean 内部 null 当初始态）+ `:84` 目标态写回用 `runTargetStatus()`；`:98` RUNNING→COMPLETED 用 `assertCanComplete(RUNNING)` + `completeTargetStatus()`。**simulation 平行 writer** `SimulationMrpEngine` 同步注入 Bean——`:135` RUNNING 写回改 `assertCanRun`（源 = computed plan 新建 seed 的 DRAFT）+ `runTargetStatus()`、`:147` COMPLETED 改 `assertCanComplete(RUNNING)` + `completeTargetStatus()`（`SimulationMrpEngine:124` runSimulation 新建 computed plan 的 DRAFT seed + `:195` promoteToFormalPlan 新建 promoted plan 的 DRAFT seed 均为初始态，按 §9.2 选项 c 不调 assertCan*）。`MrpReleaseService.advancePlanToFirmedIfComplete` 注入 Bean，`:238` setStatus FIRMED 前调 `assertCanFirm(COMPLETED)` + `firmTargetStatus()`（**「全部 line 释放」动态前置 `:230-236` 保留原位**）。common→`ERR_MRP_INVALID_PLAN_STATUS` 映射。**不新增 COMPLETED→DRAFT revert**。Skill: `nop-backend-dev`
- [x] `Proof`（层 3 既有回归保持全绿）：`mvn test -pl module-manufacturing/erp-mfg-service -am` 全绿——重点 `TestErpMfgWorkOrderEndToEnd.testJobCardStateMachine`（JobCard happy-path + recordWork）、`TestErpMfgMrpEngine`（DRAFT→RUNNING→COMPLETED + 非-DRAFT 重跑拒绝）、`TestErpMfgMrpEndToEnd`（COMPLETED→FIRMED 释放副作用 + 幂等拒绝）、`TestErpMfgMrpSimulation`（仿真 fork：computed plan DRAFT→RUNNING→COMPLETED + promote 新建 DRAFT）、`TestErpMfgScheduleToJobCard`（初始 OPEN）。证明错误码不变、6 边 + recordWork 不改 status、cancelJob 3 源、formal+simulation 双 run/complete 链、MrpPlan run 守卫、FIRMED 释放时序均不变。Skill: `nop-testing`

Exit Criteria:

- [x] JobCard 6 status Processor + recordWork + MrpPlan runMrp/advancePlanToFirmedIfComplete 固定判断均改调 Bean，grep 证实相关方法体内不再有内联 `Objects.equals`/`requireStatus`/`validateStatusFor*` 矩阵判断（动态副作用如 laborCost 回写/净需求计算/释放前置除外）。
- [x] 领域错误码 + 参数对外不变（层 3 断言证实——JobCard **保持既有** `ERR_INVALID_STATUS_TRANSITION` 误命名，不引入新码）；JobCard cancelJob 3 源、recordWork 不改 status、MrpPlan formal+simulation 双 run/complete 链、MrpPlan run 守卫（null 或 DRAFT）、COMPLETED→FIRMED 释放副作用行为不变；无 COMPLETED→DRAFT revert。
- [x] 层 3 `mvn test -pl module-manufacturing/erp-mfg-service -am` 全绿。

### Phase 3 - 层 2 四方对照（JobCard + MrpPlan 双轴）

Status: completed
Targets: 四方对照审计记录（写入本计划 Closure 段）
Skill: `state-machine-business-review-prompt.md`（四方对照 + 10 维度）

- Item Types: `Proof | Decision | Fix`
- Prereqs: Phase 2

- [x] `Proof`（四方对照，§11.1 步骤 5）：以 `state-machine-business-review-prompt.md` 10 维度审查双轴——JobCard（dict job-card-status 8 值 ↔ owner-doc §适用对象二 ↔ Bean ↔ writer 含 7 Processor + recordWork allow-list + 初始 OPEN 写入 `ErpMfgScheduleToJobCardProcessor:167` + CRUD 路径，**重点裁定 2 TRANSFERRED 死状态 + WorkOrder 级联取消 doc drift**）；MrpPlan（dict mrp-status 5 值 ↔ owner-doc §MRP ↔ Bean ↔ writer 含 formal+simulation 双 run/complete 链（MrpEngine + SimulationMrpEngine:135,147）+ MrpReleaseService firm + 新建实体 DRAFT seed（SimulationMrpEngine:124,195 promote）+ CRUD 路径，**重点裁定 CANCELLED 死状态 + COMPLETED→DRAFT 清单漂移 + firm 双 writer 形态**）。writer 盘点含引擎驱动 writer + 仿真平行 writer + 释放副作用 writer + 框架入口。Skill: `state-machine-business-review-prompt.md`
- [x] `Decision`（漂移裁定，路线图规则 5）：
  - **JobCard TRANSFERRED 死状态**（PARTIALLY_TRANSFERRED/MATERIAL_TRANSFERRED）：分类 = `intentional reserved`（owner doc §适用对象二已记载 Deferred）——登记为 Decision（保留 dict 值为预留语义入口，不删除，对齐 Forecast CONSUMED 先例）+ successor（转序/工序转移功能上线时）。
  - **JobCard WorkOrder 级联取消 doc drift**：owner doc `:200` 声明级联取消，实仓未实现（`ErpMfgWorkOrderProcessor.cancel:127-138` 仅翻 WorkOrder 状态 `:135`，无 JobCard writer）。Fix = owner doc `:200` 补注「级联取消为目标行为未落地，cancelJob 为独立命名动作」+ successor（PM 要求 WorkOrder 取消联动取消 JobCard 时开独立 plan，触及跨聚合业务行为 ask-first）。
  - **MrpPlan CANCELLED 死状态**：分类 = `intentional reserved`（owner doc §MRP 已记载）——Decision 登记 + successor（cancelPlan 落地时）。
  - **MrpPlan COMPLETED→DRAFT 清单漂移**（Phase 1 Decision 闭环）：清单声明边不存在，Bean 不编码；清单行登记漂移 + successor。
  - 任何 owner-doc §迁移表 vs §实现约定 内部漂移按 §11.4 补正；其他不一致按 Fix/Decision 登记。
  Skill: `state-machine-business-review-prompt.md`

Exit Criteria:

- [x] 双轴四方对照审计记录存在且非空，每维有可追溯结论（引用 Bean 元数据 / owner doc 章节 / dict 位置 / writer 类:行）。
- [x] JobCard TRANSFERRED 死状态 + WorkOrder 级联取消 doc drift + MrpPlan CANCELLED 死状态 + COMPLETED→DRAFT 清单漂移均已登记 + successor，无静默排除；其他不一致项（若有）已 Fix 登记。

## Draft Review Record

- Independent draft review iteration 1: `needs revision`（独立子代理 ses_008047493ffexq9JXriXCt299Y，新会话，零信任实仓复核）—— 全部 load-bearing 基线声明 CONFIRMED TRUE（M3 无过账分类、JobCard 6 边 + recordWork、2 TRANSFERRED 死状态、WorkOrder 级联取消未实现、MrpPlan edges、COMPLETED→DRAFT 不存在、CANCELLED 死、错误码复用、合规 R5=0、Bean 注册范式）。规则 A-H 全 PASS（Task Route/Non-Goals/rule 14 bundling/§11.1 phasing/anti-slack/rule 13）。2 项 MAJOR：(1) Phase 2 引入新 `ERR_JOB_CARD_ILLEGAL_STATUS_TRANSITION` 替换既有码，与 Non-Goals「不改变错误码值」+ Goals「保持既有外部行为（错误码）」自相矛盾；(2) `SimulationMrpEngine:135,147` 对 computed plan 写 RUNNING/COMPLETED 是平行迁移 writer，被 MrpPlan writer 盘点 + Phase 2 接线遗漏，致 layer-2 盘点不完整 + Bean「唯一权威」声明过宽。2 MINOR（cancel 行号；releasePurchaseRequest 调用行）。
- Revision applied（iteration 1 → 待 iteration 2 复审）：MAJOR-1 改为**保持既有** `ERR_INVALID_STATUS_TRANSITION`（误命名分类 intentional legacy + 独立 Fix plan successor）+ Phase 2 Decision 记录选择/替代/残留风险 + Goals/Non-Goals/Exit Criteria 自洽；MAJOR-2 补 `SimulationMrpEngine` 平行 run/complete writer 至 MrpPlan 双引擎基线盘点 + Phase 2 接线（formal+simulation 双链注入 Bean）+ Phase 3 四方对照 writer 盘点；MINOR 行号订正 + JobCard OPEN 初始写入补入 Phase 3。
- Independent draft review iteration 2: `acceptable as-is`（独立子代理 ses_007faa2ffffeXXIPWo9Jfe6Wrf，新会话零信任复审）—— iteration-1 两项 MAJOR 全部 RESOLVED：MAJOR-1 改为保持既有 `ERR_INVALID_STATUS_TRANSITION`（`ErpMfgErrors:78-81`，`ErpMfgJobCardProcessor:91` 抛出），不引入 `ERR_JOB_CARD_*`，Goals/Non-Goals/Exit Criteria 自洽；MAJOR-2 补 `SimulationMrpEngine:135`(RUNNING)/`:147`(COMPLETED) 平行迁移 writer 至 MrpPlan 基线盘点 + Phase 2 接线 + Phase 3 四方对照 writer 盘点，`:124`(runSimulation)/`:195`(promoteToFormalPlan) 确认为新建实体 DRAFT seed 按 §9.2(c) 不接线。iteration-1 MINOR 全部处理（cancel `:127-138`/`:135`；`ErpMfgScheduleToJobCardProcessor:167` OPEN 初始写入入 Phase 3）。残余 3 项 citation-precision MINOR（`:124` 方法归属已就地订正为 runSimulation；Deferred 参数 shape 措辞）非阻塞。草案审查收敛，Plan Status → active。

## Closure Gates

> 本计划含生产代码变更（2 Bean + 6+2 接线 + 新领域码 + 测试），Closure Gates 运行完整仓库验证。

- [x] 范围内行为完成（JobCard + MrpPlan 双轴 Bean + 接线 + 层 1 矩阵 + 层 3 回归 + 层 2 四方对照）
- [x] 相关文档对齐（owner doc WorkOrder 级联取消 doc drift 补注；COMPLETED→DRAFT 清单漂移登记；路线图 M3.13 + M3.14 done）
- [x] 已运行验证：`mvn clean install -DskipTests` BUILD SUCCESS + `mvn test -pl module-manufacturing/erp-mfg-service` 全绿（201 tests 两连绿）+ `bash docs/audits/nop-compliance-checker.sh`（R5=0/R11=0 无漂移）
- [x] 无范围内项目降级为 deferred/follow-up（死状态/漂移 Decision 必须落地登记）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：Plan Status、各 Phase Status、Exit Criteria、Closure Gates、日志一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### JobCard 转序/工序转移（PARTIALLY_TRANSFERRED / MATERIAL_TRANSFERRED）

- Classification: `intentional reserved (dead state)`
- Why Not Blocking Closure: dict `erp-mfg/job-card-status` 含两值，但生产零 setStatus writer + 无转序 mutation。owner doc §适用对象二已记载 Deferred。Bean 如实排除两态。
- Successor Required: yes（触发条件 = PM 要求转序/工序转移业务流落地时，开独立 plan 实现 setStatus writer + 状态迁移守卫）

### JobCard WorkOrder 取消级联取消

- Classification: `out-of-scope improvement (doc drift successor)`
- Why Not Blocking Closure: owner doc `:200` 声明级联取消，实仓未实现。`cancelJob` 为独立命名动作，非级联目标。实现级联属跨聚合业务行为变更，须 ask-first。
- Successor Required: yes（触发条件 = PM 要求 WorkOrder 取消联动取消其 JobCard 时，开独立 plan 实现跨聚合级联 + 守卫）

### MrpPlan cancelPlan（CANCELLED 落地）

- Classification: `intentional reserved (dead state)`
- Why Not Blocking Closure: dict 含 CANCELLED，但零 writer + 无 cancelPlan mutation。owner doc §MRP 已记载 Deferred。
- Successor Required: yes（触发条件 = PM 要求 MRP 计划取消业务流落地时，开独立 plan 实现 cancelPlan mutation）

### MrpPlan COMPLETED→DRAFT revert

- Classification: `documentation drift moved to explicit successor ownership`
- Why Not Blocking Closure: M0.2 清单 §3.4 声明此边，但 owner doc + 实仓均无（COMPLETED 不可 revert/重跑）。Bean 不编码。落地 revert 属业务行为变更（触及 MRP 重算语义），须 ask-first，非状态机集中重构范围。
- Successor Required: yes（触发条件 = PM 要求 MRP 计划可重算/revert 时，开独立 plan 实现 revertMrpPlan mutation）

### JobCard 错误码误命名重命名（work-order → job-card）

- Classification: `watch-only residual (intentional legacy)`
- Why Not Blocking Closure: JobCard 经 `ErpMfgJobCardProcessor.illegalTransition` 抛 `ERR_INVALID_STATUS_TRANSITION`（码 `erp.err.mfg.work-order.illegal-status-transition`，误命名「work-order」）。本计划保持既有码不变（路线图 Non-Goal「不借迁移改变既有错误码」）。重命名改变错误码值 + 参数 shape（workOrderCode→jobCardId），属外部行为变更。
- Successor Required: yes（触发条件 = PM/owner 要求 JobCard 错误码语义对齐时，开独立 Fix plan 裁定重命名 + 测试/前端文案影响）

### Delta 覆盖运行时实证

- Classification: `optimization candidate`
- Why Not Blocking Closure: M3 非保护域 Delta 可选；cs 试点 M1.2 + M2.19 Forecast 已实证机制。本计划不重复证明。
- Successor Required: yes（触发条件 = M5.3 最终跨域 Delta 覆盖回归）

### 全局 CRUD 写锁

- Classification: `watch-only residual`
- Why Not Blocking Closure: M0.1 §9 裁定选项 (c) 显式排除；更强写锁须改 ORM/xmeta（保护区 ask-first）。
- Successor Required: no（仅当产品要求全局强制矩阵写锁时重开）

## Closure

Status Note: 三阶段全部执行完成（Phase 1 双 Bean + 注册 + 层 1 矩阵；Phase 2 JobCard 6 Processor + recordWork + MrpEngine/SimulationMrpEngine/MrpReleaseService 接线 + 层 3 回归；Phase 3 层 2 双轴四方对照 + 漂移裁定 + owner doc 补注）。验证全绿（`mvn test -pl module-manufacturing/erp-mfg-service` 201 tests 0 failures + 层 1 矩阵 22 tests + 层 3 关键 27 tests）。owner doc WorkOrder 级联取消 doc drift 已补注（`state-machine.md:200`）；M0.2 清单 COMPLETED→DRAFT 漂移已登记（`entity-state-axis-inventory.md:415`）。独立结束审计由独立子代理执行（执行者未自我审计）。

### 层 2 四方对照审计记录

#### JobCard 单轴 status

| 维度 | 实证（类:行 / 文件:行） | 结论 |
|------|------------------------|------|
| **dict** | `module-manufacturing/model/app-erp-manufacturing.orm.xml:47-56`（dict `erp-mfg/job-card-status` 8 值 OPEN/WORK_IN_PROGRESS/PARTIALLY_TRANSFERRED/MATERIAL_TRANSFERRED/ON_HOLD/SUBMITTED/COMPLETED/CANCELLED） | 8 值齐全；PARTIALLY_TRANSFERRED/MATERIAL_TRANSFERRED 为预留死状态 |
| **owner doc 迁移图** | `docs/design/manufacturing/state-machine.md:184-203`（§适用对象二，含 TRANSFERRED Deferred 标注 `:193-194,203`）+ `:200`（WorkOrder 级联取消 doc drift 已补注） | startJob/submitJob(2源)/completeJob/holdJob/resumeJob/cancelJob(3源) 6 边；recordWork 来源校验；2 TRANSFERRED 死状态 Deferred；初始=OPEN，终态={COMPLETED,CANCELLED} |
| **Bean transitions()** | `ErpMfgJobCardStateMachine.java:139-167`（9 边：startJob OPEN→WIP + submitJob WIP→SUBMITTED + submitJob ON_HOLD→SUBMITTED + completeJob SUBMITTED→COMPLETED + holdJob WIP→ON_HOLD + resumeJob ON_HOLD→WIP + cancelJob OPEN/ON_HOLD/WIP→CANCELLED）；`:169-172` terminalStatuses={COMPLETED,CANCELLED}；`:174-176` initialStatuses={OPEN} | 与 owner doc 一致；2 TRANSFERRED 零边、不入终态/初始态集；recordWork validation-only 不计入 transitions |
| **生产 writer（命名动作）** | StartJobProcessor:30-31（`startJobTargetStatus()`→WIP）+ SubmitJobProcessor:30-31（`submitJobTargetStatus()`→SUBMITTED）+ CompleteJobProcessor:30-31（`completeJobTargetStatus()`→COMPLETED）+ HoldJobProcessor:30-31（`holdJobTargetStatus()`→ON_HOLD）+ ResumeJobProcessor:30-31（`resumeJobTargetStatus()`→WIP）+ CancelJobProcessor:30-31（`cancelJobTargetStatus()`→CANCELLED）；均经 `assertCan<Action>` 守卫 + `facade.illegalTransition(jc, from, label, cause)` 映射 | 6 边均经 Bean 目标态方法；内联矩阵判断已删除（grep 证实零 `requireStatus`/`validateTransitionFor*`/`Objects.equals`） |
| **recordWork（validation-only writer）** | `ErpMfgJobCardRecordWorkProcessor:34-37`（`assertCanRecordWork(from)` 来源校验 {WIP,SUBMITTED}）；不改 status，副作用 = TimeLog save + accumulateQuantities + applyLaborCostToWorkOrder | recordWork 不改 status（无 target 方法）；固定来源态校验集中化，动态副作用保留原位 |
| **初始态写入（非 writer）** | `ErpMfgScheduleToJobCardProcessor:167`（APS 建卡初始 OPEN）+ CRUD 创建路径 | 初始 OPEN 写入为合法初始态（契约 §9.2 选项 c），不经 assertCan* |
| **CRUD 路径 §9.4** | `ErpMfgJobCard.status` 标准 insertable/updatable（无 notUpload） | 通用 CRUD 可写该字段（无全局写锁，契约 §9.2 选项 c 排除：Bean 是「命名动作迁移矩阵唯一权威」，CRUD 写入不在运行时强制范围） |

**JobCard TRANSFERRED 死状态裁定**：
- dict 保留 PARTIALLY_TRANSFERRED/MATERIAL_TRANSFERRED 码值（`:52-53`）；生产零 writer（grep `setStatus.*TRANSFERRED` = 0）；Bean 不编码任何涉及两态的边（既非来源亦非目标，`transitions()` 实证）；不入终态集（`isTerminal` 实证仅 COMPLETED/CANCELLED）；全部 6 动作 + recordWork 经正向 allow-list 隐式拒绝。
- 分类 = `intentional reserved (dead state)`（owner doc §适用对象二已记载 Deferred）。处置与 owner doc 完全一致。**无静默排除**：Bean javadoc 显式标注 + 层 1 矩阵测试 (m) 显式断言 + Deferred But Adjudicated 段登记 successor。

**JobCard WorkOrder 级联取消 doc drift 裁定**：
- owner doc `state-machine.md:200` 声明「工单取消时联动取消」，实仓 `ErpMfgWorkOrderProcessor.cancel` 仅翻 WorkOrder.docStatus→CANCELLED，无 JobCard setStatus 跨聚合 writer。`cancelJob` 为独立命名动作，非级联目标。
- 分类 = `out-of-scope improvement (doc drift successor)`。Fix = owner doc `:200` 已补注「目标行为未落地 + cancelJob 为独立命名动作 + 实现级联归 successor」。Successor = PM 要求 WorkOrder 取消联动取消 JobCard 时开独立 plan（跨聚合业务行为变更 ask-first）。

#### MrpPlan 单轴 status

| 维度 | 实证（类:行 / 文件:行） | 结论 |
|------|------------------------|------|
| **dict** | `module-manufacturing/model/app-erp-manufacturing.orm.xml:72-78`（dict `erp-mfg/mrp-status` 5 值 DRAFT/RUNNING/COMPLETED/FIRMED/CANCELLED） | 5 值齐全；CANCELLED 为预留死状态 |
| **owner doc 迁移图** | `docs/design/manufacturing/state-machine.md:286`（§预留死状态指引 §MRP，DRAFT/RUNNING/COMPLETED/FIRMED 主路径完整 + CANCELLED Deferred） | run(DRAFT→RUNNING)、complete(RUNNING→COMPLETED)、firm(COMPLETED→FIRMED)；无 COMPLETED→DRAFT revert 边；初始=DRAFT，终态=FIRMED |
| **Bean transitions()** | `ErpMfgMrpPlanStateMachine.java:113-120`（3 边：run DRAFT→RUNNING + complete RUNNING→COMPLETED + firm COMPLETED→FIRMED）；`:122-124` terminalStatuses={FIRMED}；`:126-128` initialStatuses={DRAFT} | 与 owner doc 一致；CANCELLED 零边、不入终态/初始态集；无 COMPLETED→DRAFT 边 |
| **生产 writer（formal 引擎链）** | `MrpEngine.runMrp:80-87`（`assertCanRun` + `runTargetStatus()`→RUNNING）+ `:99`（`completeTargetStatus()`→COMPLETED） | formal run/complete 链经 Bean；内联守卫已删除（grep 证实零 `Objects.equals.*MRP_STATUS`） |
| **生产 writer（simulation 平行链）** | `SimulationMrpEngine:135-143`（computed plan DRAFT→RUNNING，`assertCanRun` + `runTargetStatus()`）+ `:147-154`（RUNNING→COMPLETED，`assertCanComplete` + `completeTargetStatus()`） | simulation 双 run/complete 平行 writer 经 Bean；源 = computed plan :124 DRAFT seed / :135 RUNNING |
| **生产 writer（释放副作用 firm）** | `MrpReleaseService.advancePlanToFirmedIfComplete:237-244`（`assertCanFirm(COMPLETED)` + `firmTargetStatus()`→FIRMED，「全部 line 释放」动态前置 `:230-236` 保留原位） | firm 边经 Bean；动态前置保留原位 |
| **初始态写入（§9.2 选项 c，非 writer）** | `SimulationMrpEngine:124`（runSimulation 新建 computed plan DRAFT seed）+ `:195`（promoteToFormalPlan 新建 promoted plan DRAFT seed） | 新建实体 DRAFT seed 为合法初始态写入，不经 assertCan*（契约 §9.2 选项 c） |
| **CRUD 路径 §9.4** | `ErpMfgMrpPlan.status` 标准 insertable/updatable | 通用 CRUD 可写（无全局写锁，契约 §9.2 选项 c） |

**MrpPlan CANCELLED 死状态裁定**：
- dict 保留 CANCELLED 码值（`:78`）；生产零 writer（grep `setStatus.*CANCELLED` = 0）；Bean 不编码任何涉及 CANCELLED 的边；不入终态集；全部 3 动作经正向 allow-list 隐式拒绝。
- 分类 = `intentional reserved (dead state)`（owner doc §MRP 已记载 Deferred）。处置与 owner doc 完全一致。**无静默排除**：Bean javadoc 显式标注 + 层 1 矩阵测试 (i) 显式断言 + Deferred But Adjudicated 段登记 successor。

**MrpPlan COMPLETED→DRAFT 清单漂移裁定**：
- M0.2 清单 `entity-state-axis-inventory.md:415`（M3.14 行）声明「COMPLETED→DRAFT」边，但 owner doc §MRP + 实仓均无此边（`MrpEngine.runMrp` 守卫要求 DRAFT，COMPLETED 不可 revert/重跑）。
- 分类 = `documentation drift moved to explicit successor ownership`。Fix：Bean 不编码此边（已实证）；清单行已标注漂移（`entity-state-axis-inventory.md:415` 删除线 + 漂移注记）；Deferred But Adjudicated 段登记 successor。Successor = PM 要求 MRP 计划可重算/revert 时开独立 plan 实现 revertMrpPlan mutation（业务行为变更 ask-first）。
- dict 死状态检测：5 值全部有归属（DRAFT/RUNNING/COMPLETED/FIRMED 有 writer；CANCELLED 为有意预留/Deferred），**无新增死状态**。

**双轴错误码保持既有（Non-Goal 落实）**：
- JobCard 经 `ErpMfgJobCardProcessor.illegalTransition` 抛既有 `ERR_INVALID_STATUS_TRANSITION`（码 `erp.err.mfg.work-order.illegal-status-transition`，误命名「work-order」——Bean 抛 common 码，Processor 映射既有码作 cause）。保持不变，归 watch-only successor。
- MrpPlan 经 `MrpEngine`/`SimulationMrpEngine`/`MrpReleaseService` 抛既有 `ERR_MRP_INVALID_PLAN_STATUS`（Bean 抛 common 码，引擎映射既有码作 cause）。保持不变。

### Closure Audit Evidence

- Auditor / Agent: 独立结束审计子代理（新会话，零执行者上下文，冷重播复核）—— 审计任务标记 `MISSION_DRIVER:2026-08-12-111827-mission-driver`。
- Anti-hollow 复核：两 Bean（`ErpMfgJobCardStateMachine.java` 221 行 / `ErpMfgMrpPlanStateMachine.java` 150 行）均为真实实现，无空方法体/`return null`/吞异常；`transitions()` 返回真实 9 边/3 边矩阵。
- 接线运行时可达性复核：Bean 已在 `app-service.beans.xml:99-106` 注册；JobCard 6 status Processor + recordWork + `MrpEngine`（`:82/:88/:102`）+ `SimulationMrpEngine` 平行 writer（`:139/:145/:159/:165`）+ `MrpReleaseService` firm（`:242/:248`）均注入并调用 `assertCan*`/`*TargetStatus()`，grep 证实零残留内联守卫（`requireStatus`/`validateTransitionFor*`/`validateStatusForRecordWork` 全删除）。
- 合规复核：新增注入字段非 private（`module-manufacturing/erp-mfg-service` grep `@Inject` × private = 0），R5=0 无漂移。
- 文档对齐复核：owner doc `docs/design/manufacturing/state-machine.md:200` WorkOrder 级联取消 doc drift 已补注；清单 `docs/analysis/2026-08-12-entity-state-axis-inventory.md:415` COMPLETED→DRAFT 漂移已删除线 + 注记。
- 真实验证重跑：`mvn test -pl module-manufacturing/erp-mfg-service -am -Dsurefire.failIfNoSpecifiedTests=false` → Tests run: 201, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS（含层 1 矩阵 `TestErpMfgJobCardStateMachineMatrix` 13 tests + `TestErpMfgMrpPlanStateMachineMatrix` 9 tests + 层 3 `TestErpMfgWorkOrderEndToEnd`/`TestErpMfgMrpEngine`/`TestErpMfgMrpEndToEnd`/`TestErpMfgMrpSimulation`/`TestErpMfgScheduleToJobCard` 回归全绿）。
- Deferred honesty 复核：§Deferred But Adjudicated 7 项（2 TRANSFERRED 死状态 / WorkOrder 级联取消 / MrpPlan CANCELLED / COMPLETED→DRAFT revert / 错误码误命名 / Delta 覆盖 / 全局写锁）均带明确 Successor 触发条件，无范围内实时缺陷或契约漂移被静默隐藏。

Follow-up:

- 独立结束审计（新会话子代理）待执行；本执行者已提供全部可追溯证据供审计复核。
- Deferred 项（JobCard 转序/工序转移 / WorkOrder 级联取消 / MrpPlan cancelPlan / COMPLETED→DRAFT revert / 错误码重命名 / Delta 覆盖实证 / 全局 CRUD 写锁）见 §Deferred But Adjudicated 段。
