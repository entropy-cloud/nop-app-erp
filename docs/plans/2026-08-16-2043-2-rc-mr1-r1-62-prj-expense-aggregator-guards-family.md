# 2026-08-16-2043-2-rc-mr1-r1-62-prj-expense-aggregator-guards-family RC-R1.62 — projects 报销归集状态门控 + 预算检查族（P1-RC-050 + P1-RC-051 同站点 ExpenseCostAggregator）

> Plan Status: active
> Last Reviewed: 2026-08-16
> Mission: requirement-compliance
> Work Item: RC-R1.62（P1-RC-050 + P1-RC-051，UC-PRJ-09 AC-① + UC-PRJ-04 AC-①）
> Source: `docs/backlog/requirement-compliance-roadmap.md` §MR1 RC-R1.62 行 + `docs/audits/arm-index.md` P1-RC-050 行（:221）+ P1-RC-051 行（:223）+ 展开器 `docs/audits/2026-08-07-1910-rc-mr1-r1-0-expander.md`（G9 projects 归集族：同根因同控制点 ExpenseCostAggregator.refreshExpenseCost 同站点）
> Related: `docs/design/projects/use-cases.md`（L1 UC-PRJ-09 :153 + UC-PRJ-04 :71-74）；`docs/design/projects/state-machine.md`（§1:15 + §4:53）；`docs/design/projects/cost-collection.md`（§3.3 预算检查时机 + §4.1）；`docs/audits/2026-08-05-2200-2-rc-ma1-a1-34-projects-f1-startup-cost-collection.md`（A1.34 P1-RC-050 :219-223）；`docs/audits/2026-08-05-2200-3-rc-ma1-a1-35-projects-f2-budget-dag.md`（A1.35 P1-RC-051）；`docs/audits/2026-08-07-2359-rc-ma4-a4-2-113-123-projects-f1-f2-f3-runtime.md`（A4.2.113）
> Audit: required

## Current Baseline

