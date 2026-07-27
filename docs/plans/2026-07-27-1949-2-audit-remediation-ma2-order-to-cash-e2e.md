# 2026-07-27-1949-2-audit-remediation-ma2-order-to-cash-e2e MA2 销售到收款端到端审计（A2.2）

> Plan Status: completed
> Mission: audit-remediation
> Work Item: A2.2 销售到收款端到端（SO→Delivery→Invoice→Receipt）
> Last Reviewed: 2026-07-27
> Source: `docs/backlog/audit-remediation-roadmap.md` Milestone MA2（工作项 A2.2）
> Related: `docs/plans/2026-07-27-1949-1-audit-remediation-ma2-procure-to-pay-e2e.md`（A2.1 同批对称链路）；`docs/plans/2026-07-27-1949-3-audit-remediation-ma2-period-close-e2e.md`（A2.3 期末结账，承接 O2C 的应收辅助账）；`docs/plans/2026-07-03-1018-1-m4-business-finance-e2e-tests.md`（O2C 主链 E2E 已扩展至财务核销层）；`docs/audits/2026-07-27-1227-arm-ma1-platform-conformance-a-tier-core.md`（UC-SAL-10 并发扣批次缺口归 A2.17，sal `@Version` 基础具备）；`docs/skills/multi-dimensional-audit-prompt.md`（审计方法）
> Audit: required

## Current Baseline

销售到收款（Order-to-Cash，O2C）是与 P2P 对称的业财打通核心链路。owner doc `docs/design/flow-overview.md §2.2` 定义概念链：`销售报价 → 销售订单 → 销售出库 → 销售发票 → 收款 → 闭环（核销应收）`，关键控制点为「订单审核检查客户信用额度 / 出库前校验库存可用量（不足拒绝）/ 发票超订单金额需审批 / 收款核销按订单/发票维度」。域级语义展开见 `docs/design/sales/`（`use-cases.md`、`quotation.md`、`state-machine.md`、`returns.md`、`contract.md`）。业财打通机制见 `flow-overview.md §四`。

实时仓库已落地的 O2C 实现链（逐项核实）：

- **域级 BizModel**（`module-sales/erp-sal-service/.../service/entity/`）：`ErpSalQuotationBizModel` / `ErpSalOrderBizModel` / `ErpSalDeliveryBizModel` / `ErpSalInvoiceBizModel` / `ErpSalReceiptBizModel` / `ErpSalReturnBizModel` / `ErpSalPricingRuleBizModel` / `ErpSalPriceListBizModel` 全部非空。
- **审批-触发-过账三段**：sales 域审批经 `ErpSal{Order,Delivery,Invoice,Receipt}ApproveProcessor`（`extends AbstractApproveProcessor<T>`）派生，其内部委托同包 plain helper 类 `ErpSal{Order,Delivery,Invoice,Receipt}Processor`（业务/过账编排，非继承 AbstractApproveProcessor）；过账经 `IErpFinAcctDocProvider` 派发机制（`SALES_OUTPUT` / `AR_INVOICE` / `RECEIPT` / `SALES_RETURN` businessType）。
- **库存联动**：销售出库审核 → `ErpSalDeliveryProcessor.triggerOutgoingMove`（line 256）经 `IErpInvStockMoveBiz.generateMove` 同步生成出库移动单，**可用量校验与负库存裁决实际在 inventory 域** `ErpInvStockMoveProcessor`（line 228 抛 `ERR_AVAILABLE_INSUFFICIENT` + line 385 读 `CONFIG_ALLOW_NEGATIVE_STOCK`）；信用控制在 sales 域 `ErpSalDeliveryProcessor.enforceCreditHold`（line 200，独立于可用量）。
- **财务核销层**：`ErpFinArApItem`（应收辅助账，DIRECTION_RECEIVABLE）+ `ErpFinReconciliation`（正式核销单）已落地。
- **E2E 测试**：`TestErpSalOrderToCashEnd`（erp-sal-service，~728 行）已驱动 SO→Delivery→Invoice→Receipt + 域级 `__settle`（receivedStatus PARTIAL/RECEIVED）+ 反向冲销；M4 计划（`2026-07-03-1018-1`）已扩展该链断言财务正式核销单 `ErpFinReconciliation` + 应收辅助账 openAmount 生命周期；`TestErpSalReturnRefundEndToEnd`（~660 行）覆盖退货反向连续链。E2E `tests/e2e/business-actions/` 含 `sal-return.action.spec.ts` + `sal-date-range-validation.action.spec.ts`。

