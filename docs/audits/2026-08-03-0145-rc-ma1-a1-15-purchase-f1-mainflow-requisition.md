# A1.15 purchase-F1 主流程与请购 需求-实现符合性审计报告（rc-ma1-a1-15）

> Mission: requirement-compliance · Work Item: A1.15（UC-PUR-01 标准采购全流程主路径 + UC-PUR-08 请购转订单）
> 来源计划: `docs/plans/2026-08-03-0100-1-rc-ma1-a1-15-purchase-f1-mainflow-requisition.md`
> 方法论: `docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议）
> 审计类型: 只读审计（无代码/ORM/api.xml/view.xml/真相源变更）
> 审计日期: 2026-08-03

## 9. 与既有 MA2 / P2P e2e / A1.1 报告的差异增量声明（前置）

本报告是 **requirement-compliance** mission MA1 切片 A1.15 的五级追踪审计，视角 = **需求契约（L1 use-cases）→ 实现符合性**。按 §去重协议，以下既有审计已证实的结论本报告**直接复用，不重审**：

- **A2.8**（`docs/audits/2026-07-28-0230-arm-ma2-purchase-state-machine.md`）：purchase 9 实体状态机迁移守卫齐全（PROC 路径 `validateNotCancelled`/`validateTransition*`/`validateBusinessRules*` 三段守卫 + `doApprove`/`doReject`/`doReverseApprove`/`doCancel` 四动作齐全）+ @BizMutation 事务回滚保证 approve 触发的跨域写（承付 commit/release + 库存 incoming + 过账 AP_INVOICE/PAYMENT/PURCHASE_INPUT）失败原子性 + reverseApprove 红冲闭环强一致 + 跨域写经 I*Biz Facade（production 代码无 `daoFor(Erp*)` 跨域写直写）。本报告复用其 L5 行为证据。
- **A2.1 P2P e2e**（`docs/audits/2026-07-27-1949-arm-ma2-procure-to-pay-e2e.md`）：P2P 链路行为已证实（回链 / 库存 incoming / 过账 / 核销 主路径完整）+ P1-MA2-003 settle 三单匹配复核 resolved（plan 2026-07-29-2322-1 方案 A）+ P2-MA2-007 价格锁缺失 watch-only + P2-MA2-008 settle 并发无锁归 A2.17。
- **A1.1**（`docs/audits/2026-08-02-1645-rc-ma1-a1-1-finance-f1-posting.md`）：业财过账引擎 GR/IR + AP 凭证范式已审（Provider 路由 + VoucherBillR 业财回链 + GR/IR 暂估应付），本切片引用其过账正确性结论，只补采购侧触发契约 + GOODS_RECEIPT 触发路径核验。

本报告**只补需求视角差异**：(i) UC-PUR-01 八条验收标准逐条（订单回链 / 入库行回链 / 库存可用量 / GOODS_RECEIPT 凭证 / PURCHASE_INVOICE 凭证 / 已过账标志 / 付款核销派生 paidStatus / 应付余额）；(ii) UC-PUR-08 六条验收标准逐条（前置 APPROVED / 生成订单 / 行继承可编辑 / 一请购拆多订单 / 已转订单幂等 / 重复转化报错）；(iii) **resolved finding HEAD 复核（R1.17/R1.27/MR5 R5.8/plan 2026-07-29-2322-1）落地确认**——其中 **P1-MA2-083（承付恢复）HEAD 复核发现 audit-remediation 侧的「方案B Deferred」关闭在 requirement-compliance Q4=(a) 下不成立，须按 §10 经 MR1（RC-R1.n）实现**（与 A1.14 P1-MA4-017 同型重开模式）；(iv) **UC-PUR-08 ④ 一请购拆多订单（不同供应商）被 `validateConsistentSupplier` 守卫阻断**——L1 显式支持多供应商拆分而实现强制单一供应商，新发现 P1 级需求分歧。

---

## 1. 需求契约原文（L1，逐字引用）

> 真相源：`docs/design/purchase/use-cases.md`（层级 2 功能契约，§4 真相源层级）。以下逐字引用，不转述。

### UC-PUR-01 标准采购全流程(主路径)（`use-cases.md:19`）

| 项目 | 原文 |
|------|------|
| 场景 | 从请购到付款的完整正向采购 |
| 前置 | 主数据就绪(物料/往来单位/组织/币种/税率);账套与科目配置完成 |
| 行为链路 | 1. 创建请购单,审核通过<br>2. 由请购单生成采购订单,审核通过<br>3. 收货 → 创建采购入库单(关联订单),审核通过<br>4. 收票 → 创建采购发票(关联入库),审核通过<br>5. 创建付款单,审核通过,核销发票 |
| 可验证断言 | `// 单据回链` `订单.来源单号 == 请购单.单号` / `入库单行.订单行号 回链 订单行`（见 three-way-match §回链关系）<br>`// 库存(入库单审核时)` `库存余额[物料, 仓库].可用量 += 入库明细数量之和`<br>`// 过账(入库单/发票审核时, 见 posting.md)` `存在凭证: 业务类型 == GOODS_RECEIPT 且 来源单号 == 入库单.单号` / `存在凭证: 业务类型 == PURCHASE_INVOICE 且 来源单号 == 发票.单号` / `入库单.已过账 == true 且 发票.已过账 == true`<br>`// 核销(付款时)` `发票.付款状态: 未付 → 部分(部分核销) / 已付清(全额核销)` / `往来单位.应付余额 == 发票金额 - 已核销金额` |
| 涉及机制 | state-machine.md、three-way-match.md、../finance/posting.md、../inventory |

### UC-PUR-08 请购转订单（`use-cases.md:204`）

| 项目 | 原文 |
|------|------|
| 场景 | 请购单审批后转化为采购订单 |
| 可验证断言 | `// 前置` `请购单.审核状态 == 已审核   // 必要条件`<br>`// 转化` `由请购单生成订单` / `订单行(数量/物料) 继承自 请购单行, 可编辑`<br>`// 一个请购可拆多个订单(不同供应商/到货期)` `生成订单数 >= 1`<br>`// 幂等` `请购单.已转订单 == true   // 标记后不可重复转化` / `再次转化 → 报错或返回已转化` |
| 涉及机制 | state-machine.md |

---

## 2. 实现证据（L3，`file:line`，含跨域调用链）

### UC-PUR-01 标准采购全流程主路径

#### step1 请购审核
- `module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/entity/ErpPurRequisitionBizModel.java:25-48` — `cancel/convertToOrder` Facade（标准审批动作走 Processor）。
- `module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/processor/ErpPurRequisitionApproveProcessor.java` — per-mutation approve Processor（R6.5），含 `validateNotCancelled` + `validateTransitionForApprove` + `doApprove`（含 SoDGuard `assertApproverNotCreator`）。
- `module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/processor/ErpPurRequisitionProcessor.java:120-125/154-161/207-213` — `validateTransitionForApprove`（SUBMITTED 守卫）+ `validateApprovedForConversion`（APPROVED 守卫）+ `doApprove`（设 APPROVED + approvedBy/approvedAt）。

