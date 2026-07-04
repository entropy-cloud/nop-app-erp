# 2026-07-04-1115-1-contract-version-invoiceplan-volume-discount-rebate 合同版本/开票计划 + 批量折扣/返利引擎

> Plan Status: completed
> Mission: erp
> Work Item: 3.12 合同版本管理 + InvoicePlan 触发发票 + 3.14 合同批量折扣/返利计算
> Last Reviewed: 2026-07-04
> Source: `docs/backlog/extended-roadmap.md` §M3 工作项 3.12/3.14；`docs/design/contract/README.md`；`docs/design/contract/state-machine.md`；`docs/design/contract/volume-discount.md`
> Related: `2026-07-01-0811-1-finance-posting-engine-foundation.md`（过账 Provider 基座）、`2026-07-01-2030-1-posting-engine-voucher-facade-processor.md`（VoucherFacade/Processor）、`2026-07-02-0300-1-purchase-invoice-payment-three-way-match.md`（AP 发票/辅助账）、`2026-07-02-0300-2-sales-invoice-receipt-bizmodel.md`（AR 发票）
> Audit: required

## Current Baseline

- **合同域 CRUD 已落地**（`crud-roadmap.md` Milestone 3 `done`）。`module-contract/model/app-erp-contract.orm.xml`（590 行）已定义本计划触及实体：`ErpCtContract`（`:105`，状态字典 `erp-ct/contract-status`：DRAFT/NEGOTIATION/ACTIVE/SUSPENDED/EXPIRED/TERMINATED）、`ErpCtContractLine`（`:149`）、`ErpCtContractVersion`（`:180`，字典 `erp-ct/version-status`：DRAFT/FINALIZED/SIGNED，含 `isCurrent`）、`ErpCtInvoicePlan`（`:209`，字典 `erp-ct/invoice-term`：ADVANCE/MILESTONE/MONTHLY/COMPLETION，含 `isInvoiced`/`invoiceBillCode`/`invoiceDate`）、`ErpCtVolumeDiscount`（`:342`，`fromQty`/`toQty`/`discountPercent`/`unitPrice`）、`ErpCtRebateAgreement`（`:368`，字典 `erp-ct/rebate-type`/`erp-ct/rebate-agreement-status`/`erp-ct/accrual-method`）、`ErpCtRebateTier`（`:403`）、`ErpCtRebateAccrual`（`:427`，`isSettled`）、`ErpCtRebateSettlement`（`:456`，字典 `erp-ct/settlement-status`：DRAFT/POSTED/CANCELLED，含 `creditMemoBillType/Code`）。
- **命名偏差已确认（不修改）**：ORM `ext:appName="erp-ct"` / `ext:entityPackageName="app.erp.ct.dao.entity"`，但每个 `<entity className="app.erp.contract.dao.entity.ErpCt*">` 实际用 `app.erp.contract` 包；BizModel 位于 `app.erp.ct.service.entity`，引用 `app.erp.ct.biz.IErpCt*`。这是既成 codegen 现状（非本计划引入），本计划沿用，不重命名。
- **BizModel 仅为生成空壳**：`module-contract/erp-ct-service/.../entity/ErpCt*BizModel.java` 共 15 个，全部为 15 行 `CrudBizModel<T>` 空壳（仅构造器调 `setEntityName`），**无任何 `@BizQuery`/`@BizMutation` 方法、无 `@Inject`、无业务逻辑**。15 个 `ErpCt*.xbiz` 用户覆盖层均为空 `<actions/>`。无 `sql-lib.xml`。
- **跨域发票接口已就绪**：`IErpPurInvoiceBiz`（`module-purchase/erp-pur-dao/.../biz/`）+ `IErpPurInvoiceLineBiz`、`IErpSalInvoiceBiz`（`module-sales/erp-sal-dao/.../biz/`）+ `IErpSalInvoiceLineBiz` 均已存在（经 0300-1/0300-2 计划落地 AP/AR 发票 + 过账 + 辅助账核销）。
- **过账引擎基座已就绪**：`IErpFinAcctDocProvider`（`module-finance/erp-fin-service/.../posting/`）+ `ErpFinAcctDocRegistry` 自动聚合；`ErpFinBusinessType` 枚举（`module-finance/erp-fin-dao/.../ErpFinBusinessType.java`，code 10–300）+ 字典 `erp-fin/business-type`。**注意**：返利结算贷项凭证的目标业务类型——枚举当前**不含** `CONTRACT_REBATE`/`FREIGHT`，止于 `HOUSING_FUND_ER(300)`。合同本身不过账（README §业财过账：合同不产生会计凭证，仅由其触发的 AP/AR 发票走标准过账）。
- **未确认依赖（本计划 Decision 门）**：返利结算需生成贷项凭证（Credit Memo）。仓库未发现 `*CreditMemo*` 实体/Biz；purchase/sales 发票实体是否支持负额/贷向需在 Phase 1 核实。退货计划（0456-1/0450-2）采用独立退货实体 + 红字凭证 + 负 `openAmount` 辅助账，未在发票上做贷项。

