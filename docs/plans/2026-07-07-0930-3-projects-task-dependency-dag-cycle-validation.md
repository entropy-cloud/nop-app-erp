# 2026-07-07-0930-3 projects-task-dependency-dag-cycle-validation

> Plan Status: completed
> Last Reviewed: 2026-07-07
> Source: `docs/backlog/extended-roadmap.md` 工作项 2.6c（UC-PRJ-05 任务依赖 DAG 成环校验，`todo`）
> Related: `2026-07-03-1018-2-projects-cost-collection.md`（项目域成本归集前置，工时/采购归集时校验项目状态但**不**校验任务依赖）、`2026-07-07-0305-1-projects-pnl-settlement-capitalization.md`（同期 projects 域深化；任务状态校验在 0305-1 之外）
> Audit: required

## Current Baseline

- **任务实体与依赖列已就绪但无校验**：`ErpPrjTask`（`module-projects/model/app-erp-projects.orm.xml:158`）含 `dependsOnId` 列（:173，BIGINT，单前置依赖，`stdDataType="long"`）+ `dependsOn` to-one 关系（:190，自引用 FK）+ `parentTaskId`/`parentTask`（父子任务树，与依赖关系正交）+ `status` 列（绑定字典 `erp-prj/task-status`，4 态 TODO/IN_PROGRESS/BLOCKED/DONE，确认存在）。`grep dependsOn module-projects/erp-prj-service` 返回 0 命中（除 ORM 关系 getter）—— **无任何业务代码消费 dependsOn 列**：无前置任务未完成拦截、无成环校验。
- **任务 BizModel 为空壳**：`module-projects/erp-prj-service/src/main/java/app/erp/prj/service/entity/ErpPrjTaskBizModel.java` 仅 `extends CrudBizModel<ErpPrjTask>`，无业务方法。`IErpPrjTaskBiz` 仅 `ICrudBiz`。
- **任务状态字典存在**：`erp-prj/task-status`（TODO/IN_PROGRESS/BLOCKED/DONE，4 态，与 `state-machine.md §任务` 一致）。
- **设计分散且 task-dag.md 不存在**：owner 设计在 `docs/design/projects/state-machine.md §适用对象二：任务` 与 `§任务依赖规则`（约 20 行）+ `use-cases.md §UC-PRJ-05`（约 15 行），分散两处且缺算法细节（DFS 三色标记 vs 上行链追溯、单前置 vs 多前置 DAG 表、成环深度上限、自环 A→A 防护）。`grep task-dag docs/design/projects` 返回 0 命中 —— **`projects/task-dag.md` 不存在**（roadmap 目标路径）。
- **依赖模型为单前置（树/森林），非真 DAG**：`dependsOnId` 单列限制每任务最多一个前置；`use-cases.md §UC-PRJ-05`「依赖关系 DAG 成环」用 DAG 术语但模型实为单链。Decision 项（Phase 1）：(a) 维持单前置 + 上行链成环检测（最小 ORM churn，覆盖单链语义）；(b) 新增 `ErpPrjTaskDependency` 多对多表（真 DAG，需 DFS）。本期建议 (a)。
- **平台乐观锁范式已就绪**：`ErpPrjTask` 含 `version` 列（标准八列），并发依赖编辑可经乐观锁兜底（0024-2 范式）。
- **剩余差距**：(1) 任务 TODO→IN_PROGRESS 迁移时未校验 dependsOn.status==DONE；(2) 保存任务 dependsOn 时未做成环校验（A→B→A、自环 A→A、长链成环）；(3) task-dag.md 设计文档缺失。

## Goals

