# ARM-MA2 sales 状态机系统业务审查报告（A2.9）

> 里程碑：MA2（业务正确性层 / 状态机正确性维度）
> Roadmap 工作项：A2.9（A 级单域，七实体 × 三轴）
> Plan：`docs/plans/2026-07-28-0400-1-audit-remediation-ma2-sales-state-machine.md`
> 行为基线：`docs/design/sales/{state-machine,returns,quotation,contract}.md` + `docs/architecture/{processor-extension-pattern,posting-exemptions}.md`
> Skill：`docs/skills/state-machine-business-review-prompt.md`（10 维度审查方法）
> 实仓快照：2026-07-28（HEAD 经 `compliance-baseline.md §M0 锚点` 验证一致）
> 裁决：**Verdict = ⚠️(P1)**——销售七实体状态机核心契约经证据确认（PROC 路径迁移守卫齐全、@BizMutation 事务回滚、出库 approve 可用量校验**经库存域 doConfirm→validateAvailable 强制落实**销售独有约束、跨域写经 I\*Biz Facade、SalReversalListener 三实体降级 + delivery 仅 posted=false deliberate 不对称）；零 P0；**新增 2 项 P1**（P1-MA2-056 Contract reverseApprove→SUBMITTED 违反 owner doc §2 强制 REJECTED 契约漂移 / P1-MA2-057 6 实体 INLINE withdrawApproval + Contract 全 INLINE 缺 isCancelled/customer active/lines empty 守卫致 CANCELLED 单据 approveStatus 副轴漂移）；**新增 3 项 P2** watch-only（P2-MA2-056 三种并行模式 + 6 实体 vs Contract 模式分裂 owner doc 未声明 / P2-MA2-057 SalReversalListener.rollbackDelivery 不对称 Javadoc deliberate owner doc 未同步 / P2-MA2-058 ErpSalReturn writtenOffStatus/returnStatus/refundStatus 未落地为存储字段[returns.md §88-93 已显式漂移注记]）；9 项已登记 MA1/MA2 finding 运行时复核**无升级**；并发敏感点 5 处交接 A2.17。

---

## 1. 范围与基线

### 1.1 在范围

七销售实体（`module-sales/model/app-erp-sales.orm.xml`，~1305 行）× 三轴（docStatus/approveStatus/业务轴）。**实仓 dict-bound 状态字段实测 18 列**（plan baseline 称 25——差异源于 plan 计入 `posted` 布尔 + `isAccepted` 布尔 + 审计字段；本审计按 dict-bound 字段统计为 18，与 owner doc §三轴状态分离 表对齐）：

| 实体 | docStatus | approveStatus | 业务轴 | posted | 备注 |
|------|-----------|---------------|--------|--------|------|
| `ErpSalOrder` | erp/doc-status | wf/approve-status | receivedStatus(erp-sal/received-status) + deliveryStatus(erp-sal/delivery-status) | — | 订单是意向，approve 仅状态推进 + 信用占用 + 承付 commit/intercompany（config-gated），无直接库存/凭证写 |
| `ErpSalDelivery` | erp/doc-status | wf/approve-status | —（订单 deliveryStatus 经 orderBiz.updateDeliveryStatus 滚动汇总回写） | ✅ | approve 触发**库存 outgoing + 可用量校验**（销售独有，经 IErpInvStockMoveBiz.generateMove→doConfirm→validateAvailable） + SALES_OUTPUT 凭证（inventory 侧 InvAcctDocProvider） |
| `ErpSalInvoice` | erp/doc-status | wf/approve-status | receivedStatus(erp-sal/received-status)（派生——由 ReceiptSettler.recomputeInvoiceReceived 按累计核销金额滚动回写） | ✅ | approve 触发 AR_INVOICE 过账（config-gated 信用 hold） |
| `ErpSalReceipt` | erp/doc-status | wf/approve-status | writtenOffStatus(复用 erp-sal/received-status)（派生——由 ReceiptSettler.recomputeReceiptWrittenOff 按累计核销金额滚动回写） | ✅ | approve 触发 RECEIPT 过账；settle 是独立动作（核销到发票，MVP 解耦） |
| `ErpSalReturn` | erp/doc-status | wf/approve-status | —（`writtenOffStatus`/`returnStatus`/`refundStatus` **未落地为 ORM 字段**——returns.md:88-93 显式漂移注记，按"派生视图"语义实现） | ✅ | approve 触发库存 incoming（IErpInvStockMoveBiz）+ SALES_RETURN 过账 + ReturnRefundOrchestrator 退款编排（红字反向核销行） |
| `ErpSalQuotation` | erp/doc-status | wf/approve-status | isAccepted（布尔，非 dict——客户确认子状态） | — | approve 仅状态推进（寻源前置，无下游业务副作用）；confirmCustomerAccepted + convertToOrder 是独立动作 |
| `ErpSalContract` | erp/doc-status | wf/approve-status | — | — | **唯一全 INLINE 实体**（无 Processor），approve 仅状态推进（合同框架，无下游业务副作用） |

### 1.2 不在范围（Non-Goals 见 plan）

- A2.2 O2C 端到端编排正确性（done；多币种/汇兑损益归 A2.2 finding P1-MA2-009）
- A2.5 finance 凭证/AR-AP 状态机（done；本审计只复核销售过账经 finance I*Biz + SalReversalListener 反向回滚的**状态机迁移**正确性）
- A4.5 pur+sal+inv+qa+crm 代码质量
- A2.17 并发与乐观锁（P2-MA2-014 并发核销）
- A4.7 view.xml drift
- config-gated Deferred 偏离本身（信用控制 config-gated / 负库存 / 多级审批链）

---

## 2. 七实体 × 三轴状态图与转换矩阵

### 2.1 审批轴 UNSUBMITTED→SUBMITTED→APPROVED→REJECTED 迁移矩阵（按实体×动作×实现路径）

> 实现路径列：**PROC** = 经大 Processor 全守卫（validateNotCancelled/validateTransition*/validateBusinessRules* + doPosting/doApprove）；**INLINE** = xbiz 脚本直设 approveStatus，仅校验 `status==='SUBMITTED'`（reject/withdraw）或 `status==='APPROVED'`（reverseApprove）。Sales 域**无平台审批 nop-wf 路径**（与 purchase Payment 双路径不同——sales Receipt submitForApproval xbiz 虽尝试启动 nopFlowId 但 xmeta 未配 wf:wfName，等同 PROC 单路径）。

| 实体 | submitForApproval | approve | reject | reverseApprove | withdrawApproval | cancel | 其他动作 |
|------|------|--------|--------|----------------|------------------|--------|---------|
| Order | PROC→SUBMITTED | PROC→APPROVED + 信用占用 + 承付 commit hook（config-gated）+ intercompany hook（config-gated） | PROC→REJECTED | PROC→**REJECTED**（清 approvedBy/At）+ 承付 release + intercompany 红冲 hook | **INLINE**→UNSUBMITTED ⚠️ 缺 isCancelled 守卫 | PROC（BizModel.cancel）→docStatus=CANCELLED + 承付 release + intercompany 红冲 | batchApprove / applyPricingRules / createFromQuotation / updateDeliveryStatus |
| Delivery | PROC→SUBMITTED | PROC→APPROVED + **可用量校验经库存域 doConfirm→validateAvailable**（销售独有）+ outgoing 移动单 + SALES_OUTPUT 过账（inventory 侧）+ order.deliveryStatus 滚动汇总 + 信用 hold（config-gated）+ 强制质检 gate | PROC→REJECTED | PROC→REJECTED + posted=false + ensureReversed（库存 reverse + posted 清零） | **INLINE**→UNSUBMITTED ⚠️ 缺 isCancelled 守卫 | PROC（BizModel.cancel）→docStatus=CANCELLED + ensureReversed | — |
| Invoice | PROC→SUBMITTED | PROC→APPROVED + tryPost AR_INVOICE + 信用 hold（config-gated）+ 承付 release-on-invoice-approve hook（config-gated，经 invoiceLine→deliveryLine→delivery→order 反查 SALES_ORDER_COMMITMENT） | PROC→REJECTED | PROC→REJECTED + posted=false + 凭证 reverse | **INLINE**→UNSUBMITTED ⚠️ 缺 isCancelled 守卫 | PROC（BizModel.cancel）→docStatus=CANCELLED + 凭证 reverse（如已过账） | — |
| Receipt | PROC(+wf 启动占位)→SUBMITTED | PROC→APPROVED + tryPost RECEIPT | PROC→REJECTED | PROC→REJECTED + posted=false + 凭证 reverse | **INLINE**→UNSUBMITTED ⚠️ 缺 isCancelled 守卫 | PROC（BizModel.cancel）→docStatus=CANCELLED + 凭证 reverse（如已过账） | settle / reverseSettlement（核销——独立动作经 ReceiptSettler） |
| Return | PROC→SUBMITTED | PROC→APPROVED + 库存 incoming + tryPost SALES_RETURN + refundOrchestrator.orchestrateRefund（红字反向核销行 + 回写 invoice receivedStatus）+ returnQtyValidator | PROC→REJECTED | PROC→REJECTED + posted=false + ensureReversed（凭证 reverse + 库存 reverse + refundOrchestrator.restoreRefund[MVP 空操作]） | **INLINE**→UNSUBMITTED ⚠️ 缺 isCancelled 守卫 | PROC（BizModel.cancel）→docStatus=CANCELLED + ensureReversed | — |
| Quotation | PROC→SUBMITTED | PROC→APPROVED（无下游副作用——寻源前置合法） | PROC→REJECTED | PROC→**REJECTED**（清 approvedBy/At） | **INLINE**→UNSUBMITTED ⚠️ 缺 isCancelled 守卫 | PROC（BizModel.cancel）→docStatus=CANCELLED | confirmCustomerAccepted / convertToOrder（PROC，APPROVED+isAccepted 前置 + 防重 + 未过期） |
| Contract | **INLINE**→SUBMITTED ⚠️ | **INLINE**→APPROVED ⚠️ | **INLINE**→REJECTED ⚠️ | **INLINE**→**SUBMITTED** ⚠️⚠️ 违反 owner doc §2 | **INLINE**→UNSUBMITTED ⚠️ | **无 cancel 动作** ⚠️ | —（ErpSalContractBizModel 是 15 行 CrudBizModel 桩） |