#### step2 订单生成与审核（UC-PUR-01 ① + UC-PUR-08 ②③ + 价格锁）
- `module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/entity/ErpPurOrderBizModel.java:91-102/106-119` — `createFromRequisition`（`converter.build` + `saveEntity` + `buildLines` 逐行 `daoFor(ErpPurOrderLine).saveEntity`）+ `existsActiveByRequisition`（按 `requisitionId` 查非作废订单，幂等依据）。
- `module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/entity/RequisitionToOrderConverter.java:49-99` — `build`（订单头：`code`/`orgId`/`requisitionId`[回链]/`supplierId`/`warehouseId`/`businessDate`/`currencyId`/`approveStatus=UNSUBMITTED`/`docStatus=DRAFT`）+ `buildLines`（订单行：复制 `materialId`/`uoMId`/`quantity`/`projectId`，按调用方按行号提供 `unitPrice`/`taxRate`，计算 `amount`/`taxAmount`/`amountWithTax`，VARCHAR 写入对齐采购域金额约定）。
- `module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/processor/ErpPurOrderApproveProcessor.java:42-56` — `validateNotCancelled`（委托 `processor.validateNotCancelled`）+ `validateBusinessRules`（`requireSupplierActive` + `runBudgetCheckHook`）+ `afterStateChange`（`runCommitmentCommitHook` + `runIntercompanyApproveHook`）。
- `module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/processor/ErpPurOrderProcessor.java:197-220/358-374` — `runCommitmentCommitHook`（commit-on-order-approve）+ `requireSupplierActive`（`IErpMdPartnerBiz.findById` + status==ACTIVE 校验）。**订单审核价格锁（P2-MA2-007 watch-only）未实现**——`ErpPurOrderProcessor.java` 无 `priceLock`/`lockPrice` 字段或方法。

#### step3 收货入库（UC-PUR-01 ②③ + ④ GOODS_RECEIPT 触发路径）
- `module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/processor/ErpPurReceiveApproveProcessor.java:26-49` — override `approve`：`validateNotCancelled` + `validateTransitionForApprove` + `validateBusinessRulesForApprove` + `enforceInspectionGate`（QA 来料质检门控）+ **`triggerIncomingMove`（库存 incoming Facade）** + 设 APPROVED + `applyPostingResult`（**receive.posted = move.posted**）+ `setReceiveStatus(RECEIVED)` + `postProcessApprove`（`rollupOrderReceiveStatus`）。
- `module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/processor/ErpPurReceiveProcessor.java:215-219/221-227/244-284` — `triggerIncomingMove`（`IErpInvStockMoveBiz.generateMove` Facade，库存 incoming）+ `applyPostingResult`（**posted 来自 move.getPosted()——GOODS_RECEIPT/PURCHASE_INPUT 过账经 stockMoveBiz.generateMove 内部触发，无独立 PurReceivePostingDispatcher**）+ `rollupOrderReceiveStatus`（按 orderId 聚合 receive lines 累计数量 → 更新订单 receiveStatus：UNRECEIVED/PARTIAL/RECEIVED）。
- 跨域库存 incoming Facade：`IErpInvStockMoveBiz.generateMove` → inventory 域 `ErpInvStockMove` DONE → `module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/posting/InvPostingDispatcher.java:51-54/169` → **`PURCHASE_INPUT`（业务类型 = 入库借存货/贷暂估应付 GR/IR，对齐 L1 §UC-PUR-07 ① 「借 存货科目, 贷 暂估应付(GR/IR)」语义）**。**L1 字面 GOODS_RECEIPT 在 ErpFinBusinessType 枚举（`module-finance/erp-fin-dao/.../ErpFinBusinessType.java:13-69`）不存在——经 PURCHASE_INPUT 语义等价实现（命名漂移，§5 ④ 验收标准分级 = P2）**。

#### step4 收票（UC-PUR-01 ⑤ PURCHASE_INVOICE 触发）
- `module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/processor/ErpPurInvoiceApproveProcessor.java:18-31` — override `approve`：`validateNotCancelled` + `validateTransitionForApprove` + `validateBusinessRulesForApprove`（含 `threeWayMatcher.match`）+ `doPosting`（`postingDispatcher.tryPost`）+ `doApprove`（设 APPROVED + posted postedAt postedBy）+ **`runCommitmentReleaseOnInvoiceApproveHook`**（承付 release，config-gated `erp-fin.budget-commitment-enabled`）。
- `module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/processor/ErpPurInvoiceProcessor.java:161-165/169-171/183-194/273-292/300-340` — `validateBusinessRulesForApprove`（`requireSupplierActive` + 三单匹配）+ `doPosting`（委托 `postingDispatcher.tryPost`）+ `doApprove`（含 SoDGuard）+ `runCommitmentReleaseOnInvoiceApproveHook`（按 invoiceLine.receiveLineId → receiveLine.receiveId → receive.orderId → order.code 反查关联订单编码，对每个唯一 order.code 调 `budgetCommitmentBiz.release`，容错 ERR_BUDGET_COMMITMENT_ALREADY_RELEASED）。
- `module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/posting/PurInvoicePostingDispatcher.java:39-89` — `tryPost`（组装 `PostingEvent`(**businessType=AP_INVOICE**) → `executor.postEvent(event)` → 返回 voucherId）+ `reverse`（红冲，调用方 reverseApprove/cancel 用）+ `buildEvent`（设 billHeadCode=invoice.code/orgId/currencyId/exchangeRate[null→BigDecimal.ONE]/voucherDate/billData[totalAmount/totalTaxAmount/totalAmountWithTax/SUPPLIER_ID]）。**L1 字面 PURCHASE_INVOICE 在 ErpFinBusinessType 枚举不存在——经 AP_INVOICE 语义等价实现（命名漂移，§5 ⑤ 验收标准分级 = P2）**。

