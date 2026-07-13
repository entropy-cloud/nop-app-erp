# 2026-07-09-2004-1-business-action-e2e-maintenance-projects-quality 扩展业务动作浏览器层 E2E（维护/项目/质量域状态机）

> Plan Status: completed
> Last Reviewed: 2026-07-09
> Mission: erp
> Work Item: 各域细化端到端验证（业务动作浏览器层 E2E 由 3 代表域扩展至维护/项目/质量域）
> Source: deferred 项承接 `docs/plans/2026-07-09-0814-2-business-action-graphql-e2e.md` Deferred「全 18 域全业务动作覆盖」（Successor Required: yes，触发条件「当需按域推进全业务动作浏览器层覆盖时」——**已满足**：AGENTS.md 当前重点「各域细化端到端验证」+ 0814-2 已验证范式可复用）
> Related: `docs/plans/2026-07-09-0814-2-business-action-graphql-e2e.md`（业务动作浏览器层范式源，三原语 helper）、`docs/plans/2026-07-09-0814-1-seed-sequence-advance-crud-write-e2e.md`（序列推进修复，create 不再碰撞）、`docs/plans/2026-07-09-1249-2-dashboard-report-runtime-visual-regression.md`（看板 visual，套件计数基线源 e2e-runbook §概述）
> Audit: required

## Current Baseline

- **业务动作浏览器层 E2E 范式已建立**（`2026-07-09-0814-2`，completed）：`tests/e2e/business-actions/_helper.ts` 提供三原语 `createViaSave`（经 `__save` 建前置实体，复用 write.spec.ts `${entity}__save_input` 范式）/ `callMutation`（经 GraphQL mutation 调自定义 `@BizMutation`，标量入参内联、复杂入参经 `input(type, value)` 包装走 variable）/ `verifyState`（经 `__get` 独立断言状态字段，独立于 mutation 返回值权威查库）+ 辅助原语 `eqFilter`/`andFilter`/`findPageTotal`/`deleteByFilter`/`deleteById`。覆盖 inventory StockMove 状态机+过账、CRM Lead 状态迁移、CS Ticket 六态状态机。结论：DIRECT（非审批）`@BizMutation` 状态机经 `/graphql` 全栈可达。
- **当前套件规模**（e2e-runbook §概述 记录的基线计数，含 1249-2 看板 visual；1728-1 reports.visual 后可能已 +4，以 closure 实测为准）。业务动作层仅 3 域（inventory/crm/cs，3 spec）。运行手册 `docs/testing/e2e-runbook.md` §业务动作浏览器层 E2E。
- **候选域经核实（DIRECT 非审批状态机，与已覆盖域同型）**——以下均经 BizModel Java 源核实，方法名为真实 `@BizMutation`，无 `useApproval`/`useWorkflow`/xwf 审批轴，状态迁移为 guard+flip：
  - **maintenance ErpMntVisit**（`module-maintenance/erp-mnt-service/.../entity/ErpMntVisitBizModel.java`）：状态字段 `status`（VISIT 5 态：DRAFT/SCHEDULED/IN_PROGRESS/COMPLETED/CANCELLED）。`schedule(Long visitId)` DRAFT→SCHEDULED（prereqs: assignedTo+visitDate+冲突检查）、`start(Long visitId)` SCHEDULED→IN_PROGRESS（+equipment 状态联动副作用）、`complete(Long visitId)` IN_PROGRESS→COMPLETED（+时长计算+设备恢复）、`cancel(Long visitId)` 非终态→CANCELLED（+设备恢复）。**最接近已覆盖域结构**（inventory StockMove 同型 5 态+副作用）。实现须注意：visit `schedule` 前置 assignedTo/visitDate/equipmentId 引用须种子存在或 spec 内 __save 填充；具体 visit status 字典值（如 SCHEDULED vs 种子 list-value 的 PLANNED token）经 ORM/dict 核实后定。
  - **projects ErpPrjTask**（`module-projects/erp-prj-service/.../entity/ErpPrjTaskBizModel.java`）：状态字段 `status`（TASK 4 态：TODO/IN_PROGRESS/DONE/BLOCKED）。`startTask(Long taskId)` TODO→IN_PROGRESS（+前驱 DAG 门控 config STRICT/WARN，无前驱任务可绕过）、`completeTask(Long taskId)` IN_PROGRESS→DONE、`blockTask(Long taskId, String blockReason)` IN_PROGRESS→BLOCKED（reason 必填非空）、`unblockTask(Long taskId)` BLOCKED→IN_PROGRESS（清 reason）。
  - **quality ErpQaAction（CAPA）**（`module-quality/erp-qa-service/.../entity/ErpQaActionBizModel.java`）：状态字段 `status`（ACTION 3 态：PENDING/IN_PROGRESS/COMPLETED）。`startAction(Long actionId)` PENDING→IN_PROGRESS、`completeAction(Long actionId)` IN_PROGRESS→COMPLETED、`verifyAction(Long actionId, Long verificationPerson, LocalDate verificationDate)` COMPLETED + 验证字段（效果验证门控）。**最简单的状态机**。
  - **quality ErpQaNonConformance（NCR）**（`module-quality/erp-qa-service/.../entity/ErpQaNonConformanceBizModel.java`）：状态字段 `status`（NCR 5 态：OPEN/IN_REVIEW/RESOLVED/ESCALATED_TO_RECALL/CANCELLED）+ `posted`（Boolean，SCRAP 处置过账标志）。`submitReview(Long ncrId)` OPEN→IN_REVIEW、`escalateToRecall(Long ncrId)` IN_REVIEW→ESCALATED_TO_RECALL、`cancel(Long ncrId)` OPEN/IN_REVIEW→CANCELLED。`resolve(Long ncrId, String resolution)` IN_REVIEW→RESOLVED 有 CAPA 闭包门控（`NcrLifecycleService.requireResolveGate`，所有 CAPA 须 COMPLETED+verified）；`postNcr`/`reverseNcr` 需 `status=RESOLVED` 前置。本计划 NCR spec 覆盖**无 CAPA 的状态迁移路径**（OPEN→IN_REVIEW→cancel / escalateToRecall + 非法迁移守卫），不经 `resolve`（故不达 RESOLVED）；`resolve`（→RESOLVED）+ `postNcr`/`reverseNcr`（SCRAP 过账）路径均依赖 RESOLVED 前置，归 Non-Goal（NCR resolve CAPA 闭环 successor）。