### 2.2 PROC vs INLINE 模式对比矩阵（同一动作两路径行为对比）

> **核心安全问题**：INLINE 路径仅校验 `status==='SUBMITTED'`（reject/withdraw/Contract-approve）或 `status==='APPROVED'`（reverseApprove），**缺失** PROC 路径的下列守卫：

| 守卫 | PROC 路径（Order/Delivery/Invoice/Receipt/Return/Quotation 4 主动作 + 全 cancel） | INLINE 路径（Contract 全 5 动作 + 6 实体 withdrawApproval） |
|------|------|------|
| `validateNotCancelled`（docStatus != CANCELLED） | ✅ 拒绝 CANCELLED 单据做任何审批迁移 | ❌ **不校验**——CANCELLED 单据的 approveStatus 副轴可漂移（P1-MA2-057） |
| `validateTransition*`（src 状态匹配） | ✅ 完整 src→target 迁移表守卫（含 idempotency `if (isApproved) return` / `if (isRejected) return`） | ✅ 仅校验 src==='SUBMITTED'/'APPROVED'（基础迁移守卫齐全） |
| `validateBusinessRules*`（业务规则） | ✅ requireCustomerActive + requireLinesNonEmpty + 信用占用/hold + 强制质检 gate + returnQtyValidator + sourceDeliveryApproved + reason 配置 | ❌ **不校验业务规则**——Contract 可在客户停用/行空时迁移审批状态；6 实体 withdrawApproval 不复核客户/行 |
| `doApprove` 触发后续业务 | ✅ 承付 commit/release + 库存写（含可用量校验）+ 过账 + intercompany + 信用占用 + 质检 gate + order.deliveryStatus 滚动汇总 + refund 编排 | ❌ **不触发任何后续业务**（Contract 是合同框架，approve 无下游副作用——合法；但须 owner doc 显式声明） |
| `doReverseApprove` 目标态 | ✅ APPROVE_STATUS_REJECTED（owner doc §2 合规） | ⚠️ **Contract 设 SUBMITTED**（违反 owner doc §2 强制 REJECTED，P1-MA2-056） |
| `doReverseApprove` 清审计字段 | ✅ 清 approvedBy/At | ✅ 清 approvedBy/At（一致） |
| `doCancel` docStatus=CANCELLED | ✅ + release 承付 + intercompany 红冲 + 凭证 reverse（如已过账）+ ensureReversed（Delivery/Return） | ⚠️ Contract **无 cancel 动作**——docStatus=CANCELLED 不可经服务层可达 |

### 2.3 业务轴派生状态（owner doc 声明为派生，非工作流）

| 派生状态 | 持有实体 | 计算逻辑 | 写入点 | 一致性裁决 |
|---------|---------|---------|--------|---------|
| `receivedStatus`（UNRECEIVED/PARTIAL/RECEIVED） | Invoice（自身）+ Order（滚动汇总） | Invoice: Σ ErpSalReceiptLine.amount / invoice.totalAmountWithTax；Order: 复用 Invoice 的派生 | ReceiptSettler.recomputeInvoiceReceived:161-177 | ✅ 派生状态正确——经聚合回写，反向核销生成负金额行自然回退 |
| `deliveryStatus`（UNDELIVERED/PARTIAL/DELIVERED） | Order（自身，delivery 无自身 deliveryStatus） | Delivery approve 后置 → Σ approved DeliveryLine.qty / orderLine.qty 滚动判定 | ErpSalDeliveryProcessor.rollupOrderDeliveryStatus:285-325 → orderBiz.updateDeliveryStatus | ✅ 滚动汇总经 `findApprovedDeliveries` 过滤 APPROVED，逻辑正确（并发竞态交接 A2.17） |
| `writtenOffStatus`（复用 erp-sal/received-status） | Receipt | Σ settled / totalAmount | ReceiptSettler.recomputeReceiptWrittenOff:179-194 | ⚠️ 字典复用——UNRECEIVED/PARTIAL/RECEIVED 语义对应"未核销/部分核销/已核销"功能等价但命名"RECEIVED"对 receipt 侧误导（与 purchase P2-MA2-055 同型，P2 watch-only 不重复登记——本审计标注 sales 侧同一漂移） |
| `isAccepted`（布尔） | Quotation | 客户确认 / convertToOrder 后置 | QuotationProcessor.doConfirmCustomerAccepted:244 + convertToOrder 后 markQuotationAccepted:254 | ✅ owner doc quotation.md:23 状态机图 ACCEPTED 是显式状态，实现以布尔承载——清晰性可接受（与 purchase Quotation 同型，已在 P2-MA2-053 涵盖） |
| `returnStatus`/`refundStatus`（returns.md:81-86 声明 dict） | Return（按设计应持有） | —（按 owner doc 应是"部分/全额退货"派生视图） | **未落地为 ORM 字段** | ⚠️ returns.md:88-93 **显式漂移注记**——`returnStatus`/`refundStatus` 两轴按"派生视图"语义实现，未加 ORM 列。列表页无法直接按 returnStatus/refundStatus 筛选。**P2-MA2-058** watch-only（owner doc 已显式登记，触发条件满足时再加 `ErpSalDeliveryLine.returnedQuantity` 冗余列） |

### 2.4 终态可达性

- **审核轴终态**：`APPROVED`（审核轴无出边，纠错需 reverseApprove→REJECTED 显式路径）
- **业务轴终态**：`docStatus=CANCELLED`（不可恢复，需重新创建）
- **唯一"回退到可修改态"路径**：`APPROVED→REJECTED`（reverseApprove），需冲销前置；目标态 REJECTED 非 UNSUBMITTED（owner doc §16.4 + state-machine.md §2/§3/§5 强制规则）
- **REJECTED→SUBMITTED→APPROVED 回环**：合法循环，退出条件是"审核通过→APPROVED 终态"
- **withdrawApproval→UNSUBMITTED→submit→SUBMITTED→approve→APPROVED 回环**：合法循环，但 INLINE withdrawApproval 缺 isCancelled 守卫（P1-MA2-057）

---

## 3. 10 维度审查裁决

> 维度编号对齐 `state-machine-business-review-prompt.md`。

### 维度 1：状态定义（裁决：**PASS**）