#### step5 付款核销（UC-PUR-01 ⑦ 派生 paidStatus + ⑧ 应付余额）
- `module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/entity/ErpPurPaymentBizModel.java` — Facade（标准审批 + settle 委托 Processor）。
- `module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/processor/ErpPurPaymentSettleProcessor.java` — per-mutation settle Processor。
- `module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/entity/PaymentSettler.java:65-121/151-191/201-217` — `settle`（**前置守卫：payment.approveStatus==APPROVED + 跨发票供应商一致 + invoice.approveStatus==APPROVED + 核销金额不超 invoiceBalance / paymentRemaining**）+ 写 PaymentLine + **`recomputeInvoicePaid`（按 SUM(PaymentLine.amount) 派生 invoice.paidAmount + paidStatus：UNPAID/PARTIAL/PAID）** + `recomputePaymentWrittenOff`（派生 payment.writtenOffStatus）+ `requireInvoiceForSettle`（含 **R1.8 P1-MA2-003 方案 A settle 三单匹配复核 `recheckThreeWayMatchAtSettle`，config-gated `erp-pur.settle-recheck-three-way-match` 默认 false**）。
- 跨域过账 Facade：`module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/posting/PurPaymentPostingDispatcher.java`（PAYMENT 凭证）— 与 A1.1 业财过账引擎范式一致，经 `IErpFinVoucherBiz.post()` REQUIRES_NEW Facade。
- **应付余额 ⑧ 不持久化于 ErpMdPartner.payableBalance 单字段，而是经 AP_INVOICE/PAYMENT 过账生成 ErpFinArApItem（DIRECTION_PAYABLE）辅助账项，查询时 SUM(openAmount) == 发票金额 - 已核销金额**（标准 ERP 辅助账模式，`TestErpPurProcureToPayEnd:244-272` 强断言）。

### UC-PUR-08 请购转订单

- `module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/processor/ErpPurRequisitionProcessor.java:89-97/154-193/232-235` — `convertToOrder`：`requireRequisition` → **`validateApprovedForConversion`（APPROVED 守卫，断言①）** → `loadLines` → **`validateLinesNonEmptyForConversion`（断言行非空）** → **`validateConsistentSupplier`（**强制单一供应商**——Set<Long> suppliers，size!=1 抛 ERR_REQ_MIXED_OR_MISSING_SUPPLIER，**与 L1 ④ 「不同供应商」允许冲突，§5 ④ 分级 = P1）**）→ **`validateNotAlreadyConverted`（`orderBiz.existsActiveByRequisition` 存在性查询幂等，断言⑤+⑥）** → `doConvertToOrder`（委托 `orderBiz.createFromRequisition`）。
- ORM 字段核验：`grep "已转订单\|convertedToOrder\|requisitionConverted\|orderConverted" module-purchase/model/app-erp-purchase.orm.xml` = **0 命中**——L1 字面「请购单.已转订单==true」字段不存在，幂等经 `existsActiveByRequisition`（按 requisitionId 查非作废订单）查询实现（语义等价，命名漂移，§5 ⑤ 分级 = P2）。

### 过账 Dispatchers（汇总）

- `posting/PurInvoicePostingDispatcher.java`（AP_INVOICE，发票审核触发）+ `posting/PurPaymentPostingDispatcher.java`（PAYMENT，付款审核触发）+ `posting/PurReturnPostingDispatcher.java`（PURCHASE_RETURN，归 A1.17）+ `posting/PurReversalListener.java:46-126`（finance→purchase 反向回滚 AP_INVOICE/PAYMENT/PURCHASE_RETURN/PURCHASE_INPUT，rollbackInvoice/Payment/Return/Receive 四实体 posted=false + APPROVED→REJECTED，对齐 A2.8 §reversal listener 回退目标态表）。
- **GOODS_RECEIPT/PURCHASE_INPUT 触发路径 = 经 ReceiveApproveProcessor.triggerIncomingMove → IErpInvStockMoveBiz.generateMove → InvPostingDispatcher（库存域 dispatcher 内嵌 PURCHASE_INPUT）→ receive.posted=move.posted**（无独立 PurReceivePostingDispatcher，A1.1 业财过账引擎范式）。

---

## 3. 测试证据（L4，注明断言强度）

| 测试 | 覆盖 | 断言强度 |
|------|------|---------|
| `TestErpPurProcureToPayEnd.java` | UC-PUR-01 P2P 全链（订单→入库→发票→付款→settle 部分核销）+ AP_INVOICE/PAYMENT 凭证回链 + 应付辅助账 openAmount 生命周期 + 红冲链路 + 异常路径（未审发票无辅助账 / 超额核销拒绝） | **强断言**（posted/paidStatus/openAmount/红冲凭证数全断言；注释明示 PURCHASE_INPUT + AP_INVOICE + PAYMENT 凭证行 1401/2202/1403/2221/1002 科目） |
| `TestErpPurRequisitionConvertToOrder.java` | UC-PUR-08 转订单（转化产物 approveStatus/docStatus/requisitionId 回链/supplierId/warehouseId/currencyId/businessDate/orgId + 行 materialId/uoMId/quantity/unitPrice/amount/taxRate/taxAmount/amountWithTax 全断言）+ 未审核前置拒绝 + 混供应商拒绝 + 已转化重复拒绝 + 取消后再转化成功 + 转化产物可走 submit/approve | **强断言**（每验收标准一独立 @Test，含异常路径） |
| `TestErpPurRequisitionApproval.java` + `TestErpPurRequisitionToOrderEnd.java` | UC-PUR-08 step1（请购审批）+ 端到端（请购→订单→入库） | 强断言 |
| `TestErpPurOrderApproval.java` + `TestErpPurOrderToReceiveEnd.java` + `TestErpPurOrderCommitment.java` | UC-PUR-01 step2 订单审批 + 承付 commit-on-approve + 订单→入库端到端 | 强断言 |
| `TestErpPurReceiveApproval.java` + `TestErpPurReceiveStockMove.java` | UC-PUR-01 step3 入库审批 + 库存 incoming（IErpInvStockMoveBiz.generateMove）+ order receiveStatus 派生 | 强断言 |
| `TestErpPurInvoiceApproval.java` + `TestErpPurInvoicePosting.java` | UC-PUR-01 step4 发票审批 + AP_INVOICE 凭证 + 三单匹配 + 承付 release-on-invoice-approve | 强断言 |
| `TestErpPurPaymentApproval.java` + `TestErpPurPaymentSettlement.java` + `TestErpPurPaymentWorkflowApproval.java` | UC-PUR-01 step5 付款审批 + settle（paidStatus UNPAID/PARTIAL/PAID + writtenOffStatus） + workflow 审批 | 强断言 |
| `TestErpPurSettleThreeWayMatchRecheck.java` | P1-MA2-003 方案 A settle 三单匹配二次门控（`erp-pur.settle-recheck-three-way-match=true`）| 强断言 |
| `TestErpPurBudgetControlIntegration.java` | UC-FIN-11 跨域预算硬拦截（采购审核调 BudgetControlBiz.check） | 强断言 |
| E2E `tests/e2e/orchestration/p2p-chain.spec.ts` | UC-PUR-01 P2P 全链 E2E | 强断言 |
| E2E `tests/e2e/orchestration/p2p-reverse-approve.spec.ts` + `p2p-reverse.spec.ts` | UC-PUR-01 红冲链路 E2E | 强断言 |
| E2E `tests/e2e/crud/purchase.smoke.spec.ts` | 采购域 CRUD 冒烟 | 冒烟 |

