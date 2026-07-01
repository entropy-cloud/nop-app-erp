# 2026-07-01-1426-2 销售报价审批→转订单 + 销售订单审核状态机（含客户信用额度）

> Plan Status: completed
> Mission: erp
> Work Item: core-business-roadmap P1 / 1.0b（销售报价单审批→转订单逻辑）+ 1.2 遗留「销售订单审核状态机 + 客户信用额度校验」段
> Last Reviewed: 2026-07-01
> Source: `docs/backlog/core-business-roadmap.md` P1 工作项 1.0b；`docs/plans/2026-07-01-1132-2-sales-delivery-approval-inventory-trigger.md` 的 Deferred「销售订单（ErpSalOrder）审核状态机 + 客户信用额度校验」（显式后继，触发条件＝实施 1.0b 或订单信用控制时一并落地）
> Related: `docs/plans/2026-07-01-1132-2-sales-delivery-approval-inventory-trigger.md`（同域前驱：销售出库单审核状态机 + 出库触发，本计划复用其客户启用校验与状态机模式）、`docs/plans/2026-07-01-1426-1-purchase-requisition-to-order-and-order-approval.md`（同批 N=1，采购镜像项）、`docs/design/sales/quotation.md`（报价单生命周期权威源）、`docs/design/sales/state-machine.md`（三轴状态机，§2「销售订单｜仅状态推进」）、`docs/design/sales/README.md` §信用额度控制（三级策略 + 额度计算口径）、`docs/design/flow-overview.md` §2.2（订单信用额度检查）
> Audit: required

## Current Baseline

**项目阶段**（实时核实）：codegen 完成、CRUD 全绿（18 域 90 冒烟测试）。销售域出库单审核状态机 + 出库触发 `generateMove`（含可用量校验）已 `completed`（计划 1132-2，16 测试全绿）——`IErpSalDeliveryBiz`/`ErpSalDeliveryBizModel` 三轴审批状态机 + `DeliveryStockMoveBuilder` 已落地。`erp-sal-service` 已具备结构性依赖：`app-erp-inventory-dao`（compile）、`app-erp-master-data-dao`（compile）、`app-erp-inventory-service`/`app-erp-master-data-service`（test）。本计划**不新增跨域结构性依赖**（订单审核是纯状态推进 + 信用额度查询；转化逻辑在 sales 域内部）。

**目标 BizModel/I*Biz 现状**（实时核实，均为 `CrudBizModel<T>` 空壳，无自定义方法）：
- `module-sales/erp-sal-service/.../service/entity/ErpSalOrderBizModel extends CrudBizModel<ErpSalOrder> implements IErpSalOrderBiz`。
- `module-sales/erp-sal-service/.../service/entity/ErpSalQuotationBizModel extends CrudBizModel<ErpSalQuotation> implements IErpSalQuotationBiz`。
- `IErpSalOrderBiz extends ICrudBiz<ErpSalOrder>`、`IErpSalQuotationBiz extends ICrudBiz<ErpSalQuotation>`（空壳）。

**实体模型已就绪**（实时核实 `module-sales/model/app-erp-sales.orm.xml`）：
- `ErpSalOrder`（销售订单头）：`code`/`orgId`/`quotationId`(→ErpSalQuotation FK，**转化回链键**)/`contractId`/`customerId`(**mandatory**)/`warehouseId`/`businessDate`(**mandatory**)/`currencyId`(**mandatory**)/`exchangeRate`(DECIMAL 20,8)/金额族(`amountSource`/`amountFunctional`/`totalAmount`/`totalTaxAmount`/`totalAmountWithTax` 均 **DECIMAL 20,4**，规范无 VARCHAR 陷阱)/`discountRate`/`discountAmount`/`receivedAmount`/`settlementMethodId`/`docStatus`(int dict `erp-sal/doc-status`)/`approveStatus`(int dict `erp-sal/approve-status`)/`receivedStatus`/`deliveryStatus`/`posted`/`postedAt`/`postedBy`/`approvedBy`/`approvedAt`；关系 `customer`→ErpMdPartner、`warehouse`、`currency`、`lines`→ErpSalOrderLine。
- `ErpSalOrderLine`（订单行）：`orderId`/`lineNo`/`materialId`(**mandatory**)/`skuId`/`uoMId`(**mandatory**)/`quantity`(DECIMAL，**mandatory**)/`unitPrice`(DECIMAL，**mandatory**)/`taxRate`(DECIMAL)/`taxRateId`/`taxAmount`/`amount`(**mandatory**)/`amountWithTax`/`deliveredQuantity`/`invoicedQuantity`/`warehouseId`/`projectId`。
- `ErpSalQuotation`（报价单头，**转化源**）：`code`/`orgId`/`customerId`(**mandatory**)/`businessDate`(**mandatory**)/`validFrom`/`validTo`/`currencyId`(**mandatory**)/`exchangeRate`(DECIMAL 20,8)/`totalAmount`/`totalTaxAmount`/`totalAmountWithTax`(均 DECIMAL)/`isAccepted`(BOOLEAN，displayName「是否已转订单」)/`docStatus`/`approveStatus`/`remark`。**关键缺口**：报价单头**无 `approvedBy`/`approvedAt` 列**；`isAccepted` 是布尔标记（非独立「客户确认」状态轴）。
- `ErpSalQuotationLine`（报价行）：`quotationId`/`lineNo`/`materialId`(**mandatory**)/`uoMId`(**mandatory**)/`quantity`(DECIMAL)/`unitPrice`(DECIMAL，**mandatory**)/`taxRate`/`taxAmount`/`amount`/`amountWithTax`。

