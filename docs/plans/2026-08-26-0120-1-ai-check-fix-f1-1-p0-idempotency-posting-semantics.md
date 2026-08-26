# 2026-08-26-0120-1-ai-check-fix-f1-1-p0-idempotency-posting-semantics F1.1：P0 完工入库幂等键 + 过账幂等返回语义

> Plan Status: completed
> Last Reviewed: 2026-08-26
> Source: ai-check mission 修复工作项 F1.1（`docs/backlog/ai-check-roadmap.md` §MF 第一批）；findings：P0-CK-mfg-001、P1-CK-fin-003（`docs/audits/check/ck-mfg-workorder.md`、`ck-finance-posting.md`）
> Related: C8.3 收官审计修复优先级建议第一批第 1 项；同链传导站点 P2-CK-qa-013 / P1-CK-prj-006（并入）/ P1-CK-log-001（并入）
> Audit: required（plan-first 保护区域：会计/财务过账引擎——`ai-autonomy-policy.md §保护区域` "accounting/finance postings | plan-first | owner doc + tests"）

## Current Baseline

- **P0-CK-mfg-001**（已实证）：`ErpMfgWorkOrderProcessor#generateCompletionMove`（L397-408）每次增量报工以固定 `(ERP_MFG_WORK_ORDER, wo.getCode())` 调 `stockMoveBiz.generateMove`；inventory 侧 `ErpInvStockMoveGenerateMoveProcessor#generateMove`（L29-35）对 business-linked 请求 `findExisting` 命中即静默返回首单——第二次起增量报工的库存移动被吞，`WO.completedQuantity` 与库存/GL 收货数量静默分叉。现有测试全部单发达量零覆盖。
- **P1-CK-fin-003**（已实证）：过账引擎 `ErpFinPostingProcessor#process`（L139-142）幂等命中（`alreadyPosted` 扫 voucher-bill 回链发现 POSTED 未红冲凭证）返回 `null`；全域 8+ dispatcher（mfg×2/prj×2/log/qa/ast×6 等）以 `voucherId != null` 判定成功置 posted——重试/重入场景幂等命中后 posted 永久 false，且部分 dispatcher 的后续动作（如 mfg 领料红冲守卫）被封死。
- 红冲侧安全面（已核）：`ErpMfgWorkOrderReverseApproveProcessor`（83 行）不反查完工移动单（完工移动红冲本就未实现，属 mfg-003 范畴）——改 completion move 的 relatedBillCode 无既有消费者被破坏。委外/subcontract 的 `findByRelatedBill`（SubcontractOrderProcessor:192）用的是自己的 relatedBillType，不受影响。
- 剩余差距：两条缺陷均未修复，且互相放大（mfg 重试路径同时踩两条）。

## Goals

- 增量报工每次生成自己的完工入库移动单（数量守恒：Σ移动 = WO.completedQuantity），幂等去重对同键重试仍有效。
- 过账幂等命中返回既有凭证 id（非 null），使 `voucherId != null` 族 dispatcher 在重试/重入后正确置 posted=true。

## Non-Goals

- 不改 ORM/api.xml（无模型变更）；不实现完工移动的红冲路径（mfg-003/F2.5 范畴）；不修 sweep 源单 posted 回写通道（fin-001/F2.1 范畴）；不动 REQUIRES_NEW 孤儿凭证族（F1.2）。

## Task Route

- Type: implementation-only change（缺陷修复，行为契约已在检查报告中实证）
- Owner Docs: `docs/architecture/processor-extension-pattern.md`（过账失败回退纪律）、`docs/design/manufacturing/state-machine.md`（报工语义：增量报工为设计语义）
- Skill Selection Basis: `bug-diagnosis-prompt`（根因已在检查阶段完成实证，本计划直接进入修复）；无匹配 ORM 技能（无模型变更）

## Infrastructure And Config Prereqs

No infra prereqs beyond existing baseline.

## Execution Plan

### Phase 1 - 过账引擎幂等返回语义（fin-003）

