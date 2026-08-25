# ck-sales — sales 实现代码检查报告

> 工作项：C2.2。执行日期：2026-08-25。执行者：域检查子代理（read-only 检查阶段，ZCode 会话）。
> 范围：`module-sales/erp-sal-service/src/main/java` 全部手写生产代码（103 个文件、12,544 行：entity/ 24 个 BizModel 与领域服务、processor/ 59 个、posting/ 6 个、statemachine/ 12 个、support/ 3 个、spi/ 1 个、dashboard/ 1 个、根包 4 个）+ 手写 `*.xbiz`（ErpSalContract 全 5 INLINE 动作、ErpSalOrder/Receipt 委托链抽样）+ `erp-sal.data-auth.xml` / `erp-sal.action-auth.xml` + `app-service.beans.xml`。`erp-sal-web`/`erp-sal-api` 骨架未深查。**module-sales 无 `*.batch.xml`/job 声明**——但设计声明了报价过期日扫 job，D4 由此产生 1 项 finding（见 P2-CK-sal-015/023），其余 D4 维度 N/A（sales 过账失败重试依赖 finance 域 `fin/deferred-posting-sweep.batch.xml`，已跨域核对）。
> 方法：Skill: `code-quality-audit-prompt`（发现骨架 + P0-P3 分级）+ `behavioral-failure-mode-scan-prompt`（B1/B2/B3.2/B4 grep 程式）。机械扫描（D1 反模式 grep 全量）+ 核心文件逐行深读（约 40 个：Order/Invoice/Delivery/Return/Receipt/Quotation 六大 Processor 及其 per-mutation 家族各抽样、全部 posting 类、定价引擎与解析器、CreditLimitChecker、ReceiptSettler、ReturnRefundOrchestrator、Dashboard、状态机、xBiz INLINE 源码）+ 平台 API 语义实证（`FilterBeans.eq(name,null)` → IS NULL 经 nop-entropy `FilterBeanToSQLTransformer.java:126-137` 源码确认）。owner docs：`docs/design/sales/`（README/state-machine/quotation/contract/returns/use-cases）全读。

## 发现清单

> 状态初始 `open`；修复阶段逐项测试验证后回填 `ai-check-index.md`。行号以 2026-08-25 HEAD 为准。

### P1-CK-sal-001（D6/D8）ReturnRefundOrchestrator 客户级全量反转核销——不限于退货关联发票，无关联发票时也触发

- **控制点**：`app/erp/sal/service/entity/ReturnRefundOrchestrator.java#orchestrateRefund / findReceivedInvoicesOfCustomer`（L49-57、L69-77）；第二调用点 `processor/ErpSalReturnGenerateExchangeDeliveryProcessor.java#processPriceDifference`（L245：Δ<0 分支 `refundOrchestrator.orchestrateRefund(returnOrder)`）。
- **证据**：`findReceivedInvoicesOfCustomer` 仅过滤 `customerId + approveStatus=APPROVED + receivedAmount > 0`——**无任何与本次退货单 deliveryLine/invoice 链路的关联过滤**：
  ```java
  q.addFilter(and(eq("customerId", customerId),
          eq("approveStatus", ErpSalConstants.APPROVE_STATUS_APPROVED),
          gt("receivedAmount", BigDecimal.ZERO)));
  ```
  随后对其中每张发票调 `reverseSettlementsForInvoice`（该发票全部正核销行整体反向）。
- **问题**：退货审核通过后，客户名下**所有**已部分/全部核销的发票（含与退货物料完全无关的其他订单发票）的核销被整体反向——发票 `receivedAmount` 归零、`receivedStatus` 回 UNRECEIVED、收款单 `writtenOffStatus` 回退。典型触发：客户 A 有订单 O1（发票 I1 已收清）与订单 O2（未开票出库）；对 O2 退货（未开票退货场景，pre-approve 守卫 `validateInvoiceNotSettled` 因无关联发票直接放行）→ I1 的核销被错误反转，AR/收款状态与资金实收不符。换货 Δ<0 分支复用同一槽位（影响面相同）。
- **与 owner doc 的张力**：`returns.md` RC-R1.19 注记「PARTIAL/OPEN 放行（post-approve `ReturnRefundOrchestrator` 既有反向兜底**承接客户级残余**）」——短语「客户级残余」可读作对本实现的追认，但类 javadoc 自述场景为「**客户原发票**已被收款核销时」（原发票 = 退货货物的发票），实现无原发票关联判定。arm-index A2.9/A1.20 审查过该机制的核销反向载体（P2-RC-022 复用接受）但未审「作用域」这一控制点。
- **建议修复方向**：`findReceivedInvoicesOfCustomer` 增加经退货行 `deliveryLineId → ErpSalInvoiceLine.deliveryLineId` 链路限定关联发票（对齐 `ErpSalReturnProcessor.validateInvoiceNotSettled` 的既有链路解析）；换货 Δ<0 分支改为按价差金额定向退款而非复用全量槽位。**此条为本报告最不确定 finding，建议主 agent 复核 owner doc「客户级残余」语义后定级**。

### P1-CK-sal-002（D3/D6）Dashboard 订单量 KPI 查询死状态 ACTIVE——orderCount/conversionRate 恒 0

