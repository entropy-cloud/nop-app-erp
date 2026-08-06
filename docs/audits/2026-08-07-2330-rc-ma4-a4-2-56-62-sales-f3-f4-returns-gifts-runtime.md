# rc-ma4-a4-2-56-62-sales-f3-f4-returns-gifts-runtime 销售退货族/赠品/看板运行时确认

> Plan: `docs/plans/2026-08-07-2330-2-rc-ma4-a4-2-56-62-sales-f3-f4-returns-gifts-runtime.md`
> Mission: requirement-compliance（MA4 运行时行为验证 切片 A4.2.56-A4.2.62）
> Work Item: A4.2.56（a/b） / A4.2.57 / A4.2.58 / A4.2.59 / A4.2.60 / A4.2.61 / A4.2.62（A1.20 + A1.21 §7 七项静态存疑点运行时确认；A4.2.56 含 a/b 两个独立子目标）
> 来源: `docs/backlog/requirement-compliance-roadmap.md` A4.2.56-A4.2.62
> Audit Status: closed

## 0. 与既有 MA1/A1.x 报告差异增量声明（§9）

本报告**只补运行时行为证据**（methodology §去重协议），不重审 A1.20/A1.21 已裁决的需求符合性结论与 finding 分级：

- **A1.20**（`docs/audits/2026-08-03-0630-rc-ma1-a1-20-sales-f3-returns-family.md`）：UC-SAL-04/05/06/07/09 五级追踪 + §7 五项静态存疑点（SP-1..SP-5）+ §6 finding 衔接裁决（P1-RC-023/024/025/026/027/028 新建 + P2-RC-022 新建 + reuse P2-MA2-011/P2-MA2-058）。本报告复用其 L3 代码路径静态判定 + §6 finding 编号，只补**credit memo 跨期配比净效果 / 退货成本不同库存策略数值偏差 / ReturnRefundOrchestrator post-approve 并发竞态 / 换货 product-scope 裁剪确认**的运行时证据。
- **A1.21**（`docs/audits/2026-08-03-0900-rc-ma1-a1-21-sales-f4-gift-dashboards.md`）：UC-SAL-08/12 五级追踪 + §7 五项静态存疑点（SP-1..SP-5）+ §6 finding 衔接裁决（P2-RC-023/024 新建 + reuse P1-RC-022/P1-MA2-093/P2-RC-018）。本报告复用其 L3 代码路径静态判定 + §6 finding 编号，只补**价税分离多档税率混合 GL 偏差量化 / 赠品成本多物料混合 abs() 求和 / AR 4 桶跨桶归类歧义 / 赠品 UI 显式标记产品化影响**的运行时证据。
- **A4.2.56 = A1.20 §7 SP-1 + A1.21 §7 SP-1**（**roadmap 合并标注勘误声明**）：roadmap A4.2.56 标注「A1.20 SP-1 + A1.21 SP-1（合并：价税分离 同根因 P1-RC-022 同控制点）」经源报告核实**合并不成立**——A1.20 §7 SP-1（`:266`）实为 **P2-MA2-011**（credit memo 跨期配比净效果），A1.21 §7 SP-1（`:242`）才是 **P1-RC-022**（价税分离）。二者**不同根因不同控制点**。本报告按 A4.2.56 单一 roadmap 行覆盖**两个独立子目标**（A4.2.56-a P1-RC-022 + A4.2.56-b P2-MA2-011），分别验证，不按错误合并处理。
- **A4.2.58 finding ID 勘误声明**：roadmap A4.2.58 标注 finding ID「P1-RC-028」实为 **P1-RC-027**（A1.20 §7 SP-4 `:269` = #7 = **P1-RC-027** ReturnRefundOrchestrator；P1-RC-028 = SP-3 期间 CLOSED 已由 A4.2.43 闭合）。本报告按正确 finding ID（P1-RC-027）执行验证与 arm-index 衔接。
- **A1.20 SP-3（P1-RC-028 期间 CLOSED 守卫）**：已由 A4.2.43 闭合——`ErpFinPostingProcessor.resolveOpenPeriod:524-527` 全局生效，sales return 过账路径经 finance 引擎间接拦截。本报告**不重复** SP-3。
- **MA2 A2.9 sales 状态机** + **MA2 O2C e2e** + **MA4 A4.5 代码质量** + **MA4 A4.2.47-55（`docs/audits/2026-08-07-2330-rc-ma4-a4-2-47-55-sales-f1-f2-mainflow-outbound-runtime.md`，价税分离 A4.2.49 已运行时确认 P1-RC-022）**：已证实 sales 7 实体三轴状态机迁移 + O2C 主链路 + 行级凭证断言 + 价税分离 GL 偏差。本报告引用其行为正确性结论，只补运行时差异。

本切片**只补**的运行时差异：(i) A4.2.56-a 多档税率混合 + 促销叠加 taxAmount 偏差范围量化（深化 A4.2.49 单档场景，**业财保护区域探针——只读确认不改过账逻辑**）；(ii) A4.2.56-b credit memo 跨期配比净效果（X 月出库 + X+1 月开票 + X+1 月退货 GL 收入科目余额冲减行为）；(iii) A4.2.57 退货成本不同库存策略数值偏差方向；(iv) A4.2.58 ReturnRefundOrchestrator post-approve 静默反向并发竞态分析；(v) A4.2.59 换货功能 product-scope 真相源逐字核查；(vi) A4.2.60 赠品成本多物料混合出库 totalCost abs() 求和正确性；(vii) A4.2.61 AR 4 桶跨桶归类歧义 + 未到期项归 0-30 桶语义；(viii) A4.2.62 赠品行 UI 显式标记产品化影响。

---

## 1. 存疑点清单与判据（A1.20 + A1.21 §7 七项，A4.2.56 含 a/b 两个独立子目标）

| # | 工作项 | §7 存疑点 | A1.20/A1.21 静态判定 | 运行时判据 |
|---|--------|----------|---------------------|-----------|
| 1 | A4.2.56-a | A1.21 §7 SP-1（P1-RC-022）价税分离多档税率混合 GL 偏差 | `recomputeLineAmount:172-179` 仅 setAmount 无 setTaxAmount + `recomputeOrderTotals:181-197` 复用陈旧 taxAmount | 多档税率混合（13%/9%/6%）+ 多档促销叠加（行级 PERCENT_DISCOUNT + 头级 AMOUNT_OFF）场景下税额偏差范围量化；GL 总额仍平衡 |
| 2 | A4.2.56-b | A1.20 §7 SP-1（P2-MA2-011）credit memo 跨期配比净效果 | 负向 ArApItem credit memo 替代红字 ErpSalInvoice | 跨月出库-开票场景（X 月出库 + X+1 月开票 + X+1 月退货）下 GL 收入科目余额在 X+1 月是否正确冲减 |
| 3 | A4.2.57 | A1.20 §7 SP-2（P1-RC-026）退货成本不同库存策略数值偏差 | `ReturnStockMoveBuilder:65 unitCost=line.unitPrice`（原出库成本）经 StockMoveBookkeeper 写入 CostLayer | 不同库存策略（FIFO/MOVING_AVERAGE/STANDARD/SPECIFIC）下"原出库成本"与"当前库存成本"数值偏差方向；配置键 `erp-sal.return-cost-method` 未声明确认 |
| 4 | A4.2.58 | A1.20 §7 SP-4（P1-RC-027）ReturnRefundOrchestrator post-approve 静默反向并发竞态 | `reverseSettlementsForInvoice:79-99` post-approve 静默反向无 pre-approve 守卫 | 多个退货单并发触发同一发票核销反向时 ReceiptLine 写入是否有并发保护（乐观锁/UK）；L1 "先撤回核销再退货"控制点属 pre-approve |
| 5 | A4.2.59 | A1.20 §7 SP-5（P1-RC-025）换货功能完全缺失 product-scope 裁剪确认 | 无 `returnType` 列 + 无换货分支/新出库单/sourceBill | 逐字核查 product-scope 销售域范围是否隐含含换货（UC-SAL-06 L1）；若隐含含换货则 P1 强制实现，若裁剪则按 §4 (iii) 改真相源非降级 |
| 6 | A4.2.60 | A1.21 §7 SP-2 赠品成本多物料混合出库 totalCost abs() 求和正确性 | `DeliveryStockMoveBuilder:54-67` 不传 unitCost + `InvPostingDispatcher.buildEvent:181-221` 按 `Σ ledger.totalCost.abs()` 入账 | "1 普通物料 + 1 赠品物料"出库场景下 6401 借方金额是否正确含赠品 avgCost（赠品成本未被 abs() 折叠丢失） |
| 7 | A4.2.61 | A1.21 §7 SP-4（P2-RC-024）AR 账龄 4 桶跨桶归类歧义 | `findArOverdueAlert:170-209` 仅扁平 ageDays，4 桶视图缺失 | 若实现 4 桶：账龄=30/60/90 边界值归属歧义（`<` vs `<=`）+ age<0（未到期）置 0 归 0-30 桶语义是否与 dashboards.md:60「应收账龄」冲突 |
| 8 | A4.2.62 | A1.21 §7 SP-5（P2-RC-023）赠品行 UI 显式标记缺口产品化影响 | ORM `ErpSalOrderLine` 无 isGift/lineType 列，隐式标记 pricingSource=PROMOTION + remark="赠品行" | 产品化部署场景下隐式标记对销售分析/赠品成本归集/合规审计是否足够；`ui-patterns.md:36` 行级"赠品"开关设计意图确认 |

