# RC MA1 A1.18 — sales-F1 主流程与价格 需求-实现符合性审计

> Audit Status: closed
> 里程碑：MA1（需求-实现符合性层 / 五级追踪矩阵维度）
> 工作项：A1.18（MA1 需求追踪矩阵审计 — sales-F1 主流程与价格）
> 审计 plan：`docs/plans/2026-08-03-0407-1-rc-ma1-a1-18-sales-f1-mainflow-pricing.md`
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§1 五级矩阵 / §2 分级判据 / §3 完整枚举 / §4 Q1 真相源层级 / §5 Q4 修复义务 + 保护区域暂停协议 / §6 报告 9 段骨架 / §7 arm-index 衔接 / §8 过程纪律自检 / §9 真相源冻结 / §10 MR0/MR1 机制 / §去重协议）
> L1 真相源：`docs/design/sales/use-cases.md`（UC-SAL-01/11，2 UC）
> L1 锚点清单：`docs/audits/rc-requirement-baseline-inventory.md` §sales + §切片索引 A1.18
> 审计性质：**只读审计**（读代码/测试/报告，不改代码/ORM/api.xml/真相源；方法论 §5 保护区域，roadmap 预授权类目）
> 审计日期：2026-08-03
> 审计者：主代理（独立结束审计由独立子代理执行，见 plan §Closure Gates）

## 0. 审计结论（TL;DR）

| 项 | 数量 | 处置 |
|---|---|---|
| **P0**（活跃数据破坏 / 会计过账正确性破坏） | **0** | 无 MR0 即时通道触发 |
| **P1**（新登记） | **3** | P1-RC-020（UC-SAL-01 订单级可用量校验缺失）/ P1-RC-021（UC-SAL-11 最低价校验缺失）/ P1-RC-022（UC-SAL-11 价税分离缺失）→ 待 MR1（R1.0 展开为 RC-R1.n） |
| **P2**（新登记） | **3** | P2-RC-016（UC-SAL-01 SALES_DELIVERY 命名漂移，行为等价）/ P2-RC-017（UC-SAL-01 JUnit 凭证仅合计+计数，断言强度）/ P2-RC-018（UC-SAL-11 价格端到端仅 status==0 冒烟，断言强度）→ successor watch-only |
| **接受**（符合需求契约） | 部分验收标准 | UC-SAL-01 ②③④⑤回链/扣减/AR/收款核销/双层应收 + UC-SAL-11 ①②价格清单/促销规则主体（取价优先级链位置属设计选择 G5；客户应收双层更新 G6 已文档化） |
| MA2 既有行为证据复用 | 11+ 项 finding | 无升级（详见 §4 / §9） |

**整体裁决**：A1.18 切片 2 UC 五级追踪矩阵填齐。O2C 主链路（订单审核→出库审核→发票审核→收款审核→核销）+ 价格主体（价格清单 SPI 解析 + 促销规则纯函数引擎）经 L3-L5 四级证据确认符合 UC-SAL-01/11 **多数验收标准**。**三项 P1 需求分歧**：①UC-SAL-01 断言①「订单审核触发可用量校验 + 出库移动单生成」字面要求订单审核触发，实际**挪至出库审核**（`ErpSalDeliveryProcessor.triggerOutgoingMove:241-245`），订单审核仅校验客户激活 + 信用额度（`ErpSalOrderProcessor.validateBusinessRulesForApprove:166-170`）——属行为实质偏离 L1 字面；②UC-SAL-11 断言「最低价校验（见 UC-MD-04）」sales 层 `ErpSalOrderBizModel.applyPricingRules:96-114` 不调最低价守卫（UC-MD-04 在 master-data `ErpMdMaterialSkuBizModel:189-202` 独立实现，sales 应用促销层完全缺失）；③UC-SAL-11 断言「价税分离（促销后）税额 = 折扣后金额 / (1+税率) × 税率」sales `ErpSalOrderBizModel.recomputeLineAmount:172-179` 仅 `amount = gross − discountAmount`，**不按公式重算 taxAmount**。三项均按 §2 判据定为 P1（行为实质偏离 / 功能完全缺失），按 §10 经 MR1 批量修复通道修复；**无 P0**——可用量校验挪至出库环节仍落实（不构成 §2 P0③ 核心循环断裂）/ 价格最低价+价税分离缺失非活跃数据破坏（订单 CRUD + 主路径过账正确）/ GL 平衡不受影响（与 A1.1 业财过账引擎 pass 范围一致）。本审计**不实施修复**（§5 保护区域 + plan Non-Goals）。

---

## 1. 需求契约原文（L1，逐字引用）

> 来源：`docs/design/sales/use-cases.md`（L1 权威真相源，方法论 §4）。以下逐 UC 逐字引用验收标准原文，**禁止转述**（§1 L1 格式 + Q1 裁决根因守卫）。

### UC-SAL-01 标准销售全流程（主路径）（`use-cases.md:20`）

```
场景:从订单到收款的完整正向销售。

前置:主数据就绪;客户授信充足(如有信用控制);库存可用量满足。

行为链路:
1. 创建销售订单,审核通过(触发可用量校验 + 出库移动单生成,见 state-machine.md §2)
2. 创建销售出库单(关联订单),审核通过(扣减库存)
3. 创建销售发票(关联出库),审核通过(生成应收凭证)
4. 创建收款单,审核通过,核销发票

可验证断言:
// 回链
出库单.来源单号 == 订单.单号
发票行.来源单号 == 出库单.单号    // 见 returns/三单匹配约定

// 库存(出库单审核时)
库存余额[物料, 仓库].可用量 -= 出库明细数量之和
库存余额.现有量 -= 出库明细数量之和

// 过账
存在凭证: 业务类型 == SALES_DELIVERY(存货估值红冲) 且 来源 == 出库单
存在凭证: 业务类型 == AR_INVOICE 且 来源 == 发票
出库单.已过账 == true 且 发票.已过账 == true

// 核销(收款时)
发票.收款状态: 未收 → 部分 / 已收清
客户.应收余额 == 发票金额 - 已核销金额
```

**验收标准编号化（本切片逐条核验）**：
- ① 订单审核通过 → 触发可用量校验 + 出库移动单生成（行为链路 step 1）
- ② 回链：出库单.来源单号 == 订单.单号 + 发票行.来源单号 == 出库单.单号
- ③ 库存余额[物料,仓库].可用量 -= 出库明细数量之和（出库单审核时）
- ④ 库存余额.现有量 -= 出库明细数量之和（出库单审核时）
- ⑤ 存在凭证：业务类型 == SALES_DELIVERY（存货估值红冲）且 来源 == 出库单
- ⑥ 存在凭证：业务类型 == AR_INVOICE 且 来源 == 发票
- ⑦ 出库单.已过账 == true 且 发票.已过账 == true
- ⑧ 发票.收款状态：未收 → 部分 / 已收清
- ⑨ 客户.应收余额 == 发票金额 - 已核销金额

### UC-SAL-11 销售价格管理（`use-cases.md:231`）

```
场景:维护销售价格清单与促销规则,订单取价时按优先级应用。

> 实现说明：ErpSalPriceList（头/行）+ ErpSalPricingRule + ErpSalCustomerPriceResolver（IErpMdCustomerPriceResolver SPI 实现）
> + ErpSalPricingRuleEngine（纯函数式引擎）+ ErpSalOrderBizModel.applyPricingRules（订单促销应用）
> + 订单行/报价行行级折扣字段（discountRate/discountAmount/pricingSource）+ ErpMdPartner.customerGroup 客户组维度。

可验证断言:
// 价格清单(按客户分级/物料/数量阶梯)
价格清单行(客户组, 物料, 起订量, 单价, 生效区间)
订单取价优先级: 手工价 > 价格清单(匹配客户组/物料/阶梯) > SKU 默认档
  (与 UC-MD-03 价格优先级一致, 见 ../master-data/use-cases.md)

// 促销规则(买赠/满减/折扣)
促销规则(物料/客户/时段, 规则类型, 优惠值)
订单命中促销 → 应用优惠(改单价/加赠品行/减总额)
促销与价格清单可叠加(按配置决定)

// 最低价校验(见 UC-MD-04)
最终售价 < SKU.minPrice → 按配置拒绝/警告

// 价税分离(促销后)
折扣后金额 = 原金额 - 促销优惠
税额 = 折扣后金额 / (1 + 税率) × 税率
```

**验收标准编号化（本切片逐条核验）**：
- ① 价格清单行（客户组 / 物料 / 起订量 / 单价 / 生效区间）— ORM `ErpSalPriceList`（头）+ `ErpSalPriceListLine`（行）
- ② 订单取价优先级：手工价 > 价格清单（匹配客户组/物料/阶梯）> SKU 默认档（与 UC-MD-03 一致）
- ③ 促销规则（物料/客户/时段，规则类型，优惠值）
- ④ 订单命中促销 → 应用优惠（改单价 / 加赠品行 / 减总额）
- ⑤ 促销与价格清单可叠加（按配置决定）
- ⑥ 最低价校验（见 UC-MD-04）：最终售价 < SKU.minPrice → 按配置拒绝/警告
- ⑦ 价税分离（促销后）：折扣后金额 = 原金额 − 促销优惠；税额 = 折扣后金额 / (1 + 税率) × 税率

---

## 2. 实现证据（L3，`file:line`，跨域调用链列全）

> 审计对象实仓逐项核实（`module-sales/erp-sal-service/.../`）。L3 引用格式遵循 §1 L3 规范（含行号）。

### 2.1 订单审核（UC-SAL-01 行为链路 step 1）