---

## 4. 运行时行为证据（L5）

按 §去重协议，L5 行为证据复用 A2.8 / A2.1 P2P e2e / A1.1 已证实结论：

- **9 实体三轴状态机迁移守卫 + 跨域 Facade + reverseApprove 红冲闭环**：行为已证实（A2.8）。
- **P2P 链路行为**：订单→入库→发票→付款→settle 主路径完整，回链 / 库存 incoming / 过账 / 核销 全链路行为已证实（A2.1）。
- **业财过账引擎范式**：GR/IR 暂估应付 + AP 应付 + 跨域经 IErpFinVoucherBiz.post() REQUIRES_NEW Facade，业财回链 VoucherBillR 完整（A1.1）。
- **承付 commit/release 路径完整性**：commit-on-order-approve + release-on-invoice-approve + config-gated 默认 false（A2.16）。

**本切片差异（需求契约↔行为）**：
- UC-PUR-01 ④ GOODS_RECEIPT 凭证——**L1 字面 businessType==GOODS_RECEIPT 在枚举不存在，实仓以 PURCHASE_INPUT 语义等价实现**（行为正确，命名漂移）。
- UC-PUR-01 ⑤ PURCHASE_INVOICE 凭证——**L1 字面 businessType==PURCHASE_INVOICE 在枚举不存在，实仓以 AP_INVOICE 语义等价实现**（行为正确，命名漂移）。
- UC-PUR-01 ⑧ 应付余额——**经辅助账 ErpFinArApItem.openAmount 聚合实现**，非 ErpMdPartner.payableBalance 单字段（标准 ERP 模式）。
- UC-PUR-08 ④ 一请购拆多订单（不同供应商）——**`validateConsistentSupplier` 守卫强制单一供应商**，L1 「不同供应商」拆分场景运行时被 ERR_REQ_MIXED_OR_MISSING_SUPPLIER 阻断。
- UC-PUR-08 ⑤ 已转订单标记——**无持久化 `已转订单` 字段**，幂等经 `existsActiveByRequisition` 查询实现；**取消全部衍生订单后允许再次转化**（比 L1 字面「不可重复转化」更宽松）。
- 承付 release-on-invoice-approve 已实现，**但 invoice reverseApprove/cancel 不对称恢复承付**（P1-MA2-083 方案B Deferred 关闭在 Q4=(a) 下不成立）——交 §7 静态存疑点 + §5 结论。

---

## 5. 五级追踪矩阵 + 每 UC 符合性结论

### 矩阵（2 行，每 UC 一行）

| UC | L1 需求契约 | L2 owner doc（设计参考，冲突以 L1 为准） | L3 代码路径 | L4 测试断言 | L5 运行时行为 | 符合性结论 |
|----|----|----|----|----|----|----|
| **UC-PUR-01** | `use-cases.md:19` 标准采购全流程主路径（①订单来源回链 + ②入库行回链订单行 + ③入库审核库存可用量+= + ④GOODS_RECEIPT 凭证 + ⑤PURCHASE_INVOICE 凭证 + ⑥入库/发票已过账 + ⑦付款核销派生 paidStatus + ⑧应付余额==发票-已核销） | `state-machine.md §2 审核轴`（SUBMITTED→APPROVED 触发后续业务）+ `three-way-match.md §回链关系`（入库行可选回链订单行）+ `flow-overview.md §2.1`（订单审核锁定价格 P2-MA2-007 watch-only，**L1 冲突以 L1 为准**） | `ErpPurRequisitionBizModel.java:25-48` + `ErpPurRequisitionApproveProcessor` + `ErpPurOrderBizModel.java:91-102/106-119` + `RequisitionToOrderConverter.java:49-99` + `ErpPurOrderApproveProcessor.java:42-56` + `ErpPurReceiveApproveProcessor.java:26-49` + `ErpPurReceiveProcessor.java:215-284`（库存 incoming Facade + receiveStatus 派生）+ `ErpPurInvoiceApproveProcessor.java:18-31` + `ErpPurInvoiceProcessor.java:161-292`（含 commitment release）+ `PurInvoicePostingDispatcher.java:39-89`（AP_INVOICE）+ `PaymentSettler.java:65-217`（含 R1.8 P1-MA2-003 方案 A recheck）+ `PurPaymentPostingDispatcher`（PAYMENT）+ `PurReversalListener.java:46-126` | `TestErpPurProcureToPayEnd`（强，全链 + 辅助账 openAmount）+ `TestErpPurReceiveStockMove`（强，库存 incoming）+ `TestErpPurInvoicePosting`（强，AP_INVOICE 凭证）+ `TestErpPurPaymentSettlement`（强，paidStatus 三态）+ `TestErpPurSettleThreeWayMatchRecheck`（强，R1.8 复核）+ E2E p2p-chain/p2p-reverse（强） | 行为已证实（A2.8 + A2.1 + A1.1）；GOODS_RECEIPT→PURCHASE_INPUT / PURCHASE_INVOICE→AP_INVOICE 命名漂移行为等价；⑧ 辅助账聚合模式；P1-MA2-083 invoice 冲销不恢复承付（**方案B Deferred 在 Q4=(a) 下不成立，reuse 重开**） | **接受 on ①②③⑥⑦⑧；P2 on ④⑤ 命名漂移（P2-RC-011 新，合并登记）；P1 on 承付恢复（reuse P1-MA2-083 重开）** |
| **UC-PUR-08** | `use-cases.md:204` 请购转订单（①前置请购已审核 + ②生成订单 + ③订单行继承可编辑 + ④一请购拆多订单不同供应商/到货期 + ⑤已转订单幂等标记 + ⑥重复转化报错） | `state-machine.md §实现模式与守卫边界`（PROC/INLINE 路径）+ `requisition.md`（请购→转订单设计参考，**L1 冲突以 L1 为准**——L2 设计单一供应商，L1 显式允许多供应商拆分） | `ErpPurRequisitionBizModel.java:43-48`（convertToOrder Facade）+ `ErpPurRequisitionProcessor.java:89-97/154-193/232-235`（validateApprovedForConversion + validateConsistentSupplier + validateNotAlreadyConverted + doConvertToOrder）+ `RequisitionToOrderConverter.java:49-99`（行继承）+ `ErpPurOrderBizModel.java:91-119`（createFromRequisition + existsActiveByRequisition）+ ORM 实仓无 `已转订单` 字段 | `TestErpPurRequisitionConvertToOrder`（强，6 @Test 全覆盖：转化成功 + 行字段 + 未审核前置拒绝 + 混供应商拒绝 + 已转化重复拒绝 + 取消后再转化成功 + 转化产物可走审批） | 行为已证实；④ 多供应商拆分被 `validateConsistentSupplier` 守卫阻断（运行时 ERR_REQ_MIXED_OR_MISSING_SUPPLIER）；⑤ 幂等经 `existsActiveByRequisition` 查询（取消衍生订单后允许再转化，比 L1 字面更宽松） | **接受 on ①②③⑥；P1 on ④ 多供应商拆分（P1-RC-017 新）；P2 on ⑤ 幂等实现漂移（P2-RC-012 新）** |

