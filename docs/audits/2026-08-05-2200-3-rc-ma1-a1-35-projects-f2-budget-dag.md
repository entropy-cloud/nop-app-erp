# A1.35 projects-F2 预算与 DAG 需求-实现符合性五级追踪审计报告

> 报告类型：MA1（RC）需求-实现符合性五级追踪审计
> 切片：A1.35 projects-F2 预算与 DAG（项目预算 STRICT 超支拦截 + 任务依赖 DAG 成环校验）
> 审计范围：UC-PRJ-04 / UC-PRJ-05（2 UC，逐 UC 一矩阵行，§3 完整枚举）
> 真相源层级（§4 Q1）：L1 = `docs/design/projects/use-cases.md`（UC-PRJ-04 `:65` / UC-PRJ-05 `:81`）；L2 = `docs/design/projects/cost-collection.md §三` + `state-machine.md §任务` + `task-dag.md §2/§4/§6/§7`（设计参考，冲突一律以 L1 为准）；L3 = 实仓代码；L4 = 测试；L5 = 复用 A2.13 + 本切片差异。
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议 + §4 三判据）
> Skill：`docs/skills/multi-dimensional-audit-prompt.md`
> 计划：`docs/plans/2026-08-05-2200-3-rc-ma1-a1-35-projects-f2-budget-dag.md`
> L1 锚点：`docs/audits/rc-requirement-baseline-inventory.md`（A1.35 UC 清单 = UC-PRJ-04/05，覆盖率 ✅ 一致，无基线分歧）
> 结论速览：⚠️(P1) + 接受（DAG 强实现）— **UC-PRJ-04 项目预算 STRICT 超支拦截[接受 on 工时时机[主路径] + STRICT/WARNING 双模式[完整实现 + 强测]；P1 on 采购审核 + 报销审核两预算检查时机缺失[报销独立缺口 + 采购路径依赖物料归集 successor P1-RC-049]；P2 on 余量公式缺"已承诺成本"项[主路径已归集成本拦截可用，watch-only]]** / **UC-PRJ-05 任务依赖 DAG 成环校验[接受 on 全部 4 验收标准——DAG 成环检测算法 + 跨项目校验 + 自环优先 + 前置未完成 config-gated STRICT/WARN 守卫完整实现 + 319 行强测 + A2.13 §2.5 PASS]**。**零 P0**。**新登记 2 finding**：`P1-RC-051`（UC-PRJ-04 预算检查采购/报销时机缺失，纯 BizModel 代码逻辑修复预授权）+ `P2-RC-049`（UC-PRJ-04 余量公式缺"已承诺成本"项，watch-only）。**复用 A2.13 任务状态机/DAG 已证实行为**（不重复验证）+ **复用 P1-RC-049 物料归集 successor 衔接**（采购路径预算检查依赖物料归集先落地）。

---

## 9. 与既有 MA2 报告差异增量声明（§6 段落 9，置顶便于去重）

本切片为 projects 域**第二批 RC 切片**（projects 域共 3 切片 A1.34/A1.35/A1.36，本切片覆盖 UC-PRJ-04/05 预算控制 + DAG；A1.34 立项+成本归集 done [P1-RC-048/049/050 + P2-RC-048]；A1.36 结算+看板由独立 plan 覆盖）。按 §去重协议，本报告**不复跑** MA2 状态机/业财链路审计，直接复用既有 MA2 报告已证实行为作为 L5 既有证据输入，只补"需求契约↔实际行为"差异。

### 复用 A2.13（projects 状态机审计，`docs/audits/2026-07-28-1020-arm-ma2-projects-state-machine.md`）

- **复用 A2.13 已证实行为**（与本切片直接相关）：
  - **任务 4 态状态机 + DAG 成环检测**（A2.13 §2.5）：`TaskDependencyValidator.detectCycle:44-86` 与 `task-dag.md §2.1` 算法 1:1 对应（自环优先 `taskId==dependsOnId → ERR_TASK_SELF_DEPENDENCY` / visited HashSet + chainOrder + cursor 上行链 / `depth++ > maxDepth → ERR_TASK_DEPENDENCY_DEPTH_EXCEEDED` / `cursor∈visited → ERR_TASK_DEPENDENCY_CYCLE`）；跨项目校验 `task.projectId == dependsOnTask.projectId` 否则 `ERR_TASK_DEPENDENCY_CROSS_PROJECT`；保存钩子双触发（`defaultPrepareSave` 插入 + `defaultPrepareUpdate` 修改）；前置未完成 config-gated `erp-prj.task-strict-predecessor-check`（默认 true）：STRICT 抛 `ERR_TASK_PREDECESSOR_NOT_DONE` / WARN LOG.warn 放行；任务状态机 startTask(TODO→IN_PROGRESS) / completeTask(IN_PROGRESS→DONE) / blockTask(IN_PROGRESS→BLOCKED, blockReason 必填) / unblockTask(BLOCKED→IN_PROGRESS)；非法迁移抛 `ERR_TASK_ILLEGAL_STATUS_TRANSITION`。
- **复用 A2.13 P1 finding**（任务状态机 + DAG 已 resolved，本切片 HEAD 复核确认无回退）：
  | Finding | 描述 | Resolution | 本切片 HEAD 复核 |
  |---------|------|-----------|------------------|
  | **P1-MA2-069** | Milestone/Billing/CostCollection doc-status dict-value drift + CRUD 桩死状态 | R1.21 done（方案 B Deferred） | `cost-collection.md §4.2:194` APPROVED drift documented；`state-machine.md §适用对象三` Deferred 段落存在。**确认 resolved（resolved-via-deferral）无回退**——属 doc-only Deferred 非 implementation；本切片**不重开**（§去重协议） |
  | **P1-MA2-070** | startProject 缺前置 + cancelProject 多源 | R1.21 done | 非本切片控制点（UC-PRJ-01/09），仅引用不重审 |
- **复用 A2.13 P2 watch-only**：P2-MA2-065（state-machine.md 缺独立章节）/ P2-MA2-066（IErpFinAcctDocProvider vs IErpFinVoucherBiz 文字漂移）——与本切片 UC-PRJ-04/05 无控制点重叠，不投影。
- **复用 A2.13 已登记 MA1 finding 状态机角度复核**（无升级）：P1-MA1-010 多币种 propId 缺失（与预算检查/DAG 正交，仅引用）/ P1-MA1-022 跨域只读 daoFor ErpMdSubject + ErpFinExpenseClaimLine（与预算检查正交，仅引用）。

### 本切片只补的需求视角差异（4 项）

