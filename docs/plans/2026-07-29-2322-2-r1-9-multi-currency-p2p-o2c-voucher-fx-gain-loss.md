# 2026-07-29-2322-2-r1-9-multi-currency-p2p-o2c-voucher-fx-gain-loss R1.9 — 多币种 P2P/O2C 本位币凭证路径 + 收款核销汇兑损益

> Plan Status: completed
> Last Reviewed: 2026-07-30
> Source: `docs/backlog/audit-remediation-roadmap.md` MR1 R1.9（P1-MA2-002 + P1-MA2-009 + P1-MA3-025 MR1 侧 + P1-MA3-039 MR1 侧）
> Related: `docs/plans/2026-07-29-2322-1-r1-8-p2p-grni-accrual-reversal-payment-match.md`（R1.8 共享 P2P 链路但结果面正交）、`docs/plans/2026-07-28-2130-arm-ma4-finance-posting-voucher-code-quality.md`（P1-MA3-039 首次登记）、`docs/plans/2026-07-27-1949-1-audit-remediation-ma2-procure-to-pay-e2e.md`+`2026-07-27-1949-2-...-order-to-cash-e2e.md`（MA2 端到端审计，本计划修复其 P1）
> Audit: required

## Current Baseline

- **VoucherFact 单一 amount 字段**（P1-MA2-002 / P1-MA2-009 / P1-MA3-039）：`VoucherFact`（`module-finance/erp-fin-service/.../service/posting/VoucherFact.java`）仅单一 `amount` 字段，无 `amountSource`/`amountFunctional` 分离。`PurAcctDocProvider.createFacts` + `SalAcctDocProvider.createFacts` 将 source-currency `TOTAL_*` 直接写入 `fact.amount`。本位币折算**设计意图**为过账引擎装配 `ErpFinVoucherLine` 时按 `PostingEvent.exchangeRate` 转换，但 P1-MA3-039 证实 `ErpFinPostingProcessor` 实际写 `amountSource=amountFunctional=amt`（引擎当前未折算），故多币种行级金额当前不正确。
- **多币种 P2P 路径未验证**（P1-MA2-002）：上述折算路径在 P2P 链无 E2E 证据。E2E（`TestErpPurProcureToPayEnd`/`TestErpPurReturnRefundEndToEnd`）均单币种（`exchangeRate=ONE`）。owner doc `posting.md §多币种处理` 契约在 P2P GL 层落实无测试证据。
- **多币种 O2C + 收款核销汇兑损益未实现**（P1-MA2-009，比 P2P 更严重）：(a) 同上折算路径在 O2C 无 E2E 证据；(b) `SalAcctDocProvider.RECEIPT` 只生成 借银行存款 / 贷应收（同金额），无 6051 汇兑损益科目插平；`ErpFinArApItemGenerator` + `ErpFinReconciliationBizModel` 不计算外币 AR 与外币 RECEIPT 的汇率差 plug。当前实现假设 invoice 与 receipt 同币种同汇率。
- **persistVoucher amountSource=amountFunctional**（P1-MA3-039 MR1 侧）：需核实 `persistVoucher` 装配行级时 `amountSource`/`amountFunctional`/`exchangeRate` 写入是否正确（MA3 发现 doc 描述与代码可能漂移，MR1 侧为代码核实）。
- **预算公式 javadoc**（P1-MA3-025 MR1 侧）：`ErpFinBudgetControlBiz` 余量公式 javadoc 与实际聚合口径核实。
- **验证基线**：`mvn clean install -DskipTests` 全绿；`mvn test` 全绿。所有过账 E2E 单币种。

## Goals

