# 2026-07-02-0456-2 sales-return-and-refund

> Plan Status: active
> Last Reviewed: 2026-07-02
> Source: `docs/backlog/core-business-roadmap.md` 工作项 1.10（销售退货与退款）；前置计划 0300-2 Deferred「销售退货发票（红冲）属 1.10」、0300-3 Deferred「自动核销/退货归 1.10」
> Related: `2026-07-02-0300-2-sales-invoice-receipt-bizmodel.md`（AR 段已完成）、`2026-07-02-0456-1-purchase-return-and-refund.md`（AP 对称面，同批先执行）、`2026-07-02-0300-3-ar-ap-settlement-subledger.md`（财务辅助账/核销，已完成）
> Mission: erp
> Work Item: 销售退货单 BizModel + 库存反向入库 + 退货过账（红字冲减应收）+ 退款（1.10）
> Audit: required

## Current Baseline

实时仓库已核实的事实（逐一打开 ORM/BizModel/字典/过账基础设施确认，非采信记忆）：

- **销售退货单头** `ErpSalReturn`（`module-sales/model/app-erp-sales.orm.xml:626`）：`code`(orderCode)、`deliveryId`(to-one `delivery` 回链 `ErpSalDelivery`)、`customerId`(mandatory)、`warehouseId`(mandatory，"入库仓库")、`businessDate`(mandatory)、`currencyId`(mandatory)、`exchangeRate`(DECIMAL 20,8)；金额族 `amountSource`/`amountFunctional`/`totalAmount`(不含税)/`totalTaxAmount`/`totalAmountWithTax` 全 DECIMAL(20,4)；两轴状态——`docStatus`(erp-sal/doc-status DRAFT/ACTIVE/CANCELLED)、`approveStatus`(erp-sal/approve-status UNSUBMITTED/SUBMITTED/APPROVED/REJECTED)；`posted`+`postedAt`/`postedBy`；`approvedBy`/`approvedAt`。**无 `returnStatus`、无 `refundStatus` 字段**（设计 `sales/returns.md §退货单状态机` 的第三/四轴 `returnStatus`/`refundStatus` 在模型中不存在）。
- **销售退货单行** `ErpSalReturnLine`(:681)：`returnId`(mandatory)、`deliveryLineId`(to-one `deliveryLine` 回链 `ErpSalDeliveryLine`)、`lineNo`、`materialId`(mandatory)、`skuId`、`uoMId`(mandatory)、`quantity`(mandatory, DECIMAL)、`unitPrice`/`taxRate`/`taxAmount`/`amount`(DECIMAL 20,4)、`reason`。
- **源出库行无退货累计字段**：全仓 grep `returnedQuantity` 在 `*.orm.xml` 零命中；`ErpSalDeliveryLine` 不持有「已退货数量」列。退货数量上限校验须基于聚合查询（已审核退货行 SUM）而非存储列。
- **BizModel 空壳**：`ErpSalReturnBizModel`/`ErpSalReturnLineBizModel` 为 codegen 空 `CrudBizModel`（与 `ErpPurReturnBizModel` 同形，仅构造器）；`IErpSalReturnBiz` 空 `ICrudBiz`。
- **库存跨域契约**（已完成于 0811-2）：`IErpInvStockMoveBiz.generateMove(StockMoveRequest, IServiceContext)`（幂等键 `(relatedBillType, relatedBillCode)`，业务联动自动 DRAFT→DONE）。`StockMoveRequest` 含 `moveType`/`sourceWarehouseId`/`destWarehouseId`/`relatedBillType`/`relatedBillCode`/`lines`(`StockMoveLineRequest`: materialId/skuId/uoMId/quantity/unitCost/currencyId/batchNo/serialNo)。`IErpInvStockMoveBiz` 另有 `reverse(Long moveId, IServiceContext)`（DONE 纠错路径，生成反向冲销移动单）与 `findByRelatedBill(relatedBillType, relatedBillCode, ctx)`（跨域只读反查）。`billType` 枚举表（`data-dependency-matrix.md §5.2`）**已登记** `SAL_RETURN`（销售退货单）作为 inventory 弱指针取值。
- **过账基础设施**（已完成于 0811-1/2030-1/0300-2）：入口 facade `IErpFinVoucherBiz.post`/`reverse`；`IErpFinAcctDocProvider` SPI + `ErpFinAcctDocRegistry`。`SalAcctDocProvider`（AR_INVOICE/RECEIPT）已存在；`SalPostingExecutor`（共享执行器，executor 无 `@Transactional`）已存在。**SALES_OUTPUT 过账实际记账**（`InvAcctDocProvider.java:53-54`）：借 6401 主营业务成本/贷 1401 库存商品，金额=`TOTAL_COST`，**不记账应收/收入/销项税**（收入/应收/税由 AR_INVOICE 在开票时记账）——故 SALES_RETURN 凭证反向 SALES_OUTPUT 只冲成本/存货。
- **业财业务类型枚举/字典**：`ErpFinBusinessType` 当前 13 常量（PURCHASE_INPUT(10)…EXCHANGE_GAIN_LOSS(130)），**无退货冲减类型**；`business-type.dict.yaml` 同步无退货项。（0456-1 将追加 `PURCHASE_RETURN(140)`；本计划追加 `SALES_RETURN(150)`，两者独立无冲突。）
- **财务辅助账/核销**（已完成于 0300-3，机制已逐行核实）：`ErpFinArApItemGenerator.resolveProfile`（`ErpFinArApItemGenerator.java:121-137`）switch **仅识别** AP_INVOICE/AR_INVOICE/PAYMENT/RECEIPT，**default→null**——SALES_RETURN 当前**不生成**辅助账（**扩展确认必需**，非「如需」）。`PartnerBalanceUpdater.sumOpen`（`:46-62`）按 `eq("direction",DIRECTION)` 过滤后 **SUM** 所有 `openAmountFunctional`；方向常量仅 `DIRECTION_RECEIVABLE=10`/`DIRECTION_PAYABLE=20`（`ErpFinConstants.java:23-24`），**无「减少」方向**。故退货回减应收的唯一**无侵入**机制 = `DIRECTION_RECEIVABLE` + **负** `openAmountFunctional`（credit-memo 贷项，`sumOpen` 自然减计）。`sourceBillType` 常量仅 4 项（`:38-41`），退货需新增 `SOURCE_BILL_SAL_RETURN`。
- **AR 收款/核销链**（已完成于 0300-2）：`ErpSalReceipt`/`ErpSalReceiptLine`（域级核销）、`receivedStatus`/`writtenOffStatus` 已落地；`ErpSalInvoice.receivedStatus`/`receivedAmount` 回写机制在位。已收款退货的退款编排可复用收款反向路径。
- **客户启用校验**：`ErpMdPartner.status` + `requireCustomerActive` 机制已由 1132-2/1426-2 建立，可复用 `ErpSalErrors` 作用域码模式。
- **剩余差距**：(1) 退货单无审批状态机；(2) 审核不触发库存反向入库移动；(3) 无退货数量上限校验（≤ 出库未退货量）；(4) 无 SALES_RETURN 过账类型与红字冲减凭证；(5) 退货不回减客户应收余额；(6) 已收款退货无退款/反向收款路径。

