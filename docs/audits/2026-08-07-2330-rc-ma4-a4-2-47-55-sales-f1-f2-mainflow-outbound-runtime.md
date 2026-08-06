# rc-ma4-a4-2-47-55-sales-f1-f2-mainflow-outbound-runtime 销售主流程/取价/出库并发运行时确认

> Plan: `docs/plans/2026-08-07-2330-1-rc-ma4-a4-2-47-55-sales-f1-f2-mainflow-outbound-runtime.md`
> Mission: requirement-compliance（MA4 运行时行为验证 切片 A4.2.47-A4.2.55）
> Work Item: A4.2.47 / A4.2.48 / A4.2.49 / A4.2.50 / A4.2.51 / A4.2.52 / A4.2.53 / A4.2.54 / A4.2.55（A1.18 + A1.19 §7 九项静态存疑点运行时确认）
> 来源: `docs/backlog/requirement-compliance-roadmap.md` A4.2.47-A4.2.55
> Audit Status: closed

## 0. 与既有 MA1/A1.x 报告差异增量声明（§9）

本报告**只补运行时行为证据**（methodology §去重协议），不重审 A1.18/A1.19 已裁决的需求符合性结论与 finding 分级：

- **A1.18**（`docs/audits/2026-08-03-0430-rc-ma1-a1-18-sales-f1-mainflow-pricing.md`）：UC-SAL-01/11 五级追踪 + §7 五项静态存疑点 + §6 finding 衔接裁决（P1-RC-020/021/022 新建 + P2-RC-016/017/018 新建 + 接受 on ②③④⑤⑥⑦⑧⑨/①②③④⑤）。本报告复用其 L3 代码路径静态判定 + §6 finding 编号，只补**订单审核守卫复核 / 促销配置触发面普查 / 价税分离凭证行追踪 / 应收余额双层一致性 / 取价优先级链跨域协作 / Processor 守卫复核**的运行时证据。
- **A1.19**（`docs/audits/2026-08-03-0530-rc-ma1-a1-19-sales-f2-outbound-concurrency.md`）：UC-SAL-02/03/10 五级追踪 + §7 五项静态存疑点 + §6 finding 衔接裁决（P1-RC-020 reuse + P2-RC-019/020/021 新建 + 接受主路径）。本报告复用其 L3 代码路径静态判定 + §6 finding 编号，只补**并发出库 seam 行为分析 / 负库存配置并发边界 / deliveredQuantity 查询返回值 / 1 行×2 分批(60+40) 运行时**的运行时证据。
- **A4.2.47 = A1.18 §7-1 + A1.19 §7-2 合并**：同根因（订单审核 `ErpSalOrderProcessor.validateBusinessRulesForApprove` 不调库存 Facade）同控制点，合并确认一次。
- **MA2 A2.9 sales 状态机** + **MA2 A2.17 并发乐观锁** + **MA2 O2C e2e** + **MA5 E2E**：已证实 sales 7 实体三轴状态机迁移 + 库存域乐观锁 + O2C 主链路 + 行级凭证断言。本报告引用其行为正确性结论，只补运行时差异。

本切片**只补**的运行时差异：(i) A4.2.47 订单级可用量校验缺失运营影响面 + 跨域 Facade seam 控制点确认；(ii) A4.2.48 最低价校验促销配置触发面 census；(iii) A4.2.49 价税分离 taxAmount 偏差 + GL 影响追踪（**业财保护区域探针——只读确认不改过账逻辑**）；(iv) A4.2.50 客户应收余额双层设计运行时一致性；(v) A4.2.51 取价优先级链跨域协作 pricingSource 写入一致性；(vi) A4.2.52 销售级并发 seam 行为分析（同事务/异常传播/重试边界）；(vii) A4.2.53 负库存配置并发边界 census；(viii) A4.2.54 deliveredQuantity 查询实际返回值；(ix) A4.2.55 1 行×2 分批(60+40) 运行时验证。

---

## 1. 存疑点清单与判据（A1.18 + A1.19 §7 九项）

| # | 工作项 | §7 存疑点 | A1.18/A1.19 静态判定 | 运行时判据 |
|---|--------|----------|---------------------|-----------|
| 1 | A4.2.47 | A1.18 §7-1 + A1.19 §7-2 合并（P1-RC-020）订单级可用量校验缺失运行时影响 | 订单审核 `ErpSalOrderProcessor.validateBusinessRulesForApprove:166-170` 无库存 Facade 注入 | 确认订单审核仅 requireCustomerActive + creditLimitChecker；实际校验落点 = 出库审核 `triggerOutgoingMove:241-245` → inv `validateAvailable:116-136`；运营影响 = 接单后到出库才发现缺货（SLA/客户体验类） |
| 2 | A4.2.48 | A1.18 §7-2（P1-RC-021）最低价校验促销配置触发面 | sales `applyPricingRules:96-114` 不调最低价守卫 | 确认 grep `minPrice` 跨 module-sales = 零命中；促销 `applyPercentDiscount` 写 discountRate/discountAmount + setPricingSource(PROMOTION) 可致售价 < SKU.minPrice 无门控 |
| 3 | A4.2.49 | A1.18 §7-3（P1-RC-022）价税分离 GL 偏差 | `recomputeLineAmount:172-179` 仅 setAmount 无 setTaxAmount | 确认促销后 taxAmount 沿用旧值致销项税高估；确认 GL 平衡不破坏（Dr 1131 == Cr 6001 + Cr 2221） |
| 4 | A4.2.50 | A1.18 §7-4（P2-MA2-038）应收余额双层设计一致性 | sales ReceiptSettler receivedAmount + finance openAmount 双路径 | 确认 ReceiptSettler 不直写 partner.receivableBalance，finance `PartnerBalanceUpdater.refresh:33-44` 经辅助账聚合；主路径恒等式成立 |
| 5 | A4.2.51 | A1.18 §7-5 取价优先级链跨域协作 | 取价在 master-data 实现，sales 仅 audit 日志 | 确认 master-data `resolvePriceWithSource:158-176` 返回 source + sales 引擎写 setPricingSource(PROMOTION)；跨域协作 main path 正确 |
| 6 | A4.2.52 | A1.19 §7-1（P2-RC-021）UC-SAL-10 销售 seam 并发行为 | 销售级并发测试为零 | 确认 module-sales grep `Executors|CountDownLatch` = 零命中；sales→inv Facade seam 同 @BizMutation 事务委托 inv 域乐观锁兜底 |
| 7 | A4.2.53 | A1.19 §7-3 负库存配置并发边界 | inv 域负库存并发已测，sales 同批次并发未测 | 确认 `isNegativeStockAllowed:285-288` config 默认 false；config=true 时 `validateAvailable:117-118` 短路无下界；config-gate = 部署启用决策 |
| 8 | A4.2.54 | A1.19 §7-4（P2-RC-019）deliveredQuantity 查询返回值 | 零 writer（与 P2-RC-013 结构等价） | 确认 `setDeliveredQuantity` 零生产 writer；`rollupOrderDeliveryStatus:270-310` 仅写头级；ORM defaultValue=0 → 查询返回 0 |
| 9 | A4.2.55 | A1.19 §7-5（P2-RC-019）1 行×2 分批(60+40) 运行时验证 | deliveredQuantity 不被写入，L1 断言不可静态验证 | 确认 `addLineQuantities:387-395` 按 orderLineId 聚合跨出库；1 行×60+40 → 头级 DELIVERED + 行级 deliveredQuantity=0 + 库存正确扣减 100 |