### 逐条验收标准分级（§3 完整枚举，14 条）

| # | 验收标准 | UC | HEAD 状态 | 分级 |
|---|---------|-----|---------|------|
| ① | 订单.来源单号 == 请购单.单号（回链写入） | PUR-01 | `RequisitionToOrderConverter.build:54` `order.setRequisitionId(req.getId())` ✅（订单反链 requisitionId 字段） | 接受 |
| ② | 入库单行.订单行号 回链 订单行 | PUR-01 | `ErpPurReceiveLine.orderLineId`（threeway-match §回链关系：可选回链）+ `ErpPurReceiveProcessor.addLineQuantities:366-374` 按 orderLineId 聚合 ✅ | 接受 |
| ③ | 库存余额[物料, 仓库].可用量 += 入库明细数量之和 | PUR-01 | `ErpPurReceiveProcessor.triggerIncomingMove:215-219` → `IErpInvStockMoveBiz.generateMove`（库存 incoming Facade，A2.1/A2.4 已证实库存余额 += 实现） ✅ | 接受 |
| ④ | 存在凭证: businessType == GOODS_RECEIPT 且 来源单号 == 入库单.单号 | PUR-01 | **L1 字面 GOODS_RECEIPT 在 ErpFinBusinessType 枚举不存在**；实仓经 `InvPostingDispatcher.java:169` `ErpFinBusinessType.PURCHASE_INPUT`（1401/2202 借存货/贷暂估应付 GR/IR，与 L1 §UC-PUR-07 ① 语义等价）；`receive.posted` 来自 `move.getPosted()`（InvPostingDispatcher 内部过账结果） | **P2 → P2-RC-011**（命名漂移，行为等价） |
| ⑤ | 存在凭证: businessType == PURCHASE_INVOICE 且 来源单号 == 发票.单号 | PUR-01 | **L1 字面 PURCHASE_INVOICE 在 ErpFinBusinessType 枚举不存在**；实仓经 `PurInvoicePostingDispatcher.buildEvent:73` `ErpFinBusinessType.AP_INVOICE`（1403/2221/2202 借 IPP/进项税 + 贷应付 + GR/IR 反向，与 L1 §UC-PUR-07 ② 语义等价）；`invoice.posted=true` 落地 | **P2 → P2-RC-011**（命名漂移，行为等价，与 ④ 同根因同控制点合并登记） |
| ⑥ | 入库单.已过账 == true 且 发票.已过账 == true | PUR-01 | `ErpPurReceiveProcessor.applyPostingResult:221-227`（receive.posted=move.posted）+ `ErpPurInvoiceProcessor.doApprove:188-192`（invoice.posted=true 落地） ✅ | 接受 |
| ⑦ | 发票.付款状态: 未付 → 部分 / 已付清 | PUR-01 | `PaymentSettler.recomputeInvoicePaid:201-217` 按 SUM(PaymentLine.amount) 派生 paidAmount + paidStatus UNPAID/PARTIAL/PAID ✅ | 接受 |
| ⑧ | 往来单位.应付余额 == 发票金额 - 已核销金额 | PUR-01 | **不持久化 ErpMdPartner.payableBalance 单字段**；经 AP_INVOICE/PAYMENT 过账生成 `ErpFinArApItem`（DIRECTION_PAYABLE）辅助账项 + 查询时 SUM(openAmount) == 发票金额 - 已核销金额（标准 ERP 辅助账模式，`TestErpPurProcureToPayEnd:244-272` 强断言） ✅ | 接受（辅助账聚合语义等价） |
| ⑨ | 前置：请购单.审核状态 == 已审核（必要条件） | PUR-08 | `ErpPurRequisitionProcessor.validateApprovedForConversion:154-161` APPROVED 守卫抛 ERR_REQ_NOT_APPROVED ✅ | 接受 |
| ⑩ | 由请购单生成订单 | PUR-08 | `ErpPurRequisitionProcessor.doConvertToOrder:232-235` → `orderBiz.createFromRequisition` ✅ | 接受 |
| ⑪ | 订单行(数量/物料) 继承自 请购单行, 可编辑 | PUR-08 | `RequisitionToOrderConverter.buildLines:67-99` 复制 materialId/uoMId/quantity/projectId + 调用方提供 unitPrice/taxRate + 计算金额族；产物 approveStatus=UNSUBMITTED → 可编辑 ✅ | 接受 |
| ⑫ | 一个请购可拆多个订单(不同供应商/到货期) | PUR-08 | **`ErpPurRequisitionProcessor.validateConsistentSupplier:171-186` 强制单一供应商**——Set<Long> suppliers，size!=1 抛 ERR_REQ_MIXED_OR_MISSING_SUPPLIER；`TestErpPurRequisitionConvertToOrder#test_convertFailsWhenMixedSuppliers` 显式断言此拒绝；**L1 「不同供应商」拆分场景运行时被阻断** | **P1 → P1-RC-017**（功能实质偏离 L1 字面，§2 P1①） |
| ⑬ | 请购单.已转订单 == true（标记后不可重复转化） | PUR-08 | **无持久化 `已转订单` 字段**（grep ORM 0 命中）；幂等经 `ErpPurOrderBizModel.existsActiveByRequisition:106-119`（按 requisitionId 查非作废订单）查询实现；**取消全部衍生订单后允许再次转化**（`TestErpPurRequisitionConvertToOrder#test_convertIsIdempotentButReallowsAfterCancel:147-162` 显式断言） | **P2 → P2-RC-012**（语义漂移——比 L1 字面更宽松，主路径[活动订单存在时阻断]OK 边界[全取消后允许]弱） |
| ⑭ | 再次转化 → 报错或返回已转化 | PUR-08 | `validateNotAlreadyConverted:188-193` 抛 ERR_REQ_ALREADY_CONVERTED（活动订单存在时）+ `test_convertFailsWhenAlreadyConverted` 强断言 ✅ | 接受（与 ⑬ 联动：仅在活动订单存在时报错） |

### resolved finding HEAD 复核（关键证据，§逻辑非行号）

