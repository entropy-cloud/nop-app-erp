# ck-purchase — purchase 实现代码检查报告

> 工作项：C2.1。执行日期：2026-08-25。执行者：域检查子代理（ZCode 会话）。
> 范围：`module-purchase/erp-pur-service/src/main/java` 全部 104 个手写生产文件（entity/ 29、processor/ 45、posting/ 6、statemachine/ 16、support/ 2、spi/ 1、dashboard/ 1、根 4）+ `app-service.beans.xml` + 6 份手写 xbiz 守卫核对 + `model/app-erp-purchase.orm.xml`（versionProp/dict/receiveLineId 可空性核对）。api/web 骨架按任务书不深查。
> 方法：Skill: `code-quality-audit-prompt`（发现骨架 + P0-P3 分级）+ `behavioral-failure-mode-scan-prompt`（B1/B2/B3.2/B4 grep 程式）。机械扫描 + 核心链逐文件深读（过账链 4 类、5 大单据 Processor 及 Approve/Cancel/ReverseApprove、ThreeWayMatcher/PaymentSettler/ReturnQtyValidator/ScorecardCalculator/EligibilityChecker/CtDiscountApplier/Converter 全读）+ 跨域核实（finance `ErpFinPostingProcessor.alreadyPosted` 去重、`ErpFinDeferredPostingRetryHelper` 兜底、common `SoDGuard`）。
> D4：module-purchase 无 `*.batch.xml`/job 配置 → 调度链维度除死配置键（P3-CK-pur-012）外 **N/A**。

## 发现清单

> 状态初始 `open`；修复阶段逐项测试验证后回填 `ai-check-index.md`。

### P1-CK-pur-001（D3）采购看板全部主查询消费 docStatus=ACTIVE 死状态（全域零 writer），KPI/趋势/TOP N/及时率/三单预警恒空

- **控制点**：`module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/dashboard/ErpPurDashboardBizModel.java#loadActiveInvoicesInRange`（L245-252）、`#countActiveOrders`（L254-259）、`#computeOnTimeRate`（L271-291）
- **证据**：三处 `q.addFilter(eq("docStatus", ErpPurConstants.DOC_STATUS_ACTIVE))`（L248/L257/L274）。grep 全模块（service Java + `_vfs` xbiz + erp-pur-web）`setDocStatus(...ACTIVE...)` **零命中**——docStatus 生产 writer 仅有 DRAFT（`RequisitionToOrderConverter#build` L68 等）与 CANCELLED（各 CancelProcessor）。owner doc `docs/design/purchase/state-machine.md` L23 明注：「Receive/Invoice/Payment/Return **零 setDocStatus(ACTIVE) 生产 writer**——业务『已生效』由 approveStatus=APPROVED + posted 表达」。
- **问题**：单据实际 docStatus 值域 ∈ {DRAFT, CANCELLED}，`eq(docStatus, ACTIVE)` 恒空集 → `getDashboardKpi`（purchaseAmount/orderCount/onTimeRate）、`getDashboardTrend`、`findVendorTopN`、`findThreeWayMatchDiffAlert` 全部对空集聚合，看板恒零/空（`apBalance`/`findApOverdueAlert` 经跨域 `IErpFinArApItemBiz.findOpenItems` 不受影响）。这是 B2 dict 死状态的**查询侧消费**（owner doc 只裁决了状态机不编码 ACTIVE 入边，未豁免查询消费）。
- **建议修复方向**：过滤条件改为 `approveStatus=APPROVED`（语义=已生效；CANCELLED 单据经 approveStatus 副轴仍 APPROVED 的残留另见 P2-CK-pur-004，修复时一并定口径 docStatus ne CANCELLED 的 xmeta 可行性）；或按 owner doc 死状态裁决补 ACTIVE writer（不建议，动状态机）。
- **arm-index 裁决**：新增。P2-MA4-014/017（view badge ACTIVE 死状态）为 view 层不同控制点；A4.7 明言「docStatus ACTIVE badge 在 pur 正确（绑定共享 erp/doc-status 含 ACTIVE）」——未覆盖后端查询消费。

