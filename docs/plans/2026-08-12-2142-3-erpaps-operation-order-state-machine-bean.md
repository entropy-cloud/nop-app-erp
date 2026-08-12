# 2026-08-12-2142-3-erpaps-operation-order-state-machine-bean APS ErpApsOperationOrder 实体级状态机 Bean（M2.13）

> Plan Status: active
> Last Reviewed: 2026-08-12
> Source: `docs/backlog/entity-state-machine-migration-roadmap.md` M2.13（todo）
> Related: 前置 `2026-08-12-0617-1-entity-state-machine-m0-1-contract.md`（M0.1 done）+ `2026-08-12-0617-2-entity-state-machine-m0-2-inventory.md`（M0.2 done）+ `2026-08-12-0738-1-cs-ticket-state-machine-bean-pilot.md`（M1.1 范式）+ `2026-08-12-0738-2-cs-ticket-state-machine-pilot-evaluation.md`（M1.3 模板 done）；姊妹范式 `2026-08-12-1118-1-erpct-contract-state-machine-bean.md`（单实体单轴 + INLINE BizModel 接线 + 多源 cancel 范本）+ `2026-08-12-1841-1-erpdrp-plan-line-state-machine-beans.md`（引擎/服务类写 status + 隐式门控 advance 范本）
> Mission: entity-state-machine
> Work Item: M2.13
> Audit: required

## Current Baseline

- **M0.1 契约 + M1.3 模板已就绪**（`docs/architecture/entity-state-machine-bean.md` §1-§11）。M1.3 已裁定 **go**，M2 各项 Deps（M1.3）门控解除；`todo → ready` 仍需独立 plan 草案审查（路线图规则 1）。
- **M2.13 归类**：M2 简单生命周期（§11.2），非保护域、无审批子矩阵、无跨域过账副作用（OperationOrder 头状态不触发业财过账；容量预留 DB unique 是强一致约束但非财务过账）。**非 plan-first**；但仍跨模块行为变更，须独立 plan + 独立草案审查 + 独立结束审计（路线图规则 3）。
- **工序工单（ErpApsOperationOrder.status）语义**（owner doc `docs/design/aps/state-machine.md`，142 行，采用 10 维度审查结构而非「迁移表/实现约定」分节）：5 态 DRAFT/PLANNED/IN_PROGRESS/FINISHED/CANCELLED；初始 = DRAFT（§1 `:13`、§5 `:63`）；终态 = FINISHED/CANCELLED（§3 `:46-49`，不可恢复，须重建）。设计声明边（§2 `:21-42`）：DRAFT→PLANNED（APS 排产 `:23`）、DRAFT→CANCELLED（取消 `:24`）、PLANNED→IN_PROGRESS（开始执行 `:27`）、PLANNED→DRAFT（重排回退 `:28`）、PLANNED→CANCELLED（取消 `:29`）、IN_PROGRESS→FINISHED（完工 `:32`）、IN_PROGRESS→CANCELLED（异常终止 `:33`）。
- **owner doc 已登记的 Deferred / drift 项（须保持，非本计划落地）**：
  - §3 `:49`（P1-MA2-077）：illegal-transition 守卫已落地——start 仅 PLANNED、complete 仅 IN_PROGRESS、cancel 仅 DRAFT/PLANNED/IN_PROGRESS。**已实现**（见下 writer）。
  - §4 `:58`（末句，DEFERRED）：PLANNED→IN_PROGRESS/FINISHED/CANCELLED 状态翻转的**容量预留释放归 P1-MA2-077 MR1**——即 start/complete/cancel 当前**不释放**预留，仅 PLANNED→DRAFT 重排回退路径今日释放。本计划**不得新增**预留释放逻辑。
  - §6 `:77-79`（P1-MA2-078 DEFERRED）：IN_PROGRESS cancel 的审批工作流（需生产主管审批）未落地，今日 cancel 仅经 entry-permission overlay 门控。
