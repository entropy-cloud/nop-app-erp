# 2026-08-13-0810-1-purchase-docstatus-m4-state-machine-bean 采购入库/发票/付款/退货单 ErpPurReceive/Invoice/Payment/Return.docStatus 实体级状态机 Bean（M4.13 + M4.15 + M4.17 + M4.19）

> Plan Status: draft
> Review Hold: §11.2 M4 (i) 人工/owner-doc 门控待确认——本计划触及受保护采购业财过账行为（cancel 路径在 approved+posted 时逆转存货移动 + 红字凭证：Receive/Return 经 `IErpInvStockMoveBiz` 逆转；Invoice/Payment 经 `PurInvoice/PurPaymentPostingDispatcher.reverse`→`IErpFinVoucherBiz.reverse`；`PurReversalListener` 回写 posted=false + APPROVED→REJECTED，已由起草者经 live code 实证）。M4 plan-first 门控成立；该人工裁定非起草者可自主解除（project-context.md 会计/财务保护域硬停止）。计划格式/完备性/范围/结束证据就绪后，保持 `draft` 直至门控确认。
> Last Reviewed: 2026-08-13
> Source: `docs/backlog/entity-state-machine-migration-roadmap.md` 工作项 M4.13（ErpPurReceive.docStatus）+ M4.15（ErpPurInvoice.docStatus）+ M4.17（ErpPurPayment.docStatus）+ M4.19（ErpPurReturn.docStatus），均 plan-first；M0.2 清单行 `docs/analysis/2026-08-12-entity-state-axis-inventory.md §3.5 purchase`（440 行段）
> Related: M4 plan-first 先例 `2026-08-13-0805-3-erpprj-timesheet-settlement-state-machine-beans.md`（§11.2 M4 硬约束 (i)–(v) + 人工门控 honest framing）；M0.1 契约 `2026-08-12-0617-1-entity-state-machine-m0-1-contract.md`（done）+ M1.3 模板 `2026-08-12-0738-2-cs-ticket-state-machine-pilot-evaluation.md`（done）；purchase 域已迁移先例 `2026-08-12-0918-1-purchase-docstatus-state-machine-bean.md`（M2.5–M2.8 docStatus）+ `2026-08-13-0945-1-purchase-approvestatus-state-machine-bean.md`（M3.2–M3.5 approveStatus）；姊妹 M4 计划 `2026-08-13-0810-2-sales-docstatus-m4-state-machine-bean.md`、`2026-08-13-0810-3-inventory-docstatus-m4-state-machine-bean.md`
> Mission: entity-state-machine
> Work Item: M4.13 + M4.15 + M4.17 + M4.19
> Audit: required
>
> **治理声明（§11.2 M4）**：本计划按 M4 plan-first 约束执行。4 实体 cancel 路径在 approved+posted 时逆转存货移动（Receive/Return→`IErpInvStockMoveBiz`）与红字凭证（Invoice→AP_INVOICE / Payment→PAYMENT / Return→PURCHASE_RETURN，经 `*PostingDispatcher.reverse`→`IErpFinVoucherBiz.reverse`），属财务影响保护区。声明 §11.2 M4 硬约束：(i) plan-first + 受保护行为人工/owner-doc 门控；(ii) 过账时序/编排/失败回退（posted 回写）/红冲闭环不改，继续由 `*PostingDispatcher` + `PurReversalListener` + `posted` 契约管理；(iii) `posted` 不入轴；(iv) 跨域副作用（`IErpInvStockMoveBiz`、`IErpFinVoucherBiz`、commitment-restore）保留原 Processor/`I*Biz` 路径；(v) 既有红冲/reversal-listener 回写闭环以 `posted`+`approveStatus` 为契约不改。本计划是 plan-first 产物（满足 (i) 的 plan 要件），人工/owner-doc 确认门控未满足前保持 `draft`。
>
> **规则 14 bundling 声明**：M4.13（Receive）+ M4.15（Invoice）+ M4.17（Payment）+ M4.19（Return）属同一组件（同一 owner doc `docs/design/purchase/state-machine.md`、同一 `erp/doc-status` dict、同一「cancel: DRAFT→CANCELLED；ACTIVE 死状态」docStatus 行为契约、同一结果表面 = 采购单据 docStatus 生命周期），按指南规则 14 合并为单计划，而非一实体一计划。docStatus 轴与 approveStatus 轴（M4.14/16/18/20）结果表面不同（不同字段/矩阵/Bean），按既定 M2/M3 先例分计划。

## Current Baseline

> 按 M0.2 清单 `docs/analysis/2026-08-12-entity-state-axis-inventory.md §3.5 purchase`（440 行段）+ 实仓核实。

