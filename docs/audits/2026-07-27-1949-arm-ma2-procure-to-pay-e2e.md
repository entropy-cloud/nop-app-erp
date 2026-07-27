# MA2 采购到付款（P2P）端到端多维审计报告

> Audit Status: closed
> 里程碑：MA2（业务正确性层）
> 工作项：A2.1 采购到付款端到端（PO→Receive→Invoice→Pay）
> 审计日期：2026-07-27
> Skill：`docs/skills/multi-dimensional-audit-prompt.md`（项目定制化层 `docs/skills/README.md`）
> 来源 plan：`docs/plans/2026-07-27-1949-1-audit-remediation-ma2-procure-to-pay-e2e.md`
> 行为基线：`docs/design/flow-overview.md §2.1` + `§四`；`docs/design/purchase/{use-cases,three-way-match,state-machine,returns}.md`；`docs/design/finance/{ar-ap-reconciliation,posting}.md`
> 审计结论：**passes multi-dimensional audit**（零 P0；3 项 P1 登记入 arm-index 待 MR1；5 项 P2 watch-only；MA1 finding 运行时影响复核无升级）

---

## 1. 链路覆盖矩阵

P2P 全链经实时仓库逐文件核实，链路组件齐备：

| 链路环节 | 实现文件（核实存在） | 三段归属 | 核实结论 |
|----------|---------------------|----------|----------|
| PO 审批-触发-过账 | `ErpPurOrderBizModel` → `ErpPurOrderApproveProcessor`（extends `AbstractApproveProcessor`）→ plain `ErpPurOrderProcessor` + `IErpFinBudgetCommitmentBiz`/`IErpFinIntercompanyTransferBiz` 钩子 | 审批/编排 | ✅ 组件齐备；COMMITMENT/intercompany 经 config-gate 默认关 |
| Receive 审批-触发-过账 | `ErpPurReceiveBizModel` → `ErpPurReceiveApproveProcessor` → plain `ErpPurReceiveProcessor` + `IErpInvStockMoveBiz`（库存移动 DONE）+ InvAcctDocProvider `PURCHASE_INPUT`（借 1401 存货 / 贷 2202 暂估应付） | 审批/编排/过账 | ✅ 库存联动 + 暂估应付凭证生成 |
| Invoice 审批-触发-过账 | `ErpPurInvoiceBizModel` → `ErpPurInvoiceApproveProcessor` → plain `ErpPurInvoiceProcessor.approve`（`validateBusinessRulesForApprove` → `ThreeWayMatcher.match`）+ `PurInvoicePostingDispatcher`（AP_INVOICE）→ `PurAcctDocProvider`（借 1403 在途物资 + 借 2221 进项税 / 贷 2202 应付）+ `ErpFinArApItemGenerator`（DIRECTION_PAYABLE 正项） | 审批/编排/过账 | ✅ 三单匹配 + 正式应付凭证 + 辅助账项 |
| Payment 审批-触发-过账 | `ErpPurPaymentBizModel` → `ErpPurPaymentApproveProcessor` → plain `ErpPurPaymentProcessor.approve` + `PurPaymentPostingDispatcher`（PAYMENT）→ `PurAcctDocProvider`（借 2202 应付 / 贷 1002 银行存款）+ `ErpFinArApItemGenerator`（DIRECTION_PAYABLE 正项，供核销） | 审批/编排/过账 | ✅ 付款凭证 + 付款辅助账项 |
| 域级核销（运营核销） | `ErpPurPayment__settle` / `__reverseSettlement` → `ErpPurPaymentProcessor` → `PaymentSettler`（`ErpPurPaymentLine` 载体，回写发票 `paidAmount`/`paidStatus` 与付款 `writtenOffStatus`） | 编排 | ✅ 部分核销（PARTIAL）/ 全额核销（PAID）/ 反向负金额行回退 |
| 正式核销（GL/账龄） | `ErpFinReconciliationBizModel.{create,post,reverse,previewReverse,runAutoReconciliation}` + `ReconciliationSettler` + `PartnerBalanceUpdater`（作用于辅助账 `ErpFinArApItem`） | 编排 | ✅ 头+行结构；DRAFT→POSTED→REVERSED 状态机 |
| 退货反向链 | `ErpPurReturnBizModel` → `ErpPurReturnApproveProcessor` → plain `ErpPurReturnProcessor.approve`（出库移动 + `PurReturnPostingDispatcher` PURCHASE_RETURN）→ `PurAcctDocProvider`（借 2202 暂估应付 / 贷 1401 存货）+ `ErpFinArApItemGenerator`（PUR_RETURN 负 openAmount credit memo） | 审批/编排/过账 | ✅ 红字冲减 + 负辅助账项；reverseApprove 经 `ensureReversed`（postingDispatcher.reverse + stockMoveBiz.reverse） |
| 冲销反写 | `PurReversalListener`（`IErpFinVoucherReversedListener`）监听 `VoucherReversedEvent`，AP_INVOICE/PAYMENT/PUR_RETURN→REJECTED+posted=false；PURCHASE_INPUT→仅 posted=false（库存物理冲销独立） | 反写 | ✅ 经 I*Biz / Facade，未绕过 |