| 组件 | 文件:行 | 审计状态 |
|---|---|---|
| 订单审核编排 | `ErpSalOrderApproveProcessor.java:25-49`（custom public override approve → requireEntity → isApproved 早返 → SoDGuard.assertApproverNotCreator → validateNotCancelled → validateTransitionForApprove → validateBusinessRules → beforeStateChange[auditPricingSourceDistribution] → afterStateChange[runCommitmentCommitHook + runIntercompanyApproveHook]） | ✅ |
| 业务规则校验 | `ErpSalOrderProcessor.java:166-170` `validateBusinessRulesForApprove` → `requireCustomerActive` + `creditLimitChecker.check(customerId, totalAmountWithTax, exchangeRate, code, context)` | ⚠️ **G1 缺口**：仅校验客户激活 + 信用额度，**不调用任何库存 Facade**（`@Inject` 簇 `ErpSalOrderProcessor.java:54-67` 无 `IErpInvStockMoveBiz`/`IErpInvStockBalanceBiz`） |
| 承付 commit 钩子 | `ErpSalOrderProcessor.runCommitmentCommitHook:311-325`（config-gated `erp-fin.budget-commitment-enabled` 默认 false） | ✅（config-gated 默认关） |
| Intercompany 钩子 | `ErpSalOrderProcessor.runIntercompanyApproveHook:277-290`（config-gated `erp-fin.intercompany-posting-enabled` 默认 false） | ✅（config-gated 默认关） |
| pricingSource 审计 | `ErpSalOrderProcessor.auditPricingSourceDistribution:248-269`（仅记录日志用于审计追踪，**不重取价，不驱动取价优先级链**） | ✅（G5 旁注） |

### 2.2 出库审核（UC-SAL-01 行为链路 step 2 — 实际可用量校验落实点）

| 组件 | 文件:行 | 审计状态 |
|---|---|---|
| 出库审核编排 | `ErpSalDeliveryApproveProcessor.java:26-46`（custom public override approve → requireEntity → isApproved 早返 → SoDGuard.assertApproverNotCreator → validateNotCancelled → validateTransitionForApprove → validateBusinessRulesForApprove → **enforceInspectionGate** → triggerOutgoingMove → applyPostingResult → setApproveStatus APPROVED → setApprovedBy/At → dao.updateEntity → postProcessApprove[rollupOrderDeliveryStatus]） | ✅ |
| 出库业务规则 | `ErpSalDeliveryProcessor.validateBusinessRulesForApprove:173-176`（requireCustomerActive + enforceCreditHold config-gated 默认 false） | ✅ |
| **跨域可用量校验 + 出库移动单生成** | `ErpSalDeliveryProcessor.triggerOutgoingMove:241-245`（loadLines → `stockMoveBuilder.build(delivery, lines, context)` → `stockMoveBiz.generateMove(request, context)`） → 库存域 `ErpInvStockMoveProcessor.doConfirm:86-98` → `validateAvailable:116-136`（不足抛 `ERR_AVAILABLE_INSUFFICIENT`，@BizMutation 回滚，出库单保持 SUBMITTED） | ✅ **可用量校验在此环节落实**（非订单审核环节——G1 根因） |
| 出库请求构造 | `DeliveryStockMoveBuilder.java:28-41`（moveType=OUTGOING + relatedBillType=SAL_DELIVERY + relatedBillCode=delivery.code → **回链到出库单编码**，断言②来源回链）+ `:54-67` buildLines 逐行映射（materialId/skuId/uoMId/quantity/batchNo） | ✅ |
| 库存扣减 + 过账 | 库存域 `IErpInvStockMoveBiz.generateMove`（businessLinked=true）内部 doConfirm + doComplete → 扣减 totalQuantity/reservedQuantity + InvPostingDispatcher `SALES_OUTPUT`（Dr 6401 主营业务成本 / Cr 1401 库存商品） | ⚠️ **G2 命名偏离**：业务类型 `SALES_OUTPUT(20)` 非 L1 字面 `SALES_DELIVERY`（`ErpFinBusinessType.java:14-69` 无 `SALES_DELIVERY` 常量；功能等价——存货估值红冲语义经库存域实现） |
| 过账结果回写 | `ErpSalDeliveryProcessor.applyPostingResult:247-253`（`delivery.setPosted(move.getPosted())` + postedAt/postedBy） | ✅ |
| 信用冻结（config-gated） | `ErpSalDeliveryProcessor.enforceCreditHold:184-191`（config-gated `erp-sal.credit-check-on-delivery` 默认 false） | ✅（config-gated 默认关） |
| 质检门控 | `ErpSalDeliveryProcessor.enforceInspectionGate:312-329`（强制质检，BLOCKED 抛 ERR_DELIVERY_INSPECTION_BLOCKED） | ✅ |
| 冲销闭环 | `ErpSalDeliveryProcessor.ensureReversed:255-268`（reverseApprove 时 stockMoveBiz.findByRelatedBill + reverse，红冲硬前置） | ✅ |

### 2.3 发票审核（UC-SAL-01 行为链路 step 3）

| 组件 | 文件:行 | 审计状态 |
|---|---|---|
| 发票审核编排 | `ErpSalInvoiceApproveProcessor.java:25-49`（custom public override approve → requireEntity → SoDGuard → validateNotCancelled → validateTransitionForApprove → validateBusinessRulesForApprove → **doPosting** → 重载 invoice → setApproveStatus APPROVED + posted 三件套[if posted] → runCommitmentReleaseOnInvoiceApproveHook） | ✅ |
| 发票过账派发 | `SalInvoicePostingDispatcher.tryPost:39-52`（buildEvent → executor.postEvent → return voucherId != null；catch 吞异常记 LOG + return false 保持 APPROVED+posted=false，DeferredPostingSweepJob 兜底） | ✅ |
| 业务类型 | `SalInvoicePostingDispatcher.buildEvent:71-90`：`event.setBusinessType(AR_INVOICE)` + billHeadCode=invoice.code（**回链**） + orgId/acctSchemaId/currencyId/exchangeRate(fallback ONE)/voucherDate + billData=TOTAL_AMOUNT/TOTAL_TAX_AMOUNT/TOTAL_AMOUNT_WITH_TAX/CUSTOMER_ID | ✅ 断言⑥ AR_INVOICE 业务类型 + 来源==发票 |
| AR_INVOICE 凭证 facts | `SalAcctDocProvider.createFacts:73-93`：AR_INVOICE 分支 → Dr 1131 应收账款（withTax）+ Cr 6001 主营业务收入（amount）+ Cr 2221 销项税（tax）+ R1.9 双金额字段（amountSource/amountFunctional=source×rate） | ✅ 断言⑥ 借贷平衡 + 三行结构 |
| 红字冲销硬前置 | `SalInvoicePostingDispatcher.reverse:58-69`（`executor.reverse(invoice.code, AR_INVOICE)`，失败抛出阻断状态迁移） | ✅ |

### 2.4 收款 + 核销（UC-SAL-01 行为链路 step 4）

| 组件 | 文件:行 | 审计状态 |
|---|---|---|
| 收款审核编排 | `ErpSalReceiptApproveProcessor.java`（per AbstractApproveProcessor skeleton + SalReceiptPostingDispatcher） | ✅ |
| 收款过账派发 | `SalReceiptPostingDispatcher`（businessType=RECEIPT，Dr 1002 银行存款 / Cr 1131 应收账款） | ✅ |
| 域级核销编排 | `ErpSalReceiptSettleProcessor.settle:24-27`（requireReceipt → 委派 ReceiptSettler） | ✅ |
| **核销约束 + 写入** | `ReceiptSettler.settle:55-111`：① receipt.approveStatus==APPROVED 守卫（:56-61） + ② amount ≤ invoiceBalance 守卫（:82-88，ERR_SETTLE_OVER_INVOICE_BALANCE） + ③ amount ≤ receiptRemaining 守卫（:89-94，ERR_SETTLE_OVER_RECEIPT_BALANCE） + ④ 同客户校验（requireInvoiceForSettle:141-159） + ⑤ 写 ErpSalReceiptLine（:96-100） | ✅ 断言⑧ 接收 PARTIAL/RECEIVED |
| 发票 receivedAmount/receivedStatus 派生 | `ReceiptSettler.recomputeInvoiceReceived:161-177`（按 Σ ReceiptLine.amount vs totalAmountWithTax 派生 UNRECEIVED/PARTIAL/RECEIVED；ORM invoice.receivedAmount/receivedStatus 字段写入） | ✅ 断言⑧ |
| 收款 writtenOffStatus 派生 | `ReceiptSettler.recomputeReceiptWrittenOff:179-194`（同型派生 receipt.writtenOffStatus） | ✅ |
| **客户应收余额更新（双层）** | sales 域 `ReceiptSettler` **不直接更新** `ErpMdPartner.receivableBalance`；客户应收余额由 finance `PartnerBalanceUpdater.setReceivableBalance:42`（`partner.setReceivableBalance(sumOpen(partnerId, DIRECTION_RECEIVABLE))`）经 `ErpFinArApItem`（RECEIVABLE + AR_INVOICE/RECEIPT/SALES_RETURN）层聚合更新 | ⚠️ **G6 双层设计**：sales ReceiptSettler 仅更新 invoice.receivedAmount；客户应收余额由 finance 独立层维护（设计并行，MA2 A2.5c §2.3 + `TestErpSalOrderToCashEnd:333-336` 文档化双层设计） |
| 反向核销 | `ReceiptSettler.reverseSettlement:116-137`（生成负金额 ReceiptLine 保留审计轨迹，余额与状态自然回退） | ✅ |

### 2.5 价格（UC-SAL-11）

