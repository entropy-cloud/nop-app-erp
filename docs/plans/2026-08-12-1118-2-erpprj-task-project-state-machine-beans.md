# 2026-08-12-1118-2-erpprj-task-project-state-machine-beans 项目 ErpPrjTask + ErpPrjProject 实体级状态机 Bean（M2.3 + M2.4）

> Plan Status: completed
> Last Reviewed: 2026-08-12
> Source: `docs/backlog/entity-state-machine-migration-roadmap.md` M2.3（todo）+ M2.4（todo）
> Related: 前置 `2026-08-12-0617-1-entity-state-machine-m0-1-contract.md`（M0.1 done）+ `2026-08-12-0617-2-entity-state-machine-m0-2-inventory.md`（M0.2 done）+ `2026-08-12-0738-1-cs-ticket-state-machine-bean-pilot.md`（M1.1 范式）+ `2026-08-12-0738-2-cs-ticket-state-machine-pilot-evaluation.md`（M1.3 模板 done）
> Mission: entity-state-machine
> Work Item: M2.3 + M2.4
> Audit: required

## Current Baseline

- **M0.1 契约 + M1.3 模板已就绪**（`docs/architecture/entity-state-machine-bean.md` §1-§11）。M1.3 已裁定 **go**，M2 各项 Deps（M1.3）门控解除；`todo → ready` 仍需独立 plan 草案审查（路线图规则 1）。
- **本计划按规则 14 将同组件（同 owner doc `projects/state-machine.md`、同结果表面「项目域生命周期 StateMachine Bean」、同验证路径）的两条状态轴合并为一个计划的两个阶段**：Task（§适用对象二）+ Project（§适用对象一）。二者均 M2 简单生命周期（§11.2），非保护域、无审批子矩阵、无跨域过账副作用（项目成本归集经 `IErpFinAcctDocProvider` 是独立业财面，非项目头状态轴触发）。
- **任务（ErpPrjTask.status）语义**（owner doc §适用对象二）：4 态 TODO/IN_PROGRESS/DONE/BLOCKED；终态 = DONE；IN_PROGRESS↔BLOCKED 可往复；owner doc 提及「取消→已取消（隐含于项目取消）」，但 task dict `erp-prj/task-status` **无 CANCELLED 值**，亦无 task cancel writer——任务取消是项目取消的隐含语义（非 task 轴状态）。任务依赖（`dependsOn` DAG 成环检测）是**动态守卫**，保留 BizModel。
- **项目（ErpPrjProject.status）语义**（owner doc §适用对象一）：5 态 DRAFT/OPEN/ON_HOLD/COMPLETED/CANCELLED；终态 = COMPLETED/CANCELLED；OPEN↔ON_HOLD 可往复；`cancelProject` 多源（DRAFT/OPEN/ON_HOLD→CANCELLED，拒绝 COMPLETED/CANCELLED 终态）。`validateStartPreconditions`（立项前置字段，config-gated STRICT/WARN）+ `validateTasksFinished`（完工时未结束任务检查，config-gated STRICT/WARN）是**动态守卫**，保留 BizModel/Processor。
- **dict 实况（无死状态）**：`erp-prj/task-status`（`module-projects/model/app-erp-projects.orm.xml:38-43`）= TODO/IN_PROGRESS/DONE/BLOCKED 4 值；`erp-prj/project-status`（`:31-37`）= DRAFT/OPEN/ON_HOLD/COMPLETED/CANCELLED 5 值。两 dict 值均有对应 writer（layer-2 复核）。
- **生产 writer 实况（固定迁移判断散布，已核实）**：
  - **Task**：`ErpPrjTaskBizModel`（`entity/`）：startTask `:111-115`（守卫 `Objects.equals(TODO)` `:111`→IN_PROGRESS）、completeTask `:126-129`（守卫 IN_PROGRESS→DONE）、blockTask `:142-149`（守卫 IN_PROGRESS→BLOCKED）、unblockTask `:160-163`（守卫 BLOCKED→IN_PROGRESS）；`illegalTransition` helper `:326-331`（领域码 `ERR_TASK_ILLEGAL_STATUS_TRANSITION` `ErpPrjErrors.java:181`）。**DAG 依赖校验分两处动态守卫**：成环检测在 save-time `validateDependency`（`:60-69,75-102`，非迁移时）、前置任务完成检查 `validatePredecessorDone` 在 startTask（`:114`，config-gated STRICT/WARN）——二者均保留 BizModel。
  - **Project**：`ErpPrjProjectBizModel`（`entity/`）：startProject `:94-99`（守卫 `Objects.equals(DRAFT)` `:94`→OPEN，前置 `validateStartPreconditions`）、cancelProject `:121-127`（多源守卫——拒绝 `COMPLETED/CANCELLED` 终态 `:121-122`→CANCELLED）；`ErpPrjProjectHoldProjectProcessor`：hold `:31-36`（守卫 OPEN→ON_HOLD）；`ErpPrjProjectResumeProjectProcessor`：resume `:31-36`（守卫 ON_HOLD→OPEN）；`ErpPrjProjectCloseProjectProcessor`：close `:53-67`（守卫 `Objects.equals(OPEN)` `:53`→COMPLETED，前置 `validateTasksFinished`）。
  - cancelProject 多源 + 终态拒绝触发 §11.4 警示「cancel 多来源态与终态领域异常重叠」——接线须令**终态**走领域码（保持既有外部错误码）、**非终态非法**走 Bean→领域映射（参照 M1.1 `ErpCsTicketBizModel.cancel` 范式）。**项目域共享单一错误码** `ERR_PROJECT_NOT_CLOSABLE`（`ErpPrjErrors.java:107`，start/cancel/Hold/Resume/Close 五处非法迁移均抛此码）；Hold/Resume 经参数化 `transition(expected, target)` helper（`:28-39`，`Objects.equals(status, expected)` 变量形式）——接线 grep 须含此 helper 形式，不只匹配 `Objects.equals(from, *_STATUS_*)` 字面常量。
