# 2026-07-14-0035-2-subcontract-lifecycle-e2e-extension 委外生命周期浏览器层 E2E 扩展

> Plan Status: completed
> Last Reviewed: 2026-07-14
> Source: `docs/plans/2026-07-13-0701-2-subcontracting-e2e-frontend.md` Deferred But Adjudicated「委外生命周期浏览器层 E2E 扩展（MRP 释放 / 多行发料 / 部分收货 / 红冲）」（Successor Required: yes，触发条件=对应业务场景深化需求落地时——委外生命周期 E2E spec 0701-2 已落地正向链 + 非法迁移守卫，本计划扩展覆盖 MRP 释放自动建单 / 多行发料 / 部分收货 / cancel 守卫）
> Related: `2026-07-13-0701-2`（委外 E2E + 前端 completed，本计划解除其 Deferred）、`2026-07-13-0455-1`（委外引擎 completed）
> Audit: required

## Current Baseline

- `tests/e2e/orchestration/mfg-subcontract-chain.spec.ts`（166 行）当前 2 测试：(A) 正向全链 approve→issue→receive→post-fee + 3 段 GL 凭证行数值断言 + posted=true；(B) DRAFT→issueMaterials 非法迁移守卫。
- `runSubcontractChain`（`tests/e2e/orchestration/_helper.ts:1022-1147`）编排单行委外链：建测试专用 component/product 物料 → 备货 → 建单（DRAFT, processingFee=50）+ 1 行（qty=2）→ submit→approve → issueMaterials（全量）→ receiveFinished（receivedQty=1）→ postProcessingFee → COMPLETED。`cleanupSubcontract`（:1159-1196）逆序清理。
- `SUBCONTRACT_EXPECT`（:999-1008）确定性期望值：setupQty=10/componentUnitCost=5/lineQty=2/issueCost=10/processingFee=50/receivedQty=1/receiptCost=50。
- `MrpReleaseService.releaseSubcontractRequest`（`module-manufacturing/erp-mfg-service/.../mrp/MrpReleaseService.java:100-115`）config-gated `erp-mfg.subcontract-release-enabled`（默认关），开启后释放 `SUBCONTRACT_REQUEST` 计划行 → 自动建 `ErpMfgSubcontractOrder`（code=`SUB-MRP-{lineId}`，processingFee=0/totalAmount=0 骨架，docStatus=APPROVED 跳审批）+ 1 行（qty=plannedQuantity）。无 E2E 覆盖。
- `ErpMfgSubcontractOrderProcessor.generateIssueMove`（:291-315）**已支持多行**（遍历所有 `ErpMfgSubcontractOrderLine` 推入 StockMoveRequest.lines）。但 helper 仅建 1 行。
- `receiveFinished`（:170-190）有 `receivedQty` 参数，支持部分收货数量，但无累计/超量校验。无 E2E 覆盖部分收货。
- `cancel`（:109-120）支持 DRAFT/SUBMITTED/APPROVED → CANCELLED。无 E2E 覆盖。
- **红冲（reverseProcess）后端未实现**——`ErpMfgSubcontractOrderProcessor` 仅有 `reverseApprove`（审批轴回退，:278-283），无 GL 凭证红冲/库存反向移动编排。`SubcontractPostingDispatcher` 无 reverse 方法。E2E 红冲覆盖须等后端实现。

剩余差距：(1) MRP 释放→自动建单零 E2E；(2) 多行发料零 E2E（代码已支持）；(3) 部分收货零 E2E（参数已支持）；(4) cancel 守卫零 E2E；(5) 红冲后端未实现（归 Non-Goal）。

## Goals

- 扩展委外生命周期浏览器层 E2E 至 MRP 释放→自动建单场景（config-gated 开启后释放 SUBCONTRACT_REQUEST 验证自动建 APPROVED 委外单 + 骨架字段）。
- 扩展多行发料 E2E（helper 支持 N 行订单 → issueMaterials 生成多行 StockMove 验证）。
- 扩展部分收货 E2E（receivedQty < 订购量 → 单据 RECEIVED + 部分入库移动验证）。
- 扩展 cancel 路径 E2E（DRAFT/SUBMITTED/APPROVED → cancel → CANCELLED + 非法迁移守卫）。

## Non-Goals

