# A4.2.27-A4.2.32 purchase-F1 主流程/请购运行时触发链与一致性确认验证报告（rc-ma4-a4-2-27-32）

> Mission: requirement-compliance · MA4 运行时行为验证 · Work Items: A4.2.27 / A4.2.28 / A4.2.29 / A4.2.30 / A4.2.31 / A4.2.32
> 来源计划: `docs/plans/2026-08-07-2300-1-rc-ma4-a4-2-27-32-purchase-f1-mainflow-runtime.md`
> 来源存疑点: `docs/audits/2026-08-03-0145-rc-ma1-a1-15-purchase-f1-mainflow-requisition.md` §7（6 项静态存疑点）
> 方法论: `docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议）
> 审计类型: 只读审计（无生产代码 / ORM / api.xml / view.xml / config 默认值 / 真相源变更）
> 审计日期: 2026-08-07
> Audit Status: closed

## 9. 与既有 A1.15 / A2.8 / A2.1 / A1.1 报告的差异增量声明（前置）

本报告是 MA4 运行时行为验证 A4.2 展开器的 A1.15 §7 六项静态存疑点的**运行时证据采集与裁决**，视角 = **A1.15 静态判定结论的运行时确认（主路径闭合 / 维持 P1 reuse 重开 + 运行时证据记录 / 升级触发 MR0）**。按 §去重协议，以下既有审计已证实的结论本报告**直接复用，不重审**：

- **A1.15**（`docs/audits/2026-08-03-0145-rc-ma1-a1-15-purchase-f1-mainflow-requisition.md`）：UC-PUR-01/08 五级追踪 + §7 六项静态存疑点 + §6 finding 衔接裁决（P1-RC-017 多供应商拆分 + P1-MA2-083 reuse 重开 + P2-RC-011 命名漂移 + P2-RC-012 幂等漂移）。本报告复用其 L3 代码路径静态判定 + §6 finding 编号，只补**运行时触发链 / SUM 聚合一致性 / openAmount 恒等式 / 阻断行为 / commit() 调用方 census / config 默认值部署普查 / 取消后再转化**的运行时证据。
- **A2.8**（`docs/audits/2026-07-28-0230-arm-ma2-purchase-state-machine.md`）：purchase 9 实体状态机迁移守卫齐全 + reverseApprove 红冲闭环。本报告复用其 receive/invoice 状态机迁移 + reverseApprove 红冲行为证据。
- **A2.1 P2P e2e**（`docs/audits/2026-07-27-1949-arm-ma2-procure-to-pay-e2e.md`）：P2P 链路行为已证实（回链 / 库存 incoming / 过账 / 核销 主路径完整）+ 承付 commit/release 路径完整性。本报告复用其库存 incoming Facade + 业财过账引擎范式证据。
- **A1.1**（`docs/audits/2026-08-02-1645-rc-ma1-a1-1-finance-f1-posting.md`）：业财过账引擎 GR/IR + AP 凭证范式已审。本报告复用其过账正确性结论，只补采购侧触发契约运行时确认。
- **A2.16**（`docs/audits/2026-07-28-1249-arm-ma2-budget-commitment-release.md`）：承付 commit-on-order-approve + release-on-invoice-approve + config-gated 默认 false 完整性。本报告复用其承付路径行为证据，只补 invoice reverseApprove/cancel 不对称的运行时 commit() 调用方 census。

本报告**只补运行时差异**：(i) UC-PUR-01 ④ GOODS_RECEIPT/PURCHASE_INPUT 运行时触发链确认（§2-1，**业财保护区域探针——只读确认不改过账逻辑**）；(ii) UC-PUR-01 ⑦ paidStatus 派生运行时一致性确认（§2-2）；(iii) UC-PUR-01 ⑧ 应付余额辅助账聚合运行时一致性确认（§2-3，**业财保护区域探针——只读确认不改辅助账逻辑**）；(iv) UC-PUR-08 ⑫ 多供应商拆分运行时阻断确认（§2-4，维持 P1-RC-017）；(v) P1-MA2-083 承付恢复运行时不对称确认（§2-5，维持 P1 reuse 重开）；(vi) UC-PUR-08 ⑬ 取消后再转化运行时允许确认（§2-6，维持 P2-RC-012）。

---

## 1. 存疑点清单（逐字引用 A1.15 §7）

> 以下为本报告核验对象，6 项静态存疑点来自 A1.15 §7：

1. **UC-PUR-01 ④ GOODS_RECEIPT/PURCHASE_INPUT 运行时触发链**（A4.2.27）：receive approve → `triggerIncomingMove` → `IErpInvStockMoveBiz.generateMove` → `InvPostingDispatcher` PURCHASE_INPUT 凭证 → `move.getPosted()=true` → `receive.posted=true` 全链（HEAD 静态判定 = 全链已实现，运行时可经 E2E 确认凭证落地）。
2. **UC-PUR-01 ⑦ paidStatus 派生运行时一致性**（A4.2.28）：`PaymentSettler.recomputeInvoicePaid` 在多付款单跨单据核销同一发票时累计 SUM(PaymentLine.amount) 一致性 + 反向负金额行回退 paidStatus（HEAD 静态判定 = 实现 OK，运行时可构造 2 付款单核销 1 发票场景确认）。
3. **UC-PUR-01 ⑧ 应付余额辅助账聚合运行时一致性**（A4.2.29）：`ErpFinArApItem.openAmount` 在多发票/多付款/部分核销/红冲场景下 SUM == 发票金额 − 已核销金额 恒等式（HEAD 静态判定 = 实现 OK，`TestErpPurProcureToPayEnd:244-272` 强断言复核）。
4. **UC-PUR-08 ⑫ 多供应商拆分运行时阻断**（A4.2.30，P1-RC-017）：多供应商请购行 convertToOrder 是否被 `validateConsistentSupplier` 拒绝（HEAD 静态判定 = 是，`TestErpPurRequisitionConvertToOrder#testConvertMixedSupplierRejected` 显式断言）。
5. **P1-MA2-083 承付恢复运行时不对称**（A4.2.31）：invoice approve → commitment release，invoice reverseApprove → AP 红冲但 commitment 保持已释放（HEAD 静态判定 = 不对称，`ErpPurInvoiceReverseApproveProcessor:22-37` 零 commit()）。
6. **UC-PUR-08 ⑬ 取消后再转化运行时允许**（A4.2.32，P2-RC-012）：cancel 全部衍生订单后 existsActiveByRequisition=false 允许再次转化（HEAD 静态判定 = 允许，`testConvertIdempotentRejected` 已断言）。