- **既有层 3 回归基线（非 greenfield）**：`TestErpPrjProjectPrecheck`（startProject/cancelProject/closeProject 前置 + 状态迁移）、`TestErpPrjBudgetAndCollection`、`TestErpPrjProjectSettlement`、`TestErpPrjTimesheetCost`（均经 BizModel/IGraphQLEngine 入口）。M0.1 §10 登记的 8 个 `TestErp*StateMachine` 基线不含 projects——projects 域层 3 = 上述既有集成测试，**不是**命名矩阵测试（层 1 新增）。
- **领域错误码已存在**：`ErpPrjErrors.ERR_TASK_ILLEGAL_STATUS_TRANSITION`（task，`:181`）、`ERR_PROJECT_NOT_CLOSABLE`（project，`:107`，start/cancel/Hold/Resume/Close 共享）、`ERR_PROJECT_HAS_UNFINISHED_TASKS`（`:111`）、`ERR_PROJECT_START_PRECONDITION_FAILED`（`:115`）。common 层码 `ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION` 已存在（M1.1 Option A 复用 + `action` 补充参数范式）。
- **生产 Bean 注册范式已存在**：`module-projects/erp-prj-service/src/main/resources/_vfs/erp/prj/beans/app-service.beans.xml` 已以 `<bean id="<FQN>" class="<FQN>"/>` 注册 per-mutation Processor（Hold/Resume/Close 等）。StateMachine Bean 沿用此范式。
- **合规基线**：R5（`@Inject private`）= 0、R11= 0。本计划新增 2 Bean 注册 + 注入须保持 R5=0；接线后内联守卫收敛，R11 不增。

## Goals

- 落地 `ErpPrjTaskStateMachine`（4 态任务轴）+ `ErpPrjProjectStateMachine`（5 态项目轴）两个独立 Bean，各承载已实现迁移矩阵 + 终态/初始态分类 + 只读 `transitions()` 元数据，严格无状态、可经 Delta 同名覆盖。
- 将 `ErpPrjTaskBizModel`（start/complete/block/unblock）与 `ErpPrjProjectBizModel`（start/cancel）+ Hold/Resume/Close Processor 的**固定来源态/目标态判断**改调 Bean；动态业务守卫（DAG 依赖成环检测、`validateStartPreconditions`/`validateTasksFinished` config-gated STRICT/WARN、成本归集、乐观锁）保留原位。
- 保持全部既有外部行为不变（错误码、cancel 多源 + 终态拒绝、config-gated 前置校验、DAG 校验）。
- 各新增层 1 矩阵完备性表驱动测试；层 3 既有集成测试回归全绿。
- 层 2 四方对照确认两 dict 无死状态、owner-doc 迁移图与元数据一致；任何漂移按 Fix/Decision 登记。