### P1-CK-pur-002（D7/D2）Invoice/Payment approve 链 SoD 守卫位于 doPosting（REQUIRES_NEW 已提交）之后——守卫抛错回滚主事务但凭证已独立提交成孤儿

- **控制点**：`app/erp/pur/service/processor/ErpPurInvoiceApproveProcessor.java#approve`（L18-31：L26 `doPosting` → L28 `doApprove`；SoD 在 `ErpPurInvoiceProcessor#doApprove` L225 首行）；`ErpPurPaymentApproveProcessor.java#approve`（L18-30 同构，SoD 在 `ErpPurPaymentProcessor#doApprove` L235 首行）
- **证据**：`doPosting` → `postingDispatcher.tryPost` → `IErpFinVoucherBiz.post()`（`@Transactional(REQUIRES_NEW)`，`ErpFinVoucherBizModel.java:74-75` 实证独立事务）。随后 `doApprove` 首行 `SoDGuard.assertApproverNotCreator(...)` 抛 `ERR_PUR_APPROVER_IS_CREATOR` → 外层 @BizMutation 事务回滚（approveStatus 保持 SUBMITTED、posted=false），**REQUIRES_NEW 凭证已提交不可回滚**。对照组：`ErpPurReceiveApproveProcessor#approve` L38 / `ErpPurReturnApproveProcessor#approve` L46 的 SoD 检查位于一切副作用之前——同域内两种顺序并存。
- **问题**：创建人审核自己的发票/付款单（SoD 守卫存在的目标场景）→ AP_INVOICE/PAYMENT 凭证落账但源单未审核。缓解项：finance `ErpFinPostingProcessor` 有 `alreadyPosted` 去重（L139/L168/L488），后续合法审核人重审会复用凭证；但若该单随后被 reject/cancel，cancel 路径因 `posted=false` 不触发 `postingDispatcher.reverse`（`ErpPurInvoiceCancelProcessor#cancel` L30 前置 `posted==true`）→ **孤儿凭证永久残留 GL/AP**。任何 doPosting 之后的异常（如 updateEntity 乐观锁冲突）同构触发。
- **建议修复方向**：对齐 Receive/Return 顺序——SoD 检查提前到 `doPosting` 之前（最简）；或把 doPosting 挪到 doApprove 成功写库之后（与既有「posted 由调用方主事务统一持久化」注释兼容性需评估）。
- **arm-index 裁决**：新增（grep SoD/REQUIRES_NEW 顺序、孤儿凭证 零命中）。

### P1-CK-pur-003（D5/D3）通用 CRUD update 路径无「已审核/已过账不可修改」守卫（头+行全实体）——三单匹配/核销/聚合的数据基线可被直接改写