- **共享 dict**：`erp/doc-status`（`module-common-service/.../dict/erp/doc-status.dict.yaml`）3 值 `DRAFT/ACTIVE/CANCELLED`。常量 `ErpPurDocStatus.DOC_STATUS_*`（`erp-pur-dao/.../constants/ErpPurDocStatus.java:22-24`）+ `ErpPurConstants extends ErpPurDocStatus`（`erp-pur-service/.../ErpPurConstants.java:14`）。
- **docStatus 行为契约（4 实体一致，实仓核实）**：4 实体的 5 个审批动作（`submitForApproval/approve/reject/reverseApprove/withdrawApproval`，各实体 xbiz INLINE→per-mutation Processor）**只写 approveStatus，从不写 docStatus**。docStatus 的**唯一生产 writer = `cancel` 路径 → CANCELLED**。`ACTIVE`（已生效）dict 有值但**零生产 writer**（死状态，与 Order/Quotation M2 先例一致）。故每实体 docStatus 矩阵 = 单边 `cancel: DRAFT → CANCELLED`。
- **迁移模板（已 done，复刻）**：`ErpPurOrderDocumentStateMachine`（`erp-pur-service/.../statemachine/ErpPurOrderDocumentStateMachine.java`，单边 cancel(DRAFT→CANCELLED)；`assertCanCancel` 抛 common 码 `ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION` 携 `action`/`fromStatus`）+ `ErpPurOrderCancelProcessor`（注入 Bean；`validateTransitionForCancel` 捕 common 码映射域码 `ERR_ORDER_ILLEGAL_DOC_STATUS_TRANSITION`；`cancelledDocStatus()` 回 `stateMachine.cancelTargetStatus()`）。4 实体复刻此范式。
- **实体一：ErpPurReceive**（`app-erp-purchase.orm.xml:683`），`docStatus` `ext:dict="erp/doc-status"`（`:703`）+ `approveStatus`（`:704`）+ `receiveStatus`（`:701`）+ `posted`（`:705`）。
  - cancel writer：`ErpPurReceiveCancelProcessor.cancel()`（`erp-pur-service/.../processor/ErpPurReceiveCancelProcessor.java:30` `setDocStatus(DOC_STATUS_CANCELLED)`，内联整个 cancel 流程：validateTransitionForCancel `:25` → 若 approved 逆转 stock move `:26-29` → setDocStatus `:30`）。守卫在主 Processor `ErpPurReceiveProcessor.validateTransitionForCancel:158-163`（已 CANCELLED 抛非法）。
  - 过账/副作用：approve 触发入库移动单 `ErpPurReceiveProcessor.triggerIncomingMove:281-285`→`IErpInvStockMoveBiz.generateMove`（库存域 InvAcctDocProvider 持有 PURCHASE_INPUT 过账；purchase 域无 PurReceivePostingDispatcher）。cancel approved 逆转 stock move。`PurReversalListener.rollbackReceive`（`erp-pur-service/.../posting/PurReversalListener.java:112-126`）回写 `posted=false` + `APPROVED→REJECTED`，**不触 docStatus**。
  - 错误码：`ErpPurErrors.ERR_ILLEGAL_DOC_STATUS_TRANSITION`（`erp-pur-service/.../ErpPurErrors.java:53`，`erp.err.pur.illegal-doc-status-transition`，**无 RECEIVE_ 前缀的泛型命名**，msg 硬编码「入库单」，携 `{receiveCode}/{currentDocStatus}/{expectedDocStatus}`；注意 `:49` 是 approveStatus 兄弟码 `ERR_ILLEGAL_STATUS_TRANSITION`）。
- **实体二：ErpPurInvoice**（`:812`），`docStatus`（`:831`）+ `approveStatus`（`:832`）+ `paidStatus`（`:833`，`erp-pur/paid-status`）+ `posted`（`:834`）。
  - cancel writer：`ErpPurInvoiceCancelProcessor.cancel()`（`ErpPurInvoiceCancelProcessor.java:24-40`，编排 validateTransitionForCancel `:26`→ 若 posted `postingDispatcher.reverse` `:30-36` → `processor.doCancel` `:37`）。docStatus 实际写在主 Processor `ErpPurInvoiceProcessor.doCancel:221-224` `setDocStatus(CANCELLED)`（**经 cancel 路径调用**）。守卫 `ErpPurInvoiceProcessor.validateTransitionForCancel:160-165`。
  - 过账：`PurInvoicePostingDispatcher`（businessType `AP_INVOICE`，`ErpFinBusinessType:16`），approve→`tryPost`→`IErpFinVoucherBiz.post`；cancel/reverse→`reverse`。`PurReversalListener.rollbackInvoice:70-82` 回写 posted=false + APPROVED→REJECTED。
  - 错误码：`ERR_INVOICE_ILLEGAL_DOC_STATUS_TRANSITION`（`ErpPurErrors.java:134`，`erp.err.pur.invoice-illegal-doc-status-transition`）。
- **实体三：ErpPurPayment**（`:921`），`docStatus`（`:939`）+ `approveStatus`（`:940`）+ `writtenOffStatus`（`:941`，复用 paid-status dict）+ `posted`（`:942`）+ `nopFlowId`（`:954`，`useWorkflow="true"`）。
  - cancel writer：`ErpPurPaymentCancelProcessor.cancel()`（`ErpPurPaymentCancelProcessor.java:24-38`，编排 validateTransitionForCancel `:26`→ 若 posted `postingDispatcher.reverse` `:28-35` → `processor.doCancel` `:36`）。docStatus 写在主 Processor `ErpPurPaymentProcessor.doCancel:248-251`。守卫 `ErpPurPaymentProcessor.validateTransitionForCancel:144-148`。
  - 过账：`PurPaymentPostingDispatcher`（businessType `PAYMENT`，`:18`）。`PurReversalListener.rollbackPayment:84-96` 回写 posted=false + APPROVED→REJECTED。
  - 错误码：`ERR_PAYMENT_ILLEGAL_DOC_STATUS_TRANSITION`（`ErpPurErrors.java:162`，`erp.err.pur.payment-illegal-doc-status-transition`）。
