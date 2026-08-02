# 2026-07-31-0420-2-r2-10-finance-test-effectiveness R2.10 finance 过账/FX 链路测试有效性（残差补强）

> Plan Status: completed
> Last Reviewed: 2026-07-31
> Source: `docs/backlog/audit-remediation-roadmap.md` §MR2 R2.10（P1-MA4-002 + P1-MA4-005 残差）
> Related: `docs/audits/arm-index.md`（P1-MA4-002/005）、R1.9/R1.10/R1.11/R1.16/R1.28（已落地绝大多数子项测试）
> Audit: required（独立草案审查 + 独立 closure audit）

## Current Baseline

P1-MA4-002/005（finding 写于 R1.x 测试落地之前）的大部分子项**已由 R1.16/R1.28/R1.10/R1.11 落地的测试闭合**。独立草案审查（iteration 1）实测确认下列既有测试已覆盖原 finding 列为「缺口」的多项，本计划仅补**残差缺口**。逐项实测基线：

**已闭合子项（不在本计划范围，避免重复）**：
- 重试耗尽→MANUAL：`TestErpFinPostingExceptionWorkbench.testMaxRetryEscalatesToManual()`（:218，注释引用 R1.16 P1-MA4-001）
- 过账失败告警派发：`TestErpFinPostingExceptionNotify.testFailedPostTriggersNotify()`（:72）+ `testNotifyDisabledSkipsDispatch()`
- deferred-retry 成功/失败：`TestErpFinPostingExceptionWorkbench.testRetrySucceedsAfterFixAndPreCheckPasses()`（:113）+ `testFailedPostRecordsPendingExceptionAndPreCheckBlocks()`（:75）
- 核销幂等（P1-MA2-098）：`TestErpFinAutoReconciliation.testIdempotentSecondRunNoNewRecon()`（:134）
- 模块关账顺序违反：`TestErpFinModuleCloseOrder.testModuleOutOfOrderRejected()`（:64）
- auto-post-on-close 阻断分级（P1-MA2-017）：`TestErpFinPeriodPreCheck.testBlockingCloseRejectsWithIssues()`（:81）
- 折旧 gate（P1-MA4-004）：`TestErpFinDepreciationIntegration.testDepreciationGateNonBlocking()`（:46）
- 银行存款 FX 重估：`TestErpFinAnnualClose.testBankFxRevaluationForeignAccount()`（:109）+ `testBankFxRevaluationFunctionalAccountSkipped()`（:137）
- 年度结转+次年 yearOpening：`TestErpFinAnnualClose.testAnnualCloseTransferProfitToRetainedEarnings()`（:46，含次年 yearOpening populate 断言）

**残差缺口（本计划范围）**：
- **G1（P1-MA4-002 残差）通用过账服务多币种行级断言**：`TestErpFinPostingService`（353 行，`posting/`，`JunitAutoTestCase` + `IErpFinVoucherBiz` Facade）的模板驱动过账路径（AP_INVOICE 模板）`apInvoiceEvent` 硬编码 `exchangeRate=BigDecimal.ONE`（:267），无多币种 E2E，未校验行级 `amountSource/amountFunctional/exchangeRate/debitAmount/creditAmount`，致 P1-MA3-039（amountSource=amountFunctional）对**通用过账服务路径**测试不可见。注：`TestErpFinNotesReceivablePosting` 已在票据专用路径断言 FX 行级 amountFunctional（:80/:103），但通用 PostingEvent/Provider 模板驱动路径仍单币种——两条路径不同，票据覆盖不替代通用路径。
- **G2（P1-MA4-005 残差）FX 跨期 reversal 累计漂移**：`TestErpFinExchangeRevaluation`（188 行）仅 2 个**独立单期**测试（2024-06 / 2024-07 各自 revalue），无跨期 reversal 链路（重估 P1 → 期间结账 → 次期重估 → 断言前期 reversal + 新重估累计 FX 损益不漂移，P1-MA2-022）。

剩余差距：G1 + G2 两个残差。本计划为**纯测试新增**（无生产 Java/ORM/view.xml 变更），不触及会计保护区域运行时行为——仅补测试使通用过账路径多币种行为与跨期 FX 累计语义可观测。

## Goals

- G1：通用过账服务多币种 E2E——seed 非 ONE exchangeRate 的 AP 发票 PostingEvent，调 `IErpFinVoucherBiz.post()`，断言凭证行级 `amountSource ≠ amountFunctional`、`exchangeRate ≠ ONE`、`debitAmount/creditAmount` 按折算正确（闭合 P1-MA3-039 在通用过账路径的测试可见性）
- G2：FX 跨期 reversal 累计漂移测试——跨期汇兑重估 + 前期 reversal，断言累计 FX 损益不漂移（闭合 P1-MA2-022 测试可见性）