**E2E 测试覆盖**：`TestErpPurProcureToPayEnd`（644 行，IGraphQLEngine 全链：建单→入库→发票→付款→部分核销→反向冲销，断言 posted/paidStatus/voucher 回链）+ `TestErpPurReturnRefundEndToEnd`（632 行，退货反向连续链）+ finance 侧 `TestErpFinArApItemGeneration` / `TestErpFinReversalDispatch`。E2E `tests/e2e/business-actions/pur-return.action.spec.ts` 含浏览器层。

---

## 2. 五个已识别控制点裁决

### 2.1 三单匹配边界 → ⚠️ 业务正确性基本符合 owner doc，但有 P2 偏差

**裁决：PASS（带 P2 偏差）**

`ThreeWayMatcher.match`（`module-purchase/erp-pur-service/.../service/entity/ThreeWayMatcher.java`）逐路径核实：

- **数量匹配**：`invoiceQty > receivedQty` 硬拒绝（严格模式抛 `ERR_INVOICE_QTY_MISMATCH`，非严格模式 warn 放行）。对齐 `three-way-match.md §差异处理`「发票数量 > 入库数量 → 拒绝（除非配置允许）」。✅
- **价格匹配**：`priceDiffPercent(invoicePrice, orderPrice) > priceTolerance`（默认 5%），经入库行 `orderLineId` 回链订单行 `unitPrice`。对齐 `three-way-match.md §价格匹配`。✅ 价格容差百分比计算 `|diff|/orderPrice*100`（4 位 HALF_UP），边界（恰等于容差）放行，合理。
- **回链路径**：`invoiceLine.receiveLineId → ErpPurReceiveLine.orderLineId → ErpPurOrderLine.unitPrice`（非 design 概念名 `source_order_line_id`），与文件 Javadoc 自述一致。`receiveLineId == null` 的行跳过匹配——对齐 `three-way-match.md §回链关系`「回链可选，支持无订单采购/直接凭发票入库」。✅
- **严格模式开关**：`erp-pur.match-strict-mode` 默认 false（非严格 warn+放行），生产传 null 走配置、测试可注入 strictOverride。✅

**偏差（P2）**：
- `P2-MA2-004`：`erp-pur.match-qty-tolerance` 配置被读取（`qtyTolerancePercent()`）但在 invoice 侧未实际使用——`qtyTolerance` 变量计算后被空守护置零，实际数量校验是硬 `invoiceQty > receivedQty` 无容差。语义上正确（invoice 侧数量容差属「运费/杂费明细」例外，非默认），但配置读取后未使用易误导。watch-only，MR1 顺手清理或文档化「qty 容差仅作用于 receive-vs-order 侧」。
- `P2-MA2-005`：owner doc `three-way-match.md` 内部不一致——`§一致性规则:92`「失败则拒绝审核」与 `§匹配严格度:84-88`「默认非严格模式：超容差差异提示警告，允许审核通过」冲突。代码遵循可配 strict-default-false（warn+放行）。watch-only，MR1 顺手统一 owner doc 表述（建议 §一致性规则注记「严格模式生效时」）。