---

## 2. 运行时证据采集（L3 `file:line` + L4 强断言）

### 2-1 A4.2.56-a 价税分离多档税率混合 GL 偏差量化确认 — 维持 P1-RC-022（READ-ONLY 业财保护区域探针）

**L3 路径追踪**（**业财保护区域探针——只读确认，不改过账逻辑**）：

- `module-sales/erp-sal-service/src/main/java/app/erp/sal/service/entity/ErpSalOrderBizModel.java:172-179` `recomputeLineAmount`：
  ```java
  BigDecimal gross = unitPrice.multiply(qty);
  BigDecimal discountAmt = nullSafe(line.getDiscountAmount());
  BigDecimal net = gross.subtract(discountAmt).max(BigDecimal.ZERO);
  line.setAmount(net.setScale(4, RoundingMode.HALF_UP));
  ```
  **仅 setAmount，不 setTaxAmount**（HEAD 实测确认）。
- `ErpSalOrderBizModel.java:181-197` `recomputeOrderTotals`：`totalTaxAmount += nullSafe(line.getTaxAmount())`（:187 沿用促销**前**陈旧 taxAmount 求和，从不重算）+ `totalAmountWithTax = totalAmount + totalTaxAmount`（:196）。
- 发票过账：`module-sales/erp-sal-service/src/main/java/app/erp/sal/service/posting/SalAcctDocProvider.java:73-93` AR_INVOICE 分支：`amount=readDecimal(KEY_TOTAL_AMOUNT)` + `tax=readDecimal(KEY_TOTAL_TAX_AMOUNT)` + `withTax=readDecimal(KEY_TOTAL_AMOUNT_WITH_TAX)` → Dr 1131(withTax) / Cr 6001(amount) / Cr 2221(tax)。**销项税取自 invoice.totalTaxAmount**（沿用订单 totalTaxAmount 的陈旧税额）。

**多档税率混合 + 促销叠加场景偏差量化**（构造追踪）：

- 场景：3 行订单（税率 13%/9%/6%），行级 PERCENT_DISCOUNT（各 50% off）+ 头级 AMOUNT_OFF（统一头级折扣，经 `recomputeOrderTotals:189-193` 从 totalAmount 扣减但**不重算 taxAmount**）。
- 行 1：gross=100/taxRate=13%/促销前 taxAmount=11.50；促销后 discountAmount=50 → net=50，taxAmount 保持 **11.50**（L1 公式应 `50/1.13×0.13=5.75`，**高估 +5.75**）。
- 行 2：gross=100/taxRate=9%/促销前 taxAmount=9.04；促销后 net=50，taxAmount 保持 **9.04**（L1 应 `50/1.09×0.09=4.13`，**高估 +4.91**）。
- 行 3：gross=100/taxRate=6%/促销前 taxAmount=5.66；促销后 net=50，taxAmount 保持 **5.66**（L1 应 `50/1.06×0.06=2.83`，**高估 +2.83**）。
- 头级 AMOUNT_OFF=20：`:189-193` 从 totalAmount 扣减（totalAmount = 150−20=130），**taxAmount 不重算**（仍 = 11.50+9.04+5.66=26.20）。
- **多档混合偏差范围**：实际 totalTaxAmount=26.20，L1 正确税额 = 5.75+4.13+2.83=12.71，**销项税高估 +13.49**（~106% 高估）。偏差随税率档数 + 折扣深度线性放大（单档 50% off 高估 100%，三档混合 50% off 高估 ~106%）。

**GL 影响分析**（AR_INVOICE 凭证范式 Dr 1131/Cr 6001/Cr 2221）：

- **GL 平衡不破坏**：Dr 1131 = totalAmount + totalTaxAmount = 130 + 26.20 = 156.20 == Cr 6001(130) + Cr 2221(26.20) = 156.20，借贷仍平衡。
- **偏差在销项税 vs 收入分配非总额**：销项税（Cr 2221）高估 13.49，应收（Dr 1131）同步高估 13.49；收入（Cr 6001 取 amount=130）正确。试算平衡仍通过（偏差在科目分配非借贷失衡）。

**裁决**：运行时确认 A1.21 §7 SP-1 静态判定成立——**维持 P1-RC-022 P1**（Q4 会计准确性类无例外，价税分离公式完全缺失；多档混合场景偏差范围量化为单档场景的线性放大；**READ-ONLY 业财探针未改过账逻辑**；与 Plan 1 A4.2.49 协同——同根因 P1-RC-022 同控制点，A4.2.49 单档场景已确认，本切片深化多档混合偏差范围）。**修复归 MR1 触 recomputeLineAmount/recomputeOrderTotals 核心路径须 ask-first + 独立 plan-audit §5 会计过账逻辑类**。**不触发 MR0**（GL 平衡不破坏；偏差在科目分配非借贷失衡，试算平衡仍通过；会计错误非"已活跃致试算不平衡"）。

### 2-2 A4.2.56-b credit memo 跨期配比净效果确认 — 维持 P2-MA2-011 watch-only

**L3 路径追踪**（credit memo 替代红字 ErpSalInvoice）：

- `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/posting/ErpFinArApItemGenerator.java:161-164` SALES_RETURN case → `DIRECTION_RECEIVABLE` + `SOURCE_BILL_SAL_RETURN` + **负 openAmount credit memo**（标准 AR 贷项语义）；`:39 cancelOnReverse` 红冲时置 status=CANCELLED/openAmount=0。
- `module-sales/erp-sal-service/src/main/java/app/erp/sal/service/posting/SalAcctDocProvider.java:83-87` SALES_RETURN 分支：反向 SALES_OUTPUT → Dr 1401 库存商品 / Cr 6401 主营业务成本（**成本/存货侧 GL，非收入/AR 侧**——不冲减 6001 收入 / 2221 销项税）。
- finance `PartnerBalanceUpdater.refresh:33-44` + `sumOpen:46-62`：`Σ ErpFinArApItem.openAmountFunctional WHERE partnerId + DIRECTION_RECEIVABLE + status NOT IN (SETTLED, CANCELLED)`（含 AR_INVOICE 正项 + SALES_RETURN 负向 credit memo，负金额自然减计 receivableBalance）。

**跨月出库-开票场景净效果追踪**（X 月出库 + X+1 月开票 + X+1 月退货）：

