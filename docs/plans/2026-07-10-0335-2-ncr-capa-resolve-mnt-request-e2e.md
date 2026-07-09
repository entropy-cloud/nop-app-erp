# 2026-07-10-0335-2 依赖门控与剩余 DIRECT 业务动作浏览器层 E2E

> Plan Status: active
> Last Reviewed: 2026-07-10
> Source: deferred 项承接 `docs/plans/2026-07-09-2004-1-business-action-e2e-maintenance-projects-quality.md` Deferred「NCR resolve 经 CAPA 闭包门控路径」（Successor Required: yes，触发条件「当需验证 NCR→CAPA 闭环 resolve 路径浏览器层 E2E 时」）+ Deferred「全 18 域全业务动作覆盖（DIRECT 域剩余）」maintenance Request 子集
> Related: `docs/plans/2026-07-10-0335-1-approval-gated-direct-business-action-e2e.md`（同批，审批轴 E2E，先执行）、`docs/plans/2026-07-09-0814-2-business-action-graphql-e2e.md`（business-actions 原语基线）、`docs/plans/2026-07-09-2004-1`（quality-capa + quality-ncr 无 CAPA 路径基线）
> Audit: required

## Current Baseline

**已落地 E2E 覆盖（business-actions 层）**：`2004-1` 已交付 `quality-ncr.action.spec.ts`（NCR 无 CAPA 路径：submitReview/escalateToRecall/cancel）+ `quality-capa.action.spec.ts`（CAPA 3 态：startAction/completeAction/verifyAction）。本计划在此基础上补齐两条 DIRECT 业务动作路径。

**剩余差距 1 — NCR→CAPA 闭包 resolve 门控路径**：

`ErpQaNonConformanceBizModel.resolve(ncrId, resolution)`（`:82-101`）经 `NcrLifecycleService.requireResolveGate(ncrId, code)`（`:131-136`）门控：须全部关联 `ErpQaAction`（CAPA）`status=COMPLETED` 且 `verificationPerson`+`verificationDate` 已填。任一 CAPA 未闭包抛 `ERR_NCR_RESOLVE_CAPA_NOT_COMPLETED`。`2004-1` quality-ncr spec 明示此路径归 successor（需 CAPA 预置：startAction→completeAction→verifyAction 三步前置）。`resolve` 成功后 status=RESOLVED；若 dispositionType=SCRAP 则 `postNcr`（config-gated）触发过账。

**剩余差距 2 — maintenance Request 状态机**：

`ErpMntRequestBizModel`（`module-maintenance/erp-mnt-service`）5 态状态机：`accept`:31（OPEN→ACCEPTED，生成 ErpMntVisit）/ `startRepair`:41（ACCEPTED→IN_PROGRESS）/ `complete`:50（IN_PROGRESS→COMPLETED）/ `rejectRequest`:59（OPEN/ACCEPTED→REJECTED）/ `cancel`:72（OPEN/ACCEPTED→CANCELLED）。状态字段 `status`（dict `erp-mnt/request-status`）。

> **ORM 异常注记**：ErpMntRequest 标 `use-approval` tagSet 且 xbiz extends `approval-support.xbiz`，但 ORM 无 `approveStatus` 列——生命周期完全由自定义 `status` 状态机驱动。本计划验证自定义 `status` 迁移路径，不调用平台 `submitForApproval`/`approve`（无列可写）。

**e2e helper 原语已就绪**：`createViaSave`/`callMutation`/`callMutationOk`/`verifyState`/`deleteById`（`tests/e2e/business-actions/_helper.ts`）。NCR/CAPA 实体经 `2004-1` quality-capa + quality-ncr spec 已验证可达。

## Goals