| 控制点 | 裁决 | 证据 |
|--------|------|------|
| 三轴组合语义（docStatus CANCELLED × approveStatus APPROVED 合法性） | PASS | CANCELLED 是 docStatus 终态，approveStatus APPROVED 是审核轴终态——二者独立演化，"已审核但已作废"语义合法（业务上经"审核后作废"路径产生，作废须先冲销已生成结果，owner doc state-machine.md §3 显式定义） |
| receivedStatus 派生状态 vs DB 持久化写时机一致性 | PASS | owner doc §收款状态机 明示「由系统根据累计已核销金额/发票金额自动计算」；ReceiptSettler.recomputeInvoiceReceived:161-177 + recomputeReceiptWrittenOff:179-194 在每次 settle/reverseSettlement 后置回写，写时机与核销动作同事务（@BizMutation 包裹），一致性保证 |
| writtenOffStatus 复用 received-status 字典语义匹配 | ⚠️ | UNRECEIVED/PARTIAL/RECEIVED 三态语义在 receipt 侧对应"未核销/部分核销/已核销"——功能等价但命名"RECEIVED"对 receipt 侧误导（与 purchase P2-MA2-055 同型 watch-only，sales 侧不重复登记新 ID，本审计标注同一漂移） |
| deliveryStatus 派生滚动汇总一致性 | PASS | ErpSalDeliveryProcessor.rollupOrderDeliveryStatus:285-325 经 `findApprovedDeliveries` 过滤 APPROVED + 按 orderLineId 聚合数量 → `orderBiz.updateDeliveryStatus` 写 order.deliveryStatus；逻辑正确（并发竞态交接 A2.17） |
| invoice receivedStatus 与 receipt 核销派生计算正确性 | PASS | recomputeInvoiceReceived 按 Σ ReceiptLine.amount（含负金额反向核销行）vs totalAmountWithTax 判定 UNRECEIVED/PARTIAL/RECEIVED；ReturnRefundOrchestrator.orchestrateRefund 经 receiptSettler.reverseSettlement 自然回退发票 receivedStatus，退款闭环一致 |
| Return `writtenOffStatus`/`returnStatus`/`refundStatus` 未落地为存储字段 | ⚠️ | returns.md:88-93 **显式漂移注记**——按"派生视图"语义实现，未加 ORM 列。**P2-MA2-058** watch-only（owner doc 已登记） |

### 维度 2：转换完整性（裁决：**FAIL**——两处 P1）

| 控制点 | 裁决 | 证据 |
|--------|------|------|
| **三种并行模式等价性**（PROC 全守卫 vs INLINE 缺守卫） | ❌ FAIL | 同一动作两路径行为不一致——INLINE 路径缺 `validateNotCancelled`/`requireCustomerActive`/`requireLinesNonEmpty` 守卫（见 §2.2 矩阵）。**登记 P1-MA2-057**（CANCELLED 单据 approveStatus 副轴漂移）+ P2-MA2-056（owner doc 未声明三模式 + 6 实体 vs Contract 模式分裂） |
| **reverseApprove 目标态矛盾** | ❌ FAIL | Contract `reverseApprove` xbiz 设 SUBMITTED（`ErpSalContract.xbiz:97`），违反 owner doc `state-machine.md §2 L43-44 + §3 L60-63 + §5 L84` + `domain-design-guidelines.md §16.4` 强制 REJECTED 规则；6 大 Processor 路径全部合规（`ErpSalOrderProcessor.doReverseApprove:224-229` / Delivery/Invoice/Receipt/Return/Quotation 大 Processor 全设 APPROVE_STATUS_REJECTED + 清 approvedBy/At）。**登记 P1-MA2-056**（契约漂移） |
| **出库 approve 可用量校验前置**（销售独有——最易遗漏） | ✅ PASS | 销售独有约束经**库存域** `ErpInvStockMoveProcessor.doConfirm:185-197 → validateAvailable:215-235` 强制落实：`if (available < required) throw ERR_AVAILABLE_INSUFFICIENT`，config-gated by `erp-inv.allow-negative-stock` 默认 false。ErpSalDeliveryProcessor.triggerOutgoingMove:256-260 经 `stockMoveBiz.generateMove` 同步调用，businessLinked=true 自动 doConfirm+doComplete；@BizMutation 事务回滚保证可用量不足时整个 delivery approve 回滚至 SUBMITTED。owner doc state-machine.md §2/§4 + §场景B 销售独有约束**已落实** |
| INLINE reject/withdrawApproval 缺守卫 | ❌ FAIL | 6 实体的 INLINE withdrawApproval 仅校验 `status==='SUBMITTED'` 后设 UNSUBMITTED，不校验 `isCancelled`——CANCELLED 单据（docStatus=CANCELLED）若 approveStatus 仍 SUBMITTED（取消前置未完整清审批），可被 withdraw 设 UNSUBMITTED（副轴漂移）。Contract 全 5 INLINE 动作同型。**同 P1-MA2-057** |
| **settle/reverseSettlement 前置**（发票 APPROVED+客户匹配+余额不超+信用未冻结） | ⚠️ | ReceiptSettler.settle:55-111 守卫齐全（receipt approveStatus=APPROVED + invoice approveStatus=APPROVED + 客户匹配 + amount ≤ invoiceBalance + amount ≤ receiptRemaining），**缺信用未冻结复核**——owner doc §场景A 隐含信用检查在 approve 时已 gate，settle 是资金动作非信用动作，**裁定可接受**（信用 hold 是 approve 时点检查，settle 不复核合理）。反向核销（reverseSettlement）经负金额 ReceiptLine 自然回退，守卫完整 |
| quotation→order 转换前置 | PASS | QuotationProcessor.convertToOrder:108-116 + 守卫 `validateReadyForConvert`（APPROVED + isAccepted）+ `requireNotExpired` + `validateNotAlreadyConverted`（经 `orderBiz.existsActiveByQuotation` 防重）齐全 |
| confirmCustomerAccepted 前置 | PASS | `validateTransitionForConfirm`（APPROVED）+ `requireNotExpired` + `validateNotCancelled` 齐全 |

### 维度 3：终端状态和恢复（裁决：**PASS**）

| 控制点 | 裁决 | 证据 |
|--------|------|------|
| docStatus CANCELLED 终态（不可恢复） | PASS | 无任何 CANCELLED→其他态迁移代码；owner doc state-machine.md §3 + returns.md §退货单状态机 一致声明。Contract **无 cancel 动作**——CANCELLED 经服务层不可达（CRUD update 路径不在状态机审计范围） |
| approveStatus REJECTED 可重新 submit | PASS | submit 守卫允许 UNSUBMITTED + REJECTED 作为 src（`ErpSalOrderProcessor.validateTransitionForSubmit:141-150` / 6 实体 INLINE withdrawApproval 等价语义；Contract INLINE submit `if (status !== 'UNSUBMITTED' && status !== null && status !== 'REJECTED')` 同型） |
| reverseApprove 红冲恢复（posted=false + APPROVED→REJECTED——非真终态可再审批） | PASS（PROC 6 实体）/ ❌（INLINE Contract） | PROC 路径：doReverseApprove 设 REJECTED + 清 approvedBy/At + posted=false + 凭证 reverse（Invoice/Receipt/Return）+ ensureReversed（Delivery/Return 含库存 reverse）——REJECTED 可经 submit 重新推进至 APPROVED（合法循环）。INLINE 路径（Contract）：设 SUBMITTED——同维度 2 P1-MA2-056 |
| receivedStatus RECEIVED 终态（再核销回退经 reverseSettlement） | PASS | reverseSettlement:116-137 经负金额 ReceiptLine 回写 receivedAmount → recomputeInvoiceReceived 自然回退 UNRECEIVED/PARTIAL |

### 维度 4：异常路径（裁决：**FAIL**——两处 P1 + SalReversalListener 不对称 P2）

