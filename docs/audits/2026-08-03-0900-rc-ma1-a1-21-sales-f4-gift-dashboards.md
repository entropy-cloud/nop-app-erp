# rc-ma1-a1-21 sales-F4 赠品与看板 需求-实现符合性审计报告

> Report Status: active
> Mission: requirement-compliance
> Work Item: A1.21（MA1 需求追踪矩阵审计 — sales-F4 赠品与看板，UC-SAL-08/12，2 UC）
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§1 五级追踪矩阵 / §2 四级分级判据 / §3 完整枚举 / §4 Q1 真相源层级 + 三判据 / §5 Q4 修复义务 + 保护区域暂停协议 / §6 9 段报告骨架 / §7 arm-index 衔接 / §8 过程纪律自检 / §9 真相源冻结 / §10 MR0/MR1 通道 / §去重协议）
> 锚点：`docs/audits/rc-requirement-baseline-inventory.md`（A1.21 UC 锚点 = UC-SAL-08/12，覆盖率 ✅ 一致）
> L1 真相源：`docs/design/sales/use-cases.md`（机制见 `docs/design/sales/state-machine.md §9` + `docs/design/dashboards.md §销售看板` — L2 设计参考，非真相源；冲突以 L1 为准）
> L5 既有证据复用：A2.9（`2026-07-28-0400-arm-ma2-sales-state-machine.md`，sales 状态机 §场景(i) 赠品库存扣减 PASS）/ O2C（`2026-07-27-1949-arm-ma2-order-to-cash-e2e.md`，O2C 链路 + SalAcctDocProvider/InvAcctDocProvider 链路 PASS，无赠品/看板 finding）/ A4.5（`2026-07-29-0430-arm-ma4-pur-sal-inv-qa-crm-code-quality.md`，sales 6 Processor + SalAcctDocProvider + 3 PostingDispatcher + DeliveryStockMoveBuilder 代码质量 PASS + P1-MA4-021 resolved R2.14）/ A2.18（`2026-07-28-1510-arm-ma2-multi-company-isolation.md`，P1-MA2-093 行级权限 dashboard 直访，resolved R1.29）

---

## 9. 与 MA2 报告差异增量声明（前置段，对应方法论 §去重协议）

本报告**不复跑 MA2 既有行为审计**，按 §去重协议只补"需求契约↔实际行为"差异：

- **复用 A2.9**（sales 状态机 PASS）：§场景(i) 赠品库存扣减（**PASS**：`applyPricingRules` 后置追加赠品行 amount=0 quantity 计入 → 经标准 delivery approve 路径扣库存 → 库存域 doConfirm→validateAvailable 守卫 → 赠品 quantity 参与可用量校验与扣减，与 owner doc §场景D 一致，`2026-07-28-0400-arm-ma2-sales-state-machine.md:180,260-264`）。赠品库存扣减的运行时行为已由 A2.9 证实，本切片不重测，只补需求视角差异（赠品行 UI 显式标记 / 折扣价税分离）。
- **复用 O2C**：销售链路 SalAcctDocProvider/InvAcctDocProvider 过账正确性 + AR 余额经辅助账回减（无赠品/看板 finding；P1-MA2-009 多币种经 R1.0 后已 resolved，本切片不重审）。
- **复用 A4.5**：sales 6 Processor + SalAcctDocProvider + 3 PostingDispatcher + DeliveryStockMoveBuilder + ReceiptSettler 代码质量 PASS；P1-MA4-021（测试有效性系统性不足）**resolved R2.14**。
- **复用 A2.18**：P1-MA2-093（11 dashboard BizModel 经 IDaoProvider 直访绕过认证管道，含 `ErpSalDashboardBizModel`）**resolved R1.29**（全局 `ErpOrgIsolationQueryTransformer` 注入覆盖）。
- **本切片只补的需求视角差异**（候选缺口 #1-#5 见 §5）：①价税分离缺失（**复用 P1-RC-022**，A1.18 已登记，UC-SAL-11⑦ 与 UC-SAL-08 同根因同控制点同修复点）/ ②赠品行 UI 显式标记缺口（ui-patterns.md:11,36,116 要求行级"赠品"开关，候选新 P2）/ ③AR 账龄 4 桶视图缺失（dashboards.md:60 要求 0-30/31-60/61-90/90+ 分桶，候选新 P2）/ ④看板行级权限（**复用 P1-MA2-093**，resolved R1.29）/ ⑤pricing 赠品场景冒烟（**复用 P2-RC-018**，A1.18 已登记端到端 7 场景仅 status==0 冒烟）。

---

## 1. 需求契约原文（2 UC 验收标准逐字引用）

> 来源：`docs/design/sales/use-cases.md`（L1 权威功能契约）；机制引用 `docs/design/sales/state-machine.md §9` + `docs/design/dashboards.md §销售看板` + `docs/design/sales/ui-patterns.md`（L2 设计参考，冲突以 L1 为准）。

### UC-SAL-08 赠品行扣库存 + 价税分离 — `use-cases.md:180-197`

**场景**：订单含赠品行(数量>0,单价=0),赠品也需扣库存;同时验证折扣的价税分离。

**可验证断言**（见 state-machine.md §9 场景 D）：
```
赠品行.单价 == 0
但 赠品行 也触发出库扣库存(可用量 -= 赠品数量)
赠品成本计入销售成本(存货估值红冲按成本,非按售价0)

// 折扣价税分离
折扣后金额 = 原金额 - 折扣额
税额 = 折扣后金额 / (1 + 税率) * 税率
不含税金额 = 折扣后金额 - 税额
```

**涉及机制**：state-machine.md §9

### UC-SAL-12 销售看板 — `use-cases.md:265-282`

**场景**：销售看板的指标展示与异常预警。见 ../dashboards.md §销售看板。

**可验证断言**：
```
// KPI 指标数据源正确(实时聚合, 非硬编码)
KPI 卡片值 == 对应实体的实时聚合(按期间/orgId/权限过滤)
  本期销售额/订单量, 应收账龄, 销售趋势, 客户TOP10

// 预警触发
预警项 == 满足阈值条件的记录(阈值来自系统配置, 非硬编码)

// 权限
看板数据受行级权限约束(只看自己组织/部门/成本中心)
```

**涉及机制**：../dashboards.md、各域 state-machine.md、roles-and-permissions.md(行级权限)

