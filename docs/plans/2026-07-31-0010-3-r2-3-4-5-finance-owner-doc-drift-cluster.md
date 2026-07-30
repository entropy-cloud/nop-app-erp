# 2026-07-31-0010-3-r2-3-4-5-finance-owner-doc-drift-cluster

> Plan Status: active
> Last Reviewed: 2026-07-31
> Source: `docs/backlog/audit-remediation-roadmap.md` MR2 / R2.3 + R2.4 + R2.5（P1-MA3-024~039 文档侧）
> Related: `docs/audits/2026-07-28-1953-arm-ma3-owner-doc-vs-code-drift.md`（A3.3 finance drift 审计报告）；`docs/plans/2026-07-31-0010-1-r2-1-design-doc-execution-status-leakage-cleanup.md`（R2.1）；`docs/plans/2026-07-31-0010-2-r2-2-global-view-docs-extension-domains.md`（R2.2）
> Audit: required

## Current Baseline

- **审计来源**：A3.3 finance owner doc vs 代码 drift 审计（报告 `2026-07-28-1953-arm-ma3-owner-doc-vs-code-drift.md`）登记 finance 域 16 项 NEW P1（P1-MA3-024~039）+ 9 项 P2 watch-only（P2-MA3-023~031）。R2.0 将文档侧展开为 R2.3（状态机/dict/字段语义）、R2.4（过账/事务/承付/多币种）、R2.5（配置键/门控）三个工作项行（均 status `todo`）。
- **同组件同结果表面**：三工作项均为 finance 域 owner doc vs 代码 drift，同一组件（finance 设计文档）同一结果表面（finance owner doc 与代码一致）。按 plan 指南规则 14（同一组件多功能 = 单 owner plan），合并为本计划三阶段。
- **MR1 代码侧已闭合**：P1-MA3-025（预算公式）+ P1-MA3-039（persistVoucher 多币种）的**代码侧**核实归 R1.9（MR1 done）。本计划仅处理**文档侧**——更新 finance owner doc 反映已核实的 code 行为。
- **现状差距**（逐 finding 实时基线，grep 可复现）：
  - **R2.3 — 状态机/dict/字段语义（5 项）**：
    - **P1-MA3-024 [blocker]**：期间状态机 CLOSED 语义三源冲突。`state-machine.md:130,138` CLOSED=未开启；`period-close.md:153-158` CLOSED=已结账待复核；`use-cases.md:11` 列 4 态无 NEVER_OPENED。Code+ORM（`ErpFinConstants.java:139-143` + `orm.xml:200-206`）有 5 态含 NEVER_OPENED，CLOSED=已结账。
    - **P1-MA3-026 [blocker]**：postingType 字典三处真相源不一致。`budget.md:96` 用数值码 ACTUAL=10/BUDGET=20/COMMITMENT=30/RESERVATION=40；ORM `orm.xml:40-50` 有 7 字符串值（NORMAL/OPENING_BALANCE/ADJUSTMENT/CLOSING/REVERSAL/BUDGET/COMMITMENT，无 ACTUAL 无 RESERVATION）；`ErpFinConstants.java:164-169` 仅 4 常量。
    - **P1-MA3-027**：ar-ap-status 命名漂移。`ar-ap-reconciliation.md:139-153` 声明 UNRECONCILED/PARTIAL/RECONCILED/OVER，ORM `orm.xml:222-228` 有 OPEN/PARTIAL/SETTLED/CANCELLED/WRITTEN_OFF。
    - **P1-MA3-028**：bank-stmt-status 字典文档自相矛盾。`bank-reconciliation.md:27,89` 声明加 dict（DRAFT/RECONCILING/RECONCILED/CANCELLED），同文件 :145 撤回（复用 voucher-status）。ORM 无 bank-stmt-status dict，code 用 VOUCHER_STATUS_*。
    - **P1-MA3-029**：合并抵消实体命名不一致。`intercompany-consolidation.md:40-46` 列 5 实体名，code 仅 2 实体（ErpFinConsolidationElimination/ErpFinIntercompanyMatch）。
  - **R2.4 — 过账/事务/承付/多币种（5 项，文档侧）**：
    - **P1-MA3-025 MR2 侧**：预算余量公式 doc 三项式（`budget.md:59` + `use-cases.md:212,253`：`available = budget − commitment − actual`）vs code javadoc 二项式（`ErpFinBudgetControlBiz.java:40-42,74-76`：`actualBalance = postingType=NORMAL`，`available = budgetBalance − actualBalance`）。
    - **P1-MA3-030 [blocker]**：`IErpFinVoucherBiz.reverse()` REQUIRES_NEW。`posting.md:399` 明示「不像 post() 叠加 REQUIRES_NEW」，code `ErpFinVoucherBizModel.java:77-79` 实际有 `@Transactional(REQUIRES_NEW)` + O-7 注释承认 doc 未更新。
    - **P1-MA3-031 [blocker]**：CommitmentAcctDocProvider budget.md vs posting.md 矛盾。`budget.md:264-267` 说 Provider 支持 businessType + 生成 VoucherFacts；`posting.md:541-543` 说返回空集 + 承付凭证由 CommitmentVoucherGenerator 直接写入。Code `CommitmentAcctDocProvider.java:33-44` getSupportedBusinessTypes 返回 emptySet + createFacts 返回 emptyList，匹配 posting.md。
    - **P1-MA3-032 [blocker]**：auto-post-on-close 默认值相反。`period-close.md:285` 声明默认 true，code `ErpFinConstants.java:114` + `ErpFinAccountingPeriodProcessor.java:681-684` 默认 false。
    - **P1-MA3-039 MR2 侧**：persistVoucher 多币种折算路径未文档化。`posting.md:481-484` 声明凭证分录行同时记录 amountSource/amountFunctional/币种/汇率，code `ErpFinPostingProcessor.java:816-819` 设 `amountSource=amountFunctional`（两者相等，无币种折算）。
  - **R2.5 — 配置键/门控（6 项）**：
    - **P1-MA3-033 [blocker]**：auto-depreciation 键名漂移。doc `erp-fin.auto-depreciation`（`period-close.md:287` + `domain-design-guidelines.md:662`）vs code `erp-fin.auto-depreciation-on-close`（`ErpFinConstants.java:116`）。
    - **P1-MA3-034**：多账套配置 4 doc 键 vs 2 code 键。`multiple-accounting-schemas.md:251-256` 声明 default-schema/multi-schema-enabled/schema-inheritance/auto-create-all-schemas；code `ErpFinConstants.java:193-196` 仅 multi-schema-enabled + default-schema-nature。
    - **P1-MA3-035**：合并抵消配置 4 doc 键 vs 3 code 键零重叠。`intercompany-consolidation.md:147-150` 声明 4 键；code `ErpFinConstants.java:455-460` 有 3 键（intercompany-posting-enabled/consolidation-elimination-enabled/elimination-inventory-profit-enabled）。
    - **P1-MA3-036**：reverse-close-approval-required 审批框架 vs kill-switch。doc 框架为「审批门控」，code 默认 true 直接 throw（无审批 action，纯 kill-switch）。
    - **P1-MA3-037**：报销/借款默认值相反 + 幻影键。`expense-claim.md:186` 声明 expense-budget-check-enabled 默认 true，code 默认 false；imprest-topup-threshold grep 零 code 引用。
    - **P1-MA3-038**：AR-AP 自动核销规则命名漂移。`ar-ap-reconciliation.md:122-127` 声明 4 规则键，code `ErpFinConstants.java:24-26` 实际 1 策略枚举 auto-recon-strategy（FIFO/BY_AMOUNT/BY_RATIO）。