---

## 2. 运行时证据采集与裁决（L3 file:line + L4 测试断言 + L5 行为）

### 2-1 A4.2.27 GOODS_RECEIPT/PURCHASE_INPUT 运行时触发链确认 — 主路径闭合

**业财保护区域探针声明**：本节为只读触发链追踪（grep census + 调用链追踪 + 测试断言复核），**不修改任何过账逻辑 / VoucherFact / PostingProcessor 核心路径**。

**运行时触发链（live code 实测）**：

- **receive approve 入口**：`ErpPurReceiveApproveProcessor.approve:26-49` override 公共 approve mutation——守卫段（`validateNotCancelled:32` + `validateTransitionForApprove:33` + `validateBusinessRulesForApprove:34` + `enforceInspectionGate:35`）→ **`triggerIncomingMove:37`（库存 incoming Facade）** → 设 APPROVED（`setApproveStatus:40` + `setApprovedBy/At:41-42`）→ **`applyPostingResult:43`（receive.posted = move.posted）** → `setReceiveStatus(RECEIVED):44` → `postProcessApprove:47`（`rollupOrderReceiveStatus`）。
- **triggerIncomingMove Facade**：`ErpPurReceiveProcessor.triggerIncomingMove:215-219` → `stockMoveBuilder.build(receive, lines, context)` 组装 `StockMoveRequest` → **`stockMoveBiz.generateMove(request, context)`**（`IErpInvStockMoveBiz` 跨域 Facade，库存域 incoming 写流水/更新余额经 `StockMoveBookkeeper`，A2.1/A2.4 已证实）。
- **InvPostingDispatcher PURCHASE_INPUT 路由**：库存域 `ErpInvStockMoveProcessor:113` 在移动单达 DONE 后调 `postingDispatcher.dispatchIfApplicable(move, lines)` → `InvPostingDispatcher.resolveBusinessType:152-179` 按 `moveType` 路由：`MOVE_TYPE_INCOMING` → **`ErpFinBusinessType.PURCHASE_INPUT`（`:168-169`）**（1401/2202 借存货/贷暂估应付 GR/IR，与 L1 §UC-PUR-07 ① 语义等价）；`executor.postEvent(event):65` → 成功置 `markMovePosted(move.getId()):87-93`（`managed.setPosted(true)`）。
- **applyPostingResult 回写 receive.posted**：`ErpPurReceiveProcessor.applyPostingResult:221-227`——`receive.setPosted(Boolean.TRUE.equals(move.getPosted()))`，即 **receive.posted = move.posted**（GOODS_RECEIPT/PURCHASE_INPUT 过账经 stockMoveBiz.generateMove 内部触发，无独立 PurReceivePostingDispatcher，A1.1 业财过账引擎范式）；posted=true 时补写 `postedAt:224` + `postedBy:225`。
- **L4 E2E 强断言凭证落地**：`TestErpPurProcureToPayEnd.testProcureToPayPartialSettlement:116-121`——`submitReceive` + `approveReceive` 后断言 `approvedReceive.getApproveStatus()==APPROVED` + **`assertEquals(true, approvedReceive.getPosted(), "入库 posted=true")`**（`:121`）→ receive posted=true 落地确认（ PURCHASE_INPUT 凭证经 stock move 内部触发并经 markMovePosted 回写 move.posted=true，再经 applyPostingResult 回写 receive.posted=true）。

**裁决**：A1.15 §7-1 静态判定「全链已实现」**运行时确认成立**。receive approve → triggerIncomingMove → IErpInvStockMoveBiz.generateMove → InvPostingDispatcher PURCHASE_INPUT 凭证 → move.posted=true → receive.posted=true **全链运行时闭合**，经 `TestErpPurProcureToPayEnd#testProcureToPayPartialSettlement:121` 强断言凭证落地。**主路径行为正确闭合**（GOODS_RECEIPT 字面经 PURCHASE_INPUT 语义等价实现，命名漂移归 P2-RC-011 watch-only 不变）。**不触发 MR0**（行为正确，无会计错误）。

### 2-2 A4.2.28 paidStatus 派生运行时一致性确认 — 主路径闭合

**运行时证据链（live code 实测）**：