- 新增 `quality-ncr-resolve-capa-gate.action.spec.ts`：验证 NCR→CAPA 闭包 resolve 门控——CAPA 未闭包时 `resolve` 抛 `ERR_NCR_RESOLVE_CAPA_NOT_COMPLETED`（errors 断言）；CAPA 全部 COMPLETED+verified 后 `resolve` 成功 status=RESOLVED
- 新增 `mnt-request.action.spec.ts`：验证 ErpMntRequest 5 态状态机——accept→startRepair→complete 正路径 + rejectRequest/cancel 路径 + 非法迁移守卫
- 验证 CAPA 门控作为依赖前置的业务约束经浏览器层可观测（不止 CRUD 状态机，而是跨实体依赖门控）

## Non-Goals

- NCR `postNcr`/`reverseNcr`（SCRAP 过账，需 RESOLVED + config-gated auto-post off）——归过账数值断言 successor；本计划仅验证 resolve 门控 + status=RESOLVED
- maintenance Request `accept` 生成的 ErpMntVisit 副作用编排——归 orchestration successor；本计划仅断言 Request 自身 `status` 迁移
- finance voucher 手工 `post`/`reverse` 业务动作——`reverse` 已在 `2004-2` orchestration 反向链覆盖；`post` 入参为复杂 `PostingEvent`、边际收益递减（`2004-1` Deferred 明示），归独立 successor
- 全 18 域逐实体全业务动作覆盖——本计划补齐最高价值两路径，其余边际域按需推进

## Task Route

- Type: `verification work`（浏览器层 E2E 覆盖扩展，无生产代码变更）
- Owner Docs: `docs/testing/e2e-runbook.md`（业务动作层），`docs/design/quality/state-machine.md`（NCR/CAPA resolve 门控）、`docs/design/maintenance/state-machine.md`（Request 状态机）
- Skill Selection Basis: `none`（复用既有 Playwright E2E 范式 + `_helper.ts` 三原语，无后端变更）

## Infrastructure And Config Prereqs

No infra prereqs beyond existing baseline.

## Execution Plan

### Phase 1 - NCR→CAPA 闭包 resolve 门控 E2E

Status: planned
Targets: `tests/e2e/business-actions/quality-ncr-resolve-capa-gate.action.spec.ts`
Skill: `none`

- Item Types: `Add | Proof`
- Prereqs: `2004-1` quality-capa + quality-ncr spec 已落地（CAPA 三步 + NCR 无 CAPA 路径基线）

- [ ] `Add`：新建 `quality-ncr-resolve-capa-gate.action.spec.ts`——`createViaSave` 建 NCR（code 保唯一，dispositionType 选 RETURN 避免自动过账，status=OPEN）+ `createViaSave` 建关联 ErpQaAction（CAPA，ncrId=上面 NCR id，status=PENDING）
- [ ] `Proof`：门控负路径——NCR `submitReview`(OPEN→IN_REVIEW) → `resolve`（CAPA 仍 PENDING 未闭包）→ 断言 errors 非空 + 含 `ERR_NCR_RESOLVE_CAPA_NOT_COMPLETED` → `verifyState status` 仍 IN_REVIEW（未迁移）
- [ ] `Proof`：CAPA 闭包——`callMutationOk startAction`(PENDING→IN_PROGRESS) → `callMutationOk completeAction`(→COMPLETED) → `callMutationOk verifyAction`(填 verificationPerson+verificationDate，:55-76) → `verifyState` CAPA status=COMPLETED
- [ ] `Proof`：门控正路径——`callMutationOk resolve`(ncrId, resolution) → `verifyState status=RESOLVED`（全部 CAPA 闭包后门控通过）
- [ ] `Proof`：清理——NCR + CAPA 自身删除（CAPA 先于 NCR 删，因 FK 依赖）

Exit Criteria:

- [ ] 门控负路径（CAPA 未闭包 → resolve 抛错 + status 不变）+ 正路径（CAPA 闭包 → resolve 成功 + status=RESOLVED）均通过
- [ ] CAPA 三步（start→complete→verify）+ resolve 经 `verifyState` 独立断言

### Phase 2 - maintenance Request 5 态状态机 E2E

Status: planned
Targets: `tests/e2e/business-actions/mnt-request.action.spec.ts`
Skill: `none`

