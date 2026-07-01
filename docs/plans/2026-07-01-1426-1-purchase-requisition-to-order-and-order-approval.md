# 2026-07-01-1426-1 采购请购审批→转订单 + 采购订单审核状态机

> Plan Status: completed
> Mission: erp
> Work Item: core-business-roadmap P1 / 1.0a（采购申请审批→转订单逻辑）+ 1.1 遗留「采购订单审核状态机」段
> Last Reviewed: 2026-07-01
> Source: `docs/backlog/core-business-roadmap.md` P1 工作项 1.0a；`docs/plans/2026-07-01-1132-1-purchase-receipt-approval-inventory-trigger.md` 的 Deferred「采购订单（ErpPurOrder）审核状态机」（显式后继，触发条件＝实施 1.0a 时一并落地）
> Related: `docs/plans/2026-07-01-1132-1-purchase-receipt-approval-inventory-trigger.md`（同域前驱：采购入库单审核状态机 + 入库触发，本计划复用其供应商启用校验与状态机模式）、`docs/plans/2026-07-01-1426-2-sales-quotation-to-order-and-order-approval.md`（同批 N=2，销售镜像项）、`docs/design/purchase/requisition.md`（请购→询价→转订单权威源）、`docs/design/purchase/state-machine.md`（三轴状态机权威源，§2 单据类型触发表「采购订单｜仅状态推进」）、`docs/design/purchase/README.md`
> Audit: required

## Current Baseline

**项目阶段**（实时核实）：codegen 完成、CRUD 全绿（18 域 90 冒烟测试）。采购域入库单审核状态机 + 入库触发 `generateMove` 已 `completed`（计划 1132-1，14 测试全绿）——`IErpPurReceiveBiz`/`ErpPurReceiveBizModel` 三轴审批状态机 + `ReceiveStockMoveBuilder` 已落地。`erp-pur-service` 已具备结构性依赖：`app-erp-inventory-dao`（compile）、`app-erp-master-data-dao`（compile）、`app-erp-inventory-service`/`app-erp-master-data-service`（test）。本计划**不新增跨域结构性依赖**（订单审核是纯状态推进，不触发库存/凭证；转化逻辑在 purchase 域内部）。

**目标 BizModel/I*Biz 现状**（实时核实，均为 `CrudBizModel<T>` 空壳，无自定义方法）：
- `module-purchase/erp-pur-service/.../service/entity/ErpPurOrderBizModel extends CrudBizModel<ErpPurOrder> implements IErpPurOrderBiz`。
- `module-purchase/erp-pur-service/.../service/entity/ErpPurRequisitionBizModel extends CrudBizModel<ErpPurRequisition> implements IErpPurRequisitionBiz`。
- `IErpPurOrderBiz extends ICrudBiz<ErpPurOrder>`、`IErpPurRequisitionBiz extends ICrudBiz<ErpPurRequisition>`（空壳）。

**实体模型已就绪**（实时核实 `module-purchase/model/app-erp-purchase.orm.xml`）：
- `ErpPurOrder`（采购订单头）：`code`/`orgId`/`requisitionId`(→ErpPurRequisition FK，**转化回链键**)/`quotationId`(→ErpPurQuotation FK，RFQ 路径用)/`supplierId`(**mandatory**)/`warehouseId`/`businessDate`(**mandatory**)/`currencyId`(**mandatory**)/`exchangeRate`(VARCHAR)/金额族(`amountSource`/`amountFunctional`/`totalAmount`/`totalTaxAmount`/`totalAmountWithTax` 均 **VARCHAR**)/`docStatus`(int dict `erp-pur/doc-status`)/`approveStatus`(int dict `erp-pur/approve-status`)/`paidStatus`/`receiveStatus`/`posted`/`postedAt`/`postedBy`/`approvedBy`/`approvedAt`；关系 `supplier`→ErpMdPartner、`warehouse`、`currency`、`lines`→ErpPurOrderLine。
- `ErpPurOrderLine`（订单行）：`orderId`/`lineNo`/`materialId`(**mandatory**)/`skuId`/`uoMId`(**mandatory**)/`quantity`(DECIMAL 20,4，**mandatory**)/`unitPrice`(VARCHAR，**mandatory**)/`taxRate`(VARCHAR)/`taxRateId`/`taxAmount`(VARCHAR)/`amount`(VARCHAR，**mandatory**)/`amountWithTax`(VARCHAR)/`receivedQuantity`/`invoicedQuantity`/`warehouseId`/`projectId`。
- `ErpPurRequisition`（请购单头，**转化源**）：`code`/`orgId`/`requesterId`(**mandatory**)/`departmentId`/`businessDate`(**mandatory**)/`requiredDate`/`docStatus`/`approveStatus`/`approvedBy`/`approvedAt`/`remark`。**关键缺口**：头级**无 `supplierId`/`warehouseId`/`currencyId`/金额族**（请购是数量型意向单）。
- `ErpPurRequisitionLine`（请购行）：`requisitionId`/`lineNo`/`materialId`(**mandatory**)/`uoMId`(**mandatory**)/`quantity`(DECIMAL 20,4，**mandatory**)/`requiredDate`/`suggestedSupplierId`(可选，**转化时供应商的唯一来源**)/`projectId`/`remark`。**关键缺口**：行级**无 `unitPrice`**（请购不携带价格）。