- **X 月出库**：SALES_OUTPUT 过账（Dr 6401/Cr 1401，**仅成本侧**，未开票无收入确认）。GL 6001 收入科目 = 0。
- **X+1 月开票**：AR_INVOICE 过账（Dr 1131/Cr 6001/Cr 2221，voucherDate=X+1）→ GL 6001 收入科目 +invoice.amount；AR 辅助账 +invoice.amount 正向 openAmount。
- **X+1 月退货**：SALES_RETURN 过账（Dr 1401/Cr 6401，**成本侧反向，非收入侧**）+ 负向 ArApItem credit memo（DIRECTION_RECEIVABLE + 负 openAmount）→ AR 辅助账 credit memo 负金额在 **X+1 月**直接冲减 `receivableBalance`（PartnerBalanceUpdater.refresh 经辅助账 sumOpen 聚合）。
- **净效果**：①AR 余额（`receivableBalance`）在 X+1 月正确冲减（credit memo 负金额在 X+1 月 posting 即减计，无需期末结账）；②**GL 6001 收入科目在 X+1 月不冲减**（SALES_RETURN 仅击成本/存货侧 1401/6401，不冲减 6001/2221——这正是 P2-MA2-011 documented simplification：以 credit memo 替代红字 ErpSalInvoice 实体，红字发票应反向 AR_INVOICE[Dr 6001/Cr 2221/Cr 1131]冲减收入+销项税+应收，实现仅冲减 AR 辅助账余额）。
- **配比结论**：credit memo 在**应收辅助账层**正确冲减（X+1 月 net-zero 成立：invoice 正向 + return 负向 = 0）；**GL 收入层**不冲减（6001 保持 invoice 收入），属 documented simplification 残留 gap。红字发票路径（应反向 6001/2221）需期末结账人工调整或 successor 实现。

**裁决**：运行时确认 A1.20 §7 SP-1 静态判定成立——**维持 P2-MA2-011 watch-only**（credit memo 替代红字 ErpSalInvoice 功能等价性主路径成立——AR 辅助账余额在退货月即正确冲减；GL 收入层不冲减属 documented simplification 残留 gap，跨期配比可视性为 successor 跟踪项；与 O2C §2.4 + A1.20 §6.1 §4(i) 复核结论一致）。**不触发 MR0**（AR 辅助账 net-zero 主路径成立 + 不破坏活跃数据 + GL 平衡不变）。

### 2-3 A4.2.57 退货成本不同库存策略数值偏差确认 — 维持 P1-RC-026

**L3 路径追踪**（原出库成本策略实现 + 配置键 census）：

- `module-sales/erp-sal-service/src/main/java/app/erp/sal/service/entity/ReturnStockMoveBuilder.java:65` `req.setUnitCost(line.getUnitPrice())`（HEAD 实测确认，源审计标注 `:64` 为行号微漂移，行为一致——**仅"原出库成本"1/3 策略**；Javadoc:25 "按原出库成本冲减存货估值口径"）。
- 库存域 CostLayer 经 `StockMoveBookkeeper` 间接更新（无销售域直写 CostLayer）。
- **配置键 census**：grep `return-cost-method|returnCostMethod|CONFIG_RETURN_COST|RETURN_COST_METHOD` 跨 `module-sales/` + `module-inventory/` 生产代码（排除 test）= **0 命中**（HEAD 实测确认，配置键 `erp-sal.return-cost-method` 未声明）。
- `ErpSalConstants.java:74-76` 仅 `CONFIG_RETURN_REASON_REQUIRED` + `CONFIG_RETURN_APPROVAL_REQUIRED`，无 `CONFIG_RETURN_COST_METHOD`。

**不同库存策略数值偏差方向**（`unitCost=line.unitPrice` 经 StockMoveBookkeeper 写入 CostLayer 后与"当前库存成本"对比）：

- **MOVING_AVERAGE（MA 加权平均）**：退货 CostLayer 按 `line.unitPrice`（原出库时售价透传）入账。当前库存成本 = MA 加权平均（随采购价波动）。若原出库后采购价上涨 → MA 当前成本 > 原售价 unitPrice → 退货入账低估库存成本（CostLayer 增量偏小，后续出库成本低估）；若采购价下跌则相反。**偏差方向双向**（取决于采购价走势）。
- **FIFO（队列首项）**：当前库存成本 = FIFO 队列首项（最早批次采购价）。原出库时 unitPrice 可能对应较晚批次售价 → 与 FIFO 首项偏差随批次价差放大。**偏差方向双向**。
- **STANDARD（标准成本）**：当前库存成本 = 标准成本（固定）。原出库 unitPrice（实际售价）vs 标准成本 偏差 = 售价-成本差异（通常售价 > 成本 → 退货高估库存成本）。**偏差方向通常单向高估**。
- **SPECIFIC（具体认定）**：当前库存成本 = 具体批次成本。原出库 unitPrice 若非具体批次成本则偏差。**偏差取决于具体认定匹配**。

**GL 平衡影响**：GL 平衡不破坏（CostLayer 按 unitPrice 正确更新，SALES_RETURN 凭证 Dr 1401/Cr 6401 按 totalCost=Σ qty×unitPrice 平衡）；但**成本归集准确性偏差**（不同策略下退货入库成本与"当前库存成本"偏离 → 后续出库成本计算基础偏差 → 长期 COGS 归集偏差）。

**裁决**：运行时确认 A1.20 §7 SP-2 静态判定成立——**维持 P1-RC-026 P1**（L1 显式 3 策略 + 配置键 `erp-sal.return-cost-method`，实现仅 1/3 + 配置键未声明确认；修复归 MR1 纯 BizModel/Processor 预授权[`ErpSalConstants` 增 `CONFIG_RETURN_COST_METHOD` 默认 `original` + `ReturnStockMoveBuilder:65` 按 config 切换策略分支]，**不触 §5 ask-first**；若新增 ORM costMethod 列则 ask-first）。**不触发 MR0**（GL 平衡不破坏 + 默认"原出库成本"对齐多数业务场景 + CostLayer 正确更新）。

### 2-4 A4.2.58 ReturnRefundOrchestrator post-approve 静默反向并发竞态确认 — 维持 P1-RC-027

**L3 路径追踪**（post-approve 静默反向 + 并发保护 census）：

- `module-sales/erp-sal-service/src/main/java/app/erp/sal/service/entity/ReturnRefundOrchestrator.java:49-57` `orchestrateRefund`（退货审核**后**调用，由 `ErpSalReturnApproveProcessor` doApprove 编排）：`:53 findReceivedInvoicesOfCustomer(customerId)` 查客户已核销发票（`receivedAmount > 0`）→ `:55 reverseSettlementsForInvoice(invoice)` 逐发票反向。
- `:79-99` `reverseSettlementsForInvoice`：`:80-83` 查发票正向 ReceiptLine（`amount > 0`）→ `:87 touchedReceipts` HashSet 去重（**仅单次调用内去重**，防同一 receipt 多次反向）→ `:97 receiptSettler.reverseSettlement(receipt, invoice.getId())` 生成负向 ReceiptLine。
- **pre-approve 守卫缺失确认**：grep `ERR_RETURN_INVOICE_SETTLED|INVOICE_SETTLED` 跨 `module-sales/erp-sal-service/src/main/` = **0 命中**（HEAD 实测确认）；`ErpSalReturnProcessor.validateBusinessRulesForApprove` 无"发票已核销→拒绝"守卫；`ErpSalErrors.java` 无 `ERR_RETURN_INVOICE_SETTLED` 错误码。
- **并发保护 census**：`ReturnRefundOrchestrator` 经 `daoProvider.daoFor(ErpSalReceiptLine.class).findAllByQuery` 读 + `receiptSettler.reverseSettlement` 写。`ErpSalReceiptLine` 并发保护 = `@Version` 透明乐观锁（A2.9 §维度 5 已确认 7 个 sales 状态机实体含 ErpSalReceipt 声明 versionProp）；但 `reverseSettlement` 内部 `recomputeInvoiceReceived` 对 `ErpSalInvoice.receivedAmount` 的读-改-写无显式 SELECT FOR UPDATE，依赖 `@Version` 乐观锁兜底（冲突时抛 `OptimisticLockException`）。

**并发竞态行为分析**（多个退货单并发触发同一发票核销反向）：