---

## 2. 运行时证据采集（L3 `file:line` + L4 强断言）

### 2-1 A4.2.47 订单级可用量校验缺失运行时影响确认 — 维持 P1-RC-020

**L3 路径追踪**（live code 实测）：

- `module-sales/erp-sal-service/src/main/java/app/erp/sal/service/processor/ErpSalOrderProcessor.java:54-85` `@Inject` 簇：`IDaoProvider` / `IErpMdPartnerBiz` / `CreditLimitChecker` / `IErpFinIntercompanyTransferBiz` / `IErpFinBudgetCommitmentBiz` / 6 个 per-mutation Processor — **无** `IErpInvStockMoveBiz` / `IErpInvStockBalanceBiz` 注入。
- `ErpSalOrderProcessor.java:166-170` `validateBusinessRulesForApprove`：仅 `requireCustomerActive(order, context)` + `creditLimitChecker.check(order.getCustomerId(), order.getTotalAmountWithTax(), order.getExchangeRate(), order.getCode(), context)`。
- 实际可用量校验落点（出库审核）：`module-sales/erp-sal-service/src/main/java/app/erp/sal/service/processor/ErpSalDeliveryProcessor.java:241-245` `triggerOutgoingMove` → `stockMoveBiz.generateMove(request, context)` → 跨域 `ErpInvStockMoveProcessor.doConfirm:86-98`（DRAFT→CONFIRMED 前调 `validateAvailable`）→ `validateAvailable:116-136`（不足抛 `ERR_AVAILABLE_INSUFFICIENT`，@BizMutation 回滚）。
- `ErpInvStockMoveProcessor.java:242-248` `reservesOnConfirm` 销售独有性：仅 `OUTGOING`/`INTERNAL_TRANSFER` 触发校验，`INCOMING` 跳过。

**运营影响面确认**：

- 订单审核通过后**无库存预占/可用量预校验**——订单审核成功不代表库存可满足，库存不足要到出库审核才暴露。
- 影响 = SLA/客户体验类（销售员接单后才发现缺货，承诺无法兑现），**不破坏活跃数据**（出库审核仍守卫，不会超卖）+ **不破坏会计过账**（无 GL 影响）+ **O2C 核心循环完整**（订单→出库→发票→收款）。

**L4 测试覆盖**：`TestErpSalOrderApproval` 11 方法全为状态迁移/客户激活/信用额度，**零库存可用量断言**（Javadoc 明示"仅状态推进，不触发库存/凭证"）；`TestErpSalDeliveryStockMove#testApproveInsufficientAvailableRollsBack:121-155` 强断言**出库级**回滚（错误码 + approveStatus=SUBMITTED + posted=false + 无 DONE 移动单 + 余额不变=5）。

**裁决**：运行时确认 A1.18 §7-1 + A1.19 §7-2 静态判定成立——**维持 P1-RC-020 P1**（L1 字面"订单审核触发"控制点 vs 实现"出库审核触发"行为实质偏离；L1↔L3 真相源冲突按 §4 L1 胜 + §9 冻结不直改真相源，修复方向[补订单级校验 OR 修 L1 措辞]须人工裁决）。**不触发 MR0**（可用量校验功能存在[出库审核环节]+不破坏活跃数据+不破坏会计过账+O2C 核心循环完整，非 §2 P0③ 核心循环断裂）。

### 2-2 A4.2.48 最低价校验促销配置触发面确认 — 维持 P1-RC-021

**L3 路径追踪**（grep census）：

- `grep minPrice|MinPrice|MIN_PRICE|min_price` 跨 `module-sales/`（含 erp-sal-service / dao / api）= **0 命中**（sales 层零 minPrice 引用）。
- `module-sales/erp-sal-service/src/main/java/app/erp/sal/service/entity/ErpSalOrderBizModel.java:96-114` `applyPricingRules`：加载订单行 → 解析 customerGroup → `pricingRuleEngine.evaluate` → `persistPricingResult:143-160`，**完全不调用最低价守卫**。
- `module-sales/erp-sal-service/src/main/java/app/erp/sal/service/support/ErpSalPricingRuleEngine.java:173-186` `applyPercentDiscount`：`discountRate=percent` + `discountAmount=gross×percent/100` + `line.setPricingSource(PRICING_SOURCE_PROMOTION)`——促销后行级金额变更但无 minPrice 比对。
- UC-MD-04 守卫存在于 master-data：`module-master-data/erp-md-service/src/main/java/app/erp/md/service/entity/ErpMdMaterialSkuBizModel.java:180-203` `validatePrice`：`deriveMinPrice:189` + `finalVal.compareTo(minPrice) < 0` → HARD 抛 `ERR_PRICE_BELOW_MIN` / WARN 放行；但由 master-data 取价路径触发，**sales 促销应用层完全缺失**。

**触发面确认**：

- 促销配置（`ErpSalPricingRule` discountPercent，如 PERCENT_DISCOUNT 50% off）应用后行级 `amount` 减少但 sales 层无 minPrice 比对 → 最终售价可能 < SKU.minPrice 无门控。
- 影响范围 = 价格管控功能缺失（销售员配置错误促销规则[折扣过深]时订单以低于底线价格成交），**不破坏活跃数据**（CRUD + 主路径过账正确，订单仍可成交）+ **不破坏会计过账**（GL 平衡不变）。

**裁决**：运行时确认 A1.18 §7-2 静态判定成立——**维持 P1-RC-021 P1**（Q4 强制实现，sales 促销应用层完全缺失；修复归 MR1 纯 BizModel/Processor 预授权[`applyPricingRules` 在 `persistPricingResult` 后追加调 `IErpMdMaterialSkuBiz.validatePrice` 或直接比对 SKU.minPrice]，不触 §5 ask-first）。**不触发 MR0**（不破坏活跃数据 + 不破坏会计过账）。

### 2-3 A4.2.49 价税分离缺失实际 GL 偏差确认 — 维持 P1-RC-022（READ-ONLY 业财保护区域探针）

**L3 路径追踪**（**业财保护区域探针——只读确认，不改过账逻辑**）：