- **dict 实况（无死状态）**：`module-aps/model/app-erp-aps.orm.xml:8-14` inline dict `erp-aps/operation-order-status` = 5 值 DRAFT/PLANNED/IN_PROGRESS/FINISHED/CANCELLED（镜像 `module-aps/erp-aps-meta/.../_vfs/dict/erp-aps/operation-order-status.dict.yaml:8-25`）；绑定 `ErpApsOperationOrder.status`（`app-erp-aps.orm.xml:78 ext:dict`）。5 值全部有生产 writer（无死状态）。常量 `ErpApsConstants.OP_STATUS_*`（5 个）。
- **生产 writer 实况（固定迁移判断散布，已核实）—— 三条写路径**：
  - **(A) INLINE BizModel 用户命名 mutation**（`module-aps/erp-aps-service/src/main/java/app/erp/aps/service/entity/ErpApsOperationOrderBizModel.java`）：start `:124-137`（守卫 `Objects.equals(status, PLANNED) :128`→IN_PROGRESS `:134`）、complete `:139-152`（守卫 `Objects.equals(status, IN_PROGRESS) :143`→FINISHED `:149`）、cancel `:154-173`（三源守卫 `:159-161` status∈{DRAFT,PLANNED,IN_PROGRESS}→CANCELLED `:170`）；三处内联抛 `ERR_APS_OP_ILLEGAL_TRANSITION`（`:129,:144,:162`）。**无私有 `illegalTransition` helper、无共享 abstract Processor 骨架**。`updateSchedule :239-257` 不写 status（仅 plannedStart/End）。
  - **(B) 排产引擎算法路径**（`ErpApsSchedulingEngine.java`，纯 POJO 无 Spring/DB，javadoc `:18-34`）：scheduleForward `:85`(不可行→DRAFT)/`:93`(成功→PLANNED)、scheduleBackward `:122/:132/:140`(失败→DRAFT)/`:149`(成功→PLANNED)。**无 status 守卫**——算法按可行性写状态。调用链：BizModel.scheduleForward → ScheduleForwardProcessor → SchedulingProcessor.run（filter status=DRAFT `:79`）→ Engine 写实体 status → SchedulingProcessor.persist `:126`（`acquireReservation :131-133` 仅 PLANNED+machineId+timestamps + `dao.saveOrUpdateEntity :135`）。引擎写 status 是**算法内部状态**，非用户命名 mutation 的 status 守卫——**不在 Bean 治理范围**（Decision，见 Phase 3）。
  - **(C) 抢单重排回退路径**（`ErpApsSchedulingInsertRushOrderProcessor.java`）：insertRushOrder 对窗口内低优先级 op `:73-81` 调 `facade.releaseReservationsByOrder(op.getId()) :76` + `op.setStatus(DRAFT) :77` + 清 plannedStart/End `:78-79` + saveOrUpdate `:80`；抢单单 `:89` setStatus DRAFT。**硬守卫 `:53-59`**：窗口内 IN_PROGRESS op → 抛 `ERR_APS_OP_IN_PROGRESS_NOT_RESCHEDULABLE`（不可重排）。选择规则 `:64-72`：opPriority > rushPriority 的 PLANNED op 被回退，否则冻结。此 PLANNED→DRAFT 是 insertRushOrder 的**内部副作用**，非独立用户 mutation。
  - **(D) CTP 影子仿真**（`ErpApsAtpCtpServiceImpl.java:226` setStatus DRAFT）：作用于**非持久化影子实体**，无生产 DB 影响，out of scope。