- **实体四：ErpPurReturn**（`:1027`），`docStatus`（`:1045`）+ `approveStatus`（`:1046`）+ `posted`（`:1047`）。**无 paidStatus/receiveStatus**（4 实体中最简）。
  - cancel writer：`ErpPurReturnCancelProcessor.cancel()`（`ErpPurReturnCancelProcessor.java:31` `setDocStatus(CANCELLED)`，内联整个 cancel：validateTransitionForCancel `:25` → 若 approved 逆转 stock move + 过账 `:26-30` → setDocStatus `:31`）。守卫主 Processor `ErpPurReturnProcessor.validateTransitionForCancel:175-179`。
  - 过账：`PurReturnPostingDispatcher`（businessType `PURCHASE_RETURN`，`:27`）+ 出库 stock move（`IErpInvStockMoveBiz`）。`PurReversalListener.rollbackReturn:98-110` 回写 posted=false + APPROVED→REJECTED。
  - 错误码：`ERR_RETURN_ILLEGAL_DOC_STATUS_TRANSITION`（`ErpPurErrors.java:198`，`erp.err.pur.return-illegal-doc-status-transition`）。
- **两种 cancel 编排形态（影响接线点）**：Receive/Return 的 `CancelProcessor.cancel()` **整体覆写并内联 setDocStatus**，Bean 接线点 = CancelProcessor 的 validateTransitionForCancel（同 Order 先例）；Invoice/Payment 的 `CancelProcessor.cancel()` **委托回主 Processor**（`processor.validateTransitionForCancel` + `processor.doCancel`），Bean 接线点 = 主 Processor 的 validateTransitionForCancel。
- **死 doCancel 辅助 + facade 守卫残留**：`ErpPurReceiveProcessor.doCancel:270` 与 `ErpPurReturnProcessor.doCancel:234` 写 CANCELLED 但**不在 cancel 路径**（CancelProcessor 内联自己的写）；同理 `ErpPurReceiveProcessor.validateTransitionForCancel:158-163` / `ErpPurReturnProcessor.validateTransitionForCancel:175-179` 为 facade 残留（live cancel 守卫由 CancelProcessor 继承骨架持有，同 Order 先例 2026-08-12-0918-1）。Phase 3 layer-2 四方对照显式裁定其为非 live 残留 + Decision（保持原状不双写，避免引入双写）。
- **生产 Bean 注册**：`erp-pur-service/.../beans/app-service.beans.xml` 已注册 4 Document SM（Order/Requisition/Quotation/Rfq，`:176-183`）+ 4 Approval SM（`:186-193`）+ 4 实体各 6 per-mutation Processor（Receive `:115-126` / Return `:127-138` / Invoice `:139-150` / Payment `:151-162`）。**4 实体 docStatus SM Bean 未注册**（greenfield）。新 4 Bean 追加于 Document SM 段。
- **既有测试（层 3 回归基线）**：Receive `TestErpPurReceiveApproval.testCancelFromDraft:111`（断言 CANCELLED `:120`）；Invoice `TestErpPurInvoiceApproval.testCancelFromDraft:125`（`:133`）；Return `TestErpPurReturnApproval.testCancelApprovedReversesMove:169`（`:180`）。**Payment 无 cancel 集成测试**（`TestErpPurPaymentApproval` 仅 submit/approve/reverseApprove/illegalTransition——回归缺口）。跨域 `TestPurReversalListenerReceiveRollback`、`TestErpPurFinanceReversalWriteback`。**无矩阵测试**（4 实体均无 `TestErpPur*StateMachineMatrix`）。common 层码 `ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION` 已存在。
- **合规基线**：`@Inject private` 须保持 R5=0。本计划保持 R5=0、R11 不增。
- **owner doc 覆盖**：`docs/design/purchase/state-machine.md §适用对象`（5 类采购单据）+ `§三轴状态分离`（docStatus=单据生命周期）。docStatus 轴的「ACTIVE 死状态」与 owner doc §1「已审核=终态」描述以 approveStatus 表达——docStatus ACTIVE 无业务入口（Decision 登记）。

## Goals

- 落地 4 个无状态 Bean：`ErpPurReceiveDocumentStateMachine` / `ErpPurInvoiceDocumentStateMachine` / `ErpPurPaymentDocumentStateMachine` / `ErpPurReturnDocumentStateMachine`（各 docStatus 单轴），遵循 §1 命名 + §2 无状态约束，各可经 Delta 同名覆盖。
  - 各矩阵：`cancel: {DRAFT} → CANCELLED`。分类 initial=`{DRAFT}`、terminal=`{CANCELLED}`、`transitions()`=1 边。`ACTIVE` 为死状态（dict 有值零 writer），Bean 不编码 ACTIVE 入边（同 Order 先例），javadoc 标注。