- `module-sales/erp-sal-service/src/main/java/app/erp/sal/service/entity/ErpSalOrderBizModel.java:172-179` `recomputeLineAmount`：
  ```java
  BigDecimal gross = unitPrice.multiply(qty);
  BigDecimal discountAmt = nullSafe(line.getDiscountAmount());
  BigDecimal net = gross.subtract(discountAmt).max(BigDecimal.ZERO);
  line.setAmount(net.setScale(4, RoundingMode.HALF_UP));
  ```
  **仅 setAmount，不 setTaxAmount**。
- `ErpSalOrderBizModel.java:181-197` `recomputeOrderTotals`：`totalAmount = Σ line.getAmount()` + `totalTaxAmount = Σ line.getTaxAmount()`（:187 沿用促销**前**旧 taxAmount）+ `totalAmountWithTax = totalAmount + totalTaxAmount`（:196）。
- 构造促销场景追踪（PERCENT_DISCOUNT 50% off on 行 amount=100, taxRate=0.13，促销前 taxAmount=11.50）：
  - 促销后 `discountAmount=50` → `net=50` → `line.setAmount(50)`，但 `line.taxAmount` 保持 **11.50**（未按 L1 公式 `50 / 1.13 × 0.13 = 5.75` 重算）。
  - L1 公式（`use-cases.md:253-255`）：`税额 = 折扣后金额 / (1 + 税率) × 税率 = 50 / 1.13 × 0.13 = 5.75`；实际 taxAmount=**11.50**（高估 +5.75）。

**GL 影响分析**（AR_INVOICE 凭证范式，`SalAcctDocProvider.createFacts:73-93` Dr 1131/Cr 6001/Cr 2221）：

- **GL 平衡不破坏**：`Dr 1131 应收 = totalAmount + totalTaxAmount = 50 + 11.50 = 61.50` == `Cr 6001 收入(50) + Cr 2221 销项税(11.50) = 61.50`，借贷仍平衡。
- **偏差在销项税 vs 收入分配非总额**：销项税（Cr 2221）高估 5.75（应 5.75，实 11.50），应收（Dr 1131）同步高估 5.75；收入（Cr 6001 取 amount）正确。
- 发票过账时 AR_INVOICE 销项税取自 `invoice.totalTaxAmount`（沿用订单 totalTaxAmount）→ 销项税高估传播到 GL（属管理会计准确性缺口，非活跃数据破坏，因 GL 仍平衡）。

**裁决**：运行时确认 A1.18 §7-3 静态判定成立——**维持 P1-RC-022 P1**（Q4 会计准确性类无例外，价税分离公式完全缺失；修复归 MR1 触 `recomputeLineAmount:172-179`/`recomputeOrderTotals:181-197` 核心路径须 ask-first + 独立 plan-audit §5 会计过账逻辑类）。**不触发 MR0**（GL 平衡不破坏——Dr 1131 == Cr 6001 + Cr 2221 仍成立；偏差在销项税 vs 收入分配非总额，属 §2 P1① 功能完全缺失非 P0④ GL 不平衡；会计错误未活跃致试算不平衡——偏差在科目分配非借贷失衡，试算平衡仍通过）。

### 2-4 A4.2.50 客户应收余额双层设计运行时一致性确认 — 维持 P2-MA2-038 watch-only

**L3 路径追踪**（双路径 census）：

- **sales 域侧**（`ReceiptSettler`，仅更新发票/收款域级派生）：`module-sales/erp-sal-service/src/main/java/app/erp/sal/service/entity/ReceiptSettler.java:55-111` `settle`：守卫（receipt APPROVED :56-61 + amount ≤ invoiceBalance :82-88 + amount ≤ receiptRemaining :89-94 + 同客户 :141-159）→ 写 ErpSalReceiptLine :96-100 → `recomputeInvoiceReceived:161-177` 仅写 `invoice.setReceivedAmount` + `invoice.setReceivedStatus`（UNRECEIVED/PARTIAL/RECEIVED）+ `recomputeReceiptWrittenOff:179-194` 仅写 `receipt.setWrittenOffStatus`。**ReceiptSettler 不直接更新 `ErpMdPartner.receivableBalance`**。
- **finance 辅助账侧**（`PartnerBalanceUpdater`，经辅助账聚合更新客户应收余额）：`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/reconciliation/PartnerBalanceUpdater.java:33-44` `refresh`：`partner.setReceivableBalance(sumOpen(partnerId, DIRECTION_RECEIVABLE))` + `partner.setPayableBalance(sumOpen(partnerId, DIRECTION_PAYABLE))`。`sumOpen:46-62`：`eq("partnerId") + eq("direction") + notIn("status", [SETTLED, CANCELLED])` 聚合 `ErpFinArApItem.openAmountFunctional`（排除 SETTLED/CANCELLED，红冲/credit memo 负金额自然减计）。

**主路径恒等式成立**（L1 字面「客户应收余额 == 发票金额 - 已核销金额」）：

- finance 辅助账层 `sumOpen` = Σ 未核销 AR openAmount（含 AR_INVOICE 正项 + RECEIPT 核销回减 + SALES_RETURN credit memo 负项），即「发票金额 - 已核销金额」语义经辅助账聚合正确实现。
- sales 域侧 receivedAmount（发票级）+ finance 辅助账 openAmount（partner 级）双路径各自正确，行为并行（设计非分歧，MA2 A2.5c §2.3 + `TestErpSalOrderToCashEnd:333-336` 文档化）。

**裁决**：运行时确认 A1.18 §7-4 静态判定成立——**维持 P2-MA2-038 watch-only**（主路径一致性成立，finance `PartnerBalanceUpdater.refresh` 经辅助账聚合正确实现 L1 恒等式；边界场景[域侧 receivedAmount 与辅助账 openAmount 双路径无对账守卫理论可 diverge]归 P2-MA2-038 `DualSideConsistencyChecker` successor 跟踪）。**不触发 MR0**（主路径一致性成立 + 不破坏活跃数据 + 不破坏会计过账）。

### 2-5 A4.2.51 取价优先级链跨域协作运行时一致性确认 — 主路径接受，闭合

**L3 路径追踪**（跨域协作 main path）：

- **取价优先级链在 master-data 实现**（UC-MD-03 归属 A1.41）：`module-master-data/erp-md-service/src/main/java/app/erp/md/service/entity/ErpMdMaterialSkuBizModel.java:130-154` `resolvePrice`：manualPrice :135-137 → customerPriceResolver（SPI）:140-146 → supplierPriceResolver（SPI）:147-152 → `pickDefaultTierPrice` SKU 默认档 :153。`resolvePriceWithSource:158-176` 返回 `ResolvedPrice(unitPrice, source, ...)` 含 pricingSource 标记。
- **sales 层消费 master-data 取价结果**：sales `ErpSalCustomerPriceResolver`（implements `IErpMdCustomerPriceResolver` SPI，由 master-data 注入）实现客户价格清单匹配。
- **sales 促销引擎写 pricingSource**：`module-sales/erp-sal-service/src/main/java/app/erp/sal/service/support/ErpSalPricingRuleEngine.java:185/200/213` `applyPercentDiscount`/`applyPriceOverride`/`addGiftLine` 均 `line.setPricingSource(PRICING_SOURCE_PROMOTION)`（常量 `:45`）。
- **sales 审核时审计 pricingSource 分布**（仅记录日志，不重取价，不驱动取价优先级链）：`ErpSalOrderProcessor.java:248-269` `auditPricingSourceDistribution` 统计 PRICING_SOURCE_MANUAL/PRICE_LIST/PROMOTION/SKU_DEFAULT 分布写 LOG。

