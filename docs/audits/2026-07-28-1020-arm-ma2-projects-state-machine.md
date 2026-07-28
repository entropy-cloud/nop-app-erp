# MA2 项目状态机审查（A2.13）

> 里程碑：MA2（业务正确性层 / 状态机正确性维度）
> 域/功能模块：projects / 项目 5 态 + 任务 4 态 + 成本归集 + 开票 + 工时 + 里程碑 + 项目结算 + PnL（16 状态字段）
> 审计 plan：`docs/plans/2026-07-28-1020-2-audit-remediation-ma2-projects-state-machine.md`
> 行为基线：`docs/design/projects/{state-machine,task-dag,cost-collection,profitability}.md` + `docs/architecture/{processor-extension-pattern,posting-exemptions}.md`
> Skill：`docs/skills/state-machine-business-review-prompt.md`（10 维度审查方法）
> 范围：16 状态字段（plan baseline），实际审查经逐文件全文阅读 + grep 验证
> 审计执行：2026-07-28
> 上游基线：MA1 done（P1-MA1-010 propId 5 列 + P1-MA1-022 跨域只读 9 域合并含 prj 已登记，本审计复核状态机角度）；A2.1 P2P done（项目采购引用 projectId 归集成本）；A2.2 O2C done（项目销售引用 projectId 归集成本）；A2.5a done（finance 凭证 reverseApprove 红冲闭环 + tryPost 吞异常悬挂同型范式）；A2.6a done（TimesheetPostingDispatcher 跨域过账同型）；A2.11 done（inventory posted 三件套 + tryPost 容错同型范式）；A2.12 done（quality NCR 过账 + 跨域只读 + tryPost 容错同型范式）

## 1. 审查范围与状态字段清单

| 实体 / 组件 | 状态轴（dict） | 实现文件 | 审查方式 |
|------------|---------------|----------|---------|
| **ErpPrjProject**（项目 5 态） | `status`(erp-prj/project-status DRAFT/OPEN/ON_HOLD/COMPLETED/CANCELLED) | `ErpPrjProjectBizModel.java`（132 行 startProject/holdProject/resumeProject/closeProject/cancelProject/requireReferenceable/refreshActualCost） | 全文逐行 |
| **ErpPrjTask**（任务 4 态） | `status`(erp-prj/task-status TODO/IN_PROGRESS/DONE/BLOCKED) + `dependsOnId` 单前置 | `ErpPrjTaskBizModel.java`（260 行 startTask/completeTask/blockTask/unblockTask + findPredecessors/findSuccessors/getDependencyChain + validateDependency 钩子） | 全文逐行 |
| **任务依赖 DAG 校验** | — | `TaskDependencyValidator.java`（172 行 detectCycle 上行链+HashSet+maxDepth + collectPredecessors + acceptsTimesheet） | 全文逐行 |
| **ErpPrjTimesheet**（工时状态机） | `status`(wf/approve-status UNSUBMITTED/SUBMITTED/APPROVED/REJECTED) + `posted`/`postedAt`/`postedBy` 三件套 | `ErpPrjTimesheetBizModel.java`（235 行 submit/approve/reject/cancel + validateProjectReferenceable + validateTaskAcceptsTimesheet + runBudgetCheckHook） | 全文逐行 |
| **工时成本凭证过账** | — | `TimesheetPostingDispatcher.java`（160 行 tryPost 容错 + reverse 红冲 + buildEvent） + `ProjectPostingExecutor.java`（42 行 IErpFinVoucherBiz Facade） + `ProjectCostCollectionProvider.java` | 全文逐行 |
| **ErpPrjCostCollection**（成本归集） | `docStatus`(erp-prj/project-status 复用 ⚠️) + `approveStatus`(wf/approve-status) + `posted`/`postedAt`/`postedBy` 三件套 + 多币种四件套（exchangeRate/amountSource/amountFunctional — P1-MA1-010 propId 缺失） | `ErpPrjCostCollectionBizModel.java`（43 行 CRUD + refreshExpenseCost config-gated） + `ProjectCostAggregator.java`（190 行 aggregateFromTimesheet + refreshActualCost） + `ExpenseCostAggregator.java`（222 行 refreshExpenseCost） + `ProjectCostCollectionProvider.java` | 全文逐行 |
| **ErpPrjBilling**（项目开票） | `docStatus`(erp-prj/project-status 复用 ⚠️) + `approveStatus`(wf/approve-status) + `posted`/`postedAt`/`postedBy` 三件套 + 多币种四件套部分（amountSource/amountFunctional — P1-MA1-010） | `ErpPrjBillingBizModel.java`（**18 行 CRUD 桩**，零 setStatus writer） | 全文逐行 |
| **ErpPrjMilestone**（里程碑） | `status`(erp-prj/task-status 复用 ⚠️) | `ErpPrjMilestoneBizModel.java`（**18 行 CRUD 桩**，零 setStatus writer） | 全文逐行 |
| **ErpPrjProjectSettlement**（项目结算） | `docStatus`(erp-prj/project-status 复用 ⚠️) + `approveStatus`(wf/approve-status) + `settlementType`(erp-prj/settlement-type FINAL/INTERIM/CLOSE) + `posted` | `ErpPrjProjectSettlementBizModel.java`（71 行 Facade） + `ErpPrjProjectSettlementProcessor.java` + 4 per-mutation Processor（SubmitForApproval/Approve/Reject/Cancel） | 全文逐行 |
| **ErpPrjProjectPnl**（项目损益） | `calcStatus`(erp-prj/pnl-calc-status PENDING/CALCULATED) + `docStatus` | `ErpPrjProjectPnlBizModel.java` + `ProjectPnlCalculator.java`（:122 写 CALCULATED） | 全文逐行 |
| **成本率/预算检查助手** | — | `CostRateResolver.java` + `BudgetChecker.java`（config-gated WARNING/STRICT） | 全文逐行 |

16 状态字段分布在 8 个状态承载实体（含复用字典）+ 工时过账/成本归集助手（与 plan baseline 一致 ✓；**注**：plan Draft Review iter 1 已移除不存在的 `ErpPrjDeliverable`/「交付物轴」，本审计对齐——里程碑轴由 `ErpPrjMilestone` 承载）。

## 2. 10 维度审查

### 2.1 维度「状态定义」

**裁决：PASS（含复用字典语义偏移注记 + Deferred CRUD 空壳清晰性注记）」

#### 项目 status（erp-prj/project-status DRAFT/OPEN/ON_HOLD/COMPLETED/CANCELLED 5 态）

✅ **每个状态表达「等待什么」**（owner doc `state-machine.md §1` 表）：DRAFT=等待立项确认；OPEN=活跃执行可归集成本；ON_HOLD=暂停（等待恢复决策）；COMPLETED/CANCELLED 是终态。dict option 与 `ErpPrjConstants.PROJECT_STATUS_*` 常量 1:1 对齐。

✅ **ON_HOLD 语义清晰**——「等待恢复决策」（owner doc §1 表 + §2「暂停费用归集（可配置）」+ §8「ON_HOLD 产生 assigned TODO——项目经理待决策恢复/取消」）。`requireReferenceable:46` 仅允许 OPEN 状态被新单据引用（含 ON_HOLD 在内的非 OPEN 全拒绝），故 ON_HOLD 业务上明确为「暂停执行+拒绝新归集」等待点。

✅ **终态语义清晰**：COMPLETED（正常完成，可出项目成本/利润报表）+ CANCELLED（已归集成本保留）均无出边（owner doc §3 强制；startProject/holdProject/resumeProject/closeProject/cancelProject 全守卫源态非终态）。

#### 任务 status（erp-prj/task-status TODO/IN_PROGRESS/DONE/BLOCKED 4 态）

✅ **每个状态表达业务等待点**（`task-dag.md §4.1` 表）：TODO=等待启动；IN_PROGRESS=执行中（允许录入工时）；BLOCKED=等待解除阻塞原因；DONE 是终态。dict option 与 `ErpPrjConstants.TASK_STATUS_*` 常量 1:1 对齐。

✅ **是否允许录入工时** 字段语义清晰（`task-dag.md §4.1`：TODO/IN_PROGRESS 允许，BLOCKED/DONE 拒绝）——`TaskDependencyValidator.acceptsTimesheet:168-171` + `ErpPrjTimesheetBizModel.validateTaskAcceptsTimesheet:179-186` 双重守卫。

#### 工时 status（wf/approve-status 标准审批轴 UNSUBMITTED/SUBMITTED/APPROVED/REJECTED 4 态）

✅ **使用平台标准审批轴**（非 erp-prj/timesheet-status 字典）——`ErpPrjTimesheet.status` 列 `ext:dict="wf/approve-status"`（ORM :230 确认）。`erp-prj/timesheet-status` 字典（DRAFT/SUBMITTED/APPROVED 3 态）存在但**未被任何列绑定**——是孤立字典（与 hr P2-MA1-020 orphan dict salary-approval-status 同型）。代码使用 `APPROVE_STATUS_*` 常量（UNSUBMITTED/SUBMITTED/APPROVED/REJECTED）合规。

⚠️ **孤立字典 erp-prj/timesheet-status**——未绑定列 + 不影响状态机判定。watch-only（本审计不单独登记，归并到 §6 残留风险，MR1 顺手清理）。

#### 成本归集/开票/里程碑/结算/PnL 状态轴（复用字典语义偏移）

⚠️ **5 处复用 `erp-prj/project-status` 字典语义偏移**：

| 实体 | 列 | 绑定字典 | 业务语义 | 偏移 |
|------|----|----------|---------|------|
| ErpPrjProject.status | status | erp-prj/project-status | 项目生命周期 5 态 | ✅ 对齐 |
| ErpPrjCostCollection.docStatus | docStatus | erp-prj/project-status | 归集单据状态（DRAFT/APPROVED/CANCELLED） | ❌ 复用 project-status 字典但代码写 `DOC_STATUS_APPROVED="APPROVED"`（project-status 字典无 APPROVED 项——DRAFT/OPEN/ON_HOLD/COMPLETED/CANCELLED） |
| ErpPrjBilling.docStatus | docStatus | erp-prj/project-status | 开票单据状态 | ❌ 同上（APPROVED 不在字典） + 5 态全 dead（BillingBizModel 18 行 CRUD 桩零 writer） |
| ErpPrjProjectSettlement.docStatus | docStatus | erp-prj/project-status | 结算单据状态 | ❌ 同上（APPROVED 不在字典） |
| ErpPrjMilestone.status | status | erp-prj/task-status | 里程碑进度状态 | ❌ 复用任务 4 态字典（TODO/IN_PROGRESS/DONE/BLOCKED）但 MilestoneBizModel 18 行 CRUD 桩零 writer——4 态全 dead |

按 finance A2.5a P1-MA2-031（DRAFT→CANCELLED 不可达）+ mfg A2.6a P1-MA2-035（作业卡 TRANSFERRED 死状态）+ mfg A2.6b P1-MA2-036（MRP CANCELLED + 预测 CONSUMED 死状态）+ hr A2.7a P1-MA2-040/041/042 + hr A2.7b P1-MA2-043/045 + inv A2.11 P1-MA2-063（PickingOrder PICKING/PICKED 死状态）+ qa A2.12 P1-MA2-065（QualityGoal/RiskRegister/Calibration/Review/SPC-CalcStatus-STALE/CAPA-OVERDUE dict 死状态合并）同型裁决。合并登记 **P1-MA2-069**（详 §4）。

#### 工时/归集/开票/结算 approveStatus 轴（标准 wf/approve-status）

✅ **使用平台标准审批轴**——4 实体均 `ext:dict="wf/approve-status"`。ProjectSettlement 经 Processor 完整覆盖 UNSUBMITTED→SUBMITTED→APPROVED/REJECTED + CANCELLED 4 轴。CostCollection/Billing 由聚合器写入 APPROVED（CRUD 桩无独立审批流，归集即视为已审批——owner doc `cost-collection.md §4.2` 设计语义）。

