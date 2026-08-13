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

---

## 适用对象四：工时记录（ErpPrjTimesheet）

> 工时记录状态机为 `status` 单轴，绑定标准审批字典 `wf/approve-status`（四态：UNSUBMITTED/SUBMITTED/APPROVED/REJECTED）。
> 实体级 Bean：`ErpPrjTimesheetStateMachine`。业务语义详见 `cost-collection.md §2`。

### 1. 状态定义

| 状态 | 业务含义 | 可被新单据引用 |
|------|----------|----------------|
| 未提交（UNSUBMITTED） | 初始态：草稿/撤回后可重新提交 | — |
| 已提交（SUBMITTED） | 待审批 | — |
| 已审批（APPROVED） | 业务终态（可逆）：触发项目成本归集 + 业财过账 | 是（成本归集引用） |
| 已驳回（REJECTED） | dict 死状态（见下） | — |

### 2. 迁移矩阵

| 迁移 | 触发人 | 前置条件 | 结果 |
|------|--------|----------|------|
| UNSUBMITTED → SUBMITTED | 工时提交人 | 项目 OPEN + 任务允许（TODO/IN_PROGRESS）+ 成本率解析 + 预算检查（config-gated WARNING/STRICT） | 待审批 |
| SUBMITTED → APPROVED | 审批人 | 提交态 | 触发 `TimesheetPostingDispatcher.tryPost`（PROJECT_COST_COLLECTION 凭证，过账成功 `posted=true`）+ `ProjectCostAggregator` 归集行回写 |
| SUBMITTED → UNSUBMITTED | 审批人 | 提交态（驳回） | 回到可重新提交 |
| SUBMITTED → UNSUBMITTED | 工时提交人 | 撤回（cancel 撤回语义） | 回到未提交 |
| APPROVED → UNSUBMITTED | 工时提交人 | APPROVED+posted 先红冲过账（`postingDispatcher.reverse` + 清 posted 契约） | 回到未提交 |

### 3. 终态与恢复

- 业务终态：`已审批（APPROVED）`。其为**可逆终态**——经 `cancel` 红冲过账后置回 UNSUBMITTED（对齐采购/资产审批轴可逆终态先例）。`posted`（业财过账契约，boolean）不入状态轴，红冲闭环以 `posted` 为契约管理。
- 终态不可直接恢复为 APPROVED；撤回后须重新 submit→approve（过账重发）。

### 4. 异常路径

| 异常场景 | 处理 |
|----------|------|
| 非提交态审批 | `ERR_TIMESHEET_ILLEGAL_STATUS_TRANSITION`（固定守卫委托 Bean） |
| 项目非 OPEN / 任务不允许 | `ERR_TIMESHEET_PROJECT_NOT_OPEN` / `ERR_TIMESHEET_TASK_NOT_ALLOWED`（动态守卫，保留 Processor） |
| 成本率无法解析 | `ERR_COST_RATE_NOT_AVAILABLE`（动态守卫） |
| 预算超支 | `ERR_BUDGET_EXCEEDED`（STRICT 模式）/ 警告放行（WARNING 模式） |
| 过账失败 | `TimesheetPostingDispatcher` catch 块返回 false（`posted` 不被置 true）+ 派发 `prj.timesheet-posting-failure` 告警；不阻塞终态（失败隔离） |
| cancel APPROVED+posted | 先红冲过账 + 清 posted/postedAt/postedBy，再置 UNSUBMITTED |
| 并发状态变更 | 乐观锁 |

### 5. 可达性

从 UNSUBMITTED 可达 SUBMITTED、APPROVED。REJECTED 不可达（死状态，见 §6）。

### 6. 漂移与裁决（cancel→UNSUBMITTED + REJECTED 死状态）