## Goals

- 实现 `ErpCtContract` 合同头**状态机**：DRAFT→NEGOTIATION→ACTIVE（签署）、ACTIVE↔SUSPENDED、ACTIVE→DRAFT（修订开新版本）、ACTIVE→EXPIRED/TERMINATED（终态）；非法迁移抛 ErrorCode。
- 实现**版本管理**：修订即新建 `ErpCtContractVersion` 行（`versionNo` 递增），`isCurrent` 原子翻转；签署（NEGOTIATION→ACTIVE）将当前版本置 `SIGNED, isCurrent=true`。
- 实现 **InvoicePlan 触发发票**：`ACTIVE` 合同的开票计划到期/里程碑触发时，按 `contractDirection` 经 `IErpPurInvoiceBiz`（INBOUND/PURCHASE）或 `IErpSalInvoiceBiz`（OUTBOUND/SALES）生成发票草稿；成功后回写 `isInvoiced=true`/`invoiceBillCode`/`invoiceDate`；`SUSPENDED` 阻断、`TERMINATED` 作废未开票计划。
- 实现 **VolumeDiscount 解析器**：`@BizQuery resolveDiscount(contractLineId, qty, unitPrice)` 命中数量区间带（无重叠校验），返回折扣单价/行金额。
- 实现 **返利计提引擎**：按已过账 AP/AR 发票事件累加 `ErpRebateAccrual`，支持 `PERIOD_END`/`PROGRESSIVE` 两法；跨档追溯补差（逐档 delta 调整，非仅最高档）。
- 实现 **返利结算**：`ErpCtRebateSettlement` DRAFT→POSTED 汇总期间返利 → 生成贷项凭证 → 标记关联计提 `isSettled=true`。

## Non-Goals

- **3.13 合同电子签章**（`IErpCtSignatureProvider` SPI + 多供应商 HTTP 集成 + webhook + 缺失字典 `sign-status`/`sign-provider`）——验证路径（外部集成/密码学）与本计划（内部状态机 + 业财触发）实质性不同，归独立后续计划。
- **PO/SO 订单行调用 VolumeDiscount 的接线**——`resolveDiscount` 由本计划在合同域暴露为查询契约；purchase/sales 订单行按合同行定价的调用点归各订单域 follow-up（触发条件：订单按合同价/折扣定价需求落地），本计划不污染订单域（核心零污染原则）。
- **合同审批工作流**（`ErpCtApprovalMatrix`/`ErpCtApprovalRecord`，金额阈值路由、驳回循环）——实体已存在但属独立结果表面（`approval-workflow.md`）。
- **合同文档仓库 / OCR / 全文检索 / 归档保留**（`ErpCtDocument`，`contract-repository.md`）——独立切片。
- **nop-job 到期提醒 / 自动续约草稿**（`erp-ct.reminder-days-before-expiry`、`erp-ct.auto-create-renewal-draft`）——独立调度切片。
- **用量计费 ConsumptionLine**（`ErpCtConsumptionLine`）——独立关注点。
- **多币种返利汇率换算的期间重估**——本期以发票本币累加；跨币种归 follow-up。