| finding | arm-index 声称 | HEAD 复核结论 |
|---------|--------------|--------------|
| **P1-MA2-083**（AP/AR 发票冲销不恢复承付） | ✅ resolved (R1.27 done) | **未落地（方案B Deferred）**。R1.27 计划 `2026-07-30-0841-1-r1-27-budget-commitment-release-path.md` §Phase 1 line 44 显式裁决：**「不实现发票冲销自动恢复承付（P1-MA2-083 方案A——须跨实体反查原始 PO/SO + 处理部分冲销 + 跨期语义；归 successor，保守方向 documented）」**——即 R1.27 实际选择 **方案B Deferred**（owner doc 标注，非方案A 实现）。HEAD 实仓复核：`ErpPurInvoiceReverseApproveProcessor.reverseApprove:22-37` 仅 `postingDispatcher.reverse()` 红冲 AP 凭证 + `doReverseApprove` 设 REJECTED（**零 budgetCommitmentBiz.commit() 调用**）；`ErpPurInvoiceCancelProcessor.cancel:24-38` 同型（仅红冲 + doCancel，**零 commit()**）；`PurReversalListener.rollbackInvoice:70-82` 同型（仅 posted=false + APPROVED→REJECTED，**零 commit()**）。系统不对称：invoice approve → `runCommitmentReleaseOnInvoiceApproveHook` release commitment，invoice reverseApprove/cancel → AP ACTUAL 回退但 commitment 保持已释放。**Q4 裁决=(a)**：audit-remediation 侧方案B Deferred 关闭在 requirement-compliance Q4=(a)（"P0/P1 必须实现，禁方案B，无例外通道"）下**不成立**——P1-MA2-083 虽非严格会计正确性类（承付属预算/承诺管理类，config-gated 默认 false），但 Q4=(a) 对所有 P1 一视同仁禁方案B，须经 MR1（RC-R1.n）实现方案A（按 invoice 关联 PO/SO 反查 + `commit()` 恢复承付 + 处理部分冲销/跨期语义）。**与 A1.14 P1-MA4-017（hr 薪酬计提过账 Deferred 重开）同型**——audit-remediation 方案B Deferred finding 在 RC Q4=(a) 下系统性重开。 |
| **P1-MA2-050**（INLINE reject/withdrawApproval 绕过 isCancelled 守卫） | ✅ resolved (R1.17 done 方案B) | **已落地（实际超越方案B，完成方案A Processor 迁移）**。xbiz source `ErpPurInvoice.xbiz:35-54`（reject/withdrawApproval mutation）已**委托 per-mutation Processor**（`ErpPurInvoiceRejectProcessor.reject` / `ErpPurInvoiceWithdrawApprovalProcessor.withdrawApproval`），不再 INLINE；Processor 经 `AbstractRejectProcessor`/`AbstractWithdrawApprovalProcessor` 框架（validateNotCancelled override :37-39 → `processor.validateNotCancelled` → isCancelled 守卫）。**R1.17 计划声称方案B（INLINE + 守卫），但实仓已实现方案A（Processor 迁移）——方案A 是方案B 的超集**，守卫边界完整。R5.1/R5.8 联动落地（见 P2-MA2-054）。 |
| **P2-MA2-054**（死代码 WithdrawApproval/Reject Processor 未接线） | ✅ closed (MR5 R5.8 done) | **已落地**。xbiz source 已接线 `ErpPurInvoiceRejectProcessor`/`ErpPurInvoiceWithdrawApprovalProcessor`（同 P1-MA2-050 证据）；Receive/Payment/Return/Requisition/Order 全部 Reject/WithdrawApproval Processor 经 xbiz source 接线激活（非死代码）。 |
| **P1-MA2-003**（付款核销缺发票三单匹配完成态复核） | ✅ resolved (plan 2026-07-29-2322-1 方案 A) | **已落地（方案A）**。`PaymentSettler.java:60` 注入 `ThreeWayMatcher threeWayMatcher`；`requireInvoiceForSettle:171-173` config-gated 调 `recheckThreeWayMatchAtSettle`；`recheckThreeWayMatchAtSettle:181-191` 强制 strict 复核（`threeWayMatcher.match(code, lines, Boolean.TRUE)`），失败包装 `ERR_SETTLE_INVOICE_MATCH_NOT_COMPLETED` 含 cause 链；`isSettleRecheckEnabled:177-179` 读 `CONFIG_SETTLE_RECHECK_THREE_WAY_MATCH`（默认 false）；`TestErpPurSettleThreeWayMatchRecheck` 含 3 @Test 覆盖（启用后阻断 + 关闭时跳过 + cause 链）。 |
| **P2-MA2-006**（returns.md red invoice drift） | ✅ resolved (plan 2026-07-29-2322-1) | 归 A1.17 切片（UC-PUR-04/07），本切片仅交叉引用 resolved 状态（arm-index :451 confirmed），不重复核验。 |
| P2-MA2-007（订单审核价格锁缺失） | watch-only | 维持 watch-only——`ErpPurOrderProcessor` 无 `priceLock`/`lockPrice` 字段或方法；L1 §UC-PUR-01 + flow-overview.md §2.1 标注为 watch-only（L2 owner doc 已声明）。本切片不重开。 |
| P2-MA2-008（PaymentSettler 并发核销无锁） | watch-only 归 A2.17 | 维持归 A2.17——本切片不重审并发维度。 |
| P2-MA2-053（三种并行模式 owner doc 未声明） | watch-only | 维持 watch-only——owner doc drift，行为正确。 |
| P2-MA2-055（payment writtenOffStatus 字典语义漂移） | watch-only | 维持 watch-only——字典语义，行为正确。 |

---

## 6. 与 arm-index 衔接（复用 or 新增 裁决，§7）

按 §7 规则，每条 finding 产出前 grep `arm-index.md` 同域同控制点后裁决：

### 复用（同根因同控制点，追加 RC 交叉引用注记，不新建）

| 既有 finding | 本切片对应 | 裁决依据 |
|-------------|----------|---------|
| `P1-MA2-083` | UC-PUR-01 承付恢复（invoice reverseApprove/cancel 不对称） | **同根因同控制点**（AP 发票冲销不恢复承付）。audit-remediation 侧 R1.27 方案B Deferred 关闭；requirement-compliance Q4=(a) 下重开，经 MR1（RC-R1.n）实现方案A。arm-index :284 行追加 RC 交叉引用。**与 A1.14 P1-MA4-017 同型重开模式**（audit-remediation 方案B Deferred finding 系统性重开）。 |

### 新增（新根因/新功能点/新维度）

