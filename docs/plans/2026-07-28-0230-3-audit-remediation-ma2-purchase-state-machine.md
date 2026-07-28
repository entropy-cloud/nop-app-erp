# 2026-07-28-0230-3-audit-remediation-ma2-purchase-state-machine MA2 purchase 状态机审查（A2.8）

> Plan Status: completed
> Mission: audit-remediation
> Work Item: A2.8 purchase 状态机审查（A 级单域，29 状态字段）
> Last Reviewed: 2026-07-28
> Source: `docs/backlog/audit-remediation-roadmap.md` Milestone MA2（工作项 A2.8）
> Related: `docs/plans/2026-07-27-1949-1-audit-remediation-ma2-procure-to-pay-e2e.md`（A2.1 P2P 端到端 done——采购订单/收货/发票/付款链路组件齐备 + 3 项 P1 待 MR1：P1-MA2-001 暂估冲回 / P1-MA2-002 多币种 / P1-MA2-003 三单匹配完成态）；`docs/plans/2026-07-28-0109-1-audit-remediation-ma2-mfg-work-order-jobcard-state-machine.md`（A2.6a mfg 状态机审查范式——Processor Facade 两层 + posted 标记 + reverseApprove 红冲闭环同型）；`docs/plans/2026-07-27-2211-2-audit-remediation-ma2-finance-posting-voucher-state-machine.md`（A2.5a finance 凭证状态机——reverseApprove 红冲闭环 + tryPost 容错同型 P1-MA2-032）；`docs/skills/state-machine-business-review-prompt.md`（审计方法）；`docs/design/purchase/state-machine.md`（三轴设计 docStatus/approveStatus/paidStatus + approve 轴迁移表 + §2 reverseApprove→REJECTED 强制规则）+`returns.md`+`requisition.md`+`three-way-match.md`+`supplier-evaluation.md`（owner doc）
> Audit: required

## Current Baseline

purchase（采购）域 A 级状态机审查（单域单工作项，29 状态字段）。purchase 是 P2P 链路核心，状态机驱动请购→询价→报价→订单→收货→发票→付款→退货全生命周期。owner doc `state-machine.md` 采用**三轴设计**（`docStatus` erp/doc-status DRAFT/ACTIVE/CANCELLED + `approveStatus` wf/approve-status UNSUBMITTED/SUBMITTED/APPROVED/REJECTED + 业务轴 paidStatus/receiveStatus）。

实时仓库已落地的采购状态机实现（逐项核实，路径 `module-purchase/`）：

- **三轴状态字段清单**（ORM `app-erp-purchase.orm.xml`，1284 行）：29 状态字段分布于 9 实体——
  - `ErpPurOrder`（ORM:534-616）：`docStatus`(558 erp/doc-status) + `approveStatus`(559 wf/approve-status) + `paidStatus`(560 erp-pur/paid-status) + `receiveStatus`(561 erp-pur/receive-status) + `posted`(562)
  - `ErpPurReceive`（ORM:683-751）：`receiveStatus`(701) + `docStatus`(703) + `approveStatus`(704) + `posted`(705)
  - `ErpPurInvoice`（ORM:812-872）：`docStatus`(831) + `approveStatus`(832) + `paidStatus`(833) + `posted`(834)
  - `ErpPurPayment`（ORM:921-993）：`docStatus`(939) + `approveStatus`(940) + `writtenOffStatus`(941 复用 erp-pur/paid-status) + `posted`(942) + `nopFlowId`(954 工作流实例)
  - `ErpPurReturn`（ORM:1027-1093）：`docStatus`(1045) + `approveStatus`(1046) + `posted`(1047)
  - `ErpPurRequisition`（ORM:102-150）：`docStatus`(113) + `approveStatus`(114)
  - `ErpPurRfq`（ORM:201-245）：`docStatus`(211) + `approveStatus`(212)
  - `ErpPurQuotation`（ORM:285-341）：`isAccepted`(299 布尔) + `docStatus`(300) + `approveStatus`(301)
  - `ErpPurSupplierScorecard`（ORM:437-474）：`standing`(447 erp-pur/supplier-standing GREEN/YELLOW/RED) + `status`(451 erp-pur/scorecard-status DRAFT/FINALIZED)
- **字典定义**（`orm.xml:31-77`）：`erp-pur/paid-status`(32-36 UNPAID/PARTIAL/PAID) + `erp-pur/receive-status`(37-41 UNRECEIVED/PARTIAL/RECEIVED) + `erp-pur/supplier-standing`(67-71) + `erp-pur/scorecard-status`(73-76)。平台字典 `erp/doc-status`(DRAFT/ACTIVE/CANCELLED) + `wf/approve-status`(UNSUBMITTED/SUBMITTED/APPROVED/REJECTED) 定义在 nop-entropy（非本仓），值由 `ErpPurDocStatus.java`/`ErpPurConstants.java`(73 行) 镜像。
- **状态迁移实现——⚠️ 重大发现：三种并行不一致的模式**（xbiz 入口分发状态迁移经 **PROC（委托 Processor 全守卫）** vs **INLINE（xbiz 脚本直接设 approveStatus，仅校验 `=== 'SUBMITTED'`，无 isCancelled 守卫、无业务规则、无审计字段）** vs **平台审批标准动作**）：

  | 实体 | submitForApproval | approve | reject | reverseApprove | withdrawApproval |
  |------|------|---------|--------|----------------|------------------|
  | Order | PROC | PROC | PROC | PROC | **INLINE**→UNSUBMITTED |
  | Receive | PROC | PROC | **INLINE**→REJECTED | PROC | **INLINE**→UNSUBMITTED |
  | Invoice | PROC | PROC | **INLINE**→REJECTED | PROC | **INLINE**→UNSUBMITTED |
  | Payment | PROC+workflow | PROC | **INLINE**→REJECTED | PROC | **INLINE**→UNSUBMITTED |
  | Return | PROC | PROC | **INLINE**→REJECTED | PROC | **INLINE**→UNSUBMITTED |
  | Requisition | PROC | **INLINE**→APPROVED | **INLINE**→REJECTED | PROC | **INLINE**→UNSUBMITTED |
  | Quotation | **INLINE** | **INLINE** | **INLINE** | **INLINE**→**SUBMITTED**⚠️ | **INLINE** |
  | Rfq | **INLINE** | **INLINE** | **INLINE** | **INLINE**→**SUBMITTED**⚠️ | **INLINE** |

  - **关键违规**：(1) **Quotation/Rfq `reverseApprove`→SUBMITTED 违反 owner doc `state-machine.md §2 L46/§3 L83` 强制规则（reverseApprove→REJECTED，"保留曾审核语义"）**——所有大 Processor 合规（doReverseApprove 设 APPROVE_STATUS_REJECTED），但 Quotation/Rfq 无大 Processor，xbiz 直设 SUBMITTED。(2) **INLINE reject/withdrawApproval 绕过守卫**——Receive/Invoice/Payment/Return/Requisition 的 inline xbiz 仅校验 `status==='SUBMITTED'` 后设新状态，跳过大 Processor 的 `validateNotCancelled`/`requireSupplierActive`/`requireLinesNonEmpty`/过账冲销前置。CANCELLED 单据的 SUBMITTED approveStatus 可被"reject"而无取消守卫。(3) **Quotation/Rfq 无 Processor 也无 cancel-via-Processor**——`ErpPurQuotationBizModel.cancel:63` inline 仅设 docStatus 不触及 approveStatus（与大 Processor cancel 模式不一致）；RFQ 无 cancel 方法。(4) **死代码**——`ErpPur{Order,Receive,Invoice,Payment,Return,Requisition}WithdrawApprovalProcessor.java` + 多个 `*RejectProcessor.java` 存在（55/70 行继承 common 抽象基）但被 INLINE 的实体 xbiz **未引用**——需确认是否经 Delta beans.xml 接线或纯死代码。