**字典权威值**（实时核实 `module-purchase/erp-pur-meta/.../dict/erp-pur/`，与 1132-1 一致）：
- `approve-status`（int）：`UNSUBMITTED`=10 / `SUBMITTED`=20 / `APPROVED`=30 / `REJECTED`=40。
- `doc-status`（int）：`DRAFT`=10 / `ACTIVE`=20 / `CANCELLED`=30。

**state-machine.md 关键约束**（实时核实 `docs/design/purchase/state-machine.md` §2 单据类型触发表）：
- 「采购订单 | 审核通过后触发：仅状态推进，不直接触发库存/凭证（订单是意向，下游单据才触发）」——**订单审核 = 纯状态机，无跨域调用**（与入库单审核触发 `generateMove` 实质性不同，故订单审核是可分离的轻量面）。
- 反审核目标态 = `REJECTED`（非 `UNSUBMITTED`，§3/§11.4）。
- 供应商停用后开单拒绝（§4，经 `IErpMdPartnerBiz` 校验 `ErpMdPartner.status` dict `erp-md/active-status`）。

**requisition.md 关键约束**（实时核实 `docs/design/purchase/requisition.md`）：
- 请购审批通过 → 可选「直接转订单」或「转询价」；本计划只覆盖「直接转订单」路径。
- 业务规则 1：APPROVED 后按采购策略决定；反模式警示：「请购直通订单跳过审批——请购与订单是不同实体，各有独立审批流，请购 APPROVED 后才创建订单」。
- 转订单：调用 `IErpPurOrderBiz` 创建采购订单，请购行转为订单行。

**模块依赖现状**（实时核实 pom.xml）：`erp-pur-service` 已含 inventory-dao/master-data-dao（compile）+ inventory-service/master-data-service（test）。master-data-dao 提供 `IErpMdPartnerBiz`/`ErpMdPartner`（供应商启用校验，订单审核复用）。**无新增依赖、无环**。

**剩余差距**：(1) ErpPurOrder 三轴审核状态机落地（纯状态推进）；(2) ErpPurRequisition 审核状态机落地（DRAFT→SUBMITTED→APPROVED/REJECTED/CANCELLED）；(3) 请购→订单转化逻辑：APPROVED 请购 + 调用方补充字段 → 创建 ErpPurOrder(UNSUBMITTED) + 行，回链 `requisitionId`，幂等防重复转化；(4) 服务层集成测试。

## Goals

- **采购订单审核状态机落地**：`IErpPurOrderBiz` 增 `submit`/`approve`/`reject`/`cancel`/`reverseApprove`/`withdrawSubmit` 方法签名；`ErpPurOrderBizModel` 实现三轴迁移（与入库单状态机同构：UNSUBMITTED→SUBMITTED→APPROVED/REJECTED、驳回→重提、APPROVED→REJECTED 反审核、作废 docStatus→CANCELLED）。`approve` 仅状态推进（**不触发库存/凭证**，对齐 state-machine §2）；`submit`/`approve` 校验供应商启用（停用抛 `NopException(ERR_PARTNER_INACTIVE)`）。`@SingleSession @Transactional`，`approvedBy`/`approvedAt`(`CoreMetrics`) 落地。
- **请购单审核状态机落地**：`IErpPurRequisitionBiz` 增 `submit`/`approve`/`reject`/`cancel`/`reverseApprove`/`withdrawSubmit`；`ErpPurRequisitionBizModel` 实现迁移（同构三轴），`approve` 仅推进（请购无下游单据自动触发，转化是显式独立动作）。
- **请购→订单转化落地**：`IErpPurRequisitionBiz` 增 `convertToOrder(requisitionId, ConvertToOrderRequest)`（或等效 `IErpPurOrderBiz.createFromRequisition(...)`）；APPROVED 请购 + 调用方补充 `supplierId`/`warehouseId`/`currencyId`/每行 `unitPrice` → 创建 `ErpPurOrder`(approveStatus=UNSUBMITTED, docStatus=DRAFT) + 行（复制 `materialId`/`skuId`/`uoMId`/`quantity`，写入 `unitPrice`/`amount` 族按 VARCHAR），回链 `order.requisitionId`，订单行 `orderId`/`lineNo` 落地。
- **转化校验与幂等**：(a) 仅 APPROVED 请购可转化（非 APPROVED 抛 `NopException(ERR_REQ_NOT_APPROVED)`）；(b) 幂等防重复：若已存在 `docStatus≠CANCELLED` 且 `requisitionId=该请购` 的订单，抛 `NopException(ERR_REQ_ALREADY_CONVERTED)`；(c) 供应商一致性约束（见 Decision）。
- **服务层集成测试证明**：订单审核 happy/非法迁移/供应商停用拒绝、请购审核迁移、请购→订单转化（行/字段/回链正确）、重复转化拒绝、未审批请购转化拒绝，全部可重复通过。