- 将 4 实体 cancel 路径的固定来源态/目标态守卫改调 Bean 委托（Receive/Return 接 CancelProcessor；Invoice/Payment 接主 Processor）；**动态业务守卫与副作用保留原位**（stock move 逆转、过账 dispatcher reverse、commitment-restore、`PurReversalListener` 回写、乐观锁、Payment workflow）。
- 保持全部既有外部行为不变（错误码 + 参数、迁移边、过账时序/失败回退/红冲 listener 回写、receiveStatus/paidStatus/writtenOffStatus 不触）。
- 各新增层 1 矩阵完备性表驱动测试；层 3 既有集成测试回归全绿；**补 Payment cancel 路径回归测试**（填补既有缺口）。
- 层 2 四方对照：确认 DRAFT/CANCELLED 可达 + ACTIVE 死状态裁定 + reversal-listener 不触 docStatus + 两种 cancel 编排形态接线正确。

## Non-Goals

- 不迁移 `posted`、`approveStatus`、`receiveStatus`/`paidStatus`/`writtenOffStatus`（§11.2 M4 (iii)；approveStatus 轴属 M4.14/16/18/20 另计划）。
- 不改变 `*PostingDispatcher` 过账编排、`PurReversalListener` 回写语义、stock move 生成/逆转时序、commitment-restore（§11.2 M4 (ii)/(iv)/(v)）。
- 不修改 `model/*.orm.xml`、字典值或 API 契约（ACTIVE 死状态保留 dict 值不改绑）。
- 不迁移 purchase 其余轴（approveStatus M4.14/16/18/20——另计划）。
- 不重命名 Receive 的泛型错误码 `ERR_ILLEGAL_DOC_STATUS_TRANSITION`（路线图 Non-Goal「不借迁移改变既有错误码」）；错误码语义对齐 successor（同 M3.15 JobCard 先例）。
- 不引入通用 CRUD 对 docStatus 写入的运行时禁止（M0.1 successor）。
- 不自主跳过 M4 plan-first 人工/owner-doc 门控（§11.2 M4 (i)）：门控未确认前计划保持 `draft`。
- 不证 Delta 覆盖（M4 保护域单项，归 M5.3）。

## Task Route

- Type: `implementation-only change`（消费 M0.1 契约 + M1.3 模板 + M0.2 清单 + Order/Quotation M2 先例；落地 4 轴 Bean + 接线 + 三层测试 + 四方对照；不改契约/模型/公共 API；**M4 plan-first**——cancel 路径逆转采购业财过账/存货移动）
- Owner Docs: `docs/architecture/entity-state-machine-bean.md`（M0.1 契约 + §11 迁移模板 + §11.2 M4 变体 + §3 posted 不入轴 + §8 死状态）、`docs/design/purchase/state-machine.md`（§适用对象 + §三轴状态分离 + §reversal listener 回退目标态表）、`docs/analysis/2026-08-12-entity-state-axis-inventory.md`（M0.2 清单 §3.5 purchase）、`docs/architecture/processor-extension-pattern.md`、`docs/plans/2026-08-13-0805-3-erpprj-timesheet-settlement-state-machine-beans.md`（M4 plan-first 先例）
- Skill Selection Basis: 路线图 M4.13/15/17/19 指定 `nop-backend-dev` + `nop-testing`。`nop-backend-dev` 匹配「CancelProcessor/facade 接线、两种 cancel 编排形态、过账/stock move 副作用保留、错误码 common→域映射、`@Inject` 非 private」；`nop-testing` 匹配「矩阵表驱动测试 + 既有集成回归 + Payment cancel 缺口补全」。层 2 引用 `state-machine-business-review-prompt.md`。必需输入均已就绪。

## Infrastructure And Config Prereqs

- **M4 plan-first 人工/owner-doc 门控（阻塞前置，§11.2 M4 (i)）**：本计划触及受保护采购业财过账行为（cancel approved+posted 逆转存货移动 + 红字凭证）。在人工/owner-doc 确认「以行为保持的矩阵集中化方式迁移此 4 轴、过账/stock move/reversal-listener 路径完整保留」可接受前，计划保持 `draft`，不得进入实施。门控记录须写入本计划 Draft Review Record。
- 无端口/环境变量/CORS/密钥/.env/外部服务依赖（除既有 commitment/budget 配置，保留不动）。无数据迁移。

## Execution Plan

### Phase 1 - 4 个 StateMachine Bean + 注册 + 层 1 矩阵完备性测试

Status: planned
Targets: `module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/statemachine/{ErpPurReceiveDocumentStateMachine,ErpPurInvoiceDocumentStateMachine,ErpPurPaymentDocumentStateMachine,ErpPurReturnDocumentStateMachine}.java`（新建）、`.../beans/app-service.beans.xml`（注册 4 Bean）、`.../statemachine/TestErpPurReceiveInvoicePaymentReturnDocumentStateMachines.java`（新建）
Skill: `nop-backend-dev`（Bean 形状/注册）+ `nop-testing`（层 1 表驱动测试）

- Item Types: `Add | Decision | Proof`
- Prereqs: M1.3 done

- [ ] 新建 4 个无状态 DocumentStateMachine，矩阵一致：`assertCanCancel(DRAFT)`→CANCELLED；分类 initial=`{DRAFT}`、terminal=`{CANCELLED}`、`transitions()`=1 边。**ACTIVE 死状态不编码入边**（javadoc 标注：dict 有 ACTIVE 但零生产 writer，同 Order 先例）。非法来源态（CANCELLED/ACTIVE）抛 common 码 `ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION` 携 `action`/`fromStatus`。grep 证实不 import DAO/IBiz/IServiceContext/事务。
  - Skill: `nop-backend-dev`
