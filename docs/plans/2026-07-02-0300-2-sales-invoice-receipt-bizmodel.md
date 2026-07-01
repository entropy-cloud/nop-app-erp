# 2026-07-02-0300-2 sales-invoice-receipt-bizmodel

> Plan Status: active
> Last Reviewed: 2026-07-02
> Source: `docs/backlog/core-business-roadmap.md` 工作项 1.7（销售到收款串联，AR 段）；前置计划 1132-2 Deferred「销售发票/收款 Provider（AR_INVOICE/RECEIPT）」
> Related: `2026-07-01-1132-2-sales-delivery-approval-inventory-trigger.md`（出库段，已完成）、`2026-07-02-0300-1-purchase-invoice-payment-three-way-match.md`（AP 对称面，同批）、`2026-07-02-0300-3-ar-ap-settlement-subledger.md`（财务核销/辅助账后继）、`2026-07-01-1426-2-sales-quotation-to-order-and-order-approval.md`（信用额度 AR 分量 Deferred 解除触发）
> Mission: erp
> Work Item: 销售发票 + 收款单 BizModel + AR 过账 Provider（1.7 AR 段）
> Audit: required

## Current Baseline

实时仓库已核实的事实（逐一打开 ORM/BizModel/字典确认）：

- **销售发票实体** `ErpSalInvoice`（`module-sales/model/app-erp-sales.orm.xml:447`）：三轴——`docStatus`(erp-sal/doc-status)、`approveStatus`(erp-sal/approve-status)、`receivedStatus`(erp-sal/received-status UNRECEIVED/PARTIAL/RECEIVED)；`posted`+`postedAt`/`postedBy`；`customerId` mandatory、`currencyId` mandatory、`businessDate` mandatory；金额族 `amountSource`/`amountFunctional`/`totalAmount`(不含税)/`totalTaxAmount`/`totalAmountWithTax`/`receivedAmount`。`approvedBy`/`approvedAt` 在位。
- **销售发票行** `ErpSalInvoiceLine`(:500)：`invoiceId` mandatory、`deliveryLineId`（可选回链出库行）、`materialId` mandatory、`uoMId` mandatory、`quantity` mandatory、`unitPrice`/`taxRate`/`taxAmount`/`amount` 可空 DECIMAL(20,4)。`deliveryLine` to-one 已建。
- **收款单头** `ErpSalReceipt`(:539)：三轴——`docStatus`、`approveStatus`、`writtenOffStatus`(复用 erp-sal/received-status 字典)；`posted`；`customerId`/`currencyId`/`businessDate` mandatory；`amountSource`/`amountFunctional`/`totalAmount` mandatory；`bankAccountId`/`partnerBankAccountId`/`settlementMethodId`/`receiptMethod` 可空。`approvedBy`/`approvedAt` 在位。
- **收款核销行** `ErpSalReceiptLine`(:596)：`receiptId` mandatory、`invoiceId` mandatory、`amount`(核销金额) mandatory。to-one 回链 receipt 与 invoice。**域级核销载体**。
- **BizModel 空壳**：`ErpSalInvoiceBizModel`/`ErpSalReceiptBizModel`/`ErpSalReceiptLineBizModel` 为 codegen 空 `CrudBizModel`；`IErpSalInvoiceBiz` 空 `ICrudBiz`（IErpSalReceiptBiz 已由 1132-2 确认存在空壳）。
- **过账基础设施**：`ErpFinBusinessType.AR_INVOICE(40)`/`RECEIPT(60)` 枚举常量已存在；`IErpFinVoucherBiz.post`/`reverse` 入口；`IErpFinAcctDocProvider` SPI（参照 `InvAcctDocProvider`）。
- **sales 域当前无过账派发器**：grep 确认 module-sales 下无 PostingDispatcher。本计划需新增 `SalPostingDispatcher`/`Executor`（对齐 inventory/purchase 模式）。
- **客户启用校验**：`ErpMdPartner.status` + `requireCustomerActive` 机制已由 1132-2/1426-2 在出库/订单审核建立（daoFor 机制 B 读 status）；可复用 `ErpSalErrors` 作用域码模式。
- **出库单回链**：`ErpSalInvoiceLine.deliveryLineId` 回链 `ErpSalDeliveryLine`；`ErpSalOrder` 经 1132-2 `rollupOrderDeliveryStatus` 已有 deliveryStatus 回写。
- **信用额度 AR 分量（1426-2 Deferred）**：`ErpMdPartner.receivableBalance`(DECIMAL 20,4) 缓存字段已存在；AR_INVOICE 落地后可补入应收分量。本计划为该 Deferred 的解除触发点，但**信用额度分量增强本身移出范围**（见 Non-Goals + Deferred，带命名触发条件），不阻塞发票/收款文档流结果表面。
- **剩余差距**：(1) 发票/收款 BizModel 无审批状态机与过账接线；(2) 无 AR_INVOICE/RECEIPT 过账 Provider；(3) 无收款→发票核销与 receivedStatus/writtenOffStatus 回写。