- **控制点**：`app/erp/pur/service/entity/ErpPurInvoiceBizModel.java`（仅覆写 cancel，`defaultPrepareUpdate` 无守卫）；`ErpPurOrderLineBizModel.java#defaultPrepareSave/Update`（L29-38 仅应用 CT 折扣）；`ErpPurInvoiceLineBizModel`/`ErpPurReceiveLineBizModel`/`ErpPurReturnLineBizModel`/`ErpPurPaymentLineBizModel`（裸 CrudBizModel）
- **证据**：`docs/design/purchase/three-way-match.md §一致性规则`：「已审核的发票不允许修改回链关系（避免破坏已生成的应付凭证）」；`state-machine.md §1`：可修改单据=UNSUBMITTED/REJECTED。实现层仅审批动作有状态守卫，GraphQL `ErpPurInvoice__update`/`ErpPurOrderLine__update` 等通用 mutation 无 approveStatus/posted 拦截。
- **问题**：APPROVED+posted 发票的 totalAmount/行回链、APPROVED 订单行 unitPrice（`ThreeWayMatcher#match` L92 价格比对基线 + `computeOverTolerancePriceVariance` PPV 基线）、PaymentLine 金额（`PaymentSettler#sumInvoiceLines/sumPaymentLines` 聚合基线）均可直接改写 → 已过账凭证金额与源单漂移、paidStatus/超收聚合/退货可退量全部失真，且无任何告警。
- **建议修复方向**：头/行 BizModel 的 `defaultPrepareUpdate`（及 line 的 Save）加 `approveStatus ∈ {UNSUBMITTED, REJECTED} && docStatus != CANCELLED` 守卫（REJECTED 可改是设计允许）；修复阶段先核对 AMIS 页面是否已隐藏已审核单据编辑入口（UI 掩盖不豁免后端守卫缺失）。
- **arm-index 裁决**：新增（grep 通用 CRUD/已审核修改 零命中）。**跨域同型**：sales/inventory 等域行级 BizModel 同为裸 CRUD——归属各域检查工作项核对（C2.2/C2.3）。
- **注**：本条为本报告最不确定 finding（见 §剩余风险）——若平台/xmeta 层存在本代理未发现的统一更新拦截，修复阶段证伪即可。

### P2-CK-pur-004（D6/D8）「CANCELLED 但仍 APPROVED 计入聚合」三处联发 + cancel/reverseApprove 后订单收货进度不重算

- **控制点**：
  1. `app/erp/pur/service/processor/ErpPurReceiveProcessor.java#rollupOrderReceiveStatus`（L451-491）+ `#findApprovedReceives`（L567-571，仅滤 `approveStatus=APPROVED` 不滤 docStatus）——且 rollup 仅在 `postProcessApprove`（L293-295）调用；`ErpPurReceiveCancelProcessor#cancel`（L42-53）与 `ErpPurReceiveReverseApproveProcessor#reverseApprove`（L31-45）冲销库存后**不重算** order.receiveStatus。
  2. `ErpPurReceiveProcessor#validateOverReceiptTolerance`（L203-252）——javadoc L201 自认「聚合口径继承 rollupOrderReceiveStatus 现状：CANCELLED 但仍 APPROVED 的入库单计入 Σ」（RC-R1.11 修复时显式继承）。
  3. `app/erp/pur/service/entity/ReturnQtyValidator.java#sumApprovedReturnedByReceiveLine`（L72-101）——已作废退货单（approveStatus 仍 APPROVED）继续占用可退量。
- **问题**：作废已审核入库单（库存已冲回）后：订单 receiveStatus 陈旧保持 RECEIVED/PARTIAL；同订单再次入库在 strict 模式被超收双计误拒（非 strict 误 warn）。作废退货单后可退量不释放，阻断后续合法退货。三个聚合点 + 一个缺失的重算点同根因。
- **建议修复方向**：聚合查询统一追加 docStatus 过滤（xmeta 若不支持 ne 则按 `ErpPurOrderBizModel#existsActiveByRequisition` L140-148 的「管道查后内存剔除」范式）；receive cancel/reverseApprove 后置调 `rollupOrderReceiveStatus` 重算。
- **arm-index 裁决**：新增（带复用注记）。RC-R1.11 修复计划显式记录该口径为「继承现状」但从未登记为 finding；A2.8 审计判 rollup「逻辑正确」仅针对 approve 路径（L79/L104），cancel 路径陈旧与 ReturnQtyValidator 未被任何记录覆盖。

### P2-CK-pur-005（D8）Payment cancel/reverseApprove 不守卫也不回滚核销；Invoice cancel 不守卫已核销状态