- **红冲 E2E**——后端 `reverseProcess` 未实现（0701-2 Deferred 已确认）。本计划不含红冲 E2E；待后端红冲 successor 落地后再做。
- **partial fee posting E2E**——`postProcessingFee` 无部分参数（全量 processingFee 一次性过账），无可测部分路径。
- 委外前端 drawer/edit 表单内动作入口（0900-1/0701-2 Deferred，触发条件=表单内入口用户面需求落地时）——独立 successor。
- SUBCONTRACT 差异 E2E 断言——属计划 `2026-07-14-0035-1` 后端能力，其 E2E 覆盖待该计划落地后按需扩展。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/design/manufacturing/subcontracting.md`
- Skill Selection Basis: 浏览器层 E2E 测试扩展，加载 `nop-testing`（Playwright + IGraphQLEngine 范式 + 既有 orchestration/_helper.ts 原语复用）。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. `playwright.config.ts` webServer JVM args 已含 `-Derp-mfg.subcontract-posting-enabled=true`（0701-2 范式）。本计划新增 `-Derp-mfg.subcontract-release-enabled=true`（MRP 释放 config-gated 默认关，E2E 按需开启，对齐 inspection-gate/variance-auto-calc 既有范式）。

## Execution Plan

### Phase 1 — MRP 释放→自动建单 E2E

Status: completed
Targets: `tests/e2e/orchestration/mfg-subcontract-chain.spec.ts`（新增 test block）；`tests/e2e/orchestration/_helper.ts`（`runSubcontractMrpRelease` + cleanup 原语）；`playwright.config.ts`（webServer JVM arg 追加）
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: 无

- [x] `playwright.config.ts` webServer JVM args 追加 `-Derp-mfg.subcontract-release-enabled=true`。
  - Skill: `nop-testing`
- [x] 在 `_helper.ts` 新增 `runSubcontractMrpRelease(page)`：建 component/product 物料 → 建带 SUBCONTRACT_REQUEST 计划行的 MRP 计划 → 调 `MrpPlan__releaseSubcontractRequest` GraphQL mutation → 断言自动建 `ErpMfgSubcontractOrder`（code=`SUB-MRP-{lineId}`，docStatus=APPROVED，approveStatus=APPROVED 跳审批，processingFee=0/totalAmount=0 骨架）+ 1 行（qty=plannedQuantity）+ 计划行 isFirmed=true。配套 `cleanupSubcontractMrpRelease` 逆序清理（删委外单+行 → 删 MRP 计划+行 → 删物料）。
  - Skill: `nop-testing`
- [x] 在 `mfg-subcontract-chain.spec.ts` 新增 test：调用 `runSubcontractMrpRelease` + 断言自动建单骨架字段 + try/finally cleanup。
  - Skill: `nop-testing`

Exit Criteria:

> MRP 释放→自动建单场景在浏览器层可验证：释放后委外单自动创建为 APPROVED 骨架 + 计划行标记 firmed。

- [x] 新增 spec 测试通过（`npx playwright test mfg-subcontract-chain` 全绿）

### Phase 2 — 多行发料 + 部分收货 E2E

Status: completed
Targets: `tests/e2e/orchestration/mfg-subcontract-chain.spec.ts`（新增 test block）；`tests/e2e/orchestration/_helper.ts`（`runSubcontractChain` 扩展 `MultiLineSubcontractOptions`）
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: 无

- [x] 扩展 `runSubcontractChain` 支持 `lineCount` 可选参数（默认 1，对齐既有行为）：lineCount>1 时建 N 行订单（每行不同 component 物料或同物料不同 qty），issueMaterials 后断言 OUTGOING StockMove 含 N 行移动明细（`findPageTotal(ErpInvStockMoveLine, relatedBillCode={order}) == N`）。
  - Skill: `nop-testing`
- [x] 新增多行 test：`runSubcontractChain({ lineCount: 3 })` → 断言 3 行 OUTGOING 移动 + 3 段 issueCost 汇总 SUBCONTRACT_ISSUE 凭证。
  - Skill: `nop-testing`
- [x] 新增部分收货 test：`runSubcontractChain` 至 ISSUED 态后，`receiveFinished({ receivedQty: < lineQty })` → 断言单据 RECEIVED + MANUFACTURE 入库移动 quantity=receivedQty（部分量）。
  - Skill: `nop-testing`

Exit Criteria:

> 多行发料在浏览器层可验证 OUTGOING 移动含 N 行明细；部分收货 quantity 经 receivedQty 参数精确控制。

- [x] 新增 spec 测试通过（多行 3 行 + 部分收货各 1 test）

### Phase 3 — cancel 路径 E2E

Status: completed
Targets: `tests/e2e/orchestration/mfg-subcontract-chain.spec.ts`（新增 test block）
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: 无

- [x] 新增 cancel 正路径 test：建单 DRAFT → `cancel` → 断言 docStatus=CANCELLED；建单 submit→SUBMITTED → `cancel` → 断言 CANCELLED；建单 submit→approve→APPROVED → `cancel` → 断言 CANCELLED。
  - Skill: `nop-testing`
- [x] 新增 cancel 非法迁移守卫 test：ISSUED/RECEIVED/COMPLETED 态 → `cancel` 抛错 + docStatus 不变。
  - Skill: `nop-testing`

Exit Criteria:

> cancel 路径正/负路径在浏览器层可验证：DRAFT/SUBMITTED/APPROVED 可取消，ISSUED 及之后不可取消。

- [x] 新增 spec 测试通过（cancel 正路径 3 + 负路径守卫 1）

## Draft Review Record

- Independent draft review iteration 1: needs revision (ses_0a3a801aeffe0fS83TM3Tw6arI) because Goal 承诺 APPROVED→cancel 但 Phase 3 执行项仅覆盖 DRAFT/SUBMITTED→cancel（规则 10 检查清单完整性 + 规则 11 文本一致性违规）。修复：Phase 3 正路径执行项补 approve→APPROVED→cancel→CANCELLED 第三条 + 退出标准正路径数 2→3。
- Independent draft review iteration 2: accept (ses_0a3a801aeffe0fS83TM3Tw6arJ) after blocker 修复——Goal↔Execution 一致性已恢复（APPROVED→cancel 正路径补入），全部基线声明经实时仓库核实诚实，范围单结果表面清晰，红冲 Non-Goal 后端未实现已正确排除，config-gate 处理对齐既有范式，反松弛无违规词。

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。纯测试计划，完整仓库验证在此处运行一次。

- [x] 范围内行为完成（4 场景 E2E：MRP 释放 / 多行发料 / 部分收货 / cancel 守卫）
- [x] 相关文档对齐（e2e-runbook 委外段更新）
- [x] 已运行验证：`npx playwright test mfg-subcontract-chain`（全绿）+ `mvn clean install -DskipTests`（154 模块，确保 webServer config 变更不破坏构建）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 委外红冲 E2E

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 后端 `reverseProcess` 未实现（`ErpMfgSubcontractOrderProcessor` 无 GL 红冲/库存反向编排，`SubcontractPostingDispatcher` 无 reverse 方法）。E2E 无可验证对象。
- Successor Required: `yes`（触发条件：委外红冲后端 successor 落地时）

## Closure

Status Note: 全部 3 Phase 落地并验证通过。执行者验证（非独立结束审计）：`PLAYWRIGHT_PORT=8011 npx playwright test mfg-subcontract-chain` 7 passed（既有 2 + 新增 5：MRP 释放 / 多行发料 / 部分收货 / cancel 正路径 / cancel 守卫）；`mvn clean install -DskipTests` BUILD SUCCESS（154 模块，1:34）。

Closure Audit Evidence:

- Auditor / Agent: 执行者验证（executor self-verification，2026-07-14）。独立结束审计建议由新会话子代理按 Closure Gates 复核。
- Evidence:
  - Phase 1：`playwright.config.ts` webServer JVM arg 追加 `-Derp-mfg.subcontract-release-enabled=true`；`_helper.ts` 新增 `runSubcontractMrpRelease`/`cleanupSubcontractMrpRelease`/`SUBCONTRACT_MRP_EXPECT`（建 FINISHED_PRODUCT 物料 → MRP 计划+SUBCONTRACT_REQUEST 行 → `ErpMfgMrpPlanLine__releaseSubcontractRequest` → 断言 code=`SUB-MRP-{lineId}`/docStatus=APPROVED/approveStatus=APPROVED/processingFee=0/totalAmount=0/单行 qty=plannedQuantity/计划行 isFirmed/plan status=FIRMED）；spec 新增 1 test 通过。
  - Phase 2：`runSubcontractChain(page, options?)` 扩展 `MultiLineSubcontractOptions`（`lineCount` 建 N 独立组件物料+N 行+N 备货移动；`stopAfterIssue` 发料后停）；多行 test 断言 3 StockMoveLine（moveId 过滤）+ Dr 1408 汇总 30/Cr 1401 按物料分列 3 行；部分收货 test 断言 receivedQty<lineQty → RECEIVED + MANUFACTURE 移动行 quantity=receivedQty。`SubcontractResult`/`cleanupSubcontract` 重构为数组形态（componentMats/orderLines/setupMoves），保持单行向后兼容。
  - Phase 3：cancel 正路径 test（DRAFT/SUBMITTED/APPROVED→CANCELLED，3 场景）+ cancel 守卫 test（单链 ISSUED→RECEIVED→COMPLETED 逐态 cancel 拒绝+docStatus 不变）。
  - **种子数据修复（plan 0035-1 回归，非本计划范围但阻塞验证）**：`app-erp-all/src/main/resources/_vfs/_init-data/erp_md_subject.csv` 重复主键 id=24（1408 委外物资 与 1416 制造差异-委外 冲突，0035-1 commit 03a7d449 引入）导致 fresh-DB 初始化 `nop.err.orm.save-entity-replace-existing-entity` 启动失败。修复：1416 id 24→26（科目码引用不受影响，posting/voucher 按 subjectCode 匹配）。


- **Independent Closure Audit (2026-07-14-1449-1 batch)** — Auditor: independent closure audit subagent (fresh session, cold-replay, 2026-07-14). Verdict: **PASS**. All 3 phases verified: MRP release helper + multi-line/partial-receive/cancel-path tests with concrete assertions, config-gating, seed-data fix (1408/1416 subjects). Deferred item (红冲) honestly classified with backend blocker rationale. (Audit dispatch ref: docs/plans/2026-07-14-1449-1-closure-audit-consistency-remediation-batch.md Phase 2; this evidence block appended by Phase 3 backfill.)
Follow-up:

- 委外红冲 E2E → 见 `Deferred But Adjudicated`（Successor Required: yes）。