- **容量预留 DB unique 约束（强一致，保留不动）**：`app-erp-aps.orm.xml:333` `UK_APS_CAPACITY_RESERVATION_SLOT (machineId, plannedStartT, plannedEndT)`，实体 `ErpApsCapacityReservation`（`:313-316`，**非逻辑删除**——释放=硬删避免软删行占 UK，注释 `:307-312`）。获取：`ErpApsSchedulingProcessor.acquireReservation :161-184`（pre-check `hasOverlappingReservation :187-193` + INSERT + `flushSession :177` + JdbcException TOCTOU fallback→`ERR_APS_CAPACITY_CONFLICT`）。释放：`releaseReservationsByOrder :197-212`（按 operationOrderId 查询硬删 + `flushSession :211`），今日仅被 `InsertRushOrderProcessor:76` 调用。**start/complete/cancel 不释放预留（Deferred P1-MA2-077 MR1）——本计划不得新增**。
- **错误码**：`ErpApsErrors.ERR_APS_OP_ILLEGAL_TRANSITION`（`module-aps/erp-aps-service/src/main/java/app/erp/aps/service/ErpApsErrors.java:80-84`，值 `erp.err.aps.op-illegal-transition`，参数 operationOrderCode/currentStatus/expectedStatus）；相关 `ERR_APS_OP_IN_PROGRESS_NOT_RESCHEDULABLE :44-48`（operationOrderCode/currentStatus）、`ERR_APS_OP_ORDER_ALREADY_SCHEDULED :88-91`、`ERR_APS_OP_ORDER_NOT_FOUND :75-78`。common 层码 `ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION`（`module-common-service/.../ErpCommonErrors.java:23-27`，值 `nop.err.erp.common.illegal-status-transition`，参数 currentStatus/expectedStatus）已存在并被 cs/hr/ct/pur/sal Bean 复用（M1.1 Option A + `action` 补充参数范式）。
- **既有层 3 回归基线（非 greenfield，但不在 M0.1 §10 的 8 个登记基线内）**：aps 域**无** `TestErpAps*StateMachine`（grep 零匹配）。层 3 基线 = `TestErpApsOperationOrderStateGuards`（`module-aps/erp-aps-service/src/test/java/app/erp/aps/service/TestErpApsOperationOrderStateGuards.java`，187 行，11 个 @Test，经 `JunitAutoTestCase` + H2，覆盖 start/complete/cancel happy + 非法路径）、`TestErpApsOperationOrderCrudSmoke`、`TestErpApsSchedulingEngine`（前向/后向/抢单/CTP 影子）、`TestErpApsCapacityReservation`（TOCTOU UK + pre-check）、`TestErpApsScheduleManagement`、`TestErpApsCrossDomainIntegration`（CRP load source）、`TestErpApsDemandPlanning`。**已知覆盖缺口**（层 1 矩阵补）：无 5×N 穷举矩阵、无 cancel 三源全覆盖矩阵断言、无 `transitions()` 元数据一致性、无终态全动作拒绝、无基线 IoC + Delta 覆盖测试。
- **生产 Bean 注册范式已存在**：`module-aps/erp-aps-service/src/main/resources/_vfs/erp/aps/beans/app-service.beans.xml:12-33` 已以 `<bean id="<FQN>" class="<FQN>"/>` 注册 Scheduling Processors + AtpCtpService + LoadSourceProvider；BizModel `ErpApsOperationOrderBizModel` 经 `_service.beans.xml:16` `ioc:type="@bean:id"` 自动发现（非显式 bean）。StateMachine Bean 须显式 FQN-id 注册（沿用 cs 试点范式 `app-service.beans.xml:33-36`）。
- **greenfield 范畴**：`module-aps/**/statemachine/` 不存在、无 `ErpAps*StateMachine` Bean（grep 零匹配）。本计划为 aps 域首例 StateMachine Bean。
- **合规基线**：R5（`@Inject private`）= 0（已核实 module-aps service 零违例，BizModel `:43,46,49,52` + SchedulingProcessor `:50,53` + 各 Processor 均 @Inject 字段包级可见）、R11（Processor 重复状态判断方法）= 0。本计划新增 1 Bean 注册 + 注入须保持 R5=0；接线后内联守卫收敛至 Bean，R11 不增。

## Goals

- 落地真实 `ErpApsOperationOrderStateMachine` Bean（一 Bean 对一实体一轴 `status`），承载**已实现**迁移矩阵 + 终态/初始态分类 + 只读 `transitions()` 元数据（声明全部 7 条已实现边，含 DRAFT→PLANNED 排产与 PLANNED→DRAFT 重排回退，供可达性/完备性分析），严格无状态、可经 Delta 同名覆盖。为 aps 域首例 StateMachine Bean（建立域内范式）。
- 将 `ErpApsOperationOrderBizModel`（start/complete/cancel）的**固定来源态/目标态判断**（内联 `Objects.equals` 守卫）改调 Bean（cancel 三源 {DRAFT,PLANNED,IN_PROGRESS} 经 Bean 正向 `assertCanCancel` 表达）；对抢单重排回退路径（`ErpApsSchedulingInsertRushOrderProcessor:77` PLANNED→DRAFT）在所选 PLANNED op 回退处接入 Bean 的 `assertCanRevertToDraft(PLANNED)` 矩阵权威。**动态业务守卫保留原位**：IN_PROGRESS 不可重排硬守卫（`InsertRushOrderProcessor:53-59` `ERR_APS_OP_IN_PROGRESS_NOT_RESCHEDULABLE`）、优先级选择规则（`:64-72`）、容量预留获取/释放（SchedulingProcessor）、requireOrder 实体加载、乐观锁。
- 保持全部既有外部行为不变（错误码 + 参数 operationOrderCode/currentStatus/expectedStatus、start 仅 PLANNED、complete 仅 IN_PROGRESS、cancel 三源、IN_PROGRESS 不可重排、容量预留获取/释放时序、**start/complete/cancel 不释放预留**——Deferred P1-MA2-077 MR1 不变）。
- 新增层 1 矩阵完备性表驱动测试（greenfield，不经 BizModel 入口）；层 3 既有集成测试（`TestErpApsOperationOrderStateGuards` + scheduling/capacity 等）回归全绿。
- 层 2 四方对照（dict ↔ `aps/state-machine.md` ↔ Bean 元数据 ↔ 全部 writer 含 CRUD 路径）单轴裁定，**显式裁定「引擎算法写 status 不在 Bean 治理范围」与「预留释放 Deferred」**（Decision 登记），禁止静默排除。

