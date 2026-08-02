# ARM-MA2 purchase 状态机系统业务审查报告（A2.8）

> 里程碑：MA2（业务正确性层 / 状态机正确性维度）
> Roadmap 工作项：A2.8（A 级单域，29 状态字段）
> Plan：`docs/plans/2026-07-28-0230-3-audit-remediation-ma2-purchase-state-machine.md`
> 行为基线：`docs/design/purchase/{state-machine,returns,requisition,three-way-match,supplier-evaluation}.md` + `docs/architecture/{processor-extension-pattern,posting-exemptions}.md`
> Skill：`docs/skills/state-machine-business-review-prompt.md`（10 维度审查方法）
> 实仓快照：2026-07-28（HEAD 经 `compliance-baseline.md §M0 锚点` 验证一致）
> 裁决：**Verdict = ⚠️(P1)**——采购九实体状态机核心契约经证据确认（迁移守卫齐全、@BizMutation 事务回滚、reverseApprove 红冲闭环强一致经大 Processor 路径成立、跨域写经 I*Biz Facade）；零 P0；**新增 3 项 P1**（P1-MA2-049 Quotation/Rfq reverseApprove→SUBMITTED 违反 owner doc §2 强制 REJECTED 契约漂移 / P1-MA2-050 INLINE reject/withdrawApproval 绕过 isCancelled 守卫致 CANCELLED 单据 approveStatus 副轴漂移 / P1-MA2-051 PurReversalListener.rollbackReceive 不对称致冲销后 receive APPROVED+posted=false 悬挂）；**新增 3 项 P2** watch-only（P2-MA2-053 三种并行模式 owner doc 未声明 / P2-MA2-054 死代码 WithdrawApproval/Reject Processor 未接线 / P2-MA2-055 payment writtenOffStatus 复用 paid-status 字典语义漂移）；6 项已登记 MA1/MA2 finding 运行时复核**无升级**（P1-MA1-022 跨域只读维持治理缺陷 / P1-MA2-001 暂估冲回状态机角度无升级 / P1-MA2-002 多币种状态机角度无影响 / P1-MA2-003 settle 守卫缺口维持 P1 / P2-MA2-008 并发核销交接 A2.17 / P2-MA1-026 scorecard defaultValue 无升级）；并发敏感点 5 处交接 A2.17。

---

## 1. 范围与基线

### 1.1 在范围

九采购实体（`module-purchase/model/app-erp-purchase.orm.xml`，1284 行，29 状态字段）× 三轴（docStatus/approveStatus/业务轴）：

| 实体 | docStatus | approveStatus | 业务轴 | posted | 备注 |
|------|-----------|---------------|--------|--------|------|
| `ErpPurOrder` | erp/doc-status | wf/approve-status | paidStatus(erp-pur/paid-status) + receiveStatus(erp-pur/receive-status) | ✅ | 订单是意向，approve 仅状态推进，无直接库存/凭证 |
| `ErpPurReceive` | erp/doc-status | wf/approve-status | receiveStatus(erp-pur/receive-status) | ✅ | approve 触发库存 incoming + 暂估应付 PURCHASE_INPUT |
| `ErpPurInvoice` | erp/doc-status | wf/approve-status | paidStatus(erp-pur/paid-status) | ✅ | approve 触发 AP_INVOICE 过账 |
| `ErpPurPayment` | erp/doc-status | wf/approve-status | writtenOffStatus(复用 erp-pur/paid-status) | ✅ | useWorkflow=true + nopFlowId，双路径可达 APPROVED |
| `ErpPurReturn` | erp/doc-status | wf/approve-status | —（returnStatus 是派生视图，无 ORM 列） | ✅ | approve 触发出库 + PURCHASE_RETURN 过账 |
| `ErpPurRequisition` | erp/doc-status | wf/approve-status | — | — | 请购单，convertToOrder 前置 APPROVED |
| `ErpPurRfq` | erp/doc-status | wf/approve-status | — | — | 询价单，全 INLINE，无 Processor |
| `ErpPurQuotation` | erp/doc-status | wf/approve-status | isAccepted（布尔，无状态机字典） | — | 报价单，全 INLINE，无 Processor |
| `ErpPurSupplierScorecard` | —（无 docStatus） | — | standing(erp-pur/supplier-standing) + status(erp-pur/scorecard-status) | —（不过账） | 周期评分卡，DRAFT→FINALIZED + standing RED→AVL SUSPENDED |

### 1.2 不在范围（Non-Goals 见 plan）

- A2.1 P2P 端到端编排正确性（done）
- A2.5 finance 凭证/期间/AR-AP 状态机（done）
- A4.5 代码质量审计（Processor 异常处理/N+1/索引）
- A2.17 并发与乐观锁（P2-MA2-008 并发核销）
- A4.7 view.xml drift
- config-gated Deferred 偏离本身

---

## 2. 九实体 × 三轴状态图与转换矩阵

### 2.1 审批轴 UNSUBMITTED→SUBMITTED→APPROVED→REJECTED 迁移矩阵（按实体×动作×实现路径）

> 实现路径列：**PROC** = 经大 Processor 全守卫（validateNotCancelled/validateTransition*/validateBusinessRules*）；**INLINE** = xbiz 脚本直设 approveStatus，仅校验 `status==='SUBMITTED'`（reject/withdraw）或 `status==='APPROVED'`（reverseApprove）；**平台审批** = nop-wf 工作流（仅 Payment）

| 实体 | submitForApproval | approve | reject | reverseApprove | withdrawApproval | cancel | 其他动作 |
|------|------|--------|--------|----------------|------------------|--------|---------|
| Order | PROC→SUBMITTED | PROC→APPROVED | PROC→REJECTED | PROC→**REJECTED**（清 approvedBy/At） | **INLINE**→UNSUBMITTED | PROC→docStatus=CANCELLED | — |
| Receive | PROC→SUBMITTED | PROC→APPROVED + receiveStatus=RECEIVED + posted + order.receiveStatus 滚动汇总 | **INLINE**→REJECTED | PROC→REJECTED + posted=false + 凭证 reverse | **INLINE**→UNSUBMITTED | PROC→docStatus=CANCELLED | — |
| Invoice | PROC→SUBMITTED | PROC→APPROVED + tryPost AP_INVOICE | **INLINE**→REJECTED | PROC→REJECTED + posted=false + 凭证 reverse | **INLINE**→UNSUBMITTED | PROC→docStatus=CANCELLED + reverse | — |
| Payment | PROC+wf→SUBMITTED + 启动 nopFlowId | PROC→APPROVED + tryPost PAYMENT | **INLINE**→REJECTED | PROC→REJECTED + posted=false + 凭证 reverse | **INLINE**→UNSUBMITTED | PROC→docStatus=CANCELLED | settle/reverseSettlement（核销） |
| Return | PROC→SUBMITTED | PROC→APPROVED + posted + 出库 + PURCHASE_RETURN 凭证 | **INLINE**→REJECTED | PROC→REJECTED + posted=false + 凭证 reverse | **INLINE**→UNSUBMITTED | PROC→docStatus=CANCELLED | — |
| Requisition | PROC→SUBMITTED | **INLINE**→APPROVED ⚠️ + ErpPurRequisitionProcessor.approve 死代码 | **INLINE**→REJECTED ⚠️ + Processor.reject 死代码 | PROC→REJECTED | **INLINE**→UNSUBMITTED | PROC→docStatus=CANCELLED | convertToOrder（PROC，APPROVED 前置） |
| Quotation | **INLINE**→SUBMITTED | **INLINE**→APPROVED | **INLINE**→REJECTED | **INLINE**→**SUBMITTED** ⚠️⚠️ 违反 owner doc §2 | **INLINE**→UNSUBMITTED | ErpPurQuotationBizModel.cancel 仅设 docStatus，不触及 approveStatus | — |
| Rfq | **INLINE**→SUBMITTED | **INLINE**→APPROVED | **INLINE**→REJECTED | **INLINE**→**SUBMITTED** ⚠️⚠️ 违反 owner doc §2 | **INLINE**→UNSUBMITTED | 无 cancel 方法 | — |
| SupplierScorecard | —（无审批轴） | — | — | — | — | — | finalizeScorecard（DRAFT→FINALIZED + standing=RED→AVL SUSPENDED） |

