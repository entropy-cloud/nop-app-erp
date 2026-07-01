# 2026-07-02-0300-1 purchase-invoice-payment-three-way-match

> Plan Status: completed
> Last Reviewed: 2026-07-02
> Source: `docs/backlog/core-business-roadmap.md` 工作项 1.4（三单匹配）+ 1.6（采购到付款串联，AP 段）；前置计划 1132-1 Deferred「采购发票/付款 Provider（AP_INVOICE/PAYMENT）」与「订单行级超收容差校验（触发条件：实施三单匹配 1.4 时）」
> Related: `2026-07-01-1132-1-purchase-receipt-approval-inventory-trigger.md`（入库段，已完成）、`2026-07-02-0300-2-sales-invoice-receipt-bizmodel.md`（AR 对称面）、`2026-07-02-0300-3-ar-ap-settlement-subledger.md`（财务核销/辅助账后继）
> Mission: erp
> Work Item: 采购发票 + 付款单 BizModel + AP 过账 Provider + 三单匹配（1.4 + 1.6 AP 段）
> Audit: required

## Current Baseline

实时仓库已核实的事实（逐一打开 ORM/BizModel/字典/过账基础设施确认，非采信记忆）：

- **采购发票实体** `ErpPurInvoice`（`module-purchase/model/app-erp-purchase.orm.xml:496`）：三轴分离——`docStatus`(erp-pur/doc-status DRAFT/ACTIVE/CANCELLED=10/20/30)、`approveStatus`(erp-pur/approve-status UNSUBMITTED/SUBMITTED/APPROVED/REJECTED=10/20/30/40)、`paidStatus`(erp-pur/paid-status UNPAID/PARTIAL/PAID=10/20/30)；`posted`+`postedAt`/`postedBy`；`supplierId` mandatory、`currencyId` mandatory、`businessDate` mandatory；金额族 `amountSource`/`amountFunctional`/`totalAmount`(不含税)/`totalTaxAmount`/`totalAmountWithTax`/`paidAmount` 全 DECIMAL(20,4)。`approvedBy`/`approvedAt` 在位。
- **采购发票行** `ErpPurInvoiceLine`(:540)：`invoiceId` mandatory、`receiveLineId`（可选回链入库行）、`materialId` mandatory、`uoMId` mandatory、`quantity` mandatory(DECIMAL)、`unitPrice`/`taxRate`/`taxAmount`/`amount` 可空 DECIMAL(20,4)。`receiveLine` to-one 关系已建。
- **付款单头** `ErpPurPayment`(:572)：三轴——`docStatus`、`approveStatus`、`writtenOffStatus`(复用 erp-pur/paid-status 字典=UNPAID/PARTIAL/PAID)；`posted`；`supplierId`/`currencyId`/`businessDate` mandatory；`amountSource`/`amountFunctional`/`totalAmount` 全 mandatory；`bankAccountId`/`partnerBankAccountId`/`settlementMethodId`/`paymentMethod` 可空。`approvedBy`/`approvedAt` 在位。
- **付款核销行** `ErpPurPaymentLine`(:618)：`paymentId` mandatory、`invoiceId` mandatory、`amount`(核销金额) mandatory。to-one 回链 payment 与 invoice。**这就是域级核销载体**。
- **BizModel 空壳**：`ErpPurInvoiceBizModel`/`ErpPurPaymentBizModel`/`ErpPurPaymentLineBizModel` 均为 codegen 空 `CrudBizModel`（仅构造器，已核实 `ErpPurInvoiceBizModel.java`）；`IErpPurInvoiceBiz`/`IErpPurPaymentBiz` 空 `ICrudBiz`。
- **过账基础设施**（finance，已完成于 0811-1/2030-1）：入口 facade `IErpFinVoucherBiz.post(PostingEvent, IServiceContext)`（`erp-fin-dao/.../biz/IErpFinVoucherBiz.java`，幂等）；跨域失败隔离的 `@Transactional(REQUIRES_NEW)` **钉在 Facade `post()` 层**（`IErpFinVoucherBiz.java:21-22`，硬规则 1：事务边界钉 Facade，不下放编排层）。参照 `InvPostingExecutor.java:14-18`——executor **无 `@Transactional`**，仅以 try/catch 包裹 Facade 调用。`ErpFinBusinessType.AP_INVOICE(30)`/`PAYMENT(50)` 枚举常量已存在；`IErpFinAcctDocProvider` SPI（getSupportedBusinessTypes + createFacts → List<VoucherFact>）+ `ErpFinAcctDocRegistry` 自动聚合。参照实例 `InvAcctDocProvider`（PURCHASE_INPUT/SALES_OUTPUT，借存货/贷暂估应付 or 借成本/贷存货）+ `InvPostingDispatcher`/`InvPostingExecutor`（DONE 后派发，REQUIRES_NEW，失败吞异常保持终态+posted=false）。
- **purchase 域当前无过账派发器**：grep 确认 module-purchase 下无 PostingDispatcher（仅 inventory 有）。本计划需新增 `PurPostingDispatcher`/`PurPostingExecutor`（对齐 inventory 模式）。
- **入库单回链订单行**：`three-way-match.md` 指明 `ErpPurReceiveLine` 可选回链采购订单行（source_order_line_id）；`ErpPurReceive`/`ReceiveLine` 已由 1132-1 落地三轴状态机 + 入库审核触发库存移动。`ErpPurOrderLine.unitPrice`（DECIMAL 金额族）用于三单匹配价格比对。
- **供应商启用校验**：`ErpMdPartner.status` + `requireSupplierActive` 机制（daoFor 机制 B 读 status，停用抛 ERR_PARTNER_INACTIVE）已由 1426-1 在订单/请购审核建立，可复用错误码作用域模式。
- **剩余差距**：(1) 发票/付款 BizModel 无审批状态机与过账接线；(2) 无 AP_INVOICE/PAYMENT 过账 Provider；(3) 无三单匹配校验；(4) 无付款→发票核销与 paidStatus/writtenOffStatus 回写。