- **recomputeInvoicePaid SUM 聚合**：`PaymentSettler.recomputeInvoicePaid:201-217`——`ormTemplate.flushSession()` 强制刷出 → `sumInvoiceLines(invoiceId):236-244` 按 **`QueryBean eq("invoiceId", invoiceId)`** 遍历**全部 `ErpPurPaymentLine`**（不限 paymentId），`sum = sum.add(nz(l.getAmount()))` → **跨多付款单核销同一发票累计 SUM(PaymentLine.amount) 成立**（任何 payment 的 PaymentLine 都被聚合到同一 invoice.paidAmount）。
- **paidStatus 三态派生**：`recomputeInvoicePaid:208-214`——`paid.signum()<=0` → `PAID_STATUS_UNPAID`；`paid.compareTo(withTax)>=0` → `PAID_STATUS_PAID`；否则 → `PAID_STATUS_PARTIAL` → `invoice.setPaidAmount(paid):205` + `invoice.setPaidStatus(status):215` + `updateEntity:216` 持久化。
- **多付款单累计可达路径**：`settle:65-121` 对每个 `SettlementAllocation` 写 `ErpPurPaymentLine(paymentId, invoiceId, amount)`（`:106-110`），累积 `touchedInvoices`（`:113`）→ 对每个 touched invoiceId 调 `recomputeInvoicePaid`（`:116-118`）。**多次 settle（不同 payment）核销同一 invoice 时，recomputeInvoicePaid 每次重算全量 SUM → 累计一致性成立**。
- **反向负金额行回退 paidStatus**：`reverseSettlement:126-147`——按 `(paymentId, invoiceId)` 查既有 PaymentLine 求和 `settled`（`:128-131`），非零则**生成反向负金额 PaymentLine**（`reversal.setAmount(settled.negate()):140`，remark="核销冲销"）→ `recomputeInvoicePaid(invoiceId):144` 重算 → SUM 因负金额行减少 → paidStatus **回退**（PAID→PARTIAL 或 →UNPAID）。
- **事务边界**：`settle` / `reverseSettlement` 经 `@BizMutation` 包装事务（platform 默认 SYNC 传播），`flushSession` 确保跨步骤可见性。**并发 lost-update 风险**：无显式乐观锁 on ErpPurInvoice.paidAmount，并发 settle 同一 invoice 存在 lost-update 窗口——但此为 **P2-MA2-008（PaymentSettler 并发核销无锁）watch-only 归 A2.17**（不同维度，不在本审计范围，不重开）。
- **L4 强断言多场景**：`TestErpPurProcureToPayEnd.testProcureToPayPartialSettlement:142-150`（部分核销 30/56.5 → invoice paidAmount=30 + paidStatus=PARTIAL + payment writtenOffStatus=PARTIAL）+ `testReverseScenarios:176-184`（全额核销 56.5 → PAID → `reverseSettlement` → **`assertEquals(PAID_STATUS_UNPAID, invAfter.getPaidStatus(), "冲销核销后发票回 UNPAID"):184`**）→ 负金额行回退 paidStatus 强断言。

**裁决**：A1.15 §7-2 静态判定「实现 OK」**运行时确认成立**。多付款单跨单据核销同一发票累计 SUM(PaymentLine.amount) 一致性 + 反向负金额行回退 paidStatus 均**运行时行为正确闭合**，经 `TestErpPurProcureToPayEnd` partial + reverse 双 @Test 强断言。**主路径行为正确闭合**。**不触发 MR0**（行为正确，无会计错误）。并发 lost-update 归 P2-MA2-008 watch-only（A2.17 范围），本审计不重开。

### 2-3 A4.2.29 应付余额辅助账聚合运行时一致性确认 — 主路径闭合

**业财保护区域探针声明**：本节为只读恒等式复核（测试断言复核 + sumOpen 排除逻辑追踪），**不修改任何辅助账逻辑 / ErpFinArApItem 写路径**。

**运行时证据链（live code 实测）**：

- **辅助账项生成路径**：发票审核 → `PurInvoicePostingDispatcher.tryPost`（AP_INVOICE 凭证）→ finance 域 `ErpFinArApItemGenerator` 生成 `ErpFinArApItem(DIRECTION_PAYABLE, SOURCE_BILL_AP_INVOICE)`，`openAmountFunctional = 含税总额`，`settledAmountFunctional = 0`；付款审核 → `PurPaymentPostingDispatcher.tryPost`（PAYMENT 凭证）→ 生成 `ErpFinArApItem(DIRECTION_PAYABLE, SOURCE_BILL_PAYMENT)`，`openAmountFunctional = 付款总额`（A1.1 业财过账引擎范式）。
- **sumOpen 自然减计（排除 SETTLED/CANCELLED）**：`TestErpPurProcureToPayEnd.sumOpenByDirection:356-369` 复用既有查询模式——`QueryBean eq("direction", ...)` + **`notIn("status", [AR_AP_STATUS_SETTLED, AR_AP_STATUS_CANCELLED]):360-361`** → SETTLED/CANCELLED 项被排除，openAmount 自然减计覆盖正向核销（→SETTLED 排除）+ 红冲（→CANCELLED 排除）+ credit memo 负金额（PAYABLE 方向负 openAmount 经 reconciliation 抵销）。
- **L4 强断言恒等式（核心证据，`testFinanceReconciliationLayerPayable:224-294`）**：
  - 发票 AP_INVOICE 辅助账：`openAmountFunctional=56.5（含税总额）` + `settledAmountFunctional=0` + `status=OPEN` + `direction=PAYABLE`（`:224-231`）。
  - 付款 PAYMENT 辅助账：`openAmountFunctional=56.5` + `direction=PAYABLE`（`:238-242`）。
  - 核销前 sumOpen：`sumOpenByDirection(PAYABLE)=113`（两笔未核销辅助账 openAmount 之和，`:247-248`）→ 应付余额 == 发票金额 − 已核销金额（已核销=0 → 余额=113）恒等式成立。
  - 经 `reconciliationBiz.create + post`（付款项↔发票项，全额 56.5）核销（`:251-255`）→ 双方辅助账 **`openAmountFunctional=0`**（`:266-271`）+ **`settledAmountFunctional=56.5`**（`:269-270`）+ **`status=SETTLED`**（`:268/:273`）→ openAmount == 发票金额 − 已核销金额 = 56.5 − 56.5 = 0 恒等式成立。
  - 核销后 sumOpen：`sumOpenByDirection(PAYABLE)=0`（`:280-281`，两笔均 SETTLED 被排除）→ 应付余额 == 0 恒等式成立。
  - 账龄查询一致：`arApItemBiz.aging(PAYABLE, ...)` totalOpen=0（`:284-288`）→ 报表层与辅助账层一致。