#### PnL calcStatus 轴（erp-prj/pnl-calc-status PENDING/CALCULATED 2 态）

✅ **2 态全可达**：PENDING 经 codegen 默认值承载（实体创建）+ CALCULATED 经 `ProjectPnlCalculator.calculate:122 setCalcStatus(CALCULATED)`。`findLatestCalculated:137` 按 CALCULATED 过滤。

---

### 2.2 维度「转换完整性」

**裁决：FAIL（2 项新 P1 + 1 项字典语义偏移 + owner doc Deferred 落实）」

#### 项目生命周期迁移矩阵（核心）

| From → To | 触发 | 前置（owner doc §2） | 实际前置（代码） | 代码位置 | 裁决 |
|-----------|------|---------------------|----------------|----------|------|
| DRAFT → OPEN | `startProject` | 项目信息完整、起止日期有效、预算已定 | **仅 status==DRAFT 检查**（无信息完整/日期/预算校验） | `ErpPrjProjectBizModel.startProject:84-87 + transition:119-130` | ❌ **P1-MA2-070**（缺前置校验） |
| OPEN → ON_HOLD | `holdProject` | 进行中状态 | status==OPEN ✓ | `holdProject:91-94` | ✅ |
| ON_HOLD → OPEN | `resumeProject` | 暂停状态 | status==ON_HOLD ✓ | `resumeProject:98-101` | ✅ |
| OPEN → COMPLETED | `closeProject` | 任务已结束（或确认剩余不再执行）、成本已归集 | **仅 status==OPEN + refreshActualCost + refreshExpenseCost**（**无任务已结束校验**） | `closeProject:62-80` | ❌ **P1-MA2-067**（缺任务结束前置） |
| OPEN → CANCELLED | `cancelProject` | 进行中状态 | status != COMPLETED/CANCELLED（接受 DRAFT/OPEN/ON_HOLD） | `cancelProject:105-117` | ⚠️ **P1-MA2-070 part**（接受 DRAFT/ON_HOLD 超出 owner doc 声明 OPEN 单源） |

❌ **P1-MA2-067**（详 §4）：`closeProject:62-80` 实现 OPEN→COMPLETED 迁移，**仅刷新实际成本 + 费用归集（config-gated）+ 改状态**——**完全缺失「任务已结束」前置校验**。owner doc `state-machine.md §迁移完整性 L35` 显式声明 OPEN→COMPLETED 前置「任务已结束（或确认剩余不再执行）、成本已归集」+ §审查提示「项目完成时未结束任务的处理是否明确」。当前实现允许「项目有未结束任务（TODO/IN_PROGRESS/BLOCKED）直接关闭」，违反 owner doc 核心契约。属设计漏洞非数据破坏（仍需人工 closeProject 动作 + 终态保留归集成本可查询），按 finance/mfg/hr/inv/qa 同型 dict 死状态 + CRUD 桩 P1 范式裁决。

❌ **P1-MA2-070**（详 §4）：(a) `startProject:84-87` 实现 DRAFT→OPEN 迁移，**仅 status==DRAFT 检查**——owner doc §迁移完整性 L32 显式声明前置「项目信息完整、起止日期有效、预算已定」，代码无任何字段校验（项目名/起止日期/预算字段为空的 DRAFT 项目可直接立项）。(b) `cancelProject:105-117` 接受 DRAFT/ON_HOLD 源态超出 owner doc §迁移完整性 L36 显式声明的 OPEN 单源（DRAFT→CANCELLED + ON_HOLD→CANCELLED 路径代码可达但 owner doc 未声明）。属设计契约漂移非数据破坏（终态 CANCELLED 保留归集成本正确 + DRAFT 取消无归集影响 + ON_HOLD 取消符合「暂停项目最终决策为取消」业务场景）。

#### ON_HOLD 费用归集暂停（owner doc §2 config-gated Deferred）

✅ **owner doc §2 「暂停费用归集（可配置）」+ §4 异常路径「配置控制：暂停项目拒绝新费用归集（或允许但标记）」**——本审计确认实现路径：

- **新单据引用**经 `requireReferenceable:43-52` 仅允许 OPEN 状态被引用——**ON_HOLD 在内的非 OPEN 全拒绝**（抛 `ERR_PROJECT_NOT_REFERENCEABLE`）。故新采购/销售/费用单据在 ON_HOLD 期间无法标注 projectId。
- **工时录入**经 `ErpPrjTimesheetBizModel.validateProjectReferenceable:154-167` 同样仅允许 OPEN——**ON_HOLD 拒绝新工时提交**。
- **已审核报销单的归集扫描**（`ExpenseCostAggregator.refreshExpenseCost`）**不检查项目状态**——但该入口仅由 `closeProject:73-75` 主动调用（关闭前刷新费用归集保证关账完整），ON_HOLD 期间无任何调用方触发 refreshExpenseCost。

**裁决**：ON_HOLD 费用归集暂停经「requireReferenceable 单一入口拒绝非 OPEN」+「ExpenseCostAggregator 仅 closeProject 触发」双路径落实。owner doc §2 config-gated Deferred 指「配置控制：暂停项目拒绝新费用归集（或允许但标记）」——当前实现是**硬拒绝**（requireReferenceable 不分 config 全拒绝非 OPEN），比 owner doc 描述的「可配置」更严格。**不破坏状态机**——ON_HOLD 期间无悬挂费用归集。**不登记为新 finding**（owner doc Deferred 落实 + 当前实现比 owner doc 更严格不引入回归）。

#### 任务依赖 DAG 校验（task-dag.md §2 上行链+HashSet+maxDepth）

✅ **完整的 DAG 成环检测**——`TaskDependencyValidator.detectCycle:44-86` 实现 owner doc task-dag.md §2.1 算法：

```
detectCycle(taskId, dependsOnId, loader, maxDepth):
    1. dependsOnId null → return（无前置放行）
    2. taskId == dependsOnId → ERR_TASK_SELF_DEPENDENCY（自环优先）
    3. visited = {taskId}, chainOrder = [taskId]
    4. cursor = dependsOnId, depth = 0
    5. while cursor != null:
         depth++; if depth > maxDepth → ERR_TASK_DEPENDENCY_DEPTH_EXCEEDED
         if cursor ∈ visited → ERR_TASK_DEPENDENCY_CYCLE（chainOrder 序列）
         visited.add(cursor); chainOrder.add(cursor)
         predecessor = loader(cursor); if null → break
         cursor = predecessor.dependsOnId
```

**算法性质合规**：自环优先检测（步骤 2 优先于深度判定）+ O(N) 时间 + O(N) 空间 HashSet + maxDepth 兜底防恶意长链。**与 task-dag.md §2.1 算法描述 1:1 对应**。

✅ **跨项目依赖校验**（task-dag.md §3）——`ErpPrjTaskBizModel.validateDependency:88-94` 守卫 `task.projectId == dependsOnTask.projectId` 否则抛 `ERR_TASK_DEPENDENCY_CROSS_PROJECT`。

✅ **依赖保存钩子双触发**（task-dag.md §10）——`defaultPrepareSave:56-59`（插入）+ `defaultPrepareUpdate:62-65`（修改）均调 `validateDependency`，覆盖保存/更新双路径。

#### 任务状态机迁移矩阵（4 态全迁移）

| From → To | 触发 | 前置（task-dag.md §4.2） | 附加校验 | 代码位置 | 裁决 |
|-----------|------|------------------------|---------|----------|------|
| TODO → IN_PROGRESS | `startTask` | status==TODO | **前置任务须 DONE**（STRICT 模式拦截 / WARN 模式放行，config-gated `erp-prj.task-strict-predecessor-check` 默认 true） | `ErpPrjTaskBizModel.startTask:104-115 + validatePredecessorDone:168-189` | ✅ |
| IN_PROGRESS → DONE | `completeTask` | status==IN_PROGRESS | — | `completeTask:119-129` | ✅ |
| IN_PROGRESS → BLOCKED | `blockTask` | status==IN_PROGRESS | `blockReason` 必填（抛 `ERR_TASK_BLOCK_REASON_REQUIRED`） | `blockTask:133-149` | ✅ |
| BLOCKED → IN_PROGRESS | `unblockTask` | status==BLOCKED | — + 清 `blockReason` | `unblockTask:153-163` | ✅ |

✅ **任务状态机完整覆盖 4 态全迁移 + 前置完成 config-gated + 阻塞原因必填**。非法迁移全抛 `ERR_TASK_ILLEGAL_STATUS_TRANSITION`（参数 taskId/currentStatus/targetStatus）。owner doc task-dag.md §4.2 与代码 1:1 对应。

#### 工时状态机迁移矩阵（4 态 + 跨域过账）

| From → To | 触发 | 前置 | 结果 | 代码位置 | 裁决 |
|-----------|------|------|------|----------|------|
| UNSUBMITTED → SUBMITTED | `submit` | status==UNSUBMITTED | costRate/costAmount 计算 + budgetCheck（config-gated WARNING/STRICT） | `ErpPrjTimesheetBizModel.submit:61-88` | ✅ |
| SUBMITTED → APPROVED | `approve` | status==SUBMITTED | **跨域过账 PROJECT_COST_COLLECTION**（tryPost 容错）+ posted 三件套 + costAggregator.aggregateFromTimesheet | `approve:92-118 + TimesheetPostingDispatcher.tryPost` | ⚠️ 同型悬挂交接（详 §2.4） |
| SUBMITTED → UNSUBMITTED | `reject` | status==SUBMITTED | setStatus(UNSUBMITTED) | `reject:122-131` | ✅ |
| APPROVED → posted=false + UNSUBMITTED | `cancel` | status==APPROVED + posted==true → reverse（红冲凭证） | 清 posted 三件套 + setStatus(UNSUBMITTED) | `cancel:135-150` | ✅ |

✅ **工时状态机完整覆盖标准审批轴 + 跨域过账 + 红冲闭环**。

#### 项目结算状态机（Processor 三轴迁移）

✅ **ErpPrjProjectSettlement 三轴状态机经 Processor 实现**——`ErpPrjProjectSettlementProcessor` + 4 per-mutation Processor（SubmitForApproval/Approve/Reject/Cancel）+ Facade `ErpPrjProjectSettlementBizModel`。状态迁移完整：

- `createSettlement:84-87` 设 docStatus=DRAFT + approveStatus=UNSUBMITTED
- `submit:191` 设 approveStatus=SUBMITTED
- `approve:195-196` 设 approveStatus=APPROVED + docStatus=APPROVED
- `reject:202` 设 approveStatus=REJECTED
- `cancel:206` 设 docStatus=CANCELLED
- `reverseSettlement` 红冲凭证 + 回退

✅ **processor-extension-pattern.md Facade+Processor 两层合规**——BizModel 仅 Facade 编排，状态机迁移与跨域写（转固建卡 + 凭证）全在 Processor。

---

### 2.3 维度「终端状态和恢复」

**裁决：PASS（终态不可恢复 + 已取消保留成本 落实）」

✅ **COMPLETED/CANCELLED 项目终态无出边**——`startProject/holdProject/resumeProject/closeProject` 全部经 `transition` helper 或显式 status 检查，源态为 COMPLETED/CANCELLED 时全抛 `ERR_PROJECT_NOT_CLOSABLE`。`cancelProject:108-113` 显式拒绝 COMPLETED/CANCELLED 源态。

✅ **owner doc §3 「终态不可直接恢复；若需重启，新建项目或在原项目下记录"重新激活"事件（可选）」落实**——代码无任何 COMPLETED/CANCELLED→DRAFT/OPEN 的迁移方法。