| 组件 | 文件:行 | 审计状态 |
|---|---|---|
| 价格清单 ORM | `app-erp-sales.orm.xml` `ErpSalPriceList`（头）+ `ErpSalPriceListLine`（行）+ `ErpSalPricingRule`（促销规则）齐全 | ✅ 断言① |
| 价格清单 SPI 解析器 | `ErpSalCustomerPriceResolver.resolveCustomerPrice:51-76`（实现 `IErpMdCustomerPriceResolver` SPI，由 master-data 注入）：按 partnerId 精确匹配 + customerGroup 匹配（:99-105）+ isActive + 期间（:178-185）+ 币种（:95-97）+ priority（小者优先，:64-65）+ 行匹配 skuId>materialId+数量阶梯 minQuantity/maxQuantity（:160-167）+ 行级期间（:169-176） | ✅ 断言①② 价格清单匹配（SPI 由 master-data 调用） |
| 取价优先级链（位置） | master-data `ErpMdMaterialSkuBizModel.java:140-141, 166-167` 调 `customerPriceResolver.resolveCustomerPrice` 实现"手工价 > 价格清单 > SKU 默认档"链；sales 层 `ErpSalOrderProcessor.auditPricingSourceDistribution:248-269` 仅审计日志（不驱动取价） | ⚠️ **G5 设计选择**：取价优先级链实现在 master-data（UC-MD-03 归属），不在 sales 层；sales 仅 audit 日志——属 L1 跨域协作设计选择非缺陷（断言②与 UC-MD-03 显式一致） |
| 促销规则纯函数引擎 | `ErpSalPricingRuleEngine.evaluate:56-87`：纯函数式（不修改输入实体的原始状态），按 priority 排序（:112-114）+ 期间过滤（:117-124）+ 客户匹配（partnerId 精确 / customerGroupCode，:130-138）+ 币种（:140-142）+ targetType LINE/ORDER 分组（:74-84）+ stackable 控制（:81-83 非叠加 break） | ✅ 断言③④⑤ |
| PERCENT_DISCOUNT | `ErpSalPricingRuleEngine.applyPercentDiscount:173-186`（discountRate=percent + discountAmount=gross×percent/100 + pricingSource=PROMOTION） | ✅ 断言④ |
| AMOUNT_OFF（满减） | `ErpSalPricingRuleEngine.applyOrderRule:222-234`（头级，threshold 守卫 minOrderAmount + orderDiscountAmount 累加） | ✅ 断言④ |
| GIFT（买赠） | `ErpSalPricingRuleEngine.addGiftLine:203-217`（纯函数式返回赠品行快照，unitPrice=ZERO + quantity=giftQuantity + pricingSource=PROMOTION） | ✅ 断言④ |
| PRICE_OVERRIDE | `ErpSalPricingRuleEngine.applyPriceOverride:188-201`（unitPrice 覆盖 + discountAmount=gross−newGross + pricingSource=PROMOTION） | ✅ 断言④ |
| 订单促销应用入口 | `ErpSalOrderBizModel.applyPricingRules:96-114`（@BizMutation，加载订单行 + 解析 customerGroup + findActiveRules + 调引擎 evaluate + persistPricingResult） | ✅ 断言④ |
| **行级金额重算** | `ErpSalOrderBizModel.recomputeLineAmount:172-179`：`gross = unitPrice × qty` + `discountAmt = line.discountAmount` + `net = gross − discountAmt` + `line.setAmount(net)`——**仅重算 amount，不重算 taxAmount** | ⚠️ **G4 缺口**：UC-SAL-11 断言⑦「税额 = 折扣后金额 / (1 + 税率) × 税率」未被 sales 层 `recomputeLineAmount` 落实，taxAmount 保持 promotion 前原值（应用促销后金额变了但税额不变） |
| 订单合计重算 | `ErpSalOrderBizModel.recomputeOrderTotals:181-197`（Σ lines.amount → totalAmount + Σ lines.taxAmount → totalTaxAmount + totalAmountWithTax = totalAmount + totalTaxAmount；orderDiscountAmount 头级减） | ✅ 但 taxAmount 沿用 G4 缺口的旧值 |
| **最低价校验** | sales `ErpSalOrderBizModel.applyPricingRules:96-114` **完全不调用**最低价守卫；UC-MD-04 在 master-data `ErpMdMaterialSkuBizModel.java:189-202`（`deriveMinPrice` + `finalVal.compareTo(minPrice) < 0` + PriceValidationResult + ERR_MIN_PRICE_VIOLATION）独立实现，由 master-data 取价路径触发，**sales 促销应用层完全缺失最低价守卫** | ⚠️ **G3 缺口**：UC-SAL-11 断言⑥「最终售价 < SKU.minPrice → 按配置拒绝/警告」sales 层未实现（master-data 取价层独立守卫不覆盖 sales 促销后售价变化） |
| 行级折扣字段 | ORM `ErpSalOrderLine` 字段 `discountRate`/`discountAmount`/`pricingSource` + `ErpSalConstants` 常量 `PRICING_SOURCE_MANUAL/PRICE_LIST/PROMOTION/SKU_DEFAULT`（`ErpSalConstants.java:92-96`） | ✅ |

---

## 3. 测试证据（L4，注明断言强度）

> 断言强度分档引用 MA5（`docs/audits/2026-07-29-1430-arm-ma5-e2e-effectiveness.md`）评级口径：强断言 = 断言验收标准数值/状态；弱断言 = 仅断言不抛异常或仅冒烟。

### 3.1 UC-SAL-01 测试证据

| 验收标准 | 测试引用 | 断言强度 | 覆盖判定 |
|---|---|---|---|
| ① 订单审核触发可用量校验 + 出库移动单 | `TestErpSalOrderApproval`（14 方法）— 状态机 + 信用额度（testCreditLimitSoftWarningAllows / HardBlock / MultiCurrencyFunctional / OutstandingIncludesApprovedUndeliveredOrders / OutstandingIncludesArOpenBalanceHardBlock / ArInclusionConfigGatedOffFallsBackToOrdersOnly / ArMultiCurrencyFunctionalInclusion / SpecialApprovalWithPermission）强断言 | **强（状态机+信用）** | ⚠️ **G1 缺口**：订单审核**无可用量校验测试**（订单审核就不调库存 Facade）；Javadoc `:34-35` 明示"仅状态推进，不触发库存/凭证"；MA2 `2026-07-28-0400-arm-ma2-sales-state-machine.md:21` 同证 |
| ① 实际可用量校验路径 | `TestErpSalOrderToDeliveryEnd`（库存 20→10 强断言）+ E2E `tests/e2e/orchestration/o2c-chain.spec.ts`（行级凭证断言） | **强** | ✅ 可用量校验在出库审核环节强覆盖（行为正确，与 L1 字面"订单审核触发"控制点偏离） |
| ② 回链（出库.来源==订单 / 发票行.来源==出库） | `TestErpSalOrderToDeliveryEnd`（出库 delivery.orderId + move.relatedBillCode=delivery.code）+ `TestErpSalOrderToCashEnd`（发票行回链） | **强** | ✅ |
| ③④ 库存扣减（可用量 / 现有量） | `TestErpSalOrderToDeliveryEnd`（库存 20→10 实测）+ E2E `o2c-chain.spec.ts` | **强** | ✅ |
| ⑤ SALES_DELIVERY 凭证 | E2E `o2c-chain.spec.ts`（行级凭证 SALES_OUTPUT `{6401 DEBIT 1200}/{1401 CREDIT 1200}`）+ `TestErpSalOrderToDeliveryEnd`（posted=true + SALES_OUTPUT 凭证存在，**仅合计非行级 Dr/Cr**） | **强（E2E 行级）/ 中-强（JUnit 仅合计）** | ⚠️ **G2 命名偏离**：业务类型为 SALES_OUTPUT 非 L1 字面 SALES_DELIVERY，但**功能等价**（存货估值红冲，Dr 6401/Cr 1401）经 E2E 行级证据证实 |
| ⑥ AR_INVOICE 凭证 | `TestErpSalInvoicePosting#testApproveGeneratesArInvoiceVoucherAndPosted:65-93`（posted=true + docStatus=POSTED + **countLines=3** + totalDebit 比对）+ E2E `o2c-chain.spec.ts`（AR_INVOICE `{1131 DEBIT 113}/{6001 CREDIT 100}/{2221 CREDIT 13}`） | **强（E2E 行级）/ 中（JUnit countLines+totalDebit）** | ⚠️ **G7 缺口**：JUnit `TestErpSalInvoicePosting` 仅断言行数+合计（`countLines(voucherId)==3` + `totalDebit.compareTo(113)`），**不断言行级 subjectCode/dcDirection/amount**；E2E 行级证据补充 |
| ⑦ posted=true | `TestErpSalOrderToCashEnd:131-163`（delivery.posted=true / invoice.posted=true / receipt.posted=true 强断言） | **强** | ✅ |
| ⑧ 收款状态 PARTIAL/RECEIVED | `TestErpSalOrderToCashEnd:167-173`（settle 60 → invoice receivedAmount=60 + receivedStatus=PARTIAL + receipt writtenOffStatus=PARTIAL 强断言）+ `:305-323`（finance 核销层 openAmount=0 + status=SETTLED 强断言） | **强** | ✅ |
| ⑨ 客户应收余额 == 发票金额 - 已核销金额 | `TestErpSalOrderToCashEnd:333-336` 文档化双层设计（域侧 receivedAmount + finance 辅助账 openAmount）；`TestErpSalOrderApproval#testOutstandingIncludesArOpenBalanceHardBlock:264-280` 经 AR 余额影响信用额度（间接证实 partner.receivableBalance 由 finance PartnerBalanceUpdater 维护） | **强（间接）/ 中文档化** | ⚠️ **G6 双层设计**：sales 域 ReceiptSettler 不直写 partner.receivableBalance，由 finance `PartnerBalanceUpdater.setReceivableBalance` 经辅助账聚合——已文档化双层设计，行为等价（断言⑨功能正确） |

### 3.2 UC-SAL-11 测试证据