1. **UC-PRJ-04 预算检查"采购审核 / 报销审核"两时机缺失**（**新根因**——既有 arm-index 全分区 grep `budget.?check\|BudgetChecker\|runBudgetCheck\|预算检查\|expense.?budget\|purchase.?budget` 无 RC finding 涉及 projects 预算检查时机缺失；与 P1-RC-003 finance 三列对比报表不同控制点[finance 报表列 vs projects 检查时机]，与 P1-MA2-084 finance 控制引擎不同域/不同方法）：L1 `use-cases.md:71` 逐字「检查时机: 工时提交 / 采购审核 / 报销审核(标注项目时)」要求**三时机**。L3 实仓：projects `BudgetChecker.check(projectId, addAmount)`（`module-projects/erp-prj-service/.../cost/BudgetChecker.java:34`）**唯一调用点** = 工时提交 `ErpPrjTimesheetSubmitProcessor.runBudgetCheckHook:103-104 → budgetChecker.check(timesheet.getProjectId(), costAmount)`（在 submit `:58` 触发）。grep 全 `module-projects` `BudgetChecker|budgetChecker.check|runBudgetCheck|budgetCheck` **仅此 2 文件命中**（BudgetChecker.java 定义 + TimesheetSubmitProcessor 单调用 + beans.xml:30 bean def）——**采购审核路径 + 报销审核路径零 projects BudgetChecker 调用**。重要边界澄清：`ErpFinExpenseClaimProcessor.runBudgetCheckHook:199-211` / `ErpPurOrderProcessor.runBudgetCheckHook:177-189` 等方法**确实存在**于 finance/purchase 模块，但它们调用的是 **finance `IErpFinBudgetControlBiz.check(subjectId, ..., periodId, ...)`**（UC-FIN-11 财务域预算硬拦截，按 subjectId/periodId 聚合），与 projects `BudgetChecker` 按 `projectId` 检查项目预算是**完全不同的控制点**（finance 财务预算 vs projects 项目预算，不同维度不同聚合口径不同实体）。本 finding 专门针对 **projects 项目预算**的两时机缺失。owner doc `cost-collection.md §3.3:127-131` 显式列出三时机表（工时提交 / 采购下单 / 费用报销）与 L1 一致，**未声明 Deferred**。**§4 三判据均不成立**（(i) 无独立 plan-audit + (ii) owner doc 未声明 Deferred + (iii) product-scope 未裁剪）→ Q4=(a) 强制实现。**与 P1-RC-049 衔接**：报销→项目费用归集已实现（`ExpenseCostAggregator.refreshExpenseCost:56-126`，A1.34 confirmed），故报销审核预算检查缺失是**独立验收标准未实现**（被检查对象存在，缺失的只是 budgetChecker.check 调用）；采购入库→项目物料归集**未实现**（P1-RC-049），故采购路径预算检查的"被检查对象尚不存在"——采购路径预算检查 = 物料归集 successor（P1-RC-049）的下游，可合并或降级为 successor 触发条件（新 finding **P1-RC-051**，纯 BizModel 代码逻辑修复预授权，不触发 §5 ask-first）。
2. **UC-PRJ-04 余量公式缺"已承诺成本"项**（**新根因**——既有 arm-index 全分区 grep `commitAmount\|已承诺成本\|commitment.*project\|项目.*承付` 无 RC finding 涉及 projects 预算余量公式 commitment 项缺失；与 P1-MA2-084 finance `ErpFinBudgetControlBiz.aggregateAmount` 含 COMMITMENT 不同控制点[finance 财务预算聚合 vs projects 项目预算公式]，与 P1-MA3-025 finance 三项式 vs javadoc 二项式 drift 不同域）：L1 `use-cases.md:72` 逐字「预算余量 = 项目预算 - 已归集成本 - 已承诺成本」要求**三项式**。L3 实仓：`BudgetChecker.check:44-69`（`module-projects/erp-prj-service/.../cost/BudgetChecker.java:44-69`）余量判定为 `used + addAmount > total`（`:56-58`），其中 `used = sumUsedAmount:79-103 = Σ 全部 CostCollectionLine.amount`（即"已归集成本"），**全文零 commitment/已承诺成本/committedAmount 概念**（实仓 grep `commitment\|committedAmount\|已承诺\|承付` 跨 `module-projects` 零业务命中），L1 三项式仅 2/3 实现。owner doc `cost-collection.md §3.1:113-116` 显式 documented simplification「行级 committedAmount/actualAmount 仍记录备查但不参与拦截。待多预算行项目粒度需求落地时改为行级 STRICT（successor）」存在，但 git log `cost-collection.md` 全为 AI commits `docs:` / `docs(audit-remediation):` 无人工批准痕迹（methodology §4 line 168「AI 自写标注**不算**人工批准」）。**裁决倾向 P2**（非 P1）：① 主路径[已归集成本拦截]可用且强测；② projects 域**无生产项目承诺源**——MATERIAL（采购入库）+ MATERIAL（领料）+ SUBCONTRACT 三类归集来源完全缺失（A1.34 P1-RC-049），即"已承诺成本"目前**无可归集的数据源**，三项式第三项实际为零；③ 待 P1-RC-049 物料归集 successor 落地后才会产生真正的承诺源，届时 P1-RC-051 与本 finding 应协同修复（在 budgetChecker.check 增加 commitment 维度查询）（新 finding **P2-RC-049** watch-only）。
3. **UC-PRJ-04 默认 WARNING 模式已确认**（**接受 + §9 真相源 drift 登记不降级**）：L1 `use-cases.md:74` 逐字「WARNING 模式: 警告但放行」要求双模式（STRICT 拒绝 + WARNING 放行）。L3 实仓 `ErpPrjConfigs:9-10 DEFAULT_BUDGET_CONTROL_MODE = BUDGET_MODE_WARNING`（默认 WARNING）+ `budgetControlStrict():42-44`（mode==STRICT 返回 true）+ `BudgetChecker.check:59-67`（STRICT 抛 `ERR_BUDGET_EXCEEDED` / WARNING `LOG.warn` 放行）**双模式完整实现**。owner doc `cost-collection.md §3.1:113-114` + §3.2:120-123 与 L1 一致。**行为完全满足**（不新建 finding，不复核不降级）。
4. **UC-PRJ-05 DAG 成环 + 前置守卫 → 接受**（**无新 finding**，复用 A2.13 + 强测已证实）：L1 `use-cases.md:87-89` 三条验收标准全部完整实现且 319 行强测覆盖（详见 §2-§5）。A2.13 §2.5 已证实 DAG 算法 1:1 对应 + 自环/跨项目/深度/前置 config-gated 全路径 PASS（`:196-200`）。本切片仅补需求契约视角复核，**确认 L1 全部验收标准在 L3-L5 各级均有证据且一致**，结论 = 接受。

---

## 1. 需求契约原文（逐字引用，§1 L1 格式）

> 来源 `docs/design/projects/use-cases.md`，逐字引用验收标准（禁止转述）。

### UC-PRJ-04 项目预算 STRICT 超支拦截（`:65`）
```
检查时机: 工时提交 / 采购审核 / 报销审核(标注项目时)
预算余量 = 项目预算 - 已归集成本 - 已承诺成本
STRICT 模式: 余量 < 0 → 拒绝该笔归集
WARNING 模式: 警告但放行
```

**断言计数（逐条完整枚举，禁止抽样）**：UC-PRJ-04 共 **4 条验收标准**：
- **断言①**「检查时机: 工时提交 / 采购审核 / 报销审核(标注项目时)」（**三时机**预算检查）
- **断言②**「预算余量 = 项目预算 - 已归集成本 - 已承诺成本」（**三项式**余量公式）
- **断言③**「STRICT 模式: 余量 < 0 → 拒绝该笔归集」（STRICT 拒绝）
- **断言④**「WARNING 模式: 警告但放行」（WARNING 放行）

### UC-PRJ-05 任务依赖 DAG 成环校验（`:81`）
```
任务.dependsOn 前置任务
前置任务.状态 != DONE → 本任务不可 IN_PROGRESS
依赖关系 DAG 成环 → 校验失败(拒绝建立环依赖)
```

**断言计数**：UC-PRJ-05 共 **4 条验收标准**（首行场景描述 + 3 条可验证断言，逐条进入 L5 判读）：
- **断言①**「任务.dependsOn 前置任务」（依赖模型存在）
- **断言②**「前置任务.状态 != DONE → 本任务不可 IN_PROGRESS」（前置完成校验）
- **断言③**「依赖关系 DAG 成环 → 校验失败(拒绝建立环依赖)」（成环检测）
- **断言④**（场景描述衍生的隐含验收）：前置状态机守卫配置化（task-dag.md §4.3 config-gated STRICT/WARN，与断言②配套——L1 字面"不可"在 STRICT 模式为抛错、在 WARN 模式为告警放行，L2 task-dag.md §4.3 显式 documented）

---

## 2. 实现证据（代码路径，§1 L3 格式，含跨域调用链）

> 全部 `module-projects/erp-prj-service/src/main/...`，含行号。