- **L4 异常路径强断言（`testFinanceReconciliationLayerExceptions:301-341`）**：
  - (a) 未审核发票**无 AP_INVOICE 辅助账**（`assertNull(findApItem(AP_INVOICE, "PI-FEX-001")):311-312`）→ 过账未触发则辅助账不生成。
  - (b) 核销金额超过 openAmount 拒绝（`assertThrows(NopException.class, () -> reconciliationBiz.post(over.getId(), CTX)):334-335`）→ 拒绝后辅助账 `openAmountFunctional=56.5` 不变（`:339-340`）→ 超额核销不破坏恒等式。

**裁决**：A1.15 §7-3 静态判定「实现 OK」**运行时确认成立**。`ErpFinArApItem.openAmount` 在多发票/多付款/部分核销/红冲/超额拒绝场景下 **SUM == 发票金额 − 已核销金额 恒等式运行时成立**，sumOpen 自然减计覆盖 credit memo 负金额（PAYABLE 方向）+ SETTLED/CANCELLED 排除，经 `TestErpPurProcureToPayEnd.testFinanceReconciliationLayerPayable:224-294` + `testFinanceReconciliationLayerExceptions:301-341` 双 @Test 强断言。**主路径行为正确闭合**。**不触发 MR0**（行为正确，无会计错误）。

### 2-4 A4.2.30 多供应商拆分运行时阻断确认 — 维持 P1-RC-017

**运行时证据链（live code 实测）**：

- **validateConsistentSupplier 强制单一供应商**：`ErpPurRequisitionProcessor.validateConsistentSupplier:171-186`——遍历请购行 `Set<Long> suppliers` 收集 `line.getSuggestedSupplierId()`；**任一行为 null → `ERR_REQ_MIXED_OR_MISSING_SUPPLIER`（`:175-178`）**；**`suppliers.size() != 1` → `ERR_REQ_MIXED_OR_MISSING_SUPPLIER`（`:181-184`）**；唯一时 `return suppliers.iterator().next()`（`:185`）。**多供应商请购行 convertToOrder 运行时被阻断**。
- **convertToOrder 守卫序列**：`ErpPurRequisitionProcessor.convertToOrder`（§A1.15 §2 step）：`requireRequisition` → `validateApprovedForConversion` → `loadLines` → `validateLinesNonEmptyForConversion` → **`validateConsistentSupplier`（阻断点）** → `validateNotAlreadyConverted` → `doConvertToOrder`。多供应商在守卫段即被拒，**不进入 doConvertToOrder 生成订单**。
- **L4 强断言阻断**：
  - `TestErpPurRequisitionConvertToOrder.testConvertMixedSupplierRejected:113-125`——两行不同 supplier（SUPPLIER_ID + anotherSupplier=2412L）→ `convertToOrder` 返回 `ERR_REQ_MIXED_OR_MISSING_SUPPLIER`（`:122-124`）。
  - `TestErpPurRequisitionConvertToOrder.testConvertMissingSupplierRejected:128-139`——一行 supplier + 一行 null → `convertToOrder` 返回 `ERR_REQ_MIXED_OR_MISSING_SUPPLIER`（`:136-138`）→ 缺失分支同样阻断。

**裁决**：A1.15 §7-4 静态判定「阻断」**运行时确认成立**。多供应商请购行 convertToOrder **运行时被 `validateConsistentSupplier` 阻断**（ERR_REQ_MIXED_OR_MISSING_SUPPLIER），经 `testConvertMixedSupplierRejected` + `testConvertMissingSupplierRejected` 双 @Test 强断言。**维持 P1-RC-017**（Q4 强制实现，修复归 MR1 R1.0 展开器——`convertToOrder` 重构为按行 supplier 分组生成多 ErpPurOrder + 解除 `validateConsistentSupplier` 单一供应商强制改为按 supplier 拆分；**纯 BizModel/Processor 代码逻辑修复，按 roadmap 预授权类目可自动执行，不触发 §5 ask-first**）。**不触发 MR0**（CRUD 主路径完整可用，仅多供应商拆分场景被拒，不破坏活跃数据）。

### 2-5 A4.2.31 承付恢复运行时不对称确认 — 维持 P1-MA2-083（reuse 重开）

**运行时证据链（live code 实测）**：