## Goals

- 销售退货单 `IErpSalReturnBiz` 审批状态机（submit/withdrawSubmit/approve/reject/reverseApprove/cancel），对齐 `sales/returns.md §退货单状态机` 与 `flow-overview.md §3`（复用现有 `docStatus`+`approveStatus` 两轴，不新增状态字段——见 Task Route Decision）。
- 退货审核触发**库存反向入库移动**（`generateMove`，`relatedBillType=SAL_RETURN`，目的仓=退货仓库，幂等），库存增加。
- 退货数量上限校验：每行 `quantity` ≤ 对应 `ErpSalDeliveryLine` 已出库量 − 该出库行已审核退货量（聚合查询）。
- 退货审核触发 **SALES_RETURN** 过账（反向 **SALES_OUTPUT**：借库存商品(1401) / 贷主营业务成本(6401)，金额=退货成本 `TOTAL_COST`；SALES_OUTPUT 不记账应收/收入/销项税，故本凭证只冲成本/存货，收入/应收 GL 红字冲销属 credit-note Non-Goal），`posted=true`；反审核/作废走红字冲销。
- 退货过账回减客户应收余额（经 0300-3 辅助账/余额管线：SALES_RETURN 生成 `DIRECTION_RECEIVABLE` 负 openAmount 的 `ErpFinArApItem`（credit-memo）→ `receivableBalance` 下降）。
- **退款编排**：未收款退货→credit-memo 负 AR 辅助账回减应收；已收款退货→生成反向收款（冲销原收款核销 + 回写 `receivedStatus`），应收/退款闭环一致。退款**方式路由**（原路退回/其他账户/预收款抵扣/现金）为 Non-Goal。
- 行为测试覆盖状态机/数量约束/库存反向入库/过账/应收余额回减/退款。