- **控制点**：`app/erp/sal/service/dashboard/ErpSalDashboardBizModel.java#countActiveOrders`（L223-228）
- **证据**：`q.addFilter(eq("docStatus", ErpSalConstants.DOC_STATUS_ACTIVE))`——`DOC_STATUS_ACTIVE`（"ACTIVE"）在 sales 全部生产代码中**零 writer**（唯一引用即此处；`state-machine.md` docStatus 注记明确「ACTIVE 为零生产 writer 的死状态，分类 = intentional legacy dead state」，4 个 DocumentStateMachine Bean 仅编码 cancel(DRAFT→CANCELLED) 单边）。
- **问题**：销售看板「本期订单量」恒为 0、「订单→开票转化率」恒为 0.0（`getDashboardKpi` L74-77）。用户可见正确性错误（UC-SAL-12 断言「KPI 卡片值 == 对应实体的实时聚合」不满足）。
- **建议修复方向**：按 `approveStatus=APPROVED + docStatus≠CANCELLED`（对齐 CreditLimitChecker outstanding 口径）+ 期间过滤计数；或推动 docStatus ACTIVE 复活（触 ORM/字典裁决，不推荐）。

### P1-CK-sal-003（D8，跨域核对归属 C3.1）延迟过账重试成功后 sales 源单 posted 标志不回写——红冲门控被跳过、暂估判定失真

- **控制点**：`app/erp/sal/service/posting/SalInvoicePostingDispatcher.java#tryPost`（L39-52，javadoc L23「由 DeferredPostingSweepJob 兜底扫描重试」）；同型 `SalReceiptPostingDispatcher`（L39-52）、`SalReturnPostingDispatcher`（L66-85）。跨域对端：`module-finance/.../ErpFinDeferredPostingRetryHelper.java#doRetry/markRetried`（L96-136——成功路径仅标记异常记录 RETRIED，**无任何 sales 源单 posted=true 回写**；grep module-finance `setPosted(true)` 零 sales 实体命中）。
- **问题**：过账失败时 sales 侧 posted=false + finance 侧 ErpFinPostingException（PENDING）；sweep 重试成功后凭证已存在但源单 posted 永久 false。后果链（sales 侧消费 posted 的全部控制点受污染）：
  1. `ErpSalInvoiceReverseApproveProcessor`/`ErpSalInvoiceCancelProcessor`（`if (posted) reverse` 门控，L41-51/L44-51）跳过红冲 → **孤儿凭证**滞留 GL；
  2. `SalReturnPostingDispatcher#isEstimatedReceivableOutstanding`（L131-138，按 `delivery.posted` 判定暂估应收未清）误判「未暂估」→ 后续退货跳过 SALES_RETURN 冲减事件 → 应收/成本冲减缺失。
- **建议修复方向**（修复阶段归 finance C3.1 协同）：retry 成功后经 billHeadCode+businessType 反查回写源单 posted=true（或派发 VoucherPostedEvent 由各域监听回写，对齐既有 `SalReversalListener` 反向模式）。sales 侧可增强：cancel/reverseApprove 改为「按凭证存在性」而非 posted 标志判红冲前置。

### P1-CK-sal-004（D5）通用 CRUD 更新/删除无单据状态守卫——「posted=true 后物理锁定」设计承诺未实现

- **控制点**：`ErpSalInvoiceBizModel` / `ErpSalReceiptBizModel` / `ErpSalDeliveryBizModel` / `ErpSalReturnBizModel` / `ErpSalOrderBizModel`（除 Order 的 ct-discount 钩子外均无 `defaultPrepareUpdate` 守卫）；行级 `ErpSalInvoiceLineBizModel` 等 5 个裸 CrudBizModel（12-19 行，零钩子）。
- **证据**：`state-machine.md` §2：「`posted=true` 后物理锁定，纠错需红冲/反审核」。但已审核（APPROVED）甚至已过账（posted=true）的发票头/行仍可经标准 GraphQL `__update/__delete/__batchModify` 直接改金额/删行——平台 CrudBizModel 默认不做状态锁定；action-auth 仅对 approve/reverseApprove 等显式动作配角色，通用 update 未受状态门控。改后发票合计与已过账凭证金额失配（GL 平衡被业务侧破坏）。
- **建议修复方向**：头/行 BizModel 补 `defaultPrepareUpdate/defaultPrepareSave/defaultPrepareDelete` 状态守卫（approveStatus=APPROVED 或 posted=true 时拒绝，或仅放行 remark 类字段）；行级至少拦截 posted 父单。

### P2-CK-sal-005（D6）促销规则 materialCategoryId 目标维度未参与行匹配——类目规则全局命中所有行

- **控制点**：`app/erp/sal/service/support/ErpSalPricingRuleEngine.java#lineMatchesRuleTarget`（L166-171）
- **证据**：
  ```java
  if (rule.getMaterialId() != null) {
      return Objects.equals(rule.getMaterialId(), line.getMaterialId());
  }
  return true;   // materialId 为空（含仅按 materialCategoryId 定向的规则）→ 匹配一切行
  ```
  `ErpSalPricingRule` 实体有 `materialCategoryId` 列（`ErpSalPricingRuleBizModel.enforceStackableAware` L75 以维度键消费），引擎完全忽略该维度。
- **问题**：按物料类目定向的折扣/改价规则（如「家电类 9 折」）对订单内**所有**行生效（食品行也打 9 折）——过度折扣 = 直接金额错误。
- **建议修复方向**：`lineMatchesRuleTarget` 补 `materialCategoryId` 经 `line.getMaterial().getCategoryId()`（对齐 `ErpSalOrderBizModel.resolveMaterialCategoryId` 既有解析）匹配。