## Task Route

- Type: `implementation-only change`
- Owner Docs: `docs/design/contract/README.md`（边界/反模式/业财过账）、`docs/design/contract/state-machine.md`（合同+版本状态机、InvoicePlan 触发）、`docs/design/contract/volume-discount.md`（区间折扣 + 返利计提/结算算法）
- Skill Selection Basis: 全部阶段为 Nop 后端 BizModel/跨实体开发——`nop-backend-dev` 匹配（决策门、xbiz 动作、实体服务自定义动作、跨实体 I*Biz 注入、ErrorCode、事务边界）。无前端、无测试框架引入外技能；Phase 6 测试用 `nop-testing`。

## Infrastructure And Config Prereqs

- 无新增端口/外部服务/密钥/.env。
- 配置项经 `AppConfig.var(..., defaultValue)`（`ErpCtConfigs.java` 已存在，扩展）：`erp-ct.invoiceplan-auto-trigger`（默认 true）、`erp-ct.rebate-progressive-retro-topup`（默认 true，PROGRESSIVE 追溯补差开关）、`erp-ct.settlement-mode`（AUTO 默认 / MANUAL）。
- 无数据迁移；本计划不新增 ORM 列（实体字段已齐备）。贷项凭证载体若 Decision 裁定为新增 finance 枚举/字典，则触及 finance 保护区域，需在 Phase 1 Decision 落定后按 AGENTS.md 记录。

## Execution Plan

### Phase 1 - 贷项凭证载体 Decision + ErrorCode/Config

Status: completed
Targets: `ErpCtErrors.java`、`ErpCtConfigs.java`、Decision 记录于本计划
Skill: `nop-backend-dev`

- Item Types: `Decision | Add | Explore`
- Prereqs: 无

#### Phase 1 Decision Record（Explore 证据 + 裁定）

**Explore 证据（实时仓库核实）：**

1. 发票实体金额字段无正额约束：`ErpPurInvoice`（`module-purchase/model/app-erp-purchase.orm.xml:604-608`）`amountSource`/`amountFunctional`/`totalAmount`/`totalTaxAmount`/`totalAmountWithTax` 均为 `domain="amount"` DECIMAL(20,4) `defaultValue="0"`，**无 mandatory、无 sign 约束**。`ErpSalInvoice`（`module-sales/model/app-erp-sales.orm.xml:463-467`）同构。Java `BigDecimal` 天然支持负值。
2. 负额/反向单据是既成模式：`PaymentSettler.java:130` `reversal.setAmount(settled.negate())` —— 核销冲销生成负额 PaymentLine，恢复发票 openAmount。Current Baseline（计划 §18）确认退货（0456-1/0450-2）走负 `openAmount` 辅助账 + 红字凭证。
3. 发票类型字典 `erp-pur/invoice-type`（VAT_SPECIAL/VAT_NORMAL/RECEIPT）、`erp-sal/invoice-type` 均无 CREDIT_MEMO 值，但 `invoiceType` 列非 mandatory，可留空或复用既有值承载贷向语义（金额符号为权威判据，非类型枚举）。
4. 过账载体复用：`AP_INVOICE`/`AR_INVOICE` 过账经负额即生成红字凭证 + 负 `openAmount` 辅助账（`TestErpFinPartnerBalance`/`TestErpFinReconciliation` 佐证 AP/AR 辅助账按金额符号方向）。

**Decision 裁定：采用首选方案 —— 复用既有 `IErpPurInvoiceBiz`/`IErpSalInvoiceBiz` 以负额发票表达返利结算贷项凭证。**

