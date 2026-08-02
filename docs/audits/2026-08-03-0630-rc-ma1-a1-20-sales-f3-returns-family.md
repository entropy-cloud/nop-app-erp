# rc-ma1-a1-20 sales-F3 退货族 需求-实现符合性审计报告

> Report Status: active
> Mission: requirement-compliance
> Work Item: A1.20（MA1 需求追踪矩阵审计 — sales-F3 退货族，UC-SAL-04/05/06/07/09，5 UC）
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§1 五级追踪矩阵 / §2 四级分级判据 / §3 完整枚举 / §4 Q1 真相源层级 + 三判据 / §5 Q4 修复义务 + 保护区域暂停协议 / §6 9 段报告骨架 / §7 arm-index 衔接 / §8 过程纪律自检 / §9 真相源冻结 / §10 MR0/MR1 通道 / §去重协议）
> 锚点：`docs/audits/rc-requirement-baseline-inventory.md`（A1.20 UC 锚点 = UC-SAL-04/05/06/07/09，覆盖率 ✅ 一致）
> L1 真相源：`docs/design/sales/use-cases.md`（机制见 `docs/design/sales/returns.md` — L2 设计参考，非真相源；冲突以 L1 为准）
> L5 既有证据复用：A2.9（`2026-07-28-0400-arm-ma2-sales-state-machine.md`，sales 状态机 + 退货退款闭环 PASS）/ O2C（`2026-07-27-1949-arm-ma2-order-to-cash-e2e.md`，P2-MA2-011 红字发票 doc drift documented simplification）/ A4.5（`2026-07-29-0430-arm-ma4-pur-sal-inv-qa-crm-code-quality.md`，ReturnRefundOrchestrator/ReceiptSettler/SalAcctDocProvider 代码质量 PASS + P1-MA4-021 resolved R2.14）/ A2.5c（`2026-07-27-2315-arm-ma2-finance-arap-settlement-state-machine.md`，ReceiptSettler/reverseSettlement 对称性）

---

## 9. 与 MA2 报告差异增量声明（前置段，对应方法论 §去重协议）

本报告**不复跑 MA2 既有行为审计**，按 §去重协议只补"需求契约↔实际行为"差异：

- **复用 A2.9**（sales 状态机 PASS）：`ErpSalReturn` 状态机（docStatus + approveStatus 两轴；returnStatus/refundStatus/writtenOffStatus 不存储为 ORM 字段，按派生视图实现）、approve 触发链（triggerIncomingMove → flush → triggerPosting → refundOrchestrator.orchestrateRefund → setApproveStatus APPROVED + posted）、reverseApprove 红冲闭环（ensureReversed → 凭证 reverse + 库存 reverse + refundOrchestrator.restoreRefund[MVP no-op]）、SalReversalListener 3 实体降级（rollbackInvoice/rollbackReceipt/rollbackReturn）+ rollbackDelivery deliberate 不对称（P2-MA2-057 watch-only）。**已证实行为作为 L5 既有证据输入**。
- **复用 O2C（P2C e2e）**：P2-MA2-011 红字发票 doc drift（documented simplification — credit-memo-via-return 实现路径，SALES_RETURN 反向 SALES_OUTPUT 借 1401/贷 6401 + ErpFinArApItemGenerator 负向 ArApItem credit memo 替代红字 `ErpSalInvoice` 实体）。
- **复用 A4.5**：ReturnRefundOrchestrator/ReceiptSettler/SalAcctDocProvider 代码质量 PASS；P1-MA4-021（SalReversalListener 3/4 回滚路径零覆盖 + STANDARD 红冲成本不变量零覆盖）**resolved R2.14**。
- **本切片只补的需求视角差异**（候选缺口 #1-#10 见 §5）：换货完全缺失（#5）/ 未交货量更新缺失（#3）/ 暂估应收条件冲减缺失（#4）/ 退货成本策略 1/3 + 配置键未声明（#6）/ 已核销发票 pre-approve 守卫缺失（#7）/ 期间 CLOSED 守卫缺失（#8）/ documented simplification 复核结论（#1/#9）/ 无独立退款单（#2）/ 测试缺口（#10）。

---

## 1. 需求契约原文（5 UC 验收标准逐字引用）

> 来源：`docs/design/sales/use-cases.md`（L1 权威功能契约）；机制引用 `docs/design/sales/returns.md`（L2 设计参考，冲突以 L1 为准）。

### UC-SAL-04 销售退货退款（已开票）— `use-cases.md:99-128`

**场景**：客户退回已开票已收款的货物,需红字发票 + 退款。

**行为链路**（见 returns.md §红字发票处理、§退款处理）：
```
创建退货单(关联原出库单) → 审核通过(入库,恢复库存)
生成红字发票(冲减原应收)
创建退款单 → 核销原收款
```

**可验证断言**：
```
退货单.来源单号 == 出库单.单号
库存余额[物料, 仓库].可用量 += 退货数量   // 货物回库

// 红字发票
存在红字发票: 关联原发票, 金额取负
原发票的应收被冲减

// 退款
存在退款单: 核销原收款
原收款的核销被反向, 释放已核销金额

// 退货状态
退货单.returnStatus = 全额(若退完全部)
退货单.refundStatus = 已退(若款已退)
```

### UC-SAL-05 未开票退货冲减暂估应收 — `use-cases.md:132-145`

**场景**：货物已出库但未开票,客户退货。

**可验证断言**（见 returns.md §红字发票处理）：
```
// 未开票时退货
退货审核 → 库存恢复
冲减暂估应收(若出库时已暂估应收)
不生成红字发票(因无原发票)
订单未交货量回填: 未交货量 = 订单数量 - 已出库 + 退货  (见 §未交货量更新)
```

### UC-SAL-06 退货换货 — `use-cases.md:149-161`

**场景**：客户退回货物并要求换发等值或不同货物。

**可验证断言**（见 returns.md §退货类型）：
```
退货单(returnType=换货) 审核 → 库存恢复
换货生成新销售出库单(关联退货单) → 扣库存
若价差: 补差价开票 或 退款
退货单与换货单通过 sourceBill 双向关联
```

### UC-SAL-07 退货成本处理 — `use-cases.md:165-176`

**场景**：退货入库的成本取值(原出库成本 vs 当前库存成本 vs 协议价)。

**可验证断言**（见 returns.md §退货成本处理）：
```
退货入库成本 = 策略(原出库成本 | 当前库存成本 | 退货协议价)
  // 由配置 erp-sal.return-cost-method 决定
库存余额的成本层(CostLayer)按该成本增加
```

### UC-SAL-09 退货约束校验 — `use-cases.md:200-212`

**场景**：验证退货的各类约束拦截。