**字典权威值**（实时核实 `module-sales/erp-sal-meta/.../dict/erp-sal/`，与 1132-2 一致）：
- `approve-status`（int）：`UNSUBMITTED`=10 / `SUBMITTED`=20 / `APPROVED`=30 / `REJECTED`=40。
- `doc-status`（int）：`DRAFT`=10 / `ACTIVE`=20 / `CANCELLED`=30。
- `delivery-status`（int，派生）：`UNDELIVERED`=10 / `PARTIAL`=20 / `DELIVERED`=30。

**信用额度主数据已就绪**（实时核实）：`ErpMdPartner.creditLimit`（DECIMAL 20,4，`module-master-data/model/app-erp-master-data.orm.xml:299`）——客户信用额度字段存在，可经 `IErpMdPartnerBiz`/`ErpMdPartner` 读取（master-data-dao compile 依赖已在位）。

**state-machine.md 关键约束**（实时核实 `docs/design/sales/state-machine.md` §2 单据类型触发表）：
- 「销售订单 | 审核通过后触发：仅状态推进，不直接触发库存/凭证 | 同采购订单」——**订单审核 = 纯状态机 + 信用额度校验，无跨域库存调用**（与出库单审核触发 `generateMove`+可用量校验实质性不同）。
- 反审核目标态 = `REJECTED`（非 `UNSUBMITTED`，§3/domain-design-guidelines §16.4）。
- 客户停用后开单拒绝（§4，`ErpMdPartner.status` dict `erp-md/active-status`）。

**sales/README.md §信用额度控制 + flow-overview §2.2 关键约束**（实时核实）：
- 销售订单**审核时**检查客户信用额度，按 `erp-sal.credit-check-level`（默认 `SOFT_WARNING`）三级策略：`SOFT_WARNING`（超额度警告，允许继续）/`SPECIAL_APPROVAL`（超额度需额外审批人）/`HARD_BLOCK`（超额度直接拒绝）。
- **额度计算口径**：`客户信用额度 − 未结算应收余额（AR_INVOICE 未核销金额）− 未出库订单金额`。
- **关键依赖缺口**：销售发票（AR_INVOICE）尚未实现（1132-2 Deferred「销售发票/收款 Provider」），故「未结算应收余额」分量**当前不可得**——本计划 MVP 口径为 `creditLimit − 未出库订单金额`，「未结算应收」分量记为 Follow-up（触发条件＝销售发票 AR_INVOICE 落地时补入）。

**quotation.md 关键约束与设计-模型差异**（实时核实 `docs/design/sales/quotation.md`）：
- 设计状态机：DRAFT→SUBMITTED→APPROVED→ACCEPTED（客户确认）→转订单；EXPIRED（validTo 到期）；REJECTED/CANCELLED。
- **模型映射差异**（须裁决，见 Phase 2 Decision）：模型仅有 `docStatus`(doc-status)+`approveStatus`(approve-status)+`isAccepted`(布尔)；无独立「客户确认」状态轴、无 `approvedBy`/`approvedAt` 列、无 EXPIRED 持久化列。`ACCEPTED` 映射为 `isAccepted=true`；`EXPIRED` 由 `validTo < today` 派生（转化/确认时校验，不做后台扫描）。
- 转订单：`ACCEPTED` 后调用 `IErpSalOrderBiz` 创建 `ErpSalOrder`，报价行转为订单行。
- 业务规则 5：一次报价多次采购（分批转订单）——但 `isAccepted` 布尔只表达「已转订单」是/否，无法精确表达「部分已转」；此为模型边界（见 Phase 2 Decision）。

**模块依赖现状**（实时核实 pom.xml）：`erp-sal-service` 已含 inventory-dao/master-data-dao（compile）+ inventory-service/master-data-service（test）。master-data-dao 提供 `IErpMdPartnerBiz`/`ErpMdPartner`（客户启用 + `creditLimit` 读取）。**无新增依赖、无环**。

**剩余差距**：(1) ErpSalOrder 三轴审核状态机 + 客户信用额度校验落地；(2) ErpSalQuotation 审核/客户确认状态机落地（处理设计-模型差异）；(3) 报价→订单转化：APPROVED + 客户确认 → 创建 ErpSalOrder(UNSUBMITTED) + 行（携带定价/客户/币种），回链 `quotationId`，`isAccepted=true`，幂等防重复；(4) 服务层集成测试。

## Goals