- 裁决并落地 VoucherFact 多币种处理方案（双字段 vs 引擎折算路径核实+保留单字段）。
- 实现 O2C 收款核销汇兑损益 plug（P1-MA2-009 明确「未实现」——6051 汇兑损益科目在 RECEIPT 过账与核销环节补插平）。
- 补多币种 P2P + O2C E2E（`exchangeRate≠ONE` + 行级 `amountSource`/`amountFunctional` 断言），闭合「未验证」。
- 核实 persistVoucher 行级金额写入（P1-MA3-039 MR1 侧）+ 预算公式 javadoc（P1-MA3-025 MR1 侧）。
- arm-index 中四项发现状态回填。

## Non-Goals

- 多币种凭证行级断言的系统性测试补齐（R2.10/R2.14 MR2 测试维度，含各域多币种投影）。
- 银行存款外币重估（已落地 plan 2026-07-05-0540-2，不在范围）。
- FX 重估前期 reversal（R1.11 / P1-MA2-022）。
- 多账套/多公司多币种（R1.29）。

## Task Route

- Type: `implementation-only change`（含会计保护区域——汇兑损益是 GL 凭证科目，独立 plan-audit + closure-audit 必需；无 ORM 变更故 ORM ask-first 人工确认未触发；保护区域裁决须有 owner-doc 证据支撑，由独立结束审计核验）
- Owner Docs: `docs/design/finance/posting.md`（§多币种处理）+ `docs/design/finance/flow-overview.md §4.3`（O2C 链路）+ `docs/design/finance/ar-ap-reconciliation.md`（核销汇兑损益）+ `docs/design/finance/budget.md`（余量公式）
- Skill Selection Basis: 触及过账引擎 VoucherFact/persistVoucher + AcctDocProvider + 核销引擎（跨实体 Facade），加载 `nop-backend-dev`；多币种 E2E 测试加载 `nop-testing`。无 ORM 变更（VoucherFact 是 Java DTO 非 ORM 实体）。汇兑损益 plug 的平台参考模式见 `NotesReceivableAcctDocProvider:80,86-90`（已实现 6051 FX plug，可作 SalAcctDocProvider.RECEIPT 修复参照）。

## Infrastructure And Config Prereqs

- 汇兑损益 plug 可能 config-gated（如 `erp-fin.fx-gain-loss-on-receipt-enabled` 默认 false 保护既有单币种测试）；具体在 Phase 1 裁决后确定。
- 回滚策略：汇兑损益凭证为独立业务类型 EXCHANGE_GAIN_LOSS（已存在），reverse 路径幂等。

## Execution Plan

### Phase 1 - 多币种折算路径与汇兑损益裁决（Decision-heavy）

Status: completed
Targets: `VoucherFact.java`、`persistVoucher` 装配链、`PurAcctDocProvider`/`SalAcctDocProvider`、`ErpFinReconciliationBizModel`、owner docs
Skill: `nop-backend-dev`

- Item Types: `Decision | Explore`
- Prereqs: 无

- [x] Explore: 核实 `persistVoucher` 行级装配——`ErpFinVoucherLine` 的 `amountSource`/`amountFunctional`/`exchangeRate` 实际写入逻辑。
  - Skill: `nop-backend-dev`
  - **结论：P1-MA3-039 bug 确认**。`ErpFinPostingProcessor.persistVoucher:818-819` 写 `line.setAmountSource(amt); line.setAmountFunctional(amt);`——两字段同值（源币种金额 `amt`），未按 `ctx.exchangeRate` 折算本位币。`amt` 来自 `fact.getAmount()`，而 Provider（`PurAcctDocProvider:79-90` / `SalAcctDocProvider:79-90`）将 billData 的 source-currency `TOTAL_*` 直接写入 `fact.amount`。引擎当前不折算。`CloseVoucherWriter:122-123` 同型（期末结账凭证，本计划不触及）。单币种（exchangeRate=ONE）场景 amountSource==amountFunctional 无影响；多币种场景 GL 行级本位币金额错误。