### 2.2 暂估应付 ↔ 正式应付衔接 → ⚠️ P1 暂估冲回缺失

**裁决：FAIL（P1 — 暂估冲回缺失，影响 GL 应付余额准确性；辅助账层不受影响）**

核实 PURCHASE_INPUT（暂估应付）与 AP_INVOICE（正式应付）的衔接路径：

- **PURCHASE_INPUT**（`InvAcctDocProvider`，receive 审核触发）：借 1401 库存商品 / 贷 2202 暂估应付。
- **AP_INVOICE**（`PurAcctDocProvider`，invoice 审核触发）：借 **1403 在途物资** + 借 2221 进项税 / 贷 2202 应付。
- **关键缺口**：在「先入库后开票」（货到票未到 → 货到票到）的 P2P 黄金路径下，PURCHASE_INPUT 已贷 2202 暂估应付，AP_INVOICE 到达时**未自动红冲暂估应付**，而是再贷一笔 2202 应付并借 1403 在途物资。后果：
  1. GL 2202 应付账款**重复计列**暂估应付 + 正式应付（双计），应付余额虚高，直至期末人工清理或对账。
  2. 1403 在途物资在「货已到」场景仍被借记（语义为「票到货未到」），与 1401 库存商品**双计存货**，无清理分录。
  3. 辅助账层（`ErpFinArApItem`）**不受影响**——`ErpFinArApItemGenerator.resolveProfile` 明确不处理 PURCHASE_INPUT（`PURCHASE_INPUT/DEPRECIATION 为空操作`，文件 Javadoc:32），只 AP_INVOICE 生成 DIRECTION_PAYABLE 正项。故核销/账龄/Partner 余额在辅助账层正确。
- **owner doc 对照**：`returns.md §暂估应付冲减` 描述的暂估冲回**仅在退货链**（PURCHASE_RETURN 借 2202 暂估 / 贷 1401 存货）实现；正向 receive→invoice 的暂估冲回**未实现**。`posting.md §业务类型映射` 对 AP_INVOICE 借方写「费用/采购」（未指定 1403 在途物资），未强制暂估冲回；故代码用 1403 是实现选择，但缺冲回步骤属会计完整性缺口。
- **严重性裁定**：**非 P0**——每张凭证自身借贷平衡（无失衡），无状态机非法转移，无跨域写脏数据；属「缺失清理/冲回步骤」的会计准确性 P1。归 MR1 裁决（实现 GRNI 自动冲回 / 或登记为期末人工清理的 documented simplification）。

→ 登记 `P1-MA2-001`（见 §4）。

### 2.3 付款核销与 openAmount 回减 → ⚠️ P1 缺三单匹配完成态复核

**裁决：PASS（带 P1 — 付款核销未复核发票三单匹配完成态）**

`PaymentSettler.settle`（`module-purchase/erp-pur-service/.../service/entity/PaymentSettler.java`）逐约束核实：

- **状态依赖**：`requireInvoiceForSettle` 校验发票 `approveStatus=APPROVED` + 同 supplier（`ERR_SETTLE_SUPPLIER_MISMATCH`/`ERR_SETTLE_INVOICE_NOT_APPROVED`）。付款须 `APPROVED`（`ERR_SETTLE_PAYMENT_NOT_APPROVED`）。✅
- **金额校验**：`amount ≤ invoiceBalance(totalAmountWithTax − paidAmount)`（`ERR_SETTLE_OVER_INVOICE_BALANCE`）+ `amount ≤ paymentRemaining`（`ERR_SETTLE_OVER_PAYMENT_BALANCE`）。✅
- **openAmount 回减**：`recomputeInvoicePaid` 重算 `paidAmount = Σ PaymentLine.amount`（含反向负金额行），`paidStatus` 按 UNPAID/PARTIAL/PAID 派生；`recomputePaymentWrittenOff` 对付款 `writtenOffStatus` 同型派生。部分核销（PARTIAL）与全额核销（PAID）状态机正确。✅
- **反向核销**：`reverseSettlement` 生成负金额 PaymentLine（保留审计轨迹），余额与状态自然回退。✅

