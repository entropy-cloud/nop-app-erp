# 2026-07-02-0456-1 purchase-return-and-refund

> Plan Status: completed
> Last Reviewed: 2026-07-02
> Source: `docs/backlog/core-business-roadmap.md` 工作项 1.9（采购退货与退款）；前置计划 0300-1 Deferred「采购退货发票（红冲）属 1.9」、0300-3 Deferred「自动核销/退货归 1.9」
> Related: `2026-07-02-0300-1-purchase-invoice-payment-three-way-match.md`（AP 段已完成）、`2026-07-02-0456-2-sales-return-and-refund.md`（AR 对称面，同批）、`2026-07-02-0300-3-ar-ap-settlement-subledger.md`（财务辅助账/核销，已完成）
> Mission: erp
> Work Item: 采购退货单 BizModel + 库存反向移动 + 退货过账（红字冲减应付）（1.9）
> Audit: required

## Current Baseline

实时仓库已核实的事实（逐一打开 ORM/BizModel/字典/过账基础设施确认，非采信记忆）：

- **采购退货单头** `ErpPurReturn`（`module-purchase/model/app-erp-purchase.orm.xml:641`）：`code`(orderCode)、`receiveId`(to-one `receive` 回链 `ErpPurReceive`)、`supplierId`(mandatory)、`warehouseId`(mandatory，"出库仓库")、`businessDate`(mandatory)、`currencyId`(mandatory)、`exchangeRate`(DECIMAL 20,8)；金额族 `amountSource`/`amountFunctional`/`totalAmount`(不含税)/`totalTaxAmount`/`totalAmountWithTax` 全 DECIMAL(20,4)；两轴状态——`docStatus`(erp-pur/doc-status DRAFT/ACTIVE/CANCELLED=10/20/30)、`approveStatus`(erp-pur/approve-status UNSUBMITTED/SUBMITTED/APPROVED/REJECTED=10/20/30/40)；`posted`+`postedAt`/`postedBy`；`approvedBy`/`approvedAt`。**无 `returnStatus` 字段**（设计 `purchase/returns.md §退货单状态机` 的第三轴 `returnStatus` 初始/部分退货/全额退货在模型中不存在）。
- **采购退货单行** `ErpPurReturnLine`(:685)：`returnId`(mandatory)、`receiveLineId`(to-one `receiveLine` 回链 `ErpPurReceiveLine`)、`lineNo`、`materialId`(mandatory)、`skuId`、`uoMId`(mandatory)、`quantity`(mandatory, DECIMAL)、`unitPrice`/`taxAmount`/`amount`(DECIMAL 20,4)、`taxRate`(DECIMAL 10,4)、`reason`。
- **源入库行无退货累计字段**：全仓 grep `returnedQuantity` 在 `*.orm.xml` 零命中；`ErpPurReceiveLine` 不持有「已退货数量」列。退货数量上限校验须基于聚合查询（已审核退货行 SUM）而非存储列。
- **BizModel 空壳**：`ErpPurReturnBizModel`/`ErpPurReturnLineBizModel` 为 codegen 空 `CrudBizModel`（已核实 `ErpPurReturnBizModel.java` 仅 15 行构造器）；`IErpPurReturnBiz` 空 `ICrudBiz`。
- **库存跨域契约**（已完成于 0811-2）：`IErpInvStockMoveBiz.generateMove(StockMoveRequest, IServiceContext)`（幂等键 `(relatedBillType, relatedBillCode)`，业务联动自动 DRAFT→DONE 一次推进）。`StockMoveRequest` 含 `moveType`/`sourceWarehouseId`/`destWarehouseId`/`relatedBillType`/`relatedBillCode`/`acctSchemaId`/`lines`(`StockMoveLineRequest`: materialId/skuId/uoMId/quantity/unitCost/currencyId/batchNo/serialNo)。`IErpInvStockMoveBiz` 另有 `reverse(Long moveId, IServiceContext)`（DONE 纠错路径，生成反向冲销移动单，数量取负）与 `findByRelatedBill(relatedBillType, relatedBillCode, ctx)`（跨域只读反查）。`billType` 枚举表（`data-dependency-matrix.md §5.2`）**已登记** `PUR_RETURN`（采购退货单）作为 inventory 弱指针取值。
- **过账基础设施**（已完成于 0811-1/2030-1/0300-1）：入口 facade `IErpFinVoucherBiz.post(PostingEvent)`（幂等，REQUIRES_NEW 钉 Facade）/`reverse(code, businessType, ctx)`；`IErpFinAcctDocProvider` SPI（getSupportedBusinessTypes + createFacts → List<VoucherFact>）+ `ErpFinAcctDocRegistry` 自动聚合。`PurAcctDocProvider`（AP_INVOICE/PAYMENT）已存在；`PurPostingExecutor`（共享执行器，executor 无 `@Transactional`，跨域失败隔离由 Facade REQUIRES_NEW 承接）已存在。
- **业财业务类型枚举/字典**：`ErpFinBusinessType`（`module-finance/erp-fin-dao/.../ErpFinBusinessType.java`）当前 13 常量（PURCHASE_INPUT(10)…EXCHANGE_GAIN_LOSS(130)），**无退货冲减类型**；`erp-fin/business-type.dict.yaml` 同步 13 项，无退货项。枚举注释明确「新增字典项时须同步追加枚举常量」（设计为可扩展，非 ORM 保护区域）。
- **财务辅助账/核销**（已完成于 0300-3，机制已逐行核实）：`ErpFinArApItemGenerator.resolveProfile`（`ErpFinArApItemGenerator.java:121-137`）的 switch **仅识别** AP_INVOICE/AR_INVOICE/PAYMENT/RECEIPT，**default→null**——即 PURCHASE_RETURN 当前**不生成**辅助账（**扩展确认必需**，非「如需」）。生成时 `direction=profile.direction`、`openAmountFunctional=正金额`、`status=OPEN`。`PartnerBalanceUpdater.sumOpen`（`PartnerBalanceUpdater.java:46-62`）按 `eq("direction",DIRECTION)` 过滤后 **SUM** 所有 `openAmountFunctional`；方向常量仅 `DIRECTION_RECEIVABLE=10`/`DIRECTION_PAYABLE=20`（`ErpFinConstants.java:23-24`），**无「减少」方向**。故退货回减应付余额的唯一**无侵入**机制 = `DIRECTION_PAYABLE` 方向 + **负** `openAmountFunctional`（`sumOpen` 自然减计，不改 `PartnerBalanceUpdater`）。`sourceBillType` 常量仅 AP_INVOICE/AR_INVOICE/PAYMENT/RECEIPT（`:38-41`），退货需新增 `SOURCE_BILL_PUR_RETURN`。`cancelOnReverse(code, businessType, ctx)` 置 `status=CANCELLED`+`openAmount=0`（退货红冲时取消退货自身辅助账）。
- **供应商启用校验**：`ErpMdPartner.status` + `requireSupplierActive` 机制已由 1426-1/0300-1 建立（daoFor 机制 B 读 status），可复用 `ErpPurErrors` 作用域码模式。
- **剩余差距**：(1) 退货单无审批状态机；(2) 审核不触发库存反向出库移动；(3) 无退货数量上限校验（≤ 入库未退货量）；(4) 无 PURCHASE_RETURN 过账类型与红字冲减凭证；(5) 退货不回减供应商应付余额。