## Goals

- 销售发票 `IErpSalInvoiceBiz` 三轴审批状态机（submit/withdrawSubmit/approve/reject/reverseApprove/cancel），对齐 `state-machine.md` 与 `flow-overview.md §3`。
- 发票审核触发 **AR_INVOICE** 过账（借应收 / 贷收入 / 贷销项税），`posted=true`；反审核/作废走红字冲销。
- 收款 `IErpSalReceiptBiz` 三轴审批状态机；收款审核触发 **RECEIPT** 过账（借银行存款 / 贷应收），`posted=true`。
- 收款→发票**域级核销**（`ErpSalReceiptLine` 多对多），回写 `ErpSalInvoice.receivedStatus`/`receivedAmount` 与 `ErpSalReceipt.writtenOffStatus`，核销约束（同客户、金额不超余额、双方已审核）。
- 行为测试覆盖状态机/过账/核销/端到端（SO→Delivery→Invoice→Receipt 部分收款）。

## Non-Goals

- **财务辅助账 `ErpFinArApItem` 与正式核销单 `ErpFinReconciliation`**：属 0300-3 财务核销面；本计划只做 sales 域文档流 + 域级核销（`ErpSalReceiptLine`）。
- **销售订单信用额度「未结算应收余额」分量补入**：1426-2 Deferred，依赖 AR_INVOICE 后维护 `ErpMdPartner.receivableBalance` 缓存；本计划落地 AR_INVOICE 过账但**信用额度分量增强**（reading receivableBalance 在 credit check）留为显式 Follow-up（见 Deferred），不阻塞本计划结果表面。
- **销售退货发票（红冲）**：属 1.10（销售退货与退款）。
- **nop-wf 多级审批人路由**：审核 = 直接状态迁移 + `@BizMutation`（对齐 1132-2/1426-2 基线）。
- **自动核销规则与定时自动核销 / 多币种汇兑损益核销**：属 0300-3。
- **发票行从出库行自动复制 / 出库→发票自动生成**：手工指定回链。
- **银企直连/电子收款指令**：属 `treasury.md` 资金面。

## Task Route

- Type: `implementation-only change`。
- Owner Docs: `docs/design/sales/state-machine.md`（发票/收款三轴）、`docs/design/sales/README.md`（AR_INVOICE/RECEIPT 业务类型）、`docs/design/finance/posting.md`（过账 Provider 机制）、`docs/design/flow-overview.md §2.2/§3`（销售到收款 + 状态映射）。
- Skill Selection Basis: 全为 BizModel/IBiz/跨实体/过账 Provider 实现 → `nop-backend-dev`（与 0300-1 AP 面同构，对称参照 1132-2 已建立的 sales 跨域模式）。

## Infrastructure And Config Prereqs

- 模块依赖：`erp-sal-service` 需新增 compile 依赖 `app-erp-finance-service`（对齐 inventory 0811-2 / purchase 0300-1 接线 posting）；finance-dao/master-data-dao compile 已存在（1132-2 已接线 inventory/master-data）。无外部服务/端口/密钥/迁移。

## Execution Plan

### Phase 1 — 销售发票审批状态机 + AR_INVOICE 过账

Status: planned
Targets: `module-sales/erp-sal-dao/.../biz/IErpSalInvoiceBiz.java`、`module-sales/erp-sal-service/.../entity/ErpSalInvoiceBizModel.java`、新增 `.../posting/SalAcctDocProvider.java`（AR_INVOICE）、`.../posting/SalInvoicePostingDispatcher.java`+`SalInvoicePostingExecutor.java`、`.../ErpSalErrors.java`(扩)、`.../ErpSalConstants.java`(扩)、`erp-sal-service/.../_vfs/erp/sal/beans/app-service.beans.xml`、`erp-sal-service/pom.xml`
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: 1132-2 已完成（出库段 + ErpSalDelivery 三轴）；过账基础设施已完成。