| 验收标准 | 测试引用 | 断言强度 | 覆盖判定 |
|---|---|---|---|
| ① 价格清单行 | `TestErpSalPricingEndToEnd#testScenario1_PriceListPricing:44-59`（创建 priceList + line + 验证 status==0） | **仅冒烟**（仅 status==0） | ⚠️ **G8 缺口**：不断言行级 unitPrice 实际取自 priceList |
| ② 取价优先级链（手工>清单>SKU） | （UC-MD-03 在 master-data 实现，sales 层仅 audit 日志，sales 测试不覆盖） | — | G5 设计选择 — 优先级链测试归 master-data（UC-MD-03 切片 A1.41） |
| ③④ 促销规则（PERCENT_DISCOUNT） | `TestErpSalPricingRuleEngine#testPercentDiscountLine:36-52`（discountRate=10 + discountAmount=100 + pricingSource=PROMOTION 强断言） | **强** | ✅ |
| ③④ 促销规则（AMOUNT_OFF） | `TestErpSalPricingRuleEngine#testAmountOffOrderMeetsThreshold:56-70`（orderDiscountAmount=200 强断言）+ `testAmountOffOrderBelowThreshold:74-88`（threshold 不满足→ZERO） | **强** | ✅ |
| ③④ 促销规则（GIFT） | `TestErpSalPricingRuleEngine#testGiftLine:92-108`（2 lines + gift.unitPrice=ZERO + gift.quantity=ONE + pricingSource=PROMOTION 强断言） | **强** | ✅ |
| ③④ 促销规则（PRICE_OVERRIDE） | `TestErpSalPricingRuleEngine#testPriceOverrideLine:113-129`（unitPrice=85 + discountAmount=75 + pricingSource=PROMOTION 强断言） | **强** | ✅ |
| ⑤ stackable 叠加 | `TestErpSalPricingRuleEngine#testStackableRulesBothApplied:133-151`（2 appliedRules 强断言）+ `testNonStackableOnlyFirstApplied:155-174`（1 appliedRules[0].ruleType=PERCENT_DISCOUNT 强断言） | **强** | ✅ |
| ⑤ priority 优先级 | `TestErpSalPricingRuleEngine#testPriorityLowerWins:178-199`（priority=50 wins → price=90 强断言） | **强** | ✅ |
| ③ 期间过滤 | `TestErpSalPricingRuleEngine#testPeriodOutsideNoMatch:203-221` | **强** | ✅ |
| ③ 客户组匹配 | `TestErpSalPricingRuleEngine#testCustomerGroupMatch:221-235`（1 appliedRule 强断言） | **强** | ✅ |
| ④⑤⑥⑦ 价格端到端（多个场景） | `TestErpSalPricingEndToEnd`（7 场景：PriceList / PercentDiscount / AmountOff / Gift / Stackable / Priority / PriceOverride）— **仅 `assertEquals(0, result.getStatus())` 冒烟断言**，不断言行级 discountRate/discountAmount/pricingSource/赠品/合计重算 | **仅冒烟** | ⚠️ **G8 缺口**：端到端 7 场景全部仅 status==0 冒烟（强断言在 `TestErpSalPricingRuleEngine` 单测覆盖，端到端层零内容断言） |
| ⑥ 最低价校验 | **无测试**（sales 层 applyPricingRules 不调最低价守卫；UC-MD-04 测试归 master-data） | — | ❌ **G3 缺口**：sales 层最低价校验零实现零测试 |
| ⑦ 价税分离 | **无测试**（recomputeLineAmount 不重算 taxAmount） | — | ❌ **G4 缺口**：sales 层价税分离零实现零测试 |

**测试证据汇总**：UC-SAL-01 出库端到端 + 收款核销 + 状态机 + 信用强覆盖；UC-SAL-01 ⑤SALES_DELIVERY 命名偏离（G2，行为等价）+ ⑥ JUnit 凭证仅合计+计数（G7，E2E 行级补充）+ ⑨ 应收双层设计（G6，已文档化）。UC-SAL-11 促销引擎单测（10 方法）强覆盖；UC-SAL-11 端到端仅冒烟（G8）+ 最低价缺失（G3）+ 价税分离缺失（G4）。

---

## 4. 运行时行为证据（L5，复用 MA2/E2E + 本切片差异）

> 方法论 §去重协议：既有 MA2 报告已证实的状态机/链路行为直接引用，**不重新核实行为本身**；本切片只补"需求契约↔实际行为"差异。

### 4.1 复用 MA2 已证实行为（`2026-07-27-1949-arm-ma2-order-to-cash-e2e.md` O2C 主证据）

| MA2 已证实行为 | 引用 | 本切片复用判定 |
|---|---|---|
| O2C 链路矩阵（SO→Delivery→Invoice→Receipt→Settle→Reconciliation）组件齐备 | MA2 §1（链路覆盖矩阵 :16-29） | ✅ 复用（UC-SAL-01 行为链路 step 1-4 证实） |
| 跨域可用量校验（`ErpSalDeliveryProcessor.triggerOutgoingMove:241-245` → `IErpInvStockMoveBiz.generateMove` → inv 域 `validateAvailable`，不足抛 ERR_AVAILABLE_INSUFFICIENT + @BizMutation 回滚）+ 负库存 config-gated | MA2 §2.1（:39-46） | ✅ 复用（UC-SAL-01 断言③④ 证实——但**控制点偏离 L1 字面**：L1 要求订单审核触发，实际出库审核触发，见 §5 G1） |
| 信用额度检查（订单审核 SOFT_WARNING/HARD_BLOCK/SPECIAL_APPROVAL 三级策略 + AR 余额纳入）+ 出库/发票信用冻结 config-gated | MA2 §2.2（:53-65） | ✅ 复用（订单审核业务规则证实） |
| 应收 openAmount 生命周期（AR_INVOICE/RECEIPT 生成正项 + 核销回减 + receivedStatus 派生 + 超额收款禁止 + 反向核销负金额行） | MA2 §2.3（:70-77） | ✅ 复用（UC-SAL-01 断言⑧证实） |
| 退货反向链（SalReturnPostingDispatcher SALES_RETURN + ErpFinArApItemGenerator 负 openAmount credit memo + ReturnRefundOrchestrator） | MA2 §2.4（:83-93） | ✅ 复用（属 A1.20 退货族切片，本切片仅引用 O2C 正向链） |
| 多币种 O2C | MA2 §2.5（:95-112）= **FAIL P1-MA2-009**（多币种 O2C GL 凭证层 + 收款核销汇兑损益完全未实现） | ⚠️ **复用既有 finding**：P1-MA2-009 已登记 P1（resolved 状态由 audit-remediation 跟踪），本切片不重复登记 |
| 收入/成本时点（出库即成本结转 SALES_OUTPUT + 开票确认收入 AR_INVOICE + 配比经期末结账） | MA2 §2.6（:114-127） | ✅ 复用（UC-SAL-01 断言⑤⑥⑦ 证实——SALES_OUTPUT 非 SALES_DELIVERY 命名偏离归 G2） |

### 4.2 复用 MA2 状态机审查（`2026-07-28-0400-arm-ma2-sales-state-machine.md`）

| MA2 已证实行为 | 引用 | 本切片复用判定 |
|---|---|---|
| sales 7 实体三轴状态机主路径守卫齐全（PROC 6 实体 4 主动作 + cancel 经 BizModel.cancel→大 Processor） | MA2 A2.9 §主路径 | ✅ 复用（订单审核状态迁移证实） |
| 出库 approve 可用量校验销售独有约束已落实（经 inv 域 doConfirm→validateAvailable 强制） | MA2 A2.9 §关键裁决 | ✅ 复用（与 4.1 同——控制点偏离 L1 字面 G1） |
| SoD 创建人≠审核人程序级强制（Pattern A/B/C 三模式 + sodErrorCode） | MA2 A2.9 + R3.3 done | ✅ 复用（订单审核 SoDGuard.assertApproverNotCreator 证实） |
| 跨域写经 I*Biz Facade（production 代码无 daoFor 跨域写） | MA2 A2.9 | ✅ 复用（UC-SAL-01 跨域调用链合规） |
| 既有 finding：P1-MA2-009（多币种 O2C）/ P1-MA2-056（Contract reverseApprove）/ P1-MA2-057（INLINE withdraw 守卫）/ P2-MA2-010/011/012/013/014/015/056/057/058（doc drift + 边界场景） | MA2 A2.9 finding 集 | ⚠️ 复用注记（本切片不重开，按 §去重协议） |

### 4.3 本切片需求视角差异增量（MA2 未覆盖）

| 差异点 | MA2 视角 | RC 视角（需求契约） | 本切片裁决 |
|---|---|---|---|
| 订单级可用量校验 | MA2 §2.1 证实"出库审核时跨域可用量校验已落实"（控制点不在订单审核） | UC-SAL-01 行为链路 step 1 字面「订单审核通过(触发可用量校验 + 出库移动单生成)」——L1 字面要求订单审核触发 | **P1-RC-020**（行为实质偏离 L1 字面控制点，§5 详述） |
| SALES_DELIVERY 业务类型 | MA2 §2.6 证实"SALES_OUTPUT 借 6401/Cr 1401 实现存货估值红冲" | UC-SAL-01 断言⑤ 字面「业务类型 == SALES_DELIVERY」——`ErpFinBusinessType.java:14-69` 无 SALES_DELIVERY 常量 | **P2-RC-016**（命名漂移，行为等价经 E2E 行级证据，cosmetic） |
| 客户应收余额双层更新 | MA2 §2.3 证实"域级 ReceiptSettler + finance ErpFinReconciliation 双路径设计并行" | UC-SAL-01 断言⑨「客户.应收余额 == 发票金额 - 已核销金额」——sales ReceiptSettler 不直写 partner.receivableBalance | **接受**（双层设计已文档化 `TestErpSalOrderToCashEnd:333-336` + MA2 §2.3，功能正确） |
| AR 凭证 JUnit 仅合计+计数 | MA2 §1（链路覆盖矩阵）+ A5.6 E2E 评级（orchestration E2E strong） | UC-SAL-01 断言⑥ L1 要求"存在凭证: AR_INVOICE 且 来源 == 发票"——JUnit 仅 totalDebit+countLines，E2E 行级补充 | **P2-RC-017**（断言强度，E2E 行级补充，JUnit 增强 successor） |
| 价格最低价校验 | MA2 未审价格子系统（O2C 链路 + 状态机 + 代码质量维度无此对象） | UC-SAL-11 断言⑥「最低价校验（见 UC-MD-04）」sales applyPricingRules 不调用 | **P1-RC-021**（功能完全缺失，§5 详述） |
| 价税分离 | MA2 未审 | UC-SAL-11 断言⑦「税额 = 折扣后金额 / (1+税率) × 税率」sales recomputeLineAmount 不重算 taxAmount | **P1-RC-022**（功能完全缺失，§5 详述） |
| 取价优先级链位置 | MA2 未审 | UC-SAL-11 断言②「订单取价优先级: 手工价 > 价格清单 > SKU 默认档」实现在 master-data（UC-MD-03 归属） | **接受**（L1 明示"与 UC-MD-03 价格优先级一致"，跨域协作设计选择） |
| 价格端到端仅冒烟 | A5.6 评级 `TestErpSalPricingEndToEnd` 为仅冒烟 | UC-SAL-11 ④⑤⑥⑦ 端到端 7 场景全部 status==0 冒烟，无行级断言 | **P2-RC-018**（断言强度，单测层强覆盖，端到端 successor） |

### 4.4 E2E 行为证据（复用）