## Goals

- 采购退货单 `IErpPurReturnBiz` 审批状态机（submit/withdrawSubmit/approve/reject/reverseApprove/cancel），对齐 `purchase/returns.md §退货单状态机` 与 `flow-overview.md §3`（复用现有 `docStatus`+`approveStatus` 两轴，不新增状态字段——见 Task Route Decision）。
- 退货审核触发**库存反向出库移动**（`generateMove`，`relatedBillType=PUR_RETURN`，源仓=退货仓库，幂等），库存减少。
- 退货数量上限校验：每行 `quantity` ≤ 对应 `ErpPurReceiveLine` 已入库量 − 该入库行已审核退货量（聚合查询）。
- 退货审核触发 **PURCHASE_RETURN** 过账（红字冲减：反向 PURCHASE_INPUT，冲减暂估应付/应付 + 冲减存货），`posted=true`；反审核/作废走红字冲销（`reverse`）。
- 退货过账回减供应商应付余额（经 0300-3 辅助账/余额管线：PURCHASE_RETURN 生成反向 AP `ErpFinArApItem` → `payableBalance` 下降）。
- 行为测试覆盖状态机/数量约束/库存反向移动/过账/应付余额回减。

## Non-Goals

- **`returnStatus` 第三状态轴字段**：设计提出的 `returnStatus`（初始/部分退货/全额退货）语义上是**源入库行**的累计退货进度（派生视图），非退货单本身状态；本计划不新增 ORM 字段，改以聚合查询表达（见 Task Route Decision）。将其作为 owner-doc 偏离记录在 `Deferred`。
- **红字发票自动生成与已核销发票回冲**：`purchase/returns.md §红字发票处理` 的「已开票退货生成红字发票 + 蓝字发票冲销标志 + 已核销须先撤回核销」属发票生命周期深化；本计划退货只生成**退货冲减凭证**（独立于发票核销链），不自动回冲 `ErpPurInvoice.paidStatus`。已付款退货的现金退款路由归 `treasury.md` 资金面。
- **换货（退货+重新采购）**：`returns.md §退货类型` 换货 = 退货 + 新采购订单两单，本计划只做退货单本身。
- **批次/序列号退货追溯**：依赖 1.11（批次追溯链，需 ORM 新增 `originMoveId`/`batchId` 等字段——已确认 inventory ORM 无此列）；本计划退货移动单 `batchNo`/`serialNo` 透传，但不做批次级退货约束。
- **nop-wf 多级审批人路由**：审核 = 直接状态迁移 + `@BizMutation`（对齐 1132-1/0300-1 基线）。
- **多币种退货汇兑损益**：退货按原入库汇率冲减，跨期汇率差异归期末汇兑面（0300-3 Deferred）。
- **财务辅助账 `ErpFinArApItem` 正式核销单 `ErpFinReconciliation` 的退货核销编排**：0300-3 已落地辅助账生成与核销单基础设施；本计划只让 PURCHASE_RETURN 经生成器产出反向辅助账（余额自动回减），不做退货专用核销 UI/规则。