- [ ] `Add`：`IErpSalInvoiceBiz` 声明三轴契约 `submit/withdrawSubmit/approve/reject/reverseApprove/cancel`（对齐 `IErpSalDeliveryBiz` 签名形状）。
  - Skill: `nop-backend-dev`
- [ ] `Add`：`ErpSalInvoiceBizModel` 实现三轴状态机（与 0300-1 发票面同构）；`@SingleSession @Transactional`，校验前置态，违例抛 `NopException`。扩 `ErpSalErrors` 新增发票作用域码 `ERR_INVOICE_*` 绑定 `ARG_INVOICE_CODE`（不复用出库单 `ARG_DELIVERY_CODE` 文案）。
  - Skill: `nop-backend-dev`
- [ ] `Add`：发票审核前置校验——客户启用（复用 1132-2/1426-2 `requireCustomerActive` 机制）、行非空。
  - Skill: `nop-backend-dev`
- [ ] `Add`：`SalAcctDocProvider implements IErpFinAcctDocProvider`（`EnumSet.of(AR_INVOICE)`，非默认），产 3 条 `VoucherFact`：借应收科目 / 贷收入科目 + 贷销项税科目（金额取发票 `totalAmountWithTax`/`totalAmount`/`totalTaxAmount`，对齐 `posting.md` AR_INVOICE 映射与 `InvAcctDocProvider` 形状）。注册于 `app-service.beans.xml`。
  - Skill: `nop-backend-dev`
- [ ] `Add`：`SalInvoicePostingDispatcher`+`SalInvoicePostingExecutor`（**对齐 `InvPostingDispatcher`/`InvPostingExecutor`：executor 无 `@Transactional`，跨域失败隔离由 Facade `IErpFinVoucherBiz.post()` 的 `REQUIRES_NEW` 承接，硬规则 1**）——发票 APPROVED 后组装 `PostingEvent`(AR_INVOICE, billHeadCode=invoice.code, billData 含金额族+customerId+orgId+acctSchemaId) 调 `IErpFinVoucherBiz.post`；成功 `posted=true`，失败吞异常保持 APPROVED+`posted=false`（对齐失败不阻塞终态合约）。
  - Skill: `nop-backend-dev`
- [ ] `Add`：`reverseApprove`/`cancel` 对已 `posted=true` 发票——先 `IErpFinVoucherBiz.reverse(code, AR_INVOICE, ctx)` 红字冲销，幂等防双冲销。
  - Skill: `nop-backend-dev`
- [ ] `Proof`：`TestErpSalInvoiceApproval`（三轴迁移正向/反向/非法迁移/停用客户）、`TestErpSalInvoicePosting`（APPROVED→凭证 posted=true 分录方向正确 借应收/贷收入+销项税；红字 reverseApprove）。验证命令 `mvn test -pl module-sales/erp-sal-service -am`。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] 发票三轴状态机行为可观察（迁移 + 非法迁移抛 NopException）
- [ ] 发票审核 posted=true 且凭证分录方向正确（AR_INVOICE 借应收/贷收入+销项税）；反审核生成红字凭证

### Phase 2 — 收款单审批状态机 + RECEIPT 过账 + 域级核销

Status: planned
Targets: `IErpSalReceiptBiz.java`、`ErpSalReceiptBizModel.java`、`SalAcctDocProvider`(扩 RECEIPT) 或独立 `SalReceiptAcctDocProvider`、`SalReceiptPostingDispatcher`+`Executor`、`ReceiptSettler`、`ErpSalErrors`/`ErpSalConstants`(扩)、beans.xml/pom.xml
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: Phase 1（发票已可审核生成应收）。

- [ ] `Add`：`IErpSalReceiptBiz` 三轴契约 + 域级核销动作 `settle(receiptId, List<SettlementAllocation>, ctx)` 与 `reverseSettlement`（与 0300-1 付款面对称）。
  - Skill: `nop-backend-dev`
- [ ] `Add`：`ErpSalReceiptBizModel` 实现收款三轴状态机；`approve` 触发 RECEIPT 过账。
  - Skill: `nop-backend-dev`