- **销售订单审核状态机落地（含信用额度）**：`IErpSalOrderBiz` 增 `submit`/`approve`/`reject`/`cancel`/`reverseApprove`/`withdrawSubmit`；`ErpSalOrderBizModel` 实现三轴迁移（与出库单状态机同构）。`approve` 仅状态推进（**不触发库存/凭证**，对齐 state-machine §2）+ **客户启用校验** + **客户信用额度校验**：按 `erp-sal.credit-check-level`（`SOFT_WARNING`=超额度记录警告但放行 / `HARD_BLOCK`=超额度抛 `NopException(ERR_CREDIT_LIMIT_EXCEEDED)`）；额度口径 = `ErpMdPartner.creditLimit − 未出库订单金额`（未出库订单金额 = 该客户 `approveStatus=APPROVED AND deliveryStatus≠DELIVERED AND docStatus≠CANCELLED` 的 `ErpSalOrder.totalAmountWithTax` 之和）。
- **报价单审核/客户确认状态机落地**：`IErpSalQuotationBiz` 增 `submit`/`approve`/`reject`/`cancel`/`reverseApprove`/`withdrawSubmit`/`confirmCustomerAccepted`；`ErpSalQuotationBizModel` 实现审核迁移 + `confirmCustomerAccepted`（前置 `approveStatus=APPROVED` 且未过期 `validTo ≥ today`，置 `isAccepted=true`）。处理设计-模型差异（无 `approvedBy`/`approvedAt` 列——审核仅翻转 `approveStatus`，不记录审核人/时间，记为模型边界）。
- **报价→订单转化落地**：`IErpSalQuotationBiz.convertToOrder(quotationId)`（APPROVED + `isAccepted`）→ 创建 `ErpSalOrder`(approveStatus=UNSUBMITTED, docStatus=DRAFT) + 行（复制 `materialId`/`uoMId`/`quantity`/`unitPrice`/`taxRate`/`amount` 族，DECIMAL 直读直写；**`skuId` 不复制**——`ErpSalQuotationLine` 无该列，订单行 `skuId` 留空），头复制 `customerId`/`currencyId`/`exchangeRate`/金额族，`businessDate`=today，回链 `order.quotationId`。
- **转化校验与幂等**：(a) 仅 APPROVED + `isAccepted` 可转化（否则 `ERR_QUOTATION_NOT_READY`）；(b) 幂等：若已存在 `docStatus≠CANCELLED` 且 `quotationId=该报价` 的订单，抛 `ERR_QUOTATION_ALREADY_CONVERTED`。
- **服务层集成测试证明**：订单审核 happy/非法迁移/客户停用拒绝/信用超额度 SOFT_WARNING 放行 + HARD_BLOCK 拒绝、报价审核/客户确认、报价→订单转化（行/字段/回链正确）、重复转化拒绝、未确认报价转化拒绝，全部可重复通过。

## Non-Goals

- **不修改任何 `model/*.orm.xml` / `.api.xml`**（保护区域）——复用已生成实体（含 `ErpSalOrder.quotationId`/`ErpSalQuotation.isAccepted` 既有列）；不补 `approvedBy`/`approvedAt`/EXPIRED 列（模型边界，记为已知缺口，非 ORM 变更）。
- **不实现报价过期后台扫描**（nop-job 每日扫描置 EXPIRED）——`EXPIRED` 仅在确认/转化时由 `validTo < today` 派生校验；后台作业为 Follow-up（触发条件：接线 nop-job 时）。
- **不实现报价版本管理**（`isCurrentVersion`/历史版本）——quotation.md 业务规则 3，独立关注点。
- **不实现 CRM 集成**（线索/商机→报价单创建）——属 CRM 域（`crm/README.md`），报价单由 CRM 转化而来时通过弱指针反查。
- **不实现销售发票/收款/核销**——属 1.7；信用额度口径的「未结算应收余额」分量因此当前为 0（Follow-up）。
- **不实现 SPECIAL_APPROVAL 信用模式**——该模式需 nop-wf 额外审批人路由；MVP 只支持 SOFT_WARNING + HARD_BLOCK，SPECIAL_APPROVAL 为 Follow-up（触发条件：接线 nop-wf 时）。
- **不接入 nop-wf 审批流**——审核 = 直接状态迁移（对齐 1132-2/StockMove 基线）。
- **不支持一次报价分批多次转订单**——`isAccepted` 布尔只表达「已转订单」是/否（quotation.md 业务规则 5 的部分转化），MVP 视为单次转化；分批转化为 Follow-up（触发条件：出现一报价分批采购场景且补模型支持时）。
- **不做订单审核后的下游触发**（订单→出库已在 1132-2 完成；订单审核本身是意向确认 + 信用控制，不触发库存/凭证）。

## Task Route

- Type: `implementation-only change`（greenfield BizModel 方法 + 域内转化，不改公共 API 契约或 ORM；不改跨域调用边界——订单审核无库存调用，信用额度查询经既有 master-data-dao，转化在 sales 域内部）。
- Owner Docs: `docs/design/sales/state-machine.md`（三轴状态机，§2「销售订单｜仅状态推进」）、`docs/design/sales/quotation.md`（报价生命周期）、`docs/design/sales/README.md` §信用额度控制（三级策略 + 额度计算口径）、`docs/design/flow-overview.md` §2.2（订单信用额度检查）、`docs/architecture/testing-strategy.md`、平台 `../nop-entropy/docs-for-ai/02-core-guides/service-layer.md`。
- Skill Selection Basis: `Skill: none`（实施）。`docs/skills/README.md` 无 BizModel 编写技能匹配；遵循平台 service-layer 与 state-machine/quotation/README(信用额度) 文档。独立草案审查用 `docs/skills/plan-audit-prompt.md`；状态机正确性用 `docs/skills/state-machine-business-review-prompt.md` 复核（见 Phase 1 Proof）；结束审计用 `docs/skills/closure-audit-prompt.md`。
- 范围与 1.2 关系（规则 14）：本计划合并工作项 1.0b（报价→转订单）与 1.2 遗留的「订单审核状态机 + 信用额度」——两者同属 sales 域、共享 `ErpSalOrder` 实体与 `state-machine.md` owner doc，且 1132-2 的 Deferred 显式声明「实施 1.0b 或订单信用控制时一并落地」。同组件、同 owner doc、转化产物（订单）立即进入审核状态机 → 同一结果表面（销售订单从创建到审核的前端循环），合为一个计划的阶段，不拆分。