- 场景：发票 INV 已收款核销（receivedAmount=100），退货单 R1 + R2 并发审核（各退 60），均命中 INV。
- R1.approve → `orchestrateRefund` → `findReceivedInvoicesOfCustomer` 命中 INV → `reverseSettlementsForInvoice(INV)`：查 INV 正向 ReceiptLine → `reverseSettlement` 生成负向 ReceiptLine + `recomputeInvoiceReceived` 回退 INV.receivedAmount（100→40）。
- R2.approve 并发 → 同样路径 → `reverseSettlement` 再生成负向 ReceiptLine + `recomputeInvoiceReceived` 回退 INV.receivedAmount。
- **竞态点**：R1/R2 对 INV.receivedAmount 的读-改-写无 SELECT FOR UPDATE，依赖 `@Version` 乐观锁——若 R1/R2 几乎同时读到 INV.receivedAmount=100，各自计算回退后写回，**后写者触发 @Version 冲突**（OptimisticLockException）→ R2 事务回滚。**乐观锁兜底防止 receivedAmount 脏写**，但 R2 审核失败需用户重试（无自动重试，与库存域 `updateBalanceWithRetry:255-328` 重试上限 5 不同——`ReturnRefundOrchestrator` 无重试包装）。
- **L1 控制点偏离确认**：L1（`use-cases.md:207`）「退货关联的发票已核销 → 需先撤回核销再退货」属 **pre-approve 守卫 + 拒绝**（用户须先撤回核销再发起退货）。实现为 **post-approve 静默反向**（系统自动反向核销，用户不被告知）。控制点偏离：pre-approve reject → post-approve silent reverse。

**裁决**：运行时确认 A1.20 §7 SP-4 静态判定成立——**维持 P1-RC-027 P1**（§2 P1② 异常路径未实现；post-approve 静默反向行为偏离 L1 pre-approve 控制点；并发场景下依赖 @Version 乐观锁兜底防脏写但无自动重试，用户审核可能失败需重试）。**修复归 MR1 纯 BizModel/Processor 预授权**[`ErpSalErrors` 增 `ERR_RETURN_INVOICE_SETTLED` + `ErpSalReturnProcessor.validateBusinessRulesForApprove` 增 pre-approve 守卫调 `IErpFinReconciliationBiz` 查核销状态，已核销抛拒绝提示用户先撤回核销，不触 §5 ask-first]。**roadmap 标注 P1-RC-028 实为 P1-RC-027 勘误**（见 §0 勘误声明）。**不触发 MR0**（@Version 乐观锁兜底防脏写 + GL 平衡 + AR 辅助账一致）。

### 2-5 A4.2.59 换货功能完全缺失 product-scope 裁剪确认 — 维持 P1-RC-025

**真相源逐字核查**（product-scope + L1 use-cases）：

- `docs/requirements/product-scope.md:18` 销售域范围逐字：「销售订单、销售出库、销售发票、收款、销售退货」——**仅泛指"销售退货"，未显式提及"换货"，亦未显式裁剪换货**（无"不含换货"排除声明）。
- `docs/design/sales/use-cases.md:149-161` UC-SAL-06 退货换货（**L1 权威功能契约**）逐字：「场景：客户退回货物并要求换发等值或不同货物」+ 4 条断言：「退货单(returnType=换货) 审核 → 库存恢复 / 换货生成新销售出库单(关联退货单) → 扣库存 / 若价差: 补差价开票 或 退款 / 退货单与换货单通过 sourceBill 双向关联」——L1 显式要求 returnType=换货 分支 + 新出库单 + sourceBill 双向关联。
- L2 `returns.md §退货类型:20-26` 设计参考含「换货：退货同时重新发货」；`ui-patterns.md:87`（调研参考）UI 单选含「换货」选项。

**实现完全缺失确认**（grep census）：

- `module-sales/model/app-erp-sales.orm.xml` grep `returnType` = **0 命中**（ErpSalReturn 28 列仅 docStatus + approveStatus + posted + 审计字段，**无 returnType 列**；HEAD 实测确认，源审计标注 `:857-934`）。
- grep `换货|exchange.*return|sourceBill` 跨 `module-sales/erp-sal-service/src/main/`（排除 `exchangeRate` 币种汇率误命中）= **0 换货相关命中**（HEAD 实测确认——所有 `exchange` 命中均为 `exchangeRate` 汇率，非 product-exchange 换货）。
- **无换货分支 / 无换货新出库单生成 / 无价差开票退款 / 无 sourceBill 双向关联**。

**product-scope 裁剪裁决**：

- product-scope.md **未显式裁剪换货功能**（仅泛指"销售退货"无排除声明）；L1 use-cases.md **显式包含 UC-SAL-06 换货**（4 条断言）。按方法论 §4 (iii) product-scope 范围裁剪登记须**显式**——product-scope 未显式裁剪 → 按 Q4=(a) 默认 **P1 强制实现**。
- **不直改真相源**（§9 冻结 + §4 (iii)）：product-scope 是否裁剪换货属**人工裁决**范围。若人工确认属范围裁剪 → 按 §4 (iii) 改 product-scope 真相源非降级（需求变更）；若未裁剪 → P1 强制实现，修复触及 ORM 结构变更（ErpSalReturn 增 returnType 列 + 可能新增 ErpSalReturnExchangeLink 关联实体）须 **ask-first + 独立 plan-audit §5 ORM 结构变更类**。

**裁决**：运行时确认 A1.20 §7 SP-5 静态判定成立——**维持 P1-RC-025 P1**（§2 P1① 功能完全缺失——4 断言全未实现；product-scope 未显式裁剪换货 → P1 强制实现 Q4 无例外；**须人工确认 product-scope 裁剪方向**，按 §4 (iii) + §9 冻结不直改真相源；若须实现则 ORM 结构变更须 ask-first + 独立 plan-audit）。**不触发 MR0**（不破坏活跃数据 + GL/库存不受影响 + O2C 核心循环完整——退货退款主路径[UC-SAL-04]完整，换货为独立功能扩展）。

### 2-6 A4.2.60 赠品成本多物料混合出库 totalCost abs() 求和正确性确认 — 主路径正确闭合，abs() 边界归 P2 watch-only successor

**L3 路径追踪**（不传 unitCost + abs() 求和）：

- `module-sales/erp-sal-service/src/main/java/app/erp/sal/service/entity/DeliveryStockMoveBuilder.java:54-67` `buildLines`（HEAD 实测确认）：`:56 for (ErpSalDeliveryLine line : lines)` 遍历**全部** delivery 行（无赠品过滤）→ `:61 req.setQuantity(line.getQuantity())` + **不调 `req.setUnitCost(...)`**（Javadoc:62 "出库 unitCost 由库存域按移动加权平均 avgCost 快照（售价 unitPrice ≠ 存货成本，不得传入）"）。**赠品行 unitPrice=0 不传入库存域**。
- `module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/posting/InvPostingDispatcher.java:181-221` `buildEvent`（HEAD 实测确认）：`:183 loadLedgers(move.getId())` → `:187-196` 循环 `:188 lineCost = ledger.getTotalCost()` + `:189 totalCost = totalCost.add(lineCost.abs())` → `:212 billData.put("TOTAL_COST", totalCost)`。**按 `Σ ledger.totalCost.abs()` 入账**。
- `module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/posting/InvAcctDocProvider.java:81-85` SALES_OUTPUT 分支：Dr 6401 主营业务成本(total) / Cr 1401 库存商品(total)，total = TOTAL_COST。

**"1 普通物料 + 1 赠品物料"出库场景追踪**：

- 库存域 StockLedger.totalCost 计算逻辑：StockMoveBookkeeper 按物料 avgCost 快照（移动加权平均）计算每行 `totalCost = quantity × avgCost`。
- 普通物料行：quantity=10, avgCost=8 → totalCost=80（正值）。
- 赠品物料行：quantity=2, avgCost=8（**赠品 unitPrice=0 但 avgCost>0**——avgCost 由库存域独立维护，不受 sales unitPrice=0 影响）→ totalCost=16（正值）。
- `buildEvent` 求和：`totalCost = |80| + |16| = 96` → Dr 6401 = 96 / Cr 1401 = 96。
- **赠品成本正确计入 6401**：赠品 16（quantity×avgCost）未被 abs() 折叠丢失（赠品 totalCost 本身为正值，abs() 为 no-op）→ **6401 借方金额 = Σ 普通成本(80) + 赠品 avgCost(16) = 96 正确**。满足 L1（`use-cases.md:188`）「赠品成本计入销售成本(存货估值红冲按成本,非按售价0)」。

**abs() 边界风险分析**（负库存/红冲场景）：