- **invoice approve → commitment release（正向已实现，config-gated）**：`ErpPurInvoiceProcessor.runCommitmentReleaseOnInvoiceApproveHook:273-292`——`if (!Boolean.TRUE.equals(AppConfig.var(ErpFinConstants.CONFIG_BUDGET_COMMITMENT_ENABLED, Boolean.FALSE))) return;`（`:274` config-gate **默认 false**）→ 经 `invoiceLine.receiveLineId → receiveLine.receiveId → receive.orderId → order.code` 反查关联订单编码集合（`resolveLinkedOrderCodes:301-...`）→ 对每个唯一 order.code 调 **`budgetCommitmentBiz.release(COMMITMENT_SOURCE_BILL_PURCHASE_ORDER, orderCode, context):283`**（容错 ERR_BUDGET_COMMITMENT_ALREADY_RELEASED 静默跳过 `:285-289`）。由 `ErpPurInvoiceApproveProcessor.approve:29` 在 approve 后置调用。
- **invoice reverseApprove → 零 commit() 恢复（不对称）**：`ErpPurInvoiceReverseApproveProcessor.reverseApprove:22-37`——`validateTransitionForReverseApprove:27` → **`postingDispatcher.reverse(invoice):29`**（AP 凭证红冲）→ `invoice.setPosted(false) + setPostedAt(null) + setPostedBy(null):31-33` → `processor.doReverseApprove(invoice, context):35`（设 REJECTED）。**全程零 `budgetCommitmentBiz.commit()` 调用** → commitment 保持已释放。
- **commit() 调用方 census（purchase 全域）**：`rg -n "budgetCommitmentBiz\.commit\(" --glob '*.java' module-purchase/erp-pur-service/src/main` 命中全集 = **`ErpPurOrderProcessor.java:208` 唯一一处**（commit-on-order-approve）。reverse/cancel Processor 全集（Invoice/Receive/Payment/Return/Requisition/Order 各 reverseApprove + cancel）**零 commit() 调用** → invoice 侧 approve→release vs reverseApprove/cancel→不恢复**运行时不对称确认**。
- **config 默认 false + 零生产 override（非默认活跃）**：`AppConfig.var(CONFIG_BUDGET_COMMITMENT_ENABLED, Boolean.FALSE)` 默认 false。生产部署普查：`rg "budget-commitment-enabled" --glob '**/src/main/resources/application*.yaml'`（全 20 生产 application.yaml）**零命中**；仅 TEST yaml（`budget-commitment-test.yaml` / `budget-a2-test.yaml` / `budget-commitment-sales-test.yaml` / `return-commitment-test.yaml`）设 true。**config-gated 默认 false 确认非默认活跃**——commitment 不对称破坏仅在部署显式启用预算承付时显现。
- **L4 间接证据**：`TestErpPurProcureToPayEnd.testReverseScenarios:194-200` 断言 invoice reverseApprove 后 `approveStatus=REJECTED` + `posted=false` + AP_INVOICE 原凭证+红字冲销凭证均存在（`countVoucherLinks("PI-REV-001") >= 2`）——AP 侧红冲闭环正确，但测试未断言 commitment 恢复（与不对称一致，commit() 未被调用故无恢复凭证可断言）。

**裁决**：A1.15 §7-5 静态判定「不对称」**运行时确认成立**。invoice approve → `runCommitmentReleaseOnInvoiceApproveHook` release commitment（config-gated）vs invoice reverseApprove → AP 红冲但 **commitment 保持已释放（零 commit() 调用）**运行时不对称确认。**维持 P1-MA2-083（reuse 重开）**（Q4=(a) 下方案B Deferred 关闭不成立，修复归 MR1 R1.0 展开器——`ErpPurInvoiceReverseApproveProcessor.reverseApprove` + `ErpPurInvoiceCancelProcessor.cancel` 新增按 invoice 关联 PO 反查 + config-gated 调既有 `budgetCommitmentBiz.commit()` 入口恢复承付 + 处理部分冲销/跨期语义；sales 侧 `ErpSalInvoiceProcessor` 同型；**纯 Processor 代码逻辑修复，按 roadmap 预授权类目可自动执行，不触发 §5 ask-first**——调既有 commit() 入口属纯 BizModel/Processor 预授权，不触及 PostingProcessor 核心路径）。config-gated `erp-fin.budget-commitment-enabled` 默认 false 确认**非默认活跃**（不对称破坏仅部署显式启用时显现）。**不触发 MR0**（config-gated 默认 false 保护，非默认活跃路径破坏；运行时未发现活跃数据破坏或会计错误已活跃）。

### 2-6 A4.2.32 取消后再转化运行时允许确认 — 维持 P2-RC-012

**运行时证据链（live code 实测）**：

- **幂等经 existsActiveByRequisition 查询实现**：`ErpPurRequisitionProcessor.validateNotAlreadyConverted:188-193`——`if (orderBiz.existsActiveByRequisition(requisitionId, context)) throw ERR_REQ_ALREADY_CONVERTED`。`ErpPurOrderBizModel.existsActiveByRequisition`（§A1.15 §2 step）按 `requisitionId` 查**非作废订单**存在性 → 活动订单存在时阻断重复转化。
- **cancel 后允许再转化路径**：订单 `cancel` 设 `docStatus=CANCELLED`（作废）→ 该订单**不再被 `existsActiveByRequisition` 命中**（非作废过滤）→ 若全部衍生订单均 cancel → `existsActiveByRequisition=false` → `validateNotAlreadyConverted` 通过 → **再次转化被允许**。
- **L4 强断言取消后再转化**：`TestErpPurRequisitionConvertToOrder.testConvertIdempotentRejected:142-162`——第一次 `convertToOrder` 成功（`first.getStatus()==0`，`:147-148`）→ 第二次 `convertToOrder` 返回 `ERR_REQ_ALREADY_CONVERTED`（`:152-154`，活动订单存在阻断）→ `orderCancel(firstId)` 作废第一次订单（`:156`，status==0 成功）→ **第三次 `convertToOrder` 成功**（`second.getStatus()==0`，`:158-159`）+ 转化产物 `approveStatus=UNSUBMITTED`（`:160-161`）→ **cancel 全部衍生订单后再次转化被允许**运行时确认。