- [x] Decision: VoucherFact 多币种方案裁决。
  - Skill: `nop-backend-dev`
  - **选择方案 A**：VoucherFact 增 `amountSource`/`amountFunctional` 双字段，AcctDocProvider 显式传双值（functional = source × ctx.exchangeRate），消除引擎隐式折算。`persistVoucher` 忠实写入双字段；`amount` 字段保留作向后兼容（balanceTotals/assertBalanced/reversalDraft 复用，语义=functional）。
  - **替代方案 B（保留单字段 + 引擎折算）拒绝**：`posting.md:488` 明示「本位币金额在业务单据创建时按业务日期汇率锁定，过账时不重新计算」——functional 应由 Provider（持有文档锁定值）显式传递，而非引擎按 ctx.exchangeRate 重新派生（有使用与文档不同汇率的风险）。
  - **范围**：本计划迁移 `PurAcctDocProvider`（P2P）+ `SalAcctDocProvider`（O2C）；其余 ~18 AcctDocProvider 保持单币种（未设新字段时 fallback 到 `amount`，行为不变）——全域迁移登记为 `Deferred But Adjudicated`（successor：其余域 Provider 双字段迁移完成时复核）。
  - **残留风险**：未迁移 Provider 在外币场景下仍有 amountSource=amountFunctional（同 bug）；缓解：(1) 无需 config-gate（仅字段填充）；(2) successor 已登记。
- [x] Decision: 汇兑损益 plug 范围裁决。
  - Skill: `nop-backend-dev`
  - **(a) RECEIPT 过账时：不补独立 6051 plug**。RECEIPT 经方案 A 多币种正确化（银行存款 + 应收均按收款本位币 functional，两侧同率同额自平衡，无失衡故无 plug 需求）。收款时无发票可比对，plug 无业务依据。
  - **(b) 核销环节：补 realized FX plug**。`ErpFinReconciliationBizModel.post()` 结算后计算 realized 汇兑差额 = Σ(payment.functionalSettled) − Σ(invoice.functionalSettled)，其中 functionalSettled = settledSource × item.exchangeRate（per-item，发票按发票汇率、收付款按收付款汇率）。差额 ≠ 0 时生成 EXCHANGE_GAIN_LOSS 凭证（经 `CloseVoucherWriter` 范式，对齐 `ExchangeRevaluationService` 已验证模式），回写 `head.fxGainLoss`。config-gate `erp-fin.recon-fx-gain-loss-enabled` 默认 false 保护既有单币种测试。
  - **owner-doc 证据**：`ar-ap-reconciliation.md §汇兑损益核销规则:274-283` 明示汇兑损益在核销时点确认（「差异处理：源币种已核销金额 × 核销日汇率 − 原发票本位币金额 = 汇兑差异」→「汇兑差异凭证：汇兑收益/损失计入当期财务费用（汇兑损益科目）」）。本实现采用 realized-FX 口径（收付款 functional − 发票 functional），与 GL AR/AP 科目清零语义一致（期末重估口径由 `period-close.md §汇兑重估` 承接，两时点分离见 doc:285）。
  - **6051 科目配置已存在**：`ErpFinConstants.CONFIG_FX_GAIN_LOSS_SUBJECT_CODE = "erp-fin.exchange-gain-loss-subject-code"`（`ExchangeRevaluationService:122` 已用）；`NotesReceivableAcctDocProvider:40,86-90` 已实现 6051 plug 范式作参照。

Exit Criteria:

- [x] persistVoucher 行级金额写入逻辑已核实（确认 P1-MA3-039 bug：amountSource=amountFunctional=amt，引擎未折算）
- [x] VoucherFact 方案 + 汇兑损益范围两项 Decision 已记录选择、替代方案、残留风险

### Phase 2 - 实现汇兑损益与多币种路径（Fix | Add）

Status: completed
Targets: `SalAcctDocProvider.RECEIPT`、`ErpFinReconciliationBizModel`/`ReconciliationSettler`、（若方案 A）`VoucherFact` + 各 AcctDocProvider
Skill: `nop-backend-dev`

- Item Types: `Fix | Add`
- Prereqs: Phase 1 裁决完成

