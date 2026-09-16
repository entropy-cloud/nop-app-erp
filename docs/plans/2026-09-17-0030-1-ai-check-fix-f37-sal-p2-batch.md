# 2026-09-17-0030-1 ai-check r1 修复批次 F3.7：sales 域 16 条 P2（sal-005..020）

> Plan Status: completed
> Last Reviewed: 2026-09-17
> Source: docs/audits/check/ai-check-index.md（P2-CK-sal-005..020 open 行）+ docs/audits/check/ck-sales.md §Finding
> Related: docs/plans/2026-09-16-1230-1-ai-check-fix-f36-pur-p2-batch.md（同族前批，pur-004/005/015-r3 同型范式）；plan 2026-08-08-2219-2（P1-RC-024 暂估冲减，sal-018 关联）
> Audit: required

## Current Baseline

- HEAD `e6e0a3a78`，工作树干净。F3.6 批已完成（purchase 域 6 条 P2 + 20 文件 LOG 字段修复）。
- 全部 16 条 finding 的代码锚点已在 HEAD 实测核验（ck-sales.md L46-163 描述与现码一致）：
  - **sal-005/006/007**（ErpSalPricingRuleEngine）：`lineMatchesRuleTarget`（L169-174）仅匹配 materialId、materialCategoryId 完全忽略；GIFT 行在 `referenceLine==null` 时仍生成（L160-162 + addGiftLine L204-217 null 容忍）；非栈式 `break` 终止全链（L79-81）与类 javadoc L28「跳过同类型后续规则」相悖。类目解析既有范式：`ErpSalOrderBizModel.resolveMaterialCategoryId`（L305-308，`line.getMaterial().getCategoryId()`）。
  - **sal-008**（ErpSalCustomerPriceResolver）：`matchLine`（L127-151）取迭代序首条命中、无 comparator；`findCandidatePriceLists`（L81-111）全量加载内存过滤 + 逐清单全行加载。
  - **sal-009**：`ErpSalOrderProcessor.resolveAvailableQuantity`（L270-281）与 `ReturnCostStrategyResolver.findAvgCost`（L74-86）对批次粒度余额表 `setLimit(1)` 取单行。
  - **sal-010**（ReceiptSettler）：`settle`（L56-61）仅验收款单 approveStatus；`requireInvoiceForSettle`（L141-160）仅验存在/同客户/APPROVED；全程无 docStatus 检查。cancel 只设 docStatus 不动 approveStatus。ErpSalErrors 尚无 SETTLE_*_CANCELLED 码（pur 侧 M2.5 已加，sal 侧为 r3 M2.5 移交通道）。
  - **sal-011**：`ErpSalDeliveryProcessor.findApprovedDeliveries`（L381-385）仅滤 approveStatus；`ErpSalDeliveryCancelProcessor.cancel`（L40-54）无重 rollup；`ErpSalReturnProcessor.aggregateApprovedDelivered`（L527-556）同型仅 approveStatus 过滤。pur-004 同构修复范式可平移（管道查后内存剔除 + isEffective 门控 + cancel/reverseApprove 后置 rollup）。
  - **sal-012**：`ErpSalReceiptReverseApproveProcessor.reverseApprove`（L33-51）与 `ErpSalReceiptCancelProcessor.cancel`（L38-61）只处理 posting reverse + 状态推进，无 ReceiptSettler.reverseSettlement 编排。ReceiptSettler 已有 `reverseSettlement(receipt, invoiceId)`（L117-）与 `findLines`（L217-）。
  - **sal-013**：`module-common-service AbstractWithdrawApprovalProcessor.withdrawApproval`（L20-30）仅状态校验，无提交人比对；SoDGuard 仅 `assertApproverNotCreator`（L32）。跨域共用骨架。
  - **sal-014**：`ErpSalContract.xbiz` approve `<source>` 守卫仅 docStatus≠CANCELLED + approveStatus=SUBMITTED，无 createdBy vs svcCtx.getUserId() 比对（xbiz 内 `svcCtx.getUserId()` 表达式有 ast/cs 先例）。
  - **sal-015**：`quotation.md §业务规则 1`「validTo 到期后系统自动标记 EXPIRED（nop-job 每日扫描）」+ §配置点 `erp-sal.quotation-expiry-check-cron`（默认 0 0 2 * * *）；实仓零落地（module-sales 无 batch.xml/job bean）。EXPIRED 为声明终态（lifecycle 图：APPROVED → 报价过期 → EXPIRED）。job bean + job.yaml + batch.xml 范式齐备（ErpApsAutoDispatchJob + erp-aps-auto-dispatch.job.yaml + deferred-posting-sweep.batch.xml；F3.2 后 cron 键约定 `nop.job.<name>.cron-expr`）。
  - **sal-016**：`ErpSalQuotationProcessor.validateNotAlreadyConverted`（L160-165）存在性守卫 vs `quotation.md §业务规则 5`「ACCEPTED 报价单可按客户分批转订单……直到全部转完」——L1 owner doc 明确多次转化是需求（实现相悖，非需求分歧；审计「对照 L1 裁决」即以 L1 为准）。ErpSalOrder 头有 quotationId 弱指针（`existsActiveByQuotation` L329-344 内存剔除范式在案）；order line 无 quotationId 列。
  - **sal-017**：`ErpSalReturnGenerateExchangeDeliveryProcessor.createExchangeDelivery`（L198-208）`taxAmount = amount×rate/100`（价外税）vs 全域价内税 `tax = net×rate/(1+rate)`（`ErpSalOrderBizModel` L228-240 recomputeLineAmount、P1-RC-022 公式）。
  - **sal-018**：`SalReturnPostingDispatcher.buildEvent` L122 `KEY_OFFSET_ESTIMATED_RECEIVABLE=TRUE` 恒值；**全仓 grep 实测该键零消费**（finance 侧无任何读取点，SALES_RETURN 在 ErpFinArApItemGenerator 为单路径 credit memo L172-175）——「已开票→红字发票」路径整体未实现（P2-MA2-011 已接受 watch-only）。ErpSalReturnLine 无 invoiceId 列，动态置值缺乏可靠判定链。
  - **sal-019**：sales invoice cancel（L44-51）/reverseApprove 不处理 finance 侧 PENDING `ErpFinPostingException`；`ErpFinDeferredPostingRetryHelper.doRetry`（L101-121）从 eventData 重建事件重放，不回查源单。**依赖方向：finance 不依赖 sales（sales→finance 单向），finance 侧回查源单不可行；sales 侧联动作废通道可行**——finance 已有 `ignore` mutation（status→IGNORED，sweep 仅扫 PENDING）。
  - **sal-020**：`ErpSalDashboardBizModel.findArOverdueAlert`（L161-200）早退双关才关（L174-176）+ 命中 `dayHit && amountHit`（L192）双开才报；单阈值配置永不触发。
