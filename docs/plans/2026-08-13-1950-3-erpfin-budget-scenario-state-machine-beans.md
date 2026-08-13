# 2026-08-13-1950-3-erpfin-budget-scenario-state-machine-beans 预算方案 ErpFinBudgetScenario docStatus + approveStatus 实体级状态机 Bean（M4.11 + M4.12）

> Plan Status: completed
> Last Reviewed: 2026-08-13
> Source: `docs/backlog/entity-state-machine-migration-roadmap.md` 工作项 M4.11（ErpFinBudgetScenario.docStatus）+ M4.12（ErpFinBudgetScenario.approveStatus），均 plan-first；M0.2 清单行 `docs/analysis/2026-08-12-entity-state-axis-inventory.md §3.5 finance M4.11/12`（FIN-24/25 行）
> Related: M4 finance 同域先例 `2026-08-13-1146-2-erpfin-expense-claim-state-machine-beans.md`（M4.4+M4.5 draft）+ `2026-08-13-1146-3-erpfin-employee-advance-state-machine-beans.md`（M4.6+M4.7 draft）；M4 plan-first 先例 `2026-08-13-0805-3-erpprj-timesheet-settlement-state-machine-beans.md`；M0.1 契约 + M1.3 模板 `docs/architecture/entity-state-machine-bean.md §11`
> Mission: entity-state-machine
> Work Item: M4.11 + M4.12
> Audit: required
>
> **治理声明（§11.2 M4）**：本计划按 M4 plan-first 约束执行。BudgetScenario approve 生成 BUDGET 影子凭证（`BudgetVoucherGenerator.generate`），cancel 红冲全部 BUDGET 凭证（`BudgetVoucherGenerator.reverse`），属财务影响保护区。声明 §11.2 M4 硬约束：(i) plan-first + 受保护行为人工/owner-doc 门控；(ii) 过账时序/编排/失败回退不改，继续由 `BudgetVoucherGenerator` + `ErpFinBudgetScenarioProcessor` 管理；(iii) `posted` 不入轴；(iv) 跨域副作用保留原 Processor 路径；(v) 既有凭证生成/红冲时序不改。本计划是 plan-first 产物，人工/owner-doc 确认门控已于 2026-08-13 解除，转 `active` 进入实施。
>
> **规则 14 bundling 声明**：M4.11（docStatus）+ M4.12（approveStatus）属同一实体 ErpFinBudgetScenario 的双轴（同一 owner doc `docs/design/finance/budget.md`、同一 facade `ErpFinBudgetScenarioProcessor`、同一结果表面 = BudgetScenario 状态机生命周期），按指南规则 14 合并为单计划。两轴经同一 facade `validateTransition` 守卫且写入耦合（approve 同时写 docStatus=APPROVED + approveStatus=APPROVED），分拆为两计划会割裂同一状态迁移决策。

## Current Baseline

> 按 M0.2 清单 `docs/analysis/2026-08-12-entity-state-axis-inventory.md §3.5 finance M4.11/12`（FIN-24/25 行）+ 实仓核实。

- **dict 双轨**：docStatus 用 `erp-fin/budget-status`（6 值：DRAFT/SUBMITTED/APPROVED/REJECTED/CANCELLED/CLOSED，`budget-status.dict.yaml`），approveStatus 用 `wf/approve-status`（4 值：UNSUBMITTED/SUBMITTED/APPROVED/REJECTED）。两 dict 的 SUBMITTED/APPROVED/REJECTED 字面值重叠。
- **状态机（实仓 + owner doc `budget.md:42` + facade javadoc `ErpFinBudgetScenarioProcessor:29-35` 三方核实）**：
  ```
  DRAFT →(submit)→ SUBMITTED →(approve)→ APPROVED（生成 BUDGET 影子凭证）
  REJECTED →(submit 重提)→ SUBMITTED（live Processor allowedFrom=DRAFT/REJECTED；owner doc javadoc 写「REJECTED→DRAFT（修改重提）」——三方 drift，见下方）
  SUBMITTED →(reject)→ REJECTED
  APPROVED →(cancel)→ CANCELLED（红冲 BUDGET 凭证）
  APPROVED →(carryForward)→ CLOSED（结转；源方案 docStatus→CLOSED）
  APPROVED →(rollForward)→ 新方案 DRAFT（滚动预算；**源方案 docStatus 不变=APPROVED**，rollForward 是 spawn-new-entity 操作非源 docStatus 迁移）
  ```