**可验证断言**（见 returns.md §异常处理）：
```
退货数量 > 未退货量 → 拒绝
退货关联的发票已核销 → 需先撤回核销再退货
退货期间已结账 → 拒绝(期间控制)
超额退货(超原出库量) → 拒绝
```

---

## 2. 实现代码路径（L3 含行号 + 跨域调用链）

> 实仓源：`module-sales/erp-sal-service/src/main/java/app/erp/sal/service/`。跨域 Facade：`IErpInvStockMoveBiz`（库存）+ `IErpFinVoucherBiz`（财务）+ `IErpMdPartnerBiz`/`IErpMdAcctSchemaBiz`（主数据）。

| 组件 | 文件:line | 职责 |
|------|----------|------|
| BizModel（头） | `entity/ErpSalReturnBizModel.java:22-40`（仅 cancel 委托 Processor；审批经 approval-support.xbiz 标准 source）| 退货单实体 BizModel |
| 编排 Processor（头） | `processor/ErpSalReturnProcessor.java:50-407`（validate → triggerIncomingMove → flush → triggerPosting → refundOrchestrator.orchestrateRefund → setApproveStatus APPROVED + posted，:193-207 doApprove；ensureReversed 红冲闭环 :306-320；validateBusinessRulesForApprove :173-179）| 退货单审批状态机编排 |
| approve per-mutation | `processor/ErpSalReturnApproveProcessor.java:32-55`（requireEntity → isApproved → SoDGuard → validateNotCancelled → validateTransitionForApprove → validateBusinessRulesForApprove → triggerIncomingMove → flush → triggerPosting → refundOrchestrator.orchestrateRefund → setApproveStatus APPROVED + applyPosted）| approve 动作独立 Processor（plan 2026-07-30-1433-2 R5.2） |
| 数量守卫 | `entity/ReturnQtyValidator.java:46-66`（maxReturnable = delivered − alreadyReturned；超抛 `ERR_RETURN_QTY_EXCEED`，单一守卫覆盖"未退货量"+"超额"），:72-101 sumApprovedReturnedByDeliveryLine 聚合（排除当前退货单） | UC-SAL-09 数量守卫 |
| 库存移动构造 | `entity/ReturnStockMoveBuilder.java:34-47`（`relatedBillType=ERP_SAL_RETURN` + `destWarehouseId=returnOrder.warehouseId` + `moveType=INCOMING`），:64 `unitCost = line.unitPrice`（"按原出库成本冲减存货估值口径"，Javadoc:25），:57-68 buildLines | UC-SAL-04/05 库存恢复 + UC-SAL-07 成本策略 1/3（仅"原出库成本"）|
| 跨域库存 Facade | `IErpInvStockMoveBiz.generateMove/findByRelatedBill/reverse`（经 `ErpSalReturnProcessor:59,289,297-298,308,314,319` 调用；`resolveSourceDeliveryMoveId:292-300` 追溯原出库 moveId）| 库存入库 + 追溯链 + 红冲 |
| 过账派发 | `posting/SalReturnPostingDispatcher.java:51-65` tryPost（buildEvent → executor.postEvent，吞异常保持 APPROVED+posted=false）；:84-103 buildEvent（`businessType=SALES_RETURN` + `voucherDate=businessDate` + `billData={TOTAL_COST, TOTAL_AMOUNT_WITH_TAX, CUSTOMER_ID}`）；:109-117 computeTotalCost = Σ qty×unitPrice；:71-82 reverse（硬前置，失败抛出）| UC-SAL-04 红字发票替代路径（credit-memo-via-return） |
| 过账 Provider（sales 侧） | `posting/SalAcctDocProvider.java:83-87`（SALES_RETURN 分支：反向 SALES_OUTPUT，**借 1401 库存商品 / 贷 6401 主营业务成本**，:46-47 SUBJECT_INVENTORY/COGS，:**成本/存货侧 GL，非收入/AR 侧**）| UC-SAL-04 #1 缺口（红字 `ErpSalInvoice` 实体未生成）|
| AR 辅助账（finance 侧） | `module-finance/.../posting/ErpFinArApItemGenerator.java:161-164`（SALES_RETURN case → `DIRECTION_RECEIVABLE` + `SOURCE_BILL_SAL_RETURN` + **负 openAmount credit memo**，:39 cancelOnReverse 红冲时置 status=CANCELLED/openAmount=0）| UC-SAL-04 AR 余额回减功能等价 |
| 退款编排 | `entity/ReturnRefundOrchestrator.java:49-57` orchestrateRefund（找客户已核销发票 `receivedAmount>0`）→ :79-99 reverseSettlementsForInvoice（逐正向 `ErpSalReceiptLine` 调 `receiptSettler.reverseSettlement`）；:64-67 restoreRefund（MVP no-op，退款方式路由 Non-Goal）| UC-SAL-04 退款核销反向（#2 缺口：无独立退款单实体，仅负向 ReceiptLine）|
| 核销器 | `entity/ReceiptSettler.java:116-137` reverseSettlement（生成负向 ReceiptLine + recomputeInvoiceReceived :161-177 + recomputeReceiptWrittenOff）| UC-SAL-04 退款核销反向行为 |
| 红冲监听 | `posting/SalReversalListener.java:43-120`（rollbackInvoice/rollbackReceipt/rollbackReturn 三实体 posted=false + APPROVED→REJECTED；rollbackDelivery :109-120 仅 posted=false deliberate 不对称，Javadoc:114-115）| 财务→销售反向闭环（P2-MA2-057 watch-only） |
| 配置键 | `ErpSalConstants.java:74-76`（仅 `CONFIG_RETURN_REASON_REQUIRED` + `CONFIG_RETURN_APPROVAL_REQUIRED`；**无 `CONFIG_RETURN_COST_METHOD` / `erp-sal.return-cost-method`**）| UC-SAL-07 #6 缺口（配置键未声明）|
| 错误码 | `ErpSalErrors.java:182-209`（`ERR_RETURN_ILLEGAL_STATUS_TRANSITION` / `ERR_RETURN_ILLEGAL_DOC_STATUS_TRANSITION` / `ERR_RETURN_NOT_FOUND` / `ERR_RETURN_LINES_EMPTY` / `ERR_RETURN_QTY_EXCEED` / `ERR_RETURN_DELIVERY_NOT_APPROVED` / `ERR_RETURN_REASON_REQUIRED`；**无 `ERR_RETURN_INVOICE_SETTLED` / `ERR_RETURN_PERIOD_CLOSED`**）| UC-SAL-09 #7/#8 缺口（守卫错误码未声明）|
| ORM 实体 | `module-sales/model/app-erp-sales.orm.xml:857-934`（`ErpSalReturn` 28 列：`docStatus:15` + `approveStatus:16` + `posted:17` + 审计字段；**无 `returnType` / `returnStatus` / `refundStatus` / `writtenOffStatus` / `originalInvoiceId` / `redInvoiceId` 列**）| UC-SAL-06 #5 缺口（returnType 列缺失 → 换货分支不可能）+ #9 缺口（returnStatus/refundStatus 非 ORM 存储）|