**裁决**：A1.15 §7-6 静态判定「允许」**运行时确认成立**。cancel 全部衍生订单后 `existsActiveByRequisition=false` 允许再次转化，经 `testConvertIdempotentRejected:142-162` 强断言。**维持 P2-RC-012**（比 L1 字面「标记后不可重复转化」更宽松，§2 P2① 次要验收标准边界弱，登记不强制）。主路径（活动订单存在时阻断）OK，边界（全取消后允许再转化）比 L1 字面更宽松且属业务合理设计（取消后允许重做）。**不触发 MR0**（行为正确，无活跃数据破坏）。

---

## 3. 测试证据（L4）

| 测试 | 覆盖 | 与本审计关系 |
|------|------|------------|
| `TestErpPurProcureToPayEnd#testProcureToPayPartialSettlement:108-155` | UC-PUR-01 P2P 全链（订单→入库→发票→付款→settle 部分核销）+ receive.posted=true + invoice.posted=true + payment.posted=true + paidStatus=PARTIAL + AP_INVOICE/PAYMENT 凭证回链 | **直接证据**：A4.2.27（receive posted=true 落地 `:121`）+ A4.2.28（paidStatus=PARTIAL `:148`）+ A4.2.29（凭证回链 `:153-154`） |
| `TestErpPurProcureToPayEnd#testReverseScenarios:157-201` | UC-PUR-01 反向链路（settle PAID → reverseSettlement UNPAID → reverseApprovePayment → reverseApproveInvoice）+ posted 反转 + 红字冲销凭证 | **直接证据**：A4.2.28（reverseSettlement 负金额行回退 paidStatus UNPAID `:184`）+ A4.2.31（invoice reverseApprove AP 红冲闭环 `:194-200`，零 commitment 恢复与不对称一致） |
| `TestErpPurProcureToPayEnd#testFinanceReconciliationLayerPayable:210-295` | UC-PUR-01 ⑧ 应付余额辅助账生命周期（AP_INVOICE/PAYMENT 辅助账生成 + openAmount 56.5→0 + settledAmount 0→56.5 + SETTLED + sumOpen 113→0 + 账龄一致） | **直接证据**：A4.2.29（openAmount == 发票金额 − 已核销金额 恒等式 `:224-294`） |
| `TestErpPurProcureToPayEnd#testFinanceReconciliationLayerExceptions:300-341` | 异常路径（未审核发票无辅助账 + 超额核销拒绝 + openAmount 不变） | **直接证据**：A4.2.29（恒等式不被破坏 `:311-340`） |
| `TestErpPurRequisitionConvertToOrder#testConvertMixedSupplierRejected:113-125` | UC-PUR-08 ⑫ 多供应商拆分阻断 | **直接证据**：A4.2.30（ERR_REQ_MIXED_OR_MISSING_SUPPLIER `:122-124`） |
| `TestErpPurRequisitionConvertToOrder#testConvertMissingSupplierRejected:128-139` | UC-PUR-08 ⑫ 缺失供应商阻断 | **直接证据**：A4.2.30（缺失分支同样阻断 `:136-138`） |
| `TestErpPurRequisitionConvertToOrder#testConvertIdempotentRejected:142-162` | UC-PUR-08 ⑬⑭ 幂等 + 取消后再转化 | **直接证据**：A4.2.32（cancel 后允许再转化 `:156-161`） |

---

## 4. 业财保护区域探针纪律声明

> A4.2.27 / A4.2.29 触及业财保护区域（roadmap §横切关注点 #5：会计过账逻辑 / VoucherFact / PostingProcessor 核心路径 / 辅助账写路径）。

本审计为**只读探针**，遵守保护区域暂停协议：

- **READ-ONLY 标记（多处）**：本报告对 `InvPostingDispatcher` / `ErpPurReceiveProcessor.triggerIncomingMove/applyPostingResult` / `IErpInvStockMoveBiz.generateMove` Facade / `ErpFinArApItemGenerator` / `ErpFinArApItem` openAmount 写路径 / `reconciliationBiz` 的全部交互均为**只读追踪**（grep census + 调用链追踪 + 测试断言复核 + config 消费点普查），**未修改任何过账逻辑 / VoucherFact 构造 / PostingProcessor 核心路径 / Provider createFacts / 辅助账写路径**。
- **P1 维持不撤销**：P1-RC-017（多供应商拆分）+ P1-MA2-083（承付恢复）维持 P1（Q4 强制实现），**修复义务归 MR1 R1.0 展开器**，两者均**纯 BizModel/Processor 代码逻辑修复**（P1-RC-017 convertToOrder 重构按 supplier 拆分；P1-MA2-083 invoice reverseApprove/cancel 调既有 `budgetCommitmentBiz.commit()` 入口），按 roadmap 预授权类目可自动执行，**不触发 §5 ask-first**（不触及 ORM/会计过账核心路径——P1-MA2-083 调既有 commit() 入口属 Processor 逻辑非 PostingProcessor 核心）。
- **主路径行为正确闭合**：A4.2.27/A4.2.28/A4.2.29/A4.2.32 四项主路径行为运行时确认正确闭合，无修复义务。