### P2-CK-sal-006（D6）GIFT 规则触发物料不在订单时仍生成赠品行

- **控制点**：`ErpSalPricingRuleEngine.java#applyLineRule / addGiftLine`（L147-164、L203-217）
- **证据**：循环中无任何行命中时 `referenceLine == null`，但 `if (RULE_TYPE_GIFT.equals(rule.getRuleType()) && rule.getGiftMaterialId() != null) addGiftLine(rule, result, referenceLine)` 仍执行——赠品行 UoM 取 `referenceLine != null ? ... : null`（L209），null 容忍而非拒绝。
- **问题**：「买 A 赠 B」规则在订单不含 A 时仍加赠 B 行（0 元但扣库存、计入数量）——库存与成本被无谓占用。
- **建议修复方向**：`referenceLine == null` 时跳过赠品行生成（触发条件不满足）。

### P2-CK-sal-007（D6）非栈式规则「break 全链」终止——与自身 javadoc「跳过同类型后续规则」语义相悖

- **控制点**：`ErpSalPricingRuleEngine.java#evaluate`（L74-84）vs 类 javadoc（L28「stackable=false 的规则命中后**跳过同类型后续规则**；stackable=true 可叠加」）
- **证据**：
  ```java
  result.getAppliedRules().add(rule);
  if (!Boolean.TRUE.equals(rule.getStackable())) {
      break;   // 无条件终止整个规则循环（不分 ruleType）
  }
  ```
- **问题**：首个非栈式 PERCENT_DISCOUNT 命中后，后续不同类型的 GIFT/AMOUNT_OFF 规则（即使自身 stackable=true）全部被遮蔽——文档语义是「同类型排他、跨类型可叠加」。README §7 表述「支持 stackable 叠加和优先级排他」含糊，javadoc 是更明确的契约。
- **建议修复方向**：按 (ruleType) 维护「已被非栈式规则占用的类型集」实现同类型排他；或修订 javadoc/owner doc 明确全链排他语义（修复阶段对照 L1 UC-SAL-11「促销与价格清单可叠加」意图裁决）。

### P2-CK-sal-008（D6/D9）价格清单行「最优命中」无确定性排序 + 取价链全量内存过滤

- **控制点**：`app/erp/sal/service/support/ErpSalCustomerPriceResolver.java#matchLine`（L127-151）+ `findCandidatePriceLists`（L81-111）
- **证据**：`best` 取**迭代序第一条**命中行（`if (best == null) best = line;`），无 skuId 优先/阶梯优先 comparator——javadoc 声称「skuId 优先 > materialId + 数量阶梯」但代码无此排序。同清单内 sku 行（materialId 可空）与 material 行分属不同 MUTEX 维度（`ErpSalPriceListLineBizModel.enforceNoOverlap` 仅按 priceListId+materialId 互斥），二者可同时命中，取值取决于 DB 返回顺序。
- **问题**：多命中时取价不确定（sku 专属价可能被 material 通用价遮蔽，或反之）；`findCandidatePriceLists` 仅按 isActive 过滤后**全表加载**清单头内存过滤、`matchLine` 对每个候选清单**逐清单全行加载**（N+1）。数据量增长后性能与正确性双风险。
- **建议修复方向**：matchLine 内补确定性排序（skuId 命中优先，其次更窄数量阶梯/更晚 validFrom）；查询下推（partnerId/customerGroupCode/validFrom≤today 条件入 QueryBean，行按 skuId or materialId 过滤）。

### P2-CK-sal-009（D6）可用量预校验/退货 current 成本对批次粒度余额表 setLimit(1) 取单行——未按声明「聚合」

- **控制点**：`processor/ErpSalOrderProcessor.java#resolveAvailableQuantity`（L270-281）；同型 `entity/ReturnCostStrategyResolver.java#findAvgCost`（L74-86）
- **证据**：`q.addFilter(eq("materialId",..)); q.addFilter(eq("warehouseId",..)); q.setLimit(1);` 后取 `balances.get(0).getAvailableQuantity()`。而 `ErpInvStockBalance` 的唯一键粒度为 `(orgId, materialId, skuId, warehouseId, locationId, batchNo, ownerId)`（module-inventory orm L367 起 composite key）——同物料同仓库常有多行（批次/库位/SKU 拆分）。
- **问题**：① 订单级可用量预校验（`erp-sal.order-availability-check-level=WARN/HARD` 启用时）只看任一余额行的可用量：库存分散在多批次时 HARD 模式**误拒**（每批 5 件共 15 件，需求 10 → 只见 5）；javadoc/`state-machine.md` 注记均声称「聚合 availableQuantity」。② `return-cost-method=current` 时退货成本取任一批次行 avgCost——GL TOTAL_COST 与库存 ledger 同源但**源头行不确定**（不同批次 avgCost 不同）。
- **建议修复方向**：两处改为 SUM 聚合查询（QueryFieldBean.sum 或 in-memory Σ 全部命中行）；avgCost 场景按数量加权或取加权平均口径并在 owner doc 固化。

### P2-CK-sal-010（D5/D3）收款核销不拒绝已作废单据——settle/reverseSettlement 仅校验 approveStatus

