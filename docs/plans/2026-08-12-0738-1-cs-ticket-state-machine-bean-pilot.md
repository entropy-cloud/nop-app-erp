# 2026-08-12-0738-1-cs-ticket-state-machine-bean-pilot 客服 ErpCsTicket 实体级状态机 Bean 试点（M1.1 + M1.2）

> Plan Status: completed
> Last Reviewed: 2026-08-12
> Source: `docs/backlog/entity-state-machine-migration-roadmap.md` M1.1（done）+ M1.2（done）
> Related: `2026-08-12-0738-2-cs-ticket-state-machine-pilot-evaluation.md`（N=2，依赖本计划 M1.1+M1.2 产物，裁定批量迁移模板）；前置 `2026-08-12-0617-1-entity-state-machine-m0-1-contract.md`（M0.1 契约 done）+ `2026-08-12-0617-2-entity-state-machine-m0-2-inventory.md`（M0.2 清单 done）
> Mission: entity-state-machine
> Work Item: M1.1 + M1.2
> Audit: required

## Current Baseline

- **M0.1 契约已定稿**（`docs/architecture/entity-state-machine-bean.md`）：颗粒度（一 Bean 对应一实体一轴）、无状态约束、显式动作方法 + 只读 `transitions()` 元数据接口、Bean 按 FQN id 在非生成 `app-service.beans.xml` 注册、Processor 按类型注入、`@Inject` 字段不得 `private`、非法边由 Bean 报告 common 层码 + Processor 映射领域 ErrorCode、CRUD 写入边界裁定为选项 (c) 显式排除（命名动作矩阵唯一权威，通用 CRUD 不在运行时强制范围）。M0.1 落地了**测试作用域合成探针** `ErpProbeStateMachine` + `TestErpProbeStateMachineContract`（证明 IoC 解析/按类型注入/非法边映射/元数据形状），**未绑定真实业务实体**。
- **M0.1 Deferred → 本计划 M1.2 接管**：业务级 Delta 同名 Bean 覆盖**运行时实证**为 successor（契约 §6 + M0.1 计划 Deferred）。平台 Delta 机制经平台层实证可用；本项目业务级 Bean Delta 实证 = 0。M1.2 必须在真实应用容器证明基线/Delta 两种加载结果。
- **M0.2 清单已裁定 CS-1**：`ErpCsTicket.status`（dict `erp-cs/ticket-status`，6 态）= M1.1 试点。八属性登记：迁移语义 `NEW→ASSIGNED→IN_PROGRESS→RESOLVED→CLOSED`；`RESOLVED→IN_PROGRESS`(reopen)；非终态→CANCELLED。owner doc `customer-service/state-machine.md`。无财务影响。data-deletion 保护区（reopen 删除未答问卷）。Ticket 无 dict 死状态（6 态均有 writer）。
- **Ticket SLA 起算 drift 已裁决**（`docs/analysis/2026-08-12-entity-state-axis-inventory.md §4`）：`startDateTime = 首次 IN_PROGRESS`（start 动作设置）裁定为 **intentional legacy behavior**，§1 表「SLA 从创建时开始计时」表述被 §实现约定取代。**M1.1 必须保持此行为**，不得静默改为创建时起算。此裁决解除 M1.1 矩阵固化前置。
- **当前固定迁移判断散布在 3 处**（实时核实）：
  - `ErpCsTicketBizModel.java:108,124,149,180` 内联 `Objects.equals(from, X)` 守卫 + 私有 `illegalTransition(...)` helper（assign/start/close/cancel），抛 `ERR_INVALID_TICKET_STATUS_TRANSITION`（`erp.err.cs.ticket.illegal-status-transition`，含 ticketCode/currentStatus/expectedStatus 参数）。
  - `ErpCsTicketResolveProcessor.java:38` 内联 `Objects.equals(from, IN_PROGRESS)` 守卫（resolve）。
  - `ErpCsTicketReopenProcessor.java:39` 内联 `Objects.equals(from, RESOLVED)` 守卫（reopen；并执行 data-deletion：`cancelUnrespondedSurvey` 删除 `respondedAt` 空的调查）。
  - 三处各自重复 `illegalTransition` helper + `requireTicket` helper（cancel **不**经 `illegalTransition`——它对终态直接抛 `ERR_TICKET_ALREADY_TERMINAL`，`ErpCsTicketBizModel.java:182`；`illegalTransition` 仅 assign/start/close 使用）。cancel 守卫非单来源态（`{NEW,ASSIGNED,IN_PROGRESS,RESOLVED}` → CANCELLED）。