- 相邻类 HEAD 形态注记（审查 iteration 1 补充）：`ReturnRefundOrchestrator` 现码已含 P1-CK-sal-001 修复（`findReceivedInvoicesOfCustomer` 按 `relatedInvoiceIds` scoping、不滤 docStatus）——是 Phase 4 B1 自锁分析的输入前提。
- xmeta 约束：docStatus 仅 eq/in → 内存剔除范式（F3.6 已复用两次）。
- 测试基线：module-sales 全绿（F3.6 批实测 316/0/0）。

## Goals

- 修复 sal-005..015、017、019、020 共 15 条 + sal-018 doc-code 漂移修正（下述 Decision），回填索引/roadmap，终态 fixed。
- sal-016 按 L1 owner doc（quotation.md 规则 5）实现「累计转化数量」守卫，分批转订单可达。

## Non-Goals

- 「已开票→红字发票」GL 路径实现（跨域大特性，P2-MA2-011 watch-only 在案）；sal-018 仅修 doc-code 漂移。
- `erp-sal.quotation-auto-accept-threshold` 自动转订单特性实现（Deferred，语义需运营裁决——accept 时自动转单失败如何回滚需产品决策）。
- sal-013 的跨域 C8.2 全域盘点（本批只修共用骨架一点，全域逐域核对归 C8.2）。
- fin-006 行自身的独立修复批（本批 sal-019 建立的联动作废通道即其「源单有效性校验通道」的 sales 侧实现；fin-006 行归 finance 批复核裁决）。

