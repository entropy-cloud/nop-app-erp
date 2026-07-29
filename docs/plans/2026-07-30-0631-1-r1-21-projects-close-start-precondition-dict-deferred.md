# 2026-07-30-0631-1-r1-21-projects-close-start-precondition-dict-deferred projects 状态机前置校验补齐 + Milestone/Billing/CostCollection dict 死状态 Deferred

> Plan Status: completed
> Last Reviewed: 2026-07-30
> Source: audit-remediation-roadmap R1.21（P1-MA2-067 + P1-MA2-069 + P1-MA2-070，源自 A2.13 projects 状态机审查）
> Related: `docs/audits/2026-07-28-1020-arm-ma2-projects-state-machine.md`、`docs/audits/arm-index.md §P1-MA2-067/069/070`；plan `2026-07-30-0512-3-r1-20-quality-linkage-dict-ncr.md`（同型选择性裁决先例：便宜真实前置缺陷实现 + dict 死状态 Deferred）、plan `2026-07-30-0512-1-r1-18-assets-idle-state-machine-deferred.md`（dict 死状态 Deferred 先例）
> Audit: required

## Current Baseline

三项 finding 经实仓逐项确认：均为「owner doc 声明前置/迁移但代码未实现 + CRUD 桩 dict 死状态 + 字典语义复用偏移」类型，**不破坏已实现主路径**（项目 5 态 OPEN↔ON_HOLD→COMPLETED/CANCELLED + 任务 4 态全迁移 + DAG 成环检测 + 工时审批轴 + 项目结算三轴完整覆盖生命周期）。

**P1-MA2-067（closeProject 缺任务结束校验）— 确认：**
- `ErpPrjProjectBizModel.closeProject:62-80` 仅 `refreshActualCost` + `refreshExpenseCost`(config-gated) + `setStatus(COMPLETED)`——**完全不查询 ErpPrjTask 状态**。
- owner doc `state-machine.md §迁移完整性 L35`「OPEN→COMPLETED 前置：任务已结束（或确认剩余不再执行）、成本已归集」+ §审查提示「项目完成时未结束任务的处理是否明确」+ §4 异常路径「完成时仍有未结束任务→提示先关闭任务，或确认剩余任务取消」。
- 残留未结束任务（TODO/IN_PROGRESS/BLOCKED）在项目 COMPLETED 终态后悬挂（status 字段保留但项目已终态，无后续迁移路径）。仍需人工 closeProject 动作 + 已归集成本不丢失。

**P1-MA2-069（Milestone/Billing/CostCollection dict 死状态 + 字典语义复用偏移）— 确认：**
- `ErpPrjMilestoneBizModel`（18 行 CRUD 桩）/ `ErpPrjBillingBizModel`（18 行 CRUD 桩）零 setStatus writer——Milestone(task-status 4 态)/Billing(project-status 5 态 docStatus) 全 dict 死状态。
- `ErpPrjCostCollection.docStatus` 复用 `erp-prj/project-status` 字典，但 `ProjectCostAggregator`/`ExpenseCostAggregator` 写 `DOC_STATUS_APPROVED="APPROVED"`（**不在 project-status 字典内**——字典仅 DRAFT/OPEN/ON_HOLD/COMPLETED/CANCELLED）。
- 不破坏主路径（CRUD 空壳实体状态字段不参与主路径迁移判定；CostCollection 的 APPROVED drift 经聚合器单一入口写入，归集行为正确，仅 dict 筛选层失效）。

**P1-MA2-070（startProject 缺前置校验 + cancelProject 多源）— 确认：**
- `startProject:84-87` 仅 `transition(projectId, DRAFT, OPEN, context)`——`transition:119-130` 仅检查 `status==DRAFT`，**无字段校验**（项目名/起止日期/预算为空的 DRAFT 可直接立项）。
- `cancelProject:105-117` 接受 `status != COMPLETED && != CANCELLED`（即 DRAFT/OPEN/ON_HOLD）——超出 owner doc §迁移完整性 L36 显式声明的 OPEN 单源。
- (a) DRAFT→OPEN 缺前置是契约漂移但非数据破坏；(b) cancelProject 多源是功能扩展（更宽松），功能正确但 owner doc 未声明。

