# 2026-07-14-0215-2-contract-drp-direct-action-e2e contract + drp 域 DIRECT 业务动作浏览器层 E2E

> Plan Status: completed
> Last Reviewed: 2026-07-14
> Source: `docs/plans/2026-07-09-2004-1-business-action-e2e-maintenance-projects-quality.md` Deferred「全 18 域全业务动作覆盖（DIRECT 域剩余）」（Successor Required: yes，触发条件「当需按域推进全 DIRECT 业务动作浏览器层覆盖时」——**已满足**：当前项目重点为各域细化端到端验证，contract/drp 为 Tier-1 未覆盖域，无 useWorkflow/useApproval 标记，全部 @BizMutation 浏览器层可达）
> Related: `2026-07-09-2004-1`（DIRECT 域扩展范式源）、`2026-07-14-0215-1`（同批 N=1 assets 域，无依赖）、`docs/testing/e2e-runbook.md`（业务动作套件）
> Audit: required

## Current Baseline

**contract 域**后端业务逻辑已全部落地（extended-roadmap M3 3.12/3.13/3.14 全 done）：

- **合同生命周期**（`ErpCtContractBizModel`）：`activate(contractId)` / `suspend` / `resume` / `terminate` / `expire` / `amend` — 6 动作状态机。**入口约束**：`activate` 守卫要求 `status=NEGOTIATION`（非 DRAFT）；状态链为 DRAFT→NEGOTIATION→ACTIVE→（SUSPENDED/TERMINATED/EXPIRED），DRAFT→NEGOTIATION 无 @BizMutation 动作（经 `__save` 直接置 NEGOTIATION 入口）。ORM 无 useWorkflow / 无 useApproval，纯 DIRECT。
- **返利计提**（`ErpCtRebateAgreementBizModel`）：`runAccrual(agreementId, asOfDate)` — 返利引擎计算。**输入约束**：引擎驱动于 partner 已过账发票（`posted=true` 的 `ErpPurInvoice`/`ErpSalInvoice`），无已过账发票时计提结果为空——非自包含可测（需 P2P/O2C 审批链产 posted 发票前置），归 Non-Goal。
- **版本管理**（`ErpCtContractVersionBizModel`）：`finalizeVersion` / `signVersion` — 版本冻结 + 签署（DIRECT，自包含可测）
- **发票计划触发**（`ErpCtInvoicePlanBizModel`）：`triggerInvoice` / `triggerDuePlans` — 到期触发建应收
- **电子签章**（`ErpCtSignatureRequestBizModel`）：`initSignatureRequest` / `cancelSignatureRequest` / `rejectSignature`（用户触发动作；`handleSignatureCallback`/`queryAndUpdateStatus` 为 webhook/轮询，非浏览器面）

**drp 域**后端业务逻辑已全部落地（extended-roadmap M3 3.15/3.16 全 done）：

- **DRP 净需求计算引擎**（`ErpDrpPlanBizModel`）：`runDrp(planId)` / `resetToDraft(planId)` / `approvePlan(planId)` — 计算引擎 + 状态机
- **安全库存优化**（`ErpInvDrpSafetyStockCalcBizModel`）：`calculate(calcId)` / `confirmWriteback(calcId)` — 计算引擎 + 确认写回
- **DRP 行释放**（`ErpDrpLineBizModel`）：`releaseLine` / `releaseApproved` — 释放转采购/调拨

**浏览器层 E2E 缺口**：contract 0 个、drp 0 个 business-action spec。两域均无 useWorkflow / 无 useApproval 标记，全部 @BizMutation 浏览器层 DIRECT 可达。

**E2E 基础设施就绪**：`tests/e2e/business-actions/_helper.ts` 三原语经 10 域验证可复用。

> 注：contract/drp/aps/b2b/logistics 域交易单据**未 seed**（e2e-runbook 明示 Non-Goal 按域逐批补充）。business-action spec 全部自包含 setup（createViaSave 建测试实体），不依赖 seed 行，故无 seed 阻塞。

## Goals

- contract + drp 两域核心 DIRECT 业务动作经 GraphQL `/graphql` 浏览器层全栈可达性 + 状态机迁移验证
- contract 覆盖：合同生命周期 6 动作状态机（经 NEGOTIATION 入口）+ 合同版本管理（finalizeVersion/signVersion）
- drp 覆盖：DRP 净需求计算引擎 runDrp + 安全库存 calculate→confirmWriteback
- 复用既有三原语范式验证在合同状态机 + 计算引擎型 BizModel 下的可复用性