- **控制点**：`app/erp/sal/service/entity/ReceiptSettler.java#settle`（L55-61 仅验收款单 approveStatus=APPROVED）+ `requireInvoiceForSettle`（L142-160 仅验存在/同客户/APPROVED）
- **证据**：cancel 处理器只设 `docStatus=CANCELLED` 不动 approveStatus（`ErpSalInvoiceCancelProcessor` L52、`ErpSalReceiptCancelProcessor` L60）；settle 全程无 docStatus 检查。
- **问题**：`state-machine.md` §4 异常路径表明确「收款核销时发票已作废 → 拒绝核销」。实现允许对 CANCELLED 发票核销、允许用 CANCELLED 收款单核销——AR 状态与作废语义冲突（作废发票的 receivedStatus 被推进）。
- **建议修复方向**：settle/reverseSettlement 双侧补 `docStatus≠CANCELLED` 守卫。

### P2-CK-sal-011（D8/D3）已审核出库单作废后订单发货进度不回滚——聚合口径未排除 CANCELLED 出库单

- **控制点**：`processor/ErpSalDeliveryProcessor.java#rollupOrderDeliveryStatus`（L270-310，`findApprovedDeliveries` L381-385 仅过滤 approveStatus=APPROVED）+ `ErpSalDeliveryCancelProcessor#cancel`（L40-54，无重 rollup）；同型 `processor/ErpSalReturnProcessor.java#aggregateApprovedDelivered`（L527-556，同样仅 approveStatus 过滤）
- **问题**：作废只改 docStatus、approveStatus 保持 APPROVED → ① cancel 后订单 deliveryStatus 停留在 DELIVERED/PARTIAL（库存已恢复但订单仍显示已发满，CreditLimitChecker outstanding 口径继续排除该订单）；② 后续任何 rollup/退货重算仍把已作废出库单数量计入 deliveredQuantity。
- **建议修复方向**：cancel（及 reverseApprove）末尾重跑 rollup；`findApprovedDeliveries`/`aggregateApprovedDelivered` 补 `docStatus≠CANCELLED` 过滤。

### P2-CK-sal-012（D8/D3）收款单反审核/作废不反向核销——发票收款状态残留 RECEIVED

- **控制点**：`processor/ErpSalReceiptReverseApproveProcessor.java#reverseApprove`（L33-51）+ `ErpSalReceiptCancelProcessor#cancel`（L38-61）——均只处理 posting reverse + 状态推进，无 `ReceiptSettler.reverseSettlement` 编排。
- **问题**：已核销收款单（invoice receivedStatus=RECEIVED/PARTIAL、receipt writtenOffStatus）被反审核/作废后核销行留存——发票显示已收款但收款单已失效；且 `ErpSalReturnProcessor.validateInvoiceNotSettled` 会因 RECEIVED 拒绝该发票的合理退货（错误封锁，需逐张手动 reverseSettlement 解锁）。
- **建议修复方向**：reverseApprove/cancel 末尾对该收款单全部 invoiceId 逐项 `receiptSettler.reverseSettlement`（对齐 ReturnRefundOrchestrator 既有模式）。

### P2-CK-sal-013（D5）withdrawApproval 无「仅提交人可操作」校验

- **控制点**：`module-common-service/.../AbstractWithdrawApprovalProcessor.java#withdrawApproval`（L20-30 仅状态校验）+ sales 六实体 withdraw 处理器全部继承无覆盖（如 `ErpSalOrderWithdrawApprovalProcessor` 全文无提交人判定）。
- **证据**：`state-machine.md` §2：「撤销提交约束：**仅提交人可操作**；审核人一旦开始审核，提交人不可再撤回」。实现无 createdBy vs currentUserId 比对（SoDGuard 仅有 approve 向）。
- **问题**：任何有权限者可撤回他人提交的单据（作废「审核人未处理」窗口的归属语义）。
- **建议修复方向**：withdraw 骨架补 `SoDGuard` 同型提交人校验（非提交人抛错）。**同型模式遍布各域（骨架在 module-common-service），跨域统一核对归 C8.2。**

### P2-CK-sal-014（D5）Contract INLINE approve 缺 SoD 守卫——合同创建人可自审

- **控制点**：`_vfs/erp/sal/model/ErpSalContract/ErpSalContract.xbiz` approve 动作 `<source>`（守卫仅 docStatus≠CANCELLED + approveStatus=SUBMITTED，无 createdBy vs svcCtx.getUserId() 比对）
- **证据**：PROC 路径 6 实体 approve 均有 `SoDGuard.assertApproverNotCreator`（如 `ErpSalOrderApproveProcessor` L36）；`state-machine.md` §6 职责分离「程序级强制：approve 守卫比对 createdBy 与审核人 userId」按域声明。INLINE 注记（§实现模式）只豁免 requireCustomer/Lines，未豁免 SoD。
- **建议修复方向**：xbiz source 前置 `if (entity.createdBy === svcCtx.getUserId()) throw ...`（对齐 P1-MA2-057 修复后的守卫族形态）。

### P2-CK-sal-015（D4）报价过期日扫 job 未实现——设计声明的 `erp-sal.quotation-expiry-check-cron` 零落地

- **控制点**：`docs/design/sales/quotation.md` §业务规则 1 + §配置点（`erp-sal.quotation-expiry-check-cron` 0 0 2 * * *、`erp-sal.quotation-auto-accept-threshold`）；实仓：`grep -r 'quotation-expiry\|auto-accept' module-sales` 零命中；module-sales 无任何 batch.xml/job bean。
- **问题**：EXPIRED 状态永不落库（仅 `ErpSalQuotationProcessor#requireNotExpired` 在 confirm/convert 时点懒拦截 L167-174）；报价列表/报表无法按过期筛选、`isAccepted` 可在过期后被继续确认（confirm 有懒拦截但状态面不可见）；auto-accept 阈值为死配置。B2 式「设计状态无 writer」。
- **建议修复方向**：补 nop-batch 日扫 job（对齐 finance `deferred-posting-sweep.batch.xml` 范式，标记过期报价派生字段或 rejection）；或 owner doc 改为懒拦截声明并删除配置点（修复阶段按需求裁决）。