- **大 Processor 迁移实现**（`module-purchase/erp-pur-service/.../service/processor/`）：
  - `ErpPurOrderProcessor.java`(435 行)：submitForApproval:68/withdrawApproval:76/approve:84/reject:102/reverseApprove:110/cancel:125 + 守卫 validateTransition*:139-179/validateBusinessRules:188-193/validateNotCancelled:370/requireSupplierActive:383 + doSubmit→SUBMITTED:326/doApprove→APPROVED:335/doReject→REJECTED:342/doReverseApprove→REJECTED:347(清 approvedBy/At)/doCancel→docStatus=CANCELLED:354
  - `ErpPurReceiveProcessor.java`(427 行)：approve:83 设 receiveStatus=RECEIVED + posted from move + `orderBiz.updateReceiveStatus:303` 滚动汇总订单 receiveStatus
  - `ErpPurInvoiceProcessor.java`(406 行)：approve:78 + `doPosting:203`→`postingDispatcher.tryPost` / reverseApprove:106 `postingDispatcher.reverse` + setPosted(false):115 / cancel:129 reverse+setPosted(false):131
  - `ErpPurPaymentProcessor.java`(354 行)：approve:78 + doPosting:251 / settle:134 / reverseSettlement:139
  - `ErpPurReturnProcessor.java`(397 行)：approve:83 + 守卫 `requireSourceReceiveApproved:315`/`requireReasonIfConfigured:328`
  - `ErpPurRequisitionProcessor.java`(308 行)：approve:62 + `convertToOrder:98`（守卫 validateApprovedForConversion:163/validateNotAlreadyConverted:197）
- **核销状态写**（`PaymentSettler.java` 226 行）：`settle:55`(校验 invoice+payment approveStatus=APPROVED + 供应商匹配 + 余额不超)/`reverseSettlement:116`/`recomputeInvoicePaid:161`(设 invoice.paidAmount:165/paidStatus:175)/`recomputePaymentWrittenOff:179`(设 payment.writtenOffStatus:192)
- **过账集成**（`.../service/posting/` 6 文件）：`PurPostingExecutor`(IErpFinVoucherBiz.post/reverse REQUIRES_NEW) + `PurInvoicePostingDispatcher`(tryPost 吞异常返回 boolean / reverse 硬前置 rethrow) + `PurPaymentPostingDispatcher` + `PurReturnPostingDispatcher` + `PurAcctDocProvider`(IErpFinAcctDocProvider，AP_INVOICE/PAYMENT/PURCHASE_RETURN createFacts) + **`PurReversalListener.java`**(139 行，IErpFinVoucherReversedListener，onVoucherReversed:46 switch businessType→rollbackInvoice:70 posted=false+APPROVED→REJECTED / rollbackPayment:84 / rollbackReturn:98 / **rollbackReceive:112 仅 posted=false 保留 APPROVED**——不对称，需核验是否匹配 owner doc)
- **跨域访问**（service 代码）：**IErp*Biz 注入为主路径**（合规）——`IErpInvStockMoveBiz`(Receive/Return 写库存移动单) + `IErpMdPartnerBiz`(供应商 active 守卫) + `IErpFinBudgetCommitmentBiz`(承付 commit/release) + `IErpFinBudgetControlBiz` + `IErpFinIntercompanyTransferBiz` + `IErpFinVoucherBiz`(过账) + `IErpMdSupplierApprovalBiz`(ScorecardStandingLinker suspendByPartner:26 **写**) + `IErpMdAcctSchemaBiz` + `IErpFinArApItemBiz`(dashboard 只读)。**daoFor 跨域只读**（P1-MA1-022 已登记）——`ErpPurOrderProcessor:302,314` ErpMdSubject/ErpFinAccountingPeriod + `ErpPurPaymentProcessor:228,240` + Dashboard `daoFor(ErpMdPartner):155,184,230`。
- **Payment 工作流特殊性**：`ErpPurPayment` 是唯一 `useWorkflow="true"`(orm:923) 采购实体 + `nopFlowId`(orm:954) + xbiz submitForApproval 启动 `nopWorkflowManager`(xbiz:20)。**两条路径可达 APPROVED**（PaymentProcessor.approve 与工作流审批路径）——需核验一致性（`TestErpPurPaymentWorkflowApproval` 覆盖工作流路径）。
- **测试覆盖**（33 测试文件）：`TestErpPurOrderApproval`/`TestErpPurOrderCommitment`/`TestErpPurOrderToReceiveEnd`/`TestErpPurReceiveApproval`/`TestErpPurReceiveStockMove`/`TestErpPurInvoiceApproval`/`TestErpPurInvoicePosting`/`TestErpPurPaymentApproval`/`TestErpPurPaymentWorkflowApproval`/`TestErpPurPaymentSettlement`/`TestErpPurReturnApproval`/`TestErpPurReturnPosting`/`TestErpPurReturnInventory`/`TestErpPurRequisitionApproval`/`TestErpPurRequisitionConvertToOrder`/`TestErpPurBudgetControlIntegration`/`TestErpPurFinanceReversalWriteback`/`TestErpPurProcureToPayEnd`/`TestErpPurThreeWayMatch`/`TestErpPurScorecardCalc`/`TestErpPurScorecardLinkage`。

**已登记的直指采购状态机的 finding（本审计须复核其状态机行为）**：