## Non-Goals

- 不修改任何 `model/*.orm.xml` / `model/*.api.xml` / 字典 yaml（路线图 Non-Goal）：**不向 task dict 新增 CANCELLED**（任务取消是项目取消的隐含语义，非 task 轴状态）。
- 不改变任何业务状态值、动作名、错误码值、权限、config-gated STRICT/WARN 语义、DAG 校验（路线图 Non-Goal「不借迁移改变既有行为」）。
- 不迁移 `ErpPrjTimesheet.status`（= M3.10）、`ErpPrjProjectSettlement.docStatus/approveStatus`（= M3.11/M3.12）、`ErpPrjMilestone/ErpPrjBilling/ErpPrjCostCollection`（owner doc §适用对象三 Deferred CRUD 桩实体，非本计划结果表面）。
- 不引入全局 CRUD 写锁（M0.1 §9 选项 c；更强写锁 successor）。
- 不重构 DAG 成环检测算法或前置校验逻辑（动态守卫原位保留）。

## Task Route

- Type: `implementation-only change`（消费 M0.1 契约 + M1.3 模板 + M0.2 清单，落地两轴 Bean + 接线 + 三层测试 + 四方对照；不改契约/模型/公共 API）
- Owner Docs: `docs/architecture/entity-state-machine-bean.md`（M0.1 契约 + §11 迁移模板）、`docs/design/projects/state-machine.md`（§适用对象一 Project + §适用对象二 Task + §多源 cancel 声明 `:40`）、`docs/design/projects/task-dag.md`（DAG 依赖动态守卫边界）、`docs/analysis/2026-08-12-entity-state-axis-inventory.md`（M0.2 清单 projects 行）、`docs/architecture/processor-extension-pattern.md`
- Skill Selection Basis: 路线图 M2.3/M2.4 指定 `nop-backend-dev` + `nop-testing`。`nop-backend-dev` 匹配「BizModel/Processor 接线、Bean 注册、动态守卫边界保留」；`nop-testing` 匹配「矩阵表驱动测试 + 既有集成测试回归」。层 2 引用 `state-machine-business-review-prompt.md`（模板步骤 5 标配）。必需输入已就绪。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（纯后端 Java + 既有 prj-service 测试容器）。
- 前置依赖：M0.1 + M0.2 + M1.3 done。均已满足。

## Execution Plan

### Phase 1 - ErpPrjTaskStateMachine + ErpPrjProjectStateMachine Bean + 注册 + 层 1 矩阵完备性测试

Status: completed
Targets: `module-projects/erp-prj-service/src/main/java/app/erp/prj/service/statemachine/ErpPrjTaskStateMachine.java`（新）+ `ErpPrjProjectStateMachine.java`（新）；`.../beans/app-service.beans.xml`（追加 2 Bean 注册）；`TestErpPrjTaskStateMachineMatrix.java` + `TestErpPrjProjectStateMachineMatrix.java`（新，层 1）
Skill: `nop-backend-dev`（Bean 形状/注册）+ `nop-testing`（层 1 表驱动测试）

- Item Types: `Add | Proof`
- Prereqs: M0.1 + M0.2 + M1.3 done