- 创建 `docs/design/projects/task-dag.md` owner 设计文档：将 `state-machine.md §任务` + `use-cases.md §UC-PRJ-05` 中依赖相关内容收敛到独立文档，含算法选择（上行链 vs DFS）、单前置 vs 多前置 Decision、成环深度上限、跨项目任务依赖 Non-Goal。
- 实现任务依赖保存校验：`ErpPrjTaskBizModel.defaultPrepareSave`/`defaultPrepareUpdate` 钩子 + 显式 `validateDependency(taskId, dependsOnId, ctx)` 方法 —— 自环（taskId==dependsOnId）拒绝 `ERR_TASK_SELF_DEPENDENCY`；上行链追溯若 revisit（成环）拒绝 `ERR_TASK_DEPENDENCY_CYCLE`；深度超过 `erp-prj.task-dependency-max-depth`（默认 100）抛 `ERR_TASK_DEPENDENCY_DEPTH_EXCEEDED`（防恶意长链耗尽栈/堆）。
- 实现任务状态迁移校验：`@BizMutation startTask(@Name("taskId") Long taskId, IServiceContext context)` —— TODO→IN_PROGRESS；前置任务 dependsOn.status != DONE 时拒绝 `ERR_TASK_PREDECESSOR_NOT_DONE`（参数 taskId/dependsOnTaskId/dependsOnTaskStatus）；config-gated `erp-prj.task-strict-predecessor-check`（默认 true；false 时仅 WARN 不阻断，经 `CoreMetrics` + 日志）。
- 实现任务状态迁移完整链：TODO→IN_PROGRESS（startTask）/ IN_PROGRESS→DONE（completeTask）/ IN_PROGRESS→BLOCKED（blockTask，可选 dependsOnStatus 提示）/ BLOCKED→IN_PROGRESS（unblockTask）—— 非法迁移抛 `ERR_TASK_ILLEGAL_STATUS_TRANSITION`（参数 taskId/currentStatus/targetStatus）。
- 提供依赖查询入口（`@BizQuery`）：`findPredecessors(taskId, ctx)`（上行链全量）/ `findSuccessors(taskId, ctx)`（下行反查全量，经 QueryBean filter dependsOnId==taskId 递归）/ `getDependencyChain(taskId, ctx)`（返回单链 `List<ErpPrjTask>`，对齐单前置模型——一任务至多一前置，至多一条线性链）。
- 解除 roadmap 2.6c `todo` → `done`。同时按规则 14 收口 `state-machine.md §任务` 所属的 TODO/IN_PROGRESS/BLOCKED/DONE 状态迁移方法（startTask/completeTask/blockTask/unblockTask）—— 与 UC-PRJ-05 同属 `ErpPrjTaskBizModel` 同一组件，状态迁移是依赖校验的前置门控（startTask 触发前置校验），合并避免分散。

## Non-Goals

- **多前置 DAG（`ErpPrjTaskDependency` 多对多表）**：本期维持单 `dependsOnId` 列；真 DAG（一任务依赖多前置）属 ORM 模型 successor。触发条件：项目计划要求一任务依赖多前置时（如测试任务依赖开发 + 文档两个前置）。
- **跨项目任务依赖**：任务 `dependsOn` 仅允许同 projectId 内（跨项目依赖经里程碑 `ErpPrjMilestone` 或手工协同）；本期校验同项目内。
- **关键路径计算（CPM）/ 任务最早最晚开始时间**：CPM 属项目排程高级面，依赖多前置模型；本期 Non-Goal。触发条件：CPM 排程需求上线时。
- **任务依赖可视化（甘特图/网络图）**：归前端 successor。
- **任务层级树（parentTaskId）的成环校验**：父子树成环（A.parentTaskId=B, B.parentTaskId=A）属另一类校验，本期仅做依赖关系成环；父子树防护归 successor（与依赖关系正交）。
- **任务依赖变更的审计日志/审批**：依赖编辑直接乐观锁，不做 audit trail（平台 NopSysChangeLog 自动记录实体变更已足够）。
- **任务延期/超期自动告警**：归通知 successor（0642-1 范式）。
- **任务工时与状态联动**（IN_PROGRESS 自动开工时记开工时间，DONE 自动结算工时）：本期仅维护 status 迁移；工时联动归 cost-collection successor（1018-2）。

## Task Route