## Non-Goals

- **不修改任何 `model/*.orm.xml` / `.api.xml`**（保护区域）——复用已生成实体（含 `ErpPurOrder.requisitionId`/`ErpPurOrderLine` 既有列）；无 ORM 变更即无需 regen。
- **不实现 RFQ 询价比价流程**（`ErpPurRfq`/`ErpPurQuotation` 的发布/报价登记/中标/比价算法）——属 requisition.md 的「转询价」分支，独立的寻源策略面，显式后继。
- **不实现请购合并**（多部门同物料合并到一张 RFQ/订单）——requisition.md 业务规则 5，独立关注点。
- **不自动从供应商价格清单（`ErpPurSupplierPriceList`）解析单价**——转化 `unitPrice` 由调用方提供；价格清单自动化为 Follow-up（触发条件：实施供应商价格清单维护/自动带价时）。
- **不支持多供应商分组转化**——MVP 要求单请购单一供应商（见 Decision）；按 `suggestedSupplierId` 分组生成多张订单为 Follow-up（触发条件：出现一请购多供应商场景时）。
- **不实现采购订单审核后的下游触发**（订单→入库已在 1132-1 完成；订单审核本身是意向确认，不触发库存/凭证）。
- **不实现采购发票/付款/三单匹配**——属 1.4/1.6。
- **不接入 nop-wf 审批流**——审核 = 直接状态迁移 + `@BizMutation`（对齐 1132-1/StockMove 同步状态机基线）；nop-wf（多级审批人路由/挂起）为 Follow-up。
- **不做订单行级价税重算引擎**——转化时金额族按调用方提供的 `unitPrice`×`quantity` 计算（VARCHAR 存储，对齐采购域 VARCHAR 金额约定）；复杂折扣/税额分摊为独立规则。

## Task Route

- Type: `implementation-only change`（greenfield BizModel 方法 + 域内转化，不改公共 API 契约或 ORM；不改跨域调用边界——订单审核无跨域调用，转化在 purchase 域内部）。
- Owner Docs: `docs/design/purchase/state-machine.md`（三轴状态机，§2 单据类型触发表「采购订单｜仅状态推进」）、`docs/design/purchase/requisition.md`（请购→转订单）、`docs/architecture/testing-strategy.md`（测试 runbook + 跨域主数据依赖模式）、平台 `../nop-entropy/docs-for-ai/02-core-guides/service-layer.md`（CrudBizModel 扩展、I*Biz 跨域注入）。
- Skill Selection Basis: `Skill: none`（实施）。`docs/skills/README.md` 现有技能均为审计/审查方法，无 BizModel 编写技能匹配；实施遵循平台 service-layer 指南与 state-machine/requisition 文档。独立草案审查用 `docs/skills/plan-audit-prompt.md`；状态机正确性用 `docs/skills/state-machine-business-review-prompt.md` 复核（见 Phase 1 Proof）；结束审计用 `docs/skills/closure-audit-prompt.md`。
- 范围与 1.1 关系（规则 14）：本计划合并工作项 1.0a（请购→转订单）与 1.1 遗留的「订单审核状态机」——两者同属 purchase 域、共享 `ErpPurOrder` 实体与 purchase 域 owner doc 族（订单审核：`state-machine.md`，其 §适用对象覆盖订单；请购/转化：`requisition.md`），且 1132-1 的 Deferred 显式声明「实施 1.0a 时一并落地订单审核状态机」。同组件、转化产物（订单）立即进入审核状态机 → 同一结果表面（采购订单从创建到审核的前端循环），合为一个计划的阶段，不拆分。

## Infrastructure And Config Prereqs

- 无新增基础设施。H2 内存库（`erp-pur-app` 已含 `quarkus-jdbc-h2`；服务层测试 `@NopTestConfig(localDb=true, initDatabaseSchema=TRUE)`）。
- **无新增结构性依赖**（实时核实无环）：`erp-pur-service` 已含 master-data-dao（compile，供应商启用校验）+ master-data-service（test，跨域主数据测试数据）。订单审核/转化均在 purchase 域内部，无需 inventory/finance 依赖（订单审核不触发库存/凭证）。
- 无数据迁移/回滚脚本需求（greenfield BizModel，复用已存在实体）。

## Execution Plan

### Phase 1 - 采购订单 + 请购单审核状态机（三轴）+ 供应商启用校验

Status: completed
Targets: `module-purchase/erp-pur-dao/src/main/java/app/erp/pur/biz/IErpPurOrderBiz.java`、`IErpPurRequisitionBiz.java`（增方法签名）、`module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/entity/ErpPurOrderBizModel.java`、`ErpPurRequisitionBizModel.java`（实现）、`module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/ErpPurErrors.java`（错误码，复用 1132-1 既有 `ERR_ILLEGAL_STATUS_TRANSITION`/`ERR_PARTNER_INACTIVE`）、`module-purchase/erp-pur-service/src/test/.../`
Skill: none