- **owner doc / javadoc / live code 三方 drift（REJECTED 重提路径，须 Fix 登记）**：owner doc `budget.md:42` + facade javadoc `:31` 均写 `REJECTED → DRAFT（修改重提）`；live submit Processor `allowedFrom=DRAFT/REJECTED` 目标=SUBMITTED——即 live 行为为 `REJECTED→SUBMITTED`（直提，不回 DRAFT）。三方不一致（doc/javadoc 写 DRAFT，code 走 SUBMITTED）。按路线图规则 5/13，已确认 drift 须 Fix/Decision 登记（非 Follow-up）；本计划 Decision：Bean 据实编码 `REJECTED→SUBMITTED`（保持 live 行为不变），owner doc/javadoc drift 移交 successor 补正（非 Bean 行为变更）。
- **rollForward 非 docStatus 迁移（实仓关键发现）**：`ErpFinBudgetScenarioRollForwardProcessor:46-60,118` 创建**新**目标方案 `docStatus=DRAFT`，**源方案保持 APPROVED 不变**——rollForward 是 spawn-new-entity 操作，非源 docStatus 转换。因此 docStatus Bean 矩阵**不含 rollForward 边**；rollForward 作为**元数据-only** 记录在 `transitions()` 中（标注 spawn 而非源迁移），不纳入 `assertCanXxx` 守卫。carryForward 才是真正的源 docStatus 迁移（`APPROVED→CLOSED`）。
- **双轴耦合（实仓关键发现）**：approve Processor 同时写 docStatus=APPROVED + approveStatus=APPROVED（`ErpFinBudgetScenarioApproveProcessor:28-29`）；submit/reject 同理。`validateTransition` 守卫**只读 docStatus**（`ErpFinBudgetScenarioProcessor:104` `scenario.getDocStatus()`），approveStatus 不参与迁移守卫——它的值与 docStatus 同步推进。**Decision（Phase 1）**：docStatus Bean 是主迁移轴（承载全部命名动作的迁移矩阵），approveStatus Bean 是镜像轴（矩阵与 docStatus 一致但用 `wf/approve-status` dict 值）；两 Bean 独立注册，各自无状态（§2），但矩阵同构。
- **facade 编排模式（不同于采购/销售的 per-mutation Processor 模式）**：`ErpFinBudgetScenarioProcessor`（facade，`budget/` 包）注入 4 per-mutation Processor（Submit/Approve/Reject/Cancel）+ `BudgetVoucherGenerator`，委托 per-mutation Processor 执行。但 per-mutation Processor **自身又委托回 facade helper**（`ErpFinBudgetScenarioApproveProcessor:20` `@Inject ErpFinBudgetScenarioProcessor processor`）——形成 facade↔Processor 双向委托。approve 方法整体覆写（`:23-32`），编排 `validateTransition(target=APPROVED, allowedFrom=SUBMITTED)`→`generateBudgetVoucher`→`setDocStatus(APPROVED)+setApproveStatus(APPROVED)`→`save`。骨架 `AbstractApproveProcessor` 的 `submittedStatus()`/`approvedStatus()`/`setApproveStatus()` 等返回 null/不触达（"not reached: main method delegates to monolithic Processor"）。
- **固定迁移守卫（Bean 接线点）**：`ErpFinBudgetScenarioProcessor.validateTransition:103-118`——读 `docStatus`、比对 `allowedFrom` 白名单、非法抛 `ERR_BUDGET_SCENARIO_ILLEGAL_TRANSITION`（参数 `scenarioCode`/`currentDocStatus`/`expectedDocStatus`）。这是 Bean 替换的固定判断。各 per-mutation Processor 调 `processor.validateTransition(scenario, TARGET, ALLOWED_FROM)`。
- **逐动作 writer（实仓核实）**：
  - **submit**：`ErpFinBudgetScenarioSubmitForApprovalProcessor` → facade `validateTransition(target=SUBMITTED, allowedFrom=DRAFT/REJECTED)` → `setDocStatus(SUBMITTED)+setApproveStatus(SUBMITTED)`。
  - **approve**：`ErpFinBudgetScenarioApproveProcessor:23-32` → `validateTransition(APPROVED, SUBMITTED)` → `generateBudgetVoucher` → `setDocStatus(APPROVED)+setApproveStatus(APPROVED)` → save。**财务影响**。
  - **reject**：`ErpFinBudgetScenarioRejectProcessor` → `validateTransition(REJECTED, SUBMITTED)` → `setDocStatus(REJECTED)+setApproveStatus(REJECTED)`。
  - **cancel**：`ErpFinBudgetScenarioCancelProcessor` → `validateTransition(CANCELLED, APPROVED)` → `reverseBudgetVoucher`（红冲 BUDGET 凭证）。**财务影响**。
  - **carryForward**：`ErpFinBudgetScenarioCarryForwardProcessor` → 源方案 `APPROVED→CLOSED`（结转）。**绕过 facade `validateTransition`**——使用自身守卫 `validateCarryForwardPreconditions`/`validateApproved`（抛不同领域码 `ERR_BUDGET_SCENARIO_NOT_APPROVED`/`ERR_BUDGET_CARRY_FORWARD_RULE_INVALID`，非 `ERR_BUDGET_SCENARIO_ILLEGAL_TRANSITION`）。
  - **rollForward**：`ErpFinBudgetScenarioRollForwardProcessor:46-60` → **创建新方案** `docStatus=DRAFT`（滚动预算），**源方案 docStatus 不变**。**非源 docStatus 迁移**，不纳入 Bean `assertCanXxx` 守卫。
  - **无 withdraw/reverseApprove Processor**——BudgetScenario 审批生命周期不含反审核/撤回。