- Type: `app-layer design change + implementation-only change`（owner 设计需收敛 task-dag.md；ORM 无变更，纯 BizModel/IBiz 加性；多步校验非多步编排——单步操作直接 BizModel 方法，无需 Processor）
- Owner Docs: `docs/design/projects/state-machine.md §任务`（既有，待引用）、`docs/design/projects/use-cases.md §UC-PRJ-05`（既有，待引用）、新建 `docs/design/projects/task-dag.md`（owner 收敛）
- Skill Selection Basis: 后端 BizModel/IBiz/ErrorCode/CrudBizModel 钩子 + 无多步编排（单步校验）→ 加载 `nop-backend-dev`；测试经 `JunitAutoTestCase`+IGraphQLEngine → 加载 `nop-testing`。两技能必需输入（owner 设计 state-machine.md/use-cases.md 既有，ErpPrjTask ORM 既有）均就绪。

## Infrastructure And Config Prereqs

- 无新外部端口/密钥/数据迁移依赖；无 ORM 变更；无 codegen 增量（仅 BizModel 方法扩展）。
- 新增配置键遵循 projects 域两文件约定（`ErpPrjConstants` 字符串键 + `ErpPrjConfigs` 默认值/读取方法，对齐 1018-2/0305-1 范式）：`ErpPrjConstants.CONFIG_TASK_DEPENDENCY_MAX_DEPTH`（字符串 `"erp-prj.task-dependency-max-depth"`）+ `ErpPrjConstants.CONFIG_TASK_STRICT_PREDECESSOR_CHECK`；`ErpPrjConfigs.DEFAULT_TASK_DEPENDENCY_MAX_DEPTH = 100` + `DEFAULT_TASK_STRICT_PREDECESSOR_CHECK = true` + reader 方法 `taskDependencyMaxDepth()` / `taskStrictPredecessorCheck()`。
- 无新业务类型（无业财过账）。

## Execution Plan

### Phase 1 - owner 设计收敛：task-dag.md 文档

Status: completed
Targets: `docs/design/projects/task-dag.md`（新建）、`docs/design/projects/state-machine.md`（引用 task-dag.md）、`docs/design/projects/use-cases.md`（引用 task-dag.md）
Skill: none

- Item Types: `Decision | Add`
- Prereqs: 无

- [x] Decision: 依赖模型选择。**选择**：(a) 维持单 `dependsOnId` 列（最小 ORM churn；覆盖单前置语义；上行链成环检测 O(N)）。**替代**：(b) 新增 `ErpPrjTaskDependency` 多对多表（真 DAG，需 DFS 三色标记 O(V+E)；ORM churn 大；本期 Non-Goal）。**残留风险**：业务场景若要求一任务依赖多前置（如测试任务依赖开发 + 文档），需后续 (b) 落地 + 数据迁移；本期 (a) 已能覆盖 80% 项目计划场景。
  - Skill: none
- [x] Decision: 成环检测算法。**选择**：上行链追溯 + HashSet revisit 检测（自当前 taskId 出发，沿 dependsOnId 链向上遍历，遇自身 taskId → 成环；遇 null → 无环；步数 > maxDepth → 深度超限）。**替代**：(a) DFS 三色标记（适合多对多 DAG，单前置下冗余，rejected）；(b) 全表加载 + 拓扑排序（性能差，rejected）。**残留风险**：上行链每次保存时 O(N) 查询，N=链长；maxDepth=100 兜底；并发编辑经乐观锁兜底（不在保存时持锁遍历）。
  - Skill: none
- [x] Add: 新建 `docs/design/projects/task-dag.md`：从 `state-machine.md §任务` + `use-cases.md §UC-PRJ-05` 收敛并扩展：(1) 依赖模型 Decision（单前置 vs 多前置，本期单前置）；(2) 成环检测算法（上行链 + HashSet + maxDepth）；(3) 状态迁移规则（TODO→IN_PROGRESS 校验 dependsOn.status==DONE，config-gated STRICT/WARN）；(4) 跨项目 Non-Goal；(5) CPM/可视化 Non-Goal。在 `state-machine.md §任务` 与 `use-cases.md §UC-PRJ-05` 末尾加「详细机制见 `task-dag.md`」引用。
  - Skill: none

Exit Criteria:

- [x] `task-dag.md` 创建并通过格式审查；`state-machine.md`/`use-cases.md` 引用更新。
- [x] 无需 codegen（设计文档 only）。