### 2.2 PROC vs INLINE 模式对比矩阵（同一动作两路径行为对比）

> **核心安全问题**：INLINE 路径仅校验 `status==='SUBMITTED'`（reject/withdraw）或 `status==='APPROVED'`（reverseApprove），**缺失** PROC 路径的下列守卫：

| 守卫 | PROC 路径（Order/Receive/Invoice/Payment/Return/Requisition） | INLINE 路径（Quotation/Rfq 全动作 + 其他实体 reject/withdraw） |
|------|------|------|
| `validateNotCancelled`（docStatus != CANCELLED） | ✅ 拒绝 CANCELLED 单据做任何审批迁移 | ❌ **不校验**——CANCELLED 单据的 approveStatus 副轴可漂移（P1-MA2-050） |
| `validateTransition*`（src 状态匹配） | ✅ 完整 src→target 迁移表守卫 | ✅ 仅校验 src==='SUBMITTED'/'APPROVED'（基础迁移守卫齐全） |
| `validateBusinessRules*`（业务规则） | ✅ requireSupplierActive + requireLinesNonEmpty + 三单匹配（invoice） + 期间校验 | ❌ **不校验业务规则**——可在供应商已停用/行已空时迁移审批状态 |
| `doApprove` 触发后续业务 | ✅ 承付 commit + 库存写 + 过账 + AVL 暂停 + intercompany | ❌ **不触发任何后续业务**（Quotation/Rfq 是寻源前置，approve 无下游副作用——这是合法的，但需 owner doc 显式声明） |
| `doReverseApprove` 目标态 | ✅ APPROVE_STATUS_REJECTED（owner doc §2 合规） | ⚠️ **Quotation/Rfq 设 SUBMITTED**（违反 owner doc §2 强制 REJECTED，P1-MA2-049） |
| `doReverseApprove` 清审计字段 | ✅ 清 approvedBy/At | ✅ 清 approvedBy/At（一致） |
| `doCancel` docStatus=CANCELLED | ✅ + release 承付 + intercompany 红冲 + 凭证 reverse（如已过账） | ⚠️ Quotation.cancel 仅设 docStatus 不触及 approveStatus；Rfq 无 cancel 方法 |

### 2.3 业务轴派生状态（owner doc 声明为派生，非工作流）

| 派生状态 | 持有实体 | 计算逻辑 | 写入点 | 一致性裁决 |
|---------|---------|---------|--------|---------|
| `paidStatus`（UNPAID/PARTIAL/PAID） | Invoice | Σ PaymentLine.amount / invoice.totalAmountWithTax | PaymentSettler.recomputeInvoicePaid:161-176 | ✅ 派生状态正确——经聚合回写，反向核销生成负金额行自然回退 |
| `receiveStatus`（UNRECEIVED/PARTIAL/RECEIVED） | Receive（自身） + Order（滚动汇总） | Receive: receive approve→RECEIVED；Order: Σ approved Receive lines / order line qty | ErpPurReceiveProcessor.approve:83 + orderBiz.updateReceiveStatus:303 | ✅ 滚动汇总经 `findApprovedReceives` 过滤 APPROVED，逻辑正确（并发竞态交接 A2.17） |
| `writtenOffStatus`（复用 paid-status 字典） | Payment | Σ settled / totalAmount | PaymentSettler.recomputePaymentWrittenOff:179-193 | ⚠️ 字典复用——UNPAID/PARTIAL/PAID 语义对应"未核销/部分核销/已核销"功能等价但命名误导（P2-MA2-055） |
| `standing`（GREEN/YELLOW/RED） | SupplierScorecard | ScorecardCalculator 公式计算 | finalizeScorecard:49 | ✅ 终态后联动 AVL SUSPENDED（standing=RED）经 ScorecardStandingLinker 单事务 |
| `isAccepted`（布尔） | Quotation | 比价中标后置 | 无显式 setStatus——可能经 BizModel 路径或手工设置 | ⚠️ 无独立状态机——比价中标是动作而非等待点（owner doc requisition.md §供应商报价 状态机图 ACCEPTED 是显式状态，实现以布尔承载——清晰性缺陷，P2 watch-only 已在 P2-MA2-053 涵盖） |

### 2.4 终态可达性

- **审核轴终态**：`APPROVED`（审核轴无出边，纠错需 reverseApprove→REJECTED 显式路径）
- **业务轴终态**：`docStatus=CANCELLED`（不可恢复，需重新创建）+ `scorecard.status=FINALIZED`（守卫 ERR_SCORECARD_ALREADY_FINALIZED 拒已 FINALIZED）
- **唯一"回退到可修改态"路径**：`APPROVED→REJECTED`（reverseApprove），需冲销前置；目标态 REJECTED 非 UNSUBMITTED（owner doc §16.4 + state-machine.md §2/§3/§5 强制规则）
- **REJECTED→SUBMITTED→APPROVED 回环**：合法循环，退出条件是"审核通过→APPROVED 终态"
- **withdrawApproval→UNSUBMITTED→submit→SUBMITTED→approve→APPROVED 回环**：合法循环，但 INLINE withdrawApproval 缺 isCancelled 守卫（P1-MA2-050）

---

## 3. 10 维度审查裁决

> 维度编号对齐 `state-machine-business-review-prompt.md`。

### 维度 1：状态定义（裁决：**PASS**）

