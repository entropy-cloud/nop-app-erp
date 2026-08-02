# 2026-07-27-1949-1-audit-remediation-ma2-procure-to-pay-e2e MA2 采购到付款端到端审计（A2.1）

> Plan Status: completed
> Mission: audit-remediation
> Work Item: A2.1 采购到付款端到端（PO→Receive→Invoice→Pay）
> Last Reviewed: 2026-07-27
> Source: `docs/backlog/audit-remediation-roadmap.md` Milestone MA2（工作项 A2.1）
> Related: `docs/plans/2026-07-27-1949-2-audit-remediation-ma2-order-to-cash-e2e.md`（A2.2 同批对称链路）；`docs/plans/2026-07-27-1949-3-audit-remediation-ma2-period-close-e2e.md`（A2.3 期末结账，承接 P2P 的应付辅助账）；`docs/plans/2026-07-03-1018-1-m4-business-finance-e2e-tests.md`（P2P 主链 E2E 已扩展至财务核销层）；`docs/plans/2026-07-27-1430-3-audit-remediation-ma1-architecture-governance-review.md`（P1-MA1-029 ErpCtInvoicePlanBizModel 跨域写 pur 发票行）；`docs/plans/2026-07-27-1430-1-arm-fix-p0-ma1-021-inv-cost-adjust-voucher-writeback.md`（P0-MA1-021 已闭包，inventory 红冲绕过 I*Biz）；`docs/skills/multi-dimensional-audit-prompt.md`（审计方法）
> Audit: required

## Current Baseline

采购到付款（Procure-to-Pay，P2P）是 nop-app-erp 业财打通的核心链路之一。owner doc `docs/design/flow-overview.md §2.1` 定义了概念链：`采购申请 → 采购订单 → 采购入库 → 采购发票 → 付款 → 闭环（核销应付）`，关键控制点为「订单审核锁定价格 / 入库超容差审批 / 发票三单匹配失败拒绝审核 / 付款核销检查发票状态」。域级语义展开见 `docs/design/purchase/`（`use-cases.md`、`three-way-match.md`、`state-machine.md`、`returns.md`）。业财打通机制（L3）见 `flow-overview.md §四`。

实时仓库已落地的 P2P 实现链（逐项核实）：

- **域级 BizModel**（`module-purchase/erp-pur-service/.../service/entity/`）：`ErpPurRequisitionBizModel` / `ErpPurOrderBizModel` / `ErpPurReceiveBizModel` / `ErpPurInvoiceBizModel` / `ErpPurPaymentBizModel` / `ErpPurReturnBizModel` 全部非空。
- **审批-触发-过账三段**：purchase 域审批经 `ErpPur{Order,Receive,Invoice,Payment,Return}ApproveProcessor`（`extends AbstractApproveProcessor<T>`）派生，其内部委托同包 plain helper 类 `ErpPur{Order,Receive,Invoice,Payment,Return}Processor`（业务/过账编排，非继承 AbstractApproveProcessor）；过账经 `IErpFinAcctDocProvider` 派发机制（`PURCHASE_INPUT` / `AP_INVOICE` / `PAYMENT` / `PURCHASE_RETURN` businessType）。
- **库存联动**：采购入库审核 → `IErpInvStockMoveBiz` 同步生成入库移动单（DRAFT→CONFIRMED→DONE）→ 库存流水 + 余额更新（`flow-overview.md §一 L2`）。
- **财务核销层**：`ErpFinArApItem`（应付辅助账，DIRECTION_PAYABLE）+ `ErpFinArApItemGenerator` + `ErpFinReconciliation`（正式核销单）+ `ErpFinReconciliationBizModel` 已落地（`module-finance/erp-fin-service/.../service/entity/`）。
- **三单匹配**：`ThreeWayMatcher`（订单-入库-发票）在发票审核时校验。
- **E2E 测试**：`TestErpPurProcureToPayEnd`（erp-pur-service，~644 行）已驱动 PO→Receive→Invoice→Payment + 域级 `__settle`（paidStatus PARTIAL/PAID）+ 反向冲销；M4 计划（`2026-07-03-1018-1`）已扩展该链断言财务正式核销单 `ErpFinReconciliation` + 应付辅助账 openAmount 生命周期；`TestErpPurReturnRefundEndToEnd`（~632 行）覆盖退货反向连续链。E2E `tests/e2e/business-actions/` 含 `pur-return.action.spec.ts`。