- **生产 Bean 注册范式已存在**：`module-cs/erp-cs-service/src/main/resources/_vfs/erp/cs/beans/app-service.beans.xml` 已以 `<bean id="<FQN>" class="<FQN>"/>` 注册 9 个 per-mutation Processor（`:34-51`，含 Resolve/Reopen/MatchAndAttachSla/ScanOverdue）。StateMachine Bean 沿用此范式。
- **层 3 回归基线已存在**：`TestErpCsTicketSlaCsat` 经 `IGraphQLEngine`/BizModel 入口断言完整六态生命周期 + 终态不可恢复 + `ERR_INVALID_TICKET_STATUS_TRANSITION`（`:99,108`）+ SLA/CSAT 副作用。M0.1 §10 + M0.2 §3.1 明确：既有集成测试 = 层 3 回归，**新增表驱动测试 = 层 1 矩阵**；执行者不得将层 3 当空白重建，也不得用层 3 冒充层 1。
- **common 层非法迁移 ErrorCode 已存在（参数形状待裁定）**：契约 §7 要求 Bean 抛 common 层码、Processor 映射领域码。**已存在** `module-common-service` 的 `ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION`（`nop.err.erp.common.illegal-status-transition`，参数 `currentStatus`/`expectedStatus`，`ErpCommonErrors.java:23-27`），已被 `AbstractProcessor.defaultIllegalStatusException`（`AbstractProcessor.java:92-95`）使用。但其参数形状（`currentStatus`/`expectedStatus`）与契约 §7 + M0.1 探针形状（`action`/`fromStatus`，`ErpProbeStateMachine.java:32-35`）**不一致**——Phase 1 须裁定复用既有 common 码（参数形状不匹配）还是新增/调整 common 码匹配契约 §7 的 `action`/`fromStatus` 拒绝元数据形状。
- **合规基线**：`docs/audits/compliance-baseline.md` R5（`@Inject private`）= 0、R11（Processor 重复状态判断方法）= 0。本计划新增 Bean 注册 + 注入须保持 R5=0；接线后三处重复 `illegalTransition` helper 收敛，R11 不增。

## Goals

- 落地真实 `ErpCsTicketStateMachine` Bean（一 Bean 对一实体一轴 `status`），承载六态迁移矩阵 + 终态/初始态分类 + 只读 `transitions()` 元数据，严格无状态、可经 Delta 同名覆盖。
- 将 BizModel（assign/start/close/cancel）与 Resolve/Reopen Processor 的**固定来源态/目标态判断**改调 Bean（`assertCan<Action>` + `targetStatus`），动态业务守卫（SLA 计时、close-breached 检查、CSAT 触发、reopen 删除未答问卷、审计 actionType）保留在 BizModel/Processor。
- 保持全部既有外部行为不变（含 SLA 起算 = 首次 IN_PROGRESS、data-deletion reopen 行为、错误码与参数、审计 fromStatus/toStatus）。
- 新增层 1 矩阵完备性表驱动测试；层 3 既有 `TestErpCsTicketSlaCsat` 回归保持全绿。
- **M1.2**：在真实应用容器（非静态检查、非编译派生类）证明 Delta 同名 Bean 覆盖替换生效——替换一条允许边或分类语义，产出基线/Delta 两种加载结果证据，满足契约 §6 业务级实证义务。

## Non-Goals

- 不修改任何 `model/*.orm.xml` / `model/*.api.xml` / 字典 yaml（路线图 Non-Goal；`status` 字段语义/dict 不变）。
- 不改变任何业务状态值、动作名、错误码值、权限、审计 actionType、SLA 时序、CSAT 触发、reopen 副作用（路线图 Non-Goal「不借迁移改变既有行为」）。
- 不引入全局 CRUD 写锁或 xmeta `notUpload`（CRUD 边界已在 M0.1 §9 裁定为选项 c；更强写锁是 successor）。
- 不迁移 `docStatus`/`approveStatus`（Ticket 以 `status` 为主轴；CS-2/CS-3 经 M0.2 裁定排除-技术）。
- 不裁定批量迁移模板（归 N=2 计划 M1.3）；本计划只交付试点 Bean + Delta 实证，并在结束审计中声明「试点已落地、模板裁定归 M1.3」。
- 不构建反射型/泛型全局 `IStateMachine` 调度器（路线图 Non-Goal）。

