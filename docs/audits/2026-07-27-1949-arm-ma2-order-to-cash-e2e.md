# MA2 销售到收款（O2C）端到端多维审计报告

> Audit Status: closed
> 里程碑：MA2（业务正确性层）
> 工作项：A2.2 销售到收款端到端（SO→Delivery→Invoice→Receipt）
> 审计日期：2026-07-27
> Skill：`docs/skills/multi-dimensional-audit-prompt.md`（项目定制化层 `docs/skills/README.md`）
> 来源 plan：`docs/plans/2026-07-27-1949-2-audit-remediation-ma2-order-to-cash-e2e.md`
> 行为基线：`docs/design/flow-overview.md §2.2` + `§四` + `§4.3`；`docs/design/sales/{use-cases,state-machine,returns}.md`；`docs/design/finance/{ar-ap-reconciliation,posting}.md`
> 审计结论：**passes multi-dimensional audit**（零 P0；1 项 P1 登记入 arm-index 待 MR1；6 项 P2 watch-only；MA1 finding 运行时影响复核无升级；并发敏感点交接 A2.17）

---

## 1. 链路覆盖矩阵

O2C 全链经实时仓库逐文件核实，链路组件齐备：

| 链路环节 | 实现文件（核实存在） | 三段归属 | 核实结论 |
|----------|---------------------|----------|----------|
| SO 审批-触发-过账 | `ErpSalOrderBizModel` → `ErpSalOrderApproveProcessor`（extends `AbstractApproveProcessor`）→ plain `ErpSalOrderProcessor.approve` + `CreditLimitChecker.check`（订单审核信用额度）+ `IErpFinBudgetCommitmentBiz`/`IErpFinIntercompanyTransferBiz` 钩子 | 审批/编排 | ✅ 组件齐备；COMMITMENT/intercompany 经 config-gate 默认关 |
| Delivery 审批-触发-过账 | `ErpSalDeliveryBizModel` → `ErpSalDeliveryApproveProcessor` → plain `ErpSalDeliveryProcessor.approve`：`triggerOutgoingMove:256`（`IErpInvStockMoveBiz.generateMove`）+ `enforceCreditHold:200`（config-gated）+ `enforceInspectionGate:327`（质检门控）；出库移动单 DONE → InvPostingDispatcher `SALES_OUTPUT`（借 6401 主营业务成本 / 贷 1401 库存商品，`InvAcctDocProvider`） | 审批/编排/过账 | ✅ 跨域可用量裁决（inv 域 `ErpInvStockMoveProcessor:228/385`）+ 成本结转凭证生成 |
| Invoice 审批-触发-过账 | `ErpSalInvoiceBizModel` → `ErpSalInvoiceApproveProcessor` → plain `ErpSalInvoiceProcessor.approve`（`enforceCreditHold:200` config-gated）+ `SalInvoicePostingDispatcher`（AR_INVOICE）→ `SalAcctDocProvider`（借 1131 应收 / 贷 6001 收入 / 贷 2221 销项税）+ `ErpFinArApItemGenerator`（DIRECTION_RECEIVABLE 正项） | 审批/编排/过账 | ✅ 应收凭证 + 应收辅助账项 |
| Receipt 审批-触发-过账 | `ErpSalReceiptBizModel` → `ErpSalReceiptApproveProcessor` → plain `ErpSalReceiptProcessor.approve` + `SalReceiptPostingDispatcher`（RECEIPT）→ `SalAcctDocProvider`（借 1002 银行存款 / 贷 1131 应收）+ `ErpFinArApItemGenerator`（DIRECTION_RECEIVABLE 正项，供核销） | 审批/编排/过账 | ✅ 收款凭证 + 收款辅助账项 |
| 域级核销（运营核销） | `ErpSalReceipt__settle` / `__reverseSettlement` → `ErpSalReceiptProcessor` → `ReceiptSettler`（`ErpSalReceiptLine` 载体，回写发票 `receivedAmount`/`receivedStatus` 与收款 `writtenOffStatus`） | 编排 | ✅ 部分核销（PARTIAL）/ 全额核销（RECEIVED）/ 反向负金额行回退 |
| 正式核销（GL/账龄） | `ErpFinReconciliationBizModel.{create,post,reverse,previewReverse,runAutoReconciliation}` + `ReconciliationSettler` + `PartnerBalanceUpdater`（作用于辅助账 `ErpFinArApItem`） | 编排 | ✅ 头+行结构；DRAFT→POSTED→REVERSED 状态机 |
| 退货反向链 | `ErpSalReturnBizModel` → `ErpSalReturnApproveProcessor` → plain `ErpSalReturnProcessor.approve`（`triggerIncomingMove` + `SalReturnPostingDispatcher` SALES_RETURN + `ReturnRefundOrchestrator`）→ `SalAcctDocProvider` SALES_RETURN（借 1401 库存商品 / 贷 6401 主营业务成本，反向 `InvAcctDocProvider.SALES_OUTPUT`）+ `ErpFinArApItemGenerator` SAL_RETURN 分支（DIRECTION_RECEIVABLE 负 openAmount credit memo） | 审批/编排/过账 | ✅ 红字冲减成本/存货 + 负应收辅助账项；refund 经反向 ReceiptLine 回写 |
| 冲销反写 | `SalReversalListener`（`IErpFinVoucherReversedListener`）监听 `VoucherReversedEvent`，AR_INVOICE/RECEIPT/SALES_RETURN→REJECTED+posted=false；SALES_OUTPUT→仅 posted=false（库存物理冲销独立） | 反写 | ✅ 经 I*Biz / Facade，未绕过 |