✅ **owner doc §3 「已取消项目保留已归集成本，不可删除（审计要求）」落实**——`ErpPrjProject` ORM `useLogicalDelete="true" deleteFlagProp="delVersion"`（逻辑删除非物理删除）+ cancelProject 不删除归集行/工时单/成本归集头（`cancelProject:114-116` 仅 setStatus）。

✅ **任务 DONE 终态无出边**——`startTask/completeTask/blockTask/unblockTask` 全部守卫源态，DONE 状态无任何迁移方法。

✅ **归档与活跃区分**——`ErpPrjProject`/`ErpPrjTask`/`ErpPrjTimesheet`/`ErpPrjCostCollection`/`ErpPrjBilling` 均 `useLogicalDelete=true`（delVersion 字段承载逻辑删除），终态归档不参与活跃 TODO。

---

### 2.4 维度「异常路径」

**裁决：FAIL（1 项新 P1 同型悬挂交接 + owner doc Deferred 落实）」

| 异常场景 | 处理 | 代码位置 | 裁决 |
|----------|------|----------|------|
| **完成时仍有未结束任务** | **未实现**——closeProject 不查 ErpPrjTask.status，不校验是否有 TODO/IN_PROGRESS/BLOCKED 任务 | `closeProject:62-80` | ❌ **P1-MA2-067** |
| **暂停后仍有费用流入** | requireReferenceable 仅允许 OPEN 拒绝新引用 + validateProjectReferenceable 拒绝新工时；ExpenseCostAggregator 仅 closeProject 触发（ON_HOLD 期间无 refresh 入口） | `requireReferenceable:43-52 + validateProjectReferenceable:154-167` | ✅（owner doc §2 Deferred 落实——硬拒绝比可配置更严格） |
| **预算超支** | config-gated WARNING/STRICT——`BudgetChecker.check` 在 `submit:85 runBudgetCheckHook` 工时提交时触发；不阻止状态迁移（owner doc §4 设计） | `ErpPrjTimesheetBizModel.runBudgetCheckHook:192-194 + BudgetChecker` | ✅ |
| 并发状态变更 | ORM `versionProp="version"` 透明乐观锁——ErpPrjProject/ErpPrjTask/ErpPrjTimesheet/ErpPrjCostCollection/ErpPrjBilling/ErpPrjProjectSettlement 全部声明 versionProp | ORM 多实体 | ⚠️ 系统性并发归 **A2.17** |
| **项目删除** | ErpPrjProject `useLogicalDelete=true`——所有状态均走逻辑删除（delVersion）；owner doc §4「草稿可删除；进行中及以后只能取消/完成」由 useLogicalDelete 统一承载（逻辑删除非物理删除，符合审计要求） | ORM + CrudBizModel 默认 | ✅ |
| **任务依赖成环** | DAG 拒绝——`TaskDependencyValidator.detectCycle` 上行链+HashSet+maxDepth（详 §2.2） | `TaskDependencyValidator.detectCycle:44-86` | ✅ |
| **任务依赖自环** | 自环优先检测——`detectCycle:49-52` taskId==dependsOnId 抛 `ERR_TASK_SELF_DEPENDENCY` | `detectCycle:49-52` | ✅ |
| **任务依赖跨项目** | 拒绝——`validateDependency:88-94` 抛 `ERR_TASK_DEPENDENCY_CROSS_PROJECT` | `validateDependency:88-94` | ✅ |
| **任务依赖深度超限** | 拒绝——`detectCycle:65-70` depth > maxDepth 抛 `ERR_TASK_DEPENDENCY_DEPTH_EXCEEDED` | `detectCycle:65-70` | ✅ |
| **任务 startTask 前置未完成** | config-gated STRICT 拦截（默认）/ WARN 放行——`validatePredecessorDone:168-189` STRICT 抛 `ERR_TASK_PREDECESSOR_NOT_DONE`，WARN LOG.warn 放行 | `validatePredecessorDone:168-189` | ✅ |
| **工时成本凭证过账失败悬挂** | TimesheetPostingDispatcher.tryPost 容错——吞所有异常返回 false，工时保持 APPROVED + posted=false（悬挂窗口期），不阻塞终态 | `TimesheetPostingDispatcher.tryPost:51-64 + ErpPrjTimesheetBizModel.approve:102-117` | ⚠️ **P1-MA2-068**（同型悬挂交接） |
| reverse 红冲闭环对称性 | `cancel:138-145` 守卫 posted==true → `postingDispatcher.reverse`（红冲凭证经 IErpFinVoucherBiz Facade）+ 清 posted 三件套；对称 | `cancel:135-150 + TimesheetPostingDispatcher.reverse:69-71` | ✅ |
| 项目关闭后引用 | requireReferenceable 仅允许 OPEN——COMPLETED/CANCELLED 全拒绝新引用 | `requireReferenceable:46-50` | ✅ |

#### 工时成本凭证过账失败悬挂（owner doc §审查提示 + finance P1-MA2-032 + hr P1-MA2-048 + assets P1-MA2-060 + qa A2.12 同型评估）

**状态机角度复核**：

- **`TimesheetPostingDispatcher.tryPost:51-64`** try/catch 吞所有异常（NopException LOG.warn / 其他 LOG.error）返回 false。**不向上传播**。
- **`ErpPrjTimesheetBizModel.approve:102-117`** 调 `postingDispatcher.tryPost(timesheet):102` 后**无条件** `timesheet.setStatus(APPROVED):104`——**不检查返回值**。若返回 true → 设 `posted=true + postedAt + postedBy:108-110`；若返回 false → posted 保持 false（默认值）。
- **悬挂窗口期**：工时进入 APPROVED 终态 + posted=false + **无 finance 凭证创建** + 异常被 hr dispatcher 吞掉**不进入 finance 过账异常工作台**（finance `ErpFinPostingException` 工作台仅捕获 finance 侧未处理异常）+ **期末结账前置检查不覆盖此悬挂**（全 `module-finance/erp-fin-service/.../period/` grep `ErpPrjTimesheet\|timesheet.*posted\|TIMESHEET` 零匹配）。

**裁决**：与 finance A2.5a P1-MA2-032（IGNORED 凭证悬挂）+ hr A2.7b P1-MA2-048（salary 过账悬挂）+ assets A2.10 P1-MA2-060（Capitalization/Disposal 过账悬挂）+ qa A2.12（MANUAL_POST NCR 过账悬挂）**同型根因**（posting dispatcher 容错设计 + DeferredPostingSweepJob 兜底归 finance 域通用机制）。**不升 P0**：(1) 失败模式需 finance 过账引擎异常（配置错误/基础设施故障，非正常路径——`ERR_PROJECT_DEBIT_SUBJECT_NOT_RESOLVED`/`ERR_PAYROLL_SUBJECT_NOT_CONFIGURED`/`IErpFinVoucherBiz` Bean 未注入）；(2) LOG.warn/error 提供运维可见性；(3) `cancel` + `reverse` 提供回退路径（取消工时红冲凭证——但 posted=false 时 cancel 跳过 reverse 直接 UNSUBMITTED，工时可重新 submit→approve 触发重试过账）；(4) 业财不一致可经期末试算平衡人工发现。**维持同型 P1 治理缺陷**（详 §4 P1-MA2-068）。

#### reverse 红冲闭环对称性

✅ **对称**——`ErpPrjTimesheetBizModel.cancel:135-150` 守卫 posted==true → `postingDispatcher.reverse:69-71`（`IErpFinVoucherBiz.reverse` Facade 凭证红冲，billHeadCode=timesheet.code，businessType=PROJECT_COST_COLLECTION）+ 清 posted 三件套（posted=false + postedAt=null + postedBy=null）。与 `approve` 设 posted 三件套严格对称。工时 status 由 APPROVED 迁回 UNSUBMITTED（owner doc state-machine.md §3「终态不可恢复」——**但工时 APPROVED 在 owner doc 中不是终态**，是「已审批待过账」中间态，cancel 迁回 UNSUBMITTED 允许重新 submit→approve，合规）。

---

### 2.5 维度「可达性」

**裁决：FAIL（CRUD 空壳 dict 死状态 P1-MA2-069）」

#### 项目可达性

✅ 从 DRAFT 可达 OPEN→ON_HOLD/COMPLETED/CANCELLED；ON_HOLD 可回 OPEN。无不可达状态，无死锁（ON_HOLD↔OPEN 合法往复，退出条件 COMPLETED/CANCELLED）。owner doc §5 与代码 1:1 对应。

#### 任务可达性

✅ 从 TODO 可达 IN_PROGRESS→DONE/BLOCKED→IN_PROGRESS（IN_PROGRESS↔BLOCKED 合法往复，退出条件 DONE）。无不可达状态，无死锁。owner doc task-dag.md §4.2 与代码 1:1 对应。

#### 工时可达性

✅ 从 UNSUBMITTED 可达 SUBMITTED→APPROVED；SUBMITTED→UNSUBMITTED（reject）；APPROVED→UNSUBMITTED（cancel 红冲）。无不可达状态。

#### 项目结算可达性

✅ 从 DRAFT+UNSUBMITTED 可达 SUBMITTED→APPROVED/REJECTED + CANCELLED；APPROVED→reverseSettlement 红冲回退。无不可达状态。

#### Milestone/Billing 可达性（**新 P1-MA2-069**）

❌ **dict 死状态 + CRUD 桩**：

| 实体 | dict 死状态 | BizModel 行数 | setStatus writer |
|------|-------------|---------------|------------------|
| ErpPrjMilestone（task-status 4 态） | **TODO/IN_PROGRESS/DONE/BLOCKED 全 4 态**（除 codegen 默认值外无 writer） | 18 行 CRUD 桩 | **零 writer**（grep 全 src/main `MILESTONE.*setStatus\|milestone.setStatus` 零匹配） |
| ErpPrjBilling（project-status 5 态 docStatus） | **DRAFT/OPEN/ON_HOLD/COMPLETED/CANCELLED 全 5 态** | 18 行 CRUD 桩 | **零 writer**（grep 全 src/main `BILLING.*setStatus\|billing.setDocStatus` 零匹配） |
| ErpPrjCostCollection（project-status 5 态 docStatus） | **DRAFT/CANCELLED**（OPEN/ON_HOLD/COMPLETED 语义不适用 + APPROVED 不在字典——代码写 APPROVED 是 dict-value drift） | 43 行 CRUD + refreshExpenseCost | 经 `ProjectCostAggregator.aggregateFromTimesheet:79 + ExpenseCostAggregator.refreshExpenseCost:109` 写 `DOC_STATUS_APPROVED`（**不在 project-status 字典内**） |

按 finance A2.5a P1-MA2-031（DRAFT→CANCELLED 不可达）+ mfg A2.6a P1-MA2-035（作业卡 TRANSFERRED 死状态）+ mfg A2.6b P1-MA2-036（MRP CANCELLED + 预测 CONSUMED 死状态）+ hr A2.7a P1-MA2-040/041/042 + hr A2.7b P1-MA2-043/045 + inv A2.11 P1-MA2-063 + qa A2.12 P1-MA2-065 同型裁决（dict 死状态 + BizModel CRUD 桩 + 字典语义复用偏移）。合并登记 **P1-MA2-069**（详 §4）。**不破坏主路径**——项目 5 态 + 任务 4 态 + 工时审批轴 + 项目结算三轴完整覆盖生命周期；CRUD 空壳实体的状态字段不参与主路径迁移判定。

#### PnL calcStatus 可达性

✅ dict `erp-prj/pnl-calc-status`（PENDING/CALCULATED 2 态）：PENDING 经 codegen 默认值承载（实体创建）+ CALCULATED 经 `ProjectPnlCalculator.calculate:122 setCalcStatus(CALCULATED)`——**2 态全可达**。

---

### 2.6 维度「角色和权限」