## Task Route

- Type: `implementation-only change`（消费已定稿 M0.1 契约 + M0.2 清单，落地单个轴的 Bean + 接线 + 测试 + Delta 证明；不改契约/模型/公共 API）
- Owner Docs: `docs/architecture/entity-state-machine-bean.md`（M0.1 契约——Bean 形状/注册/Delta/错误语义/测试分层）、`docs/design/customer-service/state-machine.md`（业务状态语义 + §实现约定 + M0.2 SLA drift 补注）、`docs/analysis/2026-08-12-entity-state-axis-inventory.md §3.2`（CS-1 八属性）、`docs/architecture/processor-extension-pattern.md`（Bean 嵌入 Processor 编排点）
- Skill Selection Basis: 路线图 M1.1/M1.2 指定 `nop-backend-dev` + `nop-testing`。`nop-backend-dev` 匹配「BizModel/Processor 接线、Bean 注册、跨实体调用边界、错误码、事务边界、产品化可定制性自检」；`nop-testing` 匹配「JunitAutoTestCase/IGraphQLEngine 测试基类、request.json5、RECORDING→CHECKING 切换、矩阵表驱动测试」。其必需输入（owner doc + M0.1 契约 + 既有测试）已就绪。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（纯后端 Java + 既有 cs-service 测试容器）。
- 前置依赖：M0.1 done（契约）+ M0.2 done（CS-1 八属性 + SLA drift 裁决）。均已满足。
- data-deletion 人工批准：M1.1 属 data-deletion ask-first（reopen 删除未答问卷）；**用户已于 2026-08-06 批准**（路线图 M1 行 + M1 试点纪律记录），范围限于保留既有 reopen 删除行为的状态机迁移。本计划在此批准范围内，不重开其他保护区域。

## Execution Plan

### Phase 1 - ErpCsTicketStateMachine Bean + 注册 + common 错误码裁定 + 层 1 矩阵完备性测试

Status: completed
Targets: `module-cs/erp-cs-service/src/main/java/app/erp/cs/service/statemachine/ErpCsTicketStateMachine.java`（新）；`module-cs/erp-cs-service/src/main/resources/_vfs/erp/cs/beans/app-service.beans.xml`（追加 Bean 注册）；`module-cs/erp-cs-service/src/test/java/.../statemachine/TestErpCsTicketStateMachineMatrix.java`（新，层 1）
Skill: `nop-backend-dev`（Bean 形状/注册）+ `nop-testing`（层 1 表驱动测试）

- Item Types: `Add | Decision | Proof`
- Prereqs: M0.1 done + M0.2 done

- [x] `Explore`：核实除既有 `ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION`（`currentStatus`/`expectedStatus`）外，nop-entropy 或本项目是否还存在**其他** common 层非法迁移码（尤其携带 `action`/`fromStatus` 形状的），供 Phase 1 Decision 复用判断；产出确定性结论。Skill: none（grep 探索，非状态机业务审查）
- [x] `Decision`：common 层非法迁移码参数形状裁定——选择 **(A) 复用既有** `ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION`（参数 `currentStatus`/`expectedStatus`），并附 `action` 补充诊断参数（queryable via `getParam("action")`）。残留风险：拒绝元数据 primary 参数名（`currentStatus`/`expectedStatus`）与契约 §7 字面 `action`/`fromStatus` 不同（语义一致：currentStatus=fromStatus, expectedStatus=允许来源态），`action` 作补充参数保留；不修改保护区架构 doc `entity-state-machine-bean.md`，参数名映射作 successor 文档对齐。Skill: none
- [x] `Add`：创建 `ErpCsTicketStateMachine`（无状态、不注入 DAO/IBiz/IServiceContext），按契约 §4 实现：
  - 显式动作方法（主路径）：`assertCanAssign(NEW)` / `assertCanStart(ASSIGNED)` / `assertCanResolve(IN_PROGRESS)` / `assertCanClose(RESOLVED)` / `assertCanReopen(RESOLVED)` / `assertCanCancel(非终态)`；非法来源态抛 common 层码 + `action`/`fromStatus` 元数据。
  - 目标态方法：`assignTargetStatus()`→ASSIGNED / `startTargetStatus()`→IN_PROGRESS / `resolveTargetStatus()`→RESOLVED / `closeTargetStatus()`→CLOSED / `reopenTargetStatus()`→IN_PROGRESS / `cancelTargetStatus()`→CANCELLED。
  - 终态分类：`isTerminal(CLOSED|CANCELLED)`。
  - 只读元数据：`transitions()` 返回不可变快照（9 条边：assign 1 + start 1 + resolve 1 + close 1 + reopen 1 + cancel 4 来源态）；`terminalStatuses()`；`initialStatuses()`(NEW)。
  Skill: `nop-backend-dev`