## Goals

- 采购发票 `IErpPurInvoiceBiz` 三轴审批状态机（submit/withdrawSubmit/approve/reject/reverseApprove/cancel），对齐 `state-machine.md` 与 `flow-overview.md §3`。
- 发票审核执行**三单匹配**（订单↔入库↔发票 的数量容差与价格容差，`three-way-match.md`），失败按严格模式拒绝/非严格模式放行告警。
- 发票审核触发 **AP_INVOICE** 过账（借费用/采购 + 借进项税 / 贷应付），`posted=true`；反审核/作废走红字冲销（`IErpFinVoucherBiz.reverse`）。
- 付款 `IErpPurPaymentBiz` 三轴审批状态机；付款审核触发 **PAYMENT** 过账（借应付 / 贷银行存款），`posted=true`。
- 付款→发票**域级核销**（`ErpPurPaymentLine` 多对多），回写 `ErpPurInvoice.paidStatus`/`paidAmount` 与 `ErpPurPayment.writtenOffStatus`，核销约束（同供应商、金额不超余额、双方已审核）。
- 行为测试覆盖状态机/三单匹配/过账/核销/端到端（PO→Receive→Invoice→Pay 部分付款）。

## Non-Goals

- **财务辅助账 `ErpFinArApItem` 与正式核销单 `ErpFinReconciliation`**：属 0300-3 财务核销/辅助账面（finance 域结果表面），本计划只做 purchase 域文档流 + 域级核销（`ErpPurPaymentLine`）。
- **费用分摊（Landed Cost）/采购价格差异科目核算**：`three-way-match.md §费用分摊`，属财务域成本核算，本计划只提供匹配数据。
- **采购退货发票（红冲发票自动回链）**：属 1.9（采购退货与退款）。
- **nop-wf 多级审批人路由**：审核 = 直接状态迁移 + `@BizMutation`（对齐 1132-1/1426-1 基线）。
- **自动核销规则（按金额/比例/账龄/到期日）与定时自动核销**：`ar-ap-reconciliation.md §自动核销`，属财务核销面（0300-3）。
- **多币种汇兑损益核销**：属 0300-3（需跨币种核销日汇率）。
- **`ErpPurSupplierPriceList` 自动带价 / 发票行从入库行自动复制**：手工指定回链，不做自动带出。
- **付款单的银企直连/资金支付指令**：`treasury.md` 资金面，本计划付款仅做记账凭证。