- **finding P1-RC-050（arm-index:221，UC-PRJ-09 AC-①）**：L1（`use-cases.md:153`）逐字「OPEN → ON_HOLD(暂停): 拒绝新费用归集(工时/采购/报销)」——ON_HOLD/COMPLETED/CANCELLED 项目必须拒绝新费用归集。L3 实仓（HEAD 核查）：`ExpenseCostAggregator.refreshExpenseCost:60-64`（`module-projects/erp-prj-service/.../cost/ExpenseCostAggregator.java`）仅查 `project==null`（缺失抛 `ERR_PROJECT_NOT_REFERENCEABLE`），**从不校验项目状态**——ON_HOLD/COMPLETED/CANCELLED 项目的报销行仍被归集。owner doc `state-machine.md §1:15` + `§4:53` 明确「暂停项目拒绝新费用归集」**未声明 Deferred**。
- **finding P1-RC-051（arm-index:223，UC-PRJ-04 AC-①）**：L1（`use-cases.md:71`）逐字「检查时机: 工时提交 / 采购审核 / 报销审核(标注项目时)」——三时机。L3 实仓：projects `BudgetChecker.check(projectId, addAmount)`（`BudgetChecker.java:44-69`）**唯一调用点** = `ErpPrjTimesheetSubmitProcessor.runBudgetCheckHook:107-108`（`budgetChecker.check(timesheet.getProjectId(), costAmount)`）——报销审核路径零 projects BudgetChecker 调用（finance `ErpFinExpenseClaimProcessor.runBudgetCheckHook` 调 finance `IErpFinBudgetControlBiz` 是财务预算，不同控制点）。
- **A4.2.113 运行时确认（2026-08-07，维持 P1 双缺口）**：`refreshExpenseCost:60-64` 仅查 project==null 不校验状态 + 全方法体 :66-126 **无 budgetChecker.check 调用**（同站点双缺口确认活跃），违规归集累积 GL 不破坏。
- **同站点修复协同（G9 归集族，expander 合并先例）**：P1-RC-050（状态门控）+ P1-RC-051（报销路径预算检查）同站点 `ExpenseCostAggregator.refreshExpenseCost`，**一次性协同修复**（arm-index 两 finding 修复注记 + expander G9 行「同根因同控制点…plan 明确合并先例」）。
- **P2-RC-048 协同闭合（arm-index:222）**：`ErpPrjProjectBizModel.requireReferenceable:65-76`（仅 OPEN 通过，DRAFT/ON_HOLD/COMPLETED/CANCELLED 全拒 `ERR_PROJECT_NOT_REFERENCEABLE`）实现正确但生产代码零调用方——**P1-RC-050 修复时若改调 requireReferenceable Facade 自动闭合本 finding**（费用路径 b 分支）。
- **2026-08-12 批量裁决（roadmap 头 :49 B 类清单）**：**RC-R1.62（ExpenseCostAggregator 加状态守卫 + budgetChecker.check）在 B 类**——降级为预授权自动执行，不再须 ask-first checkbox（roadmap 行旧「越界项…双独立子 agent 批准 checkbox」字样按 B 类裁决执行期改写消除歧义，对齐 RC-R1.48/50/52-54/59 先例）。
- **A4.2.113 备注冲突消解**：A4.2.113 运行时报告称「修复归 MR1 触 ExpenseCostAggregator 业财过账路径须 ask-first」——**2026-08-12 用户批量裁决 B 类清单显式收录 RC-R1.62（ExpenseCostAggregator 加状态守卫 + budgetChecker.check）**，裁决（用户级）覆盖 A4.2 备注（审计级），按 B 类预授权执行（对齐 RC-R1.50/52-54 会计类同型先例：B 类裁决覆盖运行时报告 ask-first 备注）。
- **预算检查语义**：`BudgetChecker.check:44-69`——projectId null/预算空/非正 → 静默返回；`used=sumUsedAmount(projectId)`（归集行金额 Σ）；`used+addAmount>total` 时 STRICT（`ErpPrjConfigs.budgetControlStrict()`）抛 `ERR_BUDGET_EXCEEDED`（4 param）/ WARNING LOG.warn 放行。**在归集行写入前调用**（对齐工时路径 submit 时序 :107-108 于 updateEntity 前）。
- **状态守卫语义**：`ErpPrjProjectBizModel.requireReferenceable:65-76` 仅 OPEN 通过；复用 Facade 统一咽喉（消除 ExpenseCostAggregator 内联重复 + 闭合 P2-RC-048）。
- **测试基线**：`TestErpPrjExpenseAggregation` 4 组（聚合/幂等/config 关闭/closeProject 刷新）+ `TestErpPrjBudgetAndCollection` 7 组——**无 ON_HOLD 拒绝测试、无报销路径 STRICT 拒绝测试**。erp-prj-service **138 tests 全绿**（R1.27 基线）。
- **compliance 基线**：R2c=1422 / R2b=235 / R2d=35；修复经既有 `expenseClaimBiz`/`daoProvider` 站点 + `IErpPrjProjectBiz` Facade 注入（projects 域内 IBiz，零新增 daoFor 面预期）——结束前复跑 checker 确认零漂移。

## Goals

- **UC-PRJ-09 AC-① 运行时成立（P1-RC-050 核心）**：`ExpenseCostAggregator.refreshExpenseCost` 在归集行写入前校验项目状态——仅 OPEN 通过；ON_HOLD/COMPLETED/CANCELLED **拒绝归集**（抛 `ERR_PROJECT_NOT_REFERENCEABLE`，与 requireReferenceable 单一咽喉语义一致）。
- **UC-PRJ-04 报销审核时机运行时成立（P1-RC-051 报销路径）**：`refreshExpenseCost` 在归集行写入前调 `budgetChecker.check(projectId, 本次归集金额)`——STRICT 超预算抛 `ERR_BUDGET_EXCEEDED` 拒绝（@BizMutation 事务回滚零落库）/ WARNING 放行；采购审核时机归 RC-R1.61（物料归集 successor 协同，见 Related + Deferred But Adjudicated）。
- **P2-RC-048 协同闭合**：费用路径改调 `IErpPrjProjectBiz.requireReferenceable` Facade（消除内联重复、统一咽喉）——P2-RC-048 finding 修复注记「P1-RC-050 修复时若改调 requireReferenceable 自动闭合本 finding」。
- **守卫时序**：状态守卫 + 预算检查均在**归集行写入前**（对齐 `BudgetChecker.check` 工时路径 submit 前置时序 + `refreshExpenseCost` 第一遍 pending 收集前），守卫失败 → @BizMutation 事务回滚零副作用。
- **测试**：新增①ON_HOLD 项目报销行归集拒绝（含错误码 + 零落库 + 既有归集保留）；②COMPLETED/CANCELLED 拒绝；③OPEN 放行回归；④STRICT 超预算报销拒绝（ERR_BUDGET_EXCEEDED + 零落库）；⑤WARNING 放行；⑥P2-RC-048 闭合断言（requireReferenceable 被消费）；既有 138 tests 零回归。
- **零回归**：erp-prj-service 全量测试全绿 + 全量 `mvn test` + `mvn clean install -DskipTests` + compliance checker 零漂移。
- **owner doc 收敛**：`state-machine.md §1/§4` 补状态门控实现注记 + `cost-collection.md §3.3` 报销时机已实现注记 + `§7` requireReferenceable 消费注记；arm-index P1-RC-050 + P1-RC-051 + P2-RC-048 → 更新 + roadmap 行 done + logs 条目。