- [x] `Add`：创建 `ErpPrjTaskStateMachine`（无状态），矩阵：`assertCanStart(TODO)`/`assertCanComplete(IN_PROGRESS)`/`assertCanBlock(IN_PROGRESS)`/`assertCanUnblock(BLOCKED)` + 目标态方法 + `isTerminal(DONE)` + `transitions()`（4 边：start/complete/block/unblock）+ `terminalStatuses()`(DONE) + `initialStatuses()`(TODO)。Skill: `nop-backend-dev`
- [x] `Add`：创建 `ErpPrjProjectStateMachine`（无状态），矩阵：`assertCanStart(DRAFT)`/`assertCanHold(OPEN)`/`assertCanResume(ON_HOLD)`/`assertCanClose(OPEN)`/`assertCanCancel(DRAFT|OPEN|ON_HOLD)`（多源，拒绝 COMPLETED/CANCELLED 终态）+ 目标态方法 + `isTerminal(COMPLETED|CANCELLED)` + `transitions()`（start/hold/resume/close 各 1 + cancel 3 来源 = 7 边）+ `terminalStatuses()`(COMPLETED/CANCELLED) + `initialStatuses()`(DRAFT)。非法来源态抛 common 层码 + `action`/`fromStatus` 元数据。Skill: `nop-backend-dev`
- [x] `Add`：在 `app-service.beans.xml` 以 FQN id 注册两个 Bean（沿用既有 Processor 范式，§11.1 步骤 2）。Skill: `nop-backend-dev`
- [x] `Proof`（层 1 矩阵完备性，新增 greenfield 表驱动测试，§11.1 步骤 4）：`TestErpPrjTaskStateMachineMatrix` 覆盖 start/complete/block/unblock 合法/非法来源态 + IN_PROGRESS↔BLOCKED 往复 + DONE 终态无出边 + transitions 元数据一致；`TestErpPrjProjectStateMachineMatrix` 覆盖 (a) 无重复/冲突边；(b) 从 DRAFT 可达全部 4 非初始态、COMPLETED/CANCELLED 终态无出边；(c) cancel 多源 {DRAFT,OPEN,ON_HOLD} 合法、对终态非法；(d) transitions 元数据一致；(e) 终态/初始态集合正确。**不经 BizModel 入口**（层 1 只测 Bean）。Skill: `nop-testing`

Exit Criteria:

- [x] 两 Bean 落地（Task 4 动作 + Project 5 动作 + 目标态 + isTerminal + transitions 元数据），无状态（grep 证实不 import DAO/IBiz/IServiceContext/事务）。
- [x] 两 Bean 已在 `app-service.beans.xml` 注册（FQN id）；`@Inject` 字段非 private（合规 R5）。
- [x] 层 1 矩阵测试 `mvn test -pl module-projects/erp-prj-service -Dtest=TestErpPrjTaskStateMachineMatrix,TestErpPrjProjectStateMachineMatrix` 全绿。
- [x] 本地化编译检查：`mvn compile -pl module-projects/erp-prj-service -am` 通过（解除 Phase 2 接线依赖）。

### Phase 2 - BizModel/Processor 接线（行为保持）+ 层 3 回归

Status: completed
Targets: `ErpPrjTaskBizModel.java`（start/complete/block/unblock）；`ErpPrjProjectBizModel.java`（start/cancel）；`ErpPrjProjectHoldProjectProcessor.java`/`ErpPrjProjectResumeProjectProcessor.java`/`ErpPrjProjectCloseProjectProcessor.java`
Skill: `nop-backend-dev`（接线 + 错误码映射）+ `nop-testing`（回归断言）

- Item Types: `Fix | Proof`
- Prereqs: Phase 1

- [x] `Fix`：Task BizModel 注入 `ErpPrjTaskStateMachine`，将 start/complete/block/unblock 内联守卫替换为 `stateMachine.assertCan<Action>(from)` + 目标态写回；删除私有 `illegalTransition` 矩阵部分。**动态守卫保留原位**：DAG 依赖成环检测（startTask 内 `dependsOn` 校验）。Processor/BizModel 捕获 Bean common 层非法边映射为领域 `ERR_TASK_ILLEGAL_STATUS_TRANSITION`（common 码作 cause）。Skill: `nop-backend-dev`
- [x] `Fix`：Project BizModel 注入 `ErpPrjProjectStateMachine`，将 start 内联守卫替换为 Bean 调用；cancel 多源改 `stateMachine.assertCanCancel(from)`——**终态 {COMPLETED,CANCELLED} 走领域码（保持既有外部错误码）、非终态非法走 Bean→领域映射**（参照 §11.4 + M1.1 cancel 范式防冲突）。Hold/Resume/Close Processor 注入 Bean 替换内联守卫。**动态守卫保留原位**：`validateStartPreconditions`（start）、`validateTasksFinished`（close）、config-gated STRICT/WARN、成本归集、乐观锁。Skill: `nop-backend-dev`
- [x] `Proof`（层 3 既有回归保持全绿）：`mvn test -pl module-projects/erp-prj-service` 全绿——重点 `TestErpPrjProjectPrecheck`（start/cancel/close 前置 + 状态迁移 + config-gated）、`TestErpPrjBudgetAndCollection`、`TestErpPrjProjectSettlement`。证明错误码、cancel 多源 + 终态拒绝、config-gated STRICT/WARN、DAG 校验均不变。Skill: `nop-testing`

Exit Criteria:

- [x] Task 4 处 + Project 5 处（BizModel start/cancel + Hold/Resume/Close）固定判断均改调 Bean，grep 证实相关方法体内不再有内联 `Objects.equals(from, *_STATUS_*)` 矩阵判断（动态守卫如 DAG/prevalidate/终态领域判定除外）。
- [x] 领域错误码 + 参数对外不变（层 3 断言证实）；Project cancel 多源 + 终态拒绝行为不变。
- [x] 层 3 `mvn test -pl module-projects/erp-prj-service` 全绿。

### Phase 3 - 层 2 四方对照（Task + Project 双轴）+ Delta 适用性

Status: completed
Targets: 四方对照审计记录（写入本计划 Closure 段）；项目域单轴 Delta 证据
Skill: `state-machine-business-review-prompt.md`（四方对照 + 10 维度）+ `nop-testing`（Delta 双加载）

- Item Types: `Proof | Decision | Fix | Add`
- Prereqs: Phase 2

- [x] `Proof`（四方对照，§11.1 步骤 5）：以 `state-machine-business-review-prompt.md` 10 维度审查 Task + Project 双轴——dict（task-status 4 值 / project-status 5 值）↔ owner-doc §适用对象一/二 迁移图 ↔ 两 Bean `transitions()` ↔ 全部 writer（含 CRUD 路径）。确认两 dict 无死状态；复核 owner doc §多源 cancel 声明（`:40`）与 Bean 一致；复核 task「CANCELLED 隐含于项目取消」语义不在 task 轴编码（无 dict 值 = 一致）。Skill: `state-machine-business-review-prompt.md`
- [x] `Decision`（漂移裁定）：对四方对照任何不一致逐条分类（implementation drift / doc drift / intentional legacy）并指派 successor；已确认缺陷/契约漂移 = Fix（不得降级 Follow-up）。预期两轴干净（无死状态），若发现 owner-doc §迁移表 vs §实现约定 内部漂移按 §11.4 补正。Skill: `state-machine-business-review-prompt.md`
- [x] `Add | Proof`（Delta 适用性，§11.1 步骤 7；M2 非保护域）：在 Project 轴证 Delta（派生类覆盖一个动作，如收紧 cancel 仅 OPEN，移除 DRAFT/ON_HOLD 源），VFS Delta 层同名 bean id 覆盖，基线/Delta 双加载可区分（复用 M1.2 范式）。Task 轴继承 Project 轴 + M1.2 既有证明，不重复证。Skill: `nop-testing`

Exit Criteria:

- [x] 双轴四方对照审计记录存在且非空，每维有可追溯结论（引用 Bean 元数据 / owner doc 章节 / dict 位置 / writer 类:行）。
- [x] 不一致项（若有）已按 Fix/Decision 登记 + successor，无静默排除。
- [x] Project 轴 Delta 双加载运行时证据存在（非静态检查），基线/Delta 可区分。

## Draft Review Record

- Independent draft review iteration 1: `acceptable as-is` (`ses_00bb346ffffe4nzztzKL6kvI5m`) — 无 BLOCKER、无 MAJOR。规则 14 bundling（Task + Project）判定 **justified**（同 owner doc + 同结果表面 + 同验证路径，不拆分）。全部 load-bearing 声明经独立复核 CONFIRMED TRUE（task-status 4 值 / project-status 5 值、Task/Project writer 行号、task 无 CANCELLED 隐含于项目取消、错误码、层 3 基线、common 码、beans 注册范式、M2 非保护非 plan-first、项目状态轴不触发过账）。MINOR 已就地修正：DAG 成环检测在 save-time `validateDependency`（非 startTask）+ startTask 内为前置完成检查；项目域共享单码 `ERR_PROJECT_NOT_CLOSABLE`（5 处）+ Hold/Resume 参数化 helper grep 注意；illegalTransition 行号 :326-331。反松弛扫描 clean。草案审查收敛，Plan Status → active。

## Closure Gates

> 本计划含生产代码变更（2 Bean + 接线 + 测试），Closure Gates 运行完整仓库验证。