| 控制点 | 代码路径（file:line） | 备注 |
|--------|----------------------|------|
| **预算检查入口（projects BudgetChecker）** | `cost/BudgetChecker.java:34`（类声明，唯一预算检查器，按 projectId 检查项目预算） | L1 AC-①②③④ 核心 |
| **预算检查唯一调用点（工时提交）** | `processor/ErpPrjTimesheetSubmitProcessor.java:103-104 runBudgetCheckHook` → `budgetChecker.check(timesheet.getProjectId(), costAmount)`，在 `submit:58` 触发 | L1 AC-① 工时时机 ✅ |
| **预算检查调用面 grep（采购/报销）** | grep 全 `module-projects` `BudgetChecker\|budgetChecker.check\|runBudgetCheck\|budgetCheck` **仅 2 文件命中**（BudgetChecker 定义 + TimesheetSubmitProcessor 单调用）；**采购 Processor / 报销 Processor 零 projects BudgetChecker 调用**（finance `ErpFinExpenseClaimProcessor.runBudgetCheckHook:199-211` + purchase `ErpPurOrderProcessor.runBudgetCheckHook:177-189` 存在但调 finance `IErpFinBudgetControlBiz.check(subjectId, ...)` 财务预算，**非 projects `BudgetChecker.check(projectId, ...)` 项目预算**——不同控制点不同维度） | L1 AC-① **采购/报销时机 ❌**（**P1-RC-051**） |
| **预算余量公式** | `BudgetChecker.check:44-69`：`:52 total = project.getBudget()` + `:56 used = sumUsedAmount(projectId)` + `:57 projected = used.add(addAmount)` + `:58 if (projected.compareTo(total) > 0)`；`sumUsedAmount:79-103` = Σ `CostCollectionLine.getAmount()` 按 `costCollectionId IN (项目归集头)` 聚合（即"已归集成本"）。**全文零 commitment/已承诺成本/committedAmount 概念**（实仓 grep 跨 `module-projects` 零业务命中） | L1 AC-② **三项式仅 2/3 实现**（**P2-RC-049**） |
| **STRICT 拒绝 / WARNING 放行** | `BudgetChecker.check:59-67`：STRICT 模式（`ErpPrjConfigs.budgetControlStrict()` 返回 true）抛 `NopException(ERR_BUDGET_EXCEEDED)`（带 param `projectId/budgetTotal/budgetUsed/amount`）；WARNING 模式 `LOG.warn("项目 {} 预算超限（WARNING 模式放行）：总预算={}, 已使用={}, 拟新增={}", ...)` 放行 | L1 AC-③④ ✅ |
| **预算控制模式 config** | `ErpPrjConfigs.java:9-10 DEFAULT_BUDGET_CONTROL_MODE = ErpPrjConstants.BUDGET_MODE_WARNING`（**默认 WARNING**）+ `:33-40 budgetControlMode()` 读 `CONFIG_BUDGET_CONTROL_MODE` + `:42-44 budgetControlStrict()`（mode==STRICT 返回 true） | 默认 WARNING 已确认 |
| **任务依赖模型（单前置）** | ORM `ErpPrjTask.dependsOnId`（BIGINT，单列）+ `dependsOn`（自引用 to-one FK）；每任务**至多一个前置任务**（task-dag.md §1.1 单前置树/森林模型） | L1 AC-① ✅ |
| **DAG 成环检测算法** | `validator/TaskDependencyValidator.java:44-86 detectCycle`（task-dag.md §2.1 算法 1:1）：`:46-48 dependsOnId==null → return`（无前置放行）；`:49-52 taskId==dependsOnId → ERR_TASK_SELF_DEPENDENCY`（自环优先）；`:54-59 visited HashSet + chainOrder List`（起点 taskId 入 visited/chain）；`:61-85 while cursor!=null`：`:64-65 depth++ > maxDepth → ERR_TASK_DEPENDENCY_DEPTH_EXCEEDED`（防恶意长链）；`:71-76 cursor∈visited → ERR_TASK_DEPENDENCY_CYCLE`（chain 序列）；`:77-78 visited.add(cursor) + chainOrder.add(cursor)`；`:80-83 predecessor==null → break`（链终止）；`:84 cursor = predecessor.dependsOnId`（上行一步）。**O(N) 时间 + O(N) 空间 + maxDepth 兜底** | L1 AC-③ ✅ + A2.13 §2.5 PASS |
| **跨项目依赖校验** | `entity/ErpPrjTaskBizModel.java:92-98 validateDependency`：`if (!Objects.equals(task.getProjectId(), dependsOnTask.getProjectId())) → ERR_TASK_DEPENDENCY_CROSS_PROJECT`（带 4 param） | L1 AC-③ 配套（跨项目禁建） |
| **依赖保存钩子双触发** | `ErpPrjTaskBizModel.java:59-63 defaultPrepareSave`（插入钩子）+ `:65-69 defaultPrepareUpdate`（修改钩子）均调 `validateDependency:75-102`（→ detectCycle:101） | 保存/修改双触发 |
| **前置未完成不可开始（config-gated）** | `ErpPrjTaskBizModel.java:114 validatePredecessorDone(task)` 在 `startTask:108-119` 中调用（TODO→IN_PROGRESS 守卫 `:111-113` 之后）；`:172-193 validatePredecessorDone`：`:173-176 dependsOnId==null → return`（无前置放行）；`:177-180 predecessor==null → return`；`:181-184 TASK_STATUS_DONE → return`（前置已完成放行）；`:185-190 ErpPrjConfigs.taskStrictPredecessorCheck()` 返回 true → STRICT 抛 `ERR_TASK_PREDECESSOR_NOT_DONE`（带 3 param）；`:191-192 LOG.warn(...)` WARN 放行 | L1 AC-② ✅（STRICT/WARN config-gated） |
| **config 点（DAG）** | `ErpPrjConfigs.java:21-22 DEFAULT_TASK_DEPENDENCY_MAX_DEPTH = 100` + `:24-25 DEFAULT_TASK_STRICT_PREdecessor_CHECK = true`（默认 STRICT）；`:86-101 taskDependencyMaxDepth() / taskStrictPredecessorCheck()`（task-dag.md §6 配置点） | task-dag.md §6 |
| **任务 4 态状态机** | `ErpPrjTaskBizModel.java:106-119 startTask`（TODO→IN_PROGRESS）+ `:121-133 completeTask`（IN_PROGRESS→DONE）+ `:135-153 blockTask`（IN_PROGRESS→BLOCKED, `:145-148 blockReason 必填 ERR_TASK_BLOCK_REASON_REQUIRED`）+ `:155-167 unblockTask`（BLOCKED→IN_PROGRESS）；非法迁移 `illegalTransition:326-331` 抛 `ERR_TASK_ILLEGAL_STATUS_TRANSITION`（带 3 param） | A2.13 §2.5 PASS |

---

## 3. 测试证据（测试断言，§1 L4 格式，注明断言强度）

> 全部 `module-projects/erp-prj-service/src/test/java/app/erp/prj/service/` + `tests/e2e/business-actions/`。

### 强断言（DAG 全路径覆盖，319 行）
| 测试方法 | 断言强度 | 备注 |
|---------|---------|------|
| `service/TestErpPrjTaskDependency.java#scenario1_saveSelfDependency:61` | 强（断言 ERR_TASK_SELF_DEPENDENCY 抛出） | L1 UC-PRJ-05 AC-③ 自环优先 |
| `service/TestErpPrjTaskDependency.java#scenario2_saveCycleDependency:77` | 强（断言 ERR_TASK_DEPENDENCY_CYCLE 抛出 + chain 序列） | L1 UC-PRJ-05 AC-③ 成环检测 |
| `service/TestErpPrjTaskDependency.java#scenario3_saveCrossProjectDependency:94` | 强（断言 ERR_TASK_DEPENDENCY_CROSS_PROJECT 抛出） | L1 UC-PRJ-05 AC-③ 跨项目禁建 |
| `service/TestErpPrjTaskDependency.java#scenario4_startTask_predecessorNotDone_strict:113` | 强（STRICT 模式前置未完成抛 ERR_TASK_PREDECESSOR_NOT_DONE） | L1 UC-PRJ-05 AC-② STRICT 路径 |
| `service/TestErpPrjTaskDependency.java#scenario4_startTask_predecessorNotDone_warn:128` | 强（WARN 模式 LOG.warn 放行 + 状态迁移成功） | L1 UC-PRJ-05 AC-② WARN 路径 |
| `service/TestErpPrjTaskDependency.java#scenario5_startTask_happyPath:146` | 强（TODO→IN_PROGRESS 主路径） | L1 UC-PRJ-05 AC-② 正向 |
| `service/TestErpPrjTaskDependency.java#scenario6_illegalTransition:158` + `scenario6_blockReasonRequired:176` + `scenario6_stateMachineRoundTrip:185` | 强（非法迁移抛 ERR_TASK_ILLEGAL_STATUS_TRANSITION + blockReason 必填 ERR_TASK_BLOCK_REASON_REQUIRED + 4 态迁移完整闭环） | 任务状态机守卫 |
| `service/TestErpPrjTaskDependency.java#scenario7_findPredecessorsAndSuccessors:205` | 强（上行链全量 + 下行反查传递闭包） | task-dag.md §5 |
| `service/TestErpPrjTaskDependency.java#scenario8a_headSelfLoopFirst:235` + `scenario8b_longChainDepthExceeded:253` | 强（自环优先于深度判定 + maxDepth 兜底 ERR_TASK_DEPENDENCY_DEPTH_EXCEEDED） | task-dag.md §2.3 |
| `service/validator/TestTaskDependencyValidator.java#testSelfDependency:30 + testTwoNodeCycle:43 + testThreeNodeCycle:59 + testLongChainNoCycle:76 + testDepthExceeded:95 + testCollectPredecessors:118` | 强（6 @Test 纯函数式 detectCycle + collectPredecessors 单元测试） | task-dag.md §2.1 算法 |

### 强断言（预算-工时路径覆盖）
| 测试方法 | 断言强度 | 备注 |
|---------|---------|------|
| `service/TestErpPrjBudgetAndCollection.java#testWarningModeAllowsOverBudget:70` | 强（WARNING 模式 LOG.warn 放行 + 状态迁移成功） | L1 UC-PRJ-04 AC-④ ✅ |
| `service/TestErpPrjBudgetAndCollection.java#testStrictModeRejectsOverBudget:95` | 强（STRICT 模式抛 ERR_BUDGET_EXCEEDED + 带 4 param 精确断言） | L1 UC-PRJ-04 AC-③ ✅ |
| `service/TestErpPrjBudgetAndCollection.java#testApproveGeneratesCollectionLineAndUpdatesActualCost:122` | 强（approve 产 LABOR 归集行 + actualCost 回写） | L1 UC-PRJ-04 AC-① 工时时机正向 |
| `service/TestErpPrjBudgetAndCollection.java#testAggregationIsIdempotent:160` | 强（幂等去重） | 归集机制 |
| `service/TestErpPrjBudgetAndCollection.java#testCloseProjectFreezesAndRejectsNewTimesheet:188` | 强（closeProject 冻结拒新工时） | UC-PRJ-09 跨切片 |
| `service/TestErpPrjBudgetAndCollection.java#testCloseProjectRejectsNonOpen:226` | 强（拒非 OPEN） | UC-PRJ-01/09 跨切片 |
| `service/TestErpPrjBudgetAndCollection.java#testRequireReferenceableRejectsNonOpen:242` | 强（requireReferenceable 拒非 OPEN） | UC-PRJ-01 跨切片 |