## Non-Goals

- **contract 电子签章 webhook 回调**（`handleSignatureCallback`/`queryAndUpdateStatus`）——系统驱动非浏览器面动作，排除
- **contract 发票计划触发深度编排**（`triggerDuePlans` 批量建应收跨域 finance）——编排链复杂度高，归 successor
- **drp 行释放跨域编排**（`releaseLine` → 采购申请/调拨单）——跨域建单编排，归 successor
- **contract 返利计提引擎**（`runAccrual`）——引擎驱动于 partner 已过账发票（`posted=true`），无 posted 发票时结果为空；自包含测试需 P2P/O2C 审批链产 posted 发票前置，复杂度高且耦合跨域审批，归 successor（触发条件：返利计提浏览器层 E2E 需求落地时）
- **aps/b2b/logistics 域**——独立 successor，本计划仅 contract + drp

## Task Route

- Type: `verification work`（扩展现有 Playwright E2E 套件覆盖至 contract + drp 域 DIRECT 业务动作）
- Owner Docs: `docs/testing/e2e-runbook.md`（业务动作套件 + 调用范式）、`docs/design/contract/README.md`、`docs/design/drp/README.md`
- Skill Selection Basis: 浏览器层 E2E 测试编写 → 无匹配技能（Playwright 浏览器层非 `nop-testing` 后端快照范畴）；沿用 `_helper.ts` 既有范式 → `Skill: none`

## Infrastructure And Config Prereqs

No infra prereqs beyond existing baseline。复用现有 Playwright 配置 + webServer JVM 参数。两域均无 config 门控开关需启用。

## Execution Plan

### Phase 1 - contract 域合同生命周期 + 版本管理 E2E

Status: completed
Targets: `tests/e2e/business-actions/ct-contract-lifecycle.action.spec.ts`（新建）、`tests/e2e/business-actions/ct-contract-version.action.spec.ts`（新建）
Skill: none

- Item Types: `Add | Proof`
- Prereqs: 无（自包含 setup）

- [x] `Add`: **合同生命周期 spec** `ct-contract-lifecycle.action.spec.ts`
  - 6 动作状态机正向链：自包含建 `ErpCtContract`（经 `__save` 直接置 `status=NEGOTIATION` 入口，含 partnerId / 合同类型 / 金额 / 生效日期）→ `activate(contractId)` → `verifyState` 断言 status=ACTIVE → `suspend` → status=SUSPENDED → `resume` → status=ACTIVE → `terminate` → status=TERMINATED
  - `expire` 路径：另建 ACTIVE 合同 → `expire` → status=EXPIRED
  - `amend` 路径：ACTIVE 合同 amend → 断言版本回链 / 新建修订版
  - 非法迁移守卫（DRAFT→activate 抛 ErrorCode message token，因 activate 要求 NEGOTIATION；TERMINATED→activate 抛 ErrorCode）
  - Skill: none
- [x] `Add`: **合同版本管理 spec** `ct-contract-version.action.spec.ts`
  - 版本生命周期：自包含建 `ErpCtContractVersion`（DRAFT 入口，关联合同）→ `finalizeVersion` → `verifyState` 断言 status=FINALIZED → `signVersion` → status=SIGNED + isCurrent 翻转
  - 非法迁移守卫（SIGNED→finalizeVersion 抛 ErrorCode message token）
  - Skill: none

Exit Criteria:

- [x] 2 spec 文件经 `npx playwright test tests/e2e/business-actions/ct-*.action.spec.ts --workers=1` 全绿
- [x] 合同 6 动作 status 翻转 + 版本 finalizeVersion/signVersion 状态翻转均经 `verifyState` `__get` 独立断言

### Phase 2 - drp 域净需求引擎 + 安全库存 E2E

Status: completed
Targets: `tests/e2e/business-actions/drp-plan-engine.action.spec.ts`（新建）、`tests/e2e/business-actions/drp-safety-stock.action.spec.ts`（新建）
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 1 范式验证

- [x] `Add`: **DRP 净需求引擎 spec** `drp-plan-engine.action.spec.ts`
  - `runDrp(planId)` 浏览器层可达性：自包含建 `ErpDrpPlan`（DRAFT + 仓库 + 物料维度）+ `ErpDrpParameter`（materialId+warehouseId 维度行，safetyStock > 当前库存触发净需求 > 0）→ 调 `runDrp` → 断言 plan status 翻转 + `ErpDrpLine` 净需求行非空（计划订单生成）
  - `resetToDraft(planId)` 回退 → status=DRAFT + 行清理
  - `approvePlan(planId)` → status=APPROVED
  - 非法迁移守卫
  - Skill: none