- [ ] Decision（前置）：记录 ACTIVE 死状态分类——`erp/doc-status` 含 ACTIVE（已生效）但 4 实体零 setStatus(ACTIVE) 生产 writer（业务「已生效」由 approveStatus=APPROVED + posted 表达）；分类 = `intentional legacy dead state`（同 Order/Quotation M2 裁定），Bean 不编码 ACTIVE 入边，dict 值保留不改绑。供 Phase 3 引用。
  - Skill: `state-machine-business-review-prompt.md`
- [ ] 在 `app-service.beans.xml` 以 FQN id 注册 4 Bean（追加于既有 Document SM 段，§11.1 步骤 2）。
  - Skill: `nop-backend-dev`
- [ ] Proof（层 1 矩阵完备性，表驱动，§11.1 步骤 4）：4 Bean × {cancel 合法 DRAFT→CANCELLED + 非法 CANCELLED/ACTIVE} + terminal {CANCELLED} 无出边 + transitions(1) + initial/terminal。**不经 BizModel 入口**（层 1 只测 Bean）。
  - Skill: `nop-testing`

Exit Criteria:

- [ ] 4 Bean 无状态、矩阵完整（单边 cancel）；ACTIVE 死状态 Decision 记录在案
- [ ] 4 Bean 已在 `app-service.beans.xml` 注册（FQN id）；`@Inject` 字段非 private（合规 R5）
- [ ] 层 1 矩阵测试通过；本地化编译 `mvn compile -pl module-purchase/erp-pur-service -am` 通过（解除 Phase 2 接线依赖）

### Phase 2 - cancel 路径接线（两种编排形态，行为保持，过账/stock move 副作用保留）+ 层 3 回归

Status: planned
Targets: `ErpPurReceiveCancelProcessor`、`ErpPurReturnCancelProcessor`（CancelProcessor 接线）；`ErpPurInvoiceProcessor`、`ErpPurPaymentProcessor`（主 Processor 接线）
Skill: `nop-backend-dev`（接线 + 错误码映射）+ `nop-testing`（回归断言 + Payment cancel 补全）

- Item Types: `Fix | Add | Proof`
- Prereqs: Phase 1 四 Bean 落地

- [ ] Receive/Return（CancelProcessor 形态，同 Order 先例）：`ErpPurReceiveCancelProcessor` + `ErpPurReturnCancelProcessor` 注入各自 `ErpPur*DocumentStateMachine`，`validateTransitionForCancel` 改 `stateMachine.assertCanCancel(from)` + `cancelledDocStatus()` 回 `stateMachine.cancelTargetStatus()`。common→既有域码映射（Receive→`ERR_ILLEGAL_DOC_STATUS_TRANSITION`；Return→`ERR_RETURN_ILLEGAL_DOC_STATUS_TRANSITION`），common 作 cause，`{xxxCode}/{currentDocStatus}/{expectedDocStatus}` 参数对外不变（**不新增错误码、不重命名 Receive 泛型码**）。**完整保留**：Receive 若 approved 逆转 stock move；Return 若 approved 逆转 stock move + 过账。
  - Skill: `nop-backend-dev`
- [ ] Invoice/Payment（主 Processor 形态）：`ErpPurInvoiceProcessor.validateTransitionForCancel` + `ErpPurPaymentProcessor.validateTransitionForCancel` 注入各自 Bean，固定来源态守卫改 Bean 委托；`doCancel` 目标态回写沿用 `stateMachine.cancelTargetStatus()`（经 CancelProcessor 编排链调用）。common→域码映射（Invoice→`ERR_INVOICE_ILLEGAL_DOC_STATUS_TRANSITION`；Payment→`ERR_PAYMENT_ILLEGAL_DOC_STATUS_TRANSITION`）。**完整保留**：Invoice 若 posted `postingDispatcher.reverse`；Payment 若 posted `postingDispatcher.reverse` + commitment-restore hook。
  - Skill: `nop-backend-dev`
- [ ] Add（填补缺口）：为 Payment 补 cancel 路径集成回归（`TestErpPurPaymentApproval` 增 `testCancelFromDraft` 断言 `DOC_STATUS_CANCELLED`，对齐 Receive/Invoice/Return 既有 cancel 测试形态）。
  - Skill: `nop-testing`
- [ ] Proof（层 3 回归）：`mvn test -pl module-purchase/erp-pur-service -am` 全绿——重点 Receive/Invoice/Return `testCancel*`（cancel→CANCELLED + 过账/stock move 逆转）、Payment 新增 `testCancelFromDraft`、`TestPurReversalListenerReceiveRollback`（reversal listener 回写 posted=false + APPROVED→REJECTED，**不触 docStatus**）、`TestErpPurInvoiceCommitmentRestore`/`TestErpPurReturnCommitmentRestore`（commitment-restore 不变）、Payment workflow `TestErpPurPaymentWorkflowApproval`。
  - Skill: `nop-testing`

Exit Criteria:

- [ ] 4 实体接线后既有测试全绿 + Payment cancel 新测试通过（行为、过账/stock move 逆转时序、reversal-listener 回写、commitment-restore、错误码、乐观锁无回归）
- [ ] grep 证实 cancel 路径方法体内不再有内联固定状态矩阵判断（动态副作用如 stock move 逆转/过账 reverse/commitment 除外）