---

## 3. 测试断言证据（L4 注明断言强度）

> 测试源：`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/`。E2E：`tests/e2e/business-actions/sal-return.action.spec.ts`。强度评级对齐 MA5（A5.6 E2E effectiveness）。

| 测试文件#方法 | 覆盖 UC | 断言强度 | 证据摘要 |
|--------------|---------|---------|---------|
| `TestErpSalReturnApproval.java`（7 方法） | UC-SAL-09 状态机 + 守卫 | 强（状态机/客户激活/源出库/原因守卫全断言）| happy path + reject/resubmit + illegalFromUnsubmitted + customerInactiveRejected + sourceDeliveryNotApprovedRejected + cancelApprovedReversesMove + reasonRequiredRejected |
| `TestErpSalReturnQty.java`（4 方法）| UC-SAL-09 数量守卫 | **强**（数量上限精确断言）| testReturnQtyOverDeliveredRejected（12>10 拒）+ testPartialReturnAllowed（部分允）+ testSecondReturnCumulativeLimit（累积 5>3 拒）+ testSecondReturnWithinCumulativeLimit（累积内允） |
| `TestErpSalReturnInventory.java`（3 方法）| UC-SAL-04/05 库存恢复 | 强（行级余额）| testApproveGeneratesIncomingMoveAndStockIncrease（20+4=24 行级）+ testApproveIdempotent + testReverseApproveRestoresStock（24−4=20）|
| `TestErpSalReturnTrace.java`（2 方法）| UC-SAL-04 追溯链 | 强（双向）| testReturnMoveLinkedToSourceDeliveryMove（originReturnedMoveId）+ testReturnTraceFromReturnMoveReachesOriginal |
| `TestErpSalReturnPosting.java`（2 方法）| UC-SAL-04 过账 + 反向 | **强（行级凭证）** | testApproveGeneratesSalesReturnVoucherAndNegativeArItem（totalDebit/totalCredit=20+2 行 + ArApItem.openAmountFunctional=−RETURN_WITH_TAX 行级）+ testReverseApproveCancelsArItemAndRestoresBalance |
| `TestErpSalReturnRefund.java`（2 方法）| UC-SAL-04 退款核销反向 | 强（行级 ReceiptLine + receivedStatus）| testReceivedReturnReversesSettlement（已收→反向核销负向 ReceiptLine + receivedStatus=UNRECEIVED 行级）+ testUnreceivedReturnNoSettlementReversal（未收→no-op）|
| `TestErpSalReturnRefundEndToEnd.java`（2 方法）| UC-SAL-04 端到端 + 异常拒 | 强（全链+异常）| testSalesReturnRefundEndToEnd + testSalesReturnRefundExceptions |
| E2E `sal-return.action.spec.ts` | UC-SAL-04 审核路径 + 拒绝/取消 | 行级凭证强 + 状态级 | 审核路径行级凭证 1401/6401 强；拒绝+取消状态级 |

**测试缺口**（候选缺口 #10）：
- UC-SAL-05 暂估应收冲减：仅 no-op 测（功能缺失导致无可测路径，下游 P1-RC-024）
- UC-SAL-06 换货：路径不存在（功能完全缺失，下游 P1-RC-025）
- UC-SAL-07 成本策略切换：无测试（功能 1/3，下游 P1-RC-026）
- UC-SAL-09 已核销发票 pre-approve 拒：无测试（守卫缺失，下游 P1-RC-027）
- UC-SAL-09 期间 CLOSED 拒：无测试（守卫缺失，下游 P1-RC-028）
- P1-MA4-021（SalReversalListener 3/4 回滚路径零覆盖 + STANDARD 红冲成本不变量零覆盖）**resolved R2.14**（A4.5 §resolved 复核确认）

---

## 4. 运行时行为证据（L5 — 复用 MA2 + E2E）

| 行为 | 证据来源 | 结论 |
|------|---------|------|
| 退货审核 → 库存恢复（行级） | A2.9 §维度 3 + `TestErpSalReturnInventory` 行级 20+4=24 | **行为已证实**（库存域 `IErpInvStockMoveBiz.generateMove` INCOMING + StockMoveBookkeeper 增加余额）|
| 退货审核 → SALES_RETURN 反向 SALES_OUTPUT 凭证 | O2C §2.4 + `TestErpSalReturnPosting` 行级凭证 | **行为已证实**（借 1401/贷 6401 成本/存货侧；GL 平衡）|
| 退货审核 → AR 余额回减（credit memo）| O2C §2.4 + `TestErpSalReturnPosting` 行级 ArApItem | **行为已证实**（ErpFinArApItemGenerator 负 openAmount；sumOpen 自然减计 receivableBalance）|
| 退货审核 → 退款核销反向 | A2.9 §维度 4(g) + `TestErpSalReturnRefund` 行级 ReceiptLine | **行为已证实**（ReceiptSettler.reverseSettlement 生成负向 ReceiptLine + recomputeInvoiceReceived 回退 receivedStatus）|
| 退货反审核 → 红冲闭环 | A2.9 §维度 4 + `TestErpSalReturnRefund` 反向 + `TestErpSalReturnInventory#testReverseApproveRestoresStock` | **行为已证实**（ensureReversed → 凭证 reverse + 库存 reverse + posted=false + APPROVED→REJECTED）|
| SalReversalListener 反向（finance→sales）| A2.9 §维度 4 + A4.5 resolved R2.14 | **行为已证实**（3 实体降级 + delivery deliberate 不对称，P2-MA2-057 watch-only）|
| 数量守卫（未退货量 + 超额） | `TestErpSalReturnQty` 4 方法 | **行为已证实**（单一守卫 `ReturnQtyValidator:46-66` 覆盖 L1 两条断言）|
| **未交货量更新（UC-SAL-05）** | grep `undeliveredQty\|未交货量` 全生产代码 0 命中 | **行为缺失**（#3，P1-RC-023）|
| **暂估应收条件冲减（UC-SAL-05）** | `SalReturnPostingDispatcher.buildEvent:84-103` 无条件分支 | **行为缺失**（#4，P1-RC-024）|
| **换货分支（UC-SAL-06）** | ORM 无 `returnType` 列 + grep `换货\|exchange.*return\|sourceBill` 生产代码 0 命中 | **行为缺失**（#5，P1-RC-025）|
| **退货成本策略切换（UC-SAL-07）** | `ReturnStockMoveBuilder:64 unitCost = line.unitPrice`（仅"原出库成本"）+ 配置键 `erp-sal.return-cost-method` 未声明 | **行为部分**（1/3 策略，#6，P1-RC-026）|
| **已核销发票 pre-approve 守卫（UC-SAL-09）** | 无 `ERR_RETURN_INVOICE_SETTLED` + post-approve silent 反向替代 | **行为偏离**（#7，P1-RC-027）|
| **期间 CLOSED 守卫（UC-SAL-09）** | 无 `requirePeriodOpen` + 无 `ERR_RETURN_PERIOD_CLOSED` | **行为缺失**（#8，P1-RC-028）|