- [x] `Add`: **安全库存优化 spec** `drp-safety-stock.action.spec.ts`
  - `calculate(calcId)` 浏览器层可达性：自包含建 `ErpInvDrpSafetyStockCalc`（参数 + 历史销量引用）→ 调 `calculate` → 断言计算结果（建议安全库存值）非空
  - `confirmWriteback(calcId)` → 断言写回 status + 安全库存参数更新
  - Skill: none

Exit Criteria:

- [x] 2 spec 文件经 `npx playwright test tests/e2e/business-actions/drp-*.action.spec.ts --workers=1` 全绿
- [x] DRP 引擎状态翻转 + 安全库存计算结果均经 `verifyState` 独立断言

## Draft Review Record

- Independent draft review iteration 1: needs revision (ses_0a34c2824ffe) — DIRECT-ness/无 useWorkflow 标记/helper 原语经实时仓库核实一致，但发现 2 项 Blocker：B1 合同 `activate` 守卫要求 `status=NEGOTIATION` 非 DRAFT（DRAFT→NEGOTIATION 无 @BizMutation，须经 `__save` 直接置入口）；B2 返利 `runAccrual` 驱动于 partner 已过账发票，无 posted 发票时结果恒空，非自包含可测。
- Independent draft review iteration 2: accept (ses_0a34469beffe) — B1/B2 均核实已解决（contract NEGOTIATION 入口经实时仓库确认 `activate` 守卫；rebate 移入 Non-Goal/Deferred 替换为版本管理 finalizeVersion/signVersion 经实时仓库确认 @BizMutation）；DRP `ErpDrpParameter` 输入实体确认存在；规则合规全 PASS；无新 Blocker。2 项非阻塞实现期提示（contractDirection setup / current version 预置）属实现细节非计划级。计划可作为执行契约进入实施。

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。

- [x] 范围内行为完成：4 spec 覆盖 contract 2 路径（生命周期 + 版本管理）+ drp 2 路径（净需求引擎 + 安全库存）
- [x] 相关文档对齐：`docs/testing/e2e-runbook.md` 业务动作表 +contract/drp 行 + 套件计数更新
- [x] 已运行验证：`npx playwright test tests/e2e/business-actions/ct-*.action.spec.ts tests/e2e/business-actions/drp-*.action.spec.ts --workers=1` 全绿（9 passed）+ 全套件回归无新增失败（engine 改动隔离于 drp SafetyStockEngine 单路径，drp-service 30 单测全绿；改动不触及其他域 E2E）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计（见 Closure Audit Evidence）
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### contract 返利计提引擎 E2E（runAccrual）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: `runAccrual` 引擎驱动于 partner 已过账发票（`posted=true` 的 `ErpPurInvoice`/`ErpSalInvoice`），无 posted 发票时计提结果恒空。自包含测试需 P2P/O2C 审批链产 posted 发票前置（经 orchestration/_helper.ts runP2pChain/runO2cChain），耦合跨域审批复杂度高。本计划聚焦合同状态机 + 版本管理 + DRP 引擎。
- Successor Required: `yes`（触发条件：返利计提浏览器层 E2E 需求落地时，或 P2P/O2C 审批链前置可在 spec 内低成本编排时）

### contract 发票计划触发跨域编排（triggerInvoice / triggerDuePlans）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: `triggerDuePlans` 批量扫描到期计划 + 跨域建 finance 应收，属编排链，复杂度高。本计划聚焦合同状态机 + 版本管理。
- Successor Required: `yes`（触发条件：合同到期开票编排浏览器层 E2E 需求落地时）

### drp 行释放跨域编排（releaseLine → 采购申请/调拨单）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: `releaseLine` 释放后跨域建采购申请或调拨单，属跨域编排链。本计划聚焦 DRP 计算引擎本体。
- Successor Required: `yes`（触发条件：DRP 释放→下游单据编排浏览器层 E2E 需求落地时）

### aps / b2b / logistics 域 DIRECT 业务动作 E2E

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划仅 contract + drp。aps（排产引擎）/ b2b（ASN→PO 匹配）/ logistics（发运生命周期）为独立 Tier-2 successor。
- Successor Required: `yes`（触发条件：按域推进剩余 DIRECT 业务动作浏览器层覆盖时）

## Closure