**跨域协作一致性确认**：

- master-data 取价后 pricingSource（MANUAL/PRICE_LIST/SKU_DEFAULT）+ sales 促销应用后 pricingSource（PROMOTION）各自写入 orderLine.pricingSource，`auditPricingSourceDistribution` 日志读取这些值记录——跨域协作 main path 一致（与 A1.41 master-data 切片协同，L1 明示"与 UC-MD-03 一致"）。

**裁决**：运行时确认 A1.18 §7-5 静态判定成立——**主路径接受，闭合**（取价优先级链在 master-data 实现[L1 跨域设计选择]，sales 层消费结果 + 促销引擎写 PROMOTION source + audit 日志记录，跨域协作 main path 一致）。**无 finding 升级**（G5 设计选择，非缺陷）。

### 2-6 A4.2.52 UC-SAL-10 销售级 seam 并发行为确认 — 维持 P2-RC-021 watch-only

**L3 路径追踪**（Facade seam + 乐观锁兜底）：

- **sales→inv Facade seam**：`ErpSalDeliveryProcessor.triggerOutgoingMove:241-245` → `stockMoveBiz.generateMove(request, context)`（跨域 `IErpInvStockMoveBiz` Facade）→ inv 域 `ErpInvStockMoveProcessor.doConfirm:86-98` + `doComplete:100-114`。销售出库侧无独立锁，完全委托库存域同一 `@BizMutation` 事务（无 REQUIRES_NEW 隔离）。
- **inv 域乐观锁完备**（A2.17 sustained）：`module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/stock/StockMoveBookkeeper.java:255-328` `updateBalanceWithRetry`：
  - `:260-261` maxRetry = `AppConfig.var("erp-inv.concurrent-deduct-max-retry", 5)`；
  - `:271` `dao.tryUpdateWithVersionCheck(current)`（`UPDATE WHERE id=? AND version=?` 0 行→冲突）；
  - `:284-286` 无冲突返回；
  - `:290` 冲突计量 `ErpInvConcurrencyMetrics.recordOptimisticLockFailure`；
  - `:293-297` 耗尽抛 `buildConflictExhaustedEx`；
  - `:299-326` 冲突→evict→按自然键/主键 reload 重算。
- **UK 兜底**：`module-inventory/model/app-erp-inventory.orm.xml:415` `UK_INV_STOCK_BALANCE_NATURAL`（orgId/materialId/skuId/warehouseId/locationId/batchNo/ownerId，P0-MA2-020 已落地）兜底 INSERT 竞态。

**销售级并发测试覆盖 census**：

- `grep Executors|newFixedThreadPool|newCachedThreadPool|CountDownLatch` 跨 `module-sales/` = **0 命中**（销售级并发 seam 测试为零）。
- inv 域 `TestErpInvConcurrentDeduct` 6 测试（3 单线程版本偏斜模拟 + 3 真实多线程 ExecutorService+CountDownLatch）强覆盖库存域（A2.17 PASS）。

**行为分析**：

- sales→inv Facade seam 在同 @BizMutation 事务，异常传播经 @BizMutation 回滚（`ERR_AVAILABLE_INSUFFICIENT`/`ERR_INV_CONCURRENT_DEDUCT_CONFLICT` 裸抛触发回滚）；重试边界在 inv 域 `updateBalanceWithRetry` 内部闭环（sales 侧不感知重试）。
- 销售→库存并发扣批次理论上若 Facade seam 存在缺陷（如 RPC 重试不当），现有测试不可见——但 inv 域乐观锁兜底覆盖跨域并发（与 A4.2.1/A4.2.2 mfg reservation 同根因家族）。

**裁决**：运行时确认 A1.19 §7-1 静态判定成立——**主路径接受（inv 域乐观锁兜底），维持 P2-RC-021 watch-only**（销售级 seam 无独立并发测试覆盖；测试覆盖缺口归 MR2 follow-up 非本审计修复）。**不触发 MR0**（inv 域乐观锁 + UK 兜底完备，跨域并发不超卖经 A2.17 sustained）。

### 2-7 A4.2.53 负库存配置下并发结果确认 — 主路径接受，维持 config-gate watch-only residual

**L3 路径追踪**（config 消费点 census）：

- `module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/ErpInvConstants.java:14` `CONFIG_ALLOW_NEGATIVE_STOCK = "erp-inv.allow-negative-stock"`。
- `module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/processor/ErpInvStockMoveProcessor.java:285-288` `isNegativeStockAllowed`：`AppConfig.var(CONFIG_ALLOW_NEGATIVE_STOCK, Boolean.FALSE)` → **默认 false**。
- `ErpInvStockMoveProcessor.java:116-122` `validateAvailable`：`if (isNegativeStockAllowed()) return;`（短路，跳过可用量校验）+ `if (!reservesOnConfirm(move.getMoveType())) return;` → 否则逐行 `available < required` 抛 `ERR_AVAILABLE_INSUFFICIENT`。

**并发边界确认**：

- config 默认 false → `validateAvailable` 正常守卫（不足抛异常回滚，不超卖）。
- config=true 时 `validateAvailable:117-118` 短路（无下界检查）→ sales 出库同批次并发可下探至负余额；inv 域 `TestErpInvConcurrentDeduct#testConcurrentDeductWithNegativeStockAllowed:189`（2 线程 × 各扣 2 from initial 2，config=true → 最终 -2）强覆盖 inv 域负库存并发；sales 出库同批次并发最终余额下限 = 理论可负（无下界），由 config=true 部署决策承担。

**config-gate 部署普查**（对齐 A4.1.4/A4.2.12 范式）：

- config 默认 false + 零生产 application.yaml override（A4.2.12 已普查同型 config-gate 范式）→ **非默认活跃**；config-gate = 部署启用决策（非契约缺失），对齐 A4.1.4/A4.2.12 范式。

**裁决**：运行时确认 A1.19 §7-3 静态判定成立——**主路径接受（config 默认关闭），维持 config-gate watch-only residual**（config=true 部署启用时负库存并发边界由部署决策承担，非契约缺失；记录报告非 arm-index 新行，对齐 A4.1.4/A4.2.12 范式）。**不触发 MR0**（config 默认 false 非默认活跃路径）。

### 2-8 A4.2.54 deliveredQuantity 查询实际返回值确认 — 维持 P2-RC-019

**L3 路径追踪**（writer census）：

