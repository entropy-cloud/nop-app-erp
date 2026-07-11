# 2026-07-10-0335-2 依赖门控与剩余 DIRECT 业务动作浏览器层 E2E

> Plan Status: completed
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

Status: completed
Targets: `tests/e2e/business-actions/quality-ncr-resolve-capa-gate.action.spec.ts`
Skill: `none`

- Item Types: `Add | Proof`
- Prereqs: `2004-1` quality-capa + quality-ncr spec 已落地（CAPA 三步 + NCR 无 CAPA 路径基线）

- [x] `Add`：新建 `quality-ncr-resolve-capa-gate.action.spec.ts`——`createViaSave` 建 NCR（code 保唯一，dispositionType 选 CONCESSION 干净隔离门控【实现裁决：RETURN 触发 NcrReturnOrchestrator 退货单副作用属 Non-Goal，SCRAP 触发 config-gated 自动过账，CONCESSION 在 dispatchFinancialImpact 无分派】，status=OPEN）+ `createViaSave` 建关联 ErpQaAction（CAPA，ncrId=上面 NCR id，status=PENDING）
- [x] `Proof`：门控负路径——NCR `submitReview`(OPEN→IN_REVIEW) → `resolve`（CAPA 仍 PENDING 未闭包）→ 断言 errors 非空 + 含 `ERR_NCR_RESOLVE_CAPA_NOT_COMPLETED` 对应标志性 message token（Nop GraphQL 此配置仅回传 i18n message 不序列化 extensions.errorCode，故断言「CAPA」+「未完成」token 唯一区分状态迁移类错误）→ `verifyState status` 仍 IN_REVIEW（未迁移）
- [x] `Proof`：CAPA 闭包——`callMutationOk startAction`(PENDING→IN_PROGRESS) → `callMutationOk completeAction`(→COMPLETED) → `callMutationOk verifyAction`(填 verificationPerson+verificationDate，:55-76) → `verifyState` CAPA status=COMPLETED
- [x] `Proof`：门控正路径——`callMutationOk resolve`(ncrId, resolution) → `verifyState status=RESOLVED`（全部 CAPA 闭包后门控通过）
- [x] `Proof`：清理——NCR + CAPA 自身删除（CAPA 先于 NCR 删，因 FK 依赖）

Exit Criteria:

- [x] 门控负路径（CAPA 未闭包 → resolve 抛错 + status 不变）+ 正路径（CAPA 闭包 → resolve 成功 + status=RESOLVED）均通过
- [x] CAPA 三步（start→complete→verify）+ resolve 经 `verifyState` 独立断言

### Phase 2 - maintenance Request 5 态状态机 E2E

Status: completed
Targets: `tests/e2e/business-actions/mnt-request.action.spec.ts`
Skill: `none`

- Item Types: `Add | Proof`
- Prereqs: 无

- [x] `Add`：新建 `mnt-request.action.spec.ts`——`createViaSave` 建 ErpMntRequest（mandatory code+requestDate+equipmentId+description+priority+status+requestedBy，code 保唯一；equipmentId=1 种子设备 EQ-2026-001；requestedBy=2 种子员工李四）
- [x] `Proof`：正路径——`callMutationOk accept`(OPEN→ACCEPTED) → `verifyState status=ACCEPTED` → `startRepair`(→IN_PROGRESS) → `complete`(→COMPLETED + completedAt)
- [x] `Proof`：分支路径——`rejectRequest`(OPEN/ACCEPTED→REJECTED) + `cancel`(OPEN/ACCEPTED→CANCELLED)
- [x] `Proof`：非法迁移守卫——如 COMPLETED 再 accept → 断言 errors 非空
- [x] `Proof`：清理——accept 生成响应式 ErpMntVisit 副作用（code=VST-REQ-{requestId}，visitDate=today 落入看板区间），按 code 删除避免污染 periodVisitCount 基线 + Request 自身删除

Exit Criteria:

- [x] 5 态状态机正路径（accept→startRepair→complete）+ 分支路径（rejectRequest/cancel）+ 守卫全通过
- [x] `status` 翻转经 `verifyState` 独立断言

## Draft Review Record

- Independent draft review iteration 1: accept (draft review 2026-07-10) — format compliant with template; all required sections present; Phase structure valid with phase-level Item Types and Skill declarations; Exit Criteria clear and testable for both phases (NCR→CAPA gate negative+positive paths, maintenance 5-state transitions); scope tightly bounded to 2 spec files under same result surface (browser-layer E2E business-actions) with no scope creep; 3 deferred items properly adjudicated with successor trigger conditions; Non-Goals explicitly exclude postNcr/reverseNcr/Visit orchestration/voucher post; Closure Gates define sufficient evidence (2 specs green + validation commands + independent closure audit); no Blocker or Major issues found. Minor notes (non-blocking): `mvn clean install -DskipTests` redundant for no-Java-change plan but follows project convention; line 83 cleanup tactic uses "按需" language (acceptable as test-cleanup instruction, not scope).

## Closure Gates