- **控制点**：`ErpPurPaymentCancelProcessor.java#cancel`（L24-38：仅凭证 reverse + docStatus=CANCELLED）；`ErpPurPaymentReverseApproveProcessor.java#reverseApprove`（L22-37：仅凭证 reverse + APPROVED→REJECTED）；`ErpPurInvoiceCancelProcessor.java#cancel`（L24-39：无 paidStatus 守卫）
- **证据**：三条路径均不触及 PaymentLine 核销行，也不调用 `PaymentSettler#reverseSettlement`；`validateTransitionForCancel` 仅检查 docStatus≠CANCELLED。owner doc：`returns.md §异常处理`「已核销退货：发票已全额核销付款 → **需先撤回核销**」；`state-machine.md §6`「作废已审核单据需冲销已生成结果」（核销即已生成结果）。
- **问题**：已核销的付款单被作废/反审核 → GL 付款凭证已红冲，但发票 `paidAmount/paidStatus` 仍为已付（陈旧派生态），且付款 writtenOffStatus 不回退；后续对同发票的核销/余额判断失真。已部分/全额付款的发票也可直接作废。`reverseSettlement` 是独立手动动作，无强制前置。
- **建议修复方向**：cancel/reverseApprove 前置守卫「存在净核销（sumPaymentLines>0）即拒绝，提示先 reverseSettlement」（对齐 returns.md 拒绝语义）；或自动逐发票生成反向核销行（保留审计轨迹范式）。
- **arm-index 裁决**：新增（P1-MA2-003 settle 守卫缺口已修，是 settle 入口不同控制点）。

### P2-CK-pur-006（D7）三处聚合校验无共享行锁的并发竞态：超收容差 / 退货可退量 / 请购转订单幂等

- **控制点**：
  1. `ErpPurReceiveProcessor#validateOverReceiptTolerance`（L203-252）——两张入库单并发 approve：各自聚合读不到对方未提交量，双双通过（订单 10 + 容差 5% + 并发两张各 8）。
  2. `ReturnQtyValidator#validate`（L46-66）——两退货单并发 approve 同上超额退货；javadoc L33 声称「跨退货单并发超额由退货单自身 version 乐观锁 + 审核时重查聚合兜底」**不成立**：不同退货单是不同行，互不触发 version 冲突，无共享行被更新。
  3. `ErpPurRequisitionProcessor#convertToOrder`（L95-103）+ `#validateNotAlreadyConverted`（L197-202）——幂等纯靠 `existsActiveByRequisition` 存在性检查；转化不更新请购行（无 version bump）→ 并发两次调用双双通过检查生成重复订单。
- **对照（验证为正确）**：`PaymentSettler.settle` 并发核销受发票行乐观锁保护（`recomputeInvoicePaid` L203-219 updateEntity 触发 version 冲突；orm 全实体 `versionProp="version"`，A2.17 审计 purchase 32/32 100%）。
- **建议修复方向**：共享行加锁点——校验前对被聚合的 orderLine/receiveLine 行做一次 touch 更新（或 SELECT FOR UPDATE 经 IOrmTemplate）；请购转化在请购头写「已转化」标记列或至少 bump version。
- **arm-index 裁决**：新增（带复用注记）。A2.17 证伪的是核销/超卖类「有共享行写」路径（不同控制点）；P2-RC-012 裁决的是 cancel-后-重转语义（非并发维度）。

### P2-CK-pur-007（D6）Dashboard 三单价格容差与 ThreeWayMatcher 同配置键不同量纲（ratio vs percent）