| 控制点 | 裁决 | 证据 |
|--------|------|------|
| 三轴组合语义（docStatus CANCELLED × approveStatus APPROVED 合法性） | PASS | CANCELLED 是 docStatus 终态，approveStatus APPROVED 是审核轴终态——二者独立演化，"已审核但已作废"语义合法（业务上经"审核后作废"路径产生，作废须先冲销已生成结果，owner doc state-machine.md §3 显式定义） |
| payment writtenOffStatus 复用 paid-status 字典语义匹配 | ⚠️ | UNPAID/PARTIAL/PAID 三态语义在 payment 侧对应"未核销/部分核销/已核销"——功能等价但命名"PAID"对 payment 侧误导（应在 payment 侧用"WRITTEN_OFF"语义），登记 P2-MA2-055 watch-only |
| receive receiveStatus 与 order receiveStatus 滚动汇总一致性 | PASS | `ErpPurReceiveProcessor.approve:235-303` 经 `findApprovedReceives` 过滤 APPROVED receives + 按 orderLineId 聚合数量 → `orderBiz.updateReceiveStatus` 写 order.receiveStatus；逻辑正确 |
| quotation isAccepted 布尔（无状态机——是否够） | ⚠️ | owner doc requisition.md §供应商报价 状态机图声明 DRAFT→SUBMITTED→ACCEPTED/REJECTED 状态机，实现以 `isAccepted` 布尔承载——清晰性缺陷。但实际审批轴 approveStatus 已覆盖 DRAFT/SUBMITTED/APPROVED/REJECTED 4 态，isAccepted 仅表达"中标"子状态——功能上够用。登记 P2 watch-only（在 P2-MA2-053 涵盖） |
| scorecard standing vs status 双轴 | PASS | standing（GREEN/YELLOW/RED 业务评级）+ status（DRAFT/FINALIZED 生命周期）双轴独立演化，FINALIZED 后 standing 不可变（守卫 ERR_SCORECARD_ALREADY_FINALIZED），语义清晰 |

### 维度 2：转换完整性（裁决：**FAIL**——三处 P1）

| 控制点 | 裁决 | 证据 |
|--------|------|------|
| **三种并行模式等价性**（PROC 全守卫 vs INLINE 缺守卫） | ❌ FAIL | 同一动作两路径行为不一致——INLINE 路径缺 `validateNotCancelled`/`requireSupplierActive`/`requireLinesNonEmpty` 守卫（见 §2.2 矩阵）。**登记 P1-MA2-050**（CANCELLED 单据 approveStatus 副轴漂移）+ P2-MA2-053（owner doc 未声明三模式） |
| **reverseApprove 目标态矛盾** | ❌ FAIL | Quotation/Rfq `reverseApprove` xbiz 设 SUBMITTED（`ErpPurQuotation.xbiz:97` / `ErpPurRfq.xbiz:97`），违反 owner doc `state-machine.md §2 L46/L52/L66/L83 + §3 L105` + `domain-design-guidelines.md §16.4` 强制 REJECTED 规则；大 Processor 路径全部合规（`ErpPurOrderProcessor.doReverseApprove:347-352` / Receive/Invoice/Payment/Return/Requisition 大 Processor 全设 REJECTED）。**登记 P1-MA2-049**（契约漂移） |
| INLINE reject/withdrawApproval 缺守卫 | ❌ FAIL | Receive/Invoice/Payment/Return/Requisition 的 INLINE reject 仅校验 `status==='SUBMITTED'` 后设 REJECTED，不校验 `isCancelled`——CANCELLED 单据（docStatus=CANCELLED）若 approveStatus 仍 SUBMITTED（取消前置未完整清审批），可被 reject 设 REJECTED（副轴漂移）。同 P1-MA2-050 |
| convertToOrder 前置（APPROVED + 未已转 + 行非空 + 供应商一致） | PASS | `ErpPurRequisitionProcessor.convertToOrder:98-106` + 守卫 `validateApprovedForConversion:163` / `validateNotAlreadyConverted:197` / `validateConsistentSupplier` / `validateLinesNonEmptyForConversion` 齐全 |
| settle/reverseSettlement 前置（双 APPROVED + 供应商匹配 + 余额不超） | ⚠️ | `PaymentSettler.settle:55-111` 守卫齐全（payment approveStatus=APPROVED + invoice approveStatus=APPROVED + supplier 匹配 + amount ≤ invoiceBalance + amount ≤ paymentRemaining），**但不复核 invoice 三单匹配完成态**（P1-MA2-003 已登记，状态机角度复核维持 P1 不升 P0——APPROVED 是 settle 守卫的必要不充分条件，三单匹配完成态复核缺口不破坏 settle 路径正确性，仅缺二次门禁） |
| **Payment 双路径可达 APPROVED** | PASS | (a) Processor.approve 路径：`ErpPurPaymentProcessor.approve` 经 `ErpPurPaymentApproveProcessor` 设 APPROVED；(b) 工作流路径：`ErpPurPayment.xbiz:submitForApproval` 启动 nopFlowId + `TestErpPurPaymentWorkflowApproval` 覆盖工作流审批路径。两条路径行为一致（doApprove→APPROVED + tryPost PAYMENT），由 `nopFlowId` 区分实例 |
| receive approve→order receiveStatus 滚动汇总 | PASS | 见维度 1 证据 |

### 维度 3：终端状态和恢复（裁决：**PASS**）

| 控制点 | 裁决 | 证据 |
|--------|------|------|
| docStatus CANCELLED 终态（不可恢复） | PASS | 无任何 CANCELLED→其他态迁移代码；owner doc state-machine.md §3 + returns.md §退货单状态机 一致声明 |
| approveStatus REJECTED 可重新 submit | PASS | submit 守卫允许 UNSUBMITTED + REJECTED 作为 src（`ErpPurOrderProcessor.validateTransitionForSubmit:145-148` / Quotation/Rfq INLINE `if (status !== 'UNSUBMITTED' && status !== null && status !== 'REJECTED')`） |
| reverseApprove 红冲恢复（posted=false + APPROVED→REJECTED——非真终态可再审批） | PASS（PROC）/ ❌（INLINE） | PROC 路径：doReverseApprove 设 REJECTED + 清 approvedBy/At + posted=false + 凭证 reverse（Invoice/Payment/Return）——REJECTED 可经 submit 重新推进至 APPROVED（合法循环）。INLINE 路径（Quotation/Rfq）：设 SUBMITTED——同维度 2 P1-MA2-049 |
| scorecard FINALIZED 终态（守卫拒已 FINALIZED） | PASS | `ErpPurSupplierScorecardBizModel.finalizeScorecard:44-47` 守卫 `ERR_SCORECARD_ALREADY_FINALIZED` 拒已 FINALIZED |

### 维度 4：异常路径（裁决：**FAIL**——一处 P1）