- [x] 范围内行为完成（2 spec 全绿：quality-ncr-resolve-capa-gate + mnt-request）
- [x] 相关文档对齐（`docs/testing/e2e-runbook.md` 业务动作层新增 2 实体段 + 套件计数更新；`docs/logs/2026/07-10.md` 聚合条目）
- [x] 已运行验证：`npx playwright test tests/e2e/business-actions/`（新 spec 全绿，0 回归）+ `mvn clean install -DskipTests`（154 模块 BUILD SUCCESS）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位符
- [x] 结束证据存在于文件中

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
- **Resolved**: plan `2026-07-11-2329-2` Phase 1 落地 `mnt-request-visit-orchestration.action.spec.ts`（accept → 响应式 DRAFT Visit 6 字段精确断言：code/equipmentId/visitDate/visitType/status/assignedTo）。

### finance voucher 手工 post 业务动作

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: `reverse` 已在 `2004-2` orchestration 覆盖；`post` 入参复杂 `PostingEvent`、边际收益递减（`2004-1` Deferred 明示）。
- Successor Required: `yes`
- Trigger Condition: 当需验证 voucher 手工过账浏览器层可达性时。
- **Resolved**: plan `2026-07-11-2329-2` Phase 2 落地 `finance-voucher-post.action.spec.ts`（PostingEventInput → `ErpFinVoucher__post` LANDED_COST 最简路径 + 凭证头/回链/凭证行断言 + 幂等路径）。

## Closure

Status Note: 执行完成，独立结束审计已通过（独立子代理新会话冷重播审计，无执行者上下文复用）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，plan-check FAIL 触发的 closure-audit 轮次，非执行者会话）
- Evidence:
  - 结构校验：front matter `Plan Status: completed` + `Last Reviewed: 2026-07-10`；两 Phase 均 `Status: completed`，Exit Criteria 全 `[x]`，Phase items 全 `[x]`；Closure Gates 8/8 全 `[x]`（closure-audit gate 经本轮独立审计勾选）；Closure section 含具体证据非占位符。
  - 活仓库核实（grep/glob/read）：`tests/e2e/business-actions/quality-ncr-resolve-capa-gate.action.spec.ts`（124 行，1 test：负路径 resolve 门控拒绝+message token 断言+status 不变 + CAPA 三步闭包 startAction/completeAction/verifyAction + 正路径 resolve 成功 status=RESOLVED + 清理）+ `mnt-request.action.spec.ts`（115 行，2 test：正路径 OPEN→ACCEPTED→IN_PROGRESS→COMPLETED+completedAt+COMPLETED→accept 非法守卫 + 分支 rejectRequest/cancel + 清理）均存在且非空壳（无空函数体/return null/吞异常）；`docs/testing/e2e-runbook.md` 含两实体段（mnt-request :221 / quality-ncr-resolve-capa-gate :225 标 0335-2）+ 套件计数 12 代表域 / 167 测试；`docs/logs/2026/07-10.md` 含聚合条目 + full-green 验证状态。
  - Anti-Hollow：两 spec 均经 `callMutation`/`callMutationOk`/`verifyState` 真实驱动 GraphQL mutation + `__get` 独立断言状态翻转，断言密度高（状态值断言 + errors 断言 + message token 断言），无占位实现。
  - Deferred honesty：3 个 Deferred But Adjudicated 项（NCR postNcr/reverseNcr SCRAP 过账数值断言 / Request→Visit 编排 / finance voucher post）均为不同结果表面的 out-of-scope improvement，非范围内实时缺陷降级，每项标注 Successor Required + Trigger Condition。
  - 五点一致性：Plan Status=completed / 两 Phase Status=completed / 两 Phase Exit Criteria 全 [x] / Closure Gates 全 [x] / Closure 证据非占位符 — 一致。

- Phase 1（quality-ncr-resolve-capa-gate）：`quality-ncr-resolve-capa-gate.action.spec.ts` 1 测试全绿——负路径（CAPA PENDING 时 resolve 抛错 + 含「CAPA」「未完成」message token + status 仍 IN_REVIEW）+ CAPA 三步闭包（start→complete→verify）+ 正路径（resolve 成功 status=RESOLVED）+ 清理。实现裁决：dispositionType 用 CONCESSION 非 RETURN（RETURN 触发 NcrReturnOrchestrator 退货单副作用属 Non-Goal，SCRAP 触发自动过账，CONCESSION 干净隔离门控本身）。Nop GraphQL 此配置仅回传 i18n message 不序列化 extensions.errorCode，故错误码断言改为标志性 message token。
- Phase 2（mnt-request）：`mnt-request.action.spec.ts` 2 测试全绿——正路径（OPEN→ACCEPTED→IN_PROGRESS→COMPLETED + completedAt + COMPLETED→accept 非法守卫）+ 分支路径（rejectRequest→REJECTED / cancel→CANCELLED）。清理裁决：accept 生成响应式 visit（visitDate=today 落入看板区间），按 code 删除避免污染 maintenance.value.spec periodVisitCount 基线（1）。
- 验证：`npx playwright test tests/e2e/business-actions/ tests/e2e/dashboards/{maintenance,quality}.value.spec.ts --workers=1` → 28 passed（含 3 新测试，0 回归，看板基线 intact）；`mvn install -DskipTests` → BUILD SUCCESS（154 模块）。

Follow-up:

- NCR SCRAP 过账数值断言 / maintenance Request→Visit 编排 / finance voucher post —— 见「Deferred But Adjudicated」各自 successor 触发条件。