- [x] 范围内行为完成（Task + Project 双轴 Bean + 接线 + 层 1 矩阵 + 层 3 回归 + 层 2 四方对照 + Delta 证据）
- [x] 相关文档对齐（`projects/state-machine.md` 漂移补正若有；路线图 M2.3 + M2.4 done）
- [x] 已运行验证：`mvn clean install -DskipTests`（全仓库 BUILD SUCCESS）+ `mvn test -pl module-projects/erp-prj-service`（102 用例全绿）+ `bash docs/audits/nop-compliance-checker.sh`（exit 0，R5=0/R11=0 无漂移）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：Plan Status、各 Phase Status、Exit Criteria、Closure Gates、日志一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### task CANCELLED 状态

- Classification: `watch-only residual`
- Why Not Blocking Closure: owner doc §适用对象二 提及任务取消「隐含于项目取消」，但 task dict 无 CANCELLED 值、零 task cancel writer——任务取消是项目取消的隐含业务语义（非 task 轴独立状态）。Bean 如实不编码 CANCELLED。
- Successor Required: yes（触发条件 = PM 要求任务独立取消命名动作时，新增 dict 值 + cancelTask mutation，触及 ORM 保护区 ask-first）

### projects 域其余状态轴（Timesheet / ProjectSettlement / CRUD 桩实体）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: ErpPrjTimesheet.status = M3.10、ErpPrjProjectSettlement.docStatus/approveStatus = M3.11/M3.12（独立 plan）；ErpPrjMilestone/Billing/CostCollection = owner doc §适用对象三 Deferred CRUD 桩（零 writer， successor）。
- Successor Required: yes（触发条件 = 各对应 M3 工作项启动时）

## Closure

Status Note: 执行完成（2026-08-12）。Phase 1-3 全部落地：两轴 StateMachine Bean（Task 4 边 / Project 7 边）+ 注册 + 层 1 矩阵测试（18 用例全绿）；BizModel + Hold/Resume/Close Processor 接线完成（动态守卫原位保留），层 3 既有集成测试全绿（95 用例）；层 2 四方对照双轴干净（无死状态、无漂移）；Project 轴 Delta 双加载运行时证据存在。

### 层 2 四方对照审计记录（§11.1 步骤 5，`state-machine-business-review-prompt.md` 10 维度）

> 审查方法：四方对照（dict ↔ owner-doc 迁移图 ↔ StateMachine `transitions()` 元数据 ↔ 全部 writer 含 CRUD 路径 §9.4）+ 10 维度业务审查。

#### Task 轴四方对照（`erp-prj/task-status`）

| 源 | 值/边 | 位置 |
|---|---|---|
| dict `erp-prj/task-status` | TODO / IN_PROGRESS / DONE / BLOCKED（4 值） | `module-projects/model/app-erp-projects.orm.xml:38-43` |
| owner-doc §适用对象二 迁移图 | start(TODO→IN_PROGRESS)、complete(IN_PROGRESS→DONE)、block(IN_PROGRESS→BLOCKED)、unblock(BLOCKED→IN_PROGRESS)；DONE 终态无出边 | `docs/design/projects/state-machine.md:120-127` |
| Bean `transitions()` | 4 边（与上同） | `ErpPrjTaskStateMachine.java:77-81` |
| 生产 writer（命名动作） | startTask `ErpPrjTaskBizModel.java:119`、completeTask `:131`、blockTask `:150`、unblockTask `:162`（均经 `stateMachine.<action>TargetStatus()` 写回；守卫经 `assertCan` helper `:331-347` 调 Bean） | 4 处 |
| CRUD 路径（§9.4） | xmeta `status` insertable/updatable（M0.1 §9.1 全局结论；通用 CRUD 可写状态字段，不在矩阵运行时强制范围，option c 显式排除） | M0.1 §9.2 |

**一致性**：4 dict 值 = 4 owner-doc 边 = 4 Bean 边 = 4 writer。**无死状态**：每个 dict 值可达——TODO 初始、IN_PROGRESS（start 入）、DONE（complete 入，终态）、BLOCKED（block 入）。DONE 终态无出边（层 1 矩阵测试 `testDoneTerminalHasNoOutgoingAction` 证实）。任务「CANCELLED 隐含于项目取消」：task dict 无 CANCELLED 值、零 task cancel writer，Bean 如实不编码——**一致**（非死状态，是非 task 轴的隐含业务语义）。

#### Project 轴四方对照（`erp-prj/project-status`）