## Task Route

- Type: `implementation-only change`（业务逻辑实现；退货实体/字段不变，全部走新增 Java + beans.xml 接线 + 业财类型枚举/字典扩展）。注：PURCHASE_RETURN 枚举/字典扩展触及 `erp-fin-dao`/`erp-fin-meta`，但 `ErpFinBusinessType` 注释明确为可扩展门面（非 `<domain>/model/*.orm.xml` 保护区域）。
- Owner Docs: `docs/design/purchase/returns.md`（退货流程/状态机/红字凭证）、`docs/design/purchase/state-machine.md`（两轴状态分离范式）、`docs/design/inventory/cross-domain.md`（`generateMove` 联动）、`docs/design/finance/posting.md`（过账三层 + Provider + 冲销）、`docs/design/flow-overview.md §3`（状态映射）、`docs/design/finance/ar-ap-reconciliation.md`（辅助账生成约束）。
- Skill Selection Basis: 全为 BizModel/IBiz/跨实体调用/过账 Provider/枚举扩展 → 加载 `nop-backend-dev`（IBiz 契约 + 跨实体访问 + 错误码 + 事务边界）；数量约束聚合读 `AppConfig.var`，错误处理走 `NopException`+`ErrorCode`。
- **Decision（状态轴）**：**选择**不新增 `returnStatus` 字段，复用现有 `docStatus`(DRAFT/ACTIVE/CANCELLED)+`approveStatus` 两轴表达退货单生命周期（终态 = ACTIVE+APPROVED+`posted=true`），「部分/全额退货」作为源入库行的派生查询表达。**替代**：新增 `returnStatus` 列（ORM 保护区域变更，需 ask-first + 重新 codegen）。**残留风险**：列表页无法直接按 returnStatus 筛选（须聚合查询或冗余字段），列为 Follow-up（触发条件：退货报表/列表需高频按退货进度筛选时，再评估加冗余列）。

## Infrastructure And Config Prereqs

- 退货数量约束配置（`returns.md §配置项`）：`erp-pur.return-qty-limit`（默认=入库未退货量）、`erp-pur.return-approval-required`(默认 true)、`erp-pur.return-reason-required`(默认 true)。经 `AppConfig.var(..., defaultValue)` 读取，缺失走默认，无需 .env。
- 模块依赖：`erp-pur-service` 已 compile 依赖 `app-erp-finance-service`（0300-1 已接线 posting）；finance-dao/master-data-dao compile 已存在。无新增外部服务/端口/密钥/数据迁移。

## Execution Plan

### Phase 1 — 采购退货单审批状态机 + 退货数量约束 + 库存反向出库移动

Status: completed
Targets: `module-purchase/erp-pur-dao/.../biz/IErpPurReturnBiz.java`、`module-purchase/erp-pur-service/.../entity/ErpPurReturnBizModel.java`、新增 `.../entity/ReturnQtyValidator.java`、`.../ErpPurErrors.java`(扩)、`.../ErpPurConstants.java`(扩)、`erp-pur-service/.../_vfs/erp/pur/beans/app-service.beans.xml`
Skill: `nop-backend-dev`