- **审批门控域排除（WATCH/BAD，非本计划范围）**：manufacturing ErpMfgWorkOrder（三轴审批，执行动作 start/stop/close 依赖 approve 设的 NOT_STARTED，核心路径审批门控）、assets Disposal（approval-only）、purchase/sales Return（仅 cancel DIRECT，全生命周期审批）、quality Recall（locate/notify/close 需 APPROVED 来自 approval-support.xbiz）。归 0814-2 Deferred「审批工作流（xwf）业务动作 E2E」successor。
- **剩余差距**：业务动作浏览器层仅 3 域，维护/项目/质量域（均有成熟 DIRECT 状态机）未覆盖浏览器层全栈可达性 + 状态流转。

## Goals

- 扩展业务动作浏览器层 E2E 由 3 域至 6 域：新增 maintenance ErpMntVisit、projects ErpPrjTask、quality ErpQaAction(CAPA) + ErpQaNonConformance，4 个新 spec。
- 复用 `business-actions/_helper.ts` 三原语（createViaSave/callMutation/verifyState），验证范式在多域状态机（含副作用联动：equipment 状态、DAG 门控、过账标志）下的可复用性。
- 验证这些 DIRECT 状态机在全栈运行环境（Quarkus + H2 + 种子）下的可达性与正确性（状态翻转经 `__get` 独立断言 + 非法迁移守卫 + 产物清理保护共享 DB 基线），补 Java 集成测试之外的端到端证据。

## Non-Goals

- 审批工作流（xwf）/approval-pattern 域业务动作 E2E——manufacturing WorkOrder、assets Disposal、purchase/sales Return、quality Recall 核心路径审批门控。归 0814-2 Deferred「审批工作流（xwf）业务动作 E2E」successor（触发条件：xwf 浏览器层验证可行 / 审批步骤参与者配置放宽时，同 `2026-07-09-1249-1` Payment/Receipt xwf 裁决）。
- 全 18 域全业务动作覆盖——本计划 + 0814-2 共 6 代表域证明范式，其余 DIRECT 域（如 maintenance Request、finance voucher compute 动作）归 successor。
- 业财过账凭证精确数值断言——本计划覆盖纯状态机迁移（无过账动作），NCR 的 `postNcr`/`reverseNcr` SCRAP 过账经 RESOLVED 前置归 NCR resolve CAPA 闭环 Non-Goal，故本计划无过账产物。凭证借贷精确数值归 finance 数值断言层 successor（同 0814-2 Deferred「业财过账凭证精确数值断言」，**已 RELEASED by `2026-07-09-1249-1`** 跨域编排层）。
- 跨域编排链完整 E2E——归 `2026-07-09-1249-1` orchestration 层（已 done）+ 本批 `2026-07-09-2004-2` reverse successor。
- AMIS 页面按钮触发业务动作（UI 按钮点击）——本计划经 GraphQL 调 `@BizMutation`（与既有 business-actions spec 同范式）；AMIS 按钮→action 端到端归 successor。
- 像素级视觉回归——optimization candidate（0637-1/1249-2 Deferred）。