## Task Route

- Type: `implementation-only change`
- Owner Docs: docs/design/sales/quotation.md（规则 1/5 + 配置点）、docs/design/sales/returns.md（核销/红字）、docs/design/sales/state-machine.md（§2 撤销提交约束/§4 异常路径/§6 职责分离）、docs/design/sales/use-cases.md（UC-SAL-11）
- Skill Selection Basis: 实现阶段无匹配可复用技能；审查阶段 plan-audit / closure-audit。`Skill: none`（代码阶段）

## Infrastructure And Config Prereqs

- 无基础设施变更（EXPIRED 状态载体与报价过期日扫 job 归 Deferred，见 Deferred But Adjudicated；sal-015 以 owner doc 如实修订关闭）。

## Execution Plan

### Phase 1 — 促销引擎三缺陷（sal-005/006/007，Fix-heavy）

Status: completed
Targets: ErpSalPricingRuleEngine.java
Skill: none

- Item Types: `Fix`

- [x] sal-005：`lineMatchesRuleTarget` 补 materialCategoryId 匹配——rule.materialCategoryId 非空时经 `line.getMaterial().getCategoryId()` 比对（对齐 resolveMaterialCategoryId 既有解析；material 为 null 时该维度不命中）；materialId 与 categoryId 同时非空时须同时命中
- [x] sal-006：`applyLineRule` 中 `referenceLine == null`（无行命中）时跳过 GIFT 赠品行生成（触发条件不满足）
- [x] sal-007：非栈式语义改为「同类型排他、跨类型可叠加」——维护已占用类型集（`Set<String> exclusiveTypes`），命中非栈式规则后加入其 ruleType，循环内 `exclusiveTypes.contains(rule.getRuleType())` 跳过后续同类型规则（含栈式），替换无条件 break；Decision：javadoc L28 语义为更明确契约且与 UC-SAL-11「促销与价格清单可叠加」意图一致，owner doc 不改
- [x] sal-007 附带裁决：被排他跳过的规则**不**写入 appliedRules（未应用不记录；现状循环对每条 processed 规则无条件 add 的行为仅保留于实际应用者）
- [x] Proof：扩展/新增促销引擎测试——类目规则命中同类目行、异类目行不打折；无触发物料的 GIFT 不生成赠品行；非栈式 PERCENT 命中后**后续 PERCENT 被跳过、后续 GIFT/AMOUNT_OFF/PRICE_OVERRIDE 仍生效**（异类型可叠加），非栈式 GIFT 命中后后续 GIFT 被跳过；被跳过规则不入 appliedRules（红→绿）

Exit Criteria:

- [x] 三缺陷修复 + 测试红→绿 + 既有促销引擎测试零回归

### Phase 2 — 价格清单确定性排序（sal-008，Fix | Decision）

Status: completed
Targets: ErpSalCustomerPriceResolver.java
Skill: none

- Item Types: `Fix | Decision`

- [x] Decision：`matchLine` 补确定性 comparator——skuId 专属命中 > materialId 通用命中 > 更窄数量阶梯（minQty 更大）> validFrom 更晚 > id 兜底（实体无 lineNo 列，实测后以 id 字符串序兜底）；javadoc 声称的优先序据此落地（QueryBean 无排序 comparator 表达力，内存排序即确定）
- [x] 查询下推（审计方向部分采纳）：`matchLine` 行查询技术上可下推（FilterBeans 有 or 组合、eq(name,null)→IS NULL 已实证），本批保持全行加载 + priceListId 单清单范围；`findCandidatePriceLists` 头查询保持 isActive 下推 + 内存期间/币种过滤（nullable 空端开放语义）。以确定性排序为主修复，下推登记 residual（数据量/性能权衡，非技术不可行）
- [x] Proof：测试——同清单 sku 行与 material 行并存时 sku 优先；同维度多行取更窄阶梯；红→绿