| 控制点 | 裁决 | 证据 |
|--------|------|------|
| **出库可用量不足**（approve 拒绝回滚——销售独有） | ✅ PASS | 库存域 `validateAvailable:215-235` 抛 `ERR_AVAILABLE_INSUFFICIENT`，经 IErpInvStockMoveBiz.generateMove 同步传播至 ErpSalDeliveryProcessor.doApprove:221-229 → @BizMutation 事务回滚整个 approve（delivery 保持 SUBMITTED + 不写库存移动单 + 不写凭证）。owner doc §场景B 销售独有异常路径**已落实** |
| approve 已 CANCELLED 单据（PROC vs INLINE） | PARTIAL PASS | PROC 路径 `validateNotCancelled` 拒绝；INLINE 路径（Contract）无守卫。Contract approve 前置 src==='SUBMITTED'——若 CANCELLED 单据仍 SUBMITTED 可被 approve 设 APPROVED。实际危害有限（同维度 2 P1-MA2-057） |
| settle 超余额 | PASS | `ReceiptSettler.settle:82-94` 守卫 `ERR_SETTLE_OVER_INVOICE_BALANCE` / `ERR_SETTLE_OVER_RECEIPT_BALANCE` 拒绝 |
| **过账 tryPost 吞异常**（posted=false 悬挂——同 finance P1-MA2-032 IGNORED 同型） | ⚠️ | `SalInvoicePostingDispatcher.tryPost:39-52` / `SalReceiptPostingDispatcher.tryPost` / `SalReturnPostingDispatcher.tryPost` try/catch 吞所有异常返回 boolean——失败时业务侧 posted=false 永久悬挂。**与 finance A2.5a P1-MA2-032 + purchase A2.8 P1-MA2-051 + mfg/hr posting dispatcher tryPost 容错同型根因**，按既定裁决范式 P1 watch-only（不重复登记新 ID——sales 侧同一架构范式，DeferredPostingSweepJob 兜底扫描重试）。**不升 P0**——Deferred 兜底 + LOG.warn/error 可见性 |
| **退货退款红字收款单 + 回退发票状态**（owner doc §3/§9 完整性重点） | ✅ PASS | ReturnRefundOrchestrator.orchestrateRefund:49-57 经 `findReceivedInvoicesOfCustomer`（同客户 APPROVED 发票 + receivedAmount > 0）+ `reverseSettlementsForInvoice`（对每条已核销 receipt 调 `receiptSettler.reverseSettlement`）→ 生成负金额 ReceiptLine → recomputeInvoiceReceived 自然回退 receivedStatus/Amount + recomputeReceiptWrittenOff 回退 receipt.writtenOffStatus。owner doc §9 + returns.md §退款 完整性**已落实**。**MVP 缺口**：`restoreRefund:64-67` 是空操作（红冲退货时不恢复原收款核销——经红字凭证 cancelOnReverse 取消负 AR 辅助账即可，原收款核销恢复属退款方式路由 treasury Non-Goal）。MVP 缺口不破坏状态机正确性，归 treasury successor |
| 客户停用后开单 | PASS | 6 实体 PROC `requireCustomerActive`（Order/Delivery/Invoice/Receipt/Return/Quotation）守卫 `ERR_PARTNER_INACTIVE` 拒绝（Quotation 仅在 submit 时校验，approve 时不重检——可接受，submit 已 gate）；Contract 全 INLINE 无此守卫（同 P1-MA2-057） |
| **SalReversalListener 反向回滚对称性**（对称于 PurReversalListener.rollbackReceive 不对称发现） | ⚠️ | SalReversalListener:43-120：(a) `rollbackInvoice:67-79` posted=false + APPROVED→REJECTED ✓；(b) `rollbackReceipt:81-93` posted=false + APPROVED→REJECTED ✓；(c) `rollbackReturn:95-107` posted=false + APPROVED→REJECTED ✓；(d) `rollbackDelivery:109-120` **仅 posted=false 保留 APPROVED**（Javadoc:114-115 标注 deliberate：「库存物理冲销独立于凭证红冲，由业务侧 reverseApprove 链触发；财务侧红冲仅回退 posted 标志」）。**与 PurReversalListener.rollbackReceive 不对称（P1-MA2-051）完全同型**——sales 侧 delivery 是出库（与 receive 入库对称），SALES_OUTPUT 凭证由 inventory 侧 InvAcctDocProvider 过账，财务侧红冲 SALES_OUTPUT 后 delivery 保持 APPROVED+posted=false 悬挂。**登记 P2-MA2-057** watch-only（Javadoc deliberate + owner doc 未同步）——按 P1-MA2-051 同型但 sales delivery 经业务侧 reverseApprove 链可恢复（库存物理 reverse 经 `ensureReversed:270-283` 触发，与 finance 凭证红冲独立），故裁决 P2 watch-only 不升 P1（与 purchase 不同——sales delivery 业务侧恢复路径完整，purchase receive 业务侧需运营手工 reverseApprove） |
| 并发 settle 同发票（无锁——P2-MA2-014 交接 A2.17） | ⚠️ | ReceiptSettler 无悲观/乐观锁，并发核销同一发票可双读双写过收；recomputeInvoiceReceived 事后聚合不能阻止中间态过收。**P2-MA2-014 已登记，归 A2.17** |

### 维度 5：可达性（裁决：**FAIL**——一处 P1）

| 控制点 | 裁决 | 证据 |
|--------|------|------|
| **reverseApprove 经 INLINE 可达 SUBMITTED（Contract）vs PROC 可达 REJECTED（其他 6 实体）** | ❌ FAIL | 同一概念 reverseApprove 经两路径可达两不同态——契约不一致（owner doc §2 强制 REJECTED，Contract 违规→SUBMITTED）。**同 P1-MA2-056** |
| withdrawApproval→UNSUBMITTED→submit→SUBMITTED→approve→APPROVED 回环可达性 | PASS | 各迁移前置齐全（src 状态匹配），合法循环退出条件是 APPROVED 终态。INLINE withdrawApproval 缺 isCancelled 守卫不影响回环本身（同 P1-MA2-057） |
| receivedStatus UNRECEIVED→PARTIAL→RECEIVED 派生可达性 | PASS | recomputeInvoiceReceived 按 Σ amount vs totalAmountWithTax 判定，3 态均可经 settle（PARTIAL→RECEIVED）+ reverseSettlement（RECEIVED→PARTIAL→UNRECEIVED）可达 |
| Contract CANCELLED 不可经服务层可达 | ⚠️ | 无 cancel action——CANCELLED dict 项经服务层不可达（仅经 CRUD update 或 DB 直改）。**P2-MA2-056** watch-only 涵盖 |
| 死循环或不可达终态 | PASS | 无不可达状态（除 Contract CANCELLED 经 CRUD 路径，不计状态机审计）；合法循环均有退出条件 |

### 维度 6：角色和权限（裁决：**PASS**）

| 控制点 | 裁决 | 证据 |
|--------|------|------|
| 提交（销售员）/审核（销售主管）/settle（出纳/会计）/convertToOrder（销售员）角色绑定 | PASS | 各 @BizMutation 经 nop-auth 权限模型绑定角色（不在本审计范围——A4.x 平台合规已覆盖）；@BizMutation 自动事务回滚保证失败原子性 |
| 危险操作：approve 触发出库跨域库存写 + 过账跨域会计写 + 信用冻结 | PASS | Delivery.approve → IErpInvStockMoveBiz.generateMove（跨域写库存，含可用量校验）；Invoice/Receipt/Return.approve → IErpFinVoucherBiz.post（跨域写会计保护区域，经 REQUIRES_NEW Facade）；Order/Delivery/Invoice.approve → CreditLimitChecker.check/checkCreditHold（三级策略 SOFT_WARNING/HARD_BLOCK/SPECIAL_APPROVAL）——全部经 I\*Biz Facade，无 daoFor 跨域写 |
| 危险操作：settle 资金核销 | PASS | ReceiptSettler.settle 双 APPROVED 守卫 + 余额校验 + 客户匹配 |
| 危险操作：reverseApprove 红冲恢复余额 | PASS | PROC 路径 doReverseApprove 设 REJECTED + posted=false + 凭证 reverse（Invoice/Receipt/Return）+ ensureReversed（Delivery/Return 含库存 reverse）+ 承付 release hook（Order config-gated）+ refundOrchestrator.restoreRefund（Return，MVP 空操作） |
| 危险操作：cancel 已过账单据（须 reverse 凭证） | PASS | doCancel 前 Invoice/Receipt/Return 凭证 reverse（如已过账）；Delivery/Return ensureReversed（库存物理 reverse + 凭证 reverse）；Order release 承付 + intercompany 红冲 |
| 多角色冲突（销售员 approve vs 出纳 settle vs 会计 reverseApprove） | PASS | 职责分离经 @BizMutation 入口 + 权限模型保证（不在本审计范围） |

### 维度 7：外部依赖（裁决：**PASS**）