## Non-Goals

- 不修改任何 `model/*.orm.xml` / `model/*.api.xml` / 字典 yaml（路线图 Non-Goal）：不向 dict 新增/删除值（5 值不变）。
- **不把排产引擎 `ErpApsSchedulingEngine` 的 DRAFT↔PLANNED 算法写路由经 Bean**——引擎是纯算法 POJO，按可行性写状态，无 status 守卫可集中；其调用方 `SchedulingProcessor.persist` 的预留获取/释放逻辑保留不动（Decision 见 Phase 3）。
- **不在 start/complete/cancel 路径新增容量预留释放**——owner doc §4 `:58` 明示 Deferred P1-MA2-077 MR1；本计划只集中既有固定判断，不实现 Deferred 项。
- 不实现 IN_PROGRESS cancel 审批工作流（P1-MA2-078 Deferred）。
- 不改变任何业务状态值、动作名、错误码值、权限、预留获取/释放时序、抢单优先级规则（路线图 Non-Goal「不借迁移改变既有行为」）。
- 不迁移 aps Schedule 实体状态（`ErpApsSchedule.status`，独立轴，技术派生性质待 M0.2 复核）或 DispatchLog previousStatus/newStatus（审计日志）。
- 不引入全局 CRUD 写锁（M0.1 §9 选项 c；更强写锁 successor）。
- 不声称全域 Delta 覆盖已验证（M1.2 已证客服单轴；本计划证 OperationOrder 单轴 Delta，全域回归归 M5.3）。

## Task Route

- Type: `implementation-only change`（消费已定稿 M0.1 契约 + M1.3 模板 + M0.2 清单，落地单轴 Bean + 接线 + 三层测试 + 四方对照；不改契约/模型/公共 API）
- Owner Docs: `docs/architecture/entity-state-machine-bean.md`（M0.1 契约 + §11 迁移模板）、`docs/design/aps/state-machine.md`（§1-§10——含 §2 迁移 `:21-42`、§3 终态 `:46-49`、§4 异常路径 `:58` Deferred、§7 外部依赖 WorkOrder 级联）、`docs/analysis/2026-08-12-entity-state-axis-inventory.md`（M0.2 清单 aps 行 M2.13）、`docs/architecture/processor-extension-pattern.md`（Bean 嵌入 BizModel/Processor 编排点）
- Skill Selection Basis: 路线图 M2.13 指定 `nop-backend-dev` + `nop-testing`。`nop-backend-dev` 匹配「INLINE BizModel 接线、抢单 Processor 回退路径接线、cancel 三源守卫、容量预留动态守卫保留、引擎边界划分、错误码」；`nop-testing` 匹配「矩阵表驱动测试 + 既有 11 个 state-guard + scheduling/capacity 集成测试回归」。层 2 四方对照引用 `state-machine-business-review-prompt.md` 10 维度（模板步骤 5 标配）。必需输入（owner doc + M0.1 契约 + 既有层 3 基线）已就绪。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（纯后端 Java + 既有 aps-service 测试容器，H2 模拟容量预留 UK）。
- 前置依赖：M0.1 done + M0.2 done + M1.3 done（模板 go）。均已满足。

## Execution Plan

### Phase 1 - ErpApsOperationOrderStateMachine Bean + 注册 + 层 1 矩阵完备性测试

Status: planned
Targets: `module-aps/erp-aps-service/src/main/java/app/erp/aps/service/statemachine/ErpApsOperationOrderStateMachine.java`（新）；`module-aps/erp-aps-service/src/main/resources/_vfs/erp/aps/beans/app-service.beans.xml`（追加 Bean 注册）；`module-aps/erp-aps-service/src/test/java/app/erp/aps/service/statemachine/TestErpApsOperationOrderStateMachineMatrix.java`（新，层 1）
Skill: `nop-backend-dev`（Bean 形状/注册）+ `nop-testing`（层 1 表驱动测试）