- [x] `Add`：在 `app-service.beans.xml` 以 `<bean id="app.erp.cs.service.statemachine.ErpCsTicketStateMachine" class="...ErpCsTicketStateMachine"/>` 注册（沿用既有 Processor FQN-id 范式）。Skill: `nop-backend-dev`
- [x] `Proof`（层 1 矩阵完备性，新增 greenfield 表驱动测试 `TestErpCsTicketStateMachineMatrix`）：遍历每个动作的合法/非法来源态，验证——(a) 无重复/冲突边；(b) 从 NEW 可达全部 5 非初始态且 CLOSED/CANCELLED 终态无出边；(c) cancel 对四非终态合法、对两终态非法；(d) `transitions()` 元数据与显式方法语义一致（每条边的 action/fromStatus/toStatus）；(e) 终态/初始态集合正确。**不**经 BizModel 入口（层 1 只测 Bean），不断言副作用/审计（层 3 职责）。Skill: `nop-testing`

Exit Criteria:

- [x] `ErpCsTicketStateMachine` 落地（6 动作 + 目标态 + isTerminal + transitions 元数据），无状态（grep 证实不 import DAO/IBiz/IServiceContext/事务）。
- [x] Bean 已在 `app-service.beans.xml` 注册（FQN id）；`@Inject` 字段（如有）非 private（合规 R5）。
- [x] common 层非法迁移码裁定有确定性结论与落地点（非悬置）。
- [x] 层 1 矩阵测试 `mvn test -pl module-cs/erp-cs-service -Dtest=TestErpCsTicketStateMachineMatrix` 全绿，覆盖上述 (a)-(e)。
- [x] 本地化编译检查：`mvn compile -pl module-cs/erp-cs-service -am` 通过（解除 Phase 2 接线对 Bean 已编译的依赖）。

### Phase 2 - BizModel/Processor 接线（行为保持）+ 层 3 回归

Status: completed
Targets: `ErpCsTicketBizModel.java`（assign/start/close/cancel）、`ErpCsTicketResolveProcessor.java`（resolve）、`ErpCsTicketReopenProcessor.java`（reopen）
Skill: `nop-backend-dev`（接线 + 错误码映射）+ `nop-testing`（回归断言）

- Item Types: `Fix | Proof`
- Prereqs: Phase 1

- [x] `Fix`：BizModel 注入 `ErpCsTicketStateMachine`（按类型注入，字段非 private），将 assign/start/close 的内联 `Objects.equals` 守卫替换为 `stateMachine.assertCan<Action>(from)`，目标态写回改为 `stateMachine.<action>TargetStatus()`；cancel 先经 `stateMachine.assertCanCancel(from)`（非终态合法）再保留终态 `ERR_TICKET_ALREADY_TERMINAL` 抛出路径——注意 cancel 的 Bean 断言与终态领域异常存在有意重叠（Bean 报告 common 非法边 vs 领域对终态抛 `ERR_TICKET_ALREADY_TERMINAL`），接线须令**终态**走领域码（保持既有外部错误码）、**非终态非法**走 Bean→领域映射，不得让两者冲突。删除 BizModel 内私有 `illegalTransition` helper 的矩阵部分。**动态业务守卫保留原位**：close-breached 检查（`ERR_TICKET_CLOSE_BREACHED_NO_REASON`）、start 设 `startDateTime`（保持 = 首次 IN_PROGRESS）、审计 actionType/fromStatus/toStatus 写回不变。Skill: `nop-backend-dev`
- [x] `Fix`：Resolve/Reopen Processor 注入 `ErpCsTicketStateMachine`，将内联守卫替换为 Bean 调用；Processor 捕获/感知 Bean 的 common 层非法边报告，映射为领域 `ERR_INVALID_TICKET_STATUS_TRANSITION`（保留 ticketCode/currentStatus/expectedStatus 参数，common 码作 cause 保留——对齐契约 §7，**参照** M0.1 探针 `ProbeProcessorStub` 的 common→领域映射**范式**，该桩为测试作用域合成件，仅作形状参照、不导入不复用）。**保留** resolve 的 SLA duration/isSlaCompleted 计算 + CSAT 触发；**保留** reopen 的 data-deletion 行为（`cancelUnrespondedSurvey`，2026-08-06 批准范围内，不得改变）。Skill: `nop-backend-dev`
- [x] `Proof`（层 3 既有回归保持全绿）：`mvn test -pl module-cs/erp-cs-service -Dtest=TestErpCsTicketSlaCsat` 全绿——证明六态生命周期、终态不可恢复、`ERR_INVALID_TICKET_STATUS_TRANSITION` 错误码、SLA/CSAT 副作用、reopen 行为均不变。若既有测试因 helper 私有化等需调整断言，仅调整与矩阵无关的部分并记录理由（不得弱化断言）。Skill: `nop-testing`