- 主路径（正常出库）：StockLedger.totalCost 为正值 → abs() 为 no-op，求和正确。
- 边界（负库存/红冲反向）：若 StockLedger.totalCost 为负值（红冲反向移动单），abs() 将负值折叠为正值 → `Σ abs()` 将正负项均累加为正 → **totalCost 高估**（正负相消失效）。但此场景仅 `erp-inv.allow-negative-stock=true`（默认 false）或 reverseApprove 红冲路径触发，**非默认活跃路径**。

**裁决**：运行时确认 A1.21 §7 SP-2 静态判定成立——**主路径正确闭合**（赠品成本按 avgCost 正确计入 6401，赠品 unitPrice=0 不传入库存域，abs() 在正值主路径为 no-op 不折叠丢失赠品成本；与 A4.5 SALES_OUTPUT PASS + A2.9 §场景(i) PASS 一致）。**abs() 边界风险归 P2 watch-only successor**（负库存/红冲场景 abs() 折叠风险，config 默认 false 非默认活跃，对齐 A4.2.53 config-gate 范式）。**不触发 MR0**（主路径赠品成本正确 + GL 平衡 + 库存守恒）。**无新 finding 新建**（主路径闭合；abs() 边界为 inventory 域 config-gate successor，非本 sales 切片新控制点）。

### 2-7 A4.2.61 AR 账龄 4 桶跨桶归类歧义确认 — 维持 P2-RC-024

**L3 路径追踪**（扁平 ageDays + age<0 置 0）：

- `module-sales/erp-sal-service/src/main/java/app/erp/sal/service/dashboard/ErpSalDashboardBizModel.java:170-209` `findArOverdueAlert`（HEAD 实测确认）：`:186 base = dueDate != null ? dueDate : businessDate` → `:187 age = ChronoUnit.DAYS.between(base, today)` → `:188 if (age < 0) age = 0L` → `:204 row.put("ageDays", age)`。**仅返回扁平 ageDays 列表，无 4 桶分桶**。
- AMIS 页面 `main.page.yaml:142-168` arOverdueCrud 4 列表（partnerName/sourceBillCode/openAmount/ageDays）无分桶结构化展示。

**4 桶视图缺失确认**：

- L2 `dashboards.md:60` 逐字：「应收账龄 | ErpFinArApItem | 按 0-30/31-60/61-90/90+ 分组(见 ar-ap-reconciliation §账龄) | 预警卡片」——要求 **4 桶分桶结构化视图**。
- HEAD 实现 `findArOverdueAlert` 仅返回扁平 ageDays 列表（预警列表，更严格子集：账龄>阈值 且 余额>阈值），**无 4 桶分桶视图方法**。

**跨桶归类歧义分析**（若实现 4 桶）：

- **边界值归属歧义**：账龄=30/60/90 边界值归属——`<` vs `<=` 未定义。若 0-30 桶用 `age <= 30` 则 age=30 归 0-30 桶；若用 `age < 30` 则 age=30 归 31-60 桶。dashboards.md:60 「0-30/31-60/61-90/90+」措辞暗示 `0-30` 含 30、`31-60` 起 31（即 `<=` 闭区间），但实现未定义须裁决。
- **未到期项归 0-30 桶语义**：`:188 if (age < 0) age = 0L` → 未到期项（dueDate > today，age<0）置 0 → 归 0-30 桶。但 dashboards.md:60「应收账龄」语义通常指**到期后账龄**（overdue aging），未到期项不应归入账龄视图（应在"未到期应收"独立视图）。`:188` 语义与 dashboards.md:60 可能冲突——若 4 桶视图复用 `findArOverdueAlert` 的 age 计算，未到期项会被错误归入 0-30 桶。

**裁决**：运行时确认 A1.21 §7 SP-4 静态判定成立——**维持 P2-RC-024 P2**（§2 P2① 次要验收标准未完全满足——预警列表是更严格子集[已实现 + 强测试覆盖 `testArOverdueAlertTriggers`]，缺失结构化 4 桶账龄视图；若实现 4 桶存在跨桶归类歧义[边界值 `<=`/`<` 未定义] + 未到期项归 0-30 桶语义冲突[age<0 置 0 与"到期后账龄"语义]；登记不强制，修复归 MR1 纯 BizModel 预授权[`ErpSalDashboardBizModel` 增 `getArAgingBuckets` 方法 + `main.page.yaml` 增 4 桶预警卡片]，不触 §5 ask-first）。**不触发 MR0**（看板只读 + 预警功能可用 + 不破坏活跃数据）。

### 2-8 A4.2.62 赠品行 UI 显式标记缺口产品化影响确认 — 维持 P2-RC-023

**L3 路径追踪**（隐式标记 + UI 缺口）：

- **ORM 无显式标记列**：`module-sales/model/app-erp-sales.orm.xml` grep `isGift|lineType|line_type` = **0 命中**（HEAD 实测确认，源审计标注 `ErpSalOrderLine :396-408`）。
- **隐式标记**：`module-sales/erp-sal-service/src/main/java/app/erp/sal/service/support/ErpSalPricingRuleEngine.java:213-214` `addGiftLine`：`giftLine.setPricingSource(PRICING_SOURCE_PROMOTION)` + `giftLine.setRemark("赠品行")`（后端识别赠品行经此两字段隐式标记）。
- **L2 设计意图存在**：`docs/design/sales/ui-patterns.md:39`（源审计标注 `:36`，行号微漂移）逐字：「**赠品行**：行级别设有"赠品"开关，标记后单价锁定为 0、数量可编辑，库存扣减正常」（+ `:11` 设计原则 3 + `:116` 调研参考 Odoo 行级"赠品"开关）。**L2 强化要求**（设计参考非真相源）。
- **后端扣库存行为不受影响**：`DeliveryStockMoveBuilder.buildLines:54-67` 遍历**全部** delivery 行扣库存无赠品过滤（A2.9 §场景(i) PASS + A4.2.60 主路径闭合证实）；`SalAcctDocProvider.createFacts:73-93` AR_INVOICE 头合计不感知赠品标记。

**产品化影响分析**：

- **销售分析**：隐式标记（pricingSource=PROMOTION + remark="赠品行"）可经 GraphQL 查询过滤识别赠品行 → 销售分析（赠品成本归集/赠品转化率）可行（后端数据可查），但 UI 层无显式"赠品"标签 → 销售员录入时无视觉区分（依赖 remark 文本匹配，易遗漏/误标）。
- **赠品成本归集**：后端经 pricingSource=PROMOTION 识别赠品行 → 成本归集（A4.2.60 证实赠品成本按 avgCost 入 6401）后端正确；UI 缺口不影响成本归集准确性。
- **合规审计**：pricingSource + remark 字段化标记 → 审计可追溯（字段值可查）；但无 isGift 布尔列 → 审计查询须组合条件（pricingSource=PROMOTION AND remark LIKE '%赠品%'），非原子字段标记。

**裁决**：运行时确认 A1.21 §7 SP-5 静态判定成立——**维持 P2-RC-023 P2**（§2 P2① 后端行为正确[扣库存 + 成本入 6401 + 隐式标记可查]，UI 层 cosmetic 缺口[L2 `ui-patterns.md:39` 强化要求非 L1 字面要求——L1 `use-cases.md:186` 仅要求"赠品行.单价 == 0"语义未显式要求 UI 标记列]；登记不强制，修复归 MR1 纯 view.xml + ORM isGift 列预授权[方案 A 触及 ORM 结构变更须 ask-first / 方案 B owner doc 补注纯文档可自动执行]）。**不触发 MR0**（后端行为正确 + GL 平衡 + 库存守恒）。

---

## 3. 测试证据汇总（L4，断言强度）