**保护区域：** 不触及会计/财务/数据删除保护区域（无凭证/折旧/删除写路径变更）。P1-MA2-067/070 方案A 涉及 BizModel 行为变更 + ErrorCode，按 roadmap 规则走标准 plan-audit + closure-audit（不触及 ORM ask-first——不改 model/*.orm.xml）。

## Goals

- 消除 projects 域 owner doc 与代码间三项悬空：(1) **实现** closeProject 任务结束前置校验（config-gated STRICT/WARN）；(2) **实现** startProject 字段前置校验（config-gated）+ owner doc 补充 cancelProject 多源声明；(3) Milestone/Billing/CostCollection dict 死状态对齐（owner doc Deferred 标注，dict 保留为预留）。
- owner doc 与代码一致；项目生命周期前置不变量经显式门控落地。

## Non-Goals

- 不实现 Milestone/Billing 状态机 BizMutation（P1-MA2-069）——裁决 Deferred（项目管理全面状态机 successor），dict 死状态保留为预留。
- 不改 CostCollection/Milestone/Billing 的 ORM `ext:dict` 绑定（避免 ORM ask-first + 生成层漂移；owner doc Deferred 标注 dict-value drift 即可）。
- 不从 ORM 删除 dict 死状态值（采纳「保留为预留 + 文档 Deferred」对齐 R1.13/R1.14/R1.15/R1.18-R1.20 既有先例）。
- 不强制任务结束（即禁止任何未结束任务 closeProject）——STRICT 默认拦截 + WARN 模式放行（对齐 task-dag.md §4.3 范式）；WARN successor 命名严格强制门控。
- 不实现工时过账悬挂修复（P1-MA2-068）——归 R1.16 整体裁决（已完成）。

## Task Route

- Type: `app-layer design change`（owner doc 行为契约对齐）+ `implementation-only change`（BizModel 前置门控 + ErrorCode）
- Owner Docs: `docs/design/projects/state-machine.md`、`docs/design/projects/cost-collection.md`
- Skill Selection Basis: P1-MA2-067/070 涉及 BizModel 方法行为变更 + 跨实体（IErpPrjTaskBiz）+ ErrorCode + config-gated → `Skill: nop-backend-dev`；P1-MA2-069 owner doc Deferred 标注为纯文档 → 该部分 `Skill: none`。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline.

## Execution Plan

### Phase 1 - 三项 finding 裁决（Decision）

Status: completed
Targets: 本计划（裁决记录）
Skill: `none`

- Item Types: `Decision`
- Prereqs: none

- [x] **Decision**：三项 finding 处置方案逐项裁决（**选择性裁决**——对齐 R1.20 先例：便宜真实前置缺陷实现 + dict 死状态 Deferred）。
      - P1-MA2-067 closeProject 缺任务结束校验：**实现（arm-index 推荐方向）**。理由：(1) arm-index §P1-MA2-067 方案A（推荐）即补任务结束前置；(2) containment 友好（closeProject 增单点前置查询 + 1 ErrorCode + config-gated STRICT/WARN）；(3) 针对 owner doc 核心契约「OPEN→COMPLETED 前置任务已结束」非 dict 死状态/TODO 噪音；(4) 对齐 task-dag.md §4.3 config-gated STRICT/WARN 范式（ErpPrjConfigs 已有 `taskStrictPredecessorCheck()` 同型模式）。残留风险：WARN 模式放行仍允许未结束任务关闭 → 不禁止，仅 config 控制严格度。
      - P1-MA2-070 startProject 缺前置 + cancelProject 多源：**实现（arm-index 推荐方向）**。(a) startProject 增字段校验（项目名/起止日期/预算 config-gated）+ ErrorCode；(b) owner doc §迁移完整性 L36 补充 cancelProject 多源声明（DRAFT/ON_HOLD→CANCELLED，与代码对齐）。理由：containment 友好（startProject 增单点前置校验 + 1 ErrorCode）+ owner doc 补声明纯文档。
      - P1-MA2-069 Milestone/Billing/CostCollection dict 死状态 + 字典语义复用偏移：**Deferred + dict 保留为预留**。**与 arm-index 推荐偏差声明**：arm-index §P1-MA2-069 方案A 推荐「三处分别实现状态机/独立字典 + ORM 改 ext:dict」；本计划裁决 Deferred，理由：(1) 与 R1.13/R1.14/R1.15/R1.18-R1.20「保留 dict 死状态为预留语义入口」先例一致（保留优于删除——避免 ORM ext:dict 改动触发 codegen 漂移 + 数据迁移）；(2) CRUD 空壳实体状态字段不参与主路径迁移判定；(3) CostCollection APPROVED dict-value drift 经聚合器单一入口写入，归集行为正确，owner doc 标注 drift 即可。successor：项目管理全面状态机需求时。
      - Skill: `none`

Exit Criteria:

- [x] Phase 1 Decision 逐项记录选择 + 理由 + 与 arm-index 推荐偏差声明 + successor 触发条件；067/070 进 Phase 2/3（实现），069 进 Phase 4（Deferred 标注）。

### Phase 2 - closeProject 任务结束前置校验实现（P1-MA2-067）

Status: completed
Targets: `module-projects/erp-prj-service/.../entity/ErpPrjProjectBizModel.java`、`IErpPrjProjectBiz`、`ErpPrjErrors.java`、`ErpPrjConfigs.java`、`ErpPrjConstants.java`、`docs/design/projects/state-machine.md`
Skill: `nop-backend-dev`

- Item Types: `Fix | Add`
- Prereqs: Phase 1

- [x] **Add（config）**：`ErpPrjConfigs` 增 `strictProjectTaskCompletionCheck()`（config key `erp-prj.strict-project-task-completion-check`，默认 true=STRICT 拦截，false=WARN 放行）+ `ErpPrjConstants` 增对应 `CONFIG_STRICT_PROJECT_TASK_COMPLETION_CHECK` 键 + `DEFAULT_STRICT_PROJECT_TASK_COMPLETION_CHECK=true`。
      - Skill: `nop-backend-dev`
- [x] **Add（ErrorCode）**：`ErpPrjErrors` 增 `ERR_PROJECT_HAS_UNFINISHED_TASKS`（`erp.err.prj.project-has-unfinished-tasks`，描述「项目 {projectId} 存在未结束任务（状态={taskStatuses}），不允许关闭（须先 completeTask 或取消剩余任务）」，i18n 中文描述 + ARG_PROJECT_ID + 新增 ARG_TASK_STATUSES）。
      - Skill: `nop-backend-dev`
- [x] **Fix（closeProject 前置门控）**：`ErpPrjProjectBizModel` 注入 `IErpPrjTaskBiz`；`closeProject` 在 OPEN 守卫通过后、`refreshActualCost` 之前增 `validateTasksFinished(projectId, context)`：构造 QueryBean（projectId=projectId + status NOT IN {DONE}）经 `taskBiz.findCount(query, context)`，count>0 时——STRICT 模式抛 `ERR_PROJECT_HAS_UNFINISHED_TASKS`；WARN 模式 `LOG.warn` 放行。task-status 字典为 TODO/IN_PROGRESS/DONE/BLOCKED（无 CANCELLED——任务取消走 useLogicalDelete），故 NOT IN {DONE} 精确捕获未结束集（TODO/IN_PROGRESS/BLOCKED 及 null 默认值计入）。
      - Skill: `nop-backend-dev`
- [x] **Proof**：测试——(1) 项目有 TODO/IN_PROGRESS 任务 → closeProject STRICT 模式 assertThrows `ERR_PROJECT_HAS_UNFINISHED_TASKS`；(2) 项目全部任务 DONE → closeProject 成功（OPEN→COMPLETED）；(3) STRICT 模式下项目有未结束任务 + WARN 模式（config=false）→ closeProject 成功（LOG.warn 放行）。迁移现有依赖「无任务结束校验直接关闭」的测试（若 closeProject 测试未建任务则行为不变）。
      - Skill: `nop-backend-dev`
- [x] **Add（owner doc）**：state-machine.md §迁移完整性 L35 + §4 异常路径 + §审查提示 更新为「closeProject 经 `validateTasksFinished` 校验：STRICT 模式（`erp-prj.strict-project-task-completion-check` 默认 true）存在未结束任务（非 DONE）时抛 `ERR_PROJECT_HAS_UNFINISHED_TASKS`；WARN 模式 LOG.warn 放行」。
      - Skill: `none`

Exit Criteria:

- [x] closeProject STRICT 模式存在未结束任务时抛 `ERR_PROJECT_HAS_UNFINISHED_TASKS`（grep 确认 findCount 调用落地）；config 默认 true；新增/迁移测试全绿（Closure Gates 跑全量 mvn）；owner doc §迁移完整性/§4/§审查提示 与代码一致。

### Phase 3 - startProject 字段前置校验实现 + cancelProject 多源声明（P1-MA2-070）

Status: completed
Targets: `module-projects/erp-prj-service/.../entity/ErpPrjProjectBizModel.java`、`IErpPrjProjectBiz`、`ErpPrjErrors.java`、`ErpPrjConfigs.java`、`ErpPrjConstants.java`、`docs/design/projects/state-machine.md`
Skill: `nop-backend-dev`

- Item Types: `Fix | Add`
- Prereqs: Phase 1

- [x] **Add（config）**：`ErpPrjConfigs` 增 `strictProjectStartPrecheck()`（config key `erp-prj.strict-project-start-precheck`，默认 true=STRICT，false=WARN）+ `ErpPrjConstants` 增 `CONFIG_STRICT_PROJECT_START_PRECHECK` + `DEFAULT_STRICT_PROJECT_START_PRECHECK=true`。
      - Skill: `nop-backend-dev`
- [x] **Add（ErrorCode）**：`ErpPrjErrors` 增 `ERR_PROJECT_START_PRECONDITION_FAILED`（`erp.err.prj.project-start-precondition-failed`，描述「项目 {projectId} 立项前置校验失败：缺少必填字段 {missingFields}（项目名/起止日期/预算）」，ARG_PROJECT_ID + 新增 ARG_MISSING_FIELDS）。
      - Skill: `nop-backend-dev`
- [x] **Fix（startProject 前置门控）**：`ErpPrjProjectBizModel.startProject` 在 `transition` 调用前增 `validateStartPreconditions(project, context)`：校验项目名（name 非空）+ 起止日期（startDate/endDate 非空且 startDate<=endDate）+ 预算（budget 非空 或 config 允许无预算）——缺失时收集 missingFields——STRICT 模式抛 `ERR_PROJECT_START_PRECONDITION_FAILED`；WARN 模式 LOG.warn 放行。
      - Skill: `nop-backend-dev`
- [x] **Proof**：测试——(1) DRAFT 项目缺起止日期 → startProject STRICT 模式 assertThrows `ERR_PROJECT_START_PRECONDITION_FAILED`；(2) DRAFT 项目字段完整 → startProject 成功（DRAFT→OPEN）；(3) WARN 模式下字段缺失 → startProject 成功（LOG.warn 放行）。迁移现有依赖「无字段校验直接立项」的测试（补全字段或切 WARN）。
      - Skill: `nop-backend-dev`
- [x] **Add（owner doc）**：state-machine.md §迁移完整性 L32 更新为「startProject 经 `validateStartPreconditions` 校验：STRICT 模式（`erp-prj.strict-project-start-precheck` 默认 true）缺少必填字段（项目名/起止日期/预算）时抛 `ERR_PROJECT_START_PRECONDITION_FAILED`」；§迁移完整性 L36 补充「cancelProject 接受 DRAFT/OPEN/ON_HOLD 多源（与代码对齐）——DRAFT/ON_HOLD 取消是 OPEN 取消的业务扩展（暂停项目最终决策为取消）」。
      - Skill: `none`

Exit Criteria:

- [x] startProject STRICT 模式字段缺失时抛 `ERR_PROJECT_START_PRECONDITION_FAILED`；config 默认 true；新增/迁移测试全绿；owner doc §迁移完整性 L32/L36 与代码一致。

### Phase 4 - Milestone/Billing/CostCollection dict 死状态 Deferred 标注（P1-MA2-069）

Status: completed
Targets: `docs/design/projects/state-machine.md`、`docs/design/projects/cost-collection.md`
Skill: `none`

- Item Types: `Add`
- Prereqs: Phase 1

- [x] state-machine.md 新增「CRUD 桩实体状态机（Deferred）」补注段：合并标注 ErpPrjMilestone（task-status 4 态全 dead）/ ErpPrjBilling（project-status 5 态 docStatus 全 dead）/ ErpPrjCostCollection（docStatus 复用 project-status 但实际写 APPROVED——dict-value drift）各 dict 死状态为预留值（零 writer 或单一聚合器入口），CRUD 桩为主路径可用，完整状态机属项目管理全面需求 successor；dict 值保留不删除；命名 successor 触发条件。
      - Skill: `none`
- [x] cost-collection.md §4.2 核对一致：标注 CostCollection.docStatus 经聚合器单一入口写 `DOC_STATUS_APPROVED`，与 project-status 字典值不重合是已知 dict-value drift（归集行为正确，仅按 dict 筛选失效）；successor 独立字典时收敛。
      - Skill: `none`

Exit Criteria:

- [x] state-machine.md + cost-collection.md 明确 069 Deferred，owner doc 与代码零 writer/单一 writer 一致；successor 触发事件已命名。

## Draft Review Record

- Independent draft review iteration 1: accept (ses_04ffc8a0bffe96SHnDnCxq1THn, fresh session) because 全部基线事实经实仓 file:line 验证 TRUE（closeProject:62-80 无任务查询 / startProject:84-87 仅 status==DRAFT / cancelProject:105-117 多源 / Milestone·Billing 零 writer / CostCollection APPROVED drift）；067/070 实现=arm-index 方案A 推荐方向 + 069 Deferred 显式声明偏差 + 命名 successor，选择性裁决与 R1.20 先例一致；保护区域识别正确（无 ORM/会计触及）；实现可行（镜像既有 validatePredecessorDone + taskStrictPredecessorCheck 模式）；14 最低规则 + 反松弛规则全部合规。修订：采纳非阻塞注记——精确化 task-status 集合（TODO/IN_PROGRESS/DONE/BLOCKED 无 CANCELLED，NOT IN {DONE} 精确捕获未结束集）。

## Closure Gates

> 本计划含代码变更（P1-MA2-067/070），故 Closure Gates 含全量 `mvn` 验证（见执行时规则 7）。

- [x] 范围内行为/文档完成（067/070 前置校验实现落地 + 069 Deferred 标注）
- [x] 相关文档对齐（projects/state-machine.md + cost-collection.md）
- [x] 已运行验证（`mvn clean install -DskipTests` 全绿 + projects 域 `mvn test` 全绿（76 tests） + compliance checker 本计划零新增命中；grep 验证 067/070 门控落地——validateTasksFinished:97/validateStartPreconditions:115 + ErrorCode + config 均在位）
- [x] 无范围内项目降级为 deferred/follow-up（067/070 为范围内存活实现项；069 Deferred 是处置裁决 + 已命名 successor，非范围内缺陷隐瞒）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### Milestone/Billing 完整状态机 + CostCollection 独立字典（P1-MA2-069 successor）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: dict 死状态保留为预留语义入口；CRUD 桩为主路径可用；状态字段不参与主路径迁移判定；CostCollection APPROVED drift 经聚合器单一入口写入行为正确。
- Successor Required: `yes`（项目管理全面状态机需求时实现 ErpPrjMilestone startMilestone/achieveMilestone + ErpPrjBilling submitForApproval/approve/reject + CostCollection 独立 `erp-prj/cost-collection-status` 字典 + ORM ext:dict 改绑）

### closeProject WARN 模式严格强制（禁止任何未结束任务 closeProject）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 本计划已实现 STRICT 模式（默认）拦截未结束任务 closeProject；WARN 模式放行是 config 控制的更宽松策略，业务可按需切 STRICT。
- Successor Required: `yes`（业务裁决禁止任何未结束任务 closeProject 时，移除 WARN 放行分支或固定 STRICT）

## Closure

Status Note: 全部 4 Phase 执行完成。P1-MA2-067（closeProject 任务结束前置）+ P1-MA2-070（startProject 字段前置 + cancelProject 多源声明）已实现（config-gated STRICT/WARN + ErrorCode + 测试 6 例全绿）；P1-MA2-069（Milestone/Billing/CostCollection dict 死状态）Deferred 标注落地（state-machine.md 新增 CRUD 桩实体 Deferred 段 + cost-collection.md dict-value drift 注）。验证：`mvn clean install -DskipTests` 全工作区 BUILD SUCCESS + projects 域 `mvn test` 76 tests 全绿（含新增 TestErpPrjProjectPrecheck 6 例 + 迁移 testCloseProjectFreezesAndRejectsNewTimesheet 快照）。

Closure Audit Evidence:

- 代码：`ErpPrjProjectBizModel.java`（validateTasksFinished:165/validateStartPreconditions:186 + closeProject:97/startProject:115 调用点）、`ErpPrjErrors.java`（ERR_PROJECT_HAS_UNFINISHED_TASKS:101/ERR_PROJECT_START_PRECONDITION_FAILED:105 + ARG_TASK_STATUSES/ARG_MISSING_FIELDS）、`ErpPrjConfigs.java`（strictProjectTaskCompletionCheck:103/strictProjectStartPrecheck:110）、`ErpPrjConstants.java`（CONFIG_STRICT_PROJECT_TASK_COMPLETION_CHECK/CONFIG_STRICT_PROJECT_START_PRECHECK）。
- 测试：`TestErpPrjProjectPrecheck.java`（6 例：closeProject STRICT 拦截/全 DONE 放行/WARN 放行 + startProject STRICT 拦截/字段完整放行/WARN 放行）；`TestErpPrjBudgetAndCollection.testCloseProjectFreezesAndRejectsNewTimesheet` 迁移（任务 IN_PROGRESS→DONE + 快照重录）。
- 文档：`state-machine.md`（§迁移完整性 DRAFT→OPEN/OPEN→COMPLETED 加校验说明 + cancelProject 多源声明 + §4 异常路径 + §审查提示 + §适用对象三 CRUD 桩实体 Deferred）、`cost-collection.md`（§4.2 dict-value drift + §4.3 关闭前置）。
- 实现注记：ErpPrjTask.status 的 XMeta 仅允许 `in` 不允许 `notIn`（allowFilterOp=[eq, in, dateBetween, dateTimeBetween]），故 validateTasksFinished 用 `in {TODO, IN_PROGRESS, BLOCKED}` 计数未结束任务——等价于 `not in {DONE}`（task-status 字典仅 4 态无 CANCELLED）。
- **独立结束审计（新会话 cold-replay，未重用执行者上下文）**：
  - 代码逐项 file:line 复核 PASS：`ErpPrjProjectBizModel.java` validateTasksFinished:165（config-gated STRICT 抛 ERR_PROJECT_HAS_UNFINISHED_TASKS / WARN LOG.warn 放行，`in {TODO,IN_PROGRESS,BLOCKED}` 等价 not-in-{DONE}）+ validateStartPreconditions:186（STRICT 抛 ERR_PROJECT_START_PRECONDITION_FAILED / WARN 放行，校验 name/startDate/endDate/budget + startDate>endDate）+ closeProject:97/startProject:115 调用点在状态迁移守卫正确位置 + cancelProject:143 多源与 owner doc 对齐。
  - ErrorCode/Config/Constants 复核 PASS：ErpPrjErrors ERR_PROJECT_HAS_UNFINISHED_TASKS:101 / ERR_PROJECT_START_PRECONDITION_FAILED:105 + ARG_TASK_STATUSES/ARG_MISSING_FIELDS:53-54；ErpPrjConfigs strictProjectTaskCompletionCheck:103 / strictProjectStartPrecheck:110（默认 true）；ErpPrjConstants CONFIG_STRICT_PROJECT_TASK_COMPLETION_CHECK:31 / CONFIG_STRICT_PROJECT_START_PRECHECK:33。
  - 测试复核 PASS：TestErpPrjProjectPrecheck 6 例（closeProject STRICT 拦截/全 DONE 放行/WARN 放行 + startProject STRICT 拦截/字段完整放行/WARN 放行）覆盖 STRICT×WARN 全象限；TestErpPrjBudgetAndCollection.testCloseProjectFreezesAndRejectsNewTimesheet 任务 IN_PROGRESS→DONE 迁移适配。
  - owner doc 复核 PASS：state-machine.md §迁移完整性 L32/L35/L37-L38 + §4 异常路径 + §审查提示 + §适用对象三 CRUD 桩 Deferred 段（Milestone/Billing 零 writer / CostCollection APPROVED dict-value drift）+ successor 触发条件；cost-collection.md §4.2 drift 注 + §4.3 关闭前置注。
  - Anti-hollow 复核 PASS：无空方法体/return null 占位/吞异常——两个 validate 方法均有完整校验逻辑 + 实际抛 NopException + 运行时经 closeProject/startProject 调用点可达。
  - Deferred honesty 复核 PASS：069 为真实 CRUD 桩 dict 死状态 + 单一聚合器入口 drift，非范围内缺陷隐瞒；successor 触发事件（项目管理全面状态机需求）已命名；WARN 模式严格强制作为 optimization candidate 已命名 successor。
  - 五点一致性 PASS：Plan Status: completed / 全 4 Phase Status: completed / 全 Phase Exit Criteria [x] / Closure Gates 全 [x]（本次审计勾选结束审计门控）/ Closure 证据非占位。
  - 日志复核 PASS：docs/logs/2026/07-30.md 含 full-green verification 记录（mvn clean install -DskipTests 全 154 模块 + projects-service 76 tests 0 failures）。

Follow-up:

- 非阻塞；successor 已在 Deferred But Adjudicated 命名触发条件。