## Infrastructure And Config Prereqs

- 无新增基础设施。H2 内存库（`erp-sal-app` 已含 `quarkus-jdbc-h2`；`@NopTestConfig(localDb=true, initDatabaseSchema=TRUE)`）。
- **无新增结构性依赖**（实时核实无环）：`erp-sal-service` 已含 master-data-dao（compile，客户启用 + `creditLimit`）+ master-data-service（test）。订单审核/转化均在 sales 域内部，无需 inventory/finance 依赖。
- **配置项**：`erp-sal.credit-check-level`（`SOFT_WARNING`/`HARD_BLOCK`，默认 `SOFT_WARNING`）——经 `AppConfig.var(...)` 读取（复用 1132-2 的 `erp-inv.allow-negative-stock` 同款 `AppConfig.var` 读取模式）；按客户覆盖为 Follow-up。
- 无数据迁移/回滚脚本需求（greenfield BizModel）。

## Execution Plan

### Phase 1 - 销售订单审核状态机（三轴）+ 客户启用校验 + 信用额度校验

Status: completed
Targets: `module-sales/erp-sal-dao/src/main/java/app/erp/sal/biz/IErpSalOrderBiz.java`（增签名）、`module-sales/erp-sal-service/src/main/java/app/erp/sal/service/entity/ErpSalOrderBizModel.java`（实现）、`module-sales/erp-sal-service/src/main/java/app/erp/sal/service/.../CreditLimitChecker.java`（信用额度计算）、`module-sales/erp-sal-service/src/main/java/app/erp/sal/service/ErpSalErrors.java`（错误码，复用 1132-2 既有 `ERR_ILLEGAL_STATUS_TRANSITION`/`ERR_PARTNER_INACTIVE`，新增 `ERR_CREDIT_LIMIT_EXCEEDED`）、`module-sales/erp-sal-service/src/test/.../`
Skill: none

- Item Types: `Decision | Add | Proof`
- Prereqs: 无

- [x] `Decision`：订单状态迁移拓扑。裁决：与出库单（1132-2）同构三轴——提交 UNSUBMITTED(10)→SUBMITTED(20)（前置 docStatus≠CANCELLED、行非空）；审核通过 SUBMITTED→APPROVED(30)（前置客户启用 + 信用额度校验，**纯状态推进无库存/凭证触发**，对齐 state-machine §2「销售订单｜仅状态推进」）；驳回/重提/撤销提交/反审核（APPROVED→REJECTED）/作废。每条迁移校验前置，违反抛 `NopException(ERR_ORDER_ILLEGAL_STATUS_TRANSITION)`。备选（被否）：单状态机——三轴分离避免组合爆炸。残留风险：撤销提交在无 nop-wf 时未防审核人介入（对齐基线，Follow-up）。
  - Skill: none
- [x] `Decision`：信用额度口径与 MVP 分量。裁决：`outstanding = Σ(totalAmountWithTax) of ErpSalOrder where customerId=该客户 AND approveStatus=APPROVED AND deliveryStatus≠DELIVERED(30) AND docStatus≠CANCELLED(30)`；`available = ErpMdPartner.creditLimit − outstanding`；若 `available < 本单 totalAmountWithTax` 判定超额度。「未结算应收余额（AR_INVOICE 未核销）」分量当前为 0（销售发票未实现），MVP 不计入；Follow-up 在 AR_INVOICE 落地后补入。备选（被否）：等 AR_INVOICE 落地再做信用控制——阻塞订单审核核心能力，且 SOFT_WARNING（默认）不阻塞业务。残留风险：MVP 口径**低估**客户风险敞口（缺应收分量），故默认 SOFT_WARNING（仅警告放行）；HARD_BLOCK 模式下须运营知悉口径不含应收。
  - Skill: none
- [x] `Decision`：信用额度三级策略处理。裁决：读 `AppConfig.var("erp-sal.credit-check-level")`（默认 `SOFT_WARNING`）；`SOFT_WARNING`=超额度记录告警日志（SLF4J）但放行审核；`HARD_BLOCK`=超额度抛 `NopException(ERR_CREDIT_LIMIT_EXCEEDED)`；`SPECIAL_APPROVAL`=MVP 降级为 SOFT_WARNING 行为并记 WARN（nop-wf 未接线，无法路由额外审批人），记为 Follow-up。`creditLimit` 为 null（未设置）视为不控制（放行）。备选（被否）：SPECIAL_APPROVAL 抛异常阻断——无 nop-wf 时无法满足，阻断业务。残留风险：SPECIAL_APPROVAL 降级为软警告，运营须配置 HARD_BLOCK 才能硬拦截。
  - Skill: none