| 控制点 | 裁决 | 证据 |
|--------|------|------|
| **INLINE reject CANCELLED 单据**（无取消守卫） | ❌ FAIL | INLINE 路径无 `validateNotCancelled`，CANCELLED 单据的 SUBMITTED approveStatus 可被 reject 设为 REJECTED——副轴漂移。**同 P1-MA2-050**。**实际危害有限**：(1) docStatus=CANCELLED 是主终态，approveStatus 副轴漂移不影响业务查询（按 docStatus=CANCELLED 过滤即可）；(2) settle/过账查询都校验 approveStatus=APPROVED，CANCELLED+REJECTED 不会被误纳入；(3) 不产生脏数据（仅审计轨迹混淆）。故裁决 P1 非 P0 |
| approve 已 CANCELLED 单据 | PARTIAL PASS | PROC 路径 `validateNotCancelled` 拒绝；INLINE 路径（Quotation/Rfq）无守卫。Quotation/Rfq approve 前置 src==='SUBMITTED'——若 CANCELLED 单据仍 SUBMITTED 可被 approve 设 APPROVED。实际危害有限（同上） |
| settle 超余额 | PASS | `PaymentSettler.settle:82-94` 守卫 `ERR_SETTLE_OVER_INVOICE_BALANCE` / `ERR_SETTLE_OVER_PAYMENT_BALANCE` 拒绝 |
| convertToOrder 已转 | PASS | `validateNotAlreadyConverted:197` 守卫拒绝 |
| 三单匹配超容差（P1-MA2-003 settle 不复核匹配完成态） | ⚠️ | settle 仅校验 invoice approveStatus=APPROVED 不复核三单匹配完成态。**维持 P1-MA2-003 不升 P0**——APPROVED 是 settle 守卫的必要不充分条件，三单匹配缺口不破坏 settle 路径正确性 |
| 过账 tryPost 吞异常（posted=false 悬挂） | ⚠️ | `PurInvoicePostingDispatcher.tryPost:39-52` / `PurPaymentPostingDispatcher.tryPost` / `PurReturnPostingDispatcher.tryPost` try/catch 吞所有异常返回 boolean——失败时业务侧 posted=false 永久悬挂。**与 finance A2.5a P1-MA2-032 IGNORED 悬挂 + mfg/hr posting dispatcher tryPost 容错同型根因**，按既定裁决范式 P1。已登记 deferred posting sweep job 兜底，不升 P0 |
| **PurReversalListener.rollbackReceive 不对称** | ❌ FAIL | `PurReversalListener.rollbackReceive:112-123` 仅设 posted=false，**保留 APPROVED**（Javadoc:117-118 标注 deliberate：库存物理冲销独立于凭证红冲，财务侧红冲仅回退 posted 标志保留 APPROVED 审计轨迹），与 `rollbackInvoice/Payment/Return:70-110` 全部降级 APPROVED→REJECTED 不对称。**登记 P1-MA2-051**——若 finance 红冲 receive 的 PURCHASE_INPUT 凭证后，receive 保持 APPROVED+posted=false 悬挂：receive 不能再 approve（已 APPROVED），不能 reverseApprove（需手工触发），需运营人工处置。Javadoc 标注 deliberate 但与同型实体不对称，裁决 P1（功能性悬挂数据需运营介入，非数据破坏） |
| receive 超收 | PASS | `ErpPurReceiveProcessor` 守卫 `requireSourceOrderApproved` + 数量校验（超收容差由全局配置） |
| payment settle 供应商不匹配 | PASS | `PaymentSettler.requireInvoiceForSettle:147-151` 守卫 `ERR_SETTLE_SUPPLIER_MISMATCH` 拒绝 |

### 维度 5：可达性（裁决：**FAIL**——一处 P1）

| 控制点 | 裁决 | 证据 |
|--------|------|------|
| **reverseApprove 经 INLINE 可达 SUBMITTED（Quotation/Rfq）vs PROC 可达 REJECTED（其他）** | ❌ FAIL | 同一概念 reverseApprove 经两路径可达两不同态——契约不一致（owner doc §2 强制 REJECTED，Quotation/Rfq 违规→SUBMITTED）。**同 P1-MA2-049** |
| withdrawApproval→UNSUBMITTED→submit→SUBMITTED→approve→APPROVED 回环可达性 | PASS | 各迁移前置齐全（src 状态匹配），合法循环退出条件是 APPROVED 终态 |
| scorecard FINALIZED 后回 DRAFT 不可达（终态） | PASS | 无任何 FINALIZED→DRAFT 迁移；finalizeScorecard 守卫拒已 FINALIZED |
| 死循环或不可达终态 | PASS | 无不可达状态（除 dict 项无代码写入——本域无 dict 死状态，与 hr/mfg 不同）；合法循环均有退出条件 |

### 维度 6：角色和权限（裁决：**PASS**）

| 控制点 | 裁决 | 证据 |
|--------|------|------|
| 提交（采购员）/审核（采购主管）/settle（出纳/会计）/convertToOrder（采购员）/scorecard finalize（供应商管理）角色绑定 | PASS | 各 @BizMutation 经 nop-auth 权限模型绑定角色（不在本审计范围——A4.x 平台合规已覆盖）；@BizMutation 自动事务回滚保证失败原子性 |
| 危险操作：approve 触发承付 commit/库存写/过账跨域会计写 | PASS | Order.approve → runCommitmentCommitHook（config-gated）+ intercompany（config-gated）；Receive.approve → IErpInvStockMoveBiz（跨域写库存）；Invoice/Payment/Return.approve → IErpFinVoucherBiz.post（跨域写会计保护区域，经 REQUIRES_NEW Facade）； Scorecard.finalize → IErpMdSupplierApprovalBiz.suspendByPartner（跨域写 AVL）——全部经 I*Biz Facade，无 daoFor 跨域写（已确认） |
| 危险操作：settle 资金核销 | PASS | PaymentSettler.settle 双 APPROVED 守卫 + 余额校验 |
| 危险操作：reverseApprove 红冲恢复余额 | PASS | PROC 路径 doReverseApprove 设 REJECTED + posted=false + 凭证 reverse（Invoice/Payment/Return）；承付 release hook（config-gated）+ intercompany 红冲（config-gated） |
| 危险操作：cancel 已过账单据（须 reverse 凭证） | PASS | doCancel 前 `runCommitmentReleaseHook` + `runIntercompanyReverseHook` + 凭证 reverse（Invoice/Payment/Return） |
| 多角色冲突（采购员 approve vs 出纳 settle vs 会计 reverseApprove） | PASS | 职责分离经 @BizMutation 入口 + 权限模型保证（不在本审计范围） |

### 维度 7：外部依赖（裁决：**PASS**）

| 控制点 | 裁决 | 证据 |
|--------|------|------|
| approve→承付 commit/release（IErpFinBudgetCommitmentBiz config-gated） | PASS | `ErpPurOrderProcessor.runCommitmentCommitHook:223-237` / `runCommitmentReleaseHook:248-258`（容错对称性已 fix，catch NopException 静默跳过） |
| receive·return→库存移动单（IErpInvStockMoveBiz 跨域写） | PASS | `ErpPurReceiveProcessor.triggerIncomingMove` + `ErpPurReturnProcessor` 出库——经 I*Biz Facade |
| invoice·payment·return→过账（IErpFinVoucherBiz 跨域写会计保护区域） | PASS | `PurPostingExecutor` → `IErpFinVoucherBiz.post/reverse` REQUIRES_NEW Facade——经 I*Biz Facade |
| scorecard RED→供应商暂停（IErpMdSupplierApprovalBiz.suspendByPartner 写） | PASS | `ScorecardStandingLinker.onScorecardRed:26` 经 I*Biz Facade 写 master-data AVL |
| **PurReversalListener 反向**（finance→purchase 回滚——onVoucherReversed） | ⚠️ | 监听者失败经 `ErpFinReversalListenerRegistry.dispatch` try/catch 隔离，不阻断其他域监听者；失败落入 finance 异常工作台。回退目标态表见维度 4——rollbackReceive 不对称（P1-MA2-051），其他三实体（Invoice/Payment/Return）回退对称 |
| 外部步骤失败是否阻断状态迁移 | PASS | @BizMutation 事务回滚保证 approve 触发的库存写/承付/过账跨域写失败时业务单据回滚至 SUBMITTED；过账 tryPost 吞异常路径（posted=false 悬挂）是设计容错（与 finance P1-MA2-032 同型） |