### Phase 3 - 层 2 四方对照 + 漂移 Decision + owner doc 补注

Status: planned
Targets: 四方对照审计记录（写入本计划 Closure 段）；`docs/design/purchase/state-machine.md`（§三轴 docStatus ACTIVE 死状态补注）；本计划 Closure
Skill: `state-machine-business-review-prompt.md`（四方对照 + 10 维度）

- Item Types: `Proof | Decision | Add`
- Prereqs: Phase 2 接线完成

- [ ] Proof（层 2 四方对照，§11.1 步骤 5，10 维度 × 4 轴）：dict（doc-status 3 值）↔ owner doc（§适用对象 + §三轴）↔ Bean ↔ writer。重点：(a) ACTIVE 死状态裁定（dict 有值零 writer）；(b) DRAFT/CANCELLED 可达（DRAFT=初始；CANCELLED=cancel）；(c) reversal-listener 回写 posted+approveStatus 不触 docStatus；(d) 两种 cancel 编排形态接线正确；(e) Receive 泛型错误码命名漂移（successor）。
  - Skill: `state-machine-business-review-prompt.md`
- [ ] Add owner doc：在 `docs/design/purchase/state-machine.md §三轴状态分离` 补 docStatus ACTIVE 死状态注记（docStatus 实际仅 DRAFT→CANCELLED；ACTIVE dict 有值但 4 单据零 writer，「已生效」由 approveStatus=APPROVED + posted 表达）。
  - Skill: `state-machine-business-review-prompt.md`
- [ ] Decision（漂移裁定，路线图规则 5）：(a) ACTIVE 死状态 = `intentional legacy dead state`，Bean 不编码 + owner doc 补注；(b) Receive 泛型错误码 `ERR_ILLEGAL_DOC_STATUS_TRANSITION`（无 RECEIVE_ 前缀）vs Invoice/Payment/Return entity-scoped 命名 = `naming drift successor`（本计划保持既有码不改，语义对齐归 successor，同 M3.15 JobCard 先例）；(c) 死 doCancel 辅助（Receive/Return 主 Processor）保持原状不双写。
  - Skill: `state-machine-business-review-prompt.md`

Exit Criteria:

- [ ] 四方对照无未裁决漂移（ACTIVE 死状态 + reversal-listener + 两种编排形态 + Receive 错误码命名均裁定并落入 owner doc/计划）
- [ ] owner doc §三轴 docStatus ACTIVE 补注与 dict/Bean/代码一致

## Draft Review Record