## Task Route

- Type: `implementation-only change`（业务逻辑实现；模型/契约不变，全部走新增 Java + beans.xml 接线）。
- Owner Docs: `docs/design/purchase/state-machine.md`（发票/付款三轴）、`docs/design/purchase/three-way-match.md`（匹配规则）、`docs/design/purchase/README.md`（AP_INVOICE/PAYMENT 业务类型）、`docs/design/finance/posting.md`（过账三层模型 + Provider 机制）、`docs/design/flow-overview.md §2.1/§3`（采购到付款 + 状态映射）。
- Skill Selection Basis: 本计划全为 BizModel/IBiz/跨实体调用/过账 Provider 实现 → 加载 `nop-backend-dev`（IBiz 契约 + 跨实体访问 + 错误码 + 事务边界）；三单匹配容差配置读 `AppConfig.var`，错误处理走 `NopException`+`ErrorCode`，均属 skill 路由范围。

## Infrastructure And Config Prereqs

- 三单匹配配置项（`three-way-match.md §不匹配处理策略`）：`erp-pur.match-qty-tolerance`(默认 5)、`erp-pur.match-price-tolerance`(默认 5)、`erp-pur.match-strict-mode`(默认 false)。经 `AppConfig.var(..., defaultValue)` 读取，缺失走默认，无需 .env。
- 模块依赖：`erp-pur-service` 需新增 compile 依赖 `app-erp-finance-service`（对齐 inventory 0811-2 接线 posting）；finance-dao/master-data-dao compile 已存在。无外部服务/端口/密钥。
- 无数据迁移；无回滚脚本需求（纯新增）。

## Execution Plan

### Phase 1 — 采购发票审批状态机 + 三单匹配 + AP_INVOICE 过账

Status: completed
Targets: `module-purchase/erp-pur-dao/.../biz/IErpPurInvoiceBiz.java`、`module-purchase/erp-pur-service/.../entity/ErpPurInvoiceBizModel.java`、新增 `.../posting/PurAcctDocProvider.java`（AP_INVOICE）、`.../posting/PurInvoicePostingDispatcher.java`+`PurPostingExecutor.java`（共享执行器，Phase 2 PAYMENT 复用）、`.../entity/ThreeWayMatcher.java`、`.../ErpPurErrors.java`(扩)、`.../ErpPurConstants.java`(扩)、`erp-pur-service/.../_vfs/erp/pur/beans/app-service.beans.xml`、`erp-pur-service/pom.xml`
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: 1132-1 已完成（入库段 + ErpPurReceive 三轴）；过账基础设施（0811-1/2030-1）已完成。

- [x] `Add`：`IErpPurInvoiceBiz` 声明三轴契约 `submit/withdrawSubmit/approve/reject/reverseApprove/cancel`（`@BizMutation` + `@Name`，对齐 `IErpPurReceiveBiz` 签名形状）。
  - Skill: `nop-backend-dev`
- [x] `Add`：`ErpPurInvoiceBizModel` 实现三轴状态机（UNSUBMITTED↔SUBMITTED→APPROVED/REJECTED；reverseApprove APPROVED→REJECTED；cancel 非终态→CANCELLED，APPROVED 须先冲销）。`@BizMutation` 自动包装事务（对齐 1132-1 与 `ErpPurReceiveBizModel`，不叠加 `@Transactional`/`@SingleSession`），每迁移校验前置态，违例抛 `NopException`。
  - Skill: `nop-backend-dev`