**裁决：PASS（owner doc §6 角色绑定齐全，运行时经 @BizMutation 入口权限）」

✅ **每个迁移绑定执行角色**（owner doc `state-machine.md §6` 表）：

- 项目经理/管理员：立项（DRAFT→OPEN）+ 完成（OPEN→COMPLETED）+ 取消（→CANCELLED）
- 项目经理：暂停/恢复（OPEN↔ON_HOLD）
- 任务负责人/项目经理：startTask/completeTask/blockTask/unblockTask

✅ **危险操作控制**：

- **项目完成**（`closeProject` OPEN→COMPLETED 终态）：owner doc §6「项目经理/管理员（因影响项目报表与成本结转）」+ closeProject 末尾刷新实际成本保证关账数据完整。
- **项目取消**（`cancelProject` →CANCELLED 终态保留归集成本）：owner doc §6「项目经理/管理员」。

⚠️ **状态迁移方法无显式角色校验**——`startProject/holdProject/resumeProject/closeProject/cancelProject/startTask/completeTask/blockTask/unblockTask` 等均不校验调用者角色，依赖 @BizMutation 入口权限。owner doc §6 暗示「项目经理/管理员/任务负责人」角色。与 finance/mfg/pur/sal/qa 同型（业务规则文档化 + 权限由平台层统一）。**不登记为新 finding**。

✅ **角色名与状态名同源业务词汇**（项目经理/管理员/任务负责人，见 `roles-and-permissions.md`）。

---

### 2.7 维度「外部依赖」

**裁决：PASS（跨域写经 I*Biz Facade + 跨域只读维持 P1-MA1-022 todo MR1）」

✅ **采购/销售/费用单据引用项目**（owner doc §7）：业务单据经 `IErpPrjProjectBiz.requireReferenceable` Facade 校验项目 OPEN 状态——`requireReferenceable:46-50` 仅允许 OPEN（拒绝 ON_HOLD/COMPLETED/CANCELLED/DRAFT）。**项目关闭后拒绝引用**落实（COMPLETED/CANCELLED 全拒绝）。**跨域写经 I\*Biz Facade 合规**（grep 全 `module-projects/erp-prj-service/src/main` `daoFor(ErpFin` 仅 `TimesheetPostingDispatcher:149 daoFor(ErpMdSubject)` 跨域只读 + `daoProvider.daoFor(ErpFinExpenseClaim/ErpFinExpenseClaimLine)` 跨域只读，**无 `daoFor(ErpFin*).saveEntity/updateEntity` 跨域写**）。

✅ **工时触发成本凭证**（owner doc §7）：`ErpPrjTimesheetBizModel.approve:102` → `TimesheetPostingDispatcher.tryPost:51-64` → `ProjectPostingExecutor.postEvent:26-32` → `IErpFinVoucherBiz.post` Facade（@Transactional(REQUIRES_NEW) 跨域失败隔离）。**跨域写经 I\*Biz Facade 合规**（`ProjectPostingExecutor.java:23-24` 注入 `IErpFinVoucherBiz`，对齐 `processor-extension-pattern.md` 硬规则 2）。

✅ **费用报销归集**（owner doc §3.2 finance 纯读）：`ExpenseCostAggregator.findApprovedClaims:140-158` 经 `IErpFinExpenseClaimBiz.findList` Facade 只读查询已审核报销单（projects→finance R 读）+ `findLinesForProject:164-169 daoFor(ErpFinExpenseClaimLine)` 跨域只读。**projects 自写 `erp_prj_cost_collection`**（归集表由 projects 域写入，非 finance 回写）。**幂等**：按 `sourceBillType=EXPENSE + sourceBillCode=报销单号` 去重（`existsLine:171-177`）。

⚠️ **跨域只读 daoFor**（P1-MA1-022 持续）——`TimesheetPostingDispatcher.resolveSubjectCode:145-155 daoFor(ErpMdSubject)`（科目解析只读）+ `ExpenseCostAggregator.findLinesForProject:165 daoFor(ErpFinExpenseClaimLine)`（报销行只读——注释 :161-163 显式声明「IErpFinExpenseClaimLineBiz 仅为 CRUD 壳，无业务逻辑，daoProvider 等效且避免引入额外 IBiz 依赖」）+ `ErpPrjReportBizModel` facade read-only（plan baseline 已声明）。**状态机角度复核无升级**：跨域只读是科目解析/报销行扫描的副作用，不破坏状态机迁移（异常路径经 @BizMutation 事务回滚覆盖；approve→tryPost→resolveSubjectCode 跨域读失败抛 NopException → tryPost 吞异常返回 false → 工时 posted=false 悬挂——P1-MA2-068 同型交接；approve→aggregateFromTimesheet 不跨域读 finance；closeProject→refreshExpenseCost→findApprovedClaims/findLinesForProject 跨域读失败抛 NopException → @BizMutation 事务回滚 → closeProject 不进 COMPLETED）。维持 P1-MA1-022 todo MR1。

✅ **DAG 无环**——projects 反向依赖 finance（凭证过账经 Facade）+ master-data（科目只读）+ finance 费用报销（只读查询），不反向依赖 purchase/sales（purchase/sales 引用 projectId 是反向——业务域→projects，详 A2.1 P2P/A2.2 O2C done 已确认）。

---

### 2.8 维度「TODO / 任务策略」

**裁决：PASS（owner doc §8 避免沉没设计已落实）」

✅ **DRAFT 产生 assigned TODO**（项目经理待立项）——owner doc §8 表 + DRAFT 项目查询入口。

✅ **OPEN 产生 monitor TODO**（进度监控）——owner doc §8 表 + `ErpPrjDashboardBizModel` + `ErpPrjReportBizModel` 项目进度/成本/利润报表。

✅ **ON_HOLD 产生 assigned TODO**（项目经理待决策恢复/取消）——owner doc §8 表。

✅ **COMPLETED/CANCELLED 不产生 TODO**（终态）——owner doc §8 表。

✅ **避免项目静默下沉**（owner doc §8）——项目状态机 5 态全迁移守卫齐全，DRAFT/OPEN/ON_HOLD 三非终态均有明确 TODO 类型。

✅ **任务 IN_PROGRESS 产生 TODO**（执行）——owner doc task-dag.md + `validateTaskAcceptsTimesheet` 允许工时录入。

⚠️ **任务 BLOCKED 产生 assigned TODO**（解除阻塞决策）——`blockTask:141-144 blockReason` 必填强制记录阻塞原因，但**无自动通知/告警机制**（无 `IErpSysNotificationBiz` 派发）。owner doc task-dag.md §8 Non-Goals「任务延期/超期自动告警归通知 successor（0642-1 范式）」已声明 Deferred。**不登记为新 finding**（owner doc Deferred 已声明）。

⚠️ **Milestone/Billing/PnL 等 CRUD 空壳 TODO 策略未定义**——owner doc 无独立章节。CRUD 桩经 CrudBizModel 默认机制承载，不产生沉没（与 inv 拣货单 + qa QualityGoal/RiskRegister 同型 Deferred）。

---

### 2.9 维度「场景演练」（最重要）

**裁决：FAIL（10 场景覆盖；场景 E 完成时未结束任务 + 场景 I 工时凭证过账失败悬挂 两场景暴露 finding）」

#### 场景 A：研发项目 happy path

1. 项目经理创建研发项目（DRAFT）→ `startProject`（DRAFT→OPEN）→ 开放成本归集
2. 成员提交工时 → `submit`（UNSUBMITTED→SUBMITTED）→ `approve`（SUBMITTED→APPROVED）→ 触发项目成本凭证（借项目成本/贷应付薪酬）+ `costAggregator.aggregateFromTimesheet` 增量回写 actualCost
3. 采购项目相关物料 → 采购单经 `IErpPrjProjectBiz.requireReferenceable` 校验项目 OPEN → 标注 projectId → 费用归集
4. 项目交付完成 → `closeProject`（OPEN→COMPLETED）→ 出项目成本/利润报表（`ErpPrjReportBizModel`）

证据：`startProject:84-87` + `ErpPrjTimesheetBizModel.approve:92-118` + `requireReferenceable:43-52` + `closeProject:62-80`。✅ 状态机迁移路径完整（但 closeProject 缺任务结束校验——场景 E 暴露）。

#### 场景 B：项目暂停与恢复（OPEN→ON_HOLD→费用归集暂停→恢复）

1. 项目因外部原因暂停 → `holdProject`（OPEN→ON_HOLD）
2. 期间费用归集暂停——`requireReferenceable` 拒绝非 OPEN → 新采购/销售单据无法引用 + `validateProjectReferenceable` 拒绝新工时
3. 恢复 → `resumeProject`（ON_HOLD→OPEN）→ 继续归集

证据：`holdProject:91-94` + `requireReferenceable:46-50` + `validateProjectReferenceable:154-167` + `resumeProject:98-101`。✅ owner doc §2「暂停费用归集（可配置）」落实（硬拒绝）。

#### 场景 C：项目取消（已归集成本保留）

1. 项目取消 → `cancelProject`（OPEN→CANCELLED 或 DRAFT→CANCELLED 或 ON_HOLD→CANCELLED——代码接受非终态源态）
2. 已归集成本保留——`cancelProject:114-116` 仅 setStatus，不删除归集行/工时单/成本归集头
3. 可查询已取消项目的成本报表（用于经验沉淀）——`ErpPrjReportBizModel` 经 docStatus 过滤

证据：`cancelProject:105-117` + ORM `useLogicalDelete=true`。✅ owner doc §3「已取消项目保留已归集成本，不可删除」落实。

#### 场景 D：任务依赖成环拒绝（DAG 校验）

1. 任务 A 依赖任务 B（A.dependsOnId=B）+ 任务 B 依赖任务 A（B.dependsOnId=A）
2. 保存任务 A 时 `defaultPrepareSave` 触发 `validateDependency` → `TaskDependencyValidator.detectCycle`
3. 自环检查：taskId=A, dependsOnId=B → 不等，跳过自环
4. 上行链：visited={A}, cursor=B, depth=1, B 不在 visited, visited={A,B}, predecessor=B 的 dependsOnId=A, cursor=A, depth=2, A 在 visited → 抛 `ERR_TASK_DEPENDENCY_CYCLE`（chain=A→B→A）

证据：`TaskDependencyValidator.detectCycle:44-86` + `ErpPrjTaskBizModel.validateDependency:71-98`。✅ owner doc task-dag.md §2.1 算法落实。

#### 场景 E：完成时仍有未结束任务（**P1-MA2-067**）

1. 项目 OPEN，有 3 个任务：T1=DONE，T2=IN_PROGRESS，T3=TODO
2. 项目经理调 `closeProject(projectId)`
3. **预期**（owner doc §审查提示「项目完成时未结束任务的处理是否明确」+ §迁移完整性 L35「任务已结束（或确认剩余不再执行）」）：抛错提示「先关闭任务 T2/T3 或确认剩余取消」
4. **实际**：`closeProject:62-80` 仅 refreshActualCost + refreshExpenseCost + setStatus(COMPLETED) + updateEntity——**项目直接进 COMPLETED 终态，T2/T3 任务悬挂（IN_PROGRESS/TODO 状态保留但项目已终态）**

❌ **P1-MA2-067**（详 §4）。owner doc §审查提示 + §迁移完整性 显式前置未实现。

#### 场景 F：暂停后费用流入（配置控制）

1. 项目 ON_HOLD
2. 成员尝试提交工时 → `submit:74 validateProjectReferenceable` → 项目 status != OPEN → 抛 `ERR_TIMESHEET_PROJECT_NOT_OPEN`
3. 采购员尝试创建采购单标注 projectId → `requireReferenceable` → 项目 status != OPEN → 抛 `ERR_PROJECT_NOT_REFERENCEABLE`

