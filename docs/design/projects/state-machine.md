# 项目管理域状态机

> **设计要点依据**：本状态机按 `docs/skills/state-machine-business-review-prompt.md` 的 10 个审查维度组织。审查本状态机时使用该提示词。
>
> 项目域有两类状态对象：**项目**（生命周期）与**任务**（执行进度）。

## 适用对象一：项目（Project）

### 1. 状态定义

| 状态 | 业务含义（等待什么） | 可被新单据引用 |
|------|----------------------|----------------|
| 草稿（DRAFT） | 等待立项确认 | 否 |
| 进行中（OPEN） | 项目活跃执行，可归集成本 | 是 |
| 暂停（ON_HOLD） | 暂停执行（等待恢复或决策） | 否（新费用不归集） |
| 已完成（COMPLETED） | 终态：项目正常完成 | 否 |
| 已取消（CANCELLED） | 终态：项目取消 | 否 |

### 2. 迁移完整性

```
草稿 (DRAFT)
  └─ 立项 → 进行中 (OPEN)
              ├─ 暂停 → 暂停 (ON_HOLD)
              │          └─ 恢复 → 进行中 (OPEN)
              ├─ 完成 → 已完成 (COMPLETED)
              └─ 取消 → 已取消 (CANCELLED)
```

| 迁移 | 触发人 | 前置条件 | 结果 |
|------|--------|----------|------|
| DRAFT → OPEN | 项目经理/管理员 | 项目信息完整、起止日期有效、预算已定（`startProject` 经 `validateStartPreconditions` 校验：STRICT 模式 `erp-prj.strict-project-start-precheck` 默认 true，缺项目名/起止日期/预算或 startDate>endDate 时抛 `ERR_PROJECT_START_PRECONDITION_FAILED`；WARN 模式 LOG.warn 放行） | 开放成本归集 |
| OPEN → ON_HOLD | 项目经理 | 进行中状态 | 暂停费用归集（可配置） |
| ON_HOLD → OPEN | 项目经理 | 暂停状态 | 恢复 |
| OPEN → COMPLETED | 项目经理/管理员 | 任务已结束（或确认剩余不再执行）、成本已归集（`closeProject` 经 `validateTasksFinished` 校验：STRICT 模式 `erp-prj.strict-project-task-completion-check` 默认 true，存在未结束任务——status 为 TODO/IN_PROGRESS/BLOCKED——时抛 `ERR_PROJECT_HAS_UNFINISHED_TASKS`；WARN 模式 LOG.warn 放行） | 终态，可出项目成本/利润报表 |
| OPEN → CANCELLED | 项目经理/管理员 | 进行中状态 | 终态，已归集成本保留 |
| DRAFT → CANCELLED | 项目经理/管理员 | 草稿状态（业务扩展） | 终态，草稿无归集成本 |
| ON_HOLD → CANCELLED | 项目经理/管理员 | 暂停状态（业务扩展，暂停项目最终决策为取消） | 终态，已归集成本保留 |

> **cancelProject 多源声明（对齐代码 `cancelProject:105-117`）**：`cancelProject` 接受 DRAFT/OPEN/ON_HOLD 多源（仅拒绝 COMPLETED/CANCELLED 终态）。owner doc 原仅声明 OPEN 单源是文档漂移；DRAFT/ON_HOLD→CANCELLED 是 OPEN 取消的业务扩展（暂停项目最终决策为取消、草稿项目放弃立项）。

### 3. 终态与恢复

- 终态：`已完成（COMPLETED）`、`已取消（CANCELLED）`。
- 终态不可直接恢复；若需重启，新建项目或在原项目下记录"重新激活"事件（可选）。
- 已取消项目保留已归集成本，不可删除（审计要求）。

### 4. 异常路径

| 异常场景 | 处理 |
|----------|------|
| 完成时仍有未结束任务 | `closeProject` 经 `validateTasksFinished` 校验：STRICT 模式（`erp-prj.strict-project-task-completion-check` 默认 true）存在未结束任务（TODO/IN_PROGRESS/BLOCKED）时抛 `ERR_PROJECT_HAS_UNFINISHED_TASKS`，提示先 `completeTask` 或取消剩余任务；WARN 模式 LOG.warn 放行 |
| 暂停后仍有费用流入 | 配置控制：暂停项目拒绝新费用归集（或允许但标记） |
| 预算超支 | 警告或拦截（按配置），不阻止状态迁移 |
| 立项时缺必填字段 | `startProject` 经 `validateStartPreconditions` 校验：STRICT 模式（`erp-prj.strict-project-start-precheck` 默认 true）缺项目名/起止日期/预算或 startDate>endDate 时抛 `ERR_PROJECT_START_PRECONDITION_FAILED`；WARN 模式 LOG.warn 放行 |
| 并发状态变更 | 乐观锁 |
| 项目删除 | 草稿可删除；进行中及以后只能取消/完成，不可删除 |

### 5. 可达性

从 DRAFT 可达 OPEN；从 OPEN 可达 ON_HOLD、COMPLETED、CANCELLED；ON_HOLD 可回 OPEN。无不可达状态，无死锁。ON_HOLD↔OPEN 是合法往复，退出条件为 COMPLETED/CANCELLED。

### 6. 角色与权限

| 迁移 | 执行角色 |
|------|----------|
| 立项（DRAFT→OPEN） | 项目经理/管理员 |
| 暂停/恢复 | 项目经理 |
| 完成 | 项目经理/管理员（因影响项目报表与成本结转） |
| 取消 | 项目经理/管理员 |

### 7. 外部依赖

| 外部场景 | 内部处理 |
|----------|----------|
| 采购/销售/费用单据引用项目 | 通过 `projectId` 字段标注，项目关闭后拒绝引用 |
| 工时触发成本凭证 | 通过 `IErpFinAcctDocProvider` 注册工时成本 businessType |