- `tests/e2e/orchestration/o2c-chain.spec.ts`：O2C 全链 E2E（MA5 `2026-07-29-1430-arm-ma5-e2e-effectiveness.md:51,138` 评 strong）——**行级凭证断言**：SALES_OUTPUT `{6401 DEBIT 1200}/{1401 CREDIT 1200}` + AR_INVOICE `{1131 DEBIT 113}/{6001 CREDIT 100}/{2221 CREDIT 13}` + AR 辅助项 direction/openAmount/status
- `tests/e2e/orchestration/o2c-reverse.spec.ts`：红冲行级负金额（MA5 strong）
- 本切片无新 E2E 探针需求（存疑点见 §7）

---

## 5. 符合性结论（五级追踪矩阵 + 每 UC 符合性结论，§2 判据）

### 5.1 五级追踪矩阵（2 UC，每 UC 一行，不合并）

| UC | L1 use-case 需求契约 | L2 owner doc 契约 | L3 代码路径 | L4 测试断言 | L5 运行时行为 | 符合性结论 |
|----|---------------------|------------------|------------|------------|--------------|-----------|
| **UC-SAL-01** 标准销售全流程 | `use-cases.md:20` ①订单审核触发可用量校验+出库移动单 ②回链三元组 ③④库存扣减 ⑤SALES_DELIVERY 凭证 ⑥AR_INVOICE 凭证 ⑦posted=true ⑧收款状态派生 ⑨客户应收余额 | `state-machine.md §2/§9 + §实现模式与守卫边界`（设计参考；§2 表格 :50-56 显式「销售订单 \| 仅状态推进，不直接触发库存/凭证」+「销售出库单 \| 校验可用量 → 生成出库移动单」——**L2 与 L1 字面冲突**：L1:27 行为链路 step 1 字面"订单审核触发"，L2 state-machine.md:56 "订单仅状态推进"；按 §4 Q1 真相源层级，**以 L1 为准，L2 推定已向实现妥协**） | 订单审核：`ErpSalOrderApproveProcessor:25-49` → `ErpSalOrderProcessor.validateBusinessRulesForApprove:166-170`（仅 requireCustomerActive + creditLimitChecker）+ afterStateChange `runCommitmentCommitHook:60` + `runIntercompanyApproveHook:61`（均 config-gated 默认关）——**订单审核不调任何库存 Facade**；出库审核：`ErpSalDeliveryApproveProcessor:26-46` → `ErpSalDeliveryProcessor.triggerOutgoingMove:241-245` → `IErpInvStockMoveBiz.generateMove`（跨域 Facade）+ `applyPostingResult:247-253`；发票审核：`ErpSalInvoiceApproveProcessor:25-49` → `SalInvoicePostingDispatcher.tryPost:39-52`（AR_INVOICE）→ `SalAcctDocProvider.createFacts:73-93`（Dr 1131/Cr 6001/Cr 2221）；收款+核销：`ErpSalReceiptSettleProcessor.settle:24-27` → `ReceiptSettler.settle:55-111` + `recomputeInvoiceReceived:161-177`；应收余额：finance `PartnerBalanceUpdater.setReceivableBalance:42` 经辅助账（**非 sales ReceiptSettler 直写**） | `TestErpSalOrderApproval`（14 方法，状态机+信用强，**订单审核无可用量校验测试**）+ `TestErpSalOrderToDeliveryEnd`（库存 20→10、posted=true、SALES_OUTPUT 凭证**仅合计**）+ `TestErpSalInvoicePosting`（posted=true + **countLines=3 + totalDebit=113**，非行级 Dr/Cr）+ `TestErpSalOrderToCashEnd`（状态/posted/receivedStatus 强；AR 凭证**仅 totalDebit=113**）+ E2E `o2c-chain.spec.ts`（**行级凭证断言** SALES_OUTPUT/AR_INVOICE 强） | MA2 O2C §1-§2.6（链路矩阵 + 可用量+扣减 + 信用 + AR openAmount + 退货反向 + 多币种 P1-MA2-009 + 收入成本时点）+ MA2 sales 状态机 §主路径+关键裁决 复用 | **接受 on ②③④⑥⑦⑧**（回链 + 库存扣减 + AR 凭证 + posted + 收款派生 L3-L5 全证据一致）+ **P1 on ①**（订单级可用量校验缺失，**P1-RC-020**）+ **P2 on ⑤**（SALES_DELIVERY 命名漂移，**P2-RC-016**）+ **P2 on ⑥ JUnit 断言**（**P2-RC-017**）+ **接受 on ⑨**（双层设计已文档化 G6，功能等价） |
| **UC-SAL-11** 销售价格管理 | `use-cases.md:231` ①价格清单行 ②取价优先级链 ③促销规则 ④命中应用 ⑤stackable ⑥最低价校验 ⑦价税分离 | `state-machine.md §9 场景D + §10`（设计参考；§9 :142-146 仅述「赠品可用量 + 折扣价税分离」，未定义取价优先级链 + 最低价校验位置——**L1 明示②与 UC-MD-03 一致，跨域协作属 L1 字面设计**） | 价格清单 SPI：`ErpSalCustomerPriceResolver.resolveCustomerPrice:51-76`（partnerId/customerGroup/期间/币种/阶梯/priority 匹配）；促销纯函数引擎：`ErpSalPricingRuleEngine.evaluate:56-87`（PERCENT_DISCOUNT/AMOUNT_OFF/GIFT/PRICE_OVERRIDE × LINE/ORDER + stackable/priority/期间/客户组）；订单促销应用：`ErpSalOrderBizModel.applyPricingRules:96-114` + `recomputeLineAmount:172-179`（**仅 amount=gross−discountAmount，不重算 taxAmount**）+ `recomputeOrderTotals:181-197`；取价优先级链：master-data `ErpMdMaterialSkuBizModel:140-141, 166-167` 调 SPI 实现；最低价校验：master-data `ErpMdMaterialSkuBizModel:189-202` 独立实现，**sales applyPricingRules 不调用** | `TestErpSalPricingRuleEngine`（10 方法，**强**：discountRate/amount/pricingSource/赠品/stackable/priority/期间/客户组）+ `TestErpSalPricingEndToEnd`（7 场景**仅 status==0 冒烟**，无行级折扣/赠品/合计重算断言） | MA2 未审价格子系统（O2C/状态机/代码质量维度均无此对象）；本切片 L5 = 单测强覆盖 + 端到端冒烟（无运行时探针需求） | **接受 on ①②③④⑤**（价格清单 + 促销引擎主体 L3-L4 强一致，②取价优先级链位置属 L1 跨域设计选择 G5）+ **P1 on ⑥**（最低价校验缺失，**P1-RC-021**）+ **P1 on ⑦**（价税分离缺失，**P1-RC-022**）+ **P2 on ④⑤⑥⑦端到端测试**（**P2-RC-018**） |

### 5.2 分级判据命中明细（§2）

#### P1-RC-020 — UC-SAL-01 ① 订单级可用量校验缺失（行为实质偏离 L1 字面控制点）

- **命中判据**：§2 **P1①**「需求契约要求的功能完全缺失或行为实质偏离验收标准」
- **三源对照**：
  - L1（`use-cases.md:27`）：行为链路 step 1 逐字「创建销售订单,审核通过(触发可用量校验 + 出库移动单生成,见 state-machine.md §2)」——L1 字面明确**订单审核**为控制点。
  - L2（`state-machine.md:56`）：表格行「销售订单 \| 仅状态推进，不直接触发库存/凭证」——**L2 与 L1 字面冲突**，按 §4 Q1 真相源层级**以 L1 为准**，L2 推定已向实现妥协。
  - L3（`ErpSalOrderProcessor.validateBusinessRulesForApprove:166-170`）：仅 `requireCustomerActive` + `creditLimitChecker.check`；`@Inject` 簇 `:54-67` 无 `IErpInvStockMoveBiz`/`IErpInvStockBalanceBiz`；`afterStateChange` 钩子 `runCommitmentCommitHook`/`runIntercompanyApproveHook` 均 config-gated 默认关。实际可用量校验 + 出库移动单生成在**出库审核**环节落实（`ErpSalDeliveryProcessor.triggerOutgoingMove:241-245`）。
- **运行时影响**：可用量校验功能**存在**（出库审核环节落实，O2C 主链路功能正确），但**控制点偏离 L1 字面**——订单审核通过后无库存预占/可用量预校验，库存不足要到出库审核才暴露（销售员接到订单后才发现无法发货）。**不破坏活跃数据**（出库审核仍守卫，不会超卖）+ **不破坏会计过账**（无 GL 影响）+ **O2C 核心循环完整**（订单→出库→发票→收款），故非 §2 P0③ 核心循环断裂。
- **严重性**：major（L1 字面控制点偏离 + 用户体验降级，但功能等价经出库环节守卫）
- **P0 升级评估**：经评估**维持 P1 不升 P0**。理由：(1) 可用量校验功能存在（出库审核环节），非"完全缺失"；(2) 不破坏活跃数据（出库仍守卫）；(3) 不破坏会计过账；(4) MA2 A2.9 + R3.3 状态机审查已对同控制点裁决 P1 范围内。
- **修复义务**：§5 Q4=(a) 强制实现。经 MR1（R1.0 展开为 RC-R1.n）。修复方案 = 订单审核 `ErpSalOrderProcessor.validateBusinessRulesForApprove` 增加可选库存可用量预校验（调 `IErpInvStockBalanceBiz` 查询可用量，不足按 config 拒绝/警告），或 owner doc 显式记录"订单级可用量校验挪至出库审核"的设计裁决（经 §4 三判据之一人工批准）。**纯 BizModel 代码逻辑修复 + 可能新增 config key，按 roadmap 预授权类目可自动执行，不触发 §5 ask-first**（不触及 ORM/会计过账核心路径）。
- **与既有 finding 关系**：grep arm-index 「订单审核 + 可用量」「order approve stock check」「available quantity sales order」零命中同域同控制点 finding。P1-MA2-009（多币种 O2C）/ P1-MA2-056/057（Contract + INLINE 守卫）/ P2-MA2-010~015/056/057/058（doc drift + 边界场景）均不同控制点。**新建 P1-RC-020**。

#### P1-RC-021 — UC-SAL-11 ⑥ 最低价校验缺失（sales 促销应用层完全缺失）