**E2E 测试覆盖**：`TestErpSalOrderToCashEnd`（728 行，IGraphQLEngine 全链：建单→出库→发票→收款→域级 settle（receivedStatus PARTIAL/RECEIVED）+ 财务正式核销 `ErpFinReconciliation`（openAmount→0 断言）+ 反向冲销）+ `TestErpSalReturnRefundEndToEnd`（660 行，退货反向连续链：负辅助账 credit memo + cancelOnReverse + 退款核销超额拒绝）。E2E `tests/e2e/business-actions/` 含 `sal-return.action.spec.ts` + `sal-date-range-validation.action.spec.ts`。

---

## 2. 六个已识别控制点裁决

### 2.1 可用量校验与库存扣减 → ✅ PASS

**裁决：PASS**

跨域可用量裁决链 `ErpSalDeliveryProcessor.triggerOutgoingMove:256` → `IErpInvStockMoveBiz.generateMove:57` → inventory 域 `ErpInvStockMoveProcessor.doConfirm:185-197` → `validateAvailable:215-235`（line 228 抛 `ERR_AVAILABLE_INSUFFICIENT`）+ `CONFIG_ALLOW_NEGATIVE_STOCK`（line 385，默认 false）逐路径核实：

- **可用量计算**：`recomputeAvailable:377-382` `available = totalQuantity − reservedQuantity − lockedQuantity`（onHand 三量分离，对齐 `flow-overview.md §2.4 关键控制点 预留量机制`）。✅
- **预留量机制**：`applyReservation:237-253` 在 CONFIRMED 时 +reserve，`releaseReservation:255` 在 DONE 时 −reserve（OUTGOING/INTERNAL_TRANSFER 路径，`reservesOnConfirm:341-347`）。销售出库 businessLinked=true → `generateMove:75-78` 同步 doConfirm+doComplete，预留立即释放。✅
- **负库存配置**：`isNegativeStockAllowed:384-387` 读 `erp-inv.allow-negative-stock`（默认 false）；为 true 时 `validateAvailable` 入口短路。对齐 `sales/state-machine.md §4 异常路径 负库存配置`。✅
- **错误码传播**：`ERR_AVAILABLE_INSUFFICIENT` 经 `IErpInvStockMoveBiz.generateMove` 抛出 → `ErpSalDeliveryProcessor.triggerOutgoingMove` 不吞异常 → `doApprove` 整个出库审核回滚（包括已 setApproveStatus 的更改不会持久化，因为异常在 `updateEntity` 之前）。✅ 销售侧未做任何"吞下 inventory 错误强行 approve"的路径。
- **owner doc 对齐**：`flow-overview.md §2.2 关键控制点 出库前校验库存可用量（不足拒绝出库）` ✓ 实现；`state-machine.md §4 异常路径 出库可用量不足 SUBMITTED→APPROVED 时拒绝` ✓ 实现。

**信用控制是独立维度**（`enforceCreditHold:200`，config-gated `erp-sal.credit-check-on-delivery` 默认 false），与可用量校验分离——见 §2.2。

### 2.2 信用额度检查 → ✅ PASS（带 P2 owner-doc 漏述扩展点）

**裁决：PASS（带 P2 — 实现已超出 owner doc 文字，但 owner doc 漏述出库/发票信用冻结扩展点）**

`ErpSalOrderProcessor.approve:194-198` → `CreditLimitChecker.check:109-130` 主路径逐约束核实：

- **额度计算口径**：`available = ErpMdPartner.creditLimit − outstanding`（`CreditLimitChecker:122-123`），其中 outstanding 由两部分组成：
  - **sales 域订单聚合**：`sumOutstandingOrders:285-300`（`approveStatus=APPROVED ∧ deliveryStatus≠DELIVERED ∧ docStatus≠CANCELLED`，按 `totalAmountWithTax × exchangeRate` 折算本位币）。
  - **finance 域 AR 辅助账**：`sumArOpenFunctional:303-314` 经 `IErpFinArApItemBiz.findOpenItemsByPartner` 跨域只读查询 DIRECTION_RECEIVABLE + status∈{OPEN,PARTIAL} 项的 `openAmountFunctional`（config-gated `erp-sal.credit-check-include-ar` 默认开启）。✅ 应收辅助账余额联动已落实。
- **本单纳入比较**：`orderAmountFunctional = totalAmountWithTax × exchangeRate`（`toFunctional:341-346`），与 outstanding 同口径（本位币）。`available < orderAmountFunctional` 触发 `enforceOverLimit:176-201`。✅
- **三级策略**：`erp-sal.credit-check-level` 默认 SOFT_WARNING（放行 + 派发通知）；HARD_BLOCK 抛 `ERR_CREDIT_LIMIT_EXCEEDED`；SPECIAL_APPROVAL 经 `IActionAuthChecker.isPermitted("erp-sal:credit-over-limit-approve")` 命令式权限门控。✅
- **超额审批路径**：SPECIAL_APPROVAL 路径 + 专项权限 = owner doc「订单审核检查客户信用额度」的语义实现（owner doc 未细分三级策略，属实现增强）。
- **信用冻结扩展点**（`enforceCreditHold:200`，delivery/invoice 审核）：`CreditLimitChecker.checkCreditHold:143-162` 检查客户**当前** `available < 0`（不叠加本单），三级策略与订单审核一致；由 `erp-sal.credit-check-on-delivery`/`erp-sal.credit-check-on-invoice`（均默认 false 向后兼容）门控。`TestErpSalCreditHoldOnDelivery` + `TestErpSalCreditHoldOnInvoice` 覆盖。