### Phase 2 - ErpPrjTaskBizModel 校验与状态机方法实现

Status: completed
Targets: `IErpPrjTaskBiz`、`ErpPrjTaskBizModel`、`ErpPrjConstants`、`ErpPrjConfigs`、`ErpPrjErrors`、`TaskDependencyValidator` 工具类
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 1

- [x] Add: `IErpPrjTaskBiz` 扩展方法：
  - `@BizMutation ErpPrjTask startTask(@Name("taskId") Long taskId, IServiceContext context)` —— TODO→IN_PROGRESS；校验 dependsOn（若非 null）status==DONE，否则 `ERR_TASK_PREDECESSOR_NOT_DONE`（config-gated strict/warn）；非法迁移 `ERR_TASK_ILLEGAL_STATUS_TRANSITION`。
  - `@BizMutation ErpPrjTask completeTask(@Name("taskId") Long taskId, IServiceContext context)` —— IN_PROGRESS→DONE；非法迁移 ErrorCode。
  - `@BizMutation ErpPrjTask blockTask(@Name("taskId") Long taskId, @Name("blockReason") String blockReason, IServiceContext context)` —— IN_PROGRESS→BLOCKED；blockReason 必填（`ERR_TASK_BLOCK_REASON_REQUIRED`）。
  - `@BizMutation ErpPrjTask unblockTask(@Name("taskId") Long taskId, IServiceContext context)` —— BLOCKED→IN_PROGRESS。
  - `@BizQuery List<ErpPrjTask> findPredecessors(@Name("taskId") Long taskId, IServiceContext context)` —— 上行链全量（经 `TaskDependencyValidator.collectPredecessors`）。
  - `@BizQuery List<ErpPrjTask> findSuccessors(@Name("taskId") Long taskId, IServiceContext context)` —— 下行反查全量（经 QueryBean filter dependsOnId==taskId 递归）。
  - `@BizQuery List<ErpPrjTask> getDependencyChain(@Name("taskId") Long taskId, IServiceContext context)` —— 单链全量（对齐单前置模型，至多一条线性链；多前置 successor 落地后扩展为 `List<List<>>`）。
  - Skill: `nop-backend-dev`
- [x] Add: `ErpPrjTaskBizModel` 钩子扩展：
  - 覆盖 `defaultPrepareSave` + `defaultPrepareUpdate`：当 `entity.dependsOnId` 变化或非 null 时，调 `TaskDependencyValidator.validate(taskId, dependsOnId, ctx)` —— 自环拒绝 `ERR_TASK_SELF_DEPENDENCY`；成环拒绝 `ERR_TASK_DEPENDENCY_CYCLE`（参数 chain 描述）；深度超限 `ERR_TASK_DEPENDENCY_DEPTH_EXCEEDED`；跨项目拒绝 `ERR_TASK_DEPENDENCY_CROSS_PROJECT`。
  - Skill: `nop-backend-dev`
- [x] Add: `TaskDependencyValidator` 工具类（`module-projects/erp-prj-service/src/main/java/app/erp/prj/service/validator/`）—— 纯函数式校验 + 上行链遍历：`validate(taskId, dependsOnId, this::loadTask)`（注入加载函数便于单测）+ `collectPredecessors(taskId, this::loadTask)` + `detectCycle(taskId, dependsOnId, this::loadTask, maxDepth)` 返回链路描述。便于单元测试（mock 加载函数）。
  - Skill: `nop-backend-dev`