- Item Types: `Add | Proof`
- Prereqs: 无

- [ ] `Add`：新建 `mnt-request.action.spec.ts`——`createViaSave` 建 ErpMntRequest（mandatory code+requestDate+equipmentId+description+status=OPEN，code 保唯一；equipmentId 引用种子设备）
- [ ] `Proof`：正路径——`callMutationOk accept`(OPEN→ACCEPTED) → `verifyState status=ACCEPTED` → `startRepair`(→IN_PROGRESS) → `complete`(→COMPLETED + completedAt)
- [ ] `Proof`：分支路径——`rejectRequest`(OPEN/ACCEPTED→REJECTED) + `cancel`(OPEN/ACCEPTED→CANCELLED)
- [ ] `Proof`：非法迁移守卫——如 COMPLETED 再 accept → 断言 errors 非空
- [ ] `Proof`：清理——Request 自身删除（若 accept 生成 ErpMntVisit 副作用，按需清理或接受种子级隔离）

Exit Criteria:

- [ ] 5 态状态机正路径（accept→startRepair→complete）+ 分支路径（rejectRequest/cancel）+ 守卫全通过
- [ ] `status` 翻转经 `verifyState` 独立断言

## Draft Review Record

- Independent draft review iteration 1: accept (draft review 2026-07-10) — format compliant with template; all required sections present; Phase structure valid with phase-level Item Types and Skill declarations; Exit Criteria clear and testable for both phases (NCR→CAPA gate negative+positive paths, maintenance 5-state transitions); scope tightly bounded to 2 spec files under same result surface (browser-layer E2E business-actions) with no scope creep; 3 deferred items properly adjudicated with successor trigger conditions; Non-Goals explicitly exclude postNcr/reverseNcr/Visit orchestration/voucher post; Closure Gates define sufficient evidence (2 specs green + validation commands + independent closure audit); no Blocker or Major issues found. Minor notes (non-blocking): `mvn clean install -DskipTests` redundant for no-Java-change plan but follows project convention; line 83 cleanup tactic uses "按需" language (acceptable as test-cleanup instruction, not scope).

## Closure Gates

- [ ] 范围内行为完成（2 spec 全绿：quality-ncr-resolve-capa-gate + mnt-request）
- [ ] 相关文档对齐（`docs/testing/e2e-runbook.md` 业务动作层新增 2 实体段 + 套件计数更新；`docs/logs/2026/07-10.md` 聚合条目）
- [ ] 已运行验证：`npx playwright test tests/e2e/business-actions/`（新 spec 全绿，0 回归）+ `mvn clean install -DskipTests`（154 模块 BUILD SUCCESS）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### NCR postNcr/reverseNcr SCRAP 过账数值断言

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: `postNcr`（RESOLVED + config-gated auto-post off 手动入口）触发 SCRAP 报废损失凭证。本计划验证 resolve 门控 + status=RESOLVED。过账凭证数值断言属 finance 数值断言层。
- Successor Required: `yes`
- Trigger Condition: 当需对 NCR SCRAP 过账凭证行做精确数值断言时。

### maintenance Request accept 生成的 ErpMntVisit 副作用编排

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: `accept` 生成响应式 ErpMntVisit。本计划仅断言 Request 自身 `status` 迁移。
- Successor Required: `yes`
- Trigger Condition: 当需推进 maintenance Request→Visit 编排 E2E 时。

### finance voucher 手工 post 业务动作

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: `reverse` 已在 `2004-2` orchestration 覆盖；`post` 入参复杂 `PostingEvent`、边际收益递减（`2004-1` Deferred 明示）。
- Successor Required: `yes`
- Trigger Condition: 当需验证 voucher 手工过账浏览器层可达性时。

## Closure

Status Note: <pending>

Closure Audit Evidence:

- <pending>

Follow-up:

- NCR SCRAP 过账数值断言 / maintenance Request→Visit 编排 / finance voucher post —— 见「Deferred But Adjudicated」各自 successor 触发条件。