**缺口（P2 owner-doc drift）**：
- `P2-MA2-012`：`flow-overview.md §2.2 关键控制点 销售订单审核时检查客户信用额度` 仅描述**订单审核**环节；实现已扩展至**出库/发票审核**（credit hold，config-gated 默认 false）。owner doc 漏述该扩展点。watch-only，MR1 顺手更新 owner doc 反映信用冻结扩展点 + 三级策略。

### 2.3 应收 openAmount 生命周期 → ✅ PASS

**裁决：PASS**

`ErpFinArApItemGenerator.generate:65-119` + `ErpFinReconciliationBizModel` 链路逐约束核实：

- **AR_INVOICE 生成正项**：`resolveProfile:147-148`（DIRECTION_RECEIVABLE + SOURCE_BILL_AR_INVOICE）+ `resolveAmountFunctional:268-306`（取 TOTAL_AMOUNT_WITH_TAX → fallback TOTAL→TOTAL_AMOUNT→AMOUNT）→ `item.openAmountFunctional = amountFunctional`（line 113-114），`item.status = OPEN`。✅
- **RECEIPT 生成正项**：`resolveProfile:151-152`（DIRECTION_RECEIVABLE + SOURCE_BILL_RECEIPT，供核销多对多匹配）+ amount 取 TOTAL → fallback AMOUNT。✅
- **核销回减**：`ErpFinReconciliationBizModel.create` → `ReconciliationSettler` 写 `ErpFinArApItem.settledAmountSource/Functional` + 重算 `openAmountSource/Functional`，全额核销后 status=SETTLED；`PartnerBalanceUpdater.sumOpen` 重聚合 Partner 余额。`TestErpSalOrderToCashEnd:265-330` E2E 断言「发票/收款项 openAmount 核销后归零 + status=SETTLED」。✅
- **部分收款（PARTIAL）**：`recomputeInvoiceReceived:161-177` 按 Σ ReceiptLine.amount vs totalAmountWithTax 派生 UNRECEIVED/PARTIAL/RECEIVED；`recomputeReceiptWrittenOff:179-194` 同型派生 writtenOffStatus。✅ 状态机正确。
- **超额收款**：`ReceiptSettler.settle:82-94` 严格校验 `amount ≤ invoiceBalance`（`ERR_SETTLE_OVER_INVOICE_BALANCE`）+ `amount ≤ receiptRemaining`（`ERR_SETTLE_OVER_RECEIPT_BALANCE`）——**实现禁止超额收款**（与 P2P `PaymentSettler` 同型，回写后聚合）。owner doc `state-machine.md §收款状态机` 未显式提"超额"，与实现一致。
- **反向核销**：`reverseSettlement:116-137` 生成负金额 ReceiptLine（保留审计轨迹），余额与状态自然回退。✅

### 2.4 退货反向链 → ✅ PASS（带 P2 owner doc 实现偏离）

**裁决：PASS（带 P2 — 已开票退货的红字发票流程以 credit-memo-via-return 替代）**

`ErpSalReturnProcessor.approve:208-221` + `SalReturnPostingDispatcher` + `ErpFinArApItemGenerator` SAL_RETURN 分支 + `ReturnRefundOrchestrator` 逐路径核实：

- **库存联动**：`triggerIncomingMove:299-304`（`IErpInvStockMoveBiz.generateMove`，INCOMING 入库 + `originReturnedMoveId` 追溯原出库 move）→ flushSession → 过账。reverseApprove/cancel → `ensureReversed:320-334`（`postingDispatcher.reverse` + `stockMoveBiz.reverse`）。✅
- **SALES_RETURN 红字过账**：`SalAcctDocProvider`（借 1401 库存商品 / 贷 6401 主营业务成本，`KEY_TOTAL_COST` = Σ 行 quantity×unitPrice）——反向 `InvAcctDocProvider.SALES_OUTPUT`。✅ 借贷平衡。
- **辅助账回减**：`ErpFinArApItemGenerator` SAL_RETURN 分支（`resolveProfile:157-160` + `resolveAmountFunctional:296-304`）生成 DIRECTION_RECEIVABLE + **负 openAmountFunctional** credit memo（`TOTAL_AMOUNT_WITH_TAX.negate()`），使 `PartnerBalanceUpdater.sumOpen` 自然减计应收余额。✅
- **退款编排**：`ReturnRefundOrchestrator.orchestrateRefund:49-57`：对客户已收款核销的发票，调用 `ReceiptSettler.reverseSettlement` 生成负金额 ReceiptLine，回写 `ErpSalInvoice.receivedStatus`/`receivedAmount` 与 `ErpSalReceipt.writtenOffStatus`，使应收/退款闭环一致。`TestErpSalReturnRefundEndToEnd:143-210` 覆盖。✅
- **状态机**：UNSUBMITTED→SUBMITTED→APPROVED→（reverseApprove）REJECTED / CANCELLED，三轴（docStatus/approveStatus）一致。✅
- **与正向 O2C 的一致性**：退货数量/金额回退经负项表达（负辅助账 + 反向 ReceiptLine），不破坏已开票发票的账龄与余额（核销在辅助账项层面多对多匹配）。✅