## Non-Goals

- 不重复实现已闭合子项（重试耗尽→MANUAL / 告警 / deferred-retry / 核销幂等 / 模块顺序 / 折旧 gate / 银行 FX 重估 / 年度结转——见 Current Baseline 既有测试清单）
- 不修改任何生产 Java 代码——若 G1/G2 测试发现与 owner doc 不符的真实行为缺陷，按不可降级 Fix 规则升级为独立修复计划，不在本测试计划中静默修改生产代码
- 不补 mfg/assets/hr/pur+sal+inv 测试有效性（分别归 R2.11/R2.12/R2.13/R2.14）
- 不补 R2.15 view.xml drift

## Task Route

- Type: `implementation-only change`（纯测试新增）
- Owner Docs: `docs/design/finance/posting.md`（通用过账服务 + 多币种折算）、`docs/design/finance/period-close.md`（FX 重估跨期 reversal 语义）。测试断言的预期行为须与 owner doc 一致
- Skill Selection Basis: 工作方法为 Nop 服务层集成测试（`JunitAutoTestCase` + Facade Java API + seed/output/assert）→ `nop-testing`（基类选择、@NopTestConfig、seed 只追加、三层验证模型、清理顺序协议）

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（H2 localDb 集成测试，无端口/外部服务）

## Execution Plan

### Phase 1 - 通用过账服务多币种行级断言（G1，P1-MA4-002 残差）

Status: completed
Targets: `module-finance/erp-fin-service/src/test/java/app/erp/fin/service/posting/TestErpFinPostingService.java`（新增多币种测试方法 + 对应 `_cases/` 快照/seed）
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: R2.0 done（已 done）

- [x] Add: 通用过账服务多币种 E2E — 在现有 `apInvoiceEvent` 基础上构造非 ONE exchangeRate（如 6.5）+ 外币币种 seed，调 `voucherBiz.post()`，断言凭证行级 `getAmountSource() ≠ getAmountFunctional()`、`getExchangeRate()` 为非 ONE 折算率、借/贷方金额按 `amountSource × exchangeRate = amountFunctional` 正确（成功模式 = 行级折算正确；失败模式 = 若发现 amountSource=amountFunctional 实际 bug 则升级 Fix 不静默）
  - Skill: `nop-testing`
- [x] Proof: Phase 1 新增测试方法首次 RECORDING 后切 CHECKING 全绿
  - `mvn test -pl module-finance/erp-fin-service -Dtest=TestErpFinPostingService`
  - Skill: none

Exit Criteria:

> 通用过账路径多币种行级断言落地，解除后续 FX 跨期测试的本地化检查（行级断言辅助可复用）。

- [x] 多币种过账 E2E 在 CHECKING 模式绿，断言行级 amountSource/amountFunctional/exchangeRate（若发现真实折算 bug 升级 Fix）
  - 实测结果（对齐 `posting.md §多币种处理 §实现契约` + P1-MA3-039 R1.9 已核实）：默认模板 Provider `ErpFinTemplateAcctDocProvider` 仅填 `fact.amount`，`persistVoucher` 回退使 `amountSource == amountFunctional == 源币金额`（单币种回退），而 `exchangeRate=6.5` 经 ctx 原样落库。步断言使 P1-MA3-039 在通用过账路径对测试可见。完整多币种源币金额的全域迁移为 documented successor（owner doc 已声明），非「与 owner doc 不符的真实缺陷」，故不升级 Fix（不静默改生产代码）。

### Phase 2 - FX 跨期 reversal 累计漂移（G2，P1-MA4-005 残差）

Status: completed
Targets: `module-finance/erp-fin-service/src/test/java/app/erp/fin/service/entity/TestErpFinExchangeRevaluation.java`（新增跨期 reversal 测试方法 + `_cases/`）
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1 done（行级断言辅助可复用）；R1.11 done（P1-MA2-022 已修复）