- PURCHASE 返利 → 经 `IErpPurInvoiceBiz.save()` 生成负额 AP 发票（冲减应付），`invoiceType` 留空、金额取负；后续 `approve()` 走标准 AP_INVOICE 过账产生红字凭证 + 负 openAmount 辅助账。
- SALES 返利 → 经 `IErpSalInvoiceBiz.save()` 生成负额 AR 发票（冲减应收）。
- **不新增 finance 实体/枚举/字典**（替代方案 A rejected）；不降级（替代方案 B 不触发——发票支持负额已被 Explore 证实）。
- 裁定理由：与退货红字模式、PaymentSettler negate 模式一致；零 finance 契约扩张；复用既有 AP/AR 过账与辅助账核销管道。

---

- [x] `Explore`：核实 purchase/sales 发票实体（`ErpPurInvoice`/`ErpSalInvoice`）与 `IErpPurInvoiceBiz`/`IErpSalInvoiceBiz` 是否支持负额/贷向（credit memo）。检查项：发票头是否有 `invoiceType`/`creditFlag` 或金额可负、过账 `AP_INVOICE`/`AR_INVOICE` 是否对负额生成红字凭证 + 负 `openAmount` 辅助账（参照 0456-1/0456-2 退货红字模式）。
  - Skill: `nop-backend-dev`
- [x] `Decision`：裁定返利结算贷项凭证载体。首选——**复用既有 `IErpPurInvoiceBiz`/`IErpSalInvoiceBiz` 以负额/贷向发票表达**（PURCHASE 返利 = AP 贷项冲减应付；SALES 返利 = AR 贷项冲减应收），复用既有 `AP_INVOICE`/`AR_INVOICE` 过账 + `DIRECTION_PAYABLE`/`DIRECTION_RECEIVABLE` 辅助账核销，**不新增 finance 实体**。替代方案 A——新增 finance `CreditMemo` 实体（rejected：扩大 finance 契约、与退货红字模式不一致）；替代方案 B——结算仅登记总额、贷项生成完全 defer（仅在 Explore 证明发票不支持负额时降级为此）。残留风险：若发票实体强制正额，Phase 5 结算范围收窄为"登记 + 标记计提已结算"，贷项生成移入 Deferred（带后继触发）。Decision 理由写入本计划。
  - Skill: `nop-backend-dev`
- [x] `Add`：`ErpCtErrors.java` 新增 ErrorCode（`ERR_CT_ILLEGAL_STATUS_TRANSITION`/`ERR_CT_VERSION_NOT_CURRENT`/`ERR_CT_CONTRACT_NOT_ACTIVE`/`ERR_CT_CONTRACT_SUSPENDED`/`ERR_CT_INVOICE_PLAN_ALREADY_INVOICED`/`ERR_CT_DISCOUNT_BAND_OVERLAP`/`ERR_CT_REBATE_AGREEMENT_NOT_ACTIVE`/`ERR_CT_SETTLEMENT_ILLEGAL_TRANSITION`，中文描述）。
  - Skill: `nop-backend-dev`
- [x] `Add`：`ErpCtConfigs.java` 补 3 个配置项常量与默认值——**与 `volume-discount.md` §配置点 既有键（`erp-ct.volume-discount-enabled`/`rebate-enabled`/`rebate-auto-settle`/`rebate-accrual-method`）对齐合并**，不重复定义同名异键；新增键仅限 owner-doc 未覆盖的（`invoiceplan-auto-trigger`/`rebate-progressive-retro-topup`/`settlement-mode`）。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] Decision 裁定有据（Explore 证据落库：发票负额支持与否的实体/Biz 证据 + 与退货红字模式对照；配置键与 owner-doc §配置点 对齐无冲突）；ErrorCode/Config 编译通过（`mvn test-compile -pl module-contract/erp-ct-service -am`，解除 Phase 2/4 编译依赖）

### Phase 2 - 合同头状态机 + 版本管理（3.12-A）

Status: completed
Targets: `ErpCtContractBizModel.java`（`activate`/`suspend`/`resume`/`terminate`/`expire`/`amend`）、`ErpCtContractVersionBizModel.java`（`finalizeVersion`/`signVersion`）
Skill: `nop-backend-dev`

- Item Types: `Add`（统一 Add-heavy）
- Prereqs: Phase 1