- `module-sales/model/app-erp-sales.orm.xml:402` `<column name="deliveredQuantity" code="DELIVERED_QUANTITY" ... defaultValue="0" ...>`（列存在，defaultValue=0）。
- `grep setDeliveredQuantity` 全仓 = **6 命中**，全部非生产 writer：
  - `module-sales/erp-sal-api/.../ErpSalOrderLineInputBean.java:206`（生成 GraphQL Input DTO setter）
  - `module-sales/erp-sal-api/.../ErpSalOrderLineOutputBean.java:207`（生成 GraphQL Output DTO setter）
  - `module-sales/erp-sal-dao/.../dao/entity/_gen/_ErpSalOrderLine.java:632`（生成框架反序列化）
  - `module-sales/erp-sal-dao/.../dao/entity/_gen/_ErpSalOrderLine.java:1228`（生成框架 setter）
  - `module-manufacturing/erp-mfg-service/src/test/.../TestErpMfgMrpEngine.java:350`（MFG 测试 seed ZERO）
  - `module-manufacturing/erp-mfg-service/src/test/.../TestErpMfgMrpEndToEnd.java:321`（MFG 测试 seed ZERO）
- **零生产 service/processor/BizModel writer**。
- `ErpSalDeliveryProcessor.java:270-310` `rollupOrderDeliveryStatus`：内部聚合 `Σ approved DeliveryLine.qty by orderLineId`（`addLineQuantities:387-395` map.merge by orderLineId）→ 3 态字符串（:301-308）→ `orderBiz.updateDeliveryStatus(orderId, rolled, context)`（:309）**仅写订单头 deliveryStatus**（UNDELIVERED/PARTIAL/DELIVERED），**不写 orderLine.deliveredQuantity**。

**查询返回值确认**：

- ORM `defaultValue="0"` → delivery 审核后查询 `orderLine.deliveredQuantity` 返回 **0**（非 null，ORM defaultValue 兜底）。
- 与 P2-RC-013（purchase `receivedQuantity` 列存在零 writer）**结构等价不同域**（header 级进度跟踪主路径 OK，行级派生字段零 writer）。

**裁决**：运行时确认 A1.19 §7-4 静态判定成立——**维持 P2-RC-019 P2**（§2 P2① 次要验收标准未完全满足——派生字段列存在但未写入，header 级进度跟踪可用；与 P2-RC-013 同型不同域/UC）。修复归 MR1 纯 Processor 预授权[`rollupOrderDeliveryStatus` 计算结果写到 orderLine.deliveredQuantity]，登记不强制。**不触发 MR0**（不影响审核流程，仅影响行级进度报表/查询）。

### 2-9 A4.2.55 1 行×2 分批(60+40) 运行时验证 — 维持 P2-RC-019

**L3 路径追踪**（rollup 聚合逻辑追踪）：

- `ErpSalDeliveryProcessor.java:270-310` `rollupOrderDeliveryStatus`：
  - `:280-287` `deliveredByOrderLine` map：先加当前出库行（:281 `addLineQuantities(deliveredByOrderLine, loadLines(currentDelivery.getId()))`）+ 遍历**所有**已审核出库（:282-287 `for (ErpSalDelivery d : findApprovedDeliveries(orderId))`，跳过自身）累加。
  - `:387-395` `addLineQuantities`：`map.merge(dl.getOrderLineId(), qty, BigDecimal::add)`——**按 orderLineId 聚合跨出库数量**。
  - `:289-300` 判定：`anyDelivered`（任一行 delivered>0）/ `allFullyDelivered`（所有行 delivered >= ordered）。
  - `:301-309` 3 态：DELIVERED / PARTIAL / UNDELIVERED → `orderBiz.updateDeliveryStatus`（仅写头级）。

**1 行×2 分批(60+40) 运行时追踪**：

- 构造：1 订单行 qty=100 + 出库1(60) + 出库2(40)，两出库行均 `orderLineId = 同一 orderLine.id`。
- 出库1 审核后 `rollupOrderDeliveryStatus`：`deliveredByOrderLine[orderLineId] = 60`（:281 当前）+ `findApprovedDeliveries` 此时仅出库1 → `delivered=60 < ordered=100` → `anyDelivered=true, allFullyDelivered=false` → 头级 **PARTIAL**。
- 出库2 审核后 `rollupOrderDeliveryStatus`：`deliveredByOrderLine[orderLineId] = 40`（:281 当前出库2）+ `findApprovedDeliveries` 含出库1（:286 加 60）→ `delivered=100 == ordered=100` → `allFullyDelivered=true` → 头级 **DELIVERED**。
- **行级 deliveredQuantity**：两次出库审核均不写 `orderLine.deliveredQuantity`（零 writer，见 §2-8）→ **始终 0**。
- **库存余额**：两次出库各扣 60/40 → 总扣 100（库存正确扣减，经 inv 域 `StockMoveBookkeeper.updateBalanceWithRetry` 乐观锁兜底，跨出库不超扣）。

**裁决**：运行时确认 A1.19 §7-5 静态判定成立——**维持 P2-RC-019 P2**（与 A4.2.54 同根因同控制点——`rollupOrderDeliveryStatus` 仅写头级 deliveryStatus 不写行级 deliveredQuantity；头级 rollup 正确[UNDELIVERED→PARTIAL→DELIVERED]，行级 deliveredQuantity 缺失不影响库存正确性；登记不强制，修复归 MR1 纯 Processor 预授权与 P2-RC-019/P2-RC-020 协同）。**不触发 MR0**（库存正确扣减 + 头级进度跟踪正确 + 不破坏活跃数据）。

---

## 3. 测试证据汇总（L4，断言强度）