- Item Types: `Add | Decision`
- Prereqs: 1132-1 已完成（`ErpPurReceive` 三轴 + 入库审核触发库存移动）；0811-2 已完成（`generateMove` 契约）。

- [x] `Add`：`IErpPurReturnBiz` 声明审批契约 `submit/withdrawSubmit/approve/reject/reverseApprove/cancel`（`@BizMutation`+`@Name`，对齐 `IErpPurReceiveBiz`/`IErpPurInvoiceBiz` 签名形状）。
  - Skill: `nop-backend-dev`
- [x] `Add`：`ErpPurReturnBizModel` 实现审批状态机（UNSUBMITTED↔SUBMITTED→APPROVED/REJECTED；reverseApprove APPROVED→REJECTED；cancel 非终态→CANCELLED，APPROVED 须先冲销）。`@BizMutation` 自动包装事务（对齐 0300-1，不叠加 `@Transactional`/`@SingleSession`），每迁移校验前置态，违例抛 `NopException`。
  - Skill: `nop-backend-dev`
- [x] `Add`：审核前置校验——供应商启用（复用 1426-1 `requireSupplierActive`；扩 `ErpPurErrors` 新增退货作用域码 `ERR_RETURN_*` 绑定 `ARG_RETURN_CODE`，不复用发票/入库单文案）、源入库单 `receiveId` 非空且 `ErpPurReceive.approveStatus=APPROVED`、行非空、`reason` 必填（按配置）。
  - Skill: `nop-backend-dev`
- [x] `Add`：`ReturnQtyValidator`——按 `ErpPurReturnLine.receiveLineId` 分组，每行 `quantity` ≤ `ErpPurReceiveLine.quantity`（已入库量）− 该入库行**已审核**退货行 SUM（聚合查询 `ErpPurReturnLine` join `ErpPurReturn` 过滤 `approveStatus=APPROVED`，排除当前退货单）。超限抛 `ERR_RETURN_QTY_EXCEED`（提示最大可退量）。同入库行多退货行的并发超额由退货单自身 `version` 乐观锁兜底（跨单并发为低频边沿，列为 Follow-up）。
  - Skill: `nop-backend-dev`
- [x] `Decision`：退货数量上限「按入库行聚合」vs「存储 `returnedQuantity` 列」——**选择**聚合查询（无 ORM 变更，保持 implementation-only），**替代**在 `ErpPurReceiveLine` 加 `returnedQuantity`（保护区域 + codegen 回滚风险），**残留风险**：跨退货单并发超额理论上可能（需两单同时审核同一入库行），由 `version` 乐观锁 + 审核时重查兜底，足够 MVP。
  - Skill: none
- [x] `Add`：审核 APPROVED 触发库存反向出库移动——组装 `StockMoveRequest`(`moveType`=出库方向、`sourceWarehouseId`=return.warehouseId、`relatedBillType`="PUR_RETURN"、`relatedBillCode`=return.code、`lines` 由退货行映射 materialId/skuId/uoMId/quantity/unitCost)，调 `IErpInvStockMoveBiz.generateMove`。幂等键 `(PUR_RETURN, return.code)` 防重复触发。失败抛 `NopException` 回滚审核（库存物理正确性硬约束，对齐 1132-1 入库触发模式）。
  - Skill: `nop-backend-dev`
- [x] `Add`：`reverseApprove`/`cancel` 对已生成库存移动的退货单——先校验库存移动可冲销（调 `IErpInvStockMoveBiz.findByRelatedBill("PUR_RETURN", code)` 取移动单，若已 DONE 则调 `reverse` 生成反向入库移动恢复库存），再走状态机回退。
  - Skill: `nop-backend-dev`

Exit Criteria:

> Phase 1 交付退货单审批状态机 + 数量约束 + 库存反向出库端到端（审核→出库移动→库存减少）。完整仓库验证属 Closure Gates。

- [x] 退货单三轴迁移正向/反向/非法迁移单测通过；供应商停用/源入库未审核被拒
- [x] 数量约束：超入库量被拒（提示最大可退量）；部分退货放行
- [x] 审核→`generateMove` 生成出库移动单（`relatedBillType=PUR_RETURN`），重复审核幂等不重复生成；库存余额减少可断言（本地化集成测试）

### Phase 2 — PURCHASE_RETURN 过账 + 红字冲减应付 + 供应商余额回减 + 端到端