### P2-CK-sal-016（D3，疑似需求分歧只登记不裁决）一次报价多次转订单被单活跃订单阻断

- **控制点**：`processor/ErpSalQuotationProcessor.java#validateNotAlreadyConverted`（L160-165）+ `ErpSalOrderBizModel#existsActiveByQuotation`（L329-344）
- **证据**：`quotation.md` §业务规则 5：「**一次报价多次采购**：ACCEPTED 报价单可按客户分批转订单（部分交货场景），报价单状态保持 ACCEPTED 直到全部转完」。实现：存在任一非 CANCELLED 关联订单即抛 `ERR_QUOTATION_ALREADY_CONVERTED`——第二次转化被拒。
- **问题**：设计承诺的分批转订单结构性不可达；且转化后 `markQuotationAccepted` 再置 isAccepted=true（L229-233）与「已 ACCEPTED 才可转」前置叠加，状态机自洽但与 L1 规则冲突。
- **建议修复方向**：修复阶段对照 L1 裁决——若多次转化是需求：改为「累计转化数量 < 报价数量」口径守卫；若单次是需求：修订 quotation.md 规则 5。

### P2-CK-sal-017（D6）换货出库单行税额公式与全域价税分离口径不一致（价外税 vs 价内税）

- **控制点**：`processor/ErpSalReturnGenerateExchangeDeliveryProcessor.java#createExchangeDelivery`（L198-208）
- **证据**：`taxAmount = amount.multiply(taxRate).divide(100)`（价外税：amount=qty×unitPrice 为不含税净额，税另加）。而全域约定（`ErpSalOrderBizModel.recomputeLineAmount` L233-234、`ErpSalCtDiscountApplier.recomputeTax` L104-105、state-machine.md §9 注记）为 `tax = net × rate/(1+rate)`（价内税：unitPrice 含税，倒挤净额）。
- **问题**：同一 unitPrice 与 taxRate 在换货单与订单/发票上产生不同税额与含税合计（换货 Δ 价差与发票口径失配；补差价发票 taxRate=0 规避了 Δ>0 侧，Δ 本身含税口径混用）。
- **建议修复方向**：统一为价税分离公式（net×rate/(1+rate)），或 owner doc 显式声明换货行价外税口径。

### P2-CK-sal-018（D6/D8，跨域核对归属 C3.1）OFFSET_ESTIMATED_RECEIVABLE 标记硬编码 TRUE——「已开票/未开票」双路径区分失效

- **控制点**：`posting/SalReturnPostingDispatcher.java#buildEvent`（L121 `billData.put(KEY_OFFSET_ESTIMATED_RECEIVABLE, Boolean.TRUE)`）vs 同文件 L46-50 javadoc「P1-RC-024 条件标记：true = 未开票且已暂估 → 冲减路径；false = 已开票 → 红字替代路径」
- **问题**：标记恒 TRUE，下游（finance SalAcctDocProvider / ArApItemGenerator）按 javadoc 的双路径区分永不进入「已开票」分支；returns.md §红字发票（场景 2：借收入/借销项税/贷应收）在 GL 层无对应凭证（仅辅助账 credit memo，P2-MA2-011 已接受 watch-only）。doc-code 漂移 + 条件语义失效。
- **建议修复方向**：按退货行→发票链路是否命中已开发票动态置值；或修订 javadoc 承认单路径。与 C3.1（finance 消费方）联合核对实际消费点。

### P2-CK-sal-019（D2/D7，跨域核对归属 C3.1）PENDING 过账异常与源单 cancel/reverseApprove 竞态——sweep 可能为已作废单生成凭证

- **控制点**：`ErpSalInvoiceCancelProcessor#cancel`（L44-51：posted=false 时跳过 reverse，**不取消 finance 侧 PENDING 的 ErpFinPostingException**）；`ErpSalInvoiceReverseApproveProcessor` 同型。跨域对端 `ErpFinDeferredPostingRetryHelper#doRetry`（重试仅查异常记录自身，不回查源单状态）+ `deferred-posting-sweep.batch.xml` loader（status=PENDING 即扫）。
- **问题**：时序 = 发票审核 → 过账失败（异常 PENDING）→ 单据作废/驳回（posted=false 无红冲）→ sweep 24h 窗口内重试成功 → **为已作废单据生成有效凭证**（AR/收入入账），且此后无人红冲（posted=false 使后续 reverse 门控全部跳过）。
- **建议修复方向**：sales cancel/reverseApprove 时联动作废对应 PENDING 异常记录（跨域 mutation 或标记）；或 finance retry 前回查源单状态。归 C3.1 联合裁决。

### P2-CK-sal-020（D5/D6）应收超期预警双阈值 AND 耦合——只配置单一阈值时永不触发

