# 2026-08-13-0805-3-erpprj-timesheet-settlement-state-machine-beans 项目工时与结算单状态机 Bean

> Plan Status: completed
> Review Hold: §11.2 M4 (i) 人工/owner-doc 门控**已于 2026-08-13 经人工确认解除**——本计划触及受保护会计过账行为（Timesheet/Settlement approve 触发过账，已由审查者经 live code 实证：`ErpPrjTimesheetApproveProcessor:42-56` + `ErpPrjProjectSettlementApproveProcessor:32-35`）。M3(iii)→M4 升级与保护域门控成立；该人工裁定非审查者可自主解除（project-context.md 会计保护域硬停止）。计划本身格式/完备性/范围/结束证据均已就绪 + 人工门控已确认，已转 `active` 进入实施。
> Last Reviewed: 2026-08-13
> Source: `docs/backlog/entity-state-machine-migration-roadmap.md` 工作项 M3.10（ErpPrjTimesheet.status）+ M3.11（ErpPrjProjectSettlement.docStatus）+ M3.12（ErpPrjProjectSettlement.approveStatus）；M0.2 清单行 `docs/analysis/2026-08-12-entity-state-axis-inventory.md §3.4`
> Related: `2026-08-13-1430-3-erpct-version-rebate-state-machine-beans.md`（owner-doc 缺口补章节 + 死状态 Decision 先例）、`2026-08-12-0738-1-cs-ticket-state-machine-bean-pilot.md`（M1.1 data-deletion ask-first 人工门控先例——本计划 M4 plan-first 人工门控同形）
> Audit: required
>
> **治理声明（§11.2 M3(iii) 升级裁定）**：Timesheet `approve` 与 Settlement `approve` 均触发会计过账 dispatcher（live code 实证，见 Current Baseline）。依 `entity-state-machine-bean.md §11.2 M3(iii)`「审批结果若触发下游过账则该项升级为 M4，不留在 M3」+ 路线图 M2-M4 纪律「会触发过账的 action 一律归 M4」，本计划**按 M4 plan-first 约束执行**（声明 §11.2 M4 硬约束 (i)–(v)），而非 M3。M0.2 清单 §3.4 将此三项标 `过账=无` 与 live code 冲突，属 inventory 分类漂移——本计划在 Phase 3 登记 Fix 并建议 M0.2 reconcile 路线图表内分类（M3→M4）。因触及受保护过账行为，本计划**人工/owner-doc plan-first 门控**已于 2026-08-13 确认解除，已转 `active` 进入实施（对齐 M1.1 data-deletion ask-first 先例：批准记录为计划起草/审查/实施的共同前置）。

## Current Baseline

- **实体一：ErpPrjTimesheet**（`module-projects/model/app-erp-projects.orm.xml:214`），`status` 单轴 `ext:dict="wf/approve-status"`（`:230`）。另有 `posted` boolean（`:231`，业财过账契约，不迁移）。
- **实体二：ErpPrjProjectSettlement**（`orm.xml:784`），双轴：
  - `docStatus` `ext:dict="erp-prj/project-status"`（`:808`；字典含 DRAFT/OPEN/ON_HOLD/COMPLETED/CANCELLED，Settlement 实际仅用 DRAFT/APPROVED/CANCELLED → OPEN/ON_HOLD/COMPLETED 为 Settlement 死状态，共享 dict 部分使用）。
  - `approveStatus` `ext:dict="wf/approve-status"`（`:809`）。

- **Timesheet.status 现状 writer**（3 Processor + 1 inline）：
  - `submit`（`ErpPrjTimesheetSubmitProcessor:57`）：`UNSUBMITTED → SUBMITTED`（guard 校验项目 OPEN + 任务 TODO/IN_PROGRESS + CostRateResolver + BudgetChecker）。
  - `approve`（`ErpPrjTimesheetApproveProcessor:42-56`）：`SUBMITTED → APPROVED`（**触发过账**：`TimesheetPostingDispatcher.tryPost` → PROJECT_COST_COLLECTION 凭证 + 成功则 `posted=true` + `ProjectCostAggregator.aggregateFromTimesheet`）。
  - `reject`（`ErpPrjTimesheetBizModel:71` inline）：`SUBMITTED → UNSUBMITTED`。
  - `cancel`（`ErpPrjTimesheetCancelProcessor:29-38`）：对 APPROVED+posted 工时**先红冲过账**（`postingDispatcher.reverse` + 清 `posted=false`/`postedAt=null`/`postedBy=null`，`:29-37`），再置 status→UNSUBMITTED（`:38`）。（**命名/语义漂移**：动作名 cancel 但目标态为 UNSUBMITTED 而非 CANCELLED——`wf/approve-status` 无 CANCELLED，cancel 语义为撤回/重置；Phase 3 裁定。BizModel Javadoc `:27-28` 误述 reject→DRAFT / cancel→CANCELLED，同为 doc drift。）

- **Settlement 双轴现状 writer**（`ErpPrjProjectSettlementProcessor` facade + per-mutation Processor）：
  - `createSettlement`（`CreateSettlementProcessor:49-50`）：初始 `docStatus=DRAFT` + `approveStatus=UNSUBMITTED`。
  - `doSubmit`（`Processor:122`）：`approveStatus UNSUBMITTED → SUBMITTED`。
  - `doApprove`（`Processor:126-127`）：`approveStatus → APPROVED` **且** `docStatus → APPROVED`（双轴同动）。**触发过账**：`ApproveProcessor:32-35` 先 `createAndActivateAsset`（资本化级联）+ facade `doPost:140-147`（`ProjectSettlementPostingDispatcher.tryPost` + `posted=true`），再回 doApprove 置状态。
  - `doReject`（`Processor:133`）：`approveStatus → REJECTED`。
  - `doCancel`（`Processor:137`）：`docStatus → CANCELLED`（cancel 内含 `rollbackAssetIfNeeded` + posting reverse）。
  - `reverseSettlement`（`ReverseSettlementProcessor:21-37`）：guard `posted=true`，调 `postingDispatcher.reverse` + `rollbackAssetIfNeeded`，置 `posted=false` + 清 postedAt/postedBy。**经核对：该动作既不写 docStatus 也不写 approveStatus**——是纯 `posted` 轴冲销动作，在 docStatus/approveStatus Bean 中**无迁移边**。

