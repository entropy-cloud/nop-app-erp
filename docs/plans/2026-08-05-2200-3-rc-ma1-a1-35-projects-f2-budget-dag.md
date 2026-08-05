# 2026-08-05-2200-3 rc-ma1-a1-35-projects-f2-budget-dag projects 域 projects-F2 预算与 DAG 需求符合性审计

> Plan Status: active
> Last Reviewed: 2026-08-05
> Mission: requirement-compliance
> Work Item: A1.35（MA1 需求追踪矩阵审计 — projects-F2 项目预算 STRICT 超支拦截 + 任务依赖 DAG 成环校验）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A1.35
> Related: `docs/plans/2026-08-02-1458-1-requirement-compliance-methodology.md`（M0.1 done）、`2026-08-02-1530-1-requirement-baseline-extraction.md`（M0.2 done，解除 A1.35 的 0.2 依赖）、`2026-08-05-2200-2-rc-ma1-a1-34-projects-f1-startup-cost-collection.md`（projects 域同批 N=2，F1 成本归集为预算检查的归集前置——预算余量依赖已归集成本）
> Audit: required

## Current Baseline

> 本计划是**审计工作项**（verification or audit work），结果表面 = 一份审计报告。基线盘点的是被审功能的现状代码/测试/既有证据，**不修改任何代码**。