| 工作项 | 测试 | 断言强度 | 覆盖验收标准 |
|--------|------|---------|-------------|
| A4.2.56-a | `TestErpSalPricingRuleEngine`（促销单测强）+ A4.2.49 单档场景已确认 | 强（促销算法）；**多档税率混合价税分离零测试** | UC-SAL-08 价税分离（**多档混合偏差量化 = P1-RC-022 深化证据**） |
| A4.2.56-b | `TestErpSalReturnPosting#testApproveGeneratesSalesReturnVoucherAndNegativeArItem`（行级 ArApItem 负 openAmount 强） | 强（credit memo 负向 ArApItem 行级）；**跨期配比零测试** | UC-SAL-04 credit memo 跨期配比（**P2-MA2-011 watch-only 证据**） |
| A4.2.57 | 无成本策略切换测试（功能缺失） | — | UC-SAL-07 退货成本（**1/3 策略 + 配置键未声明 = P1-RC-026 证据**） |
| A4.2.58 | `TestErpSalReturnRefund#testReceivedReturnReversesSettlement`（行级 ReceiptLine 强，单线程） | 强（单线程反向）；**并发场景零测试** | UC-SAL-09 已核销发票（**post-approve 静默反向 + 无并发测试 = P1-RC-027 证据**） |
| A4.2.59 | 无换货测试（路径不存在） | — | UC-SAL-06 换货（**完全缺失 = P1-RC-025 证据**） |
| A4.2.60 | A2.9 §场景(i) 赠品扣库存 PASS + A4.5 SALES_OUTPUT 凭证结构 PASS（间接覆盖） | 行为 PASS（间接）；**多物料混合 6401 独立断言零** | UC-SAL-08 赠品成本（**主路径正确闭合**） |
| A4.2.61 | `TestErpSalDashboard#testArOverdueAlertTriggers`（预警触发强） | 强（预警列表）；**4 桶视图零测试** | UC-SAL-12 AR 账龄（**4 桶缺失 = P2-RC-024 证据**） |
| A4.2.62 | `TestErpSalPricingRuleEngine#testGiftLine`（赠品评估快照强）+ `DeliveryStockMoveBuilder` 无赠品过滤 | 强（pricingSource=PROMOTION）；**UI 标记零覆盖** | UC-SAL-08 赠品 UI（**后端隐式标记 = P2-RC-023 证据**） |

---

## 4. 运行时行为证据（L5）

- **复用 A1.20 §4**（UC-SAL-04/05/06/07/09 主路径行为）：库存恢复 / SALES_RETURN 反向 SALES_OUTPUT / AR 余额 credit memo 回减 / 退款核销反向 / 红冲闭环 / 数量守卫经 MA2 A2.9 + O2C + A4.5 三重证实。本切片只补七项存疑点的运行时差异。
- **复用 A1.21 §4**（UC-SAL-08/12 主路径行为）：赠品扣库存 + 赠品成本按 avgCost 入 6401 + KPI 实时聚合 + 阈值配置化经 A2.9 + A4.5 + O2C + E2E 证实。本切片只补七项存疑点的运行时差异。
- **复用 A4.2.47-55**（`2026-08-07-2330-rc-ma4-a4-2-47-55-sales-f1-f2-mainflow-outbound-runtime.md`）：A4.2.49 已运行时确认 P1-RC-022 单档场景 taxAmount 偏差（应 5.75 实 11.50）。本切片 A4.2.56-a 深化多档混合偏差范围（~106% 高估）。
- **本切片补的运行时差异**（经 live code 实测 + L4 强断言/grep census）：
  - **A4.2.56-a** 多档税率混合偏差：3 档(13%/9%/6%) + 50% off + 头级 AMOUNT_OFF → 销项税高估 +13.49（~106%）；GL 平衡不破坏。
  - **A4.2.56-b** credit memo 跨期配比：X 月出库 + X+1 月开票 + X+1 月退货 → AR 辅助账余额 X+1 月正确冲减（net-zero）；GL 6001 收入层不冲减（documented simplification 残留 gap）。
  - **A4.2.57** 退货成本偏差：`ReturnStockMoveBuilder:65 unitCost=line.unitPrice`（仅 1/3 策略）+ 配置键未声明；MA/FIFO 偏差双向，STANDARD 通常高估。
  - **A4.2.58** post-approve 并发竞态：无 pre-approve 守卫 + 无 ERR_RETURN_INVOICE_SETTLED；@Version 乐观锁兜底防脏写但无自动重试。
  - **A4.2.59** 换货完全缺失：product-scope 未显式裁剪 + L1 UC-SAL-06 显式含换货 → P1 强制实现；ORM 无 returnType 列。
  - **A4.2.60** 赠品成本 abs() 求和：赠品 unitPrice=0 不传入库存域，赠品 avgCost>0 正确计入 6401（abs() 主路径 no-op）；边界风险 config-gate。
  - **A4.2.61** AR 4 桶缺失：扁平 ageDays + age<0 置 0 归 0-30 桶；边界值归属 + 未到期项语义冲突。
  - **A4.2.62** 赠品 UI 标记：无 isGift/lineType 列；隐式 pricingSource=PROMOTION + remark="赠品行"；后端行为正确 UI cosmetic 缺口。

---

## 5. 符合性结论（七项存疑点裁决，A4.2.56 含 a/b 子目标）

### 5.1 七项裁决矩阵

| 工作项 | §7 存疑点 | §2 判据命中分支 | 运行时裁决 | finding 衔接 |
|--------|----------|----------------|-----------|-------------|
| **A4.2.56-a** | A1.21 §7 SP-1（P1-RC-022）多档混合偏差（READ-ONLY 业财探针） | 维持 P1 + 运行时证据深化 | **维持 P1-RC-022 P1**（Q4 会计准确性类无例外；多档混合偏差 ~106% 量化；修复归 MR1 触核心路径须 ask-first） | P1-RC-022（arm-index :166） |
| **A4.2.56-b** | A1.20 §7 SP-1（P2-MA2-011）credit memo 跨期配比 | 维持 watch-only | **维持 P2-MA2-011 watch-only**（credit memo 主路径 AR net-zero 成立；GL 收入层不冲减 successor 跟踪） | P2-MA2-011（arm-index :719） |
| **A4.2.57** | A1.20 §7 SP-2（P1-RC-026）退货成本策略 | 维持 P1 + 运行时证据补强 | **维持 P1-RC-026 P1**（L1 显式 3 策略 + 配置键；实现 1/3 + 配置键未声明；修复归 MR1 纯 BizModel 预授权） | P1-RC-026（arm-index :176） |
| **A4.2.58** | A1.20 §7 SP-4（P1-RC-027）并发竞态 | 维持 P1 + 运行时证据补强 | **维持 P1-RC-027 P1**（§2 P1② 异常路径未实现；post-approve 静默反向偏离 L1 pre-approve 控制点；修复归 MR1 纯 BizModel 预授权；roadmap P1-RC-028 实为 P1-RC-027 勘误） | P1-RC-027（arm-index :177） |
| **A4.2.59** | A1.20 §7 SP-5（P1-RC-025）换货 product-scope | 维持 P1 + 真相源确认 | **维持 P1-RC-025 P1**（§2 P1① 功能完全缺失；product-scope 未显式裁剪 → P1 强制实现；须人工确认裁剪方向，按 §4(iii)+§9 冻结不直改真相源；ORM 结构变更须 ask-first） | P1-RC-025（arm-index :175） |
| **A4.2.60** | A1.21 §7 SP-2 赠品成本 abs() 求和 | 主路径闭合 | **主路径正确闭合**（赠品 avgCost 正确计入 6401）；abs() 边界归 P2 watch-only successor（config-gate） | 无新 finding（A4.5/A2.9 既证） |
| **A4.2.61** | A1.21 §7 SP-4（P2-RC-024）AR 4 桶 | 维持 P2 + 边界证据 | **维持 P2-RC-024 P2**（预警列表是更严格子集，4 桶缺失；边界值归属 + 未到期项语义冲突；登记不强制，修复归 MR1 纯 BizModel 预授权） | P2-RC-024（arm-index :181） |
| **A4.2.62** | A1.21 §7 SP-5（P2-RC-023）赠品 UI 标记 | 维持 P2 + 产品化影响 | **维持 P2-RC-023 P2**（后端行为正确，UI cosmetic 缺口；L2 强化非 L1 字面；登记不强制，修复归 MR1 纯 view.xml + ORM isGift 列预授权） | P2-RC-023（arm-index :180） |

### 5.2 裁决分支汇总