Exit Criteria:

- [x] 三处固定来源态/目标态判断均改调 Bean（BizModel 4 动作 + Resolve + Reopen），grep 证实相关方法体内不再有内联 `Objects.equals(from, TICKET_STATUS_*)` 矩阵判断（动态守卫如 close-breached/终态判定除外）。
- [x] 接线后 `ERR_INVALID_TICKET_STATUS_TRANSITION` 错误码 + 参数（ticketCode/currentStatus/expectedStatus）对外不变（层 3 断言证实）；`ERR_TICKET_ALREADY_TERMINAL` 保留。
- [x] reopen data-deletion 行为不变（层 3 断言证实未答问卷仍被删除）。
- [x] SLA 起算仍 = 首次 IN_PROGRESS（start 设 startDateTime 行为不变；无创建时起算回退）。
- [x] 层 3 `TestErpCsTicketSlaCsat` 全绿。

### Phase 3 - M1.2 Delta 同名 Bean 覆盖运行时证明

Status: completed
Targets: Delta 层测试 fixture（`module-cs/erp-cs-service/src/test/...` 或 app 层 Delta 测试）；基线/Delta 双加载证据
Skill: `nop-backend-dev`（Delta 覆盖机制）+ `nop-testing`（运行时双加载断言）

- Item Types: `Add | Proof | Decision`
- Prereqs: Phase 2

- [x] `Decision`：选定 Delta 覆盖语义——放开 `assign` 来源态：基线仅允许 `NEW`，Delta 额外允许 `RESOLVED`（重新分派已解决工单）。派生类 `ErpCsTicketStateMachineDelta` 仅覆盖 `assertCanAssign` 一个方法。预期双加载差异：`assertCanAssign(RESOLVED)` 基线非法（抛 common 码）、Delta 合法（放行）。Skill: `nop-backend-dev`
- [x] `Add | Proof`（运行时双加载证据，**非静态检查**）：`TestErpCsTicketStateMachineBaselineIoC`（基线加载，生产 beans）+ `TestErpCsTicketStateMachineDeltaOverride`（Delta 加载，VFS Delta 层 `test-cs-delta` 以同名 bean id 覆盖）。日志证实容器解析 bean 为 `ErpCsTicketStateMachineDelta`（class=...Delta，源自 `_delta/test-cs-delta/...`）。基线 `assertCanAssign(RESOLVED)` 抛异常、Delta 放行——二者可区分。Skill: `nop-testing`
- [x] `Proof`（Delta 覆盖范围限定）：`testNonOverriddenActionsInheritBaseline` 证实 Delta 仅替换 `assertCanAssign`，resolve/start/isTerminal 等非覆盖动作继承基线不变（Bean 无副作用，audit/SLA/CSAT 经 Processor 路径不受影响）。Skill: `nop-testing`

Exit Criteria:

- [x] 存在一份运行时证据（测试或可复现步骤），同时展示基线加载与 Delta 加载两种结果且二者可区分——满足契约 §6 业务级 Delta 实证义务（解除 M0.1 Deferred successor）。
- [x] Delta 覆盖经真实容器 bean 解析注入生效（非静态检查）。
- [x] Delta 仅替换选定语义，基线其余动作不变。