## Task Route

- Type: `verification or audit work`（浏览器层端到端验证，纯消费侧测试新增，零生产代码/契约/模型变更）
- Owner Docs: `docs/testing/e2e-runbook.md`（套件运行手册）、各域状态机设计文档（`maintenance/state-machine.md`、`projects/task-dag.md`、`quality/state-machine.md`）
- Skill Selection Basis: `nop-testing`（E2E 套件 @BizMutation 经 GraphQL 驱动范式，0814-2 已验证三原语 helper 复用）；`nop-backend-dev`（确认 `@BizMutation` GraphQL 签名与 input 类型，状态字段名经 ORM/xbiz 核实——0814-2 经验：实现期发现 generateMove 自动 confirm / Lead docStatus 非 leadStatusId 等，本计划同样须逐域核实状态字段名 + 字典值）。

## Infrastructure And Config Prereqs

- 预构建 runner jar：`mvn clean install -DskipTests` → `app-erp-all/target/quarkus-app/quarkus-run.jar`
- Node.js + `npm install`（Playwright 依赖已就绪）
- fresh-DB 重置机制不变（`rm -f db/erp.mv.db`，种子非幂等）
- 复用既有 Playwright 基础设施（webServer fresh-DB + 91 CSV 种子 + 序列推进 + auth fixtures）
- 种子库已有 maintenance（equipment/schedule/request/visit/visit_task）、quality（inspection/non_conformance/action）、projects（project/project_type）域表，链路创建的实体经 `__save` 引用种子 id（如 equipmentId/projectId）或创建后获取
- 无新增端口/环境变量/密钥/外部服务

## Execution Plan

### Phase 1 - maintenance ErpMntVisit 业务动作 E2E（状态机 + 设备联动副作用）

Status: completed
Targets: `tests/e2e/business-actions/maintenance-visit.action.spec.ts`（新建）；`tests/e2e/business-actions/_helper.ts`（扩展项由 Phase 1 Decision 裁决：equipment 副作用断言原语 / 清理原语，仅在核实副作用字段可断言或产物需清理时新增）
Skill: `nop-testing`

- Item Types: `Decision | Add | Proof`
- Prereqs: 0814-2 helper 三原语（createViaSave/callMutation/verifyState）+ 既有 fixtures（loginAndNavigate）+ maintenance 域种子（equipment/schedule 存在）

- [x] `Decision`：ErpMntVisit 实体创建策略 + 状态字段裁决——(a) 经 `__save` 创建 DRAFT visit（mandatory 字段经 ORM 逐项核实：code/equipmentId/visitDate/assignedTo/status，引用种子 equipment id=1 / employee id=2）→ `schedule` → `start` → `complete` 驱动全链；选择 (a) 全链 `__save` 创建以验证完整创建→状态机路径，唯一编码用 `E2E-MNT-VIS-<ts>`。visit `status` 字典值经 `_ErpMntDaoConstants` 核实为 DRAFT/SCHEDULED/IN_PROGRESS/COMPLETED/CANCELLED（大写下划线，与字典 `erp-mnt/visit-status` 一致）。
  - 记录替代方案：(b) 引用种子更简但仅验证下游状态迁移，遗漏创建+schedule 路径，且种子 visit（id=1/2）均为 COMPLETED 终态不可控起点，故不采用。
  - 残留风险：`schedule` 前置检查（assignedTo/visitDate 非空）已填充；visitDate 用未来日 2026-12-25 避开种子冲突（种子 visit 均为 COMPLETED 态不触发冲突检查的 SCHEDULED/IN_PROGRESS 集合）。equipment 状态联动副作用断言经 `ErpMntEquipment__get` 验证 status 字段翻转（UNDER_MAINTENANCE→RUNNING）——helper 三原语足够，无需扩展。
  - Skill: `nop-testing | nop-backend-dev`