- Item Types: `Decision | Add | Proof`
- Prereqs: 无

- [x] `Decision`：订单/请购状态迁移拓扑。裁决：与入库单（1132-1）同构三轴——提交 UNSUBMITTED(10)→SUBMITTED(20)（前置 docStatus≠CANCELLED、行非空）；审核通过 SUBMITTED→APPROVED(30)（订单：前置供应商启用；**纯状态推进，无库存/凭证触发**，对齐 state-machine §2「采购订单｜仅状态推进」）；驳回 SUBMITTED→REJECTED(40)；重提 REJECTED→SUBMITTED；撤销提交 SUBMITTED→UNSUBMITTED；反审核 APPROVED→REJECTED（目标态 REJECTED 非 UNSUBMITTED，§3/§11.4）；作废 任意非终态→docStatus=CANCELLED(30)。每条迁移校验前置，违反抛 `NopException(ERR_ILLEGAL_STATUS_TRANSITION)`。请购单 `approve` 仅推进（请购无自动下游触发，转化是显式独立动作）。备选（被否）：单状态机——三轴分离避免组合爆炸（state-machine §三轴分离）。残留风险：撤销提交在无 nop-wf 时未防「审核人已介入」（对齐基线，Follow-up）。
  - Skill: none
- [x] `Decision`：供应商启用校验时机。裁决：订单 `submit` 与 `approve` 均校验 `ErpMdPartner.status`（dict `erp-md/active-status`）为启用值——经 `IErpMdPartnerBiz`（或 `daoProvider` 加载 `ErpMdPartner`）按 `supplierId` 查，停用抛 `NopException(ERR_PARTNER_INACTIVE)`（state-machine §4）。请购单无头级供应商，不校验。备选（被否）：仅 submit 校验——approve 距 submit 有时差，供应商可能期间被停用。残留风险：依赖 master-data-dao compile 依赖（已在位）。
  - Skill: none