### 维度 8：TODO/任务策略（裁决：**PASS**）

| 控制点 | 裁决 | 证据 |
|--------|------|------|
| SUBMITTED 待审核 TODO | PASS | 各实体 SUBMITTED 状态产生审批 TODO（经 nop-wf 工作流或审批池分配，不在本审计范围） |
| receive PARTIAL 补收 TODO | PASS | receive PARTIAL 是 receiveStatus 派生态，由 order.receiveStatus 滚动汇总表达；order 未全收时产生补收 TODO（不在本审计范围） |
| invoice 超容差三单匹配争议 TODO | PASS | invoice 超容差经 `ThreeWayMatcher` 拒绝审核保持 SUBMITTED，由审核人决策（不在本审计范围） |
| payment UNPAID/PARTIAL 付款 TODO | PASS | invoice paidStatus PARTIAL 是派生态，由 PaymentSettler.recomputeInvoicePaid 计算；未付清时产生付款 TODO（不在本审计范围） |
| 是否存在期望有人行动但不产生待办的状态 | PASS | reverseApprove 后 REJECTED 是合法的"等待修改后重新提交"等待点，产生 assigned TODO；posted=false 悬挂（过账 tryPost 失败）由 DeferredPostingSweepJob 兜底扫描（不属本审计） |

### 维度 9：场景演练（最重要，裁决：**FAIL**——三处 P1 在场景中暴露）

> 12 个代表性场景，覆盖 owner doc state-machine.md §9 + 本审计识别风险点。

#### 场景 (a) P2P 黄金路径（裁决：**PASS**）

请购→approve→convertToOrder→订单 approve→收货 approve→发票 approve+过账→付款 approve+settle+过账：
- Requisition SUBMITTED→INLINE approve→APPROVED（无下游副作用——寻源前置，合法）
- convertToOrder 守卫齐全→生成 ErpPurOrder UNSUBMITTED
- Order SUBMITTED→PROC approve→APPROVED + 承付 commit（config-gated）
- Receive SUBMITTED→PROC approve→APPROVED + receiveStatus=RECEIVED + 库存 incoming + 暂估应付 PURCHASE_INPUT 过账 + order.receiveStatus 滚动汇总
- Invoice SUBMITTED→PROC approve→APPROVED + AP_INVOICE 过账
- Payment SUBMITTED→PROC+wf approve→APPROVED + PAYMENT 过账 + settle 核销（双 APPROVED + 余额校验）→ invoice paidStatus=PAID

**全链状态迁移守卫齐全，跨域写经 I*Biz Facade，事务回滚保证原子性。PASS。**

#### 场景 (b) INLINE reject 路径（裁决：**FAIL**——P1-MA2-050）

Receive/Invoice/Payment/Return reject 经 INLINE 路径：
- 守卫仅 `status==='SUBMITTED'` 后设 REJECTED
- **缺 isCancelled 守卫**——若单据 docStatus=CANCELLED 但 approveStatus=SUBMITTED（取消前置未完整清审批），可被 reject 设 REJECTED（副轴漂移）
- **缺 requireSupplierActive 守卫**——可在供应商已停用时迁移审批状态（实际危害有限——单据已 SUBMITTED 时供应商已校验过，停用发生在 SUBMITTED 与 reject 之间的窗口期狭窄）
- PROC 路径（Order reject）有 `validateNotCancelled` + `validateTransitionForReject` 守卫齐全

**两种路径行为不一致——P1-MA2-050。**

#### 场景 (c) reverseApprove 红冲（裁决：**FAIL**——P1-MA2-049）

PROC 路径（Order/Receive/Invoice/Payment/Return/Requisition）：
- doReverseApprove 设 REJECTED + 清 approvedBy/At + posted=false + 凭证 reverse（Invoice/Payment/Return）
- 承付 release hook（config-gated）+ intercompany 红冲（config-gated）
- 与 owner doc §2 强制 REJECTED 规则一致 ✓

INLINE 路径（Quotation/Rfq）：
- 设 SUBMITTED + 清 approvedBy/At
- **违反 owner doc §2 强制 REJECTED 规则** ⚠️
- 但 Quotation/Rfq 无 posted 副作用（不过账），不破坏红冲闭环一致性（无凭证需 reverse）
- 实际危害：契约漂移——审查者期望 reverseApprove 后处于 REJECTED（"曾审核过"语义），实际处于 SUBMITTED（"重新提交中"语义）

**契约漂移，按 finance A2.5a P1-MA2-031 + mfg A2.6a P1-MA2-035 + hr A2.7a P1-MA2-039~042 同型裁决 P1-MA2-049。**

#### 场景 (d) withdrawApproval 回环（裁决：**PARTIAL FAIL**——同 P1-MA2-050）

APPROVED→（reverseApprove）→REJECTED→（submit）→SUBMITTED→（withdraw）→UNSUBMITTED→（submit）→SUBMITTED→（approve）→APPROVED：
- PROC 路径 withdraw 守卫齐全
- INLINE 路径 withdraw 缺 isCancelled 守卫（同 P1-MA2-050）

#### 场景 (e) cancel 已过账单据（裁决：**PASS**）

Invoice/Payment/Return 已过账后 cancel：
- doCancel 前 runCommitmentReleaseHook + runIntercompanyReverseHook + 凭证 reverse
- 失败经 @BizMutation 事务回滚，cancel 不生效
- Order.approve 无直接过账副作用，cancel 仅置 docStatus=CANCELLED + release 承付

#### 场景 (f) settle/reverseSettlement（裁决：**PASS**）

- settle：双 APPROVED + 供应商匹配 + 余额校验齐全
- reverseSettlement：生成负金额 PaymentLine，余额与状态据此自然回退
- **不复核 invoice 三单匹配完成态**（P1-MA2-003 维持不升 P0）

#### 场景 (g) Payment 工作流路径（裁决：**PASS**）

- submitForApproval 经 Processor 设 SUBMITTED + 启动 nopFlowId（xmeta 配 wf:wfName 时）
- 工作流审批路径与 Processor.approve 路径都可达 APPROVED
- TestErpPurPaymentWorkflowApproval 覆盖工作流路径
- 两条路径行为一致（doApprove→APPROVED + tryPost PAYMENT）

#### 场景 (h) PurReversalListener rollback（裁决：**FAIL**——P1-MA2-051）