**已登记的跨入 P2P 链路的 MA1 finding（本审计须复核其运行时行为是否正确，不重复裁决根因）**：

- `P1-MA1-022`：purchase Processor/Dispatcher 跨域只读经 `IDaoProvider.daoFor(ErpMd*)`（`ErpPurOrderProcessor:302,314` ErpMdSubject/ErpFinAccountingPeriod + `ErpPurPaymentProcessor:228,240`）—— ORM/平台合规层 finding，待 MR1；本审计复核其只读查询语义是否影响 P2P 正确性（应不影响）。
- `P1-MA1-029`：`ErpCtInvoicePlanBizModel`（contract→pur）跨域写 pur 发票行（绕过 `IErpPurInvoiceBiz` 审批管道）—— 影响 P2P 的「发票经审批管道」契约，本审计须复核该跨域写是否产生未过账/未匹配/状态不一致的发票。

**但从未做过一次覆盖 P2P 全链、按 `multi-dimensional-audit-prompt.md` 维度的系统性业务正确性审计**。已知未核验输入：

- **三单匹配的边界正确性**：owner doc `three-way-match.md` 声明「发票三单匹配失败拒绝审核」，但超容差匹配 / 部分匹配 / 数量容差 vs 金额容差的运行时裁决路径未在多维度下被挑战。
- **暂估应付 vs 正式应付的衔接**：PURCHASE_INPUT 生成暂估应付，AP_INVOICE 生成正式应付，二者在 `ErpFinArApItem` 辅助账的开闭与回冲关系（暂估冲回路径）未在端到端连续场景下验证一致性。
- **退货反向链与辅助账的回减**：PURCHASE_RETURN 红字过账 → 应付辅助账负 openAmount 回减 → 退款核销的连续路径（M4 计划已建 `TestErpPurReturnRefundEndToEnd`），其与正向 P2P 的状态/金额一致性未在多维度下被挑战。
- **多币种 P2P**：`flow-overview.md §4.3` 多币种（amountSource/exchangeRate/amountFunctional）在 P2P 链路的端到端汇率传递与本位币凭证生成正确性未单独审计。
- **C-11 / flow-overview 事务描述**：`flow-overview.md:499`「单据审核+凭证生成 = 分布式事务（REQUIRES_NEW）」描述与本仓单库 Quarkus 实际（`flow-overview.md §八.2` 已修正为 Facade `REQUIRES_NEW` 跨域失败隔离）的一致性，在 P2P 过账失败回滚路径上的影响。

剩余差距：需要一次系统性多维审计，将上述已落地组件与未核验控制点整合为通过/失败裁决，发现任何遗漏的 P0（数据不一致 / 凭证借贷失衡 / 状态机非法转移 / 跨域写绕过审批管道致脏数据）走即时通道，P1 登记入 arm-index 待 MR1。

## Goals

- 按 `multi-dimensional-audit-prompt.md` 多维上下文对 P2P 全链（PO→Receive→Invoice→Pay + 退货反向 + 应付核销）做系统性业务正确性审计，产出审计报告。
- 重点核验 5 个已识别控制点：(1) 三单匹配边界（超容差/部分匹配/数量 vs 金额容差）；(2) 暂估应付（PURCHASE_INPUT）↔ 正式应付（AP_INVOICE）辅助账衔接与暂估冲回；(3) 付款核销对发票状态的依赖与 openAmount 回减一致性；(4) 退货反向链 PURCHASE_RETURN 红字过账与正向 P2P 的状态/金额一致性；(5) 多币种 P2P 端到端汇率传递与本位币凭证。
- 复核已登记 MA1 finding（P1-MA1-022 跨域只读 / P1-MA1-029 contract 跨域写 pur 发票行）在 P2P 运行时的行为影响，标注终态（不影响业务正确性 / 产生运行时缺陷升级）。
- scope matrix §2.2 "业财端到端" 行 finance/purchase 相关列 `❓` → `✅`/`⚠️(P1)` 终态标记。
- 发现的 P0 走即时通道；P1 汇总登记至 `arm-index.md` §P1 发现汇总（目标 MR1）。roadmap A2.1 推进至 `done`（经独立 closure audit）。