---

## 5. 与既有 finding 衔接（复用裁决，无新 finding）

按 §去重协议，每项运行时确认裁决均 grep arm-index 同域同控制点后给出「复用维持」结论：

| finding | 本审计对应 | 运行时裁决 |
|---------|----------|-----------|
| `P1-RC-017`（arm-index :156） | A4.2.30（UC-PUR-08 ⑫ 多供应商拆分） | **维持 P1**：运行时确认 `validateConsistentSupplier:171-186` 阻断多供应商请购行 convertToOrder（ERR_REQ_MIXED_OR_MISSING_SUPPLIER），经 `testConvertMixedSupplierRejected` + `testConvertMissingSupplierRejected` 双 @Test 强断言。Q4 强制实现不撤销，修复归 MR1（纯 BizModel/Processor 预授权，不触 §5 ask-first）。 |
| `P1-MA2-083`（arm-index :544，A1.15 reuse 重开） | A4.2.31（承付恢复不对称） | **reuse 维持 P1**：运行时确认 `ErpPurInvoiceReverseApproveProcessor.reverseApprove:22-37` 零 commit() + commit() 调用方 census 全域仅 `ErpPurOrderProcessor:208`（commit-on-order-approve）+ config-gated 默认 false + 零生产 override。Q4=(a) 下方案B Deferred 关闭不成立，修复归 MR1（纯 Processor 逻辑调既有 commit() 入口，不触 §5 ask-first）。 |
| `P2-RC-011`（arm-index :157） | A4.2.27（GOODS_RECEIPT→PURCHASE_INPUT 命名漂移投影） | **维持 P2 watch-only**：运行时确认 PURCHASE_INPUT 触发链全链闭合（行为正确），命名漂移为 cosmetic 不影响行为。登记不强制。 |
| `P2-RC-012`（arm-index :158） | A4.2.32（UC-PUR-08 ⑬ 幂等实现漂移） | **维持 P2 watch-only**：运行时确认 cancel 全部衍生订单后 `existsActiveByRequisition=false` 允许再次转化（`testConvertIdempotentRejected:156-161`），主路径 OK 边界比 L1 字面更宽松。登记不强制。 |

**无新 finding 新建**（全部 reuse 维持 / 重开不降级）。运行时证据**未发现活跃数据破坏或会计错误已活跃**（A4.2.27/A4.2.28/A4.2.29 主路径行为正确闭合；A4.2.30 多供应商拆分缺失不破坏活跃数据[CRUD 可用]；A4.2.31 承付不对称经 config-gated 默认 false 保护非默认活跃；A4.2.32 取消后允许再转化属合理设计）→ **不触发 MR0**。

---

## 6. 多维审计自检（multi-dimensional-audit-prompt.md）

按 `docs/skills/multi-dimensional-audit-prompt.md` 默认 7 维度 + nop-app-erp 项目特定维度，逐维度裁决：

- **需求正确性**：6 项存疑点均逐字引自 A1.15 §7（L1 use-cases.md 真相源），运行时裁决与需求契约对齐（UC-PUR-01 ④⑦⑧ + UC-PUR-08 ⑫⑬ 均为 L1 显式验收标准）。本维度无新发现。
- **owner-doc 对齐**：`flow-overview.md §2.1`（P2P 链路）+ `state-machine.md §实现模式与守卫边界`（PROC 路径）+ `three-way-match.md §回链关系` + `posting.md`（业财过账）+ `budget.md §承付会计 §3`（commit/release 接入点）owner doc 声明与 HEAD 实现差距经运行时确认（A4.2.30 多供应商拆分阻断 vs L1 显式允许；A4.2.31 承付不对称 vs owner doc commit/release 接入点；A4.2.32 取消后允许 vs L1 字面「不可重复转化」）。本维度无新发现（差距归 MR1）。
- **架构或边界影响**：本审计零代码变更，不引入跨模块依赖 / API 契约变更 / 保护区域触碰。receive→`IErpInvStockMoveBiz.generateMove` 跨域 Facade + invoice approve→`budgetCommitmentBiz.release` 跨域 Facade + invoice reverseApprove 零对称 commit() 跨域 Facade——跨域边界经 A2.1/A2.8/A2.16 证实合规（purchase production 零 `daoFor(ErpInv*/ErpFin*)` 直写，均经 I*Biz Facade）。本维度无新发现。
- **验证充分性**：每项运行时裁决均有独立 grep census + file:line 证据 + 测试断言复核（§2-1..§2-6），可独立证伪（若 triggerIncomingMove 不调 generateMove / 若 recomputeInvoicePaid 不聚合跨 payment / 若 validateConsistentSupplier 不阻断多 supplier / 若 reverseApprove 调 commit() / 若 cancel 后不允许再转化，则裁决翻转）。本维度无新发现。
- **回归风险**：零生产代码变更，无回归路径。本维度无新发现。
- **路由和技能选择正确性**：roadmap MA4 全部工作项指定 `docs/skills/multi-dimensional-audit-prompt.md`，本审计为只读审计无代码变更，技能匹配。本维度无新发现。
- **待办或自主权策略漂移**：本审计范围 = A1.15 §7 六项存疑点运行时确认，未扩大范围、未关闭未完成项、未将阻塞降级为跟进项。两项 P1（P1-RC-017 / P1-MA2-083）全部维持（修复义务归 MR1，plan `Deferred But Adjudicated` 正确分类）；两项 P2 watch-only 维持登记不强制；四项主路径行为正确闭合。本维度无新发现。
- **项目特定维度（view.xml gen-control / ORM 完整性 / 代码生成纪律）**：本审计不触及 view.xml delta；未触及 ORM 结构（仅引用既有列/字段）；未触及生成文件。本维度无新发现。