---

## 5. 符合性结论（五级追踪矩阵 + 每 UC 结论）

### 5.1 五级追踪矩阵（5 UC × 5 列，逐 UC 一行）

| UC | L1 use-case 需求契约 | L2 owner doc 契约 | L3 代码路径 | L4 测试断言 | L5 运行时行为 | 符合性结论 |
|----|---------------------|------------------|------------|------------|--------------|-----------|
| **UC-SAL-04** 销售退货退款（已开票） | `use-cases.md:99-128`（行为链路 + 9 条断言：来源回链/库存恢复/红字发票存在/原应收冲减/退款单存在/原核销反向/returnStatus/refundStatus）— 验收标准原文见 §1 | `returns.md §退货流程:29-73` + `§红字发票处理:178-241` + `§退款处理:277-306`（设计参考；`:213-241` 红字发票 doc drift — impl 以 SALES_RETURN posting + credit memo 替代红字 `ErpSalInvoice` 实体）| `ErpSalReturnApproveProcessor:32-55`（approve 编排）+ `ErpSalReturnProcessor:193-207`（doApprove）+ `ReturnStockMoveBuilder:34-47,64`（库存恢复 + 单价透传）+ `SalReturnPostingDispatcher:51-65,84-103`（SALES_RETURN 过账）+ `SalAcctDocProvider:83-87`（反向 SALES_OUTPUT 借 1401/贷 6401，**成本/存货侧非收入/AR 侧**）+ `ErpFinArApItemGenerator:161-164`（负向 ArApItem credit memo，AR 余额回减）+ `ReturnRefundOrchestrator:49-57,79-99`（退款核销反向）+ `ReceiptSettler:116-137`（负向 ReceiptLine）；跨域 `IErpInvStockMoveBiz.generateMove/findByRelatedBill/reverse` + `IErpFinVoucherBiz.post/reverse`（经 SalPostingExecutor）| `TestErpSalReturnInventory#testApproveGeneratesIncomingMoveAndStockIncrease`（行级余额 20+4=24，**强**）+ `TestErpSalReturnPosting#testApproveGeneratesSalesReturnVoucherAndNegativeArItem`（行级凭证 + ArApItem 负 openAmount，**强**）+ `TestErpSalReturnRefund#testReceivedReturnReversesSettlement`（行级 ReceiptLine + receivedStatus=UNRECEIVED，**强**）+ `TestErpSalReturnTrace`（双向追溯，**强**）+ `TestErpSalReturnRefundEndToEnd`（端到端 + 异常拒，**强**）+ E2E `sal-return.action.spec.ts`（行级凭证 1401/6401 强 + 拒绝/取消状态级） | 复用 A2.9 §维度 3/4 PASS + O2C §2.4（P2-MA2-011 documented simplification）+ A4.5（resolved R2.14）；行为已证实：库存恢复 / SALES_RETURN 反向 SALES_OUTPUT / AR 余额 credit memo 回减 / 退款核销反向 / 红冲闭环 | **P2**（#1 reuse P2-MA2-011 documented simplification 维持 watch-only，§4 复核结论见 §6；#2 无独立退款单 P2-RC-022 新建 watch-only；#9 reuse P2-MA2-058 维持 watch-only；其余断言"接受"）|
| **UC-SAL-05** 未开票退货冲减暂估应收 | `use-cases.md:132-145`（4 条断言：库存恢复 / 冲减暂估应收[条件] / 不生成红字发票 / 未交货量回填）— 验收标准原文见 §1 | `returns.md §红字发票处理:178-211`（未开票退货：仅冲减应收；`:245-258` 凭证分录示例）+ `§未交货量更新:322-338`（设计参考）| 库存恢复：同 UC-SAL-04（`ReturnStockMoveBuilder` + `IErpInvStockMoveBiz`）；**#3 未交货量更新缺失**（grep `undeliveredQty\|未交货量` 全生产代码 0 命中；`ErpSalReturnProcessor.doApprove:193-207` 不调任何订单量更新）；**#4 暂估应收条件冲减缺失**（`SalReturnPostingDispatcher.buildEvent:84-103` 无论是否暂估应收统一发 SALES_RETURN posting + credit memo，无分支）| 库存恢复经 `TestErpSalReturnInventory` 间接覆盖；**暂估应收冲减**：仅 no-op 测；**未交货量更新**：无测试（功能缺失）| 库存恢复行为已证实；暂估应收条件冲减行为缺失（无分支判定）；未交货量更新行为缺失（无字段/逻辑）| **P1**（#3 P1-RC-023 未交货量更新缺失 + #4 P1-RC-024 暂估应收条件冲减缺失；§2 P1① 功能完全缺失 + §2 P1② 异常路径未实现）|
| **UC-SAL-06** 退货换货 | `use-cases.md:149-161`（4 条断言：returnType=换货审核 → 库存恢复 / 换货生成新销售出库单 → 扣库存 / 价差补开票或退款 / sourceBill 双向关联）— 验收标准原文见 §1 | `returns.md §退货类型:20-26`（换货：退货同时重新发货）+ `§退货流程:29-73`（设计参考）| **#5 完全缺失**：ORM `app-erp-sales.orm.xml:857-934` `ErpSalReturn` **无 `returnType` 列**；grep `换货\|exchange.*return\|sourceBill` 全 `module-sales/erp-sal-service/src/main/` **0 命中**；无换货分支 / 无换货新出库单生成 / 无价差开票退款 / 无 sourceBill 双向关联 | **路径不存在**：无测试可构造 | 行为完全缺失 | **P1**（#5 P1-RC-025 换货完全缺失；§2 P1① 功能完全缺失；**须人工确认 product-scope 是否裁剪**：若 product-scope 含换货则 P1 强制实现，若裁剪则按 §4 (iii) 改真相源非降级；当前 product-scope 未显式裁剪，按 Q4 默认 P1 强制实现）|
| **UC-SAL-07** 退货成本处理 | `use-cases.md:165-176`（2 条断言：退货入库成本 = 策略[原出库成本 \| 当前库存成本 \| 退货协议价]由 `erp-sal.return-cost-method` 决定 / CostLayer 按该成本增加）— 验收标准原文见 §1 | `returns.md §退货成本处理:145-174`（三方式表 + 批次追溯）+ `../finance/costing-methods.md`（设计参考；`:461` 配置项 `erp-sal.return-cost-method` 默认 `original`）| **#6 策略 1/3 + 配置键未声明**：`ReturnStockMoveBuilder.java:64 req.setUnitCost(line.getUnitPrice())`（**仅"原出库成本"**，Javadoc:25 "按原出库成本冲减存货估值口径"）；`SalReturnPostingDispatcher:109-117 computeTotalCost = Σ qty×unitPrice`；**配置键 `erp-sal.return-cost-method` 未声明**（`ErpSalConstants.java:74-76` 仅 `return-reason-required`+`return-approval-required`；跨模块 grep `return-cost-method\|returnCostMethod\|CONFIG_RETURN_COST` 0 命中）；CostLayer 经库存域 StockMoveBookkeeper 间接更新（无销售域直写） | 无成本策略切换测试（功能缺失导致无可测路径） | 行为部分（1/3 策略，"原出库成本"已实现；"当前库存成本"/"退货协议价"缺失；配置键未声明）| **P1**（#6 P1-RC-026 退货成本策略 1/3 + 配置键未声明；§2 P1① 功能实质偏离验收标准 — L1 显式 3 策略 + 配置键，实现仅 1/3 + 配置键未声明）|
| **UC-SAL-09** 退货约束校验 | `use-cases.md:200-212`（4 条断言：退货数量 > 未退货量 → 拒绝 / 退货关联发票已核销 → 需先撤回核销再退货 / 退货期间已结账 → 拒绝[期间控制] / 超额退货 → 拒绝）— 验收标准原文见 §1 | `returns.md §异常处理:390-408`（退货数量限制表 + 批次处理异常）+ `§业务规则:415-422`（4 退货约束）+ `../finance/period-close.md`（设计参考）| **已实现守卫**：`ReturnQtyValidator.java:46-66`（maxReturnable = delivered − alreadyReturned；超抛 `ERR_RETURN_QTY_EXCEED`，单一守卫覆盖"未退货量"+"超额"断言 1+4）+ `requireSourceDeliveryApproved:230-241`（`ERR_RETURN_DELIVERY_NOT_APPROVED`）+ `requireReasonIfConfigured:243-255`（`ERR_RETURN_REASON_REQUIRED`）+ `requireCustomerActive:357-367` + `requireLinesNonEmpty:350-355`；**#7 已核销发票 pre-approve 守卫缺失**（无 `ERR_RETURN_INVOICE_SETTLED`，`ErpSalErrors.java:182-209` 无；改为 post-approve `ReturnRefundOrchestrator.reverseSettlementsForInvoice:79-99` 静默反向）；**#8 期间 CLOSED 守卫缺失**（无 `requirePeriodOpen`/`isPeriodClosed`，`ErpSalReturnProcessor.validateBusinessRulesForApprove:173-179` 无；无 `ERR_RETURN_PERIOD_CLOSED`）| `TestErpSalReturnApproval`（7 方法，**强** — 状态机/客户激活/源出库/原因守卫）+ `TestErpSalReturnQty`（4 方法，**强** — 12>10 拒 + 部分允 + 累积 5>3 拒 + 累积内允）；**缺口测试**：已核销发票 pre-approve 拒（无）+ 期间 CLOSED 拒（无）| 数量守卫行为已证实（强）；已核销发票守卫行为偏离（post-approve silent 反向替代 pre-approve reject）；期间 CLOSED 守卫行为缺失 | **P1**（#7 P1-RC-027 + #8 P1-RC-028；§2 P1② 异常路径未实现；#8 涉期间控制属会计正确性类，Q4 无例外）|