- **R2.1 协调点**：R2.1 Phase 1 会清除 finance 文档中的执行状态标记（裁决 1/2/3 等）。本计划处理 dict/状态机/config 与 code 的语义 drift，不同维度。建议本计划在 R2.1 Phase 1 之后执行（避免 finance 文档被两计划同时编辑产生合并冲突），或顺序执行 R2.1→R2.3-R2.5。

## Goals

- **G1（R2.3）**：统一 finance dict/状态机/字段语义文档为单一真相源，对齐 ORM/code 实际命名与值集。
- **G2（R2.4）**：更新 finance 过账/事务/承付/多币种文档反映已核实的 code 行为（事务传播 / Provider 描述 / 默认值 / 多币种折算路径）。
- **G3（R2.5）**：将 finance 配置键表更新为 code 实际键名与默认值，移除幻影键，标注 kill-switch successor。

## Non-Goals

- R2.1 finance 文档执行状态 scrub——见 plan `...-1-...`。
- R2.2 全局视图覆盖——见 plan `...-2-...`。
- R2.6 manufacturing owner-doc drift（P1-MA3-040~045）——后续 MR2 工作项，不同组件。
- **任何应用代码 / ORM / 常量变更**——本计划纯文档。code 侧已由 MR1（R1.8~R1.13, R1.9, R1.16, R1.27 等）闭合或核实。本计划只改 doc 对齐 code，不改 code 对齐 doc（drift 方向 = doc→code 修复）。
- P2 watch-only 项（P2-MA3-023~031）——显式 out-of-scope，处理边界见 §Deferred But Adjudicated。