| 控制点 | 裁决 | 证据 |
|--------|------|------|
| approve→出库移动单（IErpInvStockMoveBiz 跨域写——含可用量校验） | PASS | `ErpSalDeliveryProcessor.triggerOutgoingMove:256-260` 经 `stockMoveBiz.generateMove` 同步调用——经 I\*Biz Facade，库存域 doConfirm→validateAvailable 强制校验 |
| approve→退货入库移动单（IErpInvStockMoveBiz 跨域写） | PASS | `ErpSalReturnProcessor.triggerIncomingMove:299-304` 经 `stockMoveBiz.generateMove` 同步调用 + `resolveSourceDeliveryMoveId` 设置 originReturnedMoveId 追溯链——经 I\*Biz Facade |
| 发票·收款·退货→过账（IErpFinVoucherBiz 跨域写会计保护区域） | PASS | `SalPostingExecutor.postEvent/reverse` → `IErpFinVoucherBiz.post/reverse` REQUIRES_NEW Facade——经 I\*Biz Facade |
| 客户 active 守卫（IErpMdPartnerBiz） | PASS | 6 实体 `requireCustomerActive` 经 `mdPartnerBiz.findById` 只读——经 I\*Biz Facade |
| SalReversalListener 反向（finance→sales） | ⚠️ | 监听者失败经 `ErpFinReversalListenerRegistry.dispatch` try/catch 隔离，不阻断其他域监听者；失败落入 finance 异常工作台。回退目标态表见维度 4——rollbackDelivery 不对称（P2-MA2-057），其他三实体（Invoice/Receipt/Return）回退对称 |
| 承付 commit/release（IErpFinBudgetCommitmentBiz config-gated） | PASS | `ErpSalOrderProcessor.runCommitmentCommitHook:338-352` / `runCommitmentReleaseHook:359-370`（容错对称性 catch NopException 静默跳过）+ `ErpSalInvoiceProcessor.runCommitmentReleaseOnInvoiceApproveHook:347-365`（AR 发票过账 = 实际收入产生 = 释放承付，经 invoiceLine→deliveryLine→delivery→order.code 反查 SALES_ORDER_COMMITMENT） |
| intercompany 跨法人（IErpFinIntercompanyTransferBiz config-gated） | PASS | `ErpSalOrderProcessor.runIntercompanyApproveHook:304-317` / `runIntercompanyReverseHook:323-330`（config-gated；非阻塞 try-catch） |
| 外部步骤失败是否阻断状态迁移 | PASS | @BizMutation 事务回滚保证 approve 触发的库存写/承付/过账/intercompany 跨域写失败时业务单据回滚至 SUBMITTED；过账 tryPost 吞异常路径（posted=false 悬挂）是设计容错（与 finance P1-MA2-032 同型） |

### 维度 8：TODO/任务策略（裁决：**PASS**）

| 控制点 | 裁决 | 证据 |
|--------|------|------|
| SUBMITTED 待审批 TODO | PASS | 各实体 SUBMITTED 状态产生审批 TODO（经 nop-wf 工作流或审批池分配，不在本审计范围） |
| UNRECEIVED/PARTIAL 收款 TODO | PASS | invoice receivedStatus PARTIAL 是派生态，由 ReceiptSettler.recomputeInvoiceReceived 计算；未收清时产生收款 TODO（不在本审计范围） |
| REJECTED 修改重提 TODO | PASS | reverseApprove 后 REJECTED 是合法的"等待修改后重新提交"等待点，产生 assigned TODO（销售员）；posted=false 悬挂（过账 tryPost 失败）由 DeferredPostingSweepJob 兜底扫描 |
| 赠品库存扣减 TODO | PASS | applyPricingRules 后置追加赠品行（金额 0、数量计入）→ 经标准 delivery approve 路径扣库存（赠品也要扣库存——与 owner doc §场景D 一致）。**实现无遗漏**：赠品行 amount=0 但 quantity 参与可用量校验与扣减 |
| 是否存在期望有人行动但不产生待办的状态 | PASS | Contract approve 无下游副作用，但不产生悬挂（合同框架语义清晰）；delivery posted=false 悬挂由 DeferredPostingSweepJob 兜底 |

### 维度 9：场景演练（最重要，裁决：**FAIL**——两处 P1 在场景中暴露）

> 10 个代表性场景，覆盖 owner doc state-machine.md §9 + 本审计识别风险点。

#### 场景 (a) O2C 黄金路径（裁决：**PASS**）

报价→订单 approve→出库 approve+可用量校验+库存写→发票 approve+过账→收款 approve+settle+过账：
- Quotation SUBMITTED→PROC approve→APPROVED → confirmCustomerAccepted → convertToOrder（守卫齐全）→ 生成 ErpSalOrder UNSUBMITTED
- Order SUBMITTED→PROC approve→APPROVED + 信用占用 + 承付 commit（config-gated）+ intercompany（config-gated）
- Delivery SUBMITTED→PROC approve→**库存域 doConfirm→validateAvailable 强制可用量校验**（销售独有）→ outgoing 移动单 + SALES_OUTPUT 过账（inventory 侧）+ order.deliveryStatus 滚动汇总 + 信用 hold（config-gated）
- Invoice SUBMITTED→PROC approve→APPROVED + tryPost AR_INVOICE + 信用 hold（config-gated）+ 承付 release-on-invoice-approve（config-gated）
- Receipt SUBMITTED→PROC approve→APPROVED + tryPost RECEIPT → settle（双 APPROVED + 客户匹配 + 余额校验）→ invoice receivedStatus=RECEIVED + receipt writtenOffStatus=RECEIVED

**全链状态迁移守卫齐全，跨域写经 I\*Biz Facade，事务回滚保证原子性。PASS。**

#### 场景 (b) 出库可用量不足（裁决：**PASS**——销售独有，最易遗漏）

- Delivery SUBMITTED，审核时库存域 validateAvailable 抛 `ERR_AVAILABLE_INSUFFICIENT`
- 经 IErpInvStockMoveBiz.generateMove 同步传播 → ErpSalDeliveryProcessor.doApprove 异常 → @BizMutation 事务回滚
- delivery 保持 SUBMITTED + 不写库存移动单 + 不写凭证 + 不滚动 order.deliveryStatus
- 销售员调整出库数量（分批出库）或等待库存补充后重新提交

**owner doc §场景B 销售独有异常路径已落实。PASS。**

#### 场景 (c) reverseApprove 红冲（裁决：**FAIL**——P1-MA2-056）

PROC 路径（Order/Delivery/Invoice/Receipt/Return/Quotation）：
- doReverseApprove 设 REJECTED + 清 approvedBy/At + posted=false + 凭证 reverse（Invoice/Receipt/Return）+ ensureReversed（Delivery/Return 含库存 reverse）+ refundOrchestrator.restoreRefund（Return MVP 空操作）
- 承付 release hook（Order config-gated）+ intercompany 红冲（Order config-gated）
- 与 owner doc §2 强制 REJECTED 规则一致 ✓

INLINE 路径（Contract）：
- 设 SUBMITTED + 清 approvedBy/At
- **违反 owner doc §2 强制 REJECTED 规则** ⚠️
- Contract 无 posted 副作用（不过账），不破坏红冲闭环一致性（无凭证需 reverse）
- 实际危害：契约漂移——审查者期望 reverseApprove 后处于 REJECTED（"曾审核过"语义），实际处于 SUBMITTED（"重新提交中"语义）

**契约漂移，按 finance A2.5a P1-MA2-031 + mfg A2.6a P1-MA2-035 + hr A2.7a P1-MA2-039~042 + purchase A2.8 P1-MA2-049 同型裁决 P1-MA2-056。**

#### 场景 (d) withdrawApproval 回环（裁决：**PARTIAL FAIL**——同 P1-MA2-057）

APPROVED→（reverseApprove）→REJECTED→（submit）→SUBMITTED→（withdraw）→UNSUBMITTED→（submit）→SUBMITTED→（approve）→APPROVED：
- PROC 路径 withdraw 守卫齐全
- INLINE 路径 withdraw（6 实体 + Contract 全动作）缺 isCancelled 守卫（同 P1-MA2-057）

#### 场景 (e) cancel 已过账单据（裁决：**PASS**）

Invoice/Receipt/Return 已过账后 cancel：
- BizModel.cancel → Processor.cancel → validateTransitionForCancel（拒绝已 CANCELLED）+ 凭证 reverse（如 APPROVED+posted=true）+ ensureReversed（Delivery/Return）
- 失败经 @BizMutation 事务回滚，cancel 不生效
- Order.cancel 无直接过账副作用，仅置 docStatus=CANCELLED + release 承付 + intercompany 红冲

#### 场景 (f) settle/reverseSettlement（裁决：**PASS**）

- settle：双 APPROVED + 客户匹配 + 余额校验齐全
- reverseSettlement：生成负金额 ReceiptLine，余额与状态据此自然回退（recomputeInvoiceReceived/recomputeReceiptWrittenOff）

#### 场景 (g) 退货退款（红字收款单+回退发票状态——完整性）（裁决：**PASS**）

Return approve → refundOrchestrator.orchestrateRefund：
- findReceivedInvoicesOfCustomer（同客户 APPROVED + receivedAmount > 0 发票）
- reverseSettlementsForInvoice（对每条已核销 receipt 调 reverseSettlement → 负金额 ReceiptLine）
- recomputeInvoiceReceived 回退 receivedStatus/Amount + recomputeReceiptWrittenOff 回退 receipt.writtenOffStatus
- 退款闭环一致 ✓

**owner doc §9 + returns.md §退款 完整性已落实。PASS。**

#### 场景 (h) 信用冻结 HARD_BLOCK（裁决：**PASS**）