Status: completed
Targets: `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/posting/ErpFinPostingProcessor.java`
Skill: none

- Item Types: `Fix | Proof`
- Prereqs: 独立草案审查通过

- [x] Fix: `process()` 入口幂等命中分支改为返回既有 POSTED 凭证 id（从 `findBillLinks` 命中行取 `voucherId`，保持 INFO 日志）；javadoc 同步三处契约面——引擎本类、`IErpFinVoucherBiz#post`（erp-fin-dao L28-31「幂等…返回 null」改「返回既有凭证 id」）、`ErpFinDeferredPostingRetryHelper` L32-33 O-16 注释
- [x] Fix（审查 R1）：更新既有断言——`TestErpFinPostingService.java` L126 与 `TestErpFinCreditFacilityInterest.java` L145 的 `assertNull(second…)` 改为断言返回与首单相同 voucherId 且凭证数不变（幂等语义强化而非弱化）
- [x] 核查（审查 S2 结论落档）：`ErpFinDeferredPostingRetryHelper#doRetry` L104 返回值仅 DEBUG 日志、markRetried 无条件执行（两态皆成功，已由审查实证）；手动重试 `ErpFinPostingExceptionRetryProcessor#retry` L48-56 非 null 分支额外记 voucherId（严格更好）；`ErpAstValueAdjustmentProcessor` L85 增量式 NBV 联动在「REQUIRES_NEW 已提交+主事务回滚」场景首次 apply 已回滚、重试补执行恰好正确
- [x] Proof: `erp-fin-service` 新增单测：同一 PostingEvent 二次 process() 返回相同 voucherId（非 null）且不新增凭证；既有 fin 测试全绿（497 基线）

Exit Criteria:

- [x] 二次过账返回既有凭证 id 且凭证数不增（单测断言）
- [x] `mvn test -pl module-finance/erp-fin-service` 全绿

### Phase 2 - 完工入库幂等键（mfg-001）

Status: completed
Targets: `module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/processor/ErpMfgWorkOrderProcessor.java`（generateCompletionMove）及/或其调用方 `ErpMfgWorkOrderReportCompletionProcessor`
Skill: none

- Item Types: `Fix | Proof`
- Prereqs: Phase 1（无硬依赖，可并行，但同链修复便于一次回归）

- [x] Fix（审查 R2/R3 修订方案）：completion move 的 `relatedBillCode` 采用**首单精确、后续后缀**方案——首张（`findByRelatedBill(type, wo.code)` 未命中时）保持 `wo.getCode()` 精确值（既有消费者零破坏：E2E `_helper.ts` L995-1001 / `mfg-work-order-exception` L253-257 精确查找、单测 findMove 精确 eq 均命中首单）；后续报工用 `wo.getCode() + "-C" + 累计完工量规范化串`（`stripTrailingZeros().toPlainString()`，累计量严格单调保证唯一；同状态重试因首试整体回滚无误命中）。**长度守卫（R3 裁决）**：`ErpInvStockMove.relatedBillCode` VARCHAR(50)——拼接后 > 50 时抛显式 `NopException`（业务错误码「完工移动关联单号超长」），把「静默吞增量」 worst-case 转为「显式失败」，不做静默截断
- [x] Decision（R2 E2E 影响裁决）: 受影响 E2E（mfg-work-order-exception/mfg-chain/mfg-inspection-gate/mfg-genealogy 经 `_helper.ts#completionMove`）均只断言**首张**完工移动存在性与数量——首单保持精确 code 后语义不变，无需改 E2E；本计划以单测证明「首单 code 不变 + 后续单后缀」后书面裁决 E2E 零影响（全量 E2E 回归归 V.1）
- [x] Proof: `erp-mfg-service` 新增单测：两次增量报工（q1、q2）→ 两张完工移动单（数量分别为 q1、q2），库存累计 q1+q2 = WO.completedQuantity；既有工单报工测试全绿（279 基线；若有断言 relatedBillCode 精确值的既有测试同步更新期望值）