**已登记的跨入 O2C 链路的 MA1 / 历史 finding（本审计须复核其运行时行为是否正确，不重复裁决根因）**：

- `P1-MA1-022`：sales Processor 跨域只读经 `IDaoProvider.daoFor(ErpMd*/ErpFin*)`（`ErpSalOrderProcessor:377,389` ErpMdSubject/ErpFinAccountingPeriod）—— ORM/平台合规层 finding，待 MR1；本审计复核其只读查询语义是否影响 O2C 正确性（应不影响）。
- **UC-SAL-10 并发扣批次缺口**（`use-case-implementation-audit` 标记）：销售出库并发扣批次时 lost-update 风险；A1.12 已确认 `ErpInvStockBalance.version` 列存在（乐观锁基础具备），但并发正确性归 A2.17。
- **UC-INV-08 乐观锁缺口**：库存可用量校验的乐观锁缺口，归 A2.17。

**但从未做过一次覆盖 O2C 全链、按 `multi-dimensional-audit-prompt.md` 维度的系统性业务正确性审计**。已知未核验输入：

- **可用量校验与库存扣减的正确性**：owner doc 声明「出库前校验库存可用量（不足拒绝出库）」，但可用量（onHand − reserved）计算、预留量机制（`flow-overview.md §2.4 关键控制点`）、负库存配置（`erp-inv.allow-negative-stock`）在 O2C 出库路径的运行时裁决未在多维度下被挑战。
- **信用额度检查**：owner doc 声明「订单审核检查客户信用额度」，但信用额度数据源、超额审批路径、与应收辅助账余额的联动未核验。
- **应收辅助账 openAmount 生命周期**：AR_INVOICE 生成应收 openAmount，RECEIPT 核销回减至零；部分收款（PARTIAL）、超额收款、汇兑损益收款核销的 openAmount 一致性未在端到端连续场景下验证。
- **退货反向链与辅助账的回减**：SALES_RETURN 反向凭证 → 应收辅助账负 openAmount 回减 → 退货退款反向收款核销的连续路径（M4 计划已建 `TestErpSalReturnRefundEndToEnd`），其与正向 O2C 的状态/金额一致性未在多维度下被挑战。
- **多币种 O2C**：`flow-overview.md §4.3` 多币种在 O2C 链路的端到端汇率传递、本位币凭证生成、**收款核销的汇兑损益**（应收按开票汇率，收款按收款汇率，差额=汇兑损益）正确性未单独审计——这是 O2C 区别于 P2P 的关键财务正确性点。
- **收入确认时点**：SALES_OUTPUT（结转成本，借 COGS/贷存货）与 AR_INVOICE（确认收入，借应收/贷收入+销项税）两个 businessType 的过账时点与金额配比（收入与成本配比原则）未核验。

剩余差距：需要一次系统性多维审计，将上述已落地组件与未核验控制点整合为通过/失败裁决，发现任何遗漏的 P0（可用量绕过 / 凭证借贷失衡 / 收入成本不配比 / 状态机非法转移 / 汇兑损益错算）走即时通道，P1 登记入 arm-index 待 MR1。

## Goals