**缺口（P1）**：
- `P1-MA2-003`：`three-way-match.md §匹配时机:48` 声明「付款前最终校验：付款核销时确认发票已完成三单匹配」。`PaymentSettler` **不复核三单匹配完成态**——仅依赖 invoice APPROVED（而 approve 在非严格默认模式下 match 为 warn+放行，匹配「完成」语义弱）。即：一张价格严重超容差的发票在非严格模式下 APPROVED 后，付款核销无任何二次门禁。归 MR1 裁决（settle 前复核 invoice 三单匹配状态标记，或显式接受「APPROVED 即匹配通过」并更新 owner doc）。

**并发敏感点（deferred A2.17）**：`settle` 的「读 invoiceBalance → 写 PaymentLine」无悲观/乐观锁；并发核销同一发票可双读余额双写过付。`recomputeInvoicePaid` 事后聚合不能阻止中间态过付。→ `P2-MA2-008` 观察，归 A2.17 系统性并发审计。

### 2.4 退货反向链 → ⚠️ P2 owner doc 实现偏离

**裁决：PASS（带 P2 — 已开票退货的红字发票流程以 credit-memo-via-return 替代）**

`ErpPurReturnProcessor.approve` + `PurReturnPostingDispatcher` + `ErpFinArApItemGenerator`（PUR_RETURN 分支）逐路径核实：

- **库存联动**：approve → `triggerOutgoingMove`（`IErpInvStockMoveBiz.generateMove`，OUTGOING 出库）→ flushSession → 过账。reverseApprove/cancel → `ensureReversed`（`postingDispatcher.reverse` + `stockMoveBiz.reverse`）。✅
- **PURCHASE_RETURN 红字过账**：`PurAcctDocProvider`（借 2202 暂估应付 / 贷 1401 存货，不含税 TOTAL_AMOUNT）——反向 InvAcctDocProvider.PURCHASE_INPUT。✅ 借贷平衡。
- **辅助账回减**：`ErpFinArApItemGenerator` PUR_RETURN 分支生成 DIRECTION_PAYABLE + **负 openAmount** credit memo（`resolveAmountFunctional` 对 SOURCE_BILL_PUR_RETURN 取 `TOTAL_AMOUNT.negate()`），使 `PartnerBalanceUpdater.sumOpen` 自然减计应付余额。✅ 与正向 P2P 的一致性：退货数量/金额回退经负项表达，不破坏已核销发票的账龄与余额（核销在辅助账项层面多对多匹配）。
- **状态机**：UNSUBMITTED→SUBMITTED→APPROVED→（reverseApprove）REJECTED / CANCELLED，三轴（docStatus/approveStatus + 派生 returnStatus 经 `ReturnQtyValidator`）一致。✅

**owner doc 偏离（P2）**：
- `P2-MA2-006`：`returns.md §红字发票处理` 描述「已开票退货 → 生成红字发票单（ErpPurInvoice 负金额）→ 审核红字发票 → 红字凭证」流程；**实现未生成红字 ErpPurInvoice**，而是以 PURCHASE_RETURN 过账 + 负 ArApItem credit memo 表达。功能上 AP 余额回减等价（辅助账层），但 GL 层只冲暂估应付（2202 暂估侧），不冲正式应付（2202 应付侧，即 AP_INVOICE 已贷记的 formal AP）——**与 §2.2 暂估冲回缺失同根**：已开票退货应冲 formal AP（红字 AP_INVOICE），实冲 accrued AP。watch-only，MR1 顺手更新 owner doc 反映 credit-memo-via-return 实现，或与 P1-MA2-001 一并裁决。

### 2.5 多币种 P2P → ⚠️ P1 本位币凭证路径未验证

**裁决：FAIL（P1 — 多币种端到端未验证，本位币凭证生成路径不可证）**

核实多币种传递链：