finance 红冲已过账凭证→purchase 回滚：
- rollbackInvoice/Payment/Return：posted=false + APPROVED→REJECTED（对称降级）
- **rollbackReceive：仅 posted=false 保留 APPROVED**（不对称，Javadoc 标注 deliberate：库存物理冲销独立于凭证红冲）
- 实际危害：receive 保持 APPROVED+posted=false 悬挂——不能再 approve（已 APPROVED），不能 reverseApprove（需手工触发），需运营人工处置

**不对称——P1-MA2-051。Javadoc 标注 deliberate 但与同型实体不一致，裁决 P1（功能性悬挂数据需运营介入，非数据破坏）。**

#### 场景 (i) convertToOrder（裁决：**PASS**）

请购 APPROVED→convertToOrder：
- 守卫 validateApprovedForConversion + validateNotAlreadyConverted + validateConsistentSupplier + validateLinesNonEmptyForConversion 齐全
- 生成 ErpPurOrder UNSUBMITTED，独立审批流

#### 场景 (j) scorecard finalize（裁决：**PASS**）

DRAFT→finalizeScorecard→FINALIZED + standing=RED→AVL SUSPENDED：
- ScorecardCalculator 公式计算 totalScore/standing
- 守卫 ERR_SCORECARD_ALREADY_FINALIZED 拒已 FINALIZED
- standing=RED 经 ScorecardStandingLinker 跨域写 AVL SUSPENDED（单事务）

#### 场景 (k) 三单匹配超容差 settle（裁决：**PASS**——维持 P1-MA2-003）

- 非严格默认模式（match=warn+放行）下价格严重超容差发票 APPROVED 后付款核销无二次门禁
- settle 仅校验 invoice approveStatus=APPROVED 不复核三单匹配完成态
- **维持 P1-MA2-003 不升 P0**——APPROVED 是 settle 守卫的必要不充分条件，三单匹配缺口不破坏 settle 路径正确性，仅缺二次门禁

#### 场景 (l) 并发 settle 同发票（裁决：**PASS**——交接 A2.17）

- PaymentSettler 无悲观/乐观锁，并发核销同一发票可双读双写过付
- recomputeInvoicePaid 事后聚合不能阻止中间态过付
- **P2-MA2-008 已登记，归 A2.17 并发与乐观锁系统性审计**

### 维度 10：与设计文档一致性（裁决：**FAIL**——三处 owner doc 漂移）

| 控制点 | 裁决 | 证据 |
|--------|------|------|
| **§2 reverseApprove→REJECTED 强制规则被 Quotation/Rfq xbiz 违反** | ❌ FAIL | owner doc `state-machine.md §2 L46/L52/L66/L83 + §3 L105` + `domain-design-guidelines.md §16.4` 多处强制 reverseApprove→REJECTED；Quotation/Rfq xbiz 设 SUBMITTED。**同 P1-MA2-049** |
| **三种并行模式 owner doc 未声明** | ❌ FAIL | owner doc `state-machine.md` 假设单一审批状态机，未声明 PROC/INLINE/平台三模式并存——审查者/开发者期望单一模式行为一致，实际 INLINE 缺守卫。**登记 P2-MA2-053** watch-only（owner doc 未声明）+ P1-MA2-050（实际安全缺口） |
| **INLINE reject/withdrawApproval 缺守卫 owner doc 是否声明** | ❌ FAIL | owner doc `state-machine.md §2 迁移表` 声明迁移前置（如"已提交状态"），未声明 INLINE 路径缺 isCancelled/requireSupplierActive 守卫。**同 P1-MA2-050 + P2-MA2-053** |
| **PurReversalListener rollbackReceive 不对称 owner doc 是否声明** | ❌ FAIL | owner doc `returns.md §暂估应付冲减` 仅在退货链实现冲回，未声明 PURCHASE_INPUT 凭证红冲后 receive 状态回退目标；`state-machine.md` 无 PurReversalListener 章节描述回退目标态表。**同 P1-MA2-051**（Javadoc 标注 deliberate 但 owner doc 未同步） |
| payment writtenOffStatus 复用 paid-status 语义 owner doc 是否声明 | ⚠️ | owner doc `state-machine.md §三轴状态分离` 表列出 paidStatus 适用于"采购发票"，未声明 payment 复用同一字典承载 writtenOffStatus 语义。**登记 P2-MA2-055** watch-only |
| 死代码 WithdrawApproval/Reject Processor owner doc 是否声明未接线 | ⚠️ | Order/Receive/Invoice/Payment/Return/Requisition 均存在 `*WithdrawApprovalProcessor` + `*RejectProcessor` Java 类（继承 common 抽象基），但 INLINE 实体的 xbiz 未引用——纯死代码或经 Delta beans.xml 接线。**登记 P2-MA2-054** watch-only（未确认接线方式） |

---

## 4. 已登记 finding 采购状态机角度运行时复核

| Finding ID | 原描述 | 状态机角度复核 | 终态 |
|-----------|--------|--------------|------|
| `P1-MA1-022`（todo MR1，9 域合并） | pur `daoFor(ErpMdSubject/ErpFinAccountingPeriod)` 只读（OrderProcessor:302,314 + PaymentProcessor:228,240） | 跨域只读是 budget/period 查询副作用，不破坏状态机——异常路径经 @BizMutation 事务回滚覆盖 | **不升级**（维持 P1 治理待 MR1） |
| `P1-MA2-001`（todo MR1，P2P） | 暂估应付冲回缺失 | 状态机角度 receive→invoice 两单均 APPROVED+posted=true 状态迁移正确，漂移在 GL 层非状态机层 | **不升级**（GL 层 finding，状态机角度无影响） |
| `P1-MA2-002`（todo MR1，P2P） | 多币种 P2P 本位币凭证路径未验证 | 状态迁移不涉及币种——状态机角度无影响 | **不升级**（GL 层 finding，状态机角度无影响） |
| `P1-MA2-003`（todo MR1，P2P） | 付款核销缺三单匹配完成态复核 | **状态机守卫缺口**——PaymentSettler.settle 仅校验 invoice approveStatus=APPROVED 不复核三单匹配完成态。**复核裁决**：APPROVED 是 settle 守卫的必要不充分条件，三单匹配完成态复核缺口不破坏 settle 路径正确性，仅缺二次门禁 | **维持 P1 不升 P0**（必要不充分守卫缺口） |
| `P2-MA2-008`（todo MR1→A2.17） | PaymentSettler.settle 无锁并发核销同一发票可双读双写过付 | 并发 SETTLED 漂移——状态机角度观察并发敏感点 | **交接 A2.17** |
| `P2-MA1-026`（todo MR1） | scorecard-status defaultValue 残留 int「10」 | defaultValue 漂移非状态迁移——状态机角度无影响 | **不升级**（ defaultValue 缺陷，状态机角度无影响） |

---

## 5. 新登记 finding

### 5.1 P1 finding（3 项，目标 MR1）