### 5.2 候选缺口分级汇总（10 项）

| 缺口# | UC | 描述 | 分级 | 命中判据 | finding ID |
|-------|----|------|------|---------|-----------|
| #1 | UC-SAL-04 | 红字发票 credit-memo 替代（无红字 `ErpSalInvoice` 实体，GL 击成本/存货侧非收入/AR 侧）| **P2 watch-only** | §2 P2③（documented simplification §4 (i) 满足，复核结论见 §6.1）| **reuse P2-MA2-011**（追加 RC A1.20 交叉引用 + §4 复核注记）|
| #2 | UC-SAL-04 | 无独立退款单（仅负向 ReceiptLine + ReceiptSettler.reverseSettlement）| **P2 watch-only** | §2 P2①（次要验收标准未完全满足，主路径[AR 余额回减]OK）| **新建 P2-RC-022** |
| #3 | UC-SAL-05 | 未交货量更新缺失（无 `undeliveredQty` 字段/逻辑）| **P1** | §2 P1①（功能完全缺失 — L1 派生断言不可满足）| **新建 P1-RC-023** |
| #4 | UC-SAL-05 | 暂估应收条件冲减缺失（`buildEvent:84-103` 无条件分支）| **P1** | §2 P1①（功能完全缺失 — L1 条件分支未实现）| **新建 P1-RC-024** |
| #5 | UC-SAL-06 | 换货完全缺失（无 `returnType` 列 + 无换货分支/新出库单/sourceBill）| **P1**（须人工确认 product-scope 范围裁剪）| §2 P1①（功能完全缺失）| **新建 P1-RC-025** |
| #6 | UC-SAL-07 | 退货成本策略 1/3（仅"原出库成本"，配置键 `erp-sal.return-cost-method` 未声明）| **P1** | §2 P1①（功能实质偏离验收标准 — L1 显式 3 策略 + 配置键）| **新建 P1-RC-026** |
| #7 | UC-SAL-09 | 已核销发票 pre-approve 守卫缺失（无 `ERR_RETURN_INVOICE_SETTLED`，改 post-approve 静默反向）| **P1** | §2 P1②（异常路径未实现 — L1 "需先撤回核销再退货"控制点偏离）| **新建 P1-RC-027** |
| #8 | UC-SAL-09 | 期间 CLOSED 守卫缺失（无 `requirePeriodOpen` / `ERR_RETURN_PERIOD_CLOSED`）| **P1** | §2 P1②（异常路径未实现）+ §5 Q4（会计正确性类 — 期间控制）| **新建 P1-RC-028** |
| #9 | UC-SAL-04 | returnStatus/refundStatus 非 ORM 存储（按派生视图实现）| **P2 watch-only** | §2 P2③（documented simplification §4 (i) 满足，复核结论见 §6.1）| **reuse P2-MA2-058**（追加 RC A1.20 交叉引用 + §4 复核注记）|
| #10 | UC-SAL-05/06/07/09 | 测试缺口（暂估应收 no-op / 换货路径不存在 / 成本策略切换无测试 / 已核销发票 pre-approve 拒无 / 期间 CLOSED 拒无）| **P2 successor**（下游于 #3-#8 P1 实现，MA5 测试覆盖维度）| §2 P2① | **不新建**（derivative — MR1 实现 #3-#8 时测试建立义务自动激活，详见 §6.4）|