- **命中判据**：§2 **P1①**「需求契约要求的功能完全缺失或行为实质偏离验收标准」
- **三源对照**：
  - L1（`use-cases.md:249-250`）：逐字「// 最低价校验(见 UC-MD-04)\n最终售价 < SKU.minPrice → 按配置拒绝/警告」——L1 显式要求**促销后**最终售价低于 SKU.minPrice 时守卫。
  - L2（`state-machine.md §9 场景D`：:142-146）：仅述「赠品 + 折扣价税分离」，**未覆盖最低价校验位置**——L2 漏述。
  - L3（`ErpSalOrderBizModel.applyPricingRules:96-114`）：加载规则→解析客户组→调引擎→`persistPricingResult:143-160` 重算行金额——**完全不调用最低价守卫**；UC-MD-04 在 master-data `ErpMdMaterialSkuBizModel.java:189-202`（`deriveMinPrice` + `finalVal.compareTo(minPrice) < 0` + `PriceValidationResult` + `ERR_MIN_PRICE_VIOLATION`）独立实现，由 master-data 取价路径触发，**sales 促销应用层完全缺失最低价守卫**。**促销后售价**（如 PERCENT_DISCOUNT 50% off）可能跌破 SKU.minPrice，但 sales 层无任何守卫拦截。
- **运行时影响**：促销应用后售价可能低于 SKU 底线，sales 层无守卫 → 销售员配置错误促销规则（如折扣过深）时，订单以低于成本/底价的价格成交。**不破坏活跃数据**（CRUD + 主路径过账正确，订单仍可成交）+ **不破坏会计过账**（GL 平衡不变），但**价格管控功能缺失**。
- **严重性**：major（最低价校验功能在 sales 促销层完全缺失，但 UC-MD-04 取价层独立守卫覆盖部分场景）
- **修复义务**：§5 Q4=(a) 强制实现。经 MR1。修复方案 = `ErpSalOrderBizModel.applyPricingRules` 在 `persistPricingResult` 后追加调 `IErpMdMaterialSkuBiz.validatePrice` 或直接比对 SKU.minPrice + config-gated 拒绝/警告（与 UC-MD-04 范式对齐）。**纯 BizModel 代码逻辑修复 + 可能新增 config key，按 roadmap 预授权类目可自动执行，不触发 §5 ask-first**。
- **与既有 finding 关系**：grep arm-index 「minPrice」「最低价」「minimum price」「UC-MD-04」零命中 sales 域同控制点 finding。master-data 侧 `ErpMdMaterialSkuBizModel.java:189-202` 最低价守卫存在但归 UC-MD-04（A1.41 master-data 全功能切片），本 finding 覆盖 sales 促销应用层缺失。**新建 P1-RC-021**（与未来 A1.41 UC-MD-04 审计互补）。

#### P1-RC-022 — UC-SAL-11 ⑦ 价税分离缺失（recomputeLineAmount 不重算 taxAmount）

- **命中判据**：§2 **P1①**「需求契约要求的功能完全缺失或行为实质偏离验收标准」
- **三源对照**：
  - L1（`use-cases.md:253-255`）：逐字「// 价税分离(促销后)\n折扣后金额 = 原金额 - 促销优惠\n税额 = 折扣后金额 / (1 + 税率) × 税率」——L1 显式要求**促销后**重算税额。
  - L2（`state-machine.md §9 场景D`：:142-146）：仅 generic 概述「折扣影响应收金额（价税分离计算）」，未定义重算位置——L2 漏述。
  - L3（`ErpSalOrderBizModel.recomputeLineAmount:172-179`）：`gross = unitPrice × qty` + `discountAmt = line.getDiscountAmount()` + `net = gross − discountAmt` + `line.setAmount(net.setScale(4, HALF_UP))`——**仅 setAmount，不 setTaxAmount**；`recomputeOrderTotals:181-197` 沿用 `line.getTaxAmount()` 旧值（promotion 前税额）汇总到 `order.totalTaxAmount`。**促销应用后订单行 amount 减少了（折扣生效），但 taxAmount 保持原值**——L1 要求的"折扣后金额 / (1+税率) × 税率"重算未实现。
- **运行时影响**：促销应用后订单 taxAmount 高估（基于原金额而非折扣后金额），发票过账时 AR_INVOICE 销项税（Cr 2221）取自 `invoice.totalTaxAmount` → 销项税高估 → 应收（Dr 1131 = withTax = amount + taxAmount）同步高估。**潜在 GL 错误**：销项税 + 应收金额偏高（收入 Cr 6001 取 amount 正确）。**不破坏 GL 平衡**（Dr 1131 = Cr 6001 + Cr 2221 仍平衡，只是销项税偏高 + 应收偏高 + 收入正确），属 §2 P1①（功能完全缺失——价税分离公式未实现）非 P0④（GL 仍平衡，仅销项税/应收金额错误）。
- **严重性**：major（价税分离公式完全缺失，影响 AR/销项税金额准确性）
- **修复义务**：§5 Q4=(a) 强制实现。经 MR1。修复方案 = `ErpSalOrderBizModel.recomputeLineAmount:172-179` 在 setAmount 后追加 `BigDecimal taxRate = deriveTaxRate(line); BigDecimal newTax = net.divide(BigDecimal.ONE.add(taxRate), 4, HALF_UP).multiply(taxRate); line.setTaxAmount(newTax);`（按 L1 公式）+ `recomputeOrderTotals` 沿用重算后的 taxAmount 汇总。**纯 BizModel 代码逻辑修复，按 roadmap 预授权类目可自动执行，不触发 §5 ask-first**。
- **与既有 finding 关系**：grep arm-index 「价税分离」「tax separation」「recomputeLineAmount」「taxAmount recompute」零命中 sales 域同控制点 finding。**新建 P1-RC-022**。

### 5.3 P2 命中明细（接受类 + successor）

#### P2-RC-016 — UC-SAL-01 ⑤ SALES_DELIVERY 命名漂移（行为等价，cosmetic 文档类）

- **命中判据**：§2 **P2①**「次要验收标准未完全满足，主路径 OK 边界弱」（cosmetic 命名漂移，行为等价）
- **三源对照**：
  - L1（`use-cases.md:43`）：逐字「存在凭证: 业务类型 == SALES_DELIVERY(存货估值红冲) 且 来源 == 出库单」。
  - L3（`ErpFinBusinessType.java:14-69`）：枚举**不存在** `SALES_DELIVERY` 常量；实际经 `SALES_OUTPUT(20)`（库存域 `InvAcctDocProvider` 实现，Dr 6401 主营业务成本 / Cr 1401 库存商品）——**与 L1 存货估值红冲语义等价**。
  - L5（E2E `o2c-chain.spec.ts`）：行级凭证 SALES_OUTPUT `{6401 DEBIT 1200}/{1401 CREDIT 1200}` 强断言证实存货估值红冲行为正确。
- **裁决**：**行为完全正确**，仅 L1 字面 businessType 命名漂移（SALES_DELIVERY↔SALES_OUTPUT）。**与 P2-RC-011（A1.15 GOODS_RECEIPT↔PURCHASE_INPUT + PURCHASE_INVOICE↔AP_INVOICE 命名漂移）同型不同控制点**（finance businessType 枚举命名，sales 域投影）。successor watch-only。
- **修复**：触及 L1 use-cases + L2 posting.md 真相源，§9 冻结条款须经人工批准；或仅在 use-cases.md 补注「SALES_DELIVERY 在 ORM/字典权威名为 SALES_OUTPUT（库存域 InvAcctDocProvider 实现，存货估值红冲语义等价）」对齐表，L2 设计参考段落可与代码协同修订。**纯文档修复可自动执行，不触发 §5 ask-first**。

#### P2-RC-017 — UC-SAL-01 ⑥ JUnit AR 凭证仅合计+计数（断言强度，E2E 行级补充）

- **命中判据**：§2 **P2①**「次要验收标准未完全满足」（断言强度——L4 JUnit 仅 totalDebit+countLines，E2E 行级补充）
- **三源对照**：
  - L1（`use-cases.md:44`）：逐字「存在凭证: 业务类型 == AR_INVOICE 且 来源 == 发票」。
  - L3（`SalAcctDocProvider.createFacts:73-93`）：AR_INVOICE 分支 → Dr 1131 / Cr 6001 / Cr 2221 三行结构 + R1.9 双金额字段。
  - L4（`TestErpSalInvoicePosting#testApproveGeneratesArInvoiceVoucherAndPosted:65-93`）：断言 `posted=true` + `docStatus=POSTED` + `countLines(voucherId)==3` + `totalDebit.compareTo(113)`——**仅合计+计数，不断言行级 subjectCode/dcDirection/amount**。E2E `o2c-chain.spec.ts` 行级凭证 `{1131 DEBIT 113}/{6001 CREDIT 100}/{2221 CREDIT 13}` 强断言补充。
- **裁决**：**功能完全正确**（L3 实现完整 + E2E 行级证据强），仅 JUnit 单测断言强度不足（A5.6 评级为弱）。**与 P1-RC-010（A1.11 mfg UC-MFG-13 召回报告测试仅冒烟）同型不同控制点**（mfg 基因链召回报告 vs sales AR 凭证）。successor watch-only。
- **修复**：补强 `TestErpSalInvoicePosting` 行级断言（断言每行 subjectCode + dcDirection + amount 精确值）+ `TestErpSalOrderToCashEnd` 同型增强。**纯测试补充，按 roadmap 预授权类目可自动执行，不触发 §5 ask-first**。

#### P2-RC-018 — UC-SAL-11 ④⑤⑥⑦ 价格端到端仅 status==0 冒烟（断言强度，单测层强覆盖）

- **命中判据**：§2 **P2①**「次要验收标准未完全满足」（断言强度——端到端仅 status==0 冒烟）
- **三源对照**：
  - L1（`use-cases.md:237-255`）：④⑤⑥⑦ 验收标准要求行级折扣 + 赠品 + 合计重算 + 最低价 + 价税分离。
  - L3（`ErpSalPricingRuleEngine.evaluate:56-87` + `ErpSalOrderBizModel.applyPricingRules:96-114`）：促销引擎 + 应用层完整。
  - L4（`TestErpSalPricingEndToEnd` 7 场景：PriceListPricing / PercentDiscount / AmountOff / Gift / Stackable / Priority / PriceOverride）：**全部仅 `assertEquals(0, result.getStatus())` 冒烟**，不断言行级 discountRate/discountAmount/pricingSource/赠品/合计重算（G8）。单测层 `TestErpSalPricingRuleEngine`（10 方法强）覆盖算法正确性。