## Non-Goals

- **不**审计 A2.2 销售到收款（O2C）— 对称链路但独立 owner doc（`sales/`）+ 独立结果表面，留作 `2026-07-27-1949-2`。
- **不**审计 A2.3 期末结账 — 不同链路（期间/结转/坏账/成本），留作 `2026-07-27-1949-3`；P2P 的应付辅助账期末未核销项是 A2.3 的前置检查输入，但本审计只确认其生成正确，不审计期末门禁。
- **不**审计 A2.4 库存核算一致性（成本+余额+流水三方对账）— 不同结果表面，留作下一批；P2P 入库的成本入账（PURCHASE_INPUT 借存货）只在本审计确认凭证方向，不审计成本核算方法正确性。
- **不**审计 A2.5a-c finance 状态机 / A2.8 purchase 状态机 — 不同 skill（`state-machine-business-review-prompt.md`）；本审计覆盖 P2P 链路中状态转移的**业务正确性**，但不做 purchase 域状态机的系统性可达性审查。
- **不**审计 A2.17 并发与乐观锁 — P2P 的并发风险（如并发付款核销同一发票）归 A2.17；本审计只标注观察到的并发敏感点。
- **不**在本计划内批量修复 P1 — P1 经 R1.0 展开机制进入 MR1。仅 P0 走即时通道。
- **不**重复裁决 MA1 finding 根因（P1-MA1-022/P1-MA1-029）— 本审计引用其结论，复核运行时行为影响；若复现已闭包 P0-MA1-021（inventory 红冲绕过 I*Biz），标注引用不升级。
- **不**手改生成物（`_gen/`、`_` 前缀、`_app.orm.xml`）。任何代码/ORM 变更（P0 即时修复）须改源文件 + `mvn clean install -DskipTests` + 该修复子切片独立审计。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/design/flow-overview.md`（§2.1 P2P 概念链 + §四 业财打通机制为行为基线）；`docs/design/purchase/use-cases.md`+`three-way-match.md`+`state-machine.md`+`returns.md`（域级业务语义权威）；`docs/design/finance/ar-ap-reconciliation.md`（应付核销层权威）；`docs/design/finance/posting.md`（过账与凭证映射权威）
- Skill Selection Basis: `multi-dimensional-audit-prompt.md`（roadmap A2.1 指定此 skill，业财端到端多维审计专用方法，项目定制化层见 `docs/skills/README.md`）
- Verification: 审计不改代码，故无单测回归；报告产出即更新 `arm-index.md`。若 P0 即时修复触及代码/ORM，则该修复需 `mvn clean install -DskipTests` + 相关测试。

## Infrastructure And Config Prereqs

- 无超出现有基线的 infra 依赖。构建走 Maven Reactor，`nop-entropy` 父 POM 已在本地 Maven 仓库。
- **保护区域门控**：会计/财务（凭证/辅助账/核销）与 ORM 模型是 ask-first 保护区域。P0 即时修复若触及过账 Provider/Processor/核销逻辑/ORM，须有 owner doc 描述预期行为 + 该修复子切片的独立审计。
- **审计 plan 的 BUILD_VERIFY**：审计不改代码，按 roadmap §其他纪律声明 BUILD_VERIFY 的 `mvn test` 仅作回归基线确认（~20min）。P0 即时修复的 build 在其各自 fix plan 内验证。

## Execution Plan

### Phase 1 - P2P 全链多维审计

Status: completed
Targets: `module-purchase/erp-pur-service/.../service/entity/ErpPur{Order,Receive,Invoice,Payment,Return}BizModel.java`；purchase Processor/Dispatcher；finance 过账 Provider（PURCHASE_INPUT/AP_INVOICE/PAYMENT/PURCHASE_RETURN）；`ErpFinArApItemGenerator`/`ErpFinReconciliationBizModel`；`docs/design/purchase/`+`flow-overview.md §2.1/§四`；`TestErpPurProcureToPayEnd`+`TestErpPurReturnRefundEndToEnd`
Skill: `multi-dimensional-audit-prompt.md`

- Item Types: `Proof`
- Prereqs: M0.3 done（绿色基线）；MA1 done（ORM/平台合规/跨模块依赖层 0 blocker，P1-MA1-022/P1-MA1-029 已登记供本审计复核运行时影响）；P0-MA1-021 已闭包（inventory 红冲经 I*Biz）

- [x] 维度「需求正确性」：对照 `flow-overview.md §2.1` 关键控制点 + `purchase/use-cases.md` 用例，确认 P2P 实现声明的链路与范围不偏离；找「承诺但无证据」的控制点（如「订单审核锁定价格」是否真正禁止价格修改）。
      - Skill: `multi-dimensional-audit-prompt.md`
- [x] 维度「owner-doc 对齐」：`three-way-match.md` 的匹配规则（容差/部分匹配）、`returns.md` 的退货反向链、`ar-ap-reconciliation.md` 的应付核销 openAmount 生命周期，逐条核对实现是否符合 owner doc。
      - Skill: `multi-dimensional-audit-prompt.md`
- [x] 维度「业务正确性 — 三单匹配边界」：核验 `ThreeWayMatcher` 在超容差/部分匹配/数量容差 vs 金额容差的裁决路径；发票审核时三单匹配失败的拒绝路径与错误码（`erp.err.pur.*`）。
      - Skill: `multi-dimensional-audit-prompt.md`
- [x] 维度「业务正确性 — 暂估应付 ↔ 正式应付衔接」：PURCHASE_INPUT（暂估应付）与 AP_INVOICE（正式应付）在 `ErpFinArApItem` 的生成/开闭关系；暂估冲回路径（发票到达后暂估回冲）在端到端连续场景的借贷平衡与 openAmount 一致性。
      - Skill: `multi-dimensional-audit-prompt.md`
- [x] 维度「业务正确性 — 付款核销与 openAmount 回减」：付款核销（`ErpPurPayment__settle` + `ErpFinReconciliation`）对发票状态的依赖校验；openAmount 回减至零的路径；部分核销（PARTIAL）与全额核销（PAID）的状态机正确性。
      - Skill: `multi-dimensional-audit-prompt.md`
- [x] 维度「业务正确性 — 退货反向链」：PURCHASE_RETURN 红字过账 → 应付辅助账负 openAmount 回减 → 退款核销的连续路径；与正向 P2P 的状态/金额一致性（退货数量/金额回退不破坏已核销发票的账龄与余额）。
      - Skill: `multi-dimensional-audit-prompt.md`
- [x] 维度「业务正确性 — 多币种 P2P」：amountSource/exchangeRate/amountFunctional 在 PO→Receive→Invoice→Payment 链的汇率传递；本位币凭证生成的借贷平衡；多币种核销的汇兑损益处理（若有）。
      - Skill: `multi-dimensional-audit-prompt.md`
- [x] 维度「架构或边界影响」：复核 P1-MA1-029（contract 跨域写 pur 发票行绕过 `IErpPurInvoiceBiz` 审批管道）在 P2P 运行时的行为影响——是否产生未过账/未三单匹配/状态不一致的发票（升级为运行时缺陷）或仅治理问题（业务正确性不受影响）。
      - Skill: `multi-dimensional-audit-prompt.md`
- [x] 维度「验证充分性」：对 P2P 主链 E2E（`TestErpPurProcureToPayEnd`+`TestErpPurReturnRefundEndToEnd`）的每个验收断言，问「如果它假了，我怎么知道？」；核验断言是否覆盖暂估冲回、部分核销、退货回减等关键路径而非仅 paidStatus 派生状态。
      - Skill: `multi-dimensional-audit-prompt.md`
- [x] 维度「回归风险」：寻找「仅偶然通过狭窄验证」的 P2P 代码——如三单匹配仅在黄金路径测试通过、暂估冲回仅在单币种场景验证、退货回减仅在金额相等场景验证。
      - Skill: `multi-dimensional-audit-prompt.md`
- [x] 维度「路由和技能选择正确性」：复核 P2P 实现的任务路由与技能选择（审批三段 / 过账 Provider / 核销 BizModel）是否与工作类型匹配。
      - Skill: `multi-dimensional-audit-prompt.md`
- [x] 复核已登记 MA1 finding 的运行时行为影响：P1-MA1-022（purchase Processor 跨域只读 ErpMd*）应不影响业务正确性（只读查询）；P0-MA1-021（已闭包）确认未回退。
      - Skill: none
- [x] 产出审计报告 `docs/audits/2026-07-27-1949-arm-ma2-procure-to-pay-e2e.md`（含：链路覆盖矩阵、各维度通过/失败裁决、finding 按 P0/P1/P2 分级、MA1 finding 运行时影响复核表、残留风险）。
      - Skill: none

Exit Criteria:

- [x] P2P 全链 5 个已识别控制点（三单匹配边界 / 暂估↔正式应付衔接 / 付款核销 openAmount / 退货反向链 / 多币种）均有通过/失败裁决与证据
- [x] 每个多维审计维度（至少 7 维 + 项目特定 P2P 维度）至少一句裁决（含「本维度无发现」）
- [x] MA1 finding（P1-MA1-022 / P1-MA1-029 / P0-MA1-021）运行时影响复核结论已记录

### Phase 2 - P0 即时通道处理 + P1 汇总交接 MR1 + 索引/矩阵更新

Status: completed
Targets: P2P 审计发现的 P0/P1 finding；`docs/audits/arm-index.md`；`docs/audits/audit-remediation-scope-and-dimension-matrix.md` §2.2
Skill: none

- Item Types: `Fix | Add | Follow-up`
- Prereqs: Phase 1 完成（finding 全部识别）

- [x] P0 finding 即时处理：每个 P0（数据不一致 / 凭证借贷失衡 / 状态机非法转移 / 跨域写绕过审批管道致脏数据）当即就地修复（改源文件 + `mvn clean install -DskipTests` + 该修复独立审计）或异步注入 fix plan（`docs/plans/YYYY-MM-DD-HHmm-arm-fix-*.md`）。P0 永不进入 MR 批量修复。每个 P0 在报告中标注修复路径与状态。
      - Skill: none
- [x] P1 finding 汇总：全部 P1 登记至 `arm-index.md` §P1 发现汇总（Finding ID `P1-MA2-NNN`、报告、描述、目标 MR1、修复状态 todo），供 R1.0 展开机制转化为具体修复工作项行。
      - Skill: none
- [x] 更新 arm-index 报告清单（新增本报告行）+ scope matrix §2.2 "业财端到端" 行 finance/purchase 相关列终态标记（`❓` → `✅`/`⚠️(P1)`）。
      - Skill: none

Exit Criteria:

- [x] 所有 P0 已即时处理（修复或注入 fix plan）并标注状态
- [x] 所有 P1 已登记 arm-index §P1 汇总，待 R1.0 展开
- [x] arm-index 报告清单 + scope matrix §2.2 已反映审计结论

## Draft Review Record

- Independent draft review iteration 1: **acceptable-as-is**（`ses_05c91a5a`，独立 general 子代理，对照实时仓库逐项复核）。VERDICT = acceptable-as-is，**无 BLOCKER**。核实要点：6 个 purchase BizModel + 4 个 Processor + finance 核销层 + `ThreeWayMatcher` + 过账 Provider 全部存在；`ErpPurOrderProcessor:302,314`/`ErpPurPaymentProcessor:228,240` daoFor 与 arm-index P1-MA1-022 一致；E2E 测试非平凡；owner docs 全部存在；MA1 finding（P0-MA1-021/P1-MA1-022/P1-MA1-029）登记无误；roadmap A1.1–A1.14 全 done、A2.1 是 MA2 首个 todo；零 anti-slack 违规；Plan Status 正确保持 draft。采纳的非阻塞修正：(1) 测试行数 `~427` → 实测 `~644`（`TestErpPurReturnRefundEndToEnd` `~632`）；(2) 同步修正「审批-触发-过账三段」继承描述（plain Processor 非 `AbstractApproveProcessor` 继承者，继承者为 `*ApproveProcessor` 兄弟类——与 A2.2 iteration 1 同型修订一并执行，保持两份计划基线措辞一致）。两项均已完成。
- Independent draft review iteration 2: _（可选；iteration 1 = acceptable-as-is 且非阻塞修正已落地，基线现已精确）_

## Closure Gates

> 本计划主体是审计（不改代码）。完整仓库验证在此处运行一次（确认审计期间任何 P0 即时修复未引入回归）。若无 P0 即时修复（仅 P1 登记），则 build/test 门控为回归基线确认。

- [x] 范围内行为完成（A2.1 P2P 全链多维审计报告产出 + arm-index 更新 + scope matrix §2.2 标记完成）
- [x] 相关文档对齐（审计报告、arm-index、scope matrix、flow-overview/purchase owner doc 结论已反映）
- [x] 已运行验证：零 P0 即时修复 → 全量 `mvn clean install -DskipTests` + `mvn test` 作回归基线确认；若有 P0 即时修复则该修复子切片独立验证
- [x] 无范围内项目降级为 deferred/follow-up（P1 不属降级——按设计进入 MR1；P0 不得降级为 MR）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控、日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### A2.17 并发风险（P2P 并发付款核销同一发票）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: roadmap 显式分配给 A2.17 并发与乐观锁审计（独立 skill `open-ended-audit-prompt.md`）。本审计仅标注观察到的并发敏感点，不做系统性并发正确性裁决。
- Successor Required: `yes`——A2.17 执行时复核。

### A2.4 P2P 入库成本核算方法正确性

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本审计确认 PURCHASE_INPUT 凭证方向（借存货）正确，但存货成本核算方法（移动加权/FIFO/批次）的正确性归 A2.4 库存核算一致性。
- Successor Required: `yes`——A2.4 执行时复核。

## Closure

Status Note: A2.1 P2P 端到端多维审计已完成。审计结论 **passes multi-dimensional audit**——零 P0（无数据不一致/凭证失衡/状态机非法转移/跨域脏数据）；P2P 全链组件齐备（PO→Receive→Invoice→Pay + 域级/正式双层核销 + 退货反向链 + 冲销反写），E2E 覆盖黄金路径与反向冲销。3 项 P1 登记入 arm-index 待 MR1：P1-MA2-001 暂估冲回缺失 / P1-MA2-002 多币种 P2P 本位币凭证路径未验证 / P1-MA2-003 付款核销缺三单匹配完成态复核；5 项 P2 watch-only；MA1 finding（P1-MA1-022 跨域只读 / P1-MA1-029 contract 跨域写 / P0-MA1-021 已闭包）运行时影响复核无升级。回归基线验证通过：`mvn clean install -DskipTests` BUILD SUCCESS（156 模块 reactor）+ `mvn test` BUILD SUCCESS（全绿，9:12 min，仅已知 web 冒烟 @Disabled skip）。

Closure Audit Evidence:

- 审计报告：`docs/audits/2026-07-27-1949-arm-ma2-procure-to-pay-e2e.md`（> Audit Status: closed）——链路覆盖矩阵 + 12 维裁决 + 5 控制点裁决 + finding 分级 + MA1 运行时复核表 + 残留风险。
- arm-index 同步：`docs/audits/arm-index.md` 报告清单新增本报告行（done）；§P1 发现汇总新增 P1-MA2-001/002/003（含 P1-MA1-029 运行时复核注记）；§P2 汇总新增 P2-MA2-004~008。
- scope matrix 同步：`docs/audits/audit-remediation-scope-and-dimension-matrix.md §2.2`「业财端到端」行 finance/purchase `❓` → `⚠️(P1)`。
- roadmap 同步：`docs/backlog/audit-remediation-roadmap.md` A2.1 `ready` → `done`。
- 回归基线：`mvn clean install -DskipTests -T 1C` → BUILD SUCCESS（156 模块）；`mvn test -T 1C` → BUILD SUCCESS（9:12 min，全绿）。

Follow-up:

- P1 finding 经 R1.0 展开机制进入 MR1
- 若 P0 即时修复注入 fix plan，该 fix plan 独立 closure（本审计零 P0，无注入）