- **方法论契约 + UC 锚点已就绪**：`docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议 + §4 三判据）已落盘；`docs/audits/rc-requirement-baseline-inventory.md` 已为 A1.35 给出 UC 清单 = `UC-PRJ-04/05`（2 UC），含 `use-cases.md:65/81` 锚点，覆盖率 `✅ 一致`（无基线分歧 D-xx）。本切片为 projects 域第二个 RC 切片（projects 域共 3 切片 A1.34/A1.35/A1.36）。

- **L1 需求契约（权威真相源）**：`docs/design/projects/use-cases.md`（机制见 `cost-collection.md §三`、`state-machine.md §任务`、`task-dag.md`）：
  - **UC-PRJ-04 项目预算 STRICT 超支拦截**（`:65`）：检查时机: **工时提交 / 采购审核 / 报销审核**(标注项目时)；预算余量 = 项目预算 - 已归集成本 - 已承诺成本；STRICT 模式: 余量 < 0 → 拒绝该笔归集；WARNING 模式: 警告但放行。
  - **UC-PRJ-05 任务依赖 DAG 成环校验**（`:81`）：任务.dependsOn 前置任务；前置任务.状态 != DONE → 本任务不可 IN_PROGRESS；依赖关系 DAG 成环 → 校验失败(拒绝建立环依赖)。
  - **L1 关键不变量**：① 预算检查**三时机**（工时提交 + 采购审核 + 报销审核）；② STRICT 拒绝 / WARNING 放行 双模式；③ 预算余量含已承诺成本；④ 前置未完成不可开始；⑤ DAG 成环校验失败。

- **L3 代码实现现状（实测）**——DAG 成环校验完整实现，预算检查**仅工时路径**（采购/报销时机缺失）：
  - **UC-PRJ-04 预算 STRICT 超支拦截（⚠️ 仅工时路径，采购/报销时机缺失）**：
    - BudgetChecker：`module-projects/erp-prj-service/.../cost/BudgetChecker.java:34 check(projectId, costAmount)`——计算预算余量并按 config 模式 STRICT 抛错/WARNING LOG。
    - **检查时机**：唯一调用点 `ErpPrjTimesheetSubmitProcessor.runBudgetCheckHook:103-104` → `budgetChecker.check(timesheet.getProjectId(), costAmount)`（在 submit `:58` 触发）。**grep 全 `module-projects` `BudgetChecker|runBudgetCheck|budgetCheck` 仅此 2 文件命中**——采购审核路径 + 报销审核路径**零预算检查调用**。
    - config 模式：`ErpPrjConfigs.budgetControlMode()`（`DEFAULT_BUDGET_CONTROL_MODE = BUDGET_MODE_WARNING` `ErpPrjConfigs:9-10/33-40`，**默认 WARNING**）；STRICT 抛 `ERR_PROJECT_BUDGET_EXCEEDED`。
    - **⚠️ 余量公式缺口**：`BudgetChecker.check` 余量判定为 `used(=Σ 全部 CostCollectionLine.amount, 即已归集成本) + addAmount > total`——**无"已承诺成本/commitAmount"项**（BudgetChecker 全文零 commitment 概念），L1"预算余量 = 项目预算 - 已归集成本 - 已承诺成本"仅 2/3 实现。
    - **⚠️ 关键缺口**：L1 要求"检查时机: 工时提交 / 采购审核 / 报销审核(标注项目时)"三时机，实现**仅工时提交 1 时机**——采购审核 + 报销审核两时机**完全缺失**。（注：与 A1.34 衔接——采购入库→项目物料归基本身未实现，故采购路径预算检查的"被检查对象"尚不存在；但报销→项目费用归集已实现（ExpenseCostAggregator），报销审核预算检查缺失是独立缺口。）
  - **UC-PRJ-05 任务依赖 DAG 成环校验（✅ 完整实现 + 强测）**：
    - DAG 成环检测：`TaskDependencyValidator.detectCycle:44-86`（owner doc task-dag.md §2.1 算法 1:1）：①dependsOnId null→放行；②taskId==dependsOnId→`ERR_TASK_SELF_DEPENDENCY`（自环优先）；③visited HashSet + chainOrder + cursor 上行链；④depth++ > maxDepth→`ERR_TASK_DEPENDENCY_DEPTH_EXCEEDED`；⑤cursor∈visited→`ERR_TASK_DEPENDENCY_CYCLE`（chainOrder 序列）。O(N) 时间 + O(N) 空间 + maxDepth 兜底。
    - 跨项目依赖校验：`ErpPrjTaskBizModel.validateDependency:88-94`（`task.projectId == dependsOnTask.projectId` 否则 `ERR_TASK_DEPENDENCY_CROSS_PROJECT`）。
    - 依赖保存钩子双触发：`defaultPrepareSave:56-59`（插入）+ `defaultPrepareUpdate:62-65`（修改）均调 validateDependency。
    - 前置未完成不可开始：`ErpPrjTaskBizModel.startTask:104-115` + `validatePredecessorDone:172-193`（config-gated `erp-prj.task-strict-predecessor-check` 默认 true：STRICT 抛 `ERR_TASK_PREDECESSOR_NOT_DONE` / WARN LOG.warn 放行）。
    - 任务状态机：startTask(TODO→IN_PROGRESS)/completeTask(IN_PROGRESS→DONE)/blockTask(IN_PROGRESS→BLOCKED, blockReason 必填 `ERR_TASK_BLOCK_REASON_REQUIRED`)/unblockTask(BLOCKED→IN_PROGRESS)；非法迁移抛 `ERR_TASK_ILLEGAL_STATUS_TRANSITION`。
    - config 点：`ErpPrjConfigs.taskDependencyMaxDepth + taskStrictPredecessorCheck`（task-dag.md §6）。
  - **跨域 daoFor**：projects 跨域只读归 P1-MA1-022 todo MR1，本切片不重审（BudgetChecker/TaskDependencyValidator 均不跨域）。

- **L4 测试证据现状**（`module-projects/erp-prj-service/src/test/java/`）：
  - **强（DAG）**：`TestErpPrjTaskDependency.java`（319 行，多 @Test：DAG 成环拒绝 + 自环优先 + 跨项目拒绝 + 深度超限拒绝 + STRICT 前置未完成拦截 + WARN 放行 + 非法迁移守卫 + blockReason 必填）——owner doc task-dag.md §2.1 算法逐路径覆盖。
  - **强（预算-工时路径）**：`TestErpPrjBudgetAndCollection.java`（7 @Test：WARNING 放行 `:70-92` / STRICT 抛错 `:95-119` / approve 产 LABOR 行 + actualCost 回写 / 幂等 / closeProject 冻结 / 拒非 OPEN / requireReferenceable 拒 CANCELLED）——**覆盖工时提交预算检查**。
  - **E2E**：`tests/e2e/business-actions/projects-task.action.spec.ts`（强：TODO→IN_PROGRESS→DONE + block/unblock + 前置守卫）。
  - **⚠️ 测试缺口**：① UC-PRJ-04 采购审核 / 报销审核预算检查**零测试**（与功能缺失一致）；② 预算余量**不含"已承诺成本"项**（BudgetChecker.check 仅 used+addAmount>total）——已确认，无对应负向测试。

- **L5 既有证据（MA2 复用输入）**：
  - `docs/audits/2026-07-28-1020-arm-ma2-projects-state-machine.md`（A2.13）：任务 4 态状态机 + DAG 成环检测经实仓确认（`TaskDependencyValidator.detectCycle:44-86` 与 task-dag.md §2.1 1:1 对应，`:110-141`）；任务可达性/异常路径（自环/跨项目/深度/前置 config-gated）全 PASS（`:196-200`）；**P1-MA2-069**（Milestone dict 死状态）resolved。
  - `docs/plans/2026-07-07-0930-3-projects-task-dependency-dag-cycle-validation.md`（done）：DAG 成环校验首例落地 plan（TaskDependencyValidator 实现 + 7 ErrorCode）。
  - **本切片须声明与 MA2 报告差异增量**（报告段落 9）：复用 A2.13 任务状态机 + DAG 成环检测已证实行为，只补需求视角差异（预算检查仅工时路径 / 采购+报销时机缺失 / 余量"已承诺成本"项 / WARN 默认值）。

- **arm-index 既有 finding 衔接**：相关既有 finding：`P1-MA2-069`（dict 死状态 resolved）、`P1-MA1-010`（多币种 propId todo MR1）、`P1-MA1-022`（跨域只读 todo MR1）。**RC 系列对 projects 预算/DAG 为零**（本切片为 projects 域首批 RC 切片之一）。本切片须 grep arm-index prj budget/check/strict/warning/commitment/predecessor/dag/cycle 同域同控制点后裁决。

- **保护区域**：本审计为**只读审计**。属 roadmap 预授权类目。发现的 P0/P1 finding **不在本计划实施修复**——按 §10，P0 经 MR0、P1 经 MR1。本切片候选偏差（预算检查时机缺失）属**代码逻辑**类（预授权——在采购/报销 Processor 加 budgetChecker.check 调用）。

- **剩余差距**：A1.35 切片五级追踪审计报告缺失 = MA4 及 MR1 的该切片证据缺口来源。本计划产出 A1.35 报告并登记 finding，解除 projects 域预算/DAG 切片证据缺口。

## Goals

- 产出 A1.35 切片审计报告 `docs/audits/<执行时间戳>-rc-ma1-a1-35-projects-f2-budget-dag.md`，含方法论 §6 **9 段全部内容**。
- 对 UC-PRJ-04/05 逐条核验**每条验收标准**（完整枚举，§3）：预算三时机/余量公式（含已承诺成本）/STRICT-WARNING 双模式、前置未完成不可开始/DAG 成环校验 全链逐条。
- 对候选缺口给出分级结论：①UC-PRJ-04 预算检查**仅工时提交**（采购审核 + 报销审核两时机缺失，倾向 **P1/P2**——须 §4 三判据复核 cost-collection.md §三 是否显式 documented simplification Deferred + 人工批准；报销路径费用归集已实现故报销审核预算检查缺失是独立缺口；采购路径因物料归集未实现[见 A1.34]被检查对象尚不存在）；②UC-PRJ-04 余量公式缺"已承诺成本"项（BudgetChecker.check 仅 used+addAmount>total，倾向 **P2** 次要验收标准弱）；③UC-PRJ-05 DAG 成环 + 前置守卫 → 倾向**接受**（完整实现 + 强测）——按 §2 判据定级，若为 P0/P1 则新建 `P*-RC-xxx`（与既有 RC 系列协调，最新 P2-RC-043/P1-RC-041）并按 §10 触发 MR1（本计划仅登记，不实施修复）。
- 报告产出即更新 `docs/audits/arm-index.md`（新 RC finding 入对应分区；新 audit reports 表行）。

## Non-Goals

- **不修复 finding**（修复属 MR0/MR1；本计划是审计）。
- **不修改真相源**（§9 冻结条款——分歧记入报告，不直改 use-cases.md/cost-collection.md/task-dag.md/product-scope.md）。
- **不修改代码/ORM/api.xml**（只读审计）。
- **不审计其他 MA1 切片**（A1.34 projects-F1 立项/成本归集 / A1.36 projects-F3 结算/看板 独立 plan；A1.35 只覆盖 UC-PRJ-04/05）。
- **不复审项目立项/工时成本/多来源归集/暂停约束**（UC-PRJ-01/02/03/09 属 A1.34；本切片仅核预算检查 + DAG）。
- **不重审 P1-MA2-069 任务状态机/DAG 行为**（§去重协议：resolved，只补需求视角差异[预算检查时机]）。
- **不执行 MA4 运行时探针展开**（本计划只产出静态存疑点清单）。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议 + §4 三判据）+ `docs/backlog/requirement-compliance-roadmap.md`（A1.35 工作项）+ `docs/audits/rc-requirement-baseline-inventory.md`（A1.35 UC 锚点）+ `docs/design/projects/use-cases.md`（L1 真相源）+ `docs/design/projects/cost-collection.md §三` + `state-machine.md §任务` + `task-dag.md`（L2 设计参考，非真相源）+ `docs/audits/arm-index.md`（finding 衔接）+ 上述 A2.13 报告（L5 既有证据）
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA1 全部 A1.x 指定）。其必需输入均已就绪。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 审计以读代码/测试/报告为主（纯分析）。L5 行为证据复用既有 MA2 报告 + 单测；若需即时行为确认可跑既有 JUnit（如 `mvn test -pl module-projects/erp-prj-service -Dtest=TestErpPrjTaskDependency,TestErpPrjBudgetAndCollection`）。§8 过程纪律自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter，退出码恒 0；本审计无生产代码变更）。

## Execution Plan

### Phase 1 - 五级追踪矩阵填充与逐 UC 符合性结论

Status: planned
Targets: `docs/audits/<执行时间戳>-rc-ma1-a1-35-projects-f2-budget-dag.md`（产出 §1-§5）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: M0.1 + M0.2 done

- [ ] `Proof` 对 UC-PRJ-04/05 **逐验收标准一矩阵行**填 L1-L5（§1 格式）：L1 逐字引用 `use-cases.md:65/81` 验收标准原文；L2 引用 `cost-collection.md §三` + `state-machine.md §任务` + `task-dag.md §2/§4/§6/§7`（标注"设计参考，冲突以 L1 为准"）；L3 引用 `BudgetChecker`/`ErpPrjTimesheetSubmitProcessor`/`ErpPrjTaskBizModel`/`TaskDependencyValidator`/`ErpPrjConfigs`（含行号）；L4 引用 `TestErpPrjTaskDependency`/`TestErpPrjBudgetAndCollection`#method（注明断言强度）；L5 复用 A2.13（任务状态机/DAG PASS + P1-MA2-069 resolved）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Proof` 重点核验**候选缺口**（逐条验收标准对照）：UC-PRJ-04 ①**检查时机**（工时提交 ✅ `runBudgetCheckHook:103-104`；**采购审核 ❌ 零调用**；**报销审核 ❌ 零调用**——grep 全 module-projects BudgetChecker 仅 TimesheetSubmitProcessor 1 调用点）②**预算余量公式缺"已承诺成本"项**（BudgetChecker.check 余量 = used(Σ CostCollectionLine.amount) + addAmount > total，**零 commitment 概念**，L1 三项式仅 2/3 实现 → 倾向 **P2** 次要验收标准弱）③STRICT 拒绝 / WARNING 放行（config `ErpPrjConfigs.budgetControlMode`，**默认 WARNING** `ErpPrjConfigs:9-10`）；UC-PRJ-05 ①dependsOn 前置（`validatePredecessorDone:172-193` config-gated STRICT 抛 `ERR_TASK_PREDECESSOR_NOT_DONE`/WARN ✅）②前置未 DONE 不可 IN_PROGRESS（startTask:104-115 ✅）③DAG 成环校验失败（`TaskDependencyValidator.detectCycle:44-86` 自环优先 + 上行链 HashSet + maxDepth ✅，ERR_TASK_SELF_DEPENDENCY/CYCLE/DEPTH_EXCEEDED/CROSS_PROJECT 全落地）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Decision` 按 §2 判据对 2 UC 给出符合性结论（取最高）：UC-PRJ-05 DAG 成环 + 前置守卫 → 倾向**接受**（完整实现 + 319 行强测 + A2.13 PASS）；UC-PRJ-04 预算检查 → 工时时机**接受** + 采购/报销时机缺失倾向 **P1/P2**（§4 三判据复核 cost-collection.md §三 是否显式 documented simplification Deferred + 人工批准：报销路径归集已实现故报销审核预算检查缺失是独立验收标准未实现；采购路径因物料归集未实现[A1.34]被检查对象尚不存在→可降级或与物料归集 successor 合并）+ 余量缺"已承诺成本"项倾向 **P2**（次要验收标准弱，主路径已归集成本拦截可用）+ 默认 WARNING 已确认。每结论须列明命中判据编号 + 三源对照 + §4 三判据复核（P1 项核 plan-audit/owner doc documented simplification/product-scope 裁剪 + 人工批准痕迹）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

- [ ] 报告 §1-§5 已落盘：UC-PRJ-04/05 矩阵行（逐验收标准进入 L5 判读），L1 逐字引用、L3 含行号、L4 注明断言强度、L5 标注复用 A2.13 来源
- [ ] 2 UC 各有符合性结论（P0/P1/P2/接受）且列明 §2 判据编号；候选缺口 ①-③ 有明确分级；预算检查时机缺失有 §4 三判据复核路径；余量"已承诺成本"项缺失（P2）+ 默认 WARNING 已确认

### Phase 2 - finding 登记 / arm-index 衔接 / 静态存疑点 / 过程纪律自检 / 报告完整性

Status: planned
Targets: `docs/audits/<执行时间戳>-rc-ma1-a1-35-projects-f2-budget-dag.md`（补 §6-§9）；`docs/audits/arm-index.md`（新 RC finding 入分区）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1 完成

- [ ] `Decision` **复用 or 新增 裁决**（§7）：grep `arm-index.md` prj budget/check/strict/warning/commitment/predecessor/dag/cycle 同域同控制点后裁决——DAG 成环 + 前置守卫**复用** A2.13 已证实（不新建）；预算检查时机缺失为**新根因**（既有 arm-index 无 RC finding 涉及 projects 预算检查时机）→ 若确认 P1 则新建 `P*-RC-xxx`（与既有 RC 系列协调，最新 P2-RC-043/P1-RC-041）列明差异依据。禁止未经比对新建。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Add` 报告 §6 与 arm-index 衔接段：列明每条 finding 复用/新增裁决 + 双向可追溯（finding ID ↔ 修复行预留 MR1）。
      - Skill: none