Exit Criteria:

- [x] 多命中取价确定且符合 javadoc 声称优先序；测试红→绿

### Phase 3 — 余额聚合口径（sal-009，Fix）

Status: completed
Targets: ErpSalOrderProcessor.java、ReturnCostStrategyResolver.java
Skill: none

- Item Types: `Fix`

- [x] `resolveAvailableQuantity`：去 setLimit(1)，全命中行 Σ availableQuantity（对齐声明「聚合 availableQuantity」）
- [x] `findAvgCost`：去 setLimit(1)，按数量加权平均 avgCost（Σ(avgCost×totalQty)/Σ(totalQty)，零量保护回退任一非空 avgCost）；Decision：加权口径（对齐库存估值语义），owner doc state-machine.md §退货成本注记补一句加权声明
- [x] Proof：测试——同物料同仓两批次行（5+10）HARD 模式需求 10 不再误拒；退货成本两批次加权值断言；红→绿

Exit Criteria:

- [x] 两处聚合口径落地；测试红→绿

### Phase 4 — 收款核销守卫 + 反审核/作废反向核销（sal-010 + sal-012，Fix）

Status: completed
Targets: ReceiptSettler.java、ErpSalErrors.java、ErpSalReceiptCancelProcessor.java、ErpSalReceiptReverseApproveProcessor.java
Skill: none

- Item Types: `Fix | Add`

- [x] sal-010：新码 `ERR_SETTLE_INVOICE_CANCELLED`/`ERR_SETTLE_RECEIPT_CANCELLED`（镜像 pur-015-r3 M2.5 语义，中文描述）；**仅 settle 新方向**加守卫——`settle` 入口收款单 docStatus≠CANCELLED + `requireInvoiceForSettle` 发票 docStatus≠CANCELLED。
  - Decision（偏离审计「reverseSettlement 双侧同守卫」字面）：reverse 方向是**存量核销清理通道**，加发票侧守卫将自锁——CANCELLED 发票的核销行仍存（cancel 不动 approveStatus/核销行），`ReturnRefundOrchestrator.reverseSettlementsForInvoice`（findReceivedInvoicesOfCustomer 不滤 docStatus，P1-CK-sal-001 修复后 scoping 形态）与本 Phase 的 `reverseAllSettlements` 两条链都必须能反向已作废发票的存量核销；双侧守卫会使收款单永不可作废、退货审核被 CANCELLED 发票阻断。备选「双侧守卫 + 存量清理豁免分支」弃（同一动作两种守卫语义更易错）。
- [x] sal-012：ReceiptSettler 新增 `reverseAllSettlements(receipt)`——按 receiptId 查全部 ReceiptLine 的去重 invoiceId 逐项 reverseSettlement（净额回 0 幂等）；receipt cancel/reverseApprove 在状态推进前调用（对齐 ReturnRefundOrchestrator 既有自动反向模式；与 pur-005 拒绝式守卫分域异构——审计两 finding 各自明确方向，如实分立）
- [x] Proof：测试——CANCELLED 发票/收款单 settle 拒绝；已核销收款单 cancel/reverseApprove 后发票 receivedStatus 回 UNPAID、二次 cancel 幂等；红→绿

Exit Criteria:

- [x] 双守卫 + 自动反向核销落地；测试红→绿；既有收款测试零回归

### Phase 5 — 出库作废聚合口径（sal-011，Fix）

Status: completed
Targets: ErpSalDeliveryProcessor.java、ErpSalDeliveryCancelProcessor.java、ErpSalDeliveryReverseApproveProcessor.java（实测存在）、ErpSalReturnProcessor.java
Skill: none