- [x] Add: `ErpPrjErrors` 扩展 ErrorCode：`ERR_TASK_ILLEGAL_STATUS_TRANSITION`（参数 taskId/currentStatus/targetStatus）、`ERR_TASK_PREDECESSOR_NOT_DONE`（参数 taskId/dependsOnTaskId/dependsOnTaskStatus）、`ERR_TASK_SELF_DEPENDENCY`（参数 taskId）、`ERR_TASK_DEPENDENCY_CYCLE`（参数 taskId/chain）、`ERR_TASK_DEPENDENCY_DEPTH_EXCEEDED`（参数 taskId/maxDepth/actualDepth）、`ERR_TASK_DEPENDENCY_CROSS_PROJECT`（参数 taskId/taskProjectId/dependsOnTaskId/dependsOnProjectId）、`ERR_TASK_BLOCK_REASON_REQUIRED`。新增 `ARG_*` 常量：`ARG_DEPENDS_ON_TASK_ID`、`ARG_DEPENDS_ON_TASK_STATUS`、`ARG_CHAIN`、`ARG_MAX_DEPTH`、`ARG_ACTUAL_DEPTH`、`ARG_TARGET_STATUS`、`ARG_BLOCK_REASON`、`ARG_TASK_PROJECT_ID`、`ARG_DEPENDS_ON_PROJECT_ID`（既有 `ARG_TASK_ID`/`ARG_CURRENT_STATUS` 复用）。描述中文。
  - Skill: `nop-backend-dev`
- [x] Add: `ErpPrjConstants` 扩展配置键常量 `CONFIG_TASK_DEPENDENCY_MAX_DEPTH` + `CONFIG_TASK_STRICT_PREDECESSOR_CHECK`；`ErpPrjConfigs` 扩展 `DEFAULT_TASK_DEPENDENCY_MAX_DEPTH = 100` + `DEFAULT_TASK_STRICT_PREDECESSOR_CHECK = true` + reader 方法 `taskDependencyMaxDepth()` / `taskStrictPredecessorCheck()`，经 `AppConfig.var(..., defaultValue)` 读取（对齐 1018-2/0305-1 既有 `ErpPrjConfigs.budgetControlMode()` 范式）。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 任务保存时 dependsOnId 自环/成环/深度超限/跨项目 校验可观察（ErrorCode 触发）；状态迁移（startTask/completeTask/blockTask/unblockTask）非法迁移 ErrorCode 触发；前置任务未完成 config-gated strict/warn 行为可观察。
- [x] 局部编译通过（`mvn compile -pl module-projects/erp-prj-service`）；具体行为测试在 Phase 3 统一编写。

### Phase 3 - 行为测试 + 日志 + 文档对齐

Status: completed
Targets: `module-projects/erp-prj-service/src/test/java/app/erp/prj/service/TestErpPrjTaskDependency.java`、`module-projects/erp-prj-service/src/test/java/app/erp/prj/service/validator/TestTaskDependencyValidator.java`、`docs/logs/2026/07-{执行当日}.md`、`docs/backlog/extended-roadmap.md`
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 2

- [x] Add: `TestTaskDependencyValidator`（纯单元测试，mock 加载函数）：
  - 自环 A.dependsOnId=A → `ERR_TASK_SELF_DEPENDENCY`。
  - 二环 A→B→A → `ERR_TASK_DEPENDENCY_CYCLE`，chain="A→B→A"。
  - 三环 A→B→C→A → `ERR_TASK_DEPENDENCY_CYCLE`。
  - 长链无环 A→B→C→D（D 无前置）→ validate 通过。
  - 深度超限 101 链（A1→A2→...→A101，自 A101 上行追溯至 A1，步数=100；若 maxDepth=100 容许，则 maxDepth=99 触发 `ERR_TASK_DEPENDENCY_DEPTH_EXCEEDED`，actualDepth=100）。
  - 上行链全量 `collectPredecessors(A)` 在 A→B→C→D 链返回 [B,C,D]。
  - Skill: `nop-testing`
