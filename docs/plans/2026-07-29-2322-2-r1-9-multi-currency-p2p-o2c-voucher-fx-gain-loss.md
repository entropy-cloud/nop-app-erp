# 2026-07-29-2322-2-r1-9-multi-currency-p2p-o2c-voucher-fx-gain-loss R1.9 — 多币种 P2P/O2C 本位币凭证路径 + 收款核销汇兑损益

> Plan Status: active
> Last Reviewed: 2026-07-29
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

Status: planned
Targets: `VoucherFact.java`、`persistVoucher` 装配链、`PurAcctDocProvider`/`SalAcctDocProvider`、`ErpFinReconciliationBizModel`、owner docs
Skill: `nop-backend-dev`

- Item Types: `Decision | Explore`
- Prereqs: 无

- [ ] Explore: 核实 `persistVoucher` 行级装配——`ErpFinVoucherLine` 的 `amountSource`/`amountFunctional`/`exchangeRate` 实际写入逻辑（`amountSource=source`，`amountFunctional=source÷rate`？还是 P1-MA3-039 所述 `amountSource=amountFunctional` bug？），用单币种 + 构造多币种 PostingEvent 验证。
  - Skill: `nop-backend-dev`
- [ ] Decision: VoucherFact 多币种方案裁决。选择 A（VoucherFact 增 `amountSource`/`amountFunctional` 双字段，AcctDocProvider 显式传双值，消除引擎隐式折算）或 B（保留单字段 + 引擎折算路径，但补 E2E 证明正确 + 修复 P1-MA3-039 若属 bug）。记录替代方案与残留风险。注意：双字段变更触及全域 AcctDocProvider（~20 实现），范围需评估。
  - Skill: `nop-backend-dev`
- [ ] Decision: 汇兑损益 plug 范围裁决。确认 P1-MA2-009「收款核销汇兑损益未实现」的修复落在 (a) RECEIPT 过账时（SalAcctDocProvider 补 6051 plug）+ (b) 核销环节（ErpFinReconciliationBizModel 计算外币 AR vs RECEIPT 汇率差）。确认 6051 汇兑损益科目已配置（ErpFinAcctDocProvider 注册）。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] persistVoucher 行级金额写入逻辑已核实（确认或证伪 P1-MA3-039 bug）
- [ ] VoucherFact 方案 + 汇兑损益范围两项 Decision 已记录选择、替代方案、残留风险

### Phase 2 - 实现汇兑损益与多币种路径（Fix | Add）

Status: planned
Targets: `SalAcctDocProvider.RECEIPT`、`ErpFinReconciliationBizModel`/`ReconciliationSettler`、（若方案 A）`VoucherFact` + 各 AcctDocProvider
Skill: `nop-backend-dev`

- Item Types: `Fix | Add`
- Prereqs: Phase 1 裁决完成

- [ ] Fix: P1-MA2-009 收款核销汇兑损益 plug——在 RECEIPT 过账与核销环节补 6051 汇兑损益科目插平（外币 AR 与外币 RECEIPT 汇率差），config-gated 默认 false 保护既有测试 + ErrorCode。
  - Skill: `nop-backend-dev`
- [ ] 若 VoucherFact 方案 A：Add `amountSource`/`amountFunctional` 双字段 + 迁移全域 AcctDocProvider 显式传双值 + persistVoucher 行级写入对齐。
  - Skill: `nop-backend-dev`
- [ ] 若 VoucherFact 方案 B：Fix persistVoucher 行级金额写入（若 P1-MA3-039 属 bug）+ owner doc `posting.md §多币种处理` 对齐引擎折算路径实际行为。
  - Skill: `nop-backend-dev`
- [ ] Fix | Explore: P1-MA3-025 预算公式核实——`ErpFinBudgetControlBiz` 余量公式 javadoc 与实际聚合口径核实（若漂移则修正 javadoc 或代码）。注意 arm-index 标注需交叉核验代码内部是否隐含 COMMITMENT 通道（与 P1-MA2-084 / R1.27 协同，但不实现 R1.27 范围）。
  - Skill: none