- Item Types: `Fix`

- [x] `findApprovedDeliveries` 与 `aggregateApprovedDelivered` 管道查后内存剔除 CANCELLED（F3.6 pur-004 平移范式）
- [x] `rollupOrderDeliveryStatus` 当前单自身行 `isDeliveryEffective` 门控（APPROVED 且未作废）；Delivery cancel/reverseApprove 末尾后置调 rollup
- [x] Proof：测试——已审核出库单作废后订单 deliveryStatus 回落；作废后聚合不再计入（CreditLimitChecker 口径随 rollup 修正）；红→绿

Exit Criteria:

- [x] 聚合 + 重算点落地；测试红→绿

### Phase 6 — 撤回提交人守卫（sal-013，Fix | Add，跨域共用骨架）

Status: completed
Targets: module-common-service AbstractWithdrawApprovalProcessor.java、SoDGuard.java（如需新断言）、common 错误码
Skill: none

- Item Types: `Fix | Add`

- [x] 骨架 `withdrawApproval` 在 validateTransitionForWithdraw 后补提交人校验：`createdBy` 与当前 userId 均非空且不等 → 抛错（SoDGuard 新增 `assertWithdrawerIsCreator`，与 approve 向一致尊重 `erp-common.sod-enabled` 总开关；null 容忍：系统/批处理上下文与直接种子实体不阻断）；新公共错误码（module-common-service 错误码宿主，中文描述）。**破坏面预估**：骨架遍布全部审批域（assets/finance/hr/mfg 等十余处 withdraw 测试），全 reactor 测试回随批修复或如实登记
- [x] Proof：测试（module-common-service 或 sal 侧）——非提交人撤回拒绝、提交人/null createdBy 放行；红→绿；全 reactor 测试暴露的其他域回随批修复或如实登记
- [x] Decision：骨架一点修全域（审计方向）；全域 C8.2 逐域盘点 Non-Goal

Exit Criteria:

- [x] 骨架守卫落地；新测试红→绿；受影响域测试零回归（全 reactor 证明）

### Phase 7 — Contract INLINE approve SoD（sal-014，Fix）

Status: completed
Targets: ErpSalContract.xbiz（src/main/resources 版本）
Skill: none

- Item Types: `Fix`

- [x] Decision（B5 门控）：INLINE 守卫须经 Java helper 复用 `SoDGuard` 同一 `erp-common.sod-enabled` 开关（%test profile 关闭以容纳 admin 单账号范式）——新 helper bean `ErpSalInlineSodGuard`（sal-service，`assertApproverNotCreator(createdBy, ctx)` 委托 SoDGuard，全仓首例 INLINE SoD，无脚本内读 AppConfig 先例）；xbiz approve `<source>` 状态校验前 `inject('erpSalInlineSodGuard').assertApproverNotCreator(entity.createdBy, svcCtx)`（命中抛 `erp.err.sal.approver-is-creator`，已注册码）；备选「脚本内无条件比对」弃（破坏 %test E2E 范式与单账号部署）
- [x] Proof：测试——创建人 approve 自审被拒（sod-enabled=true 上下文）、他人 approve 放行、开关关闭时放行回归断言；红→绿

Exit Criteria:

- [x] INLINE 守卫落地；测试红→绿

### Phase 8 — 报价懒拦截语义入 owner doc + 分批转订单（sal-015 + sal-016，Fix | Decision）

Status: completed
Targets: docs/design/sales/quotation.md、ErpSalQuotationProcessor.java、ErpSalOrderBizModel.java + IErpSalOrderBiz.java
Skill: none

- Item Types: `Decision | Fix`