**owner doc 偏离（P2）**：
- `P2-MA2-011`：`returns.md §红字发票处理:213-241` 描述「已开票退货 → 创建红字发票单（关联原蓝字，金额取负）→ 审核红字发票 → 生成红字凭证 → 更新蓝字发票冲销标志」；**实现未生成红字 ErpSalInvoice**，而是以 SALES_RETURN 过账（反向 SALES_OUTPUT 成本/存货侧）+ 负 ArApItem credit memo（应收辅助账侧）替代。功能上 AR 余额回减等价（辅助账层），但 GL 层只冲成本/存货侧（6401/1401），不冲收入/应收 GL（6001/1131/2221）。已开票退货的「收入/应收 GL 红字」未生成。watch-only，MR1 更新 owner doc 反映 credit-memo-via-return 实现（与 P2P `P2-MA2-006` 同型对称偏离）。

### 2.5 多币种 O2C 与汇兑损益 → ❌ FAIL（P1）

**裁决：FAIL（P1 — 多币种端到端未验证，收款核销汇兑损益完全未实现）**

核实多币种传递链 + 汇兑损益路径：

- **PostingEvent 契约**：`SalInvoicePostingDispatcher.buildEvent:71-90` / `SalReceiptPostingDispatcher.buildEvent:70-87` / `SalReturnPostingDispatcher.buildEvent:84-103` 均传 `currencyId` + `exchangeRate`（fallback `BigDecimal.ONE`）+ `voucherDate`。`ErpFinArApItemGenerator.generate` 同时存 `amountSource`（fallback amountFunctional）+ `amountFunctional` + `exchangeRate`（fallback ONE，line 92, 108-109）。**辅助账层多币种四件套齐备**。✅
- **信用层多币种折算**：`CreditLimitChecker` 在订单审核 + 信用冻结时均按 `totalAmountWithTax × exchangeRate` → 本位币比较，AR 辅助账取 `openAmountFunctional`（`TestErpSalOrderApproval:271-276` 覆盖外币 AR openAmountFunctional 纳入）。✅
- **GL 凭证层缺口（与 P2P `P1-MA2-002` 同根因）**：`VoucherFact`（`module-finance/erp-fin-service/.../service/posting/VoucherFact.java`）**仅有单一 `amount` 字段**（line 16），无 `amountSource`/`amountFunctional` 分离。`SalAcctDocProvider.createFacts:73-93` 将 source-currency 的 `TOTAL_AMOUNT`/`TOTAL_TAX_AMOUNT`/`TOTAL_AMOUNT_WITH_TAX`/`TOTAL` 直接写入 `VoucherFact.amount`。即：Provider 产出的是**源币种金额**的 fact，本位币折算是否发生取决于过账引擎装配 `ErpFinVoucherLine` 时是否按 `PostingEvent.exchangeRate` 转换——该转换路径在 O2C 链**无 E2E 证据**。
- **O2C 特有汇兑损益缺口（比 P2P P1-MA2-002 更严重）**：`flow-overview.md §4.3 多币种处理` + `returns.md §应收冲减` + `ar-ap-reconciliation.md` 隐含的 **收款核销汇兑损益**（应收按开票汇率，收款按收款汇率，差额 = 汇兑损益 → 6051 科目）**完全未实现**：
  - `SalAcctDocProvider.RECEIPT` 分支只生成 `借 1002 银行存款 / 贷 1131 应收`（两行同金额），**无 6051 汇兑损益科目插平**。
  - `ErpFinArApItemGenerator` 不计算汇兑损益；`ErpFinReconciliationBizModel` 核销按 `openAmountFunctional` 多对多匹配，**未对外币 AR 项与外币 RECEIPT 项的汇率差做 plug**。
  - 当前实现假设 invoice 与 receipt **同币种同汇率**；多币种 O2C 链路在 GL 层 + 核销层均**不会产生汇兑损益凭证**。
  - **唯一已实现的汇兑损益路径**：`ExchangeRevaluationService`（期末外币 AR/AP/银行存款重估，生成 `EXCHANGE_GAIN_LOSS(130)` 凭证）+ `NotesReceivableAcctDocProvider`（票据贴现 plug）。收款核销时汇兑损益属于"业务时点确认"vs 期末重估属于"期末调整"——两者不互相替代。
- **E2E 验证空白**：`TestErpSalOrderToCashEnd:660-661,697-698` + `TestErpSalReturnRefundEndToEnd:573-574,609-610,631-632` 均为单币种场景（`setExchangeRate(BigDecimal.ONE)`，source=functional）。owner doc `posting.md §多币种处理` 契约在 O2C GL 层与核销层的落实**无测试证据**。
- **严重性裁定**：**非 P0**——单币种下 source=functional，无错误；多币种为「未验证 + 部分未实现路径」而非「已证错误」。但汇兑损益完全缺失比 P2P 的「未验证」更严重一档：P2P 是「不知道是否正确折算」，O2C 是「确定没有汇兑损益 plug」。归 MR1 裁决（补多币种 O2C E2E + 核实引擎折算路径 + 在 RECEIPT 过账 + 核销环节补 6051 汇兑损益 plug + 必要时 `VoucherFact` 增 amountSource/amountFunctional 双字段）。

→ 登记 `P1-MA2-009`（见 §4）。

### 2.6 收入确认时点与收入成本配比 → ✅ PASS（带 P2 跨月配比 owner doc 漏述）

**裁决：PASS（带 P2 — 出库即成本结转 + 期末配比模型，owner doc 漏述跨月场景）**

`InvAcctDocProvider.SALES_OUTPUT`（出库 DONE 触发）+ `SalAcctDocProvider.AR_INVOICE`（发票审核触发）过账时点与金额配比逐路径核实：