证据：`ErpPrjTimesheetBizModel.validateProjectReferenceable:154-167` + `requireReferenceable:43-52`。✅ owner doc §4「配置控制：暂停项目拒绝新费用归集（或允许但标记）」落实（硬拒绝）。

#### 场景 G：预算超支（警告或拦截）

1. 项目预算 100,000，已归集成本 95,000
2. 成员提交工时 costAmount=10,000（超额 5,000）
3. `submit:85 runBudgetCheckHook` → `BudgetChecker.check`
4. **WARNING 模式**（默认）：LOG.warn 放行，工时进 SUBMITTED
5. **STRICT 模式**（config 切换）：抛错拒绝 submit

证据：`ErpPrjTimesheetBizModel.runBudgetCheckHook:192-194` + `BudgetChecker` + `ErpPrjConfigs.budgetControlMode`。✅ owner doc §4「预算超支警告或拦截（按配置），不阻止状态迁移」落实（WARNING/STRICT 双模式 config-gated）。

#### 场景 H：工时成本凭证过账（TimesheetPostingDispatcher 跨域）

1. 工时 SUBMITTED → `approve`（SUBMITTED→APPROVED）
2. `approve:102 postingDispatcher.tryPost(timesheet)` → `TimesheetPostingDispatcher.buildEvent:73-121` 构造 PostingEvent(PROJECT_COST_COLLECTION, billHeadCode=timesheet.code, debit=projectType.defaultSubjectId, credit=erp-prj.default-payroll-subject-id)
3. `ProjectPostingExecutor.postEvent:26-32` → `IErpFinVoucherBiz.post(event, context)` Facade → 跨域失败隔离（REQUIRES_NEW）
4. 成功 → tryPost 返回 true → `approve:108-110` 设 posted=true + postedAt + postedBy + `costAggregator.aggregateFromTimesheet` 增量归集

证据：`ErpPrjTimesheetBizModel.approve:92-118` + `TimesheetPostingDispatcher.tryPost:51-64 + buildEvent:73-121` + `ProjectPostingExecutor.postEvent:26-32`。✅ 跨域写经 Facade 合规。

#### 场景 I：工时成本凭证过账失败悬挂（**P1-MA2-068** tryPost 容错）

1. 工时 SUBMITTED → `approve:102 postingDispatcher.tryPost(timesheet)` → finance 过账引擎故障（如 `ERR_PROJECT_DEBIT_SUBJECT_NOT_RESOLVED` 项目类型未配 defaultSubjectId）
2. `tryPost:53-63` catch (NopException) → LOG.warn "工时过账失败，工时单 X 保持 APPROVED、posted=false" → 返回 false
3. `approve:104 timesheet.setStatus(APPROVED)` —— **工时进 APPROVED 终态**
4. `approve:107 if (posted)` posted=false → **跳过设 posted 三件套**
5. `approve:116 costAggregator.aggregateFromTimesheet(timesheet)` —— **归集行仍生成**（基于 costAmount，与凭证独立）
6. **结果**：(a) 工时 APPROVED；(b) 无 finance 凭证创建；(c) posted 永远 false；(d) 异常被 projects dispatcher 吞掉不进入 finance 过账异常工作台；(e) 期末结账前置检查不覆盖此悬挂；(f) 归集行已写入 + actualCost 已增量回写——**GL 缺 PROJECT_COST_COLLECTION 入账分录，projects 已计 actualCost，业财不一致**

⚠️ **P1-MA2-068**（详 §4）。同 finance P1-MA2-032 + hr P1-MA2-048 + assets P1-MA2-060 + qa A2.12 同型根因。

#### 场景 J：项目关闭后拒绝引用

1. 项目 COMPLETED（或 CANCELLED）
2. 采购员尝试创建采购单标注 projectId → `requireReferenceable:46-50` → 项目 status != OPEN → 抛 `ERR_PROJECT_NOT_REFERENCEABLE`
3. 成员尝试提交工时 → `validateProjectReferenceable:162-166` → 项目 status != OPEN → 抛 `ERR_TIMESHEET_PROJECT_NOT_OPEN`

证据：`requireReferenceable:43-52` + `ErpPrjTimesheetBizModel.validateProjectReferenceable:154-167`。✅ owner doc §7「项目关闭后拒绝引用」落实。

---

### 2.10 维度「与设计文档一致性」

**裁决：FAIL（2 项 P1 owner doc 契约漂移 + 1 项 P2 owner doc 章节缺失）」

| owner doc 章节 | 代码位置 | 一致性 | 裁决 |
|---------------|----------|--------|------|
| `state-machine.md §1 状态定义`（项目 5 态） | `ErpPrjConstants.PROJECT_STATUS_*` + `erp-prj/project-status` dict | ✅ | ✓ |
| `state-machine.md §2 迁移完整性` DRAFT→OPEN 前置「项目信息完整、起止日期有效、预算已定」 | `startProject:84-87` 仅 status==DRAFT 检查（无字段校验） | ❌ 契约漂移 | **P1-MA2-070** |
| `state-machine.md §2 迁移完整性` OPEN→COMPLETED 前置「任务已结束（或确认剩余不再执行）、成本已归集」 | `closeProject:62-80` 仅 refreshActualCost + setStatus（无任务结束校验） | ❌ 契约漂移 | **P1-MA2-067** |
| `state-machine.md §2 迁移完整性` OPEN→CANCELLED 单源 | `cancelProject:105-117` 接受 DRAFT/OPEN/ON_HOLD 多源 | ⚠️ 契约扩展 | **P1-MA2-070 part**（功能正确但 owner doc 未声明） |
| `state-machine.md §3 终态与恢复`「终态不可直接恢复 + 已取消保留成本」 | `transition:119-130` 守卫 + `cancelProject:108-113` 拒绝终态源 + ORM useLogicalDelete | ✅ | ✓ |
| `state-machine.md §4 异常路径`「完成时仍有未结束任务→提示先关闭任务或确认剩余取消」 | `closeProject` 不查任务状态 | ❌ 未落地 | **P1-MA2-067** |
| `state-machine.md §4 异常路径`「暂停后仍有费用流入→配置控制」 | requireReferenceable 硬拒绝非 OPEN + validateProjectReferenceable 硬拒绝非 OPEN | ✅（硬拒绝比可配置更严格） | ✓ |
| `state-machine.md §4 异常路径`「预算超支→警告或拦截（按配置），不阻止状态迁移」 | `BudgetChecker.check` config-gated WARNING/STRICT | ✅ | ✓ |
| `state-machine.md §4 异常路径`「项目删除→草稿可删除；进行中及以后只能取消/完成」 | ORM useLogicalDelete 统一逻辑删除（所有状态可逻辑删除） | ⚠️ 偏离已登记 | owner doc Deferred（useLogicalDelete 统一承载） |
| `state-machine.md §6 角色与权限` | @BizMutation 入口权限 | ✅ | ✓ |
| `state-machine.md §7 外部依赖`「项目关闭后拒绝引用」 | `requireReferenceable:46-50` 仅允许 OPEN | ✅ | ✓ |
| `state-machine.md §7 外部依赖`「工时触发成本凭证经 IErpFinAcctDocProvider」 | `TimesheetPostingDispatcher` → `IErpFinVoucherBiz.post` Facade（实际经 IErpFinVoucherBiz 而非 IErpFinAcctDocProvider） | ⚠️ 实现层级调整 | owner doc 文字「IErpFinAcctDocProvider」与实际「IErpFinVoucherBiz」差异——两者都是 finance Facade，IErpFinVoucherBiz 是凭证聚合根 Facade（更合规），归 **P2-MA2-065** |
| `state-machine.md §8 TODO 任务策略` | DRAFT/OPEN/ON_HOLD TODO 类型齐全 | ✅ | ✓ |
| `state-machine.md §审查提示`「项目完成时未结束任务的处理是否明确」 | `closeProject` 不处理 | ❌ 未落地 | **P1-MA2-067** |
| `state-machine.md §审查提示`「任务依赖成环是否校验（DAG）」 | `TaskDependencyValidator.detectCycle` 完整 | ✅ | ✓ |
| `state-machine.md §审查提示`「暂停项目的费用归集控制是否配置化」 | 硬拒绝（非配置化，更严格） | ⚠️ 偏离已登记 | owner doc Deferred |
| `state-machine.md §审查提示`「工时成本凭证的触发是否覆盖（与 finance 的业财打通）」 | `TimesheetPostingDispatcher.tryPost` + `ProjectPostingExecutor.postEvent` Facade | ✅ | ✓（但 tryPost 容错致悬挂——P1-MA2-068） |
| `task-dag.md §2 成环检测算法` | `TaskDependencyValidator.detectCycle` 上行链+HashSet+maxDepth 1:1 | ✅ | ✓ |
| `task-dag.md §3 跨项目依赖校验` | `validateDependency:88-94` | ✅ | ✓ |
| `task-dag.md §4 任务状态机` | startTask/completeTask/blockTask/unblockTask 全守卫 + config-gated STRICT/WARN | ✅ | ✓ |
| `task-dag.md §6 配置点` | `ErpPrjConfigs.taskDependencyMaxDepth + taskStrictPredecessorCheck` | ✅ | ✓ |
| `task-dag.md §7 错误码` | 7 ErrorCode 全落地（ERR_TASK_SELF_DEPENDENCY/CYCLE/DEPTH_EXCEEDED/CROSS_PROJECT/PREDECESSOR_NOT_DONE/ILLEGAL_STATUS_TRANSITION/BLOCK_REASON_REQUIRED） | ✅ | ✓ |
| `cost-collection.md §4.2 工时归集` | `ProjectCostAggregator.aggregateFromTimesheet` 同事务增量归集 | ✅ | ✓ |
| `cost-collection.md §七 项目状态引用校验` | `requireReferenceable` + `validateProjectReferenceable` | ✅ | ✓ |
| `profitability.md §关键流程` | `ProjectPnlCalculator.calculate` + `ErpPrjProjectSettlementProcessor` 三轴状态机 | ✅ | ✓ |
| `processor-extension-pattern.md` Facade+Processor 两层 | `ErpPrjProjectSettlementProcessor` protected step 方法 + 4 per-mutation Processor | ✅ | ✓ |
| `posting-exemptions.md` 跨域写豁免登记 | projects 无新豁免（TimesheetPostingDispatcher 经 IErpFinVoucherBiz Facade + ExpenseCostAggregator 经 IErpFinExpenseClaimBiz Facade 只读） | ✅ | ✓ |

#### owner doc 章节缺失（**新 P2-MA2-065**）

⚠️ `state-machine.md` 仅含「适用对象一：项目」+「适用对象二：任务」两章节。**5 个其他状态承载实体（成本归集/开票/工时/项目结算/PnL）无独立章节**——散落在 `cost-collection.md`、`profitability.md`、各 plan 文件中。与 purchase P2-MA2-053 + sales P2-MA2-056 + mfg P2-MA2-045/047 + hr P2-MA2-047/052 + assets P2-MA2-059 + inv P2-MA2-062 + qa P2-MA2-063 同型（owner doc 缺独立章节）。登记 **P2-MA2-065**（详 §4）。

#### owner doc 文字 IErpFinAcctDocProvider vs 实现 IErpFinVoucherBiz（**新 P2-MA2-066**）

⚠️ `state-machine.md §7 外部依赖`文字「工时触发成本凭证 → 通过 `IErpFinAcctDocProvider` 注册工时成本 businessType」与实现 `TimesheetPostingDispatcher → ProjectPostingExecutor → IErpFinVoucherBiz.post` Facade 不一致——实现实际经 `IErpFinVoucherBiz`（凭证聚合根 Facade，更合规），而非 `IErpFinAcctDocProvider`（AcctDoc 是更底层 Provider，由 finance 凭证引擎内部调用）。两者都是 finance Facade 但层级不同。owner doc 文字误导审查者期望 IErpFinAcctDocProvider，实际是 IErpFinVoucherBiz（更合规——`processor-extension-pattern.md` 硬规则 2「跨域注入 IErpXxxBiz」）。与 qa A2.12 P2-MA2-064（§审查提示文字 vs §实现偏离补注未同步）同型。登记 **P2-MA2-066**（详 §4）。