| 源 | 值/边 | 位置 |
|---|---|---|
| dict `erp-prj/project-status` | DRAFT / OPEN / ON_HOLD / COMPLETED / CANCELLED（5 值） | `module-projects/model/app-erp-projects.orm.xml:31-37` |
| owner-doc §适用对象一 迁移图 + §多源 cancel 声明 `:40` | start(DRAFT→OPEN)、hold(OPEN→ON_HOLD)、resume(ON_HOLD→OPEN)、close(OPEN→COMPLETED)、cancel{DRAFT/OPEN/ON_HOLD→CANCELLED}（7 边） | `docs/design/projects/state-machine.md:21-40` |
| Bean `transitions()` | 7 边（与上同） | `ErpPrjProjectStateMachine.java:97-105` |
| 生产 writer（命名动作） | startProject `ErpPrjProjectBizModel.java:98`、cancelProject `:128`（BizModel）；hold `ErpPrjProjectHoldProjectProcessor.java:35`、resume `ErpPrjProjectResumeProjectProcessor.java:35`、close `ErpPrjProjectCloseProjectProcessor.java:72`（Processor）——均经 `stateMachine.<action>TargetStatus()` 写回；守卫经 `assertCanCancel`/`assertCan<Action>` 调 Bean | 5 处 |
| CRUD 路径（§9.4） | xmeta `status` insertable/updatable（option c 显式排除） | M0.1 §9.2 |

**一致性**：5 dict 值 = 5 owner-doc 状态 = 7 Bean 边 = 7 writer。**无死状态**：DRAFT 初始、OPEN（start 入）、ON_HOLD（hold 入）、COMPLETED（close 入，终态）、CANCELLED（cancel 入，终态）。cancel 多源 {DRAFT,OPEN,ON_HOLD} 与 owner-doc §多源 cancel 声明（`:40`）一致。COMPLETED/CANCELLED 终态无出边（层 1 矩阵测试 `testTerminalStatusesHaveNoOutgoingEdges` 证实）。

#### 10 维度业务审查（双轴）

- **D1 状态定义**：4+5 状态均清晰表达「等待 X」业务等待点，无「动作作为状态」。Task DONE = 等待（无）/终态；Project COMPLETED/CANCELLED 终态。
- **D2 迁移完整性**：每状态进/出边齐全（见四方表）。cancel 多源在 owner-doc §多源 cancel 声明（`:40`）+ Bean 显式编码 3 来源。
- **D3 终态与恢复**：Task DONE / Project COMPLETED,CANCELLED 无出边。owner-doc §3 明确终态不可直接恢复，重启 = 新建项目（intentional，Bean 如实不编码 reactivation 边）。
- **D4 异常路径**：STRICT/WARN config-gated 三处（startProject `validateStartPreconditions`、closeProject `validateTasksFinished`、startTask `validatePredecessorDone`）+ DAG 成环检测（`TaskDependencyValidator`）+ 乐观锁，均保留原位。重复迁移 → 领域码非法迁移异常（幂等：同态再迁移即拒）。
- **D5 可达性**：从初始态（TODO/DRAFT）全部声明状态可达；IN_PROGRESS↔BLOCKED、OPEN↔ON_HOLD 合法往复均有终态退出条件（DONE/COMPLETED/CANCELLED）；无不可达状态、无死锁。
- **D6 角色权限**：owner-doc §6 定义执行角色（项目经理/管理员）；Bean 严格无状态不读用户（契约 §2），权限经 xbiz auth/Processor 层（本计划不改权限，超出范围）。
- **D7 外部依赖**：项目成本归集经 `IErpFinAcctDocProvider` 是独立业财面（计划 §14 明示非项目头状态轴触发）；Bean 持零跨域耦合。
- **D8 TODO 策略**：owner-doc §8 定义，非 Bean 关注。
- **D9 场景演练**：happy path（立项→执行→完成）+ 暂停/恢复 + 取消已在 owner-doc §9 走查。
- **D10 设计文档一致性**：owner-doc §适用对象一/二 与 Bean + dict 一致，无漂移。
- **D11 dict 可达性**：两 dict 全部值均有 `setStatus` writer（见四方表 writer 列），**零死状态**。Deferred 项（Milestone/Billing/CostCollection CRUD 桩）= owner-doc §适用对象三 Deferred，超出本计划范围（M3 successor，已在 Deferred But Adjudicated 登记）。