- [ ] `Add` 报告 §7 静态存疑点清单（供 MA4 展开）：登记 L5 无法静态定论、需运行时确认的点（如 STRICT 模式下工时提交超支的实际拒绝运行时行为 / 采购路径在物料归集 successor 实现后预算检查的衔接条件 / 报销审核补加预算检查后 ON_HOLD/COMPLETED 项目报销提交的实际拦截行为 等；每存疑点一行；无则注明"无"）。**P0 即时通道**：若 Phase 1 定级出 P0，按 §10 登记 + 本计划记录"已触发 MR0 追加 R0.n"（不实施修复）。
      - Skill: none
- [ ] `Proof` 报告 §8 过程纪律自检段：实际运行 `bash docs/audits/nop-compliance-checker.sh` 附 actual vs baseline 表（无生产代码变更，注明"无回归风险"）；closure-audit 独立性声明；与 arm-index 交叉去重声明。**不以 checker 退出码 0 为门控通过依据**。
      - Skill: none
- [ ] `Add` 报告 §9 与 MA2 报告差异增量声明：复用 `2026-07-28-1020-arm-ma2-projects-state-machine.md`（A2.13 任务状态机/DAG 成环 PASS + P1-MA2-069 resolved），列明只补的需求视角差异（预算检查仅工时路径 / 采购+报销时机缺失 / 余量已承诺成本项 / WARN 默认值）。
      - Skill: none
