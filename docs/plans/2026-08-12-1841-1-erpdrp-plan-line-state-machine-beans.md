# 2026-08-12-1841-1-erpdrp-plan-line-state-machine-beans 分销 DrpPlan + DrpLine 实体级状态机 Bean（M2.14 + M2.15）

> Plan Status: completed
> Last Reviewed: 2026-08-12
> Source: `docs/backlog/entity-state-machine-migration-roadmap.md` M2.14（todo）+ M2.15（todo）
> Related: 前置 `2026-08-12-0617-1-entity-state-machine-m0-1-contract.md`（M0.1 done）+ `2026-08-12-0617-2-entity-state-machine-m0-2-inventory.md`（M0.2 done）+ `2026-08-12-0738-1-cs-ticket-state-machine-bean-pilot.md`（M1.1 范式）+ `2026-08-12-0738-2-cs-ticket-state-machine-pilot-evaluation.md`（M1.3 模板 done）；姊妹范式 `2026-08-12-1118-2-erpprj-task-project-state-machine-beans.md`（同 owner doc 双轴 bundling 范本）
> Mission: entity-state-machine
> Work Item: M2.14 + M2.15
> Audit: required

## Current Baseline

- **M0.1 契约 + M1.3 模板已就绪**（`docs/architecture/entity-state-machine-bean.md` §1-§11）。M1.3 已裁定 **go**，M2 各项 Deps（M1.3）门控解除；`todo → ready` 仍需独立 plan 草案审查（路线图规则 1）。
- **本计划按规则 14 将同组件（同 owner doc `docs/design/drp/state-machine.md`、同结果表面「DRP 域生命周期 StateMachine Bean」、同验证路径）的两条状态轴合并为一个计划的两个阶段**：Plan（§1）+ Line（§2）。二者均 M2 简单生命周期（§11.2），非保护域、无审批子矩阵、无跨域过账副作用（DRP 计划头/行状态本身不触发业财过账；补货单生成经 TransferOrder/PurchaseOrder 是独立实体的独立轴）。
- **计划头（ErpDrpPlan.status）语义**（owner doc `state-machine.md` §1 §3）：4 态 DRAFT/COMPUTED/APPROVED/EXECUTED；EXECUTED 终态；APPROVED 经 `resetToDraft` 可回退 DRAFT（owner doc §3 `:42`「APPROVED 可直接回退到 DRAFT」）——即 APPROVED 是**可逆中间态**（非终态，有出边）。命名动作：runDrp(DRAFT→COMPUTED)、approvePlan(COMPUTED→APPROVED)、resetToDraft(COMPUTED|APPROVED→DRAFT，多源)、APPROVED→EXECUTED（经 `DrpReleaseService.advancePlanToExecutedIfComplete` 自动推进，无独立命名动作 mutation）。**计划头无 cancel 动作**（owner doc 无此边）。
- **明细行（ErpDrpLine.status）语义**（owner doc `state-machine.md` §2）：4 态 SUGGESTED/APPROVED/ORDERED/CANCELLED；终态 = ORDERED/CANCELLED。命名动作：approveLine(SUGGESTED→APPROVED)、releaseLine(APPROVED→ORDERED)、cancelLine/rejectLine(SUGGESTED|APPROVED→CANCELLED，多源)。
- **dict 实况（无死状态）**：`erp-drp/drp-plan-status`（`module-drp/model/app-erp-drp.orm.xml:8-13`）= DRAFT/COMPUTED/APPROVED/EXECUTED 4 值；`erp-drp/drp-line-status`（`:14-19`）= SUGGESTED/APPROVED/ORDERED/CANCELLED 4 值。两 dict 全部值均有对应 writer（layer-2 复核）。
- **生产 writer 实况（固定迁移判断散布，已核实）**：
  - **Plan**：`DrpEngine.runDrp`（`drp/DrpEngine.java:63-68` 守卫 `!Objects.equals(status, DRAFT)`→COMPUTED `:114`）；`DrpEngine.resetToDraft`（`drp/DrpEngine.java:125-132` 多源守卫 `status∈{COMPUTED, APPROVED}`→DRAFT `:135`）；`ErpDrpPlanApprovePlanProcessor.approvePlan`（`processor/ErpDrpPlanApprovePlanProcessor.java:31-36` 守卫 `!Objects.equals(status, COMPUTED)`→APPROVED `:45`，并级联 SUGGESTED→APPROVED 行 `:38-44`）；`DrpReleaseService.advancePlanToExecutedIfComplete`（`drp/DrpReleaseService.java:115-132` **无显式 status 守卫**，仅隐式「全部行终态」门控→EXECUTED `:132`）。
  - **Line**：`DrpEngine.runDrp`（新建行→SUGGESTED `:103`，引擎入口已守卫 Plan=DRAFT）；`ErpDrpLineBizModel.approveLine`（`entity/ErpDrpLineBizModel.java:67-77` **INLINE BizModel** 守卫 `:69 !Objects.equals(status, SUGGESTED)`→APPROVED `:74`，DRP 域唯一仍 inline 的状态写 mutation）；`DrpReleaseService.releaseLine`（`drp/DrpReleaseService.java:144-149` 守卫 ORDERED→幂等码 `ERR_DRP_LINE_ALREADY_ORDERED`、`status != APPROVED`→`ERR_DRP_LINE_NOT_SUGGESTED`→ORDERED `:88`）；`ErpDrpLineCancelLineProcessor`/`ErpDrpLineRejectLineProcessor`（`processor/`，两者 `doCancel` `:34-40` **逐字节相同**守卫 `status∈{ORDERED, CANCELLED}` 拒绝→CANCELLED `:40`，按各自 JavaDoc 自包含副本约定）。
  - **无 `AbstractCancelProcessor` 骨架**（DRP 不复用 purchase 的共享骨架；Plan 无 cancel；Line cancel 经 per-mutation Processor）。**无私有 `illegalTransition(...)` helper**——每处内联 `new NopException(...)` 构造。