- **CLOSED 状态**：`erp-fin/budget-status` dict 第 6 值 CLOSED（已结转），经 carryForward 从 APPROVED 到达（源方案迁移）。rollForward 不改变源方案状态。CLOSED 为终态（无出边）。owner doc `budget.md:38` 字段表仅列 5 值（遗漏 CLOSED，CLOSED 见 `budget.md:222` A2 扩展节）——owner-doc 字段表 stale drift，移交 successor 补正。
- **Bean 接线策略（Decision Phase 1）**：(A) 落地 docStatus Bean `ErpFinBudgetScenarioDocumentStateMachine`（主轴，承载 **5 源迁移动作**：submit/approve/reject/cancel/carryForward；rollForward 不纳入 assertCan 守卫，仅 metadata-only in `transitions()` 标注 spawn）；(B) 落地 approveStatus Bean `ErpFinBudgetScenarioApprovalStateMachine`（镜像轴，承载 submit/approve/reject 3 动作，无 withdraw/reverseApprove）；(C) **Bean 守卫执行范围**：仅 facade `validateTransition` 路由的 4 动作（submit/approve/reject/cancel）经 Bean `assertCanXxx` 守卫；carryForward/rollForward **绕过 `validateTransition`**（使用自身守卫 + 不同错误码），Bean **不接管**其守卫——carryForward 的 `APPROVED→CLOSED` 边仅作 `transitions()` 元数据记录，不经 Bean 运行时守卫，避免改变 carryForward 的错误码值（兑现「保持全部既有外部行为不变」）。
- **common 层非法迁移码已存在**：`ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION`（参数 `currentStatus`/`expectedStatus` + `action`）。
- **领域错误码**：`ErpFinErrors.ERR_BUDGET_SCENARIO_ILLEGAL_TRANSITION`（`:397`，`erp.err.fin.budget.scenario.illegal-transition`，参数 `scenarioCode`/`currentDocStatus`/`expectedDocStatus`）。
- **Bean 注册范式已存在**：`_vfs/erp/fin/beans/app-service.beans.xml` 已注册既有 finance Processor/Bean。新 2 Bean 追加。
- **既有测试（层 3 回归基线）**：须实仓扫描 `TestErpFinBudget*` 测试（M0.2 清单 §3.5 标「无」）。跨域 `CommitmentVoucherGenerator` 测试。
- **合规基线**：R5=0、R11=0。本计划保持。
- **owner doc 覆盖**：`docs/design/finance/budget.md`（BudgetScenario 状态机 §42 + §38 字段表 + A2 扩展 §222 CLOSED）。owner doc 存在但含 stale drift（§38 字段表遗漏 CLOSED；§42 REJECTED→DRAFT 与 live REJECTED→SUBMITTED 不一致）——Phase 2 四方对照 Fix/successor 登记。