## Non-Goals

- **不实现采购审核时机预算检查**（P1-RC-051 采购路径 = 物料归集 successor 的下游，归 RC-R1.61 协同，触发条件=物料归集落地；见 Deferred But Adjudicated）。
- **不实现 P2-RC-049（预算余量公式 commitment 第三项）**（watch-only P2 登记项：projects 域无生产承诺源，与 RC-R1.61 物料归集落地后协同，非本行范围）。
- **不重写 refreshExpenseCost 归集主链**（仅增状态守卫 + 预算检查前置调用，不改幂等/聚合/回写逻辑）。
- **不新增 config key**（状态守卫与预算检查是 L1 硬契约，不加 config 门控，对齐 A4.1.4 config-gate 范式仅适用可配置行为；A4.2.118 STRICT 行为既有 config-gated）。
- **不改真相源契约段落**（use-cases L1 不动；state-machine.md 契约段不动，仅补实现注记）。

## Task Route

- Type: `implementation-only change`（P1 需求分歧修复：纯 BizModel 代码逻辑预授权，2026-08-12 B 类裁决降级；Q4=(a) 强制实现禁止方案 B；与 P1-RC-051 同站点协同 + P2-RC-048 协同闭合）
- Owner Docs: `docs/design/projects/use-cases.md`（L1 UC-PRJ-09/04）+ `docs/design/projects/state-machine.md`（§1/§4/§7）+ `docs/design/projects/cost-collection.md`（§3.3）
- Skill Selection Basis: 跨实体 Facade 统一咽喉 + 守卫接线（`nop-backend-dev`：IBiz 注入 + 事务回滚语义 + 幂等范式）；测试（`nop-testing`：JunitBaseTestCase 直断言 + GraphQL 集成范式）。

## Infrastructure And Config Prereqs

- 无新 config key/ORM 变更/外部服务。
- 分域验证前置：`mvn test -pl module-projects/erp-prj-service`。

## Execution Plan

### Phase 1 - 守卫接入裁决（Decision）

Status: planned
Targets: `module-projects/erp-prj-service/.../cost/ExpenseCostAggregator.java`、`module-projects/erp-prj-service/.../entity/ErpPrjProjectBizModel.java`（只读）、`docs/design/projects/state-machine.md`
Skill: `nop-backend-dev`
Item Types: `Decision`

- Prereqs: 无

- [ ] Decision：状态守卫载体 = 复用 `IErpPrjProjectBiz.requireReferenceable` Facade（记录选择：统一咽喉语义 + P2-RC-048 协同闭合；备选：ExpenseCostAggregator 内联 status 校验——否决因重复实现 + 不闭合 P2-RC-048；残留风险：Facade @BizMutation 在聚合器内部调用的事务语义——须确认同事务内调用无 REQUIRES_NEW 分裂）。
      - Skill: `nop-backend-dev`
