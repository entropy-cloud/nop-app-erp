# 项目任务依赖与状态迁移（Task Dependency & State Machine）

> 本文档是 **UC-PRJ-05（任务依赖 DAG 成环校验）** 与 **任务 4 态状态机** 的权威收敛点。
> 内容从 `state-machine.md §适用对象二：任务` 与 `use-cases.md §UC-PRJ-05` 收敛并扩展，含算法选择、配置点、错误码与 Non-Goal。
>
> **真相源**：实体字段与字典在 `module-projects/model/app-erp-projects.orm.xml`（`ErpPrjTask` 实体、`erp-prj/task-status` 字典）。本文仅描述业务语义、校验规则与算法。

---

## 1. 依赖模型

### 1.1 当前模型：单前置（树/森林，非真 DAG）

`ErpPrjTask.dependsOnId`（BIGINT，单列）+ `dependsOn`（自引用 to-one FK）—— 每任务**至多一个前置任务**。

- 形态本质：每个任务是某棵"依赖树"的节点；多棵依赖树构成森林。
- 链路形态：从任一节点向上追溯（`dependsOnId → dependsOnId → ...`）是一条**线性链**，至多一条。
- 上行链长度 = 节点数 − 1（边数）。

### 1.2 Decision：本期维持单前置

| 维度 | 选择 (a) 单 `dependsOnId`（本期） | 替代 (b) `ErpPrjTaskDependency` 多对多表 |
|------|----------------------------------|------------------------------------------|
| ORM churn | 最小（无表新增） | 大（新增表 + 中间表 + 关系重导） |
| 语义覆盖 | 一任务至多一前置（覆盖 80% 项目计划场景） | 真 DAG（一任务依赖多前置） |
| 成环检测 | 上行链 O(N) | DFS 三色标记 O(V+E) |
| 触发升级条件 | — | 业务出现"测试任务依赖开发 + 文档两个前置"等需求 |

**残留风险**：业务场景若要求一任务依赖多前置，需后续落地 (b) + 数据迁移。本期 (a) 不阻塞常规项目计划。

---

## 2. 成环检测算法

### 2.1 选择：上行链追溯 + HashSet revisit + 深度上限

```
detectCycle(taskId, dependsOnId, loader, maxDepth):
    if taskId == dependsOnId:               # 自环 A→A
        → ERR_TASK_SELF_DEPENDENCY
    visited = {taskId}                       # 起点
    cursor = dependsOnId
    depth = 0
    while cursor != null:
        depth++
        if depth > maxDepth:                 # 防恶意长链耗尽栈/堆
            → ERR_TASK_DEPENDENCY_DEPTH_EXCEEDED (actualDepth=depth)
        if cursor in visited:                # revisit → 成环
            → ERR_TASK_DEPENDENCY_CYCLE (chain=visited 序列)
        visited.add(cursor)
        predecessor = loader.apply(cursor)
        if predecessor == null:              # 任务不存在
            break
        cursor = predecessor.dependsOnId     # 上行一步
    return OK
```

**算法性质**：

- 时间复杂度：O(N)，N = 链长。每次保存时一次上行链遍历。
- 空间复杂度：O(N)，HashSet 存储已访问节点。
- **自环优先**：自环在第一步即被检测（taskId==dependsOnId），优先于深度判定。
- **链路终止**：遇 `null`（无前置）或节点不存在 → 无环。

### 2.2 替代算法（rejected）

| 替代 | 为什么不选 |
|------|-----------|
| DFS 三色标记 | 适合多对多 DAG；单前置模型下冗余 |
| 全表加载 + 拓扑排序 | 性能差；不必要的全表扫描 |
| 数据库递归 CTE | 跨数据库可移植性差；H2 测试环境支持不一致 |

### 2.3 深度上限

- 配置键：`erp-prj.task-dependency-max-depth`
- 默认值：`100`（`ErpPrjConfigs.DEFAULT_TASK_DEPENDENCY_MAX_DEPTH`）
- 残留风险：上行链每次保存时 O(N) 查询，N=链长；maxDepth 兜底防止恶意/意外长链耗尽栈/堆；并发编辑经乐观锁兜底（不在保存时持锁遍历）。