### 8. TODO / 任务策略

| 状态 | 是否产生 TODO | TODO 类型 |
|------|---------------|-----------|
| DRAFT | 是 | assigned（项目经理）—— 待立项 |
| OPEN | 是 | monitor（进度监控）—— 项目执行中 |
| ON_HOLD | 是 | assigned（项目经理）—— 暂停项目待决策恢复/取消 |
| COMPLETED/CANCELLED | 否 | — |

### 9. 场景演练

#### 场景 A：研发项目 happy path

1. 创建研发项目（DRAFT）→ 立项（OPEN）。
2. 成员提交工时 → 触发项目成本凭证（借项目成本/贷应付薪酬）。
3. 采购项目相关物料 → 采购单标注项目 → 费用归集。
4. 项目交付完成 → OPEN → COMPLETED → 出项目成本/利润报表。

#### 场景 B：项目暂停与恢复

1. 项目因外部原因暂停 → OPEN → ON_HOLD。
2. 期间费用归集暂停（配置拦截）。
3. 恢复 → ON_HOLD → OPEN → 继续归集。

#### 场景 C：项目取消

1. 项目取消 → OPEN → CANCELLED。
2. 已归集成本保留，可查询已取消项目的成本报表（用于经验沉淀）。

### 10. 与设计文档一致性

- 项目辅助核算模型见 `projects/README.md`。
- 状态码归 `model/app-erp-projects.orm.xml`。
- 工时成本凭证见 `finance/posting.md`。

---

## 适用对象二：任务（Task）

任务状态机较简单，4 态：

```
待开始 (TODO)
  ├─ 开始 → 进行中 (IN_PROGRESS)
  │           ├─ 完成 → 已完成 (DONE)
  │           └─ 阻塞 → 阻塞 (BLOCKED)
  │                        └─ 解除阻塞 → 进行中 (IN_PROGRESS)
  └─ 取消 → 已取消（隐含于项目取消）
```

### 任务依赖规则

- 任务可有前置依赖（`dependsOn`）。
- 前置任务未完成时，后继任务不可开始（迁移 TODO→IN_PROGRESS 时校验）。
- 依赖成环时拒绝（DAG 校验）。

任务状态机的其他维度（异常/角色/TODO）与项目类似，不重复展开；审查时同样使用提示词。

> **详细机制**：依赖模型（单前置 vs 多前置 Decision）、成环检测算法（上行链 + HashSet + maxDepth）、状态迁移完整链（startTask/completeTask/blockTask/unblockTask）、配置点、错误码、Non-Goal 见 `task-dag.md`。

---

## 适用对象三：CRUD 桩实体状态机（Deferred）

> **状态：Deferred（P1-MA2-069）**。以下实体的 `status`/`docStatus` 字段绑定字典，但当前为 CRUD 桩（零 BizMutation writer）或经单一聚合器入口写入，状态字段不参与主路径迁移判定。dict 死状态保留为预留语义入口（与 R1.13/R1.14/R1.15/R1.18-R1.20 先例一致——保留优于删除，避免 ORM `ext:dict` 改动触发 codegen 漂移与数据迁移）。

### ErpPrjMilestone（里程碑）

- `status` 绑定 `erp-prj/task-status` 字典（TODO/IN_PROGRESS/DONE/BLOCKED 四态）。
- **零 writer**：`ErpPrjMilestoneBizModel` 为 18 行 CRUD 桩，无 `setStatus` 调用——四态全为 dict 死状态。
- CRUD 桩为主路径可用（查询/编辑经 CRUD 管道）；完整状态机（startMilestone/achieveMilestone）属 successor。

### ErpPrjBilling（项目开票）

- `docStatus` 绑定 `erp-prj/project-status` 字典（DRAFT/OPEN/ON_HOLD/COMPLETED/CANCELLED 五态）。
- **零 writer**：`ErpPrjBillingBizModel` 为 18 行 CRUD 桩，无 `setStatus` 调用——五态全为 dict 死状态。
- CRUD 桩为主路径可用；完整状态机（submitForApproval/approve/reject）属 successor。

### ErpPrjCostCollection（成本归集头）

- `docStatus` 绑定 `erp-prj/project-status` 字典，但 `ProjectCostAggregator`/`ExpenseCostAggregator` 经单一聚合器入口写 `DOC_STATUS_APPROVED="APPROVED"`。
- **dict-value drift**：`APPROVED` 不在 `erp-prj/project-status` 字典内（字典仅 DRAFT/OPEN/ON_HOLD/COMPLETED/CANCELLED）。归集行为正确（聚合器单一入口写入），仅按 dict 筛选层失效。
- 详见 `cost-collection.md §4.2`。

### Successor 触发条件

当**项目管理全面状态机**需求落地时，实现：
1. ErpPrjMilestone：`startMilestone`/`achieveMilestone` BizMutation + 独立字典（复用 task-status 或新增 milestone-status）。
2. ErpPrjBilling：`submitForApproval`/`approve`/`reject` BizMutation + 独立字典。
3. ErpPrjCostCollection：新增独立 `erp-prj/cost-collection-status` 字典 + ORM `ext:dict` 改绑，收敛 APPROVED drift。

## 审查提示

审查本状态机时，使用 `docs/skills/state-machine-business-review-prompt.md`，重点检查：
- 项目完成时未结束任务的处理是否明确（`validateTasksFinished` config-gated STRICT/WARN）。
- 立项前置字段校验是否明确（`validateStartPreconditions` config-gated STRICT/WARN）。
- 任务依赖成环是否校验（DAG）。
- 暂停项目的费用归集控制是否配置化。
- 工时成本凭证的触发是否覆盖（与 finance 的业财打通）。