- **SALES_OUTPUT（出库 → 结转成本）**：`InvPostingDispatcher.dispatchIfApplicable:172` `case OUTGOING → SALES_OUTPUT` → `InvAcctDocProvider.createFacts:81-86` `借 6401 主营业务成本 / 贷 1401 库存商品`（金额取 `KEY_TOTAL_COST`）。对齐 `flow-overview.md §L3 业务类型映射 销售出库 SALES_OUTPUT 借：结转成本 / 贷：存货`。✅
- **AR_INVOICE（发票 → 确认收入）**：`SalInvoicePostingDispatcher.buildEvent:71-90`（AR_INVOICE）→ `SalAcctDocProvider.createFacts:75-81` `借 1131 应收(TOTAL_AMOUNT_WITH_TAX) / 贷 6001 收入(TOTAL_AMOUNT) / 贷 2221 销项税(TOTAL_TAX_AMOUNT)`。对齐 `flow-overview.md §L3 销售发票 AR_INVOICE 借：应收 / 贷：收入 / 贷：销项税`。✅
- **时序正确性**：出库即结转成本（delivery.approve → triggerOutgoingMove → move DONE → SALES_OUTPUT 凭证），开票再确认收入（invoice.approve → AR_INVOICE 凭证）。**每张凭证自身借贷平衡**（SALES_OUTPUT 借 6401 = 贷 1401；AR_INVOICE 借 1131 = 贷 6001 + 贷 2221），无借贷失衡。✅
- **配比原则（matching principle）**：当前实现是"出库即成本结转"（period costing / streamlined costing）——出库时立即将存货成本转入 COGS，收入在开票时确认。**月度层面配比经期末结账完成**（`flow-overview.md §L4 期末结账 结转损益`：收入→本年利润，费用→本年利润）。这是主流 ERP 实践（与 P2P GRNI 同性质），不在出库时严格按订单匹配收入与成本。
- **SALES_RETURN 反向配比**：`SalAcctDocProvider.SALES_RETURN` `借 1401 / 贷 6401`（反向 SALES_OUTPUT）正确冲减成本——但**不冲收入/应收 GL**（见 §2.4 P2-MA2-011）。在 credit-memo 模型下，收入冲减经辅助账层 openAmount 完成，GL 层收入科目余额不变（直到期末结账）。

**owner doc drift（P2）**：
- `P2-MA2-015`：`flow-overview.md §2.2 + §L3 + §L4` 描述出库（SALES_OUTPUT）与开票（AR_INVOICE）两阶段，但**未显式声明**：(a) 跨月出库-开票场景下成本在 X 月确认、收入在 X+1 月确认时，月度毛利不配比（需期末结账摊平）；(b) 已开票退货 GL 层不冲收入/应收，仅期末通过辅助账 credit memo + 期末结账完成。owner doc §八「流程设计特点」可补注。watch-only，MR1 顺手在 owner doc 标注"期间配比经期末结账完成"。

---

## 3. 多维审计裁决（每维至少一句）

| # | 维度 | 裁决 | 证据/说明 |
|---|------|------|-----------|
| 1 | **需求正确性** | ⚠️ P2 | `flow-overview.md §2.2 关键控制点 发票金额超过订单金额需审批` 在 `ErpSalInvoiceProcessor.validateBusinessRulesForApprove:189-192` **无订单-发票金额比对守卫**——只校验客户启用 + 信用冻结（config-gated），无 invoiceTotal vs orderTotal 校验。「承诺但无证据」控制点。→ `P2-MA2-010`。其余控制点（订单信用额度 / 出库可用量 / 收款核销发票状态）均有实现证据。 |
| 2 | **owner-doc 对齐** | ⚠️ P2 | `returns.md §红字发票处理` 与实现偏离（P2-MA2-011）；`flow-overview.md §2.2 信用额度检查` 漏述出库/发票信用冻结扩展点（P2-MA2-012）；`flow-overview.md §2.2 收款核销按订单/发票维度` 实现仅按发票维度（P2-MA2-013）；`state-machine.md` + `ar-ap-reconciliation.md` 辅助账 openAmount 生命周期实现符合（头+行结构、settledAmount/openAmount/status 回写一致）。 |
| 3 | **业务正确性 — 可用量校验与库存扣减** | ✅ PASS | 见 §2.1。跨域可用量裁决链完整，负库存配置生效，错误码传播无吞异常。 |
| 4 | **业务正确性 — 信用额度检查** | ✅ PASS（P2） | 见 §2.2。订单审核信用检查 + AR 辅助账余额联动 + 三级策略齐全；P2-MA2-012 owner doc 漏述扩展点。 |
| 5 | **业务正确性 — 应收 openAmount 生命周期** | ✅ PASS | 见 §2.3。AR_INVOICE/RECEIPT 生成 + 核销回减 + 部分核销 + 反向核销完整。 |
| 6 | **业务正确性 — 退货反向链** | ✅ PASS（P2） | 见 §2.4。SALES_RETURN 红字过账 + 负 credit memo + 反向 ReceiptLine 正确；owner doc 偏离 P2-MA2-011。 |
| 7 | **业务正确性 — 多币种 O2C 与汇兑损益** | ❌ FAIL（P1） | 见 §2.5。`P1-MA2-009` 本位币凭证路径未验证 + 收款核销汇兑损益完全未实现（比 P2P P1-MA2-002 更严重）。 |
| 8 | **业务正确性 — 收入确认时点与收入成本配比** | ✅ PASS（P2） | 见 §2.6。SALES_OUTPUT + AR_INVOICE 凭证方向与时序正确，期间配比经期末结账完成；P2-MA2-015 owner doc 漏述跨月场景。 |
| 9 | **架构或边界影响** | ✅ PASS | P1-MA1-022 复核：`ErpSalOrderProcessor.resolveBudgetSubjectId:377,382`（daoFor ErpMdSubject）+ `resolvePeriodId:389`（daoFor ErpFinAccountingPeriod）——**纯只读查询**（按 code/日期取 ID 用于承付科目/期间解析），无写、无状态变更、无脏读。O2C 主链行为正确。维持治理层 finding（MR1 迁移至 I*Biz 便捷只读方法），**不升级**。 |
| 10 | **验证充分性** | ⚠️ | E2E 断言覆盖 posted/receivedStatus/voucher 回链/部分核销/反向冲销/辅助账 openAmount→0/退货负 credit memo + cancelOnReverse，强度高于 P2P。但**未覆盖**：多币种（单币种 only，P1-MA2-009）、汇兑损益（功能未实现）、收入 GL 红字（只断言成本/存货侧，P2-MA2-011）、订单维度核销（功能未实现，P2-MA2-013）。对每个验收断言「如果它假了，我怎么知道」：posted/receivedStatus/openAmount 有独立证据；多币种/汇兑损益/订单维度核销**无独立证明策略**（因实现/测试双缺）。 |
| 11 | **回归风险** | ⚠️ P1/P2 | 「仅偶然通过狭窄验证」代码：①可用量校验仅在单仓库 + 单批次场景测试通过，多仓库/多批并发出库未验证（→ A2.17）；②多币种 O2C 完全无验证（P1-MA2-009）；③汇兑损益仅在票据贴现 + 期末重估验证，收款核销时无（P1-MA2-009）；④收入成本配比仅在出库即开票场景隐式成立，跨月出库-开票未验证（P2-MA2-015）；⑤`ReceiptSettler.settle` 并发核销无验证（P2-MA2-014，A2.17）。 |
| 12 | **路由和技能选择正确性** | ✅ PASS | O2C 实现任务路由正确（审计工作 → multi-dimensional-audit-prompt skill；实现期审批三段 → Facade+Processor+IErpFinAcctDocProvider；核销 → ReconciliationSettler+ReceiptSettler 双层；可用量校验跨域 → inventory 域 ErpInvStockMoveProcessor）。技能选择与工作类型匹配。 |
| 13 | **待办或自主权策略漂移** | ✅ PASS | 审计范围（O2C 全链 6 控制点 + MA1 finding 复核 + 并发敏感点交接）与产出（本报告 + arm-index 登记）边界一致；A2.1/A2.3/A2.4/A2.5/A2.9/A2.17 显式 Non-Goal，无静默扩缩范围；P1 不属降级（按设计进 MR1）。 |