- Item Types: `Add | Decision | Proof`
- Prereqs: M0.1 + M0.2 + M1.3 done

- [ ] `Add`：创建 `ErpApsOperationOrderStateMachine`（无状态、不注入 DAO/IBiz/IServiceContext/事务），按契约 §4 + §11.1 步骤 1 实现。矩阵编码**已实现**迁移：
  - 显式动作方法（主路径，集中有守卫的边）：`assertCanStart(PLANNED)`、`assertCanComplete(IN_PROGRESS)`、`assertCanCancel(DRAFT|PLANNED|IN_PROGRESS)`（三源，正向枚举合法来源）、`assertCanRevertToDraft(PLANNED)`（抢单回退路径矩阵权威）；非法来源态抛 common 层码 + `action`/`fromStatus` 元数据。
  - 目标态方法：`startTargetStatus()`→IN_PROGRESS / `completeTargetStatus()`→FINISHED / `cancelTargetStatus()`→CANCELLED / `revertToDraftTargetStatus()`→DRAFT。
  - 终态分类：`isTerminal(FINISHED|CANCELLED)`=true；DRAFT/PLANNED/IN_PROGRESS=false。
  - 只读元数据：`transitions()` 返回不可变快照，**声明全部 7 条已实现边**（DRAFT→PLANNED、DRAFT→CANCELLED、PLANNED→IN_PROGRESS、PLANNED→DRAFT、PLANNED→CANCELLED、IN_PROGRESS→FINISHED、IN_PROGRESS→CANCELLED）——含 DRAFT→PLANNED（排产，引擎驱动无 assertCan 守卫但属已实现边，供可达性分析）与 PLANNED→DRAFT（重排回退）；`terminalStatuses()`（FINISHED, CANCELLED）；`initialStatuses()`（DRAFT）。
  Skill: `nop-backend-dev`
- [ ] `Add`：在 `app-service.beans.xml` 以 `<bean id="app.erp.aps.service.statemachine.ErpApsOperationOrderStateMachine" class="...ErpApsOperationOrderStateMachine"/>` 显式 FQN-id 注册（沿用 cs 试点范式，§11.1 步骤 2；注意 BizModel 是 `ioc:type="@bean:id"` 自动发现，但 StateMachine Bean 须显式 class 注册）。
  Skill: `nop-backend-dev`
- [ ] `Proof`（层 1 矩阵完备性，新增 greenfield 表驱动测试 `TestErpApsOperationOrderStateMachineMatrix`，§11.1 步骤 4）：遍历每个动作的合法/非法来源态——(a) 无重复/冲突边；(b) 从 DRAFT 经声明边可达全部状态（DRAFT→PLANNED→IN_PROGRESS→FINISHED；DRAFT/PLANNED/IN_PROGRESS→CANCELLED；PLANNED→DRAFT 回退环）；(c) cancel 三源 {DRAFT,PLANNED,IN_PROGRESS} 全覆盖、对终态 FINISHED/CANCELLED 非法；(d) `transitions()` 元数据（7 条边）与显式方法 + owner doc §2 一致；(e) 终态/初始态集合正确。**不经 BizModel 入口**（层 1 只测 Bean）。Skill: `nop-testing`

Exit Criteria:

- [ ] `ErpApsOperationOrderStateMachine` 落地（4 assertCan 动作 + 目标态 + isTerminal + transitions 元数据 7 边），无状态（grep 证实不 import DAO/IBiz/IServiceContext/事务）。
- [ ] Bean 已在 `app-service.beans.xml` 显式注册（FQN id）；Bean 自身无 `@Inject`（严格无状态），BizModel/Processor 接线点的 `@Inject` 字段非 private（合规 R5）。
- [ ] 层 1 矩阵测试 `mvn test -pl module-aps/erp-aps-service -Dtest=TestErpApsOperationOrderStateMachineMatrix` 全绿，覆盖上述 (a)-(e)。
- [ ] 本地化编译检查：`mvn compile -pl module-aps/erp-aps-service -am` 通过（解除 Phase 2 接线依赖）。

### Phase 2 - BizModel/Processor 接线（行为保持）+ 层 3 回归