## Goals

- 落地 2 个无状态 Bean：
  - `ErpFinBudgetScenarioDocumentStateMachine`（docStatus 主轴，`erp-fin/budget-status` dict，承载 **5 源迁移动作**矩阵：submit DRAFT/REJECTED→SUBMITTED、approve SUBMITTED→APPROVED、reject SUBMITTED→REJECTED、cancel APPROVED→CANCELLED、carryForward APPROVED→CLOSED；rollForward 为 spawn-new-entity 非 source 迁移，仅 metadata-only in `transitions()`）+ 终态/初始态分类 + 只读 `transitions()`。
  - `ErpFinBudgetScenarioApprovalStateMachine`（approveStatus 镜像轴，`wf/approve-status` dict，承载 3 动作：submit、approve、reject；无 withdraw/reverseApprove）。
- 将 facade `ErpFinBudgetScenarioProcessor.validateTransition`（4 动作路由：submit/approve/reject/cancel）改调 docStatus Bean 委托（`assertCanXxx`）；**carryForward/rollForward 绕过 `validateTransition`，Bean 不接管其守卫**（仅 `transitions()` metadata）；**动态业务守卫与副作用保留原位**（`generateBudgetVoucher`/`reverseBudgetVoucher`、carryForward/rollForward 编排）。
- 保持全部既有外部行为不变（错误码值/参数、迁移边、凭证生成/红冲时序）。
- 新增层 1 矩阵完备性测试（2 Bean）；层 3 既有集成测试全绿回归。
- 层 2 四方对照（dict ↔ owner doc ↔ Bean 元数据 ↔ 全部 writer）。

## Non-Goals

- 不修改任何 `model/*.orm.xml` / `model/*.api.xml` / 字典 yaml。
- 不触碰 `posted`；凭证生成/红冲编排保留在 `BudgetVoucherGenerator` + facade 原位（§11.2 M4 (ii)）。
- 不改变 `BudgetVoucherGenerator.generate/reverse` 时序/失败回退（§11.2 M4 (ii)/(iv)）。
- 不合并双轴为单 Bean（§1 双轴约定：一 Bean 对一实体一轴）。
- 不引入全局 CRUD 写锁。
- 不自主跳过 M4 plan-first 人工/owner-doc 门控（§11.2 M4 (i)）。
- 不证 Delta 覆盖（M4 保护域单项，归 M5.3）。
- 不迁移 `ErpFinBudgetLine.status`（子表行状态，排除-技术/派生）。

## Task Route

- Type: `implementation-only change`（消费 M0.1 契约 + M0.2 清单 + M1.3 模板 §11 + finance 同域先例；落地 2 轴 Bean + facade 接线 + 三层测试 + 四方对照；不改契约/模型/公共 API。**M4 plan-first**——approve/cancel 触发预算凭证生成/红冲）
- Owner Docs: `docs/architecture/entity-state-machine-bean.md`（M0.1 契约 + §11 模板 + §11.2 M4 变体 + §1 双轴约定 + §2 无状态约束）、`docs/design/finance/budget.md`（BudgetScenario 状态机 §42 + 字段表 §38 stale drift + A2 扩展 §222 CLOSED）、`docs/analysis/2026-08-12-entity-state-axis-inventory.md`（FIN-24/25 行）、`docs/architecture/processor-extension-pattern.md`（Facade + Processor 范式）、`docs/skills/state-machine-business-review-prompt.md`
- Skill Selection Basis: 路线图 M4.11/12 指定 `nop-backend-dev` + `nop-testing`。`nop-backend-dev` 匹配「facade validateTransition 接线、Bean 注册、双轴耦合 Decision、凭证副作用保留、错误码映射、`@Inject` 非 private」；`nop-testing` 匹配「矩阵表驱动测试 + 既有集成回归」。层 2 引用 `state-machine-business-review-prompt.md`。必需输入均已就绪。

## Infrastructure And Config Prereqs

- **M4 plan-first 人工/owner-doc 门控（阻塞前置，§11.2 M4 (i)）**：本计划触及受保护预算凭证过账行为（approve 生成 BUDGET 凭证；cancel 红冲）。在人工/owner-doc 确认「以行为保持的矩阵集中化方式迁移此双轴、凭证生成/红冲路径完整保留」可接受前为阻塞前置。**[此门控已于 2026-08-13 经人工确认解除，见 Draft Review Record 门控确认记录]**
- 无端口/环境变量/CORS/密钥/.env/外部服务依赖。无数据迁移。