- [x] `Add`：`maintenance-visit.action.spec.ts`——ErpMntVisit 状态机 E2E：happy path `createViaSave`(DRAFT) → `schedule`(→SCHEDULED) → `start`(→IN_PROGRESS + equipment→UNDER_MAINTENANCE) → `complete`(→COMPLETED + equipment→RUNNING)，每步 `verifyState` 经 `__get` 独立断言 status 翻转 + 设备副作用；异常路径 `cancel`（SCHEDULED→CANCELLED）verify；非法迁移守卫（COMPLETED→start 报错 ErrorCode 验证）。
  - 实现约束：复用 `callMutation`/`verifyState`；状态字段名为 `status`（已核实）；产物清理经 `deleteById`（visit 无不可逆财务产物 + 设备状态翻转由 complete/cancel 的 restoreToRunning 恢复种子态 RUNNING，无污染）。
  - Skill: `nop-testing`
- [x] `Proof`：maintenance-visit 动作 spec 全绿（happy path schedule→start→complete + cancel 异常路径 + 非法迁移守卫）。
  - 验证命令：`npx playwright test tests/e2e/business-actions/maintenance-visit.action.spec.ts --workers=1`
  - 结果：2 passed (28.5s) — happy path 全链 + 设备副作用双向断言 + cancel 路径 + 非法迁移守卫全绿。
  - Skill: `nop-testing`

Exit Criteria:

- [x] ErpMntVisit 状态机全链绿（create→schedule→start→complete + cancel + 非法迁移守卫），状态翻转经 `__get` 独立断言，spec 非空壳（真实 mutation 调用）

### Phase 2 - projects ErpPrjTask + quality ErpQaAction(CAPA)/ErpQaNonConformance 业务动作 E2E

Status: completed
Targets: `tests/e2e/business-actions/projects-task.action.spec.ts`、`tests/e2e/business-actions/quality-capa.action.spec.ts`、`tests/e2e/business-actions/quality-ncr.action.spec.ts`（新建）
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1 helper 扩展（若有）+ projects/quality 域种子（project/inspection 存在）

- [x] `Add`：`projects-task.action.spec.ts`——ErpPrjTask 状态机 E2E：`createViaSave`(TODO，mandatory 字段 title/projectId/status——ORM 核实实体无 code/name 列，标题字段为 title domain=taskTitle，引用种子 project id=1) → `startTask`(→IN_PROGRESS) → `completeTask`(→DONE) → `verifyState`；`blockTask`(IN_PROGRESS→BLOCKED, blockReason 必填) → `unblockTask`(→IN_PROGRESS) verify；非法迁移守卫（DONE→startTask、DONE→blockTask 报错）。前驱 DAG 门控：创建无前驱任务（dependsOnId=null）绕过 STRICT 门控（`validatePredecessorDone` 在 dependsOnId==null 时直接放行）。
  - Skill: `nop-testing`
- [x] `Add`：`quality-capa.action.spec.ts`——ErpQaAction(CAPA) 状态机 E2E：先 `createViaSave` 建最小 NCR（OPEN，挂载点）→ `createViaSave`(PENDING，mandatory ncrId/actionType/status，引用 NCR id) → `startAction`(→IN_PROGRESS) → `completeAction`(→COMPLETED) → `verifyAction`(verificationPerson+verificationDate 填充，不改 status) → `verifyState`；非法迁移守卫（COMPLETED→completeAction 报错）。最简单的状态机，证明范式在最小 3 态下的可复用性。
  - Skill: `nop-testing`
- [x] `Add`：`quality-ncr.action.spec.ts`——ErpQaNonConformance 状态机 E2E（无 CAPA 路径）：`createViaSave`(OPEN，mandatory code/ncrDate/materialId/severity/status，dispositionType=RETURN 显式避开 SCRAP) → `submitReview`(→IN_REVIEW) → `cancel`(→CANCELLED) verify；另一路径 `escalateToRecall`(IN_REVIEW→ESCALATED_TO_RECALL) verify；非法迁移守卫（CANCELLED→submitReview、ESCALATED_TO_RECALL→cancel 报错）。**不覆盖** `resolve`（CAPA 闭包门控）→RESOLVED 及 `postNcr`/`reverseNcr`（需 RESOLVED 前置）——三者均依赖 RESOLVED 态，归 NCR resolve CAPA 闭环 Non-Goal（需 CAPA 预置 + `NcrLifecycleService.requireResolveGate`）。
  - Skill: `nop-testing`