- [x] Add: `TestErpPrjTaskDependency`（集成测试，H2 + 直接调 BizModel API）：
  - 场景 1（保存任务带自环依赖）：save task.dependsOnId=自身 → `ERR_TASK_SELF_DEPENDENCY`。
  - 场景 2（成环保存）：A→B 已建，更新 B.dependsOnId=A → `ERR_TASK_DEPENDENCY_CYCLE`。
  - 场景 3（跨项目保存）：A.projectId=1，新建任务 projectId=2 + dependsOnId=A → `ERR_TASK_DEPENDENCY_CROSS_PROJECT`。
  - 场景 4（startTask 前置未完成）：A TODO + dependsOn=B（B TODO 未 DONE）→ startTask(A) → `ERR_TASK_PREDECESSOR_NOT_DONE`；config-gated warn 模式 → WARN 但放行。
  - 场景 5（startTask happy path）：B status=DONE → startTask(A) → A.status=IN_PROGRESS。
  - 场景 6（completeTask/blockTask/unblockTask 非法迁移）：TODO 直接 completeTask → `ERR_TASK_ILLEGAL_STATUS_TRANSITION`；BLOCKED 直接 completeTask → ErrorCode。
  - 场景 7（findPredecessors/findSuccessors）：A→B→C 链（A.dependsOnId=B, B.dependsOnId=C）+ D 反向引用 A（D.dependsOnId=A）→ findPredecessors(A)=[B,C]（A 的前置是 B，B 的前置是 C → 上行链全量 [B,C]）+ findSuccessors(C)=[B,A]（C 的后继是 B，B 的后继是 A → 下行反查全量 [B,A]）。
  - 场景 8a（head 自环）：构造 A1→A2→...→A101 链 + A1.dependsOnId=A1（自环）→ 校验 → `ERR_TASK_SELF_DEPENDENCY`（自环优先于深度判定，自环在第一步即被检测）。
  - 场景 8b（深度超限）：构造 A1→A2→...→A102 链（102 节点 = 101 边，自 A102 上行追溯至 A1 步数=101）+ `erp-prj.task-dependency-max-depth` 临时配置为 100 → 校验 → `ERR_TASK_DEPENDENCY_DEPTH_EXCEEDED`，actualDepth=101（与单元测试 `TestTaskDependencyValidator` 计数一致：节点数−1=边数=深度）。
  - Skill: `nop-testing`
  - **实现注记**：场景 7 oracle 修正 — D.dependsOnId=A 使 D 经传递归入 C 的下行链，`findSuccessors(C)` 实际返回 [B,A,D]（3 个，BFS 传递闭包正确语义），已在测试断言对齐并在 `task-dag.md §10` 注记。
- [x] Proof: `mvn test -pl module-projects/erp-prj-service -am`（含本期新增 18 测试（6 单元 + 12 集成）+ 1018-2/0305-1 既有）→ 63 tests / 0 failures / 0 errors。
  - Skill: `nop-testing`
- [x] Add: `docs/logs/2026/{执行当日 month-day}.md` 新增本计划条目（含验证状态）；`extended-roadmap.md` 工作项 2.6c `todo` → `done`；`task-dag.md` 实现注记补注（如有偏离）。
  - Skill: none

Exit Criteria:

- [x] 新增行为测试 18 全绿（单元 6 + 集成 12，集成场景 4/6 拆为多个测试方法覆盖更多分支：strict/warn、illegal/blockReason/roundTrip）；projects-service 既有测试无回归（63 total / 0 failures/0 errors）。
- [x] 当日日志条目在位；roadmap 2.6c 标 done。

## Draft Review Record