- [x] `Decision`：客户启用校验时机。裁决：订单 `submit` 与 `approve` 均校验 `ErpMdPartner.status`（dict `erp-md/active-status`）启用值，停用抛 `ERR_PARTNER_INACTIVE`（state-machine §4）。备选（被否）：仅 submit 校验——approve 距 submit 有时差。
  - Skill: none
- [x] `Add`：`IErpSalOrderBiz` 增 6 方法签名；`ErpSalOrderBizModel` 实现迁移（校验前置、落地 `approvedBy`/`approvedAt`(`CoreMetrics`)）；`approve` 内调 `CreditLimitChecker.check(customerId, thisOrderAmount)`——超额度按上述 Decision 处理；订单 `reverseApprove`/`cancel` 仅状态迁移（**无库存冲销前置**——订单审核未触发库存，与出库单不同；已有下游出库单的订单作废影响属 1.7/1.10 范畴）。`CreditLimitChecker` 经 `@Inject` 注入（bean 注册于 `app-service.beans.xml`）。`ErpSalErrors` 新增订单作用域错误码 `ERR_ORDER_*`（**S3 已落实**：不复用出库单作用域 `ERR_ILLEGAL_STATUS_TRANSITION` 绑定 `ARG_DELIVERY_CODE` 的文案，改为订单作用域 `ERR_ORDER_ILLEGAL_STATUS_TRANSITION` 绑定 `ARG_ORDER_CODE`）+ `ERR_CREDIT_LIMIT_EXCEEDED`。
  - Skill: none
- [x] `Proof`：服务层集成测试（`@NopTestConfig(localDb=true, initDatabaseSchema=TRUE, enableActionAuth=FALSE)` + master-data test 依赖，`createPrereqs()` 自建客户/物料/单位，设置客户 `creditLimit`，复用 1132-2 测试 setup 模式）——`testOrderSubmitApproveRejectResubmit`、`testOrderIllegalTransitionRejected`、`testOrderInactiveCustomerRejected`、`testCreditLimitSoftWarningAllows`（`credit-check-level=SOFT_WARNING` + 超额度 → 放行 + 告警）、`testCreditLimitHardBlockRejects`（`HARD_BLOCK` + 超额度 → `ERR_CREDIT_LIMIT_EXCEEDED`）、`testCreditLimitNullNoCheck`（`creditLimit=null` → 不控制）、`testOutstandingIncludesApprovedUndeliveredOrders`（已审核未出库订单占用额度，发货后释放）。`mvn test -pl module-sales/erp-sal-service -am` 全绿（7 测试 0 Failures/0 Errors）。
  - Skill: none
- [x] `Proof`：审批状态机正确性复核——用 `docs/skills/state-machine-business-review-prompt.md` 针对终态/可达性/反审核目标态(REJECTED)/订单审核纯状态无下游/信用额度异常路径自检，结论记录于本阶段（须执行）。
  - Skill: state-machine-business-review-prompt

  > **复核结论（Verdict: pass，无 P0/P1）**：实现与 `sales/state-machine.md` §2「销售订单｜仅状态推进」全部 7 条迁移一致；终态 APPROVED（审核轴）/CANCELLED（单据轴）正确；反审核目标态为 REJECTED（非 UNSUBMITTED，对齐 §3/domain-design-guidelines §16.4）；可达性无死状态/死锁，合法循环 UNSUBMITTED→SUBMITTED→REJECTED→SUBMITTED 退出条件为审核通过→APPROVED；非法迁移与客户停用均抛 `NopException`（已测试）；订单审核**纯状态推进、不触发库存/凭证**（approve 无 generateMove 调用，与出库单实质性差异），对齐 §2。信用额度异常路径：SOFT_WARNING 放行、HARD_BLOCK 抛 `ERR_CREDIT_LIMIT_EXCEEDED`、creditLimit=null 不控制，均经测试证明；outstanding 口径（`approveStatus=APPROVED AND deliveryStatus≠DELIVERED AND docStatus≠CANCELLED`）经 `testOutstandingIncludesApprovedUndeliveredOrders` 证明（含发货后释放）。剩余风险（非阻塞）：(a) `withdrawSubmit` 在无 nop-wf 时未防审核人已介入（对齐基线，Follow-up）；(b) SPECIAL_APPROVAL 降级为软警告（Follow-up）；(c) 信用口径缺 AR 应收分量（Follow-up）；(d) 角色职责分离未在代码强制（无 nop-wf，Follow-up）。

Exit Criteria:

> 本阶段交付订单审核状态机 + 客户启用 + 信用额度校验。完整仓库 `mvn test` 归 Closure Gates。

- [x] 7 个订单审核行为测试存在且 `mvn test -pl module-sales/erp-sal-service -am` 全绿
- [x] 信用额度 SOFT_WARNING 放行 + HARD_BLOCK 拒绝 + outstanding 口径均经测试证明（解除 Phase 2 转化「目标订单可被审核」的状态前置阻塞）

### Phase 2 - 报价单审核/客户确认状态机 + 报价→订单转化 + 幂等 + 回链

Status: completed
Targets: `module-sales/erp-sal-dao/src/main/java/app/erp/sal/biz/IErpSalQuotationBiz.java`（增签名）、`module-sales/erp-sal-service/src/main/java/app/erp/sal/service/entity/ErpSalQuotationBizModel.java`（实现）、`module-sales/erp-sal-service/src/main/java/app/erp/sal/service/.../QuotationToOrderConverter.java`（域内转化组装器）、`module-sales/erp-sal-service/src/test/.../`
Skill: none

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1（订单状态机就绪，订单可处于 UNSUBMITTED 接收转化产物）