| 工作项 | 测试 | 断言强度 | 覆盖验收标准 |
|--------|------|---------|-------------|
| A4.2.47 | `TestErpSalOrderApproval`（11 方法，状态机+信用强）+ `TestErpSalDeliveryStockMove#testApproveInsufficientAvailableRollsBack:121-155` | 强（出库级回滚）；订单级零库存断言 | UC-SAL-01 ① + UC-SAL-02 出库级回滚（**订单级可用量校验零覆盖 = P1-RC-020 证据**） |
| A4.2.48 | `TestErpSalPricingRuleEngine#testPercentDiscountLine:36-52`（促销引擎强） | 强（促销算法）；**sales minPrice 零测试** | UC-SAL-11 ④ 促销（**⑥ 最低价零实现零测试 = P1-RC-021 证据**） |
| A4.2.49 | `TestErpSalPricingRuleEngine`（促销单测强） | 强（促销算法）；**价税分离零测试** | UC-SAL-11 ④ 促销（**⑦ 价税分离零实现零测试 = P1-RC-022 证据**） |
| A4.2.50 | `TestErpSalOrderToCashEnd:167-173`（发票侧 receivedStatus）+ `:305-323`（finance 核销层 openAmount）+ `:333-336`（双层设计文档化） | 强（域侧+辅助账双路径） | UC-SAL-01 ⑨ 客户应收余额（双层设计 G6） |
| A4.2.51 | `TestErpSalPricingRuleEngine`（10 方法，pricingSource=PROMOTION 强）+ master-data 取价测试（A1.41） | 强（pricingSource 写入） | UC-SAL-11 ② 取价优先级链（跨域协作 G5） |
| A4.2.52 | `TestErpInvConcurrentDeduct`（6 测试，inv 域强覆盖含 3 真实多线程） | 强（inv 域）；**sales 域零并发测试** | UC-SAL-10 库存域乐观锁（**销售级 seam 零覆盖 = P2-RC-021 证据**） |
| A4.2.53 | `TestErpInvConcurrentDeduct#testConcurrentDeductWithNegativeStockAllowed:189`（inv 域负库存并发） | 强（inv 域 config=true → -2） | UC-SAL-10 负库存并发（config-gate） |
| A4.2.54 | `TestErpSalDeliveryStockMove#testDeliveryStatusRollupToOrder:212-247`（头级强） | 强（头级 deliveryStatus）；**行级 deliveredQuantity 零断言** | UC-SAL-03 头级 rollup（**行级派生零覆盖 = P2-RC-019 证据**） |
| A4.2.55 | （无 1 行×2 分批(60+40) 测试） | — | UC-SAL-03 1 行×2 分批结构（**零覆盖 = P2-RC-020 证据**，与 P2-RC-019 互为因果） |

---

## 4. 运行时行为证据（L5）

- **复用 A1.18 §4**（UC-SAL-01/11 主路径行为）：O2C 主链路（订单审核→出库审核→发票审核→收款审核→核销）+ 价格主体（价格清单 SPI 解析 + 促销规则纯函数引擎）经 MA2 O2C e2e + A2.9 sales 状态机 + A1.1 业财过账引擎三重证实。本切片只补九项存疑点的运行时差异。
- **复用 A1.19 §4**（UC-SAL-02/03/10 主路径行为）：出库级回滚 + 头级 deliveryStatus rollup + 发票/收款 receivedStatus 派生 + 库存域乐观锁完备经 A2.17 + A2.9 + O2C 三重证实。本切片只补九项存疑点的运行时差异。
- **复用 MA2 A2.17**（UC-SAL-10 库存域乐观锁）：`StockMoveBookkeeper.updateBalanceWithRetry:255-328` tryUpdateWithVersionCheck + 重试上限 5 + UK 兜底 + L4 真实多线程不超卖 sustained。本切片只补销售级 seam 行为分析（同事务委托 inv 域乐观锁）。
- **本切片补的运行时差异**（经 live code 实测 + L4 强断言/grep census）：
  - **A4.2.47** 订单级校验缺失运营影响：`ErpSalOrderProcessor.validateBusinessRulesForApprove:166-170` 仅 requireCustomerActive + creditLimitChecker，零库存 Facade 注入；接单后到出库才发现缺货。
  - **A4.2.48** 最低价促销触发面：grep minPrice 跨 module-sales = 零命中；促销引擎写 discountAmount + PROMOTION source 无 minPrice 比对。
  - **A4.2.49** 价税分离 GL 偏差：`recomputeLineAmount:172-179` 仅 setAmount；促销后 taxAmount 沿用旧值高估，GL 平衡不破坏（Dr 1131 == Cr 6001 + Cr 2221）。
  - **A4.2.50** 应收余额双层一致性：`ReceiptSettler` 不直写 partner.receivableBalance，finance `PartnerBalanceUpdater.refresh:33-44` 经辅助账 sumOpen 聚合；主路径恒等式成立。
  - **A4.2.51** 取价链跨域协作：master-data `resolvePriceWithSource:158-176` 返回 source + sales 引擎写 PROMOTION source；audit 日志记录分布。
  - **A4.2.52** 销售 seam 并发：sales grep Executors|CountDownLatch = 零命中；inv 域乐观锁兜底跨域并发。
  - **A4.2.53** 负库存并发边界：config 默认 false + `validateAvailable:117-118` 短路；config-gate = 部署决策。
  - **A4.2.54** deliveredQuantity 查询：零生产 writer + ORM defaultValue=0 → 查询返回 0。
  - **A4.2.55** 1 行×2 分批：`addLineQuantities:387-395` 按 orderLineId 聚合跨出库；头级 PARTIAL→DELIVERED + 行级 0 + 库存扣 100。

---

## 5. 符合性结论（九项存疑点裁决）

### 5.1 九项裁决矩阵

| 工作项 | §7 存疑点 | §2 判据命中分支 | 运行时裁决 | finding 衔接 |
|--------|----------|----------------|-----------|-------------|
| **A4.2.47** | 订单级可用量校验缺失（A1.18 §7-1 + A1.19 §7-2 合并） | 维持 P1 + 运行时证据补强 | **维持 P1-RC-020 P1**（L1↔L3 真相源冲突按 §9 冻结不直改真相源，修复方向须人工裁决） | P1-RC-020 :164 |
| **A4.2.48** | 最低价校验促销配置触发面 | 维持 P1 + 运行时证据补强 | **维持 P1-RC-021 P1**（Q4 强制实现，修复归 MR1 纯 BizModel/Processor 预授权） | P1-RC-021 :165 |
| **A4.2.49** | 价税分离 GL 偏差（READ-ONLY 业财探针） | 维持 P1 + 运行时证据记录 | **维持 P1-RC-022 P1**（Q4 会计准确性类无例外，修复归 MR1 触核心路径须 ask-first） | P1-RC-022 :166 |
| **A4.2.50** | 应收余额双层设计一致性 | 维持 watch-only | **维持 P2-MA2-038 watch-only**（主路径一致，边界场景 successor 跟踪） | P2-MA2-038 :737 |
| **A4.2.51** | 取价优先级链跨域协作 | 主路径闭合 | **主路径接受，闭合**（L1 跨域设计选择 G5） | 无新 finding |
| **A4.2.52** | UC-SAL-10 销售 seam 并发行为 | 维持 P2 watch-only | **主路径接受（inv 域乐观锁兜底），维持 P2-RC-021 watch-only**（销售级 seam 无独立并发测试覆盖） | P2-RC-021 :172 |
| **A4.2.53** | 负库存配置并发边界 | config-gate watch-only residual | **主路径接受（config 默认关闭），维持 config-gate watch-only residual**（对齐 A4.1.4/A4.2.12 范式） | 无新 finding（范式注记） |
| **A4.2.54** | deliveredQuantity 查询返回值 | 维持 P2 + 运行时证据记录 | **维持 P2-RC-019 P2**（与 P2-RC-013 同型不同域，登记不强制） | P2-RC-019 :170 |
| **A4.2.55** | 1 行×2 分批(60+40) 运行时验证 | 维持 P2 + 运行时证据记录 | **维持 P2-RC-019 P2**（与 A4.2.54 同根因同控制点，登记不强制） | P2-RC-019 :170 + P2-RC-020 :171 |