- Order.approve → CreditLimitChecker.check（三级策略）
- Delivery/Invoice.approve → enforceCreditHold（config-gated by erp-sal.credit-check-on-delivery/invoice 默认 false 向后兼容）
- HARD_BLOCK → 抛 `ERR_CREDIT_LIMIT_EXCEEDED` / `ERR_CREDIT_HOLD_DELIVERY` / `ERR_CREDIT_HOLD_INVOICE`，approve 拒绝
- SOFT_WARNING → 派发 `NOTIFY_EVENT_CREDIT_OVER_LIMIT` 通知（config-gated）
- SPECIAL_APPROVAL → 经 `erp-sal:creditOverLimitApprove` 权限门控

**owner doc §角色权限 + CreditLimitChecker Javadoc 三级策略一致。PASS。**

#### 场景 (i) 赠品库存扣减（裁决：**PASS**）

- Order 含赠品行（amount=0、quantity 计入）+ 折扣行
- applyPricingRules 经 ErpSalPricingRuleEngine 追加赠品行 + 重算订单头合计
- Delivery.approve → triggerOutgoingMove → 库存域 doConfirm→validateAvailable：赠品 quantity 参与可用量校验与扣减（owner doc §场景D 一致，无遗漏）

#### 场景 (j) 并发 settle 同发票（无锁——P2-MA2-014，交接 A2.17）（裁决：**PASS**——交接 A2.17）

- ReceiptSettler 无悲观/乐观锁，并发核销同一发票可双读双写过收
- recomputeInvoiceReceived 事后聚合不能阻止中间态过收
- **P2-MA2-014 已登记，归 A2.17 并发与乐观锁系统性审计**

### 维度 10：与设计文档一致性（裁决：**FAIL**——三处 owner doc 漂移）

| 控制点 | 裁决 | 证据 |
|--------|------|------|
| **§2 reverseApprove→REJECTED 强制规则被 Contract xbiz 违反** | ❌ FAIL | owner doc `state-machine.md §2 L43-44 + §3 L60-63 + §5 L84` + `domain-design-guidelines.md §16.4` 多处强制 reverseApprove→REJECTED；Contract xbiz 设 SUBMITTED。**同 P1-MA2-056** |
| **三种并行模式 + 6 实体 vs Contract 模式分裂 owner doc 未声明** | ❌ FAIL | owner doc `state-machine.md` 假设单一审批状态机，未声明 PROC（6 实体 4 主动作 + 全 cancel）/INLINE（6 实体 withdrawApproval + Contract 全 5 动作）两模式并存——审查者/开发者期望单一模式行为一致，实际 INLINE 缺守卫。**登记 P2-MA2-056** watch-only（owner doc 未声明）+ P1-MA2-057（实际安全缺口） |
| **INLINE withdrawApproval + Contract 全动作缺守卫 owner doc 是否声明** | ❌ FAIL | owner doc `state-machine.md §2 迁移表` 声明迁移前置（如"已提交状态"），未声明 INLINE 路径缺 isCancelled/requireCustomerActive/requireLinesNonEmpty 守卫。**同 P1-MA2-057 + P2-MA2-056** |
| **SalReversalListener.rollbackDelivery 不对称 owner doc 是否声明** | ❌ FAIL | owner doc `state-machine.md §2 SUBMITTED→APPROVED 触发后续业务` 表列出 delivery approve 触发库存 outgoing，但未声明 SALES_OUTPUT 凭证红冲后 delivery 状态回退目标；`returns.md` 无 SalReversalListener 章节描述回退目标态表。**同 P2-MA2-057**（Javadoc deliberate 但 owner doc 未同步） |
| **returns.md `returnStatus`/`refundStatus` 未落地为 ORM 字典字段** | ❌ FAIL | returns.md:88-93 **已显式漂移注记**（"实现偏离说明"段落）—— owner doc 已主动登记此漂移，按"派生视图"语义实现。**登记 P2-MA2-058** watch-only（owner doc 已登记，触发条件满足时再加冗余列） |
| receipt writtenOffStatus 复用 received-status 字典语义 owner doc 是否声明 | ⚠️ | owner doc `state-machine.md §三轴状态分离` 表列出 receivedStatus 适用于"销售发票"，未声明 receipt 复用同一字典承载 writtenOffStatus 语义。与 purchase P2-MA2-055 同型 watch-only，sales 侧不重复登记新 ID |

---

## 4. 已登记 finding 销售状态机角度运行时复核

| Finding ID | 原描述 | 状态机角度复核 | 终态 |
|-----------|--------|--------------|------|
| `P1-MA1-022`（todo MR1，9 域合并） | sal `daoFor(ErpMdSubject/ErpFinAccountingPeriod)` 只读（`ErpSalOrderProcessor:377,389`） | 跨域只读是 budget/period 查询副作用，不破坏状态机——异常路径经 @BizMutation 事务回滚覆盖 | **不升级**（维持 P1 治理待 MR1） |
| `P1-MA2-009`（todo MR1，O2C） | 多币种 O2C + 收款核销汇兑损益未实现 | 状态迁移不涉及币种——状态机角度无影响；但 ReceiptSettler.settle 守卫完整性已复核（双 APPROVED + 客户匹配 + 余额校验齐全，缺信用复核裁定可接受——信用 hold 是 approve 时点检查） | **不升级**（GL 层 + 核销层 finding，状态机角度无影响；settle 守卫完整） |
| `P2-MA2-010`（todo MR1，sales） | 销售发票 approve 无订单-发票金额比对守卫 | 状态机角度：approve 前置守卫缺口——本审计复核 Invoice.approve `validateBusinessRulesForApprove` 仅 requireCustomerActive + enforceCreditHold（config-gated），无金额比对。**不破坏 approve 路径正确性**——金额比对是业务规则缺口（与 purchase three-way-match P1-MA2-003 同型"必要不充分守卫缺口"），按既定裁决维持 P2 | **不升级**（维持 P2，必要不充分守卫缺口） |
| `P2-MA2-011`（todo MR1，docs+sales） | returns.md §红字发票处理 doc drift | 状态机角度：退货过账路径——Return.approve→triggerPosting(SALES_RETURN) + refundOrchestrator.orchestrateRefund 状态迁移正确，未破坏 owner doc §9 退货退款闭环（红字反向核销行 + 回退 receivedStatus 已落实）。drift 在 GL 层非状态机层 | **不升级**（GL 层 finding，状态机角度无影响） |
| `P2-MA2-012`（todo MR1，docs sales） | 信用控制扩展点 owner doc 漂述 | 状态机角度：approve 信用冻结守卫——CreditLimitChecker.check/checkCreditHold 三级策略已实现 + config-gated 控制暴露面。flow-overview.md §2.2 漏述是 doc 漂移非状态机缺口 | **不升级**（doc 漂移，状态机角度信用控制已落实） |
| `P2-MA2-013`（todo MR1，sales） | 收款核销仅发票维度（订单维度预收款未实现） | 状态机角度：receipt settle 维度——ReceiptSettler.settle 守卫完整（不区分发票/订单维度），状态机无影响。订单维度预收款是功能缺口 | **不升级**（功能缺口，状态机角度无影响） |
| `P2-MA2-014`（todo MR1→A2.17） | ReceiptSettler.settle 无锁并发核销同一发票可双读双写过收 | 并发 RECEIVED 漂移——状态机角度观察并发敏感点（见 §6 并发敏感点 #1） | **交接 A2.17** |
| `P2-MA2-015`（todo MR1，docs sales+finance） | 出库-开票跨月期间配比 owner doc 漂述 | 状态机角度：无影响（期间配比归期末结账） | **不升级**（doc 漂移 + 期间配比归 A2.3） |
| `P2-MA2-038`（todo MR1，finance/sales/purchase） | 域侧-finance 双路径核销无对账守卫 | 状态机角度：双路径设计并行——ReceiptSettler（sales 域侧）+ ErpFinReconciliation（finance 侧）状态迁移一致性经 @BizMutation 事务回滚 + reverseSettlement 对称回退保证 | **不升级**（设计并行非分歧，状态机角度双路径一致） |

---

## 5. 新登记 finding

### 5.1 P1 finding（2 项，目标 MR1）