## Task Route

- Type: `app-layer design change`（finance owner doc 语义对齐，无代码变更）
- Owner Docs: `docs/design/finance/`（state-machine.md / period-close.md / use-cases.md / budget.md / ar-ap-reconciliation.md / bank-reconciliation.md / intercompany-consolidation.md / posting.md / posting-log.md / expense-claim.md / README.md）+ `docs/design/domain-design-guidelines.md`（§16.2 状态码 + config 键）
- Skill Selection Basis: 无匹配技能。可用技能集均针对代码/前端/测试/Git/PPT，不覆盖文档编辑。本计划为 finance 文档对齐 code，Skill: none。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 纯文档变更。

## Execution Plan

### Phase 1 — R2.3 finance 状态机/dict/字段语义 drift（P1-MA3-024 / 026 / 027 / 028 / 029）

Status: planned
Targets: `docs/design/finance/state-machine.md`、`docs/design/finance/period-close.md`、`docs/design/finance/use-cases.md`、`docs/design/finance/budget.md`、`docs/design/finance/ar-ap-reconciliation.md`、`docs/design/finance/bank-reconciliation.md`、`docs/design/finance/intercompany-consolidation.md`
Skill: none

- Item Types: `Fix | Proof`
- Prereqs: 无（可与 R2.1 顺序执行避免 finance 文档并发编辑）

- [ ] [Proof] grep ORM `app-erp-finance.orm.xml` 确认 5 个 dict 的权威值集（accounting-period-status / postingType[如有 dict] / ar-ap-status / voucher-status / 合并抵消实体名），作为文档对齐基准
      - Skill: none
- [ ] [Fix] P1-MA3-024：统一期间状态机三文档为 code 5 态（DRAFT/OPEN/CLOSED/NEVER_OPENED/CLOSED_FINAL），CLOSED=已结账；state-machine.md + period-close.md + use-cases.md 对齐；NEVER_OPENED→OPEN 迁移补充（与 P1-MA2-033 MR1 侧一致）
      - Skill: none
- [ ] [Fix] P1-MA3-026：postingType 单一真相源——doc 对齐 ORM 7 字符串值（NORMAL/OPENING_BALANCE/ADJUSTMENT/CLOSING/REVERSAL/BUDGET/COMMITMENT）；移除 budget.md:96 的数值码 ACTUAL=10/RESERVATION=40；补 OPENING_BALANCE/ADJUSTMENT/CLOSING 文档化
      - Skill: none
- [ ] [Fix] P1-MA3-027：ar-ap-status doc 对齐 dict 实际命名（OPEN/PARTIAL/SETTLED/CANCELLED/WRITTEN_OFF）；移除 RECONCILED（实际 SETTLED）+ OVER（不存在，code 以错误码拒绝）
      - Skill: none
- [ ] [Fix] P1-MA3-028：bank-stmt-status 文档统一为复用 voucher-status（DRAFT/POSTED/CANCELLED），删除 bank-reconciliation.md:27,89 的独立 dict 声明（对齐 :145 撤回 + ORM 无此 dict + code 用 VOUCHER_STATUS_*）
      - Skill: none
- [ ] [Fix] P1-MA3-029：合并抵消实体命名 doc 对齐 code 2 实体（ErpFinConsolidationElimination/ErpFinIntercompanyMatch），移除 5 不存在名
      - Skill: none
- [ ] [Proof] grep 确认 5 个 dict/状态机在 finance doc 中值集与 ORM 一致（ACTUAL/RESERVATION/RECONCILED/OVER/bank-stmt-status dict/5 幻影实体名 命中数 = 0）
      - Skill: none