## Non-Goals

- **`returnStatus`/`refundStatus` 状态轴字段**：设计提出的第三/四轴语义上是**源出库行累计退货进度**与**退款进度**（派生视图）；本计划不新增 ORM 字段，改以聚合查询 + AR 辅助账 open/reconciled 状态表达（见 Task Route Decision）。
- **红字发票自动生成与已核销发票回冲**：`sales/returns.md §红字发票处理` 的「已开票退货生成红字发票 + 蓝字冲销标志 + 已核销须先撤回」属发票生命周期深化；本计划退货只生成**退货冲减凭证**，不自动回冲 `ErpSalInvoice.receivedStatus`。
- **退款方式路由（原路退回/其他账户/预收款抵扣/现金）**：属 `treasury.md` 资金面（资金账户/支付通道）；本计划退款仅做记账（反向收款凭证），不做支付指令。
- **换货（退货+重新发货）**：换货 = 退货单 + 新销售出库两单，本计划只做退货单本身。
- **退货质检联动（NCR/CAPA）**：`sales/returns.md §质量域协作` 的退货触发质检/NCR 属 2.4（质检触发）跨域事件，本计划不做。
- **批次/序列号退货追溯**：依赖 1.11（批次追溯链，inventory ORM 无 `originMoveId`/`batchId` 列）；本计划退货移动单 `batchNo`/`serialNo` 透传，不做批次级约束。
- **退货入库成本三选一（原出库成本/当前库存成本/退货协议价）**：`returns.md §退货成本处理`；MVP 退货入库按原出库成本（`unitCost` 透传），成本重估归财务成本核算面。
- **nop-wf 多级审批人路由**：审核 = 直接状态迁移 + `@BizMutation`（对齐 1132-2/0300-2 基线）。
- **多币种退货汇兑损益**：退货按原出库汇率冲减，跨期汇率差异归期末汇兑面。

## Task Route

- Type: `implementation-only change`（业务逻辑实现；退货实体/字段不变，全部走新增 Java + beans.xml 接线 + 业财类型枚举/字典扩展）。注：SALES_RETURN 枚举/字典扩展触及 `erp-fin-dao`/`erp-fin-meta`，但 `ErpFinBusinessType` 注释明确为可扩展门面（非 `<domain>/model/*.orm.xml` 保护区域）。
- Owner Docs: `docs/design/sales/returns.md`（退货流程/状态机/红字凭证/退款）、`docs/design/sales/state-machine.md`（两轴状态分离范式）、`docs/design/inventory/cross-domain.md`（`generateMove` 联动）、`docs/design/finance/posting.md`（过账三层 + Provider + 冲销）、`docs/design/flow-overview.md §3`（状态映射）、`docs/design/finance/ar-ap-reconciliation.md`（辅助账生成约束）。
- Skill Selection Basis: 全为 BizModel/IBiz/跨实体调用/过账 Provider/枚举扩展 → 加载 `nop-backend-dev`（与 0456-1 AP 退货面对称参照，复用 0300-2 已建立的 sales 跨域模式）。
- **Decision（状态轴）**：**选择**不新增 `returnStatus`/`refundStatus` 字段，复用现有 `docStatus`(DRAFT/ACTIVE/CANCELLED)+`approveStatus` 两轴表达退货单生命周期（终态 = ACTIVE+APPROVED+`posted=true`），「部分/全额退货」作源出库行派生查询、「退款进度」作 AR 辅助账 open/reconciled 派生。**替代**：新增两列（ORM 保护区域变更，需 ask-first + 重新 codegen）。**残留风险**：列表页无法直接按 returnStatus/refundStatus 筛选，列为 Follow-up（触发条件：退货/退款报表需高频筛选时再评估加冗余列）。