Status: planned
Targets: `ErpApsOperationOrderBizModel.java`（start/complete/cancel）、`ErpApsSchedulingInsertRushOrderProcessor.java`（抢单回退循环 PLANNED→DRAFT）
Skill: `nop-backend-dev`（接线 + 错误码映射）+ `nop-testing`（回归断言）

- Item Types: `Fix | Proof`
- Prereqs: Phase 1

- [ ] `Fix`：BizModel 注入 `ErpApsOperationOrderStateMachine`（按类型注入，字段非 private），将 start/complete/cancel 的内联 `Objects.equals` 守卫（`:128,:143,:159-161`）替换为 `stateMachine.assertCanStart/Complete/Cancel(from)`，目标态写回改 `stateMachine.<action>TargetStatus()`；cancel 三源改 `stateMachine.assertCanCancel(from)`（Bean 内部判定 {DRAFT,PLANNED,IN_PROGRESS}）。BizModel 捕获 Bean 的 common 层非法边报告，映射为领域 `ERR_APS_OP_ILLEGAL_TRANSITION`（保留 operationOrderCode/currentStatus/expectedStatus 参数，common 码作 cause——对齐契约 §7 + M1.1 Option A 范式）。**动态业务守卫保留原位**：requireOrder 实体加载、乐观锁。**不得新增** start/complete/cancel 的容量预留释放（Deferred P1-MA2-077 MR1）。Skill: `nop-backend-dev`
- [ ] `Fix`：`ErpApsSchedulingInsertRushOrderProcessor` 注入 `ErpApsOperationOrderStateMachine`，在抢单回退循环（`:73-81`）所选 PLANNED op `setStatus(DRAFT) :77` 前调 `stateMachine.assertCanRevertToDraft(op.getStatus())`（矩阵权威——所选 op 本为 PLANNED，调用确认矩阵合法性）；**IN_PROGRESS 不可重排硬守卫 `:53-59`（`ERR_APS_OP_IN_PROGRESS_NOT_RESCHEDULABLE`）保留在 Processor 选择之前不动**（动态业务守卫，非纯状态迁移守卫）；保留 `releaseReservationsByOrder :76` + 清 plannedStart/End `:78-79` + saveOrUpdate `:80` + 优先级选择 `:64-72`。Processor 捕获 Bean common 码 → 领域码映射。Skill: `nop-backend-dev`
- [ ] `Proof`（层 3 既有回归保持全绿）：`mvn test -pl module-aps/erp-aps-service` 全绿——重点 `TestErpApsOperationOrderStateGuards`（11 @Test：start/complete/cancel happy + 非法路径）、`TestErpApsSchedulingEngine`（前向/后向/抢单/CTP 影子）、`TestErpApsCapacityReservation`（TOCTOU UK + pre-check + 释放）、`TestErpApsOperationOrderCrudSmoke`、`TestErpApsScheduleManagement`、`TestErpApsCrossDomainIntegration`、`TestErpApsDemandPlanning`。证明错误码 + 参数、start 仅 PLANNED、complete 仅 IN_PROGRESS、cancel 三源、IN_PROGRESS 不可重排、容量预留获取/释放时序、抢单优先级规则均不变。若既有测试因 helper 调整需微调断言，仅调整与矩阵无关部分并记录理由（不得弱化断言）。Skill: `nop-testing`

Exit Criteria:

- [ ] 四处固定来源态/目标态判断（BizModel start/complete/cancel 3 + InsertRushOrderProcessor 回退 1）均改调 Bean，grep 证实相关方法体内不再有内联 `Objects.equals(*, OP_STATUS_*)` 矩阵判断（动态守卫 IN_PROGRESS-not-reschedulable、优先级选择、预留获取/释放除外）。
- [ ] `ERR_APS_OP_ILLEGAL_TRANSITION` + 参数（operationOrderCode/currentStatus/expectedStatus）对外不变（层 3 断言证实）；start/complete/cancel 守卫、cancel 三源、IN_PROGRESS 不可重排行为不变；容量预留获取/释放时序不变。
- [ ] 层 3 `mvn test -pl module-aps/erp-aps-service` 全绿。

### Phase 3 - 层 2 四方对照（dict ↔ owner-doc ↔ 元数据 ↔ writer）+ Delta 适用性

Status: planned
Targets: 四方对照审计记录（写入本计划 Closure 段）；OperationOrder 单轴 Delta 证据
Skill: `state-machine-business-review-prompt.md`（四方对照 + 10 维度）+ `nop-testing`（Delta 双加载）

- Item Types: `Proof | Decision | Add`
- Prereqs: Phase 2