**裁决：Verdict: pass（无 P0/P1 发现；双轴干净）**。不一致项 = 0，无 Fix/Decision 需登记。唯一 intentional-legacy 注记：task CANCELLED 隐含于项目取消（非 task 轴状态），dict 无该值 = 一致。

### Delta 适用性证据（§11.1 步骤 7，M2 非保护域；Project 轴）

Project 轴 Delta 双加载运行时证据（复用 M1.2 范式）：

- **Delta 派生类**：`ErpPrjProjectStateMachineDelta`（测试作用域）覆盖 `assertCanCancel`——基线允许 {DRAFT/OPEN/ON_HOLD}，Delta 收紧为仅 {OPEN}（暂停项目须先恢复才能取消、草稿项目不可取消）。仅覆盖 1 方法，其余继承基线。
- **VFS Delta 层**：`module-projects/erp-prj-service/src/test/resources/_vfs/_delta/test-prj-delta/erp/prj/beans/app-service.beans.xml` 以同名 bean id `app.erp.prj.service.statemachine.ErpPrjProjectStateMachine` 覆盖基线为派生类。
- **双加载测试（运行时证据，非静态检查）**：
  - `TestErpPrjProjectStateMachineBaselineIoC`（4 用例）：容器解析 = 基线类（非 Delta），`assertCanCancel(DRAFT)` **放行**，`assertCanCancel(COMPLETED/CANCELLED)` 抛 common 码。
  - `TestErpPrjProjectStateMachineDeltaOverride`（3 用例，`@NopTestProperty nop.core.vfs.delta-layer-ids=test-prj-delta`）：容器解析 = `ErpPrjProjectStateMachineDelta` 派生类，`assertCanCancel(DRAFT)` **抛异常**（收紧，与基线可区分），`assertCanCancel(OPEN)` 放行，非覆盖动作（start/hold/resume/close/isTerminal）继承基线不变。
  - 可区分差异点：同一 `assertCanCancel(DRAFT)` 基线放行、Delta 抛异常 → 基线/Delta 双加载证据成立。
- Task 轴继承 Project 轴 + M1.2 既有证明（同机制、同容器），不重复证（计划 Phase 3 明示）。

Closure Audit Evidence:

- Executor / Agent: 执行者（本会话）完成 Phase 1-3 全部代码 + 测试 + 四方对照 + Delta 证据。
- 验证证据：
  - 层 1 矩阵：`mvn test -pl module-projects/erp-prj-service -Dtest=TestErpPrjTaskStateMachineMatrix,TestErpPrjProjectStateMachineMatrix` → 18 用例全绿。
  - Delta 双加载：`TestErpPrjProjectStateMachineBaselineIoC`（4）+ `TestErpPrjProjectStateMachineDeltaOverride`（3）→ 7 用例全绿。
  - 层 3 回归：`mvn test -pl module-projects/erp-prj-service` → 95 用例全绿（含 `TestErpPrjProjectPrecheck` 6 例、`TestErpPrjTaskDependency` 12 例等）。
  - 全仓库构建：`mvn clean install -DskipTests` → BUILD SUCCESS（两次，Phase 1 后 + Phase 2 后）。
  - 状态性 grep：两 Bean 不 import DAO/IBiz/IServiceContext/事务（仅 Javadoc 提及）。
  - 接线 grep：Task 4 + Project 5 迁移方法体内无内联 `Objects.equals(from, *_STATUS_*)` 矩阵判断（`requireReferenceable` 引用校验除外）。
- Auditor / Agent: 独立结束审计子代理 `ses_00b6fd390ffetrrucJeA2J2HuU`（新会话，2026-08-12）
- Evidence: 独立审计 Verdict: pass（无 P0/P1 发现）。审计独立复核证实：两 Bean 严格无状态（grep 0 违例）、已注册、Task/Project 接线动态守卫保留、cancel 多源+终态拒绝行为不变、`requireReferenceable` 引用校验正确不接线、Delta 双加载运行时证据成立（cancel(DRAFT) 基线放行/Delta 抛异常可区分）、ORM/dict 保护区无改动、层 1 矩阵 18 + Delta 7 + 层 3 回归（ProjectPrecheck 6 + TaskDependency 12）全绿、compliance exit 0/R5=0/R11=0。审计记录详见本会话 task 输出。

Follow-up:

- <无非阻塞跟进；Deferred 项（task CANCELLED / projects 域其余状态轴）已在 Deferred But Adjudicated 登记并指派 successor 触发条件>