## Execution Plan

### Phase 1 - 2 个 StateMachine Bean + 注册 + facade 接线 + 层 1 矩阵完备性测试

Status: completed
Targets: `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/statemachine/{ErpFinBudgetScenarioDocumentStateMachine,ErpFinBudgetScenarioApprovalStateMachine}.java`（新建）、`.../budget/ErpFinBudgetScenarioProcessor.java`（facade `validateTransition` 改调 Bean）、`.../beans/app-service.beans.xml`（注册 2 Bean）、`.../statemachine/TestErpFinBudgetScenarioStateMachines.java`（新建）
Skill: `nop-backend-dev` + `nop-testing`

- Item Types: `Add | Decision | Proof`
- Prereqs: M1.3 done（已满足）；M4.12 deps = M1.3 + M4.11（同计划，同 Phase）

- [x] `Decision`（双轴耦合裁定 + rollForward 非 source 迁移 + REJECTED 重提三方 drift + carryForward/rollForward 守卫绕过）：
  (A) docStatus 为主迁移轴；approveStatus 镜像轴（3 动作）。双轴矩阵同构。
  (B) rollForward 是 spawn-new-entity（源 docStatus 不变），**不纳入 docStatus Bean assertCan 矩阵**；仅 `transitions()` metadata。
  (C) carryForward 是 source 迁移 APPROVED→CLOSED，但**绕过 facade `validateTransition`**（使用自身守卫 + 不同错误码 `ERR_BUDGET_SCENARIO_NOT_APPROVED`/`ERR_BUDGET_CARRY_FORWARD_RULE_INVALID`）。Bean **不接管** carryForward 守卫——carryForward 边仅作 `transitions()` metadata。避免改变 carryForward 错误码（行为保持）。
  (D) REJECTED 重提三方 drift：owner doc `budget.md:42` + javadoc 写 REJECTED→DRAFT，live Processor allowedFrom=DRAFT/REJECTED 目标=SUBMITTED。Bean 据实编码 REJECTED→SUBMITTED（保持 live 行为）；owner doc/javadoc drift 移交 successor。
  Skill: `state-machine-business-review-prompt.md`
- [x] `Add`：落地 `ErpFinBudgetScenarioDocumentStateMachine`（docStatus 主轴）——`assertCanSubmit/Approve/Reject/Cancel/CarryForward(String docStatus)`（5 源迁移动作；**rollForward 不纳入 assertCan**）+ 各 `*TargetStatus()`（carryForward 目标=CLOSED）+ `isTerminal`/`initialStatuses`/`terminalStatuses` + 只读 `transitions()`（含 carryForward APPROVED→CLOSED 边 + rollForward metadata-only 标注）。非法来源态 → common 码 `ERR_ILLEGAL_STATUS_TRANSITION` + `action`/`fromStatus`。严格无状态（§2）。
  Skill: `nop-backend-dev`
- [x] `Add`：落地 `ErpFinBudgetScenarioApprovalStateMachine`（approveStatus 镜像轴）——`assertCanSubmit/Approve/Reject(String approveStatus)` + 各 `*TargetStatus()` + `isTerminal`/`initialStatuses`/`terminalStatuses` + `transitions()`（3 动作 3 边 + resubmit 边）。严格无状态。
  Skill: `nop-backend-dev`
- [x] `Add`：在 `app-service.beans.xml` 以 FQN id 注册 2 Bean。
  Skill: `nop-backend-dev`
- [x] `Decision | Add`（facade 接线）：`ErpFinBudgetScenarioProcessor.validateTransition`（4 动作路由：submit/approve/reject/cancel）改调 docStatus Bean 的对应 `assertCanXxx`。错误码映射 common→`ERR_BUDGET_SCENARIO_ILLEGAL_TRANSITION`（保留领域码 + 参数）。**carryForward/rollForward 不经 facade `validateTransition`，Bean 不接管其守卫**。`generateBudgetVoucher`/`reverseBudgetVoucher` 副作用保留原位。
  Skill: `nop-backend-dev`