### E2E（`tests/e2e/business-actions/`）
| Spec | 断言强度 | 备注 |
|------|---------|------|
| `business-actions/projects-task.action.spec.ts` | 强值断言 | TODO→IN_PROGRESS→DONE + block/unblock + 前置守卫 STRICT 拒绝 |

### ⚠️ 测试缺口（与功能缺口一致）
1. **UC-PRJ-04 采购审核预算检查**：**零测试**（与 P1-RC-051 实现缺口一致；采购路径物料归集未实现[P1-RC-049]，被检查对象尚不存在）；
2. **UC-PRJ-04 报销审核预算检查**：**零测试**（与 P1-RC-051 实现缺口一致；报销→项目费用归集已实现[ExpenseCostAggregator]，但 budgetChecker.check 调用缺失）；
3. **UC-PRJ-04 余量公式"已承诺成本"项**：**零负向测试**（与 P2-RC-049 实现缺口一致；projects 域无生产项目承诺源）。

---

## 4. 运行时行为证据（§1 L5 格式）

> 按 §去重协议，本切片复用 A2.13 已证实行为，只补需求视角差异。

### 复用 A2.13（projects 状态机审计）已证实行为
- **任务 4 态 + DAG 成环检测**（A2.13 §2.5）：`TaskDependencyValidator.detectCycle:44-86` 上行链 + HashSet + maxDepth + 自环优先 + 跨项目校验 + 保存/更新双钩子 + 前置 config-gated STRICT/WARN，与 `task-dag.md §2.1` 算法 1:1 对应。本切片 HEAD 复核确认 `:44-86` 行号未漂移。
- **跨域写经 I*Biz Facade 合规**（A2.13 §1 表）：本切片的 budget check 调用为 projects 内部调用（BudgetChecker 注入 IDaoProvider，无跨域）；工时过账经 `IErpFinVoucherBiz.post REQUIRES_NEW`（A1.34 复用），不重复验证。

### 复用 A2.13 P1 finding HEAD 复核（本切片直接相关的任务状态机 + DAG）
| Finding | A2.13 状态 | 本切片 HEAD 行号复核 | 复核结论 |
|---------|-----------|---------------------|---------|
| **P1-MA2-069** | ✅ resolved R1.21（方案 B Deferred） | `state-machine.md §适用对象三` Deferred 段落 + `cost-collection.md §4.2:194` APPROVED drift documented | **确认 resolved（resolved-via-deferral）无回退**——属 doc-only Deferred 非 implementation；本切片**不重开**（§去重协议）；与 P2-RC-049 余量公式不同控制点（dict drift vs 余量公式），互补不重复 |
| **P1-MA2-070** | ✅ resolved R1.21 | startProject 前置 `validateStartPreconditions:136-165` + cancelProject 多源 owner doc state-machine.md `:36-38` 补充（A1.34 复核） | **确认 resolved 无回退**（genuinely RESOLVED via implementation）；非本切片控制点（UC-PRJ-01/09），仅引用 |

### 本切片 L5 行为判读（结合 L3 代码静态分析 + 既有测试）
- **UC-PRJ-04 AC-① 工时时机**：工时提交 `ErpPrjTimesheetSubmitProcessor.submit:34-61` → `runBudgetCheckHook:103-104` → `budgetChecker.check(projectId, costAmount)` 在状态迁移 SUBMITTED 之前触发；STRICT 抛错回滚，WARNING 放行继续 `:59 timesheetDao().updateEntity(timesheet)`。**主路径 ✅**（`TestErpPrjBudgetAndCollection` 7 @Test 强断言 + E2E `projects-task` 强值）。
- **UC-PRJ-04 AC-① 采购/报销时机**：projects BudgetChecker **零调用方**（采购/报销路径）。报销路径 `ExpenseCostAggregator.refreshExpenseCost:56-126` 在 closeProject 前 `:63-65` 触发，归集已审报销行到 ErpPrjCostCollection——但**不调 budgetChecker.check**（违规归集行直接持久化）。采购路径物料归集未实现（P1-RC-049 successor）。**两时机缺失**（与 P1-RC-051 一致）。
- **UC-PRJ-04 AC-② 余量公式**：`BudgetChecker.check:52-58` 三项式仅 2/3 实现（total/used），第三项 commitment 概念实仓零命中。但 projects 域无生产项目承诺源（物料/分包归集均缺失[P1-RC-049]），三项式第三项实际为零——**主路径[已归集成本拦截]可用，边界[承诺维度]弱**（与 P2-RC-049 一致）。
- **UC-PRJ-04 AC-③④ STRICT/WARNING 双模式**：`BudgetChecker.check:59-67` + `ErpPrjConfigs:42-44` 完整实现，**默认 WARNING**（`DEFAULT_BUDGET_CONTROL_MODE = BUDGET_MODE_WARNING`），STRICT 抛 `ERR_BUDGET_EXCEEDED` / WARNING `LOG.warn` 放行——**完整实现且强测**。
- **UC-PRJ-05 AC-①②③④ DAG 全路径**：`TaskDependencyValidator.detectCycle:44-86`（成环 + 自环 + 深度 + 跨项目全错误码落地）+ `ErpPrjTaskBizModel.validateDependency:75-102`（保存钩子双触发）+ `validatePredecessorDone:172-193`（config-gated STRICT/WARN）+ 任务 4 态状态机（非法迁移守卫 + blockReason 必填）——**全部实现 + 319 行强测 + A2.13 §2.5 PASS**。

---

## 5. 五级追踪矩阵 + 符合性结论（§1 矩阵 + §2 判据）

### UC-PRJ-04 项目预算 STRICT 超支拦截

| 验收标准 | L1（逐字） | L3（代码） | L4（测试） | L5（行为） | 符合性 |
|---------|-----------|-----------|-----------|-----------|--------|
| ① 检查时机: 工时提交 / 采购审核 / 报销审核(标注项目时) | `use-cases.md:71` | **工时 ✅** `ErpPrjTimesheetSubmitProcessor.runBudgetCheckHook:103-104`；**采购 ❌** projects BudgetChecker 零调用方（finance `ErpPurOrderProcessor.runBudgetCheckHook:177-189` 调 finance `IErpFinBudgetControlBiz` 财务预算，非 projects 项目预算）；**报销 ❌** projects BudgetChecker 零调用方（finance `ErpFinExpenseClaimProcessor.runBudgetCheckHook:199-211` 同上调 finance 财务预算） | 工时强测 `TestErpPrjBudgetAndCollection` 7 @Test；**采购/报销预算检查零测试** | 工时 ✅；采购/报销 projects 预算检查缺失 | **接受 on 工时子维度**；采购/报销子维度 → **P1** → **P1-RC-051** |
| ② 预算余量 = 项目预算 - 已归集成本 - 已承诺成本 | `use-cases.md:72` | `BudgetChecker.check:52-58`：total = `project.getBudget()`；used = `sumUsedAmount(projectId)` = Σ `CostCollectionLine.amount`（已归集成本）；**第三项 commitment 零实现**（实仓 grep 跨 module-projects 零业务命中） | `TestErpPrjBudgetAndCollection#testStrictModeRejectsOverBudget:95` 强测二项式（used + addAmount > total）；**三项式无负向测试** | 主路径二项式拦截可用 + 强测；承诺项缺失但 projects 域无生产承诺源（物料/分包归集缺失[P1-RC-049]） | **P2** → **P2-RC-049** |
| ③ STRICT 模式: 余量 < 0 → 拒绝该笔归集 | `use-cases.md:73` | `BudgetChecker.check:59-64`（`ErpPrjConfigs.budgetControlStrict()` 返回 true 时抛 `NopException(ERR_BUDGET_EXCEEDED)` 带 4 param） | `TestErpPrjBudgetAndCollection#testStrictModeRejectsOverBudget:95-119` 强（断言 ERR_BUDGET_EXCEEDED 抛出） | 行为已证实（A2.13 + 强测） | **接受** |
| ④ WARNING 模式: 警告但放行 | `use-cases.md:74` | `BudgetChecker.check:65-67`（`LOG.warn` 放行）；`ErpPrjConfigs:9-10 DEFAULT_BUDGET_CONTROL_MODE = BUDGET_MODE_WARNING`（**默认 WARNING**） | `TestErpPrjBudgetAndCollection#testWarningModeAllowsOverBudget:70-92` 强（断言状态迁移成功） | 行为已证实（A2.13 + 强测 + 默认 WARNING 确认） | **接受** |