- [x] sal-015 Decision（审计选项 c，三选一裁决）：**维持懒拦截并修订 quotation.md**——EXPIRED 落库载体实测不存在（quotation.approveStatus 绑定平台字典 `wf/approve-status`，无 erp-sal 报价状态字典/列；grep EXPIRED 生产代码零命中；对比 ct 域能写 EXPIRED 因自有 `erp-ct/contract-status` 字典），批量日扫需 ORM 模型变更前置（dual-agent 保护区），不在本批。备选 a（ORM 新增字典/列）Deferred 带 successor；备选 b（写未注册字典值）弃（平台校验风险）。quotation.md 修订：规则 1 改「报价过期经 confirm/convert 时点懒拦截（`requireNotExpired`）实现；EXPIRED 终态与批量日扫未实现，登记 Deferred」；配置点表 `erp-sal.quotation-expiry-check-cron` 行标注「未实现（Deferred，以 EXPIRED 状态载体落地为前置）」
- [x] sal-016 Decision（B3 操作数落实）：报价总数量 = Σ 报价行 quantity；已转数量 = Σ 关联非 CANCELLED 订单行 quantity（头 `quotationId` 管道查 + 内存剔除 CANCELLED 范式 + 逐单 `loadLines` 求和；订单行无 quotationLineId，头级 Σ 为唯一可行口径）——混合 UoM（个+箱）时 Σ 为近似口径，登记 residual（reference ERP 简化，行级映射需模型变更归 Deferred 同前）
- [x] sal-016 实现：ErpSalOrderBizModel + IErpSalOrderBiz 新增 `sumConvertedQuantityByQuotation(quotationId, ctx)`；`validateNotAlreadyConverted` 改「已转数量 > 0 且 ≥ 报价总数量 → 拒绝」（复用 ERR_QUOTATION_ALREADY_CONVERTED；分批转订单可达，规则 5 落地；既有 testConvertIdempotentRejected 行为兼容——首转后已转=总量拒绝、取消后已转归零放行）
- [x] Proof：转化测试——首转放行、转满再转拒绝、部分转后再转放行、取消订单后可再转（红→绿）；quotation.md 修订落盘
- [x] `erp-sal.quotation-auto-accept-threshold` 归 Deferred（见下）

Exit Criteria:

- [x] quotation.md 规则 1/配置点修订落盘；转化守卫改累计口径；测试红→绿

### Phase 9 — 换货行税额价内税统一（sal-017，Fix）

Status: completed
Targets: ErpSalReturnGenerateExchangeDeliveryProcessor.java
Skill: none

- Item Types: `Fix`

- [x] `createExchangeDelivery` 税额公式改价内税：`rate = taxRate/100(6dp)`；`taxAmount = amount×rate/(1+rate)(4dp HALF_UP)`（对齐 recomputeLineAmount P1-RC-022 公式）；含税合计与头合计联动复核
- [x] Proof：测试——同 unitPrice/taxRate 下换货行税额与订单行口径一致（数值断言）；红→绿

Exit Criteria:

- [x] 公式统一；测试红→绿

### Phase 10 — 过账异常 sweep 竞态通道（sal-019，Fix | Add，跨域 sales→finance）

Status: completed
Targets: ErpFinPostingExceptionBizModel.java + IErpFinPostingExceptionBiz.java（finance 新 biz 方法）、ErpSalInvoiceCancelProcessor.java、ErpSalInvoiceReverseApproveProcessor.java
Skill: none

- Item Types: `Add | Fix`

- [x] finance 新增 `ignorePendingByBill(billHeadCode, businessType, context)`：按 billHeadCode+businessType+status=PENDING 查询逐条置 IGNORED（复用 ignore 状态推进语义；sweep 仅扫 PENDING → 重放通道关闭）
- [x] sales invoice cancel/reverseApprove 的**全部无红冲路径**（posted=false 全跳过分支，及 P1-CK-sal-003 双判后 reverse 已跑分支）均联调 `ignorePendingByBill(invoice.code, SAL_INVOICE)`（失败隔离 try/catch LOG.warn 不阻断主流程，对齐 RC-R1.85 容错范式）——PENDING 异常在任一「不红冲」出口都须关闭重放通道
- [x] Decision：修 sales 侧联动而非 finance retry 回查——模块依赖方向 finance ↛ sales，回查不可行；通道建立同时构成 fin-006「源单有效性校验通道」的 sales 侧实现（fin-006 行归 finance 批复核）
- [x] Proof：测试——发票过账异常 PENDING 后 cancel → 异常记录置 IGNORED、sweep 扫描零命中；红→绿