- **裁决**：**功能正确**（单测层强覆盖 + 端到端冒烟通过），仅端到端断言强度不足（A5.6 评级为仅冒烟）。**与 P1-RC-010（A1.11 mfg 召回报告）+ P2-RC-017（本切片 AR 凭证）同型不同控制点**（sales 价格端到端）。successor watch-only。
- **修复**：补强 `TestErpSalPricingEndToEnd` 7 场景的行级断言（每场景断言 modifiedLines 的 discountRate/discountAmount/pricingSource + 赠品行的 unitPrice=0/quantity/pricingSource + order totalAmount/totalTaxAmount 重算）。**纯测试补充，按 roadmap 预授权类目可自动执行，不触发 §5 ask-first**。

### 5.4 接受类 UC 验收标准汇总

| UC | 验收标准 | 接受依据 |
|---|---|---|
| UC-SAL-01 ② | 回链三元组（出库.来源==订单 / 发票行.来源==出库） | `DeliveryStockMoveBuilder:37-38` relatedBillType=SAL_DELIVERY + relatedBillCode=delivery.code（回链到出库单编码）+ 发票行回链经 invoiceLine.deliveryLineId；L4 `TestErpSalOrderToDeliveryEnd` + `TestErpSalOrderToCashEnd` 强断言 |
| UC-SAL-01 ③④ | 库存扣减（可用量/现有量 -= 出库明细数量之和） | `ErpSalDeliveryProcessor.triggerOutgoingMove:241-245` → `IErpInvStockMoveBiz.generateMove` → inv 域 doConfirm+doComplete 扣减；L4 `TestErpSalOrderToDeliveryEnd` 库存 20→10 实测 + E2E 行级凭证 |
| UC-SAL-01 ⑥ | AR_INVOICE 凭证（Dr 1131/Cr 6001/Cr 2221） | `SalAcctDocProvider.createFacts:73-93` 三行结构 + R1.9 双金额；E2E 行级凭证强断言（JUnit 仅合计归 P2-RC-017） |
| UC-SAL-01 ⑦ | posted=true（出库 + 发票） | `ErpSalDeliveryProcessor.applyPostingResult:247-253` + `ErpSalInvoiceApproveProcessor:40-44`；L4 `TestErpSalOrderToCashEnd:131-163` 强断言 |
| UC-SAL-01 ⑧ | 收款状态：未收 → 部分/已收清 | `ReceiptSettler.recomputeInvoiceReceived:161-177` 按 Σ ReceiptLine.amount vs totalAmountWithTax 派生；L4 `TestErpSalOrderToCashEnd:167-173, 305-323` 强断言（PARTIAL/RECEIVED/SETTLED） |
| UC-SAL-01 ⑨ | 客户应收余额 == 发票金额 - 已核销金额 | sales ReceiptSettler 不直写，finance `PartnerBalanceUpdater.setReceivableBalance:42` 经辅助账聚合（双层设计已文档化 `TestErpSalOrderToCashEnd:333-336` + MA2 §2.3）；行为正确（G6） |
| UC-SAL-11 ① | 价格清单行（客户组/物料/起订量/单价/生效区间） | ORM `ErpSalPriceList` + `ErpSalPriceListLine` + SPI `ErpSalCustomerPriceResolver.resolveCustomerPrice:51-76` |
| UC-SAL-11 ② | 取价优先级链（手工>清单>SKU） | L1 明示"与 UC-MD-03 一致"，实现在 master-data `ErpMdMaterialSkuBizModel:140-141, 166-167`（UC-MD-03 归属 A1.41），跨域协作属 L1 字面设计（G5） |
| UC-SAL-11 ③④⑤ | 促销规则（PERCENT_DISCOUNT/AMOUNT_OFF/GIFT/PRICE_OVERRIDE × LINE/ORDER + stackable + priority + 期间 + 客户组） | `ErpSalPricingRuleEngine.evaluate:56-87` + 4 apply 方法；L4 `TestErpSalPricingRuleEngine` 10 方法强覆盖（端到端仅冒烟归 P2-RC-018） |

---

## 6. 与 arm-index 衔接（§7 复用 or 新增 裁决）

> 产出 finding 前已 grep `arm-index.md` sales 同域同控制点。裁决遵循 §7 规则。

### 6.1 grep 比对结果

| 候选既有 finding | 控制点 | 与本切片 finding 关系 | 裁决 |
|---|---|---|---|
| `P1-MA2-009` 多币种 O2C（GL 凭证层 + 收款核销汇兑损益） | O2C 链路多币种 FX plug | 不同控制点（FX 折算 vs 订单级可用量校验 vs 价格最低价/价税分离），属 O2C 链路主题但子控制点不同 | **不相关**（本切片不复核多币种，P1-MA2-009 维持 audit-remediation resolved 状态） |
| `P1-MA2-056` Contract reverseApprove→SUBMITTED 违反 owner doc | Contract 状态机 | 不同实体（Contract vs Order/Delivery/Invoice/Receipt/价格），归 A1.19-A1.21 切片 | 不相关 |
| `P1-MA2-057` 6 实体 INLINE withdrawApproval 守卫缺失 | INLINE 路径守卫 | 不同控制点（审批路径守卫 vs 主流程/价格），归 A1.19 sales-F2 切片 | 不相关 |
| `P2-MA2-010` 发票>订单金额守卫缺失 | 发票审核金额守卫 | 不同控制点（发票审核金额 vs 订单审核可用量/价格） | 不相关 |
| `P2-MA2-011` 红字发票 doc drift（credit-memo-via-return） | 已开票退货红字发票实现路径 | 不同切片（退货族归 A1.20 sales-F3） | 不相关（本切片仅 O2C 正向链） |
| `P2-MA2-012` 信用控制 doc drift（信用冻结扩展点） | 出库/发票信用冻结 config-gated | 不同控制点（信用冻结 vs 订单级可用量/价格） | 不相关 |
| `P2-MA2-013` 收款核销订单维度缺失 | 收款核销维度（发票维度 vs 订单维度） | 不同控制点（核销维度 vs 订单级可用量/价格） | 不相关 |
| `P2-MA2-014` ReceiptSettler 并发核销无锁 | 域级核销并发缺口 | 不同控制点（并发 vs 订单级可用量/价格） | 不相关（归 A2.17 并发审计） |
| `P2-MA2-015` 期间配比 doc drift | 跨月出库-开票配比 | 不同控制点（期间配比 vs 订单级可用量/价格） | 不相关 |
| `P2-MA2-056/057/058` sales state-machine doc drift | state-machine.md 缺独立章节 | 不同维度（owner-doc drift vs 需求契约） | 不相关 |
| `P2-MA2-073` 承付测试缺 Dr/Cr | 承付释放路径测试断言强度 | 不同控制点（承付测试 vs 订单级可用量/价格） | 不相关 |
| `P1-MA1-022` 跨域 daoFor（sales 投影） | 跨域只读 daoFor | 不同控制点（daoFor vs 订单级可用量/价格） | 不相关 |

### 6.2 新建 finding 裁决

| Finding ID | UC | 根因/控制点 | 与既有 finding 差异依据 | 裁决 |
|---|---|---|---|---|
| **P1-RC-020** | UC-SAL-01 ① | 订单级可用量校验缺失（L1 字面要求订单审核触发，实际挪至出库审核） | arm-index 无 finding 覆盖"订单级可用量校验"主题；既有 P1 均为多币种/状态机/承付/信用等不同控制点 | **新建** |
| **P1-RC-021** | UC-SAL-11 ⑥ | 最低价校验在 sales 促销应用层完全缺失（UC-MD-04 在 master-data 独立实现） | arm-index grep 「minPrice」「最低价」零命中 sales 域同控制点；master-data 侧守卫归 UC-MD-04（A1.41）不同切片 | **新建**（与 A1.41 互补） |
| **P1-RC-022** | UC-SAL-11 ⑦ | 价税分离缺失（recomputeLineAmount 不重算 taxAmount） | arm-index grep 「价税分离」「recomputeLineAmount」「taxAmount recompute」零命中 sales 域同控制点 | **新建** |
| **P2-RC-016** | UC-SAL-01 ⑤ | SALES_DELIVERY 命名漂移（行为等价 SALES_OUTPUT，cosmetic） | 与 P2-RC-011（A1.15 GOODS_RECEIPT/PURCHASE_INVOICE 命名漂移）同型不同控制点（sales 域投影） | **新建**（与 P2-RC-011 同型模式） |
| **P2-RC-017** | UC-SAL-01 ⑥ | JUnit AR 凭证仅合计+计数（断言强度） | 与 P1-RC-010（A1.11 召回报告测试）同型不同控制点；A5.6 评级引用 | **新建** |
| **P2-RC-018** | UC-SAL-11 ④⑤⑥⑦ | 价格端到端仅 status==0 冒烟（断言强度） | 与 P1-RC-010/P2-RC-017 同型不同控制点（价格端到端） | **新建** |

### 6.3 双向可追溯

- **新 finding → arm-index**：6 finding（3 P1 + 3 P2）将写入 `arm-index.md` MA1 RC finding 分区（§7 归档纪律）。
- **finding → 修复**：3 P1 待 MR1 R1.0 展开为 RC-R1.n 修复行；3 P2 successor watch-only 不强制。
- **既有 finding 复用注记**：本切片不复用既有 finding 编号（grep 无同域同控制点），但 §4.1 复用 MA2 行为证据（P1-MA2-009 等维持 audit-remediation resolved 状态，本切片不重开）。

---

## 7. 静态存疑点清单（供 MA4 A4.1 展开）

> 本切片 L5 无法静态定论、需运行时确认的点。每存疑点一行；无则注明。