### 5.3 每 UC 总结论

- **UC-SAL-04**：**P2**（#1 reuse P2-MA2-011 + #2 P2-RC-022 + #9 reuse P2-MA2-058；其余断言"接受"——库存恢复/AR 余额回减/退款核销反向/红冲闭环/追溯链行为经 A2.9+O2C+A4.5+单测/E2E 三重证实）
- **UC-SAL-05**：**P1**（#3 P1-RC-023 + #4 P1-RC-024；§2 P1①）
- **UC-SAL-06**：**P1**（#5 P1-RC-025；§2 P1①；须人工确认 product-scope 范围裁剪）
- **UC-SAL-07**：**P1**（#6 P1-RC-026；§2 P1①）
- **UC-SAL-09**：**P1**（#7 P1-RC-027 + #8 P1-RC-028；§2 P1② + §5 Q4 会计正确性类）

---

## 6. 与 arm-index 衔接（复用 or 新增 裁决）

### 6.1 既有 documented simplification §4 三判据复核结论

| Finding | 简化内容 | §4 (i) plan-audit 通过记录 | §4 (ii) owner doc 显式标注 + 可追溯人工批准 | §4 (iii) product-scope 范围裁剪登记 | 复核结论 |
|---------|---------|---------------------------|---------------------------------------------|-------------------------------------|---------|
| **P2-MA2-011**（#1 红字发票 credit-memo 替代）| 无红字 `ErpSalInvoice` 实体，以 SALES_RETURN 反向 SALES_OUTPUT（借 1401/贷 6401）+ 负向 ArApItem credit memo 替代 | ✅ plan `2026-07-02-0456-2`（sales-return-and-refund）`Draft Review Record` 含独立子代理 ses_0e0839e3bffe.../ses_0e0774747ffe... 通过记录；plan `2026-07-29-2322-1`（R1.8 P2P 同型 P2-MA2-006）独立 plan-audit 通过 + closure-audit 通过 | ⚠️ owner doc `returns.md §红字发票处理:213-241` doc drift 注记存在（plan 0456-2 Closure Gates line 143 落地）但批准来源为 AI 子代理（无 git log 人工批准痕迹/无讨论文档）；按 §4 段落"代理独立审计通过 = 审计裁决质量证据...不算人工批准"严格解释**不满足**（与 A1.17 P2-RC-015 reuse P2-MA2-006 同型先例一致，A1.17 实务上接受 §4 (i) 即维持 watch-only，本切片沿用此先例保持 RC mission 内一致性）| ❌ `product-scope.md` 未显式裁剪红字发票实体 | **沿用 A1.17 先例：§4 (i) 满足（独立 AI plan-audit 通过），维持 P2 watch-only**。**残留风险**：严格解释下（AI plan-audit ≠ 人工批准）§4 三判据均不满足，应重新打开为 P1 入 MR1；本切片记录此严格解释 gap 供后续 MR1 人工批准 backfill（建议：MR1 修复 P1-RC-023..028 时由人工 reviewer 显式批准 P2-MA2-011/P2-MA2-058 简化决策，将 §4 (ii) 人工批准痕迹补齐）。**不静默接受**：§4 复核结论已显式记录。|
| **P2-MA2-058**（#9 returnStatus/refundStatus 非 ORM 存储）| returnStatus/refundStatus 两轴不存储为 ORM 字段，按派生视图实现（部分/全额退货 = 源出库行累计退货进度派生；退款进度 = AR 辅助账 open/reconciled 状态派生）| ✅ plan `2026-07-02-0456-2` `Draft Review Record` 独立子代理通过记录 | ⚠️ owner doc `returns.md §退货单状态机:88-93` 显式漂移注记（"实现偏离说明（计划 0456-2）"段落），AI-written；按 §4 严格解释**不满足**（同 #1）| ❌ product-scope 未裁剪 | **沿用 A1.17/#1 先例：§4 (i) 满足，维持 P2 watch-only**。残留风险同 #1（严格解释 gap），MR1 人工批准 backfill 建议同上。**功能等价性**：L1 line 124-125 value-based 断言（returnStatus/refundStatus 值）可通过派生视图满足（值可计算），仅持久化/列表筛选便利性弱化；与 #1 不同（#1 L1 line 116 existence 断言"存在红字发票"严格不可满足，但 GL 净零 + AR 余额回减功能等价），故 #9 维持 P2 watch-only 更稳健。|

### 6.2 arm-index grep 比对 + 复用 or 新增裁决（§7 规则）

> 对每条候选缺口 grep arm-index 同域同控制点后裁决（禁止未经比对新建）。

| 缺口# | grep 关键词 | 既有 finding | 裁决 |
|-------|------------|-------------|------|
| #1 | 「红字发票」「credit memo」「SALES_RETURN」「red invoice」「P2-MA2-011」 | `P2-MA2-011`（O2C，doc drift，watch-only MR1 owner-doc 更新）+ 同型 `P2-MA2-006`（P2P，resolved plan 2026-07-29-2322-1 R1.8 documented simplification）| **复用 P2-MA2-011**（追加 RC A1.20 交叉引用 + §4 复核注记，不新建编号）|
| #2 | 「独立退款单」「RefundOrder」「refund order」「no independent refund」「负向 ReceiptLine」 | 无 sales 域同控制点 finding | **新建 P2-RC-022** |
| #3 | 「undeliveredQty」「未交货量」「undelivered quantity」 | 无命中 | **新建 P1-RC-023** |
| #4 | 「暂估应收」「estimated receivable」「accrued revenue」 | 无 sales 域同控制点 finding（finance 域不同控制点）| **新建 P1-RC-024** |
| #5 | 「换货」「exchange」「returnType」「sourceBill」「UC-SAL-06」 | 无 sales 域同控制点 finding | **新建 P1-RC-025** |
| #6 | 「return-cost-method」「returnCostMethod」「退货成本」「CostLayer 退货」 | 无 sales 域同控制点 finding（finance/inventory 域不同控制点）| **新建 P1-RC-026** |
| #7 | 「ERR_RETURN_INVOICE_SETTLED」「invoice settled return」「pre-approve 守卫」 | 无 sales 域同控制点 finding（P2-MA2-041 finance 核销无 CLOSED_FINAL 守卫不同域不同控制点）| **新建 P1-RC-027** |
| #8 | 「ERR_RETURN_PERIOD_CLOSED」「period closed return」「requirePeriodOpen」 | 无 sales 域同控制点 finding（P1-MA2-021 finance CLOSED_FINAL 凭证锁定是过账侧 vs 本 finding 退货审核侧，不同控制点；P2-MA2-015 期间配比归 A2.3 不同维度）| **新建 P1-RC-028** |
| #9 | 「returnStatus」「refundStatus」「writtenOffStatus」「P2-MA2-058」 | `P2-MA2-058`（A2.9，owner doc drift watch-only MR1）| **复用 P2-MA2-058**（追加 RC A1.20 交叉引用 + §4 复核注记，不新建编号）|
| #10 | 「测试缺口」「test gap」「UC-SAL-05/06/07/09 测试」 | P1-MA4-021（resolved R2.14，SalReversalListener 3/4 回滚 + STANDARD 红冲成本不变量）| **不新建**（derivative — 测试缺口下游于 #3-#8 功能缺失；MR1 实现 P1-RC-023..028 时测试建立义务自动激活）|