| 新 finding | UC | 与既有 finding 差异依据 |
|-----------|-----|----------------------|
| `P1-RC-017` | UC-PUR-08 ⑫ 一请购拆多订单（不同供应商） | **新功能点**：L1 `use-cases.md:217` 显式「一个请购可拆多个订单(不同供应商/到货期)」，但 `ErpPurRequisitionProcessor.validateConsistentSupplier:171-186` 强制单一供应商（suppliers.size()!=1 抛 ERR_REQ_MIXED_OR_MISSING_SUPPLIER）。**与既有 finding 不同控制点**：arm-index grep 「requisition」「convertToOrder」「multi-supplier」「split-order」无同域同控制点 finding；P1-MA2-050/054 覆盖审批轴守卫，P1-MA2-083 覆盖承付恢复，P1-MA2-003 覆盖 settle 三单匹配——均非「转订单多供应商拆分」。L1 owner doc `requisition.md` 设计单一供应商是 L2 设计参考（与 L1 冲突以 L1 为准，§4 Q1）。 |
| `P2-RC-011` | UC-PUR-01 ④⑤ businessType 命名漂移（GOODS_RECEIPT/PURCHASE_INVOICE ↔ PURCHASE_INPUT/AP_INVOICE） | **新控制点**：L1 `use-cases.md:42-43` + `:180` 字面「businessType == GOODS_RECEIPT」「businessType == PURCHASE_INVOICE」，实仓 `ErpFinBusinessType.java:13-69` 枚举不存在这两项；经 `PURCHASE_INPUT`（InvPostingDispatcher.java:169）+ `AP_INVOICE`（PurInvoicePostingDispatcher.java:73）语义等价实现。**与 P2-RC-005（A1.10 StockQueue 命名漂移）同型不同控制点**（finance businessType 枚举命名 vs mfg StockQueue 实体命名）。**与 A1.1 业财过账引擎审计不同维度**（A1.1 = 过账正确性，本切片 = L1 字面 businessType 命名漂移）。 |
| `P2-RC-012` | UC-PUR-08 ⑬ 幂等实现漂移（无 `已转订单` 字段；取消全部衍生订单后允许再转化） | **新控制点**：L1 `use-cases.md:221` 字面「请购单.已转订单 == true   // 标记后不可重复转化」，实仓无持久化字段（grep ORM 0 命中）；幂等经 `existsActiveByRequisition`（按 requisitionId 查非作废订单）查询实现；**取消全部衍生订单后允许再次转化**（`TestErpPurRequisitionConvertToOrder#test_convertIsIdempotentButReallowsAfterCancel:147-162` 显式断言）。主路径（活动订单存在时阻断）OK，边界（全取消后允许再转化）比 L1 字面更宽松。**与既有 finding 不同控制点**：arm-index grep 「已转订单」「convertedToOrder」「requisition converted」零命中。 |

### MR1 修复行预留（R1.0 展开器读取本报告后向 MR1 追加 RC-R1.n 实体行）

- **UC-PUR-08 ⑫ 多供应商拆分**（P1-RC-017）：`ErpPurRequisitionProcessor.convertToOrder` 重构为支持按行 supplier 分组生成多个 ErpPurOrder（每个 supplier 一个订单）；解除 `validateConsistentSupplier` 单一供应商强制，改为按 supplier 拆分；调用方 `ConvertToOrderRequest` 改为接受 supplierId→warehouseId/currencyId/deliveryDate 映射或按行调用。**纯 BizModel/Processor 代码逻辑修复，按 roadmap 预授权类目可自动执行，不触发 §5 ask-first**。
- **invoice reverseApprove/cancel 承付恢复**（reuse P1-MA2-083）：`ErpPurInvoiceReverseApproveProcessor.reverseApprove` + `ErpPurInvoiceCancelProcessor.cancel` 新增按 invoice 关联 PO 反查 + config-gated 调 `budgetCommitmentBiz.commit()` 恢复承付；处理部分冲销/跨期语义；sales 侧 `ErpSalInvoiceProcessor` 同型。**纯 Processor 代码逻辑修复，按 roadmap 预授权类目可自动执行，不触发 §5 ask-first**（不触及 ORM/会计过账核心路径——调既有 commit() 入口）。
- **UC-PUR-01 ④⑤ businessType 命名漂移**（P2-RC-011）：修复触及 L1 use-cases + L2 posting.md 真相源（§9 冻结条款须经人工批准）；或仅在 use-cases.md 补注「GOODS_RECEIPT 在 ORM/字典权威名为 PURCHASE_INPUT；PURCHASE_INVOICE 在 ORM/字典权威名为 AP_INVOICE」对齐表（L2 设计参考段落可与代码协同修订），**纯文档修复可自动执行，不触发 §5 ask-first**。
- **UC-PUR-08 ⑬ 幂等实现漂移**（P2-RC-012）：修复触及 L1 use-cases（§9 冻结条款须经人工批准）；或 owner doc `requisition.md` 补注「已转订单 标记当前实现为 existsActiveByRequisition 查询语义（取消全部衍生订单后允许再转化）」，**纯文档修复可自动执行，不触发 §5 ask-first**。

---

## 7. 静态存疑点清单（供 MA4 / A4.1 展开）

> 以下为本切片 L5 无法静态定论、需运行时确认的点（MA4 / A4.1 展开器读取）：

1. **UC-PUR-01 ④ GOODS_RECEIPT/PURCHASE_INPUT 运行时触发链**：receive approve → `triggerIncomingMove` → `IErpInvStockMoveBiz.generateMove` → `InvPostingDispatcher` PURCHASE_INPUT 凭证生成 → `move.getPosted()=true` → `receive.posted=true` 全链运行时确认（HEAD 静态判定 = 全链已实现，A2.1/A2.4 行为证据已证实；运行时可经 `TestErpPurProcureToPayEnd#receiveApprove` E2E 确认凭证落地）。
2. **UC-PUR-01 ⑦ paidStatus 派生运行时一致性**：`PaymentSettler.recomputeInvoicePaid` 在多付款单跨单据核销同一发票时累计 SUM(PaymentLine.amount) 一致性 + 反向负金额行回退 paidStatus（HEAD 静态判定 = 实现 OK，A2.1 已证实；运行时可构造 2 付款单核销 1 发票场景确认）。
3. **UC-PUR-01 ⑧ 应付余额辅助账聚合运行时一致性**：`ErpFinArApItem.openAmount` 在多发票/多付款/部分核销/红冲场景下 SUM == 发票金额 - 已核销金额 恒等式（HEAD 静态判定 = 实现 OK，`TestErpPurProcureToPayEnd:244-272` 强断言；运行时可构造复杂场景确认）。
4. **UC-PUR-08 ⑫ 多供应商拆分运行时阻断**：多供应商请购行 convertToOrder 是否被 `validateConsistentSupplier` 拒绝（HEAD 静态判定 = 是，`TestErpPurRequisitionConvertToOrder#test_convertFailsWhenMixedSuppliers:122-124` 显式断言；P1-RC-017 已确认）。
5. **P1-MA2-083 承付恢复运行时不对称**：invoice approve → commitment release，invoice reverseApprove → AP 红冲但 commitment 保持已释放（HEAD 静态判定 = 不对称，`ErpPurInvoiceReverseApproveProcessor:22-37` 零 commit()；运行时可构造 approve→reverseApprove 序列断言 commitment 余额不归位）。
6. **UC-PUR-08 ⑬ 取消后再转化运行时允许**：cancel 全部衍生订单后 existsActiveByRequisition=false 允许再次转化（HEAD 静态判定 = 允许，`test_convertIsIdempotentButReallowsAfterCancel:147-162` 已断言；P2-RC-012 已确认）。