- **控制点**：`dashboard/ErpSalDashboardBizModel.java#findArOverdueAlert`（L161-200）
- **证据**：早退条件 `if (daysThreshold <= 0 && amountThreshold.signum() <= 0) return empty`（双关才关）；命中条件 `if (dayHit && amountHit)`（双开才报）。默认值 days=0/amount=ZERO（`ErpSalConstants` L107-111，整体默认关闭符合注释）。
- **问题**：运维只配置天数阈值（如 60 天）而金额阈值留默认 0 → `amountHit` 恒 false → **零告警**，与「阈值 ≤0 时不触发预警（该维度停用）」的直觉语义相反（实际语义：必须同时启用两维度）。
- **建议修复方向**：改为 OR 语义（任一启用维度命中即报，未启用维度不参与），或文档明示 AND 耦合。

### P3-CK-sal-021（D8）报价→订单转化丢失行级折扣字段

- **控制点**：`entity/QuotationToOrderConverter.java#buildLines`（L60-79——复制 material/uom/qty/price/tax/amount/amountWithTax，**不复制 discountRate/discountAmount/pricingSource**；UC-SAL-11 声明报价行具备该三列）。
- **问题**：转化后订单行金额为折后值但折扣元数据为空——折扣不可追溯、后续 recomputeLineAmount（gross−discountAmount）口径失真风险。头级合计复制保持一致，影响限于元数据与重算口径。

### P3-CK-sal-022（D8）deliveredQuantity 仅退货审核路径写入——出库审核路径仍零 writer（P2-RC-019 部分残留）

- **控制点**：`grep setDeliveredQuantity` 生产代码唯一命中 `ErpSalReturnProcessor#updateUndeliveredQuantity`（L494-521）；`ErpSalDeliveryProcessor#rollupOrderDeliveryStatus` 计算 deliveredByOrderLine 却只写头级 deliveryStatus（L309），不回写行级。
- **问题**：无退货发生时 deliveredQuantity 恒空/0（与「毛口径 Σ APPROVED 出库行」应然值不符）；当前无生产读者（仅测试读），属潜伏失同步——returns.md 注记自认「此处为首个写入口」，出库侧补写归 P2-RC-019 修复承诺范围。

### P3-CK-sal-023（D4/D8）returns.md/quotation.md 声明的配置项与特性未落地清单；sales/contract.md 实体名漂移

- **控制点**：`grep` 实证零命中：`erp-sal.return-qty-limit` / `return-period-days` / `auto-create-invoice` / `return-quality-check` / `refund-method`（returns.md §配置点 8 项仅 2 项落地：reason-required、cost-method；qty 上限由 ReturnQtyValidator 硬语义替代、质检门经 qa 域 InspectionTrigger 强制单据类型替代——语义近似可达，period-days/refund-method/auto-create-invoice 无任何替代）；`quotation-auto-accept-threshold`（并入 P2-CK-sal-015）。
- **另**：`docs/design/sales/contract.md` 声称 ORM 实体 `ErpSalSalesContract/Line/Milestone` 与 ACTIVE/COMPLETED/TERMINATED 生命周期、总量控制、里程碑开票——实仓 `ErpSalContract` 为头级骨架实体（orm L224-287 无行/无生命周期轴），完整合同域能力在 module-contract（ErpCt*，归 C7.2 检查）。doc-code 漂移登记，修复方向 = owner doc 收敛或能力归并声明。

### P3-CK-sal-024（D2）currentUserId 宽 catch 返回 null（6 处处理器同型）+ readBoolConfig 宽 catch 吞异常

- **控制点**：`ErpSalOrderProcessor#currentUserId`（L495-505）及 Invoice/Delivery/Receipt/Quotation/Return 同型拷贝；`ErpSalReturnProcessor#readBoolConfig`（L280-290 `catch (Exception e) { return defaultValue; }` 无日志）。
- **问题**：与 md pilot P3-CK-md-008 同型（不同域新登记）：`IUserContext.get()` 正常不抛，防御性 catch 但无痕迹；currentUserId=null 叠加 SoDGuard null 放行语义（SoDGuard.java L40-43 有文档化豁免）时审批人审计字段为空。建议窄化 catch 或补 log.debug。

### P3-CK-sal-025（D2）承付 commit hook 无容错——与 release hook 容错语义不对称

- **控制点**：`ErpSalOrderProcessor#runCommitmentCommitHook`（L422-436，无 try-catch，运行时异常阻断订单审核）vs `runCommitmentReleaseHook`（L443-454，try-catch 容错）与 `runIntercompanyApproveHook`（L388-401，文档化非阻塞）。
- **问题**：javadoc 声称「科目/期间/金额缺失时静默跳过（不阻塞业务流）」——缺失场景确已 null-guard 跳过，但 `budgetCommitmentBiz.commit` 运行时失败（如期间 CLOSED）会传播回滚订单审核。方向偏安全（fail-closed），但与同类 hook 语义不一致，修复阶段统一裁决。

### P3-CK-sal-026（D9）聚合计算加载全实体内存求和 / N+1

- **控制点**：① `CreditLimitChecker#sumOutstandingOrders`（L285-300，findAllByQuery 全实体逐条 toFunctional 求和，可 DB 聚合）；② `ErpSalDeliveryProcessor#rollupOrderDeliveryStatus`（L282-287 对每张已审核出库单逐单 loadLines，N+1）；③ `ErpSalDashboardBizModel#loadPostedInvoicesInRange`（L227-235 全实体加载求 Σ amountFunctional——同文件 findCustomerTopN 已改 DB 聚合并留有「消除 OOM」注记，KPI/趋势两方法未同步改造）；④ `ErpSalCustomerPriceResolver` 见 P2-CK-sal-008。数据量基线下可容忍，列为性能观察项。