---

## 3. MA1 finding 运行时影响复核（projects 状态机角度）

| Finding ID | 原登记 | 本审计复核（状态机角度） | 终态 |
|-----------|--------|------------------------|------|
| **P1-MA1-010** | todo MR1（projects `ErpPrjCostCollection.{exchangeRate, amountSource, amountFunctional}`/`ErpPrjBilling.{amountSource, amountFunctional}` 共 5 列 propId 缺失） | **状态机角度无升级**——多币种四件套 propId 缺失是 ORM 规范层缺陷（`app-erp-projects.orm.xml:491-492 ErpPrjCostCollection exchangeRate/amountSource/amountFunctional + :634-635 ErpPrjBilling amountSource/amountFunctional` 确认无 propId）。`exchangeRate`/`amountSource`/`amountFunctional` 不参与状态机迁移判定（`closeProject`/`startProject`/`cancelProject`/`startTask` 等状态机方法均不读多币种字段决定 status）；工时过账 `TimesheetPostingDispatcher.buildEvent:100 event.setExchangeRate(BigDecimal.ONE)` 硬编码 ONE（单币种路径，与 P1-MA2-002 + P1-MA2-009 多币种 E2E 未验证同型）；归集头经聚合器写入 `exchangeRate=ONE + amountSource=amountFunctional=totalAmount`（单币种）。**仅规范层缺陷** | **不升级**（维持 todo MR1） |
| **P1-MA1-022** | todo MR1（9 域合并含 prj `TimesheetPostingDispatcher daoFor(ErpMdSubject)` + `ErpPrjReportBizModel` facade read-only） | **状态机角度无升级**——跨域只读是科目解析/报销行扫描的副作用，不破坏状态机迁移（异常路径经 @BizMutation 事务回滚覆盖；approve→tryPost→resolveSubjectCode 跨域读失败抛 NopException → tryPost 吞异常返回 false → 工时 posted=false 悬挂——P1-MA2-068 同型交接；closeProject→refreshExpenseCost→findApprovedClaims/findLinesForProject 跨域读失败抛 NopException → @BizMutation 事务回滚 → closeProject 不进 COMPLETED）。维持 P1-MA1-022 todo MR1 | **不升级**（维持 todo MR1） |

## 4. 新登记 finding

### P0（0 项）

**零 P0**（5 个候选 P0 经证据证伪或降级）：

1. **「项目完成未强制任务已结束」候选 P0 降级 P1-MA2-067**：owner doc §迁移完整性 L35 显式声明前置「任务已结束（**或确认剩余不再执行**）、成本已归集」——「或确认剩余不再执行」是软门控（允许人工确认取消剩余任务后完成），代码完全缺校验是契约漂移但非数据破坏（仍需人工 closeProject 动作 + 终态保留归集成本可查询 + 已归集成本不丢失）。按 finance P1-MA2-031 + mfg P1-MA2-035/036 + hr P1-MA2-039~042 + inv P1-MA2-063 + qa P1-MA2-064/065/066 同型裁决 P1（owner doc 契约漂移不破坏主路径）。
2. **「任务依赖 DAG 成环校验缺失」候选 P0 证伪**：`TaskDependencyValidator.detectCycle:44-86` 完整实现上行链+HashSet+maxDepth 算法 + 自环优先检测 + 跨项目校验 + 保存/更新双钩子触发。**证伪**——DAG 成环校验完整落地。
3. **「ON_HOLD 费用归集未暂停」候选 P0 证伪**：`requireReferenceable:46-50` 仅允许 OPEN（拒绝 ON_HOLD 等非 OPEN）+ `validateProjectReferenceable:154-167` 同样仅允许 OPEN——**硬拒绝比 owner doc §2「可配置」更严格**。`ExpenseCostAggregator.refreshExpenseCost` 不检查项目状态但仅由 `closeProject` 触发（ON_HOLD 期间无调用方）。**证伪**——ON_HOLD 费用归集暂停经双路径落实。
4. **「项目关闭后仍可被引用」候选 P0 证伪**：`requireReferenceable:46-50` 仅允许 OPEN → COMPLETED/CANCELLED 全拒绝新引用。**证伪**。
5. **「工时成本凭证过账失败悬挂无告警闭环」候选 P0 降级 P1-MA2-068**：与 finance P1-MA2-032（IGNORED 悬挂）+ hr P1-MA2-048（salary 过账悬挂）+ assets P1-MA2-060（Capitalization/Disposal 过账悬挂）+ qa A2.12（MANUAL_POST NCR 过账悬挂）**同型根因**——posting dispatcher 容错设计 + DeferredPostingSweepJob 兜底归 finance 域通用机制。**不升 P0**：失败模式需 finance 引擎异常（非正常路径）+ LOG.warn/error 可见性 + cancel/reverse 回退路径 + 业财不一致经期末试算平衡人工发现。按同型裁决 P1。

### P1（4 项，目标 MR1）

#### P1-MA2-067 closeProject (OPEN→COMPLETED) 未强制任务已结束（owner doc §审查提示 + §迁移完整性 显式前置缺失）

- **位置**：`module-projects/erp-prj-service/src/main/java/app/erp/prj/service/entity/ErpPrjProjectBizModel.java:62-80`（closeProject 方法）
- **现象**：`closeProject` 实现 OPEN→COMPLETED 迁移，**仅刷新实际成本（refreshActualCost）+ 刷新费用归集（refreshExpenseCost config-gated）+ setStatus(COMPLETED) + updateEntity**——**完全缺失「任务已结束」前置校验**。owner doc `state-machine.md §迁移完整性 L35` 显式声明 OPEN→COMPLETED 前置「任务已结束（或确认剩余不再执行）、成本已归集」+ §审查提示「项目完成时未结束任务的处理是否明确」+ §4 异常路径「完成时仍有未结束任务→提示先关闭任务，或确认剩余任务取消」。
- **影响**：项目经理可绕过任务结束校验直接关闭项目——项目进 COMPLETED 终态后，遗留任务（TODO/IN_PROGRESS/BLOCKED）悬挂（status 字段保留但项目已终态，无后续迁移路径）。破坏 owner doc §迁移完整性 + §审查提示 + §4 异常路径核心契约。
- **裁决**：**P1 非 P0**——(1) owner doc §迁移完整性 L35 用词「任务已结束（**或确认剩余不再执行**）」是软门控（允许人工确认取消剩余任务后完成），代码完全缺校验是契约漂移但非数据破坏；(2) 仍需人工 closeProject 动作；(3) 终态 COMPLETED 保留归集成本可查询；(4) 已归集成本不丢失（refreshActualCost 已在 closeProject 末尾执行）；(5) 按同型 owner doc 契约漂移裁决范式 P1。属设计漏洞非数据破坏。
- **修复方式**：MR1 裁决——方案 A（推荐）`closeProject` 增「任务结束校验」前置：查询项目下所有非终态任务（status != DONE 且 status != null），抛 `ERR_PROJECT_HAS_UNFINISHED_TASKS`（参数 projectId/taskIds/taskStatuses）+ 提示「先 completeTask/cancel 剩余任务」；config-gated `erp-prj.strict-task-completion-check`（默认 true=STRICT 拦截，false=仅 WARN 放行，对齐 task-dag.md §4.3 范式）。方案 B owner doc §迁移完整性 L35 标注「任务已结束校验 Deferred——closeProject 不强制，由项目经理保证」+ 删除 owner doc §审查提示 + §4 异常路径相关语义。触及 xbiz 契约（closeProject 行为变更），修复须独立 plan-audit + 人工确认。

#### P1-MA2-068 TimesheetPostingDispatcher tryPost 吞异常悬挂致 posted=false 无告警闭环（同型悬挂）

- **位置**：`module-projects/erp-prj-service/src/main/java/app/erp/prj/service/posting/TimesheetPostingDispatcher.java:51-64`（tryPost 方法）+ `module-projects/erp-prj-service/src/main/java/app/erp/prj/service/entity/ErpPrjTimesheetBizModel.java:102-117`（approve 方法不检查返回值）
- **现象**：`tryPost:51-64` try/catch 吞所有异常返回 boolean（`NopException` LOG.warn / 其他 LOG.error），**不向上传播**。`ErpPrjTimesheetBizModel.approve:102-117` 调用 `postingDispatcher.tryPost(timesheet):102` 后**无条件** `timesheet.setStatus(APPROVED):104`——**不检查返回值**。若 finance 过账引擎异常：(a) 工时进入 APPROVED 终态；(b) 无 finance 凭证创建；(c) posted 永远 false；(d) 异常被 projects dispatcher 吞掉不进入 finance 过账异常工作台；(e) 期末结账前置检查不覆盖此悬挂（全 `module-finance/erp-fin-service/.../period/` grep `ErpPrjTimesheet\|timesheet.*posted\|TIMESHEET` 零匹配）；(f) **`approve:116 costAggregator.aggregateFromTimesheet(timesheet)` 仍执行**——**归集行已写入 + actualCost 已增量回写，GL 缺 PROJECT_COST_COLLECTION 入账分录，业财不一致**。
- **影响**：与 finance P1-MA2-032（IGNORED 悬挂）+ hr P1-MA2-048（salary 过账悬挂）+ assets P1-MA2-060（Capitalization/Disposal 过账悬挂）+ qa A2.12（MANUAL_POST NCR 过账悬挂）**同型根因**。
- **裁决**：**P1 非 P0**——(1) 失败模式需 finance 过账引擎异常（如 `ERR_PROJECT_DEBIT_SUBJECT_NOT_RESOLVED` 项目类型未配 defaultSubjectId / `ERR_PAYROLL_SUBJECT_NOT_CONFIGURED` 配置缺失 / `IErpFinVoucherBiz` Bean 未注入抛 NopException——非正常路径）；(2) LOG.warn/error 提供运维可见性；(3) `cancel` + `reverse` 提供回退路径（取消工时红冲凭证——但 posted=false 时 cancel 跳过 reverse 直接 UNSUBMITTED，工时可重新 submit→approve 触发重试过账）；(4) 业财不一致可经期末试算平衡人工发现（虽无自动门控）；(5) 与同型范式按既定裁决 P1；(6) 不破坏 approve 主路径。按同型裁决。
- **修复方式**：MR1 裁决（与 finance P1-MA2-032 + hr P1-MA2-048 + assets P1-MA2-060 + qa A2.12 一并整体裁决）——方案 A（推荐）`approve` 检查 tryPost 返回值 + 失败时设 `posted=false` + 派发 `IErpSysNotificationBiz` 告警 + 不进 APPROVED 终态（保持 SUBMITTED）+ 期末结账前置检查扩展至 timesheet APPROVED-with-posted=false；方案 B owner doc `cost-collection.md §业财过账` 标注「过账失败吞异常为容错设计，业财不一致经期末试算平衡人工发现」+ posted 字段语义化（与 hr P1-MA2-047 + assets P1-MA2-060 方案 B 一并裁决）。触及会计保护区域，修复须独立 plan-audit + 人工确认。

#### P1-MA2-069 Milestone/Billing/CostCollection doc-status 字典语义复用偏移 + CRUD 桩死状态（合并裁决）