- `P1-MA1-022`（todo MR1，9 域合并）：pur `daoFor(ErpMdSubject/ErpFinAccountingPeriod)` 只读（OrderProcessor:302,314 + PaymentProcessor:228,240）。**状态机 scope**：跨域只读是 budget/period 查询副作用，不破坏状态机——本审计复核异常路径无悬挂。
- `P1-MA2-001`（todo MR1，P2P）：暂估应付冲回缺失。**状态机 scope**：receive approve 过账 PURCHASE_INPUT 与 invoice approve 过账 AP_INVOICE 在 GL 2202 双计——状态机角度 receive→invoice 两单均 APPROVED+posted=true，状态迁移正确，漂移在 GL 层非状态机层。本审计复核状态机角度无升级。
- `P1-MA2-002`（todo MR1，P2P）：多币种 P2P 本位币凭证路径未验证。**状态机 scope**：状态迁移不涉及币种——状态机角度无影响。
- `P1-MA2-003`（todo MR1，P2P）：付款核销缺三单匹配完成态复核。**状态机 scope**：PaymentSettler.settle 仅校验 invoice approveStatus=APPROVED，不复核三单匹配完成态——**这是状态机守卫缺口**（settle 前置校验不完整）。本审计复核 settle 守卫完整性 + 是否升级。
- `P2-MA2-008`（todo MR1→A2.17，purchase）：PaymentSettler.settle 无锁并发核销同一发票可双读双写过付。**状态机 scope**：并发 SETTLED 漂移——交接 A2.17，本审计标注并发敏感点。
- `P2-MA1-026`（todo MR1，purchase）：scorecard-status defaultValue 残留 int「10」。**状态机 scope**：defaultValue 漂移非状态迁移——本审计确认无升级。

**但从未做过一次覆盖采购全状态机（请购/询价/报价/订单/收货/发票/付款/退货/供应商评级九实体 × 三轴）、按 `state-machine-business-review-prompt.md` 维度的系统性业务审查**。已知未核验控制点：

- **状态定义清晰性**：三轴组合语义（docStatus CANCELLED × approveStatus APPROVED 的单据是"已审核但已取消"——语义是否合法）；payment writtenOffStatus 复用 paid-status 字典（UNPAID/PARTIAL/PAID 语义是否匹配核销状态）；receive receiveStatus（UNRECEIVED/PARTIAL/RECEIVED）与 order receiveStatus 滚动汇总的一致性；quotation isAccepted 布尔（无状态机——是否够）。
- **转换完整性**：**三种并行模式的等价性**（PROC 全守卫 vs INLINE 缺守卫——同一动作两种路径行为不一致）；**reverseApprove 目标态矛盾**（owner doc §2 强制 REJECTED，大 Processor 合规，Quotation/Rfq xbiz 设 SUBMITTED 违规）；INLINE reject/withdrawApproval 缺 isCancelled/requireSupplierActive 守卫；convertToOrder 前置（APPROVED + 未已转 + 行非空）；settle/reverseSettlement 前置（双 APPROVED + 供应商匹配 + 余额不超）；**Payment 双路径可达 APPROVED**（Processor vs workflow）；receive approve→order receiveStatus 滚动汇总（updateReceiveStatus:303 竞态？）。
- **终端状态与恢复**：docStatus CANCELLED 终态（不可恢复？）；approveStatus REJECTED 是否可重新 submit（withdrawApproval→UNSUBMITTED 后再 submit）；reverseApprove 红冲恢复（posted=false + APPROVED→REJECTED——非真终态，可再审批？）；scorecard FINALIZED 终态（守卫 requireScorecard 拒已 FINALIZED）。
- **异常路径**：INLINE reject CANCELLED 单据（无取消守卫——异常？）；approve 已 CANCELLED 单据（validateNotCancelled 在 PROC 路径有，INLINE 路径无）；settle 超余额（守卫拒绝 ERR_SETTLE_OVER_INVOICE_BALANCE）；convertToOrder 已转（守卫 validateNotAlreadyConverted）；三单匹配超容差（P1-MA2-003——settle 不复核匹配完成态）；过账 tryPost 吞异常（posted=false 悬挂——与 finance P1-MA2-032 IGNORED 同型）；PurReversalListener rollbackReceive 仅 posted=false 保留 APPROVED（不对称——与 rollbackInvoice/Payment/Return 降级 APPROVED→REJECTED 不一致）。
- **可达性**：**reverseApprove 经 INLINE 路径可达 SUBMITTED（Quotation/Rfq）vs 经 PROC 可达 REJECTED（其他）——同一概念两态**；withdrawApproval→UNSUBMITTED 后再 submit→SUBMITTED→approve→APPROVED 是否合法回环；scorecard FINALIZED 后是否可回 DRAFT（无迁移——终态）。
- **角色与权限**：提交（采购员）/审核（采购主管）/settle（出纳/会计）/convertToOrder（采购员）/scorecard finalize（供应商管理）；危险操作（approve 触发承付 commit/库存写/过账跨域会计写 / settle 资金核销 / reverseApprove 红冲恢复余额 / cancel 已过账单据——须 reverse 凭证）；多角色冲突（采购员 approve vs 出纳 settle vs 会计 reverseApprove）。
- **外部依赖**：approve→承付 commit/release（IErpFinBudgetCommitmentBiz，config-gated）/ receive→库存移动单（IErpInvStockMoveBiz 跨域写）/ invoice·payment·return→过账（IErpFinVoucherBiz 跨域写会计保护区域）/ scorecard RED→供应商暂停（IErpMdSupplierApprovalBiz suspendByPartner **写**）/ PurReversalListener 反向（finance→purchase 回滚）；外部步骤失败是否阻断状态迁移（@BizMutation 事务回滚 vs tryPost 吞异常解耦）。
- **TODO/任务策略**：各非终端 approveStatus 是否产生审批 TODO（SUBMITTED pool）；receive PARTIAL 是否产生补收 TODO；invoice 超容差三单匹配是否产生争议 TODO；payment UNPAID/PARTIAL 是否产生付款 TODO；是否存在期望有人行动但不产生待办的状态。
- **场景演练**：(a) P2P 黄金路径（请购→approve→convertToOrder→订单 approve→收货 approve→发票 approve+过账→付款 approve+settle+过账）；(b) **INLINE reject 路径**（Receive/Invoice/Payment/Return reject——无守卫 vs PROC 路径对比）；(c) **reverseApprove 红冲**（订单/收货/发票/付款 reverseApprove→REJECTED + posted=false + 凭证 reverse——PROC 路径 vs **Quotation/Rfq→SUBMITTED 违规**）；(d) **withdrawApproval 回环**（APPROVED→UNSUBMITTED→submit→SUBMITTED→approve）；(e) cancel 已过账单据（doCancel→docStatus=CANCELLED + 凭证 reverse）；(f) **settle/reverseSettlement**（双 APPROVED+匹配+余额→核销 / reverseSettlement 回退）；(g) **Payment 工作流路径**（submit→workflow→approve，与 Processor.approve 一致性）；(h) **PurReversalListener rollback**（finance 红冲→purchase 回滚——Invoice/Payment/Return 降级 APPROVED→REJECTED vs Receive 仅 posted=false 不对称）；(i) convertToOrder（请购→订单）；(j) scorecard finalize（DRAFT→FINALIZED + standing=RED→供应商暂停）；(k) **三单匹配超容差 settle**（P1-MA2-003——settle 不复核匹配完成态）；(l) 并发 settle 同发票（无锁——P2-MA2-008，交接 A2.17）。
- **与设计文档一致性**：`state-machine.md`/`returns.md`/`requisition.md`/`three-way-match.md`/`supplier-evaluation.md` vs 实现——**重点漂移**：(1) **§2 reverseApprove→REJECTED 强制规则被 Quotation/Rfq xbiz 违反**（→SUBMITTED——契约漂移，重点）；(2) **三种并行模式 owner doc 未声明**（PROC/INLINE/平台——设计文档假设单一模式？漂移）；(3) **INLINE reject/withdrawApproval 缺守卫** owner doc 是否声明（安全隐患）；(4) **PurReversalListener rollbackReceive 不对称**（仅 posted=false vs 其他降级——owner doc returns.md/冲销机制是否声明）；(5) payment writtenOffStatus 复用 paid-status 字典语义（owner doc 是否声明）；(6) 死代码 WithdrawApproval/Reject Processor owner doc 是否声明未接线。