- [x] `Add`：发票审核前置校验——供应商启用（复用 1426-1 `requireSupplierActive` 机制；扩 `ErpPurErrors` 新增发票作用域码 `ERR_INVOICE_*` 绑定 `ARG_INVOICE_CODE`，不复用入库单 `ARG_RECEIVE_CODE`/「入库单…」文案）、行非空。
  - Skill: `nop-backend-dev`
- [x] `Add`：`ThreeWayMatcher` 回链路径核对——`ErpPurInvoiceLine.receiveLineId`（已确认 to-one `receiveLine` @ orm.xml:566）回链 `ErpPurReceiveLine`，后者经 **`orderLineId`**（to-one `orderLine` @ orm.xml:490，**非** design 文档概念名 `source_order_line_id`）回链 `ErpPurOrderLine.unitPrice` 用于价格比对。匹配按 `three-way-match.md`：发票数量 vs 入库数量（发票不得超入库，除非配置）、发票单价 vs 订单单价（价格容差）。容差经 `AppConfig.var("erp-pur.match-qty-tolerance", "5")` / `match-price-tolerance` / `match-strict-mode`。严格模式超容差拒绝审核（`ERR_INVOICE_QTY_MISMATCH`/`ERR_INVOICE_PRICE_MISMATCH`）；非严格模式记录警告但放行。`receiveLineId` 缺失的行跳过匹配（支持无订单/直接凭发票场景）。
  - Skill: `nop-backend-dev`
- [x] `Decision`：三单匹配「发票数量不得超过入库数量」的强制项 vs 配置项——**选择**：默认强制不得超入库（数量差异不设容差放行，仅价格差异走容差配置），**替代**：全部走容差配置，**残留风险**：运费/杂费明细可能需超开票，留配置开关 `match-strict-mode` 在非严格模式下放行告警。
  - Skill: none
- [x] `Add`：`PurAcctDocProvider implements IErpFinAcctDocProvider`（`EnumSet.of(AP_INVOICE, PAYMENT)`，非默认——Phase 2 复用同类集中），产 AP_INVOICE 3 条 `VoucherFact`：借 1403 在途物资 + 借 2221 进项税 / 贷 2202 应付（金额取发票 `totalAmount`/`totalTaxAmount`/`totalAmountWithTax`，对齐 `posting.md` AP_INVOICE 映射与 `InvAcctDocProvider` 形状）。注册于 `app-service.beans.xml`（由 `ErpFinAcctDocRegistry` 自动聚合）。
  - Skill: `nop-backend-dev`
- [x] `Add`：`PurInvoicePostingDispatcher`+`PurPostingExecutor`（**对齐 `InvPostingDispatcher`/`InvPostingExecutor`：executor 无 `@Transactional`，跨域失败隔离由 Facade `IErpFinVoucherBiz.post()` 的 `REQUIRES_NEW` 承接**）——发票 APPROVED 后组装 `PostingEvent`(AP_INVOICE, billHeadCode=invoice.code, billData 含金额族+supplierId+orgId+acctSchemaId 由组织解析) 调 `IErpFinVoucherBiz.post`；成功置 `posted=true`+postedAt/postedBy，失败 try/catch 吞异常记日志保持 APPROVED+`posted=false`（对齐 InvPostingDispatcher 失败不阻塞终态合约）。源单据 posted 标志由 BizModel 在主事务内统一持久化（dispatcher 不跨 REQUIRES_NEW 操作托管实体，规避 `save-entity-not-transient`）。
  - Skill: `nop-backend-dev`
- [x] `Add`：`reverseApprove`/`cancel` 对已 `posted=true` 发票——先调 `IErpFinVoucherBiz.reverse(code, AP_INVOICE, ctx)` 生成红字凭证（对齐 posting.md §冲销），幂等防双冲销（未过账跳过；状态机 APPROVED→REJECTED 终态保证不重复冲销）。
  - Skill: `nop-backend-dev`