- [ ] `Proof`（四方对照，§11.1 步骤 5）：以 `state-machine-business-review-prompt.md` 10 维度审查 OperationOrder 单轴——
  - **dict ↔ 元数据**：dict 5 值 ↔ Bean `transitions()` 7 条边覆盖；每个 dict 值 writer 可达性（含 CRUD 路径，M0.1 §9.4；DRAFT/PLANNED 经引擎 + CRUD + 抢单回退、IN_PROGRESS/FINISHED/CANCELLED 经命名动作）。
  - **owner-doc 迁移图 ↔ 元数据**：`aps/state-machine.md §2 :21-42` 迁移图（7 条边）↔ Bean 边覆盖；owner doc 采用 10 维度审查结构（无独立「迁移表/实现约定」分节），§11.4「§迁移表 vs §实现约定 内部漂移」警示结构不直接适用——重点核对 §2 声明边与 §3 终态/§4 异常路径（预留释放 Deferred `:58`）的一致性。
  - **元数据 ↔ 全部 writer**：盘点 `ErpApsOperationOrder.status` 全部写路径——生产命名动作（BizModel start/complete/cancel）+ 引擎算法（SchedulingEngine DRAFT↔PLANNED）+ 抢单回退（InsertRushOrderProcessor PLANNED→DRAFT）+ CTP 影子（非持久化）+ 框架入口（`__save`/`save`，xmeta `status` insertable/updatable）+ 测试 fixture。
  - **可达性/终态/异常路径**：从 DRAFT 经声明边可达性、终态 FINISHED/CANCELLED 无出边、IN_PROGRESS 不可重排异常路径、容量预留 UK 冲突路径与 owner doc §3-§5 一致。
  Skill: `state-machine-business-review-prompt.md`
- [ ] `Decision`（漂移/边界裁定，路线图规则 5）：
  - **引擎算法写 status 不在 Bean 治理范围**：`ErpApsSchedulingEngine` 的 DRAFT↔PLANNED 写是纯算法按可行性写状态（无 status 守卫可集中），调用方 `SchedulingProcessor.persist` 的预留获取/释放是强一致约束（保留不动）。Bean `transitions()` 声明 DRAFT→PLANNED 边供可达性分析，但不路由引擎写经 Bean（无可集中守卫）——登记为 intentional architecture boundary（引擎=算法状态，Bean=命名动作矩阵）。
  - **预留释放 Deferred**：start/complete/cancel 不释放预留（owner doc §4 `:58` P1-MA2-077 MR1），仅 PLANNED→DRAFT 抢单回退释放——本计划保持此行为，不实现 Deferred 项。登记 watch-only residual（successor = P1-MA2-077 MR1 落地时）。
  - **IN_PROGRESS cancel 审批工作流 Deferred**（§6 `:77-79` P1-MA2-078）：今日 cancel 仅经 entry-permission overlay——本计划保持。登记 watch-only residual。
  - 若层 2 实仓证据发现任何 dict 死状态、owner-doc↔实现 漂移或非法边，按 Fix/Decision 登记 + successor（禁止静默排除）。
  Skill: `state-machine-business-review-prompt.md`
- [ ] `Add | Proof`（Delta 适用性，§11.1 步骤 7；M2 非保护域可选证 Delta）：经 VFS Delta 层同名 bean id 覆盖证明替换生效——派生类覆盖一个 `assertCan<Action>`（如收紧 cancel 仅 DRAFT/PLANNED，移除 IN_PROGRESS 异常终止源），基线/Delta 双加载可区分（复用 M1.2 范式：`TestErpApsOperationOrderStateMachineBaselineIoC` + `TestErpApsOperationOrderStateMachineDeltaOverride`）。Skill: `nop-testing`

Exit Criteria:

- [ ] 四方对照审计记录存在且非空，每维有可追溯结论（引用 Bean 元数据 / owner doc 章节 / dict 位置 / writer 类:行）。
- [ ] 引擎边界、预留释放 Deferred、IN_PROGRESS cancel 审批 Deferred 均已按 Decision 登记 + successor，无静默排除；任何死状态/漂移（若发现）已 Fix 登记。
- [ ] Delta 双加载运行时证据存在（非静态检查），基线/Delta 可区分。

## Draft Review Record

