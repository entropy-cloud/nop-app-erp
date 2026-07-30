# 2026-07-31-0420-2-r2-10-finance-test-effectiveness R2.10 finance 过账/FX 链路测试有效性（残差补强）

> Plan Status: active
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

Status: planned
Targets: `module-finance/erp-fin-service/src/test/java/app/erp/fin/service/posting/TestErpFinPostingService.java`（新增多币种测试方法 + 对应 `_cases/` 快照/seed）
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: R2.0 done（已 done）

- [ ] Add: 通用过账服务多币种 E2E — 在现有 `apInvoiceEvent` 基础上构造非 ONE exchangeRate（如 6.5）+ 外币币种 seed，调 `voucherBiz.post()`，断言凭证行级 `getAmountSource() ≠ getAmountFunctional()`、`getExchangeRate()` 为非 ONE 折算率、借/贷方金额按 `amountSource × exchangeRate = amountFunctional` 正确（成功模式 = 行级折算正确；失败模式 = 若发现 amountSource=amountFunctional 实际 bug 则升级 Fix 不静默）
  - Skill: `nop-testing`
- [ ] Proof: Phase 1 新增测试方法首次 RECORDING 后切 CHECKING 全绿
  - `mvn test -pl module-finance/erp-fin-service -Dtest=TestErpFinPostingService`
  - Skill: none

Exit Criteria:

> 通用过账路径多币种行级断言落地，解除后续 FX 跨期测试的本地化检查（行级断言辅助可复用）。

- [ ] 多币种过账 E2E 在 CHECKING 模式绿，断言行级 amountSource/amountFunctional/exchangeRate（若发现真实折算 bug 升级 Fix）

### Phase 2 - FX 跨期 reversal 累计漂移（G2，P1-MA4-005 残差）

Status: planned
Targets: `module-finance/erp-fin-service/src/test/java/app/erp/fin/service/entity/TestErpFinExchangeRevaluation.java`（新增跨期 reversal 测试方法 + `_cases/`）
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1 done（行级断言辅助可复用）；R1.11 done（P1-MA2-022 已修复）

- [ ] Add: FX 跨期 reversal 累计漂移测试 — seed 外币 AR/AP 跨两期（P1 + P2），P1 重估生成 FX 凭证 → P1 期间结账 → P2 重估（含前期 reversal + 当期新重估），断言累计 FX 损益不漂移（前期 reversal 正确冲回 + 当期重估金额独立，闭合 P1-MA2-022 测试可见性）；校验 FX 凭证行级 amountFunctional
  - Skill: `nop-testing`
- [ ] Proof: Phase 2 新增测试方法首次 RECORDING 后切 CHECKING 全绿
  - `mvn test -pl module-finance/erp-fin-service -Dtest=TestErpFinExchangeRevaluation`
  - Skill: none

Exit Criteria:

- [ ] FX 跨期 reversal 累计漂移测试在 CHECKING 模式绿，断言前期 reversal + 当期重估累计不漂移

## Draft Review Record

- Independent draft review iteration 1: needs revision (ses_04b01dca9ffeZLunyzd8RGuynX) — 原 baseline 仅盘 3 个测试文件，遗漏 R1.16/R1.28/R1.10/R1.11 已落地的 7+ 测试类（重试耗尽/告警/deferred-retry/核销幂等/模块顺序/折旧 gate/银行 FX/年度结转），致多项提议为重复。已修订：Current Baseline 完整盘点既有覆盖 + 标注已闭合子项，范围收敛至 G1（通用过账多币种行级）+ G2（FX 跨期 reversal 漂移）两个残差，删除所有重复项，闭合声明不再对已闭合行为重复声明。
- Independent draft review iteration 2: accept (ses_04afd0356ffeeYubhAPQcUPVd9) — 实测复核：既有测试声明逐项精确（testMaxRetryEscalatesToManual:218 / testIdempotentSecondRunNoNewRecon:134 / testModuleOutOfOrderRejected:64 / testBankFxRevaluationForeignAccount:109）；G1 残差真实（TestErpFinPostingService apInvoiceEvent:267 硬编码 exchangeRate=ONE，无行级断言；NotesReceivable 走不同 Facade 不替代通用路径）；G2 残差真实（TestErpFinExchangeRevaluation 仅 2 独立单期测试，无跨期 reversal 链路）；无阻塞项。非阻塞建议（Phase1→2 helper 跨包复用为软依赖；owner-doc 扩展含 period-close.md 合理）已悉，不阻塞实施。草案审查收敛，可开始实施。

## Closure Gates

> 纯测试新增，无生产代码/ORM/view.xml 变更。完整仓库验证在此处一次。

- [ ] 范围内行为完成（G1 + G2 残差测试方法落地并 CHECKING 绿）
- [ ] 相关文档对齐（若 G1/G2 测试发现与 owner doc 不符的真实缺陷，已升级为独立 Fix 计划并记录；否则无 owner-doc 更新）
- [ ] 已运行验证：`mvn clean install -DskipTests` 全绿 + `mvn test -pl module-finance/erp-fin-service` 全绿（含新测试）+ `bash docs/audits/nop-compliance-checker.sh` 零新增命中
- [ ] 无范围内项目降级为 deferred/follow-up（发现的真实行为缺陷按不可降级规则升级 Fix，不降级）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

（无——P1-MA4-002/005 其余子项已由 R1.x 既有测试闭合，非本计划 deferred；本计划仅补 G1/G2 残差）

## Closure

Status Note: <待 closure audit 后填写>

Closure Audit Evidence:

- Auditor / Agent: <独立子代理>
- Evidence: <待 closure audit>

Follow-up:

- 无（G1 行级多币种断言辅助可被 R2.11/R2.14 复用，但不属本计划义务）