Exit Criteria:

- [x] 通道 + 双入口联动落地；测试红→绿

### Phase 11 — 应收超期预警 OR 语义（sal-020，Fix）+ sal-018 doc 漂移修正（Fix）

Status: completed
Targets: ErpSalDashboardBizModel.java、SalReturnPostingDispatcher.java（javadoc）
Skill: none

- Item Types: `Fix | Decision`

- [x] sal-020：命中条件改 OR——`dayEnabled=daysThreshold>0`、`amountEnabled=amountThreshold>0`，`命中 = (dayEnabled && age>days) || (amountEnabled && open>amount)`；早退保持「双关才关」（默认双关 = 关闭，注释语义不变）；javadoc 同步「任一启用维度命中即报」
- [x] sal-018 Decision：选审计选项 b（修订 javadoc 承认单路径）——KEY_OFFSET_ESTIMATED_RECEIVABLE 全仓零消费实测、「已开票→红字发票」为跨域未实现特性（P2-MA2-011 watch-only 在案）、ErpSalReturnLine 无 invoiceId 列动态置值无判定链；javadoc 改为「预留标记，恒 TRUE；消费方未接线（单路径 credit memo），接线时须实现双路径」；备选 a（动态置值）弃——无消费方时为无效改动且误导
- [x] Proof：sal-020 测试——仅配天数阈值命中、仅配金额阈值命中、双关关闭、双开 AND→OR 语义数值断言；红→绿

Exit Criteria:

- [x] OR 语义 + javadoc 修正落地；测试红→绿

## Draft Review Record

- Independent draft review iteration 1: needs revision (agent_8fe0cf88, 2026-09-17) because B1 Phase 4 reverseSettlement 双侧守卫自锁（CANCELLED 发票存量核销清理通道 + ReturnRefundOrchestrator 链断裂）；B2 EXPIRED 落库载体不存在（approveStatus 绑定 wf/approve-status，无报价状态字典/列）；B3 sal-016 累计口径操作数未落实；B4 Phase 1 Proof 与同类型排他语义自相矛盾；B5 Phase 7 INLINE 守卫未处理 erp-common.sod-enabled %test 门控；B6 job 三件套范式二义。非阻塞 7 条（行号微漂/Phase 2 residual 理由/Phase 6 门控一致性与破坏面/Phase 5 措辞/Phase 10 出口覆盖/Closure Gates auto-accept 标注/Baseline orchestrator 形态注记）。
- Independent draft review iteration 2: needs revision (agent_8fe0cf88, 2026-09-17) because B1-B6 修订与技术断言全部经实码验证成立，残留 2 处文本级陈旧矛盾：L51 Infrastructure Prereqs 残留已删除的 job yaml 交付项、Deferred auto-accept 条目「过期日扫已实现」虚假陈述（与选项 c 决策相反）。本轮修正该两处；审查者明示「修正后可直接记录为 acceptable（无需第三轮全量复审）」——按此授权本计划于两处修正后收敛为 acceptable，翻 active 实施。

## Closure Gates

- [x] 范围内行为完成（Phase 1-11 全部退出标准勾选）
- [x] 相关文档对齐（state-machine.md 退货成本加权注记 + 撤回守卫；quotation.md 规则 1/配置点修订 + auto-accept 配置点标注 deferred 未实现；ai-check-index.md sal-005..020 → fixed；ai-check-roadmap.md F3.5 进度；known-good-baselines.md 基线行；compliance-baseline.md 如有 daoFor 增量 per-site 裁决；docs/logs 当日日志）
- [x] 已运行验证：module-sales `mvn test`；module-common-service `mvn test`；受影响域模块测试；Closure 全 reactor `mvn clean install -DskipTests` + `mvn test`；compliance checker
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符（agent_494153e3，iteration 1 pass）
- [x] 结束证据存在于文件中（本节 + known-good-baselines.md + docs/logs/2026/09-17.md）