**UC-PRJ-04 整体裁决：P1**（取最高）。**接受 on ③④ STRICT/WARNING 双模式**（完整实现 + 强测 + 默认 WARNING 已确认）；**接受 on ① 工时子维度**（主路径）；**P1 on ① 采购/报销子维度**（projects BudgetChecker 零调用方——报销路径归集已实现故报销审核预算检查缺失是独立验收标准未实现；采购路径物料归集未实现[P1-RC-049]故被检查对象尚不存在→与物料归集 successor 合并）；**P2 on ② 余量公式"已承诺成本"项**（三项式仅 2/3，主路径[已归集成本]拦截可用）。**§4 三判据复核 P1-RC-051/P2-RC-049** 见下。

### UC-PRJ-05 任务依赖 DAG 成环校验

| 验收标准 | L1（逐字） | L3（代码） | L4（测试） | L5（行为） | 符合性 |
|---------|-----------|-----------|-----------|-----------|--------|
| ① 任务.dependsOn 前置任务 | `use-cases.md:87` | ORM `ErpPrjTask.dependsOnId`（单列）+ `dependsOn` 自引用 to-one FK；`ErpPrjTaskBizModel.defaultPrepareSave/Update:59-69` 经 `validateDependency:75-102` 保存钩子双触发 | `TestErpPrjTaskDependency#scenario5_startTask_happyPath:146` + `scenario7_findPredecessorsAndSuccessors:205` 强 | 行为已证实（A2.13 §2.5） | **接受** |
| ② 前置任务.状态 != DONE → 本任务不可 IN_PROGRESS | `use-cases.md:88` | `ErpPrjTaskBizModel.startTask:108-119`（TODO→IN_PROGRESS 守卫 `:111-113` 之后调 `validatePredecessorDone:114`）；`validatePredecessorDone:172-193`：`dependsOnId==null → return`/`predecessor==null → return`/`TASK_STATUS_DONE → return`/`ErpPrjConfigs.taskStrictPredecessorCheck()` STRICT 抛 `ERR_TASK_PREDECESSOR_NOT_DONE` / WARN `LOG.warn` 放行 | `TestErpPrjTaskDependency#scenario4_startTask_predecessorNotDone_strict:113` + `scenario4_startTask_predecessorNotDone_warn:128` + `scenario5_startTask_happyPath:146` 强（三路径全覆盖） | 行为已证实（A2.13 §2.5 + 强测 + E2E） | **接受** |
| ③ 依赖关系 DAG 成环 → 校验失败(拒绝建立环依赖) | `use-cases.md:89` | `TaskDependencyValidator.detectCycle:44-86`（task-dag.md §2.1 算法 1:1）：自环优先 `:49-52 ERR_TASK_SELF_DEPENDENCY` / 上行链 HashSet + chainOrder `:54-85` / `depth++ > maxDepth → ERR_TASK_DEPENDENCY_DEPTH_EXCEEDED` / `cursor∈visited → ERR_TASK_DEPENDENCY_CYCLE`；跨项目校验 `ErpPrjTaskBizModel.validateDependency:92-98 ERR_TASK_DEPENDENCY_CROSS_PROJECT` | `TestErpPrjTaskDependency#scenario1_saveSelfDependency:61` + `scenario2_saveCycleDependency:77` + `scenario3_saveCrossProjectDependency:94` + `scenario8a_headSelfLoopFirst:235` + `scenario8b_longChainDepthExceeded:253` 强（5 错误码全覆盖）+ `validator/TestTaskDependencyValidator` 6 @Test 纯函数式单元测试 | 行为已证实（A2.13 §2.5 + 319 行强测） | **接受** |
| ④ 配置化 STRICT/WARN（隐含验收，task-dag.md §4.3） | `task-dag.md §4.3` + `use-cases.md:88` 衍生 | `ErpPrjConfigs:24-25 DEFAULT_TASK_STRICT_PREDECESSOR_CHECK = true`（默认 STRICT）+ `:96-101 taskStrictPredecessorCheck()`；`ErpPrjConfigs:21-22 DEFAULT_TASK_DEPENDENCY_MAX_DEPTH = 100` + `:86-94 taskDependencyMaxDepth()` | `scenario4_strict/warn` 双路径覆盖 | 行为已证实（默认 STRICT 确认 + WARN 路径强测） | **接受** |

**UC-PRJ-05 整体裁决：接受**（取最高 = 接受）。**接受 on 全部 4 验收标准**（DAG 成环检测算法 + 跨项目校验 + 自环优先 + 前置未完成 config-gated STRICT/WARN 守卫 + 任务 4 态状态机非法迁移守卫 + blockReason 必填——全部完整实现 + 319 行强测 + A2.13 §2.5 PASS + E2E `projects-task` 强值断言）。**无新 finding**（复用 A2.13 §2.5 + 强测已证实）。

### 矩阵结论汇总

| UC | 整体裁决 | 主 finding |
|----|---------|-----------|
| UC-PRJ-04 | **P1**（接受 on ① 工时子维度 + ③④ STRICT/WARNING 双模式；P1 on ① 采购/报销子维度；P2 on ② 余量公式） | P1-RC-051（采购/报销预算检查时机缺失）+ P2-RC-049（余量公式缺"已承诺成本"项） |
| UC-PRJ-05 | **接受**（接受 on 全部 4 验收标准） | 无新 finding（复用 A2.13 §2.5 + 强测） |

**零 P0**。**1 新 P1 + 1 新 P2 + 0 复用 resolved finding（A2.13 §2.5 DAG 行为直接复用，无交叉引用注记）**。

### §4 三判据复核（P1 项强制）

| P1 候选 | (i) plan-audit | (ii) owner doc documented simplification | (iii) product-scope 裁剪 | 裁决 |
|---------|---------------|------------------------------------------|------------------------|------|
| **P1-RC-051**（UC-PRJ-04 预算检查采购/报销时机缺失） | ❌（本切片候选偏差未经独立 plan-audit 裁决为简化） | ❌（owner doc `cost-collection.md §3.3:127-131` 显式列出三时机表「工时提交 / 采购下单 / 费用报销」与 L1 一致，**未声明 Deferred**；§3.1:113-116 的 documented simplification 仅覆盖"行级 controlMode 字段"，**不覆盖检查时机**——是明确需求契约） | ❌（`product-scope.md` grep `预算检查\|budget.?check\|采购审核\|报销审核\|三时机` 零命中——预算检查时机未列入范围裁剪） | **P1 强制实现**（Q4=(a) 三判据均不成立）。**非 P0**（不破坏主路径——工时路径预算检查完整实现，采购/报销缺失是"漏检"非"破坏活跃数据"——违规归集行经幂等去重可手工清理；业财影响：超预算的采购/报销归集行直接持久化，项目成本虚增 + PnL 失真，但 GL 借贷平衡 + 工时主路径预算检查不受影响）。**与 P1-RC-050 ON_HOLD 费用门控缺失[UC-PRJ-09]互补**（不同 UC、不同控制点：状态门控[UC-PRJ-09 拒绝新费用归集] vs 预算检查[UC-PRJ-04 拒绝超预算归集]，但都涉及 ExpenseCostAggregator 的归集守卫，可一次性协同修复）。**与 P1-RC-049 物料归集缺失衔接**：报销路径归集已实现（ExpenseCostAggregator works），故报销审核预算检查缺失是独立验收标准未实现；采购路径物料归集未实现（P1-RC-049），故采购路径预算检查的"被检查对象尚不存在"——采购路径预算检查 = 物料归集 successor（P1-RC-049）的下游，**合并修复触发条件**（物料归集实现时同步接 budgetChecker.check）。修复 = 报销路径 `ExpenseCostAggregator.refreshExpenseCost:60-64` 增 `budgetChecker.check(projectId, expenseAmount)` 调用（在归集行写入前；与 P1-RC-050 状态门控修复协同，一次性闭合两 finding）+ 采购路径物料归集实现时（P1-RC-049 successor）同步接 budgetChecker.check；**纯 BizModel 代码逻辑修复，按 roadmap 预授权类目可自动执行，不触发 §5 ask-first**；建议补 STRICT 模式报销审核超预算拒绝负向测试 |

### §4 三判据复核（P2 项声明 Q4=(a) 张力）