- [ ] Decision：预算检查时序 = 归集行写入前（第一遍 pending 收集后、写入前；对齐 `ErpPrjTimesheetSubmitProcessor.runBudgetCheckHook:107-108` 时序）——记录 addAmount 口径（本次新增归集金额 Σ，非累计）。
      - Skill: `nop-backend-dev`
- [ ] Decision：守卫失败语义 = 抛 NopException（ERR_PROJECT_NOT_REFERENCEABLE / ERR_BUDGET_EXCEEDED）→ 调用方 @BizMutation 事务回滚（对齐 A4.2.118 STRICT 抛错回滚范式）；零新 ErrorCode（复用既有）。**设计后果记录**：`closeProject` 触发 `refreshExpenseCost`（`ErpPrjProjectCloseProjectProcessor:69`，归集发生于项目仍 OPEN 时）——若超预算报销行待归集，STRICT 模式将使 closeProject 抛 ERR_BUDGET_EXCEEDED（WARNING 默认放行）；该后果在 UC-PRJ-04 预算检查时机语义下可接受（关闭前预算拦截 = 防御性校验），作为残留风险登记。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] 三裁决落地（计划内记录选择/备选/残留风险；Facade 事务语义经实仓核实）

### Phase 2 - 守卫实现（Add）

Status: planned
Targets: `module-projects/erp-prj-service/.../cost/ExpenseCostAggregator.java`
Skill: `nop-backend-dev`
Item Types: `Add`

- Prereqs: Phase 1

- [ ] Add：`refreshExpenseCost` 注入 `IErpPrjProjectBiz`（或经既有注入面）→ 归集行写入前调 `requireReferenceable(projectId)`（状态守卫，非 OPEN 抛 ERR_PROJECT_NOT_REFERENCEABLE）。
      - Skill: `nop-backend-dev`
- [ ] Add：同处接入 `budgetChecker.check(projectId, 本次新增归集金额)`（预算检查，STRICT 抛 ERR_BUDGET_EXCEEDED / WARNING 放行）——注入 `BudgetChecker` bean（app-service.beans.xml:43-44 已注册；`ErpPrjConfigs` 仅提供 `budgetControlStrict()` config 读取，不替代 check() 调用）。
      - Skill: `nop-backend-dev`
- [ ] Add：守卫顺序确定化（状态守卫先于预算检查，对齐工时路径 submit 顺序）；javadoc 同步（§实现约定 状态门控 + 预算检查注记）。

Exit Criteria:

- [ ] ON_HOLD/COMPLETED/CANCELLED 项目报销归集被拒（可复现：Phase 3 测试或 GraphQL 冒烟）；OPEN 项目零回归；STRICT/WARNING 双分支可验证

### Phase 3 - 测试 + 文档 + 回填（Add | Proof）

Status: planned
Targets: `module-projects/erp-prj-service/src/test/`、`docs/design/projects/state-machine.md`、`docs/design/projects/cost-collection.md`、`docs/audits/arm-index.md`、`docs/backlog/requirement-compliance-roadmap.md`、`docs/logs/2026/08-16.md`
Skill: `nop-testing`
Item Types: `Add | Proof`

- Prereqs: Phase 2

- [ ] Proof：新增 `TestErpPrjExpenseAggregation` 扩展或新测试类——①ON_HOLD 拒绝（错误码 + 零落库 + 既有归集保留）；②COMPLETED/CANCELLED 拒绝；③OPEN 放行回归；④STRICT 超预算拒绝（ERR_BUDGET_EXCEEDED + 零落库）；⑤WARNING 放行（LOG.warn + 归集落库）；⑥P2-RC-048 闭合断言（requireReferenceable 消费路径）。
      - Skill: `nop-testing`
- [ ] Proof：`mvn test -pl module-projects/erp-prj-service` 全绿（既有 138 零回归）+ 全量 `mvn test` + `mvn clean install -DskipTests` + `bash docs/audits/nop-compliance-checker.sh`（零漂移确认）。
      - Skill: `nop-testing`
- [ ] Add：owner doc 回填——`state-machine.md §1/§4` 状态门控实现注记 + `cost-collection.md §3.3` 报销时机 + `§7` requireReferenceable 消费注记（P2-RC-048 闭合）；arm-index P1-RC-050/P1-RC-051（报销路径）/P2-RC-048 → 更新 + roadmap RC-R1.62 → done ✅（行标签按 B 类裁决改写，对齐 RC-R1.48/59 先例）+ 本日志条目。
      - Skill: `none`