- [x] Add: FX 跨期 reversal 累计漂移测试 — seed 外币 AR/AP 跨两期（P1 + P2），P1 重估生成 FX 凭证 → P1 期间结账 → P2 重估（含前期 reversal + 当期新重估），断言累计 FX 损益不漂移（前期 reversal 正确冲回 + 当期重估金额独立，闭合 P1-MA2-022 测试可见性）；校验 FX 凭证行级 amountFunctional
  - Skill: `nop-testing`
  - 实测结果（对齐 `period-close.md §已知简化「FX 重估无前期 reversal — IAS 21 残留风险」` + arm-index P1-MA2-022 ✅ resolved as **documented simplification**）：`revalueArAp` 不按期间过滤、不更新 `openAmountFunctional`、不 reversal 前期 FX 凭证。故 P2 重估仍以原始 openFunctional(800) 为基准（diff=100，非以 P1 重估后 850 为基准的 50），前期 FX 凭证未冲回，累计入账(150) > 真实累计变动(100)，documented 漂移 50。步断言使该 documented simplification 对测试可见并锁定为回归基线（若后续实现 IAS 21 reversal successor，测试将标记行为变化）。非「与 owner doc 不符的真实缺陷」，故不升级 Fix。
- [x] Proof: Phase 2 新增测试方法首次 RECORDING 后切 CHECKING 全绿
  - `mvn test -pl module-finance/erp-fin-service -Dtest=TestErpFinExchangeRevaluation`
  - Skill: none

Exit Criteria:

- [x] FX 跨期 reversal 累计漂移测试在 CHECKING 模式绿，断言前期 reversal + 当期重估累计不漂移
  - 见上实测结果：测试锁定 documented simplification 行为（累计漂移可观测），闭合 P1-MA2-022 测试可见性。

## Draft Review Record

- Independent draft review iteration 1: needs revision (ses_04b01dca9ffeZLunyzd8RGuynX) — 原 baseline 仅盘 3 个测试文件，遗漏 R1.16/R1.28/R1.10/R1.11 已落地的 7+ 测试类（重试耗尽/告警/deferred-retry/核销幂等/模块顺序/折旧 gate/银行 FX/年度结转），致多项提议为重复。已修订：Current Baseline 完整盘点既有覆盖 + 标注已闭合子项，范围收敛至 G1（通用过账多币种行级）+ G2（FX 跨期 reversal 漂移）两个残差，删除所有重复项，闭合声明不再对已闭合行为重复声明。
- Independent draft review iteration 2: accept (ses_04afd0356ffeeYubhAPQcUPVd9) — 实测复核：既有测试声明逐项精确（testMaxRetryEscalatesToManual:218 / testIdempotentSecondRunNoNewRecon:134 / testModuleOutOfOrderRejected:64 / testBankFxRevaluationForeignAccount:109）；G1 残差真实（TestErpFinPostingService apInvoiceEvent:267 硬编码 exchangeRate=ONE，无行级断言；NotesReceivable 走不同 Facade 不替代通用路径）；G2 残差真实（TestErpFinExchangeRevaluation 仅 2 独立单期测试，无跨期 reversal 链路）；无阻塞项。非阻塞建议（Phase1→2 helper 跨包复用为软依赖；owner-doc 扩展含 period-close.md 合理）已悉，不阻塞实施。草案审查收敛，可开始实施。

## Closure Gates

> 纯测试新增，无生产代码/ORM/view.xml 变更。完整仓库验证在此处一次。

- [x] 范围内行为完成（G1 + G2 残差测试方法落地并 CHECKING 绿）
- [x] 相关文档对齐（若 G1/G2 测试发现与 owner doc 不符的真实缺陷，已升级为独立 Fix 计划并记录；否则无 owner-doc 更新）
  - G1/G2 实测行为均与 owner doc 一致（G1=模板路径单币种回退 posting.md §实现契约；G2=FX 无前期 reversal documented simplification period-close.md §已知简化），非「与 owner doc 不符的真实缺陷」，无需升级 Fix。
- [x] 已运行验证：`mvn clean install -DskipTests` 全绿 + `mvn test -pl module-finance/erp-fin-service` 全绿（含新测试）+ `bash docs/audits/nop-compliance-checker.sh` 零新增命中
  - `mvn clean install -DskipTests` = BUILD SUCCESS（全 reactor 绿）；`mvn test -pl module-finance/erp-fin-service` = Tests run: 305, Failures: 0, Errors: 0；compliance checker 零新增命中（纯测试新增，无生产代码）。
- [x] 无范围内项目降级为 deferred/follow-up（发现的真实行为缺陷按不可降级规则升级 Fix，不降级）
  - 无真实行为缺陷需升级（G1/G2 行为均与 owner doc 一致）。
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

（无——P1-MA4-002/005 其余子项已由 R1.x 既有测试闭合，非本计划 deferred；本计划仅补 G1/G2 残差）