| P2 候选 | (i) plan-audit | (ii) owner doc documented simplification | (iii) product-scope 裁剪 | 裁决 |
|---------|---------------|------------------------------------------|------------------------|------|
| **P2-RC-049**（UC-PRJ-04 余量公式缺"已承诺成本"项） | ❌（未经独立 plan-audit） | ⚠ **部分满足但非人工批准**：owner doc `cost-collection.md §3.1:113-116` 显式 documented simplification「行级 committedAmount/actualAmount 仍记录备查但不参与拦截。待多预算行项目粒度需求落地时改为行级 STRICT（successor）」存在，但 git log `cost-collection.md` 全为 AI commits `docs:` / `docs(audit-remediation):` 无人工批准痕迹——methodology §4 line 168「AI 自写标注**不算**人工批准」。**声明 Q4=(a) 张力**：若严格按 Q4=(a) 应升级 P1，但实际影响受限（见裁决列） | ❌（`product-scope.md` 未将承诺成本项裁剪） | **倾向 P2 watch-only**（不强制修复）+ **声明 Q4=(a) 张力**：① 主路径[已归集成本拦截]可用且强测；② **projects 域无生产项目承诺源**——MATERIAL（采购入库）+ MATERIAL（领料）+ SUBCONTRACT 三类归集来源完全缺失（P1-RC-049），即"已承诺成本"目前**无可归集的数据源**，三项式第三项实际为零；③ 待 P1-RC-049 物料归集 successor 落地后才会产生真正的承诺源，届时 P1-RC-051 与本 finding 应协同修复（在 budgetChecker.check 增加 commitment 维度查询 + 三项式补全）；④ 与 A1.34 P1-RC-048/049/050 同型 AI 自标 Non-Goal 无人工批准痕迹 → §4 三判据不满足。**降级为 P2 的合理性**：实际运行时影响为零（无可归集的承诺数据源），与 P1-RC-049 修复触发条件绑定。**若人工确认 product-scope 要求"已承诺成本"项独立于物料归集实现 → 升级 P1** |

---

## 6. 与 arm-index 衔接（§7 复用 or 新增 裁决）

按 §7 规则，本报告产出 finding 前已 grep `arm-index.md` 同域（projects）同控制点（budget check / strict / warning / commitment / predecessor / dag / cycle），裁决如下：

### 6.1 复用既有 finding（追加 RC A1.35 交叉引用，不新建）

| Finding ID | 报告 | 复用理由 |
|-----------|------|---------|
| **A2.13 §2.5 任务 4 态 + DAG 成环检测 PASS**（A2.13 report 无独立 P-finding，其状态机审计确认任务 DAG 行为正确） | `2026-07-28-1020-arm-ma2-projects-state-machine.md` | UC-PRJ-05 全部 4 验收标准**同根因同控制点**（DAG 成环 + 前置守卫 + 状态机）；A2.13 §2.5 PASS（`:196-200`）+ 本切片 HEAD 复核 `TaskDependencyValidator.detectCycle:44-86` + `ErpPrjTaskBizModel.validatePredecessorDone:172-193` 行号未漂移。**追加 RC A1.35 交叉引用注记**：本切片需求契约视角复核 UC-PRJ-05 全部 4 验收标准**确认接受**，无升级，无新 finding |
| **P1-MA2-069** | `2026-07-28-1020-arm-ma2-projects-state-machine.md` | CostCollection doc-status dict drift resolved-via-deferral；与 P2-RC-049 余量公式不同控制点（dict drift vs 余量公式 commitment 项），互补不重复。本切片**不重开**（§去重协议），仅引用 |
| **P1-MA1-010**（reuse，todo MR1） | `2026-07-2*-arm-ma1-*` | UC-PRJ-04 多币种 propId 缺失（工时过账 buildEvent 硬编码 exchangeRate=ONE 单币种路径）；与预算检查时机/余量公式不同维度/不同控制点。**维持 todo MR1** |
| **P1-MA1-022**（reuse，todo MR1） | `2026-07-2*-arm-ma1-*` | 跨域只读 daoFor（projects→finance ErpMdSubject + ErpFinExpenseClaimLine）；与预算检查/DAG 正交，仅引用。**维持 todo MR1** |
| **P1-RC-003**（reuse，todo MR1，finance） | `rc-ma1-a1-2-finance-f2-budget` | finance 三列对比报表（getBudgetVsActual）缺 COMMITMENT 列；与 P2-RC-049（projects BudgetChecker 余量公式缺 commitment 项）**不同域不同控制点**（finance 报表 vs projects 检查器 / 报表展示 vs 拦截决策 / VoucherLine 聚合 vs CostCollectionLine 聚合）。互补不重复 |
| **P1-MA2-084**（reuse，resolved，finance） | `ma2-budget-commitment-release` | finance `ErpFinBudgetControlBiz.aggregateAmount` 含 COMMITMENT（已 fix 三通道分离）；与 P2-RC-049（projects BudgetChecker 余量公式缺 commitment 项）**不同域不同方法**（finance 财务预算控制引擎 vs projects 项目预算检查器）。互补不重复 |
| **P1-RC-049**（reuse，todo MR1，projects UC-PRJ-03） | `rc-ma1-a1-34-projects-f1-startup-cost-collection` | UC-PRJ-03 物料+领料+分包归集缺失；**P1-RC-051 采购路径预算检查依赖物料归集先落地**（采购路径被检查对象尚不存在），故采购路径预算检查 = P1-RC-049 successor 的下游；**P2-RC-049 余量公式承诺项**亦依赖物料归集 successor（projects 域无生产承诺源）。**协同修复触发条件**：P1-RC-049 物料归集实现时同步接 budgetChecker.check + 在 check 公式增加 commitment 维度查询 |
| **P1-RC-050**（reuse，todo MR1，projects UC-PRJ-09） | `rc-ma1-a1-34-projects-f1-startup-cost-collection` | UC-PRJ-09 ON_HOLD 费用路径状态门控缺失；**P1-RC-051 报销路径预算检查缺失**与 P1-RC-050 同站点（ExpenseCostAggregator.refreshExpenseCost）不同控制点（预算检查 vs 状态门控）。**协同修复**：报销路径 `ExpenseCostAggregator:60-64` 一次性补状态守卫[P1-RC-050] + budgetChecker.check 调用[P1-RC-051]，两 finding 同步闭合 |
| **P2-RC-048**（reuse，watch-only，projects UC-PRJ-01） | `rc-ma1-a1-34-projects-f1-startup-cost-collection` | UC-PRJ-01 requireReferenceable 单一咽喉未被使用；与 P1-RC-051 不同控制点（requireReferenceable Facade 未被消费 vs budgetChecker.check 未被报销路径调用）。互补不重复 |

### 6.2 新建 finding（与既有 arm-index 无同根因同控制点）