---

## 3. 跨项目依赖校验

任务 `dependsOn` 仅允许**同 `projectId` 内**。

- 保存校验：`task.projectId == dependsOnTask.projectId`，否则抛 `ERR_TASK_DEPENDENCY_CROSS_PROJECT`。
- 跨项目协同经里程碑 `ErpPrjMilestone` 或人工协同（本期 Non-Goal）。

---

## 4. 任务状态机

### 4.1 4 态定义

| 状态 | 业务含义（等待什么） | 是否允许录入工时 |
|------|----------------------|------------------|
| TODO（待开始） | 等待启动 | 否 |
| IN_PROGRESS（进行中） | 执行中 | 是 |
| BLOCKED（阻塞） | 等待解除阻塞原因 | 否 |
| DONE（已完成） | 终态 | 否 |

字典：`erp-prj/task-status`。

### 4.2 迁移图

```
TODO
  └─ startTask → IN_PROGRESS
                    ├─ completeTask → DONE（终态）
                    └─ blockTask    → BLOCKED
                                        └─ unblockTask → IN_PROGRESS
```

| 迁移 | 方法 | 前置态 | 附加校验 |
|------|------|--------|----------|
| TODO → IN_PROGRESS | `startTask(taskId)` | TODO | **前置任务须 DONE**（config-gated STRICT/WARN） |
| IN_PROGRESS → DONE | `completeTask(taskId)` | IN_PROGRESS | — |
| IN_PROGRESS → BLOCKED | `blockTask(taskId, blockReason)` | IN_PROGRESS | `blockReason` 必填 |
| BLOCKED → IN_PROGRESS | `unblockTask(taskId)` | BLOCKED | — |

非法迁移抛 `ERR_TASK_ILLEGAL_STATUS_TRANSITION`（参数 `taskId`/`currentStatus`/`targetStatus`）。

### 4.3 前置任务完成校验（config-gated）

`startTask` 触发：

- 若 `task.dependsOnId != null` 且 `dependsOnTask.status != DONE`：
  - **STRICT 模式**（默认 `erp-prj.task-strict-predecessor-check=true`）：抛 `ERR_TASK_PREDECESSOR_NOT_DONE`（参数 `taskId`/`dependsOnTaskId`/`dependsOnTaskStatus`）。
  - **WARN 模式**（配置为 `false`）：经 `CoreMetrics` 时间戳 + 日志告警，**放行**迁移。

WARN 模式适用：探索性项目、依赖关系非强约束的团队（如研发原型阶段）。

---

## 5. 依赖查询入口

| 方法 | 语义 | 实现 |
|------|------|------|
| `findPredecessors(taskId)` | 上行链全量（前置 + 前置的前置 + ...） | 经 `TaskDependencyValidator.collectPredecessors` 递归 `dependsOnId` |
| `findSuccessors(taskId)` | 下行反查全量（所有直接/间接后继） | 经 `QueryBean filter dependsOnId==taskId` 递归 |
| `getDependencyChain(taskId)` | 单链全量（对齐单前置模型，至多一条线性链） | 上行链顺序列表 |

> 单前置模型下 `getDependencyChain` 与 `findPredecessors` 返回相同结构（线性链）；多前置 successor 落地后 `getDependencyChain` 扩展为 `List<List<ErpPrjTask>>`。

---

## 6. 配置点

| 配置键 | 默认值 | 含义 |
|--------|--------|------|
| `erp-prj.task-dependency-max-depth` | `100` | 上行链深度上限（防恶意长链） |
| `erp-prj.task-strict-predecessor-check` | `true` | STRICT=true 拦截；false 仅 WARN 放行 |

经 `AppConfig.var(...)` 读取，默认值与解释器集中在 `ErpPrjConfigs`（对齐 1018-2/0305-1 范式）。

---