| Finding ID | 描述 | 严重性 | 修复方式 |
|-----------|------|-------|---------|
| `P1-MA2-056` | **Contract reverseApprove→SUBMITTED 违反 owner doc §2 强制 REJECTED 契约漂移**：`ErpSalContract.xbiz:97` reverseApprove 设 `entity.approveStatus = 'SUBMITTED'`，违反 owner doc `state-machine.md §2 L43-44 + §3 L60-63 + §5 L84` + `domain-design-guidelines.md §16.4` 强制 REJECTED 规则（"反审核目标态是 REJECTED 保留曾审核语义，非 UNSUBMITTED"）。所有 6 大 Processor 合规（`ErpSalOrderProcessor.doReverseApprove:224-229` + Delivery/Invoice/Receipt/Return/Quotation 全设 APPROVE_STATUS_REJECTED + 清 approvedBy/At），但 Contract 无大 Processor，xbiz 直设 SUBMITTED。**不破坏红冲闭环一致性**——Contract 是合同框架实体，**无 posted 副作用**（不过账，无凭证需 reverse），approve 也无下游业务副作用（不触发库存/承付/过账）；仅 approveStatus 审计轨迹漂移（"重新提交中" vs 期望"曾审核过"）。按 finance A2.5a P1-MA2-031 + mfg A2.6a P1-MA2-035 + hr A2.7a P1-MA2-039~042 + purchase A2.8 P1-MA2-049 同型裁决（owner doc 强制规则 + xbiz 实现漂移，不破坏主路径）。 | major（契约漂移，不破坏业务路径） | MR1 裁决——方案 A（推荐）xbiz `reverseApprove` 改设 `entity.approveStatus = 'REJECTED'`（与 6 大 Processor 对齐 + owner doc §2 合规）；方案 B 引入 Contract 大 Processor 全守卫（与 Order/Delivery 等同型，工作量大，与 P1-MA2-057 一并裁决） |
| `P1-MA2-057` | **INLINE withdrawApproval + Contract 全 INLINE 缺 isCancelled/customer active/lines empty 守卫致 CANCELLED 单据 approveStatus 副轴漂移**：6 实体的 INLINE withdrawApproval xbiz（`ErpSalOrder.xbiz:45-67` / `ErpSalDelivery.xbiz:43-65` / `ErpSalInvoice.xbiz:43-65` / `ErpSalReceipt.xbiz:51-73` / `ErpSalReturn.xbiz:43-65` / `ErpSalQuotation.xbiz:43-65`）+ Contract 全 5 INLINE 动作（`ErpSalContract.xbiz` 全文）——均仅校验 `status==='SUBMITTED'` 后设新状态，**缺失** PROC 路径的 `validateNotCancelled`（docStatus != CANCELLED）/`requireCustomerActive`/`requireLinesNonEmpty` 守卫。CANCELLED 单据（docStatus=CANCELLED）的 SUBMITTED approveStatus 可被 withdraw 设为 UNSUBMITTED（副轴漂移）。**实际危害有限**：(1) docStatus=CANCELLED 是主终态，approveStatus 副轴漂移不影响业务查询（按 docStatus=CANCELLED 过滤即可）；(2) settle/过账查询都校验 approveStatus=APPROVED，CANCELLED+UNSUBMITTED/REJECTED 不会被误纳入；(3) 不产生脏数据（仅审计轨迹混淆）。Contract 无 cancel 动作，CANCELLED 经服务层不可达，危害进一步收窄。故裁决 P1 非 P0。 | major（安全缺口，但危害有限——主终态 docStatus 持有，副轴漂移不破坏业务路径） | MR1 裁决——方案 A（推荐）将 INLINE withdrawApproval 迁移到对应大 Processor 的 `withdrawApproval` 方法（与 P1-MA2-056 一并补 Contract 大 Processor；6 实体大 Processor 已存在 `withdrawApproval` 方法，仅需 xbiz 改 `inject('processor').withdrawApproval(id, svcCtx)` 接线 + Delta beans.xml 注册 `*WithdrawApprovalProcessor`），全守卫对齐；方案 B INLINE 路径补 `isCancelled` 守卫（最小变更：xbiz 脚本前加 `if (entity.docStatus === 'CANCELLED') throw ...`）；方案 C owner doc 标注「INLINE 路径无取消守卫，CANCELLED 单据的 approveStatus 漂移不影响业务」（永久接受） |

### 5.2 P2 finding（3 项 watch-only）

| Finding ID | 描述 | 处置 |
|-----------|------|------|
| `P2-MA2-056` | **三种并行模式 + 6 实体 vs Contract 模式分裂 owner doc 未声明**：owner doc `state-machine.md` 假设单一审批状态机，未声明 PROC（6 实体 Order/Delivery/Invoice/Receipt/Return/Quotation 4 主动作 submit/approve/reject/reverseApprove + 全 cancel 经 BizModel.cancel→大 Processor）/INLINE（6 实体 withdrawApproval + Contract 全 5 动作）两模式并存——审查者/开发者期望单一模式行为一致，实际 INLINE 缺守卫（见 P1-MA2-057）。Contract 进一步分裂为「无 Processor + 无 cancel 动作 + 全 INLINE」最低实现。与 purchase P2-MA2-053 同型（owner doc 缺独立章节/未声明）。 | watch-only，MR1 顺手——方案 A（推荐）`state-machine.md` 新增「§实现模式」章节声明两模式 + INLINE 模式的守卫边界（仅 src 状态校验，无 isCancelled/业务规则守卫）+ Contract 最低实现注记；方案 B 交叉链接到 `processor-extension-pattern.md` |
| `P2-MA2-057` | **SalReversalListener.rollbackDelivery 不对称 Javadoc deliberate owner doc 未同步**：`SalReversalListener.rollbackDelivery:109-120` 仅设 posted=false 保留 APPROVED（Javadoc:114-115 标注 deliberate：「库存物理冲销独立于凭证红冲，由业务侧 reverseApprove 链触发；财务侧红冲仅回退 posted 标志」），与 `rollbackInvoice:67-79` / `rollbackReceipt:81-93` / `rollbackReturn:95-107` 全部降级 APPROVED→REJECTED 不对称。若 finance 红冲 delivery 的 SALES_OUTPUT 凭证（由 inventory 侧 InvAcctDocProvider 过账）后，delivery 保持 APPROVED+posted=false 悬挂。**与 purchase P1-MA2-051（PurReversalListener.rollbackReceive）完全同型**，但 sales 侧裁决**降为 P2 watch-only**：(1) delivery 业务侧恢复路径完整——`ErpSalDeliveryProcessor.ensureReversed:270-283` 经 `stockMoveBiz.reverse` 触发库存物理 reverse（与 finance 凭证红冲独立），运营或自动 sweep 可触发业务侧 reverseApprove 链恢复；(2) Javadoc deliberate 标注设计意图清晰；(3) 不破坏业财一致（凭证已红冲，GL 平衡；库存物理冲销经业务侧 reverseApprove 独立；仅 sales 域 delivery 状态短暂悬挂）。 | watch-only，MR1 与 purchase P1-MA2-051 一并裁决——方案 A rollbackDelivery 与其他三实体对齐：`if (approveStatus == APPROVED) setApproveStatus(REJECTED)` + 更新 owner doc `state-machine.md`/`returns.md` 描述回退目标态表（delivery 也降级 REJECTED）；方案 B owner doc 标注「rollbackDelivery 仅 posted=false 保留 APPROVED 是设计并行（库存物理冲销独立），delivery 悬挂经业务侧 reverseApprove 链恢复」（永久接受 deliberate 不对称，与 purchase P1-MA2-051 方案 B 一致） |
| `P2-MA2-058` | **ErpSalReturn `writtenOffStatus`/`returnStatus`/`refundStatus` 未落地为 ORM 存储字段**：owner doc `returns.md §退货单状态机 §三轴状态分离:81-86` 表声明 `returnStatus`/`refundStatus` 两轴，但 ORM `app-erp-sales.orm.xml:857-886` ErpSalReturn 实体**仅有 docStatus + approveStatus 两轴**（无 returnStatus/refundStatus/writtenOffStatus 列）。returns.md:88-93 **已显式漂移注记**（"实现偏离说明（计划 0456-2）"段落）——按"派生视图"语义实现：「部分/全额退货」是源出库行累计退货进度的派生视图（按 ErpSalDeliveryLine 聚合已审核退货行 SUM 计算）；「退款进度」是 AR 辅助账 ErpFinArApItem 的 open/reconciled 状态的派生视图。残留风险：列表页无法直接按 returnStatus/refundStatus 筛选。**owner doc 已主动登记此漂移**，本审计仅交叉登记。 | watch-only，MR1 顺手——owner doc returns.md:88-93 触发条件满足时（退货/退款报表需高频筛选）再评估加 `ErpSalDeliveryLine.returnedQuantity` 冗余列 + 重新 codegen。本期维持派生视图实现（implementation-only，无 ORM 保护区域变更） |

---