Exit Criteria:

- [ ] 汇兑损益 plug 落地（RECEIPT 过账 + 核销环节）+ config-gate 默认 false
- [ ] VoucherFact 裁决方案落地（双字段迁移或单字段路径修正）
- [ ] 预算公式 javadoc 对齐

### Phase 3 - 多币种 E2E 验证（Proof）

Status: planned
Targets: 新增多币种 P2P + O2C E2E 测试
Skill: `nop-testing`

- Item Types: `Proof`
- Prereqs: Phase 2 完成

- [ ] Proof: 多币种 P2P E2E（`exchangeRate≠ONE` + 凭证行级 `amountSource≠amountFunctional` 断言 + `debitAmount`/`creditAmount` 本位币正确），闭合 P1-MA2-002「未验证」。指定测试类路径。
  - Skill: `nop-testing`
- [ ] Proof: 多币种 O2C E2E（外币 invoice + 不同汇率 receipt + 6051 汇兑损益凭证断言），闭合 P1-MA2-009「未实现」。
  - Skill: `nop-testing`
- [ ] Proof: 既有单币种 E2E 回归通过（config-gate 默认 false 不触发汇兑损益凭证）。
  - Skill: `nop-testing`

Exit Criteria:

- [ ] 多币种 P2P + O2C E2E 有行级金额 + 汇兑损益断言
- [ ] 既有单币种测试零回归

## Draft Review Record

- Independent draft review iteration 1: acceptable as-is (ses_051846e1bffeC1Kg7kURyDsQqV) — 基线准确（VoucherFact.java:16 单 amount 字段 + SalAcctDocProvider.RECEIPT 无 6051 FX plug + ErpFinPostingProcessor amountSource=amountFunctional 均经代码核实）；单一结果面连贯（多币种凭证正确性 + FX plug 共享 VoucherFact/persistVoucher 根因）；两项 Decision 含替代方案与残留风险；~20 AcctDocProvider 批量迁移正确登记 Deferred But Adjudicated。采纳非阻塞建议：Task Route 措辞精确化 + 新增保护区域裁决门控 + P1-MA3-025 改 `Fix | Explore` 含 P1-MA2-084 交叉核验注记 + 基线措辞收紧（引擎当前未折算）+ 引用 NotesReceivableAcctDocProvider 平台参考模式。

## Closure Gates

> 完整仓库验证在结束时运行一次。

- [ ] 范围内行为完成（汇兑损益 plug + VoucherFact 裁决 + 多币种 E2E + 预算 javadoc）
- [ ] 相关文档对齐（`posting.md`/`ar-ap-reconciliation.md`/`budget.md` 反映多币种与汇兑损益）
- [ ] 已运行验证：`mvn clean install -DskipTests` + `mvn test`（finance/purchase/sales 模块重点）+ compliance checker 基线不高于 M0
- [ ] 无范围内项目降级为 deferred/follow-up（已确认缺陷 P1-MA2-002/009 不得降级）
- [ ] 会计保护区域裁决已落地：所选方案有 owner-doc 证据支撑 + 残留风险已记录（由独立结束审计核验）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [ ] 结束证据存在于文件中 + arm-index P1-MA2-002/009 + P1-MA3-025/039 MR1 侧状态回填

## Deferred But Adjudicated

### VoucherFact 双字段全域迁移范围（若方案 A 触及全域 AcctDocProvider）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 若双字段迁移涉及 ~20 AcctDocProvider 全域改动，可分批迁移（O2C/P2P 优先，其余域 config-gated 跟进）；核心汇兑损益 plug 不依赖全域迁移完成。
- Successor Required: `yes`——其余域 AcctDocProvider 双字段迁移完成时复核。

## Closure

Status Note: <待 closure audit>

Closure Audit Evidence:

- Auditor / Agent: <独立审计子代理>
- Evidence: <task id / log link>

Follow-up:

- <非阻塞跟进项；已确认缺陷不得出现在此>