Exit Criteria:

- [x] 两次增量报工产生两张移动单且数量守恒（单测断言）；首张 relatedBillCode == wo.code（既有消费者兼容性断言）；>50 字符路径抛显式错误（守卫断言）
- [x] `mvn test -pl module-manufacturing/erp-mfg-service` 全绿

### Phase 3 - 传导站点回归验证（qa-013/prj-006/log-001 并入）

Status: completed
Targets: 代表性传导域 `erp-prj-service`（TimesheetPostingDispatcher）测试
Skill: none

- Item Types: `Proof`
- Prereqs: Phase 1

- [x] Proof: prj 侧新增回归测试：工时 approve→posted=true 后重入 approve 路径（或直接调 dispatcher tryPost）幂等命中 → posted 保持 true 不悬挂；qa/log 站点以既有测试回归覆盖（引擎层修复自动生效，不改其代码）

Exit Criteria:

- [x] 传导站点幂等重入后 posted 不悬挂（prj 单测断言）
- [x] `mvn test -pl module-projects/erp-prj-service` 全绿

## Draft Review Record

- Independent draft review iteration 1: `needs revision → 修订后通过`（agent `agent_88d022e1`，2026-08-26）——5 维度结论：基线准确性/Phase 1 可行性/保护区域合规通过；3 必改（R1 两处既有 assertNull 断言更新、R2 E2E 精确查找破坏面、R3 VARCHAR(50) 键长裁决）+ 4 建议（S1 契约 javadoc 三处/S2 消费面盘点/S3 posting.md 补注/S4 措辞）全部采纳落档；审查者实证了 doRetry/retry/ast NBV 三处返回值消费者兼容性与 E2E 精确查找位置。旁证：hr 域 RC-R1.89 已自行加 alreadyPosted 守卫绕行 null 语义——引擎层修法为正解。修订后 Plan Status: active。

## Closure Gates

- [x] 范围内行为完成（两缺陷修复 + 三域测试证明）
- [x] `mvn clean install -DskipTests` 全量构建通过 + `mvn test -pl module-finance/erp-fin-service,module-manufacturing/erp-mfg-service,module-projects/erp-prj-service,module-inventory/erp-inv-service` 全绿
- [x] compliance checker 不高于 C0.2 快照（R2c=1505 等——本修复新增 daoFor/saveEntity 站点若触发漂移走 baseline-raise 登记）
- [x] ai-check-index 回填：P0-CK-mfg-001、P1-CK-fin-003 → fixed（附测试与提交指针）；P2-CK-qa-013/prj-006(幂等部分)/log-001(幂等部分) → fixed（并入 F1.1，引擎层修复）
- [x] roadmap F1.1 → done
- [x] 独立结束审计（新会话子代理）
- [x] `docs/logs/2026/08-26.md` 追加条目

## Deferred But Adjudicated

### 完工移动红冲缺失

- Classification: out-of-scope improvement（本计划 Non-Goal）
- Why Not Blocking Closure: 属 P1-CK-mfg-003（F2.5 工单 P1 簇）独立工作项
- Successor Required: yes（F2.5）

## Closure

Status Note: 三 Phase 全绿（fin 497/497 + mfg 293/293 + prj 4/4 + 守卫 2/2）+ 全 reactor install SUCCESS + compliance 零漂移；5 条 finding 回填（4 fixed + 1 部分修复注记）。长度守卫测试与 E2E 零影响裁决补齐后复裁通过。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理 `agent_209b4974-e966-47a7-90f8-9848c9843d66`（fresh session）
- Evidence: 首轮 FAIL（3 必改：qa-013 编号笔误漏回填/RetryHelper javadoc 未生效/守卫零测试）→ 三项修复 → 复裁 **PASS**（三项独立复验全过：索引行/git diff javadoc/守卫测试 2/2 重跑绿；fin 7/7 复验；anti-hollow 与 Deferred 登记核验）

Follow-up:

- （非阻塞）fixed 注记的提交指针待本工作集 git 提交后补齐