### 5.2 裁决分支汇总

- **一项主路径闭合**（A4.2.51）→ 取价优先级链跨域协作行为正确，无新 finding。
- **一项 config-gate 主路径接受 + watch-only residual**（A4.2.53）→ config 默认关闭，部署决策。
- **三项维持 P1 + 运行时证据补强/记录**（A4.2.47 → P1-RC-020 / A4.2.48 → P1-RC-021 / A4.2.49 → P1-RC-022）→ Q4 强制实现，修复归 MR1 R1.0 展开器（P1-RC-020 L1↔L3 冲突须人工裁决 / P1-RC-021 纯 BizModel 预授权 / P1-RC-022 触核心路径须 ask-first）。
- **一项维持 watch-only**（A4.2.50 → P2-MA2-038）→ 主路径一致，边界场景 successor 跟踪。
- **一项主路径接受 + 维持 P2 watch-only**（A4.2.52 → P2-RC-021）→ inv 域乐观锁兜底，销售级 seam 测试缺口归 MR2 follow-up。
- **两项维持 P2 + 运行时证据记录**（A4.2.54/A4.2.55 → P2-RC-019）→ 与 P2-RC-013 同型不同域，登记不强制，修复归 MR1 纯 Processor 预授权。
- **零升级触发 MR0**（运行时未发现活跃数据破坏或会计错误已活跃——A4.2.49 GL 平衡不破坏 + 偏差在科目分配非借贷失衡，试算平衡仍通过；A4.2.47 出库仍守卫不超卖；A4.2.52 inv 域乐观锁完备不超卖）。
- **零新 finding**（全部经 grep arm-index 同域同控制点比对，维持既有分级不撤销，无未经比对直接新建的 finding）。

---

## 6. 与 arm-index 衔接（复用维持裁决）

### 6.1 比对表

| 本切片存疑点 | 比对 arm-index | 裁决 | 差异依据 |
|-------------|---------------|------|---------|
| A4.2.47 订单级校验缺失 | `P1-RC-020`（:164，A1.18 新建 + A1.19 reuse）| **维持 P1** | 同根因同控制点：`ErpSalOrderProcessor.validateBusinessRulesForApprove:166-170` 零库存 Facade 注入确认；A1.18 §7-1 + A1.19 §7-2 合并投影 |
| A4.2.48 最低价缺失 | `P1-RC-021`（:165，A1.18 新建）| **维持 P1** | 同根因同控制点：grep minPrice 跨 module-sales = 零命中；sales applyPricingRules 不调守卫确认 |
| A4.2.49 价税分离缺失 | `P1-RC-022`（:166，A1.18 新建 + A1.21 reuse）| **维持 P1** | 同根因同控制点：`recomputeLineAmount:172-179` 仅 setAmount 无 setTaxAmount 确认；GL 平衡不破坏 |
| A4.2.50 应收余额双层 | `P2-MA2-038`（:737，A2.5c watch-only）| **维持 watch-only** | 同根因同控制点：`PartnerBalanceUpdater.refresh:33-44` 经辅助账聚合 + ReceiptSettler 不直写 partner.receivableBalance 确认 |
| A4.2.52 销售 seam 并发 | `P2-RC-021`（:172，A1.19 新建）| **维持 P2 watch-only** | 同根因同控制点：sales grep Executors|CountDownLatch = 零命中；inv 域乐观锁兜底确认 |
| A4.2.54/A4.2.55 deliveredQuantity | `P2-RC-019`（:170，A1.19 新建）+ `P2-RC-020`（:171，A1.19 新建）| **维持 P2** | 同根因同控制点：零生产 writer + rollupOrderDeliveryStatus 仅写头级 + ORM defaultValue=0 确认 |

### 6.2 新 finding 清单

- **无**（零新 finding；全部经 grep arm-index 同域同控制点比对后给出「维持既有分级」裁决）。

### 6.3 复用 finding 交叉引用注记（追加 RC A4.2.47-55 运行时确认）

- **P1-RC-020**（:164，UC-SAL-01 ① + UC-SAL-02 订单级可用量校验缺失）：追加 RC A4.2.47 运行时确认注记——`ErpSalOrderProcessor.validateBusinessRulesForApprove:166-170` 仅 requireCustomerActive + creditLimitChecker.check + @Inject :54-85 零 IErpInvStockMoveBiz/IErpInvStockBalanceBiz 确认；实际校验落点 = 出库审核 `triggerOutgoingMove:241-245` → inv `validateAvailable:116-136`；运营影响 = 接单后到出库才发现缺货（SLA/客户体验类，不破坏活跃数据/会计过账/O2C 核心循环）。维持 P1（L1↔L3 真相源冲突按 §9 冻结不直改真相源，修复方向须人工裁决）。
- **P1-RC-021**（:165，UC-SAL-11 ⑥ 最低价校验缺失）：追加 RC A4.2.48 运行时确认注记——grep minPrice 跨 module-sales = 零命中；sales `applyPricingRules:96-114` 不调最低价守卫；促销引擎 `applyPercentDiscount:173-186` 写 discountAmount + PROMOTION source 无 minPrice 比对；master-data `validatePrice:180-203` 守卫存在但 sales 促销应用层完全缺失。维持 P1（修复归 MR1 纯 BizModel/Processor 预授权）。
- **P1-RC-022**（:166，UC-SAL-11 ⑦ 价税分离缺失）：追加 RC A4.2.49 运行时确认注记——`recomputeLineAmount:172-179` 仅 setAmount 无 setTaxAmount + `recomputeOrderTotals:181-197` 沿用促销前旧 taxAmount；构造促销场景追踪 taxAmount 高估（应 5.75 实 11.50）；GL 平衡不破坏（Dr 1131 == Cr 6001 + Cr 2221）。维持 P1（READ-ONLY 业财探针未改过账逻辑；修复归 MR1 触 recomputeLineAmount/recomputeOrderTotals 核心路径须 ask-first）。
- **P2-MA2-038**（:737，域侧-finance 双路径无对账守卫）：追加 RC A4.2.50 运行时确认注记——`ReceiptSettler` 仅更新 invoice.receivedAmount/receivedStatus + receipt.writtenOffStatus，不直写 partner.receivableBalance；finance `PartnerBalanceUpdater.refresh:33-44` + `sumOpen:46-62` 经 ErpFinArApItem（DIRECTION_RECEIVABLE，排除 SETTLED/CANCELLED）聚合；主路径恒等式成立。维持 watch-only（边界场景 successor 跟踪）。
- **P2-RC-021**（:172，UC-SAL-10 销售级并发 seam 测试缺失）：追加 RC A4.2.52 运行时确认注记——sales grep Executors|CountDownLatch = 零命中；sales→inv Facade seam 同 @BizMutation 事务委托 inv 域 `updateBalanceWithRetry:255-328` 乐观锁 + UK 兜底；销售级 seam 无独立并发测试覆盖。维持 P2 watch-only（测试覆盖缺口归 MR2 follow-up）。
- **P2-RC-019**（:170，UC-SAL-03 行级 deliveredQuantity 派生）+ **P2-RC-020**（:171，1 行×2 分批测试缺失）：追加 RC A4.2.54/A4.2.55 运行时确认注记——`setDeliveredQuantity` 零生产 writer（6 命中全为生成 bean/DTO + MFG 测试 seed ZERO）+ `rollupOrderDeliveryStatus:270-310` 仅写头级 + ORM defaultValue=0 → 查询返回 0；`addLineQuantities:387-395` 按 orderLineId 聚合跨出库 → 1 行×60+40 头级 PARTIAL→DELIVERED + 行级 deliveredQuantity=0 + 库存扣 100。维持 P2（与 P2-RC-013 同型不同域，登记不强制，修复归 MR1 纯 Processor 预授权）。