**反窄化自检**：本审计覆盖 13 维（≥7 维要求 + 6 O2C 特定控制点），每维至少一句裁决，未单维过度深挖。✅

---

## 4. Finding 分级汇总

### P0（即时通道）

**无。** 核实路径：无凭证借贷失衡（每张凭证自身平衡：SALES_OUTPUT 借 6401=贷 1401；AR_INVOICE 借 1131=贷 6001+2221；RECEIPT 借 1002=贷 1131；SALES_RETURN 借 1401=贷 6401）、无状态机非法转移（所有迁移经 `validateTransitionFor*` 守卫）、无跨域写绕过审批管道致脏数据、无数据不一致（辅助账层 openAmount 回减正确 + PartnerBalanceUpdater 重聚合）。多币种汇兑损益缺失属 P1「未实现路径」，不构成 P0「已证错误」（单币种下 source=functional，无错误）。

### P1（登记 arm-index §P1，目标 MR1）

| Finding ID | 域 | 描述 | 目标 MR |
|-----------|---|------|--------|
| `P1-MA2-009` | sales+finance | **多币种 O2C 端到端 + 收款核销汇兑损益未实现**：(a) `VoucherFact` 仅单一 `amount` 字段（无 amountSource/amountFunctional 分离），`SalAcctDocProvider.createFacts` 将 source-currency TOTAL_* 直接写入 fact.amount，本位币折算依赖过账引擎装配 ErpFinVoucherLine 时按 PostingEvent.exchangeRate 转换——该路径在 O2C 链无 E2E 证据；(b) **收款核销汇兑损益完全未实现**：`SalAcctDocProvider.RECEIPT` 只生成 借银行存款/贷应收（同金额），无 6051 汇兑损益科目插平；`ErpFinArApItemGenerator` + `ErpFinReconciliationBizModel` 不计算外币 AR 与外币 RECEIPT 的汇率差 plug。当前实现假设 invoice 与 receipt 同币种同汇率。E2E 测试（TestErpSalOrderToCashEnd/TestErpSalReturnRefundEndToEnd）均单币种（setExchangeRate(BigDecimal.ONE)）。owner doc `posting.md §多币种处理` + `flow-overview.md §4.3` 契约在 O2C GL 层与核销层落实无测试证据 + 部分未实现。修复方式：MR1 补多币种 O2C E2E + 核实引擎折算路径 + 在 RECEIPT 过账与核销环节补 6051 汇兑损益 plug + 必要时 VoucherFact 增 amountSource/amountFunctional 双字段（与 P2P `P1-MA2-002` 一并裁决）。 | MR1 |

### P2（watch-only，待 MR 顺手收敛）