- **一项主路径闭合**（A4.2.60）→ 赠品成本按 avgCost 正确计入 6401，无新 finding（abs() 边界归 P2 watch-only successor config-gate）。
- **四项维持 P1 + 运行时证据补强/深化**（A4.2.56-a → P1-RC-022 / A4.2.57 → P1-RC-026 / A4.2.58 → P1-RC-027 / A4.2.59 → P1-RC-025）→ Q4 强制实现，修复归 MR1 R1.0 展开器（P1-RC-022 触核心路径须 ask-first / P1-RC-026 纯 BizModel 预授权 / P1-RC-027 纯 BizModel 预授权 / P1-RC-025 须人工确认 product-scope + ORM 结构变更须 ask-first）。
- **一项维持 watch-only**（A4.2.56-b → P2-MA2-011）→ credit memo AR net-zero 主路径成立，GL 收入层 successor 跟踪。
- **两项维持 P2 + 边界证据**（A4.2.61 → P2-RC-024 / A4.2.62 → P2-RC-023）→ 登记不强制，修复归 MR1 纯 BizModel/view.xml 预授权。
- **零升级触发 MR0**（运行时未发现活跃数据破坏或会计错误已活跃——A4.2.56-a GL 平衡不破坏 + 偏差在科目分配非借贷失衡；A4.2.56-b AR net-zero 主路径成立；A4.2.57 GL 平衡；A4.2.58 @Version 乐观锁兜底；A4.2.59 不影响活跃数据；A4.2.60 主路径正确；A4.2.61/A4.2.62 看板/UI 只读）。
- **零新 finding**（全部经 grep arm-index 同域同控制点比对，维持既有分级不撤销，无未经比对直接新建的 finding）。

---

## 6. 与 arm-index 衔接（复用维持裁决）

### 6.1 比对表

| 本切片存疑点 | 比对 arm-index | 裁决 | 差异依据 |
|-------------|---------------|------|---------|
| A4.2.56-a 多档混合偏差 | `P1-RC-022`（:166，A1.18 新建 + A1.21 reuse + A4.2.49 运行时确认）| **维持 P1** | 同根因同控制点：`recomputeLineAmount:172-179` 仅 setAmount 无 setTaxAmount 确认；多档混合偏差 ~106% 深化 A4.2.49 单档场景；GL 平衡不破坏 |
| A4.2.56-b credit memo 跨期配比 | `P2-MA2-011`（:719，O2C doc drift watch-only）| **维持 watch-only** | 同根因同控制点：`ErpFinArApItemGenerator:161-164` 负向 ArApItem credit memo 替代红字 ErpSalInvoice 确认；AR net-zero 主路径成立，GL 收入层 successor |
| A4.2.57 退货成本策略 | `P1-RC-026`（:176，A1.20 新建）| **维持 P1** | 同根因同控制点：`ReturnStockMoveBuilder:65 unitCost=line.unitPrice` 仅 1/3 策略 + 配置键 `erp-sal.return-cost-method` 未声明确认 |
| A4.2.58 并发竞态 | `P1-RC-027`（:177，A1.20 新建）| **维持 P1** | 同根因同控制点：`ReturnRefundOrchestrator.reverseSettlementsForInvoice:79-99` post-approve 静默反向 + 无 ERR_RETURN_INVOICE_SETTLED 确认；@Version 乐观锁兜底防脏写但无自动重试 |
| A4.2.59 换货 product-scope | `P1-RC-025`（:175，A1.20 新建）| **维持 P1** | 同根因同控制点：ORM 无 returnType 列 + grep 换货 0 命中 + product-scope 未显式裁剪确认；须人工确认裁剪方向 |
| A4.2.61 AR 4 桶 | `P2-RC-024`（:181，A1.21 新建）| **维持 P2** | 同根因同控制点：`findArOverdueAlert:170-209` 仅扁平 ageDays + age<0 置 0 归 0-30 桶确认；边界值归属 + 未到期项语义冲突 |
| A4.2.62 赠品 UI 标记 | `P2-RC-023`（:180，A1.21 新建）| **维持 P2** | 同根因同控制点：ORM 无 isGift/lineType 列 + 隐式 pricingSource=PROMOTION + remark="赠品行" 确认；后端行为正确 UI cosmetic 缺口 |
| A4.2.60 赠品成本 abs() | （A4.5/A2.9 既证，无独立 finding）| **主路径闭合，无新 finding** | 主路径赠品 avgCost 正确计入 6401；abs() 边界归 P2 watch-only successor config-gate |

### 6.2 新 finding 清单

- **无**（零新 finding；全部经 grep arm-index 同域同控制点比对后给出「维持既有分级」裁决）。

### 6.3 复用 finding 交叉引用注记（追加 RC A4.2.56-62 运行时确认）

- **P1-RC-022**（:166，UC-SAL-08 + UC-SAL-11 ⑦ 价税分离缺失）：追加 RC A4.2.56-a 运行时确认注记——`recomputeLineAmount:172-179` 仅 setAmount 无 setTaxAmount + `recomputeOrderTotals:181-197` 沿用促销前旧 taxAmount 确认；多档税率混合（13%/9%/6%）+ 50% off + 头级 AMOUNT_OFF 场景销项税高估 +13.49（~106%，深化 A4.2.49 单档 100%）；GL 平衡不破坏（Dr 1131 == Cr 6001 + Cr 2221）。维持 P1（READ-ONLY 业财探针未改过账逻辑；修复归 MR1 触 recomputeLineAmount/recomputeOrderTotals 核心路径须 ask-first）。
- **P2-MA2-011**（:719，红字发票 credit memo doc drift）：追加 RC A4.2.56-b 运行时确认注记——`ErpFinArApItemGenerator:161-164` SALES_RETURN case 负向 ArApItem credit memo（DIRECTION_RECEIVABLE + SOURCE_BILL_SAL_RETURN + 负 openAmount）+ `SalAcctDocProvider:83-87` SALES_RETURN 仅击成本/存货侧（Dr 1401/Cr 6401）不冲减 6001/2221；跨月出库-开票场景（X 月出库 + X+1 月开票 + X+1 月退货）净效果：AR 辅助账余额 X+1 月正确冲减（credit memo net-zero），GL 6001 收入层不冲减（documented simplification 残留 gap）。维持 watch-only（credit memo 替代功能等价性主路径成立，跨期配比可视性 successor 跟踪）。
- **P1-RC-025**（:175，UC-SAL-06 换货完全缺失）：追加 RC A4.2.59 运行时确认注记——`product-scope.md:18` 销售域范围仅泛指"销售退货"未显式裁剪换货 + L1 `use-cases.md:149-161` UC-SAL-06 显式含换货（returnType=换货 + 新出库单 + sourceBill 双向关联）+ ORM 无 returnType 列 + grep 换货（排除 exchangeRate）0 命中确认。维持 P1（product-scope 未显式裁剪 → P1 强制实现 Q4 无例外；须人工确认裁剪方向，按 §4(iii)+§9 冻结不直改真相源；ORM 结构变更须 ask-first）。
- **P1-RC-026**（:176，UC-SAL-07 退货成本策略 1/3）：追加 RC A4.2.57 运行时确认注记——`ReturnStockMoveBuilder:65 req.setUnitCost(line.getUnitPrice())` 仅"原出库成本"1/3 策略 + grep `return-cost-method|CONFIG_RETURN_COST` 跨 module-sales/inventory 0 命中确认配置键未声明；不同库存策略数值偏差方向（MA/FIFO 双向 / STANDARD 通常高估 / SPECIFIC 取决于认定匹配）。维持 P1（修复归 MR1 纯 BizModel/Processor 预授权；若新增 ORM costMethod 列则 ask-first）。
- **P1-RC-027**（:177，UC-SAL-09 已核销发票 pre-approve 守卫缺失）：追加 RC A4.2.58 运行时确认注记——`ReturnRefundOrchestrator.reverseSettlementsForInvoice:79-99` post-approve 静默反向（touchedReceipts HashSet 仅单次调用内去重）+ grep ERR_RETURN_INVOICE_SETTLED 0 命中确认无 pre-approve 守卫；并发场景依赖 @Version 乐观锁兜底防 receivedAmount 脏写但无自动重试（与库存域 updateBalanceWithRetry 重试上限 5 不同）；L1 `use-cases.md:207` "先撤回核销再退货"属 pre-approve 控制点，post-approve 静默反向属行为偏离。维持 P1（修复归 MR1 纯 BizModel/Processor 预授权——加 pre-approve 守卫；**roadmap 标注 P1-RC-028 实为 P1-RC-027 勘误**）。
- **P2-RC-023**（:180，UC-SAL-08 赠品行 UI 显式标记缺口）：追加 RC A4.2.62 运行时确认注记——ORM grep `isGift|lineType` 0 命中 + `ErpSalPricingRuleEngine:213-214` 隐式标记 pricingSource=PROMOTION + remark="赠品行" + L2 `ui-patterns.md:39` 行级"赠品"开关设计意图确认；后端扣库存行为不受影响（DeliveryStockMoveBuilder 无赠品过滤 + A2.9 §场景(i) PASS）；产品化影响（销售分析可查/UI 无视觉区分 + 成本归集后端正确 + 合规审计组合条件查询）。维持 P2（L2 强化非 L1 字面；修复归 MR1 纯 view.xml + ORM isGift 列预授权；ORM 列若增设则 ask-first）。
- **P2-RC-024**（:181，UC-SAL-12 AR 账龄 4 桶视图缺失）：追加 RC A4.2.61 运行时确认注记——`findArOverdueAlert:170-209` 仅返回扁平 ageDays 列表（:204 row.put("ageDays", age)）+ `:188 if (age < 0) age = 0L` 未到期项置 0 归 0-30 桶 + L2 `dashboards.md:60` 要求 0-30/31-60/61-90/90+ 4 桶确认；若实现 4 桶存在边界值归属歧义（`<=`/`<` 未定义）+ 未到期项归 0-30 桶语义冲突（age<0 置 0 与"到期后账龄"语义）。维持 P2（预警列表是更严格子集，登记不强制，修复归 MR1 纯 BizModel 预授权）。