- **位置**：`ErpPrjMilestoneBizModel.java`（18 行 CRUD 桩）/ `ErpPrjBillingBizModel.java`（18 行 CRUD 桩）/ `ErpPrjCostCollectionBizModel.java`（43 行 CRUD + refreshExpenseCost）/ `app-erp-projects.orm.xml:482 ErpPrjCostCollection.docStatus ext:dict="erp-prj/project-status"` + `:626 ErpPrjBilling.docStatus ext:dict="erp-prj/project-status"` + `:586 ErpPrjMilestone.status ext:dict="erp-prj/task-status"`
- **现象**：3 处字典语义复用偏移 + CRUD 桩死状态合并登记：
  - **ErpPrjMilestone**（`erp-prj/task-status` 4 态 TODO/IN_PROGRESS/DONE/BLOCKED 复用）：BizModel 18 行 CRUD 桩零 setStatus writer——**全 4 态死状态**（除 codegen 默认值外）。grep 全 `module-projects/erp-prj-service/src/main` `milestone\.setStatus\|Milestone.*setStatus` 零匹配。
  - **ErpPrjBilling**（`erp-prj/project-status` 5 态 DRAFT/OPEN/ON_HOLD/COMPLETED/CANCELLED 复用 docStatus）：BizModel 18 行 CRUD 桩零 setStatus writer——**全 5 态死状态**。grep 全 `module-projects/erp-prj-service/src/main` `billing\.setDocStatus\|Billing.*setDocStatus` 零匹配。
  - **ErpPrjCostCollection**（`erp-prj/project-status` 5 态复用 docStatus）：BizModel 43 行（仅 refreshExpenseCost config-gated）。docStatus 经 `ProjectCostAggregator.aggregateFromTimesheet:79 + ExpenseCostAggregator.refreshExpenseCost:109` 写 `DOC_STATUS_APPROVED`（ErpPrjConstants.DOC_STATUS_APPROVED="APPROVED"）——**APPROVED 不在 erp-prj/project-status 字典内**（字典仅 DRAFT/OPEN/ON_HOLD/COMPLETED/CANCELLED），dict-value drift（与 finance P1-MA1-018 enum↔dict 漂移同型）。OPEN/ON_HOLD/COMPLETED 三态语义不适用于成本归集（成本归集是单据，非项目生命周期）。
- **影响**：字典语义复用偏移 + dict-value drift + CRUD 桩死状态。UI/查询层期望按 dict option 筛选时会失效或漂移（如 Billing 按 docStatus=OPEN 过滤会查到零行，因代码从不写 OPEN）。
- **裁决**：按 finance A2.5a P1-MA2-031（DRAFT→CANCELLED）+ mfg A2.6a P1-MA2-035（作业卡 TRANSFERRED）+ mfg A2.6b P1-MA2-036（MRP CANCELLED + 预测 CONSUMED）+ hr A2.7a P1-MA2-040/041/042 + hr A2.7b P1-MA2-043/045 + inv A2.11 P1-MA2-063（PickingOrder PICKING/PICKED）+ qa A2.12 P1-MA2-065 + finance P1-MA1-018（enum↔dict 漂移）同型裁决（dict 死状态 + BizModel CRUD 桩 + dict-value drift）。**不破坏主路径**——项目 5 态 + 任务 4 态 + 工时审批轴 + 项目结算三轴完整覆盖生命周期；CRUD 空壳实体的状态字段不参与主路径迁移判定；CostCollection 的 APPROVED drift 经聚合器单一入口写入，归集行为正确（仅 dict 筛选层失效）。
- **修复方式**：MR1 裁决——方案 A（推荐）三处分别处理：(1) Milestone 新增独立 `erp-prj/milestone-status` 字典（如 PLANNED/IN_PROGRESS/ACHIEVED/CANCELLED）+ ORM 改 `ext:dict` + 实现 `startMilestone/achieveMilestone/cancelMilestone` BizMutation（owner doc state-machine.md 新增「对象三：里程碑状态机」章节）；(2) Billing docStatus 改 `ext:dict="erp/doc-status"` 或新增 `erp-prj/billing-status` + 实现 `submitForApproval/approve/reject/cancel` BizMutation（与 finance expense claim 同型审批流）；(3) CostCollection docStatus 改 `ext:dict="erp/doc-status"`（含 APPROVED）+ owner doc cost-collection.md 标注 docStatus 语义。方案 B owner doc state-machine.md 标注「Milestone/Billing/CostCollection 状态机 Deferred——CRUD 桩承载 + CostCollection 经聚合器自动 APPROVED」+ Milestone `ext:dict` 改回 task-status 但 owner doc 注记「里程碑状态字段预留，本期不实现迁移」+ Billing/CostCollection docStatus 字典改 `erp/doc-status` 对齐 APPROVED 实际值。

#### P1-MA2-070 startProject 缺前置校验 + cancelProject 接受 DRAFT/ON_HOLD 超出 owner doc 单源声明（owner doc §迁移完整性 契约漂移）

- **位置**：`module-projects/erp-prj-service/src/main/java/app/erp/prj/service/entity/ErpPrjProjectBizModel.java:84-87`（startProject）+ `:105-117`（cancelProject）
- **现象**：
  - (a) `startProject:84-87` 实现 DRAFT→OPEN 迁移，**仅 `transition(projectId, DRAFT, OPEN, context)` 检查 status==DRAFT**——无任何字段校验。owner doc `state-machine.md §迁移完整性 L32` 显式声明前置「项目信息完整、起止日期有效、预算已定」，代码不校验（项目名/起止日期/预算字段为空的 DRAFT 项目可直接立项）。
  - (b) `cancelProject:105-117` 接受 `status != COMPLETED && status != CANCELLED` 源态（即 DRAFT/OPEN/ON_HOLD）——**超出** owner doc `state-machine.md §迁移完整性 L36` 显式声明的 OPEN 单源（DRAFT→CANCELLED + ON_HOLD→CANCELLED 路径代码可达但 owner doc 未声明）。
- **影响**：(a) DRAFT 项目信息不完整/起止日期无效/预算未定仍可立项 → 项目进 OPEN 后业务单据可引用（requireReferenceable 仅校验 OPEN），后续成本归集/工时录入可能出现数据不完整（如未配起止日期的项目 indefinite 归集）。(b) DRAFT→CANCELLED + ON_HOLD→CANCELLED 路径代码可达但 owner doc 未声明——契约漂移（功能正确：DRAFT 取消无归集影响 + ON_HOLD 取消符合「暂停项目最终决策为取消」业务场景，但 owner doc 未声明）。
- **裁决**：**P1 非 P0**——(1) DRAFT→OPEN 缺前置校验是契约漂移但非数据破坏（仍需人工 startProject 动作 + 后续业务单据引用经 requireReferenceable 校验 OPEN 状态 + 成本归集字段为空时归集行为不破坏主路径）；(2) cancelProject 接受多源是功能扩展（更宽松的取消路径），不破坏终态语义（CANCELLED 保留归集成本正确）；(3) 按 owner doc 契约漂移裁决范式 P1。属设计契约漂移非数据破坏。
- **修复方式**：MR1 裁决——方案 A（推荐）(a) `startProject` 增字段校验：项目名非空 + 起止日期有效（startDate <= endDate 且非空）+ 预算已定（budget 不为 null 或 config-gated 允许无预算）+ 抛 `ERR_PROJECT_START_PRECONDITION_FAILED`（参数 projectId/missingFields）；(b) owner doc §迁移完整性 L36 显式补充「DRAFT→CANCELLED + ON_HOLD→CANCELLED」两迁移路径（与代码对齐）。方案 B owner doc §迁移完整性 L32 标注「DRAFT→OPEN 前置校验 Deferred——由项目经理保证项目信息完整」+ §迁移完整性 L36 补充多源声明（与代码对齐）。

### P2 watch-only（2 项）

#### P2-MA2-065 state-machine.md 缺 5 状态承载实体独立章节

- **位置**：`docs/design/projects/state-machine.md`
- **现象**：state-machine.md 仅含「适用对象一：项目」+「适用对象二：任务」两章节。**5 个其他状态承载实体（成本归集/开票/工时/项目结算/PnL）无独立章节**——散落在 `cost-collection.md`、`profitability.md`、各 plan 文件中。
- **裁决**：与 purchase P2-MA2-053 + sales P2-MA2-056 + mfg P2-MA2-045/047 + hr P2-MA2-047/052 + assets P2-MA2-059 + inv P2-MA2-062 + qa P2-MA2-063 同型（owner doc 缺独立章节）。无运行时影响（每实体状态机经代码 + plan 文件证据可追溯），仅 owner doc 可读性缺陷。
- **修复方式**：watch-only，MR1 顺手——方案 A（推荐）state-machine.md 新增「对象三：成本归集单据状态机」+「对象四：项目开票状态机」+「对象五：工时审批状态机」+「对象六：项目结算三轴状态机」+「对象七：PnL 计算状态机」（本审计 §2.1-2.5 状态图可直接采用）；方案 B 交叉链接到各 owner doc。

#### P2-MA2-066 state-machine.md §7 IErpFinAcctDocProvider vs 实现 IErpFinVoucherBiz 文字漂移

- **位置**：`docs/design/projects/state-machine.md §7 外部依赖`
- **现象**：§7 文字「工时触发成本凭证 → 通过 `IErpFinAcctDocProvider` 注册工时成本 businessType」与实现 `TimesheetPostingDispatcher → ProjectPostingExecutor → IErpFinVoucherBiz.post` Facade 不一致——实现实际经 `IErpFinVoucherBiz`（凭证聚合根 Facade，更合规），而非 `IErpFinAcctDocProvider`（AcctDoc 是更底层 Provider，由 finance 凭证引擎内部调用）。
- **裁决**：owner doc 文字误导审查者期望 IErpFinAcctDocProvider，实际是 IErpFinVoucherBiz（更合规——`processor-extension-pattern.md` 硬规则 2「跨域注入 IErpXxxBiz」）。与 qa A2.12 P2-MA2-064（§审查提示文字 vs §实现偏离补注未同步）同型。无运行时影响。
- **修复方式**：watch-only，MR1 顺手——`state-machine.md §7 外部依赖` 更新为「工时触发成本凭证 → 经 `IErpFinVoucherBiz.post` Facade（凭证聚合根，对齐 processor-extension-pattern.md 硬规则 2）」。

## 5. 并发敏感点（交接 A2.17）

| 敏感点 | 位置 | 风险 | 交接状态 |
|--------|------|------|----------|
| ErpPrjProject 并发状态变更 | `ErpPrjProjectBizModel.startProject/holdProject/resumeProject/closeProject/cancelProject` 读-改-写 status 无显式锁 | 并发 closeProject + cancelProject 可能 silent lost-update（前者 OPEN→COMPLETED，后者 OPEN→CANCELLED） | 交接 A2.17（依赖 ErpPrjProject ORM `versionProp="version"` 透明乐观锁降级为 detectable conflict） |
| ErpPrjTask 并发 startTask/completeTask + 依赖保存 | `ErpPrjTaskBizModel.startTask/completeTask/blockTask/unblockTask` + `defaultPrepareSave/Update` 读-改-写 status + dependsOnId 无显式锁 | 并发 startTask 同任务可能 silent lost-update；并发保存依赖关系可能短暂成环（A→B + B→A 并发保存，依赖 optimistic lock detect） | 交接 A2.17（依赖 ErpPrjTask ORM versionProp 透明乐观锁 + DAG 校验在保存钩子内执行） |
| ErpPrjTimesheet 并发 approve + cancel | `ErpPrjTimesheetBizModel.approve/cancel` 读-改-写 status + posted 无显式锁 | 并发 approve（SUBMITTED→APPROVED + posted=true）+ cancel（posted=true→false 红冲）可能状态漂移 | 交接 A2.17（依赖 ErpPrjTimesheet ORM versionProp 透明乐观锁） |
| ErpPrjCostCollection 并发 aggregateFromTimesheet + refreshActualCost | `ProjectCostAggregator.aggregateFromTimesheet/refreshActualCost` + `ExpenseCostAggregator.refreshExpenseCost` 读-改-写 totalAmount + actualCost 无显式锁 | 并发工时 approve 触发并发 aggregateFromTimesheet 可能 totalAmount 重复累加（幂等键 sourceBillType+sourceBillCode 兜底——`existsLine:141-147` 已守卫，但 actualCost 增量回写无幂等） | 交接 A2.17（依赖 ErpPrjCostCollection/ErpPrjProject ORM versionProp 透明乐观锁 + existsLine 幂等键兜底；actualCost 增量回写并发漂移归 A2.17 系统性裁决） |
| ProjectPostingExecutor.postEvent 并发同 timesheet.code | `TimesheetPostingDispatcher.tryPost` + `approve` 无 timesheet.code 互斥 | 并发 approve 同工时不可能（ErpPrjTimesheet versionProp 守卫）；但 approve 失败重试（posted=false 重 submit→approve）可能重复过账（幂等键 `(billHeadCode=timesheet.code, businessType=PROJECT_COST_COLLECTION)` 经 `IErpFinVoucherBiz` 引擎反查兜底——详 A2.5a finance 凭证状态机审计） | 交接 A2.17（凭证引擎幂等键兜底 + timesheet.posted=true 守卫二次 approve 抛 ERR_TIMESHEET_ILLEGAL_STATUS_TRANSITION） |