## Draft Review Record

- Independent draft review iteration 1: `needs revision` (`ses_00ccc9799ffe64MaewUvOM5qhn`) — 无 BLOCKER。1 MAJOR：Current Baseline 误称 common 层非法迁移码「归属未定」并设存在性 Explore，实仓已有 `ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION`（`nop.err.erp.common.illegal-status-transition`，参数 `currentStatus`/`expectedStatus`，被 `AbstractProcessor.defaultIllegalStatusException` 使用）——真正 Decision 是参数形状复用(不匹配) vs 新增匹配契约 §7 `action`/`fromStatus`。4 MINOR：per-mutation Processor 计数 8→9；cancel 不经 `illegalTransition`（直接抛 `ERR_TICKET_ALREADY_TERMINAL`）；Phase 2 cancel Bean 断言与终态领域异常有意重叠须防冲突；`ProbeProcessorStub` 为测试作用域合成件仅作形状参照。迭代 1 已全部修订：基线改为「common 码已存在 + 参数形状不一致」并据实重述；Phase 1 Explore 收窄为「核实是否有其他 action/fromStatus 形状码」、Decision 改为参数形状复用 vs 新增裁定（含双向残留风险）；Processor 计数改 9；cancel 描述校正 + Phase 2 接线补防冲突说明；ProbeProcessorStub 补形状参照不导入说明。全部 load-bearing 行号经独立复核 CONFIRMED TRUE。
- Independent draft review iteration 2: `accept` (`ses_00cc78b44ffeL0R1QxB3GLKdCp`) — 迭代 1 MAJOR 经实仓复核 RESOLVED（common 码已存在 + 参数形状不一致已据实重述；Explore 收窄为「核实是否有其他 action/fromStatus 形状码」；Decision 改为参数形状复用 vs 新增裁定含双向残留风险）。4 MINOR 全 RESOLVED（Processor 9、cancel 不经 illegalTransition、Phase 2 cancel 防冲突说明、ProbeProcessorStub 形状参照不导入）。无新问题引入（边/Processor 计数自洽、无新反松弛、Decision 未预判）。执行期附注（非阻塞）：若 Decision 选 (A) 复用既有 common 码并据其参数形状微调契约 §7 描述，该 `entity-state-machine-bean.md` 变更属架构 owner doc（保护区），执行时须作独立 Fix/Decision 工件跟踪、不得静默折叠进 Bean PR。草案审查收敛，Plan Status → active。

## Closure Gates

> 本计划含生产代码变更（新增 Bean + 接线 + 测试），Closure Gates 运行完整仓库验证。验证命令见 `docs/context/project-context.md`。

- [x] 范围内行为完成（Bean + 接线 + 层 1 矩阵测试 + M1.2 Delta 运行时证据）
- [x] 相关文档对齐（`entity-state-machine-bean.md` §6 业务级 Delta 实证状态由 successor 占位→本计划落地后可标注已验证；若接线或 SLA 行为发现与 owner doc 冲突，按 Fix 记录，不改业务语义）
- [x] 已运行验证：`mvn clean install -DskipTests`（全仓库 BUILD SUCCESS，156 模块）+ `mvn test -pl module-cs/erp-cs-service`（cs-service 测试全绿 116，含层 1 + 层 3 + Delta）+ `bash docs/audits/nop-compliance-checker.sh`（exit 0，无 actual > baseline 漂移——R5=0、R11=0）
- [x] 无范围内项目降级为 deferred/follow-up（M1.2 Delta 运行时证明是硬交付，不得降级；data-deletion 行为保持是硬约束）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：Plan Status、各 Phase Status、Exit Criteria、Closure Gates、日志一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 批量迁移模板裁定

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划交付试点 Bean + Delta 实证；批量迁移模板（矩阵 Bean/Delta/测试的标准化模板与 M2/M3 适用性裁定）是 M1.3 的专属交付（N=2 计划），不属本计划结果表面。
- Successor Required: yes（触发条件 = 本计划 M1.1+M1.2 闭包后，由 `2026-08-12-0738-2-...-pilot-evaluation.md` M1.3 接管）

### 全局 CRUD 写锁