- **控制点**：`ErpPurDashboardBizModel#findThreeWayMatchDiffAlert`（L178-187：默认 `new BigDecimal("0.05")`，`ratio = diff.divide(orderPrice, 4, ROUND_HALF_UP)` 后 `ratio.compareTo(tolerance) > 0`）vs `ThreeWayMatcher#priceDiffPercent`（L151-156：`diff×100/orderPrice` 百分比，默认 5）
- **问题**：同一键 `erp-pur.match-price-tolerance`（owner doc `three-way-match.md §不匹配的处理策略` 默认 5 = **百分数**）在 dashboard 被当**小数比例**消费。未配置时 0.05≈5% 恰好等效（掩盖缺陷）；一旦按文档语义配置（如 8 = 8%），dashboard 比较域 (0,1] vs 8 → 预警除差异>800% 外永不触发。另 `configured.signum() <= 0` 把 0（零容差意图）静默回退 5%。
- **建议修复方向**：dashboard 统一 `×100` 百分比口径（与 matcher 同式）；零值语义显式裁决（0=零容差全报警 or 显式禁用）。
- **arm-index 裁决**：新增。当前被 P1-CK-pur-001 掩盖（查询恒空），但独立成立。

### P2-CK-pur-008（D5）退货行 receiveLineId 可空 → 无回链行完全绕过数量上限校验

- **控制点**：`ReturnQtyValidator#validate`（L48-52：`receiveLineId == null → continue`）；`module-purchase/model/app-erp-purchase.orm.xml` `erp_pur_return_line.receiveLineId` 列无 `mandatory`；`ReturnStockMoveBuilder#buildLines`（L57-69）对全部行（含无回链行）生成出库请求
- **问题**：owner doc `returns.md §退货约束`「退货数量 ≤ 原入库未退货数量」对无回链行完全失效——可退任意数量，库存照减、PURCHASE_RETURN 凭证照红冲。设计允许「独立创建退货单」，但独立路径应有库存充足性兜底；修复阶段需核对 inventory 域出库守卫是否拦截（跨域核对归属：C2.3 inventory）。
- **建议修复方向**：或强制 receiveLineId 必填（ORM 变更走 dual-agent），或对无回链行改为仅依赖 inventory 出库校验并在 javadoc/owner doc 显式登记该边界。
- **arm-index 裁决**：新增。

### P2-CK-pur-009（D6/D2）请购转订单：非法税率串静默按零税处理（与单价的抛错不对称）

- **控制点**：`RequisitionToOrderConverter#parseTaxRate`（L129-139：`NumberFormatException → LOG.warn → return null` → `taxAmount = ZERO`）vs `#parseUnitPrice`（L108-127：空/负/非法一律抛 `ERR_INVALID_UNIT_PRICE`）
- **问题**：调用方 `lineTaxRates` 传入非法串（如 "13%"）→ 生成订单不含税，`totalAmountWithTax` 偏低，仅日志无用户可见错误——金额正确性受静默降级影响，且与同函数内单价的严格校验不一致。
- **建议修复方向**：对齐 parseUnitPrice 抛 `NopException`（新增/复用 invalid-tax-rate 错误码）；如需宽容语义则至少 warn 内容回传调用方。
- **arm-index 裁决**：新增。

### P3-CK-pur-010（D2/D10）currentUserId 宽 catch 返回 null 无日志（5 处 Processor 同型）

- **控制点**：`ErpPurOrderProcessor#currentUserId`（L420-430）及 Receive/Invoice/Payment/Return Processor 同型方法
- **问题**：`catch (Exception e) { return null; }` 吞异常无日志——approve 的 `approvedBy/postedBy` 审计字段可静默为 null。与 pilot P3-CK-md-008 跨域同型（不同站点须独立修复，状态不继承）。
- **建议修复方向**：窄化 catch 或至少 log.warn。

### P3-CK-pur-011（D9）Dashboard 无界加载 + N+1

- **控制点**：`ErpPurDashboardBizModel#findVendorTopN`（L137 `loadActiveInvoicesInRange(null, null)` 无 limit 全表入内存）与 `#findThreeWayMatchDiffAlert`（L183 同 + `hasPriceVariance` L308-350 每发票 3 次查询）；`#findApOverdueAlert`（L221-239 逐 item `partnerDao.getEntityById` 无缓存，对照 findVendorTopN 的 nameCache L154-166）
- **问题**：vendorTopN/三单预警对发票表无界扫描；AP 超期预警 partner 名 N+1。当前被 P1-CK-pur-001 掩盖（空集），修复 001 后将显性化。`loadOrderDeliveryDates`（L294-305）有 limit 5000 属「类 C」显式裁决不登记。
- **建议修复方向**：与 001 一并修复——SQL 聚合（groupBy supplier/month）或 setLimit + truncated 标记；partner 名批量化。
- **arm-index 裁决**：orgId 行级过滤缺失维度**复用 P2-RC-086**（successor watch-only，不重复登记）；本条仅登记性能维度。与 P3-CK-md-012 同族。