| Finding ID | 报告 | 域 | UC | 描述 | 分级判据 | 目标 MR | 修复状态 |
|-----------|------|---|----|------|---------|--------|---------|
| **P1-RC-051** | rc-ma1-a1-35-projects-f2-budget-dag | projects | UC-PRJ-04 AC-① | **项目预算检查"采购审核 + 报销审核"两时机缺失（projects BudgetChecker 唯一调用点=工时提交；finance/purchase 的 runBudgetCheckHook 是财务预算非项目预算，不同控制点）**：L1（`use-cases.md:71`）逐字「检查时机: 工时提交 / 采购审核 / 报销审核(标注项目时)」要求**三时机**。L3 实仓：projects `BudgetChecker.check(projectId, addAmount)`（`BudgetChecker.java:44-69`）**唯一调用点** = `ErpPrjTimesheetSubmitProcessor.runBudgetCheckHook:103-104`；grep 全 `module-projects` `BudgetChecker\|budgetChecker.check\|runBudgetCheck\|budgetCheck` **仅 2 文件命中**——**采购审核路径 + 报销审核路径零 projects BudgetChecker 调用**。**边界澄清**：finance `ErpFinExpenseClaimProcessor.runBudgetCheckHook:199-211` + purchase `ErpPurOrderProcessor.runBudgetCheckHook:177-189` 等方法存在但调 **finance `IErpFinBudgetControlBiz.check(subjectId, ..., periodId, ...)`**（UC-FIN-11 财务预算，按 subjectId/periodId 聚合 VoucherLine），与 projects `BudgetChecker.check(projectId, ...)` 按 projectId 检查 ErpPrjProject.budget 是**完全不同的控制点**（finance 财务预算 vs projects 项目预算，不同维度不同聚合口径不同实体）。owner doc `cost-collection.md §3.3:127-131` 显式列出三时机表与 L1 一致，**未声明 Deferred**。**§4 三判据均不成立**（(i) 无独立 plan-audit + (ii) owner doc 未声明 Deferred[仅 §3.1:113-116 documented"行级 controlMode 字段"非"检查时机"] + (iii) product-scope 未裁剪）→ Q4=(a) 强制实现。**新根因**（既有 arm-index 全分区 grep `budget.?check\|BudgetChecker\|runBudgetCheck\|预算检查时机` 无 RC finding 涉及 projects 预算检查时机缺失；与 P1-RC-003 finance 三列报表不同控制点，与 P1-MA2-084 finance 控制引擎不同域不同方法）。**非 P0**（不破坏主路径——工时路径预算检查完整实现，采购/报销缺失是"漏检"非"破坏活跃数据"——违规归集行经幂等去重可手工清理；业财影响：超预算的采购/报销归集行直接持久化，项目成本虚增 + PnL 失真，但 GL 借贷平衡 + 工时主路径预算检查不受影响）。**与 P1-RC-050 ON_HOLD 费用门控缺失[UC-PRJ-09]互补**（同站点 ExpenseCostAggregator 不同控制点：状态门控 vs 预算检查，一次性协同修复）。**与 P1-RC-049 物料归集缺失衔接**：报销路径归集已实现故报销审核预算检查缺失是独立验收标准未实现；采购路径物料归集未实现（P1-RC-049）故采购路径被检查对象尚不存在——采购路径预算检查 = 物料归集 successor 的下游，**合并修复触发条件**。 | §2 P1①（功能实质偏离验收标准——三时机仅 1/3 实现）+ §2 P1②（异常路径未实现——超预算的采购/报销应拒绝未实现） | MR1（R1.0 展开为 RC-R1.n） | todo（本审计仅登记，不实施修复；修复 = 报销路径 `ExpenseCostAggregator.refreshExpenseCost:60-64` 增 `budgetChecker.check(projectId, expenseAmount)` 调用[在归集行写入前，与 P1-RC-050 状态门控修复协同一次性闭合两 finding] + 采购路径物料归集实现时[P1-RC-049 successor]同步接 budgetChecker.check；**纯 BizModel 代码逻辑修复，按 roadmap 预授权类目可自动执行，不触发 §5 ask-first**；建议补 STRICT 模式报销审核超预算拒绝负向测试） |
| **P2-RC-049** | rc-ma1-a1-35-projects-f2-budget-dag | projects | UC-PRJ-04 AC-② | **预算余量公式缺"已承诺成本"项（三项式仅 2/3 实现，主路径已归集成本拦截可用，projects 域无生产承诺源[物料/分包归集缺失 P1-RC-049]致第三项实际为零）**：L1（`use-cases.md:72`）逐字「预算余量 = 项目预算 - 已归集成本 - 已承诺成本」要求**三项式**。L3 实仓 `BudgetChecker.check:44-69`：`:52 total = project.getBudget()` + `:56 used = sumUsedAmount(projectId)` + `:57-58 if (used.add(addAmount).compareTo(total) > 0)`——**二项式实现**（total - used - addAmount），**第三项 commitment 零实现**（实仓 grep `commitment\|committedAmount\|已承诺\|承付` 跨 `module-projects` 零业务命中）。owner doc `cost-collection.md §3.1:113-116` 显式 documented simplification「行级 committedAmount/actualAmount 仍记录备查但不参与拦截」存在，但 git log 全为 AI commits 无人工批准痕迹——methodology §4 line 168「AI 自写标注**不算**人工批准」。**声明 Q4=(a) 张力**：若严格按 Q4=(a) 应升级 P1，但实际影响受限：① 主路径[已归集成本拦截]可用且强测；② **projects 域无生产项目承诺源**——MATERIAL（采购入库）+ MATERIAL（领料）+ SUBCONTRACT 三类归集来源完全缺失（P1-RC-049），即"已承诺成本"目前**无可归集的数据源**，三项式第三项实际为零；③ 待 P1-RC-049 物料归集 successor 落地后才会产生真正的承诺源，届时应协同 P1-RC-051 一次性补全三项式。**新根因**（既有 arm-index 全分区 grep `commitAmount\|已承诺成本\|commitment.*project\|项目.*承付` 无 RC finding 涉及 projects 预算余量公式 commitment 项缺失；与 P1-MA2-084 finance 控制引擎含 COMMITMENT 不同域不同方法，与 P1-MA3-025 finance 三项式 vs javadoc 二项式 drift 不同域）。**非 P0/P1**（不破坏活跃数据/会计正确性/核心循环——主路径[已归集成本]拦截可用 + projects 域无生产承诺源致第三项实际为零 + 与 P1-RC-049 物料归集 successor 修复触发条件绑定）。 | §2 P2①（次要验收标准未完全满足——主路径[已归集成本拦截]OK 边界[承诺维度]弱）+ §2 P2③（owner doc 显式 documented simplification 但 §4 三判据(ii) 不满足"人工批准"——AI 自标 ≠ 人工批准，仅作 Q4 张力声明） | successor watch-only（P2 登记不强制）+ 声明 Q4=(a) 张力 | todo（修复 = `BudgetChecker.check` 增 commitment 维度查询[需 projects 域新增承诺源实体或跨域查询 finance commitment 凭证] + 三项式补全 `available = total - used - commitment`；**与 P1-RC-049 物料归集 successor 协同**[物料归集实现时同步接 budgetChecker.check + 补 commitment 查询]；修复触及 ORM 结构变更或跨域契约须 ask-first + 独立 plan-audit §5） |

### 6.3 finding → 修复追踪

| Finding | 目标 MR | 触及保护区域 | 修复状态 |
|---------|--------|-------------|---------|
| **A2.13 §2.5 任务 DAG 行为**（reuse，已证实 PASS） | N/A | N/A | 行为已证实（本切片 HEAD 复核 `TaskDependencyValidator.detectCycle:44-86` 行号未漂移 + 319 行强测） |
| **P1-MA2-069**（reuse，resolved-via-deferral） | MR1（resolved-via-deferral） | N/A | resolved-via-deferral（**本切片不重开**[§去重协议]） |
| **P1-MA1-010**（reuse，todo MR1） | MR1 | 是——ORM 结构变更（多币种四件套 propId） | todo（多币种维度，本切片仅引用不重审） |
| **P1-MA1-022**（reuse，todo MR1） | MR1 | 否（跨域只读 daoFor） | todo（跨域只读，本切片仅引用不重审） |
| **P1-RC-003**（reuse，todo MR1，finance） | MR1（R1.0 → RC-R1.n） | 否（纯 BizModel/DTO 代码逻辑修复） | todo（finance 报表，本切片仅引用不重审） |
| **P1-MA2-084**（reuse，resolved，finance） | MR1（resolved） | N/A | resolved（finance 控制引擎三通道分离，本切片仅引用） |
| **P1-RC-049**（reuse，todo MR1，projects UC-PRJ-03） | MR1（R1.0 → RC-R1.n） | **是——跨域契约 + 可能 ORM 结构变更** | todo（**P1-RC-051 采购路径 + P2-RC-049 承诺项依赖本 finding 先落地**） |
| **P1-RC-050**（reuse，todo MR1，projects UC-PRJ-09） | MR1（R1.0 → RC-R1.n） | 否（纯 BizModel 代码逻辑） | todo（**与 P1-RC-051 报销路径修复协同一次性闭合**——同站点 ExpenseCostAggregator 不同控制点） |
| **P2-RC-048**（reuse，watch-only，projects UC-PRJ-01） | successor watch-only | 否（纯 BizModel） | todo（watch-only；与 P1-RC-050/P1-RC-051 修复协同可一次性闭合） |
| **P1-RC-051**（新建） | MR1（R1.0 → RC-R1.n） | 否（纯 BizModel 代码逻辑——报销路径 `ExpenseCostAggregator.refreshExpenseCost:60-64` 增 `budgetChecker.check(projectId, expenseAmount)` 调用，与 P1-RC-050 状态门控修复协同；采购路径物料归集实现时[P1-RC-049 successor]同步接线；按 roadmap 预授权类目[代码逻辑修复]可自动执行，**不触发 §5 ask-first**） | todo |
| **P2-RC-049**（新建） | successor watch-only（P2 登记不强制）+ 声明 Q4=(a) 张力 | **是——可能 ORM 结构变更或跨域契约**（projects 域新增承诺源实体或跨域查询 finance commitment 凭证；须 ask-first + 独立 plan-audit） | todo（**与 P1-RC-049 物料归集 successor 协同**[物料归集实现时同步补 commitment 查询 + 三项式补全]） |

---

## 7. 静态存疑点清单（供 MA4 展开）

> §1 L5 存疑点：L5 无法静态定论、需运行时确认的点。每存疑点一行。