- [x] `Add`：`IErpPurOrderBiz` 增 `submit`/`approve`/`reject`/`cancel`/`reverseApprove`/`withdrawSubmit` 方法签名；`ErpPurOrderBizModel` 实现迁移（校验前置、落地 `approvedBy`(当前用户)/`approvedAt`(`CoreMetrics.currentDateTime()`）；订单 `reverseApprove`/`cancel` 仅状态迁移（**无库存冲销前置**——订单审核未触发库存，与入库单不同；若订单已有下游入库单，作废订单的下游影响属 1.4/1.6 范畴，本计划不处理）。`IErpPurRequisitionBiz` 同增 6 方法签名；`ErpPurRequisitionBizModel` 实现同构迁移。`@SingleSession @Transactional`，`IErpMdPartnerBiz` 经 `@Inject` 包级可见字段注入。`ErpPurErrors` 补充本计划新增错误码（`ERR_REQ_NOT_APPROVED` 等，见 Phase 2）。**错误码复用注意**（审查 S1）：1132-1 既有 `ERR_ILLEGAL_STATUS_TRANSITION`/`ERR_ILLEGAL_DOC_STATUS_TRANSITION` 的消息文案绑定入库单参数（如 `ARG_RECEIVE_CODE`/「入库单 {receiveCode}…」）；直接复用于订单/请购迁移会产出误导消息——实施时须泛化消息与参数名，或新增订单/请购作用域错误码（`ERR_ORDER_*`/`ERR_REQ_*`），不得照搬入库单文案。
  - Skill: none
- [x] `Proof`：服务层集成测试（`@NopTestConfig(localDb=true, initDatabaseSchema=TRUE, enableActionAuth=FALSE)` + master-data test 依赖，`createPrereqs()` 自建供应商/物料/单位，复用 1132-1 测试 setup 模式）——订单：`testOrderSubmitApproveRejectResubmit`、`testOrderIllegalTransitionRejected`（APPROVED→SUBMITTED 等非法迁移抛 `NopException`）、`testOrderInactiveSupplierRejected`、`testOrderCancelFromDraft`；请购：`testReqSubmitApproveRejectResubmit`、`testReqIllegalTransitionRejected`。`mvn test -pl module-purchase/erp-pur-service -am` 全绿。
  - Skill: none
- [x] `Proof`：审批状态机正确性复核——用 `docs/skills/state-machine-business-review-prompt.md` 针对终态/可达性/反审核目标态(REJECTED 非 UNSUBMITTED)/订单审核纯状态无下游/异常路径自检，结论记录于本阶段（须执行）。
  - Skill: state-machine-business-review-prompt

  > **复核结论（Verdict: pass，无 P0/P1）**：实现与 `state-machine.md` §2 全部迁移一致；订单审核 = 纯状态推进（无库存/凭证触发，对齐 §2「采购订单｜仅状态推进」），请购 approve 仅推进（请购无自动下游触发，转化是显式独立动作）；终态 APPROVED（审核轴）/CANCELLED（单据轴）正确；反审核目标态为 REJECTED（非 UNSUBMITTED，对齐 §3/§11.4）；可达性无死状态/死锁，合法循环 UNSUBMITTED→SUBMITTED→REJECTED→SUBMITTED 退出条件为审核通过→APPROVED；非法迁移、供应商停用（订单 submit/approve 双点）、已作废拒绝均抛 `NopException`（已测试 9 用例）。剩余风险（非阻塞）：(a) 无 nop-wf，角色职责分离未在代码强制、`withdrawSubmit` 未防审核人已介入（对齐基线，Follow-up）；(b) 订单 `reverseApprove`/`cancel` 不级联下游入库单（属 1.4/1.6，已记 Non-Goal）；(c) `approve` 在已 APPROVED 时幂等空操作（§4 重复审核=空操作，订单无副作用故安全）。

Exit Criteria:

> 本阶段交付订单+请购三轴状态机 + 供应商校验。完整仓库 `mvn test` 归 Closure Gates。

- [x] 订单 4 + 请购 2 状态机行为测试存在且 `mvn test -pl module-purchase/erp-pur-service -am` 全绿
- [x] 非法迁移拒绝 + 供应商停用拒绝均经测试证明（解除 Phase 2 转化「目标订单可被审核」的状态前置阻塞）

### Phase 2 - 请购→订单转化 + 幂等 + 回链

Status: completed
Targets: `module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/entity/ErpPurRequisitionBizModel.java`（convertToOrder）或 `ErpPurOrderBizModel.java`（createFromRequisition，裁决见 Decision）、`module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/.../RequisitionToOrderConverter.java`（域内转化组装器）、`module-purchase/erp-pur-service/src/test/.../`
Skill: none

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1（订单/请购状态机就绪，订单可处于 UNSUBMITTED 接收转化产物）

- [x] `Decision`：转化方法归属。裁决：`IErpPurRequisitionBiz.convertToOrder(requisitionId, ConvertToOrderRequest)` 返回 `ErpPurOrder`——转化是请购的生命周期动作（请购 APPROVED → 派生订单），入口在请购侧；内部组装 `ErpPurOrder` + 行后以 `daoProvider`/`ICrudBiz` 持久化。备选（被否）：`IErpPurOrderBiz.createFromRequisition(...)`——把请购状态前置校验拆到订单侧，跨实体职责混乱。残留风险：`ConvertToOrderRequest` DTO 需定义（purchase 域内部 DTO，不进 api 模块）。
  - Skill: none
- [x] `Decision`：供应商一致性约束（单请购单供应商）。裁决：MVP 要求请购所有行 `suggestedSupplierId` 非空且一致；不一致或缺失抛 `NopException(ERR_REQ_MIXED_OR_MISSING_SUPPLIER)`。一致供应商作为订单 `supplierId`。备选（被否）：按 `suggestedSupplierId` 分组生成多张订单——增加复杂度且与「一张请购一张订单」的幂等键冲突；分组转化为 Follow-up。残留风险：无 `suggestedSupplierId` 的请购无法转化（要求采购员先维护建议供应商）。
  - Skill: none
- [x] `Decision`：转化缺失字段补全。裁决：`ConvertToOrderRequest` 由调用方提供 `warehouseId`/`currencyId`/每行 `unitPrice`（按 `lineNo` 或 `materialId` 映射）；`businessDate`=`requisition.businessDate`、`orgId`=`requisition.orgId`。金额族（`unitPrice`×`quantity`=`amount`、`taxRate`/`taxAmount`/`amountWithTax`）由转化器计算，按 VARCHAR 写入（对齐采购域 VARCHAR 金额约定，解析须防护空/非法格式，失败抛 `NopException(ERR_INVALID_UNIT_PRICE)`）。订单创建为 `approveStatus=UNSUBMITTED, docStatus=DRAFT`，随后走 Phase 1 审核状态机。备选（被否）：自动从 `ErpPurSupplierPriceList` 带价——价格清单维护/匹配是独立寻源逻辑，且 requisition.md 明确供应商价格清单属 `supplier-evaluation.md` 边界；自动带价为 Follow-up。残留风险：调用方须提供完整价格/仓库/币种，否则转化拒绝（API 契约清晰）。
  - Skill: none
- [x] `Decision`：幂等防重复转化。裁决：转化前查询 `ErpPurOrder` where `requisitionId=该请购 AND docStatus≠CANCELLED`——存在则抛 `NopException(ERR_REQ_ALREADY_CONVERTED)`（**纯查询，无 ORM FK 改动**；`requisitionId` 列已存在）。原订单作废（docStatus=CANCELLED）后允许重新转化。备选（被否）：返回既有订单——掩盖重复操作意图。残留风险：并发同请购转化需乐观锁（`version` 列）兜底，重复插入由查询+事务隔离保护（无 DB 唯一约束时由事务隔离 + 查询保证，边界场景记为 Follow-up）。
  - Skill: none
- [x] `Add`：`convertToOrder(requisitionId, request)`——(1) 校验请购 `approveStatus=APPROVED`（否则 `ERR_REQ_NOT_APPROVED`）；(2) 校验供应商一致性（上述 Decision）；(3) 幂等查询（上述 Decision）；(4) `RequisitionToOrderConverter` 组装 `ErpPurOrder`(UNSUBMITTED/DRAFT，回链 `requisitionId`、`supplierId`、调用方 `warehouseId`/`currencyId`、`businessDate`/`orgId`) + 行（复制 `materialId`/`skuId`/`uoMId`/`quantity`/`projectId`，写入 `unitPrice`/计算金额族 VARCHAR）；(5) 持久化订单 + 行；(6) 返回订单。
  - Skill: none
- [x] `Proof`：`testConvertApprovedReqToOrder`（APPROVED 请购 + 补充字段 → 订单 UNSUBMITTED + 行/字段/`requisitionId` 回链/金额族正确）、`testConvertNotApprovedRejected`（非 APPROVED 请购 → `ERR_REQ_NOT_APPROVED`）、`testConvertMixedSupplierRejected`（行供应商不一致 → `ERR_REQ_MIXED_OR_MISSING_SUPPLIER`）、`testConvertIdempotentRejected`（已转化 → `ERR_REQ_ALREADY_CONVERTED`；作废原订单后可重新转化）、`testConvertedOrderThenApprove`（转化产物走 Phase 1 审核到 APPROVED，证明两阶段衔接）。`mvn test -pl module-purchase/erp-pur-service -am` 全绿。
  - Skill: none

  > **实现注记**：`ConvertToOrderRequest` DTO 落于 `erp-pur-dao/.../biz/`（purchase 域内部，不进 api 模块）；`RequisitionToOrderConverter` 为非 BizModel bean（经 `app-service.beans.xml` 注册，对齐 `ReceiveStockMoveBuilder` 模式），负责订单头/行组装 + 金额族 VARCHAR 计算（`amount`=`unitPrice`×`quantity`、`taxAmount`=`amount`×`taxRate`/100 保留2位、`amountWithTax`=`amount`+`taxAmount`）+ 单价空/非法/负数防护（抛 `ERR_INVALID_UNIT_PRICE`）。订单行 `skuId` 因请购行无此列而置 null（请购是数量型意向单）。`unitPrice`/`taxRate` VARCHAR 经 domain 格式化读回可能含尾随零（如 "5"→"5.0000"），测试以 BigDecimal 数值比较（VARCHAR 金额约定的标准做法）。

Exit Criteria:

> 本阶段交付请购→订单转化 + 幂等 + 回链。完整仓库 `mvn test` 归 Closure Gates。

- [x] 5 个转化行为测试存在且 `mvn test -pl module-purchase/erp-pur-service -am` 全绿
- [x] 转化产物订单可被 Phase 1 审核状态机推进到 APPROVED（证明两阶段衔接）

### Phase 3 - 端到端串联 + 收尾

Status: completed
Targets: `module-purchase/erp-pur-service/src/test/.../`、`docs/logs/2026/{执行当日 month-day}.md`、`docs/backlog/core-business-roadmap.md`
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 2

- [x] `Proof`：端到端测试 `testRequisitionToOrderToEnd`（建请购→提交→审核 APPROVED→转化生成订单(UNSUBMITTED)→提交→审核 APPROVED→（订单审核纯状态，不下游触发）→作废订单→可重新转化）。证明请购→订单前端循环打通 + 与 Phase 1/2 衔接。`mvn test -pl module-purchase/erp-pur-service -am` 全绿。
  - Skill: none
- [x] `Add`：更新当日开发日志 `docs/logs/2026/{执行当日 month-day}.md`（按 `docs/logs/00-log-writing-guide.md`，时间倒序），记录请购→订单转化 + 订单审核状态机落地 + 验证状态。
  - Skill: none
- [x] `Add`：`docs/backlog/core-business-roadmap.md` 工作项 1.0a 标注 `done`、1.1「订单审核状态机」段标注 `done`（入库触发段已于 1132-1 done；订单审核/发票/付款/三单匹配中订单审核 done，其余仍 todo）。
  - Skill: none

Exit Criteria:

> 本阶段交付端到端打通 + 文档对齐。完整仓库 `mvn test` 归 Closure Gates。

- [x] 端到端测试存在且 `mvn test -pl module-purchase/erp-pur-service -am` 全绿
- [x] 当日日志已记；roadmap 工作项 1.0a / 1.1 订单审核段进展已标注

## Draft Review Record

- Independent draft review iteration 1: **accept / consensus**（ses_0e39ea771ffepa3YpJev3yExHN，独立 general 子代理，新会话）— 全部材料性主张经实时仓库核实属实：`ErpPurOrderBizModel`/`ErpPurRequisitionBizModel` 为空壳（仅构造器）、`IErpPur*Biz` 空 `ICrudBiz`；实体字段（`ErpPurOrder.requisitionId` 可空、`supplierId`/`businessDate`/`currencyId` mandatory、`warehouseId` 非mandatory、金额族 VARCHAR、`ErpPurOrderLine.unitPrice`/`amount` VARCHAR+mandatory、请购头无 supplier/warehouse/currency/amount、请购行无 unitPrice 有可空 `suggestedSupplierId`）；dict 值（approve-status 10/20/30/40、doc-status 10/20/30）；模块依赖（inventory-dao/master-data-dao compile、inventory-service/master-data-service test 均已存在，「无新增依赖」声明正确）；核心前提 `state-machine.md:77`「采购订单｜仅状态推进」核实；Non-Goals 相对 roadmap 1.0a 与 1132-1 Deferred 无规则 10 静默降级；规则 14 合并（1.0a + 订单审核）经结果表面 + 定向后继证成；反松弛净、Follow-up/Deferred 均带命名触发条件、项目类型标注完整、Decision 均带备选 + 残留风险、Closure Gates 完整。无阻塞。非阻塞建议已吸收：S1 错误码复用消息文案绑定入库单参数（`ARG_RECEIVE_CODE`/「入库单 {receiveCode}…」）→ Phase 1 Add 已增「错误码复用注意」要求泛化消息或新增订单/请购作用域码；S2 Task Route 规则 14 措辞由「共享 state-machine.md owner doc」更正为「purchase 域 owner doc 族（订单：state-machine.md，请购：requisition.md）」，因 state-machine.md §适用对象不含请购单；S3 转化幂等的并发仅靠事务隔离 + 查询（`requisitionId` 为普通可空列、无 DB 唯一约束）已在 Phase 2 Decision 残留风险/Follow-up 记录，保留。**共识达成**：计划为可接受的执行契约，Plan Status 升级为 `active`。

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。结束时运行一次完整仓库验证。

- [x] 范围内行为完成：订单+请购三轴审核状态机 + 供应商启用校验 + 请购→订单转化（幂等/回链/缺失字段补全）全部落地，行为测试通过
- [x] 相关文档对齐：`core-business-roadmap.md` 工作项 1.0a / 1.1 订单审核段标注进展；当日日志已记
- [x] 已运行验证：`mvn test -pl module-purchase/erp-pur-service -am` 全绿（30 测试）；根 `mvn test -fae` = BUILD SUCCESS（146 reactor 模块，无回归）
- [x] 无范围内项目降级为 deferred/follow-up（RFQ 比价/请购合并/价格清单自动带价/多供应商分组/发票付款三单匹配/nop-wf 均为计划内 Non-Goal，非范围内降级）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：Plan Status、各 Phase Status、Exit Criteria、Closure Gates、日志一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于 `Closure` 节

## Deferred But Adjudicated

### RFQ 询价比价流程（ErpPurRfq/ErpPurQuotation）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 属 requisition.md「转询价」分支，独立的寻源策略面（询价发布/报价登记/中标/比价算法）；本计划只覆盖「直接转订单」。
- Successor Required: yes（触发条件：实施 RFQ 寻源/比价工作项时）

### 请购合并（多部门同物料合并到一张 RFQ/订单）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: requisition.md 业务规则 5，独立关注点。
- Successor Required: yes（触发条件：实施请购合并/集采时）

### 供应商价格清单自动带价

- Classification: `optimization candidate`
- Why Not Blocking Closure: 转化 `unitPrice` 由调用方提供；价格清单匹配是独立寻源逻辑（`supplier-evaluation.md` 边界）。
- Successor Required: yes（触发条件：实施供应商价格清单维护/自动带价时）

### 多供应商分组转化

- Classification: `optimization candidate`
- Why Not Blocking Closure: MVP 要求单请购单供应商；分组生成多张订单与幂等键冲突。
- Successor Required: yes（触发条件：出现一请购多供应商场景时）

### nop-wf 审批流编排

- Classification: `optimization candidate`
- Why Not Blocking Closure: 审核 = 直接状态迁移 + `@BizMutation`（对齐 1132-1/StockMove 基线）；nop-wf（多级审批人路由/挂起/撤销提交收紧）属运营基础设施。
- Successor Required: yes（触发条件：需要多级审批人路由与流程挂起时）

## Closure

Status Note: 计划可关闭——请购/订单三轴审核状态机 + 供应商启用校验 + 请购→订单转化（幂等/回链/缺失字段补全）全部落地，行为测试全绿（模块 30 测试，含本计划新增 16：订单状态机 5 + 请购状态机 4 + 转化 6 + 端到端 1），`mvn clean install -DskipTests` = BUILD SUCCESS，根 `mvn test -fae` = BUILD SUCCESS（146 reactor 模块，无回归），当日日志已记，roadmap 工作项 1.0a 标 `done`、1.1 订单审核段标 `done`。独立结束审计已由新会话子代理执行并通过（见下方证据）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，非执行者上下文）。逐项打开实时仓库文件核实（非采信自查）。
- 接口契约核实：`IErpPurOrderBiz`（6 方法：submit/withdrawSubmit/approve/reject/reverseApprove/cancel，`@SingleSession @Transactional`）、`IErpPurRequisitionBiz`（同 6 方法 + `convertToOrder(requisitionId, ConvertToOrderRequest)`）签名与计划 Decision 一致；`ConvertToOrderRequest` DTO 落于 `erp-pur-dao/.../biz/`（purchase 域内部，不进 api 模块），符合 Phase 2 Decision。
- 实现反空壳核实：`ErpPurOrderBizModel` 6 方法均为完整迁移逻辑（无空体/无 return null 占位），`approve` 幂等（已 APPROVED 空操作）+ `submit`/`approve` 双点 `requireSupplierActive`（`daoFor(ErpMdPartner.class)` 机制 B 读 `status`，停用抛 `ERR_PARTNER_INACTIVE`），`reverseApprove` 目标态 REJECTED（非 UNSUBMITTED，对齐 §3/§11.4）；`ErpPurRequisitionBizModel.convertToOrder` 完整 5 步（APPROVED 校验→行非空→供应商一致性→幂等查询→组装持久化），`requireNotAlreadyConverted` 查 `requisitionId=X AND docStatus≠CANCELLED`（纯查询无 FK 改动，与 Decision 一致）；`RequisitionToOrderConverter` 金额族 VARCHAR 计算 + 单价空/非法/负数防护（抛 `ERR_INVALID_UNIT_PRICE`）均为真实逻辑。
- 订单审核纯状态推进核实：`ErpPurOrderBizModel.approve` 不含任何 `generateMove`/跨域调用/`posted` 接线——对齐 state-machine §2「采购订单｜仅状态推进」，与入库单审核触发库存移动实质性不同（符合 Non-Goal）。
- 错误码审查 S1 落实核实：`ErpPurErrors` 新增订单作用域码（`ERR_ORDER_ILLEGAL_STATUS_TRANSITION`/`ERR_ORDER_ILLEGAL_DOC_STATUS_TRANSITION`/`ERR_ORDER_NOT_FOUND`/`ERR_ORDER_LINES_EMPTY`，绑定 `ARG_ORDER_CODE`）与请购作用域码（`ERR_REQ_*`，绑定 `ARG_REQUISITION_CODE`），未复用入库单 `ARG_RECEIVE_CODE`/「入库单…」文案。
- 测试覆盖核实：`TestErpPurOrderApproval`（5：submit/approve/reject/resubmit + rejectAndResubmit + illegalTransition + inactiveSupplier + cancelFromDraft）、`TestErpPurRequisitionApproval`（4：同构 + cancel）、`TestErpPurRequisitionConvertToOrder`（6：approved/notApproved/mixedSupplier/missingSupplier/idempotent/thenApprove）、`TestErpPurRequisitionToOrderEnd`（1：建请购→approve→convert→approve 订单→cancel→重新转化），共 16 新增，方法名与计划 Proof 项一致。
- 文档同步核实：`docs/logs/2026/07-01.md` 首条记录计划 1426-1（Phase 1-3 + 关键发现 + 验证全绿）；`docs/backlog/core-business-roadmap.md` 工作项 1.0a 标 `✅ done`、1.1 订单审核段标 done（入库触发段已于 1132-1 done）。
- 文本一致性核实：Plan Status=completed、3 个 Phase 均 `Status: completed`、各 Exit Criteria 全 `[x]`、Closure Gates 全 `[x]`、日志状态一致——五点一致。
- Deferred honesty 核实：5 项 Deferred（RFQ 比价/请购合并/价格清单自动带价/多供应商分组/nop-wf）均为计划内 Non-Goal，非范围内降级；均带命名后继触发条件，无隐藏的实时缺陷或契约漂移。
- 验证状态：执行者日志声明 `mvn test -pl module-purchase/erp-pur-service -am` 全绿（30 测试）+ 根 `mvn test -fae` BUILD SUCCESS（146 模块）。审计未重跑构建（信任执行者全绿记录 + 审计聚焦语义/反空壳/文档一致性，构建门控已在执行者会话验证）。
- 审计结论：PASS——范围内行为完整落地、无空壳/占位、接口与实现对齐计划 Decision、错误码审查 S1 已落实、测试覆盖证明行为、文档同步、Deferred 诚实、五点一致。

Follow-up:

- RFQ 询价比价流程（见上方 Deferred）
- 请购合并（见上方 Deferred）
- 供应商价格清单自动带价（见上方 Deferred）
- 多供应商分组转化（见上方 Deferred）
- nop-wf 审批流编排（见上方 Deferred）
- 订单作废的下游入库单影响处理（触发条件：实施 1.4 三单匹配/1.6 采购到付款端到端时，需裁决已审核订单作废是否级联影响下游入库/发票）