- [x] `Proof`：层 1 矩阵完备性（表驱动，不经 BizModel/facade）——docStatus Bean {submit DRAFT/REJECTED→SUBMITTED + approve SUBMITTED→APPROVED + reject SUBMITTED→REJECTED + cancel APPROVED→CANCELLED + carryForward APPROVED→CLOSED + terminal {CANCELLED,CLOSED} 无出边 + rollForward metadata-only in transitions}；approveStatus Bean {submit + approve + reject + resubmit}。各 assertCan 合法/非法 + transitions 一致 + initial/terminal 集合正确。
  Skill: `nop-testing`

Exit Criteria:

- [x] 2 Bean 无状态、矩阵完整；双轴耦合 Decision + CLOSED 出边核实记录在案。
- [x] 2 Bean 已在 `app-service.beans.xml` 注册；facade `validateTransition` 委托 Bean；`@Inject` 字段非 private。

### Phase 2 - 层 2 四方对照 + 层 3 既有集成回归

Status: completed
Targets: `module-finance/erp-fin-service/src/test/`（既有集成测试，零新建）
Skill: `state-machine-business-review-prompt.md` + `nop-testing`

- Item Types: `Proof`
- Prereqs: Phase 1（2 Bean + 接线已落地）

- [x] `Proof`：层 2 四方对照——dict `erp-fin/budget-status`（6 值）↔ `docs/design/finance/budget.md` BudgetScenario §42（状态图）+ §38 字段表（stale: 5 值遗漏 CLOSED，drift 登记）↔ docStatus Bean 元数据 ↔ 全部 writer（4 per-mutation Processor via `validateTransition` + carryForward Processor 自身守卫 + rollForward spawn-new-entity + 创建写 DRAFT + CRUD 路径 §9.4 选项 c 排除）。approveStatus 同理对照 `wf/approve-status`。**三方 drift 登记**：(1) REJECTED 重提 doc/javadoc=DRAFT vs code=SUBMITTED；(2) budget.md:38 字段表遗漏 CLOSED；(3) rollForward 非 source 迁移（javadoc 未标注）。drift 项按规则 5 Fix/successor 处置（见 Closure Follow-up）。
  Skill: `state-machine-business-review-prompt.md`
- [x] `Proof`：层 3 既有命名动作回归——复用既有 `TestErpFinBudget*` 测试基线（实仓确认 3 个：`TestErpFinBudgetEndToEnd` submit→approve→cancel + `TestErpFinBudgetCarryForward` + `TestErpFinBudgetRollForward`），证明凭证生成/红冲、审计、领域错误码 + 参数不变。本地 `mvn test -pl module-finance/erp-fin-service -am` 全绿（380 tests, 0 failures）。
  Skill: `nop-testing`

Exit Criteria:

- [x] 层 2 四方对照裁定完成（含 owner-doc 存在性核实 + doc drift 处置如有）。
- [x] 层 3 既有集成测试全绿（零行为回归）或缺口已登记。

## Draft Review Record

- Independent draft review iteration 1: `needs revision` (`ses_00505c4f5ffeiiTQNeOOg0Fs9D`) — BLOCKERS：(B1) rollForward 非 source docStatus 迁移（spawn-new-entity，源保持 APPROVED），Bean 矩阵误含 rollForward 边——v2 已修正为 5 源迁移动作 + rollForward metadata-only；(B2) carryForward/rollForward 绕过 facade `validateTransition`（使用自身守卫 + 不同错误码），Bean 接管会改变错误码——v2 已修正 Bean 守卫范围仅 4 动作，carryForward 边仅 `transitions()` metadata。MAJORS：(M1) owner doc/javadoc 写 REJECTED→DRAFT 与 live code REJECTED→SUBMITTED 三方 drift——v2 已登记为 Fix/successor；(M2) owner doc 实际为 `budget.md` 非 `state-machine.md`，§38 字段表 stale（遗漏 CLOSED）——v2 已修正所有引用 + 登记 drift。其余（模板结构、§11.2 M4 治理、rule 14 bundling、双轴耦合 Decision、Deferred 诚实性）均 pass。
- Independent draft review iteration 2: `acceptable as draft` (`ses_004fc3ef5ffemW6B6TtzcPvLwR`) — 全部 4 项 iteration 1 问题（B1/B2/M1/M2）已解决并实仓核实。docStatus Bean 5 源迁移动作 + rollForward metadata-only；Bean 守卫范围 4 动作（carryForward 边仅 transitions() metadata）；REJECTED 重提三方 drift 登记；owner doc 引用全部修正为 `budget.md`。非阻塞观察：(1) approveStatus dict-name owner-doc drift（budget.md:39 写 `erp-fin/approve-status` vs ORM/Bean 用 `wf/approve-status`）将由 Phase 2 四方对照自然发现；(2) assertCanCarryForward 为 runtime-dead（matrix 完整性 + 保留 carryForward 错误码，显式 justified）。计划保持 `draft`（§11.2 M4 plan-first 门控未解除）。
- **M4 plan-first 人工/owner-doc 门控状态：confirmed（2026-08-13）**（§11.2 M4 (i)）。草案审查已收敛（acceptable as draft）。
- **M4 plan-first 门控确认记录（人工，2026-08-13）**：人工确认「以行为保持的矩阵集中化方式迁移此双轴、凭证生成/红冲路径完整保留」可接受。门控解除，`Plan Status: draft → active`。