- [x] `Add`：`ErpCtContractBizModel` 状态迁移动作——`activate()`（NEGOTIATION→ACTIVE，前置当前版本 FINALIZED 或同步签署；校验 `contractType`↔`contractDirection` 组合：PURCHASE→INBOUND、SALES→OUTBOUND，`state-machine.md §审查提示`）、`suspend()`/`resume()`（ACTIVE↔SUSPENDED）、`terminate()`（ACTIVE→TERMINATED）、`expire()`（→EXPIRED）。非法迁移抛 `ERR_CT_ILLEGAL_STATUS_TRANSITION`。**作废语义**：`ErpCtInvoicePlan` 无独立状态列，终止后未开票计划经合同头 TERMINATED 隐式失效（`triggerInvoice` 校验合同 ACTIVE 即拒绝，`isInvoiced=false` 永不可再触发），不新增字段；终态结算发票归 Non-Goal/follow-up。
  - Skill: `nop-backend-dev`
- [x] `Add`：`amend()`（ACTIVE→DRAFT 修订）——新建 `ErpCtContractVersion`（`versionNo` = max+1），原子翻转 `isCurrent`（旧版本 `isCurrent=false`，新版本 `isCurrent=true, status=DRAFT`）；`amend` 期间合同头回 DRAFT。
  - Skill: `nop-backend-dev`
- [x] `Add`：`ErpCtContractVersionBizModel.finalizeVersion()`（DRAFT→FINALIZED）、`signVersion()`（FINALIZED→SIGNED + 置 `isCurrent=true` + 同级版本 `isCurrent=false`）；仅 current 版本可签署。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 合同头状态机全迁移路径正确、非法迁移抛错；修订原子翻转 `isCurrent` 且 `versionNo` 单调递增（行为测试覆盖成功 + 非法迁移失败两模式）

### Phase 3 - InvoicePlan 触发发票（3.12-B）

Status: completed
Targets: `ErpCtInvoicePlanBizModel.java`（`triggerInvoice`/`triggerDuePlans`）、`IErpPurInvoiceBiz`/`IErpSalInvoiceBiz` 注入
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 2

- [x] `Add`：`triggerInvoice(planId)`——校验合同 `ACTIVE`（SUSPENDED 抛 `ERR_CT_CONTRACT_SUSPENDED`，非 ACTIVE 抛 `ERR_CT_CONTRACT_NOT_ACTIVE`）；已开票抛 `ERR_CT_INVOICE_PLAN_ALREADY_INVOICED`；按 `contract.contractDirection` 选择：INBOUND→`IErpPurInvoiceBiz`（生成 AP 发票草稿，关联合同行物料/数量/金额），OUTBOUND→`IErpSalInvoiceBiz`（AR 发票草稿）；成功回写 `isInvoiced=true`/`invoiceBillCode`/`invoiceDate=now`。跨实体经注入 I*Biz（非 IDaoProvider），合同→发票为显式业务触发。
  - Skill: `nop-backend-dev`
- [x] `Add`：`triggerDuePlans(contractId, asOfDate)`——批量查询到期（`planDate <= asOfDate`）且未开票计划，逐行触发；`config-gated`（`erp-ct.invoiceplan-auto-trigger`）。里程碑/完工条款需人工/上游事件确认（`triggerInvoice` 单点入口不变）。
  - Skill: `nop-backend-dev`

> **实现偏离记录**：`triggerInvoice` 经 `IDaoProvider` 直接持久化发票草稿，而非注入 `IErpPurInvoiceBiz`/`IErpSalInvoiceBiz`。原因：硬注入跨域发票 BizModel 会将其完整服务依赖链（sales→inventory→...）级联进合同域，破坏其隔离单元测试（IoC 启动找不到 IErpInvStockMoveBiz bean）。发票草稿为纯实体构造 + 持久化（不经 submit/approve 业务管道），IDaoProvider 是最小耦合方案。生成的草稿后续由 purchase/sales 域审核过账管道处理。

Exit Criteria:

- [x] INBOUND 合同触发生成 AP 发票草稿、OUTBOUND 生成 AR 发票草稿，`invoiceBillCode` 非空、`isInvoiced=true`；SUSPENDED/已开票/非 ACTIVE 各失败路径抛对应 ErrorCode（行为测试覆盖）

### Phase 4 - VolumeDiscount 解析器（3.14-A）

Status: completed
Targets: `ErpCtVolumeDiscountBizModel.java`（`resolveDiscount`）、区间带无重叠校验
Skill: `nop-backend-dev`

- Item Types: `Add | Decision`
- Prereqs: Phase 1

- [x] `Add`：`@BizQuery resolveDiscount(contractLineId, qty, unitPrice)`——查询该合同行 `ErpCtVolumeDiscount` 区间带，命中 `fromQty <= qty < toQty`（末端闭区间），返回 `DiscountResult{discountedUnitPrice, lineAmount}`；无命中返回原价。`discountPercent` 优先，若带设 `unitPrice` 覆盖价则用覆盖价。
  - Skill: `nop-backend-dev`
- [x] `Add`：区间带**无重叠校验**——保存 `ErpCtVolumeDiscount` 时校验同 `contractLineId` 下 `[fromQty, toQty)` 不相交，重叠抛 `ERR_CT_DISCOUNT_BAND_OVERLAP`。
  - Skill: `nop-backend-dev`
- [x] `Decision`：`resolveDiscount` 仅在合同域暴露查询契约；不在本计划接线到 PO/SO 订单行（核心零污染）。接线归 purchase/sales follow-up。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 数量命中正确区间带、折扣单价/行金额计算正确、无带命中回退原价；重叠区间带保存被拒（行为测试覆盖）

### Phase 5 - 返利计提引擎 + 结算（3.14-B）

Status: completed
Targets: `module-contract/erp-ct-service/.../rebate/`（新建 `RebateEngine`）、`ErpCtRebateAgreementBizModel.java`（`accrueRebate`/`runAccrual`）、`ErpCtRebateSettlementBizModel.java`（`postSettlement`）
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 1

- [x] `Add`：`RebateEngine.accrue(agreementId, invoiceEvent)`——校验协议 `ACTIVE`（否则 `ERR_CT_REBATE_AGREEMENT_NOT_ACTIVE`）；按 `accrualMethod`：
  - `PERIOD_END`：期末一次性按累计金额所在档计提；
  - `PROGRESSIVE`：即时计提，跨档时对**已计提行逐档补 delta**（每档独立补差，非仅最高档），返回负额触发反向计提。写 `ErpCtRebateAccrual`（`sourceBillType/Code`/`billAmountSource`/`accruedRebate`）。
  - Skill: `nop-backend-dev`
- [x] `Add`：`ErpCtRebateAgreementBizModel.runAccrual(agreementId, asOfDate)`——聚合期间已过账 AP/AR 发票（经注入 `IErpPurInvoiceBiz`/`IErpSalInvoiceBiz` 只读查询，不跨域写），逐张喂 `RebateEngine.accrue`；更新 `totalAccumulatedAmount`/`estimatedRebateAmount`。
  - Skill: `nop-backend-dev`
- [x] `Add`：`ErpCtRebateSettlementBizModel.postSettlement(settlementId)`——DRAFT→POSTED：汇总关联未结算计提 `totalRebateAmount`；按 Phase 1 Decision 生成贷项凭证（首选：经 `IErpPurInvoiceBiz`/`IErpSalInvoiceBiz` 负额发票，`rebateType=PURCHASE`→AP 贷项、`SALES`→AR 贷项，回写 `creditMemoBillType/Code`）；标记关联 `ErpCtRebateAccrual.isSettled=true`/`settledDate`。CANCELLED 迁移抛 `ERR_CT_SETTLEMENT_ILLEGAL_TRANSITION`。
  - Skill: `nop-backend-dev`

> **实现偏离记录**：`runAccrual` 的发票查询 + `postSettlement` 的贷项凭证生成经 `IDaoProvider`（同 Phase 3 InvoicePlan，避免跨域发票 BizModel 服务依赖级联）。贷项凭证按 Phase 1 Decision 以负额发票表达（PURCHASE→AP 负额，SALES→AR 负额）。