- [ ] `Add`：RECEIPT 过账——按 Phase 2 Decision 选定 `SalAcctDocProvider` 扩 `RECEIPT` 或独立 `SalReceiptAcctDocProvider`，产 2 条 VoucherFact：借银行存款科目 / 贷应收科目（金额取收款 totalAmount）。`SalReceiptPostingDispatcher`+`Executor`（同 Phase 1：executor 无 `@Transactional`，REQUIRES_NEW 由 Facade 承接），APPROVED→posted=true。
  - Skill: `nop-backend-dev`
- [ ] `Add`：`ReceiptSettler` 域级核销——按 `ErpSalReceiptLine`(receiptId→invoiceId+amount) 登记；约束：同客户、双方 APPROVED、核销金额不超发票未收余额（`totalAmountWithTax − receivedAmount`）与收款未核销余额；违例抛 `ERR_SETTLE_*`。回写 `ErpSalInvoice.receivedAmount`+=amt + `receivedStatus`(UNRECEIVED→PARTIAL→RECEIVED) 与 `ErpSalReceipt.writtenOffStatus`。
  - Skill: `nop-backend-dev`
- [ ] `Decision`：(a) RECEIPT Provider 归属——`SalAcctDocProvider` 扩 RECEIPT（与 AR_INVOICE 同类集中）vs 独立 `SalReceiptAcctDocProvider`。记录选择（与 0300-1 PAYMENT Decision 保持一致口径）、替代、残留风险。(b) 核销触发时机（审核自动 vs 独立 `settle` 两步）——与 0300-1 保持一致选择（MVP 独立 `settle`），记录理由与残留风险。
  - Skill: none
- [ ] `Add`：`reverseSettlement`——生成反向 ReceiptLine 冲销，恢复发票/收款余额与状态。
  - Skill: `nop-backend-dev`
- [ ] `Proof`：`TestErpSalReceiptApproval`（三轴 + RECEIPT 过账 posted=true）、`TestErpSalReceiptSettlement`（部分核销 receivedStatus=PARTIAL、全额 RECEIVED、跨客户拒绝、超额拒绝、reverseSettlement 恢复）。验证命令 `mvn test -pl module-sales/erp-sal-service -am`。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] 收款三轴状态机行为可观察；RECEIPT 过账 posted=true（借银行存款/贷应收）
- [ ] 域级核销：部分/全额核销正确回写 receivedStatus/receivedAmount/writtenOffStatus；约束违例拒绝；冲销恢复余额

### Phase 3 — 销售到收款端到端串联 + 反向场景 + 文档/日志

Status: planned
Targets: `TestErpSalOrderToCashEnd.java`、`docs/logs/2026/{07-02}.md`、`docs/backlog/core-business-roadmap.md`
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 1 + Phase 2。

- [ ] `Add`：端到端测试 `TestErpSalOrderToCashEnd`——建 SO→approve(信用额度校验)→Delivery(1132-2)→approve(触发出库移动+SALES_OUTPUT 成本结转凭证)→建 Invoice(回链 DeliveryLine)→approve(AR_INVOICE 凭证)→建 Receipt→approve(RECEIPT 凭证)→settle(部分核销，invoice.receivedStatus=PARTIAL)。断言全链状态、posted、receivedStatus 一致。
  - Skill: `nop-backend-dev`
- [ ] `Add`：反向场景测试——发票 reverseApprove（红字冲销 AR 凭证）、收款 reverseSettlement + reverseApprove（红字冲销 RECEIPT 凭证）。
  - Skill: `nop-backend-dev`
- [ ] `Proof`：根 `mvn test -fae` 无回归。验证命令 `mvn test -fae`（根目录）。
  - Skill: none
- [ ] `Add`：`docs/logs/2026/{执行当日 month-day}.md` 新增本计划条目（含验证状态）；`docs/backlog/core-business-roadmap.md` 工作项 1.7 AR 段标注进展。
  - Skill: none

Exit Criteria:

- [ ] 端到端 SO→Delivery→Invoice→Receipt(部分) 全链行为可观察（状态+posted+receivedStatus 一致），含反向冲销
- [ ] 当日日志已记；roadmap 1.7 AR 段进展已标注

## Draft Review Record