- [x] `Decision`：报价单设计-模型差异映射。裁决：模型仅 `docStatus`+`approveStatus`+`isAccepted`——审核迁移用 `approveStatus`（UNSUBMITTED→SUBMITTED→APPROVED/REJECTED，同构三轴）；「客户确认 ACCEPTED」= 调 `confirmCustomerAccepted(quotationId)` 置 `isAccepted=true`（前置 `approveStatus=APPROVED` 且 `validTo ≥ today`，过期抛 `ERR_QUOTATION_EXPIRED`）；「EXPIRED」不在持久化（无列），由 `validTo < today` 在确认/转化时派生校验（无后台扫描）。**无 `approvedBy`/`approvedAt` 列**——审核仅翻转 `approveStatus`，不记录审核人/时间（模型边界，记为已知缺口，不改 ORM）。备选（被否）：补 ORM 列——保护区域，且 approveStatus 翻转已足够表达审核结果。残留风险：报价单无审核人审计轨迹（运营需知悉，相对订单/出库单的 `approvedBy` 是已知不对称）。
  - Skill: none
- [x] `Decision`：转化方法归属与触发前置。裁决：`IErpSalQuotationBiz.convertToOrder(quotationId)` 返回 `ErpSalOrder`——转化是报价单的生命周期动作；前置 `approveStatus=APPROVED AND isAccepted=true`（否则 `ERR_QUOTATION_NOT_READY`）；过期的报价（`validTo < today`）也拒绝（`ERR_QUOTATION_EXPIRED`）。备选（被否）：`IErpSalOrderBiz.createFromQuotation(...)`——把报价状态前置校验拆到订单侧，跨实体职责混乱。
  - Skill: none
- [x] `Decision`：幂等防重复转化 + 一次报价多次采购边界。裁决：转化前查询 `ErpSalOrder` where `quotationId=该报价 AND docStatus≠CANCELLED`——存在则抛 `ERR_QUOTATION_ALREADY_CONVERTED`；转化成功置 `quotation.isAccepted=true`（**纯查询+标记，无 ORM FK 改动**；`quotationId` 列已存在于订单）。原订单作废（CANCELLED）后允许重新转化。**一次报价分批多次转订单**（quotation.md 业务规则 5）：MVP 不支持——`isAccepted` 布尔无法表达部分转化，且幂等键按 quotationId 阻止重复；记为 Follow-up。备选（被否）：用金额/数量标记部分转化——需补模型列，超保护区域。残留风险：分批采购场景需先作废再重转或未来补模型。
  - Skill: none
- [x] `Add`：`IErpSalQuotationBiz` 增 `submit`/`approve`/`reject`/`cancel`/`reverseApprove`/`withdrawSubmit`/`confirmCustomerAccepted`/`convertToOrder`；`ErpSalQuotationBizModel` 实现审核迁移 + `confirmCustomerAccepted`（置 `isAccepted=true`）+ `convertToOrder`——`QuotationToOrderConverter` 组装 `ErpSalOrder`(UNSUBMITTED/DRAFT，回链 `quotationId`、复制 `customerId`/`currencyId`/`exchangeRate`/金额族、`businessDate`=today(`CoreMetrics.currentDate()`)) + 行（复制 `materialId`/`uoMId`/`quantity`/`unitPrice`/`taxRate`/`taxAmount`/`amount`/`amountWithTax`，DECIMAL 直读直写；`skuId` 不复制，见 Goals）；持久化订单 + 行；置 `isAccepted=true`。`ErpSalErrors` 新增报价转化作用域错误码 `ERR_QUOTATION_NOT_READY`/`ERR_QUOTATION_EXPIRED`/`ERR_QUOTATION_ALREADY_CONVERTED`（本阶段引用的错误码在此显式落地）。
  - Skill: none
- [x] `Proof`：`testQuotationSubmitApproveConfirmConvert`（DRAFT→SUBMITTED→APPROVED→`confirmCustomerAccepted`(`isAccepted=true`)→`convertToOrder` 生成订单 UNSUBMITTED + 行/字段/`quotationId` 回链正确）、`testConvertNotReadyRejected`（未 APPROVED 或未 `isAccepted` → `ERR_QUOTATION_NOT_READY`）、`testConvertExpiredRejected`（`validTo < today` → `ERR_QUOTATION_EXPIRED`）、`testConvertIdempotentRejected`（已转化 → `ERR_QUOTATION_ALREADY_CONVERTED`；作废原订单后可重转）、`testConvertedOrderThenCreditCheckAndApprove`（转化产物走 Phase 1 审核：SOFT_WARNING 放行 / HARD_BLOCK 超额度拒绝，证明两阶段衔接 + 信用控制作用于转化产物）。`mvn test -pl module-sales/erp-sal-service -am` 全绿。
  - Skill: none

Exit Criteria:

> 本阶段交付报价审核/客户确认 + 报价→订单转化 + 幂等 + 回链。完整仓库 `mvn test` 归 Closure Gates。

- [x] 5 个报价/转化行为测试存在且 `mvn test -pl module-sales/erp-sal-service -am` 全绿
- [x] 转化产物订单可被 Phase 1 审核状态机 + 信用额度校验推进（证明两阶段衔接）