- [x] `Proof`：`TestErpPurInvoiceApproval`（三轴迁移正向/反向/非法迁移/停用供应商）、`TestErpPurThreeWayMatch`（数量匹配/超入库拒绝/价格超容差严格拒绝 vs 非严格放行/无回链跳过）、`TestErpPurInvoicePosting`（APPROVED→凭证生成 posted=true；红字 reverseApprove）。验证命令 `mvn test -pl module-purchase/erp-pur-service -am`。**13/13 通过**。
  - Skill: `nop-backend-dev`

Exit Criteria:

> Phase 1 交付发票三轴状态机 + 三单匹配 + AP_INVOICE 过账端到端（审核→凭证→posted=true）+ 红字冲销前置。完整仓库验证属 Closure Gates。

- [x] 发票三轴状态机行为可观察（测试断言状态迁移 + 非法迁移抛 NopException）
- [x] 三单匹配：数量超入库/价格超容差（严格模式）拒绝审核；非严格模式放行（测试断言两种模式）
- [x] 发票审核 posted=true 且凭证分录方向正确（AP_INVOICE 借 1403+2221/贷 2202）；反审核生成红字凭证

### Phase 2 — 付款单审批状态机 + PAYMENT 过账 + 域级核销

Status: completed
Targets: `IErpPurPaymentBiz.java`、`ErpPurPaymentBizModel.java`、`SettlementAllocation.java`(dto)、`PurAcctDocProvider`(PAYMENT 已并入同类集中)、`PurPaymentPostingDispatcher`+共享 `PurPostingExecutor`、`PaymentSettler`(核销)、`ErpPurErrors`/`ErpPurConstants`(扩)、beans.xml/pom.xml
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: Phase 1（发票已可审核生成应付）。

- [x] `Add`：`IErpPurPaymentBiz` 三轴契约（submit/withdrawSubmit/approve/reject/reverseApprove/cancel）+ 域级核销动作 `settle(paymentId, List<SettlementAllocation>, ctx)`（指定发票×核销金额）与 `reverseSettlement(paymentId, invoiceId, ctx)`。
  - Skill: `nop-backend-dev`
- [x] `Add`：`ErpPurPaymentBizModel` 实现付款三轴状态机（同 Phase 1 形状，含跨域 post 后重载规避托管实体冲突）；`approve` 触发 PAYMENT 过账。
  - Skill: `nop-backend-dev`
- [x] `Add`：PAYMENT 过账——`PurAcctDocProvider` 已扩 `PAYMENT`（Phase 2 Decision (a) 同类集中），产 2 条 VoucherFact：借 2202 应付科目 / 贷 1002 银行存款科目（金额取付款 totalAmount）。`PurPaymentPostingDispatcher` 复用 `PurPostingExecutor`（executor 无 `@Transactional`，REQUIRES_NEW 由 Facade 承接），APPROVED→posted=true，失败吞异常保持终态。
  - Skill: `nop-backend-dev`
- [x] `Add`：`PaymentSettler` 域级核销——独立 `settle` 动作（Phase 2 Decision (b) MVP 解耦审核与核销）按 `ErpPurPaymentLine`(paymentId→invoiceId+amount) 登记核销；约束：同供应商、双方 approveStatus=APPROVED、核销金额不超发票未付余额（`totalAmountWithTax − paidAmount`）与付款未核销余额；违例抛 `ERR_SETTLE_*`。核销后回写 `ErpPurInvoice.paidAmount`(按发票全部 PaymentLine 含负金额行之和) + `paidStatus`(UNPAID→PARTIAL→PAID) 与 `ErpPurPayment.writtenOffStatus`。
  - Skill: `nop-backend-dev`
- [x] `Decision`：(a) PAYMENT Provider 归属——**选择** `PurAcctDocProvider` 扩 PAYMENT（同类集中，减少 Bean 数，AP_INVOICE+PAYMENT 同一 Provider），**替代** 独立 `PurPaymentAcctDocProvider`，**残留风险** 单类承载多业务类型，后续业务类型增多时可拆分。(b) 核销触发时机——**选择** MVP 独立 `settle` 两步动作（解耦审核与核销，审核只过账），**替代** 付款审核时按携带 PaymentLine 自动核销，**残留风险** 自动核销需额外配置规则，留 0300-3。
  - Skill: none