- Independent draft review iteration 1: **needs revision**（ses_0e10661e8ffe5ttk8aOEvCW5ZB，独立 general 子代理，新会话，与 0300-1 同审）— 全部 Current Baseline 主张经实时仓库核实属实（ErpSalInvoice/InvoiceLine/Receipt/ReceiptLine 字段/三轴含 receivedStatus/receivedAmount、空壳 BizModel/IBiz、字典值、`deliveryLineId` 回链、1132-2 Deferred「AR_INVOICE/RECEIPT」+ `ErpMdPartner.receivableBalance` 记录）。**一处 BLOCKER**（与 0300-1 同）：新过账 executor 误标 `@Transactional(REQUIRES_NEW)`，与权威源 `InvPostingExecutor.java:14-18` 冲突（REQUIRES_NEW 钉 Facade，executor 无注解，硬规则 1）。非阻塞：S1（Current Baseline 信用额度 AR 分量用了反松弛禁词「可选项」）、S3（RECEIPT Provider 归属未裁定）。
- Independent draft review iteration 2: **accept / consensus**（同 ses，blocker 已修 + 非阻塞已吸收）— 修订：(1) Phase 1/2 executor 项去除 `@Transactional(REQUIRES_NEW)`，改述「executor 无 @Transactional，REQUIRES_NEW 由 Facade 承接」并引 `InvPostingExecutor.java:14-18`；(2) S1 移除「可选项」禁词，信用额度分量增强明确移出范围（Non-Goal + Deferred 带命名触发条件）；(3) S3 RECEIPT Provider 归属并入 Phase 2 Decision（与 0300-1 PAYMENT 口径一致）。reviewer 明确声明「修此单一 blocker 后两计划为可接受执行契约」。规则 4/14 合并（发票+收款 AR 面）、跨计划一致性（与 0300-1 对称、0300-3 依赖一致）、反松弛、规则 10、命名/头/Closure Gates 全合规。**共识达成**：Plan Status 升级为 `active`。

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。结束时运行一次完整仓库验证。

- [ ] 范围内行为完成：发票/收款三轴状态机 + AR_INVOICE/RECEIPT 过账 + 域级核销 + 端到端全链落地，行为测试通过
- [ ] 相关文档对齐：`core-business-roadmap.md` 1.7 AR 段标注进展；当日日志已记
- [ ] 已运行验证：`mvn test -pl module-sales/erp-sal-service -am` 全绿；根 `mvn test -fae` = BUILD SUCCESS（无回归）
- [ ] 无范围内项目降级为 deferred/follow-up（财务辅助账/信用额度分量增强/退货发票/nop-wf/自动核销/汇兑损益/银企直连 均为计划内 Non-Goal）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：Plan Status、各 Phase Status、Exit Criteria、Closure Gates、日志一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于 `Closure` 节

## Deferred But Adjudicated

### 财务辅助账 ErpFinArApItem + 正式核销单 ErpFinReconciliation

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 属 finance 域结果表面（finance/ar-ap-reconciliation.md），本计划只做 sales 域文档流 + 域级核销（ErpSalReceiptLine）。由 0300-3 承接。
- Successor Required: yes（触发条件：实施 0300-3 AR/AP 核销与辅助账时）

### 销售订单信用额度「未结算应收余额」分量补入（1426-2 Deferred 解除触发）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 本计划落地 AR_INVOICE 过账，是 1426-2 Deferred「AR_INVOICE 落地时补入应收分量」的解除触发点；但信用额度分量增强（读取/维护 `ErpMdPartner.receivableBalance` 缓存并在 CreditLimitChecker 补入）是独立增强，不阻塞发票/收款文档流结果表面。MVP 信用额度口径维持 1426-2 现状（未出库订单金额）。
- Successor Required: yes（触发条件：实施信用额度应收分量增强时，需 AR_INVOICE 维护 receivableBalance 缓存）

### 自动核销规则与定时自动核销 / 多币种汇兑损益核销

- Classification: `optimization candidate`
- Why Not Blocking Closure: MVP 为手工/显式核销；属财务核销面（0300-3）。
- Successor Required: yes（触发条件：实施 0300-3 自动/跨币种核销时）

## Closure

Status Note: <待执行 + 独立结束审计后填写>

Closure Audit Evidence:

- Auditor / Agent: <待独立结束审计子代理（新会话）>

Follow-up:

- 财务辅助账 + 正式核销单（见上方 Deferred，0300-3 承接）
- 信用额度应收分量补入（见上方 Deferred；1426-2 Deferred 关联）
- 自动核销 / 汇兑损益核销（见上方 Deferred，0300-3 承接）
- 销售退货发票自动回链原发票冲销应收（触发条件：实施 1.10 销售退货与退款时）