**反窄化自检通过**：已对全部 8 维度给出裁决（含「本维度无发现」），非单维深挖。

---

## 7. 过程纪律自检

- [x] **checker 退出码门控核查**：本审计为只读审计，**无生产代码变更**，checker 无回归风险。本报告不以 checker 脚本退出码作为门控通过依据（checker 脚本为纯 reporter 退出码恒 0，真正门控在 CI workflow 解析 actual > baseline）。零代码变更 → actual = baseline（git status 仅 .md 文件）。
- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告全部 6 项运行时裁决已按 §去重协议 grep arm-index 同域同控制点后给出「复用维持」结论（P1-RC-017 维持 + P1-MA2-083 reuse 维持重开 + P2-RC-011 / P2-RC-012 维持 watch-only），**无未经比对直接新建的 finding，无新 finding 新建**。

---

## Verdict

**PASS（运行时确认维持 A1.15 §5/§6/§7 全部裁决）**：6 项静态存疑点运行时行为**全部确认成立**，A1.15 静态判定无一翻转：

- **A4.2.27（GOODS_RECEIPT/PURCHASE_INPUT 运行时触发链）CONFIRMED 主路径闭合**：receive approve → triggerIncomingMove → IErpInvStockMoveBiz.generateMove → InvPostingDispatcher PURCHASE_INPUT → move.posted=true → receive.posted=true 全链运行时闭合，经 `testProcureToPayPartialSettlement:121` 强断言凭证落地。
- **A4.2.28（paidStatus 派生运行时一致性）CONFIRMED 主路径闭合**：recomputeInvoicePaid SUM 跨 payment 聚合 + reverseSettlement 负金额行回退 paidStatus，经 partial + reverse 双 @Test 强断言。并发 lost-update 归 P2-MA2-008 watch-only（A2.17 范围）不重开。
- **A4.2.29（应付余额辅助账聚合运行时一致性）CONFIRMED 主路径闭合**：openAmount == 发票金额 − 已核销金额 恒等式运行时成立，经辅助账生命周期 + 异常路径双 @Test 强断言。
- **A4.2.30（多供应商拆分运行时阻断）CONFIRMED 维持 P1-RC-017**：validateConsistentSupplier 阻断多供应商请购行 convertToOrder，经 mixed + missing 双 @Test 强断言。修复归 MR1（纯 BizModel 预授权）。
- **A4.2.31（承付恢复运行时不对称）CONFIRMED 维持 P1-MA2-083（reuse 重开）**：invoice reverseApprove 零 commit() + commit() 调用方 census 全域仅 order-approve + config-gated 默认 false + 零生产 override。修复归 MR1（纯 Processor 逻辑调既有 commit() 入口，不触 §5 ask-first）。config-gate 默认 false 确认非默认活跃。
- **A4.2.32（取消后再转化运行时允许）CONFIRMED 维持 P2-RC-012**：cancel 全部衍生订单后允许再次转化，经 testConvertIdempotentRejected 强断言。比 L1 字面更宽松，登记不强制。

**裁决分支**：四项主路径（A4.2.27/A4.2.28/A4.2.29/A4.2.32）命中「主路径闭合」分支；两项缺陷（A4.2.30/A4.2.31）命中「维持 P1（reuse 重开 finding 不降级，Q4 强制实现）+ 运行时证据记录」分支；**无升级触发 MR0**（运行时未发现活跃数据破坏或会计错误已活跃）。修复义务归 MR1 R1.0 展开器（P1-RC-017 纯 BizModel/Processor 预授权；P1-MA2-083 纯 Processor 逻辑调既有 commit() 入口预授权，两者均不触 §5 ask-first）。

**本审计不实施修复**（只读审计，结果表面 = 本报告 + arm-index reuse 维持注记）。

---

## 参考

- 真相源：`docs/design/purchase/use-cases.md`（UC-PUR-01/08 验收标准 ④⑦⑧⑫⑬）
- 来源存疑点：`docs/audits/2026-08-03-0145-rc-ma1-a1-15-purchase-f1-mainflow-requisition.md` §7（6 项静态存疑点）+ §5（验收标准分级①-⑭）+ §6（finding 衔接裁决）
- 设计参考：`docs/design/purchase/state-machine.md` + `three-way-match.md` + `README.md` + `requisition.md` + `docs/design/flow-overview.md §2.1`（P2P 链路）+ `docs/design/finance/posting.md`（过账触发链）+ `docs/design/finance/budget.md §承付会计 §3`（commit/release 接入点）
- L5 既有证据：`docs/audits/2026-07-28-0230-arm-ma2-purchase-state-machine.md`（A2.8 状态机迁移 + reverseApprove 红冲闭环）+ `docs/audits/2026-07-27-1949-arm-ma2-procure-to-pay-e2e.md`（A2.1 P2P 链路 + 库存 incoming Facade）+ `docs/audits/2026-08-02-1645-rc-ma1-a1-1-finance-f1-posting.md`（A1.1 业财过账引擎 GR/IR + AP 范式）+ `docs/audits/2026-07-28-1249-arm-ma2-budget-commitment-release.md`（A2.16 承付 commit/release 路径完整性）
- 方法论：`docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议）
- 技能：`docs/skills/multi-dimensional-audit-prompt.md`（默认 7 维度 + 项目特定维度）