- [x] `Add`：`reverseSettlement`——生成反向 PaymentLine（负金额，保留审计轨迹），恢复发票/付款余额与状态（对齐 ar-ap-reconciliation.md §核销冲销）。
  - Skill: `nop-backend-dev`
- [x] `Proof`：`TestErpPurPaymentApproval`（三轴 + PAYMENT 过账 posted=true，借应付/贷银行存款；反审核红字冲销）、`TestErpPurPaymentSettlement`（部分核销 PARTIAL、全额 PAID、跨供应商拒绝、超额拒绝、reverseSettlement 恢复余额+负金额行）。验证命令 `mvn test -pl module-purchase/erp-pur-service -am`。**8/8 通过**。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 付款三轴状态机行为可观察；PAYMENT 过账 posted=true（借 2202 应付/贷 1002 银行存款）
- [x] 域级核销：部分/全额核销正确回写 paidStatus/paidAmount/writtenOffStatus；约束违例拒绝；冲销恢复余额

### Phase 3 — 采购到付款端到端串联 + 反向场景 + 文档/日志

Status: completed
Targets: `TestErpPurProcureToPayEnd.java`、`docs/logs/2026/{07-02}.md`、`docs/backlog/core-business-roadmap.md`
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 1 + Phase 2。

- [x] `Add`：端到端测试 `TestErpPurProcureToPayEnd`——建 PO→approve→Receive(1132-1)→approve(触发入库移动+PURCHASE_INPUT 暂估凭证)→建 Invoice(回链 ReceiveLine)→approve(三单匹配通过+AP_INVOICE 凭证)→建 Payment→approve(PAYMENT 凭证)→settle(部分核销，invoice.paidStatus=PARTIAL)。断言全链状态、posted、paidStatus 一致。
  - Skill: `nop-backend-dev`
- [x] `Add`：反向场景测试——发票 reverseApprove（红字冲销 AP 凭证 + posted 反转）、付款 reverseSettlement + reverseApprove（红字冲销 PAYMENT 凭证）。
  - Skill: `nop-backend-dev`
- [x] `Proof`：根 `mvn test -fae` 无回归（全 reactor 模块）。验证命令 `mvn test -fae`（根目录）= **BUILD SUCCESS**（全 18 域 reactor，0 Failures/0 Errors）。
  - Skill: none
- [x] `Add`：`docs/logs/2026/07-02.md` 新增本计划条目（含验证状态）；`docs/backlog/core-business-roadmap.md` 工作项 1.4 标 `done`（✅）、1.6 AP 段标注 `partial`（🔶）。
  - Skill: none

Exit Criteria:

- [x] 端到端 PO→Receive→Invoice→Pay(部分) 全链行为可观察（状态+posted+paidStatus 一致），含反向冲销
- [x] 当日日志已记；roadmap 1.4 / 1.6 AP 段进展已标注

## Draft Review Record