- **PostingEvent 契约**：`PurInvoicePostingDispatcher.buildEvent` / `PurReturnPostingDispatcher.buildEvent` / `PurPaymentPostingDispatcher` 均传 `currencyId` + `exchangeRate`（fallback `BigDecimal.ONE`）+ `voucherDate`。`ErpFinArApItemGenerator` 同时存 `amountSource`（fallback amountFunctional）+ `amountFunctional` + `exchangeRate`（fallback ONE）。辅助账层多币种四件套齐备。✅
- **GL 凭证层缺口**：`VoucherFact`（`module-finance/erp-fin-service/.../service/posting/VoucherFact.java`）**仅有单一 `amount` 字段**，无 `amountSource`/`amountFunctional` 分离。`PurAcctDocProvider.createFacts` 将 source-currency 的 `TOTAL_AMOUNT`/`TOTAL_TAX_AMOUNT`/`TOTAL_AMOUNT_WITH_TAX` 直接写入 `VoucherFact.amount`。即：Provider 产出的是**源币种金额**的 fact，本位币折算是否发生取决于过账引擎装配 `ErpFinVoucherLine` 时是否按 `PostingEvent.exchangeRate` 转换——该转换路径在 P2P 链**无 E2E 证据**。
- **E2E 验证空白**：`TestErpPurProcureToPayEnd` / `TestErpPurReturnRefundEndToEnd` 均为单币种场景（exchangeRate=ONE，source=functional），从未以非本位币驱动 P2P 链。owner doc `posting.md §多币种处理:478-486` 声明「凭证分录行同时记录 amountSource / amountFunctional / exchangeRate」「汇率锁定时机：本位币金额在业务单据创建时按业务日期汇率锁定」——该契约在 P2P GL 层的落实**无测试证据**。
- **严重性裁定**：**非 P0**——单币种下 source=functional，无错误；多币种为「未验证路径」而非「已证错误」。归 MR1 裁决（补多币种 P2P E2E + 核实引擎折算 + 必要时 VoucherFact 增 amountSource/amountFunctional）。

→ 登记 `P1-MA2-002`（见 §4）。

---

## 3. 多维审计裁决（每维至少一句）