### Phase 3 - 端到端串联 + 收尾

Status: completed
Targets: `module-sales/erp-sal-service/src/test/.../`、`docs/logs/2026/{执行当日 month-day}.md`、`docs/backlog/core-business-roadmap.md`
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 2

- [x] `Proof`：端到端测试 `testQuotationToOrderToEnd`（建报价→提交→审核 APPROVED→客户确认(`isAccepted`)→转化生成订单(UNSUBMITTED)→提交→审核（信用额度校验通过）APPROVED→（订单审核纯状态，不下游触发）→作废订单→可重新转化）。证明报价→订单前端循环打通 + 信用控制 + 与 Phase 1/2 衔接。`mvn test -pl module-sales/erp-sal-service -am` 全绿。
  - Skill: none
- [x] `Add`：更新当日开发日志 `docs/logs/2026/{执行当日 month-day}.md`（按 `docs/logs/00-log-writing-guide.md`，时间倒序），记录报价→订单转化 + 订单审核/信用额度落地 + 验证状态。
  - Skill: none
- [x] `Add`：`docs/backlog/core-business-roadmap.md` 工作项 1.0b 标注 `done`、1.2「订单审核状态机 + 信用额度」段标注 `done`（出库触发段已于 1132-2 done；发票/收款仍 todo）。
  - Skill: none

Exit Criteria:

> 本阶段交付端到端打通 + 文档对齐。完整仓库 `mvn test` 归 Closure Gates。

- [x] 端到端测试存在且 `mvn test -pl module-sales/erp-sal-service -am` 全绿
- [x] 当日日志已记；roadmap 工作项 1.0b / 1.2 订单审核+信用额度段进展已标注

## Draft Review Record

- Independent draft review iteration 1: **accept / consensus**（ses_0e39e65bdffeKAulohC4sCzMqp，独立 general 子代理，新会话）— 全部材料性主张经实时仓库核实属实：`ErpSalOrderBizModel`/`ErpSalQuotationBizModel` 空壳（仅构造器）、`IErpSal*Biz` 空 `ICrudBiz`；`ErpSalOrder.quotationId` FK + DECIMAL 金额族 + `approvedBy`/`approvedAt` 在位；**核心设计-模型差异主张核实为真**——`ErpSalQuotation` 仅有 `docStatus`+`approveStatus`+`isAccepted`，**无 `approvedBy`/`approvedAt` 列、无 EXPIRED/ACCEPTED 独立状态轴**（propId 1-22 无审核人列）；`ErpMdPartner.creditLimit` DECIMAL(20,4) 核实；dict 值（approve-status 10/20/30/40、doc-status 10/20/30、delivery-status 10/20/30，`.dict.yaml`）；模块依赖「无新增」核实；`state-machine.md:56`「销售订单｜仅状态推进」核实；`sales/README.md:83` 信用额度公式 + `credit-check-level` 默认 SOFT_WARNING 核实。**三处收窄均诚实三重记录**（AR 分量=0、SPECIAL_APPROVAL 降级、分批转化）——Non-Goal + Decision(备选/残留风险) + Deferred(命名触发条件)，满足规则 10 + 反松弛，非隐藏缺陷。规则 4/14 合并（1.0b + 订单审核 + 信用额度）经同域 + 共享 `ErpSalOrder` 实体/结果表面 + 1132-2 Deferred 定向后继证成。项目类型标注完整、Closure Gates 完整。无阻塞。非阻塞建议已吸收：S1 `ErpSalQuotationLine` 无 `skuId` 列 → Goals/Phase 2 Add 已从复制清单移除 `skuId`（订单行 `skuId` 留空，可空）；S2 AR 分量 Deferred 已补注可读既有缓存字段 `ErpMdPartner.receivableBalance`（核实存在）；S3 `ErpSalErrors.ERR_ILLEGAL_STATUS_TRANSITION` 消息绑定出库单参数（`ARG_DELIVERY_CODE`）→ Phase 1 Add 已增「错误码复用注意」要求泛化或新增 `ERR_ORDER_*`，Phase 2 Add 已显式列出报价转化作用域错误码 `ERR_QUOTATION_*`；S4 Phase 3 日志路径占位 `{月-日}` 已具化为 `{执行当日 month-day}`。**共识达成**：计划为可接受的执行契约，Plan Status 升级为 `active`。

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。结束时运行一次完整仓库验证。

- [x] 范围内行为完成：订单审核状态机 + 客户启用 + 信用额度（SOFT_WARNING/HARD_BLOCK）+ 报价审核/客户确认 + 报价→订单转化（幂等/回链）全部落地，行为测试通过
- [x] 相关文档对齐：`core-business-roadmap.md` 工作项 1.0b / 1.2 订单审核+信用额度段标注进展；当日日志已记
- [x] 已运行验证：`mvn test -pl module-sales/erp-sal-service -am` 全绿（29 测试）；根 `mvn test -fae` = BUILD SUCCESS（163 测试，无回归）
- [x] 无范围内项目降级为 deferred/follow-up（报价过期后台扫描/版本管理/CRM 集成/SPECIAL_APPROVAL/分批转化/发票收款/nop-wf 均为计划内 Non-Goal，非范围内降级）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：Plan Status、各 Phase Status、Exit Criteria、Closure Gates、日志一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于 `Closure` 节