- Independent draft review iteration 1: **needs revision**（`ses_0c67dc78c0ffenFTLvXXKB8hCOy`，独立 general 子代理，新会话，冷重播无执行者上下文）— 17 项 baseline 主张全部核实 TRUE（ErpPrjTask/dependsOnId/dependsOn/status/4 态字典/BizModel 空壳/IErpPrjTaskBiz 空壳/task-dag.md 缺失/state-machine.md §任务 + UC-PRJ-05/2.6c todo/ErpPrjConstants/ErpPrjErrors 既有）。2 BLOCKER：(B1) 配置范式违反 projects 域两文件约定（`CFG_` 前缀应为 `CONFIG_`、`NopSysVariable` 应为 `ErpPrjConfigs.DEFAULT_*` + reader 方法、未提 `ErpPrjConfigs`）；(B2) 测试场景 8「A or B 取决于测试目的」非确定性 oracle。5 建议（S1 范围超出 2.6c 需声明规则 14 合并 / S2 `List<List<>>` 过类型化单前置模型 / S3 测试 `?` 标记 / S4「可能新增」/ S5 ARG_* 既有/新增区分）。**已修订**：B1→`CFG_` 改 `CONFIG_` + 加 `ErpPrjConfigs` 扩展项（DEFAULT_* + reader 方法）+ Infrastructure + Phase 2 Targets 同步；B2→场景 8 拆为 8a（head 自环→SELF_DEPENDENCY）+ 8b（102 节点链→DEPTH_EXCEEDED），测试计数 14→15；S1→Goals/Non-Goals 补规则 14 合并说明；S2→getDependencyChain 改 `List<ErpPrjTask>`；S3→去 `?` 标记；S4→「可能新增」改确定；S5→ARG_* 区分既有/新增。
- Independent draft review iteration 2: **accept / consensus**（`ses_0c6751c32ffeDK7nJ63oZsIDIZ`，独立 general 子代理，新会话，冷重播无执行者上下文）— B1/B2 全部 RESOLVED（`ErpPrjConstants:11`/`ErpPrjConfigs:10,21` 范式核实一致；场景 8a/8b oracle 确定性 + 计数 15 一致），S1-S5 全部 RESOLVED。0 NEW BLOCKER。2 非阻塞 nit（N1 8b actualDepth 计数与单测一致化 + N2 Draft Review Record 中 8a 简写 CYCLE 应为 SELF_DEPENDENCY）**已吸收**：8b 改 102 节点链 + maxDepth=100 + actualDepth=101（与单测"节点数−1=深度"一致）；Draft Review Record 修订。**共识达成**：Plan Status 升级为 `active`。

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。完整仓库验证在此处。

- [x] 范围内行为完成（task-dag.md 设计收敛 + 保存校验自环/成环/深度/跨项目 + 状态迁移前置校验 + 查询入口 + 配置门控）
- [x] 相关文档对齐（`extended-roadmap.md` 2.6c done；当日日志；`task-dag.md` 实现注记）
- [x] 已运行验证：`mvn clean install -DskipTests`（全模块 154）+ `mvn test -pl module-projects/erp-prj-service -am`（63 tests）；0 failures / 0 errors
- [x] 无范围内项目降级为 deferred/follow-up（多前置 DAG/跨项目依赖/CPM/可视化/父子树成环/审计/延期告警/工时联动 均为计划内 Non-Goal 附触发条件）
- [x] 独立草案审查已完成并记录（Draft Review Record 两轮迭代，B1/B2 BLOCKER + S1-S5 建议 + N1/N2 nit 全部 RESOLVED；共识达成升级 active）
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符（本次结束审计由独立 closure-auditor 子代理在新会话冷重播执行，见 Closure Audit Evidence）
- [x] 结束证据存在于文件中（`docs/logs/2026/07-07.md` 任务依赖 DAG 条目；`TestTaskDependencyValidator` 6 cases + `TestErpPrjTaskDependency` 12 cases；`docs/design/projects/task-dag.md` §10 实现注记；`extended-roadmap.md` 2.6c ✅ done）

## Deferred But Adjudicated

### 多前置 DAG（ErpPrjTaskDependency 多对多表）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本期维持单 `dependsOnId` 列；真 DAG（一任务依赖多前置）需新增多对多关系表 + DFS 三色标记算法 + 数据迁移。
- Successor Required: yes（触发条件：项目计划要求一任务依赖多前置时，如测试任务依赖开发 + 文档两个前置）

### 跨项目任务依赖

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 任务 dependsOn 仅允许同 projectId 内；跨项目依赖经里程碑 `ErpPrjMilestone` 或手工协同。
- Successor Required: yes（触发条件：跨项目任务依赖业务需求上线时）

### 关键路径计算（CPM）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: CPM 属项目排程高级面，依赖多前置模型；本期单前置不足以承载 CPM。
- Successor Required: yes（触发条件：CPM 排程需求上线时）

### 任务依赖可视化（甘特图/网络图）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 归前端 successor（数据 API 已就绪）。
- Successor Required: yes（触发条件：前端可视化套件建立时）

### 任务父子树（parentTaskId）成环校验

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 父子树成环属另一类校验，与依赖关系正交；本期仅做依赖关系成环。
- Successor Required: yes（触发条件：父子树成环 bug 出现或多级 WBS 业务上线时）