| # | 维度 | 裁决 | 证据/说明 |
|---|------|------|-----------|
| 1 | **需求正确性** | ⚠️ P2 | `flow-overview.md §2.1` 关键控制点「订单审核锁定价格」在 `ErpPurOrderProcessor.approve` **无服务端价格字段锁**——无 approve 后置对 orderLine.unitPrice 的修改守卫，依赖 CrudBizModel 常规更新约束。「承诺但无证据」控制点。→ `P2-MA2-007`。其余控制点（入库超容差审批/发票三单匹配/付款核销检查发票状态）均有实现证据。 |
| 2 | **owner-doc 对齐** | ⚠️ P2 | `three-way-match.md` 内部不一致（§一致性规则 vs §匹配严格度，P2-MA2-005）；`returns.md §红字发票处理` 与实现偏离（P2-MA2-006）；`ar-ap-reconciliation.md` 辅助账 openAmount 生命周期实现符合（头+行结构、settledAmount/openAmount/status 回写一致）。 |
| 3 | **业务正确性 — 三单匹配边界** | ✅ PASS（P2） | 见 §2.1。数量硬拒绝 + 价格容差 + 严格模式开关符合 owner doc；qty 容差配置读取未用属 P2-MA2-004。 |
| 4 | **业务正确性 — 暂估↔正式应付衔接** | ❌ FAIL（P1） | 见 §2.2。`P1-MA2-001` 暂估冲回缺失，GL 2202 双计；辅助账层不受影响。 |
| 5 | **业务正确性 — 付款核销 openAmount** | ⚠️ P1 | 见 §2.3。`P1-MA2-003` 缺三单匹配完成态复核；openAmount 回减本身正确。 |
| 6 | **业务正确性 — 退货反向链** | ✅ PASS（P2） | 见 §2.4。红字过账 + 负 credit memo 正确；owner doc 红字发票流程偏离 P2-MA2-006。 |
| 7 | **业务正确性 — 多币种 P2P** | ❌ FAIL（P1） | 见 §2.5。`P1-MA2-002` 本位币凭证路径未验证，VoucherFact 单 amount 字段，多币种 E2E 空白。 |
| 8 | **架构或边界影响** | ✅ PASS | P1-MA1-029 复核：`ErpCtInvoicePlanBizModel.createApInvoiceDraft` 生成 **unposted UNSUBMITTED DRAFT**（`posted=false`/`docStatus=DRAFT`/`approveStatus=UNSUBMITTED`），后续经 purchase 正常审批+过账管道处理（Javadoc:44-45 自述）；生成行无 `receiveLineId` → 三单匹配跳过，对齐 `three-way-match.md §回链关系`「回链可选」。**无未过账/未匹配/状态不一致的 APPROVED 脏发票产生**。裁决：**治理问题（已在 arm-index P1-MA1-029 登记），业务正确性不受影响，不升级**。 |
| 9 | **验证充分性** | ⚠️ | E2E 断言覆盖 posted/paidStatus/voucher 回链/部分核销/反向冲销，但**未覆盖**：暂估冲回（无断言因功能未实现）、多币种（单币种 only）、退货 formal-AP 回减（只断言 accrued 侧）。对每个验收断言「如果它假了，我怎么知道」：posted/paidStatus 派生状态有独立证据；暂估冲回/多币种**无独立证明策略**（因实现/测试双缺）。 |
| 10 | **回归风险** | ⚠️ P1/P2 | 「仅偶然通过狭窄验证」代码：①三单匹配仅在黄金路径（receiveLineId 非空）测试，receiveLineId-null 跳过路径无负向测试；②暂估冲回仅在单币种+未开票退货验证（实际未实现）；③多币种 P2P 完全无验证（P1-MA2-002）；④PaymentSettler 并发无验证（P2-MA2-008，A2.17）。 |
| 11 | **路由和技能选择正确性** | ✅ PASS | P2P 实现任务路由正确（审计工作 → multi-dimensional-audit-prompt skill；实现期审批三段 → Facade+Processor+IErpFinAcctDocProvider；核销 → ReconciliationSettler+PaymentSettler 双层）。技能选择与工作类型匹配。 |
| 12 | **待办或自主权策略漂移** | ✅ PASS | 审计范围（P2P 全链 5 控制点 + MA1 finding 复核）与产出（本报告 + arm-index 登记）边界一致；A2.2/A2.3/A2.4/A2.17 显式 Non-Goal，无静默扩缩范围；P1 不属降级（按设计进 MR1）。 |

**反窄化自检**：本审计覆盖 12 维（≥7 维要求），每维至少一句裁决，未单维过度深挖。✅

---

## 4. Finding 分级汇总

### P0（即时通道）

**无。** 核实路径：无凭证借贷失衡（每张凭证自身平衡）、无状态机非法转移（所有迁移经 `validateTransitionFor*` 守卫）、无跨域写绕过审批管道致脏数据（P1-MA1-029 生成 unposted DRAFT，经正常管道）、无数据不一致（辅助账层 openAmount 回减正确）。

### P1（登记 arm-index §P1，目标 MR1）