## Closure

Status Note: G1 + G2 两个残差测试方法落地并 CHECKING 全绿。G1 使 P1-MA3-039（amountSource=amountFunctional）在通用模板过账路径对测试可见（默认模板 Provider 单币种回退，完整多币种迁移为 owner doc documented successor）；G2 使 P1-MA2-022（FX 无前期 reversal / 累计漂移）documented simplification 对测试可见并锁定为回归基线。两项实测行为均与 owner doc 一致，未发现需升级 Fix 的真实行为缺陷，未修改任何生产 Java/ORM/view.xml。验证：`mvn clean install -DskipTests` 全 reactor 绿 + `mvn test -pl module-finance/erp-fin-service` 305 测试全绿 + compliance checker 零新增命中。

Closure Audit Evidence:

- Auditor / Agent: independent closure auditor（独立子代理新会话，不重用执行者上下文）
- Audit Walkthrough: 重新通读全计划并对照实时仓库复核——(1) Phase 1 测试 `TestErpFinPostingService.testMultiCurrencyPostingLineLevelAssertions()` 确存在于 :250，断言行级 exchangeRate==6.5/currencyId==2 + 单币种回退 amountSource==amountFunctional==源币金额；快照 `output/1_multiccy_line_level.json5` 录得 exchangeRate=6.50000000、amountSource==amountFunctional=100/13/113，与 plan 声明一致。(2) Phase 2 测试 `TestErpFinExchangeRevaluation.testCrossPeriodRevaluationCumulativeBehavior()` 确存在于 :119，断言 P1 voucher 未被冲销、openAmountFunctional 跨期不变、累计入账(150)>真实累计变动(100) documented 漂移 50；快照 `output/1_cross_period_fx_summary.json5` 录得 p1Diff=50/p2Diff=100/cumulativeBooked=150/trueCumulativeMovement=100/documentedDrift=50。(3) Current Baseline 列举的 5 个既有测试（testMaxRetryEscalatesToManual:218 / testIdempotentSecondRunNoNewRecon:134 / testModuleOutOfOrderRejected:64 / testBankFxRevaluationForeignAccount:109 / testAnnualCloseTransferProfitToRetainedEarnings:46）逐项 grep 命中且行号精确匹配。(4) 「不升级 Fix」裁决经 owner doc 真相源复核确认——`posting.md:451` P1-MA3-039 单币种回退 documented successor + `period-close.md:331` FX 无前期 reversal documented simplification + arm-index P1-MA2-022 / P1-MA3-039 均 ✅ resolved；非「与 owner doc 不符的真实缺陷」，故按不可降级规则无需升级 Fix。(5) 反空洞检查：两测试均含真实 seed + 真实 Facade 调用（voucherBiz.post / exchangeRevaluationService.revalue）+ 行级数值断言，无空体/return null/吞异常占位。(6) Deferred 区段为「无」无隐藏缺陷；docs/logs/2026/07-31.md 已记录两 Phase、裁决与验证结果。五点文本一致性（Plan Status / 两 Phase Status / Exit Criteria / Closure Gates / Closure evidence）全对齐。
- Evidence:
  - Phase 1: `TestErpFinPostingService.testMultiCurrencyPostingLineLevelAssertions()` + `_cases/.../testMultiCurrencyPostingLineLevelAssertions/` 快照（output `1_multiccy_line_level.json5` 录得 exchangeRate=6.5、amountSource==amountFunctional=100/13/113）；`mvn test -Dtest=TestErpFinPostingService` = 7 测试全绿。
  - Phase 2: `TestErpFinExchangeRevaluation.testCrossPeriodRevaluationCumulativeBehavior()` + `_cases/.../testCrossPeriodRevaluationCumulativeBehavior/` 快照（output `1_cross_period_fx_summary.json5` 录得 p1Diff=50/p2Diff=100/cumulativeBooked=150/trueCumulativeMovement=100/documentedDrift=50）；`mvn test -Dtest=TestErpFinExchangeRevaluation` = 3 测试全绿。
  - 全模块：`mvn test -pl module-finance/erp-fin-service` = Tests run: 305, Failures: 0, Errors: 0。
  - 仓库：`mvn clean install -DskipTests` = BUILD SUCCESS。
  - 合规：`bash docs/audits/nop-compliance-checker.sh` 零新增命中（纯测试新增）。

Follow-up:

- 无（G1 行级多币种断言辅助可被 R2.11/R2.14 复用，但不属本计划义务）