## Deferred But Adjudicated

### 报价过期后台扫描（nop-job）

- Classification: `optimization candidate`
- Why Not Blocking Closure: `EXPIRED` 由 `validTo < today` 在确认/转化时派生校验；后台每日扫描为运营自动化。
- Successor Required: yes（触发条件：接线 nop-job 时）

### SPECIAL_APPROVAL 信用模式

- Classification: `optimization candidate`
- Why Not Blocking Closure: 该模式需 nop-wf 额外审批人路由；MVP 降级为 SOFT_WARNING 行为。
- Successor Required: yes（触发条件：接线 nop-wf 时）

### 信用额度口径「未结算应收余额」分量

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 销售发票（AR_INVOICE）未实现，分量不可得；MVP 口径为 `creditLimit − 未出库订单金额`，默认 SOFT_WARNING 不阻塞。
- Successor Required: yes（触发条件：销售发票 AR_INVOICE 落地时补入应收分量；可读取既有缓存字段 `ErpMdPartner.receivableBalance`（DECIMAL 20,4，`module-master-data/model/app-erp-master-data.orm.xml:301`）而非重算，待 AR_INVOICE 维护该缓存后接入）

### 一次报价分批多次转订单

- Classification: `optimization candidate`
- Why Not Blocking Closure: `isAccepted` 布尔无法表达部分转化；幂等键按 quotationId 阻止重复。
- Successor Required: yes（触发条件：出现分批采购场景且补模型支持时）

### 报价单版本管理

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: quotation.md 业务规则 3，独立关注点。
- Successor Required: yes（触发条件：实施报价版本管理时）

### nop-wf 审批流编排

- Classification: `optimization candidate`
- Why Not Blocking Closure: 审核 = 直接状态迁移（对齐 1132-2/StockMove 基线）。
- Successor Required: yes（触发条件：需要多级审批人路由与流程挂起时）

## Closure

Status Note: completed（2026-07-01，独立结束审计 `passes closure audit`，无 P0/P1）。

Closure Audit Evidence:

- **审计会话**：独立结束审计由子代理（新会话 `ses_0e1aa2a11ffeNGz3wIg1vvzWXM`，非实施者）于 2026-07-01 针对实时工作区执行。
- **Verdict: passes closure audit。**
- **关键核实点（对照真实文件 + 测试结果，非自报）**：
  1. 订单三轴状态机（`ErpSalOrderBizModel`）—— 迁移与 `sales/state-machine.md §2` 一致；反审核→REJECTED；`approve` 是**纯状态推进**（无 generateMove/无过账——与出库单审核实质不同），由端到端测试断言 `posted=false`/`deliveryStatus=UNDELIVERED` 证明。
  2. 信用额度：`CreditLimitChecker` 实现 null=不控制 / SOFT_WARNING=告警放行 / HARD_BLOCK=抛异常；outstanding = `customerId AND approveStatus=APPROVED AND deliveryStatus≠DELIVERED AND docStatus≠CANCELLED`；由 3 个专项测试证明（含发货后释放）。
  3. `convertToOrder`：APPROVED+isAccepted 前置（`ERR_QUOTATION_NOT_READY`）、过期（`ERR_QUOTATION_EXPIRED`）、按 `quotationId AND docStatus≠CANCELLED` 幂等（`ERR_QUOTATION_ALREADY_CONVERTED`）、作废后可重转；回链 `quotationId` 正确；字段复制正确；**skuId 正确不复制**（ORM 核实 `ErpSalQuotationLine` 无 skuId 列，orm.xml:142-179）。
  4. Non-Goal 遵守：`git diff --stat` 显示**零** `*.orm.xml`/`*.api.xml` 变更。
  5. 验证独立运行：`mvn test -pl module-sales/erp-sal-service -am` = **29 测试，0 Failures，0 Errors，BUILD SUCCESS**；根 `mvn test -fae` = **BUILD SUCCESS**（163 测试，无回归）。
  6. 文档对齐：roadmap 1.0b `done` + 1.2 订单审核/信用额度段 `done`；当日日志已记。
  7. 草案审查（iter1 accept）在案。
- **残留非阻塞风险（均已登记为 Deferred/Follow-up）**：(a) 信用额度口径「未结算应收余额」分量=0（AR_INVOICE 未实现）；(b) SPECIAL_APPROVAL 降级为 SOFT_WARNING（nop-wf 未接线）；(c) 报价单无 approvedBy/approvedAt 审计轨迹（刻意模型边界，不改 ORM）；(d) 不支持部分转化（isAccepted 布尔）；(e) 无后台 EXPIRED 扫描（nop-job）；(f) withdrawSubmit 在无 nop-wf 时未防审核人介入；(g)（已修复）日志测试计数笔误 28→29。

Follow-up:

- 报价过期后台扫描（见上方 Deferred）
- SPECIAL_APPROVAL 信用模式（见上方 Deferred）
- 信用额度口径「未结算应收余额」分量（见上方 Deferred）
- 一次报价分批多次转订单（见上方 Deferred）
- 报价单版本管理（见上方 Deferred）
- nop-wf 审批流编排（见上方 Deferred）
- 订单作废的下游出库单影响处理（触发条件：实施 1.7 销售到收款/1.10 销售退货时，需裁决已审核订单作废是否级联影响下游出库/发票）