Exit Criteria:

- [ ] 新测试全绿 + 既有测试零回归 + 全量验证命令通过（成功模式）；任一失败模式须修复或登记后才勾选
- [ ] owner doc/arm-index/roadmap 回填完成

## Draft Review Record

- Independent draft review iteration 1: `acceptable as-is`（`ses_ff5608b96ffeYiCkFuppB49dEb`，独立 general 子代理，新会话冷重播无起草者上下文）— 0 Blocker / 0 Major / 5 Minor（全部非阻塞 polish，已在计划内就地修正）：① roadmap:454 行文本改写须为显式 checkbox（Phase 3 回填项已补「行标签按 B 类裁决改写」措辞）；② requireReferenceable 行号漂移 :62-73→实仓 :65-76（Baseline 已修正为 :65-76 范围描述）；③ Phase 2 Add item 2 措辞混淆（`ErpPrjConfigs` 只能提供 `budgetControlStrict()` config 不能替代 `check()` 调用——已改为「注入 BudgetChecker bean（app-service.beans.xml:43-44）或经既有注入面」）；④ Phase 2 退出「被拒（可复现）」未指明复现机制（测试在 Phase 3——已补「（可复现：Phase 3 测试或 GraphQL 冒烟）」）；⑤ closeProject STRICT 超预算抛 ERR_BUDGET_EXCEEDED 设计后果（close 触发归集于 OPEN 期——已补 Phase 1 Decision 3 记录）。12/12 实仓抽查全 PASS（refreshExpenseCost 双缺口/requireReferenceable/BudgetChecker/两调用点 @BizMutation 事务/G9 合并先例/P2-RC-048 协同/测试基线/owner doc 行号）。格式/完备性/范围/反松弛/结束证据全 PASS，规则 14 G9 合并正当。草案可接受执行。
- Independent draft review iteration 2: `acceptable as-is`（mission-driver 复核 `2026-08-16-2043` 批次）— iteration-1 全部 5 Minor 已就地修正并经实仓核实（requireReferenceable :65-76 精确/BudgetChecker bean 注册 app-service.beans.xml:43-44 存在/closeProject→refreshExpenseCost 触发链 :69 证实/roadmap :454 行标签改写项在位）。草案审查收敛 → `Plan Status: draft → active`。

## Closure Gates

- [ ] 范围内行为完成（状态门控 + 报销预算检查 + P2-RC-048 闭合）
- [ ] 相关文档对齐（state-machine.md + cost-collection.md + arm-index + roadmap）
- [ ] 已运行验证（`mvn test -pl module-projects/erp-prj-service` + 全量 `mvn test` + `mvn clean install -DskipTests` + `bash docs/audits/nop-compliance-checker.sh`）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### 采购审核时机预算检查（P1-RC-051 采购路径）

- Classification: `optimization candidate`（跨计划协同项）
- Why Not Blocking Closure: 报销路径（本行）与采购路径（RC-R1.61 物料归集 successor）分属不同控制点；采购路径被检查对象（物料归集行）在 RC-R1.61 落地前尚不存在；本行闭合报销时机，采购时机随 RC-R1.61 协同闭合
- Successor Required: `yes`（触发条件：RC-R1.61 物料归集落地时同步接 budgetChecker.check，闭合 P1-RC-051 全量）

### 预算余量公式 commitment 第三项（P2-RC-049）

- Classification: `watch-only residual`
- Why Not Blocking Closure: P2 登记项（Q4 张力声明）；projects 域无生产承诺源致第三项实际为零；与 RC-R1.61 物料归集落地后协同
- Successor Required: `yes`（触发条件：物料归集落地后 commitment 源产生时，与 P1-RC-051 采购路径协同补全三项式）

## Closure

Status Note: <why the plan can close>

Closure Audit Evidence:

- Auditor / Agent: <independent auditor or independent subagent>
- Evidence: <task id / log link / walkthrough record>

Follow-up:

- <仅非阻塞跟进项目；已确认的缺陷不得出现在此处>