### P3-CK-pur-012（D4）声明未接线的配置/常量（doc-code 漂移）

- **控制点**：`ErpPurConstants`：`CONFIG_RETURN_APPROVAL_REQUIRED`（L67，`returns.md §配置项` 声明默认 true「退货是否需要审核」——grep 全模块零消费，退货审批恒必须，`return-approval-required=false` 配置无效）；`CONFIG_SCORECARD_EVALUATION_CRON`（L84，`supplier-evaluation.md §配置点` 声明周期评估 cron——零消费且无 job，周期评估只能手动 finalizeScorecard）；`PAID_FULL_RATIO`（L71，零消费死常量）。另 `ErpPurConfigs` 为空接口（仅注释）。
- **建议修复方向**：接线或删除常量并同步 owner doc 配置表；评分卡周期评估 job 若补须新增 batch.xml（届时 D4 不再 N/A）。
- **arm-index 裁决**：新增（grep 三符号零命中）。

### P3-CK-pur-013（D7）batchApprove 逐行吞 NopException 但共享单事务——失败行此前的会话脏写随外层提交

- **控制点**：`ErpPurOrderBizModel#batchApprove`（L100-115：catch NopException → recordFailure 继续）+ `ErpPurOrderProcessor#validateBusinessRulesForApprove`（L171-175：`recalcCtDiscountForApprove` 在 SoD/doApprove 之前把行改价写脏）
- **问题**：某行 approve 在 doApprove 处失败（如 SoD）时，该行已在 validateBusinessRules 阶段被 CT 折扣重定价（托管实体脏值）——异常被 catch 后外层事务照常提交 → 失败行保持 SUBMITTED 却被静默重定价 + 头合计重算。缓解：折扣基数取合同行单价（稳定基数）幂等，语义≈重新保存，故降 P3。
- **建议修复方向**：validateBusinessRules 拆分为纯校验（不改值）+ doApprove 内改值；或 batchApprove 每行独立事务（REQUIRES_NEW 编排）。
- **arm-index 裁决**：新增。

### P3-CK-pur-014（D10）ThreeWayMatcher 悬挂回链静默跳过校验

- **控制点**：`ThreeWayMatcher#match`（L66-69：`loadReceiveLine(id)` 返回 null → `continue`）；`#computeOverTolerancePriceVariance`（L134-138 同型）
- **问题**：发票行 receiveLineId 指向不存在的入库行时（数据修复/手工导入场景），数量与价格校验双双静默跳过，发票照常过账。正常 ORM FK 路径不悬挂，概率低。
- **建议修复方向**：receiveLineId 非空但加载失败时抛 `NopException`（dangling-reference 类错误码）。
- **arm-index 裁决**：新增。

## 验证为正确（显式排除，防误报）