- Independent draft review iteration 1: **needs revision**（ses_0e10661e8ffe5ttk8aOEvCW5ZB，独立 general 子代理，新会话）— 全部 Current Baseline 主张经实时仓库核实属实（ErpPurInvoice/InvoiceLine/Payment/PaymentLine 字段/三轴/mandatory/DECIMAL 精度、空壳 BizModel/IBiz、字典值、`IErpFinVoucherBiz.post`/`reverse`、`ErpFinBusinessType` AP_INVOICE/PAYMENT、`IErpFinAcctDocProvider`+`InvAcctDocProvider` 参照、module-purchase 无既有派发器、three-way-match.md 容差键、1132-1 Deferred「AP_INVOICE/PAYMENT」记录）。**一处 BLOCKER**：新过账 executor 误标 `@Transactional(REQUIRES_NEW)`——与权威源 `InvPostingExecutor.java:14-18` 冲突（executor 明确无 `@Transactional`，跨域失败隔离的 REQUIRES_NEW 钉 Facade `IErpFinVoucherBiz.post()`，硬规则 1）；本计划 Current Baseline 另有 false claim「REQUIRES_NEW 在 executor 层」（规则 1）。非阻塞：S1/S2/S3（S2 经核实成立：`ErpPurReceiveLine` 回链列为 `orderLineId` @ orm.xml:490，**非** design 概念名 `source_order_line_id`）。
- Independent draft review iteration 2: **accept / consensus**（同 ses，blocker 已修 + 非阻塞已吸收）— 修订：(1) Current Baseline REQUIRES_NEW 归属更正为 Facade `post()` 层并引 `InvPostingExecutor.java:14-18`；(2) Phase 1/2 executor 项去除 `@Transactional(REQUIRES_NEW)`，改述「executor 无 @Transactional，REQUIRES_NEW 由 Facade 承接」；(3) S2 ThreeWayMatcher 回链名 `source_order_line_id`→`orderLineId`；(4) S3 PAYMENT Provider 归属并入 Phase 2 Decision。reviewer 明确声明「修此单一 blocker 后两计划为可接受执行契约」，修订经 `InvPostingExecutor.java`/`InvPostingDispatcher.java` 独立复核确认。规则 4/14 合并（发票+付款+三单匹配：匹配在发票审核触发，不可分）、规则 8/9（技能/Decision 备选+残留风险）、反松弛、规则 10（Non-Goal vs Deferred 诚实）、命名/头/Closure Gates 全合规。**共识达成**：计划为可接受的执行契约，Plan Status 升级为 `active`。

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。结束时运行一次完整仓库验证。

- [x] 范围内行为完成：发票/付款三轴状态机 + 三单匹配 + AP_INVOICE/PAYMENT 过账 + 域级核销 + 端到端全链落地，行为测试通过
- [x] 相关文档对齐：`core-business-roadmap.md` 1.4 / 1.6 AP 段标注进展；当日日志已记
- [x] 已运行验证：`mvn test -pl module-purchase/erp-pur-service -am` 全绿（53 tests）；根 `mvn test -fae` = BUILD SUCCESS（无回归）
- [x] 无范围内项目降级为 deferred/follow-up（财务辅助账/Landed Cost/退货发票/nop-wf/自动核销/汇兑损益/银企直连 均为计划内 Non-Goal）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：Plan Status、各 Phase Status、Exit Criteria、Closure Gates、日志一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于 `Closure` 节

## Deferred But Adjudicated

### 财务辅助账 ErpFinArApItem + 正式核销单 ErpFinReconciliation

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 属 finance 域结果表面（finance/ar-ap-reconciliation.md），本计划只做 purchase 域文档流 + 域级核销（ErpPurPaymentLine）。正式辅助账/核销单/账龄/余额由 0300-3 承接。
- Successor Required: yes（触发条件：实施 0300-3 AR/AP 核销与辅助账时）

### 三单匹配的 Landed Cost 费用分摊与采购价格差异科目核算

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 属财务域成本核算（three-way-match.md §费用分摊），本计划只提供匹配数据。
- Successor Required: yes（触发条件：实施费用分摊/PPV 科目核算时）

### 自动核销规则（按金额/比例/账龄/到期日）与定时自动核销

- Classification: `optimization candidate`
- Why Not Blocking Closure: MVP 为手工/显式核销；自动核销规则属财务核销面（0300-3）。
- Successor Required: yes（触发条件：实施 0300-3 自动核销时）

## Closure