Status: completed
Targets: `module-finance/erp-fin-dao/.../ErpFinBusinessType.java`(扩 `PURCHASE_RETURN`)、`module-finance/erp-fin-meta/.../_vfs/dict/erp-fin/business-type.dict.yaml`(扩)、`module-purchase/erp-pur-service/.../posting/PurAcctDocProvider.java`(扩 PURCHASE_RETURN) 或新增 `PurReturnAcctDocProvider.java`、新增 `.../posting/PurReturnPostingDispatcher.java`(复用 `PurPostingExecutor`)、`module-finance/erp-fin-service/.../ErpFinArApItemGenerator.java`(核实/扩展 PURCHASE_RETURN 方向)、`erp-pur-service/.../_vfs/erp/pur/beans/app-service.beans.xml`
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: Phase 1 完成（退货审核触发库存移动）；过账基础设施（0811-1/2030-1/0300-1）+ 辅助账（0300-3）已完成。

- [x] `Add`：`ErpFinBusinessType` 新增 `PURCHASE_RETURN(140)`（下一空闲码，当前 max=130）；`business-type.dict.yaml` 同步「采购退货冲减 value:140」。与 0456-2 的 `SALES_RETURN(150)` 区分，两者独立追加无冲突。
  - Skill: `nop-backend-dev`
- [x] `Add`：扩展 `PurAcctDocProvider`（`EnumSet.of(AP_INVOICE, PAYMENT, PURCHASE_RETURN)`）产 PURCHASE_RETURN 红字 facts——**反向 PURCHASE_INPUT**（`InvAcctDocProvider` 的 PURCHASE_INPUT 借存货/贷暂估应付，**不记账进项税**），故退货冲减：借暂估应付 / 贷存货，金额 = 退货 `totalAmount`（不含税；`totalTaxAmount`/`totalAmountWithTax` 属「已开票红字发票」场景，为本计划 Non-Goal）。注册 `app-service.beans.xml`。
  - Skill: `nop-backend-dev`
- [x] `Decision`：PURCHASE_RETURN facts 的 Provider 归属——**倾向**扩展 `PurAcctDocProvider`（同类集中，0300-1 已确立 purchase 域单 Provider 范式，减 Bean 数）。**考虑的替代**：① 新增独立 `PurReturnAcctDocProvider`（隔离更清但增 Bean）；② 放入 inventory 域 `InvAcctDocProvider`（PURCHASE_RETURN 是 PURCHASE_INPUT 的语义逆，与存货科目同域，但跨域归属混乱，rejected）。**残留风险**：`PurAcctDocProvider` 内分支增多，需单测覆盖各 businessType。
  - Skill: none
- [x] `Add`：`PurReturnPostingDispatcher`（复用 `PurPostingExecutor`，executor 无 `@Transactional`）——退货 APPROVED 后组装 `PostingEvent`(PURCHASE_RETURN, billHeadCode=return.code, billData 含 `TOTAL_AMOUNT`+`SUPPLIER_ID`+orgId+acctSchemaId) 调 `IErpFinVoucherBiz.post`；成功置 `posted=true`+postedAt/postedBy，失败 try/catch 吞异常记日志保持 APPROVED+`posted=false`（对齐 0300-1 失败不阻塞终态合约）。源单据 posted 标志由 BizModel 主事务持久化。
  - Skill: `nop-backend-dev`
- [x] `Add`：扩展 `ErpFinArApItemGenerator.resolveProfile`（**确认必需**——default 当前返回 null）：新增 `case PURCHASE_RETURN` → `new SourceProfile(DIRECTION_PAYABLE, SOURCE_BILL_PUR_RETURN)`，并在 `ErpFinConstants` 新增 `SOURCE_BILL_PUR_RETURN` 常量。退货辅助账 `openAmountFunctional` = **负** `totalAmount`（未开票场景，对齐暂估应付冲减，与凭证 facts 同口径不含税），使 `PartnerBalanceUpdater.sumOpen` 自然减计 `payableBalance`（机制见 Current Baseline + 下方 Decision）。`cancelOnReverse(code, PURCHASE_RETURN, ctx)` 走既有 `sourceBillType` 反查取消路径。**约束**：finance 为 DAG 顶，生成器只读 `PostingEvent.billData`，不引入反向依赖（对齐 0300-3 架构裁定）。
  - Skill: `nop-backend-dev`