## 6. 残留风险

1. **P1-MA2-067 closeProject 未强制任务已结束**：owner doc §迁移完整性 + §审查提示 显式前置缺失。归 MR1 裁决。
2. **P1-MA2-068 工时过账失败悬挂**：同 finance P1-MA2-032 + hr P1-MA2-048 + assets P1-MA2-060 + qa A2.12 同型，归 MR1 整体裁决。
3. **P1-MA2-069 Milestone/Billing/CostCollection 字典语义复用偏移 + CRUD 桩死状态**：CRUD 空壳实体状态字段不参与主路径。归 MR1 裁决。
4. **P1-MA2-070 startProject 缺前置校验 + cancelProject 多源**：owner doc 契约漂移。归 MR1 裁决。
5. **孤立字典 erp-prj/timesheet-status**：未绑定列 + 不影响状态机判定。watch-only，MR1 顺手清理。
6. **ON_HOLD 费用归集暂停「可配置」 Deferred**：owner doc §2 声明「可配置」，实现是硬拒绝（更严格）。owner doc Deferred 落实。
7. **并发敏感点 5 处交接 A2.17**：本审计不做系统性并发正确性裁决。
8. **多币种 P2P/O2E E2E 未验证**（P1-MA2-002 + P1-MA2-009）：状态机角度无影响（多币种四件套不参与状态机判定），但工时过账 buildEvent 硬编码 exchangeRate=ONE 是单币种路径，多币种项目工时凭证折算归 MR1。

## 7. 裁决

### 7.1 10 维度裁决汇总

| 维度 | 裁决 | 关键证据 |
|------|------|----------|
| 1. 状态定义 | ✅ PASS（含复用字典语义偏移注记 + Deferred CRUD 空壳清晰性注记） | 项目 5 态 + 任务 4 态 + 工时审批轴 + PnL 2 态清晰；ON_HOLD 语义清晰（等待恢复决策）；5 处复用 project-status/task-status 字典偏移（归 P1-MA2-069）；孤立字典 timesheet-status（watch-only） |
| 2. 转换完整性 | ❌ FAIL（2 项新 P1 + ON_HOLD Deferred 落实） | 项目生命周期迁移主路径 + 任务 4 态 + DAG 成环校验 + 工时审批轴 + 项目结算三轴齐全；**P1-MA2-067** closeProject 缺任务结束校验；**P1-MA2-070** startProject 缺前置校验 + cancelProject 多源；ON_HOLD 费用归集暂停 Deferred 落实（硬拒绝） |
| 3. 终端与恢复 | ✅ PASS | 项目 COMPLETED/CANCELLED 终态无出边 ✓ + 任务 DONE 终态 + 已取消保留归集成本 + useLogicalDelete 统一逻辑删除 |
| 4. 异常路径 | ❌ FAIL（1 项新 P1 + 同型悬挂交接） | ON_HOLD 费用归集 + 预算超支 + 项目删除 + DAG 成环/自环/跨项目/深度超限 + startTask 前置未完成 + 项目关闭后引用 全覆盖；**P1-MA2-067** 完成时未结束任务；**P1-MA2-068** 工时过账失败悬挂同型交接 |
| 5. 可达性 | ❌ FAIL（P1-MA2-069 CRUD 空壳死状态） | 项目 5 态 + 任务 4 态 + 工时审批轴 + 项目结算三轴 + PnL 2 态全可达 ✓；**Milestone/Billing/CostCollection CRUD 空壳 dict 死状态**（P1-MA2-069） |
| 6. 角色与权限 | ✅ PASS | owner doc §6 角色绑定齐全 + @BizMutation 入口权限 |
| 7. 外部依赖 | ✅ PASS | 跨域写经 I*Biz Facade（IErpFinVoucherBiz + IErpFinExpenseClaimBiz）+ 跨域只读维持 P1-MA1-022 todo MR1；DAG 无环 |
| 8. TODO 任务策略 | ✅ PASS | DRAFT/OPEN/ON_HOLD TODO 类型齐全 + 避免项目静默下沉；任务 BLOCKED 产生 assigned TODO + 无自动通知归 owner doc Deferred successor |
| 9. 场景演练 | ❌ FAIL（10 场景覆盖；场景 E + 场景 I 暴露 finding） | 场景 A-D + F-H + J 覆盖 ✓；**场景 E 完成时未结束任务**（P1-MA2-067）+ **场景 I 工时凭证过账失败悬挂**（P1-MA2-068） |
| 10. 与设计文档一致性 | ❌ FAIL（2 项 P1 owner doc 契约漂移 + 2 项新 P2） | owner doc 26 章节中 §迁移完整性/§审查提示/§4 被 P1-MA2-067/070 漂移；5 实体无独立章节（P2-MA2-065）+ §7 IErpFinAcctDocProvider vs 实现漂移（P2-MA2-066） |

### 7.2 状态机正确性维度 prj 列推进

| 维度（前） | prj 列（前） | prj 列（后） | 推进依据 |
|------------|-------------|-------------|----------|
| 状态机正确性 | ❓ | **⚠️P1(A2.13✅)** | 项目状态机核心契约（项目 5 态 + 任务 4 态 + 工时审批轴 + DAG 成环检测上行链+HashSet+maxDepth + 项目结算三轴 + PnL 2 态 + 工时成本凭证跨域过账经 IErpFinVoucherBiz Facade + ON_HOLD 费用归集暂停硬拒绝 + 已取消保留归集成本 + 跨域 Facade 全合规）经证据逐项确认；**零 P0**（5 个候选 P0 经证据证伪或降级）；**4 项新 P1**（P1-MA2-067 closeProject 缺任务结束校验 / P1-MA2-068 工时过账失败悬挂同型 / P1-MA2-069 Milestone/Billing/CostCollection 字典语义复用偏移 + CRUD 桩死状态 / P1-MA2-070 startProject 缺前置校验 + cancelProject 多源）；**2 项新 P2** watch-only（P2-MA2-065 owner doc 缺 5 实体独立章节 / P2-MA2-066 §7 IErpFinAcctDocProvider vs 实现漂移）；2 项已登记 MA1 finding（P1-MA1-010 propId / P1-MA1-022 跨域只读）运行时复核无升级；5 处并发敏感点交接 A2.17 含 @Version 透明乐观锁降级（6 个 prj 状态机实体均声明 versionProp） |

### 7.3 Verdict

**Verdict: pass（条件性 → pass after MR1）**——项目状态机核心契约（项目 5 态 + 任务 4 态 + 工时审批轴 + DAG 成环检测 + 项目结算三轴 + PnL + 跨域 Facade）经证据逐项确认，**零 P0**（5 个候选 P0 经证据证伪或降级为 P1）。**4 项新 P1 + 2 项新 P2** 已登记待 MR1；2 项已登记 MA1 finding 运行时复核无升级；5 处并发敏感点交接 A2.17。**MR1 修复 P1 后 Verdict 转 pass**（当前为 conditional pass——主路径完整，P1 为契约漂移/同型悬挂/字典治理缺陷非数据破坏）。

**审查范围**：module-projects 8 个状态承载实体 + 工时过账/成本归集助手 + 任务依赖 DAG 校验器 + 项目结算 Processor 链 + 4 个 owner doc + 2 个 architecture doc。

**可达性摘要**：项目 5 态 + 任务 4 态 + 工时审批轴 + 项目结算三轴 + PnL 2 态全可达；Milestone/Billing/CostCollection CRUD 空壳 dict 死状态（P1-MA2-069）。

**角色/权限摘要**：每个迁移绑定执行角色（owner doc §6）；@BizMutation 入口权限由平台层统一。

**外部依赖摘要**：跨域写经 I*Biz Facade（ProjectPostingExecutor→IErpFinVoucherBiz + ExpenseCostAggregator→IErpFinExpenseClaimBiz）；跨域只读维持 P1-MA1-022 todo MR1（TimesheetPostingDispatcher daoFor(ErpMdSubject) + ExpenseCostAggregator daoFor(ErpFinExpenseClaimLine) + ErpPrjReportBizModel facade read-only）。

**剩余风险**：详 §6（8 项，均归 MR1 / Deferred successor / A2.17）。

## 8. 引用

- 审计 plan：`docs/plans/2026-07-28-1020-2-audit-remediation-ma2-projects-state-machine.md`
- 范本（quality A2.12）：`docs/audits/2026-07-28-1020-arm-ma2-quality-state-machine.md`
- 范本（inventory A2.11）：`docs/audits/2026-07-28-0400-arm-ma2-inventory-state-machine.md`
- 范本（assets A2.10）：`docs/audits/2026-07-28-0400-arm-ma2-assets-state-machine.md`
- 上游 A2.5a finance 凭证状态机（reverse 红冲同型范式 + tryPost 吞异常悬挂同型）：`docs/audits/2026-07-27-2211-arm-ma2-finance-posting-voucher-state-machine.md`
- 上游 A2.6a manufacturing（TimesheetPostingDispatcher 跨域过账同型）：`docs/audits/2026-07-28-0109-arm-ma2-mfg-work-order-jobcard-state-machine.md`
- 上游 A2.7b hr 工资（salary 过账悬挂同型 P1-MA2-048）：`docs/audits/2026-07-28-0230-arm-ma2-hr-attendance-payroll-state-machine.md`
- 上游 A2.10 assets（Capitalization/Disposal 过账悬挂同型 P1-MA2-060）：`docs/audits/2026-07-28-0400-arm-ma2-assets-state-machine.md`
- 上游 A2.12 quality（MANUAL_POST NCR 过账悬挂同型）：`docs/audits/2026-07-28-1020-arm-ma2-quality-state-machine.md`
- 任务依赖 DAG plan（done）：`docs/plans/2026-07-07-0930-3-projects-task-dependency-dag-cycle-validation.md`
- 项目成本归集 plan（done）：`docs/plans/2026-07-03-1018-2-projects-cost-collection.md`
- 工时成本凭证 plan（done）：`docs/plans/2026-07-04-0831-2-hr-payroll-engine-income-tax.md`（TimesheetPostingDispatcher 跨域过账同型）
- owner docs：`docs/design/projects/{state-machine,task-dag,cost-collection,profitability}.md` + `docs/architecture/{processor-extension-pattern,posting-exemptions}.md`
- skill：`docs/skills/state-machine-business-review-prompt.md`
- 矩阵更新：`docs/audits/audit-remediation-scope-and-dimension-matrix.md §2.2`（状态机正确性 + prj 列推进至 ⚠️P1(A2.13✅)）