Status Note: 执行者已完成全部三阶段实现 + 端到端验证（full-green）。`mvn test -pl module-purchase/erp-pur-service -am` = 53 tests / 0 Failures / 0 Errors（发票三轴 5 + 三单匹配 6 + 发票过账 2 + 付款三轴/过账 3 + 付款核销 5 + 端到端 2 + 既有 30）；根 `mvn test -fae` = BUILD SUCCESS（全 18 域 reactor 无回归）。三 Phase `Status: completed`、所有 Phase 项与退出标准已勾选。**独立结束审计已由独立子代理（新会话，不复用执行者上下文）执行并 PASS**——审计 verdict 见下方 Closure Audit Evidence，Closure Gates 最后两项（独立结束审计 / 结束证据）已勾选。本计划无 `> Source Audits:` 行（roadmap 源生计划），关闭 source audits 步骤跳过；`> Work Item:`（1.4 + 1.6 AP 段）已在 `docs/backlog/core-business-roadmap.md` 翻转（1.4 ✅ done / 1.6 🔶 partial AP 段 done）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话 closure-audit，非执行者会话；mission-driver 流程独立生成）
- Verdict: **passes closure audit**（5 项审计任务全 PASS）
- Exit Criteria vs live repo（PASS）：逐文件核实实现真实落地、非空壳——`IErpPurInvoiceBiz`/`IErpPurPaymentBiz` 三轴+核销契约（`module-purchase/erp-pur-dao/.../biz/`）；`ErpPurInvoiceBizModel`/`ErpPurPaymentBizModel` 状态机 + 过账触发 + 反向冲销（`erp-pur-service/.../entity/`）；`ThreeWayMatcher` 回链用 `receiveLine.getOrderLineId()`（**非** design 概念名 `source_order_line_id`，吸收草案审查 S2）；`PurAcctDocProvider`（AP_INVOICE+PAYMENT 同类集中，借 1403+2221/贷 2202 + 借 2202/贷 1002）；`PurPostingExecutor` **无 `@Transactional`**（吸收草案审查 BLOCKER，REQUIRES_NEW 钉 Facade，对齐 `InvPostingExecutor`）；`PurInvoicePostingDispatcher`/`PurPaymentPostingDispatcher` 失败吞异常保持终态 posted=false、不持久化源单据；`PaymentSettler` 域级核销约束 + paidStatus/paidAmount/writtenOffStatus 回写 + reverseSettlement 负金额行。
- Anti-Hollow / 接线（PASS）：6 个新 Bean（ThreeWayMatcher/PurAcctDocProvider/PurPostingExecutor/PurInvoicePostingDispatcher/PurPaymentPostingDispatcher/PaymentSettler）全部注册于 `_vfs/erp/pur/beans/app-service.beans.xml`；2 BizModel 注册于 `_service.beans.xml`；erp-pur-service pom 已加 `app-erp-finance-service` compile 依赖。所有新代码经 BizModel `@Inject` 运行时可达，无空方法体/`return null` 占位/吞异常的隔离空壳。
- Closure Gates 一致性（PASS）：Plan Status `completed` / 三 Phase Status `completed` / 三 Phase 退出标准全 `[x]` / Closure Gates 全 `[x]` / Deferred 项均为 Non-Goal（财务辅助账+核销单 0300-3 / Landed Cost / 自动核销 0300-3 / 退货发票 1.9），无范围内缺陷降级 deferred/follow-up。
- Docs sync（PASS）：`docs/logs/2026/07-02.md` 已含本计划条目（含 full-green 验证状态 + 跨 REQUIRES_NEW 托管实体工程发现）；`docs/backlog/core-business-roadmap.md` 工作项 1.4 = ✅ done、1.6 = 🔶 partial（AP 段 done）。
- 测试落地（PASS）：6 个目标测试文件存在（TestErpPurInvoiceApproval/TestErpPurThreeWayMatch/TestErpPurInvoicePosting/TestErpPurPaymentApproval/TestErpPurPaymentSettlement/TestErpPurProcureToPayEnd）；执行者报告 53 tests 全绿、根 reactor 无回归。

Follow-up:

- 财务辅助账 + 正式核销单（见上方 Deferred，0300-3 承接）
- Landed Cost 费用分摊 / PPV 科目核算（见上方 Deferred）
- 自动核销规则（见上方 Deferred，0300-3 承接）
- 采购退货发票自动回链原发票冲销应付（触发条件：实施 1.9 采购退货与退款时）