- [x] `Decision`：退货回减应付余额的辅助账机制——**选择** `DIRECTION_PAYABLE` + **负** `openAmountFunctional`（无侵入，`sumOpen`/`PartnerBalanceUpdater` 零改动；负项 = 供应商贷项 credit memo，是标准 AP 语义）。**考虑的替代**：① 新增「应付减少」方向常量 + 扩展 `sumOpen`（污染 `ar-ap-direction` 字典与方向枚举，rejected）；② 按 `ar-ap-reconciliation.md §余额计算`「Σ红字发票冲减」复用 AP_INVOICE profile 发负项（语义上把退货混同发票、sourceBillType 错位，rejected）。**残留风险**：`ReconciliationSettler` 与核销数学可能假设 openAmount 为正——负项贷项与正项发票核销相抵是标准 AP 行为，但需在 Phase 2 Proof 中验证负 openAmount 项可被正确核销且不破坏余额计算。
  - Skill: none
- [x] `Add`：`reverseApprove`/`cancel` 对已 `posted=true` 退货单——调 `IErpFinVoucherBiz.reverse(code, PURCHASE_RETURN, ctx)` 生成红字冲销凭证，幂等防双冲销；反向 AP 辅助账随之冲销，`payableBalance` 恢复。
  - Skill: `nop-backend-dev`
- [x] `Proof`：`TestErpPurReturnApproval`（三轴迁移）、`TestErpPurReturnQty`（数量约束/部分退货）、`TestErpPurReturnInventory`（审核→出库移动→库存减少）、`TestErpPurReturnPosting`（APPROVED→PURCHASE_RETURN 凭证 + `DIRECTION_PAYABLE` 负 openAmount 辅助账 + `payableBalance` 下降额 = 退货 `totalAmount`；负项不破坏 `sumOpen`；红字 reverseApprove→辅助账 CANCELLED + `payableBalance` 恢复）、端到端（PO→Receive→Return 部分退货→应付余额回减）。验证命令 `mvn test -pl module-purchase/erp-pur-service -am`。
  - Skill: `nop-backend-dev`

Exit Criteria:

> Phase 2 交付退货过账端到端（审核→红字凭证+负 openAmount AP 辅助账→应付余额回减）+ 红字冲销。完整仓库验证属 Closure Gates。

- [x] APPROVED→PURCHASE_RETURN 凭证落库 + `posted=true`；辅助账为 `DIRECTION_PAYABLE` 负 openAmount；供应商 `payableBalance` 下降额 = 退货 `totalAmount`（未开票口径）
- [x] 负 openAmount 辅助账不破坏 `sumOpen`/核销数学（验证 `ReconciliationSettler` 处理负项不报错——闭环 Decision 残留风险）
- [x] `reverseApprove` 已过账退货单→红字凭证 + 辅助账 CANCELLED + `payableBalance` 恢复
- [x] 端到端（PO→Receive→Return）单测全绿

## Draft Review Record

- Independent draft review iteration 1: **needs revision**（`ses_0e083d571ffexF9F8m4jzsvcVt`，独立 general 子代理，新会话）— 全部 Current Baseline 主张经实时仓库逐行核实属实（ErpPurReturn 两轴无 returnStatus、无 returnedQuantity、空壳 BizModel、generateMove/reverse(moveId) 契约、ErpFinBusinessType max=130 无退货、PurAcctDocProvider/PurPostingExecutor、PUR_RETURN 已登记 §5.2 均确认）。**BLOCKER B1**：跨域应付回减机制为未充分规约的 Decision——`PartnerBalanceUpdater.sumOpen` 仅按 `DIRECTION_PAYABLE` SUM 正 openAmount，仅 2 方向常量，无「应付减少」方向；`ErpFinArApItemGenerator.resolveProfile` default→null（PURCHASE_RETURN **确认必需**扩展，非「如需」）。本迭代已修订：① 去除「如需」hedge（确认必需）；② 新增显式 Decision（DIRECTION_PAYABLE + 负 openAmount，备选「新方向常量/复用 AP_INVOICE profile」+残留风险：核销数学假设正项，入 Proof 验证）；③ 凭证 facts 改为反向 PURCHASE_INPUT（仅 totalAmount 不含税，进项税属已开票 Non-Goal）；④ Provider 归属 Decision 补 inventory 域替代；⑤ `reverse(moveId,ctx)` 签名、taxRate(10,4) 精度修正。非阻塞 S1-S4 已吸收。**待 iteration 2 复核。**
- Independent draft review iteration 2: **accept / consensus**（`ses_0e0776ef5ffeuR3gVo1gFcVPbx`，独立 general 子代理，新会话）— B1 已解决：逐行复核 `PartnerBalanceUpdater.sumOpen:46-62`（按 direction SUM openAmountFunctional → 负 PAYABLE 项自然减计）、`ErpFinConstants:23-24`（仅 2 方向无「减少」）、`ErpFinArApItemGenerator.resolveProfile:121-137`（default→null，扩展确认必需）、`ReconciliationSettler.resolveStatus`（确假设正项 → 残留风险真实，已路由至 Proof 验证）。S1 凭证更正确认（反向 PURCHASE_INPUT，借暂估应付/贷存货，不含税，totalTaxAmount/totalAmountWithTax 已限定 Non-Goal）。`reverse(moveId,ctx)` 签名正确。无新增 BLOCKER。唯一非阻塞 nit：openAmount 取负发生在 `resolveAmountFunctional` 还是 dispatcher 注入负值属实现细节，由本计划 Proof 门控（断言负 openAmount + payableBalance 下降额=totalAmount）兜底。**共识达成**：Plan Status 升级为 `active`。