| Finding ID | 域 | 描述 | 处置 |
|-----------|---|------|------|
| `P2-MA2-010` | sales | `flow-overview.md §2.2 关键控制点 发票金额超过订单金额需审批` 在 `ErpSalInvoiceProcessor.validateBusinessRulesForApprove` **无订单-发票金额比对守卫**（只校验客户启用 + 信用冻结）。「承诺但无证据」控制点（与 P2P `P2-MA2-007` 订单审核价格锁同型）。 | watch-only，MR1 顺手补 approve 后置金额守卫或更新 owner doc 标注「审核信任前置订单价格」 |
| `P2-MA2-011` | docs+sales | `returns.md §红字发票处理` 描述「已开票退货→生成红字 ErpSalInvoice（金额取负）→ 红字凭证」流程；实现以 SALES_RETURN 过账 + 负 ArApItem credit memo 替代（功能等价于 AR 余额回减，但 GL 冲成本/存货侧非收入/应收侧）。owner doc 与实现偏离（与 P2P `P2-MA2-006` 同型对称偏离）。 | watch-only，MR1 更新 owner doc 反映 credit-memo-via-return 实现 |
| `P2-MA2-012` | docs（sales） | `flow-overview.md §2.2 关键控制点 销售订单审核时检查客户信用额度` 仅描述订单审核环节；实现已扩展至出库/发票审核（`CreditLimitChecker.checkCreditHold`，config-gated `erp-sal.credit-check-on-delivery/on-invoice` 默认 false）+ 三级策略（SOFT_WARNING/HARD_BLOCK/SPECIAL_APPROVAL）+ AR 辅助账余额联动。owner doc 漏述该扩展点（实现增强无文档对齐）。 | watch-only，MR1 顺手更新 owner doc 反映信用冻结扩展点 + 三级策略 |
| `P2-MA2-013` | docs+sales | `flow-overview.md §2.2 关键控制点 收款核销按订单/发票维度` 描述订单 + 发票双维度；实现 `SettlementAllocation`（master-data）+ `ReceiptSettler` 仅按 **invoiceId** 维度核销，订单维度（receipt prepayment against order before invoice）未实现。owner doc 与实现偏离。 | watch-only，MR1 裁决：实现订单维度核销或更新 owner doc 标注「本期仅发票维度，预收款归独立 successor」 |
| `P2-MA2-014` | sales | `ReceiptSettler.settle:55-111` 「读 invoiceBalance→写 ReceiptLine→recompute」无悲观/乐观锁，并发核销同一发票可双读双写过收；`recomputeInvoiceReceived:161-177` 事后聚合不能阻止中间态过收。与 P2P `P2-MA2-008 PaymentSettler` 同型对称并发缺口。 | watch-only，归 A2.17 并发与乐观锁系统性审计 |
| `P2-MA2-015` | docs（sales+finance） | `flow-overview.md §2.2 + §L3 + §L4` 描述出库（SALES_OUTPUT）与开票（AR_INVOICE）两阶段，但**未显式声明**：(a) 跨月出库-开票（成本在 X 月，收入在 X+1 月）月度毛利不配比，需期末结账摊平；(b) 已开票退货 GL 层不冲收入/应收，仅期末通过辅助账 credit memo + 期末结账完成。owner doc §八「流程设计特点」可补注期间配比语义。 | watch-only，MR1 顺手在 owner doc 标注「期间配比经期末结账完成」 |

---

## 5. MA1 finding 运行时行为影响复核

| Finding ID | 原裁决 | 运行时行为复核（本审计） | 终态 |
|-----------|--------|------------------------|------|
| `P1-MA1-022` | 跨域只读 `daoFor(ErpMd*/ErpFin*)`（sales Processor `ErpSalOrderProcessor:377,389` ErpMdSubject/ErpFinAccountingPeriod），待 MR1 | **不影响业务正确性**——`resolveBudgetSubjectId:377,382`（daoFor ErpMdSubject 按 code 取 subjectId）+ `resolvePeriodId:389`（daoFor ErpFinAccountingPeriod 按 startDate/endDate 取 periodId）均为**纯只读查询**，返回值用于承付 hook（config-gated `erp-fin.budget-commitment-enabled` 默认 false，关闭时根本不进入此路径）。无写、无状态变更、无脏读。O2C 主链行为正确（信用检查经 I*Biz `IErpMdPartnerBiz` + `IErpFinArApItemBiz`，库存联动经 `IErpInvStockMoveBiz`）。维持治理层 finding（MR1 迁移至 I*Biz 便捷只读方法），**不升级**。 | 治理 only，无运行时影响 |
| `UC-SAL-10` 并发扣批次缺口 | sales 出库并发扣批次 lost-update 风险（`use-case-implementation-audit` 标记），归 A2.17 | **乐观锁基础具备**——`ErpInvStockBalance`（`module-inventory/model/app-erp-inventory.orm.xml:367-389`）`version` 列存在（propId=17，stdSqlType=INTEGER，mandatory=true，defaultValue=0），`versionProp="version"`（line 369）。但 `ErpInvStockMoveProcessor.applyReservation:237-253` 走 `bookkeeper.upsertBalance` → `balance.setReservedQuantity(...)` → `balanceDao.saveOrUpdateEntity(balance)` 路径——平台乐观锁触发取决于 ORM session 自动 dirty 检测；并发同 (materialId, warehouseId, locationId) 双读双写 reservedQuantity 仍存在 lost-update 风险（version 自增但两次 setReservedQuantity 基于同版本读）。**系统性并发正确性裁决归 A2.17**（独立 skill `open-ended-audit-prompt.md`）。本审计仅标注观察点。 | 并发 only，归 A2.17 |
| `UC-INV-08` 乐观锁缺口 | 库存可用量校验的乐观锁缺口，归 A2.17 | **同上**：`ErpInvStockMoveProcessor.validateAvailable:215-235` 读 `balance.getAvailableQuantity()` 与 `line.getQuantity()` 比较，无悲观/乐观锁；并发出库同一余额可双读双扣。归 A2.17。 | 并发 only，归 A2.17 |
| `P0-MA1-021` | inventory `CostAdjustmentPostingDispatcher` 跨模块写 `ErpFinVoucher`，已闭包（plan 2026-07-27-1430-1） | **确认未回退**——`SalReturnPostingDispatcher.reverse` / `SalInvoicePostingDispatcher.reverse` / `SalReceiptPostingDispatcher.reverse` 经 `SalPostingExecutor` → Facade `IErpFinVoucherBiz.reverse()`（非直接跨模块写 `ErpFinVoucher.isReversed`）；`ErpSalReturnProcessor.ensureReversed:320-334` + `ErpSalInvoiceProcessor.doReverseApprove:238-250` + `ErpSalReceiptProcessor.doReverseApprove:212-224` 库存/凭证红冲经 I*Biz 边界。O2C 链无 inventory/finance 红冲绕过 I*Biz 复现。 | 已闭包，无回退 |