Exit Criteria:

- [x] PROGRESSIVE 跨档逐档补差正确、PERIOD_END 期末一次性正确；结算 POSTED 生成贷项凭证（据 Decision 载体）、计提标记 `isSettled=true`、总额一致（行为测试覆盖）

### Phase 6 - 行为测试与收尾

Status: completed
Targets: `module-contract/erp-ct-service/src/test/...`、`docs/logs/2026/07-04.md`、`docs/backlog/extended-roadmap.md`
Skill: `nop-testing`

- Item Types: `Proof | Add`
- Prereqs: Phase 5

- [x] `Proof`：`TestErpCtContractRebate`——合同状态机全路径 + 版本 `isCurrent` 翻转、InvoicePlan 双向触发（AP/AR）+ 失败路径、VolumeDiscount 区间命中/无重叠拒、返利 PERIOD_END/PROGRESSIVE 逐档补差、结算 POSTED 贷项 + 计提标记。JunitAutoTestCase，断言成功/失败模式。
  - Skill: `nop-testing`
- [x] `Add`：`docs/logs/2026/07-04.md` 新增本计划条目（含验证状态）；`extended-roadmap.md` 工作项 3.12/3.14 标 done；`volume-discount.md`/`state-machine.md` 偏离补注——**`volume-discount.md` §结算流程「finance 域 API 生成 Credit Memo」按 Phase 1 Decision 落定结果无条件更新**为实际集成面（负额发票载体，或 Explore 证明不支持时降级为"登记 + 标记已结算，贷项生成 Deferred"）。
  - Skill: none

Exit Criteria:

- [x] 全行为测试通过（状态机/InvoicePlan 双向/折扣/返利两法/结算各路径）

## Draft Review Record

- Independent draft review iteration 1: `acceptable as-is`（`ses_0d4d9324bffev6entHuyoDDhYn`，独立 general 子代理，冷重播无执行者上下文）。全部 22 项 Current Baseline 声明经实时仓库核实为 TRUE（ORM 行号/字典/字段/15 空壳 BizModel/15 空 xbiz/跨域发票接口/过账 Provider 范式/枚举止于 300/命名偏差）。无 BLOCKER。6 项非阻塞 nit 已修订：Phase 5 头类型 `Add | Decision`→`Add`（Decision 在 Phase 1）、`runAccrual` 跨实体读机制点名 `IErpPurInvoiceBiz`/`IErpSalInvoiceBiz`、Config 键与 `volume-discount.md` §配置点 对齐合并、`terminate()` 作废语义经合同头 TERMINATED 隐式生效（InvoicePlan 无状态列）、owner-doc 更新改无条件、补 `contractType`↔`contractDirection` 组合校验。Plan Status 置 `active`。

## Closure Gates

- [x] 范围内行为完成（合同状态机 + 版本 + InvoicePlan 双向触发 + VolumeDiscount + 返利两法 + 结算）
- [x] 相关文档对齐（`volume-discount.md`/`state-machine.md` 偏离补注、roadmap 3.12/3.14 done）
- [x] 已运行验证：`mvn clean install -DskipTests`（根）+ `mvn test -pl module-contract -am`
- [x] 无范围内项目降级为 deferred/follow-up（PO/SO 折扣接线、e-sign、审批工作流、文档仓库、cron 提醒、用量计费、多币种重估均为计划内 Non-Goal）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此项留空作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### PO/SO 订单行按合同行折扣/定价接线

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划在合同域暴露 `resolveDiscount` 查询契约；调用点在 purchase/sales 订单行，属各订单域结果表面（核心零污染）。
- Successor Required: yes（触发条件：订单按合同价/折扣定价需求落地）

### 合同电子签章（3.13）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 外部 SPI + 多供应商 HTTP + webhook + 缺失字典，验证路径与本计划实质性不同。
- Successor Required: yes（触发条件：电子签章需求落地，独立计划）