- **docStatus dict-value 漂移（Minimum Rule 5/13，不可降级）**：`Settlement.docStatus` 写入 `DOC_STATUS_APPROVED="APPROVED"`（`Processor:127` + `ErpPrjConstants:62`），但 `erp-prj/project-status` 字典仅含 DRAFT/OPEN/ON_HOLD/COMPLETED/CANCELLED——**APPROVED 被写入但不在字典内**（与 `ErpPrjCostCollection` 同型 drift，owner doc §适用对象三 :158-161 已先例登记）。此外 OPEN/ON_HOLD/COMPLETED 对 Settlement 无 writer（死状态）。Phase 3 须作 Fix/Decision 登记（不在 Phase 1 预设 APPROVED 为合法 dict 值）。

- **治理裁定（§11.2 M3(iii)→M4 升级）**：Timesheet approve 与 Settlement approve 均**触发会计过账**（上述 live code 实证）。依 `entity-state-machine-bean.md §11.2 M3(iii)` 与路线图 M2-M4 纪律，此三项**升级为 M4 plan-first**，非 M3。本计划据此声明 §11.2 M4 硬约束 (i) plan-first + 受保护过账行为人工/owner-doc 门控；(ii) 过账时序/编排/失败回退（posted 回写）/红冲闭环不改，继续由过账编排与 `posted` 契约管理；(iii) `posted` 不入轴；(iv) `IErpFinVoucherBiz`/资本化级联/归集等跨域副作用保留原 Processor/`I*Biz` 路径；(v) 既有 ReversalListener/红冲闭环以 `posted` 为契约不改。**本计划是 plan-first 产物（满足 (i) 的 plan 要件），人工/owner-doc 确认门控已于 2026-08-13 解除，已转 `active` 进入实施**。M0.2 清单 §3.4 标此三项 `过账=无` 属 inventory 分类漂移，Phase 3 登记 Fix 并建议 reconcile。

- **owner doc 缺口（关键）**：`docs/design/projects/state-machine.md` 仅有 §适用对象一（Project）+ §适用对象二（Task）+ §适用对象三（CRUD 桩 Deferred：Milestone/Billing/CostCollection）。**无 Timesheet 章节、无 ProjectSettlement 章节** → Phase 3 需新增 §适用对象四（工时记录）+ §适用对象五（项目结算单双轴），对齐 contract 先例补 §适用对象二/三。

- **M0.2 分类**：M3.10/M3.11/M3.12 归 M3。inventory 行财务影响/跨域/过账均 = `无`（M3.10 跨域 = `归集→finance successor`）。Deps：M1.3（done）；M3.12 行级依赖 M3.11（同实体 docStatus 先于 approveStatus）。门控已就绪。

## Goals

- 新建 3 个无状态 Bean：`ErpPrjTimesheetStateMachine`（status 单轴）+ `ErpPrjProjectSettlementDocumentStateMachine`（docStatus 轴）+ `ErpPrjProjectSettlementApprovalStateMachine`（approveStatus 轴），遵循 §1 命名 + §3 双轴分离。
- 将 Timesheet 3 Processor/inline + Settlement facade/per-mutation Processor 的固定迁移守卫接线为 Bean 委托，**保持既有外部行为、过账编排时序、posted 回写、红冲闭环、错误码、乐观锁、审批语义不变**（§11.2 M4 (ii)/(iv)/(v)）；过账 dispatcher、资本化级联、归集与 `posted` 完整保留在既有 Processor/`I*Biz` 路径，`posted` 不入轴（§11.2 M4 (iii)）。
- 裁决 Timesheet `cancel→UNSUBMITTED` 命名/语义漂移（intentional legacy behavior）+ Settlement docStatus dict-value 漂移（APPROVED 被写入但不在 dict）+ 共享 dict 死状态（OPEN/ON_HOLD/COMPLETED）+ BizModel Javadoc 漂移。
- 层 1（3 轴矩阵完备性）+ 层 2（四方对照 + 漂移 Decision/Fix + inventory 分类漂移登记）+ 层 3（既有动作回归，含过账成功/失败/红冲路径）三层证据。
- owner doc 新增 §适用对象四（Timesheet）+ §适用对象五（ProjectSettlement）章节。

## Non-Goals

- 不迁移 `posted`、不改变 `TimesheetPostingDispatcher`/`ProjectSettlementPostingDispatcher` 过账编排（触发时机/顺序/失败回退/红冲闭环）、不改变 `createAndActivateAsset` 资本化级联或 `aggregateFromTimesheet` 归集（§11.2 M4 (ii)/(iv)/(v)）。
- 不改变 Timesheet `cancel` 的目标态语义（保持 →UNSUBMITTED 既有行为 + 既有红冲过账路径；漂移仅作 Decision 记录 + owner doc 补注，不改代码语义）。
- 不改变 `reverseSettlement`（纯 `posted` 轴动作，不写 docStatus/approveStatus，Bean 中无迁移边）。
- 不修改 `model/*.orm.xml`、字典值或 API 契约（Settlement docStatus dict-value 漂移 APPROVED + 共享 dict 死状态保留，不改绑）。
- 不引入通用 CRUD 对 status 写入的运行时禁止（M0.1 successor）。
- 不迁移 `ErpPrjMilestone/Billing/CostCollection`（projects §适用对象三 已 Deferred 的 CRUD 桩，非本工作项）。
- 不自主跳过 M4 plan-first 人工/owner-doc 门控（§11.2 M4 (i)）：门控未确认前计划保持 `draft`。