- [ ] `Add` 报告产出即更新 `docs/audits/arm-index.md`：新 `P*-RC-xxx` 入对应分区；audit reports 表新增 A1.35 行。
      - Skill: none
- [ ] `Proof` 报告 9 段完整性自检：落盘前自查 §1-§9 全部存在。
      - Skill: none

Exit Criteria:

- [ ] 报告 §6-§9 已落盘，9 段齐全；finding 复用/新增裁决均有 arm-index grep 依据
- [ ] 新 RC finding（若有）已写入 `arm-index.md`；静态存疑点清单已登记（供 A4.1/A4.2 展开）
- [ ] §8 自检段含 checker actual vs baseline 实测表 + 独立性 + 交叉去重声明

## Draft Review Record

- Independent draft review iteration 1: `acceptable as-is`（独立子代理 ses_030950687ffef7vOhORRRgkCqJ，fresh session，未起草本计划）。全部 load-bearing 引用经实仓复核 CONFIRMED TRUE：①BudgetChecker 单调用点（grep `module-projects` `BudgetChecker|budgetChecker|runBudgetCheck|budgetCheck` 仅 `BudgetChecker.java` 定义 + `ErpPrjTimesheetSubmitProcessor:104` 单调用 + beans.xml:30 bean def，采购/报销 Processor 零调用）——核心预算检查时机缺口真实；②`TaskDependencyValidator.detectCycle:44-86` DAG 算法 1:1（dependsOnId null→return / taskId==dependsOnId→ERR_TASK_SELF_DEPENDENCY 自环优先 / visited HashSet+chainOrder+cursor 上行链 / depth++>maxDepth→ERR_TASK_DEPENDENCY_DEPTH_EXCEEDED / cursor∈visited→ERR_TASK_DEPENDENCY_CYCLE）；③validatePredecessorDone config-gated STRICT/WARN 行为匹配（亚行精度 :168-189 → 实际 :172-193，已据此修订）。reviewer 预核验确认两项"待核"倾向正确：④BudgetChecker 余量 = used(Σ CostCollectionLine.amount)+addAmount>total，**零 commitment 概念**（已据此将 baseline/Goals/Phase1/ExitCriteria/§7 全部从"待核"升级为确认 P2）；⑤`ErpPrjConfigs:9-10` DEFAULT_BUDGET_CONTROL_MODE=BUDGET_MODE_WARNING（默认 WARNING 已确认）。scope（UC-PRJ-04/05 only，无 A1.34/A1.36 creep）、anti-slack、exit criteria、methodology §1-§9 + A2.13 reuse + §4 三判据（cost-collection.md §三 Deferred+人工批准）+ 跨计划连贯性（采购预算检查依赖物料归集 successor 正确框定为降级/合并理由非 scope creep）全对齐。共识达成，转 active。