- [x] Fix: P1-MA2-009 收款核销汇兑损益 plug——在 RECEIPT 过账与核销环节补 6051 汇兑损益科目插平（外币 AR 与外币 RECEIPT 汇率差），config-gated 默认 false 保护既有测试 + ErrorCode。
  - Skill: `nop-backend-dev`
  - **落地**：(a) RECEIPT 经方案 A 多币种正确化（银行+应收均按收款 functional，无需独立 plug——收款时无发票可比对）。(b) 核销环节：`ReconciliationSettler.settleWithFx`（per-item functional）+ `ErpFinReconciliationBizModel.generateReconFxVoucher`（`CloseVoucherWriter` 写 EXCHANGE_GAIN_LOSS 凭证，billHeadCode=`RECON-FX-{code}`）+ `reverseReconFxVoucher`（红冲闭环）。config `erp-fin.recon-fx-gain-loss-enabled` 默认 false。
- [x] 若 VoucherFact 方案 A：Add `amountSource`/`amountFunctional` 双字段 + 迁移全域 AcctDocProvider 显式传双值 + persistVoucher 行级写入对齐。
  - Skill: `nop-backend-dev`
  - **落地**：`VoucherFact` 增双字段；`ErpFinPostingProcessor.persistVoucher:818-819` 修正为忠实写入（debit/credit/amountFunctional 按本位币，amountSource 按源币种，fallback 到 amount）；`translateFactsForSchema` 复制双字段。迁移 `PurAcctDocProvider` + `SalAcctDocProvider`（functional = source × ctx.exchangeRate）。`ErpFinArApItemGenerator` 按 event.exchangeRate 折算辅助账 functional。其余域 Provider 单币种 fallback（successor 已登记）。
- [x] 若 VoucherFact 方案 B：Fix persistVoucher 行级金额写入（若 P1-MA3-039 属 bug）+ owner doc `posting.md §多币种处理` 对齐引擎折算路径实际行为。
  - Skill: `nop-backend-dev`
  - **N/A**——选择方案 A（方案 B 不适用）。persistVoucher bug 已在方案 A 中一并修复。
- [x] Fix | Explore: P1-MA3-025 预算公式核实——`ErpFinBudgetControlBiz` 余量公式 javadoc 与实际聚合口径核实（若漂移则修正 javadoc 或代码）。注意 arm-index 标注需交叉核验代码内部是否隐含 COMMITMENT 通道（与 P1-MA2-084 / R1.27 协同，但不实现 R1.27 范围）。
  - Skill: none
  - **核实结论**：`aggregateAmount(...,false)` 过滤器 `or(isNull("postingType"), ne("postingType", BUDGET))` **放行 COMMITMENT**——actualBalance 隐式包含承付款，故 `available = budget − actualBalance` 与 doc 三项式 `budget − commitment − actual` **等价正确**（P1-MA2-084 读法确认）。javadoc 原称「NORMAL（含 NULL）」漂移已修正为「非 BUDGET（含 NORMAL/NULL/COMMITMENT）」并标注等价关系。代码无需改动（等价正确）。

Exit Criteria:

- [x] 汇兑损益 plug 落地（核销环节 settleWithFx + generateReconFxVoucher + reverseReconFxVoucher）+ config-gate 默认 false
- [x] VoucherFact 裁决方案落地（方案 A 双字段迁移 P2P/O2C Provider + persistVoucher 修正 + ArApItemGenerator 折算）
- [x] 预算公式 javadoc 对齐（actualBalance 含 COMMITMENT 等价三项式，javadoc 已修正）

### Phase 3 - 多币种 E2E 验证（Proof）

Status: completed
Targets: 新增多币种 P2P + O2C E2E 测试
Skill: `nop-testing`

- Item Types: `Proof`
- Prereqs: Phase 2 完成