## Task Route

- Type: `implementation-only change`（固定迁移矩阵集中化；**M4 plan-first**——审批轴 approve 触发会计过账，依 §11.2 M3(iii) 升级 M4；行为/契约/模型不改，但受 §11.2 M4 硬约束 + 人工门控）
- Owner Docs: `docs/design/projects/state-machine.md`（新增 §适用对象四/五）、`docs/design/projects/cost-collection.md §2`（Timesheet 既有语义）、`docs/architecture/entity-state-machine-bean.md`（§1/§2/§3/§11.2 M4 变体）、`docs/architecture/processor-extension-pattern.md`（Processor/过账边界）
- Skill Selection Basis: 本项是「BizModel/Processor 接线 + 多轴矩阵 + 过账边界保持 + M4 plan-first」后端开发，匹配 `nop-backend-dev`（决策门、跨实体、`@Inject` 非 private、过账吞异常自检）；矩阵测试与回归匹配 `nop-testing`；Phase 3 四方对照 + 漂移裁定 + inventory 漂移 Fix 匹配 `state-machine-business-review-prompt.md`。

## Infrastructure And Config Prereqs

- **M4 plan-first 人工/owner-doc 门控（阻塞前置，§11.2 M4 (i)）**：本计划触及受保护过账行为（Timesheet/Settlement approve 触发会计过账）。在人工/owner-doc 确认「以行为保持的矩阵集中化方式迁移此三轴、过账路径完整保留」可接受前为阻塞前置。**[此门控已于 2026-08-13 经人工确认解除，见 Draft Review Record 门控确认记录]**（M0.2 路线图表内分类 reconcile M3→M4 仍建议在 Phase 3 登记，属非阻塞 follow-up）。
- 无端口/环境变量/CORS/密钥/.env/外部服务依赖。无数据迁移。

## Execution Plan

### Phase 1 - 3 个 StateMachine Bean + 注册 + 层 1 矩阵完备性测试

Status: completed
Targets: `module-projects/erp-prj-service/src/main/java/app/erp/prj/service/statemachine/{ErpPrjTimesheetStateMachine,ErpPrjProjectSettlementDocumentStateMachine,ErpPrjProjectSettlementApprovalStateMachine}.java`（新建）、`module-projects/erp-prj-service/src/main/resources/_vfs/erp/prj/beans/app-service.beans.xml`（注册）、`module-projects/erp-prj-service/src/test/.../statemachine/TestErpPrjTimesheetAndSettlementStateMachines.java`（新建）
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: M1.3 done

- [x] 新建无状态 `ErpPrjTimesheetStateMachine`（status 单轴，§2 无状态约束）：矩阵 `submit`：`{UNSUBMITTED}→SUBMITTED`；`approve`：`{SUBMITTED}→APPROVED`；`reject`：`{SUBMITTED}→UNSUBMITTED`；`cancel`：非终态→UNSUBMITTED（**保持既有 cancel→UNSUBMITTED 语义**，Phase 3 裁定命名漂移）。分类 initial=`{UNSUBMITTED}`，terminal=`{APPROVED}`（cancel 目标为 UNSUBMITTED 非终态，可重新 submit）。
  - Skill: `nop-backend-dev`
- [x] 新建无状态 `ErpPrjProjectSettlementApprovalStateMachine`（approveStatus 轴，`Approval` 后缀）：矩阵 `submit`：`{UNSUBMITTED}→SUBMITTED`；`approve`：`{SUBMITTED}→APPROVED`；`reject`：`{SUBMITTED}→REJECTED`。**`reverseSettlement` 不写 approveStatus**（已核对 `ReverseSettlementProcessor:21-37`，纯 `posted` 轴冲销）→ 在本 Bean 中**无迁移边**，不发明边。initial=`{UNSUBMITTED}`，terminal=`{APPROVED}`。
  - Skill: `nop-backend-dev`
- [x] 新建无状态 `ErpPrjProjectSettlementDocumentStateMachine`（docStatus 轴，`Document` 后缀）：矩阵 `approve`（doApprove 触发）：`{DRAFT}→APPROVED`；`cancel`：`{DRAFT, APPROVED}→CANCELLED`。initial=`{DRAFT}`，terminal=`{CANCELLED}`。**APPROVED dict-value 漂移悬而未决**：APPROVED 被 doApprove 写入但不在 `erp-prj/project-status` 字典内——Bean 按既有 writer 建模该边（保持行为），dict 补全属 ORM ask-first 保护区，Phase 3 作 Fix/Decision 登记（不在 Phase 1 改 dict）。共享 dict 死状态（OPEN/ON_HOLD/COMPLETED 对 Settlement 无 writer）记录保留。
  - Skill: `nop-backend-dev`