> **L2 owner doc 锚点**（设计参考，非真相源；冲突以 L1 为准）：
> - `state-machine.md §9 场景 D`：赠品行扣库存 + 折扣价税分离（"折扣影响应收金额[价税分离计算]"）
> - `dashboards.md §销售看板 :48-63`：销售看板 8 指标表（含 **:60 应收账龄 按 0-30/31-60/61-90/90+ 分组**，:61 预警 账龄>90天 且 余额>阈值）
> - `ui-patterns.md §设计原则 3 :11`：「赠品与折扣显式标记 — 赠品行（单价 0、数量非 0）必须在行上有显式标记（如标签"赠品"）」+ `§销售订单子表 :36`：「赠品行：行级别设有"赠品"开关，标记后单价锁定为 0、数量可编辑，库存扣减正常」+ `§调研参考 :116`：「赠品行标记 [Odoo#sale.order.line] 行级"赠品"开关，锁定单价为 0」

---

## 2. 实现代码路径（L3 含行号 + 跨域调用链）

> 实仓源：`module-sales/erp-sal-service/src/main/java/app/erp/sal/service/`。跨域 Facade：`IErpInvStockMoveBiz`（库存）/ `IErpFinArApItemBiz`（财务应收辅助账）/ `IErpMdPartnerBiz`（主数据往来单位）。

### UC-SAL-08 赠品行扣库存 + 价税分离

| 组件 | 文件:line | 职责 |
|------|----------|------|
| 赠品规则配置（ORM） | `module-sales/model/app-erp-sales.orm.xml:1125-1127`（`ErpSalPricingRule.giftMaterialId:1125` / `giftSkuId:1126` / `giftQuantity:1127` [domain=quantity, precision=20/scale=4]）| 促销规则配置赠品物料/SKU/数量 |
| 赠品引擎（纯函数式） | `support/ErpSalPricingRuleEngine.java:203-217` addGiftLine（`:206 new ErpSalOrderLine()` 纯函数式引擎，:204-205 Javadoc 明示"调用方负责持久化，不经 daoProvider.newEntity()，与 FunnelAggregationEngine 内存聚合同模式"；`:210 giftLine.setUnitPrice(BigDecimal.ZERO)` / `:211 giftLine.setQuantity(rule.getGiftQuantity()?:BigDecimal.ONE)` / `:212 giftLine.setAmount(BigDecimal.ZERO)` / `:213 giftLine.setPricingSource(PRICING_SOURCE_PROMOTION)` / `:214 giftLine.setRemark("赠品行")` / `:215 modifiedLines.add(giftLine)` / `:216 giftRuleIds.add(rule.getId())`）| 赠品行评估快照（unitPrice=0 / quantity=giftQuantity / amount=0 / pricingSource=PROMOTION / remark="赠品行"）|
| 赠品行持久化 | `entity/ErpSalOrderBizModel.applyPricingRules:96-114` → `persistPricingResult:143-160`（将 EvaluationResult.modifiedLines 含赠品行回写订单）| 赠品行落库 |
| ErpSalOrderLine ORM | `module-sales/model/app-erp-sales.orm.xml:396-408`（`unitPrice:396` mandatory / `taxAmount:399 defaultValue=0` / `amount` / `discountAmount:407 defaultValue=0` / `pricingSource:408 VARCHAR(50)`）— **无 `isGift`/`lineType` 列**（grep `isGift\|lineType` 全 `module-sales/` 生产代码 0 命中）| 赠品识别为隐式（pricingSource=PROMOTION + remark="赠品行"），**UI 层缺口**（ui-patterns 要求行级"赠品"开关） |
| 出库移动单构造 | `entity/DeliveryStockMoveBuilder.java:54-67` buildLines（`:56 for (ErpSalDeliveryLine line : lines)` 遍历**全部** delivery 行扣库存，无赠品过滤；`:61 req.setQuantity(line.getQuantity())`；`:62` Javadoc 明示「出库 unitCost 由库存域按移动加权平均 avgCost 快照（售价 unitPrice ≠ 存货成本，不得传入）」；`:63 req.setBatchNo(line.getBatchNo())`）| 赠品数量计入可用量校验与扣减（无赠品过滤）|
| 出库扣库存（跨域 Facade）| `processor/ErpSalDeliveryProcessor.java:241-245` triggerOutgoingMove（`:242 loadLines` → `:243 stockMoveBuilder.build(delivery, lines, context)` → `:244 stockMoveBiz.generateMove(request, context)`）| 跨域库存扣减（含 validateAvailable） |
| 赠品成本 → 销售成本（库存域 SALES_OUTPUT） | `module-inventory/erp-inv-service/.../posting/InvPostingDispatcher.java:181-221` buildEvent（`:183 loadLedgers` → `:184-196 Σ ledger.totalCost.abs()=totalCost` 按 avgCost 快照，**不传 unitPrice** → `:212 billData.put("TOTAL_COST", totalCost)`）+ `module-inventory/erp-inv-service/.../posting/InvAcctDocProvider.java:81-85`（SALES_OUTPUT 分支 `else` → `:82 fact(SUBJECT_COGS, "主营业务成本", DC_DEBIT, total)` + `:84 fact(SUBJECT_INVENTORY, "库存商品", DC_CREDIT, total)`）| 赠品成本按 avgCost 入 6401 主营业务成本 / 1401 库存商品（满足 L1 「存货估值红冲按成本，非按售价 0」）|
| 销售发票过账（AR 侧） | `posting/SalAcctDocProvider.java:73-93` createFacts（AR_INVOICE 分支 `:76-82` 仅用头合计 `KEY_TOTAL_AMOUNT:77 / KEY_TOTAL_TAX_AMOUNT:78 / KEY_TOTAL_AMOUNT_WITH_TAX:79` → Dr 1131 / Cr 6001 / Cr 2221）| 赠品行 unitPrice=0/amount=0 对收入/销项税贡献 0（正确——赠品免费）|
| 折扣价税分离（**缺口 #1**） | `entity/ErpSalOrderBizModel.java:172-179` recomputeLineAmount（`:175 gross = unitPrice×qty` + `:176 discountAmt = line.getDiscountAmount()` + `:177 net = gross − discountAmt` + `:178 line.setAmount(net.setScale(4, HALF_UP))`，**仅 setAmount 无 setTaxAmount**）+ `:181-197` recomputeOrderTotals（`:187 totalTaxAmount += nullSafe(line.getTaxAmount())` 复用**促销前陈旧** line.taxAmount 求和，从不重算）| **价税分离公式未实现** → 复用 P1-RC-022（A1.18 UC-SAL-11⑦ 同根因同控制点）|

### UC-SAL-12 销售看板

| 组件 | 文件:line | 职责 |
|------|----------|------|
| KPI 聚合 | `dashboard/ErpSalDashboardBizModel.java:61-92` getDashboardKpi（`:70 loadPostedInvoicesInRange(from, to)` 经 `dao.findAllByQuery` posted=true + 期内；`:71-74 Σ invoice.amountFunctional = salesAmount`；`:76 countActiveOrders = count(docStatus=ACTIVE)`；`:77 invoiceCount`；`:78 conversionRate = invoiceCount / orderCount`；`:80 arBalance = sumArApOpen(RECEIVABLE)` 跨域 `IErpFinArApItemBiz.findOpenItems(RECEIVABLE, OPEN+PARTIAL)` 实时聚合）| KPI 卡片实时聚合非硬编码（salesAmount/orderCount/invoiceCount/conversionRate/arBalance）|
| 销售趋势 | `:94-120` getDashboardTrend（`:101 loadPostedInvoicesInRange(from, today)` → `:102-108 amountByMonth LinkedHashMap` 按 `invoice.businessDate` 年月分组 Σ amountFunctional）| 月销售额序列（默认近 12 月）|
| 客户 TOP-N | `:122-164` findCustomerTopN（`:130-135 DB 级 GROUP BY customerId + SUM(amountFunctional) WHERE posted=true` → `:145-146 reverseOrder` 内存排序 → `:148 stream.limit(topN)` → `:149-161` partnerName 补全）| TOP-N 客户（默认 10）|
| 应收超期预警 | `:170-209` findArOverdueAlert（`:172-177 daysThreshold/amountThreshold = AppConfig.var(...)` 配置驱动；`:178-180 阈值 ≤0 时返回 emptyList 默认禁用`；`:182-183 arApItemBiz.findOpenItems(RECEIVABLE)` 跨域读；`:186-188 age = ChronoUnit.DAYS.between(dueDate/businessDate, today)`；`:190-193 dayHit && amountHit`；`:194-205 row = {partnerId, partnerName, sourceBillCode, openAmount, ageDays}`）| 超期应收预警列表（含 partnerName/sourceBillCode/openAmount/ageDays）|
| 配置键 | `ErpSalConstants.java:80-84`（`CONFIG_DASH_SAL_AR_OVERDUE_DAYS = "erp-dash.sal-ar-overdue-days" :80` + `DEFAULT_DASH_SAL_AR_OVERDUE_DAYS = 0 :81` + `CONFIG_DASH_SAL_AR_OVERDUE_AMOUNT = "erp-dash.sal-ar-overdue-amount" :83` + `DEFAULT_DASH_SAL_AR_OVERDUE_AMOUNT = BigDecimal.ZERO :84`）| 阈值配置化（默认 0/ZERO = 禁用）；与 mfg 看板 P2-RC-009（阈值未配置化）形成**正面对比** |
| AMIS 菜单接线 | `module-sales/erp-sal-web/src/main/resources/_vfs/erp/sal/auth/erp-sal.action-auth.xml:98-107`（`sal-dashboard` 分组 + `sal-dashboard-main app:useCases="UC-SAL-12"` component="AMIS" url="/erp/sal/pages/dashboard/main.page.yaml"）| 菜单↔UC 引用，看板入口 |
| AMIS 页面接线 | `module-sales/erp-sal-web/src/main/resources/_vfs/erp/sal/pages/dashboard/main.page.yaml`（filterForm + reload `:23-27`；KPI `kpiService:29-77` GraphQL `getDashboardKpi:36`；趋势 `trendChart:79-107` GraphQL `getDashboardTrend:87`；TOP-N `topNCustomerChart:109-140` GraphQL `findCustomerTopN:117`；超期预警 `arOverdueCrud:142-168` GraphQL `findArOverdueAlert:154`）| AMIS 接线完整（KPI/趋势/TOP-N/超期预警四区）|
| 行级权限（**复用 P1-MA2-093**） | `ErpSalDashboardBizModel` 用 `IServiceContext context` 参数但查询为 raw QueryBean over IDaoProvider（:55）/ IOrmTemplate（:57）/ `daoProvider.daoFor(...).findAllByQuery(q)`（:213-219）+ `ormTemplate.findListByQuery(q)`（:136），无显式 orgId 注入；R1.29 全局 `ErpOrgIsolationQueryTransformer` 已 resolved 覆盖直访路径 | 行级权限经全局 QueryTransformer 守卫，复用 P1-MA2-093 |

---

## 3. 测试断言证据（L4 注明断言强度）

> 测试源：`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/`。E2E：`tests/e2e/dashboards/sales.value.spec.ts` + `sales.smoke.spec.ts`。强度评级对齐 MA5（A5.6 E2E effectiveness）。

### UC-SAL-08 赠品行扣库存 + 价税分离

| 测试文件#方法 | 覆盖断言 | 断言强度 | 证据摘要 |
|--------------|---------|---------|---------|
| `TestErpSalPricingRuleEngine#testGiftLine:91-110`（单测） | 赠品行评估快照 | **强** | assertEquals(2, modifiedLines.size()) + gift.unitPrice=ZERO + gift.quantity=ONE + gift.pricingSource="PROMOTION" + giftRuleIds.contains(rule.id) |
| `TestErpSalPricingEndToEnd#testScenario4_Gift:89-101`（端到端） | 赠品行端到端应用 | **仅冒烟** | 仅 assertEquals(0, result.getStatus())，不断言 modifiedLines 行数/赠品行 unitPrice=0/quantity/pricingSource/合计重算 |
| 赠品扣库存隔离断言 | 赠品 quantity 计入可用量扣减 | **行为 PASS**（A2.9 §场景(i) 间接覆盖）| 无独立 JUnit 断言；MA2 A2.9:260-264 行为 PASS（赠品 quantity 经标准 delivery approve 路径扣库存）|
| 赠品成本按 avgCost 入 6401 | SALES_OUTPUT Dr 6401 / Cr 1401 | **行为 PASS**（A4.5/O2C 间接覆盖）| `InvAcctDocProvider:81-85` 按 totalCost=Σ ledger.totalCost 入 6401/1401；O2C e2e 链路证实 SALES_OUTPUT 凭证结构；无"赠品行 unitPrice=0 → 6401 仍按 avgCost 入账"的独立断言 |
| 价税分离重算（**缺口 #1**） | 折扣后金额/(1+税率)×税率 | **零测试**（功能缺失导致无可测路径）| `recomputeLineAmount:172-179` 不重算 taxAmount，下游 P1-RC-022 |

### UC-SAL-12 销售看板

| 测试文件#方法 | 覆盖断言 | 断言强度 | 证据摘要 |
|--------------|---------|---------|---------|
| `TestErpSalDashboard#testKpiEmptyDatasetReturnsZeros:54-61` | KPI 空数据集 | **强** | salesAmount=ZERO + orderCount=0L + conversionRate=0.0 + arBalance=ZERO |
| `TestErpSalDashboard#testKpiAggregationAndConversionRate:63-88` | KPI 聚合 + 转化率 | **强** | 2 已过票 100+200=300 + 4 ACTIVE 订单（转化率=2/4=0.5）+ 未过票不计入 + arBalance=500（RECEIVABLE+OPEN）|
| `TestErpSalDashboard#testTrendMonthlySeries:90-104` | 月销售额序列 | **强** | 2 月合计 150+250=400 |
| `TestErpSalDashboard#testCustomerTopN:106-120` | TOP-N 客户 | **强** | 2 客户累计 350>100 → TOP1 = 621L (350) |
| `TestErpSalDashboard#testArOverdueAlertDisabledByDefault:122-136` | 默认禁用 | **强** | 默认 daysThreshold/amountThreshold=0/ZERO → emptyList（不触发）|
| `TestErpSalDashboard#testArOverdueAlertTriggers:138-160` | 配置触发 | **强** | daysThreshold=90 + amountThreshold=500 + dueDate 100 天前 + openAmount=800 → 1 预警（partnerId=641L）|
| E2E `dashboards/sales.value.spec.ts:1-9` | KPI 数值断言 | **强** | `assertDashboardKpiValues({ expected: { salesAmount: 1000, orderCount: 1, invoiceCount: 1 } })` 严格相等 |
| E2E `dashboards/sales.smoke.spec.ts` | KPI 冒烟 | **仅冒烟**（弱）| P1-MA5-012 → R3.2 closed 已补 value 层（见上）|
| AR 账龄 4 桶视图（**缺口 #3**） | 0-30/31-60/61-90/90+ 分桶 | **零测试**（实现缺失导致无可测路径）| `findArOverdueAlert` 仅返回扁平 ageDays 列表，无 4 桶分桶；下游 P2-RC-024 |
| 行级权限（**缺口 #4**） | orgId/部门/成本中心过滤 | **零测试**（`enableActionAuth=FALSE` 全程关闭认证）| 复用 P1-MA2-093（resolved R1.29），A2.18:99-101 dashboard 直访登记覆盖 |

**测试缺口汇总**：
- 价税分离重算（零测试，下游 P1-RC-022 reuse）
- AR 账龄 4 桶视图（零测试，下游 P2-RC-024 新建）
- 赠品扣库存 / 赠品成本入 6401 独立断言（无独立 JUnit，行为经 A2.9 §场景(i) PASS + A4.5 SALES_OUTPUT 凭证结构 PASS 间接证实）

---

## 4. 运行时行为证据（L5 — 复用 MA2 + E2E）

### UC-SAL-08 赠品行扣库存 + 价税分离

| 行为 | 证据来源 | 结论 |
|------|---------|------|
| 赠品规则配置（giftMaterialId/giftSkuId/giftQuantity）| `app-erp-sales.orm.xml:1125-1127` | **已实现**（ORM 列存在 + `ErpSalPricingRule` 可配置）|
| 赠品行评估快照（unitPrice=0/quantity=giftQuantity/amount=0/pricingSource=PROMOTION/remark="赠品行"）| `ErpSalPricingRuleEngine.addGiftLine:203-217` + `TestErpSalPricingRuleEngine#testGiftLine:91-110` 行级断言 | **行为已证实**（单测层强覆盖）|
| 赠品行经 applyPricingRules 后置追加到 modifiedLines | A2.9 §场景(i) PASS（`:263 applyPricingRules 经 ErpSalPricingRuleEngine 追加赠品行 + 重算订单头合计`）| **行为已证实**（MA2 报告）|
| 赠品出库扣库存（赠品 quantity 参与可用量校验与扣减）| A2.9 §场景(i) PASS（`:264 Delivery.approve → triggerOutgoingMove → 库存域 doConfirm→validateAvailable：赠品 quantity 参与可用量校验与扣减，owner doc §场景D 一致，无遗漏`）+ `DeliveryStockMoveBuilder.buildLines:54-67` 全行扣减无赠品过滤 + `ErpSalDeliveryProcessor.triggerOutgoingMove:241-245` | **行为已证实**（MA2 报告 + 代码阅读）|
| 赠品成本按 avgCost 入销售成本（6401 借 / 1401 贷，非按售价 0）| O2C §2.4（SALES_OUTPUT 链路）+ `InvPostingDispatcher.buildEvent:181-221`（totalCost=Σ ledger.totalCost.abs() 按 avgCost 快照）+ `InvAcctDocProvider.createFacts:81-85`（SALES_OUTPUT Dr 6401 / Cr 1401 按 total）+ A4.5 SALES_OUTPUT 凭证结构 PASS | **行为已证实**（赠品 unitPrice=0 不传入库存域，库存域按 avgCost 入账 → 赠品成本按成本入销售成本，满足 L1 「存货估值红冲按成本，非按售价 0」）|
| 赠品行对 AR 收入/销项税贡献 0 | `SalAcctDocProvider.createFacts:73-93`（AR_INVOICE 分支 :77-79 仅用头合计 KEY_TOTAL_AMOUNT/TOTAL_TAX_AMOUNT/TOTAL_AMOUNT_WITH_TAX；赠品 amount=0 → 不参与 amount 求和；taxAmount 默认 0 → 不参与 tax 求和）| **行为已证实**（赠品免费——对收入/销项税贡献 0，符合"赠品免费"语义）|
| **价税分离重算（缺口 #1）** | `ErpSalOrderBizModel.recomputeLineAmount:172-179` 仅 setAmount 无 setTaxAmount；`recomputeOrderTotals:181-197` 复用促销前陈旧 taxAmount | **行为缺失**（促销后 taxAmount 沿用旧值致销项税+应收高估；GL 平衡不破坏故非 P0④）→ 复用 P1-RC-022 |
| **赠品行 UI 显式标记（缺口 #2）** | grep `isGift\|lineType` 全 `module-sales/` 生产代码 0 命中；`ErpSalOrderLine` ORM（:396-408）无 isGift/lineType 列；ui-patterns.md:11,36,116 要求行级"赠品"开关 | **行为缺失（UI 层）**（后端扣库存行为不受影响——赠品经 pricingSource=PROMOTION + remark="赠品行" 隐式识别；UI 缺行级显式标记开关）|

### UC-SAL-12 销售看板

| 行为 | 证据来源 | 结论 |
|------|---------|------|
| KPI 实时聚合（非硬编码）| `getDashboardKpi:61-92` 经 `ErpSalInvoice`(posted)+`ErpSalOrder`(ACTIVE)+`IErpFinArApItemBiz`(RECEIVABLE OPEN/PARTIAL) 实时聚合；`TestErpSalDashboard#testKpiAggregationAndConversionRate:63-88` 强断言（salesAmount=300/orderCount=4/conversionRate=0.5/arBalance=500）+ E2E `sales.value.spec.ts` 严格相等（salesAmount=1000/orderCount=1/invoiceCount=1）| **行为已证实**（实时聚合，非硬编码）|
| 销售趋势（月序列）| `getDashboardTrend:94-120` + `testTrendMonthlySeries:90-104` 强断言（2 月 150+250=400）| **行为已证实** |
| 客户 TOP-N | `findCustomerTopN:122-164` DB 级 GROUP BY + `testCustomerTopN:106-120` 强断言（TOP1=621L 350）| **行为已证实** |
| 应收超期预警（阈值配置化）| `findArOverdueAlert:170-209` 用 `AppConfig.var(...)`（`ErpSalConstants:80-84`）；`testArOverdueAlertDisabledByDefault:122-136`（默认禁用）+ `testArOverdueAlertTriggers:138-160`（90/500 配置→1 预警）双路径强断言 | **行为已证实**（阈值来自系统配置非硬编码，与 mfg P2-RC-009 形成正面对比）|
| 应收账龄 4 桶视图（**缺口 #3**）| `findArOverdueAlert:170-209` 仅返回扁平 `ageDays` 列表（`:204 row.put("ageDays", age)`），无 0-30/31-60/61-90/90+ 分桶；`dashboards.md:60` 要求 4 桶结构化视图（预警卡片类型）；`main.page.yaml:142-168` arOverdueCrud 4 列表（partnerName/sourceBillCode/openAmount/ageDays）无分桶 | **行为缺失**（预警列表是更严格子集，但缺失结构化账龄视图）|
| 行级权限（**复用 P1-MA2-093**）| A2.18 `:99-101` 11 dashboard BizModel 显式列 `ErpSalDashboardBizModel` 为直访绕过认证管道之一；R1.29 全局 `ErpOrgIsolationQueryTransformer` resolved | **行为已修复（R1.29）**（行级权限经全局 QueryTransformer 守卫；A2.18 + R1.29 复核确认覆盖直访路径）|

---

## 5. 符合性结论（五级追踪矩阵 + 每 UC 结论）

### 5.1 五级追踪矩阵（2 UC × 5 列，逐 UC 一行）

| UC | L1 use-case 需求契约 | L2 owner doc 契约 | L3 代码路径 | L4 测试断言 | L5 运行时行为 | 符合性结论 |
|----|---------------------|------------------|------------|------------|--------------|-----------|
| **UC-SAL-08** 赠品行扣库存 + 价税分离 | `use-cases.md:180-197`（3 条赠品断言：单价=0/赠品扣库存/赠品成本按成本入销售成本[非售价0] + 3 条价税分离公式：折扣后金额=原金额−折扣额/税额=折扣后金额/(1+税率)×税率/不含税金额=折扣后金额−税额）— 验收标准原文见 §1 | `state-machine.md §9 场景 D`（"折扣影响应收金额[价税分离计算]"，仅 generic 概述未定义重算位置——L2 漏述）+ `ui-patterns.md:11,36,116`（**设计参考；行级"赠品"开关要求**与 L1 字面"赠品行"语义强化一致，但 L1 未显式要求显式 UI 标记列——L2 强化属设计参考非真相源）| 赠品规则：`app-erp-sales.orm.xml:1125-1127`（giftMaterialId/giftSkuId/giftQuantity）；赠品引擎：`ErpSalPricingRuleEngine.addGiftLine:203-217`（unitPrice=0/quantity=giftQuantity/amount=0/pricingSource=PROMOTION/remark="赠品行"）；赠品持久化：`ErpSalOrderBizModel.applyPricingRules:96-114`+`persistPricingResult:143-160`；ErpSalOrderLine ORM `:396-408`（**无 isGift/lineType 列**）；出库扣库存：`DeliveryStockMoveBuilder.buildLines:54-67`（全行扣减无赠品过滤）+ `ErpSalDeliveryProcessor.triggerOutgoingMove:241-245` → 跨域 `IErpInvStockMoveBiz.generateMove`；赠品成本：跨域 `InvPostingDispatcher.buildEvent:181-221`（totalCost=Σ ledger.totalCost）+ `InvAcctDocProvider.createFacts:81-85`（SALES_OUTPUT Dr 6401/Cr 1401）；发票过账：`SalAcctDocProvider.createFacts:73-93`（AR_INVOICE 头合计）；**价税分离缺失**：`recomputeLineAmount:172-179`（仅 setAmount 无 setTaxAmount）+ `recomputeOrderTotals:181-197`（复用陈旧 taxAmount）| `TestErpSalPricingRuleEngine#testGiftLine:91-110`（**强**：2 lines + gift.unitPrice=ZERO + gift.quantity=ONE + pricingSource=PROMOTION）+ `TestErpSalPricingEndToEnd#testScenario4_Gift:89-101`（**仅冒烟**：仅 status==0）；价税分离重算（零测试）；赠品扣库存/成本入 6401 无独立断言（行为经 A2.9+A4.5 间接证实）| 赠品扣库存+成本 → A2.9 §场景(i) PASS（行为已证实）；赠品成本按 avgCost 入 6401 → A4.5 SALES_OUTPUT PASS；赠品对 AR 收入/销项税贡献 0 → 代码阅读证实；**价税分离缺失**（行为缺失）；**UI 显式标记缺失**（行为缺失[UI 层]）| **接受 on 赠品扣库存+成本**（行为正确，§2 接受）+ **P1 on 价税分离**（**复用 P1-RC-022**，§2 P1① 功能完全缺失——价税分离公式未实现，影响 AR/销项税金额准确性；UC-SAL-08:190-193 与 UC-SAL-11⑦ 同根因同控制点同修复点，按 §7 复用）+ **P2 on UI 显式标记**（**新建 P2-RC-023**，§2 P2① 后端行为正确，UI 层 cosmetic 缺口——L2 强化要求非 L1 字面要求）|
| **UC-SAL-12** 销售看板 | `use-cases.md:265-282`（3 条断言：KPI 实时聚合非硬编码[本期销售额/订单量/应收账龄/销售趋势/客户TOP10]/预警阈值配置化/行级权限约束）— 验收标准原文见 §1 | `dashboards.md §销售看板 :48-63`（8 指标表含 **:60 应收账龄 按 0-30/31-60/61-90/90+ 分组** + :61 预警 账龄>90天且余额>阈值）+ `§设计原则 :9-13`（指标不硬编码 + 行级权限）+ `§实现约定 :236-243`（GraphQL 聚合 + orgId/部门/成本中心过滤 + 阈值放 NopSysVariable）+ `roles-and-permissions.md`（行级权限，设计参考）| KPI：`ErpSalDashboardBizModel.getDashboardKpi:61-92`（实时聚合）；趋势：`:94-120`；TOP-N：`:122-164`；超期预警：`:170-209`（`AppConfig.var` 配置驱动）；配置键：`ErpSalConstants:80-84`（DASH_SAL_AR_OVERDUE_DAYS/AMOUNT + 默认 0/ZERO）；AMIS 菜单 `erp-sal.action-auth.xml:98-107`（useCases="UC-SAL-12"）+ 页面 `main.page.yaml`（KPI :36/趋势 :87/TOP-N :117/超期 CRUD :154）；行级权限经 R1.29 全局 `ErpOrgIsolationQueryTransformer`（A2.18 :99-101 显式列 dashboard）；**AR 4 桶缺失**：`findArOverdueAlert:170-209` 仅扁平 ageDays | `TestErpSalDashboard` 6 方法全部**强**（testKpiEmptyDatasetReturnsZeros/testKpiAggregationAndConversionRate 断言 salesAmount=300/orderCount=4/conversionRate=0.5/arBalance=500/testTrendMonthlySeries/testCustomerTopN/testArOverdueAlertDisabledByDefault/testArOverdueAlertTriggers 90/500 配置→1 预警）+ E2E `sales.value.spec.ts`（salesAmount=1000/orderCount=1/invoiceCount=1 严格相等）；AR 4 桶（零测试）；行级权限（零测试，enableActionAuth=FALSE）| KPI 实时聚合 → 行为已证实；阈值配置化 → 行为已证实（与 mfg P2-RC-009 正面对比）；**AR 4 桶视图缺失**（行为缺失——预警列表是更严格子集）；行级权限 → R1.29 resolved | **接受 on KPI/趋势/TOP-N/阈值**（§2 接受——KPI 实时聚合 + 阈值配置化强断言 PASS）+ **P2 on AR 账龄 4 桶**（**新建 P2-RC-024**，§2 P2① 次要验收标准未完全满足——预警列表是更严格子集，缺失结构化账龄视图）+ **P1 on 行级权限**（**复用 P1-MA2-093**，resolved R1.29，§2 P1① 功能未落地→已修复；A2.18 :99-101 显式列 dashboard，按 §7 复用不新建）|

### 5.2 候选缺口分级汇总（5 项）

| 缺口# | UC | 描述 | 分级 | 命中判据 | finding ID |
|-------|----|------|------|---------|-----------|
| #1 | UC-SAL-08 | 折扣价税分离缺失（`recomputeLineAmount:172-179` 仅 setAmount 无 setTaxAmount；促销后 taxAmount 沿用旧值致销项税+应收高估，但 GL 平衡不破坏故非 P0④）| **P1** | §2 P1①（功能完全缺失——价税分离公式未实现）| **复用 P1-RC-022**（A1.18 UC-SAL-11⑦ 同根因同控制点同修复点，§7 复用）|
| #2 | UC-SAL-08 | 赠品行 UI 显式标记缺失（无 `isGift`/`lineType` 列；ui-patterns.md:11,36,116 要求行级"赠品"开关；后端扣库存行为不受影响——赠品经 pricingSource=PROMOTION+remark 隐式识别）| **P2 watch-only** | §2 P2①（次要验收标准未完全满足——后端行为正确，UI 层 cosmetic 缺口）+ 注：L2 强化要求（ui-patterns）非 L1 字面要求 | **新建 P2-RC-023** |
| #3 | UC-SAL-12 | AR 账龄 4 桶视图缺失（`findArOverdueAlert` 仅返回扁平 ageDays 列表；dashboards.md:60 要求 0-30/31-60/61-90/90+ 分桶结构化视图）| **P2 watch-only** | §2 P2①（次要验收标准未完全满足——预警列表是更严格子集[已实现]，缺失结构化账龄视图）| **新建 P2-RC-024** |
| #4 | UC-SAL-12 | 看板行级权限（`ErpSalDashboardBizModel` 用 `IServiceContext context` 但查询为 raw QueryBean over IDaoProvider/IOrmTemplate，无显式 orgId 注入；R1.29 全局 `ErpOrgIsolationQueryTransformer` resolved）| **P1（resolved）** | §2 P1①（功能未落地 → R1.29 已修复）| **复用 P1-MA2-093**（A2.18 :99-101 显式列 dashboard，R1.29 resolved；§7 复用不新建）|
| #5 | UC-SAL-08 | pricing 赠品场景冒烟（`TestErpSalPricingEndToEnd#testScenario4_Gift:89-101` 仅 status==0 冒烟；单测层 `TestErpSalPricingRuleEngine#testGiftLine` 强覆盖）| **P2 watch-only** | §2 P2①（次要验收标准[端到端断言强度]未完全满足——主路径[单测层强覆盖+端到端冒烟通过]OK 边界[端到端断言强度]弱）| **复用 P2-RC-018**（A1.18 已登记 7 场景含赠品仅冒烟，§7 复用）|

### 5.3 每 UC 总结论

- **UC-SAL-08 赠品行扣库存 + 价税分离**：
  - **接受 on 赠品扣库存 + 赠品成本**（行为正确，A2.9 §场景(i) PASS + A4.5 SALES_OUTPUT PASS + 代码阅读证实）
  - **P1 on 价税分离**（**复用 P1-RC-022**，§2 P1①；UC-SAL-08:190-193 验收标准语言与 UC-SAL-11⑦ 完全一致——"价税分离"同根因[recomputeLineAmount 不重算 taxAmount]同控制点[recomputeLineAmount:172-179 + recomputeOrderTotals:181-197]同修复点，§7 复用）
  - **P2 on UI 显式标记**（**新建 P2-RC-023**，§2 P2① 后端行为正确，UI 层 cosmetic 缺口；L2 强化要求[ui-patterns 行级"赠品"开关]非 L1 字面要求，倾向接受 watch-only）

- **UC-SAL-12 销售看板**：
  - **接受 on KPI 实时聚合 + 阈值配置化 + 趋势 + TOP-N**（§2 接受——`TestErpSalDashboard` 6 方法强断言 + E2E `sales.value.spec.ts` 严格相等；阈值配置化与 mfg P2-RC-009 形成正面对比）
  - **P2 on AR 账龄 4 桶视图**（**新建 P2-RC-024**，§2 P2① 次要验收标准未完全满足——预警列表是更严格子集，缺失结构化账龄视图）
  - **P1（resolved）on 行级权限**（**复用 P1-MA2-093**，resolved R1.29；A2.18 :99-101 显式列 dashboard，§7 复用不新建）

---

## 6. 与 arm-index 衔接（复用 or 新增 裁决）

### 6.1 arm-index grep 比对 + 复用 or 新增裁决（§7 规则）

> 对每条候选缺口 grep arm-index 同域同控制点后裁决（禁止未经比对新建）。

| 缺口# | grep 关键词 | 既有 finding | 裁决 |
|-------|------------|-------------|------|
| #1 | 「价税分离」「tax separation」「recomputeLineAmount」「taxAmount recompute」「P1-RC-022」 | `P1-RC-022`（A1.18，UC-SAL-11 ⑦ 价税分离缺失，`recomputeLineAmount:172-179` 仅 setAmount 无 setTaxAmount）| **复用 P1-RC-022**（追加 RC A1.21 交叉引用注记，不新建编号；UC-SAL-08:190-193 与 UC-SAL-11⑦ 同根因同控制点同修复点——同一 `recomputeLineAmount:172-179` + `recomputeOrderTotals:181-197` 站点）|
| #2 | 「赠品行」「gift line」「isGift」「lineType」「行级赠品开关」「UI 标记」 | 无 sales 域同控制点 finding（grep arm-index sales 赠品/看板同域同控制点零命中；P2-RC-018 覆盖端到端断言强度不同控制点；P1-RC-022 覆盖价税分离不同控制点）| **新建 P2-RC-023** |
| #3 | 「AR 账龄」「aging bucket」「4 桶」「0-30」「90+」「aging-bucket」「ar aging」 | 无 sales 域同控制点 finding（grep arm-index 「账龄」「aging」「0-30」零命中 sales 看板侧；finance 域 `ar-ap-reconciliation.md §账龄分级 :189-195` 5 级账龄表与 dashboards.md:60 4 桶要求是不同视图——前者为辅助账坏账准备计提，后者为看板预警卡片结构化展示；P2-RC-008 finance CLOSED 期间门控不同控制点）| **新建 P2-RC-024** |
| #4 | 「行级权限」「dashboard 直访」「orgId」「P1-MA2-093」 | `P1-MA2-093`（A2.18，orgId 查询隔离全仓未落地，`:99-101` 显式列 `ErpSalDashboardBizModel` 为 11 dashboard 直访之一；**resolved R1.29**）| **复用 P1-MA2-093**（追加 RC A1.21 交叉引用注记，不新建；R1.29 全局 `ErpOrgIsolationQueryTransformer` resolved 覆盖直访路径；与 A1.7 UC-FIN-17 ⑫ + A1.11 UC-MFG-11 ③ 行级权限复用先例一致）|
| #5 | 「赠品冒烟」「TestErpSalPricingEndToEnd」「P2-RC-018」「7 场景」 | `P2-RC-018`（A1.18，UC-SAL-11 ④⑤⑥⑦ 价格端到端仅 status==0 冒烟，含 testScenario4_Gift）| **复用 P2-RC-018**（追加 RC A1.21 交叉引用注记，不新建编号；testScenario4_Gift 在 P2-RC-018 范围内已登记）|

### 6.2 双向可追溯（finding ↔ 修复行预留 MR0/MR1）

| Finding ID | 域 | UC | 分级 | 目标 MR | 触及保护区域 | 修复状态 |
|-----------|---|----|------|--------|------------|---------|
| `P1-RC-022`（reuse #1）| sales | UC-SAL-08 + UC-SAL-11 ⑦ | P1 | MR1（R1.0 → RC-R1.n）| 否（纯 BizModel 代码逻辑修复——`ErpSalOrderBizModel.recomputeLineAmount:172-179` 在 setAmount 后按 L1 公式重算 taxAmount + `recomputeOrderTotals:181-197` 沿用重算后 taxAmount 汇总；按 roadmap 预授权类目[代码逻辑修复]可自动执行，**不触发 §5 ask-first**）| todo（追加 RC A1.21 交叉引用注记；MR1 修复行与 A1.18 合并，无须重复）|
| `P2-RC-023` | sales | UC-SAL-08 | P2 | successor watch-only（P2 登记不强制）| **是 — ORM 结构变更**（`ErpSalOrderLine` 增 `isGift`/`lineType` 列 + view.xml 增行级"赠品"开关 UI 锁定单价为 0；**触及 ORM 结构变更须 ask-first + 独立 plan-audit §5**；或方案 B owner doc `ui-patterns.md` 补注「赠品行 UI 显式标记当前为隐式[pricingSource=PROMOTION + remark="赠品行"]，后端扣库存行为不受影响；显式 UI 标记属 successor 触发条件[产品化深度部署时]」纯文档修复可自动执行）| todo |
| `P2-RC-024` | sales | UC-SAL-12 | P2 | successor watch-only（P2 登记不强制）| 否（纯 BizModel 代码逻辑 + AMIS 页面——`ErpSalDashboardBizModel` 增 `getArAgingBuckets` 方法返回 {bucket_0_30, bucket_31_60, bucket_61_90, bucket_90_plus, Σ openAmount} 结构化视图 + `main.page.yaml` arOverdueCrud 上方增 4 桶预警卡片；按 roadmap 预授权类目[代码逻辑修复]可自动执行，**不触发 §5 ask-first**；与 finance `ar-ap-reconciliation.md §账龄分析 :185-222` 5 级账龄报表是不同视图，可复用同一 openItems 查询）| todo |
| `P1-MA2-093`（reuse #4）| 全 19 域 | UC-SAL-12 + UC-FIN-17 ⑫ + UC-MFG-11 ③ | P1 | MR1（R1.0 → RC-R1.n）| 否（架构级补能力——全局 `IQueryTransformer` 注入 + `IUserContext.getOrgId()` 扩展；按 roadmap 预授权类目可自动执行，不触发 §5 ask-first）| **✅ resolved**（R1.29 done）|
| `P2-RC-018`（reuse #5）| sales | UC-SAL-08 + UC-SAL-11 ④⑤⑥⑦ | P2 | successor watch-only（P2 登记不强制）| 否（纯测试补充——补强 `TestErpSalPricingEndToEnd` 7 场景的行级断言；按 roadmap 预授权类目可自动执行，不触发 §5 ask-first）| todo（追加 RC A1.21 交叉引用注记；MR1 测试补强时自动激活）|

### 6.3 阈值配置化正面对比（sales vs mfg）

> sales 看板阈值 config 化（`AppConfig.var(...)` + `ErpSalConstants:80-84`）与 mfg 看板 P2-RC-009（阈值未配置化）形成正面对比，正面证实 sales UC-SAL-12 "阈值来自系统配置，非硬编码" 验收标准：

| 域 | 实现 | 验收 | 结论 |
|---|------|------|------|
| **sales**（UC-SAL-12）| `findArOverdueAlert:172-177` `AppConfig.var(CONFIG_DASH_SAL_AR_OVERDUE_DAYS, DEFAULT=0)` + `AppConfig.var(CONFIG_DASH_SAL_AR_OVERDUE_AMOUNT, DEFAULT=ZERO)` + `:178-180 阈值≤0 返回 emptyList 默认禁用` | `use-cases.md:276` 「阈值来自系统配置, 非硬编码」 | **接受**（`TestErpSalDashboard#testArOverdueAlertDisabledByDefault + testArOverdueAlertTriggers` 双路径强断言）|
| **mfg**（UC-MFG-11，A1.11）| `findDelayedWorkOrderAlert:147-173` 用 `plannedEndDate.isBefore(today)`（today 动态但非 config threshold）+ 齐套不足预警 = `stockPartialCount`（状态计数，无 config threshold）+ mfg dashboard 无 `erp-mfg.dashboard-*-threshold` config key | `use-cases.md:206` 「阈值来自系统配置, 非硬编码」 | **P2-RC-009**（阈值 derivation 为状态/日期驱动而非 config 键；与 sales 形成对比偏差）|

---

## 7. 静态存疑点清单（供 MA4 展开）

> L5 无法静态定论、需运行时确认的点。每存疑点一行；MA4（A4.1/A4.2 展开器）运行时验证后回填结论。

- **SP-1**（#1 reuse P1-RC-022）：折扣价税分离在多档税率混合单据（订单行 13%/9%/6% 混合）+ 多档促销叠加（行级 PERCENT_DISCOUNT + 头级 AMOUNT_OFF）场景下，`recomputeLineAmount:172-179` 不重算 taxAmount + `recomputeOrderTotals:181-197` 复用陈旧 taxAmount 求和后的实际税额偏差范围（影响 AR 销项税 + 应收金额准确性的数值量化）。
- **SP-2**（UC-SAL-08 赠品成本）：赠品行在不同库存策略（FIFO/MOVING_AVERAGE/STANDARD/SPECIFIC）下，`DeliveryStockMoveBuilder.buildLines:54-67` 不传 unitCost + 库存域 `InvPostingDispatcher.buildEvent:181-221` 按 `Σ ledger.totalCost.abs()` 入账后，**多物料混合出库（含赠品）**场景下，totalCost 求和是否正确包含赠品 quantity 的 avgCost（即赠品成本是否被正确计入销售成本而非被 abs() 折叠丢失）—— 需运行时构造"1 普通物料 + 1 赠品物料"出库场景断言 6401 借方金额 = Σ 普通成本 + 赠品成本。
- **SP-3**（#4 reuse P1-MA2-093）：看板 orgId 行级权限在 `ErpOrgIsolationQueryTransformer`（R1.29 全局 IQueryTransformer）实际生效性 —— `ErpSalDashboardBizModel` 经 `daoProvider.daoFor(...).findAllByQuery(q)` + `ormTemplate.findListByQuery(q)` 直访路径下，全局 QueryTransformer 是否实际注入 orgId 过滤（vs CrudBizModel 标准管道）需运行时确认（复用 A1.7 SP-4 / A1.11 SP-3 / A2.18 successor 同根因）。
- **SP-4**（#3 P2-RC-024）：AR 账龄 4 桶视图在多组织 + 多客户场景下的数据完整性 —— 若实现 4 桶分桶（`bucket_0_30/31_60/61_90/90_plus`），是否存在跨桶归类歧义（如账龄 = 30/60/90 边界值归属）+ 0-30 桶是否包含未到期项（dashboards.md:60 "0-30 天"是"账龄"还是"到期后账龄"语义需运行时确认——`findArOverdueAlert:186-188 age = ChronoUnit.DAYS.between(dueDate/businessDate, today)`，age<0 时置 0，意味着未到期项也归 0-30 桶；与 dashboards.md:60 「应收账龄」语义可能冲突）。
- **SP-5**（#2 P2-RC-023）：赠品行 UI 显式标记缺口在产品化部署场景下的实际影响 —— 若客户/业务员需在订单录入时显式区分赠品行（用于销售分析/赠品成本归集/合规审计），当前隐式标记（pricingSource=PROMOTION + remark="赠品行"）是否足够；若不足，行级"赠品"开关落地后单价锁定为 0 + 数量可编辑 + 库存扣减正常的产品化路径需确认（ui-patterns.md:36 已有设计意图，ORM `ErpSalOrderLine` 加 `isGift` boolean 列 + view.xml 行级开关）。

**P0 即时通道**：本切片**未触发 P0**（最高级 = P1[价税分离，复用 P1-RC-022]，无活跃数据破坏 / 会计过账破坏 / 安全漏洞 / 核心循环断裂；价税分离虽影响 AR/销项税准确性，但 GL 平衡不破坏故非 §2 P0④；行级权限虽属安全维度，但 P1-MA2-093 已 resolved R1.29；其余为 P2 watch-only cosmetic/可用性类）。

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

  全 16 规则 actual ≤ baseline（精确匹配，0 漂移；与 A1.20 报告基线一致——本切片仅追加文档无生产代码变更）。
- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告全部 finding 已按 §7 规则 grep arm-index 同域同控制点后给出"复用 or 新增"裁决（详见 §6.1 比对表），无未经比对直接新建的 finding。P2-RC-023（赠品 UI 标记）+ P2-RC-024（AR 4 桶）经 grep 确认为新控制点；P1-RC-022（价税分离）/ P1-MA2-093（行级权限）/ P2-RC-018（pricing 冒烟）经 grep 确认同根因同控制点 → 复用。

---

## 落盘完整性自检（§6 9 段完整性）

报告产出 agent 在落盘前自查 9 段全部存在：

- [x] §1 需求契约原文（2 UC 验收标准逐字引用）
- [x] §2 实现代码路径（含行号 + 跨域调用链）
- [x] §3 测试断言证据（注明强度）
- [x] §4 运行时行为证据（复用 MA2 + E2E）
- [x] §5 符合性结论（2 UC × 5 列矩阵 + 候选缺口 5 项分级 + 每 UC 总结论）
- [x] §6 与 arm-index 衔接（grep 比对 + 复用/新增裁决 + 双向可追溯 + 阈值配置化正面对比）
- [x] §7 静态存疑点清单（5 项 SP-1..SP-5 + P0 即时通道未触发声明）
- [x] §8 过程纪律自检（checker actual vs baseline 实测表 + closure-audit 独立性 + 交叉去重）
- [x] §9 与 MA2 报告差异增量声明（前置段 — 复用 A2.9/O2C/A4.5/A2.18 + 只补需求视角差异）

9 段齐全，落盘。

---

## 附录：A1.21 切片裁决摘要

- **新 P2（2 项）**：P2-RC-023（UC-SAL-08 赠品行 UI 显式标记缺口，cosmetic/可用性类）/ P2-RC-024（UC-SAL-12 AR 账龄 4 桶视图缺失，预警列表是更严格子集但缺失结构化账龄视图）
- **复用 P1（2 项）**：P1-RC-022（UC-SAL-08 价税分离缺失，追加 RC A1.21 交叉引用注记，MR1 修复与 A1.18 合并）/ P1-MA2-093（UC-SAL-12 行级权限，resolved R1.29，追加 RC A1.21 交叉引用注记）
- **复用 P2（1 项）**：P2-RC-018（UC-SAL-08 pricing 赠品场景冒烟，追加 RC A1.21 交叉引用注记，与 A1.18 范围合并）
- **P0 即时通道**：未触发（本切片无 P0）
- **静态存疑点**：5 项（SP-1..SP-5）交 MA4 A4.1/A4.2 运行时展开
- **正面对比**：sales UC-SAL-12 阈值 config 化 PASS（与 mfg P2-RC-009 形成正面对比，证实 sales 看板阈值配置化实现质量更高）
- **行为接受面**：UC-SAL-08 赠品扣库存 + 赠品成本按 avgCost 入 6401（A2.9 §场景(i) PASS + A4.5 SALES_OUTPUT PASS）；UC-SAL-12 KPI 实时聚合 + 趋势 + TOP-N + 超期预警配置化（`TestErpSalDashboard` 6 方法强 + E2E `sales.value.spec.ts` 严格相等）