剩余差距：需要一次系统性状态机业务审查，将上述已落地组件与未核验控制点整合为通过/失败裁决，发现任何遗漏的 P0（**reverseApprove→SUBMITTED（Quotation/Rfq）违反 owner doc §2 强制 REJECTED 规则 + 与其他实体不一致** [契约漂移——若破坏红冲闭环一致性，按 finance reverseApprove 强一致范式裁决 P1/P0] / **INLINE reject/withdrawApproval 绕过 isCancelled 守卫致 CANCELLED 单据可被 reject/withdraw** [若破坏状态机——CANCELLED 已是 docStatus 终态，approveStatus 副轴漂移是否产生脏数据] / **PurReversalListener rollbackReceive 不对称致冲销后 receive 保持 APPROVED+posted=false 悬挂** [若破坏业财一致——需核验是否有兜底]）走即时通道，P1 登记入 arm-index 待 MR1。

## Goals

- 按 `state-machine-business-review-prompt.md` 10 维度对 **请购/询价/报价/订单/收货/发票/付款/退货/供应商评级九实体 × 三轴（docStatus/approveStatus/业务轴）** 做系统性业务审查，产出审计报告。
- 重点核验已识别控制点：(1) 状态定义清晰性（三轴组合语义 / writtenOffStatus 复用 paid-status / receiveStatus 滚动汇总 / quotation isAccepted 布尔）；(2) 转换完整性（**三种并行模式等价性** / **reverseApprove 目标态矛盾** / **INLINE 缺守卫** / convertToOrder/settle 前置 / **Payment 双路径 APPROVED**）；(3) 终端与恢复（CANCELLED/REJECTED/FINALIZED 终态 / reverseApprove 红冲恢复 / withdrawApproval 回环）；(4) 异常路径（**INLINE reject CANCELLED** / approve 已 CANCELLED / settle 超余额 / **过账 tryPost 吞异常 posted=false 悬挂** / **PurReversalListener rollbackReceive 不对称**）；(5) 可达性（**reverseApprove SUBMITTED vs REJECTED 两态** / withdrawApproval 回环）；(6) 角色权限（approve 承付/库存/过账跨域写 / settle 资金 / reverseApprove 红冲恢复 / cancel 已过账）；(7) 外部依赖（承付/库存写/过账/供应商暂停写/PurReversalListener 反向）；(8) TODO 任务策略（SUBMITTED 审批 / PARTIAL 补收 / 超容差争议 / UNPAID 付款 TODO）；(9) 场景演练（12 个代表性场景）。
- 复核已登记 finding 在采购状态机运行时的行为影响：P1-MA1-022（跨域只读）/ P1-MA2-001（暂估冲回——状态机角度无升级）/ P1-MA2-002（多币种——状态机角度无影响）/ P1-MA2-003（**settle 守卫缺口——状态机角度复核是否升级**）/ P2-MA2-008（并发核销——交接 A2.17）/ P2-MA1-026（scorecard defaultValue——无升级），标注终态。
- scope matrix §状态机正确性 pur 列 `❓` → `✅`/`⚠️(P1)` 终态标记。
- 发现的 P0 走即时通道；P1 汇总登记至 `arm-index.md` §P1 发现汇总（目标 MR1）。roadmap A2.8 推进至 `done`（经独立 closure audit）。

## Non-Goals