Exit Criteria:

- [ ] 5 项 dict/状态机/字段语义 drift 对齐 ORM/code，grep 证据记录

### Phase 2 — R2.4 finance 过账/事务/承付/多币种 drift（P1-MA3-025 / 030 / 031 / 032 / 039，文档侧）

Status: planned
Targets: `docs/design/finance/posting.md`、`docs/design/finance/posting-log.md`（REQUIRES_NEW 事务边界 + 过账异常悬挂语义与 P1-MA3-030 reverse 传播描述联动）、`docs/design/finance/budget.md`、`docs/design/finance/period-close.md`、`docs/design/finance/use-cases.md`
Skill: none

- Item Types: `Fix | Proof`
- Prereqs: Phase 1（budget.md 在 Phase 1 已修 postingType，本阶段修公式段）

- [ ] [Proof] grep code 确认 5 个 drift 点的实际行为作为文档对齐基准：(a) `ErpFinBudgetControlBiz.java` actualBalance 是否含 COMMITMENT——**R1.9 已核实（roadmap R1.9 done）**，执行者应读取 R1.9 owner doc 结论作为权威，本 Proof 仅复核确认；(b) `ErpFinVoucherBizModel.java:77-79` reverse() 传播；(c) `CommitmentAcctDocProvider.java:33-44` 返回值；(d) `ErpFinConstants.java:114` + `ErpFinAccountingPeriodProcessor.java:681-684` auto-post-on-close 默认值；(e) `ErpFinPostingProcessor.java:816-819` amountSource 行为
      - Skill: none
- [ ] [Fix] P1-MA3-025 MR2 侧：budget.md:59 + use-cases.md:212,253 公式更新为 R1.9 已核实的 code 实际语义（R1.9 裁决 actualBalance 是否含 COMMITMENT——按其结论对齐：若含则三项式措辞保留但澄清 actualBalance 通道含 COMMITMENT；若不含则改二项式 + 标注 commitment 释放独立路径）。执行者以 R1.9 owner doc 结论为准，不重新裁决
      - Skill: none
- [ ] [Fix] P1-MA3-030：posting.md:399 更新为反映 code 实际——reverse() 叠加 REQUIRES_NEW（与 post() 一致），红冲失败不回滚调用方主事务；补充跨域调用方（11 域 PostingExecutor/Dispatcher）的事务边界注意事项
      - Skill: none
- [ ] [Fix] P1-MA3-031：统一 CommitmentAcctDocProvider 描述——budget.md:264-267 对齐 posting.md:541-543 + code：getSupportedBusinessTypes 返回 emptySet + createFacts 返回 emptyList + 承付凭证由 CommitmentVoucherGenerator 直接写入
      - Skill: none
- [ ] [Fix] P1-MA3-032：period-close.md:285 auto-post-on-close 默认值对齐 code false
      - Skill: none
- [ ] [Fix] P1-MA3-039 MR2 侧：posting.md:481-484 多币种凭证折算路径文档化——反映 code persistVoucher amountSource=amountFunctional 实际行为（当前两者相等无币种折算）；标注多币种源币金额的 successor（R1.9 代码侧已核实）
      - Skill: none
- [ ] [Proof] grep 确认 5 个 drift 点 doc 描述与 code 一致（reverse REQUIRES_NEW 描述匹配 / Provider 描述统一 / 默认值 false / 公式与 code javadoc 一致）
      - Skill: none

Exit Criteria:

- [ ] 5 项过账/事务/承付/多币种 drift 文档对齐 code，grep 证据记录

### Phase 3 — R2.5 finance 配置键/门控 drift（P1-MA3-033 / 034 / 035 / 036 / 037 / 038）

Status: planned
Targets: `docs/design/finance/period-close.md`、`docs/design/domain-design-guidelines.md`、`docs/design/finance/multiple-accounting-schemas.md`、`docs/design/finance/intercompany-consolidation.md`、`docs/design/finance/state-machine.md`、`docs/design/finance/expense-claim.md`、`docs/design/finance/ar-ap-reconciliation.md`
Skill: none

- Item Types: `Fix | Proof`
- Prereqs: Phase 1（state-machine.md 在 Phase 1 已修状态，本阶段修 reverse-close 门控描述）