| Finding ID | 域 | 描述 | 目标 MR |
|-----------|---|------|--------|
| `P1-MA2-001` | purchase+finance | **暂估应付冲回缺失**：PURCHASE_INPUT（receive，贷 2202 暂估应付）与 AP_INVOICE（invoice，贷 2202 应付 + 借 1403 在途物资）在「先入库后开票」黄金路径无自动冲回；GL 2202 双计暂估+正式应付，1403 在途物资与 1401 库存商品双计存货，无清理分录。辅助账层（ErpFinArApItem）不受影响（不处理 PURCHASE_INPUT）。owner doc `returns.md §暂估应付冲减` 仅在退货链实现冲回，正向 receive→invoice 冲回未实现。修复方式：MR1 裁决——实现 GRNI 自动冲回（invoice approve 时红冲关联 receive 的 PURCHASE_INPUT 凭证）或登记为期末人工清理的 documented simplification + 更新 owner doc。 | MR1 |
| `P1-MA2-002` | purchase+finance | **多币种 P2P 本位币凭证路径未验证**：`VoucherFact` 仅单一 `amount` 字段（无 amountSource/amountFunctional 分离）；`PurAcctDocProvider.createFacts` 将 source-currency `TOTAL_*` 直接写入 fact.amount，本位币折算依赖过账引擎装配 ErpFinVoucherLine 时按 PostingEvent.exchangeRate 转换——该路径在 P2P 链无 E2E 证据。E2E 测试（TestErpPurProcureToPayEnd/TestErpPurReturnRefundEndToEnd）均单币种（exchangeRate=ONE）。owner doc `posting.md §多币种处理` 契约在 P2P GL 层落实无测试证据。修复方式：MR1 补多币种 P2P E2E + 核实引擎折算路径 + 必要时 VoucherFact 增 amountSource/amountFunctional 双字段。 | MR1 |
| `P1-MA2-003` | purchase | **付款核销缺发票三单匹配完成态复核**：`PaymentSettler.settle` 仅校验发票 `approveStatus=APPROVED`，不复核三单匹配完成态。owner doc `three-way-match.md §匹配时机:48`「付款前最终校验：付款核销时确认发票已完成三单匹配」未落实。在非严格默认模式（match=warn+放行）下，价格严重超容差发票 APPROVED 后付款核销无二次门禁。修复方式：MR1 裁决——settle 前复核 invoice 三单匹配状态标记，或显式接受「APPROVED 即匹配通过」并更新 owner doc §匹配时机。 | MR1 |

### P2（watch-only，待 MR 顺手收敛）