## Infrastructure And Config Prereqs

- 退货数量约束配置（`returns.md §配置项`）：`erp-sal.return-qty-limit`（默认=出库未退货量）、`erp-sal.return-approval-required`(默认 true)、`erp-sal.return-reason-required`(默认 true)、`erp-sal.return-quality-check`(默认 true，**但质检联动属 Non-Goal，配置仅存值不触发**）。经 `AppConfig.var(..., defaultValue)` 读取，缺失走默认，无需 .env。
- 模块依赖：`erp-sal-service` 已 compile 依赖 `app-erp-finance-service`（0300-2 已接线 posting）；finance-dao/master-data-dao compile 已存在。无新增外部服务/端口/密钥/数据迁移。

## Execution Plan

### Phase 1 — 销售退货单审批状态机 + 退货数量约束 + 库存反向入库移动

Status: planned
Targets: `module-sales/erp-sal-dao/.../biz/IErpSalReturnBiz.java`、`module-sales/erp-sal-service/.../entity/ErpSalReturnBizModel.java`、新增 `.../entity/ReturnQtyValidator.java`、`.../ErpSalErrors.java`(扩)、`.../ErpSalConstants.java`(扩)、`erp-sal-service/.../_vfs/erp/sal/beans/app-service.beans.xml`
Skill: `nop-backend-dev`

- Item Types: `Add | Decision`
- Prereqs: 1132-2 已完成（`ErpSalDelivery` 三轴 + 出库审核触发库存移动）；0811-2 已完成（`generateMove` 契约）。

- [ ] `Add`：`IErpSalReturnBiz` 声明审批契约 `submit/withdrawSubmit/approve/reject/reverseApprove/cancel`（`@BizMutation`+`@Name`，对齐 `IErpSalDeliveryBiz`/`IErpSalInvoiceBiz` 签名形状）。
  - Skill: `nop-backend-dev`
- [ ] `Add`：`ErpSalReturnBizModel` 实现审批状态机（UNSUBMITTED↔SUBMITTED→APPROVED/REJECTED；reverseApprove APPROVED→REJECTED；cancel 非终态→CANCELLED，APPROVED 须先冲销）。`@BizMutation` 自动包装事务（对齐 0300-2，不叠加 `@Transactional`/`@SingleSession`），每迁移校验前置态，违例抛 `NopException`。
  - Skill: `nop-backend-dev`
- [ ] `Add`：审核前置校验——客户启用（复用 1132-2 `requireCustomerActive`；扩 `ErpSalErrors` 新增退货作用域码 `ERR_RETURN_*` 绑定 `ARG_RETURN_CODE`，不复用发票/出库单文案）、源出库单 `deliveryId` 非空且 `ErpSalDelivery.approveStatus=APPROVED`、行非空、`reason` 必填（按配置）。
  - Skill: `nop-backend-dev`
- [ ] `Add`：`ReturnQtyValidator`——按 `ErpSalReturnLine.deliveryLineId` 分组，每行 `quantity` ≤ `ErpSalDeliveryLine.quantity`（已出库量）− 该出库行**已审核**退货行 SUM（聚合查询，排除当前退货单）。超限抛 `ERR_RETURN_QTY_EXCEED`（提示最大可退量）。跨退货单并发超额由退货单 `version` 乐观锁兜底（低频边沿，列为 Follow-up）。
  - Skill: `nop-backend-dev`
- [ ] `Decision`：退货数量上限「按出库行聚合」vs「存储 `returnedQuantity` 列」——**选择**聚合查询（无 ORM 变更，保持 implementation-only），**替代**在 `ErpSalDeliveryLine` 加 `returnedQuantity`（保护区域 + codegen 回滚风险），**残留风险**：跨退货单并发超额理论上可能，由 `version` 乐观锁 + 审核时重查兜底。
  - Skill: none
- [ ] `Add`：审核 APPROVED 触发库存反向入库移动——组装 `StockMoveRequest`(`moveType`=入库方向、`destWarehouseId`=return.warehouseId、`relatedBillType`="SAL_RETURN"、`relatedBillCode`=return.code、`lines` 由退货行映射)，调 `IErpInvStockMoveBiz.generateMove`。幂等键 `(SAL_RETURN, return.code)` 防重复触发。失败抛 `NopException` 回滚审核（库存物理正确性硬约束，对齐 1132-2 出库触发模式）。
  - Skill: `nop-backend-dev`
- [ ] `Add`：`reverseApprove`/`cancel` 对已生成库存移动的退货单——先校验库存移动可冲销（`findByRelatedBill("SAL_RETURN", code)` 取移动单，若已 DONE 则 `reverse` 生成反向出库移动冲减库存），再走状态机回退。
  - Skill: `nop-backend-dev`

Exit Criteria:

> Phase 1 交付退货单审批状态机 + 数量约束 + 库存反向入库端到端（审核→入库移动→库存增加）。完整仓库验证属 Closure Gates。

- [ ] 退货单三轴迁移正向/反向/非法迁移单测通过；客户停用/源出库未审核被拒
- [ ] 数量约束：超出库量被拒（提示最大可退量）；部分退货放行
- [ ] 审核→`generateMove` 生成入库移动单（`relatedBillType=SAL_RETURN`），重复审核幂等不重复生成；库存余额增加可断言（本地化集成测试）

### Phase 2 — SALES_RETURN 过账 + 红字冲减应收 + 客户余额回减 + 退款编排 + 端到端

Status: planned
Targets: `module-finance/erp-fin-dao/.../ErpFinBusinessType.java`(扩 `SALES_RETURN`)、`module-finance/erp-fin-meta/.../_vfs/dict/erp-fin/business-type.dict.yaml`(扩)、`module-sales/erp-sal-service/.../posting/SalAcctDocProvider.java`(扩 SALES_RETURN) 或新增 `SalReturnAcctDocProvider.java`、新增 `.../posting/SalReturnPostingDispatcher.java`(复用 `SalPostingExecutor`)、`module-finance/erp-fin-service/.../ErpFinArApItemGenerator.java`(核实/扩展 SALES_RETURN 方向)、`erp-sal-service/.../_vfs/erp/sal/beans/app-service.beans.xml`
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: Phase 1 完成；过账基础设施（0811-1/2030-1/0300-2）+ 辅助账（0300-3）已完成；0456-1 已追加 `PURCHASE_RETURN(140)`（枚举与字典可并行追加无冲突，本计划追加 150）。

- [ ] `Add`：`ErpFinBusinessType` 新增 `SALES_RETURN(150)`（0456-1 占用 140）；`business-type.dict.yaml` 同步「销售退货冲减 value:150」。
  - Skill: `nop-backend-dev`
- [ ] `Add`：扩展 `SalAcctDocProvider`（`EnumSet.of(AR_INVOICE, RECEIPT, SALES_RETURN)`）产 SALES_RETURN facts——**反向 SALES_OUTPUT**（`InvAcctDocProvider` 的 SALES_OUTPUT 借 6401 主营业务成本/贷 1401 库存商品，`InvAcctDocProvider.java:53-54`，**不记账应收/收入/销项税**——这些属 AR_INVOICE 职责），故退货冲减：借库存商品(1401) / 贷主营业务成本(6401)，金额 = 退货成本 `TOTAL_COST`（取原出库成本层）。**收入/应收/销项税的红字 GL 冲销属 credit-note（红字发票）面，为本计划 Non-Goal**；应收回减经辅助账层（见下方 ar_ap 项）而非 GL 凭证。注册 `app-service.beans.xml`。
  - Skill: `nop-backend-dev`
- [ ] `Decision`：SALES_RETURN facts 的 Provider 归属——**倾向**扩展 `SalAcctDocProvider`（同类集中，0300-2 范式）。**考虑的替代**：① 新增独立 `SalReturnAcctDocProvider`（隔离更清但增 Bean）；② 放入 inventory 域 `InvAcctDocProvider`（SALES_RETURN 是 SALES_OUTPUT 的语义逆，同存货科目域，但跨域归属混乱，rejected）。**残留风险**：`SalAcctDocProvider` 内分支增多，需单测覆盖各 businessType。
  - Skill: none
- [ ] `Add`：`SalReturnPostingDispatcher`（复用 `SalPostingExecutor`，executor 无 `@Transactional`）——退货 APPROVED 后组装 `PostingEvent`(SALES_RETURN, billHeadCode=return.code, billData 含 `TOTAL_COST`(退货成本)+`CUSTOMER_ID`+`TOTAL_AMOUNT_WITH_TAX`(退货含税售价，供辅助账用)+orgId+acctSchemaId) 调 `IErpFinVoucherBiz.post`；成功置 `posted=true`，失败吞异常保持 APPROVED+`posted=false`（对齐 0300-2 合约）。源单据 posted 标志由 BizModel 主事务持久化。
  - Skill: `nop-backend-dev`
- [ ] `Add`：扩展 `ErpFinArApItemGenerator.resolveProfile`（**确认必需**——default 当前返回 null）：新增 `case SALES_RETURN` → `new SourceProfile(DIRECTION_RECEIVABLE, SOURCE_BILL_SAL_RETURN)`，并在 `ErpFinConstants` 新增 `SOURCE_BILL_SAL_RETURN`。退货辅助账 `openAmountFunctional` = **负** `TOTAL_AMOUNT_WITH_TAX`（退货含税售价，credit-memo 模型），使 `PartnerBalanceUpdater.sumOpen` 自然减计 `receivableBalance`（机制见 Current Baseline + 下方 Decision）。`cancelOnReverse(code, SALES_RETURN, ctx)` 走既有 `sourceBillType` 反查取消路径。**约束**：finance 为 DAG 顶，生成器只读 `PostingEvent.billData`，不引入反向依赖。
  - Skill: `nop-backend-dev`
- [ ] `Decision`：退货回减应收余额的辅助账机制——**选择** `DIRECTION_RECEIVABLE` + **负** `openAmountFunctional`（credit-memo 贷项，无侵入，`sumOpen`/`PartnerBalanceUpdater` 零改动）。**考虑的替代**：① 新增「应收减少」方向常量 + 扩展 `sumOpen`（污染方向枚举，rejected）；② 复用 AR_INVOICE profile 发负项（语义混同发票，rejected）。**残留风险（销售面特有）**：SALES_OUTPUT 仅记账 COGS/存货、**不记账应收**，故 SALES_RETURN 凭证只冲减成本/存货，GL「应收账款」（由 AR_INVOICE 记账）不被本凭证冲减——存在 **GL 应收 vs 子账 `receivableBalance` 暂态不一致**，完整收入/应收 GL 红字冲销属 credit-note（红字发票）Non-Goal；需在 Proof 验证负项不破坏 `sumOpen`/核销数学，并在 Deferred 记录 GL 对账跟进。
  - Skill: none
- [ ] `Add`：**退款编排**——(a) 未收款退货：SALES_RETURN 过账生成负 AR 辅助账（credit-memo）即回减 `receivableBalance`（无需额外动作）；(b) 已收款退货：查 `ErpSalReceipt`/`ErpSalReceiptLine` 是否对该客户的原发票有核销记录，若有则生成**反向收款核销行**（金额为负，回写 `ErpSalInvoice.receivedStatus`/`receivedAmount` 与 `ErpSalReceipt.writtenOffStatus`，复用 0300-2 核销回写机制），使应收/退款闭环一致。退款**方式路由**（资金账户）属 Non-Goal。
  - Skill: `nop-backend-dev`
- [ ] `Add`：`reverseApprove`/`cancel` 对已 `posted=true` 退货单——调 `IErpFinVoucherBiz.reverse(code, SALES_RETURN, ctx)` 生成红字冲销凭证，幂等防双冲销；`cancelOnReverse` 取消负 AR 辅助账，`receivableBalance` 恢复；已退款情形同步回滚反向收款核销。
  - Skill: `nop-backend-dev`
- [ ] `Proof`：`TestErpSalReturnApproval`（三轴迁移）、`TestErpSalReturnQty`（数量约束/部分退货）、`TestErpSalReturnInventory`（审核→入库移动→库存增加）、`TestErpSalReturnPosting`（APPROVED→SALES_RETURN 凭证(借存货/贷成本) + `DIRECTION_RECEIVABLE` 负 openAmount 辅助账 + `receivableBalance` 下降额 = 退货含税售价；负项不破坏 `sumOpen`；红字 reverseApprove→辅助账 CANCELLED + `receivableBalance` 恢复）、`TestErpSalReturnRefund`（未收款→负 AR 辅助账回减应收；已收款→反向收款核销行 + 发票 receivedStatus 回写）、端到端（SO→Delivery→Return 部分退货→应收余额回减）。验证命令 `mvn test -pl module-sales/erp-sal-service -am`。
  - Skill: `nop-backend-dev`

Exit Criteria:

> Phase 2 交付退货过账端到端（审核→反向 SALES_OUTPUT 凭证+负 openAmount AR 辅助账→应收余额回减）+ 退款编排（未收款 credit-memo/已收款反向核销）+ 红字冲销。完整仓库验证属 Closure Gates。

- [ ] APPROVED→SALES_RETURN 凭证落库（借存货/贷成本）+ `posted=true`；辅助账为 `DIRECTION_RECEIVABLE` 负 openAmount；客户 `receivableBalance` 下降额 = 退货含税售价
- [ ] 负 openAmount 辅助账不破坏 `sumOpen`/核销数学（闭环 Decision 残留风险）
- [ ] 已收款退货→反向收款核销行 + `ErpSalInvoice.receivedStatus`/`receivedAmount` 回写
- [ ] `reverseApprove` 已过账退货单→红字凭证 + 辅助账 CANCELLED + `receivableBalance` 恢复
- [ ] 端到端（SO→Delivery→Return）单测全绿

## Draft Review Record

- Independent draft review iteration 1: **acceptable as-is / accept**（`ses_0e0839e3bffeGjyqx3YMKah9Wk`，独立 general 子代理，新会话）— 全部 Current Baseline 主张经实时仓库逐行核实属实（ErpSalReturn 两轴无 returnStatus/refundStatus、无 returnedQuantity、空壳 BizModel、generateMove/reverse 契约、ErpFinBusinessType max=130、SalAcctDocProvider/SalPostingExecutor、SAL_RETURN 已登记 §5.2、AR 核销链 receivedStatus/writtenOffStatus 均确认）。VERDICT acceptable。本迭代额外吸收跨计划一致性修订（与 0456-1 同源机制）：① 去除「如需」hedge（确认必需）；② 「方向=应收减少」更正为 `DIRECTION_RECEIVABLE` + 负 openAmount + 新增 `SOURCE_BILL_SAL_RETURN`；③ **关键更正**：SALES_RETURN 凭证反向 **SALES_OUTPUT**（借存货/贷成本，`InvAcctDocProvider:53-54`），非反向 AR_INVOICE（收入/应收属 AR_INVOICE 职责）；④ 新增 GL-vs-子账应收暂态不一致残留风险（credit-note GL 冲销为 Non-Goal）；⑤ `reverse(moveId,ctx)` 签名。非阻塞 S1-S4 已吸收。**与 0456-1 机制对称、枚举码不冲突（140 vs 150）。**
- Independent draft review iteration 2: **needs revision**（`ses_0e0774747ffegtnwtVVDJET7uk`，独立 general 子代理，新会话）— 凭证更正（反向 SALES_OUTPUT 借1401/贷6401，对齐 `InvAcctDocProvider:52-54`）、ar_ap 机制（DIRECTION_RECEIVABLE+负 openAmount，确认必需，新增 SOURCE_BILL_SAL_RETURN）、GL-vs-子账残留记录、`reverse(moveId,ctx)`、150 vs 140 不冲突——**全部 PASS**。唯一 BLOCKER：**Goals §line 32** 仍保留旧错误凭证描述（反向 AR 借收入/销项税/贷应收 红字），与更正后的 Baseline/Phase2/Proof/Exit/Deferred 矛盾。已修订：Goals line 32 改为「反向 SALES_OUTPUT（借库存商品1401/贷主营业务成本6401，TOTAL_COST）+ credit-note 收入/应收 GL 冲销 Non-Goal」，line 33/34 同步对齐 credit-memo 负 AR 辅助账措辞。该 BLOCKER 为 Goals 文本与已核实正文的对齐（非范围/机制判断），审查者已预授权「修正后即可翻 active」。**共识达成**：Plan Status 升级为 `active`。

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。结束时运行一次完整仓库验证。

- [ ] 范围内行为完成：退货单审批状态机 + 库存反向入库 + SALES_RETURN 过账 + 应收余额回减 + 退款编排，行为测试通过
- [ ] 相关文档对齐：`core-business-roadmap.md` 1.10 标注进展；当日日志已记；`sales/returns.md` 偏离（returnStatus/refundStatus 派生化）补注
- [ ] 已运行验证：`mvn test -pl module-sales/erp-sal-service -am` 全绿；根 `mvn test -fae` = BUILD SUCCESS（无回归）
- [ ] 无范围内项目降级为 deferred/follow-up（红字发票自动生成/退款方式路由/换货/退货质检/批次退货/nop-wf 均为计划内 Non-Goal）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：Plan Status、各 Phase Status、Exit Criteria、Closure Gates、日志一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于 `Closure` 节

## Deferred But Adjudicated

### `returnStatus`/`refundStatus` 状态轴字段（owner-doc 偏离）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 「部分/全额退货」「退款进度」是源出库行累计进度与 AR 辅助账 open/reconciled 状态的派生视图，非退货单固有状态；本计划以聚合查询 + 辅助账状态表达，保持 implementation-only。
- Successor Required: yes（触发条件：退货/退款报表需高频筛选，或需存储列做物化时，再评估加 `ErpSalDeliveryLine.returnedQuantity` 冗余列 + 重新 codegen）

### 跨退货单并发超额（同一出库行并发审核两单退货）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 低频边沿；退货单 `version` 乐观锁 + 审核时重查聚合兜底。
- Successor Required: yes（触发条件：高频并发退货场景出现，改出库行存储 `returnedQuantity` + 行级锁）

### 红字发票自动生成与已核销发票回冲（含收入/应收 GL 红字冲销）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 属发票生命周期深化（已开票退货生成红字发票 + 蓝字冲销标志 + 已核销须先撤回）；本计划退货只生成反向 SALES_OUTPUT 凭证（成本/存货）+ 反向收款核销。**完整收入/应收/销项税 GL 红字冲销（借收入红字/贷应收红字）属 credit-note 面**——SALES_OUTPUT 不记账应收，故 SALES_RETURN 凭证不冲 GL 应收，存在 GL 应收 vs 子账 `receivableBalance` 暂态不一致，由 credit-note 红字发票落地时补齐 GL 侧。
- Successor Required: yes（触发条件：实施发票红冲/credit-note 编排，或期末 GL-子账应收对账报告时）

### 退款方式路由（原路退回/其他账户/预收款抵扣/现金）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 属 `treasury.md` 资金面（资金账户/支付通道）；本计划退款仅做记账（反向收款凭证），不做支付指令。
- Successor Required: yes（触发条件：实施 treasury 资金支付指令时）

### 退货质检联动（NCR/CAPA）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 退货触发质检/NCR 属 2.4 跨域事件编排；本计划不做质量域联动。
- Successor Required: yes（触发条件：实施 2.4 质检触发时）

## Closure

Status Note: <why the plan can close>

Closure Audit Evidence:

- Auditor / Agent: <independent auditor or independent subagent>
- Evidence: <task id / log link / walkthrough record>

Follow-up:

- `returnStatus`/`refundStatus` 冗余列/物化（见上方 Deferred）
- 跨退货单并发超额行级锁（见上方 Deferred）
- 红字发票自动生成/已核销发票回冲（见上方 Deferred，发票深化时）
- 退款方式路由（见上方 Deferred，treasury 资金面时）
- 退货质检联动（见上方 Deferred，2.4 时）
- 批次级退货追溯约束（触发条件：1.11 批次追溯链落地后）