- [ ] [Proof] grep `ErpFinConstants.java` 全量 config key 清单作为文档对齐基准（~120 key）
      - Skill: none
- [ ] [Fix] P1-MA3-033：doc config 表键名 auto-depreciation → auto-depreciation-on-close（period-close.md:287 + domain-design-guidelines.md:662）
      - Skill: none
- [ ] [Fix] P1-MA3-034：multiple-accounting-schemas.md:251-256 config 表更新为 code 实际——保留 multi-schema-enabled；default-schema → default-schema-nature 对齐键名；移除幻影键 schema-inheritance + auto-create-all-schemas（grep 零 code 引用）
      - Skill: none
- [ ] [Fix] P1-MA3-035：intercompany-consolidation.md:147-150 config 表更新为 code 实际 3 键（intercompany-posting-enabled / consolidation-elimination-enabled / elimination-inventory-profit-enabled）；移除 4 幻影键
      - Skill: none
- [ ] [Fix] P1-MA3-036：state-machine.md:152-153,185-186 + period-close.md:165,287 reverse-close-approval-required 描述改为 code 实际（默认 true 时直接 throw，纯 kill-switch，无审批 action）+ 标注 P1-MA2-020 successor（审批流落地触发条件）
      - Skill: none
- [ ] [Fix] P1-MA3-037：expense-claim.md:186 expense-budget-check-enabled 默认值对齐 code false；移除幻影键 imprest-topup-threshold
      - Skill: none
- [ ] [Fix] P1-MA3-038：ar-ap-reconciliation.md:122-127 自动核销规则 doc 4 键更新为 code 实际 1 策略枚举 auto-recon-strategy（FIFO/BY_AMOUNT/BY_RATIO）
      - Skill: none
- [ ] [Proof] grep 确认 finance doc 中 config 键名与 `ErpFinConstants.java` 一致（幻影键 schema-inheritance/auto-create-all-schemas/consolidation-currency/consolidation-method/intercompany-tolerance/consolidation-schedule/imprest-topup-threshold/auto-match-* 命中数 = 0；code 实际键在 doc 有文档化）
      - Skill: none

Exit Criteria:

- [ ] 6 项 config 键/门控 drift 文档对齐 code，grep 证据记录

## Draft Review Record

- Independent draft review iteration 1: needs-revision (ses_04c3410f2ffe) — 1 blocking：Phase 3 Targets 列不存在的 `docs/design/finance/domain-design-guidelines.md`（应为顶层 `docs/design/domain-design-guidelines.md`），违反 Rule 1。+ 3 项 non-blocking（P1-MA3-025 引用 R1.9 已核实结论 / posting-log.md Targets 缘由澄清 / Non-Goals P2 措辞对齐 Deferred 段）
- Independent draft review iteration 2: accept (ses_04c2f461fffe) — Phase 3 Targets 路径修正为顶层；N1-N3 全部落地；无新问题。计划可执行。

## Closure Gates

> 本计划纯文档变更，无代码/ORM/契约变更。删除 typecheck/build/test 验证门控。以 doc↔code 一致性 grep 证据替代。

- [ ] 范围内行为完成（16 findings 文档侧全部落地：P1-MA3-024~039）
- [ ] 相关文档对齐（~11 份 finance owner doc + domain-design-guidelines config 段）
- [ ] 文档一致性已验证（每个 drift 点 grep 确认 doc↔code 一致 + 幻影键/dict 死值清零）；无代码变更故无 typecheck/build/test
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中
- [ ] arm-index 中 P1-MA3-024~039 状态回填为已修复（文档侧）

## Deferred But Adjudicated

### finance P2 watch-only 项（P2-MA3-023~031）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 9 项 P2 为 finance 文档卫生/边界 watch-only（reverseClose 路径 3-step vs 1-step / 银行对账恒等式简化 / 坏账争议发票 config-gated / VoucherReversedEvent billType / GL 映射试点清单矛盾 / 红字凭证两种约定 / 承付 release-on-receive guard / ErpFinConfigs 空壳 / CLOSED→CLOSED_FINAL 独立步骤）。本计划触及文件时若顺手清理零额外风险则处理。
- Successor Required: `no`（独立 watch-only）

## Closure

Status Note: _待执行与结束审计后填写_

Closure Audit Evidence:

- Auditor / Agent: _待独立结束审计_
- Evidence: _待填写_