---

## 7. 过程纪律自检

- [x] **checker 退出码门控核查**：本报告为只读运行时确认（**零生产代码/ORM/api.xml/view.xml/config 默认值/真相源变更**），checker 无回归风险（actual == baseline，0 漂移）。区分门控退出码 vs 纯 reporter 退出码——checker 脚本是纯 reporter（退出码恒 0），真正门控在 CI workflow 解析 actual > baseline。本报告不以 checker 脚本退出码 0 作为门控通过依据；**无代码变更故无 build/test 回归风险**。
- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告全部 9 项运行时裁决已按 §去重协议 grep arm-index 同域同控制点后给出「维持既有分级」结论（P1-RC-020/P1-RC-021/P1-RC-022 维持 P1 + P2-MA2-038 维持 watch-only + P2-RC-021 维持 P2 watch-only + P2-RC-019/P2-RC-020 维持 P2 + A4.2.51 主路径闭合 + A4.2.53 config-gate watch-only residual 对齐 A4.1.4/A4.2.12 范式），**无未经比对直接新建的 finding，无新 finding 新建**。
- [x] **业财保护区域探针纪律声明**：A4.2.49（价税分离 taxAmount 偏差 + GL 影响追踪）触及业财保护区域探针——**只读确认，不改过账逻辑/recomputeLineAmount/recomputeOrderTotals 核心路径**（READ-ONLY 标记）。零生产代码变更。
- [x] **A4.2.47 合并声明**：A1.18 §7-1（UC-SAL-01 ① 订单级可用量校验）+ A1.19 §7-2（UC-SAL-02 订单级可用量校验回滚场景）同根因（订单审核 `ErpSalOrderProcessor.validateBusinessRulesForApprove:166-170` 零库存 Facade）同控制点（同一订单审核站点），合并确认一次——roadmap A4.2.47 行覆盖两处来源（P1-RC-020 复用），done 后两处（A1.18 §7-1 投影 + A1.19 §7-2 投影）同步标记 done。

---

## 8. 报告 9 段完整性自检

| # | 段落 | 状态 |
|---|------|------|
| 1 | 存疑点清单与判据（A1.18 + A1.19 §7 九项 + 判据） | ✅ §1 |
| 2 | 运行时证据采集（L3 file:line + L4 强断言/grep census，九项逐项） | ✅ §2 |
| 3 | 测试证据汇总（L4 Test*.java + 断言强度） | ✅ §3 |
| 4 | 运行时行为证据（L5 复用 A1.18/A1.19 §4 + MA2 A2.17 + 本切片差异） | ✅ §4 |
| 5 | 符合性结论（九项裁决矩阵 + §2 判据命中分支） | ✅ §5 |
| 6 | 与 arm-index 衔接（复用维持裁决 + 交叉引用注记） | ✅ §6 |
| 7 | 过程纪律自检（checker actual==baseline + 独立性 + 交叉去重 + 业财保护区域探针纪律 + A4.2.47 合并声明） | ✅ §7 |
| 8 | 报告 9 段完整性自检 | ✅ §8 |
| 9 | 与既有 MA1/A1.x 报告差异增量声明 | ✅ §0 |

**9 段齐全**——本报告可定稿。

---

## 整体裁决

**PASS（九项存疑点全数收口，一项主路径闭合 + 一项 config-gate 主路径接受 watch-only residual + 三项维持 P1 + 一项维持 watch-only + 一项主路径接受维持 P2 watch-only + 两项维持 P2，零新 finding / 不触发 MR0）**：

- **A4.2.51 主路径闭合**——取价优先级链跨域协作行为正确（master-data 取价 + sales 促销写 PROMOTION source + audit 日志），L1 跨域设计选择 G5 非缺陷。
- **A4.2.53 主路径接受 + config-gate watch-only residual**——`erp-inv.allow-negative-stock` config 默认 false + 零生产 override；config=true 部署启用时负库存并发边界由部署决策承担，对齐 A4.1.4/A4.2.12 范式。
- **A4.2.47/A4.2.48/A4.2.49 维持 P1**——P1-RC-020（订单级可用量校验缺失，L1↔L3 真相源冲突按 §9 冻结不直改真相源，修复方向须人工裁决）/ P1-RC-021（最低价校验缺失，修复归 MR1 纯 BizModel/Processor 预授权）/ P1-RC-022（价税分离缺失，修复归 MR1 触 recomputeLineAmount/recomputeOrderTotals 核心路径须 ask-first + 独立 plan-audit §5）。
- **A4.2.50 维持 P2-MA2-038 watch-only**——主路径应收余额恒等式成立（finance `PartnerBalanceUpdater.refresh` 经辅助账聚合），边界场景 successor 跟踪。
- **A4.2.52 主路径接受 + 维持 P2-RC-021 watch-only**——inv 域乐观锁兜底跨域并发不超卖，销售级 seam 无独立并发测试覆盖归 MR2 follow-up。
- **A4.2.54/A4.2.55 维持 P2-RC-019 P2**——deliveredQuantity 零生产 writer + ORM defaultValue=0 → 查询返回 0；1 行×60+40 头级 PARTIAL→DELIVERED + 行级 0 + 库存扣 100；与 P2-RC-013 同型不同域，登记不强制，修复归 MR1 纯 Processor 预授权。

**A1.18 + A1.19 §7 九项静态判定无一翻转**，零新 finding，不触发 MR0，不归 MR1（本审计）。P1-RC-020/P1-RC-021/P1-RC-022 修复义务归 MR1 R1.0 展开器（P1-RC-020 L1↔L3 冲突须人工裁决 / P1-RC-021 纯 BizModel 预授权 / P1-RC-022 触核心路径须 ask-first）；P2-RC-019/P2-RC-020/P2-RC-021 successor watch-only 不强制；P2-MA2-038 successor watch-only。**本审计不实施修复**（只读审计，结果表面 = 本报告 + arm-index 交叉引用注记 + roadmap/log 同步）。