- **B1 过账吞异常范式**：三个 PostingDispatcher 的 `tryPost` catch-吞异常-返回 false 是 owner doc 显式设计（`state-machine.md §4`「财务过账失败 → posted=false + 异步重试 + 兜底扫描」；`posting.md`）；兜底经 finance `ErpFinDeferredPostingRetryHelper`（实仓存在）承接；`reverse()` 均重抛（硬前置）。非 B1 缺陷。
- **PPV 凭证借贷恒等**：`PurAcctDocProvider#createFacts`（L74-111）variance≠0 时 1403=amount−variance + 1404=|variance|（涨价借/降价贷）：涨价 Dr=amount−v+v+tax=amount+tax=Cr(withTax)；降价 Dr=amount+|v|+tax，Cr=withTax+|v|——两侧恒等（数学复核）。差异 0/键缺失走既有三行零变化。
- **核销并发**：`PaymentSettler.settle`（L65-123）跨付款单并发核销同一发票受发票行乐观锁保护（`recomputeInvoicePaid` L218 `updateEntity` 触发 version 冲突；orm 全实体 `versionProp="version"`；A2.17 审计 purchase 32/32）；单调用内 `paymentRemaining` 逐笔递减、双余额守卫（发票余额 L95 + 付款余额 L101）、同供应商校验（L159）齐备；负金额分配静默跳过（L87-89）属可接受防御。
- **平台 API 语义**：`QueryBean.addOrderField(name, desc)` 第二参为 desc（nop-entropy `QueryBean.java:433`，pilot C1.2 实证）——`SupplierEligibilityChecker#findLatestFinalizedScorecard`（L80 `addOrderField("periodTo", true)` 取最新周期）正确。
- **超收容差数学**：`validateOverReceiptTolerance` 边界 `<=` 放行（恰好容差边界通过，对齐 owner doc 注记）；tolerance/100 scale 4 HALF_UP；当前入库单排除自身不双计（L220-222）；orderLineId null 跳过；订单数量 0 基。
- **SoD 顺序正确的对照**：Receive（L38）/Return（L46）approve 的 SoD 在一切副作用之前。
- **INLINE 守卫修复落地**（P1-MA2-050 Fix）：`ErpPurQuotationBizModel`/`ErpPurRfqBizModel` 的 5 个 `prepare*` helper 均先 `requireNotCancelled` 再状态机断言——已消除 CANCELLED 副轴漂移主缺陷。
- **PurReversalListener 回退目标态表**：四业务类型均 posted=false + APPROVED→REJECTED，与 `state-machine.md §reversal listener 回退目标态表` 逐行一致（P1-MA2-051 已修）；daoFor 直写豁免已在类 javadoc 文档化（同模块自身域实体）。
- **CT 折扣幂等**：`ErpPurCtDiscountApplier#resolveBasePrice`（L75-81）基数=合同行单价（非自身折后价），save/approve 重复解析无二次折扣；remark 标记剥离重打。
- **已修复 finding 复核在位**：RC-R1.11 `validateOverReceiptTolerance`、R1.8 settle 三单复核 config-gate（`PaymentSettler#recheckThreeWayMatchAtSettle` L183-193，cause 链保留）、RC-R1.12 承付恢复 hooks（invoice L356-399 + return L337-384，含三守卫与精确容错）、P1-MA2-051 rollbackReceive 对齐——代码与登记的修复形态一致。
- **N+1 反例（正确写法在位）**：`ErpPurInvoiceProcessor#resolveLinkedOrderCodes`（L420-459）四级批量 in-query；`ErpPurSkuReferenceChecker` exists 查询 limit 1 + 关联属性路径过滤。
- **D1 机械扫描全零**：`@Inject private`=0、`System.currentTimeMillis/LocalDateTime.now`=0、`extends RuntimeException/Exception`=0、字符串 `==` 仅 null 判等（`Objects.equals` 用于值比较）、无手编生成产物（checker 基线 R4/R5/R7=0）。
- **评分卡链**：`ScorecardCalculator` 权重和=100 校验、公式异常包装 `ERR_SCORECARD_FORMULA_EVAL_FAIL`（非吞咽）、`determineStanding` 双阈值三档与 dict 三值一致；`ScorecardStandingLinker` RED→AVL suspend 经 I*Biz 单事务。
- **取价匹配链**：`ErpPurSupplierPriceResolver` 效期闭区间 + priority 小者优先 + 同 priority 低价优先，与 orm.xml:399 声明一致；`AppConfig.var` BigDecimal 泛型默认值用法正确。