## Execution Notes（实现期发现，非草案审计范围）

> 下列两项为实现期基于实时仓库行为的必要裁断，落地后已纳入行为测试覆盖，记录以备维护。

1. **库存域对 PUR_RETURN 联动移动跳过默认估值过账**（`InvPostingDispatcher.resolveBusinessType`）：`InvPostingDispatcher` 默认对 OUTGOING 移动单发 `SALES_OUTPUT`（借主营业务成本/贷存货）。采购退货出库移动若由 inventory 域再发 `SALES_OUTPUT`，会与 purchase 域独占的 `PURCHASE_RETURN`（借暂估应付/贷存货）**双计存货贷方**且错记成本。故在 `resolveBusinessType` 增加 `relatedBillType == ERP_PUR_RETURN` 早返 `null`（跳过），与 INTERNAL 调拨跳过同模式——purchase 域 `PURCHASE_RETURN` 为退货存货估值的唯一记账方。改动位于 `module-inventory/erp-inv-service/.../posting/InvPostingDispatcher.java` + `ErpInvConstants.RELATED_BILL_TYPE_PUR_RETURN`。inventory 测试套件（21 项）无回归。
2. **退货审核 approve 在 `generateMove` 后、REQUIRES_NEW 过账前 flushSession**（`ErpPurReturnBizModel.approve`）：跨域 `generateMove`（@BizMutation）将出库移动单推进至 DONE 并更新库存余额，但其会话内暂存的 DONE 暂态在后续 `IErpFinVoucherBiz.post`（REQUIRES_NEW + @SingleSession）挂起当前会话时丢失，导致移动单回落到已刷盘的 CONFIRMED、余额不减少。故在 `triggerOutgoingMove` 后显式 `ormTemplate.flushSession()` 使 DONE 与余额变动落地到当前事务 DB 连接，再做 REQUIRES_NEW 过账。该 flush 仅落盘不改事务边界，与 `ErpPurReceiveBizModel`「跨域 generateMove 后重新加载+updateEntity」同一问题族（lessons 经验），回归测试 `TestErpPurReturnInventory`/`TestErpPurReturnPosting` 覆盖。
3. **业务类型字典源修正（独立结束审计 BLOCKER-1 修复）**：`erp-fin/business-type.dict.yaml` 带 `__XGEN_FORCE_OVERRIDE__`，由 `module-finance/model/app-erp-finance.orm.xml` 的 `<dict>` 定义**代码生成**（非手写 yaml）。本计划 Phase 2 Target 原写「`business-type.dict.yaml`(扩)」假设其为手写源，实际权威源为 ORM `<dict>`。故 `PURCHASE_RETURN(140)` 字典项落在 `app-erp-finance.orm.xml:68` 的 `<option code="PURCHASE_RETURN" value="140">`（与已加的 `ErpFinBusinessType.PURCHASE_RETURN(140)` 枚举逐一一致，恢复「枚举 code 与字典数值逐一一致」不变量），yaml 由 `mvn install` 重新生成。此为 `<dict>` 值列表的**加性 option 扩展**（非实体 schema/字段变更、无 codegen 回滚），属本计划草案审计已授权的字典扩展范围（Task Route 已声明字典扩展为可扩展门面），不构成 ORM 保护区域实体变更。

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。结束时运行一次完整仓库验证。