## 7. 错误码

| ErrorCode | 触发场景 | 参数 |
|-----------|----------|------|
| `ERR_TASK_SELF_DEPENDENCY` | `task.dependsOnId == task.id` | `taskId` |
| `ERR_TASK_DEPENDENCY_CYCLE` | 上行链 revisit | `taskId`、`chain` |
| `ERR_TASK_DEPENDENCY_DEPTH_EXCEEDED` | 上行链步数 > maxDepth | `taskId`、`maxDepth`、`actualDepth` |
| `ERR_TASK_DEPENDENCY_CROSS_PROJECT` | `task.projectId != dependsOnTask.projectId` | `taskId`、`taskProjectId`、`dependsOnTaskId`、`dependsOnProjectId` |
| `ERR_TASK_PREDECESSOR_NOT_DONE` | `startTask` 时前置未 DONE（STRICT 模式） | `taskId`、`dependsOnTaskId`、`dependsOnTaskStatus` |
| `ERR_TASK_ILLEGAL_STATUS_TRANSITION` | 非法状态迁移 | `taskId`、`currentStatus`、`targetStatus` |
| `ERR_TASK_BLOCK_REASON_REQUIRED` | `blockTask` 未提供 `blockReason` | `taskId` |

---

## 8. Non-Goals（本期不做）

- **多前置 DAG（`ErpPrjTaskDependency` 多对多表）**：本期维持单 `dependsOnId` 列。触发条件：项目计划要求一任务依赖多前置时（如测试任务依赖开发 + 文档）。
- **跨项目任务依赖**：仅同 `projectId` 内。触发条件：跨项目任务依赖业务需求上线时。
- **关键路径计算（CPM）/ 任务最早最晚开始时间**：依赖多前置模型。触发条件：CPM 排程需求上线时。
- **任务依赖可视化（甘特图/网络图）**：归前端 successor。触发条件：前端可视化套件建立时。
- **任务父子树（`parentTaskId`）成环校验**：与依赖关系正交的另一类校验。触发条件：父子树成环 bug 出现或多级 WBS 业务上线时。
- **任务依赖变更的审计日志/审批**：依赖编辑直接乐观锁，平台 `NopSysChangeLog` 自动记录实体变更已足够。
- **任务延期/超期自动告警**：归通知 successor（0642-1 范式）。
- **任务工时与状态联动**（IN_PROGRESS 自动开工时间、DONE 自动结算工时）：本期仅维护 status 迁移；归 cost-collection successor（1018-2）。

---

## 9. 与设计文档的关系

- `state-machine.md §适用对象二：任务` —— 任务 4 态状态机速查（**引用本文**为详细机制）。
- `use-cases.md §UC-PRJ-05` —— 用例断言（**引用本文**为机制细节）。
- `cost-collection.md` —— 工时成本归集（与任务状态正交，工时校验任务态但**不**校验依赖）。
- `module-projects/model/app-erp-projects.orm.xml` —— 实体字段与字典真相源。

---

## 10. 实现注记

- **`findSuccessors` 为传递闭包**：返回起点任务的所有直接 + 间接后继（BFS 全图）。例如链 C←B←A←D（即 A.dependsOnId=B、B.dependsOnId=C、D.dependsOnId=A），`findSuccessors(C)` 返回 [B, A, D]（D 经 A 传递归入 C 的下行链）。这与 `findPredecessors` 的「上行链全量」语义对称。
- **场景 7 oracle 修正**：计划草案曾写 `findSuccessors(C)=[B,A]`，未计入 D 经 A 的传递后继；实际算法语义为 `[B,A,D]`。已在 `TestErpPrjTaskDependency.scenario7` 对齐。
- **保存钩子触发路径**：`taskBiz.save(map, ctx)` 在 Nop 中走插入语义（已存在 id 抛 `entity-already-exists`）；修改既有任务的依赖关系应走 `taskBiz.update(map, ctx)` 触发 `defaultPrepareUpdate`。两个钩子均调 `validateDependency`。