- Independent draft review iteration 1: `acceptable as-is (draft pending M4 gate)` (`ses_006e7bf2bffeU3HgVyJ5R5XS31`，新会话零信任实仓复核) — BLOCKER=none、MAJOR=none、MINOR=3（已采纳修正）：(1) Receive docStatus 错误码行号 `ErpPurErrors.java:49`→`:53` 漂移（`:49` 实为 approveStatus 兄弟码；码名/值/参数/泛型命名特征均 CONFIRMED 正确）→ 已修正 `:53`；(2) Receive/Return 主 Processor `validateTransitionForCancel:158-163/:175-179` 为 facade 残留（live 守卫在 CancelProcessor 骨架）→ 已在 baseline「死 doCancel 辅助 + facade 守卫残留」补注 + Phase 3 layer-2 显式裁定；(3) beans.xml Document SM 行段 `:176-193`→`:176-183`（Approval SM 在 `:186-193`）→ 已修正。CONFIRMED（独立实证）：dict/doc-status 3 值 + ACTIVE 零 writer、4 实体 docStatus 唯一 writer=cancel→CANCELLED、两种 cancel 编排形态（Receive/Return 内联 vs Invoice/Payment 委托）、过账路径（Receive→IErpInvStockMoveBiz 无 PurReceivePostingDispatcher；Invoice→AP_INVOICE/Payment→PAYMENT/Return→PURCHASE_RETURN dispatcher）、PurReversalListener 4 业务类型回写 posted+approveStatus 不触 docStatus、Order 迁移模板复刻范式 EXACT、Invoice/Payment/Return 错误码 :134/:162/:198 EXACT、Payment 无 cancel 测试（缺口）+ 无矩阵测试、§11.2 M4 (i)-(v) 声明完整 + rule 14 bundling 合理 + Plan Status=draft 不自激活。
- **M4 plan-first 人工/owner-doc 门控状态：pending**（§11.2 M4 (i) + 会计/财务保护区）。草案审查虽已收敛（acceptable-as-draft），但在人工/owner-doc 确认「以行为保持方式迁移此 4 轴、过账/stock move/reversal-listener 路径完整保留」前保持 `Plan Status: draft`（对齐 M3.10/M4.29-30/M4.1/M4.2 plan-first 先例）。确认后在此追加记录，方可转 `active`。
- Independent draft review iteration 2: `acceptable as-is (draft pending M4 gate)` (mission-driver review pass) — 复核确认：格式合规（全部必需段落/字段名/Phase 结构）、完备性（三阶段 Exit Criteria 清晰可测、Closure Gates 覆盖全项含 M4 门控）、范围清晰（docStatus 单轴 + rule 14 bundling 合理、无 scope creep、approveStatus M4.14/16/18/20 与过账/reversal-listener/模型/字典/API 均显式 Non-Goal）、结束证据已定义（验证命令 + grep + 测试引用 + Closure Audit Evidence 段）。BLOCKER=none（格式/完备/范围/结束证据维度）、MAJOR=none。**唯一阻塞 = M4 plan-first 人工/owner-doc 门控（§11.2 M4 (i)）**，属 review 时无法自主解除的「missing upstream decision」类阻塞（project-context.md「AI 阻塞条件」会计/财务保护域硬停止），`> Review Hold:` 已正确标注。按 fix-forward 逃生口：保持 `Plan Status: draft` 不激活，等待人工/owner-doc 确认后转 `active`。
- Independent draft review iteration 3: `acceptable as-is (draft pending M4 gate)` (mission-driver review pass) — 四维复核全部 PASS：(1) 格式合规——必需段落/字段名/Phase 结构齐全，`Review Hold` 行位置正确（front matter 近 `Plan Status`）；(2) 完备性——三阶段 Exit Criteria 清晰可测、Closure Gates 覆盖全项含 M4 门控；(3) 范围——rule 14 bundling 合理（同 owner doc + 同 dict + 同 docStatus 行为契约 + 同结果表面），approveStatus/过账/reversal-listener/模型/字典/API/错误码重命名 均显式 Non-Goal，无 scope creep；(4) 结束证据——验证命令 + grep + 测试引用 + Closure Audit Evidence 段齐备。BLOCKER=none（四维）、MAJOR=none、MINOR=0。唯一阻塞 = M4 plan-first 人工/owner-doc 门控，属 review 时无法自主解除的「missing upstream decision」（会计/财务保护域硬停止，project-context.md「AI 阻塞条件」），`> Review Hold:` 已正确标注。按 fix-forward 逃生口：保持 `Plan Status: draft`，等待人工/owner-doc 确认后转 `active`。
- Independent draft review iteration 4: `acceptable as-is (draft pending M4 gate)` (mission-driver review pass) — 四维复核全部 PASS：(1) 格式合规——front matter 段（Plan Status/Last Reviewed/Source/Related/Mission/Work Item/Audit）+ Review Hold（第 4 行，近 Plan Status）+ 治理声明 + 全部必需正文段落（Current Baseline/Goals/Non-Goals/Task Route/Infrastructure/Execution Plan/Draft Review Record/Closure Gates/Deferred But Adjudicated/Closure）齐全；三阶段 Phase 结构有效（Status/Targets/Skill/Item Types/Prereqs/checklist/Exit Criteria）。(2) 完备性——Exit Criteria 清晰可测（Phase 1 Bean 无状态+注册+层 1 测试+compile；Phase 2 测试全绿+grep 证实无内联矩阵；Phase 3 无未裁决漂移+owner doc 一致），Execution Plan 覆盖全部 checklist。(3) 范围——docStatus 单轴边界清晰，rule 14 bundling 合理，approveStatus/过账/reversal-listener/stock move/commitment/模型/字典/API/错误码重命名均显式 Non-Goal，无 scope creep。(4) 结束证据——验证命令 + grep + 测试引用 + Closure Audit Evidence 段齐备。BLOCKER=none（四维）、MAJOR=none、MINOR=0。唯一阻塞 = M4 plan-first 人工/owner-doc 门控（§11.2 M4 (i)，触及受保护采购业财过账行为），属 review 时无法自主解除的「missing upstream decision」（project-context.md 会计/财务保护域硬停止），`> Review Hold:` 已正确标注。按 fix-forward 逃生口：保持 `Plan Status: draft`，不激活，等待人工/owner-doc 确认后转 `active`。
- Independent draft review iteration 5: `acceptable as-is (draft pending M4 gate)` (mission-driver review pass) — 四维复核全部 PASS：(1) 格式合规——front matter + Review Hold（line 4，近 Plan Status）+ 治理声明/rule 14 bundling 声明 + 全部必需正文段落齐全；三阶段 Phase 结构有效（Status/Targets/Skill/Item Types/Prereqs/checklist/Exit Criteria）。(2) 完备性——Exit Criteria 清晰可测，Execution Plan 覆盖全部 checklist 项。(3) 范围——docStatus 单轴 + rule 14 bundling 合理，approveStatus/过账/reversal-listener/stock move/commitment/模型/字典/API/错误码重命名均显式 Non-Goal，无 scope creep。(4) 结束证据——验证命令 + grep + 测试引用 + Closure Audit Evidence 段齐备。BLOCKER=none（四维）、MAJOR=none、MINOR=0。唯一阻塞 = M4 plan-first 人工/owner-doc 门控（§11.2 M4 (i)，触及受保护采购业财过账行为 = cancel approved+posted 逆转存货移动 + 红字凭证），属 review 时无法自主解除的「missing upstream decision」（project-context.md「AI 阻塞条件」会计/财务保护域硬停止）。无格式/完备/范围/结束证据 issue 需就地修复。按 fix-forward 逃生口：保持 `Plan Status: draft`，`> Review Hold:` 已正确标注，等待人工/owner-doc 确认后转 `active`。
- Independent draft review iteration 6: `acceptable as-is (draft pending M4 gate)` (mission-driver review pass) — 四维复核全部 PASS：(1) 格式合规——front matter（Plan Status/Review Hold line 4 近 Plan Status/Last Reviewed/Source/Related/Mission/Work Item/Audit）+ 治理声明 + rule 14 bundling 声明 + 全部必需正文段落（Current Baseline/Goals/Non-Goals/Task Route/Infrastructure/Execution Plan/Draft Review Record/Closure Gates/Deferred But Adjudicated/Closure）齐全；三阶段 Phase 结构有效（Status/Targets/Skill/Item Types/Prereqs/checklist/Exit Criteria）。(2) 完备性——Exit Criteria 三阶段清晰可测（Phase 1 Bean 无状态+注册+层 1 矩阵+compile；Phase 2 接线后既有测试全绿+Payment cancel 补全+grep 证实无内联矩阵；Phase 3 四方对照无未裁决漂移+owner doc 一致），Execution Plan 覆盖全部 checklist 项。(3) 范围——docStatus 单轴边界清晰，rule 14 bundling 合理（同 owner doc + 同 dict + 同 cancel 行为契约 + 同结果表面），approveStatus/过账/reversal-listener/stock move/commitment/模型/字典/API/错误码重命名均显式 Non-Goal，无 scope creep。(4) 结束证据——验证命令（mvn test -pl module-purchase/erp-pur-service -am + mvn clean install -DskipTests + compliance checker）+ grep + 测试引用 + Closure Audit Evidence 段齐备。BLOCKER=none（四维）、MAJOR=none、MINOR=0。唯一阻塞 = M4 plan-first 人工/owner-doc 门控（§11.2 M4 (i)，触及受保护采购业财过账行为 = cancel approved+posted 逆转存货移动 + 红字凭证），属 review 时无法自主解除的「missing upstream decision」（project-context.md「AI 阻塞条件」会计/财务保护域硬停止）。无格式/完备/范围/结束证据 issue 需就地修复。按 fix-forward 逃生口：保持 `Plan Status: draft`，`> Review Hold:` 已正确标注，等待人工/owner-doc 确认后转 `active`。