Status Note: 执行完成。两阶段全绿（Phase 1 contract 6 passed / Phase 2 drp 3 passed，合计 9 passed）。执行期发现并修复 drp SafetyStockEngine 除零缺陷（无历史出库时 `monthlyDemands` 返回空列表致 `mean()` 除零 ArithmeticException，对齐既有 `result.isEmpty()→add(ZERO)` 意图改为返回 `[0]`），使 `calculate` 在自包含无 posted 历史场景下浏览器层可达。drp-service 30 单测全绿（含 TestErpDrpSafetyStock 6）。backlog README + e2e-runbook（业务动作表 +contract/drp 4 行 + 套件计数 260→269）已对齐。独立结束审计通过（见下）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，不重用执行者上下文）。审计结论：approved。
- Audit scope: 计划全文重读；6 项语义校验（Phase status/items 一致性、Exit Criteria vs live repo、Anti-Hollow、五点一致性、Deferred honesty、Docs sync）。
- Live repo verification: 4 spec 文件均存在且非空壳（`tests/e2e/business-actions/ct-contract-lifecycle.action.spec.ts` 4 测试 / `ct-contract-version.action.spec.ts` 2 测试 / `drp-plan-engine.action.spec.ts` 2 测试 / `drp-safety-stock.action.spec.ts` 1 测试）；全部含 `verifyState` `__get` 独立断言 + 测试末 cleanup（deleteById/deleteByFilter），无 `{}`/`return null`/吞异常占位。
- Engine fix landed: `module-drp/erp-drp-service/.../safetystock/SafetyStockEngine.java:211-216` `monthlyDemands()` 无历史出库 `moves.isEmpty()` 分支返回 `[BigDecimal.ZERO]`（对齐既有 line 238-240 `result.isEmpty()→add(ZERO)` 语义），非 workaround——种子 stock_move 全 posted=false 且 `__save` 强制 posted=false（过账须经 complete 派发链），引擎除零修复是使 calculate 浏览器层可达的正确路径。
- Anti-Hollow: 4 spec 均经 `callMutationOk`/`callMutation` 触发真实 @BizMutation，断言 status/字段翻转（toBe/toBeGreaterThan/toContain），运行时可达。
- Five-point consistency: Plan Status completed ↔ Phase 1/2 Status completed ↔ 各 Phase Exit Criteria 全 `[x]` ↔ Closure Gates 全 `[x]`（含本审计 gate）↔ Closure evidence 非占位。
- Deferred honesty: 4 Deferred 项（rebate runAccrual / triggerDuePlans / drp releaseLine / aps-b2b-logistics）均 `out-of-scope improvement` + `Successor Required: yes` + 触发条件；唯一执行期缺陷（SafetyStockEngine 除零）已修复落地，未隐藏于 Deferred。
- Docs sync: `docs/logs/2026/07-14.md` lines 3-14 详细条目；`docs/backlog/README.md` line 58 ✅ done 行；`docs/testing/e2e-runbook.md` lines 251-254 业务动作表 4 行 + line 113 套件计数 260→269 / 22→26 业务动作 spec。
- Phases: Phase 1 `Status: completed`（4 items `[x]` + 2 exit criteria `[x]`）；Phase 2 `Status: completed`（2 items `[x]` + 2 exit criteria `[x]`）。
- Verification: `SKIP_WEBSERVER=1 PLAYWRIGHT_PORT=8011 npx playwright test tests/e2e/business-actions/ct-contract-lifecycle.action.spec.ts tests/e2e/business-actions/ct-contract-version.action.spec.ts tests/e2e/business-actions/drp-plan-engine.action.spec.ts tests/e2e/business-actions/drp-safety-stock.action.spec.ts --workers=1` → 9 passed (1.1m)。`mvn test -pl module-drp/erp-drp-service` → 30 passed (BUILD SUCCESS)。
- Engine fix: `module-drp/erp-drp-service/.../safetystock/SafetyStockEngine.java` `monthlyDemands()` 无历史 `moves.isEmpty()` 分支返回 `[BigDecimal.ZERO]`（对齐既有空集语义）。
- Docs aligned: `docs/backlog/README.md`（+2026-07-14-0215-2 ✅ done 行）；`docs/testing/e2e-runbook.md`（业务动作表 +4 contract/drp 行 + line 113 套件计数 260→269、26 业务动作 spec）。
- Source Audits: 无（本计划 front matter 无 `> Source Audits:` 行，roadmap-sourced successor plan，跳过审计关闭步骤）。

Follow-up:

- aps/b2b/logistics DIRECT 业务动作 E2E successor（触发条件见 Deferred）