- **关键漂移（layer-2 须裁定）**：
  - **D-DRP-1 owner doc APPROVED 终态标注漂移**：`state-machine.md` **多处**将 APPROVED 与终态并列——§1 值表（`:15`）标注「终态」、§3（`:41`）「终态：已批准（APPROVED）、已执行（EXECUTED）」；但 §3（`:42`）「APPROVED 可直接回退到 DRAFT（人工撤回）」与代码（`DrpEngine.resetToDraft:125-126` 接受 APPROVED 为合法来源）一致表明 APPROVED **有出边、非终态**。即 **§3:41 与 §3:42 自相矛盾**，且 §1:15/§3:41 均与「APPROVED 可回退」事实冲突。裁定 = doc drift → Fix owner doc **§1:15 + §3:41**（将 APPROVED 移出终态标注/列举，补注「APPROVED 非终态，可经 resetToDraft 回退 DRAFT」），与 §3:42 + 代码对齐。
  - **D-DRP-2 owner doc §2 迁移表不完整**：§2 迁移表（`:32-37`）仅列 `resetToDraft` 来源 COMPUTED，但 §3（`:42`）与代码（`DrpEngine.java:125-126`）均接受 COMPUTED **或 APPROVED**。裁定 = doc drift → Fix owner doc §2 补 APPROVED 来源行。