## 6. 并发敏感点（交接 A2.17）

| 序号 | 位置 | 描述 | 处置 |
|-----|------|------|------|
| 1 | `ReceiptSettler.settle:55-111` | 「读 invoiceBalance→写 ReceiptLine」无悲观/乐观锁，并发核销同一发票可双读双写过收；recomputeInvoiceReceived 事后聚合不能阻止中间态过收 | 已登记 P2-MA2-014，归 A2.17 |
| 2 | `ErpSalDeliveryProcessor.rollupOrderDeliveryStatus:285-325`（order.deliveryStatus 滚动汇总） | 多个 Delivery 并发 approve 同一 Order 时，`findApprovedDeliveries` 读 + `orderBiz.updateDeliveryStatus` 写无锁，并发场景下 deliveryStatus 滚动汇总可能 stale read | 归 A2.17 |
| 3 | `SalReversalListener` 并发回滚 | finance 红冲 + 域侧 reverseApprove 并发触发同一 invoice/receipt/return/delivery 时，posted/approveStatus 写入竞态 | 归 A2.17 |
| 4 | `ErpSalOrderProcessor.approve` / `ErpSalDeliveryProcessor.approve` / 等双 approve 幂等 | `if (entity.isApproved()) return entity;` + 乐观锁 `@Version`（ErpSalOrder/Delivery/Invoice/Receipt/Return/Quotation/Contract 均声明 versionProp）→ detectable conflict | 已降级（@Version 透明乐观锁） |
| 5 | `ReturnRefundOrchestrator.orchestrateRefund` 并发退款 | 退货 approve + 收款核销并发触发同一 invoice 的 reverseSettlement 时，ReceiptLine 写入竞态 | 归 A2.17 |

> **重要事实**：ErpSalOrder/ErpSalDelivery/ErpSalInvoice/ErpSalReceipt/ErpSalReturn/ErpSalQuotation/ErpSalContract 均声明 `versionProp="version"`（透明乐观锁），将 silent lost-update 降级为 detectable conflict。

---

## 7. 综合裁决

### 7.1 Verdict

**⚠️(P1)**——销售七实体状态机核心契约经实仓逐项证据确认（PROC 路径迁移守卫齐全、@BizMutation 事务回滚、**出库 approve 可用量校验经库存域 doConfirm→validateAvailable 强制落实销售独有约束**、跨域写经 I\*Biz Facade、SalReversalListener 三实体降级 + delivery 仅 posted=false deliberate 不对称）；**零 P0**（四个候选 P0 经证据证伪或降级：(1) Contract reverseApprove→SUBMITTED 违反 owner doc §2 但不破坏红冲闭环一致性——Contract 无 posted 副作用，按同型裁决 P1；(2) INLINE withdrawApproval + Contract 全 INLINE 缺守卫但不破坏主终态——docStatus=CANCELLED 持有，approveStatus 副轴漂移不影响业务查询，按危害有限 P1；(3) SalReversalListener.rollbackDelivery 不对称但 Javadoc deliberate + 业务侧恢复路径完整（与 purchase P1-MA2-051 不同——sales delivery 经 ensureReversed 链可恢复）+ 不破坏业财一致，按 P2 watch-only；(4) 过账 tryPost 吞异常悬挂与 finance P1-MA2-032 + purchase P1-MA2-051 同型根因，Deferred 兜底，不升 P0）；**新增 2 项 P1**（P1-MA2-056/057）+ **新增 3 项 P2** watch-only（P2-MA2-056/057/058）；9 项已登记 MA1/MA2 finding 运行时复核**无升级**；并发敏感点 5 处交接 A2.17。

### 7.2 状态机正确性维度 sal 列推进

`❓` → **`⚠️(P1)`**（销售七实体状态机迁移正确性经审计确认 + 2 项 P1 待 MR1：P1-MA2-056 Contract reverseApprove 目标态矛盾 / P1-MA2-057 INLINE 缺守卫；3 项 P2 watch-only：P2-MA2-056 模式分裂 owner doc 未声明 / P2-MA2-057 rollbackDelivery 不对称 deliberate / P2-MA2-058 Return 业务轴字段未落地[owner doc 已登记]；9 项 MA1/MA2 finding 运行时复核无升级；并发敏感点 5 处交接 A2.17）。

### 7.3 残留风险

- **Contract reverseApprove→SUBMITTED 契约漂移**（P1-MA2-056）：审查者/自动化脚本期望 reverseApprove 后处于 REJECTED（与 6 大 Processor 一致），Contract 实际处于 SUBMITTED——若未来添加按 approveStatus 过滤的"曾审核过"业务查询，Contract 会误入"重新提交中"集合。MR1 修复时建议方案 A（xbiz 改设 REJECTED）。
- **INLINE 路径守卫缺口**（P1-MA2-057）：CANCELLED 单据的 approveStatus 副轴漂移——若未来添加按 approveStatus 过滤的业务查询（如"所有 SUBMITTED 单据"包含 CANCELLED+SUBMITTED），可能产生意外结果。MR1 修复时建议方案 A（迁移到 Processor）。
- **SalReversalListener.rollbackDelivery 不对称**（P2-MA2-057）：delivery APPROVED+posted=false 短暂悬挂——业务侧 reverseApprove 链可恢复，但若运营不熟悉该路径，delivery 可能短期悬挂。MR1 与 purchase P1-MA2-051 一并裁决。
- **Return 业务轴字段未落地**（P2-MA2-058）：列表页无法按 returnStatus/refundStatus 筛选——退货/退款报表高频需求出现时再加冗余列。
- **A2.17 并发审计未覆盖**：本审计仅标注并发敏感点，系统性并发正确性裁决归 A2.17。
- **A4.5 代码质量审计未覆盖**：Processor 代码质量（异常处理/N+1/索引/辅助方法）归 A4.5。
- **A4.7 view.xml drift 未覆盖**：销售页面契约漂移归 A4.7。

### 7.4 范围内已覆盖 / 范围外已交接

| 范围 | 状态 |
|------|------|
| 七实体 × 三轴状态机迁移正确性 | ✅ 已审计 |
| PROC vs INLINE 模式等价性 | ✅ 已审计 |
| reverseApprove 目标态矛盾 | ✅ 已审计（P1-MA2-056） |
| 出库 approve 可用量校验（销售独有） | ✅ 已审计（**已落实——经库存域 doConfirm→validateAvailable**） |
| settle/reverseSettlement 守卫 | ✅ 已审计（守卫齐全，缺信用复核裁定可接受） |
| 退货退款红字收款单 + 回退发票状态 | ✅ 已审计（**已落实——ReturnRefundOrchestrator**） |
| SalReversalListener rollback 不对称 | ✅ 已审计（P2-MA2-057 deliberate） |
| MA1/MA2 finding 状态机角度复核 | ✅ 已审计（无升级） |
| 并发敏感点 | ⚠️ 标注，交 A2.17 |
| 代码质量 | ❌ 交 A4.5 |
| view.xml drift | ❌ 交 A4.7 |
| O2C GL 正确性 | ❌ 交 A2.2 finding（已 done） |

---

## 8. 参考

- `docs/design/sales/state-machine.md`（owner doc，三轴设计 + §2 reverseApprove→REJECTED 强制规则 + §4 出库可用量校验销售独有 + §9 退货退款红字收款单 + 收款派生状态机）
- `docs/design/sales/returns.md`（退货状态机 + 红字收款单 + :88-93 漂移注记）
- `docs/design/sales/quotation.md`（报价→订单转换）
- `docs/design/sales/contract.md`（销售合同状态轴）
- `docs/architecture/processor-extension-pattern.md`（Facade+Processor 两层）
- `docs/architecture/posting-exemptions.md`（销售过账跨域写豁免登记）
- `docs/skills/state-machine-business-review-prompt.md`（10 维度审查方法）
- `docs/plans/2026-07-28-0400-1-audit-remediation-ma2-sales-state-machine.md`（本审计 plan）
- 关联审计：`docs/audits/2026-07-28-0230-arm-ma2-purchase-state-machine.md`（A2.8 purchase 状态机审查范式——PROC/INLINE 模式对比 + reverseApprove→SUBMITTED 同型 P1-MA2-049 + PurReversalListener.rollbackReceive 不对称 P1-MA2-051 同型）/ `docs/audits/2026-07-27-1949-arm-ma2-order-to-cash-e2e.md`（A2.2 O2C 端到端 done）/ `docs/audits/2026-07-27-2211-arm-ma2-finance-posting-voucher-state-machine.md`（A2.5a 凭证 tryPost 吞异常悬挂 P1-MA2-032 同型）