| Finding ID | 描述 | 严重性 | 修复方式 |
|-----------|------|-------|---------|
| `P1-MA2-049` | **Quotation/Rfq reverseApprove→SUBMITTED 违反 owner doc §2 强制 REJECTED 契约漂移**：`ErpPurQuotation.xbiz:97` + `ErpPurRfq.xbiz:97` reverseApprove 设 `entity.approveStatus = 'SUBMITTED'`，违反 owner doc `state-machine.md §2 L46/L52/L66/L83 + §3 L105` + `domain-design-guidelines.md §16.4` 强制 REJECTED 规则。所有大 Processor 合规（doReverseApprove 设 APPROVE_STATUS_REJECTED），但 Quotation/Rfq 无大 Processor，xbiz 直设 SUBMITTED。**不破坏红冲闭环一致性**（Quotation/Rfq 无 posted 副作用——不过账，无凭证需 reverse），是寻源前置实体。按 finance A2.5a P1-MA2-031 + mfg A2.6a P1-MA2-035 + hr A2.7a P1-MA2-039~042 同型裁决（owner doc 强制规则 + xbiz 实现漂移，不破坏主路径）。 | major（契约漂移，不破坏业务路径） | MR1 裁决——方案 A（推荐）xbiz `reverseApprove` 改设 `entity.approveStatus = 'REJECTED'`（与大 Processor 对齐 + owner doc §2 合规）；方案 B 引入 Quotation/Rfq 大 Processor 全守卫（与 Order/Receive 等同型，工作量大，与 P1-MA2-050 一并裁决） |
| `P1-MA2-050` | **INLINE reject/withdrawApproval 绕过 isCancelled/requireSupplierActive/requireLinesNonEmpty 守卫致 CANCELLED 单据 approveStatus 副轴漂移**：Receive/Invoice/Payment/Return/Requisition 的 INLINE reject xbiz（`ErpPurReceive.xbiz:35-58` 等）+ 全实体 INLINE withdrawApproval xbiz（`ErpPurOrder.xbiz:45-67` 等）+ Quotation/Rfq 全 INLINE 动作（`ErpPurQuotation.xbiz`/`ErpPurRfq.xbiz` 全文）——均仅校验 `status==='SUBMITTED'` 后设新状态，**缺失** PROC 路径的 `validateNotCancelled`/`requireSupplierActive`/`requireLinesNonEmpty` 守卫。CANCELLED 单据（docStatus=CANCELLED）的 SUBMITTED approveStatus 可被 reject 设为 REJECTED（副轴漂移）。**实际危害有限**：(1) docStatus=CANCELLED 是主终态，approveStatus 副轴漂移不影响业务查询（按 docStatus=CANCELLED 过滤即可）；(2) settle/过账查询都校验 approveStatus=APPROVED，CANCELLED+REJECTED 不会被误纳入；(3) 不产生脏数据（仅审计轨迹混淆）。 | major（安全缺口，但危害有限——主终态 docStatus 持有，副轴漂移不破坏业务路径） | MR1 裁决——方案 A（推荐）将 INLINE reject/withdrawApproval 迁移到对应 Processor（`ErpPurReceiveRejectProcessor` 等死代码类已存在，仅需 xbiz 改 `inject('...Processor').reject(id, svcCtx)` 接线 + Delta beans.xml 注册），全守卫对齐；方案 B INLINE 路径补 `isCancelled` 守卫（最小变更：xbiz 脚本前加 `if (entity.docStatus === 'CANCELLED') throw ...`）；方案 C owner doc 标注「INLINE 路径无取消守卫，CANCELLED 单据的 approveStatus 漂移不影响业务」（永久接受） |
| `P1-MA2-051` | **PurReversalListener.rollbackReceive 不对称致冲销后 receive APPROVED+posted=false 悬挂**：`PurReversalListener.rollbackReceive:112-123` 仅设 posted=false 保留 APPROVED（Javadoc:117-118 标注 deliberate：库存物理冲销独立于凭证红冲，财务侧红冲仅回退 posted 标志保留 APPROVED 审计轨迹），与 `rollbackInvoice/Payment/Return:70-110` 全部降级 APPROVED→REJECTED 不对称。若 finance 红冲 receive 的 PURCHASE_INPUT 凭证后，receive 保持 APPROVED+posted=false 悬挂——不能再次 approve（已 APPROVED），不能 reverseApprove（需手工触发），需运营人工处置。**不破坏业财一致**（凭证已红冲，GL 平衡；库存物理冲销独立；仅 purchase 域 receive 状态悬挂）。 | major（功能性悬挂数据需运营介入，非数据破坏） | MR1 裁决——方案 A（推荐）rollbackReceive 与其他三实体对齐：`if (approveStatus == APPROVED) setApproveStatus(REJECTED)` + 更新 owner doc `returns.md`/`state-machine.md` 描述回退目标态表（receive 也降级 REJECTED）；方案 B owner doc 标注「rollbackReceive 仅 posted=false 保留 APPROVED 是设计并行（库存物理冲销独立），receive 悬挂经运营手工 reverseApprove 处置」（永久接受 deliberate 不对称） |

### 5.2 P2 finding（3 项 watch-only）

| Finding ID | 描述 | 处置 |
|-----------|------|------|
| `P2-MA2-053` | **三种并行模式 owner doc 未声明**：owner doc `state-machine.md` 假设单一审批状态机，未声明 PROC（大 Processor 全守卫）/INLINE（xbiz 脚本直设仅基础校验）/平台（nop-wf 工作流）三模式并存——审查者/开发者期望单一模式行为一致，实际 INLINE 缺守卫。与 P2-MA2-047/052 同型（owner doc 缺独立章节/未声明）。 | watch-only，MR1 顺手——方案 A（推荐）`state-machine.md` 新增「§实现模式」章节声明三模式 + INLINE 模式的守卫边界；方案 B 交叉链接到 `processor-extension-pattern.md` |
| `P2-MA2-054` | **死代码 WithdrawApproval/Reject Processor 未接线**：Order/Receive/Invoice/Payment/Return/Requisition 均存在 `*WithdrawApprovalProcessor` + `*RejectProcessor` Java 类（如 `ErpPurReceiveRejectProcessor.java`/`ErpPurReceiveWithdrawApprovalProcessor.java`），但 INLINE 实体的 xbiz 未 `inject()` 引用——纯死代码或经 Delta beans.xml 接线（本审计未确认）。Quotation/Rfq 无对应 Processor 类（全 INLINE）。 | watch-only，MR1 裁决 P1-MA2-050 时一并处置——若选方案 A（迁移 INLINE 到 Processor），死代码类被启用；若选方案 C（owner doc 标注），死代码类应删除或标注「保留作为 future Processor 接线备用」 |
| `P2-MA2-055` | **payment writtenOffStatus 复用 paid-status 字典语义漂移**：`ErpPurPayment.writtenOffStatus`（orm:941）复用 `erp-pur/paid-status` 字典（UNPAID/PARTIAL/PAID），语义在 payment 侧对应"未核销/部分核销/已核销"——功能等价但命名"PAID"对 payment 侧误导。 | watch-only，MR1 顺手——方案 A 新增 `erp-pur/written-off-status` 字典（UNSETTLED/PARTIAL/SETTLED）+ ORM `ext:dict` 切换（codegen 增量再生）；方案 B owner doc `state-machine.md §三轴状态分离` 表注记「payment writtenOffStatus 复用 paid-status 字典，PAID 在 payment 侧语义=已核销」 |