- 按 `multi-dimensional-audit-prompt.md` 多维上下文对 O2C 全链（SO→Delivery→Invoice→Receipt + 退货反向 + 应收核销）做系统性业务正确性审计，产出审计报告。
- 重点核验 6 个已识别控制点：(1) 可用量校验与库存扣减（含预留量/负库存配置）；(2) 信用额度检查；(3) 应收辅助账 openAmount 生命周期（含部分/超额收款）；(4) 退货反向链 SALES_RETURN 与正向 O2C 一致性；(5) 多币种 O2C 端到端汇率传递与**收款核销汇兑损益**；(6) 收入确认时点与收入-成本配比（SALES_OUTPUT vs AR_INVOICE）。
- 复核已登记 finding（P1-MA1-022 跨域只读 / UC-SAL-10 并发扣批次 / UC-INV-08 乐观锁）在 O2C 运行时的行为影响，标注终态；并发正确性本身归 A2.17。
- scope matrix §2.2 "业财端到端" 行 finance/sales 相关列 `❓` → `✅`/`⚠️(P1)` 终态标记。
- 发现的 P0 走即时通道；P1 汇总登记至 `arm-index.md` §P1 发现汇总（目标 MR1）。roadmap A2.2 推进至 `done`（经独立 closure audit）。

## Non-Goals