### P3-CK-sal-027（D1）ErpSalConfigs 空接口死代码

- **控制点**：`app/erp/sal/service/ErpSalConfigs.java`（全文 3 行空 interface）。无引用、无内容。建议删除或并入 ErpSalConstants。

### P3-CK-sal-028（D5）settle 静默跳过非正数分配项

- **控制点**：`ReceiptSettler#settle`（L76-79 `if (amount.signum() <= 0) continue;`）；`invoiceId==null || amount==null` 同样 continue。
- **问题**：调用方传入 0/负数/null 分配不报错不告警——「提交了核销但实际未核销」无反馈（负数有专用 reverseSettlement 通道，误传正数 API 应显式拒绝）。

### P3-CK-sal-029（D6）合同量折扣回退基数取当前行价——重复应用二次折扣风险

- **控制点**：`support/ErpSalCtDiscountApplier.java#resolveBasePrice`（L88-94：合同行 unitPrice 缺失/非正时回退 `line.getUnitPrice()`——而 applyToLine L69 已把行价改写为折后价）。save 与 approve 两次应用间，若合同行价不可得，第二次以已折价再折。主路径（合同行价就绪）稳定基数不受影响。建议回退时直接返回 false（不应用）。

### P3-CK-sal-030（D8）库存移动行未透传库位/批号/序列号——DTO 支持但构造器留空

- **控制点**：`entity/ReturnStockMoveBuilder#buildLines`（L63-77，仅 material/sku/uom/qty/unitCost，未设 destLocationId/batchNo/serialNo——`StockMoveLineRequest` 三字段均存在）；`DeliveryStockMoveBuilder#buildLines`（L54-67，仅 batchNo，未设 sourceLocationId/serialNo）。
- **问题**：returns.md §与库存域协作的 `locationId`/`batchId` 追溯参数与「批次追溯三方案」仅余头级 `originReturnedMoveId`（processor L395 设置）承载；行级批次/库位追溯弱化。退货单行实体本身无批次列（载体缺失，非纯代码遗漏）——修复阶段与 inventory 域 DTO/ORM 裁决。

## 验证为正确（显式排除，防误报）

- **汇率消费语义裁决（任务指定核对项）**：sales 域**运行时零查询** `ErpMdExchangeRate` 表/`IErpMdExchangeRateBiz`（`grep -rn 'ErpMdExchangeRate\|IErpMdExchangeRateBiz\|erp_md_exchange' module-sales` 唯一命中为 `ErpSalPriceListLineBizModel.java` L49 javadoc 范式引用）。sales 各单据仅持久化自有 `exchangeRate` 列（orm defaultValue=1，7 处头表），消费点全部读取**单据自身列**（CreditLimitChecker L297/L325、三个 PostingDispatcher L78/L77/L112、QuotationToOrderConverter L48 复制、换货处理器 L179/L270）。**结论：master-data P2-CK-md-004（API 刷新写入 [today,today+1] 闭区间致同日双有效记录）不影响 sales——sales 不做日期窗口取汇率，无消费歧义面。** 残留：录入时点的汇率来源（前端是否查 md 表）属 view 层，未深查。
- **平台 API 语义实证**：`FilterBeans.eq(name, null)` 生成 `IS NULL` 而非恒 false（nop-entropy `nop-kernel/nop-core/.../FilterBeanToSQLTransformer.java` L126-137「对于空字符串和 null，op=eq/ne 的时候会转换为 is null 和 is not null」）——`ErpSalPricingRuleBizModel.enforceStackableAware`/`ErpSalPriceListBizModel.warnIfPriorityAmbiguous`/`ErpSalPriceListLineBizModel.enforceNoOverlap` 的 null 维度键查询语义正确，**非缺陷**。
- **B1 过账吞异常**：`tryPost catch(Exception)→log→false` 与 finance P1-MA2-032 同型且经 R1.16 分级裁决为「有 sweep 兜底」（finance 引擎 `ErpFinPostingProcessor.recordPostFailure` L212/L342 写 ErpFinPostingException PENDING + `deferred-posting-sweep.batch.xml` 重试 + MAX_RETRY 升级 MANUAL + 告警）——吞异常本身不重复登记；本域登记的是其**残余缺口**（P1-CK-sal-003 回写、P2-CK-sal-019 竞态）。
- **B2 dict 可达性**：12 个状态机 Bean 迁移矩阵与 owner doc 一致（DRAFT→CANCELLED 单边 + 审批轴 6 边、reverseApprove→REJECTED 合规 §16.4）；approveStatus 4 值全可达；receivedStatus/writtenOffStatus/deliveryStatus 派生态有 writer（ReceiptSettler/rollup）；docStatus ACTIVE 死状态已由 owner doc 显式裁定 intentional（dashboard 误用另登记 P1-CK-sal-002）。
- **报价→订单转化无重复落行**：orm to-many `insertable,updatable` 下 `createFromQuotation`（saveEntity 后手动 saveEntity 行循环，L320-325）经 `TestErpSalQuotationToOrder` L108「转化产物订单行数 = 报价行数」断言证实无双写。
- **促销 lineNo 无冲突**：`evaluate` 将**全部**订单行拷入 modifiedLines（L59），`nextLineNo` 对全量行取 max——赠品行行号不与未修改行冲突。
- **价税分离/最低价 RC 修复在位**：`recomputeLineAmount` L228-237 价内税公式（P1-RC-022 修复）、`validatePromotionPrices` L273-298 三级语义 + 赠品行跳过（P1-RC-021 修复）、`validateOrderAvailability` config-gated（P1-RC-020 修复）、换货链/未交货量回填/期间与已核销守卫（P1-RC-023~028 修复）均与 owner doc 注记一致，HEAD 无回退。
- **D1 平台反模式全域零命中**：`@Inject private`、`System.currentTimeMillis()/LocalDateTime.now()/new Date()`、非 NopException 业务异常、字符串 `==` 字典比较（命中均为 null 判定）全零；`dao().updateEntity()` 直写属 R1b/R2c 基线内已裁决模式（Processor/BizModel 同域 daoFor 家族，compliance-baseline 已登记豁免口径），未发现基线外新增越权写（跨域写均经 I*Biz：IErpInvStockMoveBiz/IErpMdPartnerBiz/IErpFinVoucherBiz/IErpFinArApItemBiz/IErpQaInspectionBiz/IErpCtVolumeDiscountBiz）。
- **乐观锁**：sales 全部 17 个本地实体均声明 `versionProp="version"`（orm 17 处），并发核销/并发退货经实体版本冲突兜底（ReturnQtyValidator javadoc 声明的兜底路径成立）。
- **`voucherId != null` 返回语义**：幂等命中返回 null（O-16 补偿）会使 tryPost 返回 false——该窗口由 sweep 补偿闭环覆盖，属已裁决设计。