- [x] Decision（前置）：在计划中记录三项漂移分类，供 Phase 3 owner-doc 补注引用：(a) Timesheet `cancel→UNSUBMITTED` = intentional legacy behavior（撤回语义，wf/approve-status 无 CANCELLED）+ BizModel Javadoc `:27-28` 误述漂移；(b) Settlement docStatus APPROVED dict-value 漂移（被写入但不在 dict，对齐 CostCollection 先例）+ 共享 dict 死状态保留；(c) M0.2 inventory 将此三项标 `过账=无` 与 live code 冲突 → 建议路线图 reconcile（M3→M4）。
  - Skill: `state-machine-business-review-prompt.md`

  > **Phase 1 Decision 落地记录（三项漂移分类）**：
  > - **(a) Timesheet `cancel→UNSUBMITTED` = intentional legacy behavior**：`wf/approve-status` 字典无 CANCELLED 值，工时 `cancel` 目标态为 UNSUBMITTED（撤回/重置语义）。Bean `assertCanCancel` 对全部 dict 值放行（行为保持——既有 `ErpPrjTimesheetCancelProcessor:26-41` 对状态不抛错，仅 APPROVED+posted 时先红冲过账）；APPROVED 为可逆终态（经 cancel 有出边）。另：`reject` 目标态为 UNSUBMITTED（非 REJECTED），故 REJECTED 对工时为 dict 死状态（无 writer 产生）。BizModel Javadoc `:27-28` 误述 reject→DRAFT / cancel→CANCELLED，Phase 3 就地修正。
  > - **(b) Settlement docStatus APPROVED dict-value 漂移**：`doApprove` 写入 `DOC_STATUS_APPROVED="APPROVED"`（`ErpPrjConstants:62`），但 `erp-prj/project-status` 字典仅 DRAFT/OPEN/ON_HOLD/COMPLETED/CANCELLED——APPROVED 被写入但不在字典内（对齐 `ErpPrjCostCollection` §适用对象三先例）。Bean 按既有 writer 建模该边（保持行为），dict 补全/rebind 列 successor（ORM ask-first）。共享 dict 死状态 OPEN/ON_HOLD/COMPLETED 对 Settlement 无 writer，保留为预留语义入口。
  > - **(c) M0.2 inventory 分类漂移**：M0.2 §3.4 将 M3.10/M3.11/M3.12 标 `过账=无` 与 live code 冲突（Timesheet approve 触发 `TimesheetPostingDispatcher.tryPost`、Settlement approve 触发 `ProjectSettlementPostingDispatcher.tryPost`+转固）。三项已按 §11.2 M3(iii) 升级 M4 plan-first 执行；Phase 3 Fix 登记建议路线图表内分类 reconcile（M3→M4），属非阻塞 follow-up。
- [x] 在非生成 `app-service.beans.xml` 以 FQN 为 bean id 注册 3 Bean
  - Skill: `nop-backend-dev`
- [x] Proof（层 1 矩阵完备性，3 轴表驱动）：`TestErpPrjTimesheetAndSettlementStateMachines`——Timesheet status 轴 × 4 动作合法/非法边 + cancel→UNSUBMITTED + initial/terminal；Settlement 双轴 × 各动作合法/非法边 + doApprove 双轴同动（docStatus DRAFT→APPROVED + approveStatus SUBMITTED→APPROVED）+ 共享 dict 死状态无 writer 核对。验证命令：`mvn test -pl module-projects/erp-prj-service -Dtest=TestErpPrjTimesheetAndSettlementStateMachines`
  - Skill: `nop-testing`

Exit Criteria:

- [x] 3 Bean 无状态、矩阵完整；doApprove 双轴同动在双 Bean 中各自表达一致；漂移 Decision 记录在案
- [x] 层 1 三轴表驱动测试通过（31 tests green，`mvn test -pl module-projects/erp-prj-service -Dtest=TestErpPrjTimesheetAndSettlementStateMachines`）

### Phase 2 - Processor/BizModel 接线（行为保持，过账副作用保留）+ 层 3 回归

Status: completed
Targets: `ErpPrjTimesheetSubmitProcessor`、`ErpPrjTimesheetApproveProcessor`、`ErpPrjTimesheetCancelProcessor`、`ErpPrjTimesheetBizModel`（reject inline）、`ErpPrjProjectSettlementProcessor`（facade）+ `ErpPrjProjectSettlement{SubmitForApproval,Approve,Reject,Cancel,CreateSettlement,ReverseSettlement}Processor`
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 1 三 Bean 落地

- [x] Timesheet：3 Processor + reject inline 守卫改 `@Inject ErpPrjTimesheetStateMachine` + `stateMachine.assertCan<Action>(from)` + 目标态回写（`<action>TargetStatus()`）。**完整保留**：项目 OPEN/任务允许/成本率/预算校验（动态守卫）；`approve` 的 `TimesheetPostingDispatcher.tryPost`+`posted=true`+`costAggregator`（**不得吞异常致 posted 悬挂**——ai-autonomy 已知失败模式，过账失败须显式不置 posted=true）；**`cancel` 对 APPROVED+posted 工时的红冲过账**（`postingDispatcher.reverse` + 清 posted/postedAt/postedBy，`CancelProcessor:29-37`）须原序保留，仅其末尾 status→UNSUBMITTED 守卫改 Bean 委托；错误码 `ERR_TIMESHEET_ILLEGAL_STATUS_TRANSITION` 及实体编号参数
  - Skill: `nop-backend-dev`
- [x] Settlement：facade/per-mutation Processor 守卫改双 Bean 委托（Approval 轴 submit/approve/reject + Document 轴 doApprove/doCancel 双轴同动）。**完整保留**：`ProjectSettlementPostingDispatcher.tryPost/reverse`、`createAndActivateAsset`/`rollbackAssetIfNeeded`、`buildLines`、`requireSettlement`/`save` helper、错误码与实体编号。`reverseSettlement` 不写 docStatus/approveStatus（纯 `posted` 轴）→ 不接线任一 Bean，仅保留其既有过账冲销路径。
  - Skill: `nop-backend-dev`