## Closure Gates

> 本计划含生产代码变更（4 Bean + cancel 路径接线 + 测试 + owner doc 补注），Closure Gates 运行完整仓库验证。无 ORM/API/字典变更（doc-status 3 值保留 + ACTIVE 死状态不改绑），Compliance 基线预期无漂移（R5=0/R11=0）。

- [ ] 范围内行为完成（4 Bean + cancel 接线 + 三层证据；过账/stock move/reversal-listener/commitment 时序完整保留，§11.2 M4 (ii)/(iv)/(v)）
- [ ] 相关文档对齐（owner doc §三轴 docStatus ACTIVE 补注 + 漂移 Decision 登记；路线图 M4.13 + M4.15 + M4.17 + M4.19 done）
- [ ] 已运行验证：`mvn test -pl module-purchase/erp-pur-service -am` + Closure 时 `mvn clean install -DskipTests` + `bash docs/audits/nop-compliance-checker.sh`
- [ ] **M4 plan-first 人工/owner-doc 门控已确认并记录于 Draft Review Record**（§11.2 M4 (i)）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：Plan Status、各 Phase Status、Exit Criteria、Closure Gates、日志一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### purchase approveStatus 轴（M4.14/16/18/20）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: approveStatus 审批轴（Receive/Invoice/Payment/Return）属独立结果表面（不同字段/矩阵/Bean），路线图列为 M4.14/16/18/20，依赖本 docStatus 计划。本计划仅迁移 docStatus 轴。
- Successor Required: yes（触发条件 = purchase approveStatus M4 批次计划启动时）

### Receive docStatus 错误码命名对齐（泛型→entity-scoped）

- Classification: `watch-only residual (naming drift successor)`
- Why Not Blocking Closure: Receive docStatus 非法码为泛型 `ERR_ILLEGAL_DOC_STATUS_TRANSITION`（无 RECEIVE_ 前缀，msg 硬编码「入库单」），Invoice/Payment/Return 均 entity-scoped。路线图 Non-Goal「不借迁移改变既有错误码」。本计划保持既有码。
- Successor Required: yes（触发条件 = PM/owner 要求 Receive 错误码语义对齐时，开独立 Fix plan 裁定重命名 + 测试/前端文案影响，同 M3.15 JobCard 先例）

### 死 doCancel 辅助（Receive/Return 主 Processor）

- Classification: `watch-only residual (intentional legacy)`
- Why Not Blocking Closure: `ErpPurReceiveProcessor.doCancel:270` 与 `ErpPurReturnProcessor.doCancel:234` 写 CANCELLED 但不在 cancel 路径（CancelProcessor 内联自己的写）。本计划不改动它们，避免引入双写。
- Successor Required: no（除非 cancel 编排统一收敛到 AbstractCancelProcessor 骨架时清理）

### 通用 CRUD 写入禁止 / Delta 覆盖证明

- Classification: `watch-only residual` / `optimization candidate`
- Why Not Blocking Closure: CRUD 写入边界 = M0.1 successor；M4 保护域单项不自带 Delta 证明，归 M5.3。
- Successor Required: no（归 M0.1/M5.3）

## Closure

Status Note: <待执行与独立结束审计后填充>

Closure Audit Evidence:

- Auditor / Agent: <独立子代理>
- Evidence: <task id / walkthrough record>

Follow-up:

- <非阻塞跟进见 §Deferred But Adjudicated；已确认缺陷不得出现在此处>