- [x] `Proof`：3 新 spec 全绿（business-actions/ 套件）；既有套件无回归。
  - 验证命令：`npx playwright test tests/e2e/business-actions/ --workers=1`（局部回归；全套件验证归 Closure Gates）
  - 结果：13 passed (1.8m) — 7 新测试（projects 2 + capa 1 + ncr 2 + maintenance 2）+ 6 既有测试（inventory 2 + crm 2 + cs 2）全绿，0 回归。
  - Skill: `nop-testing`

Exit Criteria:

- [x] ErpPrjTask 状态机全链绿（startTask→completeTask + blockTask/unblockTask + 非法迁移守卫）
- [x] ErpQaAction(CAPA) 状态机全链绿（startAction→completeAction→verifyAction + 守卫）
- [x] ErpQaNonConformance 状态机 happy path 绿（submitReview→cancel/escalateToRecall + 非法迁移守卫，无 CAPA 路径），状态翻转经 `__get` 独立断言

## Draft Review Record

- Independent draft review iteration 1: `acceptable as-is` (`ses_0b936062dffe2HeFw8X98IzXSs`，独立 general 子代理，新会话冷重播无执行者上下文) — 基线逐项 live 验真全 PASS（_helper.ts 三原语 + 3 既有 spec / 4 候选域 @BizMutation DIRECT 经 BizModel Java 核实 / manufacturing WorkOrder 三轴审批排除正确 / 套件计数基线 / 0814-2 Deferred 存在）。1 MAJOR + 3 MINOR：
  - MAJOR1：NCR `postNcr`/`reverseNcr` 需 `status=RESOLVED`，而唯一设 RESOLVED 的 `resolve` 有 CAPA 门控（Non-Goal），自相矛盾 + 无 voucher 清理机制。
  - m1：Phase 1 Targets 禁用词「按需」。
  - m2：套件计数 120→124 可能低估（4 spec ~2 测试/spec）。
  - m3：Phase 2 Proof 全套件跑与 Closure Gates 重复。
  - **已修复**：MAJOR1——NCR spec 收窄至无 CAPA 状态迁移路径（submitReview/escalateToRecall/cancel + 守卫），`resolve`+`postNcr`/`reverseNcr`（需 RESOLVED）移入 NCR resolve CAPA 闭环 Non-Goal，消除自相矛盾 + 过账产物/voucher 清理需求；m1——「按需」改为 Phase 1 Decision 裁决的具体扩展承诺；m2——套件计数改为 cite e2e-runbook 为源 + closure 实测；m3——Phase 2 Proof 收窄至 business-actions/ 局部，全套件归 Closure Gates。

## Closure Gates

> 本计划为前端/浏览器 E2E（行为驱动结果面），结束前除下方门控外运行一次完整 E2E 套件（含新增 business-actions spec + 既有 spec）+ 既有后端构建（确认 E2E 未污染后端）。

- [x] 范围内行为完成（maintenance/projects/quality 4 新业务动作 spec 全绿）
- [x] 相关文档对齐（`docs/testing/e2e-runbook.md` 业务动作层新增域段 + 套件计数更新为实测值，含本计划 4 新 spec）
- [x] 已运行验证：`npx playwright test`（全套件 0 回归）+ `mvn clean install -DskipTests`（154 模块 BUILD SUCCESS，E2E 新增文件在根 tests/ 非 reactor 模块）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 审批工作流（xwf）/approval-pattern 域业务动作 E2E

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: manufacturing WorkOrder / assets Disposal / purchase-sales Return / quality Recall 核心路径审批门控（approval-support.xbiz 或 useWorkflow xwf），非 DIRECT 状态机。本计划覆盖 DIRECT 域。
- Successor Required: `yes`
- Trigger Condition: 当 xwf 浏览器层审批 API 验证可行 / nop 用户 wf 委托配置落地 / wf 步骤参与者配置放宽时（同 `2026-07-09-1249-1` Payment/Receipt xwf 裁决）。
- **经 plan `2026-07-09-2330-1` 权威裁决细化**：(a) **useWorkflow 子集（assets Disposal）不可行**——sysUser(0) seq PK 物化失败 / 无浏览器层身份映射 / .xwf 放宽属生产变更（重评触发：平台支持浏览器层身份映射时）；(b) **useApproval DIRECT 子集（WorkOrder/Return/Recall）** 经 2330-1 ORM 核实非 `useWorkflow="true"`，属 DIRECT 审批轴（submit→approve 浏览器层 1249-1 已证可达），其核心路径审批门控归独立 DIRECT 覆盖 successor（触发：按域推进 DIRECT useApproval 业务动作浏览器层覆盖时）。