1. **UC-SAL-01 ① 订单级可用量校验缺失的运行时业务影响**：L3 静态确认订单审核不调库存 Facade，但「销售员实际接单后到出库环节才发现库存不足」的运行时频度/业务影响属运营层面，本切片仅静态裁决。交 MA4 A4.1 按需追加 A4.1.n 实体行展开运行时验证（grep 实际订单→出库拒绝率 + 销售员工作流调研）。
2. **UC-SAL-11 ⑥ 最低价校验缺失的实际触发面**：L3 静态确认 sales applyPricingRules 不调最低价守卫，但「实际促销配置是否导致最终售价 < SKU.minPrice」属运行时配置面普查——交 MA4 A4.1 按需展开（grep `ErpSalPricingRule` 实际 discountPercent 配置 + SKU minPrice 取值 + 比对）。
3. **UC-SAL-11 ⑦ 价税分离缺失的实际 GL 影响**：L3 静态确认 recomputeLineAmount 不重算 taxAmount，但「实际订单经 applyPricingRules 后 taxAmount 偏差幅度」属运行时数值——交 MA4 A4.1 按需展开（构造促销场景 + 实测 invoice.totalTaxAmount 偏差 + AR_INVOICE 销项税偏差）。
4. **UC-SAL-01 ⑨ 客户应收余额双层设计的运行时一致性**：sales ReceiptSettler 域侧 receivedAmount + finance 辅助账 openAmount 双路径（与 P2-MA2-038 同主题不同维度——本切片仅核 L1 字面"客户应收余额 == 发票金额 - 已核销金额"行为正确），运行时一致性归 P2-MA2-038（DualSideConsistencyChecker）跟踪，本切片不重复。
5. **UC-SAL-11 ② 取价优先级链跨域协作运行时一致性**：L3 静态确认取价优先级链在 master-data 实现（UC-MD-03 归属 A1.41），sales 层仅 audit 日志，但「master-data 取价后 sales 订单行 pricingSource 字段实际写入值与 audit 日志一致性」属跨域运行时——交 MA4 A4.1 按需展开（与 A1.41 master-data 切片协同）。

**P0 即时通道**：本切片 Phase 1 定级**未出 P0**（P1-RC-020/021/022 均为 P1，非 §2 P0①②③④ 活跃数据破坏/核心循环断裂/会计过账破坏），按 §10 **不触发 MR0**。三 P1 finding 经 MR1 批量修复通道（R1.0 展开为 RC-R1.n）。

---

## 8. 过程纪律自检（§8 模板）

- [x] **checker 退出码门控核查**：本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`，actual ≤ baseline（详见下表）。**区分门控退出码 vs 纯 reporter 退出码**——checker 脚本是纯 reporter（退出码恒 0），真正门控在 CI workflow（`.github/workflows/compliance.yml`）解析 actual > baseline => sys.exit(1)。本报告**不**以 checker 脚本退出码 0 作为门控通过依据。
- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告全部 finding 已按 §7 规则 grep arm-index 同域同控制点后给出"复用 or 新增"裁决（§6），无未经比对直接新建的 finding。

### checker actual vs baseline 实测表（2026-08-03 实测）

> 本审计为**只读审计**（无生产代码变更），故 checker 无回归风险；actual vs baseline 实测记录如下（基线源 `compliance-baseline.md §BASELINE (machine-readable)`）。

| 规则 | Baseline | Actual | 状态 |
|------|----------|--------|------|
| R1a/R1b/R1c | 0/0/0 | 0/0/0 | ✅ |
| R1d | 14 | 14 | ✅ |
| R2a | 34 | 34 | ✅ |
| R2b | 229 | 229 | ✅ |
| R2c | 1382 | 1382 | ✅ |
| R2d | 34 | 34 | ✅ |
| R3 | 5 | 5 | ✅ |
| R4/R5 | 0/0 | 0/0 | ✅ |
| R6 | 2 | 2 | ✅ |
| R7 | 0 | 0 | ✅ |
| R8 | 0 | 0 | ✅ |
| R10 | 6 | 6 | ✅ |
| R11 | 0 | 0 | ✅ |
| R12a/R12b/R12c | 69/66/40 | 69/66/40 | ✅ |

全 19 规则 actual ≤ baseline，**0 漂移**。本审计无生产代码变更，无回归风险。

---

## 9. 与 MA2 报告差异增量声明（§去重协议）

本切片声明与既有 MA2 报告的差异增量：

- **复用 MA2 已证实行为**（不重新核实）：
  - `2026-07-27-1949-arm-ma2-order-to-cash-e2e.md`（O2C 全链主证据）：§1 链路矩阵 + §2.1 可用量+扣减 PASS + §2.2 信用额度 PASS + §2.3 AR openAmount 生命周期 PASS + §2.4 退货红冲 PASS（归 A1.20）+ §2.5 多币种 **FAIL P1-MA2-009** + §2.6 收入/成本时点 PASS。本切片 UC-SAL-01 ②③④⑥⑦⑧⑨ 的 L5 行为证据直接引用该报告。
  - `2026-07-28-0400-arm-ma2-sales-state-machine.md`（7 实体 × 三轴状态机）：§主路径守卫 + §出库 approve 可用量校验销售独有 + §跨域写经 I*Biz Facade + §SoD R3.3 done + §既有 finding 集（P1-MA2-009/056/057 + P2-MA2-010/011/012/013/014/015/056/057/058 + P1-MA1-022）。本切片不复核状态机维度，仅复用其已证实行为。
  - `2026-07-29-0430-arm-ma4-pur-sal-inv-qa-crm-code-quality.md`（A4.5 sales 采样）：6 Processor + SalAcctDocProvider + 3 PostingDispatcher + DeliveryStockMoveBuilder + ReceiptSettler 代码质量；裁决 ⚠️(P1)（P1-MA4-021/022 已 resolved）。本切片未重复审查代码质量维度。
  - `2026-07-29-0749-arm-ma4-pur-sal-inv-view-xml-drift.md`（A4.7）：sales 域**零漂移**。
  - `2026-07-29-1430-arm-ma5-e2e-effectiveness.md`（MA5）：orchestration E2E（o2c-chain.spec.ts / o2c-reverse.spec.ts）评 **strong**（行级凭证断言）；`TestErpSalPricingEndToEnd` 评 **仅冒烟**（与 P2-RC-018 一致）；`TestErpSalInvoicePosting`/`TestErpSalOrderToCashEnd` 评 **中-强/强**（与 §3 一致）。
- **本切片只补的需求视角差异**（MA2 未覆盖）：
  1. **UC-SAL-01 ① 订单级可用量校验缺失**（P1-RC-020）：MA2 §2.1 仅证实"出库审核时跨域可用量校验已落实"（控制点不在订单审核），未从 L1 行为链路 step 1 字面视角审视"订单审核触发"控制点偏离；本切片从 L1 字面视角定级 P1。
  2. **UC-SAL-11 ⑥ 最低价校验缺失**（P1-RC-021）：MA2 状态机/代码质量/E2E 维度均无价格子系统此控制点对象（GlDistribution 不存在代码），本切片从需求契约视角首次定级。
  3. **UC-SAL-11 ⑦ 价税分离缺失**（P1-RC-022）：MA2 未审促销应用层价税分离公式，本切片从 L1 字面视角定级。
  4. **UC-SAL-01 ⑤ SALES_DELIVERY 命名漂移**（P2-RC-016）：MA2 §2.6 证实"SALES_OUTPUT 借 6401/Cr 1401 实现存货估值红冲"，未从 L1 字面 businessType 命名视角审视偏离；本切片从 L1 字面视角定级 P2（行为等价 cosmetic）。
  5. **UC-SAL-01 ⑥ JUnit AR 凭证断言强度**（P2-RC-017）：MA5 已评级 `TestErpSalInvoicePosting` 为中-强（仅合计+计数），本切片从需求契约视角定级 P2（断言强度，E2E 行级补充）。
  6. **UC-SAL-11 ④⑤⑥⑦ 价格端到端断言强度**（P2-RC-018）：MA5 已评级 `TestErpSalPricingEndToEnd` 为仅冒烟，本切片从需求契约视角定级 P2（断言强度，单测层强覆盖）。
- **MA2 finding 复核无升级**：本切片复核 MA2 已登记的 11+ 项 sales finding（P1-MA2-009 多币种 O2C / P1-MA2-056/057 + P2-MA2-010~015/056/057/058 + P1-MA1-022），运行时行为与 MA2 登记一致，**无升级 P0**（对齐 MA2 §6 + A2.9 结论）。
- **报告校正项**：`docs/audits/2026-07-06-use-case-implementation-audit.md:71-86` 部分 UC 标 ✅ 建立在早期引用上（UC-SAL-01 标 ✅ 但未核订单级可用量校验控制点偏离；UC-SAL-11 标 ✅ 但未核最低价+价税分离缺失），本切片从 L1 字面视角补需求契约差异。

---

## 10. Verdict

**Verdict: passes requirement-compliance audit**（带 3 项 P1 残留 + 3 项 P2 successor + 多数验收标准接受）

**审查范围**：UC-SAL-01/11 共 2 UC 五级追踪矩阵（L1-L5）+ 每 UC 符合性结论（§2 判据）+ 与 arm-index 衔接（§7 复用/新增裁决）+ 静态存疑点清单（供 MA4 A4.1 展开）+ 过程纪律自检 + 与 MA2 差异增量声明。

**接受类**：UC-SAL-01 ②③④⑥⑦⑧⑨（回链 + 库存扣减 + AR 凭证 + posted + 收款派生 + 客户应收双层设计 G6）+ UC-SAL-01 ⑨（双层设计已文档化 G6）+ UC-SAL-11 ①②③④⑤（价格清单 + 取价优先级链位置 G5 + 促销引擎主体）。

**P1 残留**：P1-RC-020（UC-SAL-01 ① 订单级可用量校验缺失）/ P1-RC-021（UC-SAL-11 ⑥ 最低价校验缺失）/ P1-RC-022（UC-SAL-11 ⑦ 价税分离缺失）→ MR1（R1.0 展开为 RC-R1.n），**修复均为纯 BizModel 代码逻辑修复**（不触及 ORM/会计过账核心路径），按 roadmap 预授权类目可自动执行，不触发 §5 ask-first。

**P2 successor**：P2-RC-016（UC-SAL-01 ⑤ SALES_DELIVERY 命名漂移，行为等价 cosmetic，纯文档修复）/ P2-RC-017（UC-SAL-01 ⑥ JUnit AR 凭证仅合计，纯测试补充）/ P2-RC-018（UC-SAL-11 ④⑤⑥⑦ 价格端到端仅冒烟，纯测试补充）→ successor watch-only 不强制。

**P0**：无。不触发 MR0。

**剩余风险**：见 §7 静态存疑点清单（5 项交 MA4 A4.1 运行时展开）。