| Finding ID | 域 | 描述 | 处置 |
|-----------|---|------|------|
| `P2-MA2-004` | purchase | `ThreeWayMatcher` 读取 `erp-pur.match-qty-tolerance` 配置但 invoice 侧未使用（invoice>receive 硬拒绝无容差；qty 容差语义属 receive-vs-order 侧）。dead config read 易误导。 | watch-only，MR1 顺手清理或文档化 |
| `P2-MA2-005` | docs（purchase） | `three-way-match.md` 内部不一致：`§一致性规则:92`「失败则拒绝审核」vs `§匹配严格度:84-88」默认非严格 warn+放行」。代码遵循可配 strict-default-false。 | watch-only，MR1 统一 owner doc 表述 |
| `P2-MA2-006` | docs+purchase | `returns.md §红字发票处理` 描述「已开票退货→红字 ErpPurInvoice」流程；实现以 PURCHASE_RETURN 过账 + 负 ArApItem credit memo 替代（功能等价于 AP 余额回减，但 GL 冲暂估侧非 formal 侧）。owner doc 与实现偏离。 | watch-only，MR1 更新 owner doc 反映 credit-memo-via-return 实现（与 P1-MA2-001 一并裁决） |
| `P2-MA2-007` | purchase | `flow-overview.md §2.1`「订单审核锁定价格」控制点在 `ErpPurOrderProcessor.approve` 无服务端价格字段锁，依赖 CrudBizModel 常规更新约束。「承诺但无证据」控制点。 | watch-only，MR1 顺手补 approve 后置价格守卫或更新 owner doc 标注「锁定」语义 |
| `P2-MA2-008` | purchase | `PaymentSettler.settle`「读 invoiceBalance→写 PaymentLine」无悲观/乐观锁，并发核销同一发票可双读双写过付；recomputeInvoicePaid 事后聚合不能阻止中间态过付。 | watch-only，归 A2.17 并发与乐观锁系统性审计 |

---

## 5. MA1 finding 运行时行为影响复核

| Finding ID | 原裁决 | 运行时行为复核（本审计） | 终态 |
|-----------|--------|------------------------|------|
| `P1-MA1-022` | 跨域只读 `daoFor(ErpMd*)`（purchase Processor `ErpPurOrderProcessor:302,314` ErpMdSubject/ErpFinAccountingPeriod + `ErpPurPaymentProcessor:228,240`），待 MR1 | **不影响业务正确性**——daoFor 返回与 I*Biz 等价的只读实体（ErpMdSubject/ErpFinAccountingPeriod），用于预算科目/期间解析的纯查询；无写、无状态变更、无脏读。P2P 主链行为正确。维持治理层 finding（MR1 迁移至 I*Biz 便捷只读方法），**不升级**。 | 治理 only，无运行时影响 |
| `P1-MA1-029` | `ErpCtInvoicePlanBizModel` 跨域写 pur/sal 发票行（绕过 `IErpPurInvoiceBiz` 审批管道），半治理待 MR1 | **不升级为运行时缺陷**——`createApInvoiceDraft:125-160` 生成 **unposted UNSUBMITTED DRAFT**（`posted=false`/`docStatus="DRAFT"`/`approveStatus=UNSUBMITTED`/`paidStatus="UNPAID"`），Javadoc:44-45 明示「生成的草稿后续由 purchase/sales 域审核过账管道处理」。生成行无 `receiveLineId` → `ThreeWayMatcher` 跳过（对齐 `three-way-match.md §回链关系`「回链可选」）。**无未过账/未三单匹配/状态不一致的 APPROVED 脏发票产生**。维持治理层 finding（MR1 登记 posting-exemptions 或迁移至 I*Biz），**不升级**。 | 治理 only，无运行时影响 |
| `P0-MA1-021` | inventory `CostAdjustmentPostingDispatcher` 跨模块写 `ErpFinVoucher`，已闭包（plan 2026-07-27-1430-1） | **确认未回退**——`PurReturnPostingDispatcher.reverse` 经 `PurPostingExecutor` → Facade `IErpFinVoucherBiz.reverse()`（非直接跨模块写 `ErpFinVoucher.isReversed`）；`ErpPurReturnProcessor.ensureReversed` 库存红冲经 `IErpInvStockMoveBiz.reverse`（I*Biz 边界）。P2P 链无 inventory 红冲绕过 I*Biz 复现。 | 已闭包，无回退 |

---

## 6. 残留风险与 successor

- **A2.17 并发**：`PaymentSettler.settle` 并发核销同一发票（P2-MA2-008）、并发付款核销、退货与付款并发——均归 A2.17 系统性并发与乐观锁审计（独立 skill `open-ended-audit-prompt.md`）。本审计仅标注观察到的并发敏感点。
- **A2.4 库存核算一致性**：PURCHASE_INPUT 凭证方向（借 1401 存货）正确，但存货成本核算方法（移动加权/FIFO/批次）正确性归 A2.4。
- **A2.3 期末结账**：P2P 的应付辅助账（AP_INVOICE 生成的 ErpFinArApItem OPEN 项）期末未核销项是 A2.3 期末门禁的前置输入；本审计确认其生成正确（AP_INVOICE → DIRECTION_PAYABLE 正项，openAmount=amountFunctional），不审计期末门禁。
- **A2.5/A2.8 状态机**：本审计覆盖 P2P 链路中状态转移的业务正确性（迁移守卫齐全），但 purchase 域状态机的系统性可达性审查归 A2.8。

---

## 7. scope matrix §2.2 终态标记（同步至 `audit-remediation-scope-and-dimension-matrix.md`）

「业财端到端」行 finance/purchase 列：`❓` → **`⚠️(P1)`**（P2P 链路组件齐备、E2E 覆盖黄金路径，但 3 项 P1 待 MR1：暂估冲回/多币种/核销匹配复核；零 P0）。其余 MA2 列保持 `❓` 待对应工作项审计。

---

> **审计结论**：P2P 全链**passes multi-dimensional audit**——零 P0（无数据不一致/凭证失衡/状态机非法转移/跨域脏数据），链路组件齐备，E2E 覆盖黄金路径与反向冲销。3 项 P1（暂估冲回缺失 P1-MA2-001 / 多币种未验证 P1-MA2-002 / 核销匹配复核缺失 P1-MA2-003）登记入 arm-index 待 MR1；5 项 P2 watch-only；MA1 finding（P1-MA1-022/P1-MA1-029/P0-MA1-021）运行时影响复核无升级。