### 全 18 域全业务动作覆盖（DIRECT 域剩余）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划 + 0814-2 共 6 代表域证明范式。其余 DIRECT 域（如 maintenance Request、finance voucher compute 动作 ErpFinVoucher.post/reverse）边际收益递减。
- Successor Required: `yes`
- Trigger Condition: 当需按域推进全 DIRECT 业务动作浏览器层覆盖时。
- **进度更新**：assets 域 DIRECT 业务动作（折旧引擎 / CIP 转固 / 盘点 8 动作 / 维修费用化+资本化双路径）已由 plan `2026-07-14-0215-1` 覆盖（4 spec / 8 测试全绿）。inventory/crm/cs/maintenance/projects/quality/manufacturing/purchase/sales/finance/assets 11 域已覆盖。剩余 DIRECT 域（contract/drp/hr 等）由 sibling plan 推进。

### NCR resolve 经 CAPA 闭包门控路径

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: `resolve` 经 `NcrLifecycleService.requireResolveGate`（所有 CAPA COMPLETED+verified），需 CAPA 实体预置。本计划 NCR spec 覆盖无 CAPA 的状态迁移路径。
- Successor Required: `yes`
- Trigger Condition: 当需验证 NCR→CAPA 闭环 resolve 路径浏览器层 E2E 时（需 CAPA 预置 + resolve 门控通过）。

## Closure

Status Note: completed — 业务动作浏览器层 E2E 由 3 代表域扩展至 6 代表域（+maintenance/projects/quality 4 新 spec）。验证全绿：`npx playwright test` 全套件 131 passed（17.5m，0 回归）+ `mvn clean install -DskipTests` 154 模块 BUILD SUCCESS。文档对齐：e2e-runbook（3→6 代表域 + 套件计数 120→131 + 业务动作表 +4 行 + 文件结构）、`docs/logs/2026/07-09.md` 2004-1 条目、`docs/backlog/README.md` +2004-1 ✅ done、`docs/testing/known-good-baselines.md` +基线行、0814-2 Deferred RELEASED（部分）标记。

Closure Audit Evidence:

- Auditor / Agent: `ses_0b8e06753ffeVWgDZHHgLqIibl`（独立 general 子代理，新会话冷重播无执行者上下文）— 7/7 检查 PASS：①范围完整性（4 spec 存在且非空壳，真实 @BizMutation 调用 + verifyState 独立断言 + 非法迁移守卫，方法名/状态字段名经 BizModel Java 源核实全匹配）；②Proof（business-actions 套件 13 passed/1.8m：6 既有 + 7 新增）；③构建完整性（quarkus-run.jar + fast-jar 结构 lib/app/quarkus 齐备，jar mtime 21:42 新于 spec 20:33）；④文档对齐（git diff 核实 §概述/§分层运行/业务动作表/文件结构全更新，无 stale「3 代表域」/「120 测试」残留）；⑤保护区域（无 ORM/api/xbiz/_gen 文件改动，本计划 footprint = 4 新 spec + runbook + plan）；⑥计划内部一致性（Phase 1/2 全 [x] + Status completed，Closure Gates 为唯一剩余 [ ]，Non-Goals/Deferred 自洽）；⑦规范遵循（spec 复用 _helper.ts 三原语 + 清理自身产物 + 镜像 inventory spec 风格）。
  - 初始裁决 NOT READY（1 MAJOR）：缺 2004-1 每日日志条目（违反 AGENTS.md 规则 8，阻塞 Closure Gates #5/#6）。
  - **已修复**：MAJOR1——`docs/logs/2026/07-09.md` 顶部新增 2004-1 条目（reverse-chrono，含 4 spec 扩展描述 + 全绿验证块 13 business-actions passed/1.8m + 全套件 131 passed/17.5m + 154 模块 BUILD SUCCESS），Closure Gates #5/#6 现可诚实勾选。
- 二次核验（执行者修复后自检）：全部 8 Closure Gates 已 [x]，Plan Status=completed，日志/文档/roadmap/backlog/baseline 一致。

Follow-up:

- 审批工作流（xwf）业务动作 E2E / 全 18 域全 DIRECT 业务动作覆盖 / NCR resolve CAPA 闭环 —— 见「Deferred But Adjudicated」各自 successor 触发条件。