---

## 6. 并发敏感点（交接 A2.17）

| 序号 | 位置 | 描述 | 处置 |
|-----|------|------|------|
| 1 | `PaymentSettler.settle:55-111` | 「读 invoiceBalance→写 PaymentLine」无悲观/乐观锁，并发核销同一发票可双读双写过付；recomputeInvoicePaid 事后聚合不能阻止中间态过付 | 已登记 P2-MA2-008，归 A2.17 |
| 2 | `ErpPurReceiveProcessor.approve:235-303`（order.receiveStatus 滚动汇总） | 多个 Receive 并发 approve 同一 Order 时，`findApprovedReceives` 读 + `orderBiz.updateReceiveStatus` 写无锁，并发场景下 receiveStatus 滚动汇总可能 stale read | 归 A2.17 |
| 3 | `PurReversalListener` 并发回滚 | finance 红冲 + 域侧 reverseApprove 并发触发同一 receive/invoice/payment/return 时，posted/approveStatus 写入竞态 | 归 A2.17 |
| 4 | `ErpPurOrderProcessor.approve` 双重 approve 幂等 | `if (order.isApproved()) return order;`（:86-88）+ 乐观锁 `@Version`（ErpPurOrder 已声明 versionProp）→ detectable conflict | 已降级（@Version 透明乐观锁） |
| 5 | `finalizeScorecard` 双重 finalize 幂等 | 守卫 `ERR_SCORECARD_ALREADY_FINALIZED`（:44-47）+ @Version → detectable conflict | 已降级（守卫 + @Version） |

> **重要事实**：ErpPurOrder/ErpPurReceive/ErpPurInvoice/ErpPurPayment/ErpPurReturn/ErpPurRequisition/ErpPurSupplierScorecard 均声明 `versionProp`（透明乐观锁），将 silent lost-update 降级为 detectable conflict。ErpPurQuotation/ErpPurRfq 也应声明（本审计未逐项核验 versionProp 列——交 A2.17 系统性审计）。

---

## 7. 综合裁决

### 7.1 Verdict

**⚠️(P1)**——采购九实体状态机核心契约经实仓逐项证据确认（迁移守卫齐全、@BizMutation 事务回滚、reverseApprove 红冲闭环强一致经大 Processor 路径成立、跨域写经 I*Biz Facade）；**零 P0**（三个候选 P0 经证据证伪或降级：(1) Quotation/Rfq reverseApprove→SUBMITTED 违反 owner doc §2 但不破坏红冲闭环一致性，按同型裁决 P1；(2) INLINE reject/withdrawApproval 缺守卫但不破坏主终态（docStatus=CANCELLED 持有），按危害有限 P1；(3) PurReversalListener.rollbackReceive 不对称但 Javadoc deliberate + 不破坏业财一致，按功能性悬挂 P1）；**新增 3 项 P1**（P1-MA2-049/050/051）+ **新增 3 项 P2** watch-only（P2-MA2-053/054/055）；6 项已登记 MA1/MA2 finding 运行时复核**无升级**；并发敏感点 5 处交接 A2.17。

### 7.2 状态机正确性维度 pur 列推进

`❓` → **`⚠️(P1)`**（采购九实体状态机迁移正确性经审计确认 + 3 项 P1 待 MR1：P1-MA2-049 reverseApprove 目标态矛盾 / P1-MA2-050 INLINE 缺守卫 / P1-MA2-051 rollbackReceive 不对称；3 项 P2 watch-only；MA1/MA2 finding 运行时复核无升级；并发敏感点 5 处交接 A2.17）。

### 7.3 残留风险

- **INLINE 路径守卫缺口**（P1-MA2-050）：CANCELLED 单据的 approveStatus 副轴漂移——若未来添加按 approveStatus 过滤的业务查询（如"所有 SUBMITTED 单据"包含 CANCELLED+SUBMITTED），可能产生意外结果。MR1 修复时建议方案 A（迁移到 Processor）。
- **rollbackReceive 不对称**（P1-MA2-051）：receive APPROVED+posted=false 悬挂需运营人工处置——若运营不熟悉该路径，receive 可能长期悬挂。MR1 修复时建议方案 A（与其他三实体对齐降级 REJECTED）。
- **死代码 Processor 类**（P2-MA2-054）：未确认是否经 Delta beans.xml 接线——若实际未接线，是纯死代码；若接线但被 INLINE 覆盖，是配置冗余。MR1 裁决 P1-MA2-050 时一并核验。
- **A2.17 并发审计未覆盖**：本审计仅标注并发敏感点，系统性并发正确性裁决归 A2.17。
- **A4.5 代码质量审计未覆盖**：Processor 代码质量（异常处理/N+1/索引/辅助方法）归 A4.5。
- **A4.7 view.xml drift 未覆盖**：采购页面契约漂移归 A4.7。

### 7.4 范围内已覆盖 / 范围外已交接

| 范围 | 状态 |
|------|------|
| 九实体 × 三轴状态机迁移正确性 | ✅ 已审计 |
| PROC vs INLINE 模式等价性 | ✅ 已审计 |
| reverseApprove 目标态矛盾 | ✅ 已审计（P1-MA2-049） |
| PurReversalListener rollback 不对称 | ✅ 已审计（P1-MA2-051） |
| Payment 双路径 APPROVED | ✅ 已审计 |
| settle/reverseSettlement 守卫 | ✅ 已审计（P1-MA2-003 维持） |
| MA1/MA2 finding 状态机角度复核 | ✅ 已审计（无升级） |
| 并发敏感点 | ⚠️ 标注，交 A2.17 |
| 代码质量 | ❌ 交 A4.5 |
| view.xml drift | ❌ 交 A4.7 |
| P2P GL 正确性 | ❌ 交 A2.1 finding（已 done） |

---

## 8. 参考

- `docs/design/purchase/state-machine.md`（owner doc，三轴设计 + reverseApprove→REJECTED 强制规则）
- `docs/design/purchase/returns.md`（退货状态机 + 暂估应付冲减）
- `docs/design/purchase/requisition.md`（请购→询价→报价→订单）
- `docs/design/purchase/three-way-match.md`（三单匹配 + settle 前置）
- `docs/design/purchase/supplier-evaluation.md`（供应商评级 + standing RED→暂停）
- `docs/architecture/processor-extension-pattern.md`（Facade+Processor 两层）
- `docs/architecture/posting-exemptions.md`（采购过账跨域写豁免登记）
- `docs/skills/state-machine-business-review-prompt.md`（10 维度审查方法）
- `docs/plans/2026-07-28-0230-3-audit-remediation-ma2-purchase-state-machine.md`（本审计 plan）
- 关联审计：`docs/audits/2026-07-27-1949-arm-ma2-procure-to-pay-e2e.md`（A2.1 P2P）/ `2026-07-27-2211-arm-ma2-finance-posting-voucher-state-machine.md`（A2.5a 凭证）/ `2026-07-28-0109-arm-ma2-mfg-work-order-jobcard-state-machine.md`（A2.6a 状态机审查范式）