### 6.3 双向可追溯（finding ↔ 修复行预留 MR0/MR1）

| Finding ID | 域 | UC | 分级 | 目标 MR | 触及保护区域 | 修复状态 |
|-----------|---|----|------|--------|------------|---------|
| `P1-RC-023` | sales | UC-SAL-05 | P1 | MR1（R1.0 → RC-R1.n）| 否（纯 BizModel/Processor 代码逻辑 — 订单未交货量回填 + 可能新增 `undeliveredQuantity` 派生字段；若新增 ORM 列则触发 §5 ORM ask-first）| todo（本审计仅登记，不实施修复）|
| `P1-RC-024` | sales | UC-SAL-05 | P1 | MR1（R1.0 → RC-R1.n）| 否（纯 BizModel/Processor 代码逻辑 — `SalReturnPostingDispatcher.buildEvent` 增条件分支判定暂估应收状态）| todo |
| `P1-RC-025` | sales | UC-SAL-06 | P1（须人工确认 product-scope 范围裁剪）| MR1（R1.0 → RC-R1.n）/ §4 (iii) product-scope 修订（若裁剪）| **是 — ORM 结构变更**（`ErpSalReturn` 增 `returnType` 列 + 可能新增 `ErpSalReturnExchangeLink` 关联实体）| todo（**须 ask-first + 独立 plan-audit §5 ORM 结构变更类**；**先须人工确认 product-scope 是否裁剪换货功能：若裁剪 → 按 §4 (iii) 改 product-scope 真相源非降级；若未裁剪 → P1 强制实现 §2 P1① Q4 无例外）|
| `P1-RC-026` | sales | UC-SAL-07 | P1 | MR1（R1.0 → RC-R1.n）| 否（纯 BizModel + config key 补充 — `ErpSalConstants` 增 `CONFIG_RETURN_COST_METHOD` + `ReturnStockMoveBuilder` 按 config 切换策略；"当前库存成本"读取跨域 `IErpInvStockBalanceBiz`，"退货协议价"读取退货行协议价字段）| todo |
| `P1-RC-027` | sales | UC-SAL-09 | P1 | MR1（R1.0 → RC-R1.n）| 否（纯 BizModel/Processor + ErrorCode — `ErpSalErrors` 增 `ERR_RETURN_INVOICE_SETTLED` + `ErpSalReturnProcessor.validateBusinessRulesForApprove` 增 pre-approve 守卫调 `IErpFinReconciliationBiz` 查核销状态）| todo |
| `P1-RC-028` | sales | UC-SAL-09 | P1 | MR1（R1.0 → RC-R1.n）| 否（纯 BizModel/Processor + ErrorCode — `ErpSalErrors` 增 `ERR_RETURN_PERIOD_CLOSED` + `ErpSalReturnProcessor.validateBusinessRulesForApprove` 增 `requirePeriodOpen` 调 `IErpFinAccountingPeriodBiz` 查期间状态；与 finance `ErpFinPostingProcessor.resolveOpenPeriod:524-527` 期间控制一致）| todo |
| `P2-RC-022` | sales | UC-SAL-04 | P2 | successor watch-only（P2 登记不强制）| 否（架构性裁决：负向 ReceiptLine 替代独立 RefundOrder 实体，功能等价；如需独立 RefundOrder 则触及 ORM 结构变更须 ask-first）| todo |
| `P2-MA2-011`（reuse #1）| docs+sales | UC-SAL-04 | P2 | watch-only MR1 owner-doc 更新 | 否（纯文档修复 — `returns.md §红字发票处理:213-241` 反映 credit-memo-via-return 实现；MR1 人工批准 backfill 建议：reviewer 显式批准简化决策补齐 §4 (ii) 痕迹）| todo（追加 RC A1.20 §4 复核注记）|
| `P2-MA2-058`（reuse #9）| sales/docs | UC-SAL-04 | P2 | watch-only MR1 owner-doc 更新 | 否（纯文档 — `returns.md:88-93` 触发条件满足时再加 `ErpSalDeliveryLine.returnedQuantity` 冗余列 + 重新 codegen；本期维持派生视图）| todo（追加 RC A1.20 §4 复核注记）|

### 6.4 #10 测试缺口裁决说明

#10 测试缺口（暂估应收 no-op / 换货路径不存在 / 成本策略切换无测试 / 已核销发票 pre-approve 拒无 / 期间 CLOSED 拒无）**不新建独立 finding**，理由：
- 暂估应收/换货/成本策略切换/已核销发票 pre-approve/期间 CLOSED 5 项测试缺口**全部下游于 #3-#8 P1 功能缺失**（功能不存在则无测试可构造）。
- MR1 实现 P1-RC-023..028 时**测试建立义务自动激活**（roadmap MA5 测试覆盖维度 + R6.x per-mutation Processor 拆分 successor 触发条件）：每条 P1 修复 plan 须含相应测试（正向 + 负向 + 边界），由该 plan 独立 closure audit 核验。
- P1-MA4-021（SalReversalListener 3/4 回滚路径 + STANDARD 红冲成本不变量零覆盖）**resolved R2.14**，与本切片不重叠（不同控制点：SalReversalListener 回滚 vs UC-SAL-05/06/07/09 功能性测试）。

---

## 7. 静态存疑点清单（供 MA4 展开）

> L5 无法静态定论、需运行时确认的点。每存疑点一行；MA4（A4.1/A4.2 展开器）运行时验证后回填结论。