## Closure Gates

- [x] **M4 plan-first 人工/owner-doc 门控已确认并记录于 Draft Review Record**（§11.2 M4 (i)；2026-08-13 人工确认，见 Draft Review Record 门控确认记录）
- [x] 范围内行为完成（双轴 Bean + facade 接线 + 层 1 矩阵 + 层 2 四方对照 + 层 3 回归）
- [x] 相关文档对齐（owner doc drift 登记为 successor，非静默排除）
- [x] 已运行验证：`mvn clean install -DskipTests` + `mvn test -pl module-finance/erp-fin-service -am` 全绿（380 tests） + `bash docs/audits/nop-compliance-checker.sh` R5=0/R11=0 actual ≤ baseline
- [x] 无范围内项目降级为 deferred/follow-up（assertCanCarryForward runtime-dead 为显式 justified 设计，非降级）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证
- [x] 结束审计由独立子代理（新会话）执行（见 Closure Audit Evidence）
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### Delta 覆盖运行时实证

- Classification: `optimization candidate`
- Why Not Blocking Closure: M4 保护域单项 Delta 可选；归 M5.3。
- Successor Required: yes（触发条件 = M5.3）

### 全局 CRUD 写锁

- Classification: `watch-only residual`
- Why Not Blocking Closure: M0.1 §9 选项 (c) 排除。
- Successor Required: no

## Closure

Status Note: closed — independent closure audit passed

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，mission-driver AUDIT 步骤）
- 审计范围与结论：
  - 实仓核实 2 Bean 已落地：`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/statemachine/ErpFinBudgetScenarioDocumentStateMachine.java`（docStatus 主轴）+ `ErpFinBudgetScenarioApprovalStateMachine.java`（approveStatus 镜像轴）。
  - beans.xml 注册核实：`_vfs/erp/fin/beans/app-service.beans.xml:386-389` 以 FQN id 注册 2 Bean。
  - facade 接线核实：`ErpFinBudgetScenarioProcessor.validateTransition:107-126` 按 target 路由到 `documentStateMachine.assertCanSubmit/Approve/Reject/Cancel`（`:111/113/115/117`），catch `ERR_ILLEGAL_STATUS_TRANSITION`（`:125`）映射为领域码 `ERR_BUDGET_SCENARIO_ILLEGAL_TRANSITION`（`:126`，common 作 cause）。`@Inject` 字段 `documentStateMachine` 非 private（`:64`）。
  - 层 1 矩阵测试核实：`TestErpFinBudgetScenarioStateMachines.java` 存在于 `module-finance/erp-fin-service/src/test/java/app/erp/fin/service/statemachine/`。
  - 反 Hollow 核实：facade `validateTransition` 实际委托 Bean assertCan 调用（非占位 `return null`/空体）；错误码映射路径可触达（非法来源态抛 `ERR_ILLEGAL_STATUS_TRANSITION` → catch → 重抛领域码）。
  - 范围内行为保持核实：carryForward/rollForward 不经 facade `validateTransition`（绕过路径保留各自错误码），`generateBudgetVoucher`/`reverseBudgetVoucher` 副作用保留原位——兑现「全部既有外部行为不变」。
  - Deferred 诚实性核实：assertCanCarryForward runtime-dead 为显式 justified 设计（Phase 1 Decision (C)，保留 carryForward 错误码），非降级；owner-doc drift 4 项作为 successor 登记于 Follow-up，非静默排除范围内缺陷。
  - Closure Gates 全部 `[x]`，无范围内项目降级为 deferred/follow-up。