## Closure Gates

> 本计划为**只读审计**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控。验证 = 报告 9 段完整性 + 五级矩阵逐验收标准覆盖 + finding arm-index 衔接 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [ ] 范围内行为完成：A1.35 报告 9 段齐全 + UC-PRJ-04/05 矩阵行（逐验收标准）+ finding 登记入 arm-index
- [ ] 相关文档对齐：报告与方法论 §1-§10 + §去重协议一致；与 rc-requirement-baseline-inventory A1.35 锚点一致
- [ ] 已运行验证：报告 9 段完整性自检 + §8 checker actual vs baseline 实测记录 + finding 复用/新增裁决可追溯（无代码变更故不跑 build/test）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、退出标准、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### finding 的修复实施

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划是审计，结果表面 = 报告 + arm-index 登记。finding 的修复按 §10 经 MR0（P0）/ MR1（R1.0 展开 RC-R1.n，P1）实施；本切片候选偏差（预算检查采购/报销时机缺失）属**代码逻辑**类（预授权——在采购/报销 Processor 加 budgetChecker.check 调用）；采购路径预算检查依赖物料归集 successor（A1.34 finding）先落地。
- Successor Required: yes（MR0/MR1 按本报告 finding 交叉引用展开修复行；采购路径预算检查 = 物料归集 successor 的下游）

## Closure

Status Note: <待执行 + 独立结束审计后填写>

Closure Audit Evidence:

- Auditor / Agent: <待独立结束审计子代理>
- Evidence: <待填>