- [x] Proof（层 3 回归）：运行 projects 既有动作测试——Timesheet submit/approve/reject/cancel happy path + 非法态 + 过账成功 posted=true + 过账失败不悬挂（复用/补 `TestPrjPostingFaultInjection` 类故障注入断言 posted 不误置 true）+ cancel 红冲过账（APPROVED+posted → reverse + posted=false）；Settlement create/submit/approve/reject/cancel/reverse happy path + 双轴同动 + 资本化级联 + 非法态。验证命令：`mvn test -pl module-projects/erp-prj-service`
  - Skill: `nop-testing`

Exit Criteria:

- [x] Timesheet + Settlement 接线后既有测试全绿（行为、过账编排/红冲闭环、错误码、双轴同动、乐观锁无回归；过账失败不悬挂已断言）—— `mvn test -pl module-projects/erp-prj-service` 133 tests green（含 TestErpPrjTimesheetCost 7 / TestErpPrjBudgetAndCollection 7 / TestErpPrjProjectSettlement 4 / TestPrjPostingFaultInjection 1 / TestTimesheetPostingFailureAlert 1 / 新矩阵 31）

### Phase 3 - 层 2 四方对照 + 漂移 Decision + owner doc 补 §适用对象四/五

Status: completed
Targets: `docs/design/projects/state-machine.md`（新增 §适用对象四 Timesheet + §适用对象五 ProjectSettlement）、本计划 Closure
Skill: `state-machine-business-review-prompt.md`

- Item Types: `Fix | Proof | Decision | Add`
- Prereqs: Phase 2 接线完成

- [x] Proof（层 2 四方对照，10 维度）：Timesheet status 轴 dict（`wf/approve-status`）↔ owner doc ↔ Bean ↔ writer（3 Processor + reject inline）；Settlement 双轴 dict ↔ owner doc ↔ 双 Bean ↔ writer（facade + per-mutation Processor 全集，含 reverseSettlement=纯 posted 轴无状态写）
  - Skill: `state-machine-business-review-prompt.md`

  > **层 2 四方对照记录（dict ↔ owner-doc ↔ Bean ↔ writer，10 维度）**：
  >
  > **A. 工时 status 轴（`wf/approve-status`）**
  > | 维度 | dict | owner-doc §适用对象四 | Bean `ErpPrjTimesheetStateMachine` | writer |
  > |------|------|------|------|------|
  > | 状态集 | UNSUBMITTED/SUBMITTED/APPROVED/REJECTED | 同 dict（REJECTED 标死状态） | 同（transitions 覆盖 UNSUBMITTED/SUBMITTED/APPROVED） | submit→SUBMITTED、approve→APPROVED、reject→UNSUBMITTED、cancel→UNSUBMITTED |
  > | 迁移矩阵 | — | §2 五条边 | transitions() 5 边一致 | 3 Processor + reject inline 经 Bean 委托 |
  > | 终态 | — | APPROVED（可逆） | terminalStatuses={APPROVED}（cancel 出边=可逆） | cancel 红冲后置回 UNSUBMITTED |
  > | 初始态 | — | UNSUBMITTED | initialStatuses={UNSUBMITTED} | create 写 UNSUBMITTED（初始态写入） |
  > | 可达性 | — | UNSUBMITTED→SUBMITTED→APPROVED 可达；REJECTED 不可达 | BFS 一致 | REJECTED 无 writer ✓ |
  > | 异常路径 | — | §4（非法态/项目/任务/成本率/预算/过账/红冲） | Bean 报告 common 码，Processor 映射 ERR_TIMESHEET_ILLEGAL_STATUS_TRANSITION | 错误码 + 实体编号保留 |
  > | dict writer 覆盖 | 4 值 | UNSUBMITTED/SUBMITTED/APPROVED 有 writer；REJECTED 死 | 一致 | ✓ |
  > | 死状态 | REJECTED | 标死状态（保留） | transitions 无 REJECTED toStatus | ✓ |
  > | 外部依赖 | — | §8（cost-collection §2/§4.2、finance/posting） | Bean 无副作用（过账/归集留 Processor） | approve 触发过账+归集保留 |
  > | doc-code 一致 | — | cancel→UNSUBMITTED 漂移已就地修正（BizModel Javadoc + owner doc） | Bean 建模 cancel→UNSUBMITTED | live code 一致 |
  >
  > **B. 结算 approveStatus 轴（`wf/approve-status`）**
  > | 维度 | dict | owner-doc §适用对象五 | Bean `ErpPrjProjectSettlementApprovalStateMachine` | writer |
  > |------|------|------|------|------|
  > | 状态集 | UNSUBMITTED/SUBMITTED/APPROVED/REJECTED | 同 dict | 同 | submit→SUBMITTED、approve→APPROVED、reject→REJECTED |
  > | 迁移矩阵 | — | §2 三条边 | transitions() 3 边一致 | facade validateTransitionFor{Submit,Approve,Reject} 经 Bean |
  > | 终态 | — | APPROVED（真终态） | terminalStatuses={APPROVED}，无出边（cancel 只写 docStatus、reverse 只写 posted） | ✓ |
  > | 初始态 | — | UNSUBMITTED | initialStatuses={UNSUBMITTED} | createSettlement 写 UNSUBMITTED |
  > | 可达性 | — | UNSUBMITTED→SUBMITTED→{APPROVED,REJECTED} | BFS 一致 | ✓ |
  > | 异常路径 | — | §4 | Bean→ERR_SETTLEMENT_ILLEGAL_STATUS_TRANSITION | 错误码 + 实体编号保留 |
  > | dict writer 覆盖 | 4 值 | 全 4 值有语义（REJECTED=驳回汇） | 一致 | ✓ |
  > | 死状态 | 无 | 无 | 无 | reverseSettlement 不写 approveStatus（纯 posted 轴）✓ |
  > | 外部依赖 | — | §7（profitability、assets 转固） | Bean 无副作用 | approve 触发过账+转固保留 |
  > | doc-code 一致 | — | approve config-gated STRICT/RELAXED | Bean 承载 STRICT 默认；RELAXED 留 Processor | 一致 |
  >
  > **C. 结算 docStatus 轴（`erp-prj/project-status`）**
  > | 维度 | dict | owner-doc §适用对象五 | Bean `ErpPrjProjectSettlementDocumentStateMachine` | writer |
  > |------|------|------|------|------|
  > | 状态集 | DRAFT/OPEN/ON_HOLD/COMPLETED/CANCELLED（+APPROVED drift） | DRAFT/APPROVED/CANCELLED 用；OPEN/ON_HOLD/COMPLETED 死 | transitions 覆盖 DRAFT/APPROVED/CANCELLED | doApprove→APPROVED、doCancel→CANCELLED |
  > | 迁移矩阵 | — | §2 三条边 | transitions() 3 边一致 | facade validateTransitionForCancel + doApprove/doCancel 经 Bean |
  > | 终态 | — | CANCELLED（真终态） | terminalStatuses={CANCELLED}，无出边 | ✓ |
  > | 初始态 | — | DRAFT | initialStatuses={DRAFT} | createSettlement 写 DRAFT |
  > | 可达性 | — | DRAFT→{APPROVED,CANCELLED}；死状态不可达 | BFS 一致 | OPEN/ON_HOLD/COMPLETED 无 writer ✓ |
  > | 异常路径 | — | §4 | Bean→ERR_SETTLEMENT_ILLEGAL_STATUS_TRANSITION | 错误码保留 |
  > | dict writer 覆盖 | 5+1 值 | DRAFT/CANCELLED 有 writer；APPROVED drift 有 writer；OPEN/ON_HOLD/COMPLETED 死 | 一致 | ✓ |
  > | 死状态 | OPEN/ON_HOLD/COMPLETED | 标死状态（保留） | transitions 无其 toStatus | ✓ |
  > | 外部依赖 | — | §7 | Bean 无副作用 | reverseSettlement 不写 docStatus（纯 posted 轴）✓ |
  > | doc-code 一致 | — | APPROVED dict-value drift 已登记（对齐 CostCollection 先例） | Bean 按既有 writer 建模 | live code 一致 |
  >
  > **结论**：三轴四方对照无未裁决漂移——cancel→UNSUBMITTED 与共享 dict 死状态均已裁定并落入 owner doc；APPROVED dict-value drift 已登记为 successor（ORM ask-first）；BizModel Javadoc 漂移已就地修正。