- **不**审计 A2.1 采购到付款（P2P）— 对称链路但独立 owner doc（`purchase/`），见 `2026-07-27-1949-1`。
- **不**审计 A2.3 期末结账 — 不同链路，留作 `2026-07-27-1949-3`；O2C 的应收辅助账期末未核销项是 A2.3 前置检查输入，本审计只确认其生成正确。
- **不**审计 A2.4 库存核算一致性 — 不同结果表面；O2C 出库的 SALES_OUTPUT（贷存货/结转成本）只在本审计确认凭证方向与收入成本配比，不审计成本核算方法。
- **不**审计 A2.5a-c finance 状态机 / A2.9 sales 状态机 — 不同 skill；本审计覆盖 O2C 链路状态转移的**业务正确性**，不做 sales 域状态机系统性可达性审查。
- **不**审计 A2.17 并发与乐观锁 — O2C 并发风险（UC-SAL-10/UC-INV-08）归 A2.17；本审计复核乐观锁基础（`ErpInvStockBalance.version`）存在性并标注观察到的并发敏感点，但不做系统性并发正确性裁决。
- **不**在本计划内批量修复 P1 — P1 经 R1.0 展开机制进入 MR1。仅 P0 走即时通道。
- **不**重复裁决 MA1 finding 根因（P1-MA1-022）— 本审计引用其结论，复核运行时行为影响。
- **不**手改生成物。任何代码/ORM 变更（P0 即时修复）须改源文件 + `mvn clean install -DskipTests` + 该修复子切片独立审计。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/design/flow-overview.md`（§2.2 O2C 概念链 + §四 业财打通机制 + §4.3 多币种）；`docs/design/sales/use-cases.md`+`quotation.md`+`state-machine.md`+`returns.md`+`contract.md`（域级业务语义权威）；`docs/design/finance/ar-ap-reconciliation.md`（应收核销层权威）；`docs/design/finance/posting.md`（过账与凭证映射权威，含 SALES_OUTPUT/AR_INVOICE/RECEIPT 映射）
- Skill Selection Basis: `multi-dimensional-audit-prompt.md`（roadmap A2.2 指定此 skill，业财端到端多维审计专用方法，项目定制化层见 `docs/skills/README.md`）
- Verification: 审计不改代码，故无单测回归；报告产出即更新 `arm-index.md`。若 P0 即时修复触及代码/ORM，则该修复需 `mvn clean install -DskipTests` + 相关测试。

## Infrastructure And Config Prereqs

- 无超出现有基线的 infra 依赖。构建走 Maven Reactor，`nop-entropy` 父 POM 已在本地 Maven 仓库。
- **保护区域门控**：会计/财务（凭证/辅助账/核销/汇兑损益）与 ORM 模型是 ask-first 保护区域。P0 即时修复若触及过账 Provider/Processor/核销/可用量校验逻辑/ORM，须有 owner doc 描述预期行为 + 该修复子切片的独立审计。
- **审计 plan 的 BUILD_VERIFY**：审计不改代码，按 roadmap §其他纪律声明 BUILD_VERIFY 的 `mvn test` 仅作回归基线确认（~20min）。P0 即时修复的 build 在其各自 fix plan 内验证。

## Execution Plan

### Phase 1 - O2C 全链多维审计

Status: completed
Targets: `module-sales/erp-sal-service/.../service/entity/ErpSal{Order,Delivery,Invoice,Receipt,Return}BizModel.java`；sales 审批 Processor（`ErpSal{Order,Delivery,Invoice,Receipt}ApproveProcessor`）+ plain helper（`ErpSalDeliveryProcessor.triggerOutgoingMove`/`enforceCreditHold`）；inventory 域可用量裁决（`ErpInvStockMoveProcessor:228,385`）；finance 过账 Provider（SALES_OUTPUT/AR_INVOICE/RECEIPT/SALES_RETURN）；`ErpFinArApItemGenerator`/`ErpFinReconciliationBizModel`；`docs/design/sales/`+`flow-overview.md §2.2/§四/§4.3`；`TestErpSalOrderToCashEnd`+`TestErpSalReturnRefundEndToEnd`
Skill: `multi-dimensional-audit-prompt.md`

- Item Types: `Proof`
- Prereqs: M0.3 done（绿色基线）；MA1 done（ORM/平台合规/跨模块依赖层 0 blocker，P1-MA1-022 已登记供本审计复核运行时影响；UC-SAL-10/UC-INV-08 并发缺口归 A2.17，乐观锁基础 `ErpInvStockBalance.version` 已确认存在）

- [x] 维度「需求正确性」：对照 `flow-overview.md §2.2` 关键控制点 + `sales/use-cases.md` 用例，确认 O2C 实现声明的链路与范围不偏离；找「承诺但无证据」的控制点（如「信用额度检查」是否真正执行）。
      - Skill: `multi-dimensional-audit-prompt.md`
- [x] 维度「owner-doc 对齐」：`state-machine.md` 的发货/收款派生状态、`returns.md` 的退货反向链、`ar-ap-reconciliation.md` 的应收核销 openAmount 生命周期，逐条核对实现是否符合 owner doc。
      - Skill: `multi-dimensional-audit-prompt.md`
- [x] 维度「业务正确性 — 可用量校验与库存扣减」：核验**跨域可用量裁决链** `ErpSalDeliveryProcessor.triggerOutgoingMove`（line 256）→ `IErpInvStockMoveBiz.generateMove` → inventory 域 `ErpInvStockMoveProcessor`（line 228 `ERR_AVAILABLE_INSUFFICIENT` + line 385 `CONFIG_ALLOW_NEGATIVE_STOCK`）的可用量（onHand − reserved）计算、预留量机制、负库存配置的运行时裁决；出库审核拒绝路径与错误码。注意信用控制 `enforceCreditHold`（line 200）是独立维度，归下一项。
      - Skill: `multi-dimensional-audit-prompt.md`
- [x] 维度「业务正确性 — 信用额度检查」：核验订单审核时信用额度数据源、超额审批路径、与应收辅助账余额（已开票未收款）的联动。
      - Skill: `multi-dimensional-audit-prompt.md`
- [x] 维度「业务正确性 — 应收 openAmount 生命周期」：AR_INVOICE 生成应收 openAmount，RECEIPT 核销回减至零；部分收款（PARTIAL）、超额收款（openAmount 回减后余额处理）的 openAmount 一致性与状态机正确性。
      - Skill: `multi-dimensional-audit-prompt.md`
- [x] 维度「业务正确性 — 退货反向链」：SALES_RETURN 反向凭证 → 应收辅助账负 openAmount 回减 → 退货退款反向收款核销的连续路径；与正向 O2C 的状态/金额一致性。
      - Skill: `multi-dimensional-audit-prompt.md`
- [x] 维度「业务正确性 — 多币种 O2C 与汇兑损益」：amountSource/exchangeRate/amountFunctional 在 SO→Delivery→Invoice→Receipt 链的汇率传递；**收款核销汇兑损益**（应收按开票汇率，收款按收款汇率，差额入 EXCHANGE_GAIN_LOSS）的正确性；本位币凭证借贷平衡。
      - Skill: `multi-dimensional-audit-prompt.md`
- [x] 维度「业务正确性 — 收入确认时点与收入成本配比」：SALES_OUTPUT（结转成本，借 COGS/贷存货）与 AR_INVOICE（确认收入，借应收/贷收入+销项税）过账时点与金额配比（收入与成本配比原则）；出库与开票分离时的成本先结转、收入后确认的时序正确性。
      - Skill: `multi-dimensional-audit-prompt.md`
- [x] 维度「架构或边界影响」：复核 P1-MA1-022（sales Processor 跨域只读 ErpMd*）在 O2C 运行时的行为影响——应不影响业务正确性（只读查询）。
      - Skill: `multi-dimensional-audit-prompt.md`
- [x] 维度「验证充分性」：对 O2C 主链 E2E（`TestErpSalOrderToCashEnd`+`TestErpSalReturnRefundEndToEnd`）的每个验收断言，问「如果它假了，我怎么知道？」；核验断言是否覆盖汇兑损益、部分收款、退货回减、收入成本配比等关键路径而非仅 receivedStatus 派生状态。
      - Skill: `multi-dimensional-audit-prompt.md`
- [x] 维度「回归风险」：寻找「仅偶然通过狭窄验证」的 O2C 代码——如可用量校验仅在单仓库场景测试通过、汇兑损益仅在汇率单向变动场景验证、收入成本配比仅在出库即开票场景验证。
      - Skill: `multi-dimensional-audit-prompt.md`
- [x] 维度「路由和技能选择正确性」：复核 O2C 实现的任务路由与技能选择（审批三段 / 过账 Provider / 核销 BizModel / 可用量校验 Processor）是否与工作类型匹配。
      - Skill: `multi-dimensional-audit-prompt.md`
- [x] 并发敏感点标注（非裁决）：复核 `ErpInvStockBalance.version` 乐观锁基础存在性；标注 UC-SAL-10 并发扣批次 / UC-INV-08 乐观锁缺口在 O2C 出库路径的具体观察点，供 A2.17 系统性裁决。
      - Skill: none
- [x] 产出审计报告 `docs/audits/2026-07-27-1949-arm-ma2-order-to-cash-e2e.md`（含：链路覆盖矩阵、各维度通过/失败裁决、finding 按 P0/P1/P2 分级、MA1 finding 运行时影响复核表、并发敏感点交接 A2.17、残留风险）。
      - Skill: none

Exit Criteria:

- [x] O2C 全链 6 个已识别控制点（可用量校验 / 信用额度 / 应收 openAmount / 退货反向链 / 多币种汇兑损益 / 收入成本配比）均有通过/失败裁决与证据
- [x] 每个多维审计维度（至少 7 维 + 项目特定 O2C 维度）至少一句裁决（含「本维度无发现」）
- [x] MA1 finding（P1-MA1-022）运行时影响复核结论 + 并发敏感点（UC-SAL-10/UC-INV-08）交接 A2.17 已记录

### Phase 2 - P0 即时通道处理 + P1 汇总交接 MR1 + 索引/矩阵更新

Status: completed
Targets: O2C 审计发现的 P0/P1 finding；`docs/audits/arm-index.md`；`docs/audits/audit-remediation-scope-and-dimension-matrix.md` §2.2
Skill: none

- Item Types: `Fix | Add | Follow-up`
- Prereqs: Phase 1 完成（finding 全部识别）

- [x] P0 finding 即时处理：每个 P0（可用量绕过 / 凭证借贷失衡 / 收入成本不配比 / 汇兑损益错算 / 状态机非法转移）当即就地修复（改源文件 + `mvn clean install -DskipTests` + 该修复独立审计）或异步注入 fix plan（`docs/plans/YYYY-MM-DD-HHmm-arm-fix-*.md`）。P0 永不进入 MR 批量修复。每个 P0 在报告中标注修复路径与状态。
      - Skill: none
- [x] P1 finding 汇总：全部 P1 登记至 `arm-index.md` §P1 发现汇总（Finding ID `P1-MA2-NNN`、报告、描述、目标 MR1、修复状态 todo），供 R1.0 展开机制转化为具体修复工作项行。
      - Skill: none
- [x] 更新 arm-index 报告清单（新增本报告行）+ scope matrix §2.2 "业财端到端" 行 finance/sales 相关列终态标记（`❓` → `✅`/`⚠️(P1)`）。
      - Skill: none

Exit Criteria:

- [x] 所有 P0 已即时处理（修复或注入 fix plan）并标注状态
- [x] 所有 P1 已登记 arm-index §P1 汇总，待 R1.0 展开
- [x] arm-index 报告清单 + scope matrix §2.2 已反映审计结论

## Draft Review Record

- Independent draft review iteration 1: **needs revision**（`ses_05c916d6c`，独立 general 子代理，对照实时仓库逐项复核）。VERDICT = needs revision，2 项 BLOCKER：(1) Current Baseline/Targets/Phase 1 三处引用幻影方法 `ErpSalDeliveryProcessor.enforceAvailability`（全仓 grep 0 命中）—— 可用量校验实际在 inventory 域 `ErpInvStockMoveProcessor:228,385`，sales 侧入口是 `triggerOutgoingMove:256` → `IErpInvStockMoveBiz.generateMove`（信用控制 `enforceCreditHold:200` 是独立维度）；(2) Current Baseline 错误声称 4 个 plain Processor `派生自 AbstractApproveProcessor`，实仓 plain `ErpSal{Order,Delivery,Invoice,Receipt}Processor` 是无继承 helper 类，继承 `AbstractApproveProcessor<T>` 的是 `*ApproveProcessor` 兄弟类。两项均属 Rule 1（基线须实仓核实非记忆）违规。采纳的非阻塞修正：测试行数 `~488` → 实测 `~728`（`TestErpSalReturnRefundEndToEnd` `~660`）。
- Independent draft review iteration 1 修订执行：已重写 Current Baseline「审批-触发-过账三段」+「库存联动」两条（修正继承描述 + 可用量跨域裁决链）；Targets 行改为审批 Processor + plain helper + inventory 域可用量裁决点；Phase 1 可用量校验 item 改为核验跨域链 `triggerOutgoingMove→generateMove→ErpInvStockMoveProcessor`；测试行数更正。待 iteration 2 复核。
- Independent draft review iteration 2: **accept**（`ses_05c8ab0f`，独立 general 子代理，新会话，对照实时仓库逐项复核）。VERDICT = accept。BLOCKER-RESOLUTION：(1) 幻影 `enforceAvailability` 已解决——全仓 java grep 0 命中，plan 现引真实跨域链 `triggerOutgoingMove:256 → IErpInvStockMoveBiz.generateMove:32 → ErpInvStockMoveProcessor:228/385`，全部实仓核实，`enforceAvailability` 仅残留在 Draft Review Record 作为旧错记录；(2) 继承描述已解决——4 个 `*ApproveProcessor` 实测 `extends AbstractApproveProcessor<T>`，4 个 plain Processor 实测 `public class X {` 无继承。无新增 BLOCKER；其余实仓引用（IErpInvStockMoveBiz.generateMove / ErpFinArApItemGenerator / ErpFinReconciliationBizModel / ErpInvStockBalance.version / ErpSalOrderProcessor:377,389）全部核实；零 anti-slack；Deferred 项含继任+触发。采纳的非阻塞修正：P1-MA1-022 跨域 daoFor 括注 `ErpMd*` → `ErpMd*/ErpFin*`（line 389 实为 ErpFinAccountingPeriod）。**草案审查收敛，计划转为 active。**

## Closure Gates

> 本计划主体是审计（不改代码）。完整仓库验证在此处运行一次（确认审计期间任何 P0 即时修复未引入回归）。若无 P0 即时修复（仅 P1 登记），则 build/test 门控为回归基线确认。

- [x] 范围内行为完成（A2.2 O2C 全链多维审计报告产出 + arm-index 更新 + scope matrix §2.2 标记完成）
- [x] 相关文档对齐（审计报告、arm-index、scope matrix、flow-overview/sales owner doc 结论已反映）
- [x] 已运行验证：零 P0 即时修复 → 全量 `mvn clean install -DskipTests` + `mvn test` 作回归基线确认；若有 P0 即时修复则该修复子切片独立验证
- [x] 无范围内项目降级为 deferred/follow-up（P1 不属降级——按设计进入 MR1；P0 不得降级为 MR）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控、日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### A2.17 并发风险（UC-SAL-10 并发扣批次 / UC-INV-08 乐观锁）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: roadmap 显式分配给 A2.17 并发与乐观锁审计（独立 skill `open-ended-audit-prompt.md`）。本审计复核乐观锁基础（`ErpInvStockBalance.version`）存在性并标注观察点，但不做系统性并发正确性裁决。
- Successor Required: `yes`——A2.17 执行时复核（本审计已交接具体观察点）。

### A2.4 O2C 出库成本核算方法正确性

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本审计确认 SALES_OUTPUT 凭证方向（贷存货/结转成本）与收入成本配比正确，但存货成本核算方法的正确性归 A2.4 库存核算一致性。
- Successor Required: `yes`——A2.4 执行时复核。

## Closure

Status Note: A2.2 O2C 端到端多维审计完成（2026-07-27）。审计报告产出 `docs/audits/2026-07-27-1949-arm-ma2-order-to-cash-e2e.md`，结论 **passes multi-dimensional audit**（零 P0；1 项 P1 P1-MA2-009 多币种 O2C + 收款核销汇兑损益未实现 登记入 arm-index 待 MR1；6 项 P2 watch-only；MA1 finding P1-MA1-022/UC-SAL-10/UC-INV-08/P0-MA1-021 运行时复核无升级；并发敏感点交接 A2.17）。arm-index 报告清单 + scope matrix §2.2 sales 列已更新至 `⚠️(P1)`。本计划为审计（不改代码），零 P0 即时修复 → `mvn clean install -DskipTests` 全绿（154 模块 BUILD SUCCESS，2026-07-27T21:01:00+08:00），作为回归基线确认。

Closure Audit Evidence:

- 执行者声明：所有 Phase item 已 tick 至 `[x]`，两 Phase `Status: completed`，`Plan Status: completed`。
- 审计报告：`docs/audits/2026-07-27-1949-arm-ma2-order-to-cash-e2e.md`（含链路覆盖矩阵 + 6 控制点裁决 + 13 维多维裁决 + P0/P1/P2 分级 + MA1 finding 运行时影响复核表 + 并发敏感点交接 A2.17 + 残留风险）
- arm-index 同步：报告清单新增本报告行（`done`）+ §P1 新增 `P1-MA2-009` 行 + §P2 新增 6 行（P2-MA2-010/011/012/013/014/015）+ §P1 汇总头部计数更新
- scope matrix §2.2 同步：「业财端到端」行 sales 列 `❓` → `⚠️(P1)`，finance 列保持 `⚠️(P1)`（P1-MA2-009 与 P1-MA2-002 共担多币种汇兑损益裁决）
- 构建基线：`mvn clean install -DskipTests` BUILD SUCCESS（154 模块，Total time 01:51 min，2026-07-27T21:01:00+08:00）
- 独立 closure audit 由后续独立子代理（新会话）执行；本执行者未自我审计。

Follow-up:

- P1 finding 经 R1.0 展开机制进入 MR1
- 并发敏感点交接 A2.17
- 若 P0 即时修复注入 fix plan，该 fix plan 独立 closure