- **错误码**：`ErpDrpErrors.ERR_DRP_PLAN_ILLEGAL_TRANSITION`（`ErpDrpErrors.java:30-33`，参数 planCode/currentStatus/**expectedStatus**）；`ERR_DRP_LINE_ILLEGAL_TRANSITION`（`:120-123`，参数 drpLineId/currentStatus，**无 expectedStatus**）——参数形状不对称（见 Decision）。common 层码 `ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION`（参数 currentStatus/expectedStatus）已存在（M1.1 Option A 复用 + `action` 补充参数范式）。另：`ERR_DRP_LINE_NOT_SUGGESTED`（`:35-38`）实为「releaseLine 非 APPROVED」语义（误名，pre-existing），`ERR_DRP_LINE_ALREADY_ORDERED`（`:55-58`）幂等码——二者均保留原位，不在本重构重命名（重命名=公共错误码契约变更）。
- **生产 Bean 注册范式已存在**：`module-drp/erp-drp-service/src/main/resources/_vfs/erp/drp/beans/app-service.beans.xml:38-61` 已以 `<bean id="<FQN>" class="<FQN>"/>` 注册 per-mutation Processor。StateMachine Bean 沿用此范式。
- **既有层 3 回归基线（非 greenfield）**：`TestErpDrpEngine`（happy path DRAFT→COMPUTED→APPROVED→ORDERED→EXECUTED + `testRunDrpRejectsNonDraftPlan` 断言 `ERR_DRP_PLAN_ILLEGAL_TRANSITION` + 幂等拒绝）、`TestErpDrpScheduleRelease`、`TestErpDrpPlanCrudSmoke` 等。M0.1 §10 登记的基线不含 drp——drp 域层 3 = 上述既有集成测试，**不是**命名矩阵测试（层 1 新增 greenfield）。**已知覆盖缺口**（层 1 矩阵将补，层 3 不重建）：cancelLine/rejectLine、inline approveLine、APPROVED→DRAFT reset 路径当前未被层 3 直接覆盖。
- **合规基线**：R5（`@Inject private`）= 0（已核实 module-drp service 零违例）、R11（Processor 重复状态判断方法）= 0。本计划新增 2 Bean 注册 + 注入须保持 R5=0；接线后内联守卫收敛至 Bean，R11 不增。

## Goals

- 落地 `ErpDrpPlanStateMachine`（4 态计划头轴）+ `ErpDrpLineStateMachine`（4 态明细行轴）两个独立 Bean，各承载**已实现**迁移矩阵 + 终态/初始态分类 + 只读 `transitions()` 元数据，严格无状态、可经 Delta 同名覆盖。
- 将 `DrpEngine`（runDrp/resetToDraft）、`ErpDrpPlanApprovePlanProcessor`（approvePlan）、`DrpReleaseService`（releaseLine/advancePlanToExecutedIfComplete）、`ErpDrpLineBizModel.approveLine`（INLINE）、`ErpDrpLineCancelLineProcessor`/`ErpDrpLineRejectLineProcessor`（cancel/reject）的**固定来源态/目标态判断**改调 Bean；**动态业务守卫保留原位**（runDrp 净需求公式与仓库补货参数、releaseLine 的 TRANSFER 需 sourceWh/PURCHASE 需 supplier 类型守卫、advancePlanToExecutedIfComplete 的「全部行终态」隐式门控、approvedQty 回填、乐观锁）。
- 保持全部既有外部行为不变（错误码 + 参数形状、cancel/reject 多源、resetToDraft 多源、releaseLine 幂等码 `ERR_DRP_LINE_ALREADY_ORDERED`、advancePlan 隐式门控语义）。
- 各新增层 1 矩阵完备性表驱动测试（greenfield，不经 BizModel 入口）；层 3 既有集成测试回归全绿。
- 层 2 四方对照（dict ↔ `drp/state-machine.md` ↔ Bean 元数据 ↔ 全部 writer 含 CRUD 路径）双轴裁定，**显式处置 D-DRP-1/D-DRP-2 owner doc 内部漂移**（Fix owner doc），禁止静默排除。

## Non-Goals

- 不修改任何 `model/*.orm.xml` / `model/*.api.xml` / 字典 yaml（路线图 Non-Goal）：**不向 dict 新增/删除值**（4+4 值不变）。
- 不改变任何业务状态值、动作名、错误码值/**错误码参数形状**、权限、净需求公式、补货单生成时序（路线图 Non-Goal「不借迁移改变既有行为」）。
- **不重命名 `ERR_DRP_LINE_NOT_SUGGESTED`**（误名，实为 releaseLine 非 APPROVED）——重命名为公共错误码契约变更，归 successor；也不为 `ERR_DRP_LINE_ILLEGAL_TRANSITION` 新增 `expectedStatus` 参数（公共契约变更，见 Decision D-DRP-3）。
- 不新增 `ErpDrpLineApproveLineProcessor`（将 inline `approveLine` 提取为 Processor）——保持 INLINE 接线范式（参照 purchase Quotation/Rfq INLINE 先例），Bean 直接注入 BizModel。
- 不合并 `ErpDrpLineCancelLineProcessor` 与 `ErpDrpLineRejectLineProcessor`（按 JavaDoc 自包含副本约定，二者语义相同但刻意分离；接线后各自委托 Bean，不合并类）。
- 不迁移 DRP 仿真域（`simulation/SimulationDrpEngine` 的 Scenario/Version 状态轴，非 M2.14/M2.15 结果表面）。
- 不引入全局 CRUD 写锁（M0.1 §9 选项 c；更强写锁 successor）。
- 不声称全域 Delta 覆盖已验证（本计划证 DRP 单域 Delta，全域回归归 M5.3）。

## Task Route

- Type: `implementation-only change`（消费 M0.1 契约 + M1.3 模板 + M0.2 清单，落地两轴 Bean + 接线 + 三层测试 + 四方对照；不改契约/模型/公共 API）
- Owner Docs: `docs/architecture/entity-state-machine-bean.md`（M0.1 契约 + §11 迁移模板）、`docs/design/drp/state-machine.md`（§1 Plan + §2 Line + §3 终态/回退语义）、`docs/design/drp/README.md`（状态概述 `:40`）、`docs/analysis/2026-08-12-entity-state-axis-inventory.md`（M0.2 清单 drp 行）、`docs/architecture/processor-extension-pattern.md`
- Skill Selection Basis: 路线图 M2.14/M2.15 指定 `nop-backend-dev` + `nop-testing`。`nop-backend-dev` 匹配「BizModel/Processor/Engine 接线、Bean 注册、动态守卫边界保留、错误码映射」；`nop-testing` 匹配「层 1 表驱动矩阵测试 + 既有集成测试回归 + Delta 双加载」。层 2 引用 `state-machine-business-review-prompt.md`（模板步骤 5 标配）。必需输入已就绪。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（纯后端 Java + 既有 drp-service 测试容器）。
- 前置依赖：M0.1 + M0.2 + M1.3 done。均已满足。
- 无 data-deletion / 财务过账 / ORM 保护区域触发（DRP 计划头/行状态本身不触发过账；补货单生成经独立实体）。

## Execution Plan

### Phase 1 - ErpDrpPlanStateMachine + ErpDrpLineStateMachine Bean + 注册 + 层 1 矩阵完备性测试

Status: completed
Targets: `module-drp/erp-drp-service/src/main/java/app/erp/drp/service/statemachine/ErpDrpPlanStateMachine.java`（新）+ `ErpDrpLineStateMachine.java`（新）；`.../beans/app-service.beans.xml`（追加 2 Bean 注册）；`TestErpDrpPlanStateMachineMatrix.java` + `TestErpDrpLineStateMachineMatrix.java`（新，层 1）
Skill: `nop-backend-dev`（Bean 形状/注册）+ `nop-testing`（层 1 表驱动测试）

- Item Types: `Add | Decision | Proof`
- Prereqs: M0.1 + M0.2 + M1.3 done

- [x] `Add`：创建 `ErpDrpPlanStateMachine`（无状态，不注入 DAO/IBiz/IServiceContext/事务），矩阵编码**已实现**迁移：`assertCanRunDrp(DRAFT)`→COMPUTED、`assertCanApprovePlan(COMPUTED)`→APPROVED、`assertCanResetToDraft(COMPUTED|APPROVED)`（多源）→DRAFT、`assertCanAdvanceToExecuted(APPROVED)`→EXECUTED；目标态方法；`isTerminal(EXECUTED)`（**APPROVED 非终态**——有 resetToDraft 出边，对应 D-DRP-1 裁定）；`transitions()` 返回 **5 条边**（按 per-(action, fromStatus, toStatus) 三元组：runDrp DRAFT→COMPUTED、approvePlan COMPUTED→APPROVED、resetToDraft COMPUTED→DRAFT、resetToDraft APPROVED→DRAFT、advanceToExecuted APPROVED→EXECUTED）；`terminalStatuses()`(EXECUTED) + `initialStatuses()`(DRAFT)。非法来源态抛 common 层码 + `action`/`fromStatus` 元数据。
      - Skill: `nop-backend-dev`
- [x] `Add`：创建 `ErpDrpLineStateMachine`（无状态），矩阵：`assertCanApproveLine(SUGGESTED)`→APPROVED、`assertCanRelease(APPROVED)`→ORDERED、`assertCanCancel(SUGGESTED|APPROVED)`（多源）→CANCELLED；目标态方法；`isTerminal(ORDERED|CANCELLED)`；`transitions()` 返回 **4 条边**（按 per-(action, fromStatus, toStatus) 三元组：approveLine SUGGESTED→APPROVED、releaseLine APPROVED→ORDERED、cancel SUGGESTED→CANCELLED、cancel APPROVED→CANCELLED）；`terminalStatuses()`(ORDERED/CANCELLED) + `initialStatuses()`(SUGGESTED)。非法来源态抛 common 层码 + `action`/`fromStatus` 元数据。
      - Skill: `nop-backend-dev`
- [x] `Decision`（D-DRP-3 错误码参数形状不对称）：`ERR_DRP_LINE_ILLEGAL_TRANSITION` 现无 `expectedStatus` 参数。**裁定：保持现状不对称**（不为该公共领域码新增参数——属公共错误码契约变更，超出重构范围）。Bean 内部非法边抛 common 层码（携带 currentStatus/expectedStatus/action），`ErpDrpLineCancelLineProcessor`/`ErpDrpLineRejectLineProcessor`/`ErpDrpLineBizModel.approveLine` 捕获后映射既有 `ERR_DRP_LINE_ILLEGAL_TRANSITION`（仅 drpLineId/currentStatus，common 码作 cause）。Plan 轴沿用既有 `ERR_DRP_PLAN_ILLEGAL_TRANSITION`（已含 expectedStatus，形状不变）。理由记录于本计划。
      - Skill: `nop-backend-dev`
- [x] `Add`：在 `app-service.beans.xml` 以 FQN id 注册两个 Bean（沿用既有 Processor 范式，§11.1 步骤 2）。
      - Skill: `nop-backend-dev`
- [x] `Proof`（层 1 矩阵完备性，新增 greenfield 表驱动测试，§11.1 步骤 4，不经 BizModel 入口）：
      - `TestErpDrpPlanStateMachineMatrix`：(a) 无重复/冲突边；(b) 从 DRAFT 可达 COMPUTED/APPROVED/EXECUTED，APPROVED 经 resetToDraft 可回 DRAFT；(c) resetToDraft 多源 {COMPUTED, APPROVED} 合法、对 EXECUTED 非法；(d) EXECUTED 终态无出边；**APPROVED 非终态**（有出边，断言 `isTerminal(APPROVED)==false`）；(e) `transitions()` 元数据一致；(f) 初始/终态集合正确。
      - `TestErpDrpLineStateMachineMatrix`：(a) 无重复/冲突边；(b) 从 SUGGESTED 可达 APPROVED/ORDERED/CANCELLED；(c) cancel 多源 {SUGGESTED, APPROVED} 合法、对终态 {ORDERED, CANCELLED} 非法；(d) ORDERED/CANCELLED 终态无出边；(e) `transitions()` 一致；(f) 初始/终态集合正确。
      - Skill: `nop-testing`

Exit Criteria:

- [x] 两 Bean 落地（Plan 4 动作 + Line 3 动作 + 目标态 + isTerminal + transitions 元数据），无状态（grep 证实不 import DAO/IBiz/IServiceContext/事务）。
- [x] 两 Bean 已在 `app-service.beans.xml` 注册（FQN id）；`@Inject` 字段非 private（合规 R5）。
- [x] 层 1 矩阵测试 `mvn test -pl module-drp/erp-drp-service -Dtest=TestErpDrpPlanStateMachineMatrix,TestErpDrpLineStateMachineMatrix` 全绿。
- [x] 本地化编译检查：`mvn compile -pl module-drp/erp-drp-service -am` 通过（解除 Phase 2 接线依赖）。

### Phase 2 - Engine/Processor/BizModel 接线（行为保持）+ 层 3 回归

Status: completed
Targets: `drp/DrpEngine.java`（runDrp/resetToDraft）、`processor/ErpDrpPlanApprovePlanProcessor.java`（approvePlan + 行级联）、`drp/DrpReleaseService.java`（releaseLine/advancePlanToExecutedIfComplete）、`entity/ErpDrpLineBizModel.java`（approveLine INLINE）、`processor/ErpDrpLineCancelLineProcessor.java`、`processor/ErpDrpLineRejectLineProcessor.java`
Skill: `nop-backend-dev`（接线 + 错误码映射）+ `nop-testing`（回归断言）

- Item Types: `Fix | Proof`
- Prereqs: Phase 1

- [x] `Fix`：`DrpEngine` 注入 `ErpDrpPlanStateMachine`（字段非 private），将 runDrp（`:63-68`）与 resetToDraft（`:125-132`）的内联守卫替换为 `stateMachine.assertCanRunDrp(from)`/`assertCanResetToDraft(from)` + 目标态写回（`runDrpTargetStatus()`/`resetToDraftTargetStatus()`）。**动态守卫保留原位**：净需求公式、仓库补货参数校验、resetToDraft 的 `clearSuggestedLines`（清行副作用 `:134`）+ totalReplenishmentQty 清零（`:136`）。Engine 抛领域 `ERR_DRP_PLAN_ILLEGAL_TRANSITION`（参数不变，common 码作 cause）。
      - Skill: `nop-backend-dev`
- [x] `Fix`：`ErpDrpPlanApprovePlanProcessor` 注入 `ErpDrpPlanStateMachine`，approvePlan 内联守卫（`:31-36`）替换为 `stateMachine.assertCanApprovePlan(from)` + 目标态写回；**行级联 SUGGESTED→APPROVED（`:38-44`）改逐行调 `lineStateMachine.assertCanApproveLine(from)` + 写回**（行级联是 approvePlan 的副作用，行的状态迁移也经 Line Bean 治理）；approvedQty 回填（`:40-42`）保留。
      - Skill: `nop-backend-dev`
- [x] `Fix`：`DrpReleaseService.releaseLine` 注入 `ErpDrpLineStateMachine`，将私有 helper `requireReleasable`（`:136-151`，被 `releaseLine:62` 调用）中「status != APPROVED」守卫（`:147-149` 抛 `ERR_DRP_LINE_NOT_SUGGESTED`）替换为 `stateMachine.assertCanRelease(from)` + 目标态写回；**幂等码 `ERR_DRP_LINE_ALREADY_ORDERED`（ORDERED→抛，`:144-146`）保留原位**（幂等是独立语义，非矩阵非法边）；**类型守卫保留原位**（TRANSFER 需 sourceWh `:67-70` / PURCHASE 需 supplier `:76-79`）。`advancePlanToExecutedIfComplete`（`:115-132`）注入 Plan Bean 调 `stateMachine.assertCanAdvanceToExecuted(APPROVED)` + 写回 EXECUTED——**注意：当前代码此处无显式 status 检查（仅「全部行终态」隐式门控 `:126-131`），加 Bean 守卫属防御性不变量加强（plan 全行终态时必为 APPROVED，故为 no-op）**，显式记录此加强而非伪装成纯 like-for-like 接线；**隐式「全部行终态」门控保留原位**（动态守卫，非状态轴判断）。
      - Skill: `nop-backend-dev`
- [x] `Fix`：`ErpDrpLineBizModel.approveLine`（INLINE `:67-77`）注入 `ErpDrpLineStateMachine`，内联守卫（`:69`）替换为 `stateMachine.assertCanApproveLine(from)` + 目标态写回（保持 INLINE，不提取 Processor）。
      - Skill: `nop-backend-dev`
- [x] `Fix`：`ErpDrpLineCancelLineProcessor` 与 `ErpDrpLineRejectLineProcessor` 各自注入 `ErpDrpLineStateMachine`，将 `doCancel`（`:34-40`）逐字节相同的终态拒绝守卫替换为 `stateMachine.assertCanCancel(from)` + 目标态写回；两者保持各自独立类（不合并），均映射 `ERR_DRP_LINE_ILLEGAL_TRANSITION`（参数不变）。
      - Skill: `nop-backend-dev`
- [x] `Proof`（补全覆盖缺口的接线回归）：当前层 3 未直接覆盖 cancelLine/rejectLine/inline approveLine/APPROVED→DRAFT reset 接线路径（见 Current Baseline「已知覆盖缺口」）。新增**针对性接线回归**（经 BizModel/IGraphQLEngine 入口，非层 1 Bean 隔离测试）：cancelLine happy（SUGGESTED→CANCELLED + APPROVED→CANCELLED 多源）+ 终态拒绝（ORDERED/CANCELLED→抛 `ERR_DRP_LINE_ILLEGAL_TRANSITION`）、rejectLine 同语义各一例、approveLine（SUGGESTED→APPROVED + 非 SUGGESTED 拒绝抛码）、resetToDraft 从 APPROVED（APPROVED→DRAFT）。证明这些接线点经 Bean 后行为/错误码不变。
      - Skill: `nop-testing`
- [x] `Proof`（层 3 既有回归保持全绿）：`mvn test -pl module-drp/erp-drp-service` 全绿——重点 `TestErpDrpEngine`（happy path DRAFT→COMPUTED→APPROVED→ORDERED→EXECUTED + `testRunDrpRejectsNonDraftPlan` 断言 `ERR_DRP_PLAN_ILLEGAL_TRANSITION` + releaseLine 幂等拒绝）、`TestErpDrpScheduleRelease`、`TestErpDrpPlanCrudSmoke`。证明 happy path 接线、runDrp 拒绝、releaseLine 幂等码、advancePlan 隐式门控不变。**cancelLine/rejectLine/approveLine/APPROVED→DRAFT 接线由上一针对性 Proof 覆盖**（既有层 3 未直接覆盖这些路径，已诚实登记）。
      - Skill: `nop-testing`

Exit Criteria:

- [x] Plan 4 处（DrpEngine runDrp/resetToDraft + ApprovePlanProcessor + ReleaseService.advanceToExecuted）+ Line 5 处（ReleaseService.releaseLine + BizModel.approveLine + CancelProcessor + RejectProcessor + ApprovePlanProcessor 行级联）固定判断均改调 Bean，grep 证实相关方法体内不再有内联 `Objects.equals(status, *_STATUS_*)` 矩阵判断（动态守卫如净需求/类型守卫/「全部行终态」门控/幂等码除外）。
- [x] 错误码 + 参数对外不变（层 3 断言 + 针对性接线回归证实）；`ERR_DRP_LINE_NOT_SUGGESTED`/`ERR_DRP_LINE_ALREADY_ORDERED` 行为不变。
- [x] 层 3 `mvn test -pl module-drp/erp-drp-service` 全绿；cancelLine/rejectLine/approveLine/APPROVED→DRAFT 针对性接线回归全绿。

### Phase 3 - 层 2 四方对照（Plan + Line 双轴）+ owner doc 漂移 Fix + Delta 适用性

Status: completed
Targets: 四方对照审计记录（写入本计划 Closure 段）；owner doc §1/§2 漂移补正；DRP 单轴 Delta 证据
Skill: `state-machine-business-review-prompt.md`（四方对照 + 10 维度）+ `nop-testing`（Delta 双加载）

- Item Types: `Proof | Fix | Decision | Add`
- Prereqs: Phase 2

- [x] `Proof`（四方对照，§11.1 步骤 5）：以 `state-machine-business-review-prompt.md` 10 维度审查 Plan + Line 双轴——dict（plan-status 4 值 / line-status 4 值）↔ owner doc §1/§2/§3 ↔ 两 Bean `transitions()` ↔ 全部 writer（含 CRUD 路径 §9.4 + simulation writer 排除理由）。确认两 dict 无死状态。
      - Skill: `state-machine-business-review-prompt.md`
- [x] `Fix`（D-DRP-1 owner doc APPROVED 终态标注漂移）：`state-machine.md` **§1 值表（`:15`）+ §3（`:41`）** 两处将 APPROVED 标注/列为终态，与 §3（`:42`）「APPROVED 可回退 DRAFT」及代码（`DrpEngine.java:125-126` 接受 APPROVED）矛盾（§3:41 与 §3:42 自相矛盾）。Fix owner doc **§1:15 + §3:41** 两处：将 APPROVED 移出终态标注/列举，补注「APPROVED 非终态，可经 resetToDraft 回退 DRAFT（见 §3:42）」，与代码对齐。
      - Skill: `state-machine-business-review-prompt.md`
- [x] `Fix`（D-DRP-2 owner doc §2 resetToDraft 来源不完整）：§2 迁移表（`:32-37`）仅列 resetToDraft 来源 COMPUTED，遗漏 APPROVED（§3 `:42` + 代码 `DrpEngine.java:125-126` 均含 APPROVED）。Fix owner doc §2 补 APPROVED 来源行。
      - Skill: `state-machine-business-review-prompt.md`
- [x] `Decision`（漂移裁定汇总）：对四方对照任何其他不一致逐条分类（implementation drift / doc drift / intentional legacy）并指派 successor；已确认缺陷/契约漂移 = Fix。预期两 dict 干净（无死状态）；`ERR_DRP_LINE_NOT_SUGGESTED` 误名登记为 watch-only residual（不重命名，见 Non-Goals）。
      - Skill: `state-machine-business-review-prompt.md`
- [x] `Add | Proof`（Delta 适用性，§11.1 步骤 7；M2 非保护域）：在 Plan 轴证 Delta（派生类覆盖一个动作，如收紧 resetToDraft 仅 COMPUTED，移除 APPROVED 源），VFS Delta 层同名 bean id 覆盖，基线/Delta 双加载可区分（复用 M1.2/contract 范式：`TestErpDrpPlanStateMachineBaselineIoC` + `TestErpDrpPlanStateMachineDeltaOverride`）。Line 轴继承 Plan 轴 + M1.2 既有证明，不重复证。
      - Skill: `nop-testing`

Exit Criteria:

- [x] 双轴四方对照审计记录存在且非空，每维有可追溯结论（引用 Bean 元数据 / owner doc 章节 / dict 位置 / writer 类:行）。
- [x] D-DRP-1/D-DRP-2 owner doc 漂移 Fix 已落地（§1 APPROVED 标注 + §2 resetToDraft 来源）；其他不一致（若有）已按 Fix/Decision 登记 + successor，无静默排除。
- [x] Plan 轴 Delta 双加载运行时证据存在（非静态检查），基线/Delta 可区分。

## Draft Review Record

- Independent draft review iteration 1: `accept` (mission-driver:2026-08-12-111827) — 格式合规（全部必需段落、字段名、Phase 结构、Item 类型有效）；退出标准可测（具体 mvn 命令 + grep 证据 + 行为断言）；范围边界清晰（M2.14+M2.15 按 guide 规则 14 同组件双轴 bundling，Non-Goals 显式排除误名重命名/仿真域/全局写锁）；Closure Gates + Closure 段定义完整证据要求。Baseline 经核实准确：roadmap M2.14/M2.15 均 todo；owner doc D-DRP-1（§1:15 + §3:41 APPROVED 误标终态 vs §3:42 可回退）+ D-DRP-2（§2 迁移表遗漏 APPROVED→DRAFT 来源）漂移判定与实仓库一致。无 Blocker/Major 问题；Minor（advanceToExecuted 防御性守卫加强、错误码参数形状不对称 D-DRP-3）已在计划内显式处置。

## Closure Gates

> 本计划含生产代码变更（2 Bean + 接线 + 测试），Closure Gates 运行完整仓库验证。

- [x] 范围内行为完成（Plan + Line 双轴 Bean + 接线 + 层 1 矩阵 + 层 3 回归 + 层 2 四方对照 + owner doc 漂移 Fix + Delta 证据）
- [x] 相关文档对齐（`drp/state-machine.md` §1/§2 漂移 Fix；路线图 M2.14 + M2.15 done）
- [x] 已运行验证：`mvn clean install -DskipTests`（全仓库 BUILD SUCCESS）+ `mvn test -pl module-drp/erp-drp-service`（全绿）+ `bash docs/audits/nop-compliance-checker.sh`（exit 0，R5=0/R11=0 无漂移）
- [x] 无范围内项目降级为 deferred/follow-up（owner doc 漂移 Fix 须落地，不得悬置）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：Plan Status、各 Phase Status、Exit Criteria、Closure Gates、日志一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此项留为未勾选占位
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### ERR_DRP_LINE_NOT_SUGGESTED 误名

- Classification: `watch-only residual`
- Why Not Blocking Closure: 该码（`ErpDrpErrors.java:35-38`）实为 releaseLine「非 APPROVED」语义，名称误导，但重命名为公共错误码契约变更，超出本重构范围。接线后行为不变（仍由 `DrpReleaseService.releaseLine` 抛出）。
- Successor Required: no（仅当产品要求统一 release 守卫错误码命名时，开独立公共契约变更 plan ask-first）

### ERR_DRP_LINE_ILLEGAL_TRANSITION 参数对称化（新增 expectedStatus）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 该码无 `expectedStatus` 参数（与 Plan 轴不对称）。新增参数为公共错误码契约变更，D-DRP-3 裁定保持现状。Bean 内部经 common 层码携带完整诊断。
- Successor Required: no（仅当产品要求错误诊断对称性时重开）

### DRP 仿真域状态轴（Scenario/Version）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: `simulation/SimulationDrpEngine` 的 Scenario/Version 状态轴非 M2.14/M2.15 结果表面，未纳入 M0.2 清单 drp 迁移行。
- Successor Required: yes（触发条件 = M0.2 清单将仿真轴纳入时）

### 全局 CRUD 写锁

- Classification: `watch-only residual`
- Why Not Blocking Closure: M0.1 §9 裁定选项 (c) 显式排除；更强全局写锁须改 ORM/xmeta（保护区 ask-first），独立 successor。
- Successor Required: no（仅当产品要求全局强制矩阵写锁时重开）

## Closure

Status Note: 执行完成（2026-08-12）。Phase 1-3 全部落地：两轴 StateMachine Bean（Plan 5 边 / Line 4 边）+ 注册 + 层 1 矩阵测试（18 用例全绿）；Engine/Processor/BizModel 接线完成（动态守卫原位保留——clearSuggestedLines 副作用、releaseLine 类型守卫与幂等码、advancePlanToExecuted 隐式「全部行终态」门控），针对性接线回归（9 用例：cancelLine/rejectLine/approveLine/APPROVED→DRAFT）+ 层 3 既有集成测试全绿（70 用例）；层 2 四方对照双轴干净（无死状态）；owner doc 漂移 Fix（D-DRP-1 + D-DRP-2）已落地；Plan 轴 Delta 双加载运行时证据存在（7 用例：基线 4 + Delta 3）。

### 层 2 四方对照审计记录（§11.1 步骤 5，`state-machine-business-review-prompt.md` 10 维度）

> 审查方法：四方对照（dict ↔ owner-doc 迁移图 ↔ StateMachine `transitions()` 元数据 ↔ 全部 writer 含 CRUD 路径 §9.4）+ 10 维度业务审查。

#### Plan 轴四方对照（`erp-drp/drp-plan-status`）

| 源 | 值/边 | 位置 |
|---|---|---|
| dict `erp-drp/drp-plan-status` | DRAFT / COMPUTED / APPROVED / EXECUTED（4 值） | `module-drp/model/app-erp-drp.orm.xml:8-13` |
| owner-doc §适用对象一 §1 值表 + §2 迁移表 + §3 终态 | runDrp(DRAFT→COMPUTED)、approvePlan(COMPUTED→APPROVED)、resetToDraft 多源 {COMPUTED→DRAFT, APPROVED→DRAFT}、advanceToExecuted(APPROVED→EXECUTED) = 5 边；EXECUTED 唯一终态（**APPROVED 非终态**——D-DRP-1 Fix 后 §1:15 + §3:41 已对齐） | `docs/design/drp/state-machine.md:11-44`（D-DRP-1 + D-DRP-2 Fix 后） |
| Bean `transitions()` | 5 边（与上同） | `ErpDrpPlanStateMachine.java:104-117` |
| 生产 writer（命名动作） | runDrp `DrpEngine.java:74-78`（经 `assertCanPlan` helper `:191-207` 调 Bean + 目标态写回 `:111`）、resetToDraft `:122-130`、approvePlan `ErpDrpPlanApprovePlanProcessor.java:42-50`、advanceToExecuted `DrpReleaseService.java:140-145`（防御性不变量加强：全行终态时 plan 必为 APPROVED，加 Bean 守卫为 no-op） | 4 处 |
| CRUD 路径（§9.4） | xmeta `status` insertable/updatable（M0.1 §9.1 全局结论；通用 CRUD 可写状态字段，不在矩阵运行时强制范围，option c 显式排除） | M0.1 §9.2 |
| 仿真 writer 排除理由 | `simulation/SimulationDrpEngine` 写 DRAFT→COMPUTED 是仿真域 Scenario/Version 轴（非 M2.14 结果表面），不纳入 Plan 状态轴治理——见 Deferred But Adjudicated「DRP 仿真域状态轴」 | `SimulationDrpEngine.java:94,146` |

**一致性**：4 dict 值 = 5 owner-doc 边（Fix 后含 APPROVED→DRAFT）= 5 Bean 边 = 4 writer。**无死状态**：每个 dict 值可达——DRAFT 初始、COMPUTED（runDrp 入）、APPROVED（approvePlan 入，**非终态**有 resetToDraft 出边）、EXECUTED（advanceToExecuted 入，终态）。EXECUTED 终态无出边（层 1 矩阵测试 `testTerminalStatusesHaveNoOutgoingEdges` 证实）。

#### Line 轴四方对照（`erp-drp/drp-line-status`）

| 源 | 值/边 | 位置 |
|---|---|---|
| dict `erp-drp/drp-line-status` | SUGGESTED / APPROVED / ORDERED / CANCELLED（4 值） | `module-drp/model/app-erp-drp.orm.xml:14-19` |
| owner-doc §适用对象二 §1 + §2 迁移图 | approveLine(SUGGESTED→APPROVED)、releaseLine(APPROVED→ORDERED)、cancel 多源 {SUGGESTED→CANCELLED, APPROVED→CANCELLED} = 4 边；ORDERED/CANCELLED 终态 | `docs/design/drp/state-machine.md:124-141` |
| Bean `transitions()` | 4 边（与上同） | `ErpDrpLineStateMachine.java:88-96` |
| 生产 writer（命名动作） | approveLine `ErpDrpLineBizModel.java:79-87`（INLINE，经 `assertCanApproveLine` + 目标态写回）、releaseLine `DrpReleaseService.java:141`（requireReleasable `:140-167` 内 Bean 守卫 + 目标态写回）、cancelLine `ErpDrpLineCancelLineProcessor.java:42-50`、rejectLine `ErpDrpLineRejectLineProcessor.java:42-50`（同语义各自独立类，均委托 Bean）+ approvePlan 行级联 `ErpDrpPlanApprovePlanProcessor.java:55-63`（逐行 `assertCanApproveLine` + 写回） | 5 处（cancel/reject 各 1 + approveLine INLINE + releaseLine + 行级联） |
| CRUD 路径（§9.4） | xmeta `status` insertable/updatable（option c 显式排除） | M0.1 §9.2 |
| 仿真 writer 排除理由 | `simulation/SimulationDrpEngine` 写 SUGGESTED 是仿真域副本（非结果表面）——见 Deferred「DRP 仿真域状态轴」 | `SimulationDrpEngine.java:136,218` |

**一致性**：4 dict 值 = 4 owner-doc 边 = 4 Bean 边 = 5 writer（cancel/reject 同语义各自内联副本 + approveLine INLINE + releaseLine + 行级联）。**无死状态**：SUGGESTED 初始、APPROVED（approveLine/approvePlan 级联入）、ORDERED（releaseLine 入，终态）、CANCELLED（cancel/reject 入，终态）。ORDERED/CANCELLED 终态无出边（层 1 矩阵测试 `testTerminalStatusesHaveNoOutgoingEdges` 证实）。

#### 10 维度业务审查（双轴）

- **D1 状态定义**：4+4 状态均清晰表达「等待 X」业务等待点。Plan APPROVED = 等待执行（**非终态**——D-DRP-1 Fix 后 owner doc §1:15 + §3:41 对齐）；Line ORDERED/CANCELLED 终态。
- **D2 迁移完整性**：每状态进/出边齐全（见四方表）。Plan resetToDraft 多源 {COMPUTED, APPROVED}（D-DRP-2 Fix 后 §2:36 含 APPROVED→DRAFT 行）；Line cancel 多源 {SUGGESTED, APPROVED} 在 owner-doc §2:140 + Bean 显式编码 2 来源。
- **D3 终态与恢复**：Plan EXECUTED / Line ORDERED,CANCELLED 无出边。owner-doc §3 明确 EXECUTED 不可直接恢复，重启 = 新建计划（intentional，Bean 如实不编码 reactivation 边）。
- **D4 异常路径**：releaseLine 幂等码 `ERR_DRP_LINE_ALREADY_ORDERED`（ORDERED 重复释放）+ 误名码 `ERR_DRP_LINE_NOT_SUGGESTED`（releaseLine 非 APPROVED，pre-existing 不重命名）+ TRANSFER/PURCHASE 类型守卫 + Plan resetToDraft clearSuggestedLines 副作用 + advancePlanToExecuted「全部行终态」隐式门控 + 乐观锁，均保留原位。
- **D5 可达性**：从初始态（Plan DRAFT / Line SUGGESTED）全部声明状态可达；Plan APPROVED 经 resetToDraft 可回 DRAFT（合法回退）；无不可达状态、无死锁。
- **D6 角色权限**：owner-doc §6 定义执行角色（计划员/计划主管）；Bean 严格无状态不读用户（契约 §2），权限经 xbiz auth/Processor 层（本计划不改权限，超出范围）。
- **D7 外部依赖**：Plan APPROVED→EXECUTED 触发的 TransferOrder/PurchaseOrder 生成经独立实体的独立状态轴（`IErpInvTransferOrderBiz`/`IErpPurOrderBiz`），非 Plan/Line 状态轴触发；Bean 持零跨域耦合。
- **D8 TODO 策略**：owner-doc §8 定义（COMPUTED/APPROVED 产生 TODO），非 Bean 关注。
- **D9 场景演练**：happy path（月度 DRP 计划完整流程）+ COMPUTED 后参数调整重算（COMPUTED→DRAFT）已在 owner-doc §9 走查；APPROVED→DRAFT 回退由 D-DRP-2 Fix 补 §2 迁移表后场景完整。
- **D10 设计文档一致性**：owner-doc §1/§2/§3 与 Bean + dict 一致（D-DRP-1 + D-DRP-2 Fix 后），无残余漂移。
- **D11 dict 可达性**：两 dict 全部值均有 `setStatus` writer（见四方表 writer 列），**零死状态**。

**裁决：Verdict: pass（无 P0/P1 发现；双轴干净）**。owner doc 内部漂移 D-DRP-1（§1:15 + §3:41 APPROVED 误标终态）+ D-DRP-2（§2 迁移表遗漏 APPROVED→DRAFT）已 Fix 落地（Fix 后 §1:15 + §2:36 + §3:41 一致），无其他不一致项。

**Watch-only residual（不重命名）**：`ERR_DRP_LINE_NOT_SUGGESTED`（`ErpDrpErrors.java:35-38`）实为 releaseLine「非 APPROVED」语义，名称误导但重命名为公共错误码契约变更，超出本重构范围——接线后行为不变，登记为 watch-only residual（见 Deferred But Adjudicated）。

### Delta 适用性证据（§11.1 步骤 7，M2 非保护域；Plan 轴）

Plan 轴 Delta 双加载运行时证据（复用 M1.2 范式）：

- **Delta 派生类**：`ErpDrpPlanStateMachineDelta`（测试作用域）覆盖 `assertCanResetToDraft`——基线允许 {COMPUTED/APPROVED}，Delta 收紧为仅 {COMPUTED}（保护已批准计划的审计留痕，禁止 APPROVED 直回 DRAFT）。仅覆盖 1 方法，其余继承基线。
- **VFS Delta 层**：`module-drp/erp-drp-service/src/test/resources/_vfs/_delta/test-drp-delta/erp/drp/beans/app-service.beans.xml` 以同名 bean id `app.erp.drp.service.statemachine.ErpDrpPlanStateMachine` 覆盖基线为派生类。
- **双加载测试（运行时证据，非静态检查）**：
  - `TestErpDrpPlanStateMachineBaselineIoC`（4 用例）：容器解析 = 基线类（非 Delta），`assertCanResetToDraft(APPROVED)` **放行**，`assertCanResetToDraft(EXECUTED/DRAFT)` 抛 common 码。
  - `TestErpDrpPlanStateMachineDeltaOverride`（3 用例，`@NopTestProperty nop.core.vfs.delta-layer-ids=test-drp-delta`）：容器解析 = `ErpDrpPlanStateMachineDelta` 派生类，`assertCanResetToDraft(APPROVED)` **抛异常**（收紧，与基线可区分），`assertCanResetToDraft(COMPUTED)` 放行，非覆盖动作（runDrp/approvePlan/advanceToExecuted/isTerminal）继承基线不变。
  - 可区分差异点：同一 `assertCanResetToDraft(APPROVED)` 基线放行、Delta 抛异常 → 基线/Delta 双加载证据成立。
- Line 轴继承 Plan 轴 + M1.2 既有证明（同机制、同容器），不重复证（计划 Phase 3 明示）。

Closure Audit Evidence:

- Executor / Agent: 执行者（本会话）完成 Phase 1-3 全部代码 + 测试 + 四方对照 + Delta 证据。
- 验证证据：
  - 层 1 矩阵：`mvn test -pl module-drp/erp-drp-service -Dtest=TestErpDrpPlanStateMachineMatrix,TestErpDrpLineStateMachineMatrix` → 18 用例全绿（10 Plan + 8 Line）。
  - Delta 双加载：`TestErpDrpPlanStateMachineBaselineIoC`（4）+ `TestErpDrpPlanStateMachineDeltaOverride`（3）→ 7 用例全绿。
  - 针对性接线回归：`TestErpDrpWiringRegression` → 9 用例全绿（cancelLine/rejectLine/approveLine/APPROVED→DRAFT 接线点）。
  - 层 3 回归：`mvn test -pl module-drp/erp-drp-service` → 70 用例全绿（含 `TestErpDrpEngine` 7 例：happy path + `testRunDrpRejectsNonDraftPlan` + `testReleaseRejectsAlreadyOrderedLine` + `testResetToDraftClearsSuggestedLines`；`TestErpDrpScheduleRelease` 6 例；`TestErpDrpPlanCrudSmoke` 5 例等）。
  - 全仓库构建：`mvn clean install -DskipTests` → BUILD SUCCESS。
  - 合规检查：`bash docs/audits/nop-compliance-checker.sh` → exit 0；R5（`@Inject private`）= 0、R11（Processor 重复状态判断方法）= 0，无新增违例。
  - 状态性 grep：两 Bean 不 import DAO/IBiz/IServiceContext/事务。
  - 接线 grep：相关方法体内无内联 `Objects.equals(*status*, *_STATUS_*)` 矩阵判断（残余 `Objects.equals` 均为显式标注的动态守卫：`DrpEngine.clearSuggestedLines` 副作用过滤、`DrpReleaseService.advancePlanToExecutedIfComplete`「全部行终态」隐式门控、`DrpReleaseService.requireReleasable` ORDERED 幂等检查）。
  - owner doc 漂移 Fix：`docs/design/drp/state-machine.md` §1:15（APPROVED 移出终态标注）+ §2:36（补 APPROVED→DRAFT 迁移行）+ §2:28-29（图补 APPROVED→DRAFT 回退边）+ §3:41（APPROVED 移出终态列举）均已落地。
- Auditor / Agent: 独立结束审计子代理（mission-driver:2026-08-12-111827-mission-driver 闭环节点，新会话，不重用执行者上下文）
- Evidence: 独立读取计划全文 + 计划指南全文；逐项核实实时仓库：(1) 两 Bean 存在且矩阵与计划一致——`ErpDrpPlanStateMachine.java:109-121` 5 边（runDrp/approvePlan/resetToDraft 双源/advanceToExecuted）、`ErpDrpLineStateMachine.java:87-97` 4 边（approveLine/releaseLine/cancel 双源）；`isTerminal` Plan 仅 EXECUTED（`:103-105`，APPROVED 非终态）、Line ORDERED+CANCELLED（`:80-83`）。两 Bean 严格无状态（仅 import ErpCommonErrors/ErpDrpConstants/NopException/集合类）。(2) `app-service.beans.xml:65-68` 两 Bean FQN-id 注册（与既有 Processor 范式一致）。(3) 6 处接线全部落地：`DrpEngine.java:49,59`（注入 + setter，字段非 private）、`DrpReleaseService.java:56,58`、`ErpDrpLineBizModel.java:48,73,79`（INLINE 保持，assertCanApproveLine + 目标态写回）、`ErpDrpPlanApprovePlanProcessor.java:33,35`（Plan + Line 双注入含行级联）、`ErpDrpLineCancelLineProcessor.java:25`、`ErpDrpLineRejectLineProcessor.java:25`（同语义各自独立类）。(4) owner doc 漂移 Fix 落地：`docs/design/drp/state-machine.md:15` APPROVED 标注「非终态」、`:30` 图补 APPROVED→DRAFT 回退边、`:38` §2 迁移表补 APPROVED→DRAFT 行、`:43-44` §3 仅 EXECUTED 为终态 + APPROVED 非终态显式声明（D-DRP-1 + D-DRP-2 完全对齐，无残余漂移）。(5) 测试文件全部存在：`TestErpDrpPlanStateMachineMatrix`/`TestErpDrpLineStateMachineMatrix`（层 1 矩阵）、`TestErpDrpPlanStateMachineBaselineIoC`/`TestErpDrpPlanStateMachineDeltaOverride`（Delta 双加载）、`TestErpDrpWiringRegression`（针对性接线回归）。(6) `docs/logs/2026/08-12.md:3-8` 含本计划三 Phase 完整日志条目（与 Closure 状态文本一致）。语义验证：Phase 状态/退出标准/Closure Gates/Closure evidence 五点一致；无非阻塞跟进隐藏范围内的实时缺陷（D-DRP-1/D-DRP-2 已 Fix 落地，ERR_DRP_LINE_NOT_SUGGESTED 误名按 D-DRP-3 诚实登记为 watch-only residual）；Deferred 项均带 successor 触发条件；Anti-Hollow 检查通过（Bean 有实际矩阵逻辑、非空方法体、接线点全部经测试覆盖）。审计结论：**approved**，计划可关闭。

Follow-up:

- <无非阻塞跟进；Deferred 项（ERR_DRP_LINE_NOT_SUGGESTED 误名 / ERR_DRP_LINE_ILLEGAL_TRANSITION 参数对称化 / DRP 仿真域状态轴 / 全局 CRUD 写锁）已在 Deferred But Adjudicated 登记并指派 successor 触发条件；owner doc 漂移 Fix 为范围内项已落地>