- **`cancel→UNSUBMITTED` = intentional legacy behavior（撤回语义）**：`wf/approve-status` 字典无 CANCELLED 值，工时 `cancel` 目标态为 UNSUBMITTED（撤回/重置，非作废）。基线 `cancel` 对全部 dict 值放行（既有 `ErpPrjTimesheetCancelProcessor` 对状态不抛错，仅 APPROVED+posted 时先红冲过账）。owner doc 原计划「cancel→CANCELLED via docStatus」与 BizModel Javadoc「reject→DRAFT / cancel→CANCELLED」均为 doc drift，已就地修正为 live code 真值（reject→UNSUBMITTED / cancel→UNSUBMITTED）。
- **REJECTED 死状态**：`wf/approve-status` 含 REJECTED，但工时 `reject` 目标态为 UNSUBMITTED（非 REJECTED），无 writer 产生 REJECTED → REJECTED 对工时为 dict 死状态，保留为字典共享语义（不从 ORM 删除）。
- **Bean 元数据建模说明**：`ErpPrjTimesheetStateMachine.transitions()` 的 cancel 边仅列实际有意义的来源 {SUBMITTED, APPROVED}（APPROVED 经红冲置回 UNSUBMITTED；SUBMITTED 撤回）；`assertCanCancel` 对全部 dict 值放行（含 UNSUBMITTED no-op 与死状态 REJECTED）——这是有意的建模选择（no-op/不可达态不计入迁移边，但守卫如实反映 live code 对状态不抛错的行为），与 `ErpPurOrderDocumentStateMachine` 守卫宽于 transitions() 的先例一致。
- **`cancel→CANCELLED` 目标态迁移**：作 successor 登记（PM 要求工时独立作废状态时，新增 dict 值 + cancel→CANCELLED 迁移 + BizMutation）。

### 7. 角色与权限

| 迁移 | 执行角色 |
|------|----------|
| submit | 工时提交人 |
| approve | 审批人 |
| reject | 审批人 |
| cancel（撤回） | 工时提交人 |

### 8. 与设计文档一致性

- 工时成本归集见 `cost-collection.md §2/§4.2`。
- 工时成本凭证见 `finance/posting.md`。
- Bean 契约见 `docs/architecture/entity-state-machine-bean.md`。

---

## 适用对象五：项目结算单（ErpPrjProjectSettlement，docStatus + approveStatus 双轴）

> 项目结算单为**双轴状态**：`docStatus`（业务生命周期，绑定 `erp-prj/project-status` 字典）+ `approveStatus`（审批轴，绑定 `wf/approve-status`）。
> 实体级 Bean：`ErpPrjProjectSettlementDocumentStateMachine`（docStatus 轴）+ `ErpPrjProjectSettlementApprovalStateMachine`（approveStatus 轴，双轴分离，契约 §3）。
> `posted`（业财过账契约，boolean）不入任一轴。业务语义详见 `profitability.md`。

### 1. 状态定义

#### docStatus 轴

| 状态 | 业务含义 |
|------|----------|
| 草稿（DRAFT） | 初始态：建头待审批 |
| 已审批（APPROVED） | doApprove 双轴同动写入（**dict-value drift，见 §5**） |
| 已作废（CANCELLED） | 终态 |
| OPEN / ON_HOLD / COMPLETED | 共享 dict 死状态（见 §5） |

#### approveStatus 轴

| 状态 | 业务含义 |
|------|----------|
| 未提交（UNSUBMITTED） | 初始态 |
| 已提交（SUBMITTED） | 待审批 |
| 已审批（APPROVED） | 业务终态（真终态） |
| 已驳回（REJECTED） | 可达汇（无出边） |

### 2. 迁移矩阵

#### docStatus 轴

| 迁移 | 触发 | 前置条件 | 结果 |
|------|------|----------|------|
| DRAFT → APPROVED | `approve`（doApprove 双轴同动） | approveStatus 守卫通过（SUBMITTED，或 RELAXED 模式 UNSUBMITTED） | 触发 `ProjectSettlementPostingDispatcher.tryPost`（posted=true）；CLOSE 类型额外 `createAndActivateAsset` 转固 |
| DRAFT → CANCELLED | `cancel` | 非 CANCELLED | 终态；posted 单据先红冲 + 回滚资产 |
| APPROVED → CANCELLED | `cancel` | 非 CANCELLED | 终态；posted 单据先红冲 + 回滚资产 |

#### approveStatus 轴