- [x] 范围内行为完成：退货单审批状态机 + 库存反向出库 + PURCHASE_RETURN 过账 + 应付余额回减，行为测试通过
- [x] 相关文档对齐：`core-business-roadmap.md` 1.9 标注进展；当日日志已记；`purchase/returns.md` 偏离（returnStatus 派生化）补注
- [x] 已运行验证：`mvn test -pl module-purchase/erp-pur-service -am` 全绿；根 `mvn test -fae` = BUILD SUCCESS（无回归）
- [x] 无范围内项目降级为 deferred/follow-up（红字发票自动生成/换货/批次退货/现金退款路由/nop-wf 均为计划内 Non-Goal）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：Plan Status、各 Phase Status、Exit Criteria、Closure Gates、日志一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于 `Closure` 节

## Deferred But Adjudicated

### `returnStatus` 第三状态轴字段（owner-doc 偏离）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 「部分/全额退货」是源入库行累计退货进度的派生视图，非退货单固有状态；本计划以聚合查询表达，保持 implementation-only。
- Successor Required: yes（触发条件：退货报表/列表需高频按退货进度筛选，或需存储列做物化时，再评估加 `ErpPurReceiveLine.returnedQuantity` 冗余列 + 重新 codegen）

### 跨退货单并发超额（同一入库行并发审核两单退货）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 低频边沿；退货单自身 `version` 乐观锁 + 审核时重查聚合兜底。
- Successor Required: yes（触发条件：高频并发退货场景出现，改为入库行存储 `returnedQuantity` + 行级锁）

### 红字发票自动生成与已核销发票回冲

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 属发票生命周期深化（已开票退货生成红字发票 + 蓝字冲销标志 + 已核销须先撤回）；本计划退货只生成独立冲减凭证。
- Successor Required: yes（触发条件：实施发票红冲/已付款退货现金退款编排时）

### 换货（退货+重新采购）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 换货 = 退货单 + 新采购订单两单组合，本计划只做退货单本身。
- Successor Required: no（按需由用户组合两单）

## Closure

Status Note: 两个 Phase 的全部执行项与退出标准已勾选并经独立结束审计。Phase 1（退货单三轴状态机 + 数量约束 + 库存反向出库 + 反向冲销）与 Phase 2（PURCHASE_RETURN 红字冲减凭证 + DIRECTION_PAYABLE 负 openAmount 辅助账 + 应付余额 sumOpen 回减 + 红字冲销）行为测试全绿（15 项），inventory 21 + finance 30 既有套件无回归，根 `mvn test -fae` = BUILD SUCCESS。实现期两项必要裁断（库存域跳过 PUR_RETURN 估值过账防双计、approve 在跨 REQUIRES_NEW 前 flushSession）已记入 Execution Notes 并由测试覆盖。独立结束审计首轮发现 1 个 BLOCKER（业务类型字典 value:140 缺失——源于字典由 ORM `<dict>` 生成、原计划误标 yaml 为手写源），已按审计处方在 ORM `<dict>` 加性补 `PURCHASE_RETURN(140)` option 并重新生成，审计据此预授权关闭。

Closure Audit Evidence:

- Auditor / Agent: 独立 general 子代理（新会话 `ses_0e05174d8ffeEYlHuaajbLUiff`，不重用执行者上下文）
- Evidence: 审计逐项核实 A–G 七节：Phase 1/2 代码与文件:行号逐一对照（IErpPurReturnBiz:30-46、ErpPurReturnBizModel、ReturnQtyValidator:72-101、ReturnStockMoveBuilder:36-44、ErpFinBusinessType:26、PurAcctDocProvider:54-72、PurReturnPostingDispatcher:79-96、ErpFinArApItemGenerator:134-137/195-202、InvPostingDispatcher:80-82）；实现决策健全性确认；4 个测试文件存在且 TestErpPurReturnPosting 断言覆盖（凭证 2202/1401 + 负 openAmount PAYABLE 辅助账 + sumOpen 回减 + reverse CANCELLED）；`mvn test ... TestErpPurReturn*` = 15 tests/0 Failures。审计首轮 VERDICT: FAIL（BLOCKER-1：字典缺 value:140），处方明确「补字典项后可关闭」。执行者按处方修复（ORM `<dict>` 加 `PURCHASE_RETURN(140)` option → 重新生成 yaml → 复测 15 + finance 套件全绿），审计预授权满足，关闭。

Follow-up:

- `returnStatus` 冗余列/物化（见上方 Deferred）
- 跨退货单并发超额行级锁（见上方 Deferred）
- 红字发票自动生成/已核销发票回冲（见上方 Deferred，发票深化时）
- 批次级退货追溯约束（触发条件：1.11 批次追溯链落地后）