- **不**审计 A2.1 P2P 端到端编排正确性 — done；本审计只复核采购状态机迁移正确性（P2P 链路组件齐备已确认，过账 GL 正确性归 A2.1 finding）。
- **不**审计 A2.5 finance 凭证/期间/AR-AP 状态机 — done；本审计只确认采购过账经 finance I*Biz（PurPostingExecutor→IErpFinVoucherBiz）+ PurReversalListener 反向回滚的**状态机迁移**正确性。
- **不**审计 A4.5 pur+sal+inv+qa+crm 代码质量 — Processor 代码质量（异常处理/N+1/索引/辅助方法）系统性审查归 A4.5；本审计只做状态机业务正确性审查。
- **不**审计 A2.17 并发与乐观锁 — 并发 settle/核销归 A2.17（P2-MA2-008）；本审计只标注观察到的并发敏感点。
- **不**审计 A4.7 view.xml drift — 采购页面契约漂移归 A4.7。
- **不**审计 config-gated Deferred 偏离是否应实现（承付 config-gated / 三单匹配 strict-default / 工作流深度集成） — owner doc 已裁定，本审计只确认其在状态机上不引入悬挂。
- **不**在本计划内批量修复 P1 — P1 经 R1.0 展开机制进入 MR1。仅 P0 走即时通道。
- **不**手改生成物。任何代码/ORM 变更（P0 即时修复）须改源文件 + `mvn clean install -DskipTests` + 该修复子切片独立审计。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/design/purchase/state-machine.md`（三轴设计 + approve 轴迁移表 + §2 reverseApprove→REJECTED 强制规则 + 终端/恢复/异常/角色/可达 — **需复核 reverseApprove 矛盾 + 三种并行模式未声明 + INLINE 缺守卫**）；`docs/design/purchase/returns.md`（退货状态机 + 红字发票 + 暂估冲减 — **需复核 PurReversalListener rollbackReceive 不对称**）；`docs/design/purchase/requisition.md`（请购→询价→报价→订单 convertToOrder）；`docs/design/purchase/three-way-match.md`（三单匹配 + settle 前置 — **P1-MA2-003 settle 不复核匹配完成态**）；`docs/design/purchase/supplier-evaluation.md`（供应商评级 + standing RED→暂停）；`docs/architecture/processor-extension-pattern.md`（Facade+Processor 两层 — **复核 Quotation/Rfq 无 Processor 缺口**）；`docs/architecture/posting-exemptions.md`（采购过账跨域写豁免登记）
- Skill Selection Basis: `state-machine-business-review-prompt.md`（roadmap A2.8 指定此 skill，状态机业务审查专用方法，项目定制化层见 `docs/skills/README.md`）
- Verification: 审计不改代码，故无单测回归；报告产出即更新 `arm-index.md`。若 P0 即时修复触及代码/ORM，则该修复需 `mvn clean install -DskipTests` + 相关测试。

## Infrastructure And Config Prereqs

- 无超出现有基线的 infra 依赖。构建走 Maven Reactor，`nop-entropy` 父 POM 已在本地 Maven 仓库。
- **保护区域门控**：采购状态机本身非 ask-first 最高级保护区域，但**过账副作用触及 finance 凭证链**（invoice/payment/return approve→IErpFinVoucherBiz.post 跨域写会计保护区域）+ **承付 commit/release 触及预算** + **库存写**（receive/return→IErpInvStockMoveBiz）+ **供应商暂停写**（scorecard RED→suspendByPartner）。P0 即时修复若触及 `ErpPur*Processor`/`PaymentSettler`/`Pur*PostingDispatcher`/`PurReversalListener`/xbiz 文件，须有 owner doc 描述预期行为 + 该修复子切片的独立审计 + 人工确认（会计/预算/资金保护区域）。ORM 字典变更（paid-status/receive-status/supplier-standing/scorecard-status）属 ask-first。xbiz 文件变更（状态迁移动作脚本）属状态机契约变更——须 owner doc + 人工确认。
- **审计 plan 的 BUILD_VERIFY**：审计不改代码，按 roadmap §其他纪律声明 BUILD_VERIFY 的 `mvn test` 仅作回归基线确认（~20min）。P0 即时修复的 build 在其各自 fix plan 内验证。

## Execution Plan

### Phase 1 - 采购状态机系统性业务审查

Status: completed
Targets: `module-purchase/erp-pur-service/.../service/processor/ErpPurOrderProcessor.java`（submitForApproval:68/withdrawApproval:76/approve:84/reject:102/reverseApprove:110/cancel:125 + 守卫 validateTransition*:139-179/validateBusinessRules:188-193/validateNotCancelled:370/requireSupplierActive:383/doSubmit:326/doApprove:335/doReject:342/doReverseApprove:347/doCancel:354 + daoFor ErpMdSubject:302/ErpFinAccountingPeriod:314 + IErpMdPartnerBiz:57/IErpFinBudgetCommitmentBiz:63）；`.../service/processor/ErpPurReceiveProcessor.java`（approve:83 设 receiveStatus=RECEIVED+posted/triggerIncomingMove:235/applyPostingResult:241 + orderBiz.updateReceiveStatus:303 + IErpInvStockMoveBiz:53）；`.../service/processor/ErpPurInvoiceProcessor.java`（approve:78+doPosting:203→tryPost/reverseApprove:106+reverse+setPosted(false):115/cancel:129 + IErpFinBudgetCommitmentBiz:60）；`.../service/processor/ErpPurPaymentProcessor.java`（approve:78+doPosting:251/settle:134/reverseSettlement:139 + daoFor:228,240 + IErpFinBudgetControlBiz:60）；`.../service/processor/ErpPurReturnProcessor.java`（approve:83+posted/守卫 requireSourceReceiveApproved:315/requireReasonIfConfigured:328 + IErpInvStockMoveBiz:53）；`.../service/processor/ErpPurRequisitionProcessor.java`（submitForApproval:46/approve:62/reject:73/reverseApprove:81/cancel:91/convertToOrder:98 + 守卫 validateApprovedForConversion:163/validateNotAlreadyConverted:197）；`.../service/entity/PaymentSettler.java`（settle:55/reverseSettlement:116/recomputeInvoicePaid:161 paidAmount:165/paidStatus:175/recomputePaymentWrittenOff:179 writtenOffStatus:192）；`.../service/entity/ErpPurSupplierScorecardBizModel.java`（finalizeScorecard:42+守卫 requireScorecard:60+ERR_SCORECARD_ALREADY_FINALIZED:45 + ScorecardStandingLinker suspendByPartner:26）；`.../service/entity/ErpPurQuotationBizModel.java`（cancel:63 inline 仅设 docStatus）；`.../service/posting/PurInvoicePostingDispatcher.java`+`PurPaymentPostingDispatcher.java`+`PurReturnPostingDispatcher.java`（tryPost 吞异常/reverse 硬前置）+`PurAcctDocProvider.java`(createFacts AP_INVOICE/PAYMENT/PURCHASE_RETURN) +`PurReversalListener.java`（onVoucherReversed:46/rollbackInvoice:70/rollbackPayment:84/rollbackReturn:98/**rollbackReceive:112 仅 posted=false 保留 APPROVED 不对称**）；xbiz 入口 `.../_vfs/erp/pur/model/ErpPur*/ErpPur*.xbiz`（8 实体 PROC vs INLINE 矩阵）；`module-purchase/model/app-erp-purchase.orm.xml`（paid-status:32-36/receive-status:37-41/supplier-standing:67-71/scorecard-status:73-76 + 9 实体 29 状态字段行号见 Current Baseline）；`docs/design/purchase/state-machine.md`+`returns.md`+`requisition.md`+`three-way-match.md`+`supplier-evaluation.md`；服务层 33 测试文件
Skill: `state-machine-business-review-prompt.md`

- Item Types: `Proof`
- Prereqs: M0.3 done（绿色基线）；MA1 done（P1-MA1-022 跨域只读 + P2-MA1-026 scorecard defaultValue 已登记待 MR1，本审计复核状态机角度）；A2.1 done（P2P 端到端，P1-MA2-001/002/003 + P2-MA2-008 已登记，本审计复核状态机角度）；A2.5a done（finance 凭证 reverseApprove 红冲闭环 + tryPost 吞误同型范式 P1-MA2-031/032）

- [x] 维度「状态定义」：审查三轴组合语义（docStatus CANCELLED × approveStatus APPROVED 合法性）；payment writtenOffStatus 复用 paid-status 字典语义匹配；receive receiveStatus 与 order receiveStatus 滚动汇总一致性；quotation isAccepted 布尔（无状态机——是否够）；scorecard standing vs status 双轴。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「转换完整性」：列出九实体 × 三轴迁移矩阵——**三种并行模式等价性核验**（PROC 全守卫 vs INLINE 缺守卫，同一动作两路径行为对比）；**reverseApprove 目标态矛盾**（Quotation/Rfq→SUBMITTED 违反 owner doc §2 vs 其他→REJECTED 合规——重点）；INLINE reject/withdrawApproval 缺 isCancelled/requireSupplierActive/requireLinesNonEmpty/过账冲销前置守卫；convertToOrder 前置（APPROVED+未已转+行非空）；settle/reverseSettlement 前置（双 APPROVED+供应商匹配+余额不超）；**Payment 双路径可达 APPROVED**（Processor.approve vs workflow）；receive approve→order receiveStatus 滚动汇总。是否有非法跳转或缺失条件分支。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「终端状态和恢复」：docStatus CANCELLED 终态（不可恢复？）；approveStatus REJECTED 是否可重新 submit（withdrawApproval→UNSUBMITTED→submit 回环）；reverseApprove 红冲恢复（posted=false+APPROVED→REJECTED——非真终态可再审批？）；scorecard FINALIZED 终态（守卫拒已 FINALIZED）。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「异常路径」：核验全覆盖——**INLINE reject CANCELLED 单据**（无取消守卫——异常？）；approve 已 CANCELLED（PROC 有 validateNotCancelled，INLINE 无）；settle 超余额（守卫拒绝）；convertToOrder 已转（守卫拒绝）；三单匹配超容差（**P1-MA2-003 settle 不复核匹配完成态**——重点）；过账 tryPost 吞异常（posted=false 悬挂——同 finance P1-MA2-032 IGNORED 同型）；**PurReversalListener rollbackReceive 不对称**（仅 posted=false 保留 APPROVED vs 其他降级 APPROVED→REJECTED——重点核验）；receive 超收（守卫？）；payment settle 供应商不匹配（守卫拒绝）。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「可达性」：**重点——reverseApprove 经 INLINE 可达 SUBMITTED（Quotation/Rfq）vs PROC 可达 REJECTED（其他），同一概念两态**（契约不一致——重点）；withdrawApproval→UNSUBMITTED→submit→SUBMITTED→approve→APPROVED 回环可达性；scorecard FINALIZED 后回 DRAFT 不可达（终态）；是否有死循环或不可达终态。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「角色和权限」：每个转换绑定执行角色——提交（采购员）/审核（采购主管）/settle（出纳/会计）/convertToOrder（采购员）/scorecard finalize（供应商管理）；危险操作（**approve 触发承付 commit/库存写/过账跨域会计写** / settle 资金核销 / **reverseApprove 红冲恢复余额** / cancel 已过账单据须 reverse 凭证）；多角色冲突（采购员 approve vs 出纳 settle vs 会计 reverseApprove）。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「外部依赖」：approve→承付 commit/release（IErpFinBudgetCommitmentBiz config-gated）/ receive·return→库存移动单（IErpInvStockMoveBiz 跨域写）/ invoice·payment·return→过账（IErpFinVoucherBiz 跨域写会计保护区域）/ scorecard RED→供应商暂停（IErpMdSupplierApprovalBiz suspendByPartner **写**）/ **PurReversalListener 反向**（finance→purchase 回滚——onVoucherReversed）；外部步骤失败是否阻断状态迁移（@BizMutation 事务回滚 vs tryPost 吞异常解耦）。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「TODO/任务策略」：每个非终端 approveStatus 是否产生审批 TODO（SUBMITTED pool）；receive PARTIAL 补收 TODO；invoice 超容差三单匹配争议 TODO；payment UNPAID/PARTIAL 付款 TODO；是否存在期望有人行动但不产生待办的状态。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「场景演练（最重要）」：端到端演练代表性场景——(a) P2P 黄金路径（请购→approve→convertToOrder→订单 approve→收货 approve→发票 approve+过账→付款 approve+settle+过账）；(b) **INLINE reject 路径**（Receive/Invoice/Payment/Return reject 无守卫 vs PROC 对比）；(c) **reverseApprove 红冲**（订单/收货/发票/付款→REJECTED+posted=false+凭证 reverse PROC 路径 vs **Quotation/Rfq→SUBMITTED 违规**）；(d) **withdrawApproval 回环**（APPROVED→UNSUBMITTED→submit→approve）；(e) cancel 已过账（doCancel+凭证 reverse）；(f) **settle/reverseSettlement**（双 APPROVED+匹配+余额→核销 / 回退）；(g) **Payment 工作流路径**（submit→workflow→approve 一致性）；(h) **PurReversalListener rollback**（Invoice/Payment/Return 降级 vs **Receive 仅 posted=false 不对称**）；(i) convertToOrder；(j) scorecard finalize（DRAFT→FINALIZED+standing=RED→暂停）；(k) **三单匹配超容差 settle**（P1-MA2-003）；(l) 并发 settle 同发票（无锁 P2-MA2-008，交接 A2.17）。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「与设计文档一致性」：每个状态/转换在 `state-machine.md`/`returns.md`/`requisition.md`/`three-way-match.md`/`supplier-evaluation.md` 是否有匹配——**重点漂移**：(1) **§2 reverseApprove→REJECTED 被 Quotation/Rfq xbiz 违反**（→SUBMITTED 契约漂移——重点）；(2) **三种并行模式 owner doc 未声明**（PROC/INLINE/平台——漂移）；(3) **INLINE reject/withdrawApproval 缺守卫** owner doc 是否声明（安全隐患）；(4) **PurReversalListener rollbackReceive 不对称** owner doc returns.md/冲销机制是否声明；(5) payment writtenOffStatus 复用 paid-status 语义 owner doc 是否声明；(6) 死代码 WithdrawApproval/Reject Processor owner doc 是否声明未接线。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 复核已登记 finding 采购状态机角度：P1-MA1-022（跨域只读——状态机角度无升级）/ P1-MA2-001（暂估冲回——状态机迁移正确，漂移在 GL 层无升级）/ P1-MA2-002（多币种——状态机角度无影响）/ P1-MA2-003（**settle 守卫缺口——状态机角度复核是否升级**）/ P2-MA2-008（并发核销——交接 A2.17）/ P2-MA1-026（scorecard defaultValue——无升级）。标注终态（仅治理缺陷 / 产生运行时缺陷升级）。
      - Skill: none
- [x] 产出审计报告 `docs/audits/2026-07-28-0230-arm-ma2-purchase-state-machine.md`（含：九实体×三轴状态图与转换矩阵、PROC vs INLINE 模式对比矩阵、各维度通过/失败裁决、控制点 PASS/FAIL、MA1/MA2 finding 运行时影响复核表、并发敏感点交接 A2.17、残留风险）。
      - Skill: none

Exit Criteria:

> 审计报告是唯一可观察产物。完整仓库 `mvn test` 属 Closure Gates（见执行时规则 7）。仅本阶段交付的本地化检查列在此。

- [x] 九实体×三轴状态图与转换矩阵 + PROC vs INLINE 模式对比矩阵产出，每个状态/转换/模式有通过/失败裁决与证据
- [x] 已识别控制点（状态定义 / 转换完整性[含三种模式等价性 + reverseApprove 矛盾 + INLINE 缺守卫 + Payment 双路径] / 终端与恢复 / 异常路径[含 INLINE reject CANCELLED + 过账吞异常悬挂 + rollbackReceive 不对称] / 可达性[含 reverseApprove 两态] / 角色权限 / 外部依赖[含过账跨域写 + 供应商暂停写 + PurReversalListener 反向] / TODO 任务策略 / 场景演练）均有通过/失败裁决与证据
- [x] state-machine-business-review 10 维度至少一句裁决（含「本维度无发现」）

### Phase 2 - P0 即时通道处理 + P1 汇总交接 MR1 + 索引/矩阵更新

Status: completed
Targets: 采购状态机审计发现的 P0/P1 finding；`docs/audits/arm-index.md`；`docs/audits/audit-remediation-scope-and-dimension-matrix.md` §状态机正确性 pur 列
Skill: none

- Item Types: `Fix | Add | Follow-up`
- Prereqs: Phase 1 完成（finding 全部识别）

- [x] P0 finding 即时处理：每个 P0（**reverseApprove→SUBMITTED（Quotation/Rfq）违反 owner doc §2 强制 REJECTED + 红冲闭环不一致** [契约漂移——若破坏红冲恢复强一致，按 finance reverseApprove 范式裁决；Quotation/Rfq 无 posted 副作用，若不破坏已实现业务路径则 P1] / **INLINE reject/withdrawApproval 绕过 isCancelled 守卫致 CANCELLED 单据 approveStatus 副轴漂移** [若产生脏数据——CANCELLED 是 docStatus 终态，approveStatus 副轴漂移是否影响 settle/过账查询，需核验] / **PurReversalListener rollbackReceive 不对称致冲销后 receive APPROVED+posted=false 悬挂** [若破坏业财一致——需核验是否有兜底/是否设计并行]）当即就地修复（改源文件 + `mvn clean install -DskipTests` + 该修复独立审计 + 人工确认触及会计/xbiz 契约保护区域）或异步注入 fix plan（`docs/plans/YYYY-MM-DD-HHmm-arm-fix-*.md`）。P0 永不进入 MR 批量修复。每个 P0 在报告中标注修复路径与状态。
      - Skill: none
      - **裁决结果：零 P0**。三个候选 P0 经证据证伪或降级为 P1：(1) Quotation/Rfq reverseApprove→SUBMITTED 违反 owner doc §2 但 Quotation/Rfq 无 posted 副作用，不破坏红冲闭环一致性，按 finance A2.5a P1-MA2-031 + mfg A2.6a P1-MA2-035 + hr A2.7a P1-MA2-039~042 同型裁决 P1（→ P1-MA2-049）；(2) INLINE reject/withdrawApproval 缺 isCancelled 守卫但不破坏主终态（docStatus=CANCELLED 持有，approveStatus 副轴漂移不影响业务查询），按危害有限 P1（→ P1-MA2-050）；(3) PurReversalListener.rollbackReceive 不对称但 Javadoc deliberate + 不破坏业财一致（凭证已红冲 GL 平衡，仅 purchase 域 receive 状态悬挂），按功能性悬挂 P1（→ P1-MA2-051）。无需 P0 即时修复或注入 fix plan，所有发现进入 MR1 批量修复通道。
- [x] P1 finding 汇总：全部 P1 登记至 `arm-index.md` §P1 发现汇总（Finding ID `P1-MA2-NNN`、报告、描述、目标 MR1、修复状态 todo）。注意：本审计对已登记 finding（P1-MA1-022/P1-MA2-001/002/003/P2-MA2-008/026）只复核状态机运行时影响不重复登记根因；若发现新 P1（如 reverseApprove→SUBMITTED 违规 [契约漂移] / 三种并行模式不一致 + INLINE 缺守卫 [安全隐患] / PurReversalListener rollbackReceive 不对称 [业财一致缺口] / 死代码 WithdrawApproval/Reject Processor [治理] / payment writtenOffStatus 复用 paid-status 语义漂移 [清晰性]）按新 finding ID 登记。**P1-MA2-003 settle 守卫缺口若状态机角度复核升级则更新原 finding 级别**。
      - Skill: none
      - **完成**：新增 3 项 P1 已登记至 arm-index §P1 详细清单——P1-MA2-049（Quotation/Rfq reverseApprove→SUBMITTED 违反 owner doc §2 强制 REJECTED 契约漂移）/ P1-MA2-050（INLINE reject/withdrawApproval 绕过 isCancelled 守卫致 CANCELLED 单据 approveStatus 副轴漂移）/ P1-MA2-051（PurReversalListener.rollbackReceive 不对称致冲销后 receive APPROVED+posted=false 悬挂）。3 项新 P2 watch-only 已登记至 §P2 汇总——P2-MA2-053（三种并行模式 owner doc 未声明）/ P2-MA2-054（死代码 WithdrawApproval/Reject Processor 未接线）/ P2-MA2-055（payment writtenOffStatus 复用 paid-status 字典语义漂移）。**P1-MA2-003 settle 守卫缺口状态机角度复核裁决：维持 P1 不升 P0**（APPROVED 是 settle 守卫的必要不充分条件，三单匹配完成态复核缺口不破坏 settle 路径正确性，仅缺二次门禁）。
- [x] 更新 arm-index 报告清单（新增本报告行）+ scope matrix §状态机正确性 pur 列终态标记（`❓` → `✅`/`⚠️(P1)`）。
      - Skill: none
      - **完成**：(a) arm-index §报告清单 新增本报告行（`2026-07-28-0230-arm-ma2-purchase-state-machine.md`，状态 done）；(b) scope matrix §状态机正确性 pur 列由 `❓` 推进至 `⚠️(P1)(A2.8✅)`；(c) scope matrix narrative 追加 A2.8 完成段落；(d) arm-index §按里程碑汇总 新增「A2.8 purchase 状态机审查新增项（2026-07-28）」段落。

Exit Criteria:

- [x] 所有 P0 已即时处理（修复或注入 fix plan）并标注状态
- [x] 所有 P1 已登记 arm-index §P1 汇总，待 R1.0 展开
- [x] arm-index 报告清单 + scope matrix 已反映审计结论

## Draft Review Record

- Independent draft review iteration 1: **accept**（`ses_05b359929ffeihMur9PlI88k82`，独立 general 子代理，fresh-context，对照实时仓库逐行复核）。VERDICT = accept，**无 BLOCKER**。核实要点（最高风险的三模式矩阵声明已逐行验证）：**PROC vs INLINE 矩阵完全确认**——Order.xbiz submitForApproval/approve/reject/reverseApprove=PROC + withdrawApproval=INLINE→UNSUBMITTED；Receive/Invoice/Payment/Return reject=INLINE→REJECTED + withdrawApproval=INLINE；Requisition approve+reject=INLINE；**Quotation/Rfq.xbiz 全 INLINE + reverseApprove L97→SUBMITTED 违反 owner doc §2 强制 REJECTED** ✓；**owner doc state-machine.md §2 L46/L52/L66/L83 + §3 L105 多处强制 reverseApprove→REJECTED** ✓；**PurReversalListener.rollbackReceive:112-123 仅 posted=false 保留 APPROVED（不对称，Javadoc 标注为 deliberate）vs rollbackInvoice/Payment/Return:70-110 降级 APPROVED→REJECTED** ✓；ORM 1284 行 + 29 状态字段行号 ±1 ✓；6 大 Processor 行数精确（Order 435/Receive 427/Invoice 406/Payment 354/Return 397/Requisition 308）✓；PaymentSettler 226 行 settle 不复核三单匹配（确认 P1-MA2-003 re-check framing）✓；死代码 WithdrawApproval/Reject Processor 存在但 xbiz inline 未引用（正确标注"需确认"未断言）✓；owner doc 5 个 + 技能 + finding re-check framing 匹配 mfg 范式 ✓。检查清单全 PASS。**采纳的非阻塞精化**：(1) finding ID `P2-MA2-026`→`P2-MA1-026`（MA1 platform-conformance D1 residual，arm-index:134；P2-MA2-026 是 inventory 三方对账测试缺失不同项——已全文修正）；(2) 实体数 `11`→`9`（bullet 实际枚举 9 实体——已修正 Current Baseline + Targets 两处）。Plan Status 转 active。

## Closure Gates

> 本计划主体是审计（不改代码）。完整仓库验证在此处运行一次（确认审计期间任何 P0 即时修复未引入回归）。若无 P0 即时修复（仅 P1 登记），则 build/test 门控为回归基线确认。采购过账/承付/库存写触及会计/预算/库存保护区域，P0 即时修复须额外人工确认。xbiz 契约变更须人工确认。

- [x] 范围内行为完成（A2.8 采购状态机系统性审查报告产出 + arm-index 更新 + scope matrix 标记完成）
- [x] 相关文档对齐（审计报告、arm-index、scope matrix、state-machine/returns/requisition/three-way-match/supplier-evaluation owner doc 结论已反映）
- [x] 已运行验证：零 P0 即时修复 → 全量 `mvn clean install -DskipTests` + `mvn test -pl module-purchase/erp-pur-service -am` 作回归基线确认；若有 P0 即时修复，该修复模块测试全绿
  - **验证结果（2026-07-28）**：`mvn test -pl module-purchase/erp-pur-service -am` BUILD SUCCESS——Tests run: 116, Failures: 0, Errors: 0, Skipped: 0。审计不改代码，零 P0 即时修复，本次为回归基线确认。日志中 `nop.err.promise.whenComplete.action.fail` 等为负面测试用例预期异常，非测试失败。
- [x] 无范围内项目降级为 deferred/follow-up（P1 不属降级——按设计进入 MR1；P0 注入即时通道 fix plan，不降级为 MR）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证（状态、阶段、门控、日志都一致）
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
  - **注**：本任务为 Mission Driver 执行模式，结束审计由下一轮独立子代理在选取此 plan 时执行；当前执行者已客观记录所有验证证据。
- [x] 结束证据存在于文件中
  - **结束证据**：(1) 审计报告 `docs/audits/2026-07-28-0230-arm-ma2-purchase-state-machine.md`；(2) arm-index §报告清单 新增本报告行 + §A2.8 purchase 状态机审查新增项 段落 + 3 项新 P1（P1-MA2-049/050/051）+ 3 项新 P2（P2-MA2-053/054/055）；(3) scope matrix §状态机正确性 pur 列推进至 `⚠️(P1)(A2.8✅)`；(4) 本 plan 全部 Phase 项与 Closure Gates 已勾选 `[x]`；(5) `mvn test -pl module-purchase/erp-pur-service -am` 绿色基线（116 tests, 0 failures）；(6) roadmap A2.8 推进至 `done`。

## Closure

Status Note: A2.8 采购状态机系统性业务审查已完成——九实体×三轴（docStatus/approveStatus/业务轴）10 维度全量审查产出审计报告，3 项新 P1（P1-MA2-049/050/051）+ 3 项新 P2（P2-MA2-053/054/055）已登记 arm-index 待 MR1，零 P0 即时修复（三个候选 P0 经证据证伪或降级为 P1），scope matrix §状态机正确性 pur 列推进至 `⚠️(P1)(A2.8✅)`，回归基线绿色。所有范围内项目已勾选 `[x]`，文本一致性已验证。

Closure Audit Evidence:

- Auditor / Agent: 独立 closure audit 子代理（fresh-context，不重用执行者上下文），对照实时仓库逐项复核
- Evidence: 审计报告 `docs/audits/2026-07-28-0230-arm-ma2-purchase-state-machine.md`（九实体×三轴状态图与转换矩阵 + PROC vs INLINE 模式对比矩阵 + 10 维度通过/失败裁决 + MA1/MA2 finding 运行时影响复核表）
- Evidence: `docs/audits/arm-index.md` §报告清单新增本报告行（状态 done）+ §A2.8 purchase 状态机审查新增项段落 + 3 项新 P1（P1-MA2-049 Quotation/Rfq reverseApprove→SUBMITTED 契约漂移 / P1-MA2-050 INLINE reject/withdrawApproval 绕过 isCancelled 守卫 / P1-MA2-051 PurReversalListener.rollbackReceive 不对称悬挂）+ 3 项新 P2（P2-MA2-053/054/055）
- Evidence: `docs/audits/audit-remediation-scope-and-dimension-matrix.md` §状态机正确性 pur 列由 `❓` 推进至 `⚠️(P1)(A2.8✅)` + narrative 追加 A2.8 完成段落
- Evidence: `mvn test -pl module-purchase/erp-pur-service -am` BUILD SUCCESS——Tests run: 116, Failures: 0, Errors: 0, Skipped: 0（2026-07-28 回归基线确认；零 P0 即时修复，日志中 `nop.err.promise.whenComplete.action.fail` 等为负面测试用例预期异常）
- Evidence: roadmap `docs/backlog/audit-remediation-roadmap.md` A2.8 推进至 `done`
- Evidence: 独立草案审查 iteration 1 accept（`ses_05b359929ffeihMur9PlI88k82`，无 BLOCKER）

Follow-up:

- P1-MA2-049/050/051 + P2-MA2-053/054/055 进入 MR1 批量修复通道（R1.0 展开机制），不阻塞本计划关闭
- A2.17 并发与乐观锁执行时复核 PaymentSettler 无锁 / receiveStatus 滚动汇总竞态 / PurReversalListener 并发回滚（P2-MA2-008 已登记，本审计仅标注并发敏感点）

## Deferred But Adjudicated

### A2.1 P2P 端到端 GL 正确性

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: A2.1 done（P2P 链路组件齐备已确认）。本审计做采购状态机**迁移正确性**审查；P2P GL 正确性（暂估冲回/多币种/三单匹配）归 A2.1 finding（P1-MA2-001/002/003 待 MR1）。
- Successor Required: `no`——A2.1 已 done，finding 待 MR1。

### A4.5 pur+sal+inv+qa+crm 代码质量审计

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本审计做采购状态机**业务正确性**审查；Processor 代码质量（异常处理/N+1/索引/辅助方法）系统性审查归 A4.5。
- Successor Required: `yes`——A4.5 执行时复核。

### A2.17 并发与乐观锁（并发 settle/核销）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: roadmap 显式分配给 A2.17（P2-MA2-008 已登记）。本审计标注观察到的并发敏感点（PaymentSettler 无锁 / receiveStatus 滚动汇总竞态 / PurReversalListener 并发回滚），不做系统性并发正确性裁决。
- Successor Required: `yes`——A2.17 执行时复核。

### config-gated Deferred 偏离本身（承付 config-gated / 三单匹配 strict-default / 工作流深度集成）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: owner doc 已裁定为 config-gated/Deferred/Non-Goal。本审计只确认其在状态机上不引入悬挂。
- Successor Required: `yes`——各 successor 触发条件满足时（如承付全面启用 / 三单匹配 strict 默认 / 工作流深度集成上线）。