- [x] Proof: 多币种 P2P E2E（`exchangeRate≠ONE` + 凭证行级 `amountSource≠amountFunctional` 断言 + `debitAmount`/`creditAmount` 本位币正确），闭合 P1-MA2-002「未验证」。指定测试类路径。
  - Skill: `nop-testing`
  - **落地**：`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurMultiCurrencyPosting.java`（2 用例）。外币 AP_INVOICE（rate=7.0）+ PAYMENT，断言行级 amountSource=源币 / amountFunctional=源币×7.0 / debitAmount=creditAmount=本位币 / amountSource≠amountFunctional / 头合计按本位币平衡。
- [x] Proof: 多币种 O2C E2E（外币 invoice + 不同汇率 receipt + 6051 汇兑损益凭证断言），闭合 P1-MA2-009「未实现」。
  - Skill: `nop-testing`
  - **落地**：`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalMultiCurrencyReconFx.java`（1 用例）。外币 AR_INVOICE（rate=7.0）+ RECEIPT（rate=7.1）+ 核销（config-gate enabled），断言：行级多币种分离 + 辅助账 amountFunctional=source×rate（7910/8023）+ 核销单 fxGainLoss=113（收益）+ EXCHANGE_GAIN_LOSS 凭证生成（Dr 应收 113 / Cr 汇兑损益 113）。
- [x] Proof: 既有单币种 E2E 回归通过（config-gate 默认 false 不触发汇兑损益凭证）。
  - Skill: `nop-testing`
  - **落地**：全工作区 `mvn test` BUILD SUCCESS（零 failures / 零 errors），finance 135 + purchase + sales 136 + 全域下游模块全绿。config-gate 默认 false → 既有单币种核销走 `settle`（非 `settleWithFx`），行为完全不变。

Exit Criteria:

- [x] 多币种 P2P + O2C E2E 有行级金额 + 汇兑损益断言
- [x] 既有单币种测试零回归

## Draft Review Record

- Independent draft review iteration 1: acceptable as-is (ses_051846e1bffeC1Kg7kURyDsQqV) — 基线准确（VoucherFact.java:16 单 amount 字段 + SalAcctDocProvider.RECEIPT 无 6051 FX plug + ErpFinPostingProcessor amountSource=amountFunctional 均经代码核实）；单一结果面连贯（多币种凭证正确性 + FX plug 共享 VoucherFact/persistVoucher 根因）；两项 Decision 含替代方案与残留风险；~20 AcctDocProvider 批量迁移正确登记 Deferred But Adjudicated。采纳非阻塞建议：Task Route 措辞精确化 + 新增保护区域裁决门控 + P1-MA3-025 改 `Fix | Explore` 含 P1-MA2-084 交叉核验注记 + 基线措辞收紧（引擎当前未折算）+ 引用 NotesReceivableAcctDocProvider 平台参考模式。

## Closure Gates

> 完整仓库验证在结束时运行一次。

- [x] 范围内行为完成（汇兑损益 plug + VoucherFact 裁决 + 多币种 E2E + 预算 javadoc）
- [x] 相关文档对齐（`posting.md`/`ar-ap-reconciliation.md` 反映多币种与汇兑损益；budget javadoc 已修正）
- [x] 已运行验证：`mvn clean install -DskipTests` + `mvn test`（全工作区 BUILD SUCCESS，零 failures/errors）
- [x] 无范围内项目降级为 deferred/follow-up（已确认缺陷 P1-MA2-002/009 已闭合；P1-MA3-025/039 MR1 侧已闭合）
- [x] 会计保护区域裁决已落地：所选方案有 owner-doc 证据支撑（`posting.md:488` 汇率锁定 + `ar-ap-reconciliation.md §汇兑损益核销规则`）+ 残留风险已记录（全域 AcctDocProvider 迁移 successor）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [x] 结束证据存在于文件中 + arm-index P1-MA2-002/009 + P1-MA3-025/039 MR1 侧状态回填

## Deferred But Adjudicated