## arm-index 复用 or 新增裁决

- 关键符号 grep `docs/audits/arm-index.md`：`orchestrateRefund`（1 命中 = A1.20 P2-RC-022「无独立退款单」——审的是**核销反向载体**，未审**作用域**）→ **P1-CK-sal-001 新增**（不同控制点）；`resolveAvailableQuantity/setLimit(1)`（3 命中均 finance/projects 域不同控制点）→ **新增**；`stackable`/`matchLine`/`existsActiveByQuotation` 零命中 → **新增**。
- **复用（不重复登记）**：过账吞异常悬挂 = P1-MA2-032/R1.16（有 sweep 兜底裁决）；CreditLimitChecker 口径、SalReversalListener 不对称（P2-MA2-058 watch-only）、INLINE 守卫族（P1-MA2-057 已修复形态在位）均维持既有结论。
- **同型跨域（登记并注明归属）**：currentUserId 宽 catch（md P3-CK-md-008 同型）；withdrawApproval 无提交人校验（common 骨架，跨域归 C8.2）；P1-CK-sal-003/P2-CK-sal-018/P2-CK-sal-019 的 finance 侧对端归 C3.1；contract.md 特性漂移归 C7.2。
- P2-RC-019（deliveredQuantity 零 writer，resolved）经 HEAD 复核为**部分残留**（退货侧写入口在位、出库侧未写）→ P3-CK-sal-022 以残留形态新登记。

## 统计

| 级别 | 数量 | 编号 |
| --- | --- | --- |
| P0 | 0 | — |
| P1 | 4 | P1-CK-sal-001/002/003/004 |
| P2 | 16 | P2-CK-sal-005..020 |
| P3 | 10 | P3-CK-sal-021..030 |

按主维度：D1×1、D2×3（024/025/019 计主 D2 之一）、D3×4（002/011/012/016）、D4×2（015/023）、D5×6（004/010/013/014/020/028）、D6×8（001/005/006/007/008/009/017/018/029 计 9 项中 008/009 双维度计主 D6）、D7×1（019 并 D2）、D8×7（003/011/012/021/022/023/030）、D9×2（008 并/026）、D10×0（未发现链式 NPE/除零/Map.get 强转类问题——除法均带 scale+RoundingMode）。

## 剩余风险（查了什么/没查什么）

- **已查**：erp-sal-service 103 个生产文件中约 40 个核心文件逐行深读（全部 posting/、support/、entity 领域服务与主 BizModel、六大 Processor、Dashboard、状态机、xBiz INLINE 源码、data-auth/action-auth），其余 per-mutation submit/reject/withdraw 家族经同型抽样 + 全量机械 grep（吞异常/反模式/状态字面量）确认同构无独立缺陷；`module-sales/model/app-erp-sales.orm.xml` 关键段（实体键/versionProp/exchangeRate 列/Contract 实体）核对；finance 侧 sweep/retry/recorder 跨域链路追踪到源码。
- **未深查**：`erp-sal-web` AMIS view.xml 与后端契约 drift（属 C8.2/低频抽查）；`erp-sal-api` 骨架；`receipt-approval/v1.xwf` 工作流定义正确性；P1-CK-sal-001 的 owner doc「客户级残余」语义终裁（需主 agent/人工）；P1-CK-sal-003/P2-CK-sal-019 的 finance 侧终裁（C3.1）；orgId 透传（D8）：`findActiveRules`/`findCandidatePriceLists`/`sumOutstandingOrders` 等 dao 直查未过滤 orgId——与「单组织基线 + data-auth 双层默认关闭」（data-auth xml 头注记）一致，多组织上线时需回归，未单列 finding；测试代码质量（57 个测试文件仅用于交叉验证行为，未按 D1-D10 独立检查）。
- **索引回填**：本报告产出后 `docs/audits/check/ai-check-index.md` 的 finding 追踪表更新归主 agent（本子代理硬约束限定仅产出本报告文件）。