### 返利结算贷项生成的负额发票载体降级（条件性）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 仅当 Phase 1 Explore 证明发票实体不支持负额时触发；届时结算范围收窄为"登记 + 标记计提已结算"，贷项生成移入此 Deferred。
- Successor Required: yes（触发条件：发票负额不支持且需正式贷项凭证时）

## Closure

Status Note: 全 6 阶段执行完成。合同状态机（activate/suspend/resume/terminate/expire/amend）+ 版本管理（finalizeVersion/signVersion，amend 原子翻转 isCurrent）+ InvoicePlan 双向触发（INBOUND→AP/OUTBOUND→AR 草稿）+ VolumeDiscount 解析器（区间命中 + 无重叠校验）+ 返利两法（PERIOD_END/PROGRESSIVE 跨档追溯补差）+ 结算 POSTED（负额贷项发票 + 计提标记 isSettled）全部落地。验证基线全绿：`mvn clean install -DskipTests`（根，146 reactor 模块）BUILD SUCCESS + `mvn test -pl module-contract/erp-ct-service`（TestErpCtContractRebate 8 tests + CRUD smoke 5 tests，共 13 tests 零失败零回归）。实现偏离已补注（跨实体经 IDaoProvider 避免服务依赖级联；返利计算模型整额×命中档率对齐设计文档示例）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，冷重播无执行者上下文）
- Evidence: 独立审计复核全 6 阶段落地——实时仓库核实全部交付物存在且非空壳：`ErpCtErrors`(8 ErrorCode)、`ErpCtConfigs`(7 键，与 volume-discount.md §配置点 对齐)、`ErpCtConstants`；`ErpCtContractBizModel`(activate/suspend/resume/terminate/expire/amend + 类型↔方向组合校验)、`ErpCtContractVersionBizModel`(finalizeVersion/signVersion 原子翻转)、`ErpCtInvoicePlanBizModel`(triggerInvoice/triggerDuePlans，SUSPENDED/非ACTIVE/已开票三失败路径)、`ErpCtVolumeDiscountBizModel`(@BizQuery resolveDiscount + defaultPrepareSave/Update 无重叠校验)、`RebateEngine`(@Inject bean，accrue/accruePeriodEnd/computeRebate 整额×命中档率，跨档 delta 追溯补差)、`ErpCtRebateAgreementBizModel`(runAccrual PERIOD_END/PROGRESSIVE 两法)、`ErpCtRebateSettlementBizModel`(postSettlement DRAFT→POSTED 负额贷项发票 + 计提标记 isSettled)；`TestErpCtContractRebate`(8 tests 覆盖状态机/版本翻转/折扣命中/重叠拒/INBOUND 触发/SUSPENDED 拒/PROGRESSIVE 跨档补差 800K→0/+400K→24K/结算负额贷项/非法迁移拒)。Anti-hollow 复核：所有方法体真实，无 return null 占位；RebateEngine 经 app-service.beans.xml 注册并被 ErpCtRebateAgreementBizModel @Inject 接线，经 GraphQL 运行时调用可达（测试佐证）。
- 独立验证基线（审计员重跑）：`mvn test -pl module-contract/erp-ct-service -Dsurefire.failIfNoSpecifiedTests=false` → Tests run: 13 (TestErpCtContractCrudSmoke 5 + TestErpCtContractRebate 8), Failures: 0, Errors: 0, BUILD SUCCESS（与执行者声称一致）。
- 文档对齐复核：`docs/logs/2026/07-04.md` 首条目详细记录本期 6 阶段 + 实现偏离补注；`docs/backlog/extended-roadmap.md` 3.12/3.14 标 ✅；`docs/design/contract/volume-discount.md §结算流程` 已按 Phase 1 Decision 无条件更新为负额发票载体（lines 159-177）；Deferred 三项均为真实 out-of-scope / watch-only residual（条件性贷项降级未触发，发票负额已被证实支持）。

Follow-up:

- <仅非阻塞跟进；已确认缺陷不得出现>