### VoucherFact 双字段全域迁移范围（若方案 A 触及全域 AcctDocProvider）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 若双字段迁移涉及 ~20 AcctDocProvider 全域改动，可分批迁移（O2C/P2P 优先，其余域 config-gated 跟进）；核心汇兑损益 plug 不依赖全域迁移完成。
- Successor Required: `yes`——其余域 AcctDocProvider 双字段迁移完成时复核。

## Closure

Status Note: 全部 3 Phase 执行完毕（Decision + Fix/Add + Proof），全工作区 `mvn test` BUILD SUCCESS。独立 closure audit 会话（2026-07-30）通过。

Closure Audit Evidence:

- Auditor / Agent: 独立 closure-audit 子代理（新会话，不重用执行者上下文）— 语义五点核验 + 反空心检查通过。
- Evidence: (1) `VoucherFact.java:21,26` 双字段 `amountSource`/`amountFunctional` 落地（含 getter/setter + fallback javadoc 契约）；(2) `ErpFinPostingProcessor.persistVoucher:810-826` 忠实写入双字段（debit/credit 按 functional，fallback 到 amount 向后兼容）—— P1-MA3-039 bug 已修复核实；(3) `PurAcctDocProvider:113-114` + `SalAcctDocProvider:112-114` 双字段迁移（functional=source×ctx.exchangeRate）+ 单行构造辅助 `appendFact(...)` 范式一致；(4) `ErpFinReconciliationBizModel.post():139-144` FX plug 经 `isReconFxGainLossEnabled()` config-gate 接线（默认 false 走 `settle` 单币种路径，enabled 走 `settleWithFx`+`generateReconFxVoucher`）—— 反空心确认运行时可达；(5) `ReconciliationSettler.settleWithFx` per-item functional（settledSource × item.exchangeRate）+ FX 差额计算（Σpayment.functional − Σinvoice.functional）；(6) `generateReconFxVoucher`/`reverseReconFxVoucher` 经 CloseVoucherWriter 写/红冲 EXCHANGE_GAIN_LOSS 凭证（billHeadCode=`RECON-FX-{code}`），与 `NotesReceivableAcctDocProvider` 已验证 6051 FX plug 范式对齐；(7) `ErpFinConstants.CONFIG_RECON_FX_GAIN_LOSS_ENABLED="erp-fin.recon-fx-gain-loss-enabled"` 默认 FALSE 保护既有测试；(8) `ErpFinBudgetControlBiz` javadoc 修正为「非 BUDGET（含 NORMAL/NULL/COMMITMENT）」并标注与 doc 三项式等价（P1-MA3-025 MR1 侧核实：代码等价正确，无需改动）；(9) 测试断言实仓核实——`TestErpPurMultiCurrencyPosting`（rate=7.0 + 行级 amountSource=源币/amountFunctional=700/debit=credit=本位币 791/amountSource≠amountFunctional）+ `TestErpSalMultiCurrencyReconFx`（invoice rate=7.0 + receipt rate=7.1 + 辅助账 functional 7910/8023 + fxGainLoss=113 收益 + Dr 应收 113/Cr 汇兑损益 113 凭证断言）；(10) 文档同步——`docs/logs/2026/07-30.md` 完整条目 + `posting.md §多币种处理` 增实现契约段（line 490-496）+ `ar-ap-reconciliation.md §汇兑损益核销规则` 增实现契约段（line 285-288）+ `arm-index.md` P1-MA2-002/009 + P1-MA3-025/039 MR1 侧 ✅ resolved 回填（line 148,150,242,256）；(11) Deferred 诚实——VoucherFact 双字段全域迁移登记 successor（触发条件明确：其余域 Provider 双字段迁移完成时复核），无范围内缺陷降级；(12) 五点一致性——Plan Status=completed / 3 Phase Status=completed / 各 Exit Criteria 全 `[x]` / Closure Gates 全 `[x]` / 日志条目一致。

Follow-up:

- <非阻塞跟进项；已确认缺陷不得出现在此>