- [x] Add owner doc：`docs/design/projects/state-machine.md` 新增 §适用对象四（工时记录 ErpPrjTimesheet status 轴：状态定义/迁移矩阵 submit·approve·reject·cancel/终态 APPROVED/异常/可达性/cancel→UNSUBMITTED 漂移补注 + BizModel Javadoc 漂移补注）+ §适用对象五（项目结算单 ErpPrjProjectSettlement docStatus+approveStatus 双轴：双轴矩阵/doApprove 双轴同动/终态/APPROVED dict-value 漂移声明 + 共享 dict 死状态 OPEN·ON_HOLD·COMPLETED 保留声明）
  - Skill: `state-machine-business-review-prompt.md`
- [x] Fix owner-doc/dict 漂移登记（不改 ORM，属 ask-first 保护区）：(a) Settlement docStatus APPROVED 被写入但不在 `erp-prj/project-status` 字典——登记为 dict-value drift（对齐 CostCollection §适用对象三先例），保留行为，dict 补全/rebind 列 successor（ORM ask-first）；(b) Timesheet BizModel Javadoc `:27-28` 误述 reject→DRAFT/cancel→CANCELLED——就地修正 Javadoc 对齐 live code（reject→UNSUBMITTED/cancel→UNSUBMITTED）；(c) 登记建议 M0.2 路线图表 reconcile 此三项分类（M3→M4，§11.2 M3(iii)）
  - Skill: `state-machine-business-review-prompt.md`
- [x] Decision（落入 owner doc/计划）：Timesheet `cancel→UNSUBMITTED` = intentional legacy behavior（撤回语义，wf/approve-status 无 CANCELLED）；Settlement docStatus 共享 dict 死状态（OPEN/ON_HOLD/COMPLETED 对 Settlement 无 writer）= 保留为预留语义入口，不从 ORM 删除（对齐 projects §适用对象三 + assets 保留死状态先例）
  - Skill: `state-machine-business-review-prompt.md`

Exit Criteria:

- [x] 四方对照无未裁决漂移（cancel 漂移 + 共享 dict 死状态均裁定并落入 owner doc）
- [x] owner doc §适用对象四/五与 dict/Bean/代码一致

## Draft Review Record