## arm-index 复用 or 新增裁决（汇总）

- 关键符号 `docStatus=ACTIVE 查询消费`/`SoD doPosting 顺序`/`通用 CRUD update 守卫`/`ReturnQtyValidator`/`convertToOrder 并发`/`parseTaxRate`/`CONFIG_RETURN_APPROVAL_REQUIRED` 在 `docs/audits/arm-index.md` **零命中 → 新增**。
- **复用（不重复登记）**：dashboard orgId 行级过滤缺失 → P2-RC-086（successor watch-only，全域看板直访路径，含 pur）。
- **带复用注记的新增**：P2-CK-pur-004（RC-R1.11 计划显式「继承现状」+ A2.8 只审 approve 路径）、P2-CK-pur-006（A2.17 证伪的是有共享行写的核销/超卖类；P2-RC-012 裁决的是非并发语义）。
- 已验证修复在位不登记：P1-MA2-050（INLINE 守卫）、P1-MA2-051（rollbackReceive）、P1-MA2-003/R1.8（settle 复核）、P1-RC-019/RC-R1.11（超收容差）、P1-MA2-083/RC-R1.12（承付恢复）、P1-RC-018/RC-R1.50（PPV 过账）。

## 统计

| 级别 | 数量 | 编号 |
| --- | --- | --- |
| P0 | 0 | — |
| P1 | 3 | P1-CK-pur-001/002/003 |
| P2 | 6 | P2-CK-pur-004..009 |
| P3 | 5 | P3-CK-pur-010..014 |

按维度（主维度计）：D3×1、D5×2、D6×3、D7×3、D8×1、D9×1、D2×1、D10×1、D4×1（P1-CK-pur-002 跨 D7/D2 计 D7；P1-CK-pur-003 跨 D5/D3 计 D5；P2-CK-pur-004 跨 D6/D8 计 D6；P2-CK-pur-009 跨 D6/D2 计 D6）。D4 除死配置键外 N/A（无 batch.xml）。

## 剩余风险（查了什么/没查什么）

- **已查**：erp-pur-service 104 个手写文件中约 60 个逐行深读（全部 posting/entity 核心辅助类、5 大单据的 Processor+Approve+Cancel+ReverseApprove、Requisition convert 链、Quotation/Rfq BizModel、Dashboard、SPI、beans.xml）；其余 ~44 个为薄委托/状态机 Bean/Submit/Withdraw/Reject/LINE BizModel（结构性同构，抽样核对了守卫形态与 beans 注册一一对应）；xbiz 6 份审批动作接线与 auth 抽查（approve/reverseApprove 带 auth，submit/reject/withdraw 裸注解——MA6 已裁决 FNPT 坍缩范式，不重开）；orm versionProp/dict/returnLine 可空性核对；跨域核实 finance 过账去重与兜底、common SoDGuard。
- **未深查**：`erp-pur-web` AMIS view.xml 与后端契约 drift（属 A4.7 已审范畴 + C8.2）；`erp-pur-api`/meta 生成物；P1-CK-pur-003 的平台层统一更新拦截可能性（xmeta updatability/data-auth 全量矩阵未逐字段核——列为该 finding 的证伪路径）；P2-CK-pur-008 的 inventory 出库充足性守卫（归 C2.3）；P1-CK-pur-001 修复后 P3-CK-pur-011 性能问题显性化的联合修复方案；16 个状态机 Bean 仅抽样 2 个（守卫矩阵已由 M2-M4 计划审计覆盖）。
- **最不确定、建议主 agent 复核**：P1-CK-pur-003（通用 CRUD 可改性——若平台有未发现的统一拦截则降 not-a-problem）；P1-CK-pur-002 的实际 GL 影响面（依赖 finance alreadyPosted 去重语义的边界——已查 L139/L168/L488 存在，但重入返回值路径未逐行追）。