- Independent draft review iteration 1: `accept`（`ses_009c4a759ffeKkpvllK1lrC7ER`）——无 BLOCKER、无 MAJOR。全部 load-bearing 声明经独立复核 CONFIRMED TRUE（owner doc 142 行 10 维度结构 + 7 边含 PLANNED→DRAFT 回退 + §4 `:58`/§6 `:77-79` Deferred 项、dict 5 值 + 容量预留 UK `:333`、BizModel start/complete/cancel 守卫与 setStatus 行号 + 无 private illegalTransition helper、Engine 纯 POJO DRAFT↔PLANNED 算法写无守卫、抢单 Processor 回退 `:73-81` + IN_PROGRESS 硬守卫 `:53-59` + 优先级选择 `:64-72`、SchedulingProcessor 预留获取/释放、错误码、common 码、层 3 基线 11 @Test + 无 TestErpAps*StateMachine、Bean 注册范式 + BizModel 自动发现、greenfield、R5=0/R11=0）。**引擎边界 Decision 经独立审查裁定可辩护**：引擎 DRAFT↔PLANNED 写无 status 守卫可集中，transitions() 声明 DRAFT→PLANNED 边仅供可达性分析、§4.1/§4.2 不要求每条声明边配 assertCan 方法——非 under-scope。回退路径接线经审查裁定行为保持（IN_PROGRESS 硬守卫在选择之前保留，Bean assertCanRevertToDraft 对所选 PLANNED op 为矩阵权威确认）。Deferred 项（预留释放 P1-MA2-077 MR1、cancel 审批 P1-MA2-078）保持不动经审查确认。2 MINOR（SchedulingProcessor.persist 行范围 `:126-137` 非 `:126-135`、`updateSchedule :247` 第 4 处 ERR_APS_OP_ILLEGAL_TRANSITION 为参数校验非状态守卫已正确在范围外但可补注）均非阻塞、为执行期精度修正。反松弛扫描 clean。草案审查收敛，Plan Status → active。

## Closure Gates

> 本计划含生产代码变更（新增 Bean + 接线 + 测试），Closure Gates 运行完整仓库验证。验证命令见 `docs/context/project-context.md`。

- [ ] 范围内行为完成（Bean + 接线 + 层 1 矩阵 + 层 3 回归 + 层 2 四方对照 + Delta 证据）
- [ ] 相关文档对齐（路线图 M2.13 done；owner doc 若有漂移补注）
- [ ] 已运行验证：`mvn clean install -DskipTests`（全仓库 BUILD SUCCESS）+ `mvn test -pl module-aps/erp-aps-service`（全绿）+ `bash docs/audits/nop-compliance-checker.sh`（exit 0，R5=0/R11=0 无漂移）
- [ ] 无范围内项目降级为 deferred/follow-up（漂移裁定必须落地登记 + successor，不得悬置）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：Plan Status、各 Phase Status、Exit Criteria、Closure Gates、日志一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### 容量预留释放（start/complete/cancel 路径）

- Classification: `watch-only residual`
- Why Not Blocking Closure: owner doc §4 `:58` 明示 PLANNED→IN_PROGRESS/FINISHED/CANCELLED 状态翻转的预留释放归 P1-MA2-077 MR1（Deferred）。今日仅 PLANNED→DRAFT 抢单回退释放。本计划保持行为，不实现 Deferred 项。
- Successor Required: yes（触发条件 = P1-MA2-077 MR1 容量预留释放落地时）

### IN_PROGRESS cancel 审批工作流

- Classification: `watch-only residual`
- Why Not Blocking Closure: owner doc §6 `:77-79` 明示 P1-MA2-078 Deferred（需生产主管审批）。今日 cancel 仅经 entry-permission overlay。
- Successor Required: yes（触发条件 = P1-MA2-078 审批工作流落地时）

### 引擎算法写 status 经 Bean 路由

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: `ErpApsSchedulingEngine` 是纯算法 POJO，DRAFT↔PLANNED 写按可行性（无 status 守卫可集中）；路由经 Bean 会破坏算法纯函数性且无可集中守卫。Bean `transitions()` 声明 DRAFT→PLANNED 边仅供可达性分析。
- Successor Required: no（架构边界裁决，非待实现项；若未来引擎改为经命名 mutation 驱动排产则重评）

## Closure

Status Note: <待执行 + 独立结束审计后填写>

Closure Audit Evidence:

- Auditor / Agent: <独立结束审计子代理（新会话）>
- Evidence: <task id / 测试结果 / 四方对照记录>

Follow-up:

- <仅非阻塞跟进项目；已确认的缺陷不得出现在此处>