| # | 存疑点 | 静态判定 | MA4 展开方向 |
|---|--------|---------|-------------|
| SP-1 | `ExpenseCostAggregator.refreshExpenseCost` 在已超预算项目上归集的实际运行时行为（P1-RC-051 静态判定为"漏检"——超预算的报销行直接持久化，projects BudgetChecker 未被调用，违规归集行经幂等去重累积使项目成本虚增 + PnL 失真；GL 借贷平衡不受影响；需运行时确认是否经 closeProject 触发链路反向清理或仅累积） | 静态：L3 `ExpenseCostAggregator:60-64` 仅查 project==null 不调 budgetChecker；closeProject 前 `:63-65` 触发 `refreshExpenseCost`——若项目已超预算时报销行已归集，closeProject 触发的 refresh 是否归集该期间单据需运行时确认（与 P1-RC-050 SP-1 同根因——ON_HOLD/超预算两路径在 ExpenseCostAggregator 同站点） | A4.1 运行时：构造超预算项目（budget < 已审报销金额）+ closeProject 前 `refreshExpenseCost` → 断言违规报销行是否被归集 + actualCost 是否含超预算金额 + STRICT 模式是否抛错（预期不抛——budgetChecker 未被调用） |
| SP-2 | STRICT 模式下工时提交超预算的实际拒绝运行时行为（UC-PRJ-04 AC-③ 静态判定为"抛错回滚"——`BudgetChecker.check:59-64` STRICT 模式抛 `ERR_BUDGET_EXCEEDED`，submit 抛错回滚 timesheet 状态不迁移；需运行时确认事务边界是否完整回滚 + 幂等性） | 静态：`ErpPrjTimesheetSubmitProcessor.submit:34-61` `runBudgetCheckHook:58` 在 setStatus SUBMITTED 之后 `:57`、`timesheetDao().updateEntity:59` 之前触发；STRICT 抛错应触发 @BizMutation 事务回滚使 timesheet 状态保持 UNSUBMITTED；需运行时确认事务边界 | A4.1 运行时：构造 STRICT 模式 + 工时 costAmount 使 used+amount > total → submit → 断言 timesheet.status==UNSUBMITTED[事务回滚] + 异常为 ERR_BUDGET_EXCEEDED 带 4 param |
| SP-3 | 报销路径在 ON_HOLD/COMPLETED 项目 + 超预算两条件叠加的实际运行时行为（P1-RC-050 + P1-RC-051 双缺口叠加——ExpenseCostAggregator 既不校验状态[ON_HOLD/COMPLETED/CANCELLED] 也不调 budgetChecker，违规归集行直接持久化；两 finding 修复须协同） | 静态：L3 `ExpenseCostAggregator:60-64` 双缺口并存；与 P1-RC-050 SP-1 + P1-RC-051 SP-1 同根因——一次性补状态守卫 + budgetChecker.check 调用可闭合三 finding | A4.1 运行时：构造 ON_HOLD + 超预算项目 + 期间已审报销单 → closeProject 前 refreshExpenseCost → 断言违规归集行是否被归集（预期是——两缺口并存） |
| SP-4 | 采购路径物料归集实现后 budgetChecker.check 接入的实际运行时行为（P1-RC-049 successor 落地后，采购入库→项目物料归集同步接 budgetChecker.check 应阻断超预算采购；与 P2-RC-049 承诺项协同——物料归集实现时同步补 commitment 查询 + 三项式补全） | 静态：本切片无生产代码（P1-RC-049 物料归集未实现）；P1-RC-049 修复时同步接 budgetChecker.check + 在 check 公式增加 commitment 维度查询为 successor 触发条件 | A4.1 运行时：依赖 P1-RC-049 物料归集 successor 实现——届时构造采购入库→项目归集（订单行标 projectId）+ 超 budget → 断言 budgetChecker.check 是否阻断 + STRICT 模式是否抛 ERR_BUDGET_EXCEEDED |

---

## 8. 过程纪律自检

- [x] **checker 退出码门控核查**：本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`，actual vs baseline 见下表。**区分门控退出码 vs 纯 reporter 退出码**——checker 脚本是纯 reporter（退出码恒 0），真正门控在 CI workflow（`.github/workflows/compliance.yml`）解析 actual > baseline => sys.exit(1)。本报告**不**以 checker 脚本退出码 0 作为门控通过依据。**本报告无生产代码变更（纯审计报告），checker 无回归风险**。

  | 规则 | 描述 | actual | baseline | 状态 |
  |------|------|--------|----------|------|
  | R1d | dao().findAllByQuery (BizModel) | 14 | 14 | ✓ ≤ |
  | R2a | BizModel daoFor(ErpMd*) | 34 | 34 | ✓ ≤ |
  | R2b | BizModel daoFor(Erp*) 跨域 | 229 | 229 | ✓ ≤ |
  | R2c | 全生产代码 daoFor() 总量 | 1382 | 1382 | ✓ ≤ |
  | R2d | Processor/Dispatcher/Engine 中 daoFor(ErpMd*) | 34 | 34 | ✓ ≤ |
  | R3+ | new Erp*() / R5-R12 等 | **未实测**（pre-existing tooling bug） | 5/0/2/0/6/0/69/66/40 | **⚠ pre-existing 工具漂移**（见下注） |

  > **R3+ 未实测注记（pre-existing tooling bug，非本审计引入）**：`nop-compliance-checker.sh:177-180` R3 ENTITY_WHITELIST 构建管道使用 `xargs grep -oh '<entity className="[^"]*"'`，但 orm.xml 在 commit `738810aa5`（2026-08-04 "feat(flux): 全18域实体翻转 web-renderer=flux 并重生成"）后改为**多行 `<entity` + `className="..."` 格式**（如 `<entity ext:web-renderer="flux" className="..."`），单行 grep 无法匹配 → ENTITY_WHITELIST 空 → `set -euo pipefail` 致脚本在 R3 段早退（实测 7s 完成，输出截断至 141 行 R3 头）。**该 tooling bug 是 pre-existing**（2026-08-04 引入，**与本审计无关**——本审计为只读，零生产代码变更）；本报告**不处理**工具修复（属 compliance-baseline 维护范畴，非审计范围）。R1d/R2a/R2b/R2c/R2d 实测均 ≤ baseline（无回归），R3+ 引用 A1.34 报告与最新 baseline.md（post-R6.9）实测记录作为参考（actual=5/0/2/0/6/0/69/66/40 与 baseline 一致）。**本报告无生产代码变更故不引入任何 compliance 漂移**。

- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告全部 finding 已按 §7 规则 grep arm-index projects 同域同控制点（budget check / strict / warning / commitment / predecessor / dag / cycle）后给出"复用 or 新增"裁决（§6.1 复用裁决表 + §6.2 新建裁决表），无未经比对直接新建的 finding。P1-RC-051（预算检查采购/报销时机缺失）+ P2-RC-049（余量公式承诺项缺失）经 grep 确认为 projects 域新控制点（与 P1-RC-003 finance 报表 + P1-MA2-084 finance 控制引擎 + P1-MA3-025 finance javadoc drift 不同域不同控制点不同方法）；A2.13 §2.5 任务 DAG 行为 + P1-MA2-069 dict drift + P1-MA1-010/022 跨切片 + P1-RC-003/049/050/048 同域不同控制点经 grep 确认 → 复用并列明差异依据。

---

## Verdict

**pass（零 P0、1 新 P1[P1-RC-051 UC-PRJ-04 预算检查采购/报销时机缺失，纯 BizModel 代码逻辑修复预授权] + 1 新 P2[P2-RC-049 UC-PRJ-04 余量公式缺"已承诺成本"项 watch-only + Q4=(a) 张力声明] + 0 复用 resolved finding[本切片直接复用 A2.13 §2.5 DAG 行为，无独立 P-finding 交叉引用注记] + 6 复用 reuse finding[A2.13 §2.5 任务 DAG + P1-MA2-069 resolved-via-deferral + P1-MA1-010/022 跨切片 todo MR1 + P1-RC-003 finance 报表 + P1-MA2-084 finance 控制引擎 resolved + P1-RC-049 物料归集 + P1-RC-050 ON_HOLD 费用门控 + P2-RC-048 requireReferenceable watch-only] + 1 UC 接受含 P1/P2[UC-PRJ-04 工时时机 + STRICT/WARNING 双模式接受 on 主路径，采购/报销时机 P1 + 余量公式 P2] + 1 UC 接受[UC-PRJ-05 DAG 全部 4 验收标准接受，无新 finding，复用 A2.13 §2.5 + 319 行强测]）**。本切片解除 A1.35 在 MA4（A4.1 运行时展开器 SP-1~SP-4）及 MR1（R1.0 RC-R1.n 链路）的该切片证据缺口。**P0 即时通道未触发**（本切片无 P0——P1-RC-051 预算检查时机缺失属"漏检"非"破坏活跃数据"——违规归集行经幂等去重可手工清理 + GL 借贷平衡不受影响 + 工时主路径预算检查完整实现；P2-RC-049 余量公式承诺项缺失实际影响为零因 projects 域无生产承诺源[物料/分包归集缺失 P1-RC-049]）。**与 A1.34 projects-F1 衔接**：P1-RC-051 报销路径修复与 P1-RC-050 ON_HOLD 费用门控修复**一次性协同闭合**（同站点 ExpenseCostAggregator.refreshExpenseCost，不同控制点——状态门控 vs 预算检查）；P1-RC-051 采购路径 + P2-RC-049 承诺项**依赖 P1-RC-049 物料归集 successor 先落地**（合并修复触发条件）。