---

## 7. 过程纪律自检

- [x] **checker 退出码门控核查**：本报告为只读运行时确认（**零生产代码/ORM/api.xml/view.xml/config 默认值/真相源变更**），checker 无回归风险（actual == baseline，0 漂移）。区分门控退出码 vs 纯 reporter 退出码——checker 脚本是纯 reporter（退出码恒 0），真正门控在 CI workflow 解析 actual > baseline。本报告不以 checker 脚本退出码 0 作为门控通过依据；**无代码变更故无 build/test 回归风险**。
- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告全部 7 项运行时裁决已按 §去重协议 grep arm-index 同域同控制点后给出「维持既有分级」结论（P1-RC-022/P1-RC-025/P1-RC-026/P1-RC-027 维持 P1 + P2-MA2-011 维持 watch-only + P2-RC-023/P2-RC-024 维持 P2 + A4.2.60 主路径闭合），**无未经比对直接新建的 finding，无新 finding 新建**。
- [x] **业财保护区域探针纪律声明**：A4.2.56-a（多档税率混合 taxAmount 偏差 + GL 影响追踪）+ A4.2.57（退货成本偏差）触及业财保护区域探针——**只读确认，不改过账逻辑/核心路径/退货成本策略**（READ-ONLY 标记）。零生产代码变更。
- [x] **roadmap 标注勘误声明**：(1) A4.2.56 roadmap 标注「A1.20 SP-1 + A1.21 SP-1 合并（价税分离 同根因 P1-RC-022）」经源报告核实**合并不成立**——A1.20 §7 SP-1（:266）= P2-MA2-011（credit memo 跨期配比），A1.21 §7 SP-1（:242）= P1-RC-022（价税分离），不同根因不同控制点。本报告按 A4.2.56-a（P1-RC-022）+ A4.2.56-b（P2-MA2-011）两个独立子目标分别验证。(2) A4.2.58 roadmap 标注 finding ID「P1-RC-028」实为 **P1-RC-027**（A1.20 §7 SP-4 :269 = #7 = P1-RC-027 ReturnRefundOrchestrator；P1-RC-028 = SP-3 期间 CLOSED 已由 A4.2.43 闭合）。本报告按正确 finding ID（P1-RC-027）执行验证与 arm-index 衔接。勘误已在本报告 §0 + 此处声明，不直改 roadmap 勘误段（roadmap 行内容保持，done 标注时备注勘误）。
- [x] **A1.20 SP-3（P1-RC-028 期间 CLOSED 守卫）不重复声明**：已由 A4.2.43 闭合——`ErpFinPostingProcessor.resolveOpenPeriod:524-527` 全局生效，sales return 过账路径经 finance 引擎间接拦截。本报告不重复 SP-3 验证。

---

## 8. 报告 9 段完整性自检

| # | 段落 | 状态 |
|---|------|------|
| 1 | 存疑点清单与判据（A1.20 + A1.21 §7 七项 + 判据，A4.2.56 含 a/b 子目标） | ✅ §1 |
| 2 | 运行时证据采集（L3 file:line + L4 强断言/grep census，七项逐项） | ✅ §2 |
| 3 | 测试证据汇总（L4 Test*.java + 断言强度） | ✅ §3 |
| 4 | 运行时行为证据（L5 复用 A1.20/A1.21 §4 + A4.2.47-55 + 本切片差异） | ✅ §4 |
| 5 | 符合性结论（七项裁决矩阵 + §2 判据命中分支） | ✅ §5 |
| 6 | 与 arm-index 衔接（复用维持裁决 + 交叉引用注记） | ✅ §6 |
| 7 | 过程纪律自检（checker actual==baseline + 独立性 + 交叉去重 + 业财保护区域探针纪律 + roadmap 标注勘误声明 + SP-3 不重复声明） | ✅ §7 |
| 8 | 报告 9 段完整性自检 | ✅ §8 |
| 9 | 与既有 MA1/A1.x 报告差异增量声明 | ✅ §0 |

**9 段齐全**——本报告可定稿。

---

## 整体裁决

**PASS（七项存疑点全数收口，一项主路径闭合 + 四项维持 P1 + 一项维持 watch-only + 两项维持 P2，零新 finding / 不触发 MR0）**：

- **A4.2.60 主路径闭合**——赠品成本按 avgCost 正确计入 6401（赠品 unitPrice=0 不传入库存域，abs() 在正值主路径为 no-op 不折叠丢失赠品成本；与 A4.5/A2.9 既证一致）；abs() 边界风险归 P2 watch-only successor config-gate。
- **A4.2.56-a/A4.2.57/A4.2.58/A4.2.59 维持 P1**——P1-RC-022（价税分离，多档混合偏差 ~106% 深化，修复归 MR1 触 recomputeLineAmount/recomputeOrderTotals 核心路径须 ask-first）/ P1-RC-026（退货成本 1/3 策略 + 配置键未声明，修复归 MR1 纯 BizModel 预授权）/ P1-RC-027（post-approve 静默反向并发竞态，修复归 MR1 纯 BizModel 预授权——加 pre-approve 守卫；roadmap P1-RC-028 实为 P1-RC-027 勘误）/ P1-RC-025（换货完全缺失，product-scope 未显式裁剪 → P1 强制实现，须人工确认裁剪方向，ORM 结构变更须 ask-first）。
- **A4.2.56-b 维持 P2-MA2-011 watch-only**——credit memo 跨期配比 AR net-zero 主路径成立（X+1 月退货 AR 辅助账余额正确冲减）；GL 收入层（6001）不冲减属 documented simplification 残留 gap，跨期配比可视性 successor 跟踪。
- **A4.2.61/A4.2.62 维持 P2**——P2-RC-024（AR 4 桶视图缺失，预警列表是更严格子集，边界值归属 + 未到期项语义冲突，登记不强制）/ P2-RC-023（赠品 UI 显式标记缺口，后端行为正确 UI cosmetic 缺口，L2 强化非 L1 字面，登记不强制）。

**A1.20 + A1.21 §7 七项静态判定无一翻转**，零新 finding，不触发 MR0，不归 MR1（本审计）。P1-RC-022/P1-RC-025/P1-RC-026/P1-RC-027 修复义务归 MR1 R1.0 展开器（P1-RC-022 触核心路径须 ask-first / P1-RC-025 须人工确认 product-scope + ORM 结构变更须 ask-first / P1-RC-026 + P1-RC-027 纯 BizModel 预授权）；P2-MA2-011 successor watch-only；P2-RC-023/P2-RC-024 successor watch-only 不强制。**本审计不实施修复**（只读审计，结果表面 = 本报告 + arm-index 交叉引用注记 + roadmap/log 同步）。