## Deferred But Adjudicated

### quotation-auto-accept 自动转订单（sal-015 附带死配置）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 规则/配置点声明「低于阈值客户确认后自动转订单」——accept 动作内自动触发转化涉及转化失败（校验拒绝）时的回滚语义与用户可见反馈，需产品裁决；过期日扫与 EXPIRED 终态未实现，归下方「EXPIRED 状态载体与报价过期日扫 job」Deferred，本 finding（sal-015）以 quotation.md 如实修订关闭（审计选项 c）
- Successor Required: `yes`（触发条件：产品裁决自动转单失败语义后专项实现）

### 已开票→红字发票 GL 路径（sal-018 实质）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 跨域 finance GL 特性，P2-MA2-011 已接受 watch-only；本批完成 doc-code 漂移修正（javadoc 如实声明单路径 + 预留标记）
- Successor Required: `yes`（触发条件：产品要求已开票退货走红字发票路径时立项）

### EXPIRED 状态载体与报价过期日扫 job（sal-015 实质特性）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: EXPIRED 落库需 ORM 模型变更（erp-sal 报价状态字典/列，dual-agent 保护区 + 增量重生成）；懒拦截（requireNotExpired）已覆盖 confirm/convert 时点的过期拦截，owner doc 已修订为如实声明（本批 Phase 8 Decision）
- Successor Required: `yes`（触发条件：EXPIRED 状态字典/列 ORM 变更经双 agent 批准后，按 ct-contract-expiry 范式补日扫 job）

### 报价行级转化映射（sal-016 混合 UoM 近似口径消除）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 头级 Σ 跨行混合 UoM 为近似口径；行级 quotationLineId 映射需模型变更（同上保护区）
- Successor Required: `yes`（触发条件：多 UoM 报价分批转化实际场景出现）

### matchLine 查询下推（sal-008 性能面）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 本批修复确定性（正确性）；N+1 全行加载为性能面，数据量门槛未到
- Successor Required: `no`（触发条件：价格清单行数据量显著增长且取价延迟可观测）

## Closure

Status Note: 计划可关闭——sal-005..020 十六条 P2 全修（含 sal-015 选项 c 设计修订、sal-016 L1 裁决实现、sal-018 选项 b javadoc 修正）；module-sales 331/0/0、全 reactor 4139/0/0/1、checker R2c=1558（+3 per-site 裁决）；4 快照重录为修复预期行为（税率倒推自洽经审计验证）。

Closure Audit Evidence:

- Auditor / Agent: agent_494153e3（独立子代理，fresh session）
- Iteration 1: pass（2026-09-17，无阻塞）——七维度全过：勾选-文本-树一致（15 抽查文件全在树）；代码正确性 12 点实读吻合；验证证据 4139/0/0/1 + sales 331/0/0 + R2c=1558 三站点裁决逐一对应；回填完整（index/roadmap/baselines/三 owner doc/日志）；快照重录无篡改（testTaxSeparation 税率倒推自洽验证：Σ 行税 242.7124 不受头级 AMOUNT_OFF 影响恒等式成立）；反 hollow/Deferred honesty 通过（9 测试类计数精确匹配 + 5 Deferred 带 successor）；baselines 48 行无损。非阻塞残留：owner doc 落点微漂（加权注记落 returns.md 而非 state-machine.md——实际 owner 位置更合理）；matchLine javadoc lineNo→id 措辞（本轮已修）；Phase 10 简写 SAL_INVOICE 实现用 AR_INVOICE（实现正确，计划注记更正）。

Follow-up:

- matchLine 查询下推/EXPIRED 载体+日扫 job/红字发票 GL 路径/报价行级转化映射/auto-accept 自动转订单——均见 Deferred But Adjudicated（带 successor 触发条件）

Follow-up:

- (pending)