---

## 8. 过程纪律自检

- [x] **checker 退出码门控核查**：本报告产出后运行 `bash docs/audits/nop-compliance-checker.sh`，actual vs baseline（machine-readable 块 `compliance-baseline.md` 锚点）：

  | 规则 | actual | baseline | 状态 |
  |------|--------|----------|------|
  | R1a/R1b/R1c | 0/0/0 | 0/0/0 | = |
  | R1d | 14 | 14 | = |
  | R2a | 34 | 34 | = |
  | R2b | 229 | 229 | = |
  | R2c | 1382 | 1382 | = |
  | R2d | 34 | 34 | = |
  | R3 | 5 | 5 | = |
  | R4/R5/R7/R8/R11 | 0 | 0 | = |
  | R6 | 2 | 2 | = |
  | R10 | 6 | 6 | = |
  | R12a/R12b/R12c | 69/66/40 | 69/66/40 | = |

  全 16 可计数规则 actual ≤ baseline（全 =），exit 0。**区分门控退出码 vs 纯 reporter 退出码**：checker 脚本是纯 reporter（退出码恒 0），真正门控在 CI workflow（`.github/workflows/compliance.yml`）解析 actual > baseline => sys.exit(1)。本报告**不以** checker 脚本退出码 0 作为门控通过依据。**本审计为只读审计，无生产代码变更，checker 无回归风险**（actual = baseline 全等进一步印证零变更）。
- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告全部 finding 已按 §7 规则 grep arm-index 同域同控制点后给出"复用 or 新增"裁决（P1-MA2-083 复用 + RC 视角重开；P1-RC-017/P2-RC-011/P2-RC-012 新增并附差异依据），无未经比对直接新建的 finding。

---

## Verdict

**FAIL（有需求-实现符合性分歧）**：2 UC 中 **UC-PUR-01 主路径接受 on 6/8 验收标准 + 2 项 P2 命名漂移 + 1 项 P1 承付恢复（reuse 重开）；UC-PUR-08 接受 on 4/6 验收标准 + 1 项 P1 多供应商拆分 + 1 项 P2 幂等漂移**：

- **UC-PUR-08 ⑫ 多供应商拆分（P1-RC-017，最高优先新 finding）**：L1 `use-cases.md:217` 显式「一个请购可拆多个订单(不同供应商/到货期)」，但 `ErpPurRequisitionProcessor.validateConsistentSupplier:171-186` 强制单一供应商，多供应商请购行 convertToOrder 被 ERR_REQ_MIXED_OR_MISSING_SUPPLIER 阻断。须经 MR1 实现按 supplier 拆分生成多 ErpPurOrder。
- **P1-MA2-083（承付恢复，reuse 重开）**：R1.27 计划显式选择方案B Deferred（owner doc 标注），未实现 invoice reverseApprove/cancel 的对称 commit() 恢复。在 requirement-compliance Q4=(a) 下方案B 关闭不成立，须经 MR1 实现方案A（按 invoice 关联 PO 反查 + `budgetCommitmentBiz.commit()`）。**与 A1.14 P1-MA4-017 同型重开模式**——audit-remediation 方案B Deferred finding 在 RC Q4=(a) 下系统性重开。
- **UC-PUR-01 ④⑤ businessType 命名漂移（P2-RC-011）**：L1 字面 GOODS_RECEIPT/PURCHASE_INVOICE 在 ErpFinBusinessType 枚举不存在，经 PURCHASE_INPUT/AP_INVOICE 语义等价实现（行为正确，命名漂移，登记不强制）。
- **UC-PUR-08 ⑬ 幂等实现漂移（P2-RC-012）**：L1 字面「请购单.已转订单==true 标记后不可重复转化」，实仓无持久化字段，幂等经 existsActiveByRequisition 查询，取消全部衍生订单后允许再转化（比 L1 更宽松，登记不强制）。

**零 P0**：UC-PUR-01 主路径全链路行为正确（A2.8 + A2.1 + A1.1 三重证实）+ GOODS_RECEIPT/PURCHASE_INVOICE 命名漂移但语义等价（不破坏会计正确性）+ UC-PUR-08 多供应商拆分缺失不破坏活跃数据（CRUD 可用，仅多供应商拆分场景被拒）+ 承付恢复不对称经 config-gated 默认 false 保护（非默认活跃路径破坏）。

**resolved finding HEAD 复核**：P1-MA2-050（实际超越方案B 完成方案A Processor 迁移）+ P2-MA2-054（xbiz source 已接线激活）+ P1-MA2-003（方案A 落地）已落地；**P1-MA2-083 方案B Deferred 关闭在 Q4=(a) 下重开**（经 MR1 RC-R1.n 实现）。

**本审计不实施修复**（只读审计，结果表面 = 本报告 + arm-index 登记）。finding 修复按 §10 经 MR1（R1.0 展开为 RC-R1.n），触及会计过账逻辑 + ORM 结构的修复行须 ask-first + 独立 plan-audit（§5 保护区域暂停协议）。

---

## 参考

- 真相源：`docs/design/purchase/use-cases.md:19/:204`（UC-PUR-01/08）
- 设计参考：`docs/design/purchase/state-machine.md` + `three-way-match.md` + `README.md` + `docs/design/flow-overview.md §2.1`
- 方法论：`docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议）
- L5 既有证据：`docs/audits/2026-07-28-0230-arm-ma2-purchase-state-machine.md`（A2.8）+ `docs/audits/2026-07-27-1949-arm-ma2-procure-to-pay-e2e.md`（A2.1）+ `docs/audits/2026-08-02-1645-rc-ma1-a1-1-finance-f1-posting.md`（A1.1）+ `docs/audits/2026-07-28-1249-arm-ma2-budget-commitment-release.md`（A2.16 承付）
- resolved 裁决计划：`docs/plans/2026-07-30-0341-1-r1-17-purchase-state-machine-dict-dead-state.md`（R1.17 方案B→实际方案A）+ `docs/plans/2026-07-30-0841-1-r1-27-budget-commitment-release-path.md`（R1.27 方案B Deferred）+ R5.1/R5.8（MR5 Processor 接线）+ `docs/plans/2026-07-29-2322-1-r1-8-pur-settle-three-way-match-recheck.md`（plan 2026-07-29-2322-1 方案A settle 三单匹配复核）