### 任务延期/超期自动告警

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 归通知 successor（0642-1 范式）。
- Successor Required: yes（触发条件：任务延期告警业务需求启动时）

## Closure

Status Note: 执行者（opencode glm-5.2 主代理）已完成全部 3 个 Phase 的实施工作（owner 设计收敛 + BizModel/IBiz/Validator/ErrorCode/Config 加性 + 18 行为测试全绿 + 文档对齐）。所有可由执行者验证的 Closure Gates 已勾选（范围/文档/验证/Non-Goal/草案审查/文本一致性/结束证据）。仅「结束审计由独立子代理（新会话）执行」一 gate 按 AGENTS.md 规则 12 必须由独立子代理勾选，执行者未自我审计该 gate。Plan Status 标 `completed` 系执行者层完工声明，正式关闭仍需独立结束审计通过。

Closure Audit Evidence:

- Auditor / Agent: 独立 closure-auditor 子代理（新会话，冷重播无执行者上下文；opencode glm-5.2）
- Evidence: 独立审计复核（live repo grep/glob/read）—— (1) **Phase 1**：`docs/design/projects/task-dag.md` 存在；(2) **Phase 2**：`ErpPrjTaskBizModel.java`（264 行，`defaultPrepareSave/Update:57-66` 钩子调 `validateDependency:72-99`，`startTask/completeTask/blockTask/unblockTask:106-168` 状态机，`findPredecessors/findSuccessors/getDependencyChain:200-237`，`validatePredecessorDone:173-194` STRICT/WARN 门控）；`TaskDependencyValidator.java`（172 行，`detectCycle:44-86` 上行链+HashSet+maxDepth，`collectPredecessors:97-135`）；`ErpPrjErrors.java:139-163` 7 ErrorCode 全部定义；`ErpPrjConstants.java:27-29` 两 CONFIG_ 键 + `:43-46` 4 态；`ErpPrjConfigs.java:22,25` DEFAULT_* + `:80,90` reader 方法（范式对齐 1018-2/0305-1）；(3) **Phase 3**：`TestTaskDependencyValidator.java` 6 cases（自环/二环/三环/长链无环/深度超限/collectPredecessors）+ `TestErpPrjTaskDependency.java` 12 cases（scenario1-3,4_strict,4_warn,5,6_illegal,6_blockReason,6_roundTrip,7,8a,8b）= 18 测试。**Anti-hollow 复核**：钩子运行时被调用（非孤立）、无空方法体、无 `return null` 占位、WARN 模式经 `LOG.warn` 记录非静默吞没、`findSuccessors` BFS 含 visited 防重入。**Deferred honesty**：多前置 DAG/跨项目/CPM/可视化/父子树/审计/延期告警/工时联动 8 项均为计划内 Non-Goal，附触发条件，无范围内缺陷隐藏。**Docs sync**：`docs/logs/2026/07-07.md` 任务依赖 DAG 条目在位；`docs/design/projects/task-dag.md` §10 实现注记；`docs/backlog/extended-roadmap.md:32,80` 2.6c ✅ done。**执行者层证据**：`mvn test -pl module-projects/erp-prj-service -am` 63 tests / 0 failures / 0 errors；`mvn clean install -DskipTests` 全工作区 BUILD SUCCESS。**五点一致性**：Plan Status / 3 Phase Status / 全部 Exit Criteria / 全部 Closure Gates / Closure 证据一致。审计结论：**APPROVED — 可正式关闭**。

Follow-up:

- 独立结束审计（新会话子代理）：复核实施符合计划范围、验证证据、Non-Goal 边界，勾选 Closure Gates 第 7 项。
- 多前置 DAG（`ErpPrjTaskDependency` 多对多表） successor：触发条件为一任务依赖多前置业务需求上线。
- 跨项目任务依赖 successor：触发条件为跨项目任务依赖业务需求。
- CPM 排程 successor：触发条件为关键路径排程需求。
- 任务父子树（parentTaskId）成环校验 successor：触发条件为多级 WBS 或父子树成环 bug。
- 任务延期/超期告警 successor（0642-1 范式）：触发条件为通知业务启动。