- **SP-1**（#1 reuse P2-MA2-011）：负向 ArApItem credit memo 对客户应收余额（`receivableBalance`）的净效果在不同期间配比场景下是否始终等价于红字 `ErpSalInvoice`（特别是跨月出库-开票场景：X 月出库 + X+1 月开票 + X+1 月退货 → GL 收入科目余额在 X+1 月是否正确冲减，还是延至期末结账）。
- **SP-2**（#6 P1-RC-026）：退货成本在不同库存策略（FIFO/MOVING_AVERAGE/STANDARD/SPECIFIC）下，`ReturnStockMoveBuilder:64 unitCost=line.unitPrice`（"原出库成本"）经 StockMoveBookkeeper 写入 CostLayer 后，与"当前库存成本"（MA 加权平均/FIFO 队列首项）的实际数值偏差范围（影响"原出库成本"策略与"当前库存成本"策略选择差异）。
- **SP-3**（#8 P1-RC-028）：期间 CLOSED 状态下退货审核实际是否被拦截（`ErpSalReturnProcessor.validateBusinessRulesForApprove:173-179` 无 `requirePeriodOpen`，但 finance 引擎 `ErpFinPostingProcessor.resolveOpenPeriod:524-527` 期间控制可能在过账环节间接拦截 — 需运行时确认 SALES_RETURN 过账路径是否经此守卫）。
- **SP-4**（#7 P1-RC-027）：`ReturnRefundOrchestrator.reverseSettlementsForInvoice:79-99` post-approve 静默反向在已核销发票高并发场景下的实际行为（多个退货单并发触发同一发票核销反向 → ReceiptLine 写入竞态，A2.9 §维度 5 已交接 A2.17 但仅状态机维度，本切片从需求契约维度复核确认 — L1 要求"先撤回核销再退货"控制点属 pre-approve，post-approve 静默反向属行为偏离）。
- **SP-5**（#5 P1-RC-025）：换货功能完全缺失，须运行时确认 product-scope 是否裁剪（`docs/requirements/product-scope.md` 销售域范围 — 若隐含含换货则 P1 强制实现，若裁剪则按 §4 (iii) 改真相源非降级）。

**P0 即时通道**：本切片**未触发 P0**（最高级 = P1，无活跃数据破坏 / 会计过账破坏 / 安全漏洞 / 核心循环断裂；#8 期间 CLOSED 守卫缺失属会计正确性类异常路径未实现，定 P1 非 P0 — 默认触发面依赖退货审核命中 CLOSED 期间，非默认活跃路径破坏；与 P0 示例"期间 CLOSED 后禁止过账但实际可过"不同 — 本切片退货审核本身不直接过账 GL，经 SalReturnPostingDispatcher → IErpFinVoucherBiz.post Facade，finance 引擎期间控制可能间接拦截，SP-3 运行时确认）。

---

## 8. 过程纪律自检

- [x] **checker 退出码门控核查**：本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`，actual ≤ baseline（详见下表）。**区分门控退出码 vs 纯 reporter 退出码**——checker 脚本是纯 reporter（退出码恒 0），真正门控在 CI workflow（`.github/workflows/compliance.yml`）解析 actual > baseline => sys.exit(1)。本报告**不**以 checker 脚本退出码 0 作为门控通过依据。**本报告无生产代码变更**（纯审计报告 + arm-index 文档更新），checker 无回归风险。

  | 规则 | Baseline | Actual | 状态 |
  |------|----------|--------|------|
  | R1a/R1b/R1c | 0/0/0 | 0/0/0 | ✅ |
  | R1d | 14 | 14 | ✅ |
  | R2a/R2b/R2c/R2d | 34/229/1382/34 | 34/229/1382/34 | ✅ |
  | R3 | 5 | 5 | ✅ |
  | R4/R5 | 0/0 | 0/0 | ✅ |
  | R6 | 2 | 2 | ✅ |
  | R7 | 0 | 0 | ✅ |
  | R8 | 0 | 0 | ✅ |
  | R10 | 6 | 6 | ✅ |
  | R11 | 0 | 0 | ✅ |
  | R12a/R12b/R12c | 69/66/40 | 69/66/40 | ✅ |

  全 16 规则 actual ≤ baseline（精确匹配，0 漂移）。
- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告全部 finding 已按 §7 规则 grep arm-index 同域同控制点后给出"复用 or 新增"裁决（详见 §6.2 表格），无未经比对直接新建的 finding。

---

## 落盘完整性自检（§6 9 段完整性）

报告产出 agent 在落盘前自查 9 段全部存在：

- [x] §1 需求契约原文（5 UC 验收标准逐字引用）
- [x] §2 实现代码路径（含行号 + 跨域调用链）
- [x] §3 测试断言证据（注明强度）
- [x] §4 运行时行为证据（复用 MA2 + E2E）
- [x] §5 符合性结论（5 UC × 5 列矩阵 + 候选缺口 10 项分级 + 每 UC 总结论）
- [x] §6 与 arm-index 衔接（§4 三判据复核 + 复用/新增裁决 + 双向可追溯 + #10 裁决说明）
- [x] §7 静态存疑点清单（5 项 SP-1..SP-5 + P0 即时通道未触发声明）
- [x] §8 过程纪律自检（checker actual vs baseline 实测表 + closure-audit 独立性 + 交叉去重）
- [x] §9 与 MA2 报告差异增量声明（前置段 — 复用 A2.9/O2C/A4.5/A2.5c + 只补需求视角差异）

9 段齐全，落盘。

---

## 附录：A1.20 切片裁决摘要

- **新 P1（6 项）**：P1-RC-023（UC-SAL-05 undeliveredQty）/ P1-RC-024（UC-SAL-05 暂估应收条件冲减）/ P1-RC-025（UC-SAL-06 换货完全缺失，须人工确认 product-scope）/ P1-RC-026（UC-SAL-07 退货成本策略 1/3 + 配置键）/ P1-RC-027（UC-SAL-09 已核销发票 pre-approve 守卫）/ P1-RC-028（UC-SAL-09 期间 CLOSED 守卫，会计正确性类 Q4 无例外）
- **新 P2（1 项）**：P2-RC-022（UC-SAL-04 无独立退款单）
- **复用（2 项）**：P2-MA2-011（#1 红字发票 credit-memo，§4 (i) 满足维持 watch-only + 严格解释 gap 注记）/ P2-MA2-058（#9 returnStatus/refundStatus 派生视图，§4 (i) 满足维持 watch-only + 严格解释 gap 注记）
- **不新建（1 项）**：#10 测试缺口 derivative 于 #3-#8 P1，MR1 实现时测试建立义务自动激活
- **P0 即时通道**：未触发（本切片无 P0）
- **静态存疑点**：5 项（SP-1..SP-5）交 MA4 A4.1/A4.2 运行时展开
- **Q4 关键证据**：P1-RC-025（换货）须人工确认 product-scope 范围裁剪；P1-RC-028（期间 CLOSED）属会计正确性类无例外