- **实施证据**：
  - 新建 2 Bean：`ErpFinBudgetScenarioDocumentStateMachine`（docStatus 主轴，5 assertCan + 5 targetStatus + isTerminal/initial/terminal + transitions 7 边含 rollForward spawn）+ `ErpFinBudgetScenarioApprovalStateMachine`（approveStatus 镜像轴，3 assertCan + 3 targetStatus + transitions 4 边）。均严格无状态（无 @Inject/DAO/IBiz/事务）。
  - beans.xml 注册 2 Bean（FQN id，`app-service.beans.xml`）。
  - facade 接线：`ErpFinBudgetScenarioProcessor.validateTransition` 按 target 路由到 docStatus Bean 的 `assertCanSubmit/Approve/Reject/Cancel`，catch common `ERR_ILLEGAL_STATUS_TRANSITION` 映射为领域码 `ERR_BUDGET_SCENARIO_ILLEGAL_TRANSITION`（common 作 cause）+ 实体参数。carryForward/rollForward 绕过 facade（保留各自错误码）。`generateBudgetVoucher`/`reverseBudgetVoucher` 副作用保留原位。
  - 层 1 矩阵测试：`TestErpFinBudgetScenarioStateMachines`（21 tests，全绿）。
  - 层 3 回归：380 finance-service tests 全绿（含 `TestErpFinBudgetEndToEnd` submit→approve→cancel + `TestErpFinBudgetCarryForward` + `TestErpFinBudgetRollForward`）。
- **层 2 四方对照记录**：
  - docStatus dict `erp-fin/budget-status`（6 值）↔ Bean 元数据（initial={DRAFT}, terminal={CANCELLED,CLOSED}, transitions 7 边）↔ 全部 writer（submit DRAFT/REJECTED→SUBMITTED, approve SUBMITTED→APPROVED, reject SUBMITTED→REJECTED, cancel APPROVED→CANCELLED via facade validateTransition; carryForward APPROVED→CLOSED via 自身守卫; rollForward spawn-new-entity DRAFT; CRUD 创建写 DRAFT §9.2 选项 c）。dict 无死状态（6 值全部有 writer 或可达）。
  - approveStatus dict `wf/approve-status`（4 值）↔ Bean 元数据（initial={UNSUBMITTED}, terminal={APPROVED}, transitions 4 边）↔ 全部 writer（submit→SUBMITTED, approve→APPROVED, reject→REJECTED via Processor; 创建写 UNSUBMITTED §9.2）。dict 无死状态。
- **验证**：`mvn clean install -DskipTests` 全绿 + `mvn test -pl module-finance/erp-fin-service` 380 tests 全绿 + compliance R5=0/R11=0。

Follow-up:

- **owner doc/javadoc drift（successor，规则 5 Fix 登记）**：
  - (1) `docs/design/finance/budget.md:42` 状态图 + `ErpFinBudgetScenarioProcessor:32` javadoc 写 `REJECTED → DRAFT（修改重提）`，与 live code `REJECTED→SUBMITTED`（直提）不一致。Bean 据实编码 `REJECTED→SUBMITTED`（保持 live 行为）；owner doc/javadoc 须补正为 `REJECTED → SUBMITTED`。
  - (2) `docs/design/finance/budget.md:38` 字段表列 docStatus 5 值（遗漏 CLOSED）；CLOSED 见 §222 A2 扩展。字段表须补全 6 值或交叉引用 §222。
  - (3) `ErpFinBudgetScenarioProcessor` javadoc 未标注 rollForward 为 spawn-new-entity（源保持 APPROVED 不变）；Bean 已在 transitions() metadata + javadoc 标注。
  - (4) `docs/design/finance/budget.md:39` 写 `dict erp-fin/approve-status` 但 ORM/Bean 实际用 `wf/approve-status`（dict-name owner-doc drift）。须补正为 `wf/approve-status`。
- **Delta 覆盖运行时实证**：归 M5.3（Deferred But Adjudicated 已登记）。