- Independent draft review iteration 1: `needs revision` (`ses_00776de8c2ffe7O1FVuOQx3fQs0`) — 发现 1 governance blocker + 3 baseline majors：§11.2 M3(iii) 升级冲突未正面处理（Timesheet/Settlement approve 触发过账→应 M4 plan-first，引用的 purchase-approvestatus 先例可区分/不适用）；Timesheet cancel 红冲过账路径漏列；Settlement docStatus APPROVED dict-value 漂移（被写入但不在 dict）未登记；reverseSettlement 状态写可即时核定却留 Phase 2。v2 已：正面处理治理升级（重构为 M4 plan-first + 声明 §11.2 M4 硬约束 (i)–(v) + 人工门控前置 + inventory 漂移 Fix 登记）；补 cancel 红冲过账保留；补 APPROVED dict-value 漂移 Fix/Decision；核定 reverseSettlement=纯 posted 轴无状态写（Bean 无边）。
- Independent draft review iteration 2: `acceptable as a draft pending the M4 human gate` (`ses_007762b078ffe82myfxjPMw3W96`) — 治理 crux（M4 plan-first + §11.2 M4 (i)–(v) 声明 + 人工门控 honest framing + 保持 draft）经核定 CORRECT 且 COMPLETE；B1/M1/M2/M3 全部 RESOLVED（live code 实证）。仅余 1 个 text-consistency major：Deferred 段残留「M3 分类」「过账归对应 M4 项」stale 文案与 M4 重分类矛盾。v3 已修正 Deferred 段为「本计划已是 M4 plan-first，过账编排按 §11.2 M4 (ii)/(iv)/(v) 原序保留在 Processor，非另属对应 M4 项」。无剩余 blocker/major。
- Plan review（mission-driver 2026-08-13-080540）：`approved (review ran); held as draft` — 格式合规性/完备性/范围/结束证据四维度经核定全部就绪，无除门控外的 blocker/major。唯一 Blocker = §11.2 M4 (i) 人工/owner-doc 门控，为外部依赖：本计划触及 `accounting/finance postings` 保护域（ai-autonomy-policy.md:72 = plan-first，必需证据 owner doc + tests），且现状 owner doc `docs/design/projects/state-machine.md` 无 Timesheet/Settlement 章节（计划 Phase 3 拟补），依 ai-autonomy-policy.md:11「AI 编写文档不得作为清除保护域阻塞的证据，除非人工批准」+ project-context.md 会计保护域硬停止，审查者不可自主解除。故保持 `Plan Status: draft`（对齐 holding 机制），不晋升 active。门控解除后于本记录追加（日期 + 批准范围）并转 `active`。
- Plan review（mission-driver 2026-08-13-193118）：`approved (review ran); held as draft` — 复审四维度（格式合规性/完备性/范围/结束证据）全部就绪，无新增 blocker/major。经 live code 复核确认 M4 升级前提成立：`ErpPrjTimesheetApproveProcessor.java:42` approve 触发 `postingDispatcher.tryPost`（`:47-51` 回写 `posted=true`，`:56` 归集）；`ErpPrjProjectSettlementApproveProcessor.java:32-35` approve 触发 `createAndActivateAsset`（转固级联）+ `doPost`（过账）。唯一 Blocker 仍为 §11.2 M4 (i) 人工/owner-doc 门控（外部依赖，审查者不可自主解除）。保持 `Plan Status: draft` + `Review Hold`。
- **M4 plan-first 人工/owner-doc 门控状态：confirmed（2026-08-13）**（§11.2 M4 (i)）。草案审查已收敛。
- **M4 plan-first 门控确认记录（人工，2026-08-13）**：人工确认「以行为保持的矩阵集中化方式迁移此三轴、过账路径完整保留」可接受。门控解除，`Plan Status: draft → active`。M0.2 路线图表内分类 reconcile（M3→M4）作为 Phase 3 Fix 登记项保留（非阻塞）。

## Closure Gates

> 完整仓库验证在此处运行一次。无 ORM/API/字典变更（死状态 + APPROVED dict-value drift 保留不改绑，属 ask-first 保护区 successor），Compliance 基线预期无漂移。

- [x] 范围内行为完成（3 Bean + Timesheet/Settlement 接线 + 三层证据；过账编排时序/失败回退/红冲闭环完整保留，§11.2 M4 (ii)/(iv)/(v)）
- [x] 相关文档对齐（owner doc 新增 §适用对象四/五 + APPROVED dict-value/BizModel Javadoc/inventory 三类漂移 Fix/Decision 登记）
- [x] 已运行验证：`mvn test -pl module-projects/erp-prj-service`（133 green）+ Closure 时 `mvn clean install -DskipTests`（全 reactor BUILD SUCCESS）+ `bash docs/audits/nop-compliance-checker.sh`（exit 0，R5=0/R11=0/R12 无漂移/R2c=1392=基线）
- [x] **M4 plan-first 人工/owner-doc 门控已确认并记录于 Draft Review Record**（§11.2 M4 (i)；2026-08-13 人工确认，见 Draft Review Record 门控确认记录）
- [x] 无范围内项目降级为 deferred/follow-up（Deferred 项均为设计性 out-of-scope，非范围内降级）
- [x] 独立草案审查已完成并记录（Draft Review Record 5 轮收敛 + 人工门控确认）
- [x] 文本一致性已验证（plan/doc/Bean/代码四方一致；Deferred 段 M4 文案已对齐）
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计（独立子代理 `ses_004cd4b69ffe0JAoJa2YXOobu8` 审计结论 = **PASS**，无 blocker；证据见 Closure Audit Evidence）
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### Timesheet cancel→CANCELLED 目标态迁移

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 既有 cancel 语义为撤回（→UNSUBMITTED），`wf/approve-status` 无 CANCELLED 值。本计划保持既有行为，仅作 owner doc 漂移补注，不改语义。
- Successor Required: `yes`（PM 要求工时独立作废状态时，新增 dict 值 + cancel→CANCELLED 迁移 + BizMutation）

### projects §适用对象三 CRUD 桩（Milestone/Billing/CostCollection）状态机

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 已在 projects §适用对象三 Deferred（P1-MA2-069），零 writer CRUD 桩，非本工作项。
- Successor Required: `yes`（项目管理全面状态机需求时，见 §适用对象三 Successor 触发条件）