| 迁移 | 触发 | 前置条件 | 结果 |
|------|------|----------|------|
| UNSUBMITTED → SUBMITTED | `submit` | UNSUBMITTED | 待审批 |
| SUBMITTED → APPROVED | `approve` | SUBMITTED（STRICT 默认）；RELAXED 模式允许 UNSUBMITTED 直审 | 双轴同动 docStatus→APPROVED |
| SUBMITTED → REJECTED | `reject` | SUBMITTED | 驳回 |

> **`reverseSettlement` 不写任一轴**：纯 `posted` 轴冲销动作（`postingDispatcher.reverse` + `rollbackAssetIfNeeded` + 清 posted），在两 Bean 中均无迁移边。

### 3. 终态与恢复

- **docStatus 轴终态**：`CANCELLED`（无 writer 将 docStatus 从 CANCELLED 迁出）。APPROVED 非终态（经 cancel 有出边）。
- **approveStatus 轴终态**：`APPROVED`（真终态——cancel 只写 docStatus、reverseSettlement 只写 posted，无 writer 将 approveStatus 从 APPROVED 迁出）。
- 终态不可直接恢复。

### 4. 异常路径

| 异常场景 | 处理 |
|----------|------|
| 非法审批态迁移 | `ERR_SETTLEMENT_ILLEGAL_STATUS_TRANSITION`（固定守卫委托 Bean） |
| 已作废单据再次 cancel | docStatus=CANCELLED → 非法（Document Bean 守卫） |
| 转固失败 | `ERR_SETTLEMENT_CAPITALIZATION_FAILED` |
| 过账失败 | `ProjectSettlementPostingDispatcher` 失败隔离（不阻塞终态） |
| reverseSettlement 非 posted 单据 | `ERR_SETTLEMENT_ILLEGAL_STATUS_TRANSITION`（posted=true 硬前置） |
| 并发状态变更 | 乐观锁 |

### 5. 漂移与裁决（APPROVED dict-value drift + 共享 dict 死状态）

- **docStatus APPROVED dict-value drift**：`doApprove` 写入 `DOC_STATUS_APPROVED="APPROVED"`，但 `erp-prj/project-status` 字典仅含 DRAFT/OPEN/ON_HOLD/COMPLETED/CANCELLED——APPROVED 被写入但不在字典内（对齐 `ErpPrjCostCollection` §适用对象三先例）。Bean 按既有 writer 建模该边（保持行为），按 dict 筛选层对该值失效。dict 补全/rebind 列 successor（ORM ask-first 保护区）。
- **共享 dict 死状态保留（Decision）**：`erp-prj/project-status` 的 OPEN/ON_HOLD/COMPLETED 对结算单**无 writer**（死状态），保留为预留语义入口（对齐 §适用对象三 + assets 保留死状态先例），不从 ORM 删除。
- **approve 的 config-gated 动态守卫**：`erp-prj.settlement-require-approval`（默认 true=STRICT，仅 SUBMITTED 可审批；false=RELAXED，允许 UNSUBMITTED 直审）。Approval Bean 承载默认 STRICT 矩阵（SUBMITTED 单源）；RELAXED 分支为 config-gated 动态扩展，保留在 `ErpPrjProjectSettlementProcessor.validateTransitionForApprove`。

### 6. 可达性

- docStatus 轴：从 DRAFT 可达 APPROVED、CANCELLED。OPEN/ON_HOLD/COMPLETED 不可达（死状态）。
- approveStatus 轴：从 UNSUBMITTED 可达 SUBMITTED、APPROVED、REJECTED。

### 7. 与设计文档一致性

- 结算盈利计算见 `profitability.md`。
- 转固级联见 `assets/state-machine.md`。
- Bean 契约见 `docs/architecture/entity-state-machine-bean.md`。

---

## 审查提示

审查本状态机时，使用 `docs/skills/state-machine-business-review-prompt.md`，重点检查：
- 项目完成时未结束任务的处理是否明确（`validateTasksFinished` config-gated STRICT/WARN）。
- 立项前置字段校验是否明确（`validateStartPreconditions` config-gated STRICT/WARN）。
- 任务依赖成环是否校验（DAG）。
- 暂停项目的费用归集控制是否配置化。
- 工时成本凭证的触发是否覆盖（与 finance 的业财打通）。