---

## 6. 并发敏感点交接 A2.17（非裁决）

> 本节为观察点交接，归 A2.17 系统性并发与乐观锁审计（独立 skill `open-ended-audit-prompt.md`）。本审计仅标注，不裁决。

| 观察点 | 位置 | 风险描述 | 交接 |
|--------|------|---------|------|
| O2C 并发扣批次 | `ErpSalDeliveryProcessor.triggerOutgoingMove:256` → `IErpInvStockMoveBiz.generateMove` → `ErpInvStockMoveProcessor.{validateAvailable:215, applyReservation:237, releaseReservation:255}` | 并发出库同一 (materialId, warehouseId, locationId) 双读 balance.availableQuantity/reservedQuantity → 双写 setReservedQuantity，基于同版本读，存在 lost-update 风险（version 自增不能阻止两次写都基于陈旧读） | → A2.17 |
| O2C 收款核销并发 | `ReceiptSettler.settle:55-111`（读 invoiceBalance→写 ReceiptLine）+ `recomputeInvoiceReceived:161-177`（事后聚合） | 并发核销同一发票可双读 invoiceBalance 双写 ReceiptLine 致过收；recompute 事后聚合不能阻止中间态过收（与 P2P `P2-MA2-008` 同型对称）→ `P2-MA2-014` | → A2.17 |
| O2C 信用检查并发 | `CreditLimitChecker.check:109-130`（读 partner.creditLimit + sumOutstanding → 比较） | 并发订单审核同一客户可双读 outstanding 双通过信用检查（信用额度瞬时超额）；非阻塞业务缺陷（信用控制为风控手段，非强一致性约束） | → A2.17（低优先级） |
| `ErpInvStockBalance.version` 乐观锁基础 | `module-inventory/model/app-erp-inventory.orm.xml:367-389` | 列存在 + versionProp 配置正确（乐观锁基础具备）；是否在 saveOrUpdateEntity 路径触发自增取决于 ORM session 配置，需 A2.17 在运行时验证 | → A2.17 |

---

## 7. 残留风险与 successor

- **A2.17 并发**：O2C 并发扣批次 / 收款核销 / 信用检查并发敏感点（见 §6）均归 A2.17 系统性并发与乐观锁审计。本审计仅标注观察点。
- **A2.4 库存核算一致性**：SALES_OUTPUT 凭证方向（贷 1401 存货/借 6401 COGS）正确，但存货成本核算方法（移动加权/FIFO/批次）正确性归 A2.4。
- **A2.3 期末结账**：O2C 的应收辅助账（AR_INVOICE 生成的 ErpFinArApItem OPEN 项）期末未核销项是 A2.3 期末门禁的前置输入；本审计确认其生成正确（AR_INVOICE → DIRECTION_RECEIVABLE 正项，openAmount=amountFunctional），不审计期末门禁。期末外币 AR 重估（`ExchangeRevaluationService`）属 A2.3 范围。
- **A2.5c/A2.9 状态机**：本审计覆盖 O2C 链路中状态转移的业务正确性（迁移守卫齐全 + 反审核目标态 REJECTED + 红冲硬前置），但 sales 域状态机的系统性可达性审查归 A2.9，finance AR/AP 核销状态机归 A2.5c。

---

## 8. scope matrix §2.2 终态标记（同步至 `audit-remediation-scope-and-dimension-matrix.md`）

「业财端到端」行 finance/sales 列：`❓` → **`⚠️(P1)`**（O2C 链路组件齐备、E2E 覆盖黄金路径+反向冲销+财务正式核销，零 P0；1 项 P1 待 MR1：多币种 O2C + 收款核销汇兑损益 P1-MA2-009；6 项 P2 watch-only；MA1 finding 运行时复核无升级；并发敏感点交接 A2.17）。finance 列保持 `⚠️(P1)`（与 P2P 共担 P1-MA2-009 汇兑损益多币种裁决）。

---

> **审计结论**：O2C 全链**passes multi-dimensional audit**——零 P0（无凭证失衡/状态机非法转移/跨域脏数据/数据不一致），链路组件齐备（域级 + 财务核销双层），E2E 覆盖黄金路径、反向冲销、辅助账 openAmount 生命周期、退货负 credit memo。1 项 P1（多币种 O2C + 收款核销汇兑损益 P1-MA2-009）登记入 arm-index 待 MR1；6 项 P2 watch-only；MA1 finding（P1-MA1-022 / UC-SAL-10 / UC-INV-08 / P0-MA1-021）运行时影响复核无升级；并发敏感点交接 A2.17。