### 过账编排 / posted 契约（行为保持边界）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 本计划**已是 M4 plan-first**（审批轴 approve 触发会计过账，依 §11.2 M3(iii) 升级），过账编排时序/失败回退/红冲闭环按 §11.2 M4 (ii)/(iv)/(v) **原序保留在 Processor/`I*Biz` 路径、`posted` 不入轴**——这是本计划的硬约束而非另属「对应 M4 项」。此处仅登记：过账 dispatcher 内部实现细节（凭证科目映射、ReversalListener 细节）不在本迁移矩阵集中化范围内。
- Successor Required: `no`（行为保持已由本计划 M4 硬约束覆盖）

### 通用 CRUD 写入禁止 / Delta 覆盖证明

- Classification: `watch-only residual` / `optimization candidate`
- Why Not Blocking Closure: CRUD 写入边界 = M0.1 successor；M4 保护域单项不自带 Delta 证明，Delta 覆盖回归归 M5.3（§11.2 M4 Delta 适用性）。
- Successor Required: `no`（归 M0.1/M5.3）

## Closure

Status Note: 三 Phase 全部执行完成（Phase 1/2/3 items 全 [x]，Status=completed）。执行者验证：`mvn test -pl module-projects/erp-prj-service` 133 green；`mvn clean install -DskipTests` 全 reactor BUILD SUCCESS；`bash docs/audits/nop-compliance-checker.sh` exit 0（R5=0/R11=0/R12 无漂移/R2c=1392=基线）。独立结束审计由子代理执行（见下证据）。独立审计门控项随审计结论勾选。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理（general，新会话任务 `ses_004cd4b69ffe0JAoJa2YXOobu8`）。
- **审计结论：PASS（无 blocker）**。逐区核验（file:line 证据）：
  - Phase 1：3 Bean 严格无状态（无任何 @Inject）；矩阵正确（Timesheet assertCanCancel 故意不抛=行为保持，APPROVED 可逆终态；Settlement 双轴 reverseSettlement 无边）；3 Bean 在 `app-service.beans.xml:17-22` FQN 注册；层 1 矩阵测试复跑 31 green。
  - Phase 2：行为保持核验——(a) Timesheet approve `tryPost→if(posted) setPosted(true)→aggregateFromTimesheet` 顺序与过账不悬挂保留；(b) cancel 红冲在 status 写回前；(c) Settlement doApprove 双轴同写；(d) doPost `tryPost+if(posted)`；(e) reverseSettlement 未接线（纯 posted 轴）；(f) approve STRICT/RELAXED config 分支保留；(g) 错误码 + 实体编号保留；R5 `@Inject private`=0。
  - Phase 3：owner doc §适用对象四/五存在；BizModel Javadoc 漂移已修正；三项漂移 Fix/Decision 落入 owner doc/计划。
  - Closure gates：Phase items 全 [x] + Status=completed；Plan Status=completed；roadmap M3.10/11/12=done。
  - 已知失败模式自检：R5=0、过账不悬挂、执行者未自我审计（独立子代理审计）。
- 非阻塞 nit（审计建议，已采纳补注）：`ErpPrjTimesheetStateMachine.transitions()` 的 cancel 边仅列 {SUBMITTED, APPROVED}，而 `assertCanCancel` 对全部 dict 值放行——为有意的建模选择（UNSUBMITTED→UNSUBMITTED 为 no-op，REJECTED 不可达死状态），已在 owner doc §适用对象四 §6 补注澄清。
- Executor Evidence（执行者自证，供审计复核）:
  - **Phase 1**：3 Bean 新建——`ErpPrjTimesheetStateMachine`（status 单轴，5 边）、`ErpPrjProjectSettlementApprovalStateMachine`（approveStatus 轴，3 边）、`ErpPrjProjectSettlementDocumentStateMachine`（docStatus 轴，3 边）；`app-service.beans.xml` 注册 3 FQN bean；层 1 矩阵测试 `TestErpPrjTimesheetAndSettlementStateMachines` 31 tests green。
  - **Phase 2**：Timesheet `Submit/Approve/Cancel`Processor + BizModel `reject` inline + Settlement facade `validateTransitionFor*/do*` 接线双 Bean（行为保持：过账/红冲/转固/归集/错误码/双轴同动原序保留）；层 3 回归 `mvn test -pl module-projects/erp-prj-service` 133 green（含 TestErpPrjTimesheetCost 7 / TestErpPrjBudgetAndCollection 7 / TestErpPrjProjectSettlement 4 / TestPrjPostingFaultInjection 1 / TestTimesheetPostingFailureAlert 1）。
  - **Phase 3**：owner doc `docs/design/projects/state-machine.md` 新增 §适用对象四（Timesheet）+ §适用对象五（ProjectSettlement 双轴）；BizModel Javadoc 漂移就地修正（reject→UNSUBMITTED / cancel→UNSUBMITTED）；层 2 四方对照（三轴 × 10 维度）+ 三项漂移 Fix/Decision 落入 owner doc/计划。
  - **全仓验证**：`mvn clean install -DskipTests` BUILD SUCCESS；`nop-compliance-checker.sh` exit 0。

Follow-up:

- M0.2 路线图表内分类 reconcile（M3.10/M3.11/M3.12 标注 M3→M4，§11.2 M3(iii)）——非阻塞 inventory 文案修正（本计划已按 M4 plan-first 执行，分类漂移仅影响路线图标注层）。
- Settlement docStatus APPROVED dict-value 补全/rebind（ORM ask-first 保护区 successor，对齐 CostCollection §适用对象三）。
- Timesheet cancel→CANCELLED 独立作废态（PM 需求时 successor）。