- Classification: `watch-only residual`
- Why Not Blocking Closure: M0.1 §9 已裁定选项 (c) 显式排除；更强全局写锁须改 ORM/xmeta（保护区 ask-first），是独立 successor。
- Successor Required: no（仅当产品要求全局强制矩阵写锁时重开）

## Closure

Status Note: 三阶段全部执行完毕（2026-08-12）。Phase 1 落地 `ErpCsTicketStateMachine` Bean（6 动作 + 9 边 + isTerminal + transitions 元数据，无状态）+ common 错误码裁定（Option A 复用既有 `ERR_ILLEGAL_STATUS_TRANSITION` + action 补充参数）+ 层 1 矩阵完备性测试（8 用例全绿）。Phase 2 接线 BizModel 4 动作 + Resolve/Reopen Processor，固定来源态/目标态判断改调 Bean，common→领域错误码映射保留，动态守卫（SLA 起算/close-breached/CSAT/data-deletion）原位不变；层 3 回归 13 用例全绿。Phase 3 经 VFS Delta 层 `test-cs-delta` 证明同名 bean id 覆盖替换生效（容器解析为 `ErpCsTicketStateMachineDelta`），基线/Delta 双加载可区分（`assertCanAssign(RESOLVED)` 基线非法/Delta 合法），3+3 用例全绿。全仓库 `mvn clean install -DskipTests` BUILD SUCCESS（156 模块）；cs-service 全量测试 116 全绿；合规检查器 exit 0（R5=0、R11=0）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话 `ses_00ccc9799ffe64MaewUvOM5qhn` 草案审查后另起的 closure-audit 会话），非执行者。
- Audit Scope: 五点一致性 + Exit Criteria vs 实仓 + Anti-Hollow + Deferred honesty + Docs sync。
- Evidence:
  - Phase 1 层 1: `TestErpCsTicketStateMachineMatrix` — 8 用例全绿（矩阵完备性 (a)-(e)）
  - Phase 2 层 3: `TestErpCsTicketSlaCsat` — 13 用例全绿（六态生命周期 + 非法迁移 + SLA/CSAT + reopen data-deletion + close-breached + 终态不可恢复）
  - Phase 3 Delta: `TestErpCsTicketStateMachineBaselineIoC` — 3 用例全绿（基线）；`TestErpCsTicketStateMachineDeltaOverride` — 3 用例全绿（Delta 覆盖经真实容器解析注入）
  - cs-service 全量: 116 用例全绿
  - 全仓库: `mvn clean install -DskipTests` BUILD SUCCESS（156 模块）
  - 合规: `nop-compliance-checker.sh` exit 0（R5=0、R11=0）
  - 实仓复核（独立审计）：`ErpCsTicketStateMachine.java`（165 行，无状态、6 动作 + 9 边 + transitions 元数据，common 码 + action 补充参数）CONFIRMED；`app-service.beans.xml:35-36` FQN id 注册 CONFIRMED；BizModel（`ErpCsTicketBizModel.java:177-183` cancel 终态/非终态防冲突、`assertCan` helper:354-372）+ Resolve Processor（`:42,56`）+ Reopen Processor（`:42,46`）接线 CONFIRMED；`grep Objects.equals(from, TICKET_STATUS*)` 在 cs-service 全域返回 **0 命中**（矩阵判断已收敛至 Bean）；Delta 派生类 + VFS Delta 层 `_delta/test-cs-delta/.../app-service.beans.xml` 同名 bean id 覆盖 CONFIRMED；`docs/logs/2026/08-12.md` 含本计划条目（M1.1+M1.2 详记）CONFIRMED。
  - Anti-Hollow 复核：Bean 方法均有实际矩阵逻辑（无空体/无 `return null` 占位）；Processors/BizModel 接线后运行时可达（beans 注册 + 按类型注入闭环）；common→领域错误码映射保留 cause（`new NopException(domain, cause)`）。
  - Deferred honesty 复核：批量迁移模板裁定（→ M1.3 successor，触发条件 = 本计划闭包）+ 全局 CRUD 写锁（M0.1 §9 裁定选项 c，watch-only）均如实登记，无范围内的实时缺陷或契约漂移被隐藏。
  - 裁定：PASS — 五点一致、Exit Criteria 全对齐实仓、无空壳代码、Deferred 诚实、docs/logs 已同步。计划可关闭。
